package com.flights.studio

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.runtimeShaderEffect

/**
 * Progressive blur + tint mask (Kyant style) for bottom chrome.
 *
 * The shader follows Kyant's alpha-mask approach, flipped so the blur/tint is
 * strongest behind the bottom bar and fades upward into the page.
 */
@Composable
fun BackdropGradientLayer(
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier,
    height: Dp,
    blurDp: Dp = 2.dp,
    tintColor: Color = if (!isSystemInDarkTheme()) Color.White else Color(0xFF1A1919),
    tintIntensity: Float = 0.8f,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .drawPlainBackdrop(
                backdrop = backdrop,
                shape = { RectangleShape },
                effects = {
                    blur(blurDp.toPx(), edgeTreatment = TileMode.Clamp)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        runtimeShaderEffect(
                            key = "BackdropGradientLayerAlphaMask",
                            shaderString = """
uniform shader content;
uniform float2 size;
layout(color) uniform half4 tint;
uniform float tintIntensity;

half4 main(float2 coord) {
    float y = size.y - coord.y;
    float blurAlpha = smoothstep(size.y, size.y * 0.5, y);
    float tintAlpha = smoothstep(size.y, size.y * 0.5, y);

    return mix(content.eval(coord) * blurAlpha, tint * tintAlpha, tintIntensity);
}
    """.trimIndent(),
                            uniformShaderName = "content"
                        ) {
                            setFloatUniform("size", size.width, size.height)
                            setColorUniform("tint", tintColor)
                            setFloatUniform("tintIntensity", tintIntensity)
                        }
                    }
                }
            )
    )
}
