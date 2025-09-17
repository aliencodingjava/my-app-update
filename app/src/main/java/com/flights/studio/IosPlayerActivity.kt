package com.flights.studio

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.transition.TransitionManager
import android.view.View
import android.view.ViewOutlineProvider
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isEmpty
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.snackbar.Snackbar
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import eightbitlab.com.blurview.BlurTarget
import eightbitlab.com.blurview.BlurView
import java.util.Locale


@Suppress("DEPRECATION")
class IosPlayerActivity : AppCompatActivity() {

    private lateinit var youTubePlayerView: YouTubePlayerView
    private lateinit var loadingBar: ProgressBar
    private lateinit var fullscreenToggle: ImageView
    private var isFullscreen = false
    private lateinit var playToggle: ImageView
    private var isPlaying = false
    private var cachedPlayer: com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer? = null
    private var totalDurationSeconds = 0f
    private lateinit var playstopBlurView: BlurView
    private var isQualityMenuVisible = false
    private var selectedVideoId: String? = null
    private lateinit var progressBar: SeekBar
    private lateinit var timeCurrent: TextView
    private var suppressTimeUpdateUntil = 0L
    private lateinit var timeTotal: TextView
    private var isUserSeeking = false
    private lateinit var qualityMenuContainer: ChipGroup
    private lateinit var qualityMenuFrame: MaterialCardView





    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(R.anim.zoom_in, 0)
        setContentView(R.layout.activity_ios_player)

        qualityMenuContainer = findViewById(R.id.qualityMenuContainer)
        qualityMenuFrame = findViewById(R.id.qualityMenuFrame)



        timeCurrent = findViewById(R.id.timeCurrent)
        progressBar = findViewById(R.id.videoProgressBar)
        timeTotal = findViewById(R.id.timeTotal)


        val progressBar = findViewById<SeekBar>(R.id.videoProgressBar)
        progressBar.progress = 0

        progressBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                isUserSeeking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                isUserSeeking = false
                if (totalDurationSeconds > 0f) {
                    val targetTime = (seekBar.progress / 100f) * totalDurationSeconds
                    cachedPlayer?.seekTo(targetTime)
                    suppressTimeUpdateUntil = System.currentTimeMillis() + 800
                }
            }


            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (totalDurationSeconds > 0f) {
                    val targetTime = (progress / 100f) * totalDurationSeconds

                    if (fromUser) {
                        // 🔁 Show live time during swipe
                        timeCurrent.text = formatTime(targetTime)
                    }


                }
            }

        })


        val qualityToggle = findViewById<ImageView>(R.id.qualityToggle)
        qualityToggle.setOnClickListener {
            toggleQualityPopup()
        }

        val closeButton = findViewById<ImageView>(R.id.closeButton)
        closeButton.setOnClickListener {
            finish()
            overridePendingTransition(0, R.anim.zoom_out)
        }

        val blurTarget = findViewById<BlurTarget>(R.id.target)
        playstopBlurView = findViewById(R.id.playstopBlurView)
        val windowBackground = window.decorView.background
        val blurRadius = 20f

        playstopBlurView.setupWith(blurTarget)
            .setFrameClearDrawable(windowBackground)
            .setBlurRadius(blurRadius)
            .setBlurEnabled(true)
        playstopBlurView.outlineProvider = ViewOutlineProvider.BACKGROUND
        playstopBlurView.clipToOutline = true

        val animDrawable = createAnimatedGradientWithCorners()
        playstopBlurView.background = animDrawable
        animDrawable.start()

        loadingBar = findViewById(R.id.playerLoading)
        fullscreenToggle = findViewById(R.id.fullscreenToggle)
        youTubePlayerView = findViewById(R.id.youtube_player_view)
        lifecycle.addObserver(youTubePlayerView)
        playToggle = findViewById(R.id.playToggle)

        youTubePlayerView.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {

            override fun onReady(
                youTubePlayer: com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
            ) {
                cachedPlayer = youTubePlayer

                val defaultId = "xR9tE01Z8oY"          // “Summer Serenity”
                youTubePlayer.loadVideo(defaultId, 0f)

                // ✅ remember it so the chip can highlight later
                selectedVideoId = defaultId

                loadingBar.visibility = View.GONE
                isPlaying = true
                playToggle.setImageResource(R.drawable.ic_oui_control_pause)

                /* ───────── listen for duration, playback time, etc. ───────── */
                youTubePlayer.addListener(object : AbstractYouTubePlayerListener() {

                    override fun onVideoDuration(
                        youTubePlayer: com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer,
                        duration: Float
                    ) {
                        totalDurationSeconds = duration
                        timeTotal.text = formatTime(duration)
                    }

                    override fun onCurrentSecond(
                        youTubePlayer: com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer,
                        second: Float
                    ) {
                        if (totalDurationSeconds <= 0f) return

                        val percentage = (second / totalDurationSeconds) * 100

                        if (!isUserSeeking) {
                            progressBar.progress = percentage.toInt()
                            if (System.currentTimeMillis() >= suppressTimeUpdateUntil) {
                                timeCurrent.text = formatTime(second)
                            }
                        }

                        val blocker = findViewById<View>(R.id.videoTouchBlocker)
                        if (second > 0.3f && blocker.alpha == 1f) {
                            blocker.animate().alpha(0f).setDuration(800).start()
                        }

                        if (percentage >= 99 && isPlaying) {
                            isPlaying = false
                            playToggle.setImageResource(R.drawable.ic_oui_reply)
                            playToggle.animate().rotationBy(360f).setDuration(500).start()
                        }
                    }
                })
            }
        })


        playToggle.setOnClickListener {
            cachedPlayer?.let { player ->
                if (!isPlaying && progressBar.progress >= 99) {
                    player.seekTo(0f)
                    player.play()
                    playToggle.setImageResource(R.drawable.ic_oui_control_pause)
                    isPlaying = true
                } else if (isPlaying) {
                    player.pause()
                    playToggle.setImageResource(R.drawable.ic_oui_control_play)
                    isPlaying = false
                } else {
                    player.play()
                    playToggle.setImageResource(R.drawable.ic_oui_control_pause)
                    isPlaying = true
                }
            }
        }

        fullscreenToggle.setOnClickListener {
            toggleFullscreenSmooth()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
                overridePendingTransition(0, R.anim.zoom_out)
            }
        })

        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.getWindowInsetsController(window.decorView)?.apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

    }

    override fun onStop() {
        super.onStop()

    }


    private fun toggleFullscreenSmooth() {
        val closeButton = findViewById<ImageView>(R.id.closeButton)
        val playToggle = findViewById<ImageView>(R.id.playToggle)
        val root = findViewById<ConstraintLayout>(R.id.iosRoot)
        val qualityFrame = findViewById<MaterialCardView>(R.id.qualityMenuFrame)
        val wasQualityMenuOpen = isQualityMenuVisible
        val barGoingToTop = !isFullscreen

        val constraintSet = ConstraintSet().apply { clone(root) }

        if (barGoingToTop) {
            val topMargin = resources.getDimensionPixelSize(R.dimen.playstop_blur_bottom_margin)
            constraintSet.clear(R.id.playstopBlurView, ConstraintSet.BOTTOM)
            constraintSet.connect(R.id.playstopBlurView, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, topMargin)

            closeButton.visibility = View.VISIBLE
            playToggle.visibility = View.GONE
        } else {
            val bottomMargin = resources.getDimensionPixelSize(R.dimen.playstop_blur_bottom_margin)
            constraintSet.clear(R.id.playstopBlurView, ConstraintSet.TOP)
            constraintSet.connect(R.id.playstopBlurView, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, bottomMargin)

            closeButton.visibility = View.GONE
            playToggle.visibility = View.VISIBLE
        }

        val transition = android.transition.ChangeBounds().apply {
            interpolator = AccelerateDecelerateInterpolator()
            duration = 300
        }
        TransitionManager.beginDelayedTransition(root, transition)
        constraintSet.applyTo(root)

        fullscreenToggle.setImageResource(
            if (barGoingToTop)
                R.drawable.fullscreen_exit_24dp_ffffff_fill0_wght400_grad0_opsz24
            else
                R.drawable.ic_oui_fit_to_screen
        )

        isFullscreen = !isFullscreen

        if (wasQualityMenuOpen) {
            val container = findViewById<ChipGroup>(R.id.qualityMenuContainer)
            qualityFrame.animate()
                .alpha(0f)
                .translationY(if (barGoingToTop) 40f else -40f)
                .setDuration(120)
                .withEndAction {
                    qualityFrame.visibility = View.GONE
                    isQualityMenuVisible = false
                    container.alpha = 1f
                    container.translationY = 0f
                    root.postDelayed({ toggleQualityPopup() }, 180)
                }.start()

        }

    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }



    private fun toggleQualityPopup() {
        val qualityToggleIcon = findViewById<ImageView>(R.id.qualityToggle)
        val container = findViewById<ChipGroup>(R.id.qualityMenuContainer)
        val root = findViewById<ConstraintLayout>(R.id.iosRoot)

        val playBlurY = playstopBlurView.y
        val screenHeight = root.height
        val isBarAtBottom = playBlurY > screenHeight / 2

        val slideDistance = if (isBarAtBottom) dpToPx(5).toFloat() else -dpToPx(5).toFloat()
        val resetDistance = dpToPx(0).toFloat()
        // Define Material3-style color references (custom colors)
        val textColor = ContextCompat.getColor(this, R.color.md3_chip_text)
        val selectedColor = ContextCompat.getColorStateList(this, R.color.md3_chip_selected)
        val backgroundColor = ContextCompat.getColorStateList(this, R.color.md3_chip_background)
        val strokeColor = ContextCompat.getColorStateList(this, R.color.md3_chip_stroke)
        for (i in 0 until container.childCount) {
            val chip = container.getChildAt(i) as? Chip ?: continue
            chip.isChecked = (chip.tag == selectedVideoId)
            chip.chipBackgroundColor =
                if (chip.isChecked) selectedColor else backgroundColor
        }

        if (isQualityMenuVisible) {
            qualityMenuFrame.animate()
                .alpha(0f)
                .translationY(slideDistance)
                .setDuration(200)
                .withEndAction {
                    qualityMenuFrame.visibility = View.GONE
                    isQualityMenuVisible = false
                    qualityToggleIcon.setImageResource(R.drawable.folder_24dp_ffffff_fill1_wght400_grad0_opsz24)
                    qualityMenuFrame.alpha = 1f
                    qualityMenuFrame.translationY = 0f
                    playstopBlurView.animate().translationY(resetDistance).setDuration(200).start()
                }
                .start()
            return
        }

        MediaPlayer.create(this, R.raw.notification_decorative)?.apply {
            setOnCompletionListener { release() }
            start()
        }

        if (container.isEmpty()) {
            val videos = listOf(
                "Summer Serenity (YouTube)" to "xR9tE01Z8oY",
                "Sunset Reflections" to "isvsG6Uu9WU",
                "Grand Teton (4K Ultra HD)" to "O07ph1cZTR8",
                "Through My Lens: Jenny Lake & Canyon" to "mmYjxQIQEsU",
                "World Wild Hearts: Grand Teton Hike" to "WPo41u_-D_o",
                "Surviving Grand Teton • Nat Geo" to "LdW3jRm4B6s"
            )

            videos.forEach { (title, videoId) ->
                val chip = Chip(this).apply {
                    text = title
                    tag  = videoId
                    isCheckable = true
                    isClickable = true
                    isChecked = videoId == selectedVideoId
                    setEnsureMinTouchTargetSize(false)

                    setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelLarge)

                    // ✅ Fix white background behind text
                    setBackgroundColor(Color.TRANSPARENT)
                    backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)

                    // 🎨 Colors
                    setTextColor(textColor)
                    chipBackgroundColor = if (isChecked) selectedColor else backgroundColor
                    chipStrokeColor = strokeColor
                    chipStrokeWidth = resources.getDimension(R.dimen.m3_chip_stroke_width)
                    // ✨ Interaction
                    setOnClickListener {
                        if (selectedVideoId == videoId) return@setOnClickListener

                        /* normal play-logic */
                        selectedVideoId = videoId
                        cachedPlayer?.loadVideo(videoId, 0f)
                        isPlaying = true
                        playToggle.setImageResource(R.drawable.ic_oui_control_pause)
                        progressBar.progress = 0
                        timeCurrent.text = getString(R.string.time_zero)

                        /* ─── show Material-3 Snackbar ─── */
                        val sb = Snackbar
                            .make(
                                this@IosPlayerActivity.findViewById(android.R.id.content),
                                "Now playing: $title",
                                Snackbar.LENGTH_INDEFINITE          // we’ll close it manually
                            )
                            .setAnchorView(playstopBlurView)
                            .setTextMaxLines(2)

                        sb.show()

                         // force-dismiss after 700 ms, bypassing accessibility timeout inflation
                        sb.view.postDelayed({ sb.dismiss() }, 2000)


                        /* ──────────────────────────────── */

                        toggleQualityPopup()   // close chip list

                        // 🔁 refresh chips
                        for (i in 0 until container.childCount) {
                            val child = container.getChildAt(i) as? Chip ?: continue
                            child.isChecked = (child.tag == selectedVideoId)
                            child.chipBackgroundColor =
                                if (child.isChecked) selectedColor else backgroundColor
                        }
                    }


                }
                container.addView(chip)
            }

        }

        val constraintSet = ConstraintSet().apply { clone(root) }
        val playstopId = playstopBlurView.id
        val scrollViewId = qualityMenuFrame.id

        val transition = android.transition.AutoTransition().apply {
            duration = 280
            interpolator = AccelerateDecelerateInterpolator()
        }
        TransitionManager.beginDelayedTransition(root, transition)

        if (!isBarAtBottom) {
            constraintSet.clear(playstopId, ConstraintSet.BOTTOM)
            constraintSet.connect(playstopId, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 34)
            constraintSet.clear(scrollViewId, ConstraintSet.BOTTOM)
            constraintSet.connect(scrollViewId, ConstraintSet.TOP, playstopId, ConstraintSet.BOTTOM, 34)
        } else {
            constraintSet.clear(playstopId, ConstraintSet.TOP)
            constraintSet.connect(playstopId, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 34)
            constraintSet.clear(scrollViewId, ConstraintSet.TOP)
            constraintSet.connect(scrollViewId, ConstraintSet.BOTTOM, playstopId, ConstraintSet.TOP, 24)
        }

        constraintSet.applyTo(root)

        qualityMenuFrame.alpha = 0f
        qualityMenuFrame.translationY = slideDistance
        qualityMenuFrame.visibility = View.VISIBLE

        playstopBlurView.animate().translationY(slideDistance).setDuration(200).start()

        qualityMenuFrame.post {
            qualityMenuFrame.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(240)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withStartAction {
                    isQualityMenuVisible = true
                    qualityToggleIcon.setImageResource(R.drawable.folder_open_24dp_ffffff_fill1_wght400_grad0_opsz24)
                }
                .start()
        }
    }


    private fun formatTime(seconds: Float): String {
        val totalSec = seconds.coerceAtLeast(0f).toInt()
        val minutes = totalSec / 60
        val secs = totalSec % 60
        return String.format(Locale.US, "%02d:%02d", minutes, secs)
    }

    private fun createAnimatedGradientWithCorners(
        colorSet1: List<Int> = listOf(
            "#36FF0000".toColorInt(),
            "#3600FF00".toColorInt(),
            "#360000FF".toColorInt()
        ),
        colorSet2: List<Int> = listOf(
            "#36FFFFFF".toColorInt(),
            "#3600FFFF".toColorInt(),
            "#36228B22".toColorInt()
        ),
        colorSet3: List<Int> = listOf(
            "#368B4513".toColorInt(),
            "#36FF69B4".toColorInt(),
            "#364B0082".toColorInt()
        ),
      ): AnimationDrawable {
        val radius = 40f
        fun makeGradient(vararg colors: Int): GradientDrawable {
            return GradientDrawable(GradientDrawable.Orientation.BL_TR, colors).apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = radius
            }
        }
        return AnimationDrawable().apply {
            isOneShot = false
            setEnterFadeDuration(4000)
            setExitFadeDuration(4000)
            addFrame(makeGradient(*colorSet1.toIntArray()), 4000)
            addFrame(makeGradient(*colorSet2.toIntArray()), 4000)
            addFrame(makeGradient(*colorSet3.toIntArray()), 4000)
        }
    }

}