package com.flights.studio

import java.io.Serializable

data class AllContact(
    val id: String,
    val name: String,
    val phone: String,
    val email: String?,
    val address: String?,
    val color: Int,
    var photoUri: String?
) : Serializable

