package com.flights.studio

import android.content.Context
import android.util.AttributeSet
import com.ortiz.touchview.TouchImageView

class FixedTouchImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : TouchImageView(context, attrs) {

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        // 🔥 FORCE `CENTER_CROP` Every Time Layout Changes
        if (scaleType != ScaleType.CENTER_CROP) {
            scaleType = ScaleType.CENTER_CROP
        }
    }
}
