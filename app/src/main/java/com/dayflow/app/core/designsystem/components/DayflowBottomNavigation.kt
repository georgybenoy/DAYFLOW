package com.dayflow.app.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dayflow.app.core.designsystem.DayflowTheme
import com.dayflow.app.core.designsystem.DayflowTypography

enum class DayflowTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    TODAY("Today", Icons.Filled.Dashboard, Icons.Outlined.Dashboard, "tab_today"),
    REMINDERS("Reminders", Icons.Filled.Notifications, Icons.Outlined.Notifications, "tab_reminders"),
    TASKS("Tasks", Icons.Filled.CheckCircle, Icons.Outlined.CheckCircle, "tab_tasks"),
    HABITS("Habits", Icons.Filled.CalendarToday, Icons.Outlined.CalendarToday, "tab_habits"),
    INSIGHTS("Insights", Icons.Filled.Insights, Icons.Outlined.Insights, "tab_insights")
}

@Composable
fun DayflowBottomNavigation(
    currentTab: DayflowTab,
    onTabSelected: (DayflowTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DayflowTheme.colors

    NavigationBar(
        modifier = modifier
            .height(72.dp)
            .background(colors.surface)
            .border(width = 1.dp, color = colors.border),
        containerColor = colors.surface,
        contentColor = colors.textPrimary,
        tonalElevation = 0.dp
    ) {
        DayflowTab.values().forEach { tab ->
            val isSelected = tab == currentTab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.label
                    )
                },
                label = {
                    Text(
                        text = tab.label,
                        style = DayflowTypography.labelSmall
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.background,
                    selectedTextColor = colors.primary,
                    indicatorColor = colors.primary,
                    unselectedIconColor = colors.textSecondary,
                    unselectedTextColor = colors.textSecondary
                ),
                modifier = Modifier.testTag(tab.testTag)
            )
        }
    }
}

@Preview(name = "Bottom Nav Dark", showBackground = true, backgroundColor = 0xFF07120F)
@Composable
private fun DayflowBottomNavigationPreview() {
    DayflowTheme {
        DayflowBottomNavigation(
            currentTab = DayflowTab.TODAY,
            onTabSelected = {}
        )
    }
}
