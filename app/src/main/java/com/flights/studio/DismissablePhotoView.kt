package com.flights.studio

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import com.github.chrisbanes.photoview.PhotoView
import kotlin.math.abs

class DismissablePhotoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : PhotoView(context, attrs, defStyle) {

    interface DragDismissListener {
        fun onDrag(dy: Float)
        fun onRelease(dy: Float)
    }

    var dragDismissListener: DragDismissListener? = null

    private var downX = 0f
    private var downY = 0f
    private var dragging = false
    private val slop = ViewConfiguration.get(context).scaledTouchSlop

    private fun isZoomed(): Boolean = scale > minimumScale + 0.01f

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.rawX
                downY = ev.rawY
                dragging = false
            }
            MotionEvent.ACTION_MOVE -> {
                if (!isZoomed()) {
                    val dx = ev.rawX - downX
                    val dy = ev.rawY - downY
                    if (!dragging && abs(dy) > abs(dx) && abs(dy) > slop) {
                        dragging = true
                        parent?.requestDisallowInterceptTouchEvent(true)
                    }
                    if (dragging) {
                        dragDismissListener?.onDrag(dy)
                        return true
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (!isZoomed() && dragging) {
                    val dy = ev.rawY - downY
                    dragDismissListener?.onRelease(dy)
                    dragging = false
                    return true
                }
                // treat as tap if no drag happened
                val dx = ev.rawX - downX
                val dy = ev.rawY - downY
                if (!dragging && abs(dx) < slop && abs(dy) < slop && ev.actionMasked == MotionEvent.ACTION_UP) {
                    performClick()
                }
            }
        }
        // keep PhotoView gestures (pinch/double-tap)
        return super.onTouchEvent(ev)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
