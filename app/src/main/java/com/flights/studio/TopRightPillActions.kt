package com.flights.studio

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle


@Composable
fun TopRightPillActions(
    modifier: Modifier = Modifier,
    backdrop: Backdrop,
    onHome: () -> Unit,
    onSettings: () -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(999.dp)

    val (tint, contrast) = glassTint(isDark)

    Surface(
        modifier = modifier
            .height(50.dp)
            .width(100.dp)
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
                            width = 0.30.dp,
                            blurRadius = 1.0.dp,
                            alpha = 0.95f,
                            style = HighlightStyle.Plain // very subtle
                        )
                    }
                },
                shadow = null,
                effects = {
//                    colorControls(
//                        brightness = if (isDark) -0.03f else 0.02f,
//                        contrast = if (isDark) 1.10f else 1.05f,
//                        saturation = if (isDark) 1.10f else 1.05f
//                    )
                    vibrancy()
                    blur(0f.dp.toPx())
                    lens(
                        refractionHeight = 10f.dp.toPx(),
                        refractionAmount = 22f.dp.toPx(),
                        depthEffect = true
                    )
                },
                onDrawSurface = {
                    drawRect(tint)
                    contrast?.let { drawRect(it) }
                }
            ),
        color = Color.Transparent,
        shape = shape,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 0.dp) // ✅ small space left/right
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
    val hovered by interaction.collectIsHoveredAsState() // ✅ pen/mouse hover

    val active = pressed || hovered // ✅ hover OR press

    val pressAlpha by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
        label = "pressAlpha"
    )

    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 1.06f else if (hovered) 1.03f else 1.0f, // ✅ subtle hover, stronger press
        animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
        label = "pressScale"
    )

    val bubbleColor = if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.10f)
    val iconTint = if (isDark) Color.White.copy(alpha = 0.95f) else Color.Black.copy(alpha = 0.85f)

    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .hoverable(interactionSource = interaction) // ✅ THIS enables hover highlight
            .clickable(
                interactionSource = interaction,
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
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
            modifier = Modifier.size(24.dp)
        )
    }
}
