package com.dayflow.app.feature.plan

import androidx.lifecycle.SavedStateHandle
import com.dayflow.app.core.time.TimeProvider
import com.dayflow.app.domain.model.DailyPlan
import com.dayflow.app.domain.model.DailyPlanItem
import com.dayflow.app.domain.model.PlanItemSourceType
import com.dayflow.app.domain.model.PlanStatus
import com.dayflow.app.domain.model.Priority
import com.dayflow.app.domain.model.Task
import com.dayflow.app.domain.model.TaskStatus
import com.dayflow.app.domain.planner.DraftPlanResult
import com.dayflow.app.domain.planner.UnscheduledItem
import com.dayflow.app.domain.repository.DailyPlanRepository
import com.dayflow.app.domain.repository.TaskRepository
import com.dayflow.app.domain.usecase.ApproveDailyPlanUseCase
import com.dayflow.app.domain.usecase.GenerateDailyPlanUseCase
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class PlanViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val generateDailyPlanUseCase: GenerateDailyPlanUseCase = mockk()
    private val approveDailyPlanUseCase: ApproveDailyPlanUseCase = mockk()
    private val dailyPlanRepository: DailyPlanRepository = mockk(relaxed = true)
    private val taskRepository: TaskRepository = mockk(relaxed = true)

    private val fixedDate = LocalDate.of(2026, 11, 20)
    private val timeProvider = object : TimeProvider {
        override fun nowInstant(): Instant = Instant.now()
        override fun nowLocalDateTime(): LocalDateTime = LocalDateTime.of(fixedDate, LocalTime.of(10, 0))
        override fun nowLocalDate(): LocalDate = fixedDate
        override fun nowLocalTime(): LocalTime = LocalTime.of(10, 0)
        override fun zoneId(): ZoneId = ZoneId.systemDefault()
    }

    private val sampleTask1 = Task(
        id = "task-1",
        title = "Write API Specs",
        priority = Priority.HIGH,
        status = TaskStatus.PENDING,
        estimatedDurationMinutes = 60,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )

    private val sampleTask2 = Task(
        id = "task-2",
        title = "Fix UI Glitch",
        priority = Priority.MEDIUM,
        status = TaskStatus.PENDING,
        estimatedDurationMinutes = 30,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )

    private val samplePlanItem1 = DailyPlanItem(
        id = "item-1",
        planId = "plan-1",
        sourceType = PlanItemSourceType.TASK,
        sourceId = "task-1",
        title = "Write API Specs",
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(10, 0),
        orderIndex = 0
    )

    private val samplePlanItem2 = DailyPlanItem(
        id = "item-2",
        planId = "plan-1",
        sourceType = PlanItemSourceType.TASK,
        sourceId = "task-2",
        title = "Fix UI Glitch",
        startTime = LocalTime.of(10, 0),
        endTime = LocalTime.of(10, 30),
        orderIndex = 1
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { taskRepository.getAllTasks() } returns flowOf(listOf(sampleTask1, sampleTask2))
        coEvery { dailyPlanRepository.getPlanByDateDirect(fixedDate) } returns null
        coEvery { generateDailyPlanUseCase(fixedDate, any()) } returns DraftPlanResult(
            date = fixedDate,
            scheduledItems = listOf(samplePlanItem1, samplePlanItem2),
            unscheduledItems = emptyList(),
            conflicts = emptyList(),
            totalScheduledMinutes = 90,
            availableMinutes = 480
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(dateString: String? = fixedDate.toString()): PlanViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("dateString" to dateString))
        return PlanViewModel(
            generateDailyPlanUseCase = generateDailyPlanUseCase,
            approveDailyPlanUseCase = approveDailyPlanUseCase,
            dailyPlanRepository = dailyPlanRepository,
            taskRepository = taskRepository,
            timeProvider = timeProvider,
            savedStateHandle = savedStateHandle
        )
    }

    @Test
    fun `initial load - sets scheduled items, plan status and total minutes`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(PlanStatus.DRAFT, state.planStatus)
        assertEquals(2, state.scheduledItems.size)
        assertEquals(90, state.totalScheduledMinutes)
    }

    @Test
    fun `moveItemUp - swaps order and adjusts time slots`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Move item 1 (index 1) up to index 0
        viewModel.moveItemUp(1)
        advanceUntilIdle()

        val items = viewModel.uiState.value.scheduledItems
        assertEquals("item-2", items[0].id)
        assertEquals("item-1", items[1].id)

        // item-2 duration was 30 mins, now starts at 9:00 -> ends at 9:30
        assertEquals(LocalTime.of(9, 0), items[0].startTime)
        assertEquals(LocalTime.of(9, 30), items[0].endTime)

        // item-1 duration was 60 mins, now starts at 9:30 -> ends at 10:30
        assertEquals(LocalTime.of(9, 30), items[1].startTime)
        assertEquals(LocalTime.of(10, 30), items[1].endTime)
    }

    @Test
    fun `removeItemToUnscheduled - moves item to unscheduled list and never deletes task`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.removeItemToUnscheduled("item-1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.scheduledItems.size)
        assertEquals("item-2", state.scheduledItems[0].id)

        assertEquals(1, state.unscheduledItems.size)
        assertEquals("task-1", state.unscheduledItems[0].task.id)
    }

    @Test
    fun `scheduleUnscheduledTask - moves unscheduled task into scheduled list`() = runTest(testDispatcher) {
        // Setup initial state with unscheduled task
        coEvery { generateDailyPlanUseCase(fixedDate, any()) } returns DraftPlanResult(
            date = fixedDate,
            scheduledItems = listOf(samplePlanItem1),
            unscheduledItems = listOf(
                UnscheduledItem(task = sampleTask2, reason = "Time limit")
            ),
            conflicts = emptyList(),
            totalScheduledMinutes = 60,
            availableMinutes = 480
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.unscheduledItems.size)

        viewModel.scheduleUnscheduledTask(sampleTask2)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(0, state.unscheduledItems.size)
        assertEquals(2, state.scheduledItems.size)
        assertEquals("Fix UI Glitch", state.scheduledItems[1].title)
    }

    @Test
    fun `approvePlan - calls approve use case and updates status to APPROVED`() = runTest(testDispatcher) {
        coEvery { approveDailyPlanUseCase(fixedDate, any()) } returns DailyPlan(
            id = "plan-approved",
            localDate = fixedDate,
            status = PlanStatus.APPROVED,
            items = listOf(samplePlanItem1, samplePlanItem2)
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.approvePlan()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSaving)
        assertEquals(PlanStatus.APPROVED, state.planStatus)
        coVerify { approveDailyPlanUseCase(fixedDate, any()) }
    }
}
