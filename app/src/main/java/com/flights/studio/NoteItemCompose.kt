package com.flights.studio

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.capsule.ContinuousRoundedRectangle
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

private const val LIST_PREVIEW_CHARS = 50
private const val NOTE_SELECT_HOLD_MS = 850L

private fun notePreview(s: String): String {
    if (s.isBlank()) return ""
    val oneLine = s
        .replace(Regex("""\[[^\]]*]"""), " ")
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
    tight: Boolean
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
    val countText = if (count > 99) "99+" else count.toString()

    Surface(
        modifier = Modifier.height(chipHeight),
        shape = RoundedCornerShape(
            topStart = 0.dp,
            topEnd = if (tight) 8.dp else 10.dp,
            bottomEnd = 0.dp,
            bottomStart = if (tight) 8.dp else 10.dp
        ),
        color = accent.copy(alpha = if (isDark) 0.16f else 0.12f),
        border = BorderStroke(
            if (tight) 0.5.dp else 0.65.dp,
            accent.copy(alpha = if (isDark) 0.30f else 0.24f)
        )
    ) {
        Row(
            modifier = Modifier.padding(
                start = if (tight) 4.dp else 5.dp,
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
    backdrop: Backdrop,
    titleTopCompactDp: Int,
    titleTopNormalDp: Int,
    isInteractive: Boolean = true,
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
    val cardShape = RoundedCornerShape(18.dp)
    val noteMaxLines = 2
    val titleMaxLines = 1
    val colBottom = when {
        dense || smallMediaBadges -> 24.dp
        compact -> 24.dp
        else -> 34.dp
    }
    val afterTitleSpace = if (compact) 4.dp else 10.dp
    val isDark = isSystemInDarkTheme()
    val scheme = MaterialTheme.colorScheme
    val isLightTheme = !isSystemInDarkTheme()


// Container: in light, use warmBase; in dark, keep your dark glass
    val containerColor = palette?.noteTint ?: if (isLightTheme) {
        Color(0xFFFDF9FF).copy(alpha = 0.54f)
    } else {
        Color(0xFF201923).copy(alpha = 0.58f)
    }

    val noteAccentSurfaceColor = palette?.titleRail ?: if (isLightTheme) {
        Color(0xFFEFE7F6).copy(alpha = 0.76f)
    } else {
        Color(0xFF2A2131).copy(alpha = 0.72f)
    }
    val noteActionColor = palette?.accent ?: if (isLightTheme) Color(0xFF6D4B86) else Color(0xFFCBB6E5)
    val settingsBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = if (isDark) 0.34f else 0.22f)



// Selected overlay should be weaker in light (or it turns “blue paint”)
    val selectedOverlay = if (isDark) {
        scheme.primary.copy(alpha = 0.22f)
    } else {
        scheme.primary.copy(alpha = 0.10f)
    }


    val adaptiveColor = if (isDark) Color.White else Color.Black

    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }
    val createdLabel = remember(createdAtMs) { noteCreatedAtLabel(createdAtMs) }
    //  OUTER ELEVATION WRAPPER (NO clickable here anymore)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(h)
            .graphicsLayer {
                if (isInteractive && size.width > 0f && size.height > 0f) {
                    val progress = interactiveHighlight.pressProgress
                    val zoomAmountPx = 3.5.dp.toPx()
                    val scale = lerp(1f, 1f + zoomAmountPx / size.height, progress)
                    val maxOffset = size.minDimension
                    val k = 0.025f
                    val offset = interactiveHighlight.offset
                    translationX = maxOffset * tanh(k * offset.x / maxOffset)
                    translationY = maxOffset * tanh(k * offset.y / maxOffset)

                    val maxDragScale = 1.5.dp.toPx() / size.height
                    val ang = atan2(offset.y, offset.x)
                    scaleX = scale +
                        maxDragScale *
                        abs(cos(ang) * offset.x / size.maxDimension) *
                        (size.width / size.height).fastCoerceAtMost(1f)
                    scaleY = scale +
                        maxDragScale *
                        abs(sin(ang) * offset.y / size.maxDimension) *
                        (size.height / size.width).fastCoerceAtMost(1f)
                }
            }
            .clip(cardShape)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { ContinuousRoundedRectangle(18f.dp) },
                highlight = null,
                effects = {
                    blur(3f, edgeTreatment = TileMode.Clamp)

                    val cornerRadiusPx = size.height / 2f
                    val safeHeight = cornerRadiusPx * 0.15f

                    lens(
                        refractionHeight = safeHeight.coerceIn(0f, cornerRadiusPx),
                        refractionAmount = (size.minDimension * 0.65f)
                            .coerceIn(0f, size.minDimension),
                        depthEffect = false,
                        chromaticAberration = false
                    )
                },
                onDrawSurface = {
                    drawRect(containerColor)
                    if (selected) drawRect(selectedOverlay)
                }
            )
            .border(
                BorderStroke(0.dp, settingsBorderColor),
                cardShape
            )
            .then(if (isInteractive) interactiveHighlight.modifier else Modifier)
            .then(if (isInteractive) interactiveHighlight.gestureModifier else Modifier)
            .pointerInput(onClick, onLongClick) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
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

                if (createdLabel.isNotBlank()) {
                    val tightTimestamp = dense || smallMediaBadges
                    val timestampBottomPad = when {
                        tightTimestamp -> 2.dp
                        compact -> 2.dp
                        else -> 3.dp
                    }
                    val timestampCorner = when {
                        tightTimestamp -> 8.dp
                        compact -> 10.dp
                        else -> 10.dp
                    }
                    val timestampLeftBottomCorner = when {
                        tightTimestamp -> 10.dp
                        compact -> 18.dp
                        else -> 14.dp
                    }
                    val timestampShape = RoundedCornerShape(
                        topStart = 0.dp,
                        topEnd = timestampCorner,
                        bottomEnd = 0.dp,
                        bottomStart = timestampLeftBottomCorner
                    )
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = if (tightTimestamp) 4.dp else 6.dp, bottom = timestampBottomPad)
                            .height(
                                when {
                                    tightTimestamp -> 17.dp
                                    compact -> 19.dp
                                    else -> 23.dp
                                }
                            ),
                        shape = timestampShape,
                        color = noteAccentSurfaceColor.copy(alpha = if (isDark) 0.82f else 0.90f),
                        border = BorderStroke(
                            if (tightTimestamp) 0.5.dp else 0.65.dp,
                            settingsBorderColor.copy(alpha = if (isDark) 0.34f else 0.24f)
                        )
                    ) {
                        Box(
                            modifier = Modifier.padding(
                                start = when {
                                    tightTimestamp -> 4.dp
                                    compact -> 5.dp
                                    else -> 5.dp
                                },
                                end = when {
                                    tightTimestamp -> 5.dp
                                    compact -> 6.dp
                                    else -> 6.dp
                                }
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = createdLabel,
                                maxLines = 1,
                                fontSize = when {
                                    tightTimestamp -> 7.sp
                                    compact -> 7.5.sp
                                    else -> 9.sp
                                },
                                lineHeight = when {
                                    tightTimestamp -> 8.sp
                                    compact -> 9.sp
                                    else -> 11.sp
                                },
                                fontWeight = FontWeight.SemiBold,
                                color = noteActionColor.copy(alpha = if (isDark) 0.94f else 0.86f)
                            )
                        }
                    }
                }

                // Media chips stay intentionally small so compact and two-column notes remain clean.
                val tightMediaBadges = dense || smallMediaBadges
                val mediaChipBottomPad = when {
                    tightMediaBadges -> 2.dp
                    compact -> 2.dp
                    else -> 3.dp
                }
                val mediaChipStartPad = when {
                    createdLabel.isNotBlank() && tightMediaBadges -> 50.dp
                    createdLabel.isNotBlank() && compact -> 54.dp
                    createdLabel.isNotBlank() -> 84.dp
                    tightMediaBadges -> 6.dp
                    compact -> 8.dp
                    else -> 12.dp
                }
                val mediaChipGap = if (tightMediaBadges) 2.dp else 4.dp
                if (imagesCount > 0 || attachmentsCount > 0 || audioCount > 0 || videoCount > 0) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = mediaChipStartPad, bottom = mediaChipBottomPad),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(mediaChipGap)
                    ) {
                        if (imagesCount > 0) {
                            NoteMediaCountChip(
                                icon = Icons.Filled.Image,
                                count = imagesCount,
                                accent = noteActionColor,
                                isDark = isDark,
                                compact = compact,
                                tight = tightMediaBadges
                            )
                        }
                        if (attachmentsCount > 0) {
                            NoteMediaCountChip(
                                icon = Icons.Filled.Description,
                                count = attachmentsCount,
                                accent = MaterialTheme.colorScheme.secondary,
                                isDark = isDark,
                                compact = compact,
                                tight = tightMediaBadges
                            )
                        }
                        if (audioCount > 0) {
                            NoteMediaCountChip(
                                icon = Icons.Filled.Audiotrack,
                                count = audioCount,
                                accent = MaterialTheme.colorScheme.tertiary,
                                isDark = isDark,
                                compact = compact,
                                tight = tightMediaBadges
                            )
                        }
                        if (videoCount > 0) {
                            NoteMediaCountChip(
                                icon = Icons.Filled.Videocam,
                                count = videoCount,
                                accent = MaterialTheme.colorScheme.primary,
                                isDark = isDark,
                                compact = compact,
                                tight = tightMediaBadges
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

                    val adaptivePrimary = lerp(primary, adaptiveColor, adaptiveStrength)
                    val previewFontSize = if (compact) 12.5.sp else 15.sp
                    val previewLineHeight = if (compact) 16.sp else 21.sp
                    val previewRuleColor = noteActionColor.copy(alpha = if (isDark) 0.18f else 0.12f)
                    val previewLikelyTwoLines = preview.length > if (compact) 18 else 24


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
                                    .height(if (compact) 44.dp else 62.dp)
                                    .drawBehind {
                                        if (preview.isNotBlank()) {
                                            val stroke = 0.55.dp.toPx()
                                            val gap = previewLineHeight.toPx()
                                            val center = size.height / 2f
                                            val startX = size.width * 0.08f
                                            val endX = size.width * 0.92f
                                            val lines = if (previewLikelyTwoLines) {
                                                listOf(center - gap * 0.90f, center, center + gap * 0.90f)
                                            } else {
                                                listOf(center - gap * 0.70f, center + gap * 0.70f)
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
                                    color = adaptivePrimary.copy(alpha = if (isDark) 0.62f else 0.54f),
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
