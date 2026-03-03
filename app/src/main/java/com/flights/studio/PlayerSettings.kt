package com.flights.studio

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

data class PlayerSettings(
    val autoplayNext: Boolean = true,
    val resumePlayback: Boolean = true,
    val playbackSpeed: Float = 1.0f,
    val glassIntensity: Int = 1,       // 0 = light, 1 = medium, 2 = strong
    val menuElasticity: Int = 1,       // 0 = soft, 1 = normal, 2 = heavy
    val animateLikes: Boolean = true,
    val dimStrength: Int = 1           // 0 = none, 1 = light, 2 = dark
)


val Context.playerSettingsDataStore by preferencesDataStore(name = "player_settings")

val KEY_AUTOPLAY = booleanPreferencesKey("autoplay_next")
val KEY_RESUME = booleanPreferencesKey("resume_playback")
val KEY_SPEED = floatPreferencesKey("playback_speed")
val KEY_GLASS = intPreferencesKey("glass_intensity")
val KEY_ELASTIC = intPreferencesKey("menu_elasticity")
val KEY_ANIMATE_LIKES = booleanPreferencesKey("animate_likes")
val KEY_DIM = intPreferencesKey("dim_strength")

fun Context.playerSettingsFlow() = playerSettingsDataStore.data.map { prefs ->
    PlayerSettings(
        autoplayNext = prefs[KEY_AUTOPLAY] ?: true,
        resumePlayback = prefs[KEY_RESUME] ?: true,
        playbackSpeed = prefs[KEY_SPEED] ?: 1.0f,
        glassIntensity = prefs[KEY_GLASS] ?: 1,
        menuElasticity = prefs[KEY_ELASTIC] ?: 1,
        animateLikes = prefs[KEY_ANIMATE_LIKES] ?: true,
        dimStrength = prefs[KEY_DIM] ?: 1
    )
}