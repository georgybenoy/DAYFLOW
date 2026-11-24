package com.dayflow.app.feature.calendar

import app.cash.turbine.test
import com.dayflow.app.core.time.TimeProvider
import com.dayflow.app.domain.calendar.CalendarEvent
import com.dayflow.app.domain.calendar.CalendarEventType
import com.dayflow.app.domain.calendar.GetCalendarEventsUseCase
import com.dayflow.app.domain.model.UserProfile
import com.dayflow.app.domain.model.WeekStartDay
import com.dayflow.app.domain.repository.SettingsRepository
import io.mockk.coEvery
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val getCalendarEventsUseCase: GetCalendarEventsUseCase = mockk()
    private val settingsRepository: SettingsRepository = mockk()
    private val timeProvider: TimeProvider = mockk()

    private val userProfileFlow = MutableStateFlow(UserProfile(weekStartDay = WeekStartDay.MONDAY))
    private val testDate = LocalDate.of(2026, 11, 19)
    private val testZoneId = ZoneId.of("UTC")

    private val sampleEvents = listOf(
        CalendarEvent(
            id = "t1",
            title = "Task Event",
            type = CalendarEventType.TASK,
            date = testDate,
            startTime = LocalTime.of(10, 0),
            referenceId = "task_1"
        ),
        CalendarEvent(
            id = "r1",
            title = "Reminder Event",
            type = CalendarEventType.REMINDER,
            date = testDate,
            startTime = LocalTime.of(12, 0),
            referenceId = "rem_1"
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        every { timeProvider.zoneId() } returns testZoneId
        every { timeProvider.nowLocalDate() } returns testDate
        every { settingsRepository.userProfile } returns userProfileFlow
        coEvery { getCalendarEventsUseCase(any(), any(), any()) } returns sampleEvents
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadEvents_updatesUiStateWithEvents() = runTest {
        val viewModel = CalendarViewModel(
            getCalendarEventsUseCase = getCalendarEventsUseCase,
            settingsRepository = settingsRepository,
            timeProvider = timeProvider
        )

        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(testDate, state.selectedDate)
            assertEquals(CalendarViewMode.DAY, state.viewMode)
            assertEquals(2, state.events.size)
        }
    }

    @Test
    fun setFilter_filtersEventsByType() = runTest {
        val viewModel = CalendarViewModel(
            getCalendarEventsUseCase = getCalendarEventsUseCase,
            settingsRepository = settingsRepository,
            timeProvider = timeProvider
        )

        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setFilter(CalendarEventType.TASK)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(CalendarEventType.TASK, state.selectedFilter)
            assertEquals(1, state.events.size)
            assertEquals("Task Event", state.events.first().title)
        }
    }

    @Test
    fun dateNavigation_advancesAndDecrementsDatesAccurately() = runTest {
        val viewModel = CalendarViewModel(
            getCalendarEventsUseCase = getCalendarEventsUseCase,
            settingsRepository = settingsRepository,
            timeProvider = timeProvider
        )

        testDispatcher.scheduler.advanceUntilIdle()

        // In DAY mode, next advances 1 day
        viewModel.navigateNext()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(testDate.plusDays(1), viewModel.uiState.value.selectedDate)

        // Previous decrements back
        viewModel.navigatePrevious()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(testDate, viewModel.uiState.value.selectedDate)

        // In WEEK mode, next advances 1 week
        viewModel.setViewMode(CalendarViewMode.WEEK)
        viewModel.navigateNext()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(testDate.plusWeeks(1), viewModel.uiState.value.selectedDate)

        // In MONTH mode, previous decrements 1 month
        viewModel.setViewMode(CalendarViewMode.MONTH)
        viewModel.navigatePrevious()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(testDate.plusWeeks(1).minusMonths(1), viewModel.uiState.value.selectedDate)

        // goToToday returns to today
        viewModel.goToToday()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(testDate, viewModel.uiState.value.selectedDate)
    }

    @Test
    fun setViewMode_changesViewModeAndReloadsRange() = runTest {
        val viewModel = CalendarViewModel(
            getCalendarEventsUseCase = getCalendarEventsUseCase,
            settingsRepository = settingsRepository,
            timeProvider = timeProvider
        )

        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setViewMode(CalendarViewMode.MONTH)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(CalendarViewMode.MONTH, viewModel.uiState.value.viewMode)
    }
}
