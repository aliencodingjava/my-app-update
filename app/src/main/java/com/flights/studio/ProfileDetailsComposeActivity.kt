@file:Suppress("DEPRECATION")

package com.flights.studio

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
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
import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.draw.shadow
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
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
                overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation)

            }
        }
        val openAuthDirectly = intent.getBooleanExtra("open_auth", false)

        val startMode = when (intent.getStringExtra("auth_mode")) {
            "signup" -> AuthMode.SignUp
            else -> AuthMode.Login
        }

        setContent {
            val profileThemeModeState = rememberSaveable { mutableIntStateOf(userPrefs.profileThemeMode) }
            var isLoggedIn by rememberSaveable { mutableStateOf(userPrefs.isLoggedIn) }

            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val obs = LifecycleEventObserver { _, e ->
                    if (e == Lifecycle.Event.ON_RESUME) {
                        val v = userPrefs.isLoggedIn
                        if (v != isLoggedIn) isLoggedIn = v
                    }
                }
                lifecycleOwner.lifecycle.addObserver(obs)
                onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
            }

            val effectiveMode = if (!isLoggedIn) 6 else profileThemeModeState.intValue

            LaunchedEffect(isLoggedIn) {
                if (!isLoggedIn && userPrefs.profileThemeMode != 6) {
                    userPrefs.profileThemeMode = 6
                    profileThemeModeState.intValue = 6
                }
            }

            val view = LocalView.current
            val isDark = when (effectiveMode) {
                7 -> true
                else -> isSystemInDarkTheme()
            }

            SideEffect {
                val window = (view.context as Activity).window
                val controller = WindowCompat.getInsetsController(window, view)

                controller.isAppearanceLightStatusBars = !isDark
                controller.isAppearanceLightNavigationBars = !isDark
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
                var showAuth by rememberSaveable { mutableStateOf(openAuthDirectly) }

                val closeAuth = remember { { showAuth = false } }
                val openAuth = remember {
                    {
                        profileThemeModeState.intValue = 6
                        userPrefs.profileThemeMode = 6
                        showAuth = true
                    }
                }
                var pendingMfaEmail by rememberSaveable { mutableStateOf<String?>(null) }

                suspend fun completeLogin(email: String) {
                    userPrefs.isLoggedIn = true
                    isLoggedIn = true
                    getSharedPreferences("notes_meta", MODE_PRIVATE)
                        .edit()
                        .remove("last_sync_at")
                        .apply()

                    profileThemeModeState.intValue = 3
                    userPrefs.profileThemeMode = 3

                    val pendingEmail = userPrefs.pendingEmail
                    val pendingName = userPrefs.pendingFullName
                    val pendingPhone = userPrefs.pendingPhone

                    if (!pendingEmail.isNullOrBlank()
                        && pendingEmail.equals(email, ignoreCase = true)
                        && !pendingName.isNullOrBlank()
                        && !pendingPhone.isNullOrBlank()
                    ) {
                        runCatching {
                            SupabaseProfilesRepo.upsertMyProfile(
                                fullName = pendingName,
                                phone = pendingPhone,
                                email = email,
                                bio = null,
                                birthday = null
                            )

                            userPrefs.userName = pendingName
                            userPrefs.userPhone = pendingPhone
                            userPrefs.userEmail = email
                            userPrefs.pendingEmail = null
                            userPrefs.pendingFullName = null
                            userPrefs.pendingPhone = null
                        }
                    }

                    pendingMfaEmail = null
                    closeAuth()
                }

                BackHandler(enabled = showAuth, onBack = closeAuth)

                if (showAuth) {
                    AuthScreen(
                        startMode = startMode,
                        onLogin = { email, password ->
                            val e = email.trim()
                            if (e.isBlank() || password.isBlank()) {
                                return@AuthScreen Result.failure(
                                    IllegalArgumentException("Enter email and password")
                                )
                            }

                            val res = SupabaseAuthRepo.signIn(e, password)

                            if (res.isSuccess && SupabaseAuthRepo.hasSession()) {
                                if (SupabaseAuthRepo.needsMfaChallenge()) {
                                    pendingMfaEmail = e
                                    return@AuthScreen Result.failure(IllegalStateException("MFA_REQUIRED"))
                                }

                                completeLogin(e)
                                return@AuthScreen Result.success(Unit)
                            }

                            val raw = res.exceptionOrNull()?.message.orEmpty()
                            val msg = raw.lowercase()

                            val shortMsg = when {
                                msg.contains("invalid login credentials") ||
                                        msg.contains("invalid credentials") ||
                                        (msg.contains("invalid") && (msg.contains("email") || msg.contains("password") || msg.contains("login"))) ||
                                        msg.contains("credentials") ->
                                    "Invalid email or password"

                                msg.contains("rate") || msg.contains("too many") ->
                                    "Too many attempts. Try later."

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
                        onVerifyMfa = { code ->
                            val res = SupabaseAuthRepo.verifyMfaCode(code)
                            if (res.isSuccess) {
                                completeLogin(pendingMfaEmail.orEmpty())
                                Result.success(Unit)
                            } else {
                                val raw = res.exceptionOrNull()?.message.orEmpty()
                                Log.e("AUTH_MFA_FAIL", "raw=$raw", res.exceptionOrNull())
                                Result.failure(
                                    IllegalStateException(
                                        if (raw.contains("MFA_NO_FACTOR")) "MFA_NO_FACTOR" else "MFA_INVALID"
                                    )
                                )
                            }
                        },
                        onCancelMfa = {
                            pendingMfaEmail = null
                            SupabaseAuthRepo.signOutLocal()
                        },
                        onSignUp = { fullName, phone, email, password, avatarUri ->
                            val e = email.trim()
                            val p = SupabaseProfilesRepo.normalizePhone(phone)
                            val n = fullName.trim()

                            if (n.isBlank() || p.isBlank() || e.isBlank() || password.isBlank()) {
                                return@AuthScreen Result.failure(IllegalArgumentException("Fill all fields"))
                            }
                            val available =
                                runCatching { SupabaseProfilesRepo.isPhoneAvailable(p) }.getOrDefault(false)
                            if (!available) {
                                return@AuthScreen Result.failure(IllegalStateException("PHONE_TAKEN"))
                            }
                            if (avatarUri != null) {
                                userPrefs.setPhotoString(avatarUri.toString())
                                userPrefs.userInitials = null
                            }

                            val signUpRes = SupabaseAuthRepo.signUpWithProfileCheck(
                                email = e,
                                password = password,
                                phone = p
                            )

                            if (signUpRes.isFailure) {
                                val raw = signUpRes.exceptionOrNull()?.message.orEmpty()
                                val msg = raw.lowercase()
                                if (msg.contains("signup_conflict")) {
                                    return@AuthScreen Result.failure(IllegalStateException("SIGNUP_CONFLICT"))
                                }
                                if (msg.contains("weak_password")) {
                                    return@AuthScreen Result.failure(IllegalArgumentException("WEAK_PASSWORD"))
                                }

                                Log.e("SIGNUP_FAIL", "raw=$raw", signUpRes.exceptionOrNull())

                                val isWeakPassword =
                                    msg.contains("weak_password") ||
                                            msg.contains("weak password") ||
                                            (msg.contains("password") && msg.contains("weak")) ||
                                            (msg.contains("password") && msg.contains("at least") && msg.contains("character")) ||
                                            (msg.contains("password") && msg.contains("minimum")) ||
                                            (msg.contains("password") && msg.contains("length"))
                                if (isWeakPassword) {
                                    return@AuthScreen Result.failure(IllegalArgumentException("WEAK_PASSWORD"))
                                }

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

                                return@AuthScreen Result.failure(IllegalStateException("SIGNUP_FAILED"))
                            }

                            val sessionAfterSignUp = SupabaseManager.client.auth.currentSessionOrNull()
                            if (sessionAfterSignUp == null) {
                                userPrefs.pendingEmail = e
                                userPrefs.pendingFullName = n
                                userPrefs.pendingPhone = p

                                val signInRes = SupabaseAuthRepo.signIn(e, password)
                                if (signInRes.isSuccess && SupabaseAuthRepo.hasSession()) {
                                    userPrefs.isLoggedIn = true
                                    closeAuth()
                                    return@AuthScreen Result.success(Unit)
                                }

                                FancyPillToast.show(
                                    activity = this@ProfileDetailsComposeActivity,
                                    text = "Confirm your email, then log in.",
                                    durationMs = 2600L
                                )
                                return@AuthScreen Result.failure(IllegalStateException("SIGNUP_NO_SESSION"))
                            }

                            val userId = sessionAfterSignUp.user?.id
                                ?: return@AuthScreen Result.failure(IllegalStateException("NO_USER_ID"))
                            val token = sessionAfterSignUp.accessToken

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
                                FancyPillToast.show(
                                    activity = this@ProfileDetailsComposeActivity,
                                    text = "Could not save profile. Try again.",
                                    durationMs = 3000L
                                )
                                Log.e("PROFILE_SAVE", "Profile save failed", profileRes.exceptionOrNull())
                                return@AuthScreen Result.failure(IllegalStateException("PROFILE_SAVE_FAILED"))
                            }

                            if (!photoPathToStore.isNullOrBlank()) {
                                runCatching {
                                    SupabaseStorageUploader.updateProfilePhotoUrl(
                                        userId = userId,
                                        authToken = token,
                                        photoPath = photoPathToStore
                                    )
                                }
                            }

                            userPrefs.userName = n
                            userPrefs.userPhone = p
                            userPrefs.userEmail = e
                            userPrefs.isLoggedIn = true

                            if (!photoPathToStore.isNullOrBlank()) {
                                userPrefs.setPhotoString(photoPathToStore)
                                userPrefs.userInitials = null
                            }

                            userPrefs.pendingEmail = null
                            userPrefs.pendingFullName = null
                            userPrefs.pendingPhone = null

                            closeAuth()
                            Log.d("AVATAR", "photoPathToStore=$photoPathToStore")
                            Log.d("AVATAR", "prefs.userPhotoUriString=${userPrefs.userPhotoUriString}")

                            Result.success(Unit)
                        },
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
                            overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation)
                        },
                        themeMode = profileThemeModeState.intValue,
                        onThemeModeChange = { newMode ->
                            profileThemeModeState.intValue = newMode
                            userPrefs.profileThemeMode = newMode
                        },
                        onOpenLogin = openAuth,
                        isLoggedIn = isLoggedIn,
                        setLoggedIn = { v -> isLoggedIn = v }
                    )
                }
            }
        }

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

@Composable
private fun ProfileAvatarImage(
    data: Any,
    avatarVersion: Int,
    shape: RoundedCornerShape,
    onError: () -> Unit
) {
    val context = LocalContext.current
    val request = remember(context, data, avatarVersion) {
        ImageRequest.Builder(context)
            .data(data)
            .setParameter("v", avatarVersion)
            .crossfade(false)
            .build()
    }
    val painter = rememberAsyncImagePainter(model = request)
    val painterState = painter.state

    LaunchedEffect(painterState) {
        if (painterState is AsyncImagePainter.State.Error) onError()
    }

    if (painterState is AsyncImagePainter.State.Success) {
        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    } else {
        ProfileAvatarSkeleton(shape = shape)
    }
}

@Composable
private fun ProfileAvatarSkeleton(shape: RoundedCornerShape) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .shimmerEffect(
                visible = true,
                shape = shape
            )
    )
}

@Composable
private fun ProfileAvatarFallback() {
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
    onOpenLogin: () -> Unit,
    isLoggedIn: Boolean,
    setLoggedIn: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    // trigger recomposition like loadAndDisplayProfile() + invalidateOptionsMenu()
    val scope = rememberCoroutineScope()
    val createProfileMsg = stringResource(R.string.prompt_create_profile)
//    val comingSoonMsg = stringResource(R.string.coming_soon)
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
                    } else {
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
    val showMfaDialog = rememberSaveable { mutableStateOf(false) }

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

                if (canonicalPath != remoteRaw && !canonicalPath.startsWith(
                        "http",
                        ignoreCase = true
                    )
                ) {
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
                        scope.launch { SupabaseAuthRepo.signOutLocal() }
                        hostActivity.getSharedPreferences("notes_meta", android.content.Context.MODE_PRIVATE)
                            .edit()
                            .remove("last_sync_at")
                            .apply()
                        userPrefs.clear()
                        setLoggedIn(false)
                        onThemeModeChange(6)
                        userPrefs.profileThemeMode = 6
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
    val profileContentBackdrop = rememberLayerBackdrop()
    val elevated = style != ProfileBackdropStyle.Solid
    val dialogSubTextColor = MaterialTheme.colorScheme.onSurfaceVariant



    Box(Modifier.fillMaxSize()) {

        Box(
            Modifier
                .fillMaxSize()
                .layerBackdrop(profileContentBackdrop)
        ) {
            Scaffold(
        containerColor = Color.Transparent,

        topBar = {
            val topBarShape = RoundedCornerShape(
                bottomStart = 24.dp,
                bottomEnd = 24.dp
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = topBarShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = if (elevated) 1.dp else 0.dp
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(topBarShape)
                ) {
                    Box(
                        Modifier
                            .matchParentSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )

                    Box(
                        Modifier
                            .matchParentSize()
                            .clip(topBarShape)
                            .profileGlassBackdrop(
                                backdrop = pageBackdrop,
                                shape = topBarShape,
                                enabled = useGlassBackdrop
                            )
                    )

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
                                        editProfileLauncher.launch(
                                            Intent(
                                                hostActivity,
                                                EditProfileComposeActivity::class.java
                                            )
                                        )
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
                                    onSecurity = {
                                        menuOpen = false
                                        if (isLoggedIn) showMfaDialog.value = true
                                        else onOpenLogin()
                                    },
                                    onPrivacy = {
                                        menuOpen = false
                                        hostActivity.startActivity(
                                            Intent(hostActivity, WebviewflightActivity::class.java)
                                                .putExtra("start_card", "privacy_policy")
                                        )
                                        hostActivity.overridePendingTransition(
                                            R.anim.enter_animation,
                                            R.anim.exit_animation
                                        )
                                    },
                                    onLoginLogout = {
                                        menuOpen = false
                                        if (isLoggedIn) openLogoutDialog() else onOpenLogin()
                                    },

                                    flags = MenuFeatureFlags(
                                        vibrant = false,
                                        groupLabels = false,
                                        groupDividers = false,
                                        supportingText = false,
                                        trailingIcon = false,
                                        badge = false,
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
        }

    ) { padding ->


                Box(Modifier.fillMaxSize()) {

                    // 1) record page background into backdrop
                    Box(
                        Modifier
                            .matchParentSize()
                            .layerBackdrop(pageBackdrop)
                    ) {
                        ProfileBackdropImageLayer(
                            modifier = Modifier.matchParentSize(),
                            lightRes = R.drawable.light_grid_pattern,
                            darkRes = R.drawable.dark_grid_pattern,
                            imageAlpha = when (themeMode) {
                                2 -> if (isSystemInDarkTheme()) 0.82f else 0.62f
                                3 -> if (isSystemInDarkTheme()) 0.70f else 0.54f
                                4 -> if (isSystemInDarkTheme()) 0.58f else 0.45f
                                5 -> if (isSystemInDarkTheme()) 1f else 0.88f
                                6 -> 0f
                                7 -> 0f
                                else -> if (isSystemInDarkTheme()) 1f else 0.8f
                            },
                            scrimDark = if (themeMode == 7) 1f else 0f,
                            scrimLight = 0f
                        )
                        val themeWash = when (themeMode) {
                            1 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                            2 -> MaterialTheme.colorScheme.surface.copy(alpha = 0.16f)
                            3 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                            4 -> Color.White.copy(alpha = if (isSystemInDarkTheme()) 0.06f else 0.22f)
                            5 -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f)
                            6 -> MaterialTheme.colorScheme.background.copy(alpha = 1f)
                            7 -> Color.Black
                            else -> Color.Transparent
                        }
                        if (themeWash.alpha > 0f) {
                            Box(
                                Modifier
                                    .matchParentSize()
                                    .background(themeWash)
                            )
                        }
                    }


                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(1f)
                    ) {
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
                            val style = LocalProfileBackdropStyle.current
                            val elevated = style != ProfileBackdropStyle.Solid


                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                shape = RoundedCornerShape(22.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant, // ✅ SAME AS QUICK ACTIONS
                                shadowElevation = if (elevated) 1.dp else 0.dp    // ✅ SAME AS QUICK ACTIONS
                            ) {

                                val headerShape = RoundedCornerShape(22.dp)
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
                                                backdrop = pageBackdrop,
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
                                            color = dialogSubTextColor,
                                            modifier = Modifier.padding(
                                                start = 18.dp,
                                                top = 12.dp,
                                                bottom = 0.dp
                                            )
                                        )
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(
                                                    start = 16.dp,
                                                    end = 16.dp,
                                                    top = 12.dp,
                                                    bottom = 14.dp
                                                ),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val alpha = if (isLoggedIn) 1f else 0.4f
                                            val avatarShape = RoundedCornerShape(38.dp)

                                            Surface(
                                                modifier = Modifier
                                                    .size(76.dp)
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
                                                tonalElevation = 1.dp,
                                                color = MaterialTheme.colorScheme.surface,
                                                shape = avatarShape
                                            ) {
                                                val rawPhoto = ui.photoRaw.trim()

                                                var avatarFailed by remember(rawPhoto) {
                                                    mutableStateOf(
                                                        false
                                                    )
                                                }

                                                val avatarState by produceState(
                                                    initialValue = if (rawPhoto.isBlank()) AvatarLoadState.Idle else AvatarLoadState.Loading,
                                                    key1 = rawPhoto,
                                                    key2 = refreshTick,
                                                    key3 = avatarVersion
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
                                                    val local =
                                                        AvatarDiskCache.localFile(context, rawPhoto)
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
                                                    val session =
                                                        SupabaseManager.client.auth.currentSessionOrNull()
                                                    if (session != null) {
                                                        val fresh =
                                                            SupabaseStorageUploader.createSignedUrl(
                                                                objectPath = rawPhoto,
                                                                authToken = session.accessToken,
                                                                bucket = "profile-photos"
                                                            )
                                                        if (!fresh.isNullOrBlank()) {
                                                            SignedUrlCache.put(
                                                                rawPhoto,
                                                                fresh,
                                                                60 * 60
                                                            )
                                                            AvatarDiskCache.cacheFromSignedUrl(
                                                                context,
                                                                rawPhoto,
                                                                fresh
                                                            )
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
                                                            ProfileAvatarImage(
                                                                data = st.data,
                                                                avatarVersion = avatarVersion,
                                                                shape = avatarShape,
                                                                onError = {
                                                                    avatarFailed = true
                                                                    if (rawPhoto.isNotBlank()) SignedUrlCache.invalidate(rawPhoto)
                                                                }
                                                            )
                                                        } else {
                                                            ProfileAvatarFallback()
                                                        }
                                                    }

                                                    AvatarLoadState.Loading -> {
                                                        ProfileAvatarSkeleton(shape = avatarShape)
                                                    }

                                                    AvatarLoadState.Idle,
                                                    AvatarLoadState.Failed -> {
                                                        ProfileAvatarFallback()
                                                    }
                                                }
                                            }

                                            Spacer(Modifier.width(14.dp))

                                            Column(Modifier.weight(1f)) {
                                                val nameStyle = MaterialTheme.typography.titleLarge
                                                val dialogTextColor = MaterialTheme.colorScheme.onSurface

                                                Text(
                                                    text = ui.displayName.ifEmpty { stringResource(R.string.default_user_name) },
                                                    style = nameStyle.copy(fontWeight = FontWeight.Bold),
                                                    fontSize = nameStyle.fontSize.us(bodyS),
                                                    color = dialogTextColor,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(Modifier.height(4.dp))

                                                val secondaryStyle =
                                                    MaterialTheme.typography.bodyMedium
                                                Text(
                                                    text = ui.secondaryTextRaw.ifEmpty {
                                                        stringResource(
                                                            R.string.unknown_contact
                                                        )
                                                    },
                                                    style = secondaryStyle,
                                                    fontSize = secondaryStyle.fontSize.us(bodyS),
                                                    color = dialogSubTextColor,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(Modifier.height(4.dp))

                                                val statusStyle =
                                                    MaterialTheme.typography.labelMedium
                                                Text(
                                                    text = if (isLoggedIn) "🟢 Signed in" else "⚪ Guest mode",
                                                    style = statusStyle,
                                                    fontSize = statusStyle.fontSize.us(labelS),
                                                    color = dialogSubTextColor
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
                                shape = RoundedCornerShape(22.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shadowElevation = if (elevated) 1.dp else 0.dp   // 🔥 SAME AS QUICK ACTIONS
                            ) {

                                val headerShape = RoundedCornerShape(22.dp)
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
                                                enabled = useGlassBackdrop,
                                            )
                                    )

                                    // ✅ 3) CONTENT ON TOP (sharp)
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                    ) {

                                        val infoTitle = MaterialTheme.typography.labelLarge
                                        Text(
                                            text = "Information",
                                            style = infoTitle,
                                            fontSize = infoTitle.fontSize.us(labelS),
                                            color = dialogSubTextColor,
                                            modifier = Modifier.padding(
                                                start = 18.dp,
                                                bottom = 2.dp
                                            )
                                        )
                                        InfoRow(
                                            iconRes = R.drawable.ic_oui_email,
                                            label = stringResource(R.string.email),
                                            value = stringResource(
                                                R.string.label_email,
                                                ui.emailRaw
                                            )
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
                                            value = stringResource(
                                                R.string.label_birthday,
                                                ui.birthdayRaw
                                            )
                                        )
                                    }
                                }
                            }

                            QuickActionsCard(
                                isLoggedIn = isLoggedIn,
                                backdrop = pageBackdrop,
                                useGlassBackdrop = useGlassBackdrop,
                                onEdit = {
                                    editProfileLauncher.launch(
                                        Intent(hostActivity, EditProfileComposeActivity::class.java)
                                    )
                                    hostActivity.overridePendingTransition(
                                        R.anim.enter_animation,
                                        R.anim.exit_animation
                                    )
                                },

                                onLogin = {
                                    onOpenLogin()
                                },

                                onTheme = cycleTheme,

                                onSecurity = {
                                    if (isLoggedIn) showMfaDialog.value = true else onOpenLogin()
                                },
                                onRequireProfile = {
                                    FancyPillToast.show(hostActivity, createProfileMsg, 3000L)
                                }
                            ) {
                                hostActivity.startActivity(
                                    Intent(hostActivity, WebviewflightActivity::class.java)
                                        .putExtra("start_card", "privacy_policy")
                                )
                                hostActivity.overridePendingTransition(
                                    R.anim.enter_animation,
                                    R.anim.exit_animation
                                )
                            }

                            Spacer(Modifier.height(24.dp))
                        }
                    }

                }
            }

        }
        if (showMfaDialog.value) {
            MfaSettingsDialog(
                backdrop = profileContentBackdrop,
                onDismiss = { showMfaDialog.value = false },
                onEnabled = {
                    refreshTick += 1
                    FancyPillToast.show(
                        hostActivity,
                        "Two-factor authentication enabled",
                        2200L
                    )
                }
            )
        }

    }

}





@Composable
private fun MfaSettingsDialog(
    backdrop: LayerBackdrop,
    onDismiss: () -> Unit,
    onEnabled: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dialogShape = RoundedCornerShape(30.dp)
    val fieldShape = RoundedCornerShape(18.dp)
    val glassEnabled = LocalProfileBackdropStyle.current != ProfileBackdropStyle.Solid

    var isChecking by remember { mutableStateOf(true) }
    var isEnabled by remember { mutableStateOf(false) }
    var isBusy by remember { mutableStateOf(false) }
    var enrollment by remember { mutableStateOf<MfaEnrollmentInfo?>(null) }
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val dialogTextColor = MaterialTheme.colorScheme.onSurface
    val dialogSubTextColor = MaterialTheme.colorScheme.onSurfaceVariant

    LaunchedEffect(Unit) {
        isChecking = true
        isEnabled = SupabaseAuthRepo.hasVerifiedTotpFactor()
        isChecking = false
    }
    val dialogSurfaceTint = if (isSystemInDarkTheme()) {
        Color(0xFF202124).copy(alpha = 0.62f)
    } else {
        Color.White.copy(alpha = 0.72f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.38f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 34.dp)
                .widthIn(max = 352.dp)
                .shadow(18.dp, dialogShape, clip = false)
                .clip(dialogShape)
        ) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )

            Box(
                Modifier
                    .matchParentSize()
                    .clip(dialogShape)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { dialogShape },
                        shadow = null,
                        highlight = {
                            Highlight(
                                width = 0.45.dp,
                                blurRadius = 1.2.dp,
                                alpha = 0.22f,
                                style = HighlightStyle.Plain
                            )
                        },
                        effects = {
                            vibrancy()
                            blur(4.dp.toPx())
                            lens(
                                refractionHeight = 22.dp.toPx(),
                                refractionAmount = 32.dp.toPx(),
                                depthEffect = false,
                                chromaticAberration = true
                            )
                        },
                        onDrawSurface = {
                            drawRect(dialogSurfaceTint)
                        }
                    )
            )

            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Two-factor authentication",
                    color = dialogTextColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    lineHeight = 21.sp
                )

                when {
                    isChecking -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Checking security status...",
                                color = dialogSubTextColor,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    isEnabled -> {
                        Text(
                            text = "2FA is enabled for this account. Next time you log in, you enter your password first, then your authenticator code.",
                            color = dialogSubTextColor,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }

                    enrollment == null -> {
                        Text(
                            text = "Add an authenticator app to protect your login with a 6-digit code.",
                            color = dialogSubTextColor,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }

                    else -> {
                        val info = enrollment!!
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = Color.White.copy(alpha = 0.96f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(info.qrCodeSvg.toByteArray(Charsets.UTF_8))
                                        .decoderFactory(SvgDecoder.Factory())
                                        .build(),
                                    contentDescription = "Authenticator QR code",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.size(184.dp)
                                )
                            }
                        }

                        Text(
                            text = "Scan the QR code, then enter the 6-digit code from your authenticator app.",
                            color = dialogSubTextColor,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (glassEnabled) 0.54f else 1f),
                            shape = fieldShape
                        ) {
                            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Manual setup key",
                                        color = dialogSubTextColor,
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp
                                    )
                                    TextButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(ClipboardManager::class.java)
                                            clipboard?.setPrimaryClip(
                                                ClipData.newPlainText("Authenticator setup key", info.secret)
                                            )
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ContentCopy,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text("Copy key", fontSize = 11.sp)
                                    }
                                }
                                Text(
                                    text = info.secret,
                                    color = dialogTextColor,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        TextField(
                            value = code,
                            onValueChange = { value ->
                                code = value.filter(Char::isDigit).take(6)
                                error = null
                            },
                            singleLine = true,
                            label = { Text("Authenticator code") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            shape = fieldShape,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (glassEnabled) 0.54f else 1f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (glassEnabled) 0.54f else 1f),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(fieldShape)
                        )
                    }
                }

                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isChecking && !isBusy && !isEnabled) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel", fontSize = 13.sp)
                        }
                    }
                    Spacer(Modifier.width(8.dp))

                    when {
                        isChecking || isEnabled -> {
                            Button(onClick = onDismiss) {
                                Text("Done", fontSize = 13.sp)
                            }
                        }

                        enrollment == null -> {
                            Button(
                                enabled = !isBusy,
                                onClick = {
                                    isBusy = true
                                    error = null
                                    scope.launch {
                                        val result = SupabaseAuthRepo.beginTotpEnrollment()
                                        isBusy = false
                                        result.fold(
                                            onSuccess = { enrollment = it },
                                            onFailure = { ex ->
                                                val raw = ex.message.orEmpty()
                                                error = when {
                                                    raw.contains("not enabled", ignoreCase = true) ->
                                                        "Authenticator 2FA is not enabled in Supabase."
                                                    raw.contains("session", ignoreCase = true) ->
                                                        "Please log out and log in again before enabling 2FA."
                                                    raw.isNotBlank() -> raw.take(140)
                                                    else -> "Could not start 2FA setup."
                                                }
                                            }
                                        )
                                    }
                                }
                            ) {
                                if (isBusy) {
                                    CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text("Enable", fontSize = 13.sp)
                            }
                        }

                        else -> {
                            Button(
                                enabled = !isBusy && code.length == 6,
                                onClick = {
                                    val info = enrollment ?: return@Button
                                    isBusy = true
                                    error = null
                                    scope.launch {
                                        val result = SupabaseAuthRepo.verifyTotpEnrollment(info.factorId, code)
                                        isBusy = false
                                        result.fold(
                                            onSuccess = {
                                                isEnabled = true
                                                enrollment = null
                                                code = ""
                                                onEnabled()
                                            },
                                            onFailure = {
                                                code = ""
                                                error = "Invalid code. Check your authenticator app and try again."
                                            }
                                        )
                                    }
                                }
                            ) {
                                if (isBusy) {
                                    CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text("Verify", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
        }
    }

@Composable
fun QuickActionsCard(
    isLoggedIn: Boolean,
    backdrop:LayerBackdrop,
    useGlassBackdrop: Boolean,
    onEdit: () -> Unit,
    onLogin: () -> Unit,
    onTheme: () -> Unit,
    onSecurity: () -> Unit,
    onRequireProfile: () -> Unit,
    onPrivacy: () -> Unit
) {
    val style = LocalProfileBackdropStyle.current
    val elevated = style != ProfileBackdropStyle.Solid
    val scales = rememberUiScales()
    val labelS = scales.label
    val cardShape = RoundedCornerShape(28.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
        shape = cardShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp,
        shadowElevation = if (elevated) 1.dp else 1.dp // ✅ same as top bar
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
        ) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Box(
                Modifier
                    .matchParentSize()
                    .clip(cardShape)
                    .profileGlassBackdrop(
                        backdrop = backdrop,
                        shape = cardShape,
                        enabled = useGlassBackdrop
                    )
            )
            Column(Modifier.padding(14.dp)) {
                val qaTitle = MaterialTheme.typography.labelLarge
                val dialogSubTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                Text(
                    text = "Quick actions",
                    style = qaTitle,
                    fontSize = qaTitle.fontSize.us(labelS),
                    color = dialogSubTextColor
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
                val securityClick = if (isLoggedIn) onSecurity else onRequireProfile
                val securityAlpha = if (isLoggedIn) 1f else 0.6f

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
                            modifier = Modifier
                                .weight(1f)
                                .alpha(securityAlpha),
                            onClick = securityClick,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Filled.Security, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "2FA",
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
                            icon = Icons.Filled.Security,
                            label = "2FA",
                            onClick = securityClick,
                            alpha = securityAlpha
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
            val dialogSubTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            Text(
                text = label,
                style = qaLabel,
                fontSize = qaLabel.fontSize.us(labelS),
                color = dialogSubTextColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )

        }
    }


    @Composable
    private fun InfoRow(
        @DrawableRes iconRes: Int,
        label: String,
        value: String
    ) {
        val scales = rememberUiScales()
        val bodyS = scales.body
        val labelS = scales.label
        val dialogTextColor = MaterialTheme.colorScheme.onSurface

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(19.dp)
            )

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                val infoLabel = MaterialTheme.typography.labelMedium
                val dialogSubTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                Text(
                    text = label,
                    style = infoLabel.copy(fontWeight = FontWeight.Bold),
                    fontSize = infoLabel.fontSize.us(labelS),
                    color = dialogSubTextColor
                )

                Spacer(Modifier.height(1.dp))
                val infoValue = MaterialTheme.typography.bodyLarge
                Text(
                    text = value,
                    style = infoValue,
                    fontSize = infoValue.fontSize.us(bodyS * 0.92f),
                    color = dialogTextColor
                )
            }
        }
    }



