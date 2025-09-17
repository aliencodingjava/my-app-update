package com.flights.studio

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Note(
    val id: Int? = null,
    val content: String,
    @SerialName("created_at")
    val createdAt: String? = null

)