@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.flights.studio

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

// ---------- Defaults ----------
private val MenuIconSize = 20.dp
private val MenuMinWidth = 240.dp

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
fun ProfileExpressiveMenuPopup(
    expanded: Boolean,
    onDismiss: () -> Unit,
    offset: DpOffset,
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
    val containerColor = if (flags.vibrant) {
        MenuDefaults.groupVibrantContainerColor
    } else {
        MenuDefaults.groupStandardContainerColor
    }

    DropdownMenuPopup(
        expanded = expanded,
        onDismissRequest = onDismiss,
        offset = offset,
        modifier = modifier.widthIn(min = MenuMinWidth),
    ) {
        // Use groupCount = 2 because we have two DropdownMenuGroups
        val totalGroups = 2

        // ----- Group 0: Profile Actions -----
        val topItems = remember(hasProfile, isLoggedIn, flags) {
            buildList {
                if (hasProfile) {
                    add(MenuEntry(
                        key = KEY_EDIT,
                        label = "Edit profile",
                        leadingIcon = Icons.Filled.Edit,
                        supportingText = if (flags.supportingText) "Edit your profile info" else null,
                        trailingIcon = if (flags.trailingIcon) Icons.AutoMirrored.Filled.KeyboardArrowRight else null,
                        onClick = onEdit
                    ))
                }
                if (isLoggedIn) {
                    add(MenuEntry(
                        key = KEY_PHOTO,
                        label = "Change photo",
                        leadingIcon = Icons.Filled.PhotoCamera,
                        supportingText = if (flags.supportingText) "Update your avatar" else null,
                        trailingIcon = if (flags.trailingIcon) Icons.AutoMirrored.Filled.KeyboardArrowRight else null,
                        onClick = onChangePhoto
                    ))
                }
                add(MenuEntry(
                    key = KEY_PRIVACY,
                    label = "Privacy",
                    leadingIcon = Icons.Filled.PrivacyTip,
                    supportingText = if (flags.supportingText) "Permissions & policies" else null,
                    trailingIcon = if (flags.trailingIcon) Icons.AutoMirrored.Filled.KeyboardArrowRight else null,
                    badgeText = if (flags.badge) "New" else null,
                    onClick = onPrivacy
                ))
            }
        }

        DropdownMenuGroup(
            shapes = MenuDefaults.groupShape(index = 0, count = totalGroups),
            containerColor = containerColor,
        ) {
            if (flags.groupLabels) {
                MenuDefaults.Label { Text("Account") }
                if (flags.groupDividers) {
                    HorizontalDivider(Modifier.padding(horizontal = MenuDefaults.HorizontalDividerPadding))
                }
            }

            topItems.forEachIndexed { index, item ->

                val hasPhoto = topItems.any { it.key == KEY_PHOTO }

                val itemShapes = when (// ✅ LOGGED OUT CASE: only Privacy in top group
                    // Make it look like "top item of 2" => top corners rounded only
                    item.key) {
                    KEY_PRIVACY if topItems.size == 1 ->
                        MenuDefaults.itemShape(index = 0, count = 2)

                    // ✅ LOGGED IN CASES where you want Privacy to be square like Change photo
                    KEY_PRIVACY if hasPhoto ->
                        MenuDefaults.itemShape(index = 1, count = 3) // middle => square

                    // ✅ Default behavior
                    else -> MenuDefaults.itemShape(index = index, count = topItems.size)
                }

                ExpressiveMenuItem(
                    entry = item,
                    selected = flags.persistentSelection && (selectedKey == item.key),
                    shapes = itemShapes,
                    flags = flags
                )
            }

        }


        Spacer(Modifier.height(MenuDefaults.GroupSpacing))

        // ----- Group 1: Auth Actions -----
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
                // Since there is only 1 item in this group, index is 0 and count is 1
                shapes = MenuDefaults.itemShape(index = 1, count = 2),
                flags = flags,
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
    colors: MenuItemColors? = null,
) {
    val trailingContent: (@Composable () -> Unit)? = remember(flags, entry) {
        val hasTrailing = (flags.trailingIcon && entry.trailingIcon != null) ||
                (flags.badge && entry.badgeText != null) ||
                (flags.trailingText && entry.trailingText != null)

        if (hasTrailing) {
            {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (flags.trailingText && entry.trailingText != null) {
                        Text(
                            text = entry.trailingText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(8.dp))
                    }

                    if (flags.badge && entry.badgeText != null) {
                        BadgePill(text = entry.badgeText)
                    }

                    if (flags.trailingIcon && entry.trailingIcon != null) {
                        if (flags.badge || flags.trailingText) Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = entry.trailingIcon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        } else null
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
                modifier = Modifier.size(MenuIconSize)
            )
        },
        text = {
            if (flags.supportingText && entry.supportingText != null) {
                // FIX: Pass the label as the trailing lambda, not a named 'label' parameter
                MenuDefaults.LabelWithSupportingText(
                    supportingText = {
                        Text(
                            text = entry.supportingText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                ) {
                    Text(
                        text = entry.label,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                Text(
                    text = entry.label,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        },
        trailingIcon = trailingContent
    )
}

@Composable
private fun BadgePill(text: String) {
    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}
