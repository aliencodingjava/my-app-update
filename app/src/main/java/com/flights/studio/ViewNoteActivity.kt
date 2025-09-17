@file:Suppress("DEPRECATION")

package com.flights.studio

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.flights.studio.databinding.ActivityViewNoteBinding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.carousel.CarouselLayoutManager
import com.google.android.material.carousel.CarouselSnapHelper
import com.google.android.material.carousel.HeroCarouselStrategy
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ViewNoteActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NOTE = "NOTE"
        const val EXTRA_POSITION = "NOTE_POSITION"
        const val EXTRA_TITLE = "NOTE_TITLE"

        private const val TAG = "ViewNoteActivity"
        private const val CLICK_DELAY = 500L

        fun newIntent(context: Context, note: String, position: Int, title: String?): Intent {
            return Intent(context, ViewNoteActivity::class.java).apply {
                putExtra(EXTRA_NOTE, note)
                putExtra(EXTRA_POSITION, position)
                putExtra(EXTRA_TITLE, title)
            }
        }
    }

    private lateinit var binding: ActivityViewNoteBinding
    private lateinit var editNoteLauncher: ActivityResultLauncher<Intent>
    private var noteIsModified = false
    private lateinit var rv: RecyclerView

    private var snapHelper: CarouselSnapHelper? = null
    private var lastClickedTN: String? = null
    private var lastClickedIndex: Int = RecyclerView.NO_POSITION
    private var imageDialog: Dialog? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityViewNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Smooth shared-element transitions
        setupSharedElementTransitions()

        adjustStatusBarIcons()
        setupToolbar(binding.toolbar, R.drawable.layered_arrow)

        // Read extras (support legacy keys too)
        val note: String = intent.getStringExtra(EXTRA_NOTE)
            ?: intent.getStringExtra("extra_note")
            ?: ""
        val position: Int = intent.getIntExtra(
            EXTRA_POSITION, intent.getIntExtra("extra_position", -1)
        )
        val title: String? = intent.getStringExtra(EXTRA_TITLE)
            ?: intent.getStringExtra("extra_title")

        if (note.isEmpty() || position == -1) {
            Log.w(TAG, "Invalid note or position: note=$note, position=$position")
            Toast.makeText(this, getString(R.string.error_loading_note), Toast.LENGTH_SHORT).show()
            finish()
            applyTransition()
            return
        }

        // Text + optional title
        binding.tvNoteContent.text = note
        if (!title.isNullOrBlank()) binding.toolbar.title = title

        // Setup images carousel
        setupImagesCarousel(note)

        // Setup edit result launcher
        setupEditLauncher()

        // Optional edit icon inside toolbar
        setupEditIcon(note, position, title)


    }




    private fun setupSharedElementTransitions() {
        val exitSet = android.transition.TransitionSet()
            .addTransition(android.transition.ChangeBounds())
            .addTransition(android.transition.ChangeTransform())
            .addTransition(android.transition.ChangeImageTransform())
            .apply {
                duration = 600L
                interpolator = androidx.interpolator.view.animation.FastOutSlowInInterpolator()
                setPathMotion(android.transition.ArcMotion())
            }

        window.sharedElementEnterTransition = exitSet
        window.sharedElementExitTransition = exitSet
        window.sharedElementReturnTransition = exitSet
        window.sharedElementReenterTransition = exitSet

        window.exitTransition = android.transition.Fade().apply { duration = 180L }
        window.enterTransition = android.transition.Fade().apply { duration = 180L }

        // Reattach snap only after the shared-element reenter finishes
        (window.sharedElementReenterTransition ?: return).addListener(
            object : android.transition.Transition.TransitionListener {
                override fun onTransitionStart(t: android.transition.Transition) {}
                override fun onTransitionCancel(t: android.transition.Transition) {}
                override fun onTransitionPause(t: android.transition.Transition) {}
                override fun onTransitionResume(t: android.transition.Transition) {}
                override fun onTransitionEnd(t: android.transition.Transition) {
                    // Make sure the centered child stays put; then reattach snapping
                    findContainerByTN(lastClickedTN)?.let { centerChildExactly(it) }
                    snapHelper?.attachToRecyclerView(rv)
                }
            }
        )
    }

    private fun setupImagesCarousel(note: String) {
        rv = findViewById(R.id.rv_images)
        val uris = NoteMediaStore.getUris(this, note)
        if (uris.isEmpty()) { rv.isVisible = false; snapHelper = null; return }
        rv.isVisible = true

        val layoutManager = CarouselLayoutManager(HeroCarouselStrategy())
        rv.layoutManager = layoutManager
        rv.clipToPadding = false
        rv.clipChildren = false
        rv.setHasFixedSize(true)

        snapHelper = CarouselSnapHelper().also { it.attachToRecyclerView(rv) }

        // 🔴 Remove the side padding you previously added
        rv.setPadding(0, rv.paddingTop, 0, rv.paddingBottom)

        // ✅ Add spacing between items but NOT before the first item
        rv.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: android.graphics.Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State,
            ) {
                val pos = parent.getChildAdapterPosition(view)
                val s = (6 * view.resources.displayMetrics.density).toInt() // 6dp
                outRect.left = if (pos == 0) 0 else s
                outRect.right = 0
            }
        })

        rv.adapter = SimpleImageAdapter(uris, ::showImageDialog)
    }


    class SimpleImageAdapter(
        private val items: List<Uri>,
        private val onImageClick: (List<Uri>, Int) -> Unit,  // ✅ sends full list and index
    ) : RecyclerView.Adapter<SimpleImageAdapter.VH>() {

        class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val image: ImageView = itemView.findViewById(R.id.carousel_image_view)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_carousel_image, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val uri = items[position]
            Glide.with(holder.image)
                .load(uri)
                .centerCrop()
                .into(holder.image)

            holder.image.setOnClickListener {
                onImageClick(items, holder.adapterPosition)
            }
        }


        override fun getItemCount(): Int = items.size
    }


    private fun findContainerByTN(tn: String?): View? {
        if (tn.isNullOrEmpty()) return null
        for (i in 0 until rv.childCount) {
            val c = rv.getChildAt(i).findViewById<View>(R.id.carousel_item_container)
            if (c != null && tn == c.transitionName) return c
        }
        return null
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        imageDialog = null
        super.onDestroy()
    }

    private fun showImageDialog(uris: List<Uri>, startIndex: Int) {
        if (uris.isEmpty()) return
        val urls = uris.map { it.toString() }               // Compose screen takes strings
        startActivity(
            ViewImageComposeActivity.intent(
                this,
                urls = urls,
                startIndex = startIndex
            )
        )
        // Optional: keep your fade if you like
        overridePendingTransition(R.anim.m3_motion_fade_enter, R.anim.m3_motion_fade_exit)
    }



    private fun centerChildExactly(child: View) {
        // center of RV content area
        val rvCenter = (rv.paddingLeft + rv.width - rv.paddingRight) / 2
        // child center in RV coordinates
        val childCenter = (child.left + child.right) / 2
        val dx = childCenter - rvCenter
        if (dx != 0) rv.scrollBy(dx, 0)
    }


    override fun onActivityReenter(resultCode: Int, data: Intent?) {
        super.onActivityReenter(resultCode, data)

        postponeEnterTransition()
        rv.stopScroll()

        // Freeze everything and detach snap so it can’t “help”
        rv.suppressLayout(true)
        snapHelper?.attachToRecyclerView(null)

        if (lastClickedIndex != RecyclerView.NO_POSITION) {
            rv.scrollToPosition(lastClickedIndex) // ensure it’s visible
        }

        rv.viewTreeObserver.addOnPreDrawListener(object : android.view.ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                rv.viewTreeObserver.removeOnPreDrawListener(this)

                // First precise center before starting the return
                findContainerByTN(lastClickedTN)?.let { centerChildExactly(it) }

                startPostponedEnterTransition()

                // Thaw layout right after we start animating (still no snap yet)
                rv.post { rv.suppressLayout(false) }
                return true
            }
        })
    }





    private fun setupEditLauncher() {
        editNoteLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                val updatedNote = data?.getStringExtra("UPDATED_NOTE")
                val updatedTitle = data?.getStringExtra("UPDATED_TITLE")
                val updatedPos = data?.getIntExtra("NOTE_POSITION", -1) ?: -1

                if (!updatedNote.isNullOrEmpty() && updatedPos != -1) {
                    noteIsModified = true
                    setResult(
                        RESULT_OK,
                        Intent().apply {
                            putExtra("UPDATED_NOTE", updatedNote)
                            putExtra("UPDATED_TITLE", updatedTitle)
                            putExtra("NOTE_POSITION", updatedPos)
                        }
                    )
                    finish()
                    applyTransition()
                }
            }
        }
    }

    private fun setupEditIcon(note: String, position: Int, title: String?) {
        val editIcon = binding.toolbar.findViewById<ImageView>(R.id.expandCollapseIcon)
        editIcon?.setOnClickListener { v ->
            preventDoubleClick(v)
            Log.d(TAG, "Launching edit activity for position: $position")
            editNoteLauncher.launch(EditNoteActivity.newIntent(this, note, position, title))
            applyTransition()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                if (noteIsModified) showExitConfirmationDialog()
                else {
                    finish()
                    applyTransition()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(this)
            .setMessage(getString(R.string.confirm_exit_message))
            .setPositiveButton(getString(R.string.exit)) { _, _ ->
                finish()
                applyTransition()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
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

    private fun setupToolbar(toolbar: MaterialToolbar, iconResId: Int) {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(
                AppCompatResources.getDrawable(this@ViewNoteActivity, iconResId)
            )
        }
    }

    private fun applyTransition() {
        overridePendingTransition(R.anim.m3_motion_fade_enter, R.anim.m3_motion_fade_exit)
    }

    private fun preventDoubleClick(view: View) {
        view.isEnabled = false
        lifecycleScope.launch {
            delay(CLICK_DELAY)
            view.isEnabled = true
        }
    }
}