@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.flights.studio

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh


data class InstagramPreviewActions(
    val onShare: (Uri) -> Unit,
    val onShareWithNote: (Uri) -> Unit,
    val onRotate: ((Uri) -> Unit)? = null,
    val onReplace: ((Uri) -> Unit)? = null,
    val onRemove: ((Uri) -> Unit)? = null,
)

private fun formatVoiceDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(java.util.Locale.US, minutes, seconds)
}

private fun stripLegacyVoiceMarkers(note: String, hasVoiceNotes: Boolean): String {
    if (!hasVoiceNotes) return note
    return note
        .lines()
        .filterNot { it.trim().matches(Regex("""Voice note - \d+:\d{2}""")) }
        .joinToString("\n")
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()
}

@Composable
fun rememberInstagramPreviewActions(
    context: Context,
    noteProvider: () -> String,
    onRotate: ((Uri) -> Unit)? = null,
    onReplace: ((Uri) -> Unit)? = null,
    onRemove: ((Uri) -> Unit)? = null,
): InstagramPreviewActions {
    // no heavy remember needed; these lambdas are cheap
    return InstagramPreviewActions(
        onShare = { uri -> shareImageUri(context, uri) },
        onShareWithNote = { uri -> shareImageWithNote(context, uri, noteProvider()) },
        onRotate = onRotate,
        onReplace = onReplace,
        onRemove = onRemove
    )
}

@Composable
fun EditNoteScreen(
    initialTitle: String,
    initialNote: String,
    initialImages: List<Uri>,
    initialAttachments: List<NoteAttachmentItem> = emptyList(),
    initialVoiceNotes: List<NoteVoiceItem> = emptyList(),
    initialWantsReminder: Boolean,
    onBack: () -> Unit,
    onSave: (note: String, title: String, images: List<Uri>, attachments: List<NoteAttachmentItem>, voiceNotes: List<NoteVoiceItem>, wantsReminder: Boolean) -> Unit
) {

    val pageBg = LocalAppPageBg.current
    val pageBackdrop = rememberLayerBackdrop {
        drawRect(pageBg)
        drawContent()
    }


    val context = LocalContext.current
    val topBarBackdrop = rememberLayerBackdrop()
    val mediaSheetBackdrop = rememberLayerBackdrop()

    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    var previewUri by remember { mutableStateOf<Uri?>(null) }
    val showInstagramDialog = previewUri != null


    // state
    var title by rememberSaveable { mutableStateOf(initialTitle) }
    var note by rememberSaveable {
        mutableStateOf(stripLegacyVoiceMarkers(initialNote, initialVoiceNotes.isNotEmpty()))
    }
    var wantsReminder by rememberSaveable { mutableStateOf(initialWantsReminder) }

    val images = remember { mutableStateListOf<Uri>() }
    val attachments = remember { mutableStateListOf<NoteAttachmentItem>() }
    val voiceNotes = remember { mutableStateListOf<NoteVoiceItem>() }
    LaunchedEffect(Unit) {
        images.clear()
        initialImages.forEach { uri ->
            val finalUri =
                if (uri.scheme == "content") {
                    val f = importImageIntoAppStorage(context, uri)
                    f?.let { Uri.fromFile(it) } ?: uri
                } else uri

            images.add(finalUri)
        }
        attachments.clear()
        attachments.addAll(initialAttachments)
        voiceNotes.clear()
        voiceNotes.addAll(initialVoiceNotes)
    }


    var launchPicker by remember { mutableStateOf(false) }
    var showHelp by rememberSaveable { mutableStateOf(false) }
    var mediaSheetOpen by rememberSaveable { mutableStateOf(false) }
    var requestAutofillTitle by rememberSaveable { mutableStateOf(false) }
    var saving by rememberSaveable { mutableStateOf(false) }

    val prefs = remember { context.getSharedPreferences(NotesPagePrefs.NAME, Context.MODE_PRIVATE) }
    var tipEnabled by rememberSaveable {
        mutableStateOf(
            prefs.getBoolean(
                NotesPagePrefs.KEY_ENABLE_TITLE_TIPS,
                NotesPagePrefs.DEFAULT_ENABLE_TITLE_TIPS
            )
        )
    }
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

    var replaceTargetUri by remember { mutableStateOf<Uri?>(null) }

    val replacePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { picked: Uri? ->
        val target = replaceTargetUri
        replaceTargetUri = null
        if (picked == null || target == null) return@rememberLauncherForActivityResult

        runCatching {
            context.contentResolver.takePersistableUriPermission(
                picked,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

        val file = importImageIntoAppStorage(context, picked) ?: return@rememberLauncherForActivityResult
        val newLocalUri = Uri.fromFile(file)

        val idx = images.indexOf(target)
        if (idx >= 0) images[idx] = newLocalUri else images.add(newLocalUri)
        previewUri = newLocalUri

    }

    fun copyEditedNote() {
        context.getSystemService(ClipboardManager::class.java)
            ?.setPrimaryClip(ClipData.newPlainText("note", note))
        Toast.makeText(context, "Note copied", Toast.LENGTH_SHORT).show()
    }

    fun shareEditedNote() {
        val shareText = buildString {
            val cleanTitle = title.trim()
            if (cleanTitle.isNotBlank()) {
                append(cleanTitle)
                append("\n\n")
            }
            append(note)
        }
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            title.trim().takeIf { it.isNotBlank() }?.let {
                putExtra(Intent.EXTRA_SUBJECT, it)
            }
        }
        runCatching {
            context.startActivity(Intent.createChooser(sendIntent, "Share note"))
        }
    }


    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult

        val existing = images.toSet()

        uris.forEach { uri ->
            // optional: harmless
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

            // ✅ copy into app storage (same as AddNote)
            val file = importImageIntoAppStorage(context, uri) ?: return@forEach
            val localUri = Uri.fromFile(file) // ✅ file:// saved

            if (localUri !in existing) images.add(localUri)
        }
    }

    LaunchedEffect(launchPicker) {
        if (!launchPicker) return@LaunchedEffect
        launchPicker = false
        picker.launch(arrayOf("image/*"))
    }

    // Only react to explicit request
    LaunchedEffect(showHelp, requestAutofillTitle) {
        if (!showHelp) return@LaunchedEffect
        if (!requestAutofillTitle) return@LaunchedEffect

        requestAutofillTitle = false

        if (tipEnabled && title.isBlank() && note.length >= 20) {
            val ai = runCatching {
                GeminiTitles.generateTitles(
                    note = note,
                    hasImages = images.isNotEmpty(),
                    currentTitle = ""
                ).firstOrNull()?.title.orEmpty()
            }.getOrDefault("")

            if (ai.isNotBlank()) {
                title = enforceMeaningfulTitle(ai, note, images.isNotEmpty())
            }
        }
    }



    val previewActions = rememberInstagramPreviewActions(
        context = context,
        noteProvider = { note },
        onRotate = { uri ->
            scope.launch {
                val out = rotate90AndSaveNewFile(context, uri) ?: return@launch
                val newUri = Uri.fromFile(out)

                val idx = images.indexOf(uri)
                if (idx >= 0) images[idx] = newUri else images.add(newUri)
                previewUri = newUri
            }
        },
        onReplace = { uri ->
            replaceTargetUri = uri
            replacePicker.launch(arrayOf("image/*"))
        },
        onRemove = { uri ->
            images.remove(uri)
            previewUri = null
        }
    )

    val canSave = note.isNotBlank()

    fun saveEditedNote() {
        if (!canSave || saving) return
        focusManager.clearFocus()
        keyboard?.hide()

        scope.launch {
            saving = true
            try {
                val finalTitle =
                    if (title.isNotBlank() || !tipEnabled) {
                        title
                    } else {
                        runCatching {
                            GeminiTitles.generateTitles(
                                note = note,
                                hasImages = images.isNotEmpty(),
                                currentTitle = title
                            ).firstOrNull()?.title?.let {
                                enforceMeaningfulTitle(it, note, images.isNotEmpty())
                            }.orEmpty()
                        }.getOrDefault("")
                    }

                onSave(
                    note,
                    finalTitle,
                    images.toList(),
                    attachments.toList(),
                    voiceNotes.toList(),
                    wantsReminder
                )
            } finally {
                saving = false
            }
        }
    }

    val mediaCount = images.size + voiceNotes.size + attachments.size

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
                    imageAlpha = if (isDark) 1f else 0.8f,
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

                Box(
                    Modifier
                        .fillMaxSize()
                        .blur(if (showInstagramDialog) 8.dp else 0.dp)
                        .background(
                            if (showInstagramDialog) {
                                Color.Black.copy(alpha = 0.20f)
                            } else {
                                Color.Transparent
                            }
                        )
                        .padding(start = 6.dp, end = 6.dp, top = 104.dp, bottom = 18.dp)
                ) {
                    EditNoteReaderCard(
                        title = title,
                        note = note,
                        isDark = isDark,
                        backdrop = pageBackdrop,
                        onTitleChange = { title = it },
                        onNoteChange = { note = it },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        EditNoteTopActionBar(
            backdrop = topBarBackdrop,
            noteLength = note.length,
            canSave = canSave,
            saving = saving,
            onBack = onBack,
            onShare = ::shareEditedNote,
            onCopy = ::copyEditedNote,
            onSave = ::saveEditedNote,
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
            EditNoteFloatingMediaButton(
                visible = mediaCount > 0 && !mediaSheetOpen,
                backdrop = mediaSheetBackdrop,
                onClick = { mediaSheetOpen = true }
            )
        }

        EditNoteMediaSheet(
            visible = mediaSheetOpen,
            backdrop = mediaSheetBackdrop,
            images = images,
            voiceNotes = voiceNotes,
            attachments = attachments,
            onAddPhoto = { launchPicker = true },
            onDismiss = { mediaSheetOpen = false },
            onPreviewImage = { previewUri = it }
        )

        InstagramPreviewOverlay(
            uri = previewUri,
            onDismiss = { previewUri = null },
            onShare = previewActions.onShare,
            onShareWithNote = previewActions.onShareWithNote,
            onRotate = previewActions.onRotate,
            onReplace = previewActions.onReplace,
            onRemove = previewActions.onRemove
        )

        if (showHelp) {
            AlertDialog(
                onDismissRequest = { showHelp = false },
                title = { Text("Tips") },
                text = {
                    Text(
                        buildString {
                            appendLine("• Keep your title short and clear.")
                            appendLine("• Use the note area for full details.")
                            appendLine("• Tap and hold an image to preview it.")
                            appendLine("• Drag handle lets you reorder precisely.")
                            appendLine("• You can remove images anytime.")
                            appendLine("• Notes are saved locally and synced automatically.")
                            appendLine("• Your layout adapts to screen size.")
                            if (tipEnabled) {
                                appendLine("• If the title is empty, Save can auto AI-generate one.")
                            }
                        }
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showHelp = false }) { Text("OK") }
                }
            )

        }
    }
}



@Composable
private fun EditNoteTopActionBar(
    backdrop: Backdrop,
    noteLength: Int,
    canSave: Boolean,
    saving: Boolean,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onCopy: () -> Unit,
    onSave: () -> Unit,
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
                .padding(start = 4.dp, end = 8.dp),
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
                text = "Edit Note",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                color = contentColor,
                maxLines = 1
            )
            EditNoteActionsPill(
                noteLength = noteLength,
                isDark = isDark,
                backdrop = backdrop,
                canSave = canSave,
                saving = saving,
                onShare = onShare,
                onCopy = onCopy,
                onSave = onSave
            )
        }
    }
}

@Composable
private fun EditNoteReaderCard(
    title: String,
    note: String,
    isDark: Boolean,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    onTitleChange: (String) -> Unit,
    onNoteChange: (String) -> Unit
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
    val sheenBrush = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = if (isDark) 0.10f else 0.30f),
            Color.Transparent,
            Color.White.copy(alpha = if (isDark) 0.03f else 0.09f)
        )
    )
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
            EditNoteLiquidSurface(
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
                BasicTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Black,
                        color = titleTextColor,
                        textAlign = TextAlign.Center
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        autoCorrectEnabled = false
                    ),
                    decorationBox = { innerTextField ->
                        if (title.isBlank()) {
                            Text(
                                text = "Title",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = titleTextColor.copy(alpha = 0.62f),
                                    textAlign = TextAlign.Center
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        innerTextField()
                    }
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
                BasicTextField(
                    value = note,
                    onValueChange = onNoteChange,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 12.dp, end = 12.dp, top = 12.dp)
                        .verticalScroll(rememberScrollState()),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = textColor),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        autoCorrectEnabled = false
                    ),
                    decorationBox = { innerTextField ->
                        if (note.isBlank()) {
                            Text(
                                text = "Write your note",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.58f)
                            )
                        }
                        innerTextField()
                    }
                )
            }
        }
    }
}

@Composable
private fun EditNoteActionsPill(
    noteLength: Int,
    isDark: Boolean,
    backdrop: Backdrop,
    canSave: Boolean = true,
    saving: Boolean = false,
    onShare: () -> Unit,
    onCopy: () -> Unit,
    onSave: () -> Unit
) {
    val shape = RoundedCornerShape(999.dp)
    val glassAmount = rememberLiquidGlassTintAmount()
    val pillColor = if (isDark) {
        Color.White.copy(alpha = 0.09f + 0.05f * glassAmount)
    } else {
        Color.White.copy(alpha = 0.48f + 0.16f * glassAmount)
    }
    EditNoteLiquidSurface(
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
        EditNoteActionDivider()
        EditNotePillAction(label = "Share", isDark = isDark, onClick = onShare)
        EditNoteActionDivider()
        EditNotePillAction(label = "Copy", isDark = isDark, onClick = onCopy)
        EditNoteActionDivider()
        EditNotePillAction(
            label = if (saving) "Saving" else "Save",
            isDark = isDark,
            enabled = canSave && !saving,
            accent = true,
            onClick = onSave
        )
    }
}

@Composable
private fun EditNoteActionDivider() {
    Box(
        Modifier
            .width(1.dp)
            .height(12.dp)
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.34f))
    )
}

@Composable
private fun EditNotePillAction(
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
        modifier = Modifier.clickable(
            enabled = enabled,
            onClick = onClick
        )
    )
}

@Composable
private fun EditNoteFloatingMediaButton(
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
        EditNoteLiquidSurface(
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
private fun EditNoteLiquidSurface(
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
private fun EditNoteMediaSheet(
    visible: Boolean,
    backdrop: Backdrop,
    images: MutableList<Uri>,
    voiceNotes: MutableList<NoteVoiceItem>,
    attachments: MutableList<NoteAttachmentItem>,
    onAddPhoto: () -> Unit,
    onDismiss: () -> Unit,
    onPreviewImage: (Uri) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val panelColor = if (isDark) {
        Color(0xFF176B8A).copy(alpha = 0.58f)
    } else {
        Color(0xFF8EC8F6).copy(alpha = 0.56f)
    }
    val mediaCount = images.size + voiceNotes.size + attachments.size

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
                    .padding(start = 6.dp, end = 6.dp, bottom = 18.dp)
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
                            text = "Add photo",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .clickable(onClick = onAddPhoto)
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                        Text(
                            text = "Done",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .clickable(onClick = onDismiss)
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
                        androidx.compose.foundation.lazy.LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 300.dp, max = 390.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 6.dp)
                        ) {
                            if (images.isNotEmpty()) {
                                item {
                                    EditNoteMediaCategoryColumn(title = "Images", count = images.size) {
                                        ReorderableImageRow(
                                            images = images,
                                            onRemove = { uri -> images.remove(uri) },
                                            onOpenPreview = onPreviewImage,
                                            itemSize = 92.dp
                                        )
                                    }
                                }
                            }
                            if (voiceNotes.isNotEmpty()) {
                                item {
                                    EditNoteMediaCategoryRow(title = "Voice", count = voiceNotes.size) {
                                        itemsIndexed(voiceNotes) { index, item ->
                                            NoteAudioMiniPlayer(
                                                uri = item.asUri,
                                                title = "Voice ${index + 1}",
                                                subtitle = formatVoiceDuration(item.durationMs),
                                                modifier = Modifier.fillMaxWidth(),
                                                onRemove = {
                                                    voiceNotes.remove(item)
                                                    if (item.asUri.scheme == "file") {
                                                        File(item.asUri.path.orEmpty()).delete()
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            if (attachments.isNotEmpty()) {
                                item {
                                    val sortedAttachments = attachments.sortedWith(
                                        compareBy<NoteAttachmentItem> {
                                            when {
                                                it.isAudioAttachment() -> 1
                                                it.isVideoAttachment() -> 2
                                                else -> 0
                                            }
                                        }.thenBy { it.name.lowercase() }
                                    )
                                    EditNoteMediaCategoryRow(title = "Files", count = attachments.size) {
                                        itemsIndexed(sortedAttachments) { _, item ->
                                            EditableAttachmentPill(
                                                item = item,
                                                modifier = Modifier.fillMaxWidth(),
                                                onRemove = {
                                                    attachments.remove(item)
                                                    if (item.asUri.scheme == "file") {
                                                        File(item.asUri.path.orEmpty()).delete()
                                                    }
                                                }
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
    }
}

@Composable
private fun EditNoteMediaCategoryColumn(
    title: String,
    count: Int,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        EditNoteMediaCategoryTitle(title = title, count = count)
        content()
    }
}

@Composable
private fun EditNoteMediaCategoryRow(
    title: String,
    count: Int,
    content: LazyListScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        EditNoteMediaCategoryTitle(title = title, count = count)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

@Composable
private fun EditNoteMediaCategoryTitle(
    title: String,
    count: Int
) {
    Text(
        text = "$title ($count)",
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1
    )
}

@Composable
private fun AttachmentPreviewChip(item: NoteAttachmentItem) {
    val label = when {
        item.isAudioAttachment() -> "Audio"
        item.isVideoAttachment() -> "Video"
        else -> "File"
    }
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.09f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f))
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
            color = MaterialTheme.colorScheme.secondary,
            maxLines = 1
        )
    }
}

@Composable
private fun EditableAttachmentPill(
    item: NoteAttachmentItem,
    modifier: Modifier = Modifier,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val isAudio = item.isAudioAttachment()
    val isVideo = item.isVideoAttachment()
    if (isAudio) {
        NoteAudioMiniPlayer(
            uri = item.asUri,
            title = item.name,
            subtitle = "Audio",
            modifier = modifier.fillMaxWidth(),
            onRemove = onRemove
        )
        return
    }

    fun openAttachment() {
        val uri = item.asUri
        val openUri = if (uri.scheme == "file") {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                File(uri.path.orEmpty())
            )
        } else {
            uri
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(openUri, item.mime ?: "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { context.startActivity(Intent.createChooser(intent, "Open attachment")) }
    }

    Surface(
        modifier = modifier.height(42.dp),
        shape = RoundedCornerShape(14.dp),
        color = when {
            isVideo -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f)
            else -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f)
        },
        border = BorderStroke(
            1.dp,
            when {
                isVideo -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.20f)
                else -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = when {
                    isVideo -> "Video"
                    else -> "File"
                },
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                color = when {
                    isVideo -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.secondary
                },
                maxLines = 1
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
                text = "Open",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable { openAttachment() }
                    .padding(horizontal = 5.dp, vertical = 3.dp)
            )
            Text(
                text = "Remove",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable {
                        onRemove()
                    }
                    .padding(horizontal = 5.dp, vertical = 3.dp)
            )
        }
    }
}

private fun toSharableContentUri(context: Context, uri: Uri): Uri? {
    return when (uri.scheme) {
        "content" -> uri
        "file" -> {
            val path = uri.path ?: return null
            val file = File(path)
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        }
        else -> null
    }
}

fun shareImageUri(context: Context, uri: Uri) {
    val shareUri = toSharableContentUri(context, uri) ?: return

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/*"
        putExtra(Intent.EXTRA_STREAM, shareUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        clipData = ClipData.newUri(context.contentResolver, "shared_image", shareUri)
    }

    context.startActivity(Intent.createChooser(intent, "Share image"))
}

fun shareImageWithNote(
    context: Context,
    uri: Uri,
    note: String
) {
    val shareUri = toSharableContentUri(context, uri) ?: return
    val text = note.trim()

    if (text.isBlank()) {
        shareImageUri(context, uri)
        return
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/*"
        putExtra(Intent.EXTRA_STREAM, shareUri)
        putExtra(Intent.EXTRA_TEXT, text) // ✅ NOTE ONLY (no title)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        clipData = ClipData.newUri(context.contentResolver, "shared_image", shareUri)
    }

    context.startActivity(Intent.createChooser(intent, "Share"))
}
private fun decodeBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        when (uri.scheme) {
            "file" -> BitmapFactory.decodeFile(uri.path)
            "content" -> context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input)
            }
            else -> null
        }
    } catch (_: Throwable) {
        null
    }
}
private suspend fun rotate90AndSaveNewFile(
    context: Context,
    src: Uri
): File? = withContext(Dispatchers.IO) {
    val bmp = decodeBitmap(context, src) ?: return@withContext null

    val matrix = Matrix().apply { postRotate(90f) } // ✅ android.graphics.Matrix
    val rotated = try {
        Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
    } catch (_: Throwable) {
        null
    } ?: return@withContext null

    val dir = File(context.filesDir, "notes_photos").apply { mkdirs() }
    val outFile = File(dir, "rot_${System.currentTimeMillis()}.jpg")

    try {
        FileOutputStream(outFile).use { out ->
            rotated.compress(Bitmap.CompressFormat.JPEG, 92, out) // ✅ works
        }
        outFile
    } catch (_: Throwable) {
        null
    } finally {
        // cleanup bitmaps
        if (rotated != bmp) rotated.recycle()
        bmp.recycle()
    }
}
