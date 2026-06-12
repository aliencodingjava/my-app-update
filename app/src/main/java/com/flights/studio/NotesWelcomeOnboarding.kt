@file:Suppress("FunctionName")

package com.flights.studio

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle

@Composable
fun NotesWelcomeOnboardingOverlay(
    visible: Boolean,
    backdrop: Backdrop?,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    onSecondary: (() -> Unit)? = null,
    secondaryText: String = "Maybe later",
) {
    // slight “breathing” scale when visible (feels premium)
    val t by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.95f, stiffness = Spring.StiffnessLow),
        label = "welcomeT"
    )

    AnimatedVisibility(
        visible = visible,
        modifier = modifier.fillMaxSize(),
        enter = fadeIn() +
                slideInVertically(
                    initialOffsetY = { (it * 0.04f).toInt() },
                    animationSpec = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessLow)
                ) +
                scaleIn(initialScale = 0.975f, animationSpec = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessLow)),
        exit = fadeOut() +
                slideOutVertically(
                    targetOffsetY = { (it * 0.03f).toInt() },
                    animationSpec = spring(dampingRatio = 1.0f, stiffness = Spring.StiffnessMedium)
                ) +
                scaleOut(targetScale = 0.985f, animationSpec = spring(dampingRatio = 1.0f, stiffness = Spring.StiffnessMedium))
    ) {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.36f))
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 18.dp)
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                NotesWelcomeCardPro(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = 1f + (t * 0.01f)
                            scaleY = 1f + (t * 0.01f)
                        },
                    backdrop = backdrop,
                    onContinue = onContinue,
                    onSecondary = onSecondary,
                    secondaryText = secondaryText
                )
            }
        }
    }
}

@Composable
private fun NotesWelcomeCardPro(
    onContinue: () -> Unit,
    backdrop: Backdrop?,
    onSecondary: (() -> Unit)?,
    secondaryText: String,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(28.dp)
    val glassTint = if (isDark) {
        Color.Black.copy(alpha = 0.42f)
    } else {
        Color.White.copy(alpha = 0.50f)
    }
    val stroke = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = if (isDark) 0.28f else 0.58f),
            Color.White.copy(alpha = if (isDark) 0.08f else 0.20f),
            cs.outline.copy(alpha = 0.10f)
        )
    )
    val sizedModifier = modifier.heightIn(min = 320.dp)
    val glassModifier = if (backdrop != null) {
        sizedModifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            shadow = null,
            highlight = {
                Highlight(
                    width = if (isDark) 0.45.dp else 0.30.dp,
                    blurRadius = if (isDark) 1.6.dp else 1.dp,
                    alpha = if (isDark) 0.50f else 0.80f,
                    style = HighlightStyle.Plain
                )
            },
            effects = {
                vibrancy()
                blur(24.dp.toPx(), edgeTreatment = TileMode.Mirror)
                lens(
                    refractionHeight = 24.dp.toPx(),
                    refractionAmount = 24.dp.toPx(),
                    depthEffect = false,
                    chromaticAberration = false
                )
            },
            onDrawSurface = {
                drawRect(glassTint)
            }
        )
    } else {
        sizedModifier.background(glassTint, shape)
    }

    Surface(
        modifier = glassModifier,
        shape = shape,
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .border(width = 1.dp, brush = stroke, shape = shape)
                .border(
                    width = 1.dp,
                    color = cs.outline.copy(alpha = if (isDark) 0.12f else 0.16f),
                    shape = shape
                )
        ) {
            Column(
                Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Welcome to Notes",
                    color = cs.onSurface,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                )

                Text(
                    text = "Create notes fast, add photos, set reminders, and keep everything organized.",
                    color = cs.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )


                PremiumFeaturesGrid(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                )

                Spacer(Modifier.size(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onSecondary != null) {
                        OutlinedButton(
                            onClick = onSecondary,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Text(
                                text = secondaryText,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                                maxLines = 1
                            )
                        }
                    }

                    Button(
                        onClick = onContinue,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = cs.surfaceContainerHigh,
                            contentColor = if (isDark) Color.White else cs.onSurface
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text(
                            text = "Add note",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1
                        )
                    }
                }

            }
        }
    }
}

@Composable
private fun PremiumFeaturesGrid(
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)

    val items = listOf(
        ChipItem("AI title", "✨"),
        ChipItem("To-do", "☑"),
        ChipItem("Grammar", "Aa"),
        ChipItem("Voice", "Mic"),
        ChipItem("Reminder", "⏰"),
        ChipItem("Photos", "Img"),
        ChipItem("Files", "Doc"),
        ChipItem("Composer", "✎"),
        ChipItem("Settings", "⚙️"),
        ChipItem("Search", "🔎"),
        ChipItem("Sort", "⇅"),
        ChipItem("Sync", "☁️"),
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            cs.surfaceContainerHigh.copy(alpha = 0.82f),
                            cs.surfaceContainer.copy(alpha = 0.64f)
                        )
                    )
                )
                .border(1.dp, cs.outline.copy(alpha = 0.12f), shape)
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Highlights",
                    color = cs.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    "${items.size} features",
                    color = cs.onSurfaceVariant.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.labelSmall
                )
            }

            PremiumChipGrid3(
                items = items,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private data class ChipItem(
    val text: String,
    val badge: String
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PremiumChipGrid3(
    items: List<ChipItem>,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        maxItemsInEachRow = 3,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            PremiumMiniChip(
                text = item.text,
                badge = item.badge,
                modifier = Modifier
                    .fillMaxWidth(0.31f) // keeps 3-per-row stable
            )
        }
    }
}

@Composable
private fun PremiumMiniChip(
    text: String,
    badge: String,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val chipShape = RoundedCornerShape(14.dp)

    val chipBg = Brush.verticalGradient(
        listOf(
            cs.surface.copy(alpha = 0.46f),
            cs.surface.copy(alpha = 0.26f)
        )
    )

    Box(
        modifier
            .clip(chipShape)
            .background(chipBg)
            .border(1.dp, cs.outline.copy(alpha = 0.10f), chipShape)
            .height(34.dp) // ✅ smaller height
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = badge,
                style = MaterialTheme.typography.labelMedium,
                color = cs.onSurfaceVariant
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = text,
                maxLines = 1,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = cs.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}


@Preview(name = "Welcome Overlay - Pro", showBackground = true, backgroundColor = 0xFFEFEFEF)
@Composable
private fun Preview_NotesWelcomeOverlay_Pro() {
    MaterialTheme {
        NotesWelcomeOnboardingOverlay(
            visible = true,
            backdrop = null,
            onContinue = {},
            onSecondary = {},
            secondaryText = "Create first note"
        )
    }
}
