package com.flights.studio

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.LayoutDirection

/**
 * Shimmer mask for text (or any content if you use SrcIn / SrcAtop correctly).
 * - No visible rectangle (Offscreen + SrcIn)
 * - Brush cached per size
 * - Band is narrower/softer (more premium)
 */
fun Modifier.textShimmer(
    baseColor: Color,
    highlightColor: Color,
    durationMillis: Int = 1100,
): Modifier = composed {

    val layoutDirection = LocalLayoutDirection.current
    val directionMultiplier = if (layoutDirection == LayoutDirection.Ltr) 1f else -1f

    val transition = rememberInfiniteTransition(label = "shimmer")
    val t by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerT"
    )

    this
        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        .drawWithCache {
            val w = size.width.coerceAtLeast(1f)

            val brush = Brush.linearGradient(
                colorStops = arrayOf(
                    0.00f to baseColor,
                    0.40f to baseColor,
                    0.50f to highlightColor,
                    0.60f to baseColor,
                    1.00f to baseColor,
                ),
                start = if (layoutDirection == LayoutDirection.Ltr)
                    Offset.Zero
                else
                    Offset(w, 0f),

                end = if (layoutDirection == LayoutDirection.Ltr)
                    Offset(w, 0f)
                else
                    Offset.Zero

            )

            onDrawWithContent {
                drawContent()

                val xOffset = w * t * directionMultiplier

                translate(left = xOffset) {
                    drawRect(
                        brush = brush,
                        size = size,
                        blendMode = BlendMode.SrcIn
                    )
                }
            }
        }
}


@Composable
fun ShimmerThinkingTextCompat(
    modifier: Modifier = Modifier,
    text: String = "Thinking…",
    style: TextStyle = MaterialTheme.typography.labelMedium,
    baseColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
    // ✅ better default highlight: always “light”
    highlightColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f),
) {
    Text(
        text = text,
        style = style,
        maxLines = 1,
        color = baseColor,
        modifier = modifier.textShimmer(
            baseColor = baseColor,
            highlightColor = highlightColor
        )
    )
}
