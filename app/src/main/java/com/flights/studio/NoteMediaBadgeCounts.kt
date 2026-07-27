package com.flights.studio

import android.content.Context
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

private fun noteMediaStorageKeys(context: Context, note: String): List<String> {
    val keys = linkedSetOf(note)
    val json = context.getSharedPreferences("notes_uids", Context.MODE_PRIVATE)
        .getString("uid_to_content", "{}")
        .orEmpty()
    runCatching {
        val obj = JSONObject(json.ifBlank { "{}" })
        obj.keys().forEach { uid ->
            if (obj.optString(uid) == note) keys += uid
        }
    }
    return keys.toList()
}
