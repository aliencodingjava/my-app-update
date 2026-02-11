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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import coil.compose.SubcomposeAsyncImage
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

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

        if (note.isEmpty() || position == -1) {
            Toast.makeText(this, getString(R.string.error_loading_note), Toast.LENGTH_SHORT).show()
            finish()
            applyTransition()
            return
        }

        setContent {

            // 🔁 inverted system bar logic
            val isDark = androidx.compose.foundation.isSystemInDarkTheme()
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

    val uris: List<Uri> = remember(uid) {
        if (uid.isNullOrBlank()) emptyList()
        else NoteMediaStore.getUris(ctx, uid)
    }

    val canEdit = !uid.isNullOrBlank()

    val pageBg = LocalAppPageBg.current
    val pageBackdrop = rememberLayerBackdrop {
        drawRect(pageBg)
        drawContent()
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            // ✅ same AddNote top bar style
            Surface(
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 1.dp
            ) {
                CenterAlignedTopAppBar(
                    title = {
                        val t = MaterialTheme.typography.titleLarge
                        Text(
                            text = title?.takeIf { it.isNotBlank() } ?: "Note",
                            style = t.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        if (canEdit) {
                            IconButton(onClick = onEdit) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                        }
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
                .layerBackdrop(pageBackdrop)
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(padding)
                .imePadding()
                .padding(horizontal = 14.dp, vertical = 0.dp)
        ) {

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // ✅ PINNED images (not scroll)
                if (uris.isNotEmpty()) {
                    NoteImagesRow(
                        uris = uris,
                        onImageClick = { index ->
                            onOpenImages(uris.map { it.toString() }, index)
                        }
                    )
                }

                // ✅ ONLY text scrolls
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 18.dp)
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shadowElevation = 1.dp
                        ) {
                            Text(
                                text = note,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
    }
    @Composable
private fun NoteImagesRow(
    uris: List<Uri>,
    onImageClick: (Int) -> Unit
) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(uris) { index, uri ->
                val shape = RoundedCornerShape(18.dp)

                Surface(
                    shape = shape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .width(240.dp)
                        .aspectRatio(16f / 10f)
                        .clickable { onImageClick(index) }
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
                                iconSize = 34,
                                alpha = 0.55f
                            )
                        },

                        error = {
                            ImagePlaceholderSurface(
                                modifier = Modifier.fillMaxSize(),
                                shape = shape,
                                iconSize = 34,
                                alpha = 0.55f
                            )
                        }
                    )
                }
            }
        }
}
