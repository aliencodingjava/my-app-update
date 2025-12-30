package com.flights.studio

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Controls *look* of the glass/blur effects (theme-level). */
@Stable
data class ProfileGlassSpec(
    val blur: Dp,
    val lensInner: Dp,
    val lensOuter: Dp,
    val surfaceAlpha: Float,
    val vibrancy: Boolean,
    val refractionAmount: Float,
    val refractionHeight: Float,
)

val LocalProfileGlassSpec = staticCompositionLocalOf {
    ProfileGlassSpec(
        blur = 4.dp,
        lensInner = 16.dp,
        lensOuter = 32.dp,
        surfaceAlpha = 0.50f,
        vibrancy = true,
        refractionAmount = 0.20f,
        refractionHeight = 0.15f,
    )
}

@Composable
fun rememberProfileGlassSpec(
    darkTheme: Boolean,
    style: ProfileBackdropStyle
): ProfileGlassSpec {
    return remember(darkTheme, style) {
        when (style) {
            ProfileBackdropStyle.Glass -> ProfileGlassSpec(
                blur = 1.dp,
                lensInner = 18.dp,
                lensOuter = 40.dp,
                surfaceAlpha = if (darkTheme) 0.26f else 0.44f,
                vibrancy = true,
                refractionAmount = 0.20f,
                refractionHeight = 0.15f,
            )

            ProfileBackdropStyle.Auto -> ProfileGlassSpec(
                // ✅ Softer than Glass (so you can see difference)
                blur = 4.dp,
                lensInner = 10.dp,
                lensOuter = 22.dp,
                surfaceAlpha = if (darkTheme) 0.30f else 0.52f,
                vibrancy = true,
                refractionAmount = 0.12f,
                refractionHeight = 0.10f,
            )

            ProfileBackdropStyle.Blur -> ProfileGlassSpec(
                blur = if (darkTheme) 14.dp else 12.dp,
                lensInner = 0.dp,
                lensOuter = 0.dp,
                surfaceAlpha = if (darkTheme) 0.50f else 0.62f,
                vibrancy = false,
                refractionAmount = 0f,
                refractionHeight = 0f,
            )

            ProfileBackdropStyle.Solid -> ProfileGlassSpec(
                blur = 0.dp,
                lensInner = 0.dp,
                lensOuter = 0.dp,
                surfaceAlpha = 0f,
                vibrancy = false,
                refractionAmount = 0f,
                refractionHeight = 0f,
            )
        }
    }
}


/** Theme wash for readability (no hardcoded white). */
@Composable
fun rememberProfileSurfaceWashColor(): Color {
    val cs = MaterialTheme.colorScheme
    return cs.surface.copy(alpha = LocalProfileGlassSpec.current.surfaceAlpha)
}
