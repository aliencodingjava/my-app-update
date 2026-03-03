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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.ui.zIndex
import androidx.core.text.HtmlCompat
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.flights.studio.FlightsTabsInjector.injectHideTriggers
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

            ModalDrawerSheet(drawerShape = RectangleShape) {

                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp)
                ) {

                    Spacer(Modifier.height(12.dp))

                    Text(
                        "Main",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp)
                    )

                    NavigationDrawerItem(
                        label = { Text("Welcome") },
                        selected = cardId == "card1",
                        icon = { Icon(Icons.Default.Home, null) },
                        onClick = { setCard("card1") }
                    )

                    NavigationDrawerItem(
                        label = { Text("News") },
                        selected = cardId == "card2",
                        icon = { Icon(Icons.AutoMirrored.Filled.Article, null) },
                        onClick = { setCard("card2") }
                    )

                    NavigationDrawerItem(
                        label = { Text("Flights") },
                        selected = cardId == "card3",
                        icon = { Icon(Icons.Default.Flight, null) },
                        onClick = { setCard("card3") }
                    )

                    NavigationDrawerItem(
                        label = { Text("FBO") },
                        selected = cardId == "card4",
                        icon = { Icon(Icons.Default.Business, null) },
                        onClick = { setCard("card4") }
                    )

                    HorizontalDivider(Modifier.padding(vertical = 12.dp))

                    Text(
                        "Information",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp)
                    )

                    NavigationDrawerItem(
                        label = { Text("About us") },
                        selected = cardId == "about_us",
                        icon = { Icon(Icons.Default.Info, null) },
                        onClick = { setCard("about_us") }
                    )

                    NavigationDrawerItem(
                        label = { Text("Contact us") },
                        selected = cardId == "contact_us",
                        icon = { Icon(Icons.Default.Email, null) },
                        onClick = { setCard("contact_us") }
                    )

                    HorizontalDivider(Modifier.padding(vertical = 12.dp))

                    Text(
                        "Legal",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp)
                    )

                    NavigationDrawerItem(
                        label = { Text("Privacy Policy") },
                        selected = cardId == "privacy_policy",
                        icon = { Icon(Icons.Default.PrivacyTip, null) },
                        onClick = { setCard("privacy_policy") }
                    )

                    NavigationDrawerItem(
                        label = { Text("Licenses") },
                        selected = cardId == "licenses",
                        icon = { Icon(Icons.Default.Description, null) },
                        onClick = { setCard("licenses") }
                    )

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    ) {

        val isDark = isSystemInDarkTheme()

        val topBarFg = if (isDark) Color.White
        else MaterialTheme.colorScheme.onSurface
        val gradient = Brush.verticalGradient(
            colors = if (isDark) {
                listOf(
                    Color(0xFF2B2924),                     // fully solid
                    Color(0xFF2B2924),                     // keep solid longer
                    Color(0xFF2E2C27),                     // still solid
                    Color(0xFF2E2C27).copy(alpha = 0.95f),
                    Color(0xFF2E2C27).copy(alpha = 0.90f),
                    Color(0xFF2E2C27).copy(alpha = 0.80f),
                    Color(0xFF2E2C27).copy(alpha = 0.70f),
                    Color(0xFF2E2C27).copy(alpha = 0.60f),
                    Color.Transparent
                )
            } else {
                listOf(
                    MaterialTheme.colorScheme.surface,
                    MaterialTheme.colorScheme.surface,
                    MaterialTheme.colorScheme.surface,
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.70f),
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.50f),
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.25f),
                    Color.Transparent
                )
            }
        )
        Box(modifier = Modifier.fillMaxSize()) {

            // ===== FULLSCREEN CONTENT =====
            when (cardId) {

                "privacy_policy" -> {
                    PrivacyPolicyScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 90.dp)
                    )
                }

                "licenses" -> {
                    LicensesScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 90.dp)
                    )
                }

                else -> {
                    key(cardId) {
                        WebCardContent(
                            modifier = Modifier.fillMaxSize(),
                            cardId = cardId
                        )
                    }
                }
            }

            // ===== OVERLAY GRADIENT TOP BAR =====
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(gradient)
                    .align(Alignment.TopCenter)
            ) {

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
                            Icon(Icons.Default.Menu, null, tint = topBarFg)
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                if (returnHome) onExitToHome()
                                else onExitNormal()
                            }
                        ) {
                            Icon(Icons.Default.Close, null, tint = topBarFg)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    )
                )
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
    val baseWebColor = if (isDark) Color(0xFF2B2924) else Color(0xFFF5F3EF)
    val url = remember(cardId) { urlForCard(cardId) }

    var progress by remember(url) { mutableIntStateOf(0) }
    var showError by remember(url) { mutableStateOf(false) }
    var reloadTick by remember(url) { mutableIntStateOf(0) }

    val adHosts = remember { listOf("doubleclick.net", "googlesyndication.com") }

    var hasMainFrameError by remember(url) { mutableStateOf(false) }

// Create ONE WebView and remember it
    val webView = remember {

        WebView(context).apply {

            setLayerType(View.LAYER_TYPE_HARDWARE, null)
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
                                v.parent?.requestDisallowInterceptTouchEvent(true)
                            } else {
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

                    // Hide page immediately before it paints
                    view?.alpha = 0f
                    view?.animate()?.cancel()

                    view?.animate()
                        ?.alpha(0f)
                        ?.translationX(-30f)
                        ?.scaleX(0.98f)
                        ?.scaleY(0.98f)
                        ?.alpha(0f)
                        ?.translationX(-30f)
                        ?.setDuration(180)
                        ?.setInterpolator(android.view.animation.AccelerateDecelerateInterpolator())
                        ?.start()

                    super.onPageStarted(view, u, favicon)
                }

                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest
                ): WebResourceResponse? {

                    val u = request.url.toString()

                    if (u.contains("scripts.min.js") ||
                        u.contains("trigger") ||
                        u.contains("footer")
                    ) {
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


                override fun onPageFinished(view: WebView?, url: String?) {

                    val isFlightsMain =
                        url?.startsWith("https://www.jacksonholeairport.com/flights/") == true &&
                                url.endsWith("/flights/")

                    injectHideTriggers(view, cardId == "card3", isFlightsMain)

                    if (!hasMainFrameError) {

                        view?.animate()?.cancel()

                        // Initial state (before animation)
                        view?.scaleX = 1.02f
                        view?.scaleY = 1.02f
                        view?.translationX = 40f
                        view?.alpha = 0f

                        // Animate to natural state
                        view?.animate()
                            ?.alpha(1f)
                            ?.translationX(0f)
                            ?.scaleX(1f)
                            ?.scaleY(1f)
                            ?.setDuration(300)
                            ?.setInterpolator(
                                android.view.animation.PathInterpolator(0.4f, 0f, 0.2f, 1f)
                            )
                            ?.start()
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
                        view.alpha = 0f
                    }
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    progress = newProgress
                }
            }
        }
    }

    Box(modifier) {

        AndroidView(
            modifier = Modifier
                .background(baseWebColor)
                .fillMaxSize()
                .nestedScroll(rememberNestedScrollInteropConnection()),
            factory = { webView },
            update = { wv ->

                wv.setBackgroundColor(baseWebColor.toArgb())
                applyWebViewDarkMode(wv.settings, isDark)

                // Only load first time
                if (wv.url == null) {
                    wv.loadUrl(url)
                }

                if (reloadTick > 0) {
                    progress = 0
                    showError = false
                    wv.reload()
                    reloadTick = 0
                }
            }
        )

        DisposableEffect(Unit) {
            onDispose {
                webView.stopLoading()
            }
        }

        // ---------------- PROGRESS ----------------

        val target = (progress.coerceIn(0, 100) / 100f)
        val displayTarget = if (target >= 0.99f) 1f else minOf(target, 0.95f)

        val animatedProgress by animateFloatAsState(
            targetValue = displayTarget,
            animationSpec = tween(
                durationMillis = if (target >= 0.99f) 300 else 220,
                easing = FastOutSlowInEasing
            ),
            label = "webProgress"
        )

        val progressAlpha by animateFloatAsState(
            targetValue = if (progress >= 100) 0f else 1f,
            animationSpec = tween(300),
            label = "progressFade"
        )

        if (!showError && progressAlpha > 0f) {

            val infinite = rememberInfiniteTransition(label = "wave")

            val offset by infinite.animateFloat(
                initialValue = -400f,
                targetValue = 1200f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2200, easing = LinearEasing)
                ),
                label = "offset"
            )

            val baseColor =
                if (isDark)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)

            val waveBrush = Brush.linearGradient(
                colors = listOf(
                    baseColor.copy(alpha = 0.15f),
                    baseColor.copy(alpha = 0.35f),
                    baseColor.copy(alpha = 0.75f),
                    baseColor.copy(alpha = 1f),
                    baseColor.copy(alpha = 0.75f),
                    baseColor.copy(alpha = 0.35f),
                    baseColor.copy(alpha = 0.15f)
                ),
                start = Offset(offset, 0f),
                end = Offset(offset + 300f, 0f)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 92.dp)
                    .graphicsLayer { alpha = progressAlpha }
                    .zIndex(50f)
            ) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(70))
                        .background(
                            if (isDark)
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                ) {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(50))
                            .background(waveBrush)
                    )
                }
            }
        }

        // ---------------- ERROR ----------------

        if (showError) {

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
                        style = MaterialTheme.typography.titleMedium
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
            }
        }
    }}

private fun urlForCard(cardId: String): String =
    when (cardId) {
        "card2" -> "https://www.jacksonholeairport.com/about/news/"
        "card3" -> "https://www.jacksonholeairport.com/flights/"
        "card4" -> "https://www.jacksonholeflightservices.com/"
        "about_us" -> "https://www.jacksonholeairport.com/about/"
        "contact_us" -> "https://www.jacksonholeairport.com/about/contact/"
        else -> "https://www.jacksonholeairport.com/"
    }




