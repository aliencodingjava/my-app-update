@file:Suppress("DEPRECATION")

package com.flights.studio

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
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


// ---------- MAIN SCREEN ----------
@Composable
fun LiveCamerasPage(
    cards: List<CameraCard>,
    onClose: () -> Unit
) {
    var fullscreenKey by rememberSaveable { mutableStateOf<String?>(null) }
    BackHandler(enabled = true) {
        if (fullscreenKey != null) {
            fullscreenKey = null
        } else {
            onClose()
        }
    }
    var stack by remember { mutableStateOf(cards.take(3)) }
    val isDark = isSystemInDarkTheme()

    val rootBackdrop = rememberLayerBackdrop()

    Box(
        Modifier
            .fillMaxSize()
            .layerBackdrop(rootBackdrop)
    ) {

        // BACKGROUND
        ProfileBackdropImageLayer(
            modifier = Modifier.matchParentSize(),
            lightRes = R.drawable.lightgridpattern,
            darkRes = R.drawable.darkgridpattern,
            imageAlpha = if (isDark) 1f else 0.8f,
            scrimDark = 0f,
            scrimLight = 0f
        )


        val lazyListState = rememberLazyListState()

        val reorderableState = rememberReorderableLazyListState(
            lazyListState
        ) { from, to ->
            stack = stack.toMutableList().apply {
                add(to.index, removeAt(from.index))
            }
        }

        LazyColumn(
            state = lazyListState,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(
                top = 120.dp,   // height of top bar area
                start = 16.dp,
                end = 16.dp,
                bottom = 16.dp
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            items(stack, key = { it.url }) { item ->

                ReorderableItem(reorderableState, key = item.url) { isDragging ->

                    val scale by animateFloatAsState(
                        if (isDragging) 1.02f else 1f,
                        label = ""
                    )

                    GlassCameraCard(
                        item = item,
                        isDark = isDark,
                        scale = scale,
                        onClick = { fullscreenKey = item.url },
                        handle = {
                            Box(
                                modifier = Modifier
                                    .draggableHandle()
                                    .align(Alignment.TopEnd)
                                    .padding(12.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.Black.copy(alpha = 0.7f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DragHandle,
                                    contentDescription = "Reorder",
                                    tint = Color.White
                                )
                            }
                        }
                    )
                }
            }
        }

        GlassTopBar(
            onClose = {
                if (fullscreenKey != null) fullscreenKey = null else onClose()
            }
        )

        val current = stack.firstOrNull { it.url == fullscreenKey }

        AnimatedVisibility(
            visible = current != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            current?.let { camera ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(appTopBarColor())
                ) {
                    FullscreenImageViewer(
                        title = camera.title,
                        url = camera.url,
                        onClose = { fullscreenKey = null }
                    )
                }
            }
        }
    }
}

// ---------- GLASS CARD ----------
@Composable
private fun GlassCameraCard(
    item: CameraCard,
    isDark: Boolean,
    scale: Float,
    onClick: () -> Unit,
    handle: @Composable BoxScope.() -> Unit
) {
    val cameraBackdrop = rememberLayerBackdrop()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(28.dp))
            .clickable { onClick() }
    ) {

        // Just image
        Box(
            Modifier
                .matchParentSize()
                .layerBackdrop(cameraBackdrop)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.url)
                    .crossfade(true)
                    .build(),
                contentDescription = item.title,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop
            )
        }

        // 🔹 2 — GLASS OVERLAY (sample that layer)
        Box(
            Modifier
                .matchParentSize()
                .drawBackdrop(
                    backdrop = cameraBackdrop,
                    shape = { RoundedCornerShape(28.dp) },
                    effects = {
                        vibrancy()
                        blur(0f.dp.toPx())   // strong to SEE it
                        lens(8f.dp.toPx(), 8f.dp.toPx())
                        colorControls(
                            brightness = if (isDark) -0.02f else 0.01f,
                            contrast = 1.3f,
                            saturation = 1.7f
                        )
                    },
                    onDrawSurface = {
                        drawRect(
                            if (isDark)
                                Color.Unspecified.copy(alpha = 0.08f)
                            else
                                Color.Unspecified.copy(alpha = 0.25f)
                        )
                    }
                )
        )
        // 3️⃣ TITLE BAND (NEW 🔥)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.75f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .align(Alignment.TopStart)
        ) {
            Text(
                text = item.title,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
        }

        // 4️⃣ DRAG HANDLE
        handle()
    }
}
// ---------- TOP BAR ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BoxScope.GlassTopBar(
    onClose: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val topBarBackdrop = rememberLayerBackdrop()

    val topBarShape = RoundedCornerShape(
        bottomStart = 24.dp,
        bottomEnd = 24.dp
    )
    val tint = if (isDark) {
        Color(0xFF1A1A1A).copy(alpha = 0.92f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.80f)
    }

    Surface(
        shape = topBarShape,
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = topBarBackdrop,
                shape = { topBarShape },
                highlight = {
                    Highlight(
                        width = 0.50.dp,
                        blurRadius = 1.dp,
                        alpha = 0.20f,
                        style = HighlightStyle.Ambient
                    )
                },
                effects = {
                    blur(
                        radius = 1f.dp.toPx(),
                        edgeTreatment = androidx.compose.ui.graphics.TileMode.Mirror
                    )
                    lens(
                        refractionHeight = 60f,
                        refractionAmount = 80f,
                        depthEffect = true,
                        chromaticAberration = false
                    )
                },
                onDrawSurface = { drawRect(tint) }
            )
    ) {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = "Live Cameras",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            actions = {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, null)
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color.Transparent
            )
        )
    }
}

// ---------- FULLSCREEN VIEWER ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FullscreenImageViewer(
    title: String,
    url: String,
    onClose: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val topBarBackdrop = rememberLayerBackdrop()
    val shape = RoundedCornerShape(
        bottomStart = 24.dp,
        bottomEnd = 24.dp
    )

    val tint = if (isDark) {
        Color(0xFF1A1A1A).copy(alpha = 0.92f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.80f)
    }

    Box(Modifier.fillMaxSize()) {

        // ✅ IMAGE — NO backdrop here
        HomeGlassZoomImage(
            model = url,
            modifier = Modifier.fillMaxSize(),
            cornerRadiusDp = 0.dp
        )

        // ✅ GLASS TOP BAR ONLY samples backdrop
        Surface(
            shape = shape,
            color = Color.Transparent,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .drawBackdrop(
                    backdrop = topBarBackdrop,
                    shape = { shape },
                    highlight = {
                        Highlight(
                            width = 0.5.dp,
                            blurRadius = 1.dp,
                            alpha = 0.20f,
                            style = HighlightStyle.Plain
                        )
                    },
                    effects = {
                        blur(1f.dp.toPx())
                        lens(
                            refractionHeight = 60f,
                            refractionAmount = 80f,
                            depthEffect = true,
                            chromaticAberration = false
                        )
                    },
                    onDrawSurface = {
                        drawRect(tint)
                    }
                )
        ) {
            CenterAlignedTopAppBar(
                title = { Text(title) },
                actions = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, null)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    }
}
// ---------- COLORS ----------
@Composable
private fun appTopBarBaseColor(): Color =
    if (isSystemInDarkTheme()) Color(0xFF1A1A1A) else Color(0xFFFAFAFA)

@Composable
private fun appTopBarColor(alpha: Float = 0.92f): Color =
    appTopBarBaseColor().copy(alpha = alpha)