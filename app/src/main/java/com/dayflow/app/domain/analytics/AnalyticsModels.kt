package com.dayflow.app.domain.analytics

import java.time.LocalDate
import java.time.YearMonth

data class DailyAnalytics(
    val date: LocalDate,
    val executionScore: Int, // 0 to 100
    val hasActivity: Boolean,
    val tasksCompleted: Int,
    val tasksTotal: Int,
    val taskRatio: Float,
    val habitsCompleted: Int,
    val habitsScheduled: Int,
    val habitRatio: Float,
    val remindersCompleted: Int,
    val remindersMissed: Int,
    val reminderRatio: Float,
    val planItemsCompleted: Int,
    val planItemsTotal: Int,
    val planRatio: Float
)

data class WeeklyAnalytics(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val averageScore: Int,
    val days: List<DailyAnalytics>,
    val totalTasksCompleted: Int,
    val habitConsistencyPercentage: Int,
    val completedRemindersCount: Int,
    val missedRemindersCount: Int
)

data class MonthlyAnalytics(
    val yearMonth: YearMonth,
    val days: List<DailyAnalytics>,
    val averageScore: Int,
    val totalTasksCompleted: Int,
    val habitConsistencyPercentage: Int
)

data class StreakSummary(
    val currentStreakDays: Int,
    val longestStreakDays: Int,
    val habitStreaks: Map<String, Int> = emptyMap()
)

data class InsightsData(
    val today: DailyAnalytics,
    val weekly: WeeklyAnalytics,
    val monthly: MonthlyAnalytics,
    val streakSummary: StreakSummary,
    val trendDays: List<DailyAnalytics> // Last 14 or 30 days for trend line chart
)
