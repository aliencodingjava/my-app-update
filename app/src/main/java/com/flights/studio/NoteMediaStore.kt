package com.flights.studio

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import androidx.core.net.toUri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Persists image URIs per note text in SharedPreferences.
 * Key = note text, Value = ordered list of URI strings.
 */
object NoteMediaStore {

    private const val PREF = "note_media"
    private const val KEY_MAP = "map" // Map<String noteText, List<String uri>>

    private val gson = Gson()
    private val type = object : TypeToken<MutableMap<String, MutableList<String>>>() {}.type

    private fun readMap(ctx: Context): MutableMap<String, MutableList<String>> {
        val sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val json = sp.getString(KEY_MAP, "{}") ?: "{}"
        return try {
            gson.fromJson<MutableMap<String, MutableList<String>>>(json, type) ?: mutableMapOf()
        } catch (_: Exception) {
            mutableMapOf()
        }
    }

    private fun writeMap(ctx: Context, map: MutableMap<String, MutableList<String>>) {
        val sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        sp.edit { putString(KEY_MAP, gson.toJson(map)) }
    }

    /** Returns a mutable, ordered list of URIs associated with this note. */
    fun getUris(ctx: Context, noteText: String): MutableList<Uri> {
        val list = readMap(ctx)[noteText]?.toMutableList() ?: mutableListOf()
        return list.map { it.toUri() }.toMutableList()
    }

    /** Replaces the entire URI list for a note (order is preserved). */
    fun setUris(ctx: Context, noteText: String, uris: List<Uri>) {
        val map = readMap(ctx)
        map[noteText] = uris.map { it.toString() }.toMutableList()
        writeMap(ctx, map)
    }

    /** Appends a URI if not already present (preserves order). */
    fun addUri(ctx: Context, noteText: String, uri: Uri) {
        val map = readMap(ctx)
        val list = map.getOrPut(noteText) { mutableListOf() }
        val asString = uri.toString()
        if (asString !in list) list.add(asString)
        writeMap(ctx, map)
    }

    /** Removes the URI from a note; deletes the key if empty afterward. */
    fun removeUri(ctx: Context, noteText: String, uri: Uri) {
        val map = readMap(ctx)
        val list = map[noteText] ?: return
        list.remove(uri.toString())
        if (list.isEmpty()) map.remove(noteText)
        writeMap(ctx, map)
    }

    /**
     * Call when the note TEXT changes so images follow the note.
     * If the new note already has images, this appends missing ones (keeps order).
     */
    fun migrateNoteKey(ctx: Context, oldNote: String, newNote: String) {
        if (oldNote == newNote) return
        val map = readMap(ctx)
        val existing = map.remove(oldNote) ?: return
        val target = map.getOrPut(newNote) { mutableListOf() }
        // Append only non-duplicates, maintaining original order
        existing.forEach { if (it !in target) target.add(it) }
        writeMap(ctx, map)
    }

    /** Removes all images for a given note. */
    fun deleteAllForNote(ctx: Context, noteText: String) {
        val map = readMap(ctx)
        if (map.remove(noteText) != null) writeMap(ctx, map)
    }
}
