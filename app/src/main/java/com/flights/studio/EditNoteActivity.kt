package com.flights.studio
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.WindowInsetsController
import android.view.animation.DecelerateInterpolator
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.AnimRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.textfield.TextInputEditText


class EditNoteActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_NOTE = "NOTE"
        private const val EXTRA_POSITION = "NOTE_POSITION"
        private const val EXTRA_TITLE = "NOTE_TITLE"


        fun newIntent(context: Context, note: String, position: Int, title: String? = null): Intent {
            return Intent(context, EditNoteActivity::class.java).apply {
                putExtra(EXTRA_NOTE, note)
                putExtra(EXTRA_POSITION, position)
                putExtra(EXTRA_TITLE, title.orEmpty())
            }
        }
    }

    private lateinit var tvImagesCount: TextView
    private var isNoteModified = false
    private var currentNoteText: String = ""               // 👈 track the key used for images
    private val pickedImages = mutableListOf<Uri>()
    private lateinit var imagesAdapter: EditImagesAdapter  // 👈 fixed type
    private lateinit var toolbarView: com.google.android.material.appbar.MaterialToolbar
    private lateinit var nestedScroll: NestedScrollView
    private lateinit var titleEt: TextInputEditText
    private var helpSheet: com.google.android.material.bottomsheet.BottomSheetDialog? = null
    private lateinit var imagesCard: com.google.android.material.card.MaterialCardView


    private fun refreshImagesUI() {
        val c = pickedImages.size
        imagesCard.visibility = if (c > 0) View.VISIBLE else View.GONE
        tvImagesCount.text = c.toString()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_note)
        adjustStatusBarIcons()

        toolbarView   = findViewById(R.id.toolbar)
        nestedScroll  = findViewById(R.id.nestedScroll)
        titleEt       = findViewById(R.id.et_title)
        tvImagesCount = findViewById(R.id.tv_images_count)
        imagesCard    = findViewById(R.id.card_images)   // ✅ INIT THIS
        refreshImagesUI()                                 // ✅ call AFTER both are ready

        // Toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
            setHomeAsUpIndicator(
                AppCompatResources.getDrawable(this@EditNoteActivity, R.drawable.layered_arrow)
            )
            title = ""
        }
        bindDynamicToolbarTitle()

        val noteEt  = findViewById<AutoCompleteTextView>(R.id.edit_note)
        val titleEt = findViewById<TextInputEditText>(R.id.et_title)
        val saveBtn = findViewById<Button>(R.id.btn_save)

        // Suggestions
        val wordSuggestions = mapOf(
            "hello" to listOf("world", "there", "everyone"),
            "how"   to listOf("are you", "is it going", "much"),
            "thank" to listOf("you", "goodness", "heavens"),
            "good"  to listOf("morning", "evening", "job"),
            "what"  to listOf("is", "are", "happened")
        )
        val suggestionAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, emptyList<String>())
        noteEt.setAdapter(suggestionAdapter)
        noteEt.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val lastWord = s?.toString()?.split(" ")?.lastOrNull()?.lowercase().orEmpty()
                val suggestions = wordSuggestions[lastWord] ?: emptyList()
                suggestionAdapter.clear()
                suggestionAdapter.addAll(suggestions)
                suggestionAdapter.notifyDataSetChanged()
                if (suggestions.isNotEmpty()) noteEt.showDropDown()
                isNoteModified = true
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Prefill
        val note     = intent.getStringExtra(EXTRA_NOTE).orEmpty()
        val position = intent.getIntExtra(EXTRA_POSITION, -1)
        val title    = intent.getStringExtra(EXTRA_TITLE).orEmpty()

        currentNoteText = note
        noteEt.setText(note)
        if (title.isNotBlank()) titleEt.setText(title)
        titleEt.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { isNoteModified = true }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // IMAGES: ViewPager2 (fast + no parent steal)
        val imagesRv = findViewById<RecyclerView>(R.id.vp_images)

        imagesAdapter = EditImagesAdapter(
            items = pickedImages,
            onRemove = { removed ->
                val idx = pickedImages.indexOf(removed)
                if (idx != -1) {
                    pickedImages.removeAt(idx)
                    imagesAdapter.notifyItemRemoved(idx)
                }
                NoteMediaStore.removeUri(this, currentNoteText, removed)
                isNoteModified = true
                refreshImagesUI()
            }
        )

        imagesRv.apply {
            layoutManager =
                LinearLayoutManager(this@EditNoteActivity, RecyclerView.HORIZONTAL, false)
            adapter = imagesAdapter
            setHasFixedSize(true)
            isNestedScrollingEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            itemAnimator = null
            setItemViewCacheSize(16)

            // no start/end padding → no visible gap at the edges
            setPadding(0, 0, 0, 0)
            clipToPadding = true
        }

// small space ONLY between items
        // remove your old addItemDecoration first, then:
        val space = (8f * resources.displayMetrics.density).toInt() // 8dp

        imagesRv.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                out: android.graphics.Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
            ) {
                val pos = parent.getChildAdapterPosition(view)
                val isRtl = view.layoutDirection == View.LAYOUT_DIRECTION_RTL
                if (pos == 0) {
                    out.set(0, 0, 0, 0) // first: no start gap
                } else {
                    if (isRtl) out.right = space else out.left = space
                }
            }
        })


// pager-like snap (one item per swipe)
        androidx.recyclerview.widget.PagerSnapHelper().attachToRecyclerView(imagesRv)

// Let horizontal drags win over the NestedScrollView (same logic you had)
        run {
            val touchSlop = ViewConfiguration.get(this).scaledTouchSlop
            var startX = 0f
            var startY = 0f
            var lockedHorizontal = false

            imagesRv.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
                override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                    when (e.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            startX = e.x; startY = e.y
                            lockedHorizontal = false
                            rv.disallowParents(true)
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val dx = kotlin.math.abs(e.x - startX)
                            val dy = kotlin.math.abs(e.y - startY)
                            if (!lockedHorizontal) {
                                when {
                                    dx > touchSlop && dx > dy -> {
                                        lockedHorizontal = true
                                        rv.disallowParents(true)
                                    }
                                    dy > touchSlop && dy > dx -> {
                                        rv.disallowParents(false)
                                        return false
                                    }
                                }
                            } else {
                                rv.disallowParents(true)
                            }
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            rv.disallowParents(false)
                            val dx = kotlin.math.abs(e.x - startX)
                            val dy = kotlin.math.abs(e.y - startY)
                            if (dx < touchSlop && dy < touchSlop) {
                                rv.findChildViewUnder(e.x, e.y)?.performClick()
                            }
                        }
                    }
                    return false
                }
                override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
                override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
            })
        }

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT, 0
        ) {
            override fun onMove(
                rv: RecyclerView,
                vh: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = vh.bindingAdapterPosition
                val to   = target.bindingAdapterPosition
                if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) return false
                imagesAdapter.move(from, to)   // mutates pickedImages via adapter
                return true
            }

            override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) {}
            override fun isLongPressDragEnabled(): Boolean = true

            override fun clearView(rv: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(rv, viewHolder)
                NoteMediaStore.setUris(this@EditNoteActivity, currentNoteText, pickedImages) // persist once
                isNoteModified = true
                refreshImagesUI()
            }
        }).attachToRecyclerView(imagesRv)
        /*  ▼▼▼ PASTE THIS BLOCK HERE ▼▼▼  */
// Load any existing images (show card only if there are some)
        val initial = NoteMediaStore.getUris(this, currentNoteText)
        if (initial.isNotEmpty()) {
            val start = pickedImages.size // usually 0 on first open
            pickedImages.addAll(initial)
            imagesAdapter.notifyItemRangeInserted(start, initial.size)
            imagesRv.scrollToPosition(0)
        }
        refreshImagesUI()

        /*  ▲▲▲ PASTE ENDS ▲▲▲  */


        // Add-image button
        findViewById<View>(R.id.ll_add_image).setOnClickListener {
            pickImagesLauncher.launch(arrayOf("image/*"))
        }

        // Help
        findViewById<com.google.android.material.card.MaterialCardView>(R.id.HelpButton)
            ?.setOnClickListener {
                if (helpSheet?.isShowing == true) return@setOnClickListener
                showEditNoteHelpDialog()
            }


        ViewCompat.setTooltipText(findViewById(R.id.HelpButton), getString(R.string.help))

        // Save
        saveBtn.setOnClickListener {
            val updatedNote  = noteEt.text?.toString().orEmpty()
            val updatedTitle = titleEt.text?.toString()?.trim().orEmpty()

            if (updatedNote.isNotEmpty()) {
                if (updatedNote != currentNoteText) {
                    NoteMediaStore.migrateNoteKey(this, currentNoteText, updatedNote)
                    currentNoteText = updatedNote
                }
                NoteMediaStore.setUris(this, updatedNote, pickedImages)

                setResult(
                    RESULT_OK,
                    Intent().apply {
                        putExtra("UPDATED_NOTE", updatedNote)
                        putExtra("NOTE_POSITION", position)
                        putExtra("UPDATED_TITLE", updatedTitle)
                        putStringArrayListExtra(
                            "UPDATED_IMAGES",
                            ArrayList(pickedImages.map { it.toString() })
                        )
                        putExtra("UPDATED_IMAGES_COUNT", pickedImages.size) // ✅ added
                    }
                )

                finish()
                applyCloseTransition(
                    androidx.navigation.ui.R.anim.nav_default_enter_anim,
                    androidx.navigation.ui.R.anim.nav_default_exit_anim
                )

            } else {
                noteEt.error = "Note cannot be empty"
            }
        }

        // Make the whole images section non-focusable (prevents focus jumps / NSV auto-scroll)
        findViewById<View>(R.id.images_row)?.apply {
            isFocusable = false
            isFocusableInTouchMode = false
        }
    }

    private fun View.disallowParents(disallow: Boolean) {
        var p = parent
        while (p != null) {
            p.requestDisallowInterceptTouchEvent(disallow)
            p = p.parent
        }
    }

    private fun applyCloseTransition(@AnimRes enterAnim: Int, @AnimRes exitAnim: Int) {
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, enterAnim, exitAnim)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(enterAnim, exitAnim)
        }
    }

//    private fun applyOpenTransition(@AnimRes enterAnim: Int, @AnimRes exitAnim: Int) {
//        if (Build.VERSION.SDK_INT >= 34) {
//            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, enterAnim, exitAnim)
//        } else {
//            @Suppress("DEPRECATION")
//            overridePendingTransition(enterAnim, exitAnim)
//        }
//    }
//


    private fun showEditNoteHelpDialog() {
        val parent = findViewById<ViewGroup>(android.R.id.content)
        val view = layoutInflater.inflate(
            R.layout.dialog_add_note_help,
            parent,            // ✅ real parent so LayoutParams resolve
            false              // ✅ don't attach yet
        )

        val sheet = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        sheet.setContentView(view)

        sheet.setOnShowListener {
            val bs = sheet.findViewById<android.widget.FrameLayout>(
                com.google.android.material.R.id.design_bottom_sheet
            )

            // No rounded corners on the container
            bs?.background = android.graphics.Color.TRANSPARENT.toDrawable()

            // Give YOUR content a square background (no corners)
            // If your dialog_add_note_help already has its own bg, keep it.
            if (view.background == null) {
                val surface = com.google.android.material.color.MaterialColors.getColor(
                    view, com.google.android.material.R.attr.colorSurface
                )
                view.background = surface.toDrawable()
            }

            // Open fully from bottom, no janky slide/drag
            val behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(bs!!)
            behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
            behavior.isFitToContents = true
            behavior.isDraggable = false  // prevent sliding after open
        }

        // Buttons
        view.findViewById<View>(R.id.btnHelpAddImage).setOnClickListener {
            findViewById<View>(R.id.ll_add_image)?.performClick()
            sheet.dismiss()
        }
        view.findViewById<View>(R.id.btnHelpAutoTitle).setOnClickListener {
            val noteEt  = findViewById<AutoCompleteTextView>(R.id.edit_note)
            val titleEt = findViewById<TextInputEditText>(R.id.et_title)
            val text = noteEt.text?.toString().orEmpty()
            if (text.isBlank()) {
                android.widget.Toast.makeText(this, getString(R.string.type_note_first), android.widget.Toast.LENGTH_SHORT).show()
                sheet.dismiss()
            } else {
                NotesAdapter.suggestTitle(this, text) { aiTitle ->
                    val chosen = aiTitle.takeIf { it.isNotBlank() && it != "📄 Unclassified" }
                        ?: generateSmartTitle(text)
                    titleEt.setText(chosen)
                    titleEt.post { runCatching { titleEt.setSelection(titleEt.text?.length ?: 0) } }
                    android.widget.Toast.makeText(this, getString(R.string.title_set), android.widget.Toast.LENGTH_SHORT).show()
                    sheet.dismiss()
                }
            }
        }
        view.findViewById<View>(R.id.btnHelpClose)?.setOnClickListener { sheet.dismiss() }

        helpSheet = sheet
        sheet.show()
    }
    private fun generateSmartTitle(note: String): String {
        val clean = note.trim()
            .replace(Regex("\\s+"), " ")
            .replace(Regex("(https?://\\S+|www\\.\\S+)"), "")
            .trim()
        if (clean.isEmpty()) return ""
        val firstSentence = clean.split(Regex("[.!?]")).firstOrNull()?.trim().orEmpty()
        val raw = if (firstSentence.length >= 8) firstSentence else clean
        val words = raw.split(" ").filter { it.isNotBlank() }
        val clipped = words.take(10).joinToString(" ")
        return clipped.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }


    // Back navigation handling (unchanged)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> { handleBackNavigation(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun handleBackNavigation() {
        if (isNoteModified) {
            AlertDialog.Builder(this)
                .setMessage(getString(R.string.confirm_exit_message))
                .setPositiveButton(getString(R.string.exit)) { _, _ ->
                    finish()
                    applyCloseTransition(R.anim.m3_motion_fade_enter, R.anim.m3_motion_fade_exit)

                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        } else {
            finish()
            applyCloseTransition(R.anim.m3_motion_fade_enter, R.anim.m3_motion_fade_exit)

        }
    }


    private val pickImagesLauncher =
        registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.OpenMultipleDocuments()
        ) { uris ->
            if (uris.isEmpty()) return@registerForActivityResult

            // Where the new items will start in the adapter
            val startIndex = pickedImages.size
            var actuallyInserted = 0

            uris.forEach { uri ->
                // Persist read access for future loads
                try {
                    contentResolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: SecurityException) { /* ignore if already granted */ }

                // Avoid duplicates if user selects the same image again
                if (!pickedImages.contains(uri)) {
                    // Persist this image for the current note key
                    NoteMediaStore.addUri(this, currentNoteText, uri)

                    // Update in-memory list used by the adapter
                    pickedImages.add(uri)
                    actuallyInserted++
                }
            }

            // Tell the adapter exactly what changed
            if (actuallyInserted > 0) {
                imagesAdapter.notifyItemRangeInserted(startIndex, actuallyInserted)
                isNoteModified = true
                refreshImagesUI()
            }
        }


    private fun adjustStatusBarIcons() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        when (nightModeFlags) {
            Configuration.UI_MODE_NIGHT_YES -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2) {
                    window.insetsController?.setSystemBarsAppearance(
                        0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    )
                } else {
                    windowInsetsController.isAppearanceLightStatusBars = false
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

    private class EditImagesAdapter(
        private val items: MutableList<Uri>,
        private val onRemove: (Uri) -> Unit,
    ) : RecyclerView.Adapter<EditImagesAdapter.VH>() {

        init { setHasStableIds(true) }

        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val img: ImageView = v.findViewById(R.id.img)
            val btnRemove: ImageView = v.findViewById(R.id.btn_remove)
        }

        override fun getItemId(position: Int): Long = items[position].hashCode().toLong()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_note_image, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(h: VH, pos: Int) {
            val size = (120f * h.itemView.resources.displayMetrics.density).toInt()
            (h.itemView.layoutParams as RecyclerView.LayoutParams).width = size
            h.img.layoutParams.apply { width = size; height = size }

            val main = Glide.with(h.img)
                .load(items[pos])
                .centerCrop()
                .dontAnimate()

            val thumb = main.clone().sizeMultiplier(0.25f)

            main.thumbnail(thumb).into(h.img)

            h.btnRemove.setOnClickListener {
                val idx = h.bindingAdapterPosition
                if (idx != RecyclerView.NO_POSITION) {
                    val removed = items.removeAt(idx)
                    notifyItemRemoved(idx)
                    onRemove(removed)
                }
            }
        }



        override fun getItemCount(): Int = items.size

        fun move(from: Int, to: Int) {
            if (from == to) return
            val item = items.removeAt(from)
            items.add(to, item)
            notifyItemMoved(from, to)
        }

    }
    private fun bindDynamicToolbarTitle() {
        val d = resources.displayMetrics.density
        val thresholdPx = (25 * d).toInt()
        val baseColor = com.google.android.material.color.MaterialColors.getColor(
            toolbarView, com.google.android.material.R.attr.colorOnBackground
        )

        // ensure full-opacity at start
        toolbarView.setTitleTextColor(ColorUtils.setAlphaComponent(baseColor, 255))

        fun noteTitleOrFallback(): String =
            titleEt.text?.toString()?.takeIf { it.isNotBlank() } ?: getString(R.string.edit_note)

        // TOP: show "Edit note"
        toolbarView.title = getString(R.string.edit_note)
        var showingNoteTitle = false
        var isAnimating = false

        fun crossfadeTo(newTitle: String) {
            if (toolbarView.title == newTitle || isAnimating) return
            isAnimating = true

            // fade OUT current title
            val fadeOut = ValueAnimator.ofInt(255, 0).apply {
                duration = 100
                interpolator = DecelerateInterpolator()
                addUpdateListener { va ->
                    val a = va.animatedValue as Int
                    toolbarView.setTitleTextColor(ColorUtils.setAlphaComponent(baseColor, a))
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        // swap title, then fade IN
                        toolbarView.title = newTitle
                        ValueAnimator.ofInt(0, 255).apply {
                            duration = 100
                            interpolator = DecelerateInterpolator()
                            addUpdateListener { va2 ->
                                val a2 = va2.animatedValue as Int
                                toolbarView.setTitleTextColor(ColorUtils.setAlphaComponent(baseColor, a2))
                            }
                            addListener(object : AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: android.animation.Animator) {
                                    isAnimating = false
                                }
                            })
                        }.start()
                    }
                })
            }
            fadeOut.start()
        }

        nestedScroll.setOnScrollChangeListener { _: NestedScrollView, _: Int, scrollY: Int, _: Int, _: Int ->
            val atTop = scrollY <= thresholdPx
            val newTitle = if (atTop) getString(R.string.edit_note) else noteTitleOrFallback()
            crossfadeTo(newTitle)
            showingNoteTitle = !atTop
           // toolbarView.elevation = if (scrollY > 0) 2f * d else 0f
        }

        // While typing and we're scrolled (showing note title), update it live WITHOUT re-animating every keystroke
        titleEt.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (showingNoteTitle && !isAnimating) {
                    // direct set (no fade) to avoid flicker while typing
                    toolbarView.title = noteTitleOrFallback()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }


}
