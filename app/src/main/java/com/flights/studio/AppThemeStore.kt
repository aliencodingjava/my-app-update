package com.flights.studio

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.preference.PreferenceManager

enum class AppThemePreset(
    val label: String,
    val summary: String
) {
    Classic("Classic", "Electric blue and magenta"),
    Sky("Sky", "Clear ocean and sun"),
    Sunset("Sunset", "Coral, orange, and violet"),
    Aurora("Aurora", "Neon cyan, lime, and pink"),
    Graphite("Graphite", "Ink, cobalt, and gold")
}

@Immutable
data class AppThemePalette(
    val accent: Color,
    val warm: Color,
    val rose: Color
)

val LocalAppThemePreset = staticCompositionLocalOf { AppThemePreset.Classic }

val LocalAppThemePalette = staticCompositionLocalOf {
    AppThemePalette(
        accent = Color(0xFF0A84FF),
        warm = Color(0xFFFFB000),
        rose = Color(0xFFFF2D55)
    )
}

object AppThemeStore {
    private const val KEY_APP_THEME_PRESET = "app_theme_preset"

    val presets: List<AppThemePreset> = AppThemePreset.entries

    fun get(context: Context): AppThemePreset {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        val saved = prefs.getString(KEY_APP_THEME_PRESET, AppThemePreset.Classic.name)
        return AppThemePreset.entries.firstOrNull { it.name == saved } ?: AppThemePreset.Classic
    }

    fun set(context: Context, preset: AppThemePreset) {
        PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
            .edit()
            .putString(KEY_APP_THEME_PRESET, preset.name)
            .apply()
    }

    @Composable
    fun rememberPreset(context: Context): AppThemePreset {
        val appContext = context.applicationContext
        val prefs = remember(appContext) { PreferenceManager.getDefaultSharedPreferences(appContext) }
        var preset by remember(appContext) { mutableStateOf(get(appContext)) }

        DisposableEffect(prefs) {
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == KEY_APP_THEME_PRESET) {
                    preset = get(appContext)
                }
            }
            prefs.registerOnSharedPreferenceChangeListener(listener)
            onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
        }

        return preset
    }
}

fun appThemePaletteFor(preset: AppThemePreset, isDark: Boolean): AppThemePalette {
    return when (preset) {
        AppThemePreset.Classic -> AppThemePalette(
            accent = if (isDark) Color(0xFF64D2FF) else Color(0xFF0A84FF),
            warm = if (isDark) Color(0xFFFFD60A) else Color(0xFFFFB000),
            rose = if (isDark) Color(0xFFFF6B9A) else Color(0xFFFF2D55)
        )
        AppThemePreset.Sky -> AppThemePalette(
            accent = if (isDark) Color(0xFF5AC8FA) else Color(0xFF00A6FF),
            warm = if (isDark) Color(0xFFFFD60A) else Color(0xFFFFC400),
            rose = if (isDark) Color(0xFF30F2D2) else Color(0xFF00C7BE)
        )
        AppThemePreset.Sunset -> AppThemePalette(
            accent = if (isDark) Color(0xFFFF6B6B) else Color(0xFFFF3B30),
            warm = if (isDark) Color(0xFFFFB340) else Color(0xFFFF9500),
            rose = if (isDark) Color(0xFFD97CFF) else Color(0xFFBF5AF2)
        )
        AppThemePreset.Aurora -> AppThemePalette(
            accent = if (isDark) Color(0xFF00E5FF) else Color(0xFF00B8D9),
            warm = if (isDark) Color(0xFF8CFF6B) else Color(0xFF32D74B),
            rose = if (isDark) Color(0xFFFF5CE1) else Color(0xFFFF2D92)
        )
        AppThemePreset.Graphite -> AppThemePalette(
            accent = if (isDark) Color(0xFF8EAEFF) else Color(0xFF3366FF),
            warm = if (isDark) Color(0xFFFFD60A) else Color(0xFFFFB800),
            rose = if (isDark) Color(0xFFFF6B8A) else Color(0xFFFF375F)
        )
    }
}
