package com.flights.studio

import android.content.Intent
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.TranslateAnimation
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat

class OpenSplash : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.opensplash_screen)

        val imageViewLogo = findViewById<ImageView>(R.id.imageViewLogo)
        val textViewAnimated = findViewById<TextView>(R.id.textViewAnimated)

        // Set visibility of the text
        textViewAnimated.visibility = View.VISIBLE

        // Wait until the TextView is laid out to get its actual height for the gradient
        textViewAnimated.viewTreeObserver.addOnGlobalLayoutListener {
            // Apply the gradient shader to the TextView
            val textShader = LinearGradient(
                0f, 0f, 0f, textViewAnimated.height.toFloat(),  // Use the height of the TextView for a smooth gradient
                intArrayOf(
                    android.graphics.Color.BLUE,   // Top color (Sky blue)
                    android.graphics.Color.GRAY,   // Middle color (Mountain gray)
                    android.graphics.Color.BLACK   // Bottom color (Soil black)
                ),
                null,
                Shader.TileMode.CLAMP  // No repetition, just a clean gradient
            )
            textViewAnimated.paint.shader = textShader

            // Invalidate to apply the shader
            textViewAnimated.invalidate()
        }


        // Set the glow effect using shadow layer
        textViewAnimated.setShadowLayer(10f, 0f, 0f, android.graphics.Color.YELLOW)

        // Set up the glow pulsing animation
        val glowAnimation = AlphaAnimation(0.1f, 1f)
        glowAnimation.duration = 100
        glowAnimation.repeatMode = Animation.REVERSE
        glowAnimation.repeatCount = Animation.INFINITE

        // Start glowing animation
        textViewAnimated.startAnimation(glowAnimation)

        // Set up the fade-in animation for the TextView (optional)
        val fadeAnimation = AlphaAnimation(0f, 1f)
        fadeAnimation.duration = 1500L
        fadeAnimation.startOffset = 100L
        textViewAnimated.startAnimation(fadeAnimation)

        // Translate animation for the ImageView
        val translateAnimation = TranslateAnimation(
            TranslateAnimation.RELATIVE_TO_PARENT, 0f,
            TranslateAnimation.RELATIVE_TO_PARENT, 0f,
            TranslateAnimation.RELATIVE_TO_PARENT, 1f,
            TranslateAnimation.RELATIVE_TO_PARENT, -0.4f
        )
        translateAnimation.duration = 1500L
        translateAnimation.startOffset = 100L
        translateAnimation.fillAfter = true

        // Fade-out animation for the ImageView
        val fadeOutAnimation = AlphaAnimation(1f, 0f)
        fadeOutAnimation.duration = 500L
        fadeOutAnimation.startOffset = 1000L

        // Combine the translate and fade-out animations for the ImageView
        val animationSet = AnimationSet(true)
        animationSet.addAnimation(translateAnimation)
        animationSet.addAnimation(fadeOutAnimation)
        animationSet.fillAfter = true

        // Set animation listener
        animationSet.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}

            override fun onAnimationEnd(animation: Animation) {
                // Start next activity
                val intent = Intent(this@OpenSplash, SplashActivity::class.java)

                val options = ActivityOptionsCompat.makeCustomAnimation(
                    this@OpenSplash,
                    com.google.android.material.R.anim.m3_motion_fade_enter,
                    R.anim.m3_motion_fade_exit
                )

                startActivity(intent, options.toBundle())
                finishAfterTransition()
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })

        // Start the combined animation for the ImageView
        imageViewLogo.startAnimation(animationSet)
    }
}
