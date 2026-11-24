package com.dayflow.app.domain.recurrence

import com.dayflow.app.domain.model.Habit
import com.dayflow.app.domain.model.HabitLog
import java.time.LocalDate
import java.time.YearMonth

/**
 * Result containing the calculated current and longest streak for a habit.
 */
data class StreakResult(
    val currentStreak: Int,
    val longestStreak: Int
)

/**
 * Status representation of a habit for a single calendar day.
 */
data class HabitDayStatus(
    val date: LocalDate,
    val isScheduled: Boolean,
    val isCompleted: Boolean,
    val progressRatio: Float,
    val log: HabitLog?
)

/**
 * Pure Kotlin streak calculator for DAYFLOW habits.
 *
 * Invariants:
 * 1. Counts ONLY scheduled days according to [habit.activeDaysMask].
 * 2. Inactive weekdays neither break nor extend streaks.
 * 3. An uncompleted scheduled habit on the current calendar day (today) does NOT break
 *    the streak until the day ends (at midnight).
 * 4. Supports retroactive logging (recalculates deterministically).
 */
object StreakCalculator {

    /**
     * Calculates current and longest streaks.
     */
    fun calculateStreak(
        habit: Habit,
        logs: List<HabitLog>,
        today: LocalDate,
        habitCreatedAt: LocalDate? = null
    ): StreakResult {
        if (habit.activeDaysMask == 0) {
            return StreakResult(currentStreak = 0, longestStreak = 0)
        }

        val logMap: Map<LocalDate, HabitLog> = logs.associateBy { it.localDate }

        // --- Calculate Current Streak ---
        var currentStreak = 0
        val isTodayScheduled = ActiveDays.isDayActive(habit.activeDaysMask, today.dayOfWeek)
        val todayProgress = HabitProgressCalculator.calculateProgress(habit, logMap[today])

        if (isTodayScheduled && todayProgress.isCompleted) {
            currentStreak = 1
        }

        // Walk backwards starting from yesterday
        var checkDate = today.minusDays(1)
        val habitStartBound = habitCreatedAt ?: checkDate.minusYears(1)
        val scanLimit = minOf(habitStartBound, checkDate.minusYears(2))

        while (!checkDate.isBefore(scanLimit)) {
            val isScheduled = ActiveDays.isDayActive(habit.activeDaysMask, checkDate.dayOfWeek)
            if (isScheduled) {
                val progress = HabitProgressCalculator.calculateProgress(habit, logMap[checkDate])
                if (progress.isCompleted) {
                    currentStreak++
                } else {
                    // Missed a scheduled day in the past -> break streak
                    break
                }
            }
            // Inactive day -> skip without breaking or extending
            checkDate = checkDate.minusDays(1)
        }

        // --- Calculate Longest Streak ---
        val earliestLogDate = logs.minOfOrNull { it.localDate }
        val effectiveStart = listOfNotNull(habitCreatedAt, earliestLogDate).minOrNull() ?: today
        var longestStreak = 0
        var runningStreak = 0

        var walkDate = effectiveStart
        while (!walkDate.isAfter(today)) {
            val isScheduled = ActiveDays.isDayActive(habit.activeDaysMask, walkDate.dayOfWeek)
            if (isScheduled) {
                val progress = HabitProgressCalculator.calculateProgress(habit, logMap[walkDate])
                if (walkDate == today) {
                    if (progress.isCompleted) {
                        runningStreak++
                        longestStreak = maxOf(longestStreak, runningStreak)
                    }
                    // If today is not completed, do not reset runningStreak because the day has not ended
                } else {
                    if (progress.isCompleted) {
                        runningStreak++
                        longestStreak = maxOf(longestStreak, runningStreak)
                    } else {
                        runningStreak = 0
                    }
                }
            }
            walkDate = walkDate.plusDays(1)
        }

        longestStreak = maxOf(longestStreak, currentStreak)

        return StreakResult(
            currentStreak = currentStreak,
            longestStreak = longestStreak
        )
    }

    /**
     * Aggregates a 7-day status sequence starting from [weekStartDate] (typically Monday).
     */
    fun getWeeklyHistory(
        habit: Habit,
        logs: List<HabitLog>,
        weekStartDate: LocalDate
    ): List<HabitDayStatus> {
        val logMap = logs.associateBy { it.localDate }
        return (0..6).map { dayOffset ->
            val date = weekStartDate.plusDays(dayOffset.toLong())
            val isScheduled = ActiveDays.isDayActive(habit.activeDaysMask, date.dayOfWeek)
            val log = logMap[date]
            val progress = HabitProgressCalculator.calculateProgress(habit, log)
            HabitDayStatus(
                date = date,
                isScheduled = isScheduled,
                isCompleted = progress.isCompleted,
                progressRatio = progress.progressRatio,
                log = log
            )
        }
    }

    /**
     * Aggregates status for all days within [yearMonth].
     */
    fun getMonthlyHistory(
        habit: Habit,
        logs: List<HabitLog>,
        yearMonth: YearMonth
    ): List<HabitDayStatus> {
        val logMap = logs.associateBy { it.localDate }
        val daysInMonth = yearMonth.lengthOfMonth()
        return (1..daysInMonth).map { day ->
            val date = yearMonth.atDay(day)
            val isScheduled = ActiveDays.isDayActive(habit.activeDaysMask, date.dayOfWeek)
            val log = logMap[date]
            val progress = HabitProgressCalculator.calculateProgress(habit, log)
            HabitDayStatus(
                date = date,
                isScheduled = isScheduled,
                isCompleted = progress.isCompleted,
                progressRatio = progress.progressRatio,
                log = log
            )
        }
    }
}
