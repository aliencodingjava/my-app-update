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
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
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

    Box(
        modifier
            .height(diameter)
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(50.dp) },
                effects = {
                    vibrancy()

                    val blurPx = if (isDark) 4.dp.toPx() else 0.dp.toPx()
                    blur(blurPx)

                    lens(
                        refractionHeight = 8.dp.toPx(),
                        refractionAmount = 48.dp.toPx(),
                        depthEffect = true,
                        chromaticAberration = false
                    )

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
                !isDark -> ColorFilter.tint(Color.Black)
                else -> null
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

