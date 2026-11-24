# DAYFLOW — IMPLEMENTATION ROADMAP

Specification Authority: [PRD.md](file:///c:/Projects/DayFlow/docs/PRD.md), [DESIGN_SYSTEM.md](file:///c:/Projects/DayFlow/docs/DESIGN_SYSTEM.md), [AGENTS.md](file:///c:/Projects/DayFlow/AGENTS.md)

---

## Phase 1: Foundation
- [x] Initialize Android Studio project with Gradle Kotlin DSL and version catalog (`gradle/libs.versions.toml`)
- [x] Configure `minSdk = 26`, `targetSdk = 35`, `compileSdk = 35`, Java 17 toolchain
- [x] Integrate core dependencies: Jetpack Compose, Material 3, Hilt, Room, DataStore, Navigation Compose, Kotlinx Serialization
- [x] Set up clean architecture package structure (`core`, `data`, `domain`, `presentation`)
- [x] Implement centralized theme tokens and color palette (Dark forest green `#07120F`, Surface `#101D18`, Mint `#A5E6D6`, Yellow `#F0E99B`, Lavender `#DCC3F5`, Light theme tokens)
- [x] Implement typography scale (Inter / system fallback) and 4dp spacing grid
- [x] Build reusable UI foundation components (`DayflowScaffold`, `DayflowBottomNavigation`, `MetricCard`, `ProgressRing`, `MultiProgressRing`, `EmptyState`, `PermissionBanner`)
- [x] Configure type-safe Navigation Compose graph with primary bottom tabs (Today, Reminders, Tasks, Habits, Insights)
- [x] Implement Onboarding flow (Greeting preference, theme choice, briefing/review times, quiet hours, contextual permission explanations)
- [x] Verify application shell builds, launches, and navigates cleanly without crashes

---

## Phase 2: Database
- [x] Implement Room database (`dayflow.db`, version 1, exportSchema enabled)
- [x] Create database entities:
  - [x] `Category`
  - [x] `Reminder`
  - [x] `ReminderOccurrence`
  - [x] `Task`
  - [x] `TaskSubtask`
  - [x] `Habit`
  - [x] `HabitLog`
  - [x] `DailyPlan`
  - [x] `DailyPlanItem`
  - [x] `DailyReview`
  - [x] `AppSetting`
- [x] Implement DAOs with reactive Flow queries and transactional suspend operations
- [x] Implement Room TypeConverters (`LocalDateTime`, `LocalDate`, `LocalTime`, enums, lists)
- [x] Implement DataStore Preferences for user settings (theme, quiet hours, briefing/review times, alarm consent)
- [x] Implement repository interfaces and Room/DataStore implementations
- [x] Write unit and in-memory Room DAO tests for all entities and relationships

---

## Phase 3: Recurrence Engine
- [x] Implement recurrence domain models (`RecurrenceRule`, `RecurrenceType`, `ScheduleMode`)
- [x] Support recurrence types:
  - [x] One-time
  - [x] Daily
  - [x] Weekly
  - [x] Custom weekday patterns
  - [x] Fixed-clock interval (e.g., every 2 hours between 8 AM and 10 PM)
  - [x] Completion-based interval (e.g., 1 hour after previous completion)
- [x] Implement recurrence calculation engine handling:
  - [x] Midnight boundaries and date transitions
  - [x] Variable month lengths and leap years
  - [x] Timezone modifications and daylight-saving transitions
  - [x] Missed occurrences handling without burst notifications
  - [x] Disabled weekdays and schedule end dates
  - [x] Snooze logic without modifying original recurrence anchor
- [x] Write comprehensive unit tests for all recurrence patterns and calendar edge cases

---

## Phase 4: Alarms and Notifications
- [x] Create dedicated Android Notification Channels:
  - [x] Critical deadlines (`IMPORTANCE_HIGH`)
  - [x] Routine reminders (`IMPORTANCE_DEFAULT`)
  - [x] Daily briefing (`IMPORTANCE_LOW` / `DEFAULT`)
  - [x] Evening review (`IMPORTANCE_LOW` / `DEFAULT`)
  - [x] General notifications
- [x] Implement one-shot AlarmManager scheduling (`setExactAndAllowWhileIdle` with fallback to `setAndAllowWhileIdle` when permission absent)
- [x] Handle exact alarm permission checks (`canScheduleExactAlarms`) and disclose inexact fallback transparently via `SchedulingCapability`
- [x] Implement idempotent notification delivery with unique occurrence IDs to prevent duplicates
- [x] Implement BroadcastReceivers:
  - [x] AlarmReceiver for scheduled delivery with post-delivery next-slot calculation
  - [x] BootReceiver (`ACTION_BOOT_COMPLETED`, `ACTION_LOCKED_BOOT_COMPLETED`) for schedule recovery
  - [x] TimeChangeReceiver (`ACTION_TIME_SET`, `ACTION_TIMEZONE_CHANGED`, `ACTION_DATE_CHANGED`) for schedule reconciliation
  - [x] ExactAlarmPermissionReceiver (`ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`) for runtime permission adaptation
  - [x] PackageReplacedReceiver (`ACTION_MY_PACKAGE_REPLACED`) for post-upgrade alarm validation
- [x] Implement notification action buttons (Complete, Snooze, Skip, Open deep link) with unique PendingIntents and dismissal handling
- [x] Respect quiet hours settings during notification triggering (silence non-urgent, preserve urgent)

---

## Phase 5: Reminder UI
- [x] Implement Reminders list screen (active, paused, upcoming occurrences, recurrence type badge, next delivery time)
- [x] Implement Reminder creation and edit screen:
  - [x] One-time, fixed-interval, completion-interval
  - [x] Custom weekday selection
  - [x] Start/end times and recurrence boundaries
  - [x] Priority and exact/approximate preference
- [x] Implement user actions: Complete, Snooze, Skip, Pause, Resume, Edit, Delete (with confirm dialog)
- [x] Implement stale alarm cancellation on edit/delete
- [x] Implement permission banner for notifications and exact alarms with direct settings navigation
- [x] Implement Today screen integration with real upcoming reminders and single-tap Complete action
- [x] Write ViewModel unit tests, Compose UI tests, and required working behaviour validation tests (water reminder every 2h 8AM-10PM & hourly completion push-ups)

---

## Phase 6: Tasks
- [x] Implement Task list screen with filter tabs (Today, Upcoming, Overdue, Completed, All)
- [x] Implement Task creation and edit screen:
  - [x] Title, description, category, priority (Urgent, High, Medium, Low)
  - [x] Deadline, estimated duration, planned execution window
  - [x] Subtask creation and ordering
  - [x] Carry-forward preference
- [x] Implement subtask tracking (independent completion states)
- [x] Implement derived overdue status calculation (pure function, never stored in DB)
- [x] Implement task completion, cancellation, and rescheduling (preserving task identity)
- [x] Maintain strict independence between task deadline and planned execution time
- [x] Implement configurable deadline reminders via Reminder pipeline reuse on Critical Deadlines channel
- [x] Implement Today screen integration (Priority Tasks, Upcoming Deadlines, Daily Execution progress ring from real data)
- [x] Write unit tests for task domain logic, ViewModels, and Compose UI tests, verifying Example 3 (assignment due tomorrow)

---

## Phase 7: Habits
- [x] Implement Habit management (create, edit, disable, delete)
- [x] Support 4 habit tracking types:
  - [x] Binary (Done / Not done)
  - [x] Quantity (e.g., glasses of water, target value + unit)
  - [x] Duration (e.g., minutes spent)
  - [x] Repetition (e.g., push-ups, count)
- [x] Build daily habit logging interface with fast logging controls (QuickLogBottomSheet)
- [x] Implement streak calculation algorithm (evaluating scheduled active days without penalizing inactive days)
- [x] Implement weekly calendar and monthly history visualization for habits (WeeklyDotsStrip, HabitCalendar heatmap)
- [x] Ensure notification dismissal never falsely records habit completion
- [x] Link habit reminder preferences to the core reminder pipeline via HabitReminderScheduler (single source of scheduling truth)
- [x] Integrate Habit Overview on Today screen with real completion metrics
- [x] Write unit tests for streak calculations, progress calculators, schedulers, ViewModels, and Compose UI tests

---

## Phase 8: Planner
- [x] Implement deterministic daily planning algorithm:
  - [x] Ingest pending tasks, deadlines, priorities, estimated durations, user availability, habits, review decisions
  - [x] Identify overdue tasks and today's deadlines
  - [x] Detect scheduling conflicts and overlaps
  - [x] Calculate priority scores and suggest time slots
  - [x] Generate reproducible draft plan for the day
- [x] Build semi-automatic approval UI:
  - [x] Review proposed time slots and task ordering
  - [x] Adjust times, reorder, remove items, add unscheduled tasks
  - [x] Approve draft to make it the active daily plan
- [x] Ensure planner never automatically mutates task deadlines or silently drops tasks
- [x] Write unit tests verifying deterministic planning behavior and conflict detection

---

## Phase 9: Briefing and Review
- [x] Morning Briefing:
  - [x] Scheduled trigger (default 7:00 AM, user-configurable via Settings and one-shot alarms)
  - [x] Morning briefing screen, notification (`channel_daily_briefing`), and dashboard card
  - [x] Displays today's plan summary, priorities, deadlines, and habit checklist
  - [x] Gracefully handles empty schedules without fabricating data
- [x] Evening Review:
  - [x] Scheduled trigger (default 9:30 PM, user-configurable via Settings and one-shot alarms)
  - [x] Review screen displaying completed tasks, unfinished tasks, habit status, missed items
  - [x] Review action handling for each unfinished task: Complete, Reschedule, Carry forward, Exclude, Cancel
  - [x] Invariant enforced: undecided unfinished tasks default to Carry Forward (+350 planner boost)
  - [x] Persist review decisions by date (single review record per local date, reopenable)
  - [x] Feed evening review outcomes into tomorrow's draft plan via deterministic planner
- [x] Write unit tests for briefing summary generation, review state transitions, and Workflow D E2E verification

---

## Phase 10: Backup
- [x] Implement periodic automatic local backups using WorkManager (every 24 hours, retaining 7 rolling snapshots)
- [x] Implement backup packaging:
  - [x] Versioned JSON serialization (Reminders, Occurrences, Tasks, Subtasks, Categories, Habits, Logs, Plans, Reviews, Settings)
  - [x] ZIP archive compression in application-private storage
  - [x] SHA-256 integrity checksum verification
  - [x] Atomic file replacement via temporary files and fsync
- [x] Implement manual backup export and import via Storage Access Framework (SAF)
- [x] Implement safe transactional restoration engine:
  - [x] Archive integrity check (SHA-256 validation)
  - [x] Schema version compatibility check
  - [x] Safety snapshot creation prior to overwriting database
  - [x] Transactional database replacement
  - [x] Alarm and notification schedule reconciliation post-restore
  - [x] Error reporting and fallback rollback on corrupt/incompatible archives
- [x] Write unit tests for serialization, checksum verification, and restore safety
- [x] Implement Backup & Restore Management UI screen with honest data loss disclosure notice

---

## Phase 11: Analytics and Calendar
- [x] Today Dashboard screen:
  - [x] Personalized greeting & horizontal 7-day date selector
  - [x] Daily Execution card with circular progress ring
  - [x] Habit Overview card with completion progress
  - [x] Upcoming Reminders card with completion action
  - [x] Priority Tasks card with deadline badges
  - [x] Productivity Summary card (weekly completion, current streak)
  - [x] Chronological Today's Schedule timeline
  - [x] Real-time updates from database flows (no hardcoded statistics)
- [x] Calendar screen:
  - [x] Day, week, and month view modes
  - [x] Unified display of tasks, deadlines, reminders, habit logs, and approved plan items
  - [x] Consistent color-coded event indicators with zero color-only encoding
  - [x] Dynamic future recurrence projection using RecurrenceCalculator (zero stored future spam)
  - [x] Deep-link navigation to Task, Reminder, Habit, and Plan detail screens
- [x] Insights & Analytics screen:
  - [x] App-defined Daily Execution Score (explicitly disclaimed, non-medical)
  - [x] Weekly and monthly task completion rates
  - [x] Habit consistency metrics and streak history (scheduled-day based)
  - [x] Native Compose Canvas charts (concentric progress rings, bar charts, line charts, calendar heatmap)
  - [x] Empty state handling when history is limited
- [x] Settings Enrichment (FR-12):
  - [x] Week start day preference (Monday, Sunday, Saturday)
  - [x] Date format preference (System, ISO yyyy-MM-dd, MM/dd/yyyy, dd/MM/yyyy)
  - [x] Time format preference (System, 12-hour AM/PM, 24-hour)
  - [x] Notification sound and vibration toggles
  - [x] Default reminder precision and snooze duration controls

---

## Phase 12: Production Readiness
- [x] Compliance Audit:
  - [x] Verified zero `android.permission.INTERNET`
  - [x] Verified zero `FOREGROUND_SERVICE`
  - [x] Verified zero repeating alarms (`setRepeating` absent, one-shot reconciliation only)
  - [x] Verified zero destructive migrations (`fallbackToDestructiveMigration` banned)
  - [x] Verified zero Firebase / network libraries
  - [x] Verified zero `TODO`, `mock`, `fake` placeholder data in production code (`app/src/main`)
- [x] System Recovery & Broadcast Receivers:
  - [x] `BootReceiver`: restores and reconciles all active alarms on `BOOT_COMPLETED`
  - [x] `TimeChangeReceiver`: recalculates occurrences on `TIME_SET`, `TIMEZONE_CHANGED`, `DATE_CHANGED`
  - [x] `ExactAlarmPermissionReceiver`: handles `SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`
  - [x] `PackageReplacedReceiver`: re-arms alarms and WorkManager tasks on `MY_PACKAGE_REPLACED`
  - [x] Comprehensive unit tests for all recovery receivers (`BootReceiverTest`, `TimeChangeReceiverTest`, `ExactAlarmPermissionReceiverTest`, `PackageReplacedReceiverTest`)
- [x] Non-Destructive Database Migrations:
  - [x] Implemented `MIGRATION_1_2` non-destructive schema evolution scaffold in `DayflowMigrations.kt`
  - [x] Registered in `ALL_MIGRATIONS` and wired into `DatabaseModule.kt`
  - [x] Verified data preservation with `MigrationTest.kt` using `MigrationTestHelper`
- [x] Reliability & StrictMode:
  - [x] Enabled StrictMode in `DayflowApplication.kt` for debug builds (disk/network thread policy, leaked SQLite/closable VM policy)
  - [x] Implemented structured, PII-free logger `DayflowLogger.kt`
- [x] Accessibility & Responsive Layouts:
  - [x] Touch target sizes >= 48dp on all interactive elements
  - [x] System font scaling (up to 200%) via scalable `sp` typography and fluid column/row weights
  - [x] Semantic content descriptions on all charts (`WeeklyBarChart`, `TrendLineChart`, `CalendarHeatmap`, `DailyTimeline`, `MultiProgressRing`)
  - [x] WCAG AA/AAA contrast ratios verified in dark (`#121212`) and light (`#FAFAFA`) themes
  - [x] Reduced-motion support via instant transitions (`EnterTransition.None` / `ExitTransition.None`)
- [x] Security & Offline Boundary:
  - [x] Disabled Google Drive auto-backup (`android:allowBackup="false"`) with documented offline privacy rationale
  - [x] Adaptive vector launcher icon with mint `#A5E6D6` branding on dark background
- [x] R8 Optimization & Release Build:
  - [x] Configured `isMinifyEnabled = true`, `isShrinkResources = true`
  - [x] Populated `app/proguard-rules.pro` with keep rules for Room, Hilt, Kotlinx Serialization, and WorkManager
  - [x] Configured release signing with `keystore.properties` / environment variables and verification fallback
- [x] Documentation & Checklists:
  - [x] Created `RELEASE_CHECKLIST.md` with complete OEM battery optimization matrix (Xiaomi/HyperOS, Samsung OneUI, OnePlus/OxygenOS, Huawei EMUI, Pixel Doze)
  - [x] Synchronized all 11 required project documents in root directory

---

## Phase 13: Web-First Architecture & Desktop Browser Preview
- [x] Architectural pivot to Web-First development:
  - [x] Removed Android emulator / AVD UI development workflow
  - [x] React 18+, TypeScript, Tailwind CSS, Lucide React, Recharts, Zustand, Zod stack
  - [x] Vite 6 development server with HMR at `http://localhost:5173`
  - [x] Mobile frame and responsive desktop preview
- [x] Local Storage & Repositories:
  - [x] IndexedDB persistence via Dexie for 10 entities (Categories, Tasks, Subtasks, Habits, HabitLogs, Reminders, ReminderOccurrences, DailyPlans, DailyReviews, UserSettings)
  - [x] Idempotent seed for default categories and initial settings
  - [x] Versioned JSON export and safe transactional restore
- [x] Pure Domain Engines:
  - [x] RecurrenceCalculator (one-time, daily, weekly, custom weekdays, interval)
  - [x] StreakCalculator (scheduled-day based, inactive weekday preservation)
  - [x] DeterministicPlanner (10-step priority scoring, availability slots, unscheduled task safety)
  - [x] BriefingGenerator (pure summary derived from stored records)
- [x] UI & Interactive Screens:
  - [x] Today: Execution Score ring, Morning Briefing card, Today's Schedule timeline, Habit Overview, Evening Review banner
  - [x] Reminders: Active/Paused tabs, next 5 occurrences calculation modal, create reminder modal
  - [x] Tasks: All/Today/Upcoming/Overdue/Completed tabs, priority badges, category pills, subtasks checklist
  - [x] Habits: Daily habit list, streak counters, weekly dots strip, monthly heatmap matrix, quick-log sheet
  - [x] Insights: App-defined Daily Execution Score, metrics grid, accessible weekly bar chart
  - [x] Calendar: Day/Week/Month views, month matrix, agenda timeline, "Jump to Today"
  - [x] Settings: Day start/end hours, week start, 12h/24h time format, JSON backup export & restore
  - [x] Evening Review: Per-task actions (Carry Forward, Reschedule, Complete, Exclude, Cancel), reflection notes, "Complete Review & Generate Tomorrow's Plan"
- [x] Verification & Tests:
  - [x] 100% passing Vitest pure domain suite (6 tests)
  - [x] Zero TypeScript compilation errors (`tsc --noEmit`)
  - [x] Clean Vite production build (`npm run build`)
  - [x] End-to-end browser QA testing across all 9 core workflows via browser subagent with real DOM evidence
