package com.flights.studio

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class ReminderAudioPlaybackService : Service() {
    private var player: MediaPlayer? = null
    private var currentUri: Uri? = null
    private var currentTitle: String = "Reminder audio"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> {
                player?.pause()
                showNotification(playing = false)
            }
            ACTION_PLAY -> {
                player?.start()
                showNotification(playing = true)
            }
            ACTION_STOP -> {
                stopPlayback()
                stopSelf()
            }
            else -> {
                val uri = intent?.getStringExtra(EXTRA_URI)?.let(Uri::parse) ?: return START_NOT_STICKY
                val title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "Reminder audio" }
                val position = intent.getIntExtra(EXTRA_POSITION_MS, 0).coerceAtLeast(0)
                startPlayback(uri, title, position)
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopPlayback()
        super.onDestroy()
    }

    private fun startPlayback(uri: Uri, title: String, positionMs: Int) {
        if (currentUri != uri) {
            stopPlayback()
            currentUri = uri
            currentTitle = title
            player = MediaPlayer.create(this, uri)?.apply {
                setOnCompletionListener {
                    stopPlayback()
                    stopSelf()
                }
            }
        } else {
            currentTitle = title
        }

        player?.let {
            if (positionMs > 0) runCatching { it.seekTo(positionMs) }
            it.start()
            showNotification(playing = true)
        } ?: stopSelf()
    }

    private fun stopPlayback() {
        runCatching { player?.pause() }
        runCatching { player?.release() }
        player = null
        currentUri = null
    }

    private fun showNotification(playing: Boolean) {
        ensureChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.airplane_svgrepo_com)
            .setContentTitle(currentTitle)
            .setContentText(if (playing) "Playing reminder audio" else "Reminder audio paused")
            .setOngoing(playing)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                if (playing) R.drawable.ic_samsung_close else R.drawable.play_arrow_24dp_ffffff_fill1_wght400_grad0_opsz24,
                if (playing) "Pause" else "Play",
                serviceIntent(if (playing) ACTION_PAUSE else ACTION_PLAY, 1)
            )
            .addAction(
                R.drawable.ic_samsung_close,
                "Stop",
                serviceIntent(ACTION_STOP, 2)
            )
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun serviceIntent(action: String, requestCode: Int): PendingIntent {
        return PendingIntent.getService(
            this,
            requestCode,
            Intent(this, ReminderAudioPlaybackService::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Reminder audio",
                NotificationManager.IMPORTANCE_LOW
            )
        )
    }

    companion object {
        private const val CHANNEL_ID = "reminder_audio_playback"
        private const val NOTIFICATION_ID = 4408
        private const val ACTION_PLAY = "com.flights.studio.reminder_audio.PLAY"
        private const val ACTION_PAUSE = "com.flights.studio.reminder_audio.PAUSE"
        private const val ACTION_STOP = "com.flights.studio.reminder_audio.STOP"
        private const val EXTRA_URI = "uri"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_POSITION_MS = "position_ms"

        fun startIntent(context: Context, uri: Uri, title: String, positionMs: Int): Intent {
            return Intent(context, ReminderAudioPlaybackService::class.java)
                .putExtra(EXTRA_URI, uri.toString())
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_POSITION_MS, positionMs)
        }

        fun stopIntent(context: Context): Intent {
            return Intent(context, ReminderAudioPlaybackService::class.java)
                .setAction(ACTION_STOP)
        }
    }
}
