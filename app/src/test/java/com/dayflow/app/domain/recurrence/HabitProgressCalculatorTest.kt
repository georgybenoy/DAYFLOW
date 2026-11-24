package com.dayflow.app.domain.recurrence

import com.dayflow.app.domain.model.Habit
import com.dayflow.app.domain.model.HabitLog
import com.dayflow.app.domain.model.HabitTrackingType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HabitProgressCalculatorTest {

    private val testDate = LocalDate.of(2026, 11, 18)

    @Test
    fun binaryHabit_noLog_isNotCompleted() {
        val habit = Habit(name = "Meditate", trackingType = HabitTrackingType.BINARY)
        val progress = HabitProgressCalculator.calculateProgress(habit, null)

        assertFalse(progress.isCompleted)
        assertEquals(0.0f, progress.progressRatio, 0.001f)
        assertEquals("Not completed", progress.formattedProgress)
    }

    @Test
    fun binaryHabit_completedLog_isCompleted() {
        val habit = Habit(name = "Meditate", trackingType = HabitTrackingType.BINARY)
        val log = HabitLog(habitId = habit.id, localDate = testDate, completedValue = 1.0, isCompleted = true)
        val progress = HabitProgressCalculator.calculateProgress(habit, log)

        assertTrue(progress.isCompleted)
        assertEquals(1.0f, progress.progressRatio, 0.001f)
        assertEquals("Completed", progress.formattedProgress)
    }

    @Test
    fun quantityHabit_partialProgress() {
        val habit = Habit(name = "Drink Water", trackingType = HabitTrackingType.QUANTITY, targetValue = 2.0, unit = "L")
        val log = HabitLog(habitId = habit.id, localDate = testDate, completedValue = 1.0, isCompleted = false)
        val progress = HabitProgressCalculator.calculateProgress(habit, log)

        assertFalse(progress.isCompleted)
        assertEquals(0.5f, progress.progressRatio, 0.001f)
        assertEquals("1 / 2 L", progress.formattedProgress)
    }

    @Test
    fun quantityHabit_targetReached_isCompleted() {
        val habit = Habit(name = "Drink Water", trackingType = HabitTrackingType.QUANTITY, targetValue = 2.5, unit = "L")
        val log = HabitLog(habitId = habit.id, localDate = testDate, completedValue = 2.5, isCompleted = false)
        val progress = HabitProgressCalculator.calculateProgress(habit, log)

        assertTrue(progress.isCompleted)
        assertEquals(1.0f, progress.progressRatio, 0.001f)
        assertEquals("2.5 / 2.5 L", progress.formattedProgress)
    }

    @Test
    fun durationHabit_exceedingTarget_ratioClampedToOne() {
        val habit = Habit(name = "Reading", trackingType = HabitTrackingType.DURATION, targetValue = 30.0, unit = "min")
        val log = HabitLog(habitId = habit.id, localDate = testDate, completedValue = 45.0, isCompleted = false)
        val progress = HabitProgressCalculator.calculateProgress(habit, log)

        assertTrue(progress.isCompleted)
        assertEquals(1.0f, progress.progressRatio, 0.001f)
        assertEquals("45 / 30 min", progress.formattedProgress)
    }

    @Test
    fun repetitionHabit_formattingWithoutUnit() {
        val habit = Habit(name = "Pushups", trackingType = HabitTrackingType.REPETITION, targetValue = 50.0, unit = null)
        val log = HabitLog(habitId = habit.id, localDate = testDate, completedValue = 25.0, isCompleted = false)
        val progress = HabitProgressCalculator.calculateProgress(habit, log)

        assertFalse(progress.isCompleted)
        assertEquals(0.5f, progress.progressRatio, 0.001f)
        assertEquals("25 / 50", progress.formattedProgress)
    }
}
