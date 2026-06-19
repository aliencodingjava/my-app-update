package com.flights.studio

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.edit
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh
import kotlin.random.Random

class BackdropPlaygroundActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // ✅ If user comes back from reset page, go directly to MainActivity
        if (isAuthLoginDeepLink(intent)) {
            navigateToMain(this, openLogin = true)
            return
        }

        if (isResetDoneDeepLink(intent)) {
            navigateToMain(this) // or openLogin=true if you want reset to go login
            return
        }


        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val isDark = isSystemInDarkTheme()
            val controller = remember {
                WindowCompat.getInsetsController(window, window.decorView)
            }
            SideEffect { controller.isAppearanceLightStatusBars = !isDark }

            BackdropPlaygroundScreen(
                onNavigateToMain = { navigateToMain(this) }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        if (isAuthLoginDeepLink(intent)) {
            navigateToMain(this, openLogin = true)
            return
        }

        if (isResetDoneDeepLink(intent)) {
            navigateToMain(this)
        }

    }
    private fun isResetDoneDeepLink(intent: Intent?): Boolean {
        val uri: Uri = intent?.data ?: return false
        return uri.scheme == "flightsstudio" && uri.host == "resetdone"
    }
    private fun isAuthLoginDeepLink(intent: Intent?): Boolean {
        val uri: Uri = intent?.data ?: return false
        return uri.scheme == "flightsstudio" && uri.host == "auth" && uri.path == "/login"
    }



}

// -------------------------------------------------------------
// Screen
// -------------------------------------------------------------

private sealed class SplashState {
    object Hidden : SplashState()
    object Initial : SplashState()
    object Dropping : SplashState()
    object Ready : SplashState()
}



@Composable
fun BackdropPlaygroundScreen(onNavigateToMain: () -> Unit) {
    // ✅ one scale value for this screen
    val uiTight = rememberUiScale()

    var splashState by remember { mutableStateOf<SplashState>(SplashState.Hidden) }

    val isReady = splashState == SplashState.Ready

    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val screenHeight = remember(windowInfo, density) {
        with(density) { windowInfo.containerSize.height.toDp() }
    }

    val buttonDiameter = 64.dp
    val heroTopPadding = 46.dp
    val heroHeight = 166.dp
    val glassEdgePadding = 14.dp
    val splashCornerRadius = 32.dp
    val bottomMargin = 24.dp

    val centerScreenY = screenHeight / 2
    val initialButtonCenterY = heroTopPadding + heroHeight + (buttonDiameter / 2)
    val finalButtonCenterY = screenHeight - bottomMargin - (buttonDiameter / 2)
    val initialOffsetY = initialButtonCenterY - centerScreenY
    val targetOffsetY = finalButtonCenterY - centerScreenY

    val enterProgressAnimation = remember { Animatable(0f) }
    val safeEnterProgressAnimation = remember { Animatable(0f) }
    val heroDragAnimation = remember { Animatable(0f) }
    val animationScope = rememberCoroutineScope()
    val heroDragRangePx = remember(density) {
        with(density) { 118.dp.toPx() }
    }
    val enterProgress by remember {
        derivedStateOf {
            liquidProgress(enterProgressAnimation.value)
        }
    }
    val safeEnterProgress by remember {
        derivedStateOf {
            safeEnterProgressAnimation.value.coerceIn(0f, 1f)
        }
    }
    val dropOvershoot = (enterProgress - 1f).coerceAtLeast(0f)
    val buttonOffsetY = lerp(initialOffsetY.value, targetOffsetY.value, enterProgress).dp
    val heroManualProgress by remember {
        derivedStateOf {
            liquidProgress(heroDragAnimation.value)
        }
    }
    val heroGlassProgress =
        1f +
                0.55f * sin((enterProgress.coerceIn(0f, 1f) * PI).toFloat()) +
                dropOvershoot +
                heroManualProgress
    val heroDragModifier = Modifier.draggable(
        orientation = Orientation.Vertical,
        state = rememberDraggableState { delta ->
            val target = (heroDragAnimation.value + delta / heroDragRangePx)
                .coerceIn(-0.45f, 1.85f)
            animationScope.launch {
                heroDragAnimation.snapTo(target)
            }
        },
        enabled = splashState != SplashState.Hidden,
        onDragStopped = { velocity ->
            animationScope.launch {
                heroDragAnimation.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = 0.46f,
                        stiffness = 52f
                    ),
                    initialVelocity = velocity / heroDragRangePx
                )
            }
        }
    )

    val breatheTransition = rememberInfiniteTransition(label = "button_breathe")

    val rawBreatheScale by breatheTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "button_breathe_value"
    )

    val breatheFactor by animateFloatAsState(
        targetValue = if (isReady) 1f else 0f,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "breathe_factor"
    )

    val arriveScale by animateFloatAsState(
        targetValue = when (splashState) {
            SplashState.Hidden -> 0.96f
            SplashState.Initial -> 1f
            SplashState.Dropping -> 1.015f
            SplashState.Ready -> 1f
        },
        animationSpec = spring(
            dampingRatio = 0.74f,
            stiffness = 120f
        ),
        label = "button_arrive_scale"
    )

    val buttonScale = arriveScale * (1f + (rawBreatheScale - 1f) * breatheFactor)

    LaunchedEffect(Unit) {
        splashState = SplashState.Hidden
        enterProgressAnimation.snapTo(0f)
        safeEnterProgressAnimation.snapTo(0f)
        heroDragAnimation.snapTo(0f)
        delay(80L)

        splashState = SplashState.Initial
        delay(120L)

        splashState = SplashState.Dropping
        launch {
            safeEnterProgressAnimation.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
            )
        }
        enterProgressAnimation.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.62f,
                stiffness = 24f
            )
        )

        splashState = SplashState.Ready
    }

    OpenSplashBackdropScaffold { backdrop ->
        Box(Modifier.fillMaxSize()) {
            LiquidGlassRoundIconButton(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset {
                        IntOffset(0, buttonOffsetY.roundToPx())
                    }
                    .padding(horizontal = glassEdgePadding)
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = safeEnterProgress
                        scaleX = buttonScale / (1f + 0.10f * dropOvershoot)
                        scaleY = buttonScale * (1f + 0.10f * dropOvershoot)
                        transformOrigin = TransformOrigin(0.5f, 0.5f)
                    },
                backdrop = backdrop,
                iconRes = R.drawable.newmainlogo,
                diameter = buttonDiameter,
                cornerRadius = splashCornerRadius,
                isInteractive = isReady,
                splashState = splashState,
                uiTight = uiTight,
                onClick = onNavigateToMain
            )

            SplashHeroBar(
                uiTight = uiTight,
                backdrop = backdrop,
                visible = splashState != SplashState.Hidden,
                glassProgress = heroGlassProgress,
                safeProgress = safeEnterProgress,
                cornerRadius = splashCornerRadius,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(3f)
                    .then(heroDragModifier)
                    .padding(top = heroTopPadding, start = glassEdgePadding, end = glassEdgePadding)
            )
        }
    }
}

// -------------------------------------------------------------
// Greeting text (scaled)
// -------------------------------------------------------------

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GreetingBlock(uiTight: Float) {
    val context = LocalContext.current

    val greeting by produceState(initialValue = "Loading…") {
        val prefs = context.getSharedPreferences("boot_prefs", Context.MODE_PRIVATE)
        val isFirstLaunch = prefs.getBoolean("first_launch", true)
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        val messages: List<String> = if (isFirstLaunch) {
            prefs.edit { putBoolean("first_launch", false) }

            listOf(
                "Welcome! Thanks for trying this app.",
                "Nice to meet you. Let’s get things ready.",
                "Welcome! Hope this helps you keep an eye on the airport.",
                "First time here. Take a moment to look around."
            )
        } else {
            when (hour) {
                in 5..10 -> listOf(
                    "Good morning. Hope your day starts calmly.",
                    "Good morning. A quick look before the day gets busy?",
                    "Morning already. Take a second to check what’s going on."
                )

                in 11..16 -> listOf(
                    "Good afternoon. Hope your day is going well.",
                    "Afternoon check-in. Just a quick look at things.",
                    "Good afternoon. Take a short break and have a look."
                )

                in 17..21 -> listOf(
                    "Good evening. How was your day?",
                    "Evening already. Time to slow down a bit.",
                    "Good evening. A quiet moment to check the airport."
                )

                else -> listOf(
                    "It’s late. Thanks for still being here.",
                    "Late hours. Hope you get some rest soon.",
                    "Quiet time. Perfect moment for a quick look."
                )
            }
        }

        value = messages.random(Random(System.currentTimeMillis()))
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val t1 = MaterialTheme.typography.titleMediumEmphasized
        Text(
            text = greeting,
            style = t1,
            fontSize = t1.fontSize.us(uiTight),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        val t2 = MaterialTheme.typography.labelSmall
        Text(
            text = stringResource(R.string.tap_to_enter),
            style = t2,
            fontSize = t2.fontSize.us(uiTight),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// -------------------------------------------------------------
// Liquid-glass button (scaled labels)
// -------------------------------------------------------------

@Composable
private fun LiquidGlassRoundIconButton(
    backdrop: LayerBackdrop,
    @DrawableRes iconRes: Int,
    splashState: SplashState,
    uiTight: Float,
    modifier: Modifier = Modifier,
    diameter: Dp = 84.dp,
    cornerRadius: Dp = 32.dp,
    isInteractive: Boolean = true,
    tint: Color = Color.Unspecified,
    surfaceColor: Color = Color.Unspecified,
    contentDescription: String? = null,
    onClick: () -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }
    val labelColor = if (isDark) Color.White else Color(0xFF111111)
    val iconFilter = remember(labelColor) { ColorFilter.tint(labelColor) }
    val glassTintAmount = rememberLiquidGlassTintAmount()
    val glassColor = bottomTabBarTintForAmount(glassTintAmount, isDark)
    val overlayTint = bottomTabBarOverlayTintForAmount(glassTintAmount, isDark)
    val backdropBlurDp = bottomChromeBackdropBlurDpForAmount(glassTintAmount, isDark)
    val logoSize = diameter * 0.76f


    Box(
        modifier
            .height(diameter)
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(cornerRadius) },
                shadow = null,
                highlight = {
                    if (isDark) {
                        Highlight(
                            width = 0.45.dp,
                            blurRadius = 1.6.dp,
                            alpha = 0.50f,
                            style = HighlightStyle.Plain
                        )
                    } else {
                        Highlight(
                            width = 0.65.dp,
                            blurRadius = 1.4.dp,
                            alpha = 0.95f,
                            style = HighlightStyle.Plain
                        )
                    }
                },
                effects = {
                    vibrancy()
                    blur(
                        radius = backdropBlurDp.dp.toPx(),
                        edgeTreatment = TileMode.Mirror
                    )
                    lens(
                        GlassChromeRefractionHeightDp.dp.toPx(),
                        GlassChromeRefractionAmountDp.dp.toPx(),
                        depthEffect = false
                    )
                },
                layerBlock = if (isInteractive) {
                    {
                        val width = size.width
                        val height = size.height
                        val progress = interactiveHighlight.pressProgress

                        val pressScaleDelta = 2.dp.toPx()
                        val dragScaleDelta = 2.dp.toPx()

                        val scale = lerp(1f, 1f + pressScaleDelta / size.height, progress)

                        val maxOffset = size.minDimension
                        val k = 0.05f
                        val offset = interactiveHighlight.offset

                        translationX = maxOffset * tanh(k * offset.x / maxOffset)
                        translationY = maxOffset * tanh(k * offset.y / maxOffset)

                        val maxDragScale = dragScaleDelta / size.height
                        val ang = atan2(offset.y, offset.x)

                        scaleX = scale +
                                maxDragScale *
                                abs(cos(ang) * offset.x / size.maxDimension) *
                                (width / height).fastCoerceAtMost(1f)

                        scaleY = scale +
                                maxDragScale *
                                abs(sin(ang) * offset.y / size.maxDimension) *
                                (height / width).fastCoerceAtMost(1f)
                    }
                } else null,
                onDrawSurface = {
                    if (surfaceColor.isSpecified) {
                        drawRect(surfaceColor)
                    } else {
                        drawRect(glassColor)
                        drawRect(overlayTint)
                    }
                },
                onDrawBackdrop = { drawBackdrop -> drawBackdrop() }
            )
            .then(
                if (isInteractive) {
                    Modifier
                        .then(interactiveHighlight.modifier)
                        .then(interactiveHighlight.gestureModifier)
                } else Modifier
            )
            .clickable(
                enabled = isInteractive,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = splashState == SplashState.Ready,
            enter = fadeIn(tween(durationMillis = 400)) + scaleIn(
                initialScale = 0.90f,
                animationSpec = tween(
                    durationMillis = 260,
                    easing = CubicBezierEasing(0.18f, 0.92f, 0.20f, 1.00f)
                )
            ),
            exit = fadeOut(tween(durationMillis = 180))
        ) {
            ScreenLabel(textRes = R.string.Enter_here, uiTight = uiTight)
        }

        Image(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            colorFilter = when {
                tint.isSpecified -> ColorFilter.tint(tint)
                else -> iconFilter
            },
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 14.dp)
                .size(logoSize)
        )
    }
}

@Composable
private fun ScreenLabel(@StringRes textRes: Int, uiTight: Float) {
    val base = MaterialTheme.typography.titleLarge
    Text(
        text = stringResource(id = textRes),
        style = base,
        fontSize = base.fontSize.us(uiTight),
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center
    )
}

//3 bands



fun Modifier.movingTripleBandPacket(
    durationMillis: Int = 2200,
    packetWidthFrac: Float = 0.30f,
    gapFrac: Float = 0.00f,
    disabledAlpha: Float = 0.34f,
    activeAlpha: Float = 0.52f,
    darkDisabledColor: Color = Color(0xFFB8C1CC),
    lightDisabledColor: Color = Color(0xFF7C8A99),
    darkActiveColor: Color = Color(0xFFE6ECF5),
    lightActiveColor: Color = Color(0xFF4F5D6B),
    compositingOffscreen: Boolean = false,
    runCount: Int = 1
): Modifier = composed {

    // Early return: if not enabled, don't attach any drawing/animation work

    // --- Input safety / clamps
    val packetWidthSafe = packetWidthFrac.coerceIn(0.05f, 1f)
    val gapSafe = gapFrac.coerceIn(0f, 0.5f)
    val durationSafe = durationMillis.coerceAtLeast(100)
    val disabledAlphaSafe = disabledAlpha.coerceIn(0f, 1f)
    val activeAlphaSafe = activeAlpha.coerceIn(0f, 1f)

    val isDark = isSystemInDarkTheme()

    // Animatable stored in remember so it survives recomposition
    val anim = remember { Animatable(0f) }

    // Keep latest runCount if caller changes it while coroutine is running
    val runCountState = rememberUpdatedState(runCount.coerceAtLeast(1))

    // Scope the animation to the enabled flag so it starts/stops when enabled toggles.
    LaunchedEffect(Unit) {
        anim.snapTo(0f)

        repeat(runCountState.value) {
            anim.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = durationSafe + 300,
                    easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)
                )
            )

            delay(700)

            anim.snapTo(0f)

            anim.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = (durationSafe - 250).coerceAtLeast(100),
                    easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1f)
                )
            )

            anim.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 250,
                    easing = LinearEasing
                )
            )

            if (it != runCountState.value - 1) delay(120)
        }
    }

    val t = anim.value

    // Choose compositing strategy based on flag; Offscreen can be expensive.
    val compositingModifier = if (compositingOffscreen) {
        graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
    } else {
        graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Auto }
    }

    compositingModifier.drawWithCache {
        val w = size.width
        val h = size.height

        // packet and band geometry
        val packetW = w * packetWidthSafe
        val bandW = packetW / 3f
        val gap = packetW * gapSafe

        // extra distance so bands fully exit/enter
        val exitExtra = bandW * 2f
        val x = -packetW - exitExtra + (w + packetW + exitExtra * 2f) * t

        // color selection with theme-aware defaults
        val disabledColor = if (isDark)
            darkDisabledColor.copy(alpha = disabledAlphaSafe)
        else
            lightDisabledColor.copy(alpha = (disabledAlphaSafe * 2.4f).coerceAtMost(1f))

        val activeColor = if (isDark)
            darkActiveColor.copy(alpha = activeAlphaSafe)
        else
            lightActiveColor.copy(alpha = (activeAlphaSafe * 2.0f).coerceAtMost(1f))

        onDrawWithContent {
            drawContent()

            clipRect {
                withTransform({
                    // rotate around center to create diagonal sweep
                    rotate(22f, pivot = Offset(w / 2f, h / 2f))
                }) {
                    val diagonal = sqrt(w * w + h * h)
                    val top = -diagonal
                    val tall = diagonal * 2f

                    var cx = x
                    // left disabled band
                    drawRect(disabledColor, topLeft = Offset(cx, top), size = androidx.compose.ui.geometry.Size(bandW, tall), blendMode = BlendMode.Overlay)
                    cx += bandW + gap
                    // center active band
                    drawRect(activeColor, topLeft = Offset(cx, top), size = androidx.compose.ui.geometry.Size(bandW, tall), blendMode = BlendMode.Overlay)
                    cx += bandW + gap
                    // right disabled band
                    drawRect(disabledColor, topLeft = Offset(cx, top), size = androidx.compose.ui.geometry.Size(bandW, tall), blendMode = BlendMode.Overlay)
                }
            }
        }
    }
}


@Composable
private fun SplashHeroBar(
    uiTight: Float,
    backdrop: LayerBackdrop,
    visible: Boolean,
    glassProgress: Float,
    safeProgress: Float,
    cornerRadius: Dp,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current.applicationContext
    val glassTintAmount = rememberLiquidGlassTintAmount()
    val glassColor = bottomTabBarTintForAmount(glassTintAmount, isDark)
    val overlayTint = bottomTabBarOverlayTintForAmount(glassTintAmount, isDark)
    val backdropBlurDp = bottomChromeBackdropBlurDpForAmount(glassTintAmount, isDark)

    val calendar = remember { Calendar.getInstance() }
    val hour = remember(calendar) { calendar.get(Calendar.HOUR_OF_DAY) }
    val hasSeenHero = remember(context) {
        context.getSharedPreferences("boot_prefs", Context.MODE_PRIVATE)
            .getBoolean("splash_hero_seen", false)
    }
    val heroTitle = remember(hasSeenHero, hour) {
        when {
            hasSeenHero -> "Welcome back"
            hour in 5..10 -> "Good morning"
            hour in 11..16 -> "Good afternoon"
            hour in 17..21 -> "Good evening"
            else -> "Welcome"
        }
    }
    val smartLine = remember(hasSeenHero, hour) {
        val messages = if (hasSeenHero) {
            when (hour) {
                in 5..10 -> listOf(
                    "I’ve got your morning view ready.",
                    "Let’s ease into the day together.",
                    "A fresh check-in is ready when you are."
                )
                in 11..16 -> listOf(
                    "Your afternoon snapshot is waiting.",
                    "A quick look, then back to your day.",
                    "I’ll keep things clear and easy to scan."
                )
                in 17..21 -> listOf(
                    "Let’s catch up on what changed today.",
                    "A calmer evening view is ready.",
                    "I saved you a quiet place to check in."
                )
                else -> listOf(
                    "Quiet mode is ready.",
                    "I’ll keep the late check-in simple.",
                    "A soft landing for a quick look."
                )
            }
        } else {
            when (hour) {
                in 5..10 -> listOf(
                    "Let’s start with a calm, clear view.",
                    "I’ll help keep the morning easy to scan.",
                    "Everything you need is just ahead."
                )
                in 11..16 -> listOf(
                    "Let’s make the first look simple.",
                    "A clear afternoon view is ready.",
                    "I’ll keep the important things close."
                )
                in 17..21 -> listOf(
                    "Settle in. I’ll keep the view clean.",
                    "A softer way to check the day.",
                    "Let’s make this first visit feel easy."
                )
                else -> listOf(
                    "A quiet first look is ready.",
                    "I’ll keep everything gentle and clear.",
                    "No rush. Start wherever you like."
                )
            }
        }
        val dayIndex = calendar.get(Calendar.DAY_OF_YEAR).coerceAtLeast(0)
        messages[dayIndex % messages.size]
    }

    LaunchedEffect(context) {
        context.getSharedPreferences("boot_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("splash_hero_seen", true)
            .apply()
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "heroAlpha"
    )

    val lift by animateDpAsState(
        targetValue = if (visible) 0.dp else 10.dp,
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = 120f
        ),
        label = "heroLift"
    )
    val glassStretch = (glassProgress - 1f).coerceAtLeast(0f)
    val heroShape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .offset {
                IntOffset(0, lift.roundToPx())
            }
            .fillMaxWidth()
            .graphicsLayer {
                translationY = -48.dp.toPx() * (1f - glassProgress)
                this.alpha = alpha * safeProgress
                scaleX = 1f / (1f + 0.1f * glassStretch)
                scaleY = 1f + 0.1f * glassStretch
                transformOrigin = TransformOrigin(0.5f, 0.5f)
                clip = false
            }
            .height(166.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { heroShape },
                shadow = { bottomChromeShadow() },
                highlight = {
                    if (isDark) {
                        Highlight(
                            width = 0.45.dp,
                            blurRadius = 1.6.dp,
                            alpha = 0.50f,
                            style = HighlightStyle.Plain
                        )
                    } else {
                        Highlight(
                            width = 0.30.dp,
                            blurRadius = 1.0.dp,
                            alpha = 0.35f,
                            style = HighlightStyle.Plain
                        )
                    }
                },
                effects = {
                    vibrancy()
                    blur(
                        radius = (backdropBlurDp * safeProgress).dp.toPx(),
                        edgeTreatment = TileMode.Mirror
                    )
                    lens(
                        (GlassChromeRefractionHeightDp * safeProgress).dp.toPx(),
                        (GlassChromeRefractionAmountDp * safeProgress).dp.toPx(),
                        depthEffect = false
                    )
                },
                onDrawSurface = {
                    drawRect(glassColor)
                    drawRect(overlayTint)
                },
                onDrawBackdrop = { drawBackdrop -> drawBackdrop() }
            )
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // MAIN TITLE
                val title = MaterialTheme.typography.headlineMedium
                Text(
                    text = heroTitle,
                    style = title,
                    fontSize = title.fontSize.us(uiTight),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                // SUBTITLE
                Text(
                    text = if (hasSeenHero) "Good to see you again" else "Let’s get you settled",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp)
                )

                // SMART LINE
                @OptIn(ExperimentalAnimationApi::class)
                AnimatedContent(
                    targetState = smartLine,
                    transitionSpec = {
                        (fadeIn(tween(350)) + slideInVertically { it / 4 }) togetherWith
                                (fadeOut(tween(250)) + slideOutVertically { -it / 4 })
                    },
                    label = "SmartLineTransition"
                ) { targetText ->
                    Text(
                        text = targetText,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
    }
}



// -------------------------------------------------------------
// Navigation
// -------------------------------------------------------------

private fun navigateToMain(activity: Activity, openLogin: Boolean = false) {
    val intent = Intent(activity, MainActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        putExtra(EXTRA_OPEN_LOGIN, openLogin)
    }
    val options = ActivityOptionsCompat.makeCustomAnimation(
        activity,
        R.anim.enter_animation,
        R.anim.exit_animation
    )

    activity.startActivity(intent, options.toBundle())
    activity.finish()
}

private fun liquidProgress(value: Float): Float {
    return when {
        value < 0f -> -liquidProgressResistance(-value)
        value <= 1f -> value
        else -> 1f + liquidProgressResistance(value - 1f)
    }
}

private fun liquidProgressResistance(value: Float): Float {
    val v = value.coerceAtLeast(0f)
    return v / (1f + v)
}
