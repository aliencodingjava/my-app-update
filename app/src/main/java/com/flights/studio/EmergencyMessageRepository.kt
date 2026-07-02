package com.flights.studio

import com.google.android.gms.tasks.Tasks
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

data class EmergencyMessage(
    val enabled: Boolean,
    val title: String,
    val body: String,
    val severity: String,
    val actionLabel: String,
    val actionUrl: String,
    val revision: Long,
    val mode: String,
    val pages: String,
    val startsAt: String,
    val endsAt: String
) {
    val key: String = listOf(title, body, severity, revision, mode, pages, startsAt, endsAt).joinToString("|")

    fun shouldShowOnPage(page: String, nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (!enabled || (title.isBlank() && body.isBlank())) return false
        if (!isWithinWindow(nowMillis)) return false

        val requestedPages = pages
            .split(",", ";", "|")
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }

        return requestedPages.isEmpty() ||
            requestedPages.any { it == "all" || it == "*" || it == page.lowercase() }
    }

    fun canDismiss(): Boolean = mode.lowercase() != "always"

    private fun isWithinWindow(nowMillis: Long): Boolean {
        val starts = startsAt.toEpochMillisOrNull()
        val ends = endsAt.toEpochMillisOrNull()
        if (starts != null && nowMillis < starts) return false
        if (ends != null && nowMillis > ends) return false
        return true
    }

    companion object {
        val Disabled = EmergencyMessage(
            enabled = false,
            title = "",
            body = "",
            severity = "info",
            actionLabel = "",
            actionUrl = "",
            revision = 0L,
            mode = "once",
            pages = "all",
            startsAt = "",
            endsAt = ""
        )
    }
}

object EmergencyMessageRepository {
    private const val KEY_ENABLED = "emergency_enabled"
    private const val KEY_TITLE = "emergency_title"
    private const val KEY_BODY = "emergency_body"
    private const val KEY_SEVERITY = "emergency_severity"
    private const val KEY_ACTION_LABEL = "emergency_action_label"
    private const val KEY_ACTION_URL = "emergency_action_url"
    private const val KEY_REVISION = "emergency_revision"
    private const val KEY_MODE = "emergency_mode"
    private const val KEY_PAGES = "emergency_pages"
    private const val KEY_STARTS_AT = "emergency_starts_at"
    private const val KEY_ENDS_AT = "emergency_ends_at"

    suspend fun fetch(): EmergencyMessage = withContext(Dispatchers.IO) {
        runCatching {
            val remoteConfig = FirebaseRemoteConfig.getInstance()
            val minimumFetchInterval = if (BuildConfig.DEBUG) 60L else 900L
            val settings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(minimumFetchInterval)
                .build()

            Tasks.await(remoteConfig.setConfigSettingsAsync(settings))
            Tasks.await(
                remoteConfig.setDefaultsAsync(
                    mapOf(
                        KEY_ENABLED to false,
                        KEY_TITLE to "",
                        KEY_BODY to "",
                        KEY_SEVERITY to "info",
                        KEY_ACTION_LABEL to "",
                        KEY_ACTION_URL to "",
                        KEY_REVISION to 0L,
                        KEY_MODE to "once",
                        KEY_PAGES to "all",
                        KEY_STARTS_AT to "",
                        KEY_ENDS_AT to ""
                    )
                )
            )
            Tasks.await(remoteConfig.fetchAndActivate())

            EmergencyMessage(
                enabled = remoteConfig.getBoolean(KEY_ENABLED),
                title = remoteConfig.getString(KEY_TITLE).trim(),
                body = remoteConfig.getString(KEY_BODY).trim(),
                severity = remoteConfig.getString(KEY_SEVERITY).trim().ifBlank { "info" },
                actionLabel = remoteConfig.getString(KEY_ACTION_LABEL).trim(),
                actionUrl = remoteConfig.getString(KEY_ACTION_URL).trim(),
                revision = remoteConfig.getLong(KEY_REVISION),
                mode = remoteConfig.getString(KEY_MODE).trim().ifBlank { "once" },
                pages = remoteConfig.getString(KEY_PAGES).trim().ifBlank { "all" },
                startsAt = remoteConfig.getString(KEY_STARTS_AT).trim(),
                endsAt = remoteConfig.getString(KEY_ENDS_AT).trim()
            ).takeIf { it.enabled && (it.title.isNotBlank() || it.body.isNotBlank()) }
                ?: EmergencyMessage.Disabled
        }.getOrDefault(EmergencyMessage.Disabled)
    }
}

private fun String.toEpochMillisOrNull(): Long? {
    val value = trim()
    if (value.isBlank()) return null
    return runCatching { Instant.parse(value).toEpochMilli() }.getOrNull()
}
