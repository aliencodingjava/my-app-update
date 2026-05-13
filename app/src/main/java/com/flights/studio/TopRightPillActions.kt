package com.flights.studio

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
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

@Composable
fun TopRightPillActions(
    modifier: Modifier = Modifier,
    backdrop: Backdrop,
    onHome: () -> Unit,
    onSettings: () -> Unit,
    isInteractive: Boolean = true,
) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(999.dp)

    val tint = if (isDark) {
        Color.White.copy(alpha = 0.10f)
    } else {
        Color.White.copy(alpha = 0.34f)
    }

    val contrast = if (isDark) {
        Color.White.copy(alpha = 0.03f)
    } else {
        Color.White.copy(alpha = 0.50f)
    }

    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(
            animationScope = animationScope
        )
    }

    Surface(
        modifier = modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                highlight = {
                    if (isDark) {
                        Highlight(
                            width = 0.45.dp,
                            blurRadius = 1.6.dp,
                            alpha = 0.50f,
                            style = HighlightStyle.Plain
                        )
                    } else {
                        Highlight(
                            width = 0.40.dp,
                            blurRadius = 1.2.dp,
                            alpha = 1.0f,
                            style = HighlightStyle.Plain
                        )
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
                onDrawSurface = {
                    drawRect(tint)
                    drawRect(contrast)
                }
            )
            .clickable(
                interactionSource = null,
                indication = if (isInteractive) null else LocalIndication.current,
                role = Role.Button,
                onClick = { }
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
            .width(95.dp),
        color = Color.Transparent,
        shape = shape,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 0.dp)
        ) {
            PillIconButton(
                iconRes = R.drawable.ic_oui_home,
                onClick = onHome
            )
            PillIconButton(
                iconRes = R.drawable.baseline_settings_24,
                onClick = onSettings
            )
        }
    }
}

@Composable
private fun RowScope.PillIconButton(
    iconRes: Int,
    onClick: () -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    val interaction = remember { MutableInteractionSource() }

    val pressed by interaction.collectIsPressedAsState()
    val hovered by interaction.collectIsHoveredAsState()

    val active = pressed || hovered

    val pressAlpha by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
        label = "pressAlpha"
    )

    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 1.06f else if (hovered) 1.03f else 1.0f,
        animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
        label = "pressScale"
    )

    val bubbleColor = if (isDark) {
        Color.White.copy(alpha = 0.10f)
    } else {
        Color.White.copy(alpha = 0.28f)
    }

    val iconTint = if (isDark) {
        Color.White.copy(alpha = 0.95f)
    } else {
        Color.Black.copy(alpha = 0.78f)
    }

    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .hoverable(interactionSource = interaction)
            .clickable(
                interactionSource = interaction,
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .graphicsLayer {
                    alpha = pressAlpha
                    scaleX = pressScale
                    scaleY = pressScale
                }
                .clip(RoundedCornerShape(999.dp))
                .background(bubbleColor)
        )

        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
    }
}
