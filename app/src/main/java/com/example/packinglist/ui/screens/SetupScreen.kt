package com.example.packinglist.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BeachAccess
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.packinglist.data.Season
import com.example.packinglist.data.Trip
import com.example.packinglist.data.TripCount
import com.example.packinglist.data.TripType
import com.example.packinglist.ui.components.AppDropdown
import com.example.packinglist.ui.components.ConfirmDialog
import com.example.packinglist.ui.components.StatusBadge
import com.example.packinglist.ui.components.TripFormDialog

@Composable
fun SetupScreen(
    trips: List<Trip>,
    counts: Map<Long, TripCount>,
    onOpenTrip: (Trip) -> Unit,
    onCreateTrip: (String, TripType, Season) -> Unit,
    onDeleteTrip: (Trip) -> Unit,
    onOpenDatabase: () -> Unit
) {
    var tripType by rememberSaveable { mutableStateOf(TripType.PRIVATE) }
    var season by rememberSaveable { mutableStateOf(Season.SUMMER) }
    var query by rememberSaveable { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var tripToDelete by remember { mutableStateOf<Trip?>(null) }

    val cleanQuery = query.trim()
    val matches = remember(trips, tripType, season, cleanQuery) {
        trips
            .filter { trip ->
                trip.tripType == tripType &&
                    trip.season == season &&
                    (cleanQuery.isBlank() || trip.name.contains(cleanQuery, ignoreCase = true))
            }
            .sortedBy { it.name.lowercase() }
    }
    val exactMatch = matches.firstOrNull { it.name.equals(cleanQuery, ignoreCase = true) }
    val isSearching = cleanQuery.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Wohin geht's?",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Wähle Reiseart und Jahreszeit. Danach lädst du deine Packliste " +
                    "aus der Datenbank.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppDropdown(
                    label = "Reiseart",
                    selected = tripType,
                    options = TripType.entries.toList(),
                    optionLabel = { it.label },
                    onSelect = { tripType = it }
                )
                AppDropdown(
                    label = "Jahreszeit",
                    selected = season,
                    options = Season.entries.toList(),
                    optionLabel = { "${it.emoji} ${it.label}" },
                    onSelect = { season = it }
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Name der Reise") },
                placeholder = {
                    Text(
                        if (tripType == TripType.BUSINESS) "z. B. Geschäftsreise Berlin"
                        else "z. B. Sommerurlaub Italien"
                    )
                },
                singleLine = true,
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Eingabe löschen")
                        }
                    }
                }
            )

            Button(
                onClick = {
                    if (exactMatch != null) {
                        onOpenTrip(exactMatch)
                    } else {
                        showCreateDialog = true
                    }
                },
                enabled = cleanQuery.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (exactMatch != null) Icons.Rounded.ChevronRight
                    else Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    if (exactMatch != null) "Packliste öffnen"
                    else "Neue Reise anlegen"
                )
            }
        }

        Text(
            text = if (isSearching) "Treffer" else
                "Reisen für ${tripType.label} · ${season.label}",
            style = MaterialTheme.typography.titleMedium
        )

        if (trips.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Noch keine Reisen in der Datenbank.", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Lege zuerst eine Reise an – entweder über das Namensfeld oben " +
                            "oder direkt in der Datenbank.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = onOpenDatabase) {
                        Text("Zur Datenbank")
                    }
                }
            }
        } else if (matches.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Keine passende Reise gefunden", fontWeight = FontWeight.SemiBold)
                    Text(
                        if (cleanQuery.isBlank()) {
                            "Für ${tripType.label} · ${season.label} gibt es noch keine " +
                                "Reise. Mit „Neue Reise anlegen“ erstellst du sie."
                        } else {
                            "Für „$cleanQuery“ gibt es noch keine Liste. Mit „Neue Reise " +
                                "anlegen“ erstellst du sie."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            matches.forEach { trip ->
                TripSuggestionCard(
                    trip = trip,
                    count = counts[trip.id],
                    onClick = { onOpenTrip(trip) },
                    onDelete = { tripToDelete = trip }
                )
            }
        }

        TextButton(
            onClick = onOpenDatabase,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Rounded.Storage,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.size(8.dp))
            Text("Datenbank verwalten")
        }
    }

    if (showCreateDialog) {
        TripFormDialog(
            title = "Neue Reise anlegen",
            confirmLabel = "Anlegen & öffnen",
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, type, selectedSeason ->
                onCreateTrip(name, type, selectedSeason)
            },
            initialName = cleanQuery,
            initialTripType = tripType,
            initialSeason = season
        )
    }

    val deletingTrip = tripToDelete
    if (deletingTrip != null) {
        ConfirmDialog(
            title = "Reise löschen?",
            text = "„${deletingTrip.name}“ und der Packstand werden dauerhaft entfernt. " +
                "Die Artikel im Katalog bleiben erhalten.",
            confirmLabel = "Löschen",
            destructive = true,
            onConfirm = {
                onDeleteTrip(deletingTrip)
                tripToDelete = null
            },
            onDismiss = { tripToDelete = null }
        )
    }
}

@Composable
private fun TripSuggestionCard(
    trip: Trip,
    count: TripCount?,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector = if (trip.tripType == TripType.BUSINESS) Icons.Rounded.Work
                    else Icons.Rounded.BeachAccess,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(22.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = trip.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                    text = if (count == null || count.total == 0) {
                        "Noch keine Artikel"
                    } else {
                        "${count.total} Artikel · ${count.packed} gepackt"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Reise löschen",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
