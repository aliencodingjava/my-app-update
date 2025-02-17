@file:Suppress("DEPRECATION")

package com.flights.studio

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.lifecycleScope
import com.flights.studio.databinding.ActivityViewNoteBinding
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.*

class ViewNoteActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_NOTE = "extra_note"
        private const val EXTRA_POSITION = "extra_position"
        private const val TAG = "ViewNoteActivity"
        private const val CLICK_DELAY = 500L

        fun newIntent(context: Context, note: String, position: Int): Intent {
            return Intent(context, ViewNoteActivity::class.java).apply {
                putExtra(EXTRA_NOTE, note)
                putExtra(EXTRA_POSITION, position)
            }
        }
    }

    private lateinit var binding: ActivityViewNoteBinding
    private lateinit var editNoteLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adjustStatusBarIcons()
        setupToolbar(binding.toolbar, R.drawable.layered_arrow)

        // Register ActivityResultLauncher
        editNoteLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                val updatedNote = data?.getStringExtra(getString(R.string.updated_note_key))
                val position = data?.getIntExtra(getString(R.string.note_position_key), -1) ?: -1
                if (updatedNote != null && position != -1) {
                    noteIsModified = true // Mark as modified
                    val resultIntent = Intent().apply {
                        putExtra(getString(R.string.updated_note_key), updatedNote)
                        putExtra(getString(R.string.note_position_key), position)
                    }
                    setResult(RESULT_OK, resultIntent)
                    finish()
                    applyTransition()
                }
            }
        }

        // Get the note and position from the intent
        val note = intent.getStringExtra(EXTRA_NOTE) ?: ""
        val position = intent.getIntExtra(EXTRA_POSITION, -1)

        if (note.isEmpty() || position == -1) {
            Log.w(TAG, "Invalid note or position: note=$note, position=$position")
            Toast.makeText(this, getString(R.string.error_loading_note), Toast.LENGTH_SHORT).show()
            finish()
            applyTransition()
            return
        }

        // Display the note
        binding.tvNoteContent.text = note

        // Add click listener to edit icon
        binding.expandCollapseIcon.setOnClickListener {
            it.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).withEndAction {
                it.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                preventDoubleClick(it)
                Log.d(TAG, "Launching edit activity for position: $position")
                val editIntent = EditNoteActivity.newIntent(this, note, position)
                editNoteLauncher.launch(editIntent)
                applyTransition()
            }.start()
        }


        Log.i(TAG, "Activity created with note: $note and position: $position")
    }
    private var noteIsModified = false



    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                if (noteIsModified) {
                    showExitConfirmationDialog() // Show the dialog if changes exist
                } else {
                    finish() // Close the activity directly if no changes
                    applyTransition()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }




    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(this)
            .setMessage(getString(R.string.confirm_exit_message))
            .setPositiveButton(getString(R.string.exit)) { _, _ ->
                finish()
                applyTransition()
            }
            .setNegativeButton(getString(R.string.cancel), null)
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

    private fun setupToolbar(toolbar: MaterialToolbar, iconResId: Int) {
        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(AppCompatResources.getDrawable(this@ViewNoteActivity, iconResId))
        }
    }

    private fun applyTransition() {
        overridePendingTransition(R.anim.m3_motion_fade_enter, R.anim.m3_motion_fade_exit)
    }

    private fun preventDoubleClick(view: View) {
        view.isEnabled = false
        lifecycleScope.launch {
            delay(CLICK_DELAY)
            view.isEnabled = true
        }
    }


}
