package com.flights.studio

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object SettingsStore {

    private const val PREF = "web_settings"

    private const val KEY_TEXT_ZOOM = "text_zoom"
    private const val KEY_HW_ACCEL = "hw_accel"
    private const val KEY_BLOCK_TRACKERS = "block_trackers"
    private const val KEY_CACHE_PAGES = "cache_pages"
    private const val KEY_HIGH_CONTRAST_WEB = "high_contrast_web"
    private const val KEY_REDUCE_WEB_MOTION = "reduce_web_motion"
    private const val KEY_ENHANCED_TABLE = "enhanced_table"
    private const val KEY_GROUP_FLIGHTS = "group_flights"
    private const val KEY_WEB_THEME = "web_theme"
    private const val KEY_FLIGHT_BRIEF_SNAPSHOT = "flight_brief_snapshot"
    private const val KEY_BRIEFING_WEATHER_SNAPSHOT = "briefing_weather_snapshot"
    private const val KEY_BRIEFING_WEATHER_ENABLED = "briefing_weather_enabled"
    private const val KEY_MAIN_PAGE_KEEP_AWAKE = "main_page_keep_awake"
    private const val KEY_LIVE_CAMERAS_KEEP_AWAKE = "live_cameras_keep_awake"

    const val DEFAULT_WEB_THEME = "auto"
    const val DEFAULT_TEXT_ZOOM = 90

    fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun textZoom(context: Context) =
        prefs(context).getInt(KEY_TEXT_ZOOM, DEFAULT_TEXT_ZOOM).coerceIn(60, 100)

    fun setTextZoom(context: Context, value: Int) =
        prefs(context).edit { putInt(KEY_TEXT_ZOOM, value.coerceIn(60, 100)) }

    fun webTheme(context: Context) =
        prefs(context).getString(KEY_WEB_THEME, DEFAULT_WEB_THEME) ?: DEFAULT_WEB_THEME

    fun setWebTheme(context: Context, value: String) =
        prefs(context).edit { putString(KEY_WEB_THEME, value) }

    fun hardwareAccel(context: Context) =
        prefs(context).getBoolean(KEY_HW_ACCEL, true)

    fun setHardwareAccel(context: Context, value: Boolean) =
        prefs(context).edit { putBoolean(KEY_HW_ACCEL, value) }

    fun blockTrackers(context: Context) =
        prefs(context).getBoolean(KEY_BLOCK_TRACKERS, true)

    fun setBlockTrackers(context: Context, value: Boolean) =
        prefs(context).edit { putBoolean(KEY_BLOCK_TRACKERS, value) }

    fun cachePages(context: Context) =
        prefs(context).getBoolean(KEY_CACHE_PAGES, true)

    fun setCachePages(context: Context, value: Boolean) =
        prefs(context).edit { putBoolean(KEY_CACHE_PAGES, value) }

    fun highContrastWeb(context: Context) =
        prefs(context).getBoolean(KEY_HIGH_CONTRAST_WEB, false)

    fun setHighContrastWeb(context: Context, value: Boolean) =
        prefs(context).edit { putBoolean(KEY_HIGH_CONTRAST_WEB, value) }

    fun reduceWebMotion(context: Context) =
        prefs(context).getBoolean(KEY_REDUCE_WEB_MOTION, false)

    fun setReduceWebMotion(context: Context, value: Boolean) =
        prefs(context).edit { putBoolean(KEY_REDUCE_WEB_MOTION, value) }

    fun enhancedTable(context: Context) =
        prefs(context).getBoolean(KEY_ENHANCED_TABLE, true)

    fun setEnhancedTable(context: Context, value: Boolean) =
        prefs(context).edit { putBoolean(KEY_ENHANCED_TABLE, value) }

    fun groupFlights(context: Context) =
        prefs(context).getBoolean(KEY_GROUP_FLIGHTS, false)

    fun setGroupFlights(context: Context, value: Boolean) =
        prefs(context).edit { putBoolean(KEY_GROUP_FLIGHTS, value) }

    fun flightBriefSnapshot(context: Context) =
        prefs(context).getString(KEY_FLIGHT_BRIEF_SNAPSHOT, "") ?: ""

    fun setFlightBriefSnapshot(context: Context, value: String) =
        prefs(context).edit { putString(KEY_FLIGHT_BRIEF_SNAPSHOT, value.take(4_000)) }

    fun briefingWeatherSnapshot(context: Context) =
        prefs(context).getString(KEY_BRIEFING_WEATHER_SNAPSHOT, "") ?: ""

    fun setBriefingWeatherSnapshot(context: Context, value: String) =
        prefs(context).edit { putString(KEY_BRIEFING_WEATHER_SNAPSHOT, value.take(1_200)) }

    fun briefingWeatherEnabled(context: Context) =
        prefs(context).getBoolean(KEY_BRIEFING_WEATHER_ENABLED, true)

    fun setBriefingWeatherEnabled(context: Context, value: Boolean) =
        prefs(context).edit { putBoolean(KEY_BRIEFING_WEATHER_ENABLED, value) }

    fun mainPageKeepAwake(context: Context) =
        prefs(context).getBoolean(KEY_MAIN_PAGE_KEEP_AWAKE, false)

    fun setMainPageKeepAwake(context: Context, value: Boolean) =
        prefs(context).edit { putBoolean(KEY_MAIN_PAGE_KEEP_AWAKE, value) }

    fun liveCamerasKeepAwake(context: Context) =
        prefs(context).getBoolean(KEY_LIVE_CAMERAS_KEEP_AWAKE, false)

    fun setLiveCamerasKeepAwake(context: Context, value: Boolean) =
        prefs(context).edit { putBoolean(KEY_LIVE_CAMERAS_KEEP_AWAKE, value) }
}
