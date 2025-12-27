package com.flights.studio

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/* ---------- LIGHT ---------- */
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0A6DFF),
    onPrimary = Color.White,

    surface = Color(0xFFF4F5F7),
    onSurface = Color(0xFF101214),

    surfaceVariant = Color(0xFFE9EBEF),
    onSurfaceVariant = Color(0xFF4A4F57),

    outline = Color(0xFFD5D9E0),
    outlineVariant = Color(0xFFC9CED6)
)

/* ---------- DARK ---------- */
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF7DB4FF),
    onPrimary = Color(0xFF0B1220),

    surface = Color(0xFF0E1116),
    onSurface = Color(0xFFE6E9EF),

    surfaceVariant = Color(0xFF161A22),
    onSurfaceVariant = Color(0xFF9AA3B2),

    outline = Color(0xFF2A3040),
    outlineVariant = Color(0xFF343B4F)
)

@Composable
fun FlightsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colors,
        typography = Typography(),
        content = content
    )
}
