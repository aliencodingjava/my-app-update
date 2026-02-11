@file:Suppress("COMPOSE_APPLIER_CALL_MISMATCH")

package com.flights.studio

import android.content.Context
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
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
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

// -----------------------------
// ✅ SUPABASE LIKES INTEGRATION
// -----------------------------
//
// This file assumes you already have a SupabaseClient singleton in your app.
// Replace SupabaseHolder.client with your own.
// Works with your SQL RPC:
//
//   public.toggle_video_like(p_video_id text)
//   returns table(is_liked boolean, like_count bigint)
//
// For initial state (isLiked + likeCount), we do 2 small queries per video:
// - count(*) where video_id = id
// - exists row where video_id=id AND user_id=auth.uid()
//
// If you want 1 query instead, create another RPC get_video_like_state(video_id).
//

// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
// Replace with your own Supabase singleton
// Example (you likely already have something like this in your project):
// object SupabaseHolder { lateinit var client: SupabaseClient }
// <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

private object JhIcons {
    val MenuClosed = R.drawable.folder_24dp_ffffff_fill1_wght400_grad0_opsz24
    val MenuOpen = R.drawable.folder_open_24dp_ffffff_fill1_wght400_grad0_opsz24
}

private val Context.favoritesDataStore by preferencesDataStore(name = "jh_favorites")
private val KEY_FAVORITES = stringSetPreferencesKey("favorite_video_ids")

/** ✅ Sectioned menu rows so user can SEE grouping + order */
sealed class MenuRow {
    data class Header(
        val title: String,
        val iconRes: Int? = null
    ) : MenuRow()

    data class Video(val title: String, val id: String) : MenuRow()
}

class IosPlayerActivity1 : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            MaterialTheme {
                IosPlayerScreen(
                    supabase = SupabaseManager.client, // ✅ see step 3
                    onExitToHome = { goHome() },
                    playClickSound = { playSound(R.raw.hero_simple) },
                    playPickSound = { playSound(R.raw.time_click) }
                )
            }
        }
    }

    private fun playSound(res: Int) {
        MediaPlayer.create(this, res)?.apply {
            setOnCompletionListener { release() }
            start()
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
        finish() // ✅ close this screen

        overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation)
    }

}


@OptIn(FlowPreview::class)
@Composable
private fun IosPlayerScreen(
    supabase: SupabaseClient,
    onExitToHome: () -> Unit,
    playClickSound: () -> Unit,
    playPickSound: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val controlsBarHeight = 60.dp
    val menuGap = 18.dp
    val controlsBottomInset = 2.dp
    val scope = rememberCoroutineScope()
    val likesRepo = remember(supabase) {
        LikesRepository(supabase)
    }


    // ✅ Favorites persisted
    val favoriteIds by remember {
        context.favoritesDataStore.data.map { prefs ->
            prefs[KEY_FAVORITES] ?: emptySet()
        }
    }.collectAsState(initial = emptySet())

    // ✅ Likes UI state map (videoId -> ui state)
    var likesMap by remember { mutableStateOf<Map<String, LikeUiState>>(emptyMap()) }

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

    // ✅ blocks background touches (video + empty area)
    fun Modifier.consumeAllTouches() = pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                event.changes.forEach { it.consume() }
            }
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { pendingVideoId }
            .filterNotNull()
            .distinctUntilChanged()
            .debounce(pickDebounceMs)
            .collectLatest { id ->
                if (selectedVideoId == id) return@collectLatest

                val p = cachedPlayer ?: return@collectLatest

                delay(480L) // iOS-ish pending highlight feel

                selectedVideoId = id
                currentSecond = 0f
                sliderValue = 0f
                suppressTimeUpdateUntil = System.currentTimeMillis() + 800

                isPlaying = false
                isSwitchingVideo = true

                p.loadVideo(id, 0f)
            }
    }

    /** ✅ MENU ORDER + HEADERS visible to user */
    val allRows = remember {
        listOf(
            MenuRow.Header("Jackson Hole / Welcome", iconRes = R.drawable.hand_gesture_24dp_ffffff_fill1_wght400_grad0_opsz24),
            MenuRow.Video("Winter Serenity (YouTube)", "yQkBQtWB5pc"),
            MenuRow.Video("Summer Serenity (YouTube)", "xR9tE01Z8oY"),
            MenuRow.Video("Sunset", "isvsG6Uu9WU"),
            MenuRow.Header("Jackson Hole / KJAC", iconRes = R.drawable.travel_24dp_ffffff_fill1_wght400_grad0_opsz24),
            MenuRow.Video("Jackson Hole Airport", "GO8vO7Tt3HU"),
            MenuRow.Video("Day in the Life (KJAC)", "nquyjx53h2E"),
            MenuRow.Video("Jackson Hole Airport ARFF", "V6Q-Poslsxk"),
            MenuRow.Video("JAC Virtual Tour", "ZUkFLlWDQY8"),
            MenuRow.Video("KJAC Flight Services", "kAdmU1M6qA"),
            MenuRow.Header("Grand Teton", iconRes = R.drawable.landscape_2_24dp_ffffff_fill1_wght400_grad0_opsz24),
            MenuRow.Video("Grand Teton (YouTube)", "O07ph1cZTR8"),
            MenuRow.Video("Jenny Lake & Canyon", "mmYjxQIQEsU"),
            MenuRow.Video("Grand Teton Hike", "WPo41u_-D_o"),
            MenuRow.Video("Grand Teton • Nat Geo", "LdW3jRm4B6s"),
        )
    }

    // ✅ Used for sizing / maxVisible: count only videos (not headers)
    val menuRows = remember(allRows, favoriteIds) {
        val allVideos = allRows.filterIsInstance<MenuRow.Video>()
        val favVideos = allVideos.filter { favoriteIds.contains(it.id) }

        if (favVideos.isEmpty()) {
            allRows
        } else {
            val allRowsWithoutFavVideos = allRows
                .filterNot { row -> row is MenuRow.Video && favoriteIds.contains(row.id) }
                .pruneEmptyHeaders()

            buildList {
                add(MenuRow.Header("Favorites", iconRes = dev.oneuiproject.oneui.R.drawable.ic_oui_favorite_on))
                addAll(favVideos)

                // Only show "All Videos" if there are videos left after removing favorites
                if (allRowsWithoutFavVideos.any { it is MenuRow.Video }) {
                    add(MenuRow.Header("All Videos"))
                    addAll(allRowsWithoutFavVideos)
                }
            }
        }
    }

    val videoRows = remember(menuRows) { menuRows.filterIsInstance<MenuRow.Video>() }
    val allVideoIds = remember(allRows) { allRows.filterIsInstance<MenuRow.Video>().map { it.id } }

    // ✅ Load likes when menu opens (preload for all videos)
    LaunchedEffect(isMenuVisible) {
        if (!isMenuVisible) return@LaunchedEffect

        val current = likesMap
        val missingIds = allVideoIds.filter { id -> current[id] == null }
        if (missingIds.isEmpty()) return@LaunchedEffect

        likesMap = likesMap.toMutableMap().apply {
            for (id in missingIds) put(id, LikeUiState(false, 0L, true))
        }

        for (id in missingIds) {
            try {
                val state = likesRepo.fetchLikeState(id)
                likesMap = likesMap.toMutableMap().apply { put(id, state) }
            } catch (t: Throwable) {
                t.printStackTrace() // <-- YOU NEED THIS while debugging
                likesMap = likesMap.toMutableMap().apply { put(id, LikeUiState(false, 0L, false)) }
            }
        }
    }

    BackHandler {
        if (isMenuVisible) isMenuVisible = false else showExitDialog = true
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
                TextButton(onClick = { showExitDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
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
        // --- video layer ---
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

        // ✅ touch shield ABOVE video, BELOW controls/menu
        Box(
            Modifier
                .fillMaxSize()
                .consumeAllTouches()
        )

        TopRightPillActions(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 14.dp, top = 12.dp)
                .statusBarsPadding(),
            backdrop = sceneBackdrop,
            onHome = onExitToHome,
            onSettings = {
                context.startActivity(
                    Intent(context, SettingsActivity::class.java)
                )
            }
        )

        val menuFrac by animateFloatAsState(
            targetValue = if (isMenuVisible) 1f else 0f,
            animationSpec = spring(
                dampingRatio = 0.82f, // less bounce
                stiffness = 520f      // snappy
            ),
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
                        highlight = null,
                        shadow = null,
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
                        })
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
                    finished -> {
                        p.seekTo(0f); p.play(); isPlaying = true
                    }
                    isPlaying -> {
                        p.pause(); isPlaying = false
                    }
                    else -> {
                        p.play(); isPlaying = true
                    }
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
                rows = menuRows,
                videoCount = videoRows.size,
                selectedVideoId = pendingVideoId ?: selectedVideoId,
                currentSecond = safeCurrent,
                isPlaying = isPlaying,
                favoriteIds = favoriteIds,
                likesMap = likesMap,
                onToggleFavorite = { id ->
                    scope.launch {
                        context.favoritesDataStore.edit { prefs ->
                            val cur = prefs[KEY_FAVORITES] ?: emptySet()
                            prefs[KEY_FAVORITES] = if (cur.contains(id)) cur - id else cur + id
                        }
                    }
                },
                onToggleLike = { id ->
                    scope.launch {
                        val prev = likesMap[id] ?: LikeUiState(false, 0L, false)

                        likesMap = likesMap.toMutableMap().apply { put(id, prev.copy(isLoading = true)) }

                        try {
                            val next = likesRepo.toggleLike(id)
                            likesMap = likesMap.toMutableMap().apply { put(id, next) }
                        } catch (t: Throwable) {
                            t.printStackTrace() // <-- see the REAL reason it fails (RLS/auth/etc)
                            likesMap = likesMap.toMutableMap().apply { put(id, prev.copy(isLoading = false)) }
                        }
                    }
                },
                onTogglePlayPause = {
                    val p = cachedPlayer ?: return@JhVideoMenuExpressive
                    if (isPlaying) {
                        p.pause(); isPlaying = false
                    } else {
                        p.play(); isPlaying = true
                    }
                },
                onPick = { _, id ->
                    if (pendingVideoId == id) return@JhVideoMenuExpressive
                    pendingVideoId = id
                    playPickSound()
                },
                onDismiss = { isMenuVisible = false }
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
                })
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
    rows: List<MenuRow>,
    videoCount: Int,
    selectedVideoId: String?,
    currentSecond: Float,
    isPlaying: Boolean,
    onTogglePlayPause: () -> Unit,
    onPick: (title: String, id: String) -> Unit,
    favoriteIds: Set<String>,
    likesMap: Map<String, LikeUiState>,
    onToggleFavorite: (String) -> Unit,
    onToggleLike: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    val (tint, contrast) = glassTint(isDark)

    val outerShape = RoundedCornerShape(22.dp)
    val itemShape = RoundedCornerShape(16.dp)

    val rowHeight = 52.dp
    val maxVisible = 6
    val menuHeight = rowHeight * minOf(maxVisible, videoCount)

    val listState = rememberLazyListState()
    val isScrollable by remember {
        derivedStateOf { listState.canScrollBackward || listState.canScrollForward }
    }

    // ✅ scroll to selected video row (skips headers)
    LaunchedEffect(selectedVideoId) {
        val idx = rows.indexOfFirst { it is MenuRow.Video && it.id == selectedVideoId }
        if (idx >= 0) listState.animateScrollToItem(idx)
    }

    // Tap outside to dismiss (this is ABOVE touch-shield because of composition order)
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

    val maxStretchPx = with(density) { 140.dp.toPx() }
    val resistance = 0.55f

    val stretch = remember { androidx.compose.animation.core.Animatable(0f) }

    val hasScrolledDown by remember { derivedStateOf { listState.canScrollBackward } }
    val isAtBottom by remember { derivedStateOf { isScrollable && hasScrolledDown && !listState.canScrollForward } }

    val rubberBand = remember(listState) {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val dy = available.y

                if (dy < 0f && isAtBottom) {
                    val newValue = (stretch.value + dy * resistance).coerceIn(-maxStretchPx, 0f)
                    scope.launch { stretch.snapTo(newValue) }
                    return Offset(0f, dy)
                }

                if (stretch.value < 0f && dy > 0f) {
                    val newValue = (stretch.value + dy).coerceIn(-maxStretchPx, 0f)
                    scope.launch { stretch.snapTo(newValue) }
                    return Offset(0f, dy)
                }

                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (stretch.value != 0f) {
                    stretch.animateTo(0f, spring(dampingRatio = 0.72f, stiffness = 320f))
                }
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (stretch.value != 0f) {
                    stretch.animateTo(0f, spring(dampingRatio = 0.72f, stiffness = 320f))
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
            .height(menuHeight + extraHeightDp)
            .graphicsLayer {
                translationY = stretch.value * 0.25f
                alpha = menuFrac
                translationY += (1f - menuFrac) * 6f.dp.toPx()
            }
            .drawBackdrop(backdrop = backdrop, shape = { outerShape }, shadow = null, highlight = {
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
                        style = HighlightStyle.Plain
                    )
                }
            }, effects = {
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
            }, onDrawSurface = {
                drawRect(tint)
                contrast?.let { drawRect(it) }
            })
            .clip(outerShape)
    ) {
        LazyColumn(
            modifier = Modifier.nestedScroll(rubberBand),
            state = listState,
            contentPadding = PaddingValues(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(
                items = rows,
                key = { index, item ->
                    when (item) {
                        is MenuRow.Header -> "h:${item.title}:$index"
                        is MenuRow.Video -> "v:${item.id}:$index" // ✅ unique even if repeated
                    }
                }
            ) { _, row ->
                when (row) {
                    is MenuRow.Header -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            row.iconRes?.let { res ->
                                Icon(
                                    painter = painterResource(res),
                                    contentDescription = null,
                                    tint = if (isDark) Color.White.copy(alpha = 0.70f) else Color.Black.copy(alpha = 0.65f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Text(
                                text = row.title,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isDark) Color.White.copy(alpha = 0.62f) else Color.Black.copy(alpha = 0.60f),
                            )
                        }
                    }

                    is MenuRow.Video -> {
                        val title = row.title
                        val id = row.id
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

                        val isFav = favoriteIds.contains(id)
                        val likeState = likesMap[id] ?: LikeUiState(isLiked = false, likeCount = 0L, isLoading = false)



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
                                    .padding(start = 12.dp, end = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val leftIcon =
                                    if (selected && isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow

                                val chipBg = when {
                                    selected -> Color(0xFF2EA8FF).copy(alpha = 0.32f)
                                    isDark -> Color.Black.copy(alpha = 0.28f)
                                    else -> Color.White.copy(alpha = 0.45f)
                                }

                                val chipBorder = when {
                                    selected -> Color.White.copy(alpha = 0.22f)
                                    isDark -> Color.White.copy(alpha = 0.14f)
                                    else -> Color.Black.copy(alpha = 0.12f)
                                }

                                val iconTint = when {
                                    selected -> Color.White.copy(alpha = 0.96f)
                                    isDark -> Color.White.copy(alpha = 0.86f)
                                    else -> Color.Black.copy(alpha = 0.74f)
                                }

                                Surface(
                                    modifier = Modifier.size(34.dp),
                                    shape = RoundedCornerShape(999.dp),
                                    color = chipBg,
                                    border = androidx.compose.foundation.BorderStroke(0.7.dp, chipBorder),
                                    tonalElevation = 0.dp,
                                    shadowElevation = 0.dp,
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
                                            if (isPlaying) synthBandsFromTime(currentSecond)
                                            else listOf(0f, 0f, 0f)

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

                                    Spacer(Modifier.width(6.dp))
                                }
                                val badgeText = when {
                                    likeState.likeCount <= 0L -> ""
                                    likeState.likeCount < 100L -> likeState.likeCount.toString()
                                    else -> "99+"
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    // --- Heart with count badge (NO CLIP) ---
                                    Box(
                                        modifier = Modifier.size(40.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        IconButton(
                                            onClick = { if (!likeState.isLoading) onToggleLike(id) },
                                            modifier = Modifier.matchParentSize()
                                        ) {
                                            Icon(
                                                imageVector = if (likeState.isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                                contentDescription = null,
                                                tint = if (likeState.isLiked) Color(0xFFD50000) else menuTextColor.copy(alpha = 0.75f),
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }

                                        // ✅ Badge (top-right) — kept INSIDE the 40dp box
                                        if (badgeText.isNotEmpty()) {
                                            Surface(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .offset(x = (-2).dp, y = 4.dp), // inside the box
                                                shape = RoundedCornerShape(999.dp),
                                                color = Color.Black.copy(alpha = 0.40f),
                                                tonalElevation = 0.dp,
                                                shadowElevation = 0.dp
                                            ) {
                                                Text(
                                                    text = badgeText,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White.copy(alpha = 0.92f),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }

                                    // --- Bookmark (close) ---
                                    IconButton(
                                        onClick = { onToggleFavorite(id) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isFav) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                            contentDescription = null,
                                            tint = if (isFav) Color(0xFF2EA8FF) else menuTextColor.copy(alpha = 0.75f),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }


                            }
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------
// Likes (Supabase RPC only)
// -----------------------------

@Serializable
data class VideoLikeStateRow(
    @SerialName("is_liked") val isLiked: Boolean,
    @SerialName("like_count") val likeCount: Long
)

data class LikeUiState(
    val isLiked: Boolean,
    val likeCount: Long,
    val isLoading: Boolean
)

class LikesRepository(private val client: SupabaseClient) {

    suspend fun toggleLike(videoId: String): LikeUiState {
        try {
            val res = client.postgrest.rpc(
                function = "toggle_video_like",
                parameters = buildJsonObject { put("p_video_id", videoId) }
            )
            val row = res.decodeList<VideoLikeStateRow>().first()
            return LikeUiState(row.isLiked, row.likeCount, false)
        } catch (t: Throwable) {
            t.printStackTrace()   // IMPORTANT
            throw t               // bubble up so you SEE it
        }
    }

    suspend fun fetchLikeState(videoId: String): LikeUiState {
        try {
            val res = client.postgrest.rpc(
                function = "get_video_like_state",
                parameters = buildJsonObject { put("p_video_id", videoId) }
            )
            val row = res.decodeList<VideoLikeStateRow>().first()
            return LikeUiState(row.isLiked, row.likeCount, false)
        } catch (t: Throwable) {
            t.printStackTrace()
            return LikeUiState(false, 0L, false)
        }
    }
}



fun synthBandsFromTime(tSeconds: Float): List<Float> {
    val t = tSeconds.coerceAtLeast(0f)

    fun band(phase: Float, speed: Float): Float {
        val a = sin((t * speed) + phase) * 0.5f + 0.5f
        val b = sin((t * speed * 1.7f) + phase * 1.3f) * 0.5f + 0.5f
        val mix = a * 0.65f + b * 0.35f
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
fun glassTint(isDark: Boolean): Pair<Color, Color?> {
    return if (isDark) {
        Color.Black.copy(alpha = 0.16f) to Color.White.copy(alpha = 0.07f)
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

private fun List<MenuRow>.pruneEmptyHeaders(): List<MenuRow> {
    val out = mutableListOf<MenuRow>()
    var pendingHeader: MenuRow.Header? = null

    fun flushHeaderIfNeeded(hasContent: Boolean) {
        if (hasContent && pendingHeader != null) {
            out += pendingHeader!!
        }
        pendingHeader = null
    }

    var headerHasVideo = false

    for (row in this) {
        when (row) {
            is MenuRow.Header -> {
                flushHeaderIfNeeded(headerHasVideo)
                pendingHeader = row
                headerHasVideo = false
            }
            is MenuRow.Video -> {
                headerHasVideo = true
                if (pendingHeader != null) {
                    out += pendingHeader!!
                    pendingHeader = null
                }
                out += row
            }
        }
    }

    flushHeaderIfNeeded(headerHasVideo)
    return out.dropWhile { it is MenuRow.Header }
}
