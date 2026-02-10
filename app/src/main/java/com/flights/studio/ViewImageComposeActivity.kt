package com.flights.studio

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge   // ← add this import
import androidx.annotation.RequiresApi


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
            )
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
