package com.flights.studio

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.capsule.ContinuousRoundedRectangle

/**
 * ===== 1) Inner glass content (no ModalBottomSheet chrome) =====
 *
 * this is basically your old FlightsMenuLiquidSheet body from :contentReference[oaicite:2]{index=2}
 * BUT without drawing its own fullscreen scrim.
 *
 * we also keep the same blur/vibrancy/lens stack you used.
 */
@Composable
fun FlightsMenuLiquidSheetContent(
    backdrop: LayerBackdrop,
    onSelectOption: (String) -> Unit
) {
    val isDark = isSystemInDarkTheme()

    // sheet background tint
    val sheetSurfaceColor =
        if (isDark) Color(0xFF121212).copy(alpha = 0.40f)
        else Color(0xFFFAFAFA).copy(alpha = 0.60f)

    // text colors
    val mainTextColor =
        if (isDark) Color.White.copy(alpha = 0.92f)
        else Color(0xFF111111)

    val exitTextColor =
        if (isDark) Color(0xFFFF6B6B)
        else Color(0xFFB00020)

    // dark mode "lift"
    val liftTextMod =
        if (isDark) Modifier.graphicsLayer(blendMode = BlendMode.Plus)
        else Modifier

    val sheetShapeRound = RoundedCornerShape(32.dp)
    val sheetShapeCont = ContinuousRoundedRectangle(32.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(sheetShapeRound)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { sheetShapeCont },
                effects = {
                    vibrancy()
                    if (isDark) {
                        blur(12.dp.toPx())
                    } else {
                        blur(16.dp.toPx())
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        lens(
                            refractionHeight = 25.dp.toPx(),
                            refractionAmount = 30.dp.toPx(),
                            chromaticAberration = true
                        )
                    }
                },
                highlight = { Highlight.Plain },
                onDrawSurface = {
                    drawRect(sheetSurfaceColor)
                }
            )
            // consume taps so taps inside don't fall through
            .clickable(
                interactionSource = null,
                indication = null
            ) { /* no-op */ }
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // little handle
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = 36.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = if (isDark) 0.45f else 0.35f))
        )

        Spacer(Modifier.height(4.dp))

        // Profile row
        BasicText(
            text = "Profile",
            modifier = Modifier
                .fillMaxWidth()
                .then(liftTextMod)
                .clickable(
                    interactionSource = null,
                    indication = null
                ) {
                    onSelectOption("profile")
                }
                .padding(vertical = 12.dp),
            style = TextStyle(
                color = mainTextColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        )

        // Settings row
        BasicText(
            text = "Settings",
            modifier = Modifier
                .fillMaxWidth()
                .then(liftTextMod)
                .clickable(
                    interactionSource = null,
                    indication = null
                ) {
                    onSelectOption("settings")
                }
                .padding(vertical = 12.dp),
            style = TextStyle(
                color = mainTextColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        )

        // Exit row
        BasicText(
            text = "Exit App",
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = null,
                    indication = null
                ) {
                    onSelectOption("exit")
                }
                .padding(vertical = 12.dp),
            style = TextStyle(
                color = exitTextColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        )

        Spacer(Modifier.height(12.dp))
    }
}

/**
 * ===== 2) The modal bottom sheet wrapper =====
 *
 * same idea as ReminderOptionsSheetModal you gave me:
 * - rememberModalBottomSheetState(skipPartiallyExpanded = true)
 * - scrimColor = semi black
 * - containerColor = transparent so OUR blur card is visible
 *
 * we play sound on open, and we handle back press by passing onDismiss.
 *
 * you call THIS from your screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightsMenuLiquidSheetModal(
    backdrop: Backdrop,
    visible: Boolean,
    onDismissRequest: () -> Unit,
    onSelectOption: (String) -> Unit
) {
    val context = LocalContext.current

    // play the "open sheet" sound when visible becomes true
    LaunchedEffect(visible) {
        if (visible) {
            playSheetOpenSound(context, R.raw.confirm)
        }
    }

    // if not visible, don't emit anything
    if (!visible) return

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    // hardware back should also dismiss
    BackHandler {
        onDismissRequest()
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        // make M3 sheet chrome invisible so our glass is the actual panel
        containerColor = Color.Transparent,
        dragHandle = null,
        tonalElevation = 0.dp,
        // scrim like ReminderOptionsSheetModal (0.35f)
        scrimColor = Color.Black.copy(alpha = 0.35f)
    ) {
        // our glass body
        FlightsMenuLiquidSheetContent(
            backdrop = backdrop as LayerBackdrop,
            onSelectOption = { which ->
                onSelectOption(which)
            }
        )
    }
}
