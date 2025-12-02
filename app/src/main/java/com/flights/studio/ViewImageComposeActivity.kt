package com.flights.studio

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge   // ← add this import
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign

class ViewImageComposeActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge() // ← call this BEFORE setContent

        val urls = intent.getStringArrayListExtra(EXTRA_URLS)?.toList().orEmpty()
        val start = intent.getIntExtra(EXTRA_START_INDEX, 0)
            .coerceIn(0, (urls.size - 1).coerceAtLeast(0))

        setContent {
            LiquidGlassGalleryScreen(
                imageUrls = urls.ifEmpty { listOf() },
                startIndex = start,
                onBack = { finishInstant() },
                onClose = { finishInstant() },
                bottomBar = {
                    val c = androidx.compose.material3.LocalContentColor.current
                    Text(
                        "© 2025 Flights Studio — All Rights Reserved",
                        color = c,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            )
        }
    }

    private fun finishInstant() {
        finish()
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(
                OVERRIDE_TRANSITION_CLOSE,
                /* enterAnim = */ 0,
                /* exitAnim  = */ 0
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }



    companion object {
        const val EXTRA_URLS = "urls"
        const val EXTRA_START_INDEX = "start"
        fun intent(from: ComponentActivity, urls: List<String>, startIndex: Int = 0) =
            Intent(from, ViewImageComposeActivity::class.java).apply {
                putStringArrayListExtra(EXTRA_URLS, ArrayList(urls))
                putExtra(EXTRA_START_INDEX, startIndex)
            }
    }
}
