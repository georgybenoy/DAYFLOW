import { create } from 'zustand';
import { 
  Category, 
  Task, 
  Subtask, 
  Habit, 
  HabitLog, 
  Reminder, 
  DailyPlan, 
  DailyReview, 
  UserSettings 
} from '../domain/models';
import { 
  CategoryRepository, 
  TaskRepository, 
  HabitRepository, 
  ReminderRepository, 
  PlanRepository, 
  ReviewRepository, 
  SettingsRepository, 
  BackupRepository 
} from '../data/repositories';
import { seedInitialDatabaseIfEmpty, DEFAULT_SETTINGS } from '../data/seed';
import { DeterministicPlanner } from '../domain/planner';

export type ScreenType = 
  | 'today' 
  | 'reminders' 
  | 'tasks' 
  | 'habits' 
  | 'insights' 
  | 'calendar' 
  | 'settings' 
  | 'review' 
  | 'briefing' 
  | 'plan';

interface DayFlowState {
  selectedDate: string; // YYYY-MM-DD
  activeScreen: ScreenType;
  categories: Category[];
  tasks: Task[];
  habits: Habit[];
  habitLogs: HabitLog[];
  reminders: Reminder[];
  dailyPlan: DailyPlan | null;
  dailyReview: DailyReview | null;
  settings: UserSettings;
  isLoading: boolean;

  // Navigation
  setSelectedDate: (date: string) => Promise<void>;
  setActiveScreen: (screen: ScreenType) => void;

  // Data Loading
  initApp: () => Promise<void>;
  refreshAll: () => Promise<void>;

  // Tasks
  createTask: (task: Omit<Task, 'id' | 'createdAt' | 'updatedAt'>, subtasks?: string[]) => Promise<void>;
  updateTask: (task: Task, subtasks?: Subtask[]) => Promise<void>;
  toggleTask: (taskId: string) => Promise<void>;
  deleteTask: (taskId: string) => Promise<void>;
  toggleSubtask: (subtaskId: string, isCompleted: boolean) => Promise<void>;

  // Habits
  createHabit: (habit: Omit<Habit, 'id' | 'createdAt' | 'updatedAt'>) => Promise<void>;
  updateHabit: (habit: Habit) => Promise<void>;
  archiveHabit: (habitId: string) => Promise<void>;
  logHabit: (habitId: string, date: string, value: number, isCompleted: boolean, notes?: string) => Promise<void>;

  // Reminders
  createReminder: (reminder: Omit<Reminder, 'id' | 'createdAt' | 'updatedAt'>) => Promise<void>;
  updateReminder: (reminder: Reminder) => Promise<void>;
  toggleReminderPause: (reminderId: string) => Promise<void>;
  deleteReminder: (reminderId: string) => Promise<void>;

  // Planner
  generateDailyPlan: (date?: string) => Promise<DailyPlan>;
  approveDailyPlan: (date: string) => Promise<void>;
  togglePlanItemCompleted: (itemId: string) => Promise<void>;

  // Review
  saveDailyReview: (review: DailyReview) => Promise<void>;

  // Settings & Backup
  updateSettings: (settings: Partial<UserSettings>) => Promise<void>;
  exportBackup: () => Promise<string>;
  importBackup: (json: string) => Promise<boolean>;
}

export const useDayFlowStore = create<DayFlowState>((set, get) => ({
  selectedDate: new Date().toISOString().split('T')[0],
  activeScreen: 'today',
  categories: [],
  tasks: [],
  habits: [],
  habitLogs: [],
  reminders: [],
  dailyPlan: null,
  dailyReview: null,
  settings: DEFAULT_SETTINGS,
  isLoading: true,

  setSelectedDate: async (date: string) => {
    set({ selectedDate: date });
    const plan = await PlanRepository.getPlanForDate(date);
    const review = await ReviewRepository.getReviewForDate(date);
    set({ dailyPlan: plan || null, dailyReview: review || null });
  },

  setActiveScreen: (screen: ScreenType) => {
    set({ activeScreen: screen });
  },

  initApp: async () => {
    set({ isLoading: true });
    await seedInitialDatabaseIfEmpty();
    await get().refreshAll();
    set({ isLoading: false });
  },

  refreshAll: async () => {
    const selectedDate = get().selectedDate;
    const [categories, tasks, habits, habitLogs, reminders, settings, plan, review] = await Promise.all([
      CategoryRepository.getAll(),
      TaskRepository.getAll(),
      HabitRepository.getAll(),
      HabitRepository.getAllLogs(),
      ReminderRepository.getAll(),
      SettingsRepository.getSettings(),
      PlanRepository.getPlanForDate(selectedDate),
      ReviewRepository.getReviewForDate(selectedDate),
    ]);

    set({
      categories,
      tasks,
      habits,
      habitLogs,
      reminders,
      settings,
      dailyPlan: plan || null,
      dailyReview: review || null,
    });
  },

  createTask: async (taskInput, subtaskTitles = []) => {
    const now = new Date().toISOString();
    const taskId = `task_${Date.now()}`;
    const newTask: Task = {
      ...taskInput,
      id: taskId,
      createdAt: now,
      updatedAt: now,
    };

    const subtasks: Subtask[] = subtaskTitles.map((title, idx) => ({
      id: `subtask_${taskId}_${idx}`,
      taskId,
      title,
      isCompleted: false,
      sortOrder: idx,
    }));

    await TaskRepository.add(newTask, subtasks);
    await get().refreshAll();
  },

  updateTask: async (task, subtasks) => {
    await TaskRepository.update(task, subtasks);
    await get().refreshAll();
  },

  toggleTask: async (taskId: string) => {
    await TaskRepository.toggleStatus(taskId);
    await get().refreshAll();
  },

  deleteTask: async (taskId: string) => {
    await TaskRepository.delete(taskId);
    await get().refreshAll();
  },

  toggleSubtask: async (subtaskId: string, isCompleted: boolean) => {
    await TaskRepository.toggleSubtask(subtaskId, isCompleted);
    await get().refreshAll();
  },

  createHabit: async (habitInput) => {
    const now = new Date().toISOString();
    const newHabit: Habit = {
      ...habitInput,
      id: `habit_${Date.now()}`,
      createdAt: now,
      updatedAt: now,
    };
    await HabitRepository.add(newHabit);
    await get().refreshAll();
  },

  updateHabit: async (habit) => {
    await HabitRepository.update(habit);
    await get().refreshAll();
  },

  archiveHabit: async (habitId: string) => {
    await HabitRepository.archive(habitId);
    await get().refreshAll();
  },

  logHabit: async (habitId, date, value, isCompleted, notes) => {
    await HabitRepository.logHabit(habitId, date, value, isCompleted, notes);
    await get().refreshAll();
  },

  createReminder: async (reminderInput) => {
    const now = new Date().toISOString();
    const newReminder: Reminder = {
      ...reminderInput,
      id: `rem_${Date.now()}`,
      createdAt: now,
      updatedAt: now,
    };
    await ReminderRepository.add(newReminder);
    await get().refreshAll();
  },

  updateReminder: async (reminder) => {
    await ReminderRepository.update(reminder);
    await get().refreshAll();
  },

  toggleReminderPause: async (reminderId: string) => {
    await ReminderRepository.togglePause(reminderId);
    await get().refreshAll();
  },

  deleteReminder: async (reminderId: string) => {
    await ReminderRepository.delete(reminderId);
    await get().refreshAll();
  },

  generateDailyPlan: async (targetDate) => {
    const date = targetDate || get().selectedDate;
    const { tasks, habits, reminders, settings } = get();
    const previousReviews = await ReviewRepository.getAllReviews();

    const plan = DeterministicPlanner.generatePlan({
      date,
      tasks,
      habits,
      reminders,
      settings,
      previousReviews,
    });

    await PlanRepository.savePlan(plan);
    set({ dailyPlan: plan });
    return plan;
  },

  approveDailyPlan: async (date: string) => {
    await PlanRepository.approvePlan(date);
    const updated = await PlanRepository.getPlanForDate(date);
    set({ dailyPlan: updated || null });
  },

  togglePlanItemCompleted: async (itemId: string) => {
    const plan = get().dailyPlan;
    if (!plan) return;

    const updatedItems = plan.items.map(item => {
      if (item.id === itemId) {
        return { ...item, isCompleted: !item.isCompleted };
      }
      return item;
    });

    const updatedPlan: DailyPlan = { ...plan, items: updatedItems };
    await PlanRepository.savePlan(updatedPlan);
    set({ dailyPlan: updatedPlan });

    // Also toggle the underlying task if this item is a TASK
    const item = plan.items.find(i => i.id === itemId);
    if (item && item.itemType === 'TASK') {
      await TaskRepository.toggleStatus(item.referenceId);
      await get().refreshAll();
    }
  },

  saveDailyReview: async (review: DailyReview) => {
    await ReviewRepository.saveReview(review);
    set({ dailyReview: review });
  },

  updateSettings: async (settingsUpdate) => {
    const updated = await SettingsRepository.updateSettings(settingsUpdate);
    set({ settings: updated });
  },

  exportBackup: async () => {
    return BackupRepository.exportData();
  },

  importBackup: async (json: string) => {
    const success = await BackupRepository.importData(json);
    if (success) {
      await get().refreshAll();
    }
    return success;
  },
}));
