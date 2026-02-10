package com.flights.studio

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest

@Composable
fun ZoomableImageContent(
    model: Any?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    minScale: Float = 1f,
    maxScale: Float = 5f,
    iconSize: Int = 34,
    alpha: Float = 0.55f,
    onBaseScaleChange: (Boolean) -> Unit = {},
    onViewportChange: (scale: Float, translation: Offset) -> Unit = { _, _ -> },
    onInteractingChange: (Boolean) -> Unit = {},
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    fun notifyViewport() = onViewportChange(scale, offset)

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
        return Offset(
            x = o.x.coerceIn(-maxX, maxX),
            y = o.y.coerceIn(-maxY, maxY),
        )
    }

    // Coil request
    val ctx = LocalContext.current
    val imageRequest = remember(model) {
        ImageRequest.Builder(ctx)
            .data(model)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .crossfade(true)
            .build()
    }



    // Double-tap zoom
    val doubleTapToZoom = Modifier.pointerInput(minScale, maxScale) {
        detectTapGestures(
            onDoubleTap = {
                val target = if (scale <= minScale + 0.01f) 2f else 1f
                scale = target.coerceIn(minScale, maxScale)
                offset = Offset.Zero
                notifyBaseScaleIfNeeded(scale)
                notifyViewport()
            }
        )
    }

    // ✅ No drag-to-dismiss. One-finger pan only works when zoomed in.
    val gestureModifier = Modifier
        .fillMaxSize()
        .then(doubleTapToZoom)
        .pointerInput(minScale, maxScale) {
            awaitEachGesture {
                onInteractingChange(true)

                awaitFirstDown(requireUnconsumed = false)

                do {
                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                    val pressed = event.changes.count { it.pressed }
                    val pan = event.calculatePan()
                    val zoomChange = event.calculateZoom()

                    val isPinching = pressed >= 2 || zoomChange != 1f
                    val canPan = scale > minScale + 0.01f

                    if (isPinching || canPan) {
                        val newScale = (scale * zoomChange).coerceIn(minScale, maxScale)
                        val scaleFactor = if (scale == 0f) 1f else newScale / scale

                        val panToApply = if (canPan) pan else Offset.Zero
                        val newOffset = (offset + panToApply) * scaleFactor

                        scale = newScale
                        offset = clampOffset(newOffset, scale)

                        notifyBaseScaleIfNeeded(scale)
                        notifyViewport()

                        event.changes.forEach { if (it.pressed) it.consume() }
                    }
                } while (event.changes.any { it.pressed })

                onInteractingChange(false)
            }
        }
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            translationX = offset.x
            translationY = offset.y
        }

    Box(
        modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
    ) {
        SubcomposeAsyncImage(
            model = imageRequest,
            contentDescription = "Image",
            contentScale = contentScale,
            modifier = gestureModifier.fillMaxSize(),

            // ✅ NO placeholder while loading
            loading = {
                Box(Modifier.fillMaxSize()) // draw nothing
            },

            success = { success ->
                Image(
                    painter = success.painter,
                    contentDescription = "Image",
                    contentScale = contentScale,
                    modifier = Modifier.fillMaxSize()
                )
            },

            // ✅ keep only error drawable/surface
            error = {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                        modifier = Modifier.size(iconSize.dp)
                    )
                }
            }

        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ZoomableImageContentPreview() {
    MaterialTheme {
        ZoomableImageContent(model = "https://example.com/image.jpg")
    }
}
