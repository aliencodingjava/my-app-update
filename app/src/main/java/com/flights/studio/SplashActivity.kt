package com.flights.studio

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ReplacementSpan
import android.util.Log
import android.view.MotionEvent
import android.view.OrientationEventListener
import android.view.Surface
import android.view.View
import android.view.animation.AnimationUtils
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.github.chrisbanes.photoview.PhotoView
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import java.util.Date
import java.util.Locale

@Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

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
    private var isRefreshing: Boolean = false // Default is not refreshing
    private var countdownTime: Long = 0 // Change countdownTime to Long
    private lateinit var currentTimeTextView: TextView
    private lateinit var timeHandler: Handler
    private lateinit var timeRunnable: Runnable
    private lateinit var batteryInfoTextView: TextView
    private lateinit var batteryManagerHelper: BatteryManagerHelper
    private var isCountdownRunning: Boolean = false
    private var isNoInternetBottomSheetShown = false // Prevent duplicate calls
    private var lastRefreshTime: Long = 0
    private val minRefreshInterval = 5000L // 5 seconds cooldown

    private lateinit var fullScreenHelper: FullScreenHelper
    private lateinit var cardView: MaterialCardView
    private lateinit var photoView: PhotoView
    private var isFullScreen = false


    private val imageLoadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // This is where we load the image when FAB1 is clicked
            loadNetworkImage()
        }
    }

    private fun openBottomSheetWithCard(cardId: String) {
        val bottomSheetFragment = CardBottomSheetFragment.newInstance(cardId)
        bottomSheetFragment.show(supportFragmentManager, "CardBottomSheetFragment")
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        // Initialize the FullScreenHelper here, at the class level
        fullScreenHelper = FullScreenHelper()

        // Find views
        photoView = findViewById(R.id.photoView)  // Make sure the ID matches in your layout XML
        cardView = findViewById(R.id.cardView2)    // Same for the cardView ID


        // ✅ Find the FrameLayout that contains `widget_timer`
        val widgetTimerContainer = findViewById<View>(R.id.widget_timer_container)
        widgetTimerContainer.isClickable = false // ✅ Prevent entire widget from being clickable
        widgetTimerContainer.isFocusable = false

        // ✅ Find `open_close_full_screen` inside `widget_timer`
        val includedLayout = findViewById<View>(R.id.widget_timer)
        val fullScreenButtonTextView =
            includedLayout.findViewById<AppCompatTextView>(R.id.open_close_full_screen)

        // ✅ Ensure ONLY `open_close_full_screen` is clickable
        fullScreenButtonTextView.setOnClickListener {
            isFullScreen = fullScreenHelper.toggleFullScreen(cardView, photoView, isFullScreen)
        }


        // ✅ Re-register network state receiver
        try {
            unregisterReceiver(networkStateReceiver) // Avoid duplicate registration
        } catch (_: IllegalArgumentException) {
            Log.w("NetworkState", "Receiver not registered yet, skipping unregister.")
        }
        registerReceiver(networkStateReceiver, IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"))




        if (intent.extras?.getBoolean("fallback", false) == true) {
            Log.e("SplashActivity", "Unexpected fallback to SplashActivity")
            // Redirect to a more appropriate activity if needed
        }

        // Initialize views
        countdownTextView = findViewById(R.id.countdownTextView)
        refreshingTextView = findViewById(R.id.refreshingTextView)
        refreshingInTextView = findViewById(R.id.refreshingInTextView)
        nointernetTextView = findViewById(R.id.nointernetTextView)
        batteryInfoTextView = findViewById(R.id.battery_info)
        photoView = findViewById(R.id.photoView)
        currentTimeTextView = findViewById(R.id.currentTimeTextView)
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        batteryManagerHelper = BatteryManagerHelper(this).apply {
            onBatteryStatusUpdated = { batteryPct, isCharging ->
                updateBatteryStatusUI(batteryPct, isCharging)
            }
        }

        val countdownBackground = countdownTextView.background
        if (countdownBackground is LayerDrawable) {
            val clipDrawable = countdownBackground.findDrawableByLayerId(R.id.fill)
            if (clipDrawable is ClipDrawable) {
                countdownDrawable = clipDrawable
                countdownDrawable.level = 10000 // Start fully filled
            }
        }


        // Initialize click listeners for cards
        findViewById<View>(R.id.card1).setOnClickListener { openBottomSheetWithCard("card1") }
        findViewById<View>(R.id.card2).setOnClickListener { openBottomSheetWithCard("card2") }
        findViewById<View>(R.id.card3).setOnClickListener { openBottomSheetWithCard("card3") }
        findViewById<View>(R.id.card4).setOnClickListener { openBottomSheetWithCard("card4") }



        setupHandlersAndStartCountdown()
        startCountdownAndRefreshCycle()
        initializeFirebase()
        setStatusBarColor()
        startUpdatingCurrentTime()


        // Show the bottom sheet after a short delay (e.g., 1 second)
        Handler(Looper.getMainLooper()).postDelayed({
            openCustomBottomSheet()
        }, 100) // 200 milliseconds = 0.2 second


        networkConnectivityHelper = NetworkConnectivityHelper(this)

        // Register the receiver to listen for the broadcast
        val filter = IntentFilter("LOAD_IMAGE")
        LocalBroadcastManager.getInstance(this).registerReceiver(imageLoadReceiver, filter)


        orientationListener = object : OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {
                updateGuidelineBasedOnOrientation()
            }
        }

        if (orientationListener.canDetectOrientation()) {
            orientationListener.enable()
        }
        val bottomAppBar = findViewById<BottomAppBar>(R.id.bottomAppBar)

        val imageMap = mapOf(
            R.id.nav_curb to "https://www.jacksonholeairport.com/wp-content/uploads/webcams/parking-curb.jpg",
            R.id.nav_north to "https://www.jacksonholeairport.com/wp-content/uploads/webcams/parking-north.jpg",
            R.id.nav_south to "https://www.jacksonholeairport.com/wp-content/uploads/webcams/parking-south.jpg"
        )

// ✅ Use `setOnMenuItemClickListener` instead of `setOnItemSelectedListener`
        bottomAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_curb, R.id.nav_north, R.id.nav_south -> {
                    val baseUrl = imageMap[menuItem.itemId]
                    val imageUrl =
                        baseUrl?.let { "$it?v=${System.currentTimeMillis()}" } // Add timestamp
                    if (imageUrl != null) {
                        triggerRefreshImageAnimation(imageUrl)
                    }
                    true
                }

                else -> false
            }
        }

        val fabAction = findViewById<FloatingActionButton>(R.id.fab_action)

        fabAction.setOnClickListener {
            Log.d("SplashActivity", "FAB clicked - Opening Bottom Sheet")
            openCustomBottomSheet()
        }

    }



    override fun onTouchEvent(event: MotionEvent): Boolean {
        Log.d("TouchTest", "Touch event: ${event.actionMasked}, at: (${event.x}, ${event.y})")
        return super.onTouchEvent(event)
    }





    override fun onResume() {
        super.onResume()
        fullScreenHelper.resetToDefaultState(cardView, photoView)  // Reset height to 50%

        // Start monitoring the battery
        batteryManagerHelper.startMonitoring()
        // Register the network state receiver in onResume
        val intentFilter = IntentFilter("android.net.conn.CONNECTIVITY_CHANGE")
        try {
            registerReceiver(networkStateReceiver, intentFilter)
        } catch (e: IllegalArgumentException) {
            Log.w("NetworkState", "Receiver already registered or error occurred: ${e.message}")
        }

        // Check for internet connectivity and update the UI accordingly
        if (networkConnectivityHelper.isNetworkAvailable()) {
            // Internet is available, hide the "No Internet" text and restart refresh
            updateViewVisibility(showRefreshing = false, showCountdown = true, showRefreshingIn = true, showNoInternet = false)

            // Restart the countdown if not running
            if (!isCountdownRunning) {
                startCountdownAndRefreshCycle()
            }
            triggerRefreshImageAnimation()

        } else {
            // Internet is not available, show the "No Internet" text
            handleNoInternetState()
        }
    }

    override fun onPause() {
        super.onPause()
        batteryManagerHelper.stopMonitoring()
        // Unregister the network state receiver in onPause
        try {
            unregisterReceiver(networkStateReceiver)
        } catch (e: IllegalArgumentException) {
            Log.w("NetworkState", "Receiver was not registered: ${e.message}")
        }
    }


    private fun updateBatteryStatusUI(batteryPct: Int, isCharging: Boolean) {
        // Update the TextView with battery percentage
        val batteryText = if (batteryPct != -1) "$batteryPct" else "N/A"
        batteryInfoTextView.text = batteryText
        val batteryIcon = ContextCompat.getDrawable(this, R.drawable.battery_icon) as LayerDrawable
        val fillLayer = batteryIcon.findDrawableByLayerId(R.id.fill) as ClipDrawable
        val fillLevel = (batteryPct * 10000 / 100) // Scale the percentage correctly
        fillLayer.level = fillLevel
        val fillColor = if (isCharging) {
            ContextCompat.getColor(this, R.color.material_yellow) // Yellow when charging
        } else {
            ContextCompat.getColor(this, R.color.white) // White when not charging
        }
        fillLayer.setTint(fillColor)
        batteryInfoTextView.setCompoundDrawablesWithIntrinsicBounds(batteryIcon, null, null, null)
    }





    private fun startUpdatingCurrentTime() {
        timeHandler = Handler(Looper.getMainLooper())
        timeRunnable = object : Runnable {
            override fun run() {
                val currentTime = System.currentTimeMillis()
                val dateFormat = java.text.SimpleDateFormat("MMM/dd/yyyy", Locale.getDefault())
                val timeFormat = java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                val date = dateFormat.format(Date(currentTime))
                val time = timeFormat.format(Date(currentTime))

                // Create a SpannableString to include dynamic spacing
                val space = " " // Placeholder for spacing
                val spannable = SpannableString("$date$space$time")
                val spaceWidth = 20 // Adjust this for spacing in pixels (e.g., 50px)

                spannable.setSpan(
                    object : ReplacementSpan() {
                        override fun getSize(
                            paint: Paint,
                            text: CharSequence,
                            start: Int,
                            end: Int,
                            fm: Paint.FontMetricsInt?
                        ): Int {
                            return spaceWidth
                        }

                        override fun draw(
                            canvas: Canvas,
                            text: CharSequence,
                            start: Int,
                            end: Int,
                            x: Float,
                            top: Int,
                            y: Int,
                            bottom: Int,
                            paint: Paint
                        ) {
                            // No drawing; leave blank space
                        }
                    },
                    date.length, // Start of the space
                    date.length + space.length, // End of the space
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                // Update the TextView with the formatted date and time
                currentTimeTextView.text = spannable

                // Repeat every second
                timeHandler.postDelayed(this, 1000)
            }
        }
        timeHandler.post(timeRunnable)
    }


    private fun updateViewVisibility(
        showRefreshing: Boolean,
        showCountdown: Boolean,
        showRefreshingIn: Boolean,
        showNoInternet: Boolean
    ) {
        refreshingTextView.visibility = if (showRefreshing) View.VISIBLE else View.GONE
        countdownTextView.visibility = if (showCountdown) View.VISIBLE else View.GONE
        refreshingInTextView.visibility = if (showRefreshingIn) View.VISIBLE else View.GONE
        nointernetTextView.visibility = if (showNoInternet) View.VISIBLE else View.GONE
    }



    private fun setupHandlersAndStartCountdown() {
        // Avoid duplicate handlers
        if (::refreshHandler.isInitialized) {
            refreshHandler.removeCallbacksAndMessages(null)
        }

        // Initialize refreshHandler if not already initialized
        if (!::refreshHandler.isInitialized) {
            refreshHandler = Handler(Looper.getMainLooper())
        }

        refreshRunnable = object : Runnable {
            override fun run() {
                if (isRefreshing) {
                    Log.d("Refresh", "Already refreshing. Skipping this cycle.")
                    refreshHandler.postDelayed(this, refreshInterval)
                    return
                }

                // Trigger image refresh and animation
                triggerRefreshImageAnimation()

                // Schedule the next refresh
                refreshHandler.postDelayed(this, refreshInterval)
            }
        }
    }




    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(webView: WebView) {
        // Enable JavaScript
        webView.settings.textZoom =
            80 // 100% is the default. Use a value greater than 100 to increase.

        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true // Enable DOM storage (for modern web apps)
        webSettings.javaScriptCanOpenWindowsAutomatically = true // Allow JavaScript to open windows

        // Set a custom WebViewClient to handle links inside the WebView
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                view.loadUrl(request.url.toString()) // Open URLs inside the WebView
                return true
            }
        }

    }

    private fun openCustomBottomSheet() {
        bottomSheetFragment = CustomBottomSheetFooter().apply {}
        bottomSheetFragment?.show(supportFragmentManager, "CustomBottomSheetFooter")

    }


    private fun updateGuidelineBasedOnOrientation() {
        val newRotation = this.display?.rotation ?: return
        val materialCardView = findViewById<MaterialCardView>(R.id.cardView2)
        val layoutParams = materialCardView.layoutParams as ConstraintLayout.LayoutParams

        when (newRotation) {
            Surface.ROTATION_90, Surface.ROTATION_270 -> {
                // Landscape mode: Make card full screen
                layoutParams.width = ConstraintLayout.LayoutParams.MATCH_PARENT
                layoutParams.height = ConstraintLayout.LayoutParams.MATCH_PARENT
                findViewById<View>(R.id.bottomAppBar).visibility = View.GONE
            }
            else -> {
                // Portrait mode: Restore original height (50%)
                layoutParams.width = ConstraintLayout.LayoutParams.MATCH_PARENT
                layoutParams.height = ConstraintLayout.LayoutParams.WRAP_CONTENT
                layoutParams.matchConstraintPercentHeight = 0.50f // 50% of screen height
                findViewById<View>(R.id.bottomAppBar).visibility = View.VISIBLE
            }
        }

        materialCardView.layoutParams = layoutParams
        materialCardView.requestLayout() // Force the layout update
    }

    // Removed the unused function updateGuidelinePercentage since it's not called in the code.

    override fun onDestroy() {
        super.onDestroy()
        clearResources()
        if (::refreshHandler.isInitialized) {
            refreshHandler.removeCallbacksAndMessages(null) // 🔥 Stop all refresh callbacks
        }

        if (::countdownHandler.isInitialized) {
            countdownHandler.removeCallbacksAndMessages(null) // 🔥 Stop countdown callbacks
        }
        if (::timeHandler.isInitialized) {
            timeHandler.removeCallbacksAndMessages(null) // ✅ Stop time updates
        }
        batteryManagerHelper.stopMonitoring()
        isCountdownRunning = false
    }



    private fun clearResources() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(imageLoadReceiver)
        orientationListener.disable()
    }

    private fun startCountdownAndRefreshCycle() {
        if (isCountdownRunning || isRefreshing) {
            Log.d("DEBUG_COUNTDOWN", "⚠️ Countdown already running or refreshing - Skipping restart")
            return // Prevent duplicate countdowns
        }

        isCountdownRunning = true
        countdownTime = refreshInterval

        countdownHandler = Handler(Looper.getMainLooper())
        countdownHandler.removeCallbacksAndMessages(null)

        countdownRunnable = object : Runnable {
            override fun run() {
                // Check for network connectivity at each step
                if (!networkConnectivityHelper.isNetworkAvailable()) {
                    handleNoInternetState()
                    return // Stop countdown when there's no internet
                }

                if (isRefreshing) {
                    Log.d("DEBUG_COUNTDOWN", "⏸️ Countdown paused while refreshing")
                    return // Pause countdown while refreshing
                }

                if (countdownTime <= 0) {
                    countdownTime = 0
                    updateCountdownUI()
                    isCountdownRunning = false // Mark countdown as stopped

                    if (!isRefreshing) { // Ensure we refresh only if it's not already in progress
                        Log.d("DEBUG_COUNTDOWN", "⏳ Countdown reached zero - Triggering refresh")
                        triggerRefreshImageAnimation()
                    } else {
                        Log.d("DEBUG_COUNTDOWN", "⚠️ Countdown reached zero but already refreshing - Skipping refresh")
                    }
                } else {
                    countdownTime -= 1000
                    updateCountdownUI()
                    countdownHandler.postDelayed(this, 1000)
                }
            }
        }

        countdownHandler.post(countdownRunnable)
    }


    private fun updateCountdownUI() {
        // Calculate minutes and seconds
        val minutes = countdownTime / 60000
        val seconds = (countdownTime % 60000) / 1000

        // Update countdown text
        countdownTextView.text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

        // Access the ClipDrawable using LayerDrawable
        val layerDrawable = countdownTextView.background as LayerDrawable
        val clipDrawable = layerDrawable.findDrawableByLayerId(R.id.fill) as ClipDrawable

        // Update the drawable level for progress visualization (clip level)
        clipDrawable.level = ((countdownTime.toFloat() / refreshInterval) * MAX_DRAWABLE_LEVEL).toInt()

        // Gradually transition from white to red when less than 15 seconds
        val fillColor = when {
            minutes == 0L && seconds <= 10 -> {
                // Set to red when countdown reaches 10 seconds or less
                ContextCompat.getColor(this, R.color.red)
            }
            minutes == 0L && seconds <= 15 -> {
                // Gradually transition from white to red when time is between 15 and 10 seconds
                val red = (255 * (1 - (seconds / 15f))).toInt()  // Gradual change from white to red
                val green = (255 * (seconds / 15f)).toInt()     // Gradual change from green to red
                val blue = 0
                Color.rgb(red, green, blue)
            }
            else -> {
                // Default white when time is greater than 15 seconds
                ContextCompat.getColor(this, R.color.white)
            }
        }


        // Update the ClipDrawable color dynamically
        clipDrawable.setTint(fillColor)

        // Handle the animation for the countdown text color and pulsing effect
        if (minutes == 0L && seconds <= PULSE_THRESHOLD) {
            startFadeScaleAnimation()
            countdownTextView.setTextColor(ContextCompat.getColor(this, R.color.green))

            // Reset color after animation duration
            Handler(Looper.getMainLooper()).postDelayed({
                if (seconds > 0) { // Ensure countdown is still running
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


    // Helper to reset the countdown text color
    private fun resetCountdownTextColor() {
        countdownTextView.setTextColor(ContextCompat.getColor(this, R.color.black))
    }

    fun triggerRefreshImageAnimation(imageUrl: String? = null) {
        val currentTime = System.currentTimeMillis()

        Log.d("DEBUG_REFRESH", "🔄 triggerRefreshImageAnimation() called - isRefreshing: $isRefreshing")
        if (currentTime - lastRefreshTime < minRefreshInterval) {
            Log.d("DEBUG_REFRESH", "⚠️ Refresh called too soon - Skipping")
            return
        }
        lastRefreshTime = currentTime

        if (!networkConnectivityHelper.isNetworkAvailable()) {
            Log.d("DEBUG_REFRESH", "🚫 No Internet - Skipping refresh")
            updateViewVisibility(showRefreshing = false, showCountdown = false, showRefreshingIn = false, showNoInternet = true)
            return
        }

        if (isRefreshing) {
            Log.d("DEBUG_REFRESH", "⚠️ Already refreshing - Skipping this call")
            return
        }

        isRefreshing = true
        Log.d("DEBUG_REFRESH", "✅ Refresh started")

        // Stop countdown while refreshing
        if (::countdownHandler.isInitialized) {
            countdownHandler.removeCallbacks(countdownRunnable)
            Log.d("DEBUG_REFRESH", "⏳ Countdown stopped while refreshing")
        }

        // 🟢 Step 1: Slide out Countdown Timer & Refreshing In (to the left)
        animateView(countdownTextView, R.anim.slide_out_left)
        animateView(refreshingInTextView, R.anim.slide_out_left)

        // Introduce a delay before loading the image and continuing animation
        Handler(Looper.getMainLooper()).postDelayed({
            loadNetworkImage(imageUrl)

            // 🟢 Step 2: Slide in "Refreshing" text (from the left) - Appears only once
            updateViewVisibility(showRefreshing = true, showCountdown = false, showRefreshingIn = false, showNoInternet = false)
            animateView(refreshingTextView, R.anim.slide_in_left) {

                Handler(Looper.getMainLooper()).postDelayed({
                    // 🟢 Step 3: Slide **out** "Refreshing" text (to the left)
                    animateView(refreshingTextView, R.anim.slide_out_left)

                    Handler(Looper.getMainLooper()).postDelayed({
                        // 🟢 Step 4: Slide in Countdown Timer & Refreshing In from **LEFT to RIGHT**
                        updateViewVisibility(showRefreshing = false, showCountdown = true, showRefreshingIn = true, showNoInternet = false)

                        animateView(countdownTextView, R.anim.slide_in_left)
                        animateView(refreshingInTextView, R.anim.slide_in_left)

                        Handler(Looper.getMainLooper()).postDelayed({
                            isRefreshing = false
                            Log.d("DEBUG_REFRESH", "✅ Refresh finished")

                            if (!isCountdownRunning) {
                                Log.d("DEBUG_REFRESH", "⏳ Restarting countdown after refresh")
                                startCountdownAndRefreshCycle()
                            }
                        }, 500) // Delay before completing refresh cycle
                    }, 500) // Delay before sliding Countdown Timer & Refreshing In back in
                }, 500) // Delay before sliding out refreshing text
            }
        }, 500) // Delay before loading image
    }

    /**
     * Helper function to animate a view with an optional end action
     */
    private fun animateView(view: View, animationRes: Int, onEndAction: (() -> Unit)? = null) {
        val animation = AnimationUtils.loadAnimation(this, animationRes)
        view.startAnimation(animation)
        animation.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
            override fun onAnimationStart(animation: android.view.animation.Animation?) {}
            override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                onEndAction?.invoke()
            }
            override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
        })
    }

    private fun loadNetworkImage(imageUrl: String? = null) {
        if (!networkConnectivityHelper.isNetworkAvailable()) {
            handleNoInternetState()
            return
        }

        // Prepare for image loading with visible countdown and hidden error states
        prepareForNetworkOperation(
            showCountdown = true,
            showRefreshingIn = true,
            showNoInternet = false
        )


        val finalImageUrl = imageUrl
            ?: "https://www.jacksonholeairport.com/wp-content/uploads/webcams/parking-curb.jpg?v=${System.currentTimeMillis()}"

        logDebug("Attempting to load image: $finalImageUrl")
        logError(NO_INTERNET_MESSAGE)

        Glide.with(this)
            .load(finalImageUrl)
            .placeholder(R.drawable.placeholder) // Display while loading
            .error(R.drawable.error_logo) // Display on failure
            .diskCacheStrategy(DiskCacheStrategy.NONE) // Avoid caching issues
            .skipMemoryCache(true) // Force reload every time
            .transition(DrawableTransitionOptions.withCrossFade(500))
            .into(photoView)
        // Start the countdown timer if it was stopped
        if (::countdownHandler.isInitialized) {
            countdownHandler.postDelayed(countdownRunnable, 1000) // Resume countdown
        }
    }

    private fun handleNoInternetState() {
        if (!isNoInternetBottomSheetShown) {
            logNoInternetError()
            prepareForNetworkOperation(
                showCountdown = false,
                showRefreshingIn = false,
                showNoInternet = true
            )
            countdownHandler.removeCallbacks(countdownRunnable)
            isNoInternetBottomSheetShown = true // Ensure UI changes only once per network disconnect
        }
    }


    private fun prepareForNetworkOperation(
        showCountdown: Boolean = false,
        showRefreshingIn: Boolean = false,
        showNoInternet: Boolean = false
    ) {
        isRefreshing = false // Hardcoded since it's always false
        updateViewVisibility(
            showRefreshing = false, // Hardcoded since it's always false
            showCountdown = showCountdown,
            showRefreshingIn = showRefreshingIn,
            showNoInternet = showNoInternet
        )
    }


    private fun logDebug(message: String) {
        Log.d("SplashActivity", message) // Hardcoded tag
    }

    private fun logError(message: String) {
        Log.e("SplashActivity", message) // Hardcoded tag
    }


    private fun logNoInternetError() {
        logError("No network connection, stopping countdown and showing fallback image.")
    }





    companion object {
        private const val MAX_DRAWABLE_LEVEL = 10000
        private const val PULSE_THRESHOLD = 5 // Start pulsing when 5 seconds remain
        private const val PULSE_DURATION = 400L // Duration of the pulse animation in ms
        private const val NO_INTERNET_MESSAGE = "No network connection, stopping countdown and showing fallback image."
    }



    private fun setStatusBarColor() {
        val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val isDarkTheme = nightModeFlags == Configuration.UI_MODE_NIGHT_YES

        val statusBarColorResId = if (isDarkTheme) {
            R.color.darkSplashStatusBarColor
        } else {
            R.color.lightSplashStatusBarColor
        }

        // ✅ Ensure window is updated
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
            val isConnected = networkConnectivityHelper.isNetworkAvailable()
            Log.d("DEBUG_NETWORK", "Network state changed - Connected: $isConnected")

            if (isConnected) {
                // Clear any "No Internet" UI elements
                updateViewVisibility(showRefreshing = false, showCountdown = true, showRefreshingIn = true, showNoInternet = false)

                // Ensure the countdown restarts if it was stopped
                if (!isCountdownRunning) {
                    startCountdownAndRefreshCycle()
                }

                isNoInternetBottomSheetShown = false
            } else {
                handleNoInternetState()
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        // Perform any additional tasks before finishing, if needed.
        finishAffinity()
    }



}