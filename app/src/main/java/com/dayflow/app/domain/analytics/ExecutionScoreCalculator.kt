package com.dayflow.app.domain.analytics

import kotlin.math.roundToInt

/**
 * Deterministic calculator for DAYFLOW's Daily Execution Score.
 *
 * IMPORTANT DISCLAIMER:
 * This metric is an internal application-defined execution score calculated strictly
 * from user-created tasks, scheduled habits, triggered reminders, and daily plans.
 * It is NOT an objective measure of productivity, psychological well-being, medical health,
 * or personal worth.
 *
 * Formula:
 * - Task Completion Factor (Weight = 0.40):
 *     T = if (tasksTotal == 0) 1.0 else (tasksCompleted / tasksTotal)
 * - Habit Consistency Factor (Weight = 0.30):
 *     H = if (habitsScheduled == 0) 1.0 else (habitsCompleted / habitsScheduled)
 * - Reminder Execution Factor (Weight = 0.15):
 *     R = if (occurrencesTriggered == 0) 1.0 else (remindersCompleted / occurrencesTriggered)
 * - Daily Plan Execution Factor (Weight = 0.15):
 *     P = if (planItemsTotal == 0) 1.0 else (planItemsCompleted / planItemsTotal)
 *
 * Composite Score:
 *     RawScore = (0.40 * T + 0.30 * H + 0.15 * R + 0.15 * P) * 100
 *     ExecutionScore = round(RawScore).coerceIn(0, 100)
 *
 * Edge Cases:
 * - If total commitments across all categories equal 0 (no activity), returns score = 0 with hasActivity = false.
 */
object ExecutionScoreCalculator {

    const val TASK_WEIGHT = 0.40f
    const val HABIT_WEIGHT = 0.30f
    const val REMINDER_WEIGHT = 0.15f
    const val PLAN_WEIGHT = 0.15f

    data class ScoreInput(
        val tasksCompleted: Int = 0,
        val tasksTotal: Int = 0,
        val habitsCompleted: Int = 0,
        val habitsScheduled: Int = 0,
        val remindersCompleted: Int = 0,
        val remindersMissed: Int = 0,
        val planItemsCompleted: Int = 0,
        val planItemsTotal: Int = 0
    )

    data class ScoreResult(
        val score: Int, // 0 to 100
        val hasActivity: Boolean,
        val taskRatio: Float,
        val habitRatio: Float,
        val reminderRatio: Float,
        val planRatio: Float
    )

    fun calculate(input: ScoreInput): ScoreResult {
        val totalCommitments = input.tasksTotal + input.habitsScheduled +
            (input.remindersCompleted + input.remindersMissed) + input.planItemsTotal

        if (totalCommitments == 0) {
            return ScoreResult(
                score = 0,
                hasActivity = false,
                taskRatio = 0f,
                habitRatio = 0f,
                reminderRatio = 0f,
                planRatio = 0f
            )
        }

        val taskRatio = if (input.tasksTotal > 0) {
            (input.tasksCompleted.toFloat() / input.tasksTotal.toFloat()).coerceIn(0f, 1f)
        } else {
            1.0f
        }

        val habitRatio = if (input.habitsScheduled > 0) {
            (input.habitsCompleted.toFloat() / input.habitsScheduled.toFloat()).coerceIn(0f, 1f)
        } else {
            1.0f
        }

        val reminderTriggered = input.remindersCompleted + input.remindersMissed
        val reminderRatio = if (reminderTriggered > 0) {
            (input.remindersCompleted.toFloat() / reminderTriggered.toFloat()).coerceIn(0f, 1f)
        } else {
            1.0f
        }

        val planRatio = if (input.planItemsTotal > 0) {
            (input.planItemsCompleted.toFloat() / input.planItemsTotal.toFloat()).coerceIn(0f, 1f)
        } else {
            1.0f
        }

        val rawScore = (TASK_WEIGHT * taskRatio +
            HABIT_WEIGHT * habitRatio +
            REMINDER_WEIGHT * reminderRatio +
            PLAN_WEIGHT * planRatio) * 100f

        val finalScore = rawScore.roundToInt().coerceIn(0, 100)

        return ScoreResult(
            score = finalScore,
            hasActivity = true,
            taskRatio = if (input.tasksTotal > 0) taskRatio else 0f,
            habitRatio = if (input.habitsScheduled > 0) habitRatio else 0f,
            reminderRatio = if (reminderTriggered > 0) reminderRatio else 0f,
            planRatio = if (input.planItemsTotal > 0) planRatio else 0f
        )
    }
}
