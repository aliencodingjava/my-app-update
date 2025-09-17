package com.flights.studio

import android.animation.AnimatorSet
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import java.lang.StrictMath.abs

class NeonGlowFrameLayout @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(ctx, attrs, defStyle) {

    // corner radius (30dp)
    private val cornerRadius = dp(10f)
    // base border width (will animate 4→12→4)
    private val baseStrokeDp = 2f
    private var sweepAnimator: ValueAnimator? = null


    // your Siri‑palette colors
    private val cStart = ContextCompat.getColor(ctx, R.color.material_yellow)
    private val cMid   = ContextCompat.getColor(ctx, R.color.siri_glow_mid)
    private val cEnd   = ContextCompat.getColor(ctx, R.color.siri_glow_end)

    // paint for our neon border
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        // this blur makes it soft
        maskFilter = BlurMaskFilter(dp(8f), BlurMaskFilter.Blur.NORMAL)
    }

    init {
        // we need software rendering for BlurMaskFilter to work
        setLayerType(LAYER_TYPE_SOFTWARE, paint)
        // clip children to our rounded outline
        clipToPadding = false
        clipToOutline = false
        // default background can still be whatever your sheet is
        // we just draw on top of it
    }

    fun playSweep() {
        // cancel any previous sweep
        sweepAnimator?.cancel()

        if (width == 0) return         // view not measured yet

        val w    = width.toFloat()
        val neon = ContextCompat.getColor(context, R.color.holo_yellow_light)

        //  transparent → neon → neon → transparent  (horizontal)
        val shader = LinearGradient(
            -w, 0f, +w, 0f,
            intArrayOf(Color.TRANSPARENT, neon, neon, Color.TRANSPARENT),
            floatArrayOf(0f, 0.45f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.shader = shader
        paint.alpha  = 255                       // show outline

        sweepAnimator = ValueAnimator.ofFloat(-w, +w).apply {
            duration     = 1_200L
            interpolator = AccelerateDecelerateInterpolator()

            /* move the gradient */
            addUpdateListener { a ->
                val dx = a.animatedValue as Float
                shader.setLocalMatrix(Matrix().apply { setTranslate(dx, 0f) })
                invalidate()
            }

            /* ← NEW – wipe everything when we’re done */
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    paint.shader = null      // no gradient
                    paint.alpha  = 255         // fully transparent
                    invalidate()             // redraw without outline
                }
            })

            start()
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        // draw twice for concentric glow
        drawRoundCorners(canvas, paint)
        val innerPaint = Paint(paint).apply {
            strokeWidth = paint.strokeWidth / 2
            maskFilter  = BlurMaskFilter(dp(8f), BlurMaskFilter.Blur.NORMAL)
        }
        drawRoundCorners(canvas, innerPaint)
    }

    private fun drawRoundCorners(c: Canvas, p: Paint) {
        val half = p.strokeWidth / 2
        val rect = RectF(half, half, width-half, height-half)
        // only top‑corners rounding
        val radii = floatArrayOf(cornerRadius,cornerRadius,
            cornerRadius,cornerRadius,
            0f,0f, 0f,0f)
        val path = Path().apply {
            reset()
            addRoundRect(rect, radii, Path.Direction.CW)
        }
        c.drawPath(path, p)
    }

    /** Call this to kick off the glow+pop animation when you succeed. */
    /** Call this to kick off the glow animation when you succeed. */
    fun playGlow() {
        // 1) animate stroke width & color
        val glow = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1200
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { ani ->
                val t = ani.animatedFraction
                // strokeWidth pulses 4→16→4
                paint.strokeWidth = dp(baseStrokeDp + 12 * (1 - abs(2*t - 1)))

                // horizontal gradient from left→right
                paint.shader = LinearGradient(
                    0f, 0f, width.toFloat(), 0f,
                    intArrayOf(
                        if (t < 0.5f) lerpColor(cStart, cMid, t*2f)
                        else lerpColor(cMid, cEnd, (t-0.5f)*2f),
                        Color.YELLOW
                    ),
                    floatArrayOf(0f, 1f),
                    Shader.TileMode.CLAMP
                )
                invalidate()
            }
        }

        // 2) only horizontal pop (no Y‐scale)
        val popX = ValueAnimator.ofFloat(1f, 1.03f, 1f).apply {
            duration = 1000
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { ani ->
                val sx = ani.animatedValue as Float
                scaleX = sx
                // leave scaleY == 1f
            }
        }

        AnimatorSet().apply {
            playTogether(glow, popX)
            start()
        }
    }
    private fun dp(v: Float) = (v * resources.displayMetrics.density)

    private fun lerpColor(a: Int, b: Int, t: Float) =
        ArgbEvaluator().evaluate(t.coerceIn(0f, 1f), a, b) as Int
}

