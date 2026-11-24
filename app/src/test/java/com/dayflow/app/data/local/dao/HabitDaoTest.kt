package com.dayflow.app.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.dayflow.app.data.local.DayflowDatabase
import com.dayflow.app.data.local.entity.HabitEntity
import com.dayflow.app.data.local.entity.HabitLogEntity
import com.dayflow.app.domain.model.HabitTrackingType
import kotlinx.coroutines.flow.first
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

@RunWith(RobolectricTestRunner::class)
class HabitDaoTest {

    private lateinit var database: DayflowDatabase
    private lateinit var habitDao: HabitDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, DayflowDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        habitDao = database.habitDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun sampleHabit(id: String = "habit_reading"): HabitEntity {
        return HabitEntity(
            id = id,
            name = "Read Books",
            description = "Read 20 pages every day",
            categoryId = null,
            trackingType = HabitTrackingType.QUANTITY,
            targetValue = 20.0,
            unit = "pages",
            activeDaysMask = 127,
            reminderPreference = true,
            reminderTime = LocalTime.of(20, 0),
            isEnabled = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }

    @Test
    fun habitCrud_worksCorrectly() = runBlocking {
        val habit = sampleHabit()
        habitDao.insertHabit(habit)

        val retrieved = habitDao.getHabitById("habit_reading").first()
        assertNotNull(retrieved)
        assertEquals("Read Books", retrieved?.name)
        assertEquals(HabitTrackingType.QUANTITY, retrieved?.trackingType)
    }

    @Test
    fun habitLog_uniqueConstraintReplacesExistingDailyLog() = runBlocking {
        val habit = sampleHabit("habit_water")
        habitDao.insertHabit(habit)

        val logDate = LocalDate.of(2026, 11, 18)
        val morningLog = HabitLogEntity(
            id = "log_morning",
            habitId = "habit_water",
            localDate = logDate,
            completedValue = 3.0,
            isCompleted = false,
            loggedAt = Instant.now()
        )
        habitDao.insertOrUpdateHabitLog(morningLog)

        val retrievedMorning = habitDao.getLogForHabitAndDate("habit_water", logDate).first()
        assertEquals(3.0, retrievedMorning?.completedValue ?: 0.0, 0.01)

        // Log evening progress on same day (replaces previous log due to unique index)
        val eveningLog = HabitLogEntity(
            id = "log_morning", // or same habitId + date
            habitId = "habit_water",
            localDate = logDate,
            completedValue = 8.0,
            isCompleted = true,
            loggedAt = Instant.now()
        )
        habitDao.insertOrUpdateHabitLog(eveningLog)

        val allLogs = habitDao.getLogsForHabit("habit_water").first()
        assertEquals(1, allLogs.size)
        assertEquals(8.0, allLogs[0].completedValue, 0.01)
        assertTrue(allLogs[0].isCompleted)
    }

    @Test
    fun deleteHabit_cascadesToHabitLogs() = runBlocking {
        val habit = sampleHabit("habit_cascade")
        habitDao.insertHabit(habit)

        val log = HabitLogEntity(
            id = "log_cascade",
            habitId = "habit_cascade",
            localDate = LocalDate.now(),
            completedValue = 1.0,
            isCompleted = true,
            loggedAt = Instant.now()
        )
        habitDao.insertOrUpdateHabitLog(log)

        assertEquals(1, habitDao.getLogsForHabit("habit_cascade").first().size)

        // Delete habit
        habitDao.deleteHabitById("habit_cascade")

        // Logs must be cascade deleted
        assertEquals(0, habitDao.getLogsForHabit("habit_cascade").first().size)
    }
}
