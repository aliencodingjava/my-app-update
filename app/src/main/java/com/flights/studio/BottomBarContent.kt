package com.flights.studio


import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.refraction
import com.kyant.backdrop.effects.saturation
import com.kyant.backdrop.highlight.onDrawSurfaceWithHighlight
import com.kyant.backdrop.rememberLayerBackdrop
import com.kyant.backdrop.shadow.backdropShadow

@Composable
fun FlightsGlassScreen(
    onTabChanged: (FlightsTab) -> Unit,
    onFullScreen: () -> Unit,
    onBack: () -> Unit,
    onMenu: () -> Unit
) {
    val background = Color.Transparent
    val backdrop = rememberLayerBackdrop(background)


// --- HAPTICS SETUP ---
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
    fun thresholdActivate() { // used when crossing to a new tab
        val act = if (Build.VERSION.SDK_INT >= 34)
            android.view.HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE
        else android.view.HapticFeedbackConstants.CLOCK_TICK
        if (!view.performHapticFeedback(act)) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }


    Box(
        Modifier.fillMaxSize()
    ) {        // === Content layer that provides the “liquid” for everything above ===
        Box(
            Modifier
                .backdrop(backdrop)
                .fillMaxSize()
        ) {
            // === Content === will draw xml instead of this
            // SongsContent()
        }

        // === Top bar (left/right round glass buttons) ===
        Row(
            Modifier
                .padding(horizontal = 16.dp)
                .safeDrawingPadding()
                .height(56.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .backdropShadow(CircleShape)
                    .drawBackdrop(backdrop) {
                        shape = CircleShape
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            saturation()
                            blur(2f.dp)
                            refraction(height = 8f.dp.toPx(), amount = size.minDimension)
                        }
                        onDrawSurfaceWithHighlight { drawRect(background.copy(alpha = 0.5f)) }
                    }
                    .clickable { tick(); onBack() }
                    .size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier.paint(
                        painterResource(R.drawable.ic_oui_arrow_to_left),
                        colorFilter = ColorFilter.tint(Color.White)
                    )
                )
            }

            Box(
                Modifier
                    .backdropShadow(CircleShape)
                    .drawBackdrop(backdrop) {
                        shape = CircleShape
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            saturation()
                            blur(2f.dp)
                            refraction(height = 8f.dp.toPx(), amount = size.minDimension)
                        }
                        onDrawSurfaceWithHighlight { drawRect(background.copy(alpha = 0.5f)) }
                    }
                    .clickable { tick(); onMenu() }
                    .size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier.paint(
                        painterResource(R.drawable.more_vert_24dp_ffffff_fill1_wght400_grad0_opsz24),
                        colorFilter = ColorFilter.tint(Color.White)
                    )
                )
            }
        }

        // === Bottom bars (rail + tabs + action) ===
        Column(
            Modifier
                .padding(32.dp, 8.dp)
                .safeDrawingPadding()
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // translucent rail behind the tabs (matches the lib)
//              Box(
//                Modifier
//                    .backdropShadow(CircleShape)
//                    .drawBackdrop(backdrop) {
//                        shape = CircleShape
//                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//                            saturation()
//                            blur(2f.dp)
//                            refraction(height = size.minDimension / 4f, amount = size.minDimension / 2f)
//                        }
//                        drawHighlight()
//                    }
//                    .height(56.dp)
//                    .fillMaxWidth()
//            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
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
                    background = background,
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
                            label = { c -> BasicText("Curb", color = c) }
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
                            label = { c -> BasicText("North", color = c) }
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
                            label = { c -> BasicText("South", color = c) }
                        )
                    }
                }

                // round yellow action button (fullscreen)
               // round yellow action button (fullscreen) — with the same press animation
                run {
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
                        Modifier
                            .size(64.dp)                    // keep size first
                            .backdropShadow(CircleShape)
                            // 🔘 press handling identical to the cards
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        pressed = true
                                        val released = try { tryAwaitRelease() } finally { pressed = false }
                                        if (released) {
                                            tick()          // your existing haptic helper
                                            onFullScreen()
                                        }
                                    }
                                )
                            }
                            // lift the WHOLE circle
                            .drawWithContent {
                                translate(0f, lerp(0f, 4.dp.toPx(), sy)) { this@drawWithContent.drawContent() }
                            }
                            // scale the WHOLE circle
                            .graphicsLayer {
                                scaleX = lerp(1f, 0.9f, sx)
                                scaleY = lerp(1f, 0.9f, sy)
                            }
                            // glass drawing (unchanged)
                            .drawBackdrop(backdrop) {
                                shape = CircleShape
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    saturation()
                                    blur(2f.dp)
                                    refraction(height = 8f.dp.toPx(), amount = size.minDimension)
                                }
                                onDrawSurfaceWithHighlight(
                                    width = 2f.dp.toPx(),
                                    color = Color(0xFFFFF59D),
                                    blendMode = BlendMode.Overlay
                                ) {
                                    drawRect(Color(0xFFFDD835).copy(alpha = 0.8f))
                                }
                            },
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
    }
}

@Preview
@Composable
fun FlightsGlassScreenPreview() {
    FlightsGlassScreen(onTabChanged = {}, onFullScreen = {}, onBack = {}, onMenu = {})
}

