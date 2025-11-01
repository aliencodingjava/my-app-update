package com.flights.studio

import android.graphics.drawable.Drawable
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.ortiz.touchview.TouchImageView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Defines the possible states for the zoomable image component.
 */
private sealed class ImageLoadState {
    object Idle : ImageLoadState()
    object Loading : ImageLoadState()
    object Success : ImageLoadState()
    data class Error(val isTimeoutExpired: Boolean = false) : ImageLoadState()
}

@Composable
fun ZoomableImageContentInlineImpl(
    model: Any?,
    modifier: Modifier = Modifier,
    onImageLoadedOk: () -> Unit = {},
    onImageLoadFailed: () -> Unit = {},
    overlayContent: (@Composable BoxScope.() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var state by remember { mutableStateOf<ImageLoadState>(ImageLoadState.Idle) }
    var lastLoadedUrl by remember { mutableStateOf<String?>(null) }

    // Holder for the Android Views, remembered across recompositions.
    val viewHolder = remember {
        val container = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        val tiv = TouchImageView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        val overlay = ComposeView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            isClickable = false
            isFocusable = false
        }
        container.addView(tiv)
        container.addView(overlay)

        object {
            val container: FrameLayout = container
            val tiv: TouchImageView = tiv
            val overlay: ComposeView = overlay
        }
    }

    // A single effect to handle all loading logic when the model changes.
    LaunchedEffect(model) {
        val url = model?.toString()

        if (url.isNullOrBlank()) {
            state = ImageLoadState.Error()
            viewHolder.tiv.setImageDrawable(null)
            lastLoadedUrl = null
            onImageLoadFailed()
            coroutineScope.launch {
                delay(5_000)
                if (state is ImageLoadState.Error) {
                    state = ImageLoadState.Error(isTimeoutExpired = true)
                }
            }
            return@LaunchedEffect
        }

        if (lastLoadedUrl == url && viewHolder.tiv.drawable != null && state is ImageLoadState.Success) {
            onImageLoadedOk()
            return@LaunchedEffect
        }

        state = ImageLoadState.Loading
        val prevMatrix = if (viewHolder.tiv.drawable != null) viewHolder.tiv.imageMatrix else null

        Glide.with(context)
            .asDrawable()
            .load(url)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .into(object : CustomTarget<Drawable>() {
                override fun onResourceReady(
                    resource: Drawable,
                    transition: Transition<in Drawable>?
                ) {
                    state = ImageLoadState.Success
                    viewHolder.tiv.setImageDrawable(resource)
                    if (prevMatrix != null) {
                        viewHolder.tiv.imageMatrix = prevMatrix
                    }
                    lastLoadedUrl = url
                    onImageLoadedOk()
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    state = ImageLoadState.Error()
                    viewHolder.tiv.setImageDrawable(null)
                    lastLoadedUrl = null
                    onImageLoadFailed()
                    coroutineScope.launch {
                        delay(5_000)
                        if (state is ImageLoadState.Error) {
                            state = ImageLoadState.Error(isTimeoutExpired = true)
                        }
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    viewHolder.tiv.setImageDrawable(null)
                }
            })
    }

    DisposableEffect(Unit) {
        onDispose {
            if (viewHolder.container.isAttachedToWindow) {
                Glide.with(context).clear(viewHolder.tiv)
            }
        }
    }

    AndroidView(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp)),
        factory = { viewHolder.container },
        update = {
            viewHolder.overlay.setContent {
                ZoomableImageOverlay(

                    overlayContent = overlayContent
                )
            }
        }
    )
}


/**
 * A private, stateless composable to render the overlay UI.
 */
@Composable
private fun ZoomableImageOverlay(

    overlayContent: (@Composable BoxScope.() -> Unit)?
) {
    Box(Modifier.fillMaxSize()) {

        overlayContent?.invoke(this)
    }
}
