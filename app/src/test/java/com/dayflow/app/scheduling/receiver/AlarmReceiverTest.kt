package com.dayflow.app.scheduling.receiver

import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.dayflow.app.data.local.DayflowDatabase
import com.dayflow.app.data.repository.ReminderRepositoryImpl
import com.dayflow.app.domain.model.OccurrenceStatus
import com.dayflow.app.domain.model.Priority
import com.dayflow.app.domain.model.Reminder
import com.dayflow.app.domain.model.ReminderOccurrence
import com.dayflow.app.domain.model.ReminderType
import com.dayflow.app.domain.model.UserProfile
import com.dayflow.app.domain.repository.ReminderRepository
import com.dayflow.app.domain.repository.SettingsRepository
import com.dayflow.app.notifications.NotificationDispatcher
import com.dayflow.app.scheduling.AlarmScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
class AlarmReceiverTest {

    private lateinit var database: DayflowDatabase
    private lateinit var reminderRepository: ReminderRepository
    private val alarmScheduler: AlarmScheduler = mockk(relaxed = true)
    private val notificationDispatcher: NotificationDispatcher = mockk(relaxed = true)
    private val settingsRepository: SettingsRepository = mockk(relaxed = true)

    private lateinit var receiver: AlarmReceiver
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, DayflowDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        reminderRepository = ReminderRepositoryImpl(
            reminderDao = database.reminderDao(),
            occurrenceDao = database.reminderOccurrenceDao(),
            ioDispatcher = Dispatchers.Unconfined
        )

        every { settingsRepository.userProfile } returns flowOf(
            UserProfile(
                quietHoursStart = LocalTime.of(22, 0),
                quietHoursEnd = LocalTime.of(7, 0)
            )
        )

        receiver = AlarmReceiver().apply {
            this.reminderRepository = this@AlarmReceiverTest.reminderRepository
            this.alarmScheduler = this@AlarmReceiverTest.alarmScheduler
            this.notificationDispatcher = this@AlarmReceiverTest.notificationDispatcher
            this.settingsRepository = this@AlarmReceiverTest.settingsRepository
            this.ioDispatcher = Dispatchers.Unconfined
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun sampleDailyReminder(id: String = "rem_daily"): Reminder {
        return Reminder(
            id = id,
            title = "Daily Standup",
            type = ReminderType.DAILY,
            startDate = LocalDate.of(2026, 11, 18),
            startTime = LocalTime.of(9, 0),
            priority = Priority.HIGH,
            timeZoneId = ZoneId.of("UTC")
        )
    }

    @Test
    fun alarmDelivery_isIdempotent_doubleDeliveryOnlyDispatchesOnce() = runBlocking {
        val reminder = sampleDailyReminder()
        reminderRepository.saveReminder(reminder)

        val scheduledInstant = ZonedDateTime.of(2026, 11, 18, 9, 0, 0, 0, ZoneId.of("UTC")).toInstant()
        val occurrence = ReminderOccurrence(
            id = "occ_idempotent_1",
            reminderId = reminder.id,
            scheduledAt = scheduledInstant,
            status = OccurrenceStatus.SCHEDULED
        )
        reminderRepository.scheduleOccurrence(occurrence)

        val intent = Intent().apply {
            putExtra(AlarmReceiver.EXTRA_OCCURRENCE_ID, occurrence.id)
            putExtra(AlarmReceiver.EXTRA_REMINDER_ID, reminder.id)
        }

        // 1st delivery attempt
        receiver.handleAlarm(occurrence.id, reminder.id)

        // Notification must be dispatched
        verify(exactly = 1) {
            notificationDispatcher.dispatchReminderNotification(any(), any(), any())
        }

        // Occurrence status in DB must now be DELIVERED
        val updated = reminderRepository.getOccurrenceByIdDirect(occurrence.id)
        assertNotNull(updated)
        assertEquals(OccurrenceStatus.DELIVERED, updated?.status)
        assertNotNull(updated?.deliveredAt)

        // 2nd delivery attempt (e.g. duplicate OS broadcast)
        receiver.handleAlarm(occurrence.id, reminder.id)

        // NotificationDispatcher must STILL have been called exactly 1 time (no duplicate notification)
        verify(exactly = 1) {
            notificationDispatcher.dispatchReminderNotification(any(), any(), any())
        }
    }

    @Test
    fun completionIntervalReminder_doesNotAdvanceOnDelivery() = runBlocking {
        val reminder = Reminder(
            id = "rem_comp_1",
            title = "Post-Meditation Journal",
            type = ReminderType.COMPLETION_INTERVAL,
            startDate = LocalDate.of(2026, 11, 18),
            startTime = LocalTime.of(8, 0),
            intervalMinutes = 60,
            timeZoneId = ZoneId.of("UTC")
        )
        reminderRepository.saveReminder(reminder)

        val triggerTime = Instant.now()
        val occurrence = ReminderOccurrence(
            id = "occ_comp_1",
            reminderId = reminder.id,
            scheduledAt = triggerTime,
            status = OccurrenceStatus.SCHEDULED
        )
        reminderRepository.scheduleOccurrence(occurrence)

        val intent = Intent().apply {
            putExtra(AlarmReceiver.EXTRA_OCCURRENCE_ID, occurrence.id)
            putExtra(AlarmReceiver.EXTRA_REMINDER_ID, reminder.id)
        }

        receiver.handleAlarm(occurrence.id, reminder.id)

        // Occurrence marked delivered
        val deliveredOcc = reminderRepository.getOccurrenceByIdDirect(occurrence.id)
        assertEquals(OccurrenceStatus.DELIVERED, deliveredOcc?.status)

        // Verify alarmScheduler.scheduleOccurrence was NOT called to create a future slot
        // (completion reminders must wait for user interaction)
        coVerify(exactly = 0) {
            alarmScheduler.scheduleOccurrence(match { it.id != occurrence.id }, any())
        }
    }
}
