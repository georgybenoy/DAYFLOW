package com.dayflow.app.domain.usecase

import com.dayflow.app.domain.model.TaskStatus
import com.dayflow.app.domain.repository.TaskRepository
import com.dayflow.app.scheduling.DeadlineReminderScheduler
import java.time.Instant
import javax.inject.Inject

class CancelTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val deadlineReminderScheduler: DeadlineReminderScheduler
) {
    suspend operator fun invoke(taskId: String, now: Instant = Instant.now()): Boolean {
        val task = taskRepository.getTaskByIdDirect(taskId) ?: return false
        val cancelledTask = task.copy(
            status = TaskStatus.CANCELLED,
            updatedAt = now
        )
        taskRepository.saveTask(cancelledTask)
        deadlineReminderScheduler.cancelDeadlineReminders(taskId)
        return true
    }
}
