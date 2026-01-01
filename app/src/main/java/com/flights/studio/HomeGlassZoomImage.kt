@file:Suppress("COMPOSE_APPLIER_CALL_MISMATCH")

package com.flights.studio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.ortiz.touchview.TouchImageView
import kotlinx.coroutines.launch

/**
 * ✅ TRUE crossfade (old image stays, new image fades in on top) + no flashes.
 * Works great for webcam refresh URLs.
 *
 * Use inside your card (you already clip with your cardShape).
 */
@Composable
fun HomeGlassZoomImage(
    model: Any?,
    modifier: Modifier = Modifier,
    onImageLoadedOk: () -> Unit = {},
    onImageLoadFailed: () -> Unit = {},
    onBitmapReady: (Bitmap) -> Unit = {},
    cornerRadiusDp: Dp = 20.dp, // keep 0 if your outer card already clips
) {
    ZoomableImageContentInlineImpl(
        model = model,
        modifier = modifier,
        onImageLoadedOk = onImageLoadedOk,
        onImageLoadFailed = onImageLoadFailed,
        onBitmapReady = onBitmapReady,
        cornerRadiusDp = cornerRadiusDp
    )
}

/* -------------------------------------------------------------------------- */

@Composable
fun ZoomableImageContentInlineImpl(
    model: Any?,
    modifier: Modifier = Modifier,
    onImageLoadedOk: () -> Unit = {},
    onImageLoadFailed: () -> Unit = {},
    onBitmapReady: (Bitmap) -> Unit = {},
    cornerRadiusDp: Dp = 0.dp,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Always call latest lambdas even if recomposed
    val onLoadedOk by rememberUpdatedState(onImageLoadedOk)
    val onLoadFailed by rememberUpdatedState(onImageLoadFailed)
    val onBitmapReadyState by rememberUpdatedState(onBitmapReady)

    val inPreview = LocalInspectionMode.current
    val holder = remember { ZoomViewHolder(context) }

    val shape = RoundedCornerShape(cornerRadiusDp)

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                this.shape = shape
                clip = true
            }
    ) {

        if (!inPreview) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { holder.container },
                update = { /* nothing, we drive updates from Glide */ }
            )
        }
    }

    LaunchedEffect(model) {
        if (inPreview) return@LaunchedEffect

        val url = model?.toString().orEmpty()

        // ✅ If url blank: do NOT clear (avoid flash). Just signal fail.
        if (url.isBlank()) {
            onLoadFailed()
            return@LaunchedEffect
        }

        // Snapshot current matrix to restore zoom/pan best-effort
        val prevMatrix = holder.tiv.drawable?.let { holder.tiv.imageMatrix }

        Glide.with(context)
            .asBitmap()
            .load(url)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .into(object : CustomTarget<Bitmap>() {

                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap>?
                ) {
                    scope.launch {
                        // Put new bitmap on incoming layer (top) and fade it in
                        holder.incoming.animate().cancel()
                        holder.incoming.setImageBitmap(resource)
                        holder.incoming.alpha = 0f

                        holder.incoming.animate()
                            .alpha(1f)
                            .setDuration(580)
                            .setInterpolator(androidx.interpolator.view.animation.FastOutSlowInInterpolator())
                            .withEndAction {
                                // Commit bitmap to gesture layer (base)
                                holder.tiv.setImageBitmap(resource)

                                // Restore zoom/pan best-effort
                                prevMatrix?.let { holder.tiv.imageMatrix = it }

                                // Clear incoming layer (now both identical)
                                holder.incoming.setImageDrawable(null)
                                holder.incoming.alpha = 0f

                                // ✅ Report success after crossfade fully finished
                                onLoadedOk()
                            }
                            .start()
                    }

                    // Optional bitmap callback (don’t crash if used)
                    scope.launch { runCatching { onBitmapReadyState(resource) } }
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    // ✅ Keep previous image; do NOT clear (avoid flash)
                    onLoadFailed()
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    // Do nothing to avoid flashes when target is cleared.
                }
            })
    }

    DisposableEffect(Unit) {
        onDispose {
            if (!inPreview) {
                holder.clearAll()
            }
        }
    }
}

/* -------------------------------------------------------------------------- */

private data class ZoomViewHolder(
    val container: FrameLayout,
    val tiv: TouchImageView,
    val incoming: ImageView,
) {
    constructor(context: Context) : this(
        container = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            // ✅ Ensure view hierarchy clips zoom-draw outside bounds
            clipChildren = true
            clipToPadding = true
        },
        tiv = TouchImageView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
        },
        incoming = ImageView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
            alpha = 0f
        }
    ) {
        // base gesture layer first
        container.addView(tiv)
        // incoming crossfade layer on top
        container.addView(incoming)
    }

    fun clearAll() {
        tiv.setImageDrawable(null)
        incoming.setImageDrawable(null)
        incoming.alpha = 0f
    }
}
