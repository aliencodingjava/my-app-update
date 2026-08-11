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
            palette("orchid_mint", "Default", 0xFF090B0E, 0xFF151617, 0xFF252B31, 0xFF9FDBFF, 0xFF151617, 0.10f, true),
            palette("lake_slate", "Signal blue", 0xFF071223, 0xFF101C35, 0xFF17366A, 0xFF4F8DFF, 0xFF0D1830, 0.12f, true),
            palette("rose_cocoa", "Dragon fruit", 0xFF1A0812, 0xFF2A101E, 0xFF4B1A34, 0xFFFF4696, 0xFF2C1120, 0.12f, true),
            palette("prime_blue", "Ultra violet", 0xFF110720, 0xFF20103A, 0xFF3A176A, 0xFFB995FF, 0xFF251046, 0.13f, true),
            palette("aurora_plum", "Electric orchid", 0xFF190A1C, 0xFF2A112F, 0xFF53205D, 0xFFE46CFF, 0xFF311337, 0.13f, true),
            palette("moss_lilac", "Emerald ink", 0xFF04120D, 0xFF0A211A, 0xFF164A39, 0xFF52E0A4, 0xFF0C2A20, 0.12f, true),
            palette("storm_peach", "Soft apricot", 0xFF1B1008, 0xFF2B1A10, 0xFF53311F, 0xFFFFD6A5, 0xFF321D11, 0.12f, true),
            palette("glacier_iris", "Sky mint", 0xFF07161A, 0xFF0D272D, 0xFF164750, 0xFF8FFFE0, 0xFF113039, 0.12f, true),
            palette("forest_gold", "Champagne", 0xFF171207, 0xFF29200E, 0xFF4C3B18, 0xFFF8E7C9, 0xFF302511, 0.11f, true)
        )
    } else {
        listOf(
            palette("orchid_mint", "Default", 0xFFFFFFFF, 0xFFE2E9F1, 0xFFE6EEF7, 0xFF2A79D8, 0xFFF8F8FF, 0.06f, false),
            palette("lake_slate", "Signal blue", 0xFFF0F8FF, 0xFFF8FBFF, 0xFFDFF7FF, 0xFF0057FF, 0xFFF0F8FF, 0.09f, false),
            palette("rose_cocoa", "Dragon fruit", 0xFFFFF5F9, 0xFFFFF9FC, 0xFFFFE0ED, 0xFFC2185B, 0xFFFFF4F8, 0.09f, false),
            palette("prime_blue", "Ultra violet", 0xFFF8F4FF, 0xFFFCFAFF, 0xFFE8DEFF, 0xFF6A00F4, 0xFFF7F2FF, 0.10f, false),
            palette("aurora_plum", "Electric orchid", 0xFFFFF5FF, 0xFFFFFAFF, 0xFFF6DFFF, 0xFFA929C5, 0xFFFFF3FF, 0.09f, false),
            palette("moss_lilac", "Emerald ink", 0xFFF2FBF7, 0xFFFAFFFC, 0xFFDDF6EB, 0xFF064E3B, 0xFFF4FBF7, 0.09f, false),
            palette("storm_peach", "Soft apricot", 0xFFFFF8F1, 0xFFFFFCF8, 0xFFFFE8CF, 0xFFFC6C26, 0xFFFFF5EA, 0.09f, false),
            palette("glacier_iris", "Sky mint", 0xFFF1FFFB, 0xFFFAFFFD, 0xFFB8F7E4, 0xFF03313A, 0xFFEFFFFA, 0.09f, false),
            palette("forest_gold", "Champagne", 0xFFFFFAF0, 0xFFFFFCF7, 0xFFFFF0C9, 0xFF8A5D00, 0xFFFFF8EA, 0.08f, false)
        )
    }
}

fun notesBasicPaletteOptions(isDark: Boolean): List<NotesPaletteColors> {
    return if (isDark) {
        listOf(
            palette("flat_graphite", "Graphite", 0xFF0B0D10, 0xFF1F2329, 0xFF343942, 0xFFC8FF3D, 0xFF25272C, 0.08f, true),
            palette("flat_ink", "Violet ink", 0xFF100B20, 0xFF2D1B69, 0xFF3A2580, 0xFFB6A7FF, 0xFF20134B, 0.10f, true),
            palette("flat_sage", "Lime spark", 0xFF0E1308, 0xFF1A2410, 0xFF33451C, 0xFFB6FF2E, 0xFF202B14, 0.10f, true),
            palette("flat_wine", "Raspberry", 0xFF160710, 0xFF2A0D1D, 0xFF4D1734, 0xFFFF5D9E, 0xFF310F22, 0.10f, true),
            palette("flat_amber", "Butter yellow", 0xFF171304, 0xFF2A2409, 0xFF4F4312, 0xFFFFF275, 0xFF312A0B, 0.10f, true),
            palette("prime_emerald", "Cyber teal", 0xFF061316, 0xFF03313A, 0xFF0A5360, 0xFF8FFFE0, 0xFF073C45, 0.13f, true),
            palette("prime_violet", "Royal iris", 0xFF100522, 0xFF21094D, 0xFF3A0CA3, 0xFFC6ADFF, 0xFF2B0B70, 0.13f, true),
            palette("prime_sunset", "Burnt orange", 0xFF1A0B05, 0xFF321409, 0xFF6A2A0F, 0xFFFC6C26, 0xFF431B0B, 0.12f, true),
            palette("prime_ruby", "Soft lilac", 0xFF140F1D, 0xFF241B31, 0xFF49395F, 0xFFE8DEFF, 0xFF2D223D, 0.10f, true),
            palette("prime_gold", "Porcelain", 0xFF121211, 0xFF222221, 0xFF3A3936, 0xFFF8F7F4, 0xFF292826, 0.08f, true)
        )
    } else {
        listOf(
            palette("flat_graphite", "Graphite", 0xFFF5F5F5, 0xFFFAFAFA, 0xFFF0F0F0, 0xFF23262F, 0xFFF8F7F4, 0.05f, false),
            palette("flat_ink", "Violet ink", 0xFFF6F2FF, 0xFFFBF9FF, 0xFFE8DEFF, 0xFF2D1B69, 0xFFF2EDFF, 0.08f, false),
            palette("flat_sage", "Lime spark", 0xFFF8FDEB, 0xFFFCFFF5, 0xFFE8F5CC, 0xFF4C6A00, 0xFFF5FBDD, 0.08f, false),
            palette("flat_wine", "Raspberry", 0xFFFFF3F8, 0xFFFFFAFC, 0xFFFFDDEA, 0xFFC2185B, 0xFFFFEFF5, 0.08f, false),
            palette("flat_amber", "Butter yellow", 0xFFFFFBEA, 0xFFFFFEF8, 0xFFFFF2B8, 0xFF786500, 0xFFFFF8D8, 0.08f, false),
            palette("prime_emerald", "Cyber teal", 0xFFF0FFFB, 0xFFFAFFFD, 0xFF8FFFE0, 0xFF03313A, 0xFFE8FFF8, 0.10f, false),
            palette("prime_violet", "Royal iris", 0xFFF6F1FF, 0xFFFCFAFF, 0xFFE4D8FF, 0xFF3A0CA3, 0xFFF0E9FF, 0.10f, false),
            palette("prime_sunset", "Burnt orange", 0xFFFFF4EC, 0xFFFFFBF8, 0xFFFFDCC8, 0xFFB84200, 0xFFFFEDE1, 0.09f, false),
            palette("prime_ruby", "Soft lilac", 0xFFFAF7FF, 0xFFFEFCFF, 0xFFE8DEFF, 0xFF5B3DF5, 0xFFF5F0FF, 0.08f, false),
            palette("prime_gold", "Porcelain", 0xFFF8F7F4, 0xFFFBFBFB, 0xFFF0EEE9, 0xFF5C574F, 0xFFF5F3EE, 0.05f, false)
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
        noteTint = Color(noteTint),
        titleRail = Color(titleRail),
        accent = Color(accent),
        actionBarTint = Color(actionBarTint).copy(alpha = if (isDark) 0.86f else 0.88f),
        backdropTint = Color(accent).copy(alpha = backdropAlpha)
    )
}
