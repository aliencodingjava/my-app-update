package com.flights.studio

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit

class ReminderActivity : AppCompatActivity() {



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Retrieve the note passed from the previous screen or notification
        val note = intent.getStringExtra("note") ?: "No note available"
// 2️⃣ CLEAR the badge flag for this note
        val badgeKey = note.hashCode().toString()          // ← same key you used elsewhere
        getSharedPreferences("reminder_badges", MODE_PRIVATE)
            .edit {
                putBoolean(badgeKey, false)                   // badge OFF
            }

        setContent {
            FlightsTheme {
                ReminderNotificationScreen(
                    note = note.trim(),
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
}
