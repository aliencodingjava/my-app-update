package com.flights.studio

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

/**
 * Different visual treatments, like in Kyant's demo:
 * - Transparent: raw glass
 * - Surface: frosted/milky white for readability
 * - Tinted: filled with a color (blue/orange pill vibes)
 */
sealed interface LiquidStyle {
    data object Transparent : LiquidStyle
//    data object Surface : LiquidStyle
//    data class Tinted(val color: Color) : LiquidStyle
}

/**
 * One self-contained "liquid glass bubble" button.
 * - Handles squish/drag physics (InteractiveHighlight)
 * - Renders glass via drawBackdrop()
 * - Applies style overlay (Surface/Tinted/etc)
 * - Shows [icon + label]
 */
@Composable
fun LiquidCircleButtonExactPhysics(
    onClick: () -> Unit,
    @DrawableRes iconRes: Int,
    label: String,
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier,
    tint: Color = Color(0xFF40C4FF),
    surfaceColor: Color = Color.White.copy(alpha = 0.08f),
    iconTintOverride: Color? = null,
    sizeDp: Int = 72
) {
    val scope = rememberCoroutineScope()
    val interactiveHighlight = remember(scope) {
        InteractiveHighlight(animationScope = scope)
    }

    val darkMode = isSystemInDarkTheme()

    val baseContentColor = if (darkMode) {
        Color.White.copy(alpha = 0.95f)
    } else {
        Color.Black.copy(alpha = 0.85f)
    }
    val contentColor = iconTintOverride ?: baseContentColor

    Box(
        modifier
            .then(interactiveHighlight.modifier)
            .then(interactiveHighlight.gestureModifier)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { CircleShape },
                effects = {
                    vibrancy()
                    blur(SoftGlassTheme.blurRadius.toPx())
                    lens(
                        SoftGlassTheme.lensInner.toPx(),
                        SoftGlassTheme.lensOuter.toPx()
                    )
                },
                layerBlock = {
                    val w = size.width
                    val h = size.height
                    val pressProgress = interactiveHighlight.pressProgress
                    val offset = interactiveHighlight.offset
                    val minDim = kotlin.math.min(w, h)

                    val baseScale = lerp(
                        1f,
                        1f + 4.dp.toPx() / h,
                        pressProgress
                    )

                    val k = 0.05f
                    translationX = minDim * kotlin.math.tanh(k * offset.x / minDim)
                    translationY = minDim * kotlin.math.tanh(k * offset.y / minDim)

                    val maxDragScale = 4.dp.toPx() / h
                    val angle = kotlin.math.atan2(offset.y, offset.x)

                    scaleX = baseScale +
                            maxDragScale *
                            kotlin.math.abs(
                                kotlin.math.cos(angle) * offset.x / size.maxDimension
                            ) *
                            (w / h).coerceAtMost(1f)

                    scaleY = baseScale +
                            maxDragScale *
                            kotlin.math.abs(
                                kotlin.math.sin(angle) * offset.y / size.maxDimension
                            ) *
                            (h / w).coerceAtMost(1f)
                },
                onDrawSurface = {
                    if (darkMode) {
                        // DARK THEME: your neon-tinted chip style
                        drawRect(
                            color = tint.copy(alpha = 0.10f),
                            blendMode = androidx.compose.ui.graphics.BlendMode.Hue
                        )
                        drawRect(
                            color = tint.copy(alpha = 0.06f)
                        )
                        drawRect(
                            color = surfaceColor.copy(alpha = 0.05f)
                        )
                    } else {
                        // LIGHT THEME: bright frosted acrylic / milky glass
                        drawRect(
                            color = Color.White.copy(alpha = 0.45f)
                        )
                        drawRect(
                            color = Color.White.copy(alpha = 0.18f)
                        )
                        // no blue tint in light mode so it doesn't look "dirty"
                    }
                }
            )
            .clickable(
                interactionSource = null,
                indication = null,
                onClick = onClick
            )
            .size(sizeDp.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = label,
                modifier = Modifier.size(22.dp),
                colorFilter = ColorFilter.tint(contentColor)
            )

            if (label.isNotEmpty()) {
                Spacer(Modifier.height(3.dp))
                BasicText(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = contentColor
                    )
                )
            }
        }
    }
}
