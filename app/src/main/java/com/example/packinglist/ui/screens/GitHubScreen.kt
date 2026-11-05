package com.example.packinglist.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.packinglist.data.GitHubSettings
import com.example.packinglist.ui.components.StatusBadge
import com.example.packinglist.ui.theme.LocalStatusColors

@Composable
fun GitHubScreen(
    settings: GitHubSettings,
    onSave: (GitHubSettings) -> Unit,
    onTest: (GitHubSettings) -> Unit,
    onUploadLocalPhotos: () -> Unit
) {
    val context = LocalContext.current
    val statusColors = LocalStatusColors.current

    var owner by remember(settings) { mutableStateOf(settings.owner) }
    var repo by remember(settings) { mutableStateOf(settings.repo) }
    var branch by remember(settings) { mutableStateOf(settings.branch) }
    var directory by remember(settings) { mutableStateOf(settings.directory) }
    var token by remember(settings) { mutableStateOf(settings.token) }

    fun currentSettings(): GitHubSettings = GitHubSettings(
        owner = owner,
        repo = repo,
        branch = branch,
        directory = directory,
        token = token
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "GitHub-Speicher",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Fotos von Artikeln werden zusätzlich in deinem GitHub-Repository " +
                    "gespeichert.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Repository ist öffentlich",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "Hochgeladene Fotos sind in einem öffentlichen Repository " +
                            "für alle sichtbar. Lade keine privaten oder sensiblen Bilder hoch.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudUpload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = "Zugangsdaten",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    StatusBadge(
                        text = if (settings.isConfigured) "Verbunden"
                        else "Nicht eingerichtet",
                        containerColor = if (settings.isConfigured) {
                            statusColors.packedContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        contentColor = if (settings.isConfigured) {
                            statusColors.packed
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = owner,
                        onValueChange = { owner = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Owner") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = repo,
                        onValueChange = { repo = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Repository") },
                        singleLine = true
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = branch,
                        onValueChange = { branch = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Branch") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = directory,
                        onValueChange = { directory = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Ordner") },
                        singleLine = true,
                        placeholder = { Text("article_photos") }
                    )
                }

                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("GitHub-Token") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Key,
                            contentDescription = null
                        )
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true
                )

                Text(
                    text = "Fine-grained Token mit „Contents: Read and write“ für genau " +
                        "dieses Repository. Das Token wird verschlüsselt auf diesem Gerät " +
                        "gespeichert.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                TextButton(
                    onClick = {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://github.com/settings/tokens?type=beta")
                            )
                        )
                    }
                ) {
                    Text("GitHub-Token erstellen")
                }

                Button(
                    onClick = { onSave(currentSettings()) },
                    enabled = owner.isNotBlank() &&
                        repo.isNotBlank() &&
                        branch.isNotBlank() &&
                        token.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Einstellungen speichern")
                }

                OutlinedButton(
                    onClick = { onTest(currentSettings()) },
                    enabled = owner.isNotBlank() &&
                        repo.isNotBlank() &&
                        token.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Verbindung testen")
                }

                if (settings.isConfigured) {
                    TextButton(
                        onClick = { onSave(settings.copy(token = "")) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "GitHub-Verbindung trennen",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "So funktioniert der Upload",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Text(
                    text = "Beim Speichern eines Artikelfotos lädt die App das Bild in " +
                        "${settings.owner.ifBlank { "owner" }}/" +
                        "${settings.repo.ifBlank { "repo" }}/" +
                        "${settings.directory.ifBlank { "article_photos" }} hoch. " +
                        "Bereits vorhandene lokale Fotos kannst du unten nachträglich " +
                        "hochladen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                OutlinedButton(
                    onClick = onUploadLocalPhotos,
                    enabled = settings.isConfigured,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Lokale Fotos jetzt hochladen")
                }
            }
        }
    }
}
