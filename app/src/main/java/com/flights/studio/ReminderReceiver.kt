package com.flights.studio

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val note = intent.getStringExtra("note")

        // Create a notification to remind the user
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create the notification content
        val notification = NotificationCompat.Builder(context, "reminder_channel")
            .setSmallIcon(dev.oneuiproject.oneui.R.drawable.ic_oui_reminder)
            .setContentTitle("Reminder!")
            .setContentText("It's time for: $note")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        // Show the notification
        notificationManager.notify(1, notification)
    }
}
