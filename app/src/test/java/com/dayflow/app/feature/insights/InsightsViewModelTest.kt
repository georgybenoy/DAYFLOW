package com.dayflow.app.feature.insights

import app.cash.turbine.test
import com.dayflow.app.core.time.TimeProvider
import com.dayflow.app.domain.analytics.DailyAnalytics
import com.dayflow.app.domain.analytics.GetInsightsUseCase
import com.dayflow.app.domain.analytics.InsightsData
import com.dayflow.app.domain.analytics.MonthlyAnalytics
import com.dayflow.app.domain.analytics.StreakSummary
import com.dayflow.app.domain.analytics.WeeklyAnalytics
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class InsightsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val getInsightsUseCase: GetInsightsUseCase = mockk()
    private val settingsRepository: SettingsRepository = mockk()
    private val timeProvider: TimeProvider = mockk()

    private val userProfileFlow = MutableStateFlow(UserProfile(weekStartDay = WeekStartDay.MONDAY))
    private val testDate = LocalDate.of(2026, 11, 19)
    private val testZoneId = ZoneId.of("UTC")

    private val dummyInsightsData = InsightsData(
        today = DailyAnalytics(
            date = testDate,
            executionScore = 85,
            hasActivity = true,
            tasksCompleted = 4,
            tasksTotal = 5,
            taskRatio = 0.8f,
            habitsCompleted = 2,
            habitsScheduled = 2,
            habitRatio = 1.0f,
            remindersCompleted = 3,
            remindersMissed = 0,
            reminderRatio = 1.0f,
            planItemsCompleted = 1,
            planItemsTotal = 1,
            planRatio = 1.0f
        ),
        weekly = WeeklyAnalytics(
            startDate = testDate.minusDays(3),
            endDate = testDate.plusDays(3),
            averageScore = 80,
            days = emptyList(),
            totalTasksCompleted = 12,
            habitConsistencyPercentage = 90,
            completedRemindersCount = 10,
            missedRemindersCount = 1
        ),
        monthly = MonthlyAnalytics(
            yearMonth = YearMonth.from(testDate),
            days = emptyList(),
            averageScore = 75,
            totalTasksCompleted = 45,
            habitConsistencyPercentage = 88
        ),
        streakSummary = StreakSummary(
            currentStreakDays = 5,
            longestStreakDays = 14
        ),
        trendDays = emptyList()
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        every { timeProvider.zoneId() } returns testZoneId
        every { timeProvider.nowLocalDate() } returns testDate
        every { settingsRepository.userProfile } returns userProfileFlow
        coEvery { getInsightsUseCase(any(), any(), any()) } returns dummyInsightsData
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadInsights_emitsSuccessStateWithData() = runTest {
        val viewModel = InsightsViewModel(
            getInsightsUseCase = getInsightsUseCase,
            settingsRepository = settingsRepository,
            timeProvider = timeProvider
        )

        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is InsightsUiState.Success)
            val success = state as InsightsUiState.Success
            assertEquals(85, success.data.today.executionScore)
            assertEquals(InsightsViewFilter.WEEK, success.selectedFilter)
        }
    }

    @Test
    fun setFilter_updatesSelectedFilterInUiState() = runTest {
        val viewModel = InsightsViewModel(
            getInsightsUseCase = getInsightsUseCase,
            settingsRepository = settingsRepository,
            timeProvider = timeProvider
        )

        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setFilter(InsightsViewFilter.MONTH)

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is InsightsUiState.Success)
            val success = state as InsightsUiState.Success
            assertEquals(InsightsViewFilter.MONTH, success.selectedFilter)
        }
    }

    @Test
    fun loadInsights_whenError_emitsErrorState() = runTest {
        coEvery { getInsightsUseCase(any(), any(), any()) } throws RuntimeException("Database error")

        val viewModel = InsightsViewModel(
            getInsightsUseCase = getInsightsUseCase,
            settingsRepository = settingsRepository,
            timeProvider = timeProvider
        )

        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is InsightsUiState.Error)
            val error = state as InsightsUiState.Error
            assertEquals("Database error", error.message)
        }
    }
}
