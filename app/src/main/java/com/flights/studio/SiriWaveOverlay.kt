package com.flights.studio

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun SiriWaveOverlay(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val t = progress.coerceIn(0f, 1f)
    if (t <= 0f) return

    // ✅ reuse paths to avoid GC during animation
    val outerPath = remember { Path() }
    val innerPath = remember { Path() }
    val ringPath = remember { Path() }

    // ✅ vivid palette (keep)
    val blue   = Color(0xFF00E5FF)
    val violet = Color(0xFF7C4DFF)
    val pink   = Color(0xFFFF2D8D)
    val aqua   = Color(0xFF00FFD5)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val minSide = min(w, h)
        val cx = w * 0.5f
        val cy = h * 0.5f
        val radius = minSide * 0.5f

        // -------------------------
        // 1) Intensity curve (smooth + premium)
        // -------------------------
        val intensity = when {
            t < 0.12f -> FastOutSlowInEasing.transform((t / 0.12f).coerceIn(0f, 1f))
            t < 0.90f -> 1f
            else -> 1f - FastOutSlowInEasing.transform(((t - 0.90f) / 0.10f).coerceIn(0f, 1f))
        }.coerceIn(0f, 1f)
        if (intensity <= 0f) return@Canvas

        // subtle breathe (slower, less “cheap”)
        val breathe = 0.97f + 0.03f * sin(t * PI.toFloat() * 1.6f)

        // overall alpha (controls “neon”)
        val edgeAlpha = (0.52f * intensity * breathe).coerceIn(0f, 1f)

        // -------------------------
        // 2) Thickness (clamped = consistent on all screens)
        // -------------------------
        val baseThickness = (minSide * 0.012f)
            .coerceIn(4.dp.toPx(), 12.dp.toPx())

        val bloomThickness = baseThickness * 1.6f
        val specThickness = (baseThickness * 0.55f)
            .coerceIn(2.dp.toPx(), 7.dp.toPx())

        // -------------------------
        // 3) Ring path builder (no allocations)
        // -------------------------
        val cornerRadius = 12.dp.toPx()

        fun buildRing(th: Float) {
            val outer = RoundRect(
                left = 0f,
                top = 0f,
                right = w,
                bottom = h,
                cornerRadius = CornerRadius(cornerRadius, cornerRadius)
            )
            val inner = RoundRect(
                left = th,
                top = th,
                right = w - th,
                bottom = h - th,
                cornerRadius = CornerRadius(cornerRadius, cornerRadius)
            )

            outerPath.reset()
            innerPath.reset()
            ringPath.reset()

            outerPath.addRoundRect(outer)
            innerPath.addRoundRect(inner)
            ringPath.op(outerPath, innerPath, PathOperation.Difference)
        }

        // -------------------------
        // 4) Rotation (slower)
        // -------------------------
        val angle = t * PI.toFloat() * 1.25f

        val start = Offset(
            x = cx + cos(angle) * radius,
            y = cy + sin(angle) * radius
        )
        val end = Offset(
            x = cx - cos(angle) * radius,
            y = cy - sin(angle) * radius
        )

        // ✅ avoid list allocations by building array once is tricky in Compose,
        // but listOf here is ok; still, we keep it minimal.
        val ringBrush = Brush.linearGradient(
            colors = listOf(
                blue.copy(alpha = edgeAlpha),
                violet.copy(alpha = edgeAlpha),
                pink.copy(alpha = edgeAlpha),
                aqua.copy(alpha = edgeAlpha),
                blue.copy(alpha = edgeAlpha)
            ),
            start = start,
            end = end
        )

        // -------------------------
        // 5) Draw passes (bloom + main + spec)
        // -------------------------
        val blend = BlendMode.Screen

        // PASS A: bloom (very soft, fades nicely)
        buildRing(bloomThickness)
        drawPath(
            path = ringPath,
            brush = ringBrush,
            alpha = 0.18f,
            blendMode = blend
        )

        // PASS B: main ring
        buildRing(baseThickness)
        drawPath(
            path = ringPath,
            brush = ringBrush,
            alpha = 0.78f,
            blendMode = blend
        )

        // PASS C: specular hot line (makes it look “HD”)
        // This is subtle white-ish reinforcement, not more rainbow.
        val hot = (0.10f * intensity * (0.92f + 0.08f * sin(t * PI.toFloat() * 3.0f))).coerceIn(0f, 0.18f)
        buildRing(specThickness)
        drawPath(
            path = ringPath,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = hot),
                    Color.Transparent
                ),
                start = Offset(cx, 0f),
                end = Offset(cx, h)
            ),
            alpha = 1f,
            blendMode = blend
        )
    }
}
