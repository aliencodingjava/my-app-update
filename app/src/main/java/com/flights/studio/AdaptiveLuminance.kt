package com.flights.studio

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.core.graphics.scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.nio.IntBuffer
import kotlin.math.abs

/**
 * State you can reuse for any liquid glass surface.
 *
 * - [layer]: record backdrop into this every frame.
 * - [luminance]: 0f..1f average brightness of what’s behind.
 * - [contentColor]: animated text/icon color based on luminance.
 */
data class AdaptiveLuminanceState(
    val layer: GraphicsLayer,
    val luminance: Float,
    val contentColor: Color
)

private const val TAG_LUMINANCE = "AdaptiveLum"

@Composable
fun rememberAdaptiveLuminance(
    enabled: Boolean,
    initialLuminance: Float = 0.5f,
    lightOnBright: Color = Color.Black,
    lightOnDark: Color = Color.White,
    animationMillis: Int = 450
): AdaptiveLuminanceState {
    Log.d(
        TAG_LUMINANCE,
        "rememberAdaptiveLuminance() COMPOSE enabled=$enabled initial=$initialLuminance"
    )

    val layer = rememberGraphicsLayer().also {
        Log.d(TAG_LUMINANCE, "rememberGraphicsLayer() created: $it")
    }
    val isDark = isSystemInDarkTheme()
    Log.d(TAG_LUMINANCE, "isSystemInDarkTheme() = $isDark")

    // luminance 0f..1f
    val luminanceAnim = remember {
        Log.d(TAG_LUMINANCE, "Animatable(initial=$initialLuminance) created")
        Animatable(initialLuminance)
    }

    // target color we animate towards
    val targetContentColor = remember(isDark, lightOnBright, lightOnDark) {
        val startColor = if (isDark) lightOnDark else lightOnBright
        Log.d(TAG_LUMINANCE, "targetContentColor remember: startColor=$startColor")
        mutableStateOf(startColor)
    }

    val contentColor by animateColorAsState(
        targetValue = targetContentColor.value,
        animationSpec = tween(animationMillis),
        label = "adaptiveContentColor"
    )

    LaunchedEffect(enabled) {
        Log.d(TAG_LUMINANCE, "LaunchedEffect(enabled=$enabled) START")

        if (!enabled) {
            Log.d(TAG_LUMINANCE, "Adaptive luminance DISABLED -> reset and return")
            luminanceAnim.snapTo(initialLuminance)
            Log.d(TAG_LUMINANCE, "Animatable snapped to $initialLuminance")
            return@LaunchedEffect
        }

        val sampleSize = 5          // 5x5 thumbnail
        val totalPixels = sampleSize * sampleSize
        val buffer = IntBuffer.allocate(totalPixels)

        var loopIteration = 0

        while (isActive) {
            loopIteration++
            val previous = luminanceAnim.value

            val avg = withContext(Dispatchers.Default) {
                val imageBitmap = runCatching { layer.toImageBitmap() }.getOrNull()
                    ?: run {
                        if (loopIteration % 20 == 0) {
                            Log.w(TAG_LUMINANCE, "loop#$loopIteration: toImageBitmap() returned null, keeping previous=$previous")
                        }
                        return@withContext previous
                    }

                if (imageBitmap.width <= 0 || imageBitmap.height <= 0) {
                    if (loopIteration % 20 == 0) {
                        Log.w(
                            TAG_LUMINANCE,
                            "loop#$loopIteration: bitmap size invalid ${imageBitmap.width}x${imageBitmap.height}, keeping previous=$previous"
                        )
                    }
                    return@withContext previous
                }

                val scaled: Bitmap = imageBitmap
                    .asAndroidBitmap()
                    .scale(sampleSize, sampleSize, /* filter = */ false)
                    .copy(Bitmap.Config.ARGB_8888, false)

                buffer.rewind()
                scaled.copyPixelsToBuffer(buffer)
                buffer.rewind()

                var sum = 0.0
                repeat(totalPixels) {
                    val c = buffer.get()
                    val r = ((c shr 16) and 0xFF) / 255.0
                    val g = ((c shr 8) and 0xFF) / 255.0
                    val b = (c and 0xFF) / 255.0
                    sum += 0.2126 * r + 0.7152 * g + 0.0722 * b
                }

                val avgInner = (sum / totalPixels.toDouble()).toFloat()
                if (abs(avgInner - previous) > 0.02f) {
                    Log.d(
                        TAG_LUMINANCE,
                        "loop#$loopIteration: sampled luminance=$avgInner (prev=$previous)"
                    )
                }
                avgInner
            }

            if (abs(avg - previous) > 0.01f) {
                Log.d(
                    TAG_LUMINANCE,
                    "loop#$loopIteration: animate luminance from $previous -> $avg"
                )
            }

            // animate luminance like his demo
            luminanceAnim.animateTo(avg, tween(animationMillis))

            // decide text/icon color
            val newColor = if (avg > 0.5f) lightOnBright else lightOnDark
            if (newColor != targetContentColor.value) {
                Log.d(
                    TAG_LUMINANCE,
                    "loop#$loopIteration: contentColor change -> $newColor (avg=$avg)"
                )
            }
            targetContentColor.value = newColor

            // don't hammer CPU/GPU every frame
            delay(100L)
        }

        Log.d(TAG_LUMINANCE, "LaunchedEffect(enabled=${true}) END (isActive=false)")
    }

    return AdaptiveLuminanceState(
        layer = layer,
        luminance = luminanceAnim.value,
        contentColor = contentColor
    )
}
