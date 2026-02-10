package com.flights.studio

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

@Composable
fun ReminderOptionsSheetContent(
    backdrop: Backdrop,            // rc01 type
    onDismiss: () -> Unit,
    onTimer: () -> Unit,
    onCalendar: () -> Unit,
) {
    val sheetShape = RoundedCornerShape(24.dp)
    val isDark = isSystemInDarkTheme()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(sheetShape)
    ) {
        // === Glass sheet background ===
        Box(
            Modifier
                .matchParentSize()
                .clip(sheetShape)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { sheetShape },
                    effects = {
                        vibrancy()
                        blur(4.dp.toPx())
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            lens(
                                refractionHeight = 8.dp.toPx(),
                                refractionAmount = 48.dp.toPx(),
                                chromaticAberration = true
                            )
                        }
                    },
                    onDrawSurface = {

                        val tint = if (isDark) {
                            // 🌙 Dark mode → subtle light frost
                            Color.White.copy(alpha = 0.05f)
                        } else {
                            // ☀️ Light mode → subtle dark tint
                            Color.Black.copy(alpha = 0.05f)
                        }

                        drawRect(tint)
                    }

                )
        )

        // === Content ===
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.50f))
            )
            Spacer(Modifier.height(12.dp))

            // Header card (glass)
            val headerShape = RoundedCornerShape(12.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(headerShape)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { headerShape },
                        effects = {
                            vibrancy()
                            blur(4.dp.toPx())
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                lens(
                                    refractionHeight = 8.dp.toPx(),
                                    refractionAmount = 48.dp.toPx(),
                                    chromaticAberration = true
                                )
                            }
                        },
                        onDrawSurface = {
                            drawRect(Color.Black.copy(alpha = 0.50f)) // subtle tint
                        }
                    )
                    .padding(vertical = 16.dp, horizontal = 16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val iconTint = if (isSystemInDarkTheme()) Color.Green else Color.Green
                    Image(
                        painter = painterResource(id = R.drawable.add_alert_24dp_ffffff_fill1_wght400_grad0_opsz24),
                        contentDescription = stringResource(R.string.choose_reminder_type),
                        colorFilter = ColorFilter.tint(iconTint),
                        modifier = Modifier
                            .size(32.dp)
                            .padding(bottom = 8.dp)
                    )
                    Text(
                        text = stringResource(id = R.string.choose_reminder_type),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.secondaryFixed,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(id = R.string.reminder_hint_short),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondaryFixed,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassOptionCard(
                    backdrop = backdrop,
                    title = stringResource(id = R.string.timer),
                    caption = stringResource(id = R.string.timer_caption_short),
                    iconRes = R.drawable.ic_oui_time,
                    onClick = onTimer,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(16.dp))
                GlassOptionCard(
                    backdrop = backdrop,
                    title = stringResource(id = R.string.calendar),
                    caption = stringResource(id = R.string.calendar_caption_short),
                    iconRes = R.drawable.ic_oui_calendar,
                    onClick = onCalendar,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    BackHandler(onBack = onDismiss)
}

@Preview(showBackground = true, showSystemUi = true, name = "Reminder Sheet Preview")
@Composable
fun ReminderOptionsSheetContentPreview() {
    // rc01: create the backdrop state (no backgroundColor arg)
    val backdrop: Backdrop = rememberLayerBackdrop()

    Box(Modifier.fillMaxWidth()) {
        // Writer layer: the content to be sampled by glass
        Box(
            Modifier
                .fillMaxWidth()
                .height(400.dp)
                .layerBackdrop(backdrop as LayerBackdrop)              // was .backdrop(...)
                .background(Color(0xFF0E0E10))       // opaque base
        ) {
            GlassBackdrop(Modifier.matchParentSize())
        }

        // Foreground: the sheet using the same backdrop
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        ) {
            ReminderOptionsSheetContent(
                backdrop = backdrop,
                onDismiss = {},
                onTimer = {},
                onCalendar = {}
            )
        }
    }
}

@Composable
fun GlassBackdrop(modifier: Modifier = Modifier) {
    Box(modifier) {
        // gradient base
        Box(
            Modifier
                .matchParentSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF3D4A62), Color(0xFF1C2129))
                    )
                )
        )
        // radial blobs
        Box(
            Modifier
                .matchParentSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF49A6FF).copy(alpha = 0.16f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(0.22f, 0.12f),
                        radius = 900f
                    )
                )
        )
        Box(
            Modifier
                .matchParentSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF8B5CF6).copy(alpha = 0.14f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(0.82f, 0.22f),
                        radius = 800f
                    )
                )
        )
        Box(
            Modifier
                .matchParentSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF22C55E).copy(alpha = 0.12f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(0.5f, 0.95f),
                        radius = 1100f
                    )
                )
        )
    }
}

// === Option Card ===
@Composable
private fun GlassOptionCard(
    backdrop: Backdrop,           // rc01 type
    title: String,
    caption: String,
    iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardShape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .clip(cardShape)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { cardShape },
                effects = {
                    vibrancy()
                    blur(4.dp.toPx())
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        lens(
                            refractionHeight = 8.dp.toPx(),
                            refractionAmount = 48.dp.toPx(),
                            chromaticAberration = true
                        )
                    }
                },
                onDrawSurface = {
                    drawRect(Color.DarkGray.copy(alpha = 0.50f))
                }
            )
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.40f)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = title,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.secondaryFixed,
                modifier = Modifier.padding(top = 10.dp)
            )
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondaryFixed,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
