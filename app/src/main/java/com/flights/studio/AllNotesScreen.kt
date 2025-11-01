package com.flights.studio

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


@Composable
fun themedIconTint(): Color {
    val isDark = isSystemInDarkTheme()
    // brighter in dark, slightly softer in light
    return if (isDark) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)
    }
}


@Composable
fun AllNotesScreen(
    notesAdapter: NotesAdapter,
    notes: List<String>,
    notesSize: Int,
    onAddNote: () -> Unit,
    onNavItemClick: (Int) -> Unit,
    isDeleteVisible: Boolean
) {
    val iconTint = themedIconTint()

    Box(
        Modifier
            .fillMaxSize()
            .background(colorResource(R.color.box_qrcode))   // <- make the whole screen same color
    ) {


        Row(Modifier.fillMaxSize()) {

            // ── Navigation Rail ──
                NavigationRail(
                    modifier = Modifier
                        .width(80.dp)                    // <- fixed rail width
                        .fillMaxHeight()
                        .background(colorResource(R.color.box_qrcode)),
                    containerColor = Color.Transparent,
                    header = {}
                ) {
                    NavigationRailItem(
                        selected = false,
                        onClick = { onNavItemClick(R.id.nav_home) },
                        icon = {
                            Icon(
                                painterResource(R.drawable.ic_oui_home),
                                contentDescription = stringResource(R.string.mainhome),
                                tint = iconTint,
                                modifier = Modifier.size(30.dp) // ⬅️ icon size
                            )
                        },
                        label = null,
                        alwaysShowLabel = false
                    )
                    NavigationRailItem(
                        selected = false,
                        onClick = { onNavItemClick(R.id.nav_contacts) },
                        icon = {
                            Icon(
                                painterResource(R.drawable.contact_page_24dp_ffffff_fill1_wght400_grad0_opsz24),
                                contentDescription = stringResource(R.string.contacts),
                                tint = iconTint,
                                modifier = Modifier.size(30.dp)
                            )
                        },
                        label = null,
                        alwaysShowLabel = false
                    )
                    NavigationRailItem(
                        selected = false,
                        onClick = { onNavItemClick(R.id.nav_all_contacts) },
                        icon = {
                            Icon(
                                painterResource(R.drawable.groups_2_24dp_ffffff_fill1_wght400_grad0_opsz24),
                                contentDescription = stringResource(R.string.contacts),
                                tint = iconTint,
                                modifier = Modifier.size(30.dp)
                            )
                        },
                        label = null,
                        alwaysShowLabel = false
                    )
                    NavigationRailItem(
                        selected = false,
                        onClick = { onNavItemClick(R.id.nav_settings) },
                        icon = {
                            Icon(
                                painterResource(R.drawable.settings_account_box_24dp_ffffff_fill1_wght400_grad0_opsz24),
                                contentDescription = stringResource(R.string.nav_settings),
                                tint = iconTint,
                                modifier = Modifier.size(30.dp)
                            )
                        },
                        label = null,
                        alwaysShowLabel = false
                    )
                    NavigationRailItem(
                        selected = true,
                        onClick = { onNavItemClick(R.id.openAddNoteScreen) },
                        icon = {
                            Icon(
                                painterResource(R.drawable.book_24dp_ffffff_fill1_wght400_grad0_opsz24),
                                contentDescription = stringResource(R.string.nav_notes),
                                tint = colorResource(R.color.day_ground_base),
                                modifier = Modifier.size(30.dp)
                            )
                        },
                        label = null,
                        alwaysShowLabel = false
                    )
                    NavigationRailItem(
                        selected = false,
                        onClick = { onNavItemClick(R.id.action_search) },
                        icon = {
                            Icon(
                                painterResource(R.drawable.person_search_24dp_ffffff_fill1_wght400_grad0_opsz24),
                                contentDescription = stringResource(R.string.nav_search),
                                tint = iconTint,
                                modifier = Modifier.size(30.dp)
                            )
                        },
                        label = null,
                        alwaysShowLabel = false
                    )
                    androidx.compose.animation.AnimatedVisibility(visible = isDeleteVisible) {
                        NavigationRailItem(
                            selected = false,
                            onClick = { onNavItemClick(R.id.action_delete) },
                            icon = {
                                Icon(
                                    painterResource(R.drawable.delete_24dp_ffffff_fill1_wght400_grad0_opsz24),
                                    contentDescription = stringResource(R.string.nav_delete),
                                    tint = colorResource(R.color.red),
                                    modifier = Modifier.size(30.dp)
                                )
                            },
                            label = null,
                            alwaysShowLabel = false
                        )
                    }

                    Spacer(Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                            .offset(y = (-54).dp),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        FloatingActionButton(
                            onClick = onAddNote,
                            containerColor = colorResource(R.color.box_contact_expand)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.add_notes_24dp_ffffff_fill1_wght400_grad0_opsz24),
                                contentDescription = stringResource(R.string.add_note),
                                tint = Color.Red,
                                modifier = Modifier.size(30.dp) // FAB icon size
                            )
                        }

                        if (notesSize > 0) {
                            Badge(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 4.dp, y = (-4).dp)
                            ) {
                                Text(
                                    text = notesSize.toString(),
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }

                // ── Notes content ──
                val cardShape = RoundedCornerShape(12.dp)

                androidx.compose.material3.Card(
                    modifier = Modifier
                        .padding(end = 5.dp, bottom = 5.dp)  // like XML margins
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = cardShape,
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = colorResource(R.color.box_qrcode)
                    ),
                    elevation = androidx.compose.material3.CardDefaults.cardElevation(0.dp)
                ) {
                    Box(
                        Modifier
                            .clip(cardShape)
                            .background(colorResource(R.color.box_qrcode))
                            .fillMaxSize()
                    ) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { ctx ->
                                RecyclerView(ctx).apply {
                                    layoutManager = LinearLayoutManager(ctx)
                                    adapter = notesAdapter
                                }
                            },
                            update = { it.adapter = notesAdapter }
                        )

                        val isEmpty = notes.isEmpty()
                        androidx.compose.animation.AnimatedVisibility(
                            visible = isEmpty,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 10.dp, bottom = 78.dp),
                            enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
                            exit = slideOutHorizontally(targetOffsetX = { +it }) + fadeOut()
                        ) {
                            Text(
                                text = stringResource(R.string.add_your_first_note),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

            }
        }
    }
