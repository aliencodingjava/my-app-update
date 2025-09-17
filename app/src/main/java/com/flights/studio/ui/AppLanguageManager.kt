package com.flights.studio.ui

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.core.content.edit
import androidx.preference.PreferenceManager

class AppLanguageManager : Application() {

    companion object {
        const val PREF_KEY_LANGUAGE = "selected_language"
        const val DEFAULT_LANGUAGE_TAG = "en"

        @Volatile private var blinkNext: Boolean = false

        fun currentLanguageTag(context: Context): String {
            val sp = PreferenceManager.getDefaultSharedPreferences(context)
            return sp.getString(PREF_KEY_LANGUAGE, DEFAULT_LANGUAGE_TAG) ?: DEFAULT_LANGUAGE_TAG
        }

        fun persistLanguage(context: Context, languageTag: String) {
            PreferenceManager.getDefaultSharedPreferences(context)
                .edit(commit = true) { putString(PREF_KEY_LANGUAGE, languageTag) }
        }

        /** Call this right before you recreate() to get a fast fade “blink”. */
        fun markBlink() { blinkNext = true }
        fun consumeBlinkNext(): Boolean {
            val v = blinkNext
            blinkNext = false
            return v
        }
    }

    override fun onCreate() {
        super.onCreate()

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(a: Activity, s: Bundle?) {
                if (!blinkNext) return

                // kill default transitions so it feels instant
                if (Build.VERSION.SDK_INT >= 34) {
                    a.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, 0, 0)
                    a.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, 0, 0)
                } else {
                    @Suppress("DEPRECATION")
                    a.overridePendingTransition(0, 0)
                }

                // start new content invisible; we’ll fade it in on resume
                a.findViewById<View>(android.R.id.content)?.alpha = 0f
            }

            override fun onActivityResumed(a: Activity) {
                if (!blinkNext) return
                a.findViewById<View>(android.R.id.content)?.animate()
                    ?.alpha(1f)
                    ?.setDuration(120) // tweak: 90–150ms feels “blink-y”
                    ?.withEndAction {
                        blinkNext = false
                        if (Build.VERSION.SDK_INT >= 34) {
                            a.clearOverrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN)
                            a.clearOverrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE)
                        }
                    }
                    ?.start()
            }

            // unused callbacks
            override fun onActivityStarted(a: Activity) {}
            override fun onActivityPaused(a: Activity) {}
            override fun onActivityStopped(a: Activity) {}
            override fun onActivitySaveInstanceState(a: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(a: Activity) {}
        })
    }
}
