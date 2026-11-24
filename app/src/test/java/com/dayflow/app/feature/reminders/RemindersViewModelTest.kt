package com.dayflow.app.feature.reminders

import app.cash.turbine.test
import com.dayflow.app.domain.model.Priority
import com.dayflow.app.domain.model.Reminder
import com.dayflow.app.domain.model.ReminderOccurrence
import com.dayflow.app.domain.model.ReminderType
import com.dayflow.app.domain.repository.CategoryRepository
import com.dayflow.app.domain.repository.ReminderRepository
import com.dayflow.app.domain.usecase.DeleteReminderUseCase
import com.dayflow.app.domain.usecase.GetUpcomingRemindersUseCase
import com.dayflow.app.domain.usecase.PauseReminderUseCase
import com.dayflow.app.domain.usecase.ResumeReminderUseCase
import com.dayflow.app.domain.usecase.UpcomingReminderItem
import com.dayflow.app.scheduling.AlarmScheduler
import com.dayflow.app.scheduling.SchedulingCapability
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
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class RemindersViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val reminderRepository: ReminderRepository = mockk(relaxed = true)
    private val pauseReminderUseCase: PauseReminderUseCase = mockk(relaxed = true)
    private val resumeReminderUseCase: ResumeReminderUseCase = mockk(relaxed = true)
    private val deleteReminderUseCase: DeleteReminderUseCase = mockk(relaxed = true)
    private val getUpcomingRemindersUseCase: GetUpcomingRemindersUseCase = mockk(relaxed = true)

    private val remindersFlow = MutableStateFlow<List<Reminder>>(emptyList())
    private val capabilityFlow = MutableStateFlow(SchedulingCapability.EXACT)
    private val upcomingFlow = MutableStateFlow<List<UpcomingReminderItem>>(emptyList())

    private val alarmScheduler = object : AlarmScheduler {
        override val schedulingCapability: kotlinx.coroutines.flow.Flow<SchedulingCapability> = capabilityFlow
        override fun getSchedulingCapability(): SchedulingCapability = capabilityFlow.value
        override suspend fun scheduleOccurrence(occurrence: ReminderOccurrence, reminder: Reminder): Boolean = true
        override suspend fun cancelOccurrence(occurrenceId: String, reminderId: String) {}
        override suspend fun cancelAllForReminder(reminderId: String) {}
    }

    private val categoryRepository = object : CategoryRepository {
        override fun getAllCategories(): kotlinx.coroutines.flow.Flow<List<com.dayflow.app.domain.model.Category>> =
            kotlinx.coroutines.flow.flowOf(emptyList())
        override fun getCategoryById(id: String): kotlinx.coroutines.flow.Flow<com.dayflow.app.domain.model.Category?> =
            kotlinx.coroutines.flow.flowOf(null)
        override suspend fun saveCategory(category: com.dayflow.app.domain.model.Category) {}
        override suspend fun deleteCategory(category: com.dayflow.app.domain.model.Category) {}
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { reminderRepository.getAllReminders() } returns remindersFlow
        every { reminderRepository.getStandaloneReminders() } returns remindersFlow
        every { getUpcomingRemindersUseCase(any(), any()) } returns upcomingFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_separatesActiveAndPausedReminders() = runTest(testDispatcher) {
        val activeReminder = Reminder(
            id = "rem-active",
            title = "Drink Water",
            type = ReminderType.DAILY,
            startDate = LocalDate.now(),
            startTime = LocalTime.of(9, 0),
            isEnabled = true,
            isPaused = false
        )
        val pausedReminder = Reminder(
            id = "rem-paused",
            title = "Read Book",
            type = ReminderType.DAILY,
            startDate = LocalDate.now(),
            startTime = LocalTime.of(21, 0),
            isEnabled = true,
            isPaused = true
        )
        remindersFlow.value = listOf(activeReminder, pausedReminder)

        val viewModel = RemindersViewModel(
            reminderRepository,
            categoryRepository,
            alarmScheduler,
            pauseReminderUseCase,
            resumeReminderUseCase,
            deleteReminderUseCase,
            getUpcomingRemindersUseCase
        )

        viewModel.uiState.test {
            val state = awaitItem()
            // May start with initial then emit
            val finalState = if (state.isLoading && state.activeReminders.isEmpty()) awaitItem() else state

            assertEquals(1, finalState.activeReminders.size)
            assertEquals("Drink Water", finalState.activeReminders.first().reminder.title)
            assertEquals(1, finalState.pausedReminders.size)
            assertEquals("Read Book", finalState.pausedReminders.first().reminder.title)
            assertEquals(SchedulingCapability.EXACT, finalState.schedulingCapability)
        }
    }

    @Test
    fun pauseReminder_delegatesToPauseReminderUseCase() = runTest(testDispatcher) {
        val viewModel = RemindersViewModel(
            reminderRepository,
            categoryRepository,
            alarmScheduler,
            pauseReminderUseCase,
            resumeReminderUseCase,
            deleteReminderUseCase,
            getUpcomingRemindersUseCase
        )

        viewModel.pauseReminder("rem-123")
        testScheduler.advanceUntilIdle()

        coVerify { pauseReminderUseCase("rem-123", any()) }
    }

    @Test
    fun resumeReminder_delegatesToResumeReminderUseCase() = runTest(testDispatcher) {
        val viewModel = RemindersViewModel(
            reminderRepository,
            categoryRepository,
            alarmScheduler,
            pauseReminderUseCase,
            resumeReminderUseCase,
            deleteReminderUseCase,
            getUpcomingRemindersUseCase
        )

        viewModel.resumeReminder("rem-123")
        testScheduler.advanceUntilIdle()

        coVerify { resumeReminderUseCase("rem-123", any()) }
    }

    @Test
    fun deleteReminder_delegatesToDeleteReminderUseCase() = runTest(testDispatcher) {
        val viewModel = RemindersViewModel(
            reminderRepository,
            categoryRepository,
            alarmScheduler,
            pauseReminderUseCase,
            resumeReminderUseCase,
            deleteReminderUseCase,
            getUpcomingRemindersUseCase
        )

        viewModel.deleteReminder("rem-123")
        testScheduler.advanceUntilIdle()

        coVerify { deleteReminderUseCase("rem-123") }
    }
}
