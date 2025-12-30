package com.flights.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.luminance
import com.kyant.backdrop.effects.vibrancy

/* ---------- LIGHT ---------- */
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0A6DFF),
    onPrimary = Color.White,
    surface = Color(0xFFF4F5F7),
    onSurface = Color(0xFF101214),
    surfaceVariant = Color(0xFFE9EBEF),
    onSurfaceVariant = Color(0xFF4A4F57),
    outline = Color(0xFFD5D9E0),
    outlineVariant = Color(0xFFC9CED6)
)

/* ---------- DARK ---------- */
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF7DB4FF),
    onPrimary = Color(0xFF0B1220),
    surface = Color(0xFF0E1116),
    onSurface = Color(0xFFE6E9EF),
    surfaceVariant = Color(0xFF161A22),
    onSurfaceVariant = Color(0xFF9AA3B2),
    outline = Color(0xFF2A3040),
    outlineVariant = Color(0xFF343B4F)
)

/** Controls how the profile backdrop is drawn (NOT whether it shows). */
enum class ProfileBackdropStyle { Auto, Glass, Blur, Solid }

/** Theme-local setting */
val LocalProfileBackdropStyle = staticCompositionLocalOf { ProfileBackdropStyle.Auto }

/** Spec for the *background* that glass/blur will read */
@Immutable
data class ProfileBackdropSpec(
    val base: Color,
    val enabledFancyBackground: Boolean,
    val blob1: Color,
    val blob2: Color,
    val blob3: Color,
    val r1: Float,
    val r2: Float,
    val r3: Float,
    val c1: Offset,
    val c2: Offset,
    val c3: Offset,
)

val LocalProfileBackdropSpec = staticCompositionLocalOf {
    ProfileBackdropSpec(
        base = Color(0xFFE9EBEF),
        enabledFancyBackground = false,
        blob1 = Color.Transparent, blob2 = Color.Transparent, blob3 = Color.Transparent,
        r1 = 1200f, r2 = 1100f, r3 = 1300f,
        c1 = Offset(0.25f, 0.25f),
        c2 = Offset(0.85f, 0.20f),
        c3 = Offset(0.70f, 0.90f),
    )
}

@Composable
private fun rememberProfileBackdropSpec(
    darkTheme: Boolean,
    style: ProfileBackdropStyle,
    colors: ColorScheme,
): ProfileBackdropSpec {
    // Decide if we want fancy bg (so glass/blur looks different than solid)
    val enableFancy = when (style) {
        ProfileBackdropStyle.Solid -> false
        ProfileBackdropStyle.Glass, ProfileBackdropStyle.Blur -> true
        ProfileBackdropStyle.Auto -> true // Auto can still draw bg; you decide visibility elsewhere
    }

    val a1 = if (darkTheme) 0.30f else 0.22f
    val a2 = if (darkTheme) 0.22f else 0.18f
    val a3 = if (darkTheme) 0.18f else 0.14f

    return remember(darkTheme, style, colors.primary, colors.secondary, colors.tertiary, colors.surfaceVariant) {
        val base = if (style == ProfileBackdropStyle.Solid) colors.background else colors.surfaceVariant

        // ✅ On dark, push blob colors toward the base (darker), and reduce alpha
        val (b1, b2, b3) = if (darkTheme) {
            Triple(
                lerp(colors.primary, base, 0.40f).copy(alpha = 0.46f),
                lerp(colors.secondary, base, 0.45f).copy(alpha = 0.50f),
                lerp(colors.tertiary, base, 0.50f).copy(alpha = 0.16f),
            )
        } else {
            Triple(
                colors.primary.copy(alpha = a1),
                colors.secondary.copy(alpha = a2),
                colors.tertiary.copy(alpha = a3),
            )
        }


        ProfileBackdropSpec(
            base = base,
            enabledFancyBackground = enableFancy,
            blob1 = b1,
            blob2 = b2,
            blob3 = b3,

            r1 = 520f,
            r2 = 620f,
            r3 = 760f,

            c1 = Offset(0.50f, 0.18f),
            c2 = Offset(0.85f, 0.45f),
            c3 = Offset(0.30f, 0.88f),
        )
    }
}


@Composable
fun Modifier.profileGlassBackdrop(
    backdrop: com.kyant.backdrop.backdrops.LayerBackdrop,
    shape: RoundedCornerShape,
    enabled: Boolean = true
): Modifier {
    if (!enabled) return this

    val spec = LocalProfileGlassSpec.current
    val wash = rememberProfileSurfaceWashColor()

    return this
        .clip(shape)
        .drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                if (spec.vibrancy) vibrancy()
                if (spec.blur.value > 0f) blur(spec.blur.toPx())
                if (spec.lensOuter.value > 0f) lens(spec.lensInner.toPx(), spec.lensOuter.toPx())
            },
            onDrawSurface = { drawRect(wash) } // ✅ readability like Kyant tutorial
        )
}


@Composable
fun FlightsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    profileBackdropStyle: ProfileBackdropStyle = ProfileBackdropStyle.Auto,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColorScheme else LightColorScheme

    val backdropSpec =
        rememberProfileBackdropSpec(darkTheme, profileBackdropStyle, colors)

    // ✅ ADD THIS
    val glassSpec =
        rememberProfileGlassSpec(darkTheme, profileBackdropStyle)

    MaterialTheme(
        colorScheme = colors,
        typography = Typography()
    ) {
        CompositionLocalProvider(
            LocalProfileBackdropStyle provides profileBackdropStyle,
            LocalProfileBackdropSpec provides backdropSpec,

            // ✅ THIS is why the functions were "never used"
            LocalProfileGlassSpec provides glassSpec
        ) {
            content()
        }
    }
}


/**
 * Theme-backed background that gives Glass/Blur something to refract/blur.
 * - Solid style => flat base
 * - Glass/Blur/Auto => base + blobs
 */
@Composable
fun Modifier.profileBackdropBackground(shape: RoundedCornerShape): Modifier {
    val spec = LocalProfileBackdropSpec.current

    // Always clip to page shape
    val m = this.clip(shape)

    // If fancy bg disabled (Solid), just paint base and stop
    if (!spec.enabledFancyBackground) {
        return m.background(spec.base)
    }

    return m.drawBehind {
        val w = size.width
        val h = size.height

        fun p(n: Offset) = Offset(n.x * w, n.y * h)

        val cutoff = h * 0.5f
        val fadeHeight = h * 0.08f // tweak 0.06–0.12

        // 1) Blobs only above cutoff
        clipRect(left = 0f, top = 0f, right = w, bottom = cutoff) {
            drawCircle(color = spec.blob1, radius = spec.r1, center = p(spec.c1))
            drawCircle(color = spec.blob2, radius = spec.r2, center = p(spec.c2))
            drawCircle(color = spec.blob3, radius = spec.r3, center = p(spec.c3))
        }

        // 2) Inverted gradient: solid at cutoff → fades UPWARD
        val isDark = spec.base.luminance() < 0.5f
        val endColor = if (isDark) {
            Color(0xFF0E1116)          // dark theme
        } else {
            Color.White               // light theme
        }

        clipRect(
            left = 0f,
            top = cutoff - fadeHeight,
            right = w,
            bottom = cutoff
        ) {
            drawRect(
                brush = Brush.verticalGradient(
                    0.0f to Color.Transparent, // top
                    1.0f to endColor,          // solid at cutoff
                    startY = cutoff - fadeHeight,
                    endY = cutoff
                )
            )
        }
    }



}