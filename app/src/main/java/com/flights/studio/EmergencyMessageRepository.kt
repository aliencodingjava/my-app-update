package com.flights.studio

import com.google.android.gms.tasks.Tasks
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class EmergencyMessage(
    val enabled: Boolean,
    val title: String,
    val body: String,
    val severity: String,
    val actionLabel: String,
    val actionUrl: String,
    val revision: Long
) {
    val key: String = listOf(title, body, severity, revision).joinToString("|")

    companion object {
        val Disabled = EmergencyMessage(
            enabled = false,
            title = "",
            body = "",
            severity = "info",
            actionLabel = "",
            actionUrl = "",
            revision = 0L
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
                        KEY_REVISION to 0L
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
                revision = remoteConfig.getLong(KEY_REVISION)
            ).takeIf { it.enabled && (it.title.isNotBlank() || it.body.isNotBlank()) }
                ?: EmergencyMessage.Disabled
        }.getOrDefault(EmergencyMessage.Disabled)
    }
}
