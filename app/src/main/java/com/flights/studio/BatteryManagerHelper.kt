package com.flights.studio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

class BatteryManagerHelper(private val context: Context) {

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
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(batteryReceiver, filter)
    }

    fun stopMonitoring() {
        context.unregisterReceiver(batteryReceiver)
    }
}
