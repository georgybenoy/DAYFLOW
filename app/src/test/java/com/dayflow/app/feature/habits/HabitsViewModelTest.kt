package com.dayflow.app.feature.habits

import app.cash.turbine.test
import com.dayflow.app.domain.model.Category
import com.dayflow.app.domain.model.Habit
import com.dayflow.app.domain.model.HabitLog
import com.dayflow.app.domain.model.HabitTrackingType
import com.dayflow.app.domain.repository.CategoryRepository
import com.dayflow.app.domain.repository.HabitRepository
import com.dayflow.app.domain.usecase.DeleteHabitUseCase
import com.dayflow.app.domain.usecase.LogHabitProgressUseCase
import com.dayflow.app.domain.usecase.ToggleHabitEnableUseCase
import io.mockk.coEvery
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HabitsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val habitRepository: HabitRepository = mockk(relaxed = true)
    private val categoryRepository: CategoryRepository = mockk(relaxed = true)
    private val logHabitProgressUseCase: LogHabitProgressUseCase = mockk(relaxed = true)
    private val toggleHabitEnableUseCase: ToggleHabitEnableUseCase = mockk(relaxed = true)
    private val deleteHabitUseCase: DeleteHabitUseCase = mockk(relaxed = true)

    private val allHabitsFlow = MutableStateFlow<List<Habit>>(emptyList())
    private val allCategoriesFlow = MutableStateFlow<List<Category>>(emptyList())
    private val logsInRangeFlow = MutableStateFlow<List<HabitLog>>(emptyList())

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { habitRepository.getAllHabits() } returns allHabitsFlow
        every { categoryRepository.getAllCategories() } returns allCategoriesFlow
        every { habitRepository.getLogsInRange(any(), any()) } returns logsInRangeFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = HabitsViewModel(
        habitRepository = habitRepository,
        categoryRepository = categoryRepository,
        logHabitProgressUseCase = logHabitProgressUseCase,
        toggleHabitEnableUseCase = toggleHabitEnableUseCase,
        deleteHabitUseCase = deleteHabitUseCase
    )

    @Test
    fun uiState_loadsHabitsAndCalculatesDailyProgress() = runTest(testDispatcher) {
        val today = LocalDate.now()
        val h1 = Habit(id = "h1", name = "Habit 1", trackingType = HabitTrackingType.BINARY, activeDaysMask = 127)
        val h2 = Habit(id = "h2", name = "Habit 2", trackingType = HabitTrackingType.BINARY, activeDaysMask = 127)
        val log1 = HabitLog(habitId = "h1", localDate = today, completedValue = 1.0, isCompleted = true)

        allHabitsFlow.value = listOf(h1, h2)
        logsInRangeFlow.value = listOf(log1)

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // initial
            val state = awaitItem()
            assertEquals(2, state.habits.size)
            assertEquals(1, state.completedCount)
            assertEquals(2, state.totalScheduledCount)
            assertEquals(0.5f, state.dailyProgress, 0.001f)
        }
    }

    @Test
    fun toggleBinaryHabit_callsLogHabitProgressUseCase() = runTest(testDispatcher) {
        val today = LocalDate.now()
        val viewModel = createViewModel()
        coEvery { habitRepository.getLogForHabitAndDateDirect("h1", today) } returns null

        viewModel.toggleBinaryHabit("h1")
        testScheduler.advanceUntilIdle()

        coVerify {
            logHabitProgressUseCase(
                habitId = "h1",
                date = today,
                completedValue = 1.0,
                isCompleted = true
            )
        }
    }

    @Test
    fun openQuickLogAndSave_callsLogHabitProgressUseCase() = runTest(testDispatcher) {
        val today = LocalDate.now()
        val habit = Habit(id = "h-qty", name = "Water", trackingType = HabitTrackingType.QUANTITY, targetValue = 2.0)
        val viewModel = createViewModel()

        viewModel.openQuickLog(habit)
        testScheduler.advanceUntilIdle()

        viewModel.saveQuickLog(1.5)
        testScheduler.advanceUntilIdle()

        coVerify {
            logHabitProgressUseCase(
                habitId = "h-qty",
                date = today,
                completedValue = 1.5
            )
        }
    }

    @Test
    fun toggleHabitEnable_callsUseCase() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.toggleHabitEnable("h1")
        testScheduler.advanceUntilIdle()

        coVerify { toggleHabitEnableUseCase("h1") }
    }

    @Test
    fun deleteHabit_callsUseCase() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.deleteHabit("h1")
        testScheduler.advanceUntilIdle()

        coVerify { deleteHabitUseCase("h1") }
    }
}
