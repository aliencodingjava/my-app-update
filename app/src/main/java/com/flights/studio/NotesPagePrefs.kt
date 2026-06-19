package com.flights.studio

import android.content.Context

// ✅ Not private: usable from AllNotesActivity + NotesSettingsScreen + NotesAdapter
object NotesPagePrefs {
    const val NAME = "notes_page_settings"

    const val KEY_COMPACT = "compact"
    const val KEY_SHOW_IMAGES_BADGE = "show_images_badge"
    const val KEY_SHOW_REMINDER_BADGE = "show_reminder_badge"
    const val KEY_SHOW_REMINDER_BELL = "show_reminder_bell"
    const val KEY_SORT = "sort"
    const val KEY_TWO_COLUMNS = "two_columns_grid"
    const val KEY_PALETTE_ENABLED = "palette_enabled"
    const val KEY_PALETTE_ID = "palette_id"
    const val KEY_SYNC_ONLINE = "sync_online"

    // ✅ NEW: title vertical offset (in dp)
    const val KEY_TITLE_TOP_COMPACT = "title_top_compact"
    const val KEY_TITLE_TOP_NORMAL  = "title_top_normal"

    // ✅ defaults (dp)
    const val DEFAULT_TITLE_TOP_COMPACT = 8
    const val DEFAULT_TITLE_TOP_NORMAL  = 13
    const val TITLE_TOP_MIN_DP = 3
    const val TITLE_TOP_MAX_DP = 13

    const val SORT_NEWEST = "newest"
    const val SORT_OLDEST = "oldest"
    const val SORT_TITLE = "title"
    const val SORT_REMINDERS_FIRST = "reminders_first"
    // ✅ NEW: Add Note “AI title suggestions / info tip”
    const val KEY_ENABLE_TITLE_TIPS = "enable_title_tips"
    const val DEFAULT_ENABLE_TITLE_TIPS = true
    const val DEFAULT_SYNC_ONLINE = true
    const val DEFAULT_PALETTE_ID = "orchid_mint"
}


data class NotesPageSettings(
    val compact: Boolean,
    val showImagesBadge: Boolean,
    val showReminderBadge: Boolean,
    val showReminderBell: Boolean,
    val sortMode: String,
    val twoColumns: Boolean,

    // ✅ NEW
    val titleTopCompactDp: Int,
    val titleTopNormalDp: Int,
    val enableTitleTips: Boolean,
    val paletteEnabled: Boolean,
    val paletteId: String,
    val syncOnline: Boolean,

    )


    fun Context.readNotesPageSettings(): NotesPageSettings {
        val sp = getSharedPreferences(NotesPagePrefs.NAME, Context.MODE_PRIVATE)
        return NotesPageSettings(
            compact = sp.getBoolean(NotesPagePrefs.KEY_COMPACT, false),
            twoColumns = sp.getBoolean(NotesPagePrefs.KEY_TWO_COLUMNS, false),
            showImagesBadge = sp.getBoolean(NotesPagePrefs.KEY_SHOW_IMAGES_BADGE, true),
            showReminderBadge = sp.getBoolean(NotesPagePrefs.KEY_SHOW_REMINDER_BADGE, true),
            showReminderBell = sp.getBoolean(NotesPagePrefs.KEY_SHOW_REMINDER_BELL, true),
            sortMode = sp.getString(NotesPagePrefs.KEY_SORT, NotesPagePrefs.SORT_NEWEST)
                ?: NotesPagePrefs.SORT_NEWEST,
            titleTopCompactDp = sp.getInt(
                NotesPagePrefs.KEY_TITLE_TOP_COMPACT,
                NotesPagePrefs.DEFAULT_TITLE_TOP_COMPACT
            ).coerceIn(NotesPagePrefs.TITLE_TOP_MIN_DP, NotesPagePrefs.TITLE_TOP_MAX_DP),
            titleTopNormalDp = sp.getInt(
                NotesPagePrefs.KEY_TITLE_TOP_NORMAL,
                NotesPagePrefs.DEFAULT_TITLE_TOP_NORMAL
            ).coerceIn(NotesPagePrefs.TITLE_TOP_MIN_DP, NotesPagePrefs.TITLE_TOP_MAX_DP),

            // ✅ NEW

            enableTitleTips = sp.getBoolean(
                NotesPagePrefs.KEY_ENABLE_TITLE_TIPS,
                NotesPagePrefs.DEFAULT_ENABLE_TITLE_TIPS
            ),
            paletteEnabled = sp.getBoolean(NotesPagePrefs.KEY_PALETTE_ENABLED, false),
            paletteId = sp.getString(
                NotesPagePrefs.KEY_PALETTE_ID,
                NotesPagePrefs.DEFAULT_PALETTE_ID
            ) ?: NotesPagePrefs.DEFAULT_PALETTE_ID,
            syncOnline = sp.getBoolean(
                NotesPagePrefs.KEY_SYNC_ONLINE,
                NotesPagePrefs.DEFAULT_SYNC_ONLINE
            )
        )
    }
