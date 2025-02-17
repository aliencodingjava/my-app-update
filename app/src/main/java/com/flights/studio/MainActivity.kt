package com.flights.studio

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.io.ByteArrayInputStream


@Suppress("NAME_SHADOWING", "DEPRECATION")
class MainActivity : AppCompatActivity() {

    // Declare the flag at the class level
    private var isNoInternetBottomSheetShown = false
    private lateinit var webView: WebView
    private var currentUrl: String = ""
    private lateinit var linearProgressIndicator: LinearProgressIndicator
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback
    private lateinit var networkConnectivityHelper: NetworkConnectivityHelper
    private var isFabMenuOpen = false
    private var currentStatusBarColor: Int = R.color.actionbarwebsitestandart // Default color

    private fun updateUIForUrl(url: String, color: Int) {
        loadUrlInWebView(url)
        setStatusBarColor(color)
        updateProgressBarColor(color)
        saveState(url, color)
    }


    @SuppressLint("MissingInflatedId", "SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d("MainActivity", "onCreate: Activity created successfully")


        val preferences = getSharedPreferences("AppState", MODE_PRIVATE)
        currentUrl =
            preferences.getString("currentUrl", "https://www.jacksonholeairport.com/flights/") ?: ""
        val savedColor = preferences.getInt("statusBarColor", R.color.actionbarwebsitestandart)

        // Restore the last saved URL and color
        loadUrlInWebView(currentUrl)
        setStatusBarColor(savedColor)
        updateProgressBarColor(savedColor)

        linearProgressIndicator = findViewById(R.id.linearProgressIndicator)

        networkConnectivityHelper = NetworkConnectivityHelper(this)
        // To increase text size
        webView.settings.textZoom = 80
        webView = findViewById(R.id.webView)


        // Initialize the FABs

        val mainFab: FloatingActionButton = findViewById(R.id.mainFab)
        val fab1: FloatingActionButton = findViewById(R.id.fab1)
        val fab2: FloatingActionButton = findViewById(R.id.fab2)
        val fab3: FloatingActionButton = findViewById(R.id.fab3)
        val fabHome: FloatingActionButton = findViewById(R.id.fab_home) // New FAB

// Main FAB click listener
        mainFab.setOnClickListener {
            if (isFabMenuOpen) {
                closeFabMenu(fab1, fab2, fab3, fabHome) // Include fabHome
            } else {
                openFabMenu(fab1, fab2, fab3, fabHome) // Include fabHome
            }
            isFabMenuOpen = !isFabMenuOpen
        }


        fab1.setOnClickListener {
            if (networkConnectivityHelper.isNetworkAvailable()) {
                updateUIForUrl("https://www.jacksonholeairport.com/flights/", R.color.actionbarwebsitestandart)
            } else {
                showNoInternetBottomSheet()
            }
        }


        fab2.setOnClickListener {
            if (networkConnectivityHelper.isNetworkAvailable()) {
                updateUIForUrl("https://www.flightaware.com/live/airport/KJAC/", R.color.actionbarwebsitebluelight)
            } else {
                showNoInternetBottomSheet()
            }
        }

        fab3.setOnClickListener {
            if (networkConnectivityHelper.isNetworkAvailable()) {
                updateUIForUrl("https://beta.flightaware.com/live/airport/KJAC", R.color.actionbarwebsiteBlue)
            } else {
                showNoInternetBottomSheet()
            }
        }

        // Extension function to make custom animations for activity transitions
        fun Context.startActivityWithTransition(intent: Intent, enterAnim: Int, exitAnim: Int) {
            val options = ActivityOptions.makeCustomAnimation(this, enterAnim, exitAnim)
            startActivity(intent, options.toBundle())
        }

        fabHome.setOnClickListener {
            // Create the intent for navigating to SplashActivity
            val intent = Intent(this, SplashActivity::class.java).apply {
                // Optionally add flags to clear the back stack if necessary
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            // Start the activity with custom transition animation
            startActivityWithTransition(intent, R.anim.slide_in_right, R.anim.slide_out_right)

            // Optionally, log or handle additional actions
            Log.d("MainActivity", "Navigating to SplashActivity")

            // Finish current activity to avoid it being part of the back stack
            finish()
        }



        fun setupWebViewClient() {
            webView.webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    return if (request?.url?.toString()?.contains("ads") == true) {
                        WebResourceResponse(
                            "text/plain",
                            "utf-8",
                            ByteArrayInputStream("".toByteArray())
                        )
                    } else super.shouldInterceptRequest(view, request)
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val url = request?.url.toString()
                    // Directly return the result of the condition check.
                    // This eliminates the need for an explicit 'if' statement.
                    return !networkConnectivityHelper.isNetworkAvailable() || !isUrlSafe(url)
                }

                private fun isUrlSafe(url: String): Boolean {
                    val allowedDomains = fetchAllowedDomains()
                    val isSafe = allowedDomains.any { url.startsWith(it) }

                    // Check if the URL is not safe and is not intended to be opened in an external browser
                    if (!isSafe && !url.startsWith("https://www.google.com")) {
                        // Open the URL in an external browser
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(intent)
                        return false // Return false to prevent loading the URL in the WebView
                    }
                    return isSafe
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)

                    if (error?.errorCode == ERROR_HOST_LOOKUP ||
                        error?.errorCode == ERROR_CONNECT ||
                        error?.errorCode == ERROR_TIMEOUT
                    ) {
                        // Show custom BottomSheet for no internet connection.
                        showNoInternetBottomSheet()
                        // Make the noInternetOverlay visible
                        runOnUiThread {
                            findViewById<FrameLayout>(R.id.noInternetOverlay)?.visibility =
                                View.VISIBLE
                        }
                    }
                }


                private fun fetchAllowedDomains(): List<String> {
                    // Simulated function to fetch allowed domains from a source
                    val fetchedDomains = listOf(
                        "https://beta.flightaware.com/live/airport/KJAC",
                        "https://www.safe-site.com",
                        "https://www.flightaware.com",
                        "https://www.jacksonholeairport.com",
                        "https://visitjacksonhole.com/teton-pass-closure-information",
                        "https://www.flightaware.com/live/airport/KJAC/surface",
                        "https://www.jacksonholechamber.com/for-locals/commuters/?utm_source=visitjacksonhole.com&utm_medium=referral&utm_campaign=VisitJacksonHole",
                        "https://facebook.com"

                        // Add more domains as needed
                    )
                    return fetchedDomains
                }

            }
        }

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState)
            currentUrl = savedInstanceState.getString("currentUrl").toString()
        } else {

            initUIComponents()
            setupWebViewClient()
            handleNetworkAvailability()
        }

        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false
        webView.settings.allowFileAccess = false
        webView.settings.setGeolocationEnabled(false)


        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                linearProgressIndicator.progress = newProgress
                val progressTextView = findViewById<TextView>(R.id.progressTextView)
                progressTextView.visibility = if (newProgress == 100) View.GONE else View.VISIBLE

                val progressText = "$newProgress%"
                progressTextView.text = progressText
                val progressBarContainer = findViewById<FrameLayout>(R.id.progressBarContainer)

                if (newProgress < 100) {
                    progressBarContainer.visibility = View.VISIBLE
                    val progressText = "Loading the page… $newProgress%"
                    progressTextView.text = progressText
                } else {
                    progressBarContainer.visibility = View.GONE
                }
            }
        }


    }



    // Function to open the FAB menu with animations
    private fun openFabMenu(vararg fabs: FloatingActionButton) {
        // Make the fabMenuLayout visible and animate it
        val fabMenuLayout: LinearLayout = findViewById(R.id.fabMenuLayout)
        fabMenuLayout.visibility = View.VISIBLE
        fabMenuLayout.alpha = 0f

        fabMenuLayout.animate()
            .alpha(1f)
            .setDuration(300)
            .setInterpolator(DecelerateInterpolator()) // Smooth fade-in
            .start()

        // Create an AnimatorSet for FAB animations
        val animatorSet = AnimatorSet()
        val fabAnimations = mutableListOf<Animator>()

        // Animate individual FABs
        fabs.forEachIndexed { index, fab ->
            fab.visibility = View.VISIBLE
            fab.alpha = 0f
            fab.translationY = 100f * (index + 1) // Start off-screen

            // Adjust speed for fabHome
            val isFabHome = fab.id == R.id.fab_home
            val duration = if (isFabHome) 200L else 300L // Make fabHome faster
            val startDelay = if (isFabHome) 0L else 50L * index // No delay for fabHome

            // Create translation animation
            val translationAnimation =
                ObjectAnimator.ofFloat(fab, "translationY", 100f * (index + 1), 0f)
            translationAnimation.duration = duration
            translationAnimation.startDelay = startDelay
            translationAnimation.interpolator = DecelerateInterpolator() // Smooth motion

            // Create alpha (fade-in) animation
            val alphaAnimation = ObjectAnimator.ofFloat(fab, "alpha", 0f, 1f)
            alphaAnimation.duration = duration
            alphaAnimation.startDelay = startDelay
            alphaAnimation.interpolator = DecelerateInterpolator()

            // Add animations to the set
            fabAnimations.add(translationAnimation)
            fabAnimations.add(alphaAnimation)
        }

        // Play all FAB animations together
        animatorSet.playTogether(fabAnimations)
        animatorSet.start()
    }


    // Function to close the FAB menu with animations
    private fun closeFabMenu(vararg fabs: FloatingActionButton) {
        val fabMenuLayout: LinearLayout = findViewById(R.id.fabMenuLayout)
        if (!isFabMenuOpen) return // Prevent redundant calls
        isFabMenuOpen = true

        // Animate the fabMenuLayout to fade out smoothly
        fabMenuLayout.animate()
            .alpha(0f)
            .setDuration(300)
            .setInterpolator(DecelerateInterpolator())  // Smooth deceleration
            .withEndAction {
                fabMenuLayout.visibility = View.GONE
            }
            .start()

        // Create an AnimatorSet for FAB animations
        val animatorSet = AnimatorSet()
        val fabAnimations = mutableListOf<Animator>()

        // Animate each FAB
        fabs.forEachIndexed { index, fab ->
            // Translate FAB slightly and fade out with a decelerate interpolator
            val translationAnimation =
                ObjectAnimator.ofFloat(fab, "translationY", 0f, 100f * (index + 1))
            translationAnimation.duration = 300
            translationAnimation.startDelay = 50L * index  // Staggering with delay
            translationAnimation.interpolator = DecelerateInterpolator()  // Smooth out the movement

            val alphaAnimation = ObjectAnimator.ofFloat(fab, "alpha", 1f, 0f)
            alphaAnimation.duration = 300
            alphaAnimation.startDelay = 50L * index
            alphaAnimation.interpolator = DecelerateInterpolator()

            // Add animations to the set
            fabAnimations.add(translationAnimation)
            fabAnimations.add(alphaAnimation)
        }

        // Play animations together with an AnimatorSet
        animatorSet.playTogether(fabAnimations)
        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // Hide FABs after animations are completed
                fabs.forEach { it.visibility = View.GONE }
            }
        })

        animatorSet.start()
    }


    private fun updateProgressBarColor(colorResId: Int) {
        findViewById<FrameLayout>(R.id.progressBarContainer).setBackgroundColor(
            ContextCompat.getColor(this, colorResId)
        )
    }

    // Handle network availability
    private fun handleNetworkAvailability() {
        val noInternetOverlay = findViewById<FrameLayout>(R.id.noInternetOverlay)

        if (networkConnectivityHelper.isNetworkAvailable()) {
            noInternetOverlay.visibility = View.GONE
            // Network is available, load the initial URL
        } else {
            // Network is not available, show no internet dialog
            noInternetOverlay.visibility = View.VISIBLE
            showNoInternetBottomSheet()
        }
    }

    private fun loadInitialUrl() {
        if (networkConnectivityHelper.isNetworkAvailable()) {
            // Network is available, proceed with loading the URL
            currentUrl = "https://www.jacksonholeairport.com/flights/"
            webView.loadUrl(currentUrl)
            // Set the status bar color to match the initial URL's section
            setStatusBarColor(R.color.actionbarwebsitestandart)
        } else {
            // Network is not available, inform the user with the No Internet Bottom Sheet
            showNoInternetBottomSheet()
        }
    }

    private fun showNoInternetBottomSheet() {
        if (!isNoInternetBottomSheetShown) {
            isNoInternetBottomSheetShown = true
            val noInternetBottomSheet = NoInternetBottomSheetFragment.newInstance()
            noInternetBottomSheet.onTryAgainClicked = {
                if (networkConnectivityHelper.isNetworkAvailable()) {
                    findViewById<WebView>(R.id.webView)?.reload()
                    findViewById<FrameLayout>(R.id.noInternetOverlay)?.visibility = View.GONE
                    isNoInternetBottomSheetShown = false // Reset flag
                }
            }
            noInternetBottomSheet.show(supportFragmentManager, NO_INTERNET_BOTTOM_SHEET_TAG)

        }
    }

    companion object {
        const val NO_INTERNET_BOTTOM_SHEET_TAG = "NoInternetBottomSheetFragment"
    }

    // Initialize UI components
    @SuppressLint("SetJavaScriptEnabled")
    private fun initUIComponents() {
        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            useWideViewPort = true
            displayZoomControls = false
            builtInZoomControls = true
            loadWithOverviewMode = true

            // Secure WebView Settings
        }
        // Further UI component initialization
    }


    private fun setStatusBarColor(colorResId: Int) {
        val window = window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = ContextCompat.getColor(this, colorResId)
    }

    // Load URL in WebView
    @SuppressLint("SetJavaScriptEnabled")
    private fun loadUrlInWebView(url: String) {
        webView.loadUrl(url)
    }

    private fun setupNetworkCallback() {
        val connectivityManager =
            getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                Log.d("NetworkCallback", "Network is available")
                runOnUiThread {
                    retryConnection()
                }
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                Log.d("NetworkCallback", "Network is lost")
                // Handle network loss if needed
            }
        }
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
    }

    private fun resetWebViewState() {

        webView.settings.textZoom = 80 // Reset text zoom to default
        webView.settings.setSupportZoom(false) // Disable zooming if needed
        // Reset any other WebView settings as required
    }


    private fun retryConnection() {
        runOnUiThread {
            // Hide the noInternetOverlay
            findViewById<FrameLayout>(R.id.noInternetOverlay)?.visibility = View.GONE

            // Attempt to find and dismiss the bottom sheet
            (supportFragmentManager.findFragmentByTag(NO_INTERNET_BOTTOM_SHEET_TAG) as? NoInternetBottomSheetFragment)?.dismiss()

            // Reload the webView to refresh content now that the connection is restored
            webView.reload()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
        outState.putString("currentUrl", webView.url)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        // Restore the state of the WebView
        webView.restoreState(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        setupNetworkCallback()
        handleNetworkAvailability()


    }

    override fun onStop() {
        super.onStop()
        val connectivityManager =
            getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    override fun onResume() {
        super.onResume()
        resetWebViewState()
        loadInitialUrl()
        val preferences = getSharedPreferences("AppState", MODE_PRIVATE)
        val savedUrl = preferences.getString("currentUrl", "https://www.jacksonholeairport.com/flights/") ?: ""
        if (savedUrl != currentUrl) {
            currentUrl = savedUrl
            loadUrlInWebView(currentUrl)  // Load the restored or default URL if it has changed
        }
        // Restore the status bar color only if it has changed
        val savedColor = preferences.getInt("statusBarColor", R.color.actionbarwebsitestandart)
        if (savedColor != currentStatusBarColor) {
            setStatusBarColor(savedColor)
            currentStatusBarColor = savedColor  // Save the current status bar color for comparison next time
        }
        updateProgressBarColor(savedColor)
    }




    private fun saveState(url: String, color: Int) {
        val preferences = getSharedPreferences("AppState", MODE_PRIVATE)
        val editor = preferences.edit()
        editor.putString("currentUrl", url)
        editor.putInt("statusBarColor", color)
        editor.apply()
    }

}


