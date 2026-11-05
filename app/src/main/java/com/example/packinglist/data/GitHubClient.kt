package com.example.packinglist.data

import android.util.Base64
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

data class RemotePhoto(
    val downloadUrl: String?,
    val remotePath: String
)

class GitHubException(
    val statusCode: Int,
    override val message: String
) : Exception(message)

/**
 * Minimaler GitHub-Clients für den Contents-API-Upload von Artikelfotos.
 */
class GitHubClient(
    private val client: OkHttpClient = defaultClient()
) {

    fun testConnection(settings: GitHubSettings): Result<String> = runCatching {
        val url = "https://api.github.com".toHttpUrl().newBuilder()
            .addPathSegment("repos")
            .addPathSegment(settings.owner.trim())
            .addPathSegment(settings.repo.trim())
            .build()

        val request = Request.Builder()
            .url(url)
            .withGitHubHeaders(settings.token)
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw GitHubException(response.code, errorMessage(response.code, body))
            }
            val json = JSONObject(body)
            val fullName = json.optString(
                "full_name",
                "${settings.owner}/${settings.repo}"
            )
            "Verbindung zu $fullName erfolgreich."
        }
    }

    fun uploadPhoto(
        settings: GitHubSettings,
        file: File,
        articleName: String
    ): Result<RemotePhoto> = runCatching {
        val extension = file.extension.ifBlank { "jpg" }
        val fileName = "article_${System.currentTimeMillis()}_" +
            "${UUID.randomUUID().toString().take(8)}.$extension"
        val directory = settings.directory.trim().trim('/')
        val remotePath = if (directory.isEmpty()) fileName else "$directory/$fileName"

        val payload = JSONObject().apply {
            put("message", "Foto für Artikel: $articleName")
            put("content", Base64.encodeToString(file.readBytes(), Base64.NO_WRAP))
            put("branch", settings.branch.trim())
        }.toString()

        val request = Request.Builder()
            .url(contentsUrl(settings, remotePath))
            .withGitHubHeaders(settings.token)
            .put(payload.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw GitHubException(response.code, errorMessage(response.code, body))
            }
            val json = JSONObject(body)
            val content = json.optJSONObject("content")
            RemotePhoto(
                downloadUrl = content?.optString("download_url")
                    ?.takeIf { it.isNotBlank() && it != "null" },
                remotePath = content?.optString("path")
                    ?.takeIf { it.isNotBlank() } ?: remotePath
            )
        }
    }

    fun deletePhoto(
        settings: GitHubSettings,
        remotePath: String
    ): Result<Unit> = runCatching {
        val fileUrl = contentsUrl(settings, remotePath).newBuilder()
            .addQueryParameter("ref", settings.branch.trim())
            .build()

        val getRequest = Request.Builder()
            .url(fileUrl)
            .withGitHubHeaders(settings.token)
            .get()
            .build()

        val sha = client.newCall(getRequest).execute().use { response ->
            if (response.code == 404) return@runCatching
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw GitHubException(response.code, errorMessage(response.code, body))
            }
            JSONObject(body).optString("sha")
        }

        if (sha.isBlank()) {
            throw IllegalStateException("GitHub-Datei ohne SHA")
        }

        val deletePayload = JSONObject().apply {
            put("message", "Foto entfernt")
            put("sha", sha)
            put("branch", settings.branch.trim())
        }.toString()

        val deleteRequest = Request.Builder()
            .url(contentsUrl(settings, remotePath))
            .withGitHubHeaders(settings.token)
            .delete(deletePayload.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(deleteRequest).execute().use { response ->
            if (!response.isSuccessful && response.code != 404) {
                throw GitHubException(
                    response.code,
                    errorMessage(response.code, response.body?.string().orEmpty())
                )
            }
        }
        Unit
    }

    private fun contentsUrl(settings: GitHubSettings, remotePath: String): HttpUrl =
        "https://api.github.com".toHttpUrl().newBuilder()
            .addPathSegment("repos")
            .addPathSegment(settings.owner.trim())
            .addPathSegment(settings.repo.trim())
            .addPathSegment("contents")
            .addPathSegments(remotePath)
            .build()

    private fun Request.Builder.withGitHubHeaders(token: String): Request.Builder = apply {
        header("Authorization", "Bearer ${token.trim()}")
        header("Accept", "application/vnd.github+json")
        header("X-GitHub-Api-Version", "2022-11-28")
        header("User-Agent", "PackingList-Android")
    }

    private fun errorMessage(statusCode: Int, body: String): String {
        val apiMessage = runCatching {
            JSONObject(body).optString("message")
        }.getOrNull().orEmpty()

        val hint = when (statusCode) {
            401 -> "Token ungültig."
            403 -> "Kein Schreibrecht. Das Token braucht Contents: Read and write."
            404 -> "Repository oder Pfad nicht gefunden."
            409, 422 -> "GitHub konnte die Datei nicht anlegen."
            else -> ""
        }
        val detail = apiMessage.ifBlank { body.take(180) }
        return listOf("GitHub $statusCode", detail, hint)
            .filter { it.isNotBlank() }
            .joinToString(": ")
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json".toMediaType()

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }
}
