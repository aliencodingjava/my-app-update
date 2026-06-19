@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package com.flights.studio

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.OpenableColumns
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.SplitButtonShapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.app.ActivityCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.flights.studio.GeminiTitles.looksLikeCode
import com.google.android.material.transition.platform.MaterialSharedAxis
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur as backdropBlur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.io.File
import java.util.Locale
import androidx.compose.foundation.lazy.itemsIndexed as listItemsIndexed

data class TipMeta(
    val headline: String,
    val message: String,
    val reason: String
)

data class TitleSuggestion(
    val title: String,
    val why: String
)

private fun addNoteTipMetaFor(note: String, hasImages: Boolean): TipMeta {
    val n = note.trim()
    val lines = n.lines().map { it.trim() }.filter { it.isNotBlank() }
    val hasLink = Regex("""https?://\S+""").containsMatchIn(n)
    val hasChecklist =
        lines.any { it.startsWith("□") || it.startsWith("☐") || it.startsWith("- ") || it.startsWith("• ") || it.matches(Regex("""\d+\.\s+.*""")) }
    val isLong = n.length >= 800

    return when {
        hasImages && n.length >= 60 -> TipMeta(
            headline = "Title suggestion",
            message = "You added photos. A short title helps you spot this note instantly later.",
            reason = "Reason: photos attached"
        )

        hasLink -> TipMeta(
            headline = "Title suggestion",
            message = "This note contains a link. Adding a title makes it easier to find when you search.",
            reason = "Reason: contains a link"
        )

        hasChecklist -> TipMeta(
            headline = "Quick organization",
            message = "This looks like a list. A simple title keeps it tidy and searchable.",
            reason = "Reason: checklist / list"
        )

        isLong -> TipMeta(
            headline = "Save time later",
            message = "This is a long note. A title now will save you scrolling later.",
            reason = "Reason: long note"
        )

        else -> TipMeta(
            headline = "Title helper",
            message = "Want a title? Pick one now or skip and come back later from Info.",
            reason = "Tip: optional"
        )
    }
}

private fun addNoteBuildTitleSuggestions(note: String, hasImages: Boolean): List<TitleSuggestion> {
    val n = note.trim()
    val lines = n.lines().map { it.trim() }.filter { it.isNotBlank() }

    fun clean(s: String) = s
        .replace(Regex("""https?://\S+"""), "")
        .replace(Regex("""[#*_`>|]+"""), " ")
        .replace(Regex("""\s+"""), " ")
        .trim()

    val link = Regex("""https?://\S+""").find(n)?.value
    val firstLine = lines.firstOrNull()?.let(::clean).orEmpty()
    val firstSentence = clean(n.split(Regex("""[.!?\n]""")).firstOrNull().orEmpty())
    val picks = mutableListOf<TitleSuggestion>()

    if (!link.isNullOrBlank()) {
        val domain = runCatching {
            link.toUri().host?.removePrefix("www.")
        }.getOrNull()
        if (!domain.isNullOrBlank()) {
            picks += TitleSuggestion(domain.take(32), "Pulled from the link domain")
        }
        picks += TitleSuggestion("Link to check", "This note contains a link")
    }

    if (hasImages) {
        picks += TitleSuggestion("Photos", "You attached images")
        picks += TitleSuggestion("Receipt / Screenshot", "Common photo note type")
    }

    val hasChecklist = lines.any {
        it.startsWith("□") || it.startsWith("☐") || it.startsWith("- ") || it.startsWith("• ") || it.matches(Regex("""\d+\.\s+.*"""))
    }
    if (hasChecklist) {
        picks += TitleSuggestion("To-do", "Looks like a checklist")
        picks += TitleSuggestion("Shopping list", "List-style note")
    }

    if (firstSentence.length >= 8) {
        picks += TitleSuggestion(firstSentence.take(40), "From the first sentence")
    }
    if (firstLine.length in 8..60) {
        picks += TitleSuggestion(firstLine.take(40), "From the first line")
    }

    if (picks.isEmpty()) {
        picks += TitleSuggestion("Quick note", "Simple default")
        picks += TitleSuggestion("Idea", "Good for short thoughts")
        picks += TitleSuggestion("Reminder", "Good generic label")
    }

    return picks
        .map { it.copy(title = it.title.trim().ifBlank { "Quick note" }) }
        .distinctBy { it.title.lowercase() }
        .take(6)
}

@Composable
private fun AddNoteTitleHelperSheetContent(
    meta: TipMeta,
    currentTitle: String,
    disableTips: Boolean,
    pickedIndex: Int,
    suggestions: List<TitleSuggestion>,
    aiLoading: Boolean,
    aiError: String?,
    panelInner: Color,
    panelBorder: Color,
    onPick: (Int) -> Unit,
    onToggleDisable: () -> Unit,
    onDismiss: () -> Unit,
    onPrimary: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()
    val helperTextColor = if (isDark) {
        Color.White.copy(alpha = 0.90f)
    } else {
        Color(0xFF111318)
    }
    val visibleSuggestions = suggestions.take(4)
    val picked = suggestions.getOrNull(pickedIndex) ?: suggestions.firstOrNull()
    val actionText = when {
        disableTips -> "Turn off helper"
        currentTitle.isBlank() -> "Use title"
        else -> "Replace title"
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AddNoteMenuIcon(iconText = "AI", accent = scheme.primary)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Title helper",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = when {
                        aiLoading -> "Thinking..."
                        aiError != null -> "Using local suggestions"
                        else -> meta.reason
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = helperTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = scheme.primary.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, scheme.primary.copy(alpha = 0.22f))
            ) {
                Text(
                    text = "Done",
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismiss
                        )
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = scheme.primary
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = meta.message,
            style = MaterialTheme.typography.bodySmall,
            color = helperTextColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            visibleSuggestions.forEachIndexed { index, suggestion ->
                val selected = index == pickedIndex && !disableTips
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onPick(index) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (selected) scheme.primary.copy(alpha = 0.14f) else panelInner,
                    border = BorderStroke(
                        1.dp,
                        if (selected) scheme.primary.copy(alpha = 0.35f) else panelBorder
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = if (selected) scheme.primary else Color.Transparent,
                            border = if (selected) null else BorderStroke(1.dp, scheme.outlineVariant),
                            modifier = Modifier.size(22.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = scheme.onPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = suggestion.title,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = scheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = suggestion.why,
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.onSurfaceVariant.copy(alpha = 0.72f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggleDisable
                    ),
                shape = RoundedCornerShape(18.dp),
                color = if (disableTips) scheme.error.copy(alpha = 0.12f) else panelInner,
                border = BorderStroke(
                    1.dp,
                    if (disableTips) scheme.error.copy(alpha = 0.35f) else panelBorder
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (disableTips) scheme.error else Color.Transparent,
                        border = if (disableTips) null else BorderStroke(1.dp, scheme.outlineVariant),
                        modifier = Modifier.size(22.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (disableTips) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = scheme.onError,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(9.dp))
                    Text(
                        text = "Turn off",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = if (disableTips) scheme.error else scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            val sendInteraction = remember { MutableInteractionSource() }
            val sendPressed by sendInteraction.collectIsPressedAsState()
            val sendScale by animateFloatAsState(
                targetValue = if (sendPressed) 0.96f else 1f,
                animationSpec = tween(durationMillis = 110),
                label = "titleHelperSendScale"
            )
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .graphicsLayer {
                        scaleX = sendScale
                        scaleY = sendScale
                    }
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(
                        interactionSource = sendInteraction,
                        indication = null,
                        onClick = onPrimary
                    ),
                shape = RoundedCornerShape(18.dp),
                color = if (disableTips) scheme.error else scheme.primary,
                border = BorderStroke(
                    1.dp,
                    if (disableTips) scheme.error.copy(alpha = 0.50f) else scheme.primary.copy(alpha = 0.35f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = actionText,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                            color = if (disableTips) scheme.onError else scheme.onPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!disableTips && picked != null) {
                            Text(
                                text = picked.title.take(18),
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.onPrimary.copy(alpha = 0.78f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Icon(
                        imageVector = if (disableTips) Icons.Filled.Check else Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        tint = if (disableTips) scheme.onError else scheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AddNoteMenuAction(
    iconText: String,
    title: String,
    subtitle: String,
    accent: Color,
    container: Color,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    showDot: Boolean = false,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = container,
        border = BorderStroke(1.dp, scheme.outline.copy(alpha = 0.14f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AddNoteMenuIcon(iconText = iconText, accent = accent)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant.copy(alpha = 0.76f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (showDot) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(scheme.error)
                )
            }

            if (trailing != null) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = accent.copy(alpha = 0.13f),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.18f))
                ) {
                    Text(
                        text = trailing,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = accent
                    )
                }
            }
        }
    }
}

@Composable
private fun AddNoteMenuCompactAction(
    iconText: String,
    title: String,
    accent: Color,
    container: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier
            .height(86.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = container,
        border = BorderStroke(1.dp, scheme.outline.copy(alpha = 0.14f))
    ) {
        Column(
            modifier = Modifier.padding(11.dp),
            verticalArrangement = Arrangement.Center
        ) {
            AddNoteMenuIcon(iconText = iconText, accent = accent)
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AddNoteTodoTemplateCard(
    template: AddNoteTodoTemplate,
    accent: Color,
    container: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier
            .height(126.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = container,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.22f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AddNoteMenuIcon(iconText = template.icon, accent = accent)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = template.title,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = template.badge,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = template.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant.copy(alpha = 0.82f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .height(4.dp)
                        .weight(1f)
                        .clip(RoundedCornerShape(999.dp))
                        .background(accent.copy(alpha = 0.26f))
                )
                Spacer(Modifier.width(9.dp))
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = accent.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = "Insert",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = accent,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun AddNoteMenuIcon(
    iconText: String,
    accent: Color
) {
    Surface(
        modifier = Modifier.size(34.dp),
        shape = RoundedCornerShape(13.dp),
        color = accent.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.18f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = iconText,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
    }
}

@Composable
private fun AddNoteMenuStatusChip(
    text: String,
    active: Boolean,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val color = if (active) scheme.primary else scheme.onSurfaceVariant
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = if (active) 0.14f else 0.08f),
        border = BorderStroke(1.dp, color.copy(alpha = if (active) 0.22f else 0.12f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AddNoteVoiceRecorderCard(
    voiceNotes: List<AddNoteVoiceAttachment>,
    isRecording: Boolean,
    elapsedMs: Long,
    level: Float,
    isDark: Boolean,
    compact: Boolean,
    onPressStart: () -> Unit,
    onPressEnd: (Boolean) -> Unit,
    onRemove: (AddNoteVoiceAttachment) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val accent = if (isRecording) scheme.error else scheme.primary
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = if (isDark) Color(0xFF1D242C) else Color(0xFFF4F8FC),
        border = BorderStroke(
            1.dp,
            if (isRecording) scheme.error.copy(alpha = 0.44f) else scheme.outline.copy(alpha = 0.14f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = if (compact) 8.dp else 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier.size(if (compact) 34.dp else 38.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = accent.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.20f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (isRecording) "REC" else "Mic",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                            color = accent,
                            maxLines = 1
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isRecording) "Recording voice note" else "Voice note",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isRecording) "Release to save ${formatVoiceDuration(elapsedMs)}" else "Hold mic, talk, release to attach",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant.copy(alpha = 0.82f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    modifier = Modifier
                        .height(if (compact) 34.dp else 38.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                onPressStart()
                                val up = waitForUpOrCancellation()
                                onPressEnd(up == null)
                            }
                        },
                    shape = RoundedCornerShape(999.dp),
                    color = accent.copy(alpha = if (isRecording) 0.24f else 0.14f),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.28f))
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isRecording) "Release" else "Hold",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = accent,
                            maxLines = 1
                        )
                    }
                }
            }

            if (isRecording) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    repeat(18) { index ->
                        val wave = ((index % 6) + 1) / 6f
                        val height = (6 + (22 * (level * 0.65f + wave * 0.35f))).dp
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(height)
                                .clip(RoundedCornerShape(999.dp))
                                .background(accent.copy(alpha = 0.34f + (level * 0.32f)))
                        )
                    }
                }
            }

            if (voiceNotes.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    voiceNotes.forEachIndexed { index, item ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = if (isDark) Color(0xFF26313B) else Color.White.copy(alpha = 0.70f),
                            border = BorderStroke(1.dp, scheme.outline.copy(alpha = 0.10f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Voice ${index + 1}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = scheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = formatVoiceDuration(item.durationMs),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = scheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Remove",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = scheme.error,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .clickable { onRemove(item) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddNoteQuickToolPill(
    label: String,
    detail: String,
    accent: Color,
    selected: Boolean,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    starIcon: Boolean = false,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) accent.copy(alpha = 0.18f) else scheme.surfaceVariant.copy(alpha = 0.48f),
        border = BorderStroke(1.dp, accent.copy(alpha = if (selected) 0.34f else 0.16f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            when {
                starIcon -> Text(
                    text = "✦",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                    color = accent,
                    maxLines = 1
                )
                icon != null -> Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(13.dp)
                )
                else -> Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(accent.copy(alpha = if (selected) 1f else 0.58f))
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AddNoteFloatingToolsBar(
    backdrop: LayerBackdrop,
    isRecording: Boolean,
    elapsedMs: Long,
    wantsReminder: Boolean,
    fileCount: Int,
    tipEnabled: Boolean,
    modifier: Modifier = Modifier,
    onVoiceStart: () -> Unit,
    onVoiceEnd: (Boolean) -> Unit,
    onReminder: () -> Unit,
    onAttach: () -> Unit,
    onSuggest: () -> Unit,
    onOpenSheet: () -> Unit
) {
    val glassColor = bottomTabBarTint()
    val overlayTint = bottomTabBarOverlayTint()
    val backdropBlurDp = bottomChromeBackdropBlurDp()

    Box(
        modifier = modifier
            .padding(horizontal = GlassChromeHorizontalPadding)
            .fillMaxWidth()
            .height(56.dp)
            .shadow(GlassChromeShadowElevation, GlassChromeShape, clip = false)
            .clip(GlassChromeShape)
            .adaptiveLiquidGlassBackdrop(
                backdrop = backdrop,
                shape = GlassChromeShape,
                surfaceColor = glassColor,
                blurDp = backdropBlurDp,
                shadow = { bottomChromeShadow() },
                refractionHeightDp = GlassChromeRefractionHeightDp,
                refractionAmountDp = GlassChromeRefractionAmountDp
            )
            .background(overlayTint, GlassChromeShape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AddNoteFloatingToolTab(
                iconText = if (isRecording) formatVoiceDuration(elapsedMs) else "V",
                label = "Voice",
                selected = isRecording,
                holdToUse = true,
                onPressStart = onVoiceStart,
                onPressEnd = onVoiceEnd
            )
            AddNoteFloatingToolTab(
                icon = if (wantsReminder) Icons.Filled.Notifications else Icons.Filled.NotificationsNone,
                label = "Alert",
                selected = wantsReminder,
                onClick = onReminder
            )
            AddNoteFloatingToolTab(
                icon = Icons.Filled.AttachFile,
                label = if (fileCount == 0) "File" else fileCount.toString(),
                selected = fileCount > 0,
                onClick = onAttach
            )
            AddNoteFloatingToolTab(
                icon = Icons.Rounded.AutoAwesome,
                label = if (tipEnabled) "AI" else "Off",
                selected = false,
                onClick = onSuggest
            )
            AddNoteFloatingToolTab(
                icon = Icons.Filled.Menu,
                label = "Menu",
                selected = false,
                onClick = onOpenSheet
            )
        }
    }
}

@Composable
private fun RowScope.AddNoteFloatingToolTab(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconText: String? = null,
    strong: Boolean = false,
    holdToUse: Boolean = false,
    onPressStart: () -> Unit = {},
    onPressEnd: (Boolean) -> Unit = {},
    onClick: () -> Unit = {}
) {
    val pressSource = remember { MutableInteractionSource() }
    val pressed by pressSource.collectIsPressedAsState()
    val selectedColor = bottomTabSelectedContentColor()
    val inactiveColor = bottomTabInactiveColor()
    val selectedPillColor = bottomTabSelectedPillColor()
    val contentColor = when {
        strong -> selectedColor
        selected -> selectedColor
        else -> inactiveColor
    }
    val pillAlpha by animateFloatAsState(
        targetValue = when {
            strong -> 0.22f
            selected -> 1f
            pressed -> 0.12f
            else -> 0f
        },
        animationSpec = tween(durationMillis = 140),
        label = "addNoteFloatingTabPill"
    )

    val clickModifier = if (holdToUse) {
        Modifier.pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                onPressStart()
                val up = waitForUpOrCancellation()
                onPressEnd(up == null)
            }
        }
    } else {
        Modifier.clickable(
            interactionSource = pressSource,
            indication = null,
            onClick = onClick
        )
    }

    Box(
        modifier = modifier
            .weight(1f)
            .height(48.dp)
            .clip(GlassChromeInnerShape)
            .then(clickModifier),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = pillAlpha }
                .background(selectedPillColor, GlassChromeInnerShape)
        )
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = contentColor,
                    modifier = Modifier.size(if (strong) 22.dp else 19.dp)
                )
            } else {
                Text(
                    text = iconText.orEmpty(),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (selected || strong) FontWeight.Black else FontWeight.Medium
                ),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AddNoteVoiceHoldPill(
    isRecording: Boolean,
    elapsedMs: Long,
    modifier: Modifier = Modifier,
    onPressStart: () -> Unit,
    onPressEnd: (Boolean) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val accent = if (isRecording) scheme.error else scheme.primary
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(999.dp))
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    onPressStart()
                    val up = waitForUpOrCancellation()
                    onPressEnd(up == null)
                }
            },
        shape = RoundedCornerShape(999.dp),
        color = accent.copy(alpha = if (isRecording) 0.20f else 0.12f),
        border = BorderStroke(1.dp, accent.copy(alpha = if (isRecording) 0.42f else 0.20f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(if (isRecording) 8.dp else 6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(accent)
            )
            Text(
                text = if (isRecording) formatVoiceDuration(elapsedMs) else "Voice",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AddNoteVoiceInputChip(
    voiceNotes: List<AddNoteVoiceAttachment>,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    onRemove: (AddNoteVoiceAttachment) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val accent = scheme.primary
    val container = if (isDark) Color(0xFF232425) else Color(0xFFFEFEFE)
    androidx.compose.foundation.lazy.LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        listItemsIndexed(voiceNotes) { index, item ->
            Surface(
                modifier = Modifier.height(36.dp),
                shape = RoundedCornerShape(999.dp),
                color = container,
                border = BorderStroke(1.dp, scheme.outline.copy(alpha = if (isDark) 0.24f else 0.18f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(accent)
                    )
                    Text(
                        text = formatVoiceDuration(item.durationMs),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Remove",
                        modifier = Modifier
                            .size(18.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .clickable { onRemove(item) }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        tint = scheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

class AddNoteComposeActivity : ComponentActivity() {

    companion object {
        @Suppress("unused")
        fun newIntent(context: Context): Intent =
            Intent(context, AddNoteComposeActivity::class.java)
    }
    override fun onCreate(savedInstanceState: Bundle?) {

        window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)

        // Entering FORWARD
        window.enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true).apply {
            addTarget(android.R.id.content)
        }

        // Returning BACKWARD
        window.returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false).apply {
            addTarget(android.R.id.content)
        }

        window.allowEnterTransitionOverlap = true
        window.allowReturnTransitionOverlap = true
        super.onCreate(savedInstanceState)

        // Let Compose handle system bars paddings
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING or
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        )

        setContent {
            // ✅ match your other pages: set bar icon colors from Compose (no NPE)
            val isDark = isSystemInDarkTheme()
            val view = LocalView.current

            SideEffect {
                val w = (view.context as Activity).window
                WindowCompat.getInsetsController(w, view).apply {
                    isAppearanceLightStatusBars = !isDark
                    isAppearanceLightNavigationBars = !isDark
                }
            }

            // ✅ Use your app theme
            FlightsTheme {
                AddNoteScreen(
                    onBack = { finishWithAnim() },
                    onSave = { note, title, images, voiceNotes, fileAttachments, wantsReminder ->
                        if (note.isBlank()) return@AddNoteScreen

                        val result = Intent().apply {
                            putExtra("NEW_NOTE", note)
                            putExtra("NEW_NOTE_TITLE", title.trim())
                            putStringArrayListExtra(
                                "NEW_NOTE_IMAGES",
                                ArrayList(images.map { it.toString() })
                            )
                            putStringArrayListExtra(
                                "NEW_NOTE_VOICE_URIS",
                                ArrayList(voiceNotes.map { it.uri.toString() })
                            )
                            putExtra(
                                "NEW_NOTE_VOICE_DURATIONS",
                                voiceNotes.map { it.durationMs }.toLongArray()
                            )
                            putExtra(
                                "NEW_NOTE_VOICE_CREATED_AT",
                                voiceNotes.map { it.createdAtMs }.toLongArray()
                            )
                            putStringArrayListExtra(
                                "NEW_NOTE_FILE_URIS",
                                ArrayList(fileAttachments.map { it.uri.toString() })
                            )
                            putStringArrayListExtra(
                                "NEW_NOTE_FILE_NAMES",
                                ArrayList(fileAttachments.map { it.name })
                            )
                            putStringArrayListExtra(
                                "NEW_NOTE_FILE_MIMES",
                                ArrayList(fileAttachments.map { it.mime.orEmpty() })
                            )
                            putExtra(
                                "NEW_NOTE_FILE_SIZES",
                                fileAttachments.map { it.sizeBytes }.toLongArray()
                            )
                            putExtra("NEW_NOTE_WANTS_REMINDER", wantsReminder)
                        }
                        setResult(RESULT_OK, result)
                        finishWithAnim()
                    }
                )
            }

        }
    }


    private fun finishWithAnim() {
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation)

    }
}
private enum class SheetViewMode { Grid, Large, List }
private enum class AddNoteToolMenuPage { Main, TodoTemplates }

private data class AddNoteTodoTemplate(
    val icon: String,
    val title: String,
    val subtitle: String,
    val badge: String,
    val body: String
)

private data class AddNoteVoiceAttachment(
    val uri: Uri,
    val durationMs: Long,
    val createdAtMs: Long
)

private data class AddNoteFileAttachment(
    val uri: Uri,
    val name: String,
    val mime: String?,
    val sizeBytes: Long
)

private fun AddNoteFileAttachment.extension(): String =
    name.substringAfterLast('.', missingDelimiterValue = "").lowercase()

private fun AddNoteFileAttachment.isAudioFile(): Boolean {
    val m = mime.orEmpty().lowercase()
    return m.startsWith("audio/") || extension() in setOf("mp3", "m4a", "aac", "wav", "flac", "ogg", "opus", "amr")
}

private fun AddNoteFileAttachment.isVideoFile(): Boolean {
    val m = mime.orEmpty().lowercase()
    return m.startsWith("video/") || extension() in setOf("mp4", "mov", "m4v", "mkv", "webm", "avi", "3gp")
}

private fun AddNoteFileAttachment.sortBucket(): Int = when {
    isAudioFile() -> 1
    isVideoFile() -> 2
    else -> 0
}

private fun formatVoiceDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(Locale.US, minutes, seconds)
}

private fun createVoiceNoteFile(context: Context): File {
    val dir = File(context.filesDir, "voice_notes").apply { mkdirs() }
    return File(dir, "voice_${System.currentTimeMillis()}.m4a")
}

private fun queryAttachmentMeta(context: Context, uri: Uri): Triple<String, String?, Long> {
    val cr = context.contentResolver
    var name: String? = null
    var size = -1L
    cr.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
        ?.use { c ->
            if (c.moveToFirst()) {
                val nameIdx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = c.getColumnIndex(OpenableColumns.SIZE)
                if (nameIdx >= 0) name = c.getString(nameIdx)
                if (sizeIdx >= 0 && !c.isNull(sizeIdx)) size = c.getLong(sizeIdx)
            }
        }
    val fallback = uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
        ?: "attachment_${System.currentTimeMillis()}"
    val mime = cr.getType(uri)
    return Triple(name?.takeIf { it.isNotBlank() } ?: fallback, mime, size.coerceAtLeast(0L))
}

private const val MAX_NOTE_CHARS = 5_000_000      // you can raise to 5_000_000 if you want
private const val PASTE_JUMP = 5_000              // consider it a paste if jump is big
private const val PASTE_CHUNK = 8_192             // chunk size for safe appending



@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AddNoteScreen(
    onBack: () -> Unit,
    onSave: (note: String, title: String, images: List<Uri>, voiceNotes: List<AddNoteVoiceAttachment>, fileAttachments: List<AddNoteFileAttachment>, wantsReminder: Boolean) -> Unit
) {

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    val noteTap = remember { MutableInteractionSource() }
    val noteFocusRequester = remember { FocusRequester() }
    var noteFocused by rememberSaveable { mutableStateOf(false) }
    val isDark = isSystemInDarkTheme()
    val scheme = MaterialTheme.colorScheme
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val imeBottomDp = with(density) { WindowInsets.ime.getBottom(this).toDp() }
    val keyboardActive = noteFocused && imeBottomDp > 0.dp
    val tinyComposer = configuration.screenHeightDp < 680
    val compactComposer = configuration.screenHeightDp < 760
    val composerPadding = when {
        keyboardActive -> 8.dp
        tinyComposer -> 10.dp
        compactComposer -> 12.dp
        else -> 16.dp
    }
    val composerGap = when {
        keyboardActive -> 6.dp
        tinyComposer -> 8.dp
        compactComposer -> 10.dp
        else -> 12.dp
    }
    val actionTileHeight = when {
        keyboardActive -> 36.dp
        tinyComposer -> 38.dp
        compactComposer -> 42.dp
        else -> 46.dp
    }
    val actionTilePadding = if (keyboardActive || tinyComposer) 3.dp else 5.dp
    val actionIconSize = if (keyboardActive || tinyComposer) 12.dp else 14.dp
    val actionIconGap = if (keyboardActive || tinyComposer) 1.dp else 2.dp
    val noteEditorHeight = when {
        keyboardActive -> 88.dp
        tinyComposer -> 132.dp
        compactComposer -> 174.dp
        else -> 250.dp
    }
    val titleIconSize = if (keyboardActive) 30.dp else if (tinyComposer) 32.dp else 38.dp

    @Stable
    data class NoteUiPalette(
        val topBarBg: Color,
        val cardBg: Color,
        val cardBgStrong: Color,
        val titleColor: Color,
        val secondaryText: Color,
        val outline: Color,
    )

    val ui = remember(isDark, scheme) {

        val baseCard = if (isDark) {
            scheme.surface
        } else {
            scheme.surfaceContainerHigh
        }

        NoteUiPalette(

            // Header slightly separated from panels
            topBarBg = if (isDark) {
                Color(0xFF232425)
            } else {
                Color(0xFFFEFEFE)
            },

            // One unified panel color
            cardBg = baseCard,
            cardBgStrong = baseCard,

            titleColor = scheme.onSurface,

            secondaryText = scheme.onSurfaceVariant,

            outline = scheme.outline
        )
    }




    // State
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var isCorrectingNote by rememberSaveable { mutableStateOf(false) }
    var correctionStatus by rememberSaveable { mutableStateOf<String?>(null) }
    var wantsReminder by rememberSaveable { mutableStateOf(false) }
    val images = remember { mutableStateListOf<Uri>() }
    val voiceNotes = remember { mutableStateListOf<AddNoteVoiceAttachment>() }
    val fileAttachments = remember { mutableStateListOf<AddNoteFileAttachment>() }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var activeRecordingFile by remember { mutableStateOf<File?>(null) }
    var recordingStartedAtMs by remember { mutableStateOf(0L) }
    var recordingElapsedMs by remember { mutableStateOf(0L) }
    var recordingLevel by remember { mutableStateOf(0f) }
    var isRecordingVoice by rememberSaveable { mutableStateOf(false) }
    var showAllImages by rememberSaveable { mutableStateOf(false) }
    var showAllFiles by rememberSaveable { mutableStateOf(false) }
    var sheetViewMode by rememberSaveable { mutableStateOf(SheetViewMode.Grid) }
    val todoTemplates = remember {
        listOf(
            AddNoteTodoTemplate(
                icon = "📅",
                title = "Planner",
                subtitle = "Time blocks, errands, waiting items, tomorrow carry-over.",
                badge = "Daily",
                body = """
                    Planner
                    
                    Today
                    □ Top priority:
                    □ Time block:
                    □ Calls / messages:
                    □ Errands:
                    
                    Waiting on
                    □ 
                    
                    Move to tomorrow
                    □ 
                """.trimIndent()
            ),
            AddNoteTodoTemplate(
                icon = "💡",
                title = "Idea",
                subtitle = "Capture the problem, why it matters, and the first test.",
                badge = "Create",
                body = """
                    Idea brief
                    
                    □ Problem:
                    □ Who it helps:
                    □ Better solution:
                    □ First test:
                    □ Save for later:
                """.trimIndent()
            ),
            AddNoteTodoTemplate(
                icon = "⭐",
                title = "Priority",
                subtitle = "Separate urgent work from nice-to-have noise.",
                badge = "Focus",
                body = """
                    Priority list
                    
                    Must finish
                    □ 
                    □ 
                    
                    Important next
                    □ 
                    □ 
                    
                    Not today
                    □ 
                """.trimIndent()
            ),
            AddNoteTodoTemplate(
                icon = "🛒",
                title = "Shopping",
                subtitle = "Essentials, optional items, store and budget.",
                badge = "List",
                body = """
                    Shopping list
                    
                    Essentials
                    □ 
                    □ 
                    □ 
                    
                    Optional
                    □ 
                    
                    Before checkout
                    □ Budget:
                    □ Store:
                """.trimIndent()
            ),
            AddNoteTodoTemplate(
                icon = "📍",
                title = "Location",
                subtitle = "A place-based checklist for errands or visits.",
                badge = "Place",
                body = """
                    Location task
                    
                    Place:
                    □ Arrive by:
                    □ Do there:
                    □ Ask / confirm:
                    □ Before leaving:
                """.trimIndent()
            ),
            AddNoteTodoTemplate(
                icon = "⏰",
                title = "Reminder",
                subtitle = "Time, prep, action, and follow-up in one note.",
                badge = "Time",
                body = """
                    Reminder plan
                    
                    When:
                    □ Date:
                    □ Time:
                    □ Prepare before:
                    □ Action:
                    □ Follow up:
                """.trimIndent()
            ),
            AddNoteTodoTemplate(
                icon = "📷",
                title = "Photo",
                subtitle = "Shot list so photos are not random or missing context.",
                badge = "Media",
                body = """
                    Photo checklist
                    
                    □ Main shot:
                    □ Detail shot:
                    □ Wide/context shot:
                    □ Retake if blurry:
                    □ Add caption:
                """.trimIndent()
            ),
            AddNoteTodoTemplate(
                icon = "🎤",
                title = "Voice note",
                subtitle = "Turn a recording into clear actions and details.",
                badge = "Audio",
                body = """
                    Voice note plan
                    
                    □ Record point:
                    □ Key quote:
                    □ Action item:
                    □ Person/place mentioned:
                    □ Convert to note:
                """.trimIndent()
            ),
            AddNoteTodoTemplate(
                icon = "📎",
                title = "Attachment",
                subtitle = "Track why a file or link matters.",
                badge = "File",
                body = """
                    Attachment checklist
                    
                    □ File/link:
                    □ What it proves:
                    □ Related note:
                    □ Share with:
                    □ Save location:
                """.trimIndent()
            ),
            AddNoteTodoTemplate(
                icon = "🏷",
                title = "Tags",
                subtitle = "Make the note easier to find later.",
                badge = "Sort",
                body = """
                    Tags
                    
                    □ Category:
                    □ People:
                    □ Place:
                    □ Status:
                    □ Priority:
                """.trimIndent()
            ),
            AddNoteTodoTemplate(
                icon = "📊",
                title = "Progress",
                subtitle = "Milestones from started to finished.",
                badge = "Track",
                body = """
                    Progress tracker
                    
                    Goal:
                    □ 0% - started:
                    □ 25% - first milestone:
                    □ 50% - halfway:
                    □ 75% - review:
                    □ 100% - done:
                """.trimIndent()
            ),
            AddNoteTodoTemplate(
                icon = "🎯",
                title = "Goal",
                subtitle = "Define the target, deadline, and success measure.",
                badge = "Aim",
                body = """
                    Goal plan
                    
                    Goal:
                    □ Why it matters:
                    □ First action:
                    □ Deadline:
                    □ Success measure:
                    □ Next review:
                """.trimIndent()
            ),
            AddNoteTodoTemplate(
                icon = "🧠",
                title = "Brainstorm",
                subtitle = "Collect options, risks, and the next experiment.",
                badge = "Think",
                body = """
                    Brainstorm board
                    
                    Topic:
                    □ Option:
                    □ Option:
                    □ Risk:
                    □ Best direction:
                    □ Next experiment:
                """.trimIndent()
            ),
            AddNoteTodoTemplate(
                icon = "🚀",
                title = "Project",
                subtitle = "Outcome, scope, blocker, owner, and review.",
                badge = "Build",
                body = """
                    Project checklist
                    
                    Project:
                    □ Outcome:
                    □ Scope:
                    □ Next task:
                    □ Blocker:
                    □ Owner:
                    □ Ship / review:
                """.trimIndent()
            ),
            AddNoteTodoTemplate(
                icon = "🧩",
                title = "Bug fix",
                subtitle = "Reproduce, cause, fix, test, and verify.",
                badge = "Dev",
                body = """
                    Bug fix checklist
                    
                    Issue:
                    □ Steps to reproduce:
                    □ Expected result:
                    □ Actual result:
                    □ Likely cause:
                    □ Fix:
                    □ Test / verify:
                """.trimIndent()
            ),
            AddNoteTodoTemplate(
                icon = "✈",
                title = "Airport shift",
                subtitle = "Arrivals, departures, delays, and follow-up.",
                badge = "Ops",
                body = """
                    Airport shift
                    
                    Today:
                    □ Arrivals to watch:
                    □ Departures to watch:
                    □ Delays:
                    □ Cancellations:
                    □ Calls / follow-up:
                    □ Notes for next shift:
                """.trimIndent()
            ),
            AddNoteTodoTemplate(
                icon = "🗓",
                title = "Meeting",
                subtitle = "Agenda, decisions, owners, and next actions.",
                badge = "Work",
                body = """
                    Meeting notes
                    
                    Topic:
                    □ Agenda:
                    □ Decision:
                    □ Owner:
                    □ Deadline:
                    □ Follow-up message:
                """.trimIndent()
            ),
            AddNoteTodoTemplate(
                icon = "🧳",
                title = "Travel prep",
                subtitle = "Pack, documents, timing, and last checks.",
                badge = "Trip",
                body = """
                    Travel prep
                    
                    Before leaving
                    □ Documents:
                    □ Charger / tech:
                    □ Clothes:
                    □ Ride / parking:
                    □ Arrival plan:
                    
                    Last check
                    □ Keys / wallet:
                    □ Door locked:
                """.trimIndent()
            )
        )
    }

    LaunchedEffect(correctionStatus, isCorrectingNote) {
        if (correctionStatus != null && !isCorrectingNote) {
            kotlinx.coroutines.delay(2400)
            correctionStatus = null
        }
    }

    // Tip/dot logic (same behavior you already had)
    var showHelp by rememberSaveable { mutableStateOf(false) }
    var showNotifDialog by rememberSaveable { mutableStateOf(false) }

// ✅ manual title edit inside the SAME hint box
    var showManualTitle by rememberSaveable { mutableStateOf(false) }
    var titleFocused by rememberSaveable { mutableStateOf(false) }
    var showEmptySavePrompt by rememberSaveable { mutableStateOf(false) }
    val titleFocusRequester = remember { FocusRequester() }

    LaunchedEffect(showManualTitle) {
        if (showManualTitle) titleFocusRequester.requestFocus()
    }
    LaunchedEffect(showEmptySavePrompt) {
        if (showEmptySavePrompt) {
            kotlinx.coroutines.delay(2200)
            showEmptySavePrompt = false
        }
    }
    // ---- AI placeholder (title field) ----
    var aiPlaceholder by rememberSaveable { mutableStateOf<String?>(null) }
    var aiPlaceholderLoading by rememberSaveable { mutableStateOf(false) }
    var lastAiPlaceholderKey by rememberSaveable { mutableStateOf<String?>(null) }
    var titleFromAi by rememberSaveable { mutableStateOf(false) }





    // --- AI tip prefs (connected to Settings) ---
    val prefs = remember { context.getSharedPreferences(NotesPagePrefs.NAME, Context.MODE_PRIVATE) }

    var seenTitleTip by rememberSaveable {
        mutableStateOf(prefs.getBoolean("seen_title_tip", false))
    }


    var tipEnabled by rememberSaveable {
        mutableStateOf(
            prefs.getBoolean(
                NotesPagePrefs.KEY_ENABLE_TITLE_TIPS,
                NotesPagePrefs.DEFAULT_ENABLE_TITLE_TIPS
            )
        )
    }

    val notifPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && NotificationGate.areNotificationsEnabled(context)) {
            wantsReminder = true
        } else {
            // still not allowed (or user denied) -> keep OFF
            wantsReminder = false
            // show your dialog / snackbar here
            showNotifDialog = true
        }
    }
    val audioPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { recorder?.release() }
            recorder = null
        }
    }

    LaunchedEffect(isRecordingVoice) {
        while (isRecordingVoice) {
            val started = recordingStartedAtMs
            recordingElapsedMs = if (started > 0L) SystemClock.elapsedRealtime() - started else 0L
            val amplitude = runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)
            recordingLevel = (amplitude / 32767f).coerceIn(0.08f, 1f)
            kotlinx.coroutines.delay(80)
        }
        recordingLevel = 0f
    }

    fun closeManualTitleOnly() {
        showManualTitle = false
        titleFocused = false
        focusManager.clearFocus(force = true)
        keyboard?.hide()
    }





// red dot logic
    val showDot = tipEnabled && !seenTitleTip && title.isBlank()

    var openExpanded by rememberSaveable { mutableStateOf(false) }

    fun showTitleTip(expanded: Boolean) {
        if (!tipEnabled) return
        seenTitleTip = true
        prefs.edit { putBoolean("seen_title_tip", true) }
        openExpanded = expanded
        showHelp = true
    }


    // Picker
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult

        val existing = images.toSet()

        uris.forEach { uri ->
            // optional: not needed anymore once copied, but harmless
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

            // ✅ copy into app storage
            val file = importImageIntoAppStorage(context, uri) ?: return@forEach

            // ✅ store local file uri (stable even if gallery deletes original)
            val localUri = Uri.fromFile(file)

            if (localUri !in existing) images.add(localUri)
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult

        val existing = fileAttachments.map { it.uri }.toSet()
        uris.forEach { uri ->
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val (name, mime, size) = queryAttachmentMeta(context, uri)
            if (mime?.startsWith("image/", ignoreCase = true) == true) {
                val file = importImageIntoAppStorage(context, uri, subDir = "notes_photos") ?: return@forEach
                val localUri = Uri.fromFile(file)
                if (localUri !in images) images.add(localUri)
                return@forEach
            }

            val file = importImageIntoAppStorage(context, uri, subDir = "notes_files") ?: return@forEach
            val localUri = Uri.fromFile(file)
            if (localUri !in existing) {
                fileAttachments.add(
                    AddNoteFileAttachment(
                        uri = localUri,
                        name = name,
                        mime = mime,
                        sizeBytes = if (size > 0L) size else file.length()
                    )
                )
            }
        }
    }

    fun toggleReminder() {
        if (wantsReminder) {
            wantsReminder = false
            return
        }

        if (NotificationGate.areNotificationsEnabled(context)) {
            wantsReminder = true
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            showNotifDialog = true
        }
    }

    fun focusNoteEditor() {
        showManualTitle = false
        titleFocused = false
        focusManager.clearFocus(force = true)
        closeManualTitleOnly()
        noteFocusRequester.requestFocus()
        keyboard?.show()
    }

    fun appendToNote(block: String) {
        val spacer = when {
            note.isBlank() -> ""
            note.endsWith("\n\n") -> ""
            note.endsWith("\n") -> "\n"
            else -> "\n\n"
        }
        note = (note + spacer + block).take(MAX_NOTE_CHARS)
        focusNoteEditor()
    }

    fun startVoiceRecording() {
        if (isRecordingVoice) return
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            audioPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        focusManager.clearFocus(force = true)
        keyboard?.hide()

        val file = createVoiceNoteFile(context)
        val newRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }

        runCatching {
            newRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            newRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            newRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            newRecorder.setAudioEncodingBitRate(96_000)
            newRecorder.setAudioSamplingRate(44_100)
            newRecorder.setOutputFile(file.absolutePath)
            newRecorder.prepare()
            newRecorder.start()
        }.onSuccess {
            recorder = newRecorder
            activeRecordingFile = file
            recordingStartedAtMs = SystemClock.elapsedRealtime()
            recordingElapsedMs = 0L
            isRecordingVoice = true
        }.onFailure {
            runCatching { newRecorder.release() }
            file.delete()
            isRecordingVoice = false
        }
    }

    fun stopVoiceRecording(cancel: Boolean) {
        val activeRecorder = recorder ?: return
        val file = activeRecordingFile
        val duration = if (recordingStartedAtMs > 0L) {
            SystemClock.elapsedRealtime() - recordingStartedAtMs
        } else {
            recordingElapsedMs
        }

        recorder = null
        activeRecordingFile = null
        recordingStartedAtMs = 0L
        isRecordingVoice = false

        runCatching { activeRecorder.stop() }
        runCatching { activeRecorder.release() }

        if (cancel || file == null || duration < 450L || !file.exists()) {
            file?.delete()
            return
        }

        val attachment = AddNoteVoiceAttachment(
            uri = Uri.fromFile(file),
            durationMs = duration,
            createdAtMs = System.currentTimeMillis()
        )
        voiceNotes.add(attachment)
    }

    fun removeVoiceAttachment(item: AddNoteVoiceAttachment) {
        voiceNotes.remove(item)
        val line = "Voice note - ${formatVoiceDuration(item.durationMs)}"
        note = note
            .lines()
            .filterNot { it.trim() == line }
            .joinToString("\n")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
        if (item.uri.scheme == "file") File(item.uri.path.orEmpty()).delete()
    }

    fun insertTodoBlock() {
        appendToNote("To do\n□ \n□ \n□ ")
    }

    fun insertTodoTemplate(template: AddNoteTodoTemplate) {
        appendToNote(template.body)
    }

    fun insertTimestamp() {
        val stamp = SimpleDateFormat("EEE, MMM d, h:mm a", Locale.getDefault()).format(Date())
        appendToNote(stamp)
    }

    fun cleanedNoteText(raw: String): String =
        raw.lines()
            .map { it.trimEnd() }
            .joinToString("\n")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()

    suspend fun cleanPasteAndCorrectGrammar() {
        if (note.isBlank()) {
            correctionStatus = "Nothing to correct"
            focusNoteEditor()
            return
        }

        isCorrectingNote = true
        correctionStatus = "Correcting spelling, grammar, and style..."
        val original = note
        val cleaned = cleanedNoteText(original)
        if (cleaned != original) note = cleaned
        focusNoteEditor()

        val prompt = buildString {
            appendLine("Polish and correct the user's note.")
            appendLine("Rules:")
            appendLine("- Preserve the original language.")
            appendLine("- Preserve meaning, names, times, airports, flight numbers, code, and lists.")
            appendLine("- Correct spelling, grammar, punctuation, capitalization, sentence structure, and pasted spacing.")
            appendLine("- Improve style and clarity lightly, but keep the user's voice.")
            appendLine("- Do not summarize.")
            appendLine("- Do not translate.")
            appendLine("- Do not add commentary.")
            appendLine("- Return only the corrected and polished note text.")
            appendLine()
            appendLine("NOTE:")
            appendLine(cleaned.take(6000))
        }

        runCatching {
            GeminiClient.generate(prompt, callTimeoutMillis = 12_000)
                .trim()
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
        }.onSuccess { corrected ->
            if (corrected.isBlank()) {
                correctionStatus = "AI returned empty text"
                return@onSuccess
            }

            val finalText = corrected.take(MAX_NOTE_CHARS)
            if (finalText == original) {
                correctionStatus = "Already polished"
            } else {
                note = finalText
                correctionStatus = "Corrected spelling, grammar, and style"
            }
            focusNoteEditor()
        }.onFailure {
            correctionStatus =
                if (cleaned != original) "Cleaned spacing, AI unavailable" else "AI correction unavailable"
            focusNoteEditor()
        }.also {
            isCorrectingNote = false
        }
    }

    val pageBg = LocalAppPageBg.current


    val pageBackdrop = rememberLayerBackdrop {
        drawRect(pageBg)
        drawContent()
    }
    val noteToolsBackdrop = rememberLayerBackdrop()
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    var menuPage by rememberSaveable { mutableStateOf(AddNoteToolMenuPage.Main) }
    var topMenuOpen by rememberSaveable { mutableStateOf(false) }

    fun closeNoteToolsSheet() {
        menuOpen = false
        menuPage = AddNoteToolMenuPage.Main
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // ✅ re-read settings after returning from settings screen
                tipEnabled = prefs.getBoolean(
                    NotesPagePrefs.KEY_ENABLE_TITLE_TIPS,
                    true
                )


                // if you want these live-updated too:
                seenTitleTip = prefs.getBoolean("seen_title_tip", false)
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            // ✅ same “rounded bottom” header style like your profile screen

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(20f),
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                color = ui.topBarBg,
                tonalElevation = 0.dp,
                shadowElevation = 0.1.dp
            ) {
                CenterAlignedTopAppBar(
                    title = {
                        val t = MaterialTheme.typography.titleLarge
                        Text(
                            text = "Add Note",
                            style = t.copy(fontWeight = FontWeight.SemiBold),
                            color = ui.titleColor     // ✅ slightly softer in light mode
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                                contentDescription = "Back",
                                tint = ui.titleColor  // ✅ optional, matches title
                            )
                        }
                    },
                    actions = {
                        val isDark = isSystemInDarkTheme()
                        val canSave = note.isNotBlank()
                        val titleTint = when {
                            aiPlaceholderLoading -> MaterialTheme.colorScheme.onSurfaceVariant
                            aiPlaceholder != null -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        val dynamicColor = if (canSave) titleTint else MaterialTheme.colorScheme.onSurfaceVariant
                        val aiActive = titleFromAi || aiPlaceholder != null

                        val saveShape = RoundedCornerShape(50.dp)
                        val glassColor = bottomTabBarTint()
                        val overlayTint = bottomTabBarOverlayTint()
                        val backdropBlurDp = bottomChromeBackdropBlurDp()
                        var saving by rememberSaveable { mutableStateOf(false) }

                        Box(
                            modifier = Modifier
            .height(42.dp)
            .shadow(0.1.dp, saveShape, clip = false)
            .clip(saveShape)
            .adaptiveLiquidGlassBackdrop(
                backdrop = pageBackdrop,
                shape = saveShape,
                surfaceColor = glassColor,
                blurDp = backdropBlurDp,
                shadow = { bottomChromeShadow() },
                refractionHeightDp = GlassChromeRefractionHeightDp,
                refractionAmountDp = GlassChromeRefractionAmountDp
            )
            .background(overlayTint, saveShape)
                                .then(
                                    if (aiActive && canSave) {
                                        Modifier.background(
                                            MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.16f else 0.10f),
                                            saveShape
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable(
                                    enabled = !saving,
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    if (!canSave) {
                                        showEmptySavePrompt = true
                                        showManualTitle = false
                                        titleFocused = false
                                        focusManager.clearFocus(force = true)
                                        noteFocusRequester.requestFocus()
                                        keyboard?.show()
                                        return@clickable
                                    }
                                    focusManager.clearFocus()
                                    keyboard?.hide()

                                    scope.launch {
                                        saving = true
                                        try {
                                            val finalTitle = when {
                                                title.isNotBlank() -> title
                                                !aiPlaceholder.isNullOrBlank() -> aiPlaceholder!!
                                                tipEnabled -> runCatching {
                                                    GeminiTitles.generateTitles(
                                                        note = note,
                                                        hasImages = images.isNotEmpty(),
                                                        currentTitle = ""
                                                    ).firstOrNull()?.title?.let {
                                                        enforceMeaningfulTitle(
                                                            aiTitle = it,
                                                            note = note,
                                                            hasImages = images.isNotEmpty()
                                                        )
                                                    }.orEmpty()
                                                }.getOrDefault("")
                                                else -> ""
                                            }

                                            if (title.isBlank() && finalTitle.isNotBlank()) {
                                                titleFromAi = true
                                            }

                                            val finalWantsReminder =
                                                wantsReminder && NotificationGate.areNotificationsEnabled(context)

                                            if (wantsReminder && !finalWantsReminder) {
                                                showNotifDialog = true
                                                return@launch
                                            }

                                            onSave(
                                                note,
                                                finalTitle,
                                                images.toList(),
                                                voiceNotes.toList(),
                                                fileAttachments.toList(),
                                                finalWantsReminder
                                            )
                                        } finally {
                                            saving = false
                                        }
                                    }
                                }
                                .padding(horizontal = 18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Save",
                                    color = dynamicColor,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                                )

                                if (saving) {
                                    Spacer(Modifier.width(8.dp))
                                    androidx.compose.material3.CircularProgressIndicator(
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(14.dp),
                                        color = dynamicColor
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.width(8.dp))
                    },

                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .background(pageBg)
        ) {
            val isDark = isSystemInDarkTheme()
            Box(
                Modifier
                    .matchParentSize()
                    .layerBackdrop(noteToolsBackdrop)
            ) {
                Box(
                    Modifier
                        .matchParentSize()
                        .layerBackdrop(pageBackdrop)
                ) {
                    ProfileBackdropImageLayer(
                        modifier = Modifier.matchParentSize(),
                        lightRes = R.drawable.light_grid_pattern,
                        darkRes = R.drawable.dark_grid_pattern,
                        imageAlpha = if (isDark) 1f else 0.8f,
                        scrimDark = 0f,
                        scrimLight = 0f
                    )
                    Box(Modifier.fillMaxSize()) {

                // ✅ Tap outside to close the manual title + return suggestion text
                if (showManualTitle || titleFocused) {
                    Box(
                        Modifier
                            .matchParentSize()
                            .pointerInput(showManualTitle, titleFocused) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)

                                    // If the tap was already consumed (e.g. by the title TextField),
                                    // do nothing — this is the key part.
                                    if (down.isConsumed) return@awaitEachGesture

                                    closeManualTitleOnly()
                                    noteFocusRequester.requestFocus()
                                    keyboard?.show()
                                }
                            }
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(bottom = (if (keyboardActive) imeBottomDp else 0.dp) + 76.dp)
                        .clipToBounds()
                        .padding(
                            horizontal = 14.dp,
                            vertical = if (keyboardActive) 8.dp else 12.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(if (keyboardActive) 8.dp else 12.dp)
                ) {
                    fun placeholderKey(noteText: String, imgCount: Int): String {
                        val t = noteText.trim()
                        // stable key that changes when content meaningfully changes
                        return buildString {
                            append(t.take(350))
                            append("|")
                            append(t.takeLast(350))
                            append("|img=")
                            append(imgCount)
                            append("|len=")
                            append(t.length)
                        }
                    }

                    LaunchedEffect(note, images.size, tipEnabled, title) {

                        if (showManualTitle || titleFocused) return@LaunchedEffect

                    // reset conditions
                        if (!tipEnabled || title.isNotBlank()) {
                            aiPlaceholderLoading = false
                            aiPlaceholder = null
                            lastAiPlaceholderKey = null
                            return@LaunchedEffect
                        }

                        val trimmed = note.trim()
                        if (trimmed.length < AI_PLACEHOLDER_MIN_CHARS) {
                            aiPlaceholderLoading = false
                            aiPlaceholder = null
                            lastAiPlaceholderKey = null
                            return@LaunchedEffect
                        }

                        val key = placeholderKey(trimmed, images.size)

                        // if nothing meaningful changed, skip
                        if (key == lastAiPlaceholderKey) return@LaunchedEffect

                        // show thinking every time we need a new one
                        if (aiPlaceholder == null) aiPlaceholderLoading = true


                        // debounce (this coroutine will be canceled automatically when note changes)
                        kotlinx.coroutines.delay(500)

                        // re-check after debounce (note could have changed)
                        if (!tipEnabled || title.isNotBlank()) {
                            aiPlaceholderLoading = false
                            return@LaunchedEffect
                        }

                        val nowTrimmed = note.trim()
                        if (nowTrimmed.length < AI_PLACEHOLDER_MIN_CHARS) {
                            aiPlaceholderLoading = false
                            aiPlaceholder = null
                            lastAiPlaceholderKey = null
                            return@LaunchedEffect
                        }


                        val nowKey = placeholderKey(nowTrimmed, images.size)
                        if (nowKey != key) {
                            // user typed again during debounce; new run will handle it
                            aiPlaceholderLoading = false
                            return@LaunchedEffect
                        }

                        try {
                            val out = GeminiTitles.generateTitles(
                                note = note,
                                hasImages = images.isNotEmpty(),
                                currentTitle = ""
                            )
                            aiPlaceholder = out.firstOrNull()?.title?.let {
                                enforceMeaningfulTitle(
                                    aiTitle = it,
                                    note = note,
                                    hasImages = images.isNotEmpty(),
                                )
                            }
                            lastAiPlaceholderKey = key
                        } catch (_: Throwable) {
                            aiPlaceholder = null
                            lastAiPlaceholderKey = null
                        } finally {
                            aiPlaceholderLoading = false
                        }
                    }


                    val titleStripText = when {
                        title.isNotBlank() -> title
                        aiPlaceholderLoading -> "Thinking…"
                        aiPlaceholder != null -> aiPlaceholder!!
                        else -> "Smart title"
                    }
                    val titleFieldPlaceholder = when {
                        aiPlaceholderLoading -> "Thinking…"
                        aiPlaceholder != null -> aiPlaceholder!!
                        else -> "Title"
                    }
                    val titleIsSmart = titleFromAi || aiPlaceholder != null || aiPlaceholderLoading
                    val composerColor = if (isDark) Color(0xFF232425) else Color(0xFFFEFEFE)
                    val composerBorder = if (isDark) Color(0xFF333538) else Color(0xFFE3E3E4)
                    val smartTitleColor = if (isDark) Color(0xFF18314F) else Color(0xFFEAF3FF)
                    val smartTitleIdleColor = if (isDark) Color(0xFF252D38) else Color(0xFFF3F7FC)
                    val smartTitleBorder = if (isDark) Color(0xFF426B9E) else Color(0xFFB7D7FF)
                    val noteInputColor = if (isDark) Color(0xFF1B1D20) else Color(0xFFF7F4EF)
                    val noteInputBorder = if (isDark) Color(0xFF3B3E44) else Color(0xFFE6DCD0)
                    val quietTileColor = if (isDark) Color(0xFF202D3B) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f)
                    val recordingAccent = MaterialTheme.colorScheme.error
                    val composerNoteHeight = if (images.isNotEmpty()) {
                        when {
                            tinyComposer -> 92.dp
                            compactComposer -> 118.dp
                            else -> 160.dp
                        }
                    } else {
                        noteEditorHeight
                    }
                    val noteInputTopInset = if (keyboardActive || tinyComposer) 52.dp else 58.dp
                    val voiceInputVisible = voiceNotes.isNotEmpty()
                    val noteInputBottomInset = if (voiceInputVisible) 44.dp else 0.dp

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        color = composerColor,
                        shadowElevation = 0.dp,
                        border = BorderStroke(
                            1.dp,
                            composerBorder
                        )
                    ) {
                        Column(Modifier.padding(composerPadding)) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        showManualTitle = true
                                    },
                                shape = RoundedCornerShape(20.dp),
                                color = if (titleIsSmart) {
                                    smartTitleColor
                                } else {
                                    smartTitleIdleColor
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (titleIsSmart) smartTitleBorder
                                    else smartTitleBorder.copy(alpha = if (isDark) 0.42f else 0.55f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(if (tinyComposer) 10.dp else 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier.size(titleIconSize),
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (showManualTitle) Icons.Filled.Edit else Icons.Rounded.AutoAwesome,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        val titleTextStyle = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isDark) Color(0xFFE7F1FF) else Color(0xFF123B68)
                                        )
                                        if (showManualTitle) {
                                            BasicTextField(
                                                value = title,
                                                onValueChange = {
                                                    title = it
                                                    titleFromAi = false
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .focusRequester(titleFocusRequester)
                                                    .onFocusChanged { titleFocused = it.isFocused },
                                                singleLine = true,
                                                textStyle = titleTextStyle,
                                                keyboardOptions = KeyboardOptions(
                                                    capitalization = KeyboardCapitalization.Sentences,
                                                    autoCorrectEnabled = false
                                                )
                                            ) { innerTextField ->
                                                Box(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    contentAlignment = Alignment.CenterStart
                                                ) {
                                                    if (title.isBlank()) {
                                                        Text(
                                                            text = titleFieldPlaceholder,
                                                            style = titleTextStyle,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    innerTextField()
                                                }
                                            }
                                        } else if (aiPlaceholderLoading) {
                                            ShimmerThinkingTextCompat(
                                                text = "Thinking…",
                                                style = titleTextStyle,
                                            )
                                        } else {
                                            Text(
                                                text = titleStripText,
                                                style = titleTextStyle,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(composerGap))

                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(composerNoteHeight)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(
                                        if (noteFocused) {
                                            if (isDark) Color(0xFF20262E) else Color(0xFFFFF9F1)
                                        } else {
                                            noteInputColor
                                        }
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (noteFocused) {
                                            if (isDark) Color(0xFF7DB4FF) else Color(0xFFD39A61)
                                        } else {
                                            noteInputBorder
                                        },
                                        shape = RoundedCornerShape(24.dp)
                                    )
                                    .padding(2.dp)
                            ) {
                                OutlinedTextField(
                                    value = note,
                                    onValueChange = { new ->
                                        correctionStatus = null
                                        val old = note

                                        if (new.length > MAX_NOTE_CHARS) {
                                            note = new.take(MAX_NOTE_CHARS)
                                            return@OutlinedTextField
                                        }

                                        val jump = new.length - old.length
                                        val looksLikePaste = jump > PASTE_JUMP

                                        if (looksLikePaste && new.startsWith(old)) {
                                            val pasted = new.substring(old.length)
                                            val sb =
                                                StringBuilder(old.length + pasted.length).apply {
                                                    append(old)
                                                }

                                            var i = 0
                                            while (i < pasted.length) {
                                                val end =
                                                    (i + PASTE_CHUNK).coerceAtMost(pasted.length)
                                                sb.append(pasted, i, end)
                                                i = end
                                            }
                                            note = sb.toString()
                                        } else {
                                            note = new
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(top = noteInputTopInset, bottom = noteInputBottomInset)
                                        .blur(if (isRecordingVoice) 4.dp else 0.dp)
                                        .graphicsLayer { alpha = if (isRecordingVoice) 0.50f else 1f }
                                        .focusRequester(noteFocusRequester)
                                        .onFocusChanged { fs ->
                                            noteFocused = fs.isFocused
                                            if (fs.isFocused) {
                                                showManualTitle = false
                                                titleFocused = false
                                            }
                                        }
                                        .clickable(
                                            interactionSource = noteTap,
                                            indication = null
                                        ) {
                                            showManualTitle = false
                                            titleFocused = false
                                            focusManager.clearFocus(force = true)
                                            closeManualTitleOnly()
                                            noteFocusRequester.requestFocus()
                                            keyboard?.show()
                                        },
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Sentences,
                                        autoCorrectEnabled = false
                                    ),
                                    shape = RoundedCornerShape(22.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        focusedPlaceholderColor = if (isDark) Color(0xFFC8C0B6) else Color(0xFF7D6D5F),
                                        unfocusedPlaceholderColor = if (isDark) Color(0xFFC8C0B6) else Color(0xFF7D6D5F),
                                    )
                                )

                                Row(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .fillMaxWidth()
                                        .padding(start = 14.dp, top = 8.dp, end = 8.dp),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(top = 1.dp)
                                    ) {
                                        Text(
                                            text = "Write note",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                                            color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (note.isBlank()) {
                                            Text(
                                                text = "Paste text, insert a checklist, or correct it with AI.",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = if (isDark) Color(0xFFC8C0B6) else Color(0xFF7D6D5F),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .clickable(
                                                enabled = note.isNotBlank(),
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) {
                                                note = ""
                                                correctionStatus = null
                                                showEmptySavePrompt = false
                                                isCorrectingNote = false
                                                noteFocusRequester.requestFocus()
                                                keyboard?.show()
                                            },
                                        shape = RoundedCornerShape(999.dp),
                                        color = if (note.isBlank()) {
                                            if (isDark) Color(0xFF22252B) else Color(0xFFE9E4DE)
                                        } else if (isDark) {
                                            Color(0xFF262A31)
                                        } else {
                                            Color(0xFFF0E7DD)
                                        },
                                        border = BorderStroke(
                                            1.dp,
                                            if (note.isBlank()) {
                                                MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)
                                            } else if (isDark) {
                                                Color(0xFF3C424D)
                                            } else {
                                                Color(0xFFE0D1C0)
                                            }
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${note.length} chars",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )

                                            Box(
                                                Modifier
                                                    .width(1.dp)
                                                    .height(12.dp)
                                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.34f))
                                            )

                                            Text(
                                                text = "Clear",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                                color = if (note.isBlank()) {
                                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f)
                                                } else if (isDark) {
                                                    Color.White
                                                } else {
                                                    Color.Black
                                                },
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }

                                androidx.compose.animation.AnimatedVisibility(
                                    visible = showEmptySavePrompt && note.isBlank(),
                                    enter = fadeIn(animationSpec = tween(durationMillis = 180)) +
                                        scaleIn(animationSpec = tween(durationMillis = 220), initialScale = 0.92f),
                                    exit = fadeOut(animationSpec = tween(durationMillis = 180)) +
                                        scaleOut(animationSpec = tween(durationMillis = 180), targetScale = 0.96f),
                                    modifier = Modifier.align(Alignment.Center)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = if (isDark) Color(0xFF2A2521) else Color(0xFFFFF5E8),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isDark) Color(0xFF6F5C47) else Color(0xFFE4C49E)
                                        ),
                                        shadowElevation = 0.1.dp
                                    ) {
                                        Text(
                                            text = "Write something to save ... :)",
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                            color = if (isDark) Color(0xFFFFD8A8) else Color(0xFF76512B),
                                            maxLines = 1
                                        )
                                    }
                                }

                                androidx.compose.animation.AnimatedVisibility(
                                    visible = isRecordingVoice,
                                    enter = fadeIn(animationSpec = tween(durationMillis = 520)),
                                    exit = fadeOut(animationSpec = tween(durationMillis = 260)),
                                    modifier = Modifier.align(Alignment.Center)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(24.dp),
                                        color = if (isDark) {
                                            Color(0xFF121417).copy(alpha = 0.74f)
                                        } else {
                                            Color.White.copy(alpha = 0.78f)
                                        },
                                        border = BorderStroke(
                                            1.dp,
                                            recordingAccent.copy(alpha = if (isDark) 0.34f else 0.24f)
                                        ),
                                        shadowElevation = 0.dp
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(999.dp),
                                                color = recordingAccent.copy(alpha = if (isDark) 0.18f else 0.12f),
                                                border = BorderStroke(1.dp, recordingAccent.copy(alpha = 0.32f))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(9.dp)
                                                            .clip(CircleShape)
                                                            .background(recordingAccent)
                                                    )
                                                    Text(
                                                        text = formatVoiceDuration(recordingElapsedMs),
                                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                                                        color = recordingAccent,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                            NoteThinWaveLine(
                                                progress = 1f,
                                                active = true,
                                                level = recordingLevel,
                                                modifier = Modifier
                                                    .width(184.dp)
                                                    .height(14.dp),
                                                color = recordingAccent,
                                                trackColor = recordingAccent.copy(alpha = 0.12f),
                                            )
                                        }
                                    }
                                }

                                if (voiceInputVisible) {
                                    AddNoteVoiceInputChip(
                                        voiceNotes = voiceNotes,
                                        isDark = isDark,
                                        onRemove = { item -> removeVoiceAttachment(item) },
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(start = 12.dp, end = 12.dp, bottom = 10.dp)
                                    )
                                }

                                if (isCorrectingNote || correctionStatus != null) {
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(start = 14.dp, end = 14.dp, bottom = 12.dp),
                                        shape = RoundedCornerShape(999.dp),
                                        color = if (isDark) Color(0xFF2A2521) else Color(0xFFFFF5E8),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isDark) Color(0xFF6F5C47) else Color(0xFFE4C49E)
                                        )
                                    ) {
                                        Box(Modifier.padding(horizontal = 12.dp, vertical = 7.dp)) {
                                            if (isCorrectingNote) {
                                                ShimmerThinkingTextCompat(
                                                    text = "Correcting spelling, grammar, and style...",
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                                )
                                            } else {
                                                Text(
                                                    text = correctionStatus.orEmpty(),
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                                    color = if (isDark) Color(0xFFFFD8A8) else Color(0xFF76512B),
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

                    if (fileAttachments.isNotEmpty()) {
                        val sortedFiles = fileAttachments.sortedWith(
                            compareBy<AddNoteFileAttachment> { it.sortBucket() }
                                .thenBy { it.name.lowercase() }
                        )
                        val visibleFiles = if (showAllFiles) sortedFiles else sortedFiles.take(3)
                        val hiddenFiles = (fileAttachments.size - visibleFiles.size).coerceAtLeast(0)
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = composerColor),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Files (${fileAttachments.size})",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                visibleFiles.forEach { item ->
                                    if (item.isAudioFile()) {
                                        NoteAudioMiniPlayer(
                                            uri = item.uri,
                                            title = item.name,
                                            subtitle = "Audio",
                                            modifier = Modifier.fillMaxWidth(),
                                            onRemove = {
                                                fileAttachments.remove(item)
                                                if (item.uri.scheme == "file") File(item.uri.path.orEmpty()).delete()
                                            }
                                        )
                                    } else {
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(36.dp),
                                            shape = RoundedCornerShape(999.dp),
                                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.AttachFile,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    text = item.name,
                                                    modifier = Modifier.weight(1f),
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "Remove",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(999.dp))
                                                        .clickable {
                                                            fileAttachments.remove(item)
                                                            if (item.uri.scheme == "file") File(item.uri.path.orEmpty()).delete()
                                                        }
                                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                if (fileAttachments.size > 3) {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(999.dp))
                                            .clickable { showAllFiles = !showAllFiles },
                                        shape = RoundedCornerShape(999.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                                    ) {
                                        Text(
                                            text = if (showAllFiles) "Show less" else "+$hiddenFiles more",
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ---- Images preview (clean grid) ----
                    if (images.isNotEmpty()) {
                        val maxShow = when {
                            tinyComposer || compactComposer -> 2
                            else -> 4
                        }
                        val extra = (images.size - maxShow).coerceAtLeast(0)
                        val previewGridHeight = when {
                            tinyComposer -> 92.dp
                            compactComposer -> 112.dp
                            configuration.screenHeightDp < 900 -> 214.dp
                            else -> 232.dp
                        }

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = composerColor),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Column(Modifier.padding(10.dp)) {

                                // ✅ header is clickable -> opens "All images"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { showAllImages = true }
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Images (${images.size})",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        text = "View all",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                                    )
                                }

                                Spacer(Modifier.height(8.dp))

                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    state = rememberLazyGridState(),
                                    userScrollEnabled = false, // ✅ let parent scroll handle it
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(previewGridHeight)
                                ) {
                                    itemsIndexed(images.take(maxShow)) { index, uri ->
                                        val shape = RoundedCornerShape(12.dp)

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(1.6f)
                                                .clip(shape)
                                                .border(
                                                    width = 1.dp,
                                                    color = MaterialTheme.colorScheme.outline.copy(
                                                        alpha = 0.25f
                                                    ),
                                                    shape = shape
                                                )
                                        ) {
                                            AsyncImage(
                                                model = uri,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )

                                            // ✅ Remove button (top-right)
                                            FilledTonalIconButton(
                                                onClick = { images.remove(uri) },
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(5.dp)
                                                    .size(22.dp)
                                                    .zIndex(2f),
                                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                                    containerColor = MaterialTheme.colorScheme.surface.copy(
                                                        alpha = 0.70f
                                                    ),
                                                    contentColor = MaterialTheme.colorScheme.onSurface
                                                )
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Close,
                                                    contentDescription = "Remove",
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }

                                            // ✅ "+N" overlay on last tile -> opens sheet
                                            if (extra > 0 && index == maxShow - 1) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(Color.Black.copy(alpha = 0.45f))
                                                        .clickable(
                                                            interactionSource = remember { MutableInteractionSource() },
                                                            indication = null
                                                        ) { showAllImages = true }, // ✅ OPEN
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "+$extra",
                                                        style = MaterialTheme.typography.titleLarge.copy(
                                                            fontWeight = FontWeight.Black
                                                        ),
                                                        color = Color.White
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ---- All Images Bottom Sheet (delete + full list + view toggle) ----
                    if (showAllImages) {
                        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                        ModalBottomSheet(
                            sheetState = sheetState,
                            onDismissRequest = { showAllImages = false },
                            dragHandle = null,                 // ✅ no drag = no fling handoff jump
                            sheetGesturesEnabled = false       // ✅ if available in your version
                        ) {

                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Header
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "All images (${images.size})",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(Modifier.weight(1f))

                                    Text(
                                        text = "Done",
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) { showAllImages = false }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                // ✅ View toggle (Grid / Large / List) — INLINE COLORS (no helper function)
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AssistChip(
                                        onClick = { sheetViewMode = SheetViewMode.Grid },
                                        label = { Text("Grid") },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = if (sheetViewMode == SheetViewMode.Grid)
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                            else
                                                MaterialTheme.colorScheme.surfaceVariant,
                                            labelColor = if (sheetViewMode == SheetViewMode.Grid)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        border = null
                                    )

                                    AssistChip(
                                        onClick = { sheetViewMode = SheetViewMode.Large },
                                        label = { Text("Large") },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = if (sheetViewMode == SheetViewMode.Large)
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                            else
                                                MaterialTheme.colorScheme.surfaceVariant,
                                            labelColor = if (sheetViewMode == SheetViewMode.Large)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        border = null
                                    )

                                    AssistChip(
                                        onClick = { sheetViewMode = SheetViewMode.List },
                                        label = { Text("List") },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = if (sheetViewMode == SheetViewMode.List)
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                            else
                                                MaterialTheme.colorScheme.surfaceVariant,
                                            labelColor = if (sheetViewMode == SheetViewMode.List)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        border = null
                                    )
                                }

                                // Content
                                when (sheetViewMode) {

                                    SheetViewMode.Grid -> {
                                        LazyVerticalGrid(
                                            columns = GridCells.Fixed(3),
                                            verticalArrangement = Arrangement.spacedBy(10.dp),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(420.dp)
                                        ) {
                                            itemsIndexed(images) { _, uri ->
                                                val shape = RoundedCornerShape(14.dp)

                                                Box(
                                                    Modifier
                                                        .aspectRatio(1f)
                                                        .clip(shape)
                                                        .border(
                                                            1.dp,
                                                            MaterialTheme.colorScheme.outline.copy(
                                                                alpha = 0.25f
                                                            ),
                                                            shape
                                                        )
                                                ) {
                                                    AsyncImage(
                                                        model = uri,
                                                        contentDescription = null,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )

                                                    FilledTonalIconButton(
                                                        onClick = { images.remove(uri) },
                                                        modifier = Modifier
                                                            .align(Alignment.TopEnd)
                                                            .padding(6.dp)
                                                            .size(28.dp),
                                                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                                                            containerColor = MaterialTheme.colorScheme.surface.copy(
                                                                alpha = 0.70f
                                                            ),
                                                            contentColor = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Filled.Close,
                                                            contentDescription = "Remove",
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    SheetViewMode.Large -> {
                                        LazyVerticalGrid(
                                            columns = GridCells.Fixed(2),
                                            verticalArrangement = Arrangement.spacedBy(12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(520.dp)
                                        ) {
                                            itemsIndexed(images) { _, uri ->
                                                val shape = RoundedCornerShape(16.dp)

                                                Box(
                                                    Modifier
                                                        .fillMaxWidth()
                                                        .aspectRatio(1.35f)
                                                        .clip(shape)
                                                        .border(
                                                            1.dp,
                                                            MaterialTheme.colorScheme.outline.copy(
                                                                alpha = 0.25f
                                                            ),
                                                            shape
                                                        )
                                                ) {
                                                    AsyncImage(
                                                        model = uri,
                                                        contentDescription = null,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )

                                                    FilledTonalIconButton(
                                                        onClick = { images.remove(uri) },
                                                        modifier = Modifier
                                                            .align(Alignment.TopEnd)
                                                            .padding(8.dp)
                                                            .size(30.dp),
                                                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                                                            containerColor = MaterialTheme.colorScheme.surface.copy(
                                                                alpha = 0.70f
                                                            ),
                                                            contentColor = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Filled.Close,
                                                            contentDescription = "Remove",
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    SheetViewMode.List -> {
                                        androidx.compose.foundation.lazy.LazyColumn(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(520.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            listItemsIndexed(images) { _, uri ->

                                                val shape = RoundedCornerShape(16.dp)

                                                Surface(
                                                    shape = shape,
                                                    color = ui.cardBgStrong,
                                                    shadowElevation = 0.dp,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        Modifier
                                                            .fillMaxWidth()
                                                            .padding(10.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Box(
                                                            Modifier
                                                                .size(74.dp)
                                                                .clip(RoundedCornerShape(14.dp))
                                                                .border(
                                                                    1.dp,
                                                                    MaterialTheme.colorScheme.outline.copy(
                                                                        alpha = 0.22f
                                                                    ),
                                                                    RoundedCornerShape(14.dp)
                                                                )
                                                        ) {
                                                            AsyncImage(
                                                                model = uri,
                                                                contentDescription = null,
                                                                contentScale = ContentScale.Crop,
                                                                modifier = Modifier.fillMaxSize()
                                                            )
                                                        }

                                                        Spacer(Modifier.width(10.dp))

                                                        Column(Modifier.weight(1f)) {
                                                            Text(
                                                                text = "Image",
                                                                style = MaterialTheme.typography.labelLarge,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                            Text(
                                                                text = uri.lastPathSegment
                                                                    ?: uri.toString(),
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                maxLines = 2
                                                            )
                                                        }

                                                        FilledTonalIconButton(
                                                            onClick = { images.remove(uri) },
                                                            modifier = Modifier.size(40.dp),
                                                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                                                containerColor = MaterialTheme.colorScheme.surface.copy(
                                                                    alpha = 0.70f
                                                                ),
                                                                contentColor = MaterialTheme.colorScheme.onSurface
                                                            )
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Filled.Close,
                                                                contentDescription = "Remove"
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                    if (showNotifDialog) {
                        AlertDialog(
                            onDismissRequest = { showNotifDialog = false },
                            title = { Text("Enable notifications") },
                            text = { Text("Reminders need notifications. Turn them on to set a reminder.") },
                            confirmButton = {
                                Text(
                                    "Open settings",
                                    modifier = Modifier
                                        .clickable {
                                            showNotifDialog = false
                                            NotificationGate.openAppNotificationSettings(context)
                                        }
                                        .padding(12.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            },
                            dismissButton = {
                                Text(
                                    "Cancel",
                                    modifier = Modifier
                                        .clickable { showNotifDialog = false }
                                        .padding(12.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }


                    // ---- Help dialog ----
//                if (showHelp) {
//                    AlertDialog(
//                        onDismissRequest = { showHelp = false },
//                        title = { Text("Tip") },
//                        text = { Text("If you write a longer note, consider adding a title so you can find it faster later.") },
//                        confirmButton = {
//                            Button(
//                                onClick = { showHelp = false },
//                                colors = ButtonDefaults.buttonColors()
//                            ) { Text("Got it") }
//                        }
//                    )
//                }

                    Spacer(Modifier.height(8.dp))
                }
                    }
                }

                AddNoteFloatingToolsBar(
                    backdrop = pageBackdrop,
                    isRecording = isRecordingVoice,
                    elapsedMs = recordingElapsedMs,
                    wantsReminder = wantsReminder,
                    fileCount = fileAttachments.size,
                    tipEnabled = tipEnabled,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(bottom = 8.dp)
                        .zIndex(25f),
                    onVoiceStart = { startVoiceRecording() },
                    onVoiceEnd = { canceled -> stopVoiceRecording(canceled) },
                    onReminder = { toggleReminder() },
                    onAttach = { filePicker.launch(arrayOf("*/*")) },
                    onSuggest = {
                        if (tipEnabled) {
                            showTitleTip(expanded = true)
                        } else {
                            context.startActivity(Intent(context, NotesSettingsComposeActivity::class.java))
                        }
                    },
                    onOpenSheet = {
                        menuPage = AddNoteToolMenuPage.Main
                        menuOpen = true
                    }
                )
            }

                val panelColor = bottomTabBarTint()
                val overlayTint = bottomTabBarOverlayTint()
                val panelInner = bottomTabSelectedPillColor()
                val sheetBackdropBlurDp = bottomChromeBackdropBlurDp()
                val panelBorder = if (isDark) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.46f)
                val sheetShape = GlassChromeShape
                val anyBottomSheetOpen = menuOpen || showHelp

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .zIndex(40f)
                ) {
                    AnimatedVisibility(
                        visible = anyBottomSheetOpen,
                        enter = fadeIn(animationSpec = tween(durationMillis = 130)),
                        exit = fadeOut(animationSpec = tween(durationMillis = 160))
                    ) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.Black.copy(alpha = if (isDark) 0.38f else 0.18f))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        if (showHelp) {
                                            showHelp = false
                                            openExpanded = false
                                        } else {
                                            closeNoteToolsSheet()
                                        }
                                    }
                                )
                        )
                    }

                    AnimatedVisibility(
                        visible = menuOpen,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .imePadding(),
                        enter = slideInVertically(
                            animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing),
                            initialOffsetY = { it / 2 }
                        ) + fadeIn(animationSpec = tween(durationMillis = 170)) +
                            scaleIn(
                                animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing),
                                initialScale = 0.94f
                            ),
                        exit = slideOutVertically(
                            animationSpec = tween(durationMillis = 210, easing = FastOutLinearInEasing),
                            targetOffsetY = { it / 3 }
                        ) + fadeOut(animationSpec = tween(durationMillis = 150)) +
                            scaleOut(
                                animationSpec = tween(durationMillis = 210, easing = FastOutLinearInEasing),
                                targetScale = 0.98f
                            )
                    ) {
                        Surface(
                            modifier = Modifier
                                .padding(
                                    start = GlassChromeHorizontalPadding,
                                    end = GlassChromeHorizontalPadding,
                                    bottom = 8.dp
                                )
                                .fillMaxWidth()
                                .shadow(GlassChromeShadowElevation, sheetShape, clip = false)
                                .clip(sheetShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {}
                                )
                                .adaptiveLiquidGlassBackdrop(
                                    backdrop = noteToolsBackdrop,
                                    shape = sheetShape,
                                    surfaceColor = panelColor,
                                    blurDp = sheetBackdropBlurDp,
                                    shadow = null,
                                    refractionHeightDp = GlassChromeRefractionHeightDp,
                                    refractionAmountDp = GlassChromeRefractionAmountDp
                                )
                                .background(overlayTint, sheetShape),
                            shape = sheetShape,
                            color = Color.Transparent,
                            border = BorderStroke(1.dp, panelBorder)
                        ) {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (menuPage == AddNoteToolMenuPage.Main) "Note tools" else "To-do templates",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = scheme.onSurface
                                        )
                                        Text(
                                            text = if (menuPage == AddNoteToolMenuPage.Main) "Fast actions for this draft" else "Pick the checklist you need",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = scheme.onSurfaceVariant.copy(alpha = 0.74f)
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = scheme.primary.copy(alpha = 0.14f),
                                        border = BorderStroke(1.dp, scheme.primary.copy(alpha = 0.22f))
                                    ) {
                                        Text(
                                            text = if (menuPage == AddNoteToolMenuPage.Main) "Done" else "Back",
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(999.dp))
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null
                                                ) {
                                                    if (menuPage == AddNoteToolMenuPage.Main) {
                                                        closeNoteToolsSheet()
                                                    } else {
                                                        menuPage = AddNoteToolMenuPage.Main
                                                    }
                                                }
                                                .padding(horizontal = 12.dp, vertical = 7.dp),
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = scheme.primary
                                        )
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                if (menuPage == AddNoteToolMenuPage.Main) {
                                    AddNoteMenuAction(
                                        iconText = "□",
                                        title = "To-do list",
                                        subtitle = "Choose planner, project, shopping, and more",
                                        accent = scheme.primary,
                                        container = panelInner,
                                        onClick = { menuPage = AddNoteToolMenuPage.TodoTemplates }
                                    )

                                    Spacer(Modifier.height(8.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        AddNoteMenuCompactAction(
                                            modifier = Modifier.weight(1f),
                                            iconText = "Now",
                                            title = "Timestamp",
                                            accent = scheme.tertiary,
                                            container = panelInner,
                                            onClick = {
                                                closeNoteToolsSheet()
                                                insertTimestamp()
                                            }
                                        )

                                        AddNoteMenuCompactAction(
                                            modifier = Modifier.weight(1f),
                                            iconText = "Aa",
                                            title = "Clean + grammar",
                                            accent = scheme.secondary,
                                            container = panelInner,
                                            onClick = {
                                                closeNoteToolsSheet()
                                                scope.launch { cleanPasteAndCorrectGrammar() }
                                            }
                                        )
                                    }

                                    Spacer(Modifier.height(8.dp))

                                    AddNoteMenuAction(
                                        iconText = "AI",
                                        title = "Smart titles",
                                        subtitle = if (tipEnabled) "Open suggestions and title help" else "Turn suggestions on in settings",
                                        accent = scheme.primary,
                                        container = panelInner,
                                        showDot = showDot,
                                        onClick = {
                                            closeNoteToolsSheet()
                                            if (tipEnabled) {
                                                showTitleTip(expanded = false)
                                            } else {
                                                context.startActivity(Intent(context, NotesSettingsComposeActivity::class.java))
                                            }
                                        }
                                    )

                                    Spacer(Modifier.height(8.dp))

                                    AddNoteMenuAction(
                                        iconText = "i",
                                        title = "Notes settings",
                                        subtitle = "Layout, badges, title suggestions",
                                        accent = scheme.onSurfaceVariant,
                                        container = panelInner,
                                        trailing = if (tipEnabled) "On" else "Off",
                                        onClick = {
                                            closeNoteToolsSheet()
                                            context.startActivity(Intent(context, NotesSettingsComposeActivity::class.java))
                                        }
                                    )

                                    Spacer(Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        AddNoteMenuStatusChip(
                                            text = if (images.isEmpty()) "No photos" else "${images.size} photo${if (images.size == 1) "" else "s"}",
                                            active = images.isNotEmpty(),
                                            modifier = Modifier.weight(1f)
                                        )
                                        AddNoteMenuStatusChip(
                                            text = if (wantsReminder) "Reminder on" else "No reminder",
                                            active = wantsReminder,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                } else {
                                    val templateAccents = listOf(
                                        scheme.primary,
                                        scheme.tertiary,
                                        Color(0xFFFFC857),
                                        Color(0xFF7DD3FC),
                                        Color(0xFF8EE3A8),
                                        Color(0xFFFF8A80)
                                    )
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(2),
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(430.dp)
                                    ) {
                                        itemsIndexed(todoTemplates) { index, template ->
                                            AddNoteTodoTemplateCard(
                                                template = template,
                                                accent = templateAccents[index % templateAccents.size],
                                                container = panelInner,
                                                onClick = {
                                                    closeNoteToolsSheet()
                                                    insertTodoTemplate(template)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = showHelp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .imePadding(),
                        enter = slideInVertically(
                            animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing),
                            initialOffsetY = { it / 2 }
                        ) + fadeIn(animationSpec = tween(durationMillis = 170)) +
                            scaleIn(
                                animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing),
                                initialScale = 0.94f
                            ),
                        exit = slideOutVertically(
                            animationSpec = tween(durationMillis = 210, easing = FastOutLinearInEasing),
                            targetOffsetY = { it / 3 }
                        ) + fadeOut(animationSpec = tween(durationMillis = 150)) +
                            scaleOut(
                                animationSpec = tween(durationMillis = 210, easing = FastOutLinearInEasing),
                                targetScale = 0.98f
                            )
                    ) {
                        if (showHelp) {
                            val meta = remember(note, images.size) { addNoteTipMetaFor(note, images.isNotEmpty()) }
                            val suggestions =
                                remember(note, images.size) { addNoteBuildTitleSuggestions(note, images.isNotEmpty()) }
                            var aiLoading by rememberSaveable(showHelp) { mutableStateOf(false) }
                            var aiError by rememberSaveable(showHelp) { mutableStateOf<String?>(null) }
                            var aiSuggestions by rememberSaveable(showHelp) {
                                mutableStateOf<List<TitleSuggestion>>(emptyList())
                            }
                            var disableTips by rememberSaveable { mutableStateOf(false) }
                            var pickedIndex by rememberSaveable { mutableIntStateOf(0) }
                            val shownSuggestions = aiSuggestions
                                .ifEmpty { suggestions }
                                .ifEmpty { listOf(TitleSuggestion("Quick note", "Fallback")) }
                            val visibleSuggestions =
                                if (shownSuggestions.size >= 8) {
                                    shownSuggestions.take(8)
                                } else {
                                    shownSuggestions + suggestions
                                        .filterNot { s -> shownSuggestions.any { it.title == s.title } }
                                        .take(8 - shownSuggestions.size)
                                }

                            fun closeTitleSheet() {
                                showHelp = false
                                openExpanded = false
                            }

                            fun applyOffAndClose() {
                                prefs.edit {
                                    putBoolean(NotesPagePrefs.KEY_ENABLE_TITLE_TIPS, false)
                                    putBoolean("seen_title_tip", true)
                                }
                                tipEnabled = false
                                seenTitleTip = true
                                closeTitleSheet()
                            }

                            fun applyTitleAndClose() {
                                val picked = visibleSuggestions.getOrNull(pickedIndex)
                                    ?: visibleSuggestions.firstOrNull()
                                    ?: TitleSuggestion("Quick note", "Fallback")
                                title = picked.title
                                titleFromAi = true
                                showManualTitle = false
                                titleFocused = false
                                focusManager.clearFocus()
                                closeTitleSheet()
                            }

                            LaunchedEffect(showHelp, note, images.size, tipEnabled) {
                                if (!showHelp) return@LaunchedEffect
                                if (!tipEnabled) return@LaunchedEffect
                                if (aiSuggestions.isNotEmpty() || aiLoading) return@LaunchedEffect

                                aiLoading = true
                                aiError = null
                                try {
                                    val out = GeminiTitles.generateTitles(
                                        note = note,
                                        hasImages = images.isNotEmpty(),
                                        currentTitle = title
                                    )
                                    aiSuggestions = out.map {
                                        TitleSuggestion(
                                            title = enforceMeaningfulTitle(
                                                aiTitle = it.title,
                                                note = note,
                                                hasImages = images.isNotEmpty(),
                                            ),
                                            why = it.why
                                        )
                                    }
                                    pickedIndex = 0
                                } catch (t: Throwable) {
                                    aiError = t.message ?: "AI failed"
                                    aiSuggestions = emptyList()
                                } finally {
                                    aiLoading = false
                                }
                            }

                            Surface(
                                modifier = Modifier
                                    .padding(
                                        start = GlassChromeHorizontalPadding,
                                        end = GlassChromeHorizontalPadding,
                                        bottom = 8.dp
                                    )
                                    .fillMaxWidth()
                                    .shadow(GlassChromeShadowElevation, sheetShape, clip = false)
                                    .clip(sheetShape)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = {}
                                    )
                                    .adaptiveLiquidGlassBackdrop(
                                        backdrop = noteToolsBackdrop,
                                        shape = sheetShape,
                                        surfaceColor = panelColor,
                                        blurDp = sheetBackdropBlurDp,
                                        shadow = null,
                                        refractionHeightDp = GlassChromeRefractionHeightDp,
                                        refractionAmountDp = GlassChromeRefractionAmountDp
                                    )
                                    .background(overlayTint, sheetShape),
                                shape = sheetShape,
                                color = Color.Transparent,
                                border = BorderStroke(1.dp, panelBorder)
                            ) {
                                AddNoteTitleHelperSheetContent(
                                    meta = meta,
                                    currentTitle = title,
                                    disableTips = disableTips,
                                    pickedIndex = pickedIndex.coerceIn(0, visibleSuggestions.lastIndex.coerceAtLeast(0)),
                                    suggestions = visibleSuggestions,
                                    aiLoading = aiLoading,
                                    aiError = aiError,
                                    panelInner = panelInner,
                                    panelBorder = panelBorder,
                                    onPick = { pickedIndex = it },
                                    onToggleDisable = { disableTips = !disableTips },
                                    onDismiss = { closeTitleSheet() },
                                    onPrimary = {
                                        if (disableTips) applyOffAndClose() else applyTitleAndClose()
                                    }
                                )
                            }
                        }
                    }
                }
        }
    }
}

data class CodeSignals(
    val primaryName: String?,           // AddNoteComposeActivity / AddNoteScreen etc
    val composables: List<String>,       // AddNoteScreen, ...
    val keywords: List<String>,          // "reminder", "images grid", ...
    val summaryHint: String              // short purpose hint for the model
)

 fun extractCodeSignals(note: String): CodeSignals {

    fun findAll(regex: Regex): List<String> =
        regex.findAll(note).map { it.groupValues[1] }.distinct().toList()

    val classes = findAll(Regex("""\bclass\s+([A-Z]\w+)"""))
    val objects = findAll(Regex("""\bobject\s+([A-Z]\w+)"""))
    val composables = findAll(Regex("""@Composable\s+fun\s+([A-Z]\w+)""")) +
            findAll(Regex("""\bfun\s+([A-Z]\w+Screen)\b"""))

    val primary = (classes + objects + composables).firstOrNull()

    // Feature hints (tuned for YOUR code)
    val keys = mutableListOf<String>()

    fun hasAny(vararg s: String) = s.any { note.contains(it, ignoreCase = true) }

    if (hasAny("AddNoteComposeActivity", "AddNoteScreen")) keys += "add note screen"
    if (hasAny("ModalBottomSheet", "rememberModalBottomSheetState")) keys += "image bottom sheet"
    if (hasAny("OpenMultipleDocuments", "image/*", "AsyncImage", "LazyVerticalGrid")) keys += "photo picker + image grid"
    if (hasAny("POST_NOTIFICATIONS", "RequestPermission", "NotificationGate", "wantsReminder")) keys += "reminder toggle + notification permission"
    if (hasAny("SplitButtonLayout", "SplitButtonDefaults")) keys += "split save button + menu"
    if (hasAny("MaterialSharedAxis")) keys += "shared axis transitions"
    if (hasAny("layerBackdrop", "rememberLayerBackdrop", "Backdrop")) keys += "glass backdrop layer"
    if (hasAny("OutlinedTextField", "title", "aiPlaceholder")) keys += "AI title placeholder suggestions"

    // Keep it short & high-signal
    val compact = (keys.distinct()).take(6)

    val hint = buildString {
        if (primary != null) append(primary)
        if (compact.isNotEmpty()) {
            if (isNotEmpty()) append(" — ")
            append(compact.joinToString(", "))
        }
    }.ifBlank { primary ?: "Jetpack Compose feature" }

    return CodeSignals(
        primaryName = primary,
        composables = composables.distinct().take(8),
        keywords = compact,
        summaryHint = hint
    )
}



private const val TITLE_MAX_WORDS = 8
private const val AI_PLACEHOLDER_MIN_CHARS = 10


fun enforceMeaningfulTitle(
    aiTitle: String,
    note: String,
    hasImages: Boolean,
): String {
    val maxWords = TITLE_MAX_WORDS
    fun clean(s: String): String = s
        .replace(Regex("""[\r\n\t]+"""), " ")
        .replace(Regex("""\s+"""), " ")
        .trim()

    fun stripWeird(s: String): String =
        s.replace(Regex("""[^\p{L}\p{N}\s'’\-&]"""), "").trim()

    fun words(s: String) = stripWeird(clean(s))
        .split(Regex("""\s+"""))
        .filter { it.isNotBlank() }

    fun capWords(s: String): String {
        val w = words(s)
        val base = when {
            w.isEmpty() -> ""
            w.size <= maxWords -> w.joinToString(" ")
            else -> w.take(maxWords).joinToString(" ")
        }
        return base.trim()
    }

    fun looksGeneric(t: String): Boolean {
        val low = clean(t).lowercase()
        // expand this list whenever you see boring outputs
        val bad = listOf(
            "quick note", "new note", "note", "notes", "reminder", "idea", "thoughts",
            "to do", "todo", "checklist", "stuff", "things", "important", "save this"
        )
        if (low.length < 8) return true
        if (bad.any { low == it }) return true
        if (bad.any { low.startsWith(it) }) return true
        return false
    }

    // 1) Start from AI title, but only if it’s not generic
    val aiClean = clean(aiTitle)
    var base = capWords(aiClean)

    // 2) If generic, derive from note
// 2) If generic, derive fallback
    if (base.isBlank() || looksGeneric(base)) {

        val isCode = looksLikeCode(note)

        if (isCode) {
            // ---- CODE fallback (semantic, not text-based) ----
            val sig = extractCodeSignals(note)
            base = when {
                !sig.primaryName.isNullOrBlank() ->
                    sig.primaryName

                sig.keywords.isNotEmpty() ->
                    sig.keywords.first().replaceFirstChar { it.uppercase() }

                else ->
                    "Compose feature work"
            }

        } else {
            // ---- LIFE fallback (your original logic) ----
            val n = note.trim()

            val link = Regex("""https?://\S+""").find(n)?.value
            val domain = runCatching { link?.toUri()?.host?.removePrefix("www.") }.getOrNull()

            val lines = n.lines().map { it.trim() }.filter { it.isNotBlank() }
            val firstLine = lines.firstOrNull().orEmpty()
            val firstSentence = n.split(Regex("""[.!?\n]""")).firstOrNull().orEmpty()

            val hasChecklist = lines.any {
                it.startsWith("□") || it.startsWith("☐") || it.startsWith("- ") || it.startsWith("• ") || it.matches(Regex("""\d+\.\s+.*"""))
            }

            base = when {
                !domain.isNullOrBlank() -> "Link: ${capWords(domain)}"
                hasImages -> capWords(firstLine).ifBlank { "Photos" }
                hasChecklist -> capWords(firstLine).ifBlank { "To-do list" }
                else -> capWords(firstSentence).ifBlank { capWords(firstLine) }
            }.ifBlank { "Quick note" }
        }
    }


    // 3) Emoji: only if none already present
    val hasEmoji = Regex("""\p{So}""").containsMatchIn(aiClean)
    if (hasEmoji) return base

    val emoji = when {
        base.contains("link", true) -> "🔗"
        base.contains("photo", true) || hasImages -> "🖼️"
        base.contains("buy", true) || base.contains("shop", true) -> "🛒"
        base.contains("flight", true) || base.contains("travel", true) -> "✈️"
        base.contains("meet", true) || base.contains("meeting", true) -> "📅"
        base.contains("code", true) -> "💻"
        base.contains("bill", true) || base.contains("rent", true) || base.contains("pay", true) -> "💳"
        else -> "" // ✅ no sparkle default
    }

    return if (emoji.isNotBlank()) "$base $emoji" else base

}
