package com.flights.studio

import android.view.ViewGroup
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Guideline
import com.google.android.material.card.MaterialCardView
import com.ortiz.touchview.TouchImageView

class FullScreenHelper {


    fun resetToDefaultState(
        cardView: MaterialCardView,
        photoView: TouchImageView,
        errorImageView: ImageView
    ) {
        val root = cardView.parent as? ConstraintLayout ?: return
        val guideline = root.findViewById<Guideline>(R.id.guideline50) ?: return

        (cardView.layoutParams as? ConstraintLayout.LayoutParams)?.let { lp ->
            lp.height = 0
            lp.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            lp.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
            lp.bottomToTop = guideline.id
            cardView.layoutParams = lp
            cardView.requestLayout()
        }

        photoView.layoutParams = photoView.layoutParams.apply {
            height = ViewGroup.LayoutParams.MATCH_PARENT
        }
        errorImageView.layoutParams = errorImageView.layoutParams.apply {
            height = ViewGroup.LayoutParams.MATCH_PARENT
        }
        photoView.scaleType = ImageView.ScaleType.CENTER_CROP
        errorImageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
    }



}
