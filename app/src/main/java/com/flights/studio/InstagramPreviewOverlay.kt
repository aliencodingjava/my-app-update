package com.flights.studio

import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun InstagramPreviewOverlay(
    uri: Uri?,
    onDismiss: () -> Unit,
    onShare: (Uri) -> Unit,
    onShareWithNote: (Uri) -> Unit,
    onRotate: ((Uri) -> Unit)? = null,
    onReplace: ((Uri) -> Unit)? = null,
    onRemove: ((Uri) -> Unit)? = null,
) {
    if (uri == null) return

    val scale = remember { Animatable(0.96f) }
    val alpha = remember { Animatable(0f) }

    // container size in px
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current


    LaunchedEffect(uri) {
        launch { alpha.animateTo(1f, spring(stiffness = Spring.StiffnessMediumLow)) }
        launch { scale.animateTo(1f, spring(stiffness = Spring.StiffnessLow)) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .onSizeChanged { containerSize = it }

        ) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.55f * alpha.value))
                    .clickable { onDismiss() }
            )

            // MAIN CONTENT (no BoxWithConstraints)
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 72.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // if size not known yet, use safe fallback
                val screenH = if (containerSize.height == 0) 800.dp else with(density) { containerSize.height.toDp() }
                val maxWidthDp = if (containerSize.width == 0) 400.dp else with(density) { containerSize.width.toDp() }

                val menuWidthFraction = 0.56f

                // menu estimates
                val topItemsCount = 2 + (if (onReplace != null) 1 else 0) + (if (onRotate != null) 1 else 0)
                val bottomItemsCount = 1 + (if (onRemove != null) 1 else 0)
                val approxItemH = 48.dp
                val spacing = 8.dp
                val approxTopGroupH = topItemsCount * approxItemH + (topItemsCount - 1) * spacing + 12.dp
                val approxBottomGroupH = bottomItemsCount * approxItemH + (bottomItemsCount - 1) * spacing + 12.dp

                val desiredMenuTotal = approxTopGroupH + approxBottomGroupH + 12.dp
                val maxAllowedImageH = (screenH - desiredMenuTotal - 120.dp).coerceIn(170.dp, 340.dp)

                val widthBasedSquare = maxWidthDp.coerceAtMost(380.dp)
                val imageH = if (widthBasedSquare < maxAllowedImageH) widthBasedSquare else maxAllowedImageH

                // IMAGE
                Surface(

                        shape = RoundedCornerShape(24.dp),
                        color = Color.Black,
                        tonalElevation = 0.dp,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(imageH)
                            .graphicsLayer {
                                scaleX = scale.value
                                scaleY = scale.value
                            }
                            .clip(RoundedCornerShape(24.dp))
                            .clickable(enabled = false) {}
                    ) {
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // MENU (2 groups) — ✅ no DropdownMenuGroup
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Transparent,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .fillMaxWidth(menuWidthFraction)
                    ) {
                        val remainingForMenu = (screenH - imageH - 120.dp).coerceAtLeast(160.dp)
                        val maxTopGroupHeight =
                            (remainingForMenu - approxBottomGroupH - 12.dp)
                                .coerceIn(120.dp, 260.dp)

                        Column {
                            // GROUP 1 (scrolls if needed)
                            MenuGroupCard(
                                modifier = Modifier
                                    .heightIn(max = maxTopGroupHeight)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                MenuItem(
                                    text = "Share",
                                    icon = { Icon(Icons.Filled.Share, null) },
                                    onClick = { onShare(uri) }
                                )

                                MenuItem(
                                    text = "Share with note",
                                    icon = { Icon(Icons.AutoMirrored.Filled.TextSnippet, null) },
                                    onClick = { onShareWithNote(uri) }
                                )

                                if (onReplace != null) {
                                    MenuItem(
                                        text = "Replace photo",
                                        icon = { Icon(Icons.Filled.Edit, null) },
                                        onClick = { onReplace(uri) }
                                    )
                                }

                                if (onRotate != null) {
                                    MenuItem(
                                        text = "Rotate",
                                        icon = { Icon(Icons.AutoMirrored.Filled.RotateRight, null) },
                                        onClick = { onRotate(uri) }
                                    )
                                }
                            }

                            Spacer(Modifier.height(MenuDefaults.GroupSpacing))

                            // GROUP 2 (always visible)
                            MenuGroupCard {
                                if (onRemove != null) {
                                    MenuItem(
                                        text = "Remove",
                                        icon = { Icon(Icons.Filled.Delete, null) },
                                        onClick = { onRemove(uri) },
                                        danger = true
                                    )
                                }

                                MenuItem(
                                    text = "Close",
                                    icon = { Icon(Icons.Filled.Close, null) },
                                    onClick = onDismiss
                                )
                            }
                        }
                    }
                }
            }
        }
    }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MenuGroupCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MenuDefaults.groupVibrantContainerColor,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(MenuDefaults.GroupSpacing)
        ) {
            content()
        }
    }
}

@Composable
private fun MenuItem(
    text: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    danger: Boolean = false
) {
    val textColor = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    val iconColor = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 10.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompositionLocalProvider(
                LocalContentColor provides iconColor
            ) {
                icon()
            }
            Text(
                text = text,
                color = textColor,
                style = MaterialTheme.typography.titleSmall
            )
        }
    }
}

