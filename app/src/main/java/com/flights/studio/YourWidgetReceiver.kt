package com.flights.studio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class YourWidgetReceiver : BroadcastReceiver() {
    companion object {                   // give it a distinct action
    }
    override fun onReceive(context: Context, intent: Intent) { /* … */ }
}

