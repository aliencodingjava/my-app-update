@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package com.flights.studio

import android.content.Context
import android.content.SharedPreferences
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

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

data class NoteFolderUi(
    val id: String,
    val name: String,
    val count: Int
)

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
    onDeleteSelectedFolders: (Set<String>) -> Unit = {},
    onOpenNote: ((NoteRow, Int) -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    onOpenProfile: () -> Unit = {},
    syncStatus: NotesSyncUiStatus = NotesSyncUiStatus.Synced,
    syncAvailable: Boolean = false,
    onNotesSettingsChanged: () -> Unit = {},
    pageTitle: String = "Notes",
    showWelcomeOnEmptyNotes: Boolean = true,
    folderMode: Boolean = false,
    folders: List<NoteFolderUi> = emptyList(),
    onOpenFolder: (String) -> Unit = {},
    onCreateFolder: (String) -> Unit = {}
) {
    var searchActive by remember { mutableStateOf(false) }
    var showCreateFolderDialog by rememberSaveable { mutableStateOf(false) }
    var pendingFolderName by rememberSaveable { mutableStateOf("") }

    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    val selectionMode = selectedIds.isNotEmpty()
    val selectionHapticView = LocalView.current

    fun clearSelection() {
        selectedIds = emptySet()
        notesAdapter.clearSelection()
    }

    BackHandler(enabled = selectionMode) {
        clearSelection()
    }

    androidx.compose.runtime.LaunchedEffect(folderMode) {
        clearSelection()
    }


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
    val dialogIsDark = isSystemInDarkTheme()
    val dialogPalette = if (s.paletteEnabled) resolveNotesPalette(s.paletteId, dialogIsDark) else null
    val cleanFolderName = sanitizeFolderTitleInput(pendingFolderName)

    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreateFolderDialog = false
                pendingFolderName = ""
            },
            containerColor = dialogPalette?.noteTint ?: MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Text(
                    text = "New folder",
                    color = dialogPalette?.accent ?: MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                OutlinedTextField(
                    value = pendingFolderName,
                    onValueChange = { pendingFolderName = limitFolderTitleInput(it) },
                    label = { Text("Folder name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    enabled = cleanFolderName.isNotBlank(),
                    onClick = {
                        if (cleanFolderName.isNotBlank()) {
                            onCreateFolder(cleanFolderName)
                            showCreateFolderDialog = false
                            pendingFolderName = ""
                        }
                    }
                ) {
                    Text("Save", color = dialogPalette?.accent ?: MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCreateFolderDialog = false
                        pendingFolderName = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // notes snapshot + dedupe
    val notesSnapshot by remember { derivedStateOf { notes.toList() } }
    val safeNotesSnapshot by remember { derivedStateOf { notesSnapshot.distinctBy { it.id } } }
    val syncCountLabel = if (folderMode) {
        "${folders.size} folders · $notesSize notes"
    } else {
        safeNotesSnapshot.size.toString()
    }

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
                        else -> pageTitle
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
                                val idsToDelete = selectedIds
                                if (folderMode) {
                                    onDeleteSelectedFolders(idsToDelete)
                                } else {
                                    onDeleteSelected(idsToDelete)
                                }
                                clearSelection()
                            }
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete selected", tint = contentColor)
                        }
                    } else {
                        NotesSyncStatusPill(
                            syncOnline = s.syncOnline,
                            syncAvailable = syncAvailable,
                            status = syncStatus,
                            countLabel = syncCountLabel,
                            backdrop = topBarBackdrop,
                            palette = topPalette,
                            contentColor = contentColor
                        )
                        Spacer(Modifier.width(8.dp))

                        NotesGlassAddButton(
                            backdrop = topBarBackdrop,
                            palette = topPalette,
                            contentColor = contentColor,
                            onClick = {
                                if (folderMode) {
                                    pendingFolderName = ""
                                    showCreateFolderDialog = true
                                } else {
                                    onAddNote()
                                }
                            }
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
                if (folderMode) {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.fillMaxSize(),
                        overscrollEffect = overscroll,
                        contentPadding = PaddingValues(
                            start = 14.dp, end = 14.dp,
                            top = padding.calculateTopPadding() + 16.dp,
                            bottom = 14.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(items = folders, key = { it.id }) { folder ->
                            val selected = selectedIds.contains(folder.id)
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                NotesFolderCard(
                                    folder = folder,
                                    palette = palette,
                                    selected = selected,
                                    backdrop = itemBackdrop,
                                    onClick = {
                                        if (selectionMode) {
                                            toggleSelected(folder.id)
                                        } else {
                                            onOpenFolder(folder.id)
                                        }
                                    },
                                    onLongClick = { toggleSelected(folder.id) }
                                )
                            }
                        }

                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                        }
                    }
                } else if (twoCols) {
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
                    visible = showWelcomeOnEmptyNotes && !folderMode && isEmpty && showWelcome,
                    backdrop = itemBackdrop,
                    onContinue = { onAddNote() }
                )

            }
        }
    }
}

@Composable
private fun NotesFolderCard(
    folder: NoteFolderUi,
    palette: NotesPaletteColors?,
    selected: Boolean,
    backdrop: LayerBackdrop,
    onLongClick: () -> Unit,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val normalFolderBack = palette?.noteTint ?: if (isDark) Color(0xFF151617) else Color(0xFFE2E9F1)
    val folderFrontBase = if (isDark) {
        palette?.titleRail ?: Color(0xFF252B31)
    } else {
        palette?.titleRail ?: Color(0xFFE6EEF7)
    }
    val folderAccent = palette?.accent ?: if (isDark) Color(0xFF9FDBFF) else Color(0xFF2A79D8)
    val folderGlassTint = palette?.backdropTint ?: if (isDark) {
        Color(0xFF2F3439).copy(alpha = 0.34f)
    } else {
        Color(0xFFD4E4F2).copy(alpha = 0.34f)
    }
    val selectedBorder = folderAccent
    val selectedCheckFill = if (isDark) folderAccent.copy(alpha = 0.92f) else folderAccent
    val folderBack = if (selected) {
        palette?.accent?.copy(alpha = if (isDark) 0.42f else 0.28f)
            ?: if (isDark) Color(0xFFBFDDEF) else Color(0xFFE1F2FF)
    } else {
        normalFolderBack
    }
    val folderFront = if (selected) {
        palette?.titleRail ?: if (isDark) Color(0xFF2D5F76) else Color(0xFFF8FCFF)
    } else {
        folderFrontBase.copy(alpha = if (isDark) 0.94f else 0.92f)
    }
    val folderFrontDeep = if (selected) {
        palette?.actionBarTint ?: if (isDark) Color(0xFF1C3B4A) else Color(0xFFE8F5FF)
    } else {
        palette?.actionBarTint ?: if (isDark) Color(0xFF151617) else Color(0xFFD8E3EE)
    }
    val frontOutline = if (isDark) {
        Color.White.copy(alpha = if (selected) 0.20f else 0.10f)
    } else {
        Color(0xFF9DA3AD).copy(alpha = if (selected) 0.24f else 0.16f)
    }
    val frontTopHighlight = if (isDark) {
        Color.White.copy(alpha = if (selected) 0.16f else 0.10f)
    } else {
        Color.White.copy(alpha = 0.72f)
    }
    val labelColor = if (isDark) Color.White else Color(0xFF111111)
    val countColor = if (isDark) labelColor.copy(alpha = 0.68f) else Color(0xFF7B8087)
    val countText = if (folder.count == 1) "1 note" else "${folder.count} notes"
    val outerShape = RoundedCornerShape(12.dp)
    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.98f)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { outerShape },
                effects = {
                    vibrancy()
                    blur(radius = 18.dp.toPx(), edgeTreatment = TileMode.Mirror)
                    lens(10.dp.toPx(), 20.dp.toPx())
                },
                layerBlock = {
                    if (size.width > 0f && size.height > 0f) {
                        val width = size.width
                        val height = size.height
                        val progress = interactiveHighlight.pressProgress
                        val scale = lerp(1f, 1f + 2.dp.toPx() / height, progress)

                        val maxOffset = size.minDimension
                        val offset = interactiveHighlight.offset
                        val initialDerivative = 0.018f
                        translationX = maxOffset * tanh(initialDerivative * offset.x / maxOffset)
                        translationY = maxOffset * tanh(initialDerivative * offset.y / maxOffset)

                        val maxDragScale = 1.25.dp.toPx() / height
                        val offsetAngle = atan2(offset.y, offset.x)
                        scaleX =
                            scale +
                                    maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) *
                                    (width / height).fastCoerceAtMost(1f)
                        scaleY =
                            scale +
                                    maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) *
                                    (height / width).fastCoerceAtMost(1f)
                    }
                },
                onDrawSurface = {
                    drawRect(folderBack.copy(alpha = if (isDark) 0.82f else 0.84f))
                    drawRect(folderGlassTint)
                }
            )
            .clip(outerShape)
            .border(
                width = if (selected) 2.dp else if (isDark) 0.dp else 1.dp,
                color = if (selected) {
                    selectedBorder
                } else if (isDark) {
                    Color.Transparent
                } else {
                    Color(0xFFB9BEC7).copy(alpha = 0.18f)
                },
                shape = outerShape
            )
            .then(interactiveHighlight.modifier)
            .then(interactiveHighlight.gestureModifier)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 5.dp)
                .width(58.dp)
                .height(70.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .width(50.dp)
                    .height(64.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = if (isDark) 0.76f else 0.94f),
                                Color(0xFFECEFF4).copy(alpha = if (isDark) 0.62f else 0.88f)
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .width(50.dp)
                    .height(65.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = if (isDark) 0.90f else 0.98f),
                                Color(0xFFF1F4F8).copy(alpha = if (isDark) 0.76f else 0.94f)
                            )
                        )
                    )
            )
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(NotesFolderFrontShape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            folderFront.copy(alpha = if (isDark) 0.78f else 0.84f),
                            folderFrontDeep.copy(alpha = if (isDark) 0.88f else 0.92f)
                        )
                    )
                )
                .border(1.dp, frontOutline, NotesFolderFrontShape)
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = if (isDark) 0.08f else 0.30f),
                                Color.White.copy(alpha = if (isDark) 0.03f else 0.12f),
                                Color.Black.copy(alpha = if (isDark) 0.12f else 0.04f)
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .height(18.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                (palette?.accent ?: Color.White).copy(alpha = if (isDark) 0.12f else 0.16f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(NotesFolderFrontShape)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(frontTopHighlight)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 8.dp, end = 7.dp, bottom = 7.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = folder.name,
                color = labelColor,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = countText,
                color = countColor,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .size(17.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(selectedCheckFill),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = if (isDark) Color(0xFF102836) else Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

private val NotesFolderFrontShape = GenericShape { size, _ ->
    val leftTopY = size.height * 0.38f
    val rightTopY = size.height * 0.46f
    val tabStartX = size.width * 0.40f
    val curveEndX = size.width * 0.64f
    val radius = size.minDimension * 0.11f

    moveTo(0f, leftTopY)
    lineTo(tabStartX, leftTopY)
    cubicTo(
        size.width * 0.49f,
        leftTopY,
        size.width * 0.51f,
        rightTopY,
        curveEndX,
        rightTopY
    )
    lineTo(size.width, rightTopY)
    lineTo(size.width, size.height - radius)
    quadraticBezierTo(size.width, size.height, size.width - radius, size.height)
    lineTo(radius, size.height)
    quadraticBezierTo(0f, size.height, 0f, size.height - radius)
    close()
}

private fun sanitizeFolderTitleInput(raw: String): String {
    return raw
        .replace('\n', ' ')
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .take(3)
        .joinToString(" ") { it.take(24) }
        .take(72)
}

private fun limitFolderTitleInput(raw: String): String {
    val sanitized = sanitizeFolderTitleInput(raw)
    val trimmedStart = raw.replace('\n', ' ').trimStart()
    return if (trimmedStart.split(Regex("\\s+")).filter { it.isNotBlank() }.size <= 3 && trimmedStart.length <= 72) {
        trimmedStart
    } else {
        sanitized
    }
}

@Composable
private fun NotesGlassAddButton(
    backdrop: LayerBackdrop,
    palette: NotesPaletteColors?,
    contentColor: Color,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }
    Box(
        modifier = Modifier
            .size(NotesActionHeight)
            .notesLiquidTransform(CircleShape, interactiveHighlight)
            .notesActionGlass(backdrop, CircleShape, isDark, palette, interactiveHighlight)
            .clickable(
                interactionSource = null,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .then(interactiveHighlight.modifier)
            .then(interactiveHighlight.gestureModifier),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = "Add note",
            tint = contentColor,
            modifier = Modifier.size(27.dp)
        )
    }
}

@Composable
private fun NotesSyncStatusPill(
    syncOnline: Boolean,
    syncAvailable: Boolean,
    status: NotesSyncUiStatus,
    countLabel: String,
    backdrop: LayerBackdrop,
    palette: NotesPaletteColors?,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val online = syncOnline && syncAvailable
    val active = online && (
            status == NotesSyncUiStatus.Uploading ||
                    status == NotesSyncUiStatus.Deleting ||
                    status == NotesSyncUiStatus.Downloading ||
                    status == NotesSyncUiStatus.Syncing
            )
    val statusLabel = when {
        !syncOnline -> "Local"
        !syncAvailable -> "Local"
        status == NotesSyncUiStatus.Uploading -> "Uploading..."
        status == NotesSyncUiStatus.Deleting -> "Deleting..."
        status == NotesSyncUiStatus.Downloading -> "Downloading notes..."
        status == NotesSyncUiStatus.Syncing -> "Syncing..."
        status == NotesSyncUiStatus.Error -> "Sync issue"
        else -> "Online"
    }
    val dotColor = when {
        status == NotesSyncUiStatus.Error -> Color(0xFFFF6257)
        online -> Color(0xFF21C765)
        else -> if (isDark) Color(0xFFFFC95C) else Color(0xFFC47A00)
    }
    val primaryText = contentColor
    val secondaryText = primaryText.copy(alpha = if (isDark) 0.72f else 0.68f)
    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }

    Box(
        modifier = modifier
            .height(NotesActionHeight)
            .widthIn(min = 126.dp, max = 228.dp)
            .notesLiquidTransform(NotesPillShape, interactiveHighlight)
            .notesActionGlass(backdrop, NotesPillShape, isDark, palette, interactiveHighlight)
            .then(interactiveHighlight.modifier)
            .then(interactiveHighlight.gestureModifier),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (active) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 1.6.dp,
                    color = primaryText
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(dotColor)
                )
            }
            Spacer(Modifier.width(7.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                    color = primaryText,
                    maxLines = 1
                )
                Text(
                    text = countLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private val NotesActionHeight = 48.dp
private val NotesPillShape = RoundedCornerShape(percent = 50)

private fun Modifier.notesLiquidTransform(
    shape: Shape,
    interactiveHighlight: InteractiveHighlight
): Modifier = graphicsLayer {
    this.shape = shape
    clip = true

    if (size.width > 0f && size.height > 0f) {
        val width = size.width
        val height = size.height

        val progress = interactiveHighlight.pressProgress
        val scale = lerp(1f, 1f + 4.dp.toPx() / height, progress)

        val maxOffset = size.minDimension
        val initialDerivative = 0.05f
        val offset = interactiveHighlight.offset
        translationX = maxOffset * tanh(initialDerivative * offset.x / maxOffset)
        translationY = maxOffset * tanh(initialDerivative * offset.y / maxOffset)

        val maxDragScale = 4.dp.toPx() / height
        val offsetAngle = atan2(offset.y, offset.x)
        scaleX =
            scale +
                    maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) *
                    (width / height).fastCoerceAtMost(1f)
        scaleY =
            scale +
                    maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) *
                    (height / width).fastCoerceAtMost(1f)
    }
}

private fun Modifier.notesActionGlass(
    backdrop: LayerBackdrop,
    shape: Shape,
    isDark: Boolean,
    palette: NotesPaletteColors?,
    interactiveHighlight: InteractiveHighlight? = null
): Modifier {
    val surfaceColor = if (isDark) {
        (palette?.actionBarTint ?: Color(0xFF151617)).copy(alpha = 0.84f)
    } else {
        (palette?.actionBarTint ?: Color(0xFFD7ECFF)).copy(alpha = if (palette != null) 0.88f else 0.84f)
    }
    val tint = if (isDark) {
        (palette?.accent ?: Color(0xFF2F3439)).copy(alpha = if (palette != null) 0.32f else 0.34f)
    } else {
        (palette?.accent ?: Color(0xFF9FD1FF)).copy(alpha = if (palette != null) 0.46f else 0.62f)
    }
    val lift = if (isDark) {
        Color.White.copy(alpha = 0.07f)
    } else {
        (palette?.noteTint ?: Color.White).copy(alpha = if (palette != null) 0.18f else 0.10f)
    }
    return drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            vibrancy()
            blur(radius = 2.dp.toPx(), edgeTreatment = TileMode.Mirror)
            lens(12.dp.toPx(), 24.dp.toPx())
        },
        onDrawSurface = {
            drawRect(tint, blendMode = BlendMode.Hue)
            drawRect(surfaceColor)
            drawRect(lift)
        }
    )
        .clip(shape)
}
