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
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import kotlinx.coroutines.delay
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh
import kotlin.random.Random

class BackdropPlaygroundActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
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
    val dropToBottom = splashState == SplashState.Dropping || splashState == SplashState.Ready

    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val screenHeight = remember(windowInfo, density) {
        with(density) { windowInfo.containerSize.height.toDp() }
    }

    val buttonDiameter = 64.dp
    val bottomMargin = 24.dp
    val dropDurationMs = 750

    val centerScreenY = screenHeight / 2
    val finalButtonCenterY = screenHeight - bottomMargin - (buttonDiameter / 2)
    val targetOffsetY = finalButtonCenterY - centerScreenY

    val buttonOffsetY by animateDpAsState(
        targetValue = if (dropToBottom) targetOffsetY else 0.dp,
        animationSpec = tween(durationMillis = dropDurationMs, easing = FastOutSlowInEasing),
        label = "button_drop_offset"
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
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "breathe_factor"
    )

    val buttonScale = 1f + (rawBreatheScale - 1f) * breatheFactor

    LaunchedEffect(Unit) {
        splashState = SplashState.Hidden
        delay(80L)

        splashState = SplashState.Initial
        delay(1600L)

        splashState = SplashState.Dropping
        delay(dropDurationMs.toLong() + 100L)

        splashState = SplashState.Ready
    }

    OpenSplashBackdropScaffold { backdrop ->
        Box(Modifier.fillMaxSize()) {
            SplashHeroBar(
                uiTight = uiTight,
                backdrop = backdrop,
                visible = splashState != SplashState.Hidden,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 46.dp, start = 20.dp, end = 20.dp)
            )
            AnimatedVisibility(
                visible = isReady,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = bottomMargin + buttonDiameter + 12.dp)
            ) {
                GreetingBlock(uiTight = uiTight)
            }

            LiquidGlassRoundIconButton(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = buttonOffsetY)
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .scale(buttonScale),
                backdrop = backdrop,
                iconRes = R.drawable.newmainlogo,
                diameter = buttonDiameter,
                isInteractive = isReady,
                splashState = splashState,
                uiTight = uiTight,
                onClick = onNavigateToMain
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


    Box(
        modifier
            .height(diameter)
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(50.dp) },
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
                            width = 0.30.dp,
                            blurRadius = 1.0.dp,
                            alpha = 0.35f,
                            style = HighlightStyle.Plain // very subtle
                        )
                    }
                },
                effects = {
                    vibrancy()
                    blur(2f.dp.toPx())
                    lens(24f.dp.toPx(), 24f.dp.toPx())
                    colorControls(
                        brightness = 0.0f,
                        contrast = 1.0f,
                        saturation = 1.9f
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
                        drawRect(
                            if (isDark) Color.White.copy(alpha = 0.10f)
                            else Color.White.copy(alpha = 0.28f)
                        )
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
            visible = splashState == SplashState.Initial,
            enter = fadeIn(tween(durationMillis = 450)) + scaleIn(
                initialScale = 0.92f,
                animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing)
            ),
            exit = fadeOut(tween(durationMillis = 200))
        ) {
            ScreenLabel(textRes = R.string.welcome_to_jac_airport, uiTight = uiTight)
        }

        AnimatedVisibility(
            visible = splashState == SplashState.Ready,
            enter = fadeIn(tween(durationMillis = 400)) + scaleIn(
                initialScale = 0.96f,
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
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
                .padding(start = 4.dp)
                .size(99.dp)
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
    enabled: Boolean,                 // TUNE here!!!
    durationMillis: Int = 2200,      // speed
    packetWidthFrac: Float = 0.30f, // wide
    gapFrac: Float = 0.00f,        // gap
    disabledAlpha: Float = 0.08f, // sides alpha
    activeAlpha: Float = 0.12f,  // center alpha
): Modifier = composed {

    if (!enabled) return@composed this

    val isDark = isSystemInDarkTheme()

    val anim = remember { androidx.compose.animation.core.Animatable(0f) }

    LaunchedEffect(Unit) {

        // 🌊 First pass — calm glide
        anim.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = durationMillis + 300,
                easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)
            )
        )

        delay(700) // tighter pause feels more premium

        anim.snapTo(0f)

        // ⚡ Second pass — confident sweep
        anim.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = durationMillis - 250,
                easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1f)
            )
        )

        // 🧊 Soft settle (micro ease-out)
        anim.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 250,
                easing = LinearEasing
            )
        )
    }

    val t = anim.value

    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithCache {

            val w = size.width
            val h = size.height
            val packetW = w * packetWidthFrac
            val bandW = packetW / 3f
            val gap = packetW * gapFrac

            val x = -packetW + (w + packetW) * t

            val disabled = if (isDark)
                Color(0xFFB8C1CC).copy(alpha = disabledAlpha)
            else
                Color(0xFF7C8A99).copy(alpha = disabledAlpha * 2.4f)

            val active = if (isDark)
                Color(0xFFE6ECF5).copy(alpha = activeAlpha)
            else
                Color(0xFF4F5D6B).copy(alpha = activeAlpha * 2.0f)

            onDrawWithContent {
                drawContent()
                clipRect {
                    withTransform({
                        rotate(22f, pivot = Offset(w / 2f, h / 2f))
                    }) {
                        val diagonal = kotlin.math.sqrt(w * w + h * h)
                        val top = -diagonal
                        val tall = diagonal * 2f

                        var cx = x
                        drawRect(disabled, Offset(cx, top), Size(bandW, tall), blendMode = BlendMode.Overlay)
                        cx += bandW + gap
                        drawRect(active, Offset(cx, top), Size(bandW, tall), blendMode = BlendMode.Overlay)
                        cx += bandW + gap
                        drawRect(disabled, Offset(cx, top), Size(bandW, tall), blendMode = BlendMode.Overlay)
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
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val smartLine = remember(hour) {
        when (hour) {
            in 5..10 -> "Morning departures"
            in 11..16 -> "Midday flow"
            in 17..21 -> "Evening arrivals"
            else -> "Quiet runway hours"
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "heroAlpha"
    )

    val lift by animateDpAsState(
        targetValue = if (visible) 0.dp else 10.dp,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "heroLift"
    )
    val heroShape = RoundedCornerShape(28.dp)
    Box(
        modifier = modifier
            .offset(y = lift)
            .fillMaxWidth()
            .alpha(alpha)
            .height(130.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { heroShape  },
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
                            width = 0.30.dp,
                            blurRadius = 1.0.dp,
                            alpha = 0.35f,
                            style = HighlightStyle.Plain // very subtle
                        )
                    }
                },
                effects = {
                    vibrancy()
                    blur(if (isDark) 6.dp.toPx() else 3.dp.toPx())
                    lens(12.dp.toPx(), 60.dp.toPx(), depthEffect = true)
                },
                onDrawSurface = {
                    drawRect(
                        if (isDark)
                            Color.White.copy(alpha = 0.08f)
                        else
                            Color.White.copy(alpha = 0.22f)
                    )
                }
            )
            .clip(heroShape)
            .movingTripleBandPacket(
                enabled = true,
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
                text = "Flights Studio",
                style = title,
                fontSize = title.fontSize.us(uiTight),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            // SUBTITLE
            Text(
                text = "Jackson Hole • JAC",
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

