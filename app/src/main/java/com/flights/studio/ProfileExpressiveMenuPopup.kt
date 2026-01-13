@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.flights.studio

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.MenuItemColors
import androidx.compose.material3.MenuItemShapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.unit.LayoutDirection

// ---------- Defaults ----------
private val MenuIconSize = 20.dp
val MenuMinWidth = 200.dp

private const val KEY_EDIT = "edit"
private const val KEY_PHOTO = "photo"
private const val KEY_PRIVACY = "privacy"
private const val KEY_AUTH = "auth"

data class MenuFeatureFlags(
    val vibrant: Boolean = true,
    val groupLabels: Boolean = false,
    val groupDividers: Boolean = false,
    val supportingText: Boolean = false,
    val trailingIcon: Boolean = false,
    val badge: Boolean = false,
    val trailingText: Boolean = false,
    val persistentSelection: Boolean = false,
)

@Composable
fun ProfileExpressiveMenuPopupAnchored(
    expanded: Boolean,
    onDismiss: () -> Unit,
    anchorBounds: IntRect?,              // ✅ anchor in WINDOW coords
    hasProfile: Boolean,
    isLoggedIn: Boolean,
    onEdit: () -> Unit,
    onChangePhoto: () -> Unit,
    onPrivacy: () -> Unit,
    onLoginLogout: () -> Unit,
    modifier: Modifier = Modifier,
    selectedKey: String? = null,
    flags: MenuFeatureFlags = MenuFeatureFlags(),
) {
    if (!expanded || anchorBounds == null) return

    val scales = rememberUiScales()
    val density = LocalDensity.current

    val containerColor = if (flags.vibrant) {
        MenuDefaults.groupVibrantContainerColor
    } else {
        MenuDefaults.groupStandardContainerColor
    }

    val positionProvider = remember(anchorBounds, density) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset {
                val marginPx = with(density) { 8.dp.roundToPx() }

                var x = anchorBounds.right - popupContentSize.width - marginPx
                x = x.coerceIn(
                    marginPx,
                    windowSize.width - popupContentSize.width - marginPx
                )

                var y = anchorBounds.bottom + marginPx
                y = y.coerceIn(
                    marginPx,
                    windowSize.height - popupContentSize.height - marginPx
                )

                return IntOffset(x, y)

            }
        }
    }

    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismiss
    ) {
        // content width controlled by wrapContentWidth, but groups have their own min sizes
        MenuContent(
            modifier = modifier
                .wrapContentWidth()
                .widthIn(min = MenuMinWidth),
            hasProfile = hasProfile,
            isLoggedIn = isLoggedIn,
            flags = flags,
            selectedKey = selectedKey,
            scales = scales,
            containerColor = containerColor,
            onEdit = onEdit,
            onChangePhoto = onChangePhoto,
            onPrivacy = onPrivacy,
            onLoginLogout = onLoginLogout,
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun MenuContent(
    modifier: Modifier,
    hasProfile: Boolean,
    isLoggedIn: Boolean,
    flags: MenuFeatureFlags,
    selectedKey: String?,
    scales: UiScales,
    containerColor: androidx.compose.ui.graphics.Color,
    onEdit: () -> Unit,
    onChangePhoto: () -> Unit,
    onPrivacy: () -> Unit,
    onLoginLogout: () -> Unit,
    onDismiss: () -> Unit
) {
    val totalGroups = 2

    val topItems = remember(hasProfile, isLoggedIn, flags) {
        buildList {
            if (hasProfile) {
                add(
                    MenuEntry(
                        key = KEY_EDIT,
                        label = "Edit profile",
                        leadingIcon = Icons.Filled.Edit,
                        supportingText = if (flags.supportingText) "Edit your profile info" else null,
                        trailingIcon = if (flags.trailingIcon) Icons.AutoMirrored.Filled.KeyboardArrowRight else null,
                        onClick = onEdit
                    )
                )
            }
            if (isLoggedIn) {
                add(
                    MenuEntry(
                        key = KEY_PHOTO,
                        label = "Change photo",
                        leadingIcon = Icons.Filled.PhotoCamera,
                        supportingText = if (flags.supportingText) "Update your avatar" else null,
                        trailingIcon = if (flags.trailingIcon) Icons.AutoMirrored.Filled.KeyboardArrowRight else null,
                        onClick = onChangePhoto
                    )
                )
            }
            add(
                MenuEntry(
                    key = KEY_PRIVACY,
                    label = "Privacy",
                    leadingIcon = Icons.Filled.PrivacyTip,
                    supportingText = if (flags.supportingText) "Permissions & policies" else null,
                    trailingIcon = if (flags.trailingIcon) Icons.AutoMirrored.Filled.KeyboardArrowRight else null,
                    badgeText = if (flags.badge) "New" else null,
                    onClick = onPrivacy
                )
            )
        }
    }

    // Outer container: same min width behavior you want
    androidx.compose.material3.DropdownMenuPopup(
        expanded = true,
        onDismissRequest = onDismiss,
        offset = androidx.compose.ui.unit.DpOffset(0.dp, 0.dp),
        modifier = modifier
    ) {
        DropdownMenuGroup(
            shapes = MenuDefaults.groupShape(index = 0, count = totalGroups),
            containerColor = containerColor,
        ) {
            topItems.forEachIndexed { index, item ->
                val hasPhoto = topItems.any { it.key == KEY_PHOTO }

                val itemShapes = when (item.key) {
                    KEY_PRIVACY if topItems.size == 1 ->
                        MenuDefaults.itemShape(index = 0, count = 2)
                    KEY_PRIVACY if hasPhoto ->
                        MenuDefaults.itemShape(index = 1, count = 3)
                    else -> MenuDefaults.itemShape(index = index, count = topItems.size)
                }

                ExpressiveMenuItem(
                    entry = item,
                    selected = flags.persistentSelection && (selectedKey == item.key),
                    shapes = itemShapes,
                    flags = flags,
                    scales = scales
                )
            }
        }

        Spacer(Modifier.height(MenuDefaults.GroupSpacing))

        val authEntry = remember(isLoggedIn, flags.supportingText) {
            MenuEntry(
                key = KEY_AUTH,
                label = if (isLoggedIn) "Log out" else "Log in",
                leadingIcon = if (isLoggedIn) Icons.AutoMirrored.Filled.Logout else Icons.AutoMirrored.Filled.Login,
                supportingText = if (flags.supportingText) {
                    if (isLoggedIn) "Sign out of your account" else "Sign in to continue"
                } else null,
                onClick = onLoginLogout
            )
        }

        DropdownMenuGroup(
            shapes = MenuDefaults.groupShape(index = 1, count = totalGroups),
            containerColor = MenuDefaults.groupVibrantContainerColor,
        ) {
            ExpressiveMenuItem(
                entry = authEntry,
                selected = flags.persistentSelection && (selectedKey == KEY_AUTH),
                shapes = MenuDefaults.itemShape(index = 1, count = 2),
                flags = flags,
                scales = scales,
                colors = MenuDefaults.itemColors(
                    leadingIconColor = MaterialTheme.colorScheme.primary,
                )
            )
        }
    }
}

// ---------- Rendering helpers ----------

private data class MenuEntry(
    val key: String,
    val label: String,
    val leadingIcon: ImageVector,
    val supportingText: String? = null,
    val trailingIcon: ImageVector? = null,
    val badgeText: String? = null,
    val trailingText: String? = null,
    val onClick: () -> Unit,
)

@Composable
private fun ExpressiveMenuItem(
    entry: MenuEntry,
    selected: Boolean,
    shapes: MenuItemShapes,
    flags: MenuFeatureFlags,
    scales: UiScales,
    colors: MenuItemColors? = null,
) {
    val bodyS = scales.body
    val labelS = scales.label

    val leadingIconSize = MenuIconSize.us(bodyS)
    val trailingIconSize = 18.dp.us(bodyS)

    val labelStyle = MaterialTheme.typography.bodyLarge
    val supportingStyle = MaterialTheme.typography.bodySmall
    val trailingTextStyle = MaterialTheme.typography.labelSmall

    val trailingContent: (@Composable () -> Unit)? = remember(flags, entry, bodyS, labelS) {
        val hasTrailing = (flags.trailingIcon && entry.trailingIcon != null) ||
                (flags.badge && entry.badgeText != null) ||
                (flags.trailingText && entry.trailingText != null)

        if (!hasTrailing) return@remember null

        {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (flags.trailingText && entry.trailingText != null) {
                    Text(
                        text = entry.trailingText,
                        style = trailingTextStyle.copy(
                            fontSize = trailingTextStyle.fontSize.us(labelS)
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp.us(bodyS)))
                }

                if (flags.badge && entry.badgeText != null) {
                    BadgePill(text = entry.badgeText, scales = scales)
                }

                if (flags.trailingIcon && entry.trailingIcon != null) {
                    if (flags.badge || flags.trailingText) Spacer(Modifier.width(8.dp.us(bodyS)))
                    Icon(
                        imageVector = entry.trailingIcon,
                        contentDescription = null,
                        modifier = Modifier.size(trailingIconSize)
                    )
                }
            }
        }
    }

    DropdownMenuItem(
        selected = selected,
        onClick = entry.onClick,
        shapes = shapes,
        colors = colors ?: MenuDefaults.itemColors(),
        leadingIcon = {
            Icon(
                imageVector = entry.leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(leadingIconSize)
            )
        },
        text = {
            if (flags.supportingText && entry.supportingText != null) {
                MenuDefaults.LabelWithSupportingText(
                    supportingText = {
                        Text(
                            text = entry.supportingText,
                            style = supportingStyle.copy(
                                fontSize = supportingStyle.fontSize.us(labelS)
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                ) {
                    Text(
                        text = entry.label,
                        style = labelStyle.copy(
                            fontSize = labelStyle.fontSize.us(bodyS)
                        )
                    )
                }
            } else {
                Text(
                    text = entry.label,
                    style = labelStyle.copy(
                        fontSize = labelStyle.fontSize.us(bodyS)
                    )
                )
            }
        },
        trailingIcon = trailingContent
    )
}

@Composable
private fun BadgePill(text: String, scales: UiScales) {
    val badgeStyle = MaterialTheme.typography.labelSmall

    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(
            text = text,
            style = badgeStyle.copy(
                fontSize = badgeStyle.fontSize.us(scales.label)
            ),
            modifier = Modifier.padding(
                horizontal = 6.dp.us(scales.body),
                vertical = 2.dp.us(scales.body)
            ),
        )
    }
}
