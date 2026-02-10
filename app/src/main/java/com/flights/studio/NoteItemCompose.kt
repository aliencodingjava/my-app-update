package com.flights.studio

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import androidx.core.graphics.scale
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.kyant.capsule.ContinuousRoundedRectangle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.IntBuffer
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

private const val LIST_PREVIEW_CHARS = 50

private fun notePreview(s: String): String {
    if (s.isBlank()) return ""
    val oneLine = s
        .replace('\n', ' ')
        .replace('\t', ' ')
        .replace(Regex("\\s+"), " ")
        .trim()
    return if (oneLine.length <= LIST_PREVIEW_CHARS) oneLine else oneLine.take(LIST_PREVIEW_CHARS) + "…"
}

private val ColorVectorConverter: TwoWayConverter<Color, AnimationVector4D> =
    TwoWayConverter(
        convertToVector = { c -> AnimationVector4D(c.red, c.green, c.blue, c.alpha) },
        convertFromVector = { v -> Color(v.v1, v.v2, v.v3, v.v4) }
    )


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
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onEdit: () -> Unit,
    onReminderClick: () -> Unit,
    backdrop: Backdrop,
    titleTopCompactDp: Int,
    titleTopNormalDp: Int,
    isInteractive: Boolean = true,
    ) {
    val railW = if (compact) 45.dp else 60.dp
    val h = if (compact) 80.dp else 120.dp
    val ui = rememberUiScales()
    val iconSize = if (dense) 18.dp else 24.dp
    val btnSize = if (dense) 36.dp else 44.dp
    val titlePadV = if (compact) 4.dp else 7.dp
    val titlePadH = if (compact) 10.dp else 12.dp
    val titleShape = RoundedCornerShape(if (compact) 10.dp else 12.dp)
    val noteMaxLines = if (compact) 2 else 4
    val titleMaxLines = if (compact) 1 else 1
    val colBottom = if (compact) 6.dp else 10.dp
    val afterTitleSpace = if (compact) 4.dp else 10.dp
    val shape = RoundedCornerShape(18.dp)
    val elev = if (selected) 0.dp else if (compact) 0.dp else 0.dp


    val isDark = isSystemInDarkTheme()
    val scheme = MaterialTheme.colorScheme

    val tintColor = scheme.primary.copy(alpha = if (isDark) 0.02f else 0.00f)
    val selectedOverlay = scheme.primary.copy(alpha = 0.60f)
    val containerColor =
        if (!isDark) Color(0xFFFAFAFA).copy(0.0f)
        else Color(0xFF121212).copy(0.1f)

// ✅ KYANT adaptive luminance (inline, no handle)
    val layer = rememberGraphicsLayer()
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    var boxSize by remember { mutableStateOf(IntSize.Zero) }

    val luminanceAnimation = remember { Animatable(if (!isDark) 1f else 0f) }
    val contentColorAnimation = remember {
        Animatable(
            initialValue = if (!isDark) Color.Black else Color.White,
            typeConverter = ColorVectorConverter
        )
    }

    LaunchedEffect(layer) {
        val buffer = IntBuffer.allocate(25)
        val ctx = currentCoroutineContext()

        while (ctx.isActive) {
            // capture size at this tick
            val w = boxSize.width
            val h = boxSize.height
            if (w <= 0 || h <= 0) {
                delay(350)
                continue
            }

            // 1) Fill buffer in IO (Kyant style), but SAFE
            val sampled = withContext(Dispatchers.IO) {
                runCatching {
                    val imageBitmap = layer.toImageBitmap() // can throw when item not recorded / 0x0
                    val thumbnail =
                        imageBitmap.asAndroidBitmap()
                            .scale(5, 5, false)
                            .copy(Bitmap.Config.ARGB_8888, false)

                    buffer.rewind()
                    thumbnail.copyPixelsToBuffer(buffer)
                    true
                }.getOrElse {
                    false
                }
            }

            if (!sampled) {
                // item got recycled / not ready -> skip this tick, no crash
                delay(350)
                continue
            }

            // 2) Compute luminance AFTER IO (Kyant style)
            var sum = 0.0
            for (i in 0 until 25) {
                val c = buffer.get(i)
                val r = (c shr 16 and 0xFF) / 255f
                val g = (c shr 8 and 0xFF) / 255f
                val b = (c and 0xFF) / 255f
                sum += 0.2126 * r + 0.7152 * g + 0.0722 * b
            }
            val averageLuminance = (sum / 25.0).toFloat()

            launch {
                contentColorAnimation.animateTo(
                    if (averageLuminance > 0.5f) Color.Black else Color.White,
                    tween(1000)
                )
            }
            luminanceAnimation.animateTo(averageLuminance, tween(1000))

            delay(350)
        }
    }



    val adaptiveColor = contentColorAnimation.value

    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }

    val interactiveLayer = if (isInteractive) {
        Modifier
//            .then(interactiveHighlight.modifier)
            .then(interactiveHighlight.gestureModifier)
            .graphicsLayer {
                val width = size.width
                val height = size.height

                val progress = interactiveHighlight.pressProgress
                val baseScale = lerp(1f, 1f + 2f.dp.toPx() / height, progress)

                val maxOffset = size.minDimension
                val initialDerivative = 0.02f
                val offset = interactiveHighlight.offset
                translationX = maxOffset * tanh(initialDerivative * offset.x / maxOffset)
                translationY = maxOffset * tanh(initialDerivative * offset.y / maxOffset)

                val maxDragScale = 1f.dp.toPx() / height
                val offsetAngle = atan2(offset.y, offset.x)

                scaleX =
                    baseScale +
                            maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) *
                            (width / height).fastCoerceAtMost(1f)

                scaleY =
                    baseScale +
                            maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) *
                            (height / width).fastCoerceAtMost(1f)
            }
    } else Modifier

    // ✅ OUTER ELEVATION WRAPPER
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(h)
            .then(interactiveLayer)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Carousel, // ✅ ROLE HERE
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = shape,
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = elev
    ) {
        Box(
            modifier = Modifier
                .onSizeChanged { boxSize = it }
                .drawBackdrop(
                    backdrop = backdrop,
                    shadow = null,
                    shape = { ContinuousRoundedRectangle(18f.dp) },
                    highlight = {
                        if (isDark) {
                            Highlight(
                                width = 0.45.dp,
                                blurRadius = 1.6.dp,
                                alpha = 0.30f,
                                style = HighlightStyle.Plain
                            )
                        } else {
                            Highlight(
                                width = 0.30.dp,
                                blurRadius = 1.0.dp,
                                alpha = 0.35f,
                                style = HighlightStyle.Plain // very subtle
                            )
                        }
                    },
                    effects = {
                        vibrancy()

                        colorControls(
                            brightness = if (isDark) 0.06f else -0.06f,
                            contrast   = if (isDark) 1.06f else 1.00f,
                            saturation = if (isDark) 1.14f else 1.10f
                        )
                        blur(
                            radius = 0f.dp.toPx(),
                            edgeTreatment = TileMode.Clamp
                        )

                        lens(
                            refractionHeight = 24f.dp.toPx(),
                            refractionAmount = 24f.dp.toPx(),
                            depthEffect = true,
                            chromaticAberration = false
                        )
                    },
                    onDrawBackdrop = { drawBackdrop ->
                        drawBackdrop()
                        if (boxSize.width > 0 && boxSize.height > 0) {
                            layer.record(
                                density = density,
                                layoutDirection = layoutDirection,
                                size = boxSize
                            ) { drawBackdrop() }
                        }
                    },
                    onDrawSurface = {
                        if (selected) drawRect(selectedOverlay)
                        drawRect(tintColor)
                        drawRect(containerColor)
                    }
                )
                .combinedClickable(
//                    interactionSource = remember { MutableInteractionSource() },
//                    indication = if (isInteractive) null else LocalIndication.current,
                    onClick = onClick,
                    onLongClick = onLongClick,
                    role = Role.Carousel,
                )
                .fillMaxWidth()
                .height(h)

        ) {

            Box(Modifier.fillMaxSize()) {

                // RIGHT ACTION RAIL
                Surface(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(railW)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                    color = MaterialTheme.colorScheme.background.copy(alpha = if (isDark) 0.80f else 0.70f),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = if (compact) 4.dp else 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
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
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(Modifier.height(if (compact) 6.dp else 12.dp))

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
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
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

                                        @Suppress("KotlinConstantConditions")
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


                // LEFT SELECT (Radio)
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



                // ✅ IMAGES BADGE — PRO (slightly bigger everywhere)
                if (imagesCount > 0) {
                    val imgIconSize = when {
                        dense -> 19.dp
                        compact -> 23.dp
                        else -> 27.dp
                    }

                    // 🔼 badge circle slightly bigger
                    val badgeSize = when {
                        dense -> 12.dp
                        compact -> 14.dp
                        else -> 15.dp
                    }

                    val badgeOffsetX = when {
                        dense -> 2.dp
                        compact -> 3.dp
                        else -> 4.dp
                    }
                    val badgeOffsetY = when {
                        dense -> (-2).dp
                        compact -> (-3).dp
                        else -> (-3).dp
                    }

                    val leftPad = when {
                        dense -> 8.dp
                        compact -> 10.dp
                        else -> 14.dp
                    }
                    val bottomPad = when {
                        dense -> 8.dp
                        compact -> 9.dp
                        else -> 12.dp
                    }

                    val badgeText = if (imagesCount > 99) "99+" else imagesCount.toString()
                    val ringColor = if (isSystemInDarkTheme())
                        Color.White.copy(alpha = 0.9f)
                    else
                        Color.White.copy(alpha = 0.95f)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = leftPad, bottom = bottomPad)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_oui_image),
                            contentDescription = null,
                            modifier = Modifier.size(imgIconSize),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = badgeOffsetX, y = badgeOffsetY)
                                .size(badgeSize)
                                .background(
                                    color = ringColor,   // 🤍 white ring in BOTH dark + light
                                    shape = CircleShape
                                )
                                .padding(
                                    when {
                                        dense -> 1.25.dp
                                        compact -> 1.25.dp
                                        else -> 1.5.dp   // 🔼 outline thicker
                                    }
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
                                fontSize = when {
                                    dense -> 8.sp
                                    compact -> 9.sp
                                    else -> 9.sp   // 🔼 text bigger
                                },
                                lineHeight = when {
                                    dense -> 8.sp
                                    compact -> 9.sp
                                    else -> 9.sp
                                },
                                color = MaterialTheme.colorScheme.onError
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
                            color = MaterialTheme.colorScheme.background.copy(alpha = if (isDark) 0.80f else 0.70f),

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
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(afterTitleSpace))
                    }
                    val preview = remember(note) { notePreview(note) }   // ✅ cached per note
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
}
