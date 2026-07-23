package com.flights.studio


import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.activity.compose.BackHandler
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.runtime.Stable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import com.flights.studio.FlightsTabsInjector.injectHideTriggers
import com.flights.studio.SettingsStore.prefs
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

internal val FlightArrivalLantern = Color(0xFFF70D1A)
internal val FlightDepartureLantern = Color(0xFF4D4DFF)
internal val FlightAlertLantern = Color(0xFF2FEF73)

internal fun flightLanternPanelColor(
    accent: Color,
    isDark: Boolean,
    glassAmount: Float
): Color {
    val amount = glassAmount.coerceIn(0f, 1f)
    return if (isDark) {
        accent.copy(alpha = 0.36f + 0.24f * amount)
            .compositeOver(Color(0xFF130004).copy(alpha = 0.34f))
    } else {
        accent.copy(alpha = 0.18f + 0.15f * amount)
            .compositeOver(Color.White.copy(alpha = 0.68f))
    }
}

internal fun flightLanternOverlayColor(
    accent: Color,
    isDark: Boolean,
    glassAmount: Float
): Color {
    val amount = glassAmount.coerceIn(0f, 1f)
    return if (isDark) {
        accent.copy(alpha = 0.24f + 0.22f * amount)
    } else {
        accent.copy(alpha = 0.10f + 0.14f * amount)
    }
}

internal fun flightLanternSheenBrush(
    accent: Color,
    isDark: Boolean,
    glassAmount: Float
): Brush {
    val amount = glassAmount.coerceIn(0f, 1f)
    val bright = if (isDark) 0.15f + 0.10f * amount else 0.20f + 0.08f * amount
    val glow = if (isDark) 0.18f + 0.10f * amount else 0.08f + 0.06f * amount
    return Brush.linearGradient(
        0.00f to Color.White.copy(alpha = bright),
        0.20f to accent.copy(alpha = glow),
        0.54f to Color.Transparent,
        0.82f to accent.copy(alpha = glow * 0.82f),
        1.00f to Color.Black.copy(alpha = if (isDark) 0.10f else 0.00f),
        start = Offset.Zero,
        end = Offset.Infinite
    )
}

internal fun flightLanternSheetPanelColor(
    accent: Color,
    isDark: Boolean,
    glassAmount: Float,
    intensity: Float = 1f
): Color {
    val amount = glassAmount.coerceIn(0f, 1f)
    val strength = intensity.coerceIn(0f, 1f)
    return if (isDark) {
        accent.copy(alpha = (0.10f + 0.07f * amount) * strength)
            .compositeOver(Color(0xFF090D14).copy(alpha = 0.62f + 0.18f * amount))
    } else {
        accent.copy(alpha = (0.06f + 0.06f * amount) * strength)
            .compositeOver(Color.White.copy(alpha = 0.76f + 0.12f * amount))
    }
}

internal fun flightLanternSheetOverlayColor(
    accent: Color,
    isDark: Boolean,
    glassAmount: Float,
    intensity: Float = 1f
): Color {
    val amount = glassAmount.coerceIn(0f, 1f)
    val strength = intensity.coerceIn(0f, 1f)
    return if (isDark) {
        accent.copy(alpha = (0.06f + 0.07f * amount) * strength)
    } else {
        accent.copy(alpha = (0.035f + 0.055f * amount) * strength)
    }
}

internal fun flightLanternSheetSheenBrush(
    accent: Color,
    isDark: Boolean,
    glassAmount: Float,
    intensity: Float = 1f
): Brush {
    val amount = glassAmount.coerceIn(0f, 1f)
    val strength = intensity.coerceIn(0f, 1f)
    return Brush.linearGradient(
        0.00f to Color.White.copy(alpha = if (isDark) 0.10f + 0.05f * amount else 0.14f + 0.04f * amount),
        0.24f to accent.copy(alpha = (if (isDark) 0.055f + 0.04f * amount else 0.035f + 0.03f * amount) * strength),
        0.66f to Color.Transparent,
        1.00f to Color.Black.copy(alpha = if (isDark) 0.06f else 0.00f),
        start = Offset.Zero,
        end = Offset.Infinite
    )
}

@Composable
private fun SystemBarsSync() {

}
fun hasInternet(context: Context): Boolean {
    return runCatching {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }.getOrDefault(false)
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
    val context = LocalContext.current
    val flightPageBackdrop = rememberLayerBackdrop()
    val flightMenuBackdrop = rememberLayerBackdrop()
    var screenVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        screenVisible = true
    }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var cardId by rememberSaveable { mutableStateOf(startCardId) }
    var activeWebCardId by rememberSaveable { mutableStateOf(webCardOrFlights(startCardId)) }
    var flightMainTabsVisible by remember { mutableStateOf(false) }
    var selectedFlightTab by rememberSaveable { mutableStateOf("arrivals") }
    var flightWebView by remember { mutableStateOf<WebView?>(null) }
    var showFlightAlertsSheet by remember { mutableStateOf(false) }
    var showFlightMenuSheet by remember { mutableStateOf(false) }
    var showFlightTableSheet by remember { mutableStateOf(false) }
    var flightTableMode by rememberSaveable { mutableStateOf("arrival") }
    var lastFlightContentTab by rememberSaveable { mutableStateOf("arrivals") }
    var flightTitleProgress by rememberSaveable { mutableStateOf(0f) }
    var liveStatusJson by rememberSaveable { mutableStateOf(SettingsStore.flightLiveStatusSnapshot(context)) }
    var flightBriefJson by rememberSaveable { mutableStateOf(SettingsStore.flightBriefSnapshot(context)) }
    var flightTableJson by rememberSaveable { mutableStateOf(SettingsStore.flightTableSnapshot(context)) }
    var weatherJson by rememberSaveable { mutableStateOf(SettingsStore.briefingWeatherSnapshot(context)) }
    fun selectedFlightTableTab(): String =
        if (flightTableMode == "departure") "departures" else "arrivals"
    fun selectedFlightContentTab(): String =
        if (cardId == "card3") lastFlightContentTab else selectedFlightTableTab()
    val webPrefs = remember(context) { prefs(context) }
    var nativeTableSettingsRevision by remember(webPrefs) { mutableIntStateOf(0) }
    DisposableEffect(webPrefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            nativeTableSettingsRevision += 1
        }
        webPrefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            webPrefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
    val nativeTableTheme = remember(nativeTableSettingsRevision) { SettingsStore.webTheme(context) }
    val nativeTableTextZoom = remember(nativeTableSettingsRevision) { SettingsStore.textZoom(context) }
    val nativeTableGrouped = remember(nativeTableSettingsRevision) { SettingsStore.groupFlights(context) }
    val nativeTableHighContrast = remember(nativeTableSettingsRevision) { SettingsStore.highContrastWeb(context) }
    val webBlurTint = if (isSystemInDarkTheme()) Color(0xFF2B2924) else Color(0xFFF4F1E9)

    LaunchedEffect(cardId) {
        if (cardId == "card3") {
            nativeTableSettingsRevision += 1
        }
    }

    LaunchedEffect(cardId) {
        if (cardId == "card1") onOpenWelcome()
    }
    val online = hasInternet(context)

    LaunchedEffect(cardId, flightWebView, online) {
        val webView = flightWebView ?: return@LaunchedEffect
        if (cardId != "card3" || !online) return@LaunchedEffect
        while (true) {
            webView.evaluateJavascript(
                "try{window.fsRefreshNativeLiveStatus&&window.fsRefreshNativeLiveStatus()}catch(e){}",
                null
            )
            delay(60_000L)
            webView.reload()
        }
    }

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
    val flightSectionTitle = when (selectedFlightContentTab()) {
        "departures" -> "Departures"
        "alerts" -> "Alerts"
        else -> "Arrivals"
    }
    var previousCard by remember { mutableStateOf(startCardId) }

    LaunchedEffect(cardId, selectedFlightTab, flightTableMode) {
        flightTitleProgress = 0f
    }

    fun setCard(id: String) {
        previousCard = cardId
        cardId = id
        if (id == "card3") {
            nativeTableSettingsRevision += 1
        }
        showFlightAlertsSheet = false
        showFlightTableSheet = false
        showFlightMenuSheet = false
        selectedFlightTab = if (id == "card3" && flightTableMode == "departure") {
            "departures"
        } else {
            "arrivals"
        }
        lastFlightContentTab = selectedFlightTab
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

    BackHandler(enabled = showFlightAlertsSheet || showFlightMenuSheet || showFlightTableSheet) {
        showFlightAlertsSheet = false
        showFlightMenuSheet = false
        showFlightTableSheet = false
        selectedFlightTab = selectedFlightContentTab()
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
            val flightTableSnapshot = remember(flightTableJson) { parseFlightTableSnapshot(flightTableJson) }
            val liveStatusSnapshot = remember(liveStatusJson) { parseFlightLiveStatusSnapshot(liveStatusJson) }
            val flightSheetBrief = remember(flightBriefJson) { parseFlightBriefSnapshotForSheet(flightBriefJson) }
            val flightSheetWeather = remember(weatherJson) { parseWeatherSnapshotForSheet(weatherJson) }
            val activeFlightBackdrop = if (cardId == "card3") flightPageBackdrop else backdrop
            val isDarkTheme = isSystemInDarkTheme()
            val flightMenuPanelColor = if (isDarkTheme) {
                Color(0xFF0B0F17).copy(alpha = 0.42f)
            } else {
                Color(0xFFFFF8F0).copy(alpha = 0.54f)
            }
            val flightMenuOverlayTint = if (isDarkTheme) {
                Color(0xFF283141).copy(alpha = 0.035f)
            } else {
                Color(0xFFFFF4E8).copy(alpha = 0.045f)
            }
            val flightMenuButtonColor = if (isDarkTheme) {
                Color(0xFF7FA9FF)
            } else {
                Color(0xFFC68D62)
            }
            val flightMenuButtonAlpha = if (isDarkTheme) 0.24f else 0.20f
            val flightMenuBlurDp = if (isDarkTheme) 10f else 9f

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(flightMenuBackdrop)
            ) {
                WebCardContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .layerBackdrop(backdrop)
                        .graphicsLayer {
                            this.alpha = if (isWebCard(cardId) && cardId != "card3") 1f else 0f
                        }
                        .zIndex(0f),
                    cardId = activeWebCardId,
                    onFlightMainPageChange = { flightMainTabsVisible = it },
                    onFlightWebViewReady = { flightWebView = it },
                    onFlightLiveStatusChange = { liveStatusJson = it },
                    onFlightBriefChange = { flightBriefJson = it },
                    onFlightTableChange = { flightTableJson = it },
                    onWeatherChange = { weatherJson = it },
                )

                if (cardId == "card3") {
                    NativeFlightTablePage(
                        modifier = Modifier
                            .fillMaxSize()
                            .layerBackdrop(flightPageBackdrop)
                            .zIndex(1f),
                        backdrop = backdrop,
                        snapshot = flightTableSnapshot,
                        liveStatusSnapshot = liveStatusSnapshot,
                        flightSnapshot = flightSheetBrief,
                        weather = flightSheetWeather,
                        mode = if (selectedFlightTab == "alerts") "alerts" else flightTableMode,
                        webTheme = nativeTableTheme,
                        textZoom = nativeTableTextZoom,
                        groupedFlights = nativeTableGrouped,
                        highContrast = nativeTableHighContrast,
                        onTitleProgressChange = { progress ->
                            flightTitleProgress = progress
                        },
                        onRefreshAlerts = {
                            flightWebView?.evaluateJavascript(
                                "try{window.fsRefreshNativeLiveStatus&&window.fsRefreshNativeLiveStatus()}catch(e){}",
                                null
                            )
                        }
                    )
                }

                NativeFlightBottomTabs(
                    selected = selectedFlightTab,
                    backdrop = activeFlightBackdrop,
                    contentView = null,
                    onSelect = { next ->
                    val sameTabSelected = selectedFlightTab == next
                    when (next) {
                        "alerts" -> {
                            showFlightMenuSheet = false
                            if (cardId == "card3") {
                                showFlightTableSheet = false
                                showFlightAlertsSheet = false
                                selectedFlightTab = next
                                lastFlightContentTab = next
                                flightWebView?.evaluateJavascript(
                                    "try{window.fsRefreshNativeLiveStatus&&window.fsRefreshNativeLiveStatus()}catch(e){}",
                                    null
                                )
                            } else {
                                val shouldClose = showFlightAlertsSheet && sameTabSelected
                                showFlightTableSheet = false
                                showFlightAlertsSheet = !shouldClose
                                selectedFlightTab = if (shouldClose) selectedFlightTableTab() else next
                                if (!shouldClose) {
                                    flightWebView?.evaluateJavascript(
                                        "try{window.fsRefreshNativeLiveStatus&&window.fsRefreshNativeLiveStatus()}catch(e){}",
                                        null
                                    )
                                }
                            }
                        }
                        "menu" -> {
                            val shouldClose = showFlightMenuSheet
                            showFlightAlertsSheet = false
                            showFlightTableSheet = false
                            showFlightMenuSheet = !shouldClose
                            selectedFlightTab = if (cardId == "card3") {
                                lastFlightContentTab
                            } else {
                                if (shouldClose) selectedFlightTableTab() else next
                            }
                        }
                        "arrivals", "departures" -> {
                            showFlightAlertsSheet = false
                            showFlightMenuSheet = false
                            if (cardId == "card3") {
                                val nextMode = if (next == "departures") "departure" else "arrival"
                                flightTableMode = nextMode
                                selectedFlightTab = next
                                lastFlightContentTab = next
                                showFlightTableSheet = false
                                flightWebView?.evaluateJavascript(
                                    "try{window.fsNativeFlightTab&&window.fsNativeFlightTab('$next')}catch(e){}",
                                    null
                                )
                            } else {
                                val nextMode = if (next == "departures") "departure" else "arrival"
                                val shouldClose = showFlightTableSheet && sameTabSelected && flightTableMode == nextMode
                                flightTableMode = if (next == "departures") "departure" else "arrival"
                                showFlightTableSheet = !shouldClose
                                selectedFlightTab = if (shouldClose) selectedFlightTableTab() else next
                            }
                        }
                    }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 4.dp)
                        .navigationBarsPadding()
                        .zIndex(30f)
                )

                FlightScheduleSheet(
                    visible = showFlightAlertsSheet || showFlightTableSheet,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .zIndex(20f),
                    backdrop = activeFlightBackdrop,
                    tableSnapshot = flightTableSnapshot,
                    liveStatusSnapshot = liveStatusSnapshot,
                    flightSnapshot = flightSheetBrief,
                    weather = flightSheetWeather,
                    mode = if (showFlightAlertsSheet) "alerts" else flightTableMode,
                    textZoom = nativeTableTextZoom,
                    groupedFlights = nativeTableGrouped,
                    highContrast = nativeTableHighContrast,
                    onRefreshAlerts = {
                        flightWebView?.evaluateJavascript(
                            "try{window.fsRefreshNativeLiveStatus&&window.fsRefreshNativeLiveStatus()}catch(e){}",
                            null
                        )
                    },
                    onDismiss = {
                        showFlightAlertsSheet = false
                        showFlightTableSheet = false
                        selectedFlightTab = selectedFlightTableTab()
                    }
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
                    alternateTitle = flightSectionTitle.takeIf { cardId == "card3" },
                    titleProgress = if (cardId == "card3") flightTitleProgress else 0f
                )
            }

            PrimaryBottomChrome(
                selectedTab = PrimaryTabDestination.Home,
                backdrop = flightMenuBackdrop,
                menuVisible = showFlightMenuSheet,
                menuActions = webViewMenuActions(
                    onOpenWelcome = {
                        showFlightMenuSheet = false
                        setCard("card1")
                    },
                    onOpenNews = {
                        showFlightMenuSheet = false
                        setCard("card2")
                    },
                    onOpenFlights = {
                        showFlightMenuSheet = false
                        setCard("card3")
                    },
                    onOpenFbo = {
                        showFlightMenuSheet = false
                        setCard("card4")
                    },
                    onOpenAbout = {
                        showFlightMenuSheet = false
                        setCard("about_us")
                    },
                    onOpenContact = {
                        showFlightMenuSheet = false
                        setCard("contact_us")
                    },
                    onOpenWebSettings = {
                        showFlightMenuSheet = false
                        setCard("settings")
                    },
                    onOpenMainApp = {
                        showFlightMenuSheet = false
                        exitToMainApp()
                    }
                ),
                onMenuDismiss = {
                    showFlightMenuSheet = false
                    selectedFlightTab = selectedFlightContentTab()
                },
                onOpenHome = {},
                onOpenContacts = {},
                onOpenNotes = {},
                onOpenSettings = {},
                onOpenMenu = { showFlightMenuSheet = true },
                showTabs = false,
                contentView = null,
                menuPanelColor = flightMenuPanelColor,
                menuOverlayTint = flightMenuOverlayTint,
                menuButtonColor = flightMenuButtonColor,
                menuButtonAlpha = flightMenuButtonAlpha,
                menuBlurDp = flightMenuBlurDp
            )
        }
    }
}

private fun webViewMenuActions(
    onOpenWelcome: () -> Unit,
    onOpenNews: () -> Unit,
    onOpenFlights: () -> Unit,
    onOpenFbo: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenContact: () -> Unit,
    onOpenWebSettings: () -> Unit,
    onOpenMainApp: () -> Unit
): List<PrimaryMenuAction> = listOf(
    PrimaryMenuAction("Welcome", R.drawable.ic_oui_home, onOpenWelcome),
    PrimaryMenuAction("News", R.drawable.ic_oui_news, onOpenNews),
    PrimaryMenuAction("Flights", R.drawable.baseline_flight_24, onOpenFlights),
    PrimaryMenuAction("FBO", R.drawable.airplane_svgrepo_com, onOpenFbo),
    PrimaryMenuAction("About us", R.drawable.baseline_info_24, onOpenAbout),
    PrimaryMenuAction("Contact", R.drawable.baseline_contact_mail_24, onOpenContact),
    PrimaryMenuAction("Web Settings", R.drawable.ic_oui_settings, onOpenWebSettings),
    PrimaryMenuAction(
        label = "Main app",
        iconRes = R.drawable.account_circle_24dp_ffffff_fill1_profile,
        onClick = onOpenMainApp,
        useProfileAvatar = true
    )
)

@Composable
private fun WebViewSettingsStyleTopAppBar(
    backdrop: LayerBackdrop,
    title: String,
    alternateTitle: String? = null,
    titleProgress: Float = 0f,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val topBarShape = RoundedCornerShape(0.dp)
    val barColor = topActionBarTint()
    val contentColor = if (isDark) Color.White else Color(0xFF111111)
    val visibleTitle = if (alternateTitle != null && titleProgress > 0.56f) alternateTitle else title

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
                .padding(start = 18.dp, end = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedContent(
                targetState = visibleTitle,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                transitionSpec = {
                    (
                        fadeIn(animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)) +
                            slideInVertically(
                                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                                initialOffsetY = { it / 5 }
                            )
                        ).togetherWith(
                        fadeOut(animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing)) +
                            slideOutVertically(
                                animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
                                targetOffsetY = { -it / 6 }
                            )
                    ).using(SizeTransform(clip = false))
                },
                label = "webTopBarTitle"
            ) { currentTitle ->
                Text(
                    text = currentTitle,
                    color = contentColor,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
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

private class FlightBriefBridge(
    context: Context,
    private val onLiveStatusSnapshot: (String) -> Unit,
    private val onFlightBriefSnapshot: (String) -> Unit,
    private val onFlightTableSnapshot: (String) -> Unit,
    private val onWeatherSnapshot: (String) -> Unit
) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun updateFlightBriefSnapshot(json: String) {
        SettingsStore.setFlightBriefSnapshot(appContext, json)
        mainHandler.post { onFlightBriefSnapshot(json) }
    }

    @JavascriptInterface
    fun updateFlightLiveStatusSnapshot(json: String) {
        SettingsStore.setFlightLiveStatusSnapshot(appContext, json)
        mainHandler.post { onLiveStatusSnapshot(json) }
    }

    @JavascriptInterface
    fun updateFlightTableSnapshot(json: String) {
        SettingsStore.setFlightTableSnapshot(appContext, json)
        mainHandler.post { onFlightTableSnapshot(json) }
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
        mainHandler.post { onWeatherSnapshot(taggedJson) }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WebCardContent(
    modifier: Modifier,
    cardId: String,
    onFlightMainPageChange: (Boolean) -> Unit,
    onFlightWebViewReady: (WebView) -> Unit,
    onFlightLiveStatusChange: (String) -> Unit,
    onFlightBriefChange: (String) -> Unit,
    onFlightTableChange: (String) -> Unit,
    onWeatherChange: (String) -> Unit,
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

    val groupedFlights = remember(settingsRevision) { SettingsStore.groupFlights(context) }
    val highContrastWeb = remember(settingsRevision) { SettingsStore.highContrastWeb(context) }
    val aiPerformance = remember(settingsRevision) { SettingsStore.aiPerformance(context) }
    val reduceWebMotion = remember(settingsRevision, aiPerformance) { SettingsStore.reduceWebMotion(context) && !aiPerformance }
    val cachePages = remember(settingsRevision) { SettingsStore.cachePages(context) }
    val hwAccel = remember(settingsRevision, aiPerformance) { SettingsStore.hardwareAccel(context) || aiPerformance }
    val textZoomPref = remember(settingsRevision, aiPerformance) {
        SettingsStore.textZoom(context).let { if (aiPerformance) it.coerceAtLeast(95) else it }
    }
    val webTheme = remember(settingsRevision) { SettingsStore.webTheme(context) }
    val baseWebColor = if (isDark) Color(0xFF2B2924) else Color(0xFFF4F1E9)
    val url = remember(cardId) { urlForCard(cardId) }

    var progress by remember(url) { mutableIntStateOf(0) }
    var showError by remember(url) { mutableStateOf(false) }
    var reloadTick by remember(url) { mutableIntStateOf(0) }
    var loadedRootUrl by remember { mutableStateOf<String?>(null) }
    var animatedRootUrl by remember { mutableStateOf<String?>(null) }
    var currentPageUrl by remember(url) { mutableStateOf(url) }

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

            addJavascriptInterface(
                FlightBriefBridge(
                    context = context,
                    onLiveStatusSnapshot = onFlightLiveStatusChange,
                    onFlightBriefSnapshot = onFlightBriefChange,
                    onFlightTableSnapshot = onFlightTableChange,
                    onWeatherSnapshot = onWeatherChange
                ),
                "FlightsAndroidBridge"
            )


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
                    if (!targetUrl.isNullOrBlank() && targetUrl != currentPageUrl) {
                        view?.animate()?.cancel()
                        view?.alpha = 0f
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
                    if (!url.isNullOrBlank() && url != currentPageUrl) {
                        view?.animate()?.cancel()
                        view?.alpha = 0f
                    }
                    return false
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    currentPageUrl = url.orEmpty()
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
                    currentPageUrl = url.orEmpty()

                    val isFlightsMain =
                        url?.startsWith("https://www.jacksonholeairport.com/flights/") == true &&
                                url.endsWith("/flights/")

                    injectWebRuntimePreferences(
                        view,
                        SettingsStore.webTheme(context),
                        SettingsStore.textZoom(context).let {
                            if (SettingsStore.aiPerformance(context)) it.coerceAtLeast(95) else it
                        },
                        SettingsStore.groupFlights(context),
                        SettingsStore.highContrastWeb(context),
                        SettingsStore.reduceWebMotion(context) && !SettingsStore.aiPerformance(context)
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

    LaunchedEffect(webView) {
        onFlightWebViewReady(webView)
    }

    LaunchedEffect(cardId, currentPageUrl, showError) {
        val isFlightsMainPage = cardId == "card3" &&
            currentPageUrl.startsWith("https://www.jacksonholeairport.com/flights/") &&
            currentPageUrl.endsWith("/flights/") &&
            !showError
        onFlightMainPageChange(isFlightsMainPage)
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
                injectWebRuntimePreferences(wv, webTheme, textZoomPref, groupedFlights, highContrastWeb, reduceWebMotion)

                if (loadedRootUrl != url) {

                    if (!hasInternet(context)) {
                        showError = true
                        return@AndroidView
                    }

                    wv.animate().cancel()
                    wv.alpha = 0f
                    wv.translationX = 0f
                    wv.scaleX = 1f
                    wv.scaleY = 1f
                    progress = 0
                    showError = false
                    hasMainFrameError = false
                    loadedRootUrl = url
                    wv.loadUrl(url)
                }
                if (reloadTick > 0) {
                    if (!hasInternet(context)) {
                        hasMainFrameError = true
                        showError = true
                        wv.stopLoading()
                        wv.alpha = 0f
                    } else {
                        progress = 0
                        showError = false
                        hasMainFrameError = false
                        wv.reload()
                    }
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

@Stable
private data class FlightLiveStatusSnapshot(
    val updatedLabel: String = "",
    val items: List<FlightLiveStatusItem> = emptyList()
)

@Stable
private data class FlightLiveStatusItem(
    val flight: String,
    val route: String,
    val status: String,
    val detail: String,
    val tone: String,
    val badge: String,
    val pill: String,
    val etaText: String,
    val meta: String,
    val delayLabel: String,
    val progress: Float
)

@Stable
private data class FlightSheetIssue(
    val label: String,
    val flight: String,
    val route: String,
    val time: String,
    val tone: String
)

@Stable
private data class FlightSheetBrief(
    val summary: String = "",
    val arrivalCount: Int = 0,
    val departureCount: Int = 0,
    val delayedCount: Int = 0,
    val cancelledCount: Int = 0,
    val divertedCount: Int = 0,
    val scheduleDayLabel: String = "",
    val upcomingDayLabel: String = "",
    val issues: List<FlightSheetIssue> = emptyList()
)

@Stable
private data class FlightSheetWeather(
    val temp: String = "",
    val summary: String = "",
    val condition: String = ""
)

@Stable
private data class FlightTableSnapshot(
    val lastUpdated: String = "",
    val days: List<FlightTableDay> = emptyList(),
    val rows: List<FlightTableRow> = emptyList()
)

@Stable
private data class FlightTableDay(
    val label: String,
    val arrivals: Int,
    val departures: Int
)

@Stable
private data class FlightTableRow(
    val kind: String,
    val day: String,
    val airline: String,
    val flight: String,
    val place: String,
    val sched: String,
    val actual: String,
    val status: String,
    val tone: String,
    val delay: Int
)

private fun FlightTableRow.isCancelledFlight(): Boolean {
    return tone.contains("cancel", ignoreCase = true) ||
        status.contains("cancel", ignoreCase = true)
}

private fun FlightTableRow.isDivertedFlight(): Boolean {
    return tone.contains("divert", ignoreCase = true) ||
        status.contains("divert", ignoreCase = true)
}

@Stable
private data class NativeFlightTablePalette(
    val page: Color,
    val panel: Color,
    val overlay: Color,
    val surface: Color,
    val arrivedSurface: Color,
    val delayedSurface: Color,
    val cancelledSurface: Color,
    val divertedSurface: Color,
    val rowBorder: Color,
    val arrivedAccent: Color,
    val departedAccent: Color,
    val delayAccent: Color,
    val cancelledAccent: Color,
    val divertedAccent: Color,
    val text: Color,
    val muted: Color
)

private data class FlightTableAirlineGroup(
    val airline: String,
    val rows: List<FlightTableRow>,
    val firstTime: Int
)

private fun parseFlightLiveStatusSnapshot(json: String): FlightLiveStatusSnapshot {
    if (json.isBlank()) return FlightLiveStatusSnapshot()
    return runCatching {
        val root = JSONObject(json)
        if (isPreviousLocalDaySnapshot(root.optLong("updatedAt", 0L))) {
            return@runCatching FlightLiveStatusSnapshot()
        }
        val itemsJson = root.optJSONArray("items") ?: JSONArray()
        val items = buildList {
            for (index in 0 until itemsJson.length()) {
                val item = itemsJson.optJSONObject(index) ?: continue
                val rawStatus = item.optString("status").ifBlank { "Status pending" }
                val isLive = item.optBoolean("live", false)
                val wasCachedEnRoute = !isLive && rawStatus.equals("En route", ignoreCase = true)
                val rawEtaText = item.optString("etaText")
                add(
                    FlightLiveStatusItem(
                        flight = item.optString("flight").ifBlank { "Flight" },
                        route = item.optString("route"),
                        status = if (wasCachedEnRoute) "Scheduled" else rawStatus,
                        detail = item.optString("detail"),
                        tone = if (wasCachedEnRoute && item.optString("tone") == "active") "scheduled" else item.optString("tone"),
                        badge = if (wasCachedEnRoute && item.optString("badge").equals("EN ROUTE", ignoreCase = true)) "UPCOMING" else item.optString("badge"),
                        pill = if (wasCachedEnRoute && item.optString("pill").equals("Landing JAC", ignoreCase = true)) "Arrival" else item.optString("pill"),
                        etaText = if (wasCachedEnRoute && rawEtaText.endsWith(" remaining", ignoreCase = true)) {
                            "Scheduled in " + rawEtaText.removeSuffix(" remaining")
                        } else {
                            rawEtaText
                        },
                        meta = item.optString("meta"),
                        delayLabel = item.optString("delayLabel"),
                        progress = if (wasCachedEnRoute) 0f else item.optDouble("progress", 0.0).toFloat().coerceIn(0f, 100f)
                    )
                )
            }
        }
        FlightLiveStatusSnapshot(
            updatedLabel = root.optString("updatedLabel"),
            items = items
        )
    }.getOrDefault(FlightLiveStatusSnapshot())
}

private fun parseFlightTableSnapshot(json: String): FlightTableSnapshot {
    if (json.isBlank()) return FlightTableSnapshot()
    return runCatching {
        val root = JSONObject(json)
        if (isPreviousLocalDaySnapshot(root.optLong("updatedAt", 0L))) {
            return@runCatching FlightTableSnapshot()
        }
        val daysJson = root.optJSONArray("days") ?: JSONArray()
        val rowsJson = root.optJSONArray("rows") ?: JSONArray()
        val days = buildList {
            for (index in 0 until daysJson.length()) {
                val day = daysJson.optJSONObject(index) ?: continue
                add(
                    FlightTableDay(
                        label = day.optString("label").ifBlank { "Today" },
                        arrivals = day.optInt("arrivals", 0),
                        departures = day.optInt("departures", 0)
                    )
                )
            }
        }
        val rows = buildList {
            for (index in 0 until rowsJson.length()) {
                val row = rowsJson.optJSONObject(index) ?: continue
                add(
                    FlightTableRow(
                        kind = row.optString("kind").ifBlank { "arrival" },
                        day = row.optString("day").ifBlank { "Today" },
                        airline = row.optString("airline"),
                        flight = row.optString("flight"),
                        place = row.optString("place"),
                        sched = row.optString("sched"),
                        actual = row.optString("actual"),
                        status = row.optString("status").ifBlank { "Scheduled" },
                        tone = row.optString("tone"),
                        delay = row.optInt("delay", 0)
                    )
                )
            }
        }
        FlightTableSnapshot(
            lastUpdated = root.optString("lastUpdated"),
            days = days,
            rows = rows
        )
    }.getOrDefault(FlightTableSnapshot())
}

private fun parseFlightBriefSnapshotForSheet(json: String): FlightSheetBrief {
    if (json.isBlank()) return FlightSheetBrief()
    return runCatching {
        val root = JSONObject(json)
        if (isPreviousLocalDaySnapshot(root.optLong("updatedAt", 0L))) {
            return@runCatching FlightSheetBrief()
        }
        val issuesJson = root.optJSONArray("issues") ?: JSONArray()
        val issues = buildList {
            for (index in 0 until issuesJson.length()) {
                val item = issuesJson.optJSONObject(index) ?: continue
                add(
                    FlightSheetIssue(
                        label = item.optString("label"),
                        flight = item.optString("flight"),
                        route = item.optString("route"),
                        time = item.optString("time"),
                        tone = item.optString("tone")
                    )
                )
            }
        }
        FlightSheetBrief(
            summary = root.optString("summary"),
            arrivalCount = root.optInt("arrivalCount", 0),
            departureCount = root.optInt("departureCount", 0),
            delayedCount = root.optInt("delayedCount", 0),
            cancelledCount = root.optInt("cancelledCount", 0),
            divertedCount = root.optInt("divertedCount", 0),
            issues = issues
        )
    }.getOrDefault(FlightSheetBrief())
}

private fun currentFlightRows(snapshot: FlightTableSnapshot): List<FlightTableRow> {
    if (snapshot.rows.isEmpty()) return emptyList()
    val firstDay = snapshot.days
        .map { it.label }
        .firstOrNull { label -> label.isNotBlank() && snapshot.rows.any { row -> row.day == label } }
        ?: snapshot.rows.firstOrNull { it.day.isNotBlank() }?.day
        ?: snapshot.days.firstOrNull { it.arrivals > 0 || it.departures > 0 }?.label
        ?: return snapshot.rows
    return snapshot.rows.filter { it.day == firstDay }.ifEmpty { snapshot.rows }
}

private fun isTodayFlightDayLabel(label: String): Boolean {
    if (label.isBlank()) return false
    val now = Calendar.getInstance().time
    val normalized = label.lowercase(Locale.US)
        .replace(Regex("[^a-z0-9 ]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
    val day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH).toString()
    val monthShort = SimpleDateFormat("MMM", Locale.US).format(now).lowercase(Locale.US)
    val monthLong = SimpleDateFormat("MMMM", Locale.US).format(now).lowercase(Locale.US)
    val weekday = SimpleDateFormat("EEEE", Locale.US).format(now).lowercase(Locale.US)
    return normalized.contains(Regex("\\b$day(st|nd|rd|th)?\\b")) &&
        (normalized.contains(monthShort) || normalized.contains(monthLong)) &&
        (normalized.contains(weekday) || normalized.length < 18)
}

private fun FlightSheetBrief.withCurrentTableCounts(snapshot: FlightTableSnapshot): FlightSheetBrief {
    val rows = currentFlightRows(snapshot)
    if (rows.isEmpty()) return this
    val issues = rows
        .filter { row ->
            row.delay > 0 ||
                row.isCancelledFlight() ||
                row.isDivertedFlight()
        }
        .sortedWith(
            compareByDescending<FlightTableRow> {
                when {
                    it.isCancelledFlight() -> 3
                    it.isDivertedFlight() -> 2
                    else -> 1
                }
            }.thenByDescending { it.delay }
        )
        .map { row ->
            val label = when {
                row.isCancelledFlight() -> "Cancelled"
                row.isDivertedFlight() -> "Diverted"
                row.delay > 0 -> "+${row.delay} min"
                else -> row.status.ifBlank { "Alert" }
            }
            FlightSheetIssue(
                label = label,
                flight = "${row.airline} ${row.flight}".trim(),
                route = row.place,
                time = if (row.actual.isNotBlank() && row.actual != row.sched) {
                    "${row.sched} -> ${row.actual}"
                } else {
                    row.sched.ifBlank { "time pending" }
                },
                tone = when {
                    row.isCancelledFlight() -> "cancelled"
                    row.isDivertedFlight() -> "diverted"
                    else -> row.tone
                }
            )
        }
    val arrivalCount = rows.count { it.kind != "departure" }
    val departureCount = rows.count { it.kind == "departure" }
    val delayedCount = rows.count { it.delay > 0 }
    val cancelledCount = rows.count { it.isCancelledFlight() }
    val divertedCount = rows.count { it.isDivertedFlight() }
    val day = rows.firstOrNull()?.day.orEmpty()
    val upcomingDay = snapshot.days
        .map { it.label }
        .dropWhile { label -> !label.equals(day, ignoreCase = true) }
        .drop(1)
        .firstOrNull { it.isNotBlank() }
        .orEmpty()
    val summary = buildString {
        if (day.isNotBlank()) append(day).append(": ")
        append(arrivalCount).append(" arrival")
        if (arrivalCount != 1) append("s")
        append(", ")
        append(departureCount).append(" departure")
        if (departureCount != 1) append("s")
        append(".")
        if (delayedCount == 0 && cancelledCount == 0 && divertedCount == 0) {
            append(" No delays, cancellations, or diversions visible right now.")
        }
    }
    return copy(
        summary = summary,
        arrivalCount = arrivalCount,
        departureCount = departureCount,
        delayedCount = delayedCount,
        cancelledCount = cancelledCount,
        divertedCount = divertedCount,
        scheduleDayLabel = day,
        upcomingDayLabel = upcomingDay,
        issues = issues
    )
}

private fun isPreviousLocalDaySnapshot(updatedAt: Long): Boolean {
    if (updatedAt <= 0L) return false
    val startOfToday = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    return updatedAt < startOfToday
}

private fun parseWeatherSnapshotForSheet(json: String): FlightSheetWeather {
    if (json.isBlank()) return FlightSheetWeather()
    return runCatching {
        val root = JSONObject(json)
        FlightSheetWeather(
            temp = root.optString("temp"),
            summary = root.optString("summary"),
            condition = root.optString("condition")
        )
    }.getOrDefault(FlightSheetWeather())
}

private fun nativeFlightTablePalette(
    webTheme: String,
    isDark: Boolean,
    glassAmount: Float
): NativeFlightTablePalette {
    val effectiveTheme = when (webTheme.lowercase()) {
        "auto" -> if (isDark) "dark" else "light"
        "dark", "ocean", "mint", "sky", "violet", "rose", "amber", "gray", "light" -> webTheme.lowercase()
        else -> if (isDark) "dark" else "light"
    }
    val accent = when (effectiveTheme) {
        "mint" -> Color(0xFF22B981)
        "sky" -> Color(0xFF3B82F6)
        "ocean" -> Color(0xFF7DD3FC)
        "violet" -> Color(0xFF8B5CF6)
        "rose" -> Color(0xFFEC4899)
        "amber" -> Color(0xFFF59E0B)
        "gray" -> Color(0xFF64748B)
        "dark" -> Color(0xFF7DD3FC)
        else -> Color(0xFF5AC8FA)
    }
    return if (effectiveTheme == "dark" || effectiveTheme == "ocean") {
        val baseSurface = accent.copy(alpha = 0.08f).compositeOver(Color(0xFF101B27).copy(alpha = 0.66f))
        val panel = accent.copy(alpha = 0.025f + 0.05f * glassAmount)
            .compositeOver(Color(0xFF10151D).copy(alpha = 0.58f + 0.32f * glassAmount))
        NativeFlightTablePalette(
            page = if (effectiveTheme == "ocean") Color(0xFF071820) else Color(0xFF07111C),
            panel = panel,
            overlay = accent.copy(alpha = 0.03f + 0.04f * glassAmount)
                .compositeOver(Color.Black.copy(alpha = 0.16f + 0.18f * glassAmount)),
            surface = baseSurface,
            arrivedSurface = accent.copy(alpha = 0.18f).compositeOver(baseSurface),
            delayedSurface = Color(0xFFFFB020).copy(alpha = 0.18f).compositeOver(baseSurface),
            cancelledSurface = Color(0xFFFF453A).copy(alpha = 0.18f).compositeOver(baseSurface),
            divertedSurface = Color(0xFFFF9F0A).copy(alpha = 0.18f).compositeOver(baseSurface),
            rowBorder = accent.copy(alpha = 0.28f),
            arrivedAccent = accent,
            departedAccent = if (effectiveTheme == "ocean") Color(0xFF38E8C8) else Color(0xFF34C759),
            delayAccent = Color(0xFFFFB020),
            cancelledAccent = Color(0xFFFF453A),
            divertedAccent = Color(0xFFFF9F0A),
            text = Color.White.copy(alpha = 0.94f),
            muted = Color.White.copy(alpha = 0.62f)
        )
    } else {
        val page = when (effectiveTheme) {
            "mint" -> Color(0xFFF2FBF8)
            "sky" -> Color(0xFFF0F7FF)
            "violet" -> Color(0xFFF7F3FF)
            "rose" -> Color(0xFFFFF5FA)
            "amber" -> Color(0xFFFFFAEC)
            "gray" -> Color(0xFFF4F6F8)
            else -> Color(0xFFF8FAFF)
        }
        val panel = accent.copy(alpha = 0.025f + 0.035f * glassAmount)
            .compositeOver(page.copy(alpha = 0.66f + 0.26f * glassAmount))
        val surface = accent.copy(alpha = 0.035f + 0.045f * glassAmount)
            .compositeOver(Color.White.copy(alpha = 0.56f + 0.22f * glassAmount))
        val text = when (effectiveTheme) {
            "mint" -> Color(0xFF10201C)
            "sky" -> Color(0xFF10243F)
            "violet" -> Color(0xFF261B3F)
            "rose" -> Color(0xFF3E122A)
            "amber" -> Color(0xFF34230C)
            "gray" -> Color(0xFF1F2937)
            else -> Color(0xFF1E1F24)
        }
        val readableAccent = when (effectiveTheme) {
            "mint" -> Color(0xFF0F6B4A)
            "sky" -> Color(0xFF075985)
            "violet" -> Color(0xFF5B21B6)
            "rose" -> Color(0xFF9D174D)
            "amber" -> Color(0xFF8A4B08)
            "gray" -> Color(0xFF475569)
            else -> Color(0xFF0F5FA8)
        }
        NativeFlightTablePalette(
            page = page,
            panel = panel,
            overlay = accent.copy(alpha = 0.035f + 0.065f * glassAmount)
                .compositeOver(Color.White.copy(alpha = 0.10f + 0.20f * glassAmount)),
            surface = surface,
            arrivedSurface = accent.copy(alpha = 0.18f).compositeOver(surface),
            delayedSurface = Color(0xFFF59E0B).copy(alpha = 0.18f).compositeOver(surface),
            cancelledSurface = Color(0xFFFF453A).copy(alpha = 0.14f).compositeOver(surface),
            divertedSurface = Color(0xFFFF9F0A).copy(alpha = 0.16f).compositeOver(surface),
            rowBorder = readableAccent.copy(alpha = 0.30f),
            arrivedAccent = readableAccent,
            departedAccent = readableAccent,
            delayAccent = if (effectiveTheme == "amber") Color(0xFF7A3D05) else Color(0xFF9A5A00),
            cancelledAccent = Color(0xFFD93025),
            divertedAccent = Color(0xFF9A4D00),
            text = text,
            muted = text.copy(alpha = 0.66f)
        )
    }
}

private fun nativeFlightHighContrastPalette(isDark: Boolean): NativeFlightTablePalette {
    return if (isDark) {
        NativeFlightTablePalette(
            page = Color.Black,
            panel = Color.Black.copy(alpha = 0.82f),
            overlay = Color.White.copy(alpha = 0.04f),
            surface = Color.Black.copy(alpha = 0.72f),
            arrivedSurface = Color.Black.copy(alpha = 0.72f),
            delayedSurface = Color.Black.copy(alpha = 0.72f),
            cancelledSurface = Color.Black.copy(alpha = 0.72f),
            divertedSurface = Color.Black.copy(alpha = 0.72f),
            rowBorder = Color.White.copy(alpha = 0.36f),
            arrivedAccent = Color.White,
            departedAccent = Color.White,
            delayAccent = Color.White,
            cancelledAccent = Color.White,
            divertedAccent = Color.White,
            text = Color.White,
            muted = Color.White.copy(alpha = 0.70f)
        )
    } else {
        NativeFlightTablePalette(
            page = Color.White,
            panel = Color.White.copy(alpha = 0.92f),
            overlay = Color.Black.copy(alpha = 0.03f),
            surface = Color.White.copy(alpha = 0.94f),
            arrivedSurface = Color.White.copy(alpha = 0.94f),
            delayedSurface = Color.White.copy(alpha = 0.94f),
            cancelledSurface = Color.White.copy(alpha = 0.94f),
            divertedSurface = Color.White.copy(alpha = 0.94f),
            rowBorder = Color.Black.copy(alpha = 0.36f),
            arrivedAccent = Color.Black,
            departedAccent = Color.Black,
            delayAccent = Color.Black,
            cancelledAccent = Color.Black,
            divertedAccent = Color.Black,
            text = Color.Black,
            muted = Color.Black.copy(alpha = 0.64f)
        )
    }
}

@Composable
private fun NativeFlightTablePage(
    modifier: Modifier = Modifier,
    backdrop: LayerBackdrop,
    snapshot: FlightTableSnapshot,
    liveStatusSnapshot: FlightLiveStatusSnapshot,
    flightSnapshot: FlightSheetBrief,
    weather: FlightSheetWeather,
    mode: String,
    webTheme: String,
    textZoom: Int,
    groupedFlights: Boolean,
    highContrast: Boolean,
    onTitleProgressChange: (Float) -> Unit,
    onRefreshAlerts: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val glassAmount = rememberLiquidGlassTintAmount()
    val palette = remember(webTheme, isDark, glassAmount) {
        nativeFlightTablePalette(webTheme, isDark, glassAmount)
    }.let { if (highContrast) nativeFlightHighContrastPalette(isDark) else it }
    val pageShape = RoundedCornerShape(26.dp)
    val textScale = flightTableTextScale(textZoom)
    val modeAccent = when (mode) {
        "departure" -> FlightDepartureLantern
        "alerts" -> FlightAlertLantern
        else -> FlightArrivalLantern
    }
    val modePanel = flightLanternSheetPanelColor(modeAccent, isDark, glassAmount)
    val modeOverlay = flightLanternSheetOverlayColor(modeAccent, isDark, glassAmount)
    val modeSheen = flightLanternSheetSheenBrush(modeAccent, isDark, glassAmount)
    val alertSurface = if (highContrast) {
        if (isDark) Color.Black.copy(alpha = 0.72f) else Color.White.copy(alpha = 0.92f)
    } else if (isDark) {
        Color.White.copy(alpha = 0.10f + 0.08f * glassAmount)
    } else {
        Color.White.copy(alpha = 0.50f + 0.20f * glassAmount)
    }
    val effectiveFlightSnapshot = remember(flightSnapshot, snapshot) {
        flightSnapshot.withCurrentTableCounts(snapshot)
    }

    LaunchedEffect(mode) {
        onTitleProgressChange(0f)
    }

    Box(
        modifier = modifier
            .background(palette.page)
    ) {
        ProfileBackdropImageLayer(
            modifier = Modifier.matchParentSize(),
            lightRes = R.drawable.light_grid_pattern,
            darkRes = R.drawable.dark_grid_pattern,
            imageAlpha = if (isDark) 1f else 0.72f,
            scrimDark = 0f,
            scrimLight = 0f
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 2.dp, end = 2.dp, top = 106.dp, bottom = 92.dp)
                .clip(pageShape)
                .adaptiveLiquidGlassBackdrop(
                    backdrop = backdrop,
                    shape = pageShape,
                    surfaceColor = modePanel,
                    blurDp = if (isDark) 13f + 11f * glassAmount else 12f + 10f * glassAmount,
                    shadow = null,
                    highlight = null,
                    refractionHeightDp = GlassChromeRefractionHeightDp,
                    refractionAmountDp = GlassChromeRefractionAmountDp
                )
                .background(modeOverlay, pageShape)
                .background(modeSheen, pageShape)
        ) {
            AnimatedContent(
                targetState = mode,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    fun tabOrder(value: String): Int = when (value) {
                        "departure" -> 1
                        "alerts" -> 2
                        else -> 0
                    }
                    val forward = tabOrder(targetState) >= tabOrder(initialState)
                    val enterOffset: (Int) -> Int = { width -> if (forward) width / 4 else -width / 4 }
                    val exitOffset: (Int) -> Int = { width -> if (forward) -width / 5 else width / 5 }
                    (
                        slideInHorizontally(
                            animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                            initialOffsetX = enterOffset
                        ) + fadeIn(animationSpec = tween(durationMillis = 160))
                        ).togetherWith(
                        slideOutHorizontally(
                            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                            targetOffsetX = exitOffset
                        ) + fadeOut(animationSpec = tween(durationMillis = 130))
                    ).using(SizeTransform(clip = false))
                },
                label = "flightTableModeContent"
            ) { targetMode ->
                val contentScrollState = rememberScrollState()
                val titleProgress = ((contentScrollState.value - 88f) / 82f).coerceIn(0f, 1f)
                LaunchedEffect(targetMode, titleProgress) {
                    onTitleProgressChange(titleProgress)
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(contentScrollState)
                        .padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    if (targetMode == "alerts") {
                        FlightLiveStatusContent(
                            snapshot = liveStatusSnapshot,
                            flightSnapshot = effectiveFlightSnapshot,
                            weather = weather,
                            textColor = palette.text,
                            mutedColor = palette.muted,
                            surface = alertSurface,
                            lightLift = !isDark,
                            textScale = textScale.coerceIn(0.82f, 1.12f),
                            highContrast = highContrast,
                            showHandle = false,
                            showRefresh = true,
                            title = "Alerts",
                            onRefresh = onRefreshAlerts
                        )
                    } else {
                        FlightTableContent(
                            snapshot = snapshot,
                            mode = targetMode,
                            textColor = palette.text,
                            mutedColor = palette.muted,
                            surface = palette.surface,
                            tablePalette = palette,
                            textScale = textScale,
                            groupedFlights = groupedFlights,
                            highContrast = highContrast
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FlightScheduleSheet(
    visible: Boolean,
    modifier: Modifier = Modifier,
    backdrop: LayerBackdrop,
    tableSnapshot: FlightTableSnapshot,
    liveStatusSnapshot: FlightLiveStatusSnapshot,
    flightSnapshot: FlightSheetBrief,
    weather: FlightSheetWeather,
    mode: String,
    textZoom: Int,
    groupedFlights: Boolean,
    highContrast: Boolean,
    onRefreshAlerts: () -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val glassAmount = rememberLiquidGlassTintAmount()
    val modeAccent = when (mode) {
        "departure" -> FlightDepartureLantern
        "alerts" -> FlightAlertLantern
        else -> FlightArrivalLantern
    }
    val panelColor = flightLanternSheetPanelColor(modeAccent, isDark, glassAmount)
    val overlayTint = flightLanternSheetOverlayColor(modeAccent, isDark, glassAmount)
    val sheenBrush = flightLanternSheetSheenBrush(modeAccent, isDark, glassAmount)
    val sheetBlurDp = if (isDark) 18f + 14f * glassAmount else 17f + 13f * glassAmount
    val textColor = if (highContrast) {
        if (isDark) Color.White else Color.Black
    } else if (isDark) Color.White.copy(alpha = 0.94f) else Color(0xFF1E1F24)
    val mutedColor = textColor.copy(alpha = 0.62f)
    val innerSurface = if (highContrast) {
        if (isDark) Color.Black.copy(alpha = 0.72f) else Color.White.copy(alpha = 0.92f)
    } else if (isDark) Color.White.copy(alpha = 0.10f + 0.08f * glassAmount)
    else Color.White.copy(alpha = 0.50f + 0.20f * glassAmount)
    val sheetShape = RoundedCornerShape(26.dp)
    val tableTextScale = flightTableTextScale(textZoom)
    val statusTextScale = (textZoom.coerceIn(60, 100) / SettingsStore.DEFAULT_TEXT_ZOOM.toFloat())
        .coerceIn(0.82f, 1.12f)
    val effectiveFlightSnapshot = remember(flightSnapshot, tableSnapshot) {
        flightSnapshot.withCurrentTableCounts(tableSnapshot)
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 130)),
        exit = fadeOut(animationSpec = tween(durationMillis = 160))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (isDark) 0.34f else 0.16f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(
            animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing),
            initialOffsetY = { it / 2 }
        ) + fadeIn(animationSpec = tween(durationMillis = 170)) +
            scaleIn(animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing), initialScale = 0.94f),
        exit = slideOutVertically(
            animationSpec = tween(durationMillis = 210, easing = FastOutLinearInEasing),
            targetOffsetY = { it / 3 }
        ) + fadeOut(animationSpec = tween(durationMillis = 150)) +
            scaleOut(animationSpec = tween(durationMillis = 210, easing = FastOutLinearInEasing), targetScale = 0.98f)
    ) {
        Box(
            modifier = Modifier
                .padding(start = 6.dp, end = 6.dp, bottom = 68.dp)
                .navigationBarsPadding()
                .fillMaxWidth()
                .heightIn(max = 650.dp)
                .clip(sheetShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .adaptiveLiquidGlassBackdrop(
                    backdrop = backdrop,
                    shape = sheetShape,
                    surfaceColor = panelColor,
                    blurDp = sheetBlurDp,
                    shadow = null,
                    highlight = null,
                    refractionHeightDp = GlassChromeRefractionHeightDp,
                    refractionAmountDp = GlassChromeRefractionAmountDp
                )
                .background(overlayTint, sheetShape)
                .background(sheenBrush, sheetShape)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 86.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                if (mode == "alerts") {
                    FlightLiveStatusContent(
                        snapshot = liveStatusSnapshot,
                        flightSnapshot = effectiveFlightSnapshot,
                        weather = weather,
                        textColor = textColor,
                        mutedColor = mutedColor,
                        surface = innerSurface,
                        lightLift = !isDark,
                        textScale = statusTextScale,
                        highContrast = highContrast,
                        showHandle = true,
                        showRefresh = true,
                        title = "Alerts",
                        onRefresh = onRefreshAlerts
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(42.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(mutedColor.copy(alpha = 0.34f))
                    )
                    FlightTableContent(
                        snapshot = tableSnapshot,
                        mode = mode,
                        textColor = textColor,
                        mutedColor = mutedColor,
                        surface = innerSurface,
                        tablePalette = null,
                        textScale = tableTextScale,
                        groupedFlights = groupedFlights,
                        highContrast = highContrast
                    )
                }
            }
        }
    }
}

@Composable
private fun FlightTableSheet(
    visible: Boolean,
    modifier: Modifier = Modifier,
    backdrop: LayerBackdrop,
    snapshot: FlightTableSnapshot,
    mode: String,
    textZoom: Int,
    groupedFlights: Boolean,
    highContrast: Boolean,
    onDismiss: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val glassAmount = rememberLiquidGlassTintAmount()
    val modeAccent = if (mode == "departure") FlightDepartureLantern else FlightArrivalLantern
    val panelColor = flightLanternSheetPanelColor(modeAccent, isDark, glassAmount)
    val overlayTint = flightLanternSheetOverlayColor(modeAccent, isDark, glassAmount)
    val sheenBrush = flightLanternSheetSheenBrush(modeAccent, isDark, glassAmount)
    val sheetBlurDp = if (isDark) 18f + 14f * glassAmount else 17f + 13f * glassAmount
    val textColor = if (highContrast) {
        if (isDark) Color.White else Color.Black
    } else if (isDark) Color.White.copy(alpha = 0.94f) else Color(0xFF1E1F24)
    val mutedColor = textColor.copy(alpha = 0.62f)
    val innerSurface = if (highContrast) {
        if (isDark) Color.Black.copy(alpha = 0.72f) else Color.White.copy(alpha = 0.92f)
    } else if (isDark) Color.White.copy(alpha = 0.10f + 0.08f * glassAmount)
    else Color.White.copy(alpha = 0.50f + 0.20f * glassAmount)
    val sheetShape = RoundedCornerShape(26.dp)
    val textScale = flightTableTextScale(textZoom)

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 130)),
        exit = fadeOut(animationSpec = tween(durationMillis = 160))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (isDark) 0.34f else 0.16f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(
            animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing),
            initialOffsetY = { it / 2 }
        ) + fadeIn(animationSpec = tween(durationMillis = 170)) +
            scaleIn(animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing), initialScale = 0.94f),
        exit = slideOutVertically(
            animationSpec = tween(durationMillis = 210, easing = FastOutLinearInEasing),
            targetOffsetY = { it / 3 }
        ) + fadeOut(animationSpec = tween(durationMillis = 150)) +
            scaleOut(animationSpec = tween(durationMillis = 210, easing = FastOutLinearInEasing), targetScale = 0.98f)
    ) {
        Box(
            modifier = Modifier
                .padding(start = 6.dp, end = 6.dp, bottom = 68.dp)
                .navigationBarsPadding()
                .fillMaxWidth()
                .heightIn(max = 650.dp)
                .clip(sheetShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .adaptiveLiquidGlassBackdrop(
                    backdrop = backdrop,
                    shape = sheetShape,
                    surfaceColor = panelColor,
                    blurDp = sheetBlurDp,
                    shadow = null,
                    highlight = null,
                    refractionHeightDp = GlassChromeRefractionHeightDp,
                    refractionAmountDp = GlassChromeRefractionAmountDp
                )
                .background(overlayTint, sheetShape)
                .background(sheenBrush, sheetShape)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 86.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(42.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(mutedColor.copy(alpha = 0.34f))
                )
                FlightTableContent(
                    snapshot = snapshot,
                    mode = mode,
                    textColor = textColor,
                    mutedColor = mutedColor,
                    surface = innerSurface,
                    tablePalette = null,
                    textScale = textScale,
                    groupedFlights = groupedFlights,
                    highContrast = highContrast
                )
            }
        }
    }
}

@Composable
private fun FlightTableContent(
    snapshot: FlightTableSnapshot,
    mode: String,
    textColor: Color,
    mutedColor: Color,
    surface: Color,
    tablePalette: NativeFlightTablePalette?,
    textScale: Float,
    groupedFlights: Boolean,
    highContrast: Boolean
) {
    val isDeparture = mode == "departure"
    val rows = remember(snapshot, mode) {
        snapshot.rows.filter { row ->
            if (isDeparture) row.kind == "departure" else row.kind != "departure"
        }
    }
    val rowsByDay = remember(rows) {
        rows
            .groupBy { row -> row.day.ifBlank { "Flight schedule" } }
            .mapValues { (_, dayRows) -> flightTableSortedRows(dayRows) }
    }
    val title = if (isDeparture) "Departures" else "Arrivals"
    Text(
        text = title.uppercase(),
        color = textColor,
        style = MaterialTheme.typography.headlineSmall.copy(
            fontSize = 23.sp,
            lineHeight = 27.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.sp,
            textAlign = TextAlign.Center
        ),
        modifier = Modifier.fillMaxWidth(),
        maxLines = 1
    )

    if (rows.isEmpty()) {
        FlightTableLoadingSkeleton(
            placeLabel = if (isDeparture) "To" else "From",
            textColor = textColor,
            surface = surface,
            textScale = textScale,
            groupedFlights = groupedFlights,
            highContrast = highContrast
        )
    } else {
        rowsByDay.entries.forEachIndexed { index, entry ->
            if (index > 0) {
                Spacer(Modifier.height(4.dp))
            }
            FlightTableDayHeader(
                day = entry.key,
                total = entry.value.size,
                lastUpdated = snapshot.lastUpdated,
                textColor = textColor,
                mutedColor = mutedColor,
                surface = surface
            )
            FlightTableColumnHeader(
                placeLabel = if (isDeparture) "To" else "From",
                textColor = mutedColor,
                textScale = textScale
            )
            entry.value.forEach { row ->
                if (!groupedFlights) {
                    FlightTableRowCard(
                        row = row,
                        placeLabel = if (isDeparture) "To" else "From",
                        textColor = textColor,
                        mutedColor = mutedColor,
                        surface = surface,
                        tablePalette = tablePalette,
                        textScale = textScale,
                        highContrast = highContrast
                    )
                }
            }
            if (groupedFlights) {
                flightTableSortedAirlineGroups(entry.value).forEach { group ->
                    FlightTableAirlineGroupHeader(
                        airline = group.airline,
                        count = group.rows.size,
                        firstTime = group.rows.firstOrNull()?.sched.orEmpty(),
                        textColor = textColor,
                        mutedColor = mutedColor,
                        surface = surface,
                        textScale = textScale,
                        highContrast = highContrast
                    )
                    group.rows.forEach { row ->
                        FlightTableRowCard(
                            row = row,
                            placeLabel = if (isDeparture) "To" else "From",
                            textColor = textColor,
                            mutedColor = mutedColor,
                            surface = surface,
                            tablePalette = tablePalette,
                            textScale = textScale,
                            highContrast = highContrast
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberFlightSkeletonPulse(): Float {
    val transition = rememberInfiniteTransition(label = "flightSkeletonPulse")
    val alpha by transition.animateFloat(
        initialValue = 0.24f,
        targetValue = 0.58f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 880, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flightSkeletonAlpha"
    )
    return alpha
}

@Composable
private fun FlightSkeletonBone(
    modifier: Modifier,
    color: Color,
    alpha: Float,
    shape: Shape = RoundedCornerShape(999.dp)
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(color.copy(alpha = alpha.coerceIn(0f, 1f)))
    )
}

@Composable
private fun FlightTableLoadingSkeleton(
    placeLabel: String,
    textColor: Color,
    surface: Color,
    textScale: Float,
    groupedFlights: Boolean,
    highContrast: Boolean
) {
    val pulse = rememberFlightSkeletonPulse()
    FlightTableDayHeaderSkeleton(
        textColor = textColor,
        pulse = pulse
    )
    FlightTableColumnHeader(
        placeLabel = placeLabel,
        textColor = textColor.copy(alpha = 0.62f),
        textScale = textScale
    )
    if (groupedFlights) {
        FlightTableAirlineGroupHeaderSkeleton(
            textColor = textColor,
            surface = surface,
            pulse = pulse,
            highContrast = highContrast
        )
        repeat(4) { index ->
            FlightTableRowSkeleton(
                index = index,
                textColor = textColor,
                surface = surface,
                pulse = pulse,
                highContrast = highContrast
            )
        }
    } else {
        repeat(5) { index ->
            FlightTableRowSkeleton(
                index = index,
                textColor = textColor,
                surface = surface,
                pulse = pulse,
                highContrast = highContrast
            )
        }
    }
}

@Composable
private fun FlightTableDayHeaderSkeleton(
    textColor: Color,
    pulse: Float
) {
    val headerShape = RoundedCornerShape(999.dp)
    val isDark = isSystemInDarkTheme()
    val headerSurface = if (isDark) {
        Color(0xFF111111).copy(alpha = 0.88f)
    } else {
        Color.White.copy(alpha = 0.78f)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(headerShape)
            .background(headerSurface)
            .border(1.dp, flightItemBorderColor(isDark), headerShape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        FlightSkeletonBone(
            modifier = Modifier
                .fillMaxWidth(0.62f)
                .height(11.dp),
            color = textColor,
            alpha = pulse * 0.72f
        )
    }
}

@Composable
private fun FlightTableAirlineGroupHeaderSkeleton(
    textColor: Color,
    surface: Color,
    pulse: Float,
    highContrast: Boolean
) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(13.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (highContrast) surface else surface.copy(alpha = 0.76f))
            .border(1.dp, if (highContrast) textColor.copy(alpha = 0.34f) else flightItemBorderColor(isDark), shape)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FlightSkeletonBone(
            modifier = Modifier
                .width(104.dp)
                .height(12.dp),
            color = textColor,
            alpha = pulse
        )
        Spacer(Modifier.weight(1f))
        FlightSkeletonBone(
            modifier = Modifier
                .width(118.dp)
                .height(10.dp),
            color = textColor,
            alpha = pulse * 0.76f
        )
    }
}

@Composable
private fun FlightTableRowSkeleton(
    index: Int,
    textColor: Color,
    surface: Color,
    pulse: Float,
    highContrast: Boolean
) {
    val rowShape = RoundedCornerShape(18.dp)
    val isDark = isSystemInDarkTheme()
    val rowSurface = if (highContrast) {
        surface
    } else if (isDark) {
        Color.White.copy(alpha = 0.07f).compositeOver(surface)
    } else {
        Color.White.copy(alpha = 0.60f).compositeOver(surface)
    }
    val widths = when (index % 3) {
        0 -> Triple(110.dp, 72.dp, 54.dp)
        1 -> Triple(88.dp, 116.dp, 62.dp)
        else -> Triple(132.dp, 94.dp, 58.dp)
    }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(rowShape)
            .background(rowSurface)
            .border(1.dp, if (highContrast) textColor.copy(alpha = 0.30f) else flightItemBorderColor(isDark), rowShape)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FlightSkeletonBone(
                modifier = Modifier.size(38.dp),
                color = textColor,
                alpha = pulse * 0.82f
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FlightSkeletonBone(
                        modifier = Modifier
                            .width(widths.first)
                            .height(11.dp),
                        color = textColor,
                        alpha = pulse
                    )
                    Spacer(Modifier.weight(1f))
                    FlightSkeletonBone(
                        modifier = Modifier
                            .width(widths.third)
                            .height(22.dp),
                        color = textColor,
                        alpha = pulse * 0.78f
                    )
                }
                Spacer(Modifier.height(3.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FlightSkeletonBone(
                        modifier = Modifier
                            .width(widths.second)
                            .height(10.dp),
                        color = textColor,
                        alpha = pulse * 0.78f
                    )
                    Spacer(Modifier.weight(1f))
                    FlightSkeletonBone(
                        modifier = Modifier
                            .width(56.dp)
                            .height(9.dp),
                        color = textColor,
                        alpha = pulse * 0.58f
                    )
                    FlightSkeletonBone(
                        modifier = Modifier
                            .width(62.dp)
                            .height(9.dp),
                        color = textColor,
                        alpha = pulse * 0.52f
                    )
                }
            }
        }
    }
}

private fun flightTableTextScale(textZoom: Int): Float {
    return (textZoom.coerceIn(60, 100) / 100f).coerceIn(0.60f, 1f)
}

private fun flightTableScaledSp(minSp: Float, maxSp: Float, textScale: Float): androidx.compose.ui.unit.TextUnit {
    return (minSp + ((maxSp - minSp) * textScale.coerceIn(0.60f, 1f))).sp
}

private fun flightTableSortedRows(rows: List<FlightTableRow>): List<FlightTableRow> {
    return rows
        .mapIndexed { index, row -> index to row }
        .sortedWith(
            compareBy<Pair<Int, FlightTableRow>>(
                { flightTableRowSortMinutes(it.second) },
                { it.first }
            )
        )
        .map { it.second }
}

private fun flightTableSortedAirlineGroups(rows: List<FlightTableRow>): List<FlightTableAirlineGroup> {
    return rows
        .mapIndexed { index, row -> index to row }
        .groupBy { (_, row) -> row.airline.ifBlank { "Other" } }
        .map { (airline, indexedRows) ->
            val sortedRows = indexedRows
                .sortedWith(
                    compareBy<Pair<Int, FlightTableRow>>(
                        { flightTableRowSortMinutes(it.second) },
                        { it.first }
                    )
                )
                .map { it.second }
            FlightTableAirlineGroup(
                airline = airline,
                rows = sortedRows,
                firstTime = sortedRows.firstOrNull()?.let(::flightTableRowSortMinutes) ?: Int.MAX_VALUE
            )
        }
        .sortedWith(compareBy<FlightTableAirlineGroup> { it.firstTime }.thenBy { it.airline.lowercase() })
}

private fun flightTableRowSortMinutes(row: FlightTableRow): Int {
    val scheduled = parseFlightTableMinutes(row.sched)
    if (scheduled != Int.MAX_VALUE) return scheduled
    return parseFlightTableMinutes(row.actual)
}

private fun parseFlightTableMinutes(value: String): Int {
    if (value.isBlank()) return Int.MAX_VALUE
    val match = Regex("""(?i)(\d{1,2})(?:\s*:\s*(\d{2}))?\s*([ap])\.?\s*m?\.?""").find(value)
        ?: return Int.MAX_VALUE
    var hour = match.groupValues.getOrNull(1)?.toIntOrNull() ?: return Int.MAX_VALUE
    val minute = match.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() }?.toIntOrNull() ?: 0
    val marker = match.groupValues.getOrNull(3)?.lowercase().orEmpty()
    if (hour !in 1..12 || minute !in 0..59 || marker.isBlank()) return Int.MAX_VALUE
    if (marker == "p" && hour != 12) hour += 12
    if (marker == "a" && hour == 12) hour = 0
    return hour * 60 + minute
}

private const val FlightTableAirlineStart = 0.00f
private const val FlightTableFlightStart = 0.280f
private const val FlightTablePlaceStart = 0.395f
private const val FlightTableSchedStart = 0.600f
private const val FlightTableActualStart = 0.735f
private const val FlightTableStatusStart = 0.855f
private const val FlightTableEnd = 1.00f

@Composable
private fun FlightTableDayHeader(
    day: String,
    total: Int,
    lastUpdated: String,
    textColor: Color,
    mutedColor: Color,
    surface: Color
) {
    val headerShape = RoundedCornerShape(999.dp)
    val isDark = isSystemInDarkTheme()
    val updated = lastUpdated
        .replace("last updated", "updated", ignoreCase = true)
        .ifBlank { "updating" }
    val summary = "$day • $total flights • $updated"
    val headerSurface = if (isDark) {
        Color(0xFF111111).copy(alpha = 0.88f)
    } else {
        Color.White.copy(alpha = 0.78f)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(headerShape)
            .background(headerSurface)
            .border(1.dp, flightItemBorderColor(isDark), headerShape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = summary,
            color = textColor.copy(alpha = 0.82f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 10.8.sp,
                lineHeight = 12.2.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun FlightTableColumnHeader(
    placeLabel: String,
    textColor: Color,
    textScale: Float
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(31.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.105f) else Color.Black.copy(alpha = 0.075f))
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        val width = maxWidth
        FlightTablePositionedColumn(width, FlightTableAirlineStart, FlightTableFlightStart) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 48.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                FlightTableCell("Airline", textColor, header = true, textScale = textScale)
            }
        }
        FlightTablePositionedColumn(width, FlightTableFlightStart, FlightTablePlaceStart) {
            FlightTableCell("Flight", textColor, header = true, textScale = textScale)
        }
        FlightTablePositionedColumn(width, FlightTablePlaceStart, FlightTableSchedStart) {
            FlightTableCell(placeLabel, textColor, header = true, textScale = textScale)
        }
        FlightTablePositionedColumn(width, FlightTableSchedStart, FlightTableActualStart) {
            FlightTableCell("Sched", textColor, header = true, textScale = textScale)
        }
        FlightTablePositionedColumn(width, FlightTableActualStart, FlightTableStatusStart) {
            FlightTableCell("Actual", textColor, header = true, textScale = textScale)
        }
        FlightTablePositionedColumn(
            width = width,
            start = FlightTableStatusStart,
            end = FlightTableEnd,
            contentAlignment = Alignment.CenterEnd
        ) {
            FlightTableCell("Status", textColor, header = true, textScale = textScale, textAlign = TextAlign.End)
        }
    }
}

@Composable
private fun FlightTableAirlineGroupHeader(
    airline: String,
    count: Int,
    firstTime: String,
    textColor: Color,
    mutedColor: Color,
    surface: Color,
    textScale: Float,
    highContrast: Boolean
) {
    val shape = RoundedCornerShape(13.dp)
    val isDark = isSystemInDarkTheme()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (highContrast) surface else surface.copy(alpha = 0.76f))
            .border(1.dp, if (highContrast) textColor.copy(alpha = 0.34f) else flightItemBorderColor(isDark), shape)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = airline,
            color = textColor,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = flightTableScaledSp(10.2f, 11.3f, textScale),
                lineHeight = flightTableScaledSp(11.8f, 13f, textScale),
                fontWeight = FontWeight.Medium
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "$count flights" + firstTime.takeIf { it.isNotBlank() }?.let { " • first $it" }.orEmpty(),
            color = mutedColor,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = flightTableScaledSp(9.2f, 10.2f, textScale),
                lineHeight = flightTableScaledSp(10.5f, 11.8f, textScale),
                fontWeight = FontWeight.Medium
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FlightTableRowCard(
    row: FlightTableRow,
    placeLabel: String,
    textColor: Color,
    mutedColor: Color,
    surface: Color,
    tablePalette: NativeFlightTablePalette?,
    textScale: Float,
    highContrast: Boolean
) {
    val rowShape = RoundedCornerShape(18.dp)
    val isDark = isSystemInDarkTheme()
    val rowBorder = flightRowBorderColor(row, isDark, tablePalette)
    val cancelledStyledBorder = !highContrast && row.isCancelledFlight()
    val borderModifier = if (cancelledStyledBorder) {
        Modifier.border(
            width = if (isDark) 1.35.dp else 1.6.dp,
            brush = if (isDark) {
                Brush.linearGradient(
                    0.00f to Color(0xFFC99A36).copy(alpha = 0.54f),
                    0.42f to Color(0xFFFFD98A).copy(alpha = 0.30f),
                    0.70f to Color(0xFF5AC8FA).copy(alpha = 0.34f),
                    1.00f to Color(0xFFC99A36).copy(alpha = 0.46f)
                )
            } else {
                Brush.linearGradient(
                    0.00f to Color(0xFFD6A948).copy(alpha = 0.72f),
                    0.44f to Color(0xFFFFF0BC).copy(alpha = 0.58f),
                    0.68f to Color(0xFF8DD7F7).copy(alpha = 0.44f),
                    1.00f to Color(0xFFD6A948).copy(alpha = 0.62f)
                )
            },
            shape = rowShape
        )
    } else {
        Modifier.border(1.dp, if (highContrast) textColor.copy(alpha = 0.36f) else rowBorder, rowShape)
    }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(rowShape)
            .background(if (highContrast) surface else tableRowSurface(row, surface, isDark, tablePalette))
            .then(borderModifier)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AirlineBadge(airline = row.airline.ifBlank { "--" })
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = listOf(row.airline, row.flight).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "--" },
                        color = textColor,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = flightTableScaledSp(10.8f, 12f, textScale),
                            lineHeight = flightTableScaledSp(12f, 13.4f, textScale)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    FlightTableStatusPill(row = row, palette = tablePalette)
                }
                Spacer(Modifier.height(3.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = row.place.ifBlank { placeLabel },
                        color = textColor.copy(alpha = 0.88f),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = flightTableScaledSp(9.2f, 10.4f, textScale),
                            lineHeight = flightTableScaledSp(10.4f, 11.7f, textScale)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Sched ${row.sched.ifBlank { "--" }}",
                        color = mutedColor.copy(alpha = 0.88f),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = flightTableScaledSp(8.4f, 9.4f, textScale),
                            lineHeight = flightTableScaledSp(9.5f, 10.6f, textScale)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Actual ${row.actual.ifBlank { "--" }}",
                        color = mutedColor.copy(alpha = 0.88f),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = flightTableScaledSp(8.4f, 9.4f, textScale),
                            lineHeight = flightTableScaledSp(9.5f, 10.6f, textScale)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun FlightTablePositionedColumn(
    width: androidx.compose.ui.unit.Dp,
    start: Float,
    end: Float,
    contentAlignment: Alignment = Alignment.CenterStart,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .offset(x = width * start)
            .width(width * (end - start).coerceAtLeast(0.01f))
            .fillMaxHeight(),
        contentAlignment = contentAlignment
    ) {
        content()
    }
}

@Composable
private fun FlightTableAirlineCell(
    airline: String,
    color: Color,
    mutedColor: Color,
    textScale: Float,
    highContrast: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AirlineBadge(airline = airline)
        Text(
            text = airline,
            color = color,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Medium,
                fontSize = flightTableScaledSp(10.2f, 11.3f, textScale),
                lineHeight = flightTableScaledSp(11.5f, 12.8f, textScale)
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AirlineBadge(
    airline: String
) {
    val normalized = airline.lowercase()
    val isDark = isSystemInDarkTheme()
    val (code, accent) = when {
        normalized.contains("american") -> "AA" to Color(0xFF2F6FEA)
        normalized.contains("delta") -> "DL" to Color(0xFFD73737)
        normalized.contains("united") -> "UA" to Color(0xFF1FA9D8)
        normalized.contains("alaska") -> "AS" to Color(0xFF228E7F)
        normalized.contains("southwest") -> "WN" to Color(0xFFF3A11A)
        normalized.contains("jetblue") -> "B6" to Color(0xFF1F55C7)
        else -> airline
            .split(" ", "-", "/", ignoreCase = false)
            .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
            .take(2)
            .joinToString("")
            .ifBlank { "--" } to if (isDark) Color(0xFF6D827C) else Color(0xFF8EA59D)
    }
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(accent.copy(alpha = if (isDark) 0.92f else 0.86f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = code,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Black,
                fontSize = 10.5.sp,
                lineHeight = 11.5.sp,
                textAlign = TextAlign.Center
            ),
            maxLines = 1
        )
    }
}

@Composable
private fun FlightTableCell(
    text: String,
    color: Color,
    mutedColor: Color = color,
    header: Boolean = false,
    textScale: Float = 1f,
    textAlign: TextAlign = TextAlign.Start,
    highContrast: Boolean = false
) {
    Text(
        text = text,
        color = color,
        style = if (header) {
            MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = flightTableScaledSp(8.5f, 9.4f, textScale),
                lineHeight = flightTableScaledSp(9.6f, 10.7f, textScale)
            )
        } else {
            MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Medium,
                fontSize = flightTableScaledSp(10.1f, 11.3f, textScale),
                lineHeight = flightTableScaledSp(11.4f, 12.8f, textScale)
            )
        },
        textAlign = textAlign,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun FlightTableStatusPill(row: FlightTableRow, palette: NativeFlightTablePalette?) {
    val isDark = isSystemInDarkTheme()
    val arrivedAccent = palette?.arrivedAccent ?: if (isDark) Color(0xFF34D399) else Color(0xFF047857)
    val departedAccent = palette?.departedAccent ?: if (isDark) Color(0xFF5EEAD4) else Color(0xFF0F766E)
    val scheduleAccent = palette?.delayAccent ?: if (isDark) Color(0xFFFFB020) else Color(0xFFB45309)
    val onTimeAccent = palette?.arrivedAccent ?: if (isDark) Color(0xFF34D399) else Color(0xFF047857)
    val isCancelled = row.isCancelledFlight()
    val (label, accent) = when {
        isCancelled -> "Cancelled" to (palette?.cancelledAccent ?: Color(0xFFFF453A))
        row.isDivertedFlight() -> "Diverted" to (palette?.divertedAccent ?: Color(0xFFFF9F0A))
        row.delay > 0 -> "+${row.delay}" to scheduleAccent
        row.status.contains("arriv", ignoreCase = true) -> "Arrived" to arrivedAccent
        row.status.contains("depart", ignoreCase = true) -> "Departed" to departedAccent
        else -> row.status.ifBlank { "On time" } to onTimeAccent
    }
    val pillShape = RoundedCornerShape(999.dp)
    val pillBackground = if (isCancelled) {
        if (isDark) Color(0xFF12324C).copy(alpha = 0.86f) else Color(0xFFE3F4FF)
    } else {
        accent.copy(alpha = if (isDark) 0.20f else 0.14f)
    }
    val pillBorder = if (isCancelled) {
        if (isDark) Color(0xFF5AC8FA).copy(alpha = 0.48f) else Color(0xFF38BDF8).copy(alpha = 0.55f)
    } else {
        accent.copy(alpha = if (isDark) 0.24f else 0.22f)
    }
    val statusTextColor = if (isCancelled) {
        if (isDark) Color.White else Color(0xFF0F3A5A)
    } else {
        accent
    }
    Text(
        text = label,
        color = statusTextColor,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 10.4.sp,
            lineHeight = 11.5.sp
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(pillShape)
            .background(pillBackground)
            .border(1.dp, pillBorder, pillShape)
            .padding(horizontal = 7.dp, vertical = 6.dp)
    )
}

private fun tableRowSurface(
    row: FlightTableRow,
    fallback: Color,
    isDark: Boolean,
    palette: NativeFlightTablePalette?
): Color {
    return when {
        row.isCancelledFlight() -> if (isDark) {
            Color(0xFF351216).copy(alpha = 0.74f)
        } else {
            Color(0xFFFFE0DD).copy(alpha = 0.90f)
        }
        row.isDivertedFlight() -> palette?.divertedSurface ?: if (isDark) {
            Color(0xFF352407).copy(alpha = 0.74f)
        } else {
            Color(0xFFFFF0D6).copy(alpha = 0.84f)
        }
        row.delay > 0 -> palette?.delayedSurface ?: if (isDark) {
            Color(0xFF30250A).copy(alpha = 0.74f)
        } else {
            Color(0xFFFFF7DA).copy(alpha = 0.88f)
        }
        row.status.contains("arriv", ignoreCase = true) ||
            row.status.contains("depart", ignoreCase = true) -> palette?.arrivedSurface ?: if (isDark) {
            Color(0xFF063B2F).copy(alpha = 0.82f)
        } else {
            Color(0xFFE8F7EF).copy(alpha = 0.96f)
        }
        else -> fallback
    }
}

private fun flightItemBorderColor(isDark: Boolean): Color {
    return if (isDark) {
        Color.White.copy(alpha = 0.10f)
    } else {
        Color(0xFF6B7280).copy(alpha = 0.18f)
    }
}

private fun flightRowBorderColor(
    row: FlightTableRow,
    isDark: Boolean,
    palette: NativeFlightTablePalette?
): Color {
    return when {
        row.isCancelledFlight() -> if (isDark) {
            Color.Transparent
        } else {
            Color(0xFFD8B35A).copy(alpha = 0.46f)
        }
        row.isDivertedFlight() -> (palette?.divertedAccent ?: Color(0xFFFF9F0A)).copy(alpha = if (isDark) 0.34f else 0.28f)
        row.delay > 0 -> (palette?.delayAccent ?: Color(0xFFFFB020)).copy(alpha = if (isDark) 0.30f else 0.26f)
        row.status.contains("arriv", ignoreCase = true) ||
            row.status.contains("depart", ignoreCase = true) -> palette?.rowBorder ?: if (isDark) {
            Color(0xFF34D399).copy(alpha = 0.30f)
        } else {
            Color(0xFF047857).copy(alpha = 0.25f)
        }
        else -> flightItemBorderColor(isDark)
    }
}

@Composable
private fun ColumnScope.FlightLiveStatusContent(
    snapshot: FlightLiveStatusSnapshot,
    flightSnapshot: FlightSheetBrief,
    weather: FlightSheetWeather,
    textColor: Color,
    mutedColor: Color,
    surface: Color,
    lightLift: Boolean,
    textScale: Float,
    highContrast: Boolean,
    showHandle: Boolean,
    showRefresh: Boolean,
    title: String,
    onRefresh: () -> Unit
) {
    if (showHandle) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(42.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(mutedColor.copy(alpha = 0.34f))
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (showRefresh) 54.dp else 0.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (title == "Alerts") title.uppercase() else title,
                color = textColor,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 23.sp,
                    lineHeight = 27.sp,
                    letterSpacing = 0.sp,
                    textAlign = TextAlign.Center
                ),
                maxLines = 1,
                modifier = Modifier.fillMaxWidth()
            )
            if (snapshot.updatedLabel.isNotBlank()) {
                Text(
                    text = snapshot.updatedLabel,
                    color = mutedColor,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = (12f * textScale).sp,
                        lineHeight = (14f * textScale).sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        if (showRefresh) {
            var refreshSpinKey by remember { mutableIntStateOf(0) }
            val refreshRotation by animateFloatAsState(
                targetValue = refreshSpinKey * 360f,
                animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
                label = "flightAlertsRefreshRotation"
            )
            IconButton(
                modifier = Modifier.align(Alignment.CenterEnd),
                onClick = {
                    refreshSpinKey += 1
                    onRefresh()
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Refresh alerts",
                    tint = textColor,
                    modifier = Modifier.graphicsLayer {
                        rotationZ = refreshRotation
                    }
                )
            }
        }
    }

    val showLoadingSkeleton = snapshot.updatedLabel.isBlank() && snapshot.items.isEmpty()
    if (showLoadingSkeleton) {
        FlightLiveStatusLoadingSkeleton(
            textColor = textColor,
            surface = surface,
            lightLift = lightLift,
            highContrast = highContrast
        )
        return
    }

    FlightWeatherBanner(
        weather = weather,
        textColor = textColor,
        mutedColor = mutedColor,
        surface = surface,
        lightLift = lightLift,
        textScale = textScale
    )
    FlightIssueSummaryRow(
        brief = flightSnapshot,
        textColor = textColor,
        mutedColor = mutedColor,
        surface = surface,
        lightLift = lightLift,
        textScale = textScale,
        highContrast = highContrast
    )

    if (snapshot.items.isNotEmpty()) {
        snapshot.items.forEach { item ->
            FlightLiveStatusCard(
                item = item,
                textColor = textColor,
                mutedColor = mutedColor,
                surface = surface,
                lightLift = lightLift,
                textScale = textScale,
                highContrast = highContrast
            )
        }
    } else {
        FlightNoLiveStatusCard(
            textColor = textColor,
            mutedColor = mutedColor,
            surface = surface,
            lightLift = lightLift,
            textScale = textScale
        )
    }
}

@Composable
private fun FlightLiveStatusSheet(
    visible: Boolean,
    modifier: Modifier = Modifier,
    backdrop: LayerBackdrop,
    snapshot: FlightLiveStatusSnapshot,
    flightSnapshot: FlightSheetBrief,
    tableSnapshot: FlightTableSnapshot,
    weather: FlightSheetWeather,
    textZoom: Int,
    highContrast: Boolean,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val glassAmount = rememberLiquidGlassTintAmount()
    val panelColor = flightLanternSheetPanelColor(FlightAlertLantern, isDark, glassAmount, intensity = 0.52f)
    val overlayTint = flightLanternSheetOverlayColor(FlightAlertLantern, isDark, glassAmount, intensity = 0.48f)
    val sheenBrush = flightLanternSheetSheenBrush(FlightAlertLantern, isDark, glassAmount, intensity = 0.45f)
    val sheetBlurDp = if (isDark) 14f + 12f * glassAmount else 13f + 11f * glassAmount
    val textColor = if (highContrast) {
        if (isDark) Color.White else Color.Black
    } else if (isDark) Color.White.copy(alpha = 0.94f) else Color(0xFF1E1F24)
    val mutedColor = textColor.copy(alpha = 0.64f)
    val innerSurface = if (highContrast) {
        if (isDark) Color.Black.copy(alpha = 0.72f) else Color.White.copy(alpha = 0.92f)
    } else if (isDark) {
        Color.White.copy(alpha = 0.10f + 0.08f * glassAmount)
    } else {
        Color.White.copy(alpha = 0.50f + 0.20f * glassAmount)
    }
    val sheetShape = RoundedCornerShape(26.dp)
    val lightLift = !isDark
    val textScale = (textZoom.coerceIn(60, 100) / SettingsStore.DEFAULT_TEXT_ZOOM.toFloat())
        .coerceIn(0.82f, 1.12f)
    val effectiveFlightSnapshot = remember(flightSnapshot, tableSnapshot) {
        flightSnapshot.withCurrentTableCounts(tableSnapshot)
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 130)),
        exit = fadeOut(animationSpec = tween(durationMillis = 160))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (isDark) 0.34f else 0.16f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(
            animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing),
            initialOffsetY = { it / 2 }
        ) + fadeIn(animationSpec = tween(durationMillis = 170)) +
            scaleIn(
                animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing),
                initialScale = 0.94f
            ),
        exit = slideOutVertically(
            animationSpec = tween(durationMillis = 210, easing = FastOutLinearInEasing),
            targetOffsetY = { it / 3 }
        ) + fadeOut(animationSpec = tween(durationMillis = 150)) +
            scaleOut(
                animationSpec = tween(durationMillis = 210, easing = FastOutLinearInEasing),
                targetScale = 0.98f
            )
    ) {
        Box(
            modifier = Modifier
                .padding(
                    start = 6.dp,
                    end = 6.dp,
                    bottom = 68.dp
                )
                .navigationBarsPadding()
                .fillMaxWidth()
                .heightIn(max = 620.dp)
                .clip(sheetShape)
                .border(
                    1.dp,
                    if (isDark) Color(0xFF7DD3FC).copy(alpha = 0.20f)
                    else Color(0xFF2C8AA0).copy(alpha = 0.18f),
                    sheetShape
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .adaptiveLiquidGlassBackdrop(
                    backdrop = backdrop,
                    shape = sheetShape,
                    surfaceColor = panelColor,
                    blurDp = sheetBlurDp,
                    shadow = null,
                    highlight = null,
                    refractionHeightDp = GlassChromeRefractionHeightDp,
                    refractionAmountDp = GlassChromeRefractionAmountDp
                )
                .background(overlayTint, sheetShape)
                .background(sheenBrush, sheetShape)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 86.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FlightLiveStatusContent(
                    snapshot = snapshot,
                    flightSnapshot = effectiveFlightSnapshot,
                    weather = weather,
                    textColor = textColor,
                    mutedColor = mutedColor,
                    surface = innerSurface,
                    lightLift = lightLift,
                    textScale = textScale,
                    highContrast = highContrast,
                    showHandle = true,
                    showRefresh = true,
                    title = "Live arrival status",
                    onRefresh = onRefresh
                )
            }
        }
    }
}

private fun Modifier.lightThemeSurfaceLift(enabled: Boolean, shape: Shape): Modifier {
    return this
}

@Composable
private fun FlightLiveStatusLoadingSkeleton(
    textColor: Color,
    surface: Color,
    lightLift: Boolean,
    highContrast: Boolean
) {
    val pulse = rememberFlightSkeletonPulse()
    FlightWeatherBannerSkeleton(
        textColor = textColor,
        surface = surface,
        pulse = pulse,
        lightLift = lightLift,
        highContrast = highContrast
    )
    FlightIssueSummarySkeleton(
        textColor = textColor,
        surface = surface,
        pulse = pulse,
        lightLift = lightLift,
        highContrast = highContrast
    )
    repeat(2) { index ->
        FlightLiveStatusCardSkeleton(
            index = index,
            textColor = textColor,
            surface = surface,
            pulse = pulse,
            lightLift = lightLift,
            highContrast = highContrast
        )
    }
}

@Composable
private fun FlightWeatherBannerSkeleton(
    textColor: Color,
    surface: Color,
    pulse: Float,
    lightLift: Boolean,
    highContrast: Boolean
) {
    val isDark = isSystemInDarkTheme()
    val cardShape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .lightThemeSurfaceLift(lightLift, cardShape)
            .clip(cardShape)
            .background(surface)
            .border(1.dp, if (highContrast) textColor.copy(alpha = 0.30f) else flightItemBorderColor(isDark), cardShape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            FlightSkeletonBone(
                modifier = Modifier
                    .width(58.dp)
                    .height(30.dp),
                color = textColor,
                alpha = pulse * 0.84f,
                shape = RoundedCornerShape(12.dp)
            )
            FlightSkeletonBone(
                modifier = Modifier
                    .weight(1f)
                    .height(13.dp),
                color = textColor,
                alpha = pulse
            )
        }
        FlightSkeletonBone(
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .height(11.dp),
            color = textColor,
            alpha = pulse * 0.70f
        )
    }
}

@Composable
private fun FlightIssueSummarySkeleton(
    textColor: Color,
    surface: Color,
    pulse: Float,
    lightLift: Boolean,
    highContrast: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(3) { index ->
                FlightCountPillSkeleton(
                    index = index,
                    textColor = textColor,
                    pulse = pulse,
                    lightLift = lightLift,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        FlightSkeletonBone(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .fillMaxWidth(0.52f)
                .height(10.dp),
            color = textColor,
            alpha = pulse * 0.62f
        )
        FlightAlertsSummaryBoxSkeleton(
            textColor = textColor,
            surface = surface,
            pulse = pulse,
            lightLift = lightLift,
            highContrast = highContrast
        )
    }
}

@Composable
private fun FlightCountPillSkeleton(
    index: Int,
    textColor: Color,
    pulse: Float,
    lightLift: Boolean,
    modifier: Modifier = Modifier
) {
    val accents = listOf(Color(0xFF5AC8FA), Color(0xFF7C8CFF), Color(0xFFFFB020))
    val accent = accents[index % accents.size]
    val pillShape = RoundedCornerShape(999.dp)
    Row(
        modifier = modifier
            .lightThemeSurfaceLift(lightLift, pillShape)
            .clip(pillShape)
            .background(accent.copy(alpha = 0.14f))
            .border(1.dp, accent.copy(alpha = 0.22f), pillShape)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        FlightSkeletonBone(
            modifier = Modifier
                .width(16.dp)
                .height(14.dp),
            color = accent,
            alpha = pulse
        )
        Spacer(Modifier.width(5.dp))
        FlightSkeletonBone(
            modifier = Modifier
                .width(48.dp)
                .height(10.dp),
            color = textColor,
            alpha = pulse * 0.58f
        )
    }
}

@Composable
private fun FlightAlertsSummaryBoxSkeleton(
    textColor: Color,
    surface: Color,
    pulse: Float,
    lightLift: Boolean,
    highContrast: Boolean
) {
    val isDark = isSystemInDarkTheme()
    val cardShape = RoundedCornerShape(19.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .lightThemeSurfaceLift(lightLift, cardShape)
            .clip(cardShape)
            .background(if (highContrast) surface else Color.White.copy(alpha = if (isDark) 0.11f else 0.46f).compositeOver(surface))
            .border(1.dp, if (highContrast) textColor.copy(alpha = 0.30f) else flightItemBorderColor(isDark), cardShape)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                FlightSkeletonBone(
                    modifier = Modifier
                        .fillMaxWidth(0.52f)
                        .height(13.dp),
                    color = textColor,
                    alpha = pulse
                )
                FlightSkeletonBone(
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .height(10.dp),
                    color = textColor,
                    alpha = pulse * 0.62f
                )
            }
            FlightSkeletonBone(
                modifier = Modifier
                    .width(58.dp)
                    .height(24.dp),
                color = textColor,
                alpha = pulse * 0.52f
            )
        }
        repeat(2) { index ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(textColor.copy(alpha = pulse * if (index == 0) 0.22f else 0.16f))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FlightSkeletonBone(
                    modifier = Modifier
                        .width(54.dp)
                        .height(11.dp),
                    color = textColor,
                    alpha = pulse * 0.82f
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FlightSkeletonBone(
                        modifier = Modifier
                            .fillMaxWidth(0.64f)
                            .height(10.dp),
                        color = textColor,
                        alpha = pulse
                    )
                    FlightSkeletonBone(
                        modifier = Modifier
                            .fillMaxWidth(0.42f)
                            .height(8.dp),
                        color = textColor,
                        alpha = pulse * 0.58f
                    )
                }
            }
        }
    }
}

@Composable
private fun FlightLiveStatusCardSkeleton(
    index: Int,
    textColor: Color,
    surface: Color,
    pulse: Float,
    lightLift: Boolean,
    highContrast: Boolean
) {
    val isDark = isSystemInDarkTheme()
    val accent = if (index % 2 == 0) Color(0xFF5AC8FA) else Color(0xFFFFB020)
    val cardShape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .lightThemeSurfaceLift(lightLift, cardShape)
            .clip(cardShape)
            .background(surface)
            .background(
                Brush.horizontalGradient(
                    0f to accent.copy(alpha = if (isDark) 0.18f else 0.10f),
                    0.45f to accent.copy(alpha = if (isDark) 0.10f else 0.06f),
                    1f to Color.Transparent
                )
            )
            .border(1.dp, if (highContrast) textColor.copy(alpha = 0.30f) else accent.copy(alpha = if (isDark) 0.28f else 0.22f), cardShape)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FlightSkeletonBone(
                    modifier = Modifier
                        .width(if (index % 2 == 0) 92.dp else 116.dp)
                        .height(14.dp),
                    color = textColor,
                    alpha = pulse
                )
                Spacer(Modifier.weight(1f))
                FlightSkeletonBone(
                    modifier = Modifier
                        .width(54.dp)
                        .height(22.dp),
                    color = accent,
                    alpha = pulse * 0.76f
                )
                FlightSkeletonBone(
                    modifier = Modifier
                        .width(64.dp)
                        .height(22.dp),
                    color = accent,
                    alpha = pulse * 0.58f
                )
            }
            Spacer(Modifier.height(8.dp))
            FlightSkeletonBone(
                modifier = Modifier
                    .fillMaxWidth(if (index % 2 == 0) 0.80f else 0.68f)
                    .height(12.dp),
                color = textColor,
                alpha = pulse * 0.76f
            )
            Spacer(Modifier.height(6.dp))
            FlightSkeletonBone(
                modifier = Modifier
                    .fillMaxWidth(if (index % 2 == 0) 0.54f else 0.74f)
                    .height(10.dp),
                color = textColor,
                alpha = pulse * 0.54f
            )
        }
    }
}

@Composable
private fun FlightWeatherBanner(
    weather: FlightSheetWeather,
    textColor: Color,
    mutedColor: Color,
    surface: Color,
    lightLift: Boolean,
    textScale: Float
) {
    if (weather.temp.isBlank() && weather.summary.isBlank()) return
    val conditionLabel = when (weather.condition.lowercase()) {
        "sunny" -> "Sunny"
        "partly" -> "Partly cloudy"
        "cloudy" -> "Cloudy"
        "rain" -> "Rain"
        "thunder" -> "Storms"
        "night" -> "Night"
        else -> "Airport weather"
    }
    val conditionAccent = when (weather.condition.lowercase()) {
        "sunny" -> Color(0xFFFFB020)
        "partly" -> Color(0xFF5AC8FA)
        "cloudy" -> Color(0xFF9BA7B7)
        "rain" -> Color(0xFF4DA3FF)
        "thunder" -> Color(0xFFFF9F0A)
        else -> Color(0xFF5AC8FA)
    }
    val cardShape = RoundedCornerShape(18.dp)
    val isDark = isSystemInDarkTheme()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .lightThemeSurfaceLift(lightLift, cardShape)
            .clip(cardShape)
            .background(surface)
            .border(1.dp, flightItemBorderColor(isDark), cardShape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text(
                text = weather.temp.ifBlank { "--" },
                color = textColor,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = (16f * textScale).sp,
                    lineHeight = (18f * textScale).sp
                ),
                maxLines = 1,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(conditionAccent.copy(alpha = 0.18f))
                    .padding(horizontal = 9.dp, vertical = 6.dp)
            )
            Text(
                text = conditionLabel,
                color = textColor,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = (14f * textScale).sp,
                    lineHeight = (17f * textScale).sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (weather.summary.isNotBlank()) {
            Text(
                text = weather.summary,
                color = mutedColor,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = (12f * textScale).sp,
                    lineHeight = (14f * textScale).sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FlightIssueSummaryRow(
    brief: FlightSheetBrief,
    textColor: Color,
    mutedColor: Color,
    surface: Color,
    lightLift: Boolean,
    textScale: Float,
    highContrast: Boolean
) {
    val countedAlerts = brief.delayedCount + brief.cancelledCount + brief.divertedCount
    val totalAlerts = if (countedAlerts > brief.issues.size) countedAlerts else brief.issues.size
    val hasIssues = totalAlerts > 0
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FlightCountPill("${brief.arrivalCount}", "Arrivals", Color(0xFF5AC8FA), textColor, Modifier.weight(1f), lightLift, textScale)
            FlightCountPill("${brief.departureCount}", "Departures", Color(0xFF7C8CFF), textColor, Modifier.weight(1f), lightLift, textScale)
            FlightCountPill(
                "$totalAlerts",
                "Alerts",
                if (hasIssues) Color(0xFFFFB020) else Color(0xFF34C759),
                textColor,
                Modifier.weight(1f),
                lightLift,
                textScale
            )
        }
        FlightAlertScheduleContextLine(
            brief = brief,
            mutedColor = mutedColor,
            textScale = textScale
        )
        if (brief.issues.isNotEmpty() || hasIssues) {
            FlightAlertsSummaryBox(
                brief = brief,
                totalAlerts = totalAlerts,
                textColor = textColor,
                mutedColor = mutedColor,
                surface = surface,
                lightLift = lightLift,
                textScale = textScale,
                highContrast = highContrast
            )
        } else if (brief.summary.isNotBlank()) {
            Text(
                text = brief.summary,
                color = mutedColor,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FlightAlertScheduleContextLine(
    brief: FlightSheetBrief,
    mutedColor: Color,
    textScale: Float
) {
    val countedDay = compactFlightDayLabel(brief.scheduleDayLabel)
    val upcomingDay = compactFlightDayLabel(brief.upcomingDayLabel)
    val text = when {
        countedDay.isNotBlank() && upcomingDay.isNotBlank() -> "Counts for $countedDay • $upcomingDay upcoming"
        countedDay.isNotBlank() -> "Counts for $countedDay"
        upcomingDay.isNotBlank() -> "$upcomingDay upcoming"
        else -> ""
    }
    if (text.isBlank()) return
    Text(
        text = text,
        color = mutedColor.copy(alpha = 0.82f),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Medium,
            fontSize = (10f * textScale).sp,
            lineHeight = (11.5f * textScale).sp
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth()
    )
}

private fun compactFlightDayLabel(label: String): String {
    return label
        .replace("Sunday", "Sun", ignoreCase = true)
        .replace("Monday", "Mon", ignoreCase = true)
        .replace("Tuesday", "Tue", ignoreCase = true)
        .replace("Wednesday", "Wed", ignoreCase = true)
        .replace("Thursday", "Thu", ignoreCase = true)
        .replace("Friday", "Fri", ignoreCase = true)
        .replace("Saturday", "Sat", ignoreCase = true)
        .replace(",", "")
        .replace(Regex("\\s+"), " ")
        .trim()
}

@Composable
private fun FlightNoLiveStatusCard(
    textColor: Color,
    mutedColor: Color,
    surface: Color,
    lightLift: Boolean,
    textScale: Float
) {
    val isDark = isSystemInDarkTheme()
    val cardShape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .lightThemeSurfaceLift(lightLift, cardShape)
            .clip(cardShape)
            .background(surface)
            .border(1.dp, flightItemBorderColor(isDark), cardShape)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            text = "No active arrival cards",
            color = textColor,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Black,
                fontSize = (14f * textScale).sp,
                lineHeight = (16f * textScale).sp
            )
        )
        Text(
            text = "Current schedule counts are still shown above.",
            color = mutedColor,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = (12f * textScale).sp,
                lineHeight = (14f * textScale).sp
            )
        )
    }
}

@Composable
private fun FlightAlertsSummaryBox(
    brief: FlightSheetBrief,
    totalAlerts: Int,
    textColor: Color,
    mutedColor: Color,
    surface: Color,
    lightLift: Boolean,
    textScale: Float,
    highContrast: Boolean
) {
    val isDark = isSystemInDarkTheme()
    val cardShape = RoundedCornerShape(19.dp)
    val accent = when {
        brief.cancelledCount > 0 -> Color(0xFFFF453A)
        brief.divertedCount > 0 -> Color(0xFFFF9F0A)
        brief.delayedCount > 0 -> Color(0xFFFFB020)
        else -> Color(0xFF22C55E)
    }
    val boxSurface = if (highContrast) {
        surface
    } else if (isDark) {
        Color.White.copy(alpha = 0.12f).compositeOver(Color.Black.copy(alpha = 0.12f))
    } else {
        Color.White.copy(alpha = 0.48f).compositeOver(surface)
    }
    val boxBorder = if (brief.cancelledCount > 0) {
        Color.Transparent
    } else if (isDark) {
        accent.copy(alpha = 0.52f)
    } else {
        accent.copy(alpha = 0.26f)
    }
    var expanded by rememberSaveable(brief.summary, brief.issues.size, totalAlerts) { mutableStateOf(false) }
    val previewCount = 3
    val issuesToShow = if (expanded) brief.issues else brief.issues.take(previewCount)
    val hiddenCount = (brief.issues.size - issuesToShow.size).coerceAtLeast(0)
    val issueWord = if (totalAlerts == 1) "alert" else "alerts"
    val visibleLine = when {
        totalAlerts > brief.issues.size && brief.issues.isNotEmpty() -> "${brief.issues.size} visible below"
        brief.issues.isEmpty() && totalAlerts > 0 -> "Waiting for detailed alert rows"
        else -> "Schedule details below"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .lightThemeSurfaceLift(lightLift, cardShape)
            .clip(cardShape)
            .background(boxSurface)
            .border(1.dp, boxBorder, cardShape)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$totalAlerts total $issueWord",
                    color = textColor,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = (13f * textScale).sp,
                        lineHeight = (15f * textScale).sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = visibleLine,
                    color = mutedColor,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = (10.5f * textScale).sp,
                        lineHeight = (12f * textScale).sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (brief.issues.size > previewCount) {
                Text(
                    text = if (expanded) "Show less" else "Show all",
                    color = if (isDark) Color.White else accent,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = (10.5f * textScale).sp,
                        lineHeight = (12f * textScale).sp
                    ),
                    maxLines = 1,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(accent.copy(alpha = if (isDark) 0.22f else 0.14f))
                        .border(1.dp, accent.copy(alpha = if (isDark) 0.34f else 0.24f), RoundedCornerShape(999.dp))
                        .clickable { expanded = !expanded }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
        if (issuesToShow.isNotEmpty()) {
            issuesToShow.forEach { issue ->
                FlightIssuePill(issue = issue, textColor = textColor, mutedColor = mutedColor, lightLift = false, textScale = textScale)
            }
            if (!expanded && hiddenCount > 0) {
                Text(
                    text = "$hiddenCount more alerts hidden",
                    color = mutedColor,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = (10.5f * textScale).sp,
                        lineHeight = (12f * textScale).sp
                    ),
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else if (brief.summary.isNotBlank()) {
            Text(
                text = brief.summary,
                color = mutedColor,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = (11.5f * textScale).sp,
                    lineHeight = (13.5f * textScale).sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FlightCountPill(
    value: String,
    label: String,
    accent: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    lightLift: Boolean,
    textScale: Float
) {
    val pillShape = RoundedCornerShape(999.dp)
    val isDark = isSystemInDarkTheme()
    Row(
        modifier = modifier
            .lightThemeSurfaceLift(lightLift, pillShape)
            .clip(pillShape)
            .background(accent.copy(alpha = 0.16f))
            .border(1.dp, accent.copy(alpha = if (isDark) 0.28f else 0.24f), pillShape)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = value,
            color = accent,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Black,
                fontSize = (14f * textScale).sp,
                lineHeight = (16f * textScale).sp
            )
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            color = textColor.copy(alpha = 0.76f),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = (10f * textScale).sp,
                lineHeight = (11f * textScale).sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FlightIssuePill(
    issue: FlightSheetIssue,
    textColor: Color,
    mutedColor: Color,
    lightLift: Boolean,
    textScale: Float
) {
    val accent = when {
        issue.tone.contains("cancel", ignoreCase = true) -> Color(0xFFFF453A)
        issue.tone.contains("divert", ignoreCase = true) -> Color(0xFFFF9F0A)
        issue.label.contains("+") || issue.tone.contains("delay", ignoreCase = true) -> Color(0xFFFFB020)
        else -> Color(0xFF5AC8FA)
    }
    val pillShape = RoundedCornerShape(16.dp)
    val isDark = isSystemInDarkTheme()
    val isCancelled = issue.tone.contains("cancel", ignoreCase = true)
    val isDiverted = issue.tone.contains("divert", ignoreCase = true)
    val isDelayed = issue.label.contains("+") || issue.tone.contains("delay", ignoreCase = true)
    val rowSurface = if (isDark) {
        when {
            isCancelled -> Color(0xFF5C1E24).copy(alpha = 0.92f)
            isDiverted -> Color(0xFF51330A).copy(alpha = 0.90f)
            isDelayed -> Color(0xFF44320A).copy(alpha = 0.88f)
            else -> accent.copy(alpha = 0.22f)
        }
    } else {
        accent.copy(alpha = 0.13f)
    }
    val rowBorder = if (isCancelled) Color.Transparent
    else accent.copy(alpha = if (isDark) 0.42f else 0.24f)
    val labelColor = if (isCancelled) {
        if (isDark) Color(0xFFFF7A72) else Color(0xFFB42318)
    } else {
        accent
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .lightThemeSurfaceLift(lightLift, pillShape)
            .clip(pillShape)
            .background(rowSurface)
            .border(1.dp, rowBorder, pillShape)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = issue.label.ifBlank { "Alert" },
            color = labelColor,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Black,
                fontSize = (12f * textScale).sp,
                lineHeight = (14f * textScale).sp
            ),
            maxLines = 1,
            modifier = Modifier.widthIn(min = 58.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = issue.flight.ifBlank { "Flight update" },
                color = textColor,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = (13f * textScale).sp,
                    lineHeight = (15f * textScale).sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = listOf(issue.route, issue.time).filter { it.isNotBlank() }.joinToString(" • "),
                color = mutedColor,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = (10f * textScale).sp,
                    lineHeight = (11f * textScale).sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FlightLiveStatusCard(
    item: FlightLiveStatusItem,
    textColor: Color,
    mutedColor: Color,
    surface: Color,
    lightLift: Boolean,
    textScale: Float,
    highContrast: Boolean
) {
    val isDark = isSystemInDarkTheme()
    val arrivedAccent = if (isDark) Color(0xFF34D399) else Color(0xFF047857)
    val scheduleAccent = if (isDark) Color(0xFFFFC14D) else Color(0xFFB45309)
    val upcomingAccent = if (isDark) Color(0xFF5AC8FA) else Color(0xFF245B78)
    val accent = when (item.tone) {
        "arrived" -> arrivedAccent
        "delayed" -> scheduleAccent
        else -> upcomingAccent
    }
    val cardSurface = if (!isDark && item.tone == "arrived") {
        Color(0xFFE8F8F0).copy(alpha = 0.96f)
    } else if (highContrast) {
        surface
    } else {
        surface
    }
    val cardShape = RoundedCornerShape(if (item.tone == "arrived") 18.dp else 20.dp)
    val compactRoute = compactFlightRoute(item.route)
    val statusLine = compactFlightStatusLine(item, compactRoute)
    val detailLine = compactFlightDetailLine(item)
    val progress = item.effectiveProgress()
    val fillFraction = (progress / 100f).coerceIn(0.04f, 1f)
    val fillEdge = (fillFraction + 0.001f).coerceAtMost(1f)
    val progressFill = accent.copy(alpha = if (isDark) 0.26f else 0.18f)
    val progressFeather = accent.copy(alpha = if (isDark) 0.13f else 0.09f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .lightThemeSurfaceLift(lightLift, cardShape)
            .clip(cardShape)
            .background(cardSurface)
            .background(
                Brush.horizontalGradient(
                    0f to progressFill,
                    (fillFraction * 0.88f).coerceIn(0f, 1f) to progressFill,
                    fillFraction to progressFeather,
                    fillEdge to Color.Transparent,
                    1f to Color.Transparent
                )
            )
            .border(1.dp, accent.copy(alpha = if (isDark) 0.28f else 0.24f), cardShape)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = if (item.tone == "arrived") 10.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(if (item.tone == "arrived") 5.dp else 7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.flight,
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = (14f * textScale).sp,
                            lineHeight = (16f * textScale).sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    modifier = Modifier.widthIn(max = 170.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.delayLabel.isNotBlank()) {
                        FlightTinyPill(text = readableFlightPillText(item.delayLabel), accent = scheduleAccent)
                    }
                    if (item.badge.isNotBlank() || item.status.isNotBlank()) {
                        FlightTinyPill(text = readableFlightPillText(item.badge.ifBlank { item.status }), accent = accent)
                    }
                }
            }
            if (statusLine.isNotBlank()) {
                Text(
                    text = statusLine,
                    color = textColor.copy(alpha = 0.82f),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = (12f * textScale).sp,
                        lineHeight = (14f * textScale).sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (detailLine.isNotBlank()) {
                Text(
                    text = detailLine,
                    color = mutedColor,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = (12f * textScale).sp,
                        lineHeight = (14f * textScale).sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun compactFlightRoute(route: String): String {
    return route
        .replace("→", "-")
        .replace(Regex("\\s*-\\s*"), " - ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

private fun compactFlightStatusLine(item: FlightLiveStatusItem, route: String): String {
    if (item.tone == "arrived") return route
    val status = item.status.ifBlank { "Scheduled" }
    val eta = item.etaText
        .replace("Scheduled in ", "", ignoreCase = true)
        .replace(" remaining", "", ignoreCase = true)
        .trim()
    return buildString {
        append(status)
        if (route.isNotBlank()) append(" ").append(route)
        if (eta.isNotBlank()) append(" in ").append(eta)
    }.trim()
}

private fun compactFlightDetailLine(item: FlightLiveStatusItem): String {
    if (item.tone == "arrived") {
        return item.detail.replace(Regex(",\\s*"), " at ").trim()
    }
    val sched = Regex("""Sched\s+([^•]+)""", RegexOption.IGNORE_CASE)
        .find(item.detail)
        ?.groupValues
        ?.getOrNull(1)
        ?.trim()
        .orEmpty()
    val est = Regex("""Est\s+([^•]+)""", RegexOption.IGNORE_CASE)
        .find(item.detail)
        ?.groupValues
        ?.getOrNull(1)
        ?.trim()
        .orEmpty()
    return listOf(
        sched,
        est.takeIf { it.isNotBlank() }?.let { "Est $it" }.orEmpty()
    ).filter { it.isNotBlank() }.joinToString("  ").ifBlank { item.detail }
}

private fun readableFlightPillText(value: String): String {
    return value
        .trim()
        .lowercase(Locale.US)
        .split(Regex("\\s+"))
        .joinToString(" ") { word ->
            word.replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(Locale.US) else char.toString()
            }
        }
}

private fun FlightLiveStatusItem.effectiveProgress(): Float {
    if (tone == "arrived") return 100f
    val etaMinutes = parseFlightEtaMinutes(etaText)
    val timeProgress = etaMinutes?.let { ((180f - it) / 180f * 100f).coerceIn(4f, 96f) }
    return listOfNotNull(progress.takeIf { it > 4f }, timeProgress).maxOrNull() ?: 4f
}

private fun parseFlightEtaMinutes(value: String): Int? {
    val text = value.lowercase(Locale.US)
    if (text.isBlank()) return null
    val hours = Regex("""(\d+)\s*h""").find(text)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
    val minutes = Regex("""(\d+)\s*m""").find(text)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
    val total = hours * 60 + minutes
    return total.takeIf { it > 0 }
}

@Composable
private fun FlightTinyPill(
    text: String,
    accent: Color,
    filled: Boolean = true
) {
    val pillShape = RoundedCornerShape(999.dp)
    Text(
        text = text,
        color = if (filled) Color.White else accent,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Black,
            fontSize = 10.sp,
            lineHeight = 11.sp
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(pillShape)
            .background(accent.copy(alpha = if (filled) 0.86f else 0.16f))
            .border(1.dp, accent.copy(alpha = if (filled) 0.22f else 0.30f), pillShape)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
private fun NativeFlightBottomTabs(
    selected: String,
    backdrop: LayerBackdrop,
    contentView: android.view.View?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassBottomTabBar(
        modifier = modifier,
        backdrop = backdrop,
        contentView = contentView,
        tabs = listOf(
            GlassBottomTabItem(
                label = "Arrivals",
                icon = Icons.Filled.Flight,
                selected = selected == "arrivals",
                onClick = { onSelect("arrivals") },
                lanternColor = FlightArrivalLantern
            ),
            GlassBottomTabItem(
                label = "Departures",
                icon = Icons.Filled.Flight,
                selected = selected == "departures",
                onClick = { onSelect("departures") },
                lanternColor = FlightDepartureLantern
            ),
            GlassBottomTabItem(
                label = "Alerts",
                icon = Icons.Filled.Info,
                selected = selected == "alerts",
                onClick = { onSelect("alerts") },
                lanternColor = FlightAlertLantern
            ),
            GlassBottomTabItem(
                label = "Menu",
                icon = Icons.Filled.Menu,
                selected = selected == "menu",
                onClick = { onSelect("menu") }
            )
        )
    )
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




