package com.flights.studio

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBackIos
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SoftwareUpdateActivity : ComponentActivity() {

    private val vm by viewModels<SoftwareUpdateViewModel> {
        SoftwareUpdateViewModel.Factory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        maybeRequestUpdateNotificationPermission()
        handleUpdateIntent(intent)

        setContent {
            val darkTheme = isSystemInDarkTheme()

            SideEffect {
                WindowCompat.setDecorFitsSystemWindows(window, false)

                val controller = WindowInsetsControllerCompat(window, window.decorView)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }

            MaterialTheme {
                Surface {
                    SoftwareUpdateRoute(
                        state = vm.state,
                        softwareInfo = vm.softwareInfo,
                        latestUpdate = vm.latestUpdate,
                        onBack = { finish() },
                        onCheck = { vm.checkForUpdates() },
                        onDownload = { vm.downloadAndInstall() },
                        onInstall = { vm.installNow() },
                        onRefreshLastUpdate = { vm.refreshLatestUpdateChangelog() }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleUpdateIntent(intent)
    }

    private fun handleUpdateIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(AppUpdateNotificationManager.EXTRA_CHECK_FOR_UPDATES, false) == true) {
            vm.checkForUpdates()
        }
    }

    private fun maybeRequestUpdateNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (AppUpdateNotificationManager.canPostNotifications(this)) return
        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 4402)
    }
}

sealed interface UpdateScreenState {
    data class Home(
        val checking: Boolean = false,
        val statusMessage: String? = null,
        val hideCheckButton: Boolean = false
    ) : UpdateScreenState

    data class Details(
        val currentVersionCode: Long,
        val currentVersionName: String,
        val remote: RemoteUpdateInfo,
        val downloading: Boolean = false,
        val progress: DownloadProgress? = null,
        val downloadedUri: Uri? = null
    ) : UpdateScreenState

    data class Error(val message: String) : UpdateScreenState
}

data class SoftwareInfoUi(
    val appName: String = "JAC",
    val versionCode: Long = 0L,
    val versionName: String = "",
    val androidVersion: String = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        android.os.Build.VERSION.RELEASE_OR_CODENAME
    } else {
        android.os.Build.VERSION.RELEASE
    },
    val deviceModel: String = android.os.Build.MODEL,
    val deviceBrand: String = android.os.Build.BRAND,
    val architecture: String = android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown",
    val lastCheckedText: String = "Not checked yet"
)

data class LatestUpdateUi(
    val remote: RemoteUpdateInfo? = null,
    val loading: Boolean = false,
    val errorMessage: String? = null,
    val loadedAtText: String = ""
)

private enum class UpdateSheetType {
    SOFTWARE_INFO,
    LAST_UPDATE
}

class SoftwareUpdateViewModel(
    private val appContext: android.content.Context
) : ViewModel() {

    var state by mutableStateOf<UpdateScreenState>(UpdateScreenState.Home())
        private set

    var softwareInfo by mutableStateOf(SoftwareInfoUi())
        private set

    var latestUpdate by mutableStateOf(LatestUpdateUi())
        private set

    init {
        viewModelScope.launch {
            val versionCode = AppUpdater.getCurrentVersionCode(appContext)
            val versionName = appContext.packageManager
                .getPackageInfo(appContext.packageName, 0)
                .versionName
                .orEmpty()

            softwareInfo = softwareInfo.copy(
                versionCode = versionCode,
                versionName = versionName
            )
        }
    }

    fun checkForUpdates() {
        val current = state as? UpdateScreenState.Home ?: return

        state = current.copy(
            checking = true,
            statusMessage = null,
            hideCheckButton = false
        )

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val remote = AppUpdateRepository.fetchRemoteUpdate()
                val localCode = AppUpdater.getCurrentVersionCode(appContext)
                val localName = appContext.packageManager
                    .getPackageInfo(appContext.packageName, 0)
                    .versionName
                    .orEmpty()

                val nowText = SimpleDateFormat(
                    "MMMM d, yyyy 'at' h:mm a",
                    Locale.getDefault()
                ).format(Date())

                withContext(Dispatchers.Main) {
                    softwareInfo = softwareInfo.copy(
                        versionCode = localCode,
                        versionName = localName,
                        lastCheckedText = nowText
                    )

                    latestUpdate = LatestUpdateUi(
                        remote = remote,
                        loading = false,
                        loadedAtText = nowText
                    )

                    state = if (remote.versionCode > localCode) {
                        UpdateScreenState.Details(
                            currentVersionCode = localCode,
                            currentVersionName = localName,
                            remote = remote
                        )
                    } else {
                        UpdateScreenState.Home(
                            checking = false,
                            statusMessage = "Your app is up to date",
                            hideCheckButton = true
                        )
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    state = UpdateScreenState.Home(
                        checking = false,
                        statusMessage = "Update feed is unavailable right now",
                        hideCheckButton = false
                    )
                }
            }
        }
    }

    fun refreshLatestUpdateChangelog() {
        if (latestUpdate.loading) return

        latestUpdate = latestUpdate.copy(
            loading = true,
            errorMessage = null
        )

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val remote = AppUpdateRepository.fetchRemoteUpdate()
                val nowText = SimpleDateFormat(
                    "MMMM d, yyyy 'at' h:mm a",
                    Locale.getDefault()
                ).format(Date())

                withContext(Dispatchers.Main) {
                    latestUpdate = LatestUpdateUi(
                        remote = remote,
                        loading = false,
                        loadedAtText = nowText
                    )
                    softwareInfo = softwareInfo.copy(lastCheckedText = nowText)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    latestUpdate = latestUpdate.copy(
                        loading = false,
                        errorMessage = "Changelog is unavailable right now"
                    )
                }
            }
        }
    }

    fun downloadAndInstall() {
        val current = state as? UpdateScreenState.Details ?: return

        viewModelScope.launch {
            try {
                state = current.copy(
                    downloading = true,
                    progress = DownloadProgress(0, 0.0, 0.0, "--:--")
                )

                val uri = AppUpdater.downloadApk(appContext, current.remote.apkUrl) { progress ->
                    withContext(Dispatchers.Main) {
                        val latest = state as? UpdateScreenState.Details ?: return@withContext
                        state = latest.copy(
                            downloading = true,
                            progress = progress
                        )
                    }
                }

                val latest = state as? UpdateScreenState.Details ?: return@launch
                state = latest.copy(
                    downloading = false,
                    downloadedUri = uri
                )
            } catch (e: Exception) {
                state = UpdateScreenState.Error(
                    "Download failed: ${e.message ?: "Unknown error"}"
                )
            }
        }
    }

    fun installNow() {
        val current = state as? UpdateScreenState.Details ?: return
        val uri = current.downloadedUri ?: return
        AppUpdater.installApk(appContext, uri)
    }

    class Factory(private val context: android.content.Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SoftwareUpdateViewModel(context) as T
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SoftwareUpdateRoute(
    state: UpdateScreenState,
    softwareInfo: SoftwareInfoUi,
    latestUpdate: LatestUpdateUi,
    onBack: () -> Unit,
    onCheck: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onRefreshLastUpdate: () -> Unit
) {
    var sheetType by remember { mutableStateOf<UpdateSheetType?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val scope = rememberCoroutineScope()

    fun openSheet(type: UpdateSheetType) {
        sheetType = type
    }

    fun closeSheet() {
        scope.launch {
            sheetState.hide()
            sheetType = null
        }
    }

    when (state) {
        is UpdateScreenState.Home -> UpdateHomeScreen(
            state = state,
            onBack = onBack,
            onCheck = onCheck,
            onSoftwareInfo = { openSheet(UpdateSheetType.SOFTWARE_INFO) },
            onLastUpdate = {
                onRefreshLastUpdate()
                openSheet(UpdateSheetType.LAST_UPDATE)
            }
        )

        is UpdateScreenState.Details -> UpdateDetailsScreen(
            state = state,
            onBack = onBack,
            onDownload = onDownload,
            onInstall = onInstall,
            onSoftwareInfo = { openSheet(UpdateSheetType.SOFTWARE_INFO) },
            onLastUpdate = {
                onRefreshLastUpdate()
                openSheet(UpdateSheetType.LAST_UPDATE)
            }
        )

        is UpdateScreenState.Error -> ErrorScreen(
            message = state.message,
            onBack = onBack,
            onSoftwareInfo = { openSheet(UpdateSheetType.SOFTWARE_INFO) },
            onLastUpdate = {
                onRefreshLastUpdate()
                openSheet(UpdateSheetType.LAST_UPDATE)
            }
        )
    }

    val currentSheetType = sheetType

    val dark = isSystemInDarkTheme()

    if (currentSheetType != null) {
        ModalBottomSheet(
            onDismissRequest = { closeSheet() },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = if (dark) Color(0xFF1B1828) else Color.White,
            scrimColor = Color.Black.copy(alpha = if (dark) 0.52f else 0.34f),
            tonalElevation = 0.dp,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 4.dp)
                        .width(38.dp)
                        .height(4.dp)
                        .background(
                            color = if (dark) {
                                Color.White.copy(alpha = 0.18f)
                            } else {
                                Color(0xFFBFC7D3)
                            },
                            shape = RoundedCornerShape(999.dp)
                        )
                )
            }
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Transparent,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (dark) {
                        Color.White.copy(alpha = 0.08f)
                    } else {
                        Color(0xFFDCE2EA)
                    }
                )
            ) {
                when (currentSheetType) {
                    UpdateSheetType.SOFTWARE_INFO -> {
                        SoftwareInfoSheet(
                            softwareInfo = softwareInfo,
                            onDismiss = { closeSheet() }
                        )
                    }

                    UpdateSheetType.LAST_UPDATE -> {
                        LastUpdateSheet(
                            lastCheckedText = softwareInfo.lastCheckedText,
                            latestUpdate = latestUpdate,
                            onDismiss = { closeSheet() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdateHomeScreen(
    state: UpdateScreenState.Home,
    onBack: () -> Unit,
    onCheck: () -> Unit,
    onSoftwareInfo: () -> Unit,
    onLastUpdate: () -> Unit
) {
    val dark = isSystemInDarkTheme()
    val bg = if (dark) Color(0xFF120F20) else Color(0xFFF7F7FB)
    val accent = Color(0xFF77D4B2)
    val buttonBg = if (dark) Color(0xFF16372F) else Color(0xFF155E4D)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
    ) {
        UpdateGlow(
            modifier = Modifier
                .fillMaxSize()
        )

        TopBar(
            onBack = onBack,
            onSoftwareInfo = onSoftwareInfo,
            onLastUpdate = onLastUpdate
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "JAC",
                style = MaterialTheme.typography.displayMedium,
                color = accent,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(10.dp))

            val upToDate = state.statusMessage.equals("Your app is up to date", ignoreCase = true)

            Box(
                modifier = Modifier
                    .height(38.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                @Suppress("RemoveRedundantQualifierName")
                androidx.compose.animation.AnimatedVisibility(
                    visible = !state.statusMessage.isNullOrBlank(),
                    enter = fadeIn(animationSpec = tween(durationMillis = 1400)),
                    exit = fadeOut(animationSpec = tween(durationMillis = 250))
                ) {
                    Text(
                        text = state.statusMessage.orEmpty(),
                        textAlign = TextAlign.Center,
                        color = if (upToDate) {
                            if (dark) Color(0xFFBFF3DF) else Color(0xFF166B52)
                        } else if (dark) {
                            Color.White.copy(alpha = 0.62f)
                        } else {
                            Color(0xFF45454D).copy(alpha = 0.76f)
                        },
                        style = if (upToDate) {
                            MaterialTheme.typography.titleMedium
                        } else {
                            MaterialTheme.typography.bodyMedium
                        },
                        fontWeight = if (upToDate) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = !state.hideCheckButton || state.checking,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp)
                .navigationBarsPadding(),
            enter = fadeIn(animationSpec = tween(durationMillis = 160)) + expandVertically(),
            exit = fadeOut(animationSpec = tween(durationMillis = 140)) + shrinkVertically()
        ) {
            Button(
                onClick = onCheck,
                enabled = !state.checking,
                modifier = Modifier
                    .fillMaxWidth(0.64f)
                    .height(58.dp),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonBg,
                    contentColor = Color.White,
                    disabledContainerColor = buttonBg.copy(alpha = 0.78f),
                    disabledContentColor = Color.White.copy(alpha = 0.78f)
                )
            ) {
                if (state.checking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.2.dp,
                        color = Color.White
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Checking...",
                        color = Color.White
                    )
                } else {
                    Text(
                        "Check for updates",
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

}

@Composable
private fun UpdateDetailsScreen(
    state: UpdateScreenState.Details,
    onBack: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onSoftwareInfo: () -> Unit,
    onLastUpdate: () -> Unit
) {
    val dark = isSystemInDarkTheme()
    val bg = if (dark) Color(0xFF120F20) else Color(0xFFF5F6FA)
    val accent = Color(0xFF77D4B2)

    val headerHeight = 110.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp + headerHeight + 44.dp,
                bottom = 60.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(headerHeight + 0.dp))
            }


            items(state.remote.updates) { item ->
                UpdateBlockCard(item = item)
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(bg)
        ) {
            TopBar(
                onBack = onBack,
                onSoftwareInfo = onSoftwareInfo,
                onLastUpdate = onLastUpdate
            )

            UpdateHeaderHero(
                currentVersionCode = state.currentVersionCode,
                currentVersionName = state.currentVersionName,
                remoteVersionCode = state.remote.versionCode,
                remoteVersionName = state.remote.versionName,
                accent = accent,
                downloading = state.downloading,
                downloaded = state.downloadedUri != null,
                progress = state.progress?.percent ?: 0,
                onActionClick = {
                    if (state.downloadedUri != null) onInstall() else onDownload()
                },
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

    }
}

@Composable
private fun UpdateHeaderHero(
    currentVersionCode: Long,
    currentVersionName: String,
    remoteVersionCode: Int,
    remoteVersionName: String,
    accent: Color,
    downloading: Boolean,
    downloaded: Boolean,
    progress: Int,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dark = isSystemInDarkTheme()

    val container = if (dark) Color(0xFF1B1828) else Color.White
    val border = if (dark) {
        Color.White.copy(alpha = 0.08f)
    } else {
        Color(0xFFDCE2EA)
    }

    val primaryText = if (dark) Color.White else Color(0xFF111111)
    val secondaryText = if (dark) Color.White.copy(alpha = 0.72f) else Color(0xFF4B5563)
    val labelText = if (dark) Color.White.copy(alpha = 0.60f) else Color(0xFF6B7280)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        border = androidx.compose.foundation.BorderStroke(1.dp, border),
        elevation = CardDefaults.cardElevation(defaultElevation = if (dark) 2.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = if (dark) {
                                    listOf(
                                        accent.copy(alpha = 0.28f),
                                        Color(0xFF77B6FF).copy(alpha = 0.18f)
                                    )
                                } else {
                                    listOf(
                                        accent.copy(alpha = 0.20f),
                                        Color(0xFF77B6FF).copy(alpha = 0.12f)
                                    )
                                }
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "↑",
                        color = if (dark) Color.White else accent,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Version $remoteVersionCode",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (dark) Color.White else accent,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = remoteVersionName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = secondaryText
                    )
                }

                HeaderActionPill(
                    accent = accent,
                    dark = dark,
                    downloading = downloading,
                    downloaded = downloaded,
                    progress = progress,
                    onClick = onActionClick
                )
            }
            HorizontalDivider(color = border)
            Spacer(Modifier.height(12.dp))
            if (downloading) {
                LinearHeaderProgress(
                    progress = progress,
                    accent = accent,
                    dark = dark
                )
                Spacer(Modifier.height(12.dp))
            }

            CompactVersionRow(
                label = "Current",
                value = "$currentVersionName ($currentVersionCode)",
                labelColor = labelText,
                valueColor = primaryText
            )

            Spacer(Modifier.height(8.dp))

            CompactVersionRow(
                label = "Available",
                value = "$remoteVersionName ($remoteVersionCode)",
                labelColor = labelText,
                valueColor = primaryText
            )
        }
    }
}

@Composable
private fun LinearHeaderProgress(
    progress: Int,
    accent: Color,
    dark: Boolean
) {
    val track = if (dark) Color.White.copy(alpha = 0.08f) else Color(0xFFE8ECF3)

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Downloading update",
                style = MaterialTheme.typography.labelMedium,
                color = if (dark) Color.White.copy(alpha = 0.72f) else Color(0xFF667085)
            )
            Text(
                text = "$progress%",
                style = MaterialTheme.typography.labelMedium,
                color = if (dark) Color.White else accent,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(track, RoundedCornerShape(999.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0, 100) / 100f)
                    .height(8.dp)
                    .background(accent, RoundedCornerShape(999.dp))
            )
        }
    }
}

@Composable
private fun HeaderActionPill(
    accent: Color,
    dark: Boolean,
    downloading: Boolean,
    downloaded: Boolean,
    progress: Int,
    onClick: () -> Unit
) {
    val bg = when {
        downloading -> if (dark) Color(0xFF243246) else Color(0xFFE8F2FF)
        downloaded -> if (dark) Color(0xFF183626) else Color(0xFFE4F6EC)
        else -> accent
    }

    val fg = when {
        downloading -> if (dark) Color.White else Color(0xFF1C3E68)
        downloaded -> if (dark) Color(0xFFA8F0D0) else Color(0xFF0F6E56)
        else -> Color.White
    }

    Row(
        modifier = Modifier
            .background(bg, RoundedCornerShape(999.dp))
            .clickable(enabled = !downloading, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when {
            downloading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = fg
                )
                Text(
                    text = "$progress%",
                    color = fg,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            downloaded -> {
                Text(
                    text = "Install",
                    color = fg,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            else -> {
                Text(
                    text = "Download",
                    color = fg,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun CompactVersionRow(
    label: String,
    value: String,
    labelColor: Color,
    valueColor: Color
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = labelColor
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}


@Composable
private fun UpdateBlockCard(item: UpdateBlock) {
    var expanded by remember { mutableStateOf(false) }
    val dark = isSystemInDarkTheme()

    val cardBg = if (dark) Color(0xFF1B1828) else MaterialTheme.colorScheme.surface
    val cardBorder = if (dark) {
        Color.White.copy(alpha = 0.08f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    }

    val expandedBg = if (dark) {
        Color(0xFF241F36)
    } else {
        Color(0xFFF3F6FA)
    }

    val titleColor = if (dark) Color.White else MaterialTheme.colorScheme.onSurface
    val summaryColor = if (dark) {
        Color.White.copy(alpha = 0.74f)
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f)
    }

    val bulletColor = if (dark) {
        Color.White.copy(alpha = 0.68f)
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
    }

    val arrowTint = if (dark) {
        Color.White.copy(alpha = 0.52f)
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
    }

    val arrowBg = if (dark) {
        Color.White.copy(alpha = 0.08f)
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    }

    val bulletDot = if (dark) {
        Color.White.copy(alpha = 0.26f)
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = androidx.compose.foundation.BorderStroke(
            width = 0.5.dp,
            color = cardBorder
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                UpdateTagPill(title = item.title)

                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = titleColor,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    imageVector = if (expanded) {
                        Icons.Outlined.KeyboardArrowUp
                    } else {
                        Icons.Outlined.KeyboardArrowDown
                    },
                    contentDescription = null,
                    tint = arrowTint,
                    modifier = Modifier
                        .size(20.dp)
                        .background(arrowBg, CircleShape)
                        .padding(3.dp)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(expandedBg)
                        .padding(start = 16.dp, end = 16.dp, bottom = 14.dp, top = 12.dp)
                ) {
                    if (item.summary.isNotBlank()) {
                        Text(
                            text = item.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = summaryColor,
                            lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                        )
                    }

                    if (item.bullets.isNotEmpty()) {
                        Spacer(Modifier.height(if (item.summary.isNotBlank()) 10.dp else 0.dp))
                        item.bullets.forEach { bullet ->
                            Row(
                                modifier = Modifier.padding(vertical = 2.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 5.dp)
                                        .size(5.dp)
                                        .background(bulletDot, CircleShape)
                                )
                                Text(
                                    text = bullet,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = bulletColor,
                                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdateTagPill(title: String) {
    val dark = isSystemInDarkTheme()

    val (label, bg, fg) = when {
        title.contains("fix", ignoreCase = true) ||
                title.contains("bug", ignoreCase = true) -> {
            if (dark) {
                Triple("Fixed", Color(0xFF3A2024), Color(0xFFFFB4BE))
            } else {
                Triple("Fixed", Color(0xFFFCEBEB), Color(0xFFA32D2D))
            }
        }

        title.contains("new", ignoreCase = true) ||
                title.contains("add", ignoreCase = true) -> {
            if (dark) {
                Triple("New", Color(0xFF17352D), Color(0xFFA8F0D0))
            } else {
                Triple("New", Color(0xFFE1F5EE), Color(0xFF0F6E56))
            }
        }

        title.contains("perf", ignoreCase = true) ||
                title.contains("speed", ignoreCase = true) ||
                title.contains("fast", ignoreCase = true) -> {
            if (dark) {
                Triple("Perf", Color(0xFF3A2E17), Color(0xFFFFDEA6))
            } else {
                Triple("Perf", Color(0xFFFAEEDA), Color(0xFF854F0B))
            }
        }

        else -> {
            if (dark) {
                Triple("Improved", Color(0xFF1D2F46), Color(0xFFB3D8FF))
            } else {
                Triple("Improved", Color(0xFFE6F1FB), Color(0xFF185FA5))
            }
        }
    }

    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = fg,
        modifier = Modifier
            .background(bg, RoundedCornerShape(999.dp))
            .padding(horizontal = 9.dp, vertical = 3.dp)
    )
}


@Composable
private fun TopBar(
    onBack: () -> Unit,
    onSoftwareInfo: () -> Unit,
    onLastUpdate: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val dark = isSystemInDarkTheme()

    val iconTint = if (dark) Color.White else Color(0xFF111111)
    val menuBg = if (dark) Color(0xFF1B1828) else Color.White
    val menuBorder = if (dark) {
        Color.White.copy(alpha = 0.08f)
    } else {
        Color(0xFFDCE2EA)
    }
    val menuText = if (dark) Color.White else Color(0xFF111111)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 10.dp, vertical = 2.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBackIos,
                contentDescription = "Back",
                tint = iconTint
            )
        }

        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
            IconButton(onClick = { expanded = true }) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = "More",
                    tint = iconTint
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                shape = RoundedCornerShape(28.dp),
                containerColor = menuBg,
                tonalElevation = 8.dp,
                shadowElevation = 12.dp,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    menuBorder
                )
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Software information",
                            color = menuText
                        )
                    },
                    onClick = {
                        expanded = false
                        onSoftwareInfo()
                    }
                )

                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Last update",
                            color = menuText
                        )
                    },
                    onClick = {
                        expanded = false
                        onLastUpdate()
                    }
                )
            }
        }
    }
}

@Composable
private fun UpdateGlow(modifier: Modifier = Modifier) {
    val zoom = remember { Animatable(0f) }
    val dark = isSystemInDarkTheme()
    val phase by rememberInfiniteTransition(label = "updateGlowMotion").animateFloat(
        initialValue = 0f,
        targetValue = 6.2831855f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 28_000, easing = LinearEasing)
        ),
        label = "updateGlowPhase"
    )

    LaunchedEffect(Unit) {
        zoom.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 1_450,
                easing = FastOutSlowInEasing
            )
        )
    }

    Canvas(modifier = modifier) {
        val easedZoom = zoom.value.coerceIn(0f, 1f)
        if (easedZoom <= 0f) return@Canvas

        val glowRadius = size.minDimension * 0.34f * easedZoom
        val circleCenter = center.copy(y = center.y - 24.dp.toPx())

        fun paletteColor(offset: Float): Color {
            val palette = listOf(
                Color(0xFF3AAEEB),
                Color(0xFF77D4B2),
                Color(0xFFF2C94C),
                Color(0xFFD86AF7)
            )
            val progress = ((phase / 6.2831855f) + offset).let { it - it.toInt() }
            val segment = progress * palette.size
            val index = segment.toInt().coerceIn(0, palette.lastIndex)
            val nextIndex = (index + 1) % palette.size
            val rawT = segment - index
            val smoothT = rawT * rawT * (3f - 2f * rawT)
            return lerp(palette[index], palette[nextIndex], smoothT)
        }

        fun drawSoftLight(
            color: Color,
            offset: Offset,
            radiusScale: Float,
            alpha: Float
        ) {
            val lightCenter = Offset(
                circleCenter.x + offset.x * easedZoom,
                circleCenter.y + offset.y * easedZoom
            )
            val lightRadius = glowRadius * radiusScale
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        color.copy(alpha = alpha),
                        color.copy(alpha = alpha * 0.42f),
                        color.copy(alpha = alpha * 0.12f),
                        Color.Transparent
                    ),
                    center = lightCenter,
                    radius = lightRadius
                ),
                radius = lightRadius,
                center = lightCenter
            )
        }

        drawSoftLight(
            color = paletteColor(0.00f),
            offset = Offset(38.dp.toPx(), -22.dp.toPx()),
            radiusScale = 0.92f,
            alpha = 0.17f
        )
        drawSoftLight(
            color = paletteColor(0.25f),
            offset = Offset(-16.dp.toPx(), -18.dp.toPx()),
            radiusScale = 0.82f,
            alpha = 0.12f
        )
        drawSoftLight(
            color = paletteColor(0.50f),
            offset = Offset(-48.dp.toPx(), 56.dp.toPx()),
            radiusScale = 0.94f,
            alpha = 0.13f
        )
        drawSoftLight(
            color = paletteColor(0.75f),
            offset = Offset(4.dp.toPx(), 42.dp.toPx()),
            radiusScale = 0.86f,
            alpha = 0.12f
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (dark) 0.018f else 0.030f),
                    Color.Transparent
                ),
                center = circleCenter,
                radius = glowRadius * 0.48f
            ),
            radius = glowRadius * 0.48f,
            center = circleCenter
        )
    }
}

@Composable
private fun SoftwareInfoSheet(
    softwareInfo: SoftwareInfoUi,
    onDismiss: () -> Unit
) {
    val dark = isSystemInDarkTheme()
    val titleColor = if (dark) Color.White else Color(0xFF111111)
    val buttonBg = Color(0xFF77D4B2)
    val buttonText = Color.Black

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .navigationBarsPadding()
    ) {
        Text(
            text = "Software information",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = titleColor
        )

        Spacer(Modifier.height(18.dp))

        InfoRow("App", softwareInfo.appName)
        InfoRow("Version code", softwareInfo.versionCode.toString())
        InfoRow("Version name", softwareInfo.versionName)
        InfoRow("Android version", softwareInfo.androidVersion)
        InfoRow("Device model", softwareInfo.deviceModel)
        InfoRow("Brand", softwareInfo.deviceBrand)
        InfoRow("Architecture", softwareInfo.architecture)

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = buttonBg,
                contentColor = buttonText
            )
        ) {
            Text("Close")
        }

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun LastUpdateSheet(
    lastCheckedText: String,
    latestUpdate: LatestUpdateUi,
    onDismiss: () -> Unit
) {
    val dark = isSystemInDarkTheme()
    val titleColor = if (dark) Color.White else Color(0xFF111111)
    val labelColor = if (dark) {
        Color.White.copy(alpha = 0.60f)
    } else {
        Color(0xFF667085)
    }
    val bodyColor = if (dark) {
        Color.White.copy(alpha = 0.88f)
    } else {
        Color(0xFF111111)
    }
    val secondaryBodyColor = if (dark) {
        Color.White.copy(alpha = 0.72f)
    } else {
        Color(0xFF4B5563)
    }
    val mutedCard = if (dark) {
        Color.White.copy(alpha = 0.06f)
    } else {
        Color(0xFFF3F6FA)
    }
    val border = if (dark) {
        Color.White.copy(alpha = 0.08f)
    } else {
        Color(0xFFDCE2EA)
    }
    val remote = latestUpdate.remote

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Latest update",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor
                )

                Spacer(Modifier.height(3.dp))

                Text(
                    text = if (latestUpdate.loading) {
                        "Refreshing changelog..."
                    } else {
                        "Newest changelog from the update feed"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryBodyColor
                )
            }

            if (latestUpdate.loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.2.dp,
                    color = Color(0xFF77D4B2)
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = mutedCard,
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, border)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    text = if (remote != null) {
                        "Version ${remote.versionCode}"
                    } else {
                        "No changelog loaded yet"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = bodyColor
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = remote?.versionName?.takeIf { it.isNotBlank() }
                        ?: "Tap Last update to load the latest release notes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = secondaryBodyColor
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        InfoRow(
            label = "Last refreshed",
            value = latestUpdate.loadedAtText.ifBlank { lastCheckedText }
        )

        latestUpdate.errorMessage?.let { message ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (dark) Color(0xFF3A2024) else Color(0xFFFCEBEB),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (dark) Color(0xFFFFB4BE).copy(alpha = 0.22f) else Color(0xFFFFB4BE)
                )
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                    color = if (dark) Color(0xFFFFD9DE) else Color(0xFF8B1E1E),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.height(12.dp))
        }

        if (remote != null && remote.updates.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 2.dp)
            ) {
                items(remote.updates) { item ->
                    LastUpdateChangelogItem(item = item)
                }
            }
        } else {
            Text(
                text = if (latestUpdate.loading) {
                    "Loading the latest release notes..."
                } else {
                    "No release notes are available yet."
                },
                color = secondaryBodyColor,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF77D4B2),
                contentColor = Color.Black
            )
        ) {
            Text("Close")
        }

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun LastUpdateChangelogItem(item: UpdateBlock) {
    val dark = isSystemInDarkTheme()
    val container = if (dark) Color(0xFF241F36) else Color(0xFFF8FAFC)
    val border = if (dark) Color.White.copy(alpha = 0.08f) else Color(0xFFDCE2EA)
    val titleColor = if (dark) Color.White else Color(0xFF111111)
    val bodyColor = if (dark) Color.White.copy(alpha = 0.74f) else Color(0xFF4B5563)
    val dotColor = if (dark) Color.White.copy(alpha = 0.30f) else Color(0xFF6B7280).copy(alpha = 0.35f)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = container,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, border)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                UpdateTagPill(title = item.title)
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor,
                    modifier = Modifier.weight(1f)
                )
            }

            if (item.summary.isNotBlank()) {
                Spacer(Modifier.height(9.dp))
                Text(
                    text = item.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = bodyColor,
                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                )
            }

            if (item.bullets.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                item.bullets.forEach { bullet ->
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 5.dp)
                                .size(5.dp)
                                .background(dotColor, CircleShape)
                        )
                        Text(
                            text = bullet,
                            style = MaterialTheme.typography.bodySmall,
                            color = bodyColor,
                            lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                        )
                    }
                }
            }
        }
    }

}

@Composable
private fun InfoRow(label: String, value: String) {
    val dark = isSystemInDarkTheme()

    val labelColor = if (dark) {
        Color.White.copy(alpha = 0.60f)
    } else {
        Color(0xFF667085)
    }

    val valueColor = if (dark) {
        Color.White.copy(alpha = 0.90f)
    } else {
        Color(0xFF111111)
    }

    val dividerColor = if (dark) {
        Color.White.copy(alpha = 0.08f)
    } else {
        Color(0xFFDCE2EA)
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = labelColor
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = valueColor
        )

        Spacer(Modifier.height(14.dp))

        HorizontalDivider(color = dividerColor)

        Spacer(Modifier.height(14.dp))
    }
}


@Composable
private fun ErrorScreen(
    message: String,
    onBack: () -> Unit,
    onSoftwareInfo: () -> Unit,
    onLastUpdate: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopBar(
            onBack = onBack,
            onSoftwareInfo = onSoftwareInfo,
            onLastUpdate = onLastUpdate
        )

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Update Error",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = message,
                textAlign = TextAlign.Center
            )
        }
    }
}
