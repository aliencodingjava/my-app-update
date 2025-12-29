package com.flights.studio   // ⚠️ make sure folder matches this

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Stable
data class ExpandCollapseGestureConfig(
    val expandDistance: Dp = 90.dp,     // drag DOWN to expand
    val collapseDistance: Dp = 70.dp,   // drag UP to collapse
    val flingVelocity: Float = 1200f
)

@Composable
fun Modifier.expandCollapseGrabGesture(
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    config: ExpandCollapseGestureConfig = ExpandCollapseGestureConfig(),
): Modifier {
    if (!enabled) return this

    val density = LocalDensity.current

    val expandDistPx = remember(density, config.expandDistance) {
        with(density) { config.expandDistance.toPx() }
    }
    val collapseDistPx = remember(density, config.collapseDistance) {
        with(density) { config.collapseDistance.toPx() }
    }

    return pointerInput(isExpanded, expandDistPx, collapseDistPx, config.flingVelocity) {

        val totalDragY = 0f
        val velocityTracker = VelocityTracker()

        detectDragGestures(
            onDragStart = {
                velocityTracker.resetTracking()
            },
            onDrag = { change, dragAmount ->
                dragAmount.y
                velocityTracker.addPosition(change.uptimeMillis, change.position)
                // ❌ DO NOT consume
            },
            onDragEnd = {
                val velocityY = velocityTracker.calculateVelocity().y

                val shouldExpand =
                    !isExpanded &&
                            (totalDragY > expandDistPx || velocityY > config.flingVelocity)

                val shouldCollapse =
                    isExpanded &&
                            (totalDragY < -collapseDistPx || velocityY < -config.flingVelocity)

                when {
                    shouldExpand -> onExpandedChange(true)
                    shouldCollapse -> onExpandedChange(false)
                }
            }
        )
    }
}
