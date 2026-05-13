package com.flights.studio

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy


/* ---------- LIGHT ---------- */
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0A6DFF),
    onPrimary = Color.White,
    // ✅ less glare
    surface = Color(0xFFF0F2F5),
    surfaceVariant = Color(0xFFE3E7EE),
    onSurface = Color(0xFF101214),
    onSurfaceVariant = Color(0xFF4A4F57),
    outline = Color(0xFFD0D6DF),
    outlineVariant = Color(0xFFC4CBD6)
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
enum class ProfileBackdropStyle {
    Auto,          // adaptive
    Glass,         // classic glass
    ClearGlass,    // sharp glass
    Blur,          // soft blur
    Frosted,       // heavy blur + wash
    VibrantGlass,  // colorful glass
    Solid,         // flat UI
    Amoled         // pure black (special)
}


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
    // Decide if we want fancy bg (so glass/blur looks different from solid)
    val enableFancy = when (style) {

        // 🔒 Flat modes (no blobs / no glass background)
        ProfileBackdropStyle.Solid,
        ProfileBackdropStyle.Amoled -> false

        // ✨ Glass & effect modes
        ProfileBackdropStyle.Auto,
        ProfileBackdropStyle.Glass,
        ProfileBackdropStyle.ClearGlass,
        ProfileBackdropStyle.Blur,
        ProfileBackdropStyle.Frosted,
        ProfileBackdropStyle.VibrantGlass -> true
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
            lightA1 = 0.14f,
            lightA2 = 0.12f,
            lightA3 = 0.10f,
            lightA4 = 0.09f,
            lightA5 = 0.08f,
            lightA6 = 0.12f,
            lightA7 = 0.10f,

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
    enabled: Boolean = true,
): Modifier {
    if (!enabled) return this

    val spec = LocalProfileGlassSpec.current
    val wash = rememberProfileSurfaceWashColor()

    return this
        .clip(shape)
        .drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            highlight = null,
            innerShadow = null,
            effects = {
                if (spec.vibrancy) vibrancy()

                // ✅ color controls
                colorControls(
                    brightness = spec.brightness,
                    contrast = spec.contrast,
                    saturation = spec.saturation
                )

                // ✅ blur (with edgeTreatment)
                if (spec.blur.value > 0f) {
                    blur(spec.blur.toPx(), edgeTreatment = spec.blurEdge)
                }

                // ✅ lens
                if (spec.lensOuter.value > 0f || spec.refractionAmount > 0f || spec.refractionHeight > 0f) {
                    lens(
                        refractionHeight = spec.refractionHeight, // Float
                        refractionAmount = spec.refractionAmount, // Float
                        depthEffect = spec.depthEffect,
                        chromaticAberration = spec.chromaticAberration
                    )

                } else if (spec.lensOuter.value > 0f) {
                    // fallback to your old lens signature if you still want it:
                    lens(spec.lensInner.toPx(), spec.lensOuter.toPx())
                }
            },
            onDrawSurface = { drawRect(wash) }
        )
}

val LocalAppPageBg = staticCompositionLocalOf { Color.Unspecified }

@Composable
fun FlightsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    profileBackdropStyle: ProfileBackdropStyle = ProfileBackdropStyle.Auto,
    content: @Composable () -> Unit
) {
    val colors = when {
        darkTheme && profileBackdropStyle == ProfileBackdropStyle.Amoled -> darkColorScheme(
            primary = DarkColorScheme.primary,
            onPrimary = DarkColorScheme.onPrimary,

            // ✅ true AMOLED page
            background = Color.Black,
            surface = Color.Black,

            // ✅ cards must NOT be black or they disappear
            surfaceVariant = Color(0xFF14161B),          // <- lifted card color
            onSurface = Color(0xFFE9ECF2),
            onSurfaceVariant = Color(0xFFB8BFCC),

            outline = Color(0xFF2A2F3A),
            outlineVariant = Color(0xFF3A4150),
        )

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // ✅ GLOBAL page background (same everywhere)
    val appPageBg = rememberAppPageBg(
        darkTheme = darkTheme,
        profileBackdropStyle = profileBackdropStyle,
        colors = colors
    )

    val backdropSpec = rememberProfileBackdropSpec(darkTheme, profileBackdropStyle, colors)
    val glassSpec = rememberProfileGlassSpec(darkTheme, profileBackdropStyle)

    MaterialTheme(
        colorScheme = colors,
        typography = Typography()
    ) {
        CompositionLocalProvider(
            LocalProfileBackdropStyle provides profileBackdropStyle,
            LocalProfileBackdropSpec provides backdropSpec,
            LocalProfileGlassSpec provides glassSpec,
            LocalAppPageBg provides appPageBg // ✅ provide it
        ) {
            content()
        }
    }
}





@Composable
private fun rememberAppPageBg(
    darkTheme: Boolean,
    profileBackdropStyle: ProfileBackdropStyle,
    colors: ColorScheme
): Color {
    if (darkTheme && profileBackdropStyle == ProfileBackdropStyle.Amoled) return Color.Black

    return if (darkTheme) {
        colors.surface
    } else {
        // ✅ soft light canvas: between surface and surfaceVariant
        lerp(colors.surface, colors.surfaceVariant, 0.55f)
    }
}


@Composable
fun ProfileBackdropImageLayer(
    modifier: Modifier = Modifier,
    @DrawableRes lightRes: Int,
    @DrawableRes darkRes: Int,
    imageAlpha: Float = 0.18f,
    scrimDark: Float = 0.55f,
    scrimLight: Float = 0.06f,
) {
    val isDark = isSystemInDarkTheme()
    val resId = if (isDark) darkRes else lightRes

    Box(modifier
    ) {
        // base (your theme bg)
        Box(Modifier.matchParentSize()
            .background(LocalAppPageBg.current))

        // wallpaper
        Image(
            painter = painterResource(resId),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
            alpha = imageAlpha
        )

        // scrim for readability
        Box(
            Modifier
                .matchParentSize()
                .background(
                    (if (isDark) Color.Black else Color.White)
                        .copy(alpha = if (isDark) scrimDark else scrimLight)
                )
        )
    }
}
