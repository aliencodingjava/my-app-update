@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package com.flights.studio

import android.content.Context
import android.content.SharedPreferences
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as colorLerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class NoteRow(
    val id: String,
    val text: String,
    val imagesCount: Int,
    val attachmentsCount: Int = 0,
    val audioCount: Int = 0,
    val videoCount: Int = 0,
    val title: String,
    val hasReminder: Boolean,
    val hasBadge: Boolean,
    val createdAtMs: Long = 0L
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
    val count: Int,
    val createdAt: Long = 0L,
    val modifiedAt: Long = createdAt,
    val colorArgb: Long? = null
)

private data class FolderColorOption(
    val name: String,
    val colorArgb: Long?
)

private val NotesFolderTitleFont = FontFamily(
    Font(R.font.kite_one_regular, weight = FontWeight.Normal)
)

private val FolderColorOptions = listOf(
    FolderColorOption("Default", null),
    FolderColorOption("Raspberry", 0xFFC2185B),
    FolderColorOption("Dragon Fruit", 0xFFFF4696),
    FolderColorOption("Burnt Orange", 0xFFFC6C26),
    FolderColorOption("Soft Apricot", 0xFFFFD6A5),
    FolderColorOption("Champagne", 0xFFF8E7C9),
    FolderColorOption("Cream Soda", 0xFFFFF0C9),
    FolderColorOption("Vanila", 0xFFFFF4D6),
    FolderColorOption("Butter Yellow", 0xFFFFF275),
    FolderColorOption("Acid Lime", 0xFFD7FF00),
    FolderColorOption("Lime Spark", 0xFFB6FF2E),
    FolderColorOption("Neon Lime", 0xFFC8FF3D),
    FolderColorOption("Warm Lime", 0xFFCFFF74),
    FolderColorOption("Olive Ink", 0xFF2F3A1D),
    FolderColorOption("Emerald Ink", 0xFF064E3B),
    FolderColorOption("Sky Mint", 0xFFB8F7E4),
    FolderColorOption("Aqua Foam", 0xFF8FFFE0),
    FolderColorOption("Cyber Teal", 0xFF03313A),
    FolderColorOption("Pale Sky", 0xFFE0F2FE),
    FolderColorOption("Ice Glass", 0xFFDFF7FF),
    FolderColorOption("Signal Blue", 0xFF0057FF),
    FolderColorOption("Quantum Blue", 0xFF245777),
    FolderColorOption("Blueberry", 0xFF243B8F),
    FolderColorOption("Electric Indigo", 0xFF5B3DF5),
    FolderColorOption("Ultra Violet", 0xFF6A00F4),
    FolderColorOption("Cyber Grape", 0xFF6D28D9),
    FolderColorOption("Royal Iris", 0xFF3A0CA3),
    FolderColorOption("Violet Ink", 0xFF2D1B69),
    FolderColorOption("Electric Orchid", 0xFFE46CFF),
    FolderColorOption("Soft Liliac", 0xFFE8DEFF),
    FolderColorOption("Deep Plum", 0xFF2E0F35),
    FolderColorOption("Night Violet", 0xFF1E1033),
    FolderColorOption("Porcelain", 0xFFF8F7F4),
    FolderColorOption("Graphite", 0xFF25272C),
    FolderColorOption("Deep Graphite", 0xFF1F2329)
)

private enum class FolderViewMode(val storageValue: String, val label: String) {
    GridLarge("grid_large", "Grid (large)"),
    GridSmall("grid_small", "Grid (small)"),
    List("list", "List"),
    SimpleList("simple_list", "Simple list");

    companion object {
        fun fromStorage(value: String?): FolderViewMode =
            entries.firstOrNull { it.storageValue == value } ?: SimpleList
    }
}

private enum class FolderSortMode(val storageValue: String, val label: String) {
    CreatedAscending("created_ascending", "Date created (ascending)"),
    CreatedDescending("created_descending", "Date created (descending)"),
    ModifiedAscending("modified_ascending", "Date modified (ascending)"),
    ModifiedDescending("modified_descending", "Date modified (descending)"),
    NameAscending("name_ascending", "Name (A to Z)"),
    NameDescending("name_descending", "Name (Z to A)");

    companion object {
        fun fromStorage(value: String?): FolderSortMode =
            entries.firstOrNull { it.storageValue == value } ?: CreatedAscending
    }
}

private enum class FolderMenuPage { Main, View }

private enum class FolderModalPage { Create, Rename, Color, Delete }

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun AllNotesScreen(
    notesAdapter: NotesAdapter,
    notes: SnapshotStateList<NoteRow>,
    onAddNote: () -> Unit,
    onDeleteSelected: (Set<String>) -> Unit,
    onDeleteSelectedFolders: (Set<String>) -> Unit = {},
    onOpenNote: ((NoteRow, Int) -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    syncStatus: NotesSyncUiStatus = NotesSyncUiStatus.Synced,
    syncAvailable: Boolean = false,
    onOpenNotesSettings: () -> Unit = {},
    onNotesSettingsChanged: () -> Unit = {},
    pageTitle: String = "Notes",
    showWelcomeOnEmptyNotes: Boolean = true,
    folderMode: Boolean = false,
    folders: List<NoteFolderUi> = emptyList(),
    onOpenFolder: (String) -> Unit = {},
    bottomOverlayClearance: Dp = 0.dp,
    onFolderSelectionModeChanged: (Boolean) -> Unit = {},
    onRenameFolder: (String, String) -> Unit = { _, _ -> },
    onSetFolderColor: (String, Long?) -> Unit = { _, _ -> },
    onCreateFolder: (String) -> Unit = {}
) {
    var searchActive by remember { mutableStateOf(false) }
    var pendingFolderName by rememberSaveable { mutableStateOf("") }

    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var folderSelectionActive by rememberSaveable { mutableStateOf(false) }
    val selectionMode = if (folderMode) folderSelectionActive else selectedIds.isNotEmpty()
    val selectionHapticView = LocalView.current

    fun clearSelection() {
        selectedIds = emptySet()
        folderSelectionActive = false
        notesAdapter.clearSelection()
    }

    BackHandler(enabled = selectionMode) {
        clearSelection()
    }

    androidx.compose.runtime.LaunchedEffect(folderMode) {
        clearSelection()
    }

    androidx.compose.runtime.LaunchedEffect(folderMode, folderSelectionActive) {
        onFolderSelectionModeChanged(folderMode && folderSelectionActive)
    }

    DisposableEffect(Unit) {
        onDispose { onFolderSelectionModeChanged(false) }
    }


    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    val folderLargeGridState = rememberLazyGridState()
    val folderSmallGridState = rememberLazyGridState()
    val folderSimpleGridState = rememberLazyGridState()
    val folderListState = rememberLazyListState()

    fun toggleSelected(key: String) {
        if (folderMode) {
            folderSelectionActive = true
            val isSelecting = !selectedIds.contains(key)
            selectedIds = if (isSelecting) selectedIds + key else selectedIds - key
            if (isSelecting) {
                selectionHapticView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
            return
        }
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

    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences(NotesPagePrefs.NAME, Context.MODE_PRIVATE) }

    var folderViewMode by rememberSaveable {
        mutableStateOf(
            FolderViewMode.fromStorage(
                prefs.getString(NotesPagePrefs.KEY_FOLDER_VIEW, FolderViewMode.SimpleList.storageValue)
            )
        )
    }
    var folderSortMode by rememberSaveable {
        mutableStateOf(
            FolderSortMode.fromStorage(
                prefs.getString(NotesPagePrefs.KEY_FOLDER_SORT, FolderSortMode.CreatedAscending.storageValue)
            )
        )
    }
    var pendingFolderSort by remember { mutableStateOf(folderSortMode) }
    var folderMenuExpanded by remember { mutableStateOf(false) }
    var folderMenuPage by remember { mutableStateOf(FolderMenuPage.Main) }
    var showFolderSortSheet by rememberSaveable { mutableStateOf(false) }
    var pendingRenameFolderName by rememberSaveable { mutableStateOf("") }
    var folderModalPage by remember { mutableStateOf<FolderModalPage?>(null) }
    var folderModalVisible by remember { mutableStateOf(false) }
    val folderModalScope = rememberCoroutineScope()
    val folderSortSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun openFolderModal(page: FolderModalPage) {
        folderModalPage = page
        folderModalVisible = true
    }

    fun dismissFolderModal(afterDismiss: () -> Unit = {}) {
        folderModalScope.launch {
            folderModalVisible = false
            delay(220)
            folderModalPage = null
            afterDismiss()
        }
    }

    var twoCols by remember {
        mutableStateOf(prefs.getBoolean(NotesPagePrefs.KEY_TWO_COLUMNS, false))
    }

    // Settings state (so UI updates without restart)
    var s by remember(ctx, prefs) { mutableStateOf(ctx.readNotesPageSettings()) }
    val dialogIsDark = isSystemInDarkTheme()
    val dialogPalette = if (s.paletteEnabled) resolveNotesPalette(s.paletteId, dialogIsDark) else null
    val dialogContentColor = if (dialogIsDark) Color.White else Color(0xFF1D1B20)
    val dialogMutedColor = if (dialogIsDark) Color.White.copy(alpha = 0.72f) else Color(0xFF49454F)
    val dialogAccentColor = dialogPalette?.accent ?: MaterialTheme.colorScheme.primary
    val cleanFolderName = sanitizeFolderTitleInput(pendingFolderName)
    val selectedFolders = folders.filter { it.id in selectedIds }
    val editableSelectedFolders = selectedFolders.filter { it.id != NoteFolderStore.MAIN_FOLDER_ID }
    val selectedFolder = selectedFolders.singleOrNull()

    if (showFolderSortSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFolderSortSheet = false },
            sheetState = folderSortSheetState,
            containerColor = (dialogPalette?.noteTint
                ?: MaterialTheme.colorScheme.surfaceContainerHigh).copy(alpha = 1f),
            contentColor = dialogContentColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 24.dp)
            ) {
                Text(
                    text = "Sort by",
                    style = MaterialTheme.typography.headlineSmall,
                    color = dialogContentColor,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                FolderSortMode.entries.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { pendingFolderSort = option }
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = pendingFolderSort == option,
                            onClick = { pendingFolderSort = option },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = dialogAccentColor,
                                unselectedColor = dialogMutedColor
                            )
                        )
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = dialogContentColor,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(top = 8.dp),
                    color = dialogMutedColor.copy(alpha = 0.28f)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showFolderSortSheet = false }) {
                        Text("Cancel", color = dialogMutedColor)
                    }
                    TextButton(
                        onClick = {
                            folderSortMode = pendingFolderSort
                            prefs.edit().putString(
                                NotesPagePrefs.KEY_FOLDER_SORT,
                                pendingFolderSort.storageValue
                            ).apply()
                            showFolderSortSheet = false
                        }
                    ) { Text("Done", color = dialogAccentColor) }
                }
            }
        }
    }

    // notes snapshot + dedupe
    val notesSnapshot by remember { derivedStateOf { notes.toList() } }
    val safeNotesSnapshot by remember { derivedStateOf { notesSnapshot.distinctBy { it.id } } }
    val folderSnapshot = folders.toList()
    val folderBottomClearance = if (folderMode && folderSelectionActive) 92.dp else bottomOverlayClearance
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
                        folderMode && folderSelectionActive -> "${selectedIds.size} selected"
                        selectionMode -> selectedIds.size.toString()
                        searchActive -> "Search"
                        else -> pageTitle
                    }

                    val showFolderStatus = folderMode && !folderSelectionActive && !searchActive
                    if (showFolderStatus) {
                        val online = s.syncOnline && syncAvailable && syncStatus != NotesSyncUiStatus.Error
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Box {
                                Text(
                                    text = titleText,
                                    color = contentColor,
                                    fontFamily = NotesFolderTitleFont,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Normal,
                                    lineHeight = 35.sp,
                                    letterSpacing = 0.sp,
                                    maxLines = 1
                                )
                                Text(
                                    text = if (online) "online" else "local",
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 4.dp, y = (-3).dp),
                                    color = contentColor.copy(alpha = 0.78f),
                                    fontFamily = NotesFolderTitleFont,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Normal,
                                    lineHeight = 16.sp,
                                    letterSpacing = 0.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    } else {
                        Text(
                            text = titleText,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = contentColor,
                            maxLines = 1
                        )
                    }

                    if (folderMode && folderSelectionActive) {
                        TextButton(onClick = ::clearSelection) {
                            Text("Cancel", color = contentColor, fontWeight = FontWeight.SemiBold)
                        }
                    } else if (selectionMode) {
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
                        NotesGlassAddButton(
                            backdrop = topBarBackdrop,
                            palette = topPalette,
                            contentColor = contentColor,
                            onClick = {
                                if (folderMode) {
                                    pendingFolderName = ""
                                    openFolderModal(FolderModalPage.Create)
                                } else {
                                    onAddNote()
                                }
                            }
                        )
                        if (folderMode) {
                            Spacer(Modifier.width(4.dp))
                            Box {
                                NotesGlassFolderMenuButton(
                                    backdrop = topBarBackdrop,
                                    palette = topPalette,
                                    contentColor = contentColor,
                                    onClick = {
                                        folderMenuPage = FolderMenuPage.Main
                                        folderMenuExpanded = true
                                    }
                                )
                                DropdownMenu(
                                    expanded = folderMenuExpanded,
                                    onDismissRequest = {
                                        folderMenuExpanded = false
                                        folderMenuPage = FolderMenuPage.Main
                                    },
                                    shape = RoundedCornerShape(22.dp),
                                    containerColor = (topPalette?.noteTint
                                        ?: MaterialTheme.colorScheme.surfaceContainerHigh).copy(alpha = 1f),
                                    tonalElevation = 0.dp,
                                    shadowElevation = 10.dp
                                ) {
                                    if (folderMenuPage == FolderMenuPage.Main) {
                                        DropdownMenuItem(
                                            text = { Text("Select", color = dialogContentColor) },
                                            leadingIcon = {
                                                Icon(Icons.Filled.Check, contentDescription = null, tint = dialogContentColor)
                                            },
                                            onClick = {
                                                folderMenuExpanded = false
                                                folderSelectionActive = true
                                                selectedIds = emptySet()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("View", color = dialogContentColor) },
                                            leadingIcon = {
                                                Icon(Icons.Filled.GridView, contentDescription = null, tint = dialogContentColor)
                                            },
                                            onClick = { folderMenuPage = FolderMenuPage.View }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Sort", color = dialogContentColor) },
                                            leadingIcon = {
                                                Icon(Icons.Filled.Sort, contentDescription = null, tint = dialogContentColor)
                                            },
                                            onClick = {
                                                pendingFolderSort = folderSortMode
                                                folderMenuExpanded = false
                                                folderMenuPage = FolderMenuPage.Main
                                                showFolderSortSheet = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Customize notes", color = dialogContentColor) },
                                            leadingIcon = {
                                                Icon(Icons.Filled.Settings, contentDescription = null, tint = dialogContentColor)
                                            },
                                            onClick = {
                                                folderMenuExpanded = false
                                                folderMenuPage = FolderMenuPage.Main
                                                onOpenNotesSettings()
                                            }
                                        )
                                    } else {
                                        FolderViewMode.entries.forEach { option ->
                                            DropdownMenuItem(
                                                text = { Text(option.label, color = dialogContentColor) },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = if (option == FolderViewMode.List) {
                                                            Icons.Filled.ViewList
                                                        } else {
                                                            Icons.Filled.GridView
                                                        },
                                                        contentDescription = null,
                                                        tint = dialogContentColor
                                                    )
                                                },
                                                trailingIcon = {
                                                    if (folderViewMode == option) {
                                                        Icon(Icons.Filled.Check, contentDescription = null, tint = dialogContentColor)
                                                    }
                                                },
                                                onClick = {
                                                    folderViewMode = option
                                                    prefs.edit().putString(
                                                        NotesPagePrefs.KEY_FOLDER_VIEW,
                                                        option.storageValue
                                                    ).apply()
                                                    folderMenuExpanded = false
                                                    folderMenuPage = FolderMenuPage.Main
                                                }
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
                if (isDark) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color(0xFF1F2329))
                    )
                } else if (palette != null) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(palette.screenBackground)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.White)
                    )
                }
                if (!isDark) {
                    ProfileBackdropImageLayer(
                        modifier = Modifier
                            .matchParentSize()
                            .then(
                                if (folderMode) Modifier
                                else Modifier.layerBackdrop(itemBackdrop)
                            ),
                        lightRes = R.drawable.light_grid_pattern,
                        darkRes = R.drawable.dark_grid_pattern,
                        imageAlpha = if (palette != null) 0.18f else 0.8f,
                        scrimDark = 0f,
                        scrimLight = 0f
                    )
                }

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
                        createdAtMs = row.createdAtMs,

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
                        titleTopCompactDp = s.titleTopCompactDp,
                        titleTopNormalDp = s.titleTopNormalDp,
                        palette = palette,
                        smallMediaBadges = twoCols,
                    )
                }
                if (folderMode) {
                    AnimatedContent(
                        targetState = folderViewMode to folderSortMode,
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                        transitionSpec = {
                            (fadeIn(
                                animationSpec = tween(durationMillis = 140, delayMillis = 190)
                            ) + scaleIn(
                                initialScale = 0.94f,
                                animationSpec = tween(durationMillis = 140, delayMillis = 190)
                            )) togetherWith (fadeOut(
                                animationSpec = tween(durationMillis = 240)
                            ) + scaleOut(
                                targetScale = 0.94f,
                                animationSpec = tween(durationMillis = 240)
                            ))
                        },
                        label = "folder-layout-transition"
                    ) { (viewMode, sortMode) ->
                        val visibleFolders = remember(folderSnapshot, sortMode) {
                            sortFolderRows(folderSnapshot, sortMode)
                        }
                        if (viewMode == FolderViewMode.List) {
                            LazyColumn(
                                state = folderListState,
                                modifier = Modifier.fillMaxSize(),
                                overscrollEffect = overscroll,
                                contentPadding = PaddingValues(
                                    start = 8.dp,
                                    end = 8.dp,
                                    top = padding.calculateTopPadding() + 10.dp,
                                    bottom = 10.dp + folderBottomClearance
                                ),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(
                                    items = visibleFolders,
                                    key = { it.id },
                                    contentType = { "folder-list" }
                                ) { folder ->
                                    val selected = selectedIds.contains(folder.id)
                                    NotesFolderListRow(
                                        folder = folder,
                                        palette = palette,
                                        selectionMode = folderSelectionActive,
                                        selected = selected,
                                        onClick = {
                                            if (folderSelectionActive) toggleSelected(folder.id)
                                            else onOpenFolder(folder.id)
                                        },
                                        onLongClick = { toggleSelected(folder.id) }
                                    )
                                }
                                item { Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)) }
                            }
                        } else {
                            val columns = when (viewMode) {
                                FolderViewMode.GridLarge -> 2
                                FolderViewMode.GridSmall -> 3
                                else -> 4
                            }
                            val state = when (viewMode) {
                                FolderViewMode.GridLarge -> folderLargeGridState
                                FolderViewMode.GridSmall -> folderSmallGridState
                                else -> folderSimpleGridState
                            }
                            val horizontalGap = when (viewMode) {
                                FolderViewMode.GridLarge -> 10.dp
                                FolderViewMode.GridSmall -> 8.dp
                                else -> 6.dp
                            }
                            LazyVerticalGrid(
                                state = state,
                                columns = GridCells.Fixed(columns),
                                modifier = Modifier.fillMaxSize(),
                                overscrollEffect = overscroll,
                                contentPadding = PaddingValues(
                                    start = 8.dp,
                                    end = 8.dp,
                                    top = padding.calculateTopPadding() + 10.dp,
                                    bottom = 10.dp + folderBottomClearance
                                ),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(horizontalGap)
                            ) {
                                items(
                                    items = visibleFolders,
                                    key = { it.id },
                                    contentType = { "folder-grid-$columns" }
                                ) { folder ->
                                    val selected = selectedIds.contains(folder.id)
                                    NotesFolderCard(
                                        folder = folder,
                                        viewMode = viewMode,
                                        palette = palette,
                                        selectionMode = folderSelectionActive,
                                        selected = selected,
                                        onClick = {
                                            if (folderSelectionActive) toggleSelected(folder.id)
                                            else onOpenFolder(folder.id)
                                        },
                                        onLongClick = { toggleSelected(folder.id) }
                                    )
                                }

                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                                }
                            }
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
                            bottom = 12.dp + bottomOverlayClearance
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
                            bottom = 12.dp + bottomOverlayClearance
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

                if (folderMode && folderSelectionActive) {
                    FolderSelectionActionBar(
                        backdrop = itemBackdrop,
                        selectedCount = selectedIds.size,
                        editableSelectedCount = editableSelectedFolders.size,
                        onRename = {
                            selectedFolder?.let { folder ->
                                if (folder.id != NoteFolderStore.MAIN_FOLDER_ID) {
                                    pendingRenameFolderName = folder.name
                                    openFolderModal(FolderModalPage.Rename)
                                }
                            }
                        },
                        onColor = {
                            if (editableSelectedFolders.isNotEmpty()) {
                                openFolderModal(FolderModalPage.Color)
                            }
                        },
                        onDelete = {
                            val folderIds = editableSelectedFolders.mapTo(linkedSetOf()) { it.id }
                            if (folderIds.isNotEmpty()) {
                                openFolderModal(FolderModalPage.Delete)
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 4.dp)
                            .navigationBarsPadding()
                    )
                }

                if (folderMode && folderModalPage != null) {
                    FolderSolidModalShell(
                        visible = folderModalVisible,
                        isDark = dialogIsDark,
                        onDismiss = {
                            dismissFolderModal {
                                if (folderModalPage == FolderModalPage.Create) {
                                    pendingFolderName = ""
                                }
                            }
                        }
                    ) {
                        val modalPage = folderModalPage
                        val cleanRename = sanitizeFolderTitleInput(pendingRenameFolderName)
                        val modalAccent = dialogPalette?.accent ?: MaterialTheme.colorScheme.primary
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = when (modalPage) {
                                    FolderModalPage.Create -> "New folder"
                                    FolderModalPage.Rename -> "Rename folder"
                                    FolderModalPage.Color -> "Folder color"
                                    FolderModalPage.Delete -> "Delete folder"
                                    null -> ""
                                },
                                style = MaterialTheme.typography.titleLarge,
                                color = dialogContentColor,
                                fontWeight = FontWeight.SemiBold
                            )

                            when (modalPage) {
                                FolderModalPage.Create -> {
                                    OutlinedTextField(
                                        value = pendingFolderName,
                                        onValueChange = { pendingFolderName = limitFolderTitleInput(it) },
                                        label = { Text("Folder name") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    FolderModalButtons(
                                        confirmLabel = "Create",
                                        confirmEnabled = cleanFolderName.isNotBlank(),
                                        confirmColor = modalAccent,
                                        onCancel = {
                                            dismissFolderModal { pendingFolderName = "" }
                                        },
                                        onConfirm = {
                                            onCreateFolder(cleanFolderName)
                                            dismissFolderModal { pendingFolderName = "" }
                                        }
                                    )
                                }

                                FolderModalPage.Rename -> {
                                    OutlinedTextField(
                                        value = pendingRenameFolderName,
                                        onValueChange = { pendingRenameFolderName = limitFolderTitleInput(it) },
                                        label = { Text("Folder name") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    FolderModalButtons(
                                        confirmLabel = "Rename",
                                        confirmEnabled = cleanRename.isNotBlank() && selectedFolder != null,
                                        confirmColor = modalAccent,
                                        onCancel = { dismissFolderModal() },
                                        onConfirm = {
                                            selectedFolder?.let { folder ->
                                                onRenameFolder(folder.id, cleanRename)
                                            }
                                            dismissFolderModal { clearSelection() }
                                        }
                                    )
                                }

                                FolderModalPage.Color -> {
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(2),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(300.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        items(
                                            items = FolderColorOptions,
                                            key = { option -> option.name }
                                        ) { option ->
                                            val colorArgb = option.colorArgb
                                            val swatchColor = colorArgb?.let(::Color)
                                                ?: MaterialTheme.colorScheme.surfaceContainerHighest
                                            val selectedColor = editableSelectedFolders.all {
                                                it.colorArgb == colorArgb
                                            }
                                            val checkColor = if (swatchColor.luminance() > 0.58f) {
                                                Color(0xFF151515)
                                            } else {
                                                Color.White
                                            }

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(42.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(
                                                        if (selectedColor) modalAccent.copy(alpha = 0.16f)
                                                        else Color.Transparent
                                                    )
                                                    .clickable {
                                                        editableSelectedFolders.forEach { folder ->
                                                            onSetFolderColor(folder.id, colorArgb)
                                                        }
                                                        dismissFolderModal { clearSelection() }
                                                    }
                                                    .padding(horizontal = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(26.dp)
                                                        .clip(CircleShape)
                                                        .background(swatchColor)
                                                        .border(
                                                            width = if (selectedColor) 2.5.dp else 1.dp,
                                                            color = if (selectedColor) modalAccent
                                                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                                            shape = CircleShape
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (selectedColor) {
                                                        Icon(
                                                            imageVector = Icons.Filled.Check,
                                                            contentDescription = null,
                                                            tint = if (colorArgb == null) dialogContentColor else checkColor,
                                                            modifier = Modifier.size(15.dp)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = option.name,
                                                    color = dialogContentColor,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Medium,
                                                    maxLines = 1,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }
                                    TextButton(
                                        onClick = { dismissFolderModal() },
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Text("Cancel", color = dialogMutedColor)
                                    }
                                }

                                FolderModalPage.Delete -> {
                                    val deleteCount = editableSelectedFolders.size
                                    Text(
                                        text = if (deleteCount == 1) {
                                            "Are you sure you want to delete this folder? Its notes will also be removed."
                                        } else {
                                            "Are you sure you want to delete these $deleteCount folders? Their notes will also be removed."
                                        },
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = dialogMutedColor
                                    )
                                    FolderModalButtons(
                                        confirmLabel = "Delete",
                                        confirmEnabled = deleteCount > 0,
                                        confirmColor = MaterialTheme.colorScheme.error,
                                        onCancel = { dismissFolderModal() },
                                        onConfirm = {
                                            val folderIds = editableSelectedFolders
                                                .mapTo(linkedSetOf()) { it.id }
                                            onDeleteSelectedFolders(folderIds)
                                            dismissFolderModal { clearSelection() }
                                        }
                                    )
                                }

                                null -> Unit
                            }
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
    viewMode: FolderViewMode,
    palette: NotesPaletteColors?,
    selectionMode: Boolean,
    selected: Boolean,
    onLongClick: () -> Unit,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val normalFolderBack = folder.colorArgb?.let(::Color)
        ?: palette?.noteTint
        ?: if (isDark) Color(0xFF151617) else Color(0xFFE2E9F1)
    val folderFrontBase = palette?.titleRail
        ?: if (isDark) Color(0xFF252B31) else Color(0xFFE6EEF7)
    val folderAccent = palette?.accent ?: if (isDark) Color(0xFF9FDBFF) else Color(0xFF2A79D8)
    val selectedCheckFill = if (isDark) folderAccent.copy(alpha = 0.92f) else folderAccent
    val folderBack = if (selected) {
        palette?.accent?.copy(alpha = if (isDark) 0.42f else 0.28f)
            ?: if (isDark) Color(0xFF4A3E58) else Color(0xFFEDE7F6)
    } else {
        normalFolderBack
    }
    val untreatedFolderFront = if (selected) {
        if (isDark) Color(0xFF3B404A)
        else palette?.titleRail ?: Color(0xFFFAF7FF)
    } else {
        folderFrontBase.copy(alpha = 1f)
    }
    val untreatedFolderFrontDeep = if (selected) {
        if (isDark) Color(0xFF292D35)
        else palette?.actionBarTint ?: Color(0xFFF1ECF7)
    } else {
        if (isDark) Color(0xFF25272C)
        else palette?.actionBarTint ?: Color(0xFFD8E3EE)
    }
    // A low-cost frosted tint keeps large folder grids smooth without one live blur per card.
    val colorFrostAmount = if (folder.colorArgb != null) {
        if (isDark) 0.16f else 0.10f
    } else {
        0f
    }
    val folderFront = colorLerp(untreatedFolderFront, folderBack.copy(alpha = 1f), colorFrostAmount)
    val folderFrontDeep = colorLerp(untreatedFolderFrontDeep, folderBack.copy(alpha = 1f), colorFrostAmount * 0.72f)
    val labelColor = if (isDark) Color.White else Color(0xFF111111)
    val countColor = if (folder.count > 0) {
        folderAccent.copy(alpha = if (isDark) 0.92f else 0.86f)
    } else if (isDark) {
        Color.White.copy(alpha = 0.54f)
    } else {
        Color(0xFF7B8087)
    }
    val countText = if (folder.count == 1) "1 note" else "${folder.count} notes"
    val outerShape = RoundedCornerShape(6)
    val folderBackOpaque = folderBack.copy(alpha = 1f)
    val folderBackRaised = colorLerp(
        folderBackOpaque,
        folderAccent.copy(alpha = 1f),
        if (selected) 0.30f else if (isDark) 0.20f else 0.12f
    )
    val folderSurfaceTop = colorLerp(
        folderBackRaised,
        if (isDark) Color.White else Color.Black,
        if (isDark) 0.08f else 0.035f
    ).copy(alpha = 1f)
    val folderSurfaceBottom = colorLerp(
        folderBackRaised,
        folderFrontDeep.copy(alpha = 1f),
        if (isDark) 0.24f else 0.14f
    ).copy(alpha = 1f)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.98f)
            .shadow(
                elevation = if (isDark) 2.dp else 5.dp,
                shape = outerShape,
                clip = false
            )
            .clip(outerShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Constraint-relative sizing keeps the one paper stack consistent
        // across phones, tablets, and every grid density.
        val paperWidth = maxWidth * 0.90f
        val paperHeight = maxHeight * 0.92f
        val paperTopPadding = maxHeight * 0.04f

        Box(
            modifier = Modifier
                .matchParentSize()
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(folderSurfaceTop, folderSurfaceBottom)
                        )
                    )
            )

            if (folder.count > 0) {
                FolderPaperStack(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = paperTopPadding),
                    width = paperWidth,
                    height = paperHeight,
                    isDark = isDark
                )
            }
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(NotesFolderFrontShape)
                .background(
                    Brush.verticalGradient(
                        listOf(folderFront, folderFrontDeep)
                    )
                )
        )

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
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = countText,
                color = countColor,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (selectionMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .size(17.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (selected) selectedCheckFill else Color.Transparent)
                    .border(
                        1.4.dp,
                        if (selected) selectedCheckFill else labelColor.copy(alpha = 0.62f),
                        RoundedCornerShape(9.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
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
}

@Composable
private fun FolderPaperStack(
    width: Dp,
    height: Dp,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.size(width = width, height = height)) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(width = width * 0.78f, height = height * 0.85f)
                .clip(RoundedCornerShape(7))
                .background(
                    Brush.verticalGradient(
                        if (isDark) listOf(Color(0xFFB9A7E8), Color(0xFF7565A8))
                        else listOf(Color(0xFFE9E0FF), Color(0xFFC9B8F4))
                    )
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(width = width * 0.82f, height = height * 0.90f)
                .clip(RoundedCornerShape(7))
                .background(
                    Brush.verticalGradient(
                        if (isDark) listOf(Color(0xFF82C7D5), Color(0xFF397887))
                        else listOf(Color(0xFFD8F5FA), Color(0xFFA7DBE5))
                    )
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(width = width * 0.78f, height = height * 0.92f)
                .clip(RoundedCornerShape(7))
                .background(
                    Brush.verticalGradient(
                        if (isDark) listOf(Color(0xFFFFE3A3), Color(0xFFC79845))
                        else listOf(Color(0xFFFFF6D9), Color(0xFFFFDFA0))
                    )
                )
        ) {
            val lineColor = if (isDark) Color(0xFF5C4628).copy(alpha = 0.72f)
            else Color(0xFF8A6A35).copy(alpha = 0.56f)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(
                        start = width * 0.12f,
                        top = height * 0.13f,
                        end = width * 0.12f,
                        bottom = height * 0.08f
                    ),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(
                    0.48f, 0.86f, 0.68f, 0.58f, 0.76f,
                    0.90f, 0.64f, 0.82f, 0.72f, 0.88f, 0.54f
                ).forEachIndexed { index, fraction ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .height(if (index == 0) 2.dp else 1.5.dp)
                            .clip(RoundedCornerShape(50))
                            .background(lineColor)
                    )
                }
            }
        }
    }
}

private fun sortFolderRows(
    folders: List<NoteFolderUi>,
    sortMode: FolderSortMode
): List<NoteFolderUi> {
    val comparator = when (sortMode) {
        FolderSortMode.CreatedAscending -> compareBy<NoteFolderUi> { it.createdAt }
        FolderSortMode.CreatedDescending -> compareByDescending { it.createdAt }
        FolderSortMode.ModifiedAscending -> compareBy<NoteFolderUi> { it.modifiedAt }
        FolderSortMode.ModifiedDescending -> compareByDescending { it.modifiedAt }
        FolderSortMode.NameAscending -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
        FolderSortMode.NameDescending -> compareByDescending<NoteFolderUi, String>(String.CASE_INSENSITIVE_ORDER) { it.name }
    }
    return folders.sortedWith(comparator)
}

@Composable
private fun NotesFolderListRow(
    folder: NoteFolderUi,
    palette: NotesPaletteColors?,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val accent = palette?.accent ?: if (isDark) Color(0xFFBB86FC) else Color(0xFF6750A4)
    val rearColor = folder.colorArgb?.let(::Color)
        ?: palette?.noteTint
        ?: if (isDark) Color(0xFF151617) else Color(0xFFE2E9F1)
    val neutralFrontColor = palette?.titleRail
        ?: if (isDark) Color(0xFF252B31) else Color(0xFFE6EEF7)
    val frontColor = colorLerp(
        neutralFrontColor,
        rearColor.copy(alpha = 1f),
        if (folder.colorArgb != null) if (isDark) 0.16f else 0.10f else 0f
    )
    val rowColor = palette?.noteTint ?: if (isDark) Color(0xFF151617) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1D1B20)
    val secondaryColor = if (isDark) Color.White.copy(alpha = 0.64f) else Color(0xFF49454F)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp)
            .shadow(
                elevation = if (isDark) 2.dp else 5.dp,
                shape = RoundedCornerShape(18.dp),
                clip = false
            )
            .clip(RoundedCornerShape(18.dp))
            .background(rowColor.copy(alpha = 1f))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 68.dp, height = 54.dp)
                .clip(RoundedCornerShape(6))
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(rearColor)
                )
                if (folder.count > 0) {
                    FolderPaperStack(
                        width = 62.dp,
                        height = 50.dp,
                        isDark = isDark,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 2.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(NotesFolderFrontShape)
                    .background(frontColor)
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = folder.name,
                color = textColor,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (folder.count == 1) "1 note" else "${folder.count} notes",
                color = secondaryColor,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        if (selectionMode) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (selected) accent else Color.Transparent)
                    .border(1.5.dp, if (selected) accent else secondaryColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun FolderSelectionActionBar(
    backdrop: LayerBackdrop,
    selectedCount: Int,
    editableSelectedCount: Int,
    onRename: () -> Unit,
    onColor: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val showRename = selectedCount == 1 && editableSelectedCount == 1
    val actionsEnabled = editableSelectedCount > 0
    val isDark = isSystemInDarkTheme()
    val barTint = if (isDark) Color(0xE6112733) else Color(0xEE183746)
    val overlayTint = if (isDark) Color(0x66101D26) else Color(0x55112632)
    val contentColor = Color.White.copy(alpha = 0.94f)
    val backdropBlurDp = bottomChromeBackdropBlurDp()
    Box(
        modifier = modifier
            .padding(horizontal = GlassChromeHorizontalPadding)
            .fillMaxWidth()
            .height(56.dp)
            .shadow(GlassChromeShadowElevation, GlassChromeShape, clip = false)
            .clip(GlassChromeShape)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { GlassChromeShape },
                shadow = { bottomChromeShadow() },
                highlight = null,
                effects = {
                    vibrancy()
                    blur(
                        radius = backdropBlurDp.dp.toPx(),
                        edgeTreatment = TileMode.Mirror
                    )
                    lens(
                        refractionHeight = GlassChromeRefractionHeightDp.dp.toPx(),
                        refractionAmount = GlassChromeRefractionAmountDp.dp.toPx(),
                        depthEffect = false,
                        chromaticAberration = false
                    )
                },
                onDrawSurface = { drawRect(barTint) }
            )
            .background(overlayTint, GlassChromeShape)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            if (showRename) {
                FolderSelectionAction(
                    label = "Rename",
                    icon = Icons.Filled.Edit,
                    enabled = true,
                    contentColor = contentColor,
                    onClick = onRename,
                    modifier = Modifier.weight(1f)
                )
            }
            FolderSelectionAction(
                label = "Folder color",
                icon = Icons.Filled.Palette,
                enabled = actionsEnabled,
                contentColor = contentColor,
                onClick = onColor,
                modifier = Modifier.weight(1f)
            )
            FolderSelectionAction(
                label = "Delete",
                icon = Icons.Filled.Delete,
                enabled = actionsEnabled,
                contentColor = contentColor,
                onClick = onDelete,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun FolderSolidModalShell(
    visible: Boolean,
    isDark: Boolean,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    var renderedVisible by remember { mutableStateOf(false) }
    LaunchedEffect(visible) { renderedVisible = visible }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = renderedVisible,
                enter = fadeIn(animationSpec = tween(durationMillis = 130)),
                exit = fadeOut(animationSpec = tween(durationMillis = 160))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = if (isDark) 0.38f else 0.18f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismiss
                        )
                )
            }

            AnimatedVisibility(
                visible = renderedVisible,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .imePadding(),
                enter = slideInVertically(
                    animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing),
                    initialOffsetY = { it / 2 }
                ) + fadeIn(animationSpec = tween(durationMillis = 170)) +
                    scaleIn(
                        animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing),
                        initialScale = 0.94f
                    ),
                exit = slideOutVertically(
                    animationSpec = tween(durationMillis = 210, easing = FastOutLinearInEasing),
                    targetOffsetY = { it / 3 }
                ) + fadeOut(animationSpec = tween(durationMillis = 150)) +
                    scaleOut(
                        animationSpec = tween(durationMillis = 210, easing = FastOutLinearInEasing),
                        targetScale = 0.98f
                    )
            ) {
                Surface(
                    modifier = Modifier
                        .padding(
                            start = GlassChromeHorizontalPadding,
                            end = GlassChromeHorizontalPadding,
                            bottom = GlassChromeHorizontalPadding
                        )
                        .fillMaxWidth(),
                    shape = GlassChromeShape,
                    color = if (isDark) Color(0xFF252528) else Color(0xFFF7F7F9),
                    contentColor = if (isDark) Color.White else Color(0xFF1D1B20),
                    shadowElevation = 18.dp
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun FolderModalButtons(
    confirmLabel: String,
    confirmEnabled: Boolean,
    confirmColor: Color,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onCancel) {
            Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(enabled = confirmEnabled, onClick = onConfirm) {
            Text(
                text = confirmLabel,
                color = confirmColor.copy(alpha = if (confirmEnabled) 1f else 0.38f),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun FolderSelectionAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val actionColor = contentColor.copy(alpha = if (enabled) 1f else 0.38f)
    val pressSource = remember { MutableInteractionSource() }
    val isPressed by pressSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = 720f
        ),
        label = "folderActionPressScale"
    )
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 3.dp, vertical = 4.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(GlassChromeInnerShape)
            .clickable(
                enabled = enabled,
                interactionSource = pressSource,
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = label, tint = actionColor, modifier = Modifier.size(20.dp))
        Text(label, color = actionColor, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

private val NotesFolderFrontShape = GenericShape { size, _ ->
    val leftTopY = size.height * 0.30f
    val rightTopY = size.height * 0.38f
    val tabStartX = size.width * 0.40f
    val curveEndX = size.width * 0.64f
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
    lineTo(size.width, size.height)
    lineTo(0f, size.height)
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
    val appPalette = LocalAppThemePalette.current
    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }
    Box(
        modifier = Modifier
            .size(NotesActionHeight)
            .notesLiquidTransform(CircleShape, interactiveHighlight)
            .notesActionGlass(backdrop, CircleShape, isDark, palette, appPalette)
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
            modifier = Modifier.size(23.dp)
        )
    }
}

@Composable
private fun NotesGlassFolderMenuButton(
    backdrop: LayerBackdrop,
    palette: NotesPaletteColors?,
    contentColor: Color,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val appPalette = LocalAppThemePalette.current
    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }
    Box(
        modifier = Modifier
            .size(NotesActionHeight)
            .notesLiquidTransform(CircleShape, interactiveHighlight)
            .notesActionGlass(backdrop, CircleShape, isDark, palette, appPalette)
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
            imageVector = Icons.Filled.Menu,
            contentDescription = "Folder menu",
            tint = contentColor,
            modifier = Modifier.size(25.dp)
        )
    }
}

@Composable
private fun NotesSyncStatusPill(
    syncOnline: Boolean,
    syncAvailable: Boolean,
    status: NotesSyncUiStatus,
    backdrop: LayerBackdrop,
    palette: NotesPaletteColors?,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val appPalette = LocalAppThemePalette.current

    val online = syncOnline && syncAvailable
    val active = online && status != NotesSyncUiStatus.Error

    val statusLabel = if (active) "Online" else "Local"

    // Stronger colors for better visibility in both themes
    val dotColor = when {
        status == NotesSyncUiStatus.Error -> {
            if (isDark) Color(0xFFFF6B63) else Color(0xFFD92D20)
        }

        online -> {
            if (isDark) Color(0xFF4ADE80) else Color(0xFF07883E)
        }

        else -> {
            if (isDark) Color(0xFFFFD166) else Color(0xFFA95E00)
        }
    }

    val readableTextColor = if (isDark) {
        Color.White.copy(alpha = 0.96f)
    } else {
        Color(0xFF15121F)
    }

    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }

    val syncPulse by rememberInfiniteTransition(
        label = "notes_sync_status_pulse"
    ).animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1250,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "notes_sync_status_pulse_value"
    )

    Box(
        modifier = modifier
            // Original pill size remains unchanged
            .height(NotesActionHeight)
            .widthIn(min = 104.dp, max = 136.dp)
            .notesLiquidTransform(
                NotesPillShape,
                interactiveHighlight
            )
            .notesActionGlass(
                backdrop,
                NotesPillShape,
                isDark,
                palette,
                appPalette
            )
            .then(interactiveHighlight.modifier)
            .then(interactiveHighlight.gestureModifier),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Larger drawing area, but it does not enlarge the pill
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                if (active) {
                    Canvas(modifier = Modifier.size(24.dp)) {
                        val outerRadius = lerp(
                            6.dp.toPx(),
                            11.5.dp.toPx(),
                            syncPulse
                        )

                        val innerRadius = lerp(
                            5.dp.toPx(),
                            8.dp.toPx(),
                            syncPulse
                        )

                        // Larger and stronger outer pulse
                        drawCircle(
                            color = dotColor.copy(
                                alpha = lerp(0.48f, 0f, syncPulse)
                            ),
                            radius = outerRadius
                        )

                        // Softer secondary glow
                        drawCircle(
                            color = dotColor.copy(
                                alpha = lerp(0.34f, 0.06f, syncPulse)
                            ),
                            radius = innerRadius
                        )
                    }
                }

                // Dark outline keeps the dot readable on pale glass
                Box(
                    modifier = Modifier
                        .size(13.dp)
                        .clip(CircleShape)
                        .background(
                            if (isDark) {
                                Color.Black.copy(alpha = 0.28f)
                            } else {
                                Color.White.copy(alpha = 0.92f)
                            }
                        )
                )

                // Actual dot: 8 dp → 10 dp
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }

            Spacer(Modifier.width(7.dp))

            Text(
                text = statusLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black
                ),
                color = readableTextColor,
                maxLines = 1
            )
        }
    }
}

private val NotesActionHeight = 40.dp
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
    appPalette: AppThemePalette
): Modifier {
    val barTint = palette?.actionBarTint ?: appPalette.glass
    val barIsLight = barTint.luminance() > 0.5f
    val baseTint = if (barIsLight) {
        colorLerp(Color.Black, palette?.accent ?: appPalette.action, 0.34f)
    } else {
        colorLerp(Color.White, palette?.accent ?: appPalette.actionContent, 0.38f)
    }
    val hueTint = baseTint.copy(alpha = if (palette != null) 0.46f else if (isDark) 0.36f else 0.44f)
    val visibleTint = baseTint.copy(alpha = if (palette != null) 0.22f else if (isDark) 0.18f else 0.20f)
    return drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        highlight = null,
        effects = {
            vibrancy()
            blur(radius = 2.dp.toPx(), edgeTreatment = TileMode.Mirror)
            lens(12.dp.toPx(), 24.dp.toPx())
        },
        onDrawSurface = {
            drawRect(hueTint, blendMode = BlendMode.Hue)
            drawRect(visibleTint)
        }
    )
        .clip(shape)
}
