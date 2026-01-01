package com.flights.studio

import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.core.graphics.drawable.toBitmap
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import kotlin.math.abs
import kotlin.math.max

@Composable
fun ZoomableImageContent(
    model: Any?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    minScale: Float = 1f,
    maxScale: Float = 5f,
    errorDrawable: Int = R.drawable.ic_no_internet_oval,
    onBaseScaleChange: (Boolean) -> Unit = {},
    onViewportChange: (scale: Float, translation: Offset) -> Unit = { _, _ -> },
    onInteractingChange: (Boolean) -> Unit = {},
    // Handoff model
    exitHandoffFraction: Float = 0.46f,                   // where parent share begins to dominate
    exitCommitFraction: Float = 0.60f,                    // release beyond → commit dismiss
    onExitProgress: (fractionOfHeight: Float) -> Unit = {}, // parent Y / height (0..1)
    onExitReleased: (commit: Boolean) -> Unit = {},       // true => parent should dismiss
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    fun notifyViewport() = onViewportChange(scale, offset)

    // Local (photo) drag visual
    val photoDragAnim = remember { Animatable(0f) }
    val photoDrag by photoDragAnim.asState()

    var dragDy by remember { mutableFloatStateOf(0f) }
    var photoTargetDy by remember { mutableFloatStateOf(0f) }
    var resetTick by remember { mutableIntStateOf(0) }

    // Stable easing distance for photo-only phase
    val halfScreen by remember(containerSize) {
        derivedStateOf {
            val h = containerSize.height.toFloat()
            max(if (h > 0f) h * 0.5f else 600f, 200f)
        }
    }

    // Keep photo animation in sync
    LaunchedEffect(photoTargetDy) { photoDragAnim.snapTo(photoTargetDy) }

    // Spring photo back when we cancel (only if we kept movement on the photo)
    LaunchedEffect(resetTick) {
        if (resetTick > 0) {
            photoDragAnim.animateTo(
                0f,
                spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)
            )
        }
    }

    // Base-scale notify
    var lastIsBase by remember { mutableStateOf(true) }
    fun notifyBaseScaleIfNeeded(current: Float) {
        val isBase = current <= (minScale + 0.01f)
        if (isBase != lastIsBase) {
            lastIsBase = isBase
            onBaseScaleChange(isBase)
        }
    }
    LaunchedEffect(Unit) { notifyBaseScaleIfNeeded(scale) }

    fun clampOffset(o: Offset, s: Float): Offset {
        val maxX = (containerSize.width * (s - 1f)) / 2f
        val maxY = (containerSize.height * (s - 1f)) / 2f
        return Offset(o.x.coerceIn(-maxX, maxX), o.y.coerceIn(-maxY, maxY))
    }

    val ctx = LocalContext.current
    val request = remember(model) {
        ImageRequest.Builder(ctx)
            .data(model)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .crossfade(true)
            .build()
    }

    // Double-tap zoom
    val doubleTapToZoom = Modifier.pointerInput(Unit) {
        detectTapGestures(
            onDoubleTap = {
                val target = if (scale <= minScale + 0.01f) 2f else 1f
                scale = target.coerceIn(minScale, maxScale)
                offset = Offset.Zero
                dragDy = 0f
                photoTargetDy = 0f
                notifyBaseScaleIfNeeded(scale)
                notifyViewport()
            }
        )
    }

    // Helper: smoothstep (0..1)
    fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    val gestureModifier = Modifier
        .fillMaxSize()
        .then(doubleTapToZoom)
        .pointerInput(minScale, maxScale) {
            awaitEachGesture {
                onInteractingChange(true)

                dragDy = 0f
                photoTargetDy = 0f
                var decidedVertical = false
                var photoMoved = false

                awaitFirstDown(requireUnconsumed = false)

                do {
                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                    val pressed = event.changes.count { it.pressed }
                    val pan = event.calculatePan()
                    val zoomChange = event.calculateZoom()

                    val isTransforming = pressed >= 2 || zoomChange != 1f || scale > minScale + 0.01f
                    if (isTransforming) {
                        val newScale = (scale * zoomChange).coerceIn(minScale, maxScale)
                        val scaleFactor = if (scale == 0f) 1f else newScale / scale
                        val newOffset = (offset + pan) * scaleFactor

                        scale = newScale
                        offset = clampOffset(newOffset, scale)
                        notifyBaseScaleIfNeeded(scale)
                        notifyViewport()
                        event.changes.forEach { if (it.pressed) it.consume() }
                    } else {
                        if (!decidedVertical && (abs(pan.x) > 6f || abs(pan.y) > 6f)) {
                            decidedVertical = abs(pan.y) > abs(pan.x)
                        }

                        if (decidedVertical && pan.y > 0f) {
                            dragDy += pan.y

                            val h = containerSize.height.toFloat().coerceAtLeast(1f)
                            val progress = (dragDy / h).coerceIn(0f, 1f)

                            // Parent share curve:
                            // - starts near 0 and ramps to ~0.35 by handoff (0.46)
                            // - blends from 0.35 to 1.0 between 0.46..0.60
                            val preMaxShare = 0.35f
                            val preShare = smoothstep(0f, exitHandoffFraction, progress) * preMaxShare
                            val postT = ((progress - exitHandoffFraction) /
                                    (exitCommitFraction - exitHandoffFraction)).coerceIn(0f, 1f)
                            val share = if (progress < exitHandoffFraction) {
                                preShare
                            } else {
                                // blend smoothly from preShare → 1
                                preShare + (1f - preShare) * postT
                            }

                            // Split the displacement
                            val parentY = dragDy * share               // goes to parent
                            val photoY = (dragDy - parentY).coerceAtLeast(0f) // stays on photo

                            // Photo visual is eased and capped
                            val easedPhotoY = if (photoY <= halfScreen) photoY
                            else halfScreen + (photoY - halfScreen) * 0.5f

                            photoTargetDy = easedPhotoY
                            photoMoved = photoMoved || easedPhotoY > 0f

                            // Send parent progress (0..1)
                            onExitProgress((parentY / h).coerceIn(0f, 1f))

                            event.changes.forEach { if (it.pressed) it.consume() }
                        }
                    }
                } while (event.changes.any { it.pressed })

                // Release → decide commit based on total progress
                val h = containerSize.height.toFloat().coerceAtLeast(1f)
                val endProgress = (dragDy / h).coerceIn(0f, 1f)
                val commit = endProgress >= exitCommitFraction
                onExitReleased(commit)

                // If we actually moved the photo portion and didn’t commit, spring photo back
                if (photoMoved && !commit) {
                    dragDy = 0f
                    photoTargetDy = 0f
                    resetTick++
                }

                onInteractingChange(false)
            }
        }
        .graphicsLayer {
            // zoom/pan
            scaleX = scale
            scaleY = scale
            translationX = offset.x

            // apply only the photo portion locally
            val applyPhoto = scale <= (minScale + 0.01f)
            translationY = offset.y + if (applyPhoto) photoDrag else 0f

            // fade based on the photo share (not the parent)
            val progress = (photoDrag / halfScreen).coerceIn(0f, 1f)
            alpha = 1f - progress
        }

    Box(
        modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
    ) {
        SubcomposeAsyncImage(
            model = request,
            contentDescription = "Image",
            contentScale = contentScale,
            modifier = gestureModifier.fillMaxSize(),
            loading = {
                Box(
                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            },
            success = { success ->
                Image(
                    painter = success.painter,
                    contentDescription = "Image",
                    contentScale = contentScale,
                    modifier = Modifier.fillMaxSize()
                )
            },
            error = {
                val dr = AppCompatResources.getDrawable(ctx, errorDrawable)
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    val bmp = dr?.toBitmap()
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Error",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Text("Failed to load image", color = Color.White)
                    }
                }
            }
        )
    }
}

@Preview
@Composable
fun ZoomableImageContentPreview() {
    ZoomableImageContent(model = "https://example.com/image.jpg")
}
