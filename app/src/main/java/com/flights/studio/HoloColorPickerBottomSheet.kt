package com.flights.studio

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.graphics.ColorUtils
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class HoloColorPickerBottomSheet(
    private val holoColors: Map<String, Int>, // ✅ Pass color list from adapter
    private val onColorSelected: (Int) -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var listView: ListView
    private var glowDrawable: MultipleGlowDrawable? = null

    private lateinit var dynamicColorPreview: AppCompatImageView // ✅ Change View to ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.holo_color_picker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listView = view.findViewById(R.id.holo_color_list)
        dynamicColorPreview = view.findViewById(R.id.dynamic_color_preview) // ImageView for glow effect

        // Define your base color; here blue with full opacity is used as an example.
        val baseColor = ColorUtils.setAlphaComponent(0xFF0000FF.toInt(), 255)

        // Apply the glow effect with your chosen color.
        // Note: We now pass both requireContext() and baseColor.
        glowDrawable = createInfiniteSlidingGlow(requireContext(), baseColor)

        // Set the glow effect on the preview ImageView.
        dynamicColorPreview.setImageDrawable(glowDrawable)

        // Start the glow animation.
        glowDrawable?.startAnimation()

        setupListView()
    }


    private fun setupListView() {
        val colorsArray = holoColors.values.toList()
        val namesArray = holoColors.keys.toList()

        val adapter = object : ArrayAdapter<String>(
            requireContext(),
            R.layout.list_item_color,
            R.id.color_name,
            namesArray
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val colorPreview = view.findViewById<View>(R.id.color_preview)

                val color = colorsArray[position]
                view.post {
                    if (namesArray[position].isEmpty()) { // ✅ **If it's a placeholder, hide it**
                        colorPreview.visibility = View.INVISIBLE
                        view.visibility = View.GONE
                        view.layoutParams = ViewGroup.LayoutParams(0, 0) // **Make it disappear**
                    } else {
                        colorPreview.visibility = View.VISIBLE
                        colorPreview.background = toGlowDrawable(color, colorPreview)
                        view.background = createListItemBackground(color)
                    }
                }
                return view
            }
        }

        listView.adapter = adapter

        // ✅ Detect which item is at the top and update dynamic color preview
        listView.setOnScrollListener(object : AbsListView.OnScrollListener {
            override fun onScrollStateChanged(view: AbsListView?, scrollState: Int) {}

            override fun onScroll(view: AbsListView?, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
                if (firstVisibleItem in colorsArray.indices && namesArray[firstVisibleItem].isNotEmpty()) {
                    // ✅ Update color **only** for non-empty items
                    val newColor = colorsArray[firstVisibleItem]
                    dynamicColorPreview.setImageDrawable(toGlowDrawable(newColor, dynamicColorPreview))
                }
            }
        })

        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedColor = colorsArray[position]
            val selectedName = namesArray[position]

            if (selectedName.startsWith("Empty color")) return@setOnItemClickListener // ✅ Ignore placeholders

            if (selectedName == "None / Remove color") {
                onColorSelected(Color.TRANSPARENT) // ✅ Clear color
                dynamicColorPreview.setImageDrawable(null) // ✅ Remove glow and color preview
            } else {
                onColorSelected(selectedColor)
                dynamicColorPreview.setImageDrawable(toGlowDrawable(selectedColor, dynamicColorPreview))
            }

            dismiss()
        }


    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)

                // ✅ Fix White Corners Issue
                it.setBackgroundResource(R.drawable.bottom_sheet_rounded)

                // 🚀 Ensure Half-Screen Expansion
                val screenHeight = requireContext().resources.displayMetrics.heightPixels
                val halfScreenHeight = (screenHeight * 0.7).toInt()

                behavior.peekHeight = halfScreenHeight
                behavior.maxHeight = halfScreenHeight
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.isDraggable = false

                // ✅ **Smooth Slide Up (Elastic Feel)**
                it.translationY = screenHeight.toFloat() // **Start off-screen**
                it.animate()
                    .translationY(0f) // **Move to normal position**
                    .setDuration(450)
                    .setInterpolator(android.view.animation.OvershootInterpolator(1.3f))
                    .withEndAction {
                        // ✅ **Right after opening, add gentle up-down shake**
                        it.animate()
                            .translationYBy(-12f).setDuration(80).withEndAction {
                                it.animate().translationYBy(24f).setDuration(80).withEndAction {
                                    it.animate().translationYBy(-18f).setDuration(80).withEndAction {
                                        it.animate().translationYBy(9f).setDuration(80).start()
                                    }.start()
                                }.start()
                            }.start()
                    }
                    .start()

                behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                            behavior.state = BottomSheetBehavior.STATE_EXPANDED // Prevent Full Expand
                        }
                    }

                    override fun onSlide(bottomSheet: View, slideOffset: Float) {}
                })
            }
        }
        return dialog
    }

    companion object {
        // ✅ Function to apply a glowing effect to the color preview (Rounded + Glow)
        fun toGlowDrawable(color: Int, view: View): Drawable {
            val baseColor = color
            val glowColor = ColorUtils.setAlphaComponent(baseColor, 180) // **More visible glow edges**

            // **Increase glow radius** for better spread
            val coreSize = view.width.coerceAtMost(view.height) // Keeps it circular
            val glowSize = coreSize + 55 // **Extended glow**

            // **Solid Core Color (Main Circle)**
            val coreDrawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(baseColor)
                setSize(coreSize, coreSize)
            }

            // **Glow Effect (Smooth & Stronger on Sides)**
            val glowDrawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                gradientType = GradientDrawable.RADIAL_GRADIENT
                setGradientCenter(0.5f, 0.5f) // Ensure a **perfectly round** glow
                gradientRadius = glowSize / 2f // **More natural fade**

                // ✅ **Fix: More visible glow around the edges**
                colors = intArrayOf(
                    baseColor, // **Full solid color in center**
                    glowColor, // **Soft glow to make edges more visible**
                    ColorUtils.setAlphaComponent(baseColor, 50) // **Smoother fade-out**
                )

                setSize(glowSize, glowSize)
            }

            // **Stack Glow Behind Core**
            val layerDrawable = LayerDrawable(arrayOf(glowDrawable, coreDrawable))

            // **Center the core inside the glow**
            val inset = (glowSize - coreSize) / 2
            layerDrawable.setLayerInset(1, inset, inset, inset, inset)

            return layerDrawable
        }

        // ✅ Function to create a soft gradient background for the list item
        fun createListItemBackground(color: Int): Drawable {
            val glowColor = ColorUtils.setAlphaComponent(color, 220)  // 50% opacity
            val edgeColor = ColorUtils.setAlphaComponent(color, 0)   // Fully transparent at edges

            return GradientDrawable(GradientDrawable.Orientation.BL_TR, intArrayOf(
                glowColor, // Slightly visible at the center
                edgeColor  // Transparent at edges
            )).apply {
                shape = GradientDrawable.RECTANGLE
                gradientType = GradientDrawable.RADIAL_GRADIENT
                gradientRadius = 1000.coerceAtLeast(1000).toFloat() // Large radius for a smooth glow
                setGradientCenter(0.5f, 0.5f) // Center the gradient
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        // Stop the glow animation to prevent memory leaks.
        glowDrawable?.stopAnimation()
    }
}
