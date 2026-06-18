package com.flights.studio

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppUpdateFirebaseMessagingService : FirebaseMessagingService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        if (data["type"] != UPDATE_TYPE) return

        val remote = RemoteUpdateInfo(
            versionCode = data["versionCode"]?.toIntOrNull() ?: return,
            versionName = data["versionName"].orEmpty(),
            apkUrl = data["apkUrl"].orEmpty(),
            updates = emptyList()
        )

        serviceScope.launch {
            val context = applicationContext
            val remoteCode = remote.versionCode.toLong()
            val localCode = AppUpdater.getCurrentVersionCode(context)

            if (
                remoteCode > localCode &&
                AppUpdateNotificationManager.shouldNotify(context, remoteCode) &&
                AppUpdateNotificationManager.canPostNotifications(context)
            ) {
                AppUpdateNotificationManager.showUpdateAvailable(context, remote)
                AppUpdateNotificationManager.rememberNotifiedVersion(context, remoteCode)
            }
        }
    }

    companion object {
        private const val UPDATE_TYPE = "app_update"
    }
}
