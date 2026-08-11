package com.flights.studio

import android.content.Context
import androidx.core.content.edit

internal object ReminderPulseCancellationStore {
    private const val PREFS_NAME = "reminder_pulse_cancellations"

    fun isCancelled(context: Context, note: String): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(note.hashCode().toString(), false)

    fun markCancelled(context: Context, note: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit(commit = true) {
            putBoolean(note.hashCode().toString(), true)
        }
    }

    fun clear(context: Context, note: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit(commit = true) {
            remove(note.hashCode().toString())
        }
    }
}
