package com.flights.studio

import android.animation.Animator
import android.animation.ValueAnimator
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import com.github.chrisbanes.photoview.PhotoView
import com.google.android.material.card.MaterialCardView

class FullScreenHelper {
    private var isAnimating = false // Prevent spam clicks

    // Call this method when you return to the SplashActivity to reset the height
    fun resetToDefaultState(cardView: MaterialCardView, photoView: PhotoView) {
        val parent = cardView.parent as? ViewGroup ?: return

        cardView.post {
            val parentHeight = parent.height
            val halfScreenHeight = (parentHeight * 0.50).toInt() // 50% of screen

            val params = cardView.layoutParams as ConstraintLayout.LayoutParams
            val imgParams = photoView.layoutParams

            // Reset to 50% height and CENTER scale
            params.height = halfScreenHeight
            imgParams.height = halfScreenHeight
            cardView.layoutParams = params
            photoView.layoutParams = imgParams
            photoView.scaleType = ImageView.ScaleType.CENTER

            cardView.requestLayout()
        }
    }

    fun toggleFullScreen(
        cardView: MaterialCardView,
        photoView: PhotoView,
        isFullScreen: Boolean,
        onAnimationEnd: (() -> Unit)? = null // Callback after animation
    ): Boolean {
        if (isAnimating) return isFullScreen // Block multiple clicks

        val parent = cardView.parent as? ViewGroup ?: return isFullScreen

        cardView.post {
            val parentHeight = parent.height
            val halfScreenHeight = (parentHeight * 0.50).toInt() // 50% of screen
            val fullScreenHeight = parentHeight // 100% of screen

            val startHeight = if (cardView.height == 0) halfScreenHeight else cardView.height
            val endHeight = if (isFullScreen) halfScreenHeight else fullScreenHeight // Toggle between 50% & 100%

            val params = cardView.layoutParams as ConstraintLayout.LayoutParams
            val imgParams = photoView.layoutParams

            // Set scaleType only when expanding (avoid unwanted resets)
            if (!isFullScreen && photoView.scaleType != ImageView.ScaleType.CENTER_CROP) {
                photoView.scaleType = ImageView.ScaleType.CENTER_CROP
            }

            val heightAnimator = ValueAnimator.ofInt(startHeight, endHeight).apply {
                duration = 350
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener { valueAnimator ->
                    val animatedHeight = valueAnimator.animatedValue as Int
                    params.height = animatedHeight
                    imgParams.height = animatedHeight
                    cardView.layoutParams = params
                    photoView.layoutParams = imgParams
                    cardView.requestLayout()
                }
                addListener(object : Animator.AnimatorListener {
                    override fun onAnimationStart(animation: Animator) {
                        isAnimating = true
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        isAnimating = false
                        onAnimationEnd?.invoke() // Trigger optional callback after animation

                        // Apply CENTER only if collapsing (to avoid zoom reset issue)
                        if (isFullScreen && photoView.scaleType != ImageView.ScaleType.CENTER) {
                            photoView.post { photoView.scaleType = ImageView.ScaleType.CENTER }
                        }
                    }

                    override fun onAnimationCancel(animation: Animator) {
                        isAnimating = false
                    }

                    override fun onAnimationRepeat(animation: Animator) {}
                })
            }

            heightAnimator.start()
        }

        return !isFullScreen
    }
}
