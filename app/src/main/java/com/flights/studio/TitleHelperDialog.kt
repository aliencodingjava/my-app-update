package com.flights.studio

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty


@Composable
fun TitleHelperDialog(
    meta: TipMeta,
    title: String,
    expanded: Boolean,
    compactIntro: Boolean,
    disableTips: Boolean,
    pickedIndex: Int,
    suggestions: List<TitleSuggestion>,
    aiLoading: Boolean,
    aiError: String?,
    onPick: (Int) -> Unit,
    onEnterExpanded: () -> Unit,
    onToggleDisable: () -> Unit,
    onDismiss: () -> Unit,
    onPrimary: () -> Unit,
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {

        val cs = MaterialTheme.colorScheme
        val shape = RoundedCornerShape(22.dp)
        val isDark = androidx.compose.foundation.isSystemInDarkTheme()

        val lottieColor = when {
            disableTips -> cs.onSurfaceVariant        // “off” muted
            isDark -> cs.onPrimaryContainer           // bright in dark
            else -> cs.primary                        // strong in light (fix vizibilitate)
        }
        val dynamicProps = rememberLottieDynamicProperties(
            rememberLottieDynamicProperty(
                property = LottieProperty.COLOR,
                value = lottieColor.toArgb(),
                keyPath = arrayOf("**", "Fill 1", "**")
            )
        )

        val iconBg = when {
            disableTips -> cs.errorContainer
            isDark -> cs.primaryContainer
            else -> cs.primaryContainer
        }

        val iconBorder = when {
            disableTips -> cs.outlineVariant.copy(alpha = 0.35f)
            else -> cs.primary.copy(alpha = 0.45f)        // mai “crispy” în light
        }
        Surface(
            shape = shape,
            color = cs.surface,
            tonalElevation = 4.dp,
            shadowElevation = 8.dp,

            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 0.dp)
                .widthIn(max = 520.dp)
        ) {
            Column(Modifier.padding(16.dp)) {

                // ---------- Header ----------
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = iconBg,
                        border = BorderStroke(1.dp, iconBorder)
                    ) {
                        Box(
                            modifier = Modifier.padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val composition by rememberLottieComposition(
                                LottieCompositionSpec.RawRes(R.raw.modelanim)
                            )

                            val speed = if (aiLoading && !disableTips) 1.0f else 0.35f
                            val iterations = if (disableTips) 1 else LottieConstants.IterateForever

                            LottieAnimation(
                                composition = composition,
                                iterations = iterations,
                                speed = speed,
                                dynamicProperties = dynamicProps,
                                modifier = Modifier.size(25.dp)
                            )
                        }
                    }


                    Spacer(Modifier.width(12.dp))

                    Column(Modifier.weight(1f)) {
                        Text(
                            text = meta.headline,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = cs.onSurface
                        )
                        Text(
                            text = meta.reason,
                            style = MaterialTheme.typography.labelSmall,
                            color = cs.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                Spacer(Modifier.height(12.dp))



                // ---------- Intro card (compact) ----------
                if (compactIntro) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = cs.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                text = meta.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = cs.onSurfaceVariant
                            )

                            Spacer(Modifier.height(10.dp))

                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = cs.primary,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { onEnterExpanded() }
                                ) {
                                    Row(
                                        Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            "Choose title",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = cs.onPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = cs.surface,
                                    border = BorderStroke(1.dp, cs.outlineVariant),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { onDismiss() }
                                ) {
                                    Row(
                                        Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            "Skip",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = cs.onSurface,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                }

                // ---------- “More options” gate (only before expand) ----------
                if (!expanded) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = cs.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onEnterExpanded() }
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "More options",
                                style = MaterialTheme.typography.labelLarge,
                                color = cs.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.weight(1f))
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowDown,
                                contentDescription = null,
                                tint = cs.onSurfaceVariant
                            )
                        }
                    }
                }

                // ---------- Expanded content ----------
                if (expanded) {

                    Spacer(Modifier.height(12.dp))

                    // Section title row
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Suggested titles",
                            style = MaterialTheme.typography.labelLarge,
                            color = cs.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.weight(1f))
                        if (title.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = cs.surfaceVariant,
                                border = BorderStroke(1.dp, cs.outlineVariant)
                            ) {
                                Text(
                                    text = "Current: ${title.take(18)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = cs.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))

                    HorizontalDivider(
                        thickness = 1.5.dp,
                        color = cs.outlineVariant.copy(alpha = 0.95f)
                    )

                    Spacer(Modifier.height(8.dp))


                    // AI status
                    // AI status
                    if (aiLoading) {
                        Text("Thinking…", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                    } else if (aiError != null) {
                        Text("AI unavailable • using smart suggestions", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                    }

                    // ✅ Hint ONLY when you actually have many items
                    if (suggestions.size > 6) {
                        Text(
                            text = "Scroll to see more suggestions",
                            style = MaterialTheme.typography.labelSmall,
                            color = cs.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }


                    // Suggestions list (professional selectable rows)
                    val scroll = rememberScrollState()
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = cs.surfaceVariant.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, cs.outlineVariant.copy(alpha = 0.35f))
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                                .verticalScroll(scroll)
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            suggestions.forEachIndexed { idx, s ->
                                val selected = idx == pickedIndex
                                val rowBg =
                                    if (selected) cs.primary.copy(alpha = 0.10f) else cs.surfaceVariant
                                val rowBorder = if (selected)
                                    BorderStroke(1.dp, cs.primary.copy(alpha = 0.35f))
                                else
                                    BorderStroke(1.dp, cs.outlineVariant.copy(alpha = 0.65f))

                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = rowBg,
                                    border = rowBorder,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { onPick(idx) }
                                ) {
                                    Row(
                                        Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(999.dp),
                                            color = if (selected) cs.primary else cs.surface,
                                            border = if (selected) null else BorderStroke(
                                                1.dp,
                                                cs.outlineVariant
                                            ),
                                            modifier = Modifier.size(22.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                if (selected) {
                                                    Icon(
                                                        Icons.Filled.Check,
                                                        contentDescription = null,
                                                        tint = cs.onPrimary,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(Modifier.width(10.dp))

                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                text = s.title,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = cs.onSurface,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = s.why,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = cs.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    HorizontalDivider(
                        thickness = 1.5.dp,
                        color = cs.outlineVariant.copy(alpha = 0.95f)
                    )
                    Spacer(Modifier.height(12.dp))

                    // Stop suggestions (Switch - clear UX)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (disableTips) cs.primary.copy(alpha = 0.10f) else cs.surfaceVariant,
                        border = BorderStroke(1.dp, cs.outlineVariant.copy(alpha = 0.65f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onToggleDisable() } // ✅ tap row toggles too (optional)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.VisibilityOff,
                                contentDescription = null,
                                tint = if (disableTips) cs.error else cs.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )

                            Spacer(Modifier.width(10.dp))

                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "Stop showing suggestions",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (disableTips) cs.onErrorContainer else cs.onSurface,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (disableTips)
                                        "Title helper is turned off"
                                    else
                                        "Turn off title helper",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (disableTips)
                                        cs.onErrorContainer.copy(alpha = 0.85f)
                                    else
                                        cs.onSurfaceVariant
                                )
                            }

                            Switch(
                                checked = disableTips,
                                onCheckedChange = { onToggleDisable() },
                                colors = SwitchDefaults.colors(
                                    // 🔴 ON = danger
                                    checkedThumbColor = cs.onError,
                                    checkedTrackColor = cs.error,
                                    checkedBorderColor = cs.error,

                                    // ⚪ OFF = normal
                                    uncheckedThumbColor = cs.onSurfaceVariant,
                                    uncheckedTrackColor = cs.outlineVariant.copy(alpha = 0.6f),
                                    uncheckedBorderColor = cs.outlineVariant.copy(alpha = 0.6f)
                                )
                            )

                        }
                    }


                    Spacer(Modifier.height(14.dp))
// Bottom action bar (one premium action)
// ✅ Active look only after user actually selects a suggestion
                    val hasChosenTitle = pickedIndex >= 0 && !disableTips

                    val picked = suggestions.getOrNull(pickedIndex) ?: suggestions.firstOrNull()

                    val actionText = when {
                        disableTips -> "Turn off"
                        title.isBlank() -> "Use title"
                        else -> "Replace title"
                    }

                    val preview = when {
                        disableTips ->
                            "Title helper will be turned off"   // 🔴

                        picked != null ->
                            "Use “${picked.title.take(26)}”"    // 🟣

                        else ->
                            "Choose a title above"               // ⚪ guidance
                    }

// ✅ COLORS change when user chooses a title
                    val barColor = when {
                        disableTips -> cs.error        // 🔴 DANGER
                        hasChosenTitle -> cs.primary
                        else -> cs.surfaceVariant
                    }

                    val borderColor = when {
                        disableTips -> cs.outlineVariant.copy(alpha = 0.65f)
                        hasChosenTitle -> cs.primary.copy(alpha = 0.35f)
                        else -> cs.outlineVariant.copy(alpha = 0.65f)
                    }

                    val titleColor = when {
                        disableTips -> cs.onError
                        hasChosenTitle -> cs.onPrimary
                        else -> cs.onSurfaceVariant
                    }

                    val previewColor = when {
                        disableTips -> cs.onError.copy(alpha = 0.90f)          // ✅ pe red
                        hasChosenTitle -> cs.onPrimary.copy(alpha = 0.85f)
                        else -> cs.onSurfaceVariant
                    }

                    val iconTint = when {
                        disableTips -> cs.onError
                        hasChosenTitle -> cs.onPrimary
                        else -> cs.onSurfaceVariant.copy(alpha = 0.6f)
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = barColor,
                        border = BorderStroke(1.dp, borderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onPrimary() }
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = actionText,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = titleColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = preview,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = previewColor
                                )
                            }

                            Icon(
                                imageVector = if (disableTips) Icons.Filled.Block else Icons.Filled.Check,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                }
            }
        }
    }
}


