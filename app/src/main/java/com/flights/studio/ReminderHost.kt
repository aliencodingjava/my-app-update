// ReminderHost.kt
package com.flights.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

@Composable
fun ReminderHost(
    visible: Boolean,
    onDismiss: () -> Unit,
    onTimer: () -> Unit,
    onCalendar: () -> Unit,
    content: @Composable () -> Unit   // slot for your main screen
) {
    val dark = isSystemInDarkTheme()

    // OPAQUE base behind everything (rc01 doesn't take backgroundColor in rememberLayerBackdrop)
    val base = if (dark) Color(0xFF0E0E10) else Color(0xFFF2F6FF)

    // rc01 Backdrop state
    val backdrop: Backdrop = rememberLayerBackdrop()

    Box(
        Modifier
            .fillMaxSize()
            .background(base) // stability against transparency
    ) {
        // Provider: content to be sampled by glass
        Box(
            Modifier
                .matchParentSize()
                .layerBackdrop(backdrop as LayerBackdrop) // was .backdrop(...)
        ) {
            // Background you want refracted
            GlassBackdrop(modifier = Modifier.matchParentSize())

            // Your page content beneath the sheet
            content()
        }

        // The sheet draws glass by sampling the same backdrop
        ReminderOptionsSheetModal(
            backdrop = backdrop,
            visible = visible,
            onDismiss = onDismiss,
            onTimer = onTimer,
            onCalendar = onCalendar
        )
    }
}
