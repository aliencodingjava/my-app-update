package com.flights.studio

import java.io.Serializable

data class AllContact(
    val id: String,
    val name: String,
    val phone: String,
    val email: String?,
    val address: String?,
    val color: Int,
    var photoUri: String?,
    val birthday: String?,
    val flag: String? = null,
    val regionCode: String? = null
) : Serializable
