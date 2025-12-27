package com.flights.studio

import android.graphics.Bitmap
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.IntBuffer
import kotlin.math.abs

/**
 * State you can reuse for any liquid glass surface.
 *
 * - [layer]: record backdrop into this every frame (layer.record { drawBackdrop() }).
 * - [luminance]: 0f..1f average brightness of what’s behind.
 * - [contentColor]: animated text/icon color based on luminance.
 */
data class AdaptiveLuminanceState(
    val layer: GraphicsLayer,
    val luminance: Float,
    val contentColor: Color
)

@Composable
fun rememberAdaptiveLuminance(
    enabled: Boolean,
    initialLuminance: Float = if (isSystemInDarkTheme()) 0f else 1f,
    lightOnBright: Color = Color.Black,
    lightOnDark: Color = Color.White,
    animationMillis: Int = 1000,
    sampleSize: Int = 5,            // Kyant uses 5x5
    sampleEveryMs: Long = 120L      // don’t hammer GPU
): AdaptiveLuminanceState {
    val isLightTheme = !isSystemInDarkTheme()

    // This is the layer you record the backdrop into.
    val layer = rememberGraphicsLayer()

    val luminanceAnim = remember {
        Animatable(if (isLightTheme) 1f else 0f)
    }
    val targetContentColor = remember {
        mutableStateOf(if (isLightTheme) lightOnBright else lightOnDark)
    }

    val contentColor by animateColorAsState(
        targetValue = targetContentColor.value,
        animationSpec = tween(animationMillis),
        label = "adaptiveContentColor"
    )

    LaunchedEffect(layer, enabled, sampleSize, sampleEveryMs, animationMillis, lightOnBright, lightOnDark) {
        if (!enabled) {
            luminanceAnim.snapTo(initialLuminance.coerceIn(0f, 1f))
            targetContentColor.value = if (isLightTheme) lightOnBright else lightOnDark
            return@LaunchedEffect
        }

        val totalPixels = sampleSize * sampleSize
        val buffer = IntBuffer.allocate(totalPixels)

        while (isActive) {
            // ✅ Kyant-style flow (record -> sample -> animate),
            // but SAFE on Samsung: snapshot on MAIN, pixel work off-main.

            // 1) Snapshot must be on MAIN to avoid libhwui/RenderThread crashes on some devices.
            val imageBitmap = withContext(Dispatchers.Main.immediate) {
                runCatching { layer.toImageBitmap() }.getOrNull()
            }

            // 2) Scale + copy pixels on background dispatcher.
            val avg = withContext(Dispatchers.Default) {
                val bmp = imageBitmap ?: return@withContext luminanceAnim.value

                if (bmp.width <= 0 || bmp.height <= 0) {
                    return@withContext luminanceAnim.value
                }

                val thumbnail: Bitmap =
                    bmp.asAndroidBitmap()
                        .scale(sampleSize, sampleSize, false)
                        .copy(Bitmap.Config.ARGB_8888, false)

                buffer.rewind()
                thumbnail.copyPixelsToBuffer(buffer)
                buffer.rewind()

                var sum = 0.0
                repeat(totalPixels) {
                    val c = buffer.get()
                    val r = ((c shr 16) and 0xFF) / 255.0
                    val g = ((c shr 8) and 0xFF) / 255.0
                    val b = (c and 0xFF) / 255.0
                    sum += 0.2126 * r + 0.7152 * g + 0.0722 * b
                }

                (sum / totalPixels.toDouble()).toFloat().coerceIn(0f, 1f)
            }

            // 3) Animate (Kyant animates luminance + content color)
            // Avoid restarting an expensive animation if basically unchanged.
            if (abs(avg - luminanceAnim.value) > 0.01f) {
                // content color can animate in parallel like Kyant's demo
                launch {
                    targetContentColor.value = if (avg > 0.5f) lightOnBright else lightOnDark
                }
                luminanceAnim.animateTo(avg, tween(animationMillis))
            } else {
                // still update color threshold if needed
                targetContentColor.value = if (avg > 0.5f) lightOnBright else lightOnDark
            }

            delay(sampleEveryMs)
        }
    }

    return AdaptiveLuminanceState(
        layer = layer,
        luminance = luminanceAnim.value,
        contentColor = contentColor
    )
}
