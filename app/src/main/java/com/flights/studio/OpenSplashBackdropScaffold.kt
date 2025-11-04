package com.flights.studio

import android.app.Activity
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

// ---------- Private helpers (unique names to avoid collisions) ----------

private data class OSCircleElement(
    val color: Color,
    val alpha: Float,
    val radiusFactor: Float,      // relative to minDimension
    val centerFactor: Offset      // relative to size
)

private val OS_DarkCircles = listOf(
    OSCircleElement(Color(0xFF0D47A1), 0.30f, 0.55f, Offset(0.10f, 0.20f)),
    OSCircleElement(Color(0xFF00E5FF), 0.20f, 0.45f, Offset(0.85f, 0.80f)),
    OSCircleElement(Color(0xFF1DE9B6), 0.25f, 0.35f, Offset(0.20f, 0.85f)),
    OSCircleElement(Color(0xFF42A5F5), 0.15f, 0.60f, Offset(0.90f, 0.15f)),
    OSCircleElement(Color(0xFF00BFA5), 0.18f, 0.25f, Offset(0.50f, 0.50f))
)

private val OS_LightCircles = listOf(
    OSCircleElement(Color(0xFFB3E5FC), 0.40f, 0.55f, Offset(0.10f, 0.20f)),
    OSCircleElement(Color(0xFFE1F5FE), 0.30f, 0.45f, Offset(0.85f, 0.80f)),
    OSCircleElement(Color(0xFF81D4FA), 0.35f, 0.35f, Offset(0.20f, 0.85f)),
    OSCircleElement(Color(0xFF4FC3F7), 0.20f, 0.60f, Offset(0.90f, 0.15f)),
    OSCircleElement(Color(0xFF90A4AE), 0.15f, 0.25f, Offset(0.50f, 0.50f))
)

private val OS_DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4FC3F7),
    onPrimary = Color(0xFF003355),
    background = Color(0xFF0A0E18),
    onBackground = Color(0xFFE3E2E6),
    surface = Color(0xFF101522),
    onSurface = Color(0xFFE3E2E6)
)

private val OS_LightColorScheme = lightColorScheme(
    primary = Color(0xFF03A9F4),
    onPrimary = Color.White,
    background = Color(0xFFF5F8FB),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFEFF6F8),
    onSurface = Color(0xFF1C1B1F)
)

/**
 * Theme used only by OpenSplashBackdropScaffold.
 * Named uniquely to avoid conflicts with any existing theme composables.
 */
@Suppress("DEPRECATION")
@Composable
private fun OpenSplashTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) OS_DarkColorScheme else OS_LightColorScheme

    val view = LocalView.current
    val animatedBg = animateColorAsState(
        targetValue = colorScheme.background,
        animationSpec = tween(500)
    ).value

    if (!view.isInEditMode) {
        SideEffect {
            (view.context as? Activity)?.window?.let { window ->
                val c = animatedBg.toArgb()
                window.statusBarColor = c
                window.navigationBarColor = c
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}

// ---------- Public API you asked for ----------

/**
 * Exactly the scaffold pattern you wanted, but named `OpenSplashBackdropScaffold`.
 * Provides a global LayerBackdrop that all your glass elements can sample.
 */
@Composable
fun OpenSplashBackdropScaffold(
    content: @Composable (globalBackdrop: LayerBackdrop) -> Unit
) {
    OpenSplashTheme {
        val globalBackdrop = rememberLayerBackdrop()
        val isDark = isSystemInDarkTheme()

        val bgBrush = remember(isDark) {
            if (isDark) {
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0D1117), Color(0xFF010409), Color(0xFF0D1117))
                )
            } else {
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFF0F5F9), Color(0xFFFFFFFF), Color(0xFFF0F5F9))
                )
            }
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Write background into the backdrop layer
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(globalBackdrop)
                    .background(bgBrush)
            ) {
                val circles = if (isDark) OS_DarkCircles else OS_LightCircles
                OS_DecorativeCircles(circles)
            }

            // Your content samples from the same globalBackdrop
            content(globalBackdrop)
        }
    }
}

// ---------- Private drawing util ----------

@Composable
private fun OS_DecorativeCircles(elements: List<OSCircleElement>) {
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

// ---------- Preview ----------

@Preview
@Composable
private fun OpenSplashBackdropScaffoldPreview() {
    OpenSplashBackdropScaffold { /* no-op */ }
}
