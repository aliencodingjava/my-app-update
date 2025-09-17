// DraggableCardView.kt
package com.flights.studio

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import com.google.android.material.card.MaterialCardView

class DraggableCardView(context: Context, attrs: AttributeSet) : MaterialCardView(context, attrs) {

    // Variables to hold the maximum screen width and height
    private val screenWidth = resources.displayMetrics.widthPixels
    private val screenHeight = resources.displayMetrics.heightPixels

    // Variables for card view's size and its position
    private var dX = 0f
    private var dY = 0f
    private var isDragging = false

    init {
        // Attach the touch listener to the MaterialCardView
        setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Record initial touch position
                    dX = v.x - event.rawX
                    dY = v.y - event.rawY
                    v.isPressed = true
                    isDragging = false  // Reset dragging flag

                    // Animate zoom-in effect
                    zoomIn(v)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    // Handle the drag action
                    val newX = (event.rawX + dX).coerceIn(0f, screenWidth - v.width.toFloat())
                    val newY = (event.rawY + dY).coerceIn(0f, screenHeight - v.height.toFloat())

                    // Move the card smoothly with animation
                    v.animate()
                        .x(newX)
                        .y(newY)
                        .setDuration(0)
                        .setInterpolator(DecelerateInterpolator())
                        .start()

                    // Set flag if the user is dragging
                    isDragging = true
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.isPressed = false

                    // If not dragging, perform click (tapping the card)
                    if (!isDragging) {
                        v.performClick() // Trigger click event
                    }

                    // Animate zoom-out effect
                    zoomOut(v)
                    true
                }
                else -> false
            }
        }
    }

    // Zoom-in effect: Scale the card up
    private fun zoomIn(v: android.view.View) {
        val scaleX = 1.1f // 10% zoom in
        val scaleY = 1.1f // 10% zoom in
        v.animate()
            .scaleX(scaleX)
            .scaleY(scaleY)
            .setDuration(200)
            .start()
    }

    // Zoom-out effect: Scale the card back to its original size
    private fun zoomOut(v: android.view.View) {
        v.animate()
            .scaleX(1f) // Original size
            .scaleY(1f) // Original size
            .setDuration(200)
            .start()
    }

    // Override performClick to make sure it's called for accessibility and proper click handling
    override fun performClick(): Boolean {
        super.performClick() // Always call the super method for proper handling
        // You can also handle any additional logic for clicks here if needed
        Toast.makeText(context, "Card clicked!", Toast.LENGTH_SHORT).show() // Example click action
        return true
    }
}
