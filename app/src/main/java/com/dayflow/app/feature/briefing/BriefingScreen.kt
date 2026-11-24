package com.dayflow.app.feature.briefing

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dayflow.app.core.designsystem.DayflowTheme
import com.dayflow.app.core.designsystem.DayflowTypography
import com.dayflow.app.core.designsystem.components.PriorityBadge
import com.dayflow.app.core.time.DateTimeFormatters
import com.dayflow.app.domain.briefing.BriefingContent
import com.dayflow.app.domain.briefing.BriefingHabitItem
import com.dayflow.app.domain.model.Priority
import com.dayflow.app.domain.model.Reminder
import com.dayflow.app.domain.model.Task
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BriefingScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlan: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BriefingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = DayflowTheme.colors
    val spacing = DayflowTheme.spacing

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Morning Briefing",
                            style = DayflowTypography.titleLarge,
                            color = colors.textPrimary
                        )
                        Text(
                            text = DateTimeFormatters.formatFullDate(uiState.date),
                            style = DayflowTypography.labelSmall,
                            color = colors.textSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.textPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadBriefing() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = colors.textSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = colors.primary)
                    }
                }
                uiState.errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(spacing.screenHorizontal),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Failed to load briefing",
                            style = DayflowTypography.titleMedium,
                            color = colors.error
                        )
                        Spacer(modifier = Modifier.height(spacing.sm))
                        Text(
                            text = uiState.errorMessage ?: "",
                            style = DayflowTypography.bodyMedium,
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(spacing.md))
                        Button(
                            onClick = { viewModel.loadBriefing() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primary,
                                contentColor = colors.background
                            )
                        ) {
                            Text(text = "Retry")
                        }
                    }
                }
                uiState.briefing != null -> {
                    val briefing = uiState.briefing!!
                    BriefingContentList(
                        briefing = briefing,
                        onOpenPlan = { onNavigateToPlan(uiState.date) }
                    )
                }
            }
        }
    }
}

@Composable
fun BriefingContentList(
    briefing: BriefingContent,
    onOpenPlan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = DayflowTheme.spacing
    val colors = DayflowTheme.colors
    val shapes = DayflowTheme.shapes

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = spacing.screenHorizontal),
        verticalArrangement = Arrangement.spacedBy(spacing.md)
    ) {
        item {
            Spacer(modifier = Modifier.height(spacing.xs))
            BriefingGreetingCard(briefing = briefing)
        }

        item {
            BriefingScheduleCard(
                briefing = briefing,
                onOpenPlan = onOpenPlan
            )
        }

        if (briefing.priorities.isNotEmpty()) {
            item {
                BriefingSectionHeader(title = "High Priority Commitments", count = briefing.priorities.size)
            }
            items(briefing.priorities, key = { "priority_${it.id}" }) { task ->
                BriefingTaskItem(task = task)
            }
        }

        if (briefing.habitsChecklist.isNotEmpty()) {
            val completedCount = briefing.habitsChecklist.count { it.isCompleted }
            val totalCount = briefing.habitsChecklist.size
            item {
                BriefingSectionHeader(
                    title = "Habits Today",
                    count = totalCount,
                    subtitle = "$completedCount of $totalCount completed"
                )
            }
            items(briefing.habitsChecklist, key = { "habit_${it.habit.id}" }) { habitItem ->
                BriefingHabitRow(habitItem = habitItem)
            }
        }

        if (briefing.deadlines.isNotEmpty()) {
            item {
                BriefingSectionHeader(title = "Deadlines Due Today", count = briefing.deadlines.size)
            }
            items(briefing.deadlines, key = { "deadline_${it.id}" }) { task ->
                BriefingDeadlineItem(task = task)
            }
        }

        if (briefing.importantReminders.isNotEmpty()) {
            item {
                BriefingSectionHeader(title = "Important Reminders", count = briefing.importantReminders.size)
            }
            items(briefing.importantReminders, key = { "reminder_${it.id}" }) { reminder ->
                BriefingReminderItem(reminder = reminder)
            }
        }

        item {
            Spacer(modifier = Modifier.height(spacing.sm))
            Button(
                onClick = onOpenPlan,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = shapes.button,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.background
                )
            ) {
                Text(
                    text = "Open Daily Plan",
                    style = DayflowTypography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.width(spacing.sm))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(spacing.xl))
        }
    }
}

@Composable
fun BriefingGreetingCard(
    briefing: BriefingContent,
    modifier: Modifier = Modifier
) {
    val colors = DayflowTheme.colors
    val spacing = DayflowTheme.spacing
    val shapes = DayflowTheme.shapes

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = shapes.mainCard,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.cardPadding)
        ) {
            Text(
                text = briefing.greeting,
                style = DayflowTypography.headlineLarge,
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(spacing.xs))
            Text(
                text = briefing.summaryText,
                style = DayflowTypography.bodyMedium,
                color = colors.textSecondary
            )
            Spacer(modifier = Modifier.height(spacing.md))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                BriefingStatBadge(
                    label = "${briefing.pendingTasksCount} Tasks",
                    containerColor = colors.surfaceElevated,
                    contentColor = colors.textPrimary
                )
                val habitsDone = briefing.habitsChecklist.count { it.isCompleted }
                val habitsTotal = briefing.habitsChecklist.size
                if (habitsTotal > 0) {
                    BriefingStatBadge(
                        label = "$habitsDone/$habitsTotal Habits",
                        containerColor = if (habitsDone == habitsTotal) colors.surfaceVariant else colors.surfaceElevated,
                        contentColor = if (habitsDone == habitsTotal) colors.primary else colors.textPrimary
                    )
                }
                if (briefing.deadlines.isNotEmpty()) {
                    BriefingStatBadge(
                        label = "${briefing.deadlines.size} Deadline${if (briefing.deadlines.size > 1) "s" else ""}",
                        containerColor = colors.surfaceElevated,
                        contentColor = colors.error
                    )
                }
            }
        }
    }
}

@Composable
fun BriefingScheduleCard(
    briefing: BriefingContent,
    onOpenPlan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DayflowTheme.colors
    val spacing = DayflowTheme.spacing
    val shapes = DayflowTheme.shapes

    val hasScheduledItems = briefing.scheduledTasks.isNotEmpty()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = shapes.mainCard,
        colors = CardDefaults.cardColors(
            containerColor = if (hasScheduledItems) colors.surface else colors.surfaceElevated
        ),
        border = BorderStroke(
            1.dp,
            if (hasScheduledItems) colors.primary.copy(alpha = 0.5f) else colors.border
        )
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = if (hasScheduledItems) colors.primary else colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Today's Schedule",
                        style = DayflowTypography.titleMedium,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Surface(
                    color = if (hasScheduledItems) colors.surfaceVariant else colors.surfaceElevated,
                    shape = shapes.chip
                ) {
                    Text(
                        text = if (hasScheduledItems) "Plan Approved" else "Draft Ready",
                        style = DayflowTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (hasScheduledItems) colors.primary else colors.textSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(spacing.xs))
            Text(
                text = if (hasScheduledItems) {
                    "${briefing.scheduledTasks.size} activities scheduled and ready to execute."
                } else {
                    "${briefing.pendingTasksCount} tasks eligible for today's deterministic plan."
                },
                style = DayflowTypography.bodySmall,
                color = colors.textSecondary
            )
            Spacer(modifier = Modifier.height(spacing.sm))
            OutlinedButton(
                onClick = onOpenPlan,
                modifier = Modifier.fillMaxWidth(),
                shape = shapes.button,
                border = BorderStroke(1.dp, colors.primary)
            ) {
                Text(
                    text = if (hasScheduledItems) "View Full Schedule" else "Review Draft Plan",
                    style = DayflowTypography.labelMedium,
                    color = colors.primary
                )
            }
        }
    }
}

@Composable
fun BriefingHabitRow(
    habitItem: BriefingHabitItem,
    modifier: Modifier = Modifier
) {
    val colors = DayflowTheme.colors
    val spacing = DayflowTheme.spacing
    val shapes = DayflowTheme.shapes

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = shapes.secondaryCard,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.md, vertical = spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (habitItem.isCompleted) Icons.Default.CheckCircle else Icons.Outlined.CheckCircleOutline,
                contentDescription = null,
                tint = if (habitItem.isCompleted) colors.primary else colors.textMuted,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(spacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = habitItem.habit.name,
                    style = DayflowTypography.bodyMedium,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Goal: ${habitItem.habit.targetValue}${if (!habitItem.habit.unit.isNullOrBlank()) " ${habitItem.habit.unit}" else ""}",
                    style = DayflowTypography.labelSmall,
                    color = colors.textSecondary
                )
            }
            Surface(
                color = if (habitItem.isCompleted) colors.surfaceVariant else colors.surfaceElevated,
                shape = shapes.chip
            ) {
                Text(
                    text = habitItem.formattedProgress,
                    style = DayflowTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (habitItem.isCompleted) colors.primary else colors.textSecondary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun BriefingTaskItem(
    task: Task,
    modifier: Modifier = Modifier
) {
    val colors = DayflowTheme.colors
    val spacing = DayflowTheme.spacing
    val shapes = DayflowTheme.shapes

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = shapes.secondaryCard,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.md, vertical = spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PriorityBadge(priority = task.priority)
            Spacer(modifier = Modifier.width(spacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = DayflowTypography.bodyMedium,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (task.plannedStart != null) {
                    Text(
                        text = "Planned at ${DateTimeFormatters.formatTime(task.plannedStart)}",
                        style = DayflowTypography.labelSmall,
                        color = colors.textSecondary
                    )
                }
            }
            if (task.estimatedDurationMinutes != null && task.estimatedDurationMinutes > 0) {
                Text(
                    text = "${task.estimatedDurationMinutes}m",
                    style = DayflowTypography.labelSmall,
                    color = colors.textMuted,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun BriefingDeadlineItem(
    task: Task,
    modifier: Modifier = Modifier
) {
    val colors = DayflowTheme.colors
    val spacing = DayflowTheme.spacing
    val shapes = DayflowTheme.shapes

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = shapes.secondaryCard,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.error.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.md, vertical = spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Deadline warning",
                tint = colors.error,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(spacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = DayflowTypography.bodyMedium,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (task.deadlineTime != null) "Due at ${DateTimeFormatters.formatTime(task.deadlineTime)}" else "Due today",
                    style = DayflowTypography.labelSmall,
                    color = colors.error
                )
            }
            PriorityBadge(priority = task.priority)
        }
    }
}

@Composable
fun BriefingReminderItem(
    reminder: Reminder,
    modifier: Modifier = Modifier
) {
    val colors = DayflowTheme.colors
    val spacing = DayflowTheme.spacing
    val shapes = DayflowTheme.shapes

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = shapes.secondaryCard,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.md, vertical = spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(spacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.title,
                    style = DayflowTypography.bodyMedium,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = reminder.type.name.replace("_", " "),
                    style = DayflowTypography.labelSmall,
                    color = colors.textSecondary
                )
            }
            if (reminder.priority != Priority.MEDIUM) {
                PriorityBadge(priority = reminder.priority)
            }
        }
    }
}

@Composable
fun BriefingSectionHeader(
    title: String,
    count: Int,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    val colors = DayflowTheme.colors

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = DayflowTypography.titleMedium,
                color = colors.textPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Surface(
                color = colors.surfaceElevated,
                shape = DayflowTheme.shapes.chip
            ) {
                Text(
                    text = count.toString(),
                    style = DayflowTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = colors.textSecondary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = DayflowTypography.bodySmall,
                color = colors.textSecondary
            )
        }
    }
}

@Composable
fun BriefingStatBadge(
    label: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        shape = DayflowTheme.shapes.chip
    ) {
        Text(
            text = label,
            style = DayflowTypography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = contentColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}