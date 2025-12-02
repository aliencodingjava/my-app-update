package com.flights.studio

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.ScaleAnimation
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.isGone
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.flights.studio.databinding.FragmentMyBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.card.MaterialCardView
import java.io.File
import java.io.FileOutputStream

class CustomBottomSheetFooter : BottomSheetDialogFragment() {

    private var _binding: FragmentMyBottomSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentMyBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupFabListeners()
        setupCardZoomEffects() // Ensure this function is called


    }


    private fun setupFabListeners() {
        binding.apply {
            fab1.setOnClickListener {
                Log.d("CustomBottomSheetFooter", "fab1 clicked")

                // TODO: hook this up to Compose refresh later
                // Previously:
                // (requireActivity() as SplashActivity).triggerRefreshImageAnimation()
            }

            fab2.setOnClickListener { expandBottomSheetAndLoadCarousel() }
            fab3.setOnClickListener { navigateToActivity(Contact::class.java) }
            fab4.setOnClickListener { navigateToActivity(SettingsActivity::class.java) }
        }
    }



    private fun navigateToActivity(destination: Class<*>) {
        val intent = Intent(requireContext(), destination)

        // Example: If you ever want to navigate to the SplashActivity (or any other activity that should clear the back stack),
        // add the following flags. You can extend this condition if needed.
        if (destination == MainActivity::class.java) {
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val options = ActivityOptions.makeCustomAnimation(
            requireContext(),
            R.anim.m3_motion_fade_enter,
            R.anim.slide_out_left
        )

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(intent, options.toBundle())
            // If navigating to SplashActivity, finish the current activity so that it is removed from the back stack.
            if (destination == MainActivity::class.java) {
                requireActivity().finish()
            }
        }, 0)
    }

    private fun expandBottomSheetAndLoadCarousel() {
        dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let {
            with(BottomSheetBehavior.from(it)) {
                state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        childFragmentManager.beginTransaction()
            .replace(R.id.carouselContainer, PhotoCarouselFragment())
            .commit()
        binding.carouselContainer.visibility = View.VISIBLE
    }


    private fun setupBottomSheet() {
        if (isBottomSheetShown) return
        dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let {
            with(BottomSheetBehavior.from(it)) {
                isDraggable = true
                isHideable = false
                peekHeight = resources.getDimensionPixelSize(R.dimen.peek_height_90dp)
                state = BottomSheetBehavior.STATE_EXPANDED
                addBottomSheetCallback(BottomSheetCallbackListener())
            }
            isBottomSheetShown = true
        }
    }

    private class BottomSheetCallbackListener :
        BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            // Handle state changes if needed
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            // Handle slide events if needed
        }
    }


    override fun onStart() {
        super.onStart()
        setupBottomSheet()

        setupDialogProperties()
    }


    private fun setupDialogProperties() {
        val bottomSheet =
            dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet!!)
        bottomSheetBehavior.apply {
            isHideable = false
            isDraggable = true
            peekHeight = dpToPx()
            isFitToContents = true
            state = BottomSheetBehavior.STATE_COLLAPSED
            skipCollapsed = false
        }

        bottomSheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT


        bottomSheet.requestLayout()

        dialog?.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setDimAmount(0f)
        }
    }

    companion object {
        var isBottomSheetShown: Boolean = false
    }


    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        isBottomSheetShown = false // Reset the flag



    }



    private fun dpToPx(): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            95f,
            resources.displayMetrics
        ).toInt()
    }

    // Method to convert dp to px for CardView height
    private fun dpToPxForCard(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    private fun setupCardZoomEffects() {
        val includedLayout = binding.root.findViewById<View>(R.id.new_layout_grill)

        val cardViews = listOf(
            includedLayout.findViewById(R.id.cardView1),
            includedLayout.findViewById(R.id.cardView2),
            includedLayout.findViewById(R.id.cardView3),
            includedLayout.findViewById(R.id.cardView4),
            includedLayout.findViewById(R.id.cardView5),
            includedLayout.findViewById(R.id.cardView6),
            includedLayout.findViewById(R.id.cardView7),
            includedLayout.findViewById(R.id.cardView8),
            includedLayout.findViewById(R.id.cardView9),
            includedLayout.findViewById(R.id.cardView10),
            includedLayout.findViewById(R.id.cardView11),
            includedLayout.findViewById<MaterialCardView>(R.id.cardView12)
        )

        val imageNames = listOf(
            "airport.jpg",
            "entrance.jpg",
            "frontairport.jpg",
            "thehorse.jpg",
            "front.jpg",
            "indiansleeping1.jpg",
            "inside.jpg",
            "outside.jpg",
            "photojack_1.jpg",
            "photojack_2.jpg",
            "terminalinside.jpg",
            "20221115_153340.jpg"
        )



        val supabaseBaseUrl = "https://gdvhiudodnqdqhkyghsk.supabase.co/storage/v1/object/public/carousel-photos"

        cardViews.forEachIndexed { index, cardView ->
            val imageName = imageNames.getOrNull(index) ?: return@forEachIndexed
            val imageUrl = "$supabaseBaseUrl/$imageName"

            // ✅ THIS IS THE KEY FIX — get the ImageView INSIDE the card
            val imageView = cardView.findViewById<ImageView>(imageViewIds[index])
            loadOrDownloadCardBackground(requireContext(), imageView, imageName, imageUrl)

            cardView.setOnClickListener {
                val moreInfoLayout = cardView.findViewById<LinearLayout>(R.id.moreInfoLayout)
                val card2MoreInfoLayout = cardView.findViewById<LinearLayout>(R.id.card2moreInfoLayout)
                val card3MoreInfoLayout = cardView.findViewById<LinearLayout>(R.id.card3moreInfoLayout)
                val card4MoreInfoLayout = cardView.findViewById<LinearLayout>(R.id.card4moreInfoLayout)

                val currentHeight = cardView.layoutParams.height
                val newHeight = if (currentHeight == dpToPxForCard(70)) dpToPxForCard(300) else dpToPxForCard(70)

                ValueAnimator.ofInt(currentHeight, newHeight).apply {
                    duration = 300
                    addUpdateListener {
                        cardView.layoutParams.height = it.animatedValue as Int
                        cardView.requestLayout()
                    }
                    start()
                }

                fun toggleVisibility(view: View?) {
                    view?.visibility = if (view.isGone) View.VISIBLE else View.GONE
                }

                toggleVisibility(moreInfoLayout)
                toggleVisibility(card2MoreInfoLayout)
                toggleVisibility(card3MoreInfoLayout)
                toggleVisibility(card4MoreInfoLayout)
            }

            setZoomEffect(cardView)
        }
    }
    private val imageViewIds = listOf(
        R.id.cardBackground_card1,
        R.id.cardBackground_card2,
        R.id.cardBackground_card3,
        R.id.cardBackground_card4,
        R.id.cardBackground_card5,
        R.id.cardBackground_card6,
        R.id.cardBackground_card7,
        R.id.cardBackground_card8,
        R.id.cardBackground_card9,
        R.id.cardBackground_card10,
        R.id.cardBackground_card11,
        R.id.cardBackground_card12
    )

    private fun loadOrDownloadCardBackground(context: Context, imageView: ImageView, fileName: String, url: String) {
        val file = File(context.filesDir, fileName)

        if (file.exists()) {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            imageView.setImageBitmap(bitmap)
        } else {
            Glide.with(context)
                .asBitmap()
                .load(url)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        try {
                            // Save original file first
                            FileOutputStream(file).use { out ->
                                resource.compress(Bitmap.CompressFormat.JPEG, 90, out)
                            }

                            // Decode from saved file (so EXIF is preserved)
                            val rotatedBitmap = decodeAndCorrectOrientation(file)
                            imageView.setImageBitmap(rotatedBitmap)

                        } catch (e: Exception) {
                            Log.e("ImageSave", "Error saving or correcting image $fileName", e)
                            imageView.setImageBitmap(resource) // fallback
                        }
                    }


                    override fun onLoadCleared(placeholder: Drawable?) {}
                })
        }
    }

    private fun decodeAndCorrectOrientation(file: File): Bitmap {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        val exif = androidx.exifinterface.media.ExifInterface(file)

        return when (exif.getAttributeInt(
            androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
            androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
        )) {
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
            else -> bitmap
        }
    }

    private fun rotateBitmap(src: Bitmap, angle: Float): Bitmap {
        val matrix = android.graphics.Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
    }



    @SuppressLint("ClickableViewAccessibility")
    private fun setZoomEffect(cardView: MaterialCardView) {
        cardView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Scale down effect
                    val zoomIn = ScaleAnimation(
                        1f, 0.97f, 1f, 0.97f,
                        ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                        ScaleAnimation.RELATIVE_TO_SELF, 0.5f
                    )
                    zoomIn.duration = 100
                    zoomIn.fillAfter = true
                    cardView.startAnimation(zoomIn)
                }

                MotionEvent.ACTION_UP -> {
                    // Scale up effect and perform click
                    val zoomOut = ScaleAnimation(
                        0.97f, 1f, 0.97f, 1f,
                        ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                        ScaleAnimation.RELATIVE_TO_SELF, 0.5f
                    )
                    zoomOut.duration = 100
                    zoomOut.fillAfter = true
                    cardView.startAnimation(zoomOut)

                    // Trigger performClick() to simulate a click event
                    cardView.performClick()
                }

                MotionEvent.ACTION_CANCEL -> {
                    // Scale back to normal if the event is canceled
                    val zoomOut = ScaleAnimation(
                        0.97f, 1f, 0.97f, 1f,
                        ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                        ScaleAnimation.RELATIVE_TO_SELF, 0.5f
                    )
                    zoomOut.duration = 100
                    zoomOut.fillAfter = true
                    cardView.startAnimation(zoomOut)
                }
            }
            true // Indicate that the event was handled
        }
    }


}


