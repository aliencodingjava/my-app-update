package com.flights.studio

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

@Suppress("DEPRECATION")
class FixedSizeTextView(context: Context, attrs: AttributeSet) : AppCompatTextView(context, attrs) {

    init {
        // Disable text scaling by setting text size manually
        val scaledDensity = resources.displayMetrics.scaledDensity
        textSize = 3 * scaledDensity // This ensures the text size remains fixed at 9sp
    }
}
