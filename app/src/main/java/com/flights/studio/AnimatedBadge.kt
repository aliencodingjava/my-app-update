package com.flights.studio

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color

@Composable
fun AnimatedBadge(
    visible: Boolean,
    baseColor: Color,
    glowColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable (Color) -> Unit
) {
    // fade in/out
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(180),
        label = "badgeAlpha"
    )

    // pulse (breathing)
    val inf = rememberInfiniteTransition(label = "badgePulse")
    val pulse by inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // mix colors
    val t = (0.25f + 0.55f * pulse) // stronger than subtle
    val animated = Color(
        red = baseColor.red + (glowColor.red - baseColor.red) * t,
        green = baseColor.green + (glowColor.green - baseColor.green) * t,
        blue = baseColor.blue + (glowColor.blue - baseColor.blue) * t,
        alpha = 1f
    )

    if (alpha > 0.01f) {
        Box(
            modifier = modifier.drawBehind {
                val r = size.minDimension / 2f

                // ✅ SMALL tight glow (2 rings max)
                drawCircle(
                    color = glowColor.copy(alpha = 0.50f * alpha * (0.55f + 0.45f * pulse)),
                    radius = r * (1.45f + 0.19f * pulse)
                )
                drawCircle(
                    color = glowColor.copy(alpha = 0.08f * alpha * (0.55f + 0.45f * pulse)),
                    radius = r * (1.80f + 0.20f * pulse)
                )
            }
        ) {
            content(animated.copy(alpha = alpha))
        }

    }
}
