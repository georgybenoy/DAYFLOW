package com.dayflow.app.domain.usecase

import com.dayflow.app.domain.model.Task
import com.dayflow.app.domain.repository.TaskRepository
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

class RescheduleTaskPlannedTimeUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    /**
     * Reschedules the planned execution window (plannedDate, plannedStart, plannedEnd).
     *
     * Invariants:
     * - Task identity is strictly preserved (never creates duplicate task entities).
     * - Deadline date and deadline time are NOT modified.
     * - Planned execution time is completely independent of deadline.
     */
    suspend operator fun invoke(
        taskId: String,
        plannedDate: LocalDate?,
        plannedStart: LocalTime? = null,
        plannedEnd: LocalTime? = null,
        now: Instant = Instant.now()
    ): Task? {
        val existingTask = taskRepository.getTaskByIdDirect(taskId) ?: return null

        val updatedTask = existingTask.copy(
            id = existingTask.id, // Identity preserved
            deadlineDate = existingTask.deadlineDate, // Deadline untouched
            deadlineTime = existingTask.deadlineTime, // Deadline untouched
            plannedDate = plannedDate,
            plannedStart = plannedStart,
            plannedEnd = plannedEnd,
            updatedAt = now
        )

        taskRepository.saveTask(updatedTask)
        return updatedTask
    }
}
