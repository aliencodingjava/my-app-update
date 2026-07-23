package com.flights.studio

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.CalendarContract
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.flights.studio.com.flights.studio.ui.ReminderDismissReceiver
import com.google.android.material.badge.ExperimentalBadgeUtils
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.android.material.transition.platform.MaterialSharedAxis
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

@Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
class AllNotesActivity : LocaleActivity() {

    private lateinit var notesAdapter: NotesAdapter
    private val allNotes = mutableListOf<String>()
    private val notesText = mutableStateListOf<String>()          // adapter + legacy logic
    private val noteRows  = mutableStateListOf<NoteRow>()         // compose UI list with ids
    private val folderRows = mutableStateListOf<NoteFolderUi>()
    private var currentFolderId by mutableStateOf<String?>(null)
    private val sharedPreferences by lazy { getSharedPreferences("notes_prefs", MODE_PRIVATE) }
    private var isMultiSelectMode = false
    private val handler = Handler(Looper.getMainLooper())
    private var updateSecondsRunnable: Runnable? = null

    // FAB badge (legacy view path – now no-op in Compose mode)
    private var hasInitializedBadge = false
    private var notesCount by mutableIntStateOf(0)
    private var isPendingNotesSyncRunning = false
    private var pendingNotesSyncRequested = false
    private var notesSyncStatus by mutableStateOf(NotesSyncUiStatus.Synced)
    private val notesHttpClient by lazy { OkHttpClient() }


    private val metaPrefs by lazy { getSharedPreferences("notes_meta", MODE_PRIVATE) }
    private fun now() = System.currentTimeMillis()
    private val selectedKeys = mutableSetOf<String>()

    // === ID map storage (content <-> id) ===
    private val idToContent = mutableMapOf<String, String>()
    private val contentToId = mutableMapOf<String, String>()
    private val idPrefs by lazy { getSharedPreferences("notes_ids", MODE_PRIVATE) }
    private var setDeleteVisibleFromActivity: ((Boolean) -> Unit)? = null



    private var openReminderSheet: (String) -> Unit = {}
    // === local UID map storage (content <-> localUid) for unsynced notes ===
    private val uidToContent = mutableMapOf<String, String>()
    private val contentToUid = mutableMapOf<String, String>()
    private val uidPrefs by lazy { getSharedPreferences("notes_uids", MODE_PRIVATE) }

    private fun rebuildNoteRowsFromDisplay(display: List<String>) {
        noteRows.clear()

        val used = HashSet<String>() // track keys used in this render

        display.forEachIndexed { index, text ->
            val baseUid = contentToUid[text] ?: ensureLocalUid(text)

            // If two notes share same base uid (same content), make UI key unique
            var key = baseUid
            if (!used.add(key)) {
                key = "$baseUid#$index"
                used.add(key)
            }

            val imagesCount = NoteMediaStore.getUris(this, text).size
            val attachmentCounts = countNoteAttachments(NoteAttachmentStore.getItems(this, text))
            val title = resolveTitle(text).orEmpty()   // ✅ pull from adapter/cache

            val bellOn = getSharedPreferences("reminder_flags", MODE_PRIVATE)
                .getBoolean(text.hashCode().toString(), false)

            val badgeOn = getSharedPreferences("reminder_badges", MODE_PRIVATE)
                .getBoolean(text.hashCode().toString(), false)

            noteRows.add(
                NoteRow(
                    id = key,
                    text = text,
                    imagesCount = imagesCount,
                    attachmentsCount = attachmentCounts.documents,
                    audioCount = attachmentCounts.audio,
                    videoCount = attachmentCounts.video,
                    title = title,
                    hasReminder = bellOn,
                    hasBadge = badgeOn,
                    createdAtMs = NoteCreatedAtStore.ensure(this, baseUid)
                )
            )


        }
    }



    private fun rebuildBoth(display: List<String>) {
        notesText.clear()
        notesText.addAll(display)
        rebuildNoteRowsFromDisplay(display)
    }


    // ----------------------- Lifecycle -----------------------

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {

        window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)

        // Going FORWARD to AddNote = forward = true
        window.exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, true).apply {
            addTarget(android.R.id.content)
        }

        // Coming BACK from AddNote = forward = false
        window.reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, false).apply {
            addTarget(android.R.id.content)
        }

        // Let them overlap (looks more modern)
        window.allowEnterTransitionOverlap = true
        window.allowReturnTransitionOverlap = true
        super.onCreate(savedInstanceState)

        // ✅ Create adapter BEFORE Compose (Compose uses it)
        notesAdapter = NotesAdapter(
            notesText,
            applicationContext,
            ::onNoteLongClick,
            ::onNoteClick,
            { note, position ->
                val title = resolveTitle(note)
                val images = NoteMediaStore.getUris(this, note)

                val wantsReminder = getSharedPreferences("reminder_flags", MODE_PRIVATE)
                    .getBoolean(note.hashCode().toString(), false)

                val intent = EditNoteComposeActivity.newIntent(
                    context = this,
                    note = note,
                    title = title,
                    images = images,
                    attachments = NoteAttachmentStore.getItems(this, note),
                    voiceNotes = NoteVoiceStore.getItems(this, note),
                    wantsReminder = wantsReminder,
                    position = position
                )

                val opts = ActivityOptionsCompat.makeSceneTransitionAnimation(this)
                editNoteLauncher.launch(intent, opts)
            },

            { note, _ -> openReminderSheet(note) },
        ).also { adapter ->
            adapter.provideKeyResolver { note ->
                contentToUid[note] ?: ensureLocalUid(note)
            }
        }


        // Optional preloads
        notesAdapter.preloadBadgeStates(this)
        notesAdapter.preloadReminderFlags(this)

        // Let Compose handle system bars (status/nav/gesture) paddings
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // ✅ Compose root (no XML setContentView)
        setContent {
            FlightsTheme {
                val topBarColor = MaterialTheme.colorScheme.surfaceVariant
                val view = LocalView.current
                val isDark = isSystemInDarkTheme()
                var reminderNote by rememberSaveable { mutableStateOf<String?>(null) }


                SideEffect {
                    val w = (view.context as Activity).window

                    // ✅ solid bar color
                    w.statusBarColor = topBarColor.toArgb()

                    // ✅ stop “auto dim” behavior as much as possible
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        w.isStatusBarContrastEnforced = false
                    }

                    // ✅ IMPORTANT: remove translucent flags (some OEMs add them)
                    w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)

                    // ✅ icons
                    WindowCompat.getInsetsController(w, view).apply {
                        isAppearanceLightStatusBars = !isDark
                    }
                    openReminderSheet = { note ->
                        reminderNote = note
                    }
                }



                fun dismissReminder() { reminderNote = null }
                fun openTimerForCurrent() { reminderNote?.let { openMaterialTimePickerDialog(it) }; dismissReminder() }
                fun openCalendarForCurrent() { reminderNote?.let { openMaterialDateTimePickerDialog(it) }; dismissReminder() }

                ReminderHost(
                    visible = reminderNote != null,
                    onDismiss = ::dismissReminder,
                    onTimer = ::openTimerForCurrent,
                    onCalendar = ::openCalendarForCurrent
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                    ) {
                        AllNotesScreen(
                            notesAdapter = notesAdapter,
                            notes = noteRows,
                            notesSize = notesCount,
                            onAddNote = {
                                if (currentFolderId != null) {
                                    val intent = AddNoteComposeActivity.newIntent(this@AllNotesActivity)
                                    val opts = ActivityOptionsCompat.makeSceneTransitionAnimation(this@AllNotesActivity)
                                    addNoteLauncher.launch(intent, opts)
                                }
                            },
                            onOpenSearch = { onDismiss ->
                                openSearchView(onDismiss)
                            },
                            onNavItemClick = { id ->
                                when (id) {
                                    R.id.nav_home -> goToHomeScreen()
                                    R.id.nav_contacts -> goToContactScreen()
                                    R.id.nav_all_contacts -> goToAllContactsScreen()
                                    R.id.nav_settings -> {
                                        startActivity(NotesSettingsComposeActivity.newIntent(this@AllNotesActivity))
                                        overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation)                                }
                                    R.id.openAddNoteScreen -> Unit
                                    R.id.action_delete -> deleteSelectedNotes()
                                }
                            },

                            onDeleteSelected = { selectedRowKeys ->
                                selectedKeys.clear()
                                selectedKeys.addAll(selectedRowKeys)
                                isMultiSelectMode = selectedKeys.isNotEmpty()
                                deleteSelectedNotes()
                            },
                            onDeleteSelectedFolders = ::deleteSelectedNoteFolders,
                            onOpenNote = { row, position -> onNoteClick(row.text, position) },
                            onOpenProfile = {
                                startActivity(Intent(this@AllNotesActivity, ProfileDetailsComposeActivity::class.java))
                                overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation)
                            },
                            syncStatus = notesSyncStatus,
                            syncAvailable = hasActiveSupabaseSession(),
                            onNotesSettingsChanged = ::refreshNotesDisplayFromSettings,
                            onBack = if (currentFolderId != null) {
                                {
                                    currentFolderId = null
                                    refreshNotesDisplayFromSettings()
                                }
                            } else {
                                null
                            },
                            pageTitle = currentFolderTitle(),
                            showWelcomeOnEmptyNotes = false,
                            folderMode = currentFolderId == null,
                            folders = folderRows,
                            onOpenFolder = { folderId ->
                                currentFolderId = folderId
                                refreshNotesDisplayFromSettings()
                            },
                            onCreateFolder = { folderName ->
                                if (NoteFolderStore.createFolder(this@AllNotesActivity, folderName) != null) {
                                    currentFolderId = null
                                    refreshNotesDisplayFromSettings()
                                }
                            }



                        )


                    }
                }
            }
        }

        // Back press
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (currentFolderId != null) {
                    currentFolderId = null
                    refreshNotesDisplayFromSettings()
                } else {
                    finish()
                    overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation)
                }
            }
        })

        NotesCacheManager.preloadResources(this)
        loadIdMaps()
        loadUidMaps()

        // 🔄 SYNC FLOW (no XML refs)
        if (UserPreferencesManager(this).isLoggedIn && notesOnlineSyncEnabled()) {
            lifecycleScope.launch {
                try {
                    notesSyncStatus = NotesSyncUiStatus.Syncing
                    loadNotesHeadless()

                    // 1) push signed-in deletes first
                    syncAllDeletesToSupabase()

                    // 2) then push adds
                    notesSyncStatus = NotesSyncUiStatus.Uploading
                    syncLocalNotesToSupabase()

                    // 3) now pull, reconcile, render
                    notesSyncStatus = NotesSyncUiStatus.Downloading
                    pullFromSupabaseAndReconcile()

                    notesSyncStatus = NotesSyncUiStatus.Synced
                    metaPrefs.edit { putLong("last_sync_at", now()) }
                } catch (e: Exception) {
                    notesSyncStatus = NotesSyncUiStatus.Error
                    Log.e("SupabaseSync", "Initial notes sync failed", e)
                }
            }

        } else {
            loadNotesHeadless()
        }

    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onStart() {
        super.onStart()
        currentFolderId = null

        val filter = IntentFilter(ReminderDismissReceiver.ACTION_BADGE_CHANGED)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(badgeChangedReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(badgeChangedReceiver, filter)
        }

        val newNotes: List<String> = when {
            NotesCacheManager.cachedNotes.isNotEmpty() -> NotesCacheManager.cachedNotes.toList()
            else -> {
                sharedPreferences.getString("notes_list", null)
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { json ->
                        val type = object : TypeToken<List<String>>() {}.type
                        Gson().fromJson(json, type)
                    } ?: emptyList()
            }
        }

        val settings = readNotesPageSettings()

        allNotes.clear()
        allNotes.addAll(newNotes)

        // ✅ 1) apply flags FIRST
        notesAdapter.applyPageSettings(
            compact = settings.compact,
            showImagesBadge = settings.showImagesBadge,
            showReminderBadge = settings.showReminderBadge,
            showReminderBell = settings.showReminderBell,
            titleTopCompactDp = settings.titleTopCompactDp,
            titleTopNormalDp = settings.titleTopNormalDp
        )

        // ✅ 2) preload BEFORE building UI
        notesAdapter.preloadReminderFlags(this)
        notesAdapter.preloadBadgeStates(this)

        refreshDisplayedNotes()
        schedulePendingNotesSync("onStart")
    }


    override fun onStop() {
        unregisterReceiver(badgeChangedReceiver)
        super.onStop()
    }

    // Legacy hook – now no-op (no XML FAB)
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && !hasInitializedBadge) {
            showFabBadge(null) // no-op
            hasInitializedBadge = true
        }
    }

    // ----------------------- Activity result launchers -----------------------

    private val addNoteLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val newNoteContent = result.data?.getStringExtra("NEW_NOTE")
            val userTitle      = result.data?.getStringExtra("NEW_NOTE_TITLE").orEmpty()
            val imageStrings   = result.data?.getStringArrayListExtra("NEW_NOTE_IMAGES") ?: arrayListOf()
            val imageUris      = imageStrings.mapNotNull { runCatching { it.toUri() }.getOrNull() }
            val voiceUris      = result.data?.getStringArrayListExtra("NEW_NOTE_VOICE_URIS") ?: arrayListOf()
            val voiceDurations = result.data?.getLongArrayExtra("NEW_NOTE_VOICE_DURATIONS") ?: longArrayOf()
            val voiceCreatedAt = result.data?.getLongArrayExtra("NEW_NOTE_VOICE_CREATED_AT") ?: longArrayOf()
            val voiceItems = voiceUris.mapIndexed { index, uri ->
                NoteVoiceItem(
                    uri = uri,
                    durationMs = voiceDurations.getOrNull(index) ?: 0L,
                    createdAtMs = voiceCreatedAt.getOrNull(index) ?: System.currentTimeMillis()
                )
            }
            val fileUris = result.data?.getStringArrayListExtra("NEW_NOTE_FILE_URIS") ?: arrayListOf()
            val fileNames = result.data?.getStringArrayListExtra("NEW_NOTE_FILE_NAMES") ?: arrayListOf()
            val fileMimes = result.data?.getStringArrayListExtra("NEW_NOTE_FILE_MIMES") ?: arrayListOf()
            val fileSizes = result.data?.getLongArrayExtra("NEW_NOTE_FILE_SIZES") ?: longArrayOf()
            val fileItems = fileUris.mapIndexed { index, uri ->
                NoteAttachmentItem(
                    uri = uri,
                    name = fileNames.getOrNull(index).orEmpty().ifBlank { "Attachment ${index + 1}" },
                    mime = fileMimes.getOrNull(index)?.takeIf { it.isNotBlank() },
                    sizeBytes = fileSizes.getOrNull(index) ?: 0L
                )
            }

            if (!newNoteContent.isNullOrBlank()) {
                addLocalNote(newNoteContent, queuePendingSync = false)
                if (imageUris.isNotEmpty()) {
                    NoteMediaStore.setUris(this, newNoteContent, imageUris)
                }
                if (voiceItems.isNotEmpty()) {
                    NoteVoiceStore.setItems(this, newNoteContent, voiceItems)
                }
                if (fileItems.isNotEmpty()) {
                    NoteAttachmentStore.setItems(this, newNoteContent, fileItems)
                }
                if (userTitle.isNotBlank()) {
                    notesAdapter.setUserTitle(newNoteContent, userTitle)
                }

                val wantsReminder = result.data?.getBooleanExtra("NEW_NOTE_WANTS_REMINDER", false) == true
                if (wantsReminder) {
                    getSharedPreferences("reminder_flags", MODE_PRIVATE).edit {
                        putBoolean(newNoteContent.hashCode().toString(), true)
                    }
                    notesAdapter.preloadReminderFlags(this)
                    val idx = notesText.indexOf(newNoteContent)
                    if (idx != -1) notesAdapter.notifyItemChanged(idx)

                    // Open glass sheet via provider captured in Compose
                    openReminderSheet(newNoteContent)
                }
                queuePendingAdd(newNoteContent)
                uploadNoteNowOrKeepPending(
                    content = newNoteContent,
                    titleOverride = userTitle,
                    imageUrisOverride = imageUris,
                    fileItemsOverride = fileItems,
                    voiceItemsOverride = voiceItems,
                    hasReminderOverride = wantsReminder
                )
            }
        }
    }

    private fun loadUidMaps() {
        uidToContent.clear(); contentToUid.clear()
        val json = uidPrefs.getString("uid_to_content", "{}")!!
        val type = object : TypeToken<Map<String, String>>() {}.type
        uidToContent.putAll(Gson().fromJson(json, type))
        contentToUid.putAll(uidToContent.entries.associate { it.value to it.key })
    }

    private fun saveUidMaps() {
        uidPrefs.edit { putString("uid_to_content", Gson().toJson(uidToContent)) }
    }
    private fun ensureLocalUid(note: String): String {
        return contentToUid.getOrPut(note) {
            val uid = java.util.UUID.randomUUID().toString()
            uidToContent[uid] = note
            saveUidMaps()
            uid
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

    private fun showCreateFolderDialog() {
        val input = EditText(this).apply {
            hint = "Folder name"
            setSingleLine(true)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("New folder")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val folder = NoteFolderStore.createFolder(this, input.text?.toString().orEmpty())
                if (folder != null) {
                    refreshFolderRows()
                    currentFolderId = null
                    rebuildBoth(emptyList())
                }
            }
            .show()
    }

    private fun ensureFolderAssignmentsFor(notes: List<String>) {
        notes.forEach { note ->
            NoteFolderStore.ensureNoteInMain(this, contentToUid[note] ?: ensureLocalUid(note))
        }
    }

    private fun assignNoteToCurrentFolder(note: String) {
        val folderId = currentFolderId ?: NoteFolderStore.MAIN_FOLDER_ID
        NoteFolderStore.assignNoteToFolder(
            context = this,
            noteKey = contentToUid[note] ?: ensureLocalUid(note),
            folderId = folderId
        )
        refreshFolderRows()
    }

    private fun displayNotesForCurrentFolder(): List<String> {
        val folderId = currentFolderId ?: return emptyList()
        ensureFolderAssignmentsFor(allNotes)
        return allNotes.filter { note ->
            NoteFolderStore.folderForNoteKey(this, contentToUid[note] ?: ensureLocalUid(note)) == folderId
        }
    }

    private fun refreshFolderRows(query: String = "") {
        ensureFolderAssignmentsFor(allNotes)
        val counts = NoteFolderStore.countByFolder(
            this,
            allNotes.map { note -> contentToUid[note] ?: ensureLocalUid(note) }
        )
        val folders = buildList {
            if (allNotes.isNotEmpty()) {
                val main = NoteFolderStore.mainFolder()
                add(NoteFolderUi(main.id, main.name, counts[main.id] ?: 0))
            }
            NoteFolderStore.loadCustomFolders(this@AllNotesActivity).forEach { folder ->
                add(NoteFolderUi(folder.id, folder.name, counts[folder.id] ?: 0))
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
        folderRows.clear()
        folderRows.addAll(filteredFolders)
    }

    private fun noteMatchesFolderSearch(note: String, query: String): Boolean {
        val title = resolveTitle(note).orEmpty()
        return note.contains(query, ignoreCase = true) || title.contains(query, ignoreCase = true)
    }

    private fun currentFolderTitle(): String {
        val folderId = currentFolderId ?: return "Folders"
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
        val folders = NoteFolderStore.loadCustomFolders(this@AllNotesActivity)
        if (folders.isEmpty()) return@withContext

        val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
        val anonKey = BuildConfig.SUPABASE_ANON_KEY
        if (baseUrl.isBlank() || anonKey.isBlank()) return@withContext

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
            .addHeader("apikey", anonKey)
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
        val anonKey = BuildConfig.SUPABASE_ANON_KEY
        if (baseUrl.isBlank() || anonKey.isBlank()) return@withContext emptyList()

        val request = Request.Builder()
            .url(
                "$baseUrl/rest/v1/note_folders" +
                        "?select=id,name,created_at" +
                        "&user_id=eq.${urlEncode(userId)}" +
                        "&deleted_at=is.null" +
                        "&order=created_at.asc"
            )
            .addHeader("apikey", anonKey)
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
            }.onFailure { Log.e("SupabaseSync", "Delete folders from Supabase failed", it) }
        }
    }

    private suspend fun deleteRemoteNoteFolders(
        authToken: String,
        userId: String,
        folderIds: Set<String>
    ) = withContext(Dispatchers.IO) {
        if (folderIds.isEmpty()) return@withContext
        val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
        val anonKey = BuildConfig.SUPABASE_ANON_KEY
        if (baseUrl.isBlank() || anonKey.isBlank()) return@withContext

        val inList = folderIds.joinToString(",", "(", ")")
        val request = Request.Builder()
            .url(
                "$baseUrl/rest/v1/note_folders" +
                        "?user_id=eq.${urlEncode(userId)}" +
                        "&id=in.$inList"
            )
            .addHeader("apikey", anonKey)
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

    private fun refreshDisplayedNotes() {
        refreshFolderRows()
        val settings = readNotesPageSettings()
        val sorted = applyNotesSort(displayNotesForCurrentFolder(), settings.sortMode)
        rebuildBoth(sorted)
        notesAdapter.submit(sorted)
        notesCount = allNotes.size
        updateNotePlaceholder()
        showFabBadge(null)
    }



    private suspend fun syncAllDeletesToSupabase() {
        if (!notesOnlineSyncEnabled()) return

        val session = SupabaseManager.client.auth.currentSessionOrNull() ?: return
        val userId = session.user?.id ?: return
        val pendingDeletes = pendingDeletesForUser(userId)
        if (pendingDeletes.isEmpty()) return

        val remoteContents = fetchActiveRemoteRows(userId).map { it.content }.toSet()
        val stillPending = mutableSetOf<String>()
        pendingDeletes.forEach { content ->
            try {
                val id = contentToId[content]
                if (id != null || content in remoteContents) {
                    softDeleteNoteInSupabase(
                        authToken = session.accessToken,
                        userId = userId,
                        id = id,
                        content = if (id == null) content else null
                    )
                } else {
                    Log.d("SupabaseDelete", "Skipping cloud delete; note is not active in Supabase")
                }
                contentToId.remove(content)?.let { idToContent.remove(it) }
                saveIdMaps()
            } catch (e: Exception) {
                Log.e("SupabaseDelete", "Failed to mark note deleted: $content", e)
                stillPending.add(content)
            }
        }
        savePendingDeletesForUser(userId, stillPending)
    }


    @Suppress("DEPRECATION")
    private val editNoteLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) return@registerForActivityResult
            val data = result.data ?: return@registerForActivityResult

            val updatedNote  = data.getStringExtra("UPDATED_NOTE").orEmpty()
            val updatedTitle = data.getStringExtra("UPDATED_TITLE").orEmpty()
            val position     = data.getIntExtra("NOTE_POSITION", -1)

            val updatedImageStrings = data.getStringArrayListExtra("UPDATED_IMAGES") ?: arrayListOf()
            val updatedImageUris = updatedImageStrings.mapNotNull { runCatching { it.toUri() }.getOrNull() }
            val updatedFileUris = data.getStringArrayListExtra("UPDATED_FILE_URIS") ?: arrayListOf()
            val updatedFileNames = data.getStringArrayListExtra("UPDATED_FILE_NAMES") ?: arrayListOf()
            val updatedFileMimes = data.getStringArrayListExtra("UPDATED_FILE_MIMES") ?: arrayListOf()
            val updatedFileSizes = data.getLongArrayExtra("UPDATED_FILE_SIZES") ?: longArrayOf()
            val updatedFiles = updatedFileUris.mapIndexed { index, uri ->
                NoteAttachmentItem(
                    uri = uri,
                    name = updatedFileNames.getOrNull(index).orEmpty().ifBlank { "Attachment ${index + 1}" },
                    mime = updatedFileMimes.getOrNull(index)?.takeIf { it.isNotBlank() },
                    sizeBytes = updatedFileSizes.getOrNull(index) ?: 0L
                )
            }
            val updatedVoiceUris = data.getStringArrayListExtra("UPDATED_VOICE_URIS") ?: arrayListOf()
            val updatedVoiceDurations = data.getLongArrayExtra("UPDATED_VOICE_DURATIONS") ?: longArrayOf()
            val updatedVoiceCreatedAt = data.getLongArrayExtra("UPDATED_VOICE_CREATED_AT") ?: longArrayOf()
            val updatedVoiceItems = updatedVoiceUris.mapIndexed { index, uri ->
                NoteVoiceItem(
                    uri = uri,
                    durationMs = updatedVoiceDurations.getOrNull(index) ?: 0L,
                    createdAtMs = updatedVoiceCreatedAt.getOrNull(index) ?: System.currentTimeMillis()
                )
            }

            if (updatedNote.isBlank()) return@registerForActivityResult

            // ✅ IMPORTANT: capture what was edited from the CURRENT visible list
            val oldNote = notesText.getOrNull(position) ?: return@registerForActivityResult

            // ✅ 1) Update RAW truth (allNotes) by CONTENT, not by visible position
            val rawIdx = allNotes.indexOf(oldNote)
            if (rawIdx != -1) {
                if (oldNote != updatedNote) {
                    // move title mapping + images key if your images are keyed by note text
                    notesAdapter.migrateUserTitle(oldNote, updatedNote)
                    NoteMediaStore.migrateNoteKey(this, oldNote, updatedNote)
                    NoteVoiceStore.migrateNoteKey(this, oldNote, updatedNote)
                    NoteAttachmentStore.migrateNoteKey(this, oldNote, updatedNote)

                    // keep uid maps consistent
                    contentToUid[updatedNote] = contentToUid.remove(oldNote) ?: ensureLocalUid(updatedNote)
                    uidToContent.entries.firstOrNull { it.value == oldNote }?.let { entry ->
                        uidToContent[entry.key] = updatedNote
                    }
                    saveUidMaps()
                }

                allNotes[rawIdx] = updatedNote
            } else {
                // fallback: if somehow not found, replace in visible list only
                notesText[position] = updatedNote
                rebuildNoteRowsFromDisplay(notesText)   // keep Compose UI in sync
            }

            // ✅ 2) Persist images under the NEW content
            NoteMediaStore.setUris(this, updatedNote, updatedImageUris)
            NoteVoiceStore.setItems(this, updatedNote, updatedVoiceItems)
            NoteAttachmentStore.setItems(this, updatedNote, updatedFiles)

            // ✅ 2.5) Title store FIRST (so rebuild reads fresh title)
            if (updatedTitle.isNotBlank()) notesAdapter.setUserTitle(updatedNote, updatedTitle)
            else notesAdapter.removeUserTitle(updatedNote)


            refreshDisplayedNotes()

            // ✅ 5) Persist + sync
            saveNotes()
            if (oldNote != updatedNote) queueEditForSync(oldNote, updatedNote)

            FancyPillToast.show(
                activity = this,
                text = "Note updated successfully",
                durationMs = 2200L // optional
            )

        }

    // ----------------------- Data / Sync helpers -----------------------

    private fun loadIdMaps() {
        idToContent.clear(); contentToId.clear()
        val json = idPrefs.getString("id_to_content", "{}")!!
        val type = object : TypeToken<Map<String, String>>() {}.type
        idToContent.putAll(Gson().fromJson(json, type))
        contentToId.putAll(idToContent.entries.associate { it.value to it.key })
    }

    private fun saveIdMaps() {
        idPrefs.edit { putString("id_to_content", Gson().toJson(idToContent)) }
    }

    private fun notesOnlineSyncEnabled(): Boolean {
        return getSharedPreferences(NotesPagePrefs.NAME, MODE_PRIVATE).getBoolean(
            NotesPagePrefs.KEY_SYNC_ONLINE,
            NotesPagePrefs.DEFAULT_SYNC_ONLINE
        )
    }

    private fun hasActiveSupabaseSession(): Boolean {
        return notesOnlineSyncEnabled() &&
                SupabaseManager.client.auth.currentSessionOrNull() != null
    }

    private fun canQueueAccountSync(): Boolean {
        return notesOnlineSyncEnabled() &&
                (UserPreferencesManager(this).isLoggedIn ||
                        SupabaseManager.client.auth.currentSessionOrNull() != null)
    }

    private fun pendingDeletesKey(userId: String): String = "pending_deletes_$userId"

    private fun pendingDeletesForUser(userId: String): MutableSet<String> {
        val sp = getSharedPreferences("notes_prefs", MODE_PRIVATE)
        val accountDeletes = sp.getStringSet(pendingDeletesKey(userId), emptySet()).orEmpty()
        val legacyDeletes = sp.getStringSet("pending_deletes", emptySet()).orEmpty()
        return (accountDeletes + legacyDeletes).toMutableSet()
    }

    private fun savePendingDeletesForUser(userId: String, deletes: Set<String>) {
        val sp = getSharedPreferences("notes_prefs", MODE_PRIVATE)
        sp.edit(commit = true) {
            remove("pending_deletes")
            if (deletes.isEmpty()) remove(pendingDeletesKey(userId))
            else putStringSet(pendingDeletesKey(userId), deletes)
        }
    }

    private fun schedulePendingNotesSync(reason: String) {
        if (!notesOnlineSyncEnabled()) return
        if (!canQueueAccountSync()) return
        if (isPendingNotesSyncRunning) {
            pendingNotesSyncRequested = true
            Log.d("SupabaseSync", "Queued another pending note sync while one is running: $reason")
            return
        }

        val syncPrefs = getSharedPreferences("notes_prefs", MODE_PRIVATE)
        val pendingAdds = syncPrefs.getStringSet("pending_adds", emptySet()).orEmpty()
        val userId = SupabaseManager.client.auth.currentSessionOrNull()?.user?.id
        val pendingDeletes = if (userId != null) {
            pendingDeletesForUser(userId)
        } else {
            syncPrefs.getStringSet("pending_deletes", emptySet()).orEmpty()
        }
        if (pendingAdds.isEmpty() && pendingDeletes.isEmpty()) return

        isPendingNotesSyncRunning = true
        lifecycleScope.launch {
            try {
                notesSyncStatus = if (pendingDeletes.isNotEmpty()) {
                    NotesSyncUiStatus.Deleting
                } else {
                    NotesSyncUiStatus.Syncing
                }
                Log.d(
                    "SupabaseSync",
                    "Retrying pending note sync from $reason: adds=${pendingAdds.size}, deletes=${pendingDeletes.size}"
                )
                syncAllDeletesToSupabase()
                val syncSession = SupabaseManager.client.auth.currentSessionOrNull()
                val syncUserId = syncSession?.user?.id
                if (syncSession != null && syncUserId != null) {
                    runCatching {
                        syncLocalNoteFoldersToSupabase(syncSession.accessToken, syncUserId)
                    }.onFailure { Log.e("SupabaseSync", "Folder sync skipped/failed", it) }
                }
                notesSyncStatus = NotesSyncUiStatus.Uploading
                syncLocalNotesToSupabase()
                notesSyncStatus = NotesSyncUiStatus.Downloading
                pullFromSupabaseAndReconcile()
                notesSyncStatus = NotesSyncUiStatus.Synced
            } catch (e: Exception) {
                notesSyncStatus = NotesSyncUiStatus.Error
                Log.e("SupabaseSync", "Pending note sync failed from $reason", e)
            } finally {
                isPendingNotesSyncRunning = false
                if (pendingNotesSyncRequested) {
                    pendingNotesSyncRequested = false
                    schedulePendingNotesSync("queued after $reason")
                }
            }
        }
    }

    private fun uploadNoteNowOrKeepPending(
        content: String,
        titleOverride: String? = null,
        imageUrisOverride: List<Uri>? = null,
        fileItemsOverride: List<NoteAttachmentItem>? = null,
        voiceItemsOverride: List<NoteVoiceItem>? = null,
        hasReminderOverride: Boolean? = null
    ) {
        if (!canQueueAccountSync()) return

        lifecycleScope.launch {
            try {
                notesSyncStatus = NotesSyncUiStatus.Uploading
                Log.d("SupabaseSync", "Uploading note immediately")
                insertNoteToSupabase(
                    content = content,
                    titleOverride = titleOverride,
                    imageUrisOverride = imageUrisOverride,
                    fileItemsOverride = fileItemsOverride,
                    voiceItemsOverride = voiceItemsOverride,
                    hasReminderOverride = hasReminderOverride
                )
                notesSyncStatus = NotesSyncUiStatus.Downloading
                pullFromSupabaseAndReconcile()
                notesSyncStatus = NotesSyncUiStatus.Synced
            } catch (e: Exception) {
                notesSyncStatus = NotesSyncUiStatus.Error
                Log.e("SupabaseSync", "Immediate note upload failed; keeping pending", e)
                queuePendingAdd(content)
                schedulePendingNotesSync("upload retry")
            }
        }
    }

    private fun deleteQueuedNotesNowOrRetry(reason: String) {
        if (!canQueueAccountSync()) return

        lifecycleScope.launch {
            try {
                notesSyncStatus = NotesSyncUiStatus.Deleting
                Log.d("SupabaseSync", "Sending queued note deletes from $reason")
                syncAllDeletesToSupabase()
                notesSyncStatus = NotesSyncUiStatus.Synced
            } catch (e: Exception) {
                notesSyncStatus = NotesSyncUiStatus.Error
                Log.e("SupabaseSync", "Immediate note delete sync failed; keeping pending", e)
            } finally {
                schedulePendingNotesSync("delete retry")
            }
        }
    }

    private fun addLocalNote(content: String, queuePendingSync: Boolean = true) {

        // 1) add to RAW list only
        allNotes.add(content)
        NoteCreatedAtStore.ensure(this, contentToUid[content] ?: ensureLocalUid(content))
        assignNoteToCurrentFolder(content)

        // 2) build DISPLAY list (sorted copy)
        refreshDisplayedNotes()

        // 3) persist RAW
        saveNotes()
        updateNotePlaceholder()
        showFabBadge(null)

        // 4) pending sync
        if (queuePendingSync) queuePendingAdd(content)
    }

    private fun queuePendingAdd(content: String) {
        if (!notesOnlineSyncEnabled()) return
        val sp = getSharedPreferences("notes_prefs", MODE_PRIVATE)
        val pending = sp.getStringSet("pending_adds", emptySet())!!.toMutableSet()
        if (pending.add(content)) {
            sp.edit { putStringSet("pending_adds", pending) }
        }
    }


    private fun saveNotes() {
        val notesJson = Gson().toJson(allNotes) // ✅ raw list only
        sharedPreferences.edit { putString("notes_list", notesJson) }

        // cache should also be raw, not sorted
        NotesCacheManager.cachedNotes = allNotes.toMutableList()
        notesCount = allNotes.size
        refreshFolderRows()
        showFabBadge(null)
    }


    // Make sure every local note is present in the signed-in account,
    // while never re-uploading notes waiting for a cloud delete.
    private suspend fun syncLocalNotesToSupabase() {
        if (!notesOnlineSyncEnabled()) return

        if (!hasActiveSupabaseSession()) {
            Log.w("SupabaseSync", "Keeping pending notes local: no active Supabase session")
            return
        }

        val session = SupabaseManager.client.auth.currentSessionOrNull()
            ?: throw IllegalStateException("No Supabase session")
        val userId = session.user?.id ?: throw IllegalStateException("No Supabase user id")
        runCatching {
            syncLocalNoteFoldersToSupabase(session.accessToken, userId)
        }.onFailure { Log.e("SupabaseSync", "Folder sync skipped/failed", it) }

        val sp = getSharedPreferences("notes_prefs", MODE_PRIVATE)
        val pendingAdds = sp.getStringSet("pending_adds", emptySet())!!.toMutableSet()
        val pendingDeletes = pendingDeletesForUser(userId)
        val remoteRows = fetchActiveRemoteRows(userId)
        val remoteByContent = remoteRows.associateBy { it.content }
        val remoteContents = remoteByContent.keys
        remoteRows.forEach { row ->
            val id = row.id ?: return@forEach
            idToContent[id] = row.content
            contentToId[row.content] = id
        }
        saveIdMaps()

        val stillPending = mutableSetOf<String>()
        val uploadCandidates = (allNotes + pendingAdds)
            .filter { it.isNotBlank() }
            .distinct()

        for (content in uploadCandidates) {
            if (content in pendingDeletes) continue
            try {
                val remote = remoteByContent[content]
                if (remote?.id != null) {
                    syncNoteExtrasToSupabase(
                        content = content,
                        noteId = remote.id,
                        userId = userId,
                        authToken = session.accessToken
                    )
                    removePendingAdd(content)
                } else {
                    insertNoteToSupabase(content)
                }
            } catch (e: Exception) {
                Log.e("SupabaseSync", "Pending note upload failed; keeping pending", e)
                stillPending.add(content)
            }
        }
        sp.edit { putStringSet("pending_adds", stillPending) }
    }

    private suspend fun fetchActiveRemoteRows(userId: String): List<UserNote> = withContext(Dispatchers.IO) {
        val rows: List<UserNote> = SupabaseManager.client
            .postgrest
            .from("user_notes")
            .select { filter { eq("user_id", userId) } }
            .decodeList()
        rows.filter { it.deletedAt == null }
    }

    // Insert one note (suspend + updates local maps and pending_adds on success)
    private suspend fun insertNoteToSupabase(
        content: String,
        titleOverride: String? = null,
        imageUrisOverride: List<Uri>? = null,
        fileItemsOverride: List<NoteAttachmentItem>? = null,
        voiceItemsOverride: List<NoteVoiceItem>? = null,
        hasReminderOverride: Boolean? = null
    ) {
        if (!notesOnlineSyncEnabled()) return

        notesSyncStatus = NotesSyncUiStatus.Uploading
        val session = SupabaseManager.client.auth.currentSessionOrNull()
            ?: throw IllegalStateException("No Supabase session")
        val userId  = session.user?.id ?: throw IllegalStateException("No Supabase user id")

        val inserted = insertNoteInSupabaseRest(
            authToken = session.accessToken,
            userId = userId,
            content = content,
            titleOverride = titleOverride,
            hasReminderOverride = hasReminderOverride
        )

        // update id maps if Supabase returned an id
        val insertedId = inserted.optString("id").takeIf { it.isNotBlank() }
        if (insertedId != null) {
            idToContent[insertedId] = content
            contentToId[content] = insertedId
            saveIdMaps()
            updateNoteMetadataInSupabase(
                authToken = session.accessToken,
                userId = userId,
                noteId = insertedId,
                content = content,
                titleOverride = titleOverride,
                hasReminderOverride = hasReminderOverride
            )
            uploadNoteAttachmentsToSupabase(
                content = content,
                noteId = insertedId,
                userId = userId,
                authToken = session.accessToken,
                imageUrisOverride = imageUrisOverride,
                fileItemsOverride = fileItemsOverride,
                voiceItemsOverride = voiceItemsOverride
            )
        }
        // remove from pending_adds only after note + extras have uploaded
        removePendingAdd(content)
        Log.d("SupabaseSync", "Note inserted via REST: $insertedId")
    }

    private suspend fun insertNoteInSupabaseRest(
        authToken: String,
        userId: String,
        content: String,
        titleOverride: String? = null,
        hasReminderOverride: Boolean? = null
    ): JSONObject = withContext(Dispatchers.IO) {
        val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
        val anonKey = BuildConfig.SUPABASE_ANON_KEY
        if (baseUrl.isBlank() || anonKey.isBlank()) {
            throw IllegalStateException("Missing Supabase config")
        }

        val noteTitle = titleOverride
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: resolveTitle(content)?.takeIf { it.isNotBlank() }

        val body = JSONObject()
            .put("user_id", userId)
            .put("content", content)
            .put("title", noteTitle ?: JSONObject.NULL)
            .put("folder_id", localFolderIdForNote(content))
            .put("has_reminder", hasReminderOverride ?: noteHasReminder(content))
            .put("has_reminder_badge", noteHasReminderBadge(content))
            .toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$baseUrl/rest/v1/user_notes")
            .addHeader("apikey", anonKey)
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

            val rows = JSONArray(responseBody)
            if (rows.length() == 0) {
                throw IllegalStateException("Insert note returned no row")
            }
            rows.getJSONObject(0)
        }
    }


    private fun syncNoteToSupabase(content: String) {
        if (!notesOnlineSyncEnabled()) return

        val prefs = UserPreferencesManager(this)
        if (!prefs.isLoggedIn) return

        val session = SupabaseManager.client.auth.currentSessionOrNull()
        if (session == null) {
            Log.w("SupabaseSync", "Queued note upload waiting for session")
            return
        }
        val userId  = session.user?.id
        if (userId == null) {
            Log.w("SupabaseSync", "Queued note upload waiting for user id")
            return
        }

        lifecycleScope.launch {
            try {
                insertNoteToSupabase(content)
            } catch (e: Exception) {
                Log.e("SupabaseSync", "Error syncing note", e)
                schedulePendingNotesSync("syncNote retry")
            }
        }
    }

    private fun removePendingAdd(content: String) {
        val sp = getSharedPreferences("notes_prefs", MODE_PRIVATE)
        val pending = sp.getStringSet("pending_adds", emptySet())!!.toMutableSet()
        if (pending.remove(content)) {
            sp.edit { putStringSet("pending_adds", pending) }
        }
    }

    private suspend fun uploadNoteAttachmentsToSupabase(
        content: String,
        noteId: String,
        userId: String,
        authToken: String,
        replaceImages: Boolean = false,
        replaceAttachments: Boolean = false,
        includeOtherAttachments: Boolean = true,
        imageUrisOverride: List<Uri>? = null,
        fileItemsOverride: List<NoteAttachmentItem>? = null,
        voiceItemsOverride: List<NoteVoiceItem>? = null
    ) = withContext(Dispatchers.IO) {
        val imageUris = imageUrisOverride ?: NoteMediaStore.getUris(this@AllNotesActivity, content)
        val fileItems = if (includeOtherAttachments) {
            fileItemsOverride ?: NoteAttachmentStore.getItems(this@AllNotesActivity, content)
        } else {
            emptyList()
        }
        val voiceItems = if (includeOtherAttachments) {
            voiceItemsOverride ?: NoteVoiceStore.getItems(this@AllNotesActivity, content)
        } else {
            emptyList()
        }
        val uploadedImages = mutableListOf<UserNoteImage>()
        val uploaded = mutableListOf<UserNoteAttachment>()
        Log.d(
            "SupabaseSync",
            "Uploading note extras noteId=$noteId title=${resolveTitle(content).orEmpty()} images=${imageUris.size} files=${fileItems.size} voice=${voiceItems.size}"
        )

        if (replaceImages) {
            deleteNoteImageRowsInSupabase(
                authToken = authToken,
                userId = userId,
                noteId = noteId
            )
        }
        if (replaceAttachments) {
            deleteNoteAttachmentRowsInSupabase(
                authToken = authToken,
                userId = userId,
                noteId = noteId
            )
        }

        imageUris.forEachIndexed { index, uri ->
            val fileName = noteImageFileName(uri, index)
            val mime = contentResolver.getType(uri) ?: "image/jpeg"
            val path = SupabaseStorageUploader.uploadNoteAttachmentAndReturnPath(
                context = this@AllNotesActivity,
                userId = userId,
                authToken = authToken,
                noteId = noteId,
                sourceUri = uri,
                fileName = fileName,
                mimeHint = mime
            ) ?: throw IllegalStateException("Image upload failed: $fileName")
            val dimensions = noteImageDimensions(uri)
            uploadedImages += UserNoteImage(
                userId = userId,
                noteId = noteId,
                path = path,
                mimeType = mime,
                width = dimensions?.first,
                height = dimensions?.second
            )
        }

        fileItems.forEach { item ->
            if (!replaceAttachments && !item.remotePath.isNullOrBlank()) return@forEach
            val path = SupabaseStorageUploader.uploadNoteAttachmentAndReturnPath(
                context = this@AllNotesActivity,
                userId = userId,
                authToken = authToken,
                noteId = noteId,
                sourceUri = item.asUri,
                fileName = item.name,
                mimeHint = item.mime
            ) ?: throw IllegalStateException("Attachment upload failed: ${item.name}")
            NoteAttachmentStore.updateRemotePath(this@AllNotesActivity, content, item.uri, path)
            uploaded += UserNoteAttachment(
                userId = userId,
                noteId = noteId,
                storagePath = path,
                fileName = item.name,
                mimeType = item.mime,
                sizeBytes = item.sizeBytes,
                kind = when {
                    item.isAudioAttachment() -> "audio"
                    item.isVideoAttachment() -> "video"
                    else -> "file"
                }
            )
        }

        voiceItems.forEachIndexed { index, item ->
            val path = SupabaseStorageUploader.uploadNoteAttachmentAndReturnPath(
                context = this@AllNotesActivity,
                userId = userId,
                authToken = authToken,
                noteId = noteId,
                sourceUri = item.asUri,
                fileName = "voice_${index + 1}.m4a",
                mimeHint = "audio/mp4"
            ) ?: throw IllegalStateException("Voice upload failed: voice_${index + 1}.m4a")
            uploaded += UserNoteAttachment(
                userId = userId,
                noteId = noteId,
                storagePath = path,
                fileName = "voice_${index + 1}.m4a",
                mimeType = "audio/mp4",
                sizeBytes = java.io.File(item.asUri.path.orEmpty()).length().coerceAtLeast(0L),
                kind = "voice",
                durationMs = item.durationMs,
                createdAtMs = item.createdAtMs
            )
        }

        if (uploadedImages.isNotEmpty()) {
            insertNoteImageRowsInSupabase(
                authToken = authToken,
                rows = uploadedImages
            )
        }

        if (uploaded.isNotEmpty()) {
            insertNoteAttachmentRowsInSupabase(
                authToken = authToken,
                rows = uploaded
            )
        }
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
        insertRowsInSupabaseRest(
            authToken = authToken,
            table = "note_images",
            body = body
        )
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
        insertRowsInSupabaseRest(
            authToken = authToken,
            table = "user_note_attachments",
            body = body
        )
    }

    private suspend fun insertRowsInSupabaseRest(
        authToken: String,
        table: String,
        body: String
    ) = withContext(Dispatchers.IO) {
        val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
        val anonKey = BuildConfig.SUPABASE_ANON_KEY
        if (baseUrl.isBlank() || anonKey.isBlank()) {
            throw IllegalStateException("Missing Supabase config")
        }

        val request = Request.Builder()
            .url("$baseUrl/rest/v1/$table")
            .addHeader("apikey", anonKey)
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

    private suspend fun deleteNoteImageRowsInSupabase(
        authToken: String,
        userId: String,
        noteId: String
    ) = withContext(Dispatchers.IO) {
        val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
        val anonKey = BuildConfig.SUPABASE_ANON_KEY
        if (baseUrl.isBlank() || anonKey.isBlank()) return@withContext

        val request = Request.Builder()
            .url(
                "$baseUrl/rest/v1/note_images" +
                        "?user_id=eq.${urlEncode(userId)}" +
                        "&note_id=eq.${urlEncode(noteId)}"
            )
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer $authToken")
            .addHeader("Prefer", "return=minimal")
            .delete()
            .build()

        notesHttpClient.newCall(request).execute().use { response ->
            val responseBody = runCatching { response.body.string() }.getOrDefault("")
            if (!response.isSuccessful) {
                throw IllegalStateException("Delete note images failed: ${response.code} $responseBody")
            }
        }
    }

    private suspend fun deleteNoteAttachmentRowsInSupabase(
        authToken: String,
        userId: String,
        noteId: String
    ) = withContext(Dispatchers.IO) {
        val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
        val anonKey = BuildConfig.SUPABASE_ANON_KEY
        if (baseUrl.isBlank() || anonKey.isBlank()) return@withContext

        val request = Request.Builder()
            .url(
                "$baseUrl/rest/v1/user_note_attachments" +
                        "?user_id=eq.${urlEncode(userId)}" +
                        "&note_id=eq.${urlEncode(noteId)}"
            )
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer $authToken")
            .addHeader("Prefer", "return=minimal")
            .delete()
            .build()

        notesHttpClient.newCall(request).execute().use { response ->
            val responseBody = runCatching { response.body.string() }.getOrDefault("")
            if (!response.isSuccessful) {
                throw IllegalStateException("Delete note attachments failed: ${response.code} $responseBody")
            }
        }
    }

    private fun syncExistingNoteExtrasToSupabase(content: String) {
        if (!canQueueAccountSync()) return

        val session = SupabaseManager.client.auth.currentSessionOrNull() ?: return
        val userId = session.user?.id ?: return
        val noteId = contentToId[content]
        if (noteId.isNullOrBlank()) {
            Log.w("SupabaseSync", "Skipping extras sync for existing note without remote id")
            return
        }

        lifecycleScope.launch {
            try {
                notesSyncStatus = NotesSyncUiStatus.Uploading
                syncNoteExtrasToSupabase(
                    content = content,
                    noteId = noteId,
                    userId = userId,
                    authToken = session.accessToken
                )
                notesSyncStatus = NotesSyncUiStatus.Synced
            } catch (e: Exception) {
                notesSyncStatus = NotesSyncUiStatus.Error
                Log.e("SupabaseSync", "Existing note extras sync failed", e)
            }
        }
    }

    private suspend fun syncNoteExtrasToSupabase(
        content: String,
        noteId: String,
        userId: String,
        authToken: String
    ) {
        updateNoteMetadataInSupabase(
            authToken = authToken,
            userId = userId,
            noteId = noteId,
            content = content
        )
        uploadNoteAttachmentsToSupabase(
            content = content,
            noteId = noteId,
            userId = userId,
            authToken = authToken,
            replaceImages = true,
            replaceAttachments = true,
            includeOtherAttachments = true
        )
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
        val anonKey = BuildConfig.SUPABASE_ANON_KEY
        if (baseUrl.isBlank() || anonKey.isBlank()) return@withContext

        val noteTitle = titleOverride
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: resolveTitle(content)?.takeIf { it.isNotBlank() }

        val body = JSONObject()
            .put("title", noteTitle ?: JSONObject.NULL)
            .put("folder_id", localFolderIdForNote(content))
            .put("has_reminder", hasReminderOverride ?: noteHasReminder(content))
            .put("has_reminder_badge", noteHasReminderBadge(content))
            .toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(
                "$baseUrl/rest/v1/user_notes" +
                        "?user_id=eq.${urlEncode(userId)}" +
                        "&id=eq.${urlEncode(noteId)}"
            )
            .addHeader("apikey", anonKey)
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

    private fun noteImageFileName(uri: Uri, index: Int): String {
        val raw = uri.lastPathSegment
            ?.substringAfterLast('/')
            ?.takeIf { it.isNotBlank() && "." in it }
        return raw ?: "photo_${index + 1}.jpg"
    }

    private fun noteImageDimensions(uri: Uri): Pair<Int, Int>? =
        runCatching {
            contentResolver.openInputStream(uri)?.use { stream ->
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(stream, null, options)
                val width = options.outWidth.takeIf { it > 0 }
                val height = options.outHeight.takeIf { it > 0 }
                if (width != null && height != null) width to height else null
            }
        }.getOrNull()

    private data class RemoteNoteExtras(
        val images: Map<String, List<Uri>>,
        val files: Map<String, List<NoteAttachmentItem>>,
        val voice: Map<String, List<NoteVoiceItem>>
    )

    private suspend fun fetchRemoteNoteExtras(
        rows: List<UserNote>,
        authToken: String
    ): RemoteNoteExtras = withContext(Dispatchers.IO) {
        val imagesByContent = mutableMapOf<String, List<Uri>>()
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
    ): List<Uri> = withContext(Dispatchers.IO) {
        val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
        val anonKey = BuildConfig.SUPABASE_ANON_KEY
        if (baseUrl.isBlank() || anonKey.isBlank()) return@withContext emptyList()

        val request = Request.Builder()
            .url(
                "$baseUrl/rest/v1/note_images" +
                        "?select=path" +
                        "&user_id=eq.${urlEncode(userId)}" +
                        "&note_id=eq.${urlEncode(noteId)}" +
                        "&order=created_at.asc"
            )
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer $authToken")
            .build()

        notesHttpClient.newCall(request).execute().use { response ->
            val body = runCatching { response.body.string() }.getOrDefault("")
            if (!response.isSuccessful) {
                Log.e("SupabaseSync", "Fetch note images failed: ${response.code} $body")
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
        val anonKey = BuildConfig.SUPABASE_ANON_KEY
        if (baseUrl.isBlank() || anonKey.isBlank()) return@withContext emptyList<NoteAttachmentItem>() to emptyList()

        val request = Request.Builder()
            .url(
                "$baseUrl/rest/v1/user_note_attachments" +
                        "?select=*" +
                        "&user_id=eq.${urlEncode(userId)}" +
                        "&note_id=eq.${urlEncode(noteId)}"
            )
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer $authToken")
            .build()

        notesHttpClient.newCall(request).execute().use { response ->
            val body = runCatching { response.body.string() }.getOrDefault("")
            if (!response.isSuccessful) {
                Log.e("SupabaseSync", "Fetch note attachments failed: ${response.code} $body")
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

    private suspend fun pullFromSupabaseAndReconcile() {
        if (!notesOnlineSyncEnabled()) return

        notesSyncStatus = NotesSyncUiStatus.Downloading
        val session = SupabaseManager.client.auth.currentSessionOrNull()
        if (session == null) {
            Log.w("SupabaseSync", "Skipping notes pull: no Supabase session")
            return
        }
        val userId  = session.user?.id
        if (userId == null) {
            Log.w("SupabaseSync", "Skipping notes pull: no Supabase user id")
            return
        }

        runCatching {
            NoteFolderStore.mergeRemoteFolders(this, fetchRemoteNoteFolders(session.accessToken, userId))
        }.onFailure { Log.e("SupabaseSync", "Remote folder pull skipped/failed", it) }

        // 1) fetch remote snapshot
        val activeRemoteRows = fetchActiveRemoteRows(userId)
        val remoteExtras = fetchRemoteNoteExtras(
            rows = activeRemoteRows,
            authToken = session.accessToken
        )
        val remoteContents = activeRemoteRows.map { it.content }.toSet()

        // 2) load pending sets
        val sp = getSharedPreferences("notes_prefs", MODE_PRIVATE)
        val pendingAdds    = sp.getStringSet("pending_adds", emptySet())!!.toSet()
        val pendingDeletes = pendingDeletesForUser(userId).toSet()
        Log.d(
            "SupabaseSync",
            "Pull notes remote=${activeRemoteRows.size}, local=${allNotes.size}, pendingAdds=${pendingAdds.size}, pendingDeletes=${pendingDeletes.size}"
        )

        // ✅ IMPORTANT: local truth is allNotes (RAW)
        val localSet = allNotes.toSet()

        // 3) remote -> local inserts
        val toInsertLocally = remoteContents.filter { it !in localSet && it !in pendingDeletes }

        // ✅ delete locally only if it was a remote note before (has remote id)
        val toDeleteLocally = localSet.filter { content ->
            content !in remoteContents &&
                    content !in pendingAdds &&
                    contentToId.containsKey(content)
        }

        // 4) remote -> local updates by id map
        val remoteIdToContent = activeRemoteRows.mapNotNull { r -> r.id?.let { it to r.content } }.toMap()
        val updates: List<Pair<String, String>> = buildList {
            for ((rid, rContent) in remoteIdToContent) {
                val old = idToContent[rid]
                if (old != null && old != rContent) add(old to rContent)
            }
        }

        withContext(Dispatchers.Main) {

            // ✅ APPLY EVERYTHING TO allNotes (raw)

            // updates
            for ((oldContent, newContent) in updates) {
                val idx = allNotes.indexOf(oldContent)
                if (idx != -1 && oldContent !in pendingDeletes) {
                    allNotes[idx] = newContent

                    // migrate titles/badges/bells (same as you did)
                    notesAdapter.migrateUserTitle(oldContent, newContent)

                    val badgePrefs = getSharedPreferences("reminder_badges", MODE_PRIVATE)
                    val hadBadge = badgePrefs.getBoolean(oldContent.hashCode().toString(), false)
                    badgePrefs.edit {
                        remove(oldContent.hashCode().toString())
                        if (hadBadge) putBoolean(newContent.hashCode().toString(), true)
                    }

                    val bellPrefs = getSharedPreferences("reminder_flags", MODE_PRIVATE)
                    val hadBell = bellPrefs.getBoolean(oldContent.hashCode().toString(), false)
                    bellPrefs.edit {
                        remove(oldContent.hashCode().toString())
                        if (hadBell) putBoolean(newContent.hashCode().toString(), true)
                    }
                }

                // refresh id maps
                val rid = contentToId[oldContent] ?: remoteIdToContent.entries.firstOrNull { it.value == newContent }?.key
                rid?.let {
                    idToContent[it] = newContent
                    contentToId.remove(oldContent)
                    contentToId[newContent] = it
                }
            }

            // inserts
            toInsertLocally.forEach { allNotes.add(it) }

            activeRemoteRows.forEach { row ->
                if (row.content in pendingDeletes) return@forEach
                rememberRemoteCreatedAt(row)
                NoteFolderStore.assignRemoteNoteToFolder(
                    this@AllNotesActivity,
                    contentToUid[row.content] ?: ensureLocalUid(row.content),
                    row.folderId
                )

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
                    if (images.isEmpty()) NoteMediaStore.deleteAllForNote(this@AllNotesActivity, row.content)
                    else NoteMediaStore.setUris(this@AllNotesActivity, row.content, images)
                }
                remoteExtras.files[row.content]?.let { files ->
                    NoteAttachmentStore.setItems(this@AllNotesActivity, row.content, files)
                }
                remoteExtras.voice[row.content]?.let { voice ->
                    NoteVoiceStore.setItems(this@AllNotesActivity, row.content, voice)
                }
            }
            notesAdapter.preloadReminderFlags(this@AllNotesActivity)
            notesAdapter.preloadBadgeStates(this@AllNotesActivity)

            // deletes
            if (toDeleteLocally.isNotEmpty()) {
                toDeleteLocally.forEach { c ->
                    notesAdapter.removeUserTitle(c)
                    NotesCacheManager.cachedTitles.remove(c)
                    getSharedPreferences("reminder_badges", MODE_PRIVATE).edit { remove(c.hashCode().toString()) }
                    getSharedPreferences("reminder_flags", MODE_PRIVATE).edit { remove(c.hashCode().toString()) }
                    NoteMediaStore.deleteAllForNote(this@AllNotesActivity, c)
                    NoteVoiceStore.deleteAllForNote(this@AllNotesActivity, c)
                    NoteAttachmentStore.deleteAllForNote(this@AllNotesActivity, c)
                    removeUidFor(c)
                }
                allNotes.removeAll(toDeleteLocally.toSet())
            }

            refreshDisplayedNotes()

            // ✅ persist raw
            saveNotes()
            saveIdMaps()

            showFabBadge(null)
        }

        metaPrefs.edit { putLong("last_server_updated_at", System.currentTimeMillis()) }
        notesSyncStatus = NotesSyncUiStatus.Synced
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

    // ----------------------- UI helpers (Compose mode) -----------------------

    private fun resolveTitle(note: String): String? {
        return notesAdapter.getUserTitle(note)
            ?: NotesCacheManager.cachedTitles[note]?.let {
                HtmlCompat.fromHtml(it, HtmlCompat.FROM_HTML_OPTION_USE_CSS_COLORS).toString()
            }
    }

    private fun noteHasReminder(note: String): Boolean =
        getSharedPreferences("reminder_flags", MODE_PRIVATE)
            .getBoolean(note.hashCode().toString(), false)

    private fun noteHasReminderBadge(note: String): Boolean =
        getSharedPreferences("reminder_badges", MODE_PRIVATE)
            .getBoolean(note.hashCode().toString(), false)

    private fun refreshNotesDisplayFromSettings() {
        val settings = readNotesPageSettings()

        notesAdapter.applyPageSettings(
            compact = settings.compact,
            showImagesBadge = settings.showImagesBadge,
            showReminderBadge = settings.showReminderBadge,
            showReminderBell = settings.showReminderBell,
            titleTopCompactDp = settings.titleTopCompactDp,
            titleTopNormalDp = settings.titleTopNormalDp
        )
        notesAdapter.preloadReminderFlags(this)
        notesAdapter.preloadBadgeStates(this)

        refreshDisplayedNotes()
    }

    @Suppress("DEPRECATION")
    private fun onNoteClick(note: String, position: Int) {
        if (isMultiSelectMode) {
            toggleNoteSelection(note)
            return
        }

        val title = resolveTitle(note)

        // 🔴 TEMP UID (works but not ideal)
        val uid = note

        val i = ViewNoteComposeActivity.newIntent(this, uid, note, position, title)
        viewNoteLauncher.launch(i)

        overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation)
    }




    private fun onNoteLongClick(note: String) {
        isMultiSelectMode = true
        toggleNoteSelection(note)
    }
    @Suppress("DEPRECATION")
    private val viewNoteLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data        = result.data
                val updatedNote = data?.getStringExtra("UPDATED_NOTE")
                val updatedTitle= data?.getStringExtra("UPDATED_TITLE")
                val position    = data?.getIntExtra("NOTE_POSITION", -1) ?: -1
                val updatedWantsReminder = data?.getBooleanExtra("UPDATED_NOTE_WANTS_REMINDER", false) == true

                val updatedImageStrings = data?.getStringArrayListExtra("UPDATED_IMAGES") ?: arrayListOf()
                val updatedImageUris = updatedImageStrings.mapNotNull { runCatching { it.toUri() }.getOrNull() }
                val updatedFileUris = data?.getStringArrayListExtra("UPDATED_FILE_URIS") ?: arrayListOf()
                val updatedFileNames = data?.getStringArrayListExtra("UPDATED_FILE_NAMES") ?: arrayListOf()
                val updatedFileMimes = data?.getStringArrayListExtra("UPDATED_FILE_MIMES") ?: arrayListOf()
                val updatedFileSizes = data?.getLongArrayExtra("UPDATED_FILE_SIZES") ?: longArrayOf()
                val updatedFiles = updatedFileUris.mapIndexed { index, uri ->
                    NoteAttachmentItem(
                        uri = uri,
                        name = updatedFileNames.getOrNull(index).orEmpty().ifBlank { "Attachment ${index + 1}" },
                        mime = updatedFileMimes.getOrNull(index)?.takeIf { it.isNotBlank() },
                        sizeBytes = updatedFileSizes.getOrNull(index) ?: 0L
                    )
                }
                val updatedVoiceUris = data?.getStringArrayListExtra("UPDATED_VOICE_URIS") ?: arrayListOf()
                val updatedVoiceDurations = data?.getLongArrayExtra("UPDATED_VOICE_DURATIONS") ?: longArrayOf()
                val updatedVoiceCreatedAt = data?.getLongArrayExtra("UPDATED_VOICE_CREATED_AT") ?: longArrayOf()
                val updatedVoiceItems = updatedVoiceUris.mapIndexed { index, uri ->
                    NoteVoiceItem(
                        uri = uri,
                        durationMs = updatedVoiceDurations.getOrNull(index) ?: 0L,
                        createdAtMs = updatedVoiceCreatedAt.getOrNull(index) ?: System.currentTimeMillis()
                    )
                }

                if (!updatedNote.isNullOrEmpty() && position in notesText.indices) {

                    // old note from CURRENT VISIBLE list
                    val oldNote = notesText[position]

                    // 1) migrate title + media key if content changed
                    if (oldNote != updatedNote) {
                        notesAdapter.migrateUserTitle(oldNote, updatedNote)
                        NoteMediaStore.migrateNoteKey(this, oldNote, updatedNote)
                        NoteVoiceStore.migrateNoteKey(this, oldNote, updatedNote)
                        NoteAttachmentStore.migrateNoteKey(this, oldNote, updatedNote)

                        // keep uid mapping consistent
                        contentToUid[updatedNote] = contentToUid.remove(oldNote) ?: ensureLocalUid(updatedNote)
                        uidToContent.entries.firstOrNull { it.value == oldNote }?.let { entry ->
                            uidToContent[entry.key] = updatedNote
                        }
                        saveUidMaps()
                    }

                    // 2) update RAW truth (allNotes)
                    val rawIdx = allNotes.indexOf(oldNote)
                    if (rawIdx != -1) {
                        allNotes[rawIdx] = updatedNote
                    } else {
                        // fallback: if raw not found, at least update display list item
                        notesText[position] = updatedNote
                    }

                    // 3) persist images under NEW content
                    NoteMediaStore.setUris(this, updatedNote, updatedImageUris)
                    NoteVoiceStore.setItems(this, updatedNote, updatedVoiceItems)
                    NoteAttachmentStore.setItems(this, updatedNote, updatedFiles)

                    // 4) rebuild DISPLAY from the active folder
                    refreshDisplayedNotes()

                    // 6) apply title store
                    if (!updatedTitle.isNullOrBlank()) notesAdapter.setUserTitle(updatedNote, updatedTitle)
                    else notesAdapter.removeUserTitle(updatedNote)
                    getSharedPreferences("reminder_flags", MODE_PRIVATE).edit {
                        if (updatedWantsReminder) putBoolean(updatedNote.hashCode().toString(), true)
                        else remove(updatedNote.hashCode().toString())
                    }
                    notesAdapter.preloadReminderFlags(this)

                    // 7) persist + sync
                    saveNotes()
                    if (oldNote != updatedNote) queueEditForSync(oldNote, updatedNote)
                    else syncExistingNoteExtrasToSupabase(updatedNote)

                    FancyPillToast.show(
                        activity = this,
                        text = "Note updated successfully",
                        durationMs = 2200L
                    )
                }
            }
        }


    // Legacy placeholder TextView animation – now no-op in Compose (AllNotesScreen handles empty state)
    private fun updateNotePlaceholder() { /* no-op */ }

    // Legacy FAB badge APIs – now no-op in Compose mode
    override fun onResume() {
        super.onResume()
        showFabBadge(null)
        schedulePendingNotesSync("onResume")
    }
    @Suppress("OPT_IN_ARGUMENT_IS_NOT_MARKER", "SameParameterValue")
    @OptIn(ExperimentalBadgeUtils::class) private fun showFabBadge(@Suppress("UNUSED_PARAMETER") fab: com.google.android.material.floatingactionbutton.FloatingActionButton?) {}
    @Suppress("OPT_IN_ARGUMENT_IS_NOT_MARKER")
    @OptIn(ExperimentalBadgeUtils::class) private fun removeFabBadge(@Suppress("UNUSED_PARAMETER",
        "SameParameterValue"
    ) fab: com.google.android.material.floatingactionbutton.FloatingActionButton?) {}

    // NavRail XML helpers – stubbed (you can wire Compose state later)
    private fun updateDeleteButtonUI(@Suppress("UNUSED_PARAMETER", "SameParameterValue") isEnabled: Boolean) { /* no-op */ }
    private fun enableDeleteInNavigationRail(@Suppress("UNUSED_PARAMETER", "SameParameterValue") enable: Boolean) { /* no-op */ }


    private fun toggleNoteSelection(note: String) {
        val key = contentToUid[note] ?: ensureLocalUid(note)
        if (selectedKeys.contains(key)) selectedKeys.remove(key) else selectedKeys.add(key)
        isMultiSelectMode = selectedKeys.isNotEmpty()
        notesAdapter.toggleSelectionByKey(key)   // key-based toggle

        setDeleteVisibleFromActivity?.invoke(isMultiSelectMode)
    }


    private suspend fun deleteNoteFromSupabase(noteContent: String) {
        if (!notesOnlineSyncEnabled()) return

        try {
            val session = SupabaseManager.client.auth.currentSessionOrNull()
                ?: throw IllegalStateException("No Supabase session")
            val userId = session.user?.id ?: throw IllegalStateException("No Supabase user id")
            val id = contentToId[noteContent]

            if (id != null) {
                softDeleteNoteInSupabase(
                    authToken = session.accessToken,
                    userId = userId,
                    id = id,
                    content = null
                )
                contentToId.remove(noteContent)?.let { idToContent.remove(it) }
                saveIdMaps()
            } else {
                val remoteContents = fetchActiveRemoteRows(userId).map { it.content }.toSet()
                if (noteContent in remoteContents) {
                    softDeleteNoteInSupabase(
                        authToken = session.accessToken,
                        userId = userId,
                        id = null,
                        content = noteContent
                    )
                } else {
                    Log.d("Supabase", "Skipping cloud delete; note is not active in Supabase")
                }
            }
            Log.d("Supabase", "✅ Marked note deleted in Supabase: $noteContent")
        } catch (e: Exception) {
            Log.e("Supabase", "❌ Failed to delete from Supabase", e)
            throw e
        }
    }

    private suspend fun softDeleteNoteInSupabase(
        authToken: String,
        userId: String,
        id: String?,
        content: String?
    ): Boolean = withContext(Dispatchers.IO) {
        val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
        val anonKey = BuildConfig.SUPABASE_ANON_KEY
        if (baseUrl.isBlank() || anonKey.isBlank()) {
            throw IllegalStateException("Missing Supabase config")
        }

        val filters = buildList {
            add("user_id=eq.${urlEncode(userId)}")
            if (!id.isNullOrBlank()) {
                add("id=eq.${urlEncode(id)}")
            } else {
                add("content=eq.${urlEncode(content.orEmpty())}")
            }
        }.joinToString("&")

        val request = Request.Builder()
            .url("$baseUrl/rest/v1/user_notes?$filters")
            .addHeader("apikey", anonKey)
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

    private fun urlEncode(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")

    private fun deleteSelectedNotes() {
        val deletedFromRows = noteRows.mapIndexedNotNull { index, row ->
            if (row.id in selectedKeys) {
                Triple(row.text, index, notesAdapter.getUserTitle(row.text))
            } else {
                null
            }
        }
        val deleted: List<Triple<String, Int, String?>> =
            if (deletedFromRows.isNotEmpty()) {
                deletedFromRows
            } else {
                selectedKeys.mapNotNull { key ->
                    val note = uidToContent[key] ?: return@mapNotNull null
                    val idx = notesText.indexOf(note)
                    if (idx != -1) Triple(note, idx, notesAdapter.getUserTitle(note)) else null
                }
            }.sortedByDescending { it.second }

        // ✅ if nothing resolved, do nothing (prevents "deleted" toast with no real delete)
        if (deleted.isEmpty()) {
            selectedKeys.clear()
            isMultiSelectMode = false
            notesAdapter.clearSelection()
            setDeleteVisibleFromActivity?.invoke(false)
            return
        }

        val imagesByNote: Map<String, List<Uri>> =
            deleted.associate { (note, _, _) -> note to NoteMediaStore.getUris(this, note) }

        val notesToDelete: List<String> = deleted.map { it.first }

        val distinctSelected = notesToDelete.distinct()

        val deleteMsg = when (distinctSelected.size) {
            1 -> "Note deleted"
            else -> "Deleted selected notes"
        }

        notesToDelete.forEach { note ->
            allNotes.remove(note)
            NotesCacheManager.cachedNotes.remove(note)
        }


        val sp = getSharedPreferences("notes_prefs", MODE_PRIVATE)
        val deleteSession = SupabaseManager.client.auth.currentSessionOrNull()
        val remoteDeleteUserId = deleteSession?.user?.id?.takeIf { notesOnlineSyncEnabled() }
        val canDeleteRemote = remoteDeleteUserId != null
        val notesRemovedEverywhere = distinctSelected.filter { note -> note !in allNotes }

        // 1) queue for server-side delete only while the user is signed in.
        // Guest/local deletes should not erase the account's Supabase notes.
        if (remoteDeleteUserId != null) {
            val pendingDeletes = pendingDeletesForUser(remoteDeleteUserId)
            notesRemovedEverywhere.forEach { pendingDeletes.add(it) }
            savePendingDeletesForUser(remoteDeleteUserId, pendingDeletes)
        }

        // also yank them out of pending_adds so they can't be uploaded later
        run {
            val pendingAdds = sp.getStringSet("pending_adds", emptySet())!!.toMutableSet()
            var changed = false
            notesRemovedEverywhere.forEach { if (pendingAdds.remove(it)) changed = true }
            if (changed) sp.edit { putStringSet("pending_adds", pendingAdds) }
        }

        // 2) fire or retry Supabase deletes for signed-in deletes
        if (canDeleteRemote) {
            deleteQueuedNotesNowOrRetry("delete note")
        }

        // 3) local cleanup (titles, flags, etc.)
        deleted.forEach { (note, _, _) ->
            if (note !in allNotes) {
                notesAdapter.removeUserTitle(note)
                NotesCacheManager.cachedTitles.remove(note)
                getSharedPreferences("reminder_badges", MODE_PRIVATE).edit { remove(note.hashCode().toString()) }
                getSharedPreferences("reminder_flags", MODE_PRIVATE).edit { remove(note.hashCode().toString()) }
            }
        }

        // ✅ IMPORTANT: rebuild DISPLAY from RAW (no removeAt(index))
        run {
            refreshDisplayedNotes()
            if (notesText.isEmpty()) removeFabBadge(null)
        }

        // 4) clear selection
        selectedKeys.clear()
        isMultiSelectMode = false
        notesAdapter.clearSelection()
        setDeleteVisibleFromActivity?.invoke(false)

        // persist
        // ✅ now persist (saves allNotes + refreshes cache)
        saveNotes()

// ✅ FORCE DISK WRITE (apply() can be lost if you restart fast)
        sharedPreferences.edit(commit = true) {
            putString("notes_list", Gson().toJson(allNotes))
        }
        NotesCacheManager.cachedNotes = allNotes.toMutableList()

        notesCount = notesText.size
        updateNotePlaceholder()


        FancyPillToast.showUndo(
            activity = this,
            text = deleteMsg,
            actionText = "UNDO",
            durationMs = 3500L,
            onAction = {
                // ✅ restore RAW first
                notesToDelete.forEach { note ->
                    allNotes.add(note)
                }
                NotesCacheManager.cachedNotes = allNotes.toMutableList()

                // restore titles + images
                deleted.forEach { (note, _, savedTitle) ->
                    if (!savedTitle.isNullOrBlank()) notesAdapter.setUserTitle(note, savedTitle)
                    imagesByNote[note]?.let { uris -> NoteMediaStore.setUris(this, note, uris) }
                }

                // ✅ rebuild DISPLAY from RAW again
                refreshDisplayedNotes()

                saveNotes()

                sharedPreferences.edit(commit = true) {
                    putString("notes_list", Gson().toJson(allNotes))
                }
                NotesCacheManager.cachedNotes = allNotes.toMutableList()

                updateNotePlaceholder()
                showFabBadge(null)


                if (remoteDeleteUserId != null) {
                    val sp2 = getSharedPreferences("notes_prefs", MODE_PRIVATE)
                    val pDel2 = pendingDeletesForUser(remoteDeleteUserId)
                    notesRemovedEverywhere.forEach { pDel2.remove(it) }

                    val pAdd2 = sp2.getStringSet("pending_adds", emptySet())!!.toMutableSet()
                    notesRemovedEverywhere.forEach { pAdd2.add(it) }

                    savePendingDeletesForUser(remoteDeleteUserId, pDel2)
                    sp2.edit { putStringSet("pending_adds", pAdd2) }
                }

                selectedKeys.clear()
                isMultiSelectMode = false
                notesAdapter.clearSelection()
                setDeleteVisibleFromActivity?.invoke(false)
                updateDeleteButtonUI(false)
                enableDeleteInNavigationRail(false)
            },
            onTimeout = {
                // finalize media + uid cleanup
                deleted.forEach { (note, _, _) ->
                    if (note !in allNotes) {
                        NoteMediaStore.deleteAllForNote(this@AllNotesActivity, note)
                        NoteVoiceStore.deleteAllForNote(this@AllNotesActivity, note)
                        NoteAttachmentStore.deleteAllForNote(this@AllNotesActivity, note)
                        removeUidFor(note)
                    }
                }

                // ✅ FORCE DISK WRITE so delete survives restart
                saveNotes()
                sharedPreferences.edit(commit = true) {
                    putString("notes_list", Gson().toJson(allNotes))
                }
                NotesCacheManager.cachedNotes = allNotes.toMutableList()
            }

        )
    }


        // ----------------------- Search & pickers (can stay XML-based dialogs) -----------------------

    private fun loadNotesHeadless() {
        val cached = NotesCacheManager.cachedNotes
        val base: List<String> = when {
            cached.isNotEmpty() -> cached.toList()
            else -> {
                val notesJson = sharedPreferences.getString("notes_list", null)
                if (!notesJson.isNullOrEmpty()) {
                    val type = object : TypeToken<MutableList<String>>() {}.type
                    val saved: MutableList<String> = Gson().fromJson(notesJson, type)
                    NotesCacheManager.cachedNotes = saved.toMutableList()
                    saved.toList()
                } else emptyList()
            }
        }

        // ✅ keep RAW truth consistent
        allNotes.clear()
        allNotes.addAll(base)

        refreshDisplayedNotes()
    }


    private fun resolveThemeColor(attr: Int): Int {
        val tv = android.util.TypedValue()
        theme.resolveAttribute(attr, tv, true)
        return if (tv.resourceId != 0) {
            ContextCompat.getColor(this, tv.resourceId)
        } else {
            tv.data
        }
    }

    private fun deleteSelectedNoteFolders(selectedFolderIds: Set<String>) {
        if (selectedFolderIds.isEmpty()) return
        ensureFolderAssignmentsFor(allNotes)

        val notesToDelete = allNotes.filter { note ->
            NoteFolderStore.folderForNoteKey(this, contentToUid[note] ?: ensureLocalUid(note)) in selectedFolderIds
        }.distinct()

        val deleteSession = SupabaseManager.client.auth.currentSessionOrNull()
        val remoteDeleteUserId = deleteSession?.user?.id?.takeIf { notesOnlineSyncEnabled() }
        if (remoteDeleteUserId != null) {
            val pendingDeletes = pendingDeletesForUser(remoteDeleteUserId)
            notesToDelete.forEach { pendingDeletes.add(it) }
            savePendingDeletesForUser(remoteDeleteUserId, pendingDeletes)
            deleteQueuedNotesNowOrRetry("delete folder")
        }

        val sp = getSharedPreferences("notes_prefs", MODE_PRIVATE)
        val pendingAdds = sp.getStringSet("pending_adds", emptySet())!!.toMutableSet()
        if (notesToDelete.any { pendingAdds.remove(it) }) {
            sp.edit { putStringSet("pending_adds", pendingAdds) }
        }

        notesToDelete.forEach { note ->
            NoteMediaStore.deleteAllForNote(this@AllNotesActivity, note)
            NoteVoiceStore.deleteAllForNote(this@AllNotesActivity, note)
            NoteAttachmentStore.deleteAllForNote(this@AllNotesActivity, note)
            notesAdapter.removeUserTitle(note)
            NotesCacheManager.cachedTitles.remove(note)
            getSharedPreferences("reminder_badges", MODE_PRIVATE).edit { remove(note.hashCode().toString()) }
            getSharedPreferences("reminder_flags", MODE_PRIVATE).edit { remove(note.hashCode().toString()) }
            removeUidFor(note)
        }

        allNotes.removeAll(notesToDelete.toSet())
        NotesCacheManager.cachedNotes.removeAll(notesToDelete.toSet())
        NoteFolderStore.removeFolders(this, selectedFolderIds)
        syncDeletedNoteFoldersToSupabase(selectedFolderIds)
        if (currentFolderId in selectedFolderIds) currentFolderId = null

        selectedKeys.clear()
        isMultiSelectMode = false
        notesAdapter.clearSelection()
        setDeleteVisibleFromActivity?.invoke(false)

        refreshDisplayedNotes()
        saveNotes()
        sharedPreferences.edit(commit = true) {
            putString("notes_list", Gson().toJson(allNotes))
        }
        NotesCacheManager.cachedNotes = allNotes.toMutableList()
        updateNotePlaceholder()
    }

    private fun openSearchView(onDismiss: () -> Unit) {
        val parentView = findViewById<ViewGroup>(android.R.id.content)
        val inflater = LayoutInflater.from(this)
        val rootLayout = inflater.inflate(
            R.layout.dialog_search_all_notes2,
            parentView,
            false
        ) as androidx.coordinatorlayout.widget.CoordinatorLayout

        // ✅ NEW: EditText + icon (from your XML)
        val etSearch = rootLayout.findViewById<EditText>(R.id.et_search)
        val searchIcon = rootLayout.findViewById<ImageView>(R.id.search_icon)
        val onSurfaceVariant = resolveThemeColor(com.google.android.material.R.attr.colorOnSurfaceVariant)
        etSearch.setHintTextColor((onSurfaceVariant))
        etSearch.highlightColor = (onSurfaceVariant and 0x00FFFFFF) or (0x33000000) // soft highlight

        // ✅ split button refs
        val btnFilter = rootLayout.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_filter)
        val btnClose  = rootLayout.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_close)

        // ✅ mode state
        var mode = loadSearchMode()
// ✅ apply persisted mode to hint immediately (so it shows on open)
        etSearch.hint = if (currentFolderId == null) {
            "Search folders or notes…"
        } else {
            when (mode) {
                SearchMode.NOTE  -> "Search in note"
                SearchMode.TITLE -> "Search title…"
                SearchMode.BOTH  -> "Search title or note…"
            }
        }

        fun applyFilterNow() {
            val text = etSearch.text?.toString().orEmpty()
            filterNotes(text, mode)
        }

        // ✅ theme-aware tint (so it looks correct light/dark)
        val onSurface = resolveThemeColor(com.google.android.material.R.attr.colorOnSurface)
        btnFilter.iconTint = ColorStateList.valueOf(onSurface)
        btnClose.iconTint  = ColorStateList.valueOf(onSurface)
        searchIcon.imageTintList = ColorStateList.valueOf(onSurface)

        btnFilter.setOnClickListener {

            val items = arrayOf("Note content", "Title only", "Title & note")

            val checkedIndex = when (mode) {
                SearchMode.NOTE  -> 0
                SearchMode.TITLE -> 1
                SearchMode.BOTH  -> 2
            }

            var pendingIndex = checkedIndex

            fun hintFor(m: SearchMode) = if (currentFolderId == null) {
                "Search folders or notes…"
            } else {
                when (m) {
                    SearchMode.NOTE  -> "Search in note"
                    SearchMode.TITLE -> "Search title…"
                    SearchMode.BOTH  -> "Search title or note…"
                }
            }

            fun modeForIndex(i: Int) = when (i) {
                1 -> SearchMode.TITLE
                2 -> SearchMode.BOTH
                else -> SearchMode.NOTE
            }

            fun dialogPanel(d: android.app.Dialog): View? {
                val content = d.findViewById<ViewGroup>(android.R.id.content) ?: return null
                return content.getChildAt(0) // ✅ visible rounded dialog panel
            }

            fun dp(v: View, value: Float): Float = value * v.resources.displayMetrics.density

            // ✅ M3 motion: fade + subtle scale + lift (NO height hack => NO blink)
            fun motionPrepare(panel: View) {
                panel.alpha = 0f
                panel.scaleX = 0.985f
                panel.scaleY = 0.985f
                panel.translationY = dp(panel, 10f)
            }

            fun motionEnter(panel: View) {
                panel.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .translationY(0f)
                    .setDuration(220L)
                    .setInterpolator(android.view.animation.PathInterpolator(0.2f, 0f, 0f, 1f))
                    .start()
            }

            fun motionExit(panel: View, end: () -> Unit) {
                panel.animate()
                    .alpha(0f)
                    .scaleX(0.985f)
                    .scaleY(0.985f)
                    .translationY(dp(panel, 10f))
                    .setDuration(160L)
                    .setInterpolator(android.view.animation.PathInterpolator(0.2f, 0f, 0f, 1f))
                    .withEndAction(end)
                    .start()
            }

            val dlg = MaterialAlertDialogBuilder(this)
                .setTitle("Search in")
                .setSingleChoiceItems(items, checkedIndex) { _, which ->
                    pendingIndex = which
                }
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Reset", null)
                .setPositiveButton("OK", null)
                .create()


            // 🚫 disable default window animation (so ONLY our motion runs)
            dlg.window?.setWindowAnimations(0)

            fun dismissWithMotion() {
                val panel = dialogPanel(dlg)
                if (panel == null) {
                    dlg.dismiss()
                    return
                }
                motionExit(panel) { dlg.dismiss() }
            }

            dlg.setOnShowListener {
                val panel = dialogPanel(dlg) ?: return@setOnShowListener

                // ✅ prepare instantly (prevents blink)
                motionPrepare(panel)

                // ✅ start motion next frame
                panel.post { motionEnter(panel) }

                dlg.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)?.setOnClickListener {
                    dismissWithMotion()
                }

                dlg.getButton(android.app.AlertDialog.BUTTON_NEUTRAL)?.setOnClickListener {
                    mode = SearchMode.NOTE
                    saveSearchMode(mode)

                    etSearch.hint = hintFor(mode)
                    applyFilterNow()
                    etSearch.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)

                    dismissWithMotion()
                }

                dlg.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
                    val newMode = modeForIndex(pendingIndex)
                    etSearch.hint = hintFor(newMode)

                    if (newMode != mode) {
                        mode = newMode
                        saveSearchMode(mode)
                        applyFilterNow()
                        etSearch.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                    }

                    dismissWithMotion()
                }
            }

            // back press -> animate out
            dlg.setOnKeyListener { _, keyCode, event ->
                if (keyCode == android.view.KeyEvent.KEYCODE_BACK &&
                    event.action == android.view.KeyEvent.ACTION_UP
                ) {
                    dismissWithMotion()
                    true
                } else false
            }

            // outside tap -> animate out
            dlg.setOnCancelListener { dismissWithMotion() }
            dlg.setCanceledOnTouchOutside(true)

            dlg.show()
        }




        // ✅ close/clear on CLOSE split button
        btnClose.setOnClickListener {
            etSearch.setText("")
            etSearch.requestFocus()
            applyFilterNow()

            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT)
        }

        // ✅ live filtering (NO SearchView X ever again)
        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilterNow()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        // ✅ BottomSheetDialog (Material 3 rounded corners)
        val searchDialog = BottomSheetDialog(
            this,
            com.google.android.material.R.style.ThemeOverlay_Material3_BottomSheetDialog
        ).apply {
            setContentView(rootLayout)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

            setOnDismissListener {
                onDismiss()
                filterNotes("", loadSearchMode())
            }
            setOnCancelListener { onDismiss() }    // optional safety
        }

        searchDialog.show()


        searchDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let { sheet ->

            val shape = com.google.android.material.shape.MaterialShapeDrawable.createWithElevationOverlay(this)

            // 🔵 pick radius you want (28dp-ish). This number is pixels, so use dp->px:
            val r = (28 * resources.displayMetrics.density)

            shape.shapeAppearanceModel = shape.shapeAppearanceModel
                .toBuilder()
                .setTopLeftCornerSize(r)
                .setTopRightCornerSize(r)
                .build()

            // Optional: make sure it uses your surface color instead of weird gray
            shape.fillColor = ColorStateList.valueOf(
                resolveThemeColor(com.google.android.material.R.attr.colorSurface)
            )

            sheet.background = shape

            BottomSheetBehavior.from(sheet).apply {
                isHideable = true
                skipCollapsed = true
            }
        }



        // ✅ focus + show keyboard
        rootLayout.post {
            etSearch.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT)
        }

        // optional cute animation (animate the whole pill, not searchView)
        ObjectAnimator.ofFloat(rootLayout.findViewById(R.id.search_pill), "translationY", 0f, 30f).apply {
            duration = 200
            interpolator = DecelerateInterpolator()
            start()
        }

        // initial render
        applyFilterNow()
    }
    private companion object {
        private const val PREF_SEARCH_MODE = "pref_search_mode"
    }

    private fun saveSearchMode(mode: SearchMode) {
        getSharedPreferences("ui_prefs", MODE_PRIVATE).edit {
            putInt(PREF_SEARCH_MODE, mode.ordinal)
        }
    }

    private fun loadSearchMode(): SearchMode {
        val ord = getSharedPreferences("ui_prefs", MODE_PRIVATE)
            .getInt(PREF_SEARCH_MODE, SearchMode.NOTE.ordinal)
        return SearchMode.entries.getOrElse(ord) { SearchMode.NOTE }
    }


    fun openMaterialDateTimePickerDialog(note: String) {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Date")
            .build()

        val calendar = Calendar.getInstance()
        val timePicker = MaterialTimePicker.Builder()
            .setHour(calendar.get(Calendar.HOUR_OF_DAY))
            .setMinute(calendar.get(Calendar.MINUTE))
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setTitleText("Select Time")
            .build()

        datePicker.addOnPositiveButtonClickListener {
            timePicker.addOnPositiveButtonClickListener {
                openCalendarApp(note)
            }
            timePicker.show(supportFragmentManager, "timePicker")
        }
        datePicker.show(supportFragmentManager, "datePicker")
    }

    fun openCalendarApp(note: String) {
        val intent = Intent(Intent.ACTION_INSERT)
            .setData(CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.Events.TITLE, note)
            .putExtra(CalendarContract.Events.EVENT_LOCATION, "Reminder")
            .putExtra(CalendarContract.Events.DESCRIPTION, "Reminder from app")
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, System.currentTimeMillis())

        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "No calendar app found", Toast.LENGTH_SHORT).show()
        }
    }

    // time-picker bottom sheet (XML dialog UI) – unchanged
    @SuppressLint("ObsoleteSdkInt")
    fun openMaterialTimePickerDialog(note: String) {
        val bottomSheetDialog = BottomSheetDialog(this)
        val parentView = findViewById<ViewGroup>(android.R.id.content)
        val bottomSheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_time_picker, parentView, false)

        bottomSheetDialog.setContentView(bottomSheetView)
        bottomSheetDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val timePickerFrame = bottomSheetView.findViewById<View>(R.id.timePickerFrame)
        val timePicker = bottomSheetView.findViewById<TimePicker>(R.id.custom_time_picker)
        val fabCancel = bottomSheetView.findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fab_cancel)
        val fabSetReminder = bottomSheetView.findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fab_set_reminder)
        val timeDisplay = bottomSheetView.findViewById<TextView>(R.id.timeDisplay)
        val successMessage = bottomSheetView.findViewById<TextView>(R.id.tv_time_success)
        val errorMessage = bottomSheetView.findViewById<TextView>(R.id.tv_time_error)

        val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let { BottomSheetBehavior.from(it).isDraggable = false }

        timePicker.setIs24HourView(false)

        val soundPool = android.media.SoundPool.Builder().setMaxStreams(4).build()
        val volumeLevel = 0.4f
        val clickSound = soundPool.load(this, R.raw.time_click, 1)
        val successSound = soundPool.load(this, R.raw.success, 1)
        val errorSound = soundPool.load(this, R.raw.error, 1)

        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        val originalDrawable = timePickerFrame.background

        fun startRealTimeClockUpdate() {
            handler.post(object : Runnable {
                override fun run() {
                    val c = Calendar.getInstance()
                    val formatted = String.format(Locale.US, "%02d:%02d:%02d",
                        c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.SECOND))
                    timeDisplay.text = formatted
                    handler.postDelayed(this, 1000)
                }
            })
        }

        fun animateEffect(view: View, isSuccess: Boolean) {
            val successDrawable = ContextCompat.getDrawable(this, R.drawable.flash_success)
            val errorDrawable = ContextCompat.getDrawable(this, R.drawable.flash_error)

            if (isSuccess) {
                ObjectAnimator.ofFloat(view, "alpha", 0f, 1f).apply {
                    duration = 700
                    addUpdateListener { view.background = successDrawable }
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) { view.background = successDrawable }
                    })
                }.start()
            } else {
                ObjectAnimator.ofFloat(view, "alpha", 1f, 0.5f, 1f).apply {
                    duration = 500
                    repeatCount = 3
                    repeatMode = ObjectAnimator.REVERSE
                    addUpdateListener { view.background = errorDrawable }
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) { view.background = originalDrawable }
                    })
                }.start()
            }
        }

        timePicker.setOnTimeChangedListener { _, _, _ ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION") vibrator.vibrate(70)
            }
            soundPool.play(clickSound, volumeLevel, volumeLevel, 0, 0, 1f)
        }

        updateSecondsRunnable = object : Runnable {
            override fun run() {
                startRealTimeClockUpdate()
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(updateSecondsRunnable!!)

        fabCancel.setOnClickListener {
            handler.removeCallbacks(updateSecondsRunnable!!)
            bottomSheetDialog.dismiss()
        }

        fabSetReminder.setOnClickListener {
            handler.removeCallbacks(updateSecondsRunnable!!)
            val hour = timePicker.hour
            val minute = timePicker.minute

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
            }
            val delay = calendar.timeInMillis - System.currentTimeMillis()
            if (delay <= 0) {
                errorMessage.visibility = View.VISIBLE
                successMessage.visibility = View.GONE
                soundPool.play(errorSound, volumeLevel, volumeLevel, 0, 0, 1f)
                animateEffect(timePickerFrame, false)
                return@setOnClickListener
            }

            errorMessage.visibility = View.GONE
            successMessage.visibility = View.VISIBLE
            soundPool.play(successSound, volumeLevel, volumeLevel, 0, 0, 1f)
            animateEffect(timePickerFrame, true)
            bottomSheetView.postDelayed({ bottomSheetDialog.dismiss() }, 2500)

            scheduleReminderUsingWorkManager(note, hour, minute, bottomSheetView, bottomSheetDialog)
        }

        bottomSheetDialog.setOnDismissListener {
            handler.removeCallbacks(updateSecondsRunnable!!)
            soundPool.release()
        }
        bottomSheetDialog.show()
    }

    private fun scheduleReminderUsingWorkManager(
        note: String,
        hourOfDay: Int,
        minute: Int,
        bottomSheetView: View,
        bottomSheetDialog: BottomSheetDialog,
    ) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }
        val delay = calendar.timeInMillis - System.currentTimeMillis()

        // UI feedback
        val timeErrorText   = bottomSheetView.findViewById<TextView>(R.id.tv_time_error)
        val timeSuccessText = bottomSheetView.findViewById<TextView>(R.id.tv_time_success)

        if (delay <= 0) {
            timeErrorText.visibility = View.VISIBLE
            timeSuccessText.visibility = View.GONE
            timeErrorText.text = bottomSheetView.context.getString(R.string.time_in_past)
            return
        }
        runOnUiThread {
            refreshDisplayedNotes()
        }


        timeErrorText.visibility = View.GONE
        timeSuccessText.visibility = View.VISIBLE
        timeSuccessText.text = bottomSheetView.context.getString(R.string.reminder_set_successful)

        bottomSheetView.postDelayed({ bottomSheetDialog.dismiss() }, 3000)

        // Store note (WorkManager input data kept small)
        val prefs = getSharedPreferences("reminder_notes", MODE_PRIVATE)
        val noteKey = "note_${System.currentTimeMillis()}"
        prefs.edit { putString(noteKey, note) }

        val inputData = workDataOf("note_key" to noteKey)

        val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(this).enqueue(workRequest)

        // badge + bell flags (force write)
        getSharedPreferences("reminder_badges", MODE_PRIVATE).edit(commit = true) {
            putBoolean(note.hashCode().toString(), true)
        }
        getSharedPreferences("reminder_flags", MODE_PRIVATE).edit(commit = true) {
            putBoolean(note.hashCode().toString(), true)
        }

        refreshRowFor(note)

        notesAdapter.preloadBadgeStates(this)
        notesAdapter.preloadReminderFlags(this)
        notesAdapter.notifyByContent(note)
        syncExistingNoteExtrasToSupabase(note)


    }
    private fun refreshRowFor(note: String) {
        val idx = noteRows.indexOfFirst { it.text == note }
        if (idx == -1) return

        val bellOn = getSharedPreferences("reminder_flags", MODE_PRIVATE)
            .getBoolean(note.hashCode().toString(), false)

        val badgeOn = getSharedPreferences("reminder_badges", MODE_PRIVATE)
            .getBoolean(note.hashCode().toString(), false)

        noteRows[idx] = noteRows[idx].copy(
            hasReminder = bellOn,
            hasBadge = badgeOn
        )
    }



    private fun findNumberPickers(viewGroup: ViewGroup): List<NumberPicker> {
        val picks = mutableListOf<NumberPicker>()
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            if (child is NumberPicker) picks.add(child)
            else if (child is ViewGroup) picks.addAll(findNumberPickers(child))
        }
        return picks
    }

    // ----------------------- Navigation helpers -----------------------

    // NEW: MainActivity::class.java (Compose home / dashboard)
    private fun goToHomeScreen() {
        startActivity(Intent(this, MainActivity::class.java))
        overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation)
        finish()
    }


    private fun goToContactScreen() {
        startActivity(Intent(this, SettingsActivity::class.java))
        overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation)
        finish()
    }

    private fun goToAllContactsScreen() {
        val intent = Intent(this, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_START_PAGE, MainActivity.PAGE_BRIEFING)
        startActivity(intent)
        overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation)
        finish()
    }

    // ----------------------- Misc -----------------------

    private enum class SearchMode { NOTE, TITLE, BOTH }

    // ✅ mode-aware + uses the active folder list
    private fun filterNotes(query: String, mode: SearchMode) {
        val q = query.trim()
        if (currentFolderId == null) {
            refreshFolderRows(q)
            rebuildBoth(emptyList())
            notesAdapter.submit(emptyList())
            notesAdapter.preloadReminderFlags(this)
            notesAdapter.preloadBadgeStates(this)
            notesCount = allNotes.size
            showFabBadge(null)
            return
        }

        val base = displayNotesForCurrentFolder()

        val filtered = if (q.isEmpty()) {
            base
        } else {
            base.filter { note ->
                val title = resolveTitle(note).orEmpty() // IMPORTANT: your resolveTitle strips HTML
                when (mode) {
                    SearchMode.NOTE  -> note.contains(q, ignoreCase = true)
                    SearchMode.TITLE -> title.contains(q, ignoreCase = true)
                    SearchMode.BOTH  -> note.contains(q, true) || title.contains(q, true)
                }
            }
        }

        val settings = readNotesPageSettings()
        val result = applyNotesSort(filtered, settings.sortMode)

        // ✅ update COMPOSE list (what you actually see)
        rebuildBoth(result)

        // ✅ keep adapter in sync too
        notesAdapter.submit(result)

        // ✅ keep bell/badge maps correct for the new displayed list
        notesAdapter.preloadReminderFlags(this)
        notesAdapter.preloadBadgeStates(this)

        notesCount = allNotes.size
        showFabBadge(null)
    }

    private fun applyNotesSort(list: List<String>, sortMode: String): List<String> {
        val base = list.toList() // always use a safe copy

        return when (sortMode) {

            // ✅ Oldest first = keep as-is (assuming saved order is oldest->newest)
            NotesPagePrefs.SORT_OLDEST -> base

            // ✅ Newest first = reversed
            NotesPagePrefs.SORT_NEWEST -> base.asReversed()

            NotesPagePrefs.SORT_TITLE -> base.sortedWith(
                compareBy(String.CASE_INSENSITIVE_ORDER) { note ->
                    resolveTitle(note).orEmpty()
                }
            )

            NotesPagePrefs.SORT_REMINDERS_FIRST -> {
                val bellPrefs = getSharedPreferences("reminder_flags", MODE_PRIVATE)
                base.sortedWith(
                    compareByDescending<String> { note ->
                        bellPrefs.getBoolean(note.hashCode().toString(), false)
                    }.thenBy(String.CASE_INSENSITIVE_ORDER) { resolveTitle(it).orEmpty() }
                )
            }

            else -> base
        }
    }




    private fun queueEditForSync(oldNote: String, newNote: String) {
        NotesCacheManager.cachedTitles.remove(oldNote)

        val badgePrefs = getSharedPreferences("reminder_badges", MODE_PRIVATE)
        val oldKey = oldNote.hashCode().toString()
        val newKey = newNote.hashCode().toString()
        val hadBadge = badgePrefs.getBoolean(oldKey, false)
        badgePrefs.edit {
            remove(oldKey)
            if (hadBadge) putBoolean(newKey, true)
        }
        notesAdapter.preloadBadgeStates(this)

        val bellPrefs = getSharedPreferences("reminder_flags", MODE_PRIVATE)
        val hadBell = bellPrefs.getBoolean(oldKey, false)
        bellPrefs.edit {
            remove(oldKey)
            if (hadBell) putBoolean(newKey, true)
        }
        notesAdapter.preloadReminderFlags(this)

        if (notesOnlineSyncEnabled()) {
            val sp = getSharedPreferences("notes_prefs", MODE_PRIVATE)
            val pendingAdds = sp.getStringSet("pending_adds", emptySet())!!.toMutableSet()
            pendingAdds.add(newNote)
            sp.edit { putStringSet("pending_adds", pendingAdds) }

            SupabaseManager.client.auth.currentSessionOrNull()?.user?.id?.let { userId ->
                val pendingDeletes = pendingDeletesForUser(userId)
                pendingDeletes.add(oldNote)
                savePendingDeletesForUser(userId, pendingDeletes)
            }
        }

        if (SupabaseManager.client.auth.currentSessionOrNull()?.user?.id != null && notesOnlineSyncEnabled()) {
            lifecycleScope.launch {
                deleteNoteFromSupabase(oldNote)
                syncNoteToSupabase(newNote)
            }
        }

        contentToUid[newNote] = contentToUid.remove(oldNote) ?: ensureLocalUid(newNote)
        uidToContent.entries.firstOrNull { it.value == oldNote }?.let { entry ->
            uidToContent[entry.key] = newNote
        }
        saveUidMaps()
    }

    private val badgeChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            val badgeKey = intent.getStringExtra(ReminderDismissReceiver.EXTRA_BADGE_KEY) ?: return
            val changedIndex = notesText.indexOfFirst { it.hashCode().toString() == badgeKey } // ✅ was notes
            if (changedIndex != -1) {
                notesAdapter.preloadBadgeStates(ctx)
                notesAdapter.notifyItemChanged(changedIndex)
            }
        }
    }

}

