package com.flights.studio

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import androidx.core.net.toUri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class NoteVoiceItem(
    val uri: String,
    val durationMs: Long,
    val createdAtMs: Long
) {
    val asUri: Uri get() = uri.toUri()
}

object NoteVoiceStore {
    private const val PREF = "note_voice_media"
    private const val KEY_MAP = "map"

    private val gson = Gson()
    private val type = object : TypeToken<MutableMap<String, MutableList<NoteVoiceItem>>>() {}.type

    private fun readMap(ctx: Context): MutableMap<String, MutableList<NoteVoiceItem>> {
        val sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val json = sp.getString(KEY_MAP, "{}") ?: "{}"
        return try {
            gson.fromJson<MutableMap<String, MutableList<NoteVoiceItem>>>(json, type) ?: mutableMapOf()
        } catch (_: Exception) {
            mutableMapOf()
        }
    }

    private fun writeMap(ctx: Context, map: MutableMap<String, MutableList<NoteVoiceItem>>) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit {
            putString(KEY_MAP, gson.toJson(map))
        }
    }

    fun getItems(ctx: Context, noteKey: String): MutableList<NoteVoiceItem> =
        readMap(ctx)[noteKey]?.toMutableList() ?: mutableListOf()

    fun setItems(ctx: Context, noteKey: String, items: List<NoteVoiceItem>) {
        val map = readMap(ctx)
        if (items.isEmpty()) map.remove(noteKey) else map[noteKey] = items.toMutableList()
        writeMap(ctx, map)
    }

    fun migrateNoteKey(ctx: Context, oldNote: String, newNote: String) {
        if (oldNote == newNote) return
        val map = readMap(ctx)
        val existing = map.remove(oldNote) ?: return
        val target = map.getOrPut(newNote) { mutableListOf() }
        existing.forEach { item ->
            if (target.none { it.uri == item.uri }) target.add(item)
        }
        writeMap(ctx, map)
    }

    fun deleteAllForNote(ctx: Context, noteKey: String) {
        val map = readMap(ctx)
        val removed = map.remove(noteKey).orEmpty()
        removed.forEach { item ->
            runCatching {
                val uri = item.asUri
                if (uri.scheme == "file") java.io.File(uri.path.orEmpty()).delete()
            }
        }
        if (removed.isNotEmpty()) writeMap(ctx, map)
    }
}
