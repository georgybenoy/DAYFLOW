# DAYFLOW — TESTING.md

## 1. Testing Strategy
DAYFLOW enforces a comprehensive, multi-layer testing strategy prioritizing business logic correctness, data integrity, and deterministic scheduling.

```
       /  UI & Integration Tests  \      <- Compose UI Tests, On-Device E2E
      /     Room DAO & DB Tests    \     <- In-memory Room tests, Migrations
     /    ViewModel & State Tests   \    <- Turbine, Coroutines Test
    /   Domain Logic & Algorithmic   \   <- Recurrence Engine, Planner Tests
   /───────────────────────────────────\
```

## 2. Unit Testing Stack
- **JUnit 4 (`4.13.2`):** Primary test framework.
- **MockK (`1.13.13`):** Idiomatic Kotlin mocking library.
- **Turbine (`1.2.0`):** Deterministic testing library for Kotlin `Flow` and `StateFlow`.
- **Kotlinx Coroutines Test (`1.9.0`):** `runTest`, `StandardTestDispatcher`, and `TestScope` for virtual-time execution.

## 3. Test Layers

### A. Domain & Algorithmic Testing
- **Recurrence Engine Tests (`RecurrenceCalculatorTest`):**
  - Verification across all types: `ONE_TIME`, `DAILY`, `WEEKLY`, `CUSTOM_WEEKDAYS`, `FIXED_INTERVAL`, `COMPLETION_INTERVAL`.
  - Water example: verifies 08:00 to 22:00 with 120-minute interval yields exact 8 occurrences (8, 10, 12, 14, 16, 18, 20, 22).
  - Midnight-crossing windows (e.g. 22:00 to 04:00 next day).
  - Calendar boundaries: month-end transitions (Jan 31 -> Feb 1) and Feb 29 leap years (2024 leap vs 2025 non-leap).
  - Timezone change: preserves local wall-clock time while recomputing UTC epoch instants.
  - Daylight-Saving transitions: DST spring-forward gap in `America/New_York` (02:30 -> 03:30) and `Europe/London` (01:30 -> 02:30), DST fall-back overlap in `America/New_York` (01:30 repeated), and `Asia/Kolkata` constant offset.
  - End date inclusive rule: occurrences on `endDate` allowed; occurrences after `endDate` return null.
  - Boundary guards: created after window rollover, all weekdays disabled (`null`), and minimum 1-minute interval guard.
  - Skip policy: `nextAfterSkip` advances from `scheduledAt` for completion intervals to prevent cadence drift.
  - Snooze non-mutation: produces transient `SNOOZED` occurrence without modifying reminder definition.
  - Notification burst prevention: `reconcileMissed` logs older slots as `MISSED` and returns at most ONE catch-up occurrence.
  - Property tests: `result > after`, strictly inside active window, and 100% deterministic over 100 iterations.
- **Differential Schedule Reconciler Tests (`ScheduleReconcilerTest`):**
  - Differential scheduling: `CreateOccurrence`, `CancelOccurrence`, `MarkOccurrenceMissed`, `KeepOccurrence`.
  - Duplicate suppression, orphaned occurrence cancellation, and idempotency across repeated runs.
- **Planner Engine Determinism:** Conflict resolution, priority ordering, and empty schedule fallbacks.

### B. ViewModels & UI State Testing
- Test that user actions produce the exact expected sequence of `UiState` emissions via Turbine.
- Main dispatcher is replaced with `StandardTestDispatcher` in `@Before` setup and reset in `@After`.

### C. Data Persistence Testing
- **Room DAOs & Database (Robolectric JVM):**
  - Run with `RobolectricTestRunner` for rapid, deterministic execution on the JVM without emulator dependencies.
  - Tested using `Room.inMemoryDatabaseBuilder(context, DayflowDatabase::class.java).allowMainThreadQueries().build()`.
  - CRUD operations, unique index constraints, composite key idempotency, and foreign-key cascade deletions are tested across all 11 tables.
  - Atomicity of multi-row mutations verified with `TransactionRollbackTest` ensuring changes roll back completely on mid-transaction exceptions.
  - Type converter roundtrip fidelity tested across epoch millis UTC instants, ISO strings, zone IDs, and domain enums.
  - Category seeding verified upon initial database creation.
- **Migration & Schema Testing:**
  - Automated `MigrationTest` using `androidx.room.testing.MigrationTestHelper` verifies schema v1 existence and database instantiation.
  - Exported schemas are tracked in `app/schemas/` and integrated into test assets via Gradle.
  - Automated script `scripts/check_no_destructive_migrations.py` scans the entire project to guarantee `fallbackToDestructiveMigration` is never called.
- **DataStore:**
  - Preferences DataStore tested using isolated temporary file-backed instances and dedicated coroutine test scopes.

### D. Compose UI Testing
- Tests written using `createComposeRule()`.
- Verifies accessibility, node tags, clicking behaviors, and visual state transitions.

### E. Alarms, Receivers & Notification Testing (Phase 4)
- **Robolectric Receiver & Scheduler Tests:**
  - `AndroidAlarmSchedulerTest`: Verifies exact scheduling via `setExactAndAllowWhileIdle`, inexact fallback to `setAndAllowWhileIdle` when `SCHEDULE_EXACT_ALARM` permission is absent, alarm cancellation, and reactive capability state emission (`EXACT`, `APPROXIMATE_FALLBACK`).
  - `AlarmReceiverTest`: Verifies idempotent delivery (delivers once, suppresses duplicates), quiet hours suppression/silencing for non-urgent reminders while allowing urgent reminders, and pausing automatic advancement for completion-interval reminders until user interaction.
  - `NotificationActionReceiverTest`: Verifies action buttons (Complete, Snooze, Skip, Dismiss). Confirms snoozing never alters reminder anchor, and dismissal never marks reminders completed.
  - `RescheduleAllUseCaseTest`: Verifies recovery on device boot or time change, ensuring missing future occurrences are regenerated and past unhandled occurrences are cleanly marked `MISSED`.

### F. Reminder Feature & UI Testing (Phase 5)
- **Required Working Behaviour Tests (`ReminderWorkingBehavioursTest`):**
  - *Water Reminder (Every 2h 8AM-10PM):* Confirms fixed-interval reminders generate exactly 8 daily slots (08:00, 10:00, 12:00, 14:00, 16:00, 18:00, 20:00, 22:00) and roll over cleanly to 08:00 on the following active day when the window elapses.
  - *Hourly Completion Push-ups (10:15 -> 11:15):* Confirms completion-interval reminders compute subsequent occurrences strictly from the completion timestamp (`10:15 + 60m = 11:15`), update SQLite transactionally, and schedule alarms via `AlarmScheduler`.
- **Validation Engine Tests (`ReminderValidatorTest`):**
  - Confirms blank titles are rejected with `ReminderValidationError.BlankTitle`.
  - Confirms intervals < 1m are rejected with `ReminderValidationError.IntervalTooShort`.
  - Confirms custom weekdays with 0 active days are rejected with `ReminderValidationError.NoWeekdaysSelected`.
  - Confirms end dates prior to start dates are rejected with `ReminderValidationError.EndDateBeforeStartDate`.
  - Confirms valid one-time, daily, and interval schedules return `ReminderValidationResult.Valid`.
- **ViewModel Architecture Tests:**
  - `RemindersViewModelTest`: Tests dynamic segregation of active vs. paused reminders, upcoming occurrence projections, permission capability state, and lifecycle operations (`pauseReminder`, `resumeReminder`, `deleteReminder`).
  - `ReminderCreateViewModelTest`: Tests form state initialization, edit mode hydration from repository, live 5-occurrence preview computation via `RecurrenceCalculator`, and atomic persistence with `ScheduleReminderUseCase`.
  - `ReminderDetailViewModelTest`: Tests occurrence timeline sorting, active occurrence detection, and interactive actions (`completeActiveOccurrence`, `snoozeActiveOccurrence`, `skipActiveOccurrence`, `togglePause`, `deleteReminder`).
  - `TodayViewModelTest`: Tests upcoming reminders integration and completion delegation to `CompleteReminderOccurrenceUseCase`.
- **Compose UI Tests (`ReminderCardComposeTest`):**
  - Verifies `ReminderCard` rendering with title, recurrence summary, next delivery time, `PriorityBadge`, precision indicator (`Exact`), and accessible overflow menu (`Edit`, `Pause`, `Delete`).

### G. Phase 6: Task Management Tests
- **Pure Overdue Derivation (`DeriveOverdueTest`):**
  - Exhaustive boundary verification across 9 cases: null deadlines, non-pending statuses (`COMPLETED`, `CANCELLED`), past deadlines, future deadlines, same-day before deadline time, same-day exact deadline time, same-day after deadline time, and same-day with no deadline time specified (not overdue on the date, becomes overdue on midnight rollover).
- **Rescheduling & Identity Preservation (`TaskRescheduleTest`):**
  - Asserts that rescheduling planned execution times (`plannedDate`, `plannedStart`, `plannedEnd`) strictly preserves the original task ID and leaves `deadlineDate` and `deadlineTime` untouched.
  - Asserts that explicit deadline modification (`ChangeTaskDeadlineUseCase`) updates deadline fields and recalculates/reschedules deadline reminders.
- **Subtask Independence (`SubtaskIndependenceTest`):**
  - Asserts that toggling a subtask modifies only that subtask's `isCompleted` field.
  - Asserts that completing 100% of subtasks does NOT automatically mark the parent task `COMPLETED`.
  - Asserts that the parent task can be completed or reopened independently.
- **Cascade Deletion (`TaskCascadeDeletionTest`):**
  - Asserts that deleting a task cancels and deletes its scheduled deadline reminders and occurrences, cascading to subtasks.
- **Example 3 / Workflow C Verification (`TaskExample3VerificationTest`):**
  - Validates PRD Example 3: User creates an assignment due tomorrow with "1 day before" and "at deadline" reminders.
  - Asserts that the assignment immediately qualifies for the `Upcoming` tab filter.
  - Asserts that deadline reminders are scheduled with `Priority.URGENT`, directing them to `CHANNEL_CRITICAL_DEADLINES`.
  - Asserts that completing the task immediately cancels all scheduled deadline alarms.
- **Task ViewModels:**
  - `TasksViewModelTest`: Tests tab segregation (Today, Upcoming, Overdue, Completed, All), category filtering, priority filtering, and action delegation.
  - `TaskCreateViewModelTest`: Tests validation errors (`BlankTitle`, `InvalidPlannedTimeWindow`), subtask management, and edit mode hydration.
  - `TaskDetailViewModelTest`: Tests task details, subtask toggling, complete/reopen, cancel, and delete workflows.
  - `TodayViewModelTest`: Tests real daily execution calculations (`tasksCompletedCount / totalTasksCount`), priority tasks, and upcoming deadlines.
- **Compose UI Tests (`TaskCardComposeTest`):**
  - Verifies `TaskCard` rendering with title, `PriorityBadge` (displaying both icon and text, satisfying non-color-alone accessibility), category badge, deadline display, subtask progress indicator, and complete checkbox interaction.

### H. Habit Engine Tests (Phase 7)
- **Domain Calculators (`HabitProgressCalculatorTest`, `StreakCalculatorTest`):**
  - `HabitProgressCalculatorTest`: Tests binary, quantity, duration, and repetition completion evaluation, ratio calculation (clamped [0.0..1.0]), and string formatting with and without units.
  - `StreakCalculatorTest`:
    - Zero streaks on empty logs.
    - Consecutive active days increment streak.
    - Inactive weekdays skipped without breaking or extending streak.
    - Current day pending execution does NOT break the streak until midnight.
    - Missed scheduled days in the past break the current streak.
    - Retroactive log updates re-evaluate streaks correctly.
    - Historical longest streak computation.
    - Weekly (7-day) and monthly (all days) history aggregations.
- **Reminder Pipeline Integration (`HabitReminderSchedulerTest`):**
  - Verifies synchronizing habit reminder preference with `ScheduleReminderUseCase` (`type = CUSTOM_WEEKDAYS`, `priority = MEDIUM`, `precision = EXACT`).
  - Verifies disabling habit or reminder preference cancels and deletes reminder occurrences via `DeleteReminderUseCase`.
- **ViewModels (`HabitsViewModelTest`, `HabitCreateViewModelTest`, `HabitDetailViewModelTest`, `TodayViewModelTest`):**
  - `HabitsViewModelTest`: Tests habit loading with categories, logs, daily progress calculations (`completed / total scheduled`), binary toggle, quick log saving, and lifecycle actions.
  - `HabitCreateViewModelTest`: Tests validation error on blank title, dynamic target/unit defaults on tracking type changes, weekday bitmask manipulation, and habit persistence.
  - `HabitDetailViewModelTest`: Tests detail UI state loading, streak and history data, binary logging, quick log saving, and habit deletion.
  - `TodayViewModelTest`: Tests real habit overview metric computation (`habitsCompletedCount / totalHabitsCount`).
- **Compose UI Tests (`HabitCardComposeTest`):**
  - Verifies rendering of habit title, description, category chip, tracking type tag, streak badge (`🔥 7d`), formatted progress, weekly dots strip, completion toggle button, and overflow dropdown menu actions.

### I. Phase 8 — Daily Planner Verification
- **Deterministic Planning Engine (`DeterministicDailyPlannerTest`):**
  - **Determinism:** Verifies identical input evaluated twice yields byte-identical scheduled ordering, time slots, unscheduled items, and conflicts.
  - **Priority & Deadline Ordering:** Verifies urgent and due-today tasks receive high priority scores and are scheduled into earlier slots before medium and low tasks.
  - **No-Dropped-Task Invariant:** Verifies `scheduledItems.size + unscheduledItems.size == total eligible tasks`, with explanatory reason and rescheduling suggestions for all unscheduled items.
  - **Conflict Detection:** Tests detection of fixed commitment overlaps, deadline violations (scheduled finish after deadline), capacity exceeded, and overdue tasks.
  - **Review Decisions Influence:** Verifies `EXCLUDED` tasks are completely omitted, and `CARRIED_FORWARD` tasks receive a score boost.
  - **Edge Cases:** Verifies graceful handling of empty tasks, closed availability windows (start >= end), full-day blocking commitments, and active habits at fixed reminder times.
  - **Deadline Immutability:** Verifies task `deadlineDate` and `deadlineTime` are never altered by the planner.
- **ViewModels (`PlanViewModelTest`):**
  - Verifies initial plan generation, loading state, and draft status.
  - Verifies `moveItemUp` and `moveItemDown` reorder items and adjust consecutive time slots.
  - Verifies `updateItemTime` modifies slot duration and recomputes real-time conflicts.
  - Verifies `removeItemToUnscheduled` moves an item to the unscheduled list while guaranteeing the underlying task is never deleted.
  - Verifies `scheduleUnscheduledTask` moves a task into the schedule and updates conflict state.
  - Verifies `approvePlan` transitions plan status to `PlanStatus.APPROVED` and persists changes.
- **Compose UI Tests (`PlanScreenComposeTest`):**
  - Verifies `PlanStatusHeader` renders `DRAFT PLAN` status and item count.
  - Verifies `ConflictBanner` displays structured warning items.
  - Verifies `PlanItemCard` displays item title, monospace time badges (`09:00 - 10:30`), source type tag, and reorder controls.
  - Verifies `UnscheduledTaskCard` renders reason, suggestion, and triggers `+ Schedule` action.

### J. Phase 9: Morning and Evening Workflows
- **Pure Domain Generator (`BriefingGeneratorTest`):**
  - Verifies morning greeting incorporates user name or fallback.
  - Verifies summary text highlights priority tasks, deadlines, and habits without fabricating data.
  - Verifies priority commitments only include pending `URGENT` and `HIGH` tasks for today, sorted by priority then deadline time.
  - Verifies today's deadlines only include pending tasks due today.
  - Verifies habit checklist evaluates active days and formats progress ratios across all tracking types.
  - Verifies graceful empty text when the day's schedule is completely open.
  - Verifies briefing does not duplicate the detailed plan schedule.
- **Workflow Scheduler (`WorkflowReminderSchedulerTest`):**
  - Verifies morning briefing (`system_morning_briefing`) and evening review (`system_evening_review`) system reminders are created.
  - Verifies one-shot alarms are scheduled via `AlarmScheduler` at configured times.
  - Verifies `rearmNextOccurrence` marks current occurrence delivered and arms next occurrence.
  - Verifies `syncWorkflowTimes` updates reminder times and reschedules pending alarms.
- **Review ViewModel (`ReviewViewModelTest`):**
  - Verifies loading review data for target date (completed tasks, pending unfinished tasks, habit status).
  - **Strict Invariant Verification:** Verifies all undecided unfinished tasks strictly default to `ReviewDecisionType.CARRIED_FORWARD`.
  - Verifies updating task decisions (`COMPLETED`, `RESCHEDULED`, `CANCELLED`, `EXCLUDED`, `CARRIED_FORWARD`).
  - Verifies `addInlineCommitment` creates and persists a new task planned for tomorrow with high priority.
  - Verifies `finishReview` applies decisions to tasks, persists `DailyReview` record, and triggers `generateDailyPlanUseCase` for tomorrow.
- **End-to-End Workflow D Verification (`Example4VerificationTest`):**
  - End-to-end simulation of PRD Workflow D / Example 4:
    1. Initial state: 4 tasks completed, 1 habit missed (Meditation), 1 task unfinished ("Prepare Q3 Report").
    2. Reschedules "Prepare Q3 Report" to tomorrow.
    3. Adds new inline commitment for tomorrow: "Review Q3 Deck with Team" (Priority High, 45m).
    4. Completes review and executes deterministic daily planner for tomorrow.
    5. Validates tomorrow's draft plan contains the fixed standup at 09:30, rescheduled "Prepare Q3 Report", new task "Review Q3 Deck with Team", and morning run habit.
- **Compose UI Tests (`ReviewScreenComposeTest`):**
  - Verifies `UnfinishedTaskReviewCard` renders title, deadline, priority, and interactive decision chips.
  - Verifies clicking decision chips selects decisions.
  - Verifies `ReviewSummaryCard` displays completed, unfinished, and habit counts.
  - Verifies `TomorrowPlanPreviewCard` displays tomorrow's date, carried forward, rescheduled, and new commitment counts.

### K. Phase 10: Backup & Restoration Test Suite

- **Pure Serialization & Integrity Tests (`BackupEngineTest`):**
  - `round-trip packaging and extraction preserves all entities losslessly`: Full payload with 11 entity types and preferences round-trips through ZIP packaging, SHA-256 verification, and deserialization with zero data loss.
  - `writeBackupToStream streams archive properly`: Direct streaming to `OutputStream` produces verified archive and matching manifest.
  - `corrupt ZIP archive throws InvalidZipArchive`: Rejects truncated or malformed ZIP archives.
  - `tampered data json causes checksum mismatch`: Detecting modified `data.json` against `manifest.dataSha256`.
  - `unsupported newer schema version is detected`: Rejects archives with `schemaVersion > 1`.
  - `broken foreign key in task subtask throws BrokenReferenceConstraint`: Detects orphan subtask.
  - `broken foreign key in reminder occurrence throws BrokenReferenceConstraint`: Detects orphan occurrence.
  - `broken foreign key in habit log throws BrokenReferenceConstraint`: Detects orphan log.
  - `broken foreign key in plan item throws BrokenReferenceConstraint`: Detects orphan plan item.
  - `invalid date format in record throws InvalidRecordData`: Validates ISO date parsing.
  - `pruneOldSnapshots keeps exactly 7 newest snapshots and deletes older`: Retains newest 7 and cleans older.
  - `cleanupTempFiles deletes temporary files and keeps snapshots`: Prunes orphaned `.tmp` files.

- **Transactional Restoration Tests (`BackupRestoreTransactionTest`):**
  - `successful restore executes safety snapshot, transaction, settings, and alarms reconciliation`: Verifies the 10-step sequence, safety snapshot creation, single Room transaction, DataStore settings restoration, and `RescheduleAllUseCase` execution.
  - `safety snapshot failure aborts restore and leaves database untouched`: Disk/IO failure during safety snapshot creation halts restoration before Room database is touched.
  - `database transaction failure rolls back and returns DatabaseTransactionFailed`: Room transaction failure rolls back changes and prevents alarm reconciliation.

- **Presentation Tests (`BackupViewModelTest`):**
  - `initial state loads preferences and snapshots list`: Observes DataStore backup keys and existing local snapshots.
  - `createBackupNow executes snapshot creation and refreshes snapshots`: Triggers local backup creation and updates UI state.
  - `promptRestoreSnapshot sets pendingRestoreSnapshot and confirm triggers restore`: Handles confirmation dialog flow and snapshot restoration.
  - `cancelRestorePrompt clears pending restore snapshot`: Cancels restore dialog.
  - `restoreSnapshot failure exposes restoreFailure in uiState`: Surfacing `BackupRestoreError` in UI state.
  - `promptDeleteSnapshot and confirmDeletePendingSnapshot calls deleteSnapshot`: Handles delete confirmation flow.
  - `dismissResultDialog clears success and failure dialog states`: Dismisses result dialogs.

- **Phase 11: Analytics & Insights Tests:**
  - `ExecutionScoreCalculatorTest`:
    - `zeroCommitments_returnsZeroScoreAndNoActivity`: Evaluates zero activity edge case yielding 0 score and `hasActivity = false`.
    - `perfectCompletionAcrossAllCategories_returns100`: Verifies 100% completion across all 4 factors produces maximum score.
    - `taskOnlyActivity_calculatesAccurately`: Verifies dynamic weight normalization when habits/reminders/plans are not scheduled.
    - `partialCompletionAcrossCategories_computesWeightedScore`: Verifies weighted formula $(0.40T + 0.30H + 0.15R + 0.15P) \times 100$.
    - `zeroCompletionAcrossAllScheduled_computesCorrectly`: Verifies 0% completion across scheduled commitments produces 0 score with `hasActivity = true`.
  - `GetInsightsUseCaseTest`:
    - `invoke_withEmptyDatabase_returnsZeroAnalyticsWithoutCrashing`: Verifies graceful empty database handling across week, month, and trend ranges.
    - `invoke_withCompletedTasksAndScheduledHabits_computesAccurateInsights`: Tests end-to-end aggregation of tasks, habits, streaks, and weekly consistency.
  - `InsightsViewModelTest`:
    - `loadInsights_emitsSuccessStateWithData`: Verifies state flow emission from loading to success with user profile week start preference.
    - `setFilter_updatesSelectedFilterInUiState`: Verifies filter tab switching (`WEEK`, `MONTH`, `ALL_TIME`).
    - `loadInsights_whenError_emitsErrorState`: Verifies error propagation to UI state.
  - `InsightsScreenComposeTest` (Robolectric):
    - `insightsContent_successState_rendersExecutionScoreAndMetrics`: Verifies rendering of the hero execution score card, app-defined metric label, metric cards, and filter interaction.
    - `insightsContent_errorState_rendersErrorMessage`: Verifies empty/error state rendering with informative feedback.

- **Phase 11: Calendar & Timeline Tests:**
  - `GetCalendarEventsUseCaseTest`:
    - `invoke_aggregatesTasksDeadlinesHabitsPlansAndDynamicReminders`: Validates unified chronological projection of tasks, deadlines, habits, plan items, and dynamic reminders.
    - `invoke_deduplicatesStoredAndDynamicReminderOccurrences`: Validates in-memory deduplication preventing duplicate event cards when an occurrence row is already in the database.
  - `CalendarViewModelTest`:
    - `loadEvents_updatesUiStateWithEvents`: Verifies initial event loading for selected date.
    - `setFilter_filtersEventsByType`: Verifies event type filtering (`TASK`, `DEADLINE`, `REMINDER`, `HABIT`, `PLAN_ITEM`).
    - `dateNavigation_advancesAndDecrementsDatesAccurately`: Verifies date navigation across Day, Week, and Month modes, plus "Today" jump.
    - `setViewMode_changesViewModeAndReloadsRange`: Verifies view mode switching and date range recalculation.
  - `CalendarScreenComposeTest` (Robolectric):
    - `calendarContent_dayMode_rendersTimelineAndHeader`: Verifies rendering of top bar controls, view mode chips, timeline cards, and deep-link click callbacks.
    - `calendarContent_emptyEvents_rendersEmptyTimelineState`: Verifies empty timeline state rendering when no commitments are scheduled.

- **Phase 12: Production Readiness & System Recovery Tests:**
  - `BootReceiverTest`:
    - `onReceive_bootCompleted_triggersRescheduleAll`: Verifies that `android.intent.action.BOOT_COMPLETED` invokes `rescheduleAllUseCase()`.
    - `onReceive_otherAction_doesNotTriggerReschedule`: Verifies that arbitrary intents do not trigger reconciliation.
  - `TimeChangeReceiverTest`:
    - `onReceive_timeChanged_triggersRescheduleAll`: Verifies that `ACTION_TIME_CHANGED` invokes `rescheduleAllUseCase()`.
    - `onReceive_timezoneChanged_triggersRescheduleAll`: Verifies that `ACTION_TIMEZONE_CHANGED` invokes `rescheduleAllUseCase()`.
    - `onReceive_dateChanged_triggersRescheduleAll`: Verifies that `ACTION_DATE_CHANGED` invokes `rescheduleAllUseCase()`.
    - `onReceive_unrelatedAction_doesNotTriggerReschedule`: Verifies unrelated system intents are ignored.
  - `ExactAlarmPermissionReceiverTest`:
    - `onReceive_exactAlarmPermissionChanged_triggersRescheduleAll`: Verifies that `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` refreshes capabilities and triggers `rescheduleAllUseCase()`.
  - `PackageReplacedReceiverTest`:
    - `onReceive_myPackageReplaced_triggersRescheduleAll`: Verifies that `ACTION_MY_PACKAGE_REPLACED` invokes `rescheduleAllUseCase()` upon in-place application upgrade.
  - `MigrationTest`:
    - `schemaV1_existsAndValidates`: Verifies version 1 JSON schema contains all 11 tables and indices.
    - `databaseV1_canBeCreatedViaHelper`: Verifies `MigrationTestHelper` creates v1 database cleanly.
    - `migration1To2_preservesExistingData`: Verifies non-destructive `MIGRATION_1_2` preserves pre-existing categories, tasks, and adds new column with NULL default.

## 4. Manual Device Testing Checklist (On-Device & Emulator)

| Verification Area | Verification Procedure | Expected Outcome |
| :--- | :--- | :--- |
| **1. Channel Registration** | Run app once, inspect `adb shell dumpsys notification \| grep -A 5 "channel_"` | 5 channels created: `Critical deadlines` (HIGH), `Routine reminders` (DEFAULT), `Daily briefing` (DEFAULT), `Evening review` (DEFAULT), `General` (LOW). |
| **2. Device Boot Recovery** | Schedule reminder, run `adb reboot`, inspect `adb logcat -s BootReceiver` | `BootReceiver` invokes `RescheduleAllUseCase`, recovers missing occurrences, and re-registers alarms in `AlarmManager`. |
| **3. Android Doze Mode** | Schedule reminder 3m out. Run: <br>`adb shell dumpsys battery unplug`<br>`adb shell dumpsys deviceidle force-idle` | Device enters deep idle; `RTC_WAKEUP` alarm fires on schedule, wakes device, and posts notification. |
| **4. Time & Timezone Shift** | Change timezone: <br>`adb shell service call alarm 3 s16 America/New_York` | `TimeChangeReceiver` triggers reconciliation; local wall-clock reminders remain fixed to local hours (e.g. 09:00 AM) while UTC instants recompute. |
| **5. Exact Alarm Permission Revocation** | Revoke permission via adb: <br>`adb shell appops set com.dayflow.app SCHEDULE_EXACT_ALARM deny` | `ExactAlarmPermissionReceiver` fires; app falls back to `setAndAllowWhileIdle`; `SchedulingCapability` emits `APPROXIMATE_FALLBACK`. |
| **6. Notification Actions** | Tap `Complete` on notification. | Notification dismissed, occurrence marked `COMPLETED` in SQLite, next slot scheduled. |
| **7. Snooze Non-Mutation** | Tap `Snooze` on notification. | Notification dismissed, transient alarm scheduled after snooze interval, reminder `anchorDate` remains unchanged. |
| **8. Dismissal Invariant** | Swipe notification away (dismiss). | Notification dismissed, occurrence marked handled, reminder is NEVER marked completed. |

## 5. Running Tests
```powershell
# Run unit tests
.\gradlew.bat testDebugUnitTest

# Run on-device instrumentation tests
.\gradlew.bat connectedDebugAndroidTest

# Run lint checks
.\gradlew.bat lint
```
