package com.dayflow.app.domain.recurrence

import java.time.DayOfWeek
import java.time.Instant

/**
 * Utility helpers for handling the 7-bit active days mask.
 * Bit 0: MONDAY (value 1)
 * Bit 1: TUESDAY (value 2)
 * Bit 2: WEDNESDAY (value 4)
 * Bit 3: THURSDAY (value 8)
 * Bit 4: FRIDAY (value 16)
 * Bit 5: SATURDAY (value 32)
 * Bit 6: SUNDAY (value 64)
 * All days active = 127.
 */
object ActiveDays {
    const val ALL_DAYS_MASK: Int = 127
    const val WEEKDAYS_MASK: Int = 31 // Mon-Fri (1+2+4+8+16)
    const val WEEKENDS_MASK: Int = 96 // Sat-Sun (32+64)

    /**
     * Checks if a specific [DayOfWeek] is enabled in [mask].
     */
    fun isDayActive(mask: Int, dayOfWeek: DayOfWeek): Boolean {
        val bitIndex = dayOfWeek.value - 1 // MONDAY = 1 -> 0, SUNDAY = 7 -> 6
        return (mask and (1 shl bitIndex)) != 0
    }

    /**
     * Returns the count of enabled weekdays (0..7).
     */
    fun activeDayCount(mask: Int): Int {
        var count = 0
        for (day in DayOfWeek.entries) {
            if (isDayActive(mask, day)) count++
        }
        return count
    }

    /**
     * Creates a bitmask from a set of [DayOfWeek] entries.
     */
    fun createMask(vararg days: DayOfWeek): Int {
        var mask = 0
        for (day in days) {
            mask = mask or (1 shl (day.value - 1))
        }
        return mask
    }
}

/**
 * Result of reconciling overdue or missed reminder occurrences when the application
 * or device resumes after an extended inactive period.
 *
 * Guarantees that at most ONE catch-up notification is delivered immediately,
 * preventing notification bursts / storms. Older occurrences are marked as missed.
 */
data class MissedReconciliation(
    /**
     * Occurrences that occurred in the past and are deemed missed without notification.
     * These should be recorded as [com.dayflow.app.domain.model.OccurrenceStatus.MISSED].
     */
    val missedInstants: List<Instant>,

    /**
     * At most one instant eligible for immediate catch-up delivery.
     * Null if no recent occurrence qualifies for catch-up.
     */
    val catchUpInstant: Instant?,

    /**
     * The next scheduled future occurrence after [now], or null if recurrence is complete.
     */
    val nextScheduledInstant: Instant?
)

/**
 * Sealed hierarchy of pure differential schedule actions produced by [ScheduleReconciler].
 * These actions decouple scheduling evaluation from actual side-effects (e.g. AlarmManager).
 */
sealed interface ScheduleAction {

    /**
     * Instruction to persist and schedule a new occurrence at [scheduledAt].
     */
    data class CreateOccurrence(
        val reminderId: String,
        val scheduledAt: Instant
    ) : ScheduleAction

    /**
     * Instruction to cancel an existing scheduled or snoozed occurrence.
     */
    data class CancelOccurrence(
        val occurrenceId: String,
        val reminderId: String,
        val reason: String
    ) : ScheduleAction

    /**
     * Instruction to transition an unhandled past occurrence to MISSED.
     */
    data class MarkOccurrenceMissed(
        val occurrenceId: String,
        val reminderId: String
    ) : ScheduleAction

    /**
     * Confirmation that an existing scheduled occurrence is correct and should remain active.
     */
    data class KeepOccurrence(
        val occurrenceId: String,
        val reminderId: String,
        val scheduledAt: Instant
    ) : ScheduleAction
}
