# DAYFLOW — DESIGN_SYSTEM.md

Product: DAYFLOW  
Tagline: Make today count.  
Platform: Android  
Design framework: Jetpack Compose + Material 3  
Design direction: Premium dark wellness-inspired productivity dashboard  
Status: Finalized visual direction  

---

## 1. Design Philosophy

DAYFLOW should feel like a premium personal wellness and productivity application.
The visual language is inspired by the supplied three-screen reference image.

The interface must combine:
- Minimalism.
- Information density.
- Rounded dashboard cards.
- Elegant typography.
- Circular progress visualizations.
- Soft pastel accents.
- Deep dark surfaces.
- Clear visual hierarchy.
- Subtle transitions.

The application must feel calm, focused and inviting rather than corporate or excessively gamified.

### 1.1 Design principles
1. Information should be easy to scan.
2. Important information should stand out without aggressive colours.
3. Use cards to group related information.
4. Use circular progress indicators for meaningful aggregate metrics.
5. Use charts for historical trends, not decorative effects.
6. Avoid unnecessary gradients and excessive shadows.
7. Keep interactions predictable.
8. Use animations sparingly.
9. Maintain consistency across all screens.
10. Ensure readability on different Android screen sizes.

---

## 2. Visual Reference Interpretation

The supplied reference contains three major design patterns.

### Pattern A — Dashboard
- Greeting header.
- Compact horizontal date selector.
- Mood-like daily indicators, adapted to task/habit status.
- Multiple information cards.
- Circular progress visualization.
- Compact bar charts.
- Bottom navigation.

DAYFLOW application: Today dashboard.

### Pattern B — Detailed analytics
- Large concentric circular progress rings.
- Multiple related metrics.
- Secondary tracking cards.
- Compact historical charts.

DAYFLOW application: Habit and productivity analytics.

### Pattern C — Detailed activity overview
- Large central circular score.
- Time-based summary metrics.
- Segmented information cards.
- Historical timeline visualization.

DAYFLOW application: Daily activity breakdown and task execution history.

Do not copy the original health-specific labels or measurements.

---

## 3. Colour System

### 3.1 Core palette

| Token | Hex | Usage |
|---|---|---|
| Background | #07120F | Main application background |
| Surface | #101D18 | Main cards |
| Surface Elevated | #172720 | Secondary cards |
| Surface Variant | #20342B | Inputs and containers |
| Border | #263A32 | Subtle outlines |
| Primary Mint | #A5E6D6 | Primary highlights |
| Secondary Mint | #76CDBB | Secondary highlights |
| Soft Yellow | #F0E99B | Habit indicators |
| Soft Lavender | #DCC3F5 | Secondary data series |
| Text Primary | #F1F5EF | Primary text |
| Text Secondary | #A5B6AD | Secondary text |
| Text Muted | #71847A | Supporting text |
| Error | #F28D91 | Errors |
| Warning | #F1C879 | Warnings |
| Success | #A5E6D6 | Completed states |

### 3.2 Colour usage

**Mint:**
- Primary buttons.
- Main progress indicators.
- Active navigation.
- Completed states.

**Yellow:**
- Habit completion.
- Secondary progress metrics.
- Important but non-critical information.

**Lavender:**
- Secondary charts.
- Focus sessions.
- Supporting productivity metrics.

**Red:**
- Overdue tasks.
- Failed operations.
- Critical warnings.

Never use all accent colours equally. Mint should remain the primary accent.

### 3.3 Light theme
Provide a corresponding light theme.

| Token | Hex |
|---|---|
| Background | #F3F7F3 |
| Surface | #FFFFFF |
| Surface Elevated | #EAF1EC |
| Border | #DCE6DF |
| Primary | #286B5B |
| Secondary | #7D987F |
| Accent Yellow | #E7D978 |
| Accent Lavender | #B99DD8 |
| Text Primary | #17241D |
| Text Secondary | #66756B |

The dark theme is the default.

---

## 4. Typography

Use Inter as the preferred typeface, with a bundled font where licensing permits.  
Fallback: Android Sans / system sans-serif.

### Typography scale:

| Style | Size | Weight |
|---|---|---|
| Display | 32sp | Bold |
| Headline Large | 26sp | SemiBold |
| Headline Medium | 22sp | SemiBold |
| Title Large | 18sp | SemiBold |
| Title Medium | 16sp | Medium |
| Body Large | 15sp | Regular |
| Body Medium | 14sp | Regular |
| Body Small | 12sp | Regular |
| Label | 11sp | Medium |

Use tabular numerals for time, counts and metrics.  
Avoid excessively small text.

---

## 5. Layout and Spacing

Base spacing unit: 4dp.

| Token | Value |
|---|---|
| XS | 4dp |
| SM | 8dp |
| MD | 12dp |
| LG | 16dp |
| XL | 20dp |
| XXL | 24dp |
| Section | 28dp |
| Large Section | 32dp |

- Screen horizontal padding: 16dp.
- Card internal padding: 14–16dp.
- Card spacing: 12dp.
- Section spacing: 20–24dp.

---

## 6. Shapes and Components

### 6.1 Card shapes

| Component | Corner radius |
|---|---|
| Main dashboard card | 18dp |
| Secondary card | 14dp |
| Input | 12dp |
| Button | 14dp |
| Chip | 50% rounded |
| Bottom sheet | 24dp top corners |

Cards should have subtle tonal separation rather than heavy shadows.

### 6.2 Buttons

**Primary button:**
- Mint background.
- Dark foreground.
- Medium-to-bold label.
- Minimum height 48dp.

**Secondary button:**
- Surface Variant background.
- Primary text.

**Outlined button:**
- Transparent surface.
- Subtle border.

**Destructive button:**
- Muted red treatment.

### 6.3 Progress rings
Use custom Canvas-based Compose components.

Support:
- Single progress ring.
- Multiple concentric rings.
- Center labels.
- Percentage values.
- Animated progress transitions.

Use these for:
- Daily task completion.
- Weekly habit completion.
- Focus-session progress.
- Productivity overview.

Never imply that a productivity score is a medical or scientifically validated health score.

### 6.4 Charts
Use native Compose Canvas or a lightweight chart library.

Required chart types:
- Bar charts.
- Line charts.
- Circular progress charts.
- Calendar heatmaps.
- Timeline charts.

Charts must:
- Have labels.
- Display real database-derived values.
- Support empty states.
- Be accessible.
- Avoid unnecessary decoration.

---

## 7. Navigation

Primary bottom navigation:
1. Today
2. Reminders
3. Tasks
4. Habits
5. Insights

Secondary destinations:
- Calendar.
- Daily review.
- Settings.
- Backup management.
- Reminder details.
- Task details.
- Habit details.

Use simple outlined/filled icon transitions.  
Keep the active destination visually highlighted using mint.

---

## 8. Screen Specifications

### 8.1 Today Dashboard
This is the primary screen.

**Header:**
- Personalized greeting.
- Current date.
- Profile/settings access.
- Notification or upcoming-deadline indicator.

**Date selector:**
- Horizontal seven-day strip.
- Current date highlighted.
- Selected date changes dashboard context.

**Dashboard cards:**
- **A. Daily Execution:** Tasks completed, Total tasks, Completion percentage, Circular progress indicator.
- **B. Habit Overview:** Today's completed habits, Total habits, Progress visualization.
- **C. Upcoming Reminders:** Next scheduled reminders, Time, Completion action.
- **D. Priority Tasks:** Urgent and high-priority tasks, Deadline indicators.
- **E. Productivity Summary:** Weekly completion, Current streak, Upcoming deadlines.
- **F. Today's Schedule:** Chronological timeline.

The dashboard should be scrollable.  
Use responsive grid layouts where space permits.

### 8.2 Reminder Screen
Display:
- Active reminders.
- Paused reminders.
- Upcoming occurrences.
- Recurrence type.
- Next delivery time.

Include an accessible add-reminder button.  
Reminder creation must support:
- One-time.
- Fixed-time recurring.
- Completion-based recurring.
- Daily and weekly.
- Custom weekdays.
- Start/end time.
- Snooze.
- Priority.
- Exact/approximate scheduling preference.

### 8.3 Task Screen
Provide:
- Today.
- Upcoming.
- Overdue.
- Completed.
- All tasks.

Use priority indicators and compact cards.  
Task cards display:
- Title.
- Category.
- Deadline.
- Priority.
- Completion state.

### 8.4 Habit Screen
Provide:
- Daily habit list.
- Completion progress.
- Individual habit cards.
- Current streak.
- Weekly calendar.

Support binary, quantity, duration and repetition tracking.  
Use pastel accent colours to distinguish habit categories.

### 8.5 Insights Screen
Use the analytics style of the supplied reference.  
Display:
- Daily execution score, explicitly labelled as an app-defined metric.
- Weekly completion.
- Habit consistency.
- Streak history.
- Task completion trends.
- Focus duration, when recorded.
- Missed and completed reminders.

Use concentric rings and compact charts.  
Never generate statistics from placeholder values.

### 8.6 Calendar Screen
Provide day, week and month views.  
Show:
- Tasks.
- Deadlines.
- Reminders.
- Habits.
- Approved daily plan entries.

Differentiate event types using consistent accent colours.

### 8.7 Evening Review
Display:
- Today's completed tasks.
- Unfinished tasks.
- Habit progress.
- Pending deadlines.
- Rescheduling decisions.
- Tomorrow preview.

Use clear actions for each unfinished task.

### 8.8 Settings
Include:
- Theme.
- Morning briefing time.
- Evening review time.
- Quiet hours.
- Notification channels.
- Default snooze.
- Exact alarm permission status.
- Backup settings.
- Export and import.
- Data retention.
- About DAYFLOW.

---

## 9. Interactions and Motion

Animation duration:
- Small interactions: 120–180ms.
- Card transitions: 180–240ms.
- Progress transitions: 300–450ms.

Use:
- AnimatedContent.
- AnimatedVisibility.
- animateFloatAsState.
- LazyColumn.
- LazyVerticalGrid.

Avoid:
- Continuous looping animations.
- Unnecessary parallax.
- Excessive bounce effects.
- Animations that delay critical actions.

Respect Android reduced-motion and accessibility settings.

---

## 10. Accessibility
- Support Android font scaling.
- Minimum 48dp touch targets.
- Provide content descriptions.
- Never rely only on colour to indicate state.
- Maintain readable contrast.
- Support screen readers.
- Support landscape layouts where appropriate.
- Handle smaller displays without clipping.
- Use semantic Compose components.

---

## 11. Responsive Behaviour
Design for:
- Small Android phones.
- Standard Android phones.
- Large phones.
- Foldables where practical.

Do not hardcode a single device width.  
Use WindowSizeClass or adaptive layout strategies where appropriate.

---

## 12. Technical UI Rules
- Jetpack Compose only.
- Material 3 foundations.
- Reusable components.
- State hoisted to ViewModels.
- No direct database calls from Composables.
- No fake data in production.
- No hardcoded dashboard statistics.
- Preview functions for reusable components.
- Theme tokens must be centralized.
- Use vector icons rather than emoji as primary UI icons.

**Suggested component names:**
- DayflowScaffold
- DayflowBottomNavigation
- GreetingHeader
- DateSelector
- ProgressRing
- MultiProgressRing
- MetricCard
- ReminderCard
- TaskCard
- HabitCard
- PriorityBadge
- WeeklyBarChart
- HabitCalendar
- DailyTimeline
- EmptyState
- PermissionBanner
- DailyBriefingCard

---

## 13. Design Acceptance Criteria
The design is approved when:
- The visual language matches the supplied reference.
- Dark theme is the default.
- Light theme is available.
- All screens use consistent spacing and typography.
- Cards have consistent shape and elevation.
- Dashboard metrics are database-derived.
- Progress rings represent real values.
- Bottom navigation is consistent.
- Forms are usable with one hand.
- Notifications and permission screens are clear.
- Small-screen layouts remain usable.
- Accessibility requirements are satisfied.

END OF DESIGN SYSTEM.
