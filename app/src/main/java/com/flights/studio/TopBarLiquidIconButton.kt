package com.flights.studio

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tanh

@Composable
fun TopLeftPillActions(
    modifier: Modifier = Modifier,
    backdrop: Backdrop,
    @DrawableRes backIconRes: Int,
    @DrawableRes menuIconRes: Int,
    @DrawableRes exitIconRes: Int,
    onMenu: () -> Unit,
    onExit: () -> Unit,
    isInteractive: Boolean = true,
) {
    val splitAnim = remember { Animatable(0f) }
    val isDark = isSystemInDarkTheme()
    val morph = remember { Animatable(0f) }

    val p = morph.value.coerceIn(0f, 1f)
    val shape = remember(p) { RightSquirclePillShape(progress = p, minRightRadiusFactor = 0.28f) }
    val (tint, contrast) = bglassTint(isDark)

    val scope = rememberCoroutineScope()
    val pillHighlight = remember(scope) { InteractiveHighlight(animationScope = scope) }

    var exitRevealed by rememberSaveable { mutableStateOf(false) }
    var lastTarget by remember { mutableStateOf(false) }

    // Sizes
    val slot: Dp = 52.dp
    val collapsedWidth: Dp = slot * 2
    val expandedWidth: Dp = slot * 3
    val splitGap: Dp = 24.dp

    // Split state
    val splitActive = splitAnim.value > 0.001f
    val gapDp = splitGap * splitAnim.value

    // Pill width does NOT grow during split (otherwise it looks like “expanding again”)
    val pillWidth: Dp = lerp(collapsedWidth, expandedWidth, p)

    // Animation driver: expand/collapse, then split pulse after fully expanded
    LaunchedEffect(exitRevealed) {
        val expanding = exitRevealed
        lastTarget = expanding

        // Always reset split when state changes
        splitAnim.snapTo(0f)

        // 1) Expand / collapse
        morph.animateTo(
            targetValue = if (expanding) 1f else 0f,
            animationSpec = if (expanding) {
                tween(260, easing = CubicBezierEasing(0.18f, 0.92f, 0.20f, 1.00f))
            } else {
                tween(420, easing = FastOutSlowInEasing)
            }
        )

        // 2) After fully expanded -> split -> reunify -> collapse
        if (expanding) {
            // split
            splitAnim.animateTo(
                1f,
                animationSpec = tween(140, easing = CubicBezierEasing(0.18f, 0.92f, 0.20f, 1.00f))
            )

            delay(5000)

            // reunify
            splitAnim.animateTo(
                0f,
                animationSpec = tween(260, easing = FastOutSlowInEasing)
            )

            // small beat so the reunify is visible
            delay(120)

            // collapse animation
            morph.animateTo(
                targetValue = 0f,
                animationSpec = tween(420, easing = FastOutSlowInEasing)
            )

            // reset state so next tap works
            exitRevealed = false
            lastTarget = false
        }
    }

    // Tear/stretch curve (disabled while split is active so it doesn’t “fake expand”)
    fun tearCurve(t: Float): Float {
        val x = t.coerceIn(0f, 1f)
        return (x * (1f - x)).pow(0.35f)
    }

    val expanding = lastTarget
    val tear = if (expanding && !splitActive) tearCurve(p) else 0f

    val stretchX = 1f + 0.20f * tear
    val squishY = 1f - 0.16f * tear



    // ---- RENDER ----
    Box(
        modifier = modifier
            .height(50.dp)
            .width(pillWidth)
    ) {
        if (!splitActive) {
            // ✅ ONE pill (normal state)
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(0f, 0.5f)
                        scaleX = stretchX
                        scaleY = squishY
                    }
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
                                    style = HighlightStyle.Plain
                                )
                            }
                        },
                        shadow = null,
                        effects = {
                            colorControls(
                                brightness = if (isDark) -0.03f else 0.02f,
                                contrast = if (isDark) 1.10f else 1.05f,
                                saturation = if (isDark) 1.10f else 1.05f
                            )
                            vibrancy()
                            blur(4f.dp.toPx())
                            lens(
                                refractionHeight = 0f.dp.toPx(),
                                refractionAmount = 0f.dp.toPx(),
                                depthEffect = true
                            )
                        },
                        layerBlock = if (isInteractive) {
                            {
                                val width = size.width
                                val height = size.height

                                val press = pillHighlight.pressProgress
                                val base = lerp(1f, 1f + 6.dp.toPx() / height, press)

                                val maxOffset = size.minDimension
                                val k = 0.03f
                                val offset = pillHighlight.offset

                                translationX = maxOffset * tanh(k * offset.x / maxOffset)
                                translationY = maxOffset * tanh(k * offset.y / maxOffset)

                                val maxDragScale = 2.dp.toPx() / height
                                val ang = atan2(offset.y, offset.x)

                                scaleX =
                                    base +
                                            maxDragScale * abs(cos(ang) * offset.x / size.maxDimension) *
                                            (width / height).fastCoerceAtMost(1f)

                                scaleY =
                                    base +
                                            maxDragScale * abs(sin(ang) * offset.y / size.maxDimension) *
                                            (height / width).fastCoerceAtMost(1f)
                            }
                        } else null,
                        onDrawSurface = {
                            drawRect(tint)
                            contrast?.let { drawRect(it) }
                        }
                    )
                    .then(if (isInteractive) pillHighlight.modifier else Modifier)
                    .then(if (isInteractive) pillHighlight.gestureModifier else Modifier),
                color = Color.Transparent,
                shape = shape,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                val exitEnabled = p > 0.6f

                PillMorphLayout(
                    modifier = Modifier.fillMaxSize(),
                    slotWidth = slot,
                    progress = p,
                    collapsedOverlap = (-18).dp,
                    splitGap = Dp.Unspecified,
                    splitProgress = 0f
                ) {
                    PillIconSlot(
                        slotWidth = slot,
                        iconRes = backIconRes,
                        onClick = { exitRevealed = !exitRevealed }
                    )

                    PillIconSlot(
                        slotWidth = slot,
                        iconRes = menuIconRes,
                        onClick = {
                            exitRevealed = false
                            onMenu()
                        }
                    )

                    val pop = 0.92f + 0.08f * p
                    val slide = 14f * (1f - p)

                    PillIconSlot(
                        slotWidth = slot,
                        iconRes = exitIconRes,
                        enabled = exitEnabled,
                        onClick = {
                            exitRevealed = false
                            onExit()
                        },
                        modifier = Modifier.graphicsLayer {
                            alpha = p
                            scaleX = pop
                            scaleY = pop
                            translationX = slide
                        }
                    )
                }
            }
        } else {
            // ✅ TWO pills (split state)
            val pillShape = RoundedCornerShape(999.dp)

            // LEFT pill: back + menu (2 slots)
            Surface(
                modifier = Modifier
                    .width(slot * 2)
                    .fillMaxHeight()
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { pillShape },
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
                                    style = HighlightStyle.Plain
                                )
                            }
                        },
                        shadow = null,
                        effects = {
                            colorControls(
                                brightness = if (isDark) -0.03f else 0.02f,
                                contrast = if (isDark) 1.10f else 1.05f,
                                saturation = if (isDark) 1.10f else 1.05f
                            )
                            vibrancy()
                            blur(4f.dp.toPx())
                            lens(
                                refractionHeight = 0f.dp.toPx(),
                                refractionAmount = 0f.dp.toPx(),
                                depthEffect = true
                            )
                        },
                        layerBlock = null,
                        onDrawSurface = {
                            drawRect(tint)
                            contrast?.let { drawRect(it) }
                        }
                    ),
                color = Color.Transparent,
                shape = pillShape,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                // Just place two slots manually (no 3-child layout)
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.width(slot).fillMaxHeight()) {
                        PillIconSlot(
                            slotWidth = slot,
                            iconRes = backIconRes,
                            onClick = { exitRevealed = !exitRevealed }
                        )
                    }
                    Box(
                        Modifier
                            .offset(x = slot)
                            .width(slot)
                            .fillMaxHeight()
                    ) {
                        PillIconSlot(
                            slotWidth = slot,
                            iconRes = menuIconRes,
                            onClick = {
                                exitRevealed = false
                                onMenu()
                            }
                        )
                    }
                }
            }

            // RIGHT pill: exit (1 slot) moved away with a REAL GAP
            Surface(
                modifier = Modifier
                    .offset(x = (slot * 2) + gapDp)
                    .width(slot)
                    .fillMaxHeight()
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { pillShape },
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
                                    style = HighlightStyle.Plain
                                )
                            }
                        },
                        shadow = null,
                        effects = {
                            colorControls(
                                brightness = if (isDark) -0.03f else 0.02f,
                                contrast = if (isDark) 1.10f else 1.05f,
                                saturation = if (isDark) 1.10f else 1.05f
                            )
                            vibrancy()
                            blur(4f.dp.toPx())
                            lens(
                                refractionHeight = 0f.dp.toPx(),
                                refractionAmount = 0f.dp.toPx(),
                                depthEffect = true
                            )
                        },
                        layerBlock = null,
                        onDrawSurface = {
                            drawRect(tint)
                            contrast?.let { drawRect(it) }
                        }
                    ),
                color = Color.Transparent,
                shape = pillShape,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    PillIconSlot(
                        slotWidth = slot,
                        iconRes = exitIconRes,
                        enabled = true,
                        onClick = {
                            exitRevealed = false
                            onExit()
                        }
                    )
                }
            }
        }
    }
}
private fun bglassTint(isDark: Boolean): Pair<Color, Color?> {
    return if (isDark) {
        // Dark mode: lift luminance slightly so icons pop
        val base = Color.White.copy(alpha = 0.26f)
        val contrast = Color.Black.copy(alpha = 0.03f)
        base to contrast
    } else {
        // Light mode: add subtle depth without muddying
        val base = Color.White.copy(alpha = 0.55f)
        val contrast = Color.Black.copy(alpha = 0.03f)
        base to contrast
    }
}


@Composable
private fun PillIconSlot(
    slotWidth: Dp,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val isDark = isSystemInDarkTheme()
    val interaction = remember { MutableInteractionSource() }

    val pressed by interaction.collectIsPressedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val active = enabled && (pressed || hovered)

    val pressAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(120, easing = FastOutSlowInEasing),
        label = "pressAlpha"
    )

    val pressScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (!enabled) 1f else if (pressed) 1.06f else if (hovered) 1.03f else 1.0f,
        animationSpec = tween(160, easing = FastOutSlowInEasing),
        label = "pressScale"
    )

    val bubbleColor = if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.10f)
    val iconTint = if (isDark) Color.White.copy(alpha = 0.95f) else Color.Black.copy(alpha = 0.85f)

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
