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
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.toPath
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.preference.PreferenceManager
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.flights.studio.ui.AppLanguageManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
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
import java.util.Locale
import java.util.TimeZone
import kotlin.time.Duration.Companion.milliseconds
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
    val appPalette = LocalAppThemePalette.current
    val surfaceRoles = appThemeSurfaceRoles(appPalette, isDark)
    val settingsChromeBackdrop = rememberLayerBackdrop()
    val settingsModalBackdrop = rememberLayerBackdrop()
    val locale = LocalLocale.current.platformLocale
    val normalizedQuery = searchQuery.trim().lowercase(locale)
    val showLanguageSheet = remember { mutableStateOf(false) }
    val showThemeSheet = remember { mutableStateOf(false) }
    val showChangelog = remember { mutableStateOf(false) }
    val showFeedbackSheet = remember { mutableStateOf(false) }
    val showRateSheet = remember { mutableStateOf(false) }
    val showMenuSheet = remember { mutableStateOf(false) }
    val modalVisible = showLanguageSheet.value || showThemeSheet.value || showChangelog.value || showFeedbackSheet.value || showRateSheet.value ||
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
    var selectedAppTheme by remember {
        mutableStateOf(AppThemeStore.get(appContext))
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
                    title = "App theme",
                    summary = selectedAppTheme.label,
                    icon = Icons.Filled.ColorLens,
                    onClick = { showThemeSheet.value = true }
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
                    onClick = { showRateSheet.value = true }
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
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(surfaceRoles.page)
                )
                ProfileBackdropImageLayer(
                    modifier = Modifier.fillMaxSize(),
                    lightRes = R.drawable.light_grid_pattern,
                    darkRes = R.drawable.dark_grid_pattern,
                    imageAlpha = if (isDark) 0.72f else 0.42f,
                    scrimDark = 0.04f,
                    scrimLight = 0.00f
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    surfaceRoles.page.copy(alpha = if (isDark) 0.58f else 0.36f),
                                    surfaceRoles.glassCard.copy(alpha = if (isDark) 0.44f else 0.34f),
                                    appPalette.surfaceVariant.copy(alpha = if (isDark) 0.34f else 0.28f)
                                ),
                                start = Offset.Zero,
                                end = Offset(900f, 1400f)
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                        .padding(start = 8.dp, end = 8.dp, top = 112.dp, bottom = 92.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    visibleSections.forEach { section ->
                        SettingsSectionGroup(section)
                    }

                    AnimatedVisibility(visible = normalizedQuery.isNotBlank() && visibleSections.isEmpty()) {
                        Text(
                            text = stringResource(R.string.settings_no_results_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = surfaceRoles.subtitle,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
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
            }
        }

        if (showBottomChrome) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(
                        start = GlassChromeHorizontalPadding,
                        end = GlassChromeHorizontalPadding,
                        bottom = 76.dp
                    )
                    .zIndex(80f),
                contentAlignment = Alignment.BottomEnd
            ) {
                SettingsFloatingSearchButton(
                    visible = !modalVisible && !searchSheetVisible,
                    backdrop = settingsChromeBackdrop,
                    onClick = onOpenSearch
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

        RateUsGlassSheet(
            visible = showRateSheet.value,
            modifier = Modifier.align(Alignment.BottomCenter),
            backdrop = settingsModalBackdrop,
            context = context,
            bottomPadding = modalBottomPadding,
            onDismiss = { showRateSheet.value = false },
            onFeedback = {
                showRateSheet.value = false
                showFeedbackSheet.value = true
            }
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

        AppThemePickerSheet(
            visible = showThemeSheet.value,
            selectedTheme = selectedAppTheme,
            modifier = Modifier.align(Alignment.BottomCenter),
            backdrop = settingsModalBackdrop,
            bottomPadding = modalBottomPadding,
            onDismiss = { showThemeSheet.value = false },
            onThemeSelected = { preset ->
                selectedAppTheme = preset
                AppThemeStore.set(appContext, preset)
                showThemeSheet.value = false
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
    backdrop: LayerBackdrop,
    onOpenHome: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenContacts: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenMenu: () -> Unit
) {
    GlassBottomTabBar(
        modifier = modifier,
        backdrop = backdrop,
        contentView = null,
        tabs = listOf(
            GlassBottomTabItem(
                label = stringResource(R.string.Home),
                icon = Icons.Filled.Home,
                selected = false,
                onClick = onOpenHome,
                lanternColor = LocalAppThemePalette.current.accent
            ),
            GlassBottomTabItem(
                label = stringResource(R.string.total_contacts),
                icon = Icons.Filled.Groups,
                selected = false,
                onClick = onOpenContacts,
                lanternColor = LocalAppThemePalette.current.warm
            ),
            GlassBottomTabItem(
                label = stringResource(R.string.contacts_bottom_notes),
                icon = Icons.AutoMirrored.Filled.Article,
                selected = false,
                onClick = onOpenNotes,
                lanternColor = LocalAppThemePalette.current.rose
            ),
            GlassBottomTabItem(
                label = stringResource(R.string.menu_settings),
                icon = Icons.Filled.SettingsIcon,
                selected = true,
                onClick = onOpenSettings,
                lanternColor = LocalAppThemePalette.current.action
            ),
            GlassBottomTabItem(
                label = stringResource(R.string.settings_menu_tab),
                icon = Icons.Filled.Menu,
                selected = false,
                onClick = onOpenMenu,
                lanternColor = LocalAppThemePalette.current.action
            )
        )
    )
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
    val roles = appThemeSurfaceRoles(LocalAppThemePalette.current, isDark)
    val iconColor = roles.iconContent
    val buttonColor = roles.iconSurface
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = 0.70f, stiffness = 520f),
        label = "settingsSearchFabPressScale"
    )

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
                .size(58.dp)
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                }
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
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = stringResource(R.string.search),
                tint = iconColor,
                modifier = Modifier.size(25.dp)
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

    val avatarRoles = appThemeSurfaceRoles(LocalAppThemePalette.current, isSystemInDarkTheme())
    Surface(
        modifier = modifier
            .clip(CircleShape),
        shape = CircleShape,
        color = avatarRoles.iconSurface,
        contentColor = avatarRoles.iconContent,
        border = BorderStroke(1.dp, avatarRoles.border)
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
    val isDark = isSystemInDarkTheme()
    val roles = appThemeSurfaceRoles(LocalAppThemePalette.current, isDark)
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            text = section.title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.sp
            ),
            color = roles.section,
            modifier = Modifier.padding(horizontal = 10.dp)
        )

        AppThemeSectionSurface(shape = RoundedCornerShape(18.dp)) {
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
                                color = roles.border.copy(alpha = 0.58f)
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
    val isDark = isSystemInDarkTheme()
    val palette = LocalAppThemePalette.current
    val surfaceRoles = appThemeSurfaceRoles(palette, isDark)
    val rowAccent = surfaceRoles.section
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { entry.onClick() }
            .padding(horizontal = 2.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        ListItem(
            leadingContent = {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape,
                    color = rowAccent.copy(alpha = if (isDark) 0.18f else 0.14f),
                    contentColor = rowAccent
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = entry.icon,
                            contentDescription = null,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            },
            supportingContent = if (entry.summary.isNotBlank()) {
                {
                    Text(
                        text = entry.summary,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                null
            },
            trailingContent = {
                if (entry.trailing != null) {
                    entry.trailing.invoke()
                } else {
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = surfaceRoles.chevron
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent,
                headlineColor = surfaceRoles.title,
                supportingColor = surfaceRoles.subtitle
            )
        ) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    lineHeight = 19.sp,
                    letterSpacing = 0.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
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
private fun AppThemePickerSheet(
    visible: Boolean,
    selectedTheme: AppThemePreset,
    modifier: Modifier = Modifier,
    backdrop: Backdrop,
    bottomPadding: Dp,
    onDismiss: () -> Unit,
    onThemeSelected: (AppThemePreset) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val appPalette = LocalAppThemePalette.current
    val panelColor = if (isDark) {
        appPalette.glass.copy(alpha = 0.78f)
    } else {
        appPalette.glass.copy(alpha = 0.72f)
    }
    val textColor = if (isDark) Color.White else Color(0xFF111820)
    val secondaryTextColor = if (isDark) Color.White.copy(alpha = 0.82f) else Color(0xFF33404B)

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
                .heightIn(max = 620.dp)
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
                    blurDp = 10f,
                    shadow = null,
                    refractionHeightDp = 22f,
                    refractionAmountDp = 72f,
                    chromaticAberration = true
                )
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            appPalette.card.copy(alpha = if (isDark) 0.24f else 0.18f),
                            appPalette.surfaceVariant.copy(alpha = if (isDark) 0.22f else 0.16f),
                            appPalette.glass.copy(alpha = if (isDark) 0.18f else 0.14f)
                        ),
                        start = Offset.Zero,
                        end = Offset(850f, 1100f)
                    ),
                    GlassChromeShape
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 10.dp, top = 18.dp, end = 10.dp, bottom = 22.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "App theme",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        lineHeight = 22.sp
                    ),
                    color = textColor
                )
                Text(
                    text = "Choose the soft color style used by splash, tabs, and glass accents.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
                    color = secondaryTextColor
                )
                Spacer(Modifier.height(4.dp))
                AppThemeStore.presets.forEach { preset ->
                    AppThemeOption(
                        preset = preset,
                        selected = selectedTheme == preset,
                        onClick = onThemeSelected
                    )
                }
            }
        }
    }
}

@Composable
private fun AppThemeOption(
    preset: AppThemePreset,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: (AppThemePreset) -> Unit
) {
    val isDark = isSystemInDarkTheme()

    val palette = remember(preset, isDark) {
        appThemePaletteFor(
            preset = preset,
            isDark = isDark
        )
    }

    val shape = RoundedCornerShape(22.dp)

    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.99f,
        animationSpec = tween(
            durationMillis = 180,
            easing = FastOutSlowInEasing
        ),
        label = "theme_card_scale_${preset.name}"
    )

    // Fully opaque surface — no transparent card color
    val cardColor = if (selected) {
        palette.surfaceVariant
    } else {
        palette.card
    }

    val titleColor = if (isDark) {
        Color(0xFFF6F8FB)
    } else {
        Color(0xFF101418)
    }

    val summaryColor = if (isDark) {
        Color(0xFFC4CCD6)
    } else {
        Color(0xFF4F5B66)
    }

    val borderColor = if (selected) {
        palette.accent
    } else {
        palette.outline
    }

    Box(
        modifier = modifier
            .height(156.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            // Much smaller shadow
            .shadow(
                elevation = if (selected) 3.dp else 1.dp,
                shape = shape,
                clip = false
            )
            .clip(shape)
            .background(
                color = cardColor,
                shape = shape
            )
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = borderColor,
                shape = shape
            )
            .clickable(
                interactionSource = remember {
                    MutableInteractionSource()
                },
                indication = null,
                onClick = {
                    onClick(preset)
                }
            )
    ) {
        // Solid colorful strip — not transparent
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(5.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            palette.accent,
                            palette.warm,
                            palette.rose
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 12.dp,
                    top = 15.dp,
                    end = 12.dp,
                    bottom = 12.dp
                ),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = preset.label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        lineHeight = 18.sp
                    ),
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (selected) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(palette.accent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Selected",
                            tint = palette.actionContent,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            AppThemeShapePreview(
                preset = preset,
                palette = palette,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            )

            Text(
                text = preset.summary,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                ),
                color = summaryColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    palette.accent,
                    palette.warm,
                    palette.rose
                ).forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(11.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AppThemeShapePreview(
    preset: AppThemePreset,
    palette: AppThemePalette,
    modifier: Modifier = Modifier
) {
    val paths = rememberThemePreviewPaths(preset)
    val isDark = isSystemInDarkTheme()
    Box(
        modifier = modifier
            .padding(horizontal = 5.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        val colors = listOf(
            palette.accent.copy(alpha = if (isDark) 0.98f else 0.92f),
            palette.warm.copy(alpha = if (isDark) 0.94f else 0.90f),
            palette.rose.copy(alpha = if (isDark) 0.96f else 0.92f)
        )
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            paths.take(2).forEachIndexed { index, path ->
                val shape = remember(path) { MaterialPathShape(path) }
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(shape)
                        .background(colors[index % colors.size])
                )
            }
        }
    }
}

private class MaterialPathShape(
    private val source: Path
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val bounds = source.getBounds()
        val scale = minOf(
            size.width / bounds.width.coerceAtLeast(0.001f),
            size.height / bounds.height.coerceAtLeast(0.001f)
        )
        val dx = (size.width - bounds.width * scale) / 2f - bounds.left * scale
        val dy = (size.height - bounds.height * scale) / 2f - bounds.top * scale
        val matrix = android.graphics.Matrix().apply {
            setScale(scale, scale)
            postTranslate(dx, dy)
        }
        val outPath = android.graphics.Path()
        source.asAndroidPath().transform(matrix, outPath)
        return Outline.Generic(outPath.asComposePath())
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun rememberThemePreviewPaths(preset: AppThemePreset): List<Path> {
    val pill = MaterialShapes.Pill.toPath()
    val oval = MaterialShapes.Oval.toPath()
    val bun = MaterialShapes.Bun.toPath()
    val puffy = MaterialShapes.Puffy.toPath()
    val slanted = MaterialShapes.Slanted.toPath()
    val sunny = MaterialShapes.Sunny.toPath()
    val flower = MaterialShapes.Flower.toPath()
    val boom = MaterialShapes.Boom.toPath()
    val arrow = MaterialShapes.Arrow.toPath()
    val square = MaterialShapes.Square.toPath()
    val clamShell = MaterialShapes.ClamShell.toPath()
    val semiCircle = MaterialShapes.SemiCircle.toPath()
    val clover4 = MaterialShapes.Clover4Leaf.toPath()
    val fan = MaterialShapes.Fan.toPath()
    val heart = MaterialShapes.Heart.toPath()
    val gem = MaterialShapes.Gem.toPath()
    val diamond = MaterialShapes.Diamond.toPath()
    val softBurst = MaterialShapes.SoftBurst.toPath()
    val softBoom = MaterialShapes.SoftBoom.toPath()

    return remember(
        preset,
        pill,
        oval,
        bun,
        puffy,
        slanted,
        sunny,
        flower,
        boom,
        arrow,
        square,
        clamShell,
        semiCircle,
        clover4,
        fan,
        heart,
        gem,
        diamond,
        softBurst,
        softBoom
    ) {
        when (preset) {
            AppThemePreset.Classic -> listOf(pill, oval)
            AppThemePreset.Sky -> listOf(bun, puffy)
            AppThemePreset.Sunset -> listOf(slanted, sunny)
            AppThemePreset.Aurora -> listOf(flower, boom)
            AppThemePreset.Graphite -> listOf(arrow, square)
            AppThemePreset.Ocean -> listOf(clamShell, semiCircle)
            AppThemePreset.Meadow -> listOf(clover4, fan)
            AppThemePreset.Candy -> listOf(heart, puffy)
            AppThemePreset.Royal -> listOf(gem, diamond)
            AppThemePreset.Ember -> listOf(softBurst, softBoom)
        }
    }
}

private data class SettingsRatingBreakdown(
    val five: Int = 0,
    val four: Int = 0,
    val three: Int = 0,
    val two: Int = 0,
    val one: Int = 0
) {
    val total: Int get() = five + four + three + two + one
    val average: Float
        get() = if (total > 0) {
            ((five * 5) + (four * 4) + (three * 3) + (two * 2) + one).toFloat() / total.toFloat()
        } else {
            0f
        }

    fun countFor(stars: Int): Int = when (stars) {
        5 -> five
        4 -> four
        3 -> three
        2 -> two
        else -> one
    }
}

@Composable
private fun RateUsGlassSheet(
    visible: Boolean,
    modifier: Modifier = Modifier,
    backdrop: Backdrop,
    context: Context,
    bottomPadding: Dp,
    onDismiss: () -> Unit,
    onFeedback: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val panelColor = if (isDark) {
        Color(0xFF202124).copy(alpha = 0.62f)
    } else {
        Color(0xFFE6E2E7).copy(alpha = 0.52f)
    }
    val textColor = if (isDark) Color.White.copy(alpha = 0.92f) else Color(0xFF1E1F24)
    val mutedColor = if (isDark) Color.White.copy(alpha = 0.64f) else Color(0xFF5A5D66)
    val tileColor = if (isDark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.72f)
    val starColor = if (isDark) Color(0xFFFFD76A) else Color(0xFFFFB300)
    val accent = if (isDark) Color(0xFF8FC7FF) else Color(0xFF096F70)
    val prefs = remember(context) {
        context.getSharedPreferences("RateUsSubmitCount", Context.MODE_PRIVATE)
    }
    var selectedRating by remember(visible) { mutableIntStateOf(0) }
    var submitCount by remember(visible) { mutableIntStateOf(prefs.getInt("submitCount", 0)) }
    var statusMessage by remember(visible) { mutableStateOf<String?>(null) }
    var breakdown by remember { mutableStateOf(SettingsRatingBreakdown()) }

    DisposableEffect(visible) {
        if (!visible) {
            return@DisposableEffect onDispose { }
        }
        val ref = FirebaseDatabase.getInstance().getReference("ratings")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val ratings = snapshot.value as? Map<*, *>
                breakdown = SettingsRatingBreakdown(
                    five = (ratings?.get("5") as? Long)?.toInt() ?: 0,
                    four = (ratings?.get("4") as? Long)?.toInt() ?: 0,
                    three = (ratings?.get("3") as? Long)?.toInt() ?: 0,
                    two = (ratings?.get("2") as? Long)?.toInt() ?: 0,
                    one = (ratings?.get("1") as? Long)?.toInt() ?: 0
                )
            }

            override fun onCancelled(error: DatabaseError) {
                statusMessage = "Could not load ratings."
            }
        }
        ref.addValueEventListener(listener)
        onDispose { ref.removeEventListener(listener) }
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
                .heightIn(max = 560.dp)
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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.bottomsheetlogo),
                        contentDescription = stringResource(R.string.card_view_icon),
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                    )
                    Text(
                        text = stringResource(R.string.rate_us),
                        modifier = Modifier.weight(1f),
                        color = textColor,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    SettingsModalIconButton(
                        icon = Icons.AutoMirrored.Filled.Send,
                        tint = accent,
                        containerColor = tileColor,
                        onClick = onFeedback
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(GlassChromeInnerShape)
                        .background(tileColor)
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(104.dp)
                            .clip(CircleShape)
                            .border(8.dp, starColor.copy(alpha = 0.34f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.1f", breakdown.average),
                            color = textColor,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (1..5).forEach { star ->
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = "$star stars",
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        selectedRating = star
                                        statusMessage = null
                                    }
                                    .padding(4.dp),
                                tint = if (star <= selectedRating) starColor else mutedColor.copy(alpha = 0.42f)
                            )
                        }
                    }
                    Text(
                        text = if (selectedRating > 0) "$selectedRating / 5" else "Tap a star",
                        color = mutedColor,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(GlassChromeInnerShape)
                        .background(tileColor)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    (5 downTo 1).forEach { stars ->
                        RatingBreakdownRow(
                            stars = stars,
                            count = breakdown.countFor(stars),
                            total = breakdown.total,
                            textColor = textColor,
                            mutedColor = mutedColor,
                            starColor = starColor
                        )
                    }
                }

                statusMessage?.let { message ->
                    Text(
                        text = message,
                        modifier = Modifier.fillMaxWidth(),
                        color = if (message.contains("thank", ignoreCase = true)) accent else MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                Button(
                    onClick = {
                        if (selectedRating <= 0) {
                            statusMessage = "Please select a rating before submitting."
                            return@Button
                        }
                        if (submitCount >= 3) {
                            statusMessage = "Submitted"
                            return@Button
                        }
                        val nextSubmitCount = submitCount + 1
                        submitCount = nextSubmitCount
                        prefs.edit { putInt("submitCount", nextSubmitCount) }
                        submitSettingsRating(selectedRating) { success ->
                            statusMessage = if (success) "Rating submitted. Thank you!" else "Failed to submit rating."
                        }
                    },
                    enabled = submitCount < 3,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent.copy(alpha = if (isDark) 0.34f else 0.20f),
                        contentColor = if (isDark) Color.White else accent,
                        disabledContainerColor = mutedColor.copy(alpha = 0.16f),
                        disabledContentColor = mutedColor
                    ),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text(
                        text = if (submitCount >= 3) "Submitted" else stringResource(R.string.submit),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsModalIconButton(
    icon: ImageVector,
    tint: Color,
    containerColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(stiffness = 520f, dampingRatio = 0.78f),
        label = "settingsModalIconScale"
    )
    Box(
        modifier = Modifier
            .size(42.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(containerColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(21.dp))
    }
}

@Composable
private fun RatingBreakdownRow(
    stars: Int,
    count: Int,
    total: Int,
    textColor: Color,
    mutedColor: Color,
    starColor: Color
) {
    val fraction = if (total > 0) (count.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.width(72.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "$stars",
                color = textColor,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = starColor,
                modifier = Modifier.size(16.dp)
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
                .clip(CircleShape)
                .background(mutedColor.copy(alpha = 0.16f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(starColor)
            )
        }
        Text(
            text = count.toString(),
            modifier = Modifier.width(34.dp),
            color = mutedColor,
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

private fun submitSettingsRating(
    rating: Int,
    onComplete: (Boolean) -> Unit
) {
    val currentRatingRef = FirebaseDatabase.getInstance().getReference("ratings").child(rating.toString())
    currentRatingRef.runTransaction(object : Transaction.Handler {
        override fun doTransaction(currentData: MutableData): Transaction.Result {
            val currentCount = currentData.getValue(Long::class.java) ?: 0L
            currentData.value = currentCount + 1L
            return Transaction.success(currentData)
        }

        override fun onComplete(
            error: DatabaseError?,
            committed: Boolean,
            currentData: DataSnapshot?
        ) {
            onComplete(error == null && committed)
        }
    })
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

                                kotlinx.coroutines.delay(1_000.milliseconds)
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
        kotlinx.coroutines.delay(700.milliseconds)
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
