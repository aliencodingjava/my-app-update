package com.flights.studio

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ImportContacts
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalPhone
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.SplitButtonShapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.flights.studio.com.flights.studio.BadgePillTopBar
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.File
import kotlin.math.roundToInt
import android.graphics.Color as AndroidColor

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun ContactsComposeList(
    contacts: List<AllContact>,
    totalContactsCount: Int = contacts.size,
    topSearchQuery: String,
    palettes: Map<String, ContactsAdapter.ColorPalette>,
    onFloatingSearchVisibleChange: (Boolean) -> Unit,
    onEditContact: (AllContact) -> Unit,
    onDeleteContact: (AllContact) -> Unit,
    onSwipeDeleteContact: (AllContact) -> Unit,
    onPaletteClick: (AllContact) -> Unit,
    onCallContact: (AllContact) -> Unit,
    onAddContact: () -> Unit,
    onImportContacts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val topBarBackdrop = rememberLayerBackdrop()
    val density = LocalDensity.current
    val thresholdPx = with(density) { 24.dp.roundToPx() }
    val contactsSnapshot by remember { derivedStateOf { contacts.toList() } }

    LaunchedEffect(listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }
            .map { (index, offset) -> index > 0 || offset > thresholdPx }
            .distinctUntilChanged()
            .collect(onFloatingSearchVisibleChange)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            ContactsTopAppBar(
                contactCount = contacts.size,
                filtering = topSearchQuery.isNotBlank(),
                backdrop = topBarBackdrop,
                onAddContact = onAddContact,
                onOpenSearch = { onFloatingSearchVisibleChange(true) },
                onImportContacts = onImportContacts
            )
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .layerBackdrop(topBarBackdrop)
        ) {
                val isDark = isSystemInDarkTheme()
                ProfileBackdropImageLayer(
                    modifier = Modifier.fillMaxSize(),
                    lightRes = R.drawable.light_grid_pattern,
                    darkRes = R.drawable.dark_grid_pattern,
                    imageAlpha = if (isDark) 1f else 0.8f,
                    scrimDark = 0f,
                    scrimLight = 0f
                )

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 6.dp,
                        end = 6.dp,
                        top = padding.calculateTopPadding() + 10.dp,
                        bottom = 12.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item(key = "contacts-count") {
                        Text(
                            text = if (topSearchQuery.isBlank()) {
                                "${contacts.size} contacts"
                            } else {
                                "Filtering contacts"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                            modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
                        )
                    }

                    items(
                        items = contactsSnapshot,
                        key = { it.id },
                        contentType = { "contact-row" }
                    ) { contact ->
                        ContactComposeRow(
                            contact = contact,
                            palette = palettes[contact.id],
                            onEditContact = onEditContact,
                            onDeleteContact = onDeleteContact,
                            onSwipeDeleteContact = onSwipeDeleteContact,
                            onPaletteClick = onPaletteClick,
                            onCallContact = onCallContact
                        )
                    }

                    item(key = "contacts-bottom-spacer") {
                        Column {
                            Spacer(Modifier.height(142.dp))
                            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                        }
                    }
                }

                var showWelcome by rememberSaveable { mutableStateOf(true) }
                ContactsWelcomeOnboardingOverlay(
                    visible = contactsSnapshot.isEmpty() && topSearchQuery.isBlank() && showWelcome,
                    onContinue = onAddContact,
                    onSecondary = { showWelcome = false }
                )
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ContactsTopAppBar(
    contactCount: Int,
    filtering: Boolean,
    backdrop: Backdrop,
    onAddContact: () -> Unit,
    onOpenSearch: () -> Unit,
    onImportContacts: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    val topBarShape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
    val scheme = MaterialTheme.colorScheme
    val containerColor = if (!isDark) Color.White else Color(0xFF1a1a1a).copy(0.80f)
    val glassFill = scheme.surfaceVariant.copy(alpha = if (isDark) 0.35f else 0.25f)
    val glassContent = scheme.onSurface
    val btnColors = ButtonDefaults.buttonColors(
        containerColor = Color.Transparent,
        contentColor = glassContent,
        disabledContainerColor = Color.Transparent,
        disabledContentColor = glassContent.copy(alpha = 0.40f)
    )
    val leadIS = remember { MutableInteractionSource() }
    val trailIS = remember { MutableInteractionSource() }
    val leadPressed by leadIS.collectIsPressedAsState()
    val trailPressed by trailIS.collectIsPressedAsState()
    val hlColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)

    val leadPressT by animateFloatAsState(
        targetValue = if (leadPressed) 1f else 0f,
        animationSpec = tween(durationMillis = 90),
        label = "contactsLeadPressT"
    )
    val trailPressT by animateFloatAsState(
        targetValue = if (trailPressed) 1f else 0f,
        animationSpec = tween(durationMillis = 90),
        label = "contactsTrailPressT"
    )
    val shapeT by animateFloatAsState(
        targetValue = if (menuOpen) 1f else 0f,
        label = "contactsSplitShapeT"
    )

    fun lerpDp(a: Dp, b: Dp, t: Float): Dp = a + (b - a) * t

    val outer = 50.dp
    val innerClosed = 5.dp
    val innerOpen = 24.dp
    val pressedInner = 14.dp
    val rightInnerBase = lerpDp(innerClosed, innerOpen, shapeT)
    val leftInner = lerpDp(innerClosed, pressedInner, leadPressT)
    val rightInner = lerpDp(rightInnerBase, pressedInner, trailPressT)
    val leftEffectiveShape = RoundedCornerShape(
        topStart = outer,
        bottomStart = outer,
        topEnd = leftInner,
        bottomEnd = leftInner
    )
    val rightEffectiveShape = RoundedCornerShape(
        topStart = rightInner,
        bottomStart = rightInner,
        topEnd = outer,
        bottomEnd = outer
    )
    val leftShapes = SplitButtonShapes(
        shape = leftEffectiveShape,
        pressedShape = leftEffectiveShape,
        checkedShape = leftEffectiveShape
    )
    val rightShapes = SplitButtonShapes(
        shape = rightEffectiveShape,
        pressedShape = rightEffectiveShape,
        checkedShape = rightEffectiveShape
    )

    Surface(
        shape = topBarShape,
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { topBarShape },
            highlight = {
                Highlight(
                    width = 0.50.dp,
                    blurRadius = 1.dp,
                    alpha = 0.20f,
                    style = HighlightStyle.Ambient,
                )
            },
            effects = {
                blur(radius = 1f.dp.toPx(), edgeTreatment = TileMode.Mirror)
                lens(
                    refractionHeight = 60f,
                    refractionAmount = 80f,
                    depthEffect = false,
                    chromaticAberration = false
                )
            },
            onDrawSurface = { drawRect(containerColor) }
        )
    ) {
        androidx.compose.material3.CenterAlignedTopAppBar(
            navigationIcon = {},
            title = {
                val titleText = if (filtering) "Search" else "Contacts"
                val badgeText = if (filtering) "Filtering" else contactCount.toString()
                Box(Modifier.wrapContentSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 23.dp, y = (-8).dp)
                    ) {
                        BadgePillTopBar(text = badgeText)
                    }
                }
            },
            actions = {
                val scale by animateFloatAsState(
                    targetValue = if (menuOpen) 1.00f else 1f,
                    label = "contactsSplitScale"
                )
                Box(
                    Modifier
                        .wrapContentSize(Alignment.TopEnd)
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                ) {
                    SplitButtonLayout(
                        leadingButton = {
                            SplitButtonDefaults.LeadingButton(
                                enabled = true,
                                onClick = onAddContact,
                                colors = btnColors,
                                shapes = leftShapes,
                                interactionSource = leadIS,
                                modifier = Modifier
                                    .zIndex(1f)
                                    .clip(leftEffectiveShape)
                                    .drawBackdrop(
                                        backdrop = backdrop,
                                        shape = { leftEffectiveShape },
                                        shadow = null,
                                        highlight = {
                                            Highlight(
                                                width = 0.50.dp,
                                                blurRadius = 1.dp,
                                                alpha = 0.96f,
                                                style = HighlightStyle.Plain(color = hlColor)
                                            )
                                        },
                                        effects = { blur(radius = 8f.dp.toPx(), edgeTreatment = TileMode.Clamp) },
                                        onDrawSurface = { drawRect(glassFill) }
                                    )
                            ) { Text("Add") }
                        },
                        trailingButton = {
                            val rotation by animateFloatAsState(
                                targetValue = if (menuOpen) 180f else 0f,
                                label = "contactsSplitArrow"
                            )
                            SplitButtonDefaults.TrailingButton(
                                checked = menuOpen,
                                onCheckedChange = { menuOpen = it },
                                enabled = true,
                                colors = btnColors,
                                shapes = rightShapes,
                                interactionSource = trailIS,
                                modifier = Modifier
                                    .zIndex(0f)
                                    .clip(rightEffectiveShape)
                                    .drawBackdrop(
                                        backdrop = backdrop,
                                        shape = { rightEffectiveShape },
                                        shadow = null,
                                        highlight = {
                                            Highlight(
                                                width = 0.50.dp,
                                                blurRadius = 1.dp,
                                                alpha = 0.96f,
                                                style = HighlightStyle.Plain(color = hlColor)
                                            )
                                        },
                                        effects = { blur(radius = 8f.dp.toPx(), edgeTreatment = TileMode.Clamp) },
                                        onDrawSurface = { drawRect(glassFill) }
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.KeyboardArrowDown,
                                    contentDescription = "More",
                                    modifier = Modifier
                                        .size(SplitButtonDefaults.TrailingIconSize)
                                        .graphicsLayer { rotationZ = rotation }
                                )
                            }
                        }
                    )

                    DropdownMenuPopup(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false }
                    ) {
                        DropdownMenuGroup(
                            shapes = MenuDefaults.groupShape(index = 0, count = 1),
                            containerColor = MenuDefaults.groupVibrantContainerColor
                        ) {
                            DropdownMenuItem(
                                selected = false,
                                onClick = {
                                    menuOpen = false
                                    onOpenSearch()
                                },
                                text = { Text("Search") },
                                shapes = MenuDefaults.itemShape(index = 0, count = 2),
                                colors = MenuDefaults.itemColors(),
                                trailingIcon = { Icon(Icons.Filled.Search, null) }
                            )
                            DropdownMenuItem(
                                selected = false,
                                onClick = {
                                    menuOpen = false
                                    onImportContacts()
                                },
                                text = { Text("Import contacts") },
                                shapes = MenuDefaults.itemShape(index = 1, count = 2),
                                colors = MenuDefaults.itemColors(),
                                trailingIcon = { Icon(Icons.Filled.ImportContacts, null) }
                            )
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Unspecified,
                navigationIconContentColor = Color.Unspecified,
                titleContentColor = Color.Unspecified,
                actionIconContentColor = Color.Unspecified
            )
        )
    }
}

@Composable
private fun ContactComposeRow(
    contact: AllContact,
    palette: ContactsAdapter.ColorPalette?,
    onEditContact: (AllContact) -> Unit,
    onDeleteContact: (AllContact) -> Unit,
    onSwipeDeleteContact: (AllContact) -> Unit,
    onPaletteClick: (AllContact) -> Unit,
    onCallContact: (AllContact) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val view = LocalView.current
    val shape = RoundedCornerShape(22.dp)
    var expanded by rememberSaveable(contact.id) { mutableStateOf(false) }
    var offsetX by remember(contact.id) { mutableStateOf(0f) }
    val density = LocalDensity.current
    val maxSwipePx = with(density) { 104.dp.toPx() }
    val openThresholdPx = with(density) { 24.dp.toPx() }
    val revealProgress = (-offsetX / maxSwipePx).coerceIn(0f, 1f)

    val headerDefault = if (isDark) {
        Color(0xFF1D1726).copy(alpha = 0.88f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
    }
    val contentDefault = if (isDark) {
        Color(0xFF1D1726).copy(alpha = 0.88f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
    }
    val buttonDefault = if (isDark) Color(0xFF2B2D34) else Color.White

    val headerColor = palette?.mainColor?.toComposeColorOr(headerDefault) ?: headerDefault
    val contentColor = palette?.overlayColor?.toComposeColorOr(contentDefault) ?: contentDefault
    val buttonColor = palette?.buttonColor?.toComposeColorOr(buttonDefault) ?: buttonDefault
    val headerTextColor = readableTextColor(headerColor)
    val contentTextColor = readableTextColor(contentColor)
    val buttonTextColor = readableTextColor(buttonColor)
    val detailBubbleColor = buttonColor.copy(alpha = if (isDark) 0.62f else 0.72f)
    val settingsBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = if (isDark) 0.34f else 0.22f)
    val availableDetails = listOfNotNull(
        contact.phone.takeIf { it.isNotBlank() }?.let { ContactDetail("Phone", it, contact.flag) },
        contact.email?.takeIf { it.isNotBlank() }?.let { ContactDetail("Email", it) },
        contact.address?.takeIf { it.isNotBlank() }?.let { ContactDetail("Address", it) },
        contact.birthday?.takeIf { it.isNotBlank() }?.let { ContactDetail("Birthday", it) }
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .background(Color(0xFF2EA44F).copy(alpha = revealProgress)),
            contentAlignment = Alignment.CenterEnd
        ) {
            Surface(
                onClick = {
                    if (offsetX < 0f) {
                        onSwipeDeleteContact(contact)
                        offsetX = 0f
                    }
                },
                modifier = Modifier
                    .height(52.dp)
                    .padding(end = 10.dp)
                    .graphicsLayer { alpha = revealProgress },
                shape = RoundedCornerShape(14.dp),
                color = Color.Transparent,
                contentColor = Color.White
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DELETE",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .border(BorderStroke(1.dp, settingsBorderColor), shape)
                .then(
                    if (!expanded) {
                        Modifier.pointerInput(contact.id) {
                            detectHorizontalDragGestures(
                                onDragStart = {
                                    view.performHapticFeedback(HapticFeedbackConstants.GESTURE_START)
                                },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    offsetX = (offsetX + dragAmount).coerceIn(-maxSwipePx, 0f)
                                },
                                onDragCancel = { offsetX = if (-offsetX >= openThresholdPx) -maxSwipePx else 0f },
                                onDragEnd = {
                                    offsetX = if (-offsetX >= openThresholdPx) -maxSwipePx else 0f
                                }
                            )
                        }
                    } else {
                        Modifier
                    }
                ),
            shape = shape,
            color = headerColor,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(1.dp)
                    .clip(RoundedCornerShape(21.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .background(headerColor)
                        .padding(start = 10.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                    ContactAvatar(
                        contact = contact,
                        color = Color(contact.color)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = contact.name.ifBlank { "(No name)" },
                        modifier = Modifier.weight(1f),
                        color = headerTextColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ExpandMore,
                            contentDescription = null,
                            tint = headerTextColor,
                            modifier = Modifier.graphicsLayer {
                                rotationZ = if (expanded) 180f else 0f
                            }
                        )
                    }
                }

                AnimatedVisibility(visible = expanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(contentColor)
                            .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (availableDetails.isEmpty()) {
                            ContactEmptyDetailLine(
                                color = contentTextColor,
                                background = detailBubbleColor
                            )
                        } else {
                            availableDetails.forEach { detail ->
                                ContactDetailLine(
                                    detail = detail,
                                    color = contentTextColor,
                                    background = detailBubbleColor
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ContactActionButton(
                                icon = Icons.Filled.LocalPhone,
                                background = buttonColor,
                                contentColor = buttonTextColor,
                                onClick = { onCallContact(contact) }
                            )
                            ContactActionButton(
                                icon = Icons.Filled.Edit,
                                background = buttonColor,
                                contentColor = buttonTextColor,
                                onClick = { onEditContact(contact) }
                            )
                            ContactActionButton(
                                icon = Icons.Filled.Delete,
                                background = buttonColor,
                                contentColor = buttonTextColor,
                                onClick = { onDeleteContact(contact) }
                            )
                            ContactActionButton(
                                icon = Icons.Filled.Palette,
                                background = buttonColor,
                                contentColor = buttonTextColor,
                                onClick = { onPaletteClick(contact) }
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class ContactDetail(
    val label: String,
    val value: String,
    val trailing: String? = null
)

@Composable
private fun ContactInitials(name: String, color: Color) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        ContactInitialsText(name)
    }
}

@Composable
private fun ContactAvatar(
    contact: AllContact,
    color: Color
) {
    val rawPhoto = contact.photoUri?.trim().orEmpty()
    if (rawPhoto.isBlank()) {
        ContactInitials(contact.name, color)
        return
    }

    val context = LocalContext.current
    val model = remember(rawPhoto) {
        when {
            rawPhoto.startsWith("/", ignoreCase = true) -> File(rawPhoto)
            else -> rawPhoto
        }
    }
    val request = remember(model) {
        ImageRequest.Builder(context)
            .data(model)
            .memoryCacheKey("contact-avatar:$rawPhoto")
            .diskCacheKey("contact-avatar:$rawPhoto")
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.DISABLED)
            .size(96, 96)
            .crossfade(false)
            .build()
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        ContactInitialsText(contact.name)
        AsyncImage(
            model = request,
            contentDescription = contact.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun ContactInitialsText(name: String) {
    Text(
        text = initialsFor(name),
        color = Color.White,
        fontSize = 16.sp,
        fontWeight = FontWeight.Black,
        maxLines = 1
    )
}

@Composable
private fun ContactDetailLine(
    detail: ContactDetail,
    color: Color,
    background: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = background,
        contentColor = color,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = detail.label.uppercase(),
                    color = color.copy(alpha = 0.62f),
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1
                )
                Text(
                    text = detail.value,
                    color = color,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!detail.trailing.isNullOrBlank()) {
                Text(
                    text = detail.trailing,
                    color = color,
                    fontSize = 17.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun ContactEmptyDetailLine(
    color: Color,
    background: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = background,
        contentColor = color,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "DETAILS",
                color = color.copy(alpha = 0.62f),
                fontSize = 9.sp,
                lineHeight = 11.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
            Text(
                text = "Add phone, email, address, or birthday",
                color = color,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ContactActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    background: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(62.dp)
            .height(46.dp),
        shape = RoundedCornerShape(8.dp),
        color = background,
        contentColor = contentColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

private fun Int.toComposeColorOr(default: Color): Color =
    if (this == AndroidColor.TRANSPARENT) default else Color(this)

private fun readableTextColor(color: Color): Color {
    val luminance = 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue
    return if (luminance < 0.52f) Color.White else Color(0xFF121216)
}

private fun initialsFor(name: String): String =
    name.trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifBlank { "?" }
