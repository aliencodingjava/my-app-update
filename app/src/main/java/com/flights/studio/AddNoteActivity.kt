package com.flights.studio

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

@Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
class AddNoteActivity : AppCompatActivity() {

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_note)
        adjustStatusBarIcons()



        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
            setHomeAsUpIndicator(AppCompatResources.getDrawable(this@AddNoteActivity, R.drawable.layered_arrow))
        }

        val noteEditText = findViewById<TextInputEditText>(R.id.et_note)

        val saveButton = findViewById<MaterialButton>(R.id.btn_save)

        saveButton.setOnClickListener {
            val noteText = noteEditText.text.toString()
            if (noteText.isNotEmpty()) {
                val resultIntent = Intent()
                resultIntent.putExtra("NEW_NOTE", noteText)
                setResult(RESULT_OK, resultIntent)
                finish() // Close the AddNoteActivity
                overridePendingTransition(androidx.navigation.ui.R.anim.nav_default_enter_anim, androidx.navigation.ui.R.anim.nav_default_exit_anim) // Exit animation

            } else {
                noteEditText.error = "Note cannot be empty"
            }
        }


    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, AddNoteActivity::class.java)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                finish()
                overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, Contact::class.java)
        startActivity(intent)
        finish()
        overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation)
    }
    private fun adjustStatusBarIcons() {
        val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        when (nightModeFlags) {
            Configuration.UI_MODE_NIGHT_YES -> {
                // Dark mode: Light status bar icons/text
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    window.insetsController?.setSystemBarsAppearance(
                        0, // Clear light status bar appearance
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    )
                } else {
                    @Suppress("DEPRECATION")
                    window.decorView.systemUiVisibility = 0 // Clear light status bar flag
                }
            }
            Configuration.UI_MODE_NIGHT_NO -> {
                // Light mode: Dark status bar icons/text
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