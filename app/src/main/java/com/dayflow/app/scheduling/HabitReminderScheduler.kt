package com.dayflow.app.scheduling

import com.dayflow.app.domain.model.Habit
import com.dayflow.app.domain.model.HabitTrackingType
import com.dayflow.app.domain.model.Priority
import com.dayflow.app.domain.model.Reminder
import com.dayflow.app.domain.model.ReminderPrecision
import com.dayflow.app.domain.model.ReminderType
import com.dayflow.app.domain.usecase.DeleteReminderUseCase
import com.dayflow.app.domain.usecase.ScheduleReminderUseCase
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages scheduling synchronization between [Habit] reminder preferences and the
 * core Dayflow reminder pipeline.
 *
 * Invariant: Reuses [ScheduleReminderUseCase] and [DeleteReminderUseCase] as the single
 * source of scheduling truth. No secondary scheduler or duplicated alarm management.
 */
@Singleton
class HabitReminderScheduler @Inject constructor(
    private val scheduleReminderUseCase: ScheduleReminderUseCase,
    private val deleteReminderUseCase: DeleteReminderUseCase
) {

    companion object {
        const val HABIT_REMINDER_PREFIX = "habit_reminder_"

        fun getReminderId(habitId: String): String = "$HABIT_REMINDER_PREFIX$habitId"

        fun isHabitReminder(reminderId: String): Boolean = reminderId.startsWith(HABIT_REMINDER_PREFIX)

        fun extractHabitId(reminderId: String): String = reminderId.removePrefix(HABIT_REMINDER_PREFIX)
    }

    /**
     * Synchronizes a habit's reminder configuration with the alarm pipeline.
     */
    suspend fun syncHabitReminder(habit: Habit) {
        val reminderId = getReminderId(habit.id)

        if (habit.isEnabled && habit.reminderPreference && habit.reminderTime != null) {
            val typeDescription = if (habit.trackingType == HabitTrackingType.BINARY) {
                "Habit: Binary"
            } else {
                "Habit: ${habit.trackingType.name}"
            }

            val reminder = Reminder(
                id = reminderId,
                title = habit.name,
                description = habit.description?.let { "$typeDescription • $it" } ?: typeDescription,
                categoryId = habit.categoryId,
                type = ReminderType.CUSTOM_WEEKDAYS,
                startDate = LocalDate.now(),
                startTime = habit.reminderTime,
                activeDaysMask = habit.activeDaysMask,
                priority = Priority.MEDIUM,
                precision = ReminderPrecision.EXACT,
                isEnabled = true,
                isPaused = false
            )
            scheduleReminderUseCase(reminder)
        } else {
            deleteReminderUseCase(reminderId)
        }
    }

    /**
     * Cancels and deletes any scheduled reminder alarms linked to this habit.
     */
    suspend fun cancelHabitReminder(habitId: String) {
        val reminderId = getReminderId(habitId)
        deleteReminderUseCase(reminderId)
    }
}
