package com.flights.studio

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.text.SpannableString
import android.text.style.ReplacementSpan
import android.util.Log
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.OrientationEventListener
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.window.OnBackInvokedDispatcher
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.BuildCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.github.piasy.biv.BigImageViewer
import com.github.piasy.biv.loader.glide.GlideImageLoader
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.ortiz.touchview.TouchImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    // region Variables and constants
    private lateinit var orientationListener: OrientationEventListener
    private lateinit var networkConnectivityHelper: NetworkConnectivityHelper
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private var bottomSheetFragment: CustomBottomSheetFooter? = null
    private val refreshInterval: Long = 90_000L // 90 seconds
    private lateinit var refreshHandler: Handler
    private lateinit var refreshRunnable: Runnable
    private lateinit var countdownHandler: Handler
    private lateinit var countdownRunnable: Runnable
    private lateinit var countdownTextView: TextView
    private lateinit var refreshingTextView: TextView
    private lateinit var refreshingInTextView: TextView
    private lateinit var nointernetTextView: TextView
    private lateinit var countdownDrawable: ClipDrawable
    private var isRefreshing: Boolean = false
    private var countdownTime: Long = 0
    private lateinit var currentTimeTextView: TextView
    private lateinit var batteryInfoTextView: TextView
    private lateinit var batteryManagerHelper: BatteryManagerHelper
    private var isCountdownRunning: Boolean = false
    private var isNoInternetBottomSheetShown = false
    private var lastRefreshTime: Long = 0
    private val minRefreshInterval = 1000L // 1 second cooldown
    private lateinit var fullScreenHelper: FullScreenHelper
    private lateinit var cardView: MaterialCardView
    private lateinit var photoView: TouchImageView
    private var isFullScreen = false
    private lateinit var errorImageView: ImageView
    private var cachedBottomSheet: CustomBottomSheetFooter? = null
    private var isImageLoaded = false
    private val viewModel: SplashViewModel by viewModels()
    private var didTickOvershoot = false
    private var didConfirmExpand = false
    private var didConfirmCollapse = false
    private var hapticsAttached = false
    private lateinit var composeHost: FrameLayout

    private fun collapsedElevationPx(): Float =
        resources.getDimension(R.dimen.compose_host_elevation_collapsed)




    companion object {
        private const val MAX_DRAWABLE_LEVEL = 10000
        private const val PULSE_THRESHOLD = 5
        private const val PULSE_DURATION = 400L
        private const val NOTIFICATION_PERMISSION_REQUEST = 1010
    }
    // endregion

    // region Broadcast receiver
    private val imageLoadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            loadNetworkImage()
        }
    }

    override fun onStart() {
        super.onStart()
        Glide.with(this).onStart()
    }

    override fun onStop() {
        super.onStop()
        Glide.with(this).onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        WindowCompat.setDecorFitsSystemWindows(window, false)


        initializeViews()
        setupNotificationPermission()
        setupBlurEffect()
        setupBigImageViewer()
        setupBackPressedCallback()
        setupFullScreenHelper()
        setupClickListeners()
        setupHandlersAndStartCountdown()
        initializeHelpers()
        startServices()
        observeCurrentTime()


        val host = findViewById<FrameLayout>(R.id.compose_host)

        val composeOverlay = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)

            // Bottom pinned inside the host; host is already at the bottom
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.BOTTOM
            )

            setContent {
                MaterialTheme {
                    FlightsGlassScreen(
                        onTabChanged = { tab ->
                            triggerRefreshImageAnimation(
                                when (tab) {
                                    FlightsTab.Curb  -> "https://www.jacksonholeairport.com/wp-content/uploads/webcams/parking-curb.jpg?v=${System.currentTimeMillis()}"
                                    FlightsTab.North -> "https://www.jacksonholeairport.com/wp-content/uploads/webcams/parking-north.jpg?v=${System.currentTimeMillis()}"
                                    FlightsTab.South -> "https://www.jacksonholeairport.com/wp-content/uploads/webcams/parking-south.jpg?v=${System.currentTimeMillis()}"
                                }
                            )
                        },
                        onFullScreen = { openFullScreenImages() },
                        onMenu = { openCustomBottomSheet() },
                        onBack = { onBackPressedDispatcher.onBackPressed() }
                    )
                }
            }
        }


        host.removeAllViews()
        host.addView(composeOverlay)

        // --- Compose #2: the glass buttons grid that replaces the XML include
        val cardsHost = findViewById<FrameLayout>(R.id.cards_host)
        val cardsCompose = ComposeView(this).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnDetachedFromWindow
            )
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.CENTER
            )

            setContent {
                // Gradient-backed glass grid (no backdropUrl passed)
                SplashGlassScreen(
                    onOpen = { id ->
                        when (id) {
                            "card1" -> openBottomSheetWithCard("card1")
                            "card2" -> openBottomSheetWithCard("card2")
                            "card3" -> openBottomSheetWithCard("card3")
                            "card4" -> openBottomSheetWithCard("card4")
                            "card5" -> openActivityWithCard("card5")
                            "card6" -> openActivityWithCard("card6")
                            "card7" -> openActivityWithCard("card7")
                            "card8" -> openActivityWithCard("card8")
                            "card9" -> startActivity(
                                Intent(this@SplashActivity, ProfileDetailsActivity::class.java)
                            )
                            else -> Unit
                        }
                    },
                    gridHeightFraction = 0.59f // same height as your old XML percent
                )
            }
        }
        cardsHost.removeAllViews()
        cardsHost.addView(cardsCompose)
// keep this after cardsHost.addView(cardsCompose)




    }



    // region Initialization helpers
    private fun initializeViews() {
        countdownTextView = findViewById(R.id.countdownTextView)
        refreshingTextView = findViewById(R.id.refreshingTextView)
        refreshingInTextView = findViewById(R.id.refreshingInTextView)
        nointernetTextView = findViewById(R.id.nointernetTextView)
        batteryInfoTextView = findViewById(R.id.battery_info)
        photoView = findViewById(R.id.photoView)
        currentTimeTextView = findViewById(R.id.currentTimeTextView)
        cardView = findViewById(R.id.cardView2)
        errorImageView = findViewById(R.id.errorImageView)
        composeHost = findViewById(R.id.compose_host)

        val countdownBackground = countdownTextView.background
        if (countdownBackground is LayerDrawable) {
            val clipDrawable = countdownBackground.findDrawableByLayerId(R.id.fill)
            if (clipDrawable is ClipDrawable) {
                countdownDrawable = clipDrawable
                countdownDrawable.level = MAX_DRAWABLE_LEVEL
                countdownDrawable.setTint(ContextCompat.getColor(this, R.color.soft_blue))
            }
        }
    }



    // Keep this somewhere below



    private fun setupNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                Handler(Looper.getMainLooper()).postDelayed({
                    MaterialAlertDialogBuilder(this)
                        .setTitle("Enable Notifications")
                        .setMessage("We need notification access to keep you updated. Please allow it.")
                        .setPositiveButton("Allow") { _, _ ->
                            ActivityCompat.requestPermissions(
                                this,
                                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                                NOTIFICATION_PERMISSION_REQUEST
                            )
                        }
                        .setNegativeButton("No Thanks", null)
                        .show()
                }, 1000)
            }
        }
    }

    private fun setupBlurEffect() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2) {
            val blurRadius = 25f
            val blurEffect = RenderEffect.createBlurEffect(
                blurRadius, blurRadius, Shader.TileMode.CLAMP
            )
            val cardIds = listOf(
                R.id.card10, R.id.card11, R.id.card12, R.id.card13,
                R.id.card14, R.id.card15, R.id.card16
            )
            cardIds.forEach { id ->
                findViewById<MaterialCardView>(id)?.setRenderEffect(blurEffect)
            }
        }
    }

    private fun setupBigImageViewer() {
        BigImageViewer.initialize(GlideImageLoader.with(applicationContext))
        window.decorView.alpha = 1.0f
    }

    private fun setupBackPressedCallback() {
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        if (BuildCompat.isAtLeastT()) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT
            ) {
                Log.d("MainActivity", "Back gesture triggered in OnBackInvokedCallback")
            }
        }
    }

    private fun setupFullScreenHelper() {
        fullScreenHelper = FullScreenHelper()
        val widgetTimerContainer = findViewById<View>(R.id.widget_timer_container)
        widgetTimerContainer.isClickable = false
        widgetTimerContainer.isFocusable = false
    }

    private fun setupClickListeners() {
        val expandButton = findViewById<ImageView>(R.id.expand)
        val motion = findViewById<MotionLayout>(R.id.rootLayout)
        setupHaptics(motion)

        expandButton.setOnClickListener {
            if (motion.isInteractionEnabled.not()) return@setOnClickListener
            val goingFull = motion.currentState == R.id.collapsed || motion.currentState == -1
            if (goingFull) {
                motion.setTransition(R.id.t_expand)
                motion.transitionToEnd()
            } else {
                motion.setTransition(R.id.t_collapse)
                motion.transitionToEnd()
            }
            expandButton.animate()
                .alpha(0f).scaleX(0.6f).scaleY(0.6f).setDuration(120)
                .withEndAction {
                    expandButton.setImageResource(
                        if (goingFull)
                            R.drawable.fullscreen_exit_16dp_ffffff_fill0_wght400_grad0_opsz20
                        else
                            R.drawable.ic_oui_ratio_full
                    )
                    expandButton.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(120).start()
                }.start()
        }

        // NOTE:
        // - DO NOT set listeners for full_screen / menu_action / nav_* here.
        //   They are handled by the Compose bottom bar callbacks.
        // - Keep your card click listeners only if those cards still exist in your layout.
//        findViewById<View>(R.id.card1).setOnClickListener { openBottomSheetWithCard("card1") }
//        findViewById<View>(R.id.card2).setOnClickListener { openBottomSheetWithCard("card2") }
//        findViewById<View>(R.id.card3).setOnClickListener { openBottomSheetWithCard("card3") }
//        findViewById<View>(R.id.card4).setOnClickListener { openBottomSheetWithCard("card4") }
//        findViewById<View>(R.id.card5).setOnClickListener { openActivityWithCard("card5") }
//        findViewById<View>(R.id.card6).setOnClickListener { openActivityWithCard("card6") }
//        findViewById<View>(R.id.card7).setOnClickListener { openActivityWithCard("card7") }
//        findViewById<View>(R.id.card8).setOnClickListener { openActivityWithCard("card8") }
//        findViewById<View>(R.id.card9).setOnClickListener {
//            startActivity(Intent(this, ProfileDetailsActivity::class.java))
//            overridePendingTransition(R.anim.zoom_in, 0)
//        }
    }




    private fun initializeHelpers() {
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        batteryManagerHelper = BatteryManagerHelper(this).apply {
            onBatteryStatusUpdated = { batteryPct, isCharging ->
                updateBatteryStatusUI(batteryPct, isCharging)
            }
        }
        networkConnectivityHelper = NetworkConnectivityHelper(this)

    }

    private fun startServices() {
        startCountdownAndRefreshCycle()
        initializeFirebase()
        setStatusBarColor()
        preloadBottomSheet()

        val filter = IntentFilter("LOAD_IMAGE")
        LocalBroadcastManager.getInstance(this).registerReceiver(imageLoadReceiver, filter)

    }
    // endregion

    // region Lifecycle overrides
    override fun onResume() {
        super.onResume()
        fullScreenHelper.resetToDefaultState(cardView, photoView, errorImageView)
        batteryManagerHelper.startMonitoring()

        val intentFilter = IntentFilter("android.net.conn.CONNECTIVITY_CHANGE")
        try {
            registerReceiver(networkStateReceiver, intentFilter)
        } catch (e: IllegalArgumentException) {
            Log.w("NetworkState", "Receiver already registered or error occurred: ${e.message}")
        }
        lifecycleScope.launch {
            val isConnected = networkConnectivityHelper.isInternetAvailableFast()
            if (isConnected) {
                if (!isCountdownRunning) {
                    startCountdownAndRefreshCycle()
                }
                if (!isImageLoaded) {
                    triggerRefreshImageAnimation()
                    isImageLoaded = true
                }
            } else {
                handleNoInternetState()
            }
        }
        if (intent.getBooleanExtra("force_refresh", false)) {
            intent.removeExtra("force_refresh")
            startCountdownAndRefreshCycle()
        }
    }

    override fun onPause() {
        super.onPause()
        cardView.clearAnimation()
        photoView.clearAnimation()
        batteryManagerHelper.stopMonitoring()

        // Keep blur guardian running even when paused (comment this out if you want to stop it)
        // stopBlurGuardian()

        if (::refreshHandler.isInitialized) {
            refreshHandler.removeCallbacksAndMessages(null)
        }
        if (::countdownHandler.isInitialized) {
            countdownHandler.removeCallbacksAndMessages(null)
        }

        try {
            unregisterReceiver(networkStateReceiver)
        } catch (e: IllegalArgumentException) {
            Log.w("NetworkState", "Receiver was not registered: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        clearResources()
        findViewById<WebView>(R.id.webView)?.destroy()
        cardView.clearAnimation()
        photoView.clearAnimation()

        if (!isDestroyed && !isFinishing) {
            Glide.with(applicationContext).clear(photoView)
        }
        if (::refreshHandler.isInitialized) {
            refreshHandler.removeCallbacksAndMessages(null)
        }
        if (::countdownHandler.isInitialized) {
            countdownHandler.removeCallbacksAndMessages(null)
        }
        batteryManagerHelper.stopMonitoring()
        isCountdownRunning = false
    }

    // region Helper functions
    private fun preloadBottomSheet() {
        if (cachedBottomSheet == null) {
            cachedBottomSheet = CustomBottomSheetFooter().also {
                it.showNow(supportFragmentManager, "preload_sheet")
                it.dismiss()
            }
        }
    }

    private fun openBottomSheetWithCard(cardId: String) {
        val intent = Intent(this, CardBottomSheetActivity::class.java)
        intent.putExtra("CARD_ID", cardId)
        startActivity(intent)
    }

    private fun openFullScreenImages() {
        val bottomSheet = FullScreenImageBottomSheet.newInstance(
            "https://www.jacksonholeairport.com/wp-content/uploads/webcams/parking-curb.jpg",
            "https://www.jacksonholeairport.com/wp-content/uploads/webcams/parking-north.jpg",
            "https://www.jacksonholeairport.com/wp-content/uploads/webcams/parking-south.jpg"
        )
        bottomSheet.show(supportFragmentManager, "FullScreenImageBottomSheet")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("Notifications", "✅ User granted POST_NOTIFICATIONS")
            } else {
                val shouldAskAgain = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ActivityCompat.shouldShowRequestPermissionRationale(
                        this, android.Manifest.permission.POST_NOTIFICATIONS
                    )
                } else {
                    false
                }
                if (shouldAskAgain) {
                    Toast.makeText(
                        this,
                        "Permission denied. You can allow it later from settings.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    MaterialAlertDialogBuilder(this)
                        .setTitle("Notifications Blocked")
                        .setMessage("You denied notifications permanently. You can enable them manually in app settings.")
                        .setPositiveButton("Open Settings") { _, _ ->
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                            }
                            startActivity(intent)
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            }
        }
    }

    private fun updateBatteryStatusUI(batteryPct: Int, isCharging: Boolean) {
        batteryInfoTextView.text = if (batteryPct != -1) "$batteryPct%" else "N/A"

        val batteryIcon = ContextCompat.getDrawable(this, R.drawable.battery_icon) as? LayerDrawable
            ?: return

        val fillLayer = batteryIcon.findDrawableByLayerId(R.id.fill) as? ClipDrawable ?: return
        val chargerIcon = batteryIcon.findDrawableByLayerId(R.id.charger_icon) ?: return

        val targetLevel = (batteryPct * 10000 / 100)
        ObjectAnimator.ofInt(fillLayer, "level", fillLayer.level, targetLevel).apply {
            duration = 500
            interpolator = LinearInterpolator()
            start()
        }

        val startColor = batteryInfoTextView.currentTextColor
        val endColor = if (isCharging) {
            ContextCompat.getColor(this, R.color.soft_blue)
        } else {
            ContextCompat.getColor(this, R.color.white)
        }
        ValueAnimator.ofObject(ArgbEvaluator(), startColor, endColor).apply {
            duration = 500
            addUpdateListener { animation ->
                fillLayer.setTint(animation.animatedValue as Int)
            }
            start()
        }

        ObjectAnimator.ofInt(chargerIcon, "alpha", chargerIcon.alpha, if (isCharging) 255 else 0)
            .apply {
                duration = 500
                start()
            }

        val batteryBackground = batteryInfoTextView.background.mutate()
        val originalColor = ContextCompat.getColor(this, R.color.neon_blue)
        val yellowColor = ContextCompat.getColor(this, R.color.material_yellow)
        val greenColor = ContextCompat.getColor(this, R.color.green)

        val targetBackgroundColor = when {
            isCharging && batteryPct < 100 -> yellowColor
            isCharging && batteryPct == 100 -> greenColor
            else -> originalColor
        }
        ValueAnimator.ofObject(ArgbEvaluator(), batteryBackground.alpha, targetBackgroundColor)
            .apply {
                duration = 1000
                addUpdateListener { animator ->
                    batteryBackground.setTint(animator.animatedValue as Int)
                }
                start()
            }
        batteryInfoTextView.setCompoundDrawablesWithIntrinsicBounds(batteryIcon, null, null, null)
    }

    private fun observeCurrentTime() {
        viewModel.currentTime.observe(this) { timestamp ->
            val dateFormat = SimpleDateFormat("MMM/dd/yyyy", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val date = dateFormat.format(Date(timestamp))
            val time = timeFormat.format(Date(timestamp))

            val space = " "
            val spannable = SpannableString("$date$space$time")
            val spaceWidth = 20
            spannable.setSpan(
                object : ReplacementSpan() {
                    override fun getSize(
                        paint: Paint,
                        text: CharSequence,
                        start: Int,
                        end: Int,
                        fm: Paint.FontMetricsInt?,
                    ): Int = spaceWidth

                    override fun draw(
                        canvas: Canvas,
                        text: CharSequence,
                        start: Int,
                        end: Int,
                        x: Float,
                        top: Int,
                        y: Int,
                        bottom: Int,
                        paint: Paint,
                    ) { /* leave blank space */ }
                },
                date.length,
                date.length + space.length,
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            currentTimeTextView.text = spannable
        }
    }

    private fun updateViewVisibility(
        showRefreshing: Boolean,
        showCountdown: Boolean,
        showRefreshingIn: Boolean,
        showNoInternet: Boolean,
    ) {
        refreshingTextView.visibility = if (showRefreshing) View.VISIBLE else View.GONE
        countdownTextView.visibility = if (showCountdown) View.VISIBLE else View.GONE
        refreshingInTextView.visibility = if (showRefreshingIn) View.VISIBLE else View.GONE
        nointernetTextView.visibility = if (showNoInternet) View.VISIBLE else View.GONE
        Log.d(
            "DEBUG_VIEW",
            "🖥️ updateViewVisibility() CALLED -> showRefreshing=$showRefreshing, showCountdown=$showCountdown"
        )
    }

    private fun setupHandlersAndStartCountdown() {
        if (::refreshHandler.isInitialized) {
            refreshHandler.removeCallbacksAndMessages(null)
        }
        if (!::refreshHandler.isInitialized) {
            refreshHandler = Handler(Looper.getMainLooper())
        }
        refreshRunnable = object : Runnable {
            override fun run() {
                if (isRefreshing) {
                    Log.d("DEBUG_REFRESH", "⚠️ Already refreshing. Skipping scheduled refresh.")
                    refreshHandler.postDelayed(this, refreshInterval)
                    return
                }
                if (!isCountdownRunning) {
                    Log.d("DEBUG_REFRESH", "⏳ Countdown reached zero - Scheduling refresh.")
                    triggerRefreshImageAnimation()
                } else {
                    Log.d("DEBUG_REFRESH", "⚠️ Skipping refresh because countdown is handling it.")
                }
                refreshHandler.postDelayed(this, refreshInterval)
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(webView: WebView) {
        webView.settings.textZoom = 80
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.javaScriptCanOpenWindowsAutomatically = true
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest,
            ): Boolean {
                view.loadUrl(request.url.toString())
                return true
            }
        }
    }

    private fun openCustomBottomSheet() {
        bottomSheetFragment = CustomBottomSheetFooter().apply {}
        bottomSheetFragment?.show(supportFragmentManager, "CustomBottomSheetFooter")
    }


    private fun startCountdownAndRefreshCycle() {
        if (isCountdownRunning || isRefreshing) {
            Log.d(
                "DEBUG_COUNTDOWN",
                "⚠️ Countdown already running or refreshing - Skipping restart"
            )
            return
        }
        isCountdownRunning = true
        countdownTime = refreshInterval

        countdownHandler = Handler(Looper.getMainLooper())
        countdownHandler.removeCallbacksAndMessages(null)

        countdownRunnable = Runnable {
            lifecycleScope.launch {
                val isConnected = networkConnectivityHelper.isInternetAvailableFast()
                if (!isConnected) {
                    handleNoInternetState()
                    isCountdownRunning = false
                    return@launch
                }
                if (isRefreshing) {
                    Log.d("DEBUG_COUNTDOWN", "⏳ Refresh in progress - Skipping countdown trigger")
                    isCountdownRunning = false
                    return@launch
                }
                if (countdownTime <= 0) {
                    countdownTime = 0
                    updateCountdownUI()
                    isCountdownRunning = false
                    if (!isRefreshing) {
                        Log.d("DEBUG_COUNTDOWN", "⏳ Countdown reached zero - Triggering refresh")
                        triggerRefreshImageAnimation()
                    } else {
                        Log.d(
                            "DEBUG_COUNTDOWN",
                            "⚠️ Countdown reached zero but already refreshing - Skipping refresh"
                        )
                    }
                } else {
                    countdownTime -= 1000
                    updateCountdownUI()
                    countdownHandler.postDelayed(countdownRunnable, 1000)
                }
            }
        }
        countdownHandler.post(countdownRunnable)
    }

    private fun updateCountdownUI() {
        val minutes = countdownTime / 60000
        val seconds = (countdownTime % 60000) / 1000

        countdownTextView.text =
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

        val layerDrawable = countdownTextView.background as? LayerDrawable ?: return
        val clipDrawable = layerDrawable.findDrawableByLayerId(R.id.fill) as? ClipDrawable ?: return

        val targetLevel =
            ((countdownTime.toFloat() / refreshInterval) * MAX_DRAWABLE_LEVEL).toInt()
        ObjectAnimator.ofInt(clipDrawable, "level", clipDrawable.level, targetLevel).apply {
            duration = 1000
            interpolator = LinearInterpolator()
            start()
        }

        val fillColor = when {
            minutes == 0L && seconds <= 10 -> {
                ContextCompat.getColor(this, R.color.red)
            }
            minutes == 0L && seconds <= 15 -> {
                val red = (255 * (1 - (seconds / 15f))).toInt()
                val green = (255 * (seconds / 15f)).toInt()
                Color.rgb(red, green, 0)
            }
            else -> {
                ContextCompat.getColor(this, R.color.white)
            }
        }
        clipDrawable.setTint(fillColor)

        if (minutes == 0L && seconds <= PULSE_THRESHOLD) {
            startFadeScaleAnimation()
            countdownTextView.setTextColor(ContextCompat.getColor(this, R.color.material_yellow))
            Handler(Looper.getMainLooper()).postDelayed({
                if (seconds > 0) {
                    resetCountdownTextColor()
                }
            }, PULSE_DURATION)
        } else {
            stopFadeScaleAnimation()
            resetCountdownTextColor()
        }
    }

    private fun startFadeScaleAnimation() {
        val fadeScaleAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_scale)
        countdownTextView.startAnimation(fadeScaleAnimation)
    }

    private fun stopFadeScaleAnimation() {
        countdownTextView.clearAnimation()
    }

    private fun resetCountdownTextColor() {
        countdownTextView.setTextColor(ContextCompat.getColor(this, R.color.black))
    }

    fun triggerRefreshImageAnimation(imageUrl: String? = null) {
        val currentTime = System.currentTimeMillis()
        if (isRefreshing) {
            Log.d("DEBUG_REFRESH", "⚠️ Refresh already in progress - Skipping")
            return
        }
        if (currentTime - lastRefreshTime < minRefreshInterval) {
            Log.d("DEBUG_REFRESH", "⚠️ Refresh called too soon - Skipping")
            return
        }
        isRefreshing = true
        lastRefreshTime = currentTime

        if (::countdownHandler.isInitialized) {
            countdownHandler.removeCallbacks(countdownRunnable)
            isCountdownRunning = false
            Log.d("DEBUG_REFRESH", "⏳ Countdown stopped while refreshing")
        }

        loadNetworkImage(imageUrl)

        countdownTextView.translationX = 0f
        ObjectAnimator.ofFloat(
            countdownTextView,
            "translationX",
            0f,
            -countdownTextView.width.toFloat()
        ).apply {
            duration = 400
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            updateViewVisibility(
                showRefreshing = true,
                showCountdown = false,
                showRefreshingIn = false,
                showNoInternet = false
            )
            refreshingTextView.translationX = -refreshingTextView.width.toFloat()
            ObjectAnimator.ofFloat(
                refreshingTextView,
                "translationX",
                -refreshingTextView.width.toFloat(),
                0f
            ).apply {
                duration = 400
                interpolator = AccelerateDecelerateInterpolator()
                start()
            }

            Handler(Looper.getMainLooper()).postDelayed({
                ObjectAnimator.ofFloat(
                    refreshingTextView,
                    "translationX",
                    0f,
                    -refreshingTextView.width.toFloat()
                ).apply {
                    duration = 400
                    interpolator = AccelerateDecelerateInterpolator()
                    start()
                }
                Handler(Looper.getMainLooper()).postDelayed({
                    countdownTextView.translationX =
                        -countdownTextView.width.toFloat()
                    updateViewVisibility(
                        showRefreshing = false,
                        showCountdown = true,
                        showRefreshingIn = true,
                        showNoInternet = false
                    )
                    ObjectAnimator.ofFloat(
                        countdownTextView,
                        "translationX",
                        -countdownTextView.width.toFloat(),
                        0f
                    ).apply {
                        duration = 400
                        interpolator = AccelerateDecelerateInterpolator()
                        start()
                    }
                    Handler(Looper.getMainLooper()).postDelayed({
                        isRefreshing = false
                        Log.d("DEBUG_REFRESH", "✅ Refresh finished")
                        if (!isCountdownRunning) {
                            Log.d("DEBUG_REFRESH", "⏳ Restarting countdown after refresh")
                            startCountdownAndRefreshCycle()
                        }
                    }, 700)
                }, 700)
            }, 700)
        }, 700)
    }

    @SuppressLint("SetTextI18n")
    private fun loadNetworkImage(imageUrl: String? = null) {
        if (isDestroyed || isFinishing) {
            Log.d("DEBUG_REFRESH", "⚠️ Activity finishing - skip loadNetworkImage()")
            return
        }

        lifecycleScope.launch {
            if (!networkConnectivityHelper.isInternetAvailableFast()) {
                handleNoInternetState()
                return@launch
            }
            errorImageView.hideBadge()

            // keep your status labels in sync
            prepareForNetworkOperation(showCountdown = true, showRefreshingIn = true, showNoInternet = false)
            isRefreshing = true

            val finalImageUrl = imageUrl
                ?: "https://www.jacksonholeairport.com/wp-content/uploads/webcams/parking-curb.jpg?v=${System.currentTimeMillis()}"
            Log.d("SplashActivity", "Attempting to load image: $finalImageUrl")

            if (!isDestroyed && !isFinishing) {

                Glide.with(this@SplashActivity)
                    .asDrawable()
                    .load(finalImageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .dontTransform()                 // let TouchImageView control scaling
                    .placeholder(R.drawable.placeholder)    // ✅ logo only as placeholder
                    .error(R.drawable.placeholder)        // ✅ fallback to logo on failure
                    .into(object : CustomTarget<Drawable>() {

                        override fun onResourceReady(
                            resource: Drawable,
                            transition: com.bumptech.glide.request.transition.Transition<in Drawable>?,
                        ) {
                            // onResourceReady()
                            if (errorImageView.isVisible) {
                                errorImageView.hideBadge()
                            }


                            val firstTime = photoView.drawable == null || photoView.visibility != View.VISIBLE
                            val prevMatrix = photoView.imageMatrix

                            if (firstTime) {
                                // First content: set and softly fade in (no logo fade-out)
                                photoView.setImageDrawable(resource)
                                photoView.visibility = View.VISIBLE
                                photoView.alpha = 0f
                                photoView.imageMatrix = prevMatrix
                                photoView.animate().alpha(1f).setDuration(450).setInterpolator(DecelerateInterpolator()).start()
                            } else {
                                // Subsequent refreshes: quick crossfade
                                photoView.animate()
                                    .alpha(0f)
                                    .setDuration(320)
                                    .setInterpolator(AccelerateDecelerateInterpolator())
                                    .withEndAction {
                                        photoView.setImageDrawable(resource)
                                        photoView.imageMatrix = prevMatrix
                                        photoView.animate()
                                            .alpha(1f)
                                            .setDuration(420)
                                            .setInterpolator(DecelerateInterpolator())
                                            .start()
                                    }
                                    .start()
                            }
                        }

                        override fun onLoadFailed(errorDrawable: Drawable?) {
                            super.onLoadFailed(errorDrawable)
                            Log.e("GLIDE", "❌ Failed to load image: $finalImageUrl")

                            // Hide the photo view
                            if (photoView.isVisible) {
                                photoView.animate()
                                    .alpha(0f)
                                    .setDuration(250)
                                    .setInterpolator(AccelerateDecelerateInterpolator())
                                    .withEndAction {
                                        photoView.visibility = View.GONE
                                        photoView.alpha = 1f
                                    }
                                    .start()
                            }

                            // Show the badge (unified helper)
                            errorImageView.showBadge(R.drawable.ic_no_internet_oval)

                            // Notify the user
                            val root: View = findViewById(android.R.id.content)
                            Snackbar.make(root, "Failed to load image.", Snackbar.LENGTH_LONG)
                                .setBackgroundTint(ContextCompat.getColor(this@SplashActivity, R.color.red))
                                .setTextColor(Color.WHITE)
                                .show()
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                            photoView.setImageDrawable(placeholder)
                        }
                    })
            }

            // Keep your existing countdown behavior
            if (::countdownHandler.isInitialized) {
                countdownHandler.postDelayed(countdownRunnable, 700)
            }
            // NOTE: isRefreshing is reset by your triggerRefreshImageAnimation() sequence.
        }
    }
    private fun ImageView.hideBadgeImmediate() {
        animate().cancel()
        clearAnimation()
        visibility = View.GONE
        alpha = 1f
        translationZ = 0f
        setImageDrawable(null) // <- prevent stale draw
    }


    private fun handleNoInternetState() {
        if (!isNoInternetBottomSheetShown) {
            prepareForNetworkOperation(showNoInternet = true)
            if (::countdownHandler.isInitialized) countdownHandler.removeCallbacks(countdownRunnable)
            isCountdownRunning = false
            isNoInternetBottomSheetShown = true
        }

        errorImageView.showBadge(R.drawable.ic_no_internet_oval)

        photoView.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                photoView.visibility = View.GONE
                photoView.alpha = 1f
                if (!isFullScreen) {
                    fullScreenHelper.resetToDefaultState(cardView, photoView, errorImageView)
                }
            }
            .start()
    }
    // ImageView badge helpers
    private fun ImageView.showBadge(@DrawableRes iconRes: Int? = null) {
        iconRes?.let { setImageResource(it) }
        bringToFront()
        translationZ = 1f
        (parent as? View)?.invalidate()
        if (visibility != View.VISIBLE || alpha != 1f) {
            if (visibility != View.VISIBLE) {
                alpha = 0f
                visibility = View.VISIBLE
            }
            animate().alpha(1f).setDuration(200L).start()
        }
    }

    private fun ImageView.hideBadge() {
        if (isVisible && alpha != 0f) {
            animate().alpha(0f).setDuration(180L).withEndAction {
                visibility = View.GONE
                alpha = 1f
                translationZ = 0f
                setImageDrawable(null) // <- prevent stale draw
            }.start()
        }
    }


    private fun prepareForNetworkOperation(
        showCountdown: Boolean = false,
        showRefreshingIn: Boolean = false,
        showNoInternet: Boolean = false,
    ) {
        isRefreshing = false
        updateViewVisibility(
            showRefreshing = false,
            showCountdown = showCountdown,
            showRefreshingIn = showRefreshingIn,
            showNoInternet = showNoInternet
        )
    }

    private fun setStatusBarColor() {
        val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val isDarkTheme = nightModeFlags == Configuration.UI_MODE_NIGHT_YES
        val statusBarColorResId = if (isDarkTheme) {
            R.color.darkSplashStatusBarColor
        } else {
            R.color.lightSplashStatusBarColor
        }
        window.statusBarColor = ContextCompat.getColor(this, statusBarColorResId)
    }

    private fun initializeFirebase() {
        FirebaseApp.initializeApp(this)
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        val bundle =
            Bundle().apply { putString(FirebaseAnalytics.Param.METHOD, "Splash Screen Loaded") }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, bundle)
        firebaseAnalytics.setAnalyticsCollectionEnabled(true)
    }
    private val networkStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("DEBUG_NETWORK", "🔄 Network state changed - checking...")
            lifecycleScope.launch {
                val isConnected = networkConnectivityHelper.isInternetAvailableFast()
                Log.d("DEBUG_NETWORK", "🌐 Connected: $isConnected")
                if (isConnected) {
                    // Immediately clear offline UI
                    errorImageView.hideBadgeImmediate()
                    updateViewVisibility(
                        showRefreshing = false,
                        showCountdown = true,     // or false if you don’t want to show it yet
                        showRefreshingIn = true,  // optional
                        showNoInternet = false
                    )
                    isNoInternetBottomSheetShown = false

                    if (!isRefreshing) triggerRefreshImageAnimation()
                    if (!isCountdownRunning) restartCountdownTimer()
                } else {
                    Log.d("DEBUG_NETWORK", "🚫 No internet - Stopping countdown")
                    handleNoInternetState()
                }
            }
        }
    }




    private fun restartCountdownTimer() {
        if (::countdownHandler.isInitialized) {
            countdownHandler.removeCallbacksAndMessages(null)
        }
        isCountdownRunning = true
        countdownTime = refreshInterval

        countdownHandler = Handler(Looper.getMainLooper())
        countdownRunnable = Runnable {
            CoroutineScope(Dispatchers.Main).launch {
                val isConnected = networkConnectivityHelper.isInternetAvailableFast()
                if (!isConnected) {
                    handleNoInternetState()
                    return@launch
                }
                if (isRefreshing) {
                    Log.d("DEBUG_COUNTDOWN", "⏸️ Countdown paused while refreshing")
                    return@launch
                }
                if (countdownTime <= 0) {
                    countdownTime = 0
                    updateCountdownUI()
                    isCountdownRunning = false
                    if (!isRefreshing) {
                        Log.d("DEBUG_COUNTDOWN", "⏳ Countdown reached zero - Triggering refresh")
                        triggerRefreshImageAnimation()
                    } else {
                        Log.d(
                            "DEBUG_COUNTDOWN",
                            "⚠️ Countdown reached zero but already refreshing - Skipping refresh"
                        )
                    }
                } else {
                    countdownTime -= 1000
                    updateCountdownUI()
                    countdownHandler.postDelayed(countdownRunnable, 1000)
                }
            }
        }
        countdownHandler.post(countdownRunnable)
    }

    private val onBackPressedCallback: OnBackPressedCallback =
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showConfirmationDialog()
            }
        }

    private fun showConfirmationDialog() {
        val dialogView = layoutInflater.inflate(R.layout.custom_dialog_layout_close_app, null)
        val confirmDialog = AlertDialog.Builder(this).setView(dialogView)
            .setCancelable(false).create()
        confirmDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val cancelButton = dialogView.findViewById<MaterialButton>(R.id.dialog_cancel)
        val confirmButton = dialogView.findViewById<MaterialButton>(R.id.dialog_confirm)
        cancelButton.setOnClickListener {
            confirmDialog.dismiss()
        }
        confirmButton.setOnClickListener {
            onBackPressedCallback.isEnabled = false
            finishAffinity()
            finishAndRemoveTask()
            confirmDialog.dismiss()
        }
        confirmDialog.show()
    }

    private fun openActivityWithCard(cardId: String) {
        val intent = when (cardId) {
            "card5" -> Intent(this, QRCodeActivity::class.java)
            "card6" -> Intent(this, SettingsActivity::class.java)
            "card7" -> Intent(this, AllContactsActivity::class.java)
            "card8" -> Intent(this, AllNotesActivity::class.java)
            "card9" -> Intent(this, ProfileDetailsActivity::class.java)
            else -> null
        }
        intent?.let {
            startActivity(it)
            overridePendingTransition(R.anim.zoom_in, 0)
        } ?: Log.e("SplashActivity", "Unknown cardId: $cardId")
    }

    private fun clearResources() {
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(imageLoadReceiver)
        } catch (_: Exception) {
            // already unregistered or never registered
        }

        if (::orientationListener.isInitialized) {
            orientationListener.disable()
        }
    }



    private fun setupHaptics(motion: MotionLayout) {
        if (hapticsAttached) return
        hapticsAttached = true

        motion.setTransitionListener(object : MotionLayout.TransitionListener {
            override fun onTransitionStarted(layout: MotionLayout, startId: Int, endId: Int) {
                didTickOvershoot = false
                didConfirmExpand = false
                didConfirmCollapse = false

                // If heading to EXPANDED, drop elevation immediately (avoids overlay flicker)
                if (startId == R.id.collapsed && endId == R.id.expanded) {
                    ViewCompat.setElevation(composeHost, 0f)
                    composeHost.translationZ = 0f
                }

                if (!layout.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)) {
                    vibrateSoft(layout)
                }
            }

            override fun onTransitionChange(
                layout: MotionLayout, startId: Int, endId: Int, progress: Float
            ) {
                if (startId == R.id.collapsed && endId == R.id.expanded &&
                    !didTickOvershoot && progress >= 0.92f
                ) {
                    didTickOvershoot = true
                    vibrateSoft(layout)
                }
            }

            override fun onTransitionCompleted(layout: MotionLayout, currentId: Int) {
                if (currentId == R.id.expanded && !didConfirmExpand) {
                    didConfirmExpand = true
                    if (!layout.performHapticFeedback(HapticFeedbackConstants.CONFIRM)) {
                        vibrateSoft(layout)
                    }
                    // Ensure 0 when expanded
                    ViewCompat.setElevation(composeHost, 0f)
                    composeHost.translationZ = 0f

                } else if (currentId == R.id.collapsed && !didConfirmCollapse) {
                    didConfirmCollapse = true
                    layout.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    // Restore your default when collapsed
                    ViewCompat.setElevation(composeHost, collapsedElevationPx())
                    composeHost.translationZ = 0f
                }
            }

            override fun onTransitionTrigger(
                layout: MotionLayout, triggerId: Int, positive: Boolean, progress: Float
            ) { /* no-op */ }
        })
    }

    private fun vibrateSoft(view: View) {
        val vib = view.context.getSystemService(VIBRATOR_SERVICE) as Vibrator
        vib.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
    }



}
