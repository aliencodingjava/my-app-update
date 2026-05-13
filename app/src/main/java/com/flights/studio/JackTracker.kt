package com.flights.studio

import com.flights.studio.ui.AppLanguageManager
import com.google.firebase.FirebaseApp

class JHAirTracker : AppLanguageManager() {
    override fun onCreate() {
        super.onCreate()

        // ✅ Initialize Firebase (optional)
        FirebaseApp.initializeApp(this)


    }
}
