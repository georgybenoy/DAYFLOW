package com.dayflow.app.domain.recurrence

import com.dayflow.app.domain.model.Habit
import com.dayflow.app.domain.model.HabitLog
import com.dayflow.app.domain.model.HabitTrackingType

/**
 * Result of evaluating habit progress for a specific day.
 */
data class HabitProgress(
    val completedValue: Double,
    val targetValue: Double,
    val isCompleted: Boolean,
    val progressRatio: Float,
    val formattedProgress: String
)

/**
 * Pure Kotlin calculator for habit completion and progress formatting across
 * BINARY, QUANTITY, DURATION, and REPETITION tracking types.
 */
object HabitProgressCalculator {

    /**
     * Calculates the progress for a given [habit] and optional daily [log].
     */
    fun calculateProgress(habit: Habit, log: HabitLog?): HabitProgress {
        val completedValue = log?.completedValue ?: 0.0
        val isExplicitlyCompleted = log?.isCompleted == true

        return when (habit.trackingType) {
            HabitTrackingType.BINARY -> {
                val isCompleted = isExplicitlyCompleted || completedValue >= 1.0
                HabitProgress(
                    completedValue = if (isCompleted) 1.0 else 0.0,
                    targetValue = 1.0,
                    isCompleted = isCompleted,
                    progressRatio = if (isCompleted) 1.0f else 0.0f,
                    formattedProgress = if (isCompleted) "Completed" else "Not completed"
                )
            }
            HabitTrackingType.QUANTITY,
            HabitTrackingType.DURATION,
            HabitTrackingType.REPETITION -> {
                val target = if (habit.targetValue > 0.0) habit.targetValue else 1.0
                val isCompleted = isExplicitlyCompleted || completedValue >= target
                val ratio = (completedValue / target).toFloat().coerceIn(0.0f, 1.0f)
                val formatted = formatProgressText(habit, completedValue, target)

                HabitProgress(
                    completedValue = completedValue,
                    targetValue = target,
                    isCompleted = isCompleted,
                    progressRatio = ratio,
                    formattedProgress = formatted
                )
            }
        }
    }

    private fun formatProgressText(habit: Habit, completed: Double, target: Double): String {
        val unitSuffix = if (!habit.unit.isNullOrBlank()) " ${habit.unit}" else ""
        val completedStr = formatNumber(completed)
        val targetStr = formatNumber(target)
        return "$completedStr / $targetStr$unitSuffix"
    }

    private fun formatNumber(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toLong().toString()
        } else {
            String.format(java.util.Locale.US, "%.1f", value)
        }
    }
}
