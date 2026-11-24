package com.dayflow.app.domain.usecase

import com.dayflow.app.domain.model.Priority
import com.dayflow.app.domain.model.Task
import com.dayflow.app.domain.model.TaskStatus
import com.dayflow.app.domain.model.TaskSubtask
import com.dayflow.app.domain.repository.TaskRepository
import com.dayflow.app.scheduling.DeadlineReminderScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtaskIndependenceTest {

    private val taskRepository: TaskRepository = mockk(relaxed = true)
    private val deadlineReminderScheduler: DeadlineReminderScheduler = mockk(relaxed = true)

    private val toggleSubtaskUseCase = ToggleSubtaskUseCase(taskRepository)
    private val completeTaskUseCase = CompleteTaskUseCase(taskRepository, deadlineReminderScheduler)

    @Test
    fun completingSubtask_updatesOnlyThatSubtask_andNeverCompletesParentTask() = runTest {
        val taskId = "parent-task-1"
        val subtask1 = TaskSubtask(id = "sub-1", taskId = taskId, title = "Step 1", isCompleted = false)
        val subtask2 = TaskSubtask(id = "sub-2", taskId = taskId, title = "Step 2", isCompleted = false)

        val parentTask = Task(
            id = taskId,
            title = "Major Project",
            status = TaskStatus.PENDING,
            subtasks = listOf(subtask1, subtask2)
        )

        coEvery { taskRepository.getTaskByIdDirect(taskId) } returns parentTask
        coEvery { taskRepository.getSubtaskById("sub-1") } returns subtask1
        coEvery { taskRepository.getSubtaskById("sub-2") } returns subtask2

        val updatedSubtaskSlot = slot<TaskSubtask>()
        coEvery { taskRepository.updateSubtask(capture(updatedSubtaskSlot)) } returns Unit

        // Complete subtask 1
        toggleSubtaskUseCase(subtaskId = "sub-1", isCompleted = true)

        assertTrue(updatedSubtaskSlot.captured.isCompleted)
        assertEquals("sub-1", updatedSubtaskSlot.captured.id)

        // Parent task should NEVER be updated or marked completed by subtask toggling
        coVerify(exactly = 0) { taskRepository.saveTask(any()) }
    }

    @Test
    fun completingAllSubtasks_stillLeavesParentTaskPending() = runTest {
        val taskId = "parent-task-2"
        val subtask1 = TaskSubtask(id = "sub-1", taskId = taskId, title = "Design", isCompleted = false)
        val subtask2 = TaskSubtask(id = "sub-2", taskId = taskId, title = "Code", isCompleted = false)

        val parentTask = Task(
            id = taskId,
            title = "Feature",
            status = TaskStatus.PENDING,
            subtasks = listOf(subtask1, subtask2)
        )

        coEvery { taskRepository.getTaskByIdDirect(taskId) } returns parentTask
        coEvery { taskRepository.getSubtaskById("sub-1") } returns subtask1
        coEvery { taskRepository.getSubtaskById("sub-2") } returns subtask2

        toggleSubtaskUseCase("sub-1", true)
        toggleSubtaskUseCase("sub-2", true)

        // Subtasks are tracked separately; parent task remains PENDING until user completes it
        coVerify(exactly = 0) { taskRepository.saveTask(any()) }
    }

    @Test
    fun parentTaskCanBeCompleted_evenIfSubtasksAreIncomplete() = runTest {
        val taskId = "parent-task-3"
        val subtask1 = TaskSubtask(id = "sub-1", taskId = taskId, title = "Optional notes", isCompleted = false)

        val parentTask = Task(
            id = taskId,
            title = "Deliverable",
            status = TaskStatus.PENDING,
            subtasks = listOf(subtask1)
        )

        coEvery { taskRepository.getTaskByIdDirect(taskId) } returns parentTask
        val savedTaskSlot = slot<Task>()
        coEvery { taskRepository.saveTask(capture(savedTaskSlot)) } returns Unit

        completeTaskUseCase(taskId)

        assertEquals(TaskStatus.COMPLETED, savedTaskSlot.captured.status)
        coVerify { deadlineReminderScheduler.cancelDeadlineReminders(taskId) }
    }
}
