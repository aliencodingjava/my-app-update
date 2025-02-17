package com.flights.studio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class YourWidgetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val position = intent.getIntExtra("ITEM_POSITION", -1)

        when (action) {
            "YOUR_WIDGET_CLICK_ACTION" -> {
                // Handle the click action for the item
                if (position != -1) {
                    // For demonstration, show a Toast message with the clicked item position
                    Toast.makeText(context, "Clicked item at position: $position", Toast.LENGTH_SHORT).show()

                    // Optionally, start an activity if needed
                    val newIntent = Intent(context, YourTargetActivity::class.java)
                    newIntent.putExtra("ITEM_POSITION", position)
                    newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Required for starting an activity from a receiver
                    context.startActivity(newIntent)
                }
            }
        }
    }
}
