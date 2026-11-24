package com.dayflow.app.domain.recurrence

import com.dayflow.app.domain.model.OccurrenceStatus
import com.dayflow.app.domain.model.Reminder
import com.dayflow.app.domain.model.ReminderOccurrence
import java.time.Instant
import java.time.ZoneId

/**
 * Pure domain schedule reconciler.
 *
 * Given the desired state of reminders, the current persisted occurrences, and current wall-clock [now],
 * computes the exact differential operations ([ScheduleAction]) required to synchronize the schedule.
 *
 * Design Guarantees:
 * - Pure Kotlin with zero Android dependencies.
 * - Idempotency: Multiple reconciliations with unchanged inputs produce identical actions; applying
 *   the actions results in a stable state where subsequent reconciliations emit only [ScheduleAction.KeepOccurrence].
 * - Orphan handling: Occurrences associated with deleted reminders are systematically cancelled.
 * - Duplicate suppression: Only one valid future occurrence per active reminder is preserved.
 * - Past occurrences: Unhandled past occurrences are transitioned to MISSED.
 */
object ScheduleReconciler {

    fun reconcile(
        reminders: List<Reminder>,
        existingOccurrences: List<ReminderOccurrence>,
        now: Instant,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<ScheduleAction> {
        val actions = mutableListOf<ScheduleAction>()

        val reminderMap = reminders.associateBy { it.id }
        val occurrencesByReminder = existingOccurrences.groupBy { it.reminderId }

        // 1. Process all known reminders
        for (reminder in reminders) {
            val occurrences = occurrencesByReminder[reminder.id].orEmpty()
            val activeOccurrences = occurrences.filter {
                it.status == OccurrenceStatus.SCHEDULED || it.status == OccurrenceStatus.SNOOZED
            }

            if (!reminder.isEnabled || reminder.isPaused) {
                // Reminder is disabled or paused -> cancel all pending/snoozed occurrences
                for (occ in activeOccurrences) {
                    actions.add(
                        ScheduleAction.CancelOccurrence(
                            occurrenceId = occ.id,
                            reminderId = reminder.id,
                            reason = "Reminder is ${if (!reminder.isEnabled) "disabled" else "paused"}"
                        )
                    )
                }
                continue
            }

            // Reminder is enabled and active
            // Handle past active occurrences -> Mark missed
            val (pastOccurrences, futureOccurrences) = activeOccurrences.partition {
                !it.scheduledAt.isAfter(now)
            }

            for (past in pastOccurrences) {
                actions.add(
                    ScheduleAction.MarkOccurrenceMissed(
                        occurrenceId = past.id,
                        reminderId = reminder.id
                    )
                )
            }

            // Calculate expected next future occurrence
            val effectiveZone = reminder.timeZoneId ?: zoneId
            val expectedNext = RecurrenceCalculator.nextOccurrence(reminder, after = now, zoneId = effectiveZone)

            if (expectedNext == null) {
                // No future occurrences -> cancel any remaining future occurrences
                for (future in futureOccurrences) {
                    actions.add(
                        ScheduleAction.CancelOccurrence(
                            occurrenceId = future.id,
                            reminderId = reminder.id,
                            reason = "No more future occurrences"
                        )
                    )
                }
            } else {
                // Find matching future occurrence
                val matchingOccurrence = futureOccurrences.firstOrNull { it.scheduledAt == expectedNext }

                if (matchingOccurrence != null) {
                    actions.add(
                        ScheduleAction.KeepOccurrence(
                            occurrenceId = matchingOccurrence.id,
                            reminderId = reminder.id,
                            scheduledAt = matchingOccurrence.scheduledAt
                        )
                    )

                    // Cancel any other future occurrences (duplicates or mismatched times)
                    for (future in futureOccurrences) {
                        if (future.id != matchingOccurrence.id) {
                            actions.add(
                                ScheduleAction.CancelOccurrence(
                                    occurrenceId = future.id,
                                    reminderId = reminder.id,
                                    reason = "Superfluous or duplicate future occurrence"
                                )
                            )
                        }
                    }
                } else {
                    // Outdated future occurrences -> cancel them
                    for (future in futureOccurrences) {
                        actions.add(
                            ScheduleAction.CancelOccurrence(
                                occurrenceId = future.id,
                                reminderId = reminder.id,
                                reason = "Outdated future occurrence replaced by next occurrence"
                            )
                        )
                    }

                    // Create the expected occurrence
                    actions.add(
                        ScheduleAction.CreateOccurrence(
                            reminderId = reminder.id,
                            scheduledAt = expectedNext
                        )
                    )
                }
            }
        }

        // 2. Process orphaned occurrences (reminder was deleted)
        for ((reminderId, occurrences) in occurrencesByReminder) {
            if (!reminderMap.containsKey(reminderId)) {
                for (occ in occurrences) {
                    if (occ.status == OccurrenceStatus.SCHEDULED || occ.status == OccurrenceStatus.SNOOZED) {
                        actions.add(
                            ScheduleAction.CancelOccurrence(
                                occurrenceId = occ.id,
                                reminderId = reminderId,
                                reason = "Associated reminder was deleted"
                            )
                        )
                    }
                }
            }
        }

        return actions
    }
}
