package com.dayflow.app.feature.today

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dayflow.app.core.designsystem.DayflowTheme
import com.dayflow.app.core.designsystem.DayflowTypography
import com.dayflow.app.core.designsystem.components.DateSelector
import com.dayflow.app.core.designsystem.components.EmptyState
import com.dayflow.app.core.designsystem.components.GreetingHeader
import com.dayflow.app.core.designsystem.components.MetricCard
import com.dayflow.app.core.designsystem.components.PriorityBadge
import com.dayflow.app.core.designsystem.components.ProgressRing
import com.dayflow.app.core.time.ReminderFormatter
import com.dayflow.app.core.time.TaskFormatter
import com.dayflow.app.domain.briefing.BriefingContent
import com.dayflow.app.domain.model.DailyPlanItem
import com.dayflow.app.domain.model.PlanItemSourceType
import com.dayflow.app.domain.model.PlanStatus
import com.dayflow.app.domain.model.Task
import com.dayflow.app.domain.model.TaskStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton

@Composable
fun TodayScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToCalendar: () -> Unit = {},
    onNavigateToCreateReminder: () -> Unit,
    onNavigateToCreateTask: () -> Unit,
    onNavigateToCreateHabit: () -> Unit,
    onNavigateToTaskDetail: (String) -> Unit = {},
    onNavigateToPlan: (LocalDate) -> Unit = {},
    onNavigateToBriefing: (LocalDate) -> Unit = {},
    onNavigateToReview: (LocalDate) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: TodayViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    TodayContent(
        uiState = uiState,
        onDateSelected = viewModel::selectDate,
        onSettingsClick = onNavigateToSettings,
        onCalendarClick = onNavigateToCalendar,
        onCreateReminderClick = onNavigateToCreateReminder,
        onCreateTaskClick = onNavigateToCreateTask,
        onCreateHabitClick = onNavigateToCreateHabit,
        onTaskClick = onNavigateToTaskDetail,
        onToggleTaskComplete = viewModel::toggleTaskComplete,
        onCompleteReminderClick = viewModel::completeReminder,
        onNavigateToPlan = onNavigateToPlan,
        onNavigateToBriefing = onNavigateToBriefing,
        onNavigateToReview = onNavigateToReview,
        onTogglePlanItemComplete = viewModel::togglePlanItemComplete,
        modifier = modifier
    )
}

@Composable
fun TodayContent(
    uiState: TodayUiState,
    onDateSelected: (LocalDate) -> Unit,
    onSettingsClick: () -> Unit,
    onCalendarClick: () -> Unit = {},
    onCreateReminderClick: () -> Unit,
    onCreateTaskClick: () -> Unit,
    onCreateHabitClick: () -> Unit,
    onTaskClick: (String) -> Unit = {},
    onToggleTaskComplete: (Task) -> Unit = {},
    onCompleteReminderClick: (String, String) -> Unit = { _, _ -> },
    onNavigateToPlan: (LocalDate) -> Unit = {},
    onNavigateToBriefing: (LocalDate) -> Unit = {},
    onNavigateToReview: (LocalDate) -> Unit = {},
    onTogglePlanItemComplete: (DailyPlanItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = DayflowTheme.colors
    val spacing = DayflowTheme.spacing
    val shapes = DayflowTheme.shapes
    val scrollState = rememberScrollState()
    val categoryMap = remember(uiState.categories) { uiState.categories.associateBy { it.id } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = spacing.largeSection)
    ) {
        // Greeting Header
        GreetingHeader(
            userName = uiState.userName,
            date = uiState.currentDate,
            onSettingsClick = onSettingsClick,
            onCalendarClick = onCalendarClick
        )

        // 7-day Date Selector
        DateSelector(
            selectedDate = uiState.selectedDate,
            onDateSelected = onDateSelected,
            baseDate = uiState.currentDate
        )

        Spacer(modifier = Modifier.height(spacing.sectionSpacing))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(spacing.cardSpacing)
        ) {
            // Daily Briefing Card
            uiState.briefing?.let { briefing ->
                DailyBriefingCard(
                    briefing = briefing,
                    onViewBriefingClick = { onNavigateToBriefing(uiState.selectedDate) },
                    onOpenPlanClick = { onNavigateToPlan(uiState.selectedDate) }
                )
            }

            // Section A: Daily Execution Ring
            Text(
                text = "Daily Execution",
                style = DayflowTypography.titleMedium,
                color = colors.textPrimary
            )

            if (uiState.totalTasksCount == 0) {
                MetricCard(
                    title = "Tasks For This Date",
                    value = "0 scheduled",
                    subtitle = "No tasks recorded for this day yet",
                    onClick = onCreateTaskClick
                )
            } else {
                Card(
                    shape = shapes.mainCard,
                    colors = CardDefaults.cardColors(
                        containerColor = colors.surface,
                        contentColor = colors.textPrimary
                    ),
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(spacing.cardPadding),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Daily Task Progress",
                                style = DayflowTypography.titleMedium,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${uiState.tasksCompletedCount} of ${uiState.totalTasksCount} tasks completed",
                                style = DayflowTypography.bodyMedium,
                                color = colors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (uiState.tasksCompletedCount == uiState.totalTasksCount) {
                                    "All tasks completed for this day!"
                                } else {
                                    "${uiState.totalTasksCount - uiState.tasksCompletedCount} remaining"
                                },
                                style = DayflowTypography.labelSmall,
                                color = colors.primary
                            )
                        }

                        val progressRatio = if (uiState.totalTasksCount > 0) {
                            uiState.tasksCompletedCount.toFloat() / uiState.totalTasksCount.toFloat()
                        } else 0f

                        ProgressRing(
                            progress = progressRatio,
                            size = 80.dp,
                            strokeWidth = 8.dp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing.sm))

            // Section B: Priority Tasks for Selected Date
            if (uiState.priorityTasks.isNotEmpty()) {
                Text(
                    text = "Priority Tasks",
                    style = DayflowTypography.titleMedium,
                    color = colors.textPrimary
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.priorityTasks.forEach { task ->
                        val isOverdue = uiState.overdueTaskIds.contains(task.id)
                        val deadlineText = TaskFormatter.formatDeadline(task, LocalDateTime.now(), isOverdue)
                        val category = task.categoryId?.let { categoryMap[it] }

                        Card(
                            shape = shapes.secondaryCard,
                            colors = CardDefaults.cardColors(
                                containerColor = colors.surface,
                                contentColor = colors.textPrimary
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isOverdue) colors.error.copy(alpha = 0.5f) else colors.border
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onTaskClick(task.id) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(spacing.cardPadding),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    IconButton(
                                        onClick = { onToggleTaskComplete(task) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (task.status == TaskStatus.COMPLETED) Icons.Filled.CheckCircle
                                            else Icons.Outlined.CheckCircleOutline,
                                            contentDescription = "Complete task ${task.title}",
                                            tint = if (task.status == TaskStatus.COMPLETED) colors.primary
                                            else if (isOverdue) colors.error else colors.textMuted
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = task.title,
                                            style = DayflowTypography.titleSmall,
                                            color = colors.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        deadlineText?.let { dl ->
                                            Text(
                                                text = dl,
                                                style = DayflowTypography.labelSmall,
                                                color = if (isOverdue) colors.error else colors.primary
                                            )
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    PriorityBadge(priority = task.priority)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(spacing.sm))
            }

            // Section C: Upcoming Deadlines
            if (uiState.upcomingDeadlines.isNotEmpty()) {
                Text(
                    text = "Upcoming Deadlines",
                    style = DayflowTypography.titleMedium,
                    color = colors.textPrimary
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.upcomingDeadlines.forEach { task ->
                        val isOverdue = uiState.overdueTaskIds.contains(task.id)
                        val deadlineText = TaskFormatter.formatDeadline(task, LocalDateTime.now(), isOverdue)

                        Card(
                            shape = shapes.secondaryCard,
                            colors = CardDefaults.cardColors(
                                containerColor = colors.surface,
                                contentColor = colors.textPrimary
                            ),
                            border = BorderStroke(1.dp, colors.border),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onTaskClick(task.id) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(spacing.cardPadding),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = task.title,
                                        style = DayflowTypography.titleSmall,
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    deadlineText?.let { dl ->
                                        Text(
                                            text = dl,
                                            style = DayflowTypography.labelSmall,
                                            color = if (isOverdue) colors.error else colors.primary
                                        )
                                    }
                                }
                                PriorityBadge(priority = task.priority)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(spacing.sm))
            }

            // Section D: Habit Overview
            Text(
                text = "Habit Overview",
                style = DayflowTypography.titleMedium,
                color = colors.textPrimary
            )
            if (uiState.totalHabitsCount == 0) {
                MetricCard(
                    title = "Today's Habits",
                    value = "0 active",
                    subtitle = "No active habits scheduled for this day",
                    isElevated = true,
                    onClick = onCreateHabitClick
                )
            } else {
                MetricCard(
                    title = "Today's Habits",
                    value = "${uiState.habitsCompletedCount} of ${uiState.totalHabitsCount} done",
                    subtitle = if (uiState.habitsCompletedCount == uiState.totalHabitsCount) {
                        "All habits completed! Perfect streak!"
                    } else {
                        "${uiState.totalHabitsCount - uiState.habitsCompletedCount} habits remaining today"
                    },
                    isElevated = true,
                    onClick = onCreateHabitClick
                )
            }

            Spacer(modifier = Modifier.height(spacing.sm))

            // Section E: Upcoming Reminders
            Text(
                text = "Upcoming Reminders",
                style = DayflowTypography.titleMedium,
                color = colors.textPrimary
            )
            if (uiState.upcomingReminders.isEmpty()) {
                EmptyState(
                    title = "No upcoming reminders",
                    message = "You're all caught up. Schedule reminders to receive timely notifications.",
                    icon = Icons.Outlined.NotificationsNone,
                    actionLabel = "Add Reminder",
                    onActionClick = onCreateReminderClick
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.upcomingReminders.forEach { item ->
                        Card(
                            shape = shapes.secondaryCard,
                            colors = CardDefaults.cardColors(
                                containerColor = colors.surface,
                                contentColor = colors.textPrimary
                            ),
                            border = BorderStroke(1.dp, colors.border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(spacing.cardPadding),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = item.reminder.title,
                                            style = DayflowTypography.titleSmall,
                                            color = colors.textPrimary
                                        )
                                        PriorityBadge(priority = item.reminder.priority)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = ReminderFormatter.formatOccurrenceTime(item.occurrence.scheduledAt, item.reminder.timeZoneId),
                                        style = DayflowTypography.labelSmall,
                                        color = colors.primary
                                    )
                                }

                                OutlinedButton(
                                    onClick = { onCompleteReminderClick(item.occurrence.id, item.reminder.id) },
                                    shape = shapes.button,
                                    border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.5f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = "Complete ${item.reminder.title}",
                                        modifier = Modifier.size(16.dp),
                                        tint = colors.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Complete",
                                        style = DayflowTypography.labelSmall,
                                        color = colors.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing.sm))

            // Section F: Today's Schedule
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's Schedule",
                    style = DayflowTypography.titleMedium,
                    color = colors.textPrimary
                )

                if (uiState.dailyPlan?.status == PlanStatus.APPROVED) {
                    TextButton(onClick = { onNavigateToPlan(uiState.selectedDate) }) {
                        Text(
                            text = "View Full Plan",
                            style = DayflowTypography.labelSmall,
                            color = colors.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            val currentPlan = uiState.dailyPlan
            val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()) }

            if (currentPlan != null && currentPlan.status == PlanStatus.APPROVED && currentPlan.items.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    currentPlan.items.forEach { item ->
                        Card(
                            shape = shapes.secondaryCard,
                            colors = CardDefaults.cardColors(containerColor = colors.surface),
                            border = BorderStroke(1.dp, if (item.isCompleted) colors.primary.copy(alpha = 0.3f) else colors.border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { onTogglePlanItemComplete(item) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = if (item.isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircleOutline,
                                        contentDescription = "Toggle completion",
                                        tint = if (item.isCompleted) colors.primary else colors.textMuted
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Box(
                                    modifier = Modifier
                                        .clip(shapes.chip)
                                        .background(colors.surfaceElevated)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${item.startTime.format(timeFormatter)} - ${item.endTime.format(timeFormatter)}",
                                        style = DayflowTypography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                        color = colors.primary
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = item.title,
                                    style = DayflowTypography.titleSmall,
                                    color = if (item.isCompleted) colors.textMuted else colors.textPrimary,
                                    textDecoration = if (item.isCompleted) TextDecoration.LineThrough else null,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                Text(
                                    text = when (item.sourceType) {
                                        PlanItemSourceType.TASK -> "Task"
                                        PlanItemSourceType.HABIT -> "Habit"
                                        PlanItemSourceType.CUSTOM -> "Event"
                                    },
                                    style = DayflowTypography.labelSmall,
                                    color = colors.textMuted
                                )
                            }
                        }
                    }
                }
            } else if (currentPlan != null && currentPlan.status == PlanStatus.DRAFT) {
                Card(
                    shape = shapes.mainCard,
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(spacing.cardPadding),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Draft Plan Ready",
                                style = DayflowTypography.titleSmall,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${currentPlan.items.size} tasks & habits scheduled for review.",
                                style = DayflowTypography.bodySmall,
                                color = colors.textSecondary
                            )
                        }

                        Button(
                            onClick = { onNavigateToPlan(uiState.selectedDate) },
                            shape = shapes.button,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primary,
                                contentColor = colors.background
                            )
                        ) {
                            Text(text = "Review Plan", style = DayflowTypography.labelSmall)
                        }
                    }
                }
            } else {
                EmptyState(
                    title = "No plan generated yet",
                    message = "Balance tasks, habits, and commitments into a daily plan.",
                    icon = Icons.Outlined.Schedule,
                    actionLabel = "Plan My Day",
                    onActionClick = { onNavigateToPlan(uiState.selectedDate) }
                )
            }

            Spacer(modifier = Modifier.height(spacing.sm))

            // Section G: Evening Review
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = shapes.mainCard,
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = BorderStroke(1.dp, colors.border)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(spacing.cardPadding),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Evening Review",
                            style = DayflowTypography.titleMedium,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Close today's unfinished work & prepare tomorrow's draft plan.",
                            style = DayflowTypography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                    Button(
                        onClick = { onNavigateToReview(uiState.selectedDate) },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceElevated)
                    ) {
                        Text(
                            text = "Review",
                            style = DayflowTypography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = colors.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DailyBriefingCard(
    briefing: BriefingContent,
    onViewBriefingClick: () -> Unit,
    onOpenPlanClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DayflowTheme.colors
    val shapes = DayflowTheme.shapes
    val spacing = DayflowTheme.spacing

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = shapes.mainCard,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.cardPadding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(shapes.chip)
                        .background(colors.primary.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "MORNING BRIEFING",
                        style = DayflowTypography.labelSmall,
                        color = colors.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                TextButton(onClick = onViewBriefingClick) {
                    Text(
                        text = "View Briefing",
                        style = DayflowTypography.labelSmall,
                        color = colors.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = briefing.greeting,
                style = DayflowTypography.titleMedium,
                color = colors.textPrimary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = briefing.summaryText,
                style = DayflowTypography.bodySmall,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Indicator pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (briefing.priorities.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(shapes.chip)
                            .background(colors.surfaceElevated)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "🔥 ${briefing.priorities.size} Priority",
                            style = DayflowTypography.labelSmall,
                            color = colors.textPrimary
                        )
                    }
                }

                if (briefing.deadlines.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(shapes.chip)
                            .background(colors.surfaceElevated)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "⏰ ${briefing.deadlines.size} Deadline",
                            style = DayflowTypography.labelSmall,
                            color = colors.textPrimary
                        )
                    }
                }

                if (briefing.habitsChecklist.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(shapes.chip)
                            .background(colors.surfaceElevated)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "🌱 ${briefing.habitsChecklist.size} Habits",
                            style = DayflowTypography.labelSmall,
                            color = colors.textPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = onOpenPlanClick,
                    shape = shapes.button,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.background
                    )
                ) {
                    Text(
                        text = "Open Plan",
                        style = DayflowTypography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Preview(name = "TodayScreen Dark", showBackground = true, backgroundColor = 0xFF07120F)
@Composable
private fun TodayScreenPreview() {
    DayflowTheme {
        TodayContent(
            uiState = TodayUiState(
                userName = "Alex",
                selectedDate = LocalDate.now(),
                currentDate = LocalDate.now()
            ),
            onDateSelected = {},
            onSettingsClick = {},
            onCreateReminderClick = {},
            onCreateTaskClick = {},
            onCreateHabitClick = {}
        )
    }
}
