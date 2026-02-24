package com.flights.studio

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.text.method.LinkMovementMethod
import android.view.MotionEvent
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ScrollView
import android.widget.TextView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.Button
import androidx.compose.material3.DismissibleNavigationDrawer
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import kotlin.math.abs

@Composable
private fun SystemBarsSync() {

}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebviewFlights(
    startCardId: String,
    returnHome: Boolean,
    onExitToHome: () -> Unit,
    onExitNormal: () -> Unit,
    onOpenWelcome: () -> Unit,
) {
    SystemBarsSync()
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var cardId by rememberSaveable { mutableStateOf(startCardId) }

    LaunchedEffect(cardId) {
        if (cardId == "card1") onOpenWelcome()
    }

    val screenTitle = when (cardId) {
        "card1" -> "Welcome"
        "card2" -> "News"
        "card3" -> "Flights"
        "card4" -> "FBO"
        "about_us" -> "About Us"
        "contact_us" -> "Contact Us"
        "privacy_policy" -> "Privacy Policy"
        "licenses" -> "Licenses"
        else -> "Flight Tracker"
    }

    fun setCard(id: String) {
        cardId = id
        scope.launch { drawerState.close() }
    }
    DismissibleNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(
                drawerShape = RectangleShape
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp)
                ) {

                    Spacer(Modifier.height(12.dp))

                    // ===== MAIN SECTION =====
                    Text(
                        "Main",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp)
                    )

                    NavigationDrawerItem(
                        label = { Text("Welcome") },
                        selected = cardId == "card1",
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        onClick = { setCard("card1") }
                    )

                    NavigationDrawerItem(
                        label = { Text("News") },
                        selected = cardId == "card2",
                        icon = { Icon(Icons.AutoMirrored.Filled.Article, contentDescription = null) },
                        onClick = { setCard("card2") }
                    )

                    NavigationDrawerItem(
                        label = { Text("Flights") },
                        selected = cardId == "card3",
                        icon = { Icon(Icons.Default.Flight, contentDescription = null) },
                        onClick = { setCard("card3") }
                    )

                    NavigationDrawerItem(
                        label = { Text("FBO") },
                        selected = cardId == "card4",
                        icon = { Icon(Icons.Default.Business, contentDescription = null) },
                        onClick = { setCard("card4") }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // ===== INFORMATION SECTION =====
                    Text(
                        "Information",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp)
                    )

                    NavigationDrawerItem(
                        label = { Text("About us") },
                        selected = cardId == "about_us",
                        icon = { Icon(Icons.Default.Info, contentDescription = null) },
                        onClick = { setCard("about_us") }
                    )

                    NavigationDrawerItem(
                        label = { Text("Contact us") },
                        selected = cardId == "contact_us",
                        icon = { Icon(Icons.Default.Email, contentDescription = null) },
                        onClick = { setCard("contact_us") }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // ===== LEGAL SECTION =====
                    Text(
                        "Legal",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp)
                    )

                    NavigationDrawerItem(
                        label = { Text("Privacy Policy") },
                        selected = cardId == "privacy_policy",
                        icon = { Icon(Icons.Default.PrivacyTip, contentDescription = null) },
                        onClick = { setCard("privacy_policy") }
                    )

                    NavigationDrawerItem(
                        label = { Text("Licenses") },
                        selected = cardId == "licenses",
                        icon = { Icon(Icons.Default.Description, contentDescription = null) },
                        onClick = { setCard("licenses") }
                    )

                    Spacer(Modifier.height(16.dp))
                }
            }        }
    ) {
        val isDark = isSystemInDarkTheme()

        val topBarBg = if (isDark)
            Color(0xFF32302A)
        else
            MaterialTheme.colorScheme.surface   // softer than pure white

        val topBarFg = if (isDark)
            Color.White
        else
            MaterialTheme.colorScheme.onSurface
        Scaffold(
            containerColor = topBarBg,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = screenTitle,
                            color = topBarFg
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    if (drawerState.isClosed) drawerState.open()
                                    else drawerState.close()
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = topBarFg
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                if (returnHome) onExitToHome()
                                else onExitNormal()
                            }
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = topBarFg
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = topBarBg,
                        titleContentColor = topBarFg,
                        navigationIconContentColor = topBarFg,
                        actionIconContentColor = topBarFg
                    )
                )
            }
        ) { padding ->
            when (cardId) {

                "privacy_policy" -> {
                    PrivacyPolicyScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    )
                }

                "licenses" -> {
                    LicensesScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    )
                }

                else -> {
                    key(cardId) {
                        WebCardContent(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding),
                            cardId = cardId
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LegalHtmlScreen(
    html: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val backgroundColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground

    AndroidView(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor),
        factory = {

            val scrollView = ScrollView(context).apply {
                isFillViewport = true
                overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
            }

            val textView = TextView(context).apply {

                text = HtmlCompat.fromHtml(
                    html,
                    HtmlCompat.FROM_HTML_MODE_COMPACT
                )

                setTextColor(textColor.toArgb())
                textSize = 16f
                setLineSpacing(12f, 1.2f)

                movementMethod = LinkMovementMethod.getInstance()

                setPadding(40, 60, 40, 120)
            }

            scrollView.addView(textView)
            scrollView
        }
    )
}
@Composable
fun PrivacyPolicyScreen(modifier: Modifier = Modifier) {
    LegalHtmlScreen(
        html = stringResource(R.string.privacy_policy_goes_here),
        modifier = modifier
    )
}

@Composable
fun LicensesScreen(modifier: Modifier = Modifier) {
    LegalHtmlScreen(
        html = stringResource(R.string.licenses_content),
        modifier = modifier
    )
}


@Suppress("DEPRECATION")
private fun applyWebViewDarkMode(settings: WebSettings, isDark: Boolean) {
    if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
        WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, isDark)
    } else if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
        WebSettingsCompat.setForceDark(
            settings,
            if (isDark) WebSettingsCompat.FORCE_DARK_ON else WebSettingsCompat.FORCE_DARK_OFF
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WebCardContent(
    modifier: Modifier,
    cardId: String,
) {
    val context = LocalContext.current

    val isDark = isSystemInDarkTheme()
    val bgArgb = MaterialTheme.colorScheme.background.toArgb()

    val url = remember(cardId) { urlForCard(cardId) }

    var progress by remember(url) { mutableIntStateOf(0) }
    var showError by remember(url) { mutableStateOf(false) }
    var reloadTick by remember(url) { mutableIntStateOf(0) }

    val adHosts = remember { listOf("doubleclick.net", "googlesyndication.com") }

    var hasMainFrameError by remember(url) { mutableStateOf(false) }

    Box(modifier) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(rememberNestedScrollInteropConnection()),
            factory = {
                WebView(context).apply {
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)

                    setBackgroundColor(bgArgb)
                    alpha = 0f

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                        textZoom = 80
                        setSupportZoom(false)
                        builtInZoomControls = false
                        displayZoomControls = false
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                        allowFileAccess = false
                        allowContentAccess = false
                    }

                    applyWebViewDarkMode(settings, isDark)

                    setOnTouchListener(object : View.OnTouchListener {

                        var startX = 0f
                        var startY = 0f

                        override fun onTouch(v: View, event: MotionEvent): Boolean {

                            when (event.actionMasked) {

                                MotionEvent.ACTION_DOWN -> {
                                    startX = event.x
                                    startY = event.y
                                    v.parent?.requestDisallowInterceptTouchEvent(false)
                                }

                                MotionEvent.ACTION_MOVE -> {
                                    val dx = abs(event.x - startX)
                                    val dy = abs(event.y - startY)

                                    if (dy > dx) {
                                        // Vertical scroll → let WebView handle
                                        v.parent?.requestDisallowInterceptTouchEvent(true)
                                    } else {
                                        // Horizontal swipe → allow drawer
                                        v.parent?.requestDisallowInterceptTouchEvent(false)
                                    }
                                }

                                MotionEvent.ACTION_UP,
                                MotionEvent.ACTION_CANCEL -> {
                                    v.parent?.requestDisallowInterceptTouchEvent(false)
                                    v.performClick()
                                }
                            }

                            return false
                        }
                    })

                    webViewClient = object : WebViewClient() {

                        override fun onPageStarted(view: WebView?, u: String?, favicon: Bitmap?) {
                            showError = false
                            progress = 0
                            injectHideTriggers(view)
                            super.onPageStarted(view, u, favicon)
                        }

                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest
                        ): WebResourceResponse? {
                            val u = request.url.toString()

                            if (u.contains("scripts.min.js") || u.contains("trigger") || u.contains("footer")) {
                                return WebResourceResponse(
                                    "application/javascript",
                                    "UTF-8",
                                    ByteArrayInputStream("".toByteArray())
                                )
                            }

                            val host = request.url.host.orEmpty()
                            if (adHosts.any { host.contains(it) }) {
                                return WebResourceResponse(
                                    "text/plain",
                                    "utf-8",
                                    ByteArrayInputStream(ByteArray(0))
                                )
                            }

                            return super.shouldInterceptRequest(view, request)
                        }

                        override fun onPageCommitVisible(view: WebView?, url: String?) {
                            injectHideTriggers(view)
                            super.onPageCommitVisible(view, url)
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            injectHideTriggers(view)
                            if (!hasMainFrameError) {
                                view?.animate()?.alpha(1f)?.setDuration(250)?.start()
                            }
                            super.onPageFinished(view, url)
                        }

                        override fun onReceivedError(
                            view: WebView,
                            request: WebResourceRequest,
                            error: WebResourceError
                        ) {
                            if (request.isForMainFrame) {
                                hasMainFrameError = true
                                showError = true

                                view.stopLoading()
                                view.alpha = 0f   // 🔥 NEVER let WebView cover Compose UI
                            }
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            progress = newProgress
                        }
                    }

                    loadUrl(url)
                }
            },
            update = { wv ->
                wv.setBackgroundColor(bgArgb)
                applyWebViewDarkMode(wv.settings, isDark)

                if (reloadTick > 0) {
                    progress = 0
                    showError = false
                    wv.reload()
                    reloadTick = 0
                    return@AndroidView
                }

                if (wv.url != url) {
                    wv.alpha = 0f
                    progress = 0
                    showError = false
                    wv.loadUrl(url)
                }
            }
        )

        val target = (progress.coerceIn(0, 100) / 100f)

// Hold slightly before 100% for realism
        val displayTarget = if (target >= 0.99f) 1f else minOf(target, 0.95f)

// Smooth premium easing
        val animatedProgress by animateFloatAsState(
            targetValue = displayTarget,
            animationSpec = tween(
                durationMillis = if (target >= 0.99f) 300 else 220,
                easing = FastOutSlowInEasing
            ),
            label = "webProgress"
        )

// Fade out when finished
        val progressAlpha by animateFloatAsState(
            targetValue = if (progress >= 100) 0f else 1f,
            animationSpec = tween(300),
            label = "progressFade"
        )

        if (!showError && progressAlpha > 0f) {
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .graphicsLayer {
                        alpha = progressAlpha
                    },
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        if (showError) {

            // Hide WebView visually
            LaunchedEffect(Unit) {
                progress = 0
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {

                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        modifier = Modifier.size(42.dp)
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "No internet connection",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "Please check your connection and try again.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = {
                            hasMainFrameError = false
                            showError = false
                            reloadTick++
                        }
                    ) {
                        Text("Reload")
                    }
                }
            }        }
    }
}

private fun urlForCard(cardId: String): String =
    when (cardId) {
        "card2" -> "https://www.jacksonholeairport.com/about/news/"
        "card3" -> "https://www.jacksonholeairport.com/flights/"
        "card4" -> "https://www.jacksonholeflightservices.com/"
        "about_us" -> "https://www.jacksonholeairport.com/about/"
        "contact_us" -> "https://www.jacksonholeairport.com/about/contact/"
        else -> "https://www.jacksonholeairport.com/"
    }

private fun injectHideTriggers(view: WebView?) {
    view?.evaluateJavascript(
        """
        (function() {

          function removeElements() {

              // Remove mobile header
              var header = document.querySelector('header.site-header.header-mobile');
              if (header) header.remove();

              // Remove navbar
              var nav = document.querySelector('.jac-navbar');
              if (nav) nav.remove();

              // 🔥 Remove hero section (Flight Tracker title block)
              var heroSection = document.querySelector('section.page-hero.-noimage');
              if (heroSection) heroSection.remove();

              // Remove fixed trigger elements
              var triggers = document.querySelectorAll('.fixed-triggers');
              triggers.forEach(function(el) {
                  el.remove();
              });

              // Remove possible body spacing added by header
              document.body.style.marginTop = "0px";
              document.body.style.paddingTop = "0px";
          }

          // Initial removal
          removeElements();

          // Inject CSS fallback (in case elements reappear)
          var id = "fs_hide_triggers";
          var style = document.getElementById(id);
          if (!style) {
              style = document.createElement('style');
              style.id = id;
              document.head.appendChild(style);
          }

          style.innerHTML = `
            header.site-header.header-mobile,
            .jac-navbar,
            .fixed-triggers,
            section.page-hero.-noimage {
              display: none !important;
              visibility: hidden !important;
              opacity: 0 !important;
              height: 0 !important;
              pointer-events: none !important;
            }
          `;

          // Observe DOM changes (WordPress sometimes reinserts header)
          var observer = new MutationObserver(function() {
              removeElements();
          });

          observer.observe(document.body, {
              childList: true,
              subtree: true
          });

        })();
        """.trimIndent(),
        null
    )
}