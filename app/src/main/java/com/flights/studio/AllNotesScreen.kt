@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package com.flights.studio

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.SplitButtonShapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.flights.studio.com.flights.studio.BadgePillTopBar
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
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
    onBack: (() -> Unit)? = null
) {
    var searchActive by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    val selectionMode = selectedIds.isNotEmpty()


    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()

    fun toggleSelected(key: String) {
        val next = if (selectedIds.contains(key)) selectedIds - key else selectedIds + key
        selectedIds = next
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

                // adapter still based on note text list
                notesAdapter.updateList(notes.map { it.text })

                remindersTick++
            }
        }

        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    Scaffold(
        topBar = {
            val topBarShape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
            val isDark = isSystemInDarkTheme()
            val scheme = MaterialTheme.colorScheme

            // Top bar glass tint
            val tint = scheme.surface.copy(alpha = if (isDark) 0.92f else 0.80f)

            // Glass fill for split buttons
            val glassFill = scheme.surfaceVariant.copy(alpha = if (isDark) 0.35f else 0.25f)
            val glassContent = scheme.onSurface

            val btnColors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = glassContent,
                disabledContainerColor = Color.Transparent,
                disabledContentColor = glassContent.copy(alpha = 0.40f)
            )

            // --- pressed tracking (so clip follows pressed shape) ---
            // --- pressed tracking (hoisted) ---
            val leadIS = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            val trailIS = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            val leadPressed by leadIS.collectIsPressedAsState()
            val trailPressed by trailIS.collectIsPressedAsState()
            val hlColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)

            val leadPressT by animateFloatAsState(
                targetValue = if (leadPressed) 1f else 0f,
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 90),
                label = "leadPressT"
            )
            val trailPressT by animateFloatAsState(
                targetValue = if (trailPressed) 1f else 0f,
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 90),
                label = "trailPressT"
            )

            val shapeT by animateFloatAsState(
                targetValue = if (menuOpen) 1f else 0f,
                label = "splitShapeT"
            )

            fun lerpDp(a: Dp, b: Dp, t: Float): Dp = a + (b - a) * t

            val outer = 50.dp
            val innerClosed = 5.dp
            val innerOpen = 24.dp
            val pressedInner = 14.dp

            val rightInnerBase = lerpDp(innerClosed, innerOpen, shapeT)

            val leftInner = lerpDp(innerClosed, pressedInner, leadPressT)
            val rightInner = lerpDp(rightInnerBase, pressedInner, trailPressT)

            val leftEffectiveShape = RoundedCornerShape(
                topStart = outer,
                bottomStart = outer,
                topEnd = leftInner,
                bottomEnd = leftInner
            )

            val rightEffectiveShape = RoundedCornerShape(
                topStart = rightInner,
                bottomStart = rightInner,
                topEnd = outer,
                bottomEnd = outer
            )

            val leftShapes = SplitButtonShapes(
                shape = leftEffectiveShape,
                pressedShape = leftEffectiveShape,
                checkedShape = leftEffectiveShape
            )

            val rightShapes = SplitButtonShapes(
                shape = rightEffectiveShape,
                pressedShape = rightEffectiveShape,
                checkedShape = rightEffectiveShape
            )


            Surface(
                shape = topBarShape,
                color = Color.Transparent,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                modifier = Modifier.drawBackdrop(
                    backdrop = topBarBackdrop,
                    shape = { topBarShape },
                    highlight = {
                        Highlight(
                            width = 0.50.dp,
                            blurRadius = 1.dp,
                            alpha = 0.20f,
                            style = HighlightStyle.Ambient,
                        )
                    },
                    effects = {
                        blur(
                            radius = 1f.dp.toPx(),
                            edgeTreatment = TileMode.Mirror
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
                androidx.compose.material3.CenterAlignedTopAppBar(
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

                        Box(Modifier.wrapContentSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = titleText,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 1
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 23.dp, y = (-8).dp)
                            ) {
                                BadgePillTopBar(text = badgeText)
                            }
                        }
                    },
                    actions = {
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
                                        onClick = { onAddNote() },
                                        colors = btnColors,
                                        shapes = leftShapes,
                                        interactionSource = leadIS,
                                        modifier = Modifier
                                            .zIndex(1f)
                                            .clip(leftEffectiveShape)
                                            .drawBackdrop(
                                                backdrop = topBarBackdrop,
                                                shape = { leftEffectiveShape },
                                                shadow = null,
                                                highlight = {
                                                    Highlight(
                                                        width = 0.50.dp,
                                                        blurRadius = 1.dp,
                                                        alpha = 0.96f,
                                                        style = HighlightStyle.Plain(color = hlColor)
                                                    )
                                                },
                                                effects = { blur(radius = 8f.dp.toPx(), edgeTreatment = TileMode.Clamp) },
                                                onDrawSurface = { drawRect(glassFill) }
                                            )
                                    ) { Text("Add") }
                                },
                                trailingButton = {
                                    val rotation by animateFloatAsState(
                                        targetValue = if (menuOpen) 180f else 0f,
                                        label = "splitArrow"
                                    )

                                    SplitButtonDefaults.TrailingButton(
                                        checked = menuOpen,
                                        onCheckedChange = { menuOpen = it },
                                        enabled = true,
                                        colors = btnColors,
                                        shapes = rightShapes,
                                        interactionSource = trailIS,
                                        modifier = Modifier
                                            .zIndex(0f)
                                            .clip(rightEffectiveShape)
                                            .drawBackdrop(
                                                backdrop = topBarBackdrop,
                                                shape = { rightEffectiveShape },
                                                shadow = null,
                                                highlight = {
                                                    Highlight(
                                                        width = 0.50.dp,
                                                        blurRadius = 1.dp,
                                                        alpha = 0.96f,
                                                        style = HighlightStyle.Plain(color = hlColor)
                                                    )
                                                },
                                                effects = { blur(radius = 8f.dp.toPx(), edgeTreatment = TileMode.Clamp) },
                                                onDrawSurface = { drawRect(glassFill) }
                                            )
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
                                        shapes = MenuDefaults.itemShape(index = 0, count = itemCount),
                                        colors = MenuDefaults.itemColors(),
                                        trailingIcon = { Icon(Icons.Filled.Search, null) }
                                    )

                                    DropdownMenuItem(
                                        selected = false,
                                        onClick = {
                                            menuOpen = false
                                            onNavItemClick(R.id.nav_settings)
                                        },
                                        text = { Text("Settings") },
                                        shapes = MenuDefaults.itemShape(index = 1, count = itemCount),
                                        colors = MenuDefaults.itemColors(),
                                        trailingIcon = { Icon(Icons.Filled.Settings, null) }
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
                                            shapes = MenuDefaults.itemShape(index = 2, count = itemCount),
                                            colors = MenuDefaults.itemColors(),
                                            trailingIcon = { Icon(Icons.Filled.Delete, null) }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.width(8.dp))
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
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
                ProfileBackdropImageLayer(
                    modifier = Modifier
                        .matchParentSize()
                        .layerBackdrop(itemBackdrop),
                    lightRes = R.drawable.lightgridpattern,
                    darkRes = R.drawable.darkgridpattern,
                    imageAlpha = if (isDark) 1f else 0.8f,
                    scrimDark = 0f,
                    scrimLight = 0f
                )

                val headerText =
                    when {
                        selectionMode -> {
                            val n = selectedIds.size
                            if (n == 1) "1 note selected" else "$n notes selected"
                        }

                        searchActive -> "Filtering results"
                        else -> "$notesSize notes"
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
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                text = headerText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                                modifier = Modifier.padding(start = 3.dp)
                            )
                        }

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
                        item {
                            Text(
                                text = headerText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                                modifier = Modifier.padding(start = 3.dp)
                            )
                        }

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
                    onContinue = { onAddNote() },
                    onSecondary = { showWelcome = false }
                )

            }
        }
    }
}
