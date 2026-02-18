@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.flights.studio

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun LiquidGlassGalleryScreen(
    imageUrls: List<String>,
    startIndex: Int = 0,
) {
    val isDark = isSystemInDarkTheme()
    val view = LocalView.current
    val ctx = view.context
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var slideshowOn by remember { mutableStateOf(false) }
    var slideshowJob by remember { mutableStateOf<Job?>(null) }
    fun tick() {
        if (!view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    // Backdrop provider (liquid-glass)
    val bottomTabsBackdrop = rememberLayerBackdrop()

    // Pager
    val safeCount = imageUrls.size.coerceAtLeast(1)
    val initial = startIndex.coerceIn(0, (imageUrls.size - 1).coerceAtLeast(0))
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = initial,
        pageCount = { safeCount }
    )
    val fling = androidx.compose.foundation.pager.PagerDefaults.flingBehavior(state = pagerState)

    // Glass styles
    val uiOnGlass = Color.White

    val pillGlass = Modifier.drawBackdrop(
        backdrop = bottomTabsBackdrop,
        shape = { CircleShape },
        highlight = {
            if (isDark) {
                Highlight(
                    width = 0.45.dp,
                    blurRadius = 1.6.dp,
                    alpha = 0.50f,
                    style = HighlightStyle.Plain
                )
            } else {
                Highlight(
                    width = 0.30.dp,
                    blurRadius = 1.0.dp,
                    alpha = 0.95f,
                    style = HighlightStyle.Plain
                )
            }
        },
        shadow = null,
        effects = {
            vibrancy()
            blur(1.dp.toPx())
            lens(
                refractionHeight = 8.dp.toPx(),
                refractionAmount = 24.dp.toPx(),
                depthEffect = true,
                chromaticAberration = false
            )
        },
        onDrawSurface = { drawRect(Color.Black.copy(alpha = 0.26f)) }
    )

    val iconButtonGlass = Modifier.drawBackdrop(
        backdrop = bottomTabsBackdrop,
        shape = { CircleShape },
        highlight = {
            if (isDark) {
                Highlight(
                    width = 0.45.dp,
                    blurRadius = 1.6.dp,
                    alpha = 0.50f,
                    style = HighlightStyle.Plain
                )
            } else {
                Highlight(
                    width = 0.30.dp,
                    blurRadius = 1.0.dp,
                    alpha = 0.95f,
                    style = HighlightStyle.Plain
                )
            }
        },
        shadow = null,
        effects = {
            vibrancy()
            blur(1.dp.toPx())
            lens(
                refractionHeight =8.dp.toPx(),
                refractionAmount = 24.dp.toPx(),
                depthEffect = true,
                chromaticAberration = false
            )
        },
        onDrawSurface = { drawRect(Color.Black.copy(alpha = 0.26f)) }
    )

    // Menu state + anchor bounds
    var menuOpen by remember { mutableStateOf(false) }
    var menuAnchorBounds by remember { mutableStateOf<IntRect?>(null) }

    // Info bottom sheet state
    var infoOpen by remember { mutableStateOf(false) }
    var infoLoading by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf<ImageInfo?>(null) }

    val currentIndex = pagerState.currentPage.coerceIn(0, (imageUrls.size - 1).coerceAtLeast(0))
    val currentModel = imageUrls.getOrNull(currentIndex).orEmpty()

    // Load info when opening sheet (or when page changes while open)
    LaunchedEffect(infoOpen, currentModel) {
        if (!infoOpen) return@LaunchedEffect
        infoLoading = true
        info = loadImageInfo(ctx, currentModel)
        infoLoading = false
    }

    Box(Modifier.fillMaxSize()) {
        // Provider + pager
        Box(
            Modifier
                .layerBackdrop(bottomTabsBackdrop)
                .fillMaxSize()
        ) {
            androidx.compose.foundation.pager.HorizontalPager(
                state = pagerState,
                flingBehavior = fling,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val index = if (imageUrls.isEmpty()) 0 else page.coerceIn(0, imageUrls.lastIndex)
                val url = imageUrls.getOrNull(index).orEmpty()

                ZoomableImageContent(
                    model = url,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // ✅ FLOATING COUNTER (top-left) — NO TOP BAR
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .safeDrawingPadding()
                .padding(start = 16.dp, top = 12.dp)
                .then(pillGlass)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            val current = (pagerState.currentPage + 1)
                .coerceAtMost(imageUrls.size.coerceAtLeast(1))
            Text(
                text = "$current / ${imageUrls.size.coerceAtLeast(1)}",
                color = uiOnGlass,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
            )
        }

        // ✅ FLOATING MENU (top-right)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .safeDrawingPadding()
                .padding(end = 16.dp, top = 12.dp)
                .size(48.dp)
                .then(iconButtonGlass)
                .onGloballyPositioned { coords ->
                    menuAnchorBounds = coords.boundsInWindow().toIntRect()
                }
                .clickableNoRipple {
                    tick()
                    menuOpen = true
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.paint(
                    painterResource(R.drawable.baseline_menu_24),
                    colorFilter = ColorFilter.tint(uiOnGlass)
                )
            )
        }

        // ✅ ANCHORED MENU
        val hasModel = currentModel.isNotBlank()

        GalleryExpressiveMenuAnchored(
            expanded = menuOpen,
            onDismiss = { menuOpen = false },
            anchorBounds = menuAnchorBounds,
            shareEnabled = hasModel,
            infoEnabled = hasModel,
            copyEnabled = hasModel,
            openEnabled = hasModel,
            slideshowOn = slideshowOn,
            onShare = {
                menuOpen = false
                tick()
                shareFromGalleryModel(ctx, currentModel)
            },
            onInfo = {
                menuOpen = false
                tick()
                infoOpen = true
            },
            onCopy = {
                menuOpen = false
                tick()
                copyPhotoToClipboard(ctx, currentModel)
            },
            onOpenWith = {
                menuOpen = false
                tick()
                openWith(ctx, currentModel)
            },
            onToggleSlideshow = {
                tick()
                slideshowOn = !slideshowOn

                slideshowJob?.cancel()
                slideshowJob = null

                if (slideshowOn && imageUrls.size > 1) {
                    slideshowJob = scope.launch {
                        while (true) {
                            delay(2500) // speed
                            val next = (pagerState.currentPage + 1) % safeCount
                            pagerState.animateScrollToPage(next)
                        }
                    }
                }
            },
        )


        // Bottom bar
        BottomProgressiveBlurStrip(
            backdrop = bottomTabsBackdrop,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val textShadow = Shadow(
                color = Color.Black.copy(alpha = 1f),
                offset = Offset(0f, 3f),
                blurRadius = 2f
            )

            Text(
                text = "Gallery mode • Swipe to navigate",
                color = uiOnGlass,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    shadow = textShadow
                ),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
            )
        }
    }

    // ✅ INFO BOTTOM SHEET (thumbnail + clean info)
    if (infoOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val isDark = isSystemInDarkTheme()

        val sheetTint = if (isDark) {
            Color.Black.copy(alpha = 0.24f)
        } else {
            Color.Black.copy(alpha = 0.12f)
        }

        ModalBottomSheet(
            onDismissRequest = { infoOpen = false },
            sheetState = sheetState,
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(28.dp),
            dragHandle = null
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 20.dp)
                    .drawBackdrop(
                        backdrop = bottomTabsBackdrop,
                        shape = { RoundedCornerShape(28.dp) },
                        highlight = {
                            if (isDark) {
                                Highlight(
                                    width = 0.45.dp,
                                    blurRadius = 1.dp,
                                    alpha = 0.50f,
                                    style = HighlightStyle.Plain
                                )
                            } else {
                                Highlight(
                                    width = 0.30.dp,
                                    blurRadius = 1.0.dp,
                                    alpha = 0.35f,
                                    style = HighlightStyle.Plain // very subtle
                                )
                            }
                        },
                        effects = {
                            vibrancy()
                            blur(8.dp.toPx())
                            lens(
                                refractionHeight = 14.dp.toPx(),
                                refractionAmount = 60.dp.toPx(),
                                depthEffect = true,
                                chromaticAberration = true
                            )
                        },
                        onDrawSurface = { drawRect(sheetTint) }
                    )
            ) {
                ImageInfoSheetContentStyled(
                    model = currentModel,
                    loading = infoLoading,
                    info = info,
                    onShare = { shareFromGalleryModel(ctx, currentModel) },
                    onClose = { infoOpen = false },
                )
            }
        }
    }
}

/** ======= EXPRESSIVE MENU (anchored) ======= */
@Composable
private fun GalleryExpressiveMenuAnchored(
    expanded: Boolean,
    onDismiss: () -> Unit,
    anchorBounds: IntRect?,
    shareEnabled: Boolean,
    infoEnabled: Boolean,
    copyEnabled: Boolean,
    openEnabled: Boolean,
    slideshowOn: Boolean,
    onShare: () -> Unit,
    onInfo: () -> Unit,
    onCopy: () -> Unit,
    onOpenWith: () -> Unit,
    onToggleSlideshow: () -> Unit,
) {
    if (!expanded || anchorBounds == null) return
    val density = LocalDensity.current

    val anchorRect = anchorBounds // <-- this is your menuAnchorBounds from onGloballyPositioned

    val positionProvider = remember(anchorRect, density) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect, // IGNORE (Compose might pass full-window)
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset {
                val marginPx = with(density) { 8.dp.roundToPx() }
                val liftPx = with(density) { 70.dp.roundToPx() } // tweak

                var x = anchorRect.right - popupContentSize.width
                x = x.coerceIn(marginPx, windowSize.width - popupContentSize.width - marginPx)

                // default: below anchor, but lifted
                var y = anchorRect.bottom + marginPx - liftPx

                // if it would go off bottom, place above anchor
                if (y + popupContentSize.height > windowSize.height - marginPx) {
                    y = anchorRect.top - popupContentSize.height - marginPx - liftPx
                }

                // allow up to top
                y = y.coerceIn(
                    0,
                    windowSize.height - popupContentSize.height - marginPx
                )

                return IntOffset(x, y)
            }
        }
    }


    // Build a dynamic list so shapes indices are always correct.
    data class Action(
        val title: String,
        val enabled: Boolean,
        val onClick: () -> Unit,
        val leading: @Composable () -> Unit,
        val trailing: (@Composable () -> Unit)? = null,
        val danger: Boolean = false,
    )

    val actions = remember(
        shareEnabled, infoEnabled, copyEnabled, openEnabled,
        slideshowOn, onShare, onInfo, onCopy, onOpenWith, onToggleSlideshow
    ) {
        buildList {
            add(
                Action(
                    title = "Share",
                    enabled = shareEnabled,
                    onClick = onShare,
                    leading = { Icon(Icons.Filled.Share, contentDescription = null) },
                    trailing = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                )
            )
            add(
                Action(
                    title = "Info",
                    enabled = infoEnabled,
                    onClick = onInfo,
                    leading = { Icon(Icons.Filled.Info, contentDescription = null) },
                    trailing = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                )
            )
            add(
                Action(
                    title = "Copy photo",
                    enabled = copyEnabled,
                    onClick = onCopy,
                    leading = { Icon(Icons.AutoMirrored.Filled.TextSnippet, contentDescription = null) }
                )
            )
            add(
                Action(
                    title = "Open with…",
                    enabled = openEnabled,
                    onClick = onOpenWith,
                    leading = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) }
                )
            )
            add(
                Action(
                    title = if (slideshowOn) "Slideshow: On" else "Slideshow: Off",
                    enabled = true,
                    onClick = onToggleSlideshow,
                    leading = { Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = null) },
                    trailing = if (slideshowOn) {
                        { Icon(Icons.Filled.Info, null) } // replace with Check icon if you want
                    } else null
                )
            )
        }
    }

    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuPopup(
            expanded = true,
            onDismissRequest = onDismiss,
            offset = DpOffset(0.dp, 0.dp),
        ) {
            val itemCount = actions.size

            DropdownMenuGroup(
                shapes = MenuDefaults.groupShape(index = 0, count = 1),
                containerColor = MenuDefaults.groupVibrantContainerColor
            ) {
                actions.forEachIndexed { index, a ->
                    val colors = if (a.danger) {
                        MenuDefaults.itemColors(
                            textColor = MaterialTheme.colorScheme.error,
                            leadingIconColor = MaterialTheme.colorScheme.error,
                            trailingIconColor = MaterialTheme.colorScheme.error
                        )
                    } else {
                        MenuDefaults.itemColors()
                    }

                    DropdownMenuItem(
                        selected = false,
                        enabled = a.enabled,
                        onClick = {
                            a.onClick()
                            onDismiss()
                        },
                        text = { Text(a.title) },
                        shapes = MenuDefaults.itemShape(index = index, count = itemCount),
                        colors = colors,
                        leadingIcon = a.leading,
                        trailingIcon = a.trailing
                    )
                }
            }
        }
    }
}


/** ======= INFO SHEET UI ======= */
@Composable
private fun ImageInfoSheetContentStyled(
    model: String,
    loading: Boolean,
    info: ImageInfo?,
    onShare: () -> Unit,
    onClose: () -> Unit,
) {
    val isDark = isSystemInDarkTheme()

    val cardFill = if (isDark) {
        Color.White.copy(alpha = 0.18f)
    } else {
        Color.Black.copy(alpha = 0.18f)
    }

    Column(
        Modifier
            .fillMaxWidth()
            .safeDrawingPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Photo info",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onPrimary,
            )

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = cardFill,
                modifier = Modifier.clickableNoRipple { onShare() }
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = "Share",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }


            Spacer(Modifier.width(10.dp))
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = cardFill,
                modifier = Modifier.clickableNoRipple { onClose() }
            ) {
                Text(
                    text = "Close",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

        }

        Spacer(Modifier.height(14.dp))

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = cardFill,
            modifier = Modifier.fillMaxWidth()
         ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = if (isDark) 0.18f else 0.055f),
                    modifier = Modifier.size(60.dp)
                ) {
                    AsyncImage(
                        model = model,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        text = info?.displayName ?: "Photo",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1
                    )
                    Text(
                        text = when {
                            loading -> "Loading details…"
                            info == null -> "No extra details available"
                            else -> (info.mimeType ?: "Unknown type")

                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = cardFill,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                if (loading) {
                    Text(
                        text = "Reading metadata…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                } else {
                    InfoRowStyled("Size", info?.sizeBytes?.let { formatBytes(it) } ?: "—")
                    DividerHairline()
                    InfoRowStyled("Dimensions", info?.dimensions ?: "—")
                    DividerHairline()
                    InfoRowStyled("Modified", info?.lastModified ?: "—")
                    DividerHairline()

                    val source = info?.sourceLabel ?: friendlySourceLabel(model)
                    InfoRowStyled("Source", source)
                }
            }
        }

        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun DividerHairline() {
    Surface(
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
    ) {}
}

@Composable
private fun InfoRowStyled(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.width(110.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onPrimary,
            maxLines = 2
        )
    }
}

private data class ImageInfo(
    val displayName: String?,
    val mimeType: String?,
    val sizeBytes: Long?,
    val dimensions: String?,
    val lastModified: String?,
    val sourceLabel: String?,
)

/** Reads info for content:// / file://, else returns basic info for url/text. */
/** Reads info for content:// / file://, else returns basic info for url/text. */
private suspend fun loadImageInfo(context: Context, model: String): ImageInfo? =
    withContext(Dispatchers.IO) {
        if (model.isBlank()) return@withContext null

        runCatching {
            when {
                model.startsWith("content://") -> loadFromContentUri(context, model.toUri())
                model.startsWith("file://") -> loadFromFileUri(model.toUri())
                else -> ImageInfo(
                    displayName = safeLastPathSegment(model),
                    mimeType = guessMimeFromName(safeLastPathSegment(model)),
                    sizeBytes = null,
                    dimensions = null,
                    lastModified = null,
                    sourceLabel = friendlySourceLabel(model)
                )
            }
        }.getOrNull() // ✅ if anything throws, return null instead of crashing
    }


private fun loadFromContentUri(context: Context, uri: Uri): ImageInfo? = runCatching {
    val cr = context.contentResolver

    var name: String? = null
    var size: Long? = null

    cr.query(uri, null, null, null, null)?.use {
        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
        if (it.moveToFirst()) {
            if (nameIndex >= 0) name = it.getString(nameIndex)
            if (sizeIndex >= 0) size = it.getLong(sizeIndex)
        }
    }

    val mime = runCatching { cr.getType(uri) }.getOrNull()
    val dims = runCatching { decodeDimensions(cr, uri) }.getOrNull()
    val modified = runCatching { queryLastModified(cr, uri) }.getOrNull()

    ImageInfo(
        displayName = name,
        mimeType = mime,
        sizeBytes = size,
        dimensions = dims,
        lastModified = modified,
        sourceLabel = "Local photo"
    )
}.getOrNull()


private fun loadFromFileUri(uri: Uri): ImageInfo? {
    val file = runCatching { File(uri.path ?: return null) }.getOrNull() ?: return null
    val name = file.name
    val size = file.length()
    val modified = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.US)
        .format(Date(file.lastModified()))

    val dims = runCatching {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, opts)
        if (opts.outWidth > 0 && opts.outHeight > 0) "${opts.outWidth} × ${opts.outHeight}" else null
    }.getOrNull()

    return ImageInfo(
        displayName = name,
        mimeType = guessMimeFromName(name),
        sizeBytes = size,
        dimensions = dims,
        lastModified = modified,
        sourceLabel = "Device file"
    )
}

private fun decodeDimensions(cr: ContentResolver, uri: Uri): String? {
    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    cr.openInputStream(uri)?.use { input ->
        BitmapFactory.decodeStream(input, null, opts)
    }
    if (opts.outWidth <= 0 || opts.outHeight <= 0) return null
    return "${opts.outWidth} × ${opts.outHeight}"
}

private fun queryLastModified(cr: ContentResolver, uri: Uri): String? {
    val cursor = cr.query(uri, arrayOf("last_modified", "date_modified"), null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            for (i in 0 until it.columnCount) {
                val v = runCatching { it.getLong(i) }.getOrNull() ?: continue
                if (v > 0L) {
                    val millis = if (v < 10_000_000_000L) v * 1000 else v
                    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.US)
                        .format(Date(millis))
                }
            }
        }
    }
    return null
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val kb = 1024.0
    val mb = kb * 1024.0
    val gb = mb * 1024.0

    return when {
        bytes >= gb -> String.format(Locale.US, "%.2f GB", bytes / gb)
        bytes >= mb -> String.format(Locale.US, "%.2f MB", bytes / mb)
        bytes >= kb -> String.format(Locale.US, "%.0f KB", bytes / kb)
        else -> String.format(Locale.US, "%d B", bytes)
    }
}

private fun safeLastPathSegment(s: String): String {
    return runCatching {
        val u = s.toUri()
        u.lastPathSegment ?: s.substringAfterLast('/').ifBlank { "Photo" }
    }.getOrDefault("Photo")
}

private fun guessMimeFromName(name: String): String? {
    val lower = name.lowercase(Locale.US)
    return when {
        lower.endsWith(".png") -> "image/png"
        lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> "image/jpeg"
        lower.endsWith(".webp") -> "image/webp"
        lower.endsWith(".gif") -> "image/gif"
        else -> null
    }
}

private fun friendlySourceLabel(model: String): String {
    return when {
        model.startsWith("content://") -> "Local photo"
        model.startsWith("file://") -> "Device file"
        model.startsWith("http://") || model.startsWith("https://") -> "Online link"
        else -> "Unknown"
    }
}

private fun shareFromGalleryModel(context: Context, model: String) {
    if (model.isBlank()) return

    if (model.startsWith("content://") || model.startsWith("file://")) {
        shareImageUri(context, model.toUri())
        return
    }

    val share = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, model)
    }
    context.startActivity(Intent.createChooser(share, "Share"))
}


fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    composed {
        val interaction = remember { MutableInteractionSource() }
        clickable(indication = null, interactionSource = interaction, onClick = onClick)
    }

private fun androidx.compose.ui.geometry.Rect.toIntRect(): IntRect =
    IntRect(
        left = left.roundToInt(),
        top = top.roundToInt(),
        right = right.roundToInt(),
        bottom = bottom.roundToInt()
    )


@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Preview(
    name = "Gallery - Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF000000
)
@Composable
private fun LiquidGlassGalleryScreenPreviewDark() {
    MaterialTheme {
        LiquidGlassGalleryScreen(
            imageUrls = listOf(
                "https://picsum.photos/1200/1800?1",
                "https://picsum.photos/1200/1800?2",
                "https://picsum.photos/1200/1800?3"
            ),
            startIndex = 0
        )
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Preview(
    name = "Gallery - Light",
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    showBackground = true,
    backgroundColor = 0xFFFFFFFF
)
@Composable
private fun LiquidGlassGalleryScreenPreviewLight() {
    MaterialTheme {
        LiquidGlassGalleryScreen(
            imageUrls = listOf(
                "https://picsum.photos/1200/1800?1",
                "https://picsum.photos/1200/1800?2",
                "https://picsum.photos/1200/1800?3"
            ),
            startIndex = 1
        )
    }
}