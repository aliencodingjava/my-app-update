package com.flights.studio

import android.app.Activity
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
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
import java.util.Calendar
import kotlin.math.atan2
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh
import kotlinx.coroutines.delay
import kotlin.random.Random

class BackdropPlaygroundActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Make app content go edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val isDark = isSystemInDarkTheme()
            val controller = remember {
                WindowCompat.getInsetsController(window, window.decorView)
            }
            SideEffect {
                controller.isAppearanceLightStatusBars = !isDark
            }

            BackdropPlaygroundScreen(
                onNavigateToMain = {
                    navigateToMain(this)
                }
            )
        }
    }
}

// -------------------------------------------------------------
// Screen
// -------------------------------------------------------------

/**
 * Defines the animation phases for the splash screen.
 */
private sealed class SplashState {
    object Hidden : SplashState()    // NEW: nothing shown yet, used to trigger first fade-in
    object Initial : SplashState()   // Center, label "Welcome to JAC"
    object Dropping : SplashState()  // Animating down
    object Ready : SplashState()     // At bottom, label "Tap to enter"
}

@Composable
fun BackdropPlaygroundScreen(onNavigateToMain: () -> Unit) {
    // --- State Management ---
    var splashState by remember { mutableStateOf<SplashState>(SplashState.Hidden) }

    // --- Derived State ---
    val isReady = splashState == SplashState.Ready
    val dropToBottom = splashState == SplashState.Dropping || splashState == SplashState.Ready

    // --- Animation & Layout Constants ---
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val buttonDiameter = 64.dp
    val bottomMargin = 24.dp
    val dropDurationMs = 750

    val centerScreenY = screenHeight / 2
    val finalButtonCenterY = screenHeight - bottomMargin - (buttonDiameter / 2)
    val targetOffsetY = finalButtonCenterY - centerScreenY

    // --- Drop animation ---
    val buttonOffsetY by animateDpAsState(
        targetValue = if (dropToBottom) targetOffsetY else 0.dp,
        animationSpec = tween(
            durationMillis = dropDurationMs,
            easing = FastOutSlowInEasing
        ),
        label = "button_drop_offset"
    )

    // --- Breathing scale animation ---
    val breatheTransition = rememberInfiniteTransition(label = "button_breathe")

    // Raw breathing between 0.96x and 1.04x, always running
    val rawBreatheScale by breatheTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "button_breathe_value"
    )

    // This animates from 0 → 1 when the splash becomes Ready
    val breatheFactor by animateFloatAsState(
        targetValue = if (isReady) 1f else 0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "breathe_factor"
    )

    // Final scale: 1f before Ready, then smoothly transitions into breathing
    val buttonScale = 1f + (rawBreatheScale - 1f) * breatheFactor

    // --- Animation Scheduler ---
    LaunchedEffect(Unit) {
        // 0) Start hidden so AnimatedVisibility can play enter animation
        splashState = SplashState.Hidden

        // Small delay so first composition is done
        delay(80L)

        // 1) Center, fade/scale-in "Welcome to JAC Airport"
        splashState = SplashState.Initial
        delay(1600L)

        // 2) Drop down
        splashState = SplashState.Dropping
        delay(dropDurationMs.toLong() + 100L)

        // 3) At bottom, swap label to "Tap to enter"
        splashState = SplashState.Ready
    }

    OpenSplashBackdropScaffold { backdrop ->
        Box(Modifier.fillMaxSize()) {
            // Greeting appears above the final pill only when Ready
            AnimatedVisibility(
                visible = isReady,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = bottomMargin + buttonDiameter + 12.dp)
            ) {
                GreetingBlock()
            }

            // Liquid-glass pill
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
                onClick = onNavigateToMain
            )
        }
    }
}

// -------------------------------------------------------------
// Greeting text
// -------------------------------------------------------------

@Composable
private fun GreetingBlock() {
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
        Text(
            text = greeting,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = stringResource(R.string.tap_to_enter),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// -------------------------------------------------------------
// Liquid-glass button
// -------------------------------------------------------------

@Composable
private fun LiquidGlassRoundIconButton(
    backdrop: LayerBackdrop,
    @DrawableRes iconRes: Int,
    splashState: SplashState,
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

                        val scale =
                            lerp(1f, 1f + pressScaleDelta / size.height, progress)

                        val maxOffset = size.minDimension
                        val k = 0.05f
                        val offset = interactiveHighlight.offset

                        translationX =
                            maxOffset * tanh(k * offset.x / maxOffset)
                        translationY =
                            maxOffset * tanh(k * offset.y / maxOffset)

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
                            if (isDark) {
                                Color.White.copy(alpha = 0.10f)
                            } else {
                                Color.White.copy(alpha = 0.28f)
                            }
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
                } else {
                    Modifier
                }
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
        // Initial label: "Welcome to JAC Airport"
        AnimatedVisibility(
            visible = splashState == SplashState.Initial,
            enter = fadeIn(
                animationSpec = tween(durationMillis = 450)
            ) + scaleIn(
                initialScale = 0.92f,
                animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing)
            ),
            exit = fadeOut(
                animationSpec = tween(durationMillis = 200)
            )
        ) {
            ScreenLabel(textRes = R.string.welcome_to_jac_airport)
        }

        // Ready label: "Tap to enter"
        AnimatedVisibility(
            visible = splashState == SplashState.Ready,
            enter = fadeIn(
                animationSpec = tween(durationMillis = 400)
            ) + scaleIn(
                initialScale = 0.96f,
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
            ),
            exit = fadeOut(
                animationSpec = tween(durationMillis = 180)
            )
        ) {
            ScreenLabel(textRes = R.string.Enter_here)
        }

        // Logo
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
private fun ScreenLabel(@StringRes textRes: Int) {
    Text(
        text = stringResource(id = textRes),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center
    )
}

// -------------------------------------------------------------
// Navigation
// -------------------------------------------------------------

private fun navigateToMain(activity: Activity) {
    val intent = Intent(activity, MainActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
    val options = ActivityOptionsCompat.makeCustomAnimation(
        activity,
        android.R.anim.fade_in,
        android.R.anim.fade_out
    )
    activity.startActivity(intent, options.toBundle())
    activity.finish()
}
