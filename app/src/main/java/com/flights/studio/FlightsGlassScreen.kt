package com.flights.studio

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toPath

data class GlassBtn(
    val id: String,
    @get:DrawableRes val icon: Int,
    val label: String,
    val description: String,
    val tintIcon: Boolean = true,
    val iconCircleColor: Color? = null,
    val iconText: String? = null
)

private data class HomeActionSection(
    val title: String,
    val buttons: List<GlassBtn>
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun FlightsGlassScreen(
    onBack: () -> Unit,
    onOpenCard: (String) -> Unit,
    showTopArea: Boolean = true,
    backdropOverride: LayerBackdrop? = null,
) {
    val backdrop = backdropOverride ?: rememberLayerBackdrop { drawContent() }
    val haptics = rememberHapticHelper()

    Column(modifier = Modifier.fillMaxSize()) {
        if (showTopArea) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .safeDrawingPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TopLeftPillActions(
                    backdrop = backdrop,
                    backIconRes = R.drawable.exit_to_app_24dp_ffffff_fill1_wght400_grad0_opsz24,
                    exitIconRes = R.drawable.ic_samsung_close,
                    onExit = { haptics.tick(); onBack() }
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            HomeActionPanel(
                backdrop = backdrop,
                onOpen = { id ->
                    haptics.tick()
                    onOpenCard(id)
                },
                modifier = Modifier.matchParentSize()
            )
        }
    }
}

@Composable
private fun HomeActionPanel(
    backdrop: LayerBackdrop,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val bottomReserve = if (maxHeight < 460.dp) 84.dp else 96.dp

        val horizontalPadding = when {
            maxWidth >= 900.dp -> 18.dp      // tablets need small margin
            maxWidth >= 700.dp -> 14.dp
            maxWidth < 360.dp -> 6.dp
            else -> 8.dp                    // phones close to edge
        }

        val sections = remember {
            listOf(
                HomeActionSection(
                    title = "Live & Airport",
                    buttons = listOf(
                        GlassBtn(
                            id = "card1",
                            icon = R.drawable.play_arrow_24dp_ffffff_fill1_wght400_grad0_opsz24,
                            label = "Play",
                            description = "Watch live airport view",
                            iconCircleColor = Color(0xFF6D3DEB)
                        ),
                        GlassBtn(
                            id = "card2",
                            icon = R.drawable.ic_oui_news,
                            label = "News",
                            description = "Airport updates and alerts",
                            iconCircleColor = Color(0xFF2F80ED)
                        ),
                        GlassBtn(
                            id = "card3",
                            icon = R.drawable.flight_24dp_ffffff_fill0_wght400_grad0_opsz24,
                            label = "Flights",
                            description = "Check arrivals and departures",
                            iconCircleColor = Color(0xFF55B96F)
                        ),
                        GlassBtn(
                            id = "card4",
                            icon = R.drawable.travel_16dp_ffffff_fill0_wght400_grad0_opsz20,
                            label = "Travel",
                            description = "Passenger info and services",
                            iconCircleColor = Color(0xFFE8773E)
                        )
                    )
                ),
                HomeActionSection(
                    title = "Tracking & Driver Tools",
                    buttons = listOf(
                        GlassBtn(
                            id = "card12",
                            icon = 0,
                            label = "FlightRadar24",
                            description = "Live aircraft tracking",
                            iconCircleColor = Color(0xFF0B78D0),
                            iconText = "24"
                        ),
                        GlassBtn(
                            id = "card13",
                            icon = 0,
                            label = "FlightAware",
                            description = "Flight status lookup",
                            iconCircleColor = Color(0xFFF47B20),
                            iconText = "FA"
                        ),
                        GlassBtn(
                            id = "card10",
                            icon = 0,
                            label = "Uber Driver",
                            description = "Open driver app",
                            iconCircleColor = Color.Black,
                            iconText = "uber"
                        ),
                        GlassBtn(
                            id = "card11",
                            icon = 0,
                            label = "Lyft Driver",
                            description = "Open driver app",
                            iconCircleColor = Color(0xFFFF00BF),
                            iconText = "Lyft"
                        )
                    )
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(
                    start = horizontalPadding,
                    end = horizontalPadding,
                    top = 10.dp,
                    bottom = bottomReserve
                ),
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Top),
            horizontalAlignment = Alignment.Start
        ) {
            sections.forEach { section ->
                HomeActionSectionList(
                    section = section,
                    onOpen = onOpen
                )
            }
        }
    }
}

@Composable
private fun HomeActionSectionList(
    section: HomeActionSection,
    onOpen: (String) -> Unit
) {
    val darkTheme = isSystemInDarkTheme()
    val appPalette = LocalAppThemePalette.current
    val surfaceRoles = appThemeSurfaceRoles(appPalette, darkTheme)

    val sectionTextColor = surfaceRoles.section

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            text = section.title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.sp
            ),
            color = sectionTextColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp)
        )

        AppThemeSectionSurface(shape = RoundedCornerShape(18.dp)) {
            Column(modifier = Modifier.fillMaxWidth()) {
                section.buttons.forEachIndexed { index, btn ->
                    HomeActionListItem(
                        button = btn,
                        onOpen = onOpen
                    )
                    if (index != section.buttons.lastIndex) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 60.dp, end = 14.dp)
                        ) {
                            HorizontalDivider(
                                thickness = 1.dp,
                                color = surfaceRoles.border.copy(alpha = 0.58f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeActionListItem(
    button: GlassBtn,
    onOpen: (String) -> Unit
) {
    val darkTheme = isSystemInDarkTheme()
    val appPalette = LocalAppThemePalette.current
    val surfaceRoles = appThemeSurfaceRoles(appPalette, darkTheme)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.975f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "home_action_press_scale"
    )

    val titleColor = surfaceRoles.title
    val descriptionColor = surfaceRoles.subtitle
    val chevronColor = surfaceRoles.chevron

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onOpen(button.id) }
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HomeActionIcon(button, appPalette)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = button.label,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        lineHeight = 19.sp,
                        letterSpacing = 0.sp
                    ),
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = button.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = descriptionColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = chevronColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun HomeActionIcon(
    button: GlassBtn,
    appPalette: AppThemePalette
) {
    val circleColor = homeActionIconColor(button.id, appPalette)
    val contentColor = if (circleColor.luminance() > 0.58f) Color(0xFF111418) else Color.White

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(circleColor),
        contentAlignment = Alignment.Center
    ) {
        if (button.iconText != null) {
            Text(
                text = button.iconText,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = if (button.iconText.length > 3) 11.sp else 14.sp,
                    lineHeight = 14.sp
                ),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        } else {
            Icon(
                painter = painterResource(button.icon),
                contentDescription = null,
                tint = if (button.tintIcon) contentColor else Color.Unspecified,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

private fun homeActionIconColor(
    id: String,
    palette: AppThemePalette
): Color {
    return when (id) {
        "card1" -> palette.accent
        "card2" -> palette.warm
        "card3" -> palette.rose
        "card4" -> palette.action
        "card12" -> palette.accent.copy(alpha = 0.88f)
        "card13" -> palette.warm.copy(alpha = 0.90f)
        "card10" -> palette.bottomPill
        "card11" -> palette.rose.copy(alpha = 0.92f)
        else -> palette.badge
    }
}

@Composable
fun rememberHapticHelper(): HapticHelper {
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    return remember(view, haptic) { HapticHelper(view, haptic) }
}

class HapticHelper(
    private val view: android.view.View,
    private val feedback: HapticFeedback
) {
    fun tick() {
        if (!view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)) {
            feedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
}
