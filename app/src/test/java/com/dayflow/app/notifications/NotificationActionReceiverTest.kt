package com.dayflow.app.notifications

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
import com.dayflow.app.scheduling.AlarmScheduler
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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

@RunWith(RobolectricTestRunner::class)
class NotificationActionReceiverTest {

    private lateinit var database: DayflowDatabase
    private lateinit var reminderRepository: ReminderRepository
    private val alarmScheduler: AlarmScheduler = mockk(relaxed = true)
    private val notificationDispatcher: NotificationDispatcher = mockk(relaxed = true)
    private val settingsRepository: SettingsRepository = mockk(relaxed = true)

    private lateinit var receiver: NotificationActionReceiver
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

        every { settingsRepository.userProfile } returns flowOf(UserProfile())

        receiver = NotificationActionReceiver().apply {
            this.reminderRepository = this@NotificationActionReceiverTest.reminderRepository
            this.alarmScheduler = this@NotificationActionReceiverTest.alarmScheduler
            this.notificationDispatcher = this@NotificationActionReceiverTest.notificationDispatcher
            this.settingsRepository = this@NotificationActionReceiverTest.settingsRepository
            this.ioDispatcher = Dispatchers.Unconfined
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun sampleCompletionReminder(id: String = "rem_comp_act"): Reminder {
        return Reminder(
            id = id,
            title = "Drink Water",
            type = ReminderType.COMPLETION_INTERVAL,
            startDate = LocalDate.of(2026, 11, 18),
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(22, 0),
            intervalMinutes = 60,
            priority = Priority.MEDIUM,
            timeZoneId = ZoneId.of("UTC")
        )
    }

    @Test
    fun actionComplete_marksOccurrenceCompleted_andSchedulesNextForCompletionInterval() = runBlocking {
        val reminder = sampleCompletionReminder()
        reminderRepository.saveReminder(reminder)

        val occurrence = ReminderOccurrence(
            id = "occ_act_1",
            reminderId = reminder.id,
            scheduledAt = Instant.now(),
            status = OccurrenceStatus.DELIVERED
        )
        reminderRepository.scheduleOccurrence(occurrence)

        receiver.handleAction(
            NotificationActionReceiver.ACTION_COMPLETE,
            occurrence.id,
            reminder.id
        )

        // Notification cancelled
        verify(exactly = 1) { notificationDispatcher.cancelReminderNotification(occurrence.id) }

        // Occurrence status updated to COMPLETED
        val updatedOcc = reminderRepository.getOccurrenceByIdDirect(occurrence.id)
        assertEquals(OccurrenceStatus.COMPLETED, updatedOcc?.status)
        assertNotNull(updatedOcc?.handledAt)

        // Reminder lastCompletedAt updated
        val updatedReminder = reminderRepository.getReminderByIdDirect(reminder.id)
        assertNotNull(updatedReminder?.lastCompletedAt)

        // Next completion occurrence scheduled via alarmScheduler
        coVerify(exactly = 1) {
            alarmScheduler.scheduleOccurrence(match { it.id != occurrence.id }, any())
        }
    }

    @Test
    fun actionSnooze_createsTransientOccurrence_andPreservesReminderAnchor() = runBlocking {
        val reminder = Reminder(
            id = "rem_snooze",
            title = "Gym Session",
            type = ReminderType.DAILY,
            startDate = LocalDate.of(2026, 11, 18),
            startTime = LocalTime.of(18, 0),
            priority = Priority.HIGH,
            timeZoneId = ZoneId.of("UTC")
        )
        reminderRepository.saveReminder(reminder)

        val occurrence = ReminderOccurrence(
            id = "occ_snooze_1",
            reminderId = reminder.id,
            scheduledAt = Instant.now(),
            status = OccurrenceStatus.DELIVERED
        )
        reminderRepository.scheduleOccurrence(occurrence)

        receiver.handleAction(
            NotificationActionReceiver.ACTION_SNOOZE,
            occurrence.id,
            reminder.id
        )

        // Transient snooze occurrence scheduled
        coVerify(exactly = 1) {
            alarmScheduler.scheduleOccurrence(
                match { it.status == OccurrenceStatus.SNOOZED && it.snoozedUntil != null },
                match { it.id == reminder.id }
            )
        }

        // Reminder anchor fields MUST NOT be mutated
        val unchangedReminder = reminderRepository.getReminderByIdDirect(reminder.id)
        assertEquals(LocalDate.of(2026, 11, 18), unchangedReminder?.startDate)
        assertEquals(LocalTime.of(18, 0), unchangedReminder?.startTime)
    }

    @Test
    fun actionDismiss_recordsHandled_neverCompletesReminder() = runBlocking {
        val reminder = sampleCompletionReminder("rem_dismiss")
        reminderRepository.saveReminder(reminder)

        val occurrence = ReminderOccurrence(
            id = "occ_dismiss_1",
            reminderId = reminder.id,
            scheduledAt = Instant.now(),
            status = OccurrenceStatus.DELIVERED
        )
        reminderRepository.scheduleOccurrence(occurrence)

        receiver.handleAction(
            NotificationActionReceiver.ACTION_DISMISS,
            occurrence.id,
            reminder.id
        )

        val updatedOcc = reminderRepository.getOccurrenceByIdDirect(occurrence.id)
        assertNotNull(updatedOcc?.handledAt)
        // Invariant: Status must remain DELIVERED (never marked COMPLETED)
        assertNotEquals(OccurrenceStatus.COMPLETED, updatedOcc?.status)
        assertEquals(OccurrenceStatus.DELIVERED, updatedOcc?.status)
    }
}
