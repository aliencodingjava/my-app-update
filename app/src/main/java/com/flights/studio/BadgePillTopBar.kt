package com.flights.studio.com.flights.studio

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.flights.studio.rememberUiScales
import com.flights.studio.us

@Composable
fun BadgePillTopBar(
    text: String,
    modifier: Modifier = Modifier
) {
    val scales = rememberUiScales()
    val badgeStyle = MaterialTheme.typography.labelSmall

    val minW = 2.dp.us(scales.body) // ✅ looks like 2-digit width

    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier
    ) {
        Text(
            text = text,
            style = badgeStyle.copy(
                fontSize = badgeStyle.fontSize.us(scales.label)
            ),
            modifier = Modifier
                .widthIn(min = minW) // ✅ keep pill width stable
                .padding(
                    horizontal = 5.dp.us(scales.body),
                    vertical = 1.dp.us(scales.body)
                ),
            maxLines = 1
        )
    }
}
