package com.flights.studio

import android.content.Context
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sin

private fun loadSiriGlowShaderSource(context: Context): String {
    return context.applicationContext
        .resources
        .openRawResource(R.raw.siri_glow)
        .bufferedReader()
        .use { it.readText() }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun SiriWaveOverlay(
    progress: Float,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp
) {
    if (progress <= 0f) return

    val appContext = LocalContext.current.applicationContext
    var shaderSource by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(appContext) {
        shaderSource = loadSiriGlowShaderSource(appContext)
    }

    val source = shaderSource ?: return
    val shader = remember(source) { runCatching { RuntimeShader(source) }.getOrNull() } ?: return

    val t = progress.coerceIn(0f, 1f)
    val intensity = 0.62f + 0.38f * sin(t * 3.2f)
    val cornerRadiusPx = with(LocalDensity.current) { cornerRadius.toPx() }

    Canvas(modifier = modifier) {
        shader.setFloatUniform("resolution", size.width, size.height)
        shader.setFloatUniform("time", t * 6.0f)
        shader.setFloatUniform("intensity", intensity)
        shader.setFloatUniform("cornerRadius", cornerRadiusPx)

        drawRect(
            brush = ShaderBrush(shader),
            blendMode = BlendMode.Screen
        )
    }
}
