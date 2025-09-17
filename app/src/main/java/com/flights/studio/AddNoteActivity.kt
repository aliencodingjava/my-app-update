@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package com.flights.studio

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.Editable
import android.text.InputFilter
import android.text.SpannableString
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.view.WindowInsetsController
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.Toast
import android.window.OnBackInvokedDispatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.edit
import androidx.core.os.BuildCompat
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.radiobutton.MaterialRadioButton
import com.google.android.material.textfield.TextInputEditText

class AddNoteActivity : AppCompatActivity() {

    // 🔁 Adapter owns its data; no separate pickedImages list
    private lateinit var imagesAdapter: SelectedImagesAdapter




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_note)
        adjustStatusBarIcons()
        window.decorView.alpha = 1.0f

        // Toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
            setHomeAsUpIndicator(
                AppCompatResources.getDrawable(this@AddNoteActivity, R.drawable.layered_arrow)
            )
        }

        // Views
        val noteEditText  = findViewById<TextInputEditText>(R.id.et_note)
        val titleEditText = findViewById<TextInputEditText>(R.id.et_title)
        val rvImages      = findViewById<RecyclerView>(R.id.rv_images)
        val imagesCard    = findViewById<com.google.android.material.card.MaterialCardView>(R.id.card_images)
        val tvImagesCount = findViewById<android.widget.TextView?>(R.id.tv_images_count) // optional; safe if missing
        val llSetReminder = findViewById<LinearLayout>(R.id.ll_set_reminder)
        val rbReminder    = findViewById<MaterialRadioButton>(R.id.rb_reminder)

       // radio itself is non-clickable in XML; toggle via the row
        llSetReminder.setOnClickListener { rbReminder.isChecked = !rbReminder.isChecked }
        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        rbReminder.setOnCheckedChangeListener { v, isChecked ->
            // Always give a tiny UI haptic (doesn't need VIBRATE permission)
            v.performHapticFeedback(
                if (isChecked) HapticFeedbackConstants.CLOCK_TICK
                else HapticFeedbackConstants.VIRTUAL_KEY
            )

            // Extra buzz when turning it ON
            if (isChecked && vibrator.hasVibrator()) {
                @Suppress("ObsoleteSdkInt")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        VibrationEffect.createOneShot(
                            /* milliseconds = */ 70,
                            /* amplitude    = */ VibrationEffect.DEFAULT_AMPLITUDE
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(70)
                }
            }
        }
        // ---- Adapter (inline UI updates in onRemove) ----
        imagesAdapter = SelectedImagesAdapter(mutableListOf()) {
            val c = imagesAdapter.itemCount
            imagesCard.visibility = if (c > 0) View.VISIBLE else View.GONE
            tvImagesCount?.text = c.toString()
            tvImagesCount?.visibility = View.VISIBLE   // always visible, even at 0
        }

        // ---- RecyclerView wiring ----
        rvImages.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(
                this@AddNoteActivity, RecyclerView.HORIZONTAL, false
            )
            adapter = imagesAdapter
            isNestedScrollingEnabled = false
            setHasFixedSize(true)
            itemAnimator = null
            setItemViewCacheSize(16)
            clipToPadding = true
            setPadding(0, 0, 0, 0)

            // spacing 8dp, RTL-aware
            val space = (8f * resources.displayMetrics.density).toInt()
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: android.graphics.Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State,
                ) {
                    val pos = parent.getChildAdapterPosition(view)
                    if (pos > 0) {
                        val rtl = androidx.core.view.ViewCompat.getLayoutDirection(parent) ==
                                androidx.core.view.ViewCompat.LAYOUT_DIRECTION_RTL
                        if (rtl) outRect.right = space else outRect.left = space
                    }
                }
            })
        }

        // initial state: card hidden if empty, counter shown as 0
        run {
            val c = imagesAdapter.itemCount
            imagesCard.visibility = if (c > 0) View.VISIBLE else View.GONE
            tvImagesCount?.text = c.toString()
            tvImagesCount?.visibility = View.VISIBLE
        }

        // keep UI synced on dataset changes (add/remove/move)
        imagesAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            private fun refresh() {
                val c = imagesAdapter.itemCount
                imagesCard.visibility = if (c > 0) View.VISIBLE else View.GONE
                tvImagesCount?.text = c.toString()
                tvImagesCount?.visibility = View.VISIBLE
            }
            override fun onChanged() = refresh()
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) = refresh()
            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) = refresh()
            override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) = refresh()
        })

        // Snap 1 item per swipe
        androidx.recyclerview.widget.PagerSnapHelper().attachToRecyclerView(rvImages)

        // Let horizontal drags win over the NestedScrollView
        run {
            val touchSlop = android.view.ViewConfiguration.get(this).scaledTouchSlop
            var startX = 0f
            var startY = 0f
            var lockedH = false

            fun View.disallowParents(disallow: Boolean) {
                var p = parent
                while (p != null) {
                    if (p is View) p.requestDisallowInterceptTouchEvent(disallow)
                    p = p.parent
                }
            }

            rvImages.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
                override fun onInterceptTouchEvent(rv: RecyclerView, e: android.view.MotionEvent): Boolean {
                    when (e.actionMasked) {
                        android.view.MotionEvent.ACTION_DOWN -> {
                            startX = e.x; startY = e.y
                            lockedH = false
                            rv.disallowParents(true)
                        }
                        android.view.MotionEvent.ACTION_MOVE -> {
                            val dx = kotlin.math.abs(e.x - startX)
                            val dy = kotlin.math.abs(e.y - startY)
                            if (!lockedH) {
                                when {
                                    dx > touchSlop && dx > dy -> { lockedH = true; rv.disallowParents(true) }
                                    dy > touchSlop && dy > dx -> { rv.disallowParents(false); return false }
                                }
                            } else rv.disallowParents(true)
                        }
                        android.view.MotionEvent.ACTION_UP,
                        android.view.MotionEvent.ACTION_CANCEL,
                            -> {
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
                override fun onTouchEvent(rv: RecyclerView, e: android.view.MotionEvent) {}
                override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
            })
        }

        // Drag-to-reorder (update counter/card too so everything stays consistent)
        androidx.recyclerview.widget.ItemTouchHelper(object :
            androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(
                androidx.recyclerview.widget.ItemTouchHelper.UP or
                        androidx.recyclerview.widget.ItemTouchHelper.DOWN or
                        androidx.recyclerview.widget.ItemTouchHelper.LEFT or
                        androidx.recyclerview.widget.ItemTouchHelper.RIGHT, 0
            ) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                imagesAdapter.move(vh.bindingAdapterPosition, target.bindingAdapterPosition)
                val c = imagesAdapter.itemCount
                imagesCard.visibility = if (c > 0) View.VISIBLE else View.GONE
                tvImagesCount?.apply {
                    text = c.toString()
                    visibility = if (c > 0) View.VISIBLE else View.GONE
                }
                return true
            }
            override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) {}
            override fun isLongPressDragEnabled(): Boolean = true
        }).attachToRecyclerView(rvImages)

        // ---- Title hint + UX ----
        val raw = try { getString(R.string.note_title_optional) } catch (_: Exception) { "Note Title (optional • AI✨)" }
        val spannable = SpannableString(raw).apply {
            val i = raw.indexOf("AI")
            if (i >= 0) {
                setSpan(StyleSpan(Typeface.BOLD), i, i + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                val primary = MaterialColors.getColor(titleEditText, com.google.android.material.R.attr.colorOnPrimary)
                setSpan(ForegroundColorSpan(primary), i, i + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        titleEditText.hint = spannable
        titleEditText.setSingleLine(true)
        titleEditText.imeOptions = EditorInfo.IME_ACTION_NEXT
        titleEditText.setOnEditorActionListener { _, actionId, event ->
            val enter = event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN
            if (actionId == EditorInfo.IME_ACTION_NEXT || enter) { noteEditText.requestFocus(); true } else false
        }
        titleEditText.filters = arrayOf(InputFilter.LengthFilter(60))
        titleEditText.addTextChangedListener(object : TextWatcher {
            var internal = false
            override fun afterTextChanged(s: Editable?) {
                if (internal) return
                val words = s?.toString()?.trim()?.split(Regex("\\s+")) ?: return
                if (words.size > 8) {
                    val newText = words.take(8).joinToString(" ")
                    internal = true
                    titleEditText.setText(newText)
                    titleEditText.post { runCatching { titleEditText.setSelection(titleEditText.text?.length ?: 0) } }
                    internal = false
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Save
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_save).setOnClickListener {
            val noteText  = noteEditText.text?.toString().orEmpty()
            val titleText = titleEditText.text?.toString()?.trim().orEmpty()

            if (noteText.isNotEmpty()) {
                val images = imagesAdapter.getAll()
                val result = Intent().apply {
                    putExtra("NEW_NOTE", noteText)
                    putExtra("NEW_NOTE_TITLE", titleText)
                    putStringArrayListExtra("NEW_NOTE_IMAGES", ArrayList(images.map { it.toString() }))
                    putExtra("NEW_NOTE_WANTS_REMINDER", rbReminder.isChecked)   // 👈 add this
                }
                setResult(RESULT_OK, result)
                finish()
                overridePendingTransition(
                    androidx.navigation.ui.R.anim.nav_default_enter_anim,
                    androidx.navigation.ui.R.anim.nav_default_exit_anim
                )
            } else {
                noteEditText.error = "Note cannot be empty"
            }
        }


        // System back (Android 13+)
        if (BuildCompat.isAtLeastT()) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT
            ) { onBackPressedDispatcher.onBackPressed() }
        }

        // Add-image button (observer above will auto-toggle card + counter)
        findViewById<View>(R.id.ll_add_image).setOnClickListener {
            pickImagesLauncher.launch(arrayOf("image/*"))
        }

        // --- Info row + help + smart dot (refactored) ---
        val infoRow  = findViewById<View>(R.id.info_Note)
        val helpIcon = findViewById<View>(R.id.HelpButton)
        val dotInfo  = findViewById<View>(R.id.dot_info)

// a11y + tooltip (falls back if you don't add the strings yet)
        dotInfo.contentDescription = runCatching { getString(R.string.a11y_tip_available) }.getOrElse { "Tip available" }
        androidx.appcompat.widget.TooltipCompat.setTooltipText(
            infoRow,
            runCatching { getString(R.string.tip_add_title) }.getOrElse { "Add a title or let us suggest one" }
        )

        // show/hide the dot only when useful and not already "seen"
        fun refreshInfoDot() {
            val seen = getSharedPreferences("hints", MODE_PRIVATE)
                .getBoolean("seen_title_tip", false)
            val show = titleEditText.text.isNullOrBlank() && (noteEditText.text?.length ?: 0) >= 16 && !seen
            dotInfo.visibility = if (show) View.VISIBLE else View.GONE
        }

// click → mark tip as seen, hide dot, open help
        val infoClick = View.OnClickListener {
            getSharedPreferences("hints", MODE_PRIVATE)
                .edit { putBoolean("seen_title_tip", true) }
            dotInfo.visibility = View.GONE
            showAddNoteHelpDialog()
        }
        infoRow.setOnClickListener(infoClick)
        helpIcon.setOnClickListener { infoRow.performClick() } // forward icon tap

// update dot as user types
                titleEditText.doAfterTextChanged { refreshInfoDot() }
        noteEditText.doAfterTextChanged  { refreshInfoDot() }
        refreshInfoDot()


        // Keep your existing titleEditText watcher as-is; add this tiny one too:
        titleEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { refreshInfoDot() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        noteEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { refreshInfoDot() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

    }


    companion object {
        fun newIntent(context: Context): Intent = Intent(context, AddNoteActivity::class.java)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                finish()
                overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }



    // Only add to the adapter once; dedupe before inserting.
    private val pickImagesLauncher =
        registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.OpenMultipleDocuments()
        ) { uris ->
            if (uris.isEmpty()) return@registerForActivityResult

            uris.forEach { uri ->
                try {
                    contentResolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: SecurityException) { /* already granted */ }
            }

            val existing = imagesAdapter.getAll().toSet()
            val newOnes = uris.filter { it !in existing }
            if (newOnes.isNotEmpty()) {
                imagesAdapter.addAll(newOnes) // adapter will notify range inserted
            }
        }

    private fun adjustStatusBarIcons() {
        val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        when (nightModeFlags) {
            Configuration.UI_MODE_NIGHT_YES -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    window.insetsController?.setSystemBarsAppearance(
                        0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    )
                } else {
                    @Suppress("DEPRECATION")
                    window.decorView.systemUiVisibility = 0
                }
            }
            Configuration.UI_MODE_NIGHT_NO -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
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

    private fun showAddNoteHelpDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_add_note_help, null, false)
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(view)
            .create()

        // reuse fields from this screen
        val titleEt = findViewById<TextInputEditText>(R.id.et_title)
        val noteEt  = findViewById<TextInputEditText>(R.id.et_note)

        view.findViewById<View>(R.id.btnHelpAddImage).setOnClickListener {
            findViewById<View>(R.id.ll_add_image)?.performClick()
            dialog.dismiss()
        }

        view.findViewById<View>(R.id.btnHelpAutoTitle).setOnClickListener {
            val text = noteEt.text?.toString().orEmpty()
            if (text.isBlank()) {
                Toast.makeText(this, "Type some note text first", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                return@setOnClickListener
            }

            // 🧠 Ask adapter's AI helper; fallback to local generator
            NotesAdapter.suggestTitle(this, text) { aiTitle ->
                val chosen = aiTitle.takeIf { it.isNotBlank() && it != "📄 Unclassified" }
                    ?: generateSmartTitle(text)

                titleEt.setText(chosen)
                // ✅ safe cursor placement
                titleEt.post {
                    val len = titleEt.text?.length ?: 0
                    try { titleEt.setSelection(len) } catch (_: Throwable) {}
                }

                Toast.makeText(this, "Title set", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }

        view.findViewById<View>(R.id.btnHelpClose).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    /** tiny offline title helper (paste in the activity if you don't already have one) */
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

}
