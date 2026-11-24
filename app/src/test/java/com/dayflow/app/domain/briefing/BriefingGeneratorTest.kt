package com.dayflow.app.domain.briefing

import com.dayflow.app.domain.model.DailyPlanItem
import com.dayflow.app.domain.model.Habit
import com.dayflow.app.domain.model.HabitLog
import com.dayflow.app.domain.model.HabitTrackingType
import com.dayflow.app.domain.model.PlanItemSourceType
import com.dayflow.app.domain.model.Priority
import com.dayflow.app.domain.model.Reminder
import com.dayflow.app.domain.model.ReminderType
import com.dayflow.app.domain.model.Task
import com.dayflow.app.domain.model.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class BriefingGeneratorTest {

    private val testDate = LocalDate.of(2026, 11, 19)

    @Test
    fun `generate greeting uses user name when provided`() {
        val result = BriefingGenerator.generate(
            userName = "Alex",
            date = testDate,
            tasks = emptyList(),
            scheduledPlanItems = emptyList(),
            habits = emptyList(),
            habitLogs = emptyList(),
            reminders = emptyList()
        )

        assertEquals("Good morning, Alex", result.greeting)
        assertTrue(result.isEmpty)
    }

    @Test
    fun `generate greeting uses fallback when user name is blank`() {
        val result = BriefingGenerator.generate(
            userName = "",
            date = testDate,
            tasks = emptyList(),
            scheduledPlanItems = emptyList(),
            habits = emptyList(),
            habitLogs = emptyList(),
            reminders = emptyList()
        )

        assertEquals("Good morning", result.greeting)
        assertEquals("Your schedule is open today. Take time to focus on personal goals or add new commitments when you're ready.", result.summaryText)
        assertTrue(result.isEmpty)
    }

    @Test
    fun `priorities only includes high and urgent tasks for today`() {
        val urgentTask = Task(
            id = "t1",
            title = "Server Outage Fix",
            priority = Priority.URGENT,
            status = TaskStatus.PENDING,
            plannedDate = testDate,
            deadlineTime = LocalTime.of(10, 0)
        )
        val highTask = Task(
            id = "t2",
            title = "Prepare Q3 Presentation",
            priority = Priority.HIGH,
            status = TaskStatus.PENDING,
            plannedDate = testDate,
            deadlineTime = LocalTime.of(15, 0)
        )
        val mediumTask = Task(
            id = "t3",
            title = "Clean inbox",
            priority = Priority.MEDIUM,
            status = TaskStatus.PENDING,
            plannedDate = testDate
        )
        val futureTask = Task(
            id = "t4",
            title = "Next week task",
            priority = Priority.URGENT,
            status = TaskStatus.PENDING,
            plannedDate = testDate.plusDays(7)
        )

        val result = BriefingGenerator.generate(
            userName = "Alex",
            date = testDate,
            tasks = listOf(urgentTask, highTask, mediumTask, futureTask),
            scheduledPlanItems = emptyList(),
            habits = emptyList(),
            habitLogs = emptyList(),
            reminders = emptyList()
        )

        assertEquals(2, result.priorities.size)
        assertEquals("t1", result.priorities[0].id)
        assertEquals("t2", result.priorities[1].id)
        assertEquals(3, result.pendingTasksCount)
        assertFalse(result.isEmpty)
    }

    @Test
    fun `deadlines only includes pending tasks due today`() {
        val dueToday = Task(
            id = "t1",
            title = "Tax Filing",
            priority = Priority.HIGH,
            status = TaskStatus.PENDING,
            deadlineDate = testDate,
            deadlineTime = LocalTime.of(17, 0)
        )
        val dueTomorrow = Task(
            id = "t2",
            title = "Report",
            priority = Priority.MEDIUM,
            status = TaskStatus.PENDING,
            deadlineDate = testDate.plusDays(1)
        )
        val completedToday = Task(
            id = "t3",
            title = "Done Task",
            priority = Priority.HIGH,
            status = TaskStatus.COMPLETED,
            deadlineDate = testDate
        )

        val result = BriefingGenerator.generate(
            userName = "Alex",
            date = testDate,
            tasks = listOf(dueToday, dueTomorrow, completedToday),
            scheduledPlanItems = emptyList(),
            habits = emptyList(),
            habitLogs = emptyList(),
            reminders = emptyList()
        )

        assertEquals(1, result.deadlines.size)
        assertEquals("t1", result.deadlines[0].id)
    }

    @Test
    fun `habits checklist evaluates progress and active days correctly`() {
        val dailyHabit = Habit(
            id = "h1",
            name = "Morning Meditation",
            activeDaysMask = 127,
            trackingType = HabitTrackingType.BINARY,
            targetValue = 1.0
        )
        val waterHabit = Habit(
            id = "h2",
            name = "Drink Water",
            activeDaysMask = 127,
            trackingType = HabitTrackingType.QUANTITY,
            targetValue = 8.0,
            unit = "glasses"
        )
        val weekendOnlyHabit = Habit(
            id = "h3",
            name = "Long Hike",
            activeDaysMask = (1 shl 5) or (1 shl 6), // Saturday and Sunday
            trackingType = HabitTrackingType.DURATION,
            targetValue = 60.0
        )

        // testDate is 2026-11-19 which is a Thursday
        val log1 = HabitLog(habitId = "h1", localDate = testDate, isCompleted = true, completedValue = 1.0)
        val log2 = HabitLog(habitId = "h2", localDate = testDate, completedValue = 4.0)

        val result = BriefingGenerator.generate(
            userName = "Alex",
            date = testDate,
            tasks = emptyList(),
            scheduledPlanItems = emptyList(),
            habits = listOf(dailyHabit, waterHabit, weekendOnlyHabit),
            habitLogs = listOf(log1, log2),
            reminders = emptyList()
        )

        assertEquals(2, result.habitsChecklist.size)
        assertTrue(result.habitsChecklist[0].isCompleted)
        assertEquals("Completed", result.habitsChecklist[0].formattedProgress)
        assertFalse(result.habitsChecklist[1].isCompleted)
        assertEquals("4 / 8 glasses", result.habitsChecklist[1].formattedProgress)
    }

    @Test
    fun `briefing does not duplicate plan but references schedule summary`() {
        val planItems = listOf(
            DailyPlanItem(
                planId = "p1",
                sourceType = PlanItemSourceType.TASK,
                title = "Deep Work",
                startTime = LocalTime.of(9, 0),
                endTime = LocalTime.of(10, 30)
            ),
            DailyPlanItem(
                planId = "p1",
                sourceType = PlanItemSourceType.HABIT,
                title = "Morning Run",
                startTime = LocalTime.of(7, 30),
                endTime = LocalTime.of(8, 0)
            )
        )

        val result = BriefingGenerator.generate(
            userName = "Alex",
            date = testDate,
            tasks = emptyList(),
            scheduledPlanItems = planItems,
            habits = emptyList(),
            habitLogs = emptyList(),
            reminders = emptyList()
        )

        assertEquals(2, result.scheduledTasks.size)
        assertTrue(result.summaryText.contains("2 scheduled tasks"))
    }
}