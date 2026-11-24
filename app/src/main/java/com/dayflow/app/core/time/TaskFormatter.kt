package com.dayflow.app.core.time

import com.dayflow.app.domain.model.Task
import com.dayflow.app.domain.model.TaskStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object TaskFormatter {

    private val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")
    private val shortDateFormatter = DateTimeFormatter.ofPattern("MMM d")
    private val fullDateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

    fun formatDeadline(
        task: Task,
        now: LocalDateTime = LocalDateTime.now(),
        isOverdue: Boolean = false
    ): String? {
        val deadlineDate = task.deadlineDate ?: return null
        val deadlineTime = task.deadlineTime
        val currentDate = now.toLocalDate()
        val daysDiff = ChronoUnit.DAYS.between(currentDate, deadlineDate)

        val datePart = when {
            daysDiff == 0L -> "Today"
            daysDiff == 1L -> "Tomorrow"
            daysDiff == -1L -> "Yesterday"
            daysDiff in 2..6 -> deadlineDate.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
            else -> deadlineDate.format(shortDateFormatter)
        }

        val timePart = deadlineTime?.let { " at ${it.format(timeFormatter)}" } ?: ""

        return if (isOverdue && task.status == TaskStatus.PENDING) {
            "Overdue · Was due $datePart$timePart"
        } else {
            "Due $datePart$timePart"
        }
    }

    fun formatPlannedWindow(task: Task): String? {
        val plannedDate = task.plannedDate ?: return null
        val datePart = plannedDate.format(shortDateFormatter)

        val start = task.plannedStart
        val end = task.plannedEnd

        return when {
            start != null && end != null -> "Planned: $datePart, ${start.format(timeFormatter)} - ${end.format(timeFormatter)}"
            start != null -> "Planned: $datePart at ${start.format(timeFormatter)}"
            else -> "Planned: $datePart"
        }
    }

    fun formatDuration(minutes: Int?): String? {
        if (minutes == null || minutes <= 0) return null
        val hours = minutes / 60
        val remainingMinutes = minutes % 60
        return when {
            hours > 0 && remainingMinutes > 0 -> "${hours}h ${remainingMinutes}m"
            hours > 0 -> "${hours}h"
            else -> "${remainingMinutes}m"
        }
    }

    fun formatSubtaskProgress(completedCount: Int, totalCount: Int): String {
        return "$completedCount of $totalCount subtasks"
    }
}
