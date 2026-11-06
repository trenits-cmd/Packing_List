package com.example.packinglist.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Luggage
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.packinglist.data.Availability
import com.example.packinglist.data.PackEntry
import com.example.packinglist.data.Trip
import com.example.packinglist.data.TripDates
import com.example.packinglist.data.importanceLabel
import com.example.packinglist.ui.components.ArticlePhoto
import com.example.packinglist.ui.components.ArticlePhotoLarge
import com.example.packinglist.ui.components.ConfirmDialog
import com.example.packinglist.ui.components.EmptyState
import com.example.packinglist.ui.components.SectionHeader
import com.example.packinglist.ui.components.StatusBadge
import com.example.packinglist.ui.theme.LocalStatusColors

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PackScreen(
    trip: Trip,
    items: List<PackEntry>,
    onLeaveTrip: () -> Unit,
    onPackedChange: (PackEntry, Boolean) -> Unit,
    onCountChange: (PackEntry, Int) -> Unit,
    onResetPacked: () -> Unit,
    onDeleteTrip: () -> Unit,
    onOpenDatabase: () -> Unit
) {
    val statusColors = LocalStatusColors.current
    val packedCount = items.count { it.isPacked }
    val totalCount = items.size
    val clothingTarget = TripDates.daysInclusive(trip.startDate, trip.endDate) + 1
    val openItems = remember(items) {
        items.filter { !it.isPacked }
            .sortedWith(
                compareBy(
                    { it.article.importance },
                    { it.article.sortOrder },
                    { it.article.name.lowercase() }
                )
            )
    }
    val packedItems = remember(items) {
        items.filter { it.isPacked }
            .sortedByDescending { it.packedAt ?: 0L }
    }
    var showResetDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onLeaveTrip) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Zur Reiseauswahl"
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = trip.name,
                        style = MaterialTheme.typography.headlineSmall,
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
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${TripDates.formatRange(trip.startDate, trip.endDate)} · " +
                            TripDates.daysLabel(
                                TripDates.daysInclusive(trip.startDate, trip.endDate)
                            ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Reise löschen",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (items.isNotEmpty()) {
                PackingProgressCard(packed = packedCount, total = totalCount)
            }
        }

        if (items.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.Luggage,
                title = "Keine Artikel für diese Reise",
                text = "Für diese Reiseart und Jahreszeit sind noch keine Artikel in der " +
                    "Datenbank hinterlegt. Füge sie jetzt hinzu.",
                actionLabel = "Artikel hinzufügen",
                onAction = onOpenDatabase,
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(key = "open-header") {
                    SectionHeader(
                        title = "Noch offen",
                        count = openItems.size,
                        color = statusColors.open
                    )
                }

                if (openItems.isEmpty()) {
                    item(key = "all-packed") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = statusColors.packedContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = statusColors.packed
                                )
                                Text(
                                    text = "Alles gepackt – gute Reise!",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = statusColors.onPackedContainer
                                )
                            }
                        }
                    }
                } else {
                    items(openItems, key = { it.article.id }) { entry ->
                        PackingItemCard(
                            entry = entry,
                            packed = false,
                            clothingTarget = clothingTarget,
                            onPackedChange = { packed -> onPackedChange(entry, packed) },
                            onCountChange = { count -> onCountChange(entry, count) },
                            modifier = Modifier.animateItemPlacement()
                        )
                    }
                }

                if (packedItems.isNotEmpty()) {
                    item(key = "packed-header") {
                        SectionHeader(
                            title = "Bereits gepackt",
                            count = packedItems.size,
                            color = statusColors.packed,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                    items(packedItems, key = { it.article.id }) { entry ->
                        PackingItemCard(
                            entry = entry,
                            packed = true,
                            clothingTarget = clothingTarget,
                            onPackedChange = { packed -> onPackedChange(entry, packed) },
                            onCountChange = { count -> onCountChange(entry, count) },
                            modifier = Modifier.animateItemPlacement()
                        )
                    }
                }

                item(key = "reset") {
                    if (packedCount > 0) {
                        TextButton(
                            onClick = { showResetDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.size(8.dp))
                            Text("Alle Häkchen zurücksetzen")
                        }
                    }
                }
            }
        }
    }

    if (showResetDialog) {
        ConfirmDialog(
            title = "Liste zurücksetzen?",
            text = "Alle gepackten Artikel werden wieder als offen markiert.",
            confirmLabel = "Zurücksetzen",
            onConfirm = {
                onResetPacked()
                showResetDialog = false
            },
            onDismiss = { showResetDialog = false }
        )
    }

    if (showDeleteDialog) {
        ConfirmDialog(
            title = "Reise löschen?",
            text = "„${trip.name}“ und der Packstand werden dauerhaft entfernt. " +
                "Die Artikel im Katalog bleiben erhalten.",
            confirmLabel = "Löschen",
            destructive = true,
            onConfirm = {
                showDeleteDialog = false
                onDeleteTrip()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@Composable
private fun PackingProgressCard(packed: Int, total: Int) {
    val statusColors = LocalStatusColors.current
    val fraction = if (total == 0) 0f else packed.toFloat() / total
    val done = total > 0 && packed == total

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (done) statusColors.packedContainer
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$packed von $total gepackt",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (done) statusColors.onPackedContainer
                    else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${(fraction * 100).toInt()} %",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (done) statusColors.packed
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = statusColors.packed,
                trackColor = statusColors.open.copy(alpha = 0.18f)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (done) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = statusColors.packed,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Fertig – alles ist im Koffer.",
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColors.onPackedContainer
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(statusColors.open, CircleShape)
                    )
                    Text(
                        text = "${total - packed} Artikel noch offen",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PackingItemCard(
    entry: PackEntry,
    packed: Boolean,
    clothingTarget: Int,
    onPackedChange: (Boolean) -> Unit,
    onCountChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColors = LocalStatusColors.current
    val article = entry.article
    val accent = if (packed) statusColors.packed else statusColors.open
    val container = if (packed) statusColors.packedContainer else statusColors.openContainer
    val content = if (packed) statusColors.onPackedContainer else statusColors.onOpenContainer

    var expanded by rememberSaveable(article.id) { mutableStateOf(false) }
    val headerModifier = Modifier
        .fillMaxWidth()
        .then(
            if (article.isClothing) {
                Modifier.clickable { expanded = !expanded }
            } else {
                Modifier.toggleable(
                    value = packed,
                    role = Role.Checkbox,
                    onValueChange = onPackedChange
                )
            }
        )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = container)
    ) {
        Column(
            modifier = Modifier
                .animateContentSize()
                .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = headerModifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            if (packed) accent else accent.copy(alpha = 0.12f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (packed) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = container,
                            modifier = Modifier.size(17.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(accent, CircleShape)
                        )
                    }
                }
                ArticlePhoto(
                    photoPath = article.photoPath,
                    photoUrl = article.photoUrl,
                    size = 44.dp,
                    contentDescription = "Foto von ${article.name}"
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = article.name,
                        style = if (expanded) MaterialTheme.typography.titleLarge
                        else MaterialTheme.typography.titleMedium,
                        color = content
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = article.importanceLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = accent
                        )
                        if (article.availability != Availability.ALWAYS) {
                            Text(
                                text = "· ${article.availability.emoji} ${article.availability.label}",
                                style = MaterialTheme.typography.labelMedium,
                                color = accent.copy(alpha = 0.8f)
                            )
                        }
                    }
                    Text(
                        text = when {
                            article.isClothing -> "Zähle die Stücke bis zum Ziel"
                            packed -> "Antippen zum Zurücksetzen"
                            else -> "Antippen zum Erledigen"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = accent.copy(alpha = 0.85f)
                    )
                }
                StatusBadge(
                    text = if (packed) "Gepackt" else "Offen",
                    containerColor = accent.copy(alpha = 0.14f),
                    contentColor = accent
                )
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.ExpandLess
                        else Icons.Rounded.ExpandMore,
                        contentDescription = if (expanded) "Artikel verkleinern"
                        else "Artikel vergrößern",
                        tint = accent
                    )
                }
            }

            if (article.isClothing) {
                ClothingCounter(
                    count = entry.count,
                    target = clothingTarget,
                    onCountChange = onCountChange
                )
            }

            if (expanded) {
                ArticlePhotoLarge(
                    photoPath = article.photoPath,
                    photoUrl = article.photoUrl,
                    modifier = Modifier.padding(top = 6.dp),
                    contentDescription = "Großes Foto von ${article.name}"
                )
                Text(
                    text = "${article.tripType.shortLabel} · " +
                        "${article.availability.label} · ${article.importanceLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = accent.copy(alpha = 0.85f),
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )
            }
        }
    }
}

/**
 * Stückzahl-Zähler für Kleidung. Ziel ist die Reisedauer in Tagen plus eins.
 * Die Farbe wandert mit der erreichten Menge von Rot über Gelb nach Grün.
 */
@Composable
private fun ClothingCounter(
    count: Int,
    target: Int,
    onCountChange: (Int) -> Unit
) {
    val statusColors = LocalStatusColors.current
    val fraction = if (target <= 0) 1f else (count.toFloat() / target).coerceIn(0f, 1f)
    val targetColor = if (fraction <= 0.5f) {
        lerp(statusColors.open, statusColors.warning, fraction * 2f)
    } else {
        lerp(statusColors.warning, statusColors.packed, (fraction - 0.5f) * 2f)
    }
    val color by animateColorAsState(targetColor, label = "clothingCountColor")
    val missing = (target - count).coerceAtLeast(0)
    val hint = when {
        count >= target -> "Kleidung · Ziel erreicht, erledigt"
        missing == 1 -> "Kleidung · 1 Stück fehlt"
        else -> "Kleidung · $missing Stücke fehlen"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onCountChange(count - 1) },
                    enabled = count > 0
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Remove,
                        contentDescription = "Ein Kleidungsstück weniger",
                        tint = color
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$count / $target",
                        style = MaterialTheme.typography.titleLarge,
                        color = color
                    )
                    Text(
                        text = hint,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { onCountChange(count + 1) }) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Ein Kleidungsstück mehr",
                        tint = color
                    )
                }
            }

            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = color,
                trackColor = color.copy(alpha = 0.2f)
            )
        }
    }
}
