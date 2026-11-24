package com.dayflow.app.domain.analytics

import com.dayflow.app.core.time.TimeProvider
import com.dayflow.app.domain.recurrence.StreakCalculator
import com.dayflow.app.domain.model.Habit
import com.dayflow.app.domain.model.HabitLog
import com.dayflow.app.domain.model.OccurrenceStatus
import com.dayflow.app.domain.model.PlanStatus
import com.dayflow.app.domain.model.ReminderOccurrence
import com.dayflow.app.domain.model.Task
import com.dayflow.app.domain.model.TaskStatus
import com.dayflow.app.domain.model.WeekStartDay
import com.dayflow.app.domain.repository.DailyPlanRepository
import com.dayflow.app.domain.repository.HabitRepository
import com.dayflow.app.domain.repository.ReminderRepository
import com.dayflow.app.domain.repository.TaskRepository
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

class GetInsightsUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val habitRepository: HabitRepository,
    private val reminderRepository: ReminderRepository,
    private val dailyPlanRepository: DailyPlanRepository,
    private val timeProvider: TimeProvider
) {

    suspend operator fun invoke(
        targetDate: LocalDate = timeProvider.nowLocalDate(),
        weekStartDay: WeekStartDay = WeekStartDay.MONDAY,
        zoneId: ZoneId = timeProvider.zoneId()
    ): InsightsData {
        // 1. Compute bounds: 30 days prior to monthStart up to weekEnd
        val weekStart = computeWeekStart(targetDate, weekStartDay)
        val weekEnd = weekStart.plusDays(6)

        val currentMonth = YearMonth.from(targetDate)
        val monthStart = currentMonth.atDay(1)
        val monthEnd = currentMonth.atEndOfMonth()

        val trendStart = targetDate.minusDays(13) // 14-day trend ending on targetDate

        val queryStart = minOf(weekStart, monthStart, trendStart).minusDays(30)
        val queryEnd = maxOf(weekEnd, monthEnd, targetDate)

        val queryStartEpoch = queryStart.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val queryEndEpoch = queryEnd.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

        // 2. Fetch stored records directly from repositories
        val allTasks = taskRepository.getTasksBetweenDirect(queryStart, queryEnd)
        val activeHabits = habitRepository.getActiveHabitsDirect()
        val reminderOccurrences = reminderRepository.getOccurrencesBetweenDirect(queryStartEpoch, queryEndEpoch)
        val allPlans = dailyPlanRepository.getPlansBetweenDirect(queryStart, queryEnd)

        // 3. Group records by LocalDate for fast lookup
        val tasksByDate = allTasks.groupBy { task ->
            task.plannedDate ?: task.deadlineDate
        }

        val logsByHabitAndDate = mutableMapOf<Pair<String, LocalDate>, HabitLog>()
        // We will collect habit logs from the Flow or directly
        // For habit logs, let's also fetch logs for each active habit
        val habitLogsList = mutableListOf<HabitLog>()
        activeHabits.forEach { habit ->
            val logs = habitRepository.getLogsForHabitInRangeDirect(habit.id, queryStart, queryEnd)
            habitLogsList.addAll(logs)
            logs.forEach { log ->
                logsByHabitAndDate[Pair(log.habitId, log.localDate)] = log
            }
        }

        val occurrencesByDate = reminderOccurrences.groupBy { occ ->
            occ.scheduledAt.atZone(zoneId).toLocalDate()
        }

        val plansByDate = allPlans.associateBy { it.localDate }

        // Helper to compute DailyAnalytics for any date
        fun computeDayAnalytics(date: LocalDate): DailyAnalytics {
            val dayTasks = tasksByDate[date].orEmpty()
            val tasksTotal = dayTasks.size
            val tasksCompleted = dayTasks.count { it.status == TaskStatus.COMPLETED }

            // Scheduled habits for weekday
            val dayOfWeekValue = date.dayOfWeek.value // 1 (Mon) to 7 (Sun)
            val bit = 1 shl (dayOfWeekValue - 1)
            val scheduledHabits = activeHabits.filter { (it.activeDaysMask and bit) != 0 }
            val habitsScheduled = scheduledHabits.size
            val habitsCompleted = scheduledHabits.count { habit ->
                logsByHabitAndDate[Pair(habit.id, date)]?.isCompleted == true
            }

            // Reminders
            val dayOccurrences = occurrencesByDate[date].orEmpty()
            val remindersCompleted = dayOccurrences.count { it.status == OccurrenceStatus.COMPLETED }
            val remindersMissed = dayOccurrences.count { it.status == OccurrenceStatus.MISSED }

            // Daily Plan
            val dayPlan = plansByDate[date]
            val planItems = if (dayPlan != null && dayPlan.status == PlanStatus.APPROVED) dayPlan.items else emptyList()
            val planItemsTotal = planItems.size
            val planItemsCompleted = planItems.count { it.isCompleted }

            val scoreResult = ExecutionScoreCalculator.calculate(
                ExecutionScoreCalculator.ScoreInput(
                    tasksCompleted = tasksCompleted,
                    tasksTotal = tasksTotal,
                    habitsCompleted = habitsCompleted,
                    habitsScheduled = habitsScheduled,
                    remindersCompleted = remindersCompleted,
                    remindersMissed = remindersMissed,
                    planItemsCompleted = planItemsCompleted,
                    planItemsTotal = planItemsTotal
                )
            )

            return DailyAnalytics(
                date = date,
                executionScore = scoreResult.score,
                hasActivity = scoreResult.hasActivity,
                tasksCompleted = tasksCompleted,
                tasksTotal = tasksTotal,
                taskRatio = scoreResult.taskRatio,
                habitsCompleted = habitsCompleted,
                habitsScheduled = habitsScheduled,
                habitRatio = scoreResult.habitRatio,
                remindersCompleted = remindersCompleted,
                remindersMissed = remindersMissed,
                reminderRatio = scoreResult.reminderRatio,
                planItemsCompleted = planItemsCompleted,
                planItemsTotal = planItemsTotal,
                planRatio = scoreResult.planRatio
            )
        }

        // 4. Compute Today's Analytics
        val todayAnalytics = computeDayAnalytics(targetDate)

        // 5. Compute Weekly Analytics
        val weekDays = (0..6).map { offset -> computeDayAnalytics(weekStart.plusDays(offset.toLong())) }
        val weekActiveDays = weekDays.filter { it.hasActivity }
        val weekAvgScore = if (weekActiveDays.isNotEmpty()) {
            weekActiveDays.map { it.executionScore }.average().toInt()
        } else {
            0
        }
        val weekTasksCompleted = weekDays.sumOf { it.tasksCompleted }
        val weekHabitsScheduled = weekDays.sumOf { it.habitsScheduled }
        val weekHabitsCompleted = weekDays.sumOf { it.habitsCompleted }
        val weekHabitConsistency = if (weekHabitsScheduled > 0) {
            (weekHabitsCompleted * 100) / weekHabitsScheduled
        } else {
            0
        }
        val weekRemindersCompleted = weekDays.sumOf { it.remindersCompleted }
        val weekRemindersMissed = weekDays.sumOf { it.remindersMissed }

        val weeklyAnalytics = WeeklyAnalytics(
            startDate = weekStart,
            endDate = weekEnd,
            averageScore = weekAvgScore,
            days = weekDays,
            totalTasksCompleted = weekTasksCompleted,
            habitConsistencyPercentage = weekHabitConsistency,
            completedRemindersCount = weekRemindersCompleted,
            missedRemindersCount = weekRemindersMissed
        )

        // 6. Compute Monthly Analytics
        val monthDaysCount = currentMonth.lengthOfMonth()
        val monthDays = (1..monthDaysCount).map { day -> computeDayAnalytics(currentMonth.atDay(day)) }
        val monthActiveDays = monthDays.filter { it.hasActivity }
        val monthAvgScore = if (monthActiveDays.isNotEmpty()) {
            monthActiveDays.map { it.executionScore }.average().toInt()
        } else {
            0
        }
        val monthTasksCompleted = monthDays.sumOf { it.tasksCompleted }
        val monthHabitsScheduled = monthDays.sumOf { it.habitsScheduled }
        val monthHabitsCompleted = monthDays.sumOf { it.habitsCompleted }
        val monthHabitConsistency = if (monthHabitsScheduled > 0) {
            (monthHabitsCompleted * 100) / monthHabitsScheduled
        } else {
            0
        }

        val monthlyAnalytics = MonthlyAnalytics(
            yearMonth = currentMonth,
            days = monthDays,
            averageScore = monthAvgScore,
            totalTasksCompleted = monthTasksCompleted,
            habitConsistencyPercentage = monthHabitConsistency
        )

        // 7. Compute Streaks
        val habitStreaks = mutableMapOf<String, Int>()
        var maxCurrentStreak = 0
        var maxLongestStreak = 0

        activeHabits.forEach { habit ->
            val logs = habitLogsList.filter { it.habitId == habit.id }
            val streakResult = StreakCalculator.calculateStreak(
                habit = habit,
                logs = logs,
                today = targetDate
            )
            habitStreaks[habit.id] = streakResult.currentStreak
            if (streakResult.currentStreak > maxCurrentStreak) {
                maxCurrentStreak = streakResult.currentStreak
            }
            if (streakResult.longestStreak > maxLongestStreak) {
                maxLongestStreak = streakResult.longestStreak
            }
        }

        val streakSummary = StreakSummary(
            currentStreakDays = maxCurrentStreak,
            longestStreakDays = maxLongestStreak,
            habitStreaks = habitStreaks
        )

        // 8. Trend Days (last 14 days)
        val trendDays = (13 downTo 0).map { offset ->
            computeDayAnalytics(targetDate.minusDays(offset.toLong()))
        }

        return InsightsData(
            today = todayAnalytics,
            weekly = weeklyAnalytics,
            monthly = monthlyAnalytics,
            streakSummary = streakSummary,
            trendDays = trendDays
        )
    }

    private fun computeWeekStart(date: LocalDate, weekStartDay: WeekStartDay): LocalDate {
        var d = date
        while (d.dayOfWeek != weekStartDay.dayOfWeek) {
            d = d.minusDays(1)
        }
        return d
    }
}
