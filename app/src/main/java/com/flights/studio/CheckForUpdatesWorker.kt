package com.flights.studio

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class CheckForUpdatesWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val context = applicationContext
            val remote = AppUpdateRepository.fetchRemoteUpdate()
            val remoteVersionCode = remote.versionCode.toLong()
            val currentVersionCode = AppUpdater.getCurrentVersionCode(context)

            if (
                remoteVersionCode > currentVersionCode &&
                AppUpdateNotificationManager.shouldNotify(context, remoteVersionCode) &&
                AppUpdateNotificationManager.canPostNotifications(context)
            ) {
                AppUpdateNotificationManager.showUpdateAvailable(context, remote)
                AppUpdateNotificationManager.rememberNotifiedVersion(context, remoteVersionCode)
            }

            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
