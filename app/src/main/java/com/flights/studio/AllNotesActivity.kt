package com.flights.studio

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Rect
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
import android.view.WindowInsetsController
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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.flights.studio.com.flights.studio.ui.ReminderDismissReceiver
import com.google.android.material.badge.ExperimentalBadgeUtils
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
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
    private val notes = mutableListOf<String>()
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

    // Compose → Activity bridge for opening the sheet with provider captured
    private lateinit var openReminderSheet: (String) -> Unit
    // === local UID map storage (content <-> localUid) for unsynced notes ===
    private val uidToContent = mutableMapOf<String, String>()
    private val contentToUid = mutableMapOf<String, String>()
    private val uidPrefs by lazy { getSharedPreferences("notes_uids", MODE_PRIVATE) }

    // ----------------------- Lifecycle -----------------------

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Create adapter BEFORE Compose (Compose uses it)
        notesAdapter = NotesAdapter(
            notes,
            applicationContext,
            ::onNoteLongClick,
            ::onNoteClick,
            { note, position ->
                val title = resolveTitle(note)
                val intent = EditNoteActivity.newIntent(this, note, position, title)
                editNoteLauncher.launch(intent)
                overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation)
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
            MaterialTheme {
                var isDeleteVisible by remember { mutableStateOf(false) }
                var reminderNote by remember { mutableStateOf<String?>(null) }

                // one place for the sheet to open
                LaunchedEffect(Unit) {
                    openReminderSheet = { note -> reminderNote = note }
                    setDeleteVisibleFromActivity = { active -> isDeleteVisible = active }
                }

                // ✅ Wrap everything in ReminderHost – it owns the LayerBackdrop
                ReminderHost(
                    visible = reminderNote != null,
                    onDismiss = { reminderNote = null },
                    onTimer = {
                        reminderNote?.let { openMaterialTimePickerDialog(it) }
                        reminderNote = null
                    },
                    onCalendar = {
                        reminderNote?.let { openMaterialDateTimePickerDialog(it) }
                        reminderNote = null
                    }
                ) {
                    // 🔵 Your page content now lives inside the provider
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(colorResource(R.color.box_qrcode))
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                    ) {
                        AllNotesScreen(
                            notesAdapter = notesAdapter,
                            notes = notes,
                            notesSize = notesCount,
                            onAddNote = {
                                val intent = AddNoteActivity.newIntent(this@AllNotesActivity)
                                addNoteLauncher.launch(intent)
                                overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation)
                            },
                            onNavItemClick = { id ->
                                when (id) {
                                    R.id.nav_home -> goToHomeScreen()
                                    R.id.nav_contacts -> goToContactScreen()
                                    R.id.nav_all_contacts -> goToAllContactsScreen()
                                    R.id.nav_settings -> goToSettingsScreen()
                                    R.id.openAddNoteScreen -> Unit
                                    R.id.action_search -> openSearchView()
                                    R.id.action_delete -> if (isMultiSelectMode) deleteSelectedNotes()
                                }
                            },
                            isDeleteVisible = isDeleteVisible
                        )
                    }
                }
            }
        }

        // Back press
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
                overridePendingTransition(R.anim.enter_animation, de.dlyt.yanndroid.samsung.R.anim.abc_popup_exit)
            }
        })

        NotesCacheManager.preloadResources(this)
        adjustStatusBarIcons()
        loadIdMaps()
        loadUidMaps()

        // 🔄 SYNC FLOW (no XML refs)
        if (UserPreferencesManager(this).isLoggedIn) {
            lifecycleScope.launch {
                if (shouldSync()) {
                    // 1) Get the authoritative snapshot first
                    pullFromSupabaseAndReconcile()

                    // 2) Push local deletes that are still pending
                    syncAllDeletesToSupabase()

                    // 3) Push local adds (ONLY those recorded as pending_adds)
                    syncPendingAddsOnly()

                    metaPrefs.edit { putLong("last_sync_at", now()) }
                }
            }
        } else {
            loadNotesHeadless()
        }

    }

    override fun onStart() {
        super.onStart()

        // 1) badge receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(
            badgeChangedReceiver,
            IntentFilter(ReminderDismissReceiver.ACTION_BADGE_CHANGED)
        )

        // 2) Load notes, then let DiffUtil dispatch granular updates
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

        // keep your source-of-truth list in sync
        notes.clear()
        notes.addAll(newNotes)

        // 🔹 DiffUtil-based updates (replaces notifyDataSetChanged)
        notesAdapter.updateList(newNotes)

        // Per-row state after the data is in the adapter
        notesAdapter.preloadReminderFlags(this)
        notesCount = newNotes.size
        updateNotePlaceholder() // no-op
        showFabBadge(null)      // no-op
    }

    override fun onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(badgeChangedReceiver)
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
                    val idx = notes.indexOf(newNoteContent)
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
        val pendingDeletes = prefs.getStringSet("pending_deletes", emptySet()) ?: return
        if (pendingDeletes.isEmpty()) return

        val upm = UserPreferencesManager(this)
        if (!upm.isLoggedIn) return

        val session = SupabaseManager.client.auth.currentSessionOrNull() ?: return
        val userId = session.user?.id ?: return

        lifecycleScope.launch(Dispatchers.IO) {
            pendingDeletes.forEach { content ->
                try {
                    SupabaseManager.client
                        .postgrest
                        .from("user_notes")
                        .delete {
                            filter {
                                eq("user_id", userId)
                                eq("content", content)
                            }
                        }
                } catch (e: Exception) {
                    Log.e("SupabaseDelete", "Failed to delete note: $content", e)
                }
            }
            prefs.edit { remove("pending_deletes") } // clear after sync
        }
    }


    @Suppress("DEPRECATION")
    private val editNoteLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data        = result.data ?: return@registerForActivityResult
                val updatedNote = data.getStringExtra("UPDATED_NOTE")
                val updatedTitle= data.getStringExtra("UPDATED_TITLE")
                val position    = data.getIntExtra("NOTE_POSITION", -1)

                val updatedImageStrings = data.getStringArrayListExtra("UPDATED_IMAGES") ?: arrayListOf()
                val updatedImageUris = updatedImageStrings.mapNotNull { runCatching { it.toUri() }.getOrNull() }

                if (!updatedNote.isNullOrEmpty() && position in notes.indices) {
                    val oldNote = notes[position]

                    if (oldNote != updatedNote) notesAdapter.migrateUserTitle(oldNote, updatedNote)
                    NoteMediaStore.setUris(this, updatedNote, updatedImageUris)

                    notes[position] = updatedNote
                    if (oldNote == updatedNote) {
                        // position might be wrong if user was searching; use content-based notify:
                        notesAdapter.notifyByContent(updatedNote, NotesAdapter.PAYLOAD_IMAGES)
                    } else {
                        notesAdapter.submit(notes.toList())
                    }


                    saveNotes()
                    if (oldNote != updatedNote) queueEditForSync(oldNote, updatedNote)

                    if (updatedTitle != null) {
                        if (updatedTitle.isNotBlank()) {
                            notesAdapter.setUserTitle(updatedNote, updatedTitle)
                        } else {
                            notesAdapter.removeUserTitle(updatedNote)
                        }
                    }

                    val root = findViewById<View>(R.id.container) ?: findViewById(android.R.id.content)
                    com.google.android.material.snackbar.Snackbar.make(root, "Note updated successfully", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
                }
            }
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
        notes.add(content)
//        notesAdapter.notifyItemInserted(notes.size - 1)
        notesAdapter.submit(notes.toList())
        notesCount = notes.size
        saveNotes()
        updateNotePlaceholder()
        showFabBadge(null) // no-op

        // mark for later sync
        val sp = getSharedPreferences("notes_prefs", MODE_PRIVATE)
        val pending = sp.getStringSet("pending_adds", emptySet())!!.toMutableSet()
        pending.add(content)
        sp.edit { putStringSet("pending_adds", pending) }
    }

    private fun saveNotes() {
        val notesJson = Gson().toJson(notes)
        sharedPreferences.edit { putString("notes_list", notesJson) }
        NotesCacheManager.cachedNotes = notes.toMutableList()
        notesCount = notes.size
        showFabBadge(null) // no-op
    }

    private suspend fun syncPendingAddsOnly() {
        val session = SupabaseManager.client.auth.currentSessionOrNull() ?: return
        val userId  = session.user?.id ?: return

        val sp = getSharedPreferences("notes_prefs", MODE_PRIVATE)
        val pendingAdds = sp.getStringSet("pending_adds", emptySet())!!.toMutableSet()
        if (pendingAdds.isEmpty()) return

        // Fetch current remote contents once
        val remoteRows: List<UserNote> = SupabaseManager.client
            .postgrest
            .from("user_notes")
            .select { filter { eq("user_id", userId) } }
            .decodeList()
        val remoteContents = remoteRows.map { it.content }.toSet()

        // Only upload items that are truly new (and remove them from pending_adds when done)
        val stillPending = pendingAdds.toMutableSet()
        for (content in pendingAdds) {
            if (content in remoteContents) {
                stillPending.remove(content) // already exists remotely
                continue
            }
            try {
                val inserted: List<UserNote> = SupabaseManager.client
                    .postgrest
                    .from("user_notes")
                    .insert(listOf(UserNote(userId = userId, content = content))) { select() }
                    .decodeList()

                if (inserted.isNotEmpty()) {
                    stillPending.remove(content)
                    // optional: update idToContent/contentToId with inserted.first().id
                }
            } catch (_: Exception) {
                // keep in stillPending; will retry next sync
            }
        }

        sp.edit { putStringSet("pending_adds", stillPending) }
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

        // 1) Fetch COMPLETE remote snapshot
        val remoteRows: List<UserNote> = SupabaseManager.client
            .postgrest
            .from("user_notes")
            .select { filter { eq("user_id", userId) } }
            .decodeList()

        val remoteContents = remoteRows.map { it.content }.toSet()

        // 2) Load local pending sets so we don’t accidentally delete unsynced local adds
        val sp = getSharedPreferences("notes_prefs", MODE_PRIVATE)
        val pendingAdds    = sp.getStringSet("pending_adds", emptySet())!!.toMutableSet()
        val pendingDeletes = sp.getStringSet("pending_deletes", emptySet())!!.toMutableSet()

        // 3) Build maps for fast lookups
        val localSet = notes.toSet()

        // 4) Detect remote→local INSERTS (present remotely, missing locally, not locally deleted)
        val toInsertLocally = remoteContents
            .filter { it !in localSet && it !in pendingDeletes }

        // 5) Detect remote→local DELETES (present locally, missing remotely)
        //    but DO NOT delete notes that are pending local-add (not yet uploaded)
        val toDeleteLocally = localSet
            .filter { it !in remoteContents && it !in pendingAdds }

        // 6) Detect remote→local UPDATES (same row id, different content)
        //    We’ll use your id<->content maps to find content changes.
        val remoteIdToContent = remoteRows
            .mapNotNull { r -> r.id?.let { it to r.content } }
            .toMap()

        val updates: List<Pair<String, String>> = buildList {
            for ((rid, rContent) in remoteIdToContent) {
                val old = idToContent[rid]
                if (old != null && old != rContent) add(old to rContent)
            }
        }

        // 7) Apply to UI on main thread
        withContext(Dispatchers.Main) {

            // 7a) Apply updates (content changed for the same row id)
            for ((oldContent, newContent) in updates) {
                val idx = notes.indexOf(oldContent)
                if (idx != -1 && oldContent !in pendingDeletes) {
                    // migrate user title + flags/badges
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

                    notes[idx] = newContent
                }
                // refresh id maps
                val rid = contentToId[oldContent] ?: remoteIdToContent.entries
                    .firstOrNull { it.value == newContent }?.key
                rid?.let {
                    idToContent[it] = newContent
                    contentToId.remove(oldContent)
                    contentToId[newContent] = it
                }
            }

            // 7b) Inserts
            for (c in toInsertLocally) {
                notes.add(c)
            }

            // 7c) Deletes (remote deleted → remove locally)
            if (toDeleteLocally.isNotEmpty()) {
                toDeleteLocally.forEach { c ->
                    notesAdapter.removeUserTitle(c)
                    NotesCacheManager.cachedTitles.remove(c)
                    getSharedPreferences("reminder_badges", MODE_PRIVATE).edit {
                        remove(c.hashCode().toString())
                    }
                    getSharedPreferences("reminder_flags", MODE_PRIVATE).edit {
                        remove(c.hashCode().toString())
                    }
                    NoteMediaStore.deleteAllForNote(this@AllNotesActivity, c)
                    removeUidFor(c)
                }
                notes.removeAll(toDeleteLocally.toSet())
            }

            // 7d) Refresh adapter once with DiffUtil
            notesAdapter.submit(notes.toList())

            // 7e) Persist
            saveNotes()
            saveIdMaps()

            // optional cosmetics
            notesCount = notes.size
            updateNotePlaceholder()
            showFabBadge(null)
        }

        // We pulled a full snapshot; we can move the last_server_updated_at up to "now"
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
        } else {
            val title = resolveTitle(note)
            val intent = ViewNoteActivity.newIntent(this, note, position, title)
            viewNoteLauncher.launch(intent)
            overridePendingTransition(R.anim.m3_motion_fade_enter, R.anim.m3_motion_fade_exit)
        }
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

                if (!updatedNote.isNullOrEmpty() && position in notes.indices) {
                    val oldNote = notes[position]

                    if (oldNote != updatedNote) {
                        notesAdapter.migrateUserTitle(oldNote, updatedNote)
                    }

                    NoteMediaStore.setUris(this, updatedNote, updatedImageUris)

                    notes[position] = updatedNote
                    if (oldNote == updatedNote) {
                        notesAdapter.notifyItemChanged(position, NotesAdapter.PAYLOAD_IMAGES)
                    } else {
                        notesAdapter.notifyItemChanged(position)
                    }

                    saveNotes()

                    if (oldNote != updatedNote) {
                        queueEditForSync(oldNote, updatedNote)
                    }

                    if (updatedTitle != null) {
                        if (updatedTitle.isNotBlank()) {
                            notesAdapter.setUserTitle(updatedNote, updatedTitle)
                        } else {
                            notesAdapter.removeUserTitle(updatedNote)
                        }
                    }

                    val root = findViewById<View>(R.id.container) ?: findViewById(android.R.id.content)
                    com.google.android.material.snackbar.Snackbar
                        .make(root, "Note updated successfully", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
                        .show()
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
            SupabaseManager.client.postgrest.from("user_notes")
                .delete { filter { eq("user_id", userId); eq("content", noteContent) } }
            Log.d("Supabase", "✅ Deleted from Supabase: $noteContent")
        } catch (e: Exception) {
            Log.e("Supabase", "❌ Failed to delete from Supabase", e)
        }
    }

    private fun deleteSelectedNotes() {
        val deleted: List<Triple<String, Int, String?>> =
            selectedKeys.mapNotNull { key ->
                val note = uidToContent[key] ?: return@mapNotNull null
                val idx = notes.indexOf(note)
                if (idx != -1) Triple(note, idx, notesAdapter.getUserTitle(note)) else null
            }.sortedByDescending { it.second }
        val imagesByNote: Map<String, List<android.net.Uri>> =
            deleted.associate { (note, _, _) -> note to NoteMediaStore.getUris(this, note) }

        // 🚩 THIS is what you will actually delete (strings = note contents)
        val notesToDelete: List<String> = deleted.map { it.first }

        // 1) queue for server-side delete
        val sp = getSharedPreferences("notes_prefs", MODE_PRIVATE)
        val pendingDeletes = sp.getStringSet("pending_deletes", emptySet())!!.toMutableSet()
        notesToDelete.forEach { pendingDeletes.add(it) }            // <-- use notesToDelete (NOT selectedNotes)
        sp.edit { putStringSet("pending_deletes", pendingDeletes) }

        // 2) fire Supabase deletes
        lifecycleScope.launch {
            notesToDelete.forEach { deleteNoteFromSupabase(it) }    // <-- use notesToDelete
        }

        // 3) local cleanup (UI state, titles, flags, media, list)
        deleted.forEach { (note, _, _) ->
            notesAdapter.removeUserTitle(note)
            NotesCacheManager.cachedTitles.remove(note)
            getSharedPreferences("reminder_badges", MODE_PRIVATE).edit { remove(note.hashCode().toString()) }
            getSharedPreferences("reminder_flags", MODE_PRIVATE).edit { remove(note.hashCode().toString()) }
        }

        deleted.forEach { (_, index, _) -> notes.removeAt(index) }
        notesAdapter.submit(notes.toList())
        if (notes.isEmpty()) removeFabBadge(null)

        // 4) clear new selection store
        selectedKeys.clear()                                        // <-- clear KEYS
        isMultiSelectMode = false
        notesAdapter.clearSelection()
        setDeleteVisibleFromActivity?.invoke(false)

        saveNotes()
        notesCount = notes.size
        updateNotePlaceholder()

        // 5) Snackbar UNDO → restore list + remove from pending_deletes so we don't delete them later
        val parentView = findViewById<View>(R.id.container) ?: findViewById(android.R.id.content)
        com.google.android.material.snackbar.Snackbar
            .make(parentView, "Note deleted", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
            .setAction("UNDO") {
                deleted.sortedBy { it.second }.forEach { (note, index, savedTitle) ->
                    val insertAt = index.coerceIn(0, notes.size)
                    if (!notes.contains(note)) notes.add(insertAt, note)
                    if (!savedTitle.isNullOrBlank()) notesAdapter.setUserTitle(note, savedTitle)
                    imagesByNote[note]?.let { uris -> NoteMediaStore.setUris(this, note, uris) }
                }

                notesAdapter.submit(notes.toList())
                saveNotes()
                updateNotePlaceholder()
                showFabBadge(null)

                // 🔄 pull them OUT of pending_deletes so background sync won’t delete them later
                val sp2 = getSharedPreferences("notes_prefs", MODE_PRIVATE)
                val pDel2 = sp2.getStringSet("pending_deletes", emptySet())!!.toMutableSet()
                notesToDelete.forEach { pDel2.remove(it) }
                sp2.edit { putStringSet("pending_deletes", pDel2) }

                selectedKeys.clear()
                isMultiSelectMode = false
                notesAdapter.clearSelection()
                setDeleteVisibleFromActivity?.invoke(false)
                updateDeleteButtonUI(false)
                enableDeleteInNavigationRail(false)
            }
            .addCallback(object : com.google.android.material.snackbar.Snackbar.Callback() {
                override fun onDismissed(sb: com.google.android.material.snackbar.Snackbar, event: Int) {
                    if (event != DISMISS_EVENT_ACTION) {
                        deleted.forEach { (note, _, _) ->
                            NoteMediaStore.deleteAllForNote(this@AllNotesActivity, note)
                            removeUidFor(note)
                        }
                    }
                }
            })
            .show()

        updateDeleteButtonUI(false)
        enableDeleteInNavigationRail(false)
    }

    // ----------------------- Search & pickers (can stay XML-based dialogs) -----------------------

    private fun loadNotesHeadless() {
        val cached = NotesCacheManager.cachedNotes
        if (cached.isNotEmpty()) {
            notes.addAll(cached)
            Log.d("DEBUG", "✅ Loaded ${cached.size} notes from MEMORY cache")
        } else {
            val notesJson = sharedPreferences.getString("notes_list", null)
            if (!notesJson.isNullOrEmpty()) {
                val type = object : TypeToken<MutableList<String>>() {}.type
                val savedNotes: MutableList<String> = Gson().fromJson(notesJson, type)
                notes.addAll(savedNotes)
                NotesCacheManager.cachedNotes.addAll(savedNotes)
                Log.d("DEBUG", "✅ Loaded ${savedNotes.size} notes from SharedPreferences")
            }
        }
        updateNotePlaceholder()
        showFabBadge(null)
    }

    private fun openSearchView() {
        val parentView = findViewById<ViewGroup>(android.R.id.content)
        val inflater = LayoutInflater.from(this)
        val rootLayout = inflater.inflate(
            R.layout.dialog_search_all_notes,
            parentView,
            false
        ) as androidx.coordinatorlayout.widget.CoordinatorLayout

        val searchView = rootLayout.findViewById<androidx.appcompat.widget.SearchView>(R.id.material_search_view)

        searchView.isIconified = false
        searchView.requestFocus()

        val searchPlate = searchView.findViewById<View>(androidx.appcompat.R.id.search_plate)
        searchPlate.setBackgroundColor(Color.TRANSPARENT)

        val searchText = searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
        searchText.setTextColor(Color.GRAY)
        searchText.setHintTextColor("#B3FFFFFF".toColorInt())

        val searchDialog = BottomSheetDialog(this).apply {
            setContentView(rootLayout)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }

        val bottomSheet = searchDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            behavior.isHideable = true
            behavior.skipCollapsed = true

            rootLayout.viewTreeObserver.addOnGlobalLayoutListener {
                val rect = Rect()
                rootLayout.getWindowVisibleDisplayFrame(rect)
                val screenHeight = rootLayout.rootView.height
                val keyboardHeight = screenHeight - rect.bottom

                behavior.peekHeight = if (keyboardHeight > 300) {
                    val maxHeight = screenHeight - keyboardHeight - 10
                    maxHeight.coerceAtMost(screenHeight / 12)
                } else {
                    screenHeight / 4
                }
            }
        }

        searchDialog.show()

        val animator = ObjectAnimator.ofFloat(searchView, "translationY", 0f, 30f).apply {
            duration = 200
            interpolator = DecelerateInterpolator()
        }
        animator.start()

        searchView.queryHint = ""
        searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                filterNotes(newText.orEmpty()); return true
            }
        })

        val closeButton = searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)
        closeButton?.let {
            it.setImageResource(R.drawable.baseline_close_24)
            it.visibility = View.VISIBLE
            val lp = it.layoutParams
            lp.width = 90; lp.height = 90
            it.layoutParams = lp
            it.scaleType = ImageView.ScaleType.CENTER_INSIDE
            it.setBackgroundResource(R.drawable.custom_clear_button)
            it.setColorFilter(Color.BLACK, android.graphics.PorterDuff.Mode.SRC_IN)
            it.setOnClickListener {
                searchView.setQuery("", false)
                searchView.requestFocus()
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(searchView, InputMethodManager.SHOW_IMPLICIT)
            }
        }
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

        // badge + bell flags
        getSharedPreferences("reminder_badges", MODE_PRIVATE)
            .edit { putBoolean(note.hashCode().toString(), true) }
        notesAdapter.preloadBadgeStates(this)

        getSharedPreferences("reminder_flags", MODE_PRIVATE)
            .edit { putBoolean(note.hashCode().toString(), true) }
        notesAdapter.preloadReminderFlags(this)

        // refresh only this row
        notesAdapter.notifyByContent(note)

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

    private fun goToHomeScreen() {
        startActivity(Intent(this, SplashActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    private fun goToContactScreen() {
        startActivity(Intent(this, Contact::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    private fun goToAllContactsScreen() {
        val intent = Intent(this, AllContactsActivity::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    private fun goToSettingsScreen() {
        startActivity(Intent(this, SettingsActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    // ----------------------- Misc -----------------------

    private fun adjustStatusBarIcons() {
        val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        when (nightModeFlags) {
            Configuration.UI_MODE_NIGHT_YES -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2) {
                    window.insetsController?.setSystemBarsAppearance(
                        0,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    )
                } else {
                    @Suppress("DEPRECATION")
                    window.decorView.systemUiVisibility = 0
                }
            }
            Configuration.UI_MODE_NIGHT_NO -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2) {
                    window.insetsController?.setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    )
                } else {
                    @Suppress("DEPRECATION")
                    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                }
            }
        }
    }

    private fun filterNotes(query: String) {
        val filtered = if (query.isEmpty()) notes else notes.filter { it.contains(query, true) }
        notesAdapter.updateList(filtered)
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
            val changedIndex = notes.indexOfFirst { it.hashCode().toString() == badgeKey }
            if (changedIndex != -1) {
                notesAdapter.preloadBadgeStates(ctx)
                notesAdapter.notifyItemChanged(changedIndex)
            }
        }
    }
}
