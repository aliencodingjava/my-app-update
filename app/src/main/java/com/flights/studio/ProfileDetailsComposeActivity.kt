@file:Suppress("DEPRECATION")

package com.flights.studio

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.window.OnBackInvokedDispatcher
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay

class ProfileDetailsComposeActivity : AppCompatActivity() {

    private lateinit var userPrefs: UserPreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userPrefs = UserPreferencesManager(this)

        // ✅ Predictive Back Support for Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
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
                    // Light theme -> dark icons
                    // Dark theme  -> light icons
                    isAppearanceLightStatusBars = !isDark
                    isAppearanceLightNavigationBars = !isDark
                }
            }

            FlightsTheme {
                ProfileDetailsRoute(
                    userPrefs = userPrefs,
                    hostActivity = this@ProfileDetailsComposeActivity,
                    onNavigateBack = {
                        finish()
                        overridePendingTransition(0, R.anim.zoom_out)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileDetailsRoute(
    userPrefs: UserPreferencesManager,
    hostActivity: AppCompatActivity,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var menuOpen by rememberSaveable { mutableStateOf(false) }

    // trigger recomposition like loadAndDisplayProfile() + invalidateOptionsMenu()
    var refreshTick by remember { mutableIntStateOf(0) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
            }
            userPrefs.userPhotoUriString = it.toString()
            userPrefs.userInitials = null
            refreshTick += 1
        }
    }

    // onResume refresh (same as XML activity)
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
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

    var pillMessageResId by remember { mutableIntStateOf(R.string.coming_soon) }
    var pillToken by remember { mutableIntStateOf(0) } // trigger
    var pillVisible by remember { mutableStateOf(false) }

    fun showPill(@androidx.annotation.StringRes resId: Int) {
        pillMessageResId = resId
        pillToken += 1
    }

    LaunchedEffect(pillToken) {
        if (pillToken == 0) return@LaunchedEffect
        pillVisible = true
        delay(1300)
        pillVisible = false
    }

    val scrollState = rememberScrollState()


    Scaffold(

        topBar = {
            Surface(
                shape = RoundedCornerShape(
                    bottomStart = 24.dp,
                    bottomEnd = 24.dp
                ),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 0.dp,
                shadowElevation = 5.dp
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
                                    photoPickerLauncher.launch(arrayOf("image/*"))
                                },
                                onPrivacy = {
                                    menuOpen = false
                                    showPill(R.string.coming_soon)
                                },
                                onLoginLogout = {
                                    menuOpen = false
                                    if (isLoggedIn) openLogoutDialog()
                                    else {
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
                    }
,
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        }

    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 24.dp)
        ) {
            // Header card
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (isLoggedIn)
                        Color.Transparent        // gradient will be visible
                    else
                        MaterialTheme.colorScheme.surfaceVariant),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
            ) {
                Box {
                    if (isLoggedIn) {
                        Box(
                            Modifier
                                .matchParentSize()
                                .staticProfileBackdrop()
                        )

                    }

                    Column {

                    // ✅ Section label (light, professional)
                    Text(
                        text = "Profile",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(start = 20.dp, top = 14.dp, bottom = 4.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val alpha = if (isLoggedIn) 1f else 0.4f
                        val avatarShape = RoundedCornerShape(46.dp)

                        Surface(
                            modifier = Modifier
                                .size(92.dp)
                                .alpha(alpha)
                                .clip(avatarShape)
                                .clickable {
                                    if (isLoggedIn) photoPickerLauncher.launch(arrayOf("image/*"))
                                    else showPill(R.string.prompt_create_profile)
                                },
                            tonalElevation = 2.dp,
                            color = MaterialTheme.colorScheme.surface,
                            shape = avatarShape
                        ) {
                            if (ui.photoUri != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(ui.photoUri)
                                        .placeholder(R.drawable.placeholder_background)
                                        .error(R.drawable.placeholder_background)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize() // ✅ no CircleShape clip
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
                                text = ui.displayName.ifEmpty {
                                    stringResource(R.string.default_user_name)
                                },
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(Modifier.height(6.dp))

                            Text(
                                text = ui.secondaryTextRaw.ifEmpty {
                                    stringResource(R.string.unknown_contact)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
//                                modifier = Modifier.padding(start = 21.dp)
                            )

                            // ✅ Status line (tiny but powerful)
                            Spacer(Modifier.height(6.dp))

                            Text(
                                text = if (isLoggedIn) "🟢 Signed in" else "⚪ Guest mode",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
//                                modifier = Modifier.padding(start = 21.dp)
                            )
                        }
                    }
                }

                }
            }

            // Info card
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
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
                onChangePhoto = {
                    photoPickerLauncher.launch(arrayOf("image/*"))
                },
                onRequireProfile = {
                    showPill(R.string.prompt_create_profile)
                },
                onPrivacy = {
                    showPill(R.string.coming_soon)
                }
            )


            Spacer(Modifier.height(24.dp))
        }

            // ===== Modern in-app pill overlay =====
            ComingSoonPill(
                visible = pillVisible,
                text = stringResource(pillMessageResId),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 28.dp)
            )

        }
    }


}


@Composable
private fun ComingSoonPill(
    visible: Boolean,
    text: String,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier, // ✅ THIS IS THE FIX
        enter = fadeIn(animationSpec = tween(180)) +
                slideInVertically(animationSpec = tween(220)) { fullHeight -> fullHeight / 2 },
        exit = fadeOut(animationSpec = tween(180)) +
                slideOutVertically(animationSpec = tween(200)) { fullHeight -> fullHeight / 2 }
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f),
            tonalElevation = 0.dp,
            shadowElevation = 5.dp
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 50.dp, vertical = 12.dp)
            )
        }
    }
}


@Composable
fun QuickActionsCard(
    isLoggedIn: Boolean,
    onEdit: () -> Unit,
    onLogin: () -> Unit,
    onChangePhoto: () -> Unit,
    onRequireProfile: () -> Unit,
    onPrivacy: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
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

            val photoClick = if (isLoggedIn) onChangePhoto else onRequireProfile
            val photoAlpha = if (isLoggedIn) 1f else 0.6f

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
                        Text(primaryLabel, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                    }

                    FilledTonalButton(
                        modifier = Modifier
                            .weight(1f)
                            .alpha(photoAlpha),
                        onClick = photoClick,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Photo", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                    }

                    FilledTonalButton(
                        modifier = Modifier.weight(1f),
                        onClick = onPrivacy,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Filled.PrivacyTip, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Privacy", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
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
                        icon = Icons.Filled.PhotoCamera,
                        label = "Photo",
                        onClick = photoClick,
                        alpha = photoAlpha
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
            painter = androidx.compose.ui.res.painterResource(iconRes),
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

