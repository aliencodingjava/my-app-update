package com.flights.studio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Size
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class AppIconPickerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val initialAlias = AppIconManager.getActiveIcon(this)

        setContent {
            val darkTheme = isSystemInDarkTheme()

            SideEffect {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }

            MaterialTheme(
                colorScheme = if (darkTheme) {
                    androidx.compose.material3.darkColorScheme()
                } else {
                    androidx.compose.material3.lightColorScheme()
                }
            ) {
                AppIconPickerScreen(
                    initialAlias = initialAlias,
                    onBack = { finish() },
                    onSelected = { alias ->
                        AppIconManager.setActiveIcon(this@AppIconPickerActivity, alias)
                    }
                )
            }
        }
    }
}

private sealed interface GridEntry {
    data class Header(val title: String, val subtitle: String) : GridEntry
    data class Item(val option: IconOption) : GridEntry
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppIconPickerScreen(
    initialAlias: String,
    onBack: () -> Unit,
    onSelected: (String) -> Unit
) {
    BackHandler(onBack = onBack)

    var currentAlias by remember { mutableStateOf(initialAlias) }

    var toastLabel by remember { mutableStateOf<String?>(null) }
    var toastVisible by remember { mutableStateOf(false) }

    val categories = remember {
        listOf(
            Triple(
                "Standard Glass",
                "Clean solid icons",
                listOf(
                    IconOption("Original",  R.drawable.dicon,       AppIconManager.DICON),
                    IconOption("Watch",     R.drawable.watchos,     AppIconManager.WATCH_OS),
                    IconOption("Midnight",  R.drawable.cleardark,   AppIconManager.CLEAR_DARK),
                    IconOption("Cloud",     R.drawable.clearlight,  AppIconManager.CLEAR_LIGHT),
                    IconOption("Navy",      R.drawable.dark,        AppIconManager.DARK),
                    IconOption("Forest",    R.drawable.tinteddark,  AppIconManager.TINTED_DARK),
                    IconOption("Sage",      R.drawable.tintedlight, AppIconManager.TINTED_LIGHT),
                    IconOption("Classic",   R.drawable.generalicon, AppIconManager.GENERAL_ICON),
                )
            ),
            Triple(
                "Glass",
                "Frosted translucent finish",
                listOf(
                    IconOption("Original",  R.drawable.diconf,       AppIconManager.DICON_F),
                    IconOption("Watch",     R.drawable.watchosf,     AppIconManager.WATCH_OS_F),
                    IconOption("Midnight",  R.drawable.cleardarkf,   AppIconManager.CLEAR_DARK_F),
                    IconOption("Cloud",     R.drawable.clearlightf,  AppIconManager.CLEAR_LIGHT_F),
                    IconOption("Navy",      R.drawable.darkf,        AppIconManager.DARK_F),
                    IconOption("Forest",    R.drawable.tinteddarkf,  AppIconManager.TINTED_DARK_F),
                    IconOption("Sage",      R.drawable.tintedlightf, AppIconManager.TINTED_LIGHT_F),
                )
            ),
            Triple(
                "Winter",
                "Snow edition icons",
                listOf(
                    IconOption("Midnight",  R.drawable.cleardarkwinter,   AppIconManager.CLEAR_DARK_WINTER),
                    IconOption("Cloud",     R.drawable.clearlightwinter,  AppIconManager.CLEAR_LIGHT_WINTER),
                    IconOption("Dark",      R.drawable.darkwinter,        AppIconManager.DARK_WINTER),
                    IconOption("Original",  R.drawable.originalwinter,    AppIconManager.ORIGINAL_WINTER),
                    IconOption("Forest",    R.drawable.tinteddarkwinter,  AppIconManager.TINTED_DARK_WINTER),
                    IconOption("Sage",      R.drawable.tintedlightwinter, AppIconManager.TINTED_LIGHT_WINTER),
                    IconOption("Watch",     R.drawable.watchoswinter,     AppIconManager.WATCH_OS_WINTER),
                )
            ),
            Triple(
                "JH",
                "Jackson Hole edition",
                listOf(
                    IconOption("Graphite",    R.drawable.cleardarkjh,   AppIconManager.CLEAR_DARK_JH),
                    IconOption("Silver",      R.drawable.clearlightjh,  AppIconManager.CLEAR_LIGHT_JH),
                    IconOption("Espresso",    R.drawable.darkjh,        AppIconManager.DARK_JH),
                    IconOption("Amber",       R.drawable.originaljh,    AppIconManager.ORIGINAL_JH),
                    IconOption("Olive",       R.drawable.tinteddarkjh,  AppIconManager.TINTED_DARK_JH),
                    IconOption("Sage",        R.drawable.tintedlightjh, AppIconManager.TINTED_LIGHT_JH),
                    IconOption("Amber Glass", R.drawable.watchosjh,     AppIconManager.WATCH_OS_JH),
                )
            )
        )
    }

    val gridEntries = remember(categories) {
        buildList {
            categories.forEach { (title, subtitle, icons) ->
                add(GridEntry.Header(title, subtitle))
                icons.forEach { add(GridEntry.Item(it)) }
            }
        }
    }

    val allIcons = remember(categories) { categories.flatMap { it.third } }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        launch(Dispatchers.IO) {
            allIcons.forEach { item ->
                val request = ImageRequest.Builder(context)
                    .data(item.iconRes)
                    .size(Size.ORIGINAL)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.DISABLED)
                    .allowHardware(true)
                    .build()
                coil.ImageLoader.Builder(context).build().execute(request)
            }
        }
    }

    LaunchedEffect(toastLabel) {
        if (toastLabel != null) {
            toastVisible = true
            delay(1000.milliseconds)
            toastVisible = false
            delay(500.milliseconds)
            toastLabel = null
        }
    }

    val pageBg = MaterialTheme.colorScheme.background
    val isDark = isSystemInDarkTheme()

    val pageBackdrop = rememberLayerBackdrop {
        drawRect(pageBg)
        drawContent()
    }

    val contentBackdrop = rememberLayerBackdrop {
        drawRect(pageBg)
        drawContent()
    }

    val hlColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    val containerColor =
        if (!isDark) Color.White.copy(alpha = 0.72f)
        else Color(0xFF1C1C1E).copy(alpha = 0.55f)

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                // ── Glassmorphic top bar — backdrop untouched, shape + content modernized ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBackdrop(
                            backdrop = contentBackdrop,
                            shape = { RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp) },
                            shadow = null,
                            highlight = {
                                Highlight(
                                    width = 0.50.dp,
                                    blurRadius = 1.dp,
                                    alpha = 0.0f,
                                    style = HighlightStyle.Plain(color = hlColor)
                                )
                            },
                            effects = {
                                vibrancy()
                                blur(4f.dp.toPx())
                                lens(16f.dp.toPx(), 32f.dp.toPx())
                            },
                            onDrawSurface = { drawRect(containerColor)
                                drawRect(containerColor, blendMode = BlendMode.Hue)
                            }
                        )
                ) {
                    CenterAlignedTopAppBar(
                        title = {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "App Icons",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = (-0.5).sp
                                    )
                                )
                                // Pill-style count badge
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50.dp))
                                        .background(
                                            MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.18f else 0.10f)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${allIcons.size} styles",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            letterSpacing = 0.sp
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            // Modern pill-shaped back button
                            Box(
                                modifier = Modifier
                                    .padding(start = 12.dp)
                                    .clip(RoundedCornerShape(50.dp))
                                    .background(
                                        MaterialTheme.colorScheme.onBackground.copy(alpha = if (isDark) 0.10f else 0.07f)
                                    )
                                    .clickable(onClick = onBack)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBackIos,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                            titleContentColor = MaterialTheme.colorScheme.onBackground
                        )
                    )
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(pageBg)
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
                        .layerBackdrop(contentBackdrop)
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(
                            top = innerPadding.calculateTopPadding() + 12.dp,
                            bottom = 48.dp + innerPadding.calculateBottomPadding()
                        )
                    ) {
                        gridEntries.forEach { entry ->
                            when (entry) {
                                is GridEntry.Header -> {
                                    item(
                                        key = "header_${entry.title}",
                                        span = { GridItemSpan(3) }
                                    ) {
                                        SectionHeader(entry.title, entry.subtitle)
                                    }
                                }

                                is GridEntry.Item -> {
                                    item(key = entry.option.aliasName) {
                                        val isSelected = entry.option.aliasName == currentAlias
                                        val onClick = remember(entry.option.aliasName) {
                                            {
                                                currentAlias = entry.option.aliasName
                                                toastLabel = entry.option.label
                                                onSelected(entry.option.aliasName)
                                            }
                                        }
                                        IconGridItem(
                                            item = entry.option,
                                            isSelected = isSelected,
                                            onClick = onClick,
                                            backdrop = pageBackdrop,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating toast overlay
        IconAppliedToast(
            label = toastLabel ?: "",
            visible = toastVisible,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 28.dp)
        )
    }
}

@Composable
private fun IconAppliedToast(
    label: String,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    val translateY by animateFloatAsState(
        targetValue = if (visible) 0f else 120f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "toast_y"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "toast_alpha"
    )

    if (alpha == 0f && !visible) return

    Box(
        modifier = modifier
            .graphicsLayer {
                translationY = translateY.dp.toPx()
                this.alpha = alpha
            }
    ) {
        // Modern elongated pill toast with gradient tint
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.inverseSurface,
                            MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.92f)
                        )
                    )
                )
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Compact check badge
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.18f),
                modifier = Modifier.size(26.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.inverseOnSurface,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Text(
                text = "\"$label\" applied",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.sp
                ),
                color = MaterialTheme.colorScheme.inverseOnSurface
            )
        }
    }
}

// ── Section header: left-aligned category title + accent line ──────────────
@Composable
private fun SectionHeader(title: String, subtitle: String) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Vertical accent bar
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                primaryColor,
                                primaryColor.copy(alpha = 0.3f)
                            )
                        )
                    )
            )
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.2).sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 0.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.42f)
                )
            }
        }
    }
}

@Composable
private fun IconGridItem(
    item: IconOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    backdrop: Backdrop
) {
    val isDark = isSystemInDarkTheme()
    val progressAnimation = remember { Animatable(0f) }
    val animationScope = rememberCoroutineScope()
    val primaryColor = MaterialTheme.colorScheme.primary
    val context = LocalContext.current
    val containerColor =
        if (!isDark) Color.White.copy(alpha = 0.72f)
        else Color(0xFF1C1C1E).copy(alpha = 0.55f)

    val tileShape = RoundedCornerShape(24.dp)


    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp)
     ) {

        Box(modifier = Modifier.padding(3.dp)) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .drawBehind {
                            drawRoundRect(
                                brush = Brush.radialGradient(
                                    listOf(
                                        primaryColor.copy(alpha = 0.22f),
                                        primaryColor.copy(alpha = 0.05f)
                                    )
                                ),
                                cornerRadius = CornerRadius(28.dp.toPx())
                            )
                        }
                )
            }

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(tileShape)
                    .drawBackdrop(
                        backdrop = backdrop, // ✅ REAL backdrop
                        shape = { tileShape },
                        shadow = null,
                        effects = {
                            vibrancy()
                            blur(if (isDark) 0.dp.toPx() else 0.dp.toPx())
                            lens(30.dp.toPx(), 30.dp.toPx())
                        },
                        layerBlock = {
                            val progress = progressAnimation.value
                            val extra = 16.dp.toPx()
                            val maxScale =
                                if (size.width > 0f) (size.width + extra) / size.width else 1f
                            val scale = lerp(1f, maxScale, progress)
                            scaleX = scale
                            scaleY = scale
                        },
                        onDrawSurface = { drawRect(containerColor)
                            drawRect(containerColor, blendMode = BlendMode.Hue)
                        }
                    )

                    .drawBehind {
                        drawRoundRect(
                            color = if (isSelected) {
                                primaryColor
                            } else {
                                Color.White.copy(alpha = if (isDark) 0.10f else 0.22f)
                            },
                            cornerRadius = CornerRadius(24.dp.toPx()),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = if (isSelected) 2.dp.toPx() else 1.dp.toPx()
                            )
                        )
                    }
                    .pointerInput(onClick) {
                        val animationSpec = spring<Float>(
                            dampingRatio = 0.5f,
                            stiffness = 300f
                        )

                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)

                            animationScope.launch {
                                progressAnimation.animateTo(1f, animationSpec)
                            }

                            val up = waitForUpOrCancellation()

                            animationScope.launch {
                                progressAnimation.animateTo(0f, animationSpec)
                            }

                            if (up != null) onClick()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(item.iconRes)
                        .size(Size.ORIGINAL)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.DISABLED)
                        .allowHardware(true)
                        .crossfade(false)
                        .build(),
                    contentDescription = item.label,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(20.dp))
                )

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(7.dp),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(primaryColor, primaryColor.copy(alpha = 0.75f))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = item.label,
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.sp),
            color = if (isSelected) primaryColor
            else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

private data class IconOption(
    val label: String,
    val iconRes: Int,
    val aliasName: String
)