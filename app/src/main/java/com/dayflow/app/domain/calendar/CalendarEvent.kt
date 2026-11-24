package com.dayflow.app.domain.calendar

import java.time.LocalDate
import java.time.LocalTime

enum class CalendarEventType(val displayName: String) {
    TASK("Task"),
    DEADLINE("Deadline"),
    REMINDER("Reminder"),
    HABIT("Habit"),
    PLAN_ITEM("Plan")
}

data class CalendarEvent(
    val id: String,
    val title: String,
    val description: String? = null,
    val type: CalendarEventType,
    val date: LocalDate,
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    val isCompleted: Boolean = false,
    val priorityName: String? = null,
    val referenceId: String // ID to navigate to (taskId, reminderId, habitId, planDate)
)
