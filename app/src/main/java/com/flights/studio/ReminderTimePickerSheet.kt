package com.flights.studio

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class ReminderInfo(
    val note: String,
    val triggerAtMillis: Long
)

@Composable
fun ReminderTimePickerSheet(
    visible: Boolean,
    backdrop: Backdrop,
    onDismiss: () -> Unit,
    onSetReminder: (hourOfDay: Int, minute: Int, dayOffset: Int) -> Boolean
) {
    val view = LocalView.current
    val isDark = isSystemInDarkTheme()
    val panelColor = if (isDark) {
        Color(0xFF202124).copy(alpha = 0.62f)
    } else {
        Color(0xFFE6E2E7).copy(alpha = 0.52f)
    }
    val textColor = if (isDark) Color.White.copy(alpha = 0.92f) else Color(0xFF1E1F24)
    val mutedColor = if (isDark) Color.White.copy(alpha = 0.62f) else Color(0xFF5A5D66)
    val buttonColor = if (isDark) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.96f)
    val accentColor = if (isDark) Color(0xFF8EC5FF) else Color(0xFF0B57D0)
    val successColor = if (isDark) Color(0xFF75E0A1) else Color(0xFF137333)
    val errorColor = if (isDark) Color(0xFFFF9C9C) else Color(0xFFB3261E)

    var hour24 by remember(visible) {
        val calendar = Calendar.getInstance().apply { add(Calendar.MINUTE, 5) }
        mutableIntStateOf(calendar.get(Calendar.HOUR_OF_DAY))
    }
    var minute by remember(visible) {
        val calendar = Calendar.getInstance().apply { add(Calendar.MINUTE, 5) }
        mutableIntStateOf(calendar.get(Calendar.MINUTE))
    }
    var selectedDayOffset by remember(visible) { mutableIntStateOf(0) }
    var status by remember(visible) { mutableStateOf<ReminderTimeStatus>(ReminderTimeStatus.Idle) }

    LaunchedEffect(status) {
        if (status == ReminderTimeStatus.Success) {
            delay(1050)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 90)),
        exit = fadeOut(animationSpec = tween(durationMillis = 140))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (isDark) 0.38f else 0.18f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )
    }

    AnimatedVisibility(
        visible = visible,
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        enter = slideInVertically(
            animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
            initialOffsetY = { it / 2 }
        ) + fadeIn(animationSpec = tween(durationMillis = 120)) +
            scaleIn(
                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                initialScale = 0.96f
            ),
        exit = slideOutVertically(
            animationSpec = tween(durationMillis = 170, easing = FastOutLinearInEasing),
            targetOffsetY = { it / 3 }
        ) + fadeOut(animationSpec = tween(durationMillis = 120)) +
            scaleOut(
                animationSpec = tween(durationMillis = 170, easing = FastOutLinearInEasing),
                targetScale = 0.98f
            )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .padding(
                        start = GlassChromeHorizontalPadding,
                        end = GlassChromeHorizontalPadding,
                        bottom = GlassChromeHorizontalPadding
                    )
                    .fillMaxWidth()
                    .clip(GlassChromeShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .adaptiveLiquidGlassBackdrop(
                        backdrop = backdrop,
                        shape = GlassChromeShape,
                        surfaceColor = panelColor,
                        blurDp = 4f,
                        shadow = null,
                        refractionHeightDp = 22f,
                        refractionAmountDp = 72f,
                        chromaticAberration = true,
                        onDrawExtraSurface = {
                            val glowColor = when (status) {
                                ReminderTimeStatus.Success -> successColor.copy(alpha = if (isDark) 0.22f else 0.16f)
                                ReminderTimeStatus.Error -> errorColor.copy(alpha = if (isDark) 0.12f else 0.08f)
                                ReminderTimeStatus.Idle -> Color.Transparent
                            }
                            drawRect(glowColor)
                        }
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(42.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color.White.copy(alpha = 0.42f) else Color.Black.copy(alpha = 0.20f))
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = buttonColor,
                                contentColor = accentColor,
                                tonalElevation = 0.dp,
                                shadowElevation = 0.dp
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .size(22.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Choose reminder time",
                                    color = textColor,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.2.sp
                                    )
                                )
                                Text(
                                    text = if (selectedDayOffset == 0) "Today" else "Tomorrow",
                                    color = mutedColor,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }

                        SheetIconButton(
                            color = buttonColor,
                            contentColor = textColor,
                            onClick = onDismiss
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Cancel")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ActionPill(
                            label = "Today",
                            color = if (selectedDayOffset == 0) accentColor.copy(alpha = if (isDark) 0.30f else 0.14f) else buttonColor,
                            contentColor = if (selectedDayOffset == 0) accentColor else textColor,
                            onClick = {
                                view.performReminderTick()
                                selectedDayOffset = 0
                                status = ReminderTimeStatus.Idle
                            },
                            modifier = Modifier.weight(1f)
                        )
                        ActionPill(
                            label = "Tomorrow",
                            color = if (selectedDayOffset == 1) accentColor.copy(alpha = if (isDark) 0.30f else 0.14f) else buttonColor,
                            contentColor = if (selectedDayOffset == 1) accentColor else textColor,
                            onClick = {
                                view.performReminderTick()
                                selectedDayOffset = 1
                                status = ReminderTimeStatus.Idle
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TimeValuePicker(
                            label = "Hour",
                            value = displayHour(hour24).toString().padStart(2, '0'),
                            textColor = textColor,
                            mutedColor = mutedColor,
                            buttonColor = buttonColor,
                            onUp = {
                                view.performReminderTick()
                                hour24 = (hour24 + 1) % 24
                                status = ReminderTimeStatus.Idle
                            },
                            onDown = {
                                view.performReminderTick()
                                hour24 = (hour24 + 23) % 24
                                status = ReminderTimeStatus.Idle
                            },
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = ":",
                            color = textColor,
                            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black)
                        )
                        TimeValuePicker(
                            label = "Minute",
                            value = minute.toString().padStart(2, '0'),
                            textColor = textColor,
                            mutedColor = mutedColor,
                            buttonColor = buttonColor,
                            onUp = {
                                view.performReminderTick()
                                minute = (minute + 1) % 60
                                status = ReminderTimeStatus.Idle
                            },
                            onDown = {
                                view.performReminderTick()
                                minute = (minute + 59) % 60
                                status = ReminderTimeStatus.Idle
                            },
                            modifier = Modifier.weight(1f)
                        )
                        AmPmToggle(
                            isPm = hour24 >= 12,
                            textColor = textColor,
                            mutedColor = mutedColor,
                            buttonColor = buttonColor,
                            accentColor = accentColor,
                            onToggle = {
                                view.performReminderTick()
                                hour24 = (hour24 + 12) % 24
                                status = ReminderTimeStatus.Idle
                            }
                        )
                    }

                    val statusText = when (status) {
                        ReminderTimeStatus.Idle -> if (selectedDayOffset == 0) {
                            "Pick a future time for today."
                        } else {
                            "Reminder will be set for tomorrow."
                        }
                        ReminderTimeStatus.Error -> "Selected time is in the past."
                        ReminderTimeStatus.Success -> "Reminder set."
                    }
                    Text(
                        text = statusText,
                        modifier = Modifier.fillMaxWidth(),
                        color = when (status) {
                            ReminderTimeStatus.Idle -> mutedColor
                            ReminderTimeStatus.Error -> errorColor
                            ReminderTimeStatus.Success -> successColor
                        },
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ActionPill(
                            label = "Cancel",
                            color = buttonColor,
                            contentColor = textColor,
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        )
                        ActionPill(
                            label = if (status == ReminderTimeStatus.Success) "Set" else "Set reminder",
                            color = if (status == ReminderTimeStatus.Success) successColor.copy(alpha = 0.22f) else accentColor.copy(alpha = if (isDark) 0.30f else 0.14f),
                            contentColor = if (status == ReminderTimeStatus.Success) successColor else accentColor,
                            onClick = {
                                val set = onSetReminder(hour24, minute, selectedDayOffset)
                                status = if (set) {
                                    view.performReminderSetWow()
                                    ReminderTimeStatus.Success
                                } else {
                                    view.performReminderReject()
                                    ReminderTimeStatus.Error
                                }
                            },
                            modifier = Modifier.weight(1f),
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReminderDetailsSheet(
    info: ReminderInfo?,
    backdrop: Backdrop,
    onDismiss: () -> Unit,
    onEdit: (ReminderInfo) -> Unit,
    onCancelReminder: (ReminderInfo) -> Unit
) {
    val visible = info != null
    val isDark = isSystemInDarkTheme()
    val panelColor = if (isDark) {
        Color(0xFF202124).copy(alpha = 0.62f)
    } else {
        Color(0xFFE6E2E7).copy(alpha = 0.52f)
    }
    val textColor = if (isDark) Color.White.copy(alpha = 0.92f) else Color(0xFF1E1F24)
    val mutedColor = if (isDark) Color.White.copy(alpha = 0.62f) else Color(0xFF5A5D66)
    val buttonColor = if (isDark) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.96f)
    val accentColor = if (isDark) Color(0xFF8EC5FF) else Color(0xFF0B57D0)
    val dangerColor = if (isDark) Color(0xFFFF9C9C) else Color(0xFFB3261E)
    var now by remember(visible) { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(visible) {
        while (visible) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 90)),
        exit = fadeOut(animationSpec = tween(durationMillis = 140))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (isDark) 0.38f else 0.18f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )
    }

    AnimatedVisibility(
        visible = visible,
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        enter = slideInVertically(
            animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
            initialOffsetY = { it / 2 }
        ) + fadeIn(animationSpec = tween(durationMillis = 120)) +
            scaleIn(
                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                initialScale = 0.96f
            ),
        exit = slideOutVertically(
            animationSpec = tween(durationMillis = 170, easing = FastOutLinearInEasing),
            targetOffsetY = { it / 3 }
        ) + fadeOut(animationSpec = tween(durationMillis = 120)) +
            scaleOut(
                animationSpec = tween(durationMillis = 170, easing = FastOutLinearInEasing),
                targetScale = 0.98f
            )
    ) {
        val currentInfo = info ?: return@AnimatedVisibility
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .padding(
                        start = GlassChromeHorizontalPadding,
                        end = GlassChromeHorizontalPadding,
                        bottom = GlassChromeHorizontalPadding
                    )
                    .fillMaxWidth()
                    .clip(GlassChromeShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .adaptiveLiquidGlassBackdrop(
                        backdrop = backdrop,
                        shape = GlassChromeShape,
                        surfaceColor = panelColor,
                        blurDp = 4f,
                        shadow = null,
                        refractionHeightDp = 22f,
                        refractionAmountDp = 72f,
                        chromaticAberration = true
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(42.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color.White.copy(alpha = 0.42f) else Color.Black.copy(alpha = 0.20f))
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = buttonColor,
                                contentColor = accentColor,
                                tonalElevation = 0.dp,
                                shadowElevation = 0.dp
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .size(22.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Reminder set",
                                    color = textColor,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.2.sp
                                    )
                                )
                                Text(
                                    text = reminderDayLabel(currentInfo.triggerAtMillis, now),
                                    color = mutedColor,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }

                        SheetIconButton(
                            color = buttonColor,
                            contentColor = textColor,
                            onClick = onDismiss
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Close")
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = GlassChromeInnerShape,
                        color = buttonColor,
                        contentColor = textColor,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = reminderCountdown(currentInfo.triggerAtMillis - now),
                                color = accentColor,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    textAlign = TextAlign.Center
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "Alarm at ${formatReminderTime(currentInfo.triggerAtMillis)}",
                                color = textColor,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    textAlign = TextAlign.Center
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "About: ${currentInfo.note.take(160)}",
                                color = mutedColor,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ActionPill(
                            label = "Cancel alarm",
                            color = dangerColor.copy(alpha = if (isDark) 0.22f else 0.12f),
                            contentColor = dangerColor,
                            onClick = { onCancelReminder(currentInfo) },
                            modifier = Modifier.weight(1f),
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                        ActionPill(
                            label = "Edit time",
                            color = accentColor.copy(alpha = if (isDark) 0.30f else 0.14f),
                            contentColor = accentColor,
                            onClick = { onEdit(currentInfo) },
                            modifier = Modifier.weight(1f),
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeValuePicker(
    label: String,
    value: String,
    textColor: Color,
    mutedColor: Color,
    buttonColor: Color,
    onUp: () -> Unit,
    onDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SheetIconButton(color = buttonColor, contentColor = textColor, onClick = onUp) {
            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "$label up")
        }
        Text(
            text = value,
            color = textColor,
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 0.sp,
                textAlign = TextAlign.Center
            )
        )
        Text(
            text = label,
            color = mutedColor,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
        )
        SheetIconButton(color = buttonColor, contentColor = textColor, onClick = onDown) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "$label down")
        }
    }
}

@Composable
private fun AmPmToggle(
    isPm: Boolean,
    textColor: Color,
    mutedColor: Color,
    buttonColor: Color,
    accentColor: Color,
    onToggle: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ToggleSegment(
            label = "AM",
            selected = !isPm,
            buttonColor = buttonColor,
            selectedColor = accentColor,
            contentColor = textColor,
            mutedColor = mutedColor,
            onClick = { if (isPm) onToggle() }
        )
        ToggleSegment(
            label = "PM",
            selected = isPm,
            buttonColor = buttonColor,
            selectedColor = accentColor,
            contentColor = textColor,
            mutedColor = mutedColor,
            onClick = { if (!isPm) onToggle() }
        )
    }
}

@Composable
private fun ToggleSegment(
    label: String,
    selected: Boolean,
    buttonColor: Color,
    selectedColor: Color,
    contentColor: Color,
    mutedColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val baseColor = if (selected) selectedColor.copy(alpha = 0.24f) else buttonColor
    val animatedColor by animateColorAsState(
        targetValue = if (pressed) pressLift(baseColor) else baseColor,
        animationSpec = tween(durationMillis = 120),
        label = "togglePressColor"
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "togglePressScale"
    )

    Surface(
        modifier = Modifier
            .width(58.dp)
            .height(42.dp)
            .clip(GlassChromeInnerShape)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = GlassChromeInnerShape,
        color = animatedColor,
        contentColor = if (selected) selectedColor else mutedColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = if (selected) selectedColor else contentColor.copy(alpha = 0.65f),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black)
            )
        }
    }
}

@Composable
private fun SheetIconButton(
    color: Color,
    contentColor: Color,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val animatedColor by animateColorAsState(
        targetValue = if (pressed) pressLift(color) else color,
        animationSpec = tween(durationMillis = 120),
        label = "iconPressColor"
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "iconPressScale"
    )

    Surface(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = CircleShape,
        color = animatedColor,
        contentColor = contentColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}

@Composable
private fun ActionPill(
    label: String,
    color: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val animatedColor by animateColorAsState(
        targetValue = if (pressed) pressLift(color) else color,
        animationSpec = tween(durationMillis = 120),
        label = "pillPressColor"
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "pillPressScale"
    )

    Surface(
        modifier = modifier
            .height(48.dp)
            .clip(GlassChromeInnerShape)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = GlassChromeInnerShape,
        color = animatedColor,
        contentColor = contentColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = label,
                color = contentColor,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black)
            )
        }
    }
}

private enum class ReminderTimeStatus {
    Idle,
    Error,
    Success
}

private fun displayHour(hour24: Int): Int {
    val hour = hour24 % 12
    return if (hour == 0) 12 else hour
}

private fun pressLift(color: Color, extraAlpha: Float = 0.12f): Color =
    color.copy(alpha = (color.alpha + extraAlpha).coerceAtMost(1f))

private fun reminderCountdown(remainingMillis: Long): String {
    if (remainingMillis <= 0L) return "Due now"
    val totalMinutes = remainingMillis / 60_000L
    val days = totalMinutes / (24 * 60)
    val hours = (totalMinutes % (24 * 60)) / 60
    val minutes = totalMinutes % 60
    return when {
        days > 0 -> "${days}d ${hours}h ${minutes}m remaining"
        hours > 0 -> "${hours}h ${minutes}m remaining"
        minutes > 0 -> "${minutes}m remaining"
        else -> "Less than 1m remaining"
    }
}

private fun formatReminderTime(triggerAtMillis: Long): String =
    SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault()).format(Date(triggerAtMillis))

private fun reminderDayLabel(triggerAtMillis: Long, nowMillis: Long): String {
    val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
    val trigger = Calendar.getInstance().apply { timeInMillis = triggerAtMillis }
    val tomorrow = Calendar.getInstance().apply {
        timeInMillis = nowMillis
        add(Calendar.DAY_OF_YEAR, 1)
    }
    return when {
        now.get(Calendar.YEAR) == trigger.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == trigger.get(Calendar.DAY_OF_YEAR) -> "Today"
        tomorrow.get(Calendar.YEAR) == trigger.get(Calendar.YEAR) &&
            tomorrow.get(Calendar.DAY_OF_YEAR) == trigger.get(Calendar.DAY_OF_YEAR) -> "Tomorrow"
        else -> SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(triggerAtMillis))
    }
}

private fun android.view.View.performReminderTick() {
    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
}

private fun android.view.View.performReminderConfirm() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    } else {
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }
}

private fun android.view.View.performReminderReject() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        performHapticFeedback(HapticFeedbackConstants.REJECT)
    } else {
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }
}

private fun android.view.View.performReminderSetWow() {
    performReminderConfirm()
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    if (!vibrator.hasVibrator()) return

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(
            VibrationEffect.createWaveform(
                longArrayOf(0, 18, 34, 42, 42, 74),
                intArrayOf(0, 80, 0, 130, 0, 205),
                -1
            )
        )
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(longArrayOf(0, 22, 38, 54), -1)
    }
}
