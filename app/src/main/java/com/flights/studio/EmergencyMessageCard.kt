package com.flights.studio

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.capsule.ContinuousCapsule

@Composable
fun EmergencyMessageCard(
    message: EmergencyMessage?,
    backdrop: LayerBackdrop,
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    val payload = message ?: EmergencyMessage.Disabled
    val severity = remember(payload.severity) { EmergencySeverity.from(payload.severity) }
    val shape = RoundedCornerShape(30.dp)

    AnimatedVisibility(
        visible = visible && payload.enabled,
        modifier = modifier,
        enter = fadeIn(tween(180, easing = FastOutSlowInEasing)) +
            slideInVertically(
                animationSpec = tween(220, easing = FastOutSlowInEasing),
                initialOffsetY = { -it / 2 }
            ),
        exit = fadeOut(tween(140, easing = FastOutSlowInEasing)) +
            slideOutVertically(
                animationSpec = tween(160, easing = FastOutSlowInEasing),
                targetOffsetY = { -it / 3 }
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { shape },
                    highlight = null,
                    effects = {
                        vibrancy()
                        blur(10f.dp.toPx())
                        lens(18f.dp.toPx(), 42f.dp.toPx())
                    },
                    onDrawSurface = {
                        drawRect(severity.surface)
                        drawRect(Color.White.copy(alpha = severity.surfaceShine))
                    }
                )
                .background(severity.overlay, shape)
                .padding(start = 14.dp, top = 12.dp, end = 8.dp, bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(ContinuousCapsule)
                        .background(severity.accent.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = severity.icon,
                        contentDescription = null,
                        tint = severity.accent,
                        modifier = Modifier.size(23.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 1.dp)
                ) {
                    Text(
                        text = payload.title.ifBlank { "Important update" },
                        color = severity.text,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (payload.body.isNotBlank()) {
                        Text(
                            text = payload.body,
                            color = severity.text.copy(alpha = 0.78f),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    if (payload.actionLabel.isNotBlank() && payload.actionUrl.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .padding(top = 10.dp)
                                .clip(ContinuousCapsule)
                                .background(severity.accent.copy(alpha = 0.18f))
                                .clickable(role = Role.Button) {
                                    runCatching { uriHandler.openUri(payload.actionUrl) }
                                }
                                .padding(horizontal = 13.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = payload.actionLabel,
                                color = severity.accent,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(2.dp))
                IconButton(onClick = onDismiss, modifier = Modifier.size(38.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Dismiss emergency message",
                        tint = severity.text.copy(alpha = 0.72f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private data class EmergencySeverity(
    val accent: Color,
    val surface: Color,
    val overlay: Color,
    val text: Color,
    val surfaceShine: Float,
    val icon: ImageVector
) {
    companion object {
        fun from(value: String): EmergencySeverity {
            return when (value.lowercase()) {
                "critical", "danger" -> EmergencySeverity(
                    accent = Color(0xFFFF6B6B),
                    surface = Color(0xFF2A141B).copy(alpha = 0.90f),
                    overlay = Color(0xFFFF6B6B).copy(alpha = 0.07f),
                    text = Color.White,
                    surfaceShine = 0.05f,
                    icon = Icons.Filled.WarningAmber
                )
                "warning", "alert" -> EmergencySeverity(
                    accent = Color(0xFFFFC857),
                    surface = Color(0xFF2A2113).copy(alpha = 0.90f),
                    overlay = Color(0xFFFFC857).copy(alpha = 0.07f),
                    text = Color.White,
                    surfaceShine = 0.06f,
                    icon = Icons.Filled.WarningAmber
                )
                else -> EmergencySeverity(
                    accent = Color(0xFF59C9F8),
                    surface = Color(0xFF182532).copy(alpha = 0.88f),
                    overlay = Color(0xFF59C9F8).copy(alpha = 0.08f),
                    text = Color.White,
                    surfaceShine = 0.06f,
                    icon = Icons.Filled.Info
                )
            }
        }
    }
}
