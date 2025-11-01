package com.flights.studio

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

/* ---------- Connectivity (compact) ---------- */

private data class NetSnapshot(
    val hasValidatedInternet: Boolean,
    val anyWifi: Boolean,
    val anyCellular: Boolean
)

@Suppress("DEPRECATION")
private fun Context.readConnectivitySnapshot(): NetSnapshot {
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val active = cm.activeNetwork ?: return NetSnapshot(
        hasValidatedInternet = false,
        anyWifi = false,
        anyCellular = false
    )
    val caps = cm.getNetworkCapabilities(active) ?: return NetSnapshot(
        hasValidatedInternet = false,
        anyWifi = false,
        anyCellular = false
    )

    val validated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    val hasInternetCap = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

    return NetSnapshot(
        hasValidatedInternet = validated || hasInternetCap,
        anyWifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI),
        anyCellular = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    )
}

fun Context.isNetworkCompletelyOff(): Boolean = !readConnectivitySnapshot().hasValidatedInternet
fun Context.isWifiEnabledNow(): Boolean = readConnectivitySnapshot().anyWifi
fun Context.isCellDataToggleOn(): Boolean = readConnectivitySnapshot().anyCellular

@Suppress("DEPRECATION")
private fun Context.safeWifiRssiPercent(): Int = try {
    val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    val info = wm.connectionInfo ?: return 0
    WifiManager.calculateSignalLevel(info.rssi, 100).coerceIn(1, 99)
} catch (_: Throwable) {
    60
}

/* ---------- Glass helpers (no tint) ---------- */

@Composable
private fun GlassPanelBare(
    backdrop: LayerBackdrop,
    corner: Dp = 10.dp,
    isInteractive: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val animationScope = rememberCoroutineScope()
    val interactive = remember(animationScope) { InteractiveHighlight(animationScope) }

    Column(
        Modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(corner) },
                effects = {
                    // strict order, no surface tint:
                    vibrancy()
                    blur(8.dp.toPx(), edgeTreatment = TileMode.Decal)
                    lens(12.dp.toPx(), 24.dp.toPx())
                },
                layerBlock = if (isInteractive) {
                    {
                        val w = size.width
                        val h = size.height
                        val press = interactive.pressProgress
                        val base = lerp(1f, 1f + 4.dp.toPx() / h, press)
                        val off = interactive.offset
                        val maxOff = size.minDimension
                        val k = 0.05f
                        translationX = maxOff * tanh(k * off.x / maxOff)
                        translationY = maxOff * tanh(k * off.y / maxOff)

                        val maxScale = 4.dp.toPx() / h
                        val ang = atan2(off.y, off.x)
                        scaleX = base + maxScale * abs(cos(ang) * off.x / size.maxDimension) * (w / h).coerceAtMost(1f)
                        scaleY = base + maxScale * abs(sin(ang) * off.y / size.maxDimension) * (h / w).coerceAtMost(1f)
                    }
                } else null,
                onDrawSurface = { /* ← NO TINT, pure glass */ }
            )
            .then(if (isInteractive) Modifier.then(interactive.modifier).then(interactive.gestureModifier) else Modifier)
            .clip(RoundedCornerShape(corner))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) { content() }
}

@Composable
private fun LiquidRowButtonGlass(
    label: String,
    enabled: Boolean,
    backdrop: LayerBackdrop,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    isInteractive: Boolean = true,
    onClick: () -> Unit
) {
    val animationScope = rememberCoroutineScope()
    val interactive = remember(animationScope) { InteractiveHighlight(animationScope) }

    Row(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(12.dp) },
                effects = {
                    vibrancy()
                    blur(8.dp.toPx(), edgeTreatment = TileMode.Decal)
                    lens(12.dp.toPx(), 24.dp.toPx())
                },
                layerBlock = if (isInteractive) {
                    {
                        val w = size.width
                        val h = size.height
                        val press = interactive.pressProgress
                        val base = lerp(1f, 1f + 2.dp.toPx() / h, press)
                        val off = interactive.offset
                        val maxOff = size.minDimension
                        val k = 0.05f
                        translationX = maxOff * tanh(k * off.x / maxOff)
                        translationY = maxOff * tanh(k * off.y / maxOff)

                        val maxScale = 2.dp.toPx() / h
                        val ang = atan2(off.y, off.x)
                        scaleX = base + maxScale * abs(cos(ang) * off.x / size.maxDimension) * (w / h).coerceAtMost(1f)
                        scaleY = base + maxScale * abs(sin(ang) * off.y / size.maxDimension) * (h / w).coerceAtMost(1f)
                    }
                } else null,
                onDrawSurface = { /* ← NO TINT */ }
            )
            .clickable(
                interactionSource = null,
                indication = if (isInteractive) null else LocalIndication.current,
                role = Role.Button,
                onClick = onClick
            )
            .then(if (isInteractive) Modifier.then(interactive.modifier).then(interactive.gestureModifier) else Modifier)
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (isDark) Color.White.copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.9f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = if (enabled) "ON" else "OFF",
            color = if (enabled) {
                if (isDark) Color.White else Color.Black
            } else {
                if (isDark) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.6f)
            },
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/* ---------- UI: Camera error overlay (no tint panel + no tint toggles) ---------- */

@Composable
fun CameraErrorOverlay(
    modifier: Modifier = Modifier,
    backdrop: LayerBackdrop,
    signalStrength: Int? = null,
    isUserOffline: Boolean = false,
    wifiEnabled: Boolean = false,
    dataEnabled: Boolean = false,
    justRecovered: Boolean = false,
    showError: Boolean = false,                 // ← NEW: controls error vs spinner
    onRequestEnableWifi: () -> Unit = {},
    onRequestEnableData: () -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    val textColorMain = if (isDark) Color.White.copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.9f)
    val textColorSub  = if (isDark) Color.White.copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.7f)
    val ctx = LocalContext.current
    var liveSignal by remember { mutableIntStateOf(0) }

    // Poll connectivity once per second
    LaunchedEffect(Unit) {
        while (true) {
            try {
                val snap = ctx.readConnectivitySnapshot()
                liveSignal = when {
                    snap.anyWifi -> ctx.safeWifiRssiPercent()
                    snap.anyCellular && snap.hasValidatedInternet -> 65
                    snap.anyCellular && !snap.hasValidatedInternet -> 40
                    else -> 0
                }
            } catch (_: Throwable) {
                liveSignal = 50
            }
            delay(1000)
        }
    }

    val raw = (signalStrength ?: liveSignal).coerceIn(0, 100)
    val animated by animateIntAsState(targetValue = raw)

    val shouldShowNow = isUserOffline || raw < 25
    var holdShow by remember { mutableStateOf(false) }
    LaunchedEffect(shouldShowNow) {
        if (!shouldShowNow) {
            holdShow = true
            delay(6000)
            holdShow = false
        }
    }
    val overlayVisible = shouldShowNow || holdShow || justRecovered

    // animate "..."
    var dot by remember { mutableIntStateOf(0) }
    LaunchedEffect(overlayVisible) {
        if (overlayVisible) {
            while (true) { dot = (dot + 1) % 4; delay(500) }
        } else dot = 0
    }
    val dots = when (dot) { 1 -> "."; 2 -> ".."; 3 -> "..."; else -> "" }

    val baseStatus = when {
        isUserOffline -> "No connection"
        raw < 15      -> "Signal lost"
        raw < 25      -> "Reconnecting"
        raw < 50      -> "Weak signal"
        else          -> "Live"
    }
    val statusText = if (shouldShowNow) baseStatus + dots else baseStatus

    AnimatedVisibility(visible = overlayVisible, enter = fadeIn(), exit = fadeOut()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = if (isDark) 0f else 0.08f),
                            Color.Transparent
                        ),
                        radius = 800f
                    )
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .padding(bottom = 80.dp)
                    .widthIn(max = 260.dp)
            ) {
                // PANEL: pure glass, no tint
                GlassPanelBare(backdrop = backdrop, corner = 16.dp, isInteractive = true) {
                    // top line (percentage)
                    Text(
                        text = "$animated%",
                        color = textColorMain,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(6.dp))

                    // status under percent
                    Text(
                        text = statusText,
                        color = textColorSub,
                        fontSize = 17.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(16.dp))

                    // --- ERROR/SPINNER block (exact logic you asked) ---
                    if (showError) {
                        Text(
                            text = "Couldn’t load image",
                            color = Color.White.copy(alpha = 0.95f),
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        CircularProgressIndicator(color = Color.White.copy(alpha = 0.8f))
                    }

                    Spacer(Modifier.height(16.dp))

                    // Toggles: pure glass, no tint
                    LiquidRowButtonGlass(
                        label = "Wi-Fi",
                        enabled = wifiEnabled,
                        backdrop = backdrop,
                        isDark = isDark,
                        isInteractive = true,
                        onClick = onRequestEnableWifi
                    )

                    Spacer(Modifier.height(8.dp))

                    LiquidRowButtonGlass(
                        label = "Mobile data",
                        enabled = dataEnabled,
                        backdrop = backdrop,
                        isDark = isDark,
                        isInteractive = true,
                        onClick = onRequestEnableData
                    )
                }
            }
        }
    }
}

/* ---------- Preview ---------- */

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun CameraErrorOverlayPreview() {
    val backdrop = rememberLayerBackdrop()
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        CameraErrorOverlay(
            backdrop = backdrop,
            signalStrength = 10,
            isUserOffline = false,
            wifiEnabled = true,
            justRecovered = true,
            showError = true // flip to false to see the spinner
        )
    }
}
