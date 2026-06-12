package com.flights.studio

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import androidx.core.net.toUri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class NoteAttachmentItem(
    val uri: String,
    val name: String,
    val mime: String? = null,
    val sizeBytes: Long = 0L,
    val remotePath: String? = null
) {
    val asUri: Uri get() = uri.toUri()
}

data class NoteAttachmentCounts(
    val documents: Int = 0,
    val audio: Int = 0,
    val video: Int = 0
)

private fun NoteAttachmentItem.extension(): String =
    name.substringAfterLast('.', missingDelimiterValue = "").lowercase()

fun NoteAttachmentItem.isAudioAttachment(): Boolean {
    val m = mime.orEmpty().lowercase()
    val ext = extension()
    return m.startsWith("audio/") || ext in setOf("mp3", "m4a", "aac", "wav", "flac", "ogg", "opus", "amr")
}

fun NoteAttachmentItem.isVideoAttachment(): Boolean {
    val m = mime.orEmpty().lowercase()
    val ext = extension()
    return m.startsWith("video/") || ext in setOf("mp4", "mov", "m4v", "mkv", "webm", "avi", "3gp")
}

fun countNoteAttachments(items: List<NoteAttachmentItem>): NoteAttachmentCounts {
    var documents = 0
    var audio = 0
    var video = 0
    items.forEach { item ->
        when {
            item.isAudioAttachment() -> audio++
            item.isVideoAttachment() -> video++
            else -> documents++
        }
    }
    return NoteAttachmentCounts(documents = documents, audio = audio, video = video)
}

object NoteAttachmentStore {
    private const val PREF = "note_file_attachments"
    private const val KEY_MAP = "map"

    private val gson = Gson()
    private val type = object : TypeToken<MutableMap<String, MutableList<NoteAttachmentItem>>>() {}.type

    private fun readMap(ctx: Context): MutableMap<String, MutableList<NoteAttachmentItem>> {
        val json = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY_MAP, "{}") ?: "{}"
        return try {
            gson.fromJson<MutableMap<String, MutableList<NoteAttachmentItem>>>(json, type) ?: mutableMapOf()
        } catch (_: Exception) {
            mutableMapOf()
        }
    }

    private fun writeMap(ctx: Context, map: MutableMap<String, MutableList<NoteAttachmentItem>>) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit {
            putString(KEY_MAP, gson.toJson(map))
        }
    }

    fun getItems(ctx: Context, noteKey: String): MutableList<NoteAttachmentItem> =
        readMap(ctx)[noteKey]?.toMutableList() ?: mutableListOf()

    fun setItems(ctx: Context, noteKey: String, items: List<NoteAttachmentItem>) {
        val map = readMap(ctx)
        if (items.isEmpty()) map.remove(noteKey) else map[noteKey] = items.toMutableList()
        writeMap(ctx, map)
    }

    fun updateRemotePath(ctx: Context, noteKey: String, uri: String, remotePath: String) {
        val map = readMap(ctx)
        val items = map[noteKey] ?: return
        map[noteKey] = items.map {
            if (it.uri == uri) it.copy(remotePath = remotePath) else it
        }.toMutableList()
        writeMap(ctx, map)
    }

    fun migrateNoteKey(ctx: Context, oldNote: String, newNote: String) {
        if (oldNote == newNote) return
        val map = readMap(ctx)
        val existing = map.remove(oldNote) ?: return
        val target = map.getOrPut(newNote) { mutableListOf() }
        existing.forEach { item -> if (target.none { it.uri == item.uri }) target.add(item) }
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
