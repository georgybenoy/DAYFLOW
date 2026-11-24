# DAYFLOW — AGENTS.md

## 1. Project identity
You are the lead Android engineer responsible for building DAYFLOW.
DAYFLOW is an offline-first personal productivity and daily execution application.

Its purpose is to help users:
- Remember important commitments.
- Receive recurring and one-time reminders.
- Track habits and routines.
- Manage tasks and deadlines.
- Generate daily plans.
- Review unfinished work.
- Maintain consistent daily routines.

This is a native Android application.

## 2. Fixed technology stack
Do not change the selected technology stack without explicit approval.
- Kotlin
- Jetpack Compose
- Material 3
- MVVM architecture
- Clean Architecture principles
- Room / SQLite
- DataStore
- Hilt
- Kotlin Coroutines
- StateFlow
- AlarmManager
- WorkManager
- Android NotificationManager
- Kotlin Serialization
- JUnit
- MockK
- Compose UI Testing

Minimum Android version: API 26.
Use stable, compatible dependency versions.

## 3. Non-negotiable product constraints
1. The application must work completely offline.
2. Do not create a backend.
3. Do not implement AI features.
4. Do not require authentication.
5. Do not add unnecessary network dependencies.
6. Do not add INTERNET permission.
7. Do not use Firebase.
8. Do not use cloud databases.
9. Do not use web-based UI wrappers.
10. Do not use a continuously running service for reminders.
11. All important user data must be persisted locally.
12. Notifications must work when the application UI is closed, subject to Android system restrictions and permissions.
13. Scheduled reminders must recover after normal device reboot.
14. Database migrations must preserve existing user data.
15. Never silently discard unfinished tasks.

## 4. Architecture requirements
Implement clear separation between:
- Presentation
- Domain
- Data
- Background scheduling
- Notification delivery
- Backup and restoration

UI components must not directly access Room DAOs.
Use repositories and domain use cases.
Use ViewModels with StateFlow for observable UI state.
Use dependency injection rather than manually constructing application dependencies throughout the codebase.
Use immutable UI models.
Keep business logic independent of Android UI components wherever practical.

## 5. Reminder engine requirements
Implement the following reminder types:
- One-time reminders.
- Daily reminders.
- Weekly reminders.
- Custom weekday reminders.
- Fixed-clock interval reminders.
- Completion-based interval reminders.
- Custom start and end times.

Implement the following actions:
- Complete.
- Snooze.
- Skip.
- Pause.
- Resume.
- Edit.
- Delete.

Use one-shot AlarmManager scheduling.
Do not use repeating alarms as the primary recurrence engine.
Every reminder occurrence must have a unique identifier.
Implement idempotent notification delivery.
Persist occurrence state before or transactionally with scheduling changes.
Never assume exact-alarm permission is granted.
Implement an inexact fallback that is clearly disclosed to the user.

The recurrence calculator must handle:
- Midnight boundaries.
- Different month lengths.
- Leap years.
- Timezone changes.
- Daylight-saving transitions.
- Missed occurrences.
- Disabled weekdays.
- End dates.
- Snooze without recurrence-anchor modification.
Do not generate notification bursts for missed occurrences.

## 6. Notification requirements
Create separate notification channels for:
- Critical deadlines.
- Routine reminders.
- Daily briefing.
- Evening review.
- General notifications.

Implement notification action buttons.
Notifications must deep-link into the relevant application screen.
Notification dismissal must not automatically complete a task or habit.
Use unique PendingIntent identities.
Handle missing permissions gracefully.

## 7. Task management requirements
Implement:
- Task creation.
- Task editing.
- Task deletion.
- Categories.
- Four priority levels.
- Deadlines.
- Estimated duration.
- Subtasks.
- Completion.
- Cancellation.
- Overdue status.
- Rescheduling.

A task's deadline must remain independent of its planned execution time.
Preserve task identity when rescheduling.

## 8. Habit engine requirements
Implement:
- Binary tracking.
- Quantity tracking.
- Duration tracking.
- Repetition tracking.
- Daily logs.
- Weekly history.
- Monthly history.
- Streak calculations.
- Target completion.

Never assume a habit was completed simply because its notification was dismissed.

## 9. Daily planner requirements
Implement a deterministic planning engine.

The planner must consider:
- User-created tasks.
- Deadlines.
- Priority.
- Estimated duration.
- Availability.
- Fixed commitments.
- Active habits.
- Previous review decisions.
- Eligible unfinished tasks.

Generate a draft plan.
The user must approve or modify the draft.
Never automatically change task deadlines.
Detect schedule conflicts.
Never silently discard unfinished tasks.
Do not implement AI or external APIs.

## 10. Daily review requirements
Implement an evening review workflow.

The user must be able to:
- Review completed tasks.
- Review unfinished tasks.
- Complete pending tasks.
- Reschedule tasks.
- Exclude tasks from carry-forward.
- Add new commitments.
- Preview tomorrow's plan.

Save review information locally.

## 11. Backup requirements
Implement automatic local backups using WorkManager.
Backup frequency: 24 hours.
Retention: seven daily snapshots.

Use:
- Versioned JSON serialization.
- ZIP archive.
- SHA-256 integrity validation.
- Atomic file replacement.
- Temporary files for incomplete backups.
- Transaction-safe restoration.

Include all important user information.
Support manual export and import through Android's Storage Access Framework.
Never restore an invalid archive.
Never overwrite the existing database until backup validation has succeeded.
Do not claim local app-private backups protect against application uninstall or device loss.

## 12. UI requirements
Create a premium, minimalist dark-first interface.
Use lime-green accents.
Support light mode.

Primary navigation:
- Today.
- Reminders.
- Tasks.
- Habits.
- Insights.

Secondary screens:
- Calendar.
- Daily review.
- Reminder creation.
- Task creation.
- Habit creation.
- Settings.
- Backup management.

The Today screen must prioritize:
- Daily progress.
- Important tasks.
- Upcoming reminders.
- Today's habits.
- Upcoming deadlines.

Use reusable components.
Use accessible touch targets.
Provide loading, empty, success and error states.
Do not use placeholder buttons that do nothing.
Do not use fake data in production screens.

## 13. Code quality rules
- Use meaningful names.
- Keep functions focused.
- Avoid giant ViewModels.
- Avoid giant Composables.
- Avoid duplicate business logic.
- Avoid unnecessary abstractions.
- Use Kotlin null safety.
- Use sealed classes for meaningful UI states and result types.
- Use explicit error handling.
- Use transactions for related database operations.
- Use structured concurrency.
- Do not swallow exceptions.
- Do not hardcode application logic into UI components.
- Do not use destructive Room migrations.
- Document complex scheduling algorithms.
- Keep secrets out of source control.

## 14. Development workflow
Work in small, independently verifiable phases.

For each phase:
1. Inspect the existing project.
2. Understand existing implementation.
3. Identify dependencies.
4. Implement the required functionality.
5. Write relevant tests.
6. Build the application.
7. Fix compilation errors.
8. Fix test failures.
9. Verify integration with existing modules.
10. Update documentation.
11. Report completed work and outstanding issues.

Do not claim functionality is complete unless it is implemented and tested.
Do not replace working features merely to simplify implementation.
Do not remove existing features without approval.
Do not introduce unnecessary dependencies.
Do not leave core features as TODOs or mock implementations.

## 15. Required project documentation
Maintain these documents:
- PRD.md
- DESIGN_SYSTEM.md
- ARCHITECTURE.md
- DATABASE.md
- SECURITY.md
- CODE_STYLE.md
- TESTING.md
- AGENTS.md
- IMPLEMENTATION_ROADMAP.md
- RELEASE_CHECKLIST.md
- README.md

Update documentation whenever architecture or product behaviour changes.

## 16. Definition of done
A feature is complete only when:
- Its implementation exists.
- Its UI works.
- Its data persists.
- Its business logic is tested.
- Error cases are handled.
- It works offline.
- It integrates with other relevant modules.
- It does not break existing functionality.
- Relevant documentation is updated.

The application must build successfully before a development phase is declared complete.

## 17. Primary engineering objective
Build a reliable, lightweight, maintainable Android application.

Prioritize:
1. Notification reliability.
2. Data integrity.
3. Offline functionality.
4. Correct recurrence behaviour.
5. Daily planning correctness.
6. Backup and recovery.
7. User experience.
8. Visual polish.

Do not prioritize visual effects over functional reliability.
The final product must be a genuinely usable personal productivity application, not a UI prototype.
