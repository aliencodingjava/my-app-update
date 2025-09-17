@file:Suppress("OVERRIDE_DEPRECATION")

package com.flights.studio

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.media.SoundPool
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.BulletSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.toColorInt
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.card.MaterialCardView
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.TimeZone

@Suppress("DEPRECATION")
class FeedbackBottomSheet : BottomSheetDialogFragment() {
    // ────── view references ──────
    private lateinit var feedbackInputLayout: TextInputLayout
    private lateinit var feedbackEditText: TextInputEditText
    private lateinit var progressBar: ProgressBar
    private lateinit var submitButton: TextView
    private lateinit var soundCheck: MaterialCheckBox
    private lateinit var statusText: TextView
    private lateinit var statusIndicator: View

    // ────── other state ──────
    private lateinit var soundPool: SoundPool
    private var soundId: Int = 0
    private var clickSoundId: Int = 0
    private var successSoundId: Int = 0
    private var isServerOnline = false
    private var serverCheckJob: Job? = null
    private val checkInterval: Long = 3000L
    private var isBanned = false
    private var banEndEpoch: Long? = null
    private var banStatusChecked = false
    private var banCountdownTimer: Job? = null
    private var fcmToken: String? = null
    private val sharedClient = OkHttpClient()


    companion object {
        private const val PREFS_NAME = "feedback_prefs"
        private const val SOUND_ENABLED_KEY = "feedback_sound_enabled"
        private const val REQUEST_BT_CONNECT = 42

    }
    private fun ensureBluetoothPermissionAndThenRun(block: () -> Unit) {
        val connectPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2)
            "android.permission.BLUETOOTH_CONNECT"
        else
            null

        if (connectPerm != null &&
            ContextCompat.checkSelfPermission(requireContext(), connectPerm)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(connectPerm), REQUEST_BT_CONNECT)
        } else {
            block()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_BT_CONNECT &&
            grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
        ) {
            actuallySendFeedback()
        }
    }

    private val prefs by lazy {
        requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(requireContext(), theme).apply {
            setOnShowListener {
                findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let { sheet ->
                    BottomSheetBehavior.from(sheet).apply {
                        skipCollapsed = true
                        state = BottomSheetBehavior.STATE_EXPANDED
                    }
                    sheet.setBackgroundColor(Color.TRANSPARENT)
                    behavior.isDraggable = false
                }
                feedbackEditText.post {
                    feedbackEditText.requestFocus()
                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE)
                            as InputMethodManager
                    imm.showSoftInput(feedbackEditText, InputMethodManager.SHOW_IMPLICIT)
                }
            }
        }
    }



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.dialog_feedback, container, false)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeUI(view)
        updateStatusUI(null)
        adjustSubmitButtonTextColor()

        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            fcmToken = token
            Log.d("FCM", "FCM token cached: $token")
        }

        view.findViewById<ImageView>(R.id.rulesButton)
            ?.setOnClickListener { showRulesDialog() }

        // Check ban status immediately
        CoroutineScope(Dispatchers.IO).launch {
            val deviceId = getHardDeviceId()
            try {
                val (banned, banEnd) = checkBanStatus(deviceId)
                isBanned = banned
                banEndEpoch = banEnd
                withContext(Dispatchers.Main) {
                    if (isBanned) showBanStatus()
                    else startPeriodicServerChecks()
                }
            } catch (e: Exception) {
                Log.e("BanCheck", "Error checking ban status: ${e.message}")
                withContext(Dispatchers.Main) {
                    statusText.text = getString(R.string.ban_status_unavailable)
                    progressBar.visibility = View.GONE
                    feedbackEditText.isEnabled = false
                    feedbackEditText.hint = getString(R.string.ban_status_error_hint)
                    submitButton.isEnabled = false
                    startPeriodicServerChecks()
                }
            }
        }

        feedbackEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrEmpty()) {
                    submitButton.setBackgroundResource(R.drawable.bg_feedback_button)
                    submitButton.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.neon_blue)
                    )
                } else {
                    submitButton.setBackgroundResource(R.drawable.bg_feedback_button_focused)
                    adjustSubmitButtonTextColor()
                }
            }
        })
    }


    override fun onDestroyView() {
        super.onDestroyView()
        banCountdownTimer?.cancel()
        soundPool.release()
        stopPeriodicServerChecks()
    }


    @SuppressLint("MissingInflatedId")
    private fun initializeUI(view: View) {
        feedbackInputLayout = view.findViewById(R.id.feedbackInputLayout)
        feedbackEditText    = view.findViewById(R.id.feedbackEditText)
        progressBar         = view.findViewById(R.id.feedbackProgressBar)
        submitButton        = view.findViewById(R.id.submitFeedbackButton)
        soundCheck          = view.findViewById(R.id.feedbackSoundCheck)
        statusText          = view.findViewById(R.id.feedbackStatusText)
        statusIndicator     = view.findViewById(R.id.statusIndicator)



        soundPool = SoundPool.Builder().setMaxStreams(1).build().also {
            clickSoundId   = it.load(requireContext(), R.raw.time_click, 1)
            successSoundId = it.load(requireContext(), R.raw.success,    1)
        }

        submitButton.setBackgroundResource(R.drawable.bg_feedback_button)
        submitButton.setTextColor(
            ContextCompat.getColor(requireContext(), android.R.color.primary_text_light)
        )

        setupTypingVisualizer(view)
        setupVibrationWatcher()
        setupSoundCheck(soundCheck)
        setupSubmitButton()
    }
    private fun showRulesDialog() {
        val rules = listOf(
            "No profanity or hate speech",
            "No personal data or doxxing",
            "Violations incur a 60‑day ban",
            "No harassment or bullying",
            "No spam, advertising, or self‑promotion"
        )
        val gapPx = (8 * resources.displayMetrics.density).toInt()
        val ssb = SpannableStringBuilder().apply {
            rules.forEachIndexed { i, rule ->
                val start = length
                append(rule)
                if (i != lastIndex) append("\n")
                setSpan(
                    BulletSpan(gapPx, ContextCompat.getColor(requireContext(), R.color.color_success)),
                    start, start + rule.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.feedback_rules)
            .setMessage(ssb)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }


    private fun getHardDeviceId(): String {
        val rawId = listOf(
            Build.BOARD,
            Build.BRAND,
            Build.DEVICE,
            Build.HARDWARE,
            Build.MODEL,
            Build.MANUFACTURER
        ).joinToString("-")

        val digest = java.security.MessageDigest.getInstance("SHA-256")
        return digest.digest(rawId.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }


    private fun showBanStatus() {
        // disable everything
        feedbackInputLayout.isEnabled = false
        feedbackEditText.isEnabled = false
        submitButton.isEnabled = false
        soundCheck.isEnabled = false

        // update hint
        feedbackInputLayout.hint = getString(R.string.ban_reason_hint)

        banCountdownTimer?.cancel()
        banCountdownTimer = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                val timeLeft = (banEndEpoch ?: 0L) - (System.currentTimeMillis() / 1000)
                if (timeLeft <= 0) {
                    if (isAdded) {
                        isBanned = false
                        banEndEpoch = null
                        banStatusChecked = false
                        updateStatusUI(true)
                    }
                    break
                }
                val days = timeLeft / 86400
                val hours = (timeLeft % 86400) / 3600
                val minutes = (timeLeft % 3600) / 60
                val seconds = timeLeft % 60
                val message = when {
                    days > 0    -> getString(R.string.ban_days_full, days, hours, minutes, seconds)
                    hours > 0   -> getString(R.string.ban_hours_full, hours, minutes, seconds)
                    minutes > 0 -> getString(R.string.ban_minutes_full, minutes, seconds)
                    else        -> getString(R.string.ban_seconds_only, seconds)
                }
                statusText.text = message
                delay(500L)
            }
        }
    }

    @SuppressLint("MissingInflatedId")
    private fun setupTypingVisualizer(view: View) {
        val visualizer = view.findViewById<LinearLayout>(R.id.typingVisualizer)
        val bars = listOf(
            view.findViewById<View>(R.id.bar1),
            view.findViewById<View>(R.id.bar2),
            view.findViewById<View>(R.id.bar3)
        )

        // A) pulse animators
        val barAnims = bars.mapIndexed { i, bar ->
            ObjectAnimator.ofFloat(bar, "scaleY", 0.4f, 1f).apply {
                duration    = 200L + i * 50
                repeatMode  = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
            }
        }

        // B) color‑mapping
        val defaultColor = ContextCompat.getColor(requireContext(), R.color.bar_default)
        val digitColor   = ContextCompat.getColor(requireContext(), R.color.input_digit_color)
        val row1Color    = ContextCompat.getColor(requireContext(), R.color.input_row1_color)
        val row2Color    = ContextCompat.getColor(requireContext(), R.color.input_row2_color)
        val row3Color    = ContextCompat.getColor(requireContext(), R.color.input_row3_color)

        // C) slide‑out + reset
        val handler = Handler(Looper.getMainLooper())
        val hideRunnable = Runnable {
            animateVisualizerOut(visualizer, bars, barAnims) {
                // nothing extra
            }
        }

        // D) watcher
        feedbackEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, ct: Int, af: Int) {}
            override fun afterTextChanged(s: Editable?) {}

            override fun onTextChanged(s: CharSequence?, st: Int, bf: Int, ct: Int) {
                if (s.isNullOrEmpty()) {
                    handler.removeCallbacks(hideRunnable)
                    animateVisualizerOut(visualizer, bars, barAnims)
                    return
                }
                if (!prefs.getBoolean(SOUND_ENABLED_KEY, true)) return

                // 1) slide‑in + pulse
                if (visualizer.visibility != View.VISIBLE) {
                    animateVisualizerIn(visualizer)
                    barAnims.forEach { it.start() }
                }

                // 2) pick mapped color
                val lastChar = s.lastOrNull() ?: return
                val row1 = "qwertyuiop"
                val row2 = "asdfghjkl"
                val row3 = "zxcvbnm"
                val targetColor = when {
                    lastChar.isDigit()                            -> digitColor
                    row1.contains(lastChar, ignoreCase = true)    -> row1Color
                    row2.contains(lastChar, ignoreCase = true)    -> row2Color
                    row3.contains(lastChar, ignoreCase = true)    -> row3Color
                    else                                          -> defaultColor
                }

                // 3) flash bars grey→target→grey
                bars.forEach { bar ->
                    (bar.background as? GradientDrawable)?.let { dr ->
                        val bg = (dr.mutate() as GradientDrawable)
                        ValueAnimator.ofArgb(defaultColor, targetColor).apply {
                            duration    = 250
                            repeatMode  = ValueAnimator.REVERSE
                            repeatCount = 1
                            addUpdateListener { anim ->
                                bg.setColor(anim.animatedValue as Int)
                            }
                        }.start()
                    }
                }

                // 4) schedule slide‑out
                handler.removeCallbacks(hideRunnable)
                handler.postDelayed(hideRunnable, 500)
            }
        })
    }

    // ─────────────────────────────────────────────────────────────────────────────
// slide‑in from left + fade + zoom
    private fun animateVisualizerIn(view: View) {
        view.apply {
            alpha = 0f
            translationX = -width.toFloat()
            scaleX = 0.8f
            visibility = View.VISIBLE
            animate()
                .translationX(0f)
                .alpha(1f)
                .scaleX(1f)
                .setDuration(200)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    // slide‑out to right + fade + zoom
    private fun animateVisualizerOut(
        view: View,
        bars: List<View>,
        barAnims: List<ObjectAnimator>,
        onEnd: () -> Unit = {},
    ) {
        view.animate()
            .translationX(view.width.toFloat())
            .alpha(0f)
            .scaleX(0.8f)
            .setDuration(200)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction {
                barAnims.forEach { it.cancel() }
                bars.forEach { it.scaleY = 0.4f }
                view.visibility = View.GONE
                view.translationX = 0f
                view.alpha = 1f
                view.scaleX = 1f
                onEnd()
            }
            .start()
    }


    private fun setupVibrationWatcher() {

        feedbackEditText.addTextChangedListener(object : TextWatcher {
            private var lastLen = 0
            override fun beforeTextChanged(s: CharSequence?, st: Int, ct: Int, af: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, st: Int, bf: Int, ct: Int) {
                val len = s?.length ?: 0
                if (len > lastLen && prefs.getBoolean(SOUND_ENABLED_KEY, true)) {
                    soundPool.play(clickSoundId, 1f, 1f, 0, 0, 1f)
                    vibrateAndPlayClick(requireContext())
                }
                lastLen = len
            }
        })
    }
    private fun onFeedbackSentSuccess() {
        // 1️⃣ clear input & show status
        feedbackEditText.setText("")
        statusText.apply {
            text = getString(R.string.feedback_sent_success)
            visibility = View.VISIBLE
        }

        // 2️⃣ re‑enable buttons
        submitButton.apply {
            isEnabled = true
            text = getString(R.string.send_feedback)
            setBackgroundResource(R.drawable.bg_feedback_button)
        }

        if (!prefs.getBoolean(SOUND_ENABLED_KEY, true)) return

        // 3️⃣ play success sound
        val vol = 0.3f
        soundPool.play(successSoundId, vol, vol, 0, 0, 1f)

        // 4️⃣ grab the card & rotate its border colour
        view?.findViewById<MaterialCardView>(R.id.feedbackCard)?.let { card ->
            // make the stroke visible
            val strokePx = resources.getDimensionPixelSize(R.dimen.neon_stroke_width)
            card.strokeWidth = strokePx

            // your three‑stop palette
            val cStart = ContextCompat.getColor(requireContext(), R.color.material_yellow)
            val cMid   = ContextCompat.getColor(requireContext(), R.color.siri_glow_mid)
            val cEnd   = ContextCompat.getColor(requireContext(), R.color.siri_glow_end)
            val cFinal = "#CCC2DC".toColorInt()


            // animate: start → mid → end → start
            ValueAnimator.ofArgb(cStart, cMid, cEnd, cStart).apply {
                duration = 1200L
                addUpdateListener { anim ->
                    card.strokeColor = anim.animatedValue as Int
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        // hide the stroke when done
                        card.strokeColor = cFinal
                    }
                })
                start()
            }
        }
    }

    private fun adjustSubmitButtonTextColor() {
        submitButton.let { button ->
            val bg = button.background
            if (bg is GradientDrawable) {
                val colorInt = bg.color?.defaultColor ?: return
                val brightness = calculateBrightness(colorInt)
                val textColor = if (brightness < 128) {
                    ContextCompat.getColor(requireContext(), android.R.color.white)
                } else {
                    ContextCompat.getColor(requireContext(), android.R.color.black)
                }
                button.setTextColor(textColor)
            } else {
                // fallback if not GradientDrawable
                button.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            }
        }
    }

    private fun calculateBrightness(color: Int): Int {
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        return (r * 299 + g * 587 + b * 114) / 1000
    }

    /** Called from initializeUI() */
    private fun setupSoundCheck(check: MaterialCheckBox) {

        // 1️⃣ restore last choice
        val saved = prefs.getBoolean(SOUND_ENABLED_KEY, true)
        check.isChecked = saved
        refreshCheckUi(check, saved)

        // 2️⃣ update UI + prefs whenever the user flips it
        check.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit { putBoolean(SOUND_ENABLED_KEY, isChecked) }
            refreshCheckUi(check, isChecked)
        }
    }

    /** Small helper that applies icon + background for the two states */
    private fun refreshCheckUi(box: MaterialCheckBox, checked: Boolean) {

        /* ───────────────────────────────────── 1.  pick icon + background ─────────────────────────────────── */

        val iconRes   = if (checked)
            R.drawable.volume_up_16dp_ffffff_fill0_wght400_grad0_opsz20
        else
            R.drawable.volume_off_16dp_ffffff_fill0_wght400_grad0_opsz20

        box.background = ContextCompat.getDrawable(
            box.context,
            if (checked) R.drawable.bg_holo_checked
            else          R.drawable.checking_for_updates_backround
        )

        /* ───────────────────────────────────── 2.  decide colours ─────────────────────────────────────────── */

        @ColorInt val darkText   = ContextCompat.getColor(box.context, android.R.color.black)
        @ColorInt val normalText = MaterialColors.getColor(
            box.context,
            com.google.android.material.R.attr.colorOnSurface,
            0
        )

        val textColour = if (checked) darkText else normalText   // ← current colour

        /* ───────────────────────────────────── 3.  tint the icon to match ─────────────────────────────────── */

        val rawIcon   = ContextCompat.getDrawable(box.context, iconRes)!!.mutate()
        val wrapped   = DrawableCompat.wrap(rawIcon)
        DrawableCompat.setTint(wrapped, textColour)

        box.setCompoundDrawablesRelativeWithIntrinsicBounds(wrapped, null, null, null)

        /* ───────────────────────────────────── 4.  apply text colour ─────────────────────────────────────── */

        box.setTextColor(textColour)

    }
    private fun setupSubmitButton() {
        submitButton.setOnClickListener {
            ensureBluetoothPermissionAndThenRun {
                actuallySendFeedback()
            }
        }
    }



    @Suppress("DEPRECATION")
    private fun vibrateAndPlayClick(context: Context) {
        // 1. VIBRATION (always)
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
            vm.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
        }
        vibrator.vibrate(
            android.os.VibrationEffect.createOneShot(
                5,
                android.os.VibrationEffect.DEFAULT_AMPLITUDE
            )
        )

        // 2. SOUND (only if enabled in prefs)
        if (prefs.getBoolean(SOUND_ENABLED_KEY, true)) {
            val audioMgr = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVol = audioMgr.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat()
            val curVol = audioMgr.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
            val playVol = (curVol / maxVol) * 0.5f

            soundPool.play(
                soundId,
                playVol,    // left volume
                playVol,    // right volume
                0,          // priority
                0,          // loop
                1f          // playback rate
            )
        }
    }



    private suspend fun deviceIsBanned(deviceId: String): Boolean = withContext(Dispatchers.IO) {
        if (banStatusChecked) return@withContext isBanned
        val (banned, banEnd) = checkBanStatus(deviceId)
        isBanned = banned
        banEndEpoch = banEnd
        banStatusChecked = true
        banned
    }

    private fun actuallySendFeedback() {
        val feedbackText = feedbackEditText.text.toString().trim()
        if (feedbackText.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter feedback.", Toast.LENGTH_SHORT).show()
            return
        }

        // Kick off one coroutine on Main that will hop to IO as needed:
        CoroutineScope(Dispatchers.Main).launch {
            // 0️⃣ Ban check on IO
            val deviceId = getHardDeviceId()
            if (deviceIsBanned(deviceId)) {
                showBanStatus()
                return@launch
            }

            // 1️⃣ Show progress UI
            progressBar.apply {
                isIndeterminate = false
                progress = 0
                visibility = View.VISIBLE
            }
            submitButton.apply {
                isEnabled = false
                text = getString(R.string.sending_feedback)
            }

            // 2️⃣ start a little progress animator on Main
            val progressJob = launch {
                val startTime = System.currentTimeMillis()
                val max = 4_000L
                while (isActive) {
                    val elapsed = System.currentTimeMillis() - startTime
                    progressBar.progress = ((elapsed.coerceAtMost(max) * 100) / max).toInt()
                    delay(30)
                }
            }

            // 3️⃣ Build payload & actually send / queue on IO
            val result = withContext(Dispatchers.IO) {
                val deviceInfo = getDeviceInfo(requireContext()).apply { put("fcmToken", fcmToken) }
                val json = JSONObject().apply {
                    put("message", feedbackText)
                    put("deviceInfo", deviceInfo)
                    put("fcmToken", fcmToken)
                }
                val body = json.toString().toRequestBody("application/json".toMediaType())

                if (isServerOnline) {
                    val req = Request.Builder()
                    .url("https://ylgvdeiqaaikcohhpwfi.supabase.co/rest/v1/feedback")
                    .addHeader(
                        "apikey",
                        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlsZ3ZkZWlxYWFpa2NvaGhwd2ZpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDQyNjQwMTEsImV4cCI6MjA1OTg0MDAxMX0.-JYW9jeUuFW8gBsmtv-OHKYbAVKsj0IAWU80zGWnwFU"
                    )
                    .addHeader(
                        "Authorization",
                        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlsZ3ZkZWlxYWFpa2NvaGhwd2ZpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDQyNjQwMTEsImV4cCI6MjA1OTg0MDAxMX0.-JYW9jeUuFW8gBsmtv-OHKYbAVKsj0IAWU80zGWnwFU"
                    )
                    .addHeader("Content-Type", "application/json")
                        .post(body)
                        .build()
                    if (sharedClient.newCall(req).execute().isSuccessful) "sent" else "error"
                } else {
                    queueFeedbackLocally(feedbackText)
                    "queued"
                }
            }

            // 4️⃣ Tear down progress and update UI on Main
            progressJob.cancelAndJoin()
            progressBar.progress = 100
            delay(100)
            progressBar.visibility = View.GONE

            when (result) {
                "sent" -> {
                    onFeedbackSentSuccess()
                    delay(1_000)
                    updateStatusUI(true)
                }
                "queued" -> {
                    statusText.text = getString(R.string.feedback_queued_success)
                    statusText.visibility = View.VISIBLE
                    feedbackEditText.setText("")
                }
                "error" -> {
                    statusText.text = getString(R.string.feedback_send_error)
                    statusText.visibility = View.VISIBLE
                }
            }

            submitButton.apply {
                isEnabled = true
                text = getString(R.string.send_feedback)
                setBackgroundResource(R.drawable.bg_feedback_button)
            }
        }
    }

    private fun startPeriodicServerChecks() {
        serverCheckJob = CoroutineScope(Dispatchers.IO).launch {
            checkServerStatus() // 👈 run immediately
            while (isActive) {
                delay(checkInterval)
                checkServerStatus()
            }
        }
    }


    private fun stopPeriodicServerChecks() {
        serverCheckJob?.cancel()
    }

    private suspend fun checkServerStatus() {
        withContext(Dispatchers.IO) {

            val client = OkHttpClient()
            val request = Request.Builder()
                .url("https://ylgvdeiqaaikcohhpwfi.supabase.co/rest/v1/feedback?select=id&limit=1")
                .addHeader("apikey", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlsZ3ZkZWlxYWFpa2NvaGhwd2ZpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDQyNjQwMTEsImV4cCI6MjA1OTg0MDAxMX0.-JYW9jeUuFW8gBsmtv-OHKYbAVKsj0IAWU80zGWnwFU")
                .addHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlsZ3ZkZWlxYWFpa2NvaGhwd2ZpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDQyNjQwMTEsImV4cCI6MjA1OTg0MDAxMX0.-JYW9jeUuFW8gBsmtv-OHKYbAVKsj0IAWU80zGWnwFU")
                .get()
                .build()


            try {
                val response = client.newCall(request).execute()
                val isOnline = response.isSuccessful

                withContext(Dispatchers.Main) {
                    if (!isServerOnline && isOnline) {
                        processQueuedFeedback()
                    }
                    isServerOnline = isOnline

                    // 🛑 Avoid showing "online" if banned
                    if (!isBanned) {
                        updateStatusUI(isOnline)
                    }
                }


                // ✅ Refresh ban status (detect new bans OR unbans live)
                val deviceId = getHardDeviceId()
                val (currentlyBanned, banEnd) = checkBanStatus(deviceId)

                withContext(Dispatchers.Main) {
                    if (currentlyBanned && !isBanned) {
                        // ⛔ Newly banned while sheet is open
                        isBanned = true
                        banEndEpoch = banEnd
                        banStatusChecked = true
                        showBanStatus()
                    } else if (!currentlyBanned && isBanned) {
                        // ✅ Unbanned while sheet is open
                        isBanned = false
                        banEndEpoch = null
                        banStatusChecked = false

                        feedbackInputLayout.isEnabled = true
                        feedbackInputLayout.hint      = getString(R.string.enter_your_feedback_here)


                        submitButton.isEnabled = true

                        updateStatusUI(true)
                    }
                }

            } catch (e: Exception) {
                Log.e("SupabaseStatus", "Error checking Supabase: ${e.message}")
                withContext(Dispatchers.Main) {
                    isServerOnline = false
                    updateStatusUI(false)
                }
            }
        }
    }
    private fun queueFeedbackLocally(feedbackText: String) {
        if (feedbackText.isBlank()) {
            Log.w("QueueFeedback", "Empty feedback not queued.")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val queueFile = File(requireContext().filesDir, "feedbackQueue.json")
                val feedbackQueue = if (queueFile.exists()) {
                    JSONObject(queueFile.readText()).getJSONArray("queue")
                } else {
                    JSONArray()
                }

                feedbackQueue.put(feedbackText)
                queueFile.writeText(JSONObject().put("queue", feedbackQueue).toString())

                withContext(Dispatchers.Main) {
                    val statusTextView = view?.findViewById<TextView>(R.id.feedbackStatusText)
                    statusTextView?.text = getString(R.string.feedback_queued_success)
                    statusTextView?.visibility = View.VISIBLE
                    progressBar.visibility = View.GONE
                    view?.findViewById<EditText>(R.id.feedbackEditText)?.setText("") // ✅ CLEAR INPUT

                }
            } catch (e: Exception) {
                Log.e("QueueFeedback", "Error queuing feedback: ${e.message}")
                withContext(Dispatchers.Main) {
                    val statusTextView = view?.findViewById<TextView>(R.id.feedbackStatusText)
                    statusTextView?.text = getString(R.string.feedback_queue_error, e.message)
                    statusTextView?.visibility = View.VISIBLE
                    progressBar.visibility = View.GONE
                }
            }
        }
    }




    private fun processQueuedFeedback() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val queueFile = File(requireContext().filesDir, "feedbackQueue.json")
                if (!queueFile.exists()) return@launch

                // ───────────────────────────────────────────────────────────────
                // 1️⃣  Load queue safely and bail if empty
                // ───────────────────────────────────────────────────────────────
                val feedbackQueue = JSONObject(queueFile.readText())
                    .optJSONArray("queue") ?: JSONArray()

                if (feedbackQueue.length() == 0) {
                    // Nothing to resend → delete stub so the banner won’t pop up again
                    queueFile.delete()
                    return@launch
                }

                // ───────────────────────────────────────────────────────────────
                // 2️⃣  Attempt to resend items
                // ───────────────────────────────────────────────────────────────
                val client       = OkHttpClient()
                val failedQueue  = JSONArray()
                var processedCnt = 0

                for (i in 0 until feedbackQueue.length()) {
                    processedCnt++                                    // track real attempts

                    val feedbackText = feedbackQueue.getString(i).trim()
                    if (feedbackText.isBlank()) {
                        Log.w("ProcessQueue", "Skipping blank feedback.")
                        continue
                    }

                    val token = FirebaseMessaging.getInstance().token.await()
                    val deviceInfo = getDeviceInfo(requireContext()).apply {
                        put("fcmToken", token)
                    }

                    val json = JSONObject().apply {
                        put("message",    feedbackText)
                        put("deviceInfo",  deviceInfo)
                        put("fcmToken",    token)
                    }

                    val requestBody = json.toString()
                        .toRequestBody("application/json; charset=utf-8".toMediaType())

                    val request = Request.Builder()
                        .url("https://ylgvdeiqaaikcohhpwfi.supabase.co/rest/v1/feedback")
                        .addHeader("apikey", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlsZ3ZkZWlxYWFpa2NvaGhwd2ZpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDQyNjQwMTEsImV4cCI6MjA1OTg0MDAxMX0.-JYW9jeUuFW8gBsmtv-OHKYbAVKsj0IAWU80zGWnwFU")
                        .addHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlsZ3ZkZWlxYWFpa2NvaGhwd2ZpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDQyNjQwMTEsImV4cCI6MjA1OTg0MDAxMX0.-JYW9jeUuFW8gBsmtv-OHKYbAVKsj0IAWU80zGWnwFU")
                        .addHeader("Content-Type", "application/json")
                        .post(requestBody)
                        .build()

                    try {
                        val response     = client.newCall(request).execute()
                        val responseBody = response.body?.string()

                        if (!response.isSuccessful) {
                            Log.e("ProcessQueue", "❌ Failed to send: $feedbackText\nCode: ${response.code}\nBody: $responseBody")
                            failedQueue.put(feedbackText)
                        } else {
                            Log.d("ProcessQueue", "✅ Sent: $feedbackText")
                        }
                    } catch (e: Exception) {
                        Log.e("ProcessQueue", "❌ Exception: ${e.message}")
                        failedQueue.put(feedbackText)
                    }
                }

                // ───────────────────────────────────────────────────────────────
                // 3️⃣  Persist failures or clean up file
                // ───────────────────────────────────────────────────────────────
                if (failedQueue.length() == 0) {
                    queueFile.delete()   // everything went through → remove file
                } else {
                    queueFile.writeText(JSONObject().put("queue", failedQueue).toString())
                }

                // ───────────────────────────────────────────────────────────────
                // 4️⃣  UI feedback – only if we processed at least one item
                // ───────────────────────────────────────────────────────────────
                withContext(Dispatchers.Main) {
                    if (processedCnt == 0) return@withContext          // safety guard

                    val statusTextView = view?.findViewById<TextView>(R.id.feedbackStatusText)
                    if (failedQueue.length() == 0) {
                        statusTextView?.text = getString(R.string.feedback_all_sent)
                    } else {
                        statusTextView?.text = getString(R.string.feedback_retry_later)
                    }
                    statusTextView?.visibility = View.VISIBLE
                }

            } catch (e: Exception) {
                Log.e("ProcessQueue", "❌ Final error: ${e.message}")
                withContext(Dispatchers.Main) {
                    val statusTextView = view?.findViewById<TextView>(R.id.feedbackStatusText)
                    statusTextView?.text = getString(R.string.feedback_send_exception_error, e.message)
                    statusTextView?.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun checkBanStatus(deviceId: String): Pair<Boolean, Long?> {
        val url = "https://ylgvdeiqaaikcohhpwfi.supabase.co/rest/v1/bans_table?device_id=eq.$deviceId&select=ban_end"
        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlsZ3ZkZWlxYWFpa2NvaGhwd2ZpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDQyNjQwMTEsImV4cCI6MjA1OTg0MDAxMX0.-JYW9jeUuFW8gBsmtv-OHKYbAVKsj0IAWU80zGWnwFU")
            .addHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlsZ3ZkZWlxYWFpa2NvaGhwd2ZpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDQyNjQwMTEsImV4cCI6MjA1OTg0MDAxMX0.-JYW9jeUuFW8gBsmtv-OHKYbAVKsj0IAWU80zGWnwFU")
            .get()
            .build()

        val client = OkHttpClient()
        val response = client.newCall(request).execute()

        if (response.isSuccessful) {
            val body = response.body?.string()
            val jsonArray = JSONArray(body)
            if (jsonArray.length() > 0) {
                val banEndString = jsonArray.getJSONObject(0).getString("ban_end")
                val banEnd = java.time.Instant.parse(banEndString)
                val now = java.time.Instant.now()
                return Pair(banEnd.isAfter(now), banEnd.epochSecond)
            }
        }

        return Pair(false, null)
    }

    private fun updateStatusUI(isOnline: Boolean?) {
        if (isBanned) {
            showBanStatus()
            return
        }

        stopAllStatusAnimations()

        when (isOnline) {
            null -> {
                if (!isAdded) return

                statusText.apply {
                    text = getString(R.string.checking_status)
                    visibility = View.VISIBLE
                    setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_orange_light))
                }

                statusIndicator.apply {
                    setBackgroundResource(R.drawable.checking_status)
                    alpha = 1f
                    scaleX = 1f
                    scaleY = 1f
                }
            }

            true -> {
                if (!isAdded) return

                statusText.apply {
                    text = getString(R.string.server_online)
                    visibility = View.VISIBLE
                    setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark))
                }

                statusIndicator.apply {
                    setBackgroundResource(R.drawable.online_indicator)
                    alpha = 1f
                    scaleX = 1f
                    scaleY = 1f
                }

                feedbackInputLayout.hint = getString(R.string.enter_your_feedback_here)
                feedbackEditText.hint = null
            }

            false -> {
                if (!isAdded) return

                statusText.apply {
                    text = getString(R.string.server_offline)
                    visibility = View.VISIBLE
                    setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_blue_bright))
                }

                statusIndicator.apply {
                    setBackgroundResource(R.drawable.offline_indicator)
                    alpha = 1f
                    scaleX = 1f
                    scaleY = 1f
                }

                feedbackInputLayout.hint = ""
            }
        }
    }
    private fun stopAllStatusAnimations() {
        statusIndicator.clearAnimation()
        statusIndicator.animate().cancel()
        statusIndicator.alpha = 1f
        statusIndicator.scaleX = 1f
        statusIndicator.scaleY = 1f
        statusIndicator.setTag(R.id.status_animator, null)
        statusIndicator.setTag(R.id.status_blink_animator, null)
    }


    private fun isDeviceRooted(): Boolean {
        // 1) Check for “test-keys” in build tags
        Build.TAGS?.let {
            if (it.contains("test-keys")) return true
        }
        // 2) Look for su binary in common locations
        arrayOf(
            "/system/bin/su", "/system/xbin/su", "/sbin/su",
            "/system/app/Superuser.apk", "/system/app/SuperSU.apk",
            "/data/local/xbin/su", "/data/local/bin/su", "/data/local/su"
        ).forEach { path ->
            if (File(path).exists()) return true
        }
        return false
    }

    private fun getDeviceInfo(context: Context): JSONObject {
        val deviceInfo = JSONObject()

        // 1) Core fields — this must never be skipped!
        val pm      = context.packageManager
        val pkgInfo = pm.getPackageInfo(context.packageName, 0)
        val metrics = context.resources.displayMetrics
        val wm      = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
        val am      = (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
        val memInfo = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }

        deviceInfo.put("deviceModel",      Build.MODEL)
        deviceInfo.put("osVersion",        Build.VERSION.RELEASE)
        deviceInfo.put("appVersion",       pkgInfo.versionName)
        deviceInfo.put("manufacturer",     Build.MANUFACTURER)
        deviceInfo.put("deviceId",         getHardDeviceId())
        deviceInfo.put("screenResolution", "${metrics.widthPixels} x ${metrics.heightPixels}")
        deviceInfo.put("networkType",      getNetworkType(context))
        deviceInfo.put("isRooted",         isDeviceRooted())
        deviceInfo.put("hardware",         Build.HARDWARE)
        deviceInfo.put("refreshRate",      wm.defaultDisplay.refreshRate.toDouble())
        deviceInfo.put("timeZone",         TimeZone.getDefault().id)
        deviceInfo.put("totalRam",         memInfo.totalMem)
        deviceInfo.put("availRam",         memInfo.availMem)

        // ─── optionally add a user‑friendly “deviceName” ───
        try {
            val btName     = BluetoothAdapter.getDefaultAdapter()?.name
            val globalName = Settings.Global.getString(
                context.contentResolver,
                Settings.Global.DEVICE_NAME
            )
            val finalName = btName ?: globalName ?: Build.MODEL
            deviceInfo.put("deviceName", finalName)
        } catch (_: SecurityException) {
            // no BLUETOOTH_CONNECT → just skip deviceName
        } catch (_: Exception) {
            // any weird failure → skip deviceName
        }

        return deviceInfo
    }


    private fun getNetworkType(context: Context): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2) {
            val activeNetwork: Network? = cm.activeNetwork
            val capabilities: NetworkCapabilities? = cm.getNetworkCapabilities(activeNetwork)

            return when {
                capabilities == null -> "UNKNOWN"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
                else -> "UNKNOWN"
            }
        } else {
            // For Android versions below Q, NetworkInfo is deprecated and we do not handle it here.
            // If needed, you could still fallback to older methods, but ideally, avoid this on SDK >= 30.
            return "UNKNOWN"
        }
    }

}