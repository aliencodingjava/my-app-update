@file:Suppress("DEPRECATION")

package com.flights.studio

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import android.view.WindowInsetsController
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources

class EditNoteActivity : AppCompatActivity() {

    private var isNoteModified = false // Track if the note is edited

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_note)

        adjustStatusBarIcons()

        // Setup toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
            title = getString(R.string.edit_note)
            setHomeAsUpIndicator(
                AppCompatResources.getDrawable(this@EditNoteActivity, R.drawable.layered_arrow)
            )
        }

        val noteEditText = findViewById<AutoCompleteTextView>(R.id.edit_note)

        // Predefined words or logic for suggestion
        val wordSuggestions = mapOf(
            "hello" to listOf("world", "there", "everyone"),
            "how" to listOf("are you", "is it going", "much"),
            "thank" to listOf("you", "goodness", "heavens"),
            "good" to listOf("morning", "evening", "job"),
            "what" to listOf("is", "are", "happened")
        )

        // Create an adapter for AutoCompleteTextView
        val suggestionAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            emptyList<String>() // Start with an empty list
        )

        noteEditText.setAdapter(suggestionAdapter)

        // Add a text change listener to update suggestions dynamically
        noteEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val text = s.toString()
                val words = text.split(" ")
                val lastWord = words.lastOrNull()?.lowercase() ?: ""

                // Update suggestions based on the last word
                val suggestions = wordSuggestions[lastWord] ?: emptyList()
                suggestionAdapter.clear()
                suggestionAdapter.addAll(suggestions)
                suggestionAdapter.notifyDataSetChanged()

                // Show dropdown if there are suggestions
                if (suggestions.isNotEmpty()) {
                    noteEditText.showDropDown()
                }
            }
        })





        val saveButton = findViewById<Button>(R.id.btn_save)

        // Get intent extras
        val note = intent.getStringExtra("NOTE")
        val position = intent.getIntExtra("NOTE_POSITION", -1)

        // Set note text
        noteEditText.setText(note)

        // Add text change listener to track modifications
        noteEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                isNoteModified = true // Mark note as modified
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        saveButton.setOnClickListener {
            val updatedNote = noteEditText.text.toString()
            if (updatedNote.isNotEmpty()) {
                val resultIntent = Intent().apply {
                    putExtra("UPDATED_NOTE", updatedNote)
                    putExtra("NOTE_POSITION", position)
                }
                setResult(RESULT_OK, resultIntent) // Return the result to the caller
                finish() // Close the activity
                overridePendingTransition(
                    androidx.navigation.ui.R.anim.nav_default_enter_anim,
                    androidx.navigation.ui.R.anim.nav_default_exit_anim
                )
            } else {
                noteEditText.error = "Note cannot be empty"
            }
        }
    }

    companion object {
        fun newIntent(context: Context, note: String, position: Int): Intent {
            return Intent(context, EditNoteActivity::class.java).apply {
                putExtra("NOTE", note)
                putExtra("NOTE_POSITION", position)
            }
        }
    }

    // Handle back navigation for toolbar back button
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                handleBackNavigation()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Handle back navigation for system back button
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        super.onBackPressed()
        handleBackNavigation()
    }

    private fun handleBackNavigation() {
        if (isNoteModified) {
            showExitConfirmationDialog()
        } else {
            finish()
            overridePendingTransition(
                R.anim.m3_motion_fade_enter,
                R.anim.m3_motion_fade_exit
            )
        }
    }

    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(this)
            .setMessage(getString(R.string.confirm_exit_message))
            .setPositiveButton(getString(R.string.exit)) { _, _ ->
                finish()
                overridePendingTransition(
                    R.anim.m3_motion_fade_enter,
                    R.anim.m3_motion_fade_exit
                )
            }
            .setNegativeButton(getString(R.string.cancel), null) // Do nothing on cancel
            .show()
    }

    private fun adjustStatusBarIcons() {
        val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        when (nightModeFlags) {
            Configuration.UI_MODE_NIGHT_YES -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    window.insetsController?.setSystemBarsAppearance(
                        0,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    )
                } else {
                    @Suppress("DEPRECATION")
                    window.decorView.systemUiVisibility = 0
                }
            }
            Configuration.UI_MODE_NIGHT_NO -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    window.insetsController?.setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    )
                } else {
                    @Suppress("DEPRECATION")
                    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                }
            }
        }
    }
}
