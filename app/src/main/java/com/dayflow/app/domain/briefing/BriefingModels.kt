package com.dayflow.app.domain.briefing

import com.dayflow.app.domain.model.DailyPlanItem
import com.dayflow.app.domain.model.Habit
import com.dayflow.app.domain.model.Reminder
import com.dayflow.app.domain.model.Task
import java.time.LocalDate

data class BriefingHabitItem(
    val habit: Habit,
    val isCompleted: Boolean,
    val formattedProgress: String
)

data class BriefingContent(
    val date: LocalDate,
    val greeting: String,
    val summaryText: String,
    val priorities: List<Task>,
    val scheduledTasks: List<DailyPlanItem>,
    val deadlines: List<Task>,
    val habitsChecklist: List<BriefingHabitItem>,
    val pendingTasksCount: Int,
    val importantReminders: List<Reminder>,
    val isEmpty: Boolean
)
