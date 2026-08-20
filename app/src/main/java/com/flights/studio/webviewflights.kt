package com.flights.studio


import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Build
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.DismissibleNavigationDrawer
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import com.flights.studio.FlightsTabsInjector.injectHideTriggers
import com.flights.studio.SettingsStore.prefs
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.kyant.shapes.Capsule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

internal val FlightArrivalLantern = Color(0xFFF70D1A)
internal val FlightDepartureLantern = Color(0xFF4D4DFF)
internal val FlightAlertLantern = Color(0xFF2FEF73)

private val FlightAlertReadableDarkPanel = Color(0xFF06281B)

private val FlightAlertReadableDarkOverlay = Color(0xFF0B6A42).copy(alpha = 0.18f)

private val FlightAlertReadableDarkSurface = Color(0xFF1F2329)

private val FlightAlertItemDarkSurface = Color(0xFF23262F)

private val FlightAlertItemLightSurface = Color(0xFFDFF7FF)

private val FlightArrivalCountAccent = Color(0xFFFF4696)

private val FlightDepartureCountAccent = Color(0xFF0057FF)

private val FlightAlertCountAccent = Color(0xFFFC6C26)

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

@Composable
private fun rememberValidatedInternetState(context: Context): Boolean {
    var online by remember(context) { mutableStateOf(hasInternet(context)) }
    DisposableEffect(context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val mainHandler = Handler(Looper.getMainLooper())
        fun updateOnline(value: Boolean) {
            mainHandler.post { online = value }
        }
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                updateOnline(hasInternet(context))
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                updateOnline(
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                )
            }

            override fun onLost(network: Network) {
                updateOnline(hasInternet(context))
            }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        runCatching { cm.registerNetworkCallback(request, callback) }
        online = hasInternet(context)
        onDispose {
            runCatching { cm.unregisterNetworkCallback(callback) }
        }
    }
    return online
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
    var flightRefreshSignal by rememberSaveable { mutableIntStateOf(0) }
    val flightRefreshRotation by animateFloatAsState(
        targetValue = flightRefreshSignal * 360f,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "flightTopBarRefreshRotation"
    )
    var liveStatusJson by rememberSaveable { mutableStateOf(SettingsStore.flightLiveStatusSnapshot(context)) }
    var flightBriefJson by rememberSaveable { mutableStateOf(SettingsStore.flightBriefSnapshot(context)) }
    var flightTableJson by rememberSaveable { mutableStateOf(SettingsStore.flightTableSnapshot(context)) }
    var weatherJson by rememberSaveable { mutableStateOf(SettingsStore.briefingWeatherSnapshot(context)) }
    fun selectedFlightTableTab(): String =
        when (flightTableMode) {
            "departure" -> "departures"
            "transportation" -> "transportation"
            else -> "arrivals"
        }
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
    val online = rememberValidatedInternetState(context)

    LaunchedEffect(cardId, flightWebView, online) {
        val webView = flightWebView ?: return@LaunchedEffect
        if (cardId != "card3" || !online) return@LaunchedEffect
        webView.evaluateJavascript(
            "try{window.fsRefreshNativeLiveStatus&&window.fsRefreshNativeLiveStatus()}catch(e){}",
            null
        )
        webView.reload()
        while (true) {
            delay(10_000L.milliseconds)
            webView.evaluateJavascript(
                "try{window.fsRefreshNativeLiveStatus&&window.fsRefreshNativeLiveStatus()}catch(e){}",
                null
            )
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
        "transportation" -> "Transportation"
        else -> "Arrivals"
    }
    var previousCard by remember { mutableStateOf(startCardId) }

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

    fun refreshNativeFlights() {
        flightRefreshSignal += 1
        flightWebView?.evaluateJavascript(
            "try{window.fsRefreshNativeLiveStatus&&window.fsRefreshNativeLiveStatus()}catch(e){}",
            null
        )
        flightWebView?.reload()
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
            val appThemePalette = LocalAppThemePalette.current
            val flightMenuPanelColor = appThemePalette.glass.copy(alpha = if (isDarkTheme) 0.42f else 0.54f)
            val flightMenuOverlayTint = appThemePalette.glassOverlay.copy(alpha = if (isDarkTheme) 0.04f else 0.05f)
            val flightMenuButtonColor = appThemePalette.action
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
                        refreshSignal = flightRefreshSignal
                    )
                }

                NativeFlightBottomTabs(
                    selected = selectedFlightTab,
                    backdrop = activeFlightBackdrop,
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
                                    refreshNativeFlights()
                                } else {
                                    val shouldClose = showFlightAlertsSheet && sameTabSelected
                                    showFlightTableSheet = false
                                    showFlightAlertsSheet = !shouldClose
                                    selectedFlightTab = if (shouldClose) selectedFlightTableTab() else next
                                    if (!shouldClose) {
                                        refreshNativeFlights()
                                    }
                                }
                            }
                            "transportation" -> {
                                val shouldClose = showFlightTableSheet && sameTabSelected && flightTableMode == "transportation"
                                showFlightAlertsSheet = false
                                showFlightMenuSheet = false
                                flightTableMode = "transportation"
                                if (cardId == "card3") {
                                    selectedFlightTab = next
                                    lastFlightContentTab = next
                                    showFlightTableSheet = false
                                } else {
                                    showFlightTableSheet = !shouldClose
                                    selectedFlightTab = if (shouldClose) selectedFlightTableTab() else next
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
                    refreshSignal = flightRefreshSignal,
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
                    title = if (cardId == "card3") flightSectionTitle else screenTitle,
                    showRefresh = cardId == "card3",
                    refreshRotation = flightRefreshRotation,
                    onRefresh = ::refreshNativeFlights
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
    modifier: Modifier = Modifier,
    showRefresh: Boolean = false,
    refreshRotation: Float = 0f,
    onRefresh: () -> Unit = {}
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
                .padding(start = 18.dp, end = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedContent(
                targetState = title,
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
            if (showRefresh) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .shadow(
                                elevation = if (isDark) 1.dp else 3.dp,
                                shape = RoundedCornerShape(999.dp),
                                clip = false
                            )
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (isDark) Color(0xFF202734) else Color(0xFFFCFCFD))
                            .clickable(onClick = onRefresh),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh flights",
                            tint = contentColor,
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer { rotationZ = refreshRotation }
                        )
                    }
                }
            }
        }
    }
}

private fun flightTopBarStatusLabel(
    selected: String,
    tableSnapshot: FlightTableSnapshot,
    liveStatusSnapshot: FlightLiveStatusSnapshot
): String {
    return when (selected) {
        "alerts" -> liveStatusSnapshot.updatedLabel.ifBlank {
            tableSnapshot.lastUpdated.ifBlank { "Updated just now" }
        }
        "transportation" -> "Jackson Hole Airport"
        else -> tableSnapshot.lastUpdated.ifBlank { "Updated just now" }
    }
}

private fun flightUpdatedPillText(label: String): String {
    val cleaned = label
        .replace("last updated", "updated", ignoreCase = true)
        .replace("Last Updated", "Updated", ignoreCase = true)
        .trim()
    return when {
        cleaned.isBlank() -> "Updated just now"
        cleaned.startsWith("updated", ignoreCase = true) -> cleaned.replaceFirstChar { it.uppercaseChar() }
        else -> "Updated $cleaned"
    }
}

@Composable
private fun FlightUpdatedStatusPill(
    label: String,
    textColor: Color,
    mutedColor: Color,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val pillTextColor = if (isDark) textColor.copy(alpha = 0.72f) else mutedColor
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = flightUpdatedPillText(label),
            color = pillTextColor,
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 10.5.sp,
                lineHeight = 12.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .widthIn(max = 190.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(if (isDark) Color(0xFF202734) else Color(0xFFF0F2F7))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
fun LegalHtmlScreen(
    html: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val isDark = isSystemInDarkTheme()
    val palette = LocalAppThemePalette.current
    val roles = appThemeSurfaceRoles(palette, isDark)
    val textColor = roles.title
    val linkColor = palette.action
    val topPaddingPx = (108f * density).toInt()
    val sidePaddingPx = (18f * density).toInt()
    val bottomPaddingPx = (126f * density).toInt()

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            Modifier
                .matchParentSize()
                .background(roles.page)
        )

        ProfileBackdropImageLayer(
            modifier = Modifier.matchParentSize(),
            lightRes = R.drawable.light_grid_pattern,
            darkRes = R.drawable.dark_grid_pattern,
            imageAlpha = if (isDark) 0.72f else 0.42f,
            scrimDark = 0.04f,
            scrimLight = 0.00f
        )
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            roles.page.copy(alpha = if (isDark) 0.58f else 0.36f),
                            roles.glassCard.copy(alpha = if (isDark) 0.44f else 0.34f),
                            palette.surfaceVariant.copy(alpha = if (isDark) 0.34f else 0.28f)
                        ),
                        start = Offset.Zero,
                        end = Offset(900f, 1400f)
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 12.dp, end = 12.dp, top = 108.dp, bottom = 100.dp)
        ) {
            AppThemeSectionSurface(shape = RoundedCornerShape(24.dp)) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = {
                        val scrollView = ScrollView(context).apply {
                            isFillViewport = true
                            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            clipToPadding = false
                        }

                        val textView = TextView(context).apply {
                            movementMethod = LinkMovementMethod.getInstance()
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            includeFontPadding = false
                        }

                        scrollView.addView(textView)
                        scrollView
                    },
                    update = { scrollView ->
                        val textView = scrollView.getChildAt(0) as TextView
                        textView.setPadding(
                            sidePaddingPx,
                            sidePaddingPx,
                            sidePaddingPx,
                            bottomPaddingPx - topPaddingPx
                        )
                        textView.text = HtmlCompat.fromHtml(
                            html,
                            HtmlCompat.FROM_HTML_MODE_COMPACT
                        )
                        textView.setTextColor(textColor.toArgb())
                        textView.setLinkTextColor(linkColor.toArgb())
                        textView.textSize = 16f
                        textView.setLineSpacing(7f * density, 1.14f)
                    }
                )
            }
        }
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
        html.fs-ai-performance *, html.fs-ai-performance *::before, html.fs-ai-performance *::after {
          animation-duration: 0.001ms !important;
          animation-iteration-count: 1 !important;
          transition-duration: 0.001ms !important;
          scroll-behavior: auto !important;
        }
        html.fs-ai-performance iframe:not([src*="maps"]),
        html.fs-ai-performance video,
        html.fs-ai-performance .elementor-background-video-container {
          display: none !important;
        }
        html.fs-ai-performance #flight-container,
        html.fs-ai-performance #flight-container table,
        html.fs-ai-performance #flight-container tbody {
          content-visibility: auto;
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
    reduceMotion: Boolean,
    aiPerformance: Boolean = false
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
          root.classList.toggle('fs-ai-performance', $aiPerformance);
          if ($aiPerformance) {
            Array.prototype.forEach.call(document.images || [], function(img) {
              img.loading = 'lazy';
              img.decoding = 'async';
              if (!img.closest('#flight-container')) img.fetchPriority = 'low';
            });
            Array.prototype.forEach.call(document.querySelectorAll('video, iframe[src*="youtube"], iframe[src*="vimeo"], .elementor-background-video-container'), function(el) {
              if (!el.closest('#flight-container')) el.remove();
            });
          }
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

private fun applyFlightWebPerformanceSettings(
    webView: WebView,
    aiPerformance: Boolean,
    cachePages: Boolean
) {
    webView.overScrollMode = if (aiPerformance) {
        WebView.OVER_SCROLL_NEVER
    } else {
        WebView.OVER_SCROLL_IF_CONTENT_SCROLLS
    }
    webView.isSaveEnabled = !aiPerformance
    webView.settings.apply {
        cacheMode = if (cachePages || aiPerformance) WebSettings.LOAD_CACHE_ELSE_NETWORK else WebSettings.LOAD_DEFAULT
        databaseEnabled = true
        domStorageEnabled = true
        loadsImagesAutomatically = true
        blockNetworkImage = false
        mediaPlaybackRequiresUserGesture = aiPerformance
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            offscreenPreRaster = aiPerformance
        }
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        webView.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, aiPerformance)
    }
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

@Suppress("unused") // Called by JavaScript through FlightsAndroidBridge.
private class FlightBriefBridge(
    context: Context,
    private val onLiveStatusSnapshot: (String) -> Unit,
    private val onFlightBriefSnapshot: (String) -> Unit,
    private val onFlightTableSnapshot: (String) -> Unit,
    private val onWeatherSnapshot: (String) -> Unit
) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastLiveStatusSignature: String? = null

    @JavascriptInterface
    fun updateFlightBriefSnapshot(json: String) {
        SettingsStore.setFlightBriefSnapshot(appContext, json)
        mainHandler.post { onFlightBriefSnapshot(json) }
    }

    @JavascriptInterface
    fun updateFlightLiveStatusSnapshot(json: String) {
        val signature = stableFlightLiveStatusSignature(json)
        if (signature == lastLiveStatusSignature) return
        lastLiveStatusSignature = signature
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

private fun stableFlightLiveStatusSignature(json: String): String {
    return runCatching {
        val root = JSONObject(json)
        val items = root.optJSONArray("items") ?: JSONArray()
        buildString {
            append(root.optString("day"))
            append('|')
            for (index in 0 until items.length()) {
                val item = items.optJSONObject(index) ?: continue
                append(item.optString("flight"))
                append('~')
                append(item.optString("route"))
                append('~')
                append(item.optString("status"))
                append('~')
                append(item.optString("detail"))
                append('~')
                append(item.optString("tone"))
                append('~')
                append(item.optString("badge"))
                append('~')
                append(item.optString("etaText"))
                append('~')
                append(item.optDouble("progress", -1.0))
                append('|')
            }
        }
    }.getOrElse { json }
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
    val effectiveCachePages = remember(settingsRevision, aiPerformance, cachePages) { cachePages || aiPerformance }
    val hwAccel = remember(settingsRevision, aiPerformance) { SettingsStore.hardwareAccel(context) || aiPerformance }
    val textZoomPref = remember(settingsRevision, aiPerformance) {
        SettingsStore.textZoom(context).let { if (aiPerformance) it.coerceAtLeast(95) else it }
    }
    val webTheme = remember(settingsRevision) { SettingsStore.webTheme(context) }
    val baseWebColor = if (isDark) Color(0xFF2B2924) else Color(0xFFF4F1E9)
    val url = remember(cardId) { urlForCard(cardId) }
    val online = rememberValidatedInternetState(context)

    var progress by remember(url) { mutableIntStateOf(0) }
    var showError by remember(url) { mutableStateOf(false) }
    var reloadTick by remember(url) { mutableIntStateOf(0) }
    var loadedRootUrl by remember { mutableStateOf<String?>(null) }
    var animatedRootUrl by remember { mutableStateOf<String?>(null) }
    var currentPageUrl by remember(url) { mutableStateOf(url) }

    val adHosts = remember {
        listOf(
            "doubleclick.net",
            "googlesyndication.com",
            "google-analytics.com",
            "googletagmanager.com",
            "facebook.net",
            "hotjar.com",
            "scorecardresearch.com"
        )
    }

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
            overScrollMode = if (aiPerformance) WebView.OVER_SCROLL_NEVER else WebView.OVER_SCROLL_IF_CONTENT_SCROLLS
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
                cacheMode = if (effectiveCachePages) WebSettings.LOAD_CACHE_ELSE_NETWORK else WebSettings.LOAD_DEFAULT
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
            applyFlightWebPerformanceSettings(this, aiPerformance, effectiveCachePages)

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
                    if ((SettingsStore.blockTrackers(context) || SettingsStore.aiPerformance(context)) && adHosts.any { host.contains(it) }) {
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
                        SettingsStore.reduceWebMotion(context) && !SettingsStore.aiPerformance(context),
                        SettingsStore.aiPerformance(context)
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

    LaunchedEffect(online, url, showError) {
        if (online && showError) {
            reloadTick += 1
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
                wv.settings.cacheMode = if (effectiveCachePages) {
                    WebSettings.LOAD_CACHE_ELSE_NETWORK
                } else {
                    WebSettings.LOAD_DEFAULT
                }
                applyFlightWebPerformanceSettings(wv, aiPerformance, effectiveCachePages)
                injectWebRuntimePreferences(wv, webTheme, textZoomPref, groupedFlights, highContrastWeb, reduceWebMotion, aiPerformance)

                if (loadedRootUrl != url) {

                    if (!online) {
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
                    if (!online) {
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
                            .clip(RoundedCornerShape(999.dp))
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
private data class FlightParkingAvailability(
    val percent: Int,
    val statusLabel: String,
    val updatedLabel: String
)

@Stable
private data class FlightTransportProvider(
    val name: String,
    val phone: String
)

private val FlightTransportationProviders = listOf(
    FlightTransportProvider("A Black Car Service", "(307) 413-2572"),
    FlightTransportProvider("Alpine Taxi", "(307) 203-6290"),
    FlightTransportProvider("Backcountry Safaris", "(307) 413-9300"),
    FlightTransportProvider("Bison Transportation", "(307) 699-9017"),
    FlightTransportProvider("Black Diamond Transportation", "(307) 203-5990"),
    FlightTransportProvider("Broncs Car Service / Elite Car", "(307) 413-9863"),
    FlightTransportProvider("Flex Ride Taxi", "(307) 413-9080"),
    FlightTransportProvider("Grand Teton Limo Services", "(307) 371-4020"),
    FlightTransportProvider("Holiday Motor Coach", "(208) 529-3900"),
    FlightTransportProvider("Innovative Transportation Solutions", "(602) 453-0001"),
    FlightTransportProvider("JAC Transportation", "(307) 699-3113"),
    FlightTransportProvider("JH Cab", "(307) 203-5066"),
    FlightTransportProvider("Luxury Car Service", "(307) 249-6579"),
    FlightTransportProvider("Mountain Mike's Taxi", "(307) 774-1000"),
    FlightTransportProvider("Mountain Resort Services", "(307) 690-1459"),
    FlightTransportProvider("Mountain View Taxi", "(307) 690-8069"),
    FlightTransportProvider("OK Taxi Service", "(307) 200-1818"),
    FlightTransportProvider("Old West Transportation Services", "(307) 690-8898"),
    FlightTransportProvider("RKM Luxury Transportation", "(612) 581-7624"),
    FlightTransportProvider("Saddle Up", "(307) 231-6630"),
    FlightTransportProvider("Snake River Transportation", "(307) 413-9009"),
    FlightTransportProvider("Teton Private Car Services", "(307) 413-4408"),
    FlightTransportProvider("United Car Service", "(307) 413-2032"),
    FlightTransportProvider("VIP Car Service", "(307) 699-8455")
)

private const val FlightArrivalsHeaderImageUrl =
    "https://lh3.googleusercontent.com/pw/AP1GczMPrWwVOjV7Tqv-5NNc2x2tOSzQ9f2rzEvCTA16GgM3uzBnk-V8ZjFuGMawoeMJ1z4ig9WFCHF0WOS-CHhsHQxu83QAaZBSPB-2W3SqZ9BTQN0UGrQC=w2048-h2048"
private const val FlightDeparturesHeaderImageUrl =
    "https://lh3.googleusercontent.com/pw/AP1GczOMjm6Y19cL8wkbVnhyu-AnitFXtLaUx-V4rciZ_4l_W_EU9XUY_KC9y3z8APK65CGQWJ94xsG-_ybTQrT_cwK8z4id0ogyE5wfS7gXEU7bbyUsfaic=w2048-h2048"
private const val FlightAlertsHeaderImageUrl =
    "https://lh3.googleusercontent.com/pw/AP1GczMExSHkbai0vIIxlNkjlskdNusp-IxwuORKNC7z1BohtCdIF_xUqdU_4I48elgs4k4tYeuMscEzWHIiEHOfWHh0vUsWmQonJSyVHVvCaPPLhJVdDOQi=w1418-h945-p-k"
private const val FlightTransportationHeaderImageUrl =
    "https://www.jacksonholeairport.com/wp-content/uploads/2022/08/Transportation-1-1418x945.jpg"

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
    val todayDay = snapshot.days
        .map { it.label }
        .firstOrNull { label -> label.isNotBlank() && isTodayFlightDayLabel(label) && snapshot.rows.any { row -> row.day == label } }
        ?: snapshot.rows
            .map { it.day }
            .firstOrNull { label -> label.isNotBlank() && isTodayFlightDayLabel(label) }
    val firstDay = todayDay ?: snapshot.days
        .map { it.label }
        .firstOrNull { label -> label.isNotBlank() && snapshot.rows.any { row -> row.day == label } }
    ?: snapshot.rows.firstOrNull { it.day.isNotBlank() }?.day
    ?: snapshot.days.firstOrNull { it.arrivals > 0 || it.departures > 0 }?.label
    ?: return snapshot.rows
    return snapshot.rows.filter { it.day == firstDay }.ifEmpty { snapshot.rows }
}

private fun fallbackFlightLiveStatusItems(snapshot: FlightTableSnapshot): List<FlightLiveStatusItem> {
    val rows = currentFlightRows(snapshot)
    if (rows.isEmpty()) return emptyList()
    val issueRows = rows.filter { row ->
        row.delay > 0 ||
                row.isCancelledFlight() ||
                row.isDivertedFlight()
    }
    val arrivalRows = flightTableSortedRows(rows.filter { row -> row.kind != "departure" })
    val departureRows = flightTableSortedRows(rows.filter { row -> row.kind == "departure" })
    val normalArrivalRows = arrivalRows.filterNot { row ->
        row.delay > 0 ||
                row.isCancelledFlight() ||
                row.isDivertedFlight()
    }
    val normalDepartureRows = departureRows.filterNot { row ->
        row.delay > 0 ||
                row.isCancelledFlight() ||
                row.isDivertedFlight()
    }
    val sourceRows = if (issueRows.isNotEmpty()) {
        normalArrivalRows.ifEmpty { normalDepartureRows }
    } else {
        arrivalRows.ifEmpty { departureRows.ifEmpty { rows } }
    }
    return sourceRows
        .take(10)
        .map { row -> row.toFallbackLiveStatusItem() }
}

private fun FlightTableRow.toFallbackLiveStatusItem(): FlightLiveStatusItem {
    val isDeparture = kind == "departure"
    val isArrived = !isDeparture &&
            (tone.contains("arriv", ignoreCase = true) ||
                    status.contains("arriv", ignoreCase = true) ||
                    actual.isNotBlank())
    val itemTone = when {
        isCancelledFlight() -> "cancelled"
        isDivertedFlight() -> "diverted"
        delay > 0 -> "delayed"
        isArrived -> "arrived"
        else -> tone.ifBlank { "scheduled" }
    }
    val route = when {
        place.isBlank() -> if (isDeparture) "JAC departure" else "JAC arrival"
        isDeparture -> "JAC to $place"
        else -> "$place to JAC"
    }
    val detailParts = mutableListOf<String>()
    if (sched.isNotBlank()) detailParts += if (isCancelledFlight()) "Scheduled $sched" else "Sched $sched"
    if (actual.isNotBlank() && actual != sched) {
        detailParts += if (isDeparture) "Departed $actual" else "Arrived $actual"
    }
    val fallbackStatus = when {
        status.isNotBlank() -> status
        isArrived -> "Arrived"
        isDeparture -> "Scheduled departure"
        else -> "Scheduled arrival"
    }
    val badgeText = when {
        isCancelledFlight() -> "Cancelled"
        isDivertedFlight() -> "Diverted"
        delay > 0 -> "+${delay} min"
        status.isNotBlank() -> status
        else -> if (isDeparture) "Departure" else "Arrival"
    }
    return FlightLiveStatusItem(
        flight = "$airline $flight".trim().ifBlank { flight.ifBlank { "Flight" } },
        route = route,
        status = fallbackStatus,
        detail = detailParts.joinToString(", ").ifBlank { day.ifBlank { "Time pending" } },
        tone = itemTone,
        badge = badgeText,
        pill = if (isDeparture) "Departure" else "Arrival",
        etaText = "",
        meta = day,
        delayLabel = if (delay > 0) "+${delay} min" else "",
        progress = when {
            isArrived -> 100f
            isCancelledFlight() || isDivertedFlight() -> 0f
            delay > 0 -> 58f
            else -> 12f
        }
    )
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

private fun flightResolvedWeatherCondition(weather: FlightSheetWeather): String {
    val conditionText = weather.condition.trim().lowercase(Locale.US)
    val summaryText = weather.summary.trim().lowercase(Locale.US)
    if (conditionText in setOf("sunny", "clear", "partly", "cloudy", "rain", "rain_heavy", "thunder", "fog", "hail", "mix", "snow", "night", "partly_night")) {
        return flightWeatherVisualCondition(conditionText)
    }
    val raw = when {
        conditionText.contains("thunder") || summaryText.contains("thunder") -> "thunder"
        conditionText.contains("hail") || summaryText.contains("hail") -> "hail"
        conditionText.contains("snow") || summaryText.contains("snow") -> "snow"
        conditionText.contains("sleet") || summaryText.contains("sleet") ||
                conditionText.contains("mix") || summaryText.contains("wintry mix") -> "mix"
        conditionText.contains("heavy rain") || summaryText.contains("heavy rain") -> "rain_heavy"
        conditionText.contains("rain") || summaryText.contains("rain") -> "rain"
        conditionText.contains("fog") || summaryText.contains("fog") -> "fog"
        conditionText.contains("partly") || summaryText.contains("partly") -> "partly"
        conditionText.contains("clear") || summaryText.contains("clear") -> "sunny"
        conditionText.contains("sun") || summaryText.contains("sun") -> "sunny"
        conditionText.contains("cloud") -> "cloudy"
        summaryText.contains("cloud") -> ""
        else -> conditionText
    }
    val normalizedRaw = when (raw) {
        "clear" -> "sunny"
        "storm" -> "thunder"
        else -> raw
    }
    val cloudPercent = Regex("""Cloud\s+(\d+)%""", RegexOption.IGNORE_CASE)
        .find(weather.summary)
        ?.groupValues
        ?.getOrNull(1)
        ?.toIntOrNull()
    val resolved = if (normalizedRaw in setOf("rain", "rain_heavy", "thunder", "fog", "hail", "mix", "snow")) {
        normalizedRaw
    } else if (cloudPercent != null) {
        when {
            cloudPercent >= 70 -> "cloudy"
            cloudPercent >= 30 -> "partly"
            else -> normalizedRaw.ifBlank { "sunny" }
        }
    } else {
        normalizedRaw.ifBlank { "sunny" }
    }
    return flightWeatherVisualCondition(resolved)
}

private fun flightWeatherVisualCondition(condition: String): String {
    val normalized = condition.ifBlank { "sunny" }.lowercase(Locale.US)
    return if (isJacksonHoleNightForFlights() && (normalized == "sunny" || normalized == "clear" || normalized == "partly")) {
        if (normalized == "partly") "partly_night" else "night"
    } else {
        normalized
    }
}

private fun isJacksonHoleNightForFlights(): Boolean {
    val hour = Calendar.getInstance(TimeZone.getTimeZone("America/Denver")).get(Calendar.HOUR_OF_DAY)
    return hour !in 6..19
}

private fun flightWeatherSymbolName(condition: String): String {
    return when (condition) {
        "night" -> "moon_stars"
        "partly_night" -> "partly_cloudy_night"
        "partly" -> "partly_cloudy_day"
        "cloudy" -> "cloud"
        "fog" -> "foggy"
        "rain_heavy" -> "rainy_heavy"
        "rain" -> "rainy"
        "thunder" -> "thunderstorm"
        "hail" -> "weather_hail"
        "mix" -> "weather_mix"
        "snow" -> "weather_snowy"
        else -> "sunny"
    }
}

private fun flightWeatherIconColor(condition: String, isDark: Boolean): Color {
    return when (condition) {
        "sunny", "partly" -> if (isDark) Color(0xFFFFD45A) else Color(0xFFB45309)
        "thunder" -> if (isDark) Color(0xFFFFE066) else Color(0xFF7C3E00)
        "rain", "rain_heavy" -> if (isDark) Color(0xFF7DD3FC) else Color(0xFF2563EB)
        "hail", "mix", "snow" -> if (isDark) Color(0xFFE0F2FE) else Color(0xFF1D4ED8)
        "fog", "cloudy" -> if (isDark) Color(0xFFD8E2EE) else Color(0xFF63779B)
        "night", "partly_night" -> if (isDark) Color(0xFFBFD7FF) else Color(0xFF315EA8)
        else -> if (isDark) Color(0xFFD8E2EE) else Color(0xFF63779B)
    }
}

@Composable
private fun FlightWeatherConditionIcon(
    condition: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val symbolFont = remember {
        FontFamily(
            Font(
                resId = R.font.material_symbols_rounded,
                variationSettings = FontVariation.Settings(
                    FontVariation.Setting("FILL", 1f),
                    FontVariation.Setting("wght", 400f),
                    FontVariation.Setting("GRAD", 0f),
                    FontVariation.Setting("opsz", 24f)
                )
            )
        )
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = flightWeatherSymbolName(condition),
            color = tint,
            fontFamily = symbolFont,
            fontWeight = FontWeight.Normal,
            fontSize = 22.sp,
            lineHeight = 24.sp,
            maxLines = 1
        )
    }
}

private suspend fun fetchFlightParkingAvailability(): FlightParkingAvailability? = withContext(Dispatchers.IO) {
    runCatching {
        val connection = (URL("https://www.jacksonholeairport.com/wp-admin/admin-ajax.php?action=parking").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8_000
            readTimeout = 8_000
            setRequestProperty("User-Agent", "JAC-Air-Tracker/Android")
            setRequestProperty("Accept", "text/html, */*")
        }
        try {
            if (connection.responseCode !in 200..299) return@runCatching null
            val html = connection.inputStream.bufferedReader().use { it.readText() }
            parseFlightParkingAvailability(html)
        } finally {
            connection.disconnect()
        }
    }.getOrNull()
}

private fun parseFlightParkingAvailability(html: String): FlightParkingAvailability? {
    if (html.isBlank()) return null
    val statusRaw = Regex("""(?is)lot-status[^>]*>\s*([^<]+)""")
        .find(html)
        ?.groupValues
        ?.getOrNull(1)
        ?.cleanParkingText()
        .orEmpty()
    val updatedRaw = Regex("""(?is)-updated[^>]*>\s*([^<]+)""")
        .find(html)
        ?.groupValues
        ?.getOrNull(1)
        ?.cleanParkingText()
        .orEmpty()
    val percent = Regex("""(\d{1,3})\s*%""")
        .find(statusRaw)
        ?.groupValues
        ?.getOrNull(1)
        ?.toIntOrNull()
        ?.coerceIn(0, 100)
        ?: return null
    return FlightParkingAvailability(
        percent = percent,
        statusLabel = statusRaw.ifBlank { "$percent% AVAILABLE" },
        updatedLabel = updatedRaw
    )
}

private fun String.cleanParkingText(): String {
    return HtmlCompat.fromHtml(this, HtmlCompat.FROM_HTML_MODE_LEGACY)
        .toString()
        .replace(Regex("\\s+"), " ")
        .trim()
}

private fun nativeFlightTablePalette(
    webTheme: String,
    isDark: Boolean,
    glassAmount: Float
): NativeFlightTablePalette {
    val effectiveTheme = when (webTheme.lowercase()) {
        "auto" -> if (isDark) "dark" else "light"
        "dark", "ocean", "alpine_night", "pine_night", "storm_night", "spring_night", "summer_night", "autumn_night", "winter_night" -> webTheme.lowercase()
        "spring", "summer", "autumn", "winter" -> if (isDark) "${webTheme.lowercase()}_night" else webTheme.lowercase()
        "mint", "sky", "violet", "rose", "amber", "gray", "light" -> if (isDark) {
            when (webTheme.lowercase()) {
                "mint", "amber" -> "pine_night"
                "violet", "rose" -> "storm_night"
                "sky", "light" -> "alpine_night"
                else -> "dark"
            }
        } else {
            webTheme.lowercase()
        }
        else -> if (isDark) "dark" else "light"
    }
    val accent = when (effectiveTheme) {
        "mint" -> Color(0xFF22B981)
        "sky" -> Color(0xFF3B82F6)
        "ocean" -> Color(0xFF7DD3FC)
        "alpine_night" -> Color(0xFF8EDBFF)
        "pine_night" -> Color(0xFF62E6B5)
        "storm_night" -> Color(0xFFA9B8FF)
        "spring" -> Color(0xFF23B26D)
        "summer" -> Color(0xFFFFB000)
        "autumn" -> Color(0xFFD85A16)
        "winter" -> Color(0xFF2F7FE8)
        "spring_night" -> Color(0xFF8CFF6B)
        "summer_night" -> Color(0xFF61E7FF)
        "autumn_night" -> Color(0xFFFFB23F)
        "winter_night" -> Color(0xFF9AD7FF)
        "violet" -> Color(0xFF8B5CF6)
        "rose" -> Color(0xFFEC4899)
        "amber" -> Color(0xFFF59E0B)
        "gray" -> Color(0xFF64748B)
        "dark" -> Color(0xFF7DD3FC)
        else -> Color(0xFF5AC8FA)
    }
    return if (isDark || effectiveTheme == "dark" || effectiveTheme == "ocean") {
        val darkPage = when (effectiveTheme) {
            "ocean" -> Color(0xFF071820)
            "alpine_night" -> Color(0xFF07131E)
            "pine_night" -> Color(0xFF071710)
            "storm_night" -> Color(0xFF0B0D18)
            "spring_night" -> Color(0xFF07170B)
            "summer_night" -> Color(0xFF061B22)
            "autumn_night" -> Color(0xFF171107)
            "winter_night" -> Color(0xFF07111C)
            else -> Color(0xFF07111C)
        }
        val darkSurface = when (effectiveTheme) {
            "pine_night" -> Color(0xFF10251C)
            "storm_night" -> Color(0xFF151B2C)
            "alpine_night" -> Color(0xFF101B28)
            "spring_night" -> Color(0xFF132B18)
            "summer_night" -> Color(0xFF102D34)
            "autumn_night" -> Color(0xFF2F2110)
            "winter_night" -> Color(0xFF132437)
            else -> Color(0xFF101B27)
        }
        val baseSurface = accent.copy(alpha = 0.08f).compositeOver(darkSurface.copy(alpha = 0.76f))
        val panel = accent.copy(alpha = 0.025f + 0.05f * glassAmount)
            .compositeOver(darkPage.copy(alpha = 0.72f + 0.22f * glassAmount))
        NativeFlightTablePalette(
            page = darkPage,
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
            "spring" -> Color(0xFFE2FFD9)
            "summer" -> Color(0xFFFFF2C7)
            "autumn" -> Color(0xFFFFE0C2)
            "winter" -> Color(0xFFDCEEFF)
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
            "spring" -> Color(0xFF0E3318)
            "summer" -> Color(0xFF332300)
            "autumn" -> Color(0xFF3D1800)
            "winter" -> Color(0xFF12283E)
            "violet" -> Color(0xFF261B3F)
            "rose" -> Color(0xFF3E122A)
            "amber" -> Color(0xFF34230C)
            "gray" -> Color(0xFF1F2937)
            else -> Color(0xFF1E1F24)
        }
        val readableAccent = when (effectiveTheme) {
            "mint" -> Color(0xFF0F6B4A)
            "sky" -> Color(0xFF075985)
            "spring" -> Color(0xFF1C7A3A)
            "summer" -> Color(0xFF9A6400)
            "autumn" -> Color(0xFF9A3A09)
            "winter" -> Color(0xFF1D5FAE)
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
private fun FlightSeasonalFlightBackdrop(
    webTheme: String,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val season = when (webTheme.lowercase(Locale.US)) {
        "spring", "spring_night" -> "spring"
        "autumn", "autumn_night" -> "autumn"
        "winter", "winter_night" -> "winter"
        "summer", "summer_night" -> "summer"
        else -> return
    }
    val transition = rememberInfiniteTransition(label = "seasonalFlightBackdrop")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "seasonalFlightDrift"
    )
    Canvas(modifier = modifier) {
        val alpha = if (isDark) 0.22f else 0.30f
        val wave = kotlin.math.sin(drift * Math.PI.toFloat() * 2f)
        when (season) {
            "spring" -> {
                val leafBase = if (isDark) Color(0xFF8CFF6B) else Color(0xFF1F8F3A)
                val bloomBase = if (isDark) Color(0xFFFF9AC7) else Color(0xFFC2185B)
                val bloom2Base = if (isDark) Color(0xFFD7FF70) else Color(0xFF6FAF12)
                val stemBase = if (isDark) Color(0xFF23B26D) else Color(0xFF146C2E)
                val leaf = leafBase.copy(alpha = alpha * 0.86f)
                val bloom = bloomBase.copy(alpha = alpha * 0.72f)
                val bloom2 = bloom2Base.copy(alpha = alpha * 0.64f)
                val stem = stemBase.copy(alpha = alpha * 0.78f)
                listOf(
                    Offset(size.width * 0.16f, size.height * 0.22f),
                    Offset(size.width * 0.78f, size.height * 0.34f),
                    Offset(size.width * 0.28f, size.height * 0.70f),
                    Offset(size.width * 0.72f, size.height * 0.78f),
                    Offset(size.width * 0.46f, size.height * 0.46f),
                    Offset(size.width * 0.92f, size.height * 0.56f)
                ).forEachIndexed { index, center ->
                    val animatedCenter = center + Offset(wave * (6f + index), -wave * (3f + index * 0.6f))
                    drawLine(
                        color = stem,
                        start = Offset(animatedCenter.x - 14f, animatedCenter.y + 30f),
                        end = Offset(animatedCenter.x + 10f, animatedCenter.y - 20f),
                        strokeWidth = 2.8f,
                        cap = StrokeCap.Round
                    )
                    drawOval(
                        color = leaf,
                        topLeft = Offset(animatedCenter.x - 52f, animatedCenter.y - 20f),
                        size = Size(60f, 32f)
                    )
                    drawOval(
                        color = leaf.copy(alpha = alpha * 0.62f),
                        topLeft = Offset(animatedCenter.x + 1f, animatedCenter.y - 38f),
                        size = Size(58f, 31f)
                    )
                    if (index % 2 == 0) {
                        drawCircle(bloom, radius = 22f, center = Offset(animatedCenter.x + 4f, animatedCenter.y - 44f))
                    } else {
                        drawCircle(bloom2, radius = 17f, center = Offset(animatedCenter.x + 8f, animatedCenter.y - 34f))
                    }
                }
            }
            "autumn" -> {
                val colors = if (isDark) {
                    listOf(
                        Color(0xFFFFC44D).copy(alpha = alpha * 1.16f),
                        Color(0xFFFF7A3D).copy(alpha = alpha * 1.02f),
                        Color(0xFFD97900).copy(alpha = alpha * 0.96f),
                        Color(0xFFB64A18).copy(alpha = alpha * 0.88f),
                        Color(0xFFE53935).copy(alpha = alpha * 0.72f)
                    )
                } else {
                    listOf(
                        Color(0xFFC74A00).copy(alpha = alpha * 1.14f),
                        Color(0xFFB71C1C).copy(alpha = alpha * 0.96f),
                        Color(0xFFE07A00).copy(alpha = alpha * 1.08f),
                        Color(0xFF7A3A00).copy(alpha = alpha * 0.82f),
                        Color(0xFFD84315).copy(alpha = alpha * 1.00f)
                    )
                }
                fun drawMapleLeaf(center: Offset, leafSize: Size, color: Color, lean: Float) {
                    val w = leafSize.width
                    val h = leafSize.height
                    val stem = Offset(center.x - w * 0.12f * lean, center.y + h * 0.62f)
                    val path = Path().apply {
                        moveTo(stem.x, stem.y)
                        lineTo(center.x - w * 0.18f * lean, center.y + h * 0.22f)
                        lineTo(center.x - w * 0.52f * lean, center.y + h * 0.30f)
                        lineTo(center.x - w * 0.34f * lean, center.y + h * 0.04f)
                        lineTo(center.x - w * 0.68f * lean, center.y - h * 0.04f)
                        lineTo(center.x - w * 0.30f * lean, center.y - h * 0.16f)
                        lineTo(center.x - w * 0.42f * lean, center.y - h * 0.48f)
                        lineTo(center.x - w * 0.08f * lean, center.y - h * 0.30f)
                        lineTo(center.x, center.y - h * 0.70f)
                        lineTo(center.x + w * 0.08f * lean, center.y - h * 0.30f)
                        lineTo(center.x + w * 0.42f * lean, center.y - h * 0.48f)
                        lineTo(center.x + w * 0.30f * lean, center.y - h * 0.16f)
                        lineTo(center.x + w * 0.68f * lean, center.y - h * 0.04f)
                        lineTo(center.x + w * 0.34f * lean, center.y + h * 0.04f)
                        lineTo(center.x + w * 0.52f * lean, center.y + h * 0.30f)
                        lineTo(center.x + w * 0.18f * lean, center.y + h * 0.22f)
                        close()
                    }
                    drawPath(path, color)
                    val veinColor = color.copy(alpha = alpha * 0.78f)
                    drawLine(
                        color = veinColor,
                        start = stem,
                        end = Offset(center.x, center.y - h * 0.62f),
                        strokeWidth = 2.8f,
                        cap = StrokeCap.Round
                    )
                    listOf(-0.42f, -0.24f, 0.24f, 0.42f).forEach { side ->
                        drawLine(
                            color = veinColor.copy(alpha = alpha * 0.48f),
                            start = Offset(center.x, center.y - h * 0.08f),
                            end = Offset(center.x + w * side * lean, center.y - h * 0.26f),
                            strokeWidth = 1.6f,
                            cap = StrokeCap.Round
                        )
                    }
                }
                fun drawAspenLeaf(center: Offset, leafSize: Size, color: Color, lean: Float) {
                    val w = leafSize.width
                    val h = leafSize.height
                    val path = Path().apply {
                        moveTo(center.x, center.y - h * 0.58f)
                        cubicTo(center.x - w * 0.58f, center.y - h * 0.32f, center.x - w * 0.62f, center.y + h * 0.26f, center.x, center.y + h * 0.48f)
                        cubicTo(center.x + w * 0.62f, center.y + h * 0.26f, center.x + w * 0.58f, center.y - h * 0.32f, center.x, center.y - h * 0.58f)
                        close()
                    }
                    drawPath(path, color.copy(alpha = color.alpha * 0.88f))
                    drawLine(
                        color = color.copy(alpha = alpha * 0.62f),
                        start = Offset(center.x - w * 0.08f * lean, center.y + h * 0.58f),
                        end = Offset(center.x, center.y - h * 0.48f),
                        strokeWidth = 2f,
                        cap = StrokeCap.Round
                    )
                }
                listOf(
                    Triple(Offset(size.width * 0.10f, size.height * 0.16f), Size(96f, 74f), 1.0f),
                    Triple(Offset(size.width * 0.84f, size.height * 0.22f), Size(112f, 84f), -1.0f),
                    Triple(Offset(size.width * 0.66f, size.height * 0.52f), Size(92f, 70f), 0.86f),
                    Triple(Offset(size.width * 0.18f, size.height * 0.68f), Size(106f, 78f), -0.82f),
                    Triple(Offset(size.width * 0.44f, size.height * 0.34f), Size(98f, 74f), 1.0f),
                    Triple(Offset(size.width * 0.92f, size.height * 0.72f), Size(88f, 66f), -0.88f),
                    Triple(Offset(size.width * 0.34f, size.height * 0.86f), Size(80f, 60f), 0.78f),
                    Triple(Offset(size.width * 0.74f, size.height * 0.84f), Size(102f, 76f), 1.0f),
                    Triple(Offset(size.width * 0.54f, size.height * 0.18f), Size(78f, 58f), -0.74f),
                    Triple(Offset(size.width * 0.28f, size.height * 0.48f), Size(72f, 54f), 0.82f),
                    Triple(Offset(size.width * 0.58f, size.height * 0.70f), Size(84f, 62f), -0.92f)
                ).forEachIndexed { index, (center, leafSize, lean) ->
                    val color = colors[index % colors.size]
                    val animatedCenter = center + Offset(wave * (10f + index * 0.8f), drift * (18f + index) % 28f)
                    if (index % 3 == 0) {
                        drawAspenLeaf(animatedCenter, leafSize, color, lean)
                    } else {
                        drawMapleLeaf(animatedCenter, leafSize, color, lean)
                    }
                }
            }
            "winter" -> {
                val snow = (if (isDark) Color(0xFFB9E8FF) else Color(0xFF1D5FAE)).copy(alpha = alpha * 1.02f)
                val frost = (if (isDark) Color(0xFFFFFFFF) else Color(0xFF2F7FE8)).copy(alpha = alpha * 0.54f)
                val tree = (if (isDark) Color(0xFF7EF0D6) else Color(0xFF0F766E)).copy(alpha = alpha * 0.76f)
                val mountain = (if (isDark) Color(0xFFCFEFFF) else Color(0xFF2A6CB5)).copy(alpha = alpha * 0.54f)
                val mountainShadow = (if (isDark) Color(0xFF7DAED6) else Color(0xFF1B4F86)).copy(alpha = alpha * 0.34f)
                fun drawMountainRibbon(baseY: Float, height: Float, color: Color, driftOffset: Float) {
                    val path = Path().apply {
                        moveTo(-36f, baseY)
                        lineTo(size.width * 0.12f + driftOffset, baseY - height * 0.72f)
                        lineTo(size.width * 0.25f + driftOffset, baseY - height * 0.30f)
                        lineTo(size.width * 0.42f + driftOffset, baseY - height)
                        lineTo(size.width * 0.58f + driftOffset, baseY - height * 0.36f)
                        lineTo(size.width * 0.78f + driftOffset, baseY - height * 0.82f)
                        lineTo(size.width + 42f, baseY - height * 0.18f)
                        lineTo(size.width + 42f, baseY + 36f)
                        lineTo(-36f, baseY + 36f)
                        close()
                    }
                    drawPath(path, color)
                }
                drawMountainRibbon(size.height * 0.42f, 118f, mountainShadow, wave * 6f)
                drawMountainRibbon(size.height * 0.36f, 96f, mountain, -wave * 5f)
                listOf(
                    Offset(size.width * 0.10f, size.height * 0.18f),
                    Offset(size.width * 0.88f, size.height * 0.20f),
                    Offset(size.width * 0.72f, size.height * 0.48f),
                    Offset(size.width * 0.20f, size.height * 0.62f),
                    Offset(size.width * 0.54f, size.height * 0.78f),
                    Offset(size.width * 0.36f, size.height * 0.32f),
                    Offset(size.width * 0.92f, size.height * 0.84f),
                    Offset(size.width * 0.08f, size.height * 0.46f)
                ).forEachIndexed { index, point ->
                    val center = point + Offset(wave * (5f + index), drift * (10f + index) % 18f)
                    val radius = if (index % 2 == 0) 14f else 10f
                    drawCircle(color = snow.copy(alpha = snow.alpha * 0.25f), radius = radius * 0.70f, center = center)
                    repeat(3) { arm ->
                        val angle = (arm * 60f) * Math.PI.toFloat() / 180f
                        val dx = kotlin.math.cos(angle) * radius * 2.2f
                        val dy = kotlin.math.sin(angle) * radius * 2.2f
                        drawLine(snow, Offset(center.x - dx, center.y - dy), Offset(center.x + dx, center.y + dy), 1.8f, cap = StrokeCap.Round)
                    }
                    drawCircle(color = frost, radius = radius * 0.22f, center = center)
                }
                listOf(
                    Offset(size.width * 0.12f, size.height * 0.82f),
                    Offset(size.width * 0.86f, size.height * 0.66f),
                    Offset(size.width * 0.42f, size.height * 0.92f),
                    Offset(size.width * 0.66f, size.height * 0.28f)
                ).forEachIndexed { index, base ->
                    val treeScale = if (index % 2 == 0) 1.18f else 0.92f
                    val path = Path().apply {
                        moveTo(base.x, base.y - 66f * treeScale)
                        lineTo(base.x - 42f * treeScale, base.y)
                        lineTo(base.x + 42f * treeScale, base.y)
                        close()
                        moveTo(base.x, base.y - 98f * treeScale)
                        lineTo(base.x - 34f * treeScale, base.y - 36f * treeScale)
                        lineTo(base.x + 34f * treeScale, base.y - 36f * treeScale)
                        close()
                        moveTo(base.x, base.y - 126f * treeScale)
                        lineTo(base.x - 24f * treeScale, base.y - 72f * treeScale)
                        lineTo(base.x + 24f * treeScale, base.y - 72f * treeScale)
                        close()
                    }
                    drawPath(path, tree)
                }
            }
            else -> {
                val sun = (if (isDark) Color(0xFFFFD247) else Color(0xFFE69500)).copy(alpha = alpha * 1.04f)
                val flower = (if (isDark) Color(0xFFFF7AC2) else Color(0xFFD81B60)).copy(alpha = alpha * 0.86f)
                val flower2 = (if (isDark) Color(0xFFFF9F43) else Color(0xFFE65100)).copy(alpha = alpha * 0.72f)
                val water = (if (isDark) Color(0xFF61E7FF) else Color(0xFF0077B6)).copy(alpha = alpha * 0.76f)
                val meadow = (if (isDark) Color(0xFFA7F36B) else Color(0xFF2E8B57)).copy(alpha = alpha * 0.56f)
                fun drawFlowerMedallion(center: Offset, petalRadius: Float, color: Color, core: Color) {
                    repeat(10) { i ->
                        val angle = i * Math.PI.toFloat() / 5f
                        val petal = Offset(
                            center.x + kotlin.math.cos(angle) * petalRadius,
                            center.y + kotlin.math.sin(angle) * petalRadius
                        )
                        drawCircle(color, radius = petalRadius * 0.34f, center = petal)
                    }
                    drawCircle(core, radius = petalRadius * 0.32f, center = center)
                }
                val sunCenter = Offset(size.width * 0.84f + wave * 8f, size.height * 0.18f)
                drawCircle(sun, radius = 68f, center = sunCenter)
                repeat(10) { ray ->
                    val angle = ray * Math.PI.toFloat() / 5f
                    val start = Offset(
                        sunCenter.x + kotlin.math.cos(angle) * 78f,
                        sunCenter.y + kotlin.math.sin(angle) * 78f
                    )
                    val end = Offset(
                        sunCenter.x + kotlin.math.cos(angle) * 104f,
                        sunCenter.y + kotlin.math.sin(angle) * 104f
                    )
                    drawLine(sun.copy(alpha = alpha * 0.44f), start, end, 3f, cap = StrokeCap.Round)
                }
                listOf(
                    Offset(size.width * 0.18f, size.height * 0.30f),
                    Offset(size.width * 0.72f, size.height * 0.64f),
                    Offset(size.width * 0.38f, size.height * 0.78f),
                    Offset(size.width * 0.90f, size.height * 0.44f)
                ).forEachIndexed { index, center ->
                    val animatedCenter = center + Offset(wave * (5f + index), -wave * 4f)
                    drawFlowerMedallion(
                        center = animatedCenter,
                        petalRadius = if (index % 2 == 0) 34f else 27f,
                        color = if (index % 2 == 0) flower else flower2,
                        core = if (index % 2 == 0) water else sun.copy(alpha = sun.alpha * 0.76f)
                    )
                }
                listOf(
                    Offset(size.width * 0.08f, size.height * 0.70f),
                    Offset(size.width * 0.32f, size.height * 0.64f),
                    Offset(size.width * 0.62f, size.height * 0.74f),
                    Offset(size.width * 0.88f, size.height * 0.66f)
                ).forEachIndexed { index, base ->
                    drawOval(
                        color = meadow.copy(alpha = meadow.alpha * (0.70f + index * 0.05f)),
                        topLeft = Offset(base.x + wave * (4f + index), base.y),
                        size = Size(92f + index * 18f, 28f + index * 4f)
                    )
                }
                drawLine(
                    color = water,
                    start = Offset(size.width * 0.08f, size.height * 0.88f),
                    end = Offset(size.width * 0.92f, size.height * 0.84f),
                    strokeWidth = 8f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = water.copy(alpha = alpha * 0.42f),
                    start = Offset(size.width * 0.14f, size.height * 0.94f),
                    end = Offset(size.width * 0.82f, size.height * 0.91f),
                    strokeWidth = 5f,
                    cap = StrokeCap.Round
                )
            }
        }
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
    refreshSignal: Int
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
        "alerts" -> Color(0xFFFF8A3D)
        "transportation" -> LocalAppThemePalette.current.action
        else -> FlightArrivalLantern
    }
    val alertsMode = mode == "alerts"
    val cleanFlightMode = true
    val modePanel = if (alertsMode && isDark && !highContrast) {
        FlightAlertReadableDarkPanel
    } else {
        flightLanternSheetPanelColor(modeAccent, isDark, glassAmount)
    }
    val modeOverlay = if (alertsMode && isDark && !highContrast) {
        FlightAlertReadableDarkOverlay
    } else {
        flightLanternSheetOverlayColor(modeAccent, isDark, glassAmount)
    }
    val modeSheen = flightLanternSheetSheenBrush(modeAccent, isDark, glassAmount)
    val alertSurface = if (cleanFlightMode) {
        if (isDark) Color(0xFF202734) else Color(0xFFFCFCFD)
    } else if (highContrast) {
        if (isDark) Color.Black.copy(alpha = 0.72f) else Color.White
    } else if (isDark) {
        Color.White.copy(alpha = 0.10f + 0.08f * glassAmount)
    } else {
        Color.White.copy(alpha = 0.50f + 0.20f * glassAmount)
    }
    val alertTextColor = if (cleanFlightMode) {
        if (isDark) Color.White.copy(alpha = 0.96f) else Color(0xFF13294D)
    } else {
        palette.text
    }
    val alertMutedColor = if (cleanFlightMode) {
        if (isDark) Color(0xFFAAB3C5) else Color(0xFF667498)
    } else {
        palette.muted
    }
    val effectiveFlightSnapshot = remember(flightSnapshot, snapshot) {
        flightSnapshot.withCurrentTableCounts(snapshot)
    }
    val alertsUpdatedLabel = remember(liveStatusSnapshot.updatedLabel, snapshot.lastUpdated) {
        flightTopBarStatusLabel("alerts", snapshot, liveStatusSnapshot)
    }
    val tableUpdatedLabel = remember(snapshot.lastUpdated) {
        flightTopBarStatusLabel("arrival", snapshot, liveStatusSnapshot)
    }

    Box(
        modifier = modifier
            .background(if (cleanFlightMode) {
                if (isDark) Color(0xFF0E1118) else Color(0xFFFCFCFD)
            } else {
                palette.page
            })
    ) {
        if (!cleanFlightMode) {
            ProfileBackdropImageLayer(
                modifier = Modifier.matchParentSize(),
                lightRes = R.drawable.light_grid_pattern,
                darkRes = R.drawable.dark_grid_pattern,
                imageAlpha = if (isDark) 1f else 0.72f,
                scrimDark = 0f,
                scrimLight = 0f
            )
        }
        var contentPanelModifier: Modifier = Modifier
            .fillMaxSize()
            .padding(
                start = if (cleanFlightMode) 0.dp else 2.dp,
                end = if (cleanFlightMode) 0.dp else 2.dp,
                top = if (cleanFlightMode) 0.dp else 106.dp,
                bottom = if (cleanFlightMode) 0.dp else 92.dp
            )
        if (!cleanFlightMode) {
            contentPanelModifier = contentPanelModifier
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
        }
        Box(modifier = contentPanelModifier) {
            if (!cleanFlightMode && !highContrast) {
                FlightSeasonalFlightBackdrop(
                    webTheme = webTheme,
                    isDark = isDark,
                    modifier = Modifier.matchParentSize()
                )
            }
            AnimatedContent(
                targetState = mode,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    fun tabOrder(value: String): Int = when (value) {
                        "departure" -> 1
                        "alerts" -> 2
                        "transportation" -> 3
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(contentScrollState)
                        .padding(
                            start = 8.dp,
                            end = 8.dp,
                            top = 106.dp,
                            bottom = 132.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (targetMode) {
                        "alerts" -> FlightLiveStatusContent(
                            snapshot = liveStatusSnapshot,
                            tableSnapshot = snapshot,
                            flightSnapshot = effectiveFlightSnapshot,
                            weather = weather,
                            textColor = alertTextColor,
                            mutedColor = alertMutedColor,
                            surface = alertSurface,
                            textScale = textScale.coerceIn(0.82f, 1.12f),
                            highContrast = highContrast,
                            showHandle = false,
                            updatedLabel = alertsUpdatedLabel,
                            refreshSignal = refreshSignal
                        )
                        "transportation" -> FlightTransportationContent(
                            textColor = alertTextColor,
                            mutedColor = alertMutedColor,
                            textScale = textScale.coerceIn(0.82f, 1.12f),
                            highContrast = highContrast,
                            showHandle = false
                        )
                        else -> FlightTableContent(
                            snapshot = snapshot,
                            mode = targetMode,
                            textColor = alertTextColor,
                            mutedColor = alertMutedColor,
                            surface = alertSurface,
                            tablePalette = null,
                            textScale = textScale,
                            groupedFlights = groupedFlights,
                            highContrast = highContrast,
                            cleanStyle = true,
                            updatedLabel = tableUpdatedLabel
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
    refreshSignal: Int,
    onDismiss: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val glassAmount = rememberLiquidGlassTintAmount()
    val modeAccent = when (mode) {
        "departure" -> FlightDepartureLantern
        "alerts" -> Color(0xFFFF8A3D)
        "transportation" -> LocalAppThemePalette.current.action
        else -> FlightArrivalLantern
    }
    val alertsMode = mode == "alerts"
    val panelColor = if (alertsMode && isDark && !highContrast) {
        FlightAlertReadableDarkPanel
    } else {
        flightLanternSheetPanelColor(modeAccent, isDark, glassAmount)
    }
    val overlayTint = if (alertsMode && isDark && !highContrast) {
        FlightAlertReadableDarkOverlay
    } else {
        flightLanternSheetOverlayColor(modeAccent, isDark, glassAmount)
    }
    val sheenBrush = flightLanternSheetSheenBrush(modeAccent, isDark, glassAmount)
    val sheetBlurDp = if (isDark) 18f + 14f * glassAmount else 17f + 13f * glassAmount
    val textColor = if (highContrast) {
        if (isDark) Color.White else Color.Black
    } else if (alertsMode && isDark) {
        Color.White.copy(alpha = 0.97f)
    } else if (isDark) Color.White.copy(alpha = 0.94f) else Color(0xFF1E1F24)
    val mutedColor = textColor.copy(alpha = if (alertsMode && isDark && !highContrast) 0.76f else 0.62f)
    val innerSurface = if (highContrast) {
        if (isDark) Color.Black.copy(alpha = 0.72f) else Color.White
    } else if (alertsMode && isDark) {
        FlightAlertReadableDarkSurface
    } else if (alertsMode) {
        modeAccent.copy(alpha = 0.06f).compositeOver(Color(0xFFF8F8FF))
    } else if (isDark) Color.White.copy(alpha = 0.10f + 0.08f * glassAmount)
    else Color.White.copy(alpha = 0.50f + 0.20f * glassAmount)
    val sheetShape = RoundedCornerShape(26.dp)
    val tableTextScale = flightTableTextScale(textZoom)
    val statusTextScale = (textZoom.coerceIn(60, 100) / SettingsStore.DEFAULT_TEXT_ZOOM.toFloat())
        .coerceIn(0.82f, 1.12f)
    val effectiveFlightSnapshot = remember(flightSnapshot, tableSnapshot) {
        flightSnapshot.withCurrentTableCounts(tableSnapshot)
    }
    val alertsUpdatedLabel = remember(liveStatusSnapshot.updatedLabel, tableSnapshot.lastUpdated) {
        flightTopBarStatusLabel("alerts", tableSnapshot, liveStatusSnapshot)
    }
    val tableUpdatedLabel = remember(tableSnapshot.lastUpdated) {
        flightTopBarStatusLabel("arrival", tableSnapshot, liveStatusSnapshot)
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
                when (mode) {
                    "alerts" -> FlightLiveStatusContent(
                        snapshot = liveStatusSnapshot,
                        tableSnapshot = tableSnapshot,
                        flightSnapshot = effectiveFlightSnapshot,
                        weather = weather,
                        textColor = textColor,
                        mutedColor = mutedColor,
                        surface = innerSurface,
                        textScale = statusTextScale,
                        highContrast = highContrast,
                        showHandle = true,
                        updatedLabel = alertsUpdatedLabel,
                        refreshSignal = refreshSignal
                    )
                    "transportation" -> FlightTransportationContent(
                        textColor = textColor,
                        mutedColor = mutedColor,
                        textScale = statusTextScale,
                        highContrast = highContrast,
                        showHandle = true
                    )
                    else -> {
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
                            highContrast = highContrast,
                            cleanStyle = false,
                            updatedLabel = tableUpdatedLabel
                        )
                    }
                }
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
    highContrast: Boolean,
    cleanStyle: Boolean = false,
    updatedLabel: String = ""
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
    val orderedDayEntries = remember(rowsByDay) {
        rowsByDay.entries.sortedWith(
            compareBy<Map.Entry<String, List<FlightTableRow>>> { entry ->
                when {
                    isTodayFlightDayLabel(entry.key) -> 0
                    entry.key.equals("Today", ignoreCase = true) -> 0
                    else -> 1
                }
            }.thenBy { entry ->
                entry.value.firstOrNull()?.let(::flightTableRowSortMinutes) ?: Int.MAX_VALUE
            }
        )
    }
    if (rows.isEmpty()) {
        FlightTableLoadingSkeleton(
            placeLabel = if (isDeparture) "To" else "From",
            textColor = textColor,
            surface = surface,
            textScale = textScale,
            groupedFlights = groupedFlights,
            highContrast = highContrast,
            cleanStyle = cleanStyle
        )
    } else {
        orderedDayEntries.forEachIndexed { index, entry ->
            if (index > 0) {
                Spacer(Modifier.height(4.dp))
            }
            var visibleRowIndex = 0
            val cleanHiddenAlpha = if (cleanStyle) 1f else 0f
            fun nextFadeIndex(): Int = if (cleanStyle) 0 else visibleRowIndex++
            FlightDataFadeIn(
                index = nextFadeIndex(),
                key = "${mode}-${entry.key}-${snapshot.lastUpdated}-updated",
                hiddenAlpha = cleanHiddenAlpha
            ) {
                if (index == 0) {
                    FlightUpdatedStatusPill(
                        label = updatedLabel,
                        textColor = textColor,
                        mutedColor = mutedColor,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            FlightDataFadeIn(
                index = nextFadeIndex(),
                key = "${mode}-${entry.key}-${snapshot.lastUpdated}-day",
                hiddenAlpha = cleanHiddenAlpha
            ) {
                FlightTableDayHeader(
                    day = entry.key,
                    total = entry.value.size,
                    textColor = textColor
                )
            }
            FlightDataFadeIn(
                index = nextFadeIndex(),
                key = "${mode}-${entry.key}-${snapshot.lastUpdated}-columns",
                hiddenAlpha = cleanHiddenAlpha
            ) {
                FlightTableColumnHeader(
                    placeLabel = if (isDeparture) "To" else "From",
                    textColor = mutedColor,
                    textScale = textScale
                )
            }
            entry.value.forEach { row ->
                if (!groupedFlights) {
                    FlightDataFadeIn(
                        index = nextFadeIndex(),
                        key = "${mode}-${entry.key}-${row.airline}-${row.flight}-${row.sched}-${row.actual}-${row.status}",
                        hiddenAlpha = cleanHiddenAlpha
                    ) {
                        FlightTableRowCard(
                            row = row,
                            placeLabel = if (isDeparture) "To" else "From",
                            textColor = textColor,
                            mutedColor = mutedColor,
                            surface = surface,
                            tablePalette = tablePalette,
                            textScale = textScale,
                            highContrast = highContrast,
                            cleanStyle = cleanStyle
                        )
                    }
                }
            }
            if (groupedFlights) {
                flightTableSortedAirlineGroups(entry.value).forEach { group ->
                    FlightDataFadeIn(
                        index = nextFadeIndex(),
                        key = "${mode}-${entry.key}-${group.airline}-${group.rows.size}",
                        hiddenAlpha = cleanHiddenAlpha
                    ) {
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
                    }
                    group.rows.forEach { row ->
                        FlightDataFadeIn(
                            index = nextFadeIndex(),
                            key = "${mode}-${entry.key}-${row.airline}-${row.flight}-${row.sched}-${row.actual}-${row.status}",
                            hiddenAlpha = cleanHiddenAlpha
                        ) {
                            FlightTableRowCard(
                                row = row,
                                placeLabel = if (isDeparture) "To" else "From",
                                textColor = textColor,
                                mutedColor = mutedColor,
                                surface = surface,
                                tablePalette = tablePalette,
                                textScale = textScale,
                                highContrast = highContrast,
                                cleanStyle = cleanStyle
                            )
                        }
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

private const val FlightScheduleSkeletonRowCount = 10
private const val FlightAlertSkeletonCardCount = 10

@Composable
private fun FlightDataFadeIn(
    index: Int,
    key: Any? = index,
    hiddenAlpha: Float = 0f,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(key) {
        visible = false
        delay(
            (24L + (index.coerceAtMost(10) * 42L)
                .coerceAtMost(320L)).milliseconds
        )
        visible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else hiddenAlpha,
        animationSpec = tween(
            durationMillis = 560,
            easing = FastOutSlowInEasing
        ),
        label = "flightDataFadeAlpha"
    )

    val offset by animateFloatAsState(
        targetValue = if (visible) 0f else 22f,
        animationSpec = tween(
            durationMillis = 620,
            easing = FastOutSlowInEasing
        ),
        label = "flightDataFadeOffset"
    )

    Box(
        modifier = Modifier.graphicsLayer {
            this.alpha = alpha
            translationY = offset
        }
    ) {
        content()
    }
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
    highContrast: Boolean,
    cleanStyle: Boolean = false
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
        repeat(FlightScheduleSkeletonRowCount) { index ->
            FlightTableRowSkeleton(
                index = index,
                textColor = textColor,
                surface = surface,
                pulse = pulse,
                highContrast = highContrast,
                cleanStyle = cleanStyle
            )
        }
    } else {
        repeat(FlightScheduleSkeletonRowCount) { index ->
            FlightTableRowSkeleton(
                index = index,
                textColor = textColor,
                surface = surface,
                pulse = pulse,
                highContrast = highContrast,
                cleanStyle = cleanStyle
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
    highContrast: Boolean,
    cleanStyle: Boolean = false
) {
    val rowShape = RoundedCornerShape(18.dp)
    val isDark = isSystemInDarkTheme()
    val rowSurface = if (cleanStyle) {
        if (isDark) Color(0xFF202734) else Color(0xFFFCFCFD)
    } else if (highContrast) {
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .shadow(
                elevation = if (cleanStyle) {
                    if (isDark) 1.dp else 3.dp
                } else {
                    0.dp
                },
                shape = rowShape,
                clip = false
            )
            .clip(rowShape)
            .background(rowSurface)
            .then(
                if (cleanStyle) {
                    Modifier
                } else {
                    Modifier.border(
                        1.dp,
                        if (highContrast) textColor.copy(alpha = 0.30f) else flightItemBorderColor(isDark),
                        rowShape
                    )
                }
            )
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FlightSkeletonBone(
                modifier = Modifier
                    .size(if (cleanStyle) 42.dp else 38.dp)
                    .shadow(
                        elevation = if (cleanStyle) {
                            if (isDark) 1.dp else 3.dp
                        } else {
                            0.dp
                        },
                        shape = RoundedCornerShape(12.dp),
                        clip = false
                    ),
                color = textColor,
                alpha = pulse * 0.82f,
                shape = if (cleanStyle) RoundedCornerShape(12.dp) else RoundedCornerShape(999.dp)
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
            compareBy(
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
                    compareBy(
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
    textColor: Color
) {
    val headerShape = RoundedCornerShape(999.dp)
    val isDark = isSystemInDarkTheme()
    val summary = "$day • $total flights"
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
    highContrast: Boolean,
    cleanStyle: Boolean
) {
    val isDark = isSystemInDarkTheme()
    val isArrived = row.status.contains("arriv", ignoreCase = true)
    val compactArrived = isArrived && !cleanStyle

    val arrivedAccent =
        if (cleanStyle) {
            mutedColor
        } else if (isDark) {
            Color(0xFF7DD3FC)
        } else {
            Color(0xFF2563EB)
        }

    val rowShape = RoundedCornerShape(
        if (compactArrived) 12.dp else 18.dp
    )

    val rowBorder = if (cleanStyle) {
        Color.Transparent
    } else if (isArrived) {
        arrivedAccent.copy(alpha = if (isDark) 0.55f else 0.38f)
    } else {
        flightRowBorderColor(row, isDark, tablePalette)
    }

    val rowBackground = if (cleanStyle) {
        if (isDark) Color(0xFF202734) else Color(0xFFFCFCFD)
    } else if (isArrived) {
        if (isDark) {
            Color(0xFF102A43)
        } else {
            Color(0xFFDCEBFF)
        }
    } else if (highContrast) {
        surface
    } else {
        tableRowSurface(row, surface, isDark, tablePalette)
    }

    val cancelledStyledBorder =
        !cleanStyle && !highContrast && row.isCancelledFlight()

    val borderModifier = if (cleanStyle) {
        Modifier
    } else if (cancelledStyledBorder) {
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
        Modifier.border(
            width = 1.dp,
            color = if (highContrast && !isArrived) {
                textColor.copy(alpha = 0.36f)
            } else {
                rowBorder
            },
            shape = rowShape
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (compactArrived) 46.dp else 58.dp)
            .shadow(elevation = if (cleanStyle) {
                if (isDark) 1.dp else 3.dp
            } else {
                0.dp
            }, shape = rowShape, clip = false)
            .clip(rowShape)
            .background(rowBackground)
            .then(borderModifier)
            .padding(
                horizontal = if (compactArrived) 6.dp else 8.dp,
                vertical = if (compactArrived) 4.dp else 8.dp
            )
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(
                if (compactArrived) 6.dp else 10.dp
            )
        ) {
            if (compactArrived) {
                Box(
                    modifier = Modifier.size(30.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier.graphicsLayer {
                            scaleX = 0.78f
                            scaleY = 0.78f
                        }
                    ) {
                        AirlineBadge(
                            airline = row.airline.ifBlank { "--" }
                        )
                    }
                }
            } else {
                AirlineBadge(
                    airline = row.airline.ifBlank { "--" }
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(
                        if (compactArrived) 5.dp else 8.dp
                    )
                ) {
                    Text(
                        text = listOf(row.airline, row.flight)
                            .filter { it.isNotBlank() }
                            .joinToString(" ")
                            .ifBlank { "--" },
                        color = textColor,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (compactArrived) {
                                FontWeight.SemiBold
                            } else {
                                FontWeight.Medium
                            },
                            fontSize = if (compactArrived) {
                                flightTableScaledSp(9.3f, 10.3f, textScale)
                            } else {
                                flightTableScaledSp(10.8f, 12f, textScale)
                            },
                            lineHeight = if (compactArrived) {
                                flightTableScaledSp(10.2f, 11.2f, textScale)
                            } else {
                                flightTableScaledSp(12f, 13.4f, textScale)
                            }
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (isArrived) {
                        Text(
                            text = "Arrived",
                            color = arrivedAccent,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.5.sp,
                                lineHeight = 9.2.sp
                            ),
                            maxLines = 1,
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(
                                    if (cleanStyle) {
                                        if (isDark) Color(0xFF202734) else Color(0xFFF0F2F7)
                                    } else {
                                        arrivedAccent.copy(alpha = if (isDark) 0.24f else 0.14f)
                                    }
                                )
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    } else {
                        FlightTableStatusPill(
                            row = row,
                            palette = tablePalette,
                            cleanStyle = cleanStyle
                        )
                    }
                }

                Spacer(
                    Modifier.height(
                        if (compactArrived) 1.dp else 3.dp
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(
                        if (compactArrived) 5.dp else 8.dp
                    )
                ) {
                    Text(
                        text = row.place.ifBlank { placeLabel },
                        color = textColor.copy(alpha = 0.88f),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = if (compactArrived) {
                                flightTableScaledSp(7.8f, 8.7f, textScale)
                            } else {
                                flightTableScaledSp(9.2f, 10.4f, textScale)
                            },
                            lineHeight = if (compactArrived) {
                                flightTableScaledSp(8.5f, 9.4f, textScale)
                            } else {
                                flightTableScaledSp(10.4f, 11.7f, textScale)
                            }
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
                            fontSize = if (compactArrived) {
                                flightTableScaledSp(7.1f, 7.9f, textScale)
                            } else {
                                flightTableScaledSp(8.4f, 9.4f, textScale)
                            },
                            lineHeight = if (compactArrived) {
                                flightTableScaledSp(7.9f, 8.7f, textScale)
                            } else {
                                flightTableScaledSp(9.5f, 10.6f, textScale)
                            }
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "Actual ${row.actual.ifBlank { "--" }}",
                        color = mutedColor.copy(alpha = 0.88f),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = if (compactArrived) {
                                flightTableScaledSp(7.1f, 7.9f, textScale)
                            } else {
                                flightTableScaledSp(8.4f, 9.4f, textScale)
                            },
                            lineHeight = if (compactArrived) {
                                flightTableScaledSp(7.9f, 8.7f, textScale)
                            } else {
                                flightTableScaledSp(9.5f, 10.6f, textScale)
                            }
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

@Suppress("SpellCheckingInspection")
@Composable
private fun AirlineBadge(
    airline: String
) {
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current
    val logoUrl = remember(airline) { flightAirlineLogoUrl(airline) }
    val code = airline
        .split(" ", "-", "/", ignoreCase = false)
        .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
        .take(2)
        .joinToString("")
        .ifBlank { "--" }
    val badgeShape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .size(42.dp)
            .shadow(elevation = if (isDark) 2.dp else 4.dp, shape = badgeShape, clip = false)
            .clip(badgeShape)
            .background(if (logoUrl != null) Color(0xFFFCFCFD) else if (isDark) Color(0xFF202734) else Color(0xFFF0F2F7)),
        contentAlignment = Alignment.Center
    ) {
        if (logoUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(logoUrl)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .networkCachePolicy(CachePolicy.ENABLED)
                    .crossfade(true)
                    .build(),
                contentDescription = "$airline airline logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(5.dp)
            )
        } else {
            Text(
                text = code,
                color = if (isDark) Color.White else Color(0xFF13294D),
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
}

@Composable
private fun FlightTableCell(
    text: String,
    color: Color,
    header: Boolean = false,
    textScale: Float = 1f,
    textAlign: TextAlign = TextAlign.Start
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
private fun FlightTableStatusPill(
    row: FlightTableRow,
    palette: NativeFlightTablePalette?,
    cleanStyle: Boolean = false
) {
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
    val cleanMuted = if (isDark) Color(0xFFAAB3C5) else Color(0xFF667498)
    val pillShape = RoundedCornerShape(999.dp)
    val pillBackground = if (cleanStyle) {
        if (isDark) Color(0xFF202734) else Color(0xFFF0F2F7)
    } else if (isCancelled) {
        if (isDark) Color(0xFF12324C).copy(alpha = 0.86f) else Color(0xFFE3F4FF)
    } else {
        accent.copy(alpha = if (isDark) 0.20f else 0.14f)
    }
    val pillBorder = if (cleanStyle) {
        Color.Transparent
    } else if (isCancelled) {
        if (isDark) Color(0xFF5AC8FA).copy(alpha = 0.48f) else Color(0xFF38BDF8).copy(alpha = 0.55f)
    } else {
        accent.copy(alpha = if (isDark) 0.24f else 0.22f)
    }
    val statusTextColor = if (cleanStyle) {
        cleanMuted
    } else if (isCancelled) {
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
            .then(
                if (cleanStyle) {
                    Modifier
                } else {
                    Modifier.border(1.dp, pillBorder, pillShape)
                }
            )
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

private enum class FlightLiquidHeaderType {
    ARRIVALS,
    DEPARTURES,
    ALERTS,
    TRANSPORTATION
}

@Stable
private data class FlightLiquidHeaderControls(
    val title: String,
    val imageUrl: String,
    val imageOffsetY: androidx.compose.ui.unit.Dp,
    val lightBrightness: Float,
    val darkBrightness: Float,
    val lightContrast: Float,
    val darkContrast: Float,
    val lightSaturation: Float,
    val darkSaturation: Float,
    val blurDp: Float,
    val refractionHeightDp: Float,
    val refractionAmountFraction: Float
)

/*
 * ONE CONTROL BLOCK FOR EVERY FLIGHT HEADER IMAGE.
 *
 * Edit the values here to control each image independently:
 * brightness:
 *   0f = original brightness
 *   positive = brighter
 *   negative = darker
 *
 * contrast:
 *   1f = original contrast
 *
 * saturation:
 *   1f = original color
 *   above 1f = stronger color
 */
private fun flightLiquidHeaderControls(
    type: FlightLiquidHeaderType
): FlightLiquidHeaderControls = when (type) {
    FlightLiquidHeaderType.ARRIVALS -> FlightLiquidHeaderControls(
        title = "ARRIVALS",
        imageUrl = FlightArrivalsHeaderImageUrl,
        imageOffsetY = (0).dp,
        lightBrightness = 0.10f,
        darkBrightness = 0.06f,
        lightContrast = 1.06f,
        darkContrast = 1.08f,
        lightSaturation = 1.80f,
        darkSaturation = 2f,
        blurDp = 0f,
        refractionHeightDp = 5f,
        refractionAmountFraction = 0.5f
    )

    FlightLiquidHeaderType.DEPARTURES -> FlightLiquidHeaderControls(
        title = "DEPARTURES",
        imageUrl = FlightDeparturesHeaderImageUrl,
        imageOffsetY = (0).dp,
        lightBrightness = 0.10f,
        darkBrightness = 0.06f,
        lightContrast = 1.07f,
        darkContrast = 1.09f,
        lightSaturation = 1.80f,
        darkSaturation = 2.0f,
        blurDp = 0f,
        refractionHeightDp = 5f,
        refractionAmountFraction = 0.5f
    )

    FlightLiquidHeaderType.ALERTS -> FlightLiquidHeaderControls(
        title = "ALERTS",
        imageUrl = FlightAlertsHeaderImageUrl,
        imageOffsetY = (0).dp,
        lightBrightness = 0.15f,
        darkBrightness = 0.08f,
        lightContrast = 1.09f,
        darkContrast = 1.09f,
        lightSaturation = 1.84f,
        darkSaturation = 2.00f,
        blurDp = 0f,
        refractionHeightDp = 5f,
        refractionAmountFraction = 0.5f
    )

    /*
     * Your file also uses this header for Transportation.
     * It uses the same liquid-glass system so no old header code remains.
     */
    FlightLiquidHeaderType.TRANSPORTATION -> FlightLiquidHeaderControls(
        title = "TRANSPORTATION",
        imageUrl = FlightTransportationHeaderImageUrl,
        imageOffsetY = (0).dp,
        lightBrightness = 0.10f,
        darkBrightness = 0.00f,
        lightContrast = 1.07f,
        darkContrast = 1.09f,
        lightSaturation = 1.82f,
        darkSaturation = 2f,
        blurDp = 0f,
        refractionHeightDp = 5f,
        refractionAmountFraction = 0.5f
    )
}

@Composable
private fun FlightPhotoHeader(
    type: FlightLiquidHeaderType,
    subtitle: String,
    textScale: Float,
    height: androidx.compose.ui.unit.Dp = 136.dp,
    trailingContent: @Composable BoxScope.() -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current
    val imageBackdrop = rememberLayerBackdrop()
    val controls = remember(type) { flightLiquidHeaderControls(type) }

    /*
     * Every image uses the same real Kyant liquid-glass capsule.
     */
    val headerShape: Shape = Capsule()

    val brightness =
        if (isDark) controls.darkBrightness else controls.lightBrightness

    val contrast =
        if (isDark) controls.darkContrast else controls.lightContrast

    val saturation =
        if (isDark) controls.darkSaturation else controls.lightSaturation

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
        contentAlignment = Alignment.Center
    ) {
        /*
         * Fallback displayed while Coil loads the network image.
         */
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(headerShape)
                .background(
                    if (isDark) Color(0xFF0E1118)
                    else Color(0xFFF3F6FA)
                )
        )

        /*
         * The selected image becomes the LayerBackdrop source.
         */
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(controls.imageUrl)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .networkCachePolicy(CachePolicy.ENABLED)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .matchParentSize()
                .clip(headerShape)
                .offset(y = controls.imageOffsetY)
                .layerBackdrop(imageBackdrop)
        )

        /*
         * The filtered image, title, subtitle and optional icon
         * are all inside this one liquid-glass capsule.
         */
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawBackdrop(
                    backdrop = imageBackdrop,
                    shape = { headerShape },
                    effects = {
                        colorControls(
                            brightness = brightness,
                            contrast = contrast,
                            saturation = saturation
                        )

                        blur(
                            radius = controls.blurDp.dp.toPx(),
                            edgeTreatment = TileMode.Mirror
                        )

                        lens(
                            controls.refractionHeightDp.dp.toPx(),
                            size.minDimension * controls.refractionAmountFraction,
                            depthEffect = true
                        )
                    },
                    highlight = {
                        if (isDark) {
                            Highlight(
                                width = 0.45.dp,
                                blurRadius = 1.6.dp,
                                alpha = 0.50f,
                                style = HighlightStyle.Plain
                            )
                        } else {
                            Highlight(
                                width = 0.30.dp,
                                blurRadius = 1.0.dp,
                                alpha = 0.95f,
                                style = HighlightStyle.Plain
                            )
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = 56.dp,
                        end = 56.dp,
                        top = 3.dp,
                        bottom = 3.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = controls.title,
                    color = Color.Yellow,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 23.sp,
                        lineHeight = 27.sp,
                        letterSpacing = 0.sp,
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.90f),
                            offset = Offset(0f, 3f),
                            blurRadius = 6f
                        )
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                if (subtitle.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))

                    val subtitleStyle = MaterialTheme.typography.labelMedium.copy(
                        fontSize = (12f * textScale).sp,
                        lineHeight = (19f * textScale).sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        // Black outline behind the letters
                        Text(
                            text = subtitle,
                            color = Color.Black,
                            textAlign = TextAlign.Center,
                            style = subtitleStyle.copy(
                                drawStyle = Stroke(width = 7f)
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Yellow text on top
                        Text(
                            text = subtitle,
                            color = Color.Yellow,
                            textAlign = TextAlign.Center,
                            style = subtitleStyle.copy(
                                drawStyle = Fill
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            trailingContent()
        }
    }
}

@Composable
private fun ColumnScope.FlightLiveStatusContent(
    snapshot: FlightLiveStatusSnapshot,
    tableSnapshot: FlightTableSnapshot,
    flightSnapshot: FlightSheetBrief,
    weather: FlightSheetWeather,
    textColor: Color,
    mutedColor: Color,
    surface: Color,
    textScale: Float,
    highContrast: Boolean,
    showHandle: Boolean,
    updatedLabel: String = "",
    refreshSignal: Int
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
    val displayItems = remember(snapshot.items, tableSnapshot) {
        snapshot.items.ifEmpty { fallbackFlightLiveStatusItems(tableSnapshot) }
    }
    val showLoadingSkeleton = snapshot.updatedLabel.isBlank() &&
            displayItems.isEmpty() &&
            tableSnapshot.rows.isEmpty()
    if (showLoadingSkeleton) {
        FlightLiveStatusLoadingSkeleton(
            textColor = textColor,
            surface = surface,
            highContrast = highContrast
        )
        return
    }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val sortedItems = remember(displayItems) {
        displayItems.sortedWith(
            compareBy<FlightLiveStatusItem> { parseFlightEtaMinutes(it.etaText) ?: Int.MAX_VALUE }
                .thenBy { parseFlightTableMinutes(compactFlightDetailTime(it.detail)) }
                .thenBy { it.flight.lowercase(Locale.US) }
        )
    }
    val filteredItems = remember(sortedItems, searchQuery) {
        val query = searchQuery.trim()
        if (query.isBlank()) {
            sortedItems
        } else {
            sortedItems.filter { item ->
                listOf(item.flight, item.route, item.status, item.detail, item.badge, item.pill)
                    .any { value -> value.contains(query, ignoreCase = true) }
            }
        }
    }
    var parkingAvailability by remember { mutableStateOf<FlightParkingAvailability?>(null) }
    val context = LocalContext.current
    val online = rememberValidatedInternetState(context)
    LaunchedEffect(refreshSignal, online) {
        if (!online) {
            parkingAvailability = null
            return@LaunchedEffect
        }
        fetchFlightParkingAvailability()?.let { parkingAvailability = it }
    }

    FlightDataFadeIn(
        index = 0,
        key = "alerts-updated-refresh-$refreshSignal",
        hiddenAlpha = 1f
    ) {
        FlightUpdatedStatusPill(
            label = updatedLabel,
            textColor = textColor,
            mutedColor = mutedColor,
            modifier = Modifier.fillMaxWidth()
        )
    }

    FlightDataFadeIn(
        index = 1,
        key = "alerts-search-refresh-$refreshSignal",
        hiddenAlpha = 1f
    ) {
        FlightAlertSearchField(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            textColor = textColor,
            mutedColor = mutedColor,
            textScale = textScale
        )
    }

    FlightDataFadeIn(
        index = 2,
        key = "alerts-widgets-refresh-$refreshSignal",
        hiddenAlpha = 1f
    ) {
        FlightAlertsInfoWidgets(
            weather = weather,
            parkingAvailability = parkingAvailability,
            textColor = textColor,
            mutedColor = mutedColor,
            surface = surface,
            textScale = textScale,
            highContrast = highContrast
        )
    }

    FlightDataFadeIn(
        index = 3,
        key = "issues-refresh-$refreshSignal",
        hiddenAlpha = 1f
    ) {
        FlightAlertsDashboardSummary(
            brief = flightSnapshot,
            textColor = textColor,
            mutedColor = mutedColor,
            surface = surface,
            textScale = textScale,
            highContrast = highContrast
        )
    }

    FlightDataFadeIn(
        index = 4,
        key = "upcoming-refresh-$refreshSignal-$searchQuery",
        hiddenAlpha = 1f
    ) {
        FlightUpcomingFlightsSection(
            items = filteredItems,
            textColor = textColor,
            mutedColor = mutedColor,
            surface = surface,
            textScale = textScale,
            highContrast = highContrast
        )
    }
}

@Composable
private fun FlightAlertSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    textColor: Color,
    mutedColor: Color,
    textScale: Float
) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(999.dp)
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = textColor,
            fontSize = (13f * textScale).sp,
            lineHeight = (16f * textScale).sp,
            fontWeight = FontWeight.SemiBold
        ),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = if (isDark) 1.dp else 2.dp, shape = shape, clip = false)
                    .clip(shape)
                    .background(if (isDark) Color(0xFF202734) else Color(0xFFF5F7FA))
                    .padding(horizontal = 13.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = mutedColor,
                    modifier = Modifier.size(19.dp)
                )
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isBlank()) {
                        Text(
                            text = "Search flight, route, or status",
                            color = mutedColor,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = (13f * textScale).sp,
                                lineHeight = (16f * textScale).sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    innerTextField()
                }
            }
        }
    )
}

@Composable
private fun FlightAlertsInfoWidgets(
    weather: FlightSheetWeather,
    parkingAvailability: FlightParkingAvailability?,
    textColor: Color,
    mutedColor: Color,
    surface: Color,
    textScale: Float,
    highContrast: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val weatherCondition = remember(weather.condition, weather.summary) {
            flightResolvedWeatherCondition(weather)
        }
        FlightAlertInfoWidget(
            label = "Temperature",
            value = weather.temp.ifBlank { "--" },
            detail = weather.summary.ifBlank { weather.condition.ifBlank { "Weather updating" } },
            weatherCondition = weatherCondition,
            textColor = textColor,
            mutedColor = mutedColor,
            surface = surface,
            textScale = textScale,
            highContrast = highContrast,
            modifier = Modifier.weight(1f)
        )
        FlightAlertInfoWidget(
            label = "Parking",
            value = parkingAvailability?.statusLabel ?: "--% available",
            detail = parkingAvailability?.updatedLabel ?: "Parking updating",
            textColor = textColor,
            mutedColor = mutedColor,
            surface = surface,
            textScale = textScale,
            highContrast = highContrast,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun FlightAlertInfoWidget(
    label: String,
    value: String,
    detail: String,
    weatherCondition: String? = null,
    textColor: Color,
    mutedColor: Color,
    surface: Color,
    textScale: Float,
    highContrast: Boolean,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .shadow(elevation = if (isDark) 2.dp else 4.dp, shape = shape, clip = false)
            .clip(shape)
            .background(if (isDark) Color(0xFF202734) else Color(0xFFFCFCFD))
            .padding(horizontal = 12.dp, vertical = 11.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = label,
                color = mutedColor,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = (10.5f * textScale).sp,
                    lineHeight = (12f * textScale).sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = if (weatherCondition.isNullOrBlank()) 0.dp else 28.dp)
            )
            Text(
                text = value,
                color = textColor,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = (17f * textScale).sp,
                    lineHeight = (19f * textScale).sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                Text(
                    text = detail,
                    color = mutedColor,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = (9.6f * textScale).sp,
                        lineHeight = (11.5f * textScale).sp
                    ),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
        weatherCondition?.takeIf { it.isNotBlank() }?.let { condition ->
            FlightWeatherConditionIcon(
                condition = condition,
                tint = flightWeatherIconColor(condition, isDark),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(22.dp)
            )
        }
    }
}

@Composable
private fun FlightAlertsDashboardSummary(
    brief: FlightSheetBrief,
    textColor: Color,
    mutedColor: Color,
    surface: Color,
    textScale: Float,
    highContrast: Boolean
) {
    val countedAlerts = brief.delayedCount + brief.cancelledCount + brief.divertedCount
    val totalAlerts = if (countedAlerts > brief.issues.size) countedAlerts else brief.issues.size
    val issueWord = if (totalAlerts == 1) "alert" else "alerts"
    val hasIssues = totalAlerts > 0
    val headline = if (hasIssues) "$totalAlerts $issueWord today" else "No alerts today"
    val subhead = if (hasIssues) "Stay updated on changes" else "Schedule details below"
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(20.dp)
    val haptic = LocalHapticFeedback.current
    val pagerScrollState = rememberScrollState()
    val displayCardCount = maxOf(brief.issues.size, totalAlerts, 3)
    val placeholderCount = (displayCardCount - brief.issues.size).coerceAtLeast(0)
    val displayPageCount = ((displayCardCount + 2) / 3).coerceAtLeast(1)
    var selectedPage by rememberSaveable(brief.issues.size, totalAlerts) { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = if (isDark) 3.dp else 5.dp, shape = shape, clip = false)
            .background(if (isDark) Color(0xFF182233) else Color(0xFFFCFCFD), shape)
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = headline,
                    color = textColor,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = (18f * textScale).sp,
                        lineHeight = (21f * textScale).sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subhead,
                    color = mutedColor,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = (13f * textScale).sp,
                        lineHeight = (16f * textScale).sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (displayCardCount > 3) {
                FlightViewAllPill(
                    label = if (selectedPage == 0) "+${(displayCardCount - 3).coerceAtLeast(1)} more" else "Back",
                    textColor = textColor,
                    mutedColor = mutedColor,
                    textScale = textScale,
                    onClick = {
                        val targetOffset = if (selectedPage == 0) pagerScrollState.maxValue else 0
                        selectedPage = if (targetOffset > 0) displayPageCount - 1 else 0
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        coroutineScope.launch {
                            pagerScrollState.animateScrollTo(targetOffset)
                        }
                    }
                )
            }
        }
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val sideInset = 12.dp
            val cardGap = 8.dp
            val cardWidth = ((maxWidth - sideInset * 2 - cardGap * 2) / 3f).coerceAtLeast(82.dp)
            val currentPage by remember(pagerScrollState, displayPageCount) {
                derivedStateOf {
                    if (pagerScrollState.maxValue <= 0 || displayPageCount <= 1) {
                        0
                    } else {
                        ((pagerScrollState.value.toFloat() / pagerScrollState.maxValue.toFloat()) * (displayPageCount - 1))
                            .plus(0.5f)
                            .toInt()
                            .coerceIn(0, displayPageCount - 1)
                    }
                }
            }
            LaunchedEffect(currentPage) {
                if (selectedPage != currentPage) {
                    selectedPage = currentPage
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(pagerScrollState)
                    .padding(horizontal = sideInset),
                horizontalArrangement = Arrangement.spacedBy(cardGap)
            ) {
                brief.issues.forEach { issue ->
                    FlightAlertRailCard(
                        issue = issue,
                        textColor = textColor,
                        mutedColor = mutedColor,
                        textScale = textScale,
                        modifier = Modifier.width(cardWidth)
                    )
                }
                repeat(placeholderCount) { index ->
                    FlightAlertPlaceholderRailCard(
                        index = index,
                        textColor = textColor,
                        mutedColor = mutedColor,
                        textScale = textScale,
                        modifier = Modifier.width(cardWidth)
                    )
                }
                if (brief.issues.isEmpty() && placeholderCount == 0) {
                    repeat(3) { index ->
                        FlightAlertPlaceholderRailCard(
                            index = index,
                            textColor = textColor,
                            mutedColor = mutedColor,
                            textScale = textScale,
                            modifier = Modifier.width(cardWidth)
                        )
                    }
                }
            }
        }
        FlightPaginationDots(
            total = displayPageCount,
            active = selectedPage,
            accent = Color(0xFFFF8A3D)
        )
        if (brief.issues.isEmpty() && brief.summary.isNotBlank()) {
            Text(
                text = brief.summary,
                color = mutedColor,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = (12.5f * textScale).sp,
                    lineHeight = (15f * textScale).sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

@Composable
private fun FlightViewAllPill(
    label: String,
    textColor: Color,
    mutedColor: Color,
    textScale: Float,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    Row(
        modifier = Modifier
            .shadow(elevation = if (isDark) 1.dp else 2.dp, shape = RoundedCornerShape(999.dp), clip = false)
            .clip(RoundedCornerShape(999.dp))
            .background(if (isDark) Color(0xFF253044) else Color(0xFFFCFCFD))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            color = if (isDark) textColor else Color(0xFF7A4B24),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Black,
                fontSize = (11.5f * textScale).sp,
                lineHeight = (13f * textScale).sp
            ),
            maxLines = 1
        )
        Text(
            text = ">",
            color = mutedColor,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Black,
                fontSize = (12f * textScale).sp,
                lineHeight = (13f * textScale).sp
            )
        )
    }
}

@Composable
private fun FlightAlertRailCard(
    issue: FlightSheetIssue,
    textColor: Color,
    mutedColor: Color,
    textScale: Float,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val accent = if (isDark) Color(0xFFFFB47A) else Color(0xFF9A5A23)
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .height(108.dp)
            .shadow(elevation = if (isDark) 2.dp else 4.dp, shape = shape, clip = false)
            .clip(shape)
            .background(if (isDark) Color(0xFF252E3B) else Color(0xFFFCFCFD))
            .padding(horizontal = 11.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (isDark) Color(0xFF334052) else Color(0xFFFBE8D2)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AccessTime,
                    contentDescription = null,
                    tint = if (isDark) Color(0xFFFFB47A) else Color(0xFFED631A),
                    modifier = Modifier.size(15.dp)
                )
            }
            Text(
                text = issue.label.ifBlank { "Alert" },
                color = if (isDark) Color(0xFFFFB47A) else Color(0xFFED631A),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = (13.5f * textScale).sp,
                    lineHeight = (15.5f * textScale).sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = issue.flight.ifBlank { "Flight update" },
            color = textColor,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Black,
                fontSize = (12.2f * textScale).sp,
                lineHeight = (14f * textScale).sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = issue.route.ifBlank { "Route pending" },
            color = mutedColor,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = (10.8f * textScale).sp,
                lineHeight = (12f * textScale).sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (issue.time.isNotBlank()) {
            Text(
                text = issue.time,
                color = mutedColor.copy(alpha = if (isDark) 0.82f else 0.88f),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = (10.2f * textScale).sp,
                    lineHeight = (11.5f * textScale).sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FlightAlertPlaceholderRailCard(
    index: Int,
    textColor: Color,
    mutedColor: Color,
    textScale: Float,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(18.dp)
    val labels = listOf("No delay", "On schedule", "No diversion")
    val details = listOf("Flights clear", "No changes", "Airport normal")
    Column(
        modifier = modifier
            .height(108.dp)
            .shadow(elevation = if (isDark) 2.dp else 4.dp, shape = shape, clip = false)
            .clip(shape)
            .background(if (isDark) Color(0xFF252E3B) else Color(0xFFFCFCFD))
            .padding(horizontal = 11.dp, vertical = 10.dp)
            .graphicsLayer { alpha = if (isDark) 0.92f else 0.68f },
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = labels.getOrElse(index % labels.size) { "No alert" },
            color = textColor.copy(alpha = if (isDark) 0.86f else 0.66f),
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Black,
                fontSize = (12.5f * textScale).sp,
                lineHeight = (14.5f * textScale).sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        FlightSkeletonBone(
            modifier = Modifier
                .fillMaxWidth(if (index == 1) 0.68f else 0.82f)
                .height(11.dp),
            color = mutedColor,
            alpha = if (isDark) 0.34f else 0.20f,
            shape = RoundedCornerShape(999.dp)
        )
        Text(
            text = details.getOrElse(index % details.size) { "Schedule clear" },
            color = mutedColor.copy(alpha = if (isDark) 0.88f else 0.70f),
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = (10.2f * textScale).sp,
                lineHeight = (12f * textScale).sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FlightPaginationDots(
    total: Int,
    active: Int,
    accent: Color
) {
    val visibleTotal = total.coerceAtLeast(3).coerceAtMost(6)
    val activeIndex = active.coerceIn(0, visibleTotal - 1)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(visibleTotal) { index ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .width(if (index == activeIndex) 24.dp else 18.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (index == activeIndex) accent else Color(0xFFD6DCE6))
            )
        }
    }
}

@Composable
private fun FlightUpcomingFlightsSection(
    items: List<FlightLiveStatusItem>,
    textColor: Color,
    mutedColor: Color,
    surface: Color,
    textScale: Float,
    highContrast: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Upcoming Flights",
                color = textColor,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = (17f * textScale).sp,
                    lineHeight = (20f * textScale).sp
                ),
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Soonest first",
                color = mutedColor,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = (11.5f * textScale).sp,
                    lineHeight = (13f * textScale).sp
                ),
                maxLines = 1
            )
        }
        if (items.isEmpty()) {
            FlightNoLiveStatusCard(
                textColor = textColor,
                mutedColor = mutedColor,
                surface = surface,
                textScale = textScale
            )
        } else {
            items.forEach { item ->
                FlightUpcomingFlightCard(
                    item = item,
                    textColor = textColor,
                    mutedColor = mutedColor,
                    surface = surface,
                    textScale = textScale,
                    highContrast = highContrast
                )
            }
        }
    }
}

@Composable
private fun FlightUpcomingFlightCard(
    item: FlightLiveStatusItem,
    textColor: Color,
    mutedColor: Color,
    surface: Color,
    textScale: Float,
    highContrast: Boolean
) {
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current
    val accent = if (isDark) Color(0xFFFFB47A) else Color(0xFF9A5A23)
    val shape = RoundedCornerShape(22.dp)
    val flightAwareUrl = remember(item.flight) { flightAwareUrlForFlight(item.flight) }
    val routeEndpoints = flightRouteEndpoints(item.route)
    val leftRoute = routeEndpoints.first
    val rightRoute = routeEndpoints.second
    val scheduled = compactFlightDetailScheduled(item.detail)
    val estimated = compactFlightDetailEstimated(item.detail)
    val eta = item.etaText
        .replace("Scheduled in ", "", ignoreCase = true)
        .replace(" remaining", "", ignoreCase = true)
        .trim()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = if (isDark) 2.dp else 4.dp, shape = shape, clip = false)
            .clip(shape)
            .background(if (isDark) Color(0xFF202734) else Color(0xFFFCFCFD))
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FlightAirlineMark(item.flight, accent, textScale)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = item.flight,
                    color = textColor,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = (14f * textScale).sp,
                        lineHeight = (16f * textScale).sp
                    ),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = leftRoute, color = mutedColor, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black, fontSize = (12f * textScale).sp, lineHeight = (14f * textScale).sp), maxLines = 1)
                if (rightRoute.isNotBlank()) {
                    Icon(imageVector = Icons.Filled.Flight, contentDescription = null, tint = mutedColor, modifier = Modifier.size(15.dp))
                    Text(text = rightRoute, color = mutedColor, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black, fontSize = (12f * textScale).sp, lineHeight = (14f * textScale).sp), maxLines = 1)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                FlightTimeBlock(label = "Sched", value = scheduled.ifBlank { item.meta }, textColor = textColor, mutedColor = mutedColor, textScale = textScale)
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(mutedColor.copy(alpha = 0.18f))
                )
                FlightTimeBlock(label = "Est. arrival", value = estimated.ifBlank { scheduled.ifBlank { "--" } }, textColor = textColor, mutedColor = mutedColor, textScale = textScale)
            }
        }
        Column(
            modifier = Modifier
                .widthIn(min = 64.dp)
                .heightIn(min = 76.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            FlightAlertStatusPill(
                text = readableFlightPillText(item.badge.ifBlank { item.status.ifBlank { "Upcoming" } }),
                textColor = textColor,
                mutedColor = mutedColor,
                textScale = textScale
            )
            if (eta.isNotBlank()) {
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = eta,
                        color = textColor,
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = (15f * textScale).sp,
                            lineHeight = (17f * textScale).sp
                        ),
                        maxLines = 1
                    )
                    Text(
                        text = if (item.tone == "arrived") "arrived" else "to arrival",
                        color = mutedColor,
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = (10f * textScale).sp,
                            lineHeight = (11f * textScale).sp
                        ),
                        maxLines = 1
                    )
                }
            }
            FlightAwareOpenButton(
                textColor = textColor,
                textScale = textScale,
                onClick = { openExternalFlightTracker(context, flightAwareUrl) }
            )
        }
    }
}

@Composable
private fun FlightAwareOpenButton(
    textColor: Color,
    textScale: Float,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    Text(
        text = "FlightAware",
        color = textColor,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Black,
            fontSize = (10f * textScale).sp,
            lineHeight = (11.5f * textScale).sp
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (isDark) Color(0xFF2A3442) else Color(0xFFF0F2F7))
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 4.dp)
    )
}

@Composable
private fun FlightAlertStatusPill(
    text: String,
    textColor: Color,
    mutedColor: Color,
    textScale: Float
) {
    val isDark = isSystemInDarkTheme()
    Text(
        text = text,
        color = if (isDark) textColor else mutedColor,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Black,
            fontSize = (10f * textScale).sp,
            lineHeight = (11.5f * textScale).sp
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (isDark) Color(0xFF2A3442) else Color(0xFFF0F2F7))
            .padding(horizontal = 9.dp, vertical = 4.dp)
    )
}

@Composable
private fun FlightAirlineMark(
    flight: String,
    accent: Color,
    textScale: Float
) {
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current
    val logoUrl = remember(flight) { flightAirlineLogoUrl(flight) }
    val initials = flight
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
        .take(2)
        .ifBlank { "JH" }
    val tileShape = RoundedCornerShape(14.dp)
    var tileModifier = Modifier
        .size(50.dp)
        .shadow(elevation = if (isDark) 2.dp else 4.dp, shape = tileShape, clip = false)
        .clip(tileShape)
    tileModifier = if (logoUrl != null) {
        tileModifier.background(Color(0xFFFCFCFD))
    } else {
        tileModifier.background(
            Brush.linearGradient(
                listOf(
                    Color(0xFF343946).copy(alpha = if (isDark) 0.82f else 0.90f),
                    accent.copy(alpha = if (isDark) 0.72f else 0.78f)
                )
            )
        )
    }
    Box(
        modifier = tileModifier,
        contentAlignment = Alignment.Center
    ) {
        if (logoUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(logoUrl)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .networkCachePolicy(CachePolicy.ENABLED)
                    .crossfade(true)
                    .build(),
                contentDescription = "$flight airline logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp)
            )
        } else {
            Text(
                text = initials,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = (16f * textScale).sp,
                    lineHeight = (18f * textScale).sp
                ),
                maxLines = 1
            )
        }
    }
}

private fun flightAirlineLogoUrl(flight: String): String? {
    val normalized = flight.lowercase(Locale.US)
    val code = when {
        normalized.contains("alaska") || Regex("""(?i)\bAS\s*\d+""").containsMatchIn(flight) -> "AS"
        normalized.contains("american") || Regex("""(?i)\bAA\s*\d+""").containsMatchIn(flight) -> "AA"
        normalized.contains("united") || Regex("""(?i)\bUA\s*\d+""").containsMatchIn(flight) -> "UA"
        normalized.contains("delta") || Regex("""(?i)\bDL\s*\d+""").containsMatchIn(flight) -> "DL"
        normalized.contains("southwest") || Regex("""(?i)\bWN\s*\d+""").containsMatchIn(flight) -> "WN"
        normalized.contains("frontier") || Regex("""(?i)\bF9\s*\d+""").containsMatchIn(flight) -> "F9"
        normalized.contains("jetblue") || Regex("""(?i)\bB6\s*\d+""").containsMatchIn(flight) -> "B6"
        normalized.contains("allegiant") || Regex("""(?i)\bG4\s*\d+""").containsMatchIn(flight) -> "G4"
        normalized.contains("sun country") || Regex("""(?i)\bSY\s*\d+""").containsMatchIn(flight) -> "SY"
        else -> null
    }
    return code?.let { "https://images.kiwi.com/airlines/64/$it.png" }
}

private fun flightAwareUrlForFlight(flight: String): String {
    val ident = flightAwareIdentifier(flight)
    return "https://www.flightaware.com/live/flight/${ident.ifBlank { "JAC" }}"
}

private fun flightAwareIdentifier(flight: String): String {
    val trimmed = flight.trim()
    val direct = Regex("""\b([A-Z]{2,3})\s*([0-9]{1,4}[A-Z]?)\b""", RegexOption.IGNORE_CASE)
        .find(trimmed)
    if (direct != null) {
        return (direct.groupValues[1] + direct.groupValues[2]).uppercase(Locale.US)
    }
    val normalized = trimmed.lowercase(Locale.US)
    val airlineCode = when {
        normalized.contains("united") -> "UAL"
        normalized.contains("american") -> "AAL"
        normalized.contains("delta") -> "DAL"
        normalized.contains("alaska") -> "ASA"
        normalized.contains("southwest") -> "SWA"
        normalized.contains("jetblue") -> "JBU"
        normalized.contains("frontier") -> "FFT"
        normalized.contains("allegiant") -> "AAY"
        normalized.contains("sun country") -> "SCX"
        else -> ""
    }
    val number = Regex("""\b([0-9]{1,4}[A-Z]?)\b""", RegexOption.IGNORE_CASE)
        .find(trimmed)
        ?.groupValues
        ?.getOrNull(1)
        .orEmpty()
        .uppercase(Locale.US)
    if (airlineCode.isNotBlank() && number.isNotBlank()) return airlineCode + number
    return trimmed
        .replace(Regex("""\s+"""), "")
        .filter { it.isLetterOrDigit() }
        .uppercase(Locale.US)
}

@Composable
private fun FlightTimeBlock(
    label: String,
    value: String,
    textColor: Color,
    mutedColor: Color,
    textScale: Float
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = value.ifBlank { "--" },
            color = textColor,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = (13f * textScale).sp,
                lineHeight = (15f * textScale).sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = label,
            color = mutedColor,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = (10f * textScale).sp,
                lineHeight = (11.5f * textScale).sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun compactFlightDetailTime(detail: String): String {
    return compactFlightDetailScheduled(detail).ifBlank { compactFlightDetailEstimated(detail) }
}

private fun compactFlightDetailScheduled(detail: String): String {
    return Regex("""(?i)(?:Sched|Scheduled)\s+([^,•]+)""")
        .find(detail)
        ?.groupValues
        ?.getOrNull(1)
        ?.trim()
        .orEmpty()
}

private fun compactFlightDetailEstimated(detail: String): String {
    return Regex("""(?i)(?:Est|Arrived|Departed)\s+([^,•]+)""")
        .find(detail)
        ?.groupValues
        ?.getOrNull(1)
        ?.trim()
        .orEmpty()
}

@Composable
private fun ColumnScope.FlightTransportationContent(
    textColor: Color,
    mutedColor: Color,
    textScale: Float,
    highContrast: Boolean,
    showHandle: Boolean
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
    FlightTransportInfoCard(
        title = "TaxiPool",
        body = "Meet at the Information Desk across from baggage carousel two. TaxiPool riders receive a $10 discount on each posted destination fare.",
        accent = Color(0xFF34C759),
        textColor = textColor,
        mutedColor = mutedColor,
        textScale = textScale,
        highContrast = highContrast
    )
    FlightTransportInfoCard(
        title = "Ride App Pickup",
        body = "Uber and Lyft pickup is on the north island in front of the ticketing terminal under the Ride App Pickup sign. App pricing can rise during peak demand.",
        accent = Color(0xFF5AC8FA),
        textColor = textColor,
        mutedColor = mutedColor,
        textScale = textScale,
        highContrast = highContrast
    )
    FlightTransportInfoCard(
        title = "Shuttles and START Bus",
        body = "Many hotels offer airport shuttles. Around Jackson, START Bus provides local transportation after arrival.",
        accent = Color(0xFFFFB020),
        textColor = textColor,
        mutedColor = mutedColor,
        textScale = textScale,
        highContrast = highContrast
    )
    FlightTransportInfoCard(
        title = "Driving Notes",
        body = "Night speed limit in the Park is 45 mph. Jackson is idle-free and hands-free. Winter overnight street parking restrictions run Nov 1 to Apr 15 from 3-7 a.m.",
        accent = Color(0xFFFF6B4A),
        textColor = textColor,
        mutedColor = mutedColor,
        textScale = textScale,
        highContrast = highContrast
    )

    Text(
        text = "Taxis & Executive Services",
        color = textColor,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Black,
            fontSize = (16f * textScale).sp,
            lineHeight = (19f * textScale).sp
        ),
        modifier = Modifier.padding(top = 2.dp)
    )
    FlightTransportationProviders.forEach { provider ->
        FlightTransportProviderRow(
            provider = provider,
            textColor = textColor,
            mutedColor = mutedColor,
            textScale = textScale,
            highContrast = highContrast
        )
    }
}

@Composable
private fun FlightTransportInfoCard(
    title: String,
    body: String,
    accent: Color,
    textColor: Color,
    mutedColor: Color,
    textScale: Float,
    highContrast: Boolean
) {
    val isDark = isSystemInDarkTheme()
    val cardShape = RoundedCornerShape(18.dp)
    val cardSurface = if (isDark) Color(0xFF202734) else Color(0xFFFCFCFD)
    val iconAccent = if (isDark) {
        textColor.copy(alpha = 0.82f)
    } else {
        Color(0xFF13294D)
    }
    val iconSurface = if (isDark) Color(0xFF2A3442) else Color(0xFFF0F2F7)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = if (isDark) 2.dp else 4.dp, shape = cardShape, clip = false)
            .clip(cardShape)
            .background(cardSurface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Business,
                contentDescription = null,
                tint = iconAccent,
                modifier = Modifier.size(21.dp)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                color = textColor,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = (14f * textScale).sp,
                    lineHeight = (16f * textScale).sp
                )
            )
            Text(
                text = body,
                color = mutedColor,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = (12f * textScale).sp,
                    lineHeight = (15f * textScale).sp
                )
            )
        }
    }
}

@Composable
private fun FlightTransportProviderRow(
    provider: FlightTransportProvider,
    textColor: Color,
    mutedColor: Color,
    textScale: Float,
    highContrast: Boolean
) {
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current
    val cardShape = RoundedCornerShape(16.dp)
    val rowSurface = if (isDark) Color(0xFF202734) else Color(0xFFFCFCFD)
    val infoAccent = if (isDark) textColor.copy(alpha = 0.84f) else Color(0xFF13294D)
    val callAccent = if (isDark) textColor.copy(alpha = 0.88f) else Color(0xFF13294D)
    val callSurface = if (isDark) Color(0xFF2A3442) else Color(0xFFF0F2F7)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = if (isDark) 1.dp else 3.dp, shape = cardShape, clip = false)
            .clip(cardShape)
            .background(rowSurface)
            .clickable {
                val dialUri = "tel:${provider.phone.filter { it.isDigit() || it == '+' }}".toUri()
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_DIAL, dialUri))
                }
            }
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isDark) Color(0xFF2A3442) else Color(0xFFF0F2F7)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = infoAccent,
                modifier = Modifier.size(17.dp)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = provider.name,
                color = textColor,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = (13f * textScale).sp,
                    lineHeight = (15f * textScale).sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = provider.phone,
                color = mutedColor,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = (11f * textScale).sp,
                    lineHeight = (13f * textScale).sp
                ),
                maxLines = 1
            )
        }
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(callSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Call,
                contentDescription = "Call ${provider.name}",
                tint = callAccent,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun FlightLiveStatusLoadingSkeleton(
    textColor: Color,
    surface: Color,
    highContrast: Boolean
) {
    val pulse = rememberFlightSkeletonPulse()
    FlightWeatherBannerSkeleton(
        textColor = textColor,
        surface = surface,
        pulse = pulse
    )
    FlightIssueSummarySkeleton(
        textColor = textColor,
        surface = surface,
        pulse = pulse,
        highContrast = highContrast
    )
    repeat(FlightAlertSkeletonCardCount) { index ->
        FlightLiveStatusCardSkeleton(
            index = index,
            textColor = textColor,
            pulse = pulse
        )
    }
}

@Composable
private fun FlightWeatherBannerSkeleton(
    textColor: Color,
    surface: Color,
    pulse: Float
) {
    val isDark = isSystemInDarkTheme()
    val cardShape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isDark) 8.dp else 10.dp,
                shape = cardShape,
                clip = false
            )
            .clip(cardShape)
            .background(
                surface.compositeOver(
                    if (isDark) Color(0xFF0E1118) else Color.White
                )
            )
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
            highContrast = highContrast
        )
    }
}

@Composable
private fun FlightCountPillSkeleton(
    index: Int,
    textColor: Color,
    pulse: Float,
    modifier: Modifier = Modifier
) {
    val accents = listOf(
        FlightArrivalCountAccent,
        FlightDepartureCountAccent,
        FlightAlertCountAccent
    )
    val accent = accents[index % accents.size]
    val pillShape = RoundedCornerShape(999.dp)
    val isDark = isSystemInDarkTheme()
    Row(
        modifier = modifier
            .shadow(
                elevation = if (isDark) 8.dp else 10.dp,
                shape = pillShape,
                clip = false
            )
            .clip(pillShape)
            .background(
                if (isDark) {
                    accent.copy(alpha = 0.24f).compositeOver(FlightAlertReadableDarkSurface)
                } else {
                    accent.copy(alpha = 0.14f)
                }
            )
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
    highContrast: Boolean
) {
    val isDark = isSystemInDarkTheme()
    val cardShape = RoundedCornerShape(19.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isDark) 8.dp else 10.dp,
                shape = cardShape,
                clip = false
            )
            .clip(cardShape)
            .background(
                when {
                    highContrast || isDark -> surface
                    else -> Color.White.copy(alpha = 0.46f).compositeOver(surface)
                }
            )
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
    pulse: Float
) {
    val isDark = isSystemInDarkTheme()
    val accent = if (index % 2 == 0) Color(0xFF5AC8FA) else Color(0xFFFFB020)
    val cardSurface = if (isDark) FlightAlertItemDarkSurface else FlightAlertItemLightSurface
    val cardShape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .shadow(
                elevation = if (isDark) 8.dp else 10.dp,
                shape = cardShape,
                clip = false
            )
            .clip(cardShape)
            .background(cardSurface)
            .background(
                Brush.horizontalGradient(
                    0f to accent.copy(alpha = if (isDark) 0.18f else 0.10f),
                    0.45f to accent.copy(alpha = if (isDark) 0.10f else 0.06f),
                    1f to Color.Transparent
                )
            )
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
            .shadow(
                elevation = if (isDark) 8.dp else 10.dp,
                shape = cardShape,
                clip = false
            )
            .clip(cardShape)
            .background(
                surface.compositeOver(
                    if (isDark) Color(0xFF0E1118) else Color.White
                )
            )
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
private fun FlightParkingAvailabilityBox(
    availability: FlightParkingAvailability?,
    textColor: Color,
    mutedColor: Color,
    surface: Color,
    textScale: Float
) {
    val isDark = isSystemInDarkTheme()
    val percent = availability?.percent ?: 0
    val accent = when {
        availability == null -> if (isDark) Color(0xFF9CA3AF) else Color(0xFF64748B)
        percent >= 50 -> Color(0xFF34C759)
        percent >= 20 -> Color(0xFFFCD116)
        else -> Color(0xFFFF6B4A)
    }
    // In dark theme, keep the warning background/progress color,
    // but make the parking symbol and percentage easier to read.
    val parkingForegroundColor = if (isDark) {
        Color.White
    } else {
        when {
            availability == null -> Color(0xFF475569)
            percent >= 50 -> Color(0xFF064E3B)
            percent >= 20 -> Color(0xFF8A4B08)
            else -> Color(0xFFB42318)
        }
    }
    val parkingProgressColor =
        if (isDark) Color.White else Color(0xFF111827)

    val animatedProgress by animateFloatAsState(
        targetValue = (percent / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 620, easing = FastOutSlowInEasing),
        label = "parkingAvailabilityProgress"
    )
    val cardShape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isDark) 8.dp else 10.dp,
                shape = cardShape,
                clip = false
            )
            .clip(cardShape)
            .background(
                surface.compositeOver(
                    if (isDark) Color(0xFF0E1118) else Color.White
                )
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.LocalParking,
                    contentDescription = null,
                    tint = parkingForegroundColor,
                    modifier = Modifier.size(21.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Parking",
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = (14f * textScale).sp,
                        lineHeight = (16f * textScale).sp
                    ),
                    maxLines = 1
                )
                Text(
                    text = availability?.updatedLabel?.ifBlank { "Updated by JAC Airport" }
                        ?: "Checking parking availability",
                    color = mutedColor,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = (11f * textScale).sp,
                        lineHeight = (13f * textScale).sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = availability?.statusLabel ?: "--% AVAILABLE",
                color = parkingForegroundColor,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = (13f * textScale).sp,
                    lineHeight = (15f * textScale).sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(textColor.copy(alpha = if (isDark) 0.14f else 0.10f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(999.dp))
                    .background(parkingProgressColor)
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
            FlightCountPill(
                value = brief.arrivalCount.toString(),
                label = "Arrivals",
                accent = FlightArrivalCountAccent,
                textColor = textColor,
                modifier = Modifier.weight(1f),
                textScale = textScale
            )
            FlightCountPill(
                value = brief.departureCount.toString(),
                label = "Departures",
                accent = FlightDepartureCountAccent,
                textColor = textColor,
                modifier = Modifier.weight(1f),
                textScale = textScale
            )
            FlightCountPill(
                value = totalAlerts.toString(),
                label = "Alerts",
                accent = if (hasIssues) FlightAlertCountAccent else Color(0xFF34C759),
                textColor = textColor,
                modifier = Modifier.weight(1f),
                textScale = textScale
            )
        }
        FlightAlertScheduleContextLine(
            brief = brief,
            textColor = textColor,
            textScale = textScale
        )
        if (brief.issues.isNotEmpty() || hasIssues) {
            FlightAlertsSummaryBox(
                brief = brief,
                totalAlerts = totalAlerts,
                textColor = textColor,
                mutedColor = mutedColor,
                surface = surface,
                textScale = textScale,
                highContrast = highContrast
            )
        } else if (brief.summary.isNotBlank()) {
            FlightClearScheduleSummary(
                summary = brief.summary,
                textColor = textColor,
                mutedColor = mutedColor,
                surface = surface,
                textScale = textScale,
                highContrast = highContrast
            )
        }
    }
}

@Composable
private fun FlightAlertScheduleContextLine(
    brief: FlightSheetBrief,
    textColor: Color,
    textScale: Float
) {
    val isDark = isSystemInDarkTheme()
    val countedDay = compactFlightDayLabel(brief.scheduleDayLabel)
    val upcomingDay = compactFlightDayLabel(brief.upcomingDayLabel)
    val text = when {
        countedDay.isNotBlank() && upcomingDay.isNotBlank() -> "Counts for $countedDay • $upcomingDay upcoming"
        countedDay.isNotBlank() -> "Counts for $countedDay"
        upcomingDay.isNotBlank() -> "$upcomingDay upcoming"
        else -> ""
    }
    if (text.isBlank()) return
    val capsuleShape = RoundedCornerShape(999.dp)
    val capsuleSurface = if (isDark) FlightAlertItemDarkSurface else Color(0xFFF0F8FF)
    val capsuleAccent = if (isDark) Color(0xFFB8F7E4) else Color(0xFF245777)
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .shadow(
                    elevation = if (isDark) 4.dp else 6.dp,
                    shape = capsuleShape,
                    clip = false
                )
                .clip(capsuleShape)
                .background(capsuleSurface)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.CalendarMonth,
                contentDescription = null,
                tint = capsuleAccent,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = text,
                color = if (isDark) textColor.copy(alpha = 0.88f) else Color(0xFF243B8F),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = (10f * textScale).sp,
                    lineHeight = (11.5f * textScale).sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FlightClearScheduleSummary(
    summary: String,
    textColor: Color,
    mutedColor: Color,
    surface: Color,
    textScale: Float,
    highContrast: Boolean
) {
    val isDark = isSystemInDarkTheme()
    val cardShape = RoundedCornerShape(16.dp)
    val accent = if (isDark) Color(0xFF34D399) else Color(0xFF047857)
    val cardSurface = when {
        highContrast -> if (isDark) surface else surface.compositeOver(Color.White)
        isDark -> FlightAlertItemDarkSurface
        else -> Color(0xFFFFFAFA)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isDark) 6.dp else 8.dp,
                shape = cardShape,
                clip = false
            )
            .clip(cardShape)
            .background(cardSurface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(accent)
        )
        Text(
            text = summary,
            color = if (isDark) textColor.copy(alpha = 0.90f) else mutedColor,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = (11.5f * textScale).sp,
                lineHeight = (15f * textScale).sp
            ),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
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
    textScale: Float
) {
    val isDark = isSystemInDarkTheme()
    val cardShape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isDark) 8.dp else 10.dp,
                shape = cardShape,
                clip = false
            )
            .clip(cardShape)
            .background(
                surface.compositeOver(
                    if (isDark) Color(0xFF0E1118) else Color.White
                )
            )
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
        if (isDark) surface else surface.compositeOver(Color.White)
    } else if (isDark) {
        surface
    } else {
        surface.compositeOver(Color(0xFFF8F8FF))
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
            .shadow(
                elevation = if (isDark) 8.dp else 10.dp,
                shape = cardShape,
                clip = false
            )
            .clip(cardShape)
            .background(boxSurface)
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
                        .clickable { expanded = !expanded }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
        if (issuesToShow.isNotEmpty()) {
            issuesToShow.forEach { issue ->
                FlightIssuePill(issue = issue, textColor = textColor, mutedColor = mutedColor, textScale = textScale)
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
    textScale: Float
) {
    val pillShape = RoundedCornerShape(999.dp)
    val isDark = isSystemInDarkTheme()
    val pillSurface = if (isDark) {
        accent.copy(alpha = 0.24f).compositeOver(FlightAlertReadableDarkSurface)
    } else {
        accent.copy(alpha = 0.16f)
    }
    val valueColor = if (isDark) {
        Color.White
    } else {
        Color.Black.copy(alpha = 0.38f).compositeOver(accent)
    }
    Row(
        modifier = modifier
            .shadow(
                elevation = if (isDark) 8.dp else 10.dp,
                shape = pillShape,
                clip = false
            )
            .clip(pillShape)
            .background(pillSurface)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = value,
            color = valueColor,
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
        accent.copy(
            alpha = when {
                isCancelled -> 0.22f
                isDiverted -> 0.18f
                isDelayed -> 0.16f
                else -> 0.12f
            }
        ).compositeOver(FlightAlertReadableDarkSurface)
    } else {
        accent.copy(alpha = 0.13f).compositeOver(Color(0xFFF8F8FF))
    }
    val labelColor = if (isCancelled) {
        if (isDark) Color(0xFFFF7A72) else Color(0xFFB42318)
    } else {
        accent
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isDark) 8.dp else 10.dp,
                shape = pillShape,
                clip = false
            )
            .clip(pillShape)
            .background(rowSurface)
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
    textScale: Float,
    highContrast: Boolean
) {
    val isDark = isSystemInDarkTheme()
    val arrivedAccent = if (isDark) Color(0xFF34D399) else Color(0xFF047857)
    val scheduleAccent = if (isDark) Color(0xFFFFC14D) else Color(0xFFB45309)
    val upcomingAccent = if (isDark) Color(0xFF5AC8FA) else Color(0xFF245B78)
    val cancelledAccent = Color(0xFFFF453A)
    val divertedAccent = if (isDark) Color(0xFFFF9F0A) else Color(0xFFC25B00)
    val isClosedAlert = item.tone == "cancelled" || item.tone == "diverted"
    val isArrived = item.tone == "arrived"
    val accent = when (item.tone) {
        "arrived" -> arrivedAccent
        "delayed" -> scheduleAccent
        "cancelled" -> cancelledAccent
        "diverted" -> divertedAccent
        else -> upcomingAccent
    }
    val cardSurface = when {
        highContrast -> if (isDark) surface else surface.compositeOver(Color.White)
        isArrived && isDark -> Color(0xFF064E3B)
        isArrived -> Color(0xFFB8F7E4)
        isDark -> FlightAlertItemDarkSurface
        else -> FlightAlertItemLightSurface
    }
    val cardShape = RoundedCornerShape(if (isArrived) 14.dp else 20.dp)
    val compactRoute = compactFlightRoute(item.route)
    val statusLine = compactFlightStatusLine(item, compactRoute)
    val detailLine = compactFlightDetailLine(item)
    val progress = item.effectiveProgress()
    val etaMinutes = parseFlightEtaMinutes(item.etaText)
    val progressAccent = when {
        isArrived -> arrivedAccent
        etaMinutes == null -> accent
        etaMinutes <= 1 -> FlightArrivalLantern
        etaMinutes <= 15 -> FlightAlertCountAccent
        etaMinutes <= 30 -> Color(0xFFFFB020)
        else -> FlightDepartureCountAccent
    }
    val fillFraction = if (isClosedAlert) 0f else (progress / 100f).coerceIn(0.04f, 1f)
    val fillEdge = (fillFraction + 0.001f).coerceAtMost(1f)
    val progressFill = progressAccent.copy(alpha = if (isDark) 0.26f else 0.28f)
    val progressFeather = progressAccent.copy(alpha = if (isDark) 0.13f else 0.14f)
    val progressBrush = if (isClosedAlert || isArrived) {
        Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
    } else {
        Brush.horizontalGradient(
            0f to progressFill,
            (fillFraction * 0.88f).coerceIn(0f, 1f) to progressFill,
            fillFraction to progressFeather,
            fillEdge to Color.Transparent,
            1f to Color.Transparent
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isDark) 8.dp else 10.dp,
                shape = cardShape,
                clip = false
            )
            .clip(cardShape)
            .background(cardSurface)
            .background(progressBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (isArrived) 10.dp else 14.dp,
                    vertical = if (isArrived) 6.dp else 12.dp
                ),
            verticalArrangement = Arrangement.spacedBy(
                if (isArrived) 2.dp else 7.dp
            )
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
        .replace('\u00A0', ' ')
        .replace("→", "-")
        .replace(Regex("\\s*-\\s*"), " - ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

private fun flightRouteEndpoints(route: String): Pair<String, String> {
    val compact = compactFlightRoute(route)
    if (compact.isBlank()) return "" to "JAC"
    fun cleanEndpoint(value: String): String {
        return value
            .trim()
            .removeSuffix(" arrival")
            .removeSuffix(" departure")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
    val toParts = compact.split(Regex("\\s+to\\s+", RegexOption.IGNORE_CASE), limit = 2)
    val dashParts = compact.split(" - ", limit = 2)
    val parts = when {
        toParts.size == 2 -> toParts
        dashParts.size == 2 -> dashParts
        else -> listOf(compact)
    }.map(::cleanEndpoint).filter { it.isNotBlank() }
    val rawLeft = parts.firstOrNull().orEmpty()
    val rawRight = parts.getOrNull(1).orEmpty()
    val inferredRight = rawRight.ifBlank {
        Regex("""(?i)\s+to\s+(.+)$""").find(rawLeft)?.groupValues?.getOrNull(1).orEmpty()
    }.ifBlank {
        if (rawLeft.endsWith("JAC", ignoreCase = true)) "" else "JAC"
    }
    val left = cleanEndpoint(
        rawLeft
            .replace(Regex("""(?i)\s+to\s+${Regex.escape(inferredRight)}$"""), "")
            .replace(Regex("""(?i)\s+to\s+JAC$"""), "")
    )
    val right = cleanEndpoint(inferredRight)
    return left to right
}

private fun compactFlightStatusLine(item: FlightLiveStatusItem, route: String): String {
    if (item.tone == "arrived") return route
    if (item.tone == "cancelled") return listOf("Cancelled", route).filter { it.isNotBlank() }.joinToString(" - ")
    if (item.tone == "diverted") return listOf("Diverted", route).filter { it.isNotBlank() }.joinToString(" - ")
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
    if (tone == "cancelled" || tone == "diverted") return 0f
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
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassBottomTabBar(
        modifier = modifier,
        backdrop = backdrop,
        contentView = null,
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
                label = "Transport",
                icon = Icons.Filled.Business,
                selected = selected == "transportation",
                onClick = { onSelect("transportation") },
                lanternColor = LocalAppThemePalette.current.action
            ),
            GlassBottomTabItem(
                label = "Menu",
                icon = Icons.Filled.Menu,
                selected = selected == "menu",
                onClick = { onSelect("menu") },
                lanternColor = LocalAppThemePalette.current.action
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
