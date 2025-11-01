package com.flights.studio

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderOptionsSheetModal(
    backdrop: Backdrop,
    visible: Boolean,
    onDismiss: () -> Unit,
    onTimer: () -> Unit,
    onCalendar: () -> Unit,
) {

    val context = LocalContext.current

    // 🔊 Play sound when the sheet becomes visible
    LaunchedEffect(visible) {
        if (visible) {
            playSheetOpenSound(context, R.raw.confirm)
        }
    }
    if (!visible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        // transparent container so the LiquidGlass you draw is the real panel
        containerColor = Color.Transparent,
        dragHandle = null,
        tonalElevation = 0.dp,
        scrimColor = Color.Black.copy(alpha = 0.35f)
    ) {
        // your existing glass content, unchanged:
        ReminderOptionsSheetContent(
            backdrop = backdrop as LayerBackdrop,
            onDismiss = onDismiss,
            onTimer = onTimer,
            onCalendar = onCalendar
        )
    }
}
