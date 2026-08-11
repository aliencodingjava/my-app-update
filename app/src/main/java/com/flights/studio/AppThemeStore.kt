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
import androidx.core.content.edit

enum class AppThemePreset(
    val label: String,
    val summary: String
) {
    Classic("Classic", "Electric blue and magenta"),
    Sky("Sky", "Clear ocean and sun"),
    Sunset("Sunset", "Coral, orange, and violet"),
    Aurora("Aurora", "Neon cyan, lime, and pink"),
    Graphite("Graphite", "Ink, cobalt, and gold"),
    Ocean("Ocean", "Deep teal, surf, and coral"),
    Meadow("Meadow", "Leaf green, mint, and amber"),
    Candy("Candy", "Berry, bubblegum, and lemon"),
    Royal("Royal", "Indigo, orchid, and pearl"),
    Ember("Ember", "Charcoal, copper, and fire")
}

@Immutable
data class AppThemePalette(
    val accent: Color,
    val warm: Color,
    val rose: Color,
    val page: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val outline: Color,
    val card: Color,
    val glass: Color,
    val glassOverlay: Color,
    val action: Color,
    val actionContent: Color,
    val badge: Color,
    val badgeContent: Color,
    val bottomBar: Color,
    val bottomPill: Color
)

@Immutable
data class AppThemeSurfaceRoles(
    val page: Color,
    val card: Color,
    val glassCard: Color,
    val border: Color,
    val title: Color,
    val subtitle: Color,
    val section: Color,
    val iconSurface: Color,
    val iconContent: Color,
    val chevron: Color
)

fun appThemeSurfaceRoles(
    palette: AppThemePalette,
    isDark: Boolean
): AppThemeSurfaceRoles {
    val title = if (isDark) Color(0xFFF6F8FB) else Color(0xFF101418)
    val subtitle = if (isDark) Color(0xFFC4CCD6) else Color(0xFF4F5B66)
    return AppThemeSurfaceRoles(
        page = palette.page,
        card = palette.card.copy(alpha = if (isDark) 0.86f else 0.92f),
        glassCard = palette.glass.copy(alpha = if (isDark) 0.78f else 0.88f),
        border = palette.outline.copy(alpha = if (isDark) 0.46f else 0.34f),
        title = title,
        subtitle = subtitle,
        section = palette.accent,
        iconSurface = palette.action.copy(alpha = if (isDark) 0.22f else 0.18f),
        iconContent = palette.actionContent.copy(alpha = if (isDark) 0.96f else 0.98f),
        chevron = palette.actionContent.copy(alpha = if (isDark) 0.70f else 0.62f)
    )
}

val LocalAppThemePreset = staticCompositionLocalOf { AppThemePreset.Classic }

val LocalAppThemePalette = staticCompositionLocalOf {
    AppThemePalette(
        accent = Color(0xFF0A84FF),
        warm = Color(0xFFFFB000),
        rose = Color(0xFFFF2D55),
        page = Color(0xFFEAF1FA),
        surface = Color(0xFFFBFBFB),
        surfaceVariant = Color(0xFFDDE8F6),
        outline = Color(0xFFB7CBE4),
        card = Color(0xFFFBFBFB),
        glass = Color(0xFFEAF4FF),
        glassOverlay = Color(0xFF0A84FF),
        action = Color(0xFF0A84FF),
        actionContent = Color.White,
        badge = Color(0xFFFFE7F0),
        badgeContent = Color(0xFF8A1542),
        bottomBar = Color(0xFFEAF4FF),
        bottomPill = Color(0xFF17243A)
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
            .edit {
                putString(KEY_APP_THEME_PRESET, preset.name)
            }
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
            accent = if (isDark) Color(0xFF58D5FF) else Color(0xFF006CFF),
            warm = if (isDark) Color(0xFFFFD60A) else Color(0xFFFFA800),
            rose = if (isDark) Color(0xFFFF5FA2) else Color(0xFFE91E63),
            page = if (isDark) Color(0xFF07121F) else Color(0xFFE9F2FF),
            surface = if (isDark) Color(0xFF111A28) else Color(0xFFFBFBFB),
            surfaceVariant = if (isDark) Color(0xFF17263A) else Color(0xFFD9E9FF),
            outline = if (isDark) Color(0xFF29425F) else Color(0xFF9ABEEB),
            card = if (isDark) Color(0xFF121D2D) else Color(0xFFFCFEFF),
            glass = if (isDark) Color(0xFF12314A) else Color(0xFFE3F1FF),
            glassOverlay = if (isDark) Color(0xFF58D5FF) else Color(0xFF006CFF),
            action = if (isDark) Color(0xFF196B8A) else Color(0xFFD9ECFF),
            actionContent = if (isDark) Color(0xFFEAF8FF) else Color(0xFF08345A),
            badge = if (isDark) Color(0xFF3D2445) else Color(0xFFFFE5F0),
            badgeContent = if (isDark) Color(0xFFFFC7E0) else Color(0xFF8B1748),
            bottomBar = if (isDark) Color(0xFF0F2434) else Color(0xFFE4F1FF),
            bottomPill = if (isDark) Color(0xFF213B50) else Color(0xFF173150)
        )
        AppThemePreset.Sky -> AppThemePalette(
            accent = if (isDark) Color(0xFF5CE1FF) else Color(0xFF008DFF),
            warm = if (isDark) Color(0xFFFFD84D) else Color(0xFFFFC400),
            rose = if (isDark) Color(0xFF35F4D5) else Color(0xFF00BFA6),
            page = if (isDark) Color(0xFF061B2A) else Color(0xFFE4F6FF),
            surface = if (isDark) Color(0xFF0D2B3F) else Color(0xFFF8FDFF),
            surfaceVariant = if (isDark) Color(0xFF123D58) else Color(0xFFCBEFFF),
            outline = if (isDark) Color(0xFF236985) else Color(0xFF75C8F3),
            card = if (isDark) Color(0xFF0C3146) else Color(0xFFF8FDFF),
            glass = if (isDark) Color(0xFF10465E) else Color(0xFFD8F4FF),
            glassOverlay = if (isDark) Color(0xFF35F4D5) else Color(0xFF008DFF),
            action = if (isDark) Color(0xFF0E6C7C) else Color(0xFFD0F4FF),
            actionContent = if (isDark) Color(0xFFE8FFFF) else Color(0xFF003C59),
            badge = if (isDark) Color(0xFF173F35) else Color(0xFFE0FFF8),
            badgeContent = if (isDark) Color(0xFFB8FFF1) else Color(0xFF006151),
            bottomBar = if (isDark) Color(0xFF0B344A) else Color(0xFFDDF5FF),
            bottomPill = if (isDark) Color(0xFF175A70) else Color(0xFF064160)
        )
        AppThemePreset.Sunset -> AppThemePalette(
            accent = if (isDark) Color(0xFFFF6A58) else Color(0xFFE83F2F),
            warm = if (isDark) Color(0xFFFFB84F) else Color(0xFFFF8A00),
            rose = if (isDark) Color(0xFFE381FF) else Color(0xFFB536E8),
            page = if (isDark) Color(0xFF24101B) else Color(0xFFFFF0E5),
            surface = if (isDark) Color(0xFF311725) else Color(0xFFFFFBF7),
            surfaceVariant = if (isDark) Color(0xFF472139) else Color(0xFFFFD7C2),
            outline = if (isDark) Color(0xFF7B3D55) else Color(0xFFFFA47D),
            card = if (isDark) Color(0xFF351827) else Color(0xFFFFFBF7),
            glass = if (isDark) Color(0xFF512436) else Color(0xFFFFE2CF),
            glassOverlay = if (isDark) Color(0xFFFF8A65) else Color(0xFFE83F2F),
            action = if (isDark) Color(0xFF7A2F2D) else Color(0xFFFFE0D1),
            actionContent = if (isDark) Color(0xFFFFF2EC) else Color(0xFF6F1E14),
            badge = if (isDark) Color(0xFF44244F) else Color(0xFFF5E0FF),
            badgeContent = if (isDark) Color(0xFFF2C6FF) else Color(0xFF6F2396),
            bottomBar = if (isDark) Color(0xFF3A1928) else Color(0xFFFFE5D4),
            bottomPill = if (isDark) Color(0xFF653142) else Color(0xFF5B241C)
        )
        AppThemePreset.Aurora -> AppThemePalette(
            accent = if (isDark) Color(0xFF00F0FF) else Color(0xFF00A7C4),
            warm = if (isDark) Color(0xFF8CFF6B) else Color(0xFF20C846),
            rose = if (isDark) Color(0xFFFF5CE1) else Color(0xFFE82591),
            page = if (isDark) Color(0xFF071A18) else Color(0xFFE7FFF6),
            surface = if (isDark) Color(0xFF102720) else Color(0xFFFAFFFC),
            surfaceVariant = if (isDark) Color(0xFF173A31) else Color(0xFFCFFBEA),
            outline = if (isDark) Color(0xFF2E745F) else Color(0xFF78DDBB),
            card = if (isDark) Color(0xFF102B25) else Color(0xFFFAFFFC),
            glass = if (isDark) Color(0xFF164337) else Color(0xFFD9FFF2),
            glassOverlay = if (isDark) Color(0xFFFF5CE1) else Color(0xFF00A7C4),
            action = if (isDark) Color(0xFF11695E) else Color(0xFFD7FFF0),
            actionContent = if (isDark) Color(0xFFE9FFF8) else Color(0xFF00493F),
            badge = if (isDark) Color(0xFF38255B) else Color(0xFFF1E6FF),
            badgeContent = if (isDark) Color(0xFFD8C7FF) else Color(0xFF5B2FB0),
            bottomBar = if (isDark) Color(0xFF0D362E) else Color(0xFFDFFFF6),
            bottomPill = if (isDark) Color(0xFF1F5E50) else Color(0xFF074C43)
        )
        AppThemePreset.Graphite -> AppThemePalette(
            accent = if (isDark) Color(0xFF9DB7FF) else Color(0xFF255CFF),
            warm = if (isDark) Color(0xFFFFD94A) else Color(0xFFE6A400),
            rose = if (isDark) Color(0xFFFF668C) else Color(0xFFE02F5A),
            page = if (isDark) Color(0xFF0B0D11) else Color(0xFFF5F5F5),
            surface = if (isDark) Color(0xFF171A20) else Color(0xFFFBFBFB),
            surfaceVariant = if (isDark) Color(0xFF232833) else Color(0xFFDDE2ED),
            outline = if (isDark) Color(0xFF454D5F) else Color(0xFFB8C0CF),
            card = if (isDark) Color(0xFF191D25) else Color(0xFFFBFBFB),
            glass = if (isDark) Color(0xFF252B38) else Color(0xFFE4E9F3),
            glassOverlay = if (isDark) Color(0xFFFFD94A) else Color(0xFF255CFF),
            action = if (isDark) Color(0xFF30384A) else Color(0xFFE6EBF6),
            actionContent = if (isDark) Color(0xFFF5F7FF) else Color(0xFF1F293A),
            badge = if (isDark) Color(0xFF3C2F18) else Color(0xFFFFF0BC),
            badgeContent = if (isDark) Color(0xFFFFE39B) else Color(0xFF6E5100),
            bottomBar = if (isDark) Color(0xFF181D27) else Color(0xFFE6EAF3),
            bottomPill = if (isDark) Color(0xFF303848) else Color(0xFF232B38)
        )
        AppThemePreset.Ocean -> AppThemePalette(
            accent = if (isDark) Color(0xFF53E5D4) else Color(0xFF007F8A),
            warm = if (isDark) Color(0xFFFFC46B) else Color(0xFFFF8A54),
            rose = if (isDark) Color(0xFFFF7A9E) else Color(0xFFD83F66),
            page = if (isDark) Color(0xFF061819) else Color(0xFFE7FAF8),
            surface = if (isDark) Color(0xFF102A2B) else Color(0xFFFBFFFE),
            surfaceVariant = if (isDark) Color(0xFF183F40) else Color(0xFFCFF4F0),
            outline = if (isDark) Color(0xFF367273) else Color(0xFF77C9C3),
            card = if (isDark) Color(0xFF102F31) else Color(0xFFF7FFFD),
            glass = if (isDark) Color(0xFF17494A) else Color(0xFFD6F8F5),
            glassOverlay = if (isDark) Color(0xFF53E5D4) else Color(0xFF007F8A),
            action = if (isDark) Color(0xFF12656A) else Color(0xFFD4F5F1),
            actionContent = if (isDark) Color(0xFFE8FFFC) else Color(0xFF003F45),
            badge = if (isDark) Color(0xFF422B2C) else Color(0xFFFFE5DF),
            badgeContent = if (isDark) Color(0xFFFFCBC1) else Color(0xFF8A2C1D),
            bottomBar = if (isDark) Color(0xFF0E3638) else Color(0xFFDDF5F2),
            bottomPill = if (isDark) Color(0xFF1B585B) else Color(0xFF0C4B50)
        )
        AppThemePreset.Meadow -> AppThemePalette(
            accent = if (isDark) Color(0xFFA8F06D) else Color(0xFF4F8F1F),
            warm = if (isDark) Color(0xFFFFD166) else Color(0xFFE2A300),
            rose = if (isDark) Color(0xFF8AF5C0) else Color(0xFF1E9C65),
            page = if (isDark) Color(0xFF10180C) else Color(0xFFF0F9E8),
            surface = if (isDark) Color(0xFF1C2815) else Color(0xFFFEFFF9),
            surfaceVariant = if (isDark) Color(0xFF2B3C20) else Color(0xFFDCEFCB),
            outline = if (isDark) Color(0xFF587446) else Color(0xFFA8C98B),
            card = if (isDark) Color(0xFF1D2D16) else Color(0xFFFDFFF7),
            glass = if (isDark) Color(0xFF2B4520) else Color(0xFFE5F6D7),
            glassOverlay = if (isDark) Color(0xFFA8F06D) else Color(0xFF4F8F1F),
            action = if (isDark) Color(0xFF3D6927) else Color(0xFFE1F3D1),
            actionContent = if (isDark) Color(0xFFF3FFE9) else Color(0xFF244A10),
            badge = if (isDark) Color(0xFF433719) else Color(0xFFFFF1C7),
            badgeContent = if (isDark) Color(0xFFFFE6A1) else Color(0xFF6B4B00),
            bottomBar = if (isDark) Color(0xFF243719) else Color(0xFFE8F4DC),
            bottomPill = if (isDark) Color(0xFF3A5528) else Color(0xFF31551A)
        )
        AppThemePreset.Candy -> AppThemePalette(
            accent = if (isDark) Color(0xFFFF82D8) else Color(0xFFD7258F),
            warm = if (isDark) Color(0xFFFFE76A) else Color(0xFFE2AF00),
            rose = if (isDark) Color(0xFF9CCBFF) else Color(0xFF4A73E8),
            page = if (isDark) Color(0xFF21101D) else Color(0xFFFFF0FA),
            surface = if (isDark) Color(0xFF321B2D) else Color(0xFFFFFBFD),
            surfaceVariant = if (isDark) Color(0xFF4B2844) else Color(0xFFFFD8F0),
            outline = if (isDark) Color(0xFF874472) else Color(0xFFE89AC9),
            card = if (isDark) Color(0xFF371D31) else Color(0xFFFFFAFD),
            glass = if (isDark) Color(0xFF552B4C) else Color(0xFFFFDFF2),
            glassOverlay = if (isDark) Color(0xFFFF82D8) else Color(0xFFD7258F),
            action = if (isDark) Color(0xFF763064) else Color(0xFFFFD9EF),
            actionContent = if (isDark) Color(0xFFFFF0FA) else Color(0xFF72134A),
            badge = if (isDark) Color(0xFF26345B) else Color(0xFFE6EEFF),
            badgeContent = if (isDark) Color(0xFFC6D8FF) else Color(0xFF2347A0),
            bottomBar = if (isDark) Color(0xFF3F2239) else Color(0xFFFFE4F4),
            bottomPill = if (isDark) Color(0xFF653458) else Color(0xFF6F2853)
        )
        AppThemePreset.Royal -> AppThemePalette(
            accent = if (isDark) Color(0xFFB9A7FF) else Color(0xFF6545D8),
            warm = if (isDark) Color(0xFFE8D7FF) else Color(0xFF9C6BFF),
            rose = if (isDark) Color(0xFFFFA1C8) else Color(0xFFC9467A),
            page = if (isDark) Color(0xFF111025) else Color(0xFFF2EFFF),
            surface = if (isDark) Color(0xFF1F1C35) else Color(0xFFFFFCFF),
            surfaceVariant = if (isDark) Color(0xFF302A51) else Color(0xFFE5DCFF),
            outline = if (isDark) Color(0xFF62579A) else Color(0xFFB4A5E8),
            card = if (isDark) Color(0xFF211E3B) else Color(0xFFFEFBFF),
            glass = if (isDark) Color(0xFF332D60) else Color(0xFFE9E1FF),
            glassOverlay = if (isDark) Color(0xFFB9A7FF) else Color(0xFF6545D8),
            action = if (isDark) Color(0xFF50438D) else Color(0xFFE7DFFF),
            actionContent = if (isDark) Color(0xFFFAF7FF) else Color(0xFF352276),
            badge = if (isDark) Color(0xFF4A2741) else Color(0xFFFFE1F0),
            badgeContent = if (isDark) Color(0xFFFFC7E4) else Color(0xFF7A1E55),
            bottomBar = if (isDark) Color(0xFF292548) else Color(0xFFEDE6FF),
            bottomPill = if (isDark) Color(0xFF463E7D) else Color(0xFF3E2C78)
        )
        AppThemePreset.Ember -> AppThemePalette(
            accent = if (isDark) Color(0xFFFF9A5F) else Color(0xFFC94D19),
            warm = if (isDark) Color(0xFFFFD36E) else Color(0xFFE08A00),
            rose = if (isDark) Color(0xFFFF6F6F) else Color(0xFFC62828),
            page = if (isDark) Color(0xFF14100D) else Color(0xFFFFF1E8),
            surface = if (isDark) Color(0xFF241C18) else Color(0xFFFFFCFA),
            surfaceVariant = if (isDark) Color(0xFF392A22) else Color(0xFFFFD8C2),
            outline = if (isDark) Color(0xFF735342) else Color(0xFFD9A083),
            card = if (isDark) Color(0xFF2A1E18) else Color(0xFFFFFAF7),
            glass = if (isDark) Color(0xFF432E24) else Color(0xFFFFE1CF),
            glassOverlay = if (isDark) Color(0xFFFF9A5F) else Color(0xFFC94D19),
            action = if (isDark) Color(0xFF673B27) else Color(0xFFFFDDC7),
            actionContent = if (isDark) Color(0xFFFFF3EC) else Color(0xFF6A240B),
            badge = if (isDark) Color(0xFF4A2121) else Color(0xFFFFDFDF),
            badgeContent = if (isDark) Color(0xFFFFC1C1) else Color(0xFF7B1818),
            bottomBar = if (isDark) Color(0xFF302119) else Color(0xFFFFE4D3),
            bottomPill = if (isDark) Color(0xFF563624) else Color(0xFF5C2B17)
        )
    }
}
