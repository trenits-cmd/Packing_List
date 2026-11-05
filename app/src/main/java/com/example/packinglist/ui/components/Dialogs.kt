package com.example.packinglist.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import com.example.packinglist.data.Availability
import com.example.packinglist.data.Season
import com.example.packinglist.data.TripType
import java.io.File

@Composable
fun TripFormDialog(
    title: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, tripType: TripType, season: Season) -> Unit,
    initialName: String = "",
    initialTripType: TripType = TripType.PRIVATE,
    initialSeason: Season = Season.SUMMER
) {
    var name by remember { mutableStateOf(initialName) }
    var tripType by remember { mutableStateOf(initialTripType) }
    var season by remember { mutableStateOf(initialSeason) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name der Reise") },
                    placeholder = { Text("z. B. Sommerurlaub Italien") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
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
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(name.trim(), tripType, season)
                    onDismiss()
                },
                enabled = name.isNotBlank()
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

@Composable
fun ArticleFormDialog(
    title: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        tripType: TripType,
        availability: Availability,
        importance: Int,
        photoUri: android.net.Uri?,
        removePhoto: Boolean
    ) -> Unit,
    initialName: String = "",
    initialTripType: TripType = TripType.PRIVATE,
    initialAvailability: Availability = Availability.ALWAYS,
    initialImportance: Int = 1,
    existingPhotoPath: String? = null,
    existingPhotoUrl: String? = null
) {
    var name by remember { mutableStateOf(initialName) }
    var tripType by remember { mutableStateOf(initialTripType) }
    var availability by remember { mutableStateOf(initialAvailability) }
    var importance by remember { mutableStateOf(initialImportance) }
    var pickedPhotoUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var removePhoto by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pickedPhotoUri = uri
            removePhoto = false
        }
    }

    val localPhoto = existingPhotoPath
        ?.let { File(it) }
        ?.takeIf { it.exists() }
    val previewModel: Any? = when {
        removePhoto -> null
        pickedPhotoUri != null -> pickedPhotoUri
        localPhoto != null -> localPhoto
        existingPhotoUrl != null -> existingPhotoUrl
        else -> null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Artikel") },
                    placeholder = { Text("z. B. Regenjacke") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                AppDropdown(
                    label = "Reiseart",
                    selected = tripType,
                    options = TripType.entries.toList(),
                    optionLabel = { it.label },
                    onSelect = { tripType = it }
                )
                AppDropdown(
                    label = "Saison",
                    selected = availability,
                    options = Availability.entries.toList(),
                    optionLabel = {
                        if (it == Availability.ALWAYS) "♾️ Immer"
                        else "${it.emoji} ${it.label}"
                    },
                    onSelect = { availability = it }
                )
                AppDropdown(
                    label = "Wichtigkeit",
                    selected = importance,
                    options = listOf(1, 2, 3),
                    optionLabel = {
                        when (it) {
                            1 -> "1 – Sehr wichtig"
                            2 -> "2 – Wichtig"
                            else -> "3 – Optional"
                        }
                    },
                    onSelect = { importance = it }
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Foto",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (previewModel != null) {
                                AsyncImage(
                                    model = previewModel,
                                    contentDescription = "Vorschau des Artikelfotos",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Photo,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            OutlinedButton(
                                onClick = {
                                    photoPicker.launch(
                                        PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                }
                            ) {
                                Text(
                                    if (previewModel != null) "Foto ändern"
                                    else "Foto hinzufügen"
                                )
                            }
                            if (!removePhoto &&
                                (existingPhotoPath != null || pickedPhotoUri != null)
                            ) {
                                TextButton(
                                    onClick = {
                                        pickedPhotoUri = null
                                        removePhoto = true
                                    }
                                ) {
                                    Text("Foto entfernen")
                                }
                            }
                        }
                    }
                    Text(
                        text = "Fotos werden lokal gespeichert und – wenn GitHub " +
                            "eingerichtet ist – zusätzlich ins Repository hochgeladen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        name.trim(),
                        tripType,
                        availability,
                        importance,
                        pickedPhotoUri,
                        removePhoto
                    )
                    onDismiss()
                },
                enabled = name.isNotBlank()
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}
