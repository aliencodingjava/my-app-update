package com.flights.studio

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.os.bundleOf
import androidx.core.view.WindowCompat

class LiveCamerasActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val cards: List<CameraCard> =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                intent.getParcelableArrayListExtra(EXTRA_CARDS, CameraCard::class.java) ?: emptyList()
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableArrayListExtra<CameraCard>(EXTRA_CARDS) ?: emptyList()
            }

        setContent {
            FlightsTheme {

                val view = LocalView.current
                val isDark = isSystemInDarkTheme()

                SideEffect {
                    val window = (view.context as ComponentActivity).window
                    WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
                }

                LiveCamerasPage(
                    cards = cards,
                    onClose = { finish() }
                )
            }
        }
    }

    companion object {
        private const val EXTRA_CARDS = "extra_cards"

        fun intent(context: Context, cards: List<CameraCard>): Intent {
            return Intent(context, LiveCamerasActivity::class.java).apply {
                putExtras(bundleOf(EXTRA_CARDS to ArrayList(cards)))
            }
        }
    }
}