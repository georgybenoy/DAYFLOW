package com.dayflow.app.feature.tasks

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.dayflow.app.domain.model.Category
import com.dayflow.app.domain.model.DeadlineReminderLeadTime
import com.dayflow.app.domain.model.Priority
import com.dayflow.app.domain.model.Task
import com.dayflow.app.domain.model.TaskStatus
import com.dayflow.app.domain.model.TaskSubtask
import com.dayflow.app.domain.model.TaskValidationError
import com.dayflow.app.domain.repository.CategoryRepository
import com.dayflow.app.domain.repository.TaskRepository
import com.dayflow.app.domain.usecase.CreateTaskUseCase
import com.dayflow.app.domain.usecase.UpdateTaskUseCase
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class TaskCreateViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val taskRepository: TaskRepository = mockk(relaxed = true)
    private val categoryRepository: CategoryRepository = mockk(relaxed = true)
    private val createTaskUseCase: CreateTaskUseCase = mockk(relaxed = true)
    private val updateTaskUseCase: UpdateTaskUseCase = mockk(relaxed = true)
    private val deadlineReminderScheduler: DeadlineReminderScheduler = mockk(relaxed = true)

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

    private fun createViewModel(taskId: String? = null): TaskCreateViewModel {
        val savedStateHandle = SavedStateHandle().apply {
            if (taskId != null) set("taskId", taskId)
        }
        return TaskCreateViewModel(
            savedStateHandle = savedStateHandle,
            taskRepository = taskRepository,
            categoryRepository = categoryRepository,
            createTaskUseCase = createTaskUseCase,
            updateTaskUseCase = updateTaskUseCase,
            deadlineReminderScheduler = deadlineReminderScheduler
        )
    }

    @Test
    fun saveTask_withBlankTitle_setsValidationError() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.updateTitle("   ")
        viewModel.saveTask()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(TaskValidationError.BlankTitle, state.validationError)
        }
        coVerify(exactly = 0) { createTaskUseCase(any(), any(), any(), any()) }
    }

    @Test
    fun saveTask_withInvalidPlannedWindow_setsValidationError() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.updateTitle("Valid Title")
        viewModel.updatePlannedDate(LocalDate.now())
        viewModel.updatePlannedStart(LocalTime.of(15, 0))
        viewModel.updatePlannedEnd(LocalTime.of(14, 0)) // before start!

        viewModel.saveTask()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(TaskValidationError.InvalidPlannedTimeWindow, state.validationError)
        }
        coVerify(exactly = 0) { createTaskUseCase(any(), any(), any(), any()) }
    }

    @Test
    fun saveTask_validCreation_callsCreateTaskUseCase() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.updateTitle("Finish Physics Homework")
        viewModel.updateDescription("Chapters 4 and 5 problems")
        viewModel.updateCategory("cat_academics")
        viewModel.updatePriority(Priority.HIGH)
        viewModel.updateDeadlineDate(LocalDate.now().plusDays(2))
        viewModel.updateDeadlineTime(LocalTime.of(17, 0))
        viewModel.toggleDeadlineReminder(DeadlineReminderLeadTime.ONE_DAY_BEFORE)
        viewModel.addSubtask("Chapter 4")
        viewModel.addSubtask("Chapter 5")

        viewModel.saveTask()
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) {
            createTaskUseCase(
                task = match { it.title == "Finish Physics Homework" && it.priority == Priority.HIGH },
                subtasks = match { it.size == 2 },
                deadlineReminders = match { it.contains(DeadlineReminderLeadTime.ONE_DAY_BEFORE) },
                now = any()
            )
        }
    }

    @Test
    fun editMode_loadsExistingTask_andUpdatesSuccessfully() = runTest(testDispatcher) {
        val existingTask = Task(
            id = "task-edit-1",
            title = "Existing Task",
            priority = Priority.MEDIUM,
            status = TaskStatus.PENDING
        )
        val existingSubtasks = listOf(
            TaskSubtask(id = "sub-1", taskId = "task-edit-1", title = "Step 1", isCompleted = false)
        )

        coEvery { taskRepository.getTaskByIdDirect("task-edit-1") } returns existingTask
        coEvery { taskRepository.getSubtasksForTaskDirect("task-edit-1") } returns existingSubtasks
        coEvery { deadlineReminderScheduler.getActiveLeadTimes("task-edit-1") } returns setOf(DeadlineReminderLeadTime.ONE_DAY_BEFORE)

        val viewModel = createViewModel(taskId = "task-edit-1")
        testScheduler.advanceUntilIdle()

        viewModel.updateTitle("Updated Task Title")
        viewModel.saveTask()
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) {
            updateTaskUseCase(
                task = match { it.id == "task-edit-1" && it.title == "Updated Task Title" },
                subtasks = any(),
                deadlineReminders = any(),
                now = any()
            )
        }
    }
}
