package com.dayflow.app.domain.recurrence

import com.dayflow.app.domain.model.OccurrenceStatus
import com.dayflow.app.domain.model.Priority
import com.dayflow.app.domain.model.Reminder
import com.dayflow.app.domain.model.ReminderOccurrence
import com.dayflow.app.domain.model.ReminderPrecision
import com.dayflow.app.domain.model.ReminderType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class ScheduleReconcilerTest {

    private val utcZone = ZoneId.of("UTC")

    private fun sampleReminder(
        id: String = "rem_1",
        type: ReminderType = ReminderType.DAILY,
        startDate: LocalDate = LocalDate.of(2026, 11, 18),
        startTime: LocalTime = LocalTime.of(9, 0),
        isEnabled: Boolean = true,
        isPaused: Boolean = false
    ): Reminder {
        return Reminder(
            id = id,
            title = "Daily Standup",
            type = type,
            startDate = startDate,
            startTime = startTime,
            isEnabled = isEnabled,
            isPaused = isPaused,
            timeZoneId = utcZone,
            priority = Priority.HIGH,
            precision = ReminderPrecision.EXACT
        )
    }

    @Test
    fun reconcile_missingFutureOccurrence_emitsCreateOccurrence() {
        val reminder = sampleReminder()
        val now = ZonedDateTime.of(2026, 11, 18, 7, 0, 0, 0, utcZone).toInstant()

        val actions = ScheduleReconciler.reconcile(
            reminders = listOf(reminder),
            existingOccurrences = emptyList(),
            now = now,
            zoneId = utcZone
        )

        assertEquals(1, actions.size)
        assertTrue(actions[0] is ScheduleAction.CreateOccurrence)
        val createAction = actions[0] as ScheduleAction.CreateOccurrence
        assertEquals(reminder.id, createAction.reminderId)
        assertEquals(ZonedDateTime.of(2026, 11, 18, 9, 0, 0, 0, utcZone).toInstant(), createAction.scheduledAt)
    }

    @Test
    fun reconcile_existingMatchingOccurrence_emitsKeepOccurrence() {
        val reminder = sampleReminder()
        val now = ZonedDateTime.of(2026, 11, 18, 7, 0, 0, 0, utcZone).toInstant()
        val scheduledAt = ZonedDateTime.of(2026, 11, 18, 9, 0, 0, 0, utcZone).toInstant()

        val existing = ReminderOccurrence(
            id = "occ_1",
            reminderId = reminder.id,
            scheduledAt = scheduledAt,
            status = OccurrenceStatus.SCHEDULED
        )

        val actions = ScheduleReconciler.reconcile(
            reminders = listOf(reminder),
            existingOccurrences = listOf(existing),
            now = now,
            zoneId = utcZone
        )

        assertEquals(1, actions.size)
        assertTrue(actions[0] is ScheduleAction.KeepOccurrence)
        val keep = actions[0] as ScheduleAction.KeepOccurrence
        assertEquals("occ_1", keep.occurrenceId)
    }

    @Test
    fun reconcile_disabledOrPausedReminder_emitsCancelOccurrence() {
        val disabledReminder = sampleReminder(id = "rem_disabled", isEnabled = false)
        val pausedReminder = sampleReminder(id = "rem_paused", isPaused = true)
        val now = ZonedDateTime.of(2026, 11, 18, 7, 0, 0, 0, utcZone).toInstant()

        val occ1 = ReminderOccurrence("occ_d", "rem_disabled", now.plusSeconds(3600), OccurrenceStatus.SCHEDULED)
        val occ2 = ReminderOccurrence("occ_p", "rem_paused", now.plusSeconds(3600), OccurrenceStatus.SCHEDULED)

        val actions = ScheduleReconciler.reconcile(
            reminders = listOf(disabledReminder, pausedReminder),
            existingOccurrences = listOf(occ1, occ2),
            now = now,
            zoneId = utcZone
        )

        assertEquals(2, actions.size)
        assertTrue(actions.all { it is ScheduleAction.CancelOccurrence })
    }

    @Test
    fun reconcile_pastUnhandledOccurrence_emitsMarkOccurrenceMissed() {
        val reminder = sampleReminder()
        val now = ZonedDateTime.of(2026, 11, 18, 12, 0, 0, 0, utcZone).toInstant()
        val pastInstant = ZonedDateTime.of(2026, 11, 18, 9, 0, 0, 0, utcZone).toInstant()

        // Occurrence was scheduled at 9:00 AM, but now is 12:00 PM and status is still SCHEDULED
        val pastOccurrence = ReminderOccurrence("occ_past", reminder.id, pastInstant, OccurrenceStatus.SCHEDULED)

        val actions = ScheduleReconciler.reconcile(
            reminders = listOf(reminder),
            existingOccurrences = listOf(pastOccurrence),
            now = now,
            zoneId = utcZone
        )

        // Must mark past occurrence as MISSED and schedule next future occurrence (tomorrow 9:00 AM)
        assertEquals(2, actions.size)
        assertTrue(actions[0] is ScheduleAction.MarkOccurrenceMissed)
        val missedAction = actions[0] as ScheduleAction.MarkOccurrenceMissed
        assertEquals("occ_past", missedAction.occurrenceId)

        assertTrue(actions[1] is ScheduleAction.CreateOccurrence)
        val createAction = actions[1] as ScheduleAction.CreateOccurrence
        assertEquals(ZonedDateTime.of(2026, 11, 19, 9, 0, 0, 0, utcZone).toInstant(), createAction.scheduledAt)
    }

    @Test
    fun reconcile_duplicateFutureOccurrences_keepsOneAndCancelsDuplicates() {
        val reminder = sampleReminder()
        val now = ZonedDateTime.of(2026, 11, 18, 7, 0, 0, 0, utcZone).toInstant()
        val target = ZonedDateTime.of(2026, 11, 18, 9, 0, 0, 0, utcZone).toInstant()

        val occ1 = ReminderOccurrence("occ_dup_1", reminder.id, target, OccurrenceStatus.SCHEDULED)
        val occ2 = ReminderOccurrence("occ_dup_2", reminder.id, target, OccurrenceStatus.SCHEDULED)

        val actions = ScheduleReconciler.reconcile(
            reminders = listOf(reminder),
            existingOccurrences = listOf(occ1, occ2),
            now = now,
            zoneId = utcZone
        )

        assertEquals(2, actions.size)
        val keepAction = actions.filterIsInstance<ScheduleAction.KeepOccurrence>()
        val cancelAction = actions.filterIsInstance<ScheduleAction.CancelOccurrence>()

        assertEquals(1, keepAction.size)
        assertEquals("occ_dup_1", keepAction[0].occurrenceId)
        assertEquals(1, cancelAction.size)
        assertEquals("occ_dup_2", cancelAction[0].occurrenceId)
    }

    @Test
    fun reconcile_orphanedOccurrence_forDeletedReminder_emitsCancelOccurrence() {
        val now = ZonedDateTime.of(2026, 11, 18, 7, 0, 0, 0, utcZone).toInstant()
        val orphaned = ReminderOccurrence("occ_orphan", "deleted_reminder_99", now.plusSeconds(3600), OccurrenceStatus.SCHEDULED)

        val actions = ScheduleReconciler.reconcile(
            reminders = emptyList(), // Reminder list is empty
            existingOccurrences = listOf(orphaned),
            now = now,
            zoneId = utcZone
        )

        assertEquals(1, actions.size)
        assertTrue(actions[0] is ScheduleAction.CancelOccurrence)
        val cancelAction = actions[0] as ScheduleAction.CancelOccurrence
        assertEquals("occ_orphan", cancelAction.occurrenceId)
    }

    @Test
    fun reconcile_idempotency_secondRunProducesOnlyKeepActions() {
        val reminder = sampleReminder()
        val now = ZonedDateTime.of(2026, 11, 18, 7, 0, 0, 0, utcZone).toInstant()

        // 1st run: empty occurrences -> emits CreateOccurrence
        val run1Actions = ScheduleReconciler.reconcile(
            reminders = listOf(reminder),
            existingOccurrences = emptyList(),
            now = now,
            zoneId = utcZone
        )
        val create = run1Actions.filterIsInstance<ScheduleAction.CreateOccurrence>().first()

        // Simulate state persistence of created occurrence
        val persistedOccurrence = ReminderOccurrence(
            id = "occ_new",
            reminderId = create.reminderId,
            scheduledAt = create.scheduledAt,
            status = OccurrenceStatus.SCHEDULED
        )

        // 2nd run: with newly created occurrence
        val run2Actions = ScheduleReconciler.reconcile(
            reminders = listOf(reminder),
            existingOccurrences = listOf(persistedOccurrence),
            now = now,
            zoneId = utcZone
        )

        // Steady state: only KeepOccurrence, zero Create, zero Cancel
        assertEquals(1, run2Actions.size)
        assertTrue(run2Actions[0] is ScheduleAction.KeepOccurrence)
    }
}
