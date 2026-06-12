package com.flights.studio

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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ImportContacts
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.google.gson.Gson
import com.google.i18n.phonenumbers.PhoneNumberUtil
import kotlinx.coroutines.delay
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
    onImportContacts: () -> Unit,
    addFabVisible: Boolean = true,
    contentView: android.view.View? = null
) {
    var showMenuSheet by remember { mutableStateOf(false) }
    var showInfoSheet by remember { mutableStateOf(false) }
    var pendingInfoSheet by remember { mutableStateOf(false) }

    LaunchedEffect(showMenuSheet, pendingInfoSheet) {
        if (pendingInfoSheet && !showMenuSheet) {
            delay(140)
            pendingInfoSheet = false
            showInfoSheet = true
        }
    }

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
            backdrop = backdrop,
            contentView = contentView
        )

        ContactsFloatingAddButton(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 78.dp)
                .navigationBarsPadding(),
            backdrop = backdrop,
            contentView = contentView,
            visible = addFabVisible,
            onClick = onAddContact
        )

        ContactsMenuSheet(
            visible = showMenuSheet,
            modifier = Modifier.align(Alignment.BottomCenter),
            contactCount = contactCount,
            backdrop = backdrop,
            contentView = contentView,
            onDismiss = { showMenuSheet = false },
            onAddContact = onAddContact,
            onImportContacts = onImportContacts,
            onOpenInfo = {
                showMenuSheet = false
                pendingInfoSheet = true
            }
        )
        ContactsInfoSheet(
            visible = showInfoSheet,
            modifier = Modifier.align(Alignment.BottomCenter),
            contactCount = contactCount,
            backdrop = backdrop,
            contentView = contentView,
            onDismiss = { showInfoSheet = false }
        )
    }
}

@Composable
fun ContactsFloatingAddButton(
    modifier: Modifier = Modifier,
    backdrop: Backdrop,
    contentView: android.view.View?,
    visible: Boolean = true,
    onClick: () -> Unit
) {
    val fabSurfaceColor = if (isSystemInDarkTheme()) Color(0xFF202124) else Color.White
    val fabBorderColor = if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.08f)
    val contentColor = if (isSystemInDarkTheme()) Color.White else Color(0xFF1E1F24)
    val hideOffsetPx = (40f * LocalDensity.current.density).toInt()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.90f else 1f,
        animationSpec = spring(
            dampingRatio = 0.62f,
            stiffness = 520f
        ),
        label = "contacts_add_fab_press"
    )

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(animationSpec = tween(180)) +
            scaleIn(animationSpec = tween(180), initialScale = 0.4f) +
            slideInVertically(animationSpec = tween(180), initialOffsetY = { hideOffsetPx }),
        exit = fadeOut(animationSpec = tween(160)) +
            scaleOut(animationSpec = tween(160), targetScale = 0.4f) +
            slideOutVertically(animationSpec = tween(160), targetOffsetY = { hideOffsetPx })
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                }
                .shadow(0.25.dp, CircleShape, clip = false)
                .clip(CircleShape)
                .background(fabSurfaceColor, CircleShape)
                .border(1.dp, fabBorderColor, CircleShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.PersonAdd,
                contentDescription = "Add contact",
                modifier = Modifier.size(24.dp),
                tint = contentColor
            )
        }
    }
}

@Composable
private fun ContactsQuickTabBar(
    modifier: Modifier = Modifier,
    onOpenHome: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenMenu: () -> Unit,
    backdrop: Backdrop,
    contentView: android.view.View?
) {
    val glassColor = bottomTabBarTint()
    val overlayTint = bottomTabBarOverlayTint()

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
    val selectedColor = bottomTabSelectedPillColor()
    val inactiveColor = bottomTabInactiveColor()
    val selectedContentColor = primaryTabAccentColor()
    val pillAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = if (selected) 180 else 120, easing = FastOutSlowInEasing),
        label = "contactsTabPillAlpha"
    )
    val pillScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.84f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 520f),
        label = "contactsTabPillScale"
    )
    val contentScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.94f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 620f),
        label = "contactsTabContentScale"
    )

    Box(
        modifier = Modifier
            .weight(1f)
            .height(50.dp)
            .clip(GlassChromeInnerShape)
            .clickable(onClick = onClick),
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
                .background(selectedColor, GlassChromeInnerShape)
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

private data class ContactsMenuAction(
    val label: String,
    val icon: ImageVector,
    val enabled: Boolean = true,
    val onClick: () -> Unit = {}
)

private data class ContactCountryStat(
    val region: String,
    val countryName: String,
    val flag: String,
    val phones: List<ContactPhoneEntry>
) {
    val count: Int get() = phones.size
}

private data class ContactPhoneEntry(
    val name: String,
    val phone: String
)

@Composable
private fun ContactsMenuSheet(
    visible: Boolean,
    modifier: Modifier = Modifier,
    contactCount: Int,
    backdrop: Backdrop,
    contentView: android.view.View?,
    onDismiss: () -> Unit,
    onAddContact: () -> Unit,
    onImportContacts: () -> Unit,
    onOpenInfo: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val panelColor = bottomTabBarTint()
    val overlayTint = bottomTabBarOverlayTint()
    val textColor = if (isDark) Color.White.copy(alpha = 0.92f) else Color(0xFF1E1F24)
    val iconColor = if (isDark) Color.White.copy(alpha = 0.92f) else Color(0xFF1E1F24)
    val buttonColor = bottomTabSelectedPillColor()
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
            label = "Info",
            icon = Icons.Filled.Info,
            onClick = onOpenInfo
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
fun ContactsInfoSheet(
    visible: Boolean,
    modifier: Modifier = Modifier,
    contactCount: Int,
    backdrop: Backdrop,
    contentView: android.view.View? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val panelColor = if (isDark) Color(0xFF202124).copy(alpha = 0.66f) else Color(0xFFE6E2E7).copy(alpha = 0.56f)
    val textColor = if (isDark) Color.White.copy(alpha = 0.94f) else Color(0xFF1E1F24)
    val secondaryColor = textColor.copy(alpha = 0.62f)
    val stats = remember(visible, contactCount) { readContactCountryStats(context) }
    val formattedCount = remember(contactCount) {
        NumberFormat.getIntegerInstance(Locale.getDefault()).format(contactCount)
    }
    var selectedCountry by remember(visible) { mutableStateOf<ContactCountryStat?>(null) }
    val activeCountry = selectedCountry
    val activeCount = activeCountry?.count ?: contactCount
    val formattedActiveCount = remember(activeCount) {
        NumberFormat.getIntegerInstance(Locale.getDefault()).format(activeCount)
    }
    val density = LocalDensity.current

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 90)),
        exit = fadeOut(animationSpec = tween(durationMillis = 140))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(40f)
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
        modifier = modifier
            .zIndex(41f)
            .imePadding(),
        enter = slideInVertically(
            animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
            initialOffsetY = { it / 2 }
        ) + fadeIn(animationSpec = tween(durationMillis = 120)) +
            scaleIn(animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing), initialScale = 0.96f),
        exit = slideOutVertically(
            animationSpec = tween(durationMillis = 170, easing = FastOutLinearInEasing),
            targetOffsetY = { it / 3 }
        ) + fadeOut(animationSpec = tween(durationMillis = 120)) +
            scaleOut(animationSpec = tween(durationMillis = 170, easing = FastOutLinearInEasing), targetScale = 0.98f)
    ) {
        Box(
            modifier = Modifier
                .padding(
                    start = GlassChromeHorizontalPadding,
                    end = GlassChromeHorizontalPadding,
                    bottom = GlassChromeHorizontalPadding
                )
                .fillMaxWidth()
                .heightIn(max = 620.dp)
                .shadow(GlassChromeShadowElevation, GlassChromeShape, clip = false)
                .clip(GlassChromeShape)
                .border(
                    width = 1.dp,
                    color = textColor.copy(alpha = if (isDark) 0.14f else 0.10f),
                    shape = GlassChromeShape
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
            AndroidView(
                modifier = Modifier.matchParentSize(),
                factory = { FrostedActionBarBlurView(it) },
                update = {
                    it.contentView = contentView
                    it.scrimColor = panelColor.toArgb()
                    it.cornerRadiusPx = with(density) { GlassChromeCornerRadius.toPx() }
                    it.blurRadiusPx = GlassChromeNativeBlurPx
                    it.saturation = 1.16f
                    it.useLiquidRefraction = false
                }
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(panelColor, GlassChromeShape)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (activeCountry != null) {
                            Surface(
                                onClick = { selectedCountry = null },
                                shape = CircleShape,
                                color = textColor.copy(alpha = if (isDark) 0.12f else 0.08f),
                                contentColor = textColor,
                                tonalElevation = 0.dp,
                                shadowElevation = 0.dp
                            ) {
                                Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        modifier = Modifier.size(19.dp),
                                        tint = textColor
                                    )
                                }
                            }
                        }
                        Column {
                            Text(
                                text = activeCountry?.countryName ?: "Contacts info",
                                color = textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = if (activeCountry == null) "$formattedCount saved contacts" else "${activeCountry.flag} ${activeCountry.region} phones",
                                color = secondaryColor,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Surface(
                        onClick = onDismiss,
                        shape = CircleShape,
                        color = textColor.copy(alpha = if (isDark) 0.12f else 0.08f),
                        contentColor = textColor,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp
                    ) {
                        Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close",
                                modifier = Modifier.size(18.dp),
                                tint = textColor
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = textColor.copy(alpha = if (isDark) 0.10f else 0.07f),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (activeCountry == null) "Total" else "${activeCountry.countryName} total",
                            color = secondaryColor,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = formattedActiveCount,
                            color = textColor,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }

                Text(
                    text = if (activeCountry == null) "Phone countries" else "Phone numbers",
                    color = secondaryColor,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                if (activeCountry != null) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(activeCountry.phones) { entry ->
                            ContactPhoneRow(entry = entry, textColor = textColor, secondaryColor = secondaryColor)
                        }
                    }
                } else {
                    if (stats.isEmpty()) {
                        Text(
                            text = "No country data yet. Add phone numbers with country codes to see them here.",
                            color = secondaryColor,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        stats.take(8).forEach { stat ->
                            ContactCountryRow(
                                stat = stat,
                                textColor = textColor,
                                secondaryColor = secondaryColor,
                                onClick = { selectedCountry = stat }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactCountryRow(
    stat: ContactCountryStat,
    textColor: Color,
    secondaryColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stat.flag, fontSize = 22.sp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stat.countryName,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = stat.region,
                color = secondaryColor,
                style = MaterialTheme.typography.labelSmall
            )
        }
        Text(
            text = NumberFormat.getIntegerInstance(Locale.getDefault()).format(stat.count),
            color = textColor,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

@Composable
private fun ContactPhoneRow(
    entry: ContactPhoneEntry,
    textColor: Color,
    secondaryColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(textColor.copy(alpha = if (isSystemInDarkTheme()) 0.08f else 0.05f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(textColor.copy(alpha = if (isSystemInDarkTheme()) 0.12f else 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = entry.name.trim().take(1).uppercase(Locale.getDefault()).ifBlank { "#" },
                color = textColor,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.name.ifBlank { entry.phone },
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = entry.phone,
                color = secondaryColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun readContactCountryStats(context: android.content.Context): List<ContactCountryStat> =
    runCatching {
        val raw = context.getSharedPreferences("contacts_data", android.content.Context.MODE_PRIVATE)
            .getString("contacts", null)
            ?: return@runCatching emptyList()
        val contacts = runCatching {
            Gson().fromJson(raw, Array<AllContact>::class.java)?.toList().orEmpty()
        }.getOrDefault(emptyList())
        val phoneUtil = PhoneNumberUtil.getInstance()
        contacts.mapNotNull { contact ->
                val phone = contact.phone.replace("[^\\d+]".toRegex(), "")
                if (phone.length < 7) return@mapNotNull null
                val normalized = when {
                    phone.startsWith("+") -> phone
                    phone.length == 10 -> "+1$phone"
                    else -> "+$phone"
                }
                runCatching {
                    phoneUtil.getRegionCodeForNumber(phoneUtil.parse(normalized, "US"))
                }.getOrNull()
                    ?.uppercase(Locale.US)
                    ?.takeIf { it.length == 2 && it.all { char -> char in 'A'..'Z' } }
                    ?.let { region ->
                        region to ContactPhoneEntry(
                            name = contact.name,
                            phone = contact.phone
                        )
                    }
            }
            .groupBy({ it.first }, { it.second })
            .map { (region, phones) ->
                val countryName = Locale("", region).displayCountry.takeIf { it.isNotBlank() } ?: region
                ContactCountryStat(
                    region = region,
                    countryName = countryName,
                    flag = regionToFlag(region),
                    phones = phones.sortedWith(compareBy<ContactPhoneEntry> { it.name.lowercase(Locale.getDefault()) }.thenBy { it.phone })
                )
            }
            .sortedWith(compareByDescending<ContactCountryStat> { it.count }.thenBy { it.countryName })
    }.getOrDefault(emptyList())

private fun regionToFlag(region: String): String {
    val normalized = region.uppercase(Locale.US)
    if (normalized.length != 2 || !normalized.all { it in 'A'..'Z' }) return "?"
    return normalized.map { char ->
        String(Character.toChars(0x1F1E6 + (char.code - 'A'.code)))
    }.joinToString("")
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
