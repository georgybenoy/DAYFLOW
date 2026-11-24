import { z } from 'zod';

export type Priority = 'URGENT' | 'HIGH' | 'MEDIUM' | 'LOW';
export type TaskStatus = 'PENDING' | 'COMPLETED' | 'CANCELLED';

export interface Category {
  id: string;
  name: string;
  color: string;
  icon?: string;
  isDefault?: boolean;
  sortOrder: number;
}

export interface Subtask {
  id: string;
  taskId: string;
  title: string;
  isCompleted: boolean;
  sortOrder: number;
}

export interface Task {
  id: string;
  title: string;
  description?: string;
  categoryId: string;
  priority: Priority;
  deadlineDate?: string | null; // YYYY-MM-DD
  deadlineTime?: string | null; // HH:mm
  estimatedDurationMinutes: number; // default 30
  plannedDate?: string | null; // YYYY-MM-DD
  plannedStartTime?: string | null; // HH:mm
  status: TaskStatus;
  completedAt?: string | null;
  createdAt: string;
  updatedAt: string;
  subtasks?: Subtask[];
}

export type HabitTrackingType = 'BINARY' | 'QUANTITY' | 'DURATION' | 'REPETITION';

export interface Habit {
  id: string;
  title: string;
  description?: string;
  categoryId: string;
  trackingType: HabitTrackingType;
  targetValue: number; // 1 for binary, N for others
  unit?: string; // e.g. "mins", "pages", "glasses"
  scheduledDays: number[]; // 1 = Monday, ..., 7 = Sunday
  reminderTime?: string | null; // HH:mm
  reminderEnabled: boolean;
  createdAt: string;
  updatedAt: string;
  archived?: boolean;
}

export interface HabitLog {
  id: string;
  habitId: string;
  date: string; // YYYY-MM-DD
  value: number;
  isCompleted: boolean;
  loggedAt: string;
  notes?: string;
}

export type ReminderType = 
  | 'ONE_TIME'
  | 'DAILY'
  | 'WEEKLY'
  | 'CUSTOM_WEEKDAYS'
  | 'FIXED_INTERVAL'
  | 'COMPLETION_BASED';

export type OccurrenceStatus = 'PENDING' | 'COMPLETED' | 'SNOOZED' | 'SKIPPED' | 'MISSED';

export interface Reminder {
  id: string;
  title: string;
  notes?: string;
  type: ReminderType;
  categoryId: string;
  targetDate?: string | null; // YYYY-MM-DD
  targetTime?: string | null; // HH:mm
  scheduledDays?: number[]; // 1-7
  intervalMinutes?: number | null;
  isBlocking: boolean;
  isPaused: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ReminderOccurrence {
  id: string;
  reminderId: string;
  scheduledDateTime: string; // ISO string
  status: OccurrenceStatus;
  snoozedUntil?: string | null;
  completedAt?: string | null;
}

export type PlanStatus = 'DRAFT' | 'APPROVED';
export type PlanItemType = 'TASK' | 'HABIT' | 'REMINDER';

export interface DailyPlanItem {
  id: string;
  planId: string;
  itemType: PlanItemType;
  referenceId: string;
  title: string;
  startTime: string; // HH:mm
  endTime: string; // HH:mm
  durationMinutes: number;
  isCompleted: boolean;
  sortOrder: number;
}

export interface DailyPlan {
  id: string;
  date: string; // YYYY-MM-DD
  status: PlanStatus;
  items: DailyPlanItem[];
  unscheduledTasks: Task[];
  warnings: string[];
  generatedAt: string;
  approvedAt?: string | null;
}

export interface ReviewTaskDecision {
  taskId: string;
  action: 'COMPLETE' | 'RESCHEDULE' | 'CARRY_FORWARD' | 'EXCLUDE' | 'CANCEL';
  newDate?: string; // if reschedule
}

export interface DailyReview {
  id: string;
  date: string; // YYYY-MM-DD
  completedTaskCount: number;
  unfinishedTaskCount: number;
  habitCompletionRate: number; // 0-100
  notes?: string;
  decisions: ReviewTaskDecision[];
  reviewedAt: string;
}

export interface UserSettings {
  id: string; // 'current'
  theme: 'dark' | 'light';
  weekStartDay: 1 | 7; // 1 = Monday, 7 = Sunday
  dateFormat: string;
  timeFormat: '12h' | '24h';
  morningBriefingTime: string; // HH:mm, default "07:00"
  eveningReviewTime: string; // HH:mm, default "21:30"
  dayStartTime: string; // HH:mm, default "08:00"
  dayEndTime: string; // HH:mm, default "22:00"
  defaultTaskDurationMinutes: number; // 30
  soundEnabled: boolean;
  vibrationEnabled: boolean;
  exactAlarmsEnabled: boolean;
}

// Zod Schemas for validation
export const TaskInputSchema = z.object({
  title: z.string().min(1, 'Title is required').max(200),
  description: z.string().max(1000).optional(),
  categoryId: z.string().min(1),
  priority: z.enum(['URGENT', 'HIGH', 'MEDIUM', 'LOW']),
  deadlineDate: z.string().nullable().optional(),
  deadlineTime: z.string().nullable().optional(),
  estimatedDurationMinutes: z.number().int().min(5).max(720).default(30),
  plannedDate: z.string().nullable().optional(),
  plannedStartTime: z.string().nullable().optional(),
});

export const HabitInputSchema = z.object({
  title: z.string().min(1, 'Title is required').max(100),
  description: z.string().max(500).optional(),
  categoryId: z.string().min(1),
  trackingType: z.enum(['BINARY', 'QUANTITY', 'DURATION', 'REPETITION']),
  targetValue: z.number().positive(),
  unit: z.string().max(20).optional(),
  scheduledDays: z.array(z.number().min(1).max(7)).min(1, 'Select at least one day'),
  reminderTime: z.string().nullable().optional(),
  reminderEnabled: z.boolean().default(false),
});

export const ReminderInputSchema = z.object({
  title: z.string().min(1, 'Title is required').max(100),
  notes: z.string().max(500).optional(),
  type: z.enum(['ONE_TIME', 'DAILY', 'WEEKLY', 'CUSTOM_WEEKDAYS', 'FIXED_INTERVAL', 'COMPLETION_BASED']),
  categoryId: z.string().min(1),
  targetDate: z.string().nullable().optional(),
  targetTime: z.string().nullable().optional(),
  scheduledDays: z.array(z.number().min(1).max(7)).optional(),
  intervalMinutes: z.number().positive().nullable().optional(),
  isBlocking: z.boolean().default(false),
});
