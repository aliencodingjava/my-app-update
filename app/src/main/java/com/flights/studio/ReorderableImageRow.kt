package com.flights.studio

import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlin.math.abs
import kotlin.math.hypot


@Composable
fun ReorderableImageRow(
    images: MutableList<Uri>,
    onRemove: (Uri) -> Unit,
    onOpenPreview: (Uri) -> Unit,
    ) {
    val haptics = LocalHapticFeedback.current

    val itemSize = 120.dp
    val gap = 10.dp
    val shape = RoundedCornerShape(14.dp)

    val listState = rememberLazyListState()

    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val fromIndex = from.index
        val toIndex = to.index
        if (fromIndex == toIndex) return@rememberReorderableLazyListState
        if (fromIndex !in images.indices || toIndex !in images.indices) return@rememberReorderableLazyListState

        val moved = images.removeAt(fromIndex)
        images.add(toIndex, moved)

        haptics.performHapticFeedback(
            androidx.compose.ui.hapticfeedback.HapticFeedbackType.SegmentFrequentTick
        )
    }


    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(gap),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp)
    ) {
        items(
            items = images,
            key = { it.toString() }
        ) { uri ->

            ReorderableItem(
                state = reorderState,
                key = uri.toString()
            ) { isDragging: Boolean ->

                var lastX by remember(uri) { mutableFloatStateOf(0f) }
                var dragDir by remember(uri) { mutableIntStateOf(0) } // -1 left, +1 right, 0 unknown


                val scale by animateFloatAsState(
                    targetValue = if (isDragging) 1.06f else 1f,
                    label = "scale"
                )
                val targetRotation = when {
                    !isDragging -> 0f
                    dragDir > 0 -> 2.0f
                    dragDir < 0 -> -2.0f
                    else -> 0f
                }
                val rotation by animateFloatAsState(
                    targetValue = targetRotation,
                    animationSpec = spring(
                        dampingRatio = 0.50f,
                        stiffness = 200f
                    ),
                    label = "rotation"
                )


                val dragPhotoAlpha by animateFloatAsState(
                    targetValue = if (isDragging) 0.55f else 1f,
                    animationSpec = spring(dampingRatio = 0.9f, stiffness = 450f),
                    label = "dragPhotoAlpha"
                )


                Surface(
                    shape = shape,
                    color = Color.Transparent,
                    shadowElevation = if (isDragging) 10.dp else 0.dp,
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = if (isDragging) 0.55f else 0.22f)
                    ),
                    modifier = Modifier
                        .size(itemSize)
                        .pointerInput(isDragging) {
                            if (!isDragging) return@pointerInput

                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                lastX = down.position.x

                                while (true) {
                                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (!change.pressed) break

                                    val dx = change.position.x - lastX
                                    if (abs(dx) > 0.5f) dragDir = if (dx > 0f) 1 else -1
                                    lastX = change.position.x
                                }
                            }
                        }
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            rotationZ = rotation
                        }
                        .clip(shape)
                ) {
                    // ✅ backdrop that will contain ONLY the photo
                    val photoBackdrop = rememberLayerBackdrop()

                    Box(Modifier.fillMaxSize()) {

                        // 1) PHOTO -> exported as backdrop source
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .layerBackdrop(photoBackdrop)              // ✅ capture photo into backdrop
                                .graphicsLayer { alpha = dragPhotoAlpha }
                                .pointerInput(uri) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        val start = down.position
                                        val slop = viewConfiguration.touchSlop

                                        var movedTooMuch = false

                                        val finishedBeforeTimeout = withTimeoutOrNull(700L) {
                                            while (true) {
                                                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                                val change = event.changes.firstOrNull { it.id == down.id }
                                                    ?: return@withTimeoutOrNull true

                                                if (!change.pressed) return@withTimeoutOrNull true

                                                if (change.positionChanged()) {
                                                    val dx = change.position.x - start.x
                                                    val dy = change.position.y - start.y
                                                    if (hypot(dx, dy) > slop) {
                                                        movedTooMuch = true
                                                        return@withTimeoutOrNull true
                                                    }
                                                }
                                            }
                                        }

                                        if (finishedBeforeTimeout == null && !movedTooMuch) {
                                            onOpenPreview(uri)
                                            waitForUpOrCancellation()
                                        }
                                    }
                                }
                        )
                        val isDark = isSystemInDarkTheme()
                        // 2) GLASS overlay that samples the PHOTO backdrop
                        Box(
                            Modifier
                                .fillMaxSize()
                                .drawBackdrop(
                                    backdrop = photoBackdrop,
                                    shape = { shape },
                                    shadow = null,
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
                                        // 1) Makes it “glassy”
                                        vibrancy()
                                        // 2) Frost / softness
                                        blur(0.dp.toPx())
                                        // 3) Refraction
                                        lens(
                                            refractionHeight = 8.dp.toPx(),
                                            refractionAmount = 8.dp.toPx(),
                                            depthEffect = true,
                                            chromaticAberration = false
                                        )
                                        // 4) Final tone tuning
                                        colorControls(
                                            brightness = 0.02f, // tiny lift
                                            contrast = 1.15f,   // slightly punchy
                                            saturation = 1.16f  // a bit richer
                                        )
                                    }
                                )
                                .clip(shape)
                        )

                        // drag handle icon
                        androidx.compose.material3.IconButton(
                            onClick = {},
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(4.dp)
                                .size(28.dp)
                                .pointerInput(isDragging, uri) {
                                    if (!isDragging) return@pointerInput

                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent(pass = PointerEventPass.Final)
                                            val change = event.changes.firstOrNull() ?: break
                                            if (!change.pressed) break

                                            val dx = change.position.x - change.previousPosition.x
                                            if (abs(dx) > 0.5f) dragDir = if (dx > 0f) 1 else -1
                                        }
                                    }
                                }
                                .draggableHandle(
                                    onDragStarted = {
                                        dragDir = 0
                                        haptics.performHapticFeedback(
                                            androidx.compose.ui.hapticfeedback.HapticFeedbackType.GestureThresholdActivate
                                        )
                                    },
                                    onDragStopped = {
                                        dragDir = 0
                                        haptics.performHapticFeedback(
                                            androidx.compose.ui.hapticfeedback.HapticFeedbackType.GestureEnd
                                        )
                                    }
                                ),
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.70f),
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DragHandle,
                                contentDescription = "Reorder",
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // delete X
                        FilledTonalIconButton(
                            onClick = { onRemove(uri) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(28.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.70f),
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Remove",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

            }
        }
    }
}


