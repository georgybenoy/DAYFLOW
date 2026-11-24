package com.dayflow.app.feature.insights

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.dayflow.app.core.designsystem.DayflowTheme
import com.dayflow.app.domain.analytics.DailyAnalytics
import com.dayflow.app.domain.analytics.InsightsData
import com.dayflow.app.domain.analytics.MonthlyAnalytics
import com.dayflow.app.domain.analytics.StreakSummary
import com.dayflow.app.domain.analytics.WeeklyAnalytics
import com.dayflow.app.domain.model.WeekStartDay
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
class InsightsScreenComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testDate = LocalDate.of(2026, 11, 19)

    private val sampleData = InsightsData(
        today = DailyAnalytics(
            date = testDate,
            executionScore = 88,
            hasActivity = true,
            tasksCompleted = 4,
            tasksTotal = 5,
            taskRatio = 0.8f,
            habitsCompleted = 2,
            habitsScheduled = 2,
            habitRatio = 1.0f,
            remindersCompleted = 3,
            remindersMissed = 0,
            reminderRatio = 1.0f,
            planItemsCompleted = 1,
            planItemsTotal = 1,
            planRatio = 1.0f
        ),
        weekly = WeeklyAnalytics(
            startDate = testDate.minusDays(3),
            endDate = testDate.plusDays(3),
            averageScore = 82,
            days = emptyList(),
            totalTasksCompleted = 15,
            habitConsistencyPercentage = 92,
            completedRemindersCount = 14,
            missedRemindersCount = 1
        ),
        monthly = MonthlyAnalytics(
            yearMonth = YearMonth.from(testDate),
            days = emptyList(),
            averageScore = 80,
            totalTasksCompleted = 48,
            habitConsistencyPercentage = 90
        ),
        streakSummary = StreakSummary(
            currentStreakDays = 6,
            longestStreakDays = 21
        ),
        trendDays = emptyList()
    )

    @Test
    fun insightsContent_successState_rendersExecutionScoreAndMetrics() {
        var selectedFilter: InsightsViewFilter? = null

        composeTestRule.setContent {
            DayflowTheme {
                InsightsContent(
                    uiState = InsightsUiState.Success(
                        data = sampleData,
                        weekStartDay = WeekStartDay.MONDAY,
                        selectedFilter = InsightsViewFilter.WEEK
                    ),
                    onFilterSelected = { selectedFilter = it },
                    onRefresh = {},
                    onNavigateBack = {}
                )
            }
        }

        // Execution score hero
        composeTestRule.onNodeWithText("DAILY EXECUTION SCORE").assertIsDisplayed()
        composeTestRule.onNodeWithText("APP-DEFINED METRIC").assertIsDisplayed()
        composeTestRule.onNodeWithText("88").assertIsDisplayed()
        composeTestRule.onNodeWithText("/100").assertIsDisplayed()

        // Summary metric cards
        composeTestRule.onNodeWithText("Tasks Completed").assertExists()
        composeTestRule.onNodeWithText("15").assertExists()
        composeTestRule.onNodeWithText("Habit Consistency").assertExists()
        composeTestRule.onNodeWithText("92%").assertExists()
        composeTestRule.onNodeWithText("Current Streak").assertExists()
        composeTestRule.onNodeWithText("6d").assertExists()

        // Filter chips interaction
        composeTestRule.onNodeWithText("Month").performClick()
        assertEquals(InsightsViewFilter.MONTH, selectedFilter)
    }

    @Test
    fun insightsContent_errorState_rendersErrorMessage() {
        composeTestRule.setContent {
            DayflowTheme {
                InsightsContent(
                    uiState = InsightsUiState.Error("Network offline error"),
                    onFilterSelected = {},
                    onRefresh = {},
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Could not load insights").assertIsDisplayed()
        composeTestRule.onNodeWithText("Network offline error").assertIsDisplayed()
    }
}
