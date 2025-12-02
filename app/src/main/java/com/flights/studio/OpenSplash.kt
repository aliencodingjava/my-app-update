package com.flights.studio

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnticipateOvershootInterpolator
import android.view.animation.PathInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
class OpenSplash : AppCompatActivity() {

    private lateinit var logo: ImageView
    private lateinit var textView: TextView

    /** handle for the infinite ‘breathing’ loop */
    private var breathingSet: AnimatorSet? = null

    /** prevents exitSequence() from running twice */
    private var exited = false

    /** coroutine that schedules the exit */
    private var exitJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupImmersiveMode()
        setContentView(R.layout.opensplash_screen)

        logo = findViewById(R.id.logo)
        textView = findViewById(R.id.welcome_text)

        logo.setOnClickListener {
            exitJob?.cancel()
            exitSequence()
        }

        startEntrance()
    }

    /* ───────── immersive bar setup ───────── */
    private fun setupImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).run {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    /* ───────── tiny view helpers ───────── */
    private fun View.prepareEntrance(
        alpha0: Float, ty: Float, sx: Float = 1f, rot: Float = 0f
    ) = apply {
        alpha = alpha0
        translationY = ty
        scaleX = sx
        scaleY = sx
        rotation = rot
    }
    private fun View.withLayer() = apply { animate().withLayer() }

    /* subtler than stock Overshoot */
    private val easeOutBack = PathInterpolator(0.22f, 1.45f, 0.32f, 1f)

    /* ───────── splash IN ───────── */
    private fun startEntrance() {
        logo.prepareEntrance(0f, -100f, 0.3f, -15f)
        textView.prepareEntrance(0f, 100f, 0.5f)

        val logoAnim = ObjectAnimator.ofPropertyValuesHolder(
            logo.withLayer(),
            PropertyValuesHolder.ofFloat(View.ALPHA, 0f, 1f),
            PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, -100f, 0f),
            PropertyValuesHolder.ofFloat(View.SCALE_X, 0.3f, 1f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.3f, 1f),
            PropertyValuesHolder.ofFloat(View.ROTATION, -15f, 0f)
        ).apply {
            duration = 550L
            interpolator = easeOutBack
        }

        val textAnim = ObjectAnimator.ofPropertyValuesHolder(
            textView.withLayer(),
            PropertyValuesHolder.ofFloat(View.ALPHA, 0f, 1f),
            PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 100f, 0f),
            PropertyValuesHolder.ofFloat(View.SCALE_X, 0.5f, 1f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.5f, 1f)
        ).apply {
            duration = 450L
            startDelay = 180L
            interpolator = AnticipateOvershootInterpolator(0.9f, 1.2f)
        }

        AnimatorSet().apply {
            playTogether(logoAnim, textAnim)
            doOnEnd {
                logo.setLayerType(View.LAYER_TYPE_NONE, null)
                textView.setLayerType(View.LAYER_TYPE_NONE, null)
                startBreathing()
            }
            start()
        }
    }

    /* ───────── breathing loop ───────── */
    private fun startBreathing() {
        val breatheX = ObjectAnimator.ofFloat(logo, View.SCALE_X, 1f, 1.035f, 1f).apply {
            duration = 1_000L
            repeatCount = ValueAnimator.INFINITE      // ✅ on the animator itself
            interpolator = AccelerateDecelerateInterpolator()
        }

        val breatheY = ObjectAnimator.ofFloat(logo, View.SCALE_Y, 1f, 1.035f, 1f).apply {
            duration = 1_600L
            repeatCount = ValueAnimator.INFINITE      // ✅ on the animator itself
            interpolator = AccelerateDecelerateInterpolator()
        }

        breathingSet = AnimatorSet().apply {
            playTogether(breatheX, breatheY)
            // ⬇ remove the line below – AnimatorSet has no repeatCount
            // repeatCount = ValueAnimator.INFINITE   <-- ❌ delete
            start()
        }
    }

    /* ───────── schedule the exit ───────── */
    override fun onResume() {
        super.onResume()
        // restart the timer only if we haven't left yet
        if (!exited && exitJob == null) {
            exitJob = lifecycleScope.launch {
                delay(1_400L)   // tweak as you like
                exitSequence()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // cancel the timer so it won't fire while in background
        exitJob?.cancel()
        exitJob = null
    }

    /* ───────── splash OUT ───────── */
    private fun exitSequence() {
        if (exited) return
        exited = true

        // stop breathing & free the choreographer callback
        breathingSet?.end()
        breathingSet = null

        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(logo, View.ALPHA, 1f, 0f),
                ObjectAnimator.ofFloat(logo, View.TRANSLATION_Y, 0f, -70f),
                ObjectAnimator.ofFloat(logo, View.SCALE_X, 1f, 0.4f),
                ObjectAnimator.ofFloat(logo, View.SCALE_Y, 1f, 0.4f),
                ObjectAnimator.ofFloat(logo, View.ROTATION, 0f, -10f)
            )
            duration = 350L
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }

        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(textView, View.ALPHA, 1f, 0f),
                ObjectAnimator.ofFloat(textView, View.TRANSLATION_Y, 0f, 50f),
                ObjectAnimator.ofFloat(textView, View.SCALE_X, 1f, 0.6f),
                ObjectAnimator.ofFloat(textView, View.SCALE_Y, 1f, 0.6f)
            )
            duration = 300L
            startDelay = 80L
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }

        // fade the whole decor & launch next screen
        window.decorView.findViewById<View>(android.R.id.content)
            .animate()
            .alpha(0f)
            .setDuration(280L)
            .withEndAction { launchNext() }
            .start()
    }

    private fun launchNext() {
        startActivity(
            Intent(this, MainActivity::class.java),
            ActivityOptionsCompat.makeCustomAnimation(
                this,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            ).toBundle()
        )
        // fallback in case of race with ActivityOptions
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    /* ───────── cleanup ───────── */
    override fun onDestroy() {
        breathingSet?.cancel()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onDestroy()
    }
}
