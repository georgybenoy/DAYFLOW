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

class SkipReminderOccurrenceUseCase @Inject constructor(
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
                    status = OccurrenceStatus.SKIPPED,
                    handledAt = now
                )
            )
        }

        if (reminder != null && reminder.type == ReminderType.COMPLETION_INTERVAL && occurrence != null) {
            val nextInstant = RecurrenceCalculator.nextAfterSkip(
                reminder = reminder,
                scheduledAt = occurrence.scheduledAt
            )
            if (nextInstant != null) {
                val newOccurrence = ReminderOccurrence(
                    reminderId = reminder.id,
                    scheduledAt = nextInstant,
                    status = OccurrenceStatus.SCHEDULED
                )
                if (reminderRepository.scheduleOccurrence(newOccurrence)) {
                    alarmScheduler.scheduleOccurrence(newOccurrence, reminder)
                }
            }
        }
        return true
    }
}
