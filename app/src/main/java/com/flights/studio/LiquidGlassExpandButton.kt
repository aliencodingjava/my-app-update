package com.flights.studio

import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon


private const val TAG_EXPAND = "ExpandButton"

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
    surfaceColor: Color = Color.Unspecified,
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

    val horizontalPadding = 9.dp

    val baseColor = if (surfaceColor.isSpecified) {
        surfaceColor
    } else {
        val cs = MaterialTheme.colorScheme
        if (camExpanded) cs.secondaryContainer else cs.surfaceVariant
    }

    Row(
        modifier
            .graphicsLayer {
                val press = interactiveHighlight.pressProgress
                val offset = interactiveHighlight.offset

                val maxTranslation = 10.dp.toPx()
                translationX = (offset.x * 0.06f)
                    .coerceIn(-maxTranslation, maxTranslation)
                translationY = (offset.y * 0.06f)
                    .coerceIn(-maxTranslation, maxTranslation)

                val s = lerp(1f, 1.06f, press)
                scaleX = s
                scaleY = s
            }
            .drawBackdrop(
                backdrop = backdrop,
                shape = { CircleShape },
                effects = {
                    vibrancy()

                    val l = if (adaptiveLuminance) adaptiveState.luminance else 0.5f

                    val baseRadius = lerp(4.dp.toPx(), 10.dp.toPx(), expressiveProgress)
                    val radius = baseRadius * lerp(0.9f, 1.15f, l)

                    blur(
                        radius = radius,
                        edgeTreatment = TileMode.Decal
                    )

                    val lensRadius = lerp(16.dp.toPx(), 24.dp.toPx(), expressiveProgress)
                    lens(8.dp.toPx(), lensRadius)
                },
                layerBlock = null,
                onDrawSurface = {
                    if (tint.isSpecified) {
                        drawRect(tint, blendMode = BlendMode.Hue)
                        drawRect(tint.copy(alpha = lerp(0.65f, 0.85f, expressiveProgress)))
                    }
                    if (surfaceColor.isSpecified) {
                        drawRect(surfaceColor)
                    } else {
                        // Material expressive tonal hint
                        val alpha = lerp(0.22f, 0.30f, expressiveProgress)
                        drawRect(baseColor.copy(alpha = alpha))
                    }
                },
                onDrawBackdrop = { drawBackdrop ->
                    try {
                        drawBackdrop()
                        if (adaptiveLuminance) {
                            adaptiveState.layer.record { drawBackdrop() }
                        }
                    } catch (t: Throwable) {
                        Log.e(TAG_EXPAND, "onDrawBackdrop FAILED: ${t.message}", t)
                    }
                }
            )
            .then(
                if (isInteractive) {
                    Modifier
                        .then(interactiveHighlight.modifier)
                        .then(interactiveHighlight.gestureModifier)
                } else Modifier
            )
            .clickable(
                enabled = isInteractive,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = {
                    performExpandCollapseHaptic(isExpanding = !camExpanded)
                    onToggle()
                }
            )
            .height(38.dp)
            .padding(horizontal = horizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Chevron
        Icon(
            imageVector = Icons.Filled.ExpandMore,
            contentDescription = if (camExpanded) "Collapse" else "Expand",
            modifier = Modifier
                .graphicsLayer {
                    rotationZ = lerp(0f, 180f, expressiveProgress)
                    val s = lerp(1f, 1.08f, expressiveProgress)
                    scaleX = s
                    scaleY = s
                }
                .padding(end = 2.dp),
            tint = if (tint.isSpecified) Color.White else adaptiveState.contentColor
        )

        // Animated label (Material expressive)
        AnimatedContent(
            targetState = camExpanded,
            transitionSpec = {
                (fadeIn(animationSpec = tween(140)) togetherWith
                        fadeOut(animationSpec = tween(100)))
                    .using(SizeTransform(clip = false))
            },
            label = "expandLabelContent"
        ) { expanded ->
            Text(
                text = if (expanded) "Close ✕" else "Expand ⌞⌝",
                color = if (tint.isSpecified) Color.White else adaptiveState.contentColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
    }
}
