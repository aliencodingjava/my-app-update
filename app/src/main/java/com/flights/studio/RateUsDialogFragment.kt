package com.flights.studio


import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RatingBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.github.lzyzsd.circleprogress.DonutProgress
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import java.util.Locale

fun DonutProgress.setProgressWithAnimation(progress: Float, duration: Int) {
    val animator = ObjectAnimator.ofFloat(this, "progress", this.progress.toFloat(), progress)
    animator.duration = duration.toLong()
    animator.start()
}
class RateUsDialogFragment : BottomSheetDialogFragment() {

    private lateinit var ratingBar: RatingBar
    private lateinit var btnSubmit: Button
    private lateinit var fiveStarProgressBar: ProgressBar
    private lateinit var fourStarProgressBar: ProgressBar
    private lateinit var threeStarProgressBar: ProgressBar
    private lateinit var twoStarProgressBar: ProgressBar
    private lateinit var oneStarProgressBar: ProgressBar
    private lateinit var fiveStarCountTextView: TextView
    private lateinit var fourStarCountTextView: TextView
    private lateinit var threeStarCountTextView: TextView
    private lateinit var twoStarCountTextView: TextView
    private lateinit var oneStarCountTextView: TextView
    private lateinit var submitCountPref: SharedPreferences
    private lateinit var donutProgress: DonutProgress



    // Reference to Firebase database
    private val ratingsRef = FirebaseDatabase.getInstance().getReference("ratings")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_rate_us_dialog, container, false)

    }


    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        view.background = ContextCompat.getDrawable(requireContext(), R.drawable.bottom_sheet_background)

        // ✅ Initialize SharedPreferences first
        submitCountPref = requireActivity().getSharedPreferences("RateUsSubmitCount", Context.MODE_PRIVATE)

        // ✅ Initialize ALL views, including btnSubmit
        initializeViews(view)

        // ✅ Now it’s safe to update the state
        updateSubmitButtonState()

        // ✅ Then fetch data
        fetchAndDisplayRatings()

        val fabSend = view.findViewById<ImageView>(R.id.fabSend)
        fabSend.setOnClickListener {
            dismiss() // ✅ Close the rating dialog
            FeedbackBottomSheet().show(parentFragmentManager, "FeedbackDialog")
        }


        // ✅ Setup feedback button
        view.findViewById<MaterialButton>(R.id.submitButton).setOnClickListener {
            FeedbackBottomSheet().show(childFragmentManager, "FeedbackDialog")
        }

        // ✅ Setup RatingBar
        ratingBar.setOnRatingBarChangeListener { _, rating, _ ->
            val progressValue = (rating / 26f) * 100
            donutProgress.setProgressWithAnimation(progressValue, 1000)
            donutProgress.text = String.format(Locale.US, "%.1f", rating)
        }
        btnSubmit.setOnClickListener {
            val submitCount = submitCountPref.getInt("submitCount", 0)
            val rating = ratingBar.rating.toInt()

            if (rating > 0) {
                if (submitCount < 3) {
                    submitRating(rating)
                    submitCountPref.edit { putInt("submitCount", submitCount + 1) }
                    updateSubmitButtonState()
                } else {
                    updateSubmitButtonState()
                }
            } else {
                Snackbar.make(requireView(), "Please select a rating before submitting.", Snackbar.LENGTH_SHORT).apply {
                    show()
                    requireView().postDelayed({ dismiss() }, 1800)
                }
            }
        }
    }

    // Add this function to disable or enable the RatingBar
    private fun updateRatingBarState() {
        val submitCount = submitCountPref.getInt("submitCount", 0)
        ratingBar.isEnabled = submitCount < 3
    }
    private fun initializeViews(view: View) {
        ratingBar = view.findViewById(R.id.ratingBar)
        btnSubmit = view.findViewById(R.id.submitButton)
        fiveStarProgressBar = view.findViewById(R.id.fiveStarProgressBar)
        fourStarProgressBar = view.findViewById(R.id.fourStarProgressBar)
        threeStarProgressBar = view.findViewById(R.id.threeStarProgressBar)
        twoStarProgressBar = view.findViewById(R.id.twoStarProgressBar)
        oneStarProgressBar = view.findViewById(R.id.oneStarProgressBar)
        fiveStarCountTextView = view.findViewById(R.id.fiveStarCountTextView)
        fourStarCountTextView = view.findViewById(R.id.fourStarCountTextView)
        threeStarCountTextView = view.findViewById(R.id.threeStarCountTextView)
        twoStarCountTextView = view.findViewById(R.id.twoStarCountTextView)
        oneStarCountTextView = view.findViewById(R.id.oneStarCountTextView)
        donutProgress = view.findViewById(R.id.donutProgress)  // DonutProgress initialization

    }

    private fun fetchAndDisplayRatings() {
        ratingsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Safely attempt to cast snapshot.value to a Map<String, Long>
                val ratingsMap = snapshot.value as? Map<*, *>  // Use a general Map type first
                if (ratingsMap != null) {
                    // Ensure the keys and values are of the expected types
                    val validRatingsMap = ratingsMap.filterKeys { it is String }
                        .mapValues { (_, value) -> (value as? Long) ?: 0L }
                        .filterValues { true }

                    // Now safely pass the map to updateUIWithRatings
                    updateUIWithRatings(validRatingsMap)
                } else {
                    Log.e("Firebase", "Invalid data format or no ratings available")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle potential errors
                Log.e("Firebase", "Failed to read value.", error.toException())
            }
        })
    }


    private fun updateUIWithRatings(ratings: Map<Any?, Long>) {
        // Extract rating counts from the map
        val fiveStarCount = (ratings["5"] ?: 0L).toInt()
        val fourStarCount = (ratings["4"] ?: 0L).toInt()
        val threeStarCount = (ratings["3"] ?: 0L).toInt()
        val twoStarCount = (ratings["2"] ?: 0L).toInt()
        val oneStarCount = (ratings["1"] ?: 0L).toInt()

        // Calculate total ratings
        val totalRatings = fiveStarCount + fourStarCount + threeStarCount + twoStarCount + oneStarCount


        // Update TextViews with counts
        fiveStarCountTextView.text = formatRatingCount(fiveStarCount)
        fourStarCountTextView.text = formatRatingCount(fourStarCount)
        threeStarCountTextView.text = formatRatingCount(threeStarCount)
        twoStarCountTextView.text = formatRatingCount(twoStarCount)
        oneStarCountTextView.text = formatRatingCount(oneStarCount)

        Log.d("RateUsDialogFragment", "Total Ratings: $totalRatings")
        Log.d("RateUsDialogFragment", "Five Star Count: $fiveStarCount, Percentage: ${calculatePercentage(fiveStarCount, totalRatings)}")
        Log.d("RateUsDialogFragment", "Four Star Count: $fourStarCount, Percentage: ${calculatePercentage(fourStarCount, totalRatings)}")
        Log.d("RateUsDialogFragment", "Three Star Count: $threeStarCount, Percentage: ${calculatePercentage(threeStarCount, totalRatings)}")
        Log.d("RateUsDialogFragment", "Two Star Count: $twoStarCount, Percentage: ${calculatePercentage(twoStarCount, totalRatings)}")
        Log.d("RateUsDialogFragment", "One Star Count: $oneStarCount, Percentage: ${calculatePercentage(oneStarCount, totalRatings)}")


        if (totalRatings > 0) {
            // Define the maximum expected count for scaling and max progress
            val maxExpectedCount = 200

            // Calculate scale factor based on the maximum expected count
            val scaleFactor = totalRatings.coerceAtMost(maxExpectedCount).toFloat() / maxExpectedCount

            // Update ProgressBars with scaled percentage of each rating type
            fiveStarProgressBar.progress = calculateProgress(fiveStarCount, totalRatings, scaleFactor)
            fourStarProgressBar.progress = calculateProgress(fourStarCount, totalRatings, scaleFactor)
            threeStarProgressBar.progress = calculateProgress(threeStarCount, totalRatings, scaleFactor)
            twoStarProgressBar.progress = calculateProgress(twoStarCount, totalRatings, scaleFactor)
            oneStarProgressBar.progress = calculateProgress(oneStarCount, totalRatings, scaleFactor)


            // Calculate average rating as a weighted average
            val weightedSum = (5 * fiveStarCount + 4 * fourStarCount + 3 * threeStarCount + 2 * twoStarCount + 1 * oneStarCount)
            val averageRating = weightedSum.toFloat() / totalRatings.toFloat()
            val progressValue = (averageRating / 50f) * 100

            // Set the progress of the DonutProgress (scaled to 0-100 range)
            Log.d("RateUsDialogFragment", "Final Progress Value: $progressValue")
            donutProgress.progress = progressValue.coerceIn(0f, 5f) // This ensures it stays between 0 and 5
            donutProgress.text = String.format(Locale.US, "%.1f", averageRating) // Display the average rating


            // Optional: Add animation to the DonutProgress
            donutProgress.setProgressWithAnimation(progressValue, 1000)
        } else {
            // Handle the case where there are no ratings yet
            resetProgressBarsAndDonut()
        }
    }
    private fun formatRatingCount(count: Int): String {
        return count.toString()
    }

    private fun calculatePercentage(count: Int, totalRatings: Int): Float {
        return if (totalRatings > 0) (count.toFloat() / totalRatings.toFloat()) * 100 else 0f
    }

    private fun calculateProgress(count: Int, totalRatings: Int, scaleFactor: Float): Int {
        return ((count.toFloat() / totalRatings) * 100 * scaleFactor).toInt()
    }


    private fun resetProgressBarsAndDonut() {
        // Reset all progress bars and the donut progress
        fiveStarProgressBar.progress = 0
        fourStarProgressBar.progress = 0
        threeStarProgressBar.progress = 0
        twoStarProgressBar.progress = 0
        oneStarProgressBar.progress = 0
    }

    private fun submitRating(rating: Int) {
        val currentRatingRef = ratingsRef.child(rating.toString())
        currentRatingRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val currentCount = currentData.getValue(Long::class.java) ?: 0
                currentData.value = currentCount + 1
                return Transaction.success(currentData)
            }


            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                currentData: DataSnapshot?,
            ) {
                if (committed) {
                    val snackbar = Snackbar.make(
                        requireView(),
                        "Rating submitted. Thank you!",
                        Snackbar.LENGTH_SHORT
                    )
                    snackbar.show()
                    // Schedule the Snackbar to be dismissed after 1 second
                    requireView().postDelayed({ snackbar.dismiss() }, 900)
                } else {
                    val snackbar = Snackbar.make(
                        requireView(),
                        "Failed to submit rating.",
                        Snackbar.LENGTH_SHORT
                    )
                    snackbar.show()
                    // Schedule the Snackbar to be dismissed after 1 second
                    requireView().postDelayed({ snackbar.dismiss() }, 900)


                }
            }

        })
    }
    @SuppressLint("SetTextI18n")
    private fun updateSubmitButtonState() {
        val submitCount = submitCountPref.getInt("submitCount", 0)
        if (submitCount >= 3) {
            btnSubmit.text = "Submitted"
            btnSubmit.isEnabled = false
        } else {
            btnSubmit.text = "Submit"
            btnSubmit.isEnabled = true
        }
        // Also update RatingBar state
        updateRatingBarState()
    }



    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
                behavior.isFitToContents = true // ✅ this is key
                behavior.isDraggable = true

                // ❌ DO NOT force height to match_parent
                // it.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT  ← remove this
                // it.requestLayout()
            }
        }
        return dialog
    }


}
