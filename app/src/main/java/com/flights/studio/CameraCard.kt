package com.flights.studio

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

enum class CardType {
    IMAGE,
}

@Parcelize
data class CameraCard(
    val title: String,
    val url: String,
    val type: CardType = CardType.IMAGE
) : Parcelable