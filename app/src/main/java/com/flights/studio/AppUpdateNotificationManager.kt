package com.flights.studio

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit

object AppUpdateNotificationManager {
    const val EXTRA_CHECK_FOR_UPDATES = "com.flights.studio.extra.CHECK_FOR_UPDATES"
    const val EXTRA_REMOTE_VERSION_CODE = "com.flights.studio.extra.REMOTE_VERSION_CODE"
    const val EXTRA_REMOTE_VERSION_NAME = "com.flights.studio.extra.REMOTE_VERSION_NAME"
    const val EXTRA_REMOTE_APK_URL = "com.flights.studio.extra.REMOTE_APK_URL"

    private const val CHANNEL_ID = "app_updates"
    private const val NOTIFICATION_ID = 4401
    private const val PREFS = "app_update_notifications"
    private const val KEY_LAST_NOTIFIED_VERSION_CODE = "last_notified_version_code"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "App updates",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications when a new app version is available"
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    fun canPostNotifications(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
    }

    fun showUpdateAvailable(context: Context, remote: RemoteUpdateInfo) {
        if (!canPostNotifications(context)) return

        val intent = Intent(context, SoftwareUpdateActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_CHECK_FOR_UPDATES, true)
            putExtra(EXTRA_REMOTE_VERSION_CODE, remote.versionCode)
            putExtra(EXTRA_REMOTE_VERSION_NAME, remote.versionName)
            putExtra(EXTRA_REMOTE_APK_URL, remote.apkUrl)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.notifications_active_24dp_ffffff_fill1_wght400_grad0_opsz24)
            .setContentTitle("New app update available")
            .setContentText("Version ${remote.versionName} is ready to download.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    fun shouldNotify(context: Context, versionCode: Long): Boolean {
        val last = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_NOTIFIED_VERSION_CODE, 0L)
        return versionCode > last
    }

    fun rememberNotifiedVersion(context: Context, versionCode: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putLong(KEY_LAST_NOTIFIED_VERSION_CODE, versionCode)
        }
    }
}
