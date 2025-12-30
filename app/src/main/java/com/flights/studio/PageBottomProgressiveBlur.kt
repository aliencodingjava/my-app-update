package com.flights.studio

import android.graphics.RenderEffect
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.effect

/**
 * Alpha-masked progressive blur (Kyant style).
 * Put this as a full-screen overlay that reads from your page LayerBackdrop.
 *
 * Works on Android 13+ (RuntimeShader).
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun PageBottomProgressiveBlur(
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val isLightTheme = !isSystemInDarkTheme()
    val tintColor = if (isLightTheme) Color(0x7DFFFFFF) else Color(0xFF0E1116)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
            .drawPlainBackdrop(
                backdrop = backdrop,
                shape = { RectangleShape },
                effects = {
                    blur(2f.dp.toPx())
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                        effect(
                            RenderEffect.createRuntimeShaderEffect(
                                obtainRuntimeShader(
                                    "AlphaMask",
                                    """
uniform shader content;
uniform float2 size;
layout(color) uniform half4 tint;
uniform float tintIntensity;

half4 main(float2 coord) {
    float y = size.y - coord.y;
    float blurAlpha = smoothstep(size.y, size.y * 0.15, y);
    float tintAlpha = smoothstep(size.y, size.y * 0.25, y);
    return mix(content.eval(coord) * blurAlpha, tint * tintAlpha, tintIntensity);
}
                                    """.trimIndent()
                                ).apply {
                                    setFloatUniform("size", size.width, size.height)
                                    setColorUniform("tint", tintColor.toArgb())
                                    setFloatUniform("tintIntensity", 0.8f)
                                },
                                "content"
                            )
                        )
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) { content() }
}