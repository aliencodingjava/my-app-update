package com.flights.studio

import android.app.Application
import com.flights.studio.ui.AppLanguageManager
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging

class JHAirTracker : Application() {
    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)
        AppCheckProviderInstaller.install()
        AppIconManager.repairLauncherAliases(this)
        AppUpdateNotificationManager.createChannels(this)
        AppUpdateCheckWorker.schedule(this)
        AppUpdater.cleanupUpdateApks(this)
        FirebaseMessaging.getInstance().subscribeToTopic("app_updates")
    }
}
