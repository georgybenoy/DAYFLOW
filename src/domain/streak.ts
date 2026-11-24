import { Habit, HabitLog } from './models';

export interface StreakResult {
  currentStreak: number;
  longestStreak: number;
  totalCompletedDays: number;
  consistencyRate: number; // percentage (0 - 100)
}

/**
 * Pure StreakCalculator.
 * Evaluates habit streaks based on scheduled days.
 * Inactive days do not break the streak.
 * Today not yet completed does not break the streak if previous scheduled day was completed.
 */
export class StreakCalculator {
  static calculate(habit: Habit, logs: HabitLog[], todayDateStr: string = new Date().toISOString().split('T')[0]): StreakResult {
    // Map completed dates
    const completedDates = new Set(
      logs.filter(l => l.isCompleted).map(l => l.date)
    );

    const scheduledDays = new Set(habit.scheduledDays);
    if (scheduledDays.size === 0) {
      return { currentStreak: 0, longestStreak: 0, totalCompletedDays: 0, consistencyRate: 0 };
    }

    // Sort logs to find earliest date or start from habit creation
    const creationDateStr = habit.createdAt.split('T')[0];
    const startDate = new Date(creationDateStr);
    const today = new Date(todayDateStr);

    // If today is earlier than startDate, clamp
    if (today < startDate) {
      return { currentStreak: 0, longestStreak: 0, totalCompletedDays: 0, consistencyRate: 0 };
    }

    // Build timeline of scheduled dates from startDate to today
    const scheduledTimeline: string[] = [];
    const curr = new Date(startDate);
    while (curr <= today) {
      const jsDay = curr.getDay();
      const dayNum = jsDay === 0 ? 7 : jsDay;
      if (scheduledDays.has(dayNum)) {
        scheduledTimeline.push(curr.toISOString().split('T')[0]);
      }
      curr.setDate(curr.getDate() + 1);
    }

    if (scheduledTimeline.length === 0) {
      return { currentStreak: 0, longestStreak: 0, totalCompletedDays: 0, consistencyRate: 0 };
    }

    // Calculate streaks
    let longestStreak = 0;
    let tempStreak = 0;
    let totalCompleted = 0;

    for (let i = 0; i < scheduledTimeline.length; i++) {
      const d = scheduledTimeline[i];
      const isCompleted = completedDates.has(d);

      if (isCompleted) {
        tempStreak++;
        totalCompleted++;
        if (tempStreak > longestStreak) {
          longestStreak = tempStreak;
        }
      } else {
        // If it's today and not completed yet, don't reset tempStreak for current streak calculation
        if (d === todayDateStr) {
          // today not done yet
        } else {
          tempStreak = 0;
        }
      }
    }

    // Current streak: trace backwards from the most recent scheduled day
    let currentStreak = 0;
    for (let i = scheduledTimeline.length - 1; i >= 0; i--) {
      const d = scheduledTimeline[i];
      if (d === todayDateStr && !completedDates.has(d)) {
        // Today is not completed yet; skip today and check yesterday / last scheduled day
        continue;
      }
      if (completedDates.has(d)) {
        currentStreak++;
      } else {
        break;
      }
    }

    const consistencyRate = scheduledTimeline.length > 0 
      ? Math.round((totalCompleted / scheduledTimeline.length) * 100) 
      : 0;

    return {
      currentStreak,
      longestStreak: Math.max(longestStreak, currentStreak),
      totalCompletedDays: totalCompleted,
      consistencyRate,
    };
  }
}
