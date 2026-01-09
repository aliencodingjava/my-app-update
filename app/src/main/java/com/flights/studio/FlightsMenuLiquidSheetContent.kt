package com.flights.studio

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
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
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight

// ─────────────────────────────────────────────────────────────────────────────
//  MAIN GLASS SHEET – big rounded rect + stats + 3 LiquidButtons
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun FlightsMenuLiquidSheetContent(
    backdrop: LayerBackdrop,
    notesCount: Int,
    contactsCount: Int
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    // secondary backdrop for the sheet & inner boxes (Kyant: bottomSheetBackdrop)
    val sheetBackdrop = rememberLayerBackdrop()

    val sheetSurfaceColor =
        if (isDark) Color(0xFF101010).copy(alpha = 0.85f)
        else Color.White.copy(alpha = 0.90f)

    val mainTextColor =
        if (isDark) Color.White.copy(alpha = 0.96f)
        else Color(0xFF111111)

    val liftTextMod =
        if (isDark) Modifier.graphicsLayer(blendMode = BlendMode.Plus)
        else Modifier

    val sheetShapeRound = RoundedCornerShape(42.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 8.dp)
            .clip(sheetShapeRound)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(44.dp) },
                effects = {
                    vibrancy()
                    blur(2.dp.toPx())
                    lens(24.dp.toPx(), 48.dp.toPx(), true)
                },
                highlight = { Highlight.Plain },
                exportedBackdrop = sheetBackdrop,
                onDrawSurface = { drawRect(sheetSurfaceColor) }
            )
            .clickable(
                interactionSource = null,
                indication = null
            ) { }
            .padding(horizontal = 0.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // grab handle
        Box(
            modifier = Modifier
                .size(width = 36.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    Color.White.copy(alpha = if (isDark) 0.50f else 0.40f)
                )
        )

        Spacer(Modifier.height(18.dp))

        // ───── BOX #1: Stats (Notes / Contacts) – glass bar ─────
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .height(64.dp)
                .drawBackdrop(
                    backdrop = sheetBackdrop,
                    shape = { RoundedCornerShape(20.dp) },
                    shadow = null,
                    effects = {
                        vibrancy()
                        blur(2.dp.toPx())
                        lens(16.dp.toPx(), 32.dp.toPx())
                    },
                    onDrawSurface = {
                        drawRect(Color.Green.copy(alpha = 0.15f))
                    }
                )
                .clip(RoundedCornerShape(20.dp))
                .padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.weight(1f)
                ) {
                    BasicText(
                        text = "Notes",
                        modifier = liftTextMod,
                        style = TextStyle(
                            color = mainTextColor.copy(alpha = 0.65f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal
                        )
                    )
                    BasicText(
                        text = notesCount.toString(),
                        modifier = liftTextMod,
                        style = TextStyle(
                            color = mainTextColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.25).sp
                        )
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.weight(1f)
                ) {
                    BasicText(
                        text = "Contacts",
                        modifier = liftTextMod,
                        style = TextStyle(
                            color = mainTextColor.copy(alpha = 0.65f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal
                        )
                    )
                    BasicText(
                        text = contactsCount.toString(),
                        modifier = liftTextMod,
                        style = TextStyle(
                            color = mainTextColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.25).sp
                        )
                    )
                }
            }
        }

        Spacer(Modifier.height(22.dp))

        // ───── BOX #2: Open Notes – LiquidButton ─────
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            LiquidButton(
                onClick = {
                    context.startActivity(Intent(context, AllNotesActivity::class.java))
                },
                iconRes = R.drawable.ic_oui_notes,
                label = "Open Notes",
                backdrop = sheetBackdrop,
                modifier = Modifier.fillMaxWidth(),
//                centerLabel = true          // 🔹 HERE
            )
        }

        Spacer(Modifier.height(12.dp))

        // ───── BOX #3: Open Contacts – LiquidButton ─────
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            LiquidButton(
                onClick = {
                    context.startActivity(Intent(context, AllContactsActivity::class.java))
                },
                iconRes = R.drawable.contact_page_24dp_ffffff_fill1_wght400_grad0_opsz24,
                label = "Open Contacts",
                backdrop = sheetBackdrop,
                modifier = Modifier.fillMaxWidth(),
//                centerLabel = true          // 🔹 HERE
            )
        }

        Spacer(Modifier.height(12.dp))

        // ───── BOX #4: Settings – LiquidButton ─────
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            LiquidButton(
                onClick = {
                    context.startActivity(Intent(context, SettingsActivity::class.java))
                },
                iconRes = R.drawable.ic_settings_black_24dp,
                label = "Settings",
                backdrop = sheetBackdrop,
                modifier = Modifier.fillMaxWidth(),
//                centerLabel = true          // 🔹 HERE
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Modal wrapper – unchanged
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun FlightsMenuLiquidSheetModal(
    backdrop: LayerBackdrop,
    visible: Boolean,
    onDismissRequest: () -> Unit,
    notesCount: Int,
    contactsCount: Int
) {
    val context = LocalContext.current

    LaunchedEffect(visible) {
        if (visible) {
            playSheetOpenSound(context, R.raw.confirm)
        }
    }

    if (!visible) return

    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    BackHandler { onDismissRequest() }

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        dragHandle = null,
        tonalElevation = 0.dp,
        scrimColor = Color.Black.copy(alpha = 0.35f)
    ) {
        FlightsMenuLiquidSheetContent(
            backdrop = backdrop,
            notesCount = notesCount,
            contactsCount = contactsCount
        )
    }
}
