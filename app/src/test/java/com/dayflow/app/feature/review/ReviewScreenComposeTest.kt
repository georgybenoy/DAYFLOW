package com.dayflow.app.feature.review

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dayflow.app.core.designsystem.DayflowTheme
import com.dayflow.app.domain.model.Priority
import com.dayflow.app.domain.model.ReviewDecisionType
import com.dayflow.app.domain.model.Task
import com.dayflow.app.domain.model.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class ReviewScreenComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun unfinishedTaskReviewCard_rendersTitleAndDecisionChips() {
        val task = Task(
            id = "t_rev_1",
            title = "Prepare Q3 Report",
            priority = Priority.HIGH,
            status = TaskStatus.PENDING,
            plannedDate = LocalDate.now(),
            deadlineDate = LocalDate.now().plusDays(1)
        )

        var selectedDecision: ReviewDecisionType? = null

        composeTestRule.setContent {
            DayflowTheme {
                UnfinishedTaskReviewCard(
                    task = task,
                    decision = ReviewDecisionType.CARRIED_FORWARD,
                    rescheduledDate = null,
                    onSelectDecision = { selectedDecision = it },
                    onRequestReschedule = {}
                )
            }
        }

        // Assert title and decision chips are displayed
        composeTestRule.onNodeWithText("Prepare Q3 Report").assertIsDisplayed()
        composeTestRule.onNodeWithText("Carry Forward").assertIsDisplayed()
        composeTestRule.onNodeWithText("Reschedule").assertIsDisplayed()
        composeTestRule.onNodeWithText("Complete").assertIsDisplayed()
        composeTestRule.onNodeWithText("Exclude").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()

        // Click Complete chip
        composeTestRule.onNodeWithText("Complete").performClick()
        assertEquals(ReviewDecisionType.COMPLETED, selectedDecision)
    }

    @Test
    fun reviewSummaryCard_rendersStatistics() {
        val state = ReviewUiState(
            date = LocalDate.now(),
            completedTasks = listOf(
                Task(id = "1", title = "T1", status = TaskStatus.COMPLETED)
            ),
            unfinishedTasks = listOf(
                Task(id = "2", title = "T2", status = TaskStatus.PENDING),
                Task(id = "3", title = "T3", status = TaskStatus.PENDING)
            ),
            completedHabitsCount = 3,
            totalScheduledHabitsCount = 4,
            isExistingReviewSaved = true
        )

        composeTestRule.setContent {
            DayflowTheme {
                ReviewSummaryCard(uiState = state)
            }
        }

        composeTestRule.onNodeWithText("Daily Summary").assertIsDisplayed()
        composeTestRule.onNodeWithText("1 Completed").assertIsDisplayed()
        composeTestRule.onNodeWithText("2 Unfinished").assertIsDisplayed()
        composeTestRule.onNodeWithText("3/4 Habits").assertIsDisplayed()
        composeTestRule.onNodeWithText("Review Saved").assertIsDisplayed()
    }

    @Test
    fun tomorrowPlanPreviewCard_rendersCounts() {
        val tomorrow = LocalDate.now().plusDays(1)

        composeTestRule.setContent {
            DayflowTheme {
                TomorrowPlanPreviewCard(
                    tomorrowDate = tomorrow,
                    carriedForwardCount = 2,
                    rescheduledCount = 1,
                    newCommitmentsCount = 3
                )
            }
        }

        composeTestRule.onNodeWithText("Tomorrow's Plan Preview").assertIsDisplayed()
        composeTestRule.onNodeWithText("2 Carried Forward").assertIsDisplayed()
        composeTestRule.onNodeWithText("1 Rescheduled").assertIsDisplayed()
        composeTestRule.onNodeWithText("3 New").assertIsDisplayed()
    }
}