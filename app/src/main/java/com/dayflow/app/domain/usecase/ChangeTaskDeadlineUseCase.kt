package com.dayflow.app.domain.usecase

import com.dayflow.app.domain.model.DeadlineReminderLeadTime
import com.dayflow.app.domain.model.Task
import com.dayflow.app.domain.model.TaskStatus
import com.dayflow.app.domain.repository.TaskRepository
import com.dayflow.app.scheduling.DeadlineReminderScheduler
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

class ChangeTaskDeadlineUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val deadlineReminderScheduler: DeadlineReminderScheduler
) {
    /**
     * Explicitly updates the task deadline and recalculates/reschedules deadline reminders.
     */
    suspend operator fun invoke(
        taskId: String,
        deadlineDate: LocalDate?,
        deadlineTime: LocalTime?,
        deadlineReminders: Set<DeadlineReminderLeadTime> = emptySet(),
        now: Instant = Instant.now()
    ): Task? {
        val existingTask = taskRepository.getTaskByIdDirect(taskId) ?: return null

        val updatedTask = existingTask.copy(
            deadlineDate = deadlineDate,
            deadlineTime = deadlineTime,
            updatedAt = now
        )

        taskRepository.saveTask(updatedTask)
        deadlineReminderScheduler.cancelDeadlineReminders(taskId)

        if (updatedTask.status == TaskStatus.PENDING && deadlineReminders.isNotEmpty()) {
            deadlineReminderScheduler.scheduleDeadlineReminders(updatedTask, deadlineReminders, now)
        }

        return updatedTask
    }
}
