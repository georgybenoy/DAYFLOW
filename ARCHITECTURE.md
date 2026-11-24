# DAYFLOW — ARCHITECTURE.md

## 1. Architectural Philosophy
DAYFLOW is designed as a web-first, offline-first, deterministic personal productivity and daily execution system. The architecture adheres to Clean Architecture principles, modular feature boundaries, repository patterns, and unidirectional data flow (UDF).

Local development and preview occur in the desktop browser at `http://localhost:5173` via Vite with Hot Module Replacement (HMR). The application is packaged for Android via Capacitor.

```
   ┌────────────────────────────────────────────────────────┐
   │                   Presentation Layer                   │
   │  React 18+ Screens • Tailwind CSS • Lucide Icons       │
   │  Recharts • Mobile Frame & Desktop Preview • Zustand   │
   └───────────────────────────▲────────────────────────────┘
                               │ Reactive State & Actions
   ┌───────────────────────────▼────────────────────────────┐
   │                      Domain Layer                      │
   │  Domain Models & Zod Schemas • Repository Contracts    │
   │  Recurrence Engine • Deterministic Planner Engine      │
   │  Streak Calculator • Briefing Generator               │
   └───────────────────────────▲────────────────────────────┘
                               │ Repository Implementation
   ┌───────────────────────────▼────────────────────────────┐
   │                       Data Layer                       │
   │  IndexedDB (Dexie) on Web • SQLite via Capacitor       │
   │  Repositories • Versioned JSON Backup & Restore        │
   └────────────────────────────────────────────────────────┘
```

## 2. Layers & Responsibilities

### Presentation Layer (`src/presentation/*`, `src/core/*`)
- **React 18+ & Tailwind CSS:** Clean component hierarchy with responsive mobile-optimized frame.
- **Zustand Central Store:** Single reactive store (`useDayFlowStore`) managing entity caches, selected date, active screens, and domain operations.
- **Design Tokens:** Defined in `tailwind.config.js` and `src/core/theme.ts` adhering strictly to `DESIGN_SYSTEM.md` (Dark forest-green `#07120F`, surface `#101D18`, mint `#A5E6D6`, yellow `#F0E99B`, lavender `#DCC3F5`).
- **Data Visualizations:** SVG & Canvas based circular progress rings (`ProgressRing`, `MultiProgressRing`), weekly completion bar charts, and calendar matrices.

### Domain Layer (`src/domain/*`)
- **Platform Agnostic & Pure:** Pure TypeScript modules with zero browser-DOM or native platform dependencies.
- **Domain Models & Zod Validation (`src/domain/models.ts`):** Immutable TypeScript interfaces and runtime Zod validation schemas for Category, Task, Subtask, Habit, HabitLog, Reminder, ReminderOccurrence, DailyPlan, DailyReview, UserSettings.
- **Recurrence Calculator (`src/domain/recurrence.ts`):** Deterministic occurrence calculation for `ONE_TIME`, `DAILY`, `WEEKLY`, `CUSTOM_WEEKDAYS`, `FIXED_INTERVAL`, and `COMPLETION_BASED` reminders.
- **Streak Calculator (`src/domain/streak.ts`):** Calculates streaks counting exclusively scheduled days. Inactive weekdays neither break nor extend streaks; today not yet logged does not break the streak until the day concludes.
- **Briefing Generator (`src/domain/briefing.ts`):** Pure generator for morning summaries derived from stored database records.
- **Deterministic Planner Engine (`src/domain/planner.ts`):** 10-step scheduling algorithm detailed below.

### Data Layer (`src/data/*`)
- **IndexedDB via Dexie (`src/data/db.ts`):** Tables for `categories`, `tasks`, `subtasks`, `habits`, `habitLogs`, `reminders`, `occurrences`, `plans`, `reviews`, `settings`.
- **Database Seeding (`src/data/seed.ts`):** Idempotent initial seed of standard categories and default settings.
- **Repositories (`src/data/repositories.ts`):** Abstract CRUD operations with transactional guarantees.
- **Backup & Restore (`BackupRepository`):** Versioned JSON export and safe transactional restore with schema version compatibility verification.

## 3. Deterministic Planner Engine Algorithm
The planner algorithm operates deterministically with zero AI and zero randomness, resolving ties with stable alphanumeric keys:
1. **Collect Eligible Tasks:** Filters tasks where status is `PENDING` and not excluded or cancelled by review decisions.
2. **Identify Overdue Tasks:** Detects tasks with `deadlineDate < targetDate`.
3. **Today's Deadlines:** Identifies tasks due exactly on the target date.
4. **Scheduled Habits:** Gathers active habits scheduled for the target weekday.
5. **Apply Review Decisions:** Respects prior evening review actions (Carry Forward, Reschedule, Exclude, Cancel).
6. **Priority Scoring Function:**
   - Base weights: `URGENT` = 1000, `HIGH` = 500, `MEDIUM` = 200, `LOW` = 50.
   - Overdue bonus: +350.
   - Due today bonus: +250.
   - Planned for today bonus: +150.
7. **Stable Sort:** Orders tasks descending by score, then alphabetically by title, then stably by task id.
8. **Time Slot Assignment:** Sequentially allocates items into the user's availability window (e.g. 08:00 to 22:00) with a 10-minute buffer, honoring task estimated durations.
9. **Conflict & Over-Capacity Handling:** Detects time-window overflow and generates warnings without dropping tasks.
10. **Output Draft Plan:** Emits an ordered `DailyPlan` containing scheduled timeline items, the `unscheduledTasks` list (never dropped), and capacity notifications. Task deadlines are never modified.

## 3. Dependency Injection (Hilt)
All dependencies are wired using Dagger Hilt (`@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel`).
Modules provide:
- `@IoDispatcher`, `@DefaultDispatcher`, `@MainDispatcher` coroutine dispatchers.
- `TimeProvider` abstraction allowing precise time mocking in unit tests.
- `DataStore<Preferences>` and repository bindings.

## 4. Concurrency & Asynchronous Programming
- Kotlin Coroutines and Flows are used throughout.
- Database reads return reactive `Flow<T>`, ensuring UI automatically reacts to local mutations.
- Write operations are `suspend` functions executing on `Dispatchers.IO`.

## 5. Offline & Privacy Invariants
- No network permission (`android.permission.INTERNET`) is requested.
- No analytics SDKs or remote telemetry are integrated.
- All backups are encrypted ZIP archives stored in application-private storage or exported manually through Android Storage Access Framework (SAF).

## 6. Recurrence Engine & Schedule Reconciliation Architecture

### A. Pure Domain Invariants
- Located in `com.dayflow.app.domain.recurrence`.
- **Zero Android Imports:** Operates strictly on standard `java.time.*` (`Instant`, `LocalDate`, `LocalTime`, `ZoneId`, `ZonedDateTime`, `Duration`).
- **Deterministic:** Pure calculation functions without hidden side effects. Given the same reminder parameters, `after` instant, and `zoneId`, the output is mathematically identical.
- **Wall-Clock Preservation:** Schedules are anchored to the user's local wall-clock time (`startTime`, `endTime`). When timezones shift, local times (e.g. 9:00 AM) are preserved while UTC epoch instants are recomputed.

### B. Recurrence Types & Window Mechanics
1. **ONE_TIME:** Fires once at `startDate` + `startTime`. Expired after that instant.
2. **DAILY:** Recruits at `startTime` on all active days in `activeDaysMask` (default 127 = all 7 days).
3. **WEEKLY:** Recurs at `startTime` on `startDate.dayOfWeek` (or custom active weekday mask).
4. **CUSTOM_WEEKDAYS:** Recurs at `startTime` strictly on enabled weekdays (skips disabled days; safe `null` if all weekdays are disabled).
5. **FIXED_INTERVAL:**
   - Generates recurring slots anchored to `startTime` separated by `intervalMinutes` (enforces 1-minute minimum guard).
   - Constrained to the daily window `[startTime, endTime]`.
   - Supports **midnight-crossing windows** (e.g. `startTime = 22:00`, `endTime = 04:00`): slots cross midnight on day $D+1$ until `endTime`, after which the window closes and rolls over to the next active day at 22:00.
6. **COMPLETION_INTERVAL:**
   - Next occurrence = `completedAt + intervalMinutes`.
   - Clamped to the active window `[startTime, endTime]`.
   - If `next` falls after `endTime`, it rolls over to `startTime` on the next active day.

### C. Explicit Design Decisions & Policy Governance
- **Inclusive End Date Rule:** `reminder.endDate` is strictly **INCLUSIVE**. An occurrence falling on `endDate` is valid and scheduled; any candidate slot strictly after `endDate` returns `null`.
- **Skip Policy:** When skipping a completion-interval reminder, `nextAfterSkip` advances from `scheduledAt` (the planned slot) rather than `now`. *Rationale:* Advancing from `scheduledAt` prevents schedule drift caused by late user interaction (e.g. skipping a 10:00 slot at 11:30 advances to 12:00 rather than 13:30).
- **Snooze Non-Mutation Invariant:** Snoozing generates a transient `ReminderOccurrence` with status `SNOOZED` and `snoozedUntil`. The underlying `Reminder` definition, `anchorDate`, and `startDate` are NEVER mutated.
- **Notification Burst Prevention:** `reconcileMissed` handles long device downtime (e.g. phone off for 2 weeks). All overdue slots are cleanly recorded as `MISSED`, and at most ONE recent occurrence (within a configurable catch-up window) is returned for immediate delivery.

### D. Idempotent Schedule Reconciliation (`ScheduleReconciler`)
- Evaluates the desired state against existing persisted occurrences.
- Emits atomic, differential `ScheduleAction` commands:
  - `CreateOccurrence`: When a future slot is missing.
  - `CancelOccurrence`: For disabled/paused reminders, orphaned occurrences from deleted reminders, or duplicate future slots.
  - `MarkOccurrenceMissed`: For past unhandled occurrences (`scheduledAt <= now`).
  - `KeepOccurrence`: For valid, existing future occurrences matching the computed schedule.
- Guarantees scheduling idempotency: applying actions and re-evaluating produces only `KeepOccurrence` with zero modifications.

---

## 7. Alarms, Notifications & Background Execution Architecture

### A. Notification Dispatch & Channel Segmentation
DAYFLOW isolates user alerts across five dedicated Android notification channels initialized on application startup (`DayflowApplication.onCreate()`):
1. **Critical Deadlines (`channel_critical_deadlines`):** `IMPORTANCE_HIGH`. Used for urgent reminders, overdue deadlines, and high-priority commitments. Plays sound, enables vibration, and displays heads-up banners.
2. **Routine Reminders (`channel_routine_reminders`):** `IMPORTANCE_DEFAULT`. Used for standard recurring habits and daily commitments.
3. **Daily Briefing (`channel_daily_briefing`):** `IMPORTANCE_DEFAULT`. Delivers morning planning digests.
4. **Evening Review (`channel_evening_review`):** `IMPORTANCE_DEFAULT`. Prompts daily review workflows.
5. **General Notifications (`channel_general`):** `IMPORTANCE_LOW`. Background maintenance notices and backup confirmations.

Notifications feature actionable buttons (Complete, Snooze, Skip), deep linking into `MainActivity` with back-stack preservation, and grouping under `group_dayflow_reminders`.

### B. One-Shot Alarm Scheduling Engine
DAYFLOW enforces a strict one-shot alarm model using `AlarmManager`:
- Repeating alarms (`setRepeating`) are explicitly forbidden due to Android OS batching and recurrence drift.
- Each occurrence is scheduled individually via `AlarmScheduler`.
- **Exact vs. Inexact Scheduling:** Checks `alarmManager.canScheduleExactAlarms()` on Android 12+ (API 31+). Uses `setExactAndAllowWhileIdle` when granted; falls back gracefully to `setAndAllowWhileIdle` with clear UI disclosure via `SchedulingCapability` (`EXACT`, `APPROXIMATE_FALLBACK`, `NOTIFICATIONS_DENIED`).
- **Defensive Error Handling:** Defensively handles `SecurityException` if exact alarm capability is revoked at runtime, falling back to inexact alarms.
- **Occurrence Request Codes:** Derives collision-resistant request codes via `(occurrenceId.hashCode() and 0x7FFFFFFF)`.

### C. Delivery Idempotency & State Lifecycle
Every reminder trigger follows an idempotent state lifecycle:
1. `SCHEDULED`: Occurrence row is persisted before or in the same transaction as the `AlarmManager` call.
2. `DELIVERED`: `AlarmReceiver` wakes up via `goAsync()`, verifies the occurrence is still in `SCHEDULED` or `SNOOZED` state (suppressing duplicate deliveries), posts the notification, and transitions the state to `DELIVERED`.
3. Post-Delivery Recurrence Scheduling:
   - For fixed-clock and standard recurrences (`ONE_TIME`, `DAILY`, `WEEKLY`, `CUSTOM_WEEKDAYS`, `FIXED_INTERVAL`), the next occurrence is calculated and scheduled immediately.
   - For completion-interval recurrences (`COMPLETION_INTERVAL`), recurrence advancement pauses until the user interacts (complete or skip), preventing phantom intervals when notifications are unread.
4. Final States:
   - `COMPLETED`: User completed the occurrence.
   - `SNOOZED`: User snoozed; schedules transient alarm without altering base reminder anchor.
   - `SKIPPED`: User skipped; advances schedule from original slot time.
   - `MISSED`: Device was off or occurrence passed unhandled; caught and marked by reconciler.
   - Notification dismissal (`deleteIntent`) records user dismissal but NEVER marks the task or reminder completed.

### D. System Event Receivers & Rescheduling
Four broadcast receivers guarantee schedule persistence across system events by invoking `RescheduleAllUseCase` (which delegates to the pure `ScheduleReconciler`):
- `BootReceiver`: Recovers active schedules on `ACTION_BOOT_COMPLETED` and `ACTION_LOCKED_BOOT_COMPLETED`.
- `TimeChangeReceiver`: Responds to `ACTION_TIME_SET`, `ACTION_TIMEZONE_CHANGED`, and `ACTION_DATE_CHANGED` to re-align wall-clock anchors.
- `ExactAlarmPermissionReceiver`: Responds to `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` to upgrade or downgrade scheduling precision.
- `PackageReplacedReceiver`: Responds to `ACTION_MY_PACKAGE_REPLACED` to verify alarm continuity following app updates.

---

## 8. Reminder Feature Architecture (Phase 5)

### A. Presentation & Unidirectional Data Flow
- **Reminders Screen (`RemindersViewModel`, `RemindersScreen`):**
  - Segmented sections: "Active" and "Paused", observing SQLite flows dynamically.
  - Displays `ReminderCard` showing title, recurrence summary, next delivery time, priority badge, precision indicator (`Exact` vs `Approx.`), and overflow actions (`Edit`, `Pause/Resume`, `Delete`).
  - Upcoming Occurrences Ribbon: Horizontally previews imminent scheduled occurrences with reminder title and time.
  - Floating Action Button with accessible label `contentDescription = "Create new reminder"`.
  - Empty, loading, and error states.
- **Reminder Create/Edit Screen (`ReminderCreateViewModel`, `ReminderCreateScreen`):**
  - Unified one-handed friendly layout supporting both new reminder creation and editing existing schedules.
  - Dynamic schedule parameter forms adapting to selected `ReminderType` (`ONE_TIME`, `DAILY`, `WEEKLY`, `CUSTOM_WEEKDAYS`, `FIXED_INTERVAL`, `COMPLETION_INTERVAL`).
  - Bitmask weekday selector for custom days (bits 0..6 for Mon-Sun).
  - Priority selector, precision selector, and default snooze override chips.
  - **Live Validation Engine:** Validates form inputs in real time via `ReminderValidator`, emitting sealed result types (`ReminderValidationResult.Valid` / `ReminderValidationResult.Invalid`).
  - **Live "Next 5 Occurrences" Preview:** Projects future delivery times by directly invoking Phase 3's pure `RecurrenceCalculator`—strictly avoiding duplicate scheduling logic in the presentation layer.
- **Reminder Detail Screen (`ReminderDetailViewModel`, `ReminderDetailScreen`):**
  - Full configuration details and recurrence summary.
  - Interactive Active Occurrence Card: Enables instant `Complete`, `Snooze` (default 10m without anchor mutation), and `Skip` actions directly in the UI.
  - Comprehensive Occurrence History: Reverse-chronological timeline displaying past and current occurrences with status tags (`COMPLETED`, `SNOOZED`, `SKIPPED`, `DELIVERED`, `SCHEDULED`, `MISSED`, `CANCELLED`).
  - Lifecycle controls: `Pause/Resume` and `Delete` (with confirmation dialog).
- **Today Screen Integration (`TodayViewModel`, `TodayScreen`):**
  - "Upcoming Reminders" section renders real scheduled occurrences for the day.
  - Provides a single-tap "Complete" action that delegates to `CompleteReminderOccurrenceUseCase`, guaranteeing 100% behavioral parity between UI actions and system notification actions.

### B. Shared Domain Use Cases
All reminder interactions are mediated through domain use cases reused across notifications, Today screen, Reminders screen, and Reminder Detail screen:
- `CompleteReminderOccurrenceUseCase`: Updates occurrence to `COMPLETED`, cancels active notifications, updates `lastCompletedAt`, and for `COMPLETION_INTERVAL` reminders, computes and schedules the subsequent slot from completion time.
- `SnoozeReminderOccurrenceUseCase`: Cancels active notification, marks current occurrence handled, computes snooze occurrence via `RecurrenceCalculator.nextAfterSnooze`, and schedules transient alarm without altering the base reminder anchor.
- `SkipReminderOccurrenceUseCase`: Marks occurrence `SKIPPED`, cancels notification, and advances completion-interval cadence from planned `scheduledAt`.
- `PauseReminderUseCase`: Sets `isPaused = true`, saves reminder, and cancels all pending occurrences and alarms.
- `ResumeReminderUseCase`: Sets `isPaused = false`, saves reminder, and re-triggers scheduling.
- `DeleteReminderUseCase`: Cancels all pending alarms, deletes occurrences, and removes the reminder row from the database.
- `GetUpcomingRemindersUseCase`: Emits reactive flow combining pending occurrences and active reminder models.

### C. Permission & Inexact Fallback UX
- `RemindersScreen` actively observes `AlarmScheduler.schedulingCapability`.
- When `NOTIFICATIONS_DENIED`: Displays `PermissionBanner` explaining notification necessity and provides a single-tap button to open system notification settings.
- When `APPROXIMATE_FALLBACK`: Displays `PermissionBanner` disclosing that reminders deliver within a 10-15 minute inexact window, with a direct button to grant exact alarm permissions in system settings.
- When `EXACT`: No banner is displayed.

---

## 9. Task Management Architecture (Phase 6)

### A. Core Invariants & Domain Principles
1. **Strict Identity Preservation:** Rescheduling a task's planned date or execution window never generates new task entities or duplicate IDs. `RescheduleTaskPlannedTimeUseCase` updates `plannedDate`, `plannedStart`, and `plannedEnd` while maintaining the exact original primary key (`id`) and creation timestamp.
2. **Deadline & Planning Independence:** A task's commitment deadline (`deadlineDate`, `deadlineTime`) is decoupled from its planned execution window. Editing planned execution times leaves the deadline unchanged. The deadline can only be modified via explicit user intent (`ChangeTaskDeadlineUseCase`).
3. **Pure Overdue Derivation:** Overdue status is never stored as a database column to prevent stale status across days. `DeriveOverdue` evaluates pure boundary rules based on current reference time:
   - Tasks not in `PENDING` status (`COMPLETED`, `CANCELLED`) are never overdue.
   - Tasks without a `deadlineDate` are never overdue.
   - Tasks with `deadlineDate` in the past are overdue.
   - Tasks with `deadlineDate` equal to today are overdue only if `deadlineTime != null` and current time exceeds `deadlineTime`.
4. **Independent Subtask Tracking:** Subtask completion (`isCompleted`) is tracked separately. Completing all subtasks does not auto-complete the parent task, and parent tasks can be completed independently.
5. **Cascade Integrity:** Deleting a task cascades to child `task_subtasks` rows via Room's `@ForeignKey(onDelete = CASCADE)` and cancels any scheduled deadline alarms in the reminder pipeline.

### B. Deadline Reminder Pipeline Reuse
- **Zero Scheduler Duplication:** Tasks reuse the existing `AlarmScheduler` and `Reminder` domain pipeline. Configurable deadline reminders (`AT_DEADLINE`, `ONE_HOUR_BEFORE`, `TWO_HOURS_BEFORE`, `ONE_DAY_BEFORE`) are scheduled as `Priority.URGENT` one-time reminders.
- **Channel Routing:** Because priority is `URGENT`, `DayflowNotificationChannels.getChannelForPriority(priority)` automatically directs notifications to `CHANNEL_CRITICAL_DEADLINES` with high priority, heads-up presentation, vibration, and DND bypass.
- **Lifecycle Synchronization:** Completing, cancelling, deleting, or rescheduling the deadline of a task immediately cancels and clears pending deadline reminders via `DeadlineReminderScheduler`.

### C. Presentation Architecture
- **Tasks Screen (`TasksViewModel`, `TasksScreen`):**
  - Segmented filter tabs (`Today`, `Upcoming`, `Overdue`, `Completed`, `All`) with badge counters.
  - Multi-attribute filtering by `Priority` and `Category`.
  - `TaskCard` featuring `PriorityBadge` (icon + text, accessible and non-color-dependent), category label, deadline formatter, subtask progress indicator, and single-tap completion checkbox.
- **Task Create & Edit (`TaskCreateViewModel`, `TaskCreateScreen`):**
  - One-handed friendly form with thumb-reachable primary action.
  - Live sealed validation via `TaskValidator` (`BlankTitle`, `InvalidPlannedTimeWindow`, `InvalidDuration`).
  - Dynamic deadline reminder chips and subtask reordering controls.
- **Task Detail (`TaskDetailViewModel`, `TaskDetailScreen`):**
  - Interactive subtask checklist allowing independent toggling.
  - `RescheduleDialog` with explicit disclosure that deadlines remain untouched.
  - `ChangeDeadlineDialog` for recalculating deadline reminders.
- **Today Screen Integration (`TodayViewModel`, `TodayScreen`):**
  - Daily Execution `ProgressRing` with real completion ratio for the selected date.
  - Priority Tasks card list with single-tap completion routing through `CompleteTaskUseCase`.
  - Upcoming Deadlines card list surfacing approaching commitments.

---

## 10. Habit Engine Architecture (Phase 7)

### A. Core Invariants & Domain Principles
1. **Multi-Type Tracking:** DAYFLOW habits support four distinct execution modes:
   - `BINARY`: Binary boolean completion (Yes/No).
   - `QUANTITY`: Numerical accumulation towards a target (e.g. 2.0 L of water).
   - `DURATION`: Time spent towards a target duration (e.g. 30 min reading).
   - `REPETITION`: Repetition count towards a target (e.g. 50 pushups).
2. **Pure Progress Evaluation (`HabitProgressCalculator`):**
   - Independent of Android framework or database entities.
   - Computes `completedValue`, `targetValue`, `isCompleted`, `progressRatio` (clamped 0.0f..1.0f), and accessible human-readable formatted string (e.g., "1.5 / 2.0 L", "30 / 45 min", "Completed").
3. **Pure Streak Calculation Invariants (`StreakCalculator`):**
   - **Scheduled Days Only:** Evaluates streak strictly against `habit.activeDaysMask`. Only days where `ActiveDays.isDayActive(mask, dayOfWeek)` is true count towards streak progression.
   - **Inactive Day Neutrality:** Inactive weekdays neither increment nor break streaks (e.g. a weekday-only habit continues its streak across weekends).
   - **Current Day Pending Exemption:** If a habit is scheduled for the current calendar day (today) but not yet completed, it **does not break the streak** until the calendar day concludes at midnight.
   - **Missed Day Breakage:** A missed scheduled day in the past immediately terminates the streak.
   - **Retroactive Reconciliation:** Re-evaluating streak calculations deterministically reflects retroactive log entries.
   - **Longest Streak:** Tracks historical maximum consecutive scheduled completions alongside current streak.
4. **Historical Visualization:**
   - `getWeeklyHistory`: Computes 7-day status sequence (Mon-Sun) with scheduled/completion flags and progress ratios.
   - `getMonthlyHistory`: Computes full-month status sequence for heatmap rendering.

### B. Reminder Pipeline Integration & Notification Parity
1. **Single Source of Truth (`HabitReminderScheduler`):**
   - Habit reminder configurations reuse the core Phase 4/5 reminder pipeline via `ScheduleReminderUseCase` and `DeleteReminderUseCase`.
   - Generates linked `Reminder` entity with ID `habit_reminder_${habit.id}` and `ReminderType.CUSTOM_WEEKDAYS` matching `activeDaysMask`.
   - Pausing, disabling, or deleting a habit automatically cancels and removes all pending reminder alarms.
2. **Notification Action Parity:**
   - **Binary Habits:** Notification "Complete" action routes through `NotificationActionReceiver`, automatically recording a completed `HabitLog` for `LocalDate.now()` in `HabitRepository`.
   - **Non-Binary Habits:** Action button is rendered as "Log Progress", which deep-links to `MainActivity` with an intent extra opening `QuickLogBottomSheet`.
   - **Dismissal Invariant:** Swiping away or dismissing a notification (`ACTION_DISMISS`) **never** records habit completion or progress.

### C. Presentation Architecture
- **Habits Screen (`HabitsViewModel`, `HabitsScreen`):**
  - Daily list with real-time `ProgressRing` metric card.
  - Categorized into "Active Habits" and "Paused Habits".
  - `HabitCard` displaying streak badge (`🔥 Xd`), category pastel chip, tracking type tag, linear progress indicator, formatted progress, and `WeeklyDotsStrip`.
- **Habit Create & Edit Screen (`HabitCreateViewModel`, `HabitCreateScreen`):**
  - Configures name, description, category, tracking type, custom targets and units, 7-day active bitmask chips, and optional daily reminder alarms with time picker.
- **Habit Detail Screen (`HabitDetailViewModel`, `HabitDetailScreen`):**
  - Current streak and Longest streak metric cards.
  - Today's execution card with instant completion toggle / quick-log action.
  - Embedded `HabitCalendar` monthly heatmap grid with 5 intensity levels (0%, 25%, 50%, 75%, 100%).
  - Schedule and reminder configuration overview.
  - Pause/Resume and Delete controls.
- **Quick Log Bottom Sheet (`QuickLogBottomSheet`):**
  - Fast logging interface with type-specific steppers (e.g. `±5`, `±1` for reps; `±15`, `±5` for duration; `±50`, `±10` for quantity), direct numeric text input with validation, and a 1-tap "Mark as Target Reached" shortcut.
- **Today Screen Integration (`TodayViewModel`, `TodayScreen`):**
  - Section D "Habit Overview" displays real completion counts (`X of Y done`) and remaining habits for the selected date.

---

## 11. Daily Planner Architecture (Phase 8)

### A. Core Invariants & Determinism
1. **100% Deterministic Engine (`DeterministicDailyPlanner`):**
   - Zero randomness, zero AI dependencies, zero network requests.
   - Idempotent and pure: executing with identical inputs produces byte-identical output.
   - Stable tie-breaking: when priority scores, deadline dates, and deadline times are identical, candidate tasks are strictly ordered by their permanent `task.id` string.
2. **Strict Invariant: No-Dropped-Tasks:**
   - Any eligible task that cannot be scheduled due to availability constraints, fixed commitment overlaps, or time capacity is placed in `unscheduledItems` with an explanatory reason and rescheduling suggestion.
   - Tasks are **never** silently discarded or deleted from the database.
   - Invariant check: `scheduledItems.size + unscheduledItems.size == total eligible tasks`.
3. **Strict Invariant: Immutable Task Deadlines:**
   - The planning engine schedules execution time slots (`startTime`, `endTime`) without ever modifying or overwriting `task.deadlineDate` or `task.deadlineTime`.
4. **Data Persistence (`DailyPlanRepository`, `DailyPlanEntity`, `DailyPlanItemEntity`):**
   - Enforces Room unique index `Index(value = ["localDate"], unique = true)` ensuring at most one plan row per calendar date.
   - Plans exist in two explicit states: `PlanStatus.DRAFT` (generated proposal, fully editable) and `PlanStatus.APPROVED` (committed execution schedule).
   - Atomic multi-item replacement using `@Transaction suspend fun savePlanWithItems(...)`.

### B. The 10-Step Planning Algorithm
1. **Step 1 — Collect Eligible Tasks:** Filters pending tasks. Tasks with review decisions are checked (`EXCLUDED`, `COMPLETED`, `CANCELLED` are omitted; `CARRIED_FORWARD` or `RESCHEDULED` to today are included). Backlog tasks with no planned date or deadline are included.
2. **Step 2 & 3 — Identify Overdue and Today's Deadlines:** Evaluates tasks with deadlines prior to reference time or falling on the planning date.
3. **Step 4 — Scheduled Habits:** Gathers enabled habits whose active weekdays match the target date (`ActiveDays.isDayActive(mask, dayOfWeek)`).
4. **Step 5 — Apply Review Decisions:** Grants priority boosts to tasks carried forward from evening review.
5. **Step 6 — Compute Priority Score:** Weighted deterministic score function:
   - Priority level: `URGENT` = 1000, `HIGH` = 500, `MEDIUM` = 200, `LOW` = 50.
   - Overdue age bonus: 800 base + `min(daysOverdue * 50, 400)`.
   - Today's deadline: 600 base + hourly urgency bonus `(24 - deadlineHour) * 10`.
   - Tomorrow's deadline: 300 points.
   - Within 3 days: 150 points.
   - Carried forward boost: 350 points.
6. **Step 7 — Stable Sort:** Ordered by `score DESC`, then `deadlineDate ASC`, then `deadlineTime ASC`, and tie-broken by `task.id ASC`.
7. **Step 8 — Slot Assignment with Buffer:**
   - Availability window derived from user's quiet hours (default: 08:00 to 21:00).
   - Fixed commitments (meetings, pre-planned tasks, active habits with reminder times) are placed into busy intervals and merged.
   - Tasks are assigned earliest available free interval matching estimated duration (default: 60 min, habits: 15 min), applying a configurable inter-task buffer (default: 10 min).
   - Tasks that do not fit within the window become `UnscheduledItem`s.
8. **Step 9 — Real-time Conflict Detection:** Emits structured `PlanConflict` warnings for deadline violations (task scheduled to end after deadline), overlaps with fixed commitments, capacity exceeded, and overdue tasks.
9. **Step 10 — Output Draft Plan:** Emits chronological `DailyPlanItem` list with `orderIndex`, `unscheduledItems`, `conflicts`, and time totals.

### C. Presentation & Interaction Architecture
- **Plan Screen (`PlanViewModel`, `PlanScreen`):**
  - Status banner clearly distinguishing `DRAFT` from `APPROVED`.
  - Conflict alert banner rendering actionable warnings.
  - Chronological scheduled timeline with time badges (`HH:mm - HH:mm`), task/habit source chips, completion checkboxes, reordering controls (`Move Up`, `Move Down`), and slot time editing.
  - Unscheduled tasks section with rescheduling suggestions and a 1-tap `+ Schedule` button.
  - Sticky bottom action bar with `Regenerate` and `Approve Plan` buttons.
- **Today Screen Integration (`TodayViewModel`, `TodayScreen`):**
  - Section F "Today's Schedule" observes `DailyPlan` for the selected date.
  - If approved plan exists: renders live chronological execution timeline with completion toggles and "View Full Plan" navigation.
  - If draft plan exists: renders "Draft Plan Ready" review card.
  - If no plan exists: renders empty state with "Plan My Day" action linking to `Screen.Plan`.

---

## 12. Morning & Evening Workflows Architecture (Phase 9)

### A. Architectural Principles & Guarantees
1. **Zero-Duplicate Scheduling via System Reminders:**
   - Morning Briefing and Evening Review are modeled directly as system `Reminder` records with fixed IDs `system_morning_briefing` and `system_evening_review`.
   - Reuses the existing core reminder infrastructure (`AlarmScheduler`, `AlarmReceiver`, `RescheduleAllUseCase`, `ScheduleReconciler`, `BootReceiver`, `TimeChangeReceiver`).
   - One-shot alarms re-armed daily upon delivery (never repeating alarms), ensuring resilience against device reboot, timezone changes, and clock drift.
2. **Deterministic Domain Generation:**
   - `BriefingGenerator` is a pure Kotlin domain function with zero Android framework dependencies.
   - Summarizes priorities, deadlines, scheduled items, and habit progress exclusively from local database state.
   - Emits graceful empty state text when the day's schedule is clear without fabricating commitments.
3. **Review Decision Invariant:**
   - In `ReviewViewModel`, any unfinished task without an explicit user decision strictly defaults to `ReviewDecisionType.CARRIED_FORWARD`.
   - Never discards or silently drops unfinished work.
   - Supports 5 explicit task decisions: `COMPLETED`, `RESCHEDULED(date)`, `CARRIED_FORWARD` (+350 planner priority boost), `EXCLUDED` (omitted from auto-plan), and `CANCELLED`.
4. **Instant Tomorrow Draft Plan Generation:**
   - Completing an evening review triggers `GenerateDailyPlanUseCase(tomorrow, forceRegenerate = true)`.
   - Incorporates all evening review decisions, rescheduled tasks, newly added commitments, and scheduled habits into tomorrow's draft plan immediately.
5. **Reopenable Historical Reviews:**
   - Enforces Room unique index `Index(value = ["localDate"], unique = true)` on `daily_reviews`.
   - Users can revisit and inspect past reviews or update today's review at any time via `DateSelector`.

### B. Notification Channels & Action Dispatch
- **Daily Briefing Channel (`channel_daily_briefing`):**
  - Notification displays morning greeting and executive summary with an "Open Plan" action button.
- **Evening Review Channel (`channel_evening_review`):**
  - Notification prompts the user to review today's achievements and prepare tomorrow with a "Start Review" action button.
- **Deep Linking:**
  - Standardized intent routing in `MainActivity` routes intent extras (`EXTRA_DESTINATION = "briefing" | "review" | "plan"`) directly into the corresponding screen.

### C. Presentation Architecture
- **Morning Briefing Screen (`BriefingViewModel`, `BriefingScreen`):**
  - Greeting header with formatted date.
  - Executive summary card with task, habit, and deadline metric badges.
  - Today's schedule status card with "Open Daily Plan" action.
  - High-priority commitments and today's deadlines sections.
  - Active habits progress checklist.
- **Evening Review Screen (`ReviewViewModel`, `ReviewScreen`):**
  - 7-day `DateSelector` to reopen past reviews or navigate dates.
  - Summary card with completion ratios.
  - Unfinished tasks decision cards featuring 1-tap action chips (`Carry Forward`, `Reschedule`, `Complete`, `Exclude`, `Cancel`).
  - Completed tasks summary.
  - Habit progress checklist with target vs actual values.
  - Inline commitment creator to queue new tasks directly for tomorrow.
  - Tomorrow's plan preview card.
  - Reflection and notes field.
  - Sticky bottom action: "Complete Review & Plan Tomorrow".
- **Today Screen Integration (`TodayScreen`):**
  - Top Section A: `DailyBriefingCard` presenting the morning briefing summary.
  - Bottom Section G: Evening Review card offering 1-tap navigation to start the evening review workflow.

---

## 13. Backup and Restoration Architecture

### A. Overview & Pipeline Invariants
DAYFLOW implements an offline-first, local-only backup and restoration pipeline (`com.dayflow.app.backup`).
1. **100% Offline & Private:** Archives are stored locally in app-private storage (`files/backups`) or exported/imported via Android's Storage Access Framework (SAF). Zero network calls or external cloud dependencies.
2. **Atomic Writes with Durability:** Backups are written to a temporary file (`.tmp`), flushed to disk (`fsync`), checksum-verified against the generated manifest, and atomically renamed into the final `.zip` file using `Files.move(..., ATOMIC_MOVE)`.
3. **Rolling Retention:** Automatic periodic backups keep the newest 7 snapshots. Older snapshots are pruned only after a new snapshot is verified.
4. **Pre-Restore Safety Snapshot:** Before touching the live database, an automated safety snapshot of the existing database and preferences is created in app-private storage.
5. **Atomic Transactional Restore:** Database clearing (child-to-parent) and restoration (parent-to-child) execute inside a single Room database transaction. Any error triggers an immediate rollback, leaving the live database untouched.
6. **Post-Restore Alarm Reconciliation:** Upon successful restoration, `RescheduleAllUseCase` is immediately invoked to reconcile all alarms with Android's `AlarmManager`.

### B. Archive Schema & Structure
A DAYFLOW backup file is a standard ZIP archive containing two root entries:
- `manifest.json`: Metadata document including `schemaVersion`, `appVersion`, `createdAt` (UTC epoch millis), `dataSha256` checksum, `totalRecords`, and a breakdown map of entity counts.
- `data.json`: Versioned JSON payload (`BackupPayload`) containing:
  - `categories`: List of `BackupCategory`
  - `reminders`: List of `BackupReminder`
  - `reminderOccurrences`: List of `BackupReminderOccurrence`
  - `tasks`: List of `BackupTask`
  - `subtasks`: List of `BackupTaskSubtask`
  - `habits`: List of `BackupHabit`
  - `habitLogs`: List of `BackupHabitLog`
  - `dailyPlans`: List of `BackupDailyPlan`
  - `planItems`: List of `BackupDailyPlanItem`
  - `reviews`: List of `BackupDailyReview`
  - `appSettings`: List of `BackupAppSetting`
  - `preferences`: Key-value map of user settings from DataStore

### C. 10-Step Sequential Restore Engine
The restoration pipeline (`BackupEngine.executeRestorePipeline`) enforces 10 deterministic steps:
1. **Read Archive:** Validate ZIP structure, entry count ($\le 10$), and uncompressed size ($\le 50\text{ MB}$, compression ratio $\le 100$).
2. **Verify Checksum:** Compute SHA-256 of `data.json` and verify strict equality against `manifest.dataSha256`.
3. **Check Compatibility:** Reject archives with `schemaVersion > CURRENT_SCHEMA_VERSION`.
4. **Validate Records:** Check required fields, non-blank IDs, enum formats, and ISO date/time validity.
5. **Check Referential Integrity:** Validate foreign key relationships (subtasks $\to$ tasks, occurrences $\to$ reminders, habit logs $\to$ habits, plan items $\to$ plans, categories).
6. **Create Safety Snapshot:** Package existing database into `safety_snapshot_<timestamp>.zip`.
7. **Execute Room Transaction:** In `BackupDao.restoreAllDataTransaction`:
   - Delete children: `reminder_occurrences`, `reminders`, `task_subtasks`, `tasks`, `habit_logs`, `habits`, `daily_plan_items`, `daily_plans`, `daily_reviews`, `app_settings`, `categories`.
   - Insert parents first, then children with `OnConflictStrategy.ABORT`.
8. **Restore Settings:** Apply preferences from archive into DataStore.
9. **Reconcile Alarms:** Invoke `RescheduleAllUseCase` to reconcile alarms and schedule upcoming occurrences.
10. **Report Result:** Return `RestoreResult.Success` with total record counts and breakdown.

### D. WorkManager Periodic Scheduling
- Scheduled via `BackupWorkScheduler` registering `DayflowBackupWorker` (`PeriodicWorkRequestBuilder`, 24 hours, `ExistingPeriodicWorkPolicy.KEEP`).
- Injected through Hilt's `@HiltWorker` and `Configuration.Provider` in `DayflowApplication`.
- Records last execution time, status (`SUCCESS` / `FAILURE`), and snapshot filename into DataStore.

### E. Storage Access Framework (SAF)
- Users can export archives to any device storage directory (Downloads, SD card, USB, cloud sync) using `ActivityResultContracts.CreateDocument("application/zip")`.
- Users can import external archives using `ActivityResultContracts.OpenDocument()`.
- Prominent in-app security notice discloses that app-private snapshots do not survive app uninstall or device loss, guiding users to export archives for disaster recovery.

---

## 14. Analytics, Insights, and Calendar Architecture

### A. Core Philosophy & Invariants
1. **100% Database-Derived Metrics:** Every statistic, chart bar, trend line, streak, and completion ratio is computed strictly from persisted Room SQLite database rows and DataStore preferences. Zero mock, synthetic, or placeholder numbers exist anywhere in production screens.
2. **Deterministic Calculation:** All aggregation logic resides in pure domain use cases (`ExecutionScoreCalculator`, `GetInsightsUseCase`, `GetCalendarEventsUseCase`).
3. **Zero Stored Future Spam Invariant:** The database stores only past and currently pending reminder occurrences. Future calendar projections are generated dynamically on demand using `RecurrenceCalculator.occurrencesInRange()` and deduplicated against pending database records in memory.
4. **Accessible Visualizations:** Native Compose Canvas charts provide full semantic accessibility descriptions and text labels alongside color accents (zero color-only encoding).

### B. Daily Execution Score Deterministic Formula
The Daily Execution Score is an app-defined execution index calculated deterministically from four commitment dimensions:
- **Task Factor ($T$, Weight = 0.40):** Ratio of completed tasks to total tasks planned or due on that date. If no tasks are planned/due, $T = 1.0$.
- **Habit Consistency Factor ($H$, Weight = 0.30):** Ratio of completed habits to scheduled habits on that date (evaluated using `activeDaysMask`). If no habits are scheduled, $H = 1.0$.
- **Reminder Execution Factor ($R$, Weight = 0.15):** Ratio of completed reminder occurrences to total triggered occurrences (completed + missed). If no reminders triggered, $R = 1.0$.
- **Daily Plan Execution Factor ($P$, Weight = 0.15):** Ratio of completed approved plan items to total plan items. If no plan items exist, $P = 1.0$.

$$\text{RawScore} = (0.40 \cdot T + 0.30 \cdot H + 0.15 \cdot R + 0.15 \cdot P) \times 100$$
$$\text{ExecutionScore} = \text{round}(\text{RawScore}).\text{coerceIn}(0, 100)$$

**Edge Cases & Ethical Framing:**
- If total scheduled commitments across all categories equal zero (a completely empty day), the score defaults to $0$ with `hasActivity = false`.
- **Mandatory Ethical Framing:** The UI explicitly displays the score with an **"APP-DEFINED METRIC"** badge and an accompanying disclaimer clarifying that the score is a deterministic software execution metric derived strictly from local app logs, not a medical, psychological, or health assessment.

### C. Calendar Recurrence & Event Projection
`GetCalendarEventsUseCase` unifies five distinct commitment types into a single chronologically sorted `CalendarEvent` stream:
1. **Tasks:** Planned tasks (`plannedDate`, `plannedStart`, `plannedEnd`) and deadlines (`deadlineDate`, `deadlineTime`).
2. **Reminders:**
   - Queries stored Room occurrences (`getOccurrencesBetweenDirect`) for past and pending occurrences.
   - Computes dynamic future occurrences across the queried date range using `RecurrenceCalculator.occurrencesInRange(reminder, startInstant, endInstant, zoneId)`.
   - In-memory deduplication ensures that already stored occurrences are never duplicated by the recurrence calculator.
3. **Habits:** Evaluates each active habit's `activeDaysMask` against each day's DayOfWeek bit (`1 shl (dayOfWeek.value - 1)`). Cross-references `HabitLog` for completion status.
4. **Plan Items:** Queries approved `DailyPlan` records and extracts scheduled time blocks.

### D. Native Compose Chart Components
All charts are implemented with native Compose Canvas drawing without external charting library dependencies:
- `WeeklyBarChart`: Native Canvas 7-day bar chart with track drawing, rounded corner bars, current day highlight, percentage labels, and screen-reader accessibility descriptions.
- `TrendLineChart`: Native Canvas cubic spline trend line with background gridlines, gradient area fill (`Brush.verticalGradient`), data point markers, and average score summary.
- `CalendarHeatmap`: 7-column monthly grid aligning dynamically to the user's preferred `weekStartDay`, featuring 5 color intensity tiers (0%, 25%, 50%, 75%, 100%), interactive day selection, and a visual intensity legend.
- `DailyTimeline`: 24-hour vertical timeline displaying commitment blocks for tasks, deadlines, reminders, habits, and plan items with time chips, type badges, titles, and completion state.
- `MultiProgressRing`: Concentric multi-tier progress indicator displaying Task, Habit, and Reminder completion ratios with the central execution score.

### E. Settings & Localization Preferences
Analytics, calendar views, and date/time displays respect user-configured preferences:
- `WeekStartDay`: `MONDAY`, `SUNDAY`, or `SATURDAY` (governs weekly calendar columns, heatmap alignment, and weekly aggregation boundaries).
- `DateFormatPreference`: `SYSTEM`, `ISO_YYYY_MM_DD`, `MM_DD_YYYY`, `DD_MM_YYYY`.
- `TimeFormatPreference`: `SYSTEM`, `H12` (12-hour AM/PM), `H24` (24-hour).
- Audio and haptic feedback toggles for notification delivery.

---

## 15. Production Readiness, System Recovery, & Release Hardening

### A. System Recovery Architecture
Android system broadcasts trigger automated state reconciliation without requiring the user to open the application:
1. **Device Reboot (`BootReceiver`):**
   - Listens for `android.intent.action.BOOT_COMPLETED`.
   - Uses `BroadcastReceiver.goAsync()` with coroutine execution on `Dispatchers.IO`.
   - Invokes `RescheduleAllUseCase()`, which re-arms exact/inexact one-shot alarms in `AlarmManager` for all pending reminders and daily briefing/review schedules.
2. **Time and Timezone Drift (`TimeChangeReceiver`):**
   - Listens for `android.intent.action.TIME_SET`, `TIMEZONE_CHANGED`, and `DATE_CHANGED`.
   - Invokes `RescheduleAllUseCase()` to recalculate UTC epoch instants while preserving local wall-clock hours for recurring events.
3. **Dynamic Exact Alarm State Changes (`ExactAlarmPermissionReceiver`):**
   - Listens for `android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` (Android 12+ / API 31+).
   - Queries `AlarmScheduler.refreshCapabilities()`.
   - Dynamically transitions the `SchedulingCapability` state flow between `EXACT` and `APPROXIMATE_FALLBACK`.
   - Discloses current precision state transparently in the Reminders UI without crashing or throwing `SecurityException`.
4. **In-Place Application Upgrades (`PackageReplacedReceiver`):**
   - Listens for `android.intent.action.MY_PACKAGE_REPLACED` (un-exported receiver).
   - Re-registers periodic WorkManager tasks and re-arms pending alarms.

### B. Non-Destructive Database Schema Evolution
- **Permanent Ban on Destructive Migrations:** Calling `.fallbackToDestructiveMigration()` or `.fallbackToDestructiveMigrationOnDowngrade()` is architectural malpractice for Dayflow and is verified absent via automated CI checks (`check_no_destructive_migrations.py`).
- **Migration Registry:** All migrations are registered in `DayflowMigrations.ALL_MIGRATIONS` and wired into `DayflowDatabase` inside `DatabaseModule`.
- **Migration Scaffold (`MIGRATION_1_2`):** Demonstrates safe, non-destructive schema evolution by altering existing tables (`ALTER TABLE ... ADD COLUMN ... DEFAULT NULL`) preserving 100% of existing rows.
- **Verification:** Validated by `MigrationTest.kt` verifying that pre-existing tasks and categories remain fully intact after migration execution.

### C. Security Boundary & Cloud Auto-Backup Rejection
- **Zero Network Invariant:** Dayflow has zero network libraries (`Retrofit`, `OkHttp`, `Ktor`, `Firebase` absent) and requests zero network permissions (`android.permission.INTERNET` absent in `AndroidManifest.xml`).
- **Disabling Google Drive Cloud Auto-Backup:** `android:allowBackup="false"` is explicitly configured. Android's default cloud backup would upload unencrypted local SQLite database files and DataStore preferences to Google Drive, violating the 100% offline data sovereignty contract and risking cross-device database corruption.
- **Local Sovereignty:** Backups are strictly user-governed via local 24h rolling snapshots (maximum 7 retained) and manual Storage Access Framework (SAF) ZIP archives with SHA-256 checksums.

### D. R8 Optimization & Release Packaging
- **R8 Minification & Resource Shrinking:** `isMinifyEnabled = true` and `isShrinkResources = true` are active on release builds.
- **ProGuard Keep Rules:** `app/proguard-rules.pro` includes explicit rules preserving Kotlinx Serialization descriptors (`@Serializable`, `*$$serializer`), Room database entities and DAOs, Dagger/Hilt components, WorkManager workers, and Jetpack Compose runtime classes.
- **Flexible Signing Pipeline:** Releases are signed via `keystore.properties` or environment variables (`DAYFLOW_KEYSTORE_*`), with automatic debug fallback for continuous verification environments.
