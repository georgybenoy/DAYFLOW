package com.dayflow.app.domain.recurrence

import com.dayflow.app.domain.model.OccurrenceStatus
import com.dayflow.app.domain.model.Priority
import com.dayflow.app.domain.model.Reminder
import com.dayflow.app.domain.model.ReminderPrecision
import com.dayflow.app.domain.model.ReminderType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class RecurrenceCalculatorTest {

    private val utcZone = ZoneId.of("UTC")
    private val newYorkZone = ZoneId.of("America/New_York")
    private val londonZone = ZoneId.of("Europe/London")
    private val kolkataZone = ZoneId.of("Asia/Kolkata")

    private fun sampleReminder(
        type: ReminderType = ReminderType.DAILY,
        startDate: LocalDate = LocalDate.of(2026, 11, 18),
        startTime: LocalTime = LocalTime.of(9, 0),
        endTime: LocalTime? = null,
        endDate: LocalDate? = null,
        intervalMinutes: Int? = null,
        activeDaysMask: Int = ActiveDays.ALL_DAYS_MASK,
        zoneId: ZoneId = utcZone,
        isEnabled: Boolean = true,
        isPaused: Boolean = false,
        lastCompletedAt: Instant? = null
    ): Reminder {
        return Reminder(
            id = "rem_test",
            title = "Test Reminder",
            type = type,
            startDate = startDate,
            startTime = startTime,
            endTime = endTime,
            endDate = endDate,
            intervalMinutes = intervalMinutes,
            activeDaysMask = activeDaysMask,
            timeZoneId = zoneId,
            isEnabled = isEnabled,
            isPaused = isPaused,
            lastCompletedAt = lastCompletedAt,
            priority = Priority.MEDIUM,
            precision = ReminderPrecision.EXACT
        )
    }

    // =========================================================================
    // 1. Basic Recurrence Types
    // =========================================================================

    @Test
    fun oneTimeReminder_firesOnce_andReturnsNullAfter() {
        val reminder = sampleReminder(
            type = ReminderType.ONE_TIME,
            startDate = LocalDate.of(2026, 11, 20),
            startTime = LocalTime.of(15, 30)
        )
        val expectedInstant = ZonedDateTime.of(2026, 11, 20, 15, 30, 0, 0, utcZone).toInstant()

        val before = expectedInstant.minusSeconds(3600)
        val next = RecurrenceCalculator.nextOccurrence(reminder, after = before)
        assertEquals(expectedInstant, next)

        // After the occurrence has passed -> null
        val after = expectedInstant.plusSeconds(60)
        assertNull(RecurrenceCalculator.nextOccurrence(reminder, after = after))
    }

    @Test
    fun dailyReminder_advancesDayByDay() {
        val reminder = sampleReminder(
            type = ReminderType.DAILY,
            startDate = LocalDate.of(2026, 11, 18),
            startTime = LocalTime.of(8, 0)
        )

        val after1 = ZonedDateTime.of(2026, 11, 18, 7, 0, 0, 0, utcZone).toInstant()
        val next1 = RecurrenceCalculator.nextOccurrence(reminder, after = after1)
        assertEquals(ZonedDateTime.of(2026, 11, 18, 8, 0, 0, 0, utcZone).toInstant(), next1)

        // After today's 8:00 AM -> next is tomorrow 8:00 AM
        val after2 = ZonedDateTime.of(2026, 11, 18, 8, 30, 0, 0, utcZone).toInstant()
        val next2 = RecurrenceCalculator.nextOccurrence(reminder, after = after2)
        assertEquals(ZonedDateTime.of(2026, 11, 19, 8, 0, 0, 0, utcZone).toInstant(), next2)
    }

    @Test
    fun weeklyReminder_advancesBy7Days() {
        // Nov 18, 2026 is a Wednesday
        val reminder = sampleReminder(
            type = ReminderType.WEEKLY,
            startDate = LocalDate.of(2026, 11, 18),
            startTime = LocalTime.of(10, 0)
        )

        val after = ZonedDateTime.of(2026, 11, 18, 11, 0, 0, 0, utcZone).toInstant()
        val next = RecurrenceCalculator.nextOccurrence(reminder, after = after)
        // Next Wednesday is Nov 25, 2026
        assertEquals(ZonedDateTime.of(2026, 11, 25, 10, 0, 0, 0, utcZone).toInstant(), next)
    }

    @Test
    fun customWeekdays_skipsDisabledWeekdays() {
        // Monday (1) and Thursday (8) active -> mask = 9
        // Nov 16, 2026 is Mon, Nov 17 Tue, Nov 18 Wed, Nov 19 Thu, Nov 20 Fri
        val reminder = sampleReminder(
            type = ReminderType.CUSTOM_WEEKDAYS,
            startDate = LocalDate.of(2026, 11, 16),
            startTime = LocalTime.of(9, 0),
            activeDaysMask = ActiveDays.createMask(DayOfWeek.MONDAY, DayOfWeek.THURSDAY)
        )

        // After Monday 9:00 AM -> Next must skip Tuesday & Wednesday and fire Thursday Nov 19
        val afterMonday = ZonedDateTime.of(2026, 11, 16, 9, 30, 0, 0, utcZone).toInstant()
        val next = RecurrenceCalculator.nextOccurrence(reminder, after = afterMonday)
        assertEquals(ZonedDateTime.of(2026, 11, 19, 9, 0, 0, 0, utcZone).toInstant(), next)
    }

    // =========================================================================
    // 2. FIXED_INTERVAL & Water Example
    // =========================================================================

    @Test
    fun fixedInterval_waterExample_yieldsExact8OccurrencesFrom8To22() {
        // Water reminder: every 2 hours between 8:00 and 22:00
        val reminder = sampleReminder(
            type = ReminderType.FIXED_INTERVAL,
            startDate = LocalDate.of(2026, 11, 18),
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(22, 0),
            intervalMinutes = 120
        )

        val dayStart = ZonedDateTime.of(2026, 11, 18, 0, 0, 0, 0, utcZone).toInstant()
        val dayEnd = ZonedDateTime.of(2026, 11, 18, 23, 59, 59, 0, utcZone).toInstant()

        val occurrences = RecurrenceCalculator.occurrencesInRange(reminder, dayStart, dayEnd)
        assertEquals(8, occurrences.size)

        val expectedHours = listOf(8, 10, 12, 14, 16, 18, 20, 22)
        occurrences.forEachIndexed { index, instant ->
            val zdt = instant.atZone(utcZone)
            assertEquals(expectedHours[index], zdt.hour)
            assertEquals(0, zdt.minute)
        }

        // After 22:00, next occurrence rolls over to next day at 08:00
        val after10pm = ZonedDateTime.of(2026, 11, 18, 22, 0, 0, 0, utcZone).toInstant()
        val nextDayOccurrence = RecurrenceCalculator.nextOccurrence(reminder, after = after10pm)
        assertEquals(ZonedDateTime.of(2026, 11, 19, 8, 0, 0, 0, utcZone).toInstant(), nextDayOccurrence)
    }

    @Test
    fun fixedInterval_midnightCrossingWindow() {
        // Window from 22:00 to 04:00 (next day) with 120m interval
        val reminder = sampleReminder(
            type = ReminderType.FIXED_INTERVAL,
            startDate = LocalDate.of(2026, 11, 18),
            startTime = LocalTime.of(22, 0),
            endTime = LocalTime.of(4, 0),
            intervalMinutes = 120
        )

        // Slot 1: Nov 18 22:00
        val afterStart = ZonedDateTime.of(2026, 11, 18, 21, 0, 0, 0, utcZone).toInstant()
        val next1 = RecurrenceCalculator.nextOccurrence(reminder, after = afterStart)
        assertEquals(ZonedDateTime.of(2026, 11, 18, 22, 0, 0, 0, utcZone).toInstant(), next1)

        // Slot 2: Nov 19 00:00 (Midnight crossed)
        val next2 = RecurrenceCalculator.nextOccurrence(reminder, after = next1!!)
        assertEquals(ZonedDateTime.of(2026, 11, 19, 0, 0, 0, 0, utcZone).toInstant(), next2)

        // Slot 3: Nov 19 02:00
        val next3 = RecurrenceCalculator.nextOccurrence(reminder, after = next2!!)
        assertEquals(ZonedDateTime.of(2026, 11, 19, 2, 0, 0, 0, utcZone).toInstant(), next3)

        // Slot 4: Nov 19 04:00
        val next4 = RecurrenceCalculator.nextOccurrence(reminder, after = next3!!)
        assertEquals(ZonedDateTime.of(2026, 11, 19, 4, 0, 0, 0, utcZone).toInstant(), next4)

        // Window ended at 04:00. Next occurrence should be next evening Nov 19 at 22:00
        val next5 = RecurrenceCalculator.nextOccurrence(reminder, after = next4!!)
        assertEquals(ZonedDateTime.of(2026, 11, 19, 22, 0, 0, 0, utcZone).toInstant(), next5)
    }

    // =========================================================================
    // 3. COMPLETION_INTERVAL & Skip Policy
    // =========================================================================

    @Test
    fun completionInterval_normalWindow_andRollover() {
        val reminder = sampleReminder(
            type = ReminderType.COMPLETION_INTERVAL,
            startDate = LocalDate.of(2026, 11, 18),
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(20, 0),
            intervalMinutes = 60
        )

        // 10:00 occurrence done at 10:15 -> next is 11:15
        val completedAt = ZonedDateTime.of(2026, 11, 18, 10, 15, 0, 0, utcZone).toInstant()
        val next = RecurrenceCalculator.nextAfterCompletion(reminder, completedAt = completedAt)
        assertEquals(ZonedDateTime.of(2026, 11, 18, 11, 15, 0, 0, utcZone).toInstant(), next)

        // Completion near end of window: done at 19:30 + 60m = 20:30 (outside 20:00 window)
        // Must roll over to next active day at startTime (08:00)
        val lateCompletion = ZonedDateTime.of(2026, 11, 18, 19, 30, 0, 0, utcZone).toInstant()
        val rolloverNext = RecurrenceCalculator.nextAfterCompletion(reminder, completedAt = lateCompletion)
        assertEquals(ZonedDateTime.of(2026, 11, 19, 8, 0, 0, 0, utcZone).toInstant(), rolloverNext)
    }

    @Test
    fun completionInterval_skipPolicy_advancesFromScheduledTimeNotNow() {
        val reminder = sampleReminder(
            type = ReminderType.COMPLETION_INTERVAL,
            startDate = LocalDate.of(2026, 11, 18),
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(20, 0),
            intervalMinutes = 120
        )

        // Occurrence scheduled at 10:00, user skips at 11:30
        val scheduledAt = ZonedDateTime.of(2026, 11, 18, 10, 0, 0, 0, utcZone).toInstant()
        val next = RecurrenceCalculator.nextAfterSkip(reminder, scheduledAt = scheduledAt)

        // Next must be 10:00 + 120m = 12:00, preserving cadence anchor
        assertEquals(ZonedDateTime.of(2026, 11, 18, 12, 0, 0, 0, utcZone).toInstant(), next)
    }

    @Test
    fun snooze_producesTransientOccurrence_neverMutatesReminderAnchor() {
        val reminder = sampleReminder(
            type = ReminderType.DAILY,
            startDate = LocalDate.of(2026, 11, 18),
            startTime = LocalTime.of(9, 0)
        )

        val snoozedAt = ZonedDateTime.of(2026, 11, 18, 9, 5, 0, 0, utcZone).toInstant()
        val snoozeOccurrence = RecurrenceCalculator.nextAfterSnooze(
            reminder = reminder,
            snoozedAt = snoozedAt,
            snoozeDurationMinutes = 15
        )

        assertEquals(OccurrenceStatus.SNOOZED, snoozeOccurrence.status)
        assertEquals(snoozedAt.plus(Duration.ofMinutes(15)), snoozeOccurrence.scheduledAt)
        assertEquals(snoozedAt.plus(Duration.ofMinutes(15)), snoozeOccurrence.snoozedUntil)

        // Verify reminder definition is unchanged
        assertEquals(LocalDate.of(2026, 11, 18), reminder.startDate)
        assertEquals(LocalTime.of(9, 0), reminder.startTime)
    }

    // =========================================================================
    // 4. Calendar Boundaries: Month Ends & Feb 29 Leap Years
    // =========================================================================

    @Test
    fun monthEnd_transitionsCleanly() {
        // Daily reminder starting Jan 31
        val reminder = sampleReminder(
            type = ReminderType.DAILY,
            startDate = LocalDate.of(2026, 1, 31),
            startTime = LocalTime.of(9, 0)
        )

        val afterJan31 = ZonedDateTime.of(2026, 1, 31, 10, 0, 0, 0, utcZone).toInstant()
        val next = RecurrenceCalculator.nextOccurrence(reminder, after = afterJan31)
        assertEquals(ZonedDateTime.of(2026, 2, 1, 9, 0, 0, 0, utcZone).toInstant(), next)
    }

    @Test
    fun leapYear_handlesFeb29_inLeapYearAndNonLeapYear() {
        // 2024 was a leap year
        val leapYearReminder = sampleReminder(
            type = ReminderType.DAILY,
            startDate = LocalDate.of(2024, 2, 28),
            startTime = LocalTime.of(12, 0)
        )

        val afterFeb28Leap = ZonedDateTime.of(2024, 2, 28, 13, 0, 0, 0, utcZone).toInstant()
        val nextLeap = RecurrenceCalculator.nextOccurrence(leapYearReminder, after = afterFeb28Leap)
        // Must hit Feb 29 in 2024
        assertEquals(ZonedDateTime.of(2024, 2, 29, 12, 0, 0, 0, utcZone).toInstant(), nextLeap)

        // In 2025 (non-leap), Feb 28 -> Mar 1
        val nonLeapReminder = sampleReminder(
            type = ReminderType.DAILY,
            startDate = LocalDate.of(2025, 2, 28),
            startTime = LocalTime.of(12, 0)
        )
        val afterFeb28NonLeap = ZonedDateTime.of(2025, 2, 28, 13, 0, 0, 0, utcZone).toInstant()
        val nextNonLeap = RecurrenceCalculator.nextOccurrence(nonLeapReminder, after = afterFeb28NonLeap)
        assertEquals(ZonedDateTime.of(2025, 3, 1, 12, 0, 0, 0, utcZone).toInstant(), nextNonLeap)
    }

    // =========================================================================
    // 5. Timezone & DST (America/New_York, Europe/London, Asia/Kolkata)
    // =========================================================================

    @Test
    fun timezoneChange_preservesLocalWallClock_recomputesInstants() {
        // 9:00 AM local reminder
        val reminder = sampleReminder(
            type = ReminderType.DAILY,
            startDate = LocalDate.of(2026, 11, 18),
            startTime = LocalTime.of(9, 0)
        )

        val after = ZonedDateTime.of(2026, 11, 18, 0, 0, 0, 0, utcZone).toInstant()

        val instantNY = RecurrenceCalculator.nextOccurrence(reminder, after, newYorkZone)
        val instantLondon = RecurrenceCalculator.nextOccurrence(reminder, after, londonZone)
        val instantKolkata = RecurrenceCalculator.nextOccurrence(reminder, after, kolkataZone)

        assertNotNull(instantNY)
        assertNotNull(instantLondon)
        assertNotNull(instantKolkata)

        // All must resolve to 9:00 AM in their respective local time zones
        assertEquals(LocalTime.of(9, 0), instantNY!!.atZone(newYorkZone).toLocalTime())
        assertEquals(LocalTime.of(9, 0), instantLondon!!.atZone(londonZone).toLocalTime())
        assertEquals(LocalTime.of(9, 0), instantKolkata!!.atZone(kolkataZone).toLocalTime())

        // But their UTC epoch millis must differ due to timezone offsets
        assertTrue(instantKolkata.isBefore(instantLondon))
        assertTrue(instantLondon.isBefore(instantNY))
    }

    @Test
    fun dstSpringForwardGap_inNewYork_adjustsSafely() {
        // America/New_York DST spring-forward: March 8, 2026 at 02:00 -> clocks jump to 03:00
        // A reminder scheduled at 02:30 AM lands in the gap
        val reminder = sampleReminder(
            type = ReminderType.DAILY,
            startDate = LocalDate.of(2026, 3, 8),
            startTime = LocalTime.of(2, 30),
            zoneId = newYorkZone
        )

        val after = ZonedDateTime.of(2026, 3, 8, 1, 0, 0, 0, newYorkZone).toInstant()
        val next = RecurrenceCalculator.nextOccurrence(reminder, after = after, zoneId = newYorkZone)

        assertNotNull(next)
        val nextZdt = next!!.atZone(newYorkZone)
        // In standard Java ZonedDateTime, a gap time jumps to 03:30 EDT
        assertEquals(LocalDate.of(2026, 3, 8), nextZdt.toLocalDate())
        assertEquals(LocalTime.of(3, 30), nextZdt.toLocalTime())
    }

    @Test
    fun dstSpringForwardGap_inLondon_adjustsSafely() {
        // Europe/London DST spring-forward: March 29, 2026 at 01:00 -> clocks jump to 02:00
        // Reminder scheduled at 01:30 AM
        val reminder = sampleReminder(
            type = ReminderType.DAILY,
            startDate = LocalDate.of(2026, 3, 29),
            startTime = LocalTime.of(1, 30),
            zoneId = londonZone
        )

        val after = ZonedDateTime.of(2026, 3, 29, 0, 0, 0, 0, londonZone).toInstant()
        val next = RecurrenceCalculator.nextOccurrence(reminder, after = after, zoneId = londonZone)

        assertNotNull(next)
        val nextZdt = next!!.atZone(londonZone)
        assertEquals(LocalDate.of(2026, 3, 29), nextZdt.toLocalDate())
        assertEquals(LocalTime.of(2, 30), nextZdt.toLocalTime())
    }

    @Test
    fun dstFallBackOverlap_inNewYork_resolvesToValidInstant() {
        // America/New_York DST fall-back: November 1, 2026 at 02:00 -> clocks fall back to 01:00
        // Reminder scheduled at 01:30 AM (occurs twice)
        val reminder = sampleReminder(
            type = ReminderType.DAILY,
            startDate = LocalDate.of(2026, 11, 1),
            startTime = LocalTime.of(1, 30),
            zoneId = newYorkZone
        )

        val after = ZonedDateTime.of(2026, 11, 1, 0, 0, 0, 0, newYorkZone).toInstant()
        val next = RecurrenceCalculator.nextOccurrence(reminder, after = after, zoneId = newYorkZone)

        assertNotNull(next)
        val nextZdt = next!!.atZone(newYorkZone)
        assertEquals(LocalDate.of(2026, 11, 1), nextZdt.toLocalDate())
        assertEquals(LocalTime.of(1, 30), nextZdt.toLocalTime())
    }

    @Test
    fun asiaKolkata_hasNoDst_constantOffset() {
        val reminder = sampleReminder(
            type = ReminderType.DAILY,
            startDate = LocalDate.of(2026, 6, 15),
            startTime = LocalTime.of(7, 0),
            zoneId = kolkataZone
        )

        val after = ZonedDateTime.of(2026, 6, 15, 0, 0, 0, 0, kolkataZone).toInstant()
        val next = RecurrenceCalculator.nextOccurrence(reminder, after, kolkataZone)

        assertNotNull(next)
        val zdt = next!!.atZone(kolkataZone)
        assertEquals(LocalTime.of(7, 0), zdt.toLocalTime())
        assertEquals(Duration.ofHours(5).plusMinutes(30), zdt.offset.totalSeconds.let { Duration.ofSeconds(it.toLong()) })
    }

    // =========================================================================
    // 6. Boundary Rules: End Date, Created After Window, Disabled Days, Interval Guard
    // =========================================================================

    @Test
    fun endDate_isInclusive_occurrencesOnEndDateAllowed_afterEndDateRejected() {
        val reminder = sampleReminder(
            type = ReminderType.DAILY,
            startDate = LocalDate.of(2026, 11, 18),
            startTime = LocalTime.of(9, 0),
            endDate = LocalDate.of(2026, 11, 20) // Inclusive end date
        )

        // Nov 18: OK
        val next18 = RecurrenceCalculator.nextOccurrence(reminder, ZonedDateTime.of(2026, 11, 18, 0, 0, 0, 0, utcZone).toInstant())
        assertEquals(ZonedDateTime.of(2026, 11, 18, 9, 0, 0, 0, utcZone).toInstant(), next18)

        // Nov 20: OK (On end date)
        val next20 = RecurrenceCalculator.nextOccurrence(reminder, ZonedDateTime.of(2026, 11, 19, 12, 0, 0, 0, utcZone).toInstant())
        assertEquals(ZonedDateTime.of(2026, 11, 20, 9, 0, 0, 0, utcZone).toInstant(), next20)

        // After Nov 20 9:00 AM: Returns null because Nov 21 > endDate
        val next21 = RecurrenceCalculator.nextOccurrence(reminder, ZonedDateTime.of(2026, 11, 20, 10, 0, 0, 0, utcZone).toInstant())
        assertNull(next21)
    }

    @Test
    fun reminderCreatedAfterWindow_schedulesNextActiveDayStartTime() {
        val reminder = sampleReminder(
            type = ReminderType.FIXED_INTERVAL,
            startDate = LocalDate.of(2026, 11, 18),
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(17, 0),
            intervalMinutes = 60
        )

        // User opens / creates at 18:30 (after the 17:00 window has closed)
        val after = ZonedDateTime.of(2026, 11, 18, 18, 30, 0, 0, utcZone).toInstant()
        val next = RecurrenceCalculator.nextOccurrence(reminder, after = after)

        // Next must be tomorrow at 09:00
        assertEquals(ZonedDateTime.of(2026, 11, 19, 9, 0, 0, 0, utcZone).toInstant(), next)
    }

    @Test
    fun allWeekdaysDisabled_returnsNullSafely() {
        val reminder = sampleReminder(
            type = ReminderType.CUSTOM_WEEKDAYS,
            activeDaysMask = 0 // Zero weekdays active
        )

        val next = RecurrenceCalculator.nextOccurrence(reminder, Instant.now())
        assertNull(next)
    }

    @Test
    fun intervalMinimumGuard_preventsInfiniteLoop() {
        val reminderZeroInterval = sampleReminder(
            type = ReminderType.FIXED_INTERVAL,
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(10, 0),
            intervalMinutes = 0 // Zero interval should clamp to 1 minute
        )

        val after = ZonedDateTime.of(2026, 11, 18, 9, 0, 0, 0, utcZone).toInstant()
        val next = RecurrenceCalculator.nextOccurrence(reminderZeroInterval, after = after)
        assertNotNull(next)
        // 9:00 + 1 min guard = 9:01
        assertEquals(ZonedDateTime.of(2026, 11, 18, 9, 1, 0, 0, utcZone).toInstant(), next)
    }

    // =========================================================================
    // 7. Long Device-Off Gap & reconcileMissed (Zero Notification Bursts)
    // =========================================================================

    @Test
    fun reconcileMissed_longDeviceOffGap_producesZeroBurst_andAtMostOneCatchUp() {
        // Daily reminder at 9:00 AM
        val reminder = sampleReminder(
            type = ReminderType.DAILY,
            startDate = LocalDate.of(2026, 11, 1),
            startTime = LocalTime.of(9, 0)
        )

        // Device was off for 14 days, turned on Nov 15 at 10:00 AM
        val now = ZonedDateTime.of(2026, 11, 15, 10, 0, 0, 0, utcZone).toInstant()
        val lastDelivered = ZonedDateTime.of(2026, 11, 1, 9, 0, 0, 0, utcZone).toInstant()

        val reconciliation = RecurrenceCalculator.reconcileMissed(
            reminder = reminder,
            now = now,
            lastHandledAt = lastDelivered,
            catchUpWindowMinutes = 120 // 2-hour catch-up window
        )

        // Today's occurrence was at 9:00 AM (1 hour ago, inside 120-min catch-up window)
        // It qualifies for immediate catch-up
        assertNotNull(reconciliation.catchUpInstant)
        assertEquals(ZonedDateTime.of(2026, 11, 15, 9, 0, 0, 0, utcZone).toInstant(), reconciliation.catchUpInstant)

        // All intermediate 13 occurrences (Nov 2..14) must be recorded as MISSED (no notification burst)
        assertEquals(13, reconciliation.missedInstants.size)

        // Next scheduled is tomorrow Nov 16 at 09:00 AM
        assertEquals(ZonedDateTime.of(2026, 11, 16, 9, 0, 0, 0, utcZone).toInstant(), reconciliation.nextScheduledInstant)
    }

    @Test
    fun reconcileMissed_whenLatestPastIsTooOld_producesNoCatchUp() {
        // Device turned on at 18:00 (9 hours after 09:00 occurrence, outside 120-min catch-up window)
        val reminder = sampleReminder(
            type = ReminderType.DAILY,
            startDate = LocalDate.of(2026, 11, 1),
            startTime = LocalTime.of(9, 0)
        )

        val now = ZonedDateTime.of(2026, 11, 15, 18, 0, 0, 0, utcZone).toInstant()
        val lastDelivered = ZonedDateTime.of(2026, 11, 1, 9, 0, 0, 0, utcZone).toInstant()

        val reconciliation = RecurrenceCalculator.reconcileMissed(
            reminder = reminder,
            now = now,
            lastHandledAt = lastDelivered,
            catchUpWindowMinutes = 120
        )

        // No catch-up because latest is 9 hours stale
        assertNull(reconciliation.catchUpInstant)
        // All 14 past occurrences logged as missed
        assertEquals(14, reconciliation.missedInstants.size)
        // Next scheduled is tomorrow Nov 16 at 09:00 AM
        assertEquals(ZonedDateTime.of(2026, 11, 16, 9, 0, 0, 0, utcZone).toInstant(), reconciliation.nextScheduledInstant)
    }

    // =========================================================================
    // 8. Property-Style Invariant Tests
    // =========================================================================

    @Test
    fun propertyTest_resultIsAlwaysAfterGivenInstant() {
        val reminder = sampleReminder(
            type = ReminderType.FIXED_INTERVAL,
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(22, 0),
            intervalMinutes = 45
        )

        var after = ZonedDateTime.of(2026, 11, 18, 6, 0, 0, 0, utcZone).toInstant()
        for (i in 1..50) {
            val next = RecurrenceCalculator.nextOccurrence(reminder, after = after)
            assertNotNull("Occurrence $i should not be null", next)
            assertTrue("Next ($next) must be strictly after ($after)", next!!.isAfter(after))
            after = next
        }
    }

    @Test
    fun propertyTest_resultIsAlwaysInsideActiveDayWindow() {
        val reminder = sampleReminder(
            type = ReminderType.FIXED_INTERVAL,
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(20, 0),
            intervalMinutes = 90
        )

        val from = ZonedDateTime.of(2026, 11, 18, 0, 0, 0, 0, utcZone).toInstant()
        val to = ZonedDateTime.of(2026, 11, 25, 0, 0, 0, 0, utcZone).toInstant()

        val occurrences = RecurrenceCalculator.occurrencesInRange(reminder, from, to)
        assertTrue(occurrences.isNotEmpty())

        for (occ in occurrences) {
            val zdt = occ.atZone(utcZone)
            val time = zdt.toLocalTime()
            assertTrue(time >= LocalTime.of(8, 0))
            assertTrue(time <= LocalTime.of(20, 0))
            assertTrue(ActiveDays.isDayActive(reminder.activeDaysMask, zdt.dayOfWeek))
        }
    }

    @Test
    fun propertyTest_deterministicForSameInput() {
        val reminder = sampleReminder(
            type = ReminderType.DAILY,
            startTime = LocalTime.of(14, 0)
        )
        val after = ZonedDateTime.of(2026, 11, 18, 12, 0, 0, 0, utcZone).toInstant()

        val firstResult = RecurrenceCalculator.nextOccurrence(reminder, after)
        assertNotNull(firstResult)

        // Execute 100 times: must return the exact same Instant
        for (i in 1..100) {
            val repeatResult = RecurrenceCalculator.nextOccurrence(reminder, after)
            assertEquals(firstResult, repeatResult)
        }
    }
}
