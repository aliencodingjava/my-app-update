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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.runtime.key
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.time.Duration.Companion.milliseconds
import androidx.compose.ui.platform.LocalLocale

data class ReminderInfo(
    val note: String,
    val triggerAtMillis: Long
)

private const val ReminderDayWheelMaxOffsetDays = 365

private data class ReminderTodoTemplate(
    val title: String,
    val subtitle: String,
    val badge: String,
    val icon: String,
    val sections: List<ReminderTodoSection>
)

private data class ReminderTodoSection(
    val title: String,
    val items: List<ReminderTodoItem>
)

private data class ReminderTodoItem(
    val text: String,
    val checked: Boolean = false,
    val checkedAt: String? = null
)

private val ReminderCheckedAtRegex = Regex("""\s+\[checked (.+)]$""")

private val ReminderTodoTemplateHeaders = setOf(
    "planner", "idea", "priority", "shopping", "shopping list", "location", "location task",
    "reminder", "reminder plan", "photo", "photo checklist", "voice note", "voice note plan",
    "attachment", "attachment checklist", "tags", "progress", "progress tracker", "goal",
    "brainstorm", "project", "project checklist", "bug fix", "bug fix checklist",
    "airport shift", "airport shift checklist", "meeting", "meeting notes", "travel prep"
)

private fun reminderIsTodoTemplateHeader(line: String): Boolean =
    line.trim().removeSuffix(":").lowercase(Locale.getDefault()) in ReminderTodoTemplateHeaders

private fun reminderTodoItemFromLine(line: String): ReminderTodoItem? {
    val trimmedStart = line.trimStart()
    val marker = trimmedStart.firstOrNull() ?: return null
    val checked = when (marker) {
        '☑', '✓', '✔', '●', '◉' -> true
        '□', '○' -> false
        else -> return null
    }
    val rawText = trimmedStart.drop(1).removePrefix(" ")
    val checkedAt = ReminderCheckedAtRegex.find(rawText)?.groupValues?.getOrNull(1)
    return ReminderTodoItem(
        text = rawText.replace(ReminderCheckedAtRegex, "").removeSuffix(":").trim(),
        checked = checked,
        checkedAt = checkedAt
    )
}

private fun reminderTodoTemplateOrNull(note: String): ReminderTodoTemplate? {
    val lines = note.lines().filter { it.trim().isNotBlank() }
    if (lines.count { reminderTodoItemFromLine(it) != null } < 2) return null

    val header = lines.firstOrNull()?.trim().orEmpty().ifBlank { "Checklist" }
    val titleKey = header.lowercase(Locale.getDefault())
    val meta = when {
        "priority" in titleKey -> Triple("Separate urgent work from nice-to-have noise.", "Focus", "★")
        "idea" in titleKey -> Triple("Capture the problem, why it matters, and the first test.", "Create", "?")
        "planner" in titleKey || "daily" in titleKey -> Triple("Time blocks, errands, waiting items, tomorrow carry-over.", "Daily", "17")
        "reminder" in titleKey -> Triple("Time, prep, action, and follow-up in one note.", "Time", "!")
        "photo" in titleKey -> Triple("Shot list so photos are not random or missing context.", "Media", "#")
        "voice" in titleKey -> Triple("Turn a recording into clear actions and details.", "Audio", "V")
        "attachment" in titleKey || "file" in titleKey -> Triple("Track why a file or link matters.", "File", "@")
        else -> Triple("Organized tasks and follow-up items.", "List", "✓")
    }

    val sections = mutableListOf<ReminderTodoSection>()
    var currentTitle: String? = null
    val currentItems = mutableListOf<ReminderTodoItem>()

    fun flush() {
        val sectionTitle = currentTitle?.takeIf { it.isNotBlank() } ?: return
        sections += ReminderTodoSection(sectionTitle, currentItems.toList())
        currentItems.clear()
    }

    lines.drop(1).forEach { line ->
        val item = reminderTodoItemFromLine(line)
        if (item != null) {
            currentItems += item
        } else {
            flush()
            currentTitle = line.removeSuffix(":").trim()
        }
    }
    flush()

    return ReminderTodoTemplate(
        title = header.removeSuffix(":").ifBlank { "Checklist" },
        subtitle = meta.first,
        badge = meta.second,
        icon = meta.third,
        sections = sections.ifEmpty {
            listOf(
                ReminderTodoSection(
                    title = header.removeSuffix(":").ifBlank { "Checklist" },
                    items = lines.mapNotNull { reminderTodoItemFromLine(it) }
                )
            )
        }
    )
}

@Composable
private fun ReminderNotePreview(
    note: String,
    textColor: Color,
    mutedColor: Color,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val scheme = MaterialTheme.colorScheme
    val template = remember(note) { reminderTodoTemplateOrNull(note) }

    if (template == null) {
        Text(
            text = "About: ${note.take(160)}",
            color = mutedColor,
            style = MaterialTheme.typography.bodyMedium,
            modifier = modifier.fillMaxWidth()
        )
        return
    }

    val accent = when (template.badge.lowercase(LocalLocale.current.platformLocale)) {
        "focus" -> scheme.tertiary
        "create" -> scheme.primary
        "daily" -> scheme.primary
        else -> scheme.secondary
    }
    val sectionAccents = listOf(
        accent,
        scheme.error,
        scheme.tertiary,
        Color(0xFF5B8DFF),
        Color(0xFF24B39B)
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 330.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(15.dp),
                color = accent.copy(alpha = if (isDark) 0.20f else 0.14f),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.34f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = template.icon,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = accent,
                        maxLines = 1
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = template.title.uppercase(LocalLocale.current.platformLocale),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${template.badge.uppercase(LocalLocale.current.platformLocale)} · ${template.subtitle}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = mutedColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        template.sections.forEachIndexed { index, section ->
            val sectionAccent = sectionAccents[index % sectionAccents.size]
            if (index > 0 && reminderIsTodoTemplateHeader(section.title)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(mutedColor.copy(alpha = if (isDark) 0.28f else 0.18f))
                )
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = sectionAccent.copy(alpha = if (isDark) 0.13f else 0.09f),
                border = BorderStroke(1.dp, sectionAccent.copy(alpha = if (isDark) 0.24f else 0.16f))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 9.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(24.dp),
                            shape = RoundedCornerShape(9.dp),
                            color = sectionAccent.copy(alpha = if (isDark) 0.24f else 0.16f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = (index + 1).toString().padStart(2, '0'),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                    color = sectionAccent,
                                    maxLines = 1
                                )
                            }
                        }
                        Text(
                            text = section.title.uppercase(LocalLocale.current.platformLocale),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                            color = sectionAccent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    section.items.ifEmpty { listOf(ReminderTodoItem("")) }.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 28.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(18.dp),
                                shape = CircleShape,
                                color = if (item.checked) sectionAccent.copy(alpha = 0.24f) else Color.Transparent,
                                border = BorderStroke(
                                    1.3.dp,
                                    if (item.checked) sectionAccent else mutedColor.copy(alpha = if (isDark) 0.68f else 0.42f)
                                )
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (item.checked) {
                                        Text(
                                            text = "✓",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                            color = sectionAccent,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                            Text(
                                text = item.text.ifBlank { "Task here..." },
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (item.text.isBlank()) mutedColor.copy(alpha = 0.58f) else textColor.copy(alpha = 0.88f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            item.checkedAt?.takeIf { item.checked && it.isNotBlank() }?.let { checkedAt ->
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = sectionAccent.copy(alpha = if (isDark) 0.18f else 0.10f),
                                    border = BorderStroke(1.dp, sectionAccent.copy(alpha = if (isDark) 0.26f else 0.16f))
                                ) {
                                    Text(
                                        text = checkedAt,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                        color = sectionAccent,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReminderTimePickerSheet(
    visible: Boolean,
    backdrop: Backdrop,
    note: String? = null,
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

    val initialReminderTime = remember(visible) { Calendar.getInstance() }
    var dayWheelAnchorMillis by remember(visible) {
        mutableLongStateOf(initialReminderTime.timeInMillis)
    }
    val dayWheelAnchor = remember(dayWheelAnchorMillis) {
        Calendar.getInstance().apply { timeInMillis = dayWheelAnchorMillis }
    }

    var hour24 by remember(visible) {
        mutableIntStateOf(initialReminderTime.get(Calendar.HOUR_OF_DAY))
    }

    var minute by remember(visible) {
        mutableIntStateOf(initialReminderTime.get(Calendar.MINUTE))
    }
    val pickerOpenKey = remember(visible) { if (visible) System.nanoTime() else 0L }
    val dayWheelLabels = remember(dayWheelAnchorMillis) { reminderDayWheelLabels(dayWheelAnchor) }
    var selectedDayOffset by remember(visible) { mutableIntStateOf(0) }
    val selectedDayLabel = dayWheelLabels.getOrElse(selectedDayOffset) {
        reminderDayLabel(dayWheelAnchorMillis)
    }
    var status by remember(visible) { mutableStateOf(ReminderTimeStatus.Idle) }

    LaunchedEffect(status) {
        if (status == ReminderTimeStatus.Success) {
            delay(1050.milliseconds)
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
                        blurDp = 6f,
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
                        .padding(horizontal = 10.dp, vertical = 18.dp),
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
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ReminderHeaderResetPill(
                            color = buttonColor,
                            contentColor = accentColor,
                            onClick = {
                                val nowCalendar = Calendar.getInstance()
                                view.performReminderConfirm()
                                dayWheelAnchorMillis = nowCalendar.timeInMillis
                                selectedDayOffset = 0
                                hour24 = nowCalendar.get(Calendar.HOUR_OF_DAY)
                                minute = nowCalendar.get(Calendar.MINUTE)
                                status = ReminderTimeStatus.Idle
                            }
                        )

                        ReminderHeaderTitlePill(
                            title = "Schedule time",
                            subtitle = selectedDayLabel,
                            color = buttonColor,
                            titleColor = textColor,
                            subtitleColor = mutedColor,
                            modifier = Modifier.weight(1f)
                        )

                        SheetIconButton(
                            color = buttonColor,
                            contentColor = textColor,
                            onClick = onDismiss
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Cancel")
                        }
                    }

                    ReminderWheelTimePicker(
                        dayOffset = selectedDayOffset,
                        dayValues = dayWheelLabels,
                        hour24 = hour24,
                        minute = minute,
                        openKey = pickerOpenKey,
                        textColor = textColor,
                        mutedColor = mutedColor,
                        buttonColor = buttonColor,
                        accentColor = accentColor,
                        onDayOffsetChange = { value ->
                            view.performReminderTick()
                            selectedDayOffset = value
                            status = ReminderTimeStatus.Idle
                        },
                        onHour24Change = { value ->
                            view.performReminderTick()
                            hour24 = value
                            status = ReminderTimeStatus.Idle
                        },
                        onMinuteChange = { value ->
                            view.performReminderTick()
                            minute = value
                            status = ReminderTimeStatus.Idle
                        },
                        onAmPmChange = { pm ->
                            view.performReminderTick()
                            val currentIsPm = hour24 >= 12
                            if (pm != currentIsPm) {
                                hour24 = (hour24 + 12) % 24
                            }
                            status = ReminderTimeStatus.Idle
                        }
                    )

                    note?.takeIf { it.isNotBlank() }?.let { noteText ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = GlassChromeInnerShape,
                            color = buttonColor,
                            contentColor = textColor,
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp
                        ) {
                            ReminderNotePreview(
                                note = noteText,
                                textColor = textColor,
                                mutedColor = mutedColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }
                    }

                    val statusText = when (status) {
                        ReminderTimeStatus.Idle -> "Reminder will be set for $selectedDayLabel."
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
                            color = if (status == ReminderTimeStatus.Success) {
                                successColor.copy(alpha = if (isDark) 0.34f else 0.24f)
                            } else {
                                accentColor.copy(alpha = if (isDark) 0.46f else 0.26f)
                            },
                            contentColor = if (status == ReminderTimeStatus.Success) {
                                successColor
                            } else if (isDark) {
                                Color.White
                            } else {
                                accentColor
                            },
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
            delay(1000.milliseconds)
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
                        blurDp = 6f,
                        shadow = null,
                        refractionHeightDp = 22f,
                        refractionAmountDp = 72f,
                        chromaticAberration = true
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 18.dp),
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
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ReminderHeaderAnalogClockIcon(
                            nowMillis = now,
                            color = buttonColor,
                            accentColor = accentColor,
                            handColor = textColor
                        )

                        ReminderHeaderTitlePill(
                            title = "Reminder set",
                            subtitle = reminderDayLabel(currentInfo.triggerAtMillis),
                            color = buttonColor,
                            titleColor = textColor,
                            subtitleColor = mutedColor,
                            modifier = Modifier.weight(1f)
                        )

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
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 14.dp),
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
                            ReminderNotePreview(
                                note = currentInfo.note,
                                textColor = textColor,
                                mutedColor = mutedColor,
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
fun ReminderNotificationScreen(
    note: String,
    onHome: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val background = if (isDark) Color(0xFF0E1014) else Color(0xFFF2F6FF)
    val panelColor = if (isDark) Color(0xFF202124) else Color.White
    val textColor = if (isDark) Color.White.copy(alpha = 0.92f) else Color(0xFF1E1F24)
    val mutedColor = if (isDark) Color.White.copy(alpha = 0.62f) else Color(0xFF5A5D66)
    val accentColor = if (isDark) Color(0xFF8EC5FF) else Color(0xFF0B57D0)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = accentColor.copy(alpha = if (isDark) 0.24f else 0.12f),
                    contentColor = accentColor,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Schedule, contentDescription = null)
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Reminder",
                        color = textColor,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Your reminder note",
                        color = mutedColor,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = GlassChromeShape,
                color = panelColor.copy(alpha = if (isDark) 0.72f else 0.92f),
                border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.08f)),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                ReminderNotePreview(
                    note = note,
                    textColor = textColor,
                    mutedColor = mutedColor,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                )
            }

            ActionPill(
                label = "Home",
                color = accentColor.copy(alpha = if (isDark) 0.30f else 0.14f),
                contentColor = accentColor,
                onClick = onHome,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(
                        Icons.Filled.Home,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
    }
}

@Composable
private fun ReminderWheelTimePicker(
    dayOffset: Int,
    dayValues: List<String>,
    hour24: Int,
    minute: Int,
    openKey: Long,
    textColor: Color,
    mutedColor: Color,
    buttonColor: Color,
    accentColor: Color,
    onDayOffsetChange: (Int) -> Unit,
    onHour24Change: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    onAmPmChange: (Boolean) -> Unit
) {
    val isDark = isSystemInDarkTheme()

    val rowHeight = 50.dp
    val visibleRows = 5

    // IMPORTANT:
    // 5 rows × 50dp = 250dp.
    // The wheel viewport and its center math now match exactly.
    val pickerHeight = rowHeight * visibleRows

    val scheduleDayValues = remember(dayValues) {
        dayValues.ifEmpty { listOf(reminderDayLabel(System.currentTimeMillis())) }
    }

    val hourValues = remember {
        (1..12).map { it.toString() }
    }

    // Cleaner ordering: 00, 01, 02 ... 59
    val minuteValues = remember {
        (0..59).map { it.toString().padStart(2, '0') }
    }

    val amPmValues = remember {
        listOf("AM", "PM")
    }

    val selectedDayIndex =
        dayOffset.coerceIn(0, scheduleDayValues.lastIndex.coerceAtLeast(0))

    val selectedHourIndex =
        (displayHour(hour24) - 1).coerceIn(0, 11)

    val selectedMinuteIndex =
        minute.coerceIn(0, 59)

    val selectedAmPmIndex =
        if (hour24 >= 12) 1 else 0

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(pickerHeight),
        shape = RoundedCornerShape(28.dp),
        color = buttonColor.copy(
            alpha = if (isDark) 0.10f else 0.58f
        ),
        border = BorderStroke(
            1.dp,
            accentColor.copy(
                alpha = if (isDark) 0.18f else 0.12f
            )
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {

            // EXACT center selection row.
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(rowHeight)
                    .padding(horizontal = 8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        accentColor.copy(
                            alpha = if (isDark) 0.18f else 0.10f
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {

                // DAY
                ReminderWheelColumn(
                    values = scheduleDayValues,
                    selectedIndex = selectedDayIndex,
                    selectedTextColor = accentColor,
                    mutedTextColor = mutedColor,
                    itemHeight = rowHeight,
                    openKey = openKey,
                    large = false,
                    modifier = Modifier.weight(1.2f),
                    onSelected = { index ->
                        if (index != dayOffset) {
                            onDayOffsetChange(index)
                        }
                    }
                )

                // HOUR
                ReminderWheelColumn(
                    values = hourValues,
                    selectedIndex = selectedHourIndex,
                    selectedTextColor = textColor,
                    mutedTextColor = mutedColor,
                    itemHeight = rowHeight,
                    openKey = openKey,
                    large = true,
                    modifier = Modifier.weight(0.75f),
                    onSelected = { index ->

                        val hour12 = index + 1

                        val nextHour24 = when {
                            hour24 >= 12 && hour12 == 12 -> 12
                            hour24 >= 12 -> hour12 + 12
                            hour12 == 12 -> 0
                            else -> hour12
                        }

                        if (nextHour24 != hour24) {
                            onHour24Change(nextHour24)
                        }
                    }
                )

                // :
                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .height(rowHeight),
                    contentAlignment = Alignment.Center
                ) {
                    ReminderWheelSeparator(
                        textColor = textColor
                    )
                }

                // MINUTE
                ReminderWheelColumn(
                    values = minuteValues,
                    selectedIndex = selectedMinuteIndex,
                    selectedTextColor = textColor,
                    mutedTextColor = mutedColor,
                    itemHeight = rowHeight,
                    openKey = openKey,
                    large = true,
                    modifier = Modifier.weight(0.75f),
                    onSelected = { index ->
                        if (index != minute) {
                            onMinuteChange(index)
                        }
                    }
                )

                // AM / PM
                ReminderWheelColumn(
                    values = amPmValues,
                    selectedIndex = selectedAmPmIndex,
                    selectedTextColor =
                        if (isDark) Color.White
                        else accentColor,
                    mutedTextColor = mutedColor,
                    itemHeight = rowHeight,
                    openKey = openKey,
                    large = false,
                    modifier = Modifier.width(58.dp),
                    onSelected = { index ->
                        val isPm = index == 1

                        if (isPm != (hour24 >= 12)) {
                            onAmPmChange(isPm)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ReminderWheelColumn(
    values: List<String>,
    selectedIndex: Int,
    selectedTextColor: Color,
    mutedTextColor: Color,
    itemHeight: Dp,
    openKey: Long,
    large: Boolean,
    modifier: Modifier = Modifier,
    onSelected: (Int) -> Unit
) {
    val visibleRows = 5
    val centerRow = visibleRows / 2
    val density = LocalDensity.current
    val view = LocalView.current

    val cycles = 80
    val valuesSize = values.size.coerceAtLeast(1)

    val safeSelectedIndex =
        selectedIndex.coerceIn(
            0,
            valuesSize - 1
        )

    key(
        openKey,
        valuesSize
    ) {

        /*
         * Put the selected value roughly in the middle
         * of our repeated wheel.
         *
         * Example:
         * Hour = 10
         *
         * ... 8
         *     9
         *    10  <- CENTER
         *    11
         *    12 ...
         */
        val startIndex = remember(
            openKey,
            valuesSize
        ) {
            (cycles / 2) *
                    valuesSize +
                    safeSelectedIndex
        }

        /*
         * IMPORTANT:
         *
         * Because we have two rows of contentPadding
         * above the first visible item,
         * the initial firstVisibleItemIndex is actually
         * positioned in the physical CENTER.
         */
        val state =
            rememberLazyListState(
                initialFirstVisibleItemIndex =
                    startIndex,

                initialFirstVisibleItemScrollOffset =
                    0
            )

        val snapFlingBehavior =
            rememberSnapFlingBehavior(
                lazyListState = state,
                snapPosition = SnapPosition.Center
            )

        /*
         * This is normal Compose state.
         *
         * We update it only when a different whole item
         * becomes centered.
         *
         * We do NOT read state.layoutInfo directly
         * during item composition anymore.
         */
        var centeredItemIndex by remember(
            state
        ) {
            mutableIntStateOf(
                startIndex
            )
        }

        /*
         * Track the item physically closest
         * to the center of the picker.
         *
         * snapshotFlow is the correct place to read
         * frequently-changing LazyListState.layoutInfo.
         */
        LaunchedEffect(
            state,
            valuesSize
        ) {

            snapshotFlow {

                val layoutInfo =
                    state.layoutInfo

                val center =
                    (
                            layoutInfo
                                .viewportStartOffset +
                                    layoutInfo
                                        .viewportEndOffset
                            ) / 2

                layoutInfo
                    .visibleItemsInfo
                    .minByOrNull { item ->

                        abs(
                            (
                                    item.offset +
                                            item.size / 2
                                    ) -
                                    center
                        )
                    }
                    ?.index
            }
                .distinctUntilChanged()
                .collect { absoluteIndex ->

                    if (
                        absoluteIndex == null
                    ) {
                        return@collect
                    }

                    centeredItemIndex =
                        absoluteIndex

                    val normalized =
                        floorMod(
                            absoluteIndex,
                            valuesSize
                        )

                    /*
                     * Update the actual selected
                     * hour/minute/AM-PM.
                     */
                    if (
                        normalized !=
                        safeSelectedIndex
                    ) {

                        view.performReminderTick()

                        onSelected(
                            normalized
                        )
                    }
                }
        }

        /*
         * If the selected value changes externally,
         * move this wheel to that value.
         *
         * Example:
         * AM -> PM changes hour24.
         *
         * isScrollInProgress is read HERE,
         * inside LaunchedEffect,
         * not during composition.
         */
        LaunchedEffect(
            safeSelectedIndex,
            valuesSize
        ) {

            if (
                !state
                    .isScrollInProgress
            ) {

                val currentIndex =
                    centeredItemIndex

                val currentNormalized =
                    floorMod(
                        currentIndex,
                        valuesSize
                    )

                if (
                    currentNormalized !=
                    safeSelectedIndex
                ) {

                    val currentCycle =
                        currentIndex /
                                valuesSize

                    var targetIndex =
                        currentCycle *
                                valuesSize +
                                safeSelectedIndex

                    /*
                     * Stay inside our repeated
                     * wheel boundaries.
                     */
                    targetIndex =
                        targetIndex.coerceIn(
                            0,
                            valuesSize *
                                    cycles -
                                    1
                        )

                    state.scrollToItem(
                        index =
                            targetIndex,

                        scrollOffset =
                            0
                    )
                }
            }
        }

        /*
         * When user releases the wheel,
         * snap the nearest item exactly
         * onto the center line.
         */
        LaunchedEffect(
            state,
            valuesSize
        ) {

            snapshotFlow {
                state.isScrollInProgress
            }
                .distinctUntilChanged()
                .collect { scrolling ->

                    if (!scrolling) {

                        val layoutInfo =
                            state.layoutInfo

                        val center =
                            (
                                    layoutInfo
                                        .viewportStartOffset +
                                            layoutInfo
                                                .viewportEndOffset
                                    ) / 2

                        val centeredItem =
                            layoutInfo
                                .visibleItemsInfo
                                .minByOrNull { item ->

                                    abs(
                                        (
                                                item.offset +
                                                        item.size / 2
                                                ) -
                                                center
                                    )
                                }

                        centeredItem?.let {
                                item ->

                            val normalized =
                                floorMod(
                                    item.index,
                                    valuesSize
                                )

                            centeredItemIndex =
                                item.index

                            /*
                             * With our content padding,
                             * scrollOffset = 0 means this
                             * item sits exactly at center.
                             */
                            state.animateScrollToItem(
                                index =
                                    item.index,

                                scrollOffset =
                                    0
                            )

                            if (
                                normalized !=
                                safeSelectedIndex
                            ) {

                                onSelected(
                                    normalized
                                )
                            }
                        }
                    }
                }
        }

        LazyColumn(
            modifier =
                modifier.height(
                    itemHeight *
                            visibleRows
                ),

            state = state,

            flingBehavior =
                snapFlingBehavior,

            userScrollEnabled =
                true,

            /*
             * 5 rows total:
             *
             * row -2
             * row -1
             * CENTER
             * row +1
             * row +2
             *
             * This is why the selected item
             * is physically centered.
             */
            contentPadding =
                PaddingValues(
                    vertical =
                        itemHeight *
                                centerRow
                ),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            items(
                count =
                    valuesSize *
                            cycles
            ) { index ->

                val normalized =
                    floorMod(
                        index,
                        valuesSize
                    )

                /*
                 * Safe state read.
                 *
                 * centeredItemIndex only changes
                 * when another whole item becomes
                 * the centered value.
                 */
                val selected =
                    index ==
                            centeredItemIndex

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(
                                itemHeight
                            )
                            .graphicsLayer {

                                /*
                                 * This code executes
                                 * in the graphics layer,
                                 * not normal composition.
                                 *
                                 * We can use live scrolling
                                 * geometry here for the
                                 * smooth wheel effect.
                                 */
                                val layoutInfo =
                                    state.layoutInfo

                                val center =
                                    (
                                            layoutInfo
                                                .viewportStartOffset +
                                                    layoutInfo
                                                        .viewportEndOffset
                                            ) / 2

                                val currentItem =
                                    layoutInfo
                                        .visibleItemsInfo
                                        .firstOrNull {
                                                item ->
                                            item.index ==
                                                    index
                                        }

                                val distance =
                                    if (
                                        currentItem !=
                                        null
                                    ) {

                                        (
                                                (
                                                        currentItem
                                                            .offset +
                                                                currentItem
                                                                    .size /
                                                                2
                                                        ) -
                                                        center
                                                ) /
                                                currentItem
                                                    .size
                                                    .toFloat()

                                    } else {

                                        /*
                                         * Item outside
                                         * visible viewport.
                                         */
                                        4f
                                    }

                                val absDistance =
                                    abs(
                                        distance
                                    )

                                /*
                                 * Fade rows as they
                                 * move away from center.
                                 */
                                alpha =
                                    (
                                            1f -
                                                    absDistance *
                                                    0.30f
                                            )
                                        .coerceIn(
                                            0.18f,
                                            1f
                                        )

                                /*
                                 * Smaller rows away
                                 * from center.
                                 */
                                val scale =
                                    (
                                            1f -
                                                    absDistance *
                                                    0.08f
                                            )
                                        .coerceIn(
                                            0.78f,
                                            1f
                                        )

                                scaleX =
                                    scale

                                scaleY =
                                    scale

                                /*
                                 * 3D wheel rotation.
                                 */
                                rotationX =
                                    (
                                            distance *
                                                    -24f
                                            )
                                        .coerceIn(
                                            -62f,
                                            62f
                                        )

                                cameraDistance =
                                    14f *
                                            density.density
                            },

                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        text =
                            values[
                                normalized
                            ],

                        color =
                            if (selected) {

                                selectedTextColor

                            } else {

                                mutedTextColor
                            },

                        style =
                            if (large) {

                                /*
                                 * HOUR / MINUTE
                                 */
                                MaterialTheme
                                    .typography
                                    .displaySmall
                                    .copy(

                                        fontWeight =
                                            FontWeight.Black,

                                        fontSize =
                                            34.sp,

                                        letterSpacing =
                                            0.sp,

                                        lineHeight =
                                            36.sp,

                                        platformStyle =
                                            PlatformTextStyle(
                                                includeFontPadding =
                                                    false
                                            ),

                                        lineHeightStyle =
                                            LineHeightStyle(
                                                alignment =
                                                    LineHeightStyle
                                                        .Alignment
                                                        .Center,

                                                trim =
                                                    LineHeightStyle
                                                        .Trim
                                                        .Both
                                            ),

                                        textAlign =
                                            TextAlign.Center
                                    )

                            } else {

                                /*
                                 * AM / PM
                                 */
                                MaterialTheme
                                    .typography
                                    .titleMedium
                                    .copy(

                                        fontWeight =
                                            FontWeight.Black,

                                        fontSize =
                                            18.sp,

                                        letterSpacing =
                                            0.sp,

                                        lineHeight =
                                            20.sp,

                                        platformStyle =
                                            PlatformTextStyle(
                                                includeFontPadding =
                                                    false
                                            ),

                                        lineHeightStyle =
                                            LineHeightStyle(
                                                alignment =
                                                    LineHeightStyle
                                                        .Alignment
                                                        .Center,

                                                trim =
                                                    LineHeightStyle
                                                        .Trim
                                                        .Both
                                            ),

                                        textAlign =
                                            TextAlign.Center
                                    )
                            },

                        maxLines =
                            1
                    )
                }
            }
        }
    }
}

@Composable
private fun ReminderWheelSeparator(textColor: Color) {
    Column(
        modifier = Modifier
            .width(18.dp)
            .height(46.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(textColor)
        )
        Spacer(Modifier.height(7.dp))
        Box(
            Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(textColor)
        )
    }
}

@Composable
private fun ReminderHeaderResetPill(
    color: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val animatedColor by animateColorAsState(
        targetValue = if (pressed) pressLift(color) else color,
        animationSpec = tween(durationMillis = 120),
        label = "resetPressColor"
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "resetPressScale"
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
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = "Reset",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ReminderHeaderAnalogClockIcon(
    nowMillis: Long,
    color: Color,
    accentColor: Color,
    handColor: Color
) {
    Surface(
        modifier = Modifier.size(42.dp),
        shape = CircleShape,
        color = color,
        contentColor = handColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(7.dp)
        ) {
            val clock = Calendar.getInstance().apply { timeInMillis = nowMillis }
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = min(size.width, size.height) / 2f
            val stroke = 1.25.dp.toPx()
            val hour = clock.get(Calendar.HOUR) + clock.get(Calendar.MINUTE) / 60f
            val minute = clock.get(Calendar.MINUTE) + clock.get(Calendar.SECOND) / 60f
            val second = clock.get(Calendar.SECOND)

            fun handEnd(angleDegrees: Float, length: Float): Offset {
                val radians = Math.toRadians((angleDegrees - 90f).toDouble())
                return Offset(
                    x = center.x + cos(radians).toFloat() * length,
                    y = center.y + sin(radians).toFloat() * length
                )
            }

            drawCircle(
                color = accentColor.copy(alpha = 0.14f),
                radius = radius,
                center = center
            )
            drawCircle(
                color = accentColor.copy(alpha = 0.70f),
                radius = radius - stroke / 2f,
                center = center,
                style = Stroke(width = stroke)
            )
            repeat(12) { index ->
                val angle = index * 30f
                val outer = handEnd(angle, radius - 2.dp.toPx())
                val inner = handEnd(angle, radius - if (index % 3 == 0) 5.dp.toPx() else 3.5.dp.toPx())
                drawLine(
                    color = if (index % 3 == 0) accentColor.copy(alpha = 0.82f) else handColor.copy(alpha = 0.38f),
                    start = inner,
                    end = outer,
                    strokeWidth = if (index % 3 == 0) 1.35.dp.toPx() else 0.9.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            drawLine(
                color = handColor.copy(alpha = 0.92f),
                start = center,
                end = handEnd(hour * 30f, radius * 0.44f),
                strokeWidth = 2.2.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = handColor.copy(alpha = 0.88f),
                start = center,
                end = handEnd(minute * 6f, radius * 0.68f),
                strokeWidth = 1.65.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = accentColor,
                start = center,
                end = handEnd(second * 6f, radius * 0.74f),
                strokeWidth = 0.9.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawCircle(
                color = accentColor,
                radius = 2.dp.toPx(),
                center = center
            )
        }
    }
}

@Composable
private fun ReminderHeaderTitlePill(
    title: String,
    subtitle: String,
    color: Color,
    titleColor: Color,
    subtitleColor: Color,
    modifier: Modifier = Modifier
) {
    val pillShape = RoundedCornerShape(999.dp)
    Surface(
        modifier = modifier
            .height(42.dp)
            .clip(pillShape),
        shape = pillShape,
        color = color,
        contentColor = titleColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                color = titleColor,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.sp,
                    textAlign = TextAlign.Center
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = subtitleColor,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp,
                    textAlign = TextAlign.Center
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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

private fun reminderDayWheelLabels(anchor: Calendar): List<String> {
    val locale = Locale.getDefault()
    val formatter = SimpleDateFormat("EEE MMM d", locale)
    return (0..ReminderDayWheelMaxOffsetDays).map { offset ->
        val calendar = (anchor.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, offset)
        }
        formatter.format(calendar.time)
    }
}

private fun floorMod(value: Int, modulus: Int): Int =
    ((value % modulus) + modulus) % modulus

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

private fun reminderDayLabel(triggerAtMillis: Long): String =
    SimpleDateFormat("EEE MMM d", Locale.getDefault()).format(Date(triggerAtMillis))

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

    vibrator.vibrate(
        VibrationEffect.createWaveform(
            longArrayOf(0, 18, 34, 42, 42, 74),
            intArrayOf(0, 80, 0, 130, 0, 205),
            -1
        )
    )
}
