package com.example.packinglist.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

data class GitHubSettings(
    val owner: String = DEFAULT_OWNER,
    val repo: String = DEFAULT_REPO,
    val branch: String = "main",
    val directory: String = "article_photos",
    val token: String = ""
) {
    val isConfigured: Boolean
        get() = owner.isNotBlank() &&
            repo.isNotBlank() &&
            branch.isNotBlank() &&
            token.isNotBlank()

    companion object {
        const val DEFAULT_OWNER = "trenits-cmd"
        const val DEFAULT_REPO = "Packing_List"
    }
}

/**
 * Speichert die GitHub-Zugangsdaten verschlüsselt auf dem Gerät.
 */
class GitHubSettingsStore(context: Context) {

    private val prefs: SharedPreferences = createPrefs(context)

    fun load(): GitHubSettings = runCatching {
        GitHubSettings(
            owner = prefs.getString(KEY_OWNER, GitHubSettings.DEFAULT_OWNER).orEmpty(),
            repo = prefs.getString(KEY_REPO, GitHubSettings.DEFAULT_REPO).orEmpty(),
            branch = prefs.getString(KEY_BRANCH, "main").orEmpty(),
            directory = prefs.getString(KEY_DIRECTORY, "article_photos").orEmpty(),
            token = prefs.getString(KEY_TOKEN, "").orEmpty()
        )
    }.getOrElse { GitHubSettings() }

    fun save(settings: GitHubSettings) {
        prefs.edit()
            .putString(KEY_OWNER, settings.owner.trim())
            .putString(KEY_REPO, settings.repo.trim())
            .putString(KEY_BRANCH, settings.branch.trim())
            .putString(KEY_DIRECTORY, settings.directory.trim())
            .putString(KEY_TOKEN, settings.token.trim())
            .apply()
    }

    private fun createPrefs(context: Context): SharedPreferences = runCatching {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }.getOrElse {
        context.getSharedPreferences("${FILE_NAME}_fallback", Context.MODE_PRIVATE)
    }

    private companion object {
        const val FILE_NAME = "github_settings"
        const val KEY_OWNER = "owner"
        const val KEY_REPO = "repo"
        const val KEY_BRANCH = "branch"
        const val KEY_DIRECTORY = "directory"
        const val KEY_TOKEN = "token"
    }
}
