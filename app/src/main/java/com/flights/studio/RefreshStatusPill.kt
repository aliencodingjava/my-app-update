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

    // Same as your other liquid buttons
    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }

    // ----------------------------------------
    // progress calculation
    // ----------------------------------------
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

// ✅ Expand only when near-refresh (countdown). Refreshing is indicated by the image crossfade.
    val expand = safeCountdown in 1..warningWindowMs

    // Drives lens/blur a bit, not size.
    val expressiveProgress by animateFloatAsState(
        targetValue = if (expand) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pillExpressiveProgress"
    )

    // ----------------------------------------
    // label text
    // ----------------------------------------
    val secondsLeft = (safeCountdown / 1000L).coerceIn(0L, 99L).toInt()
    val secondsText = remember(secondsLeft) { String.format(Locale.US, "%02d", secondsLeft) }
    val label = "Refresh in ${secondsText}s"

    // ----------------------------------------
    // spring pop ONLY on open (layout-based, keeps capsule rounded)
    // ----------------------------------------
    val expandImpulse = remember { Animatable(0f) }
    LaunchedEffect(expand) {
        if (expand) {
            expandImpulse.snapTo(1f)
            expandImpulse.animateTo(0f, tween(durationMillis = 520, easing = FastOutSlowInEasing))
        } else {
            expandImpulse.snapTo(0f)
        }
    }
    val springPop = if (isRefreshing) 0f else expandImpulse.value // 0..1

    // ----------------------------------------
    // Layout animation (controls pill size)
    // ✅ Open = spring, Close = fast smooth tween
    // ✅ Extra tiny springPop added into layout (NOT scaleX) => no "arrow"
    // ----------------------------------------
    val t = updateTransition(expand, label = "pillExpand")

    val horizontalPaddingDp by t.animateFloat(
        transitionSpec = {
            if (targetState) spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMedium)
            else tween(durationMillis = 170, easing = FastOutSlowInEasing)
        },
        label = "pillPadding"
    ) { if (it) 10f + (springPop * 1.2f) else 9f }

    val barWidthDp = 52f
    val refreshMoment = safeCountdown == 0L || isRefreshing


    val gapWidthDp by t.animateFloat(
        transitionSpec = {
            if (targetState) spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMedium)
            else tween(durationMillis = 170, easing = FastOutSlowInEasing)
        },
        label = "gapWidth"
    ) { if (it) 8f + (springPop * 0.8f) else 0f }

    val labelAlpha by t.animateFloat(
        transitionSpec = {
            if (targetState) tween(durationMillis = 120, easing = FastOutSlowInEasing)
            else tween(durationMillis = 220, easing = FastOutSlowInEasing)
        },
        label = "labelAlpha"
    ) { expanded ->
        if (expanded && !refreshMoment) 1f else 0f
    }


    val labelStyle: TextStyle = MaterialTheme.typography.bodyMedium
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current

    val labelTargetWidthDp = remember(labelStyle, density) {
        fun measureDp(text: String): Float {
            val px = measurer.measure(
                text = AnnotatedString(text),
                style = labelStyle
            ).size.width.toFloat()
            return with(density) { px.toDp().value }
        }

        // ✅ only the real string you show
        val maxWidthDp = measureDp("Refresh in 00s")

        // ✅ small safety padding so seconds never clip
        maxWidthDp + 12f
    }


    val labelWidthDp by t.animateFloat(
        transitionSpec = {
            if (targetState) spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMedium)
            else tween(durationMillis = 220, easing = FastOutSlowInEasing) // ✅ smooth + not laggy
        },
        label = "labelWidth"
    ) { expanded ->
        if (expanded && !refreshMoment) {
            labelTargetWidthDp + (springPop * 6f)
        } else 0f
    }



    Row(
        modifier = modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { ContinuousCapsule },
                effects = {
                    vibrancy()

                    // expressiveProgress: 0f..1f
                    val p = expressiveProgress.coerceIn(0f, 1f)

                    // --- Blur (softly comes in/out) ---
                    // If you truly want blur always 0, keep 0f; otherwise lerp like below.
                    val blurRadius = lerp(0.dp.toPx(), 4.dp.toPx(), p)
                    blur(radius = blurRadius, edgeTreatment = TileMode.Decal)

                    // --- Lens (refraction animates) ---
                    val refractionHeight = lerp(6.dp.toPx(), 10.dp.toPx(), p)
                    val refractionAmount = lerp(6.dp.toPx(), 14.dp.toPx(), p)

                    lens(
                        refractionHeight = refractionHeight,
                        refractionAmount = refractionAmount,
                        depthEffect = true,
                        chromaticAberration = false
                    )

                    // --- Color controls (you can tune these targets) ---
                    val brightness = lerp(0.0f, if (isDark) 0.02f else 0.00f, p)
                    val contrast   = lerp(1.0f, 1.08f, p)
                    val saturation = lerp(1.0f, 1.9f, p)

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

                        val maxDragScale = 1.5.dp.toPx() / size.height
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

                        // ✅ NO wobble scale here => capsule stays perfectly rounded (no arrow)
                        translationX = maxOffset * tanh(k * offset.x / maxOffset)
                        translationY = maxOffset * tanh(k * offset.y / maxOffset)

                        scaleX = pressDragScaleX
                        scaleY = pressDragScaleY
                    }
                } else null,
                onDrawSurface = {
                    // darker tint in dark mode, subtle gray tint in light mode (not milky-white)
                    val baseTint = if (isDark) {
                        Color.Black.copy(alpha = 0.22f)
                    } else {
                        Color.Black.copy(alpha = 0.06f) // ✅ light theme: slight dark tint for contrast
                    }

                    // optional warmth highlight (lower in light mode)
                    val warmTint = if (isDark) {
                        Color(0xFFFFD54F).copy(alpha = 0.10f)
                    } else {
                        Color(0xFFFFD54F).copy(alpha = 0.06f)
                    }

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
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .width(barWidthDp.dp)
                .height(6.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(99.dp)),
            color = if (isDark) Color.Cyan else Color(0xFF64DD17), // iOS-ish blue in light
            trackColor = if (isDark)
                Color.White.copy(alpha = 0.25f)
            else
                Color.Black.copy(alpha = 0.10f) // ✅ light theme: soft dark track

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
