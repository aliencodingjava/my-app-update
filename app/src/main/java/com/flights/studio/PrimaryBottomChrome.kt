package com.flights.studio

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.util.fastCoerceAtMost
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.capsule.ContinuousCapsule
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.util.lerp
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

enum class PrimaryTabDestination {
    Home,
    Briefing,
    Notes,
    Settings
}

data class PrimaryMenuAction(
    val label: String,
    @param:DrawableRes @field:DrawableRes val iconRes: Int,
    val onClick: () -> Unit,
    val useProfileAvatar: Boolean = false
)

data class GlassBottomTabItem(
    val label: String,
    val icon: ImageVector,
    val selected: Boolean,
    val onClick: () -> Unit,
    val lanternColor: Color? = null
)

@Composable
fun PrimaryBottomChrome(
    selectedTab: PrimaryTabDestination,
    backdrop: LayerBackdrop,
    menuVisible: Boolean,
    menuActions: List<PrimaryMenuAction>,
    onMenuDismiss: () -> Unit,
    onOpenHome: () -> Unit,
    onOpenContacts: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenMenu: () -> Unit,
    showTabs: Boolean = true,
    showMenu: Boolean = true,
    contentView: android.view.View? = null,
    menuIcon: ImageVector = Icons.Filled.Menu,
    menuPanelColor: Color? = null,
    menuOverlayTint: Color? = null,
    menuButtonColor: Color? = null,
    menuButtonAlpha: Float? = null,
    menuBlurDp: Float? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(30f)
    ) {
        if (showTabs) {
            PrimaryQuickTabBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp)
                    .navigationBarsPadding(),
                selectedTab = selectedTab,
                backdrop = backdrop,
                onOpenHome = onOpenHome,
                onOpenContacts = onOpenContacts,
                onOpenNotes = onOpenNotes,
                onOpenSettings = onOpenSettings,
                onOpenMenu = onOpenMenu,
                contentView = contentView,
                menuIcon = menuIcon
            )
        }

        if (showMenu) {
            PrimaryMenuSheet(
                visible = menuVisible,
                modifier = Modifier.align(Alignment.BottomCenter),
                backdrop = backdrop,
                contentView = contentView,
                actions = menuActions,
                panelColorOverride = menuPanelColor,
                overlayTintOverride = menuOverlayTint,
                buttonColorOverride = menuButtonColor,
                buttonAlphaOverride = menuButtonAlpha,
                blurDpOverride = menuBlurDp,
                onDismiss = onMenuDismiss
            )
        }
    }
}

@Composable
private fun PrimaryQuickTabBar(
    modifier: Modifier = Modifier,
    selectedTab: PrimaryTabDestination,
    backdrop: LayerBackdrop,
    onOpenHome: () -> Unit,
    onOpenContacts: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenMenu: () -> Unit,
    contentView: android.view.View?,
    menuIcon: ImageVector
) {
    val tabs = listOf(
        GlassBottomTabItem(
            label = stringResource(R.string.Home),
            icon = Icons.Filled.Home,
            selected = selectedTab == PrimaryTabDestination.Home,
            onClick = onOpenHome
        ),
        GlassBottomTabItem(
            label = stringResource(R.string.chat_bottom_tab),
            icon = Icons.Filled.Info,
            selected = selectedTab == PrimaryTabDestination.Briefing,
            onClick = onOpenContacts
        ),
        GlassBottomTabItem(
            label = stringResource(R.string.contacts_bottom_notes),
            icon = Icons.AutoMirrored.Filled.Article,
            selected = selectedTab == PrimaryTabDestination.Notes,
            onClick = onOpenNotes
        ),
        GlassBottomTabItem(
            label = stringResource(R.string.menu_settings),
            icon = Icons.Filled.Settings,
            selected = selectedTab == PrimaryTabDestination.Settings,
            onClick = onOpenSettings
        ),
        GlassBottomTabItem(
            label = stringResource(R.string.settings_menu_tab),
            icon = menuIcon,
            selected = false,
            onClick = onOpenMenu
        )
    )
    GlassBottomTabBar(
        modifier = modifier,
        backdrop = backdrop,
        tabs = tabs,
        contentView = contentView,
        dragActions = remember(onOpenHome, onOpenContacts, onOpenNotes, onOpenSettings) {
            listOf(onOpenHome, onOpenContacts, onOpenNotes, onOpenSettings)
        }
    )
}

@Composable
internal fun GlassBottomTabBar(
    modifier: Modifier = Modifier,
    backdrop: LayerBackdrop,
    tabs: List<GlassBottomTabItem>,
    contentView: android.view.View?,
    dragActions: List<() -> Unit> = emptyList()
) {
    val glassColor = bottomTabBarTint()
    val overlayTint = bottomTabBarOverlayTint()
    val isDark = isSystemInDarkTheme()
    val liquidGlassTintAmount = rememberLiquidGlassTintAmount()
    val adaptiveEnabled = rememberLiquidGlassAdaptiveLuminanceEnabled()
    val backdropBlurDp = bottomChromeBackdropBlurDp()
    val nativeBlurPx = bottomChromeNativeBlurPx()
    val adaptive = rememberAdaptiveLuminance(
        enabled = adaptiveEnabled,
        lightOnBright = Color(0xFF101318),
        lightOnDark = Color.White
    )
    val adaptiveContentColor = adaptive.contentColor
    val adaptiveOffset = adaptiveLuminanceOffset(adaptive.luminance)
    val adaptiveEffectStrength = if (adaptiveEnabled) {
        adaptiveLuminanceEffectStrength(liquidGlassTintAmount)
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
        lerp(0.18f, if (isDark) 0.56f else 0.68f, adaptiveEffectStrength)
    } else {
        0f
    }
    val appThemePalette = LocalAppThemePalette.current
    val tabAccent = appThemePalette.accent
    val tabAccentWarm = appThemePalette.warm
    val tabAccentRose = appThemePalette.rose
    val adaptiveSelectedContentColor = lerpColor(
        bottomTabSelectedContentColor(),
        adaptiveContentColor,
        adaptiveContentBlend
    )
    val selectedIndex = tabs.indexOfFirst { it.selected }.takeIf { it >= 0 } ?: 0
    val selectedLanternColor = tabs.getOrNull(selectedIndex)?.lanternColor
    val lanternAlpha by animateFloatAsState(
        targetValue = if (selectedLanternColor != null) 1f else 0f,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "bottomTabLanternAlpha"
    )
    var draggedTabIndex by remember { mutableStateOf<Int?>(null) }

    Box(
        modifier = modifier
            .padding(horizontal = GlassChromeHorizontalPadding)
            .fillMaxWidth()
            .height(56.dp)
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
            .background(
                color = overlayTint,
                shape = GlassChromeShape
            )
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { FrostedActionBarBlurView(it) },
            update = {
                it.contentView = contentView
                it.scrimColor = glassColor.toArgb()
                it.cornerRadiusPx = it.resources.displayMetrics.density * 28f
                it.useLiquidRefraction = true
                it.blurRadiusPx = nativeBlurPx
                it.saturation = 1.18f
                it.refractIntensity = GlassChromeNativeRefractionIntensity
            }
        )
        if (adaptiveSurfaceStrength > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(adaptiveSurfaceTint, GlassChromeShape)
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(GlassChromeShape)
                .drawBehind {
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colors = if (isDark) {
                                listOf(
                                    Color(0xFF0F172A).copy(alpha = 0.16f),
                                    tabAccent.copy(alpha = 0.15f),
                                    tabAccentWarm.copy(alpha = 0.10f)
                                )
                            } else {
                                listOf(
                                    Color.White.copy(alpha = 0.12f),
                                    tabAccent.copy(alpha = 0.085f),
                                    tabAccentWarm.copy(alpha = 0.095f)
                                )
                            },
                            start = Offset.Zero,
                            end = Offset(size.width, size.height)
                        )
                    )
                    withTransform({
                        rotate(-12f, pivot = Offset(size.width * 0.86f, size.height * 0.40f))
                    }) {
                        drawRoundRect(
                            color = tabAccent.copy(alpha = if (isDark) 0.22f else 0.13f),
                            topLeft = Offset(size.width * 0.70f, -8.dp.toPx()),
                            size = androidx.compose.ui.geometry.Size(size.width * 0.24f, 18.dp.toPx()),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(9.dp.toPx(), 9.dp.toPx())
                        )
                        drawRoundRect(
                            color = tabAccentWarm.copy(alpha = if (isDark) 0.22f else 0.16f),
                            topLeft = Offset(size.width * 0.80f, 15.dp.toPx()),
                            size = androidx.compose.ui.geometry.Size(size.width * 0.18f, 6.dp.toPx()),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx())
                        )
                    }
                    drawRoundRect(
                        color = tabAccentRose.copy(alpha = if (isDark) 0.16f else 0.10f),
                        topLeft = Offset(size.width * 0.06f, size.height - 10.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(size.width * 0.24f, 5.dp.toPx()),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx())
                    )
                }
        )
        if (selectedLanternColor != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(GlassChromeShape)
                    .drawBehind {
                        if (lanternAlpha <= 0.001f || tabs.isEmpty()) return@drawBehind
                        drawRect(
                            brush = Brush.radialGradient(
                                0.00f to selectedLanternColor.copy(alpha = 0.96f * lanternAlpha),
                                0.18f to selectedLanternColor.copy(alpha = 0.68f * lanternAlpha),
                                0.52f to selectedLanternColor.copy(alpha = 0.24f * lanternAlpha),
                                1.00f to selectedLanternColor.copy(alpha = 0f),
                                center = Offset(0f, size.height * 0.34f),
                                radius = size.width * 0.92f
                            )
                        )
                        drawRect(
                            brush = Brush.linearGradient(
                                0.00f to selectedLanternColor.copy(alpha = 0.72f * lanternAlpha),
                                0.16f to Color.White.copy(alpha = 0.22f * lanternAlpha),
                                0.36f to selectedLanternColor.copy(alpha = 0.34f * lanternAlpha),
                                0.72f to selectedLanternColor.copy(alpha = 0.10f * lanternAlpha),
                                1.00f to Color.White.copy(alpha = 0.00f),
                                start = Offset.Zero,
                                end = Offset(size.width, size.height * 0.78f)
                            )
                        )
                        drawRect(
                            brush = Brush.verticalGradient(
                                0.00f to Color.White.copy(alpha = 0.22f * lanternAlpha),
                                0.18f to Color.White.copy(alpha = 0.07f * lanternAlpha),
                                0.56f to selectedLanternColor.copy(alpha = 0.13f * lanternAlpha),
                                1.00f to Color.Black.copy(alpha = 0.06f * lanternAlpha)
                            )
                        )
                    }
            )
        }
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
                .pointerInput(dragActions, tabs.size) {
                    fun moveSelection(x: Float) {
                        if (dragActions.isEmpty() || tabs.isEmpty()) return
                        val tabWidth = size.width / tabs.size.toFloat()
                        val index = (x / tabWidth).toInt().coerceIn(0, tabs.lastIndex)
                        if (index in dragActions.indices && draggedTabIndex != index) {
                            draggedTabIndex = index
                            dragActions[index]()
                        }
                    }

                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset -> moveSelection(offset.x) },
                        onDragCancel = { draggedTabIndex = null },
                        onDragEnd = { draggedTabIndex = null },
                        onDrag = { change, _ -> moveSelection(change.position.x) }
                    )
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            tabs.forEachIndexed { index, tab ->
                PrimaryQuickTab(
                    label = tab.label,
                    icon = tab.icon,
                    selected = draggedTabIndex == index || (draggedTabIndex == null && tab.selected),
                    adaptiveContentColor = adaptiveContentColor,
                    adaptiveSelectedContentColor = adaptiveSelectedContentColor,
                    adaptiveContentBlend = adaptiveContentBlend,
                    onClick = tab.onClick
                )
            }
        }
    }
}

@Composable
private fun RowScope.PrimaryQuickTab(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    adaptiveContentColor: Color,
    adaptiveSelectedContentColor: Color,
    adaptiveContentBlend: Float,
    onClick: () -> Unit
) {
    val inactiveColor = lerpColor(bottomTabInactiveColor(), adaptiveContentColor, adaptiveContentBlend)
    val selectedContentColor = adaptiveSelectedContentColor
    val selectedPillColor = bottomTabSelectedPillColor()
    val isDark = isSystemInDarkTheme()
    val appThemePalette = LocalAppThemePalette.current
    val pillAccent = appThemePalette.accent
    val pillAccentWarm = appThemePalette.warm
    val pressSource = remember { MutableInteractionSource() }
    val isPressed by pressSource.collectIsPressedAsState()
    val pillAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = if (selected) 180 else 120, easing = FastOutSlowInEasing),
        label = "primaryTabPillAlpha"
    )
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 720f),
        label = "primaryTabPressScale"
    )
    val pressAlpha by animateFloatAsState(
        targetValue = if (isPressed && !selected) 0.10f else 0f,
        animationSpec = tween(90),
        label = "primaryTabPressAlpha"
    )

    Box(
        modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(GlassChromeInnerShape)
            .clickable(
                interactionSource = pressSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = pillAlpha
                }
                .drawBehind {
                    drawRoundRect(
                        color = selectedPillColor,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx(), 24.dp.toPx())
                    )
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                pillAccent.copy(alpha = if (isDark) 0.26f else 0.16f),
                                pillAccentWarm.copy(alpha = if (isDark) 0.20f else 0.14f),
                                Color.Transparent
                            ),
                            start = Offset.Zero,
                            end = Offset(size.width, size.height)
                        ),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx(), 24.dp.toPx())
                    )
                    drawRoundRect(
                        color = Color.White.copy(alpha = if (isDark) 0.08f else 0.16f),
                        topLeft = Offset(8.dp.toPx(), 5.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(size.width - 16.dp.toPx(), 1.dp.toPx()),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx(), 1.dp.toPx())
                    )
                }
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = pressAlpha
                }
                .background(
                    selectedPillColor.copy(alpha = 0.35f),
                    GlassChromeInnerShape
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(20.dp),
                tint = if (selected) selectedContentColor else inactiveColor
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 10.sp,
                    lineHeight = 11.sp,
                    fontWeight = if (selected) FontWeight.Black else FontWeight.Medium
                ),
                color = if (selected) selectedContentColor else inactiveColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PrimaryMenuSheet(
    visible: Boolean,
    modifier: Modifier = Modifier,
    backdrop: LayerBackdrop,
    contentView: android.view.View?,
    actions: List<PrimaryMenuAction>,
    panelColorOverride: Color?,
    overlayTintOverride: Color?,
    buttonColorOverride: Color?,
    buttonAlphaOverride: Float?,
    blurDpOverride: Float?,
    onDismiss: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val panelColor = panelColorOverride ?: bottomTabBarTint()
    val overlayTint = overlayTintOverride ?: bottomTabBarOverlayTint()
    val backdropBlurDp = blurDpOverride ?: (bottomChromeBackdropBlurDp() + (rememberLiquidGlassBlurAmount() * 8f))
    val nativeBlurPx = bottomChromeNativeBlurPx()
    val textColor = if (isDark) Color.White.copy(alpha = 0.92f) else Color(0xFF1E1F24)
    val iconColor = if (isDark) Color.White.copy(alpha = 0.92f) else Color(0xFF1E1F24)
    val buttonColor = buttonColorOverride ?: if (isDark) Color.White else Color.Black

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 130)),
        exit = fadeOut(animationSpec = tween(durationMillis = 160))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (isDark) 0.38f else 0.18f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier.imePadding(),
        enter = slideInVertically(
            animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing),
            initialOffsetY = { it / 2 }
        ) + fadeIn(animationSpec = tween(durationMillis = 170)) +
            scaleIn(
                animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing),
                initialScale = 0.94f
            ),
        exit = slideOutVertically(
            animationSpec = tween(durationMillis = 210, easing = FastOutLinearInEasing),
            targetOffsetY = { it / 3 }
        ) + fadeOut(animationSpec = tween(durationMillis = 150)) +
            scaleOut(
                animationSpec = tween(durationMillis = 210, easing = FastOutLinearInEasing),
                targetScale = 0.98f
            )
    ) {
        Box(
            modifier = Modifier
                .padding(
                    start = GlassChromeHorizontalPadding,
                    end = GlassChromeHorizontalPadding,
                    bottom = GlassChromeHorizontalPadding
                )
                .fillMaxWidth()
                .clip(GlassChromeShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .adaptiveLiquidGlassBackdrop(
                    backdrop = backdrop,
                    shape = GlassChromeShape,
                    surfaceColor = panelColor,
                    blurDp = backdropBlurDp,
                    shadow = { bottomChromeShadow() },
                    refractionHeightDp = GlassChromeRefractionHeightDp,
                    refractionAmountDp = GlassChromeRefractionAmountDp
                )
                .background(overlayTint, GlassChromeShape)
        ) {
            AndroidView(
                modifier = Modifier.matchParentSize(),
                factory = { FrostedActionBarBlurView(it) },
                update = {
                    it.contentView = contentView
                    it.scrimColor = panelColor.toArgb()
                    it.cornerRadiusPx = it.resources.displayMetrics.density * 28f
                    it.useLiquidRefraction = true
                    it.blurRadiusPx = blurDpOverride?.let { blurDp -> it.resources.displayMetrics.density * blurDp }
                        ?: nativeBlurPx
                    it.saturation = 1.18f
                    it.refractIntensity = GlassChromeNativeRefractionIntensity
                }
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                actions.chunked(4).forEach { rowActions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        rowActions.forEach { action ->
                            PrimaryMenuButton(
                                action = action,
                                backdrop = backdrop,
                                buttonColor = buttonColor,
                                buttonAlphaOverride = buttonAlphaOverride,
                                iconColor = iconColor,
                                textColor = textColor,
                                onDismiss = onDismiss
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.PrimaryMenuButton(
    action: PrimaryMenuAction,
    backdrop: LayerBackdrop,
    buttonColor: Color,
    buttonAlphaOverride: Float?,
    iconColor: Color,
    textColor: Color,
    onDismiss: () -> Unit
) {
    val animationScope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()
    val interactiveHighlight = remember(animationScope, isDark) {
        InteractiveHighlight(
            animationScope = animationScope,
            highlightColor = if (isDark) Color.White else Color.Black
        )
    }
    Column(
        modifier = Modifier
            .weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .size(60.dp)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { ContinuousCapsule },
                    highlight = null,
                    effects = {
                        vibrancy()
                        blur(2f.dp.toPx())
                        lens(12f.dp.toPx(), 24f.dp.toPx())
                    },
                    layerBlock = {
                        val width = size.width
                        val height = size.height

                        val progress = interactiveHighlight.pressProgress
                        val scale = lerp(1f, 1f + 6f.dp.toPx() / size.height, progress)

                        val maxOffset = size.minDimension
                        val initialDerivative = 0.05f
                        val offset = interactiveHighlight.offset
                        translationX = maxOffset * tanh(initialDerivative * offset.x / maxOffset)
                        translationY = maxOffset * tanh(initialDerivative * offset.y / maxOffset)

                        val maxDragScale = 6f.dp.toPx() / size.height
                        val offsetAngle = atan2(offset.y, offset.x)
                        scaleX =
                            scale +
                                maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) *
                                (width / height).fastCoerceAtMost(1f)
                        scaleY =
                            scale +
                                maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) *
                                (height / width).fastCoerceAtMost(1f)
                    },
                    onDrawSurface = {
                        val baseAlpha = buttonAlphaOverride ?: if (isDark) 0.20f else 0.075f
                        drawRect(buttonColor.copy(alpha = baseAlpha))
                        drawRect(buttonColor.copy(alpha = baseAlpha * 0.72f * interactiveHighlight.pressProgress))
                    }
                )
                .clickable(
                    interactionSource = null,
                    indication = null,
                    role = Role.Button,
                    onClick = {
                        onDismiss()
                        action.onClick()
                    }
                )
                .then(interactiveHighlight.modifier)
                .then(interactiveHighlight.gestureModifier),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (action.useProfileAvatar) {
                PrimaryMenuProfileAvatar(
                    modifier = Modifier
                        .size(34.dp)
                )
            } else {
                Icon(
                    painter = painterResource(action.iconRes),
                    contentDescription = action.label,
                    modifier = Modifier
                        .size(24.dp),
                    tint = iconColor
                )
            }
        }
        Text(
            text = action.label,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 10.5.sp,
                lineHeight = 11.5.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            ),
            color = textColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PrimaryMenuProfileAvatar(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val contentColor = if (isDark) Color.White.copy(alpha = 0.92f) else Color(0xFF1E1F24)
    val appContext = context.applicationContext
    val userPrefs = remember(appContext) { UserPreferencesManager(appContext) }
    val rawPhoto = userPrefs.userPhotoUriString?.trim().orEmpty()
    val initials = remember(userPrefs.userName) { primaryProfileInitials(userPrefs.userName.orEmpty()) }
    var avatarFailed by remember(rawPhoto, initials) { mutableStateOf(false) }
    val initialAvatarState: PrimaryMenuAvatarState =
        if (rawPhoto.isBlank()) PrimaryMenuAvatarState.Empty else PrimaryMenuAvatarState.Loading
    val avatarState by produceState(
        initialValue = initialAvatarState,
        key1 = rawPhoto
    ) {
        value = resolvePrimaryMenuAvatar(appContext, rawPhoto)
    }

    Surface(
        modifier = modifier.clip(CircleShape),
        shape = CircleShape,
        color = if (isDark) Color.White.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.075f),
        contentColor = contentColor,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f))
    ) {
        when (val state = avatarState) {
            is PrimaryMenuAvatarState.Ready -> {
                if (!avatarFailed) {
                    AsyncImage(
                        model = ImageRequest.Builder(appContext)
                            .data(state.data)
                            .crossfade(true)
                            .build(),
                        contentDescription = stringResource(R.string.photo),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        onError = { avatarFailed = true }
                    )
                } else {
                    PrimaryMenuInitials(initials, contentColor)
                }
            }
            PrimaryMenuAvatarState.Empty,
            PrimaryMenuAvatarState.Loading -> PrimaryMenuInitials(initials, contentColor)
        }
    }
}

@Composable
private fun PrimaryMenuInitials(initials: String, contentColor: Color) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (initials.isBlank() || initials == "?") {
            Icon(
                painter = painterResource(R.drawable.account_circle_24dp_ffffff_fill1_profile),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(0.78f),
                tint = contentColor
            )
        } else {
            Text(
                text = initials,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = contentColor
            )
        }
    }
}

private fun primaryProfileInitials(name: String): String {
    return name.trim()
        .split(Regex("\\s+"))
        .filter { token -> token.any { it.isLetter() } }
        .take(3)
        .mapNotNull { token -> token.firstOrNull { it.isLetter() }?.uppercaseChar() }
        .joinToString("")
        .ifBlank { "?" }
}

private suspend fun resolvePrimaryMenuAvatar(
    appContext: Context,
    rawPhoto: String
): PrimaryMenuAvatarState {
    val raw = rawPhoto.trim()
    if (raw.isBlank() || raw.equals("null", ignoreCase = true)) return PrimaryMenuAvatarState.Empty

    if (raw.startsWith("http", true) ||
        raw.startsWith("content", true) ||
        raw.startsWith("file", true)
    ) {
        return PrimaryMenuAvatarState.Ready(raw)
    }

    val local = AvatarDiskCache.localFile(appContext, raw)
    if (local.exists() && local.length() > 0L) return PrimaryMenuAvatarState.Ready(local)

    SignedUrlCache.getValid(raw)?.let { return PrimaryMenuAvatarState.Ready(it) }

    val session = SupabaseManager.client.auth.currentSessionOrNull()
    if (session != null) {
        val fresh = withContext(Dispatchers.IO) {
            SupabaseStorageUploader.createSignedUrl(
                objectPath = raw,
                authToken = session.accessToken,
                bucket = "profile-photos"
            )
        }
        if (!fresh.isNullOrBlank()) {
            SignedUrlCache.put(raw, fresh, 60 * 60)
            AvatarDiskCache.cacheFromSignedUrl(appContext, raw, fresh)
            return PrimaryMenuAvatarState.Ready(fresh)
        }
    }

    return PrimaryMenuAvatarState.Empty
}

private sealed interface PrimaryMenuAvatarState {
    data object Empty : PrimaryMenuAvatarState
    data object Loading : PrimaryMenuAvatarState
    data class Ready(val data: Any) : PrimaryMenuAvatarState
}
