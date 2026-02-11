@file:Suppress("DEPRECATION")

package com.flights.studio

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditProfileComposeActivity : AppCompatActivity() {

    private lateinit var userPrefs: UserPreferencesManager

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userPrefs = UserPreferencesManager(this)
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // Finish in a transition-friendly way
                    finishAfterTransition()

                    // Fast, pro slide (no custom anim files)
                    overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation)
                }
            }
        )

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

            val hostActivity = this@EditProfileComposeActivity // ✅ THIS

            FlightsTheme(profileBackdropStyle = ProfileBackdropStyle.Solid) {
                EditProfileScreen(
                    userPrefs = userPrefs,
                    hostActivity = hostActivity,              // ✅ PASS IT
                    onClose = {
                        finish()
                        overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation)
                    }
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EditProfileScreen(
    userPrefs: UserPreferencesManager,
    hostActivity: AppCompatActivity,   // ✅ add this
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    BackHandler { onClose() }

    // ---------- UI state ----------
    var saving by remember { mutableStateOf(false) }

    var fullName by rememberSaveable { mutableStateOf(userPrefs.userName.orEmpty()) }
    var bio by rememberSaveable { mutableStateOf(userPrefs.userBio.orEmpty()) }
    var birthday by rememberSaveable { mutableStateOf(userPrefs.userBirthday.orEmpty()) }

    // store PATH in prefs (or content:// while previewing)
    var photoRaw by rememberSaveable { mutableStateOf(userPrefs.userPhotoUriString.orEmpty()) }
    var avatarFailed by remember(photoRaw) { mutableStateOf(false) }

    // ---------- picker ----------
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult

        // persist permission for content://
        if (uri.scheme == "content") {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }

        // instant preview
        photoRaw = uri.toString()
        userPrefs.setPhotoString(photoRaw)
        userPrefs.userInitials = null
        avatarFailed = false
    }

    // ---------- load from server once (to avoid “first open no avatar”) ----------
    LaunchedEffect(Unit) {
        // if not logged in, just edit local fields (you can also block)
        val session = SupabaseManager.client.auth.currentSessionOrNull() ?: return@LaunchedEffect
        val userId = session.user?.id ?: return@LaunchedEffect
        val token = session.accessToken

        val profile = runCatching {
            SupabaseProfileDownloader.fetchProfile(userId = userId, authToken = token)
        }.getOrNull() ?: return@LaunchedEffect

        // fill fields if empty or changed
        if (fullName.isBlank()) fullName = profile.fullName.orEmpty()
        if (bio.isBlank()) bio = profile.bio.orEmpty()
        if (birthday.isBlank()) birthday = profile.birthday.orEmpty()

        val remoteRaw = profile.photoUri?.trim().orEmpty()
        if (remoteRaw.isNotBlank()) {
            val canonicalPath = when {
                remoteRaw.startsWith("http", ignoreCase = true) ->
                    extractStoragePathIfSignedUrl(remoteRaw, bucket = "profile-photos") ?: remoteRaw
                else -> remoteRaw
            }

            // normalize DB if it was a signed url
            if (canonicalPath != remoteRaw && !canonicalPath.startsWith("http", true)) {
                runCatching {
                    SupabaseStorageUploader.updateProfilePhotoUrl(
                        userId = userId,
                        authToken = token,
                        photoPath = canonicalPath
                    )
                }
            }

            if (canonicalPath.isNotBlank() && photoRaw != canonicalPath) {
                photoRaw = canonicalPath
                userPrefs.setPhotoString(canonicalPath)
                userPrefs.userInitials = null
            }
        }
    }

    // ---------- avatar resolve (LOCAL -> signed url) ----------
    val localAvatarFile = remember(photoRaw) {
        if (photoRaw.isNotBlank()
            && !photoRaw.startsWith("http", true)
            && !photoRaw.startsWith("content", true)
            && !photoRaw.startsWith("file", true)
        ) {
            val f = AvatarDiskCache.localFile(context, photoRaw)
            if (f.exists() && f.length() > 0L) f else null
        } else null
    }

    var lastGoodUrl by rememberSaveable(photoRaw) { mutableStateOf<String?>(null) }

    val signedForUi by produceState<String?>(
        initialValue = null,
        key1 = photoRaw
    ) {
        value = null
        val raw = photoRaw.trim()
        if (raw.isBlank()) return@produceState

        // direct usable uri/url
        if (raw.startsWith("http", true) || raw.startsWith("content", true) || raw.startsWith("file", true)) {
            value = raw
            lastGoodUrl = raw
            return@produceState
        }

        // 1) instant memory cache
        SignedUrlCache.getValid(raw)?.let { cached ->
            value = cached
            lastGoodUrl = cached
        }

        // 2) wait a bit for session on cold start
        var session = SupabaseManager.client.auth.currentSessionOrNull()
        if (session == null) {
            repeat(20) {
                delay(100)
                session = SupabaseManager.client.auth.currentSessionOrNull()
                if (session != null) return@repeat
            }
        }
        val ready = session ?: run {
            value = lastGoodUrl
            return@produceState
        }

        // 3) sign
        val fresh = SupabaseStorageUploader.createSignedUrl(
            objectPath = raw,
            authToken = ready.accessToken,
            bucket = "profile-photos"
        )

        if (!fresh.isNullOrBlank()) {
            value = fresh
            lastGoodUrl = fresh
            SignedUrlCache.put(raw, fresh, 60 * 60)

            // seed disk cache so next app launch is instant
            AvatarDiskCache.cacheFromSignedUrl(context, raw, fresh)
        } else {
            value = lastGoodUrl
        }
    }

    val dataToLoad: Any? = remember(photoRaw, localAvatarFile, signedForUi, lastGoodUrl) {
        when {
            photoRaw.isBlank() -> null
            photoRaw.startsWith("http", true) || photoRaw.startsWith("content", true) || photoRaw.startsWith("file", true) ->
                photoRaw
            localAvatarFile != null -> localAvatarFile
            else -> signedForUi ?: lastGoodUrl
        }
    }

    LaunchedEffect(dataToLoad) { avatarFailed = false }

    // ---------- UI ----------
    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 0.dp,
                shadowElevation = 3.dp,
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
            ) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "Edit profile",
                            style = MaterialTheme.typography.titleMediumEmphasized.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBackIos, contentDescription = "Back")
                        }
                    },
                    actions = {
                        TextButton(
                            enabled = !saving,
                            onClick = {
                                scope.launch {
                                    if (saving) return@launch // prevent double taps
                                    saving = true
                                    try {
                                        val session = SupabaseManager.client.auth.currentSessionOrNull()
                                        if (session == null) {
                                            FancyPillToast.show(
                                                activity = hostActivity,
                                                text = "Not logged in",
                                                durationMs = 2200L
                                            )
                                            return@launch
                                        }

                                        val userId = session.user?.id ?: return@launch
                                        val token = session.accessToken

                                        // 1) handle avatar upload if user picked content://
                                        var finalPhotoPath: String? = photoRaw.trim()
                                        val pickedUri = photoRaw.trim()
                                            .takeIf { it.startsWith("content", ignoreCase = true) }
                                            ?.toUri()

                                        if (pickedUri != null) {

                                            val previousPath = userPrefs.userPhotoUriString?.trim().orEmpty()

                                            val path = runCatching {
                                                SupabaseStorageUploader.uploadProfilePhotoAndReturnPath(
                                                    context = context,
                                                    userId = userId,
                                                    authToken = token,
                                                    photoUri = pickedUri,
                                                    bucket = "profile-photos"
                                                )
                                            }.getOrNull()

                                            if (path.isNullOrBlank()) {
                                                FancyPillToast.show(
                                                    activity = hostActivity,
                                                    text = "❌ Upload failed",
                                                    durationMs = 2400L
                                                )
                                                return@launch
                                            }

                                            finalPhotoPath = path

                                            // write disk cache from picked bytes
                                            runCatching {
                                                val bytes = withContext(Dispatchers.IO) {
                                                    context.contentResolver.openInputStream(pickedUri)
                                                        ?.use { it.readBytes() }
                                                } ?: byteArrayOf()

                                                if (bytes.isNotEmpty()) {
                                                    AvatarDiskCache.localFile(context, path).writeBytes(bytes)
                                                }
                                            }

                                            // invalidate caches
                                            SignedUrlCache.invalidate(path)

                                            if (previousPath.isNotBlank() && previousPath != path) {
                                                AvatarDiskCache.delete(context, previousPath)
                                                SignedUrlCache.invalidate(previousPath)
                                            }

                                            // store new path locally
                                            userPrefs.setPhotoString(path)
                                            userPrefs.userInitials = null
//                                            photoRaw = path
                                        }

                                        val birthdayIso: String = normalizeBirthdayForDbQuick(birthday).orEmpty()

                                        val okProfile = runCatching {
                                            SupabaseProfilesRepo.upsertMyProfile(
                                                fullName = fullName.trim(),
                                                phone = userPrefs.userPhone.orEmpty(),
                                                email = userPrefs.userEmail.orEmpty(),
                                                bio = bio.trim(),
                                                birthday = birthdayIso
                                            )
                                        }.isSuccess

                                        val okBioBday = runCatching {
                                            SupabaseProfilesRepo.updateMyBioBirthday(
                                                bio = bio.trim(),
                                                birthday = birthdayIso
                                            )
                                        }.isSuccess

                                        // 4) save photo path into DB if present
                                        val okPhoto = finalPhotoPath
                                            ?.takeIf { it.isNotBlank() && !it.startsWith("content", ignoreCase = true) }
                                            ?.let { path ->
                                                runCatching {
                                                    SupabaseStorageUploader.updateProfilePhotoUrl(
                                                        userId = userId,
                                                        authToken = token,
                                                        photoPath = path
                                                    )
                                                }.getOrDefault(false)
                                            } ?: true

                                        // 5) update prefs for UI (optional lint can’t see reads across files)
                                        userPrefs.userName = fullName.trim()
                                        userPrefs.userBio = bio.trim()
                                        userPrefs.userBirthday = birthdayIso

                                        if (okProfile && okBioBday && okPhoto) {
                                            FancyPillToast.show(
                                                activity = hostActivity,
                                                text = "✅ Saved",
                                                durationMs = 1600L
                                            )

                                            hostActivity.setResult(Activity.RESULT_OK)   // ✅ ADD THIS
                                            onClose()
                                        } else {
                                            FancyPillToast.show(
                                                activity = hostActivity,
                                                text = "Saved with warnings",
                                                durationMs = 2200L
                                            )
                                        }
                                    } finally {
                                        saving = false
                                    }
                                }
                            }
                        ) {
                            Text(if (saving) "Saving…" else "Save")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // Avatar card
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = Color.Transparent)
            ) {
                val shape = RoundedCornerShape(28.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {

                        Surface(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(RoundedCornerShape(46.dp))
                                .clickable(enabled = !saving) {
                                    photoPicker.launch(arrayOf("image/*"))
                                },
                            tonalElevation = 2.dp,
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(46.dp)
                        ) {
                            if (dataToLoad != null && !avatarFailed) {

                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(dataToLoad)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                    onError = {
                                        avatarFailed = true
                                        val raw = photoRaw.trim()
                                        if (raw.isNotBlank() && !raw.startsWith("http", true)) {
                                            SignedUrlCache.invalidate(raw)
                                        }
                                    }
                                )

                            } else {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = (userPrefs.userInitials ?: "").ifBlank { "?" },
                                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                        }

                        Spacer(Modifier.width(14.dp))

                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Profile photo",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Normal),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Tap to change",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Icon(
                            imageVector = Icons.Filled.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.alpha(if (saving) 0.4f else 1f)
                        )
                    }
                }
            }

            EditableInfoRow(
                label = "Name",
                value = fullName,
                enabled = !saving,
                onValueChange = { fullName = it }
            )

            EditableInfoRow(
                label = "Bio",
                value = bio,
                enabled = !saving,
                singleLine = false,
                onValueChange = { bio = it }
            )

            EditableInfoRow(
                label = "Birthday",
                value = birthday,
                enabled = !saving,
                onValueChange = { birthday = it }
            )


            Spacer(Modifier.height(6.dp))

            FilledTonalButton(
                onClick = { onClose() },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Text("Cancel")
            }
        }
    }
}

/**
 * Small helper so this file is standalone.
 * Accepts:
 * - yyyy-MM-dd (returns as-is)
 * - MM/dd/yyyy (converts)
 * else returns null (keeps DB clean)
 */
private fun normalizeBirthdayForDbQuick(input: String): String? {
    val s = input.trim()
    if (s.isBlank()) return null

    if (Regex("""\d{4}-\d{2}-\d{2}""").matches(s)) return s

    return runCatching {
        val parts = s.split("/")
        if (parts.size != 3) return null
        val mm = parts[0].padStart(2, '0')
        val dd = parts[1].padStart(2, '0')
        val yyyy = parts[2]
        if (yyyy.length != 4) return null
        "$yyyy-$mm-$dd"
    }.getOrNull()
}

@Composable
private fun EditableInfoRow(
    label: String,
    value: String,
    enabled: Boolean,
    singleLine: Boolean = true,
    onValueChange: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(6.dp))

            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = singleLine,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

