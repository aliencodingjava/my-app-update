package com.flights.studio

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun NotesSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(NotesPagePrefs.NAME, Context.MODE_PRIVATE) }

    var compact by remember { mutableStateOf(prefs.getBoolean(NotesPagePrefs.KEY_COMPACT, false)) }
    var twoColumns by remember { mutableStateOf(prefs.getBoolean(NotesPagePrefs.KEY_TWO_COLUMNS, false)) }
    var paletteScreenOpen by rememberSaveable { mutableStateOf(false) }
    var paletteEnabled by remember {
        mutableStateOf(prefs.getBoolean(NotesPagePrefs.KEY_PALETTE_ENABLED, false))
    }
    var paletteId by remember {
        mutableStateOf(
            prefs.getString(NotesPagePrefs.KEY_PALETTE_ID, NotesPagePrefs.DEFAULT_PALETTE_ID)
                ?: NotesPagePrefs.DEFAULT_PALETTE_ID
        )
    }

    var showImagesBadge by remember {
        mutableStateOf(prefs.getBoolean(NotesPagePrefs.KEY_SHOW_IMAGES_BADGE, true))
    }
    var showReminderBadge by remember {
        mutableStateOf(prefs.getBoolean(NotesPagePrefs.KEY_SHOW_REMINDER_BADGE, true))
    }
    var showReminderBell by remember {
        mutableStateOf(prefs.getBoolean(NotesPagePrefs.KEY_SHOW_REMINDER_BELL, true))
    }
    var enableTitleTips by remember {
        mutableStateOf(
            prefs.getBoolean(
                NotesPagePrefs.KEY_ENABLE_TITLE_TIPS,
                NotesPagePrefs.DEFAULT_ENABLE_TITLE_TIPS
            )
        )
    }
    var syncOnline by remember {
        mutableStateOf(
            prefs.getBoolean(
                NotesPagePrefs.KEY_SYNC_ONLINE,
                NotesPagePrefs.DEFAULT_SYNC_ONLINE
            )
        )
    }
    val scope = rememberCoroutineScope()
    var resetActionText by remember { mutableStateOf("Reset") }

    var sortMode by remember {
        mutableStateOf(
            prefs.getString(NotesPagePrefs.KEY_SORT, NotesPagePrefs.SORT_NEWEST)
                ?: NotesPagePrefs.SORT_NEWEST
        )
    }

    var titleTopPadding by remember {
        mutableIntStateOf(
            prefs.getInt(
                NotesPagePrefs.KEY_TITLE_TOP_COMPACT,
                NotesPagePrefs.DEFAULT_TITLE_TOP_COMPACT
            ).coerceIn(NotesPagePrefs.TITLE_TOP_MIN_DP, NotesPagePrefs.TITLE_TOP_MAX_DP)
        )
    }

    val topBarShape = RoundedCornerShape(0.dp)
    val isDark = isSystemInDarkTheme()
    val scheme = MaterialTheme.colorScheme
    val glassTint = glassChromeTint()
    val glassOverlayTint = glassChromeOverlayTint()
    val actionBarContentColor = if (isDark) Color.White else Color(0xFF111111)
    val primaryText = if (isDark) Color.White else scheme.onSurface
    val secondaryText = if (isDark) Color.White.copy(alpha = 0.78f) else scheme.onSurfaceVariant
    val mutedText = if (isDark) Color.White.copy(alpha = 0.58f) else scheme.onSurfaceVariant.copy(alpha = 0.7f)
    val actionBarColor = topActionBarTint()

    val topBarBackdrop = rememberLayerBackdrop()
    if (paletteScreenOpen) {
        NotesPalettePickerScreen(
            initialEnabled = paletteEnabled,
            initialPaletteId = paletteId,
            isDark = isDark,
            primaryText = primaryText,
            secondaryText = secondaryText,
            mutedText = mutedText,
            glassTint = glassTint,
            glassOverlayTint = glassOverlayTint,
            onBack = { paletteScreenOpen = false },
            onApply = { enabled, id ->
                paletteEnabled = enabled
                paletteId = id
                prefs.edit {
                    putBoolean(NotesPagePrefs.KEY_PALETTE_ENABLED, enabled)
                    putString(NotesPagePrefs.KEY_PALETTE_ID, id)
                }
                paletteScreenOpen = false
            }
        )
        return
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Surface(
                shape = topBarShape,
                color = Color.Transparent,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
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
                        onDrawSurface = { drawRect(actionBarColor) }
                    )
            ) {
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(start = 4.dp, end = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                            contentDescription = "Back",
                            tint = actionBarContentColor
                        )
                    }
                    Text(
                        text = "Customize Notes",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = actionBarContentColor,
                        maxLines = 1
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(topBarBackdrop)
        ) {
            val itemBackdrop = rememberLayerBackdrop()

            ProfileBackdropImageLayer(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(itemBackdrop),
                lightRes = R.drawable.light_grid_pattern,
                darkRes = R.drawable.dark_grid_pattern,
                imageAlpha = if (isDark) 1f else 0.8f,
                scrimDark = 0f,
                scrimLight = 0f
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 8.dp,
                    end = 8.dp,
                    top = padding.calculateTopPadding() + 16.dp,
                    bottom = 18.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    SettingsTintCard {
                        Text("Appearance", style = MaterialTheme.typography.titleMedium, color = primaryText)

                        SettingsSwitchRow(
                            title = "Compact rows",
                            checked = compact,
                            secondaryText = secondaryText
                        ) {
                            compact = it
                            prefs.edit { putBoolean(NotesPagePrefs.KEY_COMPACT, it) }
                        }

                        SettingsSwitchRow(
                            title = "Two columns grid",
                            checked = twoColumns,
                            secondaryText = secondaryText
                        ) {
                            twoColumns = it
                            prefs.edit { putBoolean(NotesPagePrefs.KEY_TWO_COLUMNS, it) }
                        }

                        Text("Title position: ${titleTopPadding}dp", color = secondaryText)
                        Slider(
                            value = titleTopPadding.toFloat(),
                            onValueChange = { v ->
                                titleTopPadding = v.toInt()
                                prefs.edit {
                                    putInt(NotesPagePrefs.KEY_TITLE_TOP_COMPACT, titleTopPadding)
                                    putInt(NotesPagePrefs.KEY_TITLE_TOP_NORMAL, titleTopPadding)
                                }
                            },
                            valueRange = NotesPagePrefs.TITLE_TOP_MIN_DP.toFloat()..NotesPagePrefs.TITLE_TOP_MAX_DP.toFloat(),
                            steps = NotesPagePrefs.TITLE_TOP_MAX_DP - NotesPagePrefs.TITLE_TOP_MIN_DP - 1
                        )
                    }
                }

                item {
                    SettingsTintCard {
                        Text("Sync", style = MaterialTheme.typography.titleMedium, color = primaryText)

                        SettingsSwitchRow(
                            title = "Sync online",
                            checked = syncOnline,
                            secondaryText = secondaryText,
                            subtitle = if (syncOnline) {
                                "Save and restore notes with your account."
                            } else {
                                "Sync deactivated. Turn on to activate online sync."
                            },
                            mutedText = mutedText
                        ) { on ->
                            syncOnline = on
                            prefs.edit { putBoolean(NotesPagePrefs.KEY_SYNC_ONLINE, on) }
                            if (on) {
                                context.getSharedPreferences("notes_meta", Context.MODE_PRIVATE).edit {
                                    remove("last_sync_at")
                                }
                            }
                        }
                    }
                }

                item {
                    SettingsTintCard(compact = true) {
                        Text("Theme color", style = MaterialTheme.typography.titleMedium, color = primaryText)

                        PaletteSettingsRow(
                            enabled = paletteEnabled,
                            palette = resolveNotesPalette(paletteId, isDark),
                            primaryText = primaryText,
                            secondaryText = secondaryText,
                            mutedText = mutedText,
                            onClick = { paletteScreenOpen = true }
                        )
                    }
                }

                item {
                    SettingsTintCard(compact = true) {
                        Text("Badges", style = MaterialTheme.typography.titleMedium, color = primaryText)

                        CompactSwitchRow(
                            title = "Images count",
                            checked = showImagesBadge,
                            secondaryText = secondaryText
                        ) {
                            showImagesBadge = it
                            prefs.edit { putBoolean(NotesPagePrefs.KEY_SHOW_IMAGES_BADGE, it) }
                        }

                        CompactSwitchRow(
                            title = "Pulse dot",
                            checked = showReminderBadge,
                            secondaryText = secondaryText
                        ) {
                            showReminderBadge = it
                            prefs.edit { putBoolean(NotesPagePrefs.KEY_SHOW_REMINDER_BADGE, it) }
                        }

                        CompactSwitchRow(
                            title = "Bell icon",
                            checked = showReminderBell,
                            secondaryText = secondaryText
                        ) {
                            showReminderBell = it
                            prefs.edit { putBoolean(NotesPagePrefs.KEY_SHOW_REMINDER_BELL, it) }
                        }
                    }
                }

                item {
                    SettingsTintCard(compact = true) {
                        Text("Suggestions", style = MaterialTheme.typography.titleMedium, color = primaryText)

                        CompactSwitchRow(
                            title = "Title suggestions",
                            checked = enableTitleTips,
                            secondaryText = secondaryText
                        ) { on ->
                            enableTitleTips = on
                            prefs.edit { putBoolean(NotesPagePrefs.KEY_ENABLE_TITLE_TIPS, on) }
                        }

                        if (enableTitleTips) {
                            ResetSuggestionButton(
                                isDark = isDark,
                                primaryText = primaryText,
                                secondaryText = secondaryText,
                                actionText = resetActionText,
                                onClick = {
                                    prefs.edit { putBoolean("seen_title_tip", false) }

                                    scope.launch {
                                        resetActionText = "Resetting..."
                                        delay(750)
                                        resetActionText = "Please wait"
                                        delay(550)
                                        resetActionText = "Done"
                                    }
                                }
                            )
                        }
                    }
                }

                item {
                    SettingsTintCard(compact = true) {
                        Text("Sort", style = MaterialTheme.typography.titleMedium, color = primaryText)

                        CompactSortRadio("Newest", NotesPagePrefs.SORT_NEWEST, sortMode) {
                            sortMode = it
                            prefs.edit { putString(NotesPagePrefs.KEY_SORT, it) }
                        }

                        CompactSortRadio("Oldest", NotesPagePrefs.SORT_OLDEST, sortMode) {
                            sortMode = it
                            prefs.edit { putString(NotesPagePrefs.KEY_SORT, it) }
                        }

                        CompactSortRadio("A-Z", NotesPagePrefs.SORT_TITLE, sortMode) {
                            sortMode = it
                            prefs.edit { putString(NotesPagePrefs.KEY_SORT, it) }
                        }

                        CompactSortRadio("Reminders", NotesPagePrefs.SORT_REMINDERS_FIRST, sortMode) {
                            sortMode = it
                            prefs.edit { putString(NotesPagePrefs.KEY_SORT, it) }
                        }
                    }
                }

                item { Spacer(Modifier.height(6.dp)) }
            }
        }
    }
}

@Composable
private fun PaletteSettingsRow(
    enabled: Boolean,
    palette: NotesPaletteColors,
    primaryText: Color,
    secondaryText: Color,
    mutedText: Color,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val rowShape = RoundedCornerShape(18.dp)

    Surface(
        shape = rowShape,
        color = if (isDark) Color(0xFF2E3032) else Color.White,
        border = BorderStroke(1.dp, if (isDark) Color(0xFF43464A) else Color(0xFFE4E6EA)),
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(rowShape)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Palette,
                contentDescription = null,
                tint = palette.accent,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text("Color palette", style = MaterialTheme.typography.bodyMedium, color = primaryText, maxLines = 1)
                Text(
                    text = if (enabled) palette.label else "Disabled",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (enabled) secondaryText else mutedText
                )
            }
            PaletteGlyph(palette = palette, modifier = Modifier.size(width = 34.dp, height = 46.dp))
        }
    }
}

@Composable
private fun PaletteGlyph(
    palette: NotesPaletteColors,
    modifier: Modifier = Modifier
) {
    val stripes = listOf(
        palette.accent,
        palette.actionBarTint,
        palette.titleRail,
        palette.backdropTint,
        palette.noteTint,
        palette.screenBackground
    )
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, Color.White.copy(alpha = 0.30f), RoundedCornerShape(18.dp))
    ) {
        stripes.forEach { color ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(color)
            )
        }
    }
}

@Composable
private fun NotesPalettePickerScreen(
    initialEnabled: Boolean,
    initialPaletteId: String,
    isDark: Boolean,
    primaryText: Color,
    secondaryText: Color,
    mutedText: Color,
    glassTint: Color,
    glassOverlayTint: Color,
    onBack: () -> Unit,
    onApply: (Boolean, String) -> Unit
) {
    val contentBackdrop = rememberLayerBackdrop()
    val cardBackdrop = rememberLayerBackdrop()
    val buttonBackdrop = rememberLayerBackdrop()
    val context = LocalContext.current
    var enabled by rememberSaveable { mutableStateOf(initialEnabled) }
    var selectedId by rememberSaveable { mutableStateOf(initialPaletteId) }
    val wallpaperPalettes = notesWallpaperPaletteOptions(isDark)
    val basicPalettes = notesBasicPaletteOptions(isDark)
    val palettes = wallpaperPalettes + basicPalettes
    var paletteTab by rememberSaveable(initialPaletteId, isDark) {
        mutableIntStateOf(if (basicPalettes.any { it.id == initialPaletteId }) 1 else 0)
    }
    val selectedPalette = palettes.firstOrNull { it.id == selectedId } ?: palettes.first()
    val previewPalette = if (enabled) selectedPalette else null
    val hasUnappliedChanges = enabled != initialEnabled || selectedId != initialPaletteId
    val topBarShape = RoundedCornerShape(0.dp)
    val barColor = previewPalette?.actionBarTint ?: topActionBarTint()
    val applyTint = previewPalette?.accent ?: MaterialTheme.colorScheme.primary
    val applySurface = previewPalette?.titleRail?.copy(alpha = if (isDark) 0.42f else 0.62f)
        ?: glassTint.copy(alpha = if (isDark) 0.72f else 0.86f)

    fun leavePicker() {
        if (hasUnappliedChanges) {
            Toast.makeText(context, "Palette changes not applied", Toast.LENGTH_SHORT).show()
        }
        onBack()
    }

    BackHandler(onBack = ::leavePicker)

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(contentBackdrop)
        ) {
            ProfileBackdropImageLayer(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(cardBackdrop),
                lightRes = R.drawable.light_grid_pattern,
                darkRes = R.drawable.dark_grid_pattern,
                imageAlpha = if (isDark) 0.72f else 0.44f,
                scrimDark = 0f,
                scrimLight = 0f
            )
            if (previewPalette != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(previewPalette.backdropTint)
                        .layerBackdrop(buttonBackdrop)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .layerBackdrop(buttonBackdrop)
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 108.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    SettingsTintCard {
                        SettingsSwitchRow(
                            title = "Use note palette",
                            subtitle = if (enabled) selectedPalette.label else "Off",
                            checked = enabled,
                            secondaryText = secondaryText,
                            mutedText = mutedText
                        ) { enabled = it }
                    }
                }

                item {
                    PaletteSelectorCard(
                        palettes = if (paletteTab == 0) wallpaperPalettes else basicPalettes,
                        selectedId = selectedId,
                        selectedTab = paletteTab,
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        onTabSelected = { paletteTab = it },
                        onPaletteSelected = { palette ->
                            selectedId = palette.id
                            enabled = true
                        }
                    )
                }

                item {
                    PalettePreviewCard(
                        palette = selectedPalette,
                        enabled = enabled,
                        primaryText = primaryText,
                        secondaryText = secondaryText
                    )
                }
            }
        }

        Surface(
            shape = topBarShape,
            color = Color.Transparent,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(96.dp)
                .drawBackdrop(
                    backdrop = contentBackdrop,
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
                    .padding(start = 4.dp, end = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = ::leavePicker) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBackIos, contentDescription = "Back", tint = primaryText)
                }
                Text(
                    text = "Color palette",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = primaryText,
                    maxLines = 1
                )
            }
        }

        PaletteLiquidApplyButton(
            backdrop = buttonBackdrop,
            tint = applyTint,
            surfaceColor = applySurface,
            textColor = if (isDark) Color.White else Color(0xFF111111),
            onClick = { onApply(enabled, selectedId) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 22.dp)
                .width(300.dp)
                .height(58.dp)
        )
    }
}

@Composable
private fun PaletteLiquidApplyButton(
    backdrop: com.kyant.backdrop.Backdrop,
    tint: Color,
    surfaceColor: Color,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val interactive = remember(scope) { InteractiveHighlight(animationScope = scope) }
    val shape = RoundedCornerShape(999.dp)

    Box(
        modifier = modifier
            .graphicsLayer {
                val press = interactive.pressProgress
                val height = size.height.coerceAtLeast(1f)
                val baseScale = androidx.compose.ui.util.lerp(1f, 1.018f, press)
                val offsetY = interactive.offset.y
                translationY = height * kotlin.math.tanh(0.035f * offsetY / height)
                scaleX = baseScale
                scaleY = baseScale + 0.025f * kotlin.math.abs(offsetY / height)
            }
            .clip(shape)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                highlight = {
                    Highlight(
                        width = 0.35.dp,
                        blurRadius = 1.dp,
                        alpha = 0.30f,
                        style = HighlightStyle.Plain
                    )
                },
                effects = {
                    val press = interactive.pressProgress
                    blur(radius = 7.dp.toPx(), edgeTreatment = TileMode.Mirror)
                    colorControls(
                        brightness = 0.02f + press * 0.05f,
                        contrast = 1.12f + press * 0.08f,
                        saturation = 1.18f + press * 0.12f
                    )
                    lens(
                        refractionHeight = 10.dp.toPx() + press * 5.dp.toPx(),
                        refractionAmount = 24.dp.toPx() + press * 12.dp.toPx(),
                        depthEffect = false,
                        chromaticAberration = false
                    )
                },
                onDrawSurface = {
                    val press = interactive.pressProgress
                    drawRect(surfaceColor)
                    drawRect(tint, blendMode = BlendMode.Hue)
                    drawRect(tint.copy(alpha = 0.24f + press * 0.12f))
                    drawRect(Color.White.copy(alpha = press * 0.08f), blendMode = BlendMode.Plus)
                }
            )
            .clip(shape)
            .then(interactive.modifier)
            .then(interactive.gestureModifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Apply",
            color = textColor,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1
        )
    }
}

@Composable
private fun PalettePreviewCard(
    palette: NotesPaletteColors,
    enabled: Boolean,
    primaryText: Color,
    secondaryText: Color
) {
    val previewPalette = if (enabled) palette else null
    val previewBackground = previewPalette?.screenBackground
        ?: if (isSystemInDarkTheme()) Color(0xFF151515) else Color(0xFFF7F7F8)
    val titleRail = previewPalette?.titleRail
        ?: if (isSystemInDarkTheme()) Color(0xFF2A2131) else Color(0xFFEFE7F6)
    val noteTint = previewPalette?.noteTint
        ?: if (isSystemInDarkTheme()) Color(0xFF201923).copy(alpha = 0.80f) else Color.White
    val accent = previewPalette?.accent
        ?: if (isSystemInDarkTheme()) Color(0xFFCBB6E5) else Color(0xFF6D4B86)

    Surface(
        shape = RoundedCornerShape(26.dp),
        color = previewBackground,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (enabled) palette.label else "Disabled preview",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = primaryText
                )
                PaletteGlyph(palette = palette, modifier = Modifier.size(width = 34.dp, height = 44.dp))
            }
            MiniNotePreview(
                title = "Flight plan",
                body = "Confirm parking and pickup time.",
                noteTint = noteTint,
                titleRail = titleRail,
                accent = accent,
                secondaryText = secondaryText
            )
            MiniNotePreview(
                title = "Reminder",
                body = "Bring badge, jacket, and charger.",
                noteTint = noteTint,
                titleRail = titleRail,
                accent = accent,
                secondaryText = secondaryText
            )
        }
    }
}

@Composable
private fun MiniNotePreview(
    title: String,
    body: String,
    noteTint: Color,
    titleRail: Color,
    accent: Color,
    secondaryText: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(noteTint)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(18.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 10.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                color = accent,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(11.dp))
                    .background(titleRail)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(text = body, color = secondaryText, style = MaterialTheme.typography.bodySmall)
        }
        Column(
            modifier = Modifier
                .width(48.dp)
                .fillMaxSize()
                .background(titleRail),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(Icons.Filled.Palette, contentDescription = null, tint = accent, modifier = Modifier.padding(top = 12.dp).size(18.dp))
            Box(
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .size(14.dp)
                    .background(accent, CircleShape)
            )
        }
    }
}

@Composable
private fun PaletteSelectorCard(
    palettes: List<NotesPaletteColors>,
    selectedId: String,
    selectedTab: Int,
    primaryText: Color,
    secondaryText: Color,
    onTabSelected: (Int) -> Unit,
    onPaletteSelected: (NotesPaletteColors) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val panelColor = if (isDark) Color(0xFF191927) else Color(0xFFF4EFF9)
    val selectedTabColor = if (isDark) Color(0xFF29263A) else Color.White.copy(alpha = 0.86f)
    val scrollState = rememberScrollState()

    Surface(
        shape = RoundedCornerShape(30.dp),
        color = panelColor.copy(alpha = if (isDark) 0.96f else 0.92f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        modifier = Modifier
            .fillMaxWidth()
            .height(252.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp, bottom = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(56.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PaletteSegment(
                    text = "Wallpaper colors",
                    selected = selectedTab == 0,
                    selectedColor = selectedTabColor,
                    textColor = primaryText,
                    mutedColor = secondaryText,
                    modifier = Modifier.weight(1f),
                    onClick = { onTabSelected(0) }
                )
                PaletteSegment(
                    text = "Basic colors",
                    selected = selectedTab == 1,
                    selectedColor = selectedTabColor,
                    textColor = primaryText,
                    mutedColor = secondaryText,
                    modifier = Modifier.weight(1f),
                    onClick = { onTabSelected(1) }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(start = 28.dp, end = 28.dp, top = 30.dp),
                horizontalArrangement = Arrangement.spacedBy(42.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                palettes.forEach { palette ->
                    PaletteChoiceCapsule(
                        palette = palette,
                        selected = selectedId == palette.id,
                        onClick = { onPaletteSelected(palette) }
                    )
                }
            }

            Row(
                modifier = Modifier.padding(top = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val pages = 3
                repeat(pages) { index ->
                    val active = when (selectedTab) {
                        0 -> index == 0
                        else -> index == 1
                    }
                    Box(
                        modifier = Modifier
                            .size(if (active) 11.dp else 10.dp)
                            .background(
                                if (active) primaryText else secondaryText.copy(alpha = 0.45f),
                                CircleShape
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun PaletteSegment(
    text: String,
    selected: Boolean,
    selectedColor: Color,
    textColor: Color,
    mutedColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(28.dp))
            .background(if (selected) selectedColor else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) textColor else mutedColor,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1
        )
    }
}

@Composable
private fun PaletteChoiceCapsule(
    palette: NotesPaletteColors,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(56.dp)
            .height(132.dp)
            .clip(RoundedCornerShape(28.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        PaletteGlyph(
            palette = palette,
            modifier = Modifier
                .width(48.dp)
                .height(124.dp)
        )
        if (selected) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(Color(0xFF5E6399), CircleShape)
                    .border(2.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun PaletteOptionRow(
    palette: NotesPaletteColors,
    selected: Boolean,
    primaryText: Color,
    secondaryText: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .background(if (selected) palette.accent.copy(alpha = 0.12f) else Color.Transparent)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PaletteGlyph(palette = palette, modifier = Modifier.size(width = 32.dp, height = 42.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(palette.label, color = primaryText, style = MaterialTheme.typography.bodyMedium)
            Text("Background, note, title, action", color = secondaryText, style = MaterialTheme.typography.bodySmall)
        }
        Box(
            modifier = Modifier
                .size(18.dp)
                .border(1.5.dp, if (selected) palette.accent else secondaryText.copy(alpha = 0.35f), CircleShape)
                .padding(4.dp)
                .background(if (selected) palette.accent else Color.Transparent, CircleShape)
        )
    }
}

@Composable
private fun SettingsTintCard(
    compact: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(18.dp)
    val cardColor = if (isDark) Color(0xFF232425) else Color(0xFFFEFEFE)
    val borderColor = if (isDark) Color(0xFF333538) else Color(0xFFE3E3E4)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(cardColor)
            .border(BorderStroke(1.dp, borderColor), shape)
            .padding(if (compact) 10.dp else 14.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 10.dp),
        content = content
    )
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    secondaryText: Color,
    subtitle: String? = null,
    mutedText: Color = secondaryText,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(start = 2.dp, end = 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 14.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                color = secondaryText,
                style = MaterialTheme.typography.bodyMedium
            )

            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = mutedText,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun CompactSwitchRow(
    title: String,
    checked: Boolean,
    secondaryText: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(start = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = secondaryText,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1
        )

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.graphicsLayer(scaleX = 0.82f, scaleY = 0.82f)
        )
    }
}

@Composable
private fun ResetSuggestionButton(
    isDark: Boolean,
    primaryText: Color,
    secondaryText: Color,
    actionText: String,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)


    Surface(
        shape = shape,
        color = if (isDark) {
            Color.White.copy(alpha = 0.14f)
        } else {
            Color.White.copy(alpha = 0.96f)
        },
        contentColor = primaryText,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(shape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Reset suggestion dot",
                    style = MaterialTheme.typography.labelLarge,
                    color = primaryText
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Show suggestion dot again.",
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryText
                )
            }

            AnimatedContent(
                targetState = actionText,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "resetActionText"
            ) { text ->
                Text(
                    text = text,
                    color = if (isDark) Color(0xFF8FC7FF) else MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun CompactSortRadio(
    label: String,
    value: String,
    selected: String,
    onSelect: (String) -> Unit
) {
    val textColor = if (isSystemInDarkTheme()) {
        Color.White.copy(alpha = 0.78f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onSelect(value) }
            .padding(start = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = textColor,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1
        )

        RadioButton(
            selected = selected == value,
            onClick = { onSelect(value) },
            modifier = Modifier.graphicsLayer(scaleX = 0.84f, scaleY = 0.84f)
        )
    }
}
