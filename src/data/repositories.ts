import { db } from './db';
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
import { DEFAULT_SETTINGS } from './seed';

export const CategoryRepository = {
  async getAll(): Promise<Category[]> {
    return db.categories.orderBy('sortOrder').toArray();
  },
  async getById(id: string): Promise<Category | undefined> {
    return db.categories.get(id);
  },
  async add(category: Category): Promise<string> {
    return db.categories.add(category);
  },
  async update(category: Category): Promise<string> {
    return db.categories.put(category);
  },
  async delete(id: string): Promise<void> {
    await db.categories.delete(id);
  }
};

export const TaskRepository = {
  async getAll(): Promise<Task[]> {
    const tasks = await db.tasks.toArray();
    const subtasks = await db.subtasks.toArray();

    return tasks.map(task => ({
      ...task,
      subtasks: subtasks.filter(s => s.taskId === task.id).sort((a, b) => a.sortOrder - b.sortOrder),
    }));
  },

  async getById(id: string): Promise<Task | undefined> {
    const task = await db.tasks.get(id);
    if (!task) return undefined;
    const subtasks = await db.subtasks.where('taskId').equals(id).sortBy('sortOrder');
    return { ...task, subtasks };
  },

  async add(task: Task, subtasks: Subtask[] = []): Promise<void> {
    await db.transaction('rw', db.tasks, db.subtasks, async () => {
      await db.tasks.add(task);
      if (subtasks.length > 0) {
        await db.subtasks.bulkAdd(subtasks);
      }
    });
  },

  async update(task: Task, subtasks?: Subtask[]): Promise<void> {
    await db.transaction('rw', db.tasks, db.subtasks, async () => {
      await db.tasks.put({
        ...task,
        updatedAt: new Date().toISOString()
      });
      if (subtasks !== undefined) {
        await db.subtasks.where('taskId').equals(task.id).delete();
        if (subtasks.length > 0) {
          await db.subtasks.bulkAdd(subtasks);
        }
      }
    });
  },

  async toggleStatus(taskId: string): Promise<void> {
    const task = await db.tasks.get(taskId);
    if (!task) return;
    const nextStatus = task.status === 'COMPLETED' ? 'PENDING' : 'COMPLETED';
    const completedAt = nextStatus === 'COMPLETED' ? new Date().toISOString() : null;
    await db.tasks.update(taskId, {
      status: nextStatus,
      completedAt,
      updatedAt: new Date().toISOString(),
    });
  },

  async delete(taskId: string): Promise<void> {
    await db.transaction('rw', db.tasks, db.subtasks, async () => {
      await db.subtasks.where('taskId').equals(taskId).delete();
      await db.tasks.delete(taskId);
    });
  },

  async toggleSubtask(subtaskId: string, isCompleted: boolean): Promise<void> {
    await db.subtasks.update(subtaskId, { isCompleted });
  }
};

export const HabitRepository = {
  async getAll(): Promise<Habit[]> {
    return db.habits.filter(h => !h.archived).toArray();
  },

  async getById(id: string): Promise<Habit | undefined> {
    return db.habits.get(id);
  },

  async add(habit: Habit): Promise<void> {
    await db.habits.add(habit);
  },

  async update(habit: Habit): Promise<void> {
    await db.habits.put({
      ...habit,
      updatedAt: new Date().toISOString()
    });
  },

  async archive(id: string): Promise<void> {
    await db.habits.update(id, { archived: true, updatedAt: new Date().toISOString() });
  },

  async getAllLogs(): Promise<HabitLog[]> {
    return db.habitLogs.toArray();
  },

  async getLogsForHabit(habitId: string): Promise<HabitLog[]> {
    return db.habitLogs.where('habitId').equals(habitId).toArray();
  },

  async getLogsForDate(date: string): Promise<HabitLog[]> {
    return db.habitLogs.where('date').equals(date).toArray();
  },

  async logHabit(habitId: string, date: string, value: number, isCompleted: boolean, notes?: string): Promise<void> {
    const existing = await db.habitLogs.where('[habitId+date]').equals([habitId, date]).first();
    if (existing) {
      await db.habitLogs.update(existing.id, {
        value,
        isCompleted,
        loggedAt: new Date().toISOString(),
        notes: notes ?? existing.notes,
      });
    } else {
      await db.habitLogs.add({
        id: `hlog_${habitId}_${date}_${Date.now()}`,
        habitId,
        date,
        value,
        isCompleted,
        loggedAt: new Date().toISOString(),
        notes,
      });
    }
  }
};

export const ReminderRepository = {
  async getAll(): Promise<Reminder[]> {
    return db.reminders.toArray();
  },

  async getById(id: string): Promise<Reminder | undefined> {
    return db.reminders.get(id);
  },

  async add(reminder: Reminder): Promise<void> {
    await db.reminders.add(reminder);
  },

  async update(reminder: Reminder): Promise<void> {
    await db.reminders.put({
      ...reminder,
      updatedAt: new Date().toISOString()
    });
  },

  async togglePause(id: string): Promise<void> {
    const r = await db.reminders.get(id);
    if (r) {
      await db.reminders.update(id, { isPaused: !r.isPaused, updatedAt: new Date().toISOString() });
    }
  },

  async delete(id: string): Promise<void> {
    await db.transaction('rw', db.reminders, db.occurrences, async () => {
      await db.occurrences.where('reminderId').equals(id).delete();
      await db.reminders.delete(id);
    });
  },

  async recordOccurrence(occurrence: ReminderOccurrence): Promise<void> {
    await db.occurrences.put(occurrence);
  },

  async getOccurrences(reminderId?: string): Promise<ReminderOccurrence[]> {
    if (reminderId) {
      return db.occurrences.where('reminderId').equals(reminderId).toArray();
    }
    return db.occurrences.toArray();
  }
};

export const PlanRepository = {
  async getPlanForDate(date: string): Promise<DailyPlan | undefined> {
    return db.plans.get(`plan_${date}`);
  },

  async savePlan(plan: DailyPlan): Promise<void> {
    await db.plans.put(plan);
  },

  async approvePlan(date: string): Promise<void> {
    const plan = await db.plans.get(`plan_${date}`);
    if (plan) {
      plan.status = 'APPROVED';
      plan.approvedAt = new Date().toISOString();
      await db.plans.put(plan);
    }
  }
};

export const ReviewRepository = {
  async getReviewForDate(date: string): Promise<DailyReview | undefined> {
    return db.reviews.get(`review_${date}`);
  },

  async saveReview(review: DailyReview): Promise<void> {
    await db.reviews.put(review);
  },

  async getAllReviews(): Promise<DailyReview[]> {
    return db.reviews.toArray();
  }
};

export const SettingsRepository = {
  async getSettings(): Promise<UserSettings> {
    const s = await db.settings.get('current');
    if (!s) {
      await db.settings.add(DEFAULT_SETTINGS);
      return DEFAULT_SETTINGS;
    }
    return s;
  },

  async updateSettings(settings: Partial<UserSettings>): Promise<UserSettings> {
    const current = await this.getSettings();
    const updated = { ...current, ...settings, id: 'current' };
    await db.settings.put(updated);
    return updated;
  }
};

export const BackupRepository = {
  async exportData(): Promise<string> {
    const dump = {
      schemaVersion: 1,
      appVersion: '1.0.0',
      exportedAt: new Date().toISOString(),
      categories: await db.categories.toArray(),
      tasks: await db.tasks.toArray(),
      subtasks: await db.subtasks.toArray(),
      habits: await db.habits.toArray(),
      habitLogs: await db.habitLogs.toArray(),
      reminders: await db.reminders.toArray(),
      occurrences: await db.occurrences.toArray(),
      plans: await db.plans.toArray(),
      reviews: await db.reviews.toArray(),
      settings: await db.settings.toArray(),
    };

    return JSON.stringify(dump, null, 2);
  },

  async importData(jsonString: string): Promise<boolean> {
    try {
      const data = JSON.parse(jsonString);
      if (!data.schemaVersion || data.schemaVersion > 1) {
        throw new Error('Unsupported schema version');
      }

      await db.transaction('rw', [
        db.categories,
        db.tasks,
        db.subtasks,
        db.habits,
        db.habitLogs,
        db.reminders,
        db.occurrences,
        db.plans,
        db.reviews,
        db.settings
      ], async () => {
        if (data.categories) {
          await db.categories.clear();
          await db.categories.bulkAdd(data.categories);
        }
        if (data.tasks) {
          await db.tasks.clear();
          await db.tasks.bulkAdd(data.tasks);
        }
        if (data.subtasks) {
          await db.subtasks.clear();
          await db.subtasks.bulkAdd(data.subtasks);
        }
        if (data.habits) {
          await db.habits.clear();
          await db.habits.bulkAdd(data.habits);
        }
        if (data.habitLogs) {
          await db.habitLogs.clear();
          await db.habitLogs.bulkAdd(data.habitLogs);
        }
        if (data.reminders) {
          await db.reminders.clear();
          await db.reminders.bulkAdd(data.reminders);
        }
        if (data.occurrences) {
          await db.occurrences.clear();
          await db.occurrences.bulkAdd(data.occurrences);
        }
        if (data.plans) {
          await db.plans.clear();
          await db.plans.bulkAdd(data.plans);
        }
        if (data.reviews) {
          await db.reviews.clear();
          await db.reviews.bulkAdd(data.reviews);
        }
        if (data.settings) {
          await db.settings.clear();
          await db.settings.bulkAdd(data.settings);
        }
      });
      return true;
    } catch (e) {
      console.error('Failed to import backup data:', e);
      return false;
    }
  }
};
