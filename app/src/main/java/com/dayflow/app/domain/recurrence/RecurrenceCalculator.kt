package com.dayflow.app.domain.recurrence

import com.dayflow.app.domain.model.OccurrenceStatus
import com.dayflow.app.domain.model.Reminder
import com.dayflow.app.domain.model.ReminderOccurrence
import com.dayflow.app.domain.model.ReminderType
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

/**
 * Deterministic recurrence calculation engine for DAYFLOW.
 *
 * Design Invariants:
 * 1. Pure Kotlin domain component with ZERO Android dependencies (`android.*`).
 * 2. Deterministic: Given identical inputs ([reminder], [after], [zoneId]), functions
 *    always produce the exact same result.
 * 3. Wall-clock preservation: Alarms are scheduled anchored to local wall-clock time
 *    ([reminder.startTime]), preserving user intent across time zone changes.
 * 4. Safe intervals: All interval calculations enforce a minimum guard of 1 minute
 *    to prevent infinite evaluation loops.
 * 5. Inclusive End Dates: [reminder.endDate] is INCLUSIVE. Occurrences on the local end date
 *    are eligible; occurrences strictly after [reminder.endDate] return null.
 * 6. Skip Policy: Skipping a completion-interval reminder advances recurrence from [scheduledAt]
 *    (the intended slot) rather than [now]. This prevents schedule drift from delayed user interaction.
 * 7. Snooze Non-mutation: Snooze produces a transient [ReminderOccurrence] with status SNOOZED
 *    and NEVER alters the recurrence anchor or original reminder definition.
 * 8. Burst Prevention: [reconcileMissed] evaluates missed slots after extended device inactivity
 *    and produces at most ONE catch-up occurrence, logging older slots as MISSED.
 */
object RecurrenceCalculator : RecurrenceEngine {

    private const val MIN_INTERVAL_MINUTES = 1
    private const val DEFAULT_INTERVAL_MINUTES = 60
    private const val MAX_SEARCH_DAYS = 730 // 2-year horizon

    /**
     * Calculates the next occurrence instant for [reminder] occurring strictly after [after].
     * Returns null if the reminder is disabled, paused, has no active days, or has passed its end date.
     */
    override fun nextOccurrence(
        reminder: Reminder,
        after: Instant,
        zoneId: ZoneId
    ): Instant? {
        if (!reminder.isEnabled || reminder.isPaused) return null
        if (reminder.activeDaysMask == 0) return null

        return when (reminder.type) {
            ReminderType.ONE_TIME -> computeNextOneTime(reminder, after, zoneId)
            ReminderType.DAILY -> computeNextDaily(reminder, after, zoneId)
            ReminderType.WEEKLY -> computeNextWeekly(reminder, after, zoneId)
            ReminderType.CUSTOM_WEEKDAYS -> computeNextCustomWeekdays(reminder, after, zoneId)
            ReminderType.FIXED_INTERVAL -> computeNextFixedInterval(reminder, after, zoneId)
            ReminderType.COMPLETION_INTERVAL -> computeNextCompletionInterval(reminder, after, zoneId)
        }
    }

    /**
     * Convenience method returning a [ReminderOccurrence] model scheduled at [nextOccurrence].
     */
    override fun nextReminderOccurrence(
        reminder: Reminder,
        after: Instant,
        zoneId: ZoneId
    ): ReminderOccurrence? {
        val nextInstant = nextOccurrence(reminder, after, zoneId) ?: return null
        return ReminderOccurrence(
            id = UUID.randomUUID().toString(),
            reminderId = reminder.id,
            scheduledAt = nextInstant,
            status = OccurrenceStatus.SCHEDULED
        )
    }

    /**
     * Generates all occurrence instants within the closed range [[from], [to]].
     * Useful for calendar views, day planning, and multi-day preview projections.
     */
    override fun occurrencesInRange(
        reminder: Reminder,
        from: Instant,
        to: Instant,
        zoneId: ZoneId
    ): List<Instant> {
        if (from.isAfter(to)) return emptyList()
        if (!reminder.isEnabled || reminder.isPaused || reminder.activeDaysMask == 0) return emptyList()

        val results = mutableListOf<Instant>()
        var cursor = from.minusMillis(1)

        while (true) {
            val next = nextOccurrence(reminder, cursor, zoneId) ?: break
            if (next.isAfter(to)) break
            results.add(next)
            cursor = next
        }

        return results
    }

    /**
     * Computes the next occurrence following the user's completion of an occurrence at [completedAt].
     *
     * For [ReminderType.COMPLETION_INTERVAL]:
     * - Next slot is computed as `completedAt + intervalMinutes`.
     * - If the slot falls within [reminder.startTime] and [reminder.endTime], it is scheduled directly.
     * - If the slot falls after [reminder.endTime], it rolls over to [reminder.startTime] on the next active day.
     * - If the slot falls before [reminder.startTime] on that day, it is clamped to [reminder.startTime].
     *
     * For all other recurrence types, delegates to [nextOccurrence] with `after = completedAt`.
     */
    override fun nextAfterCompletion(
        reminder: Reminder,
        completedAt: Instant,
        zoneId: ZoneId
    ): Instant? {
        if (!reminder.isEnabled || reminder.isPaused || reminder.activeDaysMask == 0) return null

        if (reminder.type != ReminderType.COMPLETION_INTERVAL) {
            return nextOccurrence(reminder, completedAt, zoneId)
        }

        val interval = (reminder.intervalMinutes ?: DEFAULT_INTERVAL_MINUTES).coerceAtLeast(MIN_INTERVAL_MINUTES)
        val rawNext = completedAt.plus(Duration.ofMinutes(interval.toLong()))
        return clampOrRolloverCompletionInterval(reminder, rawNext, zoneId)
    }

    override fun nextAfterCompletionOccurrence(
        reminder: Reminder,
        completedAt: Instant,
        zoneId: ZoneId
    ): ReminderOccurrence? {
        val nextInstant = nextAfterCompletion(reminder, completedAt, zoneId) ?: return null
        return ReminderOccurrence(
            id = UUID.randomUUID().toString(),
            reminderId = reminder.id,
            scheduledAt = nextInstant,
            status = OccurrenceStatus.SCHEDULED
        )
    }

    /**
     * Computes the next occurrence when the user SKIPS a reminder.
     *
     * Design Decision:
     * For [ReminderType.COMPLETION_INTERVAL], skipping advances from [scheduledAt] (the planned slot)
     * rather than [now].
     * Rationale: Advancing from [scheduledAt] preserves the user's intended cadence. If a reminder was
     * scheduled for 10:00 with a 2-hour interval, and the user dismisses/skips it at 11:30, advancing from
     * [scheduledAt] yields 12:00 (10:00 + 2h), preventing unintentional schedule slippage.
     */
    override fun nextAfterSkip(
        reminder: Reminder,
        scheduledAt: Instant,
        zoneId: ZoneId
    ): Instant? {
        if (!reminder.isEnabled || reminder.isPaused || reminder.activeDaysMask == 0) return null

        return if (reminder.type == ReminderType.COMPLETION_INTERVAL) {
            nextAfterCompletion(reminder, completedAt = scheduledAt, zoneId = zoneId)
        } else {
            nextOccurrence(reminder, after = scheduledAt, zoneId = zoneId)
        }
    }

    override fun nextAfterSkipOccurrence(
        reminder: Reminder,
        scheduledAt: Instant,
        zoneId: ZoneId
    ): ReminderOccurrence? {
        val nextInstant = nextAfterSkip(reminder, scheduledAt, zoneId) ?: return null
        return ReminderOccurrence(
            id = UUID.randomUUID().toString(),
            reminderId = reminder.id,
            scheduledAt = nextInstant,
            status = OccurrenceStatus.SCHEDULED
        )
    }

    /**
     * Produces a transient [ReminderOccurrence] for a snoozed reminder.
     *
     * INVARIANT: Snoozing NEVER modifies the underlying [Reminder] recurrence anchors,
     * start date, or recurrence configuration. It simply creates an ephemeral occurrence.
     */
    override fun nextAfterSnooze(
        reminder: Reminder,
        snoozedAt: Instant,
        snoozeDurationMinutes: Long,
        zoneId: ZoneId
    ): ReminderOccurrence {
        val safeDuration = snoozeDurationMinutes.coerceAtLeast(1)
        val snoozeTarget = snoozedAt.plus(Duration.ofMinutes(safeDuration))
        return ReminderOccurrence(
            id = UUID.randomUUID().toString(),
            reminderId = reminder.id,
            scheduledAt = snoozeTarget,
            status = OccurrenceStatus.SNOOZED,
            snoozedUntil = snoozeTarget
        )
    }

    /**
     * Reconciles overdue occurrences after device inactivity or reboot.
     *
     * Guarantees:
     * - At most ONE occurrence is returned in [MissedReconciliation.catchUpInstant],
     *   preventing notification bursts / storms.
     * - Occurrences older than [catchUpWindowMinutes] are classified as [MissedReconciliation.missedInstants].
     * - [MissedReconciliation.nextScheduledInstant] returns the future slot after [now].
     */
    override fun reconcileMissed(
        reminder: Reminder,
        now: Instant,
        lastHandledAt: Instant?,
        catchUpWindowMinutes: Long,
        zoneId: ZoneId
    ): MissedReconciliation {
        if (!reminder.isEnabled || reminder.isPaused || reminder.activeDaysMask == 0) {
            return MissedReconciliation(emptyList(), null, null)
        }

        val pastOccurrences = if (lastHandledAt != null) {
            occurrencesInRange(reminder, lastHandledAt.plusMillis(1), now, zoneId)
        } else {
            occurrencesInRange(reminder, reminder.createdAt, now, zoneId)
        }

        val nextFuture = nextOccurrence(reminder, after = now, zoneId = zoneId)

        if (pastOccurrences.isEmpty()) {
            return MissedReconciliation(
                missedInstants = emptyList(),
                catchUpInstant = null,
                nextScheduledInstant = nextFuture
            )
        }

        val latestPast = pastOccurrences.last()
        val durationSinceLatest = Duration.between(latestPast, now)

        val isEligibleForCatchUp = durationSinceLatest.toMinutes() <= catchUpWindowMinutes.coerceAtLeast(1)

        return if (isEligibleForCatchUp) {
            MissedReconciliation(
                missedInstants = pastOccurrences.dropLast(1),
                catchUpInstant = latestPast,
                nextScheduledInstant = nextFuture
            )
        } else {
            MissedReconciliation(
                missedInstants = pastOccurrences,
                catchUpInstant = null,
                nextScheduledInstant = nextFuture
            )
        }
    }

    // =========================================================================
    // Private Recurrence Calculators
    // =========================================================================

    private fun computeNextOneTime(
        reminder: Reminder,
        after: Instant,
        zoneId: ZoneId
    ): Instant? {
        val targetZdt = ZonedDateTime.of(reminder.startDate, reminder.startTime, zoneId)
        val targetInstant = targetZdt.toInstant()

        if (targetInstant.isBefore(after) || targetInstant == after) return null
        if (reminder.endDate != null && reminder.startDate.isAfter(reminder.endDate)) return null

        return targetInstant
    }

    private fun computeNextDaily(
        reminder: Reminder,
        after: Instant,
        zoneId: ZoneId
    ): Instant? {
        var candidateDate = reminder.startDate
        val afterDate = after.atZone(zoneId).toLocalDate()
        if (afterDate.isAfter(candidateDate)) {
            candidateDate = afterDate
        }

        for (i in 0..MAX_SEARCH_DAYS) {
            val date = candidateDate.plusDays(i.toLong())
            if (reminder.endDate != null && date.isAfter(reminder.endDate)) return null

            if (ActiveDays.isDayActive(reminder.activeDaysMask, date.dayOfWeek)) {
                val zdt = ZonedDateTime.of(date, reminder.startTime, zoneId)
                val instant = zdt.toInstant()
                if (instant.isAfter(after)) {
                    return instant
                }
            }
        }
        return null
    }

    private fun computeNextWeekly(
        reminder: Reminder,
        after: Instant,
        zoneId: ZoneId
    ): Instant? {
        val targetDayOfWeek = reminder.startDate.dayOfWeek
        val effectiveMask = if (reminder.activeDaysMask == ActiveDays.ALL_DAYS_MASK) {
            ActiveDays.createMask(targetDayOfWeek)
        } else {
            reminder.activeDaysMask
        }

        var candidateDate = reminder.startDate
        val afterDate = after.atZone(zoneId).toLocalDate()
        if (afterDate.isAfter(candidateDate)) {
            candidateDate = afterDate
        }

        for (i in 0..MAX_SEARCH_DAYS) {
            val date = candidateDate.plusDays(i.toLong())
            if (reminder.endDate != null && date.isAfter(reminder.endDate)) return null

            if (ActiveDays.isDayActive(effectiveMask, date.dayOfWeek)) {
                val zdt = ZonedDateTime.of(date, reminder.startTime, zoneId)
                val instant = zdt.toInstant()
                if (instant.isAfter(after)) {
                    return instant
                }
            }
        }
        return null
    }

    private fun computeNextCustomWeekdays(
        reminder: Reminder,
        after: Instant,
        zoneId: ZoneId
    ): Instant? {
        return computeNextDaily(reminder, after, zoneId)
    }

    private fun computeNextFixedInterval(
        reminder: Reminder,
        after: Instant,
        zoneId: ZoneId
    ): Instant? {
        val intervalMinutes = (reminder.intervalMinutes ?: DEFAULT_INTERVAL_MINUTES).coerceAtLeast(MIN_INTERVAL_MINUTES)
        val startTime = reminder.startTime
        val endTime = reminder.endTime ?: LocalTime.of(23, 59, 59)
        val isMidnightCrossing = endTime.isBefore(startTime)

        var candidateDate = reminder.startDate
        val afterZdt = after.atZone(zoneId)
        val afterDate = afterZdt.toLocalDate()

        // If crossing midnight, a window starting on afterDate - 1 could still be open on afterDate
        val initialDate = if (isMidnightCrossing) {
            val prevDay = afterDate.minusDays(1)
            if (prevDay.isAfter(reminder.startDate)) prevDay else reminder.startDate
        } else {
            if (afterDate.isAfter(reminder.startDate)) afterDate else reminder.startDate
        }
        candidateDate = initialDate

        for (i in 0..MAX_SEARCH_DAYS) {
            val date = candidateDate.plusDays(i.toLong())
            if (reminder.endDate != null && date.isAfter(reminder.endDate)) return null

            if (ActiveDays.isDayActive(reminder.activeDaysMask, date.dayOfWeek)) {
                val windowStart = ZonedDateTime.of(date, startTime, zoneId)
                val windowEnd = if (isMidnightCrossing) {
                    ZonedDateTime.of(date.plusDays(1), endTime, zoneId)
                } else {
                    ZonedDateTime.of(date, endTime, zoneId)
                }

                var slot = windowStart
                while (!slot.isAfter(windowEnd)) {
                    val instant = slot.toInstant()
                    if (instant.isAfter(after)) {
                        return instant
                    }
                    slot = slot.plusMinutes(intervalMinutes.toLong())
                }
            }
        }
        return null
    }

    private fun computeNextCompletionInterval(
        reminder: Reminder,
        after: Instant,
        zoneId: ZoneId
    ): Instant? {
        // If lastCompletedAt exists, next is computed relative to lastCompletedAt
        val lastCompletion = reminder.lastCompletedAt
        if (lastCompletion != null) {
            val interval = (reminder.intervalMinutes ?: DEFAULT_INTERVAL_MINUTES).coerceAtLeast(MIN_INTERVAL_MINUTES)
            val candidate = lastCompletion.plus(Duration.ofMinutes(interval.toLong()))
            if (candidate.isAfter(after)) {
                val clamped = clampOrRolloverCompletionInterval(reminder, candidate, zoneId)
                if (clamped != null && clamped.isAfter(after)) {
                    return clamped
                }
            }
        }

        // If not completed yet or candidate is in the past, schedule at next available window start after `after`
        return computeNextDaily(reminder, after, zoneId)
    }

    private fun clampOrRolloverCompletionInterval(
        reminder: Reminder,
        targetInstant: Instant,
        zoneId: ZoneId
    ): Instant? {
        val zdt = targetInstant.atZone(zoneId)
        val date = zdt.toLocalDate()
        val time = zdt.toLocalTime()

        val startTime = reminder.startTime
        val endTime = reminder.endTime ?: LocalTime.of(23, 59, 59)
        val isMidnightCrossing = endTime.isBefore(startTime)

        if (!isMidnightCrossing) {
            // Standard Window: startTime <= endTime
            if (ActiveDays.isDayActive(reminder.activeDaysMask, date.dayOfWeek)) {
                if (time.isBefore(startTime)) {
                    // Before window opens on active day -> clamp to startTime
                    return resolveCandidateInstant(reminder, ZonedDateTime.of(date, startTime, zoneId))
                }
                if (!time.isAfter(endTime)) {
                    // Inside window
                    return resolveCandidateInstant(reminder, zdt)
                }
            }

            // Outside window or inactive day -> rollover to next active day at startTime
            val nextActiveDate = findNextActiveDate(reminder, date.plusDays(1)) ?: return null
            return resolveCandidateInstant(reminder, ZonedDateTime.of(nextActiveDate, startTime, zoneId))
        } else {
            // Midnight-crossing window: spans (D, startTime) -> (D+1, endTime)
            // 1. Check if zdt falls in the window anchored on (date - 1)
            val prevDate = date.minusDays(1)
            if (ActiveDays.isDayActive(reminder.activeDaysMask, prevDate.dayOfWeek)) {
                val prevWindowStart = ZonedDateTime.of(prevDate, startTime, zoneId)
                val prevWindowEnd = ZonedDateTime.of(date, endTime, zoneId)
                if (!zdt.isBefore(prevWindowStart) && !zdt.isAfter(prevWindowEnd)) {
                    return resolveCandidateInstant(reminder, zdt)
                }
            }

            // 2. Check if zdt falls in the window anchored on date
            if (ActiveDays.isDayActive(reminder.activeDaysMask, date.dayOfWeek)) {
                val currWindowStart = ZonedDateTime.of(date, startTime, zoneId)
                val currWindowEnd = ZonedDateTime.of(date.plusDays(1), endTime, zoneId)
                if (zdt.isBefore(currWindowStart)) {
                    // Before window opens on active date -> clamp to startTime
                    return resolveCandidateInstant(reminder, currWindowStart)
                }
                if (!zdt.isAfter(currWindowEnd)) {
                    return resolveCandidateInstant(reminder, zdt)
                }
            }

            // 3. Otherwise rollover to next active day after date
            val nextActiveDate = findNextActiveDate(reminder, date.plusDays(1)) ?: return null
            return resolveCandidateInstant(reminder, ZonedDateTime.of(nextActiveDate, startTime, zoneId))
        }
    }

    private fun findNextActiveDate(reminder: Reminder, fromDate: LocalDate): LocalDate? {
        var curr = fromDate
        if (curr.isBefore(reminder.startDate)) {
            curr = reminder.startDate
        }

        for (i in 0..MAX_SEARCH_DAYS) {
            val date = curr.plusDays(i.toLong())
            if (reminder.endDate != null && date.isAfter(reminder.endDate)) return null
            if (ActiveDays.isDayActive(reminder.activeDaysMask, date.dayOfWeek)) {
                return date
            }
        }
        return null
    }

    private fun resolveCandidateInstant(reminder: Reminder, zdt: ZonedDateTime): Instant? {
        if (reminder.endDate != null && zdt.toLocalDate().isAfter(reminder.endDate)) {
            return null
        }
        return zdt.toInstant()
    }
}
