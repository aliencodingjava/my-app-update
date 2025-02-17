package com.flights.studio

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.Intent
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
import android.widget.LinearLayout
import com.flights.studio.databinding.FragmentMyBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.card.MaterialCardView

class CustomBottomSheetFooter : BottomSheetDialogFragment() {

    private var _binding: FragmentMyBottomSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyBottomSheetBinding.inflate(inflater, container, false)
        setupBottomSheet()
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

                // Ensure the exact refresh animation logic is executed
                if (requireActivity() is SplashActivity) {
                    (requireActivity() as SplashActivity).triggerRefreshImageAnimation()
                }
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
        if (destination == SplashActivity::class.java) {
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
            if (destination == SplashActivity::class.java) {
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

    private inner class BottomSheetCallbackListener :
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
            80f,
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
            includedLayout.findViewById<MaterialCardView>(R.id.cardView1),
            includedLayout.findViewById<MaterialCardView>(R.id.cardView2),
            includedLayout.findViewById<MaterialCardView>(R.id.cardView3),
            includedLayout.findViewById<MaterialCardView>(R.id.cardView4),
            includedLayout.findViewById<MaterialCardView>(R.id.cardView5),
            includedLayout.findViewById<MaterialCardView>(R.id.cardView6),
            includedLayout.findViewById<MaterialCardView>(R.id.cardView7),
            includedLayout.findViewById<MaterialCardView>(R.id.cardView8),
            includedLayout.findViewById<MaterialCardView>(R.id.cardView9)
        )

        cardViews.forEach { cardView ->
            cardView.setOnClickListener {
                val moreInfoLayout = cardView.findViewById<LinearLayout>(R.id.moreInfoLayout)
                val card2MoreInfoLayout = cardView.findViewById<LinearLayout>(R.id.card2moreInfoLayout)
                val card3MoreInfoLayout = cardView.findViewById<LinearLayout>(R.id.card3moreInfoLayout)
                val card4MoreInfoLayout = cardView.findViewById<LinearLayout>(R.id.card4moreInfoLayout)



                // Toggle between expanded and collapsed states
                val currentHeight = cardView.layoutParams.height
                val newHeight = if (currentHeight == dpToPxForCard(70)) {
                    dpToPxForCard(300) // Expand to 300dp
                } else {
                    dpToPxForCard(70) // Collapse back to 70dp
                }

                // Animate the height change
                val anim = ValueAnimator.ofInt(currentHeight, newHeight)
                anim.addUpdateListener { valueAnimator ->
                    val params = cardView.layoutParams
                    params.height = valueAnimator.animatedValue as Int
                    cardView.layoutParams = params
                }
                anim.duration = 300 // Duration of the animation
                anim.start()

                // Show or hide the extra information (more info layout)
                if (moreInfoLayout != null) {
                    if (moreInfoLayout.visibility == View.GONE) {
                        moreInfoLayout.visibility = View.VISIBLE
                    } else {
                        moreInfoLayout.visibility = View.GONE
                    }
                }

                if (card2MoreInfoLayout != null) {
                    if (card2MoreInfoLayout.visibility == View.GONE) {
                        card2MoreInfoLayout.visibility = View.VISIBLE
                    } else {
                        card2MoreInfoLayout.visibility = View.GONE
                    }
                }
                if (card3MoreInfoLayout != null) {
                    if (card3MoreInfoLayout.visibility == View.GONE) {
                        card3MoreInfoLayout.visibility = View.VISIBLE
                    } else {
                        card3MoreInfoLayout.visibility = View.GONE
                    }
                }
                if (card4MoreInfoLayout != null) {
                    if (card4MoreInfoLayout.visibility == View.GONE) {
                        card4MoreInfoLayout.visibility = View.VISIBLE
                    } else {
                        card4MoreInfoLayout.visibility = View.GONE
                    }
                }
            }

            setZoomEffect(cardView) // Keep the zoom effect on the card view
        }
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


