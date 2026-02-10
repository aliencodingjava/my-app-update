package com.flights.studio

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

@Composable
fun TopBarLiquidIconButton(
    @DrawableRes iconRes: Int,
    backdrop: LayerBackdrop,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isInteractive: Boolean = true,
    tint: Color = Color.Yellow, // <- optional override
) {
    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }
//    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    // ✅ Decide final tint: if caller didn't pass one, use white by default
    val effectiveTint = if (tint.isSpecified) {
        tint
    } else {
        Color.White
    }

    Row(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { CircleShape },
                highlight = {
                    Highlight(
                        width = 0.35.dp,
                        blurRadius = 1.2.dp,
                        alpha = 0.55f,
                        style = HighlightStyle.Plain // softer than Default
                    )
                },
                effects = {
                    vibrancy()
                    blur(0.5f.dp.toPx())
                    lens(10f.dp.toPx(), 10f.dp.toPx())
                },
                layerBlock = if (isInteractive) {
                    {
                        val width = size.width
                        val height = size.height

                        val progress = interactiveHighlight.pressProgress
                        val scale = lerp(1f, 1f + 9.dp.toPx() / size.height, progress)

                        val maxOffset = size.minDimension
                        val k = 0.03f
                        val offset = interactiveHighlight.offset
                        translationX = maxOffset * tanh(k * offset.x / maxOffset)
                        translationY = maxOffset * tanh(k * offset.y / maxOffset)

                        val maxDragScale = 2.dp.toPx() / size.height
                        val ang = atan2(offset.y, offset.x)
                        scaleX = scale +
                                maxDragScale * abs(cos(ang) * offset.x / size.maxDimension) *
                                (width / height).fastCoerceAtMost(1f)
                        scaleY = scale +
                                maxDragScale * abs(sin(ang) * offset.y / size.maxDimension) *
                                (height / width).fastCoerceAtMost(1f)
                    }
                } else null,
//                onDrawSurface = {
//                    val base = if (isDark) 0.20f else 0.06f
//                    drawRect(Color.White.copy(alpha = base))
//                    if (!isDark) {
//                        drawRect(
//                            Color.Black.copy(alpha = 0.06f),
//                            blendMode = BlendMode.Hue
//                        )
//                    }
//                }
            )
            .then(if (isInteractive) interactiveHighlight.modifier else Modifier)
            .then(if (isInteractive) interactiveHighlight.gestureModifier else Modifier)
            .clickable(
                interactionSource = null,
                indication = if (isInteractive) null else LocalIndication.current,
                role = Role.Button,
                onClick = onClick
            )
            .size(48.dp)
            .padding(horizontal = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            // ✅ always tint with effectiveTint
            colorFilter = ColorFilter.tint(effectiveTint)
        )
    }
}
