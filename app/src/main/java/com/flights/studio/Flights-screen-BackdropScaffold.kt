package com.flights.studio

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
// COLOR SCHEMES
// -----------------------------------------------------------------------------

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4FC3F7),
    onPrimary = Color(0xFF003355),
    background = Color(0xFF020617),
    onBackground = Color(0xFFE3E2E6),
    surface = Color(0xFF101522),
    onSurface = Color(0xFFE3E2E6)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF03A9F4),
    onPrimary = Color.White,
    background = Color(0xFFF7FAFF),
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
        animationSpec = tween(500),
        label = "bgColor"
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
// BACKDROP SCAFFOLD
// -----------------------------------------------------------------------------

@Composable
fun FlightsBackdropScaffold(
    content: @Composable (
        globalBackdrop: LayerBackdrop,
        buttonsBackdrop: LayerBackdrop
    ) -> Unit
) {
    FlightsModernTheme {
        val globalBackdrop = rememberLayerBackdrop()
        val buttonsBackdrop = rememberLayerBackdrop()

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
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
// PREVIEW
// -----------------------------------------------------------------------------

@Preview
@Composable
fun FlightsBackdropScaffoldPreview() {
    FlightsBackdropScaffold { _, _ -> }
}
