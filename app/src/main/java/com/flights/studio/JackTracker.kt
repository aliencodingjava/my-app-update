package com.flights.studio

import android.app.Application
import com.google.firebase.FirebaseApp

class JHAirTracker : Application() {
    override fun onCreate() {
        super.onCreate()

        // ✅ Initialize Firebase (optional)
        FirebaseApp.initializeApp(this)


    }
}
