package com.flights.studio

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowInsetsController
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class AllNotesActivity : AppCompatActivity() {

    private lateinit var notesAdapter: NotesAdapter
    private val notes = mutableListOf<String>()
    private val selectedNotes = mutableListOf<String>() // Track selected notes
    private val sharedPreferences by lazy { getSharedPreferences("notes_prefs", MODE_PRIVATE) }
    private var isMultiSelectMode = false // Track multi-select mode
    private var selectionSnackbar: Snackbar? = null // Snackbar for showing selection count
    private lateinit var logoFab: FloatingActionButton


    private val addNoteLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val newNote = result.data?.getStringExtra("NEW_NOTE")
                if (!newNote.isNullOrEmpty()) {
                    notes.add(newNote)
                    notesAdapter.notifyItemInserted(notes.size - 1) // Efficiently update RecyclerView
                    saveNotes()

                    // Show a Snackbar indicating the note was created
                    Snackbar.make(
                        findViewById(R.id.rv_notes), // Parent view to attach the Snackbar
                        "Note created successfully",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
        }

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_notes)

        adjustStatusBarIcons()

        logoFab = findViewById(R.id.logo)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
            setHomeAsUpIndicator(AppCompatResources.getDrawable(this@AllNotesActivity, R.drawable.layered_arrow))
        }

        // Load notes from SharedPreferences
        loadNotes()

        val notesRecyclerView = findViewById<RecyclerView>(R.id.rv_notes)
        val addNoteFab = findViewById<FloatingActionButton>(R.id.fab_add_note)

        notesAdapter = NotesAdapter(
            notes,
            ::onNoteLongClick,
            ::onNoteClick, // Now accepts both `note` and `position`
        ) { note, position ->
            val intent = EditNoteActivity.newIntent(this, note, position)
            editNoteLauncher.launch(intent)
            overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation)
        }



        notesRecyclerView.layoutManager = LinearLayoutManager(this)
        notesRecyclerView.adapter = notesAdapter

        addNoteFab.setOnClickListener {
            val intent = AddNoteActivity.newIntent(this)
            addNoteLauncher.launch(intent)
            overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation)

        }
        notesRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0 && addNoteFab.visibility == View.VISIBLE) {
                    // Scrolling down, hide with zoom out
                    zoomOutHideFab(addNoteFab)
                } else if (dy < 0 && addNoteFab.visibility != View.VISIBLE) {
                    // Scrolling up, show with zoom in
                    zoomInShowFab(addNoteFab)
                }
            }
        })

        logoFab.setOnClickListener {
            val scaleUp = ObjectAnimator.ofFloat(logoFab, "scaleX", 1f, 1.2f, 1f)
            val scaleUpY = ObjectAnimator.ofFloat(logoFab, "scaleY", 1f, 1.2f, 1f)
            scaleUp.duration = 100
            scaleUpY.duration = 100

            val moveRight = ObjectAnimator.ofFloat(logoFab, "translationX", 0f, 50f)
            moveRight.duration = 200

            val flip = ObjectAnimator.ofFloat(logoFab, "rotationY", 0f, 360f, 0f, -360f, 0f, 360f, 0f, -360f, 0f, 360f, 0f, -360f, 0f, 360f)
            flip.duration = 700

            val moveBack = ObjectAnimator.ofFloat(logoFab, "translationX", 50f, 0f)
            moveBack.duration = 200

            val animatorSet = AnimatorSet()
            animatorSet.play(scaleUp).with(scaleUpY)
            animatorSet.play(moveRight).after(scaleUp)
            animatorSet.play(flip).after(moveRight)
            animatorSet.play(moveBack).after(flip)

            animatorSet.start()
        }

    }


    private fun adjustStatusBarIcons() {
        val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        when (nightModeFlags) {
            Configuration.UI_MODE_NIGHT_YES -> {
                // Dark mode: Light status bar icons/text
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    window.insetsController?.setSystemBarsAppearance(
                        0, // Clear light status bar appearance
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    )
                } else {
                    @Suppress("DEPRECATION")
                    window.decorView.systemUiVisibility = 0 // Clear light status bar flag
                }
            }
            Configuration.UI_MODE_NIGHT_NO -> {
                // Light mode: Dark status bar icons/text
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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
    @Suppress("DEPRECATION")
    private val viewNoteLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val updatedNote = result.data?.getStringExtra("UPDATED_NOTE")
                val position = result.data?.getIntExtra("NOTE_POSITION", -1) ?: -1

                if (updatedNote != null && position != -1) {
                    notes[position] = updatedNote // Update the note in the list
                    notesAdapter.notifyItemChanged(position) // Notify adapter of the change
                    saveNotes() // Save the updated notes
                    Snackbar.make(
                        findViewById(R.id.rv_notes),
                        "Note updated successfully",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
        }

    @Suppress("DEPRECATION")
    private fun onNoteClick(note: String, position: Int) {
        if (isMultiSelectMode) {
            toggleNoteSelection(note)
        } else {
            val intent = ViewNoteActivity.newIntent(this, note, position)
            viewNoteLauncher.launch(intent)
            overridePendingTransition(R.anim.m3_motion_fade_enter, R.anim.m3_motion_fade_exit) // Apply animation
        }
    }


    private fun onNoteLongClick(note: String) {
        isMultiSelectMode = true
        toggleNoteSelection(note)
    }

    private fun toggleNoteSelection(note: String) {
        if (selectedNotes.contains(note)) {
            selectedNotes.remove(note)
        } else {
            selectedNotes.add(note)
        }

        // Update multi-select mode based on selection count
        isMultiSelectMode = selectedNotes.isNotEmpty()

        notesAdapter.toggleSelection(note) // Notify the adapter about changes
        invalidateOptionsMenu() // Trigger a menu redraw to show/hide the delete icon

        // Update or create the selection Snackbar
        if (isMultiSelectMode) {
            if (selectionSnackbar == null) {
                // Create Snackbar if it doesn't exist
                selectionSnackbar = Snackbar.make(
                    findViewById(R.id.rv_notes),
                    "${selectedNotes.size} notes selected",
                    Snackbar.LENGTH_INDEFINITE
                ).setAction("DELETE") {
                    deleteSelectedNotes()
                }
                selectionSnackbar?.show()
            } else {
                // Update the text of the existing Snackbar
                selectionSnackbar?.setText("${selectedNotes.size} notes selected")
            }
        } else {
            // Dismiss the Snackbar if no notes are selected
            selectionSnackbar?.dismiss()
            selectionSnackbar = null
        }
    }

    private fun deleteSelectedNotes() {
        // Pair notes with their indices and sort by descending index to prevent shifting issues
        val deletedNotesWithIndices = selectedNotes.mapNotNull { note ->
            val index = notes.indexOf(note)
            if (index != -1) Pair(note, index) else null
        }.sortedByDescending { it.second } // Sort by index in descending order

        // Remove notes from the list and notify the adapter
        deletedNotesWithIndices.forEach { (_, index) ->
            notes.removeAt(index)
            notesAdapter.notifyItemRemoved(index)
        }

        selectedNotes.clear()
        isMultiSelectMode = false
        notesAdapter.clearSelection()
        saveNotes()
        invalidateOptionsMenu() // Update the menu

        selectionSnackbar?.dismiss() // Dismiss the selection snackbar
        selectionSnackbar = null // Reset the Snackbar

        // Show "Notes deleted" Snackbar with undo option
        Snackbar.make(findViewById(R.id.rv_notes), "Notes deleted", Snackbar.LENGTH_LONG)
            .setAction("UNDO") {
                // Restore the deleted notes in ascending order by index
                deletedNotesWithIndices.sortedBy { it.second } // Sort back to ascending order
                    .forEach { (note, index) ->
                        notes.add(index, note)
                        notesAdapter.notifyItemInserted(index)
                    }
                saveNotes() // Save restored notes
            }
            .show()
    }

    private fun loadNotes() {
        val notesJson = sharedPreferences.getString("notes_list", null)
        if (!notesJson.isNullOrEmpty()) {
            val type = object : TypeToken<MutableList<String>>() {}.type
            val savedNotes: MutableList<String> = Gson().fromJson(notesJson, type)
            notes.addAll(savedNotes)
        }
    }

    private fun saveNotes() {
        val notesJson = Gson().toJson(notes)
        sharedPreferences.edit().putString("notes_list", notesJson).apply()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.notes_menu, menu)

        val searchItem = menu?.findItem(R.id.action_search) // Assuming your menu has this ID
        val searchView = searchItem?.actionView as? androidx.appcompat.widget.SearchView

        searchView?.queryHint = "Search notes..."
        searchView?.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false // We handle text change, no action needed on submit
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterNotes(newText.orEmpty()) // Filter notes dynamically
                return true
            }
        })

        menu?.findItem(R.id.action_delete)?.isVisible = isMultiSelectMode
        return true
    }
    private fun filterNotes(query: String) {
        val filteredNotes = if (query.isEmpty()) {
            notes // Show all notes when the query is empty
        } else {
            notes.filter { it.contains(query, ignoreCase = true) } // Case-insensitive filtering
        }

        notesAdapter.updateList(filteredNotes)
    }


    @Suppress("DEPRECATION")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_delete -> {
                deleteSelectedNotes()
                true
            }
            android.R.id.home -> {
                finish() // Finish the activity
                overridePendingTransition(R.anim.m3_motion_fade_enter, R.anim.m3_motion_fade_exit) // Apply back animation
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    private fun zoomOutHideFab(fab: FloatingActionButton) {
        fab.animate()
            .scaleX(0f)
            .scaleY(0f)
            .alpha(0f)
            .setDuration(90)
            .withEndAction { fab.visibility = View.INVISIBLE }
            .start()
    }

    private fun zoomInShowFab(fab: FloatingActionButton) {
        fab.visibility = View.VISIBLE
        fab.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(90)
            .start()
    }
    private val editNoteLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val updatedNote = result.data?.getStringExtra("UPDATED_NOTE")
                val position = result.data?.getIntExtra("NOTE_POSITION", -1) ?: -1

                if (updatedNote != null && position != -1) {
                    notes[position] = updatedNote // Update the note in the list
                    notesAdapter.notifyItemChanged(position) // Notify adapter of changes
                    saveNotes() // Save the updated notes to SharedPreferences

                    Snackbar.make(
                        findViewById(R.id.rv_notes),
                        "Note updated successfully",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
        }


}
