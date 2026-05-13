package com.flights.studio

data class UpdateBlock(
    val title: String,
    val summary: String = "",
    val bullets: List<String> = emptyList()
)

