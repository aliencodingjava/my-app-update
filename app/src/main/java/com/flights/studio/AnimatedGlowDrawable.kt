package com.flights.studio

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.view.Choreographer
import androidx.core.graphics.ColorUtils
import kotlin.math.sin

@Suppress("OVERRIDE_DEPRECATION")
class MultipleGlowDrawable(
    private val baseColors: List<Int>,
    refreshRate: Float
) : Drawable(), Choreographer.FrameCallback {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        maskFilter = BlurMaskFilter(30f, BlurMaskFilter.Blur.NORMAL)
        style = Paint.Style.FILL
    }

    private val frameTime = (7000 / refreshRate).toLong()
    private var isAnimating = false
    private val choreographer = Choreographer.getInstance()

    private var pulsePhase = 0f
    private var pulseSpeed = 0.01f

    init {
        startAnimation()
    }

    private fun updatePulse() {
        pulsePhase += pulseSpeed
        if (pulsePhase > 1f) pulsePhase = 0f
    }
    private fun computeAlpha(): Int {
        val fade = (sin(pulsePhase * Math.PI * 2).toFloat() + 1f) / 2f
        return (80 * fade).toInt().coerceIn(0, 255)
    }


    private fun setupShader() {
        val width = bounds.width().toFloat()
        val height = bounds.height().toFloat()
        if (width == 0f || height == 0f) return

        val shader = LinearGradient(
            0f, 0f, width, height,
            baseColors.map { ColorUtils.setAlphaComponent(it, computeAlpha()) }.toIntArray(),
            null,
            Shader.TileMode.CLAMP
        )

        paint.shader = shader
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
        updatePulse()
        setupShader()
        invalidateSelf()
        choreographer.postFrameCallbackDelayed(this, frameTime)
    }

    override fun draw(canvas: Canvas) {
        if (!bounds.isEmpty) {
            canvas.drawRect(bounds, paint)
        }
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        setupShader()
    }
}

fun createInfiniteSlidingGlow(context: Context, color: Int): MultipleGlowDrawable {
    val refreshRate = context.display.refreshRate
    return MultipleGlowDrawable(listOf(color, color, color), refreshRate)
}
