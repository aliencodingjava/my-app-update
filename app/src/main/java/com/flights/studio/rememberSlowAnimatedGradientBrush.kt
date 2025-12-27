package com.flights.studio

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

fun Modifier.animatedProfileGradient(
    c1: Color,
    c2: Color,
    c3: Color,
    durationMillis: Int = 26000,   // ✅ now exists
): Modifier = composed {
    val t by rememberInfiniteTransition(label = "profileGradient").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "t"
    )

    drawWithCache {
        val start = Offset(
            x = size.width * lerp(0.10f, -0.10f, t),
            y = size.height * lerp(0.00f, 0.30f, t)
        )
        val end = Offset(
            x = size.width * lerp(0.90f, 1.10f, t),
            y = size.height * lerp(1.00f, 0.70f, t)
        )

        val brush = Brush.linearGradient(
            colors = listOf(c1, c2, c3),
            start = start,
            end = end
        )

        onDrawBehind { drawRect(brush) }
    }
}

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
