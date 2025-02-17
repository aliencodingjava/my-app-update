package com.flights.studio

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.WindowInsetsController
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.text.HtmlCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton

@Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
class LicensesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_licenses)

        adjustStatusBarIcons()


        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Enable back arrow
        supportActionBar?.setHomeButtonEnabled(true)
        val drawable = AppCompatResources.getDrawable(
            this,
            R.drawable.layered_arrow
        ) // Use the layered drawable
        supportActionBar?.setHomeAsUpIndicator(drawable)

        val fab = findViewById<FloatingActionButton>(R.id.logo)

        fab.setOnClickListener {
            // Zoom in
            val scaleUpX = ObjectAnimator.ofFloat(fab, "scaleX", 1f, 1.5f)
            val scaleUpY = ObjectAnimator.ofFloat(fab, "scaleY", 1f, 1.5f)

            // Rotation (flip 3 times)
            val flip = ObjectAnimator.ofFloat(fab, "rotationY", 0f, 360f, 720f, 1080f) // 3 full flips (360 * 3)

            // Zoom out back to original position
            val scaleDownX = ObjectAnimator.ofFloat(fab, "scaleX", 1.5f, 1f)
            val scaleDownY = ObjectAnimator.ofFloat(fab, "scaleY", 1.5f, 1f)

            // Set durations for each animation
            scaleUpX.duration = 300
            scaleUpY.duration = 300
            flip.duration = 1500 // Total duration for 3 flips (adjust as needed)
            scaleDownX.duration = 300
            scaleDownY.duration = 300

            // Combine the animations
            val animatorSet = AnimatorSet()
            animatorSet.play(scaleUpX).with(scaleUpY) // Play zoom in simultaneously
            animatorSet.play(flip).after(scaleUpX) // Play flip after zoom in
            animatorSet.play(scaleDownX).with(scaleDownY).after(flip) // Play zoom out after flip

            // Start the animation
            animatorSet.start()
        }



        // Optionally, you can dynamically set the licenses text if needed
        val licensesTextView = findViewById<TextView>(R.id.licensesTextView)
        licensesTextView.text = HtmlCompat.fromHtml(getString(R.string.licenses_content), HtmlCompat.FROM_HTML_MODE_LEGACY)

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
        val intent = Intent(this, SettingsActivity::class.java)
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
