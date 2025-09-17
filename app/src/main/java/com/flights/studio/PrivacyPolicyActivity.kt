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
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.text.HtmlCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton

@Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
class PrivacyPolicyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_privacy_policy)

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
            // Zoom in smoothly over 2 seconds
            val zoomInX = ObjectAnimator.ofFloat(fab, "scaleX", 1f, 1.5f)
            val zoomInY = ObjectAnimator.ofFloat(fab, "scaleY", 1f, 1.5f)
            zoomInX.duration = 2000
            zoomInY.duration = 2000

            // Fast bounce back (with force)
            val bounceBackX = ObjectAnimator.ofFloat(fab, "scaleX", 1.5f, 1.1f, 1f)
            val bounceBackY = ObjectAnimator.ofFloat(fab, "scaleY", 1.5f, 1.1f, 1f)
            bounceBackX.duration = 50
            bounceBackY.duration = 50

            // Add an optional bounce effect at the end for strength
            val bounceEffect = ObjectAnimator.ofFloat(fab, "translationY", 0f, -50f, 0f)
            bounceEffect.duration = 600
            bounceEffect.interpolator = OvershootInterpolator() // Overshoot effect for bounce

            // Create an AnimatorSet to play animations in sequence
            val animatorSet = AnimatorSet()

            // Play the zoom-in first
            animatorSet.play(zoomInX).with(zoomInY)

            // Play the bounce-back after the zoom-in
            animatorSet.play(bounceBackX).with(bounceBackY).after(zoomInX)

            // Play the bounce effect after the bounce-back for extra strength
            animatorSet.play(bounceEffect).after(bounceBackX)

            // Start the animation
            animatorSet.start()
        }


        // Optionally, you can dynamically set the privacy policy text if needed
        // For example, you can fetch the content from a file, API, or string resource
        val privacyPolicyTextView = findViewById<TextView>(R.id.privacyPolicyTextView)
        privacyPolicyTextView.text = HtmlCompat.fromHtml(getString(R.string.privacy_policy_goes_here), HtmlCompat.FROM_HTML_MODE_LEGACY)



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

