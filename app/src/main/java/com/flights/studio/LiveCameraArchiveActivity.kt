package com.flights.studio

import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.flow.distinctUntilChanged

class LiveCameraArchiveActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FlightsTheme {
                val view = LocalView.current
                val isDark = isSystemInDarkTheme()

                SideEffect {
                    val barColor = if (isDark) Color(0xFF1A1A1A) else Color(0xFFF7F7F8)
                    @Suppress("DEPRECATION")
                    window.statusBarColor = barColor.toArgb()
                    WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
                }

                LiveCameraArchiveScreen(onBack = { finish() })
            }
        }
    }
}

@Composable
private fun LiveCameraArchiveScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val contentBackdrop = rememberLayerBackdrop()
    var fullscreenImage by remember { mutableStateOf<LiveCameraArchiveImage?>(null) }
    var selectedCamera by remember { mutableStateOf<String?>(null) }
    var timeRange by remember { mutableStateOf(ArchiveTimeRange.Last10Minutes) }
    var sortMode by remember { mutableStateOf(ArchiveSortMode.NewestFirst) }
    var sortMenuOpen by remember { mutableStateOf(false) }
    var timePickerOpen by remember { mutableStateOf(false) }
    val images = remember(selectedCamera, timeRange, sortMode) {
        LiveCameraArchiveStore.listImages(context, selectedCamera)
            .filterBy(timeRange)
            .sortedFor(sortMode)
    }

    BackHandler {
        when {
            fullscreenImage != null -> fullscreenImage = null
            timePickerOpen -> timePickerOpen = false
            sortMenuOpen -> sortMenuOpen = false
            else -> onBack()
        }
    }

    val pageBg = if (isDark) Color(0xFF101112) else Color(0xFFF7F7F8)
    Box(
        Modifier
            .fillMaxSize()
            .background(pageBg)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .layerBackdrop(contentBackdrop)
        ) {
            ProfileBackdropImageLayer(
                modifier = Modifier.matchParentSize(),
                lightRes = R.drawable.light_grid_pattern,
                darkRes = R.drawable.dark_grid_pattern,
                imageAlpha = if (isDark) 0.92f else 0.62f,
                scrimDark = 0.10f,
                scrimLight = 0.02f
            )

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(top = 96.dp)
            ) {
                ArchiveCameraTabs(
                    selectedCamera = selectedCamera,
                    onSelected = { selectedCamera = it },
                    modifier = Modifier.padding(start = 12.dp, top = 12.dp, end = 12.dp)
                )

                ArchiveTimeRangeChips(
                    selectedRange = timeRange,
                    onOpenPicker = { timePickerOpen = true },
                    modifier = Modifier.padding(start = 12.dp, top = 10.dp, end = 12.dp)
                )

                if (images.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.PhotoLibrary,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                                modifier = Modifier.size(34.dp)
                            )
                            Text(
                                text = "No saved images in the latest ${timeRange.label}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(top = 10.dp)
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(150.dp),
                        contentPadding = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(images, key = { it.file.absolutePath }) { image ->
                            ArchiveImageCard(
                                image = image,
                                onClick = { fullscreenImage = image }
                            )
                        }
                    }
                }
            }
        }

        LiveCameraArchiveTopBar(
            backdrop = contentBackdrop,
            onBack = onBack,
            onOpenSort = { sortMenuOpen = !sortMenuOpen },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        ArchiveSortMenu(
            visible = sortMenuOpen,
            backdrop = contentBackdrop,
            selected = sortMode,
            onDismiss = { sortMenuOpen = false },
            onSelected = {
                sortMode = it
                sortMenuOpen = false
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 56.dp, end = 10.dp)
        )

        ArchiveTimePickerSheet(
            visible = timePickerOpen,
            backdrop = contentBackdrop,
            selectedRange = timeRange,
            selectedCamera = selectedCamera,
            onDismiss = { timePickerOpen = false },
            onSelected = {
                timeRange = it
                timePickerOpen = false
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        AnimatedVisibility(
            visible = fullscreenImage != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            fullscreenImage?.let { image ->
                ArchiveFullscreenImage(
                    image = image,
                    onClose = { fullscreenImage = null }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LiveCameraArchiveTopBar(
    backdrop: Backdrop,
    onBack: () -> Unit,
    onOpenSort: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(0.dp)
    val tint = if (isDark) Color(0xFF1A1A1A).copy(alpha = 0.78f)
    else MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
    val chromeContentColor = if (isDark) Color.White else Color(0xFF1A1A1A)

    Surface(
        shape = shape,
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                highlight = null,
                effects = {
                    vibrancy()
                    blur(2.dp.toPx(), edgeTreatment = TileMode.Mirror)
                    lens(4.dp.toPx(), 8.dp.toPx(), depthEffect = false, chromaticAberration = false)
                },
                onDrawSurface = { drawRect(tint) }
            )
    ) {
        CenterAlignedTopAppBar(
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBackIos,
                        contentDescription = "Back",
                        tint = chromeContentColor
                    )
                }
            },
            title = {
                Text(
                    text = "Camera Archive",
                    style = MaterialTheme.typography.titleLarge,
                    color = chromeContentColor,
                    maxLines = 1
                )
            },
            actions = {
                IconButton(onClick = onOpenSort) {
                    Icon(
                        Icons.Filled.Menu,
                        contentDescription = "Sort",
                        tint = chromeContentColor
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = chromeContentColor,
                navigationIconContentColor = chromeContentColor,
                actionIconContentColor = chromeContentColor
            )
        )
    }
}

@Composable
private fun ArchiveCameraTabs(
    selectedCamera: String?,
    onSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        null to "All",
        "curb" to "Curb",
        "north" to "North",
        "south" to "South"
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEach { (key, label) ->
            val selected = key == selectedCamera
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .clickable { onSelected(key) },
                shape = RoundedCornerShape(999.dp),
                color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                else MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                border = BorderStroke(
                    1.dp,
                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.48f)
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.20f)
                )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = label,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ArchiveTimeRangeChips(
    selectedRange: ArchiveTimeRange,
    onOpenPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        ArchiveRangePill(
            label = "Latest: ${selectedRange.label}",
            modifier = Modifier.width(174.dp),
            onClick = onOpenPicker
        )
    }
}

@Composable
private fun ArchiveRangePill(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.48f)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ArchiveTimePickerSheet(
    visible: Boolean,
    backdrop: Backdrop,
    selectedRange: ArchiveTimeRange,
    selectedCamera: String?,
    onDismiss: () -> Unit,
    onSelected: (ArchiveTimeRange) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = visible) { onDismiss() }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = if (isSystemInDarkTheme()) 0.28f else 0.18f))
                    .clickable(onClick = onDismiss)
            )
            ArchiveLatestPickerMenu(
                backdrop = backdrop,
                selectedRange = selectedRange,
                selectedCamera = selectedCamera,
                onDismiss = onDismiss,
                onSelected = onSelected,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun ArchiveLatestPickerMenu(
    backdrop: Backdrop,
    selectedRange: ArchiveTimeRange,
    selectedCamera: String?,
    onDismiss: () -> Unit,
    onSelected: (ArchiveTimeRange) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val contentColor = MaterialTheme.colorScheme.onSurface
    val sheetShape = RoundedCornerShape(26.dp)
    val sheetTint = if (isDark) Color(0xFF151719).copy(alpha = 0.82f)
    else Color.White.copy(alpha = 0.70f)
    var draftRange by remember(selectedRange) { mutableStateOf(selectedRange) }
    val selectedCameraLabel = when (selectedCamera) {
        "curb" -> "Curb"
        "north" -> "North"
        "south" -> "South"
        else -> "All cameras"
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .clip(sheetShape)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { sheetShape },
                effects = {
                    vibrancy()
                    blur(18.dp.toPx(), edgeTreatment = TileMode.Mirror)
                    lens(
                        refractionHeight = 9.dp.toPx(),
                        refractionAmount = 18.dp.toPx(),
                        depthEffect = false,
                        chromaticAberration = false
                    )
                },
                onDrawSurface = {
                    drawRect(sheetTint)
                }
            ),
        shape = sheetShape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = if (isDark) 0.24f else 0.18f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Choose latest",
                        color = contentColor,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "Saved images expire after 48 hours",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "Done",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable {
                            onSelected(draftRange)
                            onDismiss()
                        }
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                )
            }

            ArchiveDetailedTimePicker(
                selectedRange = draftRange,
                onSelected = { draftRange = it }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isDark) {
                            Color.White.copy(alpha = 0.08f)
                        } else {
                            Color.White.copy(alpha = 0.34f)
                        }
                    )
            ) {
                ArchivePickerInfoRow("Range", "Latest ${draftRange.label}")
                ArchivePickerInfoRow("Camera", selectedCameraLabel)
                ArchivePickerInfoRow("Retention", "48 hours")
            }
        }
    }
}

@Composable
private fun ArchiveDetailedTimePicker(
    selectedRange: ArchiveTimeRange,
    onSelected: (ArchiveTimeRange) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.10f else 0.05f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ArchivePickerWheel(
                    label = "Hours",
                    values = (0..48).toList(),
                    selectedValue = selectedRange.hours,
                    valueText = { it.toString().padStart(2, '0') },
                    onValueSelected = { hour ->
                        onSelected(selectedRange.withHours(hour))
                    },
                    modifier = Modifier.weight(1f)
                )
                ArchivePickerWheel(
                    label = "Minutes",
                    values = (0..59).toList(),
                    selectedValue = selectedRange.minutes,
                    valueText = { it.toString().padStart(2, '0') },
                    onValueSelected = { minute ->
                        onSelected(selectedRange.withMinutes(minute))
                    },
                    modifier = Modifier.weight(1f),
                    enabled = selectedRange.hours < 48
                )
            }
        }
        Text(
            text = "Minute precise from 1 minute to 48 hours",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ArchivePickerWheel(
    label: String,
    values: List<Int>,
    selectedValue: Int,
    valueText: (Int) -> String,
    onValueSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val isDark = isSystemInDarkTheme()
    val view = LocalView.current
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = values.indexOf(selectedValue).coerceAtLeast(0)
    )
    val snapFlingBehavior = rememberSnapFlingBehavior(
        lazyListState = listState,
        snapPosition = SnapPosition.Center
    )
    LaunchedEffect(enabled) {
        if (!enabled) listState.scrollToItem(0)
    }
    LaunchedEffect(listState, enabled, values, selectedValue) {
        var lastCenteredValue = selectedValue
        snapshotFlow {
            if (!enabled) {
                null
            } else {
                val layoutInfo = listState.layoutInfo
                val center = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                val centeredItem = layoutInfo.visibleItemsInfo.minByOrNull { item ->
                    abs((item.offset + item.size / 2) - center)
                }
                centeredItem?.index?.let(values::getOrNull)
            }
        }
            .distinctUntilChanged()
            .collect { value ->
                if (value == null) return@collect
                if (value != lastCenteredValue) {
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    lastCenteredValue = value
                    onValueSelected(value)
                }
            }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.42f),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(164.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.14f else 0.08f))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(164.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.68f else 0.58f),
                                Color.Transparent,
                                Color.Transparent,
                                MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.68f else 0.58f)
                            )
                        )
                    )
            )
            LazyColumn(
                state = listState,
                flingBehavior = snapFlingBehavior,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(164.dp),
                contentPadding = PaddingValues(vertical = 63.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(values) { value ->
                    val selected = value == selectedValue
                    Text(
                        text = valueText(value),
                        color = MaterialTheme.colorScheme.onSurface.copy(
                            alpha = when {
                                !enabled -> 0.24f
                                selected -> 1f
                                else -> 0.40f
                            }
                        ),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            letterSpacing = 0.sp
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(enabled = enabled) {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                onValueSelected(value)
                            }
                            .padding(top = 3.dp),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun ArchivePickerInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ArchiveSortMenu(
    visible: Boolean,
    backdrop: Backdrop,
    selected: ArchiveSortMode,
    onDismiss: () -> Unit,
    onSelected: (ArchiveSortMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val contentColor = MaterialTheme.colorScheme.onSurface
    val selectedDotColor = if (isDark) Color.White else MaterialTheme.colorScheme.primary

    BackHandler(enabled = visible) { onDismiss() }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .width(236.dp)
                .clip(RoundedCornerShape(24.dp))
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(24.dp) },
                    effects = {
                        vibrancy()
                        blur(6.dp.toPx(), edgeTreatment = TileMode.Mirror)
                        lens(4.dp.toPx(), 8.dp.toPx(), depthEffect = false, chromaticAberration = false)
                    },
                    onDrawSurface = {
                        drawRect(
                            if (isDark) Color(0xFF1A1A1A).copy(alpha = 0.82f)
                            else Color.White.copy(alpha = 0.76f)
                        )
                    }
                )
                .clickable(onClick = onDismiss),
            color = Color.Transparent,
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Column(Modifier.padding(vertical = 6.dp)) {
                ArchiveSortMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .clickable { onSelected(mode) }
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .align(Alignment.Center)
                                    .clip(CircleShape)
                                    .background(
                                        if (mode == selected) selectedDotColor
                                        else contentColor.copy(alpha = 0.34f)
                                    )
                            )
                        }
                        Text(
                            text = mode.label,
                            color = contentColor,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchiveImageCard(
    image: LiveCameraArchiveImage,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    val isDark = isSystemInDarkTheme()
    val imageBackdrop = rememberLayerBackdrop()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(184.dp)
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.86f else 0.78f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.20f))
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(imageBackdrop)
            ) {
                AsyncImage(
                    model = image.file,
                    contentDescription = image.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            ArchiveCameraColorOverlay(
                backdrop = imageBackdrop,
                isDark = isDark,
                shape = shape,
                modifier = Modifier.matchParentSize()
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f))
                        )
                    )
                    .padding(10.dp)
            ) {
                Column {
                    Text(
                        text = image.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${image.timestampLabel()} • ${image.expiryLabel()}",
                        color = Color.White.copy(alpha = 0.84f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ArchiveFullscreenImage(
    image: LiveCameraArchiveImage,
    onClose: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val fullscreenBackdrop = rememberLayerBackdrop()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(fullscreenBackdrop)
        ) {
            HomeGlassZoomImage(
                model = image.file,
                modifier = Modifier.fillMaxSize(),
                cornerRadiusDp = 0.dp
            )
        }
        ArchiveCameraColorOverlay(
            backdrop = fullscreenBackdrop,
            isDark = isDark,
            shape = RoundedCornerShape(0.dp),
            modifier = Modifier.matchParentSize()
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 16.dp, top = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = image.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = "${image.timestampLabel()} • ${image.expiryLabel()}",
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
            }
        }
    }
}

@Composable
private fun ArchiveCameraColorOverlay(
    backdrop: Backdrop,
    isDark: Boolean,
    shape: RoundedCornerShape,
    modifier: Modifier = Modifier
) {
    Box(
        modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                colorControls(
                    brightness = if (isDark) -0.02f else 0.01f,
                    contrast = 1.3f,
                    saturation = 1.7f
                )
            },
            onDrawSurface = {
                drawRect(
                    if (isDark) {
                        Color.Unspecified.copy(alpha = 0.08f)
                    } else {
                        Color.Unspecified.copy(alpha = 0.25f)
                    }
                )
            }
        )
    )
}

private enum class ArchiveSortMode(val label: String) {
    NewestFirst("Newest first"),
    OldestFirst("Oldest first"),
    ExpiringSoon("Expiring soon")
}

private data class ArchiveTimeRange(val totalMinutes: Int) {
    val hours: Int get() = totalMinutes / 60
    val minutes: Int get() = totalMinutes % 60
    val durationMillis: Long get() = totalMinutes * 60L * 1000L
    val label: String
        get() = when {
            hours == 0 -> "$minutes min"
            minutes == 0 -> "${hours}h"
            else -> "${hours}h ${minutes}m"
        }

    fun withHours(hour: Int): ArchiveTimeRange {
        val safeHour = hour.coerceIn(0, MAX_HOURS)
        val safeMinute = if (safeHour == MAX_HOURS) 0 else minutes
        return fromParts(safeHour, safeMinute)
    }

    fun withMinutes(minute: Int): ArchiveTimeRange {
        val safeMinute = if (hours == MAX_HOURS) 0 else minute.coerceIn(0, 59)
        return fromParts(hours, safeMinute)
    }

    companion object {
        private const val MAX_HOURS = 48
        private const val MAX_MINUTES = MAX_HOURS * 60
        val Last10Minutes = ArchiveTimeRange(10)

        fun fromParts(hours: Int, minutes: Int): ArchiveTimeRange {
            val total = (hours.coerceIn(0, MAX_HOURS) * 60 + minutes.coerceIn(0, 59))
                .coerceIn(1, MAX_MINUTES)
            return ArchiveTimeRange(total)
        }
    }
}

private fun List<LiveCameraArchiveImage>.filterBy(timeRange: ArchiveTimeRange): List<LiveCameraArchiveImage> {
    val duration = timeRange.durationMillis
    val cutoff = System.currentTimeMillis() - duration
    return filter { it.timestampMillis >= cutoff }
}

private fun List<LiveCameraArchiveImage>.sortedFor(sortMode: ArchiveSortMode): List<LiveCameraArchiveImage> {
    return when (sortMode) {
        ArchiveSortMode.NewestFirst -> sortedByDescending { it.timestampMillis }
        ArchiveSortMode.OldestFirst -> sortedBy { it.timestampMillis }
        ArchiveSortMode.ExpiringSoon -> sortedBy { it.expiresAtMillis }
    }
}

private fun LiveCameraArchiveImage.timestampLabel(): String {
    return SimpleDateFormat("MMM d, h:mm a", Locale.US).format(Date(timestampMillis))
}

private fun LiveCameraArchiveImage.expiryLabel(): String {
    val remainingMs = expiresAtMillis - System.currentTimeMillis()
    if (remainingMs <= 0L) return "Expired"
    val hours = remainingMs / (60L * 60L * 1000L)
    val minutes = (remainingMs % (60L * 60L * 1000L)) / (60L * 1000L)
    return when {
        hours >= 1L -> "Expires in ${hours}h"
        minutes > 1L -> "Expires in ${minutes}m"
        else -> "Expires soon"
    }
}
