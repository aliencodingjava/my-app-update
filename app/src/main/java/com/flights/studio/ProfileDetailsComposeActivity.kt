@file:Suppress("DEPRECATION")

package com.flights.studio

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.window.OnBackInvokedDispatcher
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileDetailsComposeActivity : AppCompatActivity() {

    private lateinit var userPrefs: UserPreferencesManager

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userPrefs = UserPreferencesManager(this)

        // ✅ Predictive Back Support for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT
            ) {
                finish()
                overridePendingTransition(0, R.anim.zoom_out)
            }
        }

        setContent {
            val isDark = isSystemInDarkTheme()
            val view = LocalView.current

            SideEffect {
                val window = (view.context as Activity).window
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !isDark
                    isAppearanceLightNavigationBars = !isDark
                }
            }

            // ✅ SINGLE SOURCE OF TRUTH
            val profileThemeModeState =
                rememberSaveable { mutableIntStateOf(userPrefs.profileThemeMode) }

            FlightsTheme(
                profileBackdropStyle = when (profileThemeModeState.intValue) {
                    1 -> ProfileBackdropStyle.Glass
                    2 -> ProfileBackdropStyle.Blur
                    3 -> ProfileBackdropStyle.Solid
                    else -> ProfileBackdropStyle.Auto
                }
            ) {
                ProfileDetailsRoute(
                    userPrefs = userPrefs,
                    hostActivity = this@ProfileDetailsComposeActivity,
                    onNavigateBack = {
                        finish()
                        overridePendingTransition(0, R.anim.zoom_out)
                    },

                    // ✅ Route asks, Activity decides
                    themeMode = profileThemeModeState.intValue,
                    onThemeModeChange = { newMode ->
                        profileThemeModeState.intValue = newMode
                        userPrefs.profileThemeMode = newMode
                    }
                )
            }
        }


    }

    @SuppressLint("GestureBackNavigation")
    @Deprecated(
        "Deprecated in favor of OnBackPressedDispatcher",
        ReplaceWith("onBackPressedDispatcher.onBackPressed()")
    )
    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(0, R.anim.zoom_out)
    }
}

// ----------------------------
// UI state builder (same logic as XML activity)
// ----------------------------
private data class ProfileUiState(
    val displayName: String,
    val secondaryTextRaw: String,
    val initials: String,
    val photoUri: Uri?,
    val emailRaw: String,
    val bioRaw: String,
    val birthdayRaw: String
)

private fun buildProfileUiState(userPrefs: UserPreferencesManager): ProfileUiState {
    val rawName = userPrefs.userName?.trim().orEmpty()
    val tokens = rawName.split(Regex("\\s+")).filter { it.isNotBlank() }

    val leadingOrnaments = tokens.takeWhile { t -> t.none { ch -> ch.isLetterOrDigit() } }
    val trailingOrnaments = tokens.takeLastWhile { t -> t.none { ch -> ch.isLetterOrDigit() } }

    val coreWords = tokens
        .filter { it.any { ch -> ch.isLetter() } }
        .take(3)

    val displayName = buildList {
        addAll(leadingOrnaments)
        addAll(coreWords)
        addAll(trailingOrnaments)
    }.joinToString(" ")

    val initials = coreWords
        .mapNotNull { it.firstOrNull { char -> char.isLetter() }?.uppercaseChar() }
        .joinToString("")

    val secondaryRaw = when {
        !userPrefs.userPhone.isNullOrBlank() -> userPrefs.userPhone.orEmpty()
        !userPrefs.userEmail.isNullOrBlank() -> userPrefs.userEmail.orEmpty()
        else -> ""
    }

    return ProfileUiState(
        displayName = displayName,
        secondaryTextRaw = secondaryRaw,
        initials = initials,
        photoUri = userPrefs.getUserPhotoUri(),
        emailRaw = userPrefs.userEmail.orEmpty(),
        bioRaw = userPrefs.userBio.orEmpty(),
        birthdayRaw = userPrefs.userBirthday.orEmpty()
    )
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileDetailsRoute(
    userPrefs: UserPreferencesManager,
    hostActivity: AppCompatActivity,
    onNavigateBack: () -> Unit,
    themeMode: Int,
    onThemeModeChange: (Int) -> Unit
) {
    val context = LocalContext.current
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    // trigger recomposition like loadAndDisplayProfile() + invalidateOptionsMenu()
    val scope = rememberCoroutineScope()
    val createProfileMsg = stringResource(R.string.prompt_create_profile)
    val comingSoonMsg = stringResource(R.string.coming_soon)

    var refreshTick by remember { mutableIntStateOf(0) }


    val cycleTheme: () -> Unit = {
        val next = (themeMode + 1) % 4  // ✅ 4 modes now
        onThemeModeChange(next)
        refreshTick += 1

        FancyPillToast.show(
            hostActivity,
            "🎨 Theme: " + when (next) {
                0 -> "Auto"
                1 -> "Glass"
                2 -> "Blur"
                else -> "Solid"
            },
            1600L
        )
    }


    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult

        // Keep local permission (good even if upload fails)
        try {
            context.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
        }

        // Optional: show immediately locally (nice UX)
        userPrefs.setPhotoString(uri.toString())
        userPrefs.userInitials = null
        refreshTick += 1

        // ✅ NOW upload to Supabase + update profile
        scope.launch {
            try {
                val session = SupabaseManager.client.auth.currentSessionOrNull() ?: return@launch
                val token = session.accessToken
                val userId = session.user?.id ?: return@launch

                // Upload to storage -> get public url
                val uploadedUrl = SupabaseStorageUploader.uploadProfilePhotoAndGetPublicUrl(
                    context = context,
                    userId = userId,
                    authToken = token,
                    photoUri = uri,
                    bucket = "profile-photos"
                )

                // ✅ If upload failed, show pill and stop
                if (uploadedUrl.isNullOrBlank()) {
                    withContext(Dispatchers.Main) {
                        FancyPillToast.show(
                            activity = hostActivity,
                            text = "❌ Upload failed",
                            durationMs = 2500L
                        )
                    }
                    return@launch
                }

                // ✅ Update DB row (profiles.photo_uri)
                val ok = SupabaseStorageUploader.updateProfilePhotoUrl(
                    userId = userId,
                    authToken = token,
                    photoUrl = uploadedUrl
                )

                withContext(Dispatchers.Main) {
                    if (ok) {
                        // Save remote url into prefs (cache bust so it refreshes)
                        userPrefs.setPhotoString("${uploadedUrl}?v=${System.currentTimeMillis()}")
                        userPrefs.userInitials = null
                        refreshTick += 1

                        FancyPillToast.show(
                            activity = hostActivity,
                            text = "✅ Photo updated",
                            durationMs = 2000L
                        )
                    } else {
                        FancyPillToast.show(
                            activity = hostActivity,
                            text = "❌ Failed to update profile photo",
                            durationMs = 2500L
                        )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    FancyPillToast.show(
                        activity = hostActivity,
                        text = "❌ Error: ${e.message ?: "unknown"}",
                        durationMs = 2500L
                    )
                }
            }
        }
    }


    // onResume refresh (same as XML activity)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) refreshTick += 1
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    BackHandler { onNavigateBack() }

    val ui = remember(refreshTick) { buildProfileUiState(userPrefs) }

    val hasProfile = !userPrefs.userName.isNullOrBlank()
    val isLoggedIn = userPrefs.isLoggedIn


    val hasPhoto = ui.photoUri != null

    val useGlassBackdrop = when (themeMode) {
        0 -> isLoggedIn && hasPhoto   // Auto
        1 -> true                     // Glass
        2 -> true                     // ✅ Blur also uses glass overlay
        else -> false                 // Solid
    }


    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn) return@LaunchedEffect

        try {
            val session =
                SupabaseManager.client.auth.currentSessionOrNull() ?: return@LaunchedEffect
            val userId = session.user?.id ?: return@LaunchedEffect
            val token = session.accessToken

            val profile =
                SupabaseProfileDownloader.fetchProfile(userId, token) ?: return@LaunchedEffect
            val remotePhotoBase = profile.photoUri?.trim().orEmpty()

            if (remotePhotoBase.isNotBlank()) {
                // compare BASE urls (ignore ?v=)
                val currentBase = userPrefs.getUserPhotoUri()
                    ?.toString()
                    ?.substringBefore("?v=")
                    .orEmpty()

                if (currentBase != remotePhotoBase) {
                    // Only cache-bust when it actually changed
                    userPrefs.setPhotoString("${remotePhotoBase}?v=${System.currentTimeMillis()}")
                    userPrefs.userInitials = null
                    refreshTick += 1
                }
            }

            // ⚠️ don’t refreshTick += 1 unconditionally here anymore
        } catch (_: Exception) {
            // keep existing
        }
    }


    var showLogoutDialog by remember { mutableStateOf(false) }

    val openLogoutDialog = remember { { showLogoutDialog = true } }
    val dismissLogoutDialog = remember { { showLogoutDialog = false } }


    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = dismissLogoutDialog,
            title = { Text(stringResource(R.string.title_log_out)) },
            text = { Text(stringResource(R.string.confirm_log_out)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        dismissLogoutDialog()
                        userPrefs.clear()
                        refreshTick += 1
                    }
                ) {
                    Text(stringResource(R.string.yes))
                }
            },
            dismissButton = {
                TextButton(onClick = dismissLogoutDialog) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }


    val scrollState = rememberScrollState()
    val pageBgColor = MaterialTheme.colorScheme.background
    val pageBackdrop = rememberLayerBackdrop {
        drawRect(pageBgColor)
        drawContent()
    }

    Scaffold(
        containerColor = Color.Transparent,

        topBar = {
            Surface(
                shape = RoundedCornerShape(
                    bottomStart = 24.dp,
                    bottomEnd = 24.dp
                ),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 0.dp,
                shadowElevation = 3.dp
            ) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.title_my_profile),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold // ⭐ key choice
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        Box(
                            modifier = Modifier.wrapContentSize(Alignment.TopEnd)
                        ) {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "Menu")
                            }

                            // ✅ M3 Expressive popup menu (2 blocks + gap, Log in/out separated)
                            ProfileExpressiveMenuPopup(
                                expanded = menuOpen,
                                onDismiss = { menuOpen = false },
                                offset = DpOffset(x = (-16).dp, y = 8.dp),

                                hasProfile = hasProfile,
                                isLoggedIn = isLoggedIn,

                                onEdit = {
                                    menuOpen = false
                                    showCreateProfileSheet(
                                        activity = hostActivity,
                                        isEdit = true,
                                        onProfileSaved = { refreshTick += 1 }
                                    )
                                },
                                onChangePhoto = {
                                    menuOpen = false
                                    if (isLoggedIn) {
                                        photoPickerLauncher.launch(arrayOf("image/*"))
                                    } else {
                                        FancyPillToast.show(
                                            hostActivity,
                                            hostActivity.getString(R.string.prompt_create_profile),
                                            3000L
                                        )
                                    }
                                },
                                onPrivacy = {
                                    menuOpen = false
                                    FancyPillToast.show(
                                        hostActivity,
                                        hostActivity.getString(R.string.coming_soon),
                                        3000L
                                    )
                                },
                                onLoginLogout = {
                                    menuOpen = false
                                    if (isLoggedIn) {
                                        openLogoutDialog()
                                    } else {
                                        showCreateProfileSheet(
                                            activity = hostActivity,
                                            isEdit = false,
                                            onProfileSaved = { refreshTick += 1 }
                                        )
                                    }
                                },

                                // 🔒 Everything OFF for now
                                flags = MenuFeatureFlags(
                                    vibrant = true,
                                    groupLabels = false,
                                    groupDividers = false,
                                    supportingText = true,
                                    trailingIcon = false,
                                    badge = true,
                                    trailingText = false,
                                    persistentSelection = false
                                )
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        }

    ) { padding ->


        Box(Modifier.fillMaxSize()) {

            // 1) record page background into backdrop
            Box(
                Modifier
                    .matchParentSize()
                    .layerBackdrop(pageBackdrop)
                    .profileBackdropBackground(RoundedCornerShape(0.dp))
            )

            // 2) overlay progressive blur that reads pageBackdrop
//          BottomProgressiveBlurStrip(
//        backdrop = pageBackdrop,
//        modifier = Modifier
//            .fillMaxWidth()
//            .height(66.dp)
//            .align(Alignment.TopStart)
//            .offset(y = (maxHeight * 0.5f) - 33.dp) // center strip on the line
//            .zIndex(1f)
//    )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp, bottom = 24.dp)
            ) {
// ----------------------------
// Header card (FULL)
// ----------------------------
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.elevatedCardColors(
                        // ✅ Always transparent; we paint inside so it never “disappears”
                        containerColor = Color.Transparent
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
                ) {
                    val headerShape = RoundedCornerShape(28.dp)
                    val headerBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f)

                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(headerShape)
                    ) {
                        // ✅ 1) BASE BACKGROUND ALWAYS (Solid mode stays visible)
                        Box(
                            Modifier
                                .matchParentSize()
                                .background(headerBg)
                        )

                        // ✅ 2) GLASS OVERLAY (reads PAGE backdrop behind all cards)
                        Box(
                            Modifier
                                .matchParentSize()
                                .clip(headerShape)
                                .profileGlassBackdrop(
                                    backdrop = pageBackdrop,   // ✅ THIS is the key
                                    shape = headerShape, enabled = useGlassBackdrop
                                )
                        )

                        // ✅ 3) CONTENT ON TOP (sharp)
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 0.dp)
                        ) {
                            Text(
                                text = "Profile",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(
                                    start = 20.dp,
                                    top = 14.dp,
                                    bottom = 4.dp
                                )
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        start = 16.dp, end = 16.dp, top = 20.dp, bottom = 18.dp
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val alpha = if (isLoggedIn) 1f else 0.4f
                                val avatarShape = RoundedCornerShape(46.dp)
                                var avatarFailed by remember(ui.photoUri) { mutableStateOf(false) }

                                Surface(
                                    modifier = Modifier
                                        .size(92.dp)
                                        .alpha(alpha)
                                        .clip(avatarShape)
                                        .clickable {
                                            if (!isLoggedIn) {
                                                FancyPillToast.show(
                                                    hostActivity,
                                                    hostActivity.getString(R.string.prompt_create_profile),
                                                    3000L
                                                )
                                            }
                                        },
                                    tonalElevation = 2.dp,
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = avatarShape
                                ) {
                                    if (ui.photoUri != null && !avatarFailed) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(ui.photoUri)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize(),
                                            onError = { avatarFailed = true }
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = ui.initials.ifEmpty { "?" },
                                                style = MaterialTheme.typography.headlineSmall.copy(
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.width(14.dp))

                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = ui.displayName.ifEmpty { stringResource(R.string.default_user_name) },
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(Modifier.height(6.dp))

                                    Text(
                                        text = ui.secondaryTextRaw.ifEmpty { stringResource(R.string.unknown_contact) },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(Modifier.height(6.dp))

                                    Text(
                                        text = if (isLoggedIn) "🟢 Signed in" else "⚪ Guest mode",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                val elevated = LocalProfileBackdropStyle.current != ProfileBackdropStyle.Solid

                // Info card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 0.dp,
                    shadowElevation = if (elevated) 3.dp else 0.dp // ✅ SAME as top bar
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {

                        // ✅ Section title (this is the upgrade)
                        Text(
                            text = "Information",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(start = 20.dp, bottom = 6.dp)
                        )
                        InfoRow(
                            iconRes = R.drawable.ic_oui_email,
                            label = stringResource(R.string.email),
                            value = stringResource(R.string.label_email, ui.emailRaw)
                        )
                        Divider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        InfoRow(
                            iconRes = R.drawable.ic_oui_info,
                            label = stringResource(R.string.bio),
                            value = stringResource(R.string.label_bio, ui.bioRaw)
                        )
                        Divider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        InfoRow(
                            iconRes = R.drawable.ic_oui_calendar_year,
                            label = stringResource(R.string.birthday),
                            value = stringResource(R.string.label_birthday, ui.birthdayRaw)
                        )
                    }
                }
                QuickActionsCard(
                    isLoggedIn = isLoggedIn,
                    onEdit = {
                        showCreateProfileSheet(
                            activity = hostActivity,
                            isEdit = true,
                            onProfileSaved = { refreshTick += 1 }
                        )
                    },
                    onLogin = {
                        showCreateProfileSheet(
                            activity = hostActivity,
                            isEdit = false,
                            onProfileSaved = { refreshTick += 1 }
                        )
                    },
                    onTheme = cycleTheme,
                    onRequireProfile = {
                        FancyPillToast.show(hostActivity, createProfileMsg, 3000L)
                    },
                    onPrivacy = {
                        FancyPillToast.show(hostActivity, comingSoonMsg, 3000L)
                    }
                )

                Spacer(Modifier.height(24.dp))
            }

        }
    }


}

@Composable
fun QuickActionsCard(
    isLoggedIn: Boolean,
    onEdit: () -> Unit,
    onLogin: () -> Unit,
    onTheme: () -> Unit,
    onRequireProfile: () -> Unit,
    onPrivacy: () -> Unit
) {
    val style = LocalProfileBackdropStyle.current
    val elevated = style != ProfileBackdropStyle.Solid

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp,
        shadowElevation = if (elevated) 3.dp else 0.dp // ✅ same as top bar
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                text = "Quick actions",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            var actionsWidthPx by remember { mutableIntStateOf(0) }
            val density = LocalDensity.current
            val actionsWidthDp = remember(actionsWidthPx, density) {
                with(density) { actionsWidthPx.toDp() }
            }
            val compact = actionsWidthDp in 1.dp..359.dp

            val primaryIcon = if (isLoggedIn) Icons.Filled.Edit else Icons.Filled.Login
            val primaryLabel = if (isLoggedIn) "Edit" else "Log in"
            val primaryClick = if (isLoggedIn) onEdit else onLogin

            // Theme is always available, but if user not logged in, you can require profile if you prefer:
            val themeClick = if (isLoggedIn) onTheme else onRequireProfile
            val themeAlpha = if (isLoggedIn) 1f else 0.6f

            if (!compact) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onSizeChanged { actionsWidthPx = it.width },
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        modifier = Modifier.weight(1f),
                        onClick = primaryClick,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                    ) {
                        Icon(primaryIcon, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            primaryLabel,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    FilledTonalButton(
                        modifier = Modifier
                            .weight(1f)
                            .alpha(themeAlpha),
                        onClick = themeClick,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = null
                        ) // swap icon later if you want
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Theme",
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    FilledTonalButton(
                        modifier = Modifier.weight(1f),
                        onClick = onPrivacy,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Filled.PrivacyTip, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Privacy",
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    QuickActionIcon(
                        icon = primaryIcon,
                        label = primaryLabel,
                        onClick = primaryClick
                    )

                    QuickActionIcon(
                        icon = Icons.Filled.MoreVert, // swap later
                        label = "Theme",
                        onClick = themeClick,
                        alpha = themeAlpha
                    )

                    QuickActionIcon(
                        icon = Icons.Filled.PrivacyTip,
                        label = "Privacy",
                        onClick = onPrivacy
                    )
                }
            }
        }
    }
}


@Composable
private fun QuickActionIcon(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    alpha: Float = 1f
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.alpha(alpha) // ✅ USE IT
    ) {
        FilledTonalIconButton(
            onClick = onClick,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null)
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}


@Composable
private fun InfoRow(
    iconRes: Int,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )

        Spacer(Modifier.width(14.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// BottomSheet opener (same behavior as your XML activity)
private fun showCreateProfileSheet(
    activity: AppCompatActivity,
    isEdit: Boolean,
    onProfileSaved: () -> Unit
) {
    CreateProfileBottomSheetFragment().apply {
        arguments = bundleOf("isEdit" to isEdit)
        onProfileSavedListener = object : CreateProfileBottomSheetFragment.OnProfileSavedListener {
            override fun onProfileSaved() = onProfileSaved()
        }
    }.show(activity.supportFragmentManager, "CreateProfileSheet")
}

