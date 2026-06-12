package com.flights.studio

import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import androidx.compose.ui.viewinterop.AndroidView
import com.flights.studio.UiUtils.getWebStorageStats
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.capsule.ContinuousCapsule
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

data class WebStorageStats(
    val httpCache: Float,
    val cookies: Float,
    val webDb: Float
) {
    val total: Float
        get() = httpCache + cookies + webDb
}

private data class FlightThemeSpec(
    val id: String,
    val title: String,
    val previewName: String = title,
    val accent: Color,
    val page: Color,
    val card: Color,
    val header: Color,
    val row: Color,
    val arrivedRow: Color,
    val text: Color,
    val muted: Color,
    val border: Color,
    val glow: Color
)

private val LightFlightTheme = FlightThemeSpec(
    id = "light",
    title = "Light",
    accent = Color(0xFF2D7DF6),
    page = Color(0xFFF8FAFF),
    card = Color.White,
    header = Color(0xFFF0F4FA),
    row = Color.White,
    arrivedRow = Color(0xFFEAF8F0),
    text = Color(0xFF111827),
    muted = Color(0xFF687283),
    border = Color(0xFFE1E7F0),
    glow = Color(0x332D7DF6)
)

private val MintFlightTheme = FlightThemeSpec(
    id = "mint",
    title = "Mint",
    accent = Color(0xFF22B981),
    page = Color(0xFFF2FBF8),
    card = Color(0xFFFFFFFF),
    header = Color(0xFFEAF8F3),
    row = Color(0xFFFBFFFD),
    arrivedRow = Color(0xFFE3F8EE),
    text = Color(0xFF10201C),
    muted = Color(0xFF5D706A),
    border = Color(0xFFCFE9DF),
    glow = Color(0x4422B981)
)

private val SkyFlightTheme = FlightThemeSpec(
    id = "sky",
    title = "Sky",
    accent = Color(0xFF3B82F6),
    page = Color(0xFFF0F7FF),
    card = Color(0xFFFFFFFF),
    header = Color(0xFFE1EEFF),
    row = Color(0xFFF8FBFF),
    arrivedRow = Color(0xFFE4F6FF),
    text = Color(0xFF10243F),
    muted = Color(0xFF55708F),
    border = Color(0xFFC6DBF4),
    glow = Color(0x443B82F6)
)

private val OceanFlightTheme = FlightThemeSpec(
    id = "ocean",
    title = "Ocean",
    accent = Color(0xFF22D3EE),
    page = Color(0xFF071820),
    card = Color(0xFF0D2430),
    header = Color(0xFF133443),
    row = Color(0xFF0B202B),
    arrivedRow = Color(0xFF0B3A36),
    text = Color(0xFFE8FBFF),
    muted = Color(0xFF91B5C0),
    border = Color(0xFF235062),
    glow = Color(0x5522D3EE)
)

private val VioletFlightTheme = FlightThemeSpec(
    id = "violet",
    title = "Violet",
    accent = Color(0xFF8B5CF6),
    page = Color(0xFFF7F3FF),
    card = Color(0xFFFFFFFF),
    header = Color(0xFFEDE7FF),
    row = Color(0xFFFFFCFF),
    arrivedRow = Color(0xFFEDEBFF),
    text = Color(0xFF261B3F),
    muted = Color(0xFF6E6188),
    border = Color(0xFFDCD2F8),
    glow = Color(0x448B5CF6)
)

private val RoseFlightTheme = FlightThemeSpec(
    id = "rose",
    title = "Rose",
    accent = Color(0xFFEC4899),
    page = Color(0xFFFFF5FA),
    card = Color(0xFFFFFFFF),
    header = Color(0xFFFCE7F3),
    row = Color(0xFFFFFBFD),
    arrivedRow = Color(0xFFFFE8F1),
    text = Color(0xFF3E122A),
    muted = Color(0xFF856174),
    border = Color(0xFFF3C8DC),
    glow = Color(0x44EC4899)
)

private val AmberFlightTheme = FlightThemeSpec(
    id = "amber",
    title = "Amber",
    accent = Color(0xFFF59E0B),
    page = Color(0xFFFFFAEC),
    card = Color(0xFFFFFFFF),
    header = Color(0xFFFFF0C7),
    row = Color(0xFFFFFCF4),
    arrivedRow = Color(0xFFFFF4D9),
    text = Color(0xFF34230C),
    muted = Color(0xFF7E6741),
    border = Color(0xFFEEDCA9),
    glow = Color(0x44F59E0B)
)

private val GrayFlightTheme = FlightThemeSpec(
    id = "gray",
    title = "Gray",
    accent = Color(0xFF64748B),
    page = Color(0xFFF4F6F8),
    card = Color(0xFFFFFFFF),
    header = Color(0xFFECEFF3),
    row = Color(0xFFFDFDFE),
    arrivedRow = Color(0xFFE9EEF2),
    text = Color(0xFF1F2937),
    muted = Color(0xFF6B7280),
    border = Color(0xFFD9DEE6),
    glow = Color(0x3364748B)
)

private val DarkFlightTheme = FlightThemeSpec(
    id = "dark",
    title = "Dark",
    accent = Color(0xFF38BDF8),
    page = Color(0xFF07111C),
    card = Color(0xFF111C28),
    header = Color(0xFF172433),
    row = Color(0xFF101B27),
    arrivedRow = Color(0xFF0D332C),
    text = Color(0xFFEAF2F8),
    muted = Color(0xFF91A0AE),
    border = Color(0xFF263646),
    glow = Color(0x4438BDF8)
)

private val AutoFlightTheme = LightFlightTheme.copy(
    id = "auto",
    title = "Auto",
    previewName = "Auto (System)"
)

private val FlightThemeChoices = listOf(
    LightFlightTheme,
    MintFlightTheme,
    SkyFlightTheme,
    OceanFlightTheme,
    VioletFlightTheme,
    RoseFlightTheme,
    AmberFlightTheme,
    GrayFlightTheme,
    DarkFlightTheme,
    AutoFlightTheme
)

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val bottomSafe = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    var selectedTheme by remember { mutableStateOf(SettingsStore.webTheme(context)) }
    var hwAccel by remember { mutableStateOf(SettingsStore.hardwareAccel(context)) }
    var enhancedTable by remember { mutableStateOf(SettingsStore.enhancedTable(context)) }
    var groupFlights by remember { mutableStateOf(SettingsStore.groupFlights(context)) }
    var textZoom by remember { mutableIntStateOf(SettingsStore.textZoom(context).coerceIn(60, 100)) }
    var highContrastWeb by remember { mutableStateOf(SettingsStore.highContrastWeb(context)) }
    var reduceWebMotion by remember { mutableStateOf(SettingsStore.reduceWebMotion(context)) }
    var blockTrackers by remember { mutableStateOf(SettingsStore.blockTrackers(context)) }
    var cachePages by remember { mutableStateOf(SettingsStore.cachePages(context)) }
    var storageStats by remember { mutableStateOf(getWebStorageStats(context)) }
    var savePulse by remember { mutableIntStateOf(0) }
    var showSaved by remember { mutableStateOf(true) }
    val settingsBackdrop = rememberLayerBackdrop()

    fun markSaved() {
        savePulse++
        showSaved = true
    }

    LaunchedEffect(savePulse) {
        if (savePulse > 0) {
            showSaved = true
            delay(1800)
            showSaved = true
        }
    }

    val resolvedTheme = remember(selectedTheme, systemDark) {
        resolveFlightTheme(selectedTheme, systemDark)
    }
    val uiTheme = remember(systemDark) {
        if (systemDark) DarkFlightTheme else LightFlightTheme
    }
    val pageColor by animateColorAsState(
        if (systemDark) Color(0xFF101114) else Color(0xFFF8F9FB),
        label = "webSettingsPage"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(pageColor)
    ) {
        val tablet = maxWidth >= 700.dp
        val horizontalPadding = if (tablet) 34.dp else 0.dp
        val contentWidth = if (tablet) 980.dp else Dp.Unspecified

        Box(
            modifier = Modifier
                .matchParentSize()
                .layerBackdrop(settingsBackdrop)
        ) {
            ProfileBackdropImageLayer(
                modifier = Modifier.matchParentSize(),
                lightRes = R.drawable.light_grid_pattern,
                darkRes = R.drawable.dark_grid_pattern,
                imageAlpha = if (systemDark) 0.95f else 0.70f,
                scrimDark = 0.12f,
                scrimLight = 0.03f
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    top = 108.dp,
                    start = horizontalPadding,
                    end = horizontalPadding,
                    bottom = 88.dp + bottomSafe
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = contentWidth)
            ) {
                SectionCard(
                    title = "Appearance",
                    subtitle = "Theme, text size, and row style.",
                    icon = Icons.Default.Palette,
                    theme = resolvedTheme,
                    backdrop = settingsBackdrop,
                    tablet = tablet,
                    trailing = {
                        WebSettingsGlassActionButton(
                            backdrop = settingsBackdrop,
                            theme = resolvedTheme,
                            icon = Icons.Default.Cached,
                            text = "Reset",
                            onClick = {
                                selectedTheme = SettingsStore.DEFAULT_WEB_THEME
                                textZoom = SettingsStore.DEFAULT_TEXT_ZOOM
                                enhancedTable = true
                                groupFlights = false
                                highContrastWeb = false
                                hwAccel = true
                                blockTrackers = true
                                cachePages = true
                                reduceWebMotion = false
                                SettingsStore.setWebTheme(context, selectedTheme)
                                SettingsStore.setTextZoom(context, textZoom)
                                SettingsStore.setEnhancedTable(context, enhancedTable)
                                SettingsStore.setGroupFlights(context, groupFlights)
                                SettingsStore.setHighContrastWeb(context, highContrastWeb)
                                SettingsStore.setHardwareAccel(context, hwAccel)
                                SettingsStore.setBlockTrackers(context, blockTrackers)
                                SettingsStore.setCachePages(context, cachePages)
                                SettingsStore.setReduceWebMotion(context, reduceWebMotion)
                                markSaved()
                            }
                        )
                    }
                ) {
                    ThemeChooser(
                        selectedTheme = selectedTheme,
                        systemDark = systemDark,
                        tablet = tablet,
                        onSelect = {
                            selectedTheme = it
                            SettingsStore.setWebTheme(context, it)
                            markSaved()
                        }
                    )

                    Spacer(Modifier.height(16.dp))

                    OneUiTextSizePanel(
                        value = textZoom,
                        theme = resolvedTheme,
                        backdrop = settingsBackdrop,
                        mutedColor = resolvedTheme.muted,
                        onValueChange = {
                            val next = it.coerceIn(60, 100)
                            textZoom = next
                            SettingsStore.setTextZoom(context, next)
                            markSaved()
                        }
                    )

                    Spacer(Modifier.height(18.dp))

                    FlightPreviewHeader(theme = resolvedTheme)
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, resolvedTheme.border.copy(alpha = 0.42f), RoundedCornerShape(14.dp))
                    ) {
                        FlightRowsPreview(
                            theme = resolvedTheme,
                            groupedFlights = groupFlights,
                            textScale = textZoom,
                            enhancedTable = enhancedTable,
                            highContrast = highContrastWeb
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                SectionCard(
                    title = "Table & Display",
                    subtitle = null,
                    icon = Icons.Default.GridView,
                    theme = uiTheme,
                    backdrop = settingsBackdrop,
                    tablet = tablet
                ) {
                    val tableItems: @Composable ColumnScope.() -> Unit = {
                        SettingToggleCard(
                            title = "Enhanced table cells",
                            subtitle = "Adds squared cell blocks for faster scanning.",
                            icon = Icons.Default.GridView,
                            checked = enhancedTable,
                            theme = uiTheme,
                            backdrop = settingsBackdrop,
                            onChange = {
                                enhancedTable = it
                                SettingsStore.setEnhancedTable(context, it)
                                markSaved()
                            }
                        )
                        if (!tablet) Spacer(Modifier.height(10.dp))
                        SettingToggleCard(
                            title = "Grouped flights table",
                            subtitle = "Groups Delta, United, American, Alaska, and each airline by time.",
                            icon = Icons.Default.ViewAgenda,
                            checked = groupFlights,
                            theme = uiTheme,
                            backdrop = settingsBackdrop,
                            onChange = {
                                groupFlights = it
                                SettingsStore.setGroupFlights(context, it)
                                markSaved()
                            }
                        )
                        if (!tablet) Spacer(Modifier.height(10.dp))
                        SettingToggleCard(
                            title = "High contrast table",
                            subtitle = "Black, white, and grayscale flight rows.",
                            icon = Icons.Default.Contrast,
                            checked = highContrastWeb,
                            theme = uiTheme,
                            backdrop = settingsBackdrop,
                            onChange = {
                                highContrastWeb = it
                                SettingsStore.setHighContrastWeb(context, it)
                                markSaved()
                            }
                        )
                    }
                    if (tablet) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                Box(Modifier.weight(1f)) {
                                    SettingToggleCard(
                                        title = "Enhanced table cells",
                                        subtitle = "Adds squared cell blocks for faster scanning.",
                                        icon = Icons.Default.GridView,
                                        checked = enhancedTable,
                                        theme = uiTheme,
                                        backdrop = settingsBackdrop,
                                        onChange = {
                                            enhancedTable = it
                                            SettingsStore.setEnhancedTable(context, it)
                                            markSaved()
                                        }
                                    )
                                }
                                Box(Modifier.weight(1f)) {
                                    SettingToggleCard(
                                        title = "Grouped flights table",
                                        subtitle = "Groups airlines together and sorts each group by time.",
                                        icon = Icons.Default.ViewAgenda,
                                        checked = groupFlights,
                                        theme = uiTheme,
                                        backdrop = settingsBackdrop,
                                        onChange = {
                                            groupFlights = it
                                            SettingsStore.setGroupFlights(context, it)
                                            markSaved()
                                        }
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                Box(Modifier.weight(1f)) {
                                    SettingToggleCard(
                                        title = "High contrast table",
                                        subtitle = "Black, white, and grayscale flight rows.",
                                        icon = Icons.Default.Contrast,
                                        checked = highContrastWeb,
                                        theme = uiTheme,
                                        backdrop = settingsBackdrop,
                                        onChange = {
                                            highContrastWeb = it
                                            SettingsStore.setHighContrastWeb(context, it)
                                            markSaved()
                                        }
                                    )
                                }
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    } else {
                        Column { tableItems() }
                    }
                }

                Spacer(Modifier.height(12.dp))

                SectionCard(
                    title = "Performance",
                    subtitle = null,
                    icon = Icons.Default.Speed,
                    theme = uiTheme,
                    backdrop = settingsBackdrop,
                    tablet = tablet
                ) {
                    val performanceCards = listOf<@Composable () -> Unit>(
                        {
                            SettingToggleCard(
                                title = "Hardware acceleration",
                                subtitle = "Keeps scrolling and table animations smoother.",
                                icon = Icons.Default.Memory,
                                checked = hwAccel,
                                theme = uiTheme,
                                backdrop = settingsBackdrop,
                                onChange = {
                                    hwAccel = it
                                    SettingsStore.setHardwareAccel(context, it)
                                    markSaved()
                                }
                            )
                        },
                        {
                            SettingToggleCard(
                                title = "Block ads and trackers",
                                subtitle = "Reduces noisy web requests while pages load.",
                                icon = Icons.Default.Security,
                                checked = blockTrackers,
                                theme = uiTheme,
                                backdrop = settingsBackdrop,
                                onChange = {
                                    blockTrackers = it
                                    SettingsStore.setBlockTrackers(context, it)
                                    markSaved()
                                }
                            )
                        },
                        {
                            SettingToggleCard(
                                title = "Cache web pages",
                                subtitle = "Keeps recently opened airport pages faster.",
                                icon = Icons.Default.Storage,
                                checked = cachePages,
                                theme = uiTheme,
                                backdrop = settingsBackdrop,
                                onChange = {
                                    cachePages = it
                                    SettingsStore.setCachePages(context, it)
                                    markSaved()
                                }
                            )
                        },
                        {
                            SettingToggleCard(
                                title = "Reduce web motion",
                                subtitle = "Calms page transitions and animations.",
                                icon = Icons.Default.Waves,
                                checked = reduceWebMotion,
                                theme = uiTheme,
                                backdrop = settingsBackdrop,
                                onChange = {
                                    reduceWebMotion = it
                                    SettingsStore.setReduceWebMotion(context, it)
                                    markSaved()
                                }
                            )
                        }
                    )
                    if (tablet) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            performanceCards.chunked(2).forEach { rowItems ->
                                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                    rowItems.forEach { item ->
                                        Box(Modifier.weight(1f)) { item() }
                                    }
                                }
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            performanceCards.forEach { it() }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                SectionCard(
                    title = "WebView Storage",
                    subtitle = null,
                    icon = Icons.Default.Storage,
                    theme = uiTheme,
                    backdrop = settingsBackdrop,
                    tablet = tablet
                ) {
                    StorageUsagePanel(
                        stats = storageStats,
                        theme = uiTheme,
                        tablet = tablet,
                        backdrop = settingsBackdrop,
                        onClear = {
                            WebView(context).apply {
                                clearCache(true)
                                destroy()
                            }

                            CookieManager.getInstance().apply {
                                removeAllCookies(null)
                                flush()
                            }

                            WebStorage.getInstance().deleteAllData()
                            context.deleteDatabase("webview.db")
                            context.deleteDatabase("webviewCache.db")

                            listOf(
                                File(context.cacheDir, "WebView"),
                                File(context.applicationInfo.dataDir, "app_webview")
                            ).forEach { file ->
                                if (file.exists()) file.deleteRecursively()
                            }

                            storageStats = getWebStorageStats(context)
                            markSaved()
                        }
                    )
                }

                Spacer(Modifier.height(12.dp))

                SaveConfirmation(
                    visible = showSaved,
                    theme = uiTheme
                )
            }
        }
    }
}

private fun resolveFlightTheme(themeId: String, systemDark: Boolean): FlightThemeSpec {
    return when (themeId) {
        "light" -> LightFlightTheme
        "mint" -> MintFlightTheme
        "sky" -> SkyFlightTheme
        "ocean" -> OceanFlightTheme
        "violet" -> VioletFlightTheme
        "rose" -> RoseFlightTheme
        "amber" -> AmberFlightTheme
        "gray" -> GrayFlightTheme
        "dark" -> DarkFlightTheme
        "auto" -> if (systemDark) DarkFlightTheme.copy(id = "auto", title = "Auto", previewName = "Auto (System)") else AutoFlightTheme
        else -> MintFlightTheme
    }
}

@Composable
private fun AviationGridOverlay(
    accent: Color,
    dark: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        val spacing = 44.dp.toPx()
        val lineColor = accent.copy(alpha = if (dark) 0.055f else 0.04f)
        var x = 0f
        while (x < size.width) {
            drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
            x += spacing
        }
        var y = 0f
        while (y < size.height) {
            drawLine(lineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            y += spacing
        }
        drawLine(
            color = accent.copy(alpha = if (dark) 0.13f else 0.10f),
            start = Offset(size.width * 0.08f, size.height * 0.18f),
            end = Offset(size.width * 0.86f, size.height * 0.04f),
            strokeWidth = 2f,
            cap = StrokeCap.Round
        )
    }
}

private fun Modifier.webSettingsGlass(
    backdrop: Backdrop?,
    shape: RoundedCornerShape,
    theme: FlightThemeSpec,
    surfaceAlpha: Float
): Modifier {
    val fallback = Brush.verticalGradient(
        listOf(
            theme.card.copy(alpha = surfaceAlpha + 0.12f),
            theme.card.copy(alpha = surfaceAlpha)
        )
    )
    return if (backdrop == null) {
        background(fallback)
    } else {
        drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            highlight = {
                Highlight(
                    width = 0.45.dp,
                    blurRadius = 1.4.dp,
                    alpha = 0.78f,
                    style = HighlightStyle.Plain
                )
            },
            shadow = null,
            effects = {
                vibrancy()
                blur(1.7.dp.toPx(), edgeTreatment = TileMode.Clamp)
                lens(
                    refractionHeight = 18.dp.toPx(),
                    refractionAmount = 26.dp.toPx(),
                    depthEffect = false,
                    chromaticAberration = false
                )
            },
            onDrawSurface = {
                drawRect(theme.card.copy(alpha = surfaceAlpha))
                drawRect(Color.White.copy(alpha = if (theme.id == "dark") 0.035f else 0.18f))
            }
        )
    }
}

@Composable
private fun WebSettingsGlassActionButton(
    backdrop: Backdrop?,
    theme: FlightThemeSpec,
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = theme.accent,
    surfaceAlpha: Float = 0.34f
) {
    val shape = RoundedCornerShape(999.dp)
    Row(
        modifier = modifier
            .height(38.dp)
            .clip(shape)
            .background(contentColor.copy(alpha = 0.11f), shape)
            .border(1.dp, contentColor.copy(alpha = 0.20f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(7.dp))
        Text(text, color = contentColor, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
    }
}

@Composable
private fun OneUiTextSizePanel(
    value: Int,
    theme: FlightThemeSpec,
    backdrop: Backdrop,
    mutedColor: Color,
    onValueChange: (Int) -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(theme.muted.copy(alpha = 0.07f), shape)
            .border(1.dp, theme.border.copy(alpha = 0.34f), shape)
            .padding(horizontal = 14.dp, vertical = 13.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(theme.accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.TextFields, contentDescription = null, tint = theme.accent, modifier = Modifier.size(18.dp))
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text("Text size", color = theme.text, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                Text("Flight table scale", color = theme.muted, fontSize = 12.sp, lineHeight = 16.sp)
            }

            AnimatedContent(value, label = "textZoomValue") { current ->
                Text(
                    text = "$current%",
                    color = theme.accent,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("60", color = mutedColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            GlassTextSizeSlider(
                value = value,
                onValueChange = onValueChange,
                backdrop = backdrop,
                theme = theme,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            )
            Column(horizontalAlignment = Alignment.End) {
                Text("100", color = mutedColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Max", color = mutedColor.copy(alpha = 0.72f), fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun GlassTextSizeSlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    backdrop: Backdrop,
    theme: FlightThemeSpec,
    modifier: Modifier = Modifier
) {
    val min = 60f
    val max = 100f
    val valueRange = min..max
    val isLightTheme = theme.id != "dark"
    val accentColor = if (isLightTheme) Color(0xFF0088FF) else Color(0xFF0091FF)
    val trackColor = if (isLightTheme) Color(0xFF787878).copy(alpha = 0.20f) else Color(0xFF787880).copy(alpha = 0.36f)
    val trackBackdrop = rememberLayerBackdrop()

    BoxWithConstraints(
        modifier = modifier
            .height(44.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        val trackWidth = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val animationScope = rememberCoroutineScope()
        var didDrag by remember { mutableStateOf(false) }
        var touchDragValue by remember { mutableStateOf(value.toFloat().coerceIn(valueRange)) }
        var touchDragging by remember { mutableStateOf(false) }
        val dampedDragAnimation = remember(animationScope) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = value.toFloat().coerceIn(valueRange),
                valueRange = valueRange,
                visibilityThreshold = 0.5f,
                initialScale = 1f,
                pressedScale = 1.5f,
                onDragStarted = {},
                onDragStopped = {
                    if (didDrag) {
                        onValueChange(targetValue.fastRoundToInt().coerceIn(min.toInt(), max.toInt()))
                        didDrag = false
                    }
                },
                onDrag = { _, dragAmount ->
                    if (!didDrag) {
                        didDrag = dragAmount.x != 0f
                    }
                    val delta = (valueRange.endInclusive - valueRange.start) * (dragAmount.x / trackWidth)
                    val nextValue = if (isLtr) {
                        targetValue + delta
                    } else {
                        targetValue - delta
                    }.coerceIn(valueRange)
                    onValueChange(nextValue.fastRoundToInt().coerceIn(min.toInt(), max.toInt()))
                }
            )
        }

        LaunchedEffect(dampedDragAnimation) {
            snapshotFlow { value.toFloat().coerceIn(valueRange) }
                .collectLatest { current ->
                    if (!touchDragging) {
                        touchDragValue = current
                    }
                    if (dampedDragAnimation.targetValue != current) {
                        dampedDragAnimation.updateValue(current)
                    }
                }
        }

        Box(
            Modifier.layerBackdrop(trackBackdrop)
        ) {
            Box(
                modifier = Modifier
                    .clip(ContinuousCapsule)
                    .background(trackColor)
                    .pointerInput(animationScope, trackWidth, isLtr) {
                        detectTapGestures { position ->
                            val delta = (valueRange.endInclusive - valueRange.start) * (position.x / trackWidth)
                            val targetValue = (if (isLtr) {
                                valueRange.start + delta
                            } else {
                                valueRange.endInclusive - delta
                            }).coerceIn(valueRange)
                            dampedDragAnimation.animateToValue(targetValue)
                            onValueChange(targetValue.fastRoundToInt().coerceIn(min.toInt(), max.toInt()))
                        }
                    }
                    .height(6.dp)
                    .fillMaxWidth()
            )
            Box(
                modifier = Modifier
                    .clip(ContinuousCapsule)
                    .background(accentColor)
                    .height(6.dp)
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val width = (constraints.maxWidth * dampedDragAnimation.progress).fastRoundToInt()
                        layout(width, placeable.height) {
                            placeable.place(0, 0)
                        }
                    }
            )
        }

        Box(
            modifier = Modifier
                .graphicsLayer {
                    translationX = (
                            -size.width / 2f + trackWidth * dampedDragAnimation.progress
                            ).fastCoerceIn(-size.width / 4f, trackWidth - size.width * 3f / 4f) *
                            if (isLtr) 1f else -1f
                }
                .then(dampedDragAnimation.modifier)
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(
                        backdrop,
                        rememberBackdrop(trackBackdrop) { drawBackdrop ->
                            val pressProgress = dampedDragAnimation.pressProgress
                            val scaleX = lerp(2f / 3f, 1f, pressProgress)
                            val scaleY = lerp(0f, 1f, pressProgress)
                            scale(scaleX, scaleY) {
                                drawBackdrop()
                            }
                        }
                    ),
                    shape = { ContinuousCapsule },
                    effects = {
                        val pressProgress = dampedDragAnimation.pressProgress
                        blur(8.dp.toPx() * (1f - pressProgress))
                        lens(
                            refractionHeight = 10.dp.toPx() * pressProgress,
                            refractionAmount = 14.dp.toPx() * pressProgress,
                            chromaticAberration = true
                        )
                    },
                    highlight = {
                        val pressProgress = dampedDragAnimation.pressProgress
                        Highlight.Ambient.copy(
                            width = Highlight.Ambient.width / 1.5f,
                            blurRadius = Highlight.Ambient.blurRadius / 1.5f,
                            alpha = pressProgress
                        )
                    },
                    shadow = {
                        Shadow(
                            radius = 4.dp,
                            color = Color.Black.copy(alpha = 0.05f)
                        )
                    },
                    innerShadow = {
                        val pressProgress = dampedDragAnimation.pressProgress
                        InnerShadow(
                            radius = 4.dp * pressProgress,
                            alpha = pressProgress
                        )
                    },
                    layerBlock = {
                        scaleX = dampedDragAnimation.scaleX
                        scaleY = dampedDragAnimation.scaleY
                        val velocity = dampedDragAnimation.velocity / 10f
                        scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    },
                    onDrawSurface = {
                        val pressProgress = dampedDragAnimation.pressProgress
                        drawRect(Color.White.copy(alpha = 1f - pressProgress))
                    }
                )
                .size(40.dp, 24.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .pointerInput(trackWidth, isLtr) {
                    detectTapGestures { position ->
                        val delta = (valueRange.endInclusive - valueRange.start) * (position.x / trackWidth)
                        val targetValue = (if (isLtr) {
                            valueRange.start + delta
                        } else {
                            valueRange.endInclusive - delta
                        }).coerceIn(valueRange)
                        touchDragValue = targetValue
                        dampedDragAnimation.animateToValue(targetValue)
                        onValueChange(targetValue.fastRoundToInt().coerceIn(min.toInt(), max.toInt()))
                    }
                }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { dragAmount ->
                        val delta = (valueRange.endInclusive - valueRange.start) * (dragAmount / trackWidth)
                        touchDragValue = (if (isLtr) {
                            touchDragValue + delta
                        } else {
                            touchDragValue - delta
                        }).coerceIn(valueRange)
                        dampedDragAnimation.updateValue(touchDragValue)
                        onValueChange(touchDragValue.fastRoundToInt().coerceIn(min.toInt(), max.toInt()))
                    },
                    startDragImmediately = true,
                    onDragStarted = {
                        touchDragging = true
                        touchDragValue = value.toFloat().coerceIn(valueRange)
                        dampedDragAnimation.press()
                    },
                    onDragStopped = {
                        touchDragging = false
                        dampedDragAnimation.release()
                    }
                )
        )
    }
}

@Composable
private fun FlightPreviewHeader(theme: FlightThemeSpec) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(theme.accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Visibility, contentDescription = null, tint = theme.accent, modifier = Modifier.size(18.dp))
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text("Flight row appearance", color = theme.text, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            Text("Current table style", color = theme.muted, fontSize = 12.sp, lineHeight = 16.sp)
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(theme.accent.copy(alpha = 0.11f))
                .border(1.dp, theme.accent.copy(alpha = 0.20f), RoundedCornerShape(999.dp))
                .padding(horizontal = 11.dp, vertical = 6.dp)
        ) {
            Text("Live", color = theme.accent, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String?,
    icon: ImageVector,
    theme: FlightThemeSpec,
    backdrop: Backdrop?,
    tablet: Boolean,
    modifier: Modifier = Modifier,
    cornerRadius: Dp? = null,
    contentEdgeToEdge: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardShape = RoundedCornerShape(cornerRadius ?: 0.dp)
    val contentPadding = if (tablet) 20.dp else 16.dp
    val isDark = isSystemInDarkTheme()
    val surfaceColor = if (isDark) {
        Color(0xFF17191D).copy(alpha = 0.88f)
    } else {
        Color.White.copy(alpha = 0.76f)
    }
    val borderColor = theme.border.copy(alpha = if (isDark) 0.34f else 0.22f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(surfaceColor, cardShape)
            .border(1.dp, borderColor, cardShape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = contentPadding, top = 12.dp, end = contentPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(theme.accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = theme.accent, modifier = Modifier.size(19.dp))
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    color = theme.text,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    lineHeight = 19.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (subtitle != null) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        subtitle,
                        color = theme.muted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (trailing != null) {
                Spacer(Modifier.width(10.dp))
                trailing()
            }
        }

        Spacer(Modifier.height(12.dp))
        if (contentEdgeToEdge) {
            content()
        } else {
            Column(
                modifier = Modifier.padding(
                    start = contentPadding,
                    end = contentPadding,
                    bottom = contentPadding
                ),
                content = content
            )
        }
    }
}

@Composable
private fun ThemeChooser(
    selectedTheme: String,
    systemDark: Boolean,
    tablet: Boolean,
    onSelect: (String) -> Unit
) {
    val cardWidth = if (tablet) 118.dp else 98.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FlightThemeChoices.forEach { theme ->
            ThemePreviewCard(
                theme = theme,
                actualTheme = resolveFlightTheme(theme.id, systemDark),
                selected = selectedTheme == theme.id,
                modifier = Modifier.width(cardWidth),
                onClick = { onSelect(theme.id) }
            )
        }
    }
}

@Composable
private fun ThemePreviewCard(
    theme: FlightThemeSpec,
    actualTheme: FlightThemeSpec,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        if (selected) theme.accent else actualTheme.border,
        label = "themeBorder"
    )
    val selectedScale by animateFloatAsState(
        if (selected) 1f else 0.96f,
        animationSpec = tween(180),
        label = "themeScale"
    )
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(actualTheme.card)
            .border(BorderStroke(if (selected) 2.dp else 1.dp, borderColor), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(actualTheme.page)
                .border(1.dp, actualTheme.border.copy(alpha = 0.72f), RoundedCornerShape(9.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(5.dp)).background(actualTheme.header))
            Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(5.dp)).background(actualTheme.row))
            Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(5.dp)).background(actualTheme.arrivedRow))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (selected) "${theme.previewName} (Current)" else theme.previewName,
            color = if (selected) theme.accent else actualTheme.text,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(7.dp))
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (selected) theme.accent else Color.Transparent)
                .border(1.5.dp, if (selected) theme.accent else actualTheme.muted.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size((15.dp * selectedScale))
                )
            }
        }
    }
}

@Composable
private fun FlightRowsPreview(
    theme: FlightThemeSpec,
    groupedFlights: Boolean,
    textScale: Int,
    enhancedTable: Boolean,
    highContrast: Boolean
) {
    val previewTheme = if (highContrast) highContrastTheme(theme) else theme
    val previewShape = RoundedCornerShape(14.dp)
    val previewHeight by animateDpAsState(
        targetValue = flightPreviewHeight(groupedFlights, textScale),
        animationSpec = tween(180),
        label = "flightPreviewHeight"
    )
    val previewHtml = remember(theme.id, groupedFlights, textScale, enhancedTable, highContrast) {
        flightRowsPreviewHtml(
            themeId = theme.id,
            groupedFlights = groupedFlights,
            textScale = textScale,
            enhancedTable = enhancedTable,
            highContrast = highContrast
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(previewHeight)
            .clip(previewShape)
            .background(previewTheme.page)
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            factory = { context ->
                WebView(context).apply {
                    tag = previewHtml
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    isHorizontalScrollBarEnabled = false
                    isVerticalScrollBarEnabled = false
                    overScrollMode = WebView.OVER_SCROLL_NEVER
                    settings.javaScriptEnabled = false
                    settings.domStorageEnabled = false
                    settings.textZoom = 100
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    loadDataWithBaseURL(
                        "file:///android_asset/",
                        previewHtml,
                        "text/html",
                        "UTF-8",
                        null
                    )
                }
            },
            update = { webView ->
                if (webView.tag != previewHtml) {
                    webView.tag = previewHtml
                    webView.settings.textZoom = 100
                    webView.loadDataWithBaseURL(
                        "file:///android_asset/",
                        previewHtml,
                        "text/html",
                        "UTF-8",
                        null
                    )
                }
            }
        )
    }
}

private fun flightPreviewHeight(groupedFlights: Boolean, textScale: Int): Dp {
    val scale = textScale.coerceIn(60, 100) / 100f
    val baseHeight = if (groupedFlights) {
        36f + 38f + 42f + 40f + 40f + 38f + 42f + 40f + 40f
    } else {
        36f + 38f + 40f + 40f
    }
    return (baseHeight * scale + 2f).dp
}

private fun flightRowsPreviewHtml(
    themeId: String,
    groupedFlights: Boolean,
    textScale: Int,
    enhancedTable: Boolean,
    highContrast: Boolean
): String {
    val classes = mutableListOf("fs-theme-$themeId", "fs-settings-preview").apply {
        if (enhancedTable) add("fs-enhanced-table")
        if (groupedFlights) add("fs-grouped-flights")
        if (highContrast) add("fs-web-high-contrast")
    }.joinToString(" ")

    val rows = if (groupedFlights) groupedPreviewRowsHtml() else standardPreviewRowsHtml()
    val runtimeCss = flightTableRuntimeCss(
        theme = themeId,
        textZoom = textScale,
        previewFrame = true
    )

    return """
        <!doctype html>
        <html class="$classes">
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">
          <link rel="stylesheet" href="fs_flights_style.css">
          <style>
            $runtimeCss
          </style>
        </head>

        <body>
          <div id="flight-container">
            <div class="flight-table-wrap">
              <div class="table-scroll">
                <table class="jha-flights">
                  <thead>
                    <tr>
                      <th>AIRLINE</th>
                      <th>FLIGHT</th>
                      <th>FROM</th>
                      <th>SCHED</th>
                      <th>ACTUAL</th>
                      <th>STATUS</th>
                    </tr>
                  </thead>
                  <tbody>
                    $rows
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </body>
        </html>
    """.trimIndent()
}


private fun groupedPreviewRowsHtml(): String {
    return """
        <tr>
          <td class="day" colspan="6">
            <span class="fs-day-label">Today</span>
            <span class="fs-day-count">2 flights total</span>
            <span class="fs-day-updated">last updated 11:50pm MST</span>
          </td>
        </tr>
        ${airlineGroupRowHtml("United", "first 9:46am")}
        ${flightRowHtml("United", "5730", "Denver", "9:46am", "9:34am", "Arrived", "-green", arrived = true)}
        ${flightRowHtml("United", "1306", "Denver", "8:58pm", "8:44pm", "Arrived", "-green", arrived = true)}
        <tr>
          <td class="day" colspan="6">
            <span class="fs-day-label">Tomorrow</span>
            <span class="fs-day-count">2 flights total</span>
            <span class="fs-day-updated">last updated 11:50pm MST</span>
          </td>
        </tr>
        ${airlineGroupRowHtml("Alaska", "first 2:47pm")}
        ${flightRowHtml("Alaska", "3468", "San Francisco", "2:47pm", "2:48pm", "On Time", "")}
        ${flightRowHtml("Alaska", "3469", "San Francisco", "6:15pm", "6:11pm", "On Time", "")}
    """.trimIndent()
}

private fun standardPreviewRowsHtml(): String {
    return """
        <tr>
          <td class="day" colspan="6">
            <span class="fs-day-label">Today</span>
            <span class="fs-day-count">2 flights total</span>
            <span class="fs-day-updated">last updated 11:50pm MST</span>
          </td>
        </tr>
        ${flightRowHtml("United", "5730", "Denver", "9:46am", "9:34am", "Arrived", "-green", arrived = true)}
        ${flightRowHtml("Alaska", "3468", "San Francisco", "2:47pm", "2:48pm", "On Time", "")}
    """.trimIndent()
}

private fun airlineGroupRowHtml(airline: String, firstTime: String): String {
    return """
        <tr class="fs-airline-group-row">
          <td colspan="6">
            <div class="fs-airline-group-label">
              <span>$airline</span>
              <span class="fs-airline-group-meta">2 flights • $firstTime</span>
            </div>
          </td>
        </tr>
    """.trimIndent()
}

private fun flightRowHtml(
    airline: String,
    flight: String,
    from: String,
    sched: String,
    actual: String,
    status: String,
    statusClass: String,
    arrived: Boolean = false
): String {
    val rowClass = if (arrived) " class=\"fs-flight-detail-ready fs-row-arrived\"" else " class=\"fs-flight-detail-ready\""

    return """
        <tr$rowClass>
          <td class="airline"><span class="fs-cell-chip fs-cell-airline">$airline</span></td>
          <td class="flight"><span class="fs-cell-chip fs-cell-flight">$flight</span></td>
          <td class="from"><span class="fs-cell-chip fs-cell-from">$from</span></td>

          <td class="sched">
            <span class="fs-cell-chip fs-cell-sched">$sched</span>
          </td>

          <td class="actual">
            <span class="fs-cell-chip fs-cell-actual">$actual</span>
          </td>

          <td class="status">
            <span class="$statusClass">$status</span>
          </td>
        </tr>
    """.trimIndent()
}

@Composable
private fun MiniFlightTable(
    theme: FlightThemeSpec,
    textScale: Int,
    enhancedTable: Boolean,
    groupedFlights: Boolean = false,
    highContrast: Boolean,
    compact: Boolean
) {
    val scale = (textScale / 100f).coerceIn(0.6f, 1f)
    val headerSize = ((if (compact) 6.2f else 10f) * scale).sp
    val rowSize = ((if (compact) 7.3f else 13f) * scale).sp
    val rowHeight = if (compact) 20.dp else 50.dp
    val previewTheme = if (highContrast) highContrastTheme(theme) else theme
    val tableWidth = if (compact) 104.dp else 860.dp

    Column(
        modifier = Modifier
            .then(if (compact) Modifier.fillMaxWidth() else Modifier.horizontalScroll(rememberScrollState()))
            .clip(RoundedCornerShape(if (compact) 8.dp else 13.dp))
            .background(previewTheme.card)
    ) {
        Column(modifier = Modifier.width(tableWidth)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (compact) 18.dp else 36.dp)
                    .background(previewTheme.header)
                    .padding(horizontal = if (compact) 3.dp else 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TableHeader("AIRLINE", 1.2f, previewTheme, headerSize)
                TableHeader("FLIGHT", 1f, previewTheme, headerSize)
                TableHeader("FROM", 1.55f, previewTheme, headerSize)
                if (!compact) TableHeader("SCHED", 1f, previewTheme, headerSize, alignEnd = true)
                TableHeader("ACTUAL", 1f, previewTheme, headerSize, alignEnd = true)
                TableHeader("STATUS", 1.15f, previewTheme, headerSize, alignEnd = true)
            }
            if (groupedFlights && !compact) {
                FlightDayHeader("Today", previewTheme)
                FlightGroupHeader("United", "first 9:46am", previewTheme)
            }
            FlightPreviewRow(
                airline = if (compact) "UAL" else "United",
                flight = "5730",
                from = if (compact) "DEN" else "Denver",
                sched = "9:46am",
                actual = "9:34am",
                status = "Arrived",
                rowBg = previewTheme.arrivedRow,
                theme = previewTheme,
                textSize = rowSize,
                height = rowHeight,
                enhancedTable = enhancedTable,
                compact = compact
            )
            if (groupedFlights && !compact) {
                FlightPreviewRow(
                    airline = "United",
                    flight = "1306",
                    from = "Denver",
                    sched = "8:58pm",
                    actual = "8:44pm",
                    status = "Arrived",
                    rowBg = previewTheme.arrivedRow,
                    theme = previewTheme,
                    textSize = rowSize,
                    height = rowHeight,
                    enhancedTable = enhancedTable,
                    compact = false
                )
                FlightDayHeader("Tomorrow", previewTheme)
                FlightGroupHeader("Alaska", "first 2:47pm", previewTheme)
            }
            FlightPreviewRow(
                airline = if (compact) "ASA" else "Alaska",
                flight = "3468",
                from = if (compact) "SFO" else "San Francisco",
                sched = "2:47pm",
                actual = "2:48pm",
                status = "On Time",
                rowBg = previewTheme.row,
                theme = previewTheme,
                textSize = rowSize,
                height = rowHeight,
                enhancedTable = enhancedTable,
                compact = compact
            )
            if (groupedFlights && !compact) {
                FlightPreviewRow(
                    airline = "Alaska",
                    flight = "3469",
                    from = "San Francisco",
                    sched = "6:15pm",
                    actual = "6:11pm",
                    status = "On Time",
                    rowBg = previewTheme.row,
                    theme = previewTheme,
                    textSize = rowSize,
                    height = rowHeight,
                    enhancedTable = enhancedTable,
                    compact = false
                )
            }
            if (compact) {
                FlightPreviewRow(
                    airline = "DAL",
                    flight = "4048",
                    from = "SLC",
                    sched = "4:06pm",
                    actual = "Delayed",
                    status = "Delayed",
                    rowBg = previewTheme.row,
                    theme = previewTheme,
                    textSize = rowSize,
                    height = rowHeight,
                    enhancedTable = enhancedTable,
                    compact = true
                )
            }
        }
    }
}

@Composable
private fun FlightDayHeader(
    label: String,
    theme: FlightThemeSpec
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .background(theme.header.copy(alpha = 0.98f))
            .border(0.5.dp, theme.border.copy(alpha = 0.72f))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = theme.text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(theme.accent.copy(alpha = 0.14f))
                .border(0.5.dp, theme.accent.copy(alpha = 0.45f), RoundedCornerShape(999.dp))
                .padding(horizontal = 12.dp, vertical = 5.dp)
        ) {
            Text(
                text = "2 flights total",
                color = theme.accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun FlightGroupHeader(
    airline: String,
    firstTime: String,
    theme: FlightThemeSpec
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        theme.header.copy(alpha = 0.95f),
                        theme.card.copy(alpha = 0.98f)
                    )
                )
            )
            .border(0.5.dp, theme.border.copy(alpha = 0.78f))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = airline,
            color = theme.text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "2 flights  •  $firstTime",
            color = theme.muted,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1
        )
    }
}

@Composable
private fun RowScope.TableHeader(
    label: String,
    weight: Float,
    theme: FlightThemeSpec,
    size: androidx.compose.ui.unit.TextUnit,
    alignEnd: Boolean = false
) {
    Text(
        text = label,
        color = theme.muted,
        fontSize = size,
        fontWeight = FontWeight.Black,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(weight),
        textAlign = if (alignEnd) androidx.compose.ui.text.style.TextAlign.End else androidx.compose.ui.text.style.TextAlign.Start
    )
}

@Composable
private fun FlightPreviewRow(
    airline: String,
    flight: String,
    from: String,
    sched: String,
    actual: String,
    status: String,
    rowBg: Color,
    theme: FlightThemeSpec,
    textSize: androidx.compose.ui.unit.TextUnit,
    height: Dp,
    enhancedTable: Boolean,
    compact: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .background(rowBg)
            .border(0.5.dp, theme.border.copy(alpha = 0.65f))
            .padding(horizontal = if (compact) 3.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FlightCell(airline, 1.2f, theme, textSize, enhancedTable)
        FlightCell(flight, 1f, theme, textSize, enhancedTable)
        FlightCell(from, 1.55f, theme, textSize, enhancedTable)
        if (!compact) FlightCell(sched, 1f, theme, textSize, enhancedTable, alignEnd = true)
        FlightCell(actual, 1f, theme, textSize, enhancedTable, alignEnd = true)
        StatusCell(status, 1.15f, textSize, alignEnd = true)
    }
}

@Composable
private fun RowScope.FlightCell(
    value: String,
    weight: Float,
    theme: FlightThemeSpec,
    textSize: androidx.compose.ui.unit.TextUnit,
    enhanced: Boolean,
    alignEnd: Boolean = false
) {
    Box(
        modifier = Modifier.weight(weight),
        contentAlignment = if (alignEnd) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = if (enhanced) {
                Modifier
                    .clip(RoundedCornerShape(7.dp))
                    .background(theme.muted.copy(alpha = 0.10f))
                    .border(1.dp, theme.border, RoundedCornerShape(7.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            } else {
                Modifier.padding(horizontal = 2.dp)
            }
        ) {
            Text(
                value,
                color = theme.text,
                fontSize = textSize,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RowScope.StatusCell(
    value: String,
    weight: Float,
    textSize: androidx.compose.ui.unit.TextUnit,
    alignEnd: Boolean
) {
    Box(
        modifier = Modifier.weight(weight),
        contentAlignment = if (alignEnd) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        val good = value == "Arrived"
        val chipWidth = 46.dp
        val chipHeight = 20.dp
        Box(
            modifier = Modifier
                .width(chipWidth)
                .height(chipHeight)
                .clip(RoundedCornerShape(999.dp))
                .background(if (good) Color(0xFFC6F3D4) else Color(0xFFD8ECFF))
                .border(1.dp, if (good) Color(0xFF5BCC7F) else Color(0xFF7DB7ED), RoundedCornerShape(999.dp))
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                value,
                color = if (good) Color(0xFF116D38) else Color(0xFF1165B8),
                fontSize = textSize * 0.9f,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun highContrastTheme(theme: FlightThemeSpec): FlightThemeSpec {
    val dark = theme.id == "dark" || theme.id == "ocean" || theme.id == "graphite"
    return theme.copy(
        card = if (dark) Color.Black else Color.White,
        header = if (dark) Color(0xFF181818) else Color(0xFFEDEDED),
        row = if (dark) Color(0xFF070707) else Color.White,
        arrivedRow = if (dark) Color(0xFF121212) else Color(0xFFF1F1F1),
        text = if (dark) Color.White else Color.Black,
        muted = if (dark) Color(0xFFC7C7C7) else Color(0xFF3E3E3E),
        border = if (dark) Color(0xFF444444) else Color(0xFFCCCCCC)
    )
}

@Composable
private fun SettingToggleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    theme: FlightThemeSpec,
    backdrop: Backdrop? = null,
    onChange: (Boolean) -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(shape)
                .clickable { onChange(!checked) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(theme.accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = theme.accent, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = theme.text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 19.sp)
                Spacer(Modifier.height(3.dp))
                Text(subtitle, color = theme.muted, fontSize = 12.sp, lineHeight = 16.sp)
            }
        }
        Spacer(Modifier.width(10.dp))
        LiquidSettingsToggle(
            selected = { checked },
            onSelect = onChange,
            backdrop = backdrop,
            theme = theme
        )
    }
}

@Composable
private fun LiquidSettingsToggle(
    selected: () -> Boolean,
    onSelect: (Boolean) -> Unit,
    backdrop: Backdrop?,
    theme: FlightThemeSpec,
    modifier: Modifier = Modifier
) {
    val isLightTheme = theme.id != "dark"
    val accentColor = if (isLightTheme) Color(0xFF34C759) else Color(0xFF30D158)
    val trackColor = if (isLightTheme) Color(0xFF787878).copy(alpha = 0.20f) else Color(0xFF787880).copy(alpha = 0.36f)
    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val dragWidth = with(density) { 20.dp.toPx() }
    val animationScope = rememberCoroutineScope()
    var didDrag by remember { mutableStateOf(false) }
    var fraction by remember { mutableStateOf(if (selected()) 1f else 0f) }
    val dampedDragAnimation = remember(animationScope) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = fraction,
            valueRange = 0f..1f,
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 1.5f,
            onDragStarted = {},
            onDragStopped = {
                if (didDrag) {
                    fraction = if (targetValue >= 0.5f) 1f else 0f
                    onSelect(fraction == 1f)
                    didDrag = false
                } else {
                    fraction = if (selected()) 0f else 1f
                    onSelect(fraction == 1f)
                }
            },
            onDrag = { _, dragAmount ->
                if (!didDrag) {
                    didDrag = dragAmount.x != 0f
                }
                val delta = dragAmount.x / dragWidth
                fraction = if (isLtr) {
                    (fraction + delta).fastCoerceIn(0f, 1f)
                } else {
                    (fraction - delta).fastCoerceIn(0f, 1f)
                }
            }
        )
    }

    LaunchedEffect(dampedDragAnimation) {
        snapshotFlow { fraction }
            .collectLatest { current ->
                dampedDragAnimation.updateValue(current)
            }
    }

    LaunchedEffect(selected()) {
        val target = if (selected()) 1f else 0f
        if (target != fraction) {
            fraction = target
            dampedDragAnimation.animateToValue(target)
        }
    }

    val trackBackdrop = rememberLayerBackdrop()
    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            Modifier
                .layerBackdrop(trackBackdrop)
                .clip(ContinuousCapsule)
                .drawBehind {
                    drawRect(lerpColor(trackColor, accentColor, dampedDragAnimation.value))
                }
                .size(64.dp, 28.dp)
        )
        Box(
            Modifier
                .graphicsLayer {
                    val padding = 2.dp.toPx()
                    translationX = if (isLtr) {
                        lerp(padding, padding + dragWidth, dampedDragAnimation.value)
                    } else {
                        lerp(-padding, -(padding + dragWidth), dampedDragAnimation.value)
                    }
                }
                .semantics { role = Role.Switch }
                .then(dampedDragAnimation.modifier)
                .then(
                    if (backdrop != null) {
                        Modifier.drawBackdrop(
                            backdrop = rememberCombinedBackdrop(
                                backdrop,
                                rememberBackdrop(trackBackdrop) { drawBackdrop ->
                                    val pressProgress = dampedDragAnimation.pressProgress
                                    val scaleX = lerp(2f / 3f, 0.75f, pressProgress)
                                    val scaleY = lerp(0f, 0.75f, pressProgress)
                                    scale(scaleX, scaleY) {
                                        drawBackdrop()
                                    }
                                }
                            ),
                            shape = { ContinuousCapsule },
                            effects = {
                                val pressProgress = dampedDragAnimation.pressProgress
                                blur(8.dp.toPx() * (1f - pressProgress))
                                lens(
                                    refractionHeight = 5.dp.toPx() * pressProgress,
                                    refractionAmount = 10.dp.toPx() * pressProgress,
                                    chromaticAberration = true
                                )
                            },
                            highlight = {
                                val pressProgress = dampedDragAnimation.pressProgress
                                Highlight.Ambient.copy(
                                    width = Highlight.Ambient.width / 1.5f,
                                    blurRadius = Highlight.Ambient.blurRadius / 1.5f,
                                    alpha = pressProgress
                                )
                            },
                            shadow = {
                                Shadow(
                                    radius = 4.dp,
                                    color = Color.Black.copy(alpha = 0.05f)
                                )
                            },
                            innerShadow = {
                                val pressProgress = dampedDragAnimation.pressProgress
                                InnerShadow(
                                    radius = 4.dp * pressProgress,
                                    alpha = pressProgress
                                )
                            },
                            layerBlock = {
                                scaleX = dampedDragAnimation.scaleX
                                scaleY = dampedDragAnimation.scaleY
                                val velocity = dampedDragAnimation.velocity / 50f
                                scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                                scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                            },
                            onDrawSurface = {
                                val pressProgress = dampedDragAnimation.pressProgress
                                drawRect(Color.White.copy(alpha = 1f - pressProgress))
                            }
                        )
                    } else {
                        Modifier
                            .clip(ContinuousCapsule)
                            .background(Color.White)
                    }
                )
                .size(40.dp, 24.dp)
        )
    }
}

@Composable
private fun StorageUsagePanel(
    stats: WebStorageStats,
    theme: FlightThemeSpec,
    tablet: Boolean,
    backdrop: Backdrop?,
    onClear: () -> Unit
) {
    val total = stats.total
    val safeTotal = if (total <= 0f) 1f else total
    val httpRatio = (stats.httpCache / safeTotal).coerceAtLeast(0.015f)
    val cookieRatio = (stats.cookies / safeTotal).coerceAtLeast(0.015f)
    val dbRatio = (stats.webDb / safeTotal).coerceAtLeast(0.015f)

    if (tablet) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StorageTotal(stats, theme, Modifier.width(170.dp))
            StorageBarAndLegend(
                httpRatio = httpRatio,
                cookieRatio = cookieRatio,
                dbRatio = dbRatio,
                stats = stats,
                theme = theme,
                modifier = Modifier.weight(1f)
            )
            ClearStorageButton(backdrop = backdrop, theme = theme, onClear = onClear, modifier = Modifier.width(190.dp))
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            StorageTotal(stats, theme, Modifier.fillMaxWidth())
            StorageBarAndLegend(
                httpRatio = httpRatio,
                cookieRatio = cookieRatio,
                dbRatio = dbRatio,
                stats = stats,
                theme = theme,
                modifier = Modifier.fillMaxWidth()
            )
            ClearStorageButton(backdrop = backdrop, theme = theme, onClear = onClear, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun StorageTotal(stats: WebStorageStats, theme: FlightThemeSpec, modifier: Modifier) {
    Column(modifier = modifier) {
        Text(
            text = String.format(LocalLocale.current.platformLocale, "%.1f MB used", stats.total),
            color = theme.text,
            fontWeight = FontWeight.Black,
            fontSize = 24.sp
        )
        Text("of ~100 MB available", color = theme.muted, fontSize = 12.sp)
    }
}

@Composable
private fun StorageBarAndLegend(
    httpRatio: Float,
    cookieRatio: Float,
    dbRatio: Float,
    stats: WebStorageStats,
    theme: FlightThemeSpec,
    modifier: Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(theme.muted.copy(alpha = 0.15f))
        ) {
            Box(Modifier.fillMaxHeight().weight(httpRatio).background(Color(0xFFFF4BA6)))
            Box(Modifier.fillMaxHeight().weight(cookieRatio).background(Color(0xFF7C5CFF)))
            Box(Modifier.fillMaxHeight().weight(dbRatio).background(Color(0xFF35BF66)))
            Box(Modifier.fillMaxHeight().weight(1f).background(Color.Transparent))
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StorageLegend("HTTP cache", stats.httpCache, Color(0xFFFF4BA6), theme, Modifier.weight(1f))
            StorageLegend("Local storage", stats.cookies, Color(0xFF7C5CFF), theme, Modifier.weight(1f))
            StorageLegend("Other data", stats.webDb, Color(0xFF35BF66), theme, Modifier.weight(1f))
        }
    }
}

@Composable
private fun StorageLegend(name: String, value: Float, color: Color, theme: FlightThemeSpec, modifier: Modifier) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(6.dp))
            Text(name, color = theme.text, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            String.format(LocalLocale.current.platformLocale, "%.1f MB", value),
            color = theme.muted,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun ClearStorageButton(
    backdrop: Backdrop?,
    theme: FlightThemeSpec,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val danger = Color(0xFFE23C4A)
    Row(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(danger.copy(alpha = 0.08f), RoundedCornerShape(13.dp))
            .border(1.dp, danger.copy(alpha = 0.22f), RoundedCornerShape(13.dp))
            .clickable(onClick = onClear),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Delete, contentDescription = null, tint = danger, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Clear storage", color = danger, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun SaveConfirmation(
    visible: Boolean,
    theme: FlightThemeSpec
) {
    AnimatedVisibility(visible) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(theme.card.copy(alpha = 0.66f))
                .border(1.dp, theme.border, RoundedCornerShape(18.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(theme.accent.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Security, contentDescription = null, tint = theme.accent, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(
                "Your settings are saved automatically and applied instantly.",
                color = theme.muted,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFE1F8E8)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF1B9A44), modifier = Modifier.size(22.dp))
            }
        }
    }
}


@Composable
fun StorageRow(
    name: String,
    value: Float,
    textColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            modifier = Modifier.weight(1f),
            color = textColor
        )
        Text(
            text = String.format(
                LocalLocale.current.platformLocale,
                "%.1f MB",
                value
            ),
            color = textColor.copy(alpha = 0.7f)
        )
    }
}
