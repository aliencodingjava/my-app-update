package com.flights.studio

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
import androidx.compose.material.icons.filled.ImportContacts
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ContactsBottomChrome(
    contactCount: Int,
    backdrop: Backdrop,
    onOpenHome: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenNotes: () -> Unit,
    onAddContact: () -> Unit,
    onImportContacts: () -> Unit
) {
    var showMenuSheet by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        ContactsQuickTabBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 4.dp)
                .navigationBarsPadding(),
            onOpenHome = onOpenHome,
            onOpenSettings = onOpenSettings,
            onOpenNotes = onOpenNotes,
            onOpenMenu = { showMenuSheet = true },
            backdrop = backdrop
        )

        ContactsMenuSheet(
            visible = showMenuSheet,
            modifier = Modifier.align(Alignment.BottomCenter),
            contactCount = contactCount,
            backdrop = backdrop,
            onDismiss = { showMenuSheet = false },
            onAddContact = onAddContact,
            onImportContacts = onImportContacts
        )
    }
}

@Composable
private fun ContactsQuickTabBar(
    modifier: Modifier = Modifier,
    onOpenHome: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenMenu: () -> Unit,
    backdrop: Backdrop
) {
    val isDark = isSystemInDarkTheme()
    val glassColor = if (isDark) {
        Color(0xFF34363C).copy(alpha = 0.82f)
    } else {
        Color(0xFFE6E2E7).copy(alpha = 0.78f)
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
                    blur(8.dp.toPx())
                    lens(
                        refractionHeight = 8.dp.toPx(),
                        refractionAmount = 24.dp.toPx(),
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
                    Color(0xFF7A7480).copy(alpha = 0.10f)
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
            ContactsQuickTab(
                label = stringResource(R.string.Home),
                icon = Icons.Filled.Home,
                selected = false,
                onClick = onOpenHome
            )
            ContactsQuickTab(
                label = stringResource(R.string.menu_settings),
                icon = Icons.Filled.Settings,
                selected = false,
                onClick = onOpenSettings
            )
            ContactsQuickTab(
                label = stringResource(R.string.contacts_bottom_notes),
                icon = Icons.AutoMirrored.Filled.Article,
                selected = false,
                onClick = onOpenNotes
            )
            ContactsQuickTab(
                label = stringResource(R.string.settings_menu_tab),
                icon = Icons.Filled.Menu,
                selected = true,
                onClick = onOpenMenu
            )
        }
    }
}

@Composable
private fun RowScope.ContactsQuickTab(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val selectedColor = if (isSystemInDarkTheme()) {
        Color.White.copy(alpha = 0.13f)
    } else {
        Color.White.copy(alpha = 0.72f)
    }
    val inactiveColor = if (isSystemInDarkTheme()) {
        Color.White.copy(alpha = 0.72f)
    } else {
        Color(0xFF555763)
    }
    val selectedContentColor = if (isSystemInDarkTheme()) {
        Color.White
    } else {
        MaterialTheme.colorScheme.primary
    }

    Surface(
        modifier = Modifier
            .weight(1f)
            .height(46.dp)
            .clip(GlassChromeInnerShape)
            .clickable(onClick = onClick),
        shape = GlassChromeInnerShape,
        color = if (selected) selectedColor else Color.Transparent,
        contentColor = if (selected) selectedContentColor else inactiveColor
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 10.sp,
                    lineHeight = 11.sp,
                    fontWeight = if (selected) FontWeight.Black else FontWeight.Medium
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private data class ContactsMenuAction(
    val label: String,
    val icon: ImageVector,
    val enabled: Boolean = true,
    val onClick: () -> Unit = {}
)

@Composable
private fun ContactsMenuSheet(
    visible: Boolean,
    modifier: Modifier = Modifier,
    contactCount: Int,
    backdrop: Backdrop,
    onDismiss: () -> Unit,
    onAddContact: () -> Unit,
    onImportContacts: () -> Unit
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
    val formattedCount = remember(contactCount) {
        NumberFormat.getIntegerInstance(Locale.getDefault()).format(contactCount)
    }
    val actions = listOf(
        ContactsMenuAction(
            label = stringResource(R.string.add_contact),
            icon = Icons.Filled.PersonAdd,
            onClick = onAddContact
        ),
        ContactsMenuAction(
            label = stringResource(R.string.Import_contacts),
            icon = Icons.Filled.ImportContacts,
            onClick = onImportContacts
        ),
        ContactsMenuAction(
            label = "$formattedCount ${stringResource(R.string.total_contacts)}",
            icon = Icons.Filled.Groups,
            enabled = false
        )
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 90)),
        exit = fadeOut(animationSpec = tween(durationMillis = 140))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (isSystemInDarkTheme()) 0.38f else 0.18f))
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
                            ContactsMenuButton(
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
private fun RowScope.ContactsMenuButton(
    action: ContactsMenuAction,
    buttonColor: Color,
    iconColor: Color,
    textColor: Color,
    onDismiss: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .weight(1f)
            .then(
                if (action.enabled) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = {
                            onDismiss()
                            action.onClick()
                        }
                    )
                } else {
                    Modifier
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
            Icon(
                imageVector = action.icon,
                contentDescription = action.label,
                modifier = Modifier
                    .padding(14.dp)
                    .size(24.dp)
            )
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
            color = textColor.copy(alpha = if (action.enabled) 1f else 0.78f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
