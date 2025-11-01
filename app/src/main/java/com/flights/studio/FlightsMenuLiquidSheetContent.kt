package com.flights.studio

import android.content.Intent
import android.os.Build
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
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.capsule.ContinuousRoundedRectangle

@Composable
fun FlightsMenuLiquidSheetContent(
    backdrop: LayerBackdrop,
    notesCount: Int,
    contactsCount: Int,
    isOnline: Boolean = true,          // you can pass real values later
    pendingSyncCount: Int = 0          // how many unsynced local changes
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    // sheet background tint
    val sheetSurfaceColor =
        if (isDark) Color(0xFF121212).copy(alpha = 0.40f)
        else Color(0xFFFAFAFA).copy(alpha = 0.60f)

    // text colors
    val mainTextColor =
        if (isDark) Color.White.copy(alpha = 0.92f)
        else Color(0xFF111111)

    // "lift" glow in dark mode
    val liftTextMod =
        if (isDark) Modifier.graphicsLayer(blendMode = BlendMode.Plus)
        else Modifier

    val sheetShapeRound = RoundedCornerShape(32.dp)
    val sheetShapeCont = ContinuousRoundedRectangle(32.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 8.dp)
            .clip(sheetShapeRound)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { sheetShapeCont },
                effects = {
                    vibrancy()
                    if (isDark) {
                        blur(8.dp.toPx())
                    } else {
                        blur(9.dp.toPx())
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        lens(
                            refractionHeight = 13.dp.toPx(),
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
            // block touch from passing through
            .clickable(
                interactionSource = null,
                indication = null
            ) { }
            .padding(horizontal = 0.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ───── grab handle ─────
        Box(
            modifier = Modifier
                .size(width = 36.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    Color.White.copy(alpha = if (isDark) 0.45f else 0.35f)
                )
        )

        Spacer(Modifier.height(12.dp))

        // ───── stats chip (Notes / Contacts) ─────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    if (isDark) {
                        Color(0xFF000000).copy(alpha = 0.35f)
                    } else {
                        Color(0xFFFFFFFF).copy(alpha = 0.28f)
                    }
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                // LEFT HALF: Notes (tappable)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = null,
                            indication = null
                        ) {
                            // open notes screen
                            context.startActivity(
                                Intent(context, AllNotesActivity::class.java)
                            )
                        }
                ) {
                    Column(
                        horizontalAlignment = Alignment.Start
                    ) {
                        // label
                        BasicText(
                            text = "Notes",
                            modifier = liftTextMod,
                            style = TextStyle(
                                color = mainTextColor.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal
                            )
                        )
                        // value
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
                }

                // RIGHT HALF: Contacts (tappable)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = null,
                            indication = null
                        ) {
                            // open contacts screen
                            context.startActivity(
                                Intent(context, AllContactsActivity::class.java)
                            )
                        },
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        // label
                        BasicText(
                            text = "Contacts",
                            modifier = liftTextMod,
                            style = TextStyle(
                                color = mainTextColor.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal
                            )
                        )
                        // value
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
        }

        Spacer(Modifier.height(16.dp))

        // ───── sync status pill ─────
        // We always show this, but we style it differently if offline or pending.
        val pillBgColor = when {
            !isOnline -> if (isDark) Color(0xFFFF4D4D).copy(alpha = 0.18f)
            else       Color(0xFFFF4D4D).copy(alpha = 0.14f)
            pendingSyncCount > 0 -> if (isDark) Color(0xFFFFC107).copy(alpha = 0.20f)
            else       Color(0xFFFFC107).copy(alpha = 0.16f)
            else -> if (isDark) Color(0xFF00FF88).copy(alpha = 0.18f)
            else       Color(0xFF00AA55).copy(alpha = 0.16f)
        }

        val pillLabel = when {
            !isOnline -> "Offline"
            pendingSyncCount > 0 -> "Offline changes pending"
            else -> "Synced"
        }

        Box(
            modifier = Modifier
                .padding(horizontal = 14.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(pillBgColor)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            BasicText(
                text = pillLabel,
                style = TextStyle(
                    color = mainTextColor.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }

        Spacer(Modifier.height(20.dp))

        // ───── quick action list ─────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    if (isDark) Color(0xFF000000).copy(alpha = 0.25f)
                    else Color(0xFFFFFFFF).copy(alpha = 0.22f)
                )
        ) {
            SheetRowButton(
                label = "Open Notes",
                textColor = mainTextColor,
                isLast = false,
                onClick = {
                    context.startActivity(
                        Intent(context, AllNotesActivity::class.java)
                    )
                }
            )

            SheetRowButton(
                label = "Open Contacts",
                textColor = mainTextColor,
                isLast = false,
                onClick = {
                    context.startActivity(
                        Intent(context, AllContactsActivity::class.java)
                    )
                }
            )

            SheetRowButton(
                label = "Settings",
                textColor = mainTextColor,
                isLast = true,
                onClick = {
                    context.startActivity(
                        Intent(context, SettingsActivity::class.java)
                    )
                }
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
fun FlightsMenuLiquidSheetContentPreview() {
    FlightsMenuLiquidSheetContent(
        backdrop = com.kyant.backdrop.backdrops.rememberLayerBackdrop(),
        notesCount = 123,
        contactsCount = 45,
        isOnline = true,
        pendingSyncCount = 0
    )
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
fun FlightsMenuLiquidSheetContentPendingSyncPreview() {
    FlightsMenuLiquidSheetContent(
        backdrop = com.kyant.backdrop.backdrops.rememberLayerBackdrop(),
        notesCount = 123,
        contactsCount = 45,
        isOnline = true,
        pendingSyncCount = 3
    )
}

@androidx.compose.ui.tooling.preview.Preview
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun FlightsMenuLiquidSheetModalPreview() {
    FlightsMenuLiquidSheetModal(
        backdrop = com.kyant.backdrop.backdrops.rememberLayerBackdrop(),
        visible = true,
        onDismissRequest = {},
        notesCount = 123,
        contactsCount = 45
    )
}


@Composable
private fun SheetRowButton(
    label: String,
    textColor: Color,
    isLast: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = null,
                indication = null
            ) { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        BasicText(
            text = label,
            style = TextStyle(
                color = textColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }

    if (!isLast) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(textColor.copy(alpha = 0.08f))
        )
    }
}



// ⬇️ ADD THIS RIGHT AFTER SheetRowButton
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

    androidx.compose.runtime.LaunchedEffect(visible) {
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
            contactsCount = contactsCount,
            isOnline = true,
            pendingSyncCount = 0
        )
    }
}

