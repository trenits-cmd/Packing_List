package com.example.packinglist.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.packinglist.data.Article
import com.example.packinglist.data.Availability
import com.example.packinglist.data.PackEntry
import com.example.packinglist.data.PackingDatabase
import com.example.packinglist.data.PackingRepository
import com.example.packinglist.data.PackingState
import com.example.packinglist.data.PhotoStore
import com.example.packinglist.data.Season
import com.example.packinglist.data.Trip
import com.example.packinglist.data.TripCount
import com.example.packinglist.data.TripType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

    private val activeTripId = MutableStateFlow<Long?>(null)
    private val dataTripId = MutableStateFlow<Long?>(null)

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
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

    fun createTrip(name: String, tripType: TripType, season: Season) {
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
                Trip(name = cleanName, tripType = tripType, season = season)
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
                dao.updateTrip(trip.copy(name = cleanName))
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
            dao.upsertPackingState(
                PackingState(
                    tripId = tripId,
                    articleId = entry.article.id,
                    isPacked = packed,
                    packedAt = if (packed) System.currentTimeMillis() else null
                )
            )
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
            dao.insertArticle(
                Article(
                    name = cleanName,
                    tripType = tripType,
                    availability = availability,
                    importance = importance.coerceIn(1, 3),
                    photoPath = photoPath,
                    sortOrder = dao.nextArticleSortOrder()
                )
            )
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
            var photoPath = existing.photoPath

            if (removePhoto) {
                PhotoStore.delete(existing.photoPath)
                photoPath = null
            }
            if (photoUri != null) {
                val saved = PhotoStore.save(getApplication(), photoUri)
                if (saved != null) {
                    PhotoStore.delete(existing.photoPath)
                    photoPath = saved
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
                    photoPath = photoPath
                )
            )
        }
    }

    fun deleteArticle(article: Article) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteArticle(article)
            PhotoStore.delete(article.photoPath)
        }
    }
}
