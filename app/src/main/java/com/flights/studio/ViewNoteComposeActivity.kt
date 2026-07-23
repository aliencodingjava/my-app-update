@file:Suppress("DEPRECATION")

package com.flights.studio

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import java.util.Locale
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import coil.compose.SubcomposeAsyncImage
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

class ViewNoteComposeActivity : ComponentActivity() {

    companion object {
        const val EXTRA_NOTE = "NOTE"
        const val EXTRA_POSITION = "NOTE_POSITION"
        const val EXTRA_TITLE = "NOTE_TITLE"
        const val EXTRA_UID = "NOTE_UID"

        fun newIntent(
            context: Context,
            uid: String,
            note: String,
            position: Int,
            title: String?
        ): Intent {
            return Intent(context, ViewNoteComposeActivity::class.java).apply {
                putExtra(EXTRA_UID, uid)
                putExtra(EXTRA_NOTE, note)
                putExtra(EXTRA_POSITION, position)
                putExtra(EXTRA_TITLE, title)
            }
        }
    }

    private val editLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) return@registerForActivityResult
            val data = result.data ?: return@registerForActivityResult

            // ✅ Pass-through: AllNotesActivity expects NOTE_POSITION + UPDATED_* etc
            setResult(RESULT_OK, data)
            finish()
            applyTransition()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val note: String = intent.getStringExtra(EXTRA_NOTE)
            ?: intent.getStringExtra("extra_note")
            ?: ""

        val position: Int = intent.getIntExtra(
            EXTRA_POSITION,
            intent.getIntExtra("extra_position", -1)
        )

        val title: String? = intent.getStringExtra(EXTRA_TITLE)
            ?: intent.getStringExtra("extra_title")

        val uid: String? = intent.getStringExtra(EXTRA_UID)
            ?: intent.getStringExtra("NOTE_UID")

        if (note.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_loading_note), Toast.LENGTH_SHORT).show()
            finish()
            applyTransition()
            return
        }

        setContent {

            // 🔁 inverted system bar logic
            val isDark = isSystemInDarkTheme()
            val view = LocalView.current

            SideEffect {
                val window = (view.context as Activity).window
                WindowCompat.getInsetsController(window, view).apply {
                    // 🌞 light theme  → dark icons
                    // 🌙 dark theme   → white icons
                    isAppearanceLightStatusBars = !isDark
                    isAppearanceLightNavigationBars = !isDark
                }
            }


            FlightsTheme {
                ViewNoteScreen(
                    uid = uid,
                    note = note,
                    title = title,
                    onBack = {
                        finish()
                        applyTransition()
                    },
                    onEdit = {
                        if (uid.isNullOrBlank()) return@ViewNoteScreen

                        editLauncher.launch(
                            EditNoteComposeActivity.newIntent(
                                context = this,
                                note = note,
                                title = title,
                                images = NoteMediaStore.getUris(this, uid), // pass images
                                attachments = NoteAttachmentStore.getItems(this, uid).ifEmpty {
                                    NoteAttachmentStore.getItems(this, note)
                                },
                                voiceNotes = NoteVoiceStore.getItems(this, uid).ifEmpty {
                                    NoteVoiceStore.getItems(this, note)
                                },
                                wantsReminder = false,                            // or real value if stored
                                position = position
                            )

                        )
                        applyTransition()
                    },
                    onOpenImages = { urls, startIndex ->
                        startActivity(
                            ViewImageComposeActivity.intent(
                                this,
                                urls = urls,
                                startIndex = startIndex
                            )
                        )
                        applyTransition()
                    }
                )
            }
        }
    }

    private fun applyTransition() {
        overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation)
    }
}

private fun formatVoiceDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(java.util.Locale.US, minutes, seconds)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ViewNoteScreen(
    uid: String?,
    note: String,
    title: String?,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onOpenImages: (urls: List<String>, startIndex: Int) -> Unit,
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val clipboard = LocalClipboardManager.current
    val isDark = isSystemInDarkTheme()
    val uris by produceState(
        initialValue = emptyList(),
        key1 = uid
    ) {
        value = if (uid.isNullOrBlank()) emptyList()
        else withContext(Dispatchers.IO) { NoteMediaStore.getUris(ctx, uid) }
    }
    val voiceItems by produceState(
        initialValue = emptyList(),
        key1 = uid,
        key2 = note
    ) {
        value = withContext(Dispatchers.IO) {
            val key = uid?.takeIf { it.isNotBlank() } ?: note
            NoteVoiceStore.getItems(ctx, key).ifEmpty {
                NoteVoiceStore.getItems(ctx, note)
            }
        }
    }
    val fileItems by produceState(
        initialValue = emptyList(),
        key1 = uid,
        key2 = note
    ) {
        value = withContext(Dispatchers.IO) {
            val key = uid?.takeIf { it.isNotBlank() } ?: note
            NoteAttachmentStore.getItems(ctx, key).ifEmpty {
                NoteAttachmentStore.getItems(ctx, note)
            }
        }
    }


    val canEdit = !uid.isNullOrBlank()
    val mediaCount = uris.size + voiceItems.size + fileItems.size
    var mediaSheetOpen by rememberSaveable { mutableStateOf(false) }

    fun copyNote() {
        clipboard.setText(AnnotatedString(note))
        Toast.makeText(ctx, "Note copied", Toast.LENGTH_SHORT).show()
    }

    fun shareNote() {
        val shareText = buildString {
            val cleanTitle = title?.trim().orEmpty()
            if (cleanTitle.isNotBlank()) {
                append(cleanTitle)
                append("\n\n")
            }
            append(note)
        }
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            title?.takeIf { it.isNotBlank() }?.let {
                putExtra(Intent.EXTRA_SUBJECT, it)
            }
        }
        runCatching {
            ctx.startActivity(Intent.createChooser(sendIntent, "Share note"))
        }
    }

    val pageBg = LocalAppPageBg.current
    val topBarBackdrop = rememberLayerBackdrop()
    val pageBackdrop = rememberLayerBackdrop {
        drawRect(pageBg)
        drawContent()
    }
    val mediaSheetBackdrop = rememberLayerBackdrop()

    Box(
        Modifier
            .fillMaxSize()
            .background(pageBg)
            .imePadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(topBarBackdrop)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(pageBackdrop)
            ) {
                ProfileBackdropImageLayer(
                    modifier = Modifier.matchParentSize(),
                    lightRes = R.drawable.light_grid_pattern,
                    darkRes = R.drawable.dark_grid_pattern,
                    imageAlpha = if (isDark) 1f else 0.72f,
                    scrimDark = 0f,
                    scrimLight = 0f
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(mediaSheetBackdrop)
            ) {
                ProfileBackdropImageLayer(
                    modifier = Modifier.matchParentSize(),
                    lightRes = R.drawable.light_grid_pattern,
                    darkRes = R.drawable.dark_grid_pattern,
                    imageAlpha = if (isDark) 0.95f else 0.70f,
                    scrimDark = 0.12f,
                    scrimLight = 0.03f
                )
                OpenNoteReaderCard(
                    note = note,
                    title = title,
                    isDark = isDark,
                    backdrop = pageBackdrop,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 6.dp, end = 6.dp, top = 104.dp, bottom = 18.dp)
                )
            }

        }

        OpenNoteTopActionBar(
            backdrop = topBarBackdrop,
            noteLength = note.length,
            canEdit = canEdit,
            onBack = onBack,
            onCopy = ::copyNote,
            onShare = ::shareNote,
            onEdit = onEdit,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(50f)
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(
                    start = GlassChromeHorizontalPadding,
                    end = 16.dp,
                    bottom = 30.dp
                )
                .zIndex(80f),
            contentAlignment = Alignment.BottomEnd
        ) {
            OpenNoteFloatingMediaButton(
                visible = mediaCount > 0 && !mediaSheetOpen,
                backdrop = mediaSheetBackdrop,
                onClick = { mediaSheetOpen = true }
            )
        }

        OpenNoteMediaSheet(
            visible = mediaSheetOpen,
            backdrop = mediaSheetBackdrop,
            uris = uris,
            voiceItems = voiceItems,
            fileItems = fileItems,
            onDismiss = { mediaSheetOpen = false },
            onOpenImages = onOpenImages
        )
    }
}

@Composable
private fun OpenNoteFloatingMediaButton(
    visible: Boolean,
    backdrop: Backdrop,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val iconColor = if (isDark) Color.White.copy(alpha = 0.96f) else Color(0xFF123B52)
    val buttonColor = if (isDark) {
        Color(0xFF35BFF5).copy(alpha = 0.42f)
    } else {
        Color(0xFF8EC8F6).copy(alpha = 0.86f)
    }
    val mediaShape = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 0.dp,
        bottomEnd = 18.dp,
        bottomStart = 0.dp
    )
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(animationSpec = tween(durationMillis = 130)) +
            scaleIn(
                initialScale = 0.88f,
                animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
            ),
        exit = fadeOut(animationSpec = tween(durationMillis = 120)) +
            scaleOut(
                targetScale = 0.88f,
                animationSpec = tween(durationMillis = 120, easing = FastOutLinearInEasing)
            )
    ) {
        OpenNoteLiquidSurface(
            onClick = onClick,
            backdrop = backdrop,
            shape = mediaShape,
            surfaceColor = buttonColor,
            height = 38.dp,
            horizontalPadding = 14.dp,
            contentSpacing = 5.dp
        ) {
            Icon(
                imageVector = Icons.Filled.PhotoLibrary,
                contentDescription = stringResource(R.string.search),
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Media",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                color = iconColor,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun OpenNoteLiquidSurface(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(999.dp),
    isInteractive: Boolean = true,
    tint: Color = Color.Unspecified,
    surfaceColor: Color = Color.Unspecified,
    height: Dp = 48.dp,
    horizontalPadding: Dp = 16.dp,
    contentSpacing: Dp = 8.dp,
    blurRadius: Dp = 2.dp,
    lensHeight: Dp = 12.dp,
    lensAmount: Dp = 24.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }
    val interactive = isInteractive && onClick != null

    Row(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    blur(blurRadius.toPx())
                    lens(lensHeight.toPx(), lensAmount.toPx())
                },
                layerBlock = if (interactive) {
                    {
                        val width = size.width
                        val heightPx = size.height
                        val progress = interactiveHighlight.pressProgress
                        val scale = lerp(1f, 1f + 4f.dp.toPx() / size.height, progress)

                        val maxOffset = size.minDimension
                        val initialDerivative = 0.05f
                        val offset = interactiveHighlight.offset
                        translationX = maxOffset * tanh(initialDerivative * offset.x / maxOffset)
                        translationY = maxOffset * tanh(initialDerivative * offset.y / maxOffset)

                        val maxDragScale = 4f.dp.toPx() / size.height
                        val offsetAngle = atan2(offset.y, offset.x)
                        scaleX = scale +
                            maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) *
                            (width / heightPx).fastCoerceAtMost(1f)
                        scaleY = scale +
                            maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) *
                            (heightPx / width).fastCoerceAtMost(1f)
                    }
                } else {
                    null
                },
                onDrawSurface = {
                    if (tint.isSpecified) {
                        drawRect(tint, blendMode = BlendMode.Hue)
                        drawRect(tint.copy(alpha = 0.75f))
                    }
                    if (surfaceColor.isSpecified) {
                        drawRect(surfaceColor)
                    }
                }
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = null,
                        indication = if (interactive) null else LocalIndication.current,
                        role = Role.Button,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
            .then(
                if (interactive) {
                    Modifier
                        .then(interactiveHighlight.modifier)
                        .then(interactiveHighlight.gestureModifier)
                } else {
                    Modifier
                }
            )
            .height(height)
            .padding(horizontal = horizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(contentSpacing, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
private fun OpenNoteTopActionBar(
    backdrop: LayerBackdrop,
    noteLength: Int,
    canEdit: Boolean,
    onBack: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val topBarShape = RoundedCornerShape(0.dp)
    val isDark = isSystemInDarkTheme()
    val contentColor = if (isDark) Color.White else Color(0xFF111111)
    val barColor = topActionBarTint()

    Surface(
        shape = topBarShape,
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { topBarShape },
                shadow = null,
                highlight = null,
                effects = {
                    blur(
                        radius = TopActionBarBlurDp.dp.toPx(),
                        edgeTreatment = TileMode.Mirror
                    )
                },
                onDrawSurface = { drawRect(barColor) }
            )
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .height(64.dp)
                .padding(start = 4.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                    contentDescription = "Back",
                    tint = contentColor
                )
            }
            Text(
                text = "Note",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                color = contentColor,
                maxLines = 1
            )
            OpenNoteActionsPill(
                noteLength = noteLength,
                isDark = isDark,
                canEdit = canEdit,
                backdrop = backdrop,
                onCopy = onCopy,
                onShare = onShare,
                onEdit = onEdit
            )
        }
    }
}

private data class OpenNoteTodoTemplate(
    val title: String,
    val subtitle: String,
    val badge: String,
    val icon: String,
    val sections: List<OpenNoteTodoSection>
)

private data class OpenNoteTodoSection(
    val title: String,
    val items: List<OpenNoteTodoItem>
)

private data class OpenNoteTodoItem(
    val text: String,
    val checked: Boolean = false,
    val checkedAt: String? = null
)

private val OpenNoteCheckedAtRegex = Regex("""^(.*) \[checked (.+)]$""")

private val OpenNoteTodoTemplateHeaders = setOf(
    "planner", "idea brief", "priority list", "shopping list", "location task",
    "reminder plan", "photo checklist", "voice note plan", "attachment checklist", "tags",
    "progress tracker", "goal plan", "brainstorm board",
    "project checklist", "bug fix checklist", "airport shift", "airport shift checklist",
    "meeting notes", "travel prep", "house checklist", "inside home", "outside home",
    "cleaning list", "meal prep", "bills", "auto checklist", "car repair", "road trip",
    "repair checklist", "tools checklist", "maintenance", "yard work", "video shoot",
    "content plan", "health checklist", "hospital list", "medication", "doctor visit",
    "emergency checklist", "fire safety", "air quality"
)

private fun openNoteIsTodoTemplateHeader(line: String): Boolean =
    line.trim().removeSuffix(":").lowercase(Locale.getDefault()) in OpenNoteTodoTemplateHeaders

private fun openNoteTodoTemplateOrNull(title: String?, note: String): OpenNoteTodoTemplate? {
    val lines = note.lines().filter { it.trim().isNotBlank() }
    if (lines.count { openNoteTodoItemFromLine(it) != null } < 2) return null

    val firstLine = lines.firstOrNull()?.trim().orEmpty()
    val displayTitle = title?.takeIf { it.isNotBlank() }
        ?: firstLine.removeSuffix(":").ifBlank { "Checklist" }
    val titleKey = "$displayTitle $firstLine".lowercase(Locale.getDefault())
    val meta = when {
        "priority" in titleKey -> Triple("Separate urgent work from nice-to-have noise.", "Focus", "★")
        "idea" in titleKey -> Triple("Capture the problem, why it matters, and the first test.", "Create", "?")
        "planner" in titleKey || "daily" in titleKey -> Triple("Time blocks, errands, waiting items, tomorrow carry-over.", "Daily", "17")
        "reminder" in titleKey -> Triple("Time, prep, action, and follow-up in one note.", "Time", "!")
        "photo" in titleKey -> Triple("Shot list so photos are not random or missing context.", "Media", "#")
        "voice" in titleKey -> Triple("Turn a recording into clear actions and details.", "Audio", "V")
        "attachment" in titleKey || "file" in titleKey -> Triple("Track why a file or link matters.", "File", "@")
        "house" in titleKey || "home" in titleKey -> Triple("Home tasks grouped into clear next steps.", "Home", "H")
        "cleaning" in titleKey -> Triple("Quick clean, deep clean, laundry, and reset.", "Clean", "C")
        "meal" in titleKey || "shopping" in titleKey || "bills" in titleKey -> Triple("Everyday errands, money, food, and supplies.", "Life", "L")
        "auto" in titleKey || "car" in titleKey || "road trip" in titleKey -> Triple("Vehicle checks, service, route, and driving prep.", "Auto", "A")
        "repair" in titleKey || "tools" in titleKey || "maintenance" in titleKey || "yard" in titleKey -> Triple("Fix, maintain, test, and clean up.", "Fix", "F")
        "video" in titleKey || "content" in titleKey -> Triple("Plan capture, assets, publishing, and follow-up.", "Media", "#")
        "health" in titleKey || "hospital" in titleKey || "doctor" in titleKey || "medication" in titleKey -> Triple("Care steps, questions, medication, and follow-up.", "Care", "+")
        "emergency" in titleKey || "fire" in titleKey || "air quality" in titleKey -> Triple("Safety actions, contacts, protection, and checks.", "Safe", "!")
        else -> Triple("Organized tasks and follow-up items.", "List", "✓")
    }

    val sections = mutableListOf<OpenNoteTodoSection>()
    var currentTitle: String? = null
    val currentItems = mutableListOf<OpenNoteTodoItem>()

    fun flush() {
        val sectionTitle = currentTitle?.takeIf { it.isNotBlank() } ?: return
        sections += OpenNoteTodoSection(sectionTitle, currentItems.toList())
        currentItems.clear()
    }

    lines.drop(1).forEach { line ->
        val item = openNoteTodoItemFromLine(line)
        if (item != null) {
            currentItems += item
        } else {
            flush()
            currentTitle = line.removeSuffix(":").trim()
        }
    }
    flush()

    val fallbackSections = if (sections.isEmpty()) {
        listOf(
            OpenNoteTodoSection(
                title = displayTitle,
                items = lines.mapNotNull { openNoteTodoItemFromLine(it) }
            )
        )
    } else {
        sections
    }

    return OpenNoteTodoTemplate(
        title = displayTitle,
        subtitle = meta.first,
        badge = meta.second,
        icon = meta.third,
        sections = fallbackSections
    )
}

private fun openNoteTodoItemFromLine(line: String): OpenNoteTodoItem? {
    val trimmedStart = line.trimStart()
    val marker = trimmedStart.firstOrNull() ?: return null
    val checked = when (marker) {
        '☑', '✓', '✔', '●', '◉' -> true
        '□', '○' -> false
        else -> return null
    }
    val rawText = trimmedStart.drop(1).removePrefix(" ")
    val checkedAtMatch = OpenNoteCheckedAtRegex.matchEntire(rawText)
    return OpenNoteTodoItem(
        text = (checkedAtMatch?.groupValues?.getOrNull(1) ?: rawText).removeSuffix(":").trim(),
        checked = checked,
        checkedAt = checkedAtMatch?.groupValues?.getOrNull(2)
    )
}

@Composable
private fun OpenNoteStyledTodoTemplate(
    template: OpenNoteTodoTemplate,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val accent = when (template.badge.lowercase(Locale.getDefault())) {
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
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 6.dp, vertical = 10.dp),
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
                    text = template.title.uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${template.badge.uppercase(Locale.getDefault())} · ${template.subtitle}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = scheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        template.sections.forEachIndexed { index, section ->
            val sectionAccent = sectionAccents[index % sectionAccents.size]
            if (index > 0 && openNoteIsTodoTemplateHeader(section.title)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(scheme.outline.copy(alpha = if (isDark) 0.28f else 0.18f))
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
                            text = section.title.uppercase(Locale.getDefault()),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                            color = sectionAccent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    section.items.ifEmpty { listOf(OpenNoteTodoItem("")) }.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .size(18.dp),
                                shape = CircleShape,
                                color = if (item.checked) sectionAccent.copy(alpha = 0.24f) else Color.Transparent,
                                border = BorderStroke(
                                    1.3.dp,
                                    if (item.checked) sectionAccent else scheme.onSurfaceVariant.copy(alpha = if (isDark) 0.68f else 0.42f)
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
                                color = if (item.text.isBlank()) {
                                    scheme.onSurfaceVariant.copy(alpha = 0.58f)
                                } else {
                                    scheme.onSurface.copy(alpha = 0.90f)
                                },
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
private fun OpenNoteReaderCard(
    note: String,
    title: String?,
    isDark: Boolean,
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier
) {
    val glassAmount = rememberLiquidGlassTintAmount()
    val panelShape = RoundedCornerShape(26.dp)
    val panelColor = if (isDark) {
        Color(0xFF111317).copy(alpha = 0.42f + 0.16f * glassAmount)
    } else {
        Color(0xFFE6E2E7).copy(alpha = 0.12f + 0.10f * glassAmount)
    }
    val overlayTint = if (isDark) {
        Color.Black.copy(alpha = 0.10f + 0.08f * glassAmount)
    } else {
        Color.White.copy(alpha = 0.035f + 0.05f * glassAmount)
    }
    val sheenBrush = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = if (isDark) 0.10f else 0.30f),
            Color.Transparent,
            Color.White.copy(alpha = if (isDark) 0.03f else 0.09f)
        )
    )
    val innerSurface = if (isDark) {
        Color.Black.copy(alpha = 0.22f + 0.12f * glassAmount)
    } else {
        Color.White.copy(alpha = 0.07f + 0.08f * glassAmount)
    }
    val panelBorder = if (isDark) {
        Color.White.copy(alpha = 0.18f + 0.08f * glassAmount)
    } else {
        Color.White.copy(alpha = 0.52f + 0.18f * glassAmount)
    }
    val textColor = if (isDark) Color.White.copy(alpha = 0.94f) else Color(0xFF1E1F24)
    val titleTextColor = if (isDark) Color.White.copy(alpha = 0.98f) else Color(0xFF102E56)
    val titleText = title?.takeIf { it.isNotBlank() } ?: "Note"
    val titlePillColor = if (isDark) {
        Color(0xFF342C78).copy(alpha = 0.76f + 0.10f * glassAmount)
    } else {
        Color(0xFF8EC8F6).copy(alpha = 0.82f + 0.10f * glassAmount)
    }
    val titlePillBorder = if (isDark) {
        Color(0xFF7C66FF).copy(alpha = 0.88f)
    } else {
        Color(0xFF3D8DD7).copy(alpha = 0.62f)
    }
    val todoTemplate = remember(title, note) { openNoteTodoTemplateOrNull(title, note) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 650.dp)
            .clip(panelShape)
            .adaptiveLiquidGlassBackdrop(
                backdrop = backdrop,
                shape = panelShape,
                surfaceColor = panelColor,
                blurDp = 1.35f,
                shadow = null,
                highlight = null,
                refractionHeightDp = GlassChromeRefractionHeightDp,
                refractionAmountDp = GlassChromeRefractionAmountDp,
                chromaticAberration = true
            )
            .background(overlayTint, panelShape)
            .background(sheenBrush, panelShape)
            .border(1.dp, panelBorder, panelShape)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            OpenNoteLiquidSurface(
                backdrop = backdrop,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, titlePillBorder, RoundedCornerShape(999.dp)),
                shape = RoundedCornerShape(999.dp),
                isInteractive = false,
                surfaceColor = titlePillColor,
                height = 40.dp,
                horizontalPadding = 13.dp
            ) {
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Black,
                        color = titleTextColor,
                        textAlign = TextAlign.Center
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, panelBorder.copy(alpha = 0.42f), RoundedCornerShape(18.dp))
                    .background(innerSurface, RoundedCornerShape(18.dp))
            ) {
                if (todoTemplate != null) {
                    OpenNoteStyledTodoTemplate(
                        template = todoTemplate,
                        isDark = isDark,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    SelectionContainer {
                        Text(
                            text = note,
                            style = MaterialTheme.typography.bodyLarge,
                            color = textColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, end = 12.dp, top = 12.dp)
                                .verticalScroll(rememberScrollState())
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OpenNoteMediaSheet(
    visible: Boolean,
    backdrop: LayerBackdrop,
    uris: List<Uri>,
    voiceItems: List<NoteVoiceItem>,
    fileItems: List<NoteAttachmentItem>,
    onDismiss: () -> Unit,
    onOpenImages: (urls: List<String>, startIndex: Int) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val panelColor = if (isDark) {
        Color(0xFF176B8A).copy(alpha = 0.58f)
    } else {
        Color(0xFF8EC8F6).copy(alpha = 0.56f)
    }
    val mediaCount = uris.size + voiceItems.size + fileItems.size

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(40f)
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(durationMillis = 130)),
            exit = fadeOut(animationSpec = tween(durationMillis = 160))
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
                .align(Alignment.BottomCenter)
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
            Box(
                modifier = Modifier
                    .padding(
                        start = 6.dp,
                        end = 6.dp,
                        bottom = 18.dp
                    )
                    .fillMaxWidth()
                    .heightIn(min = 360.dp, max = 520.dp)
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
                        .padding(horizontal = 14.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Media",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "Done",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onDismiss
                                )
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }

                    if (mediaCount == 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No files here",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 300.dp, max = 390.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 6.dp)
                        ) {
                            if (uris.isNotEmpty()) {
                                item {
                                    NoteMediaCategoryRow(title = "Images", count = uris.size) {
                                        itemsIndexed(uris) { index, uri ->
                                            NoteImageThumb(
                                                uri = uri,
                                                onClick = {
                                                    onOpenImages(uris.map { it.toString() }, index)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            if (voiceItems.isNotEmpty() || fileItems.isNotEmpty()) {
                                item {
                                    NoteMediaBox(
                                        voiceItems = voiceItems,
                                        fileItems = fileItems
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
private fun NoteImageThumb(
    uri: Uri,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
        shadowElevation = 0.dp,
        modifier = Modifier
            .width(128.dp)
            .aspectRatio(1.2f)
            .clickable(onClick = onClick)
    ) {
        SubcomposeAsyncImage(
            model = uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            loading = {
                ImagePlaceholderSurface(
                    modifier = Modifier.fillMaxSize(),
                    shape = shape,
                    iconSize = 24,
                    alpha = 0.55f
                )
            },
            error = {
                ImagePlaceholderSurface(
                    modifier = Modifier.fillMaxSize(),
                    shape = shape,
                    iconSize = 24,
                    alpha = 0.55f
                )
            }
        )
    }
}

@Composable
private fun OpenNoteActionsPill(
    noteLength: Int,
    isDark: Boolean,
    canEdit: Boolean,
    backdrop: Backdrop,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit
) {
    val shape = RoundedCornerShape(999.dp)
    val glassAmount = rememberLiquidGlassTintAmount()
    val pillColor = if (isDark) {
        Color.White.copy(alpha = 0.09f + 0.05f * glassAmount)
    } else {
        Color.White.copy(alpha = 0.48f + 0.16f * glassAmount)
    }
    OpenNoteLiquidSurface(
        backdrop = backdrop,
        modifier = Modifier,
        shape = shape,
        isInteractive = false,
        surfaceColor = pillColor,
        height = 34.dp,
        horizontalPadding = 9.dp,
        contentSpacing = 6.dp
    ) {
        Text(
            text = "$noteLength chars",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )

        OpenNoteActionDivider()
        OpenNotePillAction(label = "Copy", isDark = isDark, onClick = onCopy)
        OpenNoteActionDivider()
        OpenNotePillAction(label = "Share", isDark = isDark, onClick = onShare)
        OpenNoteActionDivider()
        OpenNotePillAction(label = "Edit", isDark = isDark, enabled = canEdit, accent = true, onClick = onEdit)
    }
}

@Composable
private fun OpenNoteActionDivider() {
    Box(
        Modifier
            .width(1.dp)
            .height(12.dp)
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.34f))
    )
}

@Composable
private fun OpenNotePillAction(
    label: String,
    isDark: Boolean,
    enabled: Boolean = true,
    accent: Boolean = false,
    onClick: () -> Unit
) {
    val accentColor = if (isDark) Color(0xFF7DD3FC) else Color(0xFF1268B3)
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
        color = if (enabled) {
            if (accent) accentColor else if (isDark) Color.White else Color.Black
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.48f)
        },
        maxLines = 1,
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick)
    )
}

@Composable
private fun NoteVoiceCard(index: Int, item: NoteVoiceItem) {
    NoteAudioMiniPlayer(
        uri = item.asUri,
        title = "Voice ${index + 1}",
        subtitle = formatVoiceDuration(item.durationMs),
        modifier = Modifier.width(190.dp)
    )
}

@Composable
private fun NoteMediaBox(
    voiceItems: List<NoteVoiceItem>,
    fileItems: List<NoteAttachmentItem>
) {
    val documents = remember(fileItems) {
        fileItems
            .filterNot { it.isAudioAttachment() || it.isVideoAttachment() }
            .sortedBy { it.name.lowercase() }
    }
    val audio = remember(fileItems) {
        fileItems
            .filter { it.isAudioAttachment() }
            .sortedBy { it.name.lowercase() }
    }
    val video = remember(fileItems) {
        fileItems
            .filter { it.isVideoAttachment() }
            .sortedBy { it.name.lowercase() }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)
        ),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (voiceItems.isNotEmpty()) {
                NoteMediaCategoryRow(title = "Voice", count = voiceItems.size) {
                    itemsIndexed(voiceItems) { index, item ->
                        NoteVoiceCard(index = index, item = item)
                    }
                }
            }
            if (documents.isNotEmpty()) {
                NoteMediaCategoryRow(title = "Files", count = documents.size) {
                    itemsIndexed(documents) { _, item ->
                        NoteAttachmentPill(item, label = "File")
                    }
                }
            }
            if (audio.isNotEmpty()) {
                NoteMediaCategoryRow(title = "Audio", count = audio.size) {
                    itemsIndexed(audio) { _, item ->
                        NoteAudioMiniPlayer(
                            uri = item.asUri,
                            title = item.name,
                            subtitle = "Audio",
                            modifier = Modifier.width(190.dp)
                        )
                    }
                }
            }
            if (video.isNotEmpty()) {
                NoteMediaCategoryRow(title = "Video", count = video.size) {
                    itemsIndexed(video) { _, item ->
                        NoteAttachmentPill(item, label = "Video")
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteMediaCategoryRow(
    title: String,
    count: Int,
    content: LazyListScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "$title ($count)",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 0.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

@Composable
private fun NoteAttachmentPill(
    item: NoteAttachmentItem,
    label: String
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.11f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f)
        ),
        modifier = Modifier
            .height(40.dp)
            .width(190.dp)
            .clickable {
                val uri = item.asUri
                val openUri = if (uri.scheme == "file") {
                    FileProvider.getUriForFile(
                        ctx,
                        "${ctx.packageName}.fileprovider",
                        File(uri.path.orEmpty())
                    )
                } else {
                    uri
                }
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(openUri, item.mime ?: "*/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                runCatching { ctx.startActivity(Intent.createChooser(intent, "Open attachment")) }
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.secondary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.mime ?: "Attachment",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "Open",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
