package com.example.packinglist.ui.screens

import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Luggage
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.packinglist.data.Article
import com.example.packinglist.data.Availability
import com.example.packinglist.data.Season
import com.example.packinglist.data.Trip
import com.example.packinglist.data.TripCount
import com.example.packinglist.data.TripDates
import com.example.packinglist.data.TripType
import com.example.packinglist.data.importanceLabel
import com.example.packinglist.ui.components.AppDropdown
import com.example.packinglist.ui.components.ArticleFormDialog
import com.example.packinglist.ui.components.ArticlePhoto
import com.example.packinglist.ui.components.ArticlePhotoLarge
import com.example.packinglist.ui.components.ConfirmDialog
import com.example.packinglist.ui.components.StatusBadge
import com.example.packinglist.ui.components.TripFormDialog
import com.example.packinglist.ui.theme.LocalStatusColors

@Composable
fun DatabaseScreen(
    trips: List<Trip>,
    counts: Map<Long, TripCount>,
    dataTrip: Trip?,
    articles: List<Article>,
    onSelectTrip: (Long?) -> Unit,
    onCreateTrip: (String, TripType, Season, Long, Long) -> Unit,
    onUpdateTrip: (Trip) -> Unit,
    onDeleteTrip: (Trip) -> Unit,
    onAddArticle: (String, TripType, Availability, Int, Boolean, Uri?) -> Unit,
    onUpdateArticle: (Article, Uri?, Boolean) -> Unit,
    onDeleteArticle: (Article) -> Unit,
    onOpenPackList: (Trip) -> Unit
) {
    val sortedTrips = remember(trips) {
        trips.sortedWith(compareBy({ it.tripType.name }, { it.name.lowercase() }))
    }
    val count = dataTrip?.let { counts[it.id] }

    var showCreateTripDialog by remember { mutableStateOf(false) }
    var showEditTripDialog by remember { mutableStateOf(false) }
    var showDeleteTripDialog by remember { mutableStateOf(false) }
    var showAddArticleDialog by remember { mutableStateOf(false) }
    var articleToEdit by remember { mutableStateOf<Article?>(null) }
    var articleToDelete by remember { mutableStateOf<Article?>(null) }

    var articleQuery by rememberSaveable { mutableStateOf("") }
    var filterType by rememberSaveable { mutableStateOf<TripType?>(null) }
    var filterAvailability by rememberSaveable { mutableStateOf<Availability?>(null) }

    val filteredArticles = remember(articles, articleQuery, filterType, filterAvailability) {
        val query = articleQuery.trim()
        articles.filter { article ->
            (filterType == null || article.tripType == filterType) &&
                (filterAvailability == null || article.availability == filterAvailability) &&
                (query.isBlank() || article.name.contains(query, ignoreCase = true))
        }
    }

    LaunchedEffect(trips, dataTrip) {
        if (trips.isNotEmpty() && dataTrip == null) {
            onSelectTrip(sortedTrips.first().id)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Datenbank",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Verwalte deine Reisen und den Artikelkatalog. Fotos werden " +
                        "lokal auf dem Gerät gespeichert.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.size(4.dp))
                Button(onClick = { showCreateTripDialog = true }) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.size(8.dp))
                    Text("Neue Reise")
                }
            }
        }

        item(key = "trip-selector") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Reise auswählen",
                        style = MaterialTheme.typography.titleMedium
                    )
                    AppDropdown(
                        label = "Reise",
                        selected = dataTrip,
                        options = sortedTrips,
                        optionLabel = {
                            "${it.name} · ${it.tripType.label} · ${it.season.label}"
                        },
                        selectedLabel = { it.name },
                        onSelect = { onSelectTrip(it.id) },
                        enabled = sortedTrips.isNotEmpty()
                    )
                    if (sortedTrips.isEmpty()) {
                        Text(
                            text = "Noch keine Reisen vorhanden. Lege oben eine neue Reise an.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (dataTrip != null) {
            item(key = "trip-overview") {
                TripOverviewCard(
                    trip = dataTrip,
                    count = count,
                    onEdit = { showEditTripDialog = true },
                    onDelete = { showDeleteTripDialog = true },
                    onOpenPackList = { onOpenPackList(dataTrip) }
                )
            }
        }

        item(key = "catalog-header") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Storage,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.size(8.dp))
                            Text(
                                text = "Artikel-Katalog",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                        Text(
                            text = "${filteredArticles.size} von ${articles.size} Artikeln",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(onClick = { showAddArticleDialog = true }) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.size(6.dp))
                        Text("Artikel")
                    }
                }

                OutlinedTextField(
                    value = articleQuery,
                    onValueChange = { articleQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Artikel suchen") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        if (articleQuery.isNotEmpty()) {
                            IconButton(onClick = { articleQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Rounded.Clear,
                                    contentDescription = "Suche löschen"
                                )
                            }
                        }
                    },
                    singleLine = true
                )

                Text(
                    text = "Reiseart",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filterType == null,
                        onClick = { filterType = null },
                        label = { Text("Alle") }
                    )
                    TripType.entries.forEach { type ->
                        FilterChip(
                            selected = filterType == type,
                            onClick = { filterType = type },
                            label = { Text(type.shortLabel) }
                        )
                    }
                }

                Text(
                    text = "Saison",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filterAvailability == null,
                        onClick = { filterAvailability = null },
                        label = { Text("Alle") }
                    )
                    Availability.entries.forEach { availability ->
                        FilterChip(
                            selected = filterAvailability == availability,
                            onClick = { filterAvailability = availability },
                            label = {
                                Text(
                                    if (availability == Availability.ALWAYS) "♾️ Immer"
                                    else "${availability.emoji} ${availability.label}"
                                )
                            }
                        )
                    }
                }
            }
        }

        if (filteredArticles.isEmpty()) {
            item(key = "catalog-empty") {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (articles.isEmpty()) {
                            "Der Artikelkatalog ist leer. Lege deinen ersten Artikel an."
                        } else {
                            "Kein Artikel passt zu Suche und Filter."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(filteredArticles, key = { "article-${it.id}" }) { article ->
                AdminArticleRow(
                    article = article,
                    onEdit = { articleToEdit = article },
                    onDelete = { articleToDelete = article }
                )
            }
        }
    }

    if (showCreateTripDialog) {
        TripFormDialog(
            title = "Neue Reise anlegen",
            confirmLabel = "Reise anlegen",
            onDismiss = { showCreateTripDialog = false },
            onConfirm = { name, type, season, startDate, endDate ->
                onCreateTrip(name, type, season, startDate, endDate)
            },
            initialTripType = TripType.PRIVATE,
            initialSeason = Season.SUMMER
        )
    }

    if (showEditTripDialog && dataTrip != null) {
        TripFormDialog(
            title = "Reise bearbeiten",
            confirmLabel = "Speichern",
            onDismiss = { showEditTripDialog = false },
            onConfirm = { name, type, season, startDate, endDate ->
                onUpdateTrip(
                    dataTrip.copy(
                        name = name,
                        tripType = type,
                        season = season,
                        startDate = startDate,
                        endDate = endDate
                    )
                )
            },
            initialName = dataTrip.name,
            initialTripType = dataTrip.tripType,
            initialSeason = dataTrip.season,
            initialStartDate = dataTrip.startDate,
            initialEndDate = dataTrip.endDate
        )
    }

    if (showDeleteTripDialog && dataTrip != null) {
        ConfirmDialog(
            title = "Reise löschen?",
            text = "„${dataTrip.name}“ und der zugehörige Packstand werden dauerhaft " +
                "entfernt. Die Artikel im Katalog bleiben erhalten.",
            confirmLabel = "Löschen",
            destructive = true,
            onConfirm = {
                onDeleteTrip(dataTrip)
                showDeleteTripDialog = false
            },
            onDismiss = { showDeleteTripDialog = false }
        )
    }

    if (showAddArticleDialog) {
        ArticleFormDialog(
            title = "Neuer Artikel",
            confirmLabel = "Hinzufügen",
            onDismiss = { showAddArticleDialog = false },
            onConfirm = { name, type, availability, importance, isClothing, photoUri, _ ->
                onAddArticle(name, type, availability, importance, isClothing, photoUri)
            },
            initialTripType = filterType ?: TripType.PRIVATE,
            initialAvailability = filterAvailability ?: Availability.ALWAYS
        )
    }

    val editingArticle = articleToEdit
    if (editingArticle != null) {
        ArticleFormDialog(
            title = "Artikel bearbeiten",
            confirmLabel = "Speichern",
            onDismiss = { articleToEdit = null },
            onConfirm = { name, type, availability, importance, isClothing, photoUri, removePhoto ->
                onUpdateArticle(
                    editingArticle.copy(
                        name = name,
                        tripType = type,
                        availability = availability,
                        importance = importance,
                        isClothing = isClothing
                    ),
                    photoUri,
                    removePhoto
                )
            },
            initialName = editingArticle.name,
            initialTripType = editingArticle.tripType,
            initialAvailability = editingArticle.availability,
            initialImportance = editingArticle.importance,
            initialIsClothing = editingArticle.isClothing,
            existingPhotoPath = editingArticle.photoPath,
            existingPhotoUrl = editingArticle.photoUrl
        )
    }

    val deletingArticle = articleToDelete
    if (deletingArticle != null) {
        ConfirmDialog(
            title = "Artikel löschen?",
            text = "„${deletingArticle.name}“ wird aus dem Katalog entfernt und aus allen " +
                "Packlisten gestrichen.",
            confirmLabel = "Löschen",
            destructive = true,
            onConfirm = {
                onDeleteArticle(deletingArticle)
                articleToDelete = null
            },
            onDismiss = { articleToDelete = null }
        )
    }
}

@Composable
private fun TripOverviewCard(
    trip: Trip,
    count: TripCount?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenPackList: () -> Unit
) {
    val statusColors = LocalStatusColors.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = trip.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatusBadge(
                            text = trip.tripType.label,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        StatusBadge(
                            text = "${trip.season.emoji} ${trip.season.label}",
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "${TripDates.formatRange(trip.startDate, trip.endDate)} · " +
                            TripDates.daysLabel(
                                TripDates.daysInclusive(trip.startDate, trip.endDate)
                            ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Reise bearbeiten",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Reise löschen",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(
                    text = "${count?.total ?: 0} Artikel",
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
                StatusBadge(
                    text = "${count?.packed ?: 0} gepackt",
                    containerColor = statusColors.packedContainer,
                    contentColor = statusColors.packed
                )
            }

            OutlinedButton(
                onClick = onOpenPackList,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.Luggage,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text("Packliste öffnen")
            }
        }
    }
}

@Composable
private fun AdminArticleRow(
    article: Article,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val importanceColor = when (article.importance) {
        1 -> MaterialTheme.colorScheme.error
        2 -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.outline
    }
    var expanded by rememberSaveable(article.id) { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.animateContentSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { expanded = !expanded },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ArticlePhoto(
                        photoPath = article.photoPath,
                        photoUrl = article.photoUrl,
                        size = 52.dp,
                        showPlaceholder = true,
                        contentDescription = "Foto von ${article.name}"
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = article.name,
                            style = if (expanded) MaterialTheme.typography.titleLarge
                            else MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(importanceColor, CircleShape)
                            )
                            Text(
                                text = "${article.importanceLabel} · " +
                                    "${article.tripType.shortLabel} · " +
                                    if (article.availability == Availability.ALWAYS) {
                                        "Immer"
                                    } else {
                                        "${article.availability.emoji} " +
                                            article.availability.label
                                    } +
                                    if (article.isClothing) " · Kleidung" else "",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.ExpandLess
                        else Icons.Rounded.ExpandMore,
                        contentDescription = if (expanded) "Artikel verkleinern"
                        else "Artikel vergrößern",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Artikel bearbeiten"
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Artikel löschen"
                    )
                }
            }

            if (expanded) {
                ArticlePhotoLarge(
                    photoPath = article.photoPath,
                    photoUrl = article.photoUrl,
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                    contentDescription = "Großes Foto von ${article.name}"
                )
            }
        }
    }
}
