package com.example.packinglist.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.File

/**
 * Zeigt das lokal gespeicherte Foto eines Artikels, ersatzweise die
 * GitHub-URL, oder einen Platzhalter.
 */
@Composable
fun ArticlePhoto(
    photoPath: String?,
    photoUrl: String? = null,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    showPlaceholder: Boolean = false,
    contentDescription: String? = null
) {
    val localFile = photoPath
        ?.let { File(it) }
        ?.takeIf { it.exists() }
    val model: Any? = localFile ?: photoUrl?.takeIf { it.isNotBlank() }

    if (model != null) {
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(12.dp))
        )
    } else if (showPlaceholder) {
        Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Photo,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(size * 0.4f)
            )
        }
    }
}
