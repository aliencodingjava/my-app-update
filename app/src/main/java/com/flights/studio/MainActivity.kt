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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
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

@Suppress("DEPRECATION")
class MainActivity : FragmentActivity() {

    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var notesAdapter: NotesAdapter

    private val allNotes = mutableListOf<String>()
    private val notesText = mutableStateListOf<String>()
    private val noteRows = mutableStateListOf<NoteRow>()
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
        ensureLocalUid(newNote)
        if (imageUris.isNotEmpty()) NoteMediaStore.setUris(this, newNote, imageUris)
        if (voiceItems.isNotEmpty()) NoteVoiceStore.setItems(this, newNote, voiceItems)
        if (fileItems.isNotEmpty()) NoteAttachmentStore.setItems(this, newNote, fileItems)
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

            MaterialTheme {
                Log.d(TAG_MAIN, "MaterialTheme: composition ENTER")

                val context = LocalContext.current
                val welcomePrefs = remember {
                    context.getSharedPreferences(MAIN_WELCOME_PREFS, MODE_PRIVATE)
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
                var dismissedEmergencyKey by rememberSaveable { mutableStateOf<String?>(null) }
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
                            delay(120)
                            showMenuSheet = true
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    emergencyMessage = EmergencyMessageRepository.fetch()
                }

                LaunchedEffect(pendingContactsInfoSheet) {
                    if (pendingContactsInfoSheet) {
                        delay(140)
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
                                !(selectedMainPage == PAGE_SETTINGS && settingsModalVisible)
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

                        if (!(selectedMainPage == PAGE_HOME && homeCameraExpanded)) {
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
                                dismissedEmergencyKey = emergencyMessage?.key
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
                            onDismiss = { reminderTimeNote = null },
                            onSetReminder = { hourOfDay, minute, dayOffset ->
                                reminderTimeNote?.let { note ->
                                    scheduleReminderUsingWorkManager(note, hourOfDay, minute, dayOffset)
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
        NoteMediaStore.setUris(this, updatedNote, updatedImages)
        NoteVoiceStore.setItems(this, updatedNote, updatedVoiceItems)
        NoteAttachmentStore.setItems(this, updatedNote, updatedFiles)
        if (updatedTitle.isNotBlank()) notesAdapter.setUserTitle(updatedNote, updatedTitle)
        else notesAdapter.removeUserTitle(updatedNote)

        refreshNotesDisplay()
        saveNotes()
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

    private fun refreshNotesDisplay() {
        val sorted = applyNotesSort(allNotes.toList())
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

        val filtered = allNotes.filter { note ->
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
            hint = "Search title or note"
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
            val attachmentCounts = countNoteAttachments(NoteAttachmentStore.getItems(this, text))
            noteRows.add(
                NoteRow(
                    id = key,
                    text = text,
                    imagesCount = NoteMediaStore.getUris(this, text).size,
                    attachmentsCount = attachmentCounts.documents,
                    audioCount = attachmentCounts.audio,
                    videoCount = attachmentCounts.video,
                    title = resolveTitle(text).orEmpty(),
                    hasReminder = getSharedPreferences("reminder_flags", MODE_PRIVATE)
                        .getBoolean(text.hashCode().toString(), false),
                    hasBadge = getSharedPreferences("reminder_badges", MODE_PRIVATE)
                        .getBoolean(text.hashCode().toString(), false)
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
        val selectedBaseKeys = selectedRowKeys.map { it.substringBefore('#') }.toSet()
        val toDelete = noteRows
            .filter { selectedBaseKeys.contains(it.id.substringBefore('#')) }
            .map { it.text }
            .toSet()

        if (toDelete.isEmpty()) return

        toDelete.forEach { queuePendingDelete(it) }
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
        isMultiSelectMode = false
        notesAdapter.clearSelection()
        refreshNotesDisplay()
        saveNotes()
    }

    private fun saveNotes() {
        sharedPreferences.edit {
            putString("notes_list", Gson().toJson(allNotes))
        }
    }

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
        if (SupabaseManager.client.auth.currentSessionOrNull()?.user?.id == null) return
        val pending = sharedPreferences
            .getStringSet("pending_deletes", emptySet())
            .orEmpty()
            .toMutableSet()
        if (pending.add(content)) {
            sharedPreferences.edit { putStringSet("pending_deletes", pending) }
        }
    }

    private fun scheduleNotesSyncRetry() {
        lifecycleScope.launch {
            delay(1200)
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
                val hasDeletes = sharedPreferences
                    .getStringSet("pending_deletes", emptySet())
                    .orEmpty()
                    .isNotEmpty()
                notesSyncStatus = if (hasDeletes) {
                    NotesSyncUiStatus.Deleting
                } else {
                    NotesSyncUiStatus.Syncing
                }
                syncPendingDeletesToSupabase(session.accessToken, userId)
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
        val pending = sharedPreferences
            .getStringSet("pending_deletes", emptySet())
            .orEmpty()
            .toMutableSet()
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

        sharedPreferences.edit {
            if (stillPending.isEmpty()) remove("pending_deletes")
            else putStringSet("pending_deletes", stillPending)
        }
    }

    private suspend fun syncLocalNotesToSupabase(authToken: String, userId: String) {
        val remoteRows = fetchActiveRemoteRows(userId)
        val remoteContents = remoteRows.map { it.content }.toSet()
        remoteRows.forEach { row ->
            row.id?.let { saveRemoteNoteId(it, row.content) }
        }

        val pendingAdds = sharedPreferences
            .getStringSet("pending_adds", emptySet())
            .orEmpty()
            .toMutableSet()
        val pendingDeletes = sharedPreferences
            .getStringSet("pending_deletes", emptySet())
            .orEmpty()
            .toSet()

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
        val remoteRows = fetchActiveRemoteRows(userId)
        val remoteExtras = fetchRemoteNoteExtras(remoteRows, authToken)
        val localSet = allNotes.toSet()
        var changed = false
        remoteRows.forEach { row ->
            row.id?.let { saveRemoteNoteId(it, row.content) }
            if (row.content !in localSet) {
                allNotes.add(row.content)
                changed = true
            }

            row.title?.takeIf { it.isNotBlank() }?.let { title ->
                notesAdapter.setUserTitle(row.content, title)
            } ?: notesAdapter.removeUserTitle(row.content)

            getSharedPreferences("reminder_flags", MODE_PRIVATE).edit {
                if (row.hasReminder) putBoolean(row.content.hashCode().toString(), true)
                else remove(row.content.hashCode().toString())
            }
            getSharedPreferences("reminder_badges", MODE_PRIVATE).edit {
                if (row.hasReminderBadge) putBoolean(row.content.hashCode().toString(), true)
                else remove(row.content.hashCode().toString())
            }

            remoteExtras.images[row.content]?.let { images ->
                if (images.isEmpty()) NoteMediaStore.deleteAllForNote(this, row.content)
                else NoteMediaStore.setUris(this, row.content, images)
            }
            remoteExtras.files[row.content]?.let { files ->
                NoteAttachmentStore.setItems(this, row.content, files)
            }
            remoteExtras.voice[row.content]?.let { voice ->
                NoteVoiceStore.setItems(this, row.content, voice)
            }
        }
        notesAdapter.preloadReminderFlags(this)
        notesAdapter.preloadBadgeStates(this)
        refreshNotesDisplay()
        if (changed) saveNotes()
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
        dayOffset: Int = 0
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
            .setInputData(workDataOf("note_key" to noteKey))
            .build()

        WorkManager.getInstance(this).enqueue(workRequest)
        val reminderKey = reminderKey(note)

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
        }

        notesAdapter.preloadBadgeStates(this)
        notesAdapter.preloadReminderFlags(this)
        refreshNotesDisplay()
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
        clearReminderMetadata(note, keepBell = true)
        notesAdapter.preloadBadgeStates(this)
        notesAdapter.preloadReminderFlags(this)
        refreshNotesDisplay()
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
        contentToUid.remove(note)?.let { uidToContent.remove(it) }
        saveUidMaps()
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
                kotlinx.coroutines.delay(24)
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
                kotlinx.coroutines.delay(24)
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
                        PAGE_NOTES -> NotesPage(
                            onOpenHome = onOpenHome,
                            onOpenContacts = onOpenContacts
                        )
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
            onOpenRateUs = {
                RateUsDialogFragment().show(supportFragmentManager, "RateUsDialog")
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
            val palette = rememberBriefingPalette(isDark)
            val pageColor = palette.page
            val textColor = palette.text
            val subTextColor = palette.subText
            val cardColor = palette.card
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
            LaunchedEffect(active, briefingWeatherEnabled) {
                var lastNativeRefreshAt = 0L
                while (active) {
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
                    delay(3_000L)
                }
            }
            val briefingWeather = remember(briefingWeatherJson, briefingWeatherEnabled) {
                val snapshot = parseBriefingWeatherSnapshot(briefingWeatherJson)
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                        .padding(start = 8.dp, end = 8.dp, top = 112.dp, bottom = 92.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = BriefingLabels.SUMMARY,
                        color = subTextColor,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp)
                    )

                    BriefingSmartCard(
                        title = "AI Brief",
                        insight = briefingInsight,
                        caption = if (aiCaption == "AI Brief") "Control tower" else aiCaption,
                        cardColor = aiCardColor,
                        borderColor = cardBorder,
                        accentColor = accentColor,
                        textColor = textColor,
                        subTextColor = subTextColor,
                        greetingTitle = briefingGreetingTitle,
                        friendlyMessage = briefingFriendlyMessage,
                    weather = briefingWeather,
                    weatherEnabled = briefingWeatherEnabled,
                    fallbackCondition = briefingFallbackWeatherCondition(briefingHour),
                    flightIssueCards = briefingAppContext.flightIssueCards,
                    flightSnapshot = flightBriefSnapshot,
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

                    BriefingSectionTitle("Airport")
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        BriefingActionCard(
                            title = "Flights",
                            body = "Open the live table without refreshing the whole app shell.",
                            icon = Icons.Filled.Flight,
                            cardColor = cardColor,
                            borderColor = cardBorder,
                            accentColor = accentColor,
                            textColor = textColor,
                            subTextColor = subTextColor,
                            onClick = onOpenFlights
                        )
                        BriefingActionCard(
                            title = "Live cameras",
                            body = "Check curb, north, and south airport camera views.",
                            icon = Icons.Filled.Info,
                            cardColor = cardColor,
                            borderColor = cardBorder,
                            accentColor = accentColor,
                            textColor = textColor,
                            subTextColor = subTextColor,
                            onClick = onOpenLiveCameras
                        )
                        BriefingActionCard(
                            title = "FBO services",
                            body = "Open Jackson Hole Flight Services.",
                            icon = Icons.Filled.Flight,
                            cardColor = cardColor,
                            borderColor = cardBorder,
                            accentColor = accentColor,
                            textColor = textColor,
                            subTextColor = subTextColor,
                            onClick = onOpenFbo
                        )
                        BriefingActionCard(
                            title = "News",
                            body = "Check airport updates from the same WebView behavior.",
                            icon = Icons.Filled.Info,
                            cardColor = cardColor,
                            borderColor = cardBorder,
                            accentColor = accentColor,
                            textColor = textColor,
                            subTextColor = subTextColor,
                            onClick = onOpenNews
                        )
                    }

                    BriefingSectionTitle("Your Trip")
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        BriefingActionCard(
                            title = "Notes",
                            body = "Jump back to saved notes, reminders, and travel details.",
                            icon = Icons.AutoMirrored.Filled.Article,
                            cardColor = cardColor,
                            borderColor = cardBorder,
                            accentColor = accentColor,
                            textColor = textColor,
                            subTextColor = subTextColor,
                            onClick = onOpenNotes
                        )
                        BriefingActionCard(
                            title = "Quick note",
                            body = "Capture a flight number, parking spot, or travel reminder.",
                            icon = Icons.Filled.Add,
                            cardColor = cardColor,
                            borderColor = cardBorder,
                            accentColor = accentColor,
                            textColor = textColor,
                            subTextColor = subTextColor,
                            onClick = onOpenAddNote
                        )
                    }

                    BriefingSectionTitle("Airport Info")
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        BriefingActionCard(
                            title = "Welcome",
                            body = "Open the airport welcome page.",
                            icon = Icons.Filled.Info,
                            cardColor = cardColor,
                            borderColor = cardBorder,
                            accentColor = accentColor,
                            textColor = textColor,
                            subTextColor = subTextColor,
                            onClick = onOpenWelcome
                        )
                        BriefingActionCard(
                            title = "About airport",
                            body = "History, pilot information, and airport details.",
                            icon = Icons.Filled.Info,
                            cardColor = cardColor,
                            borderColor = cardBorder,
                            accentColor = accentColor,
                            textColor = textColor,
                            subTextColor = subTextColor,
                            onClick = onOpenAbout
                        )
                        BriefingActionCard(
                            title = "Airport help",
                            body = "Open the official help and information page.",
                            icon = Icons.Filled.Info,
                            cardColor = cardColor,
                            borderColor = cardBorder,
                            accentColor = accentColor,
                            textColor = textColor,
                            subTextColor = subTextColor,
                            onClick = onOpenContact
                        )
                    }
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
    private fun rememberBriefingPalette(isDark: Boolean): BriefingPalette {
        return remember(isDark) {
            if (isDark) {
                BriefingPalette(
                    page = Color(0xFF111317),
                    card = Color(0xFF232425),
                    aiCard = Color(0xFF121C22),
                    border = Color(0xFF333538),
                    text = Color(0xFFF2F5F8),
                    subText = Color(0xFFB8C1CC),
                    accent = Color(0xFF8FD5FF),
                    warmAccent = Color(0xFFFFD166)
                )
            } else {
                BriefingPalette(
                    page = Color(0xFFF1F3F6),
                    card = Color(0xFFFEFEFE),
                    aiCard = Color(0xFFF8FBFF),
                    border = Color(0xFFE3E3E4),
                    text = Color(0xFF161718),
                    subText = Color(0xFF5F6670),
                    accent = Color(0xFF0A6DFF),
                    warmAccent = Color(0xFFD68A00)
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
            if (delayMillis > 0L) delay(delayMillis)
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
    private fun BriefingStatusCard(
        title: String,
        body: String,
        cardColor: Color,
        borderColor: Color,
        accentColor: Color,
        textColor: Color,
        subTextColor: Color,
        importantColor: Color
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .briefingElasticAppear("$title|$body", delayMillis = 35)
                .clip(RoundedCornerShape(20.dp))
                .background(cardColor)
                .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(20.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Text(
                    text = title,
                    color = textColor,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
            Text(
                text = briefingHighlightedText(
                    text = body,
                    importantColor = importantColor
                ),
                color = subTextColor,
                style = MaterialTheme.typography.bodyMedium
            )
        }
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

    private fun briefingAiFallback(
        noteCount: Int,
        recentNotes: List<BriefingNoteContext>,
        appContext: BriefingAppContext
    ): String {
        val latest = recentNotes.firstOrNull()
        if (appContext.badgeCount > 0) {
            return "You have ${appContext.badgeCount} reminder ${if (appContext.badgeCount == 1) "badge" else "badges"} waiting. Open Notes, then check Flights."
        }
        if (appContext.flightIssueCount > 0 && appContext.flightSummary.isNotBlank()) {
            return "${appContext.flightSummary} Open Flights for the affected cards."
        }
        if (appContext.flightSummary.contains("unavailable", ignoreCase = true) ||
            appContext.flightSummary.contains("not readable", ignoreCase = true)
        ) {
            return appContext.flightSummary
        }
        if (appContext.reminderCount > 0) {
            return "You have ${appContext.reminderCount} note ${if (appContext.reminderCount == 1) "reminder" else "reminders"} set. Review Notes, then check Flights or Live cameras."
        }
        if (appContext.contactsCount > 0 && noteCount <= 0) {
            return "${appContext.contactsCount} contacts are ready. Use Flights, Live cameras, or FBO services from this hub."
        }
        if (appContext.groupFlights || appContext.highContrastWeb) {
            val tableMode = if (appContext.groupFlights) "grouped" else "high-contrast"
            return "Flight table is in $tableMode mode. Open Flights for the clearest airport view."
        }
        if (latest != null) {
            val label = latest.title.ifBlank { latest.text }
                .replace(Regex("\\s+"), " ")
                .trim()
                .take(64)
            return "Latest note: $label. Open Notes to review it or check Flights."
        }

        return when {
            noteCount <= 0 -> "Flights, Live cameras, FBO services, and Quick note are ready from this hub."
            noteCount == 1 -> "You have 1 saved note. Check Flights first, then review that note."
            else -> "You have $noteCount saved notes. Check Flights, then review reminders and travel details."
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
                title = "Flight changes need eyes"
                action = BriefingInsightAction.Flights
                actionLabel = "Open Flights"
                localBody = "$criticalCount critical flight ${if (criticalCount == 1) "change" else "changes"} visible for today. Open Flights before anything else."
            }
            delayedCount > 0 -> {
                title = "Today needs Flights first"
                action = BriefingInsightAction.Flights
                actionLabel = "Open Flights"
                localBody = "$delayedCount delay ${if (delayedCount == 1) "is" else "are"} visible today. Check affected cards before cameras or notes."
            }
            appContext.badgeCount > 0 || appContext.reminderCount > 0 -> {
                val reminders = (appContext.badgeCount.takeIf { it > 0 } ?: appContext.reminderCount).coerceAtLeast(1)
                title = "Notes need a quick pass"
                action = BriefingInsightAction.Notes
                actionLabel = "Review Notes"
                localBody = "$reminders note ${if (reminders == 1) "signal is" else "signals are"} waiting. Review Notes, then check the airport view."
            }
            visualCondition == "night" || condition == "rain" || condition == "thunder" || condition == "fog" -> {
                title = "Check the field visually"
                action = BriefingInsightAction.Cameras
                actionLabel = "Open Cameras"
                localBody = "Weather and light make the cameras useful right now. Check curb, north, and south views."
            }
            noteCount <= 0 -> {
                title = "Set up your trip notes"
                action = BriefingInsightAction.QuickNote
                actionLabel = "Quick Note"
                localBody = "Flights and cameras are ready. Add a quick note for flight numbers, parking, or pickup details."
            }
            arrivalCount > 0 || departureCount > 0 -> {
                title = "Airport picture is ready"
                action = BriefingInsightAction.Flights
                actionLabel = "Open Flights"
                localBody = "Today shows ${briefingFlightCountText(arrivalCount, "arrival")} and ${briefingFlightCountText(departureCount, "departure")}. Open Flights for the live table."
            }
            else -> {
                title = "Airport tools are quiet"
                action = BriefingInsightAction.Fbo
                actionLabel = "Open FBO"
                localBody = "No flight pressure is visible. FBO services, cameras, and notes are ready from here."
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
            body = aiSentence.takeIf { it.isNotBlank() } ?: localBody,
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

    private fun parseBriefingWeatherSnapshot(json: String): BriefingWeatherSnapshot {
        if (json.isBlank()) return BriefingWeatherSnapshot()
        return runCatching {
            Gson().fromJson(json, BriefingWeatherSnapshot::class.java) ?: BriefingWeatherSnapshot()
        }.getOrDefault(BriefingWeatherSnapshot())
    }

    private fun briefingWeatherDisplayKey(json: String): String {
        val snapshot = parseBriefingWeatherSnapshot(json)
        return listOf(snapshot.temp, snapshot.condition, snapshot.summary, snapshot.source).joinToString("|")
    }

    private fun isLiveBriefingWeatherSnapshot(weather: BriefingWeatherSnapshot): Boolean {
        if (weather.updatedAt > 0L && System.currentTimeMillis() - weather.updatedAt > 2L * 60L * 60L * 1000L) {
            return false
        }
        if (weather.source == "airport_web") return true
        if (weather.temp.isBlank()) return false
        if (weather.source == "airport_web" || weather.source == "open_meteo") return true
        return weather.source.isBlank() && weather.temp.contains("/")
    }

    private fun resolvedBriefingWeatherCondition(weather: BriefingWeatherSnapshot): String {
        val raw = when (weather.condition.trim().lowercase()) {
            "clear" -> "sunny"
            "storm" -> "thunder"
            else -> weather.condition.trim().lowercase()
        }
        val cloudPercent = Regex("""Cloud\s+(\d+)%""", RegexOption.IGNORE_CASE)
            .find(weather.summary)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()

        if (raw == "rain" || raw == "thunder" || raw == "fog") {
            return raw
        }
        val resolved = if (cloudPercent != null) {
            when {
                cloudPercent >= 70 -> "cloudy"
                cloudPercent >= 30 -> "partly"
                else -> raw.ifBlank { "sunny" }
            }
        } else {
            raw.ifBlank { "sunny" }
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

    private data class BriefingDayFlightCount(
        val label: String,
        val arrivals: Int,
        val departures: Int
    )

    private fun briefingDayFlightCounts(summary: String): List<BriefingDayFlightCount> {
        return Regex("""([^.:]+?):\s*(\d+)\s+arrivals?,\s*(\d+)\s+departures?""", RegexOption.IGNORE_CASE)
            .findAll(summary)
            .mapNotNull { match ->
                val label = match.groupValues.getOrNull(1).orEmpty().trim()
                val arrivals = match.groupValues.getOrNull(2)?.toIntOrNull()
                val departures = match.groupValues.getOrNull(3)?.toIntOrNull()
                if (label.isBlank() || arrivals == null || departures == null) {
                    null
                } else {
                    BriefingDayFlightCount(label, arrivals, departures)
                }
            }
            .toList()
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
            "night"
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
                "A low-light briefing for the essentials: flights, notes, cameras, and anything that needs attention.",
                "Quiet mode is on. The airport tools stay close without crowding the screen.",
                "A calm overnight check-in, organized around what you may need next."
            )
            4 -> listOf(
                "A focused start before the day gets noisy: check flights, then notes, then the live view.",
                "Early start, clean path. The brief is ready to point you toward the next useful action.",
                "The airport day is opening up; your tools are grouped and ready."
            )
            in 5..10 -> listOf(
                "Start with the live flight picture, then use notes and cameras only if something needs a closer look.",
                "A clean morning read of the app: airport status, personal notes, and quick actions in one place.",
                "The morning brief is tuned for scanning first, acting second."
            )
            11 -> listOf(
                "A late-morning check-in with the noisy parts reduced to a few useful signals.",
                "Use this as the quick pass: flight table, cameras, notes, and airport info.",
                "The hub is ready for a fast read before the day shifts again."
            )
            in 12..14 -> listOf(
                "A midday reset: surface what changed, keep the rest quiet.",
                "Flights, notes, and airport tools are arranged for a quick second look.",
                "A compact read for the middle of the day, with the next action close by."
            )
            in 15..16 -> listOf(
                "An afternoon pass over the details that can still change: flights, reminders, and cameras.",
                "Keep the next step simple. The brief separates signal from background.",
                "Your afternoon airport tools are ready without pulling you through extra screens."
            )
            in 17..20 -> listOf(
                "An evening check that keeps the important airport details visible and the rest calm.",
                "A softer read before the day winds down: flights first, notes if needed.",
                "Review the live picture, then leave the noise behind."
            )
            else -> listOf(
                "A night briefing with the brightness lowered and the useful pieces still easy to reach.",
                "Only the essentials stay forward: airport tools, notes, and tomorrow’s quick path.",
                "Wind down with a cleaner view of what is ready for the next airport check."
            )
        }
        return messages[slot.floorMod(messages.size)]
    }

    private fun Int.floorMod(divisor: Int): Int = ((this % divisor) + divisor) % divisor

    private fun briefingHighlightedText(
        text: String,
        importantColor: Color
    ): AnnotatedString {
        val importantText = Regex(
            pattern = "\\b\\d+\\s+saved\\s+notes?\\b|" +
                "\\bFlights\\b|" +
                "\\bLive cameras\\b|" +
                "\\bFBO services\\b|" +
                "\\bNews\\b|" +
                "\\bNotes\\b|" +
                "\\bQuick note\\b|" +
                "\\bSettings\\b|" +
                "\\bAirport tools\\b|" +
                "\\bLatest note\\b",
            option = RegexOption.IGNORE_CASE
        )

        return buildAnnotatedString {
            var cursor = 0
            importantText.findAll(text).forEach { match ->
                if (match.range.first > cursor) {
                    append(text.substring(cursor, match.range.first))
                }
                withStyle(
                    SpanStyle(
                        color = importantColor,
                        fontWeight = FontWeight.SemiBold
                    )
                ) {
                    append(match.value)
                }
                cursor = match.range.last + 1
            }
            if (cursor < text.length) {
                append(text.substring(cursor))
            }
        }
    }

    @Composable
    private fun BriefingSmartCard(
        title: String,
        insight: BriefingInsight,
        caption: String,
        cardColor: Color,
        borderColor: Color,
        accentColor: Color,
        textColor: Color,
        subTextColor: Color,
        greetingTitle: String,
        friendlyMessage: String,
        weather: BriefingWeatherSnapshot,
        weatherEnabled: Boolean,
        fallbackCondition: String,
        flightIssueCards: List<BriefingFlightIssueCard>,
        flightSnapshot: BriefingFlightSnapshot,
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(cardColor.copy(alpha = 0.96f))
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
            if (weatherEnabled || DEBUG_FORCE_BRIEFING_RAIN || DEBUG_FORCE_BRIEFING_THUNDER || DEBUG_FORCE_BRIEFING_SUN) {
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
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = greetingTitle,
                            color = textColor,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.sp
                            ),
                            maxLines = 1
                        )
                        Text(
                            text = friendlyMessage,
                            color = subTextColor,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 17.sp,
                                letterSpacing = 0.sp
                            ),
                            maxLines = 2
                        )
                    }
                    if (weatherEnabled) {
                        BriefingWeatherPill(
                            weather = weather,
                            accentColor = accentColor,
                            textColor = textColor,
                            subTextColor = subTextColor
                        )
                    } else if (playEffects) {
                        BriefingAiWave(accentColor = accentColor)
                    }
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
                    BriefingAiWave(accentColor = accentColor)
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
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 19.sp,
                        letterSpacing = 0.sp
                    ),
                    maxLines = 2
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(accentColor.copy(alpha = 0.18f))
                        .clickable { onAction(state.action) }
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = state.actionLabel,
                        color = textColor,
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
        val chipColor = when (chip.tone) {
            "critical" -> Color(0xFFFF6B6B)
            "warning" -> Color(0xFFFACC15)
            else -> accentColor
        }
        Row(
            modifier = modifier
                .height(30.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(chipColor.copy(alpha = 0.14f))
                .padding(horizontal = 7.dp),
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
                    color = textColor,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                    maxLines = 1
                )
            }
            Text(
                text = " ${chip.label}",
                color = textColor.copy(alpha = 0.72f),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1
            )
        }
    }

    @Composable
    private fun BriefingFlightOverviewCard(
        snapshot: BriefingFlightSnapshot,
        textColor: Color,
        subTextColor: Color,
        accentColor: Color
    ) {
        if (snapshot.summary.isBlank()) return
        val dayCounts = briefingDayFlightCounts(snapshot.summary)
        val todayCounts = dayCounts.firstOrNull()
        val arrivalCount = todayCounts?.arrivals
            ?: snapshot.arrivalCount.takeIf { it > 0 }
            ?: countFromSummary(snapshot.summary, "arrival")
        val departureCount = todayCounts?.departures
            ?: snapshot.departureCount.takeIf { it > 0 }
            ?: countFromSummary(snapshot.summary, "departure")
        val delayedCount = snapshot.delayedCount
        val cancelledCount = snapshot.cancelledCount
        val divertedCount = snapshot.divertedCount
        val isUnavailable = snapshot.source == "native_unavailable" ||
            snapshot.summary.contains("unavailable", ignoreCase = true)
        val dateLabel = when {
            isUnavailable -> "Flights"
            todayCounts != null -> todayCounts.label
            else -> snapshot.summary
                .substringBefore(":")
                .takeIf { it.length < snapshot.summary.length && it.isNotBlank() }
                ?: "Flights"
        }
        val detail = if (isUnavailable) {
            "Open Flights to sync the live table."
        } else {
            "${briefingFlightCountText(arrivalCount, "arrival")}, ${briefingFlightCountText(departureCount, "departure")}."
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(accentColor.copy(alpha = 0.10f))
                .padding(horizontal = 11.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = dateLabel,
                    color = textColor,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (isUnavailable) "Open Flights" else "Synced",
                    color = accentColor,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                    maxLines = 1
                )
            }
            Text(
                text = detail,
                color = subTextColor,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 2
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BriefingFlightCountPill("$arrivalCount", "Arr", accentColor, textColor, Modifier.weight(1f))
                BriefingFlightCountPill("$departureCount", "Dep", accentColor, textColor, Modifier.weight(1f))
                BriefingFlightCountPill("$delayedCount", "Delay", Color(0xFFFACC15), textColor, Modifier.weight(1f))
                BriefingFlightCountPill("${cancelledCount + divertedCount}", "Critical", Color(0xFFFF6B6B), textColor, Modifier.weight(1f))
            }
        }
    }

    @Composable
    private fun BriefingFlightCountPill(
        value: String,
        label: String,
        color: Color,
        textColor: Color,
        modifier: Modifier = Modifier
    ) {
        Row(
            modifier = modifier
                .height(28.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(color.copy(alpha = 0.14f))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                color = textColor,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                maxLines = 1
            )
            Text(
                text = " $label",
                color = textColor.copy(alpha = 0.72f),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
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
        val route = issue.route
        val direction = when {
            route.startsWith("JAC to", ignoreCase = true) -> "Outbound"
            route.endsWith("to JAC", ignoreCase = true) -> "Inbound"
            else -> if (issue.tone.equals("cancelled", true) || issue.tone.equals("diverted", true)) "Flight" else "Delay"
        }
        val toneColor = when (issue.tone.lowercase()) {
            "cancelled" -> Color(0xFFFF6B6B)
            "diverted" -> Color(0xFFA78BFA)
            else -> Color(0xFFFACC15)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(toneColor.copy(alpha = 0.12f))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(toneColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Flight,
                    contentDescription = null,
                    tint = toneColor,
                    modifier = Modifier.size(16.dp)
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
                    color = subTextColor,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1
                )
            }
        }
    }

    @Composable
    private fun BriefingWeatherPanel(
        weather: BriefingWeatherSnapshot,
        weatherEnabled: Boolean,
        fallbackCondition: String,
        accentColor: Color,
        textColor: Color,
        subTextColor: Color
    ) {
        if (!weatherEnabled) return
        val hasRealAirportWeather = weather.temp.isNotBlank()
        val hasWeatherEffects = hasRealAirportWeather || weather.source == "airport_web" ||
            DEBUG_FORCE_BRIEFING_RAIN || DEBUG_FORCE_BRIEFING_THUNDER || DEBUG_FORCE_BRIEFING_SUN
        val condition = when {
            DEBUG_FORCE_BRIEFING_SUN -> "sunny"
            DEBUG_FORCE_BRIEFING_THUNDER -> "thunder"
            DEBUG_FORCE_BRIEFING_RAIN -> "rain"
            hasWeatherEffects -> resolvedBriefingWeatherCondition(weather)
            else -> fallbackCondition
        }
        val visualCondition = briefingWeatherVisualCondition(condition)
        val conditionLabel = briefingWeatherConditionLabel(visualCondition)
        val tempParts = if (hasRealAirportWeather) {
            briefingWeatherTempParts(weather.temp, conditionLabel)
        } else {
            BriefingWeatherTempParts("--", "Airport weather")
        }
        val isDark = isSystemInDarkTheme()
        val summary = weather.summary
            .split("•")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("  ")
        val sentence = if (hasRealAirportWeather) {
            val sentenceDetail = tempParts.secondary.ifBlank { tempParts.main }
            "$conditionLabel. $sentenceDetail."
        } else {
            "Airport weather --"
        }
        val weatherUiState = BriefingWeatherPanelState(
            condition = visualCondition,
            visualCondition = visualCondition,
            conditionLabel = conditionLabel,
            mainTemp = tempParts.main,
            secondaryTemp = tempParts.secondary,
            summary = summary,
            sentence = sentence
        )
        var displayedWeatherUiState by remember { mutableStateOf(weatherUiState) }
        LaunchedEffect(weatherUiState) {
            if (displayedWeatherUiState == weatherUiState) return@LaunchedEffect
            delay(220L)
            displayedWeatherUiState = weatherUiState
        }
        AnimatedContent(
            targetState = displayedWeatherUiState,
            transitionSpec = {
                fadeIn(animationSpec = tween(980, easing = FastOutSlowInEasing)) +
                    slideInVertically(
                        animationSpec = tween(980, easing = FastOutSlowInEasing),
                        initialOffsetY = { it / 5 }
                    ) +
                    scaleIn(initialScale = 0.985f, animationSpec = tween(980, easing = FastOutSlowInEasing)) togetherWith
                    fadeOut(animationSpec = tween(520, easing = FastOutSlowInEasing)) +
                    slideOutVertically(
                        animationSpec = tween(520, easing = FastOutSlowInEasing),
                        targetOffsetY = { -it / 7 }
                    ) +
                    scaleOut(targetScale = 1.01f, animationSpec = tween(520, easing = FastOutSlowInEasing))
            },
            label = "briefingWeatherPanel"
        ) { state ->
            val animatedIconTint = when (state.visualCondition) {
                "sunny", "partly" -> Color(0xFFFACC15)
                "rain" -> Color(0xFF38BDF8)
                "thunder" -> Color(0xFFFFE066)
                "cloudy", "fog" -> if (isDark) Color(0xFFE2E8F0) else Color(0xFF64748B)
                "night" -> Color(0xFFDCEBFF)
                else -> accentColor
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = state.sentence,
                    color = textColor,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.sp
                    ),
                    maxLines = 1
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 1.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = state.mainTemp,
                        color = textColor.copy(alpha = if (isDark) 0.95f else 0.90f),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Light,
                            lineHeight = 42.sp,
                            letterSpacing = 0.sp
                        ),
                        maxLines = 1
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = listOf(state.conditionLabel, state.secondaryTemp).filter { it.isNotBlank() }.joinToString(" / "),
                            color = textColor.copy(alpha = if (isDark) 0.88f else 0.80f),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.sp
                            ),
                            maxLines = 1
                        )
                        Text(
                            text = listOf("Jackson Hole", state.summary).filter { it.isNotBlank() }.joinToString("  "),
                            color = subTextColor,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.sp
                            ),
                            maxLines = 1
                        )
                    }
                    BriefingWeatherIcon(
                        condition = state.visualCondition,
                        tint = animatedIconTint,
                        modifier = Modifier.size(52.dp)
                    )
                }
            }
        }
    }

    private data class BriefingWeatherPanelState(
        val condition: String,
        val visualCondition: String,
        val conditionLabel: String,
        val mainTemp: String,
        val secondaryTemp: String,
        val summary: String,
        val sentence: String
    )

    private data class BriefingWeatherTempParts(
        val main: String,
        val secondary: String
    )

    private fun briefingWeatherTempParts(temp: String, conditionLabel: String): BriefingWeatherTempParts {
        if (temp.isBlank()) return BriefingWeatherTempParts(conditionLabel, "")
        val pieces = temp.split("/").map { it.trim() }.filter { it.isNotBlank() }
        val celsius = pieces.firstOrNull { it.contains("C", ignoreCase = true) }
        val fahrenheit = pieces.firstOrNull { it.contains("F", ignoreCase = true) }
        val mainSource = celsius ?: pieces.firstOrNull() ?: temp
        val mainNumber = Regex("""-?\d+""").find(mainSource)?.value
        val main = if (mainNumber != null) "$mainNumber°" else mainSource
        val secondary = listOfNotNull(
            fahrenheit?.takeUnless { it == mainSource },
            celsius?.takeUnless { it == mainSource }
        ).firstOrNull().orEmpty()
        return BriefingWeatherTempParts(main, secondary)
    }

    private fun briefingWeatherConditionLabel(condition: String): String {
        return when (condition) {
            "thunder" -> "Storm"
            "rain" -> "Rain"
            "fog" -> "Fog"
            "night" -> "Night"
            "cloudy" -> "Cloudy"
            "partly" -> "Partly cloudy"
            else -> "Sunny"
        }
    }

    @Composable
    private fun BriefingWeatherPill(
        weather: BriefingWeatherSnapshot,
        accentColor: Color,
        textColor: Color,
        subTextColor: Color
    ) {
        if (weather.temp.isBlank() && !DEBUG_FORCE_BRIEFING_RAIN && !DEBUG_FORCE_BRIEFING_THUNDER && !DEBUG_FORCE_BRIEFING_SUN) return
        val condition = when {
            DEBUG_FORCE_BRIEFING_SUN -> "sunny"
            DEBUG_FORCE_BRIEFING_THUNDER -> "thunder"
            DEBUG_FORCE_BRIEFING_RAIN -> "rain"
            else -> resolvedBriefingWeatherCondition(weather)
        }
        val visualCondition = briefingWeatherVisualCondition(condition)
        val displayTemp = when {
            DEBUG_FORCE_BRIEFING_SUN && weather.temp.isBlank() -> "Sun"
            DEBUG_FORCE_BRIEFING_THUNDER && weather.temp.isBlank() -> "Storm"
            DEBUG_FORCE_BRIEFING_RAIN && weather.temp.isBlank() -> "Rain"
            else -> weather.temp
        }
        val conditionLabel = briefingWeatherConditionLabel(visualCondition)
        val isDark = isSystemInDarkTheme()
        val conditionColor = when (visualCondition) {
            "sunny", "partly" -> Color(0xFFFACC15)
            "thunder" -> Color(0xFFFFE066)
            "rain" -> Color(0xFF38BDF8)
            "fog", "cloudy" -> if (isDark) Color(0xFFD8E2EE) else Color(0xFF64748B)
            "night" -> Color(0xFFBFD7FF)
            else -> accentColor
        }
        AnimatedContent(
            targetState = Triple(visualCondition, displayTemp, conditionLabel),
            transitionSpec = {
                fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing)) +
                    scaleIn(initialScale = 0.94f, animationSpec = spring(dampingRatio = 0.72f, stiffness = 460f)) togetherWith
                    fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
                    scaleOut(targetScale = 0.96f, animationSpec = tween(180, easing = FastOutSlowInEasing))
            },
            label = "briefingWeatherPill"
        ) { state ->
            val animatedCondition = state.first
            val animatedTemp = state.second
            val animatedLabel = state.third
            Row(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(conditionColor.copy(alpha = if (isDark) 0.16f else 0.18f))
                .padding(horizontal = 9.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            BriefingWeatherIcon(
                condition = animatedCondition,
                tint = conditionColor,
                modifier = Modifier.size(25.dp)
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = animatedTemp,
                    color = textColor,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                    maxLines = 1
                )
                Text(
                    text = animatedLabel,
                    color = subTextColor,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1
                )
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
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height
            val sunColor = Color(0xFFFACC15)
            val moonColor = Color(0xFFE8EEFF)
            val cloudColor = tint.copy(alpha = 0.88f)
            val rainColor = Color(0xFF38BDF8)
            if (condition == "night") {
                val c = Offset(w * 0.42f, h * 0.40f)
                val r = w * 0.22f
                drawCircle(moonColor.copy(alpha = 0.28f), radius = r * 1.55f, center = c)
                drawCircle(moonColor, radius = r, center = c)
                drawCircle(Color.Transparent, radius = r * 0.92f, center = Offset(c.x + r * 0.45f, c.y - r * 0.18f))
                drawCircle(moonColor.copy(alpha = 0.80f), radius = w * 0.035f, center = Offset(w * 0.72f, h * 0.24f))
                drawCircle(moonColor.copy(alpha = 0.62f), radius = w * 0.026f, center = Offset(w * 0.78f, h * 0.44f))
                return@Canvas
            }
            if (condition == "sunny" || condition == "partly") {
                val c = Offset(w * 0.38f, h * 0.36f)
                val r = w * 0.18f
                drawCircle(sunColor.copy(alpha = 0.24f), radius = r * 1.7f, center = c)
                drawCircle(sunColor, radius = r, center = c)
                for (i in 0 until 8) {
                    val a = (Math.PI * 2.0 * i / 8.0).toFloat()
                    drawLine(
                        color = sunColor.copy(alpha = 0.82f),
                        start = Offset(c.x + cos(a) * r * 1.35f, c.y + sin(a) * r * 1.35f),
                        end = Offset(c.x + cos(a) * r * 1.85f, c.y + sin(a) * r * 1.85f),
                        strokeWidth = 2.1f,
                        cap = StrokeCap.Round
                    )
                }
            }
            if (condition != "sunny") {
                drawCircle(cloudColor, radius = w * 0.18f, center = Offset(w * 0.42f, h * 0.50f))
                drawCircle(cloudColor, radius = w * 0.22f, center = Offset(w * 0.58f, h * 0.45f))
                drawCircle(cloudColor, radius = w * 0.16f, center = Offset(w * 0.72f, h * 0.55f))
                drawLine(
                    color = cloudColor,
                    start = Offset(w * 0.32f, h * 0.64f),
                    end = Offset(w * 0.78f, h * 0.64f),
                    strokeWidth = h * 0.20f,
                    cap = StrokeCap.Round
                )
            }
            if (condition == "rain") {
                listOf(0.42f, 0.58f, 0.74f).forEach { x ->
                    drawLine(
                        color = rainColor,
                        start = Offset(w * x, h * 0.70f),
                        end = Offset(w * (x - 0.06f), h * 0.92f),
                        strokeWidth = 2.2f,
                        cap = StrokeCap.Round
                    )
                }
            }
            if (condition == "thunder") {
                val bolt = Color(0xFFFFF3A3)
                val points = listOf(
                    Offset(w * 0.61f, h * 0.23f),
                    Offset(w * 0.50f, h * 0.52f),
                    Offset(w * 0.61f, h * 0.50f),
                    Offset(w * 0.48f, h * 0.86f)
                )
                drawLine(bolt.copy(alpha = 0.62f), points[0], points[1], strokeWidth = 2.7f, cap = StrokeCap.Round)
                drawLine(bolt.copy(alpha = 0.62f), points[1], points[2], strokeWidth = 2.7f, cap = StrokeCap.Round)
                drawLine(bolt.copy(alpha = 0.62f), points[2], points[3], strokeWidth = 2.7f, cap = StrokeCap.Round)
            }
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
        val horizontalDrift = kotlin.math.abs(dirX) * heightPx / dirY
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
            delay(80)
            playAurora = true
            delay(7_200)
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

    @Composable
    private fun BriefingOpeningText(
        importantText: String,
        baseColor: Color,
        animateEffects: Boolean,
        modifier: Modifier = Modifier,
        style: androidx.compose.ui.text.TextStyle
    ) {
        val isDark = isSystemInDarkTheme()
        val density = LocalDensity.current
        val finalText = if (isDark) Color.White else Color(0xFF111111)
        var playAurora by remember(importantText) { mutableStateOf(false) }
        var fadeAurora by remember(importantText) { mutableStateOf(false) }
        val firstPhase by animateFloatAsState(
            targetValue = if (playAurora) 1f else 0f,
            animationSpec = tween(durationMillis = 3600, easing = FastOutSlowInEasing),
            label = "briefingAuroraLayer1"
        )
        val secondPhase by animateFloatAsState(
            targetValue = if (playAurora) 1f else 0f,
            animationSpec = tween(durationMillis = 4200, easing = FastOutSlowInEasing),
            label = "briefingAuroraLayer2"
        )
        val thirdPhase by animateFloatAsState(
            targetValue = if (playAurora) 1f else 0f,
            animationSpec = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
            label = "briefingAuroraLayer3"
        )
        val fourthPhase by animateFloatAsState(
            targetValue = if (playAurora) 1f else 0f,
            animationSpec = tween(durationMillis = 5200, easing = FastOutSlowInEasing),
            label = "briefingAuroraLayer4"
        )
        val fadeProgress by animateFloatAsState(
            targetValue = if (!animateEffects || fadeAurora) 1f else 0f,
            animationSpec = tween(durationMillis = 620, easing = FastOutSlowInEasing),
            label = "briefingAuroraFadeToWhite"
        )
        val auroraAlpha = if (animateEffects) 1f - fadeProgress else 0f
        val finalAlpha = if (animateEffects) (0.28f + fadeProgress * 0.72f).coerceIn(0f, 1f) else 1f

        LaunchedEffect(importantText, animateEffects) {
            if (!animateEffects) {
                playAurora = false
                fadeAurora = true
                return@LaunchedEffect
            }
            playAurora = false
            fadeAurora = false
            delay(80)
            playAurora = true
            delay(4_300)
            fadeAurora = true
        }

        BoxWithConstraints(modifier = modifier) {
            val widthPx = with(density) { maxWidth.toPx().coerceAtLeast(1f) }
            val heightPx = with(density) { 72.dp.toPx() }

            Text(
                text = importantText,
                modifier = Modifier.fillMaxWidth(),
                color = finalText.copy(alpha = finalAlpha),
                style = style.copy(
                    shadow = Shadow(
                        color = finalText.copy(alpha = if (isDark) 0.16f * finalAlpha else 0.02f * finalAlpha),
                        offset = Offset.Zero,
                        blurRadius = if (isDark) 7f else 0f
                    )
                )
            )
            if (animateEffects && auroraAlpha > 0.01f) {
                AuroraTextLayer(
                    text = importantText,
                    style = style,
                    color = Color(0xFF22D3EE),
                    center = auroraLayerCenter(firstPhase, 1, widthPx, heightPx),
                    radius = widthPx * 0.60f,
                    alpha = auroraAlpha,
                    glowBlur = if (isDark) 22f else 15f
                )
                AuroraTextLayer(
                    text = importantText,
                    style = style,
                    color = Color(0xFFFACC15),
                    center = auroraLayerCenter(secondPhase, 2, widthPx, heightPx),
                    radius = widthPx * 0.60f,
                    alpha = auroraAlpha,
                    glowBlur = if (isDark) 26f else 18f
                )
                AuroraTextLayer(
                    text = importantText,
                    style = style,
                    color = Color(0xFF4ADE80),
                    center = auroraLayerCenter(thirdPhase, 3, widthPx, heightPx),
                    radius = widthPx * 0.60f,
                    alpha = auroraAlpha,
                    glowBlur = if (isDark) 20f else 14f
                )
                AuroraTextLayer(
                    text = importantText,
                    style = style,
                    color = Color(0xFF173EFF),
                    center = auroraLayerCenter(fourthPhase, 4, widthPx, heightPx),
                    radius = widthPx * 0.60f,
                    alpha = auroraAlpha,
                    glowBlur = if (isDark) 24f else 16f
                )
                Text(
                    text = importantText,
                    modifier = Modifier.fillMaxWidth(),
                    color = finalText.copy(alpha = finalAlpha * 0.36f),
                    style = style.copy(
                        shadow = Shadow(
                            color = finalText.copy(alpha = if (isDark) 0.20f * auroraAlpha else 0.04f * auroraAlpha),
                            offset = Offset.Zero,
                            blurRadius = if (isDark) 9f else 2f
                        )
                    )
                )
            }
        }
    }

    @Composable
    private fun AuroraTextLayer(
        text: String,
        style: androidx.compose.ui.text.TextStyle,
        color: Color,
        center: Offset,
        radius: Float,
        alpha: Float,
        glowBlur: Float
    ) {
        val brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = alpha),
                color.copy(alpha = alpha * 0.74f),
                color.copy(alpha = alpha * 0.24f),
                Color.Transparent
            ),
            center = center,
            radius = radius
        )
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth(),
            color = Color.Unspecified,
            style = style.copy(
                brush = brush,
                shadow = Shadow(
                    color = color.copy(alpha = alpha * 0.42f),
                    offset = Offset.Zero,
                    blurRadius = glowBlur
                )
            )
        )
    }

    private fun auroraLayerCenter(progress: Float, layer: Int, width: Float, height: Float): Offset {
        val p = progress.coerceIn(0f, 1f)
        return when (layer) {
            1 -> {
                val point = interpolateKeyframes(
                    p,
                    0f to Offset(width * 1.02f, height * 0.02f),
                    0.50f to Offset(width * 0.25f, height * 1.08f),
                    0.75f to Offset(width * 0.75f, height * 1.04f),
                    1f to Offset(width * 1.02f, height * 0.02f)
                )
                point
            }
            2 -> interpolateKeyframes(
                p,
                0f to Offset(width * 0.00f, height * -0.44f),
                0.60f to Offset(width * 0.75f, height * 1.08f),
                0.85f to Offset(width * 0.25f, height * 1.04f),
                1f to Offset(width * 0.00f, height * -0.44f)
            )
            3 -> interpolateKeyframes(
                p,
                0f to Offset(width * 0.02f, height * 1.02f),
                0.40f to Offset(width * 0.75f, height * -0.08f),
                0.65f to Offset(width * 0.50f, height * 0.60f),
                1f to Offset(width * 0.02f, height * 1.02f)
            )
            else -> interpolateKeyframes(
                p,
                0f to Offset(width * 1.02f, height * 1.42f),
                0.50f to Offset(width * 0.60f, height * 1.00f),
                0.90f to Offset(width * 0.75f, height * 0.50f),
                1f to Offset(width * 1.02f, height * 1.42f)
            )
        }
    }

    private fun interpolateKeyframes(progress: Float, vararg frames: Pair<Float, Offset>): Offset {
        val p = progress.coerceIn(0f, 1f)
        for (index in 0 until frames.lastIndex) {
            val start = frames[index]
            val end = frames[index + 1]
            if (p >= start.first && p <= end.first) {
                val local = ((p - start.first) / (end.first - start.first).coerceAtLeast(0.0001f))
                    .coerceIn(0f, 1f)
                val eased = smoothStep(local)
                return Offset(
                    x = start.second.x + (end.second.x - start.second.x) * eased,
                    y = start.second.y + (end.second.y - start.second.y) * eased
                )
            }
        }
        return frames.last().second
    }

    private fun smoothStep(value: Float): Float {
        val x = value.coerceIn(0f, 1f)
        return x * x * (3f - 2f * x)
    }

    @Composable
    private fun BriefingAiWave(
        accentColor: Color,
        modifier: Modifier = Modifier
    ) {
        val transition = rememberInfiniteTransition(label = "briefingAiWave")
        val phase by transition.animateFloat(
            initialValue = 0f,
            targetValue = (Math.PI * 2.0).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1250, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "briefingAiWavePhase"
        )

        Canvas(
            modifier = modifier
                .size(width = 46.dp, height = 22.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accentColor.copy(alpha = 0.10f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            val bars = 5
            val strokeWidth = 3.dp.toPx()
            val spacing = size.width / (bars - 1).coerceAtLeast(1)
            val centerY = size.height / 2f
            for (index in 0 until bars) {
                val wave = ((sin(phase + index * 0.7f) + 1f) / 2f).coerceIn(0f, 1f)
                val barHeight = size.height * (0.34f + wave * 0.62f)
                val x = index * spacing
                drawLine(
                    color = accentColor.copy(alpha = 0.42f + wave * 0.48f),
                    start = Offset(x, centerY - barHeight / 2f),
                    end = Offset(x, centerY + barHeight / 2f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
        }
    }

    @Composable
    private fun BriefingActionCard(
        title: String,
        body: String,
        icon: ImageVector,
        cardColor: Color,
        borderColor: Color,
        accentColor: Color,
        textColor: Color,
        subTextColor: Color,
        onClick: () -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .briefingElasticAppear(title, delayMillis = 70)
                .clip(RoundedCornerShape(18.dp))
                .background(cardColor)
                .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(18.dp))
                .clickable(onClick = onClick)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = title,
                    color = textColor,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = body,
                    color = subTextColor,
                    style = MaterialTheme.typography.bodySmall
                )
            }
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
            enter = androidx.compose.animation.slideInVertically { -it } + fadeIn(),
            exit = androidx.compose.animation.slideOutVertically { -it } + fadeOut(),
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
        onOpenHome: () -> Unit,
        onOpenContacts: () -> Unit,
    ) {
        LaunchedEffect(Unit) {
            notesAdapter.preloadBadgeStates(this@MainActivity)
            notesAdapter.preloadReminderFlags(this@MainActivity)
            refreshNotesDisplay()
            syncPendingNotesToSupabase()
        }

        AllNotesScreen(
            notesAdapter = notesAdapter,
            notes = noteRows,
            notesSize = notesCount.intValue,
            onAddNote = {
                addNoteLauncher.launch(
                    AddNoteComposeActivity.newIntent(this),
                    ActivityOptionsCompat.makeSceneTransitionAnimation(this)
                )
            },
            onOpenSearch = { dismiss -> openNotesSearchSheet(dismiss) },
            onNavItemClick = { id ->
                when (id) {
                    R.id.nav_home -> onOpenHome()
                    R.id.nav_contacts,
                    R.id.nav_all_contacts -> onOpenContacts()
                    R.id.nav_settings -> {
                        startActivity(NotesSettingsComposeActivity.newIntent(this@MainActivity))
                        overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation)
                    }
                }
            },
            onDeleteSelected = ::deleteSelectedNotes,
            onOpenNote = { row, position -> onNoteClick(row.text, position) },
            onBack = null,
            syncStatus = notesSyncStatus
        )
    }
}
