package com.flights.studio

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.LayerBackdrop
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

@Composable
fun <T> BottomTabs(
    tabs: List<T>,
    selectedTabState: MutableState<T>,
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier,
    tabIconRes: (T) -> Int,
    tabLabel: (T) -> String,
) {
    if (tabs.isEmpty()) return

    val isDark = isSystemInDarkTheme()

    val labelColor = if (isDark) Color.White else Color(0xFF111111)
    val iconColorFilter = remember(labelColor) { ColorFilter.tint(labelColor) }

    val labelStyle = remember(isDark) {
        TextStyle(
            fontSize = 12.sp,
            shadow = Shadow(
                color = if (isDark) Color.Black.copy(alpha = 0.85f)
                else Color.White.copy(alpha = 0.65f),
                offset = Offset(0f, 1f),
                blurRadius = 3f
            )
        )
    }

    // index state (drives LiquidBottomTabs + click)
    var selectedTabIndex by rememberSaveable(tabs) {
        val initial = tabs.indexOf(selectedTabState.value).let { if (it >= 0) it else 0 }
        mutableIntStateOf(initial.coerceIn(0, tabs.lastIndex))
    }

    // keep selectedTabIndex in sync if external state changes
    LaunchedEffect(tabs, selectedTabState) {
        snapshotFlow { selectedTabState.value }
            .map { value -> tabs.indexOf(value).takeIf { it >= 0 } }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { idx ->
                val clamped = idx.coerceIn(0, tabs.lastIndex)
                if (clamped != selectedTabIndex) selectedTabIndex = clamped
            }
    }

    // when tabs list changes, ensure index + state remain valid
    LaunchedEffect(tabs) {
        val clamped = selectedTabIndex.coerceIn(0, tabs.lastIndex)
        if (clamped != selectedTabIndex) selectedTabIndex = clamped

        val tab = tabs[clamped]
        if (selectedTabState.value != tab) selectedTabState.value = tab
    }

    LiquidBottomTabs(
        selectedTabIndex = { selectedTabIndex },
        onTabSelected = { idx ->
            val clamped = idx.coerceIn(0, tabs.lastIndex)
            if (clamped != selectedTabIndex) selectedTabIndex = clamped

            val tab = tabs[clamped]
            if (selectedTabState.value != tab) selectedTabState.value = tab
        },
        backdrop = backdrop,
        tabsCount = tabs.size,
        modifier = modifier,
    ) {
        repeat(tabs.size) { index ->
            val tab = tabs[index]

            LiquidBottomTab(
                onClick = {
                    if (index != selectedTabIndex) {
                        selectedTabIndex = index
                        selectedTabState.value = tab
                    }
                }
            ) {
                Column(
                    modifier = Modifier
                        .heightIn(min = 56.dp)
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        Modifier
                            .size(28.dp)
                            .paint(
                                painter = painterResource(tabIconRes(tab)),
                                colorFilter = iconColorFilter
                            )
                    )

                    androidx.compose.material3.Text(
                        text = tabLabel(tab),
                        color = labelColor,
                        style = labelStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
