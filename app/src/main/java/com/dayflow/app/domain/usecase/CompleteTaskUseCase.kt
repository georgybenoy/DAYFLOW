package com.dayflow.app.domain.usecase

import com.dayflow.app.domain.model.TaskStatus
import com.dayflow.app.domain.repository.TaskRepository
import com.dayflow.app.scheduling.DeadlineReminderScheduler
import java.time.Instant
import javax.inject.Inject

class CompleteTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val deadlineReminderScheduler: DeadlineReminderScheduler
) {
    suspend operator fun invoke(taskId: String, completedAt: Instant = Instant.now()): Boolean {
        val task = taskRepository.getTaskByIdDirect(taskId) ?: return false
        if (task.status == TaskStatus.COMPLETED) return true

        val completedTask = task.copy(
            status = TaskStatus.COMPLETED,
            completedAt = completedAt,
            updatedAt = completedAt
        )
        taskRepository.saveTask(completedTask)
        deadlineReminderScheduler.cancelDeadlineReminders(taskId)
        return true
    }
}
