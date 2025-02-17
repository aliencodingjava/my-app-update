package com.flights.studio

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class YourTargetActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.widget_title_layout) // Use your actual layout

        // Retrieve the clicked item's position
        intent.getIntExtra("ITEM_POSITION", -1)
        // Use the position to load data or update the UI
    }
}
