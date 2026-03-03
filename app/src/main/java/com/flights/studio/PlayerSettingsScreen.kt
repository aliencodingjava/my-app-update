package com.flights.studio

import android.app.Activity
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.core.view.WindowCompat
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import kotlinx.coroutines.launch

class PlayerSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                PlayerSettingsScreen()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSettingsScreen() {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()

    val settings by remember {
        context.playerSettingsFlow()
    }.collectAsStateWithLifecycle(initialValue = PlayerSettings())

    val rootBackdrop = rememberLayerBackdrop()
    val itemBackdrop = rememberLayerBackdrop()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    SetSystemBars(isDark)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            val collapsedFraction = scrollBehavior.state.collapsedFraction
            val topBarShape = RoundedCornerShape(
                bottomStart = 24.dp,
                bottomEnd = 24.dp
            )
            val dynamicTint =
                MaterialTheme.colorScheme.primary.copy(
                    alpha = 0.05f * collapsedFraction
                )

            val containerColor =
                if (isDark)
                    Color(0xFF1A1A1A).copy(alpha = 0.60f + 0.25f * collapsedFraction)
                else
                    Color(0xFFFAFAFA).copy(alpha = 0.40f + 0.25f * collapsedFraction)
            Surface(
                shape = topBarShape,
                color = Color.Transparent,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                modifier = Modifier.drawBackdrop(
                    backdrop = rootBackdrop, // SAME chain as content
                    shape = { topBarShape },
                    highlight = {
                        Highlight(
                            width = 0.5.dp,
                            blurRadius = 1.dp,
                            alpha = 0.20f,
                            style = HighlightStyle.Ambient
                        )
                    },
                    effects = {
                        blur(
                            radius = lerp(1f, 12f, collapsedFraction).dp.toPx(),
                            edgeTreatment = TileMode.Mirror
                        )
                        lens(
                            refractionHeight = 60f,
                            refractionAmount = 80f,
                            depthEffect = true,
                            chromaticAberration = false
                        )
                    },
                    onDrawSurface = {
                        drawRect(containerColor)
                        drawRect(dynamicTint)
                    }
                )
            ) {
                androidx.compose.material3.CenterAlignedTopAppBar(
                    scrollBehavior = scrollBehavior,
                    title = {
                        Text(
                            "Player Settings",
                            style = MaterialTheme.typography.titleLarge,
                            color = if (isDark) Color.White else Color.Black
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                        navigationIconContentColor = if (isDark) Color.White else Color.Black,
                        titleContentColor = if (isDark) Color.White else Color.Black,
                        actionIconContentColor = if (isDark) Color.White else Color.Black
                    )
                )
            }
        }
    ) { padding ->

        Box(
            modifier = Modifier.fillMaxSize()
        ) {

            // Background (uses itemBackdrop, not rootBackdrop)
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
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding() + 12.dp,
                    bottom = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // ---------------- PLAYBACK ----------------
                item {
                    GlassCard(itemBackdrop, isDark) {

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
                    GlassCard(itemBackdrop, isDark) {

                        SectionTitle("Appearance")

                        EnumSelector(
                            title = "Glass intensity",
                            options = listOf("Light", "Medium", "Strong", "Full"),
                            selectedIndex = settings.glassIntensity
                        ) { index ->
                            scope.launch {
                                context.playerSettingsDataStore.edit {
                                    it[KEY_GLASS] = index
                                }
                            }
                        }

                        EnumSelector(
                            title = "Menu elasticity",
                            options = listOf("None", "Soft", "Normal", "Heavy"),
                            selectedIndex = settings.menuElasticity
                        ) { index ->
                            scope.launch {
                                context.playerSettingsDataStore.edit {
                                    it[KEY_ELASTIC] = index
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
                    GlassCard(itemBackdrop, isDark) {

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
    }
}

@Composable
fun GlassCard(
    backdrop: com.kyant.backdrop.backdrops.LayerBackdrop,
    isDark: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    val glassTint = if (isDark)
        Color.White.copy(alpha = 0.08f)
    else
        Color.White.copy(alpha = 0.35f)

    val contentColor = if (isDark) Color.White else Color.Black

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = tween(120),
        label = "cardScale"
    )

    val configuration = LocalConfiguration.current
    val isLandscape =
        configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
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
                    if (isLandscape) {
                        // Lightweight glass
                        blur(radius = 6f, edgeTreatment = TileMode.Clamp)
                    } else {
                        // Full cinematic glass
                        blur(radius = 14f, edgeTreatment = TileMode.Clamp)
                        lens(
                            refractionHeight = size.height * 0.15f,
                            refractionAmount = size.minDimension * 0.35f,
                            depthEffect = false, // <-- IMPORTANT
                            chromaticAberration = false
                        )
                    }
                },
                onDrawSurface = { drawRect(glassTint) }
            )
            .clickable(
                interactionSource = interaction,
                indication = null
            ) {} // empty, just for press feedback
    ) {
        CompositionLocalProvider(
            LocalContentColor provides contentColor
        ) {
            Column(
                Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                content = content
            )
        }
    }
}


@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
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
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            title,
            color = LocalContentColor.current
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
    val isDark = isSystemInDarkTheme()
    val textColor = if (isDark) Color.White else Color.Black

    val speeds = listOf(0.75f, 1f, 1.25f, 1.5f)

    Column {
        Text(
            "Playback speed",
            color = textColor
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            speeds.forEach { speed ->

                val isSelected = speed == current

                FilterChip(
                    selected = isSelected,
                    onClick = { onSelect(speed) },
                    label = {
                        Text("${speed}x")
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

@Composable
fun EnumSelector(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val textColor = if (isDark) Color.White else Color.Black

    Column {
        Text(
            title,
            color = textColor
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEachIndexed { index, label ->

                val isSelected = index == selectedIndex

                FilterChip(
                    selected = isSelected,
                    onClick = { onSelect(index) },
                    label = {
                        Text(label)
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