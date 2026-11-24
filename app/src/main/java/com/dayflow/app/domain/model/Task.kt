package com.dayflow.app.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

enum class TaskStatus {
    PENDING,
    COMPLETED,
    CANCELLED
}

data class TaskSubtask(
    val id: String = UUID.randomUUID().toString(),
    val taskId: String,
    val title: String,
    val isCompleted: Boolean = false,
    val orderIndex: Int = 0,
    val createdAt: Instant = Instant.now()
)

data class Task(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String? = null,
    val categoryId: String? = null,
    val priority: Priority = Priority.MEDIUM,
    val deadlineDate: LocalDate? = null,
    val deadlineTime: LocalTime? = null,
    val estimatedDurationMinutes: Int? = null,
    val plannedDate: LocalDate? = null,
    val plannedStart: LocalTime? = null,
    val plannedEnd: LocalTime? = null,
    val status: TaskStatus = TaskStatus.PENDING,
    val shouldCarryForward: Boolean = true,
    val createdAt: Instant = Instant.now(),
    val completedAt: Instant? = null,
    val updatedAt: Instant = Instant.now(),
    val subtasks: List<TaskSubtask> = emptyList()
) {
    /**
     * Overdue is a DERIVED attribute based on the current date/time.
     * Never stored as a database column to prevent stale status.
     */
    fun isOverdue(currentDate: LocalDate, currentTime: LocalTime): Boolean {
        if (status != TaskStatus.PENDING || deadlineDate == null) return false
        if (deadlineDate.isBefore(currentDate)) return true
        if (deadlineDate.isEqual(currentDate) && deadlineTime != null) {
            return deadlineTime.isBefore(currentTime)
        }
        return false
    }
}
