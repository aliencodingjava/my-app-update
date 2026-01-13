package com.flights.studio

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.TileMode

/** Controls *look* of the glass/blur effects (theme-level). */
@Stable
data class ProfileGlassSpec(
    val blur: Dp,
    val  blurEdge: TileMode,
    val lensInner: Dp,
    val lensOuter: Dp,
    val surfaceAlpha: Float,
    val vibrancy: Boolean,
    // Kyant lens params (for effects.lens(...))
    val refractionAmount: Float,
    val refractionHeight: Float,
    val depthEffect: Boolean,
    val chromaticAberration: Boolean,
    // color controls
    val brightness: Float,
    val contrast: Float,
    val saturation: Float,
)

val LocalProfileGlassSpec = staticCompositionLocalOf {
    ProfileGlassSpec(
        blur = 4.dp,
        blurEdge = TileMode.Clamp,
        lensInner = 16.dp,
        lensOuter = 32.dp,
        surfaceAlpha = 0.50f,
        vibrancy = true,
        refractionAmount = 0.20f,
        refractionHeight = 0.15f,
        depthEffect = true,
        chromaticAberration = false,
        brightness = 0f,
        contrast = 1f,
        saturation = 1f,
    )
}

@Composable
fun rememberProfileGlassSpec(
    darkTheme: Boolean,
    style: ProfileBackdropStyle
): ProfileGlassSpec {

    val off = ProfileGlassSpec(
        blur = 0.dp,
        blurEdge = TileMode.Clamp,
        lensInner = 0.dp,
        lensOuter = 0.dp,
        surfaceAlpha = 0f,
        vibrancy = false,
        refractionAmount = 0f,
        refractionHeight = 0f,
        depthEffect = false,
        chromaticAberration = false,
        brightness = 0f,
        contrast = 1f,
        saturation = 1f,
    )

    return remember(darkTheme, style) {
        when (style) {
            ProfileBackdropStyle.Solid,
            ProfileBackdropStyle.Amoled -> off

            ProfileBackdropStyle.Auto -> ProfileGlassSpec(
                blur = 4.dp,
                blurEdge = TileMode.Mirror,
                lensInner = 10.dp,
                lensOuter = 22.dp,
                surfaceAlpha = if (darkTheme) 0.30f else 0.52f,
                vibrancy = true,
                refractionAmount = 0.12f,
                refractionHeight = 0.10f,
                depthEffect = true,
                chromaticAberration = false,
                brightness = if (darkTheme) 0.00f else 0.01f,
                contrast = 1.10f,
                saturation = 1.15f,
            )

            // Glass (you had it off)
            ProfileBackdropStyle.Glass -> off.copy(
                // if you actually want classic glass, set lens/vibrancy here
                // blur = 0.dp,
                // lensInner = 10.dp,
                // lensOuter = 22.dp,
                // surfaceAlpha = if (darkTheme) 0.18f else 0.30f,
                // vibrancy = true,
            )

            ProfileBackdropStyle.ClearGlass -> ProfileGlassSpec(
                blur = 0.dp,
                blurEdge = TileMode.Clamp,
                lensInner = 0.dp,
                lensOuter = 33.dp,
                surfaceAlpha = if (darkTheme) 0.10f else 0.18f,
                vibrancy = true,
                refractionAmount = 32f,
                refractionHeight = 10f,
                depthEffect = true,
                chromaticAberration = true, // sharp + slight CA looks nice
                brightness = if (darkTheme) 0.00f else 0.01f,
                contrast = 1.15f,
                saturation = 1.02f,
            )

            // ✅ your VibrantGlass EXACT request
            ProfileBackdropStyle.VibrantGlass -> ProfileGlassSpec(
                blur = 0.dp,
                blurEdge = TileMode.Mirror,
                lensInner = 20.dp,
                lensOuter = 40.dp,
                surfaceAlpha = if (darkTheme) 0.22f else 0.40f,
                vibrancy = true,
                refractionAmount = 25f,
                refractionHeight = 36f,
                depthEffect = true,
                chromaticAberration = true,
                brightness = if (darkTheme) 0.04f else 0.01f,
                contrast = 1.15f,
                saturation = 2.0f,
            )

            ProfileBackdropStyle.Blur -> ProfileGlassSpec(
                blur = if (darkTheme) 14.dp else 12.dp,
                blurEdge = TileMode.Mirror,
                lensInner = 0.dp,
                lensOuter = 0.dp,
                surfaceAlpha = if (darkTheme) 0.50f else 0.62f,
                vibrancy = false,
                refractionAmount = 0f,
                refractionHeight = 0f,
                depthEffect = false,
                chromaticAberration = false,
                brightness = 0f,
                contrast = 1f,
                saturation = 1f,
            )

            ProfileBackdropStyle.Frosted -> ProfileGlassSpec(
                blur = if (darkTheme) 18.dp else 16.dp,
                blurEdge = TileMode.Mirror,
                lensInner = 0.dp,
                lensOuter = 0.dp,
                surfaceAlpha = if (darkTheme) 0.74f else 0.80f,
                vibrancy = false,
                refractionAmount = 0f,
                refractionHeight = 0f,
                depthEffect = false,
                chromaticAberration = false,
                brightness = if (darkTheme) 0.08f else 0.01f,
                contrast = 1f,
                saturation = 1f,
            )
        }
    }
}




/** Theme wash for readability (no hardcoded white). */
@Composable
fun rememberProfileSurfaceWashColor(): Color {
    val cs = MaterialTheme.colorScheme
    val spec = LocalProfileGlassSpec.current
    val isDark = isSystemInDarkTheme()

    // Use surfaceVariant so the wash matches your card color, not the page black.
    val base = if (isDark) cs.surfaceVariant else cs.surface

    // Optional: clamp alpha in dark so it never becomes "black fog"
    val a = if (isDark) spec.surfaceAlpha.coerceIn(0f, 0.38f) else spec.surfaceAlpha

    return base.copy(alpha = a)
}
