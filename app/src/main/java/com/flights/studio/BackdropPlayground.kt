package com.flights.studio

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import androidx.core.app.ActivityOptionsCompat
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh
import kotlinx.coroutines.delay

class BackdropPlaygroundActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BackdropPlaygroundScreen() }
    }
}

@Composable
fun BackdropPlaygroundScreen() {
    val ctx = androidx.compose.ui.platform.LocalContext.current

    // Becomes true after boot sequence finishes
    var isReady by remember { mutableStateOf(false) }

    // Shared state for engine and text
    var engineAngle by remember { mutableFloatStateOf(0f) }
    var statusIndex by remember { mutableIntStateOf(0) }

    // HUD weather text
    var weatherText by remember { mutableStateOf<String?>(null) }

    // Gentle breathing for the bottom button (used after ready)
    val breatheTransition = rememberInfiniteTransition(label = "button_breathe")
    val buttonScale by breatheTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "button_breathe_value"
    )

    // 📡 Smooth radar rotation (background halo)
    val radarTransition = rememberInfiniteTransition(label = "radar_rotation")
    val radarAngle by radarTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_angle_value"
    )

    // 🟣 Smooth, slow orbit for the two dots (independent)
    val orbitTransition = rememberInfiniteTransition(label = "orbit_rotation")
    val orbitAngle by orbitTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_angle_value"
    )

    // Single timeline: drives both text + engine rotation (boot)
    LaunchedEffect(Unit) {
        val messagesCount = 4
        val phaseDurationMs = 2200L       // each message duration
        val frameMs = 16L                 // ~60fps
        val baseSpeed = 270f              // deg/sec

        var speed = baseSpeed

        for (phase in 0 until messagesCount) {
            statusIndex = phase
            var elapsed = 0L

            while (elapsed < phaseDurationMs) {
                val dt = frameMs / 1000f

                // Last phase: smoothly decelerate
                if (phase == messagesCount - 1) {
                    val t = elapsed / phaseDurationMs.toFloat()    // 0..1
                    val eased = 1f - (t * t)                       // ease-out
                    speed = baseSpeed * eased
                }

                engineAngle = (engineAngle + speed * dt) % 360f

                delay(frameMs)
                elapsed += frameMs
            }
        }

        // Finished all phases
        isReady = true
    }

    // 🔥 Fetch real weather once at splash start (your logic can go here)
    OpenSplashBackdropScaffold { backdrop ->
        Box(Modifier.fillMaxSize()) {

            // Central AI engine + HUD + orbiting dots + radar
            EngineWithHud(
                backdrop = backdrop,
                rotationAngle = engineAngle,   // HUD / engine rotation (boot sequence)
                radarAngle = radarAngle,       // radar halo
                orbitAngle = orbitAngle,       // two orbs (slow & smooth)
                weatherText = weatherText,
                modifier = Modifier.align(Alignment.Center)
            )

            // Status text under the lens – read from shared index
            AiStatusText(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 170.dp),
                index = statusIndex
            )

            // Tiny AI console log under status (only while NOT ready)
            AnimatedVisibility(
                visible = !isReady,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 150.dp)
            ) {
                AiMicroLog()
            }

            // Bottom area: greeting + glass button
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Dynamic greeting line, only after ready
                AnimatedVisibility(visible = isReady) {
                    val isDark = isSystemInDarkTheme()

                    val greeting = remember {
                        val hour = java.util.Calendar.getInstance()
                            .get(java.util.Calendar.HOUR_OF_DAY)
                        when (hour) {
                            in 5..8 -> "Good morning, captain — engines warming up."
                            in 9..11 -> "Good morning, captain — Jack reporting for duty."
                            in 12..16 -> "Good afternoon, ready for departures?"
                            in 17..20 -> "Good evening, checking runway visibility."
                            else -> "Late shift, captain — systems on standby."
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // main greeting
                        Text(
                            text = greeting,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            color = if (isDark) Color(0xFFCCCCCC) else Color(0xFF333333),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )

                        // small hint under greeting
                        Text(
                            text = "Tap below to enter Flight Studio",
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            color = if (isDark) Color(0xFFAAAAAA) else Color(0xFF555555),
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }

                // Bottom liquid glass "Tap to enter" bar
                LiquidGlassRoundIconButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(if (isReady) buttonScale else 1f),  // breathe only after ready
                    backdrop = backdrop,
                    iconRes = R.drawable.newmainlogo,
                    diameter = 64.dp, // height of the bar
                    isInteractive = isReady,                    // disabled until ready
                    onClick = { navigateToMain(ctx) }
                )
            }
        }
    }
}

/**
 * Wraps the glass lens + engine with HUD ring + orbiting AI dots + radar halo.
 */
@Composable
private fun EngineWithHud(
    backdrop: LayerBackdrop,
    rotationAngle: Float,   // engine / HUD motion (boot)
    radarAngle: Float,      // radar halo rotation
    orbitAngle: Float,      // two orbs slow orbit
    weatherText: String?,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    Box(
        modifier = modifier.size(240.dp),
        contentAlignment = Alignment.Center
    ) {
        // Scanning radar halo behind everything (smooth)
        RadarScanHalo(
            angle = radarAngle,
            modifier = Modifier.fillMaxSize()
        )

        // Orbiting AI "neuron" dots (own smooth orbit)
        OrbitingDots(angle = orbitAngle)

        // Outer HUD ring
        Box(
            Modifier
                .size(220.dp)
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(110.dp)
                )
        )

        // HUD label: real weather text if loaded, otherwise fallback
        val hudText = weatherText ?: "METAR  •  VISUAL  •  LIVE CAM"

        Text(
            text = hudText,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            color = if (isDark) {
                Color.White.copy(alpha = 0.6f)
            } else {
                Color(0xFF222222)      // dark text in light theme
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-20).dp)   // higher above ring
        )

        // Inner glass lens with rotating engine (boot sequence)
        SplashAiLens(
            backdrop = backdrop,
            rotationAngle = rotationAngle
        )
    }
}

/**
 * Small orbiting dots around the engine to hint "AI activity".
 */
@Composable
private fun OrbitingDots(
    angle: Float,
    modifier: Modifier = Modifier
) {
    val radius = 110.dp

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // main dot
        run {
            val angleRad = angle * (PI / 180f).toFloat()
            val offsetX = (radius.value * cos(angleRad)).dp
            val offsetY = (radius.value * sin(angleRad)).dp

            Box(
                Modifier
                    .offset(x = offsetX, y = offsetY)
                    .size(8.dp)
                    .background(
                        color = Color(0xFF4DD0E1).copy(alpha = 0.85f),
                        shape = RoundedCornerShape(50)
                    )
            )
        }

        // secondary dot opposite side
        run {
            val angleRad = (angle + 180f) * (PI / 180f).toFloat()
            val offsetX = (radius.value * cos(angleRad)).dp
            val offsetY = (radius.value * sin(angleRad)).dp

            Box(
                Modifier
                    .offset(x = offsetX, y = offsetY)
                    .size(5.dp)
                    .background(
                        color = Color(0xFFAB47BC).copy(alpha = 0.75f),
                        shape = RoundedCornerShape(50)
                    )
            )
        }
    }
}

@Composable
private fun RadarScanHalo(
    angle: Float,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.025f
        val radius = size.minDimension / 2f - strokeWidth * 1.5f

        // Soft outer circle
        drawCircle(
            color = if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.06f),
            style = Stroke(width = strokeWidth)
        )

        // Faint inner circle
        drawCircle(
            color = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.04f),
            radius = radius * 0.7f,
            style = Stroke(width = strokeWidth * 0.7f)
        )

        // Rotating sweep arc (the “scanner”)
        rotate(angle) { // smooth 0..360 from animation
            drawArc(
                color = Color(0xFF4DD0E1).copy(alpha = if (isDark) 0.50f else 0.35f),
                startAngle = -40f,
                sweepAngle = 80f,
                useCenter = false,
                style = Stroke(
                    width = strokeWidth * 1.6f,
                    cap = StrokeCap.Round
                )
            )
        }
    }
}

@Composable
private fun SplashAiLens(
    backdrop: LayerBackdrop,
    rotationAngle: Float,
    modifier: Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme()

    Box(
        modifier
            .size(200.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(100.dp) },
                effects = {
                    vibrancy()
                    blur(8.dp.toPx())
                    lens(
                        refractionHeight = 22.dp.toPx(),     // deeper distortion
                        refractionAmount = 80.dp.toPx(),
                        depthEffect = true,
                        chromaticAberration = true
                    )
                    colorControls(
                        brightness = if (isDark) 0.08f else 0.02f,
                        contrast = 1.05f,
                        saturation = 1.3f
                    )
                },
                onDrawSurface = {
                    // subtle glass gloss
                    val topLight = if (isDark)
                        Color.White.copy(alpha = 0.06f)
                    else
                        Color.White.copy(alpha = 0.18f)
                    val bottomShade = Color.Black.copy(alpha = 0.08f)
                    drawRect(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            0f to topLight,
                            1f to bottomShade
                        )
                    )
                },
                onDrawBackdrop = { drawBackdrop -> drawBackdrop() }
            ),
        contentAlignment = Alignment.Center
    ) {
        // Rotating engine inside glass (boot sequence)
        Image(
            painter = painterResource(R.drawable.jetengine125),
            contentDescription = "Jet turbine",
            modifier = Modifier
                .size(180.dp)
                .graphicsLayer {
                    rotationZ = -rotationAngle
                }
        )

        // ✨ Inner light reflection overlay
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer {
                    alpha = 0.18f
                }
        ) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 10.dp)
                    .size(120.dp, 40.dp)
                    .graphicsLayer { rotationZ = -10f }
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.45f),
                                Color.Transparent
                            )
                        ),
                        shape = RoundedCornerShape(50)
                    )
            )
        }
    }
}

/**
 * Status text driven by shared index.
 */
@Composable
private fun AiStatusText(
    modifier: Modifier = Modifier,
    index: Int
) {
    val isDark = isSystemInDarkTheme()
    val messages = listOf(
        "Analyzing sky traffic data…",
        "Preparing live runway cameras…",
        "Syncing notes and flight logs…",
        "System ready. Tap to enter."
    )

    val safeIndex = index.coerceIn(messages.indices)

    Text(
        text = messages[safeIndex],
        fontSize = 18.sp,
        textAlign = TextAlign.Center,
        color = if (isDark) Color(0xFFCCCCCC) else Color(0xFF222222),
        fontWeight = FontWeight.Medium,
        modifier = modifier
            .padding(horizontal = 32.dp)
    )
}

/**
 * Tiny AI "console log" under the main status text.
 * Visible only while !isReady (wrapped in AnimatedVisibility).
 */
@Composable
private fun AiMicroLog(
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val logs = listOf(
        "[OK] Flight graph loaded",
        "[OK] Camera matrix synced",
        "[OK] Runway map cached",
        "[OK] Notebook index ready",
        "[OK] User notes attached"
    )

    var idx by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(650)
            idx = (idx + 1) % logs.size
        }
    }

    Text(
        text = logs[idx],
        fontSize = 11.sp,
        textAlign = TextAlign.Center,
        color = if (isDark) Color(0xFF888888) else Color(0xFF555555),
        fontWeight = FontWeight.Light,
        modifier = modifier
    )
}

/**
 * SAME name kept: LiquidGlassRoundIconButton
 */
@Composable
fun LiquidGlassRoundIconButton(
    backdrop: LayerBackdrop,
    @DrawableRes iconRes: Int,
    modifier: Modifier = Modifier,
    diameter: Dp = 84.dp,                    // now bar height
    isInteractive: Boolean = true,
    tint: Color = Color.Unspecified,        // optional override for logo tint
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
                    if (isDark) {
                        blur(4.dp.toPx())
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
                    } else {
                        blur(0.dp.toPx())
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
                    }
                },
                layerBlock = if (isInteractive) {
                    {
                        // Softer press / drag scaling
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
                                maxDragScale * abs(cos(ang) * offset.x / size.maxDimension) *
                                (width / height).fastCoerceAtMost(1f)
                        scaleY = scale +
                                maxDragScale * abs(sin(ang) * offset.y / size.maxDimension) *
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
        contentAlignment = Alignment.Center      // center for the WHOLE bar
    ) {
        // Centered text in the glass bar
        Text(
            text = "Tap to enter",
            fontSize = 22.sp,
            textAlign = TextAlign.Center,
            color = if (isDark) Color(0xFFECECEC) else Color(0xFF0F0F0F),
            fontWeight = FontWeight.SemiBold
        )

        // Logo pinned more to the left
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

@Suppress("DEPRECATION")
private fun navigateToMain(context: android.content.Context) {
    val intent = Intent(context, MainActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
    if (context is Activity) {
        context.startActivity(
            intent,
            ActivityOptionsCompat.makeCustomAnimation(
                context,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            ).toBundle()
        )
        context.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        context.finish()
    } else {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
