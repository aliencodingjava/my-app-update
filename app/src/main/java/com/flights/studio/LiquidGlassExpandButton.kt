package com.flights.studio

import android.graphics.Bitmap
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import androidx.core.graphics.scale
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.capsule.ContinuousRoundedRectangle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.nio.IntBuffer
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.tanh
enum class ExpandHapticStyle { DoubleClick, Heavy, Bouncy }

@Composable
fun LiquidGlassExpandButton(
    camExpanded: Boolean,
    onToggle: () -> Unit,
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier,
    isInteractive: Boolean = true,
    tint: Color = Color.Unspecified,
    surfaceColor: Color = Color.Unspecified,
    adaptiveLuminance: Boolean = true,
    // NEW: allow toggling haptics if needed later
    enableHaptics: Boolean = true,
    expandHapticStyle: ExpandHapticStyle = ExpandHapticStyle.DoubleClick,

    ) {
    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }

    // --- vibrator handle (works with your minSdk 31) ---
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val vibrator: Vibrator? = remember {
        try { context.getSystemService(VibratorManager::class.java)?.defaultVibrator } catch (_: Throwable) { null }
    }

    // NEW: helper for custom waveform (API 26+; fine on 31+)
    fun vibrateWaveform(amplitudes: IntArray, timingsMs: LongArray) {
        if (vibrator?.hasVibrator() != true) return
        val effect = VibrationEffect.createWaveform(timingsMs, amplitudes, -1)
        vibrator.vibrate(effect)
    }

    // Call this before toggling
    fun performExpandCollapseHaptic(isExpanding: Boolean) {
        if (!enableHaptics) return
        val ok = runCatching {
            if (vibrator?.hasVibrator() == true) {
                if (isExpanding) {
                    when (expandHapticStyle) {
                        ExpandHapticStyle.DoubleClick -> {
                            // crisp pop-pop
                            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
                        }
                        ExpandHapticStyle.Heavy -> {
                            // strong single thud
                            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
                        }
                        ExpandHapticStyle.Bouncy -> {
                            // playful 3-step bounce: soft → mid → soft
                            // timings must start with a delay (0 = immediate)
                            vibrateWaveform(
                                amplitudes = intArrayOf(0, 120, 200, 120),
                                timingsMs  = longArrayOf(0, 18, 28, 22)
                            )
                        }
                    }
                } else {
                    // collapse → subtle tick
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                }
                true
            } else false
        }.getOrDefault(false)

        if (!ok) {
            // Compose fallback
            haptics.performHapticFeedback(
                if (isExpanding) HapticFeedbackType.LongPress else HapticFeedbackType.KeyboardTap
            )
        }
    }    // --- end NEW ---

    // --- sample backdrop pixels from this recorded layer ---
    val layer = rememberGraphicsLayer()

    val luminance = remember { Animatable(0.5f) }

    val initialFg = if (!isSystemInDarkTheme()) Color.Black else Color.White
    // explicit state (no 'by' delegate)
    val targetContentColor = remember { mutableStateOf(initialFg) }

// animated read (note the trailing `.value`)
    val contentColor = animateColorAsState(
        targetValue = targetContentColor.value,
        animationSpec = tween(450),
        label = "contentColor"
    ).value

    LaunchedEffect(adaptiveLuminance) {
        if (!adaptiveLuminance) return@LaunchedEffect
        val buffer = IntBuffer.allocate(25)
        while (isActive) {
            val avg = withContext(Dispatchers.IO) {
                val bmp = layer.toImageBitmap().asAndroidBitmap()
                    .scale(5, 5, false)
                    .copy(Bitmap.Config.ARGB_8888, false)
                buffer.rewind()
                bmp.copyPixelsToBuffer(buffer)
                var sum = 0.0
                repeat(25) {
                    val c = buffer.get(it)
                    val r = ((c shr 16) and 0xFF) / 255.0
                    val g = ((c shr  8) and 0xFF) / 255.0
                    val b = ( c         and 0xFF) / 255.0
                    sum += 0.2126 * r + 0.7152 * g + 0.0722 * b
                }
                (sum / 25.0).toFloat()
            }

            luminance.animateTo(avg, tween(450))
            // drive only the text color
            targetContentColor.value = if (avg > 0.5f) Color.Black else Color.White
        }
    }


    Row(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { ContinuousRoundedRectangle(24f.dp) },
                effects = {
                    vibrancy()
                    if (adaptiveLuminance) {
                        val l = (luminance.value * 2f - 1f).let { sign(it) * it * it }
                        blur(
                            radius = if (l > 0f) lerp(8.dp.toPx(), 16.dp.toPx(), l)
                            else        lerp(8.dp.toPx(),  2.dp.toPx(), -l),
                            edgeTreatment = TileMode.Decal
                        )
                    }
                    lens(12.dp.toPx(), 24.dp.toPx())
                },
                layerBlock = if (isInteractive) {
                    {
                        val width = size.width
                        val height = size.height
                        val progress = interactiveHighlight.pressProgress
                        val scale = lerp(1f, 1f + 4.dp.toPx() / size.height, progress)
                        val maxOffset = size.minDimension
                        val k = 0.05f
                        val offset = interactiveHighlight.offset
                        translationX = maxOffset * tanh(k * offset.x / maxOffset)
                        translationY = maxOffset * tanh(k * offset.y / maxOffset)
                        val maxDragScale = 4.dp.toPx() / size.height
                        val ang = atan2(offset.y, offset.x)
                        scaleX = scale +
                                maxDragScale * abs(cos(ang) * offset.x / size.maxDimension) *
                                (width / height).fastCoerceAtMost(1f)
                        scaleY = scale +
                                maxDragScale * abs(sin(ang) * offset.y / size.maxDimension) *
                                (height / width).fastCoerceAtMost(1f)
                    }
                } else null,
                onDrawSurface = {
                    if (tint.isSpecified) {
                        drawRect(tint, blendMode = BlendMode.Hue)
                        drawRect(tint.copy(alpha = 0.75f))
                    }
                    if (surfaceColor.isSpecified) drawRect(surfaceColor)
                },
                onDrawBackdrop = { drawBackdrop ->
                    drawBackdrop()
                    if (adaptiveLuminance) {
                        layer.record { drawBackdrop() } // record pixels for sampling
                    }
                }
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
            .clickable(
                enabled = isInteractive,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = {
                    // NEW: fire haptic *before* state flips so we know the direction
                    performExpandCollapseHaptic(isExpanding = !camExpanded)
                    onToggle()
                }
            )
            .height(48.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (camExpanded) "Close  ✕" else "Expand  ⌞ ⌝",
            color = if (tint.isSpecified) Color.White else contentColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}
