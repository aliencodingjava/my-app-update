@file:Suppress("FunctionName")

package com.flights.studio

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun MainWelcomeOnboardingOverlay(
    visible: Boolean,
    backdrop: Backdrop,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var modalVisible by remember { mutableStateOf(false) }

    LaunchedEffect(visible) {
        if (visible) {
            modalVisible = false
            kotlinx.coroutines.delay(1_000L.milliseconds)
            modalVisible = true
        } else {
            modalVisible = false
        }
    }

    val t by animateFloatAsState(
        targetValue = if (modalVisible) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.95f, stiffness = Spring.StiffnessLow),
        label = "mainWelcomeT"
    )

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        AnimatedVisibility(
            visible = modalVisible,
            enter = fadeIn(animationSpec = tween(durationMillis = 130)),
            exit = fadeOut(animationSpec = tween(durationMillis = 160))
        ) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.36f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDone
                    )
            )
        }

        AnimatedVisibility(
            visible = modalVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(
                animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing),
                initialOffsetY = { it / 2 }
            ) + fadeIn(animationSpec = tween(durationMillis = 170)) +
                scaleIn(
                    animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing),
                    initialScale = 0.94f
                ),
            exit = slideOutVertically(
                animationSpec = tween(durationMillis = 210, easing = FastOutLinearInEasing),
                targetOffsetY = { it / 3 }
            ) + fadeOut(animationSpec = tween(durationMillis = 150)) +
                scaleOut(
                    animationSpec = tween(durationMillis = 210, easing = FastOutLinearInEasing),
                    targetScale = 0.98f
                )
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 6.dp,
                        end = 6.dp,
                        bottom = 6.dp
                    ),
                contentAlignment = Alignment.BottomCenter
            ) {
                val cardHeight = (maxHeight - 6.dp)
                    .coerceAtLeast(360.dp)

                MainWelcomeCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(cardHeight)
                        .graphicsLayer {
                            scaleX = 1f + (t * 0.01f)
                            scaleY = 1f + (t * 0.01f)
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        ),
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
    val isDark = isSystemInDarkTheme()
    val appThemePreset = LocalAppThemePreset.current
    val appThemePalette = LocalAppThemePalette.current
    val shape = mainWelcomeMaterialShape(appThemePreset)
    val buttonShape = mainWelcomeButtonShape(appThemePreset)
    val glassTint = if (isDark) {
        Color(0xFF101824).copy(alpha = 0.66f)
    } else {
        Color(0xFFF7F0E6).copy(alpha = 0.76f)
    }
    val sheetTextColor = if (isDark) Color.White else Color(0xFF17202B)
    val sheetCardColor = if (isDark) {
        Color(0xFF172232).copy(alpha = 0.72f)
    } else {
        Color(0xFFF4E8D8).copy(alpha = 0.88f)
    }
    val sheetBorderColor = if (isDark) {
        Color(0xFF6EC6E8).copy(alpha = 0.28f)
    } else {
        Color(0xFF8A6A45).copy(alpha = 0.24f)
    }
    val stroke = Brush.verticalGradient(
        listOf(
            Color(0xFF6EC6E8).copy(alpha = if (isDark) 0.48f else 0.32f),
            Color(0xFFE6B86A).copy(alpha = if (isDark) 0.34f else 0.30f),
            Color(0xFF7CA6FF).copy(alpha = if (isDark) 0.30f else 0.22f)
        )
    )

    Surface(
        modifier = modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                shadow = null,
                highlight = null,
                effects = {
                    vibrancy()
                    blur(4.dp.toPx(), edgeTreatment = TileMode.Mirror)
                    lens(
                        refractionHeight = 8.dp.toPx(),
                        refractionAmount = 8.dp.toPx(),
                        depthEffect = false,
                        chromaticAberration = false
                    )
                },
                onDrawSurface = {
                    drawRect(glassTint)
                    drawRect(Color(0xFF6EC6E8).copy(alpha = if (isDark) 0.055f else 0.035f))
                    drawRect(Color(0xFFE6B86A).copy(alpha = if (isDark) 0.040f else 0.045f))
                }
            ),
        shape = shape,
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .fillMaxWidth()
                .border(width = 1.dp, brush = stroke, shape = shape)
                .border(width = 1.dp, color = appThemePalette.accent.copy(alpha = if (isDark) 0.20f else 0.18f), shape = shape)
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Welcome to JAC Airport",
                    color = sheetTextColor,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    textAlign = TextAlign.Center
                )

                MainHighlightsGrid(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = 4.dp)
                )

                MainWelcomeUpdateStrip(
                    modifier = Modifier.fillMaxWidth(),
                    surfaceColor = sheetCardColor,
                    borderColor = sheetBorderColor,
                    textColor = if (isDark) Color.White else Color(0xFF17202B),
                    subTextColor = if (isDark) Color.White.copy(alpha = 0.82f) else Color(0xFF344050).copy(alpha = 0.82f)
                )

                Button(
                    onClick = onDone,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) {
                            Color(0xFF233853).copy(alpha = 0.92f)
                        } else {
                            Color(0xFF1F5B7E).copy(alpha = 0.90f)
                        },
                        contentColor = Color.White
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

private fun mainWelcomeFeatureColor(title: String, isDark: Boolean): Color {
    return if (isDark) {
        when (title) {
            "Profile" -> Color(0xFFFFC857)
            "Live airport" -> Color(0xFF4DD8FF)
            "Flights board" -> Color(0xFF78A8FF)
            "AI briefing" -> Color(0xFFFFD166)
            "Alerts and parking" -> Color(0xFFFF5C7A)
            "Transportation" -> Color(0xFF57E6C2)
            "Notes and reminders" -> Color(0xFFD68CFF)
            "Airport services" -> Color(0xFF8FE388)
            "News and notices" -> Color(0xFFFF9F43)
            "Themes and settings" -> Color(0xFFA7B7FF)
            "Updates and feedback" -> Color(0xFFFF8C66)
            "QR tools" -> Color(0xFF64F4AC)
            "Flight and driver apps" -> Color(0xFFFFD60A)
            else -> Color(0xFF8FD9FF)
        }
    } else {
        when (title) {
            "Profile" -> Color(0xFF8A5A00)
            "Live airport" -> Color(0xFF006A8E)
            "Flights board" -> Color(0xFF2857B8)
            "AI briefing" -> Color(0xFF8A5A00)
            "Alerts and parking" -> Color(0xFFB0183A)
            "Transportation" -> Color(0xFF007D67)
            "Notes and reminders" -> Color(0xFF7B2CBF)
            "Airport services" -> Color(0xFF2D711F)
            "News and notices" -> Color(0xFF9A4A00)
            "Themes and settings" -> Color(0xFF4450A8)
            "Updates and feedback" -> Color(0xFF9C3E16)
            "QR tools" -> Color(0xFF087A45)
            "Flight and driver apps" -> Color(0xFF8A6500)
            else -> Color(0xFF0B668A)
        }
    }
}

@Composable
private fun MainHighlightsGrid(modifier: Modifier = Modifier) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(20.dp)
    val welcomeTextColor = if (isDark) Color.White else Color(0xFF17202B)
    val welcomeSubTextColor = if (isDark) Color.White.copy(alpha = 0.82f) else Color(0xFF344050).copy(alpha = 0.78f)
    val surfaceColor = if (isDark) {
        Color(0xFF172232).copy(alpha = 0.72f)
    } else {
        Color(0xFFF4E8D8).copy(alpha = 0.88f)
    }
    val borderColor = if (isDark) {
        Color(0xFF6EC6E8).copy(alpha = 0.28f)
    } else {
        Color(0xFF8A6A45).copy(alpha = 0.24f)
    }
    val items = listOf(
        MainFeature("Live airport", R.drawable.fullscreen_24dp_46152f_fill1_wght400_grad0_opsz24, "See what the airport looks like right now. Check the curb, north lot, or south lot before you drive over or pick someone up."),
        MainFeature("Flights board", R.drawable.flight_24dp_ffffff_fill0_wght400_grad0_opsz24, "Look up today’s arrivals and departures, see when a flight is expected, and spot cancellations or delays before heading to the airport."),
        MainFeature("AI briefing", R.drawable.ic_oui_info, "Start here when you want the short version. Briefing tells you what matters today from flights, weather, notes, and reminders."),
        MainFeature("Alerts and parking", R.drawable.ic_oui_news, "Check the things that can change your plan: weather, parking space, cancelled flights, delayed flights, and airport travel warnings."),
        MainFeature("Transportation", R.drawable.travel_24dp_ffffff_fill1_wght400_grad0_opsz24, "Find a ride to or from JAC. Open taxi, shuttle, ride app, bus, and driving information with phone numbers when available."),
        MainFeature("Notes and reminders", R.drawable.ic_oui_notes, "This is your travel notebook. Add and organize notes, make to-do lists, attach photos or files, record voice notes, and set reminders when something needs to be done later."),
        MainFeature("Airport services", R.drawable.baseline_flight_24, "Quickly find airport contacts, lost and found, operations, services, communications, and FBO information when you need help."),
        MainFeature("News and notices", R.drawable.ic_oui_news, "Read airport updates so you know about construction, service changes, closures, or other travel information from JAC."),
        MainFeature("Themes and settings", R.drawable.ic_oui_settings, "Make the app feel right for you. Change themes, glass style, language, camera behavior, app icon, and account options."),
        MainFeature("Updates and feedback", R.drawable.ic_oui_settings, "Keep the app current, read what changed, send feedback, rate the app, share it, or sign up for notifications."),
        MainFeature("QR tools", R.drawable.ic_oui_qr_code, "Use the QR code to share the app with someone else. They can scan it, open the download page, and install this beautiful JAC Airport app too."),
        MainFeature("Flight and driver apps", R.drawable.travel_16dp_ffffff_fill0_wght400_grad0_opsz20, "Jump to FlightRadar24, FlightAware, Uber Driver, or Lyft Driver from the home page when those tools are useful."),
    )
    var expandedFeature by remember { mutableStateOf<String?>(null) }

    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF6EC6E8).copy(alpha = if (isDark) 0.13f else 0.10f),
                        surfaceColor,
                        Color(0xFFE6B86A).copy(alpha = if (isDark) 0.08f else 0.12f)
                    )
                )
            )
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
                color = welcomeTextColor,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                "${items.size} features",
                color = welcomeSubTextColor,
                style = MaterialTheme.typography.labelSmall
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MainProfilePill(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 1.dp),
                surfaceColor = surfaceColor,
                borderColor = borderColor,
                textColor = welcomeTextColor,
                subTextColor = welcomeSubTextColor,
                expanded = expandedFeature == MainFeature.Profile.text,
                onClick = {
                    expandedFeature = if (expandedFeature == MainFeature.Profile.text) null else MainFeature.Profile.text
                }
            )

            items.forEach { item ->
                MainFeatureExpandableRow(
                    feature = item,
                    surfaceColor = surfaceColor,
                    borderColor = borderColor,
                    textColor = welcomeTextColor,
                    subTextColor = welcomeSubTextColor,
                    expanded = expandedFeature == item.text,
                    onClick = {
                        expandedFeature = if (expandedFeature == item.text) null else item.text
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private data class MainFeature(
    val text: String,
    @param:DrawableRes val iconRes: Int,
    val detail: String
) {
    companion object {
        val Profile = MainFeature(
            text = "Profile",
            iconRes = R.drawable.account_circle_24dp_ffffff_fill1_profile,
            detail = "Sign in when you want your notes, files, reminders, and settings to stay saved and sync online. You can still use the app offline, but signing in helps keep your data alive across devices."
        )
    }
}

@Composable
private fun MainProfilePill(
    modifier: Modifier = Modifier,
    surfaceColor: Color,
    borderColor: Color,
    textColor: Color,
    subTextColor: Color,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    val palette = LocalAppThemePalette.current
    val shape = RoundedCornerShape(18.dp)
    val profileColor = mainWelcomeFeatureColor(MainFeature.Profile.text, isDark)
    val rowStart = if (isDark) Color(0xFF24354C).copy(alpha = 0.62f) else Color(0xFFFFF7EC).copy(alpha = 0.72f)
    val rowEnd = if (isDark) Color(0xFF39425B).copy(alpha = 0.42f) else Color(0xFFE7D1B0).copy(alpha = 0.48f)

    Column(
        modifier
            .animateContentSize(animationSpec = tween(220, easing = FastOutSlowInEasing))
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        rowStart,
                        surfaceColor.copy(alpha = 0.86f),
                        rowEnd
                    )
                )
            )
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(profileColor.copy(alpha = 0.28f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.account_circle_24dp_ffffff_fill1_profile),
                    contentDescription = null,
                    tint = profileColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Profile",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = textColor
                )
                Text(
                    text = "Sign in to keep notes, files, reminders, and settings synced.",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium,
                    color = subTextColor
                )
            }
            Text(
                text = if (expanded) "Close" else "Open",
                color = palette.badgeContent,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(palette.action.copy(alpha = 0.18f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(animationSpec = tween(170, easing = FastOutSlowInEasing)),
            exit = fadeOut(animationSpec = tween(120, easing = FastOutLinearInEasing))
        ) {
            Text(
                text = MainFeature.Profile.detail,
                color = subTextColor,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun MainFeatureExpandableRow(
    feature: MainFeature,
    modifier: Modifier = Modifier,
    surfaceColor: Color,
    borderColor: Color,
    textColor: Color,
    subTextColor: Color,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    val palette = LocalAppThemePalette.current
    val shape = RoundedCornerShape(18.dp)
    val iconColor = mainWelcomeFeatureColor(feature.text, isDark)
    val iconBg = iconColor.copy(alpha = if (expanded) 0.30f else if (isDark) 0.20f else 0.18f)
    val rowStart = if (isDark) Color(0xFF24354C).copy(alpha = if (expanded) 0.68f else 0.54f) else Color(0xFFFFF7EC).copy(alpha = if (expanded) 0.80f else 0.66f)
    val rowEnd = if (isDark) Color(0xFF39425B).copy(alpha = if (expanded) 0.48f else 0.34f) else Color(0xFFE7D1B0).copy(alpha = if (expanded) 0.54f else 0.42f)

    Column(
        modifier
            .animateContentSize(animationSpec = tween(220, easing = FastOutSlowInEasing))
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        rowStart,
                        surfaceColor.copy(alpha = if (expanded) 0.86f else 0.72f),
                        rowEnd
                    )
                )
            )
            .border(1.dp, borderColor.copy(alpha = if (expanded) 0.82f else 0.54f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(feature.iconRes),
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = feature.text,
                    color = textColor,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (expanded) "Tap to close" else feature.detail,
                    color = subTextColor,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = if (expanded) "-" else "+",
                color = palette.badgeContent,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .width(28.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(palette.action.copy(alpha = if (expanded) 0.22f else 0.12f))
                    .padding(vertical = 2.dp)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(animationSpec = tween(170, easing = FastOutSlowInEasing)),
            exit = fadeOut(animationSpec = tween(120, easing = FastOutLinearInEasing))
        ) {
            Text(
                text = feature.detail,
                color = subTextColor,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun MainWelcomeUpdateStrip(
    modifier: Modifier = Modifier,
    surfaceColor: Color,
    borderColor: Color,
    textColor: Color,
    subTextColor: Color,
) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(18.dp)
    val rowStart = if (isDark) Color(0xFF24354C).copy(alpha = 0.58f) else Color(0xFFFFF7EC).copy(alpha = 0.70f)
    val rowEnd = if (isDark) Color(0xFF39425B).copy(alpha = 0.40f) else Color(0xFFE7D1B0).copy(alpha = 0.48f)
    var changelog by remember { mutableStateOf<List<UpdateBlock>?>(null) }

    LaunchedEffect(Unit) {
        changelog = runCatching {
            withContext(Dispatchers.IO) {
                AppUpdateRepository.fetchRemoteUpdate().updates
            }
        }.getOrDefault(
            listOf(
                UpdateBlock(
                    title = "Latest changes",
                    bullets = listOf(
                        "Themed flight tabs and clearer app surfaces",
                        "Transportation info, smarter alerts, and welcome polish"
                    )
                )
            )
        )
    }

    Column(
        modifier
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        rowStart,
                        surfaceColor.copy(alpha = 0.82f),
                        rowEnd
                    )
                )
            )
            .border(1.dp, borderColor, shape)
            .height(188.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "What's new",
                color = textColor,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Changelog",
                color = subTextColor,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFF6EC6E8).copy(alpha = if (isDark) 0.18f else 0.12f))
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            val visibleItems = changelog
            if (visibleItems == null) {
                Text(
                    text = "Loading latest changelog...",
                    color = subTextColor,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                visibleItems.forEach { item ->
                    MainWelcomeChangelogLine(item, textColor, subTextColor)
                }
            }
        }
    }
}

@Composable
private fun MainWelcomeChangelogLine(
    item: UpdateBlock,
    textColor: Color,
    subTextColor: Color
) {
    val firstLine = item.bullets.firstOrNull()
        ?: item.summary.lineSequence().firstOrNull { it.isNotBlank() }
        ?: ""
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(
            text = item.title,
            color = textColor,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (firstLine.isNotBlank()) {
            Text(
                text = firstLine,
                color = subTextColor,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
