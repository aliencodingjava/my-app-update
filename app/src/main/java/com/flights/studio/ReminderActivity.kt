package com.flights.studio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder

class ReminderActivity : AppCompatActivity() {

    private val notesHttpClient by lazy { OkHttpClient() }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Retrieve the note passed from the previous screen or notification
        val note = intent.getStringExtra("note") ?: "No note available"
        completeReminder(note)

        setContent {
            FlightsTheme {
                ReminderNotificationScreen(
                    note = note.trim(),
                    onOpenNote = {
                        startActivity(
                            ViewNoteComposeActivity.newIntent(
                                context = this,
                                uid = reminderNoteUid(note) ?: note,
                                note = note,
                                position = -1,
                                title = null
                            )
                        )
                    },
                    onHome = {
                        val homeIntent = Intent(this, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        startActivity(homeIntent)
                        finish()
                    }
                )
            }
        }
    }

    private fun reminderNoteUid(note: String): String? {
        val json = getSharedPreferences("notes_uids", Context.MODE_PRIVATE)
            .getString("uid_to_content", "{}")
            .orEmpty()
        return runCatching {
            val obj = JSONObject(json.ifBlank { "{}" })
            obj.keys().asSequence().firstOrNull { uid -> obj.optString(uid) == note }
        }.getOrNull()
    }

    private fun completeReminder(note: String) {
        val key = note.hashCode().toString()
        val meta = getSharedPreferences("reminder_meta", MODE_PRIVATE)
        val noteKey = meta.getString("${key}_note_key", null)

        getSharedPreferences("reminder_badges", MODE_PRIVATE).edit(commit = true) {
            putBoolean(key, false)
        }
        meta.edit(commit = true) {
            remove("${key}_trigger_at")
            remove("${key}_work_id")
            remove("${key}_note_key")
            remove("${key}_note")
        }
        if (!noteKey.isNullOrBlank()) {
            getSharedPreferences("reminder_notes", MODE_PRIVATE).edit(commit = true) {
                remove(noteKey)
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            clearReminderInSupabase(note)
        }
    }

    private fun clearReminderInSupabase(note: String) {
        runCatching {
            val session = SupabaseManager.client.auth.currentSessionOrNull() ?: return
            val userId = session.user?.id ?: return
            val body = JSONObject()
                .put("has_reminder", true)
                .put("has_reminder_badge", false)
                .toString()
                .toRequestBody("application/json".toMediaType())
            val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
            val request = Request.Builder()
                .url(
                    "$baseUrl/rest/v1/user_notes" +
                        "?user_id=eq.${urlEncode(userId)}" +
                        "&content=eq.${urlEncode(note)}"
                )
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer ${session.accessToken}")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .patch(body)
                .build()

            notesHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("Clear reminder failed: ${response.code}")
                }
            }
        }
    }

    private fun urlEncode(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")
}
