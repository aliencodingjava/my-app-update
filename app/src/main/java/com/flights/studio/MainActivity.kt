package com.flights.studio

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.CalendarContract
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.ViewGroup
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.flights.studio.ui.AppLanguageManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID
import java.util.concurrent.TimeUnit

@Suppress("DEPRECATION")
class MainActivity : FragmentActivity() {

    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var notesAdapter: NotesAdapter

    private val allNotes = mutableListOf<String>()
    private val notesText = mutableStateListOf<String>()
    private val noteRows = mutableStateListOf<NoteRow>()
    private val notesCount = mutableIntStateOf(0)
    private val uidToContent = mutableMapOf<String, String>()
    private val contentToUid = mutableMapOf<String, String>()
    private val sharedPreferences by lazy { getSharedPreferences("notes_prefs", MODE_PRIVATE) }
    private val uidPrefs by lazy { getSharedPreferences("notes_uids", MODE_PRIVATE) }
    private var isMultiSelectMode = false
    private var pendingReminderNote: String? = null
    private var pendingReminderInfo: ReminderInfo? = null
    private var openReminderSheet: (String) -> Unit = { note -> pendingReminderNote = note }
    private var openReminderDetails: (ReminderInfo) -> Unit = { info -> pendingReminderInfo = info }

    private var allContactsFragment: AllContactsFragment? = null
    private var contactsContainerView: FrameLayout? = null

    override fun attachBaseContext(newBase: Context) {
        val tag = AppLanguageManager.currentLanguageTag(newBase)
        super.attachBaseContext(LocaleUtils.wrap(newBase, tag))
    }
    private val contactsChromeCount = mutableIntStateOf(0)
    private val contactsSearchQuery = mutableStateOf("")
    private val contactsFloatingSearchVisible = mutableStateOf(false)
    private val settingsSearchQuery = mutableStateOf("")

    companion object {
        private const val TAG_MAIN = "MainActivity"
        const val EXTRA_START_PAGE = "extra_start_page"
        const val PAGE_HOME = 0
        const val PAGE_CONTACTS = 1
        const val PAGE_NOTES = 2
        const val PAGE_SETTINGS = 3
        private const val MAIN_WELCOME_PREFS = "main_welcome_prefs"
        private const val MAIN_WELCOME_SEEN_VERSION = "seen_version"
        private const val MAIN_WELCOME_VERSION = 1
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

        allNotes.add(newNote)
        ensureLocalUid(newNote)
        if (imageUris.isNotEmpty()) NoteMediaStore.setUris(this, newNote, imageUris)
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
                var homeCameraExpanded by remember { mutableStateOf(false) }
                var homeCameraGestureActive by remember { mutableStateOf(false) }
                var reminderNote by remember { mutableStateOf<String?>(null) }
                var reminderTimeNote by remember { mutableStateOf<String?>(null) }
                var reminderDetails by remember { mutableStateOf<ReminderInfo?>(null) }
                val pagerState = rememberPagerState(
                    initialPage = intent.getIntExtra(EXTRA_START_PAGE, PAGE_HOME)
                        .coerceIn(PAGE_HOME, PAGE_SETTINGS),
                    pageCount = { 4 }
                )
                val scope = rememberCoroutineScope()

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
                    if (page != PAGE_HOME) {
                        homeCameraExpanded = false
                        homeCameraGestureActive = false
                    }
                    scope.launch { pagerState.animateScrollToPage(page.coerceIn(PAGE_HOME, PAGE_SETTINGS)) }
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
                        pagerState.currentPage != PAGE_HOME -> goToPage(PAGE_HOME)
                        else -> actuallyExitApp()
                    }
                }

                FlightsBackdropScaffold { globalBackdrop, _ ->
                    val mainPageBackdrop = rememberLayerBackdrop()
                    val mainMenuBackdrop = rememberLayerBackdrop()
                    val isDark = isSystemInDarkTheme()
                    val selectedTab = when (pagerState.currentPage) {
                        PAGE_CONTACTS -> PrimaryTabDestination.Contacts
                        PAGE_NOTES -> PrimaryTabDestination.Notes
                        PAGE_SETTINGS -> PrimaryTabDestination.Settings
                        else -> PrimaryTabDestination.Home
                    }
                    var settingsModalVisible by remember { mutableStateOf(false) }

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
                                    onOpenContacts = { goToPage(PAGE_CONTACTS) },
                                    onOpenNotes = { goToPage(PAGE_NOTES) },
                                    onHomeCameraExpandedChange = { expanded ->
                                        homeCameraExpanded = expanded && pagerState.currentPage == PAGE_HOME
                                    },
                                    onHomeCameraGestureActiveChange = { active ->
                                        homeCameraGestureActive = active && pagerState.currentPage == PAGE_HOME
                                    },
                                    actuallyExitApp = ::actuallyExitApp,
                                    triggerRefreshNow = { newUrl ->
                                        Log.d(TAG_MAIN, "triggerRefreshNow(newUrl=$newUrl)")
                                    },
                                    pagerState = pagerState,
                                    pagerSwipeEnabled = !(
                                        pagerState.currentPage == PAGE_HOME &&
                                            (homeCameraExpanded || homeCameraGestureActive)
                                    )
                                ) { settingsModalVisible = it }
                            }

                            if (pagerState.currentPage == PAGE_CONTACTS) {
                                ContactsFloatingSearchOverlay(backdrop = mainPageBackdrop)
                            }

                            if (
                                !(pagerState.currentPage == PAGE_HOME && homeCameraExpanded) &&
                                !(pagerState.currentPage == PAGE_SETTINGS && settingsModalVisible)
                            ) {
                                PrimaryBottomChrome(
                                    selectedTab = selectedTab,
                                    backdrop = mainPageBackdrop,
                                    menuVisible = false,
                                    menuActions = emptyList(),
                                    onMenuDismiss = ::closeMenuSheet,
                                    onOpenHome = { goToPage(PAGE_HOME) },
                                    onOpenContacts = { goToPage(PAGE_CONTACTS) },
                                    onOpenNotes = { goToPage(PAGE_NOTES) },
                                    onOpenSettings = { goToPage(PAGE_SETTINGS) },
                                    onOpenMenu = ::openMenuSheet,
                                    showMenu = false
                                )
                            }
                        }

                        if (!(pagerState.currentPage == PAGE_HOME && homeCameraExpanded)) {
                            PrimaryBottomChrome(
                                selectedTab = selectedTab,
                                backdrop = mainMenuBackdrop,
                                menuVisible = showMenuSheet,
                                menuActions = if (pagerState.currentPage == PAGE_SETTINGS) {
                                    listOf(
                                        PrimaryMenuAction(
                                            label = "Search",
                                            iconRes = R.drawable.ic_oui_search,
                                            onClick = { openSettingsSearchSheet() }
                                        ),
                                        PrimaryMenuAction(
                                            label = "Software Update",
                                            iconRes = R.drawable.system_update_24dp_ffffff_fill1_wght400_grad0_opsz24,
                                            onClick = {
                                                startActivity(Intent(this@MainActivity, SoftwareUpdateActivity::class.java))
                                                overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation)
                                            }
                                        ),
                                        PrimaryMenuAction(
                                            label = "App Icon",
                                            iconRes = R.drawable.palette_24dp_ffffff_fill0_wght400_grad0_opsz24,
                                            onClick = {
                                                startActivity(Intent(this@MainActivity, AppIconPickerActivity::class.java))
                                                overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation)
                                            }
                                        ),
                                        PrimaryMenuAction(
                                            label = "Rate",
                                            iconRes = R.drawable.star_rate_half_24dp_ffffff_fill1_wght400_grad0_opsz24,
                                            onClick = {
                                                RateUsDialogFragment().show(supportFragmentManager, "RateUsDialog")
                                            }
                                        ),
                                        PrimaryMenuAction(
                                            label = "Feedback",
                                            iconRes = R.drawable.baseline_feedback_24,
                                            onClick = {
                                                FeedbackBottomSheet().show(supportFragmentManager, "FeedbackBottomSheet")
                                            }
                                        ),
                                        PrimaryMenuAction(
                                            label = "Share",
                                            iconRes = R.drawable.baseline_share_24,
                                            onClick = ::shareApp
                                        )
                                    )
                                } else {
                                    listOf(
                                        PrimaryMenuAction(
                                            label = "Live Cameras",
                                            iconRes = R.drawable.fullscreen_24dp_46152f_fill1_wght400_grad0_opsz24,
                                            onClick = ::openLiveCamerasScreen
                                        ),
                                        PrimaryMenuAction(
                                            label = "QR Code",
                                            iconRes = R.drawable.ic_oui_qr_code,
                                            onClick = ::openQrScreen
                                        ),
                                        PrimaryMenuAction(
                                            label = "Profile",
                                            iconRes = R.drawable.account_circle_24dp_ffffff_fill1_profile,
                                            onClick = ::openProfileScreen,
                                            useProfileAvatar = true
                                        )
                                    )
                                },
                                onMenuDismiss = ::closeMenuSheet,
                                onOpenHome = { goToPage(PAGE_HOME) },
                                onOpenContacts = { goToPage(PAGE_CONTACTS) },
                                onOpenNotes = { goToPage(PAGE_NOTES) },
                                onOpenSettings = { goToPage(PAGE_SETTINGS) },
                                onOpenMenu = ::openMenuSheet,
                                showTabs = false
                            )
                        }

                        MainWelcomeOnboardingOverlay(
                            visible = showMainWelcome,
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

    fun updateContactsChromeCount(visibleCount: Int) {
        contactsChromeCount.intValue = visibleCount
    }

    fun updateContactsSearch(query: String) {
        contactsSearchQuery.value = query
        if (query.isBlank()) contactsFloatingSearchVisible.value = false
        allContactsFragment?.filterContacts(query)
    }

    fun updateContactsFloatingSearch(query: String) {
        contactsSearchQuery.value = query
        allContactsFragment?.filterContacts(
            query = query,
            syncTopSearch = false,
            keepFloatingSearchActive = true
        )
    }

    fun updateContactsFloatingSearchVisible(visible: Boolean) {
        contactsFloatingSearchVisible.value = visible
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

        if (oldNote != updatedNote) {
            notesAdapter.migrateUserTitle(oldNote, updatedNote)
            NoteMediaStore.migrateNoteKey(this, oldNote, updatedNote)
            contentToUid[updatedNote] = contentToUid.remove(oldNote) ?: ensureLocalUid(updatedNote)
            uidToContent.entries.firstOrNull { it.value == oldNote }?.let { entry ->
                uidToContent[entry.key] = updatedNote
            }
            saveUidMaps()
        }

        allNotes.indexOf(oldNote).takeIf { it >= 0 }?.let { allNotes[it] = updatedNote }
        NoteMediaStore.setUris(this, updatedNote, updatedImages)
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
            noteRows.add(
                NoteRow(
                    id = key,
                    text = text,
                    imagesCount = NoteMediaStore.getUris(this, text).size,
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

        toDelete.forEach { note ->
            NoteMediaStore.deleteAllForNote(this, note)
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
            java.util.UUID.randomUUID().toString().also { uid ->
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

    private fun hideContactsKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(contactsContainerView?.windowToken, 0)
        contactsContainerView?.clearFocus()
    }

    private fun openSettingsSearchSheet() {
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

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun MainPager(
        currentBackdrop: LayerBackdrop,
        onOpenHome: () -> Unit,
        onOpenContacts: () -> Unit,
        onOpenNotes: () -> Unit,
        onHomeCameraExpandedChange: (Boolean) -> Unit,
        onHomeCameraGestureActiveChange: (Boolean) -> Unit,
        actuallyExitApp: () -> Unit,
        triggerRefreshNow: (String?) -> Unit,
        pagerState: PagerState,
        pagerSwipeEnabled: Boolean,
        onSettingsModalVisibleChange: (Boolean) -> Unit
    ) {
        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 2,
            userScrollEnabled = pagerSwipeEnabled,
            modifier = Modifier.fillMaxSize()
        ) { page ->
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
                PAGE_CONTACTS -> ContactsPage()
                PAGE_NOTES -> NotesPage(
                    onOpenHome = onOpenHome,
                    onOpenContacts = onOpenContacts
                )
                PAGE_SETTINGS -> SettingsPage(
                    onOpenHome = onOpenHome,
                    onOpenContacts = onOpenContacts,
                    onOpenNotes = onOpenNotes,
                    onModalVisibleChange = onSettingsModalVisibleChange
                )
            }
        }
    }

    @Composable
    private fun SettingsPage(
        onOpenHome: () -> Unit,
        onOpenContacts: () -> Unit,
        onOpenNotes: () -> Unit,
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
            onOpenProfile = {
                startActivity(
                    Intent(this, ProfileDetailsComposeActivity::class.java),
                    ActivityOptionsCompat.makeSceneTransitionAnimation(this).toBundle()
                )
            },
            showBottomChrome = false,
            modalBottomPadding = GlassChromeHorizontalPadding,
            onModalVisibleChange = onModalVisibleChange
        )
    }

    @Composable
    private fun ContactsPage() {
        Box(Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize(),
                factory = { context ->
                    FrameLayout(context).apply {
                        id = R.id.main_contacts_container
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    }
                },
                update = { container ->
                    if (contactsContainerView !== container) {
                        contactsContainerView = container
                        installContactsFragment()
                    }
                }
            )
        }
    }

    @Composable
    private fun ContactsFloatingSearchOverlay(backdrop: LayerBackdrop) {
        Box(Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val keyboardOpen = WindowInsets.ime.getBottom(density) > 0
            val searchBottomPadding = if (keyboardOpen) 12.dp else 80.dp
            var floatingSearchSawKeyboard by remember { mutableStateOf(false) }

            LaunchedEffect(contactsFloatingSearchVisible.value, keyboardOpen) {
                when {
                    !contactsFloatingSearchVisible.value -> floatingSearchSawKeyboard = false
                    keyboardOpen -> floatingSearchSawKeyboard = true
                    floatingSearchSawKeyboard -> {
                        updateContactsSearch("")
                        updateContactsFloatingSearchVisible(false)
                    }
                }
            }

            ContactsFloatingSearchBar(
                query = contactsSearchQuery.value,
                onQueryChange = { query -> updateContactsFloatingSearch(query) },
                backdrop = backdrop,
                visible = contactsFloatingSearchVisible.value,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(start = 12.dp, end = 16.dp, bottom = searchBottomPadding)
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
            onBack = null
        )
    }
}
