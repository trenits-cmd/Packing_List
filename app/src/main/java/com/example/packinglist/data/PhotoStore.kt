package com.example.packinglist.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

/**
 * Kopiert ausgewählte Fotos in den privaten App-Speicher. So bleiben sie
 * verfügbar, auch wenn der ursprüngliche Medien-Uri nicht mehr gültig ist.
 */
object PhotoStore {

    fun save(context: Context, uri: Uri): String? = runCatching {
        val extension = when (context.contentResolver.getType(uri)) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            "image/heic", "image/heif" -> "heic"
            "image/bmp" -> "bmp"
            else -> "jpg"
        }
        val directory = File(context.filesDir, "article_photos").apply { mkdirs() }
        val file = File(directory, "article_${UUID.randomUUID()}.$extension")
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        file.absolutePath
    }.getOrNull()

    fun delete(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).delete() }
    }
}
