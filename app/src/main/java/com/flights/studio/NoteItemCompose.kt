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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.kyant.capsule.ContinuousRoundedRectangle
import kotlinx.coroutines.withTimeoutOrNull
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
        .replace('\n', ' ')
        .replace('\t', ' ')
        .replace(Regex("\\s+"), " ")
        .trim()
    return if (oneLine.length <= LIST_PREVIEW_CHARS) oneLine else oneLine.take(LIST_PREVIEW_CHARS) + "…"
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
    val railW = if (compact) 45.dp else 60.dp
    val h = if (compact) 80.dp else 140.dp
    val ui = rememberUiScales()
    val iconSize = if (dense) 18.dp else 24.dp
    val btnSize = if (dense) 36.dp else 44.dp
    val titlePadV = if (compact) 4.dp else 7.dp
    val titlePadH = if (compact) 10.dp else 12.dp
    val titleShape = RoundedCornerShape(if (compact) 10.dp else 12.dp)
    val cardShape = RoundedCornerShape(18.dp)
    val noteMaxLines = if (compact) 2 else 4
    val titleMaxLines = 1
    val colBottom = if (compact) 6.dp else 10.dp
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
        Color(0xFFEFE7F6).copy(alpha = 0.96f)
    } else {
        Color(0xFF2A2131).copy(alpha = 0.92f)
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
                highlight = {
                    if (isDark) {
                        Highlight(
                            width = 0.15.dp,
                            blurRadius = 1.6.dp,
                            alpha = 0.50f,
                            style = HighlightStyle.Plain
                        )
                    } else {
                        Highlight(
                            width = 0.30.dp,
                            blurRadius = 1.6.dp,
                            alpha = 0.50f,
                            style = HighlightStyle.Plain
                        )
                    }
                },
                effects = {
                    blur(8f, edgeTreatment = TileMode.Clamp)

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
                BorderStroke(1.dp, settingsBorderColor),
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
                                            visible = showReminderBadge,
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



                // Media badges stay intentionally small so compact and two-column notes remain clean.
                val tightMediaBadges = dense || smallMediaBadges
                val mediaBadgeIconSize = when {
                    tightMediaBadges -> 12.dp
                    compact -> 13.dp
                    else -> 14.dp
                }
                val mediaBadgeSize = when {
                    tightMediaBadges -> 8.dp
                    compact -> 9.dp
                    else -> 10.dp
                }
                val mediaBadgeOffsetX = when {
                    tightMediaBadges -> 1.dp
                    compact -> 1.dp
                    else -> 2.dp
                }
                val mediaBadgeOffsetY = when {
                    tightMediaBadges -> (-1).dp
                    compact -> (-1).dp
                    else -> (-2).dp
                }
                fun mediaBadgeLeftPad(slot: Int) = when {
                    tightMediaBadges -> (8 + slot * 18).dp
                    compact -> (10 + slot * 20).dp
                    else -> (14 + slot * 23).dp
                }
                val mediaBadgeBottomPad = when {
                    tightMediaBadges -> 8.dp
                    compact -> 9.dp
                    else -> 11.dp
                }
                val mediaBadgeRingPadding = when {
                    tightMediaBadges -> 0.75.dp
                    compact -> 0.9.dp
                    else -> 1.dp
                }
                val mediaBadgeTextSize = when {
                    tightMediaBadges -> 6.sp
                    compact -> 6.sp
                    else -> 7.sp
                }

                if (imagesCount > 0) {

                    val badgeText = if (imagesCount > 99) "99+" else imagesCount.toString()
                    val ringColor = if (isSystemInDarkTheme())
                        Color.White.copy(alpha = 0.9f)
                    else
                        Color.White.copy(alpha = 0.95f)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = mediaBadgeLeftPad(0), bottom = mediaBadgeBottomPad)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Image,
                            contentDescription = null,
                            modifier = Modifier.size(mediaBadgeIconSize),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = mediaBadgeOffsetX, y = mediaBadgeOffsetY)
                                .size(mediaBadgeSize)
                                .background(
                                    color = ringColor,   // 🤍 white ring in BOTH dark + light
                                    shape = CircleShape
                                )
                                .padding(
                                    mediaBadgeRingPadding
                                )
                                .background(
                                    MaterialTheme.colorScheme.error,
                                    CircleShape
                                ), // inner red dot
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = badgeText,
                                maxLines = 1,
                                textAlign = TextAlign.Center,
                                fontSize = mediaBadgeTextSize,
                                lineHeight = mediaBadgeTextSize,
                                color = MaterialTheme.colorScheme.onError
                            )
                        }
                    }
                }
                val docSlot = if (imagesCount > 0) 1 else 0
                val audioSlot = docSlot + if (attachmentsCount > 0) 1 else 0
                val videoSlot = audioSlot + if (audioCount > 0) 1 else 0
                if (attachmentsCount > 0) {
                    val badgeText = if (attachmentsCount > 99) "99+" else attachmentsCount.toString()
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = mediaBadgeLeftPad(docSlot), bottom = mediaBadgeBottomPad)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Description,
                            contentDescription = null,
                            modifier = Modifier.size(mediaBadgeIconSize),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = mediaBadgeOffsetX, y = mediaBadgeOffsetY)
                                .size(mediaBadgeSize)
                                .background(Color.White.copy(alpha = 0.92f), CircleShape)
                                .padding(mediaBadgeRingPadding)
                                .background(MaterialTheme.colorScheme.secondary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = badgeText,
                                maxLines = 1,
                                textAlign = TextAlign.Center,
                                fontSize = mediaBadgeTextSize,
                                lineHeight = mediaBadgeTextSize,
                                color = MaterialTheme.colorScheme.onSecondary
                            )
                        }
                    }
                }
                if (audioCount > 0) {
                    val badgeText = if (audioCount > 99) "99+" else audioCount.toString()
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = mediaBadgeLeftPad(audioSlot), bottom = mediaBadgeBottomPad)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Audiotrack,
                            contentDescription = null,
                            modifier = Modifier.size(mediaBadgeIconSize),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = mediaBadgeOffsetX, y = mediaBadgeOffsetY)
                                .size(mediaBadgeSize)
                                .background(Color.White.copy(alpha = 0.92f), CircleShape)
                                .padding(mediaBadgeRingPadding)
                                .background(MaterialTheme.colorScheme.tertiary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = badgeText,
                                maxLines = 1,
                                textAlign = TextAlign.Center,
                                fontSize = mediaBadgeTextSize,
                                lineHeight = mediaBadgeTextSize,
                                color = MaterialTheme.colorScheme.onTertiary
                            )
                        }
                    }
                }
                if (videoCount > 0) {
                    val badgeText = if (videoCount > 99) "99+" else videoCount.toString()
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = mediaBadgeLeftPad(videoSlot), bottom = mediaBadgeBottomPad)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Videocam,
                            contentDescription = null,
                            modifier = Modifier.size(mediaBadgeIconSize),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = mediaBadgeOffsetX, y = mediaBadgeOffsetY)
                                .size(mediaBadgeSize)
                                .background(Color.White.copy(alpha = 0.92f), CircleShape)
                                .padding(mediaBadgeRingPadding)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = badgeText,
                                maxLines = 1,
                                textAlign = TextAlign.Center,
                                fontSize = mediaBadgeTextSize,
                                lineHeight = mediaBadgeTextSize,
                                color = MaterialTheme.colorScheme.onPrimary
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


                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = if (compact) 20.dp else 30.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = preview,
                            modifier = Modifier.align(Alignment.Center),
                            maxLines = noteMaxLines,
                            textAlign = TextAlign.Center,
                            color = adaptivePrimary
                        )
                    }
                }
            }
        }
    }
