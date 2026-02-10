package com.flights.studio

import android.content.Intent
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight

@Composable
fun FlightsMenuLiquidSheetContent(
    backdrop: LayerBackdrop,
    notesCount: Int,
    contactsCount: Int
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    val ui = rememberUiScale()
    val uiTight = rememberUiTight()

    // child sampling backdrop (correct glass-on-glass)
    val sheetBackdrop = rememberLayerBackdrop()

    val sheetShape = RoundedCornerShape(34.dp.us(ui))
    val cardShape = RoundedCornerShape(20.dp.us(ui))
    val rowShape = RoundedCornerShape(18.dp.us(ui))

    val sheetSurface =
        if (isDark) Color(0xFF0C0C0E).copy(alpha = 0.86f)
        else Color.White.copy(alpha = 0.92f)

    val textMain =
        if (isDark) Color.White.copy(alpha = 0.96f)
        else Color(0xFF101010)

    val textSub =
        if (isDark) Color.White.copy(alpha = 0.62f)
        else Color(0xFF101010).copy(alpha = 0.62f)

    val liftTextMod =
        if (isDark) Modifier.graphicsLayer(blendMode = BlendMode.Plus)
        else Modifier

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp.us(ui), vertical = 8.dp.us(ui))
            .clip(sheetShape)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { sheetShape },
                effects = {
                    vibrancy()
//                    blur(2.dp.us(ui).toPx())
                    lens(
                        refractionHeight = 24.dp.us(ui).toPx(),
                        refractionAmount = 24.dp.us(ui).toPx(),
                        depthEffect = true
                    )
                },
                highlight = { Highlight.Plain },
                exportedBackdrop = sheetBackdrop,
                onDrawSurface = { drawRect(sheetSurface) }
            )
            .clickable(interactionSource = null, indication = null) { }
            .heightIn(min = 320.dp.us(ui))
            .padding(
                start = 18.dp.us(ui),
                end = 18.dp.us(ui),
                top = 12.dp.us(ui),
                bottom = 16.dp.us(ui)
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // handle
        Box(
            modifier = Modifier
                .size(width = 40.dp.us(ui), height = 4.dp.us(ui))
                .clip(RoundedCornerShape(2.dp.us(ui)))
                .background(Color.White.copy(alpha = if (isDark) 0.48f else 0.32f))
        )

        Spacer(Modifier.height(14.dp.us(ui)))

        // ===== Header (NEW) =====
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .drawBackdrop(
                    backdrop = sheetBackdrop,
                    shape = { cardShape },
                    shadow = null,
                    effects = {
                        vibrancy()
//                        blur(2.dp.us(ui).toPx())
                        lens(0.dp.us(ui).toPx(), 0.dp.us(ui).toPx())
                    },
                    onDrawSurface = { drawRect(Color.Black.copy(alpha = if (isDark) 0.72f else 0.18f)) }
                )
                .padding(horizontal = 14.dp.us(ui), vertical = 12.dp.us(ui))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    BasicText(
                        text = "Menu",
                        modifier = liftTextMod,
                        style = TextStyle(
                            color = textMain,
                            fontSize = 16.sp.us(uiTight),
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.2).sp.us(uiTight)
                        )
                    )
                    Spacer(Modifier.height(2.dp.us(ui)))
                    BasicText(
                        text = "Quick access",
                        modifier = liftTextMod,
                        style = TextStyle(
                            color = textSub,
                            fontSize = 11.sp.us(uiTight),
                            fontWeight = FontWeight.Normal
                        )
                    )
                }

                val chipWidth = 80.dp.us(ui)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp.us(ui))) {
                    CountChip(
                        modifier = Modifier.width(chipWidth),
                        backdrop = sheetBackdrop,
                        label = "Notes",
                        value = notesCount.toString(),
                        ui = ui,
                        uiTight = uiTight,
                        isDark = isDark,
                        textMain = textMain,
                        textSub = textSub,
                        liftTextMod = liftTextMod
                    )
                    CountChip(
                        modifier = Modifier.width(chipWidth),
                        backdrop = sheetBackdrop,
                        label = "Contacts",
                        value = contactsCount.toString(),
                        ui = ui,
                        uiTight = uiTight,
                        isDark = isDark,
                        textMain = textMain,
                        textSub = textSub,
                        liftTextMod = liftTextMod
                    )
                }

            }
        }

        Spacer(Modifier.height(14.dp.us(ui)))

        // ===== Menu Rows (NEW) =====
        MenuRow(
            backdrop = sheetBackdrop,
            shape = rowShape,
            ui = ui,
            uiTight = uiTight,
            isDark = isDark,
            textMain = textMain,
            textSub = textSub,
            liftTextMod = liftTextMod,
            iconRes = R.drawable.ic_oui_notes,
            title = "Notes",
            subtitle = "Open all notes"
        ) { context.startActivity(Intent(context, AllNotesActivity::class.java)) }

        Spacer(Modifier.height(10.dp.us(ui)))

        MenuRow(
            backdrop = sheetBackdrop,
            shape = rowShape,
            ui = ui,
            uiTight = uiTight,
            isDark = isDark,
            textMain = textMain,
            textSub = textSub,
            liftTextMod = liftTextMod,
            iconRes = R.drawable.contact_page_24dp_ffffff_fill1_wght400_grad0_opsz24,
            title = "Contacts",
            subtitle = "Open contacts list"
        ) { context.startActivity(Intent(context, AllContactsActivity::class.java)) }

        Spacer(Modifier.height(10.dp.us(ui)))

        MenuRow(
            backdrop = sheetBackdrop,
            shape = rowShape,
            ui = ui,
            uiTight = uiTight,
            isDark = isDark,
            textMain = textMain,
            textSub = textSub,
            liftTextMod = liftTextMod,
            iconRes = R.drawable.ic_settings_black_24dp,
            title = "Settings",
            subtitle = "Preferences & appearance"
        ) { context.startActivity(Intent(context, SettingsActivity::class.java)) }

        Spacer(Modifier.height(14.dp.us(ui)))

        BasicText(
            text = "Swipe down to close",
            modifier = liftTextMod,
            style = TextStyle(
                color = textSub.copy(alpha = 0.9f),
                fontSize = 10.sp.us(uiTight),
                fontWeight = FontWeight.Normal
            )
        )

        Spacer(Modifier.height(6.dp.us(ui)))
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun FlightsMenuLiquidSheetModal(
    backdrop: LayerBackdrop,
    visible: Boolean,
    onDismissRequest: () -> Unit,
    notesCount: Int,
    contactsCount: Int
) {
    if (!visible) return

    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    androidx.activity.compose.BackHandler { onDismissRequest() }

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


@Composable
private fun CountChip(
    modifier: Modifier = Modifier,
    backdrop: LayerBackdrop,
    label: String,
    value: String,
    ui: Float,
    uiTight: Float,
    isDark: Boolean,
    textMain: Color,
    textSub: Color,
    liftTextMod: Modifier
) {
    val shape = RoundedCornerShape(999.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                shadow = null,
                effects = {
                    vibrancy()
                    blur(3.dp.us(ui).toPx())
                    lens(0.dp.us(ui).toPx(), 0.dp.us(ui).toPx())
                },
                onDrawSurface = {
                    drawRect(
                        if (isDark) Color.White.copy(alpha = 0.18f)
                        else Color.Black.copy(alpha = 0.18f)
                    )
                }
            )
            .padding(horizontal = 10.dp.us(ui), vertical = 7.dp.us(ui)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            BasicText(
                text = label,
                modifier = liftTextMod,
                style = TextStyle(
                    color = textSub,
                    fontSize = 9.sp.us(uiTight),
                    fontWeight = FontWeight.Normal
                )
            )
            BasicText(
                text = value,
                modifier = liftTextMod,
                style = TextStyle(
                    color = textMain,
                    fontSize = 12.sp.us(uiTight),
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.2).sp.us(uiTight)
                )
            )
        }
    }
}

@Composable
private fun MenuRow(
    modifier: Modifier = Modifier,
    backdrop: LayerBackdrop,
    shape: RoundedCornerShape,
    ui: Float,
    uiTight: Float,
    isDark: Boolean,
    textMain: Color,
    textSub: Color,
    liftTextMod: Modifier,
    iconRes: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                shadow = null,
                effects = {
                    vibrancy()
//                    blur(2.dp.us(ui).toPx())
                    lens(8.dp.us(ui).toPx(), 8.dp.us(ui).toPx())
                },
                onDrawSurface = {
                    drawRect(
                        if (isDark) Color.Black.copy(alpha = 0.60f)
                        else Color.Black.copy(alpha = 0.18f)
                    )
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp.us(ui), vertical = 12.dp.us(ui)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // left icon bubble
        Box(
            modifier = Modifier
                .size(40.dp.us(ui))
                .clip(CircleShape)
                .background(
                    if (isDark) Color.White.copy(alpha = 0.07f)
                    else Color.Black.copy(alpha = 0.05f)
                ),
            contentAlignment = Alignment.Center
        ) {
            // uses your existing painter approach elsewhere; keep it simple with LiquidButton icons
            val needsLightTint = !isDark && (
                    iconRes == R.drawable.contact_page_24dp_ffffff_fill1_wght400_grad0_opsz24 ||
                            iconRes == R.drawable.ic_settings_black_24dp
                    )

            val iconTint = if (needsLightTint) Color(0xFF101010) else Color.Unspecified

            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = iconRes),
                contentDescription = title,
                modifier = Modifier.size(20.dp.us(ui)),
                alpha = if (isDark) 0.96f else 1f, // <- no grey-out in light
                colorFilter = if (needsLightTint)
                    androidx.compose.ui.graphics.ColorFilter.tint(iconTint)
                else null
            )

        }

        Spacer(Modifier.size(12.dp.us(ui)))

        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = title,
                modifier = liftTextMod,
                style = TextStyle(
                    color = textMain,
                    fontSize = 13.sp.us(uiTight),
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.15).sp.us(uiTight)
                )
            )
            Spacer(Modifier.height(2.dp.us(ui)))
            BasicText(
                text = subtitle,
                modifier = liftTextMod,
                style = TextStyle(
                    color = textSub,
                    fontSize = 10.sp.us(uiTight),
                    fontWeight = FontWeight.Normal
                )
            )
        }

        // chevron (no new icon dependency)
        BasicText(
            text = "›",
            modifier = liftTextMod,
            style = TextStyle(
                color = textSub.copy(alpha = 0.9f),
                fontSize = 18.sp.us(uiTight),
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}
