package com.dayflow.app.domain.briefing

import com.dayflow.app.domain.model.DailyPlanItem
import com.dayflow.app.domain.model.Habit
import com.dayflow.app.domain.model.HabitLog
import com.dayflow.app.domain.model.Priority
import com.dayflow.app.domain.model.Reminder
import com.dayflow.app.domain.model.Task
import com.dayflow.app.domain.model.TaskStatus
import com.dayflow.app.domain.recurrence.ActiveDays
import com.dayflow.app.domain.recurrence.HabitProgressCalculator
import java.time.LocalDate

/**
 * Pure Kotlin generator for daily morning briefings.
 *
 * Guarantees:
 * 1. 100% deterministic and generated exclusively from local database state.
 * 2. Never fabricates tasks, activities, or placeholder commitments.
 * 3. Graceful empty text when the schedule is clear.
 * 4. Does not duplicate the full daily plan — provides executive summary highlights.
 */
object BriefingGenerator {

    fun generate(
        userName: String,
        date: LocalDate,
        tasks: List<Task>,
        scheduledPlanItems: List<DailyPlanItem>,
        habits: List<Habit>,
        habitLogs: List<HabitLog>,
        reminders: List<Reminder>
    ): BriefingContent {
        val greeting = if (userName.isNotBlank()) {
            "Good morning, $userName"
        } else {
            "Good morning"
        }

        // 1. Pending tasks due or planned for today
        val tasksForToday = tasks.filter { task ->
            task.status == TaskStatus.PENDING && (task.plannedDate == date || task.deadlineDate == date)
        }

        // 2. High and urgent priority tasks
        val priorities = tasksForToday.filter {
            it.priority == Priority.URGENT || it.priority == Priority.HIGH
        }.sortedWith(
            compareBy<Task> { it.priority }
                .thenBy { it.deadlineTime ?: java.time.LocalTime.MAX }
        )

        // 3. Today's deadlines
        val deadlines = tasks.filter { task ->
            task.status == TaskStatus.PENDING && task.deadlineDate == date
        }.sortedBy { it.deadlineTime ?: java.time.LocalTime.MAX }

        // 4. Habit checklist for today
        val activeHabits = habits.filter { habit ->
            habit.isEnabled && ActiveDays.isDayActive(habit.activeDaysMask, date.dayOfWeek)
        }
        val logsByHabitId = habitLogs.associateBy { it.habitId }
        val habitItems = activeHabits.map { habit ->
            val log = logsByHabitId[habit.id]
            val progress = HabitProgressCalculator.calculateProgress(habit, log)
            BriefingHabitItem(
                habit = habit,
                isCompleted = progress.isCompleted,
                formattedProgress = progress.formattedProgress
            )
        }

        // 5. Important Reminders (Urgent or High)
        val importantReminders = reminders.filter { reminder ->
            reminder.isEnabled && (reminder.priority == Priority.URGENT || reminder.priority == Priority.HIGH)
        }

        // 6. Check empty state
        val isEmpty = priorities.isEmpty() &&
            scheduledPlanItems.isEmpty() &&
            deadlines.isEmpty() &&
            habitItems.isEmpty() &&
            importantReminders.isEmpty()

        // 7. Executive summary text
        val summaryText = if (isEmpty) {
            "Your schedule is open today. Take time to focus on personal goals or add new commitments when you're ready."
        } else {
            buildSummaryText(
                scheduledCount = scheduledPlanItems.size,
                priorityCount = priorities.size,
                deadlineCount = deadlines.size,
                habitCount = habitItems.size
            )
        }

        return BriefingContent(
            date = date,
            greeting = greeting,
            summaryText = summaryText,
            priorities = priorities,
            scheduledTasks = scheduledPlanItems,
            deadlines = deadlines,
            habitsChecklist = habitItems,
            pendingTasksCount = tasksForToday.size,
            importantReminders = importantReminders,
            isEmpty = isEmpty
        )
    }

    private fun buildSummaryText(
        scheduledCount: Int,
        priorityCount: Int,
        deadlineCount: Int,
        habitCount: Int
    ): String {
        val parts = mutableListOf<String>()
        if (scheduledCount > 0) {
            parts.add("$scheduledCount scheduled task${if (scheduledCount > 1) "s" else ""}")
        } else if (priorityCount > 0) {
            parts.add("$priorityCount high-priority task${if (priorityCount > 1) "s" else ""}")
        }
        if (deadlineCount > 0) {
            parts.add("$deadlineCount deadline${if (deadlineCount > 1) "s" else ""}")
        }
        if (habitCount > 0) {
            parts.add("$habitCount habit${if (habitCount > 1) "s" else ""}")
        }

        return when {
            parts.isEmpty() -> "You have commitments prepared for today."
            parts.size == 1 -> "Today you have ${parts[0]}."
            parts.size == 2 -> "Today you have ${parts[0]} and ${parts[1]}."
            else -> "Today you have ${parts.dropLast(1).joinToString(", ")}, and ${parts.last()}."
        }
    }
}
