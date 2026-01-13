package com.flights.studio

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.RenderEffect
import android.os.Build
import android.util.Log
import androidx.activity.compose.LocalActivity
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import androidx.preference.PreferenceManager
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.effect
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.tanh
import androidx.core.content.edit
import androidx.fragment.app.FragmentActivity

const val TAG = "HomeScreenRoute"
private fun camBaseUrl(tab: FlightsTab): String = when (tab) {
    FlightsTab.Curb  -> "https://www.jacksonholeairport.com/wp-content/uploads/webcams/parking-curb.jpg"
    FlightsTab.North -> "https://www.jacksonholeairport.com/wp-content/uploads/webcams/parking-north.jpg"
    FlightsTab.South -> "https://www.jacksonholeairport.com/wp-content/uploads/webcams/parking-south.jpg"
}


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun HomeScreenRouteContent(
    backdrop: LayerBackdrop,
    openFullScreenImages: (String) -> Unit,
    openMenuSheet: () -> Unit,
    triggerRefreshNow: (String?) -> Unit,
    finishApp: () -> Unit,
    showExitDialog: Boolean,
    isInteractive: Boolean = true,
    onDismissExit: () -> Unit,
    tint: Color = Color.Unspecified,
    surfaceColor: Color = Color.Unspecified,
    onConfirmExit: () -> Unit
) {
    val activity = LocalActivity.current
    val hostActivity = activity as? FragmentActivity
    val isDark = isSystemInDarkTheme()
    val ui = rememberUiScale()


    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }

    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    val initialToken = remember { prefs.getLong("cam_last_token", 0L) }

    var currentTab by rememberSaveable { mutableStateOf(FlightsTab.Curb) }
    var refreshToken by rememberSaveable { mutableLongStateOf(initialToken) }

    val refreshIntervalMs = 60_000L
    val minRefreshGapMs = 1_000L

    var countdownMs by rememberSaveable { mutableLongStateOf(refreshIntervalMs) }
    var isRefreshing by rememberSaveable { mutableStateOf(false) }
    var lastRefreshAtMs by rememberSaveable { mutableLongStateOf(0L) }
    var hasInternet by rememberSaveable { mutableStateOf(true) }

    val cameraBackdrop = rememberLayerBackdrop()

    var wifiEnabled by rememberSaveable { mutableStateOf(false) }
    var dataEnabled by rememberSaveable { mutableStateOf(false) }
    var isUserOffline by rememberSaveable { mutableStateOf(false) }
    var justBecameOnline by rememberSaveable { mutableStateOf(false) }

    val currentCamUrl = remember(currentTab, refreshToken) {
        val base = camBaseUrl(currentTab)
        if (refreshToken == 0L) base else "$base?v=$refreshToken"
    }

    var isZoomInteracting by remember { mutableStateOf(false) }

    var isSiriGlowEnabled by remember {
        mutableStateOf(prefs.getBoolean("siri_camera_glow", true))
    }

    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "siri_camera_glow") {
                isSiriGlowEnabled = prefs.getBoolean("siri_camera_glow", true)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    // ----------------------------
    // STARTUP: show cached image immediately, then refresh if old
    // ----------------------------
    LaunchedEffect(Unit) {
        // if token is older than refreshIntervalMs, refresh after a short delay
        val age = System.currentTimeMillis() - refreshToken
        if (refreshToken == 0L || age > refreshIntervalMs) {
            delay(500) // let cached image show first
            val token = System.currentTimeMillis()
            refreshToken = token
            lastRefreshAtMs = token
            countdownMs = refreshIntervalMs

            // optional: tell whoever cares "refresh now"
            triggerRefreshNow(currentCamUrl)
        } else {
            // token is recent -> start countdown from remaining time
            countdownMs = (refreshIntervalMs - age).coerceAtLeast(0L)
        }
    }

    // ----------------------------
    // CONNECTIVITY MONITOR
    // ----------------------------
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

                    val token = System.currentTimeMillis()
                    refreshToken = token
                    lastRefreshAtMs = token
                    countdownMs = refreshIntervalMs

                    triggerRefreshNow(currentCamUrl)

                    // ✅ optional "back online" toast
                    hostActivity?.let { FancyPillToast.show(it, "Back online") } // :contentReference[oaicite:3]{index=3}

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

    var camExpanded by rememberSaveable { mutableStateOf(false) }
    val onCamExpandedChange: (Boolean) -> Unit = { camExpanded = it }
    var lockNoRefresh by rememberSaveable { mutableStateOf(false) }

    val camProgress by animateFloatAsState(
        targetValue = if (camExpanded) 1f else 0f,
        animationSpec = tween(durationMillis = 450),
        label = "camProgress"
    )
    val gridInteractive = camProgress < 0.02f

    var gridInstanceKey by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(camExpanded) { if (!camExpanded) gridInstanceKey++ }

    val siriWaveProgress = if (isSiriGlowEnabled) {
        siriCardEdgeGlowOverlay(expanded = camExpanded, durationMillis = 2500)
    } else 0f

    val cardWeight = if (camExpanded) 1f else 0.5f
    val listWeight = if (camExpanded) 0f else 0.5f

    // ----------------------------
    // AUTO REFRESH LOOP
    // ----------------------------
    LaunchedEffect(hasInternet, lockNoRefresh) {
        while (true) {
            if (!hasInternet) {
                delay(1000)
                continue
            }
            if (lockNoRefresh) {
                delay(250)
                continue
            }

            if (!isRefreshing) {
                if (countdownMs <= 0L) {
                    val now = System.currentTimeMillis()
                    if (now - lastRefreshAtMs >= minRefreshGapMs) {
                        isRefreshing = true

                        val token = System.currentTimeMillis()
                        refreshToken = token
                        lastRefreshAtMs = token

                        triggerRefreshNow(currentCamUrl)

                        delay(3_000L)
                        isRefreshing = false
                        countdownMs = refreshIntervalMs
                    } else {
                        delay(250)
                    }
                } else {
                    delay(1000)
                    countdownMs -= 1000
                }
            } else {
                delay(250)
            }
        }
    }

    val onTabChangeInternal: (FlightsTab) -> Unit = inner@{ tab ->
        if (tab == currentTab) return@inner

        val token = System.currentTimeMillis()

        // 1) update state
        currentTab = tab
        refreshToken = token
        lastRefreshAtMs = token
        countdownMs = refreshIntervalMs
        lockNoRefresh = false

        // 2) build the NEW url directly (no stale remember)
        val newUrl = "${camBaseUrl(tab)}?v=$token"

        // 3) refresh using the correct url
        triggerRefreshNow(newUrl)
    }


    @Suppress("DEPRECATION")
    val openActivityByCard: (String) -> Unit = { id ->
        fun launchPlain(cls: Class<*>) {
            activity?.startActivity(Intent(activity, cls))
            activity?.overridePendingTransition(R.anim.zoom_in, 0)
        }

        fun launchCardScreen(cardId: String) {
            val i = Intent(activity, CardBottomSheetActivity::class.java).apply {
                putExtra("CARD_ID", cardId)
                putExtra("RETURN_HOME", true)
            }
            activity?.startActivity(i)
            activity?.overridePendingTransition(R.anim.zoom_in, 0)
        }

        fun launchPlayerScreen() {
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
            "card5" -> launchPlain(QRCodeComposeActivity::class.java)
            "card6" -> launchPlain(SettingsActivity::class.java)
            "card7" -> launchPlain(AllContactsActivity::class.java)
            "card8" -> launchPlain(AllNotesActivity::class.java)
            "card9" -> launchPlain(ProfileDetailsComposeActivity::class.java)
            else -> Unit
        }
    }

    val cardShape = RoundedCornerShape(20.dp)
    val extraScale = 0.006f * sin(camProgress * PI).toFloat()
    val cardScale = 1f + extraScale
    val shape = RoundedCornerShape(22.dp)
    val themedSurface =
        MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            // TOP CAMERA CARD AREA (FIXED)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(cardWeight)
                    .padding(4.dp.us(ui))

            ) {

                Box(
                    modifier = Modifier
                        .matchParentSize()

                        // 1) SHADOW FIRST (this is what will move)
                        .shadow(
                            elevation = 0.dp,
                            shape = shape,
                            clip = false
                        )

                        // 2) then transform
                        .graphicsLayer {
                            if (isInteractive) {
                                val height = size.height
                                val press = interactiveHighlight.pressProgress

                                val zoomAmountPx = 1.0.dp.toPx()
                                val baseScale =
                                    lerp(1f, 1f + zoomAmountPx / height, press)

                                val k = 0.035f
                                val offsetY = interactiveHighlight.offset.y

                                translationY =
                                    height * tanh(k * offsetY / height)

                                val maxDragScale =
                                    30.dp.toPx() / height

                                val verticalStretch =
                                    maxDragScale * abs(offsetY / height)

                                scaleX = baseScale * cardScale
                                scaleY = (baseScale + verticalStretch) * cardScale
                            } else {
                                scaleX = cardScale
                                scaleY = cardScale
                            }
                        }
                        .background(
                            color = themedSurface,
                            shape = shape
                        )
                ) {

                        // 1) WRITER: image only
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .layerBackdrop(cameraBackdrop)
                        ) {

                            HomeGlassZoomImage(
                                model = currentCamUrl,
                                modifier = Modifier.matchParentSize(),
                                onImageLoadedOk = {
                                    hasInternet = true
                                    isRefreshing = false
                                    lockNoRefresh = false
                                    prefs.edit { putLong("cam_last_token", refreshToken) }

                                },
                                onImageLoadFailed = {
                                    isRefreshing = false
                                    hasInternet = false
                                    lockNoRefresh = true

                                    // ✅ Fancy pill instead of snackbar :contentReference[oaicite:4]{index=4}
                                    hostActivity?.let {
                                        FancyPillToast.show(it, "Camera image failed to load")
                                    }
                                }
                            )
                        }

                        // 2) READER: glass overlay reads backdrop
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .drawBackdrop(
                                    backdrop = cameraBackdrop,
                                    shape = { cardShape },
                                    shadow = null,
                                    effects = {
                                        // ✅ YOU WANTED THIS EVEN WHILE LOADING
                                        vibrancy()
                                        colorControls(
                                            brightness = if (isDark) 0.01f else 0.0f,
                                            contrast = 1.25f,
                                            saturation = 1.3f
                                        )

                                        if (!isZoomInteracting) {
                                            blur(if (isDark) 0.dp.toPx() else 0.dp.toPx())
                                            lens(
                                                refractionHeight = 0.dp.toPx(),
                                                refractionAmount = 0.dp.toPx(),
                                                depthEffect = true,
                                                chromaticAberration = false
                                            )
                                        }

                                    },

                                    onDrawSurface = {
                                        if (isDark) drawRect(Color.Black.copy(alpha = 0.20f))
                                        if (tint.isSpecified) {
                                            drawRect(tint, blendMode = BlendMode.Hue)
                                            drawRect(tint.copy(alpha = 0.35f))
                                        }
                                        if (surfaceColor.isSpecified) {
                                            drawRect(surfaceColor)
                                        }
                                    }
                                )
                        )
                        // 3) grab zone overlay
                        val gestureConfig = remember {
                            ExpandCollapseGestureConfig(
                                expandDistance = 180.dp.us(ui)
                            )

                        }
                        Box(
                            Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(86.dp.us(ui))
                                .zIndex(0f)
                                .then(if (isInteractive) interactiveHighlight.gestureModifier else Modifier)
                                .expandCollapseGrabGesture(
                                    isExpanded = camExpanded,
                                    onExpandedChange = onCamExpandedChange,
                                    config = gestureConfig
                                )
                        )

                        // Siri overlay
                        if (isSiriGlowEnabled) {
                            SiriWaveOverlay(
                                progress = siriWaveProgress,
                                modifier = Modifier
                                    .matchParentSize()
                                    .zIndex(0.5f)
                            )
                        }

                        // nudge hint
                        val nudge = remember { Animatable(0f) }
                        LaunchedEffect(camExpanded) {
                            nudge.stop()
                            nudge.snapTo(0f)
                            if (!camExpanded) {
                                nudge.animateTo(12f, tween(420, easing = FastOutSlowInEasing))
                                delay(120)
                                nudge.animateTo(0f, tween(520, easing = FastOutSlowInEasing))
                            } else {
                                nudge.animateTo(-10f, tween(420, easing = FastOutSlowInEasing))
                                delay(120)
                                nudge.animateTo(0f, tween(520, easing = FastOutSlowInEasing))
                            }
                        }
                        LaunchedEffect(Unit) {
                            // if we have no token yet, or it's older than ~1 minute, refresh quickly
                            val age = System.currentTimeMillis() - refreshToken
                            if (refreshToken == 0L || age > 60_000L) {
                                delay(500) // let UI show cached last image first
                                refreshToken = System.currentTimeMillis()
                                lastRefreshAtMs = refreshToken
                                countdownMs = refreshIntervalMs
                            }
                        }

//                        BottomProgressiveBlurStrip(
//                            backdrop = cameraBackdrop,
//                            modifier = Modifier
//                                .align(Alignment.BottomCenter)
//                                .zIndex(0f)
//                        ) {

                        val hintAlpha by animateFloatAsState(
                            targetValue = 1f,
                            animationSpec = tween(250),
                            label = "hintAlpha"
                        )

                    val handle = Color.White.copy(alpha = 0.88f)
                    val handleUnder = Color.Black.copy(alpha = 0.30f)

                    val infinite = rememberInfiniteTransition(label = "handleInfinite")

                    val breathe by infinite.animateFloat(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "handleBreathe"
                    )

                    val shimmer by infinite.animateFloat(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 2200, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "handleShimmer"
                    )

                    val driftTarget = if (camExpanded) -3f else 3f
                    val drift by animateFloatAsState(
                        targetValue = driftTarget,
                        animationSpec = tween(240, easing = FastOutSlowInEasing),
                        label = "handleDrift"
                    )

                    val arrowRotation by animateFloatAsState(
                        targetValue = if (camExpanded) 180f else 0f,
                        animationSpec = tween(240, easing = FastOutSlowInEasing),
                        label = "hintArrowRotation"
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 15.dp.us(ui))
                    ) {
                        Column(
                            modifier = Modifier.graphicsLayer {
                                // base nudge + our drift + a gentle breathe
                                translationY = nudge.value + drift + lerp(-1.2f, 1.2f, breathe)
                                alpha = hintAlpha
                            },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // ✅ Premium stagger: each line has slightly different width/alpha
                            // Also, add subtle shimmer: middle line slightly brighter over time
                            repeat(3) { i ->
                                val t = (i / 2f) // 0..1
                                val lineWidth = lerp(24f, 32f, 1f - abs(t - 0.5f) * 2f).dp
                                val baseAlpha = lerp(0.70f, 0.92f, 1f - abs(t - 0.5f) * 2f)

                                // shimmer gives a gentle “focus” to the middle line
                                val shimmerBoost = if (i == 1) lerp(0.0f, 0.10f, shimmer) else 0f

                                Box(
                                    Modifier
                                        .size(width = lineWidth.us(ui), height = 2.dp.us(ui))
                                        .clip(CircleShape)
                                        .background(handleUnder.copy(alpha = 0.22f + shimmerBoost))
                                        .padding(0.5.dp)
                                        .clip(CircleShape)
                                        .background(handle.copy(alpha = baseAlpha + shimmerBoost))
                                )
                            }

                            Icon(
                                imageVector = Icons.Filled.ExpandMore,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.80f),
                                modifier = Modifier
                                    .size(18.dp.us(ui))
                                    .graphicsLayer {
                                        rotationZ = arrowRotation
                                        // tiny follow drift + breathe
                                        translationY = (drift * 0.35f) + lerp(-0.6f, 0.6f, breathe)
                                        alpha = 0.85f
                                    }
                            )
                        }
                    }



                    // EXIT DIALOG (RESTORED)
                        if (showExitDialog) {
                            ExitLiquidDialog(
                                modifier = Modifier
                                    .matchParentSize()
                                    .zIndex(3f),
                                backdrop = cameraBackdrop,
                                onCancel = { onDismissExit() },
                                onConfirmExit = { onConfirmExit() }
                            )
                        }

                        // OFFLINE HUD (RESTORED)
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

                        // TOP ICONS (RESTORED)
                        TopBarLiquidIconButton(
                            iconRes = R.drawable.ic_oui_arrow_to_left,
                            backdrop = cameraBackdrop,
                            onClick = { finishApp() },
                            modifier = Modifier
                                .zIndex(5f)
                                .align(Alignment.TopStart)
                                .padding(start = 12.dp, top = 24.dp)
                        )

                        TopBarLiquidIconButton(
                            iconRes = R.drawable.more_vert_24dp_ffffff_fill1_wght400_grad0_opsz24,
                            backdrop = cameraBackdrop,
                            onClick = { openMenuSheet() },
                            modifier = Modifier
                                .zIndex(5f)
                                .align(Alignment.TopEnd)
                                .padding(end = 12.dp, top = 24.dp)
                        )

                        // REFRESH PILL (RESTORED)
                        RefreshStatusPill(
                            backdrop = cameraBackdrop,
                            isRefreshing = isRefreshing,
                            countdownMs = countdownMs,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 12.dp, bottom = 11.dp)
                                .zIndex(10f)
                        )
                    }
                }


            // ===========================
            // BOTTOM LIST / CARDS AREA
            // ===========================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(listWeight.coerceAtLeast(0.0001f))
                    .graphicsLayer { alpha = 1f - camProgress }
            ) {
                key(gridInstanceKey) {
                    FlightsGlassScreen(
                        selectedTab = currentTab,
                        onTabChanged = { tab -> onTabChangeInternal(tab) },
                        onFullScreen = { openFullScreenImages(currentCamUrl) },
                        onBack = { finishApp() },
                        onMenu = { openMenuSheet() },
                        onOpenCard = { cardId -> openActivityByCard(cardId) },
                        showTopArea = false,
                        isInteractive = gridInteractive,
                        backdropOverride = backdrop
                    )
                }
            }
        }
    }
}



@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun BottomProgressiveBlurStrip(
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val isLightTheme = !isSystemInDarkTheme()
    val tintColor = if (isLightTheme) Color(0xFFFFFFFF) else Color(0xFF000000)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(66.dp)
            .drawPlainBackdrop(
                backdrop = backdrop,
                shape = { RectangleShape },
                effects = {
                    vibrancy()
                    blur(0.dp.toPx())

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
    float blurAlpha = smoothstep(size.y, size.y * 0.15, y);
    float tintAlpha = smoothstep(size.y, size.y * 0.25, y);
    return mix(content.eval(coord) * blurAlpha, tint * tintAlpha, tintIntensity);
}
                                    """.trimIndent()
                                ).apply {
                                    setFloatUniform("size", size.width, size.height)
                                    setColorUniform("tint", tintColor.toArgb())
                                    setFloatUniform("tintIntensity", 0.8f)
                                },
                                "content"
                            )
                        )
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) { content() }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Preview(showBackground = true, device = "id:pixel_8")
@Composable
fun HomeScreenPreview() {
    val mockBackdrop = rememberLayerBackdrop()
    MaterialTheme {
        HomeScreenRouteContent(
            backdrop = mockBackdrop,
            openFullScreenImages = {},
            openMenuSheet = {},
            triggerRefreshNow = {}, // ✅ ADD THIS
            finishApp = {},
            showExitDialog = false,
            isInteractive = true,
            onDismissExit = {},
            onConfirmExit = {}
        )
    }
}
