package com.flights.studio

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.DecelerateInterpolator
import com.google.android.material.card.MaterialCardView
import kotlin.math.abs

class DraggableWidgetTimer @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : MaterialCardView(context, attrs, defStyleAttr) {

    /* ---------- dragging / clicking ---------- */
    private var dX = 0f
    private var dY = 0f
    private var isDragging = false
    private var initialX = 0f
    private var initialY = 0f
    private var hasMovedBeyondSlop = false
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    init {
        // drag from anywhere on this card
        setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> handleActionDown(v, event)
                MotionEvent.ACTION_MOVE -> handleActionMove(v, event)
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    if (handleActionUp(v)) v.performClick()
                }
            }
            true
        }
    }

    /* ====================  drag helpers  ===================== */

    private fun handleActionDown(v: View, e: MotionEvent) {
        dX = e.rawX - v.x
        dY = e.rawY - v.y
        initialX = e.rawX
        initialY = e.rawY
        isDragging = false
        hasMovedBeyondSlop = false
        scaleView(v, 0.98f)
    }

    private fun handleActionMove(v: View, e: MotionEvent): Boolean {
        val moved =
            abs(e.rawX - initialX) > touchSlop || abs(e.rawY - initialY) > touchSlop
        if (moved) {
            hasMovedBeyondSlop = true
            isDragging = true
            parent?.requestDisallowInterceptTouchEvent(true)
        }
        if (isDragging) {
            val nx = (e.rawX - dX)
                .coerceIn(0f, resources.displayMetrics.widthPixels - v.width.toFloat())
            val ny = (e.rawY - dY)
                .coerceIn(0f, resources.displayMetrics.heightPixels - v.height.toFloat())
            v.x = nx
            v.y = ny
        }
        return isDragging
    }

    private fun handleActionUp(v: View): Boolean {
        scaleView(v, 1f)
        parent?.requestDisallowInterceptTouchEvent(false)
        if (isDragging) centerHorizontally(v)
        val click = !isDragging
        isDragging = false
        hasMovedBeyondSlop = false
        return click
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    /* ---------------- misc ui helpers ----------------- */

    private fun scaleView(v: View, s: Float) =
        v.animate()
            .scaleX(s)
            .scaleY(s)
            .setDuration(150)
            .setInterpolator(DecelerateInterpolator())
            .start()

    private fun centerHorizontally(v: View) =
        v.animate()
            .x((resources.displayMetrics.widthPixels - v.width) / 2f)
            .setDuration(300)
            .setInterpolator(DecelerateInterpolator())
            .start()
}
