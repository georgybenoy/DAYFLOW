package com.dayflow.app.domain.usecase

import com.dayflow.app.domain.model.OccurrenceStatus
import com.dayflow.app.domain.model.Reminder
import com.dayflow.app.domain.model.ReminderOccurrence
import com.dayflow.app.domain.recurrence.RecurrenceCalculator
import com.dayflow.app.domain.repository.ReminderRepository
import com.dayflow.app.scheduling.AlarmScheduler
import java.time.Instant
import javax.inject.Inject

class ScheduleReminderUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val alarmScheduler: AlarmScheduler
) {
    suspend operator fun invoke(reminder: Reminder, now: Instant = Instant.now()): Boolean {
        // 1. Persist the updated reminder definition
        reminderRepository.saveReminder(reminder)

        // 2. If disabled or paused, cancel any scheduled occurrences
        if (!reminder.isEnabled || reminder.isPaused) {
            val pendingOccurrences = reminderRepository.getPendingOccurrencesDirect()
                .filter { it.reminderId == reminder.id }
            for (occ in pendingOccurrences) {
                alarmScheduler.cancelOccurrence(occ.id, reminder.id)
                reminderRepository.updateOccurrence(occ.copy(status = OccurrenceStatus.CANCELLED))
            }
            return true
        }

        // 3. Compute the next occurrence
        val nextInstant = RecurrenceCalculator.nextOccurrence(
            reminder = reminder,
            after = now,
            zoneId = reminder.timeZoneId
        ) ?: return true

        // 4. Create and persist occurrence BEFORE/transactionally with alarm scheduling
        val occurrence = ReminderOccurrence(
            reminderId = reminder.id,
            scheduledAt = nextInstant,
            status = OccurrenceStatus.SCHEDULED
        )

        val inserted = reminderRepository.scheduleOccurrence(occurrence)
        return if (inserted) {
            alarmScheduler.scheduleOccurrence(occurrence, reminder)
        } else {
            false
        }
    }
}
