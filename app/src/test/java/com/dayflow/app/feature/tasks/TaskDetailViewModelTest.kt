package com.dayflow.app.feature.tasks

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.dayflow.app.domain.model.Category
import com.dayflow.app.domain.model.DeadlineReminderLeadTime
import com.dayflow.app.domain.model.Priority
import com.dayflow.app.domain.model.Task
import com.dayflow.app.domain.model.TaskStatus
import com.dayflow.app.domain.model.TaskSubtask
import com.dayflow.app.domain.repository.CategoryRepository
import com.dayflow.app.domain.repository.TaskRepository
import com.dayflow.app.domain.usecase.AddSubtaskUseCase
import com.dayflow.app.domain.usecase.CancelTaskUseCase
import com.dayflow.app.domain.usecase.ChangeTaskDeadlineUseCase
import com.dayflow.app.domain.usecase.CompleteTaskUseCase
import com.dayflow.app.domain.usecase.DeleteSubtaskUseCase
import com.dayflow.app.domain.usecase.DeleteTaskUseCase
import com.dayflow.app.domain.usecase.DeriveOverdue
import com.dayflow.app.domain.usecase.ReopenTaskUseCase
import com.dayflow.app.domain.usecase.RescheduleTaskPlannedTimeUseCase
import com.dayflow.app.domain.usecase.ToggleSubtaskUseCase
import com.dayflow.app.scheduling.DeadlineReminderScheduler
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class TaskDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val taskRepository: TaskRepository = mockk(relaxed = true)
    private val categoryRepository: CategoryRepository = mockk(relaxed = true)
    private val completeTaskUseCase: CompleteTaskUseCase = mockk(relaxed = true)
    private val reopenTaskUseCase: ReopenTaskUseCase = mockk(relaxed = true)
    private val cancelTaskUseCase: CancelTaskUseCase = mockk(relaxed = true)
    private val deleteTaskUseCase: DeleteTaskUseCase = mockk(relaxed = true)
    private val toggleSubtaskUseCase: ToggleSubtaskUseCase = mockk(relaxed = true)
    private val addSubtaskUseCase: AddSubtaskUseCase = mockk(relaxed = true)
    private val deleteSubtaskUseCase: DeleteSubtaskUseCase = mockk(relaxed = true)
    private val rescheduleTaskPlannedTimeUseCase: RescheduleTaskPlannedTimeUseCase = mockk(relaxed = true)
    private val changeTaskDeadlineUseCase: ChangeTaskDeadlineUseCase = mockk(relaxed = true)
    private val deadlineReminderScheduler: DeadlineReminderScheduler = mockk(relaxed = true)
    private val deriveOverdue = DeriveOverdue()

    private val taskFlow = MutableStateFlow<Task?>(null)
    private val categoriesFlow = MutableStateFlow<List<Category>>(emptyList())

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { taskRepository.getTaskById("task-100") } returns taskFlow
        coEvery { taskRepository.getTaskByIdDirect("task-100") } answers { taskFlow.value }
        every { categoryRepository.getAllCategories() } returns categoriesFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = TaskDetailViewModel(
        savedStateHandle = SavedStateHandle(mapOf("taskId" to "task-100")),
        taskRepository = taskRepository,
        categoryRepository = categoryRepository,
        completeTaskUseCase = completeTaskUseCase,
        reopenTaskUseCase = reopenTaskUseCase,
        cancelTaskUseCase = cancelTaskUseCase,
        deleteTaskUseCase = deleteTaskUseCase,
        toggleSubtaskUseCase = toggleSubtaskUseCase,
        addSubtaskUseCase = addSubtaskUseCase,
        deleteSubtaskUseCase = deleteSubtaskUseCase,
        rescheduleTaskPlannedTimeUseCase = rescheduleTaskPlannedTimeUseCase,
        changeTaskDeadlineUseCase = changeTaskDeadlineUseCase,
        deadlineReminderScheduler = deadlineReminderScheduler,
        deriveOverdue = deriveOverdue
    )

    @Test
    fun loadTask_emitsCorrectDetails() = runTest(testDispatcher) {
        val sampleTask = Task(
            id = "task-100",
            title = "Research Paper",
            description = "Literature review",
            categoryId = "cat-1",
            priority = Priority.URGENT,
            status = TaskStatus.PENDING,
            subtasks = listOf(
                TaskSubtask(id = "sub-1", taskId = "task-100", title = "Abstract", isCompleted = true)
            )
        )
        val sampleCat = Category(id = "cat-1", name = "Academics", colorHex = "#76CDBB", iconName = "School")

        taskFlow.value = sampleTask
        categoriesFlow.value = listOf(sampleCat)

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // loading
            val state = awaitItem()
            assertEquals("Research Paper", state.task?.title)
            assertEquals("Academics", state.category?.name)
            assertEquals(1, state.task?.subtasks?.size)
            assertTrue(state.task?.subtasks?.first()?.isCompleted == true)
        }
    }

    @Test
    fun toggleSubtask_delegatesToToggleSubtaskUseCase() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.toggleSubtask("sub-42", isCompleted = true)
        testScheduler.advanceUntilIdle()

        coVerify { toggleSubtaskUseCase("sub-42", true) }
    }

    @Test
    fun toggleTaskComplete_delegatesToCompleteTaskUseCase() = runTest(testDispatcher) {
        taskFlow.value = Task(id = "task-100", title = "Project", status = TaskStatus.PENDING)
        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        viewModel.toggleTaskComplete()
        testScheduler.advanceUntilIdle()

        coVerify { completeTaskUseCase("task-100", any()) }
    }

    @Test
    fun cancelTask_delegatesToCancelTaskUseCase() = runTest(testDispatcher) {
        taskFlow.value = Task(id = "task-100", title = "Meeting", status = TaskStatus.PENDING)
        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        viewModel.cancelTask()
        testScheduler.advanceUntilIdle()

        coVerify { cancelTaskUseCase("task-100", any()) }
    }

    @Test
    fun deleteTask_delegatesToDeleteTaskUseCase_andSetsDeletedFlag() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.deleteTask()
        testScheduler.advanceUntilIdle()

        coVerify { deleteTaskUseCase("task-100") }
        assertTrue(viewModel.uiState.value.isDeleted)
    }

    @Test
    fun reschedulePlannedTime_delegatesToRescheduleUseCase() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val plannedDate = LocalDate.now().plusDays(1)
        val start = LocalTime.of(9, 30)
        val end = LocalTime.of(11, 0)

        viewModel.reschedulePlannedTime(plannedDate, start, end)
        testScheduler.advanceUntilIdle()

        coVerify { rescheduleTaskPlannedTimeUseCase("task-100", plannedDate, start, end, any()) }
    }

    @Test
    fun changeDeadline_delegatesToChangeDeadlineUseCase() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val deadlineDate = LocalDate.now().plusDays(5)
        val deadlineTime = LocalTime.of(18, 0)
        val reminders = setOf(DeadlineReminderLeadTime.AT_DEADLINE)

        viewModel.changeDeadline(deadlineDate, deadlineTime, reminders)
        testScheduler.advanceUntilIdle()

        coVerify { changeTaskDeadlineUseCase("task-100", deadlineDate, deadlineTime, reminders, any()) }
    }
}
