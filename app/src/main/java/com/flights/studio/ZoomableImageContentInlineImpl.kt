@file:Suppress("COMPOSE_APPLIER_CALL_MISMATCH")

package com.flights.studio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.ortiz.touchview.TouchImageView
import kotlinx.coroutines.launch

@Composable
fun ZoomableImageContentInlineImpl(
    model: Any?,
    modifier: Modifier = Modifier,
    onImageLoadedOk: () -> Unit = {},
    onImageLoadFailed: () -> Unit = {},
    onBitmapReady: (Bitmap) -> Unit = {},
    camExpanded: Boolean? = null,                     // ✅ optional
    onCamExpandedChange: ((Boolean) -> Unit)? = null, // ✅ optional
    cornerRadiusDp: Dp = 0.dp,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val onLoadedOk by rememberUpdatedState(onImageLoadedOk)
    val onLoadFailed by rememberUpdatedState(onImageLoadFailed)
    val onBitmapReadyState by rememberUpdatedState(onBitmapReady)

    val inPreview = LocalInspectionMode.current
    val shape = RoundedCornerShape(cornerRadiusDp)

    // overall container alpha (keep at 1; we do crossfade inside the views)
    val containerAlpha = remember { Animatable(1f) }


    val holder = remember { ZoomViewHolder(context) }

    LaunchedEffect(model) {
        if (inPreview) return@LaunchedEffect

        val url = model?.toString().orEmpty()
        if (url.isBlank()) {
            holder.clearAll()
            onLoadFailed()
            return@LaunchedEffect
        }

        // Snapshot zoom matrix so we can attempt to restore it
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
                        // Put new bitmap on incoming layer (top)
                        holder.incoming.animate().cancel()
                        holder.incoming.setImageBitmap(resource)
                        holder.incoming.alpha = 0f

                        // ✅ REAL crossfade that does not require AndroidView(update)
                        holder.incoming.animate()
                            .alpha(1f)
                            .setDuration(580)
                            .setInterpolator(androidx.interpolator.view.animation.FastOutSlowInInterpolator())
                            .withEndAction {
                                // Commit bitmap to TouchImageView (gesture layer)
                                holder.tiv.setImageBitmap(resource)

                                // Restore zoom/pan best-effort
                                prevMatrix?.let { holder.tiv.imageMatrix = it }

                                // Hide incoming layer (now both are identical)
                                holder.incoming.setImageDrawable(null)
                                holder.incoming.alpha = 0f
                            }
                            .start()
                    }

                    onLoadedOk()
                    scope.launch { runCatching { onBitmapReadyState(resource) } }
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    // keep previous image; do NOT clear
                    onLoadFailed()
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    // do nothing (avoid flashes)
                }
            })
    }

    DisposableEffect(Unit) {
        onDispose {
            if (!inPreview) {
                // NOTE: we are NOT clearing via Glide targets here because these are Views,
                // so we just clear drawables to avoid leaks/flashes.
                holder.clearAll()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            // ✅ Stronger clipping for AndroidView (more reliable than Modifier.clip)
            .graphicsLayer {
                this.shape = shape
                clip = true
                alpha = containerAlpha.value
            }
    ) {

        if (inPreview) {
            Box(
                Modifier
                    .fillMaxSize()
            )
        } else {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { this.shape = shape; clip = true },
                factory = { holder.container },
                update = { /* nothing */ }
            )


            if (camExpanded != null && onCamExpandedChange != null) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(66.dp)
                        .zIndex(100f)
                        .expandCollapseGrabGesture(
                            isExpanded = camExpanded,
                            onExpandedChange = onCamExpandedChange,
                            enabled = true
                        )
                )
            }

        }

    }
}

/* -------------------------------------------------------------------------- */

private data class ZoomViewHolder(
    val container: FrameLayout,
    val tiv: TouchImageView,
    val incoming: ImageView,
    val overlay: ComposeView
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
        },
        overlay = ComposeView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            isClickable = false
            isFocusable = false
        }
    ) {
        // base gesture layer first
        container.addView(tiv)
        // incoming crossfade layer on top of it
        container.addView(incoming)
        // compose overlay on top
        container.addView(overlay)
    }

    fun clearAll() {
        tiv.setImageDrawable(null)
        incoming.setImageDrawable(null)
        incoming.alpha = 0f
    }
}
