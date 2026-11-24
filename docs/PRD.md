# DAYFLOW — PRODUCT REQUIREMENTS DOCUMENT (PRD)

Product Name: DAYFLOW  
Tagline: Make today count.  
Product Category: Personal Productivity / Daily Execution  
Platform: Android  
Version: 1.0  
Document Status: Finalized Product Specification  
Architecture: Offline-first native Android  
AI Dependency: None  

---

## 1. Executive Summary

DAYFLOW is an offline-first Android personal productivity application designed to help users manage reminders, habits, tasks, deadlines and daily routines through one unified system.

The application combines:
1. Smart reminder management.
2. Recurring routine scheduling.
3. Task and deadline management.
4. Habit tracking.
5. Automatic daily planning.
6. Morning briefings.
7. Evening reviews.
8. Productivity analytics.
9. Automatic local backup and recovery.

DAYFLOW is designed to reduce the mental effort required to remember, organize and execute daily responsibilities.

Unlike traditional reminder applications, DAYFLOW does not simply notify users about isolated events.
It maintains continuity between days by remembering unfinished tasks, reviewing progress and preparing the next day's plan.

The application uses deterministic algorithms rather than AI.
All core functionality works without internet access, user accounts, external APIs or cloud infrastructure.

---

## 2. Product Vision

Build a reliable personal execution system that helps individuals become more organized and consistent.
The user should not need to remember every commitment manually.

DAYFLOW should:
- Remember what needs to be done.
- Remind the user at appropriate times.
- Organize daily responsibilities.
- Track recurring habits.
- Identify unfinished work.
- Prepare the next day's agenda.
- Maintain historical progress.

### 2.1 Product philosophy
"Remember less. Execute better."  
DAYFLOW should encourage consistency without overwhelming users with excessive notifications or productivity pressure.

---

## 3. Problem Statement

People often use multiple disconnected applications to manage daily responsibilities.
Examples include:
- Alarms for drinking water and exercising.
- Notes for assignments.
- Calendar entries for deadlines.
- To-do applications for daily tasks.
- Habit trackers for routines.

This fragmentation causes several problems:
1. Important activities are forgotten.
2. Recurring reminders are difficult to manage.
3. Tasks and deadlines are disconnected.
4. Unfinished tasks disappear between days.
5. Daily planning becomes repetitive.
6. Users lack a clear picture of their progress.
7. Excessive notifications become distracting.

DAYFLOW consolidates these workflows into one local-first application.

---

## 4. Product Goals

### 4.1 Primary goals

| Goal | Description |
|---|---|
| Reliability | Deliver scheduled reminders as reliably as Android permits |
| Organization | Centralize tasks and deadlines |
| Consistency | Track habits and routines |
| Planning | Generate daily plans automatically |
| Continuity | Preserve unfinished work between days |
| Privacy | Keep personal data locally |
| Accessibility | Make daily management simple |

### 4.2 Secondary goals
- Provide meaningful productivity statistics.
- Reduce repetitive manual planning.
- Support custom routines.
- Allow data backup and restoration.
- Offer a premium and visually consistent interface.

---

## 5. Target Users

### 5.1 Primary audience
Students and individuals managing multiple responsibilities.  
Examples:
- University students.
- Working professionals.
- Individuals building exercise routines.
- People managing personal projects.
- Individuals trying to improve consistency.

### 5.2 User characteristics
Users may:
- Have multiple daily reminders.
- Maintain recurring habits.
- Manage assignments and deadlines.
- Forget unfinished tasks.
- Prefer simple interfaces.
- Want offline functionality.
- Avoid mandatory accounts and subscriptions.

---

## 6. Product Scope

### 6.1 Included in version 1.0
- Onboarding.
- Dashboard.
- Reminder management.
- Fixed-time recurrence.
- Completion-based recurrence.
- One-time reminders.
- Task management.
- Deadline management.
- Habit tracking.
- Daily planner.
- Morning briefing.
- Evening review.
- Calendar.
- Productivity insights.
- Notification management.
- Automatic local backups.
- Manual data export/import.
- Theme customization.
- Device restart recovery.

### 6.2 Excluded from version 1.0
- AI assistant.
- Cloud synchronization.
- User accounts.
- Social features.
- Location-based reminders.
- External calendar integrations.
- Wearable integrations.
- Health monitoring.
- Online subscriptions.
- Multi-device synchronization.

These features must not be introduced without explicit product approval.

---

## 7. Functional Requirements

### FR-01: Onboarding
The application must provide an onboarding experience.

The user should be able to:
- Understand the application.
- Configure a preferred name or greeting.
- Select a theme.
- Set morning briefing time.
- Set evening review time.
- Configure quiet hours.
- Understand notification requirements.
- Review exact alarm requirements.
- Complete initial setup.

Permissions should be requested contextually rather than requesting everything immediately.

**Acceptance criteria:**
- The application launches without an account.
- Onboarding preferences are persisted locally.
- Permission denial does not prevent access to other features.
- The user can revisit settings later.

### FR-02: Dashboard
The dashboard is the main application screen. It must provide a consolidated view of the user's day.

**Required components:**
- **Greeting section:** Personalized greeting, Current date, Selected day, Upcoming notification indicator.
- **Daily progress:** Tasks completed, Total tasks, Completion percentage, Circular progress indicator.
- **Habit summary:** Number of habits completed, Total scheduled habits, Habit completion percentage.
- **Upcoming reminders:** Next reminders, Scheduled time, Reminder title, Completion action.
- **Priority tasks:** Urgent tasks, High-priority tasks, Upcoming deadlines.
- **Productivity overview:** Current streak, Weekly completion, Upcoming commitments.

The dashboard must be responsive and scrollable.

**Acceptance criteria:**
- Every metric is derived from persisted data.
- Empty states are displayed when no records exist.
- Selecting a date updates the relevant dashboard information.
- Dashboard updates when underlying data changes.

### FR-03: Reminder Management
Users must be able to create and manage reminders.

**Supported reminder types:**
1. One-time.
2. Daily.
3. Weekly.
4. Custom weekday.
5. Fixed-time interval.
6. Completion-based interval.

**Reminder fields:**
- Title.
- Description.
- Category.
- Reminder type.
- Schedule mode.
- Start date.
- Start time.
- Interval.
- Active days.
- End time.
- End date.
- Priority.
- Delivery precision.
- Enabled status.

**Reminder actions:**
- Create.
- Edit.
- Delete.
- Complete.
- Snooze.
- Skip.
- Pause.
- Resume.

**Fixed-time example:**  
Reminder: Drink water.  
Interval: Every 2 hours.  
Start: 8:00 AM.  
End: 10:00 PM.  
Expected schedule: 8 AM, 10 AM, 12 PM, 2 PM, 4 PM, 6 PM, 8 PM and 10 PM.  

**Completion-based example:**  
Reminder: 5 push-ups.  
Interval: 1 hour.  
Initial reminder: 10 AM.  
Completed: 10:15 AM.  
Next reminder: 11:15 AM.  

The recurrence engine must preserve the distinction between fixed-time and completion-based schedules.

**Acceptance criteria:**
- Reminder data persists.
- Future occurrence calculations are correct.
- Edits cancel stale alarms.
- Deletions cancel related alarms.
- Duplicate notifications are prevented.
- Snooze does not modify the original recurrence anchor.
- Completion-based reminders advance only according to the defined interaction policy.
- Permission limitations are communicated clearly.

### FR-04: Notification System
The application must deliver native Android notifications.

**Required notification types:**
- General reminders.
- Critical deadlines.
- Habit reminders.
- Morning briefing.
- Evening review.

**Required notification actions:**
- Complete.
- Snooze.
- Open application.

**The system must support:**
- Notification channels.
- Custom sounds where supported.
- Vibration settings.
- Notification grouping.
- Quiet hours.
- Permission checks.
- Exact and approximate delivery modes.

Notifications should remain actionable when the application UI is closed.  
Android may restrict delivery because of force-stop, battery policies, permission denial or manufacturer-specific background restrictions. These limitations must be explained rather than hidden.

### FR-05: Task Management
Users must be able to manage one-time responsibilities.

**Task fields:**
- Title.
- Description.
- Category.
- Priority.
- Deadline.
- Estimated duration.
- Planned start.
- Planned end.
- Status.
- Carry-forward preference.
- Creation date.
- Completion date.

**Task categories:**
- Academics.
- Personal.
- Fitness.
- Work.
- Projects.
- Finance.
- Other.

**Priority levels:**
- Urgent.
- High.
- Medium.
- Low.

**Task statuses:**
- Pending.
- Completed.
- Cancelled.

Overdue status should be derived from the deadline and task state.

**Subtasks:**
Support parent tasks containing multiple smaller tasks.  
Example: Task: Complete final-year project documentation.  
Subtasks:
- Complete architecture document.
- Complete database documentation.
- Update README.
- Review final report.

**Acceptance criteria:**
- Tasks are saved locally.
- Users can edit and delete tasks.
- Completed tasks retain historical records.
- Overdue tasks are identifiable.
- Rescheduling does not create duplicate task identities.
- Subtask completion is tracked separately.

### FR-06: Habit Management
Users must be able to create recurring habits.

**Habit fields:**
- Name.
- Description.
- Tracking type.
- Target value.
- Unit.
- Active weekdays.
- Reminder preference.
- Enabled status.

**Supported tracking types:**
- **Binary:** Example: Read today. Values: Completed / Not completed.
- **Quantity:** Example: Drink 8 glasses of water. Values: 0–8 glasses or other user-defined quantity.
- **Duration:** Example: Exercise for 30 minutes. Values: Actual duration.
- **Repetition:** Example: Complete 5 push-ups. Values: Number of repetitions.

**Habit features:**
- Daily progress.
- Weekly calendar.
- Monthly history.
- Streak calculation.
- Target completion.
- Partial completion.
- Missed habit tracking.

**Acceptance criteria:**
- Habit records persist.
- Logs are associated with local dates.
- Daily targets are respected.
- Disabled habits are excluded from active schedules.
- Streaks use scheduled days rather than counting inactive days as missed.
- Notification dismissal does not count as completion.

### FR-07: Daily Planner
The daily planner is one of the central differentiators of DAYFLOW.  
It generates a proposed daily plan using deterministic algorithms.

**Planner inputs:**
- Pending tasks.
- Task deadlines.
- Priority levels.
- Estimated durations.
- Fixed commitments.
- User availability.
- Active habits.
- Previous review decisions.

**Planner outputs:**
- Daily task list.
- Suggested task ordering.
- Suggested time slots.
- Upcoming deadlines.
- Overdue task warnings.
- Schedule conflict warnings.
- Rescheduling suggestions.

**Planning behaviour:**
The planner must:
1. Collect eligible records.
2. Identify overdue tasks.
3. Identify today's deadlines.
4. Collect scheduled habits.
5. Read previous review decisions.
6. Calculate priority.
7. Sort tasks.
8. Suggest time slots.
9. Identify conflicts.
10. Generate a draft plan.

**Semi-automatic approval:**
The user must be able to:
- Approve the generated plan.
- Edit task order.
- Change suggested times.
- Remove proposed items.
- Add additional tasks.
- Reschedule tasks.
- Save the plan.

The application must not silently alter task deadlines.

**Acceptance criteria:**
- The same input and settings produce deterministic results.
- Conflicting time slots are identified.
- Unscheduled tasks remain visible.
- Users can modify draft plans.
- Only approved plans become active daily plans.

### FR-08: Morning Briefing
The morning briefing presents the day's plan.  
Default time: 7:00 AM. (The user can customize the time).

**Briefing content:**
- Greeting.
- Today's date.
- Today's priorities.
- Scheduled tasks.
- Upcoming deadlines.
- Habit checklist.
- Pending tasks.
- Important reminders.

**The briefing must be available as:**
- An application screen.
- A native notification.
- A dashboard summary.

The briefing must not fabricate tasks or activities.

**Acceptance criteria:**
- The briefing is generated locally.
- Its contents reflect the current database.
- It does not duplicate the daily plan.
- It handles empty schedules gracefully.
- Users can open the full daily plan.

### FR-09: Evening Review
Default time: 9:30 PM.  
The user should be able to review the day's activities.

**Review information:**
- Completed tasks.
- Unfinished tasks.
- Habit progress.
- Missed reminders.
- Overdue deadlines.
- New commitments.

**Available actions for each unfinished task:**
- Complete.
- Reschedule.
- Carry forward.
- Exclude from carry-forward.
- Cancel.

The application should generate tomorrow's draft using these decisions.

**Acceptance criteria:**
- Reviews are stored by local date.
- Only one review record exists per date.
- Decisions are persisted.
- The next day's planner uses review decisions.
- Users can reopen previous reviews.

### FR-10: Calendar
Provide:
- Daily view.
- Weekly view.
- Monthly view.

Display:
- Tasks.
- Deadlines.
- Reminders.
- Habit occurrences.
- Approved plan items.

The calendar is local and independent of external calendar providers.

### FR-11: Productivity Insights
Display meaningful historical statistics.

**Required metrics:**
- Daily task completion.
- Weekly task completion.
- Monthly task completion.
- Habit consistency.
- Current streak.
- Longest streak.
- Completed reminders.
- Missed occurrences.
- Focus-session duration, if enabled.

**Visualizations:**
- Circular progress rings.
- Bar charts.
- Line charts.
- Calendar heatmaps.
- Activity timelines.

All metrics must be calculated from stored records.  
The application-defined execution score must be clearly labelled and must not be presented as an objective measure of personal worth or health.

### FR-12: Settings
Users can configure:
- Theme.
- Morning briefing time.
- Evening review time.
- Quiet hours.
- Default snooze.
- Week start day.
- Date format.
- Time format.
- Notification sound.
- Vibration.
- Reminder precision.
- Backup frequency.
- Backup retention.
- Data export.
- Data import.

### FR-13: Backup and Recovery
Automatic local backup is mandatory.

**Backup defaults:**
- Frequency: Every 24 hours.
- Retention: Seven snapshots.
- Storage: Application-private files.
- Format: Versioned ZIP archive.
- Serialization: JSON.
- Integrity: SHA-256.

**Backup contents:**
- Reminders.
- Reminder occurrence history.
- Tasks.
- Subtasks.
- Categories.
- Habits.
- Habit logs.
- Daily plans.
- Daily plan items.
- Daily reviews.
- Application settings.

**Restoration:**
The application must:
1. Read the archive.
2. Validate integrity.
3. Check schema compatibility.
4. Validate records.
5. Check references.
6. Create a safety snapshot.
7. Restore transactionally.
8. Reconcile scheduled alarms.
9. Report restoration results.

**Acceptance criteria:**
- Corrupt backups are rejected.
- Incompatible backups are rejected safely.
- Partial restoration does not corrupt live data.
- The latest valid backup remains available.
- Retention policy is enforced.
- Manual export works independently of automatic backup.

---

## 8. Non-functional Requirements

### NFR-01: Performance
- Dashboard should load quickly.
- Database queries should not block the UI thread.
- Lists should use lazy Compose components.
- Charts should render efficiently.
- Background work should remain lightweight.

### NFR-02: Reliability
- Persist reminders before scheduling.
- Recover alarms after reboot.
- Reconcile schedules after permission changes.
- Prevent duplicate occurrence delivery.
- Preserve data during edits and migrations.
- Handle scheduling failures explicitly.

### NFR-03: Offline functionality
- All core features must work without an internet connection.
- The application must not depend on remote APIs.

### NFR-04: Privacy
- No mandatory login.
- No external analytics transmission.
- No unnecessary permissions.
- No cloud storage.
- Private local database.
- User-controlled export and restoration.

### NFR-05: Maintainability
- Modular feature structure.
- Repository pattern.
- Testable domain logic.
- Explicit database migrations.
- Centralized design tokens.
- Documented scheduling logic.

### NFR-06: Compatibility
- Minimum API: 26.
- Support modern Android versions through compile-time and target SDK configurations appropriate to the release.
- Test on physical Android devices.

### NFR-07: Accessibility
- Screen-reader compatibility.
- Dynamic text sizing.
- Appropriate contrast.
- Accessible touch targets.
- Reduced-motion support.

---

## 9. User Workflows

### Workflow A: Create a recurring water reminder
1. Open Reminders.
2. Tap Add Reminder.
3. Enter "Drink water".
4. Select Fixed Interval.
5. Set interval to 2 hours.
6. Set start to 8 AM.
7. Set end to 10 PM.
8. Save.
9. Review the upcoming schedule.
10. Confirm notification permissions.  
*Expected result:* DAYFLOW schedules the required occurrences.

### Workflow B: Complete a habit
1. Open Habits.
2. Select a habit.
3. Record the completed amount.
4. Save.
5. Update daily progress.
6. Update historical logs.
7. Recalculate streaks.  
*Expected result:* The habit's progress is accurately reflected.

### Workflow C: Add an assignment
1. Open Tasks.
2. Add assignment.
3. Enter title.
4. Set category.
5. Set priority.
6. Set deadline.
7. Set estimated duration.
8. Save.  
*Expected result:* The assignment appears in the appropriate upcoming task lists and daily planning suggestions.

### Workflow D: Prepare tomorrow
1. Receive evening review notification.
2. Open review.
3. Review today's progress.
4. Decide what happens to unfinished tasks.
5. Add new commitments.
6. Generate tomorrow's draft.
7. Review and approve.  
*Expected result:* Tomorrow's plan is ready.

---

## 10. User Experience Requirements

The application must follow the approved DAYFLOW Design System.

**Visual identity:**
- Dark forest-green background.
- Mint-green primary accent.
- Soft yellow secondary accent.
- Lavender supporting accent.
- Rounded dashboard cards.
- Circular progress rings.
- Compact charts.
- Minimal typography.
- Subtle transitions.

**Primary navigation:**
1. Today.
2. Reminders.
3. Tasks.
4. Habits.
5. Insights.

**Secondary screens:**
- Calendar.
- Evening Review.
- Settings.
- Backup Management.

The supplied visual reference is the design authority for the overall visual direction.

---

## 11. Technical Requirements

### 11.1 Technology stack

| Layer | Technology |
|---|---|
| Programming | Kotlin |
| UI | Jetpack Compose |
| Design | Material 3 |
| Architecture | MVVM + Clean Architecture principles |
| Database | Room / SQLite |
| Preferences | DataStore |
| Dependency injection | Hilt |
| Background work | WorkManager |
| Exact scheduling | AlarmManager |
| Notifications | NotificationManager |
| Async processing | Coroutines / Flow |
| Serialization | Kotlin Serialization |
| Testing | JUnit / MockK / Compose UI Testing |
| Build | Gradle Kotlin DSL |

### 11.2 Architecture
- **Presentation Layer:** Screens, Components, ViewModels, UI state.
- **Domain Layer:** Models, Use cases, Repositories, Planner engine, Recurrence calculator.
- **Data Layer:** Room entities, DAOs, Repositories, Mappers, DataStore.
- **System Integration:** AlarmManager, NotificationManager, BroadcastReceivers, WorkManager, Backup engine.

### 11.3 Database
Database name: `dayflow.db`.  
Initial schema version: 1.  
Required entities:
- Category
- Reminder
- ReminderOccurrence
- Task
- TaskSubtask
- Habit
- HabitLog
- DailyPlan
- DailyPlanItem
- DailyReview
- AppSetting

The full database specification is maintained in `DATABASE.md`.

### 11.4 Scheduling
- Use one-shot AlarmManager alarms.
- Use WorkManager for: Backup, Deferred calculations, Maintenance, Non-time-critical summaries.
- Implement: Boot recovery, Time-change recovery, Timezone recovery, Exact-alarm permission recovery, Duplicate prevention, Schedule reconciliation.

---

## 12. Security Requirements
- No backend credentials.
- No external API keys.
- No network dependencies.
- Private application storage.
- Archive validation.
- SHA-256 backup verification.
- Safe restore operations.
- Explicit permission handling.
- No sensitive information in notification logs.

---

## 13. Development Phases

### Phase 1: Foundation
- Android Studio project.
- Kotlin and Compose.
- Hilt.
- Room.
- DataStore.
- Navigation.
- Design system.
- Onboarding.  
*Deliverable:* Working application shell.

### Phase 2: Reminder Engine
- Reminder CRUD.
- Recurrence.
- AlarmManager.
- Notifications.
- Snooze.
- Complete.
- Skip.
- Recovery.  
*Deliverable:* Working reminder application.

### Phase 3: Task Management
- Task CRUD.
- Categories.
- Priorities.
- Deadlines.
- Subtasks.
- Overdue management.  
*Deliverable:* Working task manager.

### Phase 4: Habit Tracking
- Habit CRUD.
- Four tracking types.
- Daily logs.
- Streaks.
- History.  
*Deliverable:* Working habit tracker.

### Phase 5: Daily Planning
- Planner engine.
- Priority scoring.
- Schedule suggestions.
- Conflict detection.
- User approval.
- Morning briefing.
- Evening review.  
*Deliverable:* Daily execution workflow.

### Phase 6: Backup and Analytics
- Automatic backup.
- Manual export/import.
- Restore validation.
- Statistics.
- Charts.
- Calendar.  
*Deliverable:* Complete productivity system.

### Phase 7: Production Readiness
- Physical device testing.
- Reboot recovery.
- Permission testing.
- Database migrations.
- Backup recovery.
- UI polish.
- Accessibility.
- APK packaging.  
*Deliverable:* DAYFLOW 1.0.

---

## 14. Acceptance Criteria

The product is ready for its first release when:

**Reminders:**
- One-time reminders work.
- Fixed-time recurring reminders work.
- Completion-based reminders work.
- Snooze and skip work.
- Duplicate delivery is prevented.
- Alarm recovery works.

**Tasks:**
- CRUD works.
- Deadlines work.
- Priority works.
- Subtasks work.
- Rescheduling works.

**Habits:**
- Four tracking modes work.
- Historical logs work.
- Streaks are accurate.

**Planner:**
- Drafts are generated.
- Priority ordering works.
- Conflicts are detected.
- User approval works.
- Unfinished tasks are handled correctly.

**Daily workflow:**
- Morning briefing works.
- Evening review works.
- Tomorrow's plan uses review decisions.

**Backup:**
- Automatic backups work.
- Retention works.
- Manual export works.
- Restoration works.
- Corrupt backups are rejected.

**Product:**
- Works offline.
- Requires no account.
- Has no AI dependency.
- Follows the approved design system.
- Builds successfully.
- Passes required automated and device tests.

---

## 15. Release Strategy

### Version 1.0 — Core DAYFLOW
- Reminders.
- Tasks.
- Habits.
- Dashboard.
- Planner.
- Morning briefing.
- Evening review.
- Notifications.
- Backup.

### Version 1.1 — Productivity Insights
- Calendar improvements.
- Advanced analytics.
- Weekly reports.
- Advanced routine management.

### Version 1.2 — Convenience
- Home-screen widgets.
- Focus sessions.
- Improved schedule customization.
- Additional dashboard customization.

These future versions remain offline-first unless the product direction is explicitly changed.

---

## 16. Success Metrics

Measure product quality using local, privacy-respecting metrics:
- Percentage of reminders successfully processed.
- Duplicate notification rate.
- Alarm recovery success.
- Task completion rate.
- Habit consistency.
- Daily review completion.
- Backup success rate.
- Restore success rate.
- Application crash rate.

Do not transmit these metrics externally.

---

## 17. Risks and Mitigation

| Risk | Mitigation |
|---|---|
| Android notification restrictions | Permission checks and reliability screen |
| Exact alarm access unavailable | Explicit approximate fallback |
| Battery optimization | Explain manufacturer-specific restrictions |
| Device reboot | Boot receiver and schedule reconciliation |
| Database corruption | Automatic backups and validation |
| Incorrect recurrence | Comprehensive recurrence tests |
| Duplicate notifications | Unique occurrence IDs and idempotency |
| Unfinished task accumulation | Review and carry-forward controls |
| Unrealistic schedules | Conflict detection and user approval |
| UI complexity | Progressive disclosure and compact dashboard |

---

## 18. Final Product Definition

DAYFLOW is a local-first personal daily execution system.

It combines:
- A reminder engine.
- A task manager.
- A habit tracker.
- A daily planning engine.
- A review system.
- A productivity dashboard.
- A local backup system.

The user supplies the commitments.  
DAYFLOW organizes them, reminds the user, tracks progress and helps prepare the next day.  
The application must remain reliable, lightweight, private and fully usable without internet connectivity.

DAYFLOW — Make today count.  
END OF PRODUCT REQUIREMENTS DOCUMENT.
