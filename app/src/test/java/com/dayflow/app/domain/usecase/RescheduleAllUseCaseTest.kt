package com.dayflow.app.domain.usecase

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.dayflow.app.data.local.DayflowDatabase
import com.dayflow.app.data.repository.ReminderRepositoryImpl
import com.dayflow.app.domain.model.OccurrenceStatus
import com.dayflow.app.domain.model.Priority
import com.dayflow.app.domain.model.Reminder
import com.dayflow.app.domain.model.ReminderOccurrence
import com.dayflow.app.domain.model.ReminderType
import com.dayflow.app.domain.repository.ReminderRepository
import com.dayflow.app.scheduling.AlarmScheduler
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
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
class RescheduleAllUseCaseTest {

    private lateinit var database: DayflowDatabase
    private lateinit var reminderRepository: ReminderRepository
    private val alarmScheduler: AlarmScheduler = mockk(relaxed = true)
    private lateinit var rescheduleAllUseCase: RescheduleAllUseCase

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

        rescheduleAllUseCase = RescheduleAllUseCase(
            reminderRepository = reminderRepository,
            alarmScheduler = alarmScheduler
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun sampleReminder(id: String, isEnabled: Boolean = true): Reminder {
        return Reminder(
            id = id,
            title = "Reminder $id",
            type = ReminderType.DAILY,
            startDate = LocalDate.of(2026, 11, 18),
            startTime = LocalTime.of(9, 0),
            isEnabled = isEnabled,
            priority = Priority.MEDIUM,
            timeZoneId = ZoneId.of("UTC")
        )
    }

    @Test
    fun rebootReconcile_schedulesMissingFutureOccurrences() = runBlocking {
        val reminder = sampleReminder("rem_boot_1")
        reminderRepository.saveReminder(reminder)

        val now = ZonedDateTime.of(2026, 11, 18, 7, 0, 0, 0, ZoneId.of("UTC")).toInstant()

        rescheduleAllUseCase(now = now)

        val pending = reminderRepository.getPendingOccurrencesDirect()
        assertEquals(1, pending.size)
        assertEquals(reminder.id, pending[0].reminderId)
        assertEquals(OccurrenceStatus.SCHEDULED, pending[0].status)
        assertEquals(
            ZonedDateTime.of(2026, 11, 18, 9, 0, 0, 0, ZoneId.of("UTC")).toInstant(),
            pending[0].scheduledAt
        )

        coVerify(exactly = 1) { alarmScheduler.scheduleOccurrence(pending[0], any()) }
    }

    @Test
    fun rebootReconcile_marksPastUnhandledOccurrencesAsMissed() = runBlocking {
        val reminder = sampleReminder("rem_past_1")
        reminderRepository.saveReminder(reminder)

        val pastTarget = ZonedDateTime.of(2026, 11, 18, 9, 0, 0, 0, ZoneId.of("UTC")).toInstant()
        val unhandledOcc = ReminderOccurrence("occ_unhandled", reminder.id, pastTarget, OccurrenceStatus.SCHEDULED)
        reminderRepository.scheduleOccurrence(unhandledOcc)

        val now = ZonedDateTime.of(2026, 11, 18, 12, 0, 0, 0, ZoneId.of("UTC")).toInstant()

        rescheduleAllUseCase(now = now)

        val updatedPastOcc = reminderRepository.getOccurrenceByIdDirect("occ_unhandled")
        assertNotNull(updatedPastOcc)
        assertEquals(OccurrenceStatus.MISSED, updatedPastOcc?.status)

        // And schedules tomorrow's slot
        val futureOccs = reminderRepository.getPendingOccurrencesDirect()
        assertEquals(1, futureOccs.size)
        assertEquals(
            ZonedDateTime.of(2026, 11, 19, 9, 0, 0, 0, ZoneId.of("UTC")).toInstant(),
            futureOccs[0].scheduledAt
        )
    }
}
