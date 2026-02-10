@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package com.flights.studio

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flights.studio.com.flights.studio.BadgePillTopBar
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle

data class NoteRow(
    val id: String,
    val text: String,
    val imagesCount: Int,
    val title: String,
    val hasReminder: Boolean,
    val hasBadge: Boolean
)



@Suppress("AssignedValueIsNeverRead")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class,
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

    onBack: (() -> Unit)? = null
) {
    var searchActive by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    val selectionMode = selectedIds.isNotEmpty()
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    var lastSize by remember { mutableIntStateOf(notes.size) }


    fun toggleSelected(key: String) {
        val next = if (selectedIds.contains(key)) selectedIds - key else selectedIds + key
        selectedIds = next

        // ✅ keep adapter selection mode consistent with Compose
        // (this avoids “can’t open until refresh” after deselect)
        if (next.isEmpty()) {
            notesAdapter.clearSelection()
        }
    }

    val itemBackdrop = rememberLayerBackdrop()
    val topBarBackdrop = rememberLayerBackdrop()


    LaunchedEffect(itemBackdrop) {
        notesAdapter.setBackdrop(itemBackdrop)
    }
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences(NotesPagePrefs.NAME, Context.MODE_PRIVATE) }

    var twoCols by remember {
        mutableStateOf(
            prefs.getBoolean(
                NotesPagePrefs.KEY_TWO_COLUMNS,
                false
            )
        )
    }
    LaunchedEffect(notes.size, twoCols, selectionMode, searchActive) {
        val newSize = notes.size
        val inserted = newSize > lastSize
        lastSize = newSize

        if (inserted && !selectionMode && !searchActive) {
            if (twoCols) {
                gridState.animateScrollToItem(0)
            } else {
                listState.animateScrollToItem(0)
            }
        }
    }


    DisposableEffect(prefs) {

        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, k ->

            // 1) Grid toggle (you already do this)
            if (k == NotesPagePrefs.KEY_TWO_COLUMNS) {
                twoCols = prefs.getBoolean(NotesPagePrefs.KEY_TWO_COLUMNS, false)
            }

            // 2) Any UI-affecting settings => refresh adapter + rebind items
            val affectsNotesUi =

                k == NotesPagePrefs.KEY_TWO_COLUMNS ||
                        k == NotesPagePrefs.KEY_COMPACT ||
                        k == NotesPagePrefs.KEY_SHOW_IMAGES_BADGE ||
                        k == NotesPagePrefs.KEY_SHOW_REMINDER_BADGE ||
                        k == NotesPagePrefs.KEY_SHOW_REMINDER_BELL ||
                        k == NotesPagePrefs.KEY_TITLE_TOP_COMPACT ||
                        k == NotesPagePrefs.KEY_TITLE_TOP_NORMAL ||
                        k == NotesPagePrefs.KEY_SORT

            if (affectsNotesUi) {
                // read fresh settings
                val s = ctx.readNotesPageSettings()

                // push flags into adapter
                notesAdapter.applyPageSettings(
                    compact = s.compact,
                    showImagesBadge = s.showImagesBadge,
                    showReminderBadge = s.showReminderBadge,
                    showReminderBell = s.showReminderBell,
                    titleTopCompactDp = s.titleTopCompactDp,
                    titleTopNormalDp = s.titleTopNormalDp,

                    )


            }
        }

        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            // ✅ topBar
            val topBarShape = RoundedCornerShape(32.dp)
            val isDark = isSystemInDarkTheme()
            Surface(
                shape = topBarShape,
                color = Color.Transparent,
                modifier = Modifier
                    .drawBackdrop(
                        backdrop = topBarBackdrop, // ✅ blurred scrolling content
                        shape = { topBarShape },
                        highlight = {
                            if (isDark) {
                                Highlight(
                                    width = 0.30.dp,
                                    blurRadius = 0.5.dp,
                                    alpha = 0.20f,
                                    style = HighlightStyle.Plain
                                )
                            } else {
                                Highlight(
                                    width = 0.30.dp,
                                    blurRadius = 0.5.dp,
                                    alpha = 0.20f,
                                    style = HighlightStyle.Plain // very subtle
                                )
                            }
                        },
                        effects = {
                            vibrancy()
                            colorControls(
                                brightness = if (isDark) 0.06f else 0.00f,
                                contrast = if (isDark) 1.06f else 1.00f,
                                saturation = if (isDark) 1.14f else 1.10f
                            )
                            blur(
                                radius = 8f.dp.toPx(),
                                edgeTreatment = TileMode.Clamp
                            )

                            lens(
                                refractionHeight = 24f.dp.toPx(),
                                refractionAmount = 24f.dp.toPx(),
                                depthEffect = true,
                                chromaticAberration = false
                            )
                        },
                        onDrawSurface = {
                            drawRect(Color.White.copy(alpha = 0.02f))
                        }
                    )
            ) {
                CenterAlignedTopAppBar(
                    navigationIcon = {
                        if (onBack != null) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                                    contentDescription = "Back"
                                )
                            }
                        }
                    },
                    title = {
                        val titleText = when {
                            selectionMode -> "selected"
                            searchActive -> "Search"
                            else -> "Notes"
                        }

                        val badgeText = when {
                            selectionMode -> selectedIds.size.toString()
                            searchActive -> "Filtering"
                            else -> notesSize.toString()
                        }

                        Box(
                            modifier = Modifier.wrapContentSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = titleText,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 1
                            )

                            // ✅ badge on the top-right corner of the TITLE
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)          // ✅ BoxScope align
                                    .offset(x = 23.dp, y = (-8).dp)   // ✅ move outside corner
                            ) {
                                BadgePillTopBar(text = badgeText)
                            }
                        }
                    },
                    actions = {
                        // ✅ SplitButton like AddNote (unchanged)
                        val scale by animateFloatAsState(
                            targetValue = if (menuOpen) 1.00f else 1f,
                            label = "notesSplitScale"
                        )

                        Box(
                            Modifier
                                .wrapContentSize(Alignment.TopEnd)
                                .graphicsLayer { scaleX = scale; scaleY = scale }
                        ) {
                            SplitButtonLayout(
                                leadingButton = {
                                    SplitButtonDefaults.LeadingButton(
                                        enabled = true,
                                        onClick = { onAddNote() }
                                    ) {
                                        Text("Add")
                                    }
                                },
                                trailingButton = {
                                    val rotation by animateFloatAsState(
                                        targetValue = if (menuOpen) 180f else 0f,
                                        label = "splitArrow"
                                    )

                                    SplitButtonDefaults.TrailingButton(
                                        checked = menuOpen,
                                        onCheckedChange = { menuOpen = it },
                                        enabled = true
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.KeyboardArrowDown,
                                            contentDescription = "More",
                                            modifier = Modifier
                                                .size(SplitButtonDefaults.TrailingIconSize)
                                                .graphicsLayer { rotationZ = rotation }
                                        )
                                    }
                                }
                            )

                            DropdownMenuPopup(
                                expanded = menuOpen,
                                onDismissRequest = { menuOpen = false }
                            ) {
                                val showDeleteMenu = selectedIds.isNotEmpty()
                                val itemCount = if (showDeleteMenu) 3 else 2

                                DropdownMenuGroup(
                                    shapes = MenuDefaults.groupShape(index = 0, count = 1),
                                    containerColor = MenuDefaults.groupVibrantContainerColor
                                ) {
                                    DropdownMenuItem(
                                        selected = false,
                                        onClick = {
                                            menuOpen = false
                                            searchActive = true
                                            onOpenSearch { searchActive = false }
                                        },
                                        text = { Text("Search") },
                                        shapes = MenuDefaults.itemShape(
                                            index = 0,
                                            count = itemCount
                                        ),
                                        colors = MenuDefaults.itemColors(),
                                        trailingIcon = {
                                            Icon(
                                                Icons.Filled.Search,
                                                contentDescription = null
                                            )
                                        }
                                    )

                                    DropdownMenuItem(
                                        selected = false,
                                        onClick = {
                                            menuOpen = false
                                            onNavItemClick(R.id.nav_settings)
                                        },
                                        text = { Text("Settings") },
                                        shapes = MenuDefaults.itemShape(
                                            index = 1,
                                            count = itemCount
                                        ),
                                        colors = MenuDefaults.itemColors(),
                                        trailingIcon = {
                                            Icon(
                                                Icons.Filled.Settings,
                                                contentDescription = null
                                            )
                                        }
                                    )

                                    if (showDeleteMenu) {
                                        DropdownMenuItem(
                                            selected = false,
                                            onClick = {
                                                menuOpen = false
                                                onDeleteSelected(selectedIds)
                                                selectedIds = emptySet()
                                                notesAdapter.clearSelection()
                                            },
                                            text = { Text("Delete selected") },
                                            shapes = MenuDefaults.itemShape(
                                                index = 2,
                                                count = itemCount
                                            ),
                                            colors = MenuDefaults.itemColors(),
                                            trailingIcon = {
                                                Icon(
                                                    Icons.Filled.Delete,
                                                    contentDescription = null
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.width(8.dp))
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
               .layerBackdrop(topBarBackdrop) // ✅ captures EVERYTHING behind the top bar (list + scroll)
        ) {
            val isDark = isSystemInDarkTheme()
            ProfileBackdropImageLayer(
                modifier = Modifier
                    .matchParentSize()
                    .layerBackdrop(itemBackdrop), // ✅ EXACT like Kyant: capture the background layer itself
                lightRes = R.drawable.notes_pattern_dark_light,
                darkRes  = R.drawable.notes_pattern_dark_light,
                imageAlpha = if (isDark) 1f else 0.8f,
                scrimDark = 0f,
                scrimLight = 0f
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
             ) {
                val headerText =
                    when {
                        selectionMode -> {
                            val n = selectedIds.size
                            if (n == 1) "1 note selected" else "$n notes selected"
                        }
                        searchActive -> "Filtering results"
                        else -> "$notesSize notes"
                    }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {

                    // ✅ tick for async title updates
                    var titleTick by remember { mutableIntStateOf(0) }

                    // ✅ settings are STATE (so compact/grid changes refresh)
                    var s by remember(ctx, prefs) { mutableStateOf(ctx.readNotesPageSettings()) }
                    val notesSnapshot by remember {
                        derivedStateOf { notes.toList() } // ✅ recomputes when notes changes
                    }

                    val safeNotesSnapshot by remember {
                        derivedStateOf { notesSnapshot.distinctBy { it.id } } // ✅ also updates
                    }

                    val droppedCount = notesSnapshot.size - safeNotesSnapshot.size
                    if (droppedCount > 0) {
                        android.util.Log.e("NOTES", "DUPLICATE IDS dropped=$droppedCount")
                    }


                    // ✅ preload reminder maps (like RV) + force one recompose after load
                    var remindersTick by remember { mutableIntStateOf(0) }
                    LaunchedEffect(notes.size) {
                        notesAdapter.preloadReminderFlags(ctx)
                        notesAdapter.preloadBadgeStates(ctx)
                        remindersTick++
                    }


                    // ✅ listen to settings changes and refresh UI without restart
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
                                        k == NotesPagePrefs.KEY_SORT

                            if (affectsNotesUi) {
                                // refresh settings state
                                s = ctx.readNotesPageSettings()

                                // keep adapter in sync
                                notesAdapter.applyPageSettings(
                                    compact = s.compact,
                                    showImagesBadge = s.showImagesBadge,
                                    showReminderBadge = s.showReminderBadge,
                                    showReminderBell = s.showReminderBell,
                                    titleTopCompactDp = s.titleTopCompactDp,
                                    titleTopNormalDp = s.titleTopNormalDp,
                                )

                                // ✅ IMPORTANT: adapter still works with note TEXT, so pass text list
                                notesAdapter.updateList(notes.map { it.text })

                                // force recomposition for badge/bell/imagesCount which come from adapter fields
                                remindersTick++
                            }
                        }

                        prefs.registerOnSharedPreferenceChangeListener(listener)
                        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
                    }
                    val topPad = padding.calculateTopPadding()
                    val bottomPad = padding.calculateBottomPadding()

                    if (twoCols) {
                        LazyVerticalGrid(
                            state = gridState,
                            columns = GridCells.Fixed(2),
                            modifier = Modifier
                                .fillMaxSize()
                                .windowInsetsPadding(WindowInsets.navigationBars),
                            contentPadding = PaddingValues(
                                start = 12.dp,
                                end = 12.dp,
                                top = topPad + 12.dp,          // ✅ below top bar
                                bottom = bottomPad + 12.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                        ) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Text(
                                    text = headerText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                                    modifier = Modifier.padding(start = 3.dp)
                                )
                            }

                            itemsIndexed(
                                items = safeNotesSnapshot,
                                key = { _, row -> row.id } // ✅ stable per note
                            ) { _, row ->

                                val note = row.text
                                val rowKey = row.id
                                val selected = selectedIds.contains(rowKey)
//                                remindersTick // forces refresh when reminder maps load/update

                                NoteItem(
                                    title = row.title,note = note,
                                    compact = s.compact,
                                    dense = s.compact,
                                    selectionMode = selectionMode,
                                    selected = selected,

                                    showReminderBell = row.hasReminder,
                                    showReminderBadge = row.hasBadge,

                                    notesAdapter.imagesCount(note),

                                    onClick = {
                                        if (selectionMode) {
                                            // ✅ Let Activity toggle selection (updates isMultiSelectMode + selectedKeys)
                                            notesAdapter.fireClick(note)
                                            // ✅ Keep Compose UI in sync
                                            toggleSelected(rowKey)
                                        } else {
                                            notesAdapter.fireClick(note)
                                        }
                                    },
                                    onLongClick = {
                                        // ✅ Enter selection mode via Activity (sets isMultiSelectMode = true)
                                        notesAdapter.fireLongClick(note)

                                        // ✅ Keep Compose UI in sync
                                        toggleSelected(rowKey)
                                    },


                                    onEdit = { notesAdapter.fireEdit(note) },
                                    onReminderClick = { notesAdapter.fireReminder(note) },

                                    backdrop = itemBackdrop,
                                    titleTopCompactDp = s.titleTopCompactDp,
                                    titleTopNormalDp = s.titleTopNormalDp,
                                )
                            }
                        }
                    } else {
                        CompositionLocalProvider(LocalOverscrollConfiguration provides null) {

                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .windowInsetsPadding(WindowInsets.navigationBars),
                                contentPadding = PaddingValues(
                                    start = 12.dp,
                                    end = 12.dp,
                                    top = topPad + 12.dp,          // ✅ below top bar
                                    bottom = bottomPad + 12.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                item {
                                    Text(
                                        text = headerText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = 0.72f
                                        ),
                                        modifier = Modifier.padding(start = 3.dp)
                                    )
                                }
                                itemsIndexed(
                                    items = safeNotesSnapshot,
                                    key = { _, row -> row.id } // ✅ stable per note
                                ) { _, row ->

                                    val note = row.text
                                    val rowKey = row.id

                                    val title = notesAdapter.titleNow(note)
                                    LaunchedEffect(rowKey, titleTick) {
                                        notesAdapter.requestTitleIfNeeded(note) { titleTick++ }
                                    }

                                    val selected = selectedIds.contains(rowKey)

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
                                        notesAdapter.imagesCount(note),

                                        onClick = {
                                            if (selectionMode) {
                                                notesAdapter.fireClick(note)
                                                toggleSelected(rowKey)
                                            } else {
                                                notesAdapter.fireClick(note)
                                            }
                                        },
                                        onLongClick = {
                                            notesAdapter.fireLongClick(note)
                                            toggleSelected(rowKey)
                                        },


                                        onEdit = { notesAdapter.fireEdit(note) },
                                        onReminderClick = { notesAdapter.fireReminder(note) },

                                        backdrop = itemBackdrop,
                                        titleTopCompactDp = s.titleTopCompactDp,
                                        titleTopNormalDp = s.titleTopNormalDp,
                                    )
                                }
                            }
                        }
                    }
                }
            }


            // ✅ Empty hint overlay (if you want it inside clip)
            val isEmpty = notes.isEmpty()
            var showEmptyHint by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { showEmptyHint = isEmpty }
            LaunchedEffect(isEmpty) { showEmptyHint = isEmpty }

            val hintColor = if (isSystemInDarkTheme()) Color.White else Color.Black

            androidx.compose.animation.AnimatedVisibility(
                visible = showEmptyHint,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 14.dp, bottom = 14.dp),
                enter = slideInHorizontally { -it / 2 } + fadeIn(),
                exit = slideOutHorizontally { -it / 2 } + fadeOut()
            ) {
                Text(
                    text = stringResource(R.string.add_your_first_note),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = hintColor
                )
            }
        }
    }
}
