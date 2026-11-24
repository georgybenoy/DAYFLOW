package com.dayflow.app.data.local.dao

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.dayflow.app.data.local.DayflowDatabase
import com.dayflow.app.data.local.entity.ReminderEntity
import com.dayflow.app.data.local.entity.ReminderOccurrenceEntity
import com.dayflow.app.domain.model.OccurrenceStatus
import com.dayflow.app.domain.model.Priority
import com.dayflow.app.domain.model.ReminderPrecision
import com.dayflow.app.domain.model.ReminderType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
class ReminderDaoTest {

    private lateinit var database: DayflowDatabase
    private lateinit var reminderDao: ReminderDao
    private lateinit var occurrenceDao: ReminderOccurrenceDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, DayflowDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        reminderDao = database.reminderDao()
        occurrenceDao = database.reminderOccurrenceDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun createSampleReminder(id: String = "rem_water"): ReminderEntity {
        return ReminderEntity(
            id = id,
            title = "Drink Water",
            description = "Stay hydrated",
            categoryId = null,
            type = ReminderType.FIXED_INTERVAL,
            startDate = LocalDate.of(2026, 11, 18),
            startTime = LocalTime.of(8, 0),
            intervalMinutes = 120,
            activeDaysMask = 127,
            endTime = LocalTime.of(22, 0),
            endDate = null,
            priority = Priority.MEDIUM,
            precision = ReminderPrecision.EXACT,
            isEnabled = true,
            isPaused = false,
            timeZoneId = ZoneId.of("UTC"),
            anchorDate = LocalDate.of(2026, 11, 18),
            anchorTime = LocalTime.of(8, 0),
            lastCompletedAt = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }

    @Test
    fun reminderCrud_functionsCorrectly() = runBlocking {
        val reminder = createSampleReminder()
        reminderDao.insertReminder(reminder)

        val fetched = reminderDao.getReminderById("rem_water").first()
        assertNotNull(fetched)
        assertEquals("Drink Water", fetched?.title)

        val updated = reminder.copy(title = "Drink Cold Water")
        reminderDao.updateReminder(updated)

        val fetchedUpdated = reminderDao.getReminderById("rem_water").first()
        assertEquals("Drink Cold Water", fetchedUpdated?.title)

        reminderDao.deleteReminderById("rem_water")
        assertNull(reminderDao.getReminderById("rem_water").first())
    }

    @Test
    fun occurrence_uniqueConstraintEnforcesIdempotency() = runBlocking {
        val reminder = createSampleReminder("rem_pushups")
        reminderDao.insertReminder(reminder)

        val triggerTime = Instant.ofEpochMilli(1700000000000L)
        val occ1 = ReminderOccurrenceEntity(
            id = "occ_1",
            reminderId = "rem_pushups",
            scheduledAt = triggerTime,
            status = OccurrenceStatus.SCHEDULED,
            snoozedUntil = null,
            deliveredAt = null,
            handledAt = null,
            alarmRequestCode = 1001
        )
        val occ2 = ReminderOccurrenceEntity(
            id = "occ_2",
            reminderId = "rem_pushups",
            scheduledAt = triggerTime, // duplicate scheduledAt for same reminderId
            status = OccurrenceStatus.SCHEDULED,
            snoozedUntil = null,
            deliveredAt = null,
            handledAt = null,
            alarmRequestCode = 1002
        )

        occurrenceDao.insertOccurrence(occ1)

        var caughtConstraintException = false
        try {
            occurrenceDao.insertOccurrence(occ2)
        } catch (e: SQLiteConstraintException) {
            caughtConstraintException = true
        }

        assertTrue("Unique constraint on (reminderId, scheduledAt) must trigger", caughtConstraintException)
    }

    @Test
    fun deleteReminder_cascadesToOccurrences() = runBlocking {
        val reminder = createSampleReminder("rem_cascade")
        reminderDao.insertReminder(reminder)

        val occ = ReminderOccurrenceEntity(
            id = "occ_cascade_1",
            reminderId = "rem_cascade",
            scheduledAt = Instant.now(),
            status = OccurrenceStatus.SCHEDULED,
            snoozedUntil = null,
            deliveredAt = null,
            handledAt = null,
            alarmRequestCode = 2001
        )
        occurrenceDao.insertOccurrence(occ)

        assertEquals(1, occurrenceDao.getOccurrencesForReminder("rem_cascade").first().size)

        // Delete parent reminder
        reminderDao.deleteReminderById("rem_cascade")

        // Occurrences must be automatically deleted by cascade
        assertEquals(0, occurrenceDao.getOccurrencesForReminder("rem_cascade").first().size)
    }
}
