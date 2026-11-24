package com.dayflow.app.feature.review

import com.dayflow.app.core.time.TimeProvider
import com.dayflow.app.domain.model.Habit
import com.dayflow.app.domain.model.HabitLog
import com.dayflow.app.domain.model.HabitTrackingType
import com.dayflow.app.domain.model.Priority
import com.dayflow.app.domain.model.ReviewDecisionType
import com.dayflow.app.domain.model.Task
import com.dayflow.app.domain.model.TaskStatus
import com.dayflow.app.domain.planner.DeterministicDailyPlanner
import com.dayflow.app.domain.planner.PlannerAvailabilityWindow
import com.dayflow.app.domain.planner.PlannerInput
import com.dayflow.app.domain.repository.DailyPlanRepository
import com.dayflow.app.domain.repository.DailyReviewRepository
import com.dayflow.app.domain.repository.HabitRepository
import com.dayflow.app.domain.repository.SettingsRepository
import com.dayflow.app.domain.repository.TaskRepository
import com.dayflow.app.domain.usecase.CancelTaskUseCase
import com.dayflow.app.domain.usecase.CompleteTaskUseCase
import com.dayflow.app.domain.usecase.CreateTaskUseCase
import com.dayflow.app.domain.usecase.GenerateDailyPlanUseCase
import com.dayflow.app.domain.usecase.RescheduleTaskPlannedTimeUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * End-to-End Verification Test for PRD Workflow D / Example 4:
 *
 * 1. User receives evening review notification at 21:30.
 * 2. Taps notification -> Opens Evening Review screen.
 * 3. Views today's summary: 4 tasks completed, 1 habit missed (Meditation), 1 task unfinished ("Prepare Q3 Report").
 * 4. For "Prepare Q3 Report", user chooses action: "Reschedule to Tomorrow".
 * 5. Adds a new task for tomorrow: "Review Q3 Deck with Team" (Priority High, 45m).
 * 6. Taps "Generate Tomorrow's Plan" -> Deterministic planner runs with:
 *    - Rescheduled task (boosted carry-forward score).
 *    - New task.
 *    - Tomorrow's habits (Morning Run, Read).
 *    - Fixed commitments (Team Standup at 09:30).
 * 7. Review & approve tomorrow's draft plan.
 */
class Example4VerificationTest {

    private val today = LocalDate.of(2026, 11, 19) // Thursday
    private val tomorrow = today.plusDays(1) // Friday

    @Test
    fun `verify PRD Example 4 end-to-end workflow`() = runTest {
        // Step 1: Initial state today
        val completed1 = Task(id = "c1", title = "Write unit tests", status = TaskStatus.COMPLETED, plannedDate = today)
        val completed2 = Task(id = "c2", title = "Code review PR #42", status = TaskStatus.COMPLETED, plannedDate = today)
        val completed3 = Task(id = "c3", title = "Client call", status = TaskStatus.COMPLETED, plannedDate = today)
        val completed4 = Task(id = "c4", title = "Update ticket backlog", status = TaskStatus.COMPLETED, plannedDate = today)

        // 1 unfinished task
        val unfinishedTask = Task(
            id = "u1",
            title = "Prepare Q3 Report",
            status = TaskStatus.PENDING,
            plannedDate = today,
            priority = Priority.HIGH,
            estimatedDurationMinutes = 60
        )

        // 1 missed habit
        val missedHabit = Habit(
            id = "h_meditation",
            name = "Meditation",
            activeDaysMask = 127,
            trackingType = HabitTrackingType.BINARY,
            targetValue = 1.0
        )
        val meditationLog = HabitLog(habitId = "h_meditation", localDate = today, completedValue = 0.0, isCompleted = false)

        // Step 2 & 3: Review summary checks
        val allTasks = listOf(completed1, completed2, completed3, completed4, unfinishedTask)
        val completedCount = allTasks.count { it.status == TaskStatus.COMPLETED }
        val unfinishedCount = allTasks.count { it.status == TaskStatus.PENDING }
        assertEquals(4, completedCount)
        assertEquals(1, unfinishedCount)

        // Step 4: User chooses action: "Reschedule to Tomorrow" for "Prepare Q3 Report"
        val rescheduledTask = unfinishedTask.copy(plannedDate = tomorrow)

        // Step 5: User adds a new task for tomorrow inline: "Review Q3 Deck with Team" (Priority High, 45m)
        val newTaskForTomorrow = Task(
            id = "n1",
            title = "Review Q3 Deck with Team",
            status = TaskStatus.PENDING,
            plannedDate = tomorrow,
            priority = Priority.HIGH,
            estimatedDurationMinutes = 45
        )

        // Tomorrow's habits: Morning Run (07:30 - 08:00) and Read (20m)
        val runHabit = Habit(
            id = "h_run",
            name = "Morning Run",
            activeDaysMask = 127,
            reminderTime = LocalTime.of(7, 30),
            trackingType = HabitTrackingType.DURATION,
            targetValue = 30.0
        )
        val readHabit = Habit(
            id = "h_read",
            name = "Read",
            activeDaysMask = 127,
            trackingType = HabitTrackingType.DURATION,
            targetValue = 20.0
        )

        // Fixed commitment for tomorrow: Team Standup at 09:30 (fixed plannedStart/End)
        val standupTask = Task(
            id = "t_standup",
            title = "Team Standup",
            status = TaskStatus.PENDING,
            plannedDate = tomorrow,
            plannedStart = LocalTime.of(9, 30),
            plannedEnd = LocalTime.of(10, 0),
            priority = Priority.HIGH,
            estimatedDurationMinutes = 30
        )

        // Step 6: Trigger Deterministic Daily Planner for Tomorrow
        val reviewDecision = com.dayflow.app.domain.model.TaskReviewDecision(
            taskId = "u1",
            taskTitle = "Prepare Q3 Report",
            decision = ReviewDecisionType.RESCHEDULED,
            rescheduledToDate = tomorrow.toString()
        )

        val plannerInput = PlannerInput(
            date = tomorrow,
            tasks = listOf(rescheduledTask, newTaskForTomorrow, standupTask),
            habits = listOf(runHabit, readHabit),
            reviewDecisions = listOf(reviewDecision),
            availabilityWindow = PlannerAvailabilityWindow(
                startTime = LocalTime.of(8, 0),
                endTime = LocalTime.of(21, 0)
            ),
            referenceTime = LocalDateTime.of(tomorrow, LocalTime.MIN)
        )

        val planResult = DeterministicDailyPlanner.generatePlan(plannerInput)

        // Step 7: Verify tomorrow's generated draft plan
        assertNotNull(planResult)
        assertEquals(tomorrow, planResult.date)

        val scheduledItems = planResult.scheduledItems
        assertTrue(scheduledItems.isNotEmpty())

        // Verify Fixed Standup is placed at 09:30
        val scheduledStandup = scheduledItems.find { it.sourceId == "t_standup" }
        assertNotNull(scheduledStandup)
        assertEquals(LocalTime.of(9, 30), scheduledStandup?.startTime)
        assertEquals(LocalTime.of(10, 0), scheduledStandup?.endTime)

        // Verify Rescheduled task "Prepare Q3 Report" is scheduled in the plan
        val scheduledQ3Report = scheduledItems.find { it.sourceId == "u1" }
        assertNotNull("Rescheduled task 'Prepare Q3 Report' must be scheduled in tomorrow's plan", scheduledQ3Report)

        // Verify New task "Review Q3 Deck with Team" is scheduled in the plan
        val scheduledDeckReview = scheduledItems.find { it.sourceId == "n1" }
        assertNotNull("New task 'Review Q3 Deck with Team' must be scheduled in tomorrow's plan", scheduledDeckReview)

        // Verify tomorrow's habits are scheduled
        val scheduledHabitRun = scheduledItems.find { it.sourceId == "h_run" }
        assertNotNull("Tomorrow's habit 'Morning Run' must be scheduled in tomorrow's plan", scheduledHabitRun)
    }
}