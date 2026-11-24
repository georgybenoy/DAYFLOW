package com.dayflow.app.domain.usecase

import com.dayflow.app.domain.model.DeadlineReminderLeadTime
import com.dayflow.app.domain.model.Task
import com.dayflow.app.domain.model.TaskStatus
import com.dayflow.app.domain.model.TaskSubtask
import com.dayflow.app.domain.repository.TaskRepository
import com.dayflow.app.scheduling.DeadlineReminderScheduler
import java.time.Instant
import javax.inject.Inject

class UpdateTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val deadlineReminderScheduler: DeadlineReminderScheduler
) {
    suspend operator fun invoke(
        task: Task,
        subtasks: List<TaskSubtask>,
        deadlineReminders: Set<DeadlineReminderLeadTime>,
        now: Instant = Instant.now()
    ): Task {
        val updatedTask = task.copy(
            updatedAt = now
        )
        taskRepository.updateTaskWithSubtasks(updatedTask, subtasks)
        deadlineReminderScheduler.cancelDeadlineReminders(task.id)
        if (updatedTask.status == TaskStatus.PENDING && deadlineReminders.isNotEmpty()) {
            deadlineReminderScheduler.scheduleDeadlineReminders(updatedTask, deadlineReminders, now)
        }
        return updatedTask
    }
}
