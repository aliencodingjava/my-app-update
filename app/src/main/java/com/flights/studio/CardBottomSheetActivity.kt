package com.flights.studio

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL


class CardBottomSheetActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var webView: WebView
    private lateinit var webViewContainer: FrameLayout
    private lateinit var errorImageView: ImageView
    private lateinit var errorReloadButton: MaterialButton
    private lateinit var navigationView: NavigationView
    private lateinit var userPrefsManager: UserPreferencesManager
    private lateinit var networkHelper: NetworkConnectivityHelper
    private var returnHome = false



    override fun onStart() {
        super.onStart()

        // Reapply WebViewClient and reinject script in case the page reloads or the WebView gets rebuilt
        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest): WebResourceResponse? {
                val url = request.url.toString()
                if (
                    url.contains("scripts.min.js") ||
                    url.contains("trigger") ||
                    url.contains("footer")
                ) {
                    return WebResourceResponse("application/javascript", "UTF-8", "".byteInputStream())
                }

                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageCommitVisible(view: WebView?, url: String?) {
                super.onPageCommitVisible(view, url)
                injectHideTriggers(view)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                injectHideTriggers(view)
                view?.animate()?.alpha(1f)?.setDuration(300)?.start()
            }
        }

        injectHideTriggers(webView)
    }




    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card_drawer)

        networkHelper = NetworkConnectivityHelper(applicationContext)
        returnHome = intent.getBooleanExtra("RETURN_HOME", false)

        // Replace deprecated launchWhenStarted:
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                networkHelper.isOnline()
                    .distinctUntilChanged()
                    .collect { online ->
                        viewModel.setOnline(online)
                    }
            }
        }



        // ✅ 4. Continue with the rest
        userPrefsManager = UserPreferencesManager(applicationContext)




        val cardId = intent.getStringExtra("CARD_ID") ?: "card1"
        if (cardId == "card1") {
            startIosPlayer()
            return
        }

        drawerLayout = findViewById(R.id.drawerLayout)
        webView = findViewById(R.id.webView)
        webView.setBackgroundColor(Color.TRANSPARENT)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.clearCache(true)
        webView.clearHistory()
        webView.clearFormData()
        webView.settings.cacheMode = WebSettings.LOAD_NO_CACHE


        webView.webViewClient = object : WebViewClient() {

            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest): WebResourceResponse? {
                val url = request.url.toString()

                // Block the specific script that injects the sticky bottom bar
                if (
                    url.contains("scripts.min.js") ||
                    url.contains("trigger") ||
                    url.contains("footer")
                ) {
                    return WebResourceResponse("application/javascript", "UTF-8", "".byteInputStream())
                }


                return super.shouldInterceptRequest(view, request)
            }


            override fun onPageCommitVisible(view: WebView?, url: String?) {
                super.onPageCommitVisible(view, url)
                injectHideTriggers(view)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                injectHideTriggers(view)
                view?.animate()?.alpha(1f)?.setDuration(300)?.start()
            }
        }




        webViewContainer = findViewById(R.id.webViewContainer)
        errorImageView = findViewById(R.id.errorImageView)
        errorReloadButton = findViewById(R.id.errorReloadButton)
        navigationView = findViewById(R.id.navigationView)

        val toolbar = findViewById<MaterialToolbar>(R.id.materialToolbar)

        val headerView = if (navigationView.headerCount > 0) {
            navigationView.getHeaderView(0)
        } else {
            navigationView.inflateHeaderView(R.layout.nav_header_layout)
        }

        setupHeader(headerView)
        setupToolbar(toolbar, drawerLayout, navigationView)
        setupNavigation(navigationView)
        setupNavigationToggle()

        lifecycleScope.launch {
            if (savedInstanceState != null) {
                webView.restoreState(savedInstanceState)
            } else {
                lifecycleScope.launch { loadWebContent(cardId) }
            }
        }

        // Smooth fade-in after theme change or restore
        webView.alpha = 0f
        webView.postDelayed({
            webView.animate().alpha(1f).setDuration(300).start()
        }, 150)

        errorReloadButton.setOnClickListener {
            lifecycleScope.launch {
                loadWebContent(cardId)
            }
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : androidx.activity.OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    exitScreen()
                }
            }
        )

    }
    @Suppress("DEPRECATION")

    private fun exitScreen() {
        if (returnHome) {
            // jump to MainActivity and clear anything above it
            val home = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(home)
            overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation)

            finish()
        } else {
            // normal behavior
            finish()
            overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation)
        }
    }


    private fun injectHideTriggers(view: WebView?) {
        view?.evaluateJavascript(
            """
        (function() {
            // Inject global style to permanently hide the triggers
            var style = document.createElement('style');
            style.innerHTML = `
                .fixed-triggers {
                    display: none !important;
                    visibility: hidden !important;
                    opacity: 0 !important;
                    height: 0px !important;
                    pointer-events: none !important;
                    position: absolute !important;
                    bottom: -9999px !important; 
                }
            `;
            document.head.appendChild(style);
        })();
        """.trimIndent(),
            null
        )
    }


    fun showCreateProfileSheet() {
        startActivity(Intent(this, ProfileDetailsComposeActivity::class.java))

        @Suppress("DEPRECATION")
        overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation)
    }





    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }


    override fun onResume() {
        super.onResume()

        // 🔁 Update data from prefs to LiveData before setting up header
        viewModel.refresh()

        val headerView = navigationView.getHeaderView(0)
        setupHeader(headerView)
    }



    private fun startIosPlayer() {
        startActivity(Intent(this, IosPlayerActivity1::class.java))
        finish()
    }

    // Updated setupHeader with 3-word limit and fade overlay
    private fun setupHeader(headerView: View) {
        // Views
        val userNameTextView      = headerView.findViewById<TextView>(R.id.userMainText)
        val userSubtitleTextView  = headerView.findViewById<TextView>(R.id.userSecondaryText)
        val userProfileImageView  = headerView.findViewById<ImageView>(R.id.iconImage)
        val userInitialsTextView  = headerView.findViewById<TextView>(R.id.iconInitials)
        val userStatusTextView    = headerView.findViewById<TextView>(R.id.userStatusText)

        // NEW: observe online/offline
        // NEW: observe online/offline with tinted icon
        viewModel.isOnline.observe(this) { online ->
            // pick the drawable
            val iconRes = if (online)
                R.drawable.signal_wifi_4_bar_24dp_ffffff_fill1_online
            else
                R.drawable.signal_wifi_bad_24dp_ffffff_fill1_offlne_icon_header

            // set the drawable on the left
            userStatusTextView.setCompoundDrawablesWithIntrinsicBounds(iconRes, 0, 0, 0)

            // tint it
            userStatusTextView.compoundDrawables[0]?.setTint(
                ContextCompat.getColor(
                    this,
                    if (online) R.color.neon_red else R.color.disabled_tint
                )
            )

            // update the text
            userStatusTextView.text = if (online) "• Online •" else "• Offline •"
            userStatusTextView.visibility = View.VISIBLE
        }


        // Click to open profile details (Compose version)
        userProfileImageView.setOnClickListener {
            startActivity(Intent(this, ProfileDetailsComposeActivity::class.java))
        }

        userInitialsTextView.setOnClickListener {
            startActivity(Intent(this, ProfileDetailsComposeActivity::class.java))
        }


        // Animate in
        val viewsToAnimate = listOf(
            userNameTextView,
            userSubtitleTextView,
            userProfileImageView,
            userInitialsTextView,
            userStatusTextView
        )
        viewsToAnimate.forEach { it.alpha = 0f }

        // Observe login state
        viewModel.isLoggedIn.observe(this) { isLoggedIn ->
            if (!isLoggedIn) {
                userNameTextView.text = getString(R.string.not_logged_in)
                userSubtitleTextView.text = getString(R.string.tap_to_log_in)
                userSubtitleTextView.visibility = View.VISIBLE
                userProfileImageView.setImageResource(R.drawable.rt_text_logo_jac)
                userProfileImageView.visibility = View.VISIBLE
                userInitialsTextView.visibility = View.GONE
            }
        }

        // Observe and bind name
        viewModel.name.observe(this) { fullName ->
            if (viewModel.isLoggedIn.value == true) {
                val raw = fullName?.trim().orEmpty()
                val tokens = raw.split(Regex("\\s+")).filter { it.isNotBlank() }
                val leading = tokens.takeWhile { tok -> tok.none { it.isLetterOrDigit() } }
                val trailing = tokens.takeLastWhile { tok -> tok.none { it.isLetterOrDigit() } }
                val core = tokens.filter { tok -> tok.any { it.isLetterOrDigit() } }
                val dispTokens = leading + core + trailing
                userNameTextView.text = dispTokens.joinToString(" ").ifEmpty { getString(R.string.not_logged_in) }
            }
        }

        // Observe phone
        viewModel.phone.observe(this) { phone ->
            if (viewModel.isLoggedIn.value == true) {
                userSubtitleTextView.text = phone.orEmpty()
                userSubtitleTextView.visibility = if (phone.isNullOrEmpty()) View.GONE else View.VISIBLE
            }
        }

        // Observe photo
        viewModel.photoUri.observe(this) { uriString ->
            if (viewModel.isLoggedIn.value == true) {
                val loaded = loadUserPhoto(uriString, userProfileImageView)
                if (loaded) {
                    userInitialsTextView.visibility = View.GONE
                    userProfileImageView.visibility = View.VISIBLE
                } else {
                    viewModel.initials.value?.let { initials ->
                        applyFallbackIcon(userProfileImageView, userInitialsTextView, initials)
                    }
                }
            }
        }

        // Smooth fade-in
        headerView.postDelayed({
            viewsToAnimate.forEach { it.animate().alpha(1f).setDuration(200).start() }
        }, 100)
    }

    private fun loadUserPhoto(photoRaw: String?, iconImage: ImageView): Boolean {
        if (photoRaw.isNullOrBlank()) {
            iconImage.visibility = View.GONE
            return false
        }

        val raw = photoRaw.trim()

        // ✅ If it's already usable in UI
        if (
            raw.startsWith("http", ignoreCase = true) ||
            raw.startsWith("content", ignoreCase = true) ||
            raw.startsWith("file", ignoreCase = true)
        ) {
            Glide.with(this)
                .load(raw) // string works for http/content/file
                .override(512, 512)
                .centerCrop()
                .placeholder(R.drawable.contact_logo_topbar)
                .error(R.drawable.contact_logo_topbar)
                .into(iconImage)

            iconImage.visibility = View.VISIBLE
            return true
        }

        // ✅ Otherwise it's a STORAGE PATH like "profiles/<uid>/avatar.jpg"
        lifecycleScope.launch {
            val session = SupabaseManager.client.auth.currentSessionOrNull()
            val token = session?.accessToken
            if (token.isNullOrBlank()) return@launch

            val signed = SupabaseStorageUploader.createSignedUrl(
                objectPath = raw,
                authToken = token,
                bucket = "profile-photos"
            )

            if (!signed.isNullOrBlank()) {
                Glide.with(this@CardBottomSheetActivity)
                    .load(signed)
                    .override(512, 512)
                    .centerCrop()
                    .placeholder(R.drawable.contact_logo_topbar)
                    .error(R.drawable.contact_logo_topbar)
                    .into(iconImage)

                iconImage.visibility = View.VISIBLE
            }
        }

        // we will show it async after signing
        iconImage.visibility = View.VISIBLE
        return true
    }

    private fun applyFallbackIcon(iconImage: ImageView, iconInitials: TextView, initials: String?) {
        if (!initials.isNullOrEmpty()) {
            iconInitials.text = initials
            iconInitials.visibility = View.VISIBLE
            iconImage.setImageDrawable(null)
            iconImage.visibility = View.INVISIBLE
        } else {
            iconImage.setImageResource(R.drawable.contact_logo_topbar)
            iconImage.visibility = View.VISIBLE
            iconInitials.visibility = View.GONE
        }
    }



    private fun setupToolbar(
        toolbar: MaterialToolbar,
        drawerLayout: DrawerLayout,
        navigationView: NavigationView
    ) {
        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(navigationView)
        }
        toolbar.setOnMenuItemClickListener { menuItem ->
            if (menuItem.itemId == R.id.action_close) {
                exitScreen()
                true
            } else {
                false
            }
        }

    }

    private fun setupNavigation(navigationView: NavigationView) {
        // 1) Initial menu label/icon
        refreshLogoutMenuItem(navigationView)

        // 2) Handle taps
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_logout -> {
                    if (viewModel.isLoggedIn.value == true) {
                        showLogoutDialog {
                            viewModel.logout()
                            showToast("Logged out")
                            val headerView = navigationView.getHeaderView(0)
                            setupHeader(headerView)
                            refreshLogoutMenuItem(navigationView)
                            setupNavigationToggle()
                        }
                    } else {
                        // — not logged in: show the sheet (it will auto-rebind on save)
                        showCreateProfileSheet()
                    }
                    drawerLayout.closeDrawer(navigationView)
                    true
                }
                R.id.nav_welcome -> {
                    startActivity(Intent(this, IosPlayerActivity1::class.java))
                    drawerLayout.closeDrawer(navigationView)
                    true
                }
                R.id.nav_about_us -> {
                    loadCard("about_us")
                    drawerLayout.closeDrawer(navigationView)
                    true
                }
                R.id.nav_contact_us -> {
                    loadCard("contact_us")
                    drawerLayout.closeDrawer(navigationView)
                    true
                }
                R.id.nav_news -> {
                    loadCard("card2")
                    drawerLayout.closeDrawer(navigationView)
                    true
                }
                R.id.nav_flights -> {
                    loadCard("card3")
                    drawerLayout.closeDrawer(navigationView)
                    true
                }
                R.id.nav_fbo -> {
                    loadCard("card4")
                    drawerLayout.closeDrawer(navigationView)
                    true
                }
                else -> false
            }
        }
    }

    private fun refreshLogoutMenuItem(navigationView: NavigationView) {
        val logoutItem = navigationView.menu.findItem(R.id.nav_logout)
        if (logoutItem != null) {
            if (userPrefsManager.isLoggedIn) {
                logoutItem.title = getString(R.string.logout)
                logoutItem.icon = AppCompatResources.getDrawable(
                    this,
                    R.drawable.logout_24dp_ffffff_fill0_wght400_grad0_opsz24
                )
            } else {
                logoutItem.title = getString(R.string.login)
                logoutItem.icon = AppCompatResources.getDrawable(
                    this,
                    R.drawable.login_24dp_ffffff_fill0_wght400_grad0_opsz24
                )
            }
        }
    }

    private fun loadCard(cardId: String) {
        lifecycleScope.launch {
            if (cardId == "card1") {
                startIosPlayer()
            } else {
                loadWebContent(cardId)
            }
            drawerLayout.closeDrawer(navigationView)
        }
    }


    private suspend fun loadWebContent(cardId: String) {
        if (isRealInternetAvailable()) {
            errorImageView.visibility = View.GONE
            errorReloadButton.visibility = View.GONE
            webView.visibility = View.VISIBLE
            CardWebViewHelper.setup(
                this,
                cardId,
                webViewContainer,
                webView,
                errorImageView,
                errorReloadButton
            )
        } else {
            errorImageView.visibility = View.VISIBLE
            errorReloadButton.visibility = View.VISIBLE
            showToast("No internet connection.")
        }
    }
    override fun onDestroy() {

        // Clean up the WebView
        findViewById<WebView>(R.id.webView)?.apply {
            loadUrl("about:blank")
            stopLoading()
            clearHistory()
            removeAllViews()
            destroy()
        }


        super.onDestroy()
    }


    private suspend fun isRealInternetAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://clients3.google.com/generate_204")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "Android")
            conn.setRequestProperty("Connection", "close")
            conn.connectTimeout = 1500
            conn.connect()
            conn.responseCode == 204
        } catch (_: Exception) {
            false
        }
    }

    private fun setupNavigationToggle() {
        val headerView       = navigationView.getHeaderView(0)
        val headerArrow      = headerView.findViewById<ImageView>(R.id.headerArrow)

        // ← NEW lookups
        val authCard         = headerView.findViewById<MaterialCardView>(R.id.headerAuthCard)
        val authIcon         = headerView.findViewById<ImageView>(R.id.headerAuthIcon)
        val authTextView     = headerView.findViewById<TextView>(R.id.headerAuthText)

        val userSecondaryText = headerView.findViewById<TextView>(R.id.userSecondaryText)

        var isExpanded = false

        // ← UPDATED to drive icon + text
        fun updateAuthUi(isLoggedIn: Boolean) {
            if (isLoggedIn) {
                authIcon.setImageResource(R.drawable.logout_24dp_ffffff_fill0_wght400_grad0_opsz24)
                authTextView.text   = getString(R.string.logout)
            } else {
                authIcon.setImageResource(R.drawable.login_24dp_ffffff_fill0_wght400_grad0_opsz24)
                authTextView.text   = getString(R.string.login)
            }
        }
         fun toggleAuthButton() {
            isExpanded = !isExpanded
            headerArrow.animate()
                .rotation(if (isExpanded) 180f else 0f)
                .setDuration(200)
                .start()

            // we need the full height to slide it completely off-screen
            val h = authCard.height.toFloat().takeIf { it>0 } ?: 200f

            if (isExpanded) {
                // start just above its normal spot
                authCard.translationY = -h
                authCard.alpha        = 0f
                authCard.visibility   = View.VISIBLE

                authCard.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(300)
                    .setInterpolator(android.view.animation.OvershootInterpolator())
                    .start()

            } else {
                authCard.animate()
                    .translationY(-h)
                    .alpha(0f)
                    .setDuration(250)
                    .withEndAction { authCard.visibility = View.INVISIBLE }
                    .start()
            }
        }

        // initial state
        updateAuthUi(viewModel.isLoggedIn.value == true)

        // wire up taps
        headerArrow.setOnClickListener    { toggleAuthButton() }
        userSecondaryText.setOnClickListener { toggleAuthButton() }
        authCard.setOnClickListener {
            if (viewModel.isLoggedIn.value == true) {
                showLogoutDialog {
                    viewModel.logout()
                    showToast("Logged out")
                    // refresh header & menu
                    setupHeader(headerView)
                    refreshLogoutMenuItem(navigationView)
                    updateAuthUi(false)
                }
            } else {
                showCreateProfileSheet()
            }
            // collapse
            isExpanded = false
            headerArrow.animate().rotation(0f).setDuration(200).start()
            authCard.animate()
                .alpha(0f)
                .translationY(-20f)
                .setDuration(150)
                .withEndAction { authCard.visibility = View.INVISIBLE }
                .start()
        }
    }
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private val viewModel: CardBottomSheetViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CardBottomSheetViewModel(userPrefsManager) as T
            }
        }
    }
    private fun showLogoutDialog(onConfirm: () -> Unit) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.title_log_out)
            .setMessage(R.string.confirm_log_out)
            .setPositiveButton(R.string.yes) { _, _ ->
                onConfirm()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }





}


