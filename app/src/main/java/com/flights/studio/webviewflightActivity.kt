package com.flights.studio

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Suppress("DEPRECATION")
class webviewflightActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val startCardId = intent.getStringExtra("start_card") ?: "card1"
        val returnHome = intent.getBooleanExtra("RETURN_HOME", false)

        setContent {
            FlightsTheme(
                profileBackdropStyle = ProfileBackdropStyle.Auto
            ) {
                val view = LocalView.current
                val window = (view.context as Activity).window
                val isDark = isSystemInDarkTheme()
                val barColor = androidx.compose.material3.MaterialTheme.colorScheme.surface

                SideEffect {
                    window.statusBarColor = barColor.toArgb()
                    window.navigationBarColor = barColor.toArgb()

                    WindowCompat.getInsetsController(window, view).apply {
                        isAppearanceLightStatusBars = !isDark
                        isAppearanceLightNavigationBars = !isDark
                    }
                }

                WebviewFlights(
                    startCardId = startCardId,
                    returnHome = returnHome,

                    onExitNormal = { finishWithAnim() },

                    onExitToHome = {
                        val home = Intent(this, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        }
                        startActivity(
                            home,
                            ActivityOptions.makeCustomAnimation(
                                this,
                                R.anim.enter_animation,
                                R.anim.exit_animation
                            ).toBundle()
                        )
                        finish()
                    },

                    onOpenWelcome = { }
                )
            }
        }
    }

    private fun finishWithAnim() {
        finish()
        overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation)
    }
}
