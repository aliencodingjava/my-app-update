package com.flights.studio

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class AppUpdateCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext

        return try {
            val remote = AppUpdateRepository.fetchRemoteUpdate()
            val currentVersionCode = AppUpdater.getCurrentVersionCode(context)

            val remoteVersionCode = remote.versionCode.toLong()
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

    companion object {
        private const val WORK_NAME = "jac_app_update_check"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<AppUpdateCheckWorker>(
                24L,
                TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setInitialDelay(30L, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}
