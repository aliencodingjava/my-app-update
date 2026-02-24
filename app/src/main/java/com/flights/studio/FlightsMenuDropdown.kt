@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.flights.studio

import android.content.Intent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.delay

@Composable
fun FlightsMenuDropdown(
    expanded: Boolean,
    onDismiss: () -> Unit,
    notesCount: Int,
    contactsCount: Int
) {
    val context = LocalContext.current

    // Track if items should be revealed (for stagger animation)
    var itemsRevealed by remember { mutableStateOf(false) }

    LaunchedEffect(expanded) {
        if (expanded) {
            // Reset when opening
            itemsRevealed = false
            // Delay before revealing items (iOS-like timing)
            delay(60)
            itemsRevealed = true
        } else {
            itemsRevealed = false
        }
    }

    DropdownMenuPopup(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {

        // ---- iOS 18/26 STYLE ANIMATION ----

        // Menu container scale (spring with iOS-like bounce)
        val scale by animateFloatAsState(
            targetValue = if (expanded) 1f else 0.88f,
            animationSpec = spring(
                dampingRatio = 0.78f,   // More bouncy (iOS feel)
                stiffness = 380f        // Quick response
            ),
            label = "menuScale"
        )

        // Menu container alpha
        val alpha by animateFloatAsState(
            targetValue = if (expanded) 1f else 0f,
            animationSpec = tween(
                durationMillis = 200,
                easing = androidx.compose.animation.core.FastOutSlowInEasing
            ),
            label = "menuAlpha"
        )

        // Subtle Y translation (drops down)
        val translateY by animateFloatAsState(
            targetValue = if (expanded) 0f else -12f,
            animationSpec = spring(
                dampingRatio = 0.82f,
                stiffness = 400f
            ),
            label = "menuTranslate"
        )

        Box(
            modifier = Modifier.graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
                translationY = translateY

                // Grows from top-right (like iOS contextual menu)
                transformOrigin = TransformOrigin(1f, 0f)
            }
        ) {

            val groupCount = 1

            DropdownMenuGroup(
                shapes = MenuDefaults.groupShape(0, groupCount),
                containerColor = MenuDefaults.groupStandardContainerColor,
                tonalElevation = MenuDefaults.TonalElevation,
                shadowElevation = MenuDefaults.ShadowElevation
            ) {

                val itemCount = 2

                // ---- ITEM 1: Notes (staggered animation) ----
                val item1Scale by animateFloatAsState(
                    targetValue = if (itemsRevealed) 1f else 0.90f,
                    animationSpec = spring(
                        dampingRatio = 0.75f,
                        stiffness = 350f
                    ),
                    label = "item1Scale"
                )

                val item1Alpha by animateFloatAsState(
                    targetValue = if (itemsRevealed) 1f else 0f,
                    animationSpec = tween(
                        durationMillis = 180,
                        delayMillis = 0  // First item appears immediately
                    ),
                    label = "item1Alpha"
                )

                val item1TranslateX by animateFloatAsState(
                    targetValue = if (itemsRevealed) 0f else -20f,
                    animationSpec = spring(
                        dampingRatio = 0.75f,
                        stiffness = 350f
                    ),
                    label = "item1TranslateX"
                )

                DropdownMenuItem(
                    selected = false,
                    onClick = {
                        onDismiss()
                        context.startActivity(
                            Intent(context, AllNotesActivity::class.java)
                        )
                    },
                    shapes = MenuDefaults.itemShape(0, itemCount),
                    text = { Text("Notes") },
                    leadingIcon = {
                        Icon(
                            painterResource(R.drawable.ic_oui_notes),
                            contentDescription = null,
                            modifier = Modifier.size(MenuDefaults.LeadingIconSize)
                        )
                    },
                    trailingIcon = { CountBadge(notesCount) },
                    modifier = Modifier.graphicsLayer {
                        scaleX = item1Scale
                        scaleY = item1Scale
                        this.alpha = item1Alpha
                        translationX = item1TranslateX
                    }
                )

                // ---- ITEM 2: Contacts (staggered with delay) ----
                val item2Scale by animateFloatAsState(
                    targetValue = if (itemsRevealed) 1f else 0.90f,
                    animationSpec = spring(
                        dampingRatio = 0.75f,
                        stiffness = 350f
                    ),
                    label = "item2Scale"
                )

                val item2Alpha by animateFloatAsState(
                    targetValue = if (itemsRevealed) 1f else 0f,
                    animationSpec = tween(
                        durationMillis = 180,
                        delayMillis = 40  // 40ms stagger delay (iOS timing)
                    ),
                    label = "item2Alpha"
                )

                val item2TranslateX by animateFloatAsState(
                    targetValue = if (itemsRevealed) 0f else -20f,
                    animationSpec = spring(
                        dampingRatio = 0.75f,
                        stiffness = 350f
                    ),
                    label = "item2TranslateX"
                )

                DropdownMenuItem(
                    selected = false,
                    onClick = {
                        onDismiss()
                        context.startActivity(
                            Intent(context, AllContactsActivity::class.java)
                        )
                    },
                    shapes = MenuDefaults.itemShape(1, itemCount),
                    text = { Text("Contacts") },
                    leadingIcon = {
                        Icon(
                            painterResource(
                                R.drawable.contact_page_24dp_ffffff_fill1_wght400_grad0_opsz24
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(MenuDefaults.LeadingIconSize)
                        )
                    },
                    trailingIcon = { CountBadge(contactsCount) },
                    modifier = Modifier.graphicsLayer {
                        scaleX = item2Scale
                        scaleY = item2Scale
                        this.alpha = item2Alpha
                        translationX = item2TranslateX
                    }
                )
            }
        }
    }
}

@Composable
fun CountBadge(
    count: Int,
    maxCount: Int = 99
) {
    if (count <= 0) return

    val display = if (count > maxCount) "$maxCount+" else count.toString()

    Badge(
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Text(
            text = display,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
