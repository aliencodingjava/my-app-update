package com.flights.studio

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import kotlin.math.exp

fun Dp.us(s: Float): Dp = this * s
fun TextUnit.us(s: Float): TextUnit = this * s

@Stable
data class UiScales(
    val body: Float,
    val label: Float
)

@Composable
fun rememberUiScales(debugLog: Boolean = false): UiScales {
    val cfg = LocalConfiguration.current
    val sw = cfg.smallestScreenWidthDp
    val fontScale = cfg.fontScale

    return remember(sw, fontScale) {
        val base = uiScaleForWidth(swDp = sw)
        val comp = (base / lerp(1f, fontScale, 0.70f)).coerceIn(0.82f, 1.06f)

        val tight = when {
            sw >= 600 -> 1.00f
            sw <= 360 -> 0.98f
            sw <= 420 -> 0.95f
            sw <= 520 -> 0.97f
            else -> 1.00f
        }


// iOS-style: button labels should NOT grow with accessibility fontScale
        val body = (comp * tight).coerceIn(0.72f, 1.06f)
        val label = (base * tight).coerceIn(0.70f, 1.00f)



        if (debugLog) {
            Log.d("SCALES", "sw=$sw fontScale=$fontScale body=$body label=$label")
        }

        UiScales(body = body, label = label)
    }
}

@Composable
fun rememberUiScale(debugLog: Boolean = false): Float =
    rememberUiScales(debugLog).body

@Composable
fun rememberUiTight(debugLog: Boolean = false): Float =
    rememberUiScales(debugLog).label

fun uiScaleForWidth(swDp: Int): Float {
    val sw = swDp.toFloat()

    val smallPhoneBase = when {
        sw <= 320f -> 0.92f
        sw <= 360f -> lerp(0.92f, 0.98f, (sw - 320f) / 40f)
        sw <= 411f -> lerp(0.98f, 1.00f, (sw - 360f) / 51f)
        else -> 1.00f
    }

    val bigPhoneDip = bigPhoneGaussianDip(sw)

    val tabletBoost = when {
        sw < 600f -> 0f
        sw < 840f -> lerp(0f, 0.10f, (sw - 600f) / 240f)
        else -> 0.10f
    }

    return ((smallPhoneBase - bigPhoneDip) * (1f + tabletBoost))
        .coerceIn(0.82f, 1.10f)
}

private fun lerp(a: Float, b: Float, t: Float): Float {
    val tt = t.coerceIn(0f, 1f)
    return a + (b - a) * tt
}

private const val BIG_PHONE_CENTER_DP = 390f
private const val BIG_PHONE_WIDTH_DP = 110f
private const val BIG_PHONE_DIP_AMOUNT = 0.08f

private fun bigPhoneGaussianDip(x: Float): Float {
    val sigma = BIG_PHONE_WIDTH_DP / 2f
    val v = exp(-((x - BIG_PHONE_CENTER_DP) * (x - BIG_PHONE_CENTER_DP)) / (2f * sigma * sigma))
    return BIG_PHONE_DIP_AMOUNT * v
}
