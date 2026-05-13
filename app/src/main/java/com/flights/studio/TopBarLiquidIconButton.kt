package com.flights.studio

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.fastCoerceAtMost
import com.kyant.backdrop.Backdrop
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
import androidx.compose.ui.util.lerp as lerpFloat

@Composable
fun TopLeftPillActions(
    modifier: Modifier = Modifier,
    backdrop: Backdrop,
    @DrawableRes backIconRes: Int = R.drawable.exit_to_app_24dp_ffffff_fill1_wght400_grad0_opsz24,
    @DrawableRes exitIconRes: Int = R.drawable.ic_samsung_close,
    onExit: () -> Unit,
    isInteractive: Boolean = true,
) {
    val isDark = isSystemInDarkTheme()
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val highlight = remember(scope) { InteractiveHighlight(animationScope = scope) }

    var isOpen by rememberSaveable { mutableStateOf(false) }
    val morph = remember { Animatable(0f) }
    LaunchedEffect(isOpen) {
        morph.animateTo(
            targetValue = if (isOpen) 1f else 0f,
            animationSpec = spring(
                dampingRatio = if (isOpen) 0.82f else 0.92f,
                stiffness = if (isOpen) 155f else 210f,
                visibilityThreshold = 0.001f
            )
        )
    }

    val p = morph.value.coerceIn(0f, 1f)
    val reveal = FastOutSlowInEasing.transform(p)
    val slot = 52.dp
    val expandedWidth = slot * 3
    val pillWidth = lerp(slot, expandedWidth, reveal)
    val shape = RoundedCornerShape(999.dp)

    val containerColor = if (isDark) {
        Color(0xFF123B6D).copy(alpha = 0.42f)
    } else {
        Color(0xFFEAF4FF).copy(alpha = 0.52f)
    }

    val spacerAlpha by animateFloatAsState(
        targetValue = if (isOpen) 0.32f else 0f,
        animationSpec = tween(260, easing = FastOutSlowInEasing),
        label = "spacerAlpha"
    )

    Surface(
        modifier = modifier
            .height(50.dp)
            .width(pillWidth)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                highlight = {
                    if (isDark) {
                        Highlight(0.45.dp, 1.6.dp, 0.50f, HighlightStyle.Plain)
                    } else {
                        Highlight(0.30.dp, 1.0.dp, 0.95f, HighlightStyle.Plain)
                    }
                },
                shadow = null,
                effects = {
                    vibrancy()
                    blur(1.5.dp.toPx(), edgeTreatment = TileMode.Clamp)
                    lens(
                        refractionHeight = 20.dp.toPx(),
                        refractionAmount = 30.dp.toPx(),
                        depthEffect = false,
                        chromaticAberration = false
                    )
                },
                layerBlock = if (isInteractive) {
                    {
                        val width = size.width
                        val height = size.height
                        val press = highlight.pressProgress
                        val baseScale = lerpFloat(1f, 1f + 8.dp.toPx() / height, press)
                        val openLift = 1f + 0.014f * reveal
                        val maxOffset = size.minDimension
                        val offset = highlight.offset
                        val angle = atan2(offset.y, offset.x)
                        val dragScale = 2.dp.toPx() / height

                        translationX = maxOffset * tanh(0.03f * offset.x / maxOffset)
                        translationY = maxOffset * tanh(0.03f * offset.y / maxOffset)
                        scaleX = baseScale * openLift +
                            dragScale * abs(cos(angle) * offset.x / size.maxDimension) *
                            (width / height).fastCoerceAtMost(1f)
                        scaleY = baseScale +
                            dragScale * abs(sin(angle) * offset.y / size.maxDimension) *
                            (height / width).fastCoerceAtMost(1f)
                    }
                } else {
                    null
                },
                onDrawSurface = { drawRect(containerColor) }
            )
            .then(if (isInteractive) highlight.modifier else Modifier)
            .then(if (isInteractive) highlight.gestureModifier else Modifier),
        color = Color.Transparent,
        shape = shape,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        PillMorphLayout(
            modifier = Modifier.fillMaxSize(),
            slotWidth = slot,
            progress = reveal,
            collapsedOverlap = 0.dp
        ) {
            MorphingIconSlot(
                slotWidth = slot,
                iconRes = backIconRes,
                bubbleMode = BubbleMode.Normal,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                    isOpen = !isOpen
                }
            )

            Box(
                modifier = Modifier
                    .width(slot)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .graphicsLayer { alpha = spacerAlpha }
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            if (isDark) Color.White.copy(alpha = 0.58f)
                            else Color.Black.copy(alpha = 0.36f)
                        )
                )
            }

            MorphingIconSlot(
                slotWidth = slot,
                iconRes = exitIconRes,
                enabled = reveal > 0.55f,
                bubbleMode = if (reveal > 0.55f) BubbleMode.Normal else BubbleMode.Hide,
                modifier = Modifier.graphicsLayer {
                    alpha = reveal
                    translationX = 10f * (1f - reveal)
                    scaleX = 0.82f + 0.18f * reveal
                    scaleY = 0.82f + 0.18f * reveal
                },
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                    isOpen = false
                    onExit()
                }
            )
        }
    }
}

private enum class BubbleMode { Normal, Hide }

@Composable
private fun MorphingIconSlot(
    slotWidth: Dp,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    bubbleMode: BubbleMode = BubbleMode.Normal,
) {
    val isDark = isSystemInDarkTheme()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val active = enabled && (pressed || hovered)

    val bubbleAlpha by animateFloatAsState(
        targetValue = if (bubbleMode == BubbleMode.Normal && active) 1f else 0f,
        animationSpec = tween(120, easing = FastOutSlowInEasing),
        label = "bubbleAlpha"
    )
    val contentScale by animateFloatAsState(
        targetValue = when {
            !enabled -> 1f
            pressed -> 0.92f
            hovered -> 1.04f
            else -> 1f
        },
        animationSpec = tween(150, easing = CubicBezierEasing(0.18f, 0.92f, 0.20f, 1.00f)),
        label = "contentScale"
    )

    val bubbleColor = if (isDark) {
        Color.White.copy(alpha = 0.12f)
    } else {
        Color.Black.copy(alpha = 0.10f)
    }
    val iconTint = if (isDark) Color.White else Color.Black

    Box(
        modifier = modifier
            .width(slotWidth)
            .fillMaxHeight()
            .hoverable(interactionSource = interaction, enabled = enabled)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .graphicsLayer {
                    alpha = bubbleAlpha
                    scaleX = contentScale
                    scaleY = contentScale
                }
                .clip(RoundedCornerShape(999.dp))
                .background(bubbleColor)
        )
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    scaleX = contentScale
                    scaleY = contentScale
                }
        )
    }
}
