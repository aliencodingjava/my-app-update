@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package com.flights.studio

import android.content.Context
import android.content.SharedPreferences
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens

data class NoteRow(
    val id: String,
    val text: String,
    val imagesCount: Int,
    val attachmentsCount: Int = 0,
    val audioCount: Int = 0,
    val videoCount: Int = 0,
    val title: String,
    val hasReminder: Boolean,
    val hasBadge: Boolean
)

enum class NotesSyncUiStatus {
    Synced,
    Uploading,
    Deleting,
    Downloading,
    Syncing,
    Error
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun AllNotesScreen(
    notesAdapter: NotesAdapter,
    notes: SnapshotStateList<NoteRow>,
    notesSize: Int,
    onAddNote: () -> Unit,
    onOpenSearch: (onDismiss: () -> Unit) -> Unit,
    onNavItemClick: (Int) -> Unit,
    onDeleteSelected: (Set<String>) -> Unit,
    onOpenNote: ((NoteRow, Int) -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    onOpenProfile: () -> Unit = {},
    syncStatus: NotesSyncUiStatus = NotesSyncUiStatus.Synced,
    onNotesSettingsChanged: () -> Unit = {}
) {
    var searchActive by remember { mutableStateOf(false) }

    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    val selectionMode = selectedIds.isNotEmpty()
    val selectionHapticView = LocalView.current


    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()

    fun toggleSelected(key: String) {
        val isSelecting = !selectedIds.contains(key)
        val next = if (selectedIds.contains(key)) selectedIds - key else selectedIds + key
        selectedIds = next
        if (isSelecting) {
            selectionHapticView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
        if (next.isEmpty()) notesAdapter.clearSelection()
    }

    val itemBackdrop = rememberLayerBackdrop()
    val topBarBackdrop = rememberLayerBackdrop()

    // keep adapter aware of backdrop once
    androidx.compose.runtime.LaunchedEffect(itemBackdrop) {
        notesAdapter.setBackdrop(itemBackdrop)
    }

    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences(NotesPagePrefs.NAME, Context.MODE_PRIVATE) }

    var twoCols by remember {
        mutableStateOf(prefs.getBoolean(NotesPagePrefs.KEY_TWO_COLUMNS, false))
    }

    // Settings state (so UI updates without restart)
    var s by remember(ctx, prefs) { mutableStateOf(ctx.readNotesPageSettings()) }

    // notes snapshot + dedupe
    val notesSnapshot by remember { derivedStateOf { notes.toList() } }
    val safeNotesSnapshot by remember { derivedStateOf { notesSnapshot.distinctBy { it.id } } }

    val scope = rememberCoroutineScope()

    val overscroll = remember(scope) {
        OffsetOverscrollEffect(
            orientation = Orientation.Vertical,
            animationScope = scope
        )
    }




    // preload reminder maps once whenever list size changes
    var remindersTick by remember { mutableIntStateOf(0) }
    androidx.compose.runtime.LaunchedEffect(notes.size) {
        notesAdapter.preloadReminderFlags(ctx)
        notesAdapter.preloadBadgeStates(ctx)
        remindersTick++
    }



    // Listen to prefs changes (grid toggle + compact + badges etc.)
    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, k ->
            if (k == NotesPagePrefs.KEY_TWO_COLUMNS) {
                twoCols = prefs.getBoolean(NotesPagePrefs.KEY_TWO_COLUMNS, false)
            }

            val affectsNotesUi =
                k == NotesPagePrefs.KEY_TWO_COLUMNS ||
                        k == NotesPagePrefs.KEY_COMPACT ||
                        k == NotesPagePrefs.KEY_SHOW_IMAGES_BADGE ||
                        k == NotesPagePrefs.KEY_SHOW_REMINDER_BADGE ||
                        k == NotesPagePrefs.KEY_SHOW_REMINDER_BELL ||
                        k == NotesPagePrefs.KEY_TITLE_TOP_COMPACT ||
                        k == NotesPagePrefs.KEY_TITLE_TOP_NORMAL ||
                        k == NotesPagePrefs.KEY_PALETTE_ENABLED ||
                        k == NotesPagePrefs.KEY_PALETTE_ID ||
                        k == NotesPagePrefs.KEY_SYNC_ONLINE ||
                        k == NotesPagePrefs.KEY_SORT

            if (affectsNotesUi) {
                s = ctx.readNotesPageSettings()

                notesAdapter.applyPageSettings(
                    compact = s.compact,
                    showImagesBadge = s.showImagesBadge,
                    showReminderBadge = s.showReminderBadge,
                    showReminderBell = s.showReminderBell,
                    titleTopCompactDp = s.titleTopCompactDp,
                    titleTopNormalDp = s.titleTopNormalDp,
                )

                onNotesSettingsChanged()

                remindersTick++
            }
        }

        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    Scaffold(
        topBar = {
            val topBarShape = RoundedCornerShape(0.dp)
            val isDark = isSystemInDarkTheme()
            val topPalette = if (s.paletteEnabled) resolveNotesPalette(s.paletteId, isDark) else null
            val barColor = topPalette?.actionBarTint ?: topActionBarTint()
            val contentColor = if (isDark) Color.White else Color(0xFF111111)

            Surface(
                shape = topBarShape,
                color = Color.Transparent,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .height(96.dp)
                    .drawBackdrop(
                        backdrop = topBarBackdrop,
                        shape = { topBarShape },
                        shadow = null,
                        highlight = null,
                        effects = {
                            blur(
                                radius = TopActionBarBlurDp.dp.toPx(),
                                edgeTreatment = TileMode.Mirror
                            )
                        },
                        onDrawSurface = { drawRect(barColor) }
                    )
            ) {
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(start = if (onBack != null) 4.dp else 20.dp, end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                                contentDescription = "Back",
                                tint = contentColor
                            )
                        }
                    }

                    val titleText = when {
                        selectionMode -> selectedIds.size.toString()
                        searchActive -> "Search"
                        else -> "Notes"
                    }

                    Text(
                        text = titleText,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = contentColor,
                        maxLines = 1
                    )

                    if (selectionMode) {
                        IconButton(
                            onClick = {
                                onDeleteSelected(selectedIds)
                                selectedIds = emptySet()
                                notesAdapter.clearSelection()
                            }
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete selected", tint = contentColor)
                        }
                    } else {
                        NotesSyncStatusPill(
                            syncOnline = s.syncOnline,
                            status = syncStatus,
                            noteCount = notesSize,
                            backdrop = topBarBackdrop,
                            contentColor = contentColor
                        )
                        Spacer(Modifier.width(8.dp))

                        NotesGlassAddButton(
                            backdrop = topBarBackdrop,
                            contentColor = contentColor,
                            onClick = onAddNote
                        )
                    }
                }
            }
        }
    )
    { padding ->
        CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .layerBackdrop(topBarBackdrop)
            ) {
                val isDark = isSystemInDarkTheme()
                val palette = if (s.paletteEnabled) {
                    resolveNotesPalette(s.paletteId, isDark)
                } else {
                    null
                }
                if (palette != null) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(palette.screenBackground)
                    )
                }
                ProfileBackdropImageLayer(
                    modifier = Modifier
                        .matchParentSize()
                        .layerBackdrop(itemBackdrop),
                    lightRes = R.drawable.light_grid_pattern,
                    darkRes = R.drawable.dark_grid_pattern,
                    imageAlpha = when {
                        palette != null && isDark -> 0.30f
                        palette != null -> 0.18f
                        isDark -> 1f
                        else -> 0.8f
                    },
                    scrimDark = 0f,
                    scrimLight = 0f
                )

                // ✅ shared row renderer (NO DUPLICATION)
                @Composable
                fun NoteRowItem(row: NoteRow) {
                    val note = row.text
                    val rowKey = row.id
                    val selected = selectedIds.contains(rowKey)


                    // ✅ prefer adapter cached title (what you used before), fallback to row.title
                    val title = remember(rowKey, row.title, note) {
                        val t = notesAdapter.titleNow(note)
                        if (!t.isNullOrBlank()) t else row.title
                    }

                    // keep this to force refresh when maps load
                    @Suppress("UNUSED_EXPRESSION")
                    remindersTick

                    NoteItem(
                        title = title,
                        note = note,
                        compact = s.compact,
                        dense = s.compact,
                        selectionMode = selectionMode,
                        selected = selected,
                        showReminderBell = notesAdapter.bellOn(note),
                        showReminderBadge = notesAdapter.badgeOn(note),
                        imagesCount = notesAdapter.imagesCount(note),
                        attachmentsCount = row.attachmentsCount,
                        audioCount = row.audioCount,
                        videoCount = row.videoCount,

                        onClick = {
                            if (selectionMode) {
                                toggleSelected(rowKey)
                            } else {
                                val rowPosition = notes.indexOfFirst { it.id == rowKey }
                                if (onOpenNote != null && rowPosition >= 0) {
                                    onOpenNote(row, rowPosition)
                                } else {
                                    notesAdapter.fireClick(note)
                                }
                            }
                        },
                        onLongClick = {
                            toggleSelected(rowKey)
                        },
                        onEdit = { notesAdapter.fireEdit(note) },
                        onReminderClick = { notesAdapter.fireReminder(note) },
                        backdrop = itemBackdrop,
                        titleTopCompactDp = s.titleTopCompactDp,
                        titleTopNormalDp = s.titleTopNormalDp,
                        palette = palette,
                        smallMediaBadges = twoCols,
//                        isInteractive = !fastMode
                    )
                }
                if (twoCols) {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        overscrollEffect = overscroll,
                        contentPadding = PaddingValues(
                            start = 12.dp, end = 12.dp,
                            top = padding.calculateTopPadding() + 12.dp,
                            bottom = 12.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        items(items = safeNotesSnapshot, key = { it.id }) { row ->
                            NoteRowItem(row)
                        }

                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                        }
                    }

                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        overscrollEffect = overscroll,
                        contentPadding = PaddingValues(
                            start = 12.dp, end = 12.dp,
                            top = padding.calculateTopPadding() + 12.dp,
                            bottom = 12.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        items(items = safeNotesSnapshot, key = { it.id }) { row ->
                            NoteRowItem(row)
                        }

                        item {
                            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                        }
                    }
                }

                var showWelcome by rememberSaveable { mutableStateOf(true) }

                val isEmpty = safeNotesSnapshot.isEmpty()

                NotesWelcomeOnboardingOverlay(
                    visible = isEmpty && showWelcome,
                    backdrop = itemBackdrop,
                    onContinue = { onAddNote() }
                )

            }
        }
    }
}

@Composable
private fun NotesGlassAddButton(
    backdrop: LayerBackdrop,
    contentColor: Color,
    onClick: () -> Unit
) {
    val shape = NotesActionShape
    Box(
        modifier = Modifier
            .size(NotesActionHeight)
            .notesActionGlass(backdrop, shape)
            .clip(shape)
    ) {
        IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Add note",
                tint = contentColor
            )
        }
    }
}

@Composable
private fun NotesSyncStatusPill(
    syncOnline: Boolean,
    status: NotesSyncUiStatus,
    noteCount: Int,
    backdrop: LayerBackdrop,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    val active = syncOnline && (
            status == NotesSyncUiStatus.Uploading ||
                    status == NotesSyncUiStatus.Deleting ||
                    status == NotesSyncUiStatus.Downloading ||
                    status == NotesSyncUiStatus.Syncing
            )
    val label = when {
        !syncOnline -> "Local · $noteCount"
        status == NotesSyncUiStatus.Uploading -> "Uploading..."
        status == NotesSyncUiStatus.Deleting -> "Deleting..."
        status == NotesSyncUiStatus.Downloading -> "Downloading notes..."
        status == NotesSyncUiStatus.Syncing -> "Syncing..."
        status == NotesSyncUiStatus.Error -> "Sync issue"
        else -> "Online · $noteCount"
    }

    Box(
        modifier = modifier
            .height(NotesActionHeight)
            .widthIn(min = 104.dp, max = 172.dp)
            .notesActionGlass(backdrop, NotesActionShape)
            .clip(NotesActionShape),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (active) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 1.6.dp,
                    color = contentColor
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                maxLines = 1
            )
        }
    }
}

private val NotesActionHeight = 42.dp
private val NotesActionShape = RoundedCornerShape(18.dp)

private fun Modifier.notesActionGlass(
    backdrop: LayerBackdrop,
    shape: RoundedCornerShape
): Modifier = drawBackdrop(
    backdrop = backdrop,
    shape = { shape },
    shadow = null,
    highlight = null,
    effects = {
        blur(radius = 18.dp.toPx(), edgeTreatment = TileMode.Mirror)
        lens(14.dp.toPx(), 24.dp.toPx())
    },
    onDrawSurface = {
        drawRect(Color.White.copy(alpha = 0.16f))
    }
)
