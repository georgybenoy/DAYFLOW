import { describe, it, expect } from 'vitest';
import { RecurrenceCalculator } from './recurrence';
import { StreakCalculator } from './streak';
import { DeterministicPlanner } from './planner';
import { BriefingGenerator } from './briefing';
import { Habit, HabitLog, Reminder, Task, UserSettings } from './models';

describe('RecurrenceCalculator', () => {
  it('calculates next daily occurrence correctly', () => {
    const reminder: Reminder = {
      id: 'rem_1',
      title: 'Daily Standup',
      type: 'DAILY',
      categoryId: 'cat_work',
      targetDate: null,
      targetTime: '10:00',
      scheduledDays: [],
      isBlocking: false,
      isPaused: false,
      createdAt: '2026-11-23T08:00:00Z',
      updatedAt: '2026-11-23T08:00:00Z',
    };

    // Before 10:00 on Nov 23
    const fromMorning = new Date('2026-11-23T08:30:00');
    const occ = RecurrenceCalculator.getNextOccurrence(reminder, fromMorning);
    expect(occ).not.toBeNull();
    expect(occ?.date).toBe('2026-11-23');
    expect(occ?.time).toBe('10:00');

    // After 10:00 on Nov 23 -> pushes to Nov 24
    const fromAfternoon = new Date('2026-11-23T11:00:00');
    const nextOcc = RecurrenceCalculator.getNextOccurrence(reminder, fromAfternoon);
    expect(nextOcc).not.toBeNull();
    expect(nextOcc?.date).toBe('2026-11-24');
    expect(nextOcc?.time).toBe('10:00');
  });

  it('calculates next 5 occurrences without duplicating', () => {
    const reminder: Reminder = {
      id: 'rem_2',
      title: 'Water Break',
      type: 'DAILY',
      categoryId: 'cat_fitness',
      targetTime: '14:00',
      isBlocking: false,
      isPaused: false,
      createdAt: '2026-11-23T08:00:00Z',
      updatedAt: '2026-11-23T08:00:00Z',
    };

    const fromDate = new Date('2026-11-23T08:00:00');
    const occurrences = RecurrenceCalculator.getNextOccurrences(reminder, 5, fromDate);
    expect(occurrences).toHaveLength(5);
    expect(occurrences[0].date).toBe('2026-11-23');
    expect(occurrences[1].date).toBe('2026-11-24');
    expect(occurrences[4].date).toBe('2026-11-27');
  });
});

describe('StreakCalculator', () => {
  const habit: Habit = {
    id: 'habit_1',
    title: 'Weekday Workout',
    categoryId: 'cat_fitness',
    trackingType: 'BINARY',
    targetValue: 1,
    scheduledDays: [1, 2, 3, 4, 5], // Mon-Fri only (weekends inactive)
    reminderEnabled: false,
    createdAt: '2026-11-16T00:00:00Z', // Monday
    updatedAt: '2026-11-16T00:00:00Z',
  };

  it('preserves streak across inactive weekend days', () => {
    // Logs for Mon (16), Tue (17), Wed (18), Thu (19), Fri (20)
    const logs: HabitLog[] = [
      { id: '1', habitId: 'habit_1', date: '2026-11-16', value: 1, isCompleted: true, loggedAt: '' },
      { id: '2', habitId: 'habit_1', date: '2026-11-17', value: 1, isCompleted: true, loggedAt: '' },
      { id: '3', habitId: 'habit_1', date: '2026-11-18', value: 1, isCompleted: true, loggedAt: '' },
      { id: '4', habitId: 'habit_1', date: '2026-11-19', value: 1, isCompleted: true, loggedAt: '' },
      { id: '5', habitId: 'habit_1', date: '2026-11-20', value: 1, isCompleted: true, loggedAt: '' },
    ];

    // Check on Monday 2026-11-23 (where Sat 21 and Sun 22 were inactive)
    // Today (Mon 23) not done yet -> streak from last scheduled day (Fri 20) should still be 5
    const result = StreakCalculator.calculate(habit, logs, '2026-11-23');
    expect(result.currentStreak).toBe(5);
    expect(result.longestStreak).toBe(5);
  });

  it('increments streak when today is completed', () => {
    const logs: HabitLog[] = [
      { id: '1', habitId: 'habit_1', date: '2026-11-20', value: 1, isCompleted: true, loggedAt: '' },
      { id: '2', habitId: 'habit_1', date: '2026-11-23', value: 1, isCompleted: true, loggedAt: '' },
    ];

    const result = StreakCalculator.calculate(habit, logs, '2026-11-23');
    expect(result.currentStreak).toBe(2);
  });
});

describe('DeterministicPlanner', () => {
  const dummySettings: UserSettings = {
    id: 'current',
    theme: 'dark',
    weekStartDay: 1,
    dateFormat: 'YYYY-MM-DD',
    timeFormat: '24h',
    morningBriefingTime: '07:00',
    eveningReviewTime: '21:30',
    dayStartTime: '09:00',
    dayEndTime: '12:00', // Short window: 3 hours = 180 mins
    defaultTaskDurationMinutes: 30,
    soundEnabled: true,
    vibrationEnabled: true,
    exactAlarmsEnabled: true,
  };

  it('prioritizes URGENT tasks and retains unscheduled tasks without dropping', () => {
    const tasks: Task[] = [
      {
        id: 't_low',
        title: 'Low Priority Clean Up',
        categoryId: 'cat_work',
        priority: 'LOW',
        estimatedDurationMinutes: 60,
        status: 'PENDING',
        createdAt: '',
        updatedAt: '',
      },
      {
        id: 't_urgent',
        title: 'Urgent Client Deliverable',
        categoryId: 'cat_work',
        priority: 'URGENT',
        deadlineDate: '2026-11-23',
        estimatedDurationMinutes: 90,
        status: 'PENDING',
        createdAt: '',
        updatedAt: '',
      },
      {
        id: 't_high',
        title: 'High Priority Refactor',
        categoryId: 'cat_work',
        priority: 'HIGH',
        estimatedDurationMinutes: 60,
        status: 'PENDING',
        createdAt: '',
        updatedAt: '',
      },
    ];

    const plan = DeterministicPlanner.generatePlan({
      date: '2026-11-23',
      tasks,
      habits: [],
      reminders: [],
      settings: dummySettings,
    });

    expect(plan.status).toBe('DRAFT');
    // First scheduled item should be the urgent task
    const scheduledTaskItems = plan.items.filter(i => i.itemType === 'TASK');
    expect(scheduledTaskItems[0].referenceId).toBe('t_urgent');
    // The low priority task could not fit in the 180 min window (90m + 10m buffer + 60m + 10m buffer = 170m)
    expect(plan.unscheduledTasks.length).toBeGreaterThan(0);
    expect(plan.unscheduledTasks.some(t => t.id === 't_low')).toBe(true);
    // Never modifies task deadline
    expect(tasks[1].deadlineDate).toBe('2026-11-23');
  });
});

describe('BriefingGenerator', () => {
  it('generates accurate briefing text and metrics from stored records', () => {
    const briefing = BriefingGenerator.generate({
      date: '2026-11-23',
      tasks: [
        {
          id: 't_1',
          title: 'Review Project',
          categoryId: 'cat_projects',
          priority: 'URGENT',
          deadlineDate: '2026-11-23',
          estimatedDurationMinutes: 30,
          status: 'PENDING',
          createdAt: '',
          updatedAt: '',
        }
      ],
      habits: [],
      habitLogs: [],
      reminders: [],
    });

    expect(briefing.urgentTasksCount).toBe(1);
    expect(briefing.todayDeadlinesCount).toBe(1);
    expect(briefing.topTasks[0].title).toBe('Review Project');
    expect(briefing.summaryText).toContain('1 pending task');
    expect(briefing.summaryText).toContain('1 deadline due today');
  });
});
