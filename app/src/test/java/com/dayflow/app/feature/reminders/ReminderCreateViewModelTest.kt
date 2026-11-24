package com.dayflow.app.feature.reminders

import app.cash.turbine.test
import com.dayflow.app.domain.model.Priority
import com.dayflow.app.domain.model.Reminder
import com.dayflow.app.domain.model.ReminderPrecision
import com.dayflow.app.domain.model.ReminderType
import com.dayflow.app.domain.recurrence.ReminderValidationError
import com.dayflow.app.domain.recurrence.ReminderValidationResult
import com.dayflow.app.domain.repository.CategoryRepository
import com.dayflow.app.domain.repository.ReminderRepository
import com.dayflow.app.domain.usecase.ScheduleReminderUseCase
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class ReminderCreateViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val reminderRepository: ReminderRepository = mockk(relaxed = true)
    private val categoryRepository: CategoryRepository = mockk(relaxed = true)
    private val scheduleReminderUseCase: ScheduleReminderUseCase = mockk(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { categoryRepository.getAllCategories() } returns MutableStateFlow(emptyList())
        coEvery { scheduleReminderUseCase(any(), any()) } returns true
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialCreateMode_defaultsCorrectly() = runTest(testDispatcher) {
        val viewModel = ReminderCreateViewModel(reminderRepository, categoryRepository, scheduleReminderUseCase)
        val state = viewModel.uiState.value

        assertFalse(state.isEditing)
        assertEquals("", state.title)
        assertEquals(Priority.MEDIUM, state.priority)
        assertEquals(ReminderPrecision.EXACT, state.precision)
    }

    @Test
    fun initForReminder_loadsExistingReminderData() = runTest(testDispatcher) {
        val existing = Reminder(
            id = "rem-edit-1",
            title = "Hydration Commitment",
            description = "Stay hydrated throughout the day",
            type = ReminderType.FIXED_INTERVAL,
            startDate = LocalDate.of(2026, 11, 18),
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(22, 0),
            intervalMinutes = 120,
            priority = Priority.HIGH,
            precision = ReminderPrecision.EXACT
        )
        coEvery { reminderRepository.getReminderByIdDirect("rem-edit-1") } returns existing

        val viewModel = ReminderCreateViewModel(reminderRepository, categoryRepository, scheduleReminderUseCase)
        viewModel.initForReminder("rem-edit-1")
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isEditing)
        assertEquals("rem-edit-1", state.reminderId)
        assertEquals("Hydration Commitment", state.title)
        assertEquals(ReminderType.FIXED_INTERVAL, state.type)
        assertEquals(120, state.intervalMinutes)
        assertEquals(LocalTime.of(8, 0), state.startTime)
        assertEquals(LocalTime.of(22, 0), state.endTime)
    }

    @Test
    fun livePreview_computesOccurrencesViaRecurrenceCalculator() = runTest(testDispatcher) {
        val viewModel = ReminderCreateViewModel(reminderRepository, categoryRepository, scheduleReminderUseCase)
        viewModel.onTitleChange("Water")
        viewModel.onTypeSelect(ReminderType.FIXED_INTERVAL)
        viewModel.onStartTimeChange(LocalTime.of(8, 0))
        viewModel.onEndTimeChange(LocalTime.of(22, 0))
        viewModel.onIntervalMinutesChange(120)

        val state = viewModel.uiState.value
        assertTrue(state.validationResult is ReminderValidationResult.Valid)
        // Preview generates up to 5 occurrences
        assertTrue(state.previewOccurrences.isNotEmpty())
        assertTrue(state.previewOccurrences.size <= 5)
    }

    @Test
    fun saveReminder_withValidData_callsScheduleReminderUseCaseAndEmitsSuccess() = runTest(testDispatcher) {
        val viewModel = ReminderCreateViewModel(reminderRepository, categoryRepository, scheduleReminderUseCase)
        viewModel.onTitleChange("Drink Fresh Water")
        viewModel.onTypeSelect(ReminderType.DAILY)
        viewModel.onStartTimeChange(LocalTime.of(9, 0))

        viewModel.events.test {
            viewModel.saveReminder()
            testScheduler.advanceUntilIdle()

            coVerify {
                scheduleReminderUseCase(
                    match { it.title == "Drink Fresh Water" && it.type == ReminderType.DAILY },
                    any()
                )
            }
            assertEquals(ReminderCreateEvent.SaveSuccess, awaitItem())
        }
    }

    @Test
    fun saveReminder_withBlankTitle_failsValidationAndDoesNotCallSchedule() = runTest(testDispatcher) {
        val viewModel = ReminderCreateViewModel(reminderRepository, categoryRepository, scheduleReminderUseCase)
        viewModel.onTitleChange("   ")

        viewModel.saveReminder()
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.validationResult is ReminderValidationResult.Invalid)
        assertTrue((state.validationResult as ReminderValidationResult.Invalid).errors.contains(ReminderValidationError.BlankTitle))

        coVerify(exactly = 0) { scheduleReminderUseCase(any(), any()) }
    }
}
