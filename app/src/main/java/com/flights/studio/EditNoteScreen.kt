@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.flights.studio

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.SplitButtonShapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream


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
    var showAttachmentEditor by rememberSaveable { mutableStateOf(false) }
    var requestAutofillTitle by rememberSaveable { mutableStateOf(false) }

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

    // ✅ Match your other pages: page bg + backdrop
//    val pageBg = LocalAppPageBg.current
////    val pageBackdrop = rememberLayerBackdrop {
////        drawRect(pageBg)
////        drawContent()
////    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Surface(
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp
            ) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "Edit Note",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBackIos, contentDescription = "Back")
                        }
                    },
                    actions = {
                        var menuOpen by remember { mutableStateOf(false) }
                        var saving by rememberSaveable { mutableStateOf(false) }
                        val canSave = note.isNotBlank()

                        val isDark = isSystemInDarkTheme()
                        val scheme = MaterialTheme.colorScheme

                        // ✅ Glass
                        val glassFill = scheme.surfaceVariant.copy(alpha = if (isDark) 0.35f else 0.25f)
                        val glassContent = scheme.onSurface

                        // ✅ kill Material container (prevents “2 shapes” flash)
                        val btnColors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = glassContent,
                            disabledContainerColor = Color.Transparent,
                            disabledContentColor = glassContent.copy(alpha = 0.40f)
                        )

                        // --- pressed tracking (hoisted) ---
                        val leadIS = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        val trailIS = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        val leadPressed by leadIS.collectIsPressedAsState()
                        val trailPressed by trailIS.collectIsPressedAsState()

                        // ✅ smooth press morph (prevents 1-frame square)
                        val leadPressT by animateFloatAsState(
                            targetValue = if (leadPressed) 1f else 0f,
                            animationSpec = androidx.compose.animation.core.tween(90),
                            label = "editLeadPressT"
                        )
                        val trailPressT by animateFloatAsState(
                            targetValue = if (trailPressed) 1f else 0f,
                            animationSpec = androidx.compose.animation.core.tween(90),
                            label = "editTrailPressT"
                        )

                        // ✅ right-only morph (menu open)
                        val shapeT by animateFloatAsState(
                            targetValue = if (menuOpen) 1f else 0f,
                            label = "editSplitShapeT"
                        )

                        fun lerpDp(a: Dp, b: Dp, t: Float): Dp = a + (b - a) * t

                        val outer = 50.dp
                        val innerClosed = 5.dp
                        val innerOpen = 24.dp
                        val pressedInner = 14.dp

                        // base inner when menu opens
                        val rightInnerBase = lerpDp(innerClosed, innerOpen, shapeT)

                        // ✅ EFFECTIVE inners (press morph blended smoothly)
                        val leftInner = lerpDp(innerClosed, pressedInner, leadPressT)
                        val rightInner = lerpDp(rightInnerBase, pressedInner, trailPressT)
                        val hlColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)


                        // ✅ EFFECTIVE shapes (single source of truth)
                        val leftEffectiveShape = RoundedCornerShape(
                            topStart = outer,
                            bottomStart = outer,
                            topEnd = leftInner,
                            bottomEnd = leftInner
                        )

                        val rightEffectiveShape = RoundedCornerShape(
                            topStart = rightInner,
                            bottomStart = rightInner,
                            topEnd = outer,
                            bottomEnd = outer
                        )

                        // ✅ Kill SplitButton internal morphs + kill default checked CircleShape
                        val leftShapes = SplitButtonShapes(
                            shape = leftEffectiveShape,
                            pressedShape = leftEffectiveShape,
                            checkedShape = leftEffectiveShape
                        )
                        val rightShapes = SplitButtonShapes(
                            shape = rightEffectiveShape,
                            pressedShape = rightEffectiveShape,
                            checkedShape = rightEffectiveShape
                        )

                        SplitButtonLayout(
                            leadingButton = {
                                SplitButtonDefaults.LeadingButton(
                                    enabled = canSave && !saving,
                                    onClick = {
                                        if (!canSave || saving) return@LeadingButton
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
                                    },
                                    colors = btnColors,
                                    shapes = leftShapes,
                                    interactionSource = leadIS,
                                    modifier = Modifier
                                        .clip(leftEffectiveShape) // ✅ clip always matches ripple/pressed
                                        .drawBackdrop(
                                            backdrop = topBarBackdrop,
                                            shape = { leftEffectiveShape }, // ✅ glass follows same shape
                                            shadow = null,
                                            highlight = {
                                                Highlight(
                                                    width = 0.50.dp,
                                                    blurRadius = 1.dp,
                                                    alpha = 0.96f,
                                                    style = HighlightStyle.Plain(color = hlColor)
                                                )
                                            },
                                            effects = { blur(radius = 8f.dp.toPx(), edgeTreatment = TileMode.Clamp) },
                                            onDrawSurface = { drawRect(glassFill) }
                                        )
                                ) { Text(if (saving) "Saving…" else "Save") }
                            },
                            trailingButton = {
                                val rotation by animateFloatAsState(
                                    targetValue = if (menuOpen) 180f else 0f,
                                    label = "editSplitArrow"
                                )

                                SplitButtonDefaults.TrailingButton(
                                    checked = menuOpen,
                                    onCheckedChange = { menuOpen = it },
                                    enabled = true,
                                    colors = btnColors,
                                    shapes = rightShapes,
                                    interactionSource = trailIS,
                                    modifier = Modifier
                                        .clip(rightEffectiveShape) // ✅ clip always matches ripple/pressed
                                        .drawBackdrop(
                                            backdrop = topBarBackdrop,
                                            shape = { rightEffectiveShape }, // ✅ glass follows same shape
                                            shadow = null,
                                            highlight = {
                                                Highlight(
                                                    width = 0.50.dp,
                                                    blurRadius = 1.dp,
                                                    alpha = 0.96f,
                                                    style = HighlightStyle.Plain(color = hlColor)
                                                )
                                            },
                                            effects = { blur(radius = 8f.dp.toPx(), edgeTreatment = TileMode.Clamp) },
                                            onDrawSurface = { drawRect(glassFill) }
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.KeyboardArrowDown,
                                        contentDescription = "More",
                                        modifier = Modifier
                                            .size(SplitButtonDefaults.TrailingIconSize)
                                            .graphicsLayer { rotationZ = rotation }
                                    )
                                }
                            }
                        )

                        DropdownMenuPopup(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false }
                        ) {
                            val itemCount = 2
                            DropdownMenuGroup(
                                shapes = MenuDefaults.groupShape(index = 0, count = 1),
                                containerColor = MenuDefaults.groupVibrantContainerColor
                            ) {
                                DropdownMenuItem(
                                    selected = false,
                                    onClick = {
                                        menuOpen = false
                                        launchPicker = true
                                    },
                                    text = { Text("Add photo") },
                                    shapes = MenuDefaults.itemShape(index = 0, count = itemCount),
                                    colors = MenuDefaults.itemColors(),
                                    trailingIcon = { Icon(Icons.Filled.PhotoCamera, null) }
                                )

                                DropdownMenuItem(
                                    selected = false,
                                    onClick = {
                                        menuOpen = false
                                        requestAutofillTitle = true
                                        showHelp = true
                                    },
                                    text = { Text("Info") },
                                    shapes = MenuDefaults.itemShape(index = 1, count = itemCount),
                                    colors = MenuDefaults.itemColors(),
                                    leadingIcon = { Icon(Icons.Filled.Info, null) }
                                )
                            }
                        }

                        Spacer(Modifier.width(8.dp))
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .background(pageBg)
                .layerBackdrop(pageBackdrop) // ✅ record ONCE (important)
        ) {
            // ✅ same grid pattern background (NO layerBackdrop here)
            ProfileBackdropImageLayer(
                modifier = Modifier.matchParentSize(),
                lightRes = R.drawable.light_grid_pattern,
                darkRes = R.drawable.dark_grid_pattern,
                imageAlpha = if (isDark) 1f else 0.8f,
                scrimDark = 0f,
                scrimLight = 0f
            )

            Box(
                Modifier
                    .fillMaxSize()
                    .blur(if (showInstagramDialog) 8.dp else 0.dp) // ✅ BLUR WHOLE SCREEN
                    .background(
                        if (showInstagramDialog)
                            Color.Black.copy(alpha = 0.20f)   // ✨ light glass haze
                        else
                            Color.Transparent
                    )
                    .padding(padding)
                    .imePadding()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // ✅ PINNED images (fixed, not scroll)
                    if (images.isNotEmpty()) {
                        Surface(
                            modifier = Modifier,
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = if (isDark) 0.22f else 0.14f)
                            ),
                            shadowElevation = 0.dp
                        ) {
                            Column(Modifier.padding(horizontal = 10.dp, vertical = 9.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Images (${images.size})",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        text = "Reorder",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = 0.70f
                                        )
                                    )
                                }

                                Spacer(Modifier.size(7.dp))

                                ReorderableImageRow(
                                    images = images,
                                    onRemove = { uri -> images.remove(uri) },
                                    onOpenPreview = { uri -> previewUri = uri },
                                    itemSize = 78.dp
                                )


                                // ✅ open dialog
                                val actions = rememberInstagramPreviewActions(
                                    context = context,
                                    noteProvider = { note },

                                    onRotate = { uri ->
                                        scope.launch {
                                            val out = rotate90AndSaveNewFile(context, uri)
                                                ?: return@launch
                                            val newUri = Uri.fromFile(out)

                                            val idx = images.indexOf(uri)
                                            if (idx >= 0) images[idx] = newUri else images.add(
                                                newUri
                                            )
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


                                InstagramPreviewOverlay(
                                    uri = previewUri,
                                    onDismiss = { previewUri = null },
                                    onShare = actions.onShare,
                                    onShareWithNote = actions.onShareWithNote,
                                    onRotate = actions.onRotate,
                                    onReplace = actions.onReplace,
                                    onRemove = actions.onRemove
                                )

                            }

                        }

                    }

                    if (voiceNotes.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = if (isDark) 0.22f else 0.14f)
                            ),
                            shadowElevation = 0.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                Text(
                                    text = "Voice (${voiceNotes.size})",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )

                                voiceNotes.chunked(2).forEach { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                                    ) {
                                        rowItems.forEach { item ->
                                            val voiceIndex = voiceNotes.indexOf(item).coerceAtLeast(0)
                                            NoteAudioMiniPlayer(
                                                uri = item.asUri,
                                                title = "Voice ${voiceIndex + 1}",
                                                subtitle = formatVoiceDuration(item.durationMs),
                                                modifier = Modifier.weight(1f),
                                                onRemove = {
                                                    voiceNotes.remove(item)
                                                    if (item.asUri.scheme == "file") {
                                                        java.io.File(item.asUri.path.orEmpty()).delete()
                                                    }
                                                }
                                            )
                                        }
                                        if (rowItems.size == 1) {
                                            Spacer(Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (attachments.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = if (isDark) 0.22f else 0.14f)
                            ),
                            shadowElevation = 0.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(36.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Files (${attachments.size})",
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = if (showAttachmentEditor) "Hide" else "Edit",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .clickable { showAttachmentEditor = !showAttachmentEditor }
                                            .padding(horizontal = 10.dp, vertical = 7.dp)
                                    )
                                    Text(
                                        text = if (showAttachmentEditor) "Less" else "More",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .clickable { showAttachmentEditor = !showAttachmentEditor }
                                            .padding(horizontal = 10.dp, vertical = 7.dp)
                                    )
                                }

                                if (showAttachmentEditor) {
                                    val sortedAttachments = attachments.sortedWith(
                                        compareBy<NoteAttachmentItem> {
                                            when {
                                                it.isAudioAttachment() -> 1
                                                it.isVideoAttachment() -> 2
                                                else -> 0
                                            }
                                        }.thenBy { it.name.lowercase() }
                                    )
                                    sortedAttachments.chunked(2).forEach { rowItems ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                                        ) {
                                            rowItems.forEach { item ->
                                                EditableAttachmentPill(
                                                    item = item,
                                                    modifier = Modifier.weight(1f),
                                                    onRemove = {
                                                        attachments.remove(item)
                                                        if (item.asUri.scheme == "file") java.io.File(item.asUri.path.orEmpty()).delete()
                                                        if (attachments.isEmpty()) showAttachmentEditor = false
                                                    }
                                                )
                                            }
                                            if (rowItems.size == 1) {
                                                Spacer(Modifier.weight(1f))
                                            }
                                        }
                                    }
                                } else {
                                    LazyRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                                    ) {
                                        itemsIndexed(attachments.take(4)) { _, item ->
                                            AttachmentPreviewChip(item)
                                        }
                                    }
                                }
                            }
                        }
                    }


                    // ✅ ONLY the content below scrolls
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            // --- Compact title + note composer
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = if (isDark) 0.28f else 0.18f)
                                ),
                                shadowElevation = 0.dp
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Edit content",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(Modifier.weight(1f))
                                        Surface(
                                            shape = RoundedCornerShape(999.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f),
                                            border = BorderStroke(
                                                1.dp,
                                                MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                                            )
                                        ) {
                                            Text(
                                                text = "${note.length} chars",
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    OutlinedTextField(
                                        value = title,
                                        onValueChange = { title = it },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp),
                                        singleLine = true,
                                        placeholder = {
                                            Text(
                                                text = "Title",
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        keyboardOptions = KeyboardOptions(
                                            capitalization = KeyboardCapitalization.Sentences,
                                            autoCorrectEnabled = false
                                        ),
                                        shape = RoundedCornerShape(14.dp)
                                    )

                                    OutlinedTextField(
                                        value = note,
                                        onValueChange = { note = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 4,
                                        placeholder = {
                                            Text("Write your note")
                                        },
                                        keyboardOptions = KeyboardOptions(
                                            capitalization = KeyboardCapitalization.Sentences,
                                            autoCorrectEnabled = false
                                        ),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                }
                            }
                        }

                        // bottom padding so keyboard doesn't feel cramped
                        item { Spacer(Modifier.size(18.dp)) }
                    }
                }
            }
        }
    }
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
            isAudio -> MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
            isVideo -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f)
            else -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f)
        },
        border = BorderStroke(
            1.dp,
            when {
                isAudio -> MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
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
                    isAudio -> "Audio"
                    isVideo -> "Video"
                    else -> "File"
                },
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                color = when {
                    isAudio -> MaterialTheme.colorScheme.primary
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

