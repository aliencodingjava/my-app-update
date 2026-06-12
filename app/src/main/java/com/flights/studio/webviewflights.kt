package com.flights.studio


import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
import android.webkit.JavascriptInterface
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import com.flights.studio.FlightsTabsInjector.injectHideTriggers
import com.flights.studio.SettingsStore.prefs
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.abs

@Composable
private fun SystemBarsSync() {

}
fun hasInternet(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

private fun isWebCard(cardId: String): Boolean =
    cardId == "card1" ||
        cardId == "card2" ||
        cardId == "card3" ||
        cardId == "card4" ||
        cardId == "about_us" ||
        cardId == "contact_us"

private fun webCardOrFlights(cardId: String): String =
    if (isWebCard(cardId)) cardId else "card3"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebviewFlights(
    startCardId: String,
    returnHome: Boolean,
    onExitToHome: () -> Unit,
    onExitNormal: () -> Unit,
    onOpenWelcome: () -> Unit,
    backdrop: LayerBackdrop,
    ) {
    SystemBarsSync()
    val scope = rememberCoroutineScope()
    var screenVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        screenVisible = true
    }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var cardId by rememberSaveable { mutableStateOf(startCardId) }
    var activeWebCardId by rememberSaveable { mutableStateOf(webCardOrFlights(startCardId)) }
    val webBlurTint = if (isSystemInDarkTheme()) Color(0xFF2B2924) else Color(0xFFF4F1E9)


    LaunchedEffect(cardId) {
        if (cardId == "card1") onOpenWelcome()
    }
    val context = LocalContext.current
    val online = hasInternet(context)

    val screenTitle = when (cardId) {
        "card1" -> "Welcome"
        "card2" -> "News"
        "card3" -> "Flights"
        "card4" -> "FBO"
        "settings" -> "Web Settings"
        "about_us" -> "About Us"
        "contact_us" -> "Contact Us"
        "privacy_policy" -> "Privacy Policy"
        "licenses" -> "Licenses"
        else -> "Flight Tracker"
    }
    var previousCard by remember { mutableStateOf(startCardId) }

    fun setCard(id: String) {
        previousCard = cardId
        cardId = id
        if (isWebCard(id)) {
            activeWebCardId = id
        }
        scope.launch { drawerState.close() }
    }

    fun exitToMainApp() {
        scope.launch { drawerState.close() }
        if (returnHome) onExitToHome() else onExitNormal()
    }

    BackHandler(enabled = cardId != startCardId) {
        cardId = previousCard
        if (isWebCard(previousCard)) {
            activeWebCardId = previousCard
        }
    }


    DismissibleNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = false,
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

                    HorizontalDivider(Modifier.padding(vertical = 12.dp))

                    NavigationDrawerItem(
                        label = { Text("Web Settings") },
                        selected = cardId == "settings",
                        icon = { Icon(Icons.Default.Settings, null) },
                        onClick = { setCard("settings") }
                    )

                    NavigationDrawerItem(
                        label = { Text("Main app") },
                        selected = false,
                        icon = { Icon(Icons.Default.Home, null) },
                        onClick = { exitToMainApp() }
                    )

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    ) {

        val alpha by animateFloatAsState(
            targetValue = if (screenVisible) 1f else 0f,
            animationSpec = if (online)
                tween(durationMillis = 0)
            else
                tween(durationMillis = 300, easing = FastOutSlowInEasing),
            label = "screenAlpha"
        )

        val offsetX by animateFloatAsState(
            targetValue = if (screenVisible) 0f else 40f,
            animationSpec = if (online)
                tween(durationMillis = 0)
            else
                tween(durationMillis = 300, easing = FastOutSlowInEasing),
            label = "screenOffset"
        )

        val scale by animateFloatAsState(
            targetValue = if (screenVisible) 1f else 1.04f,
            animationSpec = if (online)
                tween(durationMillis = 0)
            else
                tween(durationMillis = 300, easing = FastOutSlowInEasing),
            label = "screenScale"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    this.alpha = alpha
                    translationX = offsetX
                    scaleX = scale
                    scaleY = scale
                }
        ) {

            WebCardContent(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop)
                    .graphicsLayer {
                        this.alpha = if (isWebCard(cardId)) 1f else 0f
                    }
                    .zIndex(0f),
                cardId = activeWebCardId,
            )

            // ===== FULLSCREEN OVERLAYS =====
            when (cardId) {

                "settings" -> {
                    SettingsScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .layerBackdrop(backdrop)
                            .padding(top = 0.dp)
                            .zIndex(1f)
                    )
                }

                "privacy_policy" -> {
                    PrivacyPolicyScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .layerBackdrop(backdrop)
                            .zIndex(1f)
                    )
                }

                "licenses" -> {
                    LicensesScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .layerBackdrop(backdrop)
                            .zIndex(1f)
                    )
                }
            }

            if (cardId != "card3" && cardId != "privacy_policy" && cardId != "licenses") {
                BackdropGradientLayer(
                    backdrop = backdrop,
                    height = 76.dp,
                    blurDp = 4.dp,
                    tintColor = webBlurTint,
                    tintIntensity = 0.62f,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .zIndex(0.5f)
                )
            }

            WebViewSettingsStyleTopAppBar(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(3f),
                backdrop = backdrop,
                title = screenTitle,
                onNavigationClick = { scope.launch { drawerState.open() } }
            )
        }
    }
}

@Composable
private fun WebViewSettingsStyleTopAppBar(
    backdrop: LayerBackdrop,
    title: String,
    onNavigationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val topBarShape = RoundedCornerShape(0.dp)
    val barColor = topActionBarTint()
    val contentColor = if (isDark) Color.White else Color(0xFF111111)

    Surface(
        shape = topBarShape,
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { topBarShape },
                shadow = null,
                highlight = null,
                effects = {
                    blur(
                        radius = TopActionBarBlurDp.dp.toPx(),
                        edgeTreatment = TileMode.Mirror
                    )
                },
                onDrawSurface = { drawRect(barColor) }
            )
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .height(64.dp)
                .padding(start = 8.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigationClick) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "Open navigation drawer",
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = title,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 2.dp, end = 12.dp),
                color = contentColor,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1
            )
        }
    }
}

@Composable
fun LegalHtmlScreen(
    html: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val textColor = MaterialTheme.colorScheme.onBackground

    Box(
        modifier = modifier.fillMaxSize()
    ) {

        // Same background as Settings screen
        ProfileBackdropImageLayer(
            modifier = Modifier.matchParentSize(),
            lightRes = R.drawable.light_grid_pattern,
            darkRes = R.drawable.dark_grid_pattern,
            imageAlpha = if (isDark) 1f else 0.8f,
            scrimDark = 0f,
            scrimLight = 0f
        )

        AndroidView(
            modifier = Modifier.fillMaxSize(),
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

                    setPadding(40, 430, 40, 120)
                }

                scrollView.addView(textView)
                scrollView
            }
        )
    }
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


internal fun flightTableRuntimeCss(
    theme: String,
    textZoom: Int,
    previewFrame: Boolean = false
): String {
    val safeTheme = theme.filter { it.isLetterOrDigit() || it == '-' || it == '_' }.ifBlank { "mint" }
    val scale = textZoom.coerceIn(60, 100) / 100f
    val frameCss = if (previewFrame) {
        """
        html, body {
          margin: 0 !important;
          padding: 0 !important;
          width: 100% !important;
          min-height: 100% !important;
          overflow: hidden !important;
          background: transparent !important;
        }
        body {
          font-family: frutigerroman, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif !important;
        }
        #flight-container {
          padding: 0 !important;
          min-height: 100% !important;
          height: 100% !important;
        }
        #flight-container .flight-table-wrap,
        #flight-container .table-scroll {
          width: 100% !important;
          margin: 0 !important;
          padding: 0 !important;
          background: transparent !important;
        }
        html.fs-settings-preview #flight-container .jha-flights .status span {
          width: calc(46px * var(--flight-text-scale, 1)) !important;
          min-width: calc(46px * var(--flight-text-scale, 1)) !important;
          max-width: calc(46px * var(--flight-text-scale, 1)) !important;
          height: calc(20px * var(--flight-text-scale, 1)) !important;
          min-height: calc(20px * var(--flight-text-scale, 1)) !important;
          box-sizing: border-box !important;
          padding: 0 calc(4px * var(--flight-text-scale, 1)) !important;
          font-size: calc(8.6px * var(--flight-text-scale, 1)) !important;
          line-height: 1 !important;
          overflow: hidden !important;
          text-overflow: ellipsis !important;
          white-space: nowrap !important;
        }
        """.trimIndent()
    } else {
        ""
    }

    return """
        #flight-container {
          --flight-text-scale: $scale;
        }
        html.fs-theme-light #flight-container {
          --fs-table-page:#f8faff; --fs-table-card:#ffffff; --fs-table-head:#f0f4fa; --fs-table-date:#eef4ff;
          --fs-table-row:#ffffff; --fs-table-text:#111827; --fs-table-muted:#687283;
          --fs-table-border:rgba(15,23,42,0.09); --fs-row-arrived-bg:#eaf8f0;
          --fs-status-good:#056b34; --fs-status-good-bg:#c6f3d4; --fs-status-good-border:rgba(22,163,74,0.36);
          --fs-status-neutral:#263346; --fs-status-neutral-bg:#edf4fb; --fs-status-neutral-border:#b8c8d9;
          --fs-time-chip-text:#173b63; --fs-time-chip-bg:#e3f1ff; --fs-time-chip-border:#8fb8e3;
        }
        html.fs-theme-mint #flight-container {
          --fs-table-page:#f2fbf8; --fs-table-card:#ffffff; --fs-table-head:#eaf8f3; --fs-table-date:#e4f7f0;
          --fs-table-row:#fbfffd; --fs-table-text:#10201c; --fs-table-muted:#5d706a;
          --fs-table-border:rgba(34,185,129,0.18); --fs-row-arrived-bg:#e3f8ee;
          --fs-status-good:#057a45; --fs-status-good-bg:#c9f5df; --fs-status-good-border:rgba(34,185,129,0.36);
          --fs-status-neutral:#21453d; --fs-status-neutral-bg:#e5f5ef; --fs-status-neutral-border:#a9d7c8;
          --fs-time-chip-text:#105846; --fs-time-chip-bg:#daf6ec; --fs-time-chip-border:#7bc7af;
        }
        html.fs-theme-sky #flight-container {
          --fs-table-page:#f0f7ff; --fs-table-card:#ffffff; --fs-table-head:#e1eeff; --fs-table-date:#d8eaff;
          --fs-table-row:#f8fbff; --fs-table-text:#10243f; --fs-table-muted:#55708f;
          --fs-table-border:rgba(59,130,246,0.20); --fs-row-arrived-bg:#e4f6ff;
          --fs-status-good:#075985; --fs-status-good-bg:#d8f1ff; --fs-status-good-border:rgba(14,165,233,0.34);
          --fs-status-neutral:#1d4f7a; --fs-status-neutral-bg:#e4f0ff; --fs-status-neutral-border:#a8caef;
          --fs-time-chip-text:#174d7d; --fs-time-chip-bg:#dceeff; --fs-time-chip-border:#83b4e7;
        }
        html.fs-theme-ocean #flight-container {
          --fs-table-page:#071820; --fs-table-card:#0d2430; --fs-table-head:#133443; --fs-table-date:#0b2c3b;
          --fs-table-row:#0b202b; --fs-table-text:#e8fbff; --fs-table-muted:#91b5c0;
          --fs-table-border:rgba(125,211,252,0.16); --fs-row-arrived-bg:#0b3a36;
          --fs-status-good:#b4fff1; --fs-status-good-bg:#126055; --fs-status-good-border:rgba(45,212,191,0.38);
          --fs-status-neutral:#e9fbff; --fs-status-neutral-bg:#173242; --fs-status-neutral-border:#3d697a;
          --fs-time-chip-text:#d8fbff; --fs-time-chip-bg:#12384a; --fs-time-chip-border:#2c7189;
        }
        html.fs-theme-violet #flight-container {
          --fs-table-page:#f7f3ff; --fs-table-card:#ffffff; --fs-table-head:#ede7ff; --fs-table-date:#e9ddff;
          --fs-table-row:#fffcff; --fs-table-text:#261b3f; --fs-table-muted:#6e6188;
          --fs-table-border:rgba(139,92,246,0.20); --fs-row-arrived-bg:#edebff;
          --fs-status-good:#5b21b6; --fs-status-good-bg:#e8ddff; --fs-status-good-border:rgba(139,92,246,0.34);
          --fs-status-neutral:#39275f; --fs-status-neutral-bg:#f0eaff; --fs-status-neutral-border:#cbbcf1;
          --fs-time-chip-text:#4b2a86; --fs-time-chip-bg:#eee6ff; --fs-time-chip-border:#b9a0ee;
        }
        html.fs-theme-rose #flight-container {
          --fs-table-page:#fff5fa; --fs-table-card:#ffffff; --fs-table-head:#fce7f3; --fs-table-date:#fbd8eb;
          --fs-table-row:#fffbfd; --fs-table-text:#3e122a; --fs-table-muted:#856174;
          --fs-table-border:rgba(236,72,153,0.18); --fs-row-arrived-bg:#ffe8f1;
          --fs-status-good:#9d174d; --fs-status-good-bg:#ffdce9; --fs-status-good-border:rgba(236,72,153,0.30);
          --fs-status-neutral:#662846; --fs-status-neutral-bg:#fff0f6; --fs-status-neutral-border:#efb8d2;
          --fs-time-chip-text:#7a234e; --fs-time-chip-bg:#ffe6f1; --fs-time-chip-border:#e99abd;
        }
        html.fs-theme-amber #flight-container {
          --fs-table-page:#fffaec; --fs-table-card:#ffffff; --fs-table-head:#fff0c7; --fs-table-date:#ffe9aa;
          --fs-table-row:#fffcf4; --fs-table-text:#34230c; --fs-table-muted:#7e6741;
          --fs-table-border:rgba(245,158,11,0.22); --fs-row-arrived-bg:#fff4d9;
          --fs-status-good:#92500a; --fs-status-good-bg:#ffe7ae; --fs-status-good-border:rgba(245,158,11,0.34);
          --fs-status-neutral:#5b3b0d; --fs-status-neutral-bg:#fff2cc; --fs-status-neutral-border:#e2bd68;
          --fs-time-chip-text:#71470c; --fs-time-chip-bg:#ffedbf; --fs-time-chip-border:#dca94f;
        }
        html.fs-theme-gray #flight-container {
          --fs-table-page:#f4f6f8; --fs-table-card:#ffffff; --fs-table-head:#eceff3; --fs-table-date:#e7ebf0;
          --fs-table-row:#fdfdfe; --fs-table-text:#1f2937; --fs-table-muted:#6b7280;
          --fs-table-border:rgba(100,116,139,0.18); --fs-row-arrived-bg:#e9eef2;
          --fs-status-good:#475569; --fs-status-good-bg:#e2e8f0; --fs-status-good-border:rgba(100,116,139,0.28);
          --fs-status-neutral:#263346; --fs-status-neutral-bg:#edf2f7; --fs-status-neutral-border:#bdc7d3;
          --fs-time-chip-text:#334155; --fs-time-chip-bg:#e9f0f8; --fs-time-chip-border:#aab8c8;
        }
        html.fs-theme-dark #flight-container {
          --fs-table-page:#07111c; --fs-table-card:#111c28; --fs-table-head:#172433; --fs-table-date:#0d1726;
          --fs-table-row:#101b27; --fs-table-text:#eaf2f8; --fs-table-muted:#91a0ae;
          --fs-table-border:rgba(255,255,255,0.09); --fs-row-arrived-bg:#0d332c;
          --fs-status-good:#a4ffc5; --fs-status-good-bg:#115f34; --fs-status-good-border:rgba(87,255,151,0.44);
          --fs-status-neutral:#f1f7ff; --fs-status-neutral-bg:#243142; --fs-status-neutral-border:#526173;
          --fs-time-chip-text:#d7ecff; --fs-time-chip-bg:#183249; --fs-time-chip-border:#315f87;
        }
        html.fs-theme-auto #flight-container {
          --fs-table-page:#f8faff; --fs-table-card:#ffffff; --fs-table-head:#f0f4fa; --fs-table-date:#eef4ff;
          --fs-table-row:#ffffff; --fs-table-text:#111827; --fs-table-muted:#687283;
          --fs-table-border:rgba(15,23,42,0.09); --fs-row-arrived-bg:#eaf8f0;
          --fs-status-good:#056b34; --fs-status-good-bg:#c6f3d4; --fs-status-good-border:rgba(22,163,74,0.36);
          --fs-status-neutral:#263346; --fs-status-neutral-bg:#edf4fb; --fs-status-neutral-border:#b8c8d9;
          --fs-time-chip-text:#173b63; --fs-time-chip-bg:#e3f1ff; --fs-time-chip-border:#8fb8e3;
        }
        @media (prefers-color-scheme: dark) {
          html.fs-theme-auto #flight-container {
            --fs-table-page:#07111c; --fs-table-card:#111c28; --fs-table-head:#172433; --fs-table-date:#0d1726;
            --fs-table-row:#101b27; --fs-table-text:#eaf2f8; --fs-table-muted:#91a0ae;
            --fs-table-border:rgba(255,255,255,0.09); --fs-row-arrived-bg:#0d332c;
            --fs-status-good:#a4ffc5; --fs-status-good-bg:#115f34; --fs-status-good-border:rgba(87,255,151,0.44);
            --fs-status-neutral:#f1f7ff; --fs-status-neutral-bg:#243142; --fs-status-neutral-border:#526173;
            --fs-time-chip-text:#d7ecff; --fs-time-chip-bg:#183249; --fs-time-chip-border:#315f87;
          }
        }
        html.fs-theme-$safeTheme #flight-container {
          --flight-text-scale: $scale;
        }
        html.fs-grouped-flights #flight-container .jha-flights tr.fs-airline-group-row td {
          height: auto !important;
          padding: calc(12px * var(--flight-text-scale, 1)) calc(14px * var(--flight-text-scale, 1)) !important;
          background: linear-gradient(135deg, color-mix(in srgb, var(--fs-table-head) 82%, var(--fs-table-card)), var(--fs-table-card)) !important;
          color: var(--fs-table-text) !important;
          border-top: 1px solid var(--fs-table-border) !important;
        }
        html.fs-grouped-flights #flight-container .fs-airline-group-label {
          display: flex !important;
          align-items: center !important;
          justify-content: space-between !important;
          gap: calc(12px * var(--flight-text-scale, 1)) !important;
          font-weight: 950 !important;
          letter-spacing: .035em !important;
        }
        html.fs-grouped-flights #flight-container .fs-airline-group-meta {
          display: inline-flex !important;
          align-items: center !important;
          gap: calc(8px * var(--flight-text-scale, 1)) !important;
          color: var(--fs-table-muted) !important;
          font-size: calc(11px * var(--flight-text-scale, 1)) !important;
          font-weight: 850 !important;
          white-space: nowrap !important;
        }
        html.fs-web-high-contrast #flight-container .jha-flights {
          -webkit-font-smoothing: antialiased !important;
          text-rendering: geometricPrecision !important;
        }
        html.fs-web-reduce-motion *, html.fs-web-reduce-motion *::before, html.fs-web-reduce-motion *::after {
          animation-duration: 0.001ms !important;
          animation-iteration-count: 1 !important;
          transition-duration: 0.001ms !important;
          scroll-behavior: auto !important;
        }
        $frameCss
    """.trimIndent()
}

private fun String.toJavaScriptSingleQuotedString(): String {
    return replace("\\", "\\\\")
        .replace("'", "\\'")
        .replace("\r", "")
        .replace("\n", "\\n")
}

private fun injectWebRuntimePreferences(
    view: WebView?,
    theme: String,
    textZoom: Int,
    enhancedTable: Boolean,
    groupedFlights: Boolean,
    highContrast: Boolean,
    reduceMotion: Boolean
) {
    val safeTheme = theme.filter { it.isLetterOrDigit() || it == '-' || it == '_' }.ifBlank { "mint" }
    val safeZoom = textZoom.coerceIn(60, 100)
    val textScale = safeZoom / 100f
    val runtimeCss = flightTableRuntimeCss(
        theme = safeTheme,
        textZoom = safeZoom
    ).toJavaScriptSingleQuotedString()
    val js = """
        (function() {
          var root = document.documentElement;
          var body = document.body;
          var theme = '$safeTheme';
          root.style.setProperty('--flight-text-scale', '$textScale');
          root.classList.remove('fs-theme-light','fs-theme-mint','fs-theme-sky','fs-theme-ocean','fs-theme-violet','fs-theme-rose','fs-theme-amber','fs-theme-gray','fs-theme-dark','fs-theme-auto');
          root.classList.add('fs-theme-' + theme);
          if (body) {
            body.classList.remove('fs-theme-light','fs-theme-mint','fs-theme-sky','fs-theme-ocean','fs-theme-violet','fs-theme-rose','fs-theme-amber','fs-theme-gray','fs-theme-dark','fs-theme-auto');
            body.classList.add('fs-theme-' + theme);
          }
          root.classList.toggle('fs-enhanced-table', $enhancedTable);
          root.classList.toggle('fs-grouped-flights', $groupedFlights);
          root.classList.toggle('fs-web-high-contrast', $highContrast);
          root.classList.toggle('fs-web-reduce-motion', $reduceMotion);
          if (!document.head) return;
          var style = document.getElementById('fs_web_runtime_prefs');
          if (!style) {
            style = document.createElement('style');
            style.id = 'fs_web_runtime_prefs';
            document.head.appendChild(style);
          }
          style.textContent = '$runtimeCss';
          function cleanText(el) {
            return (el && el.textContent ? el.textContent : '').replace(/\s+/g, ' ').trim();
          }
          function airlineName(row) {
            var cell = row.querySelector('td.airline, .airline');
            var value = cleanText(cell);
            if (!value) return 'Other';
            var upper = value.toUpperCase();
            var map = {
              'UA': 'United', 'UAL': 'United', 'UNITED': 'United',
              'AA': 'American', 'AAL': 'American', 'AMERICAN': 'American',
              'DL': 'Delta', 'DAL': 'Delta', 'DELTA': 'Delta',
              'AS': 'Alaska', 'ASA': 'Alaska', 'ALASKA': 'Alaska',
              'B6': 'JetBlue', 'JBU': 'JetBlue', 'JETBLUE': 'JetBlue',
              'WN': 'Southwest', 'SWA': 'Southwest', 'SOUTHWEST': 'Southwest'
            };
            return map[upper] || value;
          }
          function parseMinutes(value) {
            var s = String(value || '').toLowerCase().trim();
            var m = s.match(/(\d{1,2})\s*:\s*(\d{2})\s*([ap])\.?m?\.?/i);
            if (!m) return 99999;
            var h = parseInt(m[1], 10);
            var min = parseInt(m[2], 10);
            if (m[3].toLowerCase() === 'p' && h !== 12) h += 12;
            if (m[3].toLowerCase() === 'a' && h === 12) h = 0;
            return h * 60 + min;
          }
          function rowTime(row) {
            return parseMinutes(cleanText(row.querySelector('td.sched, .sched')) || cleanText(row.querySelector('td.actual, .actual')));
          }
          function isFlightRow(row) {
            return row && !row.classList.contains('fs-airline-group-row') && !row.querySelector('.day') && !row.querySelector('th') && row.querySelector('td.airline,td.flight,.airline,.flight');
          }
          function isDayRow(row) {
            return row && !row.classList.contains('fs-airline-group-row') && row.querySelector('.day');
          }
          function originalOrder(row, fallback) {
            var value = parseInt(row.dataset.fsOriginalIndex || '', 10);
            return isNaN(value) ? fallback : value;
          }
          function ensureOriginalIndexes(body) {
            Array.prototype.forEach.call(body.children, function(row, index) {
              if (!row.classList.contains('fs-airline-group-row') && !row.dataset.fsOriginalIndex) {
                row.dataset.fsOriginalIndex = String(index);
              }
            });
            body.dataset.fsIndexed = 'true';
          }
          function tableColSpan(table, sampleRow) {
            return Math.max(1, table.querySelectorAll('thead th').length || (sampleRow && sampleRow.children.length) || 6);
          }
          function createAirlineGroupRow(table, group) {
            var header = document.createElement('tr');
            header.className = 'fs-airline-group-row';
            var td = document.createElement('td');
            td.colSpan = tableColSpan(table, group.rows[0]);
            var label = document.createElement('div');
            label.className = 'fs-airline-group-label';
            var name = document.createElement('span');
            name.textContent = group.name;
            var meta = document.createElement('span');
            meta.className = 'fs-airline-group-meta';
            var firstTime = cleanText(group.rows[0].querySelector('td.sched, .sched')) || cleanText(group.rows[0].querySelector('td.actual, .actual'));
            meta.textContent = group.rows.length + ' flight' + (group.rows.length === 1 ? '' : 's') + (firstTime ? ' • first ' + firstTime : '');
            label.appendChild(name);
            label.appendChild(meta);
            td.appendChild(label);
            header.appendChild(td);
            return header;
          }
          function appendGroupedRowsForSection(body, table, rows) {
            var groups = {};
            rows.forEach(function(row) {
              var name = airlineName(row);
              if (!groups[name]) groups[name] = [];
              groups[name].push(row);
            });
            Object.keys(groups).map(function(name) {
              var items = groups[name].slice().sort(function(a, b) { return rowTime(a) - rowTime(b); });
              return { name: name, rows: items, first: rowTime(items[0]) };
            }).sort(function(a, b) {
              if (a.first !== b.first) return a.first - b.first;
              return a.name.localeCompare(b.name);
            }).forEach(function(group) {
              body.appendChild(createAirlineGroupRow(table, group));
              group.rows.forEach(function(row) {
                row.style.display = '';
                body.appendChild(row);
              });
            });
          }
          function restoreFlightOrder(table) {
            var body = table && table.tBodies && table.tBodies[0];
            if (!body) return;
            body.querySelectorAll('tr.fs-airline-group-row').forEach(function(row) { row.remove(); });
            Array.prototype.forEach.call(body.children, function(row) { row.style.display = ''; });
            Array.prototype.slice.call(body.children).sort(function(a, b) {
              return originalOrder(a, 0) - originalOrder(b, 0);
            }).forEach(function(row) { body.appendChild(row); });
            body.dataset.fsGroupedApplied = 'false';
          }
          function groupFlightTable(table) {
            var body = table && table.tBodies && table.tBodies[0];
            if (!body || window.fsGroupingBusy) return;
            window.fsGroupingBusy = true;
            try {
              body.querySelectorAll('tr.fs-airline-group-row').forEach(function(row) { row.remove(); });
              ensureOriginalIndexes(body);
              var originalRows = Array.prototype.slice.call(body.children)
                .filter(function(row) { return !row.classList.contains('fs-airline-group-row'); })
                .sort(function(a, b) { return originalOrder(a, 0) - originalOrder(b, 0); });
              if (!originalRows.some(isFlightRow)) {
                body.dataset.fsGroupedApplied = 'false';
                return;
              }
              originalRows.forEach(function(row) { row.style.display = ''; });
              var prelude = [];
              var sections = [];
              var current = null;
              originalRows.forEach(function(row) {
                if (isDayRow(row)) {
                  if (current) sections.push(current);
                  current = { day: row, extras: [], rows: [] };
                } else if (isFlightRow(row)) {
                  if (!current) current = { day: null, extras: [], rows: [] };
                  current.rows.push(row);
                } else {
                  if (current) current.extras.push(row);
                  else prelude.push(row);
                }
              });
              if (current) sections.push(current);
              prelude.forEach(function(row) { body.appendChild(row); });
              sections.forEach(function(section) {
                if (section.day) body.appendChild(section.day);
                section.extras.forEach(function(row) { body.appendChild(row); });
                appendGroupedRowsForSection(body, table, section.rows);
              });
              body.dataset.fsGroupedApplied = 'true';
            } finally {
              window.fsGroupingBusy = false;
            }
          }
          function applyGroupedFlights() {
            document.querySelectorAll('#flight-container table.jha-flights').forEach(function(table) {
              if ($groupedFlights) groupFlightTable(table);
              else restoreFlightOrder(table);
            });
          }
          clearTimeout(window.fsGroupedFlightsTimer);
          window.fsGroupedFlightsTimer = setTimeout(applyGroupedFlights, 60);
          if (window.fsGroupedFlightsObserver) window.fsGroupedFlightsObserver.disconnect();
          var con = document.getElementById('flight-container');
          if (con) {
            window.fsGroupedFlightsObserver = new MutationObserver(function() {
              if (window.fsGroupingBusy) return;
              clearTimeout(window.fsGroupedFlightsTimer);
              window.fsGroupedFlightsTimer = setTimeout(applyGroupedFlights, 160);
            });
            window.fsGroupedFlightsObserver.observe(con, { childList: true, subtree: true });
          }
        })();
    """.trimIndent()
    view?.evaluateJavascript(js, null)
}

private fun isExternalFlightTrackerUrl(url: String?): Boolean {
    val host = runCatching { url?.toUri()?.host.orEmpty().lowercase() }.getOrDefault("")
    return host == "flightradar24.com" ||
            host.endsWith(".flightradar24.com") ||
            host == "flightaware.com" ||
            host.endsWith(".flightaware.com")
}

private fun openExternalFlightTracker(context: Context, url: String): Boolean {
    return runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, url.toUri()).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
            }
        )
        true
    }.getOrDefault(false)
}

private fun webIntroAnimationKey(url: String?): String? {
    val cleaned = url
        ?.substringBefore('#')
        ?.substringBefore('?')
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: return null
    return cleaned.removeSuffix("/")
}

private fun proxiedFlightApiResponse(path: String): WebResourceResponse? {
    val target = when (path) {
        "/__fs_proxy/adsb_lol" ->
            "https://api.adsb.lol/v2/lat/43.6073/lon/-110.7377/dist/500"
        "/__fs_proxy/adsb_fi" ->
            "https://opendata.adsb.fi/api/v3/lat/43.6073/lon/-110.7377/dist/250"
        "/__fs_proxy/opensky" ->
            "https://opensky-network.org/api/states/all?lamin=30&lomin=-125&lamax=50&lomax=-88"
        else -> return null
    }

    val body = runCatching {
        val connection = (URL(target).openConnection() as HttpURLConnection).apply {
            connectTimeout = 7000
            readTimeout = 7000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "JHAirTracker/1.0 Android WebView")
        }
        try {
            val stream = if (connection.responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        } finally {
            connection.disconnect()
        }
    }.getOrElse { "{}" }

    return WebResourceResponse(
        "application/json",
        "UTF-8",
        ByteArrayInputStream(body.toByteArray())
    )
}

private class FlightBriefBridge(context: Context) {
    private val appContext = context.applicationContext

    @JavascriptInterface
    fun updateFlightBriefSnapshot(json: String) {
        SettingsStore.setFlightBriefSnapshot(appContext, json)
    }

    @JavascriptInterface
    fun updateWeatherSnapshot(json: String) {
        val taggedJson = runCatching {
            JSONObject(json).apply {
                put("source", optString("source").ifBlank { "airport_web" })
                if (!has("updatedAt")) put("updatedAt", System.currentTimeMillis())
            }.toString()
        }.getOrElse { json }
        SettingsStore.setBriefingWeatherSnapshot(appContext, taggedJson)
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
    val webPrefs = remember(context) { prefs(context) }
    var settingsRevision by remember(webPrefs) { mutableIntStateOf(0) }

    DisposableEffect(webPrefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            settingsRevision += 1
        }
        webPrefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            webPrefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    val enhancedTable = remember(settingsRevision) { SettingsStore.enhancedTable(context) }
    val groupedFlights = remember(settingsRevision) { SettingsStore.groupFlights(context) }
    val highContrastWeb = remember(settingsRevision) { SettingsStore.highContrastWeb(context) }
    val reduceWebMotion = remember(settingsRevision) { SettingsStore.reduceWebMotion(context) }
    val cachePages = remember(settingsRevision) { SettingsStore.cachePages(context) }
    val hwAccel = remember(settingsRevision) { SettingsStore.hardwareAccel(context) }
    val textZoomPref = remember(settingsRevision) { SettingsStore.textZoom(context) }
    val webTheme = remember(settingsRevision) { SettingsStore.webTheme(context) }
    val baseWebColor = if (isDark) Color(0xFF2B2924) else Color(0xFFF4F1E9)
    val url = remember(cardId) { urlForCard(cardId) }

    var progress by remember(url) { mutableIntStateOf(0) }
    var showError by remember(url) { mutableStateOf(false) }
    var reloadTick by remember(url) { mutableIntStateOf(0) }
    var loadedRootUrl by remember { mutableStateOf<String?>(null) }
    var animatedRootUrl by remember { mutableStateOf<String?>(null) }

    val adHosts = remember { listOf("doubleclick.net", "googlesyndication.com") }

    var hasMainFrameError by remember(url) { mutableStateOf(false) }



// Create ONE WebView and remember it
    val webView = remember {

        WebView(context).apply {
            setBackgroundColor(baseWebColor.toArgb())
            if (hwAccel) {
                setLayerType(View.LAYER_TYPE_HARDWARE, null)
            } else {
                setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            }

            alpha = 0f
            overScrollMode = WebView.OVER_SCROLL_IF_CONTENT_SCROLLS
            isVerticalScrollBarEnabled = true


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
                                // vertical scroll → WebView handles it
                                v.parent?.requestDisallowInterceptTouchEvent(true)

                            } else {

                                // horizontal gesture

                                // allow drawer ONLY near screen edge
                                if (startX < 60) {
                                    v.parent?.requestDisallowInterceptTouchEvent(false)
                                } else {
                                    // allow inner DOM scroll (weather banner)
                                    v.parent?.requestDisallowInterceptTouchEvent(true)
                                }
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

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                cacheMode = if (cachePages) WebSettings.LOAD_CACHE_ELSE_NETWORK else WebSettings.LOAD_DEFAULT
                textZoom = 100

                setSupportZoom(false)
                builtInZoomControls = false
                displayZoomControls = false
                useWideViewPort = true
                loadWithOverviewMode = true
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                allowFileAccess = false
                allowContentAccess = false
            }

            addJavascriptInterface(FlightBriefBridge(context), "FlightsAndroidBridge")


            webViewClient = object : WebViewClient() {

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest
                ): Boolean {
                    val targetUrl = request.url?.toString()
                    if (isExternalFlightTrackerUrl(targetUrl)) {
                        view?.evaluateJavascript(
                            "try{document.getElementById('fs-flight-detail-overlay')?.remove();document.documentElement.classList.remove('fs-flight-detail-open');}catch(e){}",
                            null
                        )
                        return targetUrl != null && openExternalFlightTracker(context, targetUrl)
                    }
                    return false
                }

                @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    if (isExternalFlightTrackerUrl(url)) {
                        view?.evaluateJavascript(
                            "try{document.getElementById('fs-flight-detail-overlay')?.remove();document.documentElement.classList.remove('fs-flight-detail-open');}catch(e){}",
                            null
                        )
                        return url != null && openExternalFlightTracker(context, url)
                    }
                    return false
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    showError = false
                    progress = 0
                    val animationKey = webIntroAnimationKey(url)
                    if (animationKey != null && animatedRootUrl != animationKey) {
                        view?.alpha = 0f
                    }
                    super.onPageStarted(view, url, favicon)
                }

                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest
                ): WebResourceResponse? {

                    val u = request.url.toString()
                    proxiedFlightApiResponse(request.url.path.orEmpty())?.let {
                        return it
                    }

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
                    if (SettingsStore.blockTrackers(context) && adHosts.any { host.contains(it) }) {
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

                    injectWebRuntimePreferences(
                        view,
                        SettingsStore.webTheme(context),
                        SettingsStore.textZoom(context),
                        SettingsStore.enhancedTable(context),
                        SettingsStore.groupFlights(context),
                        SettingsStore.highContrastWeb(context),
                        SettingsStore.reduceWebMotion(context)
                    )
                    injectHideTriggers(view, cardId == "card3", isFlightsMain)

                    if (!hasMainFrameError) {
                        val animationKey = webIntroAnimationKey(url)
                        val shouldAnimateIntro = animationKey != null && animatedRootUrl != animationKey

                        view?.animate()?.cancel()

                        if (shouldAnimateIntro) {
                            animatedRootUrl = animationKey

                            // Initial state (before animation)
                            view?.scaleX = 1.04f
                            view?.scaleY = 1.04f
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
                        } else {
                            view?.alpha = 1f
                            view?.translationX = 0f
                            view?.scaleX = 1f
                            view?.scaleY = 1f
                        }
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
                .fillMaxSize(),
            factory = { webView },
            update = { wv ->

                wv.setBackgroundColor(baseWebColor.toArgb())
                wv.setLayerType(
                    if (hwAccel) View.LAYER_TYPE_HARDWARE else View.LAYER_TYPE_SOFTWARE,
                    null
                )
                wv.settings.textZoom = 100
                wv.settings.cacheMode = if (cachePages) {
                    WebSettings.LOAD_CACHE_ELSE_NETWORK
                } else {
                    WebSettings.LOAD_DEFAULT
                }
                injectWebRuntimePreferences(wv, webTheme, textZoomPref, enhancedTable, groupedFlights, highContrastWeb, reduceWebMotion)

                if (loadedRootUrl != url) {

                    if (!hasInternet(context)) {
                        showError = true
                        return@AndroidView
                    }

                    progress = 0
                    showError = false
                    hasMainFrameError = false
                    loadedRootUrl = url
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
                                Color(0xFF2B2924).copy(alpha = 0.65f)
                            else
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
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
            ) {

                ProfileBackdropImageLayer(
                    modifier = Modifier.matchParentSize(),
                    lightRes = R.drawable.light_grid_pattern,
                    darkRes = R.drawable.dark_grid_pattern,
                    imageAlpha = if (isDark) 1f else 0.8f,
                    scrimDark = 0f,
                    scrimLight = 0f
                )

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
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
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

                            if (!hasInternet(context)) {
                                return@Button
                            }

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




