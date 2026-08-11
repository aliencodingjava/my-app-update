package com.flights.studio

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.CalendarContract
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.compose.ui.graphics.lerp as colorLerp
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import kotlin.time.Duration.Companion.milliseconds
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ImportContacts
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.flights.studio.ui.AppLanguageManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.capsule.ContinuousCapsule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale
import java.util.Calendar
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh
import kotlin.random.Random

/**
 * The main entry point of the application, responsible for orchestrating the primary UI layers
 * including Home, Briefing, Notes, and Settings using Jetpack Compose and View-based components.
 *
 * This activity handles:
 * - Navigation between high-level application pages.
 * - Integration with Supabase for notes and folder synchronization.
 * - Management of contacts through [AllContactsFragment].
 * - Coordination of reminders and local notifications.
 * - Implementation of a glass-morphic UI design language using [FlightsBackdropScaffold].
 */
@Suppress("DEPRECATION")
class MainActivity : FragmentActivity() {

    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var notesAdapter: NotesAdapter

    private val allNotes = mutableListOf<String>()
    private val notesText = mutableStateListOf<String>()
    private val noteRows = mutableStateListOf<NoteRow>()
    private val noteFolderRows = mutableStateListOf<NoteFolderUi>()
    private var currentNotesFolderId by mutableStateOf<String?>(null)
    private var notesFolderSelectionActive by mutableStateOf(false)
    private val notesCount = mutableIntStateOf(0)
    private var notesSyncStatus by mutableStateOf(NotesSyncUiStatus.Synced)
    private val uidToContent = mutableMapOf<String, String>()
    private val contentToUid = mutableMapOf<String, String>()
    private val sharedPreferences by lazy { getSharedPreferences("notes_prefs", MODE_PRIVATE) }
    private val uidPrefs by lazy { getSharedPreferences("notes_uids", MODE_PRIVATE) }
    private val notesHttpClient by lazy { OkHttpClient() }
    private var isMultiSelectMode = false
    private var pendingReminderNote: String? = null
    private var pendingReminderInfo: ReminderInfo? = null
    private var openReminderSheet: (String) -> Unit = { note -> pendingReminderNote = note }
    private var openReminderDetails: (ReminderInfo) -> Unit = { info -> pendingReminderInfo = info }

    private var allContactsFragment: AllContactsFragment? = null
    private var contactsContainerView: FrameLayout? = null
    private val contactsContainerViewState = mutableStateOf<android.view.View?>(null)
    private var openRecentContactMenu: ((
        contact: AllContact,
        onRemove: () -> Unit
    ) -> Unit)? = null
    private var openRequestedMainPage: ((Int) -> Unit)? = null
    private var currentMainPageForScreenAwake = PAGE_HOME

    override fun attachBaseContext(newBase: Context) {
        val tag = AppLanguageManager.currentLanguageTag(newBase)
        super.attachBaseContext(LocaleUtils.wrap(newBase, tag))
    }
    private val contactsChromeCount = mutableIntStateOf(0)
    private val contactsSearchQuery = mutableStateOf("")
    private val contactsFloatingSearchVisible = mutableStateOf(false)
    private val contactsFloatingSearchActive = mutableStateOf(false)
    private val contactsSelectionCount = mutableIntStateOf(0)
    private val contactsAlphabeticalMode = mutableStateOf(false)
    private val contactsAddFabVisible = mutableStateOf(true)
    private val settingsSearchQuery = mutableStateOf("")
    private val settingsSearchSheetVisible = mutableStateOf(false)

    fun showRecentContactMenu(contact: AllContact, onRemove: () -> Unit) {
        openRecentContactMenu?.invoke(contact, onRemove)
    }

    companion object {
        private const val TAG_MAIN = "MainActivity"
        const val EXTRA_START_PAGE = "extra_start_page"
        const val EXTRA_DEV_BYPASS_LOGIN = "extra_dev_bypass_login"
        const val EXTRA_DEV_BYPASS_PAGE = "extra_dev_bypass_page"
        const val PAGE_HOME = 0
        const val PAGE_BRIEFING = 1
        const val PAGE_CONTACTS = -1
        const val PAGE_NOTES = 2
        const val PAGE_SETTINGS = 3
        private const val PAGE_PROFILE = -1
        private const val DEV_BYPASS_SCHEME = "flightsstudio-debug"
        private const val DEV_BYPASS_HOST = "bypass-login"
        private const val MAIN_WELCOME_PREFS = "main_welcome_prefs"
        private const val MAIN_WELCOME_SEEN_VERSION = "seen_version"
        private const val MAIN_WELCOME_VERSION = 1
        private const val EMERGENCY_NOTICE_PREFS = "emergency_notice_prefs"
        private const val DISMISSED_EMERGENCY_KEY = "dismissed_emergency_key"
        private const val DEBUG_FORCE_BRIEFING_RAIN = false
        private const val DEBUG_FORCE_BRIEFING_THUNDER = false
        private const val DEBUG_FORCE_BRIEFING_SUN = false
        const val APP_SHARE_URL = "https://tinyurl.com/8nhpbjap"
    }

    private val addNoteLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult

        val newNote = result.data?.getStringExtra("NEW_NOTE").orEmpty()
        if (newNote.isBlank()) return@registerForActivityResult

        val title = result.data?.getStringExtra("NEW_NOTE_TITLE").orEmpty()
        val imageUris = result.data
            ?.getStringArrayListExtra("NEW_NOTE_IMAGES")
            ?.mapNotNull { runCatching { it.toUri() }.getOrNull() }
            .orEmpty()
        val voiceUris = result.data?.getStringArrayListExtra("NEW_NOTE_VOICE_URIS").orEmpty()
        val voiceDurations = result.data?.getLongArrayExtra("NEW_NOTE_VOICE_DURATIONS") ?: longArrayOf()
        val voiceCreatedAt = result.data?.getLongArrayExtra("NEW_NOTE_VOICE_CREATED_AT") ?: longArrayOf()
        val voiceItems = voiceUris.mapIndexed { index, uri ->
            NoteVoiceItem(
                uri = uri,
                durationMs = voiceDurations.getOrNull(index) ?: 0L,
                createdAtMs = voiceCreatedAt.getOrNull(index) ?: System.currentTimeMillis()
            )
        }
        val fileUris = result.data?.getStringArrayListExtra("NEW_NOTE_FILE_URIS").orEmpty()
        val fileNames = result.data?.getStringArrayListExtra("NEW_NOTE_FILE_NAMES").orEmpty()
        val fileMimes = result.data?.getStringArrayListExtra("NEW_NOTE_FILE_MIMES").orEmpty()
        val fileSizes = result.data?.getLongArrayExtra("NEW_NOTE_FILE_SIZES") ?: longArrayOf()
        val fileItems = fileUris.mapIndexed { index, uri ->
            NoteAttachmentItem(
                uri = uri,
                name = fileNames.getOrNull(index).orEmpty().ifBlank { "Attachment ${index + 1}" },
                mime = fileMimes.getOrNull(index)?.takeIf { it.isNotBlank() },
                sizeBytes = fileSizes.getOrNull(index) ?: 0L
            )
        }

        allNotes.add(newNote)
        val newNoteUid = ensureLocalUid(newNote)
        NoteCreatedAtStore.ensure(this, newNoteUid)
        assignMainNoteToCurrentFolder(newNote)
        saveNoteMediaForKeys(
            context = this,
            note = newNote,
            noteKey = newNoteUid,
            imageUris = imageUris.takeIf { it.isNotEmpty() },
            attachments = fileItems.takeIf { it.isNotEmpty() },
            voiceItems = voiceItems.takeIf { it.isNotEmpty() }
        )
        if (title.isNotBlank()) notesAdapter.setUserTitle(newNote, title)

        val wantsReminder = result.data?.getBooleanExtra("NEW_NOTE_WANTS_REMINDER", false) == true
        if (wantsReminder) {
            getSharedPreferences("reminder_flags", MODE_PRIVATE).edit {
                putBoolean(newNote.hashCode().toString(), true)
            }
            notesAdapter.preloadReminderFlags(this)
        }

        refreshNotesDisplay()
        saveNotes()
        queuePendingAdd(newNote)
        syncNoteWithAttachmentsToSupabase(
            content = newNote,
            titleOverride = title,
            imageUrisOverride = imageUris,
            fileItemsOverride = fileItems,
            voiceItemsOverride = voiceItems,
            hasReminderOverride = wantsReminder
        )
        if (wantsReminder) {
            openReminderSheet(newNote)
        }
    }

    private val editNoteLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) handleNoteEditResult(result.data)
    }

    private val viewNoteLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) handleNoteEditResult(result.data)
    }

    /**
     * Initializes the activity, sets up window insets for edge-to-edge display,
     * initializes analytics, and sets the main Compose content.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     * previously being shut down then this Bundle contains the data it most
     * recently supplied in [onSaveInstanceState].
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG_MAIN, "onCreate() START, savedInstanceState=$savedInstanceState")
        val openLogin = intent.getBooleanExtra(EXTRA_OPEN_LOGIN, false)

        if (openLogin) {
            startActivity(Intent(this, ProfileDetailsComposeActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            })
            finish()
            return
        }

        applyDebugLoginBypassIfRequested()?.let { page ->
            if (page == PAGE_PROFILE) {
                startActivity(Intent(this, ProfileDetailsComposeActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                })
                finish()
                return
            }
            intent.putExtra(EXTRA_START_PAGE, page)
        }
        currentMainPageForScreenAwake = resolveInitialMainPage(intent)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING or
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        )
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.statusBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        Log.d(TAG_MAIN, "FirebaseAnalytics initialized")

        setupNotes()
        loadUidMaps()
        NotesCacheManager.preloadResources(this)
        loadNotesHeadless()

        setContent {
            Log.d(TAG_MAIN, "setContent: root composition ENTER")

            val context = LocalContext.current
            val appThemePreset = AppThemeStore.rememberPreset(context)
            FlightsTheme(appThemePreset = appThemePreset) {
                Log.d(TAG_MAIN, "FlightsTheme: composition ENTER")

                val welcomePrefs = remember {
                    context.getSharedPreferences(MAIN_WELCOME_PREFS, MODE_PRIVATE)
                }
                val emergencyPrefs = remember {
                    context.getSharedPreferences(EMERGENCY_NOTICE_PREFS, MODE_PRIVATE)
                }
                var showMainWelcome by remember {
                    mutableStateOf(
                        welcomePrefs.getInt(MAIN_WELCOME_SEEN_VERSION, 0) < MAIN_WELCOME_VERSION
                    )
                }
                var showMenuSheet by remember { mutableStateOf(false) }
                var showContactsInfoSheet by remember { mutableStateOf(false) }
                var pendingContactsInfoSheet by remember { mutableStateOf(false) }
                var recentMenuContact by remember { mutableStateOf<AllContact?>(null) }
                var recentMenuRemove by remember { mutableStateOf<(() -> Unit)?>(null) }
                var homeCameraExpanded by remember { mutableStateOf(false) }
                var homeCameraGestureActive by remember { mutableStateOf(false) }
                var settingsFeedbackRequest by remember { mutableIntStateOf(0) }
                var reminderNote by remember { mutableStateOf<String?>(null) }
                var reminderTimeNote by remember { mutableStateOf<String?>(null) }
                var reminderDetails by remember { mutableStateOf<ReminderInfo?>(null) }
                var emergencyMessage by remember { mutableStateOf<EmergencyMessage?>(null) }
                var dismissedEmergencyKey by remember {
                    mutableStateOf(emergencyPrefs.getString(DISMISSED_EMERGENCY_KEY, null))
                }
                var selectedMainPage by rememberSaveable { mutableIntStateOf(resolveInitialMainPage(intent)) }
                val scope = rememberCoroutineScope()
                var lastNonBriefingPage by remember {
                    mutableIntStateOf(
                        selectedMainPage.takeUnless { it == PAGE_BRIEFING } ?: PAGE_HOME
                    )
                }
                LaunchedEffect(Unit) {
                    openReminderSheet = { note -> reminderNote = note }
                    openReminderDetails = { info -> reminderDetails = info }
                    pendingReminderNote?.let { note ->
                        reminderNote = note
                        pendingReminderNote = null
                    }
                    pendingReminderInfo?.let { info ->
                        reminderDetails = info
                        pendingReminderInfo = null
                    }
                    openRecentContactMenu = { contact, onRemove ->
                        showMenuSheet = false
                        recentMenuContact = contact
                        recentMenuRemove = onRemove
                        scope.launch {
                            delay(120.milliseconds)
                            showMenuSheet = true
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    emergencyMessage = EmergencyMessageRepository.fetch()
                }

                LaunchedEffect(pendingContactsInfoSheet) {
                    if (pendingContactsInfoSheet) {
                        delay(140.milliseconds)
                        pendingContactsInfoSheet = false
                        showContactsInfoSheet = true
                    }
                }

                fun actuallyExitApp() {
                    Log.d(TAG_MAIN, "actuallyExitApp() -> finishAffinity + finishAndRemoveTask")
                    finishAffinity()
                    finishAndRemoveTask()
                }

                fun openMenuSheet() {
                    Log.d(TAG_MAIN, "openMenuSheet() called")
                    showMenuSheet = true
                }

                fun closeMenuSheet() {
                    Log.d(TAG_MAIN, "closeMenuSheet() called")
                    showMenuSheet = false
                    recentMenuContact = null
                    recentMenuRemove = null
                }

                fun dismissReminder() {
                    reminderNote = null
                }

                fun openTimerForCurrentReminder() {
                    reminderTimeNote = reminderNote
                    reminderNote = null
                }

                fun openCalendarForCurrentReminder() {
                    reminderNote?.let { openMaterialDateTimePickerDialog(it) }
                    dismissReminder()
                }

                fun goToPage(page: Int) {
                    closeMenuSheet()
                    val targetPage = page.coerceIn(PAGE_HOME, PAGE_SETTINGS)
                    val currentPage = selectedMainPage
                    if (targetPage == currentPage) {
                        return
                    }
                    if (targetPage != PAGE_HOME) {
                        homeCameraExpanded = false
                        homeCameraGestureActive = false
                    }
                    selectedMainPage = targetPage
                }
                openRequestedMainPage = { page -> goToPage(page) }

                fun openBriefingPage() {
                    closeMenuSheet()
                    val currentPage = selectedMainPage
                    if (currentPage == PAGE_BRIEFING) {
                        return
                    }
                    homeCameraExpanded = false
                    homeCameraGestureActive = false
                    selectedMainPage = PAGE_BRIEFING
                }

                LaunchedEffect(selectedMainPage) {
                    val page = selectedMainPage
                    currentMainPageForScreenAwake = page
                    applyMainPageKeepScreenOn(page)
                    if (page != PAGE_BRIEFING) {
                        lastNonBriefingPage = page
                    }
                }

                fun openQrScreen() {
                    startActivity(
                        Intent(context, QRCodeComposeActivity::class.java),
                        ActivityOptionsCompat.makeSceneTransitionAnimation(this@MainActivity).toBundle()
                    )
                }

                fun openProfileScreen() {
                    startActivity(
                        Intent(context, ProfileDetailsComposeActivity::class.java),
                        ActivityOptionsCompat.makeSceneTransitionAnimation(this@MainActivity).toBundle()
                    )
                }

                fun openNotesSettingsScreen() {
                    startActivity(NotesSettingsComposeActivity.newIntent(this@MainActivity))
                    overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation)
                }

                fun openLiveCamerasScreen() {
                    val ts = System.currentTimeMillis()
                    val cards = listOf(
                        CameraCard(
                            "Curb",
                            "https://www.jacksonholeairport.com/wp-content/uploads/webcams/parking-curb.jpg?v=$ts"
                        ),
                        CameraCard(
                            "North",
                            "https://www.jacksonholeairport.com/wp-content/uploads/webcams/parking-north.jpg?v=$ts"
                        ),
                        CameraCard(
                            "South",
                            "https://www.jacksonholeairport.com/wp-content/uploads/webcams/parking-south.jpg?v=$ts"
                        )
                    )
                    startActivity(LiveCamerasActivity.intent(context, cards))
                    overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation)
                }

                BackHandler {
                    Log.d(TAG_MAIN, "BackHandler: showMenuSheet=$showMenuSheet")
                    when {
                        showMainWelcome -> Unit
                        reminderDetails != null -> reminderDetails = null
                        reminderTimeNote != null -> reminderTimeNote = null
                        showMenuSheet -> closeMenuSheet()
                        contactsSearchQuery.value.isNotEmpty() -> {
                            updateContactsSearch("")
                            updateContactsFloatingSearchVisible(false)
                            hideContactsKeyboard()
                        }
                        selectedMainPage != PAGE_HOME -> goToPage(PAGE_HOME)
                        else -> actuallyExitApp()
                    }
                }

                FlightsBackdropScaffold { globalBackdrop, _ ->
                    val mainPageBackdrop = rememberLayerBackdrop()
                    val mainMenuBackdrop = rememberLayerBackdrop()
                    val isDark = isSystemInDarkTheme()
                    val selectedTab = when (selectedMainPage) {
                        PAGE_BRIEFING -> PrimaryTabDestination.Briefing
                        PAGE_NOTES -> PrimaryTabDestination.Notes
                        PAGE_SETTINGS -> PrimaryTabDestination.Settings
                        else -> PrimaryTabDestination.Home
                    }
                    var settingsModalVisible by remember { mutableStateOf(false) }
                    val imeDensity = LocalDensity.current
                    val settingsKeyboardOpen = WindowInsets.ime.getBottom(imeDensity) > 0

                    Box(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .layerBackdrop(mainMenuBackdrop)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .layerBackdrop(mainPageBackdrop)
                            ) {
                                ProfileBackdropImageLayer(
                                    modifier = Modifier.fillMaxSize(),
                                    lightRes = R.drawable.light_grid_pattern,
                                    darkRes = R.drawable.dark_grid_pattern,
                                    imageAlpha = if (isDark) 1f else 0.8f,
                                    scrimDark = 0f,
                                    scrimLight = 0f
                                )

                                MainPager(
                                    currentBackdrop = globalBackdrop,
                                    onOpenHome = { goToPage(PAGE_HOME) },
                                    onOpenContacts = ::openBriefingPage,
                                    onOpenNotes = { goToPage(PAGE_NOTES) },
                                    onOpenLiveCameras = ::openLiveCamerasScreen,
                                    onOpenAddNote = {
                                        addNoteLauncher.launch(
                                            AddNoteComposeActivity.newIntent(this@MainActivity),
                                            ActivityOptionsCompat.makeSceneTransitionAnimation(this@MainActivity)
                                        )
                                    },
                                    onHomeCameraExpandedChange = { expanded ->
                                        homeCameraExpanded = expanded && selectedMainPage == PAGE_HOME
                                    },
                                    onHomeCameraGestureActiveChange = { active ->
                                        homeCameraGestureActive = active && selectedMainPage == PAGE_HOME
                                    },
                                    actuallyExitApp = ::actuallyExitApp,
                                    triggerRefreshNow = { newUrl ->
                                        Log.d(TAG_MAIN, "triggerRefreshNow(newUrl=$newUrl)")
                                    },
                                    currentPage = selectedMainPage,
                                    settingsFeedbackRequestToken = settingsFeedbackRequest
                                ) { settingsModalVisible = it }
                            }

                            if (selectedMainPage == PAGE_BRIEFING) {
                                BriefingGlassTopAppBar(
                                    backdrop = mainPageBackdrop,
                                    modifier = Modifier.align(Alignment.TopCenter)
                                )
                            }

                            if (
                                !(selectedMainPage == PAGE_HOME && homeCameraExpanded) &&
                                !(selectedMainPage == PAGE_SETTINGS && settingsModalVisible) &&
                                !(selectedMainPage == PAGE_NOTES && notesFolderSelectionActive)
                            ) {
                                PrimaryBottomChrome(
                                    selectedTab = selectedTab,
                                    backdrop = mainPageBackdrop,
                                    menuVisible = false,
                                    menuActions = emptyList(),
                                    onMenuDismiss = ::closeMenuSheet,
                                    onOpenHome = { goToPage(PAGE_HOME) },
                                    onOpenContacts = ::openBriefingPage,
                                    onOpenNotes = { goToPage(PAGE_NOTES) },
                                    onOpenSettings = { goToPage(PAGE_SETTINGS) },
                                    onOpenMenu = ::openMenuSheet,
                                    showMenu = false,
                                    contentView = null,
                                    menuIcon = Icons.Filled.Menu
                                )
                            }

                            val showEmbeddedContactsChrome = false
                            if (showEmbeddedContactsChrome && selectedMainPage == PAGE_BRIEFING) {
                                ContactsDefaultTopBar(
                                    visible = contactsSelectionCount.intValue == 0,
                                    contentView = contactsContainerViewState.value,
                                    onOpenSearch = {
                                        allContactsFragment?.prepareContactsSearchOpen()
                                        contactsFloatingSearchActive.value = true
                                        contactsFloatingSearchVisible.value = true
                                    },
                                    onImportContacts = { allContactsFragment?.showImportConfirmationDialog() },
                                    onOpenContactsInfo = { pendingContactsInfoSheet = true },
                                    alphabeticalMode = contactsAlphabeticalMode.value,
                                    showSearchAction = contactsFloatingSearchVisible.value &&
                                            !contactsFloatingSearchActive.value &&
                                            contactsSearchQuery.value.isBlank(),
                                    showSortAction = !contactsFloatingSearchActive.value &&
                                            contactsSearchQuery.value.isBlank(),
                                    onToggleAlphabetical = { allContactsFragment?.toggleContactsAlphabeticalSort() }
                                )
                                ContactsSelectionTopBar(
                                    selectionCount = contactsSelectionCount.intValue,
                                    contactCount = contactsChromeCount.intValue,
                                    contentView = contactsContainerViewState.value,
                                    onClearSelection = ::clearContactsSelection,
                                    onSelectAll = ::selectAllVisibleContacts,
                                    onDeleteSelected = ::deleteSelectedContacts
                                )
                                ContactsFloatingSearchOverlay(mainMenuBackdrop)
                                ContactsFloatingAddButton(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(end = 16.dp, bottom = 78.dp)
                                        .navigationBarsPadding(),
                                    backdrop = mainMenuBackdrop,
                                    contentView = contactsContainerViewState.value,
                                    visible = contactsAddFabVisible.value &&
                                            !contactsFloatingSearchActive.value &&
                                            contactsSearchQuery.value.isBlank() &&
                                            contactsSelectionCount.intValue == 0,
                                    onClick = { allContactsFragment?.showAddContactBottomSheet() }
                                )
                                ContactsInfoSheet(
                                    visible = showContactsInfoSheet,
                                    modifier = Modifier.align(Alignment.BottomCenter),
                                    contactCount = contactsChromeCount.intValue,
                                    backdrop = mainMenuBackdrop,
                                    contentView = contactsContainerViewState.value,
                                    onDismiss = { showContactsInfoSheet = false }
                                )
                            }
                        }

                        if (
                            !(selectedMainPage == PAGE_HOME && homeCameraExpanded) &&
                            !(selectedMainPage == PAGE_NOTES && notesFolderSelectionActive)
                        ) {
                            PrimaryBottomChrome(
                                selectedTab = selectedTab,
                                backdrop = mainMenuBackdrop,
                                menuVisible = showMenuSheet,
                                menuActions = recentMenuContact?.let { contact ->
                                    val removeRecent = recentMenuRemove
                                    listOf(
                                        PrimaryMenuAction(
                                            label = "View profile",
                                            iconRes = R.drawable.account_circle_24dp_ffffff_fill1_profile,
                                            onClick = {
                                                allContactsFragment?.openContactDetails(contact)
                                            }
                                        ),
                                        PrimaryMenuAction(
                                            label = "Remove from Recent",
                                            iconRes = R.drawable.person_remove_24dp_ffffff_fill1_wght400_grad0_opsz24,
                                            onClick = {
                                                removeRecent?.invoke()
                                            }
                                        )
                                    )
                                } ?: run {
                                    when (selectedMainPage) {
                                        PAGE_NOTES -> listOf(
                                            PrimaryMenuAction(
                                                label = "Search",
                                                iconRes = R.drawable.manage_search_24dp_ffffff_fill0_wght400_grad0_opsz24,
                                                onClick = {
                                                    openNotesSearchSheet {}
                                                }
                                            ),
                                            PrimaryMenuAction(
                                                label = "Settings",
                                                iconRes = R.drawable.ic_oui_settings,
                                                onClick = ::openNotesSettingsScreen
                                            ),
                                            PrimaryMenuAction(
                                                label = "Profile",
                                                iconRes = R.drawable.account_circle_24dp_ffffff_fill1_profile,
                                                onClick = ::openProfileScreen,
                                                useProfileAvatar = true
                                            )
                                        )
                                        PAGE_SETTINGS -> listOf(
                                            PrimaryMenuAction(
                                                label = "Profile",
                                                iconRes = R.drawable.account_circle_24dp_ffffff_fill1_profile,
                                                onClick = ::openProfileScreen,
                                                useProfileAvatar = true
                                            ),
                                            PrimaryMenuAction(
                                                label = "QR Code",
                                                iconRes = R.drawable.ic_oui_qr_code,
                                                onClick = ::openQrScreen
                                            ),
                                            PrimaryMenuAction(
                                                label = "Feedback",
                                                iconRes = R.drawable.baseline_feedback_24,
                                                onClick = { settingsFeedbackRequest += 1 }
                                            )
                                        )
                                        else -> listOf(
                                            PrimaryMenuAction(
                                                label = "Live Cameras",
                                                iconRes = R.drawable.baseline_photo_camera_24,
                                                onClick = ::openLiveCamerasScreen
                                            ),
                                            PrimaryMenuAction(
                                                label = "Profile",
                                                iconRes = R.drawable.account_circle_24dp_ffffff_fill1_profile,
                                                onClick = ::openProfileScreen,
                                                useProfileAvatar = true
                                            )
                                        )
                                    }
                                },
                                onMenuDismiss = ::closeMenuSheet,
                                onOpenHome = { goToPage(PAGE_HOME) },
                                onOpenContacts = ::openBriefingPage,
                                onOpenNotes = { goToPage(PAGE_NOTES) },
                                onOpenSettings = { goToPage(PAGE_SETTINGS) },
                                onOpenMenu = ::openMenuSheet,
                                showTabs = false,
                                contentView = null
                            )
                        }

                        SettingsMainSearchButton(
                            visible = selectedMainPage == PAGE_SETTINGS &&
                                    !settingsModalVisible &&
                                    !showMenuSheet &&
                                    !settingsSearchSheetVisible.value,
                            backdrop = mainMenuBackdrop,
                            onClick = { settingsSearchSheetVisible.value = true },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 22.dp, bottom = 76.dp)
                                .navigationBarsPadding()
                                .zIndex(90f)
                        )

                        SettingsSearchGlassPanel(
                            visible = selectedMainPage == PAGE_SETTINGS && settingsSearchSheetVisible.value,
                            query = settingsSearchQuery.value,
                            onQueryChange = { settingsSearchQuery.value = it },
                            backdrop = mainMenuBackdrop,
                            onDismiss = {
                                settingsSearchQuery.value = ""
                                settingsSearchSheetVisible.value = false
                            },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .then(
                                    if (settingsKeyboardOpen) {
                                        Modifier.imePadding()
                                    } else {
                                        Modifier.navigationBarsPadding()
                                    }
                                )
                                .padding(
                                    start = 14.dp,
                                    end = 14.dp,
                                    bottom = if (settingsKeyboardOpen) 10.dp else 76.dp
                                )
                                .zIndex(95f)
                        )

                        EmergencyMessageCard(
                            message = emergencyMessage,
                            backdrop = mainMenuBackdrop,
                            visible = emergencyMessage?.let { message ->
                                val pageName = emergencyPageName(selectedMainPage)
                                val wasDismissed = message.canDismiss() && message.key == dismissedEmergencyKey
                                message.shouldShowOnPage(pageName) &&
                                        !wasDismissed &&
                                        !showMainWelcome &&
                                        !(selectedMainPage == PAGE_HOME && homeCameraExpanded)
                            } == true,
                            onDismiss = {
                                emergencyMessage?.key?.let { key ->
                                    dismissedEmergencyKey = key
                                    emergencyPrefs.edit {
                                        putString(DISMISSED_EMERGENCY_KEY, key)
                                    }
                                }
                            },
                            onAction = { message ->
                                openEmergencyAction(message.actionUrl)
                            },
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .statusBarsPadding()
                                .padding(start = 14.dp, end = 14.dp, top = 12.dp)
                                .zIndex(92f)
                        )

                        MainWelcomeOnboardingOverlay(
                            visible = showMainWelcome,
                            backdrop = mainMenuBackdrop,
                            onDone = {
                                welcomePrefs.edit {
                                    putInt(MAIN_WELCOME_SEEN_VERSION, MAIN_WELCOME_VERSION)
                                }
                                showMainWelcome = false
                            }
                        )

                        ReminderOptionsSheetModal(
                            backdrop = mainMenuBackdrop,
                            visible = reminderNote != null,
                            onDismiss = ::dismissReminder,
                            onTimer = ::openTimerForCurrentReminder,
                            onCalendar = ::openCalendarForCurrentReminder
                        )
                        ReminderTimePickerSheet(
                            visible = reminderTimeNote != null,
                            backdrop = mainMenuBackdrop,
                            note = reminderTimeNote,
                            onDismiss = { reminderTimeNote = null },
                            onSetReminder = { hourOfDay, minute, dayOffset, alarmSoundUri ->
                                reminderTimeNote?.let { note ->
                                    scheduleReminderUsingWorkManager(note, hourOfDay, minute, dayOffset, alarmSoundUri)
                                } == true
                            }
                        )
                        ReminderDetailsSheet(
                            info = reminderDetails,
                            backdrop = mainMenuBackdrop,
                            onDismiss = { reminderDetails = null },
                            onEdit = { info ->
                                reminderDetails = null
                                reminderTimeNote = info.note
                            },
                            onCancelReminder = { info ->
                                cancelReminder(info.note)
                                reminderDetails = null
                            }
                        )
                    }

                    Log.d(TAG_MAIN, "FlightsBackdropScaffold: composition EXIT")
                }

                Log.d(TAG_MAIN, "MaterialTheme: composition EXIT")
            }

            Log.d(TAG_MAIN, "setContent: root composition EXIT")
        }

        Log.d(TAG_MAIN, "onCreate() END")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.hasExtra(EXTRA_START_PAGE)) {
            openRequestedMainPage?.invoke(
                intent.getIntExtra(EXTRA_START_PAGE, PAGE_HOME)
                    .coerceIn(PAGE_HOME, PAGE_SETTINGS)
            )
        } else if (isPlainMainLaunch(intent)) {
            openRequestedMainPage?.invoke(PAGE_HOME)
        }
    }

    override fun onResume() {
        super.onResume()
        applyMainPageKeepScreenOn(currentMainPageForScreenAwake)
    }

    private fun applyMainPageKeepScreenOn(page: Int) {
        val keepAwake = page == PAGE_HOME && SettingsStore.mainPageKeepAwake(this)
        if (keepAwake) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    fun updateContactsChromeCount(visibleCount: Int) {
        contactsChromeCount.intValue = visibleCount
    }

    fun updateContactsSearch(query: String) {
        contactsSearchQuery.value = query
        if (query.isBlank() && !contactsFloatingSearchActive.value) {
            contactsFloatingSearchVisible.value = false
        }
        allContactsFragment?.filterContacts(query)
    }

    fun updateContactsFloatingSearch(query: String) {
        contactsFloatingSearchActive.value = true
        contactsFloatingSearchVisible.value = true
        contactsSearchQuery.value = query
        allContactsFragment?.filterContacts(
            query = query,
            syncTopSearch = false,
            keepFloatingSearchActive = true
        )
    }

    fun updateContactsFloatingSearchVisible(visible: Boolean) {
        if (!visible && (contactsFloatingSearchActive.value || contactsSearchQuery.value.isNotBlank())) {
            return
        }
        contactsFloatingSearchVisible.value = visible
    }

    fun updateContactsSelectionCount(count: Int) {
        contactsSelectionCount.intValue = count
        if (count > 0) {
            contactsFloatingSearchActive.value = false
            contactsFloatingSearchVisible.value = false
        }
    }

    fun updateContactsAlphabeticalMode(enabled: Boolean) {
        contactsAlphabeticalMode.value = enabled
    }

    fun updateContactsAddFabVisible(visible: Boolean) {
        contactsAddFabVisible.value = visible
    }

    private fun clearContactsSelection() {
        allContactsFragment?.clearContactSelection()
        contactsSelectionCount.intValue = 0
    }

    private fun selectAllVisibleContacts() {
        allContactsFragment?.selectAllVisibleContacts()
    }

    private fun deleteSelectedContacts() {
        allContactsFragment?.deleteSelectedContacts()
    }

    /**
     * Configures the [NotesAdapter] and its interaction callbacks for note management.
     * Sets up listeners for long-press selection, single-tap viewing, and reminder management.
     */
    private fun setupNotes() {
        notesAdapter = NotesAdapter(
            notesText,
            applicationContext,
            ::onNoteLongClick,
            ::onNoteClick,
            { note, position ->
                val wantsReminder = getSharedPreferences("reminder_flags", MODE_PRIVATE)
                    .getBoolean(note.hashCode().toString(), false)
                editNoteLauncher.launch(
                    EditNoteComposeActivity.newIntent(
                        context = this,
                        note = note,
                        title = resolveTitle(note),
                        images = NoteMediaStore.getUris(this, note),
                        attachments = NoteAttachmentStore.getItems(this, note),
                        voiceNotes = NoteVoiceStore.getItems(this, note),
                        wantsReminder = wantsReminder,
                        position = position
                    ),
                    ActivityOptionsCompat.makeSceneTransitionAnimation(this)
                )
            },
            { note, _ ->
                val activeReminder = readReminderInfo(note)
                if (activeReminder != null) {
                    openReminderDetails(activeReminder)
                } else {
                    openReminderSheet(note)
                }
            }
        ).also { adapter ->
            adapter.provideKeyResolver { note -> contentToUid[note] ?: ensureLocalUid(note) }
        }
        notesAdapter.preloadBadgeStates(this)
        notesAdapter.preloadReminderFlags(this)
    }

    private fun onNoteClick(note: String, position: Int) {
        if (isMultiSelectMode) {
            notesAdapter.toggleSelectionByKey(contentToUid[note] ?: ensureLocalUid(note))
            return
        }
        viewNoteLauncher.launch(ViewNoteComposeActivity.newIntent(this, note, note, position, resolveTitle(note)))
        overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation)
    }

    private fun onNoteLongClick(note: String) {
        isMultiSelectMode = true
        notesAdapter.toggleSelectionByKey(contentToUid[note] ?: ensureLocalUid(note))
    }

    private fun handleNoteEditResult(data: Intent?) {
        data ?: return
        val updatedNote = data.getStringExtra("UPDATED_NOTE").orEmpty()
        if (updatedNote.isBlank()) return

        val position = data.getIntExtra("NOTE_POSITION", -1)
        val oldNote = notesText.getOrNull(position) ?: return
        val updatedTitle = data.getStringExtra("UPDATED_TITLE").orEmpty()
        val updatedWantsReminder = data.getBooleanExtra("UPDATED_NOTE_WANTS_REMINDER", false)
        val updatedImages = data.getStringArrayListExtra("UPDATED_IMAGES")
            ?.mapNotNull { runCatching { it.toUri() }.getOrNull() }
            .orEmpty()
        val updatedFileUris = data.getStringArrayListExtra("UPDATED_FILE_URIS").orEmpty()
        val updatedFileNames = data.getStringArrayListExtra("UPDATED_FILE_NAMES").orEmpty()
        val updatedFileMimes = data.getStringArrayListExtra("UPDATED_FILE_MIMES").orEmpty()
        val updatedFileSizes = data.getLongArrayExtra("UPDATED_FILE_SIZES") ?: longArrayOf()
        val updatedFiles = updatedFileUris.mapIndexed { index, uri ->
            NoteAttachmentItem(
                uri = uri,
                name = updatedFileNames.getOrNull(index).orEmpty().ifBlank { "Attachment ${index + 1}" },
                mime = updatedFileMimes.getOrNull(index)?.takeIf { it.isNotBlank() },
                sizeBytes = updatedFileSizes.getOrNull(index) ?: 0L
            )
        }
        val updatedVoiceUris = data.getStringArrayListExtra("UPDATED_VOICE_URIS").orEmpty()
        val updatedVoiceDurations = data.getLongArrayExtra("UPDATED_VOICE_DURATIONS") ?: longArrayOf()
        val updatedVoiceCreatedAt = data.getLongArrayExtra("UPDATED_VOICE_CREATED_AT") ?: longArrayOf()
        val updatedVoiceItems = updatedVoiceUris.mapIndexed { index, uri ->
            NoteVoiceItem(
                uri = uri,
                durationMs = updatedVoiceDurations.getOrNull(index) ?: 0L,
                createdAtMs = updatedVoiceCreatedAt.getOrNull(index) ?: System.currentTimeMillis()
            )
        }

        if (oldNote != updatedNote) {
            notesAdapter.migrateUserTitle(oldNote, updatedNote)
            NoteMediaStore.migrateNoteKey(this, oldNote, updatedNote)
            NoteVoiceStore.migrateNoteKey(this, oldNote, updatedNote)
            NoteAttachmentStore.migrateNoteKey(this, oldNote, updatedNote)
            contentToUid[updatedNote] = contentToUid.remove(oldNote) ?: ensureLocalUid(updatedNote)
            uidToContent.entries.firstOrNull { it.value == oldNote }?.let { entry ->
                uidToContent[entry.key] = updatedNote
            }
            saveUidMaps()
        }

        allNotes.indexOf(oldNote).takeIf { it >= 0 }?.let { allNotes[it] = updatedNote }
        val updatedNoteUid = contentToUid[updatedNote] ?: ensureLocalUid(updatedNote)
        saveNoteMediaForKeys(
            context = this,
            note = updatedNote,
            noteKey = updatedNoteUid,
            imageUris = updatedImages,
            attachments = updatedFiles,
            voiceItems = updatedVoiceItems
        )
        if (updatedTitle.isNotBlank()) notesAdapter.setUserTitle(updatedNote, updatedTitle)
        else notesAdapter.removeUserTitle(updatedNote)
        getSharedPreferences("reminder_flags", MODE_PRIVATE).edit {
            if (updatedWantsReminder) putBoolean(updatedNote.hashCode().toString(), true)
            else remove(updatedNote.hashCode().toString())
        }
        notesAdapter.preloadReminderFlags(this)

        refreshNotesDisplay()
        saveNotes()
        syncEditedNoteToSupabase(
            oldNote = oldNote,
            updatedNote = updatedNote,
            titleOverride = updatedTitle,
            imageUrisOverride = updatedImages,
            fileItemsOverride = updatedFiles,
            voiceItemsOverride = updatedVoiceItems,
            hasReminderOverride = updatedWantsReminder
        )
    }

    private fun loadNotesHeadless() {
        val cached = NotesCacheManager.cachedNotes
        val base = when {
            cached.isNotEmpty() -> cached.toList()
            else -> {
                val notesJson = sharedPreferences.getString("notes_list", null)
                if (!notesJson.isNullOrEmpty()) {
                    val type = object : TypeToken<MutableList<String>>() {}.type
                    val saved: MutableList<String> = Gson().fromJson(notesJson, type)
                    NotesCacheManager.cachedNotes = saved.toMutableList()
                    saved.toList()
                } else {
                    emptyList()
                }
            }
        }

        allNotes.clear()
        allNotes.addAll(base)
        refreshNotesDisplay()
    }

    /**
     * Rebuilds the current notes display by applying sorting and filtering
     * based on the currently selected folder and user preferences.
     */
    private fun refreshNotesDisplay() {
        refreshMainNoteFolderRows()
        val sorted = applyNotesSort(displayMainNotesForCurrentFolder())
        notesText.clear()
        notesText.addAll(sorted)
        notesAdapter.submit(sorted)
        rebuildNoteRowsFromDisplay(sorted)
        notesCount.intValue = allNotes.size
        NotesCacheManager.cachedNotes = allNotes.toMutableList()
    }

    private fun filterNotesDisplay(query: String) {
        val q = query.trim()
        if (q.isBlank()) {
            refreshNotesDisplay()
            return
        }

        if (currentNotesFolderId == null) {
            refreshMainNoteFolderRows(q)
            notesText.clear()
            notesAdapter.submit(emptyList())
            rebuildNoteRowsFromDisplay(emptyList())
            notesCount.intValue = allNotes.size
            return
        }

        val filtered = displayMainNotesForCurrentFolder().filter { note ->
            val title = resolveTitle(note).orEmpty()
            note.contains(q, ignoreCase = true) || title.contains(q, ignoreCase = true)
        }
        val sorted = applyNotesSort(filtered)
        notesText.clear()
        notesText.addAll(sorted)
        notesAdapter.submit(sorted)
        rebuildNoteRowsFromDisplay(sorted)
        notesCount.intValue = sorted.size
    }

    private fun openNotesSearchSheet(onDismiss: () -> Unit) {
        val dialog = BottomSheetDialog(this)
        fun px(dpValue: Int): Int = (dpValue * resources.displayMetrics.density).toInt()
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(px(22), px(18), px(22), px(24))
        }

        container.addView(
            TextView(this).apply {
                text = getString(R.string.search_notes)
                textSize = 20f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding(px(4), 0, px(4), px(14))
            }
        )

        val searchInput = EditText(this).apply {
            hint = if (currentNotesFolderId == null) "Search folders or notes" else "Search title or note"
            isSingleLine = true
            setPadding(px(18), px(12), px(18), px(12))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = px(22).toFloat()
                setColor(android.graphics.Color.TRANSPARENT)
                setStroke(px(1), android.graphics.Color.argb(80, 127, 127, 127))
            }
        }
        container.addView(searchInput)

        dialog.setContentView(container)
        dialog.setOnDismissListener {
            filterNotesDisplay("")
            onDismiss()
        }
        dialog.show()

        searchInput.requestFocus()
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterNotesDisplay(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun rebuildNoteRowsFromDisplay(display: List<String>) {
        noteRows.clear()
        val used = HashSet<String>()
        display.forEachIndexed { index, text ->
            val baseUid = contentToUid[text] ?: ensureLocalUid(text)
            val key = if (used.add(baseUid)) baseUid else "$baseUid#$index"
            val mediaCounts = noteMediaBadgeCounts(this, text)
            noteRows.add(
                NoteRow(
                    id = key,
                    text = text,
                    imagesCount = mediaCounts.images,
                    attachmentsCount = mediaCounts.documents,
                    audioCount = mediaCounts.audio,
                    videoCount = mediaCounts.video,
                    title = resolveTitle(text).orEmpty(),
                    hasReminder = getSharedPreferences("reminder_flags", MODE_PRIVATE)
                        .getBoolean(text.hashCode().toString(), false),
                    hasBadge = getSharedPreferences("reminder_badges", MODE_PRIVATE)
                        .getBoolean(text.hashCode().toString(), false),
                    createdAtMs = NoteCreatedAtStore.ensure(this, baseUid)
                )
            )
        }
    }

    private fun applyNotesSort(list: List<String>): List<String> {
        val settings = readNotesPageSettings()
        return when (settings.sortMode) {
            NotesPagePrefs.SORT_OLDEST -> list
            NotesPagePrefs.SORT_TITLE -> list.sortedWith(
                compareBy(String.CASE_INSENSITIVE_ORDER) { resolveTitle(it).orEmpty().ifBlank { it } }
            )
            NotesPagePrefs.SORT_REMINDERS_FIRST -> {
                val flags = getSharedPreferences("reminder_flags", MODE_PRIVATE)
                list.sortedWith(
                    compareByDescending<String> { flags.getBoolean(it.hashCode().toString(), false) }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { resolveTitle(it).orEmpty().ifBlank { it } }
                )
            }
            else -> list.asReversed()
        }
    }

    private fun deleteSelectedNotes(selectedRowKeys: Set<String>) {
        val toDelete = noteRows
            .filter { it.id in selectedRowKeys }
            .map { it.text }

        if (toDelete.isEmpty()) return

        toDelete.forEach { note -> allNotes.remove(note) }
        val notesRemovedEverywhere = toDelete.distinct().filter { note -> note !in allNotes }

        notesRemovedEverywhere.forEach { queuePendingDelete(it) }
        notesRemovedEverywhere.forEach { removePendingAdd(it) }
        if (notesRemovedEverywhere.isNotEmpty() && SupabaseManager.client.auth.currentSessionOrNull()?.user?.id != null) {
            syncPendingNotesToSupabase()
        }

        toDelete.distinct().forEach { note ->
            if (note !in allNotes) {
                NoteMediaStore.deleteAllForNote(this, note)
                NoteVoiceStore.deleteAllForNote(this, note)
                NoteAttachmentStore.deleteAllForNote(this, note)
                notesAdapter.removeUserTitle(note)
                removeUidFor(note)
            }
        }
        isMultiSelectMode = false
        notesAdapter.clearSelection()
        refreshNotesDisplay()
        saveNotes()
    }

    private fun deleteSelectedNoteFolders(selectedFolderIds: Set<String>) {
        if (selectedFolderIds.isEmpty()) return
        ensureMainNoteFolderAssignments(allNotes)

        val toDelete = allNotes.filter { note ->
            NoteFolderStore.folderForNoteKey(this, contentToUid[note] ?: ensureLocalUid(note)) in selectedFolderIds
        }.toSet()

        toDelete.forEach { queuePendingDelete(it) }
        toDelete.forEach { removePendingAdd(it) }
        if (SupabaseManager.client.auth.currentSessionOrNull()?.user?.id != null) {
            syncPendingNotesToSupabase()
        }

        toDelete.forEach { note ->
            NoteMediaStore.deleteAllForNote(this, note)
            NoteVoiceStore.deleteAllForNote(this, note)
            NoteAttachmentStore.deleteAllForNote(this, note)
            notesAdapter.removeUserTitle(note)
            removeUidFor(note)
        }

        allNotes.removeAll(toDelete)
        NoteFolderStore.removeFolders(this, selectedFolderIds)
        syncDeletedNoteFoldersToSupabase(selectedFolderIds)
        if (currentNotesFolderId in selectedFolderIds) currentNotesFolderId = null

        notesAdapter.clearSelection()
        refreshNotesDisplay()
        saveNotes()
    }

    private fun saveNotes() {
        sharedPreferences.edit(commit = true) {
            putString("notes_list", Gson().toJson(allNotes))
        }
    }

    /**
     * Synchronizes a note and its associated media (images, voice notes, attachments)
     * to the Supabase backend.
     *
     * @param content The text content of the note.
     * @param titleOverride Optional title to use instead of the auto-resolved title.
     * @param imageUrisOverride Optional list of image URIs to upload.
     * @param fileItemsOverride Optional list of file attachments to upload.
     * @param voiceItemsOverride Optional list of voice recordings to upload.
     * @param hasReminderOverride Optional flag indicating if the note has an active reminder.
     */
    private fun syncNoteWithAttachmentsToSupabase(
        content: String,
        titleOverride: String? = null,
        imageUrisOverride: List<android.net.Uri>? = null,
        fileItemsOverride: List<NoteAttachmentItem>? = null,
        voiceItemsOverride: List<NoteVoiceItem>? = null,
        hasReminderOverride: Boolean? = null
    ) {
        if (!notesOnlineSyncEnabled()) return
        lifecycleScope.launch {
            try {
                notesSyncStatus = NotesSyncUiStatus.Uploading
                val session = SupabaseManager.client.auth.currentSessionOrNull()
                    ?: throw IllegalStateException("No Supabase session")
                val userId = session.user?.id ?: throw IllegalStateException("No Supabase user id")

                val inserted = insertNoteInSupabaseRest(
                    authToken = session.accessToken,
                    userId = userId,
                    content = content,
                    titleOverride = titleOverride,
                    hasReminderOverride = hasReminderOverride
                )

                inserted.optString("id").takeIf { it.isNotBlank() }?.let { noteId ->
                    saveRemoteNoteId(noteId, content)
                    updateNoteMetadataInSupabase(
                        authToken = session.accessToken,
                        userId = userId,
                        noteId = noteId,
                        content = content,
                        titleOverride = titleOverride,
                        hasReminderOverride = hasReminderOverride
                    )
                    uploadNoteAttachmentsToSupabase(
                        content = content,
                        noteId = noteId,
                        userId = userId,
                        authToken = session.accessToken,
                        imageUrisOverride = imageUrisOverride,
                        fileItemsOverride = fileItemsOverride,
                        voiceItemsOverride = voiceItemsOverride
                    )
                }
                removePendingAdd(content)
                notesSyncStatus = NotesSyncUiStatus.Synced
            } catch (e: Exception) {
                notesSyncStatus = NotesSyncUiStatus.Error
                queuePendingAdd(content)
                Log.e(TAG_MAIN, "Error syncing note attachments", e)
                scheduleNotesSyncRetry()
            }
        }
    }

    private fun syncEditedNoteToSupabase(
        oldNote: String,
        updatedNote: String,
        titleOverride: String? = null,
        imageUrisOverride: List<android.net.Uri>? = null,
        fileItemsOverride: List<NoteAttachmentItem>? = null,
        voiceItemsOverride: List<NoteVoiceItem>? = null,
        hasReminderOverride: Boolean? = null
    ) {
        if (!notesOnlineSyncEnabled()) return

        if (oldNote != updatedNote) {
            queuePendingDelete(oldNote)
            queuePendingAdd(updatedNote)
        }

        lifecycleScope.launch {
            try {
                val session = SupabaseManager.client.auth.currentSessionOrNull()
                    ?: throw IllegalStateException("No Supabase session")
                val userId = session.user?.id ?: throw IllegalStateException("No Supabase user id")

                if (oldNote != updatedNote) {
                    notesSyncStatus = NotesSyncUiStatus.Deleting
                    hardDeleteNoteInSupabaseRest(
                        authToken = session.accessToken,
                        userId = userId,
                        id = remoteNoteIdFor(oldNote),
                        content = oldNote
                    )
                    removeRemoteNoteId(oldNote)
                    removePendingDelete(oldNote)

                    notesSyncStatus = NotesSyncUiStatus.Uploading
                    val inserted = insertNoteInSupabaseRest(
                        authToken = session.accessToken,
                        userId = userId,
                        content = updatedNote,
                        titleOverride = titleOverride,
                        hasReminderOverride = hasReminderOverride
                    )
                    inserted.optString("id").takeIf { it.isNotBlank() }?.let { noteId ->
                        saveRemoteNoteId(noteId, updatedNote)
                        updateNoteMetadataInSupabase(
                            authToken = session.accessToken,
                            userId = userId,
                            noteId = noteId,
                            content = updatedNote,
                            titleOverride = titleOverride,
                            hasReminderOverride = hasReminderOverride
                        )
                        uploadNoteAttachmentsToSupabase(
                            content = updatedNote,
                            noteId = noteId,
                            userId = userId,
                            authToken = session.accessToken,
                            imageUrisOverride = imageUrisOverride,
                            fileItemsOverride = fileItemsOverride,
                            voiceItemsOverride = voiceItemsOverride
                        )
                    }
                    removePendingAdd(updatedNote)
                } else {
                    val noteId = remoteNoteIdFor(updatedNote)
                    if (noteId != null) {
                        notesSyncStatus = NotesSyncUiStatus.Uploading
                        updateNoteMetadataInSupabase(
                            authToken = session.accessToken,
                            userId = userId,
                            noteId = noteId,
                            content = updatedNote,
                            titleOverride = titleOverride,
                            hasReminderOverride = hasReminderOverride
                        )
                        uploadNoteAttachmentsToSupabase(
                            content = updatedNote,
                            noteId = noteId,
                            userId = userId,
                            authToken = session.accessToken,
                            imageUrisOverride = imageUrisOverride,
                            fileItemsOverride = fileItemsOverride,
                            voiceItemsOverride = voiceItemsOverride
                        )
                    } else {
                        queuePendingAdd(updatedNote)
                        syncLocalNotesToSupabase(session.accessToken, userId)
                    }
                }

                notesSyncStatus = NotesSyncUiStatus.Downloading
                pullNotesFromSupabase(userId, session.accessToken)
                notesSyncStatus = NotesSyncUiStatus.Synced
            } catch (e: Exception) {
                notesSyncStatus = NotesSyncUiStatus.Error
                if (oldNote != updatedNote) queuePendingDelete(oldNote)
                queuePendingAdd(updatedNote)
                Log.e(TAG_MAIN, "Error syncing edited note", e)
                scheduleNotesSyncRetry()
            }
        }
    }

    private fun notesOnlineSyncEnabled(): Boolean {
        return getSharedPreferences(NotesPagePrefs.NAME, MODE_PRIVATE).getBoolean(
            NotesPagePrefs.KEY_SYNC_ONLINE,
            NotesPagePrefs.DEFAULT_SYNC_ONLINE
        )
    }

    private fun queuePendingAdd(content: String) {
        if (!notesOnlineSyncEnabled() || content.isBlank()) return
        val pending = sharedPreferences
            .getStringSet("pending_adds", emptySet())
            .orEmpty()
            .toMutableSet()
        if (pending.add(content)) {
            sharedPreferences.edit { putStringSet("pending_adds", pending) }
        }
    }

    private fun queuePendingDelete(content: String) {
        if (!notesOnlineSyncEnabled() || content.isBlank()) return
        val userId = SupabaseManager.client.auth.currentSessionOrNull()?.user?.id ?: return
        val pending = pendingDeletesForUser(userId)
        if (pending.add(content)) {
            savePendingDeletesForUser(userId, pending)
        }
    }

    private fun removePendingDelete(content: String) {
        val userId = SupabaseManager.client.auth.currentSessionOrNull()?.user?.id
        if (userId != null) {
            val pending = pendingDeletesForUser(userId)
            if (pending.remove(content)) savePendingDeletesForUser(userId, pending)
        } else {
            val pending = sharedPreferences
                .getStringSet("pending_deletes", emptySet())
                .orEmpty()
                .toMutableSet()
            if (pending.remove(content)) {
                sharedPreferences.edit(commit = true) {
                    if (pending.isEmpty()) remove("pending_deletes")
                    else putStringSet("pending_deletes", pending)
                }
            }
        }
    }

    private fun pendingDeletesKey(userId: String): String = "pending_deletes_$userId"

    private fun pendingDeletesForUser(userId: String): MutableSet<String> {
        val accountDeletes = sharedPreferences.getStringSet(pendingDeletesKey(userId), emptySet()).orEmpty()
        val legacyDeletes = sharedPreferences.getStringSet("pending_deletes", emptySet()).orEmpty()
        return (accountDeletes + legacyDeletes).toMutableSet()
    }

    private fun savePendingDeletesForUser(userId: String, deletes: Set<String>) {
        sharedPreferences.edit(commit = true) {
            remove("pending_deletes")
            if (deletes.isEmpty()) remove(pendingDeletesKey(userId))
            else putStringSet(pendingDeletesKey(userId), deletes)
        }
    }

    private fun scheduleNotesSyncRetry() {
        lifecycleScope.launch {
            delay(1200.milliseconds)
            syncPendingNotesToSupabase()
        }
    }

    private fun syncPendingNotesToSupabase() {
        if (!notesOnlineSyncEnabled()) {
            notesSyncStatus = NotesSyncUiStatus.Synced
            return
        }
        val session = SupabaseManager.client.auth.currentSessionOrNull() ?: return
        val userId = session.user?.id ?: return

        lifecycleScope.launch {
            try {
                val hasDeletes = pendingDeletesForUser(userId).isNotEmpty()
                notesSyncStatus = if (hasDeletes) {
                    NotesSyncUiStatus.Deleting
                } else {
                    NotesSyncUiStatus.Syncing
                }
                syncPendingDeletesToSupabase(session.accessToken, userId)
                runCatching {
                    syncLocalNoteFoldersToSupabase(session.accessToken, userId)
                }.onFailure { Log.e(TAG_MAIN, "Folder sync skipped/failed", it) }
                notesSyncStatus = NotesSyncUiStatus.Uploading
                syncLocalNotesToSupabase(session.accessToken, userId)
                notesSyncStatus = NotesSyncUiStatus.Downloading
                pullNotesFromSupabase(userId, session.accessToken)
                notesSyncStatus = NotesSyncUiStatus.Synced
            } catch (e: Exception) {
                notesSyncStatus = NotesSyncUiStatus.Error
                Log.e(TAG_MAIN, "Notes sync retry failed", e)
            }
        }
    }

    private suspend fun syncPendingDeletesToSupabase(authToken: String, userId: String) {
        val pending = pendingDeletesForUser(userId)
        if (pending.isEmpty()) return

        val stillPending = mutableSetOf<String>()
        pending.forEach { content ->
            try {
                val deleted = hardDeleteNoteInSupabaseRest(
                    authToken = authToken,
                    userId = userId,
                    id = remoteNoteIdFor(content),
                    content = content
                )
                if (!deleted) Log.d(TAG_MAIN, "No Supabase note matched delete; clearing pending delete")
                removeRemoteNoteId(content)
            } catch (e: Exception) {
                Log.e(TAG_MAIN, "Failed to delete note from Supabase", e)
                stillPending.add(content)
            }
        }

        savePendingDeletesForUser(userId, stillPending)
    }

    private suspend fun syncLocalNotesToSupabase(authToken: String, userId: String) {
        runCatching {
            syncLocalNoteFoldersToSupabase(authToken, userId)
        }.onFailure { Log.e(TAG_MAIN, "Folder sync skipped/failed", it) }

        val remoteRows = fetchActiveRemoteRows(userId)
        val remoteContents = remoteRows.map { it.content }.toSet()
        remoteRows.forEach { row ->
            row.id?.let { saveRemoteNoteId(it, row.content) }
        }

        val pendingAdds = sharedPreferences
            .getStringSet("pending_adds", emptySet())
            .orEmpty()
            .toMutableSet()
        val pendingDeletes = pendingDeletesForUser(userId).toSet()

        val stillPending = mutableSetOf<String>()
        (allNotes + pendingAdds)
            .filter { it.isNotBlank() }
            .distinct()
            .forEach { content ->
                if (content in pendingDeletes || content in remoteContents) return@forEach
                try {
                    val inserted = insertNoteInSupabaseRest(authToken, userId, content)
                    inserted.optString("id").takeIf { it.isNotBlank() }?.let { noteId ->
                        saveRemoteNoteId(noteId, content)
                        updateNoteMetadataInSupabase(authToken, userId, noteId, content)
                        uploadNoteAttachmentsToSupabase(content, noteId, userId, authToken)
                    }
                } catch (e: Exception) {
                    Log.e(TAG_MAIN, "Failed to upload local note to Supabase", e)
                    stillPending.add(content)
                }
            }

        sharedPreferences.edit { putStringSet("pending_adds", stillPending) }
    }

    private suspend fun pullNotesFromSupabase(userId: String, authToken: String) {
        runCatching {
            NoteFolderStore.mergeRemoteFolders(this, fetchRemoteNoteFolders(authToken, userId))
        }.onFailure { Log.e(TAG_MAIN, "Remote folder pull skipped/failed", it) }

        val pendingDeletes = pendingDeletesForUser(userId).toSet()
        val remoteRows = fetchActiveRemoteRows(userId).filter { it.content !in pendingDeletes }
        val remoteExtras = fetchRemoteNoteExtras(remoteRows, authToken)
        val localSet = allNotes.toSet()
        var changed = false
        remoteRows.forEach { row ->
            row.id?.let { saveRemoteNoteId(it, row.content) }
            rememberRemoteCreatedAt(row)
            NoteFolderStore.assignRemoteNoteToFolder(
                this,
                contentToUid[row.content] ?: ensureLocalUid(row.content),
                row.folderId
            )
            if (row.content !in localSet) {
                allNotes.add(row.content)
                changed = true
            }

            row.title?.takeIf { it.isNotBlank() }?.let { title ->
                notesAdapter.setUserTitle(row.content, title)
            } ?: notesAdapter.removeUserTitle(row.content)

            val pulseWasCancelled = ReminderPulseCancellationStore.isCancelled(this, row.content)
            val localPendingReminder = !pulseWasCancelled && hasPendingReminderPulse(row.content)
            val shouldKeepReminder = row.hasReminder || localPendingReminder
            val shouldKeepBadge = !pulseWasCancelled && (row.hasReminderBadge || localPendingReminder)
            getSharedPreferences("reminder_flags", MODE_PRIVATE).edit {
                if (shouldKeepReminder) putBoolean(row.content.hashCode().toString(), true)
                else remove(row.content.hashCode().toString())
            }
            getSharedPreferences("reminder_badges", MODE_PRIVATE).edit {
                if (shouldKeepBadge) putBoolean(row.content.hashCode().toString(), true)
                else remove(row.content.hashCode().toString())
            }

            val noteKey = contentToUid[row.content] ?: ensureLocalUid(row.content)
            remoteExtras.images[row.content]?.takeIf { it.isNotEmpty() }?.let { images ->
                saveNoteMediaForKeys(this, row.content, noteKey, imageUris = images)
            }
            remoteExtras.files[row.content]?.takeIf { it.isNotEmpty() }?.let { files ->
                saveNoteMediaForKeys(this, row.content, noteKey, attachments = files)
            }
            remoteExtras.voice[row.content]?.takeIf { it.isNotEmpty() }?.let { voice ->
                saveNoteMediaForKeys(this, row.content, noteKey, voiceItems = voice)
            }
        }
        notesAdapter.preloadReminderFlags(this)
        notesAdapter.preloadBadgeStates(this)
        refreshNotesDisplay()
        if (changed) saveNotes()
    }

    private fun rememberRemoteCreatedAt(row: UserNote) {
        val noteKey = contentToUid[row.content] ?: ensureLocalUid(row.content)
        val remoteCreatedAt = NoteCreatedAtStore.parseSupabaseTimestamp(row.createdAt)
        if (remoteCreatedAt != null) {
            NoteCreatedAtStore.setIfAbsent(this, noteKey, remoteCreatedAt)
        } else {
            NoteCreatedAtStore.ensure(this, noteKey)
        }
    }

    private data class RemoteNoteExtras(
        val images: Map<String, List<android.net.Uri>>,
        val files: Map<String, List<NoteAttachmentItem>>,
        val voice: Map<String, List<NoteVoiceItem>>
    )

    private suspend fun fetchRemoteNoteExtras(
        rows: List<UserNote>,
        authToken: String
    ): RemoteNoteExtras = withContext(Dispatchers.IO) {
        val imagesByContent = mutableMapOf<String, List<android.net.Uri>>()
        val filesByContent = mutableMapOf<String, List<NoteAttachmentItem>>()
        val voiceByContent = mutableMapOf<String, List<NoteVoiceItem>>()

        rows.forEach { row ->
            val noteId = row.id ?: return@forEach
            imagesByContent[row.content] = fetchRemoteNoteImageUris(
                noteId = noteId,
                userId = row.userId,
                authToken = authToken
            )

            val attachments = fetchRemoteNoteAttachments(
                noteId = noteId,
                userId = row.userId,
                authToken = authToken
            )
            filesByContent[row.content] = attachments.first
            voiceByContent[row.content] = attachments.second
        }

        RemoteNoteExtras(
            images = imagesByContent,
            files = filesByContent,
            voice = voiceByContent
        )
    }

    private suspend fun fetchRemoteNoteImageUris(
        noteId: String,
        userId: String,
        authToken: String
    ): List<android.net.Uri> = withContext(Dispatchers.IO) {
        val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
        val request = Request.Builder()
            .url(
                "$baseUrl/rest/v1/note_images" +
                        "?select=path" +
                        "&user_id=eq.${urlEncode(userId)}" +
                        "&note_id=eq.${urlEncode(noteId)}" +
                        "&order=created_at.asc"
            )
            .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $authToken")
            .build()

        notesHttpClient.newCall(request).execute().use { response ->
            val body = runCatching { response.body.string() }.getOrDefault("")
            if (!response.isSuccessful) {
                Log.e(TAG_MAIN, "Fetch note images failed: ${response.code} $body")
                return@withContext emptyList()
            }

            val rowsJson = JSONArray(body.ifBlank { "[]" })
            buildList {
                for (index in 0 until rowsJson.length()) {
                    val path = rowsJson.optJSONObject(index)?.optString("path").orEmpty()
                    if (path.isBlank()) continue
                    val signedUrl = SupabaseStorageUploader.createSignedUrl(
                        objectPath = path,
                        authToken = authToken,
                        bucket = "note-attachments",
                        expiresInSeconds = 60 * 60 * 24 * 7
                    )
                    if (!signedUrl.isNullOrBlank()) add(signedUrl.toUri())
                }
            }
        }
    }

    private suspend fun fetchRemoteNoteAttachments(
        noteId: String,
        userId: String,
        authToken: String
    ): Pair<List<NoteAttachmentItem>, List<NoteVoiceItem>> = withContext(Dispatchers.IO) {
        val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
        val request = Request.Builder()
            .url(
                "$baseUrl/rest/v1/user_note_attachments" +
                        "?select=*" +
                        "&user_id=eq.${urlEncode(userId)}" +
                        "&note_id=eq.${urlEncode(noteId)}"
            )
            .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $authToken")
            .build()

        notesHttpClient.newCall(request).execute().use { response ->
            val body = runCatching { response.body.string() }.getOrDefault("")
            if (!response.isSuccessful) {
                Log.e(TAG_MAIN, "Fetch note attachments failed: ${response.code} $body")
                return@withContext emptyList<NoteAttachmentItem>() to emptyList()
            }

            val files = mutableListOf<NoteAttachmentItem>()
            val voice = mutableListOf<NoteVoiceItem>()
            val rowsJson = JSONArray(body.ifBlank { "[]" })
            for (index in 0 until rowsJson.length()) {
                val item = rowsJson.optJSONObject(index) ?: continue
                val path = item.optString("storage_path").orEmpty()
                if (path.isBlank()) continue
                val signedUrl = SupabaseStorageUploader.createSignedUrl(
                    objectPath = path,
                    authToken = authToken,
                    bucket = "note-attachments",
                    expiresInSeconds = 60 * 60 * 24 * 7
                ) ?: continue

                val kind = item.optString("kind").lowercase(Locale.US)
                if (kind == "voice") {
                    voice += NoteVoiceItem(
                        uri = signedUrl,
                        durationMs = item.optLong("duration_ms", 0L),
                        createdAtMs = item.optLong("created_at_ms", System.currentTimeMillis())
                    )
                } else {
                    files += NoteAttachmentItem(
                        uri = signedUrl,
                        name = item.optString("file_name").ifBlank { "Attachment ${files.size + 1}" },
                        mime = item.optString("mime_type").takeIf { it.isNotBlank() },
                        sizeBytes = item.optLong("size_bytes", 0L),
                        remotePath = path
                    )
                }
            }

            files to voice
        }
    }

    private suspend fun fetchActiveRemoteRows(userId: String): List<UserNote> = withContext(Dispatchers.IO) {
        val rows: List<UserNote> = SupabaseManager.client
            .postgrest
            .from("user_notes")
            .select { filter { eq("user_id", userId) } }
            .decodeList()
        rows.filter { it.deletedAt == null }
    }

    private suspend fun insertNoteInSupabaseRest(
        authToken: String,
        userId: String,
        content: String,
        titleOverride: String? = null,
        hasReminderOverride: Boolean? = null
    ): JSONObject = withContext(Dispatchers.IO) {
        val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
        val noteTitle = titleOverride
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: notesAdapter.getUserTitle(content)?.takeIf { it.isNotBlank() }

        val body = JSONObject()
            .put("user_id", userId)
            .put("content", content)
            .put("title", noteTitle ?: JSONObject.NULL)
            .put("folder_id", localFolderIdForNote(content))
            .put(
                "has_reminder",
                hasReminderOverride ?: getSharedPreferences("reminder_flags", MODE_PRIVATE)
                    .getBoolean(content.hashCode().toString(), false)
            )
            .put(
                "has_reminder_badge",
                getSharedPreferences("reminder_badges", MODE_PRIVATE)
                    .getBoolean(content.hashCode().toString(), false)
            )
            .toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$baseUrl/rest/v1/user_notes")
            .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $authToken")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=representation")
            .post(body)
            .build()

        notesHttpClient.newCall(request).execute().use { response ->
            val responseBody = runCatching { response.body.string() }.getOrDefault("")
            if (!response.isSuccessful) {
                throw IllegalStateException("Insert note failed: ${response.code} $responseBody")
            }
            val rows = JSONArray(responseBody.ifBlank { "[]" })
            if (rows.length() == 0) throw IllegalStateException("Insert note returned no row")
            rows.getJSONObject(0)
        }
    }

    private suspend fun updateNoteMetadataInSupabase(
        authToken: String,
        userId: String,
        noteId: String,
        content: String,
        titleOverride: String? = null,
        hasReminderOverride: Boolean? = null
    ) = withContext(Dispatchers.IO) {
        val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
        val noteTitle = titleOverride
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: notesAdapter.getUserTitle(content)?.takeIf { it.isNotBlank() }

        val body = JSONObject()
            .put("title", noteTitle ?: JSONObject.NULL)
            .put("folder_id", localFolderIdForNote(content))
            .put(
                "has_reminder",
                hasReminderOverride ?: getSharedPreferences("reminder_flags", MODE_PRIVATE)
                    .getBoolean(content.hashCode().toString(), false)
            )
            .put(
                "has_reminder_badge",
                getSharedPreferences("reminder_badges", MODE_PRIVATE)
                    .getBoolean(content.hashCode().toString(), false)
            )
            .toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$baseUrl/rest/v1/user_notes?user_id=eq.${urlEncode(userId)}&id=eq.${urlEncode(noteId)}")
            .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $authToken")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=minimal")
            .patch(body)
            .build()

        notesHttpClient.newCall(request).execute().use { response ->
            val responseBody = runCatching { response.body.string() }.getOrDefault("")
            if (!response.isSuccessful) {
                throw IllegalStateException("Update note metadata failed: ${response.code} $responseBody")
            }
        }
    }

    private suspend fun hardDeleteNoteInSupabaseRest(
        authToken: String,
        userId: String,
        id: String?,
        content: String
    ): Boolean = withContext(Dispatchers.IO) {
        val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
        val filters = buildList {
            add("user_id=eq.${urlEncode(userId)}")
            if (!id.isNullOrBlank()) add("id=eq.${urlEncode(id)}")
            else add("content=eq.${urlEncode(content)}")
        }.joinToString("&")

        val request = Request.Builder()
            .url("$baseUrl/rest/v1/user_notes?$filters")
            .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $authToken")
            .addHeader("Prefer", "return=representation")
            .delete()
            .build()

        notesHttpClient.newCall(request).execute().use { response ->
            val responseBody = runCatching { response.body.string() }.getOrDefault("")
            if (!response.isSuccessful) {
                throw IllegalStateException("Delete note failed: ${response.code} $responseBody")
            }
            JSONArray(responseBody.ifBlank { "[]" }).length() > 0
        }
    }

    private fun removePendingAdd(content: String) {
        val pending = sharedPreferences
            .getStringSet("pending_adds", emptySet())
            .orEmpty()
            .toMutableSet()
        if (pending.remove(content)) {
            sharedPreferences.edit { putStringSet("pending_adds", pending) }
        }
    }

    private fun saveRemoteNoteId(noteId: String, content: String) {
        val idPrefs = getSharedPreferences("notes_ids", MODE_PRIVATE)
        val json = idPrefs.getString("id_to_content", "{}") ?: "{}"
        val type = object : TypeToken<MutableMap<String, String>>() {}.type
        val idToContent: MutableMap<String, String> = runCatching {
            Gson().fromJson<MutableMap<String, String>>(json, type)
        }.getOrNull() ?: mutableMapOf()

        idToContent[noteId] = content
        idPrefs.edit { putString("id_to_content", Gson().toJson(idToContent)) }
    }

    private fun remoteNoteIdFor(content: String): String? {
        val idPrefs = getSharedPreferences("notes_ids", MODE_PRIVATE)
        val json = idPrefs.getString("id_to_content", "{}") ?: "{}"
        val type = object : TypeToken<Map<String, String>>() {}.type
        val idToContent: Map<String, String> = runCatching {
            Gson().fromJson<Map<String, String>>(json, type)
        }.getOrNull() ?: emptyMap()
        return idToContent.entries.firstOrNull { it.value == content }?.key
    }

    private fun removeRemoteNoteId(content: String) {
        val idPrefs = getSharedPreferences("notes_ids", MODE_PRIVATE)
        val json = idPrefs.getString("id_to_content", "{}") ?: "{}"
        val type = object : TypeToken<MutableMap<String, String>>() {}.type
        val idToContent: MutableMap<String, String> = runCatching {
            Gson().fromJson<MutableMap<String, String>>(json, type)
        }.getOrNull() ?: mutableMapOf()
        val removed = idToContent.entries.removeAll { it.value == content }
        if (removed) {
            idPrefs.edit { putString("id_to_content", Gson().toJson(idToContent)) }
        }
    }

    private fun urlEncode(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")

    private suspend fun uploadNoteAttachmentsToSupabase(
        content: String,
        noteId: String,
        userId: String,
        authToken: String,
        imageUrisOverride: List<android.net.Uri>? = null,
        fileItemsOverride: List<NoteAttachmentItem>? = null,
        voiceItemsOverride: List<NoteVoiceItem>? = null
    ) = withContext(Dispatchers.IO) {
        val imageUris = imageUrisOverride ?: NoteMediaStore.getUris(this@MainActivity, content)
        val fileItems = fileItemsOverride ?: NoteAttachmentStore.getItems(this@MainActivity, content)
        val voiceItems = voiceItemsOverride ?: NoteVoiceStore.getItems(this@MainActivity, content)
        val uploadedImages = mutableListOf<UserNoteImage>()
        val uploaded = mutableListOf<UserNoteAttachment>()
        Log.d(
            TAG_MAIN,
            "Uploading main note extras noteId=$noteId title=${notesAdapter.getUserTitle(content).orEmpty()} images=${imageUris.size} files=${fileItems.size} voice=${voiceItems.size}"
        )

        imageUris.forEachIndexed { index, uri ->
            val fileName = noteImageFileName(uri, index)
            val mime = contentResolver.getType(uri) ?: "image/jpeg"
            val path = SupabaseStorageUploader.uploadNoteAttachmentAndReturnPath(
                context = this@MainActivity,
                userId = userId,
                authToken = authToken,
                noteId = noteId,
                sourceUri = uri,
                fileName = fileName,
                mimeHint = mime
            ) ?: throw IllegalStateException("Image upload failed: $fileName")
            uploadedImages += UserNoteImage(
                userId = userId,
                noteId = noteId,
                path = path,
                mimeType = mime
            )
        }

        fileItems.forEach { item ->
            if (!item.remotePath.isNullOrBlank()) return@forEach
            val path = SupabaseStorageUploader.uploadNoteAttachmentAndReturnPath(
                context = this@MainActivity,
                userId = userId,
                authToken = authToken,
                noteId = noteId,
                sourceUri = item.asUri,
                fileName = item.name,
                mimeHint = item.mime
            ) ?: throw IllegalStateException("Attachment upload failed: ${item.name}")
            NoteAttachmentStore.updateRemotePath(this@MainActivity, content, item.uri, path)
            uploaded += UserNoteAttachment(
                userId = userId,
                noteId = noteId,
                storagePath = path,
                fileName = item.name,
                mimeType = item.mime,
                sizeBytes = item.sizeBytes,
                kind = "file"
            )
        }

        voiceItems.forEachIndexed { index, item ->
            val fileName = "voice_${index + 1}.m4a"
            val path = SupabaseStorageUploader.uploadNoteAttachmentAndReturnPath(
                context = this@MainActivity,
                userId = userId,
                authToken = authToken,
                noteId = noteId,
                sourceUri = item.asUri,
                fileName = fileName,
                mimeHint = "audio/mp4"
            ) ?: throw IllegalStateException("Voice upload failed: $fileName")
            uploaded += UserNoteAttachment(
                userId = userId,
                noteId = noteId,
                storagePath = path,
                fileName = fileName,
                mimeType = "audio/mp4",
                sizeBytes = noteUriSize(item.asUri),
                kind = "voice"
            )
        }

        if (uploadedImages.isNotEmpty()) insertNoteImageRowsInSupabase(authToken, uploadedImages)
        if (uploaded.isNotEmpty()) insertNoteAttachmentRowsInSupabase(authToken, uploaded)
    }

    private suspend fun insertNoteImageRowsInSupabase(
        authToken: String,
        rows: List<UserNoteImage>
    ) = withContext(Dispatchers.IO) {
        if (rows.isEmpty()) return@withContext
        val body = JSONArray().apply {
            rows.forEach { image ->
                put(
                    JSONObject()
                        .put("user_id", image.userId)
                        .put("note_id", image.noteId)
                        .put("path", image.path)
                        .put("mime_type", image.mimeType ?: JSONObject.NULL)
                        .put("width", image.width ?: JSONObject.NULL)
                        .put("height", image.height ?: JSONObject.NULL)
                )
            }
        }.toString()
        insertRowsInSupabaseRest(authToken, "note_images", body)
    }

    private suspend fun insertNoteAttachmentRowsInSupabase(
        authToken: String,
        rows: List<UserNoteAttachment>
    ) = withContext(Dispatchers.IO) {
        if (rows.isEmpty()) return@withContext
        val body = JSONArray().apply {
            rows.forEach { attachment ->
                put(
                    JSONObject()
                        .put("user_id", attachment.userId)
                        .put("note_id", attachment.noteId)
                        .put("storage_path", attachment.storagePath)
                        .put("file_name", attachment.fileName)
                        .put("mime_type", attachment.mimeType ?: JSONObject.NULL)
                        .put("size_bytes", attachment.sizeBytes)
                        .put("kind", attachment.kind)
                        .put("duration_ms", attachment.durationMs ?: JSONObject.NULL)
                        .put("created_at_ms", attachment.createdAtMs ?: JSONObject.NULL)
                )
            }
        }.toString()
        insertRowsInSupabaseRest(authToken, "user_note_attachments", body)
    }

    private suspend fun insertRowsInSupabaseRest(
        authToken: String,
        table: String,
        body: String
    ) = withContext(Dispatchers.IO) {
        val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
        val request = Request.Builder()
            .url("$baseUrl/rest/v1/$table")
            .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $authToken")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=minimal")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        notesHttpClient.newCall(request).execute().use { response ->
            val responseBody = runCatching { response.body.string() }.getOrDefault("")
            if (!response.isSuccessful) {
                throw IllegalStateException("Insert $table failed: ${response.code} $responseBody")
            }
        }
    }

    private fun noteImageFileName(uri: android.net.Uri, index: Int): String {
        val raw = uri.lastPathSegment
            ?.substringAfterLast('/')
            ?.takeIf { it.isNotBlank() && "." in it }
        return raw ?: "photo_${index + 1}.jpg"
    }

    private fun noteUriSize(uri: android.net.Uri): Long =
        runCatching {
            contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
                descriptor.length.takeIf { it >= 0L } ?: 0L
            } ?: 0L
        }.getOrDefault(0L)

    private fun openMaterialDateTimePickerDialog(note: String) {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("")
            .build()

        datePicker.addOnPositiveButtonClickListener { selectedDateMillis ->
            openCalendarApp(note, selectedDateMillis)
        }
        datePicker.show(supportFragmentManager, "mainReminderDatePicker")
    }

    private fun openCalendarApp(note: String, beginTimeMillis: Long = System.currentTimeMillis()) {
        val intent = Intent(Intent.ACTION_INSERT)
            .setData(CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.Events.TITLE, note)
            .putExtra(CalendarContract.Events.EVENT_LOCATION, "Reminder")
            .putExtra(CalendarContract.Events.DESCRIPTION, "Reminder from app")
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginTimeMillis)

        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "No calendar app found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun scheduleReminderUsingWorkManager(
        note: String,
        hourOfDay: Int,
        minute: Int,
        dayOffset: Int = 0,
        alarmSoundUri: String? = null
    ): Boolean {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, dayOffset)
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val delay = calendar.timeInMillis - System.currentTimeMillis()
        if (delay <= 0) {
            return false
        }

        cancelReminderWorkOnly(note)

        val noteKey = "note_${System.currentTimeMillis()}"
        getSharedPreferences("reminder_notes", MODE_PRIVATE).edit {
            putString(noteKey, note)
        }

        val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    "note_key" to noteKey,
                    "alarm_sound_uri" to alarmSoundUri.orEmpty()
                )
            )
            .build()

        WorkManager.getInstance(this).enqueue(workRequest)
        val reminderKey = reminderKey(note)

        ReminderPulseCancellationStore.clear(this, note)
        getSharedPreferences("reminder_badges", MODE_PRIVATE).edit(commit = true) {
            putBoolean(reminderKey, true)
        }
        getSharedPreferences("reminder_flags", MODE_PRIVATE).edit(commit = true) {
            putBoolean(reminderKey, true)
        }
        getSharedPreferences("reminder_meta", MODE_PRIVATE).edit(commit = true) {
            putLong("${reminderKey}_trigger_at", calendar.timeInMillis)
            putString("${reminderKey}_work_id", workRequest.id.toString())
            putString("${reminderKey}_note_key", noteKey)
            putString("${reminderKey}_note", note)
            if (alarmSoundUri.isNullOrBlank()) {
                remove("${reminderKey}_alarm_sound_uri")
            } else {
                putString("${reminderKey}_alarm_sound_uri", alarmSoundUri)
            }
        }

        notesAdapter.preloadBadgeStates(this)
        notesAdapter.preloadReminderFlags(this)
        refreshNotesDisplay()
        syncEditedNoteToSupabase(note, note, hasReminderOverride = true)
        return true
    }

    private fun readReminderInfo(note: String): ReminderInfo? {
        val key = reminderKey(note)
        val meta = getSharedPreferences("reminder_meta", MODE_PRIVATE)
        val triggerAt = meta.getLong("${key}_trigger_at", 0L)
        if (triggerAt <= System.currentTimeMillis()) {
            clearReminderMetadata(note, keepBell = true)
            return null
        }
        return ReminderInfo(
            note = meta.getString("${key}_note", note).orEmpty().ifBlank { note },
            triggerAtMillis = triggerAt
        )
    }

    private fun cancelReminder(note: String) {
        cancelReminderWorkOnly(note)
        ReminderPulseCancellationStore.markCancelled(this, note)
        clearReminderMetadata(note, keepBell = true)
        notesAdapter.preloadBadgeStates(this)
        notesAdapter.preloadReminderFlags(this)
        refreshNotesDisplay()
        syncEditedNoteToSupabase(note, note, hasReminderOverride = true)
    }

    private fun clearReminderMetadata(note: String, keepBell: Boolean) {
        val key = reminderKey(note)
        getSharedPreferences("reminder_badges", MODE_PRIVATE).edit(commit = true) {
            putBoolean(key, false)
        }
        if (!keepBell) {
            getSharedPreferences("reminder_flags", MODE_PRIVATE).edit(commit = true) {
                putBoolean(key, false)
            }
        }
        getSharedPreferences("reminder_meta", MODE_PRIVATE).edit(commit = true) {
            remove("${key}_trigger_at")
            remove("${key}_work_id")
            remove("${key}_note_key")
            remove("${key}_note")
        }
    }

    private fun cancelReminderWorkOnly(note: String) {
        val key = reminderKey(note)
        val meta = getSharedPreferences("reminder_meta", MODE_PRIVATE)
        meta.getString("${key}_work_id", null)?.let { workId ->
            runCatching {
                WorkManager.getInstance(this).cancelWorkById(UUID.fromString(workId))
            }
        }
    }

    private fun reminderKey(note: String): String = note.hashCode().toString()

    private fun hasPendingReminderPulse(note: String): Boolean {
        val key = reminderKey(note)
        val triggerAt = getSharedPreferences("reminder_meta", MODE_PRIVATE)
            .getLong("${key}_trigger_at", 0L)
        val badgeOn = getSharedPreferences("reminder_badges", MODE_PRIVATE)
            .getBoolean(key, false)
        return badgeOn && triggerAt > System.currentTimeMillis()
    }

    private fun resolveTitle(note: String): String? =
        notesAdapter.getUserTitle(note) ?: NotesCacheManager.cachedTitles[note]

    private fun loadUidMaps() {
        uidToContent.clear()
        contentToUid.clear()
        val json = uidPrefs.getString("uid_to_content", "{}") ?: "{}"
        val type = object : TypeToken<Map<String, String>>() {}.type
        val saved: Map<String, String> = Gson().fromJson(json, type) ?: emptyMap()
        uidToContent.putAll(saved)
        contentToUid.putAll(saved.entries.associate { it.value to it.key })
    }

    private fun saveUidMaps() {
        uidPrefs.edit {
            putString("uid_to_content", Gson().toJson(uidToContent))
        }
    }

    private fun ensureLocalUid(note: String): String {
        return contentToUid.getOrPut(note) {
            UUID.randomUUID().toString().also { uid ->
                uidToContent[uid] = note
                saveUidMaps()
            }
        }
    }

    private fun removeUidFor(note: String) {
        contentToUid.remove(note)?.let {
            NoteFolderStore.removeNote(this, it)
            uidToContent.remove(it)
            NoteCreatedAtStore.remove(this, it)
        }
        saveUidMaps()
    }

    private fun ensureMainNoteFolderAssignments(notes: List<String>) {
        notes.forEach { note ->
            NoteFolderStore.ensureNoteInMain(this, contentToUid[note] ?: ensureLocalUid(note))
        }
    }

    private fun assignMainNoteToCurrentFolder(note: String) {
        val folderId = currentNotesFolderId ?: NoteFolderStore.MAIN_FOLDER_ID
        NoteFolderStore.assignNoteToFolder(
            context = this,
            noteKey = contentToUid[note] ?: ensureLocalUid(note),
            folderId = folderId
        )
        refreshMainNoteFolderRows()
    }

    private fun displayMainNotesForCurrentFolder(): List<String> {
        val folderId = currentNotesFolderId ?: return emptyList()
        ensureMainNoteFolderAssignments(allNotes)
        return allNotes.filter { note ->
            NoteFolderStore.folderForNoteKey(this, contentToUid[note] ?: ensureLocalUid(note)) == folderId
        }
    }

    private fun refreshMainNoteFolderRows(query: String = "") {
        ensureMainNoteFolderAssignments(allNotes)
        val counts = NoteFolderStore.countByFolder(
            this,
            allNotes.map { note -> contentToUid[note] ?: ensureLocalUid(note) }
        )
        val folders = buildList {
            if (allNotes.isNotEmpty()) {
                val main = NoteFolderStore.mainFolder()
                add(NoteFolderUi(main.id, main.name, counts[main.id] ?: 0, main.createdAt, main.modifiedAt, main.colorArgb))
            }
            NoteFolderStore.loadCustomFolders(this@MainActivity).forEach { folder ->
                add(NoteFolderUi(folder.id, folder.name, counts[folder.id] ?: 0, folder.createdAt, folder.modifiedAt, folder.colorArgb))
            }
        }
        val q = query.trim()
        val filteredFolders = if (q.isBlank()) {
            folders
        } else {
            folders.filter { folder ->
                folder.name.contains(q, ignoreCase = true) ||
                        allNotes.any { note ->
                            NoteFolderStore.folderForNoteKey(
                                this,
                                contentToUid[note] ?: ensureLocalUid(note)
                            ) == folder.id && noteMatchesFolderSearch(note, q)
                        }
            }
        }
        noteFolderRows.clear()
        noteFolderRows.addAll(filteredFolders)
    }

    private fun noteMatchesFolderSearch(note: String, query: String): Boolean {
        val title = resolveTitle(note).orEmpty()
        return note.contains(query, ignoreCase = true) || title.contains(query, ignoreCase = true)
    }

    private fun mainNotesFolderTitle(): String {
        val folderId = currentNotesFolderId ?: return "Folders"
        return NoteFolderStore.folderNameForId(this, folderId)
    }

    private fun localFolderIdForNote(content: String): String {
        val key = contentToUid[content] ?: ensureLocalUid(content)
        return NoteFolderStore.folderForNoteKey(this, key)
    }

    private suspend fun syncLocalNoteFoldersToSupabase(
        authToken: String,
        userId: String
    ) = withContext(Dispatchers.IO) {
        val folders = NoteFolderStore.loadCustomFolders(this@MainActivity)
        if (folders.isEmpty()) return@withContext

        val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
        val body = JSONArray().apply {
            folders.forEach { folder ->
                put(
                    JSONObject()
                        .put("id", folder.id)
                        .put("user_id", userId)
                        .put("name", folder.name)
                )
            }
        }.toString()

        val request = Request.Builder()
            .url("$baseUrl/rest/v1/note_folders")
            .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $authToken")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        notesHttpClient.newCall(request).execute().use { response ->
            val responseBody = runCatching { response.body.string() }.getOrDefault("")
            if (!response.isSuccessful) {
                throw IllegalStateException("Sync folders failed: ${response.code} $responseBody")
            }
        }
    }

    private suspend fun fetchRemoteNoteFolders(
        authToken: String,
        userId: String
    ): List<NoteFolder> = withContext(Dispatchers.IO) {
        val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
        val request = Request.Builder()
            .url(
                "$baseUrl/rest/v1/note_folders" +
                        "?select=id,name,created_at" +
                        "&user_id=eq.${urlEncode(userId)}" +
                        "&deleted_at=is.null" +
                        "&order=created_at.asc"
            )
            .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $authToken")
            .build()

        notesHttpClient.newCall(request).execute().use { response ->
            val body = runCatching { response.body.string() }.getOrDefault("")
            if (!response.isSuccessful) {
                throw IllegalStateException("Fetch folders failed: ${response.code} $body")
            }
            val rows = JSONArray(body.ifBlank { "[]" })
            buildList {
                for (index in 0 until rows.length()) {
                    val item = rows.optJSONObject(index) ?: continue
                    val id = item.optString("id").takeIf { it.isNotBlank() } ?: continue
                    val name = item.optString("name").takeIf { it.isNotBlank() } ?: continue
                    val createdAt = item.optString("created_at")
                        .takeIf { it.isNotBlank() }
                        ?.let { runCatching { java.time.Instant.parse(it).toEpochMilli() }.getOrNull() }
                        ?: System.currentTimeMillis()
                    add(NoteFolder(id = id, name = name, createdAt = createdAt))
                }
            }
        }
    }

    private fun syncDeletedNoteFoldersToSupabase(folderIds: Set<String>) {
        val removableIds = folderIds - NoteFolderStore.MAIN_FOLDER_ID
        if (removableIds.isEmpty()) return
        val session = SupabaseManager.client.auth.currentSessionOrNull() ?: return
        val userId = session.user?.id ?: return
        lifecycleScope.launch {
            runCatching {
                deleteRemoteNoteFolders(session.accessToken, userId, removableIds)
            }.onFailure { Log.e(TAG_MAIN, "Delete folders from Supabase failed", it) }
        }
    }

    private suspend fun deleteRemoteNoteFolders(
        authToken: String,
        userId: String,
        folderIds: Set<String>
    ) = withContext(Dispatchers.IO) {
        if (folderIds.isEmpty()) return@withContext
        val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
        val inList = folderIds.joinToString(",", "(", ")")
        val request = Request.Builder()
            .url(
                "$baseUrl/rest/v1/note_folders" +
                        "?user_id=eq.${urlEncode(userId)}" +
                        "&id=in.$inList"
            )
            .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $authToken")
            .addHeader("Prefer", "return=minimal")
            .delete()
            .build()

        notesHttpClient.newCall(request).execute().use { response ->
            val responseBody = runCatching { response.body.string() }.getOrDefault("")
            if (!response.isSuccessful) {
                throw IllegalStateException("Delete folders failed: ${response.code} $responseBody")
            }
        }
    }

    private fun installContactsFragment() {
        val container = contactsContainerView ?: return
        if (!container.isAttachedToWindow) {
            container.post { installContactsFragment() }
            return
        }

        val existing = supportFragmentManager
            .findFragmentById(R.id.main_contacts_container) as? AllContactsFragment

        if (existing?.view?.parent === container) {
            allContactsFragment = existing
            return
        }

        if (supportFragmentManager.isStateSaved) {
            container.post { installContactsFragment() }
            return
        }

        if (existing != null) {
            supportFragmentManager.beginTransaction()
                .remove(existing)
                .commitNowAllowingStateLoss()
        }

        allContactsFragment = AllContactsFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_contacts_container, allContactsFragment!!)
            .commitNowAllowingStateLoss()
    }

    fun hideContactsKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(contactsContainerView?.windowToken, 0)
        contactsContainerView?.clearFocus()
    }

    private fun openSettingsSearchSheet() {
        settingsSearchSheetVisible.value = true
        val dialog = BottomSheetDialog(this)
        fun px(dpValue: Int): Int = (dpValue * resources.displayMetrics.density).toInt()
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(px(22), px(18), px(22), px(24))
        }
        container.addView(
            TextView(this).apply {
                text = getString(R.string.search)
                textSize = 20f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding(px(4), 0, px(4), px(14))
            }
        )
        val searchInput = EditText(this).apply {
            hint = getString(R.string.search)
            isSingleLine = true
            setText(settingsSearchQuery.value)
            setSelection(text?.length ?: 0)
            setPadding(px(18), px(12), px(18), px(12))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = px(22).toFloat()
                setColor(android.graphics.Color.TRANSPARENT)
                setStroke(px(1), android.graphics.Color.argb(80, 127, 127, 127))
            }
        }
        container.addView(searchInput)
        dialog.setContentView(container)
        dialog.setOnDismissListener {
            settingsSearchQuery.value = ""
            settingsSearchSheetVisible.value = false
        }
        dialog.show()
        searchInput.requestFocus()
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                settingsSearchQuery.value = s?.toString().orEmpty()
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT)
    }

    @Composable
    private fun SettingsSearchGlassPanel(
        visible: Boolean,
        query: String,
        onQueryChange: (String) -> Unit,
        backdrop: LayerBackdrop,
        onDismiss: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        val isDark = isSystemInDarkTheme()
        val focusRequester = remember { FocusRequester() }
        val keyboard = LocalSoftwareKeyboardController.current
        val focusManager = LocalFocusManager.current
        val density = LocalDensity.current
        val keyboardOpen = WindowInsets.ime.getBottom(density) > 0
        var sawKeyboard by remember { mutableStateOf(false) }
        var revealPanel by remember { mutableStateOf(false) }
        val panelColor = if (isDark) Color(0xFF1B2730).copy(alpha = 0.78f) else Color(0xFFB9DFF2).copy(alpha = 0.90f)
        val capsuleColor = if (isDark) Color.White.copy(alpha = 0.13f) else Color(0xFFE4F5FF).copy(alpha = 0.82f)
        val contentColor = if (isDark) Color.White.copy(alpha = 0.94f) else Color(0xFF123B52)
        val hintColor = if (isDark) Color.White.copy(alpha = 0.56f) else Color(0xFF254B60).copy(alpha = 0.72f)

        LaunchedEffect(visible) {
            if (visible) {
                sawKeyboard = false
                revealPanel = false
                delay(24.milliseconds)
                runCatching { focusRequester.requestFocus() }
                keyboard?.show()
            } else {
                sawKeyboard = false
                revealPanel = false
            }
        }

        LaunchedEffect(visible, keyboardOpen) {
            if (!visible) return@LaunchedEffect
            if (keyboardOpen) {
                sawKeyboard = true
                delay(24.milliseconds)
                revealPanel = true
            } else if (sawKeyboard) {
                onDismiss()
            }
        }

        val panelAlpha by animateFloatAsState(
            targetValue = if (visible && revealPanel) 1f else 0f,
            animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing),
            label = "settingsSearchPanelAlpha"
        )
        val panelScale by animateFloatAsState(
            targetValue = if (visible && revealPanel) 1f else 0.96f,
            animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
            label = "settingsSearchPanelScale"
        )

        androidx.compose.animation.AnimatedVisibility(
            visible = visible,
            modifier = modifier,
            enter = fadeIn(animationSpec = tween(1)),
            exit = fadeOut(animationSpec = tween(120)) +
                    scaleOut(
                        targetScale = 0.98f,
                        animationSpec = tween(150, easing = FastOutSlowInEasing)
                    )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = panelAlpha
                        scaleX = panelScale
                        scaleY = panelScale
                    }
                    .clip(GlassChromeShape)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { GlassChromeShape },
                        highlight = null,
                        effects = {
                            vibrancy()
                            blur(4f.dp.toPx())
                            lens(18f.dp.toPx(), 54f.dp.toPx())
                        },
                        onDrawSurface = {
                            drawRect(panelColor)
                            drawRect(Color.White.copy(alpha = if (isDark) 0.05f else 0.12f))
                        }
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(ContinuousCapsule)
                        .background(capsuleColor)
                        .padding(start = 16.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(22.dp)
                    )
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = contentColor,
                            fontWeight = FontWeight.Medium
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                focusManager.clearFocus()
                                keyboard?.hide()
                                onDismiss()
                            }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                            .focusRequester(focusRequester),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (query.isBlank()) {
                                    Text(
                                        text = "Search settings",
                                        color = hintColor,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                    IconButton(
                        onClick = {
                            focusManager.clearFocus()
                            keyboard?.hide()
                            onDismiss()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close search",
                            tint = contentColor,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }

                Text(
                    text = if (query.isBlank()) {
                        "Type to filter Settings"
                    } else {
                        "Showing matching settings"
                    },
                    modifier = Modifier.padding(horizontal = 6.dp),
                    color = hintColor,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }

    @Composable
    private fun SettingsMainSearchButton(
        visible: Boolean,
        backdrop: LayerBackdrop,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        val isDark = isSystemInDarkTheme()
        val animationScope = rememberCoroutineScope()
        val interactiveHighlight = remember(animationScope, isDark) {
            InteractiveHighlight(
                animationScope = animationScope,
                highlightColor = if (isDark) Color.White else Color.Black
            )
        }
        val buttonColor = if (isDark) Color(0xFF59C9F8) else Color(0xFFB9DFF2)
        val iconColor = if (isDark) Color.White.copy(alpha = 0.96f) else Color(0xFF123B52)

        androidx.compose.animation.AnimatedVisibility(
            visible = visible,
            modifier = modifier,
            enter = fadeIn(animationSpec = tween(durationMillis = 140)) +
                    scaleIn(
                        initialScale = 0.88f,
                        animationSpec = spring(dampingRatio = 0.72f, stiffness = 460f)
                    ),
            exit = fadeOut(animationSpec = tween(durationMillis = 120)) +
                    scaleOut(
                        targetScale = 0.88f,
                        animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing)
                    )
        ) {
            Row(
                modifier = Modifier
                    .size(60.dp)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { ContinuousCapsule },
                        highlight = null,
                        effects = {
                            vibrancy()
                            blur(2f.dp.toPx())
                            lens(12f.dp.toPx(), 24f.dp.toPx())
                        },
                        layerBlock = {
                            val width = size.width
                            val height = size.height
                            val progress = interactiveHighlight.pressProgress
                            val scale = 1f + (6f.dp.toPx() / height) * progress

                            val maxOffset = size.minDimension
                            val offset = interactiveHighlight.offset
                            translationX = maxOffset * tanh(0.05f * offset.x / maxOffset)
                            translationY = maxOffset * tanh(0.05f * offset.y / maxOffset)

                            val maxDragScale = 6f.dp.toPx() / height
                            val offsetAngle = atan2(offset.y, offset.x)
                            scaleX =
                                scale +
                                        maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) *
                                        (width / height).fastCoerceAtMost(1f)
                            scaleY =
                                scale +
                                        maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) *
                                        (height / width).fastCoerceAtMost(1f)
                        },
                        onDrawSurface = {
                            val baseAlpha = if (isDark) 0.34f else 0.92f
                            drawRect(buttonColor.copy(alpha = baseAlpha))
                            drawRect(Color.White.copy(alpha = if (isDark) 0.08f else 0.16f))
                            drawRect(buttonColor.copy(alpha = baseAlpha * 0.36f * interactiveHighlight.pressProgress))
                        }
                    )
                    .clickable(
                        interactionSource = null,
                        indication = null,
                        role = Role.Button,
                        onClick = onClick
                    )
                    .then(interactiveHighlight.modifier)
                    .then(interactiveHighlight.gestureModifier),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = getString(R.string.search),
                    modifier = Modifier.size(24.dp),
                    tint = iconColor
                )
            }
        }
    }

    private fun shareApp() {
        val versionName = runCatching {
            packageManager.getPackageInfo(packageName, 0).versionName
        }.getOrNull().orEmpty()

        val shareText = """
Download JH Flight Studio:
$APP_SHARE_URL

Version: $versionName
""".trimIndent()

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }

        startActivity(Intent.createChooser(sendIntent, null))
    }

    private fun applyDebugLoginBypassIfRequested(): Int? {
        if (!BuildConfig.DEBUG) return null

        val data = intent.data
        val requestedByExtra = intent.getBooleanExtra(EXTRA_DEV_BYPASS_LOGIN, false)
        val requestedByLink = data?.scheme == DEV_BYPASS_SCHEME && data.host == DEV_BYPASS_HOST
        if (!requestedByExtra && !requestedByLink) return null

        UserPreferencesManager(this).apply {
            saveUserProfile(
                name = "Debug Tester",
                phone = "+13075550123",
                email = "debug@test.local",
                birthday = null,
                bio = "Local debug login bypass",
                selectedPhotoUri = null
            )
            loggedInUserId = "debug-local-user"
            profileThemeMode = 3
        }

        getSharedPreferences(MAIN_WELCOME_PREFS, MODE_PRIVATE).edit {
            putInt(MAIN_WELCOME_SEEN_VERSION, MAIN_WELCOME_VERSION)
        }

        return resolveDebugBypassPage(data?.getQueryParameter("page"))
    }

    private fun resolveDebugBypassPage(linkPage: String?): Int {
        val requestedPage = intent.getStringExtra(EXTRA_DEV_BYPASS_PAGE)
            ?: linkPage
            ?: when {
                intent.hasExtra(EXTRA_START_PAGE) -> return intent.getIntExtra(EXTRA_START_PAGE, PAGE_HOME)
                else -> "home"
            }

        return when (requestedPage.lowercase()) {
            "briefing", "updates", "contacts", "chat", "messages" -> PAGE_BRIEFING
            "notes" -> PAGE_NOTES
            "settings" -> PAGE_SETTINGS
            "profile", "login" -> PAGE_PROFILE
            else -> PAGE_HOME
        }
    }

    private fun resolveInitialMainPage(source: Intent?): Int {
        if (isPlainMainLaunch(source)) {
            return PAGE_HOME
        }
        return source
            ?.getIntExtra(EXTRA_START_PAGE, PAGE_HOME)
            ?.coerceIn(PAGE_HOME, PAGE_SETTINGS)
            ?: PAGE_HOME
    }

    private fun isPlainMainLaunch(source: Intent?): Boolean {
        if (source == null) {
            return true
        }
        if (source.hasExtra(EXTRA_START_PAGE) || source.hasExtra(EXTRA_DEV_BYPASS_PAGE) || source.data != null) {
            return false
        }
        val hasLauncherCategory = source.categories?.contains(Intent.CATEGORY_LAUNCHER) == true
        return source.action == null || source.action == Intent.ACTION_MAIN || hasLauncherCategory
    }

    /**
     * The main pager component that handles transitions between the application's top-level screens.
     *
     * @param currentBackdrop The global backdrop layer for frosted glass effects.
     * @param onOpenHome Callback to navigate to the Home screen.
     * @param onOpenContacts Callback to navigate to the Briefing/Updates screen.
     * @param onOpenNotes Callback to navigate to the Notes screen.
     * @param onOpenLiveCameras Callback to launch the Live Cameras activity.
     * @param onOpenAddNote Callback to launch the Add Note flow.
     * @param onHomeCameraExpandedChange Callback when the home camera layer expands or collapses.
     * @param onHomeCameraGestureActiveChange Callback during camera gesture interactions.
     * @param actuallyExitApp Callback to terminate the application.
     * @param triggerRefreshNow Callback to force refresh data.
     * @param currentPage The current active page index.
     * @param settingsFeedbackRequestToken A token to trigger feedback actions in the settings page.
     * @param onSettingsModalVisibleChange Callback when a settings modal is shown or hidden.
     */
    @Composable
    private fun MainPager(
        currentBackdrop: LayerBackdrop,
        onOpenHome: () -> Unit,
        onOpenContacts: () -> Unit,
        onOpenNotes: () -> Unit,
        onOpenLiveCameras: () -> Unit,
        onOpenAddNote: () -> Unit,
        onHomeCameraExpandedChange: (Boolean) -> Unit,
        onHomeCameraGestureActiveChange: (Boolean) -> Unit,
        actuallyExitApp: () -> Unit,
        triggerRefreshNow: (String?) -> Unit,
        currentPage: Int,
        settingsFeedbackRequestToken: Int,
        onSettingsModalVisibleChange: (Boolean) -> Unit
    ) {
        val tabStateHolder = rememberSaveableStateHolder()

        AnimatedContent(
            targetState = currentPage,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                val direction = if (targetState > initialState) 1 else -1
                (
                        fadeIn(animationSpec = tween(120)) +
                                scaleIn(initialScale = 0.985f, animationSpec = tween(170)) +
                                slideInHorizontally(animationSpec = tween(170)) { fullWidth ->
                                    direction * (fullWidth / 24)
                                }
                        ).togetherWith(
                        fadeOut(animationSpec = tween(90)) +
                                scaleOut(targetScale = 0.995f, animationSpec = tween(110)) +
                                slideOutHorizontally(animationSpec = tween(110)) { fullWidth ->
                                    -direction * (fullWidth / 30)
                                }
                    )
            },
            label = "mainTabContent"
        ) { page ->
            tabStateHolder.SaveableStateProvider(page) {
                Box(modifier = Modifier.fillMaxSize()) {
                    when (page) {
                        PAGE_HOME -> HomeScreenRouteContent(
                            backdrop = currentBackdrop,
                            triggerRefreshNow = triggerRefreshNow,
                            exitApp = actuallyExitApp,
                            openContactsPage = onOpenContacts,
                            openNotesPage = onOpenNotes,
                            onCameraExpandedChange = onHomeCameraExpandedChange,
                            onCameraGestureActiveChange = onHomeCameraGestureActiveChange
                        )
                        PAGE_BRIEFING -> BriefingPage(
                            active = true,
                            onOpenFlights = { openWebCard("card3") },
                            onOpenNews = { openWebCard("card2") },
                            onOpenFbo = { openWebCard("card4") },
                            onOpenWelcome = { openWebCard("card1") },
                            onOpenAbout = { openWebCard("about_us") },
                            onOpenContact = { openWebCard("contact_us") },
                            onOpenLiveCameras = onOpenLiveCameras,
                            onOpenNotes = onOpenNotes,
                            onOpenAddNote = onOpenAddNote
                        )
                        PAGE_NOTES -> NotesPage()
                        PAGE_SETTINGS -> SettingsPage(
                            onOpenHome = onOpenHome,
                            onOpenContacts = onOpenContacts,
                            onOpenNotes = onOpenNotes,
                            feedbackRequestToken = settingsFeedbackRequestToken,
                            onModalVisibleChange = onSettingsModalVisibleChange
                        )
                    }
                }
            }
        }
    }

    private fun openWebCard(cardId: String) {
        startActivity(
            Intent(this, WebviewflightActivity::class.java)
                .putExtra("start_card", cardId)
        )
        overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation)
    }

    private fun emergencyPageName(page: Int): String {
        return when (page) {
            PAGE_BRIEFING -> "briefing"
            PAGE_NOTES -> "notes"
            PAGE_SETTINGS -> "settings"
            else -> "home"
        }
    }

    private fun openEmergencyAction(actionUrl: String) {
        val normalized = actionUrl.trim()
        if (normalized.isBlank()) return

        when (normalized.lowercase(Locale.US)) {
            "app://flights", "app://flight-status", "app://flight_status", "jhairtracker://flights" -> {
                openWebCard("card3")
                return
            }
            "app://briefing", "jhairtracker://briefing" -> {
                openRequestedMainPage?.invoke(PAGE_BRIEFING)
                return
            }
            "app://notes", "jhairtracker://notes" -> {
                openRequestedMainPage?.invoke(PAGE_NOTES)
                return
            }
            "app://settings", "jhairtracker://settings" -> {
                openRequestedMainPage?.invoke(PAGE_SETTINGS)
                return
            }
            "app://updates", "app://software-update", "jhairtracker://updates" -> {
                startActivity(Intent(this, SoftwareUpdateActivity::class.java))
                overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation)
                return
            }
            "app://live-cameras", "app://cameras", "jhairtracker://live-cameras" -> {
                val ts = System.currentTimeMillis()
                startActivity(
                    LiveCamerasActivity.intent(
                        this,
                        listOf(
                            CameraCard(
                                "Curb",
                                "https://www.jacksonholeairport.com/wp-content/uploads/webcams/parking-curb.jpg?v=$ts"
                            ),
                            CameraCard(
                                "North",
                                "https://www.jacksonholeairport.com/wp-content/uploads/webcams/parking-north.jpg?v=$ts"
                            ),
                            CameraCard(
                                "South",
                                "https://www.jacksonholeairport.com/wp-content/uploads/webcams/parking-south.jpg?v=$ts"
                            )
                        )
                    )
                )
                overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation)
                return
            }
        }

        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, normalized.toUri()))
        }.onFailure {
            Toast.makeText(this, "Unable to open link", Toast.LENGTH_SHORT).show()
        }
    }

    @Composable
    private fun SettingsPage(
        onOpenHome: () -> Unit,
        onOpenContacts: () -> Unit,
        onOpenNotes: () -> Unit,
        feedbackRequestToken: Int,
        onModalVisibleChange: (Boolean) -> Unit
    ) {
        ModernSettingsScreen(
            searchQuery = settingsSearchQuery.value,
            onOpenHome = onOpenHome,
            onOpenSoftwareUpdate = {
                startActivity(Intent(this, SoftwareUpdateActivity::class.java))
                overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation)
            },
            onOpenAppIcon = {
                startActivity(Intent(this, AppIconPickerActivity::class.java))
                overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation)
            },
            onOpenLiquidGlass = {
                startActivity(Intent(this, LiquidGlassSettingsActivity::class.java))
                overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation)
            },
            onOpenNotifications = {
                SignUpBottomSheetDialogFragment().show(supportFragmentManager, "SignUpBottomSheet")
            },
            onOpenCardDrawer = { cardId ->
                startActivity(
                    Intent(this, WebviewflightActivity::class.java)
                        .putExtra("start_card", cardId)
                )
                overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation)
            },
            onOpenNotes = onOpenNotes,
            onOpenContacts = onOpenContacts,
            onShareApp = ::shareApp,
            onOpenSearch = { openSettingsSearchSheet() },
            onOpenQrCode = {
                startActivity(
                    Intent(this, QRCodeComposeActivity::class.java),
                    ActivityOptionsCompat.makeSceneTransitionAnimation(this).toBundle()
                )
            },
            onOpenProfile = {
                startActivity(
                    Intent(this, ProfileDetailsComposeActivity::class.java),
                    ActivityOptionsCompat.makeSceneTransitionAnimation(this).toBundle()
                )
            },
            showBottomChrome = false,
            modalBottomPadding = GlassChromeHorizontalPadding,
            feedbackRequestToken = feedbackRequestToken,
            onModalVisibleChange = onModalVisibleChange
        )
    }

    /**
     * Displays the Briefing page, which provides AI-generated summaries of trip details,
     * flight statuses, weather updates, and quick actions.
     *
     * @param active Whether the page is currently active.
     * @param onOpenFlights Callback to open flight tracking.
     * @param onOpenNews Callback to open airport news.
     * @param onOpenFbo Callback to open FBO services.
     * @param onOpenWelcome Callback to open the welcome guide.
     * @param onOpenAbout Callback to open airport information.
     * @param onOpenContact Callback to open airport contact help.
     * @param onOpenLiveCameras Callback to view airport cameras.
     * @param onOpenNotes Callback to navigate to the Notes screen.
     * @param onOpenAddNote Callback to quickly add a new note.
     */
    @Suppress("SameParameterValue")
    @Composable
    private fun BriefingPage(
        active: Boolean,
        onOpenFlights: () -> Unit,
        onOpenNews: () -> Unit,
        onOpenFbo: () -> Unit,
        onOpenWelcome: () -> Unit,
        onOpenAbout: () -> Unit,
        onOpenContact: () -> Unit,
        onOpenLiveCameras: () -> Unit,
        onOpenNotes: () -> Unit,
        onOpenAddNote: () -> Unit
    ) {
        if (active) {
            val isDark = isSystemInDarkTheme()
            val appThemePalette = LocalAppThemePalette.current
            val surfaceRoles = appThemeSurfaceRoles(appThemePalette, isDark)
            val palette = rememberBriefingPalette(isDark, appThemePalette)
            val pageColor = palette.page
            val textColor = palette.text
            val subTextColor = palette.subText
            val aiCardColor = palette.aiCard
            val cardBorder = palette.border
            val accentColor = palette.accent
            val context = LocalContext.current
            val briefingCalendar = remember { Calendar.getInstance(TimeZone.getTimeZone("America/Denver")) }
            val briefingHour = remember { briefingCalendar.get(Calendar.HOUR_OF_DAY) }
            val briefingMessageSlot = remember {
                briefingCalendar.get(Calendar.DAY_OF_YEAR) * 72 +
                        briefingHour * 3 +
                        briefingCalendar.get(Calendar.MINUTE) / 20
            }
            val briefingGreeting = remember(briefingHour) { briefingGreetingForHour(briefingHour) }
            val briefingGreetingTitle = remember(briefingHour) { briefingGreetingTitleForHour(briefingHour) }
            val briefingFriendlyMessage = remember(briefingHour, briefingMessageSlot) {
                briefingFriendlyMessageForHour(briefingHour, briefingMessageSlot)
            }
            val briefingDayPart = remember(briefingHour) { briefingDayPartForHour(briefingHour) }
            val currentNoteCount = notesCount.intValue
            val webTheme = SettingsStore.webTheme(context)
            val webTextZoom = SettingsStore.textZoom(context)
            val groupFlights = SettingsStore.groupFlights(context)
            val highContrastWeb = SettingsStore.highContrastWeb(context)
            val cachePages = SettingsStore.cachePages(context)
            val blockTrackers = SettingsStore.blockTrackers(context)
            val reduceWebMotion = SettingsStore.reduceWebMotion(context)
            val briefingWeatherEnabled = SettingsStore.briefingWeatherEnabled(context)
            var flightBriefSnapshotJson by remember(context) {
                mutableStateOf(SettingsStore.flightBriefSnapshot(context))
            }
            val flightBriefSnapshot = remember(flightBriefSnapshotJson) {
                parseBriefingFlightSnapshot(flightBriefSnapshotJson)
            }
            var briefingWeatherJson by remember(context) {
                mutableStateOf(SettingsStore.briefingWeatherSnapshot(context))
            }
            LaunchedEffect(true, briefingWeatherEnabled) {
                var lastNativeRefreshAt = 0L
                while (true) {
                    val now = System.currentTimeMillis()
                    if (lastNativeRefreshAt == 0L || now - lastNativeRefreshAt >= 180_000L) {
                        launch {
                            BriefingFlightRepository.refresh(context)
                            val latestFlight = SettingsStore.flightBriefSnapshot(context)
                            if (briefingFlightDisplayKey(latestFlight) != briefingFlightDisplayKey(flightBriefSnapshotJson)) {
                                flightBriefSnapshotJson = latestFlight
                            }
                        }
                        if (briefingWeatherEnabled) {
                            launch {
                                BriefingWeatherRepository.refresh(context)
                                val latestWeather = SettingsStore.briefingWeatherSnapshot(context)
                                if (briefingWeatherDisplayKey(latestWeather) != briefingWeatherDisplayKey(briefingWeatherJson)) {
                                    briefingWeatherJson = latestWeather
                                }
                            }
                        }
                        lastNativeRefreshAt = now
                    }
                    val latestFlight = SettingsStore.flightBriefSnapshot(context)
                    if (briefingFlightDisplayKey(latestFlight) != briefingFlightDisplayKey(flightBriefSnapshotJson)) {
                        flightBriefSnapshotJson = latestFlight
                    }
                    if (briefingWeatherEnabled) {
                        val latestWeather = SettingsStore.briefingWeatherSnapshot(context)
                        if (briefingWeatherDisplayKey(latestWeather) != briefingWeatherDisplayKey(briefingWeatherJson)) {
                            briefingWeatherJson = latestWeather
                        }
                    }
                    delay(3_000L.milliseconds)
                }
            }
            val briefingWeather = remember(briefingWeatherJson, briefingWeatherEnabled) {
                val snapshot = par9yMnTm4NSzvG9rrwjM2ec8xZgh1cafXH8(briefingWeatherJson)
                if (briefingWeatherEnabled && isLiveBriefingWeatherSnapshot(snapshot)) snapshot else BriefingWeatherSnapshot()
            }
            val briefingWeatherConditionForBrief = remember(
                briefingWeatherEnabled,
                briefingWeather.temp,
                briefingWeather.condition,
                briefingWeather.summary
            ) {
                if (briefingWeatherEnabled && briefingWeather.temp.isNotBlank()) {
                    resolvedBriefingWeatherCondition(briefingWeather)
                } else {
                    ""
                }
            }
            val noteSignal = noteRows.joinToString("|") {
                "${it.title}:${it.text.take(90)}:${it.imagesCount}:${it.attachmentsCount}:${it.audioCount}:${it.videoCount}:${it.hasReminder}:${it.hasBadge}"
            }
            val briefingNoteContext = remember(noteSignal) {
                noteRows.take(3).map {
                    BriefingNoteContext(
                        title = it.title,
                        text = it.text
                    )
                }
            }
            val briefingAppContext = remember(
                noteSignal,
                currentNoteCount,
                contactsChromeCount.intValue,
                contactsAlphabeticalMode.value,
                webTheme,
                webTextZoom,
                groupFlights,
                highContrastWeb,
                cachePages,
                blockTrackers,
                reduceWebMotion,
                briefingWeatherEnabled,
                flightBriefSnapshot.summary,
                flightBriefSnapshot.issueCount,
                flightBriefSnapshot.issues.joinToString("|") { it.label + it.flight },
                briefingWeather.temp,
                briefingWeatherConditionForBrief,
                briefingWeather.summary,
                briefingGreeting,
                briefingDayPart
            ) {
                BriefingAppContext(
                    greeting = briefingGreeting,
                    dayPart = briefingDayPart,
                    reminderCount = noteRows.count { it.hasReminder },
                    badgeCount = noteRows.count { it.hasBadge },
                    imageNoteCount = noteRows.count { it.imagesCount > 0 },
                    contactsCount = contactsChromeCount.intValue,
                    contactsSort = if (contactsAlphabeticalMode.value) "alphabetical" else "recent",
                    webTheme = webTheme,
                    webTextZoom = webTextZoom,
                    groupFlights = groupFlights,
                    highContrastWeb = highContrastWeb,
                    cachePages = cachePages,
                    blockTrackers = blockTrackers,
                    reduceWebMotion = reduceWebMotion,
                    flightSummary = flightBriefSnapshot.summary,
                    flightIssueCount = flightBriefSnapshot.issueCount,
                    flightIssueCards = flightBriefSnapshot.issues,
                    weatherSummary = listOf(
                        briefingWeather.temp,
                        briefingWeatherConditionForBrief
                            .takeIf { it.isNotBlank() }
                            ?.let { briefingWeatherConditionLabel(it) }
                            .orEmpty(),
                        briefingWeather.summary
                    ).filter { it.isNotBlank() }.joinToString(" • ")
                )
            }
            val aiContentKey = "$currentNoteCount|$noteSignal|${briefingAppContext.cacheKey}"
            val shouldUseAi = true
            val fallbackBrief = remember(aiContentKey) {
                briefingAiFallback(currentNoteCount, briefingNoteContext, briefingAppContext)
            }
            var cachedAiContentKey by rememberSaveable { mutableStateOf<String?>(null) }
            var aiBrief by rememberSaveable { mutableStateOf("") }
            var aiCaption by rememberSaveable { mutableStateOf("AI Brief") }
            LaunchedEffect(aiContentKey, shouldUseAi) {
                if (aiBrief.isBlank()) {
                    aiBrief = fallbackBrief
                    aiCaption = "AI Brief"
                }
                if (!shouldUseAi) {
                    aiBrief = fallbackBrief
                    aiCaption = "AI Brief"
                    cachedAiContentKey = aiContentKey
                    return@LaunchedEffect
                }

                if (cachedAiContentKey == aiContentKey && aiBrief.isNotBlank()) {
                    return@LaunchedEffect
                }

                val generated = BriefingAiService.generateBrief(
                    noteCount = currentNoteCount,
                    recentNotes = briefingNoteContext,
                    appContext = briefingAppContext
                )
                val nextBrief = generated.ifBlank { fallbackBrief }
                if (nextBrief != aiBrief) {
                    aiBrief = nextBrief
                }
                aiCaption = if (generated.isBlank()) "App brief" else "AI Brief"
                cachedAiContentKey = aiContentKey
            }
            val briefingInsight = remember(aiBrief, fallbackBrief, briefingAppContext, flightBriefSnapshot, briefingWeather, briefingWeatherEnabled) {
                buildBriefingInsight(
                    noteCount = currentNoteCount,
                    appContext = briefingAppContext,
                    flightSnapshot = flightBriefSnapshot,
                    weather = briefingWeather,
                    weatherEnabled = briefingWeatherEnabled,
                    aiSentence = aiBrief.ifBlank { fallbackBrief }
                )
            }
            val smartBriefingCopy = remember(
                briefingGreetingTitle,
                briefingFriendlyMessage,
                aiCaption,
                currentNoteCount,
                briefingAppContext,
                flightBriefSnapshot,
                briefingWeather,
                briefingWeatherEnabled
            ) {
                buildBriefingSmartCopy(
                    noteCount = currentNoteCount,
                    appContext = briefingAppContext,
                    flightSnapshot = flightBriefSnapshot,
                    weather = briefingWeather,
                    weatherEnabled = briefingWeatherEnabled,
                    defaultCaption = if (aiCaption == "AI Brief") "Today at JAC" else aiCaption,
                    defaultGreetingTitle = briefingGreetingTitle,
                    defaultFriendlyMessage = briefingFriendlyMessage
                )
            }
            var lastAiBriefAnimationKey by rememberSaveable { mutableStateOf<String?>(null) }
            val aiBriefAnimationKey = if (aiBrief.isNotBlank()) {
                aiContentKey
            } else {
                ""
            }
            val shouldAnimateAiBrief = aiBriefAnimationKey.isNotBlank() &&
                    lastAiBriefAnimationKey != aiBriefAnimationKey
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(pageColor)
            ) {
                ProfileBackdropImageLayer(
                    modifier = Modifier.fillMaxSize(),
                    lightRes = R.drawable.light_grid_pattern,
                    darkRes = R.drawable.dark_grid_pattern,
                    imageAlpha = if (isDark) 0.72f else 0.42f,
                    scrimDark = 0.04f,
                    scrimLight = 0.00f
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    surfaceRoles.page.copy(alpha = if (isDark) 0.58f else 0.36f),
                                    surfaceRoles.glassCard.copy(alpha = if (isDark) 0.44f else 0.34f),
                                    appThemePalette.surfaceVariant.copy(alpha = if (isDark) 0.34f else 0.28f)
                                ),
                                start = Offset.Zero,
                                end = Offset(900f, 1350f)
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                        .padding(start = 8.dp, end = 8.dp, top = 112.dp, bottom = 92.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BriefingSmartCard(
                        title = "Briefing",
                        insight = briefingInsight,
                        caption = smartBriefingCopy.caption,
                        cardColor = aiCardColor,
                        accentColor = accentColor,
                        textColor = textColor,
                        subTextColor = subTextColor,
                        greetingTitle = smartBriefingCopy.greetingTitle,
                        friendlyMessage = smartBriefingCopy.friendlyMessage,
                        weather = briefingWeather,
                        weatherEnabled = briefingWeatherEnabled,
                        fallbackCondition = briefingFallbackWeatherCondition(briefingHour),
                        flightIssueCards = briefingAppContext.flightIssueCards,
                        animationKey = aiBriefAnimationKey,
                        animateEffects = shouldAnimateAiBrief,
                        onInsightAction = { action ->
                            when (action) {
                                BriefingInsightAction.Flights -> onOpenFlights()
                                BriefingInsightAction.Cameras -> onOpenLiveCameras()
                                BriefingInsightAction.Notes -> onOpenNotes()
                                BriefingInsightAction.QuickNote -> onOpenAddNote()
                                BriefingInsightAction.Fbo -> onOpenFbo()
                            }
                        },
                        onEffectsStarted = {
                            if (aiBriefAnimationKey.isNotBlank()) {
                                lastAiBriefAnimationKey = aiBriefAnimationKey
                            }
                        }
                    )

                    BriefingActionGroup(
                        title = "Airport",
                        actions = listOf(
                            BriefingActionEntry(
                                title = "Flights",
                                body = "See arrivals, departures, delays, and cancellations before you leave.",
                                icon = Icons.Filled.Flight,
                                onClick = onOpenFlights
                            ),
                            BriefingActionEntry(
                                title = "Live cameras",
                                body = "Look at the curb, north lot, and south lot cameras before pickup or parking.",
                                icon = Icons.Filled.Info,
                                onClick = onOpenLiveCameras
                            ),
                            BriefingActionEntry(
                                title = "FBO services",
                                body = "Open Jackson Hole Flight Services for private aviation details.",
                                icon = Icons.Filled.Flight,
                                onClick = onOpenFbo
                            ),
                            BriefingActionEntry(
                                title = "News",
                                body = "Read airport news, notices, and travel updates from JAC.",
                                icon = Icons.Filled.Info,
                                onClick = onOpenNews
                            )
                        ),
                        accentColor = accentColor,
                        textColor = textColor,
                        subTextColor = subTextColor,
                        borderColor = cardBorder
                    )

                    BriefingActionGroup(
                        title = "Your Trip",
                        actions = listOf(
                            BriefingActionEntry(
                                title = "Notes",
                                body = "Open your saved notes, reminders, photos, files, and trip details.",
                                icon = Icons.AutoMirrored.Filled.Article,
                                onClick = onOpenNotes
                            ),
                            BriefingActionEntry(
                                title = "Quick note",
                                body = "Save a flight number, pickup plan, parking spot, or reminder quickly.",
                                icon = Icons.Filled.Add,
                                onClick = onOpenAddNote
                            )
                        ),
                        accentColor = accentColor,
                        textColor = textColor,
                        subTextColor = subTextColor,
                        borderColor = cardBorder
                    )

                    BriefingActionGroup(
                        title = "Airport Info",
                        actions = listOf(
                            BriefingActionEntry(
                                title = "Welcome",
                                body = "Open the welcome guide and see what this app can help with.",
                                icon = Icons.Filled.Info,
                                onClick = onOpenWelcome
                            ),
                            BriefingActionEntry(
                                title = "About airport",
                                body = "Read useful JAC details, airport history, and pilot information.",
                                icon = Icons.Filled.Info,
                                onClick = onOpenAbout
                            ),
                            BriefingActionEntry(
                                title = "Airport help",
                                body = "Find official airport help when you need the right place to start.",
                                icon = Icons.Filled.Info,
                                onClick = onOpenContact
                            )
                        ),
                        accentColor = accentColor,
                        textColor = textColor,
                        subTextColor = subTextColor,
                        borderColor = cardBorder
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF101923))
            )
        }
    }

    private data class BriefingPalette(
        val page: Color,
        val card: Color,
        val aiCard: Color,
        val border: Color,
        val text: Color,
        val subText: Color,
        val accent: Color,
        val warmAccent: Color
    )

    @Composable
    private fun rememberBriefingPalette(
        isDark: Boolean,
        appPalette: AppThemePalette
    ): BriefingPalette {
        return remember(isDark, appPalette) {
            val surfaceRoles = appThemeSurfaceRoles(appPalette, isDark)
            if (isDark) {
                BriefingPalette(
                    page = surfaceRoles.page,
                    card = surfaceRoles.card,
                    aiCard = surfaceRoles.glassCard,
                    border = surfaceRoles.border,
                    text = surfaceRoles.title,
                    subText = surfaceRoles.subtitle,
                    accent = appPalette.accent,
                    warmAccent = appPalette.warm
                )
            } else {
                // Briefing uses its own high-contrast light palette. The app accent can be
                // very pale in some presets, which made chips, icons and cards disappear.
                BriefingPalette(
                    page = Color(0xFFF8FAFF),
                    card = Color(0xFFFFFFFF),
                    aiCard = Color(0xFFFCFBFF),
                    border = Color(0xFFC9D2E3),
                    text = Color(0xFF111827),
                    subText = Color(0xFF4B5565),
                    accent = Color(0xFF5B46D6),
                    warmAccent = Color(0xFFB45309)
                )
            }
        }
    }

    @Composable
    private fun Modifier.briefingElasticAppear(
        key: Any,
        delayMillis: Long = 0L
    ): Modifier {
        var visible by remember(key) { mutableStateOf(false) }
        LaunchedEffect(key) {
            visible = false
            if (delayMillis > 0L) delay(delayMillis.milliseconds)
            visible = true
        }
        val scale by animateFloatAsState(
            targetValue = if (visible) 1f else 0.965f,
            animationSpec = spring(dampingRatio = 0.72f, stiffness = 420f),
            label = "briefingElasticScale"
        )
        val alpha by animateFloatAsState(
            targetValue = if (visible) 1f else 0f,
            animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
            label = "briefingElasticAlpha"
        )
        return graphicsLayer {
            this.alpha = alpha
            scaleX = scale
            scaleY = scale
            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.08f)
        }
    }

    @Composable
    private fun BriefingGlassTopAppBar(
        backdrop: LayerBackdrop,
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
                    text = BriefingLabels.AREA_NAME,
                    modifier = Modifier.weight(1f),
                    color = contentColor,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1
                )
            }
        }
    }

    @Composable
    private fun BriefingSectionTitle(title: String) {
        Text(
            text = title,
            color = if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.78f) else Color(0xFF4E5965),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
        )
    }

    @Composable
    private fun BriefingSmartCardBaseGlow(
        accentColor: Color,
        modifier: Modifier = Modifier
    ) {
        val isDark = isSystemInDarkTheme()
        Canvas(modifier = modifier) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        accentColor.copy(alpha = if (isDark) 0.12f else 0.08f),
                        Color.Transparent,
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = size.height * 0.72f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = if (isDark) 0.20f else 0.12f),
                        accentColor.copy(alpha = if (isDark) 0.08f else 0.045f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.12f, size.height * 0.04f),
                    radius = size.maxDimension * 0.62f
                ),
                radius = size.maxDimension * 0.62f,
                center = Offset(size.width * 0.12f, size.height * 0.04f)
            )
        }
    }

    private class BriefingMaterialPathShape(
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

    @Composable
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    private fun BriefingSparkleBadge(
        modifier: Modifier = Modifier
    ) {
        val isDark = isSystemInDarkTheme()

        var showLoader by remember { mutableStateOf(true) }
        var revealLottie by remember { mutableStateOf(false) }

        /*
         * The file is located at:
         * app/src/main/assets/aibrief.json
         *
         * Only the asset filename is used here.
         */
        val lottieComposition by rememberLottieComposition(
            LottieCompositionSpec.Asset("aibrief.json")
        )

        val lottieProgress by animateLottieCompositionAsState(
            composition = lottieComposition,
            isPlaying = revealLottie,
            iterations = LottieConstants.IterateForever,
            speed = 0.3f,
            restartOnPlay = false
        )

        LaunchedEffect(Unit) {
            // Show the rotating Material shapes first.
            delay(3000L.milliseconds)

            // Crossfade directly into the Lottie animation.
            revealLottie = true
            showLoader = false
        }

        val loaderAlpha by animateFloatAsState(
            targetValue = if (showLoader) 1f else 0f,
            animationSpec = tween(
                durationMillis = 500,
                easing = FastOutSlowInEasing
            ),
            label = "briefingLoaderFadeOut"
        )

        val lottieAlpha by animateFloatAsState(
            targetValue = if (revealLottie) 1f else 0f,
            animationSpec = tween(
                durationMillis = 500,
                easing = FastOutSlowInEasing
            ),
            label = "briefingLottieFadeIn"
        )

        val transition = rememberInfiniteTransition(
            label = "briefingSparkleBadge"
        )

        /*
         * Rotates the initial Material 3 morphing shapes.
         * 5200 ms makes the rotation slower and smoother.
         */
        val loaderRotation by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 5200,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "briefingLoaderRotation"
        )

        /*
         * Changes the Material loader color while it is visible:
         * gold -> cyan -> lavender -> gold.
         */
        val loaderColorPhase by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 6000,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "briefingLoaderColorPhase"
        )

        val loaderGold = if (isDark) {
            Color(0xFFFFE082)
        } else {
            Color(0xFFFFC107)
        }

        val loaderCyan = if (isDark) {
            Color(0xFF67E8F9)
        } else {
            Color(0xFF00A7C4)
        }

        val loaderLavender = if (isDark) {
            Color(0xFFC4B5FD)
        } else {
            Color(0xFFA78BFA)
        }

        val loaderColor = when {
            loaderColorPhase < 0.33f -> {
                colorLerp(
                    loaderGold,
                    loaderCyan,
                    loaderColorPhase / 0.33f
                )
            }

            loaderColorPhase < 0.66f -> {
                colorLerp(
                    loaderCyan,
                    loaderLavender,
                    (loaderColorPhase - 0.33f) / 0.33f
                )
            }

            else -> {
                colorLerp(
                    loaderLavender,
                    loaderGold,
                    (loaderColorPhase - 0.66f) / 0.34f
                )
            }
        }

        val cookiePath = MaterialShapes.Cookie7Sided.toPath()

        val cookieShape = remember(cookiePath) {
            BriefingMaterialPathShape(cookiePath)
        }

        val containerColor = if (isDark) {
            Color(0xFF30206B)
        } else {
            Color(0xFF5B4BEA)
        }

        Box(
            modifier = modifier
                .clip(cookieShape)
                .background(containerColor)
                .border(
                    width = 1.dp,
                    color = loaderColor.copy(
                        alpha = if (isDark) 0.38f else 0.46f
                    ),
                    shape = cookieShape
                ),
            contentAlignment = Alignment.Center
        ) {
            /*
             * Initial Material 3 morphing shapes.
             *
             * They:
             * - morph automatically
             * - rotate slowly
             * - change color
             * - remain full size
             * - fade out after 3 seconds
             */
            LoadingIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(38.dp)
                    .graphicsLayer {
                        alpha = loaderAlpha
                        rotationZ = loaderRotation

                        // Never zoom out.
                        scaleX = 1f
                        scaleY = 1f
                    },
                color = loaderColor
            )

            /*
             * Final AI Lottie animation.
             *
             * It:
             * - replaces the previous star icons
             * - does not receive extra rotation
             * - does not receive extra zoom
             * - follows the animation stored in aibrief.json
             * - stays and loops continuously
             */
            LottieAnimation(
                composition = lottieComposition,
                progress = { lottieProgress },
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(42.dp)
                    .graphicsLayer {
                        alpha = lottieAlpha
                    }
            )
        }
    }

    private fun briefingAiFallback(
        noteCount: Int,
        recentNotes: List<BriefingNoteContext>,
        appContext: BriefingAppContext
    ): String {
        val latest = recentNotes.firstOrNull()
        if (appContext.badgeCount > 0) {
            return "You have ${appContext.badgeCount} reminder ${if (appContext.badgeCount == 1) "badge" else "badges"} waiting. Review Notes first so nothing important gets missed."
        }
        if (appContext.flightIssueCount > 0 && appContext.flightSummary.isNotBlank()) {
            val summary = appContext.flightSummary.trim().trimEnd('.')
            return "$summary. Open Flights to see what changed."
        }
        if (appContext.flightSummary.contains("unavailable", ignoreCase = true) ||
            appContext.flightSummary.contains("not readable", ignoreCase = true)
        ) {
            return appContext.flightSummary
        }
        if (appContext.reminderCount > 0) {
            return "You have ${appContext.reminderCount} note ${if (appContext.reminderCount == 1) "reminder" else "reminders"} set. Review Notes, then check flights or cameras if your plan depends on them."
        }
        if (appContext.contactsCount > 0 && noteCount <= 0) {
            return "${appContext.contactsCount} airport contacts are ready if you need help, services, or a quick airport contact."
        }
        if (appContext.groupFlights || appContext.highContrastWeb) {
            return "Your flight board is ready. Open Flights for arrival times, departure times, delays, and cancellations."
        }
        if (latest != null) {
            val label = latest.title.ifBlank { latest.text }
                .replace(Regex("\\s+"), " ")
                .trim()
                .take(64)
            return "Latest note: $label. Open Notes when you want to continue from where you left off."
        }

        return when {
            noteCount <= 0 -> "Flights, live cameras, airport services, and quick notes are ready when you need them."
            noteCount == 1 -> "You have 1 saved note. Open it when you need that trip detail again."
            else -> "You have $noteCount saved notes. Review them when your travel details need a quick check."
        }
    }

    private enum class BriefingInsightAction {
        Flights,
        Cameras,
        Notes,
        QuickNote,
        Fbo
    }

    private data class BriefingInsightChip(
        val value: String,
        val label: String,
        val tone: String = "normal"
    )

    private data class BriefingInsight(
        val title: String,
        val body: String,
        val actionLabel: String,
        val action: BriefingInsightAction,
        val chips: List<BriefingInsightChip>
    )

    private data class BriefingSmartCopy(
        val caption: String,
        val greetingTitle: String,
        val friendlyMessage: String
    )

    private fun buildBriefingSmartCopy(
        noteCount: Int,
        appContext: BriefingAppContext,
        flightSnapshot: BriefingFlightSnapshot,
        weather: BriefingWeatherSnapshot,
        weatherEnabled: Boolean,
        defaultCaption: String,
        defaultGreetingTitle: String,
        defaultFriendlyMessage: String
    ): BriefingSmartCopy {
        val firstIssue = flightSnapshot.issues.firstOrNull()
        val issueText = listOfNotNull(firstIssue?.label, firstIssue?.tone, firstIssue?.flight)
            .joinToString(" ")
            .lowercase()
        val criticalCount = flightSnapshot.cancelledCount + flightSnapshot.divertedCount
        val hasCancelled = criticalCount > 0 || issueText.contains("cancel")
        val hasDiverted = flightSnapshot.divertedCount > 0 || issueText.contains("divert")
        val hasDelayed = flightSnapshot.delayedCount > 0 || issueText.contains("delay")
        val condition = if (weatherEnabled && weather.temp.isNotBlank()) {
            resolvedBriefingWeatherCondition(weather)
        } else {
            appContext.dayPart
        }
        val weatherLabel = briefingWeatherConditionLabel(briefingWeatherVisualCondition(condition))
        val weatherNeedsAttention = condition in setOf("thunder", "rain_heavy", "hail", "mix", "snow", "rain", "fog")
        val flightLabel = firstIssue?.flight?.takeIf { it.isNotBlank() } ?: "a flight"
        val routeLabel = firstIssue?.route?.takeIf { it.isNotBlank() }
        val timeLabel = firstIssue?.time?.takeIf { it.isNotBlank() }
        val issueDetail = listOfNotNull(
            routeLabel,
            timeLabel?.let { "around $it" }
        ).joinToString(" ")
        return when {
            hasCancelled -> BriefingSmartCopy(
                caption = "Flight change",
                greetingTitle = "Check $flightLabel first",
                friendlyMessage = "There is a cancellation showing${issueDetail.takeIf { it.isNotBlank() }?.let { " for $it" }.orEmpty()}. Open Flights before you leave so the plan matches the live board."
            )
            hasDiverted -> BriefingSmartCopy(
                caption = "Flight change",
                greetingTitle = "A diverted flight needs a look",
                friendlyMessage = "$flightLabel is marked diverted${issueDetail.takeIf { it.isNotBlank() }?.let { " for $it" }.orEmpty()}. Open Flights and check the current row before making a pickup or airport run."
            )
            hasDelayed -> BriefingSmartCopy(
                caption = "Delay watch",
                greetingTitle = "Delay showing at JAC",
                friendlyMessage = "${flightSnapshot.delayedCount.coerceAtLeast(1)} delay ${if (flightSnapshot.delayedCount == 1) "is" else "are"} showing. Open Flights for the latest scheduled and actual times."
            )
            weatherNeedsAttention -> BriefingSmartCopy(
                caption = "Weather check",
                greetingTitle = "$weatherLabel at Jackson",
                friendlyMessage = "Weather may matter right now. Check the live cameras before driving, pickup, parking, or heading to the terminal."
            )
            appContext.badgeCount > 0 || appContext.reminderCount > 0 -> {
                val count = (appContext.badgeCount.takeIf { it > 0 } ?: appContext.reminderCount).coerceAtLeast(1)
                BriefingSmartCopy(
                    caption = "Notes ready",
                    greetingTitle = "You have $count reminder ${if (count == 1) "note" else "notes"}",
                    friendlyMessage = "Open Notes first if your trip depends on a saved photo, file, audio clip, checklist, or reminder."
                )
            }
            noteCount <= 0 -> BriefingSmartCopy(
                caption = "Start here",
                greetingTitle = "Set up your trip",
                friendlyMessage = "Save a flight number, parking detail, pickup note, photo, file, or voice note so it is ready when you come back."
            )
            appContext.imageNoteCount > 0 -> BriefingSmartCopy(
                caption = "Notes with media",
                greetingTitle = "Your saved media is ready",
                friendlyMessage = "Photos and files are attached to your notes. Open Notes when you need the original detail again."
            )
            else -> BriefingSmartCopy(
                caption = defaultCaption,
                greetingTitle = defaultGreetingTitle,
                friendlyMessage = defaultFriendlyMessage
            )
        }
    }

    private fun buildBriefingInsight(
        noteCount: Int,
        appContext: BriefingAppContext,
        flightSnapshot: BriefingFlightSnapshot,
        weather: BriefingWeatherSnapshot,
        weatherEnabled: Boolean,
        aiSentence: String
    ): BriefingInsight {
        val arrivalCount = flightSnapshot.arrivalCount.takeIf { it > 0 }
            ?: countFromSummary(flightSnapshot.summary, "arrival")
        val departureCount = flightSnapshot.departureCount.takeIf { it > 0 }
            ?: countFromSummary(flightSnapshot.summary, "departure")
        val delayedCount = flightSnapshot.delayedCount
        val criticalCount = flightSnapshot.cancelledCount + flightSnapshot.divertedCount
        val condition = if (weatherEnabled && weather.temp.isNotBlank()) {
            resolvedBriefingWeatherCondition(weather)
        } else {
            appContext.dayPart
        }
        val visualCondition = briefingWeatherVisualCondition(condition)
        val weatherChip = when {
            !weatherEnabled -> BriefingInsightChip("--", "Weather")
            weather.temp.isNotBlank() -> {
                val temp = Regex("""-?\d+""").find(weather.temp)?.value?.let { "$it°" }
                    ?: briefingWeatherConditionLabel(condition)
                BriefingInsightChip(temp, if (visualCondition == "night") "Night" else briefingWeatherConditionLabel(condition))
            }
            else -> BriefingInsightChip(briefingWeatherConditionLabel(visualCondition), "Weather")
        }

        val title: String
        val action: BriefingInsightAction
        val actionLabel: String
        val localBody: String
        when {
            criticalCount > 0 -> {
                title = "Flight change found"
                action = BriefingInsightAction.Flights
                actionLabel = "Open Flights"
                localBody = "$criticalCount flight ${if (criticalCount == 1) "change needs" else "changes need"} attention today. Open Flights before you leave."
            }
            delayedCount > 0 -> {
                title = "Delay showing"
                action = BriefingInsightAction.Flights
                actionLabel = "Open Flights"
                localBody = "$delayedCount delay ${if (delayedCount == 1) "is" else "are"} showing today. Open Flights to see which trip may be affected."
            }
            appContext.badgeCount > 0 || appContext.reminderCount > 0 -> {
                val reminders = (appContext.badgeCount.takeIf { it > 0 } ?: appContext.reminderCount).coerceAtLeast(1)
                title = "Review your reminders"
                action = BriefingInsightAction.Notes
                actionLabel = "Review Notes"
                localBody = "$reminders note ${if (reminders == 1) "reminder is" else "reminders are"} waiting. Review Notes so you do not miss a trip detail."
            }
            visualCondition == "night" || condition == "rain" || condition == "thunder" || condition == "fog" -> {
                title = "Check the cameras"
                action = BriefingInsightAction.Cameras
                actionLabel = "Open Cameras"
                localBody = "Weather and light make the cameras useful right now. Check curb, north, and south views."
            }
            noteCount <= 0 -> {
                title = "Set up your trip notes"
                action = BriefingInsightAction.QuickNote
                actionLabel = "Quick Note"
                localBody = "Add a note for flight numbers, parking, pickup details, or anything you want to remember later."
            }
            arrivalCount > 0 || departureCount > 0 -> {
                title = "Today at JAC"
                action = BriefingInsightAction.Flights
                actionLabel = "Open Flights"
                localBody = "Today shows ${briefingFlightCountText(arrivalCount, "arrival")} and ${briefingFlightCountText(departureCount, "departure")}. Open Flights for times and changes."
            }
            else -> {
                title = "Nothing urgent right now"
                action = BriefingInsightAction.Fbo
                actionLabel = "Open FBO"
                localBody = "No urgent flight changes are showing. Cameras, notes, and airport services are ready if you need them."
            }
        }

        val chips = buildList {
            if (arrivalCount > 0 || departureCount > 0) {
                add(BriefingInsightChip(arrivalCount.toString(), "Arr"))
                add(BriefingInsightChip(departureCount.toString(), "Dep"))
            }
            if (delayedCount > 0) add(BriefingInsightChip(delayedCount.toString(), "Delay", "warning"))
            if (criticalCount > 0) add(BriefingInsightChip(criticalCount.toString(), "Critical", "critical"))
            add(weatherChip)
            if (appContext.badgeCount > 0) {
                add(BriefingInsightChip(appContext.badgeCount.toString(), "Badges", "warning"))
            } else if (noteCount > 0) {
                add(BriefingInsightChip(noteCount.toString(), "Notes"))
            }
        }.take(4)

        return BriefingInsight(
            title = title,
            body = when {
                criticalCount > 0 || delayedCount > 0 -> localBody
                appContext.badgeCount > 0 || appContext.reminderCount > 0 -> localBody
                visualCondition in setOf("rain", "rain_heavy", "thunder", "fog", "hail", "mix", "snow") -> localBody
                else -> aiSentence.takeIf { it.isNotBlank() } ?: localBody
            },
            actionLabel = actionLabel,
            action = action,
            chips = chips.ifEmpty { listOf(weatherChip) }
        )
    }

    private fun parseBriefingFlightSnapshot(json: String): BriefingFlightSnapshot {
        if (json.isBlank()) return BriefingFlightSnapshot()
        return runCatching {
            Gson().fromJson(json, BriefingFlightSnapshot::class.java) ?: BriefingFlightSnapshot()
        }.getOrDefault(BriefingFlightSnapshot())
    }

    private fun briefingFlightDisplayKey(json: String): String {
        val snapshot = parseBriefingFlightSnapshot(json)
        return listOf(
            snapshot.summary,
            snapshot.issueCount,
            snapshot.arrivalCount,
            snapshot.departureCount,
            snapshot.delayedCount,
            snapshot.cancelledCount,
            snapshot.divertedCount,
            snapshot.source,
            snapshot.issues.joinToString("~") { "${it.label}|${it.flight}|${it.route}|${it.time}|${it.tone}" }
        ).joinToString("|")
    }

    private fun par9yMnTm4NSzvG9rrwjM2ec8xZgh1cafXH8(json: String): BriefingWeatherSnapshot {
        if (json.isBlank()) return BriefingWeatherSnapshot()
        return runCatching {
            Gson().fromJson(json, BriefingWeatherSnapshot::class.java) ?: BriefingWeatherSnapshot()
        }.getOrDefault(BriefingWeatherSnapshot())
    }

    private fun briefingWeatherDisplayKey(json: String): String {
        val snapshot = par9yMnTm4NSzvG9rrwjM2ec8xZgh1cafXH8(json)
        return listOf(snapshot.temp, snapshot.condition, snapshot.summary, snapshot.source).joinToString("|")
    }

    private fun isLiveBriefingWeatherSnapshot(weather: BriefingWeatherSnapshot): Boolean {
        if (weather.updatedAt > 0L && System.currentTimeMillis() - weather.updatedAt > 2L * 60L * 60L * 1000L) {
            return false
        }
        if (weather.source == "airport_web") return true
        if (weather.temp.isBlank()) return false
        if (weather.source == "open_meteo") return true
        return weather.source.isBlank() && weather.temp.contains("/")
    }

    private fun resolvedBriefingWeatherCondition(weather: BriefingWeatherSnapshot): String {
        val conditionText = weather.condition.trim().lowercase()
        val summaryText = weather.summary.trim().lowercase()
        val raw = when {
            conditionText.contains("thunder") || summaryText.contains("thunder") -> "thunder"
            conditionText.contains("hail") || summaryText.contains("hail") -> "hail"
            conditionText.contains("snow") || summaryText.contains("snow") -> "snow"
            conditionText.contains("sleet") || summaryText.contains("sleet") ||
                    conditionText.contains("mix") || summaryText.contains("wintry mix") -> "mix"
            conditionText.contains("heavy rain") || summaryText.contains("heavy rain") -> "rain_heavy"
            conditionText.contains("rain") || summaryText.contains("rain") -> "rain"
            conditionText.contains("fog") || summaryText.contains("fog") -> "fog"
            conditionText.contains("cloud") -> "cloudy"
            conditionText.contains("partly") -> "partly"
            conditionText.contains("clear") -> "sunny"
            conditionText.contains("sun") -> "sunny"
            else -> conditionText
        }
        val normalizedRaw = when (raw) {
            "clear" -> "sunny"
            "storm" -> "thunder"
            else -> raw
        }
        val cloudPercent = Regex("""Cloud\s+(\d+)%""", RegexOption.IGNORE_CASE)
            .find(weather.summary)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()

        if (normalizedRaw in setOf("rain", "rain_heavy", "thunder", "fog", "hail", "mix", "snow")) {
            return normalizedRaw
        }
        val resolved = if (cloudPercent != null) {
            when {
                cloudPercent >= 70 -> "cloudy"
                cloudPercent >= 30 -> "partly"
                else -> normalizedRaw.ifBlank { "sunny" }
            }
        } else {
            normalizedRaw.ifBlank { "sunny" }
        }
        return briefingWeatherVisualCondition(resolved)
    }

    private fun countFromSummary(summary: String, word: String): Int {
        return Regex("""(\d+)\s+$word""", RegexOption.IGNORE_CASE)
            .find(summary)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
            ?: 0
    }

    private fun briefingFlightCountText(count: Int, singular: String): String {
        return "$count $singular${if (count == 1) "" else "s"}"
    }

    private fun isJacksonHoleNight(): Boolean {
        val hour = Calendar.getInstance(TimeZone.getTimeZone("America/Denver")).get(Calendar.HOUR_OF_DAY)
        return hour !in 6..19
    }

    private fun briefingWeatherVisualCondition(condition: String): String {
        val normalized = condition.ifBlank { "sunny" }.lowercase()
        return if (isJacksonHoleNight() && (normalized == "sunny" || normalized == "clear" || normalized == "partly")) {
            if (normalized == "partly") "partly_night" else "night"
        } else {
            normalized
        }
    }

    private fun briefingFallbackWeatherCondition(hour: Int): String {
        return when (hour) {
            in 6..19 -> "sunny"
            in 20..21, in 4..5 -> "partly"
            else -> "night"
        }
    }

    private fun briefingGreetingForHour(hour: Int): String {
        return when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..20 -> "Good evening"
            else -> "Good night"
        }
    }

    private fun briefingDayPartForHour(hour: Int): String {
        return when (hour) {
            in 5..11 -> "morning"
            in 12..16 -> "afternoon"
            in 17..20 -> "evening"
            else -> "night"
        }
    }

    private fun briefingGreetingTitleForHour(hour: Int): String {
        return when (hour) {
            in 0..3 -> "Quiet night briefing"
            4 -> "Early airport briefing"
            in 5..10 -> "Morning airport briefing"
            11 -> "Late-morning briefing"
            in 12..14 -> "Midday airport briefing"
            in 15..16 -> "Afternoon briefing"
            in 17..20 -> "Evening airport briefing"
            else -> "Night airport briefing"
        }
    }

    private fun briefingFriendlyMessageForHour(hour: Int, slot: Int): String {
        val messages = when (hour) {
            in 0..3 -> listOf(
                "A calm overnight check for flights, notes, cameras, and anything that needs attention.",
                "Flights, notes, and cameras stay close while the screen keeps the night view simple.",
                "Use this quick night pass before checking flights, parking, or reminders."
            )
            4 -> listOf(
                "Start early with flights, notes, and the live airport view before the day gets busy.",
                "The airport day is waking up. This brief helps you choose the next useful step.",
                "Check flights, reminders, and cameras before the morning starts moving."
            )
            in 5..10 -> listOf(
                "Start with flights, then use notes or cameras if something needs a closer look.",
                "A morning read of JAC with flights, weather, notes, and quick actions ready.",
                "Scan the morning brief first, then open the part that matches your next step."
            )
            11 -> listOf(
                "A late-morning check with flights, cameras, notes, and airport info ready when needed.",
                "Use this as a quick pass before plans change again later today.",
                "The useful airport details stay close so you can decide what to open next."
            )
            in 12..14 -> listOf(
                "A midday reset for flights, weather, notes, and anything that changed since morning.",
                "Flights, notes, and airport tools are ready for a quick second look.",
                "Use this midday check to decide if Flights, Notes, or Cameras should come next."
            )
            in 15..16 -> listOf(
                "An afternoon check for details that can still change: flights, reminders, and cameras.",
                "Keep the next step simple. This brief shows what needs attention first.",
                "Your afternoon airport tools are ready if you need flights, notes, cameras, or service info."
            )
            in 17..20 -> listOf(
                "An evening check for the airport details that still matter before the day winds down.",
                "Start with flights, then review notes or cameras if your evening plan depends on them.",
                "Check the live airport picture, review reminders, and keep the rest simple."
            )
            else -> listOf(
                "A night briefing with flights, notes, cameras, and airport tools still easy to reach.",
                "Only the essentials stay forward: flights, notes, cameras, and the next airport check.",
                "Wind down with a clear view of what is ready for tomorrow or the next trip."
            )
        }
        return messages[slot.floorMod(messages.size)]
    }

    private fun Int.floorMod(divisor: Int): Int = ((this % divisor) + divisor) % divisor

    @Composable
    private fun BriefingSmartCard(
        title: String,
        insight: BriefingInsight,
        caption: String,
        cardColor: Color,
        accentColor: Color,
        textColor: Color,
        subTextColor: Color,
        greetingTitle: String,
        friendlyMessage: String,
        weather: BriefingWeatherSnapshot,
        weatherEnabled: Boolean,
        fallbackCondition: String,
        flightIssueCards: List<BriefingFlightIssueCard>,
        animationKey: String,
        animateEffects: Boolean,
        onInsightAction: (BriefingInsightAction) -> Unit,
        onEffectsStarted: () -> Unit
    ) {
        val shape = RoundedCornerShape(18.dp)
        val playEffects = remember(animationKey) { animateEffects }
        LaunchedEffect(animationKey, playEffects) {
            if (playEffects) onEffectsStarted()
        }
        val conditionKey = when {
            DEBUG_FORCE_BRIEFING_SUN -> "sunny"
            DEBUG_FORCE_BRIEFING_THUNDER -> "thunder"
            DEBUG_FORCE_BRIEFING_RAIN -> "rain"
            weather.source == "airport_web" && weather.condition.isNotBlank() -> resolvedBriefingWeatherCondition(weather)
            weather.temp.isNotBlank() -> resolvedBriefingWeatherCondition(weather)
            else -> fallbackCondition
        }
        val visualConditionKey = briefingWeatherVisualCondition(conditionKey)
        val isDark = isSystemInDarkTheme()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(cardColor.copy(alpha = if (isDark) 0.98f else 1f))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            accentColor.copy(alpha = if (isDark) 0.20f else 0.09f),
                            cardColor.copy(alpha = if (isDark) 0.18f else 0.82f),
                            Color(0xFF111827).copy(alpha = if (isDark) 0.22f else 0.025f)
                        ),
                        start = Offset.Zero,
                        end = Offset(760f, 520f)
                    )
                )
                .then(
                    if (isDark) Modifier
                    else Modifier.border(
                        width = 1.dp,
                        color = Color(0xFFC9D2E3),
                        shape = shape
                    )
                )
        ) {
            BriefingSmartCardBaseGlow(
                accentColor = accentColor,
                modifier = Modifier.matchParentSize()
            )
            if (playEffects) {
                BriefingAuroraCardGlow(
                    animationKey = animationKey,
                    modifier = Modifier.matchParentSize()
                )
            }
            if (weatherEnabled) {
                AnimatedContent(
                    targetState = visualConditionKey,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(420, easing = FastOutSlowInEasing)) +
                                scaleIn(initialScale = 0.985f, animationSpec = tween(420, easing = FastOutSlowInEasing)) togetherWith
                                fadeOut(animationSpec = tween(260, easing = FastOutSlowInEasing)) +
                                scaleOut(targetScale = 1.015f, animationSpec = tween(260, easing = FastOutSlowInEasing))
                    },
                    label = "briefingWeatherAtmosphere"
                ) { condition ->
                    BriefingWeatherAtmosphere(
                        condition = condition,
                        modifier = Modifier.matchParentSize()
                    )
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 46.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            if (isDark) accentColor.copy(alpha = 0.13f)
                            else Color(0xFFF0EDFF)
                        )
                        .then(
                            if (isDark) Modifier
                            else Modifier.border(
                                width = 1.dp,
                                color = Color(0xFFD8D1FF),
                                shape = RoundedCornerShape(999.dp)
                            )
                        )
                        .padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    BriefingSparkleBadge(
                        modifier = Modifier.size(48.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        Text(
                            text = title,
                            color = textColor,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Black,
                                lineHeight = 16.sp,
                                letterSpacing = 0.sp
                            ),
                            maxLines = 1
                        )
                        Text(
                            text = caption,
                            color = subTextColor,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 14.sp,
                                letterSpacing = 0.sp
                            ),
                            maxLines = 1
                        )
                    }
                    if (weatherEnabled) {
                        BriefingWeatherPill(
                            weather = weather,
                            accentColor = accentColor,
                            textColor = textColor,
                            subTextColor = subTextColor
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Text(
                        text = greetingTitle,
                        color = textColor,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Black,
                            lineHeight = 30.sp,
                            letterSpacing = 0.sp
                        ),
                        maxLines = 2
                    )
                    Text(
                        text = friendlyMessage,
                        color = subTextColor,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            lineHeight = 21.sp,
                            letterSpacing = 0.sp
                        ),
                        maxLines = 3
                    )
                }
                BriefingInsightPanel(
                    insight = insight,
                    accentColor = accentColor,
                    textColor = textColor,
                    subTextColor = subTextColor,
                    onAction = onInsightAction
                )
                if (flightIssueCards.isNotEmpty()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        flightIssueCards.take(3).forEach { issue ->
                            BriefingFlightIssueMiniCard(
                                issue = issue,
                                textColor = textColor,
                                subTextColor = subTextColor
                            )
                        }
                        if (flightIssueCards.size > 3) {
                            Text(
                                text = "+${flightIssueCards.size - 3} more in Flights",
                                color = subTextColor,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun BriefingInsightPanel(
        insight: BriefingInsight,
        accentColor: Color,
        textColor: Color,
        subTextColor: Color,
        onAction: (BriefingInsightAction) -> Unit
    ) {
        val isDark = isSystemInDarkTheme()
        AnimatedContent(
            targetState = insight,
            transitionSpec = {
                fadeIn(animationSpec = tween(720, easing = FastOutSlowInEasing)) +
                        slideInVertically(
                            animationSpec = tween(720, easing = FastOutSlowInEasing),
                            initialOffsetY = { it / 6 }
                        ) togetherWith
                        fadeOut(animationSpec = tween(320, easing = FastOutSlowInEasing)) +
                        slideOutVertically(
                            animationSpec = tween(320, easing = FastOutSlowInEasing),
                            targetOffsetY = { -it / 8 }
                        )
            },
            label = "briefingInsightPanel"
        ) { state ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = state.title,
                        color = textColor,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.sp
                        ),
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = state.body,
                    color = subTextColor,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        lineHeight = 20.sp,
                        letterSpacing = 0.sp
                    ),
                    maxLines = 3
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            if (isDark) accentColor.copy(alpha = 0.18f)
                            else Color(0xFF5B46D6)
                        )
                        .then(
                            if (isDark) Modifier
                            else Modifier.border(
                                width = 1.dp,
                                color = Color(0xFF4935C3),
                                shape = RoundedCornerShape(999.dp)
                            )
                        )
                        .clickable { onAction(state.action) }
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = state.actionLabel,
                        color = if (isDark) textColor else Color.White,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.sp
                        ),
                        maxLines = 1
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    state.chips.forEach { chip ->
                        BriefingInsightChipView(
                            chip = chip,
                            accentColor = accentColor,
                            textColor = textColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun BriefingInsightChipView(
        chip: BriefingInsightChip,
        accentColor: Color,
        textColor: Color,
        modifier: Modifier = Modifier
    ) {
        val isDark = isSystemInDarkTheme()
        val containerColor: Color
        val contentColor: Color

        if (isDark) {
            val toneColor = when (chip.tone) {
                "critical" -> Color(0xFFFF6B6B)
                "warning" -> Color(0xFFFACC15)
                else -> accentColor
            }
            containerColor = toneColor.copy(alpha = 0.14f)
            contentColor = textColor
        } else {
            when (chip.tone) {
                "critical" -> {
                    containerColor = Color(0xFFFEE2E2)
                    contentColor = Color(0xFFB91C1C)
                }
                "warning" -> {
                    containerColor = Color(0xFFFFE8A3)
                    contentColor = Color(0xFF7C3E00)
                }
                else -> {
                    containerColor = Color(0xFFEDE9FE)
                    contentColor = Color(0xFF4338CA)
                }
            }
        }

        Row(
            modifier = modifier
                .height(if (isDark) 30.dp else 32.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(containerColor)
                .then(
                    if (isDark) Modifier
                    else Modifier.border(
                        width = 1.dp,
                        color = contentColor.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(999.dp)
                    )
                )
                .padding(horizontal = if (isDark) 7.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            AnimatedContent(
                targetState = chip.value,
                transitionSpec = {
                    fadeIn(animationSpec = tween(360, easing = FastOutSlowInEasing)) +
                            slideInVertically(animationSpec = tween(360, easing = FastOutSlowInEasing)) { it / 2 } togetherWith
                            fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
                            slideOutVertically(animationSpec = tween(180, easing = FastOutSlowInEasing)) { -it / 2 }
                },
                label = "briefingInsightChipValue"
            ) { value ->
                Text(
                    text = value,
                    color = contentColor,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                    maxLines = 1
                )
            }
            Text(
                text = " ${chip.label}",
                color = contentColor.copy(alpha = if (isDark) 0.72f else 0.84f),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isDark) FontWeight.SemiBold else FontWeight.Bold
                ),
                maxLines = 1
            )
        }
    }

    @Composable
    private fun BriefingFlightIssueMiniCard(
        issue: BriefingFlightIssueCard,
        textColor: Color,
        subTextColor: Color
    ) {
        val isDark = isSystemInDarkTheme()
        val route = issue.route
        val direction = when {
            route.startsWith("JAC to", ignoreCase = true) -> "Outbound"
            route.endsWith("to JAC", ignoreCase = true) -> "Inbound"
            else -> if (issue.tone.equals("cancelled", true) || issue.tone.equals("diverted", true)) "Flight" else "Delay"
        }
        val tone = issue.tone.lowercase()
        val toneColor = if (isDark) {
            when (tone) {
                "cancelled" -> Color(0xFFFF6B6B)
                "diverted" -> Color(0xFFA78BFA)
                else -> Color(0xFFFACC15)
            }
        } else {
            when (tone) {
                "cancelled" -> Color(0xFFB91C1C)
                "diverted" -> Color(0xFF6D28D9)
                else -> Color(0xFF9A4D00)
            }
        }
        val containerColor = if (isDark) {
            toneColor.copy(alpha = 0.12f)
        } else {
            when (tone) {
                "cancelled" -> Color(0xFFFEE2E2)
                "diverted" -> Color(0xFFEDE9FE)
                else -> Color(0xFFFFE8A3)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(containerColor)
                .then(
                    if (isDark) Modifier
                    else Modifier.border(
                        width = 1.dp,
                        color = toneColor.copy(alpha = 0.24f),
                        shape = RoundedCornerShape(14.dp)
                    )
                )
                .padding(horizontal = 10.dp, vertical = if (isDark) 8.dp else 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(if (isDark) 28.dp else 30.dp)
                    .clip(CircleShape)
                    .background(
                        if (isDark) toneColor.copy(alpha = 0.18f)
                        else toneColor.copy(alpha = 0.13f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Flight,
                    contentDescription = null,
                    tint = toneColor,
                    modifier = Modifier.size(if (isDark) 16.dp else 17.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = listOf(direction, issue.flight.ifBlank { "Flight" }).joinToString("  "),
                        color = textColor,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = issue.label,
                        color = toneColor,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                        maxLines = 1
                    )
                }
                Text(
                    text = listOf(route, issue.time).filter { it.isNotBlank() }.joinToString(" • "),
                    color = if (isDark) subTextColor else Color(0xFF4B5565),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1
                )
            }
        }
    }

    private fun briefingWeatherConditionLabel(condition: String): String {
        return when (condition) {
            "thunder" -> "Storm"
            "rain_heavy" -> "Heavy rain"
            "hail" -> "Hail"
            "mix" -> "Wintry mix"
            "snow" -> "Snow"
            "rain" -> "Rain"
            "fog" -> "Fog"
            "night" -> "Night"
            "cloudy" -> "Cloudy"
            "partly" -> "Partly cloudy"
            else -> "Sunny"
        }
    }

    private fun briefingWeatherSymbolName(condition: String): String {
        return when (condition) {
            "night" -> "moon_stars"
            "partly_night" -> "partly_cloudy_night"
            "partly" -> "partly_cloudy_day"
            "cloudy" -> "cloud"
            "fog" -> "foggy"
            "rain_heavy" -> "rainy_heavy"
            "rain" -> "rainy"
            "thunder" -> "thunderstorm"
            "hail" -> "weather_hail"
            "mix" -> "weather_mix"
            "snow" -> "weather_snowy"
            else -> "sunny"
        }
    }

    @Composable
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    private fun BriefingWeatherPill(
        weather: BriefingWeatherSnapshot,
        accentColor: Color,
        textColor: Color,
        subTextColor: Color
    ) {
        if (weather.temp.isBlank()) return
        val condition = when {
            DEBUG_FORCE_BRIEFING_SUN -> "sunny"
            DEBUG_FORCE_BRIEFING_THUNDER -> "thunder"
            DEBUG_FORCE_BRIEFING_RAIN -> "rain"
            else -> resolvedBriefingWeatherCondition(weather)
        }
        val visualCondition = briefingWeatherVisualCondition(condition)
        val displayTemp = weather.temp
        val conditionLabel = briefingWeatherConditionLabel(visualCondition)
        val isDark = isSystemInDarkTheme()
        val conditionColor = when (visualCondition) {
            "sunny", "partly" -> if (isDark) Color(0xFFFACC15) else Color(0xFFB45309)
            "thunder" -> if (isDark) Color(0xFFFFE066) else Color(0xFF7C3E00)
            "rain", "rain_heavy" -> if (isDark) Color(0xFF38BDF8) else Color(0xFF0369A1)
            "hail", "mix", "snow" -> if (isDark) Color(0xFFE0F2FE) else Color(0xFF1D4ED8)
            "fog", "cloudy" -> if (isDark) Color(0xFFD8E2EE) else Color(0xFF475569)
            "night", "partly_night" -> if (isDark) Color(0xFFBFD7FF) else Color(0xFF315EA8)
            else -> accentColor
        }
        val pillTextColor = if (isDark) Color.White else textColor
        val pillSubTextColor = if (isDark) Color.White.copy(alpha = 0.72f) else subTextColor
        val weatherTarget = Triple(visualCondition, displayTemp, conditionLabel)
        var visibleWeatherTarget by remember { mutableStateOf(weatherTarget) }
        var showWeatherLoader by remember { mutableStateOf(false) }
        LaunchedEffect(weatherTarget) {
            if (visibleWeatherTarget == weatherTarget) return@LaunchedEffect
            showWeatherLoader = true
            delay(620L.milliseconds)
            visibleWeatherTarget = weatherTarget
            delay(360L.milliseconds)
            showWeatherLoader = false
        }
        AnimatedContent(
            targetState = showWeatherLoader to visibleWeatherTarget,
            transitionSpec = {
                fadeIn(animationSpec = tween(560, easing = FastOutSlowInEasing)) +
                        scaleIn(initialScale = 0.92f, animationSpec = tween(560, easing = FastOutSlowInEasing)) togetherWith
                        fadeOut(animationSpec = tween(360, easing = FastOutSlowInEasing)) +
                        scaleOut(targetScale = 1.04f, animationSpec = tween(360, easing = FastOutSlowInEasing))
            },
            label = "briefingWeatherPill"
        ) { state ->
            val loading = state.first
            val animatedCondition = state.second.first
            val animatedTemp = state.second.second
            val tempFontSize = when {
                animatedTemp.length >= 12 -> 12.sp
                animatedTemp.length >= 10 -> 14.sp
                else -> 16.sp
            }
            Row(
                modifier = Modifier
                    .height(46.dp)
                    .widthIn(min = 104.dp)
                    .padding(start = 4.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                if (loading) {
                    Row(
                        modifier = Modifier,
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LoadingIndicator(
                            modifier = Modifier.size(22.dp),
                            color = conditionColor
                        )
                        Text(
                            text = "Updating",
                            color = pillTextColor.copy(alpha = 0.82f),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 13.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BriefingWeatherIcon(
                            condition = animatedCondition,
                            tint = conditionColor,
                            modifier = Modifier.size(40.dp)
                        )
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            Text(
                                text = animatedTemp,
                                color = pillTextColor,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = tempFontSize,
                                    lineHeight = (tempFontSize.value + 2).sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Clip
                            )
                            Text(
                                text = "Jackson",
                                color = pillSubTextColor,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 9.sp,
                                    lineHeight = 11.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Clip
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun BriefingWeatherIcon(
        condition: String,
        tint: Color,
        modifier: Modifier = Modifier
    ) {
        val symbolFont = remember {
            FontFamily(
                Font(
                    resId = R.font.material_symbols_rounded,
                    variationSettings = FontVariation.Settings(
                        FontVariation.Setting("FILL", 1f),
                        FontVariation.Setting("wght", 400f),
                        FontVariation.Setting("GRAD", 0f),
                        FontVariation.Setting("opsz", 24f)
                    )
                )
            )
        }
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = briefingWeatherSymbolName(condition),
                color = tint,
                fontFamily = symbolFont,
                fontWeight = FontWeight.Normal,
                fontSize = 30.sp,
                lineHeight = 40.sp,
                maxLines = 1
            )
        }
    }

    @Composable
    private fun BriefingWeatherAtmosphere(
        condition: String,
        modifier: Modifier = Modifier
    ) {
        val normalized = condition.ifBlank { "sunny" }.lowercase()
        BoxWithConstraints(modifier = modifier) {
            val density = LocalDensity.current
            val widthPx = with(density) { maxWidth.toPx().coerceAtLeast(1f) }
            val heightPx = with(density) { maxHeight.toPx().coerceAtLeast(1f) }
            when (normalized) {
                "thunder", "storm" -> BriefingThunderOverlay(
                    widthPx = widthPx,
                    heightPx = heightPx,
                    modifier = Modifier.matchParentSize()
                )
                "rain" -> BriefingRainOverlay(
                    widthPx = widthPx,
                    heightPx = heightPx,
                    modifier = Modifier.matchParentSize()
                )
                "night" -> BriefingNightCornerOverlay(modifier = Modifier.matchParentSize())
                "cloudy", "fog" -> BriefingFogOverlay(modifier = Modifier.matchParentSize())
                else -> BriefingSunCornerOverlay(
                    partly = normalized == "partly",
                    modifier = Modifier.matchParentSize()
                )
            }
        }
    }

    @Composable
    private fun BriefingThunderOverlay(
        widthPx: Float,
        heightPx: Float,
        modifier: Modifier = Modifier
    ) {
        Box(modifier = modifier) {
            BriefingRainOverlay(
                widthPx = widthPx,
                heightPx = heightPx,
                modifier = Modifier.matchParentSize()
            )
            BriefingLightningOverlay(modifier = Modifier.matchParentSize())
        }
    }

    @Composable
    private fun BriefingLightningOverlay(
        modifier: Modifier = Modifier
    ) {
        val transition = rememberInfiniteTransition(label = "briefingLightning")
        val phase by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(5_800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "briefingLightningPhase"
        )
        val flashAlpha = when {
            phase < 0.025f -> 0.24f * (1f - phase / 0.025f)
            phase in 0.055f..0.085f -> 0.14f * (1f - (phase - 0.055f) / 0.030f)
            else -> 0f
        }
        Canvas(modifier = modifier) {
            if (flashAlpha <= 0.001f) return@Canvas
            drawRect(Color.White.copy(alpha = flashAlpha * 0.55f))
            val boltColor = Color(0xFFFFF3A3)
            val glowColor = boltColor.copy(alpha = flashAlpha * 0.62f)
            val bolt = listOf(
                Offset(size.width * 0.82f, size.height * 0.06f),
                Offset(size.width * 0.71f, size.height * 0.30f),
                Offset(size.width * 0.82f, size.height * 0.29f),
                Offset(size.width * 0.66f, size.height * 0.62f),
                Offset(size.width * 0.74f, size.height * 0.40f),
                Offset(size.width * 0.62f, size.height * 0.42f)
            )
            for (i in 0 until bolt.lastIndex) {
                drawLine(
                    color = glowColor.copy(alpha = flashAlpha * 0.55f),
                    start = bolt[i],
                    end = bolt[i + 1],
                    strokeWidth = 9.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = boltColor.copy(alpha = flashAlpha.coerceAtMost(0.95f)),
                    start = bolt[i],
                    end = bolt[i + 1],
                    strokeWidth = 2.4.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }

    @Composable
    private fun BriefingRainOverlay(
        widthPx: Float,
        heightPx: Float,
        modifier: Modifier = Modifier
    ) {
        val density = LocalDensity.current
        val isDark = isSystemInDarkTheme()
        val rainColor = if (isDark) Color(0xFFB3E5FC) else Color(0xFF1E2733)
        val dropCount = 38
        val angleRad = (105f * PI / 180.0).toFloat()
        val dirX = cos(angleRad)
        val dirY = sin(angleRad).coerceAtLeast(0.05f)
        val lengthMinPx = with(density) { 10.dp.toPx() }
        val lengthMaxPx = with(density) { 22.dp.toPx() }
        val speedMinPx = with(density) { 130.dp.toPx() }
        val speedMaxPx = with(density) { 270.dp.toPx() }
        val horizontalDrift = abs(dirX) * heightPx / dirY
        val spawnXMin = -horizontalDrift
        val spawnXMax = widthPx + horizontalDrift
        val spawnXSpan = spawnXMax - spawnXMin
        val drops = remember(widthPx, heightPx) {
            val rng = Random(0x21A1B5)
            List(dropCount) {
                BriefingRainDrop(
                    x = spawnXMin + rng.nextFloat() * spawnXSpan,
                    y = rng.nextFloat() * heightPx,
                    speed = speedMinPx + rng.nextFloat() * (speedMaxPx - speedMinPx),
                    length = lengthMinPx + rng.nextFloat() * (lengthMaxPx - lengthMinPx)
                )
            }
        }
        var tick by remember { mutableLongStateOf(0L) }
        LaunchedEffect(widthPx, heightPx, dirX, dirY) {
            var lastNanos = 0L
            val rng = Random(0x51A7E)
            while (true) {
                withFrameNanos { now ->
                    val dt = if (lastNanos == 0L) 0f else ((now - lastNanos) / 1_000_000_000f).coerceAtMost(0.05f)
                    lastNanos = now
                    drops.forEach { drop ->
                        drop.x += dirX * drop.speed * dt
                        drop.y += dirY * drop.speed * dt
                        if (drop.y - drop.length > heightPx || drop.x + drop.length < spawnXMin || drop.x - drop.length > spawnXMax) {
                            drop.x = spawnXMin + rng.nextFloat() * spawnXSpan
                            drop.y = -drop.length - rng.nextFloat() * heightPx * 0.28f
                        }
                    }
                    tick++
                }
            }
        }
        Canvas(modifier = modifier) {
            val touch = tick
            drops.forEach { drop ->
                val head = Offset(drop.x, drop.y)
                val tail = Offset(drop.x - dirX * drop.length, drop.y - dirY * drop.length)
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            rainColor.copy(alpha = 0f),
                            rainColor.copy(alpha = if (isDark) 0.38f else 0.34f)
                        ),
                        start = tail,
                        end = head
                    ),
                    start = tail,
                    end = head,
                    strokeWidth = 1.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                if (head.y in (size.height - 2f)..(size.height + 2f)) {
                    drawCircle(
                        color = rainColor.copy(alpha = if (isDark) 0.28f else 0.22f),
                        radius = 1.8.dp.toPx(),
                        center = Offset(head.x, size.height - 1f)
                    )
                }
            }
            touch.hashCode()
        }
    }

    @Composable
    private fun BriefingFogOverlay(
        modifier: Modifier = Modifier
    ) {
        val isDark = isSystemInDarkTheme()
        val fogColor = if (isDark) Color.White else Color(0xFF8EA4BC)
        val transition = rememberInfiniteTransition(label = "briefingFog")
        val breath by transition.animateFloat(
            initialValue = 0.86f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(6_800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "briefingFogBreath"
        )
        val drift by transition.animateFloat(
            initialValue = -0.035f,
            targetValue = 0.035f,
            animationSpec = infiniteRepeatable(
                animation = tween(9_200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "briefingFogDrift"
        )
        Canvas(modifier = modifier) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        fogColor.copy(alpha = 0.055f),
                        fogColor.copy(alpha = 0.022f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = size.height * 0.72f
                )
            )
            repeat(3) { index ->
                val layerDrift = drift * size.width * (1f + index * 0.34f)
                val radius = size.minDimension * (0.42f + index * 0.10f) * breath
                val center = Offset(
                    size.width * (0.28f + index * 0.25f) + layerDrift,
                    size.height * (0.14f + index * 0.08f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            fogColor.copy(alpha = 0.060f - index * 0.010f),
                            fogColor.copy(alpha = 0.022f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = radius
                    ),
                    radius = radius,
                    center = center
                )
            }
        }
    }

    @Composable
    private fun BriefingSunCornerOverlay(
        partly: Boolean,
        modifier: Modifier = Modifier
    ) {
        val isDark = isSystemInDarkTheme()
        val transition = rememberInfiniteTransition(label = "briefingSun")
        val pulse by transition.animateFloat(
            initialValue = 0.86f,
            targetValue = 1.10f,
            animationSpec = infiniteRepeatable(tween(4_800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "briefingSunPulse"
        )
        Canvas(modifier = modifier) {
            val center = Offset(size.width * 0.91f, size.height * 0.18f)
            val sun = Color(0xFFFACC15)
            val sunDeep = Color(0xFFF59E0B)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        sun.copy(alpha = 0.34f),
                        sunDeep.copy(alpha = 0.16f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.minDimension * 0.58f * pulse
                ),
                radius = size.minDimension * 0.58f * pulse,
                center = center
            )
            drawCircle(
                color = sun.copy(alpha = 0.24f),
                radius = size.minDimension * 0.13f * pulse,
                center = center
            )
            if (partly) {
                val cloud = if (isDark) Color.White.copy(alpha = 0.13f) else Color(0xFF64748B).copy(alpha = 0.10f)
                drawCircle(cloud, radius = size.minDimension * 0.075f, center = Offset(size.width * 0.80f, size.height * 0.25f))
                drawCircle(cloud, radius = size.minDimension * 0.095f, center = Offset(size.width * 0.88f, size.height * 0.22f))
                drawLine(
                    color = cloud,
                    start = Offset(size.width * 0.76f, size.height * 0.30f),
                    end = Offset(size.width * 0.95f, size.height * 0.30f),
                    strokeWidth = 15.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }

    @Composable
    private fun BriefingNightCornerOverlay(
        modifier: Modifier = Modifier
    ) {
        val transition = rememberInfiniteTransition(label = "briefingNight")
        val pulse by transition.animateFloat(
            initialValue = 0.82f,
            targetValue = 1.06f,
            animationSpec = infiniteRepeatable(tween(6_400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "briefingNightPulse"
        )
        Canvas(modifier = modifier) {
            val center = Offset(size.width * 0.90f, size.height * 0.18f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFBFD7FF).copy(alpha = 0.20f),
                        Color(0xFF7C9DFF).copy(alpha = 0.07f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.minDimension * 0.48f * pulse
                ),
                radius = size.minDimension * 0.48f * pulse,
                center = center
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.24f),
                radius = 1.2.dp.toPx(),
                center = Offset(size.width * 0.78f, size.height * 0.14f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.18f),
                radius = 1.dp.toPx(),
                center = Offset(size.width * 0.94f, size.height * 0.31f)
            )
        }
    }

    private data class BriefingRainDrop(
        var x: Float,
        var y: Float,
        val speed: Float,
        val length: Float
    )

    @Composable
    private fun BriefingAuroraCardGlow(
        animationKey: String,
        modifier: Modifier = Modifier
    ) {
        var playAurora by remember(animationKey) { mutableStateOf(false) }
        var fadeAurora by remember(animationKey) { mutableStateOf(false) }
        val wavePhase by animateFloatAsState(
            targetValue = if (playAurora) 1f else 0f,
            animationSpec = tween(durationMillis = 7200, easing = FastOutSlowInEasing),
            label = "briefingCardAuroraWave"
        )
        val fadeProgress by animateFloatAsState(
            targetValue = if (fadeAurora) 1f else 0f,
            animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
            label = "briefingCardAuroraFade"
        )
        val alpha = (1f - fadeProgress) * 0.14f

        LaunchedEffect(animationKey) {
            playAurora = false
            fadeAurora = false
            delay(80.milliseconds)
            playAurora = true
            delay(7_200.milliseconds)
            fadeAurora = true
        }

        Canvas(modifier = modifier) {
            if (alpha <= 0.01f) return@Canvas
            val startX = -size.width * 0.85f + size.width * 1.90f * wavePhase
            val endX = startX + size.width * 1.35f
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFF173EFF).copy(alpha = alpha * 0.36f),
                        Color(0xFF22D3EE).copy(alpha = alpha * 0.58f),
                        Color(0xFFFACC15).copy(alpha = alpha),
                        Color(0xFF4ADE80).copy(alpha = alpha * 0.34f),
                        Color.Transparent
                    ),
                    start = Offset(startX, size.height * 0.88f),
                    end = Offset(endX, size.height * 0.08f)
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFACC15).copy(alpha = alpha * 0.22f),
                        Color(0xFF22D3EE).copy(alpha = alpha * 0.12f),
                        Color.Transparent
                    ),
                    center = Offset(startX + size.width * 0.78f, size.height * 0.42f),
                    radius = size.maxDimension * 0.95f
                )
            )
        }
    }

    private data class BriefingActionEntry(
        val title: String,
        val body: String,
        val icon: ImageVector,
        val onClick: () -> Unit
    )

    @Composable
    private fun BriefingActionGroup(
        title: String,
        actions: List<BriefingActionEntry>,
        accentColor: Color,
        textColor: Color,
        subTextColor: Color,
        borderColor: Color
    ) {
        val isDark = isSystemInDarkTheme()
        val groupContent: @Composable () -> Unit = {
            Column(modifier = Modifier.fillMaxWidth()) {
                actions.forEachIndexed { index, action ->
                    BriefingActionRow(
                        action = action,
                        accentColor = accentColor,
                        textColor = textColor,
                        subTextColor = subTextColor
                    )
                    if (index != actions.lastIndex) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 64.dp, end = 14.dp)
                        ) {
                            HorizontalDivider(
                                thickness = 1.dp,
                                color = if (isDark) {
                                    borderColor.copy(alpha = 0.58f)
                                } else {
                                    Color(0xFFD7DDEA)
                                }
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .briefingElasticAppear("group:$title", delayMillis = 70),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            BriefingSectionTitle(title)
            if (isDark) {
                AppThemeSectionSurface(shape = RoundedCornerShape(18.dp)) {
                    groupContent()
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFFFFFFFF),
                    border = BorderStroke(1.dp, Color(0xFFC9D2E3)),
                    tonalElevation = 0.dp,
                    shadowElevation = 1.dp
                ) {
                    groupContent()
                }
            }
        }
    }

    @Composable
    private fun BriefingActionRow(
        action: BriefingActionEntry,
        accentColor: Color,
        textColor: Color,
        subTextColor: Color
    ) {
        val isDark = isSystemInDarkTheme()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = action.onClick)
                .padding(horizontal = 2.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            ListItem(
                headlineContent = {
                    Text(
                        text = action.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            lineHeight = 19.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingContent = {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isDark) accentColor.copy(alpha = 0.16f)
                                else Color(0xFFEDE9FE)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = action.icon,
                            contentDescription = null,
                            tint = if (isDark) accentColor else Color(0xFF5140C8),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                },
                supportingContent = {
                    Text(
                        text = action.body,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent,
                    headlineColor = textColor,
                    supportingColor = subTextColor
                )
            )
        }
    }

    @Composable
    private fun ContactsDefaultTopBar(
        visible: Boolean,
        contentView: android.view.View?,
        onOpenSearch: () -> Unit,
        onImportContacts: () -> Unit,
        onOpenContactsInfo: () -> Unit,
        alphabeticalMode: Boolean,
        showSearchAction: Boolean,
        showSortAction: Boolean,
        onToggleAlphabetical: () -> Unit
    ) {
        val isDark = isSystemInDarkTheme()
        val barColor = topActionBarTint()
        val contentColor = if (isDark) Color.White else Color(0xFF111111)
        var menuExpanded by remember { mutableStateOf(false) }

        androidx.compose.animation.AnimatedVisibility(
            visible = visible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { FrostedActionBarBlurView(it) },
                    update = {
                        it.contentView = contentView
                        it.scrimColor = barColor.toArgb()
                        it.cornerRadiusPx = 0f
                        it.useLiquidRefraction = false
                        it.blurRadiusPx = TopActionBarNativeBlurPx
                        it.saturation = TopActionBarSaturation
                    }
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .statusBarsPadding()
                        .padding(start = 20.dp, end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Contacts",
                        modifier = Modifier.weight(1f),
                        color = contentColor,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showSearchAction,
                        enter = fadeIn(animationSpec = tween(180)),
                        exit = fadeOut(animationSpec = tween(120))
                    ) {
                        IconButton(onClick = onOpenSearch) {
                            Icon(Icons.Filled.Search, contentDescription = "Search contacts", tint = contentColor)
                        }
                    }
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showSortAction,
                        enter = fadeIn(animationSpec = tween(180)),
                        exit = fadeOut(animationSpec = tween(120))
                    ) {
                        ContactsSortModeButton(
                            alphabeticalMode = alphabeticalMode,
                            contentColor = contentColor,
                            onClick = onToggleAlphabetical
                        )
                    }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Contacts menu", tint = contentColor)
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            shape = RoundedCornerShape(16.dp),
                            containerColor = if (isDark) Color(0xFF202124) else Color.White,
                            tonalElevation = 0.dp,
                            shadowElevation = 0.02.dp,
                            border = BorderStroke(
                                1.dp,
                                if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.08f)
                            )
                        ) {
                            DropdownMenuItem(
                                text = { Text("Import contacts") },
                                leadingIcon = {
                                    Icon(Icons.Filled.ImportContacts, contentDescription = null)
                                },
                                onClick = {
                                    menuExpanded = false
                                    onImportContacts()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Contacts info") },
                                leadingIcon = {
                                    Icon(Icons.Filled.Info, contentDescription = null)
                                },
                                onClick = {
                                    menuExpanded = false
                                    onOpenContactsInfo()
                                }
                            )
                        }
                    }
                }

            }
        }
    }

    @Composable
    private fun ContactsSortModeButton(
        alphabeticalMode: Boolean,
        contentColor: Color,
        onClick: () -> Unit
    ) {
        val tint = if (alphabeticalMode) MaterialTheme.colorScheme.primary else contentColor
        IconButton(onClick = onClick) {
            Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 3f
                    drawLine(tint, Offset(size.width * 0.12f, size.height * 0.25f), Offset(size.width * 0.58f, size.height * 0.25f), stroke, StrokeCap.Round)
                    drawLine(tint, Offset(size.width * 0.12f, size.height * 0.48f), Offset(size.width * 0.50f, size.height * 0.48f), stroke, StrokeCap.Round)
                    drawLine(tint, Offset(size.width * 0.12f, size.height * 0.71f), Offset(size.width * 0.42f, size.height * 0.71f), stroke, StrokeCap.Round)
                }
                Text(
                    text = if (alphabeticalMode) "T" else "A",
                    color = tint,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }
        }
    }

    @Composable
    private fun ContactsSelectionTopBar(
        selectionCount: Int,
        contactCount: Int,
        contentView: android.view.View?,
        onClearSelection: () -> Unit,
        onSelectAll: () -> Unit,
        onDeleteSelected: () -> Unit
    ) {
        val isDark = isSystemInDarkTheme()
        val density = LocalDensity.current.density
        val barColor = topActionBarTint()
        val contentColor = if (isDark) Color.White else Color(0xFF111111)
        val allSelected = contactCount in 1..selectionCount
        val countSpin by animateFloatAsState(
            targetValue = selectionCount * 360f,
            animationSpec = tween(durationMillis = 260),
            label = "contactsSelectionCountSpin"
        )

        androidx.compose.animation.AnimatedVisibility(
            visible = selectionCount > 0,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { FrostedActionBarBlurView(it) },
                    update = {
                        it.contentView = contentView
                        it.scrimColor = barColor.toArgb()
                        it.cornerRadiusPx = 0f
                        it.useLiquidRefraction = false
                        it.blurRadiusPx = TopActionBarNativeBlurPx
                        it.saturation = TopActionBarSaturation
                    }
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .statusBarsPadding()
                        .padding(start = 8.dp, end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClearSelection) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear selection", tint = contentColor)
                    }
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(22.dp))
                            .background(contentColor.copy(alpha = if (isDark) 0.12f else 0.07f))
                            .border(
                                BorderStroke(
                                    1.dp,
                                    contentColor.copy(alpha = if (allSelected) 0.34f else 0.14f)
                                ),
                                RoundedCornerShape(22.dp)
                            )
                            .clickable(enabled = contactCount > 0, onClick = onSelectAll)
                            .padding(start = 10.dp, end = 16.dp)
                            .height(42.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(23.dp)
                                .graphicsLayer { rotationZ = countSpin }
                                .border(
                                    BorderStroke(2.dp, contentColor.copy(alpha = if (allSelected) 0.92f else 0.48f)),
                                    CircleShape
                                )
                                .background(
                                    if (allSelected) contentColor else Color.Transparent,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (allSelected) {
                                Canvas(Modifier.size(12.dp)) {
                                    drawLine(
                                        color = barColor,
                                        start = Offset(size.width * 0.15f, size.height * 0.52f),
                                        end = Offset(size.width * 0.42f, size.height * 0.78f),
                                        strokeWidth = 2.4.dp.toPx(),
                                        cap = StrokeCap.Round
                                    )
                                    drawLine(
                                        color = barColor,
                                        start = Offset(size.width * 0.42f, size.height * 0.78f),
                                        end = Offset(size.width * 0.88f, size.height * 0.20f),
                                        strokeWidth = 2.4.dp.toPx(),
                                        cap = StrokeCap.Round
                                    )
                                }
                            }
                        }
                        Text(
                            text = selectionCount.toString(),
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .graphicsLayer {
                                    rotationX = countSpin
                                    cameraDistance = 12f * density
                                },
                            color = contentColor,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                    IconButton(onClick = onDeleteSelected) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete selected", tint = contentColor)
                    }
                }
            }
        }
    }

    @Composable
    private fun ContactsFloatingSearchOverlay(backdrop: LayerBackdrop) {
        Box(Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val keyboardOpen = WindowInsets.ime.getBottom(density) > 0
            var floatingSearchSawKeyboard by remember { mutableStateOf(false) }

            LaunchedEffect(
                contactsFloatingSearchActive.value,
                keyboardOpen,
                contactsSearchQuery.value
            ) {
                when {
                    !contactsFloatingSearchActive.value -> floatingSearchSawKeyboard = false
                    keyboardOpen -> floatingSearchSawKeyboard = true
                    floatingSearchSawKeyboard && contactsSearchQuery.value.isBlank() -> {
                        contactsFloatingSearchActive.value = false
                        contactsFloatingSearchVisible.value = false
                    }
                }
            }

            ContactsFloatingSearchBar(
                query = contactsSearchQuery.value,
                onQueryChange = { query -> updateContactsFloatingSearch(query) },
                backdrop = backdrop,
                visible = (contactsFloatingSearchActive.value ||
                        contactsSearchQuery.value.isNotBlank()) && contactsSelectionCount.intValue == 0,
                active = contactsFloatingSearchActive.value,
                onActiveChange = { active ->
                    contactsFloatingSearchActive.value = active
                    if (active) contactsFloatingSearchVisible.value = true
                },
                onClose = {
                    contactsFloatingSearchActive.value = false
                    contactsFloatingSearchVisible.value = false
                    updateContactsSearch("")
                    allContactsFragment?.closeContactsSearchUi()
                    hideContactsKeyboard()
                },
                contentView = contactsContainerViewState.value,
                thin = true,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(start = 14.dp, end = 14.dp, top = 92.dp)
            )
        }
    }

    @Composable
    private fun NotesPage(
    ) {
        LaunchedEffect(Unit) {
            currentNotesFolderId = null
            notesAdapter.preloadBadgeStates(this@MainActivity)
            notesAdapter.preloadReminderFlags(this@MainActivity)
            refreshNotesDisplay()
            syncPendingNotesToSupabase()
        }

        val returnToFolders = {
            currentNotesFolderId = null
            refreshNotesDisplay()
        }

        BackHandler(enabled = currentNotesFolderId != null) {
            returnToFolders()
        }

        AllNotesScreen(
            notesAdapter = notesAdapter,
            notes = noteRows,
            onAddNote = {
                if (currentNotesFolderId != null) {
                    addNoteLauncher.launch(
                        AddNoteComposeActivity.newIntent(this),
                        ActivityOptionsCompat.makeSceneTransitionAnimation(this)
                    )
                }
            },
            onDeleteSelected = ::deleteSelectedNotes,
            onDeleteSelectedFolders = ::deleteSelectedNoteFolders,
            onOpenNote = { row, position -> onNoteClick(row.text, position) },
            onBack = if (currentNotesFolderId != null) returnToFolders else null,
            syncStatus = notesSyncStatus,
            syncAvailable = notesOnlineSyncEnabled() &&
                    SupabaseManager.client.auth.currentSessionOrNull()?.user?.id != null,
            onOpenNotesSettings = {
                startActivity(NotesSettingsComposeActivity.newIntent(this@MainActivity))
            },
            pageTitle = mainNotesFolderTitle(),
            showWelcomeOnEmptyNotes = false,
            folderMode = currentNotesFolderId == null,
            folders = noteFolderRows,
            bottomOverlayClearance = 60.dp,
            onFolderSelectionModeChanged = { active -> notesFolderSelectionActive = active },
            onRenameFolder = { folderId, name ->
                if (NoteFolderStore.renameFolder(this, folderId, name)) {
                    refreshNotesDisplay()
                    syncPendingNotesToSupabase()
                }
            },
            onSetFolderColor = { folderId, colorArgb ->
                if (NoteFolderStore.setFolderColor(this, folderId, colorArgb)) {
                    refreshNotesDisplay()
                }
            },
            onOpenFolder = { folderId ->
                currentNotesFolderId = folderId
                refreshNotesDisplay()
            }
        ) { folderName ->
            if (NoteFolderStore.createFolder(this, folderName) != null) {
                currentNotesFolderId = null
                refreshNotesDisplay()
            }
        }
    }
}
