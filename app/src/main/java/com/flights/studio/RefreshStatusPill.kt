package com.flights.studio

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.capsule.ContinuousCapsule
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

@Composable
fun RefreshStatusPill(
    backdrop: Backdrop,
    isRefreshing: Boolean,
    countdownMs: Long,
    modifier: Modifier = Modifier,
    isInteractive: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val isDark = isSystemInDarkTheme()
    val ui = rememberUiScale()

    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }

    val totalIntervalMs = 60_000L
    val warningWindowMs = 10_000L

    val safeCountdown = countdownMs.coerceAtLeast(0L)
    val clamped = safeCountdown.coerceIn(0L, totalIntervalMs)
    val rawProgress = 1f - (clamped.toFloat() / totalIntervalMs.toFloat())

    val targetProgress = when {
        isRefreshing -> 1f
        safeCountdown <= 0L -> 0f
        else -> rawProgress.coerceIn(0f, 1f)
    }

    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "refreshLinearProgress"
    )

    val expand = safeCountdown in 1..warningWindowMs
    val refreshMoment = safeCountdown == 0L || isRefreshing

    // ONLY this decides expanded vs collapsed layout
    val showExpandedLayout = expand && !refreshMoment

    val expressiveProgress by animateFloatAsState(
        targetValue = if (expand) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pillExpressiveProgress"
    )

    val secondsLeft = (safeCountdown / 1000L).coerceIn(0L, 99L).toInt()
    val secondsText = remember(secondsLeft) { String.format(Locale.US, "%02d", secondsLeft) }
    val label = "Refresh in ${secondsText}s"

    val expandImpulse = remember { Animatable(0f) }
    LaunchedEffect(showExpandedLayout) {
        if (showExpandedLayout) {
            expandImpulse.snapTo(1f)
            expandImpulse.animateTo(0f, tween(durationMillis = 520, easing = FastOutSlowInEasing))
        } else {
            expandImpulse.snapTo(0f)
        }
    }
    val springPop = if (isRefreshing) 0f else expandImpulse.value

    val t = updateTransition(showExpandedLayout, label = "pillExpand")

    val openSpec = spring<Float>(dampingRatio = 0.82f, stiffness = Spring.StiffnessMedium)
    val closeSpec = tween<Float>(durationMillis = 170, easing = FastOutSlowInEasing)

    // ONE DRIVER: p = 1 expanded, p = 0 collapsed
    val p by t.animateFloat(
        transitionSpec = { if (targetState) openSpec else closeSpec },
        label = "layoutProgress"
    ) { expanded -> if (expanded) 1f else 0f }

    val labelStyle: TextStyle =
        MaterialTheme.typography.labelSmall.copy(
            fontSize = (MaterialTheme.typography.labelSmall.fontSize.value * ui).sp
        )

    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val labelTargetWidthDp = remember(labelStyle, density) {
        val px = measurer.measure(
            text = AnnotatedString("Refresh in 00s"),
            style = labelStyle
        ).size.width.toFloat()
        with(density) { px.toDp().value } + 12f
    }

    val horizontalPaddingDp = lerp(9f, 10f + (springPop * 1.2f), p)
    val gapWidthDp = lerp(0f, 8f + (springPop * 0.8f), p)

    val barWidthDp = lerp(38f, 52f, p)
    val barHeightDp = lerp(5f, 6f, p)

    val labelWidthDp = labelTargetWidthDp * p
    val labelAlpha = p

    Row(
        modifier = modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { ContinuousCapsule },
                effects = {
                    vibrancy()

                    val ep = expressiveProgress.coerceIn(0f, 1f)

                    val blurRadius = lerp(1.dp.toPx(), 4.dp.toPx(), ep)
                    blur(radius = blurRadius, edgeTreatment = TileMode.Decal)

                    val refractionHeight = lerp(6.dp.toPx(), 10.dp.toPx(), ep)
                    val refractionAmount = lerp(6.dp.toPx(), 14.dp.toPx(), ep)

                    lens(
                        refractionHeight = refractionHeight,
                        refractionAmount = refractionAmount,
                        depthEffect = true,
                        chromaticAberration = false
                    )

                    val brightness = lerp(0.0f, if (isDark) 0.02f else 0.00f, ep)
                    val contrast = lerp(1.0f, 1.18f, ep)
                    val saturation = lerp(1.0f, 1.9f, ep)

                    colorControls(
                        brightness = brightness,
                        contrast = contrast,
                        saturation = saturation
                    )
                },
                layerBlock = if (isInteractive) {
                    {
                        val width = size.width
                        val height = size.height

                        val press = interactiveHighlight.pressProgress
                        val zoomAmountPx = 1.5.dp.toPx()
                        val baseScale = lerp(1f, 1f + zoomAmountPx / size.height, press)

                        val maxOffset = size.minDimension
                        val k = 0.025f
                        val offset = interactiveHighlight.offset

                        val maxDragScale = 3.0.dp.toPx() / size.height
                        val ang = atan2(offset.y, offset.x)

                        val pressDragScaleX =
                            baseScale +
                                    maxDragScale *
                                    abs(cos(ang) * offset.x / size.maxDimension) *
                                    (width / height).fastCoerceAtMost(1f)

                        val pressDragScaleY =
                            baseScale +
                                    maxDragScale *
                                    abs(sin(ang) * offset.y / size.maxDimension) *
                                    (height / width).fastCoerceAtMost(1f)

                        translationX = maxOffset * tanh(k * offset.x / maxOffset)
                        translationY = maxOffset * tanh(k * offset.y / maxOffset)

                        scaleX = pressDragScaleX
                        scaleY = pressDragScaleY
                    }
                } else null,
                onDrawSurface = {
                    val baseTint = if (isDark) Color.Black.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.06f)
                    val warmTint = if (isDark) Color(0xFFFFD54F).copy(alpha = 0.10f) else Color(0xFFFFD54F).copy(alpha = 0.06f)
                    drawRect(baseTint)
                    drawRect(warmTint)
                }
            )
            .then(if (isInteractive) interactiveHighlight.modifier else Modifier)
            .then(if (isInteractive) interactiveHighlight.gestureModifier else Modifier)
            .clickable(
                interactionSource = null,
                indication = if (isInteractive) null else LocalIndication.current,
                role = Role.Button,
                enabled = onClick != null
            ) { onClick?.invoke() }
            .padding(horizontal = horizontalPaddingDp.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val progressYellow = if (isDark) Color(0xFFFFD54F) else Color(0xFFFFC107)

        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .clip(RoundedCornerShape(99.dp))
                .width(barWidthDp.dp)
                .height(barHeightDp.dp),
            color = progressYellow,
            trackColor = if (isDark) Color.White.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.10f)
        )

        Spacer(Modifier.width(gapWidthDp.dp))

        Text(
            text = label,
            style = labelStyle,
            modifier = Modifier
                .width(labelWidthDp.dp)
                .graphicsLayer { alpha = labelAlpha },
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
}
