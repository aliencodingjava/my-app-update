package com.flights.studio

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.SoundPool
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.preference.PreferenceManager
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.flights.studio.ui.AppLanguageManager
import com.google.firebase.messaging.FirebaseMessaging
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.TimeZone
import androidx.compose.material.icons.filled.Settings as SettingsIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernSettingsScreen(
    searchQuery: String,
    onOpenHome: () -> Unit,
    onOpenSoftwareUpdate: () -> Unit,
    onOpenAppIcon: () -> Unit,
    onOpenLiquidGlass: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenRateUs: () -> Unit,
    onOpenCardDrawer: (String) -> Unit,
    onOpenNotes: () -> Unit,
    onOpenContacts: () -> Unit,
    onShareApp: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenQrCode: () -> Unit,
    onOpenProfile: () -> Unit,
    searchSheetVisible: Boolean = false,
    showBottomChrome: Boolean = true,
    modalBottomPadding: Dp = GlassChromeHorizontalPadding,
    feedbackRequestToken: Int = 0,
    onModalVisibleChange: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val prefs = remember(appContext) { PreferenceManager.getDefaultSharedPreferences(appContext) }
    val isDark = isSystemInDarkTheme()
    val settingsChromeBackdrop = rememberLayerBackdrop()
    val settingsModalBackdrop = rememberLayerBackdrop()
    val locale = LocalLocale.current.platformLocale
    val normalizedQuery = searchQuery.trim().lowercase(locale)
    val showLanguageSheet = remember { mutableStateOf(false) }
    val showChangelog = remember { mutableStateOf(false) }
    val showFeedbackSheet = remember { mutableStateOf(false) }
    val showMenuSheet = remember { mutableStateOf(false) }
    val modalVisible = showLanguageSheet.value || showChangelog.value || showFeedbackSheet.value ||
        showMenuSheet.value

    LaunchedEffect(modalVisible) {
        onModalVisibleChange(modalVisible)
    }
    DisposableEffect(Unit) {
        onDispose { onModalVisibleChange(false) }
    }
    LaunchedEffect(feedbackRequestToken) {
        if (feedbackRequestToken > 0) {
            showFeedbackSheet.value = true
        }
    }
    var cameraGlowEnabled by remember {
        mutableStateOf(prefs.getBoolean(KEY_CAMERA_GLOW, true))
    }
    var briefingWeatherEnabled by remember {
        mutableStateOf(SettingsStore.briefingWeatherEnabled(appContext))
    }
    var mainPageKeepAwake by remember {
        mutableStateOf(SettingsStore.mainPageKeepAwake(appContext))
    }
    var liveCamerasKeepAwake by remember {
        mutableStateOf(SettingsStore.liveCamerasKeepAwake(appContext))
    }
    val userPrefs = remember(appContext) { UserPreferencesManager(appContext) }
    val profileName = userPrefs.userName
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.default_user_name)
    userPrefs.userEmail
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.unknown_contact)
    val profileInitials = remember(profileName) { profileName.settingsInitials() }
    val profilePhotoRaw = userPrefs.userPhotoUriString.orEmpty()
    val versionSummary = remember(appContext) { appContext.appVersionSummary() }
    val languageLabel = when (AppLanguageManager.currentLanguageTag(context)) {
        "es" -> stringResource(R.string.settings_language_spanish)
        else -> stringResource(R.string.settings_language_english)
    }
    val sections = listOf(
        SettingsSection(
            title = stringResource(R.string.settings_section_app),
            entries = listOf(
                SettingsEntry(
                    title = stringResource(R.string.settings_software_update_title),
                    summary = stringResource(R.string.settings_software_update_summary),
                    icon = Icons.Filled.SystemUpdate,
                    onClick = onOpenSoftwareUpdate
                ),
                SettingsEntry(
                    title = stringResource(R.string.settings_changelog_title),
                    summary = stringResource(
                        R.string.settings_release_date_template,
                        BuildConfig.RELEASE_DATE
                    ),
                    icon = Icons.AutoMirrored.Filled.Article,
                    onClick = { showChangelog.value = true }
                ),
                SettingsEntry(
                    title = stringResource(R.string.settings_app_icon_title),
                    summary = stringResource(R.string.settings_app_icon_summary),
                    icon = Icons.Filled.Palette,
                    onClick = onOpenAppIcon
                ),
                SettingsEntry(
                    title = stringResource(R.string.settings_liquid_glass_title),
                    summary = stringResource(R.string.settings_liquid_glass_summary),
                    icon = Icons.Filled.ColorLens,
                    onClick = onOpenLiquidGlass
                )
            )
        ),
        SettingsSection(
            title = stringResource(R.string.settings_section_preferences),
            entries = listOf(
                SettingsEntry(
                    title = stringResource(R.string.settings_language_title),
                    summary = languageLabel,
                    icon = Icons.Filled.Language,
                    onClick = { showLanguageSheet.value = true }
                ),
                SettingsEntry(
                    title = stringResource(R.string.settings_camera_glow_title),
                    summary = stringResource(R.string.settings_camera_glow_summary),
                    icon = Icons.Filled.ColorLens,
                    trailing = {
                        Switch(
                            checked = cameraGlowEnabled,
                            onCheckedChange = { enabled ->
                                cameraGlowEnabled = enabled
                                prefs.edit { putBoolean(KEY_CAMERA_GLOW, enabled) }
                            }
                        )
                    },
                    onClick = {
                        cameraGlowEnabled = !cameraGlowEnabled
                        prefs.edit { putBoolean(KEY_CAMERA_GLOW, cameraGlowEnabled) }
                    }
                ),
                SettingsEntry(
                    title = stringResource(R.string.settings_briefing_weather_title),
                    summary = stringResource(R.string.settings_briefing_weather_summary),
                    icon = Icons.Filled.Wifi,
                    trailing = {
                        Switch(
                            checked = briefingWeatherEnabled,
                            onCheckedChange = { enabled ->
                                briefingWeatherEnabled = enabled
                                SettingsStore.setBriefingWeatherEnabled(appContext, enabled)
                            }
                        )
                    },
                    onClick = {
                        briefingWeatherEnabled = !briefingWeatherEnabled
                        SettingsStore.setBriefingWeatherEnabled(appContext, briefingWeatherEnabled)
                    }
                ),
                SettingsEntry(
                    title = stringResource(R.string.settings_main_page_keep_awake_title),
                    summary = stringResource(R.string.settings_main_page_keep_awake_summary),
                    icon = Icons.Filled.LightMode,
                    trailing = {
                        Switch(
                            checked = mainPageKeepAwake,
                            onCheckedChange = { enabled ->
                                mainPageKeepAwake = enabled
                                SettingsStore.setMainPageKeepAwake(appContext, enabled)
                            }
                        )
                    },
                    onClick = {
                        mainPageKeepAwake = !mainPageKeepAwake
                        SettingsStore.setMainPageKeepAwake(appContext, mainPageKeepAwake)
                    }
                ),
                SettingsEntry(
                    title = stringResource(R.string.settings_live_cameras_keep_awake_title),
                    summary = stringResource(R.string.settings_live_cameras_keep_awake_summary),
                    icon = Icons.Filled.LightMode,
                    trailing = {
                        Switch(
                            checked = liveCamerasKeepAwake,
                            onCheckedChange = { enabled ->
                                liveCamerasKeepAwake = enabled
                                SettingsStore.setLiveCamerasKeepAwake(appContext, enabled)
                            }
                        )
                    },
                    onClick = {
                        liveCamerasKeepAwake = !liveCamerasKeepAwake
                        SettingsStore.setLiveCamerasKeepAwake(appContext, liveCamerasKeepAwake)
                    }
                )
            )
        ),
        SettingsSection(
            title = stringResource(R.string.settings_section_airport_services),
            entries = listOf(
                SettingsEntry(
                    title = stringResource(R.string.settings_airport_phone_title),
                    summary = AIRPORT_PHONE_DISPLAY,
                    icon = Icons.Filled.Phone,
                    onClick = { context.openDialer(AIRPORT_PHONE_TEL) }
                ),
                SettingsEntry(
                    title = stringResource(R.string.settings_airport_operations_title),
                    summary = AIRPORT_OPERATIONS_EMAIL,
                    icon = Icons.Filled.Email,
                    onClick = { context.openEmail(AIRPORT_OPERATIONS_EMAIL) }
                ),
                SettingsEntry(
                    title = stringResource(R.string.settings_airport_services_title),
                    summary = AIRPORT_SERVICES_EMAIL,
                    icon = Icons.Filled.Email,
                    onClick = { context.openEmail(AIRPORT_SERVICES_EMAIL) }
                ),
                SettingsEntry(
                    title = stringResource(R.string.settings_human_resources_title),
                    summary = HUMAN_RESOURCES_EMAIL,
                    icon = Icons.Filled.Email,
                    onClick = { context.openEmail(HUMAN_RESOURCES_EMAIL) }
                ),
                SettingsEntry(
                    title = stringResource(R.string.settings_lost_found_title),
                    summary = LOST_AND_FOUND_EMAIL,
                    icon = Icons.Filled.Email,
                    onClick = { context.openEmail(LOST_AND_FOUND_EMAIL) }
                ),
                SettingsEntry(
                    title = stringResource(R.string.settings_communications_title),
                    summary = COMMUNICATIONS_EMAIL,
                    icon = Icons.Filled.Email,
                    onClick = { context.openEmail(COMMUNICATIONS_EMAIL) }
                )
            )
        ),
        SettingsSection(
            title = stringResource(R.string.settings_section_feedback),
            entries = listOf(
                SettingsEntry(
                    title = stringResource(R.string.settings_signup_title),
                    summary = stringResource(R.string.settings_signup_summary),
                    icon = Icons.Filled.Notifications,
                    onClick = onOpenNotifications
                ),
                SettingsEntry(
                    title = stringResource(R.string.settings_rate_title),
                    summary = stringResource(R.string.settings_rate_summary),
                    icon = Icons.Filled.Star,
                    onClick = onOpenRateUs
                ),
                SettingsEntry(
                    title = stringResource(R.string.settings_share_title),
                    summary = stringResource(R.string.settings_share_summary),
                    icon = Icons.Filled.Share,
                    onClick = onShareApp
                ),
                SettingsEntry(
                    title = stringResource(R.string.settings_feedback_title),
                    summary = stringResource(R.string.settings_feedback_summary),
                    icon = Icons.Filled.Feedback,
                    onClick = { showFeedbackSheet.value = true }
                )
            )
        ),
        SettingsSection(
            title = stringResource(R.string.settings_section_about),
            entries = listOf(
                SettingsEntry(
                    title = stringResource(R.string.settings_licenses_title),
                    summary = stringResource(R.string.settings_licenses_summary),
                    icon = Icons.Filled.GppGood,
                    onClick = { onOpenCardDrawer("licenses") }
                ),
                SettingsEntry(
                    title = stringResource(R.string.settings_privacy_title),
                    summary = stringResource(R.string.settings_privacy_summary),
                    icon = Icons.Filled.PrivacyTip,
                    onClick = { onOpenCardDrawer("privacy_policy") }
                ),
                SettingsEntry(
                    title = stringResource(R.string.settings_device_title),
                    summary = versionSummary,
                    icon = Icons.Filled.Info,
                    onClick = {}
                )
            )
        )
    )
    val visibleSections = sections.mapNotNull { section ->
        val filtered = if (normalizedQuery.isBlank()) {
            section.entries
        } else {
            section.entries.filter { entry ->
                section.title.contains(normalizedQuery, ignoreCase = true) ||
                    entry.title.contains(normalizedQuery, ignoreCase = true) ||
                    entry.summary.contains(normalizedQuery, ignoreCase = true)
            }
        }
        if (filtered.isEmpty()) null else section.copy(entries = filtered)
    }

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .layerBackdrop(settingsModalBackdrop)
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .layerBackdrop(settingsChromeBackdrop)
            ) {
                ProfileBackdropImageLayer(
                    modifier = Modifier.fillMaxSize(),
                    lightRes = R.drawable.light_grid_pattern,
                    darkRes = R.drawable.dark_grid_pattern,
                    imageAlpha = if (isDark) 0.95f else 0.70f,
                    scrimDark = 0.12f,
                    scrimLight = 0.03f
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentPadding = PaddingValues(start = 8.dp, top = 112.dp, end = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    visibleSections.forEach { section ->
                        item(key = section.title) {
                            SettingsSectionGroup(section)
                        }
                    }

                    item {
                        AnimatedVisibility(visible = normalizedQuery.isNotBlank() && visibleSections.isEmpty()) {
                            Text(
                                text = stringResource(R.string.settings_no_results_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                        }
                    }
                }
            }

            SettingsGlassTopAppBar(
                backdrop = settingsChromeBackdrop,
                profileInitials = profileInitials,
                profilePhotoRaw = profilePhotoRaw,
                appContext = appContext,
                onOpenProfile = onOpenProfile,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            if (showBottomChrome) {
                SettingsQuickTabBar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 4.dp)
                        .navigationBarsPadding(),
                    backdrop = settingsChromeBackdrop,
                    onOpenHome = onOpenHome,
                    onOpenNotes = onOpenNotes,
                    onOpenContacts = onOpenContacts,
                    onOpenSettings = {},
                    onOpenMenu = { showMenuSheet.value = true }
                )

                SettingsFloatingSearchButton(
                    visible = !modalVisible && !searchSheetVisible,
                    backdrop = settingsChromeBackdrop,
                    onClick = onOpenSearch,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 22.dp, bottom = 70.dp)
                        .navigationBarsPadding()
                        .zIndex(20f)
                )
            }
        }

        SettingsChangelogSheet(
            visible = showChangelog.value,
            modifier = Modifier.align(Alignment.BottomCenter),
            backdrop = settingsModalBackdrop,
            bottomPadding = modalBottomPadding,
            onDismiss = { showChangelog.value = false }
        )

        FeedbackGlassSheet(
            visible = showFeedbackSheet.value,
            modifier = Modifier.align(Alignment.BottomCenter),
            backdrop = settingsModalBackdrop,
            context = context,
            bottomPadding = modalBottomPadding,
            onDismiss = { showFeedbackSheet.value = false }
        )

        LanguagePickerSheet(
            visible = showLanguageSheet.value,
            selectedLanguageTag = AppLanguageManager.currentLanguageTag(appContext),
            modifier = Modifier.align(Alignment.BottomCenter),
            backdrop = settingsModalBackdrop,
            bottomPadding = modalBottomPadding,
            onDismiss = { showLanguageSheet.value = false },
            onLanguageSelected = { tag ->
                val current = AppLanguageManager.currentLanguageTag(appContext)
                if (tag != current) {
                    AppLanguageManager.persistLanguage(appContext, tag)
                    AppLanguageManager.markBlink()
                    (context as? Activity)?.recreate()
                    return@LanguagePickerSheet
                }
                showLanguageSheet.value = false
            }
        )

        SettingsMenuSheet(
            visible = showBottomChrome && showMenuSheet.value,
            modifier = Modifier.align(Alignment.BottomCenter),
            backdrop = settingsModalBackdrop,
            bottomPadding = modalBottomPadding,
            onDismiss = { showMenuSheet.value = false },
            actions = listOf(
                SettingsMenuAction(
                    label = "Profile",
                    icon = Icons.Filled.AccountCircle,
                    onClick = onOpenProfile
                ),
                SettingsMenuAction(
                    label = "QR Code",
                    icon = Icons.Filled.QrCode2,
                    onClick = onOpenQrCode
                ),
                SettingsMenuAction(
                    label = stringResource(R.string.settings_feedback_title),
                    icon = Icons.Filled.Feedback,
                    onClick = { showFeedbackSheet.value = true }
                )
            )
        )
    }
}

@Composable
private fun SettingsQuickTabBar(
    modifier: Modifier = Modifier,
    backdrop: Backdrop,
    onOpenHome: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenContacts: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenMenu: () -> Unit
) {
    val glassColor = bottomTabBarTint()
    val overlayTint = bottomTabBarOverlayTint()
    val backdropBlurDp = bottomChromeBackdropBlurDp()
    Box(
        modifier = modifier
            .padding(horizontal = GlassChromeHorizontalPadding)
            .fillMaxWidth()
            .height(56.dp)
            .shadow(GlassChromeShadowElevation, GlassChromeShape, clip = false)
            .clip(GlassChromeShape)
            .adaptiveLiquidGlassBackdrop(
                backdrop = backdrop,
                shape = GlassChromeShape,
                surfaceColor = glassColor,
                blurDp = backdropBlurDp,
                shadow = { bottomChromeShadow() },
                refractionHeightDp = GlassChromeRefractionHeightDp,
                refractionAmountDp = GlassChromeRefractionAmountDp
            )
            .background(
                color = overlayTint,
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
            SettingsQuickTab(
                label = stringResource(R.string.Home),
                icon = Icons.Filled.Home,
                selected = false,
                onClick = onOpenHome
            )
            SettingsQuickTab(
                label = stringResource(R.string.total_contacts),
                icon = Icons.Filled.Groups,
                selected = false,
                onClick = onOpenContacts
            )
            SettingsQuickTab(
                label = stringResource(R.string.contacts_bottom_notes),
                icon = Icons.AutoMirrored.Filled.Article,
                selected = false,
                onClick = onOpenNotes
            )
            SettingsQuickTab(
                label = stringResource(R.string.menu_settings),
                icon = Icons.Filled.SettingsIcon,
                selected = true,
                onClick = onOpenSettings
            )
            SettingsQuickTab(
                label = stringResource(R.string.settings_menu_tab),
                icon = Icons.Filled.Menu,
                selected = false,
                onClick = onOpenMenu
            )
        }
    }
}

@Composable
private fun RowScope.SettingsQuickTab(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val inactiveColor = bottomTabInactiveColor()
    val selectedContentColor = bottomTabSelectedContentColor()
    val selectedPillColor = bottomTabSelectedPillColor()
    val pillAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = if (selected) 180 else 120, easing = FastOutSlowInEasing),
        label = "settingsTabPillAlpha"
    )
    val pillScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.84f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 520f),
        label = "settingsTabPillScale"
    )
    val contentScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.94f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 620f),
        label = "settingsTabContentScale"
    )

    Box(
        modifier = Modifier
            .weight(1f)
            .height(46.dp)
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
                .background(selectedPillColor, GlassChromeInnerShape)
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

private data class SettingsMenuAction(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
private fun SettingsMenuSheet(
    visible: Boolean,
    modifier: Modifier = Modifier,
    backdrop: Backdrop,
    bottomPadding: Dp,
    onDismiss: () -> Unit,
    actions: List<SettingsMenuAction>
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
                bottom = bottomPadding
            )
            .fillMaxWidth()
            .clip(GlassChromeShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .adaptiveLiquidGlassBackdrop(
                backdrop = backdrop,
                shape = GlassChromeShape,
                surfaceColor = panelColor,
                blurDp = 4f,
                shadow = null,
                refractionHeightDp = 22f,
                refractionAmountDp = 72f,
                chromaticAberration = true
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
                            SettingsMenuButton(
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
private fun RowScope.SettingsMenuButton(
    action: SettingsMenuAction,
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
            color = textColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SettingsGlassTopAppBar(
    backdrop: Backdrop,
    profileInitials: String,
    profilePhotoRaw: String,
    appContext: Context,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val topBarShape = RoundedCornerShape(0.dp)
    val barColor = topActionBarTint()
    val contentColor = if (isDark) Color.White else Color(0xFF111111)

    Surface(
        shape = topBarShape,
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { topBarShape },
                shadow = null,
                highlight = null,
                effects = {
                    blur(
                        radius = TopActionBarBlurDp.dp.toPx(),
                        edgeTreatment = TileMode.Mirror
                    )
                },
                onDrawSurface = { drawRect(barColor) }
            )
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .height(64.dp)
                .padding(start = 20.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.menu_settings),
                modifier = Modifier.weight(1f),
                color = contentColor,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1
            )
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onOpenProfile
                    )
            ) {
                SettingsProfileAvatar(
                    appContext = appContext,
                    rawPhoto = profilePhotoRaw,
                    initials = profileInitials,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}


@Composable
private fun SettingsFloatingSearchButton(
    visible: Boolean,
    backdrop: Backdrop,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val iconColor = if (isDark) Color.White.copy(alpha = 0.94f) else Color(0xFF1E1F24)
    val buttonColor = if (isDark) {
        Color.White.copy(alpha = 0.14f)
    } else {
        Color.White.copy(alpha = 0.96f)
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(animationSpec = tween(durationMillis = 130)) +
            scaleIn(
                initialScale = 0.88f,
                animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
            ),
        exit = fadeOut(animationSpec = tween(durationMillis = 120)) +
            scaleOut(
                targetScale = 0.88f,
                animationSpec = tween(durationMillis = 120, easing = FastOutLinearInEasing)
            )
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .adaptiveLiquidGlassBackdrop(
                    backdrop = backdrop,
                    shape = CircleShape,
                    surfaceColor = buttonColor,
                    blurDp = 4f,
                    shadow = null,
                    refractionHeightDp = 22f,
                    refractionAmountDp = 72f,
                    chromaticAberration = true
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = stringResource(R.string.search),
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}


@Composable
private fun SettingsProfileAvatar(
    modifier: Modifier = Modifier,
    appContext: Context,
    rawPhoto: String,
    initials: String
) {
    var avatarFailed by remember(rawPhoto, initials) { mutableStateOf(false) }
    val avatarState by produceState(
        initialValue = if (rawPhoto.isBlank()) SettingsAvatarState.Empty else SettingsAvatarState.Loading,
        key1 = rawPhoto
    ) {
        val raw = rawPhoto.trim()
        if (raw.isBlank() || raw.equals("null", ignoreCase = true)) {
            value = SettingsAvatarState.Empty
            return@produceState
        }

        if (raw.startsWith("http", true) ||
            raw.startsWith("content", true) ||
            raw.startsWith("file", true)
        ) {
            value = SettingsAvatarState.Ready(raw)
            return@produceState
        }

        val local = AvatarDiskCache.localFile(appContext, raw)
        if (local.exists() && local.length() > 0L) {
            value = SettingsAvatarState.Ready(local)
            return@produceState
        }

        SignedUrlCache.getValid(raw)?.let {
            value = SettingsAvatarState.Ready(it)
            return@produceState
        }

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
                value = SettingsAvatarState.Ready(fresh)
                return@produceState
            }
        }

        value = SettingsAvatarState.Empty
    }

    Surface(
        modifier = modifier
            .clip(CircleShape),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
        contentColor = MaterialTheme.colorScheme.primary,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f))
    ) {
        when (val state = avatarState) {
            is SettingsAvatarState.Ready -> {
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
                    SettingsInitials()
                }
            }

            SettingsAvatarState.Empty,
            SettingsAvatarState.Loading -> SettingsInitials()
        }
    }
}

@Composable
private fun SettingsInitials(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.account_circle_24dp_ffffff_fill1_profile),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .padding(5.dp),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun SettingsSectionGroup(section: SettingsSection) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            text = section.title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.sp
            ),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 10.dp)
        )

        SettingsSurface(shape = RoundedCornerShape(18.dp)) {
            Column(modifier = Modifier.fillMaxWidth()) {
                section.entries.forEachIndexed { index, entry ->
                    SettingsRow(entry)
                    if (index != section.entries.lastIndex) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 60.dp, end = 14.dp)
                        ) {
                            HorizontalDivider(
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.20f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsRow(entry: SettingsEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { entry.onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            modifier = Modifier.size(36.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = entry.icon,
                    contentDescription = null,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    lineHeight = 19.sp,
                    letterSpacing = 0.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (entry.summary.isNotBlank()) {
                Text(
                    text = entry.summary,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (entry.trailing != null) {
            entry.trailing.invoke()
        } else {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(21.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.66f)
            )
        }
    }
}

@Composable
private fun SettingsSurface(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    content: @Composable () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val cardColor = if (isDark) Color(0xFF232425) else Color(0xFFFEFEFE)
    val borderColor = if (isDark) Color(0xFF333538) else Color(0xFFE3E3E4)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(cardColor)
            .border(BorderStroke(1.dp, borderColor), shape)
    ) {
        content()
    }
}

@Composable
private fun LanguagePickerSheet(
    visible: Boolean,
    selectedLanguageTag: String,
    modifier: Modifier = Modifier,
    backdrop: Backdrop,
    bottomPadding: Dp,
    onDismiss: () -> Unit,
    onLanguageSelected: (String) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val panelColor = if (isDark) {
        Color(0xFF202124).copy(alpha = 0.62f)
    } else {
        Color(0xFFE6E2E7).copy(alpha = 0.52f)
    }
    val textColor = if (isDark) Color.White else Color(0xFF1E1F24)
    val secondaryTextColor = if (isDark) Color.White.copy(alpha = 0.78f) else Color(0xFF555763)

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
        modifier = modifier,
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
                    bottom = bottomPadding
                )
                .fillMaxWidth()
                .clip(GlassChromeShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .adaptiveLiquidGlassBackdrop(
                    backdrop = backdrop,
                    shape = GlassChromeShape,
                    surfaceColor = panelColor,
                    blurDp = 4f,
                    shadow = null,
                    refractionHeightDp = 22f,
                    refractionAmountDp = 72f,
                    chromaticAberration = true
                )
        ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_language_picker_title),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    lineHeight = 22.sp
                ),
                color = textColor
            )
            Spacer(Modifier.height(6.dp))
            LanguageOption(
                title = stringResource(R.string.settings_language_english),
                languageTag = "en",
                selected = selectedLanguageTag == "en",
                textColor = textColor,
                secondaryTextColor = secondaryTextColor,
                onClick = onLanguageSelected
            )
            LanguageOption(
                title = stringResource(R.string.settings_language_spanish),
                languageTag = "es",
                selected = selectedLanguageTag == "es",
                textColor = textColor,
                secondaryTextColor = secondaryTextColor,
                onClick = onLanguageSelected
            )
        }
        }
    }
}

@Composable
private fun LanguageOption(
    title: String,
    languageTag: String,
    selected: Boolean,
    textColor: Color,
    secondaryTextColor: Color,
    onClick: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(languageTag) },
        shape = RoundedCornerShape(16.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = if (isSystemInDarkTheme()) 0.26f else 0.14f)
        } else {
            if (isSystemInDarkTheme()) {
                Color.White.copy(alpha = 0.10f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)
            }
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    lineHeight = 19.sp
                ),
                color = if (selected) textColor else secondaryTextColor
            )
            RadioButton(
                selected = selected,
                onClick = { onClick(languageTag) },
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary,
                    unselectedColor = secondaryTextColor
                )
            )
        }
    }
}

@Composable
private fun FeedbackGlassSheet(
    visible: Boolean,
    modifier: Modifier = Modifier,
    backdrop: Backdrop,
    context: Context,
    bottomPadding: Dp,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()
    val panelColor = if (isDark) {
        Color(0xFF202124).copy(alpha = 0.62f)
    } else {
        Color(0xFFE6E2E7).copy(alpha = 0.52f)
    }
    val accent = if (isDark) Color(0xFF8FC7FF) else Color(0xFF096F70)
    var feedbackText by remember { mutableStateOf("") }
    var soundEnabled by remember { mutableStateOf(true) }
    var isSending by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    val successGlow = remember { mutableStateOf(false) }
    var typingPulseTick by remember { mutableIntStateOf(0) }
    val soundPool = remember(context) { SoundPool.Builder().setMaxStreams(2).build() }
    val clickSoundId = remember(context, soundPool) { soundPool.load(context, R.raw.time_click, 1) }
    val successSoundId = remember(context, soundPool) { soundPool.load(context, R.raw.success, 1) }
    val online = remember(visible) { context.isNetworkAvailable() }
    val animatedPanel by androidx.compose.animation.animateColorAsState(
        targetValue = if (successGlow.value) {
            if (isDark) Color(0xFF123927).copy(alpha = 0.88f) else Color(0xFFD8F6E5).copy(alpha = 0.88f)
        } else {
            panelColor
        },
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "settingsFeedbackPanel"
    )

    DisposableEffect(soundPool) {
        onDispose {
            soundPool.release()
        }
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
                    bottom = bottomPadding
                )
                .fillMaxWidth()
                .heightIn(max = 390.dp)
                .clip(GlassChromeShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .adaptiveLiquidGlassBackdrop(
                    backdrop = backdrop,
                    shape = GlassChromeShape,
                    surfaceColor = animatedPanel,
                    blurDp = 4f,
                    shadow = null,
                    highlight = {
                        Highlight(
                            width = if (isDark) 0.75.dp else 0.55.dp,
                            blurRadius = 0.8.dp,
                            alpha = if (isDark) 0.85f else 1.0f,
                            style = HighlightStyle.Plain
                        )
                    },
                    refractionHeightDp = 22f,
                    refractionAmountDp = 72f,
                    chromaticAberration = true
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.send_feedback_title),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                lineHeight = 24.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SettingsRoundIconButton(
                                icon = Icons.Filled.Info,
                                tint = accent,
                                onClick = {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.settings_feedback_rules_message),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            )
                            SettingsRoundIconButton(
                                icon = Icons.Filled.Close,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                                onClick = onDismiss
                            )
                        }
                    }
                        Text(
                            text = stringResource(R.string.settings_feedback_note_subtitle),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SettingsFeedbackStatusPill(
                        online = online,
                        modifier = Modifier.weight(0.95f)
                    )
                    SettingsTypingPulse(
                        pulseKey = typingPulseTick,
                        soundEnabled = soundEnabled,
                        accent = accent,
                        modifier = Modifier.weight(0.62f)
                    )
                    SettingsSoundToggle(
                        soundEnabled = soundEnabled,
                        onSoundChange = { soundEnabled = it }
                    )
                }

                OutlinedTextField(
                    value = feedbackText,
                    onValueChange = { value ->
                        val grew = value.length > feedbackText.length
                        feedbackText = value
                        if (grew && soundEnabled) {
                            typingPulseTick += 1
                            playSettingsFeedbackSound(context, soundPool, clickSoundId, volumeScale = 0.45f)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp),
                    enabled = !isSending,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.enter_your_feedback_here),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                        )
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.55f else 0.62f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.38f else 0.56f),
                        focusedBorderColor = accent.copy(alpha = 0.72f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                    ),
                    minLines = 2,
                    maxLines = 3
                )

                Button(
                    onClick = {
                        val message = feedbackText.trim()

                        if (message.isBlank()) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.settings_feedback_empty_message),
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }

                        scope.launch {
                            isSending = true

                            val result = submitSettingsFeedback(context, message)

                            statusMessage = result
                            isSending = false

                            val sentOk =
                                result == context.getString(R.string.feedback_sent_success) ||
                                        result == context.getString(R.string.feedback_queued_success)

                            if (sentOk) {
                                feedbackText = ""
                                successGlow.value = true

                                if (soundEnabled) {
                                    playSettingsFeedbackSound(
                                        context = context,
                                        soundPool = soundPool,
                                        soundId = successSoundId,
                                        volumeScale = 0.30f
                                    )
                                }

                                kotlinx.coroutines.delay(1_000)
                                successGlow.value = false
                            }
                        }
                    },
                    enabled = !isSending,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent,
                        contentColor = if (isDark) Color(0xFF071016) else Color.White,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ){
                    if (isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(17.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.sending_feedback), fontWeight = FontWeight.SemiBold)
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.send_feedback), fontWeight = FontWeight.SemiBold)
                    }
                }

                statusMessage?.takeUnless { isSending }?.let { message ->
                    Text(
                        text = message,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        ),
                        color = if (message == stringResource(R.string.feedback_sent_success)) {
                            Color(0xFF2EAD68)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsRoundIconButton(
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.size(38.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isSystemInDarkTheme()) 0.12f else 0.09f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

@Composable
private fun SettingsFeedbackStatusPill(
    online: Boolean,
    modifier: Modifier = Modifier
) {
    val color = if (online) Color(0xFF2EAD68) else MaterialTheme.colorScheme.primary
    Surface(
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.22f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Icon(Icons.Filled.Wifi, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Text(
                text = stringResource(if (online) R.string.server_online else R.string.server_offline),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = color,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SettingsTypingPulse(
    pulseKey: Int,
    soundEnabled: Boolean,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val active by produceState(
        initialValue = false,
        key1 = pulseKey,
        key2 = soundEnabled
    ) {
        if (!soundEnabled || pulseKey <= 0) {
            value = false
            return@produceState
        }
        value = true
        kotlinx.coroutines.delay(700)
        value = false
    }

    Surface(
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    ) {
        if (!active) {
            Box(Modifier.fillMaxSize())
            return@Surface
        }

        val transition = rememberInfiniteTransition(label = "settingsFeedbackTyping")
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { index ->
                val height by transition.animateFloat(
                    initialValue = 7f,
                    targetValue = 23f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(430 + index * 80, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "settingsFeedbackPulse$index"
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(width = 5.dp, height = height.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.70f))
                )
            }
        }
    }
}

@Composable
private fun SettingsSoundToggle(
    soundEnabled: Boolean,
    onSoundChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .height(40.dp)
            .width(84.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onSoundChange(!soundEnabled) },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = if (soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(17.dp)
            )
            Switch(
                checked = soundEnabled,
                onCheckedChange = onSoundChange,
                modifier = Modifier.size(width = 38.dp, height = 26.dp)
            )
        }
    }
}

@Composable
private fun SettingsChangelogSheet(
    visible: Boolean,
    modifier: Modifier = Modifier,
    backdrop: Backdrop,
    bottomPadding: Dp,
    onDismiss: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val panelColor = if (isDark) {
        Color(0xFF202124).copy(alpha = 0.62f)
    } else {
        Color(0xFFE6E2E7).copy(alpha = 0.52f)
    }
    val textColor = if (isDark) Color.White.copy(alpha = 0.92f) else Color(0xFF1E1F24)
    var items by remember { mutableStateOf<List<UpdateBlock>?>(null) }

    LaunchedEffect(visible) {
        if (visible && items == null) {
            items = withContext(Dispatchers.IO) {
                runCatching { AppUpdateRepository.fetchRemoteUpdate().updates }
                    .getOrElse {
                        listOf(UpdateBlock(title = "Unavailable", summary = ""))
                    }
            }
        }
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
        modifier = modifier,
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
                    bottom = bottomPadding
                )
                .fillMaxWidth()
                .heightIn(max = 360.dp)
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
                    highlight = {
                        Highlight(
                            width = if (isDark) 0.75.dp else 0.55.dp,
                            blurRadius = 0.8.dp,
                            alpha = if (isDark) 0.85f else 1.0f,
                            style = HighlightStyle.Plain
                        )
                    },
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
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.settings_changelog_title),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    lineHeight = 22.sp
                                ),
                                color = textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(
                                    R.string.settings_release_date_template,
                                    BuildConfig.RELEASE_DATE
                                ),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Surface(
                            modifier = Modifier.size(38.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.12f else 0.09f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable(onClick = onDismiss),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Close",
                                    tint = textColor.copy(alpha = 0.78f),
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 250.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val updateItems = items
                    if (updateItems == null) {
                        Text(
                            text = stringResource(R.string.loading),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 13.sp,
                                lineHeight = 17.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        updateItems.forEach { item ->
                            ChangelogSheetItem(item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChangelogSheetItem(item: UpdateBlock) {
    val title = if (item.title == "Offline" || item.title == "Unavailable") {
        stringResource(R.string.settings_changelog_offline_title)
    } else {
        item.title
    }
    val summary = if (item.title == "Offline" || item.title == "Unavailable") {
        stringResource(R.string.settings_changelog_offline)
    } else {
        item.summary
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = if (isSystemInDarkTheme()) 0.18f else 0.56f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    lineHeight = 18.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (summary.isNotBlank()) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        lineHeight = 17.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item.bullets.forEach { bullet ->
                Text(
                    text = "- $bullet",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        lineHeight = 17.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private data class SettingsEntry(
    val title: String,
    val summary: String,
    val icon: ImageVector,
    val trailing: (@Composable () -> Unit)? = null,
    val onClick: () -> Unit
)

private data class SettingsSection(
    val title: String,
    val entries: List<SettingsEntry>
)

private sealed interface SettingsAvatarState {
    data object Empty : SettingsAvatarState
    data object Loading : SettingsAvatarState
    data class Ready(val data: Any) : SettingsAvatarState
}

private suspend fun submitSettingsFeedback(
    context: Context,
    message: String
): String = withContext(Dispatchers.IO) {
    val token = runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull()
    val body = JSONObject().apply {
        put("message", message)
        put("fcmToken", token)
        put("deviceInfo", context.settingsFeedbackDeviceInfo().apply {
            put("fcmToken", token)
        })
    }.toString().toRequestBody("application/json".toMediaType())

    if (!context.isNetworkAvailable()) {
        return@withContext if (context.queueSettingsFeedback(message)) {
            context.getString(R.string.feedback_queued_success)
        } else {
            context.getString(R.string.feedback_queue_error, "Local queue unavailable")
        }
    }

    val request = Request.Builder()
        .url(SETTINGS_FEEDBACK_URL)
        .addHeader("apikey", SETTINGS_FEEDBACK_SUPABASE_KEY)
        .addHeader("Authorization", "Bearer $SETTINGS_FEEDBACK_SUPABASE_KEY")
        .addHeader("Content-Type", "application/json")
        .post(body)
        .build()

    runCatching {
        OkHttpClient().newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                context.getString(R.string.feedback_sent_success)
            } else if (context.queueSettingsFeedback(message)) {
                context.getString(R.string.feedback_send_error)
            } else {
                context.getString(R.string.feedback_queue_error, "HTTP ${response.code}")
            }
        }
    }.getOrElse { error ->
        if (context.queueSettingsFeedback(message)) {
            context.getString(R.string.feedback_queued_success)
        } else {
            context.getString(R.string.feedback_send_exception_error, error.message.orEmpty())
        }
    }
}

private fun playSettingsFeedbackSound(
    context: Context,
    soundPool: SoundPool,
    soundId: Int,
    volumeScale: Float
) {
    if (soundId == 0) return
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        .toFloat()
        .coerceAtLeast(1f)
    val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
    val playVolume = (currentVolume / maxVolume) * volumeScale
    soundPool.play(soundId, playVolume, playVolume, 0, 0, 1f)
}

private fun Context.queueSettingsFeedback(message: String): Boolean {
    if (message.isBlank()) return false
    return runCatching {
        val queueFile = File(filesDir, "feedbackQueue.json")
        val feedbackQueue = if (queueFile.exists()) {
            JSONObject(queueFile.readText()).optJSONArray("queue") ?: JSONArray()
        } else {
            JSONArray()
        }
        feedbackQueue.put(message)
        queueFile.writeText(JSONObject().put("queue", feedbackQueue).toString())
    }.isSuccess
}

private fun Context.settingsFeedbackDeviceInfo(): JSONObject {
    val packageInfo = packageManager.getPackageInfo(packageName, 0)
    val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo.longVersionCode
    } else {
        @Suppress("DEPRECATION")
        packageInfo.versionCode.toLong()
    }
    val displayMetrics = resources.displayMetrics
    return JSONObject().apply {
        put("deviceModel", Build.MODEL)
        put("osVersion", Build.VERSION.RELEASE)
        put("appVersion", packageInfo.versionName)
        put("versionCode", versionCode)
        put("manufacturer", Build.MANUFACTURER)
        put("deviceId", settingsFeedbackDeviceId())
        put("screenResolution", "${displayMetrics.widthPixels} x ${displayMetrics.heightPixels}")
        put("networkType", if (isNetworkAvailable()) "ONLINE" else "OFFLINE")
        put("hardware", Build.HARDWARE)
        put("timeZone", TimeZone.getDefault().id)
        put(
            "deviceName",
            runCatching {
                Settings.Global.getString(contentResolver, Settings.Global.DEVICE_NAME)
            }.getOrNull() ?: Build.MODEL
        )
    }
}

private fun settingsFeedbackDeviceId(): String {
    val rawId = listOf(
        Build.BOARD,
        Build.BRAND,
        Build.DEVICE,
        Build.HARDWARE,
        Build.MODEL,
        Build.MANUFACTURER
    ).joinToString("-")
    val digest = java.security.MessageDigest.getInstance("SHA-256")
    return digest.digest(rawId.toByteArray()).joinToString("") { "%02x".format(it) }
}

private fun Context.isNetworkAvailable(): Boolean {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

private const val SETTINGS_FEEDBACK_SUPABASE_KEY =
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlsZ3ZkZWlxYWFpa2NvaGhwd2ZpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDQyNjQwMTEsImV4cCI6MjA1OTg0MDAxMX0.-JYW9jeUuFW8gBsmtv-OHKYbAVKsj0IAWU80zGWnwFU"
private const val SETTINGS_FEEDBACK_URL =
    "https://ylgvdeiqaaikcohhpwfi.supabase.co/rest/v1/feedback"

private const val AIRPORT_PHONE_DISPLAY = "(307) 733-7682"
private const val AIRPORT_PHONE_TEL = "3077337682"
private const val AIRPORT_OPERATIONS_EMAIL = "operations@jhairport.org"
private const val AIRPORT_SERVICES_EMAIL = "info@jhairport.org"
private const val HUMAN_RESOURCES_EMAIL = "hr@jhairport.org"
private const val LOST_AND_FOUND_EMAIL = "info@jhairport.org"
private const val COMMUNICATIONS_EMAIL = "megan.jenkins@jhairport.org"

private fun Context.openDialer(phone: String) {
    startActivity(Intent(Intent.ACTION_DIAL, "tel:$phone".toUri()))
}

private fun Context.openEmail(address: String) {
    val intent = Intent(Intent.ACTION_SENDTO, "mailto:$address".toUri())
    startActivity(Intent.createChooser(intent, null))
}

private fun Context.appVersionSummary(): String {
    val packageInfo = packageManager.getPackageInfo(packageName, 0)
    val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo.longVersionCode
    } else {
        @Suppress("DEPRECATION")
        packageInfo.versionCode.toLong()
    }
    val architecture = Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown"
    return "${packageInfo.versionName} ($versionCode) - $architecture"
}

private fun String.settingsInitials(): String {
    return trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
        .joinToString("")
        .ifEmpty { "?" }
}

private const val KEY_CAMERA_GLOW = "siri_camera_glow"
