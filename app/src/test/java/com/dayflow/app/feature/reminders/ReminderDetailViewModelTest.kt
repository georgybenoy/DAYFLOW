package com.dayflow.app.feature.reminders

import app.cash.turbine.test
import com.dayflow.app.domain.model.OccurrenceStatus
import com.dayflow.app.domain.model.Priority
import com.dayflow.app.domain.model.Reminder
import com.dayflow.app.domain.model.ReminderOccurrence
import com.dayflow.app.domain.model.ReminderType
import com.dayflow.app.domain.repository.CategoryRepository
import com.dayflow.app.domain.repository.ReminderRepository
import com.dayflow.app.domain.usecase.CompleteReminderOccurrenceUseCase
import com.dayflow.app.domain.usecase.DeleteReminderUseCase
import com.dayflow.app.domain.usecase.PauseReminderUseCase
import com.dayflow.app.domain.usecase.ResumeReminderUseCase
import com.dayflow.app.domain.usecase.SkipReminderOccurrenceUseCase
import com.dayflow.app.domain.usecase.SnoozeReminderOccurrenceUseCase
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
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class ReminderDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val reminderRepository: ReminderRepository = mockk(relaxed = true)
    private val categoryRepository: CategoryRepository = mockk(relaxed = true)
    private val completeReminderOccurrenceUseCase: CompleteReminderOccurrenceUseCase = mockk(relaxed = true)
    private val snoozeReminderOccurrenceUseCase: SnoozeReminderOccurrenceUseCase = mockk(relaxed = true)
    private val skipReminderOccurrenceUseCase: SkipReminderOccurrenceUseCase = mockk(relaxed = true)
    private val pauseReminderUseCase: PauseReminderUseCase = mockk(relaxed = true)
    private val resumeReminderUseCase: ResumeReminderUseCase = mockk(relaxed = true)
    private val deleteReminderUseCase: DeleteReminderUseCase = mockk(relaxed = true)

    private val sampleReminder = Reminder(
        id = "rem-detail-1",
        title = "Take Vitamins",
        type = ReminderType.DAILY,
        startDate = LocalDate.now(),
        startTime = LocalTime.of(8, 0),
        priority = Priority.HIGH,
        isPaused = false
    )
    private val sampleOcc = ReminderOccurrence(
        id = "occ-detail-1",
        reminderId = "rem-detail-1",
        scheduledAt = Instant.now(),
        status = OccurrenceStatus.SCHEDULED
    )

    private val reminderFlow = MutableStateFlow<Reminder?>(sampleReminder)
    private val occurrencesFlow = MutableStateFlow<List<ReminderOccurrence>>(listOf(sampleOcc))

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { reminderRepository.getReminderById("rem-detail-1") } returns reminderFlow
        every { reminderRepository.getOccurrencesForReminder("rem-detail-1") } returns occurrencesFlow
        every { categoryRepository.getAllCategories() } returns MutableStateFlow(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initForReminder_loadsReminderAndOccurrenceHistory() = runTest(testDispatcher) {
        val viewModel = ReminderDetailViewModel(
            reminderRepository,
            categoryRepository,
            completeReminderOccurrenceUseCase,
            snoozeReminderOccurrenceUseCase,
            skipReminderOccurrenceUseCase,
            pauseReminderUseCase,
            resumeReminderUseCase,
            deleteReminderUseCase
        )

        viewModel.initForReminder("rem-detail-1")
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Take Vitamins", state.reminder?.title)
        assertNotNull(state.activeOccurrence)
        assertEquals("occ-detail-1", state.activeOccurrence?.id)
        assertEquals(1, state.occurrenceHistory.size)
        assertTrue(state.occurrenceHistory.first().isActionable)
    }

    @Test
    fun completeActiveOccurrence_callsCompleteUseCase() = runTest(testDispatcher) {
        val viewModel = ReminderDetailViewModel(
            reminderRepository,
            categoryRepository,
            completeReminderOccurrenceUseCase,
            snoozeReminderOccurrenceUseCase,
            skipReminderOccurrenceUseCase,
            pauseReminderUseCase,
            resumeReminderUseCase,
            deleteReminderUseCase
        )

        viewModel.initForReminder("rem-detail-1")
        viewModel.completeActiveOccurrence("occ-detail-1")
        testScheduler.advanceUntilIdle()

        coVerify { completeReminderOccurrenceUseCase("occ-detail-1", "rem-detail-1", any()) }
    }

    @Test
    fun snoozeActiveOccurrence_callsSnoozeUseCase() = runTest(testDispatcher) {
        val viewModel = ReminderDetailViewModel(
            reminderRepository,
            categoryRepository,
            completeReminderOccurrenceUseCase,
            snoozeReminderOccurrenceUseCase,
            skipReminderOccurrenceUseCase,
            pauseReminderUseCase,
            resumeReminderUseCase,
            deleteReminderUseCase
        )

        viewModel.initForReminder("rem-detail-1")
        viewModel.snoozeActiveOccurrence("occ-detail-1", 15L)
        testScheduler.advanceUntilIdle()

        coVerify { snoozeReminderOccurrenceUseCase("occ-detail-1", "rem-detail-1", 15L, any()) }
    }

    @Test
    fun skipActiveOccurrence_callsSkipUseCase() = runTest(testDispatcher) {
        val viewModel = ReminderDetailViewModel(
            reminderRepository,
            categoryRepository,
            completeReminderOccurrenceUseCase,
            snoozeReminderOccurrenceUseCase,
            skipReminderOccurrenceUseCase,
            pauseReminderUseCase,
            resumeReminderUseCase,
            deleteReminderUseCase
        )

        viewModel.initForReminder("rem-detail-1")
        viewModel.skipActiveOccurrence("occ-detail-1")
        testScheduler.advanceUntilIdle()

        coVerify { skipReminderOccurrenceUseCase("occ-detail-1", "rem-detail-1", any()) }
    }

    @Test
    fun togglePause_whenActive_callsPauseUseCase() = runTest(testDispatcher) {
        val viewModel = ReminderDetailViewModel(
            reminderRepository,
            categoryRepository,
            completeReminderOccurrenceUseCase,
            snoozeReminderOccurrenceUseCase,
            skipReminderOccurrenceUseCase,
            pauseReminderUseCase,
            resumeReminderUseCase,
            deleteReminderUseCase
        )

        viewModel.initForReminder("rem-detail-1")
        testScheduler.advanceUntilIdle()

        viewModel.togglePause()
        testScheduler.advanceUntilIdle()

        coVerify { pauseReminderUseCase("rem-detail-1", any()) }
    }

    @Test
    fun deleteReminder_callsDeleteUseCaseAndEmitsDeletedEvent() = runTest(testDispatcher) {
        val viewModel = ReminderDetailViewModel(
            reminderRepository,
            categoryRepository,
            completeReminderOccurrenceUseCase,
            snoozeReminderOccurrenceUseCase,
            skipReminderOccurrenceUseCase,
            pauseReminderUseCase,
            resumeReminderUseCase,
            deleteReminderUseCase
        )

        viewModel.initForReminder("rem-detail-1")
        testScheduler.advanceUntilIdle()

        viewModel.events.test {
            viewModel.deleteReminder()
            testScheduler.advanceUntilIdle()

            coVerify { deleteReminderUseCase("rem-detail-1") }
            assertEquals(ReminderDetailEvent.ReminderDeleted, awaitItem())
        }
    }
}
