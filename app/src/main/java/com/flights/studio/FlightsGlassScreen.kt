package com.flights.studio

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

// --- DATA MODELS ---

data class GlassBtn(
    val id: String,
    @get:DrawableRes val icon: Int,
    val label: String,
    val tintIcon: Boolean = true
)

// --- MAIN SCREEN ---

@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun FlightsGlassScreen(
    selectedTab: FlightsTab,
    onTabChanged: (FlightsTab) -> Unit,
    onFullScreen: () -> Unit,
    onBack: () -> Unit,
    onMenu: () -> Unit,
    onOpenCard: (String) -> Unit,
    showTopArea: Boolean = true,
    isInteractive: Boolean = true,
    backdropOverride: LayerBackdrop? = null,
) {
    val backdrop = backdropOverride ?: rememberLayerBackdrop { drawContent() }
    val isDark = isSystemInDarkTheme()
    val haptics = rememberHapticHelper()
    val uiTab = remember(selectedTab) { mutableStateOf(selectedTab) }
    val appliedTab = remember { mutableStateOf(uiTab.value) }

    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }
    LaunchedEffect(selectedTab) {
        uiTab.value = selectedTab
    }


    Column(modifier = Modifier.fillMaxSize()) {

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
                TopLeftPillActions(
                    backdrop = backdrop,
                    backIconRes = R.drawable.baseline_arrow_back_ios_24,
                    menuIconRes = R.drawable.more_vert_24dp_ffffff_fill1_wght400_grad0_opsz24,
                    exitIconRes = R.drawable.ic_samsung_close,
                    onMenu = { haptics.tick(); onMenu() },
                    onExit = { haptics.tick(); onBack() },

                    menuExpanded = false,          // ✅ dummy
                    onMenuDismiss = {},            // ✅ dummy
                    notesCount = 0,                // ✅ dummy
                    contactsCount = 0              // ✅ dummy
                )
            }
        }


        // ---- GRID ----
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            GlassButtonsGrid(
                backdrop = backdrop,
                onOpen = { id -> haptics.tick(); onOpenCard(id) },
                modifier = Modifier.matchParentSize()
            )
        }


        LaunchedEffect(uiTab.value) {
            if (uiTab.value == appliedTab.value) return@LaunchedEffect
            haptics.threshold()
            haptics.confirm()
            delay(140)
            appliedTab.value = uiTab.value
            onTabChanged(appliedTab.value)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 30.dp)
                .height(64.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val actionSize = 60.dp
            val gap = 12.dp

            //  PILL takes remaining width
            BottomTabs(
                tabs = FlightsTab.entries,
                selectedTabState = uiTab,
                backdrop = backdrop,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                tabIconRes = { tab ->
                    when (tab) {
                        FlightsTab.Curb -> R.drawable.ic_oui_parking
                        FlightsTab.North -> R.drawable.ic_oui_north
                        FlightsTab.South -> R.drawable.ic_oui_south
                    }
                },
                tabLabel = { it.name }
            )

            Spacer(Modifier.width(gap))

            //  FAB is separated BUT in the same row/layer as tabs
            LiquidFab(
                backdrop = backdrop,
                isDark = isDark,
                isInteractive = isInteractive,
                interactiveHighlight = interactiveHighlight,
                onClick = { haptics.tick(); onFullScreen() },
                modifier = Modifier.size(actionSize)
            )
        }
    }
}

// --- EXTRACTED COMPONENTS ---

@Composable
fun GlassButtonsGrid(
    backdrop: LayerBackdrop,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // 3x3 Grid Data
    val rows = listOf(
        listOf(
            GlassBtn("card9", R.drawable.account_circle_24dp_ffffff_fill1_profile, "Profile"),
            GlassBtn("card5", R.drawable.ic_oui_qr_code, "QR"),
            GlassBtn("card6", R.drawable.settings_account_box_24dp_ffffff_fill1_wght400_grad0_opsz24, "Settings")
        ),
        listOf(
            GlassBtn("card7", R.drawable.groups_2_24dp_ffffff_fill1_wght400_grad0_opsz24, "Contacts"),
            GlassBtn("card1", R.drawable.play_arrow_24dp_ffffff_fill1_wght400_grad0_opsz24, "Play"),
            GlassBtn("card2", R.drawable.ic_oui_news, "News")
        ),
        listOf(
            GlassBtn("card8", R.drawable.ic_oui_notes, "Notes"),
            GlassBtn("card3", R.drawable.flight_24dp_ffffff_fill0_wght400_grad0_opsz24, "Flights"),
            GlassBtn("card4", R.drawable.travel_16dp_ffffff_fill0_wght400_grad0_opsz20, "Travel")
        )
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceEvenly, // Distributes rows vertically
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        rows.forEach { row ->
            Row(

                horizontalArrangement = Arrangement.spacedBy(12.dp), // Space between buttons
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                row.forEach { btn ->
                    LiquidButton(
                        onClick = { onOpen(btn.id) },
                        iconRes = btn.icon,
                        label = btn.label,
                        backdrop = backdrop,
                        modifier = Modifier.weight(1f) // Ensures all 3 buttons share width equally
                    )
                }
            }
        }
    }
}

@Composable
fun LiquidFab(
    backdrop: LayerBackdrop,
    isDark: Boolean,
    isInteractive: Boolean,
    interactiveHighlight: InteractiveHighlight,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLightTheme = !isSystemInDarkTheme()

    val containerColor =
        if (isLightTheme) Color(0xFFFAFAFA).copy(0.4f)
        else Color(0xFF121212).copy(0.0f)
    Box(
        modifier = modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { CircleShape },
//                shadow = null,
                highlight = {
                    if (isDark) {
                        Highlight(
                            width = 0.45.dp,
                            blurRadius = 1.dp,
                            alpha = 0.50f,
                            style = HighlightStyle.Plain
                        )
                    } else {
                        Highlight(
                            width = 0.30.dp,
                            blurRadius = 1.0.dp,
                            alpha = 0.35f,
                            style = HighlightStyle.Plain // very subtle
                        )
                    }
                },
                effects = {

                    // Blur 0 = fine, lens will still work
                    blur(radius = 0f, edgeTreatment = TileMode.Clamp)

                    val cornerRadiusPx = size.height / 2f
                    val safeHeight = cornerRadiusPx * 0.55f

                    lens(
                        refractionHeight = safeHeight.coerceIn(0f, cornerRadiusPx),
                        refractionAmount = (size.minDimension * 0.80f
                                )
                            .coerceIn(0f, size.minDimension),
                        depthEffect = true,
                        chromaticAberration = false
                    )
                },
                layerBlock = if (isInteractive) {
                    {
                        val width = size.width
                        val height = size.height

                        val press = interactiveHighlight.pressProgress
                        val zoomAmountPx = 3.5.dp.toPx()
                        val baseScale = lerp(1f, 1f + zoomAmountPx / size.height, press)

                        val maxOffset = size.minDimension
                        val k = 0.025f
                        val offset = interactiveHighlight.offset

                        val maxDragScale = 4.5.dp.toPx() / size.height
                        val ang = atan2(offset.y, offset.x)

                        val pressDragScaleX =
                            baseScale +
                                    maxDragScale *
                                    abs(cos(ang) * offset.x / size.maxDimension) *
                                    (width / height).fastCoerceAtMost(1f)

                        val pressDragScaleY =
                            baseScale +
                                    maxDragScale *
                                    abs(sin(ang) * offset.y / size.maxDimension) *
                                    (height / width).fastCoerceAtMost(1f)

                        translationX = maxOffset * tanh(k * offset.x / maxOffset)
                        translationY = maxOffset * tanh(k * offset.y / maxOffset)

                        scaleX = pressDragScaleX
                        scaleY = pressDragScaleY
                    }
                } else null,
                onDrawSurface = { drawRect(containerColor) }

            )
            .then(if (isInteractive) interactiveHighlight.modifier else Modifier)
            .then(if (isInteractive) interactiveHighlight.gestureModifier else Modifier)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // ✅ EXACT SAME as BottomTabs
        val labelColor = if (isDark) Color.White else Color(0xFF111111)
        val iconColorFilter = remember(labelColor) { ColorFilter.tint(labelColor) }

        Box(
            Modifier
                .size(28.dp) // same as tabs icon size (tabs uses 28.dp.us(ui); keep 28.dp or use your scale)
                .paint(
                    painter = painterResource(R.drawable.fullscreen_24dp_46152f_fill1_wght400_grad0_opsz24),
                    colorFilter = iconColorFilter
                )
        )
    }

}

// --- HELPERS ---

@Composable
fun rememberHapticHelper(): HapticHelper {
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    return remember(view, haptic) { HapticHelper(view, haptic) }
}

class HapticHelper(
    private val view: android.view.View,
    private val feedback: HapticFeedback
) {
    fun tick() {
        if (!view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)) {
            feedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
    @RequiresApi(Build.VERSION_CODES.R)
    fun confirm() {
        val constant = HapticFeedbackConstants.CONFIRM
        if (!view.performHapticFeedback(constant)) {
            feedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
    fun threshold() {
        val act = if (Build.VERSION.SDK_INT >= 34) 34 else HapticFeedbackConstants.CLOCK_TICK
        if (!view.performHapticFeedback(act)) {
            feedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
}

