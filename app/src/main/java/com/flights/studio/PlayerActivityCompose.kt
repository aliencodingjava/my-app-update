@file:Suppress("COMPOSE_APPLIER_CALL_MISMATCH")

package com.flights.studio

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

private object JhIcons {
    val MenuClosed = R.drawable.folder_24dp_ffffff_fill1_wght400_grad0_opsz24
    val MenuOpen = R.drawable.folder_open_24dp_ffffff_fill1_wght400_grad0_opsz24
}

class IosPlayerActivity1 : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            MaterialTheme {
                IosPlayerScreen(
                    onExitToHome = { goHome() },
                    playClickSound = {
                        MediaPlayer.create(this, R.raw.hero_simple)?.apply {
                            setOnCompletionListener { release() }
                            start()
                        }
                    },
                    playPickSound = {
                        MediaPlayer.create(this, R.raw.time_click)?.apply {
                            setOnCompletionListener { release() }
                            start()
                        }
                    }
                )
            }
        }
        }

    @Suppress("DEPRECATION")
    private fun goHome() {
        val home = Intent(this, MainActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_NEW_TASK
            )
        }
        startActivity(home)
        overridePendingTransition(0, R.anim.zoom_out)
        finish()
    }
}

@OptIn(FlowPreview::class)
@Composable
private fun IosPlayerScreen(
    onExitToHome: () -> Unit,
    playClickSound: () -> Unit,
    playPickSound: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val controlsBarHeight = 60.dp
    val menuGap = 18.dp
    val controlsBottomInset = 2.dp

    var cachedPlayer by remember { mutableStateOf<YouTubePlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var isMenuVisible by remember { mutableStateOf(false) }
    var selectedVideoId by remember { mutableStateOf<String?>(null) }

    var totalDurationSeconds by remember { mutableFloatStateOf(0f) }
    var currentSecond by remember { mutableFloatStateOf(0f) }
    var sliderValue by remember { mutableFloatStateOf(0f) }
    var isUserSeeking by remember { mutableStateOf(false) }
    var suppressTimeUpdateUntil by remember { mutableLongStateOf(0L) }

    var showExitDialog by remember { mutableStateOf(false) }
    var lastMenuToggleAt by remember { mutableLongStateOf(0L) }

    var isSwitchingVideo by remember { mutableStateOf(false) }

    var pendingVideoId by remember { mutableStateOf<String?>(null) }
    val pickDebounceMs = 220L

    LaunchedEffect(Unit) {
        snapshotFlow { pendingVideoId }
            .filterNotNull()
            .distinctUntilChanged()
            .debounce(pickDebounceMs)
            .collectLatest { id ->
                if (selectedVideoId == id) return@collectLatest

                val p = cachedPlayer ?: return@collectLatest

                // ✅ keep highlight on pending selection for a moment (iOS feel)
                delay(480L) // tune 140..240

                selectedVideoId = id
                currentSecond = 0f
                sliderValue = 0f
                suppressTimeUpdateUntil = System.currentTimeMillis() + 800

                isPlaying = false
                isSwitchingVideo = true

                p.loadVideo(id, 0f)
            }

    }


    val videos = remember {
        listOf(
            "Winter Serenity (YouTube)" to "yQkBQtWB5pc",
            "Summer Serenity (YouTube)" to "xR9tE01Z8oY",
            "Sunset (YouTube)" to "isvsG6Uu9WU",
            "Grand Teton (YouTube)" to "O07ph1cZTR8",
            "Jenny Lake & Canyon" to "mmYjxQIQEsU",
            "Grand Teton Hike" to "WPo41u_-D_o",
            "Grand Teton • Nat Geo" to "LdW3jRm4B6s"
        )
    }

    BackHandler {
        if (isMenuVisible) isMenuVisible = false
        else showExitDialog = true
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(stringResource(R.string.Home)) },
            text = { Text(stringResource(R.string.exit_home_screen)) },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    onExitToHome()
                }) { Text(stringResource(R.string.yes)) }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    val duration = totalDurationSeconds.coerceAtLeast(0f)
    val safeCurrent = currentSecond.coerceIn(0f, if (duration > 0f) duration else 0f)
    val progress01 = if (duration > 0f) (safeCurrent / duration).coerceIn(0f, 1f) else 0f

    LaunchedEffect(progress01, isUserSeeking) {
        if (!isUserSeeking) sliderValue = progress01
    }

    val isDark = isSystemInDarkTheme()

    val base = if (isDark) Color(0x370B0B0D) else Color(0xFFF7F7F9)
    val sceneBackdrop: LayerBackdrop = rememberLayerBackdrop()

    val youTubeView = remember {
        YouTubePlayerView(context).apply {
            addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                override fun onReady(youTubePlayer: YouTubePlayer) {
                    cachedPlayer = youTubePlayer

                    val defaultId = "yQkBQtWB5pc"
                    selectedVideoId = defaultId
                    youTubePlayer.loadVideo(defaultId, 0f)
                    isPlaying = true

                    setBackgroundColor(android.graphics.Color.BLACK)
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)

                    youTubePlayer.addListener(object : AbstractYouTubePlayerListener() {
                        override fun onVideoDuration(youTubePlayer: YouTubePlayer, duration: Float) {
                            totalDurationSeconds = duration
                        }

                        override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                            if (totalDurationSeconds <= 0f) return
                            if (!isUserSeeking && System.currentTimeMillis() >= suppressTimeUpdateUntil) {
                                currentSecond = second
                            }
                            val percent = (second / totalDurationSeconds) * 100f
                            if (percent >= 99f && isPlaying) isPlaying = false
                        }

                        override fun onStateChange(
                            youTubePlayer: YouTubePlayer,
                            state: PlayerConstants.PlayerState
                        ) {
                            when (state) {
                                PlayerConstants.PlayerState.BUFFERING -> {
                                    pendingVideoId = null
                                    isSwitchingVideo = true
                                    isPlaying = false
                                }

                                PlayerConstants.PlayerState.PLAYING -> {
                                    isSwitchingVideo = false
                                    isPlaying = true
                                }

                                PlayerConstants.PlayerState.PAUSED,
                                PlayerConstants.PlayerState.ENDED -> {
                                    isSwitchingVideo = false
                                    isPlaying = false
                                }

                                else -> Unit
                            }
                        }


                    })
                }
            })
        }
    }

    DisposableEffect(lifecycleOwner, youTubeView) {
        lifecycleOwner.lifecycle.addObserver(youTubeView)
        onDispose { lifecycleOwner.lifecycle.removeObserver(youTubeView) }
    }

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(base)
                .layerBackdrop(sceneBackdrop)
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.999f },
                factory = { youTubeView }
            )
        }

        val menuFrac by animateFloatAsState(
            targetValue = if (isMenuVisible) 1f else 0f,
            animationSpec = tween(durationMillis = 220),
            label = "menuFrac"
        )

        val switchAlpha by animateFloatAsState(
            targetValue = if (isSwitchingVideo) 1f else 0f,
            animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing),
            label = "switchAlpha"
        )

        if (switchAlpha > 0.001f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .alpha(switchAlpha)
                    .drawBackdrop(
                        backdrop = sceneBackdrop,
                        shape = { RoundedCornerShape(0.dp) },
                        highlight = null,   // ✅ disables glass border
                        shadow = null,      // already disabled
                        effects = {
                            blur(0f.dp.toPx())
                            lens(
                                refractionHeight = 0f.dp.toPx(),
                                refractionAmount = 0f.dp.toPx(),
                                depthEffect = true
                            )
                        },
                        onDrawSurface = {
                            drawRect(Color.Black.copy(alpha = if (isDark) 0.35f else 0.18f))
                        }
                    )
            )
        }


        JhVideoControlsBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = controlsBottomInset),
            backdrop = sceneBackdrop,
            barHeight = controlsBarHeight,
            isPlaying = isPlaying,
            sliderValue = sliderValue,
            timeCurrent = formatTime(safeCurrent),
            timeTotal = formatTime(duration),
            isMenuOpen = isMenuVisible,
            onPlayPause = {
                val p = cachedPlayer ?: return@JhVideoControlsBar
                val finished = duration > 0f && sliderValue >= 0.99f
                when {
                    finished -> { p.seekTo(0f); p.play(); isPlaying = true }
                    isPlaying -> { p.pause(); isPlaying = false }
                    else -> { p.play(); isPlaying = true }
                }
            },
            onSeekStart = { isUserSeeking = true },
            onSeekChange = { v01 ->
                sliderValue = v01
                if (duration > 0f) currentSecond = v01 * duration
            },
            onSeekEnd = { v01 ->
                isUserSeeking = false
                if (duration > 0f) {
                    cachedPlayer?.seekTo(v01 * duration)
                    suppressTimeUpdateUntil = System.currentTimeMillis() + 800
                }
            },
            onToggleMenu = {
                val now = System.currentTimeMillis()
                if (now - lastMenuToggleAt < 250L) return@JhVideoControlsBar
                lastMenuToggleAt = now

                if (!isMenuVisible) playClickSound()
                isMenuVisible = !isMenuVisible
            }
        )

        if (menuFrac > 0.001f) {
            JhVideoMenuExpressive(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = controlsBarHeight + controlsBottomInset + menuGap),
                backdrop = sceneBackdrop,
                menuFrac = menuFrac,
                videos = videos,
                selectedVideoId = pendingVideoId ?: selectedVideoId,
                currentSecond = safeCurrent,
                isPlaying = isPlaying, // ✅ ADD THIS
                onTogglePlayPause = {
                    val p = cachedPlayer ?: return@JhVideoMenuExpressive
                    if (isPlaying) {
                        p.pause()
                        isPlaying = false
                    } else {
                        p.play()
                        isPlaying = true
                    }
                },
                onPick = { _, id ->
                    if (pendingVideoId == id) return@JhVideoMenuExpressive
                    pendingVideoId = id
                    playPickSound()
                },

                onDismiss = { isMenuVisible = false } // ✅ make it explicit
            )
        }

    }
}

@Composable
private fun JhVideoControlsBar(
    modifier: Modifier = Modifier,
    backdrop: Backdrop,
    barHeight: Dp,
    isPlaying: Boolean,
    sliderValue: Float,
    timeCurrent: String,
    timeTotal: String,
    isMenuOpen: Boolean,
    onPlayPause: () -> Unit,
    onSeekStart: () -> Unit,
    onSeekChange: (Float) -> Unit,
    onSeekEnd: (Float) -> Unit,
    onToggleMenu: () -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    val (tint, contrast) = glassTint(isDark)
    val luminanceHint = estimateLuminanceFromTint(tint)

    val contentColor = adaptiveGlassContentColor(
        isDark = isDark,
        backdropLuminanceHint = luminanceHint
    )
    val shape = RoundedCornerShape(22.dp)

    Box(
        modifier
            .padding(horizontal = 12.dp)
            .height(barHeight)
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                highlight = null,
                shadow = null,
                effects = {
                    colorControls(
                        brightness = if (isDark) -0.04f else 0.02f,
                        contrast = if (isDark) 1.08f else 1.04f,
                        saturation = if (isDark) 1.45f else 1.40f
                    )
                    vibrancy()
                    blur(2f.dp.toPx())
                    lens(
                        refractionHeight = 12f.dp.toPx(),
                        refractionAmount = 12f.dp.toPx(),
                        depthEffect = true
                    )
                },
                onDrawSurface = {
                    drawRect(tint)
                    contrast?.let { drawRect(it) }
                }
            )
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPlayPause) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = contentColor
                )
            }

            Spacer(Modifier.width(8.dp))

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Bottom) {
                Slider(
                    colors = SliderDefaults.colors(
                        thumbColor = contentColor,
                        activeTrackColor = contentColor,
                        inactiveTrackColor = contentColor.copy(alpha = 0.25f)
                    ),
                    value = sliderValue,
                    onValueChange = { v -> onSeekStart(); onSeekChange(v) },
                    onValueChangeFinished = { onSeekEnd(sliderValue) },
                    valueRange = 0f..1f,
                    modifier = Modifier.height(15.dp)
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(timeCurrent, style = MaterialTheme.typography.labelSmall, color = contentColor)
                    Text(timeTotal, style = MaterialTheme.typography.labelSmall, color = contentColor)
                }
            }

            Spacer(Modifier.width(8.dp))

            IconButton(onClick = onToggleMenu) {
                Icon(
                    painter = painterResource(if (isMenuOpen) JhIcons.MenuOpen else JhIcons.MenuClosed),
                    contentDescription = null,
                    tint = contentColor
                )
            }
        }
    }
}

@Composable
private fun JhVideoMenuExpressive(
    modifier: Modifier = Modifier,
    backdrop: Backdrop,
    menuFrac: Float,
    videos: List<Pair<String, String>>,
    selectedVideoId: String?,
    currentSecond: Float,
    isPlaying: Boolean,
    onTogglePlayPause: () -> Unit,
    onPick: (title: String, id: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    val (tint, contrast) = glassTint(isDark)

    val outerShape = RoundedCornerShape(22.dp)
    val itemShape = RoundedCornerShape(16.dp)

    val rowHeight = 52.dp
    val maxVisible = 6
    val menuHeight = rowHeight * minOf(maxVisible, videos.size)

    val listState = rememberLazyListState()
    // ✅ Is the list actually scrollable?
    val isScrollable by remember {
        derivedStateOf {
            listState.canScrollBackward || listState.canScrollForward
        }
    }


    LaunchedEffect(selectedVideoId) {
        val idx = videos.indexOfFirst { it.second == selectedVideoId }
        if (idx >= 0) listState.animateScrollToItem(idx)
    }

    // Tap outside to dismiss
    Box(
        Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
    )

    // ---------------- ✅ BOTTOM-ONLY elastic ----------------
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val maxStretchPx = with(density) { 140.dp.toPx() } // tune
    val resistance = 0.55f                              // tune

    // negative only (bottom rubber band)
    val stretch = remember { androidx.compose.animation.core.Animatable(0f) }

    // This works even with contentPadding
    val hasScrolledDown by remember {
        derivedStateOf { listState.canScrollBackward }
    }

    val isAtBottom by remember {
        derivedStateOf { isScrollable && hasScrolledDown && !listState.canScrollForward }
    }

    val rubberBand = remember(listState) {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {

            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val dy = available.y

                // ✅ ONLY when at bottom and user keeps dragging UP (dy < 0)
                if (dy < 0f && isAtBottom) {
                    val newValue = (stretch.value + dy * resistance)
                        .coerceIn(-maxStretchPx, 0f)

                    scope.launch { stretch.snapTo(newValue) }

                    // consume so LazyColumn doesn't move further
                    return Offset(0f, dy)
                }

                // ✅ If currently stretched (negative) and user scrolls DOWN (dy > 0),
                // release stretch first (feels natural), but do NOT block normal scrolling once released.
                if (stretch.value < 0f && dy > 0f) {
                    val newValue = (stretch.value + dy)
                        .coerceIn(-maxStretchPx, 0f)
                    scope.launch { stretch.snapTo(newValue) }
                    return Offset(0f, dy)
                }

                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (stretch.value != 0f) {
                    stretch.animateTo(
                        0f,
                        spring(dampingRatio = 0.72f, stiffness = 320f)
                    )
                }
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (stretch.value != 0f) {
                    stretch.animateTo(
                        0f,
                        spring(dampingRatio = 0.72f, stiffness = 320f)
                    )
                }
                return super.onPostFling(consumed, available)
            }
        }
    }
    // --------------------------------------------------------

    val extraHeightDp = with(density) { kotlin.math.abs(stretch.value).toDp() }

    Box(
        modifier
            .padding(horizontal = 14.dp)
            .fillMaxWidth()
            .height(menuHeight + extraHeightDp) // ✅ box stretches only when bottom overscroll
            .graphicsLayer {
                // negative stretch -> slight move UP (subtle)
                translationY = stretch.value * 0.25f

                // keep your open animation (no scale)
                alpha = menuFrac
                translationY += (1f - menuFrac) * 10f.dp.toPx()
            }
            .drawBackdrop(
                backdrop = backdrop,
                shape = { outerShape },
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
                    colorControls(
                        brightness = if (isDark) -0.03f else 0.02f,
                        contrast = if (isDark) 1.10f else 1.05f,
                        saturation = if (isDark) 1.18f else 1.12f
                    )
                    vibrancy()
                    blur(3f.dp.toPx())
                    lens(
                        refractionHeight = 10f.dp.toPx(),
                        refractionAmount = 26f.dp.toPx(),
                        depthEffect = true
                    )
                },
                onDrawSurface = {
                    drawRect(tint)
                    contrast?.let { drawRect(it) }
                }
            )
            .clip(outerShape)
    ) {
        LazyColumn(
            modifier = Modifier.nestedScroll(rubberBand), // ✅ attach to the scroll target
            state = listState,
            contentPadding = PaddingValues(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(videos, key = { it.second }) { (title, id) ->
                val selected = (id == selectedVideoId)

                val itemBg = when {
                    selected && isDark -> Color.White.copy(alpha = 0.14f)
                    selected && !isDark -> Color.Black.copy(alpha = 0.10f)
                    else -> Color.Transparent
                }




                val (t2, _) = glassTint(isDark)
                val menuTextColor = adaptiveGlassContentColor(
                    isDark = isDark,
                    backdropLuminanceHint = estimateLuminanceFromTint(t2)
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(rowHeight)
                        .clip(itemShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onPick(title, id) },
                    shape = itemShape,
                    color = itemBg,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                 ) {
                    Row(
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // ✅ Left icon: Pause ONLY for selected+playing, otherwise Play
                        val leftIcon =
                            if (selected && isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow

                        // ✅ Strong readability chip colors (works on video + glass)
                        val chipBg = when {
                            selected -> Color(0xFF2EA8FF).copy(alpha = 0.32f) // accent glass chip
                            isDark -> Color.Black.copy(alpha = 0.28f)         // dark scrim chip
                            else -> Color.White.copy(alpha = 0.45f)           // light scrim chip
                        }

                        val chipBorder = when {
                            selected -> Color.White.copy(alpha = 0.22f)
                            isDark -> Color.White.copy(alpha = 0.14f)
                            else -> Color.Black.copy(alpha = 0.12f)
                        }

                        val iconTint = when {
                            selected -> Color.White.copy(alpha = 0.96f)       // ✅ most readable
                            isDark -> Color.White.copy(alpha = 0.86f)
                            else -> Color.Black.copy(alpha = 0.74f)
                        }

                        Surface(
                            modifier = Modifier.size(34.dp), // slightly bigger touch target
                            shape = RoundedCornerShape(999.dp),
                            color = chipBg,
                            border = androidx.compose.foundation.BorderStroke(0.7.dp, chipBorder),
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp, // ✅ lift from moving video
                            onClick = { if (selected) onTogglePlayPause() },
                            enabled = selected
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = leftIcon,
                                    contentDescription = null,
                                    tint = iconTint,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(10.dp))

                        Text(
                            text = title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium,
                            color = menuTextColor,
                            modifier = Modifier.weight(1f)
                        )

                        // ✅ “Playing” pill ONLY for selected row (NOT for all)
                        if (selected) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Color.Black.copy(alpha = 0.28f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                val bands =
                                    if (isPlaying) synthBandsFromTime(currentSecond) else listOf(0f, 0f, 0f)

                                Playing15Dots(
                                    bands = bands,
                                    color = Color(0xFF2EA8FF),
                                    dot = 2.5.dp,
                                    gap = 1.5.dp,
                                )

                                Text(
                                    text = if (isPlaying) "Playing" else "Paused",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.95f)
                                )
                            }
                        }
                    }


                }
            }
        }
    }
}
/**
 * Deterministic pseudo-audio bands from time (seconds).
 * Looks “Samsung-like”, not random dancing.
 */
fun synthBandsFromTime(tSeconds: Float): List<Float> {
    val t = tSeconds.coerceAtLeast(0f)

    fun band(phase: Float, speed: Float): Float {
        val a = sin((t * speed) + phase) * 0.5f + 0.5f
        val b = sin((t * speed * 1.7f) + phase * 1.3f) * 0.5f + 0.5f
        val mix = a * 0.65f + b * 0.35f
        // add “beat gate” style pumping (still deterministic)
        val beat = (sin(t * 2.2f + phase) * 0.5f + 0.5f)
        return (mix * (0.55f + 0.45f * beat)).coerceIn(0f, 1f)
    }

    return listOf(
        band(phase = 0.0f, speed = 6.2f),
        band(phase = (PI / 2).toFloat(), speed = 7.0f),
        band(phase = (PI).toFloat(), speed = 5.6f),
    )
}


fun estimateLuminanceFromTint(tint: Color): Float {
    return (0.2126f * tint.red + 0.7152f * tint.green + 0.0722f * tint.blue)
}

@Composable
fun adaptiveGlassContentColor(
    isDark: Boolean,
    backdropLuminanceHint: Float? = null,
): Color {
    val base = if (isDark) Color.White else Color.Black
    val luma = backdropLuminanceHint ?: if (isDark) 0.2f else 0.8f

    return when {
        luma < 0.35f -> Color.White.copy(alpha = 0.95f)
        luma > 0.75f -> Color.Black.copy(alpha = 0.85f)
        else -> base.copy(alpha = 0.90f)
    }
}

@Composable
private fun glassTint(isDark: Boolean): Pair<Color, Color?> {
    return if (isDark) {
        Color.Black.copy(alpha = 0.22f) to Color.White.copy(alpha = 0.04f)
    } else {
        Color(0xFFE4E7ED).copy(alpha = 0.28f) to Color.Black.copy(alpha = 0.06f)
    }
}

private fun formatTime(seconds: Float): String {
    val totalSec = seconds.coerceAtLeast(0f).toInt()
    val minutes = totalSec / 60
    val secs = totalSec % 60
    return String.format(Locale.US, "%02d:%02d", minutes, secs)
}
