package com.flights.studio

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest

/**
 * Zoomable network image with cache-busting and clamped panning.
 */
@Composable
fun Content(
    imageUrl: String,
    modifier: Modifier = Modifier
) {
    // --- Zoom/pan state (use mutableFloatStateOf for primitives) ---
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val minScale = 1f
    val maxScale = 5f

    // We’ll clamp panning using the container size
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    fun clampOffset(o: Offset, s: Float): Offset {
        // Approx clamp based on how much larger the content is than the box.
        // Since we draw with ContentScale.Crop, the base content fills the box at scale=1.
        val maxX = (containerSize.width  * (s - 1f)) / 2f
        val maxY = (containerSize.height * (s - 1f)) / 2f
        return Offset(
            x = o.x.coerceIn(-maxX, maxX),
            y = o.y.coerceIn(-maxY, maxY)
        )
    }

    // Build a Coil request that ignores cache (like ?v=... did)
    val ctx = LocalContext.current
    val tsUrl = if (imageUrl.contains("?")) "$imageUrl&ts=${System.currentTimeMillis()}"
    else "$imageUrl?ts=${System.currentTimeMillis()}"

    val request = remember(tsUrl) {
        ImageRequest.Builder(ctx)
            .data(tsUrl)
            .diskCachePolicy(CachePolicy.DISABLED)
            .memoryCachePolicy(CachePolicy.DISABLED)
            .crossfade(true)
            .build()
    }

    // Gesture + transform modifier
    val gestureModifier = Modifier
        .fillMaxSize()
        .pointerInput(Unit) {
            detectTransformGestures { _, pan, zoom, _ ->
                val newScale = (scale * zoom).coerceIn(minScale, maxScale)

                // Re-scale existing offset so content stays under fingers
                val scaleChange = newScale / scale
                val newOffset = (offset + pan) * scaleChange

                scale = newScale
                offset = clampOffset(newOffset, scale)
            }
        }
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            translationX = offset.x
            translationY = offset.y
        }

    Box(modifier.fillMaxSize()) {
        SubcomposeAsyncImage(
            model = request,
            contentDescription = "Webcam image",
            contentScale = ContentScale.Crop,
            modifier = gestureModifier
                .fillMaxSize()
                .onSizeChanged { containerSize = it },
            loading = {
                Box(
                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            },
            success = {
                Image(
                    painter = it.painter,
                    contentDescription = "Webcam image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            },
            error = {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(R.drawable.ic_no_internet_oval),
                        contentDescription = "No internet"
                    )
                }
            }
        )
    }
}
