package com.dayflow.app.domain.recurrence

import com.dayflow.app.domain.model.Habit
import com.dayflow.app.domain.model.HabitLog
import com.dayflow.app.domain.model.HabitTrackingType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class StreakCalculatorTest {

    // 2026-11-19 is a Thursday
    private val thursday = LocalDate.of(2026, 11, 19)
    private val wednesday = thursday.minusDays(1)
    private val tuesday = thursday.minusDays(2)
    private val monday = thursday.minusDays(3)

    @Test
    fun noLogs_returnsZeroStreak() {
        val habit = Habit(name = "Running", activeDaysMask = ActiveDays.ALL_DAYS_MASK)
        val result = StreakCalculator.calculateStreak(habit, emptyList(), thursday)

        assertEquals(0, result.currentStreak)
        assertEquals(0, result.longestStreak)
    }

    @Test
    fun consecutiveCompletedDays_incrementsStreak() {
        val habit = Habit(name = "Running", activeDaysMask = ActiveDays.ALL_DAYS_MASK)
        val logs = listOf(
            HabitLog(habitId = habit.id, localDate = monday, completedValue = 1.0, isCompleted = true),
            HabitLog(habitId = habit.id, localDate = tuesday, completedValue = 1.0, isCompleted = true),
            HabitLog(habitId = habit.id, localDate = wednesday, completedValue = 1.0, isCompleted = true),
            HabitLog(habitId = habit.id, localDate = thursday, completedValue = 1.0, isCompleted = true)
        )

        val result = StreakCalculator.calculateStreak(habit, logs, thursday)
        assertEquals(4, result.currentStreak)
        assertEquals(4, result.longestStreak)
    }

    @Test
    fun todayNotLogged_doesNotBreakStreakUntilDayEnds() {
        val habit = Habit(name = "Reading", activeDaysMask = ActiveDays.ALL_DAYS_MASK)
        // Mon, Tue, Wed completed. Thursday (today) not logged yet
        val logs = listOf(
            HabitLog(habitId = habit.id, localDate = monday, completedValue = 1.0, isCompleted = true),
            HabitLog(habitId = habit.id, localDate = tuesday, completedValue = 1.0, isCompleted = true),
            HabitLog(habitId = habit.id, localDate = wednesday, completedValue = 1.0, isCompleted = true)
        )

        val result = StreakCalculator.calculateStreak(habit, logs, thursday)
        // Streak is 3 from the completed prior days; today being pending does NOT break it
        assertEquals(3, result.currentStreak)
        assertEquals(3, result.longestStreak)
    }

    @Test
    fun inactiveWeekdays_neitherBreakNorExtendStreak() {
        // Mon, Tue, Thu active. Wednesday is INACTIVE
        val mask = ActiveDays.createMask(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY)
        val habit = Habit(name = "Gym", activeDaysMask = mask)

        val logs = listOf(
            HabitLog(habitId = habit.id, localDate = monday, completedValue = 1.0, isCompleted = true),
            HabitLog(habitId = habit.id, localDate = tuesday, completedValue = 1.0, isCompleted = true),
            // Wednesday inactive (no log)
            HabitLog(habitId = habit.id, localDate = thursday, completedValue = 1.0, isCompleted = true)
        )

        val result = StreakCalculator.calculateStreak(habit, logs, thursday)
        // Mon (1), Tue (2), Wed (skipped), Thu (3) -> Streak is 3
        assertEquals(3, result.currentStreak)
        assertEquals(3, result.longestStreak)
    }

    @Test
    fun missedScheduledDayInPast_breaksStreak() {
        val habit = Habit(name = "Water", activeDaysMask = ActiveDays.ALL_DAYS_MASK)
        // Mon completed, Tuesday missed, Wed completed, Thursday completed
        val logs = listOf(
            HabitLog(habitId = habit.id, localDate = monday, completedValue = 1.0, isCompleted = true),
            // Tuesday was missed
            HabitLog(habitId = habit.id, localDate = wednesday, completedValue = 1.0, isCompleted = true),
            HabitLog(habitId = habit.id, localDate = thursday, completedValue = 1.0, isCompleted = true)
        )

        val result = StreakCalculator.calculateStreak(habit, logs, thursday)
        assertEquals(2, result.currentStreak) // Wed + Thu
        assertEquals(2, result.longestStreak)
    }

    @Test
    fun retroactiveLog_restoresStreak() {
        val habit = Habit(name = "Water", activeDaysMask = ActiveDays.ALL_DAYS_MASK)
        val logsWithGap = listOf(
            HabitLog(habitId = habit.id, localDate = monday, completedValue = 1.0, isCompleted = true),
            HabitLog(habitId = habit.id, localDate = wednesday, completedValue = 1.0, isCompleted = true),
            HabitLog(habitId = habit.id, localDate = thursday, completedValue = 1.0, isCompleted = true)
        )
        val resultBefore = StreakCalculator.calculateStreak(habit, logsWithGap, thursday)
        assertEquals(2, resultBefore.currentStreak)

        // Retroactively add Tuesday
        val logsFixed = logsWithGap + HabitLog(habitId = habit.id, localDate = tuesday, completedValue = 1.0, isCompleted = true)
        val resultAfter = StreakCalculator.calculateStreak(habit, logsFixed, thursday)
        assertEquals(4, resultAfter.currentStreak)
        assertEquals(4, resultAfter.longestStreak)
    }

    @Test
    fun weeklyHistory_returnsSevenDaysStartingFromMonday() {
        val habit = Habit(name = "Meditate", activeDaysMask = ActiveDays.WEEKDAYS_MASK)
        val logs = listOf(
            HabitLog(habitId = habit.id, localDate = monday, completedValue = 1.0, isCompleted = true)
        )

        val weekly = StreakCalculator.getWeeklyHistory(habit, logs, monday)
        assertEquals(7, weekly.size)
        assertEquals(monday, weekly[0].date)
        assertTrue(weekly[0].isCompleted)
        assertTrue(weekly[0].isScheduled)

        // Saturday (index 5) is inactive
        val saturday = weekly[5]
        assertEquals(DayOfWeek.SATURDAY, saturday.date.dayOfWeek)
        assertFalse(saturday.isScheduled)
        assertFalse(saturday.isCompleted)
    }

    @Test
    fun monthlyHistory_returnsAllDaysOfMonth() {
        val habit = Habit(name = "Meditate", activeDaysMask = ActiveDays.ALL_DAYS_MASK)
        val yearMonth = YearMonth.of(2026, 11) // 30 days
        val monthly = StreakCalculator.getMonthlyHistory(habit, emptyList(), yearMonth)

        assertEquals(30, monthly.size)
        assertEquals(LocalDate.of(2026, 11, 1), monthly.first().date)
        assertEquals(LocalDate.of(2026, 11, 30), monthly.last().date)
    }
}
