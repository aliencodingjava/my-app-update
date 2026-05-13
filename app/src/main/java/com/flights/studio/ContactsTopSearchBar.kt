package com.flights.studio

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle

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
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val surfaceColor = if (isDark) {
        Color(0xFF34363C).copy(alpha = 0.76f)
    } else {
        Color(0xFFE6E2E7).copy(alpha = 0.58f)
    }
    val contentColor = if (isDark) {
        Color.White.copy(alpha = 0.92f)
    } else {
        Color(0xFF1E1F24)
    }
    val hintColor = contentColor.copy(alpha = 0.62f)
    var expanded by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val cornerShape = RoundedCornerShape(27.dp)

    LaunchedEffect(visible) {
        if (!visible) expanded = false
    }

    LaunchedEffect(expanded, visible) {
        if (expanded && visible) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(animationSpec = tween(durationMillis = 160)) +
            scaleIn(animationSpec = tween(durationMillis = 240), initialScale = 0.74f),
        exit = fadeOut(animationSpec = tween(durationMillis = 120)) +
            scaleOut(animationSpec = tween(durationMillis = 160), targetScale = 0.82f)
    ) {
        Box(
            modifier = Modifier
                .then(
                    if (expanded) {
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    } else {
                        Modifier.size(56.dp)
                    }
                )
                .clip((cornerShape))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { expanded = true }
                )
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { cornerShape },
                    shadow = null,
                    highlight = null,
                    effects = {
                        vibrancy()
                        blur(3.dp.toPx())
                        lens(
                            refractionHeight = 14.dp.toPx(),
                            refractionAmount = 48.dp.toPx(),
                            depthEffect = false,
                            chromaticAberration = true
                        )
                    },
                    onDrawSurface = { drawRect(surfaceColor) }
                )
                .background(
                    color = if (isDark) Color.White.copy(alpha = 0.06f)
                    else Color(0xFF7A7480).copy(alpha = 0.06f),
                    shape = (cornerShape)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (!expanded) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = stringResource(R.string.search),
                    modifier = Modifier.size(20.dp),
                    tint = hintColor
                )
            } else {
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = contentColor,
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(focusRequester),
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
                                    fontSize = 13.sp,
                                    lineHeight = 17.sp,
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
                                .size(26.dp)
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
                                modifier = Modifier.size(13.dp),
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
}
