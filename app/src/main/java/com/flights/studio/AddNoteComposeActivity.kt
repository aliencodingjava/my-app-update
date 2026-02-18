@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package com.flights.studio

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import kotlinx.coroutines.launch
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
                    onSave = { note, title, images, wantsReminder ->
                        if (note.isBlank()) return@AddNoteScreen

                        val result = Intent().apply {
                            putExtra("NEW_NOTE", note)
                            putExtra("NEW_NOTE_TITLE", title.trim())
                            putStringArrayListExtra(
                                "NEW_NOTE_IMAGES",
                                ArrayList(images.map { it.toString() })
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
private const val MAX_NOTE_CHARS = 5_000_000      // you can raise to 5_000_000 if you want
private const val PASTE_JUMP = 5_000              // consider it a paste if jump is big
private const val PASTE_CHUNK = 8_192             // chunk size for safe appending



@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AddNoteScreen(
    onBack: () -> Unit,
    onSave: (note: String, title: String, images: List<Uri>, wantsReminder: Boolean) -> Unit
) {

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val noteTap = remember { MutableInteractionSource() }
    val noteFocusRequester = remember { FocusRequester() }
    var noteFocused by rememberSaveable { mutableStateOf(false) }
    val isDark = isSystemInDarkTheme()
    val scheme = MaterialTheme.colorScheme

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
                scheme.surface
            } else {
                scheme.surfaceContainerHigh
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
    var wantsReminder by rememberSaveable { mutableStateOf(false) }
    val images = remember { mutableStateListOf<Uri>() }
    var showAllImages by rememberSaveable { mutableStateOf(false) }
    var sheetViewMode by rememberSaveable { mutableStateOf(SheetViewMode.Grid) }

    // Tip/dot logic (same behavior you already had)
    var showHelp by rememberSaveable { mutableStateOf(false) }
    var showNotifDialog by rememberSaveable { mutableStateOf(false) }

// ✅ manual title edit inside the SAME hint box
    var showManualTitle by rememberSaveable { mutableStateOf(false) }
    var titleFocused by rememberSaveable { mutableStateOf(false) }
    val titleFocusRequester = remember { FocusRequester() }

    LaunchedEffect(showManualTitle) {
        if (showManualTitle) titleFocusRequester.requestFocus()
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


    val pageBg = LocalAppPageBg.current


    val pageBackdrop = rememberLayerBackdrop {
        drawRect(pageBg)
        drawContent()
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
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                color = ui.topBarBg,
                shadowElevation = if (isDark) 0.dp else 1.dp
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
                        val scheme = MaterialTheme.colorScheme

                        // ✅ same glass fill like Notes
                        val glassFill = scheme.surfaceVariant.copy(alpha = if (isDark) 0.35f else 0.25f)
                        val glassContent = scheme.onSurface

                        val canSave = note.isNotBlank()
                        var menuOpen by remember { mutableStateOf(false) }
                        val titleTint = when {
                            aiPlaceholderLoading -> MaterialTheme.colorScheme.onSurfaceVariant
                            aiPlaceholder != null -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        val dynamicColor = if (canSave) titleTint else glassContent

                        val btnColors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = dynamicColor,
                            disabledContainerColor = Color.Transparent,
                            disabledContentColor = dynamicColor
                        )
                        val aiActive = titleFromAi || aiPlaceholder != null

                        val titleBoxTint = when {
                            aiActive && isDark -> MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
                            aiActive -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                            else -> Color.Transparent
                        }

                        val tintAlpha by animateFloatAsState(
                            targetValue = if (canSave) 1f else 0f,
                            animationSpec = tween(160),
                            label = "saveTintAlpha"
                        )





                        // --- pressed tracking (hoisted) ---
                        val leadIS = remember { MutableInteractionSource() }
                        val trailIS = remember { MutableInteractionSource() }
                        val leadPressed by leadIS.collectIsPressedAsState()
                        val trailPressed by trailIS.collectIsPressedAsState()

                        // ✅ smooth press morph (prevents 1-frame square glitch)
                        val leadPressT by animateFloatAsState(
                            targetValue = if (leadPressed) 1f else 0f,
                            animationSpec = tween(durationMillis = 90),
                            label = "leadPressT"
                        )
                        val trailPressT by animateFloatAsState(
                            targetValue = if (trailPressed) 1f else 0f,
                            animationSpec = tween(durationMillis = 90),
                            label = "trailPressT"
                        )

                        // --- menu morph (right side opens) ---
                        val shapeT by animateFloatAsState(
                            targetValue = if (menuOpen) 1f else 0f,
                            label = "splitShapeT"
                        )

                        fun lerpDp(a: Dp, b: Dp, t: Float): Dp = a + (b - a) * t

                        val outer = 50.dp
                        val innerClosed = 5.dp
                        val innerOpen = 24.dp
                        val pressedInner = 14.dp

                        // ✅ base inner when menu opens
                        val rightInnerBase = lerpDp(innerClosed, innerOpen, shapeT)

                        // ✅ EFFECTIVE inners (press morph applied smoothly)
                        val leftInner = lerpDp(innerClosed, pressedInner, leadPressT)
                        val rightInner = lerpDp(rightInnerBase, pressedInner, trailPressT)

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

                        val scale by animateFloatAsState(
                            targetValue = if (menuOpen) 1.00f else 1f,
                            label = "splitScale"
                        )

                        Box(
                            Modifier
                                .wrapContentSize(Alignment.TopEnd)
                                .graphicsLayer { scaleX = scale; scaleY = scale }
                        ) {
                            val scope = rememberCoroutineScope()
                            var saving by rememberSaveable { mutableStateOf(false) }
                            val hlColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)


                            SplitButtonLayout(
                                leadingButton = {
                                    SplitButtonDefaults.LeadingButton(
                                        enabled = canSave && !saving,
                                        onClick = onClick@{
                                            if (!canSave || saving) return@onClick
                                            focusManager.clearFocus()
                                            keyboard?.hide()

                                            scope.launch {
                                                saving = true
                                                try {
                                                    val finalTitle = if (title.isNotBlank() || !tipEnabled) {
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
                                                    if (title.isBlank() && finalTitle.isNotBlank()) {
                                                        titleFromAi = true
                                                    }

                                                    val finalWantsReminder =
                                                        wantsReminder && NotificationGate.areNotificationsEnabled(context)

                                                    if (wantsReminder && !finalWantsReminder) {
                                                        showNotifDialog = true
                                                        return@launch
                                                    }

                                                    onSave(note, finalTitle, images.toList(), finalWantsReminder)
                                                } finally {
                                                    saving = false
                                                }
                                            }
                                        },
                                        colors = btnColors,
                                        shapes = leftShapes,
                                        interactionSource = leadIS,
                                        modifier = Modifier
                                            .zIndex(1f)
                                            .clip(leftEffectiveShape)
                                            .drawBackdrop(
                                                backdrop = pageBackdrop, // ✅ uses your existing backdrop
                                                shape = { leftEffectiveShape },
                                                shadow = null,
                                                highlight = {
                                                    Highlight(
                                                        width = 0.50.dp,
                                                        blurRadius = 1.dp,
                                                        alpha = 0.96f,
                                                        style = HighlightStyle.Plain(color = hlColor)
                                                    )
                                                },
                                                effects = { blur(radius = 2f.dp.toPx(), edgeTreatment = TileMode.Clamp) },
                                                onDrawSurface = {
                                                    drawRect(glassFill) // base glass
                                                    if (aiActive) {
                                                        drawRect(titleBoxTint.copy(alpha = titleBoxTint.alpha * tintAlpha)) // ✅ overlay tint
                                                    }
                                                }
                                            )
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("Save", color = dynamicColor)

                                            if (saving) {
                                                Spacer(Modifier.width(8.dp))
                                                androidx.compose.material3.CircularProgressIndicator(
                                                    strokeWidth = 2.dp,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }

                                },
                                trailingButton = {
                                    val rotation by animateFloatAsState(
                                        targetValue = if (menuOpen) 180f else 0f,
                                        label = "splitArrow"
                                    )

                                    // ✅ you want it to LOOK disabled when nothing is set
                                    val trailEnabled = wantsReminder || images.isNotEmpty()
                                    val hlColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)


                                    Box(Modifier.wrapContentSize()) {
                                        SplitButtonDefaults.TrailingButton(
                                            checked = menuOpen,
                                            onCheckedChange = { menuOpen = it },
                                            enabled = trailEnabled,
                                            colors = btnColors,
                                            shapes = rightShapes,
                                            interactionSource = trailIS,
                                            modifier = Modifier
                                                .zIndex(0f)
                                                .clip(rightEffectiveShape)
                                                .drawBackdrop(
                                                    backdrop = pageBackdrop, // ✅ uses your existing backdrop
                                                    shape = { rightEffectiveShape },
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
                                                tint = dynamicColor,
                                                modifier = Modifier
                                                    .size(SplitButtonDefaults.TrailingIconSize)
                                                    .graphicsLayer { rotationZ = rotation }
                                            )

                                        }

                                        // ✅ still allow opening even when visually disabled
                                        if (!trailEnabled) {
                                            Box(
                                                Modifier
                                                    .matchParentSize()
                                                    .clickable(
                                                        interactionSource = remember { MutableInteractionSource() },
                                                        indication = null
                                                    ) { menuOpen = !menuOpen }
                                            )
                                        }
                                    }
                                }
                            )

                            // ✅ YOUR MENU: unchanged
                            DropdownMenuPopup(
                                expanded = menuOpen,
                                onDismissRequest = { menuOpen = false }
                            ) {
                                val itemCount = 4

                                DropdownMenuGroup(
                                    shapes = MenuDefaults.groupShape(index = 0, count = 1),
                                    containerColor = MenuDefaults.groupVibrantContainerColor
                                ) {
                                    DropdownMenuItem(
                                        selected = false,
                                        onClick = {
                                            menuOpen = false
                                            picker.launch(arrayOf("image/*"))
                                        },
                                        text = { Text("Add photo") },
                                        shapes = MenuDefaults.itemShape(index = 0, count = itemCount),
                                        colors = MenuDefaults.itemColors(),
                                        trailingIcon = { Icon(Icons.Filled.PhotoCamera, contentDescription = null) }
                                    )

                                    DropdownMenuItem(
                                        selected = false,
                                        onClick = {
                                            menuOpen = false

                                            if (wantsReminder) {
                                                wantsReminder = false
                                                return@DropdownMenuItem
                                            }

                                            if (NotificationGate.areNotificationsEnabled(context)) {
                                                wantsReminder = true
                                            } else {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                                    ActivityCompat.checkSelfPermission(
                                                        context, Manifest.permission.POST_NOTIFICATIONS
                                                    ) != PackageManager.PERMISSION_GRANTED
                                                ) {
                                                    notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                } else {
                                                    showNotifDialog = true
                                                }
                                            }
                                        },
                                        text = { Text(if (wantsReminder) "Remove reminder" else "Add reminder") },
                                        shapes = MenuDefaults.itemShape(index = 1, count = itemCount),
                                        colors = MenuDefaults.itemColors(),
                                        trailingIcon = { Text(if (wantsReminder) "⏰" else "⏱️") }
                                    )

                                    DropdownMenuItem(
                                        selected = false,
                                        onClick = {
                                            menuOpen = false
                                            if (tipEnabled) {
                                                showTitleTip(expanded = false)
                                            } else {
                                                context.startActivity(Intent(context, NotesSettingsComposeActivity::class.java))
                                            }
                                        },
                                        text = {
                                            Column {
                                                Text("Info", style = MaterialTheme.typography.labelLarge)
                                                Text(
                                                    text = if (tipEnabled) "Smart suggestions active" else "Turn on in settings",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        shapes = MenuDefaults.itemShape(index = 2, count = itemCount),
                                        colors = MenuDefaults.itemColors(),
                                        leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null) },
                                        trailingIcon = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (showDot) {
                                                    Box(
                                                        Modifier
                                                            .size(8.dp)
                                                            .clip(RoundedCornerShape(50))
                                                            .background(MaterialTheme.colorScheme.error)
                                                    )
                                                    Spacer(Modifier.width(10.dp))
                                                }

                                                Surface(
                                                    shape = RoundedCornerShape(999.dp),
                                                    color = if (tipEnabled)
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                                    else
                                                        MaterialTheme.colorScheme.surfaceVariant,
                                                    border = BorderStroke(
                                                        1.dp,
                                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                                                    )
                                                ) {
                                                    Text(
                                                        text = if (tipEnabled) "On" else "Off",
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (tipEnabled)
                                                            MaterialTheme.colorScheme.primary
                                                        else
                                                            MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
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
                .layerBackdrop(pageBackdrop) // ✅ record ONCE here
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            // ✅ pattern is inside the recorded content already
            val isDark = isSystemInDarkTheme()
            ProfileBackdropImageLayer(
                modifier = Modifier.matchParentSize(), // ❌ remove .layerBackdrop(pageBackdrop)
                lightRes = R.drawable.lightgridpattern,
                darkRes = R.drawable.darkgridpattern,
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
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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


                    // ✅ STACK like your sketch: big title box, note overlaps it (no gap)
                    Box(modifier = Modifier.fillMaxWidth()) {

                        // treat title-focus / manual-title like focus too
                        val titleActive = showManualTitle || titleFocused
                        val anyFocused = noteFocused || titleActive

                        // ✅ big title box height + overlap amount
                        val titleBoxH = 92.dp
                        val overlap = 28.dp
                        val topEdgePad = 16.dp // title text ALWAYS near top edge

                        // ✅ lift BOTH together (so overlap never changes, no gap)
                        val stackLiftY by animateDpAsState(
                            targetValue = if (anyFocused) (-10).dp else 0.dp,
                            animationSpec = spring(dampingRatio = 0.85f, stiffness = 450f),
                            label = "stackLiftY"
                        )

                        val hintTextScale by animateFloatAsState(
                            targetValue = if (anyFocused) 1.14f else 1.00f,
                            label = "hintTextScale"
                        )

                        // --- Title AI box (BACKGROUND) ---
                        val showTitleBox = tipEnabled
                        if (showTitleBox) {

                            val boxText = when {
                                title.isNotBlank() -> title
                                aiPlaceholderLoading -> "Thinking…"
                                aiPlaceholder != null -> aiPlaceholder!!
                                else -> "Suggestion"
                            }

                            val fieldPlaceholder = when {
                                aiPlaceholderLoading -> "Thinking…"
                                aiPlaceholder != null -> aiPlaceholder!!
                                else -> "Title"
                            }

                            val aiActive = titleFromAi || aiPlaceholder != null

                            val boxColor = when {
                                aiActive && isDark -> MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
                                aiActive -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                else -> ui.cardBg
                            }

                            val aiBorder = when {
                                aiActive -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                            }


                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 28.dp)
                                    .padding(top = 10.dp)
                                    .height(titleBoxH)               // ✅ BIG box
                                    .offset(y = stackLiftY)          // ✅ lift with note
                                    .zIndex(0f)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { showManualTitle = true },
                                shape = RoundedCornerShape(12.dp),
                                color = boxColor,
                                shadowElevation = 0.dp,
                                border = BorderStroke(1.dp, aiBorder)

                            ) {
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .padding(start = 14.dp, end = 14.dp, top = topEdgePad, bottom = 10.dp),
                                    contentAlignment = Alignment.TopCenter
                                ) {

                                    // ✅ PINNED to top ALWAYS
                                    if (showManualTitle) {
                                        OutlinedTextField(
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
                                            shape = RoundedCornerShape(12.dp),
                                            placeholder = {
                                                Text(fieldPlaceholder, style = MaterialTheme.typography.labelSmall)
                                            },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = Color.Transparent,
                                                unfocusedContainerColor = Color.Transparent,
                                                focusedBorderColor = Color.Transparent,
                                                unfocusedBorderColor = Color.Transparent,
                                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                            )
                                        )
                                    } else {

                                        val showAiIcon = aiPlaceholderLoading || aiPlaceholder != null || titleFromAi

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .wrapContentWidth()
                                                .graphicsLayer {
                                                    scaleX = hintTextScale
                                                    scaleY = hintTextScale
                                                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                                                }
                                        ) {
                                            if (showAiIcon && !aiPlaceholderLoading) {
                                                Icon(
                                                    imageVector = Icons.Rounded.AutoAwesome,
                                                    contentDescription = "AI",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier
                                                        .size(18.dp)
                                                        .padding(end = 6.dp)
                                                )
                                            }

                                            if (aiPlaceholderLoading) {
                                                ShimmerThinkingTextCompat(
                                                    text = "Thinking…",
                                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                                )
                                            } else {
                                                Text(
                                                    text = boxText,
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                                    color = MaterialTheme.colorScheme.primary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }


                                    }

                                }
                            }
                        }

                        // --- Note card (FOREGROUND) overlaps title box ---
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = titleBoxH - overlap) // ✅ overlap = NO GAP visible
                                .offset(y = stackLiftY)             // ✅ lift with title
                                .zIndex(1f)
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
                            shape = RoundedCornerShape(18.dp),
                            color = ui.cardBg,
                            shadowElevation = 1.dp
                        ) {
                            Column(Modifier.padding(14.dp)) {

                                Box {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Write your note",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Spacer(Modifier.weight(1f))

                                        Text(
                                            text = "${note.length}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                alpha = 0.70f
                                            )
                                        )
                                    }

                                    if (wantsReminder) {
                                        AssistChip(
                                            onClick = { wantsReminder = false },
                                            label = {
                                                Text(
                                                    "Reminder set",
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            },
                                            colors = AssistChipDefaults.assistChipColors(
                                                containerColor = MaterialTheme.colorScheme.primary.copy(
                                                    alpha = 0.18f
                                                ),
                                                labelColor = MaterialTheme.colorScheme.primary
                                            ),
                                            border = null,
                                            modifier = Modifier
                                                .align(Alignment.CenterStart)
                                                .padding(start = 130.dp)
                                                .height(20.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(4.dp))

                                Text(
                                    text = "Paste links, add details, dump ideas — anything",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f)
                                )

                                Spacer(Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = note,
                                    onValueChange = { new ->
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
                                        .fillMaxWidth()
                                        .height(210.dp)
                                        .focusRequester(noteFocusRequester)
                                        .onFocusChanged { fs ->
                                            noteFocused = fs.isFocused

                                            // ✅ when note gets focus, close title edit + return to centered suggestion
                                            if (fs.isFocused) {
                                                showManualTitle = false
                                                titleFocused = false
                                            }
                                        },

                                            shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(
                                            alpha = 0.35f
                                        ),
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(
                                            alpha = 0.25f
                                        ),
                                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(
                                            alpha = 0.35f
                                        ),
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(
                                            alpha = 0.18f
                                        ),
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = 0.70f
                                        ),
                                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = 0.70f
                                        ),
                                    )
                                )
                            }
                        }
                    }

                    // ---- Images preview (clean grid) ----
                    if (images.isNotEmpty()) {
                        val maxShow = 6
                        val extra = (images.size - maxShow).coerceAtLeast(0)

                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = ui.cardBg),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Column(Modifier.padding(14.dp)) {

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
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        text = "View all",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                                    )
                                }

                                Spacer(Modifier.height(10.dp))

                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    state = rememberLazyGridState(),
                                    userScrollEnabled = false, // ✅ let parent scroll handle it
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(
                                            when {
                                                images.size <= 2 -> 180.dp
                                                images.size <= 4 -> 370.dp
                                                else -> 560.dp
                                            }
                                        )
                                ) {
                                    itemsIndexed(images.take(maxShow)) { index, uri ->
                                        val shape = RoundedCornerShape(14.dp)

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(1.25f)
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
                                                    .padding(6.dp)
                                                    .size(28.dp)
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
                                                    modifier = Modifier.size(16.dp)
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
    }
    // --- helpers (put near bottom of file or above AddNoteScreen) ---

    fun tipMetaFor(note: String, hasImages: Boolean): TipMeta {
        val n = note.trim()
        val lines = n.lines().map { it.trim() }.filter { it.isNotBlank() }
        val hasLink = Regex("""https?://\S+""").containsMatchIn(n)
        val hasChecklist =
            lines.any { it.startsWith("- ") || it.startsWith("• ") || it.matches(Regex("""\d+\.\s+.*""")) }
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
                message = "Want a title? Pick one now — or skip and come back later from Info.",
                reason = "Tip: optional"
            )
        }
    }

    fun buildTitleSuggestions(note: String, hasImages: Boolean): List<TitleSuggestion> {
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

        // 1) If it has link -> link-based title
        if (!link.isNullOrBlank()) {
            val domain = runCatching {
                link.toUri().host?.removePrefix("www.")
            }.getOrNull()
            if (!domain.isNullOrBlank()) {
                picks += TitleSuggestion(
                    title = domain.take(32),
                    why = "Pulled from the link domain"
                )
            }
            picks += TitleSuggestion(
                title = "Link to check",
                why = "This note contains a link"
            )
        }

        // 2) If it has images -> image-based title
        if (hasImages) {
            picks += TitleSuggestion(
                title = "Photos",
                why = "You attached images"
            )
            picks += TitleSuggestion(
                title = "Receipt / Screenshot",
                why = "Common photo note type"
            )
        }

        // 3) If it looks like checklist -> list title
        val hasChecklist = lines.any {
            it.startsWith("- ") || it.startsWith("• ") || it.matches(Regex("""\d+\.\s+.*"""))
        }
        if (hasChecklist) {
            picks += TitleSuggestion(
                title = "To-do",
                why = "Looks like a checklist"
            )
            picks += TitleSuggestion(
                title = "Shopping list",
                why = "List-style note"
            )
        }

        // 4) Use first sentence / line if meaningful
        if (firstSentence.length >= 8) {
            picks += TitleSuggestion(
                title = firstSentence.take(40),
                why = "From the first sentence"
            )
        }
        if (firstLine.length in 8..60) {
            picks += TitleSuggestion(
                title = firstLine.take(40),
                why = "From the first line"
            )
        }

        // fallback
        if (picks.isEmpty()) {
            picks += TitleSuggestion("Quick note", "Simple default")
            picks += TitleSuggestion("Idea", "Good for short thoughts")
            picks += TitleSuggestion("Reminder", "Good generic label")
        }

        // de-dupe and keep best 3
        return picks
            .map { it.copy(title = it.title.trim().ifBlank { "Quick note" }) }
            .distinctBy { it.title.lowercase() }
            .take(6)
    }

    if (showHelp) {
        val meta = remember(note, images.size) { tipMetaFor(note, images.isNotEmpty()) }
        val suggestions =
            remember(note, images.size) { buildTitleSuggestions(note, images.isNotEmpty()) }

        // --- AI suggestions state (real AI) ---
        var aiLoading by rememberSaveable(showHelp) { mutableStateOf(false) }
        var aiError by rememberSaveable(showHelp) { mutableStateOf<String?>(null) }
        var aiSuggestions by rememberSaveable(showHelp) {
            mutableStateOf<List<TitleSuggestion>>(
                emptyList()
            )
        }

        val shownSuggestions = aiSuggestions
            .ifEmpty { suggestions }
            .ifEmpty { listOf(TitleSuggestion("Quick note", "Fallback")) }

        val visibleSuggestions =
            if (shownSuggestions.size >= 15) {
                shownSuggestions.take(20)
            } else {
                shownSuggestions + suggestions
                    .filterNot { s -> shownSuggestions.any { it.title == s.title } }
                    .take(20 - shownSuggestions.size)
            }



        // ✅ force compact by default when opening from Info
        var expanded by rememberSaveable(showHelp) { mutableStateOf(openExpanded) }
        var compactIntro by rememberSaveable(showHelp) { mutableStateOf(!openExpanded) }

        var disableTips by rememberSaveable { mutableStateOf(false) }
        var pickedIndex by rememberSaveable { mutableIntStateOf(0) }

        // ✅ safe picked (no crash)
        val picked = visibleSuggestions.getOrNull(pickedIndex)
            ?: visibleSuggestions.firstOrNull()
            ?: TitleSuggestion("Quick note", "Fallback")


        fun enterExpanded() {
            compactIntro = false
            expanded = true
            openExpanded = true
        }

        fun closeDialog() {
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
            closeDialog()
        }

        fun applyTitleAndClose() {
            title = picked.title
            titleFromAi = true   // makes it “AI styled”
            showManualTitle = false
            titleFocused = false
            focusManager.clearFocus()
            closeDialog()
        }



        // Fetch AI when dialog is expanded (or when user enters expanded)
        LaunchedEffect(showHelp, expanded, note, images.size, tipEnabled) {
            if (!showHelp) return@LaunchedEffect
            if (!expanded) return@LaunchedEffect
            if (!tipEnabled) return@LaunchedEffect

            // avoid refetching if we already have AI for this open session
            if (aiSuggestions.isNotEmpty() || aiLoading) return@LaunchedEffect

            aiLoading = true
            aiError = null

            try {
                // IMPORTANT: put your key somewhere safe; this is just to prove it works
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
                aiSuggestions = emptyList() // keep local fallback
            } finally {
                aiLoading = false
            }
        }

        TitleHelperDialog(
            meta = meta,
            title = title,
            expanded = expanded,
            compactIntro = compactIntro,
            disableTips = disableTips,
            pickedIndex = pickedIndex,
            suggestions = visibleSuggestions,          // ✅ you already computed this
            aiLoading = aiLoading,
            aiError = aiError,
            onPick = { pickedIndex = it },
            onEnterExpanded = { enterExpanded() },
            onToggleDisable = { disableTips = !disableTips },
            onDismiss = { closeDialog() },
            onPrimary = {
                if (disableTips) applyOffAndClose() else applyTitleAndClose()
            }
        )
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
                it.startsWith("- ") || it.startsWith("• ") || it.matches(Regex("""\d+\.\s+.*"""))
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
