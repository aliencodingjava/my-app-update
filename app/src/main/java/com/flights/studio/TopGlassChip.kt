package com.flights.studio

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sign
import kotlin.math.tanh

@Composable
fun TopGlassChip(
    modifier: Modifier = Modifier,
    backdrop: com.kyant.backdrop.Backdrop,
    isInteractive: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.RowScope.(Color) -> Unit
) {
    val shape = RoundedCornerShape(999.dp)
    val adaptiveEnabled = rememberLiquidGlassAdaptiveLuminanceEnabled()
    val blurAmount = rememberLiquidGlassBlurAmount()
    val adaptive = rememberAdaptiveLuminance(enabled = adaptiveEnabled)
    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(
            animationScope = animationScope
        )
    }
    val luminanceOffset = if (adaptiveEnabled) {
        adaptiveLuminanceOffset(adaptive.luminance)
    } else {
        0f
    }

    Row(
        modifier = modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                highlight = {
                    Highlight(
                        width = 0.45.dp,
                        blurRadius = 1.4.dp,
                        alpha = 0.86f,
                        style = HighlightStyle.Plain
                    )
                },
                shadow = null,
                effects = {
                    if (adaptiveEnabled) {
                        colorControls(
                            brightness = adaptiveLuminanceBrightness(luminanceOffset),
                            contrast = adaptiveLuminanceContrast(luminanceOffset),
                            saturation = 1.5f
                        )
                    }
                    blur(
                        radius = if (adaptiveEnabled) {
                            val baseBlurPx = 8.dp.toPx() * blurAmount
                            adaptiveLuminanceBlurPx(
                                offset = luminanceOffset,
                                baseBlurPx = baseBlurPx,
                                dpToPx = { it.dp.toPx() }
                            )
                        } else {
                            8.dp.toPx() * blurAmount
                        }
                    )
                    lens(
                        refractionHeight = 24.dp.toPx(),
                        refractionAmount = size.minDimension / 2f,
                        depthEffect = true,
                        chromaticAberration = false
                    )
                },
                onDrawBackdrop = { drawBackdrop ->
                    drawBackdrop()
                    if (adaptiveEnabled) {
                        adaptive.layer.record {
                            drawBackdrop()
                        }
                    }
                },
                layerBlock = if (isInteractive) {
                    {
                        val width = size.width
                        val height = size.height

                        val progress = interactiveHighlight.pressProgress
                        val scale = lerp(1f, 1f + 4f.dp.toPx() / size.height, progress)

                        val maxOffset = size.minDimension
                        val initialDerivative = 0.05f
                        val offset = interactiveHighlight.offset
                        translationX = maxOffset * tanh(initialDerivative * offset.x / maxOffset)
                        translationY = maxOffset * tanh(initialDerivative * offset.y / maxOffset)

                        val maxDragScale = 4f.dp.toPx() / size.height
                        val offsetAngle = atan2(offset.y, offset.x)
                        scaleX =
                            scale +
                                    maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) *
                                    (width / height).fastCoerceAtMost(1f)
                        scaleY =
                            scale +
                                    maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) *
                                    (height / width).fastCoerceAtMost(1f)
                    }
                } else {
                    null
                },
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = null,
                        indication = if (isInteractive) null else LocalIndication.current,
                        role = Role.Button,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
            .then(
                if (isInteractive) {
                    Modifier
                        .then(interactiveHighlight.modifier)
                        .then(interactiveHighlight.gestureModifier)
                } else {
                    Modifier
                }
            )
            .height(45.dp)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content(adaptive.contentColor)
    }
}
