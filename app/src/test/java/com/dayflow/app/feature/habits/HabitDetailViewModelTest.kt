package com.dayflow.app.feature.habits

import androidx.lifecycle.SavedStateHandle
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HabitDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val habitRepository: HabitRepository = mockk(relaxed = true)
    private val categoryRepository: CategoryRepository = mockk(relaxed = true)
    private val logHabitProgressUseCase: LogHabitProgressUseCase = mockk(relaxed = true)
    private val toggleHabitEnableUseCase: ToggleHabitEnableUseCase = mockk(relaxed = true)
    private val deleteHabitUseCase: DeleteHabitUseCase = mockk(relaxed = true)

    private val habitFlow = MutableStateFlow<Habit?>(null)
    private val categoriesFlow = MutableStateFlow<List<Category>>(emptyList())
    private val habitLogsFlow = MutableStateFlow<List<HabitLog>>(emptyList())

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { habitRepository.getHabitById("h-123") } returns habitFlow
        every { categoryRepository.getAllCategories() } returns categoriesFlow
        every { habitRepository.getLogsForHabit("h-123") } returns habitLogsFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = HabitDetailViewModel(
        savedStateHandle = SavedStateHandle(mapOf("habitId" to "h-123")),
        habitRepository = habitRepository,
        categoryRepository = categoryRepository,
        logHabitProgressUseCase = logHabitProgressUseCase,
        toggleHabitEnableUseCase = toggleHabitEnableUseCase,
        deleteHabitUseCase = deleteHabitUseCase
    )

    @Test
    fun uiState_loadsHabitAndStreakData() = runTest(testDispatcher) {
        val today = LocalDate.now()
        val habit = Habit(
            id = "h-123",
            name = "Morning Water",
            trackingType = HabitTrackingType.QUANTITY,
            targetValue = 2.0,
            unit = "L"
        )
        val log = HabitLog(habitId = "h-123", localDate = today, completedValue = 2.0, isCompleted = true)

        habitFlow.value = habit
        habitLogsFlow.value = listOf(log)

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // initial
            val state = awaitItem()
            assertNotNull(state.habit)
            assertEquals("Morning Water", state.habit?.name)
            assertEquals(1, state.streak.currentStreak)
            assertTrue(state.todayProgress?.isCompleted == true)
        }
    }

    @Test
    fun toggleTodayBinary_callsLogHabitProgressUseCase() = runTest(testDispatcher) {
        val today = LocalDate.now()
        val viewModel = createViewModel()
        coEvery { habitRepository.getLogForHabitAndDateDirect("h-123", today) } returns null

        viewModel.toggleTodayBinary()
        testScheduler.advanceUntilIdle()

        coVerify {
            logHabitProgressUseCase(
                habitId = "h-123",
                date = today,
                completedValue = 1.0,
                isCompleted = true
            )
        }
    }

    @Test
    fun deleteHabit_callsUseCaseAndSetsDeleted() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.deleteHabit()
        testScheduler.advanceUntilIdle()

        coVerify { deleteHabitUseCase("h-123") }
        assertTrue(viewModel.uiState.value.isDeleted)
    }
}
