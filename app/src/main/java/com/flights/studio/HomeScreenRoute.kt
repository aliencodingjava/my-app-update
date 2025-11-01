package com.flights.studio

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

/**
 * This IS your SplashActivity migrated to Compose.
 *
 * It:
 *  - auto-refreshes webcam every 90 seconds
 *  - shows countdown / battery / time / internet state
 *  - renders FlightsGlassScreen (glass buttons + bottom bar)
 *  - wires the buttons to start Activities
 *
 * You will call this from MainActivity.
 */
@Composable
fun HomeScreenRoute(
    // things that used to be SplashActivity methods:
    openFullScreenImages: () -> Unit,
    openMenuSheet: () -> Unit,
    triggerRefreshNow: (String?) -> Unit,
    openBottomSheetCard: (String) -> Unit,
    finishApp: () -> Unit,
    vm: SplashViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity

    // -------------------
    // STATE copied from SplashActivity logic
    // -------------------

    // which camera tab is active
    var currentTab by rememberSaveable { mutableStateOf(FlightsTab.Curb) }

    // current webcam URL
    var currentCamUrl by rememberSaveable {
        mutableStateOf(
            "https://www.jacksonholeairport.com/wp-content/uploads/webcams/parking-curb.jpg?v=${System.currentTimeMillis()}"
        )
    }

    // countdown refresh cycle
    val refreshIntervalMs = 90_000L
    val minRefreshGapMs = 1_000L

    var countdownMs by rememberSaveable { mutableLongStateOf(refreshIntervalMs) }
    var isRefreshing by rememberSaveable { mutableStateOf(false) }
    var lastRefreshAtMs by rememberSaveable { mutableLongStateOf(0L) }

    // network state (true for now, you can wire NetworkConnectivityHelper later)
    var hasInternet by rememberSaveable { mutableStateOf(true) }

    // snackbar host ("Refreshing camera…")
    val snackbarHostState = remember { SnackbarHostState() }

    // -------------------
    // ViewModel-driven system info
    // -------------------
    val uiTime by vm.currentTimeFlow().collectAsState(initial = System.currentTimeMillis())
    val batteryState by vm.batteryInfoFlow().collectAsState(
        initial = BatteryUiState(percent = 80, charging = true)
    )

    // -------------------
    // COUNTDOWN / AUTO REFRESH LOOP
    // -------------------
    LaunchedEffect(hasInternet) {
        while (true) {
            if (!hasInternet) {
                delay(1000)
                continue
            }

            if (!isRefreshing) {
                if (countdownMs <= 0L) {
                    val now = System.currentTimeMillis()
                    if (now - lastRefreshAtMs >= minRefreshGapMs) {
                        // refresh
                        isRefreshing = true
                        lastRefreshAtMs = now

                        // swap webcam URL with cache-bust timestamp
                        currentCamUrl = when (currentTab) {
                            FlightsTab.Curb ->
                                "https://www.jacksonholeairport.com/wp-content/uploads/webcams/parking-curb.jpg?v=${System.currentTimeMillis()}"
                            FlightsTab.North ->
                                "https://www.jacksonholeairport.com/wp-content/uploads/webcams/parking-north.jpg?v=${System.currentTimeMillis()}"
                            FlightsTab.South ->
                                "https://www.jacksonholeairport.com/wp-content/uploads/webcams/parking-south.jpg?v=${System.currentTimeMillis()}"
                        }

                        snackbarHostState.currentSnackbarData?.dismiss()
                        snackbarHostState.showSnackbar("Refreshing camera…")

                        // This is where SplashActivity called Glide etc.
                        // We're calling through to native trigger so you can keep that logic in Activity if you still want.
                        triggerRefreshNow(currentCamUrl)

                        delay(800)

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

    // -------------------
    // when user taps tab (curb/north/south)
    // -------------------
    val onTabChangeInternal: (FlightsTab) -> Unit = { tab ->
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
    }

    // -------------------
    // helpers to open the old Activities from Compose buttons
    // -------------------
    val openActivityByCard: (String) -> Unit = { id ->
        @Suppress("DEPRECATION")
        fun launch(cls: Class<*>) {
            activity?.startActivity(Intent(activity, cls))
            activity?.overridePendingTransition(R.anim.zoom_in, 0)
        }

        when (id) {
            "card5" -> launch(QRCodeActivity::class.java)
            "card6" -> launch(SettingsActivity::class.java)
            "card7" -> launch(AllContactsActivity::class.java)
            "card8" -> launch(AllNotesActivity::class.java)
            "card9" -> launch(ProfileDetailsActivity::class.java)
            else -> openBottomSheetCard(id)
        }
    }

    // -------------------
    // UI
    // -------------------
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // fallback bg while we style glass
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {

            // --- TOP: this replaces MaterialCardView + TouchImageView ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp) // you can tune to match MotionLayout height
            ) {
                // the webcam image
                AsyncImage(
                    model = currentCamUrl,
                    contentDescription = "Airport webcam",
                    modifier = Modifier
                        .fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // overlay debug info (we'll style this later to match your glass chips)
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Cam:\n$currentCamUrl",
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Countdown: ${countdownMs / 1000}s  refreshing=$isRefreshing  net=$hasInternet",
                        color = Color.White
                    )
                    Text(
                        text = "Time(ms): $uiTime  Batt=${batteryState.percent}% chg=${batteryState.charging}",
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- MIDDLE: your round shortcuts grid, bottom bar, fab, etc ---
            // We pass ALL the stuff FlightsGlassScreen needs so it's not "dead"
            FlightsGlassScreen(
                currentImageUrl = currentCamUrl,
                countdownMs = countdownMs,
                isRefreshing = isRefreshing,
                internetAvailable = hasInternet,
                currentTimeMillis = uiTime,
                batteryPercent = batteryState.percent,
                isCharging = batteryState.charging,
                onTabChanged = { tab ->
                    onTabChangeInternal(tab)
                },
                onFullScreen = {
                    // fullscreen webcams bottom sheet (old SplashActivity)
                    openFullScreenImages()
                },
                onBack = {
                    // this was the back bubble
                    finishApp()
                },
                onMenu = {
                    // 3-dot menu bubble
                    openMenuSheet()
                },
                onOpenCard = { id ->
                    // grid item click
                    openActivityByCard(id)
                }
            )

            Spacer(modifier = Modifier.height(48.dp))
        }

        // global snackbar host (bottom overlay)
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )
    }
}
