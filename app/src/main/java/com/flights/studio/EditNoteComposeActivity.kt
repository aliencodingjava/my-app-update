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

        val startReminder = intent.getBooleanExtra("NOTE_WANTS_REMINDER", false)

        setContent {
            FlightsTheme {
                EditNoteScreen(
                    initialTitle = startTitle,
                    initialNote = startNote,
                    initialImages = startImages,
                    initialWantsReminder = startReminder,
                    onBack = { finish() },
                    onSave = { note, title, images, wantsReminder ->
                        val result = Intent().apply {
                            putExtra("UPDATED_NOTE", note)
                            putExtra("UPDATED_TITLE", title.trim())
                            putStringArrayListExtra(
                                "UPDATED_IMAGES",
                                ArrayList(images.map { it.toString() })
                            )
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
