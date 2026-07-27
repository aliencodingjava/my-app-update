package com.flights.studio

import android.content.Context
import android.net.Uri
import org.json.JSONObject

data class NoteMediaBadgeCounts(
    val images: Int = 0,
    val documents: Int = 0,
    val audio: Int = 0,
    val video: Int = 0
)

fun noteMediaBadgeCounts(context: Context, note: String): NoteMediaBadgeCounts {
    val keys = noteMediaStorageKeys(context, note)
    val images = keys
        .flatMap { key -> NoteMediaStore.getUris(context, key) }
        .distinctBy { it.toString() }
        .size
    val attachments = keys
        .flatMap { key -> NoteAttachmentStore.getItems(context, key) }
        .distinctBy { "${it.uri}|${it.name}|${it.remotePath.orEmpty()}" }
    val attachmentCounts = countNoteAttachments(attachments)
    val voiceCount = keys
        .flatMap { key -> NoteVoiceStore.getItems(context, key) }
        .distinctBy { "${it.uri}|${it.createdAtMs}" }
        .size

    return NoteMediaBadgeCounts(
        images = images,
        documents = attachmentCounts.documents,
        audio = attachmentCounts.audio + voiceCount,
        video = attachmentCounts.video
    )
}

fun saveNoteMediaForKeys(
    context: Context,
    note: String,
    noteKey: String,
    imageUris: List<Uri>? = null,
    attachments: List<NoteAttachmentItem>? = null,
    voiceItems: List<NoteVoiceItem>? = null
) {
    linkedSetOf(note, noteKey)
        .filter { it.isNotBlank() }
        .forEach { key ->
            imageUris?.let { NoteMediaStore.setUris(context, key, it) }
            attachments?.let { NoteAttachmentStore.setItems(context, key, it) }
            voiceItems?.let { NoteVoiceStore.setItems(context, key, it) }
        }
}

fun noteMediaStorageKeys(context: Context, note: String): List<String> {
    val keys = linkedSetOf(note)
    val json = context.getSharedPreferences("notes_uids", Context.MODE_PRIVATE)
        .getString("uid_to_content", "{}")
        .orEmpty()
    runCatching {
        val obj = JSONObject(json.ifBlank { "{}" })
        obj.keys().forEach { uid ->
            val content = obj.optString(uid)
            if (content == note) keys += uid
            if (uid == note && content.isNotBlank()) keys += content
        }
    }
    return keys.toList()
}
