package com.dayflow.app.domain.recurrence

import com.dayflow.app.domain.model.Reminder
import com.dayflow.app.domain.model.ReminderOccurrence
import java.time.Instant
import java.time.ZoneId

interface RecurrenceEngine {
    fun nextOccurrence(reminder: Reminder, after: Instant, zoneId: ZoneId = reminder.timeZoneId): Instant?
    fun nextReminderOccurrence(reminder: Reminder, after: Instant, zoneId: ZoneId = reminder.timeZoneId): ReminderOccurrence?
    fun occurrencesInRange(reminder: Reminder, from: Instant, to: Instant, zoneId: ZoneId = reminder.timeZoneId): List<Instant>
    fun nextAfterCompletion(reminder: Reminder, completedAt: Instant, zoneId: ZoneId = reminder.timeZoneId): Instant?
    fun nextAfterCompletionOccurrence(reminder: Reminder, completedAt: Instant, zoneId: ZoneId = reminder.timeZoneId): ReminderOccurrence?
    fun nextAfterSkip(reminder: Reminder, scheduledAt: Instant, zoneId: ZoneId = reminder.timeZoneId): Instant?
    fun nextAfterSkipOccurrence(reminder: Reminder, scheduledAt: Instant, zoneId: ZoneId = reminder.timeZoneId): ReminderOccurrence?
    fun nextAfterSnooze(reminder: Reminder, snoozedAt: Instant, snoozeDurationMinutes: Long = 10, zoneId: ZoneId = reminder.timeZoneId): ReminderOccurrence
    fun reconcileMissed(reminder: Reminder, now: Instant, lastHandledAt: Instant? = null, catchUpWindowMinutes: Long = 120, zoneId: ZoneId = reminder.timeZoneId): MissedReconciliation
}
