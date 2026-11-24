package com.dayflow.app.feature.tasks

import app.cash.turbine.test
import com.dayflow.app.domain.model.Category
import com.dayflow.app.domain.model.Priority
import com.dayflow.app.domain.model.Task
import com.dayflow.app.domain.model.TaskStatus
import com.dayflow.app.domain.repository.CategoryRepository
import com.dayflow.app.domain.repository.TaskRepository
import com.dayflow.app.domain.usecase.ChangeTaskDeadlineUseCase
import com.dayflow.app.domain.usecase.CompleteTaskUseCase
import com.dayflow.app.domain.usecase.DeleteTaskUseCase
import com.dayflow.app.domain.usecase.DeriveOverdue
import com.dayflow.app.domain.usecase.ReopenTaskUseCase
import com.dayflow.app.domain.usecase.RescheduleTaskPlannedTimeUseCase
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
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class TasksViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val taskRepository: TaskRepository = mockk(relaxed = true)
    private val categoryRepository: CategoryRepository = mockk(relaxed = true)
    private val completeTaskUseCase: CompleteTaskUseCase = mockk(relaxed = true)
    private val reopenTaskUseCase: ReopenTaskUseCase = mockk(relaxed = true)
    private val deleteTaskUseCase: DeleteTaskUseCase = mockk(relaxed = true)
    private val rescheduleTaskPlannedTimeUseCase: RescheduleTaskPlannedTimeUseCase = mockk(relaxed = true)
    private val changeTaskDeadlineUseCase: ChangeTaskDeadlineUseCase = mockk(relaxed = true)
    private val deriveOverdue = DeriveOverdue()

    private val allTasksFlow = MutableStateFlow<List<Task>>(emptyList())
    private val categoriesFlow = MutableStateFlow<List<Category>>(emptyList())

    private val today = LocalDate.now()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { taskRepository.getAllTasks() } returns allTasksFlow
        every { categoryRepository.getAllCategories() } returns categoriesFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = TasksViewModel(
        taskRepository = taskRepository,
        categoryRepository = categoryRepository,
        completeTaskUseCase = completeTaskUseCase,
        reopenTaskUseCase = reopenTaskUseCase,
        deleteTaskUseCase = deleteTaskUseCase,
        rescheduleTaskPlannedTimeUseCase = rescheduleTaskPlannedTimeUseCase,
        changeTaskDeadlineUseCase = changeTaskDeadlineUseCase,
        deriveOverdue = deriveOverdue
    )

    @Test
    fun tabFiltering_segregatesTasksCorrectly() = runTest(testDispatcher) {
        val todayTask = Task(id = "1", title = "Today task", plannedDate = today, status = TaskStatus.PENDING)
        val upcomingTask = Task(id = "2", title = "Upcoming task", deadlineDate = today.plusDays(2), status = TaskStatus.PENDING)
        val overdueTask = Task(id = "3", title = "Overdue task", deadlineDate = today.minusDays(1), status = TaskStatus.PENDING)
        val completedTask = Task(id = "4", title = "Completed task", status = TaskStatus.COMPLETED)

        allTasksFlow.value = listOf(todayTask, upcomingTask, overdueTask, completedTask)

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // initial loading state
            val stateToday = awaitItem()
            assertEquals(1, stateToday.tasks.size)
            assertEquals("Today task", stateToday.tasks.first().title)

            // Switch to Upcoming
            viewModel.selectTab(TaskFilterTab.UPCOMING)
            val stateUpcoming = awaitItem()
            assertEquals(1, stateUpcoming.tasks.size)
            assertEquals("Upcoming task", stateUpcoming.tasks.first().title)

            // Switch to Overdue
            viewModel.selectTab(TaskFilterTab.OVERDUE)
            val stateOverdue = awaitItem()
            assertEquals(1, stateOverdue.tasks.size)
            assertEquals("Overdue task", stateOverdue.tasks.first().title)

            // Switch to Completed
            viewModel.selectTab(TaskFilterTab.COMPLETED)
            val stateCompleted = awaitItem()
            assertEquals(1, stateCompleted.tasks.size)
            assertEquals("Completed task", stateCompleted.tasks.first().title)

            // Switch to All
            viewModel.selectTab(TaskFilterTab.ALL)
            val stateAll = awaitItem()
            assertEquals(4, stateAll.tasks.size)
        }
    }

    @Test
    fun categoryAndPriorityFilters_filterTasksCorrectly() = runTest(testDispatcher) {
        val t1 = Task(id = "1", title = "Math hw", categoryId = "cat_academics", priority = Priority.HIGH, plannedDate = today)
        val t2 = Task(id = "2", title = "Run 5k", categoryId = "cat_fitness", priority = Priority.LOW, plannedDate = today)

        allTasksFlow.value = listOf(t1, t2)

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // initial
            val state = awaitItem()
            assertEquals(2, state.tasks.size)

            // Filter by Academics
            viewModel.selectCategory("cat_academics")
            val stateCat = awaitItem()
            assertEquals(1, stateCat.tasks.size)
            assertEquals("Math hw", stateCat.tasks.first().title)

            // Filter by Priority High (matches)
            viewModel.selectPriority(Priority.HIGH)
            val statePrio = awaitItem()
            assertEquals(1, statePrio.tasks.size)

            // Filter by Priority Low (doesn't match cat_academics)
            viewModel.selectPriority(Priority.LOW)
            val stateNoMatch = awaitItem()
            assertEquals(0, stateNoMatch.tasks.size)
        }
    }

    @Test
    fun toggleTaskComplete_delegatesToCorrectUseCase() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val pendingTask = Task(id = "p-1", title = "Work", status = TaskStatus.PENDING)
        val completedTask = Task(id = "c-1", title = "Done", status = TaskStatus.COMPLETED)

        viewModel.toggleTaskComplete(pendingTask)
        testScheduler.advanceUntilIdle()
        coVerify { completeTaskUseCase("p-1", any()) }

        viewModel.toggleTaskComplete(completedTask)
        testScheduler.advanceUntilIdle()
        coVerify { reopenTaskUseCase("c-1", any(), any()) }
    }

    @Test
    fun reschedulePlannedTime_delegatesToRescheduleUseCase() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val targetDate = today.plusDays(3)
        val targetStart = LocalTime.of(10, 0)
        val targetEnd = LocalTime.of(11, 0)

        viewModel.reschedulePlannedTime("task-42", targetDate, targetStart, targetEnd)
        testScheduler.advanceUntilIdle()

        coVerify { rescheduleTaskPlannedTimeUseCase("task-42", targetDate, targetStart, targetEnd, any()) }
    }

    @Test
    fun deleteTask_delegatesToDeleteUseCase() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.deleteTask("task-del-1")
        testScheduler.advanceUntilIdle()

        coVerify { deleteTaskUseCase("task-del-1") }
    }
}
