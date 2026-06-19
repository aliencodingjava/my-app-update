package com.flights.studio

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserNoteAttachment(
    @SerialName("user_id") val userId: String,
    @SerialName("note_id") val noteId: String,
    @SerialName("storage_path") val storagePath: String,
    @SerialName("file_name") val fileName: String,
    @SerialName("mime_type") val mimeType: String? = null,
    @SerialName("size_bytes") val sizeBytes: Long = 0L,
    @SerialName("kind") val kind: String,
    @SerialName("duration_ms") val durationMs: Long? = null,
    @SerialName("created_at_ms") val createdAtMs: Long? = null
)
