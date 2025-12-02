package com.flights.studio

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.cos

/**
 * Siri-like edge glow:
 * - tight rainbow ring near the card edge
 * - colors rotate around the card
 * - extra splash from bottom, spreading & fading out
 */
@Composable
fun SiriWaveOverlay(
    progress: Float,
    modifier: Modifier = Modifier
) {
    if (progress <= 0f) return

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val t = progress.coerceIn(0f, 1f)

        val minSide = min(w, h)

        // 🔧 closer to edge: very small inset
        val maxInset = minSide * 0.02f

        // 0..1 timeline for TRAVEL only in the first 25% of the animation
        val travelPhase = (t / 0.25f).coerceIn(0f, 1f)
        val travel = FastOutSlowInEasing.transform(travelPhase)

        // 👉 Ring slides to the edge quickly, then stays there
        val inset = lerp(
            start = maxInset,
            stop = 0f,
            fraction = travel
        )

        // 🔥 INTENSITY CURVE (keep your logic)
        val baseIntensity = when {
            t < 0.80f -> {
                val local = t / 0.10f
                FastOutSlowInEasing.transform(local)
            }
            t < 0.90f -> {
                1f
            }
            else -> {
                val local = (t - 0.90f) / 0.60f
                1f - FastOutSlowInEasing.transform(local.coerceIn(0f, 1f))
            }
        }.coerceIn(0f, 1f)

        // 🌊 COLOR WAVE breathing
        val waveBoost = if (t in 0.20f..0.90f) {
            val wavePhase = (t - 0.20f) / 0.70f // 0..1 over "hold"
            val sinValue = sin(wavePhase * PI.toFloat() * 4f)
            0.8f + 0.2f * sinValue
        } else {
            1f
        }

        val edgeAlpha = (0.9f * baseIntensity * waveBoost).coerceIn(0f, 1f)
        if (edgeAlpha <= 0f) return@Canvas

        // Siri-ish colors, boosted a bit
        val blue   = Color(0xFF00E5FF)  // Siri cyan
        val violet = Color(0xFF7C4DFF)  // Siri violet
        val pink   = Color(0xFFFF4081)  // Siri magenta
        val aqua   = Color(0xFF18FFFF)  // electric aqua

        val cornerRadius = 20.dp.toPx()

        // Outer rounded rect = full card
        val outerRound = RoundRect(
            left = 0f,
            top = 0f,
            right = w,
            bottom = h,
            cornerRadius = CornerRadius(cornerRadius, cornerRadius)
        )

        // Inner rounded rect = inset inside, so we get a ring
        val innerInset = inset.coerceAtMost(minSide / 2f - 1f)
        val innerRound = RoundRect(
            left = innerInset,
            top = innerInset,
            right = w - innerInset,
            bottom = h - innerInset,
            cornerRadius = CornerRadius(cornerRadius, cornerRadius)
        )

        // Path difference: outer - inner = edge ring
        val outerPath = Path().apply { addRoundRect(outerRound) }
        val innerPath = Path().apply { addRoundRect(innerRound) }

        val ringPath = Path().apply {
            op(
                outerPath,
                innerPath,
                PathOperation.Difference
            )
        }

        // Center & radius for rotations / splash
        val cx = w / 2f
        val cy = h / 2f
        val radius = minSide / 2f

        // 🔁 angle for rotating gradient (same speed as before)
        val angle = t * PI.toFloat() * 4f

        val start = Offset(
            x = cx + cos(angle) * radius,
            y = cy + sin(angle) * radius
        )
        val end = Offset(
            x = cx - cos(angle) * radius,
            y = cy - sin(angle) * radius
        )

        val brush = Brush.linearGradient(
            colors = listOf(
                blue.copy(alpha = edgeAlpha),
                violet.copy(alpha = edgeAlpha),
                pink.copy(alpha = edgeAlpha),
                aqua.copy(alpha = edgeAlpha)
            ),
            start = start,
            end = end
        )

        // ----------------------------------------------------
        // 🌟 EXTRA SIRI SPLASH FROM BOTTOM (FILL / GLOW)
        // ----------------------------------------------------
        // Grow + fade using same t, but with its own curve
        val splashIntensity = when {
            t < 0.01f -> {              // 🚀 instant snap in
                val local = t / 0.09f
                FastOutSlowInEasing.transform(local)
            }
            t < 0.12f -> {              // very short full power
                1f
            }
            t < 0.22f -> {              // ⚡ fast fade out
                val local = (t - 0.12f) / 0.10f
                1f - FastOutSlowInEasing.transform(local.coerceIn(0f, 1f))
            }
            else -> 0f
        }
        if (splashIntensity > 0f) {
            // max radius big enough to go past the top
            val maxRadius = max(w, h) * 1.3f

            // radius grows from 0 → full only during splash window (0..0.22 of t)
            val radiusPhase = (t.coerceIn(0f, 0.22f) / 0.22f)
            val radiusProgress = FastOutSlowInEasing.transform(radiusPhase)
            val currentRadius = maxRadius * radiusProgress

            // center just below the card, so light rises up
            val splashCenter = Offset(cx, h * 1.05f)

            // main bottom→top splash (same idea as before)
            val splashColor = Color(0xFF00E5FF) // soft Siri cyan
            val splashBrush = Brush.radialGradient(
                colors = listOf(
                    splashColor.copy(alpha = 0.40f * splashIntensity),
                    Color.Transparent
                ),
                center = splashCenter,
                radius = currentRadius
            )

            drawRoundRect(
                brush = splashBrush,
                topLeft = Offset(0f, 0f),
                size = size,
                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                blendMode = BlendMode.Screen
            )

            // 🔝 when the wave is reaching the top, make the TOP area more colorful
            if (radiusProgress > 0.80f) {
                // 0..1 only while the wave finishes the climb
                val topPhase = ((radiusProgress - 0.80f) / 0.20f).coerceIn(0f, 1f)

                // stronger effect if splash itself is strong
                val topAlpha = splashIntensity * topPhase

                // full-color gradient – NO color transparency here
                val topBrush = Brush.verticalGradient(
                    colors = listOf(
                        blue,
                        violet,
                        pink,
                        aqua
                    ),
                    startY = 0f,
                    endY = h * 0.5f
                )

                // cover just the upper portion; feels like the same wave reaching the top/ temporary turned off   val topHeight = h * 0.00f
                val topHeight = h * 0.00f

                drawRoundRect(
                    brush = topBrush,
                    topLeft = Offset(0f, 0f),
                    size = androidx.compose.ui.geometry.Size(w, topHeight),
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                    alpha = topAlpha,          // controls when it's visible, NOT color strength
                    blendMode = BlendMode.Screen
                )
            }
        }



        // Finally, draw the edge ring on top
        drawPath(
            path = ringPath,
            brush = brush,
            blendMode = BlendMode.Screen
        )
    }
}
