package com.flights.studio

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.core.view.WindowCompat
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.ui.util.lerp

class LiquidGlassSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val darkTheme = isSystemInDarkTheme()

            SideEffect {
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }

            FlightsTheme(profileBackdropStyle = ProfileBackdropStyle.Auto) {
                LiquidGlassSettingsScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LiquidGlassSettingsScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current.applicationContext
    val pageBackdrop = rememberLayerBackdrop()
    var glassTint by remember(context) {
        mutableStateOf(SettingsStore.liquidGlassTint(context))
    }
    var adaptiveEnabled by remember(context) {
        mutableStateOf(SettingsStore.liquidGlassAdaptiveLuminance(context))
    }
    val previewAdaptive = rememberAdaptiveLuminance(
        enabled = adaptiveEnabled,
        lightOnBright = Color(0xFF101318),
        lightOnDark = Color.White
    )

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = {
                    Text(
                        text = stringResource(R.string.liquid_glass_preview_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBackIos,
                            contentDescription = null
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LocalAppPageBg.current)
                    .layerBackdrop(pageBackdrop)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 18.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                LiquidGlassPreviewCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(390.dp),
                    adaptive = previewAdaptive,
                    glassTint = glassTint,
                    adaptiveEnabled = adaptiveEnabled
                )

                LiquidGlassSliderPanel(
                    value = glassTint,
                    adaptive = previewAdaptive,
                    adaptiveEnabled = adaptiveEnabled,
                    onValueChange = { value ->
                        glassTint = value.coerceIn(0f, 1f)
                        SettingsStore.setLiquidGlassTint(context, glassTint)
                    },
                    onAdaptiveEnabledChange = { enabled ->
                        adaptiveEnabled = enabled
                        SettingsStore.setLiquidGlassAdaptiveLuminance(context, enabled)
                    }
                )
            }
        }
    }
}

@Composable
private fun LiquidGlassPreviewCard(
    modifier: Modifier = Modifier,
    adaptive: AdaptiveLuminanceState,
    glassTint: Float,
    adaptiveEnabled: Boolean
) {
    val isDark = isSystemInDarkTheme()
    val previewBackdrop = rememberLayerBackdrop()
    var selectedTab by remember { mutableIntStateOf(0) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(32.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(previewBackdrop)
        ) {
            TetonMountainPreviewPager(Modifier.fillMaxSize())
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (isDark) {
                            Color.Black.copy(alpha = 0.12f)
                        } else {
                            Color.White.copy(alpha = 0.03f)
                        }
                    )
            )
        }

        LiquidGlassPreviewTabs(
            backdrop = previewBackdrop,
            adaptive = adaptive,
            glassTint = glassTint,
            adaptiveEnabled = adaptiveEnabled,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(22.dp)
                .fillMaxWidth()
                .height(58.dp),
            selectedIndex = selectedTab,
            onSelect = { selectedTab = it }
        )
    }
}

@Composable
private fun TetonMountainPreviewPager(modifier: Modifier = Modifier) {
    val pages = remember {
        listOf(
            LiquidGlassPreviewPage(
                assetName = "tetonmountain.jpg",
                title = "Arrive In The Tetons",
                body = "Jackson Hole Airport sits inside Grand Teton National Park, so the first thing travelers feel is the valley itself: open sky, mountain light, and a runway framed by the Teton Range. This preview keeps real scenery and real text under the tabs, so you can see whether the glass stays readable when the page is bright, detailed, and moving."
            ),
            LiquidGlassPreviewPage(
                assetName = "tetonmountain2.jpg",
                title = "Only One Like It",
                body = "JAC is known as the only commercial airport within a U.S. national park. That makes the terminal feel quieter and closer to nature than a typical city airport. Slide the glass while this card sits behind the tabs and tune how much contrast you want for icons, labels, photos, and paragraphs in the same view."
            ),
            LiquidGlassPreviewPage(
                assetName = "tetonmountain3.jpg",
                title = "Clear Or Tinted",
                body = "Use this sample to tune the bottom tabs. Clear glass lets the scenery pass through; tinted glass adds contrast when photos, cards, and text move behind the bar. The goal is simple: keep the app beautiful without losing the icons. If the text still feels clear under the tab bar, the setting is doing its job."
            ),
            LiquidGlassPreviewPage(
                assetName = "tetonmountain.jpg",
                title = "A Small Airport With A Big View",
                body = "Commercial service began here in 1946, before the park grew around the airfield. Today the airport stays compact, practical, and strongly tied to the landscape around it. The glass preview should feel the same: clean, useful, and calm. It should look light over a mountain photo, but still carry enough surface to read at a glance."
            ),
            LiquidGlassPreviewPage(
                assetName = "tetonmountain2.jpg",
                title = "Built For Real Days",
                body = "The glass should work when the page is calm and when it is busy: flight checks, notes, family trips, ski days, summer hikes, and quick settings changes on the move. Text behind the tab bar helps show the real clear-to-tinted difference, especially when the app has cards, images, and controls stacked close together."
            )
        )
    }
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { pages.size })

    Box(modifier = modifier) {
        androidx.compose.foundation.pager.HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            LiquidGlassPreviewPageContent(
                page = pages[page],
                modifier = Modifier.fillMaxSize()
            )
        }

        LiquidGlassPagerDots(
            count = pages.size,
            current = pagerState.currentPage,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp)
        )
    }
}

private data class LiquidGlassPreviewPage(
    val assetName: String,
    val title: String,
    val body: String
)

@Composable
private fun LiquidGlassPreviewPageContent(
    page: LiquidGlassPreviewPage,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomCenter
    ) {
        TetonMountainPreviewImage(
            assetName = page.assetName,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (isDark) {
                        Color.Black.copy(alpha = 0.18f)
                    } else {
                        Color.White.copy(alpha = 0.05f)
                    }
                )
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(176.dp)
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            shape = RoundedCornerShape(24.dp),
            color = if (isDark) Color(0xFF16191F).copy(alpha = 0.78f) else Color.White.copy(alpha = 0.82f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            border = BorderStroke(
                1.dp,
                if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.06f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 18.dp, top = 12.dp, end = 18.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp, Alignment.Bottom)
            ) {
                Text(
                    text = page.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = page.body,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LiquidGlassPagerDots(
    count: Int,
    current: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = if (isSystemInDarkTheme()) 0.56f else 0.72f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(count) { index ->
                Box(
                    modifier = Modifier
                        .size(if (index == current) 7.dp else 5.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == current) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            }
                        )
                )
            }
        }
    }
}

@Composable
private fun TetonMountainPreviewImage(
    assetName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bitmap = remember(context, assetName) {
        runCatching {
            context.assets.open(assetName).use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        }.getOrNull()
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        ProfileBackdropImageLayer(
            modifier = modifier,
            lightRes = R.drawable.light_grid_pattern,
            darkRes = R.drawable.dark_grid_pattern,
            imageAlpha = 0.92f,
            scrimDark = 0.04f,
            scrimLight = 0f
        )
    }
}

@Composable
private fun LiquidGlassPreviewTabs(
    backdrop: Backdrop,
    adaptive: AdaptiveLuminanceState,
    glassTint: Float,
    adaptiveEnabled: Boolean,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = isSystemInDarkTheme()
    val tintAmount = glassTint.coerceIn(0f, 1f)
    val primary = primaryTabAccentColor()
    val glassColor = bottomTabBarTintForAmount(tintAmount, isDarkTheme)
    val overlayTint = bottomTabBarOverlayTintForAmount(tintAmount, isDarkTheme)
    val backdropBlurDp = bottomChromeBackdropBlurDpForAmount(tintAmount, isDarkTheme)
    val adaptiveOffset = adaptiveLuminanceOffset(adaptive.luminance)
    val adaptiveEffectStrength = if (adaptiveEnabled) {
        adaptiveLuminanceEffectStrength(tintAmount)
    } else {
        0f
    }
    val adaptiveSurfaceStrength = if (adaptiveEnabled) {
        lerp(0.45f, 1f, adaptiveEffectStrength)
    } else {
        0f
    }
    val adaptiveSurfaceTint = adaptiveSurfaceTint(
        luminance = adaptive.luminance,
        strength = adaptiveSurfaceStrength
    )
    val adaptiveContentBlend = if (adaptiveEnabled) {
        lerp(0.18f, if (isDarkTheme) 0.56f else 0.68f, adaptiveEffectStrength)
    } else {
        0f
    }
    val selectedContentColor = lerpColor(
        bottomTabSelectedContentColorForAmount(tintAmount, isDarkTheme, primary),
        adaptive.contentColor,
        adaptiveContentBlend
    )
    val inactiveContentColor = lerpColor(
        bottomTabInactiveColorForAmount(tintAmount, isDarkTheme),
        adaptive.contentColor,
        adaptiveContentBlend
    )
    val selectedPillColor = bottomTabSelectedPillColorForAmount(tintAmount, isDarkTheme, primary)
    var pulse by remember { mutableIntStateOf(0) }
    var barPressed by remember { mutableStateOf(false) }
    val barScale by animateFloatAsState(
        targetValue = if (barPressed) 0.982f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "liquidGlassPreviewBarScale"
    )

    LaunchedEffect(pulse) {
        if (pulse == 0) return@LaunchedEffect
        barPressed = true
        delay(110)
        barPressed = false
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = barScale
                scaleY = barScale
            }
            .shadow(GlassChromeShadowElevation, GlassChromeShape, clip = false)
            .clip(GlassChromeShape)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { GlassChromeShape },
                shadow = { bottomChromeShadow() },
                highlight = null,
                effects = {
                    if (adaptiveEffectStrength > 0.001f) {
                        val adaptiveBrightness = adaptiveLuminanceBrightness(adaptiveOffset)
                        val adaptiveContrast = adaptiveLuminanceContrast(adaptiveOffset)
                        colorControls(
                            brightness = adaptiveBrightness * adaptiveEffectStrength,
                            contrast = lerp(1f, adaptiveContrast, adaptiveEffectStrength),
                            saturation = lerp(1f, 1.5f, adaptiveEffectStrength)
                        )
                    }
                    vibrancy()
                    blur(
                        radius = if (adaptiveEffectStrength > 0.001f) {
                            val baseBlurPx = backdropBlurDp.dp.toPx()
                            val adaptiveBlurPx = adaptiveLuminanceBlurPx(
                                offset = adaptiveOffset,
                                baseBlurPx = baseBlurPx,
                                dpToPx = { it.dp.toPx() }
                            )
                            lerp(baseBlurPx, adaptiveBlurPx, adaptiveEffectStrength)
                        } else {
                            backdropBlurDp.dp.toPx()
                        },
                        edgeTreatment = TileMode.Mirror
                    )
                    lens(
                        refractionHeight = GlassChromeRefractionHeightDp.dp.toPx(),
                        refractionAmount = GlassChromeRefractionAmountDp.dp.toPx(),
                        depthEffect = false,
                        chromaticAberration = false
                    )
                },
                onDrawBackdrop = { drawBackdrop ->
                    drawBackdrop()
                    adaptive.layer.record {
                        drawBackdrop()
                    }
                },
                onDrawSurface = {
                    drawRect(glassColor)
                    if (adaptiveSurfaceStrength > 0f) {
                        drawRect(adaptiveSurfaceTint)
                    }
                }
            )
            .background(overlayTint, GlassChromeShape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LiquidGlassPreviewTab("Home", Icons.Filled.Home, selectedIndex == 0, selectedPillColor, selectedContentColor, inactiveContentColor) {
                pulse++
                onSelect(0)
            }
            LiquidGlassPreviewTab("Brief", Icons.Filled.Info, selectedIndex == 1, selectedPillColor, selectedContentColor, inactiveContentColor) {
                pulse++
                onSelect(1)
            }
            LiquidGlassPreviewTab("Notes", Icons.AutoMirrored.Filled.Article, selectedIndex == 2, selectedPillColor, selectedContentColor, inactiveContentColor) {
                pulse++
                onSelect(2)
            }
            LiquidGlassPreviewTab("Settings", Icons.Filled.Settings, selectedIndex == 3, selectedPillColor, selectedContentColor, inactiveContentColor) {
                pulse++
                onSelect(3)
            }
        }
    }
}

@Composable
private fun RowScope.LiquidGlassPreviewTab(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    selectedPillColor: Color,
    selectedContentColor: Color,
    inactiveContentColor: Color,
    onClick: () -> Unit
) {
    val contentColor = if (selected) selectedContentColor else inactiveContentColor
    val pressSource = remember { MutableInteractionSource() }
    val isPressed by pressSource.collectIsPressedAsState()
    val pressScaleX by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.62f, stiffness = 720f),
        label = "liquidPreviewTabPressScaleX"
    )
    val pressScaleY by animateFloatAsState(
        targetValue = if (isPressed) 1.08f else 1f,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.62f, stiffness = 720f),
        label = "liquidPreviewTabPressScaleY"
    )

    Box(
        modifier = Modifier
            .weight(1f)
            .height(50.dp)
            .graphicsLayer {
                scaleX = pressScaleX
                scaleY = pressScaleY
            }
            .clip(GlassChromeInnerShape)
            .background(
                if (selected) selectedPillColor else Color.Transparent,
                GlassChromeInnerShape
            )
            .clickable(
                interactionSource = pressSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = contentColor
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 10.sp,
                    lineHeight = 11.sp,
                    fontWeight = if (selected) FontWeight.Black else FontWeight.Medium
                ),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LiquidGlassSliderPanel(
    value: Float,
    adaptive: AdaptiveLuminanceState,
    adaptiveEnabled: Boolean,
    onValueChange: (Float) -> Unit,
    onAdaptiveEnabledChange: (Boolean) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val view = LocalView.current
    val cardColor = if (isDark) Color(0xFF232425) else Color(0xFFFEFEFE)
    val borderColor = if (isDark) Color(0xFF333538) else Color(0xFFE3E3E4)
    val sliderValue = (value.coerceIn(0f, 1f) * 100f).roundToInt().coerceIn(0, 100).toFloat()
    var lastStep by remember { mutableIntStateOf(sliderValue.roundToInt()) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = cardColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LiquidGlassTintIcon(
                    filled = false,
                    modifier = Modifier.size(28.dp)
                )
                LiquidGlassPercentSlider(
                    value = value,
                    modifier = Modifier.weight(1f),
                    onValueChange = { nextValue ->
                        val step = (nextValue.coerceIn(0f, 1f) * 100f).roundToInt().coerceIn(0, 100)
                        if (step != lastStep) {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            lastStep = step
                        }
                        onValueChange(step / 100f)
                    }
                )
                LiquidGlassTintIcon(
                    filled = true,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = stringResource(R.string.liquid_glass_preview_summary),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    lineHeight = 13.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            AdaptiveLuminanceReadout(
                adaptive = adaptive,
                enabled = adaptiveEnabled,
                onEnabledChange = onAdaptiveEnabledChange
            )
        }
    }
}

@Composable
private fun LiquidGlassTintIcon(
    filled: Boolean,
    modifier: Modifier = Modifier
) {
    val strokeColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.74f)
    val fillColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (filled) 0.72f else 0.13f)

    Canvas(modifier = modifier) {
        val strokeWidth = 1.5.dp.toPx()
        val iconWidth = size.width * 0.64f
        val iconHeight = size.height * 0.46f
        val left = (size.width - iconWidth) / 2f
        val top = (size.height - iconHeight) / 2f
        val radius = iconHeight / 2f

        drawRoundRect(
            color = fillColor,
            topLeft = Offset(left, top),
            size = Size(iconWidth, iconHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
        )
        drawRoundRect(
            color = strokeColor,
            topLeft = Offset(left + strokeWidth / 2f, top + strokeWidth / 2f),
            size = Size(iconWidth - strokeWidth, iconHeight - strokeWidth),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
        )
        if (!filled) {
            val offset = 4.2.dp.toPx()
            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(left - offset + strokeWidth / 2f, top - offset + strokeWidth / 2f),
                size = Size(iconWidth - strokeWidth, iconHeight - strokeWidth),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
            )
            drawRoundRect(
                color = strokeColor.copy(alpha = 0.72f),
                topLeft = Offset(left - offset + strokeWidth / 2f, top - offset + strokeWidth / 2f),
                size = Size(iconWidth - strokeWidth, iconHeight - strokeWidth),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
            )
        }
    }
}

@Composable
private fun AdaptiveLuminanceReadout(
    adaptive: AdaptiveLuminanceState,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit
) {
    val percent = (adaptive.luminance.coerceIn(0f, 1f) * 100f).roundToInt()
    val sampleIsBright = adaptive.luminance > 0.5f
    val swatchColor = if (sampleIsBright) Color(0xFFF4F6FA) else Color(0xFF111318)
    val swatchBorder = if (sampleIsBright) Color(0xFFCBD2DC) else Color.White.copy(alpha = 0.28f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Adaptive luminance",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.58f)
            }
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = if (enabled) swatchColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.46f),
                contentColor = if (enabled) adaptive.contentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                border = BorderStroke(1.dp, if (enabled) swatchBorder else swatchBorder.copy(alpha = 0.48f))
            ) {
                Text(
                    text = if (enabled) "$percent%" else "Off",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange
            )
        }
    }
}

@Composable
private fun LiquidGlassPercentSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val step = (value.coerceIn(0f, 1f) * 100f).roundToInt().coerceIn(0, 100)
    val fraction = step / 100f
    val trackPadding = 6.dp
    val indicatorWidth = 58.dp
    val indicatorHeight = 34.dp
    var widthPx by remember { mutableIntStateOf(0) }
    var pressed by remember { mutableStateOf(false) }
    val handleWidth by animateFloatAsState(
        targetValue = if (pressed) 6f else 9f,
        animationSpec = tween(durationMillis = 100),
        label = "liquidGlassSliderHandleWidth"
    )

    fun updateFromX(x: Float, onValueChange: (Float) -> Unit) {
        if (widthPx <= 0) return
        val trackPaddingPx = with(density) { trackPadding.toPx() }
        val trackWidthPx = (widthPx - trackPaddingPx * 2f).coerceAtLeast(1f)
        val nextStep = (((x - trackPaddingPx) / trackWidthPx).coerceIn(0f, 1f) * 100f)
            .roundToInt()
            .coerceIn(0, 100)
        onValueChange(nextStep / 100f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(78.dp)
            .onSizeChanged { widthPx = it.width }
            .pointerInput(widthPx) {
                detectDragGestures(
                    onDragStart = { offset ->
                        pressed = true
                        updateFromX(offset.x, onValueChange)
                    },
                    onDragEnd = { pressed = false },
                    onDragCancel = { pressed = false },
                    onDrag = { change, _ ->
                        updateFromX(change.position.x, onValueChange)
                    }
                )
            }
    ) {
        LiquidGlassDotTrack(
            value = value,
            handleWidthDp = handleWidth,
            horizontalPaddingDp = trackPadding.value,
            handleTopDp = indicatorHeight.value,
            modifier = Modifier
                .fillMaxSize()
        )

        if (widthPx > 0) {
            val pillWidthPx = with(density) { indicatorWidth.toPx() }
            val trackPaddingPx = with(density) { trackPadding.toPx() }
            val trackWidthPx = (widthPx - trackPaddingPx * 2f).coerceAtLeast(1f)
            val handleX = trackPaddingPx + trackWidthPx * fraction
            val pillX = (handleX - pillWidthPx / 2f)
                .roundToInt()
                .coerceIn(0, (widthPx - pillWidthPx).roundToInt().coerceAtLeast(0))

            Surface(
                modifier = Modifier
                    .offset { IntOffset(pillX, 0) }
                    .size(width = indicatorWidth, height = indicatorHeight),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shadowElevation = 1.dp
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$step%",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun LiquidGlassDotTrack(
    value: Float,
    handleWidthDp: Float,
    horizontalPaddingDp: Float,
    handleTopDp: Float,
    modifier: Modifier = Modifier
) {
    val activeDots = (value.coerceIn(0f, 1f) * 100f).roundToInt().coerceIn(0, 100)
    val activeColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.95f)
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f)
    val centerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)

    Canvas(modifier = modifier) {
        val count = 100
        val horizontalPaddingPx = horizontalPaddingDp.dp.toPx()
        val trackStart = horizontalPaddingPx
        val trackEnd = size.width - horizontalPaddingPx
        val trackWidth = (trackEnd - trackStart).coerceAtLeast(1f)
        val spacing = if (count > 1) trackWidth / (count - 1) else 0f
        val handleTop = handleTopDp.dp.toPx()
        val centerY = (handleTop + 24.dp.toPx()).coerceAtMost(size.height - 18.dp.toPx())
        val centerIndex = 50
        val handleX = trackStart + (value.coerceIn(0f, 1f) * trackWidth)
        val centerX = trackStart + centerIndex * spacing
        val left = minOf(centerX, handleX)
        val right = maxOf(centerX, handleX)

        drawRoundRect(
            color = inactiveColor.copy(alpha = 0.16f),
            topLeft = Offset(trackStart, centerY - 5.dp.toPx()),
            size = androidx.compose.ui.geometry.Size(trackWidth, 10.dp.toPx()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(999f, 999f)
        )
        if (abs(handleX - centerX) > 1f) {
            drawRoundRect(
                color = activeColor.copy(alpha = 0.58f),
                topLeft = Offset(left, centerY - 6.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(right - left, 12.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(999f, 999f)
            )
        }

        repeat(count) { index ->
            val tenth = index % 10 == 0 || index == count - 1
            val active = if (activeDots >= centerIndex) {
                index in centerIndex until activeDots
            } else {
                index in activeDots..centerIndex
            }
            drawCircle(
                color = when {
                    index == centerIndex -> centerColor
                    active -> activeColor
                    else -> inactiveColor
                },
                radius = if (tenth) 2.45.dp.toPx() else 1.75.dp.toPx(),
                center = Offset(trackStart + index * spacing, centerY)
            )
        }
        drawRoundRect(
            color = centerColor,
            topLeft = Offset(centerX - 1.6.dp.toPx(), centerY - 16.dp.toPx()),
            size = androidx.compose.ui.geometry.Size(3.2.dp.toPx(), 32.dp.toPx()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(999f, 999f)
        )
        drawRoundRect(
            color = activeColor,
            topLeft = Offset(handleX - (handleWidthDp / 2f).dp.toPx(), handleTop),
            size = androidx.compose.ui.geometry.Size(handleWidthDp.dp.toPx(), size.height - handleTop),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(999f, 999f)
        )
    }
}
