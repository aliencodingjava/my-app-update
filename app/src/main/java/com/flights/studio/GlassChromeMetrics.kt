package com.flights.studio

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.shadow.Shadow

internal val GlassChromeHorizontalPadding = 18.dp
internal val GlassChromeCornerRadius = 27.dp
internal val GlassChromeInnerCornerRadius = 23.dp
internal val GlassChromeShadowElevation = 10.dp
internal val GlassChromeShape = RoundedCornerShape(GlassChromeCornerRadius)
internal val GlassChromeInnerShape = RoundedCornerShape(GlassChromeInnerCornerRadius)

internal const val GlassChromeBackdropBlurDp = 2f
internal const val GlassChromeNativeBlurPx = 10f
internal const val GlassChromeRefractionHeightDp = 12f
internal const val GlassChromeRefractionAmountDp = 42f
internal const val GlassChromeNativeRefractionIntensity = 0.46f
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
    return if (isSystemInDarkTheme()) {
        Color(0xFF20242C).copy(alpha = 0.66f)
    } else {
        Color(0xFFFFFFFF).copy(alpha = 0.82f)
    }
}

@Composable
internal fun bottomTabBarOverlayTint(): Color {
    val primary = primaryTabAccentColor()
    return if (isSystemInDarkTheme()) {
        primary.copy(alpha = 0.08f)
    } else {
        Color(0xFF111827).copy(alpha = 0.045f)
    }
}

@Composable
internal fun bottomTabInactiveColor(): Color {
    return if (isSystemInDarkTheme()) {
        Color.White.copy(alpha = 0.70f)
    } else {
        Color(0xFF4D5360)
    }
}

@Composable
internal fun bottomTabSelectedPillColor(): Color {
    return primaryTabAccentColor().copy(alpha = if (isSystemInDarkTheme()) 0.24f else 0.15f)
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
