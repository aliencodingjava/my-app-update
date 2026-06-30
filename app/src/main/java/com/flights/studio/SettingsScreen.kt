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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.compositeOver
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
import androidx.compose.ui.text.style.TextAlign
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

private data class PreviewNativeFlightPalette(
    val page: Color,
    val panel: Color,
    val surface: Color,
    val datePill: Color,
    val headerPill: Color,
    val arrivedSurface: Color,
    val departedSurface: Color,
    val delayedSurface: Color,
    val cancelledSurface: Color,
    val rowBorder: Color,
    val accent: Color,
    val arrivedAccent: Color,
    val departedAccent: Color,
    val delayAccent: Color,
    val cancelledAccent: Color,
    val text: Color,
    val muted: Color
)

private val LightFlightTheme = FlightThemeSpec(
    id = "light",
    title = "Light",
    previewName = "Clear Apron",
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
    previewName = "Teton Mint",
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
    previewName = "Runway Sky",
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
    previewName = "Night Ocean",
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
    previewName = "Violet Gate",
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
    previewName = "Rose Quartz",
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
    previewName = "Amber Ramp",
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
    previewName = "Graphite",
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
    previewName = "Night Ops",
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
    previewName = "Auto Flight"
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
                    title = "Themed Appearance",
                    subtitle = "Light, Mint, Sky, Ocean, Violet, Rose, Amber, Gray, Dark, Auto System.",
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
                                groupFlights = false
                                highContrastWeb = false
                                hwAccel = true
                                blockTrackers = true
                                cachePages = true
                                reduceWebMotion = false
                                SettingsStore.setWebTheme(context, selectedTheme)
                                SettingsStore.setTextZoom(context, textZoom)
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

                    Spacer(Modifier.height(10.dp))

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

                    Spacer(Modifier.height(10.dp))

                    FlightRowsPreview(
                        theme = resolvedTheme,
                        groupedFlights = groupFlights,
                        textScale = textZoom,
                        highContrast = highContrastWeb
                    )
                }

                Spacer(Modifier.height(8.dp))

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
                            title = "Grouped flights table beta",
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
                        if (!tablet) Spacer(Modifier.height(6.dp))
                        SettingToggleCard(
                            title = "High contrast table beta",
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
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.weight(1f)) {
                                    SettingToggleCard(
                                        title = "Grouped flights table beta",
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
                                Box(Modifier.weight(1f)) {
                                    SettingToggleCard(
                                        title = "High contrast table beta",
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
                            }
                        }
                    } else {
                        Column { tableItems() }
                    }
                }

                Spacer(Modifier.height(8.dp))

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
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            performanceCards.chunked(2).forEach { rowItems ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    rowItems.forEach { item ->
                                        Box(Modifier.weight(1f)) { item() }
                                    }
                                }
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            performanceCards.forEach { it() }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

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

                Spacer(Modifier.height(8.dp))

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
        "auto" -> if (systemDark) DarkFlightTheme.copy(id = "auto", title = "Auto", previewName = "Auto Flight") else AutoFlightTheme
        else -> MintFlightTheme
    }
}

private fun previewFlightEffectiveTheme(webTheme: String, isDark: Boolean): String {
    return when (webTheme.lowercase()) {
        "auto" -> if (isDark) "dark" else "light"
        "dark", "ocean", "mint", "sky", "violet", "rose", "amber", "gray", "light" -> webTheme.lowercase()
        else -> if (isDark) "dark" else "light"
    }
}

private fun previewFlightThemeAccent(effectiveTheme: String): Color {
    return when (effectiveTheme) {
        "mint" -> Color(0xFF22B981)
        "sky" -> Color(0xFF3B82F6)
        "ocean" -> Color(0xFF7DD3FC)
        "violet" -> Color(0xFF8B5CF6)
        "rose" -> Color(0xFFEC4899)
        "amber" -> Color(0xFFF59E0B)
        "gray" -> Color(0xFF64748B)
        "dark" -> Color(0xFF7DD3FC)
        else -> Color(0xFF5AC8FA)
    }
}

private fun previewNativeFlightPalette(
    webTheme: String,
    isDark: Boolean,
    glassAmount: Float,
    highContrast: Boolean
): PreviewNativeFlightPalette {
    if (highContrast) {
        return if (isDark) {
            PreviewNativeFlightPalette(
                page = Color.Black,
                panel = Color.Black.copy(alpha = 0.82f),
                surface = Color.Black.copy(alpha = 0.72f),
                datePill = Color.Black,
                headerPill = Color(0xFF181818),
                arrivedSurface = Color.Black.copy(alpha = 0.72f),
                departedSurface = Color.Black.copy(alpha = 0.72f),
                delayedSurface = Color.Black.copy(alpha = 0.72f),
                cancelledSurface = Color.Black.copy(alpha = 0.72f),
                rowBorder = Color.White.copy(alpha = 0.36f),
                accent = Color.White,
                arrivedAccent = Color.White,
                departedAccent = Color.White,
                delayAccent = Color.White,
                cancelledAccent = Color.White,
                text = Color.White,
                muted = Color.White.copy(alpha = 0.70f)
            )
        } else {
            PreviewNativeFlightPalette(
                page = Color.White,
                panel = Color.White.copy(alpha = 0.92f),
                surface = Color.White.copy(alpha = 0.94f),
                datePill = Color.White.copy(alpha = 0.92f),
                headerPill = Color.Black.copy(alpha = 0.08f),
                arrivedSurface = Color.White.copy(alpha = 0.94f),
                departedSurface = Color.White.copy(alpha = 0.94f),
                delayedSurface = Color.White.copy(alpha = 0.94f),
                cancelledSurface = Color.White.copy(alpha = 0.94f),
                rowBorder = Color.Black.copy(alpha = 0.36f),
                accent = Color.Black,
                arrivedAccent = Color.Black,
                departedAccent = Color.Black,
                delayAccent = Color.Black,
                cancelledAccent = Color.Black,
                text = Color.Black,
                muted = Color.Black.copy(alpha = 0.64f)
            )
        }
    }

    val effectiveTheme = previewFlightEffectiveTheme(webTheme, isDark)
    val accent = previewFlightThemeAccent(effectiveTheme)
    return if (effectiveTheme == "dark" || effectiveTheme == "ocean") {
        val surface = accent.copy(alpha = 0.08f).compositeOver(Color(0xFF101B27).copy(alpha = 0.66f))
        val panel = accent.copy(alpha = 0.025f + 0.05f * glassAmount)
            .compositeOver(Color(0xFF10151D).copy(alpha = 0.58f + 0.32f * glassAmount))
        PreviewNativeFlightPalette(
            page = if (effectiveTheme == "ocean") Color(0xFF071820) else Color(0xFF07111C),
            panel = panel,
            surface = surface,
            datePill = Color(0xFF111111).copy(alpha = 0.88f),
            headerPill = Color.White.copy(alpha = 0.105f),
            arrivedSurface = accent.copy(alpha = 0.18f).compositeOver(surface),
            departedSurface = Color(0xFF38E8C8).copy(alpha = 0.16f).compositeOver(surface),
            delayedSurface = Color(0xFFFFB020).copy(alpha = 0.18f).compositeOver(surface),
            cancelledSurface = Color(0xFFFF453A).copy(alpha = 0.18f).compositeOver(surface),
            rowBorder = accent.copy(alpha = 0.28f),
            accent = accent,
            arrivedAccent = accent,
            departedAccent = if (effectiveTheme == "ocean") Color(0xFF38E8C8) else Color(0xFF34C759),
            delayAccent = Color(0xFFFFB020),
            cancelledAccent = Color(0xFFFF453A),
            text = Color.White.copy(alpha = 0.94f),
            muted = Color.White.copy(alpha = 0.62f)
        )
    } else {
        val page = when (effectiveTheme) {
            "mint" -> Color(0xFFF2FBF8)
            "sky" -> Color(0xFFF0F7FF)
            "violet" -> Color(0xFFF7F3FF)
            "rose" -> Color(0xFFFFF5FA)
            "amber" -> Color(0xFFFFFAEC)
            "gray" -> Color(0xFFF4F6F8)
            else -> Color(0xFFF8FAFF)
        }
        val panel = accent.copy(alpha = 0.025f + 0.035f * glassAmount)
            .compositeOver(page.copy(alpha = 0.66f + 0.26f * glassAmount))
        val surface = accent.copy(alpha = 0.035f + 0.045f * glassAmount)
            .compositeOver(Color.White.copy(alpha = 0.56f + 0.22f * glassAmount))
        val text = when (effectiveTheme) {
            "mint" -> Color(0xFF10201C)
            "sky" -> Color(0xFF10243F)
            "violet" -> Color(0xFF261B3F)
            "rose" -> Color(0xFF3E122A)
            "amber" -> Color(0xFF34230C)
            "gray" -> Color(0xFF1F2937)
            else -> Color(0xFF1E1F24)
        }
        PreviewNativeFlightPalette(
            page = page,
            panel = panel,
            surface = surface,
            datePill = Color.White.copy(alpha = 0.78f),
            headerPill = accent.copy(alpha = 0.12f).compositeOver(Color.Black.copy(alpha = 0.055f)),
            arrivedSurface = accent.copy(alpha = 0.18f).compositeOver(surface),
            departedSurface = accent.copy(alpha = 0.13f).compositeOver(surface),
            delayedSurface = Color(0xFFF59E0B).copy(alpha = 0.18f).compositeOver(surface),
            cancelledSurface = Color(0xFFFF453A).copy(alpha = 0.14f).compositeOver(surface),
            rowBorder = accent.copy(alpha = 0.34f),
            accent = accent,
            arrivedAccent = accent,
            departedAccent = accent.copy(alpha = 0.92f),
            delayAccent = Color(0xFFB7791F),
            cancelledAccent = Color(0xFFD93025),
            text = text,
            muted = text.copy(alpha = 0.66f)
        )
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
            shadow = null,
            highlight = null,
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
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(theme.card.copy(alpha = 0.66f), shape)
            .border(1.dp, theme.border.copy(alpha = 0.34f), shape)
            .padding(horizontal = 12.dp, vertical = 10.dp)
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
                Text("Text size", color = theme.text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("Flight table scale", color = theme.muted, fontSize = 11.sp, lineHeight = 14.sp)
            }

            AnimatedContent(value, label = "textZoomValue") { current ->
                Text(
                    text = "$current%",
                    color = theme.accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("60", color = mutedColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Slider(
                value = value.toFloat(),
                onValueChange = { onValueChange(it.roundToInt().coerceIn(60, 100)) },
                valueRange = 60f..100f,
                steps = 39,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )
            Column(horizontalAlignment = Alignment.End) {
                Text("100", color = mutedColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("Max", color = mutedColor.copy(alpha = 0.72f), fontSize = 9.sp)
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
    val cardShape = RoundedCornerShape(cornerRadius ?: 20.dp)
    val contentPadding = if (tablet) 14.dp else 12.dp
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
            .padding(horizontal = if (tablet) 0.dp else 8.dp)
            .clip(cardShape)
            .background(surfaceColor, cardShape)
            .border(1.dp, borderColor, cardShape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = contentPadding, top = 8.dp, end = contentPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(theme.accent.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = theme.accent, modifier = Modifier.size(16.dp))
            }

            Spacer(Modifier.width(9.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    color = theme.text,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    lineHeight = 17.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (subtitle != null) {
                    Spacer(Modifier.height(1.dp))
                    Text(
                        subtitle,
                        color = theme.muted,
                        fontSize = 11.sp,
                        lineHeight = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (trailing != null) {
                Spacer(Modifier.width(10.dp))
                trailing()
            }
        }

        Spacer(Modifier.height(8.dp))
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
    val glassAmount = rememberLiquidGlassTintAmount()
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
                systemDark = systemDark,
                glassAmount = glassAmount,
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
    systemDark: Boolean,
    glassAmount: Float,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val effectiveTheme = previewFlightEffectiveTheme(theme.id, systemDark)
    val previewDark = effectiveTheme == "dark" || effectiveTheme == "ocean"
    val nativePalette = remember(theme.id, systemDark, glassAmount) {
        previewNativeFlightPalette(
            webTheme = theme.id,
            isDark = systemDark,
            glassAmount = glassAmount,
            highContrast = false
        )
    }
    val borderColor by animateColorAsState(
        if (selected) nativePalette.accent else nativePalette.muted.copy(alpha = 0.34f),
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
            .background(nativePalette.panel)
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
                .background(nativePalette.page)
                .border(1.dp, nativePalette.muted.copy(alpha = 0.22f), RoundedCornerShape(9.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(5.dp)).background(nativePalette.datePill))
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(5.dp))
                    .background(nativePalette.arrivedSurface)
                    .border(1.dp, nativePalette.rowBorder, RoundedCornerShape(5.dp))
            )
            Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(5.dp)).background(nativePalette.delayAccent.copy(alpha = 0.28f)))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (selected) "${theme.previewName} (Current)" else theme.previewName,
            color = if (selected) nativePalette.accent else nativePalette.text,
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
                .background(if (selected) nativePalette.accent else Color.Transparent)
                .border(1.5.dp, if (selected) nativePalette.accent else nativePalette.muted.copy(alpha = 0.45f), CircleShape),
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
    highContrast: Boolean
) {
    val isDark = isSystemInDarkTheme()
    val glassAmount = rememberLiquidGlassTintAmount()
    val nativePalette = remember(theme.id, isDark, glassAmount, highContrast) {
        previewNativeFlightPalette(
            webTheme = theme.id,
            isDark = isDark,
            glassAmount = glassAmount,
            highContrast = highContrast
        )
    }
    val previewShape = RoundedCornerShape(14.dp)
    val previewPanel = flightLanternSheetPanelColor(FlightArrivalLantern, isDark, glassAmount)
    val previewOverlay = flightLanternSheetOverlayColor(FlightArrivalLantern, isDark, glassAmount)
    val previewSheen = flightLanternSheetSheenBrush(FlightArrivalLantern, isDark, glassAmount)
    val previewHeight by animateDpAsState(
        targetValue = flightPreviewHeight(textScale),
        animationSpec = tween(180),
        label = "flightPreviewHeight"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(previewHeight)
            .clip(previewShape)
            .background(previewPanel)
            .background(previewOverlay)
            .background(previewSheen)
            .border(1.dp, nativePalette.rowBorder.copy(alpha = 0.42f), previewShape)
            .padding(start = 8.dp, end = 8.dp, top = 10.dp, bottom = 10.dp)
    ) {
        MiniFlightTable(
            palette = nativePalette,
            textScale = textScale,
            highContrast = highContrast,
            isDark = isDark,
            compact = false
        )
    }
}

private fun flightPreviewHeight(textScale: Int): Dp {
    return 292.dp
}

@Composable
private fun MiniFlightTable(
    palette: PreviewNativeFlightPalette,
    textScale: Int,
    highContrast: Boolean,
    isDark: Boolean,
    compact: Boolean
) {
    val scale = previewFlightTextScale(textScale)
    val rowHeight = 58.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FlightPreviewRow(
            airline = "American",
            flight = "1883",
            place = "Dallas/Fort Worth",
            sched = "3:08pm",
            actual = "6:45pm",
            status = "Arrived",
            rowBg = palette.arrivedSurface,
            rowBorder = palette.rowBorder,
            statusAccent = palette.arrivedAccent,
            textColor = palette.text,
            mutedColor = palette.muted,
            textScale = scale,
            height = rowHeight
        )
        FlightPreviewRow(
            airline = "United",
            flight = "1537",
            place = "Denver",
            sched = "3:25pm",
            actual = "3:15pm",
            status = "Arrived",
            rowBg = palette.arrivedSurface,
            rowBorder = palette.rowBorder,
            statusAccent = palette.arrivedAccent,
            textColor = palette.text,
            mutedColor = palette.muted,
            textScale = scale,
            height = rowHeight
        )
        FlightPreviewRow(
            airline = "Delta",
            flight = "417",
            place = "Salt Lake City",
            sched = "5:00pm",
            actual = "5:27pm",
            status = "Arrived",
            rowBg = palette.arrivedSurface,
            rowBorder = palette.rowBorder,
            statusAccent = palette.arrivedAccent,
            textColor = palette.text,
            mutedColor = palette.muted,
            textScale = scale,
            height = rowHeight
        )
        FlightPreviewRow(
            airline = "Alaska",
            flight = "3475",
            place = "San Diego",
            sched = "3:07pm",
            actual = "2:55pm",
            status = "Arrived",
            rowBg = palette.arrivedSurface,
            rowBorder = palette.rowBorder,
            statusAccent = palette.arrivedAccent,
            textColor = palette.text,
            mutedColor = palette.muted,
            textScale = scale,
            height = rowHeight
        )
    }
}

private fun previewFlightTextScale(textZoom: Int): Float {
    return (textZoom.coerceIn(60, 100) / 100f).coerceIn(0.60f, 1f)
}

private fun previewFlightScaledSp(minSp: Float, maxSp: Float, textScale: Float): androidx.compose.ui.unit.TextUnit {
    return (minSp + ((maxSp - minSp) * textScale.coerceIn(0.60f, 1f))).sp
}

private fun previewFlightArrivedSurface(
    isDark: Boolean,
    highContrast: Boolean,
    fallback: Color
): Color {
    return if (highContrast) {
        fallback
    } else if (isDark) {
        Color(0xFF073D2E).copy(alpha = 0.62f)
    } else {
        Color(0xFFE8D6C6).copy(alpha = 0.96f)
    }
}

private fun previewFlightArrivedBorder(
    isDark: Boolean,
    highContrast: Boolean,
    textColor: Color
): Color {
    return if (highContrast) {
        textColor.copy(alpha = 0.36f)
    } else if (isDark) {
        Color(0xFF34C759).copy(alpha = 0.28f)
    } else {
        Color(0xFF8A5A3C).copy(alpha = 0.32f)
    }
}

private const val PreviewFlightAirlineStart = 0.00f
private const val PreviewFlightFlightStart = 0.245f
private const val PreviewFlightPlaceStart = 0.360f
private const val PreviewFlightSchedStart = 0.590f
private const val PreviewFlightActualStart = 0.725f
private const val PreviewFlightStatusStart = 0.845f
private const val PreviewFlightEnd = 1.00f

@Composable
private fun PreviewPositionedColumn(
    width: Dp,
    start: Float,
    end: Float,
    contentAlignment: Alignment = Alignment.CenterStart,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .offset(x = width * start)
            .width(width * (end - start).coerceAtLeast(0.01f))
            .fillMaxHeight(),
        contentAlignment = contentAlignment
    ) {
        content()
    }
}

@Composable
private fun FlightPreviewRow(
    airline: String,
    flight: String,
    place: String,
    sched: String,
    actual: String,
    status: String,
    rowBg: Color,
    rowBorder: Color,
    statusAccent: Color,
    textColor: Color,
    mutedColor: Color,
    textScale: Float,
    height: Dp
) {
    val rowShape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(rowShape)
            .background(rowBg)
            .border(1.dp, rowBorder, rowShape)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PreviewAirlineBadge(airline, textScale, large = true)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "$airline $flight",
                    color = textColor,
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = previewFlightScaledSp(10.8f, 12f, textScale),
                        lineHeight = previewFlightScaledSp(12f, 13.4f, textScale)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                StatusCell(status, textScale, accent = statusAccent)
            }
            Spacer(Modifier.height(3.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = place,
                    color = textColor.copy(alpha = 0.88f),
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = previewFlightScaledSp(9.2f, 10.4f, textScale),
                        lineHeight = previewFlightScaledSp(10.4f, 11.7f, textScale)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Sched $sched",
                    color = mutedColor.copy(alpha = 0.88f),
                    fontSize = previewFlightScaledSp(8.4f, 9.4f, textScale),
                    lineHeight = previewFlightScaledSp(9.5f, 10.6f, textScale),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Actual $actual",
                    color = mutedColor.copy(alpha = 0.88f),
                    fontSize = previewFlightScaledSp(8.4f, 9.4f, textScale),
                    lineHeight = previewFlightScaledSp(9.5f, 10.6f, textScale),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun PreviewAirlineCell(
    airline: String,
    textColor: Color,
    textScale: Float
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        PreviewAirlineBadge(airline, textScale)
        Text(
            airline,
            color = textColor,
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Medium,
                fontSize = previewFlightScaledSp(10.8f, 12f, textScale),
                lineHeight = previewFlightScaledSp(12.2f, 13.4f, textScale)
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PreviewAirlineBadge(
    airline: String,
    textScale: Float,
    large: Boolean = false
) {
    val normalized = airline.lowercase()
    val (code, color) = when {
        normalized.contains("american") || normalized == "aa" -> "AA" to Color(0xFF2563EB)
        normalized.contains("delta") || normalized == "dl" -> "DL" to Color(0xFFDC2626)
        normalized.contains("united") || normalized == "ua" -> "UA" to Color(0xFF0EA5E9)
        normalized.contains("alaska") || normalized == "as" -> "AS" to Color(0xFF0F766E)
        else -> "--" to Color(0xFF64748B)
    }
    Box(
        modifier = Modifier
            .size(if (large) 38.dp else 20.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.90f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = code,
            color = Color.White,
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Black,
                fontSize = if (large) 10.5.sp else previewFlightScaledSp(7.4f, 8.4f, textScale),
                lineHeight = if (large) 11.5.sp else previewFlightScaledSp(8.4f, 9.4f, textScale),
                textAlign = TextAlign.Center
            ),
            maxLines = 1
        )
    }
}

@Composable
private fun FlightCell(
    value: String,
    textColor: Color,
    textScale: Float
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            value,
            color = textColor,
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Medium,
                fontSize = previewFlightScaledSp(10.8f, 12f, textScale),
                lineHeight = previewFlightScaledSp(12.2f, 13.4f, textScale)
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}

@Composable
private fun StatusCell(
    value: String,
    textScale: Float,
    accent: Color
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterEnd
    ) {
        Text(
            value,
            color = accent,
            fontSize = 10.sp,
            lineHeight = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(accent.copy(alpha = 0.18f))
                .padding(horizontal = 6.dp, vertical = 5.dp)
        )
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
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable { onChange(!checked) }
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(theme.accent.copy(alpha = 0.13f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = theme.accent, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(9.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = theme.text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 16.sp)
            Text(
                subtitle,
                color = theme.muted,
                fontSize = 11.sp,
                lineHeight = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange
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
