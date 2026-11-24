package com.dayflow.app.domain.usecase

import com.dayflow.app.domain.model.Task
import com.dayflow.app.domain.model.TaskStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeriveOverdue @Inject constructor() {

    /**
     * Determines whether a task deadline is overdue based on the specified reference date and time.
     * Pure function with zero side effects. Overdue is never stored as a database column.
     *
     * Boundary rules:
     * - Only tasks in PENDING status can be overdue (COMPLETED or CANCELLED tasks are never overdue).
     * - Tasks without a deadlineDate are never overdue.
     * - If deadlineDate is before currentDate: overdue.
     * - If deadlineDate is after currentDate: not overdue.
     * - If deadlineDate equals currentDate:
     *     - If deadlineTime is specified: overdue only if currentTime > deadlineTime.
     *     - If deadlineTime is omitted: not overdue on the deadline day (entire day available).
     */
    fun isOverdue(
        deadlineDate: LocalDate?,
        deadlineTime: LocalTime?,
        status: TaskStatus,
        now: LocalDateTime = LocalDateTime.now()
    ): Boolean {
        if (status != TaskStatus.PENDING || deadlineDate == null) return false

        val currentDate = now.toLocalDate()
        val currentTime = now.toLocalTime()

        if (deadlineDate.isBefore(currentDate)) {
            return true
        }

        if (deadlineDate.isEqual(currentDate) && deadlineTime != null) {
            return currentTime.isAfter(deadlineTime)
        }

        return false
    }

    operator fun invoke(task: Task, now: LocalDateTime = LocalDateTime.now()): Boolean {
        return isOverdue(task.deadlineDate, task.deadlineTime, task.status, now)
    }
}
