package com.flights.studio

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object AppIconManager {
    private const val PREFS_NAME = "app_icon_manager"
    private const val KEY_ACTIVE_ALIAS = "active_alias"

    const val DICON = "com.flights.studio.DiconAlias"
    const val WATCH_OS = "com.flights.studio.WatchOsAlias"
    const val CLEAR_DARK = "com.flights.studio.ClearDarkAlias"
    const val CLEAR_LIGHT = "com.flights.studio.ClearLightAlias"
    const val DARK = "com.flights.studio.DarkAlias"
    const val TINTED_DARK = "com.flights.studio.TintedDarkAlias"
    const val TINTED_LIGHT = "com.flights.studio.TintedLightAlias"
    const val GENERAL_ICON = "com.flights.studio.GeneraliconAlias"

    const val WATCH_OS_F = "com.flights.studio.WatchOsFAlias"
    const val CLEAR_DARK_F = "com.flights.studio.ClearDarkFAlias"
    const val CLEAR_LIGHT_F = "com.flights.studio.ClearLightFAlias"
    const val DARK_F = "com.flights.studio.DarkFAlias"
    const val DICON_F = "com.flights.studio.DiconFAlias"
    const val TINTED_DARK_F = "com.flights.studio.TintedDarkFAlias"
    const val TINTED_LIGHT_F = "com.flights.studio.TintedLightFAlias"
    const val EXCLUSIVE_ICON_F = "com.flights.studio.ExclusiveIconFAlias"

    const val CLEAR_DARK_WINTER = "com.flights.studio.ClearDarkWinterAlias"
    const val CLEAR_LIGHT_WINTER = "com.flights.studio.ClearLightWinterAlias"
    const val DARK_WINTER = "com.flights.studio.DarkWinterAlias"
    const val ORIGINAL_WINTER = "com.flights.studio.OriginalWinterAlias"
    const val TINTED_DARK_WINTER = "com.flights.studio.TintedDarkWinterAlias"
    const val TINTED_LIGHT_WINTER = "com.flights.studio.TintedLightWinterAlias"
    const val WATCH_OS_WINTER = "com.flights.studio.WatchOsWinterAlias"

    const val CLEAR_DARK_JH = "com.flights.studio.ClearDarkJHAlias"
    const val CLEAR_LIGHT_JH = "com.flights.studio.ClearLightJHAlias"
    const val DARK_JH = "com.flights.studio.DarkJHAlias"
    const val ORIGINAL_JH = "com.flights.studio.OriginalJHAlias"
    const val TINTED_DARK_JH = "com.flights.studio.TintedDarkJHAlias"
    const val TINTED_LIGHT_JH = "com.flights.studio.TintedLightJHAlias"
    const val WATCH_OS_JH = "com.flights.studio.WatchOsJHAlias"


    private val aliases = listOf(
        DICON,
        WATCH_OS,
        CLEAR_DARK,
        CLEAR_LIGHT,
        DARK,
        TINTED_DARK,
        TINTED_LIGHT,
        GENERAL_ICON,
        WATCH_OS_F,
        CLEAR_DARK_F,
        CLEAR_LIGHT_F,
        DARK_F,
        DICON_F,
        TINTED_DARK_F,
        TINTED_LIGHT_F,
        EXCLUSIVE_ICON_F,
        CLEAR_DARK_WINTER,
        CLEAR_LIGHT_WINTER,
        DARK_WINTER,
        ORIGINAL_WINTER,
        TINTED_DARK_WINTER,
        TINTED_LIGHT_WINTER,
        WATCH_OS_WINTER,
        CLEAR_DARK_JH,
        CLEAR_LIGHT_JH,
        DARK_JH,
        ORIGINAL_JH,
        TINTED_DARK_JH,
        TINTED_LIGHT_JH,
        WATCH_OS_JH
    )

    fun setActiveIcon(context: Context, aliasClassName: String) {
        val pm = context.packageManager
        val activeAlias = aliasClassName.takeIf { it in aliases } ?: DICON

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ACTIVE_ALIAS, activeAlias)
            .apply()

        aliases.forEach { className ->
            val newState = if (className == activeAlias) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }

            pm.setComponentEnabledSetting(
                ComponentName(context, className),
                newState,
                PackageManager.DONT_KILL_APP
            )
        }
    }

    fun repairLauncherAliases(context: Context) {
        val pm = context.packageManager
        val savedAlias = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ACTIVE_ALIAS, null)
            ?.takeIf { it in aliases }
        val explicitlyEnabled = aliases.filter { alias ->
            pm.getComponentEnabledSetting(ComponentName(context, alias)) ==
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        }
        val activeAlias = when {
            savedAlias != null -> savedAlias
            explicitlyEnabled.size > 1 -> explicitlyEnabled.firstOrNull { it != DICON } ?: explicitlyEnabled.first()
            explicitlyEnabled.size == 1 -> explicitlyEnabled.first()
            else -> DICON
        }

        if (explicitlyEnabled.size != 1 || explicitlyEnabled.firstOrNull() != activeAlias) {
            setActiveIcon(context, activeAlias)
        }
    }

    fun getActiveIcon(context: Context): String {
        val pm = context.packageManager
        val savedAlias = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ACTIVE_ALIAS, null)
            ?.takeIf { it in aliases }
        if (savedAlias != null) {
            val state = pm.getComponentEnabledSetting(ComponentName(context, savedAlias))
            if (state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                return savedAlias
            }
        }

        aliases.forEach { alias ->
            val state = pm.getComponentEnabledSetting(ComponentName(context, alias))
            if (state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                return alias
            }
        }

        // First-launch fallback, because manifest defaults may return DEFAULT
        return DICON
    }
}
