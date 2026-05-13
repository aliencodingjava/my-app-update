package com.flights.studio

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
    backdrop: Backdrop,
    onDismiss: () -> Unit,
    onTimer: () -> Unit,
    onCalendar: () -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    val panelColor = if (isDark) Color(0xFF202124).copy(alpha = 0.62f) else Color(0xFFE6E2E7).copy(alpha = 0.52f)
    val textColor = if (isDark) Color.White.copy(alpha = 0.92f) else Color(0xFF1E1F24)
    val mutedColor = if (isDark) Color.White.copy(alpha = 0.62f) else Color(0xFF5A5D66)
    val buttonColor = if (isDark) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.96f)
    val accentColor = if (isDark) Color(0xFF8EC5FF) else Color(0xFF0B57D0)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = GlassChromeHorizontalPadding,
                end = GlassChromeHorizontalPadding,
                bottom = GlassChromeHorizontalPadding
            )
            .clip(GlassChromeShape)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { GlassChromeShape },
                shadow = null,
                highlight = null,
                effects = {
                    vibrancy()
                    blur(4.dp.toPx())
                    lens(
                        refractionHeight = 22.dp.toPx(),
                        refractionAmount = 72.dp.toPx(),
                        depthEffect = false,
                        chromaticAberration = true
                    )
                },
                onDrawSurface = { drawRect(panelColor) }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(width = 42.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color.White.copy(alpha = 0.42f) else Color.Black.copy(alpha = 0.20f))
            )

            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(GlassChromeInnerShape)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { GlassChromeInnerShape },
                        shadow = null,
                        highlight = null,
                        effects = {
                            vibrancy()
                            blur(4.dp.toPx())
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                lens(
                                    refractionHeight = 8.dp.toPx(),
                                    refractionAmount = 48.dp.toPx(),
                                    depthEffect = false,
                                    chromaticAberration = true
                                )
                            }
                        },
                        onDrawSurface = { drawRect(buttonColor) }
                    )
                    .padding(vertical = 15.dp, horizontal = 16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.add_alert_24dp_ffffff_fill1_wght400_grad0_opsz24),
                        contentDescription = stringResource(R.string.choose_reminder_type),
                        colorFilter = ColorFilter.tint(accentColor),
                        modifier = Modifier
                            .size(32.dp)
                            .padding(bottom = 8.dp)
                    )
                    Text(
                        text = stringResource(id = R.string.choose_reminder_type),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = textColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(id = R.string.reminder_hint_short),
                        style = MaterialTheme.typography.bodySmall,
                        color = mutedColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                GlassOptionCard(
                    backdrop = backdrop,
                    title = stringResource(id = R.string.timer),
                    caption = stringResource(id = R.string.timer_caption_short),
                    iconRes = R.drawable.ic_oui_time,
                    buttonColor = buttonColor,
                    textColor = textColor,
                    mutedColor = mutedColor,
                    accentColor = accentColor,
                    onClick = onTimer,
                    modifier = Modifier.weight(1f)
                )
                GlassOptionCard(
                    backdrop = backdrop,
                    title = stringResource(id = R.string.calendar),
                    caption = stringResource(id = R.string.calendar_caption_short),
                    iconRes = R.drawable.ic_oui_calendar,
                    buttonColor = buttonColor,
                    textColor = textColor,
                    mutedColor = mutedColor,
                    accentColor = accentColor,
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
    val backdrop: Backdrop = rememberLayerBackdrop()

    Box(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(400.dp)
                .layerBackdrop(backdrop as LayerBackdrop)
                .background(Color(0xFF0E0E10))
        ) {
            GlassBackdrop(Modifier.matchParentSize())
        }

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
        Box(
            Modifier
                .matchParentSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF3D4A62), Color(0xFF1C2129))
                    )
                )
        )
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

@Composable
private fun GlassOptionCard(
    backdrop: Backdrop,
    title: String,
    caption: String,
    iconRes: Int,
    buttonColor: Color,
    textColor: Color,
    mutedColor: Color,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "reminderOptionPressScale"
    )
    val pressAlpha by animateFloatAsState(
        targetValue = if (pressed) 0.14f else 0f,
        animationSpec = tween(durationMillis = 120),
        label = "reminderOptionPressAlpha"
    )

    Box(
        modifier = modifier
            .clip(GlassChromeInnerShape)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .drawBackdrop(
                backdrop = backdrop,
                shape = { GlassChromeInnerShape },
                shadow = null,
                highlight = null,
                effects = {
                    vibrancy()
                    blur(4.dp.toPx())
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        lens(
                            refractionHeight = 8.dp.toPx(),
                            refractionAmount = 48.dp.toPx(),
                            depthEffect = false,
                            chromaticAberration = true
                        )
                    }
                },
                onDrawSurface = {
                    drawRect(buttonColor)
                    drawRect(Color.White.copy(alpha = pressAlpha))
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            )
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
                    .background(accentColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = title,
                    colorFilter = ColorFilter.tint(accentColor),
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = textColor,
                modifier = Modifier.padding(top = 10.dp)
            )
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = mutedColor,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
