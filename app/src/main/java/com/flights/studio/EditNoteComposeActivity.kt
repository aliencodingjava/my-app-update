package com.flights.studio

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.net.toUri
import androidx.core.view.WindowCompat

class EditNoteComposeActivity : ComponentActivity() {

    companion object {
        fun newIntent(
            context: Context,
            note: String,
            title: String?,                 // ✅ nullable now
            images: List<Uri>,
            attachments: List<NoteAttachmentItem> = emptyList(),
            voiceNotes: List<NoteVoiceItem> = emptyList(),
            wantsReminder: Boolean,
            position: Int
        ): Intent {
            return Intent(context, EditNoteComposeActivity::class.java).apply {
                putExtra("NOTE_TEXT", note)
                putExtra("NOTE_TITLE", title ?: "")  // ✅ store empty if null
                putExtra("NOTE_POSITION", position)
                putStringArrayListExtra(
                    "NOTE_IMAGES",
                    ArrayList(images.map { it.toString() })
                )
                putStringArrayListExtra(
                    "NOTE_FILE_URIS",
                    ArrayList(attachments.map { it.uri })
                )
                putStringArrayListExtra(
                    "NOTE_FILE_NAMES",
                    ArrayList(attachments.map { it.name })
                )
                putStringArrayListExtra(
                    "NOTE_FILE_MIMES",
                    ArrayList(attachments.map { it.mime.orEmpty() })
                )
                putExtra("NOTE_FILE_SIZES", attachments.map { it.sizeBytes }.toLongArray())
                putStringArrayListExtra(
                    "NOTE_VOICE_URIS",
                    ArrayList(voiceNotes.map { it.uri })
                )
                putExtra("NOTE_VOICE_DURATIONS", voiceNotes.map { it.durationMs }.toLongArray())
                putExtra("NOTE_VOICE_CREATED_AT", voiceNotes.map { it.createdAtMs }.toLongArray())
                putExtra("NOTE_WANTS_REMINDER", wantsReminder)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val startNote = intent.getStringExtra("NOTE_TEXT").orEmpty()
        val startTitle = intent.getStringExtra("NOTE_TITLE").orEmpty()
        val startPos = intent.getIntExtra("NOTE_POSITION", -1)

        val startImages = intent.getStringArrayListExtra("NOTE_IMAGES")
            ?.mapNotNull { runCatching { it.toUri() }.getOrNull() }
            .orEmpty()
        val fileUris = intent.getStringArrayListExtra("NOTE_FILE_URIS").orEmpty()
        val fileNames = intent.getStringArrayListExtra("NOTE_FILE_NAMES").orEmpty()
        val fileMimes = intent.getStringArrayListExtra("NOTE_FILE_MIMES").orEmpty()
        val fileSizes = intent.getLongArrayExtra("NOTE_FILE_SIZES") ?: longArrayOf()
        val startAttachments = fileUris.mapIndexed { index, uri ->
            NoteAttachmentItem(
                uri = uri,
                name = fileNames.getOrNull(index).orEmpty().ifBlank { "Attachment ${index + 1}" },
                mime = fileMimes.getOrNull(index)?.takeIf { it.isNotBlank() },
                sizeBytes = fileSizes.getOrNull(index) ?: 0L
            )
        }
        val voiceUris = intent.getStringArrayListExtra("NOTE_VOICE_URIS").orEmpty()
        val voiceDurations = intent.getLongArrayExtra("NOTE_VOICE_DURATIONS") ?: longArrayOf()
        val voiceCreatedAt = intent.getLongArrayExtra("NOTE_VOICE_CREATED_AT") ?: longArrayOf()
        val startVoiceNotes = voiceUris.mapIndexed { index, uri ->
            NoteVoiceItem(
                uri = uri,
                durationMs = voiceDurations.getOrNull(index) ?: 0L,
                createdAtMs = voiceCreatedAt.getOrNull(index) ?: System.currentTimeMillis()
            )
        }

        val startReminder = intent.getBooleanExtra("NOTE_WANTS_REMINDER", false)

        setContent {
            FlightsTheme {
                EditNoteScreen(
                    initialTitle = startTitle,
                    initialNote = startNote,
                    initialImages = startImages,
                    initialAttachments = startAttachments,
                    initialVoiceNotes = startVoiceNotes,
                    initialWantsReminder = startReminder,
                    onBack = { finish() },
                    onSave = { note, title, images, attachments, voiceNotes, wantsReminder ->
                        val result = Intent().apply {
                            putExtra("UPDATED_NOTE", note)
                            putExtra("UPDATED_TITLE", title.trim())
                            putStringArrayListExtra(
                                "UPDATED_IMAGES",
                                ArrayList(images.map { it.toString() })
                            )
                            putStringArrayListExtra("UPDATED_FILE_URIS", ArrayList(attachments.map { it.uri }))
                            putStringArrayListExtra("UPDATED_FILE_NAMES", ArrayList(attachments.map { it.name }))
                            putStringArrayListExtra("UPDATED_FILE_MIMES", ArrayList(attachments.map { it.mime.orEmpty() }))
                            putExtra("UPDATED_FILE_SIZES", attachments.map { it.sizeBytes }.toLongArray())
                            putStringArrayListExtra("UPDATED_VOICE_URIS", ArrayList(voiceNotes.map { it.uri }))
                            putExtra("UPDATED_VOICE_DURATIONS", voiceNotes.map { it.durationMs }.toLongArray())
                            putExtra("UPDATED_VOICE_CREATED_AT", voiceNotes.map { it.createdAtMs }.toLongArray())
                            putExtra("UPDATED_NOTE_WANTS_REMINDER", wantsReminder)
                            putExtra("NOTE_POSITION", startPos)
                        }
                        setResult(RESULT_OK, result)
                        finish()
                    }
                )
            }
        }
    }
}
