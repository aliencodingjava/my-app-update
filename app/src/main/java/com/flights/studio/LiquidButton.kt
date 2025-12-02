package com.flights.studio

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

@Composable
fun LiquidButton(
    onClick: () -> Unit,
    @DrawableRes iconRes: Int,
    label: String,
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier,
    isInteractive: Boolean = true,
    tint: Color = Color.Unspecified,
    surfaceColor: Color = Color.Unspecified,
) {
    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }
    val isDark = isSystemInDarkTheme()

    Box(
        modifier = modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { CircleShape },
                effects = {
                    vibrancy()
                    if (isDark) {
                        blur(4.dp.toPx())
                        lens(
                            refractionHeight = 8.dp.toPx(),
                            refractionAmount = 48.dp.toPx(),
                            depthEffect = true,
                            chromaticAberration = false
                        )
                        colorControls(
                            brightness = 0.0f,
                            contrast = 1.0f,
                            saturation = 1.9f
                        )
                    } else {
                        blur(0.dp.toPx())
                        lens(
                            refractionHeight = 8.dp.toPx(),
                            refractionAmount = 48.dp.toPx(),
                            depthEffect = true,
                            chromaticAberration = false
                        )
                        colorControls(
                            brightness = 0.0f,
                            contrast = 1.0f,
                            saturation = 1.9f
                        )
                    }
                },
                layerBlock = if (isInteractive) {
                    {
                        val width = size.width
                        val height = size.height

                        val progress = interactiveHighlight.pressProgress

                        val zoomAmountPx = 1.5.dp.toPx()
                        val scale = lerp(1f, 1f + zoomAmountPx / size.height, progress)

                        val maxOffset = size.minDimension
                        val k = 0.025f
                        val offset = interactiveHighlight.offset

                        translationX = maxOffset * tanh(k * offset.x / maxOffset)
                        translationY = maxOffset * tanh(k * offset.y / maxOffset)

                        val maxDragScale = 1.5.dp.toPx() / size.height
                        val ang = atan2(offset.y, offset.x)

                        scaleX = scale +
                                maxDragScale *
                                abs(cos(ang) * offset.x / size.maxDimension) *
                                (width / height).fastCoerceAtMost(1f)

                        scaleY = scale +
                                maxDragScale *
                                abs(sin(ang) * offset.y / size.maxDimension) *
                                (height / width).fastCoerceAtMost(1f)
                    }
                } else null,
                onDrawSurface = {
                    // 🔹 Only darken in DARK theme
                    if (isDark) {
                        drawRect(Color.Black.copy(alpha = 0.10f))
                    }

                    if (tint.isSpecified) {
                        drawRect(tint, blendMode = BlendMode.Hue)
                        drawRect(tint.copy(alpha = 0.65f))
                    }
                    if (surfaceColor.isSpecified) {
                        drawRect(surfaceColor)
                    }
                }

            )
            .then(if (isInteractive) interactiveHighlight.modifier else Modifier)
            .then(if (isInteractive) interactiveHighlight.gestureModifier else Modifier)
            .clickable(
                interactionSource = null,
                indication = if (isInteractive) null else LocalIndication.current,
                role = Role.Button,
                onClick = onClick
            )
            .height(44.dp)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        val contentColor = Color.White
        val textShadow = Shadow(
            color = Color.Black.copy(alpha = 0.9f),
            offset = Offset(0f, 2f),
            blurRadius = 5f
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (iconRes != 0) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = label,
                    modifier = Modifier
                        .size(20.dp)
                        .drawBehind {
                            drawCircle(
                                color = Color.DarkGray.copy(alpha = 0.70f),
                                radius = size.minDimension * 0.62f,
                                center = center + Offset(0f, 0.dp.toPx())
                            )
                        },
                    colorFilter = ColorFilter.tint(contentColor)
                )
            }


            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (label.isNotEmpty()) {
                    Text(
                        text = label,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.15.sp,
                            shadow = textShadow
                        )
                    )
                }
            }

            if (iconRes != 0) {
                Box(modifier = Modifier.size(10.dp))
            }
        }
    }
}
