@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION", "COMPOSE_APPLIER_CALL_MISMATCH")

package com.flights.studio

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp // ✅ ADD THIS
import androidx.core.view.WindowCompat
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
// ✅ Kyant Backdrop
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.tanh

class QRCodeComposeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = Color.Transparent.toArgb()
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)

        setContent {
            AppM3Theme {
                QRCodeScreen(
                    onBack = {
                        finish()
                        overridePendingTransition(
                            R.anim.enter_animation,
                            de.dlyt.yanndroid.samsung.R.anim.abc_tooltip_exit
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun AppM3Theme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val scheme = if (dark) darkColorScheme() else lightColorScheme()

    MaterialTheme(
        colorScheme = scheme,
        content = content
    )
}

@Composable
private fun QRCodeScreen(onBack: () -> Unit) {
    BackHandler { onBack() }

    val isDark = isSystemInDarkTheme()
    val ui = rememberUiScale()

    // ✅ same as FancyPillToast: damp text scale a bit
    val textK = 0.92f

    val overlayScrim =
        if (isDark) MaterialTheme.colorScheme.scrim.copy(alpha = 0.42f)
        else MaterialTheme.colorScheme.scrim.copy(alpha = 0.12f)

    val headerSurface =
        if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f)
        else MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)

    val chipSurface =
        if (isDark) MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.90f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f)

    val backdrop: Backdrop = rememberLayerBackdrop { drawContent() }

    val isLandscape =
        LocalConfiguration.current.orientation ==
                android.content.res.Configuration.ORIENTATION_LANDSCAPE

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop as LayerBackdrop)
            ) {
                Image(
                    painter = painterResource(R.drawable.default_pattern),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(Modifier.fillMaxSize().background(overlayScrim))
            }

            if (!isLandscape) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 22.dp.us(ui)),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    FloatingHeader(
                        onBack = onBack,
                        surfaceColor = headerSurface,
                        ui = ui,
                        textK = textK,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp.us(ui))
                            .statusBarsPadding()
                    )

                    Spacer(Modifier.weight(1f))

                    HeroQrGlassOnly(
                        backdrop = backdrop,
                        tileColor = chipSurface,
                        ui = ui,
                        textK = textK
                    )

                    Spacer(Modifier.weight(1f))

                    BottomChips(
                        surfaceColor = headerSurface,
                        ui = ui,
                        textK = textK,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 18.dp)
                            .navigationBarsPadding()
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 22.dp.us(ui))
                        .statusBarsPadding()
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        HeroQrGlassOnly(
                            backdrop = backdrop,
                            tileColor = chipSurface,
                            ui = ui,
                            textK = textK
                        )
                    }

                    Spacer(Modifier.width(18.dp.us(ui)))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        BottomChips(
                            surfaceColor = headerSurface,
                            ui = ui,
                            textK = textK,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 18.dp)
                                .navigationBarsPadding()
                        )
                    }
                }

                FloatingHeader(
                    onBack = onBack,
                    surfaceColor = headerSurface,
                    compact = true,
                    ui = ui,
                    textK = textK,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .fillMaxWidth(0.5f)
                        .padding(top = 14.dp)
                        .statusBarsPadding()
                )
            }
        }
    }
}

@Composable
private fun FloatingHeader(
    onBack: () -> Unit,
    surfaceColor: Color,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    ui: Float,
    textK: Float
) {
    val uiScale = ui // keep your dp scaling
    val shape = RoundedCornerShape((22.dp).us(uiScale))
    val vPad = (if (compact) 6.dp else 10.dp).us(uiScale)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp.us(uiScale))
            .shadow(elevation = 3.dp.us(uiScale), shape = shape, clip = false)
            .background(color = surfaceColor.copy(alpha = 1f), shape = shape)
            .clip(shape)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp.us(uiScale), vertical = vPad)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    painter = painterResource(R.drawable.baseline_arrow_back_ios_24),
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            val base = MaterialTheme.typography.titleMedium
            Text(
                text = "QR Code",
                style = base.copy(fontSize = (base.fontSize.value * ui * textK).sp), // ✅ ONLY fontSize
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun HeroQrGlassOnly(
    backdrop: Backdrop,
    tileColor: ComposeColor,
    ui: Float,
    textK: Float,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    isInteractive: Boolean = true,
) {
    val shape = RoundedCornerShape(34.dp)

    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }
    val isLightTheme = !isSystemInDarkTheme()

    val containerColor =
        if (isLightTheme) Color(0xFFFAFAFA).copy(alpha = 0.44f)
        else Color(0xFF121212).copy(alpha = 0.18f)

    Column(
        modifier = modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    colorControls(
                        brightness = if (isLightTheme) 0.01f else 0.00f,
                        contrast = 1.15f,
                        saturation = 1.28f
                    )
                    blur(1f.dp.toPx(), edgeTreatment = TileMode.Mirror)
                    lens(
                        refractionHeight = 34f.dp.toPx(),
                        refractionAmount = 44f.dp.toPx(),
                        depthEffect = true,
                        chromaticAberration = false
                    )
                },
                layerBlock = if (isInteractive) {
                    {
                        val width = size.width
                        val height = size.height
                        val progress = interactiveHighlight.pressProgress
                        val scale = lerp(1f, 1f + 4f.dp.toPx() / size.height, progress)
                        val maxOffset = size.minDimension
                        val initialDerivative = 0.05f
                        val offset = interactiveHighlight.offset
                        translationX = maxOffset * tanh(initialDerivative * offset.x / maxOffset)
                        translationY = maxOffset * tanh(initialDerivative * offset.y / maxOffset)
                        val maxDragScale = 2f.dp.toPx() / size.height
                        val offsetAngle = atan2(offset.y, offset.x)
                        scaleX =
                            scale +
                                    maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) *
                                    (width / height).fastCoerceAtMost(1f)
                        scaleY =
                            scale +
                                    maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) *
                                    (height / width).fastCoerceAtMost(1f)
                    }
                } else null,
                onDrawSurface = { drawRect(containerColor) }
            )
            .clickable(
                interactionSource = null,
                indication = if (isInteractive) null else LocalIndication.current,
                role = Role.Button,
                onClick = onClick
            )
            .then(
                if (isInteractive) {
                    Modifier
                        .then(interactiveHighlight.modifier)
                        .then(interactiveHighlight.gestureModifier)
                } else Modifier
            )
            .padding(horizontal = 22.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val baseTitle = MaterialTheme.typography.titleLarge
        Text(
            text = "Scan to download",
            style = baseTitle.copy(fontSize = (baseTitle.fontSize.value * ui * textK).sp),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(6.dp))

        val baseBody = MaterialTheme.typography.bodySmall
        Text(
            text = "Open your camera and point at the code",
            style = baseBody.copy(fontSize = (baseBody.fontSize.value * ui * textK).sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(10.dp))

        Surface(
            shape = RoundedCornerShape(28.dp),
            color = tileColor,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Image(
                painter = painterResource(R.drawable.qrflights),
                contentDescription = stringResource(R.string.qrcode),
                modifier = Modifier
                    .padding(16.dp)
                    .size(200.dp),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(Modifier.height(14.dp))

        val baseBrand = MaterialTheme.typography.labelLarge
        Text(
            text = "Flights Studio",
            style = baseBrand.copy(fontSize = (baseBrand.fontSize.value * ui * textK).sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BottomChips(
    surfaceColor: Color,
    ui: Float,
    textK: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val density = LocalDensity.current
        var maxChipWidthPx by remember { mutableIntStateOf(0) }
        val maxChipWidthDp = with(density) { maxChipWidthPx.toDp() }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
        ) {
            InfoChip(
                title = "Tip",
                body = "Use good light",
                surfaceColor = surfaceColor,
                ui = ui,
                textK = textK,
                modifier = Modifier
                    .onSizeChanged { maxChipWidthPx = max(maxChipWidthPx, it.width) }
                    .then(if (maxChipWidthPx > 0) Modifier.width(maxChipWidthDp) else Modifier)
            )

            InfoChip(
                title = "Works on",
                body = "Only on Android",
                surfaceColor = surfaceColor,
                ui = ui,
                textK = textK,
                modifier = Modifier
                    .onSizeChanged { maxChipWidthPx = max(maxChipWidthPx, it.width) }
                    .then(if (maxChipWidthPx > 0) Modifier.width(maxChipWidthDp) else Modifier)
            )
        }

        val shape = RoundedCornerShape(18.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 3.dp, shape = shape, clip = false)
                .background(color = surfaceColor.copy(alpha = 1f), shape = shape)
                .clip(shape)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(2.dp)
                        )
                )

                val baseWhy = MaterialTheme.typography.labelLarge
                Text(
                    text = stringResource(R.string.why_download_app),
                    style = baseWhy.copy(fontSize = (baseWhy.fontSize.value * ui * textK).sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun InfoChip(
    title: String,
    body: String,
    surfaceColor: Color,
    ui: Float,
    textK: Float,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .shadow(elevation = 3.dp, shape = shape, clip = false)
            .background(color = surfaceColor.copy(alpha = 1f), shape = shape)
            .clip(shape)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val baseTitle = MaterialTheme.typography.labelMedium
            Text(
                text = title,
                style = baseTitle.copy(fontSize = (baseTitle.fontSize.value * ui * textK).sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val baseBody = MaterialTheme.typography.titleSmall
            Text(
                text = body,
                style = baseBody.copy(fontSize = (baseBody.fontSize.value * ui * textK).sp),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Preview(name = "QR Light", showBackground = true)
@Composable
private fun Preview_QR_Light() {
    AppM3Theme { QRCodeScreen(onBack = {}) }
}

@Preview(
    name = "QR Dark",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun Preview_QR_Dark() {
    AppM3Theme { QRCodeScreen(onBack = {}) }
}
