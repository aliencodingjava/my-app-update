@file:Suppress("DEPRECATION")

package com.flights.studio

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.window.OnBackInvokedDispatcher
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import dev.seyfarth.composeshimmer.shimmerEffect
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt



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

            // ✅ you MUST keep this
            val profileThemeModeState =
                rememberSaveable { mutableIntStateOf(userPrefs.profileThemeMode) }

            // ⚠️ keep as you had; just note this is not reactive (ok for your reinstall issue)
            val isLoggedIn = remember { userPrefs.isLoggedIn }

            val effectiveMode = if (!isLoggedIn) 6 else profileThemeModeState.intValue

            LaunchedEffect(isLoggedIn) {
                if (!isLoggedIn && userPrefs.profileThemeMode != 6) {
                    userPrefs.profileThemeMode = 6
                    profileThemeModeState.intValue = 6
                }
            }

            FlightsTheme(
                profileBackdropStyle = when (effectiveMode) {
                    0 -> ProfileBackdropStyle.Auto
                    1 -> ProfileBackdropStyle.Glass
                    2 -> ProfileBackdropStyle.ClearGlass
                    3 -> ProfileBackdropStyle.Blur
                    4 -> ProfileBackdropStyle.Frosted
                    5 -> ProfileBackdropStyle.VibrantGlass
                    6 -> ProfileBackdropStyle.Solid
                    else -> ProfileBackdropStyle.Amoled
                }
            ) {
                var showAuth by rememberSaveable { mutableStateOf(false) }

                val closeAuth = remember { { showAuth = false } }
                val openAuth = remember {
                    {
                        profileThemeModeState.intValue = 6
                        userPrefs.profileThemeMode = 6
                        showAuth = true
                    }
                }

                BackHandler(enabled = showAuth, onBack = closeAuth)

                if (showAuth) {
                    AuthScreen(
                        onLogin = { email, password ->
                            val e = email.trim()
                            if (e.isBlank() || password.isBlank()) {
                                return@AuthScreen Result.failure(
                                    IllegalArgumentException("Enter email and password")
                                )
                            }

                            val res = SupabaseAuthRepo.signIn(e, password)

                            if (res.isSuccess && SupabaseAuthRepo.hasSession()) {
                                userPrefs.isLoggedIn = true

                                // ✅ default theme after login = Blur
                                profileThemeModeState.intValue = 3
                                userPrefs.profileThemeMode = 3


                                // apply pending signup info if any
                                val pendingEmail = userPrefs.pendingEmail
                                val pendingName = userPrefs.pendingFullName
                                val pendingPhone = userPrefs.pendingPhone

                                if (!pendingEmail.isNullOrBlank()
                                    && pendingEmail.equals(e, ignoreCase = true)
                                    && !pendingName.isNullOrBlank()
                                    && !pendingPhone.isNullOrBlank()
                                ) {
                                    runCatching {
                                        SupabaseProfilesRepo.upsertMyProfile(
                                            fullName = pendingName,
                                            phone = pendingPhone,
                                            email = e,
                                            bio = null,
                                            birthday = null
                                        )

                                        userPrefs.userName = pendingName
                                        userPrefs.userPhone = pendingPhone
                                        userPrefs.userEmail = e
                                        userPrefs.pendingEmail = null
                                        userPrefs.pendingFullName = null
                                        userPrefs.pendingPhone = null
                                    }
                                }

                                closeAuth()
                                return@AuthScreen Result.success(Unit)
                            }

                            // ❌ LOGIN FAILED → normalize error
                            val raw = res.exceptionOrNull()?.message.orEmpty()
                            val msg = raw.lowercase()

                            val shortMsg = when {
                                // Supabase common invalid creds (covers wrong pass + non-existing email)
                                msg.contains("invalid login credentials") ||
                                        msg.contains("invalid credentials") ||
                                        (msg.contains("invalid") && (msg.contains("email") || msg.contains("password") || msg.contains("login"))) ||
                                        msg.contains("credentials") ->
                                    "Invalid email or password"

                                // Rate limit
                                msg.contains("rate") || msg.contains("too many") ->
                                    "Too many attempts. Try later."

                                // Email confirmation required (only if Supabase actually returns this)
                                msg.contains("email not confirmed") || msg.contains("confirm") ->
                                    "Please confirm your email"

                                else ->
                                    "Login failed"
                            }

                            Log.e("AUTH_LOGIN_FAIL", "raw=$raw", res.exceptionOrNull())

                            FancyPillToast.show(
                                activity = this@ProfileDetailsComposeActivity,
                                text = shortMsg,
                                durationMs = 2200L
                            )

                            Result.failure(IllegalStateException(shortMsg))

                        },

                        onSignUp = { fullName, phone, email, password, avatarUri ->
                            val e = email.trim()
                            val p = SupabaseProfilesRepo.normalizePhone(phone)
                            val n = fullName.trim()

                            if (n.isBlank() || p.isBlank() || e.isBlank() || password.isBlank()) {
                                return@AuthScreen Result.failure(IllegalArgumentException("Fill all fields"))
                            }
                            val available = runCatching { SupabaseProfilesRepo.isPhoneAvailable(p) }.getOrDefault(false)
                            if (!available) {
                                return@AuthScreen Result.failure(IllegalStateException("PHONE_TAKEN"))
                            }
                            if (avatarUri != null) {
                                userPrefs.setPhotoString(avatarUri.toString()) // content://...
                                userPrefs.userInitials = null
                            }

                            // 1) Attempt SIGN UP
                            val signUpRes = SupabaseAuthRepo.signUpWithProfileCheck(
                                email = e,
                                password = password,
                                phone = p
                            )

                            // If signup failed because already registered -> force Login mode
                            if (signUpRes.isFailure) {
                                val raw = signUpRes.exceptionOrNull()?.message.orEmpty()
                                val msg = raw.lowercase()
                                if (msg.contains("signup_conflict")) {
                                    return@AuthScreen Result.failure(IllegalStateException("SIGNUP_CONFLICT"))
                                }
                                if (msg.contains("weak_password")) {
                                    return@AuthScreen Result.failure(IllegalArgumentException("WEAK_PASSWORD"))
                                }
                                // 0) LOG ALWAYS so you can see what Supabase actually returned
                                Log.e("SIGNUP_FAIL", "raw=$raw", signUpRes.exceptionOrNull())

                                val isWeakPassword =
                                    msg.contains("weak_password") ||      // underscore variant
                                            msg.contains("weak password") ||      // space variant (your screenshot shows this)
                                            (msg.contains("password") && msg.contains("weak")) ||
                                            (msg.contains("password") && msg.contains("at least") && msg.contains("character")) ||
                                            (msg.contains("password") && msg.contains("minimum")) ||
                                            (msg.contains("password") && msg.contains("length"))
                                if (isWeakPassword) {
                                    return@AuthScreen Result.failure(IllegalArgumentException("WEAK_PASSWORD"))
                                }


                                // 2) ✅ CONFLICT (only strong signals)
                                val isConflict =
                                    msg.contains("23505") ||
                                            msg.contains("unique_violation") ||
                                            msg.contains("user already registered") ||
                                            msg.contains("already registered") ||
                                            msg.contains("email already") ||
                                            msg.contains("phone already") ||
                                            msg.contains("already exists") ||
                                            msg.contains("duplicate key")

                                if (isConflict) {
                                    return@AuthScreen Result.failure(IllegalStateException("SIGNUP_CONFLICT"))
                                }

                                // 3) ✅ otherwise generic
                                return@AuthScreen Result.failure(IllegalStateException("SIGNUP_FAILED"))
                            }



                            // 2) If confirmations are ON, Supabase often returns NO SESSION.
                            // Instead of acting like "signup worked again", we try SIGN IN immediately.
                            val sessionAfterSignUp = SupabaseManager.client.auth.currentSessionOrNull()
                            if (sessionAfterSignUp == null) {

                                // Save pending profile info; after login we will upsert profile (your login block already does this)
                                userPrefs.pendingEmail = e
                                userPrefs.pendingFullName = n
                                userPrefs.pendingPhone = p

                                // Try to sign in right now (if account already existed, this succeeds)
                                val signInRes = SupabaseAuthRepo.signIn(e, password)
                                if (signInRes.isSuccess && SupabaseAuthRepo.hasSession()) {
                                    userPrefs.isLoggedIn = true
                                    closeAuth()
                                    return@AuthScreen Result.success(Unit)
                                }

                                // New account but needs email confirmation
                                FancyPillToast.show(
                                    activity = this@ProfileDetailsComposeActivity,
                                    text = "Confirm your email, then log in.",
                                    durationMs = 2600L
                                )
                                return@AuthScreen Result.failure(IllegalStateException("SIGNUP_NO_SESSION"))
                            }

                            // 3) We DO have a session -> can create profile + upload avatar
                            val userId = sessionAfterSignUp.user?.id
                                ?: return@AuthScreen Result.failure(IllegalStateException("NO_USER_ID"))
                            val token = sessionAfterSignUp.accessToken

                            // Upload avatar if chosen (store PATH)
                            var photoPathToStore: String? = null
                            if (avatarUri != null) {
                                val path = SupabaseStorageUploader.uploadProfilePhotoAndReturnPath(
                                    context = this@ProfileDetailsComposeActivity,
                                    userId = userId,
                                    authToken = token,
                                    photoUri = avatarUri,
                                    bucket = "profile-photos"
                                )
                                if (!path.isNullOrBlank()) photoPathToStore = path
                            }

                            // Upsert profile (this should NOT crash once your DB rows are clean + unique constraint restored)
                            val profileRes = runCatching {
                                SupabaseProfilesRepo.upsertMyProfile(
                                    fullName = n,
                                    phone = p,
                                    email = e,
                                    bio = null,
                                    birthday = null
                                )

                            }

                            if (profileRes.isFailure) {
                                // If you still hit this, your table has conflicting rows (same email on another id)
                                FancyPillToast.show(
                                    activity = this@ProfileDetailsComposeActivity,
                                    text = "Could not save profile. Try again.",
                                    durationMs = 3000L
                                )
                                Log.e(
                                    "PROFILE_SAVE",
                                    "Profile save failed",
                                    profileRes.exceptionOrNull()
                                )
                                return@AuthScreen Result.failure(IllegalStateException("PROFILE_SAVE_FAILED"))
                            }

                            // Store photo path into DB
                            if (!photoPathToStore.isNullOrBlank()) {
                                runCatching {
                                    SupabaseStorageUploader.updateProfilePhotoUrl(
                                        userId = userId,
                                        authToken = token,
                                        photoPath = photoPathToStore
                                    )
                                }
                            }

                            // Local prefs + go back to profile
                            userPrefs.userName = n
                            userPrefs.userPhone = p
                            userPrefs.userEmail = e
                            userPrefs.isLoggedIn = true

                            if (!photoPathToStore.isNullOrBlank()) {
                                userPrefs.setPhotoString(photoPathToStore)
                                userPrefs.userInitials = null
                            }

                            // Clear pending (not needed anymore)
                            userPrefs.pendingEmail = null
                            userPrefs.pendingFullName = null
                            userPrefs.pendingPhone = null

                            closeAuth()
                            Log.d("AVATAR", "photoPathToStore=$photoPathToStore")
                            Log.d("AVATAR", "prefs.userPhotoUriString=${userPrefs.userPhotoUriString}")

                            Result.success(Unit)
                        }
,
                        onForgotPassword = { prefill ->
                            val e = prefill.trim()

                            if (e.isBlank()) {
                                FancyPillToast.show(
                                    this@ProfileDetailsComposeActivity,
                                    "Enter your email first",
                                    2200L
                                )
                                return@AuthScreen
                            }

                            lifecycleScope.launch {
                                val result = SupabaseAuthRepo.resetPassword(e)

                                val msg = when {
                                    result.isSuccess ->
                                        "If the email exists, you'll get a reset link."

                                    result.exceptionOrNull()?.message?.contains("rate", ignoreCase = true) == true ->
                                        "Please wait and try again."

                                    else ->
                                        "Could not send reset email."
                                }


                                FancyPillToast.show(
                                    this@ProfileDetailsComposeActivity,
                                    msg,
                                    2600L
                                )
                            }
                        }

                    )
                } else {
                    ProfileDetailsRoute(
                        userPrefs = userPrefs,
                        hostActivity = this@ProfileDetailsComposeActivity,
                        onNavigateBack = {
                            finish()
                            overridePendingTransition(0, R.anim.zoom_out)
                        },
                        themeMode = profileThemeModeState.intValue,
                        onThemeModeChange = { newMode ->
                            profileThemeModeState.intValue = newMode
                            userPrefs.profileThemeMode = newMode
                        },
                        onOpenLogin = openAuth // ✅ opens AuthScreen (starts on Login)
                    )
                }
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
    val photoRaw: String,
    val emailRaw: String,
    val bioRaw: String,
    val birthdayRaw: String
)
private sealed interface AvatarLoadState {
    data object Idle : AvatarLoadState
    data object Loading : AvatarLoadState
    data class Ready(val data: Any) : AvatarLoadState
    data object Failed : AvatarLoadState
}


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
        photoRaw = userPrefs.userPhotoUriString?.trim().orEmpty(),
        emailRaw = userPrefs.userEmail.orEmpty(),
        bioRaw = userPrefs.userBio.orEmpty(),
        birthdayRaw = userPrefs.userBirthday.orEmpty()
    )
}



@SuppressLint("ConfigurationScreenWidthHeight")
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ProfileDetailsRoute(
    userPrefs: UserPreferencesManager,
    hostActivity: AppCompatActivity,
    onNavigateBack: () -> Unit,
    themeMode: Int,
    onThemeModeChange: (Int) -> Unit,
    onOpenLogin: () -> Unit
) {
    val context = LocalContext.current
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    // trigger recomposition like loadAndDisplayProfile() + invalidateOptionsMenu()
    val scope = rememberCoroutineScope()
    val createProfileMsg = stringResource(R.string.prompt_create_profile)
    val comingSoonMsg = stringResource(R.string.coming_soon)
    var avatarVersion by remember { mutableIntStateOf(0) }

    var refreshTick by remember { mutableIntStateOf(0) }
    val editProfileLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                refreshTick += 1
                avatarVersion += 1   // 🔥 FORCE image reload
            }
        }


    val isLoggedIn = userPrefs.isLoggedIn
    val style = LocalProfileBackdropStyle.current
    LaunchedEffect(style) {
        Log.d("PROFILE_STYLE", "style = $style")
    }

    val cycleTheme: () -> Unit = {
        if (!isLoggedIn) {
            // Guests always go Solid
            onThemeModeChange(6) // Solid index in new order
            userPrefs.profileThemeMode = 6
            refreshTick += 1
            FancyPillToast.show(hostActivity, "Theme: Solid", 1600L)
        } else {

            // 8 modes: 0..7 in PRO order
            val next = (themeMode + 1) % 8
            onThemeModeChange(next)
            userPrefs.profileThemeMode = next
            refreshTick += 1

            val label = when (next) {
                0 -> "Auto"
                1 -> "Glass"
                2 -> "Clear glass"
                3 -> "Blur"
                4 -> "Frosted"
                5 -> "Vibrant"
                6 -> "Solid"
                else -> "Amoled"
            }

            FancyPillToast.show(
                hostActivity,
                "Theme: $label",
                1600L
            )
        }
    }





    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult

        // Keep local permission (ONLY for content://)
        if (uri.scheme == "content") {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // some providers don't allow persistable permission
            }
        }


        // ✅ Keep previous in case upload fails
        val previousPhoto = userPrefs.userPhotoUriString

// ✅ Optional preview (instant UX)
        userPrefs.setPhotoString(uri.toString())  // content://...
        userPrefs.userInitials = null
        refreshTick += 1


        // ✅ NOW upload to Supabase + update profile
        scope.launch {
            try {
                val session = SupabaseManager.client.auth.currentSessionOrNull() ?: return@launch
                val token = session.accessToken
                val userId = session.user?.id ?: return@launch

                // Upload to storage -> get public url
                val path = SupabaseStorageUploader.uploadProfilePhotoAndReturnPath(
                    context = context,
                    userId = userId,
                    authToken = token,
                    photoUri = uri,
                    bucket = "profile-photos"
                )

                if (path.isNullOrBlank()) {
                    withContext(Dispatchers.Main) {
                        userPrefs.setPhotoString(previousPhoto) // ✅ revert
                        FancyPillToast.show(hostActivity, "❌ Upload failed", 2500L)
                        refreshTick += 1
                    }
                    return@launch
                }


                // ✅ Update DB row (profiles.photo_uri)
                val ok = SupabaseStorageUploader.updateProfilePhotoUrl(
                    userId = userId,
                    authToken = token,
                    photoPath = path
                )


                withContext(Dispatchers.Main) {
                    if (ok) {
                        // Update stored path (same as you do)
                        userPrefs.setPhotoString(path)
                        userPrefs.userInitials = null

                        // ✅ IMPORTANT: bust caches because file content changed but path same
                        SignedUrlCache.invalidate(path)
                        AvatarDiskCache.delete(context, path)   // you need to add this function

                        refreshTick += 1
                        FancyPillToast.show(hostActivity, "✅ Photo updated", 2000L)
                    }
                    else {
                        FancyPillToast.show(hostActivity, "❌ Failed to update profile photo", 2500L)
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
    val scales = rememberUiScales()
    val bodyS = scales.body
    val labelS = scales.label



    val hasProfile = !userPrefs.userName.isNullOrBlank()


    val hasPhoto = ui.photoRaw.isNotBlank()

    val useGlassBackdrop = when (themeMode) {
        0 -> isLoggedIn && hasPhoto   // Auto
        1 -> true                     // Glass
        2 -> true                     // ClearGlass
        3 -> true                     // Blur
        4 -> true                     // Frosted
        5 -> true                     // VibrantGlass
        6 -> false                    // Solid
        else -> false                 // Amoled
    }



    LaunchedEffect(isLoggedIn, refreshTick) {
        if (!isLoggedIn) return@LaunchedEffect

        try {
            val session = SupabaseManager.client.auth.currentSessionOrNull()
                ?: return@LaunchedEffect

            val userId = session.user?.id ?: return@LaunchedEffect
            val token = session.accessToken

            val profile = SupabaseProfileDownloader.fetchProfile(
                userId = userId,
                authToken = token
            ) ?: return@LaunchedEffect

            var changed = false

            fun updateIfChanged(current: String?, incoming: String?): Boolean =
                current.orEmpty() != incoming.orEmpty()

            if (updateIfChanged(userPrefs.userName, profile.fullName)) {
                userPrefs.userName = profile.fullName.orEmpty()
                changed = true
            }
            if (updateIfChanged(userPrefs.userEmail, profile.email)) {
                userPrefs.userEmail = profile.email.orEmpty()
                changed = true
            }
            if (updateIfChanged(userPrefs.userPhone, profile.phone)) {
                userPrefs.userPhone = profile.phone.orEmpty()
                changed = true
            }
            if (updateIfChanged(userPrefs.userBio, profile.bio)) {
                userPrefs.userBio = profile.bio.orEmpty()
                changed = true
            }
            if (updateIfChanged(userPrefs.userBirthday, profile.birthday)) {
                userPrefs.userBirthday = profile.birthday.orEmpty()
                changed = true
            }

            val remoteRaw = profile.photoUri?.trim().orEmpty()
            if (remoteRaw.isNotBlank()) {
                val canonicalPath = when {
                    remoteRaw.startsWith("http", ignoreCase = true) ->
                        extractStoragePathIfSignedUrl(remoteRaw, bucket = "profile-photos")
                            ?: remoteRaw
                    else -> remoteRaw
                }

                if (canonicalPath != remoteRaw && !canonicalPath.startsWith("http", ignoreCase = true)) {
                    runCatching {
                        SupabaseStorageUploader.updateProfilePhotoUrl(
                            userId = userId,
                            authToken = token,
                            photoPath = canonicalPath
                        )
                    }
                }

                if (canonicalPath.isNotBlank() && userPrefs.userPhotoUriString != canonicalPath) {
                    userPrefs.setPhotoString(canonicalPath)
                    userPrefs.userInitials = null
                    changed = true
                }
            }

            // ✅ trigger recomposition only if something changed
            if (changed) refreshTick += 1

        } catch (_: Exception) {
        }
    }




    var showLogoutDialog by remember { mutableStateOf(false) }

    val openLogoutDialog = remember { { showLogoutDialog = true } }
    val dismissLogoutDialog = remember { { showLogoutDialog = false } }


    if (showLogoutDialog) {
        val scales = rememberUiScales()
        val bodyS = scales.body
        val labelS = scales.label

        val titleStyle = MaterialTheme.typography.titleMedium
        val bodyStyle = MaterialTheme.typography.bodyMedium
        val btnStyle = MaterialTheme.typography.labelLarge

        AlertDialog(
            onDismissRequest = dismissLogoutDialog,

            title = {
                Text(
                    text = stringResource(R.string.title_log_out),
                    style = titleStyle,
                    fontSize = titleStyle.fontSize.us(bodyS)
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.confirm_log_out),
                    style = bodyStyle,
                    fontSize = bodyStyle.fontSize.us(bodyS)
                )
            },

            confirmButton = {
                TextButton(
                    onClick = {
                        dismissLogoutDialog()

                        // ✅ logout
                        userPrefs.clear()

                        // ✅ force solid theme after logout
                        onThemeModeChange(6)
                        userPrefs.profileThemeMode = 6

                        // ✅ rebuild UI
                        refreshTick += 1
                    }
                ) {
                    Text(
                        text = stringResource(R.string.yes),
                        style = btnStyle,
                        fontSize = btnStyle.fontSize.us(labelS)
                    )
                }
            },

            dismissButton = {
                TextButton(onClick = dismissLogoutDialog) {
                    Text(
                        text = stringResource(R.string.cancel),
                        style = btnStyle,
                        fontSize = btnStyle.fontSize.us(labelS)
                    )
                }
            }
        )
    }


    val pageBg = LocalAppPageBg.current

    val scrollState = rememberScrollState()
    val pageBackdrop = rememberLayerBackdrop {
        drawRect(pageBg)
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
                shadowElevation = 1.dp
            ) {
                CenterAlignedTopAppBar(
                    title = {
                        val topTitle = MaterialTheme.typography.titleLarge
                        Text(
                            text = stringResource(R.string.title_my_profile),
                            style = topTitle.copy(fontWeight = FontWeight.SemiBold),
                            fontSize = topTitle.fontSize.us(bodyS)
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
                        Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {

                            // ✅ IMPORTANT: capture anchor bounds here (window coords)
                            var anchorBounds by remember { mutableStateOf<IntRect?>(null) }

                            val openBg = MenuDefaults.groupVibrantContainerColor
                            val openFg = MaterialTheme.colorScheme.onSurface
                            val closedFg = MaterialTheme.colorScheme.onSurface

                            val shape = RoundedCornerShape(14.dp)
                            val interaction = remember { MutableInteractionSource() }

                            val scale by animateFloatAsState(
                                targetValue = if (menuOpen) 1.05f else 1f,
                                animationSpec = spring(dampingRatio = 0.75f, stiffness = 380f),
                                label = "menuScale"
                            )

                            val rotation by animateFloatAsState(
                                targetValue = if (menuOpen) 90f else 0f,
                                animationSpec = spring(dampingRatio = 0.85f, stiffness = 320f),
                                label = "menuRotate"
                            )

                            // ✅ This box IS the anchor. We measure its bounds in window.
                            Box(
                                modifier = Modifier
                                    .width(36.dp)   // 👈 slimmer
                                    .height(44.dp)  // 👈 same height
                                    .onGloballyPositioned { coords ->
                                        val r = coords.boundsInWindow()
                                        anchorBounds = IntRect(
                                            left = r.left.roundToInt(),
                                            top = r.top.roundToInt(),
                                            right = r.right.roundToInt(),
                                            bottom = r.bottom.roundToInt()
                                        )
                                    }
                                    .scale(scale)
                                    .clip(shape)
                                    .background(
                                        color = if (menuOpen) openBg else Color.Transparent,
                                        shape = shape
                                    )
                                    .clickable(
                                        interactionSource = interaction,
                                        indication = null
                                    ) { menuOpen = !menuOpen },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = "Menu",
                                    tint = if (menuOpen) openFg else closedFg,
                                    modifier = Modifier.graphicsLayer { rotationZ = rotation }
                                )
                            }

                            // ✅ Anchored menu (works in portrait + landscape)
                            ProfileExpressiveMenuPopupAnchored(
                                expanded = menuOpen,
                                onDismiss = { menuOpen = false },
                                anchorBounds = anchorBounds,
                                hasProfile = hasProfile,
                                isLoggedIn = isLoggedIn,

                                onEdit = {
                                    menuOpen = false
                                    editProfileLauncher.launch(Intent(hostActivity, EditProfileComposeActivity::class.java))
                                },
                                onChangePhoto = {
                                    menuOpen = false
                                    if (isLoggedIn) photoPickerLauncher.launch(arrayOf("image/*"))
                                    else FancyPillToast.show(
                                        hostActivity,
                                        hostActivity.getString(R.string.prompt_create_profile),
                                        3000L
                                    )
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
                                    if (isLoggedIn) openLogoutDialog() else onOpenLogin()
                                },

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

//            val cfg = LocalConfiguration.current
//            val gradientHeight = remember(cfg.screenHeightDp) { cfg.screenHeightDp.dp * 0.52f }
//
//            BackdropGradientLayer(
//                backdrop = pageBackdrop,
//                modifier = Modifier
//                    .align(Alignment.TopStart)
//                    .zIndex(0.1f),
//                height = gradientHeight,      // your half screen (OK)
//                blurDp = 18.dp,
//                tintIntensity = if (isSystemInDarkTheme()) 0.30f else 0.18f
//            )



            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f)
                    .padding(padding)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp, bottom = 24.dp)
            ) {
// ----------------------------
// Header card (FULL)
// ----------------------------
                val style = LocalProfileBackdropStyle.current
                val elevated = style != ProfileBackdropStyle.Solid


                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant, // ✅ SAME AS QUICK ACTIONS
                    shadowElevation = if (elevated) 1.dp else 0.dp    // ✅ SAME AS QUICK ACTIONS
                ) {

                val headerShape = RoundedCornerShape(28.dp)
                    val headerBg = MaterialTheme.colorScheme.surfaceVariant

                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(headerShape)
                            .background(headerBg) // ✅ background on clipped container
                    ) {


                    // ✅ 2) GLASS OVERLAY (reads PAGE backdrop behind all cards)
                        Box(
                            Modifier
                                .matchParentSize()
                                .clip(headerShape)
                                .profileGlassBackdrop(
                                    backdrop = pageBackdrop,   // ✅ THIS is the key
                                    shape = headerShape,
                                    enabled = useGlassBackdrop
                                )
                        )

                        // ✅ 3) CONTENT ON TOP (sharp)
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 0.dp)
                        ) {
                            val profileLabel = MaterialTheme.typography.labelLarge
                            Text(
                                text = "Profile",
                                style = profileLabel,
                                fontSize = profileLabel.fontSize.us(labelS),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 4.dp)
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
                                    val rawPhoto = ui.photoRaw.trim()

                                    var avatarFailed by remember(rawPhoto) { mutableStateOf(false) }

                                    val avatarState by produceState(
                                        initialValue = if (rawPhoto.isBlank()) AvatarLoadState.Idle else AvatarLoadState.Loading,
                                        key1 = rawPhoto,
                                        key2 = refreshTick
                                    ) {
                                        if (rawPhoto.isBlank()) {
                                            value = AvatarLoadState.Idle
                                            return@produceState
                                        }

                                        value = AvatarLoadState.Loading

                                        // already usable by Coil
                                        if (
                                            rawPhoto.startsWith("http", true) ||
                                            rawPhoto.startsWith("content", true) ||
                                            rawPhoto.startsWith("file", true)
                                        ) {
                                            value = AvatarLoadState.Ready(rawPhoto)
                                            return@produceState
                                        }

                                        // 1) try local disk cache FIRST
                                        val local = AvatarDiskCache.localFile(context, rawPhoto)
                                        if (local.exists() && local.length() > 0L) {
                                            value = AvatarLoadState.Ready(local)
                                            return@produceState
                                        }

                                        // 2) try signed-url cache
                                        SignedUrlCache.getValid(rawPhoto)?.let {
                                            value = AvatarLoadState.Ready(it)
                                            return@produceState
                                        }

                                        // 3) sign fresh
                                        val session = SupabaseManager.client.auth.currentSessionOrNull()
                                        if (session != null) {
                                            val fresh = SupabaseStorageUploader.createSignedUrl(
                                                objectPath = rawPhoto,
                                                authToken = session.accessToken,
                                                bucket = "profile-photos"
                                            )
                                            if (!fresh.isNullOrBlank()) {
                                                SignedUrlCache.put(rawPhoto, fresh, 60 * 60)
                                                AvatarDiskCache.cacheFromSignedUrl(context, rawPhoto, fresh)
                                                value = AvatarLoadState.Ready(fresh)
                                                return@produceState
                                            }
                                        }

                                        value = AvatarLoadState.Failed
                                    }

                                    // reset error flag whenever our resolved data changes
                                    LaunchedEffect(avatarState) { avatarFailed = false }

                                    when (val st = avatarState) {
                                        is AvatarLoadState.Ready -> {
                                            if (!avatarFailed) {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(context)
                                                        .data(st.data)
                                                        .setParameter("v", avatarVersion)
                                                        .crossfade(true)
                                                        .build(),
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize(),
                                                    onError = {
                                                        avatarFailed = true
                                                        if (rawPhoto.isNotBlank()) {
                                                            SignedUrlCache.invalidate(rawPhoto)
                                                        }
                                                    }
                                                )
                                            } else {
                                                // logo fallback if coil failed
                                                Box(
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Image(
                                                        painter = painterResource(R.drawable.jh_airport_logo_dark),
                                                        contentDescription = null,
                                                        modifier = Modifier.fillMaxSize(0.62f),
                                                        contentScale = ContentScale.Fit,
                                                        colorFilter = ColorFilter.tint(
                                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.90f)
                                                        )
                                                    )
                                                }
                                            }
                                        }

                                        AvatarLoadState.Loading -> {
                                            // ✅ shimmer placeholder while we wait
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .shimmerEffect(
                                                        visible = true,
                                                        shape = avatarShape
                                                    )
                                            )
                                        }

                                        AvatarLoadState.Idle,
                                        AvatarLoadState.Failed -> {
                                            // logo fallback (no photo or couldn't sign)
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Image(
                                                    painter = painterResource(R.drawable.jh_airport_logo_dark),
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize(0.62f),
                                                    contentScale = ContentScale.Fit,
                                                    colorFilter = ColorFilter.tint(
                                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.90f)
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }

                                    Spacer(Modifier.width(14.dp))

                                Column(Modifier.weight(1f)) {
                                    val nameStyle = MaterialTheme.typography.titleLarge
                                    Text(
                                        text = ui.displayName.ifEmpty { stringResource(R.string.default_user_name) },
                                        style = nameStyle.copy(fontWeight = FontWeight.Bold),
                                        fontSize = nameStyle.fontSize.us(bodyS),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(6.dp))

                                    val secondaryStyle = MaterialTheme.typography.bodyMedium
                                    Text(
                                        text = ui.secondaryTextRaw.ifEmpty { stringResource(R.string.unknown_contact) },
                                        style = secondaryStyle,
                                        fontSize = secondaryStyle.fontSize.us(bodyS),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(6.dp))

                                    val statusStyle = MaterialTheme.typography.labelMedium
                                    Text(
                                        text = if (isLoggedIn) "🟢 Signed in" else "⚪ Guest mode",
                                        style = statusStyle,
                                        fontSize = statusStyle.fontSize.us(labelS),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                }
                            }
                        }
                    }
                }


                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shadowElevation = if (elevated) 1.dp else 0.dp   // 🔥 SAME AS QUICK ACTIONS
                ) {

                val headerShape = RoundedCornerShape(28.dp)
                    val headerBg = MaterialTheme.colorScheme.surfaceVariant

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
                                    shape = headerShape,
                                    enabled = useGlassBackdrop
                                )
                        )

                        // ✅ 3) CONTENT ON TOP (sharp)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        ) {

                            val infoTitle = MaterialTheme.typography.labelLarge
                            Text(
                                text = "Information",
                                style = infoTitle,
                                fontSize = infoTitle.fontSize.us(labelS),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 20.dp, bottom = 6.dp)
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
                }

                QuickActionsCard(
                    isLoggedIn = isLoggedIn,
                    onEdit = {
                        editProfileLauncher.launch(
                            Intent(hostActivity, EditProfileComposeActivity::class.java)
                        )
                        hostActivity.overridePendingTransition(R.anim.zoom_in, 0)
                    },

                    onLogin = {
                        onOpenLogin()
                    },

//                    onLogin = {
//                        showCreateProfileSheet(
//                            activity = hostActivity,
//                            isEdit = false,
//                            onProfileSaved = { refreshTick += 1 }
//                        )
//                    },
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
    val scales = rememberUiScales()
    val labelS = scales.label
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp,
        shadowElevation = if (elevated) 1.dp else 1.dp // ✅ same as top bar
    ) {
        Column(Modifier.padding(14.dp)) {
            val qaTitle = MaterialTheme.typography.labelLarge
            Text(
                text = "Quick actions",
                style = qaTitle,
                fontSize = qaTitle.fontSize.us(labelS),
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
                val btnText = MaterialTheme.typography.labelLarge
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onSizeChanged { actionsWidthPx = it.width },
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        modifier = Modifier.weight(1f),
                        onClick = primaryClick,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                    ) {
                        Icon(primaryIcon, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        val btnText = MaterialTheme.typography.labelLarge
                        Text(
                            primaryLabel,
                            style = btnText,
                            fontSize = btnText.fontSize.us(labelS),
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
                            Icons.Filled.Palette,
                            contentDescription = "Theme"
                        ) // swap icon later if you want
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Theme",
                            style = btnText,
                            fontSize = btnText.fontSize.us(labelS),
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
                            style = btnText,
                            fontSize = btnText.fontSize.us(labelS),
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
                        icon = Icons.Filled.Palette,
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
    val scales = rememberUiScales()
    val labelS = scales.label
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

        val qaLabel = MaterialTheme.typography.labelMedium
        Text(
            text = label,
            style = qaLabel,
            fontSize = qaLabel.fontSize.us(labelS),
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
    val scales = rememberUiScales()
    val bodyS = scales.body
    val labelS = scales.label
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
            val infoLabel = MaterialTheme.typography.labelMedium
            Text(
                text = label,
                style = infoLabel.copy(fontWeight = FontWeight.Bold),
                fontSize = infoLabel.fontSize.us(labelS),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(3.dp))
            val infoValue = MaterialTheme.typography.bodyLarge
            Text(
                text = value,
                style = infoValue,
                fontSize = infoValue.fontSize.us(bodyS),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// BottomSheet opener (same behavior as your XML activity)
//private fun showCreateProfileSheet(
//    activity: AppCompatActivity,
//    onProfileSaved: () -> Unit
//) {
//    CreateProfileBottomSheetFragment().apply {
//        arguments = bundleOf("isEdit" to true)
//        onProfileSavedListener = object :
//            CreateProfileBottomSheetFragment.OnProfileSavedListener {
//            override fun onProfileSaved() = onProfileSaved()
//        }
//    }.show(activity.supportFragmentManager, "CreateProfileSheet")
//}

