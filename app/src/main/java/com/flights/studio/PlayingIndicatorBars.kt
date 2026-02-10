package com.flights.studio
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * bands: 3 values in [0..1], one per column (left, mid, right)
 */
@Composable
fun Playing15Dots(
    bands: List<Float>,
    color: Color,
    modifier: Modifier = Modifier,
    dot: Dp = 2.5.dp,     // ✅ smaller
    gap: Dp = 1.5.dp,     // ✅ tighter
    rows: Int = 5,
) {
    val safe = remember(bands) {
        listOf(
            bands.getOrNull(0) ?: 0f,
            bands.getOrNull(1) ?: 0f,
            bands.getOrNull(2) ?: 0f
        )
    }

    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(gap)
    ) {
        for (c in 0 until 3) {
            val target = safe[c].coerceIn(0f, 1f)

            // Smooth like Samsung (no jitter)
            val level by animateFloatAsState(
                targetValue = target,
                animationSpec = spring(
                    dampingRatio = 0.85f,
                    stiffness = 450f
                ),
                label = "band$c"
            )

            val onCount = (level * rows).roundToInt().coerceIn(0, rows)

            Column(
                verticalArrangement = Arrangement.spacedBy(gap),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                for (r in 0 until rows) {
                    // bottom-up filling
                    val isOn = r >= (rows - onCount)
                    Box(
                        Modifier
                            .size(dot)
                            .background(
                                if (isOn) color else Color.Transparent // ✅ OFF = invisible
                            )
                    )
                }
            }
        }
    }
}
