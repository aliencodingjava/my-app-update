package com.flights.studio

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class PrimaryTabDestination {
    Home,
    Contacts,
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
    backdrop: Backdrop,
    menuVisible: Boolean,
    menuActions: List<PrimaryMenuAction>,
    onMenuDismiss: () -> Unit,
    onOpenHome: () -> Unit,
    onOpenContacts: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenMenu: () -> Unit,
    showTabs: Boolean = true,
    showMenu: Boolean = true
) {
    Box(modifier = Modifier.fillMaxSize()) {
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
                onOpenMenu = onOpenMenu
            )
        }

        if (showMenu) {
            PrimaryMenuSheet(
                visible = menuVisible,
                modifier = Modifier.align(Alignment.BottomCenter),
                backdrop = backdrop,
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
    backdrop: Backdrop,
    onOpenHome: () -> Unit,
    onOpenContacts: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenMenu: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val glassColor = if (isDark) {
        Color(0x7B000000).copy(alpha = 0.76f)
    } else {
        Color(0xFFE6E2E7).copy(alpha = 0.58f)
    }

    Box(
        modifier = modifier
            .padding(horizontal = GlassChromeHorizontalPadding)
            .fillMaxWidth()
            .height(56.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { GlassChromeShape },
                shadow = null,
                highlight = null,
                effects = {
                    vibrancy()
                    blur(1.5.dp.toPx(), edgeTreatment = TileMode.Clamp)
                    lens(
                        refractionHeight = 20.dp.toPx(),
                        refractionAmount = 30.dp.toPx(),
                        depthEffect = false,
                        chromaticAberration = false
                    )
                },
                onDrawSurface = { drawRect(glassColor) }
            )
            .background(
                color = if (isDark) {
                    Color.White.copy(alpha = 0.06f)
                } else {
                    Color(0xFF7CB342).copy(alpha = 0.06f)
                },
                shape = GlassChromeShape
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            PrimaryQuickTab(
                label = stringResource(R.string.Home),
                icon = Icons.Filled.Home,
                selected = selectedTab == PrimaryTabDestination.Home,
                onClick = onOpenHome
            )
            PrimaryQuickTab(
                label = stringResource(R.string.total_contacts),
                icon = Icons.Filled.Groups,
                selected = selectedTab == PrimaryTabDestination.Contacts,
                onClick = onOpenContacts
            )
            PrimaryQuickTab(
                label = stringResource(R.string.contacts_bottom_notes),
                icon = Icons.AutoMirrored.Filled.Article,
                selected = selectedTab == PrimaryTabDestination.Notes,
                onClick = onOpenNotes
            )
            PrimaryQuickTab(
                label = stringResource(R.string.menu_settings),
                icon = Icons.Filled.Settings,
                selected = selectedTab == PrimaryTabDestination.Settings,
                onClick = onOpenSettings
            )
            PrimaryQuickTab(
                label = stringResource(R.string.settings_menu_tab),
                icon = Icons.Filled.Menu,
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
    val isDark = isSystemInDarkTheme()
    val inactiveColor = if (isDark) {
        Color.White.copy(alpha = 0.72f)
    } else {
        Color(0xFF555763)
    }
    val selectedContentColor = if (isDark) {
        Color.White
    } else {
        Color(0xFF0B57D0)
    }

    Box(
        modifier = Modifier
            .weight(1f)
            .height(46.dp)
            .clip(GlassChromeInnerShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
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
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.34f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (selected) selectedContentColor
                        else Color.Transparent
                    )
            )
        }
    }
}

@Composable
private fun PrimaryMenuSheet(
    visible: Boolean,
    modifier: Modifier = Modifier,
    backdrop: Backdrop,
    actions: List<PrimaryMenuAction>,
    onDismiss: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val panelColor = if (isDark) {
        Color(0xFF202124).copy(alpha = 0.62f)
    } else {
        Color(0xFFE6E2E7).copy(alpha = 0.52f)
    }
    val textColor = if (isDark) Color.White.copy(alpha = 0.92f) else Color(0xFF1E1F24)
    val iconColor = if (isDark) Color.White.copy(alpha = 0.92f) else Color(0xFF1E1F24)
    val buttonColor = if (isDark) {
        Color.White.copy(alpha = 0.14f)
    } else {
        Color.White.copy(alpha = 0.96f)
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 90)),
        exit = fadeOut(animationSpec = tween(durationMillis = 140))
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
            animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
            initialOffsetY = { it / 2 }
        ) + fadeIn(animationSpec = tween(durationMillis = 120)) +
            scaleIn(
                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                initialScale = 0.96f
            ),
        exit = slideOutVertically(
            animationSpec = tween(durationMillis = 170, easing = FastOutLinearInEasing),
            targetOffsetY = { it / 3 }
        ) + fadeOut(animationSpec = tween(durationMillis = 120)) +
            scaleOut(
                animationSpec = tween(durationMillis = 170, easing = FastOutLinearInEasing),
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
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { GlassChromeShape },
                    shadow = null,
                    highlight = null,
                    effects = {
                        vibrancy()
                        blur(4.dp.toPx())
                        lens(
                            refractionHeight = 22.dp.toPx(),
                            refractionAmount = 72.dp.toPx(),
                            depthEffect = false,
                            chromaticAberration = true
                        )
                    },
                    onDrawSurface = { drawRect(panelColor) }
                )
        ) {
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
