package com.flights.studio

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.shadow.Shadow
import kotlin.math.abs
import kotlin.math.sqrt

internal val GlassChromeHorizontalPadding = 10.dp
internal val GlassChromeCornerRadius = 27.dp
internal val GlassChromeInnerCornerRadius = 23.dp
internal val GlassChromeShadowElevation = 10.dp
internal val GlassChromeShape = RoundedCornerShape(GlassChromeCornerRadius)
internal val GlassChromeInnerShape = RoundedCornerShape(GlassChromeInnerCornerRadius)

internal const val GlassChromeBackdropBlurDp = 1.35f
internal const val GlassChromeNativeBlurPx = 7.5f
internal const val GlassChromeRefractionHeightDp = 12f
internal const val GlassChromeRefractionAmountDp = 42f
internal const val GlassChromeNativeRefractionIntensity = 0.38f
internal const val TopActionBarBlurDp = 8f
internal const val TopActionBarNativeBlurPx = 8f
internal const val TopActionBarSaturation = 1.08f

@Composable
internal fun glassChromeTint(): Color {
    return if (isSystemInDarkTheme()) {
        Color(0xFF17191D).copy(alpha = 0.40f)
    } else {
        Color(0xFFF7F7F8).copy(alpha = 0.48f)
    }
}

@Composable
internal fun glassChromeOverlayTint(): Color {
    return if (isSystemInDarkTheme()) {
        Color.White.copy(alpha = 0.07f)
    } else {
        Color.Black.copy(alpha = 0.035f)
    }
}

@Composable
internal fun bottomTabBarTint(): Color {
    val amount = rememberLiquidGlassTintAmount()
    return bottomTabBarTintForAmount(amount, isSystemInDarkTheme())
}

internal fun bottomTabBarTintForAmount(amount: Float, isDarkTheme: Boolean): Color {
    return if (isDarkTheme) {
        Color(0xFF101216).copy(alpha = mix(0.32f, 0.98f, amount))
    } else {
        Color.White.copy(alpha = mix(0.34f, 0.98f, amount))
    }
}

@Composable
internal fun bottomTabBarOverlayTint(): Color {
    val amount = rememberLiquidGlassTintAmount()
    return bottomTabBarOverlayTintForAmount(amount, isSystemInDarkTheme())
}

internal fun bottomTabBarOverlayTintForAmount(amount: Float, isDarkTheme: Boolean): Color {
    return if (isDarkTheme) {
        Color.Black.copy(alpha = mix(0.03f, 0.18f, amount))
    } else {
        Color(0xFF111827).copy(alpha = mix(0.015f, 0.12f, amount))
    }
}

@Composable
internal fun bottomTabInactiveColor(): Color {
    val amount = rememberLiquidGlassTintAmount()
    return bottomTabInactiveColorForAmount(amount, isSystemInDarkTheme())
}

internal fun bottomTabInactiveColorForAmount(amount: Float, isDarkTheme: Boolean): Color {
    return if (isDarkTheme) {
        Color.White.copy(alpha = mix(0.68f, 0.82f, amount))
    } else {
        lerp(Color(0xFF4D5360), Color(0xFF1F2530), amount)
    }
}

@Composable
internal fun bottomTabSelectedPillColor(): Color {
    val amount = rememberLiquidGlassTintAmount()
    return bottomTabSelectedPillColorForAmount(amount, isSystemInDarkTheme(), primaryTabAccentColor())
}

internal fun bottomTabSelectedPillColorForAmount(
    amount: Float,
    isDarkTheme: Boolean,
    primary: Color
): Color {
    return if (isDarkTheme) {
        lerp(Color(0xFF242A33), Color(0xFF343B46), amount)
            .copy(alpha = mix(0.80f, 0.90f, amount))
    } else {
        bottomTabBarTintForAmount(amount, isDarkTheme = true)
            .copy(alpha = mix(0.58f, 0.84f, amount))
    }
}

@Composable
internal fun bottomTabSelectedContentColor(): Color {
    val amount = rememberLiquidGlassTintAmount()
    return bottomTabSelectedContentColorForAmount(amount, isSystemInDarkTheme(), primaryTabAccentColor())
}

internal fun bottomTabSelectedContentColorForAmount(
    amount: Float,
    isDarkTheme: Boolean,
    primary: Color
): Color {
    return if (isDarkTheme) {
        lerp(primary, Color.White, amount * 0.18f)
    } else {
        lerp(Color.White, Color(0xFFEAF3FF), amount * 0.35f)
    }
}

@Composable
internal fun bottomChromeBackdropBlurDp(): Float {
    val blurAmount = rememberLiquidGlassBlurAmount()
    return liquidGlassBlurRadiusDpForAmount(blurAmount)
}

internal fun bottomChromeBackdropBlurDpForAmount(amount: Float, isDarkTheme: Boolean): Float {
    return if (isDarkTheme) {
        mix(0f, GlassChromeBackdropBlurDp, amount)
    } else {
        mix(0f, 0.95f, amount)
    }
}

@Composable
internal fun bottomChromeNativeBlurPx(): Float {
    val blurAmount = rememberLiquidGlassBlurAmount()
    return liquidGlassNativeBlurPxForAmount(blurAmount)
}

internal fun bottomChromeNativeBlurPxForAmount(amount: Float, isDarkTheme: Boolean): Float {
    return if (isDarkTheme) {
        mix(0f, GlassChromeNativeBlurPx, amount)
    } else {
        mix(0f, 5.5f, amount)
    }
}

internal fun adaptiveLuminanceOffset(luminance: Float): Float {
    val centered = luminance.coerceIn(0f, 1f) * 2f - 1f
    val eased = sqrt(abs(centered))
    return if (centered < 0f) -eased else eased
}

internal fun adaptiveLuminanceEffectStrength(tintAmount: Float): Float {
    val amount = tintAmount.coerceIn(0f, 1f)
    return sqrt(amount)
}

internal fun adaptiveSurfaceTint(
    luminance: Float,
    strength: Float
): Color {
    if (strength <= 0f) return Color.Transparent
    val normalized = luminance.coerceIn(0f, 1f)
    val contrastDistance = abs(normalized - 0.5f) * 2f
    val alpha = mix(0.10f, 0.30f, contrastDistance) * strength.coerceIn(0f, 1f)
    return if (normalized > 0.5f) {
        Color.Black.copy(alpha = alpha)
    } else {
        Color.White.copy(alpha = alpha)
    }
}

internal fun liquidGlassBlurRadiusDpForAmount(amount: Float): Float {
    return mix(0f, 10f, amount)
}

internal fun liquidGlassNativeBlurPxForAmount(amount: Float): Float {
    return mix(0f, 30f, amount)
}

internal fun adaptiveLuminanceBrightness(offset: Float): Float {
    val l = offset.coerceIn(-1f, 1f)
    return if (l > 0f) {
        mix(0.1f, 0.5f, l)
    } else {
        mix(0.1f, -0.2f, -l)
    }
}

internal fun adaptiveLuminanceContrast(offset: Float): Float {
    val l = offset.coerceIn(-1f, 1f)
    return if (l > 0f) {
        mix(1f, 0f, l)
    } else {
        1f
    }
}

internal fun adaptiveLuminanceBlurPx(offset: Float, baseBlurPx: Float, dpToPx: (Float) -> Float): Float {
    val l = offset.coerceIn(-1f, 1f)
    val kyantBase = maxOf(baseBlurPx, dpToPx(8f))
    return if (l > 0f) {
        mix(kyantBase, dpToPx(16f), l)
    } else {
        mix(kyantBase, dpToPx(2f), -l)
    }
}

@Composable
internal fun rememberLiquidGlassTintAmount(): Float {
    val context = LocalContext.current.applicationContext
    var amount by remember(context) {
        mutableStateOf(SettingsStore.liquidGlassTint(context))
    }

    DisposableEffect(context) {
        val prefs = SettingsStore.prefs(context)
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == SettingsStore.KEY_LIQUID_GLASS_TINT) {
                amount = SettingsStore.liquidGlassTint(context)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    return amount
}

@Composable
internal fun rememberLiquidGlassBlurAmount(): Float {
    val context = LocalContext.current.applicationContext
    var amount by remember(context) {
        mutableStateOf(SettingsStore.liquidGlassBlur(context))
    }

    DisposableEffect(context) {
        val prefs = SettingsStore.prefs(context)
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == SettingsStore.KEY_LIQUID_GLASS_BLUR) {
                amount = SettingsStore.liquidGlassBlur(context)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    return amount
}

@Composable
internal fun rememberLiquidGlassAdaptiveLuminanceEnabled(): Boolean {
    return false
}

@Composable
internal fun topActionBarTint(): Color {
    return if (isSystemInDarkTheme()) {
        Color(0xFF151617).copy(alpha = 0.76f)
    } else {
        Color(0xFFF7F7F8).copy(alpha = 0.76f)
    }
}

@Composable
internal fun primaryTabAccentColor(): Color {
    return MaterialTheme.colorScheme.primary
}

internal fun bottomChromeShadow(): Shadow {
    return Shadow(alpha = 0.03f)
}

private fun mix(start: Float, stop: Float, fraction: Float): Float {
    val t = fraction.coerceIn(0f, 1f)
    return start + (stop - start) * t
}
