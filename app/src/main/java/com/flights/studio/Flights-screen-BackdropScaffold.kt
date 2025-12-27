package com.flights.studio

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

// -----------------------------------------------------------------------------
// COLOR SCHEMES
// -----------------------------------------------------------------------------

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4FC3F7),
    onPrimary = Color(0xFF003355),
    background = Color(0xFF020617),
    onBackground = Color(0xFFE3E2E6),
    surface = Color(0xFF101522),
    onSurface = Color(0xFFE3E2E6)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF03A9F4),
    onPrimary = Color.White,
    background = Color(0xFFF7FAFF),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFEFF6F8),
    onSurface = Color(0xFF1C1B1F)
)

// -----------------------------------------------------------------------------
// THEME
// -----------------------------------------------------------------------------

@Suppress("DEPRECATION")
@Composable
fun FlightsModernTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    val animatedBgColor = animateColorAsState(
        targetValue = colorScheme.background,
        animationSpec = tween(500),
        label = "bgColor"
    )

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as android.app.Activity).window
            window.statusBarColor = animatedBgColor.value.toArgb()
            window.navigationBarColor = animatedBgColor.value.toArgb()
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}

// -----------------------------------------------------------------------------
// BACKDROP SCAFFOLD
// -----------------------------------------------------------------------------

@Composable
fun FlightsBackdropScaffold(
    content: @Composable (
        globalBackdrop: LayerBackdrop,
        buttonsBackdrop: LayerBackdrop
    ) -> Unit
) {
    FlightsModernTheme {
        val globalBackdrop = rememberLayerBackdrop()
        val buttonsBackdrop = rememberLayerBackdrop()

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CirclesBackground(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(globalBackdrop)
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(buttonsBackdrop)
            ) {
                content(globalBackdrop, buttonsBackdrop)
            }
        }
    }
}

// -----------------------------------------------------------------------------
// SUPER-HD GLOW (NO CENTER DOT, PURE SOFT BLOOM)
// -----------------------------------------------------------------------------

@Composable
fun CirclesBackground(
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    Box(
        modifier = modifier.drawBehind {
            val w = size.width
            val h = size.height
            val minDim = size.minDimension

            // Base background
            val bgColor = if (isDark) Color(0xFF020617) else Color(0xFFF7FAFF)
            drawRect(bgColor)

            // Calmer glow
            val glowStrength = if (isDark) 0.15f else 0.12f
            val solidAlpha = 0.38f

            // Sizes
            val glowBig = 0.26f
            val glowMed = 0.38f
            val glowSmall = 0.22f
            val solidBig = 0.20f
            val solidMed = 0.26f
            val solidSmall = 0.32f

            // Glow colors
            val glowTopLeft = if (isDark) Color(0xFF22D3EE) else Color(0xFF60A5FA)
            val glowTopRight = if (isDark) Color(0xFF22C55E) else Color(0xFF34D399)
            val glowCenter = if (isDark) Color(0xFF38BDF8) else Color(0xFF3B82F6)
            val glowBottomLeft = if (isDark) Color(0xFFA855F7) else Color(0xFF22C1C3)
            val glowBottomRight = if (isDark) Color(0xFFA855F7) else Color(0xFFF97316)

            // Solid colors
            val solidLeft = if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB)
            val solidTop = if (isDark) Color(0xFF22C55E) else Color(0xFFF97316)
            val solidBottom = if (isDark) Color(0xFFA855F7) else Color(0xFF84CC16)

            fun hdGlow(center: Offset, radiusFactor: Float, c: Color) {
                val r = minDim * radiusFactor

                fun layer(alphaMul: Float, radiusMul: Float) {
                    val a = glowStrength * alphaMul
                    drawCircle(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0.00f to c.copy(alpha = a),
                                0.20f to c.copy(alpha = a * 0.65f),
                                0.42f to c.copy(alpha = a * 0.38f),
                                0.66f to c.copy(alpha = a * 0.16f),
                                0.85f to c.copy(alpha = a * 0.06f),
                                1.00f to Color.Transparent
                            ),
                            center = center,
                            radius = r * radiusMul
                        ),
                        radius = r * radiusMul,
                        center = center,
                        blendMode = BlendMode.Plus
                    )
                }

                // Pure bloom only (NO inner dot)
                layer(0.70f, 1.50f)
                layer(0.90f, 1.05f)
                layer(1.00f, 0.70f)
            }

            fun solidCircle(center: Offset, radiusFactor: Float, c: Color) {
                drawCircle(
                    color = c.copy(alpha = solidAlpha),
                    radius = minDim * radiusFactor,
                    center = center
                )
            }

            // Glow blobs
            hdGlow(Offset(w * 0.18f, h * 0.20f), glowMed, glowTopLeft)
            hdGlow(Offset(w * 0.85f, h * 0.18f), glowSmall, glowTopRight)
            hdGlow(Offset(w * 0.50f, h * 0.52f), glowBig, glowCenter)
            hdGlow(Offset(w * 0.20f, h * 0.80f), glowSmall, glowBottomLeft)
            hdGlow(Offset(w * 0.80f, h * 0.88f), glowMed, glowBottomRight)

            // Solid circles
            solidCircle(Offset(w * 0.30f, h * 0.48f), solidBig, solidLeft)
            solidCircle(Offset(w * 0.72f, h * 0.24f), solidMed, solidTop)
            solidCircle(Offset(w * 0.60f, h * 0.88f), solidSmall, solidBottom)

            // Overlay
            if (isDark) {
                drawRect(Color.Black.copy(alpha = 0.18f))
            } else {
                drawRect(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.45f),
                            Color.White.copy(alpha = 0f)
                        )
                    )
                )
            }
        }
    )
}

// -----------------------------------------------------------------------------
// PREVIEW
// -----------------------------------------------------------------------------

@Preview
@Composable
fun FlightsBackdropScaffoldPreview() {
    FlightsBackdropScaffold { _, _ -> }
}
