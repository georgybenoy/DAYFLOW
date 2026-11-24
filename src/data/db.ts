import Dexie, { Table } from 'dexie';
import { 
  Category, 
  Task, 
  Subtask, 
  Habit, 
  HabitLog, 
  Reminder, 
  ReminderOccurrence, 
  DailyPlan, 
  DailyReview, 
  UserSettings 
} from '../domain/models';

export class DayFlowDatabase extends Dexie {
  categories!: Table<Category, string>;
  tasks!: Table<Task, string>;
  subtasks!: Table<Subtask, string>;
  habits!: Table<Habit, string>;
  habitLogs!: Table<HabitLog, string>;
  reminders!: Table<Reminder, string>;
  occurrences!: Table<ReminderOccurrence, string>;
  plans!: Table<DailyPlan, string>;
  reviews!: Table<DailyReview, string>;
  settings!: Table<UserSettings, string>;

  constructor() {
    super('DayFlowDB');

    this.version(1).stores({
      categories: 'id, name, sortOrder',
      tasks: 'id, categoryId, priority, status, deadlineDate, plannedDate, createdAt',
      subtasks: 'id, taskId, sortOrder',
      habits: 'id, categoryId, trackingType, archived, createdAt',
      habitLogs: 'id, habitId, date, [habitId+date]',
      reminders: 'id, categoryId, type, isPaused, createdAt',
      occurrences: 'id, reminderId, scheduledDateTime, status',
      plans: 'id, date, status',
      reviews: 'id, date',
      settings: 'id',
    });
  }
}

export const db = new DayFlowDatabase();
