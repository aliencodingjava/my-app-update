package com.flights.studio

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.refraction
import com.kyant.backdrop.highlight.onDrawSurfaceWithHighlight
import com.kyant.backdrop.rememberLayerBackdrop
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun LiquidGlassGalleryScreen(
    imageUrls: List<String>,
    startIndex: Int = 0,
    onBack: () -> Unit,
    onClose: () -> Unit,
    bottomBar: @Composable (() -> Unit)? = null,
) {
    // α11 provider
    val bottomTabsBackdrop = rememberLayerBackdrop(null)

    // Pager
    val safeCount = imageUrls.size.coerceAtLeast(1)
    val initial = startIndex.coerceIn(0, (imageUrls.size - 1).coerceAtLeast(0))
    val pagerState = rememberPagerState(initialPage = initial, pageCount = { safeCount })
    val fling = PagerDefaults.flingBehavior(state = pagerState)

    // Drag-to-dismiss state
    var exitActive by remember { mutableStateOf(false) }
    var exitProgress by remember { mutableFloatStateOf(0f) } // 0..1 while dragging
    var rootSize by remember { mutableStateOf(IntSize.Zero) }
    val containerOffsetY = remember { androidx.compose.animation.core.Animatable(0f) }
    val scope = rememberCoroutineScope()

    // Haptics
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

    // Fixed foreground (old API feel) — readability comes from surface scrim
    val uiOnGlass = Color.White

    // Glass modifiers (α11): non-zero blur, sensible refraction, surface scrim
    val pillGlass = Modifier.drawBackdrop(bottomTabsBackdrop) {
        shape = CircleShape
        blur(2.dp)
        refraction(height = 12.dp.toPx(), amount = 56.dp.toPx())
        onDrawSurfaceWithHighlight { drawRect(Color.Black.copy(alpha = 0.24f)) }
    }
    val iconButtonGlass = Modifier.drawBackdrop(bottomTabsBackdrop) {
        shape = CircleShape
        blur(2.dp)
        refraction(height = 10.dp.toPx(), amount = 48.dp.toPx())
        onDrawSurfaceWithHighlight { drawRect(Color.Black.copy(alpha = 0.26f)) }
    }
    val bottomBarGlass = Modifier.drawBackdrop(bottomTabsBackdrop) {
        shape = RoundedCornerShape(16.dp)
        blur(2.dp)
        refraction(height = 14.dp.toPx(), amount = 64.dp.toPx())
        onDrawSurfaceWithHighlight { drawRect(Color.Black.copy(alpha = 0.22f)) }
    }

    // OUTER container translated as a whole
    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { size -> rootSize = size }
            .graphicsLayer {
                val y = if (exitActive) exitProgress * rootSize.height else containerOffsetY.value
                translationY = y
            }
    ) {
        // Provider MUST wrap the background you want to sample
        Box(
            Modifier
                .backdrop(bottomTabsBackdrop)
                .fillMaxSize()
        ) {

            HorizontalPager(
                state = pagerState,
                flingBehavior = fling,
                userScrollEnabled = !exitActive,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val index = if (imageUrls.isEmpty()) 0 else page.coerceIn(0, imageUrls.lastIndex)
                val url = imageUrls.getOrNull(index).orEmpty()

                ZoomableImageContent(
                    model = url,
                    modifier = Modifier.fillMaxSize(),
                    onBaseScaleChange = { /* pause pager if needed when zoomed */ },

                    // Parent drag handoff
                    onExitProgress = { f ->
                        exitActive = f > 0f
                        exitProgress = f
                    },
                    onExitReleased = { commit ->
                        val currentY = (exitProgress * rootSize.height).coerceAtLeast(0f)
                        scope.launch {
                            containerOffsetY.snapTo(currentY)
                            exitActive = false
                            val end = if (commit) rootSize.height.toFloat() else 0f
                            containerOffsetY.animateTo(end)
                            if (commit) onBack() else {
                                exitProgress = 0f
                                containerOffsetY.snapTo(0f)
                            }
                        }
                    },
                    exitHandoffFraction = 0.46f,
                    exitCommitFraction = 0.60f
                )
            }
        }

        // ==== TOP BAR ====
        Row(
            Modifier
                .padding(horizontal = 16.dp)
                .safeDrawingPadding()
                .height(56.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back
            Box(
                Modifier
                    .size(48.dp)
                    .then(iconButtonGlass)
                    .clickableNoRipple { tick(); onBack() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier.paint(
                        painterResource(R.drawable.ic_oui_arrow_to_left),
                        colorFilter = ColorFilter.tint(uiOnGlass)
                    )
                )
            }

            // Counter pill
            Box(
                Modifier
                    .wrapContentSize()
                    .then(pillGlass)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                val current = (pagerState.currentPage + 1)
                    .coerceAtMost(imageUrls.size.coerceAtLeast(1))
                Text(
                    text = "$current / ${imageUrls.size.coerceAtLeast(1)}",
                    color = uiOnGlass,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                )
            }

            // Close
            Box(
                Modifier
                    .size(48.dp)
                    .then(iconButtonGlass)
                    .clickableNoRipple { confirm(); onClose() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier.paint(
                        painterResource(R.drawable.ic_oui_close),
                        colorFilter = ColorFilter.tint(uiOnGlass)
                    )
                )
            }
        }

        // ==== BOTTOM BAR ====
        bottomBar?.let { content ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(
                        WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)
                    )
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .heightIn(min = 64.dp)
                    .then(bottomBarGlass)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                CompositionLocalProvider(LocalContentColor provides uiOnGlass) {
                    content()
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Preview
@Composable
private fun LiquidGlassGalleryScreenPreview() {
    LiquidGlassGalleryScreen(
        imageUrls = listOf("url1", "url2", "url3"),
        startIndex = 0,
        onBack = {},
        onClose = {},
        bottomBar = { Text("Bottom bar content") }
    )
}


/** ripple-less clickable helper */
fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    composed {
        val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
        clickable(indication = null, interactionSource = interaction, onClick = onClick)
    }
