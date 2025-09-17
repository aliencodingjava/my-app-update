// ReminderHost.kt
package com.flights.studio

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.kyant.backdrop.backdrop
import com.kyant.backdrop.rememberLayerBackdrop

@Composable
fun ReminderHost(
    visible: Boolean,
    onDismiss: () -> Unit,
    onTimer: () -> Unit,
    onCalendar: () -> Unit,
    content: @Composable () -> Unit   // <-- slot for your main screen
) {
    val dark = isSystemInDarkTheme()

    // OPAQUE background for stability (do NOT use transparent)
    val backdrop = rememberLayerBackdrop(
        backgroundColor = if (dark) Color(0xFF0E0E10) else Color(0xFFF2F6FF)
    )

    Box(Modifier.fillMaxSize()) {
        // Provider: write pixels INTO the backdrop
        Box(
            Modifier
                .matchParentSize()
                .backdrop(backdrop)
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
