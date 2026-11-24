package com.dayflow.app.domain.usecase

import com.dayflow.app.domain.recurrence.RecurrenceCalculator
import com.dayflow.app.domain.repository.ReminderRepository
import com.dayflow.app.domain.repository.SettingsRepository
import com.dayflow.app.notifications.NotificationDispatcher
import com.dayflow.app.scheduling.AlarmScheduler
import java.time.Instant
import javax.inject.Inject

class SnoozeReminderOccurrenceUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val alarmScheduler: AlarmScheduler,
    private val notificationDispatcher: NotificationDispatcher,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(
        occurrenceId: String,
        reminderId: String,
        customSnoozeMinutes: Long? = null,
        now: Instant = Instant.now()
    ): Boolean {
        notificationDispatcher.cancelReminderNotification(occurrenceId)
        val occurrence = reminderRepository.getOccurrenceByIdDirect(occurrenceId) ?: return false
        val reminder = reminderRepository.getReminderByIdDirect(reminderId) ?: return false

        val snoozeMinutes = customSnoozeMinutes ?: 10L
        val snoozeOccurrence = RecurrenceCalculator.nextAfterSnooze(
            reminder = reminder,
            snoozedAt = now,
            snoozeDurationMinutes = snoozeMinutes
        )

        val inserted = reminderRepository.scheduleOccurrence(snoozeOccurrence)
        if (inserted) {
            alarmScheduler.scheduleOccurrence(snoozeOccurrence, reminder)
        }

        reminderRepository.updateOccurrence(occurrence.copy(handledAt = now))
        return true
    }
}
