package com.flights.studio

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import kotlinx.coroutines.launch

// Create a stable data class to hold your theme-dependent values
private data class DialogStyles(
    val content: Color,
    val accent: Color,
    val container: Color,val dim: Color,
    val title: TextStyle,
    val body: TextStyle,
    val button: TextStyle
)

@Composable
fun ExitLiquidDialog(
    backdrop: Backdrop,
    onCancel: () -> Unit,
    onConfirmExit: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Exit App",
    message: String = "Are you sure you want to exit?"
) {
    val isLightTheme = !isSystemInDarkTheme()

    // 1. Properly typed styles block
    val styles = remember(isLightTheme) {
        val contentColor = if (isLightTheme) Color.Black else Color.White
        val textShadow = Shadow(
            color = Color.Black.copy(alpha = if (isLightTheme) 0.35f else 0.80f),
            offset = Offset(0f, 2f),
            blurRadius = 7f
        )

        DialogStyles(
            content = contentColor,
            accent = if (isLightTheme) Color(0xFF0088FF) else Color(0xFF0091FF),
            container = if (isLightTheme) Color(0xFFFAFAFA).copy(alpha = 0.60f)
            else Color(0xFF121212).copy(alpha = 0.40f),
            dim = if (isLightTheme) Color(0xFF29293A).copy(alpha = 0.23f)
            else Color(0xFF121212).copy(alpha = 0.56f),
            title = TextStyle(
                color = contentColor, fontSize = 24.sp,
                fontWeight = FontWeight.Medium, textAlign = TextAlign.Center,
                shadow = textShadow
            ),
            body = TextStyle(
                color = contentColor.copy(alpha = 0.70f), fontSize = 15.sp,
                textAlign = TextAlign.Center, shadow = textShadow
            ),
            button = TextStyle(
                fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center, shadow = textShadow
            )
        )
    }

    val dialogScale = remember { Animatable(1.2f) }
    val dialogAlpha = remember { Animatable(0f) }
    val scrimAlpha  = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { scrimAlpha.animateTo(1f, spring(stiffness = 900f)) }
        launch { dialogAlpha.animateTo(1f, spring(stiffness = 1200f)) }
        launch { dialogScale.animateTo(1f, spring(dampingRatio = 0.82f, stiffness = 520f)) }
    }

    BackHandler(onBack = onCancel)

    // Remember interaction sources to prevent churn during animations
    val scrimInteraction = remember { MutableInteractionSource() }
    val contentInteraction = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(styles.dim.copy(alpha = styles.dim.alpha * scrimAlpha.value))
            .clickable(interactionSource = scrimInteraction, indication = null) { onCancel() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .heightIn(min = 260.dp)
                .graphicsLayer {
                    alpha = dialogAlpha.value
                    scaleX = dialogScale.value
                    scaleY = dialogScale.value
                }
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { ContinuousRoundedRectangle(48.dp) },
                    effects = {
                        colorControls(brightness = if (isLightTheme) 0.20f else 0f, saturation = 1.50f)
                        blur(if (isLightTheme) 16.dp.toPx() else 8.dp.toPx())
                        lens(24.dp.toPx(), 48.dp.toPx(), depthEffect = true)
                    },
                    highlight = { Highlight.Plain },
                    onDrawSurface = { drawRect(styles.container) }
                )
                .clip(ContinuousRoundedRectangle(48.dp))
                .clickable(interactionSource = contentInteraction, indication = null) { /* Consume clicks */ }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 24.dp, end = 24.dp, bottom = 74.dp)
            ) {
                BasicText(text = title, modifier = Modifier.fillMaxWidth(), style = styles.title)

                Box(
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        text = message,
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (isLightTheme) Modifier else Modifier.graphicsLayer(blendMode = BlendMode.Plus)),
                        style = styles.body,
                        maxLines = 5
                    )
                }
            }

            DialogButtons(
                onCancel = onCancel,
                onConfirm = onConfirmExit,
                styles = styles
            )
        }
    }
}

@Composable
private fun BoxScope.DialogButtons(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    styles: DialogStyles // Use the specific data class
) {
    Row(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DialogButton(
            text = "Cancel",
            backgroundColor = styles.container.copy(alpha = 0.22f),
            textColor = styles.content,
            style = styles.button,
            onClick = onCancel,
            modifier = Modifier.weight(1f)
        )
        DialogButton(
            text = "Exit",
            backgroundColor = styles.accent,
            textColor = Color.White,
            style = styles.button,
            onClick = onConfirm,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DialogButton(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    style: TextStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(50.dp)
            .clip(ContinuousCapsule)
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        BasicText(text, style = style.copy(color = textColor))
    }
}