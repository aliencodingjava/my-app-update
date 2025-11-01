package com.flights.studio

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Global unified style for all "liquid glass" buttons, bars, and cards.
 * Change one value here and all buttons (top bar, grid, bottom bar) update automatically.
 */
object SoftGlassTheme {
    val tint = Color(0xFF4DA4FF).copy(alpha = 0.25f)
    val surface = Color.White.copy(alpha = 0.08f)
    val icon = Color.White.copy(alpha = 0.92f)

    // make the blur actually visible:
    val blurRadius = 8.dp

    // stronger lens
    val lensInner = 20.dp
    val lensOuter = 40.dp
}
