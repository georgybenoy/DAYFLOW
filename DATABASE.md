# DAYFLOW — DATABASE.md

## 1. Database Overview
- **Database Name:** `dayflow.db`
- **Engine:** SQLite via Android Room 2.6.1
- **Initial Schema Version:** `1`
- **Schema Export:** Enabled (`exportSchema = true`), committed in `app/schemas/`
- **Destructive Migrations:** Strictly prohibited. `fallbackToDestructiveMigration()` is banned across the entire codebase.

---

## 2. Entity-Relationship Overview

```
 ┌──────────────┐          ┌───────────────────────┐          ┌──────────────────────────┐
 │  categories  │1       * │       reminders       │1       * │   reminder_occurrences   │
 │──────────────┼──────────┼───────────────────────┼──────────┼──────────────────────────┤
 │ id (PK)      │          │ id (PK)               │          │ id (PK)                  │
 │ name (UNIQUE)│          │ categoryId (FK, NULL) │          │ reminderId (FK, CASCADE) │
 └──────────────┘          └───────────────────────┘          │ scheduledAt              │
        │ 1                                                   └──────────────────────────┘
        │
        ├─────────────────────────────┐
        │ 1                         1 │
        ▼ *                           ▼ *
 ┌──────────────┐              ┌──────────────┐
 │    tasks     │1           * │    habits    │1           *
 │──────────────┼──────────────┼──────────────┼──────────────┐
 │ id (PK)      │              │ id (PK)      │              │
 │ categoryId   │              │ categoryId   │              │
 └──────────────┘              └──────────────┘              │
        │ 1                           │ 1                    │
        ▼ *                           ▼ *                    │
 ┌──────────────┐              ┌──────────────┐              │
 │ task_subtasks│              │  habit_logs  │              │
 │──────────────┤              │──────────────┤              │
 │ id (PK)      │              │ id (PK)      │              │
 │ taskId (FK)  │              │ habitId (FK) │              │
 └──────────────┘              │ localDate    │              │
                               └──────────────┘              │
 ┌─────────────────────────┐                                 │
 │       daily_plans       │1                              * │
 │─────────────────────────┼─────────────────────────────────┘
 │ id (PK)                 │ (sourceId references task or habit)
 │ localDate (UNIQUE)      │
 └─────────────────────────┘
        │ 1
        ▼ *
 ┌─────────────────────────┐   ┌─────────────────────────┐   ┌─────────────────────────┐
 │    daily_plan_items     │   │      daily_reviews      │   │      app_settings       │
 │─────────────────────────┤   │─────────────────────────┤   │─────────────────────────┤
 │ id (PK)                 │   │ id (PK)                 │   │ key (PK)                │
 │ planId (FK, CASCADE)    │   │ localDate (UNIQUE)      │   │ value                   │
 │ sourceType (TASK/HABIT) │   │ decisionsJson           │   │ updatedAt               │
 └─────────────────────────┘   └─────────────────────────┘   └─────────────────────────┘
```

---

## 3. Date, Time & Timezone Handling Invariants

1. **Absolute Temporal Instants:**
   - Stored as `INTEGER` representing epoch milliseconds in **UTC** (`java.time.Instant`).
   - Fields: `createdAt`, `updatedAt`, `scheduledAt`, `deliveredAt`, `handledAt`, `loggedAt`, `completedAt`, `reviewedAt`.
2. **Calendar Dates:**
   - Stored as `TEXT` in ISO-8601 format (`yyyy-MM-dd`) (`java.time.LocalDate`).
   - Fields: `startDate`, `endDate`, `deadlineDate`, `plannedDate`, `localDate`.
   - Preserves user calendar identity across leap years, DST boundaries, and flight timezone adjustments.
3. **Daily Clock Times:**
   - Stored as `TEXT` in ISO-8601 format (`HH:mm`) (`java.time.LocalTime`).
   - Fields: `startTime`, `endTime`, `deadlineTime`, `plannedStart`, `plannedEnd`, `reminderTime`.
4. **Timezones:**
   - Stored as `TEXT` representing standard IANA TimeZone IDs (`java.time.ZoneId`, e.g., `"America/New_York"`, `"Asia/Kolkata"`).
   - Recurrence engines calculate the next UTC trigger instant by projecting the local date and clock time against the stored `timeZoneId`.
5. **Weekday Bitmasks (`activeDaysMask`):**
   - Stored as `INTEGER` (0..127):
     - Bit 0: Monday (`1 << 0`)
     - Bit 1: Tuesday (`1 << 1`)
     - Bit 2: Wednesday (`1 << 2`)
     - Bit 3: Thursday (`1 << 3`)
     - Bit 4: Friday (`1 << 4`)
     - Bit 5: Saturday (`1 << 5`)
     - Bit 6: Sunday (`1 << 6`)

---

## 4. Tables and Schema Specifications

### 4.1 Table: `categories`
Stores task, reminder, and habit categories.

| Column | Type | Nullable | Default | Description |
|---|---|---|---|---|
| `id` | TEXT | NO | - | Primary Key (UUID) |
| `name` | TEXT | NO | - | Category display name (UNIQUE) |
| `colorHex` | TEXT | NO | - | UI accent color hex (e.g., `#A5E6D6`) |
| `iconName` | TEXT | NO | - | Material vector icon identifier |
| `isDefault` | INTEGER | NO | 0 | 1 for system defaults, 0 for custom |
| `createdAt` | INTEGER | NO | - | Epoch millis UTC |

- **Indexes:**
  - `idx_categories_name`: UNIQUE (`name`)
- **Seeded Defaults (Idempotent):**
  1. Academics
  2. Personal
  3. Fitness
  4. Work
  5. Projects
  6. Finance
  7. Other

---

### 4.2 Table: `reminders`
Defines one-time, fixed-interval, and recurring reminder configurations.

| Column | Type | Nullable | Default | Description |
|---|---|---|---|---|
| `id` | TEXT | NO | - | Primary Key (UUID) |
| `title` | TEXT | NO | - | Reminder title |
| `description` | TEXT | YES | NULL | Optional description or notes |
| `categoryId` | TEXT | YES | NULL | Foreign Key -> `categories(id)` ON DELETE SET NULL |
| `type` | TEXT | NO | - | `ONE_TIME`, `DAILY`, `WEEKLY`, `CUSTOM_WEEKDAYS`, `FIXED_INTERVAL`, `COMPLETION_INTERVAL` |
| `startDate` | TEXT | NO | - | ISO-8601 local start date (`yyyy-MM-dd`) |
| `startTime` | TEXT | NO | - | ISO-8601 local start time (`HH:mm`) |
| `intervalMinutes` | INTEGER | YES | NULL | Interval step in minutes for interval modes |
| `activeDaysMask` | INTEGER | NO | 127 | Bitmask for active weekdays (default all 7 days) |
| `endTime` | TEXT | YES | NULL | Optional daily window end time (`HH:mm`) |
| `endDate` | TEXT | YES | NULL | Optional recurrence end date (`yyyy-MM-dd`) |
| `priority` | TEXT | NO | `'MEDIUM'` | `URGENT`, `HIGH`, `MEDIUM`, `LOW` |
| `precision` | TEXT | NO | `'EXACT'` | `EXACT` or `APPROXIMATE` |
| `isEnabled` | INTEGER | NO | 1 | 1 for active, 0 for disabled |
| `isPaused` | INTEGER | NO | 0 | 1 for paused, 0 for unpaused |
| `timeZoneId` | TEXT | NO | - | IANA timezone ID |
| `anchorDate` | TEXT | YES | NULL | Recurrence anchor local date |
| `anchorTime` | TEXT | YES | NULL | Recurrence anchor local time |
| `lastCompletedAt` | INTEGER | YES | NULL | Epoch millis UTC of last completion |
| `createdAt` | INTEGER | NO | - | Epoch millis UTC |
| `updatedAt` | INTEGER | NO | - | Epoch millis UTC |

- **Foreign Keys:**
  - `categoryId` -> `categories(id)` ON DELETE SET NULL
- **Indexes:**
  - `idx_reminders_categoryId`: (`categoryId`)
  - `idx_reminders_status`: (`isEnabled`, `isPaused`)
  - `idx_reminders_priority`: (`priority`)

---

### 4.3 Table: `reminder_occurrences`
Tracks individual scheduled trigger occurrences for notifications and idempotency.

| Column | Type | Nullable | Default | Description |
|---|---|---|---|---|
| `id` | TEXT | NO | - | Primary Key (UUID) |
| `reminderId` | TEXT | NO | - | Foreign Key -> `reminders(id)` ON DELETE CASCADE |
| `scheduledAt` | INTEGER | NO | - | Epoch millis UTC of scheduled trigger |
| `status` | TEXT | NO | `'SCHEDULED'` | `SCHEDULED`, `DELIVERED`, `COMPLETED`, `SNOOZED`, `SKIPPED`, `MISSED`, `CANCELLED` |
| `snoozedUntil` | INTEGER | YES | NULL | Epoch millis UTC if snoozed |
| `deliveredAt` | INTEGER | YES | NULL | Epoch millis UTC when notification posted |
| `handledAt` | INTEGER | YES | NULL | Epoch millis UTC when user acted |
| `alarmRequestCode` | INTEGER | NO | - | Unique PendingIntent request code |

- **Foreign Keys:**
  - `reminderId` -> `reminders(id)` ON DELETE CASCADE
- **Indexes:**
  - `idx_occurrences_unique`: UNIQUE (`reminderId`, `scheduledAt`) — Guarantees idempotency
  - `idx_occurrences_reminderId`: (`reminderId`)
  - `idx_occurrences_status_scheduledAt`: (`status`, `scheduledAt`)

---

### 4.4 Table: `tasks`
Stores one-time responsibilities, deadlines, and planning allocations.

| Column | Type | Nullable | Default | Description |
|---|---|---|---|---|
| `id` | TEXT | NO | - | Primary Key (UUID) |
| `title` | TEXT | NO | - | Task title |
| `description` | TEXT | YES | NULL | Detailed notes or context |
| `categoryId` | TEXT | YES | NULL | Foreign Key -> `categories(id)` ON DELETE SET NULL |
| `priority` | TEXT | NO | `'MEDIUM'` | `URGENT`, `HIGH`, `MEDIUM`, `LOW` |
| `deadlineDate` | TEXT | YES | NULL | Due date (`yyyy-MM-dd`) independent of planned execution |
| `deadlineTime` | TEXT | YES | NULL | Due time (`HH:mm`) |
| `estimatedDurationMinutes` | INTEGER | YES | NULL | Estimated effort in minutes |
| `plannedDate` | TEXT | YES | NULL | Planned execution date (`yyyy-MM-dd`) |
| `plannedStart` | TEXT | YES | NULL | Planned start time (`HH:mm`) |
| `plannedEnd` | TEXT | YES | NULL | Planned end time (`HH:mm`) |
| `status` | TEXT | NO | `'PENDING'` | `PENDING`, `COMPLETED`, `CANCELLED` |
| `shouldCarryForward` | INTEGER | NO | 1 | 1 = Eligible for next day carry-forward, 0 = Do not carry forward |
| `createdAt` | INTEGER | NO | - | Epoch millis UTC |
| `completedAt` | INTEGER | YES | NULL | Epoch millis UTC |
| `updatedAt` | INTEGER | NO | - | Epoch millis UTC |

> [!IMPORTANT]
> **Overdue is a Derived State:** A task is overdue if `status == 'PENDING'` AND the deadline date/time is prior to `now()`. It is never stored as a column to avoid stale data.

- **Foreign Keys:**
  - `categoryId` -> `categories(id)` ON DELETE SET NULL
- **Indexes:**
  - `idx_tasks_categoryId`: (`categoryId`)
  - `idx_tasks_status`: (`status`)
  - `idx_tasks_deadlineDate`: (`deadlineDate`)
  - `idx_tasks_plannedDate`: (`plannedDate`)
  - `idx_tasks_priority`: (`priority`)

---

### 4.5 Table: `task_subtasks`
Hierarchical sub-items within a parent task.

| Column | Type | Nullable | Default | Description |
|---|---|---|---|---|
| `id` | TEXT | NO | - | Primary Key (UUID) |
| `taskId` | TEXT | NO | - | Foreign Key -> `tasks(id)` ON DELETE CASCADE |
| `title` | TEXT | NO | - | Subtask title |
| `isCompleted` | INTEGER | NO | 0 | 1 = Completed, 0 = Incomplete |
| `orderIndex` | INTEGER | NO | 0 | Positional order index |
| `createdAt` | INTEGER | NO | - | Epoch millis UTC |

- **Foreign Keys:**
  - `taskId` -> `tasks(id)` ON DELETE CASCADE
- **Indexes:**
  - `idx_subtasks_taskId`: (`taskId`)
  - `idx_subtasks_order`: (`taskId`, `orderIndex`)

---

### 4.6 Table: `habits`
Recurring habits with 4 distinct tracking modes.

| Column | Type | Nullable | Default | Description |
|---|---|---|---|---|
| `id` | TEXT | NO | - | Primary Key (UUID) |
| `name` | TEXT | NO | - | Habit name |
| `description` | TEXT | YES | NULL | Habit motivation/notes |
| `categoryId` | TEXT | YES | NULL | Foreign Key -> `categories(id)` ON DELETE SET NULL |
| `trackingType` | TEXT | NO | - | `BINARY`, `QUANTITY`, `DURATION`, `REPETITION` |
| `targetValue` | REAL | NO | 1.0 | Daily goal (e.g. 1.0 for binary, 8.0 for water, 30.0 for duration) |
| `unit` | TEXT | YES | NULL | Unit label (e.g. `'glasses'`, `'mins'`, `'reps'`) |
| `activeDaysMask` | INTEGER | NO | 127 | Scheduled weekdays bitmask (Mon-Sun) |
| `reminderPreference` | INTEGER | NO | 0 | 1 = Deliver reminder, 0 = None |
| `reminderTime` | TEXT | YES | NULL | Scheduled reminder time (`HH:mm`) |
| `isEnabled` | INTEGER | NO | 1 | 1 = Active, 0 = Disabled/Archived |
| `createdAt` | INTEGER | NO | - | Epoch millis UTC |
| `updatedAt` | INTEGER | NO | - | Epoch millis UTC |

- **Foreign Keys:**
  - `categoryId` -> `categories(id)` ON DELETE SET NULL
- **Indexes:**
  - `idx_habits_categoryId`: (`categoryId`)
  - `idx_habits_enabled`: (`isEnabled`)

---

### 4.7 Table: `habit_logs`
Daily progress recordings for habits.

| Column | Type | Nullable | Default | Description |
|---|---|---|---|---|
| `id` | TEXT | NO | - | Primary Key (UUID) |
| `habitId` | TEXT | NO | - | Foreign Key -> `habits(id)` ON DELETE CASCADE |
| `localDate` | TEXT | NO | - | ISO-8601 date of log (`yyyy-MM-dd`) |
| `completedValue` | REAL | NO | 0.0 | Actual progress logged for the day |
| `isCompleted` | INTEGER | NO | 0 | 1 = Reached target, 0 = Partial/None |
| `loggedAt` | INTEGER | NO | - | Epoch millis UTC |

- **Foreign Keys:**
  - `habitId` -> `habits(id)` ON DELETE CASCADE
- **Indexes:**
  - `idx_habit_logs_unique`: UNIQUE (`habitId`, `localDate`) — Exactly one log entry per habit per date
  - `idx_habit_logs_habitId`: (`habitId`)
  - `idx_habit_logs_date`: (`localDate`)

---

### 4.8 Table: `daily_plans`
Approved or draft chronological daily execution plans.

| Column | Type | Nullable | Default | Description |
|---|---|---|---|---|
| `id` | TEXT | NO | - | Primary Key (UUID) |
| `localDate` | TEXT | NO | - | ISO-8601 calendar date (`yyyy-MM-dd`, UNIQUE) |
| `status` | TEXT | NO | `'DRAFT'` | `DRAFT`, `APPROVED` |
| `createdAt` | INTEGER | NO | - | Epoch millis UTC |
| `approvedAt` | INTEGER | YES | NULL | Epoch millis UTC |
| `updatedAt` | INTEGER | NO | - | Epoch millis UTC |

- **Indexes:**
  - `idx_daily_plans_date`: UNIQUE (`localDate`) — Exactly one plan per day

---

### 4.9 Table: `daily_plan_items`
Scheduled time slots within a daily plan.

| Column | Type | Nullable | Default | Description |
|---|---|---|---|---|
| `id` | TEXT | NO | - | Primary Key (UUID) |
| `planId` | TEXT | NO | - | Foreign Key -> `daily_plans(id)` ON DELETE CASCADE |
| `sourceType` | TEXT | NO | - | `TASK`, `HABIT`, `CUSTOM` |
| `sourceId` | TEXT | YES | NULL | Reference to task or habit ID |
| `title` | TEXT | NO | - | Display title in schedule |
| `startTime` | TEXT | NO | - | Slot start time (`HH:mm`) |
| `endTime` | TEXT | NO | - | Slot end time (`HH:mm`) |
| `orderIndex` | INTEGER | NO | 0 | Positional ordering index |
| `isCompleted` | INTEGER | NO | 0 | 1 = Completed, 0 = Incomplete |

- **Foreign Keys:**
  - `planId` -> `daily_plans(id)` ON DELETE CASCADE
- **Indexes:**
  - `idx_plan_items_planId`: (`planId`)
  - `idx_plan_items_order`: (`planId`, `orderIndex`)
  - `idx_plan_items_source`: (`sourceType`, `sourceId`)

---

### 4.10 Table: `daily_reviews`
Evening review records capturing daily retrospective decisions.

| Column | Type | Nullable | Default | Description |
|---|---|---|---|---|
| `id` | TEXT | NO | - | Primary Key (UUID) |
| `localDate` | TEXT | NO | - | ISO-8601 calendar date (`yyyy-MM-dd`, UNIQUE) |
| `notes` | TEXT | YES | NULL | Optional evening review reflection notes |
| `reviewedAt` | INTEGER | NO | - | Epoch millis UTC |
| `tasksCompletedCount` | INTEGER | NO | 0 | Snapshot count of completed tasks |
| `tasksUnfinishedCount` | INTEGER | NO | 0 | Snapshot count of unfinished tasks |
| `habitsCompletedCount` | INTEGER | NO | 0 | Snapshot count of completed habits |
| `decisionsJson` | TEXT | NO | `'[]'` | Serialized JSON array of review actions |

#### Justification for `decisionsJson` vs Child Table
1. **Immutable Snapshot Guarantee:** Evening reviews represent a historical snapshot frozen at the end of the day. Serializing task outcomes (`taskId`, `decision`, `rescheduledToDate`, `timestamp`) into JSON preserves the state exactly as decided, without being vulnerable to future task deletions or database cascade side effects.
2. **Atomic Batch Ingestion for Planner:** The next morning's deterministic planner reads the previous day's review decisions as a complete unit. A single column lookup eliminates unnecessary joins across historical review tables.
3. **Backup Simplicity:** A self-contained JSON structure ensures frictionless serializability for WorkManager 24h JSON/ZIP backups.

- **Indexes:**
  - `idx_daily_reviews_date`: UNIQUE (`localDate`) — Exactly one review record per day

---

### 4.11 Table: `app_settings`
Arbitrary key-value store for application state that requires relational database transactions.

| Column | Type | Nullable | Default | Description |
|---|---|---|---|---|
| `key` | TEXT | NO | - | Primary Key |
| `value` | TEXT | NO | - | Serialized setting value |
| `updatedAt` | INTEGER | NO | - | Epoch millis UTC |

---

## 5. Migration Strategy & Governance

1. **Strict Version Progression:**
   - Room migrations are written as explicit `Migration(from, to)` classes.
   - Any schema adjustment increments the database version in `DayflowDatabase` and provides a corresponding `Migration` step.
2. **Automated Verification:**
   - `MigrationTestHelper` verifies schema transitions against exported JSON schemas in `app/schemas/`.
3. **Zero Destructive Migration Invariant:**
   - `fallbackToDestructiveMigration()` is forbidden.
   - A static lint check / validation script ensures this method is never invoked in production or test database builders.

---

## 6. Backup & Restoration Data Management

### 6.1 `BackupDao` Interface
`BackupDao` provides high-performance bulk read, table truncation, and atomic transactional restoration for the entire Room database.

- **Bulk Read Operations:**
  `getAllCategories()`, `getAllReminders()`, `getAllReminderOccurrences()`, `getAllTasks()`, `getAllTaskSubtasks()`, `getAllHabits()`, `getAllHabitLogs()`, `getAllDailyPlans()`, `getAllDailyPlanItems()`, `getAllDailyReviews()`, `getAllAppSettings()`.

- **Foreign Key Safe Clear Order (Child to Parent):**
  1. `DELETE FROM reminder_occurrences`
  2. `DELETE FROM reminders`
  3. `DELETE FROM task_subtasks`
  4. `DELETE FROM tasks`
  5. `DELETE FROM habit_logs`
  6. `DELETE FROM habits`
  7. `DELETE FROM daily_plan_items`
  8. `DELETE FROM daily_plans`
  9. `DELETE FROM daily_reviews`
  10. `DELETE FROM app_settings`
  11. `DELETE FROM categories`

- **Foreign Key Safe Restoration Order (Parent to Child):**
  1. `insertCategories`
  2. `insertReminders`
  3. `insertReminderOccurrences`
  4. `insertTasks`
  5. `insertTaskSubtasks`
  6. `insertHabits`
  7. `insertHabitLogs`
  8. `insertDailyPlans`
  9. `insertDailyPlanItems`
  10. `insertDailyReviews`
  11. `insertAppSettings`

- **Transactional Guarantee:**
  `@Transaction suspend fun restoreAllDataTransaction(...)` guarantees that all clear and insert operations happen inside a single SQLite transaction with `OnConflictStrategy.ABORT`. Any constraint failure immediately rolls back the transaction, preserving the previous database state.
