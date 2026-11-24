package com.dayflow.app.domain.calendar

import com.dayflow.app.core.time.TimeProvider
import com.dayflow.app.domain.model.DailyPlan
import com.dayflow.app.domain.model.DailyPlanItem
import com.dayflow.app.domain.model.Habit
import com.dayflow.app.domain.model.HabitLog
import com.dayflow.app.domain.model.HabitTrackingType
import com.dayflow.app.domain.model.OccurrenceStatus
import com.dayflow.app.domain.model.PlanStatus
import com.dayflow.app.domain.model.Priority
import com.dayflow.app.domain.model.Reminder
import com.dayflow.app.domain.model.ReminderOccurrence
import com.dayflow.app.domain.model.ReminderPrecision
import com.dayflow.app.domain.model.ReminderType
import com.dayflow.app.domain.model.Task
import com.dayflow.app.domain.model.TaskStatus
import com.dayflow.app.domain.repository.DailyPlanRepository
import com.dayflow.app.domain.repository.HabitRepository
import com.dayflow.app.domain.repository.ReminderRepository
import com.dayflow.app.domain.repository.TaskRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class GetCalendarEventsUseCaseTest {

    private val taskRepository: TaskRepository = mockk()
    private val habitRepository: HabitRepository = mockk()
    private val reminderRepository: ReminderRepository = mockk()
    private val dailyPlanRepository: DailyPlanRepository = mockk()
    private val timeProvider: TimeProvider = mockk()

    private lateinit var useCase: GetCalendarEventsUseCase

    private val testZoneId = ZoneId.of("UTC")
    private val testDate = LocalDate.of(2026, 11, 19) // Thursday

    @Before
    fun setUp() {
        every { timeProvider.zoneId() } returns testZoneId
        every { timeProvider.nowLocalDate() } returns testDate
        every { timeProvider.nowInstant() } returns testDate.atStartOfDay(testZoneId).toInstant()

        useCase = GetCalendarEventsUseCase(
            taskRepository = taskRepository,
            habitRepository = habitRepository,
            reminderRepository = reminderRepository,
            dailyPlanRepository = dailyPlanRepository,
            timeProvider = timeProvider
        )
    }

    @Test
    fun invoke_aggregatesTasksDeadlinesHabitsPlansAndDynamicReminders() = runTest {
        // 1. Task with planned start & end
        val task = Task(
            id = "t1",
            title = "Code Review",
            categoryId = "c1",
            priority = Priority.HIGH,
            status = TaskStatus.PENDING,
            plannedDate = testDate,
            plannedStart = LocalTime.of(10, 0),
            plannedEnd = LocalTime.of(11, 0),
            deadlineDate = testDate.plusDays(1),
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        coEvery { taskRepository.getTasksBetweenDirect(testDate, testDate) } returns listOf(task)

        // 2. Active daily reminder for dynamic recurrence generation (no future spam)
        val reminder = Reminder(
            id = "r1",
            title = "Team Standup",
            type = ReminderType.DAILY,
            startDate = testDate.minusDays(5),
            startTime = LocalTime.of(9, 30),
            priority = Priority.HIGH,
            precision = ReminderPrecision.EXACT,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        coEvery { reminderRepository.getOccurrencesBetweenDirect(any(), any()) } returns emptyList()
        coEvery { reminderRepository.getActiveRemindersDirect() } returns listOf(reminder)

        // 3. Habit active on all days
        val habit = Habit(
            id = "h1",
            name = "Exercise",
            categoryId = "c1",
            trackingType = HabitTrackingType.BINARY,
            targetValue = 1.0,
            activeDaysMask = 127,
            reminderTime = LocalTime.of(8, 0),
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        coEvery { habitRepository.getActiveHabitsDirect() } returns listOf(habit)
        coEvery { habitRepository.getLogForHabitAndDateDirect("h1", testDate) } returns null

        // 4. Approved plan items
        val planItem = DailyPlanItem(
            id = "pi1",
            planId = "p1",
            title = "Focus Block",
            startTime = LocalTime.of(14, 0),
            endTime = LocalTime.of(16, 0),
            isCompleted = false
        )
        val dailyPlan = DailyPlan(
            id = "p1",
            localDate = testDate,
            status = PlanStatus.APPROVED,
            items = listOf(planItem),
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        coEvery { dailyPlanRepository.getPlansBetweenDirect(testDate, testDate) } returns listOf(dailyPlan)

        val events = useCase(startDate = testDate, endDate = testDate, zoneId = testZoneId)

        assertEquals(4, events.size)

        // Check chronological ordering:
        // 08:00 Habit
        // 09:30 Dynamic Reminder
        // 10:00 Task
        // 14:00 Plan Item
        assertEquals(CalendarEventType.HABIT, events[0].type)
        assertEquals("Exercise", events[0].title)

        assertEquals(CalendarEventType.REMINDER, events[1].type)
        assertEquals("Team Standup", events[1].title)

        assertEquals(CalendarEventType.TASK, events[2].type)
        assertEquals("Code Review", events[2].title)

        assertEquals(CalendarEventType.PLAN_ITEM, events[3].type)
        assertEquals("Plan: Focus Block", events[3].title)
    }

    @Test
    fun invoke_deduplicatesStoredAndDynamicReminderOccurrences() = runTest {
        val reminder = Reminder(
            id = "r1",
            title = "Meditation",
            type = ReminderType.DAILY,
            startDate = testDate.minusDays(2),
            startTime = LocalTime.of(7, 0),
            priority = Priority.MEDIUM,
            precision = ReminderPrecision.EXACT,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        val scheduledInstant = testDate.atTime(LocalTime.of(7, 0)).atZone(testZoneId).toInstant()
        val storedOccurrence = ReminderOccurrence(
            id = "occ_1",
            reminderId = "r1",
            scheduledAt = scheduledInstant,
            status = OccurrenceStatus.COMPLETED
        )

        coEvery { taskRepository.getTasksBetweenDirect(any(), any()) } returns emptyList()
        coEvery { reminderRepository.getOccurrencesBetweenDirect(any(), any()) } returns listOf(storedOccurrence)
        coEvery { reminderRepository.getActiveRemindersDirect() } returns listOf(reminder)
        coEvery { habitRepository.getActiveHabitsDirect() } returns emptyList()
        coEvery { dailyPlanRepository.getPlansBetweenDirect(any(), any()) } returns emptyList()

        val events = useCase(startDate = testDate, endDate = testDate, zoneId = testZoneId)

        // Should NOT produce duplicate entries for 07:00
        val reminderEvents = events.filter { it.type == CalendarEventType.REMINDER }
        assertEquals(1, reminderEvents.size)
        assertTrue(reminderEvents.first().isCompleted)
    }
}
