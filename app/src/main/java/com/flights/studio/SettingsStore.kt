package com.flights.studio

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object SettingsStore {

    private const val PREF = "web_settings"

    private const val KEY_DARK_WEB = "dark_web"
    private const val KEY_TEXT_ZOOM = "text_zoom"
    private const val KEY_HW_ACCEL = "hw_accel"

    fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun darkWeb(context: Context) =
        prefs(context).getBoolean(KEY_DARK_WEB, true)

    fun setDarkWeb(context: Context, value: Boolean) =
        prefs(context).edit { putBoolean(KEY_DARK_WEB, value) }

    fun textZoom(context: Context) =
        prefs(context).getInt(KEY_TEXT_ZOOM, 80)

    fun setTextZoom(context: Context, value: Int) =
        prefs(context).edit { putInt(KEY_TEXT_ZOOM, value) }

    fun hardwareAccel(context: Context) =
        prefs(context).getBoolean(KEY_HW_ACCEL, true)

    fun setHardwareAccel(context: Context, value: Boolean) =
        prefs(context).edit { putBoolean(KEY_HW_ACCEL, value) }
}