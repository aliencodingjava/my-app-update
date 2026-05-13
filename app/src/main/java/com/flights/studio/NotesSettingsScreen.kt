package com.flights.studio

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(NotesPagePrefs.NAME, Context.MODE_PRIVATE) }

    var compact by remember { mutableStateOf(prefs.getBoolean(NotesPagePrefs.KEY_COMPACT, false)) }
    var twoColumns by remember { mutableStateOf(prefs.getBoolean(NotesPagePrefs.KEY_TWO_COLUMNS, false)) }

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
    val scope = rememberCoroutineScope()
    var resetActionText by remember { mutableStateOf("Reset") }

    var sortMode by remember {
        mutableStateOf(prefs.getString(NotesPagePrefs.KEY_SORT, NotesPagePrefs.SORT_NEWEST)!!)
    }

    var titleTopCompact by remember {
        mutableIntStateOf(
            prefs.getInt(
                NotesPagePrefs.KEY_TITLE_TOP_COMPACT,
                NotesPagePrefs.DEFAULT_TITLE_TOP_COMPACT
            )
        )
    }
    var titleTopNormal by remember {
        mutableIntStateOf(
            prefs.getInt(
                NotesPagePrefs.KEY_TITLE_TOP_NORMAL,
                NotesPagePrefs.DEFAULT_TITLE_TOP_NORMAL
            )
        )
    }

    val topBarShape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
    val isDark = isSystemInDarkTheme()
    val scheme = MaterialTheme.colorScheme
    val glassTint = if (isDark) {
        Color(0xFF1D1726).copy(alpha = 0.88f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
    }
    val primaryText = if (isDark) Color.White else scheme.onSurface
    val secondaryText = if (isDark) Color.White.copy(alpha = 0.78f) else scheme.onSurfaceVariant
    val mutedText = if (isDark) Color.White.copy(alpha = 0.58f) else scheme.onSurfaceVariant.copy(alpha = 0.7f)
    val containerColor = if (!isDark) Color(0xFFFAFAFA).copy(0.60f) else Color(0xFF1A1A1A).copy(0.80f)

    val topBarBackdrop = rememberLayerBackdrop()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
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
                            style = HighlightStyle.Ambient
                        )
                    },
                    effects = {
                        blur(radius = 0f, edgeTreatment = TileMode.Clamp)
                        val cornerRadiusPx = size.height / 2f
                        val safeHeight = cornerRadiusPx * 0.35f
                        lens(
                            refractionHeight = safeHeight.coerceIn(0f, cornerRadiusPx),
                            refractionAmount = (size.minDimension * 0.30f).coerceIn(0f, size.minDimension),
                            depthEffect = false,
                            chromaticAberration = false
                        )
                    },
                    onDrawSurface = { drawRect(containerColor) }
                )
            ) {
                CenterAlignedTopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBackIos, contentDescription = "Back")
                        }
                    },
                    title = {
                        Text(
                            text = "Customize Notes",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.2.sp
                            ),
                            color = primaryText
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                        navigationIconContentColor = primaryText,
                        titleContentColor = primaryText,
                        actionIconContentColor = primaryText
                    )
                )
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
                    start = 14.dp,
                    end = 14.dp,
                    top = padding.calculateTopPadding() + 12.dp,
                    bottom = 18.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    SettingsGlassCard(
                        backdrop = itemBackdrop,
                        glassTint = glassTint
                    ) {
                        Text("Layout", style = MaterialTheme.typography.titleMedium, color = primaryText)

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

                        Text("Title position", style = MaterialTheme.typography.titleMedium, color = primaryText)

                        Text("Compact title top: ${titleTopCompact}dp", color = secondaryText)
                        Slider(
                            value = titleTopCompact.toFloat(),
                            onValueChange = { v ->
                                titleTopCompact = v.toInt()
                                prefs.edit {
                                    putInt(NotesPagePrefs.KEY_TITLE_TOP_COMPACT, titleTopCompact)
                                }
                            },
                            valueRange = 0f..24f,
                            steps = 23
                        )

                        Text("Normal title top: ${titleTopNormal}dp", color = secondaryText)
                        Slider(
                            value = titleTopNormal.toFloat(),
                            onValueChange = { v ->
                                titleTopNormal = v.toInt()
                                prefs.edit {
                                    putInt(NotesPagePrefs.KEY_TITLE_TOP_NORMAL, titleTopNormal)
                                }
                            },
                            valueRange = 0f..32f,
                            steps = 31
                        )
                    }
                }

                item {
                    SettingsGlassCard(
                        backdrop = itemBackdrop,
                        glassTint = glassTint
                    ) {
                        Text("Badges", style = MaterialTheme.typography.titleMedium, color = primaryText)

                        SettingsSwitchRow(
                            title = "Images badge (count)",
                            checked = showImagesBadge,
                            secondaryText = secondaryText
                        ) {
                            showImagesBadge = it
                            prefs.edit { putBoolean(NotesPagePrefs.KEY_SHOW_IMAGES_BADGE, it) }
                        }

                        SettingsSwitchRow(
                            title = "Reminder pulse dot",
                            checked = showReminderBadge,
                            secondaryText = secondaryText
                        ) {
                            showReminderBadge = it
                            prefs.edit { putBoolean(NotesPagePrefs.KEY_SHOW_REMINDER_BADGE, it) }
                        }

                        SettingsSwitchRow(
                            title = "Reminder bell icon",
                            checked = showReminderBell,
                            secondaryText = secondaryText
                        ) {
                            showReminderBell = it
                            prefs.edit { putBoolean(NotesPagePrefs.KEY_SHOW_REMINDER_BELL, it) }
                        }
                    }
                }

                item {
                    SettingsGlassCard(
                        backdrop = itemBackdrop,
                        glassTint = glassTint
                    ) {
                        Text("Suggestions", style = MaterialTheme.typography.titleMedium, color = primaryText)

                        SettingsSwitchRow(
                            title = "Title suggestions in Add Note",
                            subtitle = if (enableTitleTips) "On" else "Off",
                            checked = enableTitleTips,
                            secondaryText = secondaryText,
                            mutedText = mutedText
                        ) { on ->
                            enableTitleTips = on
                            prefs.edit { putBoolean(NotesPagePrefs.KEY_ENABLE_TITLE_TIPS, on) }
                        }

                        if (enableTitleTips) {
                            Spacer(Modifier.height(4.dp))

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
                    SettingsGlassCard(
                        backdrop = itemBackdrop,
                        glassTint = glassTint
                    ) {
                        Text("Sort", style = MaterialTheme.typography.titleMedium, color = primaryText)

                        SortRadio("Newest first", NotesPagePrefs.SORT_NEWEST, sortMode) {
                            sortMode = it
                            prefs.edit { putString(NotesPagePrefs.KEY_SORT, it) }
                        }

                        SortRadio("Oldest first", NotesPagePrefs.SORT_OLDEST, sortMode) {
                            sortMode = it
                            prefs.edit { putString(NotesPagePrefs.KEY_SORT, it) }
                        }

                        SortRadio("A-Z by title", NotesPagePrefs.SORT_TITLE, sortMode) {
                            sortMode = it
                            prefs.edit { putString(NotesPagePrefs.KEY_SORT, it) }
                        }

                        SortRadio("Reminders first", NotesPagePrefs.SORT_REMINDERS_FIRST, sortMode) {
                            sortMode = it
                            prefs.edit { putString(NotesPagePrefs.KEY_SORT, it) }
                        }

                        Text(
                            "Sorting will apply when you go back to Notes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = mutedText
                        )
                    }
                }

                item { Spacer(Modifier.height(6.dp)) }
            }
        }
    }
}

@Composable
private fun SettingsGlassCard(
    backdrop: com.kyant.backdrop.Backdrop,
    glassTint: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(18.dp) },
                highlight = {
                    Highlight(
                        width = 0.45.dp,
                        blurRadius = 1.2.dp,
                        alpha = 0.25f,
                        style = HighlightStyle.Plain
                    )
                },
                effects = {
                    blur(radius = 3f, edgeTreatment = TileMode.Clamp)
                    val cornerRadiusPx = size.height / 2f
                    val safeHeight = cornerRadiusPx * 0.15f
                    lens(
                        refractionHeight = safeHeight.coerceIn(0f, cornerRadiusPx),
                        refractionAmount = (size.minDimension * 0.40f).coerceIn(0f, size.minDimension),
                        depthEffect = false,
                        chromaticAberration = false
                    )
                },
                onDrawSurface = { drawRect(glassTint) }
            )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
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
private fun SortRadio(
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
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onSelect(value) }
            .padding(start = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = textColor,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium
        )

        RadioButton(
            selected = selected == value,
            onClick = { onSelect(value) }
        )
    }
}