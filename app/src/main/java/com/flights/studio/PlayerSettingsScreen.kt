package com.flights.studio

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import kotlinx.coroutines.launch

class PlayerSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FlightsTheme(profileBackdropStyle = ProfileBackdropStyle.Auto) {
                PlayerSettingsScreen(onBack = { finish() })
            }
        }
    }
}

@Suppress("DEPRECATION")
@Composable
fun SetSystemBars(isDark: Boolean) {
    val view = LocalView.current
    val window = (view.context as Activity).window

    SideEffect {
        WindowCompat.getInsetsController(window, view)
            .isAppearanceLightStatusBars = !isDark

        window.statusBarColor = Color.Transparent.toArgb()
    }
}

@Composable
fun PlayerSettingsScreen(
    onBack: () -> Unit = {}
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()

    val settings by remember {
        context.playerSettingsFlow()
    }.collectAsStateWithLifecycle(initialValue = PlayerSettings())

    val chromeBackdrop = rememberLayerBackdrop()
    val itemBackdrop = rememberLayerBackdrop()

    SetSystemBars(isDark)

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .layerBackdrop(chromeBackdrop)
        ) {
            ProfileBackdropImageLayer(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(itemBackdrop),
                lightRes = R.drawable.light_grid_pattern,
                darkRes = R.drawable.dark_grid_pattern,
                imageAlpha = if (isDark) 0.95f else 0.70f,
                scrimDark = 0.12f,
                scrimLight = 0.03f
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 8.dp,
                    end = 8.dp,
                    top = 104.dp,
                    bottom = 18.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                // ---------------- PLAYBACK ----------------
                item {
                    SettingsPlainCard {

                        SectionTitle("Playback")

                        SwitchSetting(
                            title = "Autoplay next video",
                            checked = settings.autoplayNext
                        ) { value ->
                            scope.launch {
                                context.playerSettingsDataStore.edit {
                                    it[KEY_AUTOPLAY] = value
                                }
                            }
                        }

                        SwitchSetting(
                            title = "Resume playback",
                            checked = settings.resumePlayback
                        ) { value ->
                            scope.launch {
                                context.playerSettingsDataStore.edit {
                                    it[KEY_RESUME] = value
                                }
                            }
                        }

                        SpeedSelector(
                            current = settings.playbackSpeed
                        ) { speed ->
                            scope.launch {
                                context.playerSettingsDataStore.edit {
                                    it[KEY_SPEED] = speed
                                }
                            }
                        }
                    }
                }

                // ---------------- APPEARANCE ----------------
                item {
                    SettingsPlainCard {

                        SectionTitle("Appearance")

                        EnumSelector(
                            title = "Glass intensity",
                            options = listOf("1", "2", "3", "4", "5"),
                            selectedIndex = settings.glassIntensity
                        ) { index ->
                            scope.launch {
                                context.playerSettingsDataStore.edit {
                                    it[KEY_GLASS] = index
                                }
                            }
                        }

                        EnumSelector(
                            title = "Dim strength",
                            options = listOf("None", "Light", "Dark"),
                            selectedIndex = settings.dimStrength
                        ) { index ->
                            scope.launch {
                                context.playerSettingsDataStore.edit {
                                    it[KEY_DIM] = index
                                }
                            }
                        }
                    }
                }

                // ---------------- INTERACTION ----------------
                item {
                    SettingsPlainCard {

                        SectionTitle("Interaction")

                        SwitchSetting(
                            title = "Animate likes",
                            checked = settings.animateLikes
                        ) { value ->
                            scope.launch {
                                context.playerSettingsDataStore.edit {
                                    it[KEY_ANIMATE_LIKES] = value
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(8.dp)) }
            }
        }

        PlayerSettingsGlassTopAppBar(
            backdrop = chromeBackdrop,
            title = "Player Settings",
            onBack = onBack,
            modifier = Modifier.align(androidx.compose.ui.Alignment.TopCenter)
        )
    }
}

@Composable
private fun PlayerSettingsGlassTopAppBar(
    backdrop: LayerBackdrop,
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val topBarShape = RoundedCornerShape(0.dp)
    val barColor = topActionBarTint()
    val contentColor = if (isDark) Color.White else Color(0xFF111111)

    Surface(
        shape = topBarShape,
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp)
            .drawBackdrop(
                backdrop = backdrop,
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
                .padding(start = 4.dp, end = 20.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                    contentDescription = "Back",
                    tint = contentColor
                )
            }
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                color = contentColor,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1
            )
        }
    }
}

@Composable
fun SettingsPlainCard(
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val cardColor = if (isDark) Color(0xFF232425) else Color(0xFFFEFEFE)
    val borderColor = if (isDark) Color(0xFF333538) else Color(0xFFE3E3E4)
    val contentColor = MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = cardColor,
        contentColor = contentColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, borderColor)
    ) {
        CompositionLocalProvider(
            LocalContentColor provides contentColor
        ) {
            Column(
                Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
                content = content
            )
        }
    }
}


@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        color = LocalContentColor.current
    )
}

@Composable
fun SwitchSetting(
    title: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text(
            title,
            modifier = Modifier.weight(1f),
            color = LocalContentColor.current,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Switch(
            checked = checked,
            onCheckedChange = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onChange(it)
            }
        )
    }
}

@Composable
fun SpeedSelector(
    current: Float,
    onSelect: (Float) -> Unit
) {
    val textColor = MaterialTheme.colorScheme.onSurface

    val speeds = listOf(0.75f, 1f, 1.25f, 1.5f)

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            "Playback speed",
            color = textColor,
            style = MaterialTheme.typography.bodyMedium
        )

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            speeds.forEach { speed ->

                val isSelected = speed == current

                FilterChip(
                    selected = isSelected,
                    onClick = { onSelect(speed) },
                    modifier = Modifier.height(32.dp),
                    label = {
                        Text(
                            "${speed}x",
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color.Transparent,
                        labelColor = LocalContentColor.current,

                        selectedContainerColor =
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),

                        selectedLabelColor =
                            if (isSystemInDarkTheme())
                                androidx.compose.ui.graphics.lerp(
                                    MaterialTheme.colorScheme.primary,
                                    Color.White,
                                    0.25f
                                )
                            else
                                MaterialTheme.colorScheme.primary
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            LocalContentColor.current.copy(alpha = 0.35f)
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EnumSelector(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    val textColor = MaterialTheme.colorScheme.onSurface

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            title,
            color = textColor,
            style = MaterialTheme.typography.bodyMedium
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEachIndexed { index, label ->

                val isSelected = index == selectedIndex.coerceIn(options.indices)

                FilterChip(
                    selected = isSelected,
                    onClick = { onSelect(index) },
                    modifier = Modifier
                        .height(32.dp)
                        .widthIn(min = 0.dp),
                    label = {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color.Transparent,
                        labelColor = LocalContentColor.current,

                        selectedContainerColor =
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),

                        selectedLabelColor =
                            if (isSystemInDarkTheme())
                                androidx.compose.ui.graphics.lerp(
                                    MaterialTheme.colorScheme.primary,
                                    Color.White,
                                    0.25f
                                )
                            else
                                MaterialTheme.colorScheme.primary
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            LocalContentColor.current.copy(alpha = 0.35f)
                    )
                )
            }
        }
    }
}
