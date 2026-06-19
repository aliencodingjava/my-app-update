package com.flights.studio

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserNoteImage(
    @SerialName("user_id") val userId: String,
    @SerialName("note_id") val noteId: String,
    val path: String,
    @SerialName("mime_type") val mimeType: String? = null,
    val width: Int? = null,
    val height: Int? = null
)
