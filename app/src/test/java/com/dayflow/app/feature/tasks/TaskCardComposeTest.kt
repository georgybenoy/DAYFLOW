package com.dayflow.app.feature.tasks

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dayflow.app.core.designsystem.DayflowTheme
import com.dayflow.app.domain.model.Category
import com.dayflow.app.domain.model.Priority
import com.dayflow.app.domain.model.Task
import com.dayflow.app.domain.model.TaskStatus
import com.dayflow.app.domain.model.TaskSubtask
import com.dayflow.app.feature.tasks.components.TaskCard
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
class TaskCardComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun taskCard_rendersTitlePriorityBadgeCategoryAndSubtaskProgress() {
        val sampleTask = Task(
            id = "task-card-1",
            title = "Final Project Report",
            description = "Complete conclusion section",
            priority = Priority.URGENT,
            deadlineDate = LocalDate.now(),
            deadlineTime = LocalTime.of(17, 0),
            estimatedDurationMinutes = 60,
            status = TaskStatus.PENDING,
            subtasks = listOf(
                TaskSubtask(id = "sub-1", taskId = "task-card-1", title = "Write conclusion", isCompleted = true),
                TaskSubtask(id = "sub-2", taskId = "task-card-1", title = "Proofread bibliography", isCompleted = false)
            )
        )
        val sampleCat = Category(id = "cat-1", name = "Academics", colorHex = "#76CDBB", iconName = "School")

        var taskClicked = false
        var toggleCompleteClicked = false
        var editClicked = false
        var rescheduleClicked = false
        var deleteClicked = false

        composeTestRule.setContent {
            DayflowTheme {
                TaskCard(
                    task = sampleTask,
                    isOverdue = false,
                    category = sampleCat,
                    onTaskClick = { taskClicked = true },
                    onToggleComplete = { toggleCompleteClicked = true },
                    onEditClick = { editClicked = true },
                    onRescheduleClick = { rescheduleClicked = true },
                    onDeleteClick = { deleteClicked = true }
                )
            }
        }

        // Verify title & details rendered
        composeTestRule.onNodeWithText("Final Project Report").assertIsDisplayed()
        composeTestRule.onNodeWithText("Complete conclusion section").assertIsDisplayed()
        composeTestRule.onNodeWithText("URGENT").assertIsDisplayed()
        composeTestRule.onNodeWithText("Academics").assertIsDisplayed()
        composeTestRule.onNodeWithText("1 of 2 subtasks").assertIsDisplayed()
        composeTestRule.onNodeWithText("50%").assertIsDisplayed()

        // Verify clicking the complete button
        composeTestRule.onNodeWithContentDescription("Mark Final Project Report as completed").performClick()
        assertTrue("Complete button should invoke onToggleComplete", toggleCompleteClicked)

        // Verify clicking the card
        composeTestRule.onNodeWithText("Final Project Report").performClick()
        assertTrue("Card click should invoke onTaskClick", taskClicked)

        // Verify overflow menu options
        composeTestRule.onNodeWithContentDescription("Task options for Final Project Report").performClick()
        composeTestRule.onNodeWithText("Edit Task").assertIsDisplayed()
        composeTestRule.onNodeWithText("Reschedule").assertIsDisplayed()
        composeTestRule.onNodeWithText("Delete").assertIsDisplayed()

        composeTestRule.onNodeWithText("Edit Task").performClick()
        assertTrue("Edit click should invoke onEditClick", editClicked)
    }
}
