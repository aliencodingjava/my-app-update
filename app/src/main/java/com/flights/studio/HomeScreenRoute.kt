package com.flights.studio

import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.activity.compose.LocalActivity
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "HomeScreenRoute"

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun HomeScreenRouteContent(
    backdrop: LayerBackdrop,
    openFullScreenImages: (String) -> Unit,
    openMenuSheet: () -> Unit,
    triggerRefreshNow: (String?) -> Unit,
    finishApp: () -> Unit,
    showExitDialog: Boolean,         // ← NEW
    onDismissExit: () -> Unit,       // ← NEW
    onConfirmExit: () -> Unit        // ← NEW
) {
    val activity = LocalActivity.current
    rememberLayerBackdrop()

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
    val dialogImageBackdrop = rememberLayerBackdrop()

    // 🔌 connection state we show in CameraErrorOverlay
    var wifiEnabled by rememberSaveable { mutableStateOf(false) }
    var dataEnabled by rememberSaveable { mutableStateOf(false) }
    var isUserOffline by rememberSaveable { mutableStateOf(false) }

    // ✨ NEW: did we JUST recover from offline?
    var justBecameOnline by rememberSaveable { mutableStateOf(false) }

    // ✨ connectivity monitor loop
    LaunchedEffect(activity) {
        val ctx = activity ?: return@LaunchedEffect

        // ✨ track previous offline state
        var wasOffline = isUserOffline

        while (true) {
            try {
                // wifi toggle
                wifiEnabled = ctx.isWifiEnabledNow()

                // mobile data radio up? (new helper you’ll add in CameraErrorOverlay.kt file)
                dataEnabled = ctx.isCellDataToggleOn()

                // are we offline (no internet at all)?
                val nowOffline = ctx.isNetworkCompletelyOff()
                isUserOffline = nowOffline

                // ✨ detect transition: offline -> online
                if (wasOffline && !nowOffline) {
                    // we just got internet back
                    justBecameOnline = true
                    hasInternet = true // make sure refresh loop can run again

                    // ✨ soft-force webcam url refresh immediately
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

                    // ✨ show highlight glow 3s then clear
                    launch {
                        delay(3000)
                        justBecameOnline = false
                    }
                }

                // keep memory for next loop
                wasOffline = nowOffline

            } catch (t: Throwable) {
                Log.w(TAG, "connectivity poll failed: ${t.message}")
            }

            // poll every 1s
            delay(1000)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    var camExpanded by rememberSaveable { mutableStateOf(false) }
    var refreshPaused by rememberSaveable { mutableStateOf(false) }
    var lockNoRefresh by rememberSaveable { mutableStateOf(false) }

    // DEBUG: log state any time these change
    LaunchedEffect(camExpanded) {
        Log.d(TAG, "camExpanded changed -> $camExpanded")
    }
    LaunchedEffect(refreshPaused) {
        Log.d(TAG, "refreshPaused changed -> $refreshPaused")
    }
    LaunchedEffect(lockNoRefresh) {
        Log.d(TAG, "lockNoRefresh changed -> $lockNoRefresh")
    }
    LaunchedEffect(currentCamUrl) {
        Log.d(TAG, "currentCamUrl changed -> $currentCamUrl")
    }

    // AUTO REFRESH LOOP (normal periodic refresh)
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

        // leaving fullscreen -> allow auto-refresh again
        lockNoRefresh = false
    }

    // EXPAND/CLOSE BUTTON
    fun toggleExpanded() {
        if (!camExpanded) {
            // COLLAPSED -> EXPANDED
            camExpanded = true
            refreshPaused = true
            lockNoRefresh = false

            Log.d(
                TAG,
                "toggleExpanded: EXPAND -> camExpanded=${true} refreshPaused=${true} lockNoRefresh=${false}"
            )

        } else {
            // EXPANDED -> COLLAPSED
            camExpanded = false

            // after closing we want to stop immediate reloads
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
            "card10", "card11", "card12", "card13", "card14", "card15", "card16" -> { /* no-op */
            }

            else -> { /* no-op */
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (camExpanded) 1f else 0.5f)
                    .padding(8.dp)
            ) {
                // 1) Card backdrop (exported to children — safe, no recursion)
                val cameraBackdrop = rememberLayerBackdrop()
                // 2) Pure image backdrop (records ONLY the image subtree)
                val isDark = androidx.compose.foundation.isSystemInDarkTheme()

                // LIQUID GLASS CARD (rounded, normal)
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .drawBackdrop(
                            backdrop = backdrop,
                            exportedBackdrop = cameraBackdrop,          // export to children
                            shape = { RoundedCornerShape(20.dp) },
                            shadow = null,
                            effects = {
                                vibrancy()
                                blur(if (isDark) 4.dp.toPx() else 6.dp.toPx())
                                lens(16.dp.toPx(), 32.dp.toPx())
                            },
                            onDrawSurface = {
                                val base = if (isDark) 0.10f else 0.06f
                                drawRect(Color.White.copy(alpha = base))
                                if (!isDark) {
                                    drawRect(Color.Black.copy(alpha = 0.14f),
                                        blendMode = androidx.compose.ui.graphics.BlendMode.Multiply)
                                    drawRect(Color.Black.copy(alpha = 0.05f),
                                        blendMode = androidx.compose.ui.graphics.BlendMode.Saturation)
                                }
                            }


                        )

                        .clip(RoundedCornerShape(20.dp))
                ) {
                    // --- IMAGE WRITER: record ONLY the image into imageBackdrop ---
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .layerBackdrop(dialogImageBackdrop)
                    ) {
                        // camera view (spinner, etc) — use the EXPORTED card backdrop for placeholder
                        ZoomableImageContentInlineImpl(
                            model = currentCamUrl,
                            modifier = Modifier
                                .matchParentSize()
                                .zIndex(0f),
                            onImageLoadedOk = {
                                if (!hasInternet) {
                                    hasInternet = true
                                    countdownMs = refreshIntervalMs
                                    lastRefreshAtMs = System.currentTimeMillis()
                                    Log.d(
                                        TAG,
                                        "onImageLoadedOk: internet restored, resume auto-refresh"
                                    )
                                }
                            },
                            onImageLoadFailed = {
                                if (hasInternet) {
                                    hasInternet = false
                                    isRefreshing = false
                                    countdownMs = refreshIntervalMs
                                    Log.d(
                                        TAG,
                                        "onImageLoadFailed: no internet, stop auto-refresh loop"
                                    )
                                }
                            },
                            // keep empty so HUD/spinners don’t pollute imageBackdrop
                            overlayContent = { }
                        )
                    }

// --- EXIT DIALOG that SAMPLES the IMAGE backdrop ----------------
                    if (showExitDialog) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .zIndex(10f) // above everything
                        ) {
                            ExitLiquidDialog(
                                backdrop = dialogImageBackdrop,   // ← use the IMAGE backdrop
                                onCancel = { onDismissExit() },
                                onConfirmExit = { onConfirmExit() }
                            )
                        }
                    }
                    // --- READERS: glass UI that should sample the IMAGE (use imageBackdrop) ---

                    // offline / recovering HUD
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
                            backdrop = dialogImageBackdrop,                   // SAMPLE IMAGE, not scaffold
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

                    TopBarLiquidIconButton(
                        iconRes = R.drawable.ic_oui_arrow_to_left,
                        backdrop = dialogImageBackdrop,                      // ← IMAGE backdrop
                        onClick = { finishApp() },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 12.dp, top = 24.dp)
                    )

                    TopBarLiquidIconButton(
                        iconRes = R.drawable.more_vert_24dp_ffffff_fill1_wght400_grad0_opsz24,
                        backdrop = dialogImageBackdrop,                      // ← IMAGE backdrop
                        onClick = { openMenuSheet() },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 12.dp, top = 24.dp)
                    )

                    LiquidGlassExpandButton(
                        camExpanded = camExpanded,
                        onToggle = { toggleExpanded() },
                        backdrop = dialogImageBackdrop,  // ← IMAGE backdrop

                        // NEW options:
                        expandHapticStyle = ExpandHapticStyle.DoubleClick, // or Heavy, Bouncy
                        enableHaptics = true,

                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 12.dp, bottom = 12.dp)
                    )
                }
            }

            if (!camExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.5f)
                ) {
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
