package com.flights.studio

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context.CONNECTIVITY_SERVICE
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.flights.studio.ContactUsAdapter.Companion.getContactUsDetails
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import java.io.ByteArrayInputStream


@Suppress("DEPRECATION")
class CardBottomSheetFragment : BottomSheetDialogFragment() {

    companion object {
        fun newInstance(cardId: String): CardBottomSheetFragment {
            val fragment = CardBottomSheetFragment()
            val args = Bundle()
            args.putString("CARD_ID", cardId)
            fragment.arguments = args
            return fragment
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true



    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val cardId = arguments?.getString("CARD_ID")
        Log.d("CardBottomSheetFragment", "Loading layout for cardId: $cardId")

        return when (cardId) {
            "card1" -> inflater.inflate(R.layout.card1_fullscreen, container, false)
            "card2" -> inflater.inflate(R.layout.card2_fullscreen, container, false)
            "card3" -> inflater.inflate(R.layout.card3_fullscreen, container, false)
            "card4" -> inflater.inflate(R.layout.card4_fullscreen, container, false)
            "about_us" -> inflater.inflate(R.layout.about_us, container, false)
            "contact_us" -> inflater.inflate(R.layout.contact_us, container, false)
            else -> {
                Log.e("CardBottomSheetFragment", "Unknown cardId: $cardId")
                null
            }
        }
    }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            window?.attributes?.windowAnimations = R.style.CustomBottomSheetAnimation


        }

    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val webView = view?.findViewById<WebView>(R.id.webView)
        webView?.saveState(outState)
    }


    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        animateSlideAndZoomOut()
    }
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)


    }




    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val drawerLayout = view.findViewById<DrawerLayout>(R.id.drawerLayout)
        val navigationView = view.findViewById<NavigationView>(R.id.navigationView)
        val params = navigationView.layoutParams
        params.width = resources.getDimensionPixelSize(R.dimen.drawer_width)
        navigationView.layoutParams = params
        val globalWebView = view.findViewById<WebView>(R.id.webView)
        val toolbar = view.findViewById<MaterialToolbar>(R.id.materialToolbar)
        val webViewContainer = view.findViewById<FrameLayout>(R.id.webViewContainer)
        val bottomSheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        val progressBarContainer = view.findViewById<View>(R.id.progressBarContainer)



        // Open drawer on menu icon click
        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(navigationView) // Open the navigation drawer
            zoomOutBottomSheet(bottomSheet) // Apply "zoom-out" effect to the BottomSheet
        }


        // Handle close button menu
        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_close -> {
                    dismiss() // Close the fragment
                    true
                }
                else -> false
            }
        }

        // Add DrawerListener to handle navigation drawer events
        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                bottomSheet?.translationY = 10f * slideOffset
            }

            override fun onDrawerOpened(drawerView: View) {}

            override fun onDrawerClosed(drawerView: View) {
                resetBottomSheetEffect(bottomSheet)
            }

            override fun onDrawerStateChanged(newState: Int) {}
        })


        // Navigation menu item selection
        navigationView.setNavigationItemSelectedListener { menuItem ->
            val cardId = when (menuItem.itemId) {
                R.id.nav_about_us -> "about_us"
                R.id.nav_contact_us -> "contact_us"
                R.id.nav_welcome -> "card1"
                R.id.nav_news -> "card2"
                R.id.nav_flights -> "card3"
                R.id.nav_fbo -> "card4"
                else -> "card1"
            }

            if (isInternetAvailable() || cardId == "about_us" || cardId == "contact_us") {
                setupWebView(globalWebView, cardId, webViewContainer, progressBarContainer)
            } else {
                showNoInternetDialog()
            }

            drawerLayout.closeDrawer(navigationView)
            resetBottomSheetEffect(bottomSheet)

            true
        }

        // Load initial page
        val initialCardId = arguments?.getString("CARD_ID") ?: "card1"
        if (savedInstanceState != null) {
            globalWebView.restoreState(savedInstanceState)
        } else {
            if (isInternetAvailable()) {
                setupWebView(globalWebView, initialCardId, webViewContainer, progressBarContainer)
            } else {
                showNoInternetDialog()
            }
        }



        }


      private fun zoomOutBottomSheet(bottomSheet: View?) {
        bottomSheet?.animate()
            ?.scaleX(1f) // Slightly reduce horizontal scale
            ?.scaleY(1f) // Slightly reduce vertical scale
            ?.translationY(1f) // Slightly move down
            ?.setDuration(150) // Quick animation duration
            ?.start()
    }

    private fun resetBottomSheetEffect(bottomSheet: View?) {
        bottomSheet?.animate()
            ?.scaleX(1f) // Reset horizontal scale
            ?.scaleY(1f) // Reset vertical scale
            ?.translationY(0f) // Reset position
            ?.setDuration(150) // Quick animation duration
            ?.start()
    }


    private fun animateSlideAndZoomOut() {
        val bottomSheet =
            dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        if (bottomSheet != null) {
            val halfScreenHeight = resources.displayMetrics.heightPixels / 2f
            bottomSheet.animate()
                .translationY(halfScreenHeight)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction {
                    bottomSheet.animate()
                        .scaleX(0f)
                        .scaleY(1f)
                        .alpha(0f)
                        .setDuration(200)
                        .withEndAction { dismiss() }
                        .start()
                }
                .start()
        } else {
            dismiss()
        }
    }

    @SuppressLint("SetJavaScriptEnabled", "RestrictedApi")
    private fun setupWebView(
        webView: WebView,
        cardId: String?,
        webViewContainer: FrameLayout,
        progressBar: View?
      ) {
        // Clear all existing views in the container
        webViewContainer.removeAllViews()

        when (cardId) {
            "about_us" -> {
                // Inflate and set up the "About Us" layout
                val aboutUsView = LayoutInflater.from(context).inflate(R.layout.about_us, webViewContainer, false)
                webViewContainer.addView(aboutUsView)

                // Set the bottom sheet background
                val hrContactText = view?.findViewById<TextView>(R.id.hrContactText)
                hrContactText?.setOnClickListener {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:hr@jhairport.org")
                    }
                    startActivity(Intent.createChooser(intent, "Send email via"))
                }

                // Initialize button click logic for Job Application
                val jobApplicationButton = aboutUsView.findViewById<MaterialButton>(R.id.aboutUsJobApplication)
                jobApplicationButton?.setOnClickListener {
                    Log.d("CardBottomSheetFragment", "Job Application button clicked!")
                    val pdfUrl = getString(R.string.job_application_link)

                    try {
                        // Create a new WebView to display the PDF
                        val dialogWebView = WebView(requireContext()).apply {
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                                setSupportZoom(true)
                                builtInZoomControls = true
                                displayZoomControls = false
                            }
                            loadUrl("https://docs.google.com/viewer?embedded=true&url=$pdfUrl")
                        }
                        // Create and configure the dialog to show the WebView
                        val dialog = Dialog(requireContext()).apply {
                            setContentView(dialogWebView)
                            setCancelable(true)
                            setCanceledOnTouchOutside(true)
                            window?.apply {
                                setLayout(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                setBackgroundDrawableResource(android.R.color.transparent)
                            }
                        }

                        dialog.show()
                    } catch (e: Exception) {
                        Log.e("CardBottomSheetFragment", "Error opening PDF: ${e.message}", e)
                        Toast.makeText(requireContext(), "Failed to load PDF.", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            "contact_us" -> {
                try {
                    // Inflate and set up the "Contact Us" layout
                    val contactUsView = LayoutInflater.from(context).inflate(R.layout.contact_us, webViewContainer, false)
                    webViewContainer.addView(contactUsView)

                    // Set up RecyclerView
                    val recyclerView = contactUsView.findViewById<RecyclerView>(R.id.recyclerView)
                    if (recyclerView == null) throw Exception("RecyclerView not found in layout")

                    val contactUsDetails = getContactUsDetails(requireContext())
                    recyclerView.layoutManager = LinearLayoutManager(requireContext())
                    recyclerView.adapter = ContactUsAdapter(contactUsDetails, requireContext()) { latitude, longitude, label ->
                        try {
                            // Create a custom Snackbar
                            val snackbar = Snackbar.make(webViewContainer, "", Snackbar.LENGTH_INDEFINITE)

                            // Inflate custom view
                            val snackbarLayout = snackbar.view as Snackbar.SnackbarLayout
                            val customView = LayoutInflater.from(requireContext())
                                .inflate(R.layout.snackbar_countdown, snackbarLayout, false)
                            snackbarLayout.addView(customView, 0)

                            // References to views in the custom layout
                            val countdownCircle = customView.findViewById<com.github.lzyzsd.circleprogress.ArcProgress>(R.id.countdownCircle)
                            val dismissButton = customView.findViewById<ImageView>(R.id.dismissButton)
                            val openMapButton = customView.findViewById<MaterialButton>(R.id.openMapButton)

                            // Countdown logic
                            countdownCircle.max = 20
                            var secondsRemaining = 20
                            val handler = Handler(Looper.getMainLooper())
                            val countdownRunnable = object : Runnable {
                                override fun run() {
                                    countdownCircle.progress = --secondsRemaining
                                    if (secondsRemaining > 0) {
                                        handler.postDelayed(this, 1000)
                                    } else {
                                        snackbar.dismiss()
                                    }
                                }
                            }
                            handler.post(countdownRunnable)

                            // Dismiss button action
                            dismissButton.setOnClickListener {
                                handler.removeCallbacks(countdownRunnable)
                                snackbar.dismiss()
                            }

                            // Open map button action
                            openMapButton.setOnClickListener {
                                handler.removeCallbacks(countdownRunnable)
                                snackbar.dismiss()

                                // Open map in WebView
                                val dialogWebView = WebView(requireContext()).apply {
                                    settings.apply {
                                        javaScriptEnabled = true
                                        domStorageEnabled = true
                                        loadWithOverviewMode = true
                                        useWideViewPort = true
                                        cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                                        setSupportZoom(true)
                                        builtInZoomControls = true
                                        displayZoomControls = false
                                    }
                                    loadUrl("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
                                }

                                val mapDialog = Dialog(requireContext()).apply {
                                    setContentView(dialogWebView)
                                    setCancelable(true)
                                    setCanceledOnTouchOutside(true)
                                    window?.setLayout(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    window?.setBackgroundDrawableResource(android.R.color.transparent)
                                }
                                mapDialog.show()
                            }

                            // Show the Snackbar
                            snackbar.show()
                        } catch (e: Exception) {
                            Log.e("CardBottomSheetFragment", "Error showing map: ${e.message}", e)
                            Toast.makeText(requireContext(), "Failed to load map.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("CardBottomSheetFragment", "Error initializing Contact Us: ${e.message}", e)
                    Toast.makeText(requireContext(), "Failed to load Contact Us content.", Toast.LENGTH_SHORT).show()
                }
            }

            else -> {
                // Handle dynamic content for other cards
                webViewContainer.addView(webView)
                webView.settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    javaScriptCanOpenWindowsAutomatically = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    cacheMode = WebSettings.LOAD_NO_CACHE
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                    textZoom = 75
                }

                val url = when (cardId) {
                    "card1" -> "https://www.youtube.com/embed/xR9tE01Z8oY?autoplay=1&start=0&end=255"
                    "card2" -> "https://www.jacksonholeairport.com/about/news/"
                    "card3" -> "https://www.jacksonholeairport.com/flights/"
                    "card4" -> "https://www.jacksonholeflightservices.com/"
                    else -> "about:blank"
                }

                // Check if zoom is not working and try to load the page with overridden viewport
                if (cardId == "card1" || cardId == "card3" || cardId == "card2") {

                    val modifiedUrl = """
                    <html>
                        <head>
                            <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=yes">
                            <style>
                                body { margin: 0; padding: 0; }
                                iframe {
                                    width: 100%;
                                    height: 100vh; /* Full screen height */
                                    border: none;
                                }
                            </style>
                        </head>
                        <body>
                            <iframe src="$url"></iframe>
                        </body>
                    </html>
                """
                    webView.loadData(modifiedUrl, "text/html", "UTF-8")
                } else {
                    webView.loadUrl(url)
                }

                // Set the rounded bottom sheet background
                (webViewContainer.parent as? View)?.setBackgroundResource(R.drawable.rounded_newbottomsheet_flat_bottom)
            }
        }

        progressBar?.visibility = if (cardId == "about_us" || cardId == "contact_us") View.GONE else View.VISIBLE

        val progressBarContainer = progressBar?.findViewById<View>(R.id.progressBarContainer)
        val progressIndicator =
            progressBarContainer?.findViewById<LinearProgressIndicator>(R.id.linearProgressIndicator)
        val progressTextView = progressBarContainer?.findViewById<TextView>(R.id.progressTextView)

        webView.webViewClient = object : WebViewClient() {
            // Declare adHosts at the top of the class
            private val adHosts = listOf(
                "ads.example.com",
                "tracking.example.com",
                "adservice.google.com",
                "doubleclick.net",
                "googlesyndication.com"
            )

            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val urlHost = request?.url?.host ?: return super.shouldInterceptRequest(view, request)
                Log.d("AdBlock", "Intercepting: $urlHost")
                if (adHosts.any { urlHost.contains(it) }) {
                    return WebResourceResponse(
                        "text/plain",
                        "utf-8",
                        ByteArrayInputStream("".toByteArray())
                    )
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                progressBarContainer?.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                progressBarContainer?.visibility = View.GONE
                webView.evaluateJavascript(
                    """
                    document.querySelectorAll('embed').forEach(function(element) {
                        element.style.display = 'none';
                    });
                """, null
                )
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                super.onReceivedError(view, request, error)
                if (request.isForMainFrame) {
                    showErrorWithRetry(webView)
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                progressIndicator?.progress = newProgress
                progressTextView?.text = getString(R.string.progress_percentage, newProgress)
                progressTextView?.visibility = if (newProgress == 100) View.GONE else View.VISIBLE

                if (newProgress == 100) {
                    progressBarContainer?.visibility = View.GONE
                } else {
                    progressBarContainer?.visibility = View.VISIBLE
                }
            }
        }
    }


    private fun showErrorWithRetry(webView: WebView) {
        val errorHtml = """
            <html>
            <body>
                <h3>Failed to load content. Please check your internet connection and try again.</h3>
                <button onclick="location.reload()">Retry</button>
            </body>
            </html>
        """
        webView.loadData(errorHtml, "text/html", "UTF-8")
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager =
            context?.getSystemService(CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = connectivityManager?.activeNetworkInfo
        return activeNetwork?.isConnectedOrConnecting == true
    }

    private fun showNoInternetDialog() {
        if (!isAdded || isRemoving) return // Prevent crashes during fragment lifecycle transitions.

        val noInternetDialog = NoInternetBottomSheetFragment.newInstance()
        noInternetDialog.onTryAgainClicked = { // Callback for when "Try Again" is clicked
            if (isAdded && !isRemoving) {
                val cardId = arguments?.getString("CARD_ID")
                if (parentFragmentManager.findFragmentByTag("CardBottomSheetFragment") == null) {
                    val cardFragment =
                        newInstance(cardId ?: "card1") // Default to card1 if ID is null
                    cardFragment.show(parentFragmentManager, "CardBottomSheetFragment")
                }
            }
        }
        noInternetDialog.show(parentFragmentManager, "NoInternetBottomSheetFragment")
    }


    override fun onStart() {
        super.onStart()
        val dialog = dialog as? BottomSheetDialog ?: return
        val bottomSheet =
            dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                ?: return
        setupBottomSheet(bottomSheet)
    }

    private fun setupBottomSheet(bottomSheet: View) {
        val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.isHideable = false
        bottomSheetBehavior.isDraggable = true
        bottomSheetBehavior.skipCollapsed = false
        bottomSheetBehavior.peekHeight = dpToPx()
        bottomSheet.layoutParams = bottomSheet.layoutParams.apply {
            width = ViewGroup.LayoutParams.MATCH_PARENT
            height = ViewGroup.LayoutParams.MATCH_PARENT
        }
        bottomSheet.requestLayout()
        (dialog as BottomSheetDialog).window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setDimAmount(0f)
        }
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun dpToPx(): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            75f,
            resources.displayMetrics
        ).toInt()
    }




}
