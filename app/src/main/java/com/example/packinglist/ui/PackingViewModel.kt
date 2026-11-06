package com.example.packinglist.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.packinglist.data.Article
import com.example.packinglist.data.Availability
import com.example.packinglist.data.GitHubClient
import com.example.packinglist.data.GitHubSettings
import com.example.packinglist.data.GitHubSettingsStore
import com.example.packinglist.data.PackEntry
import com.example.packinglist.data.PackingDatabase
import com.example.packinglist.data.PackingRepository
import com.example.packinglist.data.PackingState
import com.example.packinglist.data.PhotoStore
import com.example.packinglist.data.Season
import com.example.packinglist.data.Trip
import com.example.packinglist.data.TripCount
import com.example.packinglist.data.TripDates
import com.example.packinglist.data.TripType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class PackingUiState(
    val trips: List<Trip> = emptyList(),
    val counts: Map<Long, TripCount> = emptyMap(),
    val activeTrip: Trip? = null,
    val activeItems: List<PackEntry> = emptyList(),
    val dataTrip: Trip? = null,
    val articles: List<Article> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class PackingViewModel(application: Application) : AndroidViewModel(application) {

    private val database = PackingDatabase.get(application)
    private val dao = database.packingDao()
    private val repository = PackingRepository(
        dao = dao,
        preferences = application.getSharedPreferences("packing_list", Application.MODE_PRIVATE),
        appContext = application.applicationContext
    )
    private val githubSettingsStore = GitHubSettingsStore(application)
    private val githubClient = GitHubClient()

    private val activeTripId = MutableStateFlow<Long?>(null)
    private val dataTripId = MutableStateFlow<Long?>(null)

    private val _githubSettings = MutableStateFlow(githubSettingsStore.load())
    val githubSettings: StateFlow<GitHubSettings> = _githubSettings.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages = _messages.asSharedFlow()

    private data class Snapshot(
        val trips: List<Trip>,
        val counts: List<TripCount>,
        val articles: List<Article>,
        val activeTripId: Long?,
        val dataTripId: Long?
    )

    private val snapshot = combine(
        repository.observeTrips(),
        repository.observeTripCounts(),
        repository.observeArticles(),
        activeTripId,
        dataTripId
    ) { trips, counts, articles, activeId, dataId ->
        Snapshot(
            trips = trips,
            counts = counts,
            articles = articles,
            activeTripId = activeId,
            dataTripId = dataId
        )
    }

    val uiState: StateFlow<PackingUiState> = snapshot
        .flatMapLatest { current ->
            val activeTrip = current.trips.firstOrNull { it.id == current.activeTripId }
            val entriesFlow = if (activeTrip == null) {
                flowOf(emptyList())
            } else {
                repository.observePackEntries(activeTrip)
            }

            entriesFlow.map { entries ->
                PackingUiState(
                    trips = current.trips,
                    counts = current.counts.associateBy { it.tripId },
                    activeTrip = activeTrip,
                    activeItems = entries,
                    dataTrip = current.trips.firstOrNull { it.id == current.dataTripId },
                    articles = current.articles
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PackingUiState()
        )

    init {
        viewModelScope.launch(Dispatchers.IO) { repository.ensureSeeded() }
    }

    // --- Reiseauswahl -------------------------------------------------------

    fun openTrip(trip: Trip) {
        activeTripId.value = trip.id
    }

    fun leaveTrip() {
        activeTripId.value = null
    }

    fun createTrip(
        name: String,
        tripType: TripType,
        season: Season,
        startDate: Long,
        endDate: Long
    ) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return
        viewModelScope.launch {
            val existing = dao.findTrip(cleanName, tripType, season)
            if (existing != null) {
                activeTripId.value = existing.id
                dataTripId.value = existing.id
                _messages.tryEmit("Diese Reise existiert bereits und wurde geöffnet.")
                return@launch
            }
            val tripId = dao.insertTrip(
                Trip(
                    name = cleanName,
                    tripType = tripType,
                    season = season,
                    startDate = startDate,
                    endDate = endDate.coerceAtLeast(startDate)
                )
            )
            if (tripId > 0) {
                activeTripId.value = tripId
                dataTripId.value = tripId
            }
        }
    }

    fun updateTrip(trip: Trip) {
        val cleanName = trip.name.trim()
        if (cleanName.isBlank()) return
        viewModelScope.launch {
            val existing = dao.findTrip(cleanName, trip.tripType, trip.season)
            if (existing == null || existing.id == trip.id) {
                dao.updateTrip(
                    trip.copy(
                        name = cleanName,
                        endDate = trip.endDate.coerceAtLeast(trip.startDate)
                    )
                )
            } else {
                _messages.tryEmit(
                    "Eine Reise mit diesem Namen gibt es für diese Reiseart und " +
                        "Jahreszeit bereits."
                )
            }
        }
    }

    fun deleteTrip(trip: Trip) {
        viewModelScope.launch {
            dao.deleteTrip(trip)
            if (activeTripId.value == trip.id) activeTripId.value = null
            if (dataTripId.value == trip.id) dataTripId.value = null
        }
    }

    // --- Packen -------------------------------------------------------------

    fun setPacked(entry: PackEntry, packed: Boolean) {
        val tripId = activeTripId.value ?: return
        viewModelScope.launch {
            database.withTransaction {
                val existing = dao.getPackingState(tripId, entry.article.id)
                dao.upsertPackingState(
                    PackingState(
                        tripId = tripId,
                        articleId = entry.article.id,
                        isPacked = packed,
                        packedAt = if (packed) System.currentTimeMillis() else null,
                        count = existing?.count ?: entry.count
                    )
                )
            }
        }
    }

    /** Zählt die Stücke eines Kleidungsstücks für die aktive Reise. */
    fun setCount(entry: PackEntry, count: Int) {
        val tripId = activeTripId.value ?: return
        val safeCount = count.coerceIn(0, MAX_CLOTHING_COUNT)
        val delta = safeCount - entry.count
        if (delta == 0) return
        viewModelScope.launch {
            database.withTransaction {
                val trip = dao.getTrip(tripId)
                val existing = dao.getPackingState(tripId, entry.article.id)
                val previousCount = existing?.count ?: 0
                val newCount = (previousCount + delta)
                    .coerceIn(0, MAX_CLOTHING_COUNT)
                val targetDays = trip?.let {
                    TripDates.daysInclusive(it.startDate, it.endDate) + 1
                }
                val justReachedTarget = targetDays != null &&
                    newCount >= targetDays &&
                    previousCount < targetDays
                val wasPacked = existing?.isPacked == true
                val isPacked = wasPacked || justReachedTarget
                dao.upsertPackingState(
                    PackingState(
                        tripId = tripId,
                        articleId = entry.article.id,
                        isPacked = isPacked,
                        packedAt = if (isPacked) {
                            existing?.packedAt ?: System.currentTimeMillis()
                        } else {
                            null
                        },
                        count = newCount
                    )
                )
            }
        }
    }

    fun resetPacked(tripId: Long) {
        viewModelScope.launch { dao.resetPacked(tripId) }
    }

    // --- Datenbank / Verwaltung --------------------------------------------

    fun selectDataTrip(tripId: Long?) {
        dataTripId.value = tripId
    }

    fun useActiveTripInDatabase() {
        activeTripId.value?.let { dataTripId.value = it }
    }

    fun addArticle(
        name: String,
        tripType: TripType,
        availability: Availability,
        importance: Int,
        isClothing: Boolean,
        photoUri: Uri?
    ) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            if (dao.findArticle(cleanName, tripType, availability) != null) {
                _messages.tryEmit(
                    "Dieser Artikel existiert für diese Reiseart und Saison bereits."
                )
                return@launch
            }
            val photoPath = photoUri?.let { PhotoStore.save(getApplication(), it) }
            val articleId = dao.insertArticle(
                Article(
                    name = cleanName,
                    tripType = tripType,
                    availability = availability,
                    importance = importance.coerceIn(1, 3),
                    isClothing = isClothing,
                    photoPath = photoPath,
                    sortOrder = dao.nextArticleSortOrder()
                )
            )
            if (articleId > 0 && photoPath != null) {
                uploadPhotoToGitHub(
                    articleId = articleId,
                    articleName = cleanName,
                    localPath = photoPath,
                    oldRemotePath = null
                )
            }
        }
    }

    fun updateArticle(article: Article, photoUri: Uri?, removePhoto: Boolean) {
        val cleanName = article.name.trim()
        if (cleanName.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val duplicate = dao.findArticle(cleanName, article.tripType, article.availability)
            if (duplicate != null && duplicate.id != article.id) {
                _messages.tryEmit(
                    "Dieser Artikel existiert für diese Reiseart und Saison bereits."
                )
                return@launch
            }

            val existing = dao.getArticle(article.id) ?: return@launch
            var localPath = existing.photoPath
            var remoteUrl = existing.photoUrl
            var remotePath = existing.photoRemotePath
            var oldRemoteToDelete: String? = null
            var newLocalToUpload: String? = null

            if (removePhoto) {
                PhotoStore.delete(existing.photoPath)
                localPath = null
                remoteUrl = null
                remotePath = null
                oldRemoteToDelete = existing.photoRemotePath
            }
            if (photoUri != null) {
                val saved = PhotoStore.save(getApplication(), photoUri)
                if (saved != null) {
                    PhotoStore.delete(existing.photoPath)
                    localPath = saved
                    remoteUrl = null
                    remotePath = null
                    oldRemoteToDelete = existing.photoRemotePath
                    newLocalToUpload = saved
                }
            }

            if (existing.tripType != article.tripType ||
                existing.availability != article.availability
            ) {
                dao.clearStatesForArticle(article.id)
            }

            dao.updateArticle(
                article.copy(
                    name = cleanName,
                    importance = article.importance.coerceIn(1, 3),
                    photoPath = localPath,
                    photoUrl = remoteUrl,
                    photoRemotePath = remotePath
                )
            )

            val settings = githubSettingsStore.load()
            if (!oldRemoteToDelete.isNullOrBlank() && settings.isConfigured) {
                githubClient.deletePhoto(settings, oldRemoteToDelete)
            }
            if (newLocalToUpload != null) {
                uploadPhotoToGitHub(
                    articleId = article.id,
                    articleName = cleanName,
                    localPath = newLocalToUpload,
                    oldRemotePath = null
                )
            }
        }
    }

    fun deleteArticle(article: Article) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteArticle(article)
            PhotoStore.delete(article.photoPath)

            val settings = githubSettingsStore.load()
            val remotePath = article.photoRemotePath
            if (settings.isConfigured && !remotePath.isNullOrBlank()) {
                githubClient.deletePhoto(settings, remotePath)
            }
        }
    }

    // --- GitHub -------------------------------------------------------------

    fun saveGitHubSettings(settings: GitHubSettings) {
        val cleaned = settings.copy(
            owner = settings.owner.trim(),
            repo = settings.repo.trim(),
            branch = settings.branch.trim().ifBlank { "main" },
            directory = settings.directory.trim().trim('/'),
            token = settings.token.trim()
        )
        githubSettingsStore.save(cleaned)
        _githubSettings.value = githubSettingsStore.load()
        _messages.tryEmit("GitHub-Einstellungen gespeichert.")
    }

    fun testGitHubConnection(settings: GitHubSettings) {
        if (!settings.isConfigured) {
            _messages.tryEmit("Bitte Owner, Repository und Token ausfüllen.")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            githubClient.testConnection(settings)
                .onSuccess { _messages.tryEmit(it) }
                .onFailure {
                    _messages.tryEmit("GitHub-Test fehlgeschlagen: ${it.message}")
                }
        }
    }

    /**
     * Lädt alle lokal vorhandenen Fotos hoch, die noch keine GitHub-URL haben.
     * Damit lassen sich Fotos nachträglich sichern, die vor dem Einrichten
     * von GitHub hinzugefügt wurden.
     */
    fun uploadLocalPhotos() {
        viewModelScope.launch(Dispatchers.IO) {
            val settings = githubSettingsStore.load()
            if (!settings.isConfigured) {
                _messages.tryEmit("Bitte zuerst die GitHub-Einstellungen speichern.")
                return@launch
            }

            val pending = dao.getArticlesOnce().filter { article ->
                val file = article.photoPath?.let { File(it) }
                file != null && file.exists() && article.photoRemotePath.isNullOrBlank()
            }
            if (pending.isEmpty()) {
                _messages.tryEmit("Keine neuen lokalen Fotos zum Hochladen gefunden.")
                return@launch
            }

            var uploaded = 0
            pending.forEach { article ->
                val file = article.photoPath?.let { File(it) } ?: return@forEach
                githubClient.uploadPhoto(settings, file, article.name)
                    .onSuccess { remote ->
                        dao.updateArticle(
                            article.copy(
                                photoUrl = remote.downloadUrl,
                                photoRemotePath = remote.remotePath
                            )
                        )
                        uploaded += 1
                    }
                    .onFailure {
                        _messages.tryEmit(
                            "Upload von „${article.name}“ fehlgeschlagen: ${it.message}"
                        )
                    }
            }
            _messages.tryEmit("$uploaded von ${pending.size} Fotos auf GitHub gespeichert.")
        }
    }

    private suspend fun uploadPhotoToGitHub(
        articleId: Long,
        articleName: String,
        localPath: String,
        oldRemotePath: String?
    ) {
        val settings = githubSettingsStore.load()
        if (!settings.isConfigured) {
            _messages.tryEmit(
                "Foto lokal gespeichert. Für GitHub bitte zuerst die Einstellungen ausfüllen."
            )
            return
        }

        val file = File(localPath)
        if (!file.exists()) return

        githubClient.uploadPhoto(settings, file, articleName)
            .onSuccess { remote ->
                val current = dao.getArticle(articleId)
                if (current == null || current.photoPath != localPath) {
                    // Artikel gelöscht oder Foto inzwischen ersetzt.
                    githubClient.deletePhoto(settings, remote.remotePath)
                    return@onSuccess
                }
                dao.updateArticle(
                    current.copy(
                        photoUrl = remote.downloadUrl,
                        photoRemotePath = remote.remotePath
                    )
                )
                if (!oldRemotePath.isNullOrBlank() &&
                    oldRemotePath != remote.remotePath
                ) {
                    githubClient.deletePhoto(settings, oldRemotePath)
                }
                _messages.tryEmit("Foto auf GitHub gespeichert.")
            }
            .onFailure {
                _messages.tryEmit(
                    "Foto lokal gespeichert, GitHub-Upload fehlgeschlagen: ${it.message}"
                )
            }
    }

    private companion object {
        const val MAX_CLOTHING_COUNT = 99
    }
}
