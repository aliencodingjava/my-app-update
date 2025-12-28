package com.flights.studio

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max

@Composable
fun Modifier.staticProfileBackdrop(): Modifier {
    // ✅ Read composable values HERE
    val colors = MaterialTheme.colorScheme
    val primary = colors.primary
    val secondary = colors.secondary
    val tertiary = colors.tertiary
    val surface = colors.surface
    val isDark = primary.luminance() < 0.5f

    return this.drawWithCache {

        val a1 = if (isDark) 0.22f else 0.14f
        val a2 = if (isDark) 0.16f else 0.10f
        val a3 = if (isDark) 0.12f else 0.08f

        val base = Brush.linearGradient(
            colors = listOf(
                primary.copy(alpha = a1),
                secondary.copy(alpha = a2),
                tertiary.copy(alpha = a3)
            ),
            start = Offset.Zero,
            end = Offset(size.width, size.height)
        )

        val spotlight = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = if (isDark) 0.10f else 0.16f),
                Color.Transparent
            ),
            center = Offset(size.width * 0.20f, size.height * 0.25f),
            radius = max(size.width, size.height) * 0.55f
        )

        val vignette = Brush.radialGradient(
            colors = listOf(
                Color.Transparent,
                (if (isDark) Color.Black else surface)
                    .copy(alpha = if (isDark) 0.22f else 0.14f)
            ),
            center = Offset(size.width * 0.55f, size.height * 0.40f),
            radius = max(size.width, size.height) * 0.90f
        )

        onDrawBehind {
            drawRect(base)
            drawRect(spotlight)
            drawRect(vignette)
        }
    }
}
