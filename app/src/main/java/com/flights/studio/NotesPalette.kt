package com.flights.studio

import androidx.compose.ui.graphics.Color

data class NotesPaletteColors(
    val id: String,
    val label: String,
    val screenBackground: Color,
    val noteTint: Color,
    val titleRail: Color,
    val accent: Color,
    val actionBarTint: Color,
    val backdropTint: Color
)

fun notesPaletteOptions(isDark: Boolean): List<NotesPaletteColors> =
    notesWallpaperPaletteOptions(isDark) + notesBasicPaletteOptions(isDark)

fun notesWallpaperPaletteOptions(isDark: Boolean): List<NotesPaletteColors> {
    return if (isDark) {
        listOf(
            palette("orchid_mint", "Orchid mint", 0xFF15121A, 0xFF241C2E, 0xFF4F405F, 0xFF8FD7BB, 0xFF261D30, 0.14f, true),
            palette("lake_slate", "Lake slate", 0xFF101719, 0xFF18262A, 0xFF254A50, 0xFFA5D7E8, 0xFF172A2F, 0.13f, true),
            palette("rose_cocoa", "Rose cocoa", 0xFF1A1214, 0xFF2A1C20, 0xFF593744, 0xFFF3B7C9, 0xFF2E1D23, 0.12f, true),
            palette("prime_blue", "Prime blue", 0xFF07101F, 0xFF0C1E3A, 0xFF123B78, 0xFF4DA3FF, 0xFF0B2348, 0.18f, true),
            palette("aurora_plum", "Aurora plum", 0xFF161021, 0xFF261939, 0xFF51356D, 0xFFD59CFF, 0xFF2C1D42, 0.15f, true),
            palette("moss_lilac", "Moss lilac", 0xFF101812, 0xFF1C2A20, 0xFF3C5244, 0xFFB8D99D, 0xFF1F3125, 0.14f, true),
            palette("storm_peach", "Storm peach", 0xFF17151D, 0xFF272333, 0xFF5D4B60, 0xFFFFB59D, 0xFF322B3B, 0.13f, true),
            palette("glacier_iris", "Glacier iris", 0xFF0D1620, 0xFF172436, 0xFF354B77, 0xFF9EC8FF, 0xFF1B2D4A, 0.15f, true),
            palette("forest_gold", "Forest gold", 0xFF111608, 0xFF20270F, 0xFF4D5723, 0xFFE9CE72, 0xFF2D3515, 0.14f, true)
        )
    } else {
        listOf(
            palette("orchid_mint", "Orchid mint", 0xFFF2EAF7, 0xFFFEFBFF, 0xFFE8DAEF, 0xFF5DBE9B, 0xFFF2EAF7, 0.14f, false),
            palette("lake_slate", "Lake slate", 0xFFEAF3F4, 0xFFFBFEFF, 0xFFD7EAEE, 0xFF287B8D, 0xFFEAF3F4, 0.12f, false),
            palette("rose_cocoa", "Rose cocoa", 0xFFF8ECEE, 0xFFFFFBFB, 0xFFF0D9E0, 0xFFB85F7A, 0xFFF8ECEE, 0.11f, false),
            palette("prime_blue", "Prime blue", 0xFFE8F1FF, 0xFFFCFDFF, 0xFFD4E6FF, 0xFF006FE8, 0xFFE8F1FF, 0.13f, false),
            palette("aurora_plum", "Aurora plum", 0xFFF3E9FF, 0xFFFFFCFF, 0xFFE6D4F8, 0xFF9B5FD6, 0xFFF3E9FF, 0.12f, false),
            palette("moss_lilac", "Moss lilac", 0xFFEEF6E8, 0xFFFCFFFB, 0xFFDCECD3, 0xFF6D9B57, 0xFFEEF6E8, 0.12f, false),
            palette("storm_peach", "Storm peach", 0xFFF3EEF8, 0xFFFFFCFF, 0xFFE6DDF0, 0xFFE3836D, 0xFFF3EEF8, 0.11f, false),
            palette("glacier_iris", "Glacier iris", 0xFFEAF3FF, 0xFFFCFEFF, 0xFFD7E9FF, 0xFF4F84D9, 0xFFEAF3FF, 0.13f, false),
            palette("forest_gold", "Forest gold", 0xFFF3F5E5, 0xFFFFFFFB, 0xFFE8EBC9, 0xFFA88918, 0xFFF3F5E5, 0.11f, false)
        )
    }
}

fun notesBasicPaletteOptions(isDark: Boolean): List<NotesPaletteColors> {
    return if (isDark) {
        listOf(
            palette("flat_graphite", "Flat graphite", 0xFF101114, 0xFF1B1C20, 0xFF2C2D33, 0xFFE7E7ED, 0xFF17181C, 0.06f, true),
            palette("flat_ink", "Flat ink", 0xFF070A10, 0xFF101521, 0xFF202A3D, 0xFFBFD3FF, 0xFF0D1422, 0.08f, true),
            palette("flat_sage", "Flat sage", 0xFF0B130F, 0xFF14211A, 0xFF263D31, 0xFFA9D7BD, 0xFF101B15, 0.08f, true),
            palette("flat_wine", "Flat wine", 0xFF160911, 0xFF27101D, 0xFF442034, 0xFFFFB4D0, 0xFF220D19, 0.08f, true),
            palette("flat_amber", "Flat amber", 0xFF171006, 0xFF291B0A, 0xFF4D3413, 0xFFFFCF7A, 0xFF241707, 0.08f, true),
            palette("prime_emerald", "Prime emerald", 0xFF061510, 0xFF0C251D, 0xFF16583F, 0xFF39E29D, 0xFF0B3326, 0.16f, true),
            palette("prime_violet", "Prime violet", 0xFF120A20, 0xFF23113F, 0xFF4D22A5, 0xFFC09BFF, 0xFF2D175A, 0.16f, true),
            palette("prime_sunset", "Prime sunset", 0xFF1B0D08, 0xFF32170D, 0xFF7A3215, 0xFFFFA35A, 0xFF4B1F10, 0.14f, true),
            palette("prime_ruby", "Prime ruby", 0xFF1A0710, 0xFF310D1C, 0xFF7F1D42, 0xFFFF77A8, 0xFF4A1128, 0.14f, true),
            palette("prime_gold", "Prime gold", 0xFF171204, 0xFF2B2108, 0xFF6E5112, 0xFFFFD45A, 0xFF3B2B09, 0.15f, true)
        )
    } else {
        listOf(
            palette("flat_graphite", "Flat graphite", 0xFFF2F3F6, 0xFFFFFFFF, 0xFFE2E4EA, 0xFF525866, 0xFFF2F3F6, 0.06f, false),
            palette("flat_ink", "Flat ink", 0xFFEAF0FA, 0xFFFCFDFF, 0xFFD9E2F2, 0xFF315C9E, 0xFFEAF0FA, 0.08f, false),
            palette("flat_sage", "Flat sage", 0xFFEAF5ED, 0xFFFCFFFD, 0xFFD9ECDD, 0xFF4D8A63, 0xFFEAF5ED, 0.08f, false),
            palette("flat_wine", "Flat wine", 0xFFF7EAF1, 0xFFFFFCFE, 0xFFEED8E4, 0xFF9F4A72, 0xFFF7EAF1, 0.08f, false),
            palette("flat_amber", "Flat amber", 0xFFFFF2DE, 0xFFFFFDFA, 0xFFF6DFB9, 0xFFC17616, 0xFFFFF2DE, 0.08f, false),
            palette("prime_emerald", "Prime emerald", 0xFFE7F6EF, 0xFFFCFFFD, 0xFFD3EEDD, 0xFF009D68, 0xFFE7F6EF, 0.12f, false),
            palette("prime_violet", "Prime violet", 0xFFF0E9FF, 0xFFFEFCFF, 0xFFE0D2FF, 0xFF714BDB, 0xFFF0E9FF, 0.13f, false),
            palette("prime_sunset", "Prime sunset", 0xFFFFEFE4, 0xFFFFFCFA, 0xFFFFDEC9, 0xFFE76612, 0xFFFFEFE4, 0.11f, false),
            palette("prime_ruby", "Prime ruby", 0xFFFFEAF1, 0xFFFFFCFD, 0xFFFFD5E4, 0xFFD92868, 0xFFFFEAF1, 0.11f, false),
            palette("prime_gold", "Prime gold", 0xFFFFF4D8, 0xFFFFFFFA, 0xFFFFE5A6, 0xFFC99200, 0xFFFFF4D8, 0.12f, false)
        )
    }
}

fun resolveNotesPalette(id: String, isDark: Boolean): NotesPaletteColors {
    val palettes = notesPaletteOptions(isDark)
    return palettes.firstOrNull { it.id == id } ?: palettes.first()
}

private fun palette(
    id: String,
    label: String,
    screenBackground: Long,
    noteTint: Long,
    titleRail: Long,
    accent: Long,
    actionBarTint: Long,
    backdropAlpha: Float,
    isDark: Boolean
): NotesPaletteColors {
    return NotesPaletteColors(
        id = id,
        label = label,
        screenBackground = Color(screenBackground),
        noteTint = Color(noteTint).copy(alpha = if (isDark) 0.94f else 0.97f),
        titleRail = Color(titleRail).copy(alpha = if (isDark) 0.96f else 1f),
        accent = Color(accent),
        actionBarTint = Color(actionBarTint).copy(alpha = if (isDark) 0.86f else 0.88f),
        backdropTint = Color(accent).copy(alpha = backdropAlpha)
    )
}
