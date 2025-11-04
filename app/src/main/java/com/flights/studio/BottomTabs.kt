package com.flights.studio

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.kyant.backdrop.Backdrop

@Composable
fun <T> BottomTabs(
    tabs: List<T>,
    selectedTabState: MutableState<T>,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    content: BottomTabsScope.(tab: T) -> BottomTabsScope.BottomTab
) {
    val scope = remember { BottomTabsScope() }

    val isDark = isSystemInDarkTheme()
    val tabTextColor = if (isDark) Color.White else Color.Black

    // Map state -> index for LiquidBottomTabs
    val selectedIndexProvider: () -> Int = {
        val idx = tabs.indexOf(selectedTabState.value)
        when {
            tabs.isEmpty() -> 0
            idx < 0        -> 0
            else           -> idx.coerceIn(0, tabs.lastIndex)
        }
    }

    val selectedIndex = selectedIndexProvider()

    LiquidBottomTabs(
        selectedTabIndex = selectedIndexProvider,
        onTabSelected = { index: Int ->
            if (index in tabs.indices) {
                selectedTabState.value = tabs[index]
            }
        },
        backdrop = backdrop,
        tabsCount = tabs.size,
        modifier = modifier
    ) {
        // RowScope content for LiquidBottomTabs
        tabs.forEachIndexed { index, tab ->
            val isSelected = index == selectedIndex

            scope.content(tab).Content(
                contentColor = { tabTextColor },
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null  // keep it clean, pill animation is the feedback
                    ) {
                        if (!isSelected && index in tabs.indices) {
                            // Update external state; LiquidBottomTabs will
                            // observe the new selected index and animate the pill.
                            selectedTabState.value = tabs[index]
                        }
                    }
            )
        }
    }
}
