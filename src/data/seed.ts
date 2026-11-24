import { db } from './db';
import { Category, UserSettings, Task, Habit, Reminder } from '../domain/models';

export const DEFAULT_CATEGORIES: Category[] = [
  { id: 'cat_work', name: 'Work', color: '#F5C6A5', icon: 'Briefcase', isDefault: true, sortOrder: 0 },
  { id: 'cat_personal', name: 'Personal', color: '#DCC3F5', icon: 'User', isDefault: true, sortOrder: 1 },
  { id: 'cat_fitness', name: 'Fitness & Health', color: '#B8EBD0', icon: 'Activity', isDefault: true, sortOrder: 2 },
  { id: 'cat_academics', name: 'Academics & Learning', color: '#A5E6D6', icon: 'BookOpen', isDefault: true, sortOrder: 3 },
  { id: 'cat_finance', name: 'Finance', color: '#F0E99B', icon: 'DollarSign', isDefault: true, sortOrder: 4 },
  { id: 'cat_projects', name: 'Projects', color: '#B5D6FD', icon: 'FolderGit2', isDefault: true, sortOrder: 5 },
  { id: 'cat_other', name: 'Other', color: '#CBD5E1', icon: 'Tag', isDefault: true, sortOrder: 6 },
];

export const DEFAULT_SETTINGS: UserSettings = {
  id: 'current',
  theme: 'dark',
  weekStartDay: 1, // Monday
  dateFormat: 'YYYY-MM-DD',
  timeFormat: '24h',
  morningBriefingTime: '07:00',
  eveningReviewTime: '21:30',
  dayStartTime: '08:00',
  dayEndTime: '22:00',
  defaultTaskDurationMinutes: 30,
  soundEnabled: true,
  vibrationEnabled: true,
  exactAlarmsEnabled: true,
};

export async function seedInitialDatabaseIfEmpty() {
  const catCount = await db.categories.count();
  if (catCount === 0) {
    await db.categories.bulkAdd(DEFAULT_CATEGORIES);
  }

  const settingsCount = await db.settings.count();
  if (settingsCount === 0) {
    await db.settings.add(DEFAULT_SETTINGS);
  }

  // Seed helpful starter items if database is totally fresh
  const taskCount = await db.tasks.count();
  if (taskCount === 0) {
    const today = new Date().toISOString().split('T')[0];
    const starterTasks: Task[] = [
      {
        id: 'task_starter_1',
        title: 'Complete DAYFLOW quick orientation',
        description: 'Explore the 5 core tabs: Today, Reminders, Tasks, Habits, and Insights.',
        categoryId: 'cat_personal',
        priority: 'HIGH',
        deadlineDate: today,
        deadlineTime: '18:00',
        estimatedDurationMinutes: 15,
        plannedDate: today,
        plannedStartTime: '09:00',
        status: 'PENDING',
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      },
      {
        id: 'task_starter_2',
        title: 'Review weekly execution commitments',
        description: 'Set priority tasks and define deadlines for the upcoming week.',
        categoryId: 'cat_work',
        priority: 'URGENT',
        deadlineDate: today,
        deadlineTime: '20:00',
        estimatedDurationMinutes: 30,
        plannedDate: today,
        plannedStartTime: '11:00',
        status: 'PENDING',
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      },
      {
        id: 'task_starter_3',
        title: 'Organize project repository',
        description: 'Review commit logs and clean up stale local branches.',
        categoryId: 'cat_projects',
        priority: 'MEDIUM',
        deadlineDate: null,
        deadlineTime: null,
        estimatedDurationMinutes: 45,
        plannedDate: null,
        plannedStartTime: null,
        status: 'PENDING',
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      }
    ];
    await db.tasks.bulkAdd(starterTasks);

    const starterHabits: Habit[] = [
      {
        id: 'habit_starter_1',
        title: 'Morning hydration (2L)',
        description: 'Drink a glass of water right after waking up.',
        categoryId: 'cat_fitness',
        trackingType: 'QUANTITY',
        targetValue: 2000,
        unit: 'ml',
        scheduledDays: [1, 2, 3, 4, 5, 6, 7],
        reminderTime: '07:30',
        reminderEnabled: true,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      },
      {
        id: 'habit_starter_2',
        title: 'Daily deep focus reading',
        description: 'Read 20 pages of non-fiction or technical book.',
        categoryId: 'cat_academics',
        trackingType: 'DURATION',
        targetValue: 30,
        unit: 'mins',
        scheduledDays: [1, 2, 3, 4, 5],
        reminderTime: '20:00',
        reminderEnabled: true,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      },
      {
        id: 'habit_starter_3',
        title: 'Evening wind-down & reflection',
        description: 'No screens 30 minutes before sleep.',
        categoryId: 'cat_personal',
        trackingType: 'BINARY',
        targetValue: 1,
        scheduledDays: [1, 2, 3, 4, 5, 6, 7],
        reminderTime: '22:00',
        reminderEnabled: false,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      }
    ];
    await db.habits.bulkAdd(starterHabits);

    const starterReminders: Reminder[] = [
      {
        id: 'rem_starter_1',
        title: 'Team sync and standup briefing',
        notes: 'Review blocked items and sprint progress.',
        type: 'DAILY',
        categoryId: 'cat_work',
        targetDate: null,
        targetTime: '10:00',
        scheduledDays: [1, 2, 3, 4, 5],
        intervalMinutes: null,
        isBlocking: true,
        isPaused: false,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      },
      {
        id: 'rem_starter_2',
        title: 'Take a postural break & stretch',
        notes: 'Stand up and stretch for 2 minutes.',
        type: 'FIXED_INTERVAL',
        categoryId: 'cat_fitness',
        targetDate: null,
        targetTime: null,
        scheduledDays: [],
        intervalMinutes: 60,
        isBlocking: false,
        isPaused: false,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      }
    ];
    await db.reminders.bulkAdd(starterReminders);
  }
}
