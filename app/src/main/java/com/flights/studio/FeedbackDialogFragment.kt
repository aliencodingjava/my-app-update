@file:Suppress("OVERRIDE_DEPRECATION")

package com.flights.studio

import android.app.ActivityManager
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.SoundPool
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.GppMaybe
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.messaging.FirebaseMessaging
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
import androidx.compose.ui.graphics.Color as ComposeColor

@Suppress("DEPRECATION")
class FeedbackBottomSheet : DialogFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, android.R.style.Theme_Translucent_NoTitleBar)
    }

    private enum class FeedbackServerState {
        Checking,
        Online,
        Offline
    }

    private lateinit var soundPool: SoundPool
    private var clickSoundId: Int = 0
    private var successSoundId: Int = 0
    private var isServerOnline = false
    private var serverCheckJob: Job? = null
    private val checkInterval: Long = 3000L
    private var isBanned = false
    private var banEndEpoch: Long? = null
    private var banStatusChecked = false
    private var banCountdownTimer: Job? = null
    private var typingPulseJob: Job? = null
    private var fcmToken: String? = null
    private val sharedClient = OkHttpClient()

    private var feedbackText by mutableStateOf("")
    private var soundEnabled by mutableStateOf(true)
    private var serverState by mutableStateOf(FeedbackServerState.Checking)
    private var statusMessage by mutableStateOf("")
    private var isSending by mutableStateOf(false)
    private var inputEnabled by mutableStateOf(true)
    private var soundControlsEnabled by mutableStateOf(true)
    private var typingPulseActive by mutableStateOf(false)
    private var successEffectActive by mutableStateOf(false)

    companion object {
        private const val PREFS_NAME = "feedback_prefs"
        private const val SOUND_ENABLED_KEY = "feedback_sound_enabled"
        private const val REQUEST_BT_CONNECT = 42
        private const val SUPABASE_KEY =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlsZ3ZkZWlxYWFpa2NvaGhwd2ZpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDQyNjQwMTEsImV4cCI6MjA1OTg0MDAxMX0.-JYW9jeUuFW8gBsmtv-OHKYbAVKsj0IAWU80zGWnwFU"
        private const val FEEDBACK_URL =
            "https://ylgvdeiqaaikcohhpwfi.supabase.co/rest/v1/feedback"
    }

    private val prefs by lazy {
        requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun ensureBluetoothPermissionAndThenRun(block: () -> Unit) {
        val connectPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2) {
            "android.permission.BLUETOOTH_CONNECT"
        } else {
            null
        }

        if (
            connectPerm != null &&
            ContextCompat.checkSelfPermission(requireContext(), connectPerm) != PackageManager.PERMISSION_GRANTED
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

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(requireContext(), theme).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCanceledOnTouchOutside(true)
            window?.setWindowAnimations(0)
            setOnShowListener {
                window?.let { dialogWindow ->
                    dialogWindow.setLayout(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    dialogWindow.setBackgroundDrawableResource(android.R.color.transparent)
                    dialogWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                    dialogWindow.decorView.setPadding(0, 0, 0, 0)
                    dialogWindow.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                    val attrs = dialogWindow.attributes
                    attrs.dimAmount = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 0.48f else 0.42f
                    attrs.windowAnimations = android.R.style.Animation_Dialog
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        attrs.blurBehindRadius = 32
                        dialogWindow.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    }
                    dialogWindow.attributes = attrs
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                FlightsTheme {
                    ModernFeedbackSheet(
                        text = feedbackText,
                        soundEnabled = soundEnabled,
                        soundControlsEnabled = soundControlsEnabled,
                        serverState = serverState,
                        statusMessage = statusMessage,
                        isSending = isSending,
                        inputEnabled = inputEnabled,
                        isBanned = isBanned,
                        pulseActive = typingPulseActive && soundEnabled,
                        successActive = successEffectActive,
                        onTextChange = ::handleFeedbackTextChange,
                        onSoundChange = ::updateSoundPreference,
                        onDismiss = ::dismiss,
                        onSend = {
                            ensureBluetoothPermissionAndThenRun {
                                actuallySendFeedback()
                            }
                        },
                        onRules = ::showRulesDialog,
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeRuntime()
        updateStatusUI(null)

        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            fcmToken = token
            Log.d("FCM", "FCM token cached: $token")
        }

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val deviceId = getHardDeviceId()
            try {
                val (banned, banEnd) = checkBanStatus(deviceId)
                isBanned = banned
                banEndEpoch = banEnd
                withContext(Dispatchers.Main) {
                    if (isBanned) {
                        showBanStatus()
                    } else {
                        startPeriodicServerChecks()
                    }
                }
            } catch (e: Exception) {
                Log.e("BanCheck", "Error checking ban status: ${e.message}")
                withContext(Dispatchers.Main) {
                    inputEnabled = false
                    isSending = false
                    statusMessage = getString(R.string.ban_status_unavailable)
                    startPeriodicServerChecks()
                }
            }
        }
    }

    override fun onDestroyView() {
        banCountdownTimer?.cancel()
        typingPulseJob?.cancel()
        if (::soundPool.isInitialized) {
            soundPool.release()
        }
        stopPeriodicServerChecks()
        super.onDestroyView()
    }

    private fun initializeRuntime() {
        soundEnabled = prefs.getBoolean(SOUND_ENABLED_KEY, true)
        soundControlsEnabled = true
        soundPool = SoundPool.Builder().setMaxStreams(2).build().also {
            clickSoundId = it.load(requireContext(), R.raw.time_click, 1)
            successSoundId = it.load(requireContext(), R.raw.success, 1)
        }
    }

    private fun updateSoundPreference(enabled: Boolean) {
        soundEnabled = enabled
        prefs.edit { putBoolean(SOUND_ENABLED_KEY, enabled) }
    }

    private fun handleFeedbackTextChange(value: String) {
        val grew = value.length > feedbackText.length
        feedbackText = value
        if (grew && soundEnabled && ::soundPool.isInitialized) {
            playClickFeedback(requireContext())
            typingPulseActive = true
            typingPulseJob?.cancel()
            typingPulseJob = viewLifecycleOwner.lifecycleScope.launch {
                delay(700)
                typingPulseActive = false
            }
        }
    }

    private fun showRulesDialog() {
        val rules = arrayOf(
            "No profanity or hate speech",
            "No personal data or doxxing",
            "Violations incur a 60-day ban",
            "No harassment or bullying",
            "No spam, advertising, or self-promotion"
        ).joinToString("\n") { "• $it" }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.feedback_rules)
            .setMessage(rules)
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
        inputEnabled = false
        soundControlsEnabled = false
        isSending = false
        serverState = FeedbackServerState.Offline

        banCountdownTimer?.cancel()
        banCountdownTimer = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            while (isActive) {
                val timeLeft = (banEndEpoch ?: 0L) - (System.currentTimeMillis() / 1000)
                if (timeLeft <= 0) {
                    if (isAdded) {
                        isBanned = false
                        banEndEpoch = null
                        banStatusChecked = false
                        inputEnabled = true
                        soundControlsEnabled = true
                        updateStatusUI(true)
                    }
                    break
                }
                val days = timeLeft / 86400
                val hours = (timeLeft % 86400) / 3600
                val minutes = (timeLeft % 3600) / 60
                val seconds = timeLeft % 60
                statusMessage = when {
                    days > 0 -> getString(R.string.ban_days_full, days, hours, minutes, seconds)
                    hours > 0 -> getString(R.string.ban_hours_full, hours, minutes, seconds)
                    minutes > 0 -> getString(R.string.ban_minutes_full, minutes, seconds)
                    else -> getString(R.string.ban_seconds_only, seconds)
                }
                delay(500L)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun playClickFeedback(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
            manager.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
        }
        vibrator.vibrate(
            android.os.VibrationEffect.createOneShot(
                5,
                android.os.VibrationEffect.DEFAULT_AMPLITUDE
            )
        )

        val audioMgr = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVol = audioMgr.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat().coerceAtLeast(1f)
        val curVol = audioMgr.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
        val playVol = (curVol / maxVol) * 0.45f
        soundPool.play(clickSoundId, playVol, playVol, 0, 0, 1f)
    }

    private fun onFeedbackSentSuccess() {
        feedbackText = ""
        statusMessage = getString(R.string.feedback_sent_success)
        isSending = false
        inputEnabled = true
        successEffectActive = true
        viewLifecycleOwner.lifecycleScope.launch {
            delay(1_150)
            successEffectActive = false
        }

        if (!soundEnabled || !::soundPool.isInitialized) return
        val vol = 0.3f
        soundPool.play(successSoundId, vol, vol, 0, 0, 1f)
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
        val trimmedFeedback = feedbackText.trim()
        if (trimmedFeedback.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter feedback.", Toast.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            val deviceId = getHardDeviceId()
            if (deviceIsBanned(deviceId)) {
                showBanStatus()
                return@launch
            }

            isSending = true
            inputEnabled = false
            statusMessage = getString(R.string.sending_feedback)

            val progressJob = launch {
                while (isActive) {
                    delay(120)
                }
            }

            val result = withContext(Dispatchers.IO) {
                val deviceInfo = getDeviceInfo(requireContext()).apply {
                    put("fcmToken", fcmToken)
                }
                val json = JSONObject().apply {
                    put("message", trimmedFeedback)
                    put("deviceInfo", deviceInfo)
                    put("fcmToken", fcmToken)
                }
                val body = json.toString().toRequestBody("application/json".toMediaType())

                if (isServerOnline) {
                    val request = supabaseRequestBuilder(FEEDBACK_URL)
                        .post(body)
                        .build()
                    if (sharedClient.newCall(request).execute().isSuccessful) "sent" else "error"
                } else {
                    if (queueFeedbackLocally(trimmedFeedback)) "queued" else "queue_error"
                }
            }

            progressJob.cancelAndJoin()
            delay(120)

            when (result) {
                "sent" -> {
                    onFeedbackSentSuccess()
                    delay(1_000)
                    updateStatusUI(true)
                }
                "queued" -> {
                    feedbackText = ""
                    statusMessage = getString(R.string.feedback_queued_success)
                    isSending = false
                    inputEnabled = true
                }
                "error" -> {
                    statusMessage = getString(R.string.feedback_send_error)
                    isSending = false
                    inputEnabled = true
                }
                "queue_error" -> {
                    statusMessage = getString(R.string.feedback_queue_error, "Local queue unavailable")
                    isSending = false
                    inputEnabled = true
                }
            }
        }
    }

    private fun startPeriodicServerChecks() {
        serverCheckJob?.cancel()
        serverCheckJob = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            checkServerStatus()
            while (isActive) {
                delay(checkInterval)
                checkServerStatus()
            }
        }
    }

    private fun stopPeriodicServerChecks() {
        serverCheckJob?.cancel()
        serverCheckJob = null
    }

    private suspend fun checkServerStatus() {
        withContext(Dispatchers.IO) {
            val request = supabaseRequestBuilder("$FEEDBACK_URL?select=id&limit=1")
                .get()
                .build()

            try {
                val response = sharedClient.newCall(request).execute()
                val online = response.isSuccessful

                withContext(Dispatchers.Main) {
                    if (!isServerOnline && online) {
                        processQueuedFeedback()
                    }
                    isServerOnline = online
                    if (!isBanned) {
                        updateStatusUI(online)
                    }
                }

                val deviceId = getHardDeviceId()
                val (currentlyBanned, banEnd) = checkBanStatus(deviceId)

                withContext(Dispatchers.Main) {
                    if (currentlyBanned && !isBanned) {
                        isBanned = true
                        banEndEpoch = banEnd
                        banStatusChecked = true
                        showBanStatus()
                    } else if (!currentlyBanned && isBanned) {
                        isBanned = false
                        banEndEpoch = null
                        banStatusChecked = false
                        inputEnabled = true
                        soundControlsEnabled = true
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

    private fun queueFeedbackLocally(feedbackText: String): Boolean {
        if (feedbackText.isBlank()) {
            Log.w("QueueFeedback", "Empty feedback not queued.")
            return false
        }

        return try {
            val queueFile = File(requireContext().filesDir, "feedbackQueue.json")
            val feedbackQueue = if (queueFile.exists()) {
                JSONObject(queueFile.readText()).optJSONArray("queue") ?: JSONArray()
            } else {
                JSONArray()
            }

            feedbackQueue.put(feedbackText)
            queueFile.writeText(JSONObject().put("queue", feedbackQueue).toString())
            true
        } catch (e: Exception) {
            Log.e("QueueFeedback", "Error queuing feedback: ${e.message}")
            false
        }
    }

    private fun processQueuedFeedback() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val queueFile = File(requireContext().filesDir, "feedbackQueue.json")
                if (!queueFile.exists()) return@launch

                val feedbackQueue = JSONObject(queueFile.readText())
                    .optJSONArray("queue") ?: JSONArray()

                if (feedbackQueue.length() == 0) {
                    queueFile.delete()
                    return@launch
                }

                val failedQueue = JSONArray()
                var processedCount = 0

                for (i in 0 until feedbackQueue.length()) {
                    processedCount++
                    val queuedFeedback = feedbackQueue.getString(i).trim()
                    if (queuedFeedback.isBlank()) continue

                    val token = FirebaseMessaging.getInstance().token.await()
                    val deviceInfo = getDeviceInfo(requireContext()).apply {
                        put("fcmToken", token)
                    }
                    val json = JSONObject().apply {
                        put("message", queuedFeedback)
                        put("deviceInfo", deviceInfo)
                        put("fcmToken", token)
                    }
                    val body = json.toString()
                        .toRequestBody("application/json; charset=utf-8".toMediaType())

                    val request = supabaseRequestBuilder(FEEDBACK_URL)
                        .post(body)
                        .build()

                    try {
                        val response = sharedClient.newCall(request).execute()
                        val responseBody = response.body.string()
                        if (!response.isSuccessful) {
                            Log.e("ProcessQueue", "Failed to send queued feedback. Code: ${response.code}. Body: $responseBody")
                            failedQueue.put(queuedFeedback)
                        }
                    } catch (e: Exception) {
                        Log.e("ProcessQueue", "Exception: ${e.message}")
                        failedQueue.put(queuedFeedback)
                    }
                }

                if (failedQueue.length() == 0) {
                    queueFile.delete()
                } else {
                    queueFile.writeText(JSONObject().put("queue", failedQueue).toString())
                }

                withContext(Dispatchers.Main) {
                    if (processedCount == 0) return@withContext
                    statusMessage = if (failedQueue.length() == 0) {
                        getString(R.string.feedback_all_sent)
                    } else {
                        getString(R.string.feedback_retry_later)
                    }
                }
            } catch (e: Exception) {
                Log.e("ProcessQueue", "Final error: ${e.message}")
                withContext(Dispatchers.Main) {
                    statusMessage = getString(R.string.feedback_send_exception_error, e.message)
                }
            }
        }
    }

    private fun checkBanStatus(deviceId: String): Pair<Boolean, Long?> {
        val url = "https://ylgvdeiqaaikcohhpwfi.supabase.co/rest/v1/bans_table?device_id=eq.$deviceId&select=ban_end"
        val request = supabaseRequestBuilder(url).get().build()
        val response = sharedClient.newCall(request).execute()

        if (response.isSuccessful) {
            val body = response.body.string()
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

        when (isOnline) {
            null -> {
                serverState = FeedbackServerState.Checking
                statusMessage = getString(R.string.checking_status)
            }
            true -> {
                serverState = FeedbackServerState.Online
                statusMessage = getString(R.string.server_online)
                inputEnabled = !isSending
                soundControlsEnabled = true
            }
            false -> {
                serverState = FeedbackServerState.Offline
                statusMessage = getString(R.string.server_offline)
                inputEnabled = !isSending
                soundControlsEnabled = true
            }
        }
    }

    private fun supabaseRequestBuilder(url: String): Request.Builder {
        return Request.Builder()
            .url(url)
            .addHeader("apikey", SUPABASE_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_KEY")
            .addHeader("Content-Type", "application/json")
    }

    private fun isDeviceRooted(): Boolean {
        Build.TAGS?.let {
            if (it.contains("test-keys")) return true
        }
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
        val pm = context.packageManager
        val pkgInfo = pm.getPackageInfo(context.packageName, 0)
        val metrics = context.resources.displayMetrics
        val wm = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
        val am = (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
        val memInfo = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }

        deviceInfo.put("deviceModel", Build.MODEL)
        deviceInfo.put("osVersion", Build.VERSION.RELEASE)
        deviceInfo.put("appVersion", pkgInfo.versionName)
        deviceInfo.put("manufacturer", Build.MANUFACTURER)
        deviceInfo.put("deviceId", getHardDeviceId())
        deviceInfo.put("screenResolution", "${metrics.widthPixels} x ${metrics.heightPixels}")
        deviceInfo.put("networkType", getNetworkType(context))
        deviceInfo.put("isRooted", isDeviceRooted())
        deviceInfo.put("hardware", Build.HARDWARE)
        deviceInfo.put("refreshRate", wm.defaultDisplay.refreshRate.toDouble())
        deviceInfo.put("timeZone", TimeZone.getDefault().id)
        deviceInfo.put("totalRam", memInfo.totalMem)
        deviceInfo.put("availRam", memInfo.availMem)

        try {
            val btName = BluetoothAdapter.getDefaultAdapter()?.name
            val globalName = Settings.Global.getString(
                context.contentResolver,
                Settings.Global.DEVICE_NAME
            )
            deviceInfo.put("deviceName", btName ?: globalName ?: Build.MODEL)
        } catch (_: SecurityException) {
        } catch (_: Exception) {
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
        }

        return "UNKNOWN"
    }

    @Composable
    private fun ModernFeedbackSheet(
        text: String,
        soundEnabled: Boolean,
        soundControlsEnabled: Boolean,
        serverState: FeedbackServerState,
        statusMessage: String,
        isSending: Boolean,
        inputEnabled: Boolean,
        isBanned: Boolean,
        pulseActive: Boolean,
        successActive: Boolean,
        onTextChange: (String) -> Unit,
        onSoundChange: (Boolean) -> Unit,
        onDismiss: () -> Unit,
        onSend: () -> Unit,
        onRules: () -> Unit,
    ) {
        val dark = isSystemInDarkTheme()
        var panelVisible by remember { mutableStateOf(false) }
        val sheetShape = RoundedCornerShape(27.dp)
        val accent = if (dark) ComposeColor(0xFF8FC7FF) else ComposeColor(0xFF096F70)
        val sheetBg = if (dark) {
            ComposeColor(0xFF202124).copy(alpha = 0.74f)
        } else {
            ComposeColor(0xFFE6E2E7).copy(alpha = 0.70f)
        }
        val animatedSheetBg by animateColorAsState(
            targetValue = if (successActive) {
                if (dark) ComposeColor(0xFF123927).copy(alpha = 0.88f)
                else ComposeColor(0xFFD8F6E5).copy(alpha = 0.88f)
            } else {
                sheetBg
            },
            animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
            label = "feedbackSuccessTint"
        )
        val border = if (dark) ComposeColor.White.copy(alpha = 0.12f) else ComposeColor.Black.copy(alpha = 0.08f)

        LaunchedEffect(Unit) {
            panelVisible = true
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ComposeColor.Black.copy(alpha = if (dark) 0.38f else 0.18f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .imePadding()
                .navigationBarsPadding()
                .padding(start = 18.dp, end = 18.dp, bottom = 18.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            AnimatedVisibility(
                visible = panelVisible,
                enter = slideInVertically(
                    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                    initialOffsetY = { it / 2 }
                ) + fadeIn(animationSpec = tween(durationMillis = 120)) +
                    scaleIn(
                        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                        initialScale = 0.96f
                    ),
                exit = slideOutVertically(
                    animationSpec = tween(durationMillis = 170, easing = FastOutLinearInEasing),
                    targetOffsetY = { it / 3 }
                ) + fadeOut(animationSpec = tween(durationMillis = 120)) +
                    scaleOut(
                        animationSpec = tween(durationMillis = 170, easing = FastOutLinearInEasing),
                        targetScale = 0.98f
                    )
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .clip(sheetShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                        .border(BorderStroke(1.dp, border), sheetShape),
                    shape = sheetShape,
                    color = animatedSheetBg,
                    tonalElevation = 8.dp,
                    shadowElevation = 18.dp
                ) {
                    Box(Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            ComposeColor.White.copy(alpha = if (dark) 0.07f else 0.30f),
                                            ComposeColor.Transparent,
                                            accent.copy(alpha = if (dark) 0.10f else 0.08f),
                                        )
                                    )
                                )
                                .padding(start = 20.dp, top = 14.dp, end = 20.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Feedback",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp,
                                    letterSpacing = 0.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier.size(38.dp),
                                    shape = CircleShape,
                                    color = accent.copy(alpha = if (dark) 0.22f else 0.14f)
                                ) {
                                    IconButton(onClick = onRules) {
                                        Icon(
                                            imageVector = Icons.Filled.Info,
                                            contentDescription = null,
                                            tint = accent,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                }
                                Surface(
                                    modifier = Modifier.size(38.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (dark) 0.12f else 0.09f)
                                ) {
                                    IconButton(onClick = onDismiss) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Close",
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                }
                            }
                        }
                        Text(
                            text = "Send a note with device info attached.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FeedbackStatusPill(
                            serverState = serverState,
                            isBanned = isBanned,
                            modifier = Modifier.weight(0.95f)
                        )
                        InlineTypingPulse(
                            accent = accent,
                            active = pulseActive,
                            modifier = Modifier.weight(0.65f)
                        )
                        SoundToggleCard(
                            soundEnabled = soundEnabled,
                            enabled = soundControlsEnabled,
                            onSoundChange = onSoundChange
                        )
                    }

                    OutlinedTextField(
                        value = text,
                        onValueChange = onTextChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(92.dp),
                        enabled = inputEnabled && !isBanned,
                        placeholder = {
                            Text(
                                text = if (isBanned) {
                                    getString(R.string.ban_reason_hint)
                                } else {
                                    getString(R.string.enter_your_feedback_here)
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                            )
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = if (dark) 0.55f else 0.62f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = if (dark) 0.38f else 0.56f),
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f),
                            focusedBorderColor = accent.copy(alpha = 0.72f),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                            disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                        ),
                        minLines = 2,
                        maxLines = 3,
                    )

                    Button(
                        onClick = onSend,
                        enabled = !isSending && inputEnabled && !isBanned,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent,
                            contentColor = if (dark) ComposeColor(0xFF071016) else ComposeColor.White,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                        )
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(17.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(getString(R.string.sending_feedback), fontWeight = FontWeight.SemiBold)
                        } else {
                            Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(getString(R.string.send_feedback), fontWeight = FontWeight.SemiBold)
                        }
                    }

                    val secondaryStatus = statusMessage.takeUnless {
                        it == getString(R.string.server_online) ||
                            it == getString(R.string.server_offline) ||
                            it == getString(R.string.checking_status)
                    }.orEmpty()
                    if (secondaryStatus.isNotBlank()) {
                        Text(
                            text = secondaryStatus,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodySmall,
                            color = when {
                                isBanned -> MaterialTheme.colorScheme.error
                                serverState == FeedbackServerState.Online -> ComposeColor(0xFF2EAD68)
                                serverState == FeedbackServerState.Offline -> accent
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            textAlign = TextAlign.Center
                        )
                    }
                }
                        SuccessConfetti(
                            active = successActive,
                            modifier = Modifier.matchParentSize()
                        )
            }
        }
    }
        }
    }

    @Composable
    private fun SuccessConfetti(
        active: Boolean,
        modifier: Modifier = Modifier,
    ) {
        val progress by animateFloatAsState(
            targetValue = if (active) 1f else 0f,
            animationSpec = tween(durationMillis = 950, easing = FastOutSlowInEasing),
            label = "feedbackConfetti"
        )
        if (progress <= 0.01f) return

        val particles = remember {
            listOf(
                Triple(0.12f, 0.22f, ComposeColor(0xFF8FC7FF)),
                Triple(0.24f, 0.10f, ComposeColor(0xFFFFD166)),
                Triple(0.38f, 0.18f, ComposeColor(0xFF55DDA8)),
                Triple(0.52f, 0.08f, ComposeColor(0xFFFF8FAB)),
                Triple(0.68f, 0.19f, ComposeColor(0xFFB9A7FF)),
                Triple(0.84f, 0.13f, ComposeColor(0xFFFFB86B)),
                Triple(0.18f, 0.74f, ComposeColor(0xFF55DDA8)),
                Triple(0.44f, 0.82f, ComposeColor(0xFFFFD166)),
                Triple(0.72f, 0.70f, ComposeColor(0xFF8FC7FF)),
                Triple(0.88f, 0.78f, ComposeColor(0xFFFF8FAB)),
            )
        }
        Canvas(modifier) {
            val alpha = if (progress < 0.75f) 1f else (1f - progress) / 0.25f
            particles.forEachIndexed { index, particle ->
                val drift = ((index % 3) - 1) * 18f * progress
                val x = size.width * particle.first + drift
                val y = size.height * particle.second + (30f * progress)
                drawCircle(
                    color = particle.third.copy(alpha = alpha.coerceIn(0f, 1f)),
                    radius = (5f + (index % 3) * 2f) * (1f - progress * 0.15f),
                    center = androidx.compose.ui.geometry.Offset(x, y)
                )
            }
        }
    }

    @Composable
    private fun FeedbackStatusPill(
        serverState: FeedbackServerState,
        isBanned: Boolean,
        modifier: Modifier = Modifier,
    ) {
        val (label, icon, color) = when {
            isBanned -> Triple("Banned", Icons.Filled.GppMaybe, MaterialTheme.colorScheme.error)
            serverState == FeedbackServerState.Online -> Triple(getString(R.string.server_online), Icons.Filled.Wifi, ComposeColor(0xFF2EAD68))
            serverState == FeedbackServerState.Offline -> Triple(getString(R.string.server_offline), Icons.Filled.CloudOff, MaterialTheme.colorScheme.primary)
            else -> Triple(getString(R.string.checking_status), Icons.Filled.Wifi, MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Surface(
            modifier = modifier.height(40.dp),
            shape = RoundedCornerShape(16.dp),
            color = color.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, color.copy(alpha = 0.22f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = color,
                    maxLines = 1
                )
            }
        }
    }

    @Composable
    private fun SoundToggleCard(
        soundEnabled: Boolean,
        enabled: Boolean,
        onSoundChange: (Boolean) -> Unit,
    ) {
        Surface(
            modifier = Modifier
                .height(40.dp)
                .width(74.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable(enabled = enabled) { onSoundChange(!soundEnabled) },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = if (soundEnabled) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(17.dp)
                )
                MiniToggle(
                    checked = soundEnabled,
                    enabled = enabled,
                    accent = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    @Composable
    private fun MiniToggle(
        checked: Boolean,
        enabled: Boolean,
        accent: ComposeColor,
    ) {
        val trackColor = when {
            !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
            checked -> accent.copy(alpha = 0.34f)
            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        }
        Box(
            modifier = Modifier
                .size(width = 34.dp, height = 20.dp)
                .clip(CircleShape)
                .background(trackColor)
                .padding(2.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                    .clip(CircleShape)
                    .background(if (checked && enabled) accent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.46f))
            )
        }
    }

    @Composable
    private fun InlineTypingPulse(
        accent: ComposeColor,
        active: Boolean,
        modifier: Modifier = Modifier,
    ) {
        Surface(
            modifier = modifier.height(40.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
        ) {
            TypingPulse(
                accent = accent.copy(alpha = if (active) 1f else 0.28f),
                active = active
            )
        }
    }

    @Composable
    private fun TypingPulse(
        accent: ComposeColor,
        active: Boolean = true,
    ) {
        if (!active) {
            Box(Modifier.fillMaxSize())
            return
        }
        val transition = rememberInfiniteTransition(label = "feedbackTyping")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { index ->
                val height by transition.animateFloat(
                    initialValue = if (active) 7f else 10f,
                    targetValue = if (active) 23f else 10f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(430 + index * 80, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "feedbackBar$index"
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(width = 5.dp, height = height.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.70f))
                )
            }
        }
    }
}
