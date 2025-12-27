package com.flights.studio

import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import com.kyant.backdrop.effects.colorControls
import com.kyant.capsule.ContinuousCapsule



enum class ExpandHapticStyle { DoubleClick, Heavy, Bouncy }

@Composable
fun LiquidGlassExpandButton(
    camExpanded: Boolean,
    openProgress: Float, // SYNCED progress from the CARD
    onToggle: () -> Unit,
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier,
    isInteractive: Boolean = true,
    tint: Color = Color.Unspecified,
    adaptiveLuminance: Boolean = true,
    enableHaptics: Boolean = true,
    expandHapticStyle: ExpandHapticStyle = ExpandHapticStyle.DoubleClick,
) {
    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }

    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val vibrator: Vibrator? = remember {
        try {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } catch (_: Throwable) {
            null
        }
    }

    fun vibrateWaveform(amplitudes: IntArray, timingsMs: LongArray) {
        if (vibrator?.hasVibrator() != true) return
        val effect = VibrationEffect.createWaveform(timingsMs, amplitudes, -1)
        vibrator.vibrate(effect)
    }

    fun performExpandCollapseHaptic(isExpanding: Boolean) {
        if (!enableHaptics) return
        val ok = runCatching {
            if (vibrator?.hasVibrator() == true) {
                if (isExpanding) {
                    when (expandHapticStyle) {
                        ExpandHapticStyle.DoubleClick ->
                            vibrator.vibrate(
                                VibrationEffect.createPredefined(
                                    VibrationEffect.EFFECT_DOUBLE_CLICK
                                )
                            )

                        ExpandHapticStyle.Heavy ->
                            vibrator.vibrate(
                                VibrationEffect.createPredefined(
                                    VibrationEffect.EFFECT_HEAVY_CLICK
                                )
                            )

                        ExpandHapticStyle.Bouncy ->
                            vibrateWaveform(
                                amplitudes = intArrayOf(0, 120, 200, 120),
                                timingsMs = longArrayOf(0, 18, 28, 22)
                            )
                    }
                } else {
                    vibrator.vibrate(
                        VibrationEffect.createPredefined(
                            VibrationEffect.EFFECT_TICK
                        )
                    )
                }
                true
            } else false
        }.getOrDefault(false)

        if (!ok) {
            haptics.performHapticFeedback(
                if (isExpanding) HapticFeedbackType.LongPress
                else HapticFeedbackType.KeyboardTap
            )
        }
    }

    // Adaptive luminance
    val adaptiveState = rememberAdaptiveLuminance(enabled = adaptiveLuminance)

    // ✅ springy, expressive mapping of openProgress
    val expressiveProgress = animateFloatAsState(
        targetValue = openProgress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "expandExpressiveProgress"
    ).value



    val pillTextStyle = MaterialTheme.typography.bodyMedium

    Row(
        modifier = modifier
            .graphicsLayer {
                val press = interactiveHighlight.pressProgress
                val offset = interactiveHighlight.offset

                val maxTranslation = 10.dp.toPx()
                translationX = (offset.x * 0.06f).coerceIn(-maxTranslation, maxTranslation)
                translationY = (offset.y * 0.06f).coerceIn(-maxTranslation, maxTranslation)

                val s = lerp(1f, 1.06f, press)
                scaleX = s
                scaleY = s
            }
            .drawBackdrop(
                backdrop = backdrop,
                shape = { ContinuousCapsule },   // ✅ same as pill
                effects = {
                    vibrancy()

                    // Progress
                    val p = expressiveProgress.coerceIn(0f, 1f)

                    // Luminance (0..1, stable)
                    val lRaw = if (adaptiveLuminance) adaptiveState.luminance else 0.5f
                    val l = lRaw.coerceIn(0f, 1f)

                    // ------------------------------------------------------------------
                    // BLUR (expressive + subtle luminance influence)
                    // ------------------------------------------------------------------
                    val baseBlur = lerp(4.dp.toPx(), 10.dp.toPx(), p)
                    val lumFactor = lerp(1f, lerp(0.9f, 1.15f, l), p)
                    val blurRadius = (baseBlur * lumFactor).coerceAtLeast(0f)

                    blur(
                        radius = blurRadius,
                        edgeTreatment = TileMode.Decal
                    )

                    // ------------------------------------------------------------------
                    // LENS (expressive refraction)
                    // ------------------------------------------------------------------
                    val refractionHeight = 8.dp.toPx()
                    val refractionAmount = lerp(8.dp.toPx(), 24.dp.toPx(), p)

                    lens(
                        refractionHeight = refractionHeight,
                        refractionAmount = refractionAmount,
                        depthEffect = true,
                        chromaticAberration = false
                    )

                    // ------------------------------------------------------------------
                    // COLOR CONTROLS (identical dark/light now, ready to diverge later)
                    // ------------------------------------------------------------------
                    val brightness = 0.0f
                    val contrast = lerp(1.0f, 1.08f, p)
                    val saturation = lerp(1.0f, 1.9f, p)

                    colorControls(
                        brightness = brightness,
                        contrast = contrast,
                        saturation = saturation
                    )
                },

                onDrawSurface = { /* same as your file */ },
                onDrawBackdrop = { drawBackdrop ->
                    drawBackdrop()
                    if (adaptiveLuminance) adaptiveState.layer.record { drawBackdrop() }
                }
            )
            .then(if (isInteractive) interactiveHighlight.modifier else Modifier)
            .then(if (isInteractive) interactiveHighlight.gestureModifier else Modifier)
            .clickable(
                enabled = isInteractive,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button
            ) {
                performExpandCollapseHaptic(isExpanding = !camExpanded)
                onToggle()
            }
            // ✅ MATCH RefreshStatusPill sizing exactly:
            .padding(horizontal = 12.dp, vertical = 6.dp)   // same “pill padding”
            .defaultMinSize(minHeight = 0.dp),             // don't force height bigger
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
    ) {
        Icon(
            imageVector = Icons.Filled.ExpandMore,
            contentDescription = if (camExpanded) "Collapse" else "Expand",
            modifier = Modifier
                .size(18.dp) // ✅ keep icon from making the row taller
                .graphicsLayer {
                    rotationZ = lerp(0f, 180f, expressiveProgress)
                    val s = lerp(1f, 1.08f, expressiveProgress)
                    scaleX = s
                    scaleY = s
                },
            tint = if (tint.isSpecified) Color.White else adaptiveState.contentColor
        )

        Text(
            text = if (camExpanded) "Close ✕" else "Expand ⌞⌝",
            style = pillTextStyle,                 // ✅ same typography as pill
            color = if (tint.isSpecified) Color.White else adaptiveState.contentColor
        )
    }
}