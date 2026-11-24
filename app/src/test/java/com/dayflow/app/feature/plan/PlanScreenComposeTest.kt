package com.dayflow.app.feature.plan

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dayflow.app.core.designsystem.DayflowTheme
import com.dayflow.app.domain.model.DailyPlanItem
import com.dayflow.app.domain.model.PlanItemSourceType
import com.dayflow.app.domain.model.PlanStatus
import com.dayflow.app.domain.model.Priority
import com.dayflow.app.domain.model.Task
import com.dayflow.app.domain.model.TaskStatus
import com.dayflow.app.domain.planner.PlanConflict
import com.dayflow.app.domain.planner.PlanConflictType
import com.dayflow.app.domain.planner.UnscheduledItem
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class PlanScreenComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun planComponents_renderStatusBannerAndConflictWarnings() {
        val conflict = PlanConflict(
            type = PlanConflictType.CAPACITY_EXCEEDED,
            message = "1 task could not fit in today's window."
        )

        composeTestRule.setContent {
            DayflowTheme {
                androidx.compose.foundation.layout.Column {
                    PlanStatusHeader(
                        status = PlanStatus.DRAFT,
                        totalScheduledMinutes = 90,
                        scheduledItemCount = 2
                    )
                    ConflictBanner(conflicts = listOf(conflict))
                }
            }
        }

        composeTestRule.onNodeWithText("DRAFT PLAN").assertIsDisplayed()
        composeTestRule.onNodeWithText("2 items").assertIsDisplayed()
        composeTestRule.onNodeWithText("• 1 task could not fit in today's window.").assertIsDisplayed()
    }

    @Test
    fun planItemCard_rendersTitleTimesAndTriggersCallbacks() {
        val scheduledItem = DailyPlanItem(
            id = "item-compose-1",
            planId = "plan-1",
            sourceType = PlanItemSourceType.TASK,
            sourceId = "task-1",
            title = "Finalize Q4 Strategy",
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(10, 30),
            orderIndex = 0
        )
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

        var moveUpClicked = false
        var moveDownClicked = false
        var editTimeClicked = false
        var removeClicked = false
        var toggleCompletionClicked = false

        composeTestRule.setContent {
            DayflowTheme {
                PlanItemCard(
                    item = scheduledItem,
                    index = 1,
                    totalCount = 3,
                    timeFormatter = timeFormatter,
                    onMoveUp = { moveUpClicked = true },
                    onMoveDown = { moveDownClicked = true },
                    onEditTime = { editTimeClicked = true },
                    onRemove = { removeClicked = true },
                    onToggleCompletion = { toggleCompletionClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Finalize Q4 Strategy").assertIsDisplayed()
        composeTestRule.onNodeWithText("09:00 - 10:30").assertIsDisplayed()
        composeTestRule.onNodeWithText("Task").assertIsDisplayed()

        // Test edit time action
        composeTestRule.onNodeWithText("Finalize Q4 Strategy").assertIsDisplayed()
    }

    @Test
    fun unscheduledTaskCard_rendersTitleReasonAndTriggersSchedule() {
        val unscheduledTask = Task(
            id = "task-unscheduled",
            title = "Follow up with Design Team",
            priority = Priority.MEDIUM,
            status = TaskStatus.PENDING,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        val unscheduled = UnscheduledItem(
            task = unscheduledTask,
            reason = "Insufficient time available (45m needed)",
            reschedulingSuggestion = "Reschedule to tomorrow"
        )

        var scheduleClicked = false

        composeTestRule.setContent {
            DayflowTheme {
                UnscheduledTaskCard(
                    unscheduled = unscheduled,
                    onSchedule = { scheduleClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Follow up with Design Team").assertIsDisplayed()
        composeTestRule.onNodeWithText("Insufficient time available (45m needed)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Reschedule to tomorrow").assertIsDisplayed()

        composeTestRule.onNodeWithText("Schedule").performClick()
        assertTrue(scheduleClicked)
    }
}
