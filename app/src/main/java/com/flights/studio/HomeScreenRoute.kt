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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.BlendMode // ✅ IMPORTANT (Compose BlendMode)
import androidx.compose.ui.graphics.Brush
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

private const val TAG = "HomeScreenRoute"

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
    val isDark = isSystemInDarkTheme()

    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }

    var currentTab by rememberSaveable { mutableStateOf(FlightsTab.Curb) }
    var currentCamUrl by rememberSaveable {
        mutableStateOf(
            "https://www.jacksonholeairport.com/wp-content/uploads/webcams/parking-curb.jpg?v=${System.currentTimeMillis()}"
        )
    }

    val refreshIntervalMs = 60_000L
    val minRefreshGapMs = 1_000L
    var countdownMs by rememberSaveable { mutableLongStateOf(refreshIntervalMs) }
    var isRefreshing by rememberSaveable { mutableStateOf(false) }
    var lastRefreshAtMs by rememberSaveable { mutableLongStateOf(0L) }
    var hasInternet by rememberSaveable { mutableStateOf(true) }

    // ✅ single backdrop for image + strip + dialogs + buttons
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
    val onCamExpandedChange: (Boolean) -> Unit = { camExpanded = it }
    val hintText = if (camExpanded) "Drag up, release to collapse" else "Drag down, release to expand"

    var lockNoRefresh by rememberSaveable { mutableStateOf(false) }
    var isZoomInteracting by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val prefs = remember {
        PreferenceManager.getDefaultSharedPreferences(context)
    }
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

    val camProgress by animateFloatAsState(
        targetValue = if (camExpanded) 1f else 0f,
        animationSpec = tween(durationMillis = 450),
        label = "camProgress"
    )
    val gridInteractive = camProgress < 0.02f

    var gridInstanceKey by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(camExpanded) {
        if (!camExpanded) gridInstanceKey++
    }

    // One-shot Siri progress (your function)
    val siriWaveProgress = if (isSiriGlowEnabled) {
        siriCardEdgeGlowOverlay(expanded = camExpanded, durationMillis = 2500)
    } else 0f

    val cardWeight = if (camExpanded) 1f else 0.5f
    val listWeight = if (camExpanded) 0f else 0.5f

    // AUTO REFRESH LOOP
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
                        lastRefreshAtMs = now

                        val newUrl = when (currentTab) {
                            FlightsTab.Curb ->
                                "https://www.jacksonholeairport.com/wp-content/uploads/webcams/parking-curb.jpg?v=${System.currentTimeMillis()}"
                            FlightsTab.North ->
                                "https://www.jacksonholeairport.com/wp-content/uploads/webcams/parking-north.jpg?v=${System.currentTimeMillis()}"
                            FlightsTab.South ->
                                "https://www.jacksonholeairport.com/wp-content/uploads/webcams/parking-south.jpg?v=${System.currentTimeMillis()}"
                        }

                        currentCamUrl = newUrl
                        triggerRefreshNow(newUrl)

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
            "card5" -> launchPlain(QRCodeActivity::class.java)
            "card6" -> launchPlain(SettingsActivity::class.java)
            "card7" -> launchPlain(AllContactsActivity::class.java)
            "card8" -> launchPlain(AllNotesActivity::class.java)
            "card9" -> launchPlain(ProfileDetailsComposeActivity::class.java)
            else -> Unit
        }
    }

    val cardShape = RoundedCornerShape(20.dp)
//    val cardElevationDp = lerp(4f, 1f, camProgress).dp
    val cardElevationDp = lerp(6f, 14f, interactiveHighlight.pressProgress).dp



    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ===========================
            // TOP CAMERA CARD AREA (FIXED)
            // ===========================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(cardWeight)
                    .padding(4.dp)
            ) {
                val extraScale = 0.006f * sin(camProgress * PI).toFloat()
                val cardScale = 1f + extraScale

                // OUTER: transforms + shadow
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            if (isInteractive) {
                                val height = size.height

                                val press = interactiveHighlight.pressProgress
                                val zoomAmountPx = 1.0.dp.toPx()
                                val baseScale = lerp(1f, 1f + zoomAmountPx / height, press)

                                val k = 0.035f
                                val offsetY = interactiveHighlight.offset.y
                                translationY = height * tanh(k * offsetY / height)

                                val maxDragScale = 30.dp.toPx() / height
                                val verticalStretch = maxDragScale * abs(offsetY / height)

                                scaleX = baseScale * cardScale
                                scaleY = (baseScale + verticalStretch) * cardScale
                            } else {
                                scaleX = cardScale
                                scaleY = cardScale
                            }
                        }


                        // ✅ SHADOW RESTORED
                        .shadow(
                            elevation = cardElevationDp,
                            shape = cardShape,
                            clip = false   // ✅ MUST be false or shadow gets cut off
                        )
                ) {
                    // INNER CLIP
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(cardShape) // ✅ simpler than graphicsLayer clip
                    ) {
                        // 1) WRITER: image only
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .layerBackdrop(cameraBackdrop)
                        ) {
                            Box(
                                Modifier
                                    .matchParentSize()
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                if (isDark) Color(0x9B202A36) else Color(0x9BF7F9FC),
                                                if (isDark) Color(0x9F0E1116) else Color(0x9BE3E9F2)
                                            )
                                        )
                                    )
                            )

                            HomeGlassZoomImage(
                                model = currentCamUrl,
                                modifier = Modifier.matchParentSize(),
                                onImageLoadedOk = {
                                    hasInternet = true
                                    isRefreshing = false
                                    lockNoRefresh = false
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                },
                                onImageLoadFailed = {
                                    isRefreshing = false
                                    hasInternet = false
                                    lockNoRefresh = true
                                    scope.launch {
                                        snackbarHostState.currentSnackbarData?.dismiss()
                                        snackbarHostState.showSnackbar("Camera image failed to load")
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
                                            brightness = if (isDark) 0.1f else 0.0f,
                                            contrast = 1.45f,
                                            saturation = 1.2f
                                        )

                                        if (!isZoomInteracting) {
                                            blur(if (isDark) 0.dp.toPx() else 0.dp.toPx())
                                            lens(
                                                refractionHeight = 1.dp.toPx(),
                                                refractionAmount = 1.dp.toPx(),
                                                depthEffect = true,
                                                chromaticAberration = false
                                            )
                                        }

                                    },
                                    onDrawSurface = {
                                        // 👇 ADD THIS BACK 👇
                                        if (isDark) {
                                            drawRect(Color.Black.copy(alpha = 0.10f))
                                        }
                                        if (tint.isSpecified) {
                                            drawRect(tint, blendMode = BlendMode.Hue)
                                            drawRect(tint.copy(alpha = 0.65f))
                                        }
                                        if (surfaceColor.isSpecified) {
                                            drawRect(surfaceColor)
                                        }
                                    }
                                )
                        )
                        // 3) grab zone overlay
                        val gestureConfig = remember {
                            ExpandCollapseGestureConfig(expandDistance = 180.dp)
                        }
                        Box(
                            Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(66.dp)
                                .zIndex(10f)
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

                        val hintAlpha by animateFloatAsState(
                            targetValue = 1f,
                            animationSpec = tween(250),
                            label = "hintAlpha"
                        )

                        val handleColor = if (isDark) {
                            Color.White.copy(alpha = 0.35f)
                        } else {
                            Color.Black.copy(alpha = 0.22f)
                        }

                        val hintColor = if (isDark) {
                            Color.White.copy(alpha = 0.75f)
                        } else {
                            Color.Black.copy(alpha = 0.60f)
                        }

                        BottomProgressiveBlurStrip(
                            backdrop = cameraBackdrop,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .zIndex(1f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 20.dp)
                                    .padding(bottom = 8.dp)
                                    .graphicsLayer {
                                        translationY = nudge.value
                                        alpha = hintAlpha
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                repeat(2) {
                                    Box(
                                        Modifier
                                            .size(width = 36.dp, height = 3.dp)
                                            .clip(CircleShape)
                                            .background(handleColor)
                                    )
                                }
                                Spacer(Modifier.height(0.dp))
                                Text(
                                    text = hintText,
                                    color = hintColor,
                                    style = MaterialTheme.typography.labelMedium
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
                                .zIndex(20f)
                        )
                    }
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
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val isLightTheme = !isSystemInDarkTheme()
    val tintColor = if (isLightTheme) Color(0xFFFFFFFF) else Color(0xFF000000)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .drawPlainBackdrop(
                backdrop = backdrop,
                shape = { RectangleShape },
                effects = {
                    vibrancy()
                    blur(3.dp.toPx())

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
