package com.flights.studio

data class AllContactus(
    val id: String,
    val name: String,
    val phone: String,
    val email: String?,
    val address: String?,
    val color: Int,
    val latitude: Double?,
    val longitude: Double?,
    var photoUri: String?,
    val iconResId: Int // Add this property
)
