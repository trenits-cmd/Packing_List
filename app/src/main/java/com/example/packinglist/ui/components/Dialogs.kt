package com.example.packinglist.ui.components

import android.app.Activity
import android.content.ActivityNotFoundException
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.packinglist.data.Availability
import com.example.packinglist.data.Season
import com.example.packinglist.data.TripDates
import com.example.packinglist.data.TripType
import com.yalantis.ucrop.UCrop
import java.io.File
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripFormDialog(
    title: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        tripType: TripType,
        season: Season,
        startDate: Long,
        endDate: Long
    ) -> Unit,
    initialName: String = "",
    initialTripType: TripType = TripType.PRIVATE,
    initialSeason: Season = Season.SUMMER,
    initialStartDate: Long = TripDates.todayUtcMillis(),
    initialEndDate: Long = TripDates.todayUtcMillis()
) {
    var name by remember { mutableStateOf(initialName) }
    var tripType by remember { mutableStateOf(initialTripType) }
    var season by remember { mutableStateOf(initialSeason) }
    var startDate by remember { mutableStateOf(initialStartDate) }
    var endDate by remember { mutableStateOf(initialEndDate) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    val days = TripDates.daysInclusive(startDate, endDate)

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

                Text(
                    text = "Reisezeitraum",
                    style = MaterialTheme.typography.labelLarge
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DateField(
                        label = "Start",
                        millis = startDate,
                        onClick = { showStartPicker = true },
                        modifier = Modifier.weight(1f)
                    )
                    DateField(
                        label = "Ende",
                        millis = endDate,
                        onClick = { showEndPicker = true },
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = "Zeitraum: ${TripDates.daysLabel(days)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(name.trim(), tripType, season, startDate, endDate)
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

    if (showStartPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = startDate)
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selected = state.selectedDateMillis ?: startDate
                        startDate = selected
                        if (endDate < startDate) endDate = startDate
                        showStartPicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) {
                    Text("Abbrechen")
                }
            }
        ) {
            DatePicker(state = state)
        }
    }

    if (showEndPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = endDate)
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selected = state.selectedDateMillis ?: endDate
                        endDate = selected.coerceAtLeast(startDate)
                        showEndPicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndPicker = false }) {
                    Text("Abbrechen")
                }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
private fun DateField(
    label: String,
    millis: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Rounded.CalendarMonth,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.size(6.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = TripDates.format(millis),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
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
        isClothing: Boolean,
        photoUri: android.net.Uri?,
        removePhoto: Boolean
    ) -> Unit,
    initialName: String = "",
    initialTripType: TripType = TripType.PRIVATE,
    initialAvailability: Availability = Availability.ALWAYS,
    initialImportance: Int = 1,
    initialIsClothing: Boolean = false,
    existingPhotoPath: String? = null,
    existingPhotoUrl: String? = null
) {
    val context = LocalContext.current

    var name by remember { mutableStateOf(initialName) }
    var tripType by remember { mutableStateOf(initialTripType) }
    var availability by remember { mutableStateOf(initialAvailability) }
    var importance by remember { mutableStateOf(initialImportance) }
    var isClothing by remember { mutableStateOf(initialIsClothing) }
    var pickedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var removePhoto by remember { mutableStateOf(false) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    val cropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val cropped = result.data?.let { UCrop.getOutput(it) }
            if (cropped != null) {
                pickedPhotoUri = cropped
                removePhoto = false
            } else {
                val error = result.data?.let { UCrop.getError(it) }
                Toast.makeText(
                    context,
                    error?.message ?: "Foto konnte nicht zugeschnitten werden.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun launchCrop(source: Uri) {
        val destination = File(context.cacheDir, "cropped_${UUID.randomUUID()}.jpg")
        val intent = UCrop.of(source, Uri.fromFile(destination))
            .withMaxResultSize(1600, 1600)
            .getIntent(context)
        cropLauncher.launch(intent)
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) launchCrop(uri)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val source = cameraUri
        cameraUri = null
        if (success && source != null) {
            launchCrop(source)
        }
    }

    fun takePhoto() {
        val directory = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File(directory, "capture_${UUID.randomUUID()}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        cameraUri = uri
        try {
            cameraLauncher.launch(uri)
        } catch (exception: ActivityNotFoundException) {
            Toast.makeText(context, "Keine Kamera-App gefunden.", Toast.LENGTH_SHORT).show()
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Kleidung",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = "Bekommt in der Packliste einen Stückzahl-Zähler " +
                                "(Ziel: Reisedauer + 1).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isClothing,
                        onCheckedChange = { isClothing = it }
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Foto",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Box(
                        modifier = Modifier
                            .size(110.dp)
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { takePhoto() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PhotoCamera,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.size(6.dp))
                            Text("Kamera")
                        }
                        OutlinedButton(
                            onClick = {
                                galleryLauncher.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PhotoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.size(6.dp))
                            Text("Galerie")
                        }
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

                    Text(
                        text = "Nach Kamera oder Galerie öffnet sich der Zuschnitt. " +
                            "Beim Speichern wird das Foto automatisch nach GitHub " +
                            "hochgeladen, wenn die Verbindung eingerichtet ist.",
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
                        isClothing,
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
