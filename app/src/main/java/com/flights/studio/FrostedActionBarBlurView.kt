package com.flights.studio

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.RectF
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import android.util.AttributeSet
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.graphics.withClip

class FrostedActionBarBlurView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var contentView: View? = null
        set(value) {
            field = value
            invalidate()
        }

    var scrimColor: Int = Color.argb(238, 21, 22, 23)
        set(value) {
            field = value
            invalidate()
        }

    var cornerRadiusPx: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    var refractIntensity: Float = 0.62f
        set(value) {
            field = value
            invalidate()
        }

    var useLiquidRefraction: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    var blurRadiusPx: Float = 12f
        set(value) {
            field = value
            invalidate()
        }

    var saturation: Float = 1.18f
        set(value) {
            field = value
            invalidate()
        }

    private val fallbackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val blurPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val clipPath = Path()
    private val clipRect = RectF()
    private var renderNode: RenderNode? = null
    private var runtimeShader: RuntimeShader? = null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            canvas.isHardwareAccelerated &&
            drawRenderNodeBlur(canvas)
        ) {
            postInvalidateOnAnimation()
            return
        }

        fallbackPaint.color = scrimColor
        drawGlassSurface(canvas, fallbackPaint)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun drawRenderNodeBlur(canvas: Canvas): Boolean {
        val source = contentView ?: return false
        if (width <= 0 || height <= 0 || source.width <= 0 || source.height <= 0) return false

        val pad = (density * 44f).toInt()
        val nodeWidth = width
        val nodeHeight = height + pad * 2
        val node = renderNode ?: RenderNode("contacts_action_bar_blur").also {
            renderNode = it
        }

        node.setPosition(0, 0, nodeWidth, nodeHeight)
        node.setRenderEffect(createGlassRenderEffect(nodeWidth.toFloat(), nodeHeight.toFloat(), pad.toFloat()))

        val sourceLocation = IntArray(2)
        val ownLocation = IntArray(2)
        source.getLocationInWindow(sourceLocation)
        getLocationInWindow(ownLocation)

        val recordingCanvas = node.beginRecording(nodeWidth, nodeHeight)
        recordingCanvas.drawColor(Color.TRANSPARENT)
        recordingCanvas.translate(
            (sourceLocation[0] - ownLocation[0]).toFloat(),
            (sourceLocation[1] - ownLocation[1] + pad).toFloat()
        )
        source.draw(recordingCanvas)
        node.endRecording()

        clipToGlassShape(canvas) {
            translate(0f, -pad.toFloat())
            node.alpha = 1f
            drawRenderNode(node)
        }

        if (!useLiquidRefraction) {
            blurPaint.color = scrimColor
            drawGlassSurface(canvas, blurPaint)
        }

        return true
    }

    private inline fun clipToGlassShape(canvas: Canvas, block: Canvas.() -> Unit) {
        val save = canvas.save()
        if (cornerRadiusPx > 0f) {
            clipRect.set(0f, 0f, width.toFloat(), height.toFloat())
            clipPath.reset()
            clipPath.addRoundRect(clipRect, cornerRadiusPx, cornerRadiusPx, Path.Direction.CW)
            canvas.clipPath(clipPath)
        } else {
            canvas.clipRect(0, 0, width, height)
        }
        canvas.block()
        canvas.restoreToCount(save)
    }

    private fun drawGlassSurface(canvas: Canvas, paint: Paint) {
        if (cornerRadiusPx > 0f) {
            clipRect.set(0f, 0f, width.toFloat(), height.toFloat())
            canvas.drawRoundRect(clipRect, cornerRadiusPx, cornerRadiusPx, paint)
        } else {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun createGlassRenderEffect(nodeWidth: Float, nodeHeight: Float, topInset: Float): RenderEffect {
        val blur = RenderEffect.createBlurEffect(blurRadiusPx, blurRadiusPx, Shader.TileMode.DECAL)
        val matrix = ColorMatrix().apply { setSaturation(saturation) }
        val saturated = RenderEffect.createChainEffect(
            RenderEffect.createColorFilterEffect(ColorMatrixColorFilter(matrix)),
            blur
        )

        if (!useLiquidRefraction || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return saturated

        val shader = runtimeShader ?: RuntimeShader(
            resources.openRawResource(R.raw.liquid_glass_shader)
                .bufferedReader()
                .use { it.readText() }
        ).also { runtimeShader = it }

        val targetRadius = cornerRadiusPx.coerceAtLeast(0f)
        val alpha = Color.alpha(scrimColor) / 255f
        shader.setFloatUniform("resolution", nodeWidth, nodeHeight)
        shader.setFloatUniform("center", width / 2f, topInset + height / 2f)
        shader.setFloatUniform("size", width / 2f, height / 2f)
        shader.setFloatUniform("radius", targetRadius, targetRadius, targetRadius, targetRadius)
        shader.setFloatUniform("thickness", 18f * density)
        shader.setFloatUniform("refract_intensity", refractIntensity)
        shader.setFloatUniform("refract_index", 1.34f)
        shader.setFloatUniform(
            "foreground_color_premultiplied",
            Color.red(scrimColor) / 255f * alpha,
            Color.green(scrimColor) / 255f * alpha,
            Color.blue(scrimColor) / 255f * alpha,
            alpha
        )

        return RenderEffect.createChainEffect(
            RenderEffect.createRuntimeShaderEffect(shader, "img"),
            saturated
        )
    }

    private val density: Float
        get() = resources.displayMetrics.density
}
