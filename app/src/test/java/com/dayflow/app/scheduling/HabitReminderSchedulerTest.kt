package com.dayflow.app.scheduling

import com.dayflow.app.domain.model.Habit
import com.dayflow.app.domain.model.HabitTrackingType
import com.dayflow.app.domain.model.Reminder
import com.dayflow.app.domain.model.ReminderType
import com.dayflow.app.domain.usecase.DeleteReminderUseCase
import com.dayflow.app.domain.usecase.ScheduleReminderUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalTime

class HabitReminderSchedulerTest {

    private val scheduleReminderUseCase: ScheduleReminderUseCase = mockk(relaxed = true)
    private val deleteReminderUseCase: DeleteReminderUseCase = mockk(relaxed = true)
    private lateinit var scheduler: HabitReminderScheduler

    @Before
    fun setup() {
        scheduler = HabitReminderScheduler(
            scheduleReminderUseCase = scheduleReminderUseCase,
            deleteReminderUseCase = deleteReminderUseCase
        )
    }

    @Test
    fun syncHabitReminder_whenEnabledWithReminder_schedulesReminder() = runTest {
        val habit = Habit(
            id = "habit-123",
            name = "Morning Water",
            trackingType = HabitTrackingType.QUANTITY,
            targetValue = 2.0,
            reminderPreference = true,
            reminderTime = LocalTime.of(8, 30),
            isEnabled = true
        )

        val reminderSlot = slot<Reminder>()
        coEvery { scheduleReminderUseCase(capture(reminderSlot), any()) } returns true

        scheduler.syncHabitReminder(habit)

        coVerify { scheduleReminderUseCase(any(), any()) }
        val captured = reminderSlot.captured
        assertEquals("habit_reminder_habit-123", captured.id)
        assertEquals("Morning Water", captured.title)
        assertTrue(captured.description?.contains("Habit: QUANTITY") == true)
        assertEquals(ReminderType.CUSTOM_WEEKDAYS, captured.type)
        assertEquals(LocalTime.of(8, 30), captured.startTime)
        assertTrue(captured.isEnabled)
    }

    @Test
    fun syncHabitReminder_whenReminderPreferenceDisabled_deletesReminder() = runTest {
        val habit = Habit(
            id = "habit-456",
            name = "Evening Walk",
            reminderPreference = false,
            isEnabled = true
        )

        scheduler.syncHabitReminder(habit)

        coVerify { deleteReminderUseCase("habit_reminder_habit-456") }
    }

    @Test
    fun syncHabitReminder_whenHabitDisabled_deletesReminder() = runTest {
        val habit = Habit(
            id = "habit-789",
            name = "Journaling",
            reminderPreference = true,
            reminderTime = LocalTime.of(21, 0),
            isEnabled = false
        )

        scheduler.syncHabitReminder(habit)

        coVerify { deleteReminderUseCase("habit_reminder_habit-789") }
    }

    @Test
    fun cancelHabitReminder_callsDeleteReminder() = runTest {
        scheduler.cancelHabitReminder("habit-999")

        coVerify { deleteReminderUseCase("habit_reminder_habit-999") }
    }
}
