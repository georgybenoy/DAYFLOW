package com.dayflow.app.feature.review

import androidx.lifecycle.SavedStateHandle
import com.dayflow.app.core.time.TimeProvider
import com.dayflow.app.domain.model.DailyReview
import com.dayflow.app.domain.model.Priority
import com.dayflow.app.domain.model.ReviewDecisionType
import com.dayflow.app.domain.model.Task
import com.dayflow.app.domain.model.TaskReviewDecision
import com.dayflow.app.domain.model.TaskStatus
import com.dayflow.app.domain.planner.DraftPlanResult
import com.dayflow.app.domain.repository.DailyReviewRepository
import com.dayflow.app.domain.repository.HabitRepository
import com.dayflow.app.domain.repository.TaskRepository
import com.dayflow.app.domain.usecase.CancelTaskUseCase
import com.dayflow.app.domain.usecase.CompleteTaskUseCase
import com.dayflow.app.domain.usecase.CreateTaskUseCase
import com.dayflow.app.domain.usecase.GenerateDailyPlanUseCase
import com.dayflow.app.domain.usecase.RescheduleTaskPlannedTimeUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val taskRepository: TaskRepository = mockk(relaxed = true)
    private val habitRepository: HabitRepository = mockk(relaxed = true)
    private val dailyReviewRepository: DailyReviewRepository = mockk(relaxed = true)
    private val generateDailyPlanUseCase: GenerateDailyPlanUseCase = mockk(relaxed = true)
    private val completeTaskUseCase: CompleteTaskUseCase = mockk(relaxed = true)
    private val cancelTaskUseCase: CancelTaskUseCase = mockk(relaxed = true)
    private val rescheduleTaskPlannedTimeUseCase: RescheduleTaskPlannedTimeUseCase = mockk(relaxed = true)
    private val createTaskUseCase: CreateTaskUseCase = mockk(relaxed = true)
    private val timeProvider: TimeProvider = mockk(relaxed = true)

    private val testDate = LocalDate.of(2026, 11, 19)
    private val testInstant = Instant.parse("2026-11-19T21:30:00Z")

    private lateinit var viewModel: ReviewViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { timeProvider.nowLocalDate() } returns testDate
        every { timeProvider.nowInstant() } returns testInstant
        every { habitRepository.getActiveHabits() } returns flowOf(emptyList())
        every { habitRepository.getLogsForDate(any()) } returns flowOf(emptyList())
        coEvery { dailyReviewRepository.getReviewByDateDirect(any()) } returns null
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `undecided unfinished tasks strictly default to CARRIED_FORWARD`() = runTest {
        val completedTask = Task(
            id = "t_done",
            title = "Morning Standup",
            status = TaskStatus.COMPLETED,
            plannedDate = testDate
        )
        val unfinishedTask1 = Task(
            id = "t_pending_1",
            title = "Finish Quarterly Report",
            status = TaskStatus.PENDING,
            plannedDate = testDate
        )
        val unfinishedTask2 = Task(
            id = "t_pending_2",
            title = "Submit Budget",
            status = TaskStatus.PENDING,
            plannedDate = testDate
        )

        every { taskRepository.getAllTasks() } returns flowOf(listOf(completedTask, unfinishedTask1, unfinishedTask2))

        viewModel = ReviewViewModel(
            taskRepository = taskRepository,
            habitRepository = habitRepository,
            dailyReviewRepository = dailyReviewRepository,
            generateDailyPlanUseCase = generateDailyPlanUseCase,
            completeTaskUseCase = completeTaskUseCase,
            cancelTaskUseCase = cancelTaskUseCase,
            rescheduleTaskPlannedTimeUseCase = rescheduleTaskPlannedTimeUseCase,
            createTaskUseCase = createTaskUseCase,
            timeProvider = timeProvider,
            savedStateHandle = SavedStateHandle(mapOf("dateString" to testDate.toString()))
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.completedTasks.size)
        assertEquals(2, state.unfinishedTasks.size)

        // Strict invariant check: both unfinished tasks must default to CARRIED_FORWARD
        assertEquals(ReviewDecisionType.CARRIED_FORWARD, state.decisions["t_pending_1"]?.decision)
        assertEquals(ReviewDecisionType.CARRIED_FORWARD, state.decisions["t_pending_2"]?.decision)
    }

    @Test
    fun `user can update task decisions to reschedule, complete, or cancel`() = runTest {
        val unfinishedTask = Task(
            id = "t_pending_1",
            title = "Design Mockup",
            status = TaskStatus.PENDING,
            plannedDate = testDate
        )
        every { taskRepository.getAllTasks() } returns flowOf(listOf(unfinishedTask))

        viewModel = ReviewViewModel(
            taskRepository = taskRepository,
            habitRepository = habitRepository,
            dailyReviewRepository = dailyReviewRepository,
            generateDailyPlanUseCase = generateDailyPlanUseCase,
            completeTaskUseCase = completeTaskUseCase,
            cancelTaskUseCase = cancelTaskUseCase,
            rescheduleTaskPlannedTimeUseCase = rescheduleTaskPlannedTimeUseCase,
            createTaskUseCase = createTaskUseCase,
            timeProvider = timeProvider,
            savedStateHandle = SavedStateHandle(mapOf("dateString" to testDate.toString()))
        )
        advanceUntilIdle()

        val tomorrow = testDate.plusDays(1)
        viewModel.setTaskDecision("t_pending_1", ReviewDecisionType.RESCHEDULED, tomorrow)

        val decision = viewModel.uiState.value.decisions["t_pending_1"]
        assertNotNull(decision)
        assertEquals(ReviewDecisionType.RESCHEDULED, decision?.decision)
        assertEquals(tomorrow.toString(), decision?.rescheduledToDate)
    }

    @Test
    fun `addInlineCommitment creates task for tomorrow with high priority`() = runTest {
        every { taskRepository.getAllTasks() } returns flowOf(emptyList())
        coEvery { createTaskUseCase(any()) } answers { firstArg() }

        viewModel = ReviewViewModel(
            taskRepository = taskRepository,
            habitRepository = habitRepository,
            dailyReviewRepository = dailyReviewRepository,
            generateDailyPlanUseCase = generateDailyPlanUseCase,
            completeTaskUseCase = completeTaskUseCase,
            cancelTaskUseCase = cancelTaskUseCase,
            rescheduleTaskPlannedTimeUseCase = rescheduleTaskPlannedTimeUseCase,
            createTaskUseCase = createTaskUseCase,
            timeProvider = timeProvider,
            savedStateHandle = SavedStateHandle(mapOf("dateString" to testDate.toString()))
        )
        advanceUntilIdle()

        viewModel.addInlineCommitment(
            title = "Review Q3 Deck with Team",
            priority = Priority.HIGH,
            estimatedDurationMinutes = 45
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.newCommitments.size)
        assertEquals("Review Q3 Deck with Team", state.newCommitments[0].title)
        assertEquals(testDate.plusDays(1), state.newCommitments[0].plannedDate)
        assertEquals(Priority.HIGH, state.newCommitments[0].priority)
        assertEquals(45, state.newCommitments[0].estimatedDurationMinutes)

        coVerify {
            createTaskUseCase(
                task = match {
                    it.title == "Review Q3 Deck with Team" &&
                    it.plannedDate == testDate.plusDays(1) &&
                    it.priority == Priority.HIGH
                },
                subtasks = any(),
                deadlineReminders = any(),
                now = any()
            )
        }
    }

    @Test
    fun `finishReview applies decisions, persists DailyReview, and triggers tomorrow draft plan`() = runTest {
        val completedTask = Task(id = "t_done", title = "Task 1", status = TaskStatus.COMPLETED, plannedDate = testDate)
        val rescheduledTask = Task(id = "t_resched", title = "Task 2", status = TaskStatus.PENDING, plannedDate = testDate)
        val carriedTask = Task(id = "t_carry", title = "Task 3", status = TaskStatus.PENDING, plannedDate = testDate)

        every { taskRepository.getAllTasks() } returns flowOf(listOf(completedTask, rescheduledTask, carriedTask))
        coEvery { completeTaskUseCase(any(), any()) } returns true
        coEvery { rescheduleTaskPlannedTimeUseCase(any(), any(), any(), any(), any()) } returns rescheduledTask
        coEvery { dailyReviewRepository.saveReview(any()) } returns Unit
        coEvery { generateDailyPlanUseCase(any(), any()) } returns DraftPlanResult(
            date = testDate.plusDays(1),
            scheduledItems = emptyList(),
            unscheduledItems = emptyList(),
            conflicts = emptyList(),
            totalScheduledMinutes = 0,
            availableMinutes = 480
        )

        viewModel = ReviewViewModel(
            taskRepository = taskRepository,
            habitRepository = habitRepository,
            dailyReviewRepository = dailyReviewRepository,
            generateDailyPlanUseCase = generateDailyPlanUseCase,
            completeTaskUseCase = completeTaskUseCase,
            cancelTaskUseCase = cancelTaskUseCase,
            rescheduleTaskPlannedTimeUseCase = rescheduleTaskPlannedTimeUseCase,
            createTaskUseCase = createTaskUseCase,
            timeProvider = timeProvider,
            savedStateHandle = SavedStateHandle(mapOf("dateString" to testDate.toString()))
        )
        advanceUntilIdle()

        val tomorrow = testDate.plusDays(1)
        viewModel.setTaskDecision("t_resched", ReviewDecisionType.RESCHEDULED, tomorrow)
        // t_carry remains CARRIED_FORWARD by default!

        var finishedTomorrowDate: LocalDate? = null
        viewModel.finishReview { date ->
            finishedTomorrowDate = date
        }
        advanceUntilIdle()

        assertEquals(tomorrow, finishedTomorrowDate)

        // Verifies rescheduleTaskPlannedTimeUseCase called for rescheduled task
        coVerify {
            rescheduleTaskPlannedTimeUseCase(
                taskId = "t_resched",
                plannedDate = tomorrow,
                now = testInstant
            )
        }

        // Verifies dailyReviewRepository saved the review record
        coVerify {
            dailyReviewRepository.saveReview(match { review ->
                review.localDate == testDate &&
                review.decisions.size == 2 &&
                review.decisions.any { it.taskId == "t_resched" && it.decision == ReviewDecisionType.RESCHEDULED } &&
                review.decisions.any { it.taskId == "t_carry" && it.decision == ReviewDecisionType.CARRIED_FORWARD }
            })
        }

        // Verifies generateDailyPlanUseCase was immediately triggered for tomorrow
        coVerify {
            generateDailyPlanUseCase(date = tomorrow, forceRegenerate = true)
        }
    }
}