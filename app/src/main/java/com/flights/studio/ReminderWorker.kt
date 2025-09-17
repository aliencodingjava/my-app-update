package com.flights.studio

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.flights.studio.com.flights.studio.ui.ReminderDismissReceiver

class ReminderWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result {
        Log.d("ReminderWorker", "✅ WorkManager task started!")

        // ---------- 1. read note text ----------
        val noteKey = inputData.getString("note_key") ?: return Result.failure()
        val note = applicationContext
            .getSharedPreferences("reminder_notes", Context.MODE_PRIVATE)
            .getString(noteKey, "No note") ?: "No note"

        val channelId = "reminder_channel"
        val soundUri  =
            "android.resource://${applicationContext.packageName}/raw/reminder_sound".toUri()

        // ---------- 2. make / verify channel ----------
        val nm = applicationContext.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(channelId) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    "Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    setSound(soundUri, AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())
                    enableLights(true)
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 200, 100, 300)
                }
            )
        }

        // ---------- 3. bring volume up a bit ----------
        (applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager).run {
            val target = (getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION) * 0.7).toInt()
            setStreamVolume(AudioManager.STREAM_NOTIFICATION, target, 0)
        }

        // ---------- 4. intent when user TAPS the notification ----------
        val contentPI = PendingIntent.getActivity(
            applicationContext, 0,
            Intent(applicationContext, ReminderActivity::class.java).putExtra("note", note),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // ---------- 5. intent when user DISMISSES (swipes) the notification ----------
        val badgeKey = note.hashCode().toString()                         // the same key you stored in reminder_badges
        val dismissPI = PendingIntent.getBroadcast(
            applicationContext,
            badgeKey.hashCode(),                                          // unique requestCode
            Intent(applicationContext, ReminderDismissReceiver::class.java).apply {
                action = ReminderDismissReceiver.ACTION_BADGE_CHANGED
                putExtra(ReminderDismissReceiver.EXTRA_BADGE_KEY, badgeKey)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // ---------- 6. build + show ----------
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.airplane_svgrepo_com)
            .setContentTitle("Reminder!")
            .setContentText("It's time for: $note")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(soundUri)
            .setContentIntent(contentPI)
            .setDeleteIntent(dismissPI)           // ← this line makes the badge disappear
            .setAutoCancel(true)
            .build()

        if (ActivityCompat.checkSelfPermission(
                applicationContext, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("ReminderWorker", "🚨 Missing notification permission")
            return Result.failure()
        }

        NotificationManagerCompat.from(applicationContext)
            .notify(System.currentTimeMillis().toInt(), notification)

        Log.d("ReminderWorker", "✅ Notification sent")
        return Result.success()
    }
}
