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

            // AUTO = your default "nice" profile look (balanced)
            ProfileBackdropStyle.Auto -> ProfileGlassSpec(
                blur = if (darkTheme) 14.dp else 20.dp,
                blurEdge = TileMode.Mirror,
                lensInner = if (darkTheme) 10.dp else 14.dp,
                lensOuter = if (darkTheme) 26.dp else 34.dp,
                surfaceAlpha = if (darkTheme) 0.18f else 0.08f,
                vibrancy = true,
                refractionAmount = if (darkTheme) 0.08f else 0.10f,
                refractionHeight = 0.18f,
                depthEffect = true,
                chromaticAberration = false,
                brightness = if (darkTheme) 0.10f else 0.03f,
                contrast = 1.05f,
                saturation = 1.08f,
            )

            // GLASS = your main expressive Kyant-like glass
            ProfileBackdropStyle.Glass -> ProfileGlassSpec(
                blur = if (darkTheme) 2.dp else 2.dp,
                blurEdge = TileMode.Mirror,
                lensInner = if (darkTheme) 10.dp else 14.dp,
                lensOuter = if (darkTheme) 28.dp else 36.dp,
                surfaceAlpha = if (darkTheme) 0.20f else 0.10f,
                vibrancy = true,
                refractionAmount = if (darkTheme) 0.09f else 0.12f,
                refractionHeight = 0.22f,
                depthEffect = true,
                chromaticAberration = false,
                brightness = if (darkTheme) 0.12f else 0.04f,
                contrast = 1.07f,
                saturation = 1.10f,
            )

            // CLEAR GLASS = sharper, cleaner, minimal blur, still readable
            ProfileBackdropStyle.ClearGlass -> ProfileGlassSpec(
                blur = if (darkTheme) 0.dp else 0.dp,
                blurEdge = TileMode.Clamp,
                lensInner = 0.dp,
                lensOuter = if (darkTheme) 26.dp else 32.dp,
                surfaceAlpha = if (darkTheme) 0.12f else 0.05f,
                vibrancy = true,
                refractionAmount = if (darkTheme) 0.05f else 0.07f,
                refractionHeight = 0.14f,
                depthEffect = true,
                chromaticAberration = false, // keep clean; CA only if you REALLY want “bling”
                brightness = if (darkTheme) 0.08f else 0.02f,
                contrast = 1.23f,
                saturation = 1.05f,
            )

            // VIBRANT GLASS = more punch (but still sane values)
            ProfileBackdropStyle.VibrantGlass -> ProfileGlassSpec(
                blur = if (darkTheme) 10.dp else 14.dp,
                blurEdge = TileMode.Mirror,
                lensInner = if (darkTheme) 14.dp else 18.dp,
                lensOuter = if (darkTheme) 36.dp else 44.dp,
                surfaceAlpha = if (darkTheme) 0.18f else 0.10f,
                vibrancy = true,
                refractionAmount = if (darkTheme) 0.12f else 0.16f,
                refractionHeight = 0.26f,
                depthEffect = true,
                chromaticAberration = true, // ok here
                brightness = if (darkTheme) 0.12f else 0.03f,
                contrast = 1.10f,
                saturation = if (darkTheme) 1.90f else 1.15f,
            )

            // BLUR = utility blur strip / background soften, no lens/refraction
            ProfileBackdropStyle.Blur -> ProfileGlassSpec(
                blur = if (darkTheme) 20.dp else 28.dp,
                blurEdge = TileMode.Clamp,
                lensInner = 0.dp,
                lensOuter = 0.dp,
                surfaceAlpha = if (darkTheme) 0.18f else 0.06f,
                vibrancy = false,
                refractionAmount = 0f,
                refractionHeight = 0f,
                depthEffect = false,
                chromaticAberration = false,
                brightness = 0f,
                contrast = 1f,
                saturation = 1f,
            )

            // FROSTED = soft matte blur, safe on light theme (no haze)
            ProfileBackdropStyle.Frosted -> ProfileGlassSpec(
                blur = if (darkTheme) 12.dp else 16.dp,
                blurEdge = TileMode.Mirror,
                lensInner = 0.dp,
                lensOuter = 0.dp,
                surfaceAlpha = if (darkTheme) 0.24f else 0.04f, // 👈 don’t use 0 on light or it looks “missing”
                vibrancy = false,
                refractionAmount = 0f,
                refractionHeight = 0f,
                depthEffect = true,
                chromaticAberration = false,
                brightness = if (darkTheme) 0.18f else 0.00f,
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
