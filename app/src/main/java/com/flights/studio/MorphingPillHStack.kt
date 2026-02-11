package com.flights.studio

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import kotlin.math.roundToInt

@Composable
fun PillMorphLayout(
    modifier: Modifier = Modifier,
    slotWidth: Dp,
    progress: Float,              // 0..1
    collapsedOverlap: Dp,         // negative = overlap near menu
    splitGap: Dp = Dp.Unspecified, // extra gap between menu & exit (only when expanded)
    splitProgress: Float = 0f,     // 0..1 pulse amount for splitGap
    content: @Composable () -> Unit
) {
    val p = progress.coerceIn(0f, 1f)
    val splitP = splitProgress.coerceIn(0f, 1f)
    val density = LocalDensity.current

    Layout(
        modifier = modifier,
        content = content
    ) { measurables, constraints ->
        // Expecting 3 children: back, menu, exit
        val slotPx = with(density) { slotWidth.toPx() }.roundToInt()
        val overlapPx = with(density) { collapsedOverlap.toPx() }

        val gapPx = if (splitGap != Dp.Unspecified) with(density) { splitGap.toPx() } else 0f
        val extraGapPx = gapPx * splitP

        val placeables = measurables.map { measurable ->
            measurable.measure(
                constraints.copy(
                    minWidth = slotPx,
                    maxWidth = slotPx
                )
            )
        }

        val height = placeables.maxOfOrNull { it.height } ?: constraints.minHeight

        val xBack = 0f
        val xMenu = slotPx.toFloat()

        val xExitCollapsed = xMenu + overlapPx
        val xExitExpanded = (slotPx * 2).toFloat() + extraGapPx

        val xExit = androidx.compose.ui.util.lerp(xExitCollapsed, xExitExpanded, p)

        layout(constraints.maxWidth, height) {
            if (placeables.size < 3) return@layout

            // Always place back first
            placeables[0].placeRelative(xBack.roundToInt(), 0)

            // Draw order swap (keeps menu clickable when collapsed)
            val exitOnTop = p > 0.55f

            if (!exitOnTop) {
                placeables[2].placeRelative(xExit.roundToInt(), 0) // exit behind
                placeables[1].placeRelative(xMenu.roundToInt(), 0)
            } else {
                placeables[1].placeRelative(xMenu.roundToInt(), 0)
                placeables[2].placeRelative(xExit.roundToInt(), 0) // exit on top
            }
        }
    }
}
