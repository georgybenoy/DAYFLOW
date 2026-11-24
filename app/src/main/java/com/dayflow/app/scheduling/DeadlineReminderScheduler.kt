package com.dayflow.app.scheduling

import com.dayflow.app.domain.model.DeadlineReminderLeadTime
import com.dayflow.app.domain.model.Priority
import com.dayflow.app.domain.model.Reminder
import com.dayflow.app.domain.model.ReminderPrecision
import com.dayflow.app.domain.model.ReminderType
import com.dayflow.app.domain.model.Task
import com.dayflow.app.domain.model.TaskStatus
import com.dayflow.app.domain.repository.ReminderRepository
import com.dayflow.app.domain.usecase.DeleteReminderUseCase
import com.dayflow.app.domain.usecase.ScheduleReminderUseCase
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeadlineReminderScheduler @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val scheduleReminderUseCase: ScheduleReminderUseCase,
    private val deleteReminderUseCase: DeleteReminderUseCase
) {
    /**
     * Schedules configured deadline reminders for a task.
     * Reuses the single AlarmScheduler and Reminder pipeline, posting to Critical Deadlines channel.
     */
    suspend fun scheduleDeadlineReminders(
        task: Task,
        leadTimes: Set<DeadlineReminderLeadTime>,
        now: Instant = Instant.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ) {
        // Cancel existing deadline reminders for this task first
        cancelDeadlineReminders(task.id)

        if (task.status != TaskStatus.PENDING || task.deadlineDate == null) {
            return
        }

        val deadlineTime = task.deadlineTime ?: LocalTime.of(18, 0)
        val deadlineDateTime = LocalDateTime.of(task.deadlineDate, deadlineTime)

        for (leadTime in leadTimes) {
            val triggerDateTime = deadlineDateTime.minusMinutes(leadTime.minutesBefore)
            val triggerInstant = triggerDateTime.atZone(zoneId).toInstant()

            if (triggerInstant.isAfter(now)) {
                val reminderId = createReminderId(task.id, leadTime)
                val deadlineDescription = "Due on ${task.deadlineDate}" +
                    (if (task.deadlineTime != null) " at ${task.deadlineTime}" else "")

                val reminder = Reminder(
                    id = reminderId,
                    title = "Deadline: ${task.title}",
                    description = deadlineDescription,
                    categoryId = task.categoryId,
                    type = ReminderType.ONE_TIME,
                    startDate = triggerDateTime.toLocalDate(),
                    startTime = triggerDateTime.toLocalTime(),
                    intervalMinutes = null,
                    activeDaysMask = 127,
                    endTime = null,
                    endDate = null,
                    priority = Priority.URGENT, // Critical deadlines channel!
                    precision = ReminderPrecision.EXACT,
                    isEnabled = true,
                    isPaused = false,
                    timeZoneId = zoneId,
                    anchorDate = triggerDateTime.toLocalDate(),
                    anchorTime = triggerDateTime.toLocalTime(),
                    lastCompletedAt = null,
                    createdAt = now,
                    updatedAt = now
                )

                scheduleReminderUseCase(reminder, now)
            }
        }
    }

    /**
     * Cancels and deletes all scheduled deadline reminders and occurrences for a task.
     */
    suspend fun cancelDeadlineReminders(taskId: String) {
        val existing = reminderRepository.getDeadlineRemindersForTaskDirect(taskId)
        for (rem in existing) {
            deleteReminderUseCase(rem.id)
        }
    }

    /**
     * Retrieves the set of active deadline reminder lead times configured for a task.
     */
    suspend fun getActiveLeadTimes(taskId: String): Set<DeadlineReminderLeadTime> {
        val reminders = reminderRepository.getDeadlineRemindersForTaskDirect(taskId)
        val prefix = "task_deadline_${taskId}_"
        return reminders.mapNotNull { rem ->
            if (rem.id.startsWith(prefix)) {
                val leadName = rem.id.removePrefix(prefix)
                runCatching { DeadlineReminderLeadTime.valueOf(leadName) }.getOrNull()
            } else {
                null
            }
        }.toSet()
    }

    companion object {
        fun createReminderId(taskId: String, leadTime: DeadlineReminderLeadTime): String {
            return "task_deadline_${taskId}_${leadTime.name}"
        }
    }
}
