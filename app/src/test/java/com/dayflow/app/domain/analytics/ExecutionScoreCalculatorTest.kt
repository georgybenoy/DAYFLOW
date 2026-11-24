package com.dayflow.app.domain.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExecutionScoreCalculatorTest {

    @Test
    fun zeroCommitments_returnsZeroScoreAndNoActivity() {
        val input = ExecutionScoreCalculator.ScoreInput()
        val result = ExecutionScoreCalculator.calculate(input)

        assertEquals(0, result.score)
        assertFalse(result.hasActivity)
        assertEquals(0f, result.taskRatio, 0.001f)
        assertEquals(0f, result.habitRatio, 0.001f)
        assertEquals(0f, result.reminderRatio, 0.001f)
        assertEquals(0f, result.planRatio, 0.001f)
    }

    @Test
    fun perfectCompletionAcrossAllCategories_returns100() {
        val input = ExecutionScoreCalculator.ScoreInput(
            tasksCompleted = 5,
            tasksTotal = 5,
            habitsCompleted = 3,
            habitsScheduled = 3,
            remindersCompleted = 4,
            remindersMissed = 0,
            planItemsCompleted = 2,
            planItemsTotal = 2
        )
        val result = ExecutionScoreCalculator.calculate(input)

        assertEquals(100, result.score)
        assertTrue(result.hasActivity)
        assertEquals(1.0f, result.taskRatio, 0.001f)
        assertEquals(1.0f, result.habitRatio, 0.001f)
        assertEquals(1.0f, result.reminderRatio, 0.001f)
        assertEquals(1.0f, result.planRatio, 0.001f)
    }

    @Test
    fun taskOnlyActivity_calculatesAccurately() {
        // 2 of 4 tasks completed (50%), no habits/reminders/plans
        // Task weight = 0.40 * 0.5 = 0.20
        // Unused categories default to 1.0 factor:
        // habit = 0.30 * 1.0 = 0.30
        // reminder = 0.15 * 1.0 = 0.15
        // plan = 0.15 * 1.0 = 0.15
        // Raw = (0.20 + 0.30 + 0.15 + 0.15) * 100 = 80
        val input = ExecutionScoreCalculator.ScoreInput(
            tasksCompleted = 2,
            tasksTotal = 4
        )
        val result = ExecutionScoreCalculator.calculate(input)

        assertEquals(80, result.score)
        assertTrue(result.hasActivity)
        assertEquals(0.5f, result.taskRatio, 0.001f)
        assertEquals(0f, result.habitRatio, 0.001f)
    }

    @Test
    fun partialCompletionAcrossCategories_computesWeightedScore() {
        // Tasks: 2 / 4 = 0.5 -> 0.40 * 0.5 = 0.20
        // Habits: 1 / 2 = 0.5 -> 0.30 * 0.5 = 0.15
        // Reminders: 1 / 2 = 0.5 -> 0.15 * 0.5 = 0.075
        // Plan: 1 / 2 = 0.5 -> 0.15 * 0.5 = 0.075
        // Raw = (0.20 + 0.15 + 0.075 + 0.075) * 100 = 50
        val input = ExecutionScoreCalculator.ScoreInput(
            tasksCompleted = 2,
            tasksTotal = 4,
            habitsCompleted = 1,
            habitsScheduled = 2,
            remindersCompleted = 1,
            remindersMissed = 1,
            planItemsCompleted = 1,
            planItemsTotal = 2
        )
        val result = ExecutionScoreCalculator.calculate(input)

        assertEquals(50, result.score)
        assertTrue(result.hasActivity)
        assertEquals(0.5f, result.taskRatio, 0.001f)
        assertEquals(0.5f, result.habitRatio, 0.001f)
        assertEquals(0.5f, result.reminderRatio, 0.001f)
        assertEquals(0.5f, result.planRatio, 0.001f)
    }

    @Test
    fun zeroCompletionAcrossAllScheduled_computesCorrectly() {
        // Tasks: 0/2 = 0
        // Habits: 0/2 = 0
        // Reminders: 0/2 = 0 (2 missed)
        // Plan: 0/2 = 0
        // Raw = 0
        val input = ExecutionScoreCalculator.ScoreInput(
            tasksCompleted = 0,
            tasksTotal = 2,
            habitsCompleted = 0,
            habitsScheduled = 2,
            remindersCompleted = 0,
            remindersMissed = 2,
            planItemsCompleted = 0,
            planItemsTotal = 2
        )
        val result = ExecutionScoreCalculator.calculate(input)

        assertEquals(0, result.score)
        assertTrue(result.hasActivity)
    }
}
