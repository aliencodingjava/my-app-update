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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    menuIcon: ImageVector = Icons.Filled.Menu
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
    val glassColor = bottomTabBarTint()
    val overlayTint = bottomTabBarOverlayTint()
    var draggedTabIndex by remember { mutableStateOf<Int?>(null) }
    val pageTabActions = remember(onOpenHome, onOpenContacts, onOpenNotes, onOpenSettings) {
        listOf(onOpenHome, onOpenContacts, onOpenNotes, onOpenSettings)
    }

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
                    vibrancy()
                    blur(GlassChromeBackdropBlurDp.dp.toPx(), edgeTreatment = TileMode.Mirror)
                    lens(
                        refractionHeight = GlassChromeRefractionHeightDp.dp.toPx(),
                        refractionAmount = GlassChromeRefractionAmountDp.dp.toPx(),
                        depthEffect = false,
                        chromaticAberration = false
                    )
                },
                onDrawSurface = { drawRect(glassColor) }
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
                it.blurRadiusPx = GlassChromeNativeBlurPx
                it.saturation = 1.18f
                it.refractIntensity = GlassChromeNativeRefractionIntensity
            }
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp)
                .pointerInput(pageTabActions) {
                    fun moveSelection(x: Float) {
                        val tabWidth = size.width / 5f
                        val index = (x / tabWidth).toInt().coerceIn(0, 4)
                        if (index in pageTabActions.indices && draggedTabIndex != index) {
                            draggedTabIndex = index
                            pageTabActions[index]()
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
            PrimaryQuickTab(
                label = stringResource(R.string.Home),
                icon = Icons.Filled.Home,
                selected = draggedTabIndex == 0 || (draggedTabIndex == null && selectedTab == PrimaryTabDestination.Home),
                onClick = onOpenHome
            )
            PrimaryQuickTab(
                label = stringResource(R.string.chat_bottom_tab),
                icon = Icons.Filled.Info,
                selected = selectedTab == PrimaryTabDestination.Briefing,
                onClick = onOpenContacts
            )
            PrimaryQuickTab(
                label = stringResource(R.string.contacts_bottom_notes),
                icon = Icons.AutoMirrored.Filled.Article,
                selected = draggedTabIndex == 2 || (draggedTabIndex == null && selectedTab == PrimaryTabDestination.Notes),
                onClick = onOpenNotes
            )
            PrimaryQuickTab(
                label = stringResource(R.string.menu_settings),
                icon = Icons.Filled.Settings,
                selected = draggedTabIndex == 3 || (draggedTabIndex == null && selectedTab == PrimaryTabDestination.Settings),
                onClick = onOpenSettings
            )
            PrimaryQuickTab(
                label = stringResource(R.string.settings_menu_tab),
                icon = menuIcon,
                selected = false,
                onClick = onOpenMenu
            )
        }
    }
}

@Composable
private fun RowScope.PrimaryQuickTab(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val inactiveColor = bottomTabInactiveColor()
    val selectedContentColor = primaryTabAccentColor()
    val selectedPillColor = bottomTabSelectedPillColor()
    val pressSource = remember { MutableInteractionSource() }
    val isPressed by pressSource.collectIsPressedAsState()
    val pillAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = if (selected) 180 else 120, easing = FastOutSlowInEasing),
        label = "primaryTabPillAlpha"
    )
    val pillScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.84f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 520f),
        label = "primaryTabPillScale"
    )
    val contentScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.94f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 620f),
        label = "primaryTabContentScale"
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
                    scaleX = pillScale
                    scaleY = pillScale
                }
                .background(selectedPillColor, GlassChromeInnerShape)
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
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = contentScale
                    scaleY = contentScale
                },
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
    onDismiss: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val panelColor = bottomTabBarTint()
    val overlayTint = bottomTabBarOverlayTint()
    val textColor = if (isDark) Color.White.copy(alpha = 0.92f) else Color(0xFF1E1F24)
    val iconColor = if (isDark) Color.White.copy(alpha = 0.92f) else Color(0xFF1E1F24)
    val buttonColor = bottomTabSelectedPillColor()

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
                .shadow(GlassChromeShadowElevation, GlassChromeShape, clip = false)
                .clip(GlassChromeShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { GlassChromeShape },
                    shadow = null,
                    highlight = null,
                    effects = {
                        vibrancy()
                        blur(GlassChromeBackdropBlurDp.dp.toPx(), edgeTreatment = TileMode.Mirror)
                        lens(
                            refractionHeight = GlassChromeRefractionHeightDp.dp.toPx(),
                            refractionAmount = GlassChromeRefractionAmountDp.dp.toPx(),
                            depthEffect = false,
                            chromaticAberration = false
                        )
                    },
                    onDrawSurface = { drawRect(panelColor) }
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
                    it.blurRadiusPx = GlassChromeNativeBlurPx
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
                                buttonColor = buttonColor,
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
    buttonColor: Color,
    iconColor: Color,
    textColor: Color,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    onDismiss()
                    action.onClick()
                }
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = buttonColor,
            contentColor = iconColor,
            shadowElevation = 0.dp,
            tonalElevation = 0.dp
        ) {
            if (action.useProfileAvatar) {
                PrimaryMenuProfileAvatar(
                    modifier = Modifier
                        .padding(8.dp)
                        .size(36.dp)
                )
            } else {
                Icon(
                    painter = painterResource(action.iconRes),
                    contentDescription = action.label,
                    modifier = Modifier
                        .padding(14.dp)
                        .size(24.dp),
                    tint = iconColor
                )
            }
        }
        Text(
            text = action.label,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 11.sp,
                lineHeight = 12.sp,
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
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
        contentColor = MaterialTheme.colorScheme.primary,
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
                    PrimaryMenuInitials(initials)
                }
            }
            PrimaryMenuAvatarState.Empty,
            PrimaryMenuAvatarState.Loading -> PrimaryMenuInitials(initials)
        }
    }
}

@Composable
private fun PrimaryMenuInitials(initials: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (initials.isBlank() || initials == "?") {
            Image(
                painter = painterResource(R.drawable.contact_logo_topbar),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = initials,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
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
