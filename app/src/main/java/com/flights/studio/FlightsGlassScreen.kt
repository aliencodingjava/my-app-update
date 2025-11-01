package com.flights.studio

import android.os.Build
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

data class GlassBtn(
    val id: String,
    @param:DrawableRes val icon: Int,
    val label: String,
    val tintIcon: Boolean = true
)


@Composable
fun FlightsGlassScreen(
    onTabChanged: (FlightsTab) -> Unit,
    onFullScreen: () -> Unit,
    onBack: () -> Unit,
    onMenu: () -> Unit,
    onOpenCard: (String) -> Unit,
    showTopArea: Boolean = true,
    backdropOverride: LayerBackdrop? = null,
    tint: Color = Color(0xFF00BFA5), // same teal-green you set there
    surfaceColor: Color = Color.White.copy(alpha = 0.08f),
) {


    // ONE backdrop. This is the source of truth.
    val backdrop: LayerBackdrop = backdropOverride ?: rememberLayerBackdrop {
        // same scene we also draw visually under everything ↓


        drawContent()
    }

    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    fun tick() {
        if (!view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
    fun confirm() {
        if (!view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
    fun thresholdActivate() {
        val act = if (Build.VERSION.SDK_INT >= 34)
            android.view.HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE
        else android.view.HapticFeedbackConstants.CLOCK_TICK
        if (!view.performHapticFeedback(act)) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 16.dp)
    ) {

        // ---- TOP BAR ----
        if (showTopArea) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .safeDrawingPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TopBarLiquidIconButton(
                    iconRes = R.drawable.ic_oui_arrow_to_left,
                    backdrop = backdrop,
                    onClick = { tick(); onBack() }
                )
                TopBarLiquidIconButton(
                    iconRes = R.drawable.more_vert_24dp_ffffff_fill1_wght400_grad0_opsz24,
                    backdrop = backdrop,
                    onClick = { tick(); onMenu() }
                )
            }
        }

        // ---- GRID ----
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // visual background == SAME as backdrop above


            GlassButtonsGrid(
                backdrop = backdrop,
                onOpen = { id ->
                    tick()
                    onOpenCard(id)
                },
                modifier = Modifier
                    .matchParentSize()
                    .padding(0.dp)
            )
        }

        // ---- BOTTOM NAV + FAB ----
        Row(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 10.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val selectedTab = remember { mutableStateOf(FlightsTab.Curb) }
            val lastTab = remember { mutableStateOf(selectedTab.value) }

            LaunchedEffect(selectedTab.value) {
                if (selectedTab.value != lastTab.value) {
                    thresholdActivate()
                    confirm()
                    lastTab.value = selectedTab.value
                }
                onTabChanged(selectedTab.value)
            }

            BottomTabs(
                tabs = FlightsTab.entries,
                selectedTabState = selectedTab,
                backdrop = backdrop,

                modifier = Modifier.weight(1f)
            ) { tab ->
                when (tab) {
                    FlightsTab.Curb -> BottomTab(
                        icon = { c ->
                            Box(
                                Modifier.paint(
                                    painterResource(R.drawable.ic_oui_parking),
                                    colorFilter = ColorFilter.tint(c())
                                )
                            )
                        },
                        label = { c ->
                            BasicText("Curb", style = TextStyle(color = c()))
                        }
                    )

                    FlightsTab.North -> BottomTab(
                        icon = { c ->
                            Box(
                                Modifier.paint(
                                    painterResource(R.drawable.ic_oui_north),
                                    colorFilter = ColorFilter.tint(c())
                                )
                            )
                        },
                        label = { c ->
                            BasicText("North", style = TextStyle(color = c()))
                        }
                    )

                    FlightsTab.South -> BottomTab(
                        icon = { c ->
                            Box(
                                Modifier.paint(
                                    painterResource(R.drawable.ic_oui_south),
                                    colorFilter = ColorFilter.tint(c())
                                )
                            )
                        },
                        label = { c ->
                            BasicText("South", style = TextStyle(color = c()))
                        }
                    )
                }
            }

            // FAB bubble with same liquid glass
            var pressed by remember { mutableStateOf(false) }
            val sx by animateFloatAsState(
                targetValue = if (pressed) 1f else 0f,
                animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
                label = "fab_sx"
            )
            val sy by animateFloatAsState(
                targetValue = if (pressed) 1f else 0f,
                animationSpec = spring(dampingRatio = 0.5f, stiffness = 600f),
                label = "fab_sy"
            )

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                pressed = true
                                val released = try {
                                    tryAwaitRelease()
                                } finally {
                                    pressed = false
                                }
                                if (released) {
                                    tick()
                                    onFullScreen()
                                }
                            }
                        )
                    }
                    .drawWithContent {
                        translate(0f, lerp(0f, 4.dp.toPx(), sy)) {
                            this@drawWithContent.drawContent()
                        }
                    }
                    .graphicsLayer {
                        scaleX = lerp(1f, 0.9f, sx)
                        scaleY = lerp(1f, 0.9f, sy)
                    }
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { CircleShape },
                        effects = {
                            vibrancy()
                            blur(SoftGlassTheme.blurRadius.toPx())
                            lens(
                                SoftGlassTheme.lensInner.toPx(),
                                SoftGlassTheme.lensOuter.toPx()
                            )
                        },
                        onDrawSurface = {
                            // stronger tint overlay — visually more vibrant
                            drawRect(
                                color = tint.copy(alpha = 0.25f),
                                blendMode = BlendMode.Hue
                            )

                            // a second semi-transparent overlay for depth
                            drawRect(
                                color = tint.copy(alpha = 0.12f)
                            )

                            // faint milky layer for diffusion
                            drawRect(
                                color = surfaceColor.copy(alpha = 0.18f)
                            )
                        }

                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier.paint(
                        painterResource(R.drawable.fullscreen_24dp_46152f_fill1_wght400_grad0_opsz24),
                        colorFilter = ColorFilter.tint(Color.Black)
                    )
                )
            }
        }
    }
}

@Preview
@Composable
fun FlightsGlassScreenPreview() {
    FlightsGlassScreen(onTabChanged = {}, onFullScreen = {}, onBack = {}, onMenu = {}, onOpenCard = {})
}

@Preview
@Composable
fun GlassButtonsGridPreview() {
    val backdrop: LayerBackdrop = rememberLayerBackdrop {}
    GlassButtonsGrid(
        backdrop = backdrop,
        onOpen = {}
    )
}

/**
 * bubble grid using the SAME backdrop.
 * These buttons already use drawBackdrop with blur/lens from theme.
 */
@Composable
fun GlassButtonsGrid(
    backdrop: LayerBackdrop,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val rows: List<List<GlassBtn>> = listOf(
        listOf(
            GlassBtn("card13", R.drawable.fiber_manual_record_24dp_ffffff_fill0_wght400_grad0_opsz24, "", tintIcon = false),
            GlassBtn("card14", R.drawable.fiber_manual_record_24dp_ffffff_fill0_wght400_grad0_opsz24, "", tintIcon = false),
            GlassBtn("card15", R.drawable.fiber_manual_record_24dp_ffffff_fill0_wght400_grad0_opsz24, "", tintIcon = false),
            GlassBtn("card16", R.drawable.fiber_manual_record_24dp_ffffff_fill0_wght400_grad0_opsz24, "", tintIcon = false)
        ),
        listOf(
            GlassBtn("card9",  R.drawable.account_circle_24dp_ffffff_fill1_profile, "My Profile", tintIcon = false),
            GlassBtn("card10", R.drawable.fiber_manual_record_24dp_ffffff_fill0_wght400_grad0_opsz24, "", tintIcon = false),
            GlassBtn("card11", R.drawable.fiber_manual_record_24dp_ffffff_fill0_wght400_grad0_opsz24, "", tintIcon = false),
            GlassBtn("card12", R.drawable.fiber_manual_record_24dp_ffffff_fill0_wght400_grad0_opsz24, "", tintIcon = false)
        ),
        listOf(
            GlassBtn("card5", R.drawable.ic_oui_qr_code, "QR Code", tintIcon = false),
            GlassBtn("card6", R.drawable.settings_account_box_24dp_ffffff_fill1_wght400_grad0_opsz24, "Settings", tintIcon = false),
            GlassBtn("card7", R.drawable.groups_2_24dp_ffffff_fill1_wght400_grad0_opsz24, "Contacts", tintIcon = false),
            GlassBtn("card8", R.drawable.book_24dp_ffffff_fill1_wght400_grad0_opsz24, "Notes", tintIcon = false)
        ),
        listOf(
            GlassBtn("card1", R.drawable.play_arrow_24dp_ffffff_fill1_wght400_grad0_opsz24, "Play", tintIcon = false),
            GlassBtn("card2", R.drawable.ic_oui_news, "News", tintIcon = false),
            GlassBtn("card3", R.drawable.flight_24dp_ffffff_fill0_wght400_grad0_opsz24, "Flights", tintIcon = false),
            GlassBtn("card4", R.drawable.travel_16dp_ffffff_fill0_wght400_grad0_opsz20, "Travel", tintIcon = false)
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
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { spec ->
                    LiquidButton(
                        onClick = { onOpen(spec.id) },
                        iconRes = spec.icon,
                        label = spec.label,
                        backdrop = backdrop
                    )
                }
            }
        }
    }



}
