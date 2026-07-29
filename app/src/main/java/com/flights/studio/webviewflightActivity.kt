package com.flights.studio

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.kyant.backdrop.backdrops.rememberLayerBackdrop


@Suppress("DEPRECATION")
class WebviewflightActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val startCardId = intent.getStringExtra("start_card") ?: "card1"
        val returnHome = intent.getBooleanExtra("RETURN_HOME", false)

        setContent {
            val appThemePreset = AppThemeStore.rememberPreset(this@WebviewflightActivity)
            FlightsTheme(
                profileBackdropStyle = ProfileBackdropStyle.Auto,
                appThemePreset = appThemePreset
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

                var contentVisible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    contentVisible = true
                }
                val entranceProgress by animateFloatAsState(
                    targetValue = if (contentVisible) 1f else 0f,
                    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                    label = "flightActivityEntrance"
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = entranceProgress
                            translationY = (1f - entranceProgress) * 22f
                        }
                ) {
                    WebviewFlights(
                        startCardId = startCardId,
                        returnHome = returnHome,

                        onExitToHome = {
                            val home = Intent(this@WebviewflightActivity, MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            }
                            startActivity(
                                home,
                                ActivityOptions.makeCustomAnimation(
                                    this@WebviewflightActivity,
                                    R.anim.enter_animation,
                                    R.anim.exit_animation
                                ).toBundle()
                            )
                            finish()
                        },
                        onExitNormal = { finishWithAnim() },
                        onOpenWelcome = { },
                        backdrop = rememberLayerBackdrop(),

                    )
                }
            }
        }
    }
    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        // Do nothing — prevents activity recreation on rotation
        // The JS orientation change listener handles sheet repositioning
    }

    private fun finishWithAnim() {
        finish()
        overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation)
    }
}
