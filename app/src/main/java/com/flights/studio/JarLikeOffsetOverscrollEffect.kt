@file:Suppress("FunctionName", "unused")

package com.flights.studio

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.sign
import kotlin.math.tanh

class OffsetOverscrollEffect(
    private val orientation: Orientation,
    private val animationScope: CoroutineScope,
    private val animationSpec: AnimationSpec<Float> = DefaultAnimationSpec,
) : OverscrollEffect {

    companion object {
        // Exact: SpringSpec(dampingRatio=1f, stiffness=150f, visibilityThreshold=0.5f)
        val DefaultAnimationSpec: SpringSpec<Float> = spring(
            dampingRatio = 1f,
            stiffness = 150f,
            visibilityThreshold = 0.5f
        )

        private const val THRESHOLD = 0.5f
    }

    private val animatable = Animatable(0f, Float.VectorConverter, THRESHOLD)

    private fun axisValue(o: Offset): Float =
        if (orientation == Orientation.Vertical) o.y else o.x

    private fun axisOffset(v: Float): Offset =
        if (orientation == Orientation.Vertical) Offset(0f, v) else Offset(v, 0f)

    private fun axisVelocity(v: Velocity): Float =
        if (orientation == Orientation.Vertical) v.y else v.x

    // Exact: always false
    override val isInProgress: Boolean get() = false

    override fun applyToScroll(
        delta: Offset,
        source: NestedScrollSource,
        performScroll: (Offset) -> Offset
    ): Offset {
        val d = axisValue(delta)
        val cur0 = animatable.value

        var preConsumed = 0f

        // Unwind overscroll if we already have it and user scrolls opposite direction.
        // Note: this part runs regardless of source.
        if (abs(cur0) > THRESHOLD && d != 0f && sign(d) != sign(cur0)) {
            val next = cur0 + d
            if (sign(next) == sign(cur0)) {
                animationScope.launch { animatable.snapTo(next) }
                preConsumed = d
            } else {
                animationScope.launch { animatable.snapTo(0f) }
                preConsumed = -cur0
            }
        }

        val consumedByPre = axisOffset(preConsumed)
        val remaining = delta - consumedByPre

        val childConsumed = performScroll(remaining)
        val leftover = remaining - childConsumed

        // Only accumulate overscroll from leftover during direct user input.
        if (source == NestedScrollSource.UserInput) {
            val l = axisValue(leftover)
            if (abs(l) > THRESHOLD) {
                animationScope.launch { animatable.snapTo(cur0 + l) }
            }
        }

        // Exact: do not consume leftover.
        return consumedByPre + childConsumed
    }

    override suspend fun applyToFling(
        velocity: Velocity,
        performFling: suspend (Velocity) -> Velocity
    ) {
        // Exact behavior: run performFling, then launch spring-back work in a child coroutine
        // (so we don't block the fling pipeline).
        coroutineScope {
            launch {
                val remaining = performFling(velocity)
                val consumed = velocity - remaining
                val v0 = axisVelocity(consumed)

                if (abs(animatable.value) <= THRESHOLD) return@launch

                animatable.animateTo(
                    targetValue = 0f,
                    animationSpec = animationSpec,
                    initialVelocity = v0
                )
            }
        }
    }

    override val node: DelegatableNode = object : Modifier.Node(), LayoutModifierNode {

        // Exact: false
        override val shouldAutoInvalidate: Boolean = false

        override fun MeasureScope.measure(
            measurable: Measurable,
            constraints: Constraints
        ): MeasureResult {
            val placeable = measurable.measure(constraints)

            return layout(placeable.width, placeable.height) {
                val sizePx =
                    (if (orientation == Orientation.Vertical) constraints.maxHeight else constraints.maxWidth)
                        .coerceAtLeast(1)
                val sizeF = sizePx.toFloat()

                val v = animatable.value

                // Exact formula: round( tanh((v/size)/2) * size )
                val rubber = tanh(((v / sizeF) / 2f).toDouble()).toFloat() * sizeF
                val o = round(rubber).toInt()

                if (orientation == Orientation.Vertical) {
                    placeable.placeRelative(0, o)
                } else {
                    placeable.placeRelative(o, 0)
                }
            }
        }
    }
}
