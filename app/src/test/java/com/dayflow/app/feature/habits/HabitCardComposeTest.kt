package com.dayflow.app.feature.habits

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dayflow.app.core.designsystem.DayflowTheme
import com.dayflow.app.domain.model.Category
import com.dayflow.app.domain.model.Habit
import com.dayflow.app.domain.model.HabitTrackingType
import com.dayflow.app.domain.recurrence.HabitProgress
import com.dayflow.app.domain.recurrence.StreakResult
import com.dayflow.app.feature.habits.components.HabitCard
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HabitCardComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun habitCard_rendersNameCategoryStreakAndTriggersActions() {
        val habit = Habit(
            id = "habit-compose-1",
            name = "Morning Meditation",
            description = "15 minutes mindfulness",
            trackingType = HabitTrackingType.BINARY,
            targetValue = 1.0
        )
        val category = Category(
            id = "cat-wellness",
            name = "Wellness",
            colorHex = "#76CDBB",
            iconName = "SelfImprovement"
        )
        val progress = HabitProgress(
            completedValue = 1.0,
            targetValue = 1.0,
            isCompleted = true,
            progressRatio = 1.0f,
            formattedProgress = "Completed"
        )
        val streak = StreakResult(currentStreak = 7, longestStreak = 14)

        var cardClicked = false
        var toggleClicked = false
        var editClicked = false

        composeTestRule.setContent {
            DayflowTheme {
                HabitCard(
                    habit = habit,
                    category = category,
                    progress = progress,
                    streak = streak,
                    weeklyStatus = emptyList(),
                    isScheduledToday = true,
                    onCardClick = { cardClicked = true },
                    onToggleComplete = { toggleClicked = true },
                    onOpenQuickLog = {},
                    onEdit = { editClicked = true },
                    onTogglePause = {},
                    onDelete = {}
                )
            }
        }

        // Verify elements rendered
        composeTestRule.onNodeWithText("Morning Meditation").assertIsDisplayed()
        composeTestRule.onNodeWithText("15 minutes mindfulness").assertIsDisplayed()
        composeTestRule.onNodeWithText("Wellness").assertIsDisplayed()
        composeTestRule.onNodeWithText("Binary").assertIsDisplayed()
        composeTestRule.onNodeWithText("7d").assertIsDisplayed()
        composeTestRule.onNodeWithText("Completed").assertIsDisplayed()

        // Click complete toggle
        composeTestRule.onNodeWithContentDescription("Completed").performClick()
        assertTrue("Toggle button should fire callback", toggleClicked)

        // Click card
        composeTestRule.onNodeWithText("Morning Meditation").performClick()
        assertTrue("Card click should fire callback", cardClicked)

        // Overflow menu
        composeTestRule.onNodeWithContentDescription("Habit options").performClick()
        composeTestRule.onNodeWithText("Edit Habit").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pause Habit").assertIsDisplayed()
        composeTestRule.onNodeWithText("Delete").assertIsDisplayed()

        composeTestRule.onNodeWithText("Edit Habit").performClick()
        assertTrue("Edit option should fire callback", editClicked)
    }
}
