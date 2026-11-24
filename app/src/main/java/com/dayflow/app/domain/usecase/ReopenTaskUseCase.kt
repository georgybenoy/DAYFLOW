package com.dayflow.app.domain.usecase

import com.dayflow.app.domain.model.DeadlineReminderLeadTime
import com.dayflow.app.domain.model.TaskStatus
import com.dayflow.app.domain.repository.TaskRepository
import com.dayflow.app.scheduling.DeadlineReminderScheduler
import java.time.Instant
import javax.inject.Inject

class ReopenTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val deadlineReminderScheduler: DeadlineReminderScheduler
) {
    suspend operator fun invoke(
        taskId: String,
        deadlineReminders: Set<DeadlineReminderLeadTime> = emptySet(),
        now: Instant = Instant.now()
    ): Boolean {
        val task = taskRepository.getTaskByIdDirect(taskId) ?: return false
        val reopenedTask = task.copy(
            status = TaskStatus.PENDING,
            completedAt = null,
            updatedAt = now
        )
        taskRepository.saveTask(reopenedTask)
        if (deadlineReminders.isNotEmpty()) {
            deadlineReminderScheduler.scheduleDeadlineReminders(reopenedTask, deadlineReminders, now)
        }
        return true
    }
}
