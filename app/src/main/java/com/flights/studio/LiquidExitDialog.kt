package com.flights.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle

@Composable
fun ExitLiquidDialog(
    backdrop: LayerBackdrop,
    onCancel: () -> Unit,
    onConfirmExit: () -> Unit
) {
    val isLightTheme = !isSystemInDarkTheme()

    val contentColor = if (isLightTheme) Color.Black else Color.White
    val accentColor  = if (isLightTheme) Color(0xFF0088FF) else Color(0xFF0091FF)

    // match catalog values
    val containerColor =
        if (isLightTheme) Color(0xFFFAFAFA).copy(alpha = 0.60f)
        else Color(0xFF121212).copy(alpha = 0.40f)
    val dimColor =
        if (isLightTheme) Color(0xFF29293A).copy(alpha = 0.23f)
        else Color(0xFF121212).copy(alpha = 0.56f)

    // Fullscreen scrim that dismisses on outside tap
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(dimColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onCancel() },
        contentAlignment = Alignment.Center
    ) {
        // Dialog container (consumes taps)
        Column(
            Modifier
                .padding(horizontal = 48.dp, vertical = 56.dp) // bigger outer margin
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { ContinuousRoundedRectangle(48.dp) },
                    effects = {
                        colorControls(
                            brightness = if (isLightTheme) 0.20f else 0f, // like catalog
                            saturation = 1.50f
                        )
                        blur(if (isLightTheme) 16.dp.toPx() else 8.dp.toPx())
                        lens(24.dp.toPx(), 48.dp.toPx(), depthEffect = true)
                    },
                    highlight = { Highlight.Plain },
                    onDrawSurface = { drawRect(containerColor) }
                )
                .fillMaxWidth()
                .clip(ContinuousRoundedRectangle(48.dp))
                .clickable( // consume inside taps, don't dismiss
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* consume */ }
        ) {
            BasicText(
                text = "Close Flights Studio?",
                modifier = Modifier.padding(start = 28.dp, top = 24.dp, end = 28.dp, bottom = 12.dp),
                style = TextStyle(color = contentColor, fontSize = 24.sp, fontWeight = FontWeight.Medium)
            )

            BasicText(
                text = "Are you sure you want to exit the app?",
                modifier = Modifier
                    .then(
                        if (isLightTheme) Modifier
                        else Modifier.graphicsLayer(blendMode = BlendMode.Plus) // lighter text over dark glass
                    )
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                style = TextStyle(color = contentColor.copy(alpha = 0.68f), fontSize = 15.sp),
                maxLines = 5
            )

            Row(
                modifier = Modifier
                    .padding(start = 24.dp, top = 12.dp, end = 24.dp, bottom = 24.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cancel button (soft)
                Row(
                    modifier = Modifier
                        .clip(ContinuousCapsule)
                        .background(containerColor.copy(alpha = 0.20f))
                        .clickable { onCancel() }
                        .height(48.dp)
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicText("Cancel", style = TextStyle(color = contentColor, fontSize = 16.sp))
                }

                // Exit button (accent)
                Row(
                    modifier = Modifier
                        .clip(ContinuousCapsule)
                        .background(accentColor)
                        .clickable { onConfirmExit() }
                        .height(48.dp)
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicText("Exit", style = TextStyle(color = Color.White, fontSize = 16.sp))
                }
            }
        }
    }
}
