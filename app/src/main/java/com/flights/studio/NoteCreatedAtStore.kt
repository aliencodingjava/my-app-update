package com.flights.studio

import android.content.Context
import androidx.core.content.edit
import java.time.Instant

object NoteCreatedAtStore {
    private const val PREFS_NAME = "note_created_at"
    private const val MAP_KEY = "created_at_by_note_key"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun readMap(context: Context): MutableMap<String, Long> {
        val raw = prefs(context).getStringSet(MAP_KEY, emptySet()).orEmpty()
        return raw.mapNotNull { entry ->
            val divider = entry.indexOf('|')
            if (divider <= 0) return@mapNotNull null
            val key = entry.take(divider)
            val value = entry.drop(divider + 1).toLongOrNull() ?: return@mapNotNull null
            key to value
        }.toMap().toMutableMap()
    }

    private fun writeMap(context: Context, values: Map<String, Long>) {
        prefs(context).edit {
            putStringSet(MAP_KEY, values.map { (key, value) -> "$key|$value" }.toSet())
        }
    }

    fun get(context: Context, noteKey: String): Long? =
        readMap(context)[noteKey]?.takeIf { it > 0L }

    fun ensure(context: Context, noteKey: String, fallbackMs: Long = System.currentTimeMillis()): Long {
        val values = readMap(context)
        values[noteKey]?.takeIf { it > 0L }?.let { return it }
        values[noteKey] = fallbackMs
        writeMap(context, values)
        return fallbackMs
    }

    fun setIfAbsent(context: Context, noteKey: String, createdAtMs: Long) {
        if (createdAtMs <= 0L) return
        val values = readMap(context)
        if ((values[noteKey] ?: 0L) <= 0L) {
            values[noteKey] = createdAtMs
            writeMap(context, values)
        }
    }

    fun remove(context: Context, noteKey: String) {
        val values = readMap(context)
        if (values.remove(noteKey) != null) writeMap(context, values)
    }

    fun parseSupabaseTimestamp(value: String?): Long? =
        value
            ?.takeIf { it.isNotBlank() }
            ?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
}
