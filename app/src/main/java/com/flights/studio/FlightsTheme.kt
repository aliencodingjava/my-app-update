package com.flights.studio

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
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
    val blob4: Color,
    val blob5: Color,
    val blob6: Color,
    val blob7: Color,

    val r1: Float,
    val r2: Float,
    val r3: Float,
    val r4: Float,
    val r5: Float,
    val r6: Float,
    val r7: Float,

    val c1: Offset,
    val c2: Offset,
    val c3: Offset,
    val c4: Offset,
    val c5: Offset,
    val c6: Offset,
    val c7: Offset,
)


val LocalProfileBackdropSpec = staticCompositionLocalOf {
    ProfileBackdropSpec(
        base = Color(0xFFE9EBEF),
        enabledFancyBackground = false,

        blob1 = Color.Transparent,
        blob2 = Color.Transparent,
        blob3 = Color.Transparent,
        blob4 = Color.Transparent,
        blob5 = Color.Transparent,
        blob6 = Color.Transparent,
        blob7 = Color.Transparent,

        r1 = 1200f, r2 = 1100f, r3 = 1300f, r4 = 900f, r5 = 700f,
        r6 = 700f,
        r7 = 560f,

        c1 = Offset(0.25f, 0.25f),
        c2 = Offset(0.85f, 0.20f),
        c3 = Offset(0.70f, 0.90f),
        c4 = Offset(0.18f, 0.14f),
        c5 = Offset(0.92f, 0.16f),
        c6 = Offset(0.12f, 0.28f),
        c7 = Offset(0.18f, 0.40f),
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
        ProfileBackdropStyle.Auto -> true
    }

    // ─────────────────────────────────────────────
    // SCHEME: edit ONLY these numbers to tune blobs
    // ─────────────────────────────────────────────
    data class BlobScheme(
        val lightA1: Float, val lightA2: Float, val lightA3: Float, val lightA4: Float, val lightA5: Float,
        val lightA6: Float, val lightA7: Float,

        val darkMix1: Float, val darkMix2: Float, val darkMix3: Float, val darkMix4: Float, val darkMix5: Float,
        val darkMix6: Float, val darkMix7: Float,

        val darkA1: Float, val darkA2: Float, val darkA3: Float, val darkA4: Float, val darkA5: Float,
        val darkA6: Float, val darkA7: Float,
    )

    val scheme = if (darkTheme) {
        BlobScheme(
            // ── Light alphas (unused in dark)
            lightA1 = 0.22f,
            lightA2 = 0.18f,
            lightA3 = 0.14f,
            lightA4 = 0.12f,
            lightA5 = 0.10f,
            lightA6 = 0.22f,   // ✅ new
            lightA7 = 0.18f,   // ✅ new

            // ── Dark mix (LOWER = MUCH brighter)
            darkMix1 = 0.18f,
            darkMix2 = 0.22f,
            darkMix3 = 0.26f,
            darkMix4 = 0.24f,
            darkMix5 = 0.22f,
            darkMix6 = 0.20f,  // ✅ new
            darkMix7 = 0.22f,  // ✅ new

            // ── Dark alpha (glow visibility)
            darkA1 = 0.68f,
            darkA2 = 0.70f,
            darkA3 = 0.30f,
            darkA4 = 0.34f,
            darkA5 = 0.32f,
            darkA6 = 0.42f,    // ✅ new (kept softer)
            darkA7 = 0.38f,    // ✅ new
        )
    } else {
        BlobScheme(
            lightA1 = 0.22f,
            lightA2 = 0.18f,
            lightA3 = 0.14f,
            lightA4 = 0.12f,
            lightA5 = 0.10f,
            lightA6 = 0.22f,   // ✅ new
            lightA7 = 0.18f,   // ✅ new

            darkMix1 = 0.40f,
            darkMix2 = 0.45f,
            darkMix3 = 0.50f,
            darkMix4 = 0.42f,
            darkMix5 = 0.38f,
            darkMix6 = 0.40f,  // ✅ new
            darkMix7 = 0.42f,  // ✅ new

            darkA1 = 0.46f,
            darkA2 = 0.50f,
            darkA3 = 0.16f,
            darkA4 = 0.20f,
            darkA5 = 0.18f,
            darkA6 = 0.20f,    // ✅ new
            darkA7 = 0.18f,    // ✅ new
        )
    }


    return remember(darkTheme, style, colors.primary, colors.secondary, colors.tertiary, colors.surfaceVariant, colors.background) {
        val base = if (style == ProfileBackdropStyle.Solid) colors.background else colors.surfaceVariant

        val (b1, b2, b3, b4, b5, b6, b7) = if (darkTheme) {
            val glow = 0.22f
            val x1 = lerp(lerp(Color(0xFF1E3A8A), base, scheme.darkMix1), Color.White, glow).copy(alpha = scheme.darkA1) // Earth shadow
            val x2 = lerp(lerp(Color(0xFF4FC3FF), base, scheme.darkMix2), Color.White, glow).copy(alpha = scheme.darkA2) // Atmosphere cyan
            val x3 = lerp(lerp(Color(0xFF3A7BFF), base, scheme.darkMix3), Color.White, glow).copy(alpha = scheme.darkA3) // 🌍 Planet Earth
            val x4 = lerp(lerp(Color(0xFF6A5CFF), base, scheme.darkMix4), Color.White, glow).copy(alpha = scheme.darkA4) // 🪐 Neptune / cosmic
            val x5 = lerp(lerp(Color(0xFFFF8A4C), base, scheme.darkMix5), Color.White, glow).copy(alpha = scheme.darkA5) // 🔴 Mars
            val x6 = lerp(lerp(Color(0xFF2DD4BF), base, scheme.darkMix6), Color.White, glow).copy(alpha = scheme.darkA6) // 🧊 Uranus / ice giant
            val x7 = lerp(lerp(Color(0xFF2EFF9A), base, scheme.darkMix7), Color.White, glow * 0.6f).copy(alpha = scheme.darkA7) // 🟢 Alien life
            Septuple(x1, x2, x3, x4, x5, x6, x7)
        } else {
            Septuple(
                Color(0xFF1E3A8A).copy(alpha = scheme.lightA1), // Earth shadow
                Color(0xFF4FC3FF).copy(alpha = scheme.lightA2), // Atmosphere cyan
                Color(0xFF3A7BFF).copy(alpha = scheme.lightA3), // 🌍 Planet Earth
                Color(0xFF6A5CFF).copy(alpha = scheme.lightA4), // 🪐 Neptune / cosmic
                Color(0xFFFF8A4C).copy(alpha = scheme.lightA5), // 🔴 Mars
                Color(0xFF2DD4BF).copy(alpha = scheme.lightA6), // 🧊 Uranus / ice giant
                Color(0xFF2EFF9A).copy(alpha = scheme.lightA7)  // 🟢 Alien life
            )
        }




        ProfileBackdropSpec(
            base = base,
            enabledFancyBackground = enableFancy,

            blob1 = b1,
            blob2 = b2,
            blob3 = b3,
            blob4 = b4,
            blob5 = b5,
            blob6 = b6,
            blob7 = b7,


            r1 = 520f,
            r2 = 620f,
            r3 = 760f,
            r4 = 460f,
            r5 = 360f,
            r6 = 700f,
            r7 = 560f,

            c1 = Offset(0.50f, 0.18f),
            c2 = Offset(0.85f, 0.35f),
            c3 = Offset(0.30f, 0.88f),
            c4 = Offset(0.18f, 0.14f),
            c5 = Offset(0.92f, 0.16f),
            c6 = Offset(0.12f, 0.28f),
            c7 = Offset(0.68f, 0.30f),
        )
    }
}

private data class Septuple<A, B, C, D, E, F, G>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
    val sixth: F,
    val seventh: G
)

@Composable
fun Modifier.profileGlassBackdrop(
    backdrop: LayerBackdrop,
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
            onDrawSurface = { drawRect(wash) }
        )
}

@Composable
fun FlightsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    profileBackdropStyle: ProfileBackdropStyle = ProfileBackdropStyle.Auto,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColorScheme else LightColorScheme

    val backdropSpec = rememberProfileBackdropSpec(darkTheme, profileBackdropStyle, colors)
    val glassSpec = rememberProfileGlassSpec(darkTheme, profileBackdropStyle)

    MaterialTheme(
        colorScheme = colors,
        typography = Typography()
    ) {
        CompositionLocalProvider(
            LocalProfileBackdropStyle provides profileBackdropStyle,
            LocalProfileBackdropSpec provides backdropSpec,
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
    val cfg = LocalConfiguration.current
    val isLandscape = cfg.orientation == Configuration.ORIENTATION_LANDSCAPE

    val m = this.clip(shape)

    // Solid mode
    if (!spec.enabledFancyBackground) {
        return m.background(spec.base)
    }

    return m.drawBehind {
        val w = size.width
        val h = size.height
        fun p(n: Offset) = Offset(n.x * w, n.y * h)

        // ✅ Landscape: blobs extend lower (more glass visible)
        val cutoff = h * (if (isLandscape) 0.6f else 0.5f)

        // ✅ Make the “flat” area softer in landscape
        val fadeHeight = h * (if (isLandscape) 0.14f else 0.08f)

        // 1) Blobs only above cutoff
        clipRect(0f, 0f, w, cutoff) {
            drawCircle(spec.blob1, spec.r1 * 0.25f, p(spec.c1))
            drawCircle(spec.blob2, spec.r2 * 0.10f, p(spec.c2))
            drawCircle(spec.blob3, spec.r3 * 2.25f, p(spec.c3))
            drawCircle(spec.blob4, spec.r4 * 0.30f, p(spec.c4))
            drawCircle(spec.blob5, spec.r5 * 0.35f, p(spec.c5))
            drawCircle(spec.blob6, spec.r6 * 0.20f, p(spec.c6))
            drawCircle(spec.blob7, spec.r7 * 0.20f, p(spec.c7))
        }

        // 2) Softer “flat” fade (lower alpha = more glass effect shows through)
        val isDark = spec.base.luminance() < 0.5f
        val endColor = (if (isDark) Color(0xFF0E1116) else Color(0xFFF4F5F7))
            .copy(alpha = if (isLandscape) 0.72f else 0.92f) // ✅ key

        clipRect(0f, cutoff - fadeHeight, w, cutoff) {
            drawRect(
                brush = Brush.verticalGradient(
                    0.0f to Color.Transparent,
                    1.0f to endColor,
                    startY = cutoff - fadeHeight,
                    endY = cutoff
                )
            )
        }
    }
}