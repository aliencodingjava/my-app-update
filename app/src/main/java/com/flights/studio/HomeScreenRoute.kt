package com.flights.studio

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.RenderEffect
import android.os.Build
import android.util.Log
import androidx.activity.compose.LocalActivity
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.preference.PreferenceManager
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.effect
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

private const val TAG = "HomeScreenRoute"
private const val TAG_ZOOM = "HomeZoom"

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun HomeScreenRouteContent(
    backdrop: LayerBackdrop,
    openFullScreenImages: (String) -> Unit,
    openMenuSheet: () -> Unit,
    triggerRefreshNow: (String?) -> Unit,
    finishApp: () -> Unit,
    showExitDialog: Boolean,
    onDismissExit: () -> Unit,
    onConfirmExit: () -> Unit
) {
    val activity = LocalActivity.current

    var currentTab by rememberSaveable { mutableStateOf(FlightsTab.Curb) }
    var currentCamUrl by rememberSaveable {
        mutableStateOf(
            "https://www.jacksonholeairport.com/wp-content/uploads/webcams/parking-curb.jpg?v=${System.currentTimeMillis()}"
        )
    }

    val refreshIntervalMs = 90_000L
    val minRefreshGapMs = 1_000L
    var countdownMs by rememberSaveable { mutableLongStateOf(refreshIntervalMs) }
    var isRefreshing by rememberSaveable { mutableStateOf(false) }
    var lastRefreshAtMs by rememberSaveable { mutableLongStateOf(0L) }
    var hasInternet by rememberSaveable { mutableStateOf(true) }

    // single backdrop for image + buttons + strip + dialogs
    val cameraBackdrop = rememberLayerBackdrop()

    // connection state
    var wifiEnabled by rememberSaveable { mutableStateOf(false) }
    var dataEnabled by rememberSaveable { mutableStateOf(false) }
    var isUserOffline by rememberSaveable { mutableStateOf(false) }
    var justBecameOnline by rememberSaveable { mutableStateOf(false) }

    // connectivity monitor loop
    LaunchedEffect(activity) {
        val ctx = activity ?: return@LaunchedEffect

        var wasOffline = isUserOffline

        while (true) {
            try {
                wifiEnabled = ctx.isWifiEnabledNow()
                dataEnabled = ctx.isCellDataToggleOn()
                val nowOffline = ctx.isNetworkCompletelyOff()
                isUserOffline = nowOffline

                if (wasOffline && !nowOffline) {
                    justBecameOnline = true
                    hasInternet = true

                    val newUrl = when (currentTab) {
                        FlightsTab.Curb ->
                            "https://www.jacksonholeairport.com/wp-content/uploads/webcams/parking-curb.jpg?v=${System.currentTimeMillis()}"
                        FlightsTab.North ->
                            "https://www.jacksonholeairport.com/wp-content/uploads/webcams/parking-north.jpg?v=${System.currentTimeMillis()}"
                        FlightsTab.South ->
                            "https://www.jacksonholeairport.com/wp-content/uploads/webcams/parking-south.jpg?v=${System.currentTimeMillis()}"
                    }
                    currentCamUrl = newUrl
                    lastRefreshAtMs = System.currentTimeMillis()
                    countdownMs = refreshIntervalMs

                    launch {
                        delay(3000)
                        justBecameOnline = false
                    }
                }

                wasOffline = nowOffline
            } catch (t: Throwable) {
                Log.w(TAG, "connectivity poll failed: ${t.message}")
            }

            delay(1000)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var camExpanded by rememberSaveable { mutableStateOf(false) }
    var refreshPaused by rememberSaveable { mutableStateOf(false) }
    var lockNoRefresh by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }

    var isSiriGlowEnabled by remember {
        mutableStateOf(
            prefs.getBoolean("siri_camera_glow", true)
        )
    }

    // Listen for changes to that key while this composable is active
    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "siri_camera_glow") {
                isSiriGlowEnabled = prefs.getBoolean("siri_camera_glow", true)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)

        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    // ✅ Stable progress for card + button (0 = collapsed, 1 = expanded)
    val camProgress by animateFloatAsState(
        targetValue = if (camExpanded) 1f else 0f,
        animationSpec = tween(durationMillis = 450),
        label = "camProgress"
    )

    // ✅ One-shot progress only for the Siri edge glow (or 0f if disabled)
    val siriWaveProgress = if (isSiriGlowEnabled) {
        siriCardEdgeGlowOverlay(
            expanded = camExpanded,
            durationMillis = 2500
        )
    } else 0f



    // discrete weights so the grid doesn’t constantly shrink
    val cardWeight = if (camExpanded) 1f else 0.5f
    val listWeight = if (camExpanded) 0f else 0.5f

    LaunchedEffect(camExpanded) { Log.d(TAG, "camExpanded changed -> $camExpanded") }
    LaunchedEffect(refreshPaused) { Log.d(TAG, "refreshPaused changed -> $refreshPaused") }
    LaunchedEffect(lockNoRefresh) { Log.d(TAG, "lockNoRefresh changed -> $lockNoRefresh") }
    LaunchedEffect(currentCamUrl) { Log.d(TAG, "currentCamUrl changed -> $currentCamUrl") }

    // AUTO REFRESH LOOP
    LaunchedEffect(hasInternet, refreshPaused, lockNoRefresh) {
        Log.d(
            TAG,
            "LaunchedEffect start loop: hasInternet=$hasInternet refreshPaused=$refreshPaused lockNoRefresh=$lockNoRefresh"
        )
        while (true) {
            Log.d(
                TAG,
                "LOOP TICK | expanded=$camExpanded paused=$refreshPaused locked=$lockNoRefresh " +
                        "countdownMs=$countdownMs isRefreshing=$isRefreshing lastRefreshAtMs=$lastRefreshAtMs"
            )

            if (!hasInternet) {
                Log.d(TAG, "LOOP: no internet -> sleep 1000")
                delay(1000)
                continue
            }

            if (refreshPaused || lockNoRefresh) {
                Log.d(
                    TAG,
                    "LOOP: blocked because refreshPaused=$refreshPaused or lockNoRefresh=$lockNoRefresh -> sleep 250"
                )
                delay(250)
                continue
            }

            if (!isRefreshing) {
                if (countdownMs <= 0L) {
                    val now = System.currentTimeMillis()
                    Log.d(
                        TAG,
                        "LOOP: countdownMs <= 0L, now=$now, lastRefreshAtMs=$lastRefreshAtMs, gap=${now - lastRefreshAtMs}"
                    )

                    if (now - lastRefreshAtMs >= minRefreshGapMs) {
                        Log.d(TAG, "LOOP: starting refresh sequence (isRefreshing=true)")
                        lastRefreshAtMs = now

                        val newUrl = when (currentTab) {
                            FlightsTab.Curb ->
                                "https://www.jacksonholeairport.com/wp-content/uploads/webcams/parking-curb.jpg?v=${System.currentTimeMillis()}"
                            FlightsTab.North ->
                                "https://www.jacksonholeairport.com/wp-content/uploads/webcams/parking-north.jpg?v=${System.currentTimeMillis()}"
                            FlightsTab.South ->
                                "https://www.jacksonholeairport.com/wp-content/uploads/webcams/parking-south.jpg?v=${System.currentTimeMillis()}"
                        }

                        Log.d(TAG, "LOOP: assigning currentCamUrl=$newUrl")
                        currentCamUrl = newUrl

                        snackbarHostState.currentSnackbarData?.dismiss()
                        Log.d(TAG, "LOOP: snackbar 'Refreshing camera…'")
                        snackbarHostState.showSnackbar("Refreshing camera…")

                        Log.d(TAG, "LOOP: triggerRefreshNow($newUrl)")
                        triggerRefreshNow(newUrl)

                        delay(800)
                        isRefreshing = false
                        countdownMs = refreshIntervalMs
                        Log.d(
                            TAG,
                            "LOOP: refresh done. isRefreshing=false countdownMs reset to $refreshIntervalMs"
                        )
                    } else {
                        Log.d(TAG, "LOOP: too soon since last refresh, sleeping 250")
                        delay(250)
                    }
                } else {
                    Log.d(
                        TAG,
                        "LOOP: countdown ticking. before=$countdownMs after=${countdownMs - 1000}"
                    )
                    delay(1000)
                    countdownMs -= 1000
                }
            } else {
                Log.d(TAG, "LOOP: currently refreshing, sleep 250")
                delay(250)
            }
        }
    }

    val onTabChangeInternal: (FlightsTab) -> Unit = inner@{ tab ->
        if (tab == currentTab) {
            Log.d(
                TAG,
                "onTabChangeInternal -> same tab=$tab -> ignore, keep currentCamUrl=$currentCamUrl"
            )
            return@inner
        }

        Log.d(TAG, "onTabChangeInternal -> NEW tab=$tab (oldTab=$currentTab) -> reload")

        currentTab = tab
        currentCamUrl = when (tab) {
            FlightsTab.Curb ->
                "https://www.jacksonholeairport.com/wp-content/uploads/webcams/parking-curb.jpg?v=${System.currentTimeMillis()}"
            FlightsTab.North ->
                "https://www.jacksonholeairport.com/wp-content/uploads/webcams/parking-north.jpg?v=${System.currentTimeMillis()}"
            FlightsTab.South ->
                "https://www.jacksonholeairport.com/wp-content/uploads/webcams/parking-south.jpg?v=${System.currentTimeMillis()}"
        }

        countdownMs = refreshIntervalMs
        lastRefreshAtMs = System.currentTimeMillis()
        lockNoRefresh = false
    }

    fun toggleExpanded() {
        if (!camExpanded) {
            camExpanded = true
            refreshPaused = true
            lockNoRefresh = false
            Log.d(
                TAG,
                "toggleExpanded: EXPAND -> camExpanded=true refreshPaused=true lockNoRefresh=false"
            )
        } else {
            camExpanded = false
            refreshPaused = false
            lockNoRefresh = true
            countdownMs = refreshIntervalMs
            lastRefreshAtMs = System.currentTimeMillis()
            Log.d(
                TAG,
                "toggleExpanded: COLLAPSE -> camExpanded=$camExpanded refreshPaused=$refreshPaused lockNoRefresh=$lockNoRefresh " +
                        "countdownMs=$countdownMs lastRefreshAtMs=$lastRefreshAtMs"
            )
        }
    }

    @Suppress("DEPRECATION")
    val openActivityByCard: (String) -> Unit = { id ->
        Log.d(TAG, "openActivityByCard: $id")
        fun launchPlain(cls: Class<*>) {
            Log.d(TAG, "launchPlain: ${cls.simpleName}")
            activity?.startActivity(Intent(activity, cls))
            activity?.overridePendingTransition(R.anim.zoom_in, 0)
        }

        fun launchCardScreen(cardId: String) {
            Log.d(TAG, "launchCardScreen: $cardId")
            val i = Intent(activity, CardBottomSheetActivity::class.java).apply {
                putExtra("CARD_ID", cardId)
                putExtra("RETURN_HOME", true)
            }
            activity?.startActivity(i)
            activity?.overridePendingTransition(R.anim.zoom_in, 0)
        }

        fun launchPlayerScreen() {
            Log.d(TAG, "launchPlayerScreen")
            val i = Intent(activity, IosPlayerActivity::class.java).apply {
                putExtra("RETURN_HOME", true)
            }
            activity?.startActivity(i)
            activity?.overridePendingTransition(R.anim.zoom_in, 0)
        }

        when (id) {
            "card1" -> launchPlayerScreen()
            "card2" -> launchCardScreen("card2")
            "card3" -> launchCardScreen("card3")
            "card4" -> launchCardScreen("card4")
            "card5" -> launchPlain(QRCodeActivity::class.java)
            "card6" -> launchPlain(SettingsActivity::class.java)
            "card7" -> launchPlain(AllContactsActivity::class.java)
            "card8" -> launchPlain(AllNotesActivity::class.java)
            "card9" -> launchPlain(ProfileDetailsActivity::class.java)
            "card10", "card11", "card12", "card13", "card14", "card15", "card16" -> { /* no-op */ }
            else -> { /* no-op */ }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // ===== TOP CAMERA CARD AREA =====
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(cardWeight)
                    .padding(4.dp)
            ) {
                val isDark = isSystemInDarkTheme()
                val extraScale = 0.006f * sin(camProgress * PI).toFloat()
                val cardScale = 1f + extraScale
                val cardElevation = 4.dp + (16.dp - 4.dp) * camProgress
                val cardShape = RoundedCornerShape(20.dp)
                Log.d(TAG_ZOOM, "CameraCard: camProgress=$camProgress cardScale=$cardScale cardWeight=$cardWeight listWeight=$listWeight")

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            shape = cardShape
                            clip = true
                            scaleX = cardScale
                            scaleY = cardScale
                            shadowElevation = cardElevation.toPx()
                        }
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { cardShape },
                            shadow = null,
                            effects = {
                                vibrancy()
                                blur(if (isDark) 4.dp.toPx() else 6.dp.toPx())
                                lens(16.dp.toPx(), 32.dp.toPx())
                            },
                            onDrawSurface = {
                                Log.d(TAG_ZOOM, "CameraCard drawSurface size=(${size.width} x ${size.height})")

                                val base = if (isDark) 0.10f else 0.06f
                                drawRect(Color.White.copy(alpha = base))
                                if (!isDark) {
                                    drawRect(
                                        Color.Black.copy(alpha = 0.14f),
                                        blendMode = BlendMode.Multiply
                                    )
                                    drawRect(
                                        Color.Black.copy(alpha = 0.05f),
                                        blendMode = BlendMode.Saturation
                                    )
                                }
                            }
                        )
                ) {
                    // --- IMAGE WRITER (feeds cameraBackdrop) ---
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .layerBackdrop(cameraBackdrop)
                    ) {
                        Log.d(TAG_ZOOM, "ZoomableImageContentInlineImpl: composing with url=$currentCamUrl")

                        ZoomableImageContentInlineImpl(
                            model = currentCamUrl,
                            modifier = Modifier
                                .matchParentSize()
                                .zIndex(0f),
                            onImageLoadedOk = {
                                Log.d(TAG_ZOOM, "ZoomableImage: onImageLoadedOk url=$currentCamUrl")
                                hasInternet = true
                                isRefreshing = false
                                lockNoRefresh = false
                                snackbarHostState.currentSnackbarData?.dismiss()
                            },
                            onImageLoadFailed = {
                                Log.e(
                                    TAG_ZOOM,
                                    "ZoomableImage: onImageLoadFailed for url=$currentCamUrl"
                                )
                                isRefreshing = false
                                hasInternet = false
                                lockNoRefresh = true

                                scope.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    snackbarHostState.showSnackbar("Camera image failed to load")
                                }
                            },
                            onBitmapReady = {
                                Log.d(TAG_ZOOM, "ZoomableImage: onBitmapReady (bitmap handled inside compositor)")
                                // hook for future adaptive luminance from webcam
                            }

                        )
                    }
                    // 🔹 SIRI-LIKE WAVE OVERLAY (magic flash)
                    if (isSiriGlowEnabled) {
                        SiriWaveOverlay(
                            progress = siriWaveProgress,
                            modifier = Modifier
                                .matchParentSize()
                                .zIndex(0.5f) // above image, below chrome
                        )
                    }

                    // --- BOTTOM STRIP (liquid glass over image) ---
                    BottomProgressiveBlurStrip(
                        backdrop = cameraBackdrop,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .zIndex(1f)
                    )

                    // --- EXIT DIALOG (glass over image) ---
                    if (showExitDialog) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .zIndex(10f)
                        ) {
                            ExitLiquidDialog(
                                backdrop = cameraBackdrop,
                                onCancel = { onDismissExit() },
                                onConfirmExit = { onConfirmExit() }
                            )
                        }
                    }

                    // --- OFFLINE HUD (glass) ---
                    if (!hasInternet || isUserOffline) {
                        CameraErrorOverlay(
                            modifier = Modifier
                                .matchParentSize()
                                .zIndex(3f),
                            signalStrength = null,
                            isUserOffline = isUserOffline,
                            wifiEnabled = wifiEnabled,
                            dataEnabled = dataEnabled,
                            justRecovered = justBecameOnline,
                            backdrop = cameraBackdrop,
                            onRequestEnableWifi = {
                                activity?.startActivity(
                                    Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)
                                )
                            },
                            onRequestEnableData = {
                                activity?.startActivity(
                                    Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS)
                                )
                            }
                        )
                    }

                    // --- TOP ICONS (glass over image) ---
                    TopBarLiquidIconButton(
                        iconRes = R.drawable.ic_oui_arrow_to_left,
                        backdrop = cameraBackdrop,
                        onClick = { finishApp() },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 12.dp, top = 24.dp)
                    )

                    TopBarLiquidIconButton(
                        iconRes = R.drawable.more_vert_24dp_ffffff_fill1_wght400_grad0_opsz24,
                        backdrop = cameraBackdrop,
                        onClick = { openMenuSheet() },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 12.dp, top = 24.dp)
                    )

                    // --- EXPAND BUTTON (glass over image) ---
                    LiquidGlassExpandButton(
                        camExpanded = camExpanded,
                        openProgress = camProgress,
                        onToggle = { toggleExpanded() },
                        backdrop = cameraBackdrop,
                        expandHapticStyle = ExpandHapticStyle.DoubleClick,
                        adaptiveLuminance = true,   // 🔴 turn off for now see if crash again
                        enableHaptics = true,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 12.dp, bottom = 12.dp)
                            .zIndex(2f)
                    )
                }
            }

            // ===== BOTTOM LIST / CARDS AREA =====
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(listWeight.coerceAtLeast(0.0001f))
                    .graphicsLayer {
                        // fade out while card expands
                        alpha = 1f - camProgress
                    }
            ) {
                if (!camExpanded) {
                    FlightsGlassScreen(
                        onTabChanged = { tab -> onTabChangeInternal(tab) },
                        onFullScreen = { openFullScreenImages(currentCamUrl) },
                        onBack = { finishApp() },
                        onMenu = { openMenuSheet() },
                        onOpenCard = { cardId -> openActivityByCard(cardId) },
                        showTopArea = false,
                        backdropOverride = backdrop
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp)
        )
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun BottomProgressiveBlurStrip(
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier
) {
    val isLightTheme = !isSystemInDarkTheme()

    val tintColor = if (isLightTheme) {
        Color(0x9FFFFFFF)
    } else {
        Color(0xDF000000)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(86.dp)
            .drawPlainBackdrop(
                backdrop = backdrop,
                shape = { RectangleShape },
                effects = {
                    blur(4.dp.toPx())

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                        effect(
                            RenderEffect.createRuntimeShaderEffect(
                                obtainRuntimeShader(
                                    "AlphaMask",
                                    """
                                    uniform shader content;
                                    uniform float2 size;
                                    layout(color) uniform half4 tint;
                                    uniform float tintIntensity;

                                    half4 main(float2 coord) {
                                        float y = size.y - coord.y;

                                        float blurAlpha = smoothstep(size.y, size.y * 0.5, y);
                                        float tintAlpha = smoothstep(size.y, size.y * 0.5, y);

                                        return mix(
                                            content.eval(coord) * blurAlpha,
                                            tint * tintAlpha,
                                            tintIntensity
                                        );
                                    }
                                    """.trimIndent()
                                ).apply {
                                    setFloatUniform("size", size.width, size.height)
                                    setColorUniform("tint", tintColor.toArgb())
                                    setFloatUniform("tintIntensity", 0.7f)
                                },
                                "content"
                            )
                        )
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        // optional strip content
    }
}
