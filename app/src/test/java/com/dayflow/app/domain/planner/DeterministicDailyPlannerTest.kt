package com.dayflow.app.domain.planner

import com.dayflow.app.domain.model.Habit
import com.dayflow.app.domain.model.HabitTrackingType
import com.dayflow.app.domain.model.PlanItemSourceType
import com.dayflow.app.domain.model.Priority
import com.dayflow.app.domain.model.ReviewDecisionType
import com.dayflow.app.domain.model.Task
import com.dayflow.app.domain.model.TaskReviewDecision
import com.dayflow.app.domain.model.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class DeterministicDailyPlannerTest {

    private val testDate = LocalDate.of(2026, 11, 20)
    private val testWindow = PlannerAvailabilityWindow(
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(17, 0)
    )

    private fun createTask(
        id: String,
        title: String,
        priority: Priority = Priority.MEDIUM,
        deadlineDate: LocalDate? = null,
        deadlineTime: LocalTime? = null,
        estimatedDurationMinutes: Int? = 60,
        plannedDate: LocalDate? = testDate,
        plannedStart: LocalTime? = null,
        plannedEnd: LocalTime? = null,
        status: TaskStatus = TaskStatus.PENDING
    ) = Task(
        id = id,
        title = title,
        description = null,
        categoryId = null,
        priority = priority,
        status = status,
        deadlineDate = deadlineDate,
        deadlineTime = deadlineTime,
        estimatedDurationMinutes = estimatedDurationMinutes,
        plannedDate = plannedDate,
        plannedStart = plannedStart,
        plannedEnd = plannedEnd,
        shouldCarryForward = true,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )

    @Test
    fun `determinism - identical input twice produces byte-identical scheduled ordering`() {
        val tasks = listOf(
            createTask("t1", "Email clients", Priority.LOW, estimatedDurationMinutes = 30),
            createTask("t2", "Review budget", Priority.HIGH, estimatedDurationMinutes = 60),
            createTask("t3", "Submit report", Priority.URGENT, deadlineDate = testDate, deadlineTime = LocalTime.of(12, 0), estimatedDurationMinutes = 90),
            createTask("t4", "Fix bug", Priority.MEDIUM, estimatedDurationMinutes = 45)
        )

        val input = PlannerInput(
            date = testDate,
            tasks = tasks,
            availabilityWindow = testWindow,
            bufferMinutes = 10
        )

        val result1 = DeterministicDailyPlanner.generatePlan(input)
        val result2 = DeterministicDailyPlanner.generatePlan(input)

        assertEquals(result1.scheduledItems.size, result2.scheduledItems.size)
        assertEquals(result1.unscheduledItems.size, result2.unscheduledItems.size)
        assertEquals(result1.conflicts.size, result2.conflicts.size)

        for (i in result1.scheduledItems.indices) {
            val item1 = result1.scheduledItems[i]
            val item2 = result2.scheduledItems[i]
            assertEquals(item1.sourceId, item2.sourceId)
            assertEquals(item1.title, item2.title)
            assertEquals(item1.startTime, item2.startTime)
            assertEquals(item1.endTime, item2.endTime)
            assertEquals(item1.orderIndex, item2.orderIndex)
        }
    }

    @Test
    fun `priority and deadline ordering - urgent and due today tasks scheduled first`() {
        val tLow = createTask("t-low", "Clean desk", Priority.LOW, estimatedDurationMinutes = 60)
        val tMedium = createTask("t-med", "Prepare slides", Priority.MEDIUM, estimatedDurationMinutes = 60)
        val tUrgent = createTask("t-urg", "Server outage postmortem", Priority.URGENT, estimatedDurationMinutes = 60)
        val tDueToday = createTask("t-due", "Client invoice", Priority.HIGH, deadlineDate = testDate, deadlineTime = LocalTime.of(14, 0), estimatedDurationMinutes = 60)

        val input = PlannerInput(
            date = testDate,
            tasks = listOf(tLow, tMedium, tUrgent, tDueToday),
            availabilityWindow = testWindow,
            bufferMinutes = 0
        )

        val result = DeterministicDailyPlanner.generatePlan(input)

        assertEquals(4, result.scheduledItems.size)
        // Highest priority scores should come first in schedule
        // Server outage (URGENT = 1000) or Client Invoice (HIGH = 500 + DUE_TODAY = 600+)
        val firstTwoIds = listOf(result.scheduledItems[0].sourceId, result.scheduledItems[1].sourceId)
        assertTrue(firstTwoIds.contains("t-urg"))
        assertTrue(firstTwoIds.contains("t-due"))

        // t-low should be last
        assertEquals("t-low", result.scheduledItems.last().sourceId)
    }

    @Test
    fun `no dropped task invariant - scheduled plus unscheduled equals total eligible`() {
        // Window is 9:00 to 12:00 = 180 minutes
        val shortWindow = PlannerAvailabilityWindow(LocalTime.of(9, 0), LocalTime.of(12, 0))
        val tasks = listOf(
            createTask("t1", "Task 1", Priority.HIGH, estimatedDurationMinutes = 60),
            createTask("t2", "Task 2", Priority.HIGH, estimatedDurationMinutes = 60),
            createTask("t3", "Task 3", Priority.HIGH, estimatedDurationMinutes = 60),
            createTask("t4", "Task 4", Priority.MEDIUM, estimatedDurationMinutes = 60),
            createTask("t5", "Task 5", Priority.LOW, estimatedDurationMinutes = 60)
        )

        val input = PlannerInput(
            date = testDate,
            tasks = tasks,
            availabilityWindow = shortWindow,
            bufferMinutes = 0
        )

        val result = DeterministicDailyPlanner.generatePlan(input)

        // 3 tasks fit in 180 mins, 2 tasks must be unscheduled
        assertEquals(3, result.scheduledItems.size)
        assertEquals(2, result.unscheduledItems.size)
        assertEquals(5, result.scheduledItems.size + result.unscheduledItems.size)

        // Ensure unscheduled items have reasons and rescheduling suggestions
        result.unscheduledItems.forEach { unscheduled ->
            assertTrue(unscheduled.reason.isNotBlank())
            assertNotNull(unscheduled.reschedulingSuggestion)
        }

        // Capacity exceeded conflict must be flagged
        assertTrue(result.conflicts.any { it.type == PlanConflictType.CAPACITY_EXCEEDED })
    }

    @Test
    fun `conflict detection - fixed commitment overlap detected`() {
        val fixedCommitment = FixedCommitment(
            id = "c1",
            title = "Board Meeting",
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(12, 0)
        )

        // Task with conflicting fixed planned times
        val conflictingTask = createTask(
            id = "t-overlap",
            title = "One-on-One",
            plannedStart = LocalTime.of(11, 0),
            plannedEnd = LocalTime.of(12, 0)
        )

        val input = PlannerInput(
            date = testDate,
            tasks = listOf(conflictingTask),
            fixedCommitments = listOf(fixedCommitment),
            availabilityWindow = testWindow
        )

        val result = DeterministicDailyPlanner.generatePlan(input)

        assertEquals(0, result.scheduledItems.size)
        assertEquals(1, result.unscheduledItems.size)
        assertEquals("t-overlap", result.unscheduledItems[0].task.id)
        assertTrue(result.conflicts.any { it.type == PlanConflictType.OVERLAP_WITH_FIXED_COMMITMENT })
    }

    @Test
    fun `conflict detection - deadline violation flagged when scheduled after deadline`() {
        val window = PlannerAvailabilityWindow(LocalTime.of(9, 0), LocalTime.of(17, 0))
        val blockingCommitment = FixedCommitment(
            id = "c1",
            title = "All Morning Block",
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(14, 0)
        )

        // Task due at 11:00, but can only be scheduled after 14:00
        val tightDeadlineTask = createTask(
            id = "t-late",
            title = "Submit Grant Application",
            priority = Priority.URGENT,
            deadlineDate = testDate,
            deadlineTime = LocalTime.of(11, 0),
            estimatedDurationMinutes = 60
        )

        val input = PlannerInput(
            date = testDate,
            tasks = listOf(tightDeadlineTask),
            fixedCommitments = listOf(blockingCommitment),
            availabilityWindow = window
        )

        val result = DeterministicDailyPlanner.generatePlan(input)

        assertEquals(1, result.scheduledItems.size)
        val scheduled = result.scheduledItems[0]
        assertEquals(LocalTime.of(14, 0), scheduled.startTime)
        assertEquals(LocalTime.of(15, 0), scheduled.endTime)

        // Should flag DEADLINE_VIOLATION
        assertTrue(result.conflicts.any { it.type == PlanConflictType.DEADLINE_VIOLATION && it.affectedTaskId == "t-late" })
    }

    @Test
    fun `review decisions influence - excluded tasks omitted and carried-forward boosted`() {
        val tNormal = createTask("t-norm", "Normal Task", Priority.MEDIUM, estimatedDurationMinutes = 60)
        val tExcluded = createTask("t-excl", "Excluded Task", Priority.HIGH, estimatedDurationMinutes = 60)
        val tCarried = createTask("t-carr", "Carried Forward Task", Priority.LOW, estimatedDurationMinutes = 60)

        val decisions = listOf(
            TaskReviewDecision(taskId = "t-excl", taskTitle = "Excluded Task", decision = ReviewDecisionType.EXCLUDED),
            TaskReviewDecision(taskId = "t-carr", taskTitle = "Carried Forward Task", decision = ReviewDecisionType.CARRIED_FORWARD)
        )

        val input = PlannerInput(
            date = testDate,
            tasks = listOf(tNormal, tExcluded, tCarried),
            reviewDecisions = decisions,
            availabilityWindow = testWindow,
            bufferMinutes = 0
        )

        val result = DeterministicDailyPlanner.generatePlan(input)

        // t-excl must be completely excluded
        assertFalse(result.scheduledItems.any { it.sourceId == "t-excl" })
        assertFalse(result.unscheduledItems.any { it.task.id == "t-excl" })

        // t-carr has LOW priority, but received +350 carried-forward boost, beating t-norm (MEDIUM = 200)
        assertEquals(2, result.scheduledItems.size)
        assertEquals("t-carr", result.scheduledItems[0].sourceId)
        assertEquals("t-norm", result.scheduledItems[1].sourceId)
    }

    @Test
    fun `edge cases - empty task list, overloaded day, closed window, all-day fixed commitment`() {
        // 1. Empty tasks
        val emptyResult = DeterministicDailyPlanner.generatePlan(
            PlannerInput(date = testDate, tasks = emptyList(), availabilityWindow = testWindow)
        )
        assertTrue(emptyResult.scheduledItems.isEmpty())
        assertTrue(emptyResult.unscheduledItems.isEmpty())
        assertTrue(emptyResult.conflicts.isEmpty())

        // 2. Closed window (start >= end)
        val closedResult = DeterministicDailyPlanner.generatePlan(
            PlannerInput(
                date = testDate,
                tasks = listOf(createTask("t1", "Any task")),
                availabilityWindow = PlannerAvailabilityWindow(LocalTime.of(17, 0), LocalTime.of(9, 0))
            )
        )
        assertTrue(closedResult.scheduledItems.isEmpty())
        assertEquals(1, closedResult.unscheduledItems.size)
        assertTrue(closedResult.conflicts.any { it.type == PlanConflictType.CAPACITY_EXCEEDED })

        // 3. All-day fixed commitment
        val allDayResult = DeterministicDailyPlanner.generatePlan(
            PlannerInput(
                date = testDate,
                tasks = listOf(createTask("t1", "Any task")),
                fixedCommitments = listOf(
                    FixedCommitment("c-all", "Full Day Seminar", LocalTime.of(9, 0), LocalTime.of(17, 0))
                ),
                availabilityWindow = testWindow
            )
        )
        assertTrue(allDayResult.scheduledItems.isEmpty())
        assertEquals(1, allDayResult.unscheduledItems.size)

        // 4. Active habits scheduled at habit reminderTime
        val habit = Habit(
            id = "h1",
            name = "Morning Meditation",
            description = null,
            categoryId = null,
            trackingType = HabitTrackingType.DURATION,
            targetValue = 15.0,
            unit = "min",
            activeDaysMask = 127, // All days
            reminderTime = LocalTime.of(9, 30),
            isEnabled = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val habitResult = DeterministicDailyPlanner.generatePlan(
            PlannerInput(
                date = testDate,
                tasks = listOf(createTask("t1", "Read", estimatedDurationMinutes = 30)),
                habits = listOf(habit),
                availabilityWindow = testWindow
            )
        )
        assertTrue(habitResult.scheduledItems.any { it.sourceType == PlanItemSourceType.HABIT && it.sourceId == "h1" })
    }

    @Test
    fun `task deadlines are never modified`() {
        val originalDeadlineDate = LocalDate.of(2026, 11, 25)
        val originalDeadlineTime = LocalTime.of(15, 30)

        val task = createTask(
            id = "t-immutable",
            title = "Immutable Deadline Task",
            deadlineDate = originalDeadlineDate,
            deadlineTime = originalDeadlineTime,
            plannedDate = testDate
        )

        val input = PlannerInput(
            date = testDate,
            tasks = listOf(task),
            availabilityWindow = testWindow
        )

        DeterministicDailyPlanner.generatePlan(input)

        // Verify task object is not altered
        assertEquals(originalDeadlineDate, task.deadlineDate)
        assertEquals(originalDeadlineTime, task.deadlineTime)
    }
}
