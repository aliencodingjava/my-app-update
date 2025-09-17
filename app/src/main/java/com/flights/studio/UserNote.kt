package com.flights.studio

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserNote(
    val id: String? = null,
    @SerialName("user_id") val userId: String,
    val content: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null // 👈 add this
) {
    fun updatedAtMillis(): Long =
        updatedAt?.let { java.time.Instant.parse(it).toEpochMilli() } ?: 0L
}
