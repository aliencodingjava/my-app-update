package com.flights.studio

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.util.fastCoerceIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class InteractiveHighlight(
    val animationScope: CoroutineScope,
    // leave default for 1:1 behavior
    val position: (size: Size, offset: Offset) -> Offset = { _, offset -> offset }
) {
    private val pressProgressSpec = spring(stiffness = 300f, dampingRatio = 0.5f, visibilityThreshold = 0.001f)
    private val positionSpec      = spring(stiffness = 300f, dampingRatio = 0.5f, visibilityThreshold = Offset.VisibilityThreshold)

    private val pressProgressAnim = Animatable(0f, 0.001f)
    private val positionAnim      = Animatable(Offset.Zero, Offset.VectorConverter, Offset.VisibilityThreshold)

    private var startPosition = Offset.Zero
    val pressProgress: Float get() = pressProgressAnim.value
    val offset: Offset get() = positionAnim.value - startPosition

    // Android 13+ spotlight shader; below T we fall back to a simple white Plus blend
    private val shader = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        RuntimeShader(
            """
            uniform float2 size;
            layout(color) uniform half4 color;
            uniform float radius;
            uniform float2 position;

            half4 main(float2 coord) {
                float dist = distance(coord, position);
                float intensity = smoothstep(radius, radius * 0.5, dist);
                return color * intensity;
            }
            """.trimIndent()
        )
    } else null

    // This modifier DRAWS the luminance
    val modifier: Modifier =
        Modifier.drawWithContent {
            val p = pressProgressAnim.value
            if (p > 0f) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && shader != null) {
                    // soft overall lift
                    drawRect(Color.White.copy(alpha = 0.08f * p), blendMode = BlendMode.Plus)

                    // spotlight centered at finger
                    val pos = position(size, positionAnim.value)
                    shader.setFloatUniform("size", size.width, size.height)
                    shader.setColorUniform("color", Color.White.copy(alpha = 0.15f * p).toArgb())
                    shader.setFloatUniform("radius", size.minDimension * 1.5f)
                    shader.setFloatUniform(
                        "position",
                        pos.x.fastCoerceIn(0f, size.width),
                        pos.y.fastCoerceIn(0f, size.height)
                    )
                    drawRect(ShaderBrush(shader), blendMode = BlendMode.Plus)
                } else {
                    // fallback: uniform lift
                    drawRect(Color.White.copy(alpha = 0.25f * p), blendMode = BlendMode.Plus)
                }
            }
            drawContent()
        }

    // This modifier FEEDS touch to the animation
    val gestureModifier: Modifier =
        Modifier.pointerInput(animationScope) {
            inspectDragGestures(
                onDragStart = { down ->
                    startPosition = down.position
                    animationScope.launch {
                        launch { pressProgressAnim.animateTo(1f, pressProgressSpec) }
                        launch { positionAnim.snapTo(startPosition) }
                    }
                },
                onDragEnd = {
                    animationScope.launch {
                        launch { pressProgressAnim.animateTo(0f, pressProgressSpec) }
                        launch { positionAnim.animateTo(startPosition, positionSpec) }
                    }
                },
                onDragCancel = {
                    animationScope.launch {
                        launch { pressProgressAnim.animateTo(0f, pressProgressSpec) }
                        launch { positionAnim.animateTo(startPosition, positionSpec) }
                    }
                }
            ) { change, _ ->
                animationScope.launch { positionAnim.snapTo(change.position) }
            }
        }
}
