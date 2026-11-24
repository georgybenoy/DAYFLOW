package com.dayflow.app.feature.reminders

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.dayflow.app.data.local.DayflowDatabase
import com.dayflow.app.data.repository.ReminderRepositoryImpl
import com.dayflow.app.domain.model.OccurrenceStatus
import com.dayflow.app.domain.model.Priority
import com.dayflow.app.domain.model.Reminder
import com.dayflow.app.domain.model.ReminderOccurrence
import com.dayflow.app.domain.model.ReminderPrecision
import com.dayflow.app.domain.model.ReminderType
import com.dayflow.app.domain.recurrence.RecurrenceCalculator
import com.dayflow.app.domain.repository.ReminderRepository
import com.dayflow.app.domain.usecase.CompleteReminderOccurrenceUseCase
import com.dayflow.app.notifications.NotificationDispatcher
import com.dayflow.app.scheduling.AlarmScheduler
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

@RunWith(RobolectricTestRunner::class)
class ReminderWorkingBehavioursTest {

    private lateinit var database: DayflowDatabase
    private lateinit var reminderRepository: ReminderRepository
    private val alarmScheduler: AlarmScheduler = mockk(relaxed = true)
    private val notificationDispatcher: NotificationDispatcher = mockk(relaxed = true)
    private lateinit var completeReminderOccurrenceUseCase: CompleteReminderOccurrenceUseCase

    private val utc = ZoneId.of("UTC")
    private val baseDate = LocalDate.of(2026, 11, 18)

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, DayflowDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        reminderRepository = ReminderRepositoryImpl(
            reminderDao = database.reminderDao(),
            occurrenceDao = database.reminderOccurrenceDao(),
            ioDispatcher = Dispatchers.Unconfined
        )

        completeReminderOccurrenceUseCase = CompleteReminderOccurrenceUseCase(
            reminderRepository = reminderRepository,
            alarmScheduler = alarmScheduler,
            notificationDispatcher = notificationDispatcher
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    /**
     * Required Working Behaviour 1:
     * Water reminder every 2h between 8 AM and 10 PM.
     * Must yield exact 8 slots: 08:00, 10:00, 12:00, 14:00, 16:00, 18:00, 20:00, 22:00.
     * After 22:00, it rolls over cleanly to 08:00 next day.
     */
    @Test
    fun requiredBehaviour_waterReminder_every2Hours_8AMto10PM() {
        val waterReminder = Reminder(
            id = "rem-water-2h",
            title = "Drink Fresh Water",
            type = ReminderType.FIXED_INTERVAL,
            startDate = baseDate,
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(22, 0),
            intervalMinutes = 120, // 2 hours
            priority = Priority.HIGH,
            precision = ReminderPrecision.EXACT,
            timeZoneId = utc
        )

        val expectedHours = listOf(8, 10, 12, 14, 16, 18, 20, 22)
        var afterInstant = baseDate.atTime(7, 0).atZone(utc).toInstant()

        for (expectedHour in expectedHours) {
            val next = RecurrenceCalculator.nextOccurrence(waterReminder, afterInstant, utc)
            assertNotNull("Slot at $expectedHour:00 must not be null", next)

            val localNext = next!!.atZone(utc)
            assertEquals("Date should be baseDate", baseDate, localNext.toLocalDate())
            assertEquals("Hour should match expected", expectedHour, localNext.hour)
            assertEquals("Minute should be 0", 0, localNext.minute)

            afterInstant = next
        }

        // After 22:00, next occurrence rolls over to 08:00 tomorrow
        val rolloverNext = RecurrenceCalculator.nextOccurrence(waterReminder, afterInstant, utc)
        assertNotNull(rolloverNext)
        val rolloverLocal = rolloverNext!!.atZone(utc)
        assertEquals("Should advance to next day", baseDate.plusDays(1), rolloverLocal.toLocalDate())
        assertEquals("Rollover should start at window start (08:00)", 8, rolloverLocal.hour)
        assertEquals(0, rolloverLocal.minute)
    }

    /**
     * Required Working Behaviour 2:
     * Hourly completion-based push-ups.
     * When completed at 10:15, the next occurrence must be computed at 11:15.
     */
    @Test
    fun requiredBehaviour_hourlyCompletionPushUps_10_15_to_11_15() = runBlocking {
        val pushUpsReminder = Reminder(
            id = "rem-pushups-hourly",
            title = "Push-ups Set",
            type = ReminderType.COMPLETION_INTERVAL,
            startDate = baseDate,
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(20, 0),
            intervalMinutes = 60, // 1 hour
            priority = Priority.MEDIUM,
            timeZoneId = utc
        )

        reminderRepository.saveReminder(pushUpsReminder)

        // 1. Initial scheduled occurrence at 10:00
        val initialScheduledInstant = baseDate.atTime(10, 0).atZone(utc).toInstant()
        val initialOccurrence = ReminderOccurrence(
            id = "occ-pushups-1",
            reminderId = pushUpsReminder.id,
            scheduledAt = initialScheduledInstant,
            status = OccurrenceStatus.SCHEDULED
        )
        reminderRepository.scheduleOccurrence(initialOccurrence)

        // 2. User completes push-ups at 10:15
        val completedAt = baseDate.atTime(10, 15).atZone(utc).toInstant()
        val success = completeReminderOccurrenceUseCase(
            occurrenceId = initialOccurrence.id,
            reminderId = pushUpsReminder.id,
            now = completedAt
        )
        assertTrue("Completion use case should succeed", success)

        // 3. Verify original occurrence marked COMPLETED
        val updatedOcc = reminderRepository.getOccurrenceByIdDirect(initialOccurrence.id)
        assertNotNull(updatedOcc)
        assertEquals(OccurrenceStatus.COMPLETED, updatedOcc?.status)
        assertEquals(completedAt, updatedOcc?.handledAt)

        // 4. Verify reminder lastCompletedAt is updated
        val updatedReminder = reminderRepository.getReminderByIdDirect(pushUpsReminder.id)
        assertNotNull(updatedReminder)
        assertEquals(completedAt, updatedReminder?.lastCompletedAt)

        // 5. Verify next occurrence was generated for 11:15 (10:15 + 60m)
        val expectedNextInstant = baseDate.atTime(11, 15).atZone(utc).toInstant()
        val pendingOccurrences = reminderRepository.getPendingOccurrencesDirect()
            .filter { it.reminderId == pushUpsReminder.id }

        assertEquals("Should have exactly 1 newly scheduled occurrence", 1, pendingOccurrences.size)
        val newOccurrence = pendingOccurrences.first()
        assertEquals(expectedNextInstant, newOccurrence.scheduledAt)
        assertEquals(OccurrenceStatus.SCHEDULED, newOccurrence.status)

        // 6. Verify AlarmScheduler scheduled this new occurrence
        coVerify {
            alarmScheduler.scheduleOccurrence(
                match { it.scheduledAt == expectedNextInstant },
                match { it.id == pushUpsReminder.id }
            )
        }
    }
}
