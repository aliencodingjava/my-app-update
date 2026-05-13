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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun MainWelcomeOnboardingOverlay(
    visible: Boolean,
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
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.30f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.20f),
                            Color.Black.copy(alpha = 0.62f)
                        )
                    )
                )
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
                onDone = onDone
            )
        }
    }
}

@Composable
private fun MainWelcomeCard(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(28.dp)
    val glassBg = Brush.verticalGradient(
        listOf(
            cs.surfaceContainerHighest.copy(alpha = 0.94f),
            cs.surfaceContainerHigh.copy(alpha = 0.88f),
            cs.surface.copy(alpha = 0.84f),
        )
    )
    val stroke = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = 0.36f),
            Color.White.copy(alpha = 0.12f),
            Color.Transparent
        )
    )

    Surface(
        modifier = modifier.heightIn(max = 660.dp),
        shape = shape,
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(glassBg, shape)
                .border(width = 1.dp, brush = stroke, shape = shape)
                .border(width = 1.dp, color = cs.outline.copy(alpha = 0.12f), shape = shape)
        ) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(cs.primary.copy(alpha = 0.10f), Color.Transparent),
                            radius = 900f
                        ),
                        shape = shape
                    )
            )

            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                MainWelcomeIllustration()

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
                        containerColor = cs.primary,
                        contentColor = cs.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 13.dp),
                    modifier = Modifier.fillMaxWidth()
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
private fun MainWelcomeIllustration() {
    val cs = MaterialTheme.colorScheme
    val panelShape = RoundedCornerShape(22.dp)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(2.9f),
        shape = panelShape,
        color = cs.surfaceVariant,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WelcomeIconBubble(R.drawable.fullscreen_24dp_46152f_fill1_wght400_grad0_opsz24)
            WelcomeIconBubble(R.drawable.ic_oui_notes)
            WelcomeIconBubble(R.drawable.ic_oui_contact)

            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = cs.onSurface.copy(alpha = 0.10f)
                ) { Spacer(Modifier.size(width = 126.dp, height = 9.dp)) }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = cs.onSurface.copy(alpha = 0.07f)
                ) { Spacer(Modifier.size(width = 92.dp, height = 9.dp)) }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = cs.onSurface.copy(alpha = 0.06f)
                ) { Spacer(Modifier.size(width = 70.dp, height = 9.dp)) }
            }
        }
    }
}

@Composable
private fun WelcomeIconBubble(@DrawableRes iconRes: Int) {
    val cs = MaterialTheme.colorScheme

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = cs.primary.copy(alpha = 0.12f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = cs.primary,
            modifier = Modifier
                .padding(12.dp)
                .size(26.dp)
        )
    }
}

@Composable
private fun MainHighlightsGrid(modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)
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
            .background(
                Brush.verticalGradient(
                    listOf(
                        cs.surfaceVariant.copy(alpha = 0.46f),
                        cs.surface.copy(alpha = 0.26f)
                    )
                )
            )
            .border(1.dp, cs.outline.copy(alpha = 0.11f), shape)
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
