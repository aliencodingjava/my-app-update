@file:Suppress("FunctionName")

package com.flights.studio

import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle

@Composable
fun MainWelcomeOnboardingOverlay(
    visible: Boolean,
    backdrop: Backdrop,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val t by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.95f, stiffness = Spring.StiffnessLow),
        label = "mainWelcomeT"
    )

    AnimatedVisibility(
        visible = visible,
        modifier = modifier.fillMaxSize(),
        enter = fadeIn() +
            slideInVertically(
                initialOffsetY = { (it * 0.04f).toInt() },
                animationSpec = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessLow)
            ) +
            scaleIn(
                initialScale = 0.975f,
                animationSpec = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessLow)
            ),
        exit = fadeOut() +
            slideOutVertically(
                targetOffsetY = { (it * 0.03f).toInt() },
                animationSpec = spring(dampingRatio = 1.0f, stiffness = Spring.StiffnessMedium)
            ) +
            scaleOut(
                targetScale = 0.985f,
                animationSpec = spring(dampingRatio = 1.0f, stiffness = Spring.StiffnessMedium)
            )
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
                MainWelcomeCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = 1f + (t * 0.01f)
                            scaleY = 1f + (t * 0.01f)
                        },
                    backdrop = backdrop,
                    onDone = onDone
                )
            }
        }
    }
}

@Composable
private fun MainWelcomeCard(
    onDone: () -> Unit,
    backdrop: Backdrop,
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

    Surface(
        modifier = modifier
            .heightIn(min = 320.dp)
            .drawBackdrop(
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
            ),
        shape = shape,
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .border(width = 1.dp, brush = stroke, shape = shape)
                .border(width = 1.dp, color = cs.outline.copy(alpha = if (isDark) 0.12f else 0.16f), shape = shape)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Welcome to JAC Airport",
                    color = cs.onSurface,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Live airport tools, contacts, notes, settings, profile, QR, driver links, and flight tracking in one place.",
                    color = cs.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                MainHighlightsGrid(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                )

                Button(
                    onClick = onDone,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = cs.surfaceContainerHigh,
                        contentColor = if (isDark) Color.White else cs.onSurface
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 13.dp),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(
                        text = "Done",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}

@Composable
private fun MainHighlightsGrid(modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(18.dp)
    val surfaceColor = if (isDark) Color(0xFF232425) else Color(0xFFFEFEFE)
    val borderColor = if (isDark) Color(0xFF333538) else Color(0xFFE3E3E4)
    val items = listOf(
        MainFeature("Live cams", R.drawable.fullscreen_24dp_46152f_fill1_wght400_grad0_opsz24),
        MainFeature("Play", R.drawable.play_arrow_24dp_ffffff_fill1_wght400_grad0_opsz24),
        MainFeature("News", R.drawable.ic_oui_news),
        MainFeature("Flights", R.drawable.flight_24dp_ffffff_fill0_wght400_grad0_opsz24),
        MainFeature("Travel", R.drawable.travel_16dp_ffffff_fill0_wght400_grad0_opsz20),
        MainFeature("Flight apps", R.drawable.baseline_flight_24),
        MainFeature("Drivers", R.drawable.travel_24dp_ffffff_fill1_wght400_grad0_opsz24),
        MainFeature("Contacts", R.drawable.ic_oui_contact),
        MainFeature("Notes", R.drawable.ic_oui_notes),
        MainFeature("Settings", R.drawable.ic_oui_settings),
        MainFeature("QR code", R.drawable.ic_oui_qr_code),
        MainFeature("Profile", R.drawable.account_circle_24dp_ffffff_fill1_profile),
    )

    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(surfaceColor)
            .border(1.dp, borderColor, shape)
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

        MainFeatureChipGrid(items = items, modifier = Modifier.fillMaxWidth())
    }
}

private data class MainFeature(
    val text: String,
    @param:DrawableRes val iconRes: Int
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MainFeatureChipGrid(
    items: List<MainFeature>,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        maxItemsInEachRow = 3,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            MainFeatureChip(
                feature = item,
                modifier = Modifier.fillMaxWidth(0.31f)
            )
        }
    }
}

@Composable
private fun MainFeatureChip(
    feature: MainFeature,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()
    val chipShape = RoundedCornerShape(14.dp)
    val chipBg = if (isDark) Color(0xFF2A2B2D) else Color(0xFFF5F6F8)
    val chipBorder = if (isDark) Color(0xFF3A3C3F) else Color(0xFFE3E3E4)

    Box(
        modifier
            .clip(chipShape)
            .background(chipBg)
            .border(1.dp, chipBorder, chipShape)
            .height(34.dp)
            .padding(horizontal = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(feature.iconRes),
                contentDescription = null,
                tint = cs.onSurfaceVariant,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = feature.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = cs.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
