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
import androidx.appcompat.app.AppCompatActivity
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
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

@Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
class AllNotesActivity : AppCompatActivity() {

    private lateinit var notesAdapter: NotesAdapter
    private val allNotes = mutableListOf<String>()
    private val notesText = mutableStateListOf<String>()          // adapter + legacy logic
    private val noteRows  = mutableStateListOf<NoteRow>()         // compose UI list with ids
    private val sharedPreferences by lazy { getSharedPreferences("notes_prefs", MODE_PRIVATE) }
    private var isMultiSelectMode = false
    private val handler = Handler(Looper.getMainLooper())
    private var updateSecondsRunnable: Runnable? = null

    // FAB badge (legacy view path – now no-op in Compose mode)
    private var hasInitializedBadge = false
    private var notesCount by mutableIntStateOf(0)


    private val metaPrefs by lazy { getSharedPreferences("notes_meta", MODE_PRIVATE) }
    private fun now() = System.currentTimeMillis()
    private val selectedKeys = mutableSetOf<String>()   // selection by UID, not by content

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
                    title = title,
                    hasReminder = bellOn,
                    hasBadge = badgeOn
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
                                val intent = AddNoteComposeActivity.newIntent(this@AllNotesActivity)
                                val opts = ActivityOptionsCompat.makeSceneTransitionAnimation(this@AllNotesActivity)
                                addNoteLauncher.launch(intent, opts)
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
                                selectedKeys.addAll(selectedRowKeys.map { it.substringBefore('#') }) // ✅ uid only
                                isMultiSelectMode = selectedKeys.isNotEmpty()
                                deleteSelectedNotes()
                            }



                        ) { finish() }


                    }
                }
            }
        }

        // Back press
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
                overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation)
            }
        })

        NotesCacheManager.preloadResources(this)
        loadIdMaps()
        loadUidMaps()

        // 🔄 SYNC FLOW (no XML refs)
        if (UserPreferencesManager(this).isLoggedIn) {
            // inside onCreate(), where you start sync
            lifecycleScope.launch {
                if (shouldSync()) {
                    // 1) push deletes first
                    syncAllDeletesToSupabase()

                    // 2) then push adds
                    syncPendingAddsOnly()

                    // 3) now pull, reconcile, render
                    pullFromSupabaseAndReconcile()

                    metaPrefs.edit { putLong("last_sync_at", now()) }
                }
            }

        } else {
            loadNotesHeadless()
        }

    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onStart() {
        super.onStart()

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

        val sorted = applyNotesSort(allNotes, settings.sortMode)

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

        // ✅ 3) submit list
        notesAdapter.submit(sorted)

        // ✅ 4) now rebuild Compose models last (so UI reads fresh adapter state)
        rebuildBoth(sorted)

        notesCount = sorted.size
        updateNotePlaceholder()
        showFabBadge(null)
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

            if (!newNoteContent.isNullOrBlank()) {
                addLocalNote(newNoteContent)
                if (imageUris.isNotEmpty()) {
                    NoteMediaStore.setUris(this, newNoteContent, imageUris)
                }
                if (userTitle.isNotBlank()) {
                    notesAdapter.setUserTitle(newNoteContent, userTitle)
                }
                syncNoteToSupabase(newNoteContent)

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
        contentToUid.remove(note)?.let { uidToContent.remove(it) }
        saveUidMaps()
    }



    private fun syncAllDeletesToSupabase() {
        val prefs = getSharedPreferences("notes_prefs", MODE_PRIVATE)
        val pendingDeletes = prefs.getStringSet("pending_deletes", emptySet())!!.toMutableSet()
        if (pendingDeletes.isEmpty()) return

        val upm = UserPreferencesManager(this)
        if (!upm.isLoggedIn) return

        val session = SupabaseManager.client.auth.currentSessionOrNull() ?: return
        val userId = session.user?.id ?: return

        lifecycleScope.launch(Dispatchers.IO) {
            val stillPending = mutableSetOf<String>()
            pendingDeletes.forEach { content ->
                try {
                    SupabaseManager.client.postgrest.from("user_notes").delete {
                        filter { eq("user_id", userId); eq("content", content) }
                    }
                    // optional: scrub id maps when delete succeeds
                    contentToId.remove(content)?.let { idToContent.remove(it) }
                    saveIdMaps()
                } catch (e: Exception) {
                    Log.e("SupabaseDelete", "Failed to delete note: $content", e)
                    stillPending.add(content)
                }
            }
            prefs.edit {
                if (stillPending.isEmpty()) remove("pending_deletes")
                else putStringSet("pending_deletes", stillPending)
            }
        }
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

            // ✅ 2.5) Title store FIRST (so rebuild reads fresh title)
            if (updatedTitle.isNotBlank()) notesAdapter.setUserTitle(updatedNote, updatedTitle)
            else notesAdapter.removeUserTitle(updatedNote)


            val settings = readNotesPageSettings()
            val sorted = applyNotesSort(allNotes.toList(), settings.sortMode)

            notesText.clear()
            notesText.addAll(sorted)
            notesAdapter.submit(sorted)
            rebuildNoteRowsFromDisplay(sorted)

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

    private fun shouldSync(): Boolean {
        val last = metaPrefs.getLong("last_sync_at", 0L)
        return now() - last > 5_000 // tweak as needed
    }

    private fun addLocalNote(content: String) {

        // 1) add to RAW list only
        allNotes.add(content)

        // 2) build DISPLAY list (sorted copy)
        val settings = readNotesPageSettings()
        val sorted = applyNotesSort(allNotes.toList(), settings.sortMode)

        // ✅ adapter/legacy list
        notesText.clear()
        notesText.addAll(sorted)
        notesAdapter.submit(sorted)

        // ✅ compose list (ids)
        rebuildNoteRowsFromDisplay(sorted)

        // 3) persist RAW
        notesCount = allNotes.size
        saveNotes()
        updateNotePlaceholder()
        showFabBadge(null)

        // 4) pending sync
        val sp = getSharedPreferences("notes_prefs", MODE_PRIVATE)
        val pending = sp.getStringSet("pending_adds", emptySet())!!.toMutableSet()
        pending.add(content)
        sp.edit { putStringSet("pending_adds", pending) }
    }


    private fun saveNotes() {
        val notesJson = Gson().toJson(allNotes) // ✅ raw list only
        sharedPreferences.edit { putString("notes_list", notesJson) }

        // cache should also be raw, not sorted
        NotesCacheManager.cachedNotes = allNotes.toMutableList()
        notesCount = allNotes.size
        showFabBadge(null)
    }


    // Make sure your upload loop NEVER uploads something in pending_deletes
    private suspend fun syncPendingAddsOnly() {
        val sp = getSharedPreferences("notes_prefs", MODE_PRIVATE)
        val pendingAdds = sp.getStringSet("pending_adds", emptySet())!!.toMutableSet()
        if (pendingAdds.isEmpty()) return

        val pendingDeletes = sp.getStringSet("pending_deletes", emptySet())!!.toMutableSet()

        val remoteContents: Set<String> = fetchCurrentRemoteContents() // your existing call

        val stillPending = mutableSetOf<String>()
        for (content in pendingAdds) {
            if (content in pendingDeletes) continue        // ← NEW guard
            if (content in remoteContents) continue
            try {
                insertNoteToSupabase(content)              // your existing insert
            } catch (_: Exception) {
                stillPending.add(content)
            }
        }
        sp.edit { putStringSet("pending_adds", stillPending) }
    }
    // Fetch current remote contents for the logged-in user
    private suspend fun fetchCurrentRemoteContents(): Set<String> {
        val session = SupabaseManager.client.auth.currentSessionOrNull() ?: return emptySet()
        val userId  = session.user?.id ?: return emptySet()

        return withContext(Dispatchers.IO) {
            try {
                val rows: List<UserNote> = SupabaseManager.client
                    .postgrest
                    .from("user_notes")
                    .select { filter { eq("user_id", userId) } }
                    .decodeList()
                rows.map { it.content }.toSet()
            } catch (e: Exception) {
                Log.e("SupabaseSync", "fetchCurrentRemoteContents failed", e)
                emptySet()
            }
        }
    }

    // Insert one note (suspend + updates local maps and pending_adds on success)
    private suspend fun insertNoteToSupabase(content: String) {
        val session = SupabaseManager.client.auth.currentSessionOrNull() ?: return
        val userId  = session.user?.id ?: return

        val inserted: List<UserNote> = withContext(Dispatchers.IO) {
            SupabaseManager.client
                .postgrest
                .from("user_notes")
                .insert(listOf(UserNote(userId = userId, content = content))) { select() }
                .decodeList()
        }

        // remove from pending_adds if it was queued
        removePendingAdd(content)

        // update id maps if Supabase returned an id
        inserted.firstOrNull()?.let { row ->
            row.id?.let { id ->
                idToContent[id] = row.content
                contentToId[row.content] = id
                saveIdMaps()
            }
        }
        Log.d("SupabaseSync", "Note inserted: ${inserted.firstOrNull()?.id}")
    }


    private fun syncNoteToSupabase(content: String) {
        val prefs = UserPreferencesManager(this)
        if (!prefs.isLoggedIn) return

        val session = SupabaseManager.client.auth.currentSessionOrNull() ?: return
        val userId  = session.user?.id ?: return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val inserted: List<UserNote> = SupabaseManager.client
                    .postgrest
                    .from("user_notes")
                    .insert(listOf(UserNote(userId = userId, content = content))){ select() }
                    .decodeList()

                removePendingAdd(content)

                inserted.firstOrNull()?.let { row ->
                    if (!row.id.isNullOrBlank()) {
                        idToContent[row.id] = row.content
                        contentToId[row.content] = row.id
                        saveIdMaps()
                    }
                }
                withContext(Dispatchers.Main) {
                    Log.d("SupabaseSync", "Note synced: ${inserted.firstOrNull()?.id}")
                }
            } catch (e: Exception) {
                Log.e("SupabaseSync", "Error syncing note", e)
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

    private suspend fun pullFromSupabaseAndReconcile() {
        val session = SupabaseManager.client.auth.currentSessionOrNull() ?: return
        val userId  = session.user?.id ?: return

        // 1) fetch remote snapshot
        val remoteRows: List<UserNote> = SupabaseManager.client
            .postgrest
            .from("user_notes")
            .select { filter { eq("user_id", userId) } }
            .decodeList()

        val remoteContents = remoteRows.map { it.content }.toSet()

        // 2) load pending sets
        val sp = getSharedPreferences("notes_prefs", MODE_PRIVATE)
        val pendingAdds    = sp.getStringSet("pending_adds", emptySet())!!.toSet()
        val pendingDeletes = sp.getStringSet("pending_deletes", emptySet())!!.toSet()

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
        val remoteIdToContent = remoteRows.mapNotNull { r -> r.id?.let { it to r.content } }.toMap()
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

            // deletes
            if (toDeleteLocally.isNotEmpty()) {
                toDeleteLocally.forEach { c ->
                    notesAdapter.removeUserTitle(c)
                    NotesCacheManager.cachedTitles.remove(c)
                    getSharedPreferences("reminder_badges", MODE_PRIVATE).edit { remove(c.hashCode().toString()) }
                    getSharedPreferences("reminder_flags", MODE_PRIVATE).edit { remove(c.hashCode().toString()) }
                    NoteMediaStore.deleteAllForNote(this@AllNotesActivity, c)
                    removeUidFor(c)
                }
                allNotes.removeAll(toDeleteLocally.toSet())
            }

            // ✅ now rebuild DISPLAY list from allNotes
            val settings = readNotesPageSettings()
            val sorted = applyNotesSort(allNotes, settings.sortMode)

            notesText.clear()
            notesText.addAll(sorted)
            notesAdapter.submit(sorted)

// ✅ compose list (ids)
            rebuildNoteRowsFromDisplay(sorted)

            // ✅ persist raw
            saveNotes()
            saveIdMaps()

            notesCount = sorted.size
            showFabBadge(null)
        }

        metaPrefs.edit { putLong("last_server_updated_at", System.currentTimeMillis()) }
    }

    // ----------------------- UI helpers (Compose mode) -----------------------

    private fun resolveTitle(note: String): String? {
        return notesAdapter.getUserTitle(note)
            ?: NotesCacheManager.cachedTitles[note]?.let {
                HtmlCompat.fromHtml(it, HtmlCompat.FROM_HTML_OPTION_USE_CSS_COLORS).toString()
            }
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

                val updatedImageStrings = data?.getStringArrayListExtra("UPDATED_IMAGES") ?: arrayListOf()
                val updatedImageUris = updatedImageStrings.mapNotNull { runCatching { it.toUri() }.getOrNull() }

                if (!updatedNote.isNullOrEmpty() && position in notesText.indices) {

                    // old note from CURRENT VISIBLE list
                    val oldNote = notesText[position]

                    // 1) migrate title + media key if content changed
                    if (oldNote != updatedNote) {
                        notesAdapter.migrateUserTitle(oldNote, updatedNote)
                        NoteMediaStore.migrateNoteKey(this, oldNote, updatedNote)

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

                    // 4) rebuild DISPLAY from RAW (sort applied)
                    val settings = readNotesPageSettings()
                    val sorted = applyNotesSort(allNotes.toList(), settings.sortMode)

                    notesText.clear()
                    notesText.addAll(sorted)
                    notesAdapter.submit(sorted)

                    // 5) rebuild compose rows (ids)
                    rebuildNoteRowsFromDisplay(sorted)

                    // 6) apply title store
                    if (!updatedTitle.isNullOrBlank()) notesAdapter.setUserTitle(updatedNote, updatedTitle)
                    else notesAdapter.removeUserTitle(updatedNote)

                    // 7) persist + sync
                    saveNotes()
                    if (oldNote != updatedNote) queueEditForSync(oldNote, updatedNote)

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
    override fun onResume() { super.onResume(); showFabBadge(null) }
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
        try {
            val userId = SupabaseManager.client.auth.currentUserOrNull()?.id ?: return

            // If you have an id for this content, delete by id first
            val id = contentToId[noteContent]
            if (id != null) {
                SupabaseManager.client.postgrest.from("user_notes").delete {
                    filter { eq("user_id", userId); eq("id", id) }
                }
                contentToId.remove(noteContent)?.let { idToContent.remove(it) }
                saveIdMaps()
            } else {
                // fallback by content
                SupabaseManager.client.postgrest.from("user_notes").delete {
                    filter { eq("user_id", userId); eq("content", noteContent) }
                }
            }
            Log.d("Supabase", "✅ Deleted from Supabase: $noteContent")
        } catch (e: Exception) {
            Log.e("Supabase", "❌ Failed to delete from Supabase", e)
        }
    }

    private fun deleteSelectedNotes() {
        val deleted: List<Triple<String, Int, String?>> =
            selectedKeys.mapNotNull { key ->
                val note = uidToContent[key] ?: return@mapNotNull null
                val idx = notesText.indexOf(note)
                if (idx != -1) Triple(note, idx, notesAdapter.getUserTitle(note)) else null
            }.sortedByDescending { it.second }

        // ✅ if nothing resolved, do nothing (prevents "deleted" toast with no real delete)
        if (deleted.isEmpty()) {
            selectedKeys.clear()
            isMultiSelectMode = false
            notesAdapter.clearSelection()
            setDeleteVisibleFromActivity?.invoke(false)
            return
        }

        val imagesByNote: Map<String, List<android.net.Uri>> =
            deleted.associate { (note, _, _) -> note to NoteMediaStore.getUris(this, note) }

        val notesToDelete: List<String> = deleted.map { it.first }

// ✅ count how many copies will be deleted (because removeAll deletes ALL duplicates by content)
        val distinctSelected = notesToDelete.distinct()
        val totalCopiesDeleted = distinctSelected.sumOf { text ->
            allNotes.count { it == text }
        }

// ✅ build correct message (single vs multiple / duplicates vs normal)
        val deleteMsg = when (distinctSelected.size) {
            1 if totalCopiesDeleted > 1 -> "Deleted all duplicates of this note"
            1 -> "Note deleted"
            else -> "Deleted selected notes"
        }

// ✅ delete from RAW truth (this deletes ALL duplicates by content)
        allNotes.removeAll(distinctSelected.toSet())
        NotesCacheManager.cachedNotes.removeAll(distinctSelected.toSet())


        // 1) queue for server-side delete
        val sp = getSharedPreferences("notes_prefs", MODE_PRIVATE)
        val pendingDeletes = sp.getStringSet("pending_deletes", emptySet())!!.toMutableSet()
        distinctSelected.forEach { pendingDeletes.add(it) }
        sp.edit { putStringSet("pending_deletes", pendingDeletes) }

        // also yank them out of pending_adds so they can't be uploaded later
        run {
            val pendingAdds = sp.getStringSet("pending_adds", emptySet())!!.toMutableSet()
            var changed = false
            notesToDelete.forEach { if (pendingAdds.remove(it)) changed = true }
            if (changed) sp.edit { putStringSet("pending_adds", pendingAdds) }
        }

        // 2) fire Supabase deletes (no-op remotely if row not found)
        lifecycleScope.launch {
            notesToDelete.forEach { deleteNoteFromSupabase(it) }
        }

        // 3) local cleanup (titles, flags, etc.)
        deleted.forEach { (note, _, _) ->
            notesAdapter.removeUserTitle(note)
            NotesCacheManager.cachedTitles.remove(note)
            getSharedPreferences("reminder_badges", MODE_PRIVATE).edit { remove(note.hashCode().toString()) }
            getSharedPreferences("reminder_flags", MODE_PRIVATE).edit { remove(note.hashCode().toString()) }
        }

        // ✅ IMPORTANT: rebuild DISPLAY from RAW (no removeAt(index))
        run {
            val settings = readNotesPageSettings()
            val sorted = applyNotesSort(allNotes.toList(), settings.sortMode)
            rebuildBoth(sorted)
            notesAdapter.submit(sorted)
            if (sorted.isEmpty()) removeFabBadge(null)
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
                    if (!allNotes.contains(note)) allNotes.add(note)
                }
                NotesCacheManager.cachedNotes = allNotes.toMutableList()

                // restore titles + images
                deleted.forEach { (note, _, savedTitle) ->
                    if (!savedTitle.isNullOrBlank()) notesAdapter.setUserTitle(note, savedTitle)
                    imagesByNote[note]?.let { uris -> NoteMediaStore.setUris(this, note, uris) }
                }

                // ✅ rebuild DISPLAY from RAW again
                val settingsU = readNotesPageSettings()
                val sortedU = applyNotesSort(allNotes.toList(), settingsU.sortMode)
                rebuildBoth(sortedU)
                notesAdapter.submit(sortedU)

                saveNotes()

                sharedPreferences.edit(commit = true) {
                    putString("notes_list", Gson().toJson(allNotes))
                }
                NotesCacheManager.cachedNotes = allNotes.toMutableList()

                updateNotePlaceholder()
                showFabBadge(null)


                val sp2 = getSharedPreferences("notes_prefs", MODE_PRIVATE)
                val pDel2 = sp2.getStringSet("pending_deletes", emptySet())!!.toMutableSet()
                notesToDelete.forEach { pDel2.remove(it) }

                val pAdd2 = sp2.getStringSet("pending_adds", emptySet())!!.toMutableSet()
                notesToDelete.forEach { pAdd2.add(it) }

                sp2.edit {
                    putStringSet("pending_deletes", pDel2)
                    putStringSet("pending_adds", pAdd2)
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
                    NoteMediaStore.deleteAllForNote(this@AllNotesActivity, note)
                    removeUidFor(note)
                }

                // safety persist
                allNotes.removeAll(notesToDelete.toSet())
                NotesCacheManager.cachedNotes.removeAll(notesToDelete.toSet())

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

        // ✅ build DISPLAY list using your sort setting
        val settings = readNotesPageSettings()
        val sorted = applyNotesSort(allNotes.toList(), settings.sortMode)

        notesText.clear()
        notesText.addAll(sorted)

        // ✅ rebuild NoteRow models (this will read badge/bell prefs now)
        rebuildNoteRowsFromDisplay(sorted)

        updateNotePlaceholder()
        showFabBadge(null)
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
        etSearch.hint = when (mode) {
            SearchMode.NOTE  -> "Search in note"
            SearchMode.TITLE -> "Search title…"
            SearchMode.BOTH  -> "Search title or note…"
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

            fun hintFor(m: SearchMode) = when (m) {
                SearchMode.NOTE  -> "Search in note"
                SearchMode.TITLE -> "Search title…"
                SearchMode.BOTH  -> "Search title or note…"
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
        ObjectAnimator.ofFloat(rootLayout.findViewById<View>(R.id.search_pill), "translationY", 0f, 30f).apply {
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
            val settings = readNotesPageSettings()
            val sorted = applyNotesSort(allNotes.toList(), settings.sortMode)
            rebuildBoth(sorted)
            notesAdapter.submit(sorted)
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
        startActivity(Intent(this, Contact::class.java))
        overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation)
        finish()
    }

    private fun goToAllContactsScreen() {
        val intent = Intent(this, AllContactsActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation)
        finish()
    }

    // ----------------------- Misc -----------------------

    private enum class SearchMode { NOTE, TITLE, BOTH }

    // ✅ mode-aware + always uses RAW list
    private fun filterNotes(query: String, mode: SearchMode) {
        val q = query.trim()
        val base = allNotes.toList() // RAW truth

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

        notesCount = result.size
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

        val sp = getSharedPreferences("notes_prefs", MODE_PRIVATE)
        val pendingAdds = sp.getStringSet("pending_adds", emptySet())!!.toMutableSet()
        val pendingDeletes = sp.getStringSet("pending_deletes", emptySet())!!.toMutableSet()
        pendingAdds.add(newNote); pendingDeletes.add(oldNote)
        sp.edit {
            putStringSet("pending_adds", pendingAdds)
            putStringSet("pending_deletes", pendingDeletes)
        }

        if (UserPreferencesManager(this).isLoggedIn) {
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

