package com.dayflow.app.feature.today

import app.cash.turbine.test
import com.dayflow.app.core.time.TimeProvider
import com.dayflow.app.domain.model.Category
import com.dayflow.app.domain.model.Habit
import com.dayflow.app.domain.model.HabitLog
import com.dayflow.app.domain.model.HabitTrackingType
import com.dayflow.app.domain.model.Priority
import com.dayflow.app.domain.model.Reminder
import com.dayflow.app.domain.model.ReminderOccurrence
import com.dayflow.app.domain.model.Task
import com.dayflow.app.domain.model.TaskStatus
import com.dayflow.app.domain.model.ThemePreference
import com.dayflow.app.domain.model.UserProfile
import com.dayflow.app.domain.repository.CategoryRepository
import com.dayflow.app.domain.repository.DailyPlanRepository
import com.dayflow.app.domain.repository.HabitRepository
import com.dayflow.app.domain.repository.SettingsRepository
import com.dayflow.app.domain.repository.TaskRepository
import com.dayflow.app.domain.usecase.CompleteReminderOccurrenceUseCase
import com.dayflow.app.domain.usecase.CompleteTaskUseCase
import com.dayflow.app.domain.usecase.DeriveOverdue
import com.dayflow.app.domain.usecase.GetUpcomingRemindersUseCase
import com.dayflow.app.domain.usecase.ReopenTaskUseCase
import com.dayflow.app.domain.usecase.UpcomingReminderItem
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val settingsRepository: SettingsRepository = mockk(relaxed = true)
    private val getUpcomingRemindersUseCase: GetUpcomingRemindersUseCase = mockk(relaxed = true)
    private val completeReminderOccurrenceUseCase: CompleteReminderOccurrenceUseCase = mockk(relaxed = true)
    private val taskRepository: TaskRepository = mockk(relaxed = true)
    private val categoryRepository: CategoryRepository = mockk(relaxed = true)
    private val habitRepository: HabitRepository = mockk(relaxed = true)
    private val dailyPlanRepository: DailyPlanRepository = mockk(relaxed = true)
    private val completeTaskUseCase: CompleteTaskUseCase = mockk(relaxed = true)
    private val reopenTaskUseCase: ReopenTaskUseCase = mockk(relaxed = true)
    private val deriveOverdue = DeriveOverdue()

    private val fixedToday = LocalDate.of(2026, 11, 18)
    private val fixedTime = LocalTime.of(10, 0)
    private val timeProvider = object : TimeProvider {
        override fun nowInstant(): Instant = Instant.now()
        override fun nowLocalDateTime(): LocalDateTime = LocalDateTime.of(fixedToday, fixedTime)
        override fun nowLocalDate(): LocalDate = fixedToday
        override fun nowLocalTime(): LocalTime = fixedTime
        override fun zoneId(): ZoneId = ZoneId.systemDefault()
    }

    private val userProfileFlow = MutableStateFlow(
        UserProfile(name = "Taylor", themePreference = ThemePreference.DARK)
    )
    private val upcomingRemindersFlow = MutableStateFlow<List<UpcomingReminderItem>>(emptyList())
    private val allTasksFlow = MutableStateFlow<List<Task>>(emptyList())
    private val categoriesFlow = MutableStateFlow<List<Category>>(emptyList())
    private val activeHabitsFlow = MutableStateFlow<List<Habit>>(emptyList())
    private val habitLogsFlow = MutableStateFlow<List<HabitLog>>(emptyList())
    private val dailyPlanFlow = MutableStateFlow<com.dayflow.app.domain.model.DailyPlan?>(null)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { settingsRepository.userProfile } returns userProfileFlow
        every { getUpcomingRemindersUseCase(any(), any()) } returns upcomingRemindersFlow
        every { taskRepository.getAllTasks() } returns allTasksFlow
        every { categoryRepository.getAllCategories() } returns categoriesFlow
        every { habitRepository.getActiveHabits() } returns activeHabitsFlow
        every { habitRepository.getLogsForDate(any()) } returns habitLogsFlow
        every { dailyPlanRepository.getPlanByDate(any()) } returns dailyPlanFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = TodayViewModel(
        settingsRepository = settingsRepository,
        timeProvider = timeProvider,
        taskRepository = taskRepository,
        categoryRepository = categoryRepository,
        habitRepository = habitRepository,
        dailyPlanRepository = dailyPlanRepository,
        getUpcomingRemindersUseCase = getUpcomingRemindersUseCase,
        completeReminderOccurrenceUseCase = completeReminderOccurrenceUseCase,
        completeTaskUseCase = completeTaskUseCase,
        reopenTaskUseCase = reopenTaskUseCase,
        deriveOverdue = deriveOverdue
    )

    @Test
    fun initialSelectedDate_equalsTimeProviderToday() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        assertEquals(fixedToday, viewModel.selectedDate.value)
    }

    @Test
    fun selectDate_updatesSelectedDateFlow() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val targetDate = fixedToday.plusDays(2)

        viewModel.selectedDate.test {
            assertEquals(fixedToday, awaitItem())

            viewModel.selectDate(targetDate)
            assertEquals(targetDate, awaitItem())
        }
    }

    @Test
    fun uiState_reflectsUserProfileAndUpcomingReminders() = runTest(testDispatcher) {
        val sampleReminder = Reminder(
            id = "rem-1",
            title = "Morning Coffee",
            startDate = fixedToday,
            startTime = fixedTime,
            priority = Priority.HIGH
        )
        val sampleOcc = ReminderOccurrence(
            id = "occ-1",
            reminderId = "rem-1",
            scheduledAt = Instant.now()
        )
        upcomingRemindersFlow.value = listOf(UpcomingReminderItem(sampleOcc, sampleReminder))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val initial = awaitItem()
            assertEquals("", initial.userName)

            val combined = awaitItem()
            assertEquals("Taylor", combined.userName)
            assertEquals(fixedToday, combined.selectedDate)
            assertEquals(1, combined.upcomingReminders.size)
            assertEquals("Morning Coffee", combined.upcomingReminders.first().reminder.title)
        }
    }

    @Test
    fun completeReminder_callsCompleteReminderOccurrenceUseCase() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.completeReminder(occurrenceId = "occ-123", reminderId = "rem-456")
        testScheduler.advanceUntilIdle()

        coVerify { completeReminderOccurrenceUseCase("occ-123", "rem-456", any()) }
    }

    @Test
    fun toggleTaskComplete_callsCompleteTaskUseCaseWhenPending() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val task = Task(id = "task-1", title = "Assignment", status = TaskStatus.PENDING)

        viewModel.toggleTaskComplete(task)
        testScheduler.advanceUntilIdle()

        coVerify { completeTaskUseCase("task-1", any()) }
    }

    @Test
    fun toggleTaskComplete_callsReopenTaskUseCaseWhenCompleted() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val task = Task(id = "task-2", title = "Assignment Done", status = TaskStatus.COMPLETED)

        viewModel.toggleTaskComplete(task)
        testScheduler.advanceUntilIdle()

        coVerify { reopenTaskUseCase("task-2", any(), any()) }
    }

    @Test
    fun uiState_calculatesDailyTaskProgressCorrectly() = runTest(testDispatcher) {
        val task1 = Task(id = "t1", title = "Task 1", plannedDate = fixedToday, status = TaskStatus.COMPLETED)
        val task2 = Task(id = "t2", title = "Task 2", deadlineDate = fixedToday, status = TaskStatus.PENDING)
        val task3 = Task(id = "t3", title = "Task 3", plannedDate = fixedToday.plusDays(1), status = TaskStatus.PENDING)

        allTasksFlow.value = listOf(task1, task2, task3)

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // initial
            val state = awaitItem()
            assertEquals(1, state.tasksCompletedCount)
            assertEquals(2, state.totalTasksCount)
            assertEquals(1, state.priorityTasks.size)
            assertEquals("Task 2", state.priorityTasks.first().title)
        }
    }

    @Test
    fun uiState_calculatesHabitsOverviewCorrectly() = runTest(testDispatcher) {
        val h1 = Habit(id = "h1", name = "Morning Water", trackingType = HabitTrackingType.QUANTITY, targetValue = 2.0, activeDaysMask = 127)
        val h2 = Habit(id = "h2", name = "Gym", trackingType = HabitTrackingType.BINARY, activeDaysMask = 127)

        val log1 = HabitLog(habitId = "h1", localDate = fixedToday, completedValue = 2.0, isCompleted = true)
        val log2 = HabitLog(habitId = "h2", localDate = fixedToday, completedValue = 0.0, isCompleted = false)

        activeHabitsFlow.value = listOf(h1, h2)
        habitLogsFlow.value = listOf(log1, log2)

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // initial
            val state = awaitItem()
            assertEquals(1, state.habitsCompletedCount)
            assertEquals(2, state.totalHabitsCount)
        }
    }
}
