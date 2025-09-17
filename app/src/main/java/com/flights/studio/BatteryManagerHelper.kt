package com.flights.studio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log

class BatteryManagerHelper(private val context: Context) {

    // Flag to track registration state
    private var isReceiverRegistered = false

    // Battery receiver
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            val batteryPct = if (level != -1 && scale != -1) (level / scale.toFloat() * 100).toInt() else -1
            onBatteryStatusUpdated?.invoke(batteryPct, isCharging)
        }
    }

    var onBatteryStatusUpdated: ((Int, Boolean) -> Unit)? = null

    fun startMonitoring() {
        if (!isReceiverRegistered) {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            try {
                context.registerReceiver(batteryReceiver, filter)
                isReceiverRegistered = true
                Log.d("BatteryManagerHelper", "Battery receiver registered")
            } catch (e: Exception) {
                Log.e("BatteryManagerHelper", "Error registering battery receiver: ${e.message}")
            }
        }
    }

    fun stopMonitoring() {
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(batteryReceiver)
                Log.d("BatteryManagerHelper", "Battery receiver unregistered")
            } catch (e: IllegalArgumentException) {
                Log.e("BatteryManagerHelper", "Receiver not registered: ${e.message}")
            }
            isReceiverRegistered = false
        }
    }
}
