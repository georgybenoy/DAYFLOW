package com.dayflow.app.core.designsystem.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dayflow.app.core.designsystem.DayflowTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DayflowBottomNavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun bottomNavigation_displaysAllTabs_andHandlesSelection() {
        var selectedTab by mutableStateOf(DayflowTab.TODAY)

        composeTestRule.setContent {
            DayflowTheme {
                DayflowBottomNavigation(
                    currentTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
            }
        }

        // Verify all 5 tabs are displayed
        composeTestRule.onNodeWithText("Today").assertIsDisplayed()
        composeTestRule.onNodeWithText("Reminders").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tasks").assertIsDisplayed()
        composeTestRule.onNodeWithText("Habits").assertIsDisplayed()
        composeTestRule.onNodeWithText("Insights").assertIsDisplayed()

        // Today is initially selected
        composeTestRule.onNodeWithTag("tab_today").assertIsSelected()

        // Click Reminders tab
        composeTestRule.onNodeWithTag("tab_reminders").performClick()

        // Verify state changed
        assertEquals(DayflowTab.REMINDERS, selectedTab)
        composeTestRule.onNodeWithTag("tab_reminders").assertIsSelected()
    }
}
