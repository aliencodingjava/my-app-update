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
    val appThemePreset = LocalAppThemePreset.current
    val appThemePalette = LocalAppThemePalette.current
    val shape = mainWelcomeMaterialShape(appThemePreset)
    val buttonShape = mainWelcomeButtonShape(appThemePreset)
    val glassTint = if (isDark) {
        appThemePalette.glass.copy(alpha = 0.32f)
    } else {
        appThemePalette.glass.copy(alpha = 0.46f)
    }
    val stroke = Brush.verticalGradient(
        listOf(
            appThemePalette.accent.copy(alpha = if (isDark) 0.34f else 0.30f),
            Color.White.copy(alpha = if (isDark) 0.10f else 0.32f),
            appThemePalette.outline.copy(alpha = 0.16f)
        )
    )

    Surface(
        modifier = modifier
            .heightIn(min = 320.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                shadow = null,
                highlight = null,
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
                    drawRect(appThemePalette.glassOverlay.copy(alpha = if (isDark) 0.10f else 0.07f))
                    drawRect(appThemePalette.card.copy(alpha = if (isDark) 0.08f else 0.05f))
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
                .border(width = 1.dp, color = appThemePalette.outline.copy(alpha = if (isDark) 0.12f else 0.16f), shape = shape)
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
                        containerColor = appThemePalette.action.copy(alpha = if (isDark) 0.30f else 0.24f),
                        contentColor = appThemePalette.actionContent
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 13.dp),
                    modifier = Modifier.fillMaxWidth(),
                    shape = buttonShape
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

private fun mainWelcomeMaterialShape(preset: AppThemePreset): RoundedCornerShape {
    return when (preset) {
        AppThemePreset.Classic -> RoundedCornerShape(28.dp)
        AppThemePreset.Sky -> RoundedCornerShape(
            topStart = 34.dp,
            topEnd = 22.dp,
            bottomEnd = 34.dp,
            bottomStart = 24.dp
        )
        AppThemePreset.Sunset -> RoundedCornerShape(
            topStart = 22.dp,
            topEnd = 38.dp,
            bottomEnd = 24.dp,
            bottomStart = 38.dp
        )
        AppThemePreset.Aurora -> RoundedCornerShape(
            topStart = 38.dp,
            topEnd = 22.dp,
            bottomEnd = 38.dp,
            bottomStart = 22.dp
        )
        AppThemePreset.Graphite -> RoundedCornerShape(18.dp)
        AppThemePreset.Ocean -> RoundedCornerShape(24.dp, 38.dp, 30.dp, 38.dp)
        AppThemePreset.Meadow -> RoundedCornerShape(38.dp, 24.dp, 38.dp, 24.dp)
        AppThemePreset.Candy -> RoundedCornerShape(36.dp)
        AppThemePreset.Royal -> RoundedCornerShape(22.dp, 34.dp, 22.dp, 34.dp)
        AppThemePreset.Ember -> RoundedCornerShape(18.dp, 34.dp, 18.dp, 34.dp)
    }
}

private fun mainWelcomeButtonShape(preset: AppThemePreset): RoundedCornerShape {
    return when (preset) {
        AppThemePreset.Classic -> RoundedCornerShape(18.dp)
        AppThemePreset.Sky -> RoundedCornerShape(22.dp, 14.dp, 22.dp, 14.dp)
        AppThemePreset.Sunset -> RoundedCornerShape(14.dp, 24.dp, 14.dp, 24.dp)
        AppThemePreset.Aurora -> RoundedCornerShape(24.dp, 14.dp, 24.dp, 14.dp)
        AppThemePreset.Graphite -> RoundedCornerShape(12.dp)
        AppThemePreset.Ocean -> RoundedCornerShape(18.dp, 24.dp, 18.dp, 24.dp)
        AppThemePreset.Meadow -> RoundedCornerShape(24.dp, 18.dp, 24.dp, 18.dp)
        AppThemePreset.Candy -> RoundedCornerShape(24.dp)
        AppThemePreset.Royal -> RoundedCornerShape(16.dp, 24.dp, 16.dp, 24.dp)
        AppThemePreset.Ember -> RoundedCornerShape(12.dp, 22.dp, 12.dp, 22.dp)
    }
}

@Composable
private fun MainHighlightsGrid(modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()
    val palette = LocalAppThemePalette.current
    val shape = RoundedCornerShape(18.dp)
    val surfaceColor = palette.card.copy(alpha = if (isDark) 0.62f else 0.82f)
    val borderColor = palette.outline.copy(alpha = if (isDark) 0.26f else 0.20f)
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
    val palette = LocalAppThemePalette.current
    val chipShape = RoundedCornerShape(14.dp)
    val chipBg = palette.badge.copy(alpha = if (isDark) 0.26f else 0.34f)
    val chipBorder = palette.outline.copy(alpha = if (isDark) 0.18f else 0.16f)

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
                tint = palette.badgeContent,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = feature.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (isDark) cs.onSurface else palette.actionContent,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
