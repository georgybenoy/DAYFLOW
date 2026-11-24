package com.dayflow.app.domain.usecase

import com.dayflow.app.domain.model.OccurrenceStatus
import com.dayflow.app.domain.model.ReminderOccurrence
import com.dayflow.app.domain.recurrence.ScheduleAction
import com.dayflow.app.domain.recurrence.ScheduleReconciler
import com.dayflow.app.domain.repository.ReminderRepository
import com.dayflow.app.scheduling.AlarmScheduler
import java.time.Instant
import javax.inject.Inject

/**
 * Idempotent use case to reconcile and reschedule all system alarms.
 * Invoked on device boot, time/timezone changes, exact-alarm permission state changes,
 * and app updates.
 */
class RescheduleAllUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val alarmScheduler: AlarmScheduler
) {
    suspend operator fun invoke(now: Instant = Instant.now()) {
        val activeReminders = reminderRepository.getActiveRemindersDirect()
        val pendingOccurrences = reminderRepository.getPendingOccurrencesDirect()

        val actions = ScheduleReconciler.reconcile(
            reminders = activeReminders,
            existingOccurrences = pendingOccurrences,
            now = now
        )

        for (action in actions) {
            when (action) {
                is ScheduleAction.CreateOccurrence -> {
                    val reminder = reminderRepository.getReminderByIdDirect(action.reminderId)
                    if (reminder != null) {
                        val newOccurrence = ReminderOccurrence(
                            reminderId = action.reminderId,
                            scheduledAt = action.scheduledAt,
                            status = OccurrenceStatus.SCHEDULED
                        )
                        if (reminderRepository.scheduleOccurrence(newOccurrence)) {
                            alarmScheduler.scheduleOccurrence(newOccurrence, reminder)
                        }
                    }
                }

                is ScheduleAction.CancelOccurrence -> {
                    alarmScheduler.cancelOccurrence(action.occurrenceId, action.reminderId)
                    val occ = reminderRepository.getOccurrenceByIdDirect(action.occurrenceId)
                    if (occ != null) {
                        reminderRepository.updateOccurrence(occ.copy(status = OccurrenceStatus.CANCELLED))
                    }
                }

                is ScheduleAction.MarkOccurrenceMissed -> {
                    val occ = reminderRepository.getOccurrenceByIdDirect(action.occurrenceId)
                    if (occ != null) {
                        reminderRepository.updateOccurrence(occ.copy(status = OccurrenceStatus.MISSED))
                    }
                }

                is ScheduleAction.KeepOccurrence -> {
                    // Occurrence is already correctly scheduled and tracked; keep intact
                }
            }
        }
    }
}
