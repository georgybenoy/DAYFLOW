package com.dayflow.app.domain.usecase

import com.dayflow.app.domain.model.DeadlineReminderLeadTime
import com.dayflow.app.domain.model.Task
import com.dayflow.app.domain.model.TaskSubtask
import com.dayflow.app.domain.repository.TaskRepository
import com.dayflow.app.scheduling.DeadlineReminderScheduler
import java.time.Instant
import javax.inject.Inject

class CreateTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val deadlineReminderScheduler: DeadlineReminderScheduler
) {
    suspend operator fun invoke(
        task: Task,
        subtasks: List<TaskSubtask> = emptyList(),
        deadlineReminders: Set<DeadlineReminderLeadTime> = emptySet(),
        now: Instant = Instant.now()
    ): Task {
        val createdTask = task.copy(
            createdAt = now,
            updatedAt = now
        )
        taskRepository.saveTaskWithSubtasks(createdTask, subtasks)
        if (deadlineReminders.isNotEmpty()) {
            deadlineReminderScheduler.scheduleDeadlineReminders(createdTask, deadlineReminders, now)
        }
        return createdTask
    }
}
