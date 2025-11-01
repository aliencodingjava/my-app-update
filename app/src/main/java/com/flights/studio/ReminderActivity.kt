package com.flights.studio

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ReminderActivity : AppCompatActivity() {



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reminder)

        // Retrieve the note passed from the previous screen or notification
        val note = intent.getStringExtra("note") ?: "No note available"
// 2️⃣ CLEAR the badge flag for this note
        val badgeKey = note.hashCode().toString()          // ← same key you used elsewhere
        getSharedPreferences("reminder_badges", MODE_PRIVATE)
            .edit {
                putBoolean(badgeKey, false)                   // badge OFF
            }
        // ⬇️ 1.  Don’t split any more – keep the whole note in one element
        val noteLines = listOf(note.trim())

        // Set up the RecyclerView to display the note lines
        val noteLinesRecyclerView = findViewById<RecyclerView>(R.id.noteLinesRecyclerView)
        noteLinesRecyclerView.layoutManager = LinearLayoutManager(this)
        noteLinesRecyclerView.adapter = NoteLinesAdapter(noteLines)

        noteLinesRecyclerView.addItemDecoration(
            object : RecyclerView.ItemDecoration() {
                private val spacePx = (8 * resources.displayMetrics.density).toInt() // 8 dp
                override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                    if (parent.getChildAdapterPosition(view) == state.itemCount - 1) outRect.bottom = spacePx
                }
            }
        )


        // Set up the Home button to navigate back to home (SplashActivity in this example)
        val homeButton: ImageButton = findViewById(R.id.homeButton)
        homeButton.setOnClickListener {
            val homeIntent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(homeIntent)
            finish()
        }
    }
}
