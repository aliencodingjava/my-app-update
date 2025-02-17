package com.flights.studio

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.view.Choreographer
import androidx.core.graphics.ColorUtils
import kotlin.random.Random

@Suppress("OVERRIDE_DEPRECATION")
class MultipleGlowDrawable(
    private val baseColors: List<Int>, // List of colors for neon effect
    refreshRate: Float
) : Drawable(), Choreographer.FrameCallback {

    private val paints = baseColors.map { color ->
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            maskFilter = BlurMaskFilter(25f, BlurMaskFilter.Blur.NORMAL)
            shader = null
        }
    }

    private var offsets = baseColors.map { Random.nextFloat() * 200f }.toMutableList()
    private val moveSpeeds = baseColors.map { Random.nextFloat() * 5f + 2f } // Different speeds
    private val glowSizes = baseColors.map { Random.nextFloat() * 2.0f + 1.5f } // Different sizes

    private val frameTime = (1000 / refreshRate).toLong()
    private var isAnimating = false
    private val choreographer = Choreographer.getInstance()

    init {
        startAnimation()
    }

    private fun setupShaders() {
        val width = bounds.width().toFloat()
        val height = bounds.height().toFloat()

        if (width == 0f || height == 0f) return

        paints.forEachIndexed { index, paint ->
            val glowRadius = width * glowSizes[index]
            val continuousOffset = offsets[index]

            val shader = RadialGradient(
                continuousOffset, height / 2f, glowRadius,
                intArrayOf(
                    ColorUtils.setAlphaComponent(baseColors[index], 40),
                    ColorUtils.setAlphaComponent(baseColors[index], 60),
                    ColorUtils.setAlphaComponent(baseColors[index], 100),
                    ColorUtils.setAlphaComponent(baseColors[index], 60),
                    ColorUtils.setAlphaComponent(baseColors[index], 40)
                ),
                floatArrayOf(0f, 0.3f, 0.5f, 0.7f, 1f),
                Shader.TileMode.MIRROR
            )

            paint.shader = shader
        }

        invalidateSelf()
    }

    fun startAnimation() {
        if (!isAnimating) {
            isAnimating = true
            choreographer.postFrameCallback(this)
        }
    }

    fun stopAnimation() {
        if (isAnimating) {
            isAnimating = false
            choreographer.removeFrameCallback(this)
        }
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!isAnimating) return

        offsets = offsets.mapIndexed { index, offset -> offset + moveSpeeds[index] }.toMutableList()
        setupShaders()
        invalidateSelf()

        choreographer.postFrameCallbackDelayed(this, frameTime)
    }

    override fun draw(canvas: Canvas) {
        if (!bounds.isEmpty) {
            paints.forEach { paint ->
                canvas.drawRect(bounds, paint)
            }
        }
    }

    override fun setAlpha(alpha: Int) {
        paints.forEach { it.alpha = alpha }
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paints.forEach { it.colorFilter = colorFilter }
    }

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        setupShaders()
    }
}

// Factory function to create the glow effect using your chosen color.
fun createInfiniteSlidingGlow(context: Context, color: Int): MultipleGlowDrawable {
    val refreshRate = context.display.refreshRate
    // Create 3 glow layers using the same color.
    return MultipleGlowDrawable(listOf(color, color, color), refreshRate)
}

