package com.flights.studio

import android.webkit.CookieManager
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.flights.studio.UiUtils.getWebStorageStats
import java.io.File

data class WebStorageStats(
    val httpCache: Float,
    val cookies: Float,
    val webDb: Float
) {
    val total: Float
        get() = httpCache + cookies + webDb
}

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {

    val context = LocalContext.current

    var hwAccel by remember {
        mutableStateOf(SettingsStore.hardwareAccel(context))
    }

    var storageStats by remember {
        mutableStateOf(getWebStorageStats(context))
    }
    var darkWeb by remember {
        mutableStateOf(SettingsStore.darkWeb(context))
    }

    var textZoom by remember {
        mutableIntStateOf(SettingsStore.textZoom(context))
    }

    val isDark = isSystemInDarkTheme()
    val textColor =
        if (isDark) Color.White else MaterialTheme.colorScheme.onBackground

    Box(
        modifier = modifier.fillMaxSize()
    ) {

        // background grid
        ProfileBackdropImageLayer(
            modifier = Modifier.matchParentSize(),
            lightRes = R.drawable.light_grid_pattern,
            darkRes = R.drawable.dark_grid_pattern,
            imageAlpha = if (isDark) 1f else 0.8f,
            scrimDark = 0f,
            scrimLight = 0f
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    top = 90.dp,
                    start = 20.dp,
                    end = 20.dp,
                    bottom = 20.dp
                )
        ) {

            Spacer(Modifier.height(24.dp))

            SettingsBox {

                Text(
                    "Appearance",
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor
                )

                Spacer(Modifier.height(12.dp))

                SettingsSwitchRow(
                    title = "Force dark mode",
                    checked = darkWeb,
                    textColor = textColor
                ) {
                    darkWeb = it
                    SettingsStore.setDarkWeb(context, it)
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    "Text size: $textZoom%",
                    color = textColor
                )

                androidx.compose.material3.Slider(
                    value = textZoom.toFloat(),
                    onValueChange = {
                        textZoom = it.toInt()
                        SettingsStore.setTextZoom(context, textZoom)
                    },
                    valueRange = 60f..140f
                )
            }
            Spacer(Modifier.height(12.dp))
            // PERFORMANCE
            SettingsBox {

                Text(
                    "Performance",
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor
                )

                Spacer(Modifier.height(12.dp))

                SettingsSwitchRow(
                    title = "Hardware acceleration",
                    checked = hwAccel,
                    textColor = textColor
                ) {
                    hwAccel = it
                    SettingsStore.setHardwareAccel(context, it)
                }
            }

            Spacer(Modifier.height(12.dp))



            // WEBVIEW STORAGE CARD
            SettingsBox {

                WebStorageCard(
                    stats = storageStats,
                    textColor = textColor,
                    onClear = {

                        val webView = WebView(context)

                        // WebView internal clear
                        webView.clearCache(true)

                        CookieManager.getInstance().apply {
                            removeAllCookies(null)
                            flush()
                        }

                        android.webkit.WebStorage.getInstance().deleteAllData()

                        context.deleteDatabase("webview.db")
                        context.deleteDatabase("webviewCache.db")

                        webView.destroy()

                        // 🔥 Delete the real HTTP cache
                        val webCache = File(context.cacheDir, "WebView")
                        if (webCache.exists()) {
                            webCache.deleteRecursively()
                        }

                        storageStats = getWebStorageStats(context)
                    }
                )
            }

            Spacer(Modifier.height(24.dp))

            // ABOUT
            Text(
                "About",
                style = MaterialTheme.typography.titleMedium,
                color = textColor
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "WebView Engine v1.2.8",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isDark)
                    Color.White.copy(alpha = 0.7f)
                else
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }
    }
}
@Composable
fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    textColor: Color,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            title,
            modifier = Modifier.weight(1f),
            color = textColor
        )

        Switch(
            checked = checked,
            onCheckedChange = onChange,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
fun StorageRow(
    name: String,
    value: Float,
    textColor: Color
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = name,
            modifier = Modifier.weight(1f),
            color = textColor
        )

        Text(
            text = String.format(
                androidx.compose.ui.platform.LocalLocale.current.platformLocale,
                "%.1f MB",
                value
            ),
            color = textColor.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun SettingsBox(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {

    val isDark = isSystemInDarkTheme()

    val bg =
        if (isDark)
            Color.White.copy(alpha = 0.05f)
        else
            Color.Black.copy(alpha = 0.04f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(16.dp)
    ) {
        content()
    }
}