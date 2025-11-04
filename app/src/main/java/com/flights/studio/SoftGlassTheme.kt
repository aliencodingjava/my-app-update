package com.flights.studio

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Global unified style for all "liquid glass" buttons, bars, and cards.
 * Change one value here and all buttons (top bar, grid, bottom bar) update automatically.
 */
object SoftGlassTheme {
    // Deep royal glass hue (indigo family)
    val tint = Color(0xFF00BFA5).copy(alpha = 0.25f)

    // icon/text color on glass
    val icon = Color.White.copy(alpha = 0.92f)

    // gentle blur
    val blurRadius = 2.dp

    // refraction halo strength
    val lensInner = 16.dp
    val lensOuter = 32.dp
}