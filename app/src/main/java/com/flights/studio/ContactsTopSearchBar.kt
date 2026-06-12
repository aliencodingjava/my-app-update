package com.flights.studio

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import kotlinx.coroutines.delay

@Composable
fun ContactsTopSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val surfaceColor = if (isDark) {
        Color(0xFF202124).copy(alpha = 0.86f)
    } else {
        Color(0xFFE6E2E7).copy(alpha = 0.86f)
    }
    val contentColor = if (isDark) {
        Color.White.copy(alpha = 0.92f)
    } else {
        Color(0xFF1E1F24)
    }
    val hintColor = contentColor.copy(alpha = 0.56f)
    val backdrop = rememberLayerBackdrop()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 12.dp, top = 48.dp, end = 12.dp, bottom = 8.dp)
            .height(50.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { CircleShape },
                    shadow = null,
                    highlight = {
                        Highlight(
                            width = if (isDark) 0.45.dp else 0.30.dp,
                            blurRadius = if (isDark) 1.6.dp else 1.dp,
                            alpha = if (isDark) 0.50f else 0.80f,
                            style = HighlightStyle.Plain
                        )
                    },
                    effects = {
                        vibrancy()
                        blur(2.dp.toPx())
                        lens(
                            refractionHeight = 24.dp.toPx(),
                            refractionAmount = 24.dp.toPx(),
                            depthEffect = false,
                            chromaticAberration = false
                        )
                    },
                    onDrawSurface = { drawRect(surfaceColor) }
                )
                .background(
                    color = if (isDark) Color.Black.copy(alpha = 0.10f)
                    else Color(0xFF7A7480).copy(alpha = 0.10f),
                    shape = CircleShape
                )
        ) {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = contentColor,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.fillMaxSize(),
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 14.dp, end = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = hintColor
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (query.isBlank()) {
                                Text(
                                    text = stringResource(R.string.search),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 14.sp,
                                        lineHeight = 18.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = hintColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            innerTextField()
                        }
                        if (query.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(
                                        color = contentColor.copy(alpha = if (isDark) 0.12f else 0.08f),
                                        shape = CircleShape
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { onQueryChange("") }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = contentColor.copy(alpha = 0.82f)
                                )
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun ContactsFloatingSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    backdrop: Backdrop,
    visible: Boolean,
    active: Boolean = false,
    onActiveChange: (Boolean) -> Unit = {},
    onClose: () -> Unit = {},
    contentView: android.view.View? = null,
    modifier: Modifier = Modifier,
    thin: Boolean = false
) {
    val isDark = isSystemInDarkTheme()
    val surfaceColor = if (thin) {
        if (isDark) Color(0xFF202126) else Color(0xFFF4F5F8)
    } else {
        bottomTabBarTint()
    }
    val overlayTint = if (thin) {
        if (isDark) Color(0xFF23242A) else Color(0xFFF7F8FB)
    } else {
        bottomTabBarOverlayTint()
    }
    val contentColor = if (isDark) {
        Color.White.copy(alpha = 0.92f)
    } else {
        Color(0xFF1E1F24)
    }
    val hintColor = contentColor.copy(alpha = 0.62f)
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val wave by rememberInfiniteTransition(label = "contacts_search_wave")
        .animateFloat(
            initialValue = 0.055f,
            targetValue = 0.11f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2600, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "contacts_search_wave_alpha"
        )

    val barHeight = if (thin) 40.dp else GlassChromeCornerRadius * 2
    val cornerShape = RoundedCornerShape(if (thin) 8.dp else 27.dp)

    LaunchedEffect(visible) {
        if (!visible) {
            delay(320)
            onActiveChange(false)
        }
    }

    LaunchedEffect(query) {
        if (query.isNotBlank()) {
            onActiveChange(true)
        }
    }

    LaunchedEffect(active, visible) {
        if (active && visible) {
            delay(80)
            runCatching { focusRequester.requestFocus() }
            keyboardController?.show()
        }
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)) +
            slideInVertically(animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing), initialOffsetY = { -it / 3 }) +
            scaleIn(animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing), initialScale = 0.98f),
        exit = fadeOut(animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)) +
            slideOutVertically(animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing), targetOffsetY = { -it / 2 }) +
            scaleOut(animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing), targetScale = 0.985f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .shadow(if (thin) 0.dp else GlassChromeShadowElevation, cornerShape, clip = false)
                .clip(cornerShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        onActiveChange(true)
                    }
                )
                .background(
                    color = overlayTint,
                    shape = cornerShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = if (active) wave else wave * 0.55f }
                    .background(Color.White, cornerShape)
            )
            AndroidView(
                modifier = Modifier.matchParentSize(),
                factory = { FrostedActionBarBlurView(it) },
                update = {
                    it.contentView = contentView
                    it.scrimColor = surfaceColor.toArgb()
                    it.cornerRadiusPx = it.resources.displayMetrics.density * if (thin) 8f else GlassChromeCornerRadius.value
                    it.useLiquidRefraction = !thin
                    it.blurRadiusPx = if (thin) 4f else GlassChromeNativeBlurPx
                    it.saturation = if (thin) 1.08f else 1.18f
                    it.refractIntensity = if (thin) 0f else GlassChromeNativeRefractionIntensity
                }
            )
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = contentColor,
                    fontSize = if (thin) 12.sp else 13.sp,
                    lineHeight = if (thin) 16.sp else 17.sp,
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester),
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 14.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                        tint = hintColor
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (query.isBlank()) {
                            Text(
                                text = stringResource(R.string.search),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = if (thin) 12.sp else 13.sp,
                                    lineHeight = if (thin) 16.sp else 17.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = hintColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        innerTextField()
                    }
                    Box(
                        modifier = Modifier
                            .size(27.dp)
                            .background(
                                color = contentColor.copy(alpha = if (isDark) 0.13f else 0.08f),
                                shape = CircleShape
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    onQueryChange("")
                                    onActiveChange(false)
                                    onClose()
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = contentColor.copy(alpha = 0.68f)
                        )
                    }
                }
            }
            )
        }
    }
}
