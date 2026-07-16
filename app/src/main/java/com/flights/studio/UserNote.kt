package com.flights.studio

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserNote(
    val id: String? = null,
    @SerialName("user_id") val userId: String,
    val content: String,
    val title: String? = null,
    @SerialName("has_reminder") val hasReminder: Boolean = false,
    @SerialName("has_reminder_badge") val hasReminderBadge: Boolean = false,
    @SerialName("folder_id") val folderId: String = NoteFolderStore.MAIN_FOLDER_ID,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null
) {
    fun updatedAtMillis(): Long =
        updatedAt?.let { java.time.Instant.parse(it).toEpochMilli() } ?: 0L
}
