package com.flights.studio

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

@Composable
fun <T> BottomTabs(
    tabs: List<T>,
    selectedTabState: MutableState<T>,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    content: BottomTabsScope.(tab: T) -> BottomTabsScope.BottomTab
) {
    //////////////////////////
    // setup + constants
    //////////////////////////

    val scope = remember { BottomTabsScope() }

    val tabTextColor = Color.White        // normal text/icon
    val accent = Color(0xFF0091FF)        // accent glow / under pill
    val itemHighlight = Color(0x0069F0AE) // subtle bg behind selected tab in rail (alpha ~0x33)

    val density = LocalDensity.current
    val barHeight = 64.dp
    val pillHeight = 56.dp                // visual height of pill
    val padding = 4.dp
    val paddingPx = with(density) { padding.roundToPx() }
    val pillHeightPx = with(density) { pillHeight.roundToPx() }

    // backdrop of accent row
    val tabsBackdrop = rememberLayerBackdrop()

    // drag state
    val dragScope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }
    val offset = remember { Animatable(0f) }        // pill X position (left edge)
    val panelDrift = remember { Animatable(0f) }    // tiny wobble for rail/pill

    // press-to-scale state for the WHOLE rail (interactive bar press)
    val animationScope = rememberCoroutineScope()
    val progressAnimation = remember { Animatable(0f) }

    BoxWithConstraints(
        modifier
            .height(barHeight)
            .fillMaxWidth()
            // handle global press scale like in Interactive Glass Bottom Bar
            .pointerInput(animationScope) {
                val animationSpec = spring(0.5f, 300f, 0.001f)
                awaitEachGesture {
                    // press
                    awaitFirstDown()
                    animationScope.launch {
                        progressAnimation.animateTo(1f, animationSpec)
                    }

                    // release
                    waitForUpOrCancellation()
                    animationScope.launch {
                        progressAnimation.animateTo(0f, animationSpec)
                    }
                }
            }

    ) {
        //////////////////////////
        // geometry math
        //////////////////////////

        val widthNoPad =
            (constraints.maxWidth.toFloat() - paddingPx * 2f).fastCoerceAtLeast(0f)
        val tabWidth =
            if (tabs.isNotEmpty()) widthNoPad / tabs.size else 0f
        val maxX = (widthNoPad - tabWidth).fastCoerceAtLeast(0f)

        // how "active" the drag is, for subtle jelly/scaling
        val dragFraction by animateFloatAsState(
            targetValue = if (isDragging) 1f else 0f,
            animationSpec = spring(dampingRatio = 1f, stiffness = 300f),
            label = "dragFraction"
        )

        fun coverageForTab(index: Int): Float {
            val pillL = offset.value
            val pillR = offset.value + tabWidth
            val tabL = index * tabWidth
            val tabR = tabL + tabWidth
            val overlap = max(0f, min(pillR, tabR) - max(pillL, tabL))
            return (overlap / tabWidth).coerceIn(0f, 1f)
        }

        //////////////////////////
        // 1) Hidden accent row
        //////////////////////////
        Row(
            Modifier
                .alpha(0f) // invisible, just feeding tabsBackdrop
                .graphicsLayer {
                    // small drift like Kyant's wobble
                    val f =
                        (panelDrift.value / constraints.maxWidth).coerceIn(-1f, 1f)
                    val driftPx = with(density) { 4.dp.toPx() } *
                            androidx.compose.animation.core.EaseOut.transform(abs(f)) *
                            sign(f)
                    translationX = driftPx
                }
                .layerBackdrop(tabsBackdrop)
                .fillMaxSize()
                .padding(padding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                scope.content(tab).Content(
                    contentColor = { accent }, // <- accent text/icon color under pill
                    modifier = Modifier.weight(1f)
                )
            }
        }

        //////////////////////////
        // 2) Visible frosted rail
        //////////////////////////
        Row(
            Modifier
                .graphicsLayer {
                    // wobble drift so the rail + content move a hair with drag
                    val f =
                        (panelDrift.value / constraints.maxWidth).coerceIn(-1f, 1f)
                    val driftPx = with(density) { 4.dp.toPx() } *
                            androidx.compose.animation.core.EaseOut.transform(abs(f)) *
                            sign(f)
                    translationX = driftPx
                }
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { CircleShape },
                    effects = {
                        vibrancy()
                        blur(2f.dp.toPx())
                        lens(16f.dp.toPx(), 32f.dp.toPx())
                    },
                    shadow = {
                        // faint outer shadow so the bar sits above dark content
                        // keep alpha low so it doesn't look like a button
                        com.kyant.backdrop.shadow.Shadow(alpha = 0.35f)
                    },
                    innerShadow = {
                        // faint inner rim highlight = "thick" glass edge
                        com.kyant.backdrop.shadow.InnerShadow(
                            radius = 8.dp,
                            alpha = 0.2f
                        )
                    },
                    layerBlock = {
                        // this is the "press to scale" + micro inflate from drag
                        val progress = progressAnimation.value
                        val maxScale = (size.width + 16.dp.toPx()) / size.width
                        val scale = lerp(1f, maxScale, progress)

                        val dragInflate = lerp(1f, 1f + 1.dp / barHeight, dragFraction)
                        scaleX = scale * dragInflate
                        scaleY = scale * dragInflate
                    },
                    onDrawSurface = {
                        // translucent acrylic fill
                        drawRect(Color.Blue.copy(alpha = 0.12f))

                        // subtle bevel/rim highlight near the top edge so it feels like thick glass
                        drawRect(
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                0f to Color.White.copy(alpha = 0.28f),
                                0.15f to Color.White.copy(alpha = 0.10f),
                                1f to Color.Transparent
                            )
                        )
                    }

                )
                .fillMaxSize()
                .padding(padding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tab ->
                val hideAlpha = coverageForTab(index)      // how covered by pill
                val contentAlpha = 1f - hideAlpha          // fade out under pill

                // subtle halo behind active tab ONLY when not dragging
                val highlightAlpha by animateFloatAsState(
                    targetValue = if (
                        selectedTabState.value == tab && !isDragging
                    ) 1f else 0f,
                    animationSpec = spring(
                        dampingRatio = 0.8f,
                        stiffness = 200f
                    ),
                    label = "itemHighlight"
                )

                scope.content(tab).Content(
                    contentColor = { tabTextColor },
                    modifier = Modifier
                        .graphicsLayer {
                            alpha = contentAlpha
                        }
                        .clip(CircleShape)
                        .drawBehind {
                            if (highlightAlpha > 0f) {
                                drawRect(
                                    color = itemHighlight,
                                    alpha = highlightAlpha
                                )
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures {
                                if (selectedTabState.value != tab) {
                                    selectedTabState.value = tab
                                    dragScope.launch {
                                        // animate pill to this index
                                        launch {
                                            offset.animateTo(
                                                (index * tabWidth)
                                                    .fastCoerceIn(0f, maxX),
                                                spring(
                                                    dampingRatio = 0.8f,
                                                    stiffness = 200f
                                                )
                                            )
                                        }
                                        // fake short drag pulse for jelly glow
                                        launch {
                                            isDragging = true
                                            delay(200)
                                            isDragging = false
                                        }
                                    }
                                }
                            }
                        }
                        .weight(1f)
                )
            }
        }

        //////////////////////////
        // 3) Liquid pill
        //////////////////////////
        // We’ll REDUCE stretching so it looks clean:
        // - base size == 1 tabWidth x pillHeight
        // - while dragging we “jelly” it by ~10%, not 40%
        val pillStretchX by animateFloatAsState(
            targetValue = if (isDragging) 1.1f else 1f,
            animationSpec = spring(
                dampingRatio = 0.6f,
                stiffness = 300f
            ),
            label = "pillStretchX"
        )
        val pillStretchY by animateFloatAsState(
            targetValue = if (isDragging) 1.1f else 1f,
            animationSpec = spring(
                dampingRatio = 0.6f,
                stiffness = 600f
            ),
            label = "pillStretchY"
        )

        Spacer(
            Modifier
                .layout { measurable, _ ->
                    // Slot for the pill = exactly one tab wide, pillHeight tall.
                    val baseW = tabWidth.fastRoundToInt()
                    val baseH = pillHeightPx

                    // Now measure the child slightly bigger when dragging
                    val placeable = measurable.measure(
                        Constraints.fixed(
                            (baseW * pillStretchX).fastRoundToInt(),
                            (baseH * pillStretchY).fastRoundToInt()
                        )
                    )

                    // We still return baseW/baseH as layout size
                    layout(baseW, baseH) {
                        // center the stretched pill inside that slot
                        val childX = (baseW - placeable.width) / 2
                        val childY = (baseH - placeable.height) / 2
                        // plus paddingPx so pill sits inside rail padding
                        placeable.place(
                            x = childX + paddingPx,
                            y = childY + paddingPx
                        )
                    }
                }
                .graphicsLayer {
                    // move pill horizontally
                    translationX = offset.value

                    // no extra squish here; pillStretch already handled in layout
                }
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop),
                    shape = { RoundedCornerShape(50) },
                    effects = {
                        // tiny blur then lens for liquid look
                        blur(0.dp.toPx())
                        lens(
                            refractionHeight = 18.dp.toPx() *
                                    maxOf(0.4f, dragFraction),
                            refractionAmount = 46.dp.toPx() *
                                    maxOf(0.4f, dragFraction),
                            depthEffect = true,
                            chromaticAberration = true
                        )
                    },
                    shadow = {
                        // soft drop shadow when dragging to make it feel lifted
                        val a = lerp(0f, 1f, dragFraction)
                        com.kyant.backdrop.shadow.Shadow(alpha = a)
                    },
                    innerShadow = {
                        // inner rim highlight when dragging
                        val a = lerp(0f, 1f, dragFraction)
                        com.kyant.backdrop.shadow.InnerShadow(
                            radius = 8.dp * a,
                            alpha = a
                        )
                    },
                    onDrawSurface = {
                        // IMPORTANT: keep surface transparent so it feels like clear glass
                        // <- no frosty fill here
                        // we do NOT tint; we let CombinedBackdrop do the accent pop
                    }
                )
                .draggable(
                    state = rememberDraggableState { delta ->
                        dragScope.launch {
                            // move pill X
                            offset.snapTo(
                                (offset.value + delta)
                                    .fastCoerceIn(0f, maxX)
                            )
                            // rail wobble
                            panelDrift.snapTo(panelDrift.value + delta)
                        }
                    },
                    orientation = Orientation.Horizontal,
                    startDragImmediately = true,
                    onDragStarted = {
                        isDragging = true
                    },
                    onDragStopped = { velocity ->
                        isDragging = false

                        // snap to nearest tab
                        val currentIndex =
                            if (tabWidth == 0f) 0f
                            else offset.value / tabWidth

                        val targetIndex = when {
                            velocity > 0f -> ceil(currentIndex).toInt()
                            velocity < 0f -> floor(currentIndex).toInt()
                            else -> currentIndex.fastRoundToInt()
                        }.fastCoerceIn(
                            0,
                            (tabs.lastIndex).coerceAtLeast(0)
                        )

                        selectedTabState.value = tabs[targetIndex]

                        dragScope.launch {
                            launch {
                                // animate pill home
                                offset.animateTo(
                                    (targetIndex * tabWidth)
                                        .fastCoerceIn(0f, maxX),
                                    spring(
                                        dampingRatio = 0.8f,
                                        stiffness = 380f
                                    )
                                )
                            }
                            launch {
                                // settle wobble
                                panelDrift.animateTo(
                                    0f,
                                    spring(
                                        dampingRatio = 1f,
                                        stiffness = 300f
                                    )
                                )
                            }
                        }
                    }
                )
        )
    }
}
