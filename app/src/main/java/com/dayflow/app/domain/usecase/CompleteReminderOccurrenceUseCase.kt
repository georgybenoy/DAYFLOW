package com.dayflow.app.domain.usecase

import com.dayflow.app.domain.model.OccurrenceStatus
import com.dayflow.app.domain.model.ReminderOccurrence
import com.dayflow.app.domain.model.ReminderType
import com.dayflow.app.domain.recurrence.RecurrenceCalculator
import com.dayflow.app.domain.repository.ReminderRepository
import com.dayflow.app.notifications.NotificationDispatcher
import com.dayflow.app.scheduling.AlarmScheduler
import java.time.Instant
import javax.inject.Inject

class CompleteReminderOccurrenceUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val alarmScheduler: AlarmScheduler,
    private val notificationDispatcher: NotificationDispatcher
) {
    suspend operator fun invoke(occurrenceId: String, reminderId: String, now: Instant = Instant.now()): Boolean {
        notificationDispatcher.cancelReminderNotification(occurrenceId)
        val occurrence = reminderRepository.getOccurrenceByIdDirect(occurrenceId)
        val reminder = reminderRepository.getReminderByIdDirect(reminderId)

        if (occurrence != null) {
            reminderRepository.updateOccurrence(
                occurrence.copy(
                    status = OccurrenceStatus.COMPLETED,
                    handledAt = now
                )
            )
        }

        if (reminder != null) {
            val updatedReminder = reminder.copy(lastCompletedAt = now)
            reminderRepository.saveReminder(updatedReminder)

            if (reminder.type == ReminderType.COMPLETION_INTERVAL) {
                val nextInstant = RecurrenceCalculator.nextAfterCompletion(
                    reminder = updatedReminder,
                    completedAt = now
                )
                if (nextInstant != null) {
                    val newOccurrence = ReminderOccurrence(
                        reminderId = reminder.id,
                        scheduledAt = nextInstant,
                        status = OccurrenceStatus.SCHEDULED
                    )
                    if (reminderRepository.scheduleOccurrence(newOccurrence)) {
                        alarmScheduler.scheduleOccurrence(newOccurrence, updatedReminder)
                    }
                }
            }
            return true
        }
        return false
    }
}
