package com.flights.studio

import android.os.Build
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.LayerBackdrop
import com.kyant.backdrop.backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.refraction
import com.kyant.backdrop.highlight.onDrawSurfaceWithHighlight
import com.kyant.backdrop.rememberLayerBackdrop
import kotlin.math.min

// ---------- Data ----------
data class GlassBtn(
    val id: String,
    @param:DrawableRes @field:DrawableRes val icon: Int,
    val label: String
)

// ---------- Screen ----------
@Composable
fun SplashGlassScreen(
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
    gridHeightFraction: Float = 0.49f,
    darkPalette: List<Color> = listOf(Color(0x000E0E10), Color(0x00141515), Color(0x000A0C10)),
    lightPalette: List<Color> = listOf(Color(0x00F2F6FF), Color(0x00E3F2FD), Color(0x0099CFF8))
) {
    val isDark = isSystemInDarkTheme()
    val palette = if (isDark) darkPalette else lightPalette

    val backdrop: LayerBackdrop = rememberLayerBackdrop(
        backgroundColor = if (isDark) Color(0xFF0E0E10) else Color(0xFFD9E2F8)
    )

    Box(
        modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = palette,
                    start = Offset.Zero,
                    end = Offset(0f, 1800f)
                )
            )
    ) {
        // Provider (what glass samples)
        Box(
            Modifier
                .matchParentSize()
                .backdrop(backdrop)
        ) {
            SplashGlassBackdrop( // ← renamed to avoid clash
                palette = palette,
                modifier = Modifier.matchParentSize()
            )
        }

        // Bottom grid
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(gridHeightFraction)
                .padding(3.dp)
        ) {
            GlassButtonsGrid(
                backdrop = backdrop,
                onOpen = onOpen,
                modifier = Modifier
                    .matchParentSize()
                    .padding(4.dp)
            )
        }
    }
}

@Preview
@Composable
fun SplashGlassScreenPreview() {
    SplashGlassScreen(onOpen = {})
}

// ---------- Grid ----------
@Composable
private fun GlassButtonsGrid(
    backdrop: Backdrop,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val rows: List<List<GlassBtn>> = listOf(
        listOf(
            GlassBtn("card13", R.drawable.fiber_manual_record_24dp_ffffff_fill0_wght400_grad0_opsz24, ""),
            GlassBtn("card14", R.drawable.fiber_manual_record_24dp_ffffff_fill0_wght400_grad0_opsz24, ""),
            GlassBtn("card15", R.drawable.fiber_manual_record_24dp_ffffff_fill0_wght400_grad0_opsz24, ""),
            GlassBtn("card16", R.drawable.fiber_manual_record_24dp_ffffff_fill0_wght400_grad0_opsz24, "")
        ),
        listOf(
            GlassBtn("card9",  R.drawable.account_circle_24dp_ffffff_fill1_profile, "My Profile"),
            GlassBtn("card10", R.drawable.fiber_manual_record_24dp_ffffff_fill0_wght400_grad0_opsz24, ""),
            GlassBtn("card11", R.drawable.fiber_manual_record_24dp_ffffff_fill0_wght400_grad0_opsz24, ""),
            GlassBtn("card12", R.drawable.fiber_manual_record_24dp_ffffff_fill0_wght400_grad0_opsz24, "")
        ),
        listOf(
            GlassBtn("card5", R.drawable.ic_oui_qr_code, "QR Code"),
            GlassBtn("card6", R.drawable.settings_account_box_24dp_ffffff_fill1_wght400_grad0_opsz24, "Settings"),
            GlassBtn("card7", R.drawable.groups_2_24dp_ffffff_fill1_wght400_grad0_opsz24, "Contacts"),
            GlassBtn("card8", R.drawable.book_24dp_ffffff_fill1_wght400_grad0_opsz24, "Notes")
        ),
        listOf(
            GlassBtn("card1", R.drawable.play_arrow_24dp_ffffff_fill1_wght400_grad0_opsz24, "Play"),
            GlassBtn("card2", R.drawable.ic_oui_news, "News"),
            GlassBtn("card3", R.drawable.flight_24dp_ffffff_fill0_wght400_grad0_opsz24, "Flights"),
            GlassBtn("card4", R.drawable.travel_16dp_ffffff_fill0_wght400_grad0_opsz20, "Travel")
        )
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        rows.forEach { row ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row.forEach { spec ->
                    GlassButton(
                        spec = spec,
                        backdrop = backdrop,
                        onClick = { onOpen(spec.id) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }
}

// ---------- Button (pre-style: pointerInput + spring) ----------
@Composable
private fun GlassButton(
    spec: GlassBtn,
    backdrop: Backdrop,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(28.dp)
    val haptic = LocalHapticFeedback.current

    var isPressed by remember { mutableStateOf(false) }
    val sx by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
        label = "sx"
    )
    val sy by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 600f),
        label = "sy"
    )

    Box(
        modifier
            .fillMaxSize()
            .clip(shape)
            .pointerInput(onClick) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        val released = try { tryAwaitRelease() } finally { isPressed = false }
                        if (released) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onClick()
                        }
                    }
                )
            }
            // Lift the WHOLE card (glass + content), exactly like your old one
            .drawWithContent {
                translate(0f, lerp(0f, 4.dp.toPx(), sy)) {
                    this@drawWithContent.drawContent()
                }
            }
            // Scale the WHOLE card
            .graphicsLayer {
                scaleX = lerp(1f, 0.9f, sx)
                scaleY = lerp(1f, 0.9f, sy)
                // same pivot as before
                transformOrigin = TransformOrigin(0f, 0f)
            }
            // Glass rendering (alpha-9)
            .drawBackdrop(backdrop) {
                this.shape = shape
                // 0 blur but still acrylic via scrim below
                blur(0.dp.toPx())

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val amt = min(size.minDimension, 48.dp.toPx())
                    refraction(height = 8.dp.toPx(), amount = amt)
                }

                onDrawSurfaceWithHighlight {
                    // stronger scrim like your older snippet so it stays visible on white
                    drawRect(Color.Black.copy(alpha = 0.5f))
                }
            }
            .padding(15.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val iconColor =
                if (isSystemInDarkTheme()) MaterialTheme.colorScheme.outline
                else MaterialTheme.colorScheme.onPrimary

            Image(
                painter = painterResource(spec.icon),
                contentDescription = spec.label,
                modifier = Modifier.size(36.dp),
                colorFilter = ColorFilter.tint(iconColor)
            )
            if (spec.label.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                BasicText(
                    spec.label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    }
}


// ---------- Backdrop content (renamed to avoid collision) ----------
@Composable
private fun SplashGlassBackdrop(
    palette: List<Color>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier.background(
            Brush.linearGradient(
                colors = palette,
                start = Offset.Zero,
                end = Offset(0f, 1800f)
            )
        )
    )
}
