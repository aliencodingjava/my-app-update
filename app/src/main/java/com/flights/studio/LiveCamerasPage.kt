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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.capsule.ContinuousCapsule
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh
import kotlin.time.Duration.Companion.milliseconds


// ---------- MAIN SCREEN ----------
@Composable
fun LiveCamerasPage(
    cards: List<CameraCard>,
    onClose: () -> Unit,
    onOpenArchive: () -> Unit = {}
) {
    var fullscreenTitle by rememberSaveable { mutableStateOf<String?>(null) }
    BackHandler(enabled = true) {
        if (fullscreenTitle != null) {
            fullscreenTitle = null
        } else {
            onClose()
        }
    }
    var refreshToken by rememberSaveable {
        mutableLongStateOf(initialLiveCameraRefreshToken(cards))
    }
    var stack by remember {
        mutableStateOf(cards.take(3).map { it.withLiveRefreshToken(refreshToken) })
    }
    var countdownMs by rememberSaveable { mutableLongStateOf(LIVE_CAMERA_REFRESH_INTERVAL_MS) }
    var isRefreshing by rememberSaveable { mutableStateOf(false) }
    val isDark = isSystemInDarkTheme()
    val appPalette = LocalAppThemePalette.current
    val roles = appThemeSurfaceRoles(appPalette, isDark)
    val pageBg = roles.page
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()

    fun refreshAllCameras() {
        val token = System.currentTimeMillis()
        isRefreshing = true
        refreshToken = token
        val refreshed = stack.map { it.withLiveRefreshToken(token) }
        stack = refreshed
        countdownMs = LIVE_CAMERA_REFRESH_INTERVAL_MS
        scope.launch { LiveCameraArchiveStore.saveSnapshots(context, refreshed) }
    }

    fun refreshCamera(title: String) {
        val token = System.currentTimeMillis()
        isRefreshing = true
        val refreshed = stack.map { item ->
            if (item.title == title) item.withLiveRefreshToken(token) else item
        }
        stack = refreshed
        countdownMs = LIVE_CAMERA_REFRESH_INTERVAL_MS
        refreshed.firstOrNull { it.title == title }?.let { card ->
            scope.launch { LiveCameraArchiveStore.saveSnapshot(context, card) }
        }
    }

    LaunchedEffect(cards) {
        stack = cards.take(3).map { it.withLiveRefreshToken(refreshToken) }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000L.milliseconds)
            if (isRefreshing) {
                delay(2_000L.milliseconds)
                isRefreshing = false
            } else if (countdownMs <= 1_000L) {
                refreshAllCameras()
            } else {
                countdownMs -= 1_000L
            }
        }
    }

    val contentBackdrop = rememberLayerBackdrop {
        drawRect(pageBg)
        drawContent()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(pageBg)
    ) {

        // BACKGROUND
        ProfileBackdropImageLayer(
            modifier = Modifier.matchParentSize(),
            lightRes = R.drawable.light_grid_pattern,
            darkRes = R.drawable.dark_grid_pattern,
            imageAlpha = if (isDark) 0.72f else 0.42f,
            scrimDark = 0.04f,
            scrimLight = 0f
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            roles.page.copy(alpha = if (isDark) 0.58f else 0.34f),
                            roles.glassCard.copy(alpha = if (isDark) 0.42f else 0.30f),
                            appPalette.surfaceVariant.copy(alpha = if (isDark) 0.32f else 0.22f)
                        ),
                        start = Offset.Zero,
                        end = Offset(900f, 1350f)
                    )
                )
        )


        val lazyListState = rememberLazyListState()

        val reorderableState = rememberReorderableLazyListState(
            lazyListState
        ) { from, to ->
            stack = stack.toMutableList().apply {
                add(to.index, removeAt(from.index))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(contentBackdrop)
        ) {
            LazyColumn(
                state = lazyListState,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(
                    top = 120.dp,
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp
                ),
                modifier = Modifier.fillMaxSize()
            ) {
                items(stack, key = { it.title }) { item ->

                    ReorderableItem(reorderableState, key = item.title) { isDragging ->

                        val scale by animateFloatAsState(
                            if (isDragging) 1.02f else 1f,
                            label = ""
                        )

                        GlassCameraCard(
                            item = item,
                            isDark = isDark,
                            scale = scale,
                            isRefreshing = isRefreshing,
                            countdownMs = countdownMs,
                            onClick = {
                                refreshCamera(item.title)
                                fullscreenTitle = item.title
                            },
                            handle = {
                                Box(
                                    modifier = Modifier
                                        .draggableHandle()
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(roles.card.copy(alpha = if (isDark) 0.78f else 0.68f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = "Reorder",
                        tint = roles.title
                    )
                }
            }
                        )
                    }
                }
            }
        }

        GlassTopBar(
            backdrop = contentBackdrop,
            onBack = {
                if (fullscreenTitle != null) fullscreenTitle = null else onClose()
            },
            onOpenArchive = onOpenArchive
        )

        val current = stack.firstOrNull { it.title == fullscreenTitle }

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
                        .background(roles.page)
                ) {
                    FullscreenImageViewer(
                        title = camera.title,
                        url = camera.url,
                        isDark = isDark,
                        onClose = { fullscreenTitle = null }
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
    isRefreshing: Boolean,
    countdownMs: Long,
    onClick: () -> Unit,
    handle: @Composable BoxScope.() -> Unit
) {
    val cameraBackdrop = rememberLayerBackdrop()
    val appPalette = LocalAppThemePalette.current
    val roles = appThemeSurfaceRoles(appPalette, isDark)
    val cardShape = RoundedCornerShape(28.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(cardShape)
            .background(roles.card.copy(alpha = if (isDark) 0.44f else 0.36f), cardShape)
    ) {

        // Just image
        Box(
            Modifier
                .matchParentSize()
                .layerBackdrop(cameraBackdrop)
        ) {
            HomeGlassZoomImage(
                model = item.url,
                modifier = Modifier.matchParentSize(),
                cornerRadiusDp = 28.dp
            )
        }

        LiveCameraColorOverlay(
            backdrop = cameraBackdrop,
            isDark = isDark,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.matchParentSize()
        )
        // 3️⃣ TITLE BAND (NEW 🔥)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            roles.card.copy(alpha = if (isDark) 0.82f else 0.58f),
                            appPalette.glassOverlay.copy(alpha = if (isDark) 0.28f else 0.22f),
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

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = 10.dp)
                .graphicsLayer {
                    scaleX = 0.82f
                    scaleY = 0.82f
                    transformOrigin = TransformOrigin(0f, 1f)
                },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RefreshStatusPill(
                backdrop = cameraBackdrop,
                isRefreshing = isRefreshing,
                countdownMs = countdownMs
            )
            LiveCameraOpenButton(
                backdrop = cameraBackdrop,
                isDark = isDark,
                onClick = onClick
            )
        }

        // 4️⃣ DRAG HANDLE
        handle()
    }
}

@Composable
private fun LiveCameraOpenButton(
    backdrop: Backdrop,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val appPalette = LocalAppThemePalette.current
    val roles = appThemeSurfaceRoles(appPalette, isDark)
    val containerColor = roles.glassCard.copy(alpha = if (isDark) 0.72f else 0.52f)
    val iconColor = roles.title
    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }

    Row(
        modifier = Modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { ContinuousCapsule },
                effects = {
                    vibrancy()
                    blur(radius = 0f, edgeTreatment = TileMode.Clamp)
                    val cornerRadiusPx = size.height / 2f
                    val safeHeight = cornerRadiusPx * 0.55f
                    lens(
                        refractionHeight = safeHeight.coerceIn(0f, cornerRadiusPx),
                        refractionAmount = (size.minDimension * 0.80f)
                            .coerceIn(0f, size.minDimension),
                        depthEffect = false,
                        chromaticAberration = false
                    )
                },
                layerBlock = {
                    val width = size.width
                    val height = size.height

                    val press = interactiveHighlight.pressProgress
                    val zoomAmountPx = 1.5.dp.toPx()
                    val baseScale = androidx.compose.ui.util.lerp(
                        1f,
                        1f + zoomAmountPx / size.height,
                        press
                    )

                    val maxOffset = size.minDimension
                    val k = 0.025f
                    val offset = interactiveHighlight.offset
                    val maxDragScale = 3.0.dp.toPx() / size.height
                    val ang = atan2(offset.y, offset.x)

                    val pressDragScaleX =
                        baseScale +
                            maxDragScale *
                            abs(cos(ang) * offset.x / size.maxDimension) *
                            (width / height).fastCoerceAtMost(1f)

                    val pressDragScaleY =
                        baseScale +
                            maxDragScale *
                            abs(sin(ang) * offset.y / size.maxDimension) *
                            (height / width).fastCoerceAtMost(1f)

                    translationX = maxOffset * tanh(k * offset.x / maxOffset)
                    translationY = maxOffset * tanh(k * offset.y / maxOffset)

                    scaleX = pressDragScaleX
                    scaleY = pressDragScaleY
                },
                highlight = null,
                onDrawSurface = { drawRect(containerColor) }
            )
            .then(interactiveHighlight.modifier)
            .then(interactiveHighlight.gestureModifier)
            .clickable(
                interactionSource = null,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.fullscreen_24dp_46152f_fill1_wght400_grad0_opsz24),
            contentDescription = "Open full screen",
            tint = iconColor,
            modifier = Modifier.size(16.dp)
        )
    }
}
// ---------- TOP BAR ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BoxScope.GlassTopBar(
    backdrop: Backdrop,
    onBack: () -> Unit,
    onOpenArchive: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val appPalette = LocalAppThemePalette.current
    val roles = appThemeSurfaceRoles(appPalette, isDark)
    var menuOpen by remember { mutableStateOf(false) }

    val topBarShape = RoundedCornerShape(0.dp)
    val tint = roles.card.copy(alpha = if (isDark) 0.82f else 0.74f)
    val chromeContentColor = roles.title

    Surface(
        shape = topBarShape,
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { topBarShape },
                highlight = null,
                effects = {
                    vibrancy()
                    blur(
                        radius = 2.dp.toPx(),
                        edgeTreatment = TileMode.Mirror
                    )
                    lens(
                        refractionHeight = 4.dp.toPx(),
                        refractionAmount = 8.dp.toPx()
                    )
                },
                onDrawSurface = {
                    drawRect(tint)
                }
            )
    ) {
        CenterAlignedTopAppBar(
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBackIos, contentDescription = "Back", tint = chromeContentColor)
                }
            },
            title = {
                Text(
                    text = "Live Cameras",
                    style = MaterialTheme.typography.titleLarge,
                    color = chromeContentColor
                )
            },
            actions = {
                IconButton(onClick = { menuOpen = !menuOpen }) {
                    Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = chromeContentColor)
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = chromeContentColor,
                navigationIconContentColor = chromeContentColor,
                actionIconContentColor = chromeContentColor
            )
        )
    }

    LiveCamerasMenu(
        visible = menuOpen,
        backdrop = backdrop,
        onDismiss = { menuOpen = false },
        onOpenArchive = {
            menuOpen = false
            onOpenArchive()
        },
        modifier = Modifier
            .align(Alignment.TopEnd)
            .statusBarsPadding()
            .padding(top = 56.dp, end = 10.dp)
    )
}

@Composable
private fun LiveCamerasMenu(
    visible: Boolean,
    backdrop: Backdrop,
    onDismiss: () -> Unit,
    onOpenArchive: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val appPalette = LocalAppThemePalette.current
    val roles = appThemeSurfaceRoles(appPalette, isDark)
    val contentColor = roles.title

    BackHandler(enabled = visible) {
        onDismiss()
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(24.dp) },
                    effects = {
                        vibrancy()
                        blur(6.dp.toPx(), edgeTreatment = TileMode.Mirror)
                        lens(4.dp.toPx(), 8.dp.toPx(), depthEffect = false, chromaticAberration = false)
                    },
                    onDrawSurface = {
                        drawRect(roles.glassCard.copy(alpha = if (isDark) 0.84f else 0.78f))
                        drawRect(appPalette.action.copy(alpha = if (isDark) 0.10f else 0.08f))
                    }
                )
                .clickable(onClick = onOpenArchive),
            color = Color.Transparent,
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    Icons.Filled.PhotoLibrary,
                    contentDescription = null,
                    tint = contentColor
                )
                Text(
                    text = "Archive images saved",
                    color = contentColor,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                )
            }
        }
    }
}

// ---------- FULLSCREEN VIEWER ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FullscreenImageViewer(
    title: String,
    url: String,
    isDark: Boolean,
    onClose: () -> Unit
) {
    val appPalette = LocalAppThemePalette.current
    val roles = appThemeSurfaceRoles(appPalette, isDark)
    val shape = RoundedCornerShape(0.dp)
    val tint = roles.card.copy(alpha = if (isDark) 0.62f else 0.54f)

    Box(Modifier.fillMaxSize().background(roles.page)) {
        val fullscreenBackdrop = rememberLayerBackdrop()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(fullscreenBackdrop)
        ) {
            HomeGlassZoomImage(
                model = url,
                modifier = Modifier.fillMaxSize(),
                cornerRadiusDp = 0.dp
            )
        }

        LiveCameraColorOverlay(
            backdrop = fullscreenBackdrop,
            isDark = isDark,
            shape = RoundedCornerShape(0.dp),
            modifier = Modifier.matchParentSize(),
            useBackdrop = false
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(appPalette.glassOverlay.copy(alpha = if (isDark) 0.05f else 0.04f))
        )

        Surface(
            shape = shape,
            color = tint,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(appPalette.action.copy(alpha = if (isDark) 0.08f else 0.06f))
        ) {
            CenterAlignedTopAppBar(
                title = { Text(title, color = roles.title) },
                actions = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, null, tint = roles.title)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = roles.title,
                    actionIconContentColor = roles.title
                )
            )
        }
    }
}

@Composable
private fun LiveCameraColorOverlay(
    backdrop: Backdrop,
    isDark: Boolean,
    shape: RoundedCornerShape,
    modifier: Modifier = Modifier,
    useBackdrop: Boolean = true
) {
    val isDarkMode = isSystemInDarkTheme()
    val appPalette = LocalAppThemePalette.current
    if (!useBackdrop) {
        Box(
            modifier
                .background(
                    if (isDark) {
                        Color.Black.copy(alpha = 0.08f)
                    } else {
                        Color.White.copy(alpha = 0.12f)
                    }
                )
        )
        return
    }
    Box(
        modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            highlight = null,
            effects = {
                colorControls(
                    brightness = if (isDark) -0.02f else 0.01f,
                    contrast = 1.3f,
                    saturation = 1.7f
                )
            },
            onDrawSurface = {
                drawRect(
                    if (isDark) {
                        Color.Unspecified.copy(alpha = 0.08f)
                    } else {
                        Color.Unspecified.copy(alpha = 0.25f)
                    }
                )
                drawRect(appPalette.glassOverlay.copy(alpha = if (isDarkMode) 0.05f else 0.04f))
            }
        )
    )
}
// ---------- COLORS ----------
@Composable
private fun appTopBarBaseColor(): Color =
    if (isSystemInDarkTheme()) Color(0xFF1A1A1A) else Color(0xFFFAFAFA)

private const val LIVE_CAMERA_REFRESH_INTERVAL_MS = 60_000L

private fun CameraCard.withLiveRefreshToken(token: Long): CameraCard {
    val baseUrl = url.substringBefore("?")
    return copy(url = if (token > 0L) "$baseUrl?v=$token" else baseUrl)
}

private fun initialLiveCameraRefreshToken(cards: List<CameraCard>): Long {
    return cards.firstNotNullOfOrNull { card ->
        card.url.substringAfter("?", "")
            .split("&")
            .firstOrNull { it.startsWith("v=") }
            ?.removePrefix("v=")
            ?.toLongOrNull()
    } ?: 0L
}
