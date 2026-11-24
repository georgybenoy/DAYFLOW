package com.dayflow.app.domain.usecase

import com.dayflow.app.domain.repository.TaskRepository
import com.dayflow.app.scheduling.DeadlineReminderScheduler
import javax.inject.Inject

class DeleteTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val deadlineReminderScheduler: DeadlineReminderScheduler
) {
    suspend operator fun invoke(taskId: String) {
        deadlineReminderScheduler.cancelDeadlineReminders(taskId)
        taskRepository.deleteTaskById(taskId)
    }
}
