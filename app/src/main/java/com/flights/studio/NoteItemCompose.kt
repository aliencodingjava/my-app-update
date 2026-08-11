package com.flights.studio

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private const val LIST_PREVIEW_CHARS = 50
private const val NOTE_SELECT_HOLD_MS = 850L

private fun notePreview(s: String): String {
    if (s.isBlank()) return ""
    val oneLine = s
        .replace(Regex("""\[[^]]*]"""), " ")
        .replace(Regex("""[□☐☑☒✓✔●◉○•]"""), " ")
        .replace(Regex("""\b\d+%?\s*[-–]\s*"""), " ")
        .replace('\n', ' ')
        .replace('\t', ' ')
        .replace(Regex("\\s+"), " ")
        .replace(Regex("""\s+:"""), ":")
        .trim()
    return if (oneLine.length <= LIST_PREVIEW_CHARS) oneLine else oneLine.take(LIST_PREVIEW_CHARS) + "…"
}

private fun noteCreatedAtLabel(createdAtMs: Long): String {
    if (createdAtMs <= 0L) return ""
    val now = Calendar.getInstance()
    val created = Calendar.getInstance().apply { timeInMillis = createdAtMs }
    val pattern = when {
        now.get(Calendar.YEAR) == created.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == created.get(Calendar.DAY_OF_YEAR) -> "h:mm a"
        now.get(Calendar.YEAR) == created.get(Calendar.YEAR) -> "MMM d"
        else -> "MMM d, yyyy"
    }
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(createdAtMs))
}

@Composable
private fun NoteMediaCountChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    accent: Color,
    isDark: Boolean,
    compact: Boolean,
    tight: Boolean,
    roundedEnd: Boolean = true
) {
    val chipHeight = when {
        tight -> 17.dp
        compact -> 19.dp
        else -> 23.dp
    }
    val iconSize = when {
        tight -> 9.dp
        compact -> 10.dp
        else -> 12.dp
    }
    val mediaShape = remember(tight, roundedEnd) {
        GenericShape { size, _ ->
            val w = size.width
            val h = size.height
            val slant = h * 0.30f
            val rightRadius = h * 0.44f

            moveTo(0f, 0f)
            if (roundedEnd) {
                lineTo(w - rightRadius, 0f)
                quadraticTo(w, 0f, w, rightRadius)
                lineTo(w, h)
            } else {
                lineTo(w - slant, 0f)
                lineTo(w, h)
            }
            lineTo(slant, h)
            close()
        }
    }
    val countText = if (count > 99) "99+" else count.toString()

    Surface(
        modifier = Modifier.height(chipHeight),
        shape = mediaShape,
        color = accent.copy(alpha = if (isDark) 0.16f else 0.12f),
        border = BorderStroke(
            if (tight) 0.5.dp else 0.65.dp,
            accent.copy(alpha = if (isDark) 0.30f else 0.24f)
        )
    ) {
        Row(
            modifier = Modifier.padding(
                start = if (tight) 7.dp else 8.dp,
                end = if (tight) 5.dp else 6.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (tight) 2.dp else 3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                tint = accent.copy(alpha = if (isDark) 0.94f else 0.86f)
            )
            Text(
                text = countText,
                maxLines = 1,
                fontSize = when {
                    tight -> 7.sp
                    compact -> 7.5.sp
                    else -> 9.sp
                },
                lineHeight = when {
                    tight -> 8.sp
                    compact -> 9.sp
                    else -> 11.sp
                },
                fontWeight = FontWeight.SemiBold,
                color = accent.copy(alpha = if (isDark) 0.95f else 0.88f)
            )
        }
    }
}


@Composable
fun NoteItem(
    title: String?,
    note: String,
    compact: Boolean,
    dense: Boolean,
    selectionMode: Boolean,
    selected: Boolean,
    showReminderBell: Boolean, showReminderBadge: Boolean,
    imagesCount: Int,
    attachmentsCount: Int = 0,
    audioCount: Int = 0,
    videoCount: Int = 0,
    createdAtMs: Long = 0L,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onEdit: () -> Unit,
    onReminderClick: () -> Unit,
    titleTopCompactDp: Int,
    titleTopNormalDp: Int,
    palette: NotesPaletteColors? = null,
    smallMediaBadges: Boolean = false,
) {
    val railW = when {
        dense || smallMediaBadges -> 42.dp
        compact -> 45.dp
        else -> 56.dp
    }
    val h = if (compact) 80.dp else 140.dp
    val ui = rememberUiScales()
    val iconSize = if (dense) 18.dp else 24.dp
    val btnSize = if (dense) 36.dp else 44.dp
    val titlePadV = if (compact) 4.dp else 7.dp
    val titlePadH = if (compact) 10.dp else 12.dp
    val titleShape = RoundedCornerShape(if (compact) 10.dp else 12.dp)
    val cardShape = RoundedCornerShape(if (compact) 14.dp else 18.dp)
    val noteMaxLines = 2
    val titleMaxLines = 1
    val colBottom = when {
        dense || smallMediaBadges -> 24.dp
        compact -> 24.dp
        else -> 34.dp
    }
    val afterTitleSpace = if (compact) 4.dp else 10.dp
    val isDark = isSystemInDarkTheme()
    val isLightTheme = !isSystemInDarkTheme()
    val hasPalette = palette != null
    val appPalette = LocalAppThemePalette.current


    val paletteBackgroundIsLight = (palette?.noteTint
        ?: if (isDark) Color(0xFF151617) else Color(0xFFE2E9F1))
        .luminance() > 0.5f
    val noteActionColor = palette?.let {
        val readableBase = if (paletteBackgroundIsLight) Color.Black else Color.White
        lerp(readableBase, it.accent.copy(alpha = 1f), if (paletteBackgroundIsLight) 0.46f else 0.56f)
    } ?: appPalette.actionContent
    // Keep the row surface palette-driven; only the page behind it is fixed graphite.
    val containerTintColor = (palette?.noteTint
        ?: if (isDark) Color(0xFF151617) else Color(0xFFE2E9F1)).copy(alpha = 1f)

    val noteAccentSurfaceColor = (palette?.titleRail
        ?: if (isDark) Color(0xFF252B31) else Color(0xFFE6EEF7)).copy(alpha = 1f)
    val settingsBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = if (isDark) 0.34f else 0.22f)



// Selected overlay should be weaker in light (or it turns “blue paint”)
    val selectedOverlay = if (isDark) {
        appPalette.action.copy(alpha = 0.22f)
    } else {
        appPalette.action.copy(alpha = 0.10f)
    }
    var isPressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1f,
        animationSpec = spring(dampingRatio = 0.84f, stiffness = 700f),
        label = "notePressScale"
    )
    val pressedFill by animateColorAsState(
        targetValue = if (isPressed) {
            noteActionColor.copy(alpha = if (isDark) 0.14f else 0.10f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(durationMillis = if (isPressed) 90 else 170),
        label = "notePressFill"
    )


    val adaptiveColor = if (hasPalette) {
        if (paletteBackgroundIsLight) Color.Black else Color.White
    } else if (isDark) Color.White else Color.Black

    val createdLabel = remember(createdAtMs) { noteCreatedAtLabel(createdAtMs) }
    // Keep each row to a single cheap static surface. Large note lists should not
    // allocate backdrop, blur, lens, or interactive transform layers per item.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(h)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .shadow(
                elevation = if (compact) 4.dp else 6.dp,
                shape = cardShape,
                clip = false
            )
            .clip(cardShape)
            .background(containerTintColor)
            .then(
                if (selected) Modifier.background(selectedOverlay)
                else Modifier
            )
            .background(pressedFill)
            .pointerInput(onClick, onLongClick) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    try {
                        val pointerId = down.id
                        val downPosition = down.position
                        val touchSlop = viewConfiguration.touchSlop

                        val releasedBeforeLongPress = withTimeoutOrNull(NOTE_SELECT_HOLD_MS) {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == pointerId }
                                    ?: return@withTimeoutOrNull false

                                if (change.isConsumed) return@withTimeoutOrNull false
                                if (!change.pressed) return@withTimeoutOrNull true

                                val drag = change.position - downPosition
                                if (abs(drag.x) > touchSlop || abs(drag.y) > touchSlop) {
                                    return@withTimeoutOrNull false
                                }
                            }
                        }

                        if (releasedBeforeLongPress == true) {
                            onClick()
                        } else if (releasedBeforeLongPress == null) {
                            onLongClick()
                            waitForUpOrCancellation()?.consume()
                        }
                    } finally {
                        isPressed = false
                    }
                }
            }
    ) {
            Box(Modifier.fillMaxSize()) {

                // RIGHT ACTION RAIL
                Surface(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(railW)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                    color = noteAccentSurfaceColor,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = if (compact) 4.dp else 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // EDIT
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(btnSize)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Edit",
                                modifier = Modifier.size(iconSize),
                                tint = noteActionColor
                            )
                        }

                        // CALENDAR + DOT (dot anchored to ICON, not button)
                        if (showReminderBell) {
                            IconButton(
                                onClick = onReminderClick,
                                modifier = Modifier.size(btnSize)
                            ) {
                                // ✅ anchor box = iconSize, so badge tracks the glyph
                                Box(
                                    modifier = Modifier.size(iconSize),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CalendarMonth,
                                        contentDescription = "Reminder",
                                        modifier = Modifier.matchParentSize(),
                                        tint = noteActionColor
                                    )

                                    if (showReminderBadge) {
                                        val dotSize = when {
                                            dense -> 8.dp
                                            compact -> 9.dp
                                            else -> 10.dp
                                        }

                                        // ✅ tuned so it doesn’t feel “off” on the glyph
                                        val dx = when {
                                            dense -> 1.dp
                                            compact -> 1.dp
                                            else -> 2.dp
                                        }
                                        val dy = when {
                                            dense -> 0.dp
                                            compact -> 0.dp
                                            else -> (-1).dp
                                        }

                                        // outline thickness
                                        val ring = when {
                                            dense -> 1.dp
                                            compact -> 1.dp
                                            else -> 1.25.dp
                                        }

                                        // ring color: use surfaceVariant so it blends with the rail, but still “white-ish”
                                        val ringColor =
                                            Color.White.copy(alpha = if (isSystemInDarkTheme()) 0.90f else 0.75f)

                                        AnimatedBadge(
                                            visible = true,
                                            baseColor = MaterialTheme.colorScheme.error,
                                            glowColor = MaterialTheme.colorScheme.primary, // or errorContainer / tertiary
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .offset(x = dx, y = dy)
                                        ) { dotColor ->
                                            Box(
                                                modifier = Modifier
                                                    .size(dotSize)
                                                    .background(ringColor, CircleShape)   // ring
                                                    .padding(ring)
                                                    .background(dotColor, CircleShape)    // pulsing + fading dot
                                            )
                                        }


                                    }


                                }
                            }
                        } else {
                            // if you still want spacing even when bell hidden
                            Spacer(Modifier.size(btnSize))
                        }
                    }
                }


                // LEFT SELECT (Radio) — smooth appear/disappear
                androidx.compose.animation.AnimatedVisibility(
                    visible = selectionMode,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 10.dp, top = 10.dp),
                    enter = fadeIn(animationSpec = tween(220)) +
                            scaleIn(
                                initialScale = 0.88f,
                                animationSpec = tween(260, easing = FastOutSlowInEasing)
                            ),
                    exit  = fadeOut(animationSpec = tween(180)) +
                            scaleOut(
                                targetScale = 0.88f,
                                animationSpec = tween(180, easing = FastOutSlowInEasing)
                            )
                ) {
                    RadioButton(
                        selected = selected,
                        onClick = null
                    )
                }

                // Media chips stay intentionally small so compact and two-column notes remain clean.
                val tightMediaBadges = dense || smallMediaBadges
                val hasMediaBadges = imagesCount > 0 || attachmentsCount > 0 || audioCount > 0 || videoCount > 0
                val badgeStripBottomPad = when {
                    tightMediaBadges -> 2.dp
                    compact -> 2.dp
                    else -> 3.dp
                }
                val badgeStripStartPad = when {
                    createdLabel.isNotBlank() && tightMediaBadges -> 6.dp
                    createdLabel.isNotBlank() -> 6.dp
                    tightMediaBadges -> 6.dp
                    compact -> 8.dp
                    else -> 12.dp
                }
                val badgeStripGap = 4.dp
                if (createdLabel.isNotBlank() || hasMediaBadges) {
                    val lastMediaBadge = when {
                        videoCount > 0 -> "video"
                        audioCount > 0 -> "audio"
                        attachmentsCount > 0 -> "attachments"
                        imagesCount > 0 -> "images"
                        else -> ""
                    }
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = badgeStripStartPad, bottom = badgeStripBottomPad),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(badgeStripGap)
                    ) {
                        if (createdLabel.isNotBlank()) {
                            val timestampShape = remember(tightMediaBadges, compact) {
                                GenericShape { size, _ ->
                                    val w = size.width
                                    val h = size.height
                                    val slant = h * 0.30f
                                    val bottomLeft = when {
                                        tightMediaBadges -> h * (10f / 17f)
                                        compact -> h * (18f / 19f)
                                        else -> h * (14f / 23f)
                                    }.coerceAtMost(h)

                                    moveTo(0f, 0f)
                                    lineTo(w - slant, 0f)
                                    lineTo(w, h)
                                    lineTo(bottomLeft, h)
                                    quadraticTo(0f, h, 0f, h - bottomLeft)
                                    lineTo(0f, 0f)
                                    close()
                                }
                            }
                            Surface(
                                modifier = Modifier.height(
                                    when {
                                        tightMediaBadges -> 17.dp
                                        compact -> 19.dp
                                        else -> 23.dp
                                    }
                                ),
                                shape = timestampShape,
                                color = if (hasPalette) {
                                    noteAccentSurfaceColor
                                } else {
                                    noteAccentSurfaceColor.copy(alpha = if (isDark) 0.82f else 0.90f)
                                },
                                border = BorderStroke(
                                    if (tightMediaBadges) 0.5.dp else 0.65.dp,
                                    settingsBorderColor.copy(alpha = if (isDark) 0.34f else 0.24f)
                                )
                            ) {
                                Box(
                                    modifier = Modifier.padding(
                                        start = when {
                                            tightMediaBadges -> 4.dp
                                            compact -> 5.dp
                                            else -> 5.dp
                                        },
                                        end = when {
                                            tightMediaBadges -> 9.dp
                                            compact -> 10.dp
                                            else -> 11.dp
                                        }
                                    ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = createdLabel,
                                        maxLines = 1,
                                        fontSize = when {
                                            tightMediaBadges -> 7.sp
                                            compact -> 7.5.sp
                                            else -> 9.sp
                                        },
                                        lineHeight = when {
                                            tightMediaBadges -> 8.sp
                                            compact -> 9.sp
                                            else -> 11.sp
                                        },
                                        fontWeight = FontWeight.SemiBold,
                                        color = noteActionColor.copy(alpha = if (isDark) 0.94f else 0.86f)
                                    )
                                }
                            }
                        }
                        if (imagesCount > 0) {
                            NoteMediaCountChip(
                                icon = Icons.Filled.Image,
                                count = imagesCount,
                                accent = noteActionColor,
                                isDark = isDark,
                                compact = compact,
                                tight = tightMediaBadges,
                                roundedEnd = lastMediaBadge == "images"
                            )
                        }
                        if (attachmentsCount > 0) {
                            NoteMediaCountChip(
                                icon = Icons.Filled.Description,
                                count = attachmentsCount,
                                accent = MaterialTheme.colorScheme.secondary,
                                isDark = isDark,
                                compact = compact,
                                tight = tightMediaBadges,
                                roundedEnd = lastMediaBadge == "attachments"
                            )
                        }
                        if (audioCount > 0) {
                            NoteMediaCountChip(
                                icon = Icons.Filled.Audiotrack,
                                count = audioCount,
                                accent = MaterialTheme.colorScheme.tertiary,
                                isDark = isDark,
                                compact = compact,
                                tight = tightMediaBadges,
                                roundedEnd = lastMediaBadge == "audio"
                            )
                        }
                        if (videoCount > 0) {
                            NoteMediaCountChip(
                                icon = Icons.Filled.Videocam,
                                count = videoCount,
                                accent = MaterialTheme.colorScheme.primary,
                                isDark = isDark,
                                compact = compact,
                                tight = tightMediaBadges,
                                roundedEnd = lastMediaBadge == "video"
                            )
                        }
                    }
                }
                val targetStart = if (selectionMode) 46.dp else (if (compact) 12.dp else 14.dp)
                val startPad by animateDpAsState(
                    targetValue = targetStart,
                    animationSpec = spring(
                        dampingRatio = 0.9f,
                        stiffness = 220f
                    ),
                    label = "noteStartPad"
                )


                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = startPad,
                            end = railW + (if (compact) 10.dp else 14.dp),
                            top = (if (compact) titleTopCompactDp else titleTopNormalDp).dp,
                            bottom = colBottom
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {

                    if (!title.isNullOrBlank()) {
                        Surface(
                            shape = titleShape,
                            color = noteAccentSurfaceColor,

                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = title,
                                modifier = Modifier.padding(
                                    horizontal = titlePadH,
                                    vertical = titlePadV
                                ),
                                maxLines = titleMaxLines,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontSize = MaterialTheme.typography.labelLarge.fontSize.us(
                                        if (compact) ui.label * 0.92f else ui.label
                                    )
                                ),
                                color = noteActionColor
                            )
                        }
                        Spacer(Modifier.height(afterTitleSpace))
                    }
                    val preview = remember(note) { notePreview(note) }   //  cached per note
                    val primary = MaterialTheme.colorScheme.primary

                    val adaptiveStrength = if (isDark) 0.35f else 0.45f

                    val adaptivePrimary = if (hasPalette) {
                        lerp(adaptiveColor, noteActionColor, 0.28f)
                    } else {
                        lerp(primary, adaptiveColor, adaptiveStrength)
                    }
                    val previewFontSize = if (compact) 12.5.sp else 15.sp
                    val previewLineHeight = if (compact) 16.sp else 21.sp
                    val previewRuleColor = noteActionColor.copy(alpha = if (isDark) 0.18f else 0.12f)
                    val previewLikelyTwoLines = preview.length > if (compact) 18 else 24
                    val previewBoxHeight = if (compact) 34.dp else 62.dp


                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = if (compact) 20.dp else 30.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(if (compact) 0.82f else 0.72f)
                                    .height(previewBoxHeight)
                                    .drawBehind {
                                        if (preview.isNotBlank()) {
                                            val stroke = 0.55.dp.toPx()
                                            val gap = previewLineHeight.toPx()
                                            val center = size.height / 2f
                                            val startX = size.width * 0.08f
                                            val endX = size.width * 0.92f
                                            val lines = if (previewLikelyTwoLines) {
                                                val spread = if (compact) 0.58f else 0.90f
                                                listOf(center - gap * spread, center, center + gap * spread)
                                            } else {
                                                val spread = if (compact) 0.46f else 0.70f
                                                listOf(center - gap * spread, center + gap * spread)
                                            }
                                            lines.forEach { y ->
                                                drawLine(
                                                    color = previewRuleColor,
                                                    start = Offset(startX, y),
                                                    end = Offset(endX, y),
                                                    strokeWidth = stroke
                                                )
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = preview,
                                    maxLines = noteMaxLines,
                                    textAlign = TextAlign.Center,
                                    color = adaptivePrimary.copy(
                                        alpha = when {
                                            hasPalette && paletteBackgroundIsLight -> 0.78f
                                            hasPalette -> 0.82f
                                            isDark -> 0.62f
                                            else -> 0.54f
                                        }
                                    ),
                                    fontSize = previewFontSize,
                                    lineHeight = previewLineHeight
                                )
                            }
                        }
                    }
                }
            }
        }
    }
