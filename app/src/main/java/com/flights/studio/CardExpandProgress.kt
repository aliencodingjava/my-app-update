package com.flights.studio

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember

/**
 * 0f..1f timeline for the Siri edge effect:
 * - 0 → 1 over ~3.2s
 */
@Composable
fun siriCardEdgeGlowOverlay(
    expanded: Boolean,
    durationMillis: Int = 500 // ⬅ try 1000–1400 range
): Float {
    val anim = remember { Animatable(0f) }

    LaunchedEffect(expanded) {
        anim.stop()

        if (expanded) {
            anim.snapTo(0f)
            anim.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = durationMillis,
                    easing = LinearEasing
                )
            )
            // optional reset, so you can retrigger again:
            anim.snapTo(0f)
        } else {
            anim.snapTo(0f)
        }
    }

    return anim.value
}
