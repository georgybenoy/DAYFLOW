import { Task, Habit, Reminder, HabitLog } from './models';

export interface BriefingData {
  greeting: string;
  dateStr: string;
  urgentTasksCount: number;
  todayDeadlinesCount: number;
  habitsScheduledCount: number;
  habitsCompletedCount: number;
  remindersCount: number;
  topTasks: Task[];
  summaryText: string;
}

export class BriefingGenerator {
  static generate(params: {
    date: string;
    tasks: Task[];
    habits: Habit[];
    habitLogs: HabitLog[];
    reminders: Reminder[];
  }): BriefingData {
    const { date, tasks, habits, habitLogs, reminders } = params;

    const hour = new Date().getHours();
    let greeting = 'Good morning';
    if (hour >= 12 && hour < 17) {
      greeting = 'Good afternoon';
    } else if (hour >= 17) {
      greeting = 'Good evening';
    }

    const todayDateObj = new Date(date);
    const jsDay = todayDateObj.getDay();
    const dayNum = jsDay === 0 ? 7 : jsDay;

    // Active habits today
    const scheduledHabits = habits.filter(h => !h.archived && h.scheduledDays.includes(dayNum));
    const completedHabitIds = new Set(
      habitLogs.filter(l => l.date === date && l.isCompleted).map(l => l.habitId)
    );
    const habitsCompletedCount = scheduledHabits.filter(h => completedHabitIds.has(h.id)).length;

    // Tasks
    const pendingTasks = tasks.filter(t => t.status === 'PENDING');
    const urgentTasks = pendingTasks.filter(t => t.priority === 'URGENT');
    const todayDeadlines = pendingTasks.filter(t => t.deadlineDate === date);

    // Active reminders
    const activeReminders = reminders.filter(r => !r.isPaused);

    // Top 3 priority tasks
    const topTasks = [...pendingTasks].sort((a, b) => {
      const pWeights: Record<string, number> = { URGENT: 4, HIGH: 3, MEDIUM: 2, LOW: 1 };
      return (pWeights[b.priority] || 0) - (pWeights[a.priority] || 0);
    }).slice(0, 3);

    // Humanized summary text
    let summaryText = `You have ${pendingTasks.length} pending task${pendingTasks.length === 1 ? '' : 's'}`;
    if (todayDeadlines.length > 0) {
      summaryText += `, with ${todayDeadlines.length} deadline${todayDeadlines.length === 1 ? '' : 's'} due today.`;
    } else {
      summaryText += ` scheduled.`;
    }
    if (scheduledHabits.length > 0) {
      summaryText += ` ${scheduledHabits.length} habit${scheduledHabits.length === 1 ? '' : 's'} to track.`;
    }

    return {
      greeting,
      dateStr: date,
      urgentTasksCount: urgentTasks.length,
      todayDeadlinesCount: todayDeadlines.length,
      habitsScheduledCount: scheduledHabits.length,
      habitsCompletedCount,
      remindersCount: activeReminders.length,
      topTasks,
      summaryText,
    };
  }
}
