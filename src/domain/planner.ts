import { Task, Habit, Reminder, DailyPlan, DailyPlanItem, UserSettings, DailyReview } from './models';

export interface PlannerInput {
  date: string; // YYYY-MM-DD
  tasks: Task[];
  habits: Habit[];
  reminders: Reminder[];
  settings: UserSettings;
  previousReviews?: DailyReview[];
}

/**
 * Deterministic Daily Planner Engine.
 * Pure function: deterministic, no AI, stable sort keys.
 * Preserves unscheduled tasks. Never modifies task deadlines.
 */
export class DeterministicPlanner {
  // Scoring Constants
  private static readonly PRIORITY_WEIGHTS: Record<string, number> = {
    URGENT: 1000,
    HIGH: 500,
    MEDIUM: 200,
    LOW: 50,
  };
  private static readonly OVERDUE_BONUS = 350;
  private static readonly TODAY_DEADLINE_BONUS = 250;
  private static readonly BUFFER_MINUTES = 10;

  static generatePlan(input: PlannerInput): DailyPlan {
    const { date, tasks, habits, reminders, settings, previousReviews = [] } = input;
    const warnings: string[] = [];

    // Parse review exclusions
    const excludedTaskIds = new Set<string>();
    for (const review of previousReviews) {
      if (review.decisions) {
        for (const dec of review.decisions) {
          if (dec.action === 'EXCLUDE' || dec.action === 'CANCEL') {
            excludedTaskIds.add(dec.taskId);
          }
        }
      }
    }

    // 1. Collect eligible tasks
    const eligibleTasks = tasks.filter(
      t => t.status === 'PENDING' && !excludedTaskIds.has(t.id)
    );

    // 2. Identify active habits for this day
    const targetDateObj = new Date(date);
    const jsDay = targetDateObj.getDay();
    const dayNum = jsDay === 0 ? 7 : jsDay;
    const activeHabits = habits.filter(h => !h.archived && h.scheduledDays.includes(dayNum));

    // 3. Fixed commitments (blocking reminders)
    const fixedReminders = reminders.filter(r => !r.isPaused && r.isBlocking);

    // 4. Calculate Task Priority Scores
    interface ScoredTask {
      task: Task;
      score: number;
    }

    const scoredTasks: ScoredTask[] = eligibleTasks.map(task => {
      let score = this.PRIORITY_WEIGHTS[task.priority] || 50;

      if (task.deadlineDate) {
        if (task.deadlineDate < date) {
          score += this.OVERDUE_BONUS;
        } else if (task.deadlineDate === date) {
          score += this.TODAY_DEADLINE_BONUS;
        }
      }

      // Planned for today bonus
      if (task.plannedDate === date) {
        score += 150;
      }

      return { task, score };
    });

    // 5. Stable Sort: descending score, then title, then id
    scoredTasks.sort((a, b) => {
      if (b.score !== a.score) return b.score - a.score;
      if (a.task.title !== b.task.title) return a.task.title.localeCompare(b.task.title);
      return a.task.id.localeCompare(b.task.id);
    });

    // 6. Time Slot Allocation
    // Parse day start/end
    const [startH, startM] = (settings.dayStartTime || '08:00').split(':').map(Number);
    const [endH, endM] = (settings.dayEndTime || '22:00').split(':').map(Number);
    let currentMinuteOfDay = startH * 60 + startM;
    const dayEndMinute = endH * 60 + endM;

    const planItems: DailyPlanItem[] = [];
    const unscheduledTasks: Task[] = [];
    let sortOrder = 0;

    // Helper to format minute of day into HH:mm
    const formatTime = (min: number) => {
      const h = Math.floor(min / 60);
      const m = min % 60;
      return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
    };

    // First: Schedule active habits with designated reminder times
    for (const habit of activeHabits) {
      const duration = habit.trackingType === 'DURATION' ? Math.min(habit.targetValue, 60) : 15;
      let hStart = currentMinuteOfDay;
      if (habit.reminderTime) {
        const [rh, rm] = habit.reminderTime.split(':').map(Number);
        hStart = rh * 60 + rm;
      }

      if (hStart + duration <= dayEndMinute) {
        planItems.push({
          id: `plan_item_h_${habit.id}_${Date.now()}`,
          planId: `plan_${date}`,
          itemType: 'HABIT',
          referenceId: habit.id,
          title: habit.title,
          startTime: formatTime(hStart),
          endTime: formatTime(hStart + duration),
          durationMinutes: duration,
          isCompleted: false,
          sortOrder: sortOrder++,
        });
      }
    }

    // Schedule tasks into remaining slots
    for (const { task } of scoredTasks) {
      const duration = task.estimatedDurationMinutes || settings.defaultTaskDurationMinutes || 30;

      if (currentMinuteOfDay + duration <= dayEndMinute) {
        const startTimeStr = formatTime(currentMinuteOfDay);
        const endTimeStr = formatTime(currentMinuteOfDay + duration);

        planItems.push({
          id: `plan_item_t_${task.id}_${Date.now()}`,
          planId: `plan_${date}`,
          itemType: 'TASK',
          referenceId: task.id,
          title: task.title,
          startTime: startTimeStr,
          endTime: endTimeStr,
          durationMinutes: duration,
          isCompleted: false,
          sortOrder: sortOrder++,
        });

        currentMinuteOfDay += duration + this.BUFFER_MINUTES;
      } else {
        // Out of available time window — add to unscheduled (never dropped!)
        unscheduledTasks.push(task);
      }
    }

    if (unscheduledTasks.length > 0) {
      warnings.push(`${unscheduledTasks.length} task(s) could not fit in today's available hours (${settings.dayStartTime} - ${settings.dayEndTime}). Consider carrying forward or adjusting durations.`);
    }

    // Sort plan items chronologically
    planItems.sort((a, b) => a.startTime.localeCompare(b.startTime));

    return {
      id: `plan_${date}`,
      date,
      status: 'DRAFT',
      items: planItems,
      unscheduledTasks,
      warnings,
      generatedAt: new Date().toISOString(),
      approvedAt: null,
    };
  }
}
