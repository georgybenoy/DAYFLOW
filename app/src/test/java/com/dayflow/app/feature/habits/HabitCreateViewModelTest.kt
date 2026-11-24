package com.dayflow.app.feature.habits

import androidx.lifecycle.SavedStateHandle
import com.dayflow.app.domain.model.Category
import com.dayflow.app.domain.model.Habit
import com.dayflow.app.domain.model.HabitTrackingType
import com.dayflow.app.domain.recurrence.ActiveDays
import com.dayflow.app.domain.repository.CategoryRepository
import com.dayflow.app.domain.repository.HabitRepository
import com.dayflow.app.domain.usecase.CreateHabitUseCase
import com.dayflow.app.domain.usecase.UpdateHabitUseCase
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek

@OptIn(ExperimentalCoroutinesApi::class)
class HabitCreateViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val habitRepository: HabitRepository = mockk(relaxed = true)
    private val categoryRepository: CategoryRepository = mockk(relaxed = true)
    private val createHabitUseCase: CreateHabitUseCase = mockk(relaxed = true)
    private val updateHabitUseCase: UpdateHabitUseCase = mockk(relaxed = true)

    private val categoriesFlow = MutableStateFlow<List<Category>>(emptyList())

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { categoryRepository.getAllCategories() } returns categoriesFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()) = HabitCreateViewModel(
        savedStateHandle = savedStateHandle,
        habitRepository = habitRepository,
        categoryRepository = categoryRepository,
        createHabitUseCase = createHabitUseCase,
        updateHabitUseCase = updateHabitUseCase
    )

    @Test
    fun saveHabit_whenNameBlank_setsNameError() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.updateName("")

        viewModel.saveHabit()
        testScheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.nameError)
        coVerify(exactly = 0) { createHabitUseCase(any()) }
    }

    @Test
    fun updateTrackingType_updatesDefaultTargetAndUnit() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.updateTrackingType(HabitTrackingType.DURATION)
        assertEquals("30", viewModel.uiState.value.targetValue)
        assertEquals("min", viewModel.uiState.value.unit)

        viewModel.updateTrackingType(HabitTrackingType.REPETITION)
        assertEquals("10", viewModel.uiState.value.targetValue)
        assertEquals("reps", viewModel.uiState.value.unit)
    }

    @Test
    fun toggleWeekday_modifiesActiveDaysMask() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val initialMask = viewModel.uiState.value.activeDaysMask
        assertEquals(ActiveDays.ALL_DAYS_MASK, initialMask)

        // Toggle Wednesday off
        viewModel.toggleWeekday(DayOfWeek.WEDNESDAY)
        val bit = 1 shl (DayOfWeek.WEDNESDAY.value - 1)
        assertEquals(initialMask xor bit, viewModel.uiState.value.activeDaysMask)
    }

    @Test
    fun saveHabit_validInput_createsHabitAndSetsSaved() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.updateName("Morning Jog")
        viewModel.updateTrackingType(HabitTrackingType.DURATION)
        viewModel.updateTargetValue("45")

        coEvery { createHabitUseCase(any()) } answers { Result.success(firstArg()) }

        viewModel.saveHabit()
        testScheduler.advanceUntilIdle()

        coVerify {
            createHabitUseCase(
                match { habit ->
                    habit.name == "Morning Jog" &&
                        habit.trackingType == HabitTrackingType.DURATION &&
                        habit.targetValue == 45.0
                }
            )
        }
        assertTrue(viewModel.uiState.value.isSaved)
    }
}
