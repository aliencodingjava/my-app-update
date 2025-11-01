package com.flights.studio

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.util.fastFirstOrNull
import kotlin.math.hypot

suspend fun PointerInputScope.inspectDragGestures(
    onDragStart: (down: PointerInputChange) -> Unit = {},
    onDragEnd: (change: PointerInputChange) -> Unit = {},
    onDragCancel: () -> Unit = {},
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        onDragStart(down)

        // initial press ping so your highlight can start
        onDrag(down, Offset.Zero)

        val up = dragAfterTouchSlop(down.id) { change ->
            val delta = change.positionChange()
            if (delta != Offset.Zero) {
                onDrag(change, delta)
                if (change.positionChange() != Offset.Zero) change.consume() // don’t let parents also scroll on this move
            }
        }

        if (up == null) onDragCancel() else onDragEnd(up)
    }
}

private suspend fun AwaitPointerEventScope.dragAfterTouchSlop(
    pointerId: PointerId,
    onDrag: (PointerInputChange) -> Unit
): PointerInputChange? {
    // ✅ use the scope’s viewConfiguration (no .current here)
    val slop = viewConfiguration.touchSlop

    var passedSlop = false
    var startX = 0f
    var startY = 0f
    var started = false
    val currentId = pointerId

    while (true) {
        val event = awaitPointerEvent()
        val change = event.changes.fastFirstOrNull { it.id == currentId } ?: return null

        if (!started) {
            started = true
            startX = change.position.x
            startY = change.position.y
            change.consume() // consume the down so siblings don’t also treat it as a press
        }

        if (change.changedToUpIgnoreConsumed()) {
            val otherStillDown = event.changes.fastFirstOrNull { it.pressed && it.id != currentId }
            return if (otherStillDown == null) change else null
        }

        val dx = change.position.x - startX
        val dy = change.position.y - startY
        if (!passedSlop) {
            if (hypot(dx.toDouble(), dy.toDouble()) >= slop) {
                passedSlop = true
            } else {
                continue
            }
        }

        onDrag(change)
    }
}
