package com.flights.studio

import android.graphics.RenderEffect
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.effect

/**
 * Progressive blur + tint mask (Kyant style) — BOTTOM band only.
 *
 * Goal:
 * - blur/tint is strongest at the BOTTOM of this layer
 * - fades out quickly upward (like 5–10dp) so the "line" is visible
 *
 * Note:
 * - Keep the layer height whatever you want (ex: half screen),
 *   but the visible blur will only be in the bottom band (fadeDp).
 */
@Composable
fun BackdropGradientLayer(
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier,
    height: Dp,
    blurDp: Dp = 16.dp,
    tintColor: Color = if (!isSystemInDarkTheme()) Color(0x66FFFFFF) else Color(0xFF0E1116),
    tintIntensity: Float = 0.28f,
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
                        effect(
                            RenderEffect.createRuntimeShaderEffect(
                                obtainRuntimeShader(
                                    "BottomToMidGradient",
                                    """
uniform shader content;
uniform float2 size;
layout(color) uniform half4 tint;
uniform float tintIntensity;

// controls curve: bigger = stronger near bottom
uniform float power;

half4 main(float2 coord) {
    // 0 at top of this layer, 1 at bottom
    float t = clamp(coord.y / max(size.y, 1.0), 0.0, 1.0);

    // smooth gradient, then bias toward bottom
    float a = pow(smoothstep(0.0, 1.0, t), power);

    half4 c = content.eval(coord);
    half4 tinted = mix(c, tint, tintIntensity);

    // draw blurred+tinted overlay with alpha a
    return tinted * a;
}
    """.trimIndent()
                                ).apply {
                                    setFloatUniform("size", size.width, size.height)
                                    setColorUniform("tint", tintColor.toArgb())
                                    setFloatUniform("tintIntensity", tintIntensity)
                                    setFloatUniform("power", 1.8f) // try 1.6–2.4
                                },
                                "content"
                            )
                        )
                    }
                }
            )
    )
}
