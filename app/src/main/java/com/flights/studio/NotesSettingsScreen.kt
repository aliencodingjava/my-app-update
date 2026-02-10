package com.flights.studio

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop


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

    val pageBg = LocalAppPageBg.current
    val pageBackdrop = rememberLayerBackdrop {
        drawRect(pageBg)
        drawContent()
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Surface(
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 1.dp,
                tonalElevation = 0.dp
            ) {
                CenterAlignedTopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBackIos, contentDescription = "Back")
                        }
                    },
                    title = {
                        val t = MaterialTheme.typography.titleLarge
                        Text("Customize Notes", style = t.copy(fontWeight = FontWeight.SemiBold,letterSpacing = 0.2.sp))
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(pageBg)
                .layerBackdrop(pageBackdrop)
                .padding(padding),
            contentPadding = PaddingValues(
                start = 14.dp,
                end = 14.dp,
                top = 12.dp,
                bottom = 18.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // -------------------- Layout Card --------------------
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(
                        Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {

                        Text("Layout", style = MaterialTheme.typography.titleMedium)

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Compact rows", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Switch(
                                checked = compact,
                                onCheckedChange = {
                                    compact = it
                                    prefs.edit { putBoolean(NotesPagePrefs.KEY_COMPACT, it) }
                                }
                            )
                        }

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Two columns grid", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Switch(
                                checked = twoColumns,
                                onCheckedChange = {
                                    twoColumns = it
                                    prefs.edit { putBoolean(NotesPagePrefs.KEY_TWO_COLUMNS, it) }
                                }
                            )
                        }

                        Text("Title position", style = MaterialTheme.typography.titleMedium)

                        Text(
                            "Compact title top: ${titleTopCompact}dp",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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

                        Text(
                            "Normal title top: ${titleTopNormal}dp",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
            }

            // -------------------- Badges Card --------------------
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(
                        Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {

                        Text("Badges", style = MaterialTheme.typography.titleMedium)

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Images badge (count)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Switch(
                                checked = showImagesBadge,
                                onCheckedChange = {
                                    showImagesBadge = it
                                    prefs.edit { putBoolean(NotesPagePrefs.KEY_SHOW_IMAGES_BADGE, it) }
                                }
                            )
                        }

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Reminder pulse dot", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Switch(
                                checked = showReminderBadge,
                                onCheckedChange = {
                                    showReminderBadge = it
                                    prefs.edit { putBoolean(NotesPagePrefs.KEY_SHOW_REMINDER_BADGE, it) }
                                }
                            )
                        }

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Reminder bell icon", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Switch(
                                checked = showReminderBell,
                                onCheckedChange = {
                                    showReminderBell = it
                                    prefs.edit { putBoolean(NotesPagePrefs.KEY_SHOW_REMINDER_BELL, it) }
                                }
                            )
                        }
                    }
                }
            }
            // -------------------- Suggestions Card --------------------
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(
                        Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Suggestions", style = MaterialTheme.typography.titleMedium)

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Title suggestions in Add Note",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    if (enableTitleTips) "On" else "Off",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                                )
                            }

                            Switch(
                                checked = enableTitleTips,
                                onCheckedChange = { on ->
                                    enableTitleTips = on
                                    prefs.edit {
                                        putBoolean(NotesPagePrefs.KEY_ENABLE_TITLE_TIPS, on)
                                    }
                                }
                            )
                        }

                        // Optional: give them a way to re-show the dot by resetting "seen"
                        // (Only show when tips are enabled)
                        if (enableTitleTips) {
                            Spacer(Modifier.height(6.dp))

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        prefs.edit { putBoolean("seen_title_tip", false) }
                                    }
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text("Reset suggestion dot", style = MaterialTheme.typography.labelLarge)
                                        Text(
                                            "It will appear again in Add Note (when note is long enough).",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        "Reset",
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }

                    }
                }
            }


            // -------------------- Sort Card --------------------
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(
                        Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {

                        Text("Sort", style = MaterialTheme.typography.titleMedium)

                        SortRadio("Newest first", NotesPagePrefs.SORT_NEWEST, sortMode) {
                            sortMode = it
                            prefs.edit { putString(NotesPagePrefs.KEY_SORT, it) }
                        }

                        SortRadio("Oldest first", NotesPagePrefs.SORT_OLDEST, sortMode) {
                            sortMode = it
                            prefs.edit { putString(NotesPagePrefs.KEY_SORT, it) }
                        }

                        SortRadio("A–Z by title", NotesPagePrefs.SORT_TITLE, sortMode) {
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // little extra bottom breathing room
            item { Spacer(Modifier.height(6.dp)) }
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
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        RadioButton(
            selected = selected == value,
            onClick = { onSelect(value) }
        )
    }
}
