package com.flights.studio

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

class RightSquirclePillShape(
    private val progress: Float,
    private val minRightRadiusFactor: Float = 0.28f // smaller = more square
) : Shape {

    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val p = progress.coerceIn(0f, 1f)
        val r = size.height / 2f

        val right = lerpFloat(r, r * minRightRadiusFactor, p)

        val rr = RoundRect(
            left = 0f,
            top = 0f,
            right = size.width,
            bottom = size.height,
            topLeftCornerRadius = CornerRadius(r, r),
            bottomLeftCornerRadius = CornerRadius(r, r),
            topRightCornerRadius = CornerRadius(right, right),
            bottomRightCornerRadius = CornerRadius(right, right),
        )

        return Outline.Generic(Path().apply { addRoundRect(rr) })
    }
}

private fun lerpFloat(a: Float, b: Float, t: Float): Float =
    a + (b - a) * t.coerceIn(0f, 1f)
