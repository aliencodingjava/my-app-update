package com.flights.studio

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

// -----------------------------------------------------------------------------
// Color schemes (kept)
// -----------------------------------------------------------------------------

private val OS_DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4FC3F7),
    onPrimary = Color(0xFF003355),
    background = Color(0xFF020617),
    onBackground = Color(0xFFE3E2E6),
    surface = Color(0xFF101522),
    onSurface = Color(0xFFE3E2E6)
)

private val OS_LightColorScheme = lightColorScheme(
    primary = Color(0xFF03A9F4),
    onPrimary = Color.White,
    background = Color(0xFFF7FAFF),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFEFF6F8),
    onSurface = Color(0xFF1C1B1F)
)

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
        animationSpec = tween(500),
        label = "splash_bg"
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

// -----------------------------------------------------------------------------
// Public API (unchanged signature)
// -----------------------------------------------------------------------------

@Composable
fun OpenSplashBackdropScaffold(
    content: @Composable (globalBackdrop: LayerBackdrop) -> Unit
) {
    OpenSplashTheme {
        val globalBackdrop = rememberLayerBackdrop()

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // ✅ Background written into the same globalBackdrop
            // NOTE: CirclesBackground is defined elsewhere (keep only ONE copy in the project)
            val isDark = isSystemInDarkTheme()

            ProfileBackdropImageLayer(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(globalBackdrop),
                lightRes = R.drawable.light_grid_pattern,
                darkRes = R.drawable.dark_grid_pattern,
                imageAlpha = if (isDark) 1f else 0.8f,
                scrimDark = 0f,
                scrimLight = 0f
            )
            // ✅ Foreground samples from the same globalBackdrop
            content(globalBackdrop)
        }
    }
}

@Preview
@Composable
private fun OpenSplashBackdropScaffoldPreview() {
    OpenSplashBackdropScaffold { /* no-op */ }
}
