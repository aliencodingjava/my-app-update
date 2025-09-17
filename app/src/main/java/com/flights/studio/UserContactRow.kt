package com.flights.studio

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserContactRow(
    val id: String? = null,
    @SerialName("user_id")      val userId: String,
    val name: String,
    val phone: String,
    val email: String? = null,
    val address: String? = null,
    val birthday: String? = null,   // "YYYY-MM-DD"
    @SerialName("photo_uri")     val photoUri: String? = null,
    @SerialName("main_color")    val mainColor: String,
    @SerialName("overlay_color") val overlayColor: String,
    @SerialName("button_color")  val buttonColor: String
)
