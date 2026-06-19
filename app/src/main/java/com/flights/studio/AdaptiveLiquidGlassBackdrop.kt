package com.flights.studio

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow

@Composable
internal fun Modifier.adaptiveLiquidGlassBackdrop(
    backdrop: Backdrop,
    shape: Shape,
    surfaceColor: Color,
    blurDp: Float,
    shadow: (() -> Shadow)? = null,
    highlight: (() -> Highlight)? = null,
    refractionHeightDp: Float = GlassChromeRefractionHeightDp,
    refractionAmountDp: Float = GlassChromeRefractionAmountDp,
    depthEffect: Boolean = false,
    chromaticAberration: Boolean = false,
    onDrawExtraSurface: DrawScope.(Size) -> Unit = {}
): Modifier {
    val tintAmount = rememberLiquidGlassTintAmount()
    val adaptiveEnabled = rememberLiquidGlassAdaptiveLuminanceEnabled()
    val adaptive = rememberAdaptiveLuminance(
        enabled = adaptiveEnabled,
        lightOnBright = Color(0xFF101318),
        lightOnDark = Color.White
    )
    val adaptiveOffset = adaptiveLuminanceOffset(adaptive.luminance)
    val adaptiveEffectStrength = if (adaptiveEnabled) {
        adaptiveLuminanceEffectStrength(tintAmount)
    } else {
        0f
    }
    val adaptiveSurfaceStrength = if (adaptiveEnabled) {
        lerp(0.45f, 1f, adaptiveEffectStrength)
    } else {
        0f
    }
    val adaptiveTint = adaptiveSurfaceTint(
        luminance = adaptive.luminance,
        strength = adaptiveSurfaceStrength
    )

    return drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        shadow = shadow,
        highlight = highlight,
        effects = {
            if (adaptiveEffectStrength > 0.001f) {
                val adaptiveBrightness = adaptiveLuminanceBrightness(adaptiveOffset)
                val adaptiveContrast = adaptiveLuminanceContrast(adaptiveOffset)
                colorControls(
                    brightness = adaptiveBrightness * adaptiveEffectStrength,
                    contrast = lerp(1f, adaptiveContrast, adaptiveEffectStrength),
                    saturation = lerp(1f, 1.5f, adaptiveEffectStrength)
                )
            }
            vibrancy()
            blur(
                radius = if (adaptiveEffectStrength > 0.001f) {
                    val baseBlurPx = blurDp.dp.toPx()
                    val adaptiveBlurPx = adaptiveLuminanceBlurPx(
                        offset = adaptiveOffset,
                        baseBlurPx = baseBlurPx,
                        dpToPx = { it.dp.toPx() }
                    )
                    lerp(baseBlurPx, adaptiveBlurPx, adaptiveEffectStrength)
                } else {
                    blurDp.dp.toPx()
                },
                edgeTreatment = TileMode.Mirror
            )
            lens(
                refractionHeight = refractionHeightDp.dp.toPx(),
                refractionAmount = refractionAmountDp.dp.toPx(),
                depthEffect = depthEffect,
                chromaticAberration = chromaticAberration
            )
        },
        onDrawBackdrop = { drawBackdrop ->
            drawBackdrop()
            adaptive.layer.record {
                drawBackdrop()
            }
        },
        onDrawSurface = {
            drawRect(surfaceColor)
            if (adaptiveSurfaceStrength > 0f) {
                drawRect(adaptiveTint)
            }
            onDrawExtraSurface(size)
        }
    )
}
