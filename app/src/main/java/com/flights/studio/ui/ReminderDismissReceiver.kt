package com.flights.studio.com.flights.studio.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager   // ← add

class ReminderDismissReceiver : BroadcastReceiver() {
    companion object {
        const val EXTRA_BADGE_KEY      = "badge_key"
        const val ACTION_BADGE_CHANGED = "com.flights.studio.ACTION_REMINDER_BADGE_CHANGED"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val badgeKey = intent.getStringExtra(EXTRA_BADGE_KEY) ?: return

        // 1) turn the dot off in prefs
        context.getSharedPreferences("reminder_badges", Context.MODE_PRIVATE)
            .edit()
            .putBoolean(badgeKey, false)
            .apply()

        // 2) notify _in-app_ listeners
        LocalBroadcastManager.getInstance(context)
            .sendBroadcast(
                Intent(ACTION_BADGE_CHANGED)
                    .putExtra(EXTRA_BADGE_KEY, badgeKey)
            )
    }
}
