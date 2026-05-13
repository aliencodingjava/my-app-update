package com.flights.studio

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.shadow.Shadow

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

        val rowHeight = when {
            maxWidth >= 900.dp -> 74.dp      // tablets
            maxWidth >= 700.dp -> 70.dp      // foldables / small tablets
            maxWidth >= 500.dp -> 66.dp      // big phones / Ultra
            maxHeight < 390.dp -> 54.dp      // very tight
            maxHeight < 520.dp -> 60.dp      // compact
            else -> 64.dp                   // normal phones
        }

        val itemSpacing = when {
            maxWidth >= 700.dp -> 8.dp
            maxHeight < 420.dp -> 5.dp
            else -> 7.dp
        }

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
                    backdrop = backdrop,
                    section = section,
                    onOpen = onOpen,
                    rowHeight = rowHeight,
                    itemSpacing = itemSpacing
                )
            }
        }
    }
}

@Composable
private fun HomeActionSectionList(
    backdrop: LayerBackdrop,
    section: HomeActionSection,
    onOpen: (String) -> Unit,
    rowHeight: Dp,
    itemSpacing: Dp
) {
    val darkTheme = isSystemInDarkTheme()

    val sectionTextColor = if (darkTheme) {
        Color.White.copy(alpha = 0.90f)
    } else {
        Color.Black.copy(alpha = 0.82f)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(itemSpacing)
    ) {
        Text(
            text = section.title,
            modifier = Modifier.padding(start = 2.dp, bottom = 2.dp),
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                lineHeight = 16.sp
            ),
            color = sectionTextColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        section.buttons.forEach { btn ->
            HomeActionListItem(
                backdrop = backdrop,
                button = btn,
                rowHeight = rowHeight,
                onOpen = onOpen
            )
        }
    }
}

@Composable
private fun HomeActionListItem(
    backdrop: LayerBackdrop,
    button: GlassBtn,
    rowHeight: Dp,
    onOpen: (String) -> Unit
) {
    val darkTheme = isSystemInDarkTheme()
    val shape = RoundedCornerShape(14.dp)

    val glassSurfaceColor = if (darkTheme) {
        Color(0xFF1D1726).copy(alpha = 0.88f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
    }

//    val rowOverlayColor = if (darkTheme) {
//        Color.White.copy(alpha = 0.035f)
//    } else {
//        Color(0xFF4A90FF).copy(alpha = 0.045f)
//    }

    val titleColor = if (darkTheme) {
        Color.White.copy(alpha = 0.94f)
    } else {
        Color.Black.copy(alpha = 0.88f)
    }

    val descriptionColor = if (darkTheme) {
        Color.White.copy(alpha = 0.66f)
    } else {
        Color.Black.copy(alpha = 0.58f)
    }

    val chevronColor = if (darkTheme) {
        Color.White.copy(alpha = 0.72f)
    } else {
        Color.Black.copy(alpha = 0.42f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(rowHeight)
            .border(
                BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = if (darkTheme) 0.34f else 0.22f)
                ),
                shape
            )
            .clip(shape)
            .clickable { onOpen(button.id) }
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                shadow = {
                    Shadow.Default.copy(
                        alpha = if (darkTheme) 0.35f else 0.28f,
                    )
                },
                highlight = null,
                effects = {
                    vibrancy()
                    blur(
                        radius = if (darkTheme) 2.dp.toPx() else 3.dp.toPx(),
                        edgeTreatment = TileMode.Clamp
                    )
                    lens(
                        refractionHeight = 18.dp.toPx(),
                        refractionAmount = 58.dp.toPx(),
                        depthEffect = false,
                        chromaticAberration = true
                    )
                },
                onDrawSurface = {
                    drawRect(glassSurfaceColor)

                }
            )
            .padding(start = 10.dp, end = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HomeActionIcon(button)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = button.label,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 14.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = button.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        lineHeight = 13.sp,
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
private fun HomeActionIcon(button: GlassBtn) {
    val circleColor = button.iconCircleColor ?: Color(0xFF3B3D42)

    Box(
        modifier = Modifier
            .size(38.dp)
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
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        } else {
            Icon(
                painter = painterResource(button.icon),
                contentDescription = null,
                tint = if (button.tintIcon) Color.White else Color.Unspecified,
                modifier = Modifier.size(22.dp)
            )
        }
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
