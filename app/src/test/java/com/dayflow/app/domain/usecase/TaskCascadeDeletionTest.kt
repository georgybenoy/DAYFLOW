package com.dayflow.app.domain.usecase

import com.dayflow.app.domain.repository.TaskRepository
import com.dayflow.app.scheduling.DeadlineReminderScheduler
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class TaskCascadeDeletionTest {

    private val taskRepository: TaskRepository = mockk(relaxed = true)
    private val deadlineReminderScheduler: DeadlineReminderScheduler = mockk(relaxed = true)

    private val deleteTaskUseCase = DeleteTaskUseCase(taskRepository, deadlineReminderScheduler)

    @Test
    fun deleteTask_cancelsDeadlineRemindersAndDeletesTask() = runTest {
        val taskId = "task-to-delete-123"

        deleteTaskUseCase(taskId)

        // 1. Deadline reminders and their alarms/occurrences must be cancelled
        coVerify(exactly = 1) { deadlineReminderScheduler.cancelDeadlineReminders(taskId) }

        // 2. Task itself is deleted (Room cascade handles subtasks via ForeignKey.CASCADE)
        coVerify(exactly = 1) { taskRepository.deleteTaskById(taskId) }
    }
}
