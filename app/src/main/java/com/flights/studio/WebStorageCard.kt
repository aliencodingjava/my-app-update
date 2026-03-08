package com.flights.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.unit.dp

@Composable
fun WebStorageCard(
    stats: WebStorageStats,
    onClear: () -> Unit,
    textColor: Color
) {

    val total = stats.total
    val safeTotal = if (total <= 0f) 1f else total

    val httpRatio = (stats.httpCache / safeTotal).coerceAtLeast(0.01f)
    val cookieRatio = (stats.cookies / safeTotal).coerceAtLeast(0.01f)
    val dbRatio = (stats.webDb / safeTotal).coerceAtLeast(0.01f)

//    val httpAnim by animateFloatAsState(httpRatio.coerceAtLeast(0.01f))
//    val cookieAnim by animateFloatAsState(cookieRatio.coerceAtLeast(0.01f))
//    val dbAnim by animateFloatAsState(dbRatio.coerceAtLeast(0.01f))

    Column {

        Text(
            "WebView storage",
            style = MaterialTheme.typography.titleMedium,
            color = textColor
        )

        Spacer(Modifier.height(12.dp))

        Text(
            String.format(
                LocalLocale.current.platformLocale,
                "%.1f MB used",
                total
            ),
            style = MaterialTheme.typography.headlineSmall,
            color = textColor
        )

        Spacer(Modifier.height(16.dp))

        // segmented storage bar
        Row(
            Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.Gray.copy(alpha = 0.3f))
        ) {

            Box(
                Modifier
                    .fillMaxHeight()
                    .weight(httpRatio)
                    .background(Color(0xFF42A5F5))
            )

            Box(
                Modifier
                    .fillMaxHeight()
                    .weight(cookieRatio)
                    .background(Color(0xFF7E57C2))
            )

            Box(
                Modifier
                    .fillMaxHeight()
                    .weight(dbRatio)
                    .background(Color(0xFF66BB6A))
            )
        }

        Spacer(Modifier.height(20.dp))

        StorageRow("HTTP cache", stats.httpCache, textColor)
        StorageRow("Cookies", stats.cookies, textColor)
        StorageRow("WebView DB", stats.webDb, textColor)

        Spacer(Modifier.height(16.dp))

        Button(onClick = onClear) {
            Text("Clear Web Data")
        }
    }
}

