package com.flights.studio

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

class ImageViewerActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_URIS = "extra_uris"
        private const val EXTRA_INDEX = "extra_index"

        fun start(context: Context, uris: ArrayList<Uri>, index: Int = 0) {
            context.startActivity(
                Intent(context, ImageViewerActivity::class.java).apply {
                    putParcelableArrayListExtra(EXTRA_URIS, uris)
                    putExtra(EXTRA_INDEX, index)
                }
            )
        }
    }

    private lateinit var viewPager: ViewPager2
    private lateinit var tvCurrent: TextView
    private lateinit var tvTotal: TextView
    private lateinit var topBar: View

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_image_preview)
        findViewById<AppCompatImageView>(R.id.btnBack)?.setOnClickListener {
            onBackPressedDispatcher.onBackPressed() // or just finish()
        }

        val uris: List<Uri> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra(EXTRA_URIS, Uri::class.java) ?: emptyList()
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra(EXTRA_URIS) ?: emptyList()
        }
        val startIndex = intent.getIntExtra(EXTRA_INDEX, 0)
            .coerceIn(0, (uris.size - 1).coerceAtLeast(0))

        if (uris.isEmpty()) { finish(); return }

        viewPager = findViewById(R.id.viewPager)
        tvCurrent = findViewById(R.id.tvCurrent)
        tvTotal   = findViewById(R.id.tvSlashTotal)
        topBar    = findViewById(R.id.topBar)

        // Adapter that uses PhotoView/TouchImageView inside item_fullscreen_pager_image.xml
        viewPager.adapter = ImagePagerAdapter(uris) {
            // optional: toggle chrome on single tap
            topBar.visibility = if (topBar.isVisible) View.GONE else View.VISIBLE
        }
        viewPager.offscreenPageLimit = 1
        viewPager.isUserInputEnabled = true

        (viewPager.getChildAt(0) as? RecyclerView)?.apply {
            overScrollMode = RecyclerView.OVER_SCROLL_NEVER
            isNestedScrollingEnabled = false
            itemAnimator = null
        }
        ViewCompat.setNestedScrollingEnabled(viewPager, false)

        // Set initial counters using string resources with placeholders (no concatenation)
        viewPager.setCurrentItem(startIndex, false)
        tvCurrent.text = getString(R.string.photo_counter_current, startIndex + 1)
        tvTotal.text   = getString(R.string.photo_counter_total, uris.size)

        // a11y announcement on the pill container
        findViewById<View>(R.id.counterPill)?.contentDescription =
            getString(R.string.photo_counter_a11y, startIndex + 1, uris.size)

        // Update current index as pages change
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                tvCurrent.text = getString(R.string.photo_counter_current, position + 1)
                findViewById<View>(R.id.counterPill)?.contentDescription =
                    getString(R.string.photo_counter_a11y, position + 1, uris.size)
            }
        })

        findViewById<AppCompatImageView>(R.id.btnClose)?.setOnClickListener { finish() }
    }
}
