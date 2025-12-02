package com.flights.studio

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

// -----------------------------------------------------------------------------
// DATA
// -----------------------------------------------------------------------------

private data class CircleElement(
    val color: Color,
    val alpha: Float,
    val radiusFactor: Float,
    val centerFactor: Offset
)

// Dark theme circles
private val darkThemeCircles = listOf(
    CircleElement(Color(0xFF0D47A1), 0.3f, 0.55f, Offset(0.1f, 0.2f)),
    CircleElement(Color(0xFF00E5FF), 0.2f, 0.45f, Offset(0.85f, 0.8f)),
    CircleElement(Color(0xFF1DE9B6), 0.25f, 0.35f, Offset(0.2f, 0.85f)),
    CircleElement(Color(0xFF42A5F5), 0.15f, 0.6f, Offset(0.9f, 0.15f)),
    CircleElement(Color(0xFF00BFA5), 0.18f, 0.25f, Offset(0.5f, 0.5f))
)

// Light theme circles
private val lightThemeCircles = listOf(
    CircleElement(Color(0xFFB3E5FC), 0.4f, 0.55f, Offset(0.1f, 0.2f)),
    CircleElement(Color(0xFFE1F5FE), 0.3f, 0.45f, Offset(0.85f, 0.8f)),
    CircleElement(Color(0xFF81D4FA), 0.35f, 0.35f, Offset(0.2f, 0.85f)),
    CircleElement(Color(0xFF4FC3F7), 0.2f, 0.6f, Offset(0.9f, 0.15f)),
    CircleElement(Color(0xFF90A4AE), 0.15f, 0.25f, Offset(0.5f, 0.5f))
)

// -----------------------------------------------------------------------------
// COLOR SCHEMES
// -----------------------------------------------------------------------------

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4FC3F7),
    onPrimary = Color(0xFF003355),
    background = Color(0xFF0A0E18),
    onBackground = Color(0xFFE3E2E6),
    surface = Color(0xFF101522),
    onSurface = Color(0xFFE3E2E6)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF03A9F4),
    onPrimary = Color.White,
    background = Color(0xFFF5F8FB),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFEFF6F8),
    onSurface = Color(0xFF1C1B1F)
)

// -----------------------------------------------------------------------------
// THEME
// -----------------------------------------------------------------------------

@Suppress("DEPRECATION")
@Composable
fun FlightsModernTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    val animatedBgColor = animateColorAsState(
        targetValue = colorScheme.background,
        animationSpec = tween(500)
    )

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as android.app.Activity).window
            window.statusBarColor = animatedBgColor.value.toArgb()
            window.navigationBarColor = animatedBgColor.value.toArgb()
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}

// -----------------------------------------------------------------------------
// ✅ ✅ ✅ STEP 1: FIXED TWO-LAYER BACKDROP SCAFFOLD
// -----------------------------------------------------------------------------

@Composable
fun FlightsBackdropScaffold(
    content: @Composable (
        globalBackdrop: LayerBackdrop,   // ✅ for background & regular glass
        buttonsBackdrop: LayerBackdrop   // ✅ for button grid & bottom sheet
    ) -> Unit
) {
    FlightsModernTheme {
        val globalBackdrop = rememberLayerBackdrop()
        val buttonsBackdrop = rememberLayerBackdrop()
        val isDark = isSystemInDarkTheme()

        val bgBrush = remember(isDark) {
            if (isDark) {
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0D1117), Color(0xFF010409), Color(0xFF0D1117))
                )
            } else {
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFF0F5F9), Color.White, Color(0xFFF0F5F9))
                )
            }
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {

            // ✅ BACKGROUND → written into globalBackdrop
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(globalBackdrop)
                    .background(bgBrush)
            ) {
                val circles = if (isDark) darkThemeCircles else lightThemeCircles
                DecorativeCircles(circles)
            }

            // ✅ FOREGROUND → written into buttonsBackdrop
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(buttonsBackdrop)
            ) {
                content(globalBackdrop, buttonsBackdrop)
            }
        }
    }
}

// -----------------------------------------------------------------------------
// BACKGROUND DRAWING
// -----------------------------------------------------------------------------

@Composable
private fun DecorativeCircles(elements: List<CircleElement>) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        elements.forEach { element ->
            drawCircle(
                color = element.color.copy(alpha = element.alpha),
                radius = size.minDimension * element.radiusFactor,
                center = Offset(
                    x = size.width * element.centerFactor.x,
                    y = size.height * element.centerFactor.y
                )
            )
        }
    }
}

// -----------------------------------------------------------------------------
// PREVIEW
// -----------------------------------------------------------------------------

@Preview
@Composable
fun FlightsBackdropScaffoldPreview() {
    FlightsBackdropScaffold { _, _ -> }
}
