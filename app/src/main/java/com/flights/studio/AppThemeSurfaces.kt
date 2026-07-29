package com.flights.studio

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

@Composable
fun AppThemeSectionSurface(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(18.dp),
    content: @Composable () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val palette = LocalAppThemePalette.current
    val roles = appThemeSurfaceRoles(palette, isDark)
    val tintBrush = Brush.linearGradient(
        colors = listOf(
            roles.glassCard.copy(alpha = 0.42f),
            palette.surfaceVariant.copy(alpha = if (isDark) 0.24f else 0.20f),
            palette.action.copy(alpha = if (isDark) 0.12f else 0.10f)
        ),
        start = Offset.Zero,
        end = Offset(720f, 520f)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(roles.card, shape)
            .background(tintBrush, shape)
            .border(BorderStroke(1.dp, roles.border), shape)
    ) {
        content()
    }
}
