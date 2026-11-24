package com.dayflow.app.domain.analytics

import com.dayflow.app.core.time.TimeProvider
import com.dayflow.app.domain.model.Category
import com.dayflow.app.domain.model.DailyPlan
import com.dayflow.app.domain.model.DailyPlanItem
import com.dayflow.app.domain.model.Habit
import com.dayflow.app.domain.model.HabitLog
import com.dayflow.app.domain.model.HabitTrackingType
import com.dayflow.app.domain.model.OccurrenceStatus
import com.dayflow.app.domain.model.PlanStatus
import com.dayflow.app.domain.model.Priority
import com.dayflow.app.domain.model.ReminderOccurrence
import com.dayflow.app.domain.model.Task
import com.dayflow.app.domain.model.TaskStatus
import com.dayflow.app.domain.model.WeekStartDay
import com.dayflow.app.domain.repository.DailyPlanRepository
import com.dayflow.app.domain.repository.HabitRepository
import com.dayflow.app.domain.repository.ReminderRepository
import com.dayflow.app.domain.repository.TaskRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class GetInsightsUseCaseTest {

    private val taskRepository: TaskRepository = mockk()
    private val habitRepository: HabitRepository = mockk()
    private val reminderRepository: ReminderRepository = mockk()
    private val dailyPlanRepository: DailyPlanRepository = mockk()
    private val timeProvider: TimeProvider = mockk()

    private lateinit var useCase: GetInsightsUseCase

    private val testZoneId = ZoneId.of("UTC")
    // Let targetDate be Thursday, Nov 19, 2026
    private val targetDate = LocalDate.of(2026, 11, 19)

    @Before
    fun setUp() {
        every { timeProvider.zoneId() } returns testZoneId
        every { timeProvider.nowLocalDate() } returns targetDate
        every { timeProvider.nowInstant() } returns targetDate.atStartOfDay(testZoneId).toInstant()
        coEvery { habitRepository.getLogsForHabitInRangeDirect(any(), any(), any()) } returns emptyList()

        useCase = GetInsightsUseCase(
            taskRepository = taskRepository,
            habitRepository = habitRepository,
            reminderRepository = reminderRepository,
            dailyPlanRepository = dailyPlanRepository,
            timeProvider = timeProvider
        )
    }

    @Test
    fun invoke_withEmptyDatabase_returnsZeroAnalyticsWithoutCrashing() = runTest {
        coEvery { taskRepository.getTasksBetweenDirect(any(), any()) } returns emptyList()
        coEvery { habitRepository.getActiveHabitsDirect() } returns emptyList()
        coEvery { reminderRepository.getOccurrencesBetweenDirect(any(), any()) } returns emptyList()
        coEvery { dailyPlanRepository.getPlansBetweenDirect(any(), any()) } returns emptyList()

        val result = useCase(targetDate = targetDate, weekStartDay = WeekStartDay.MONDAY, zoneId = testZoneId)

        assertNotNull(result)
        assertEquals(0, result.today.executionScore)
        assertEquals(0, result.weekly.averageScore)
        assertEquals(0, result.monthly.averageScore)
        assertEquals(0, result.streakSummary.currentStreakDays)
        assertEquals(14, result.trendDays.size)
    }

    @Test
    fun invoke_withCompletedTasksAndScheduledHabits_computesAccurateInsights() = runTest {
        val completedTask = Task(
            id = "t1",
            title = "Task 1",
            categoryId = "c1",
            priority = Priority.MEDIUM,
            status = TaskStatus.COMPLETED,
            plannedDate = targetDate,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        coEvery { taskRepository.getTasksBetweenDirect(any(), any()) } returns listOf(completedTask)

        // Habit active on Thursday (bit 4, 1 shl 3 = 8)
        val habit = Habit(
            id = "h1",
            name = "Morning Water",
            categoryId = "c1",
            trackingType = HabitTrackingType.BINARY,
            targetValue = 1.0,
            activeDaysMask = 127, // All days
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val habitLog = HabitLog(
            id = "hl1",
            habitId = "h1",
            localDate = targetDate,
            completedValue = 1.0,
            isCompleted = true,
            loggedAt = Instant.now()
        )
        coEvery { habitRepository.getActiveHabitsDirect() } returns listOf(habit)
        coEvery { habitRepository.getLogsForHabitInRangeDirect(any(), any(), any()) } returns listOf(habitLog)

        coEvery { reminderRepository.getOccurrencesBetweenDirect(any(), any()) } returns emptyList()
        coEvery { dailyPlanRepository.getPlansBetweenDirect(any(), any()) } returns emptyList()

        val result = useCase(targetDate = targetDate, weekStartDay = WeekStartDay.MONDAY, zoneId = testZoneId)

        assertEquals(100, result.today.executionScore)
        assertEquals(1, result.today.tasksCompleted)
        assertEquals(1, result.today.tasksTotal)
        assertEquals(1, result.today.habitsCompleted)
        assertEquals(1, result.today.habitsScheduled)
        assertEquals(1, result.weekly.totalTasksCompleted)
        assertEquals(1, result.streakSummary.currentStreakDays)
    }
}
