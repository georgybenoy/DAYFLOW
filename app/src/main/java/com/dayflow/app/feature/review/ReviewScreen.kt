package com.dayflow.app.feature.review

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Forward
import androidx.compose.material.icons.filled.NotInterested
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dayflow.app.core.designsystem.DayflowTheme
import com.dayflow.app.core.designsystem.DayflowTypography
import com.dayflow.app.core.designsystem.components.DateSelector
import com.dayflow.app.core.designsystem.components.PriorityBadge
import com.dayflow.app.core.time.DateTimeFormatters
import com.dayflow.app.domain.model.Priority
import com.dayflow.app.domain.model.ReviewDecisionType
import com.dayflow.app.domain.model.Task
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlan: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = DayflowTheme.colors
    val spacing = DayflowTheme.spacing

    var showRescheduleDialogForTaskId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Evening Review",
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
                    IconButton(onClick = { viewModel.loadReview(uiState.date) }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reload",
                            tint = colors.textSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
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
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                            text = "Failed to load review",
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
                            onClick = { viewModel.loadReview(uiState.date) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primary,
                                contentColor = colors.background
                            )
                        ) {
                            Text(text = "Retry")
                        }
                    }
                }
                else -> {
                    ReviewContent(
                        uiState = uiState,
                        onDateSelected = { viewModel.loadReview(it) },
                        onDecisionChanged = { taskId, decision, date ->
                            viewModel.setTaskDecision(taskId, decision, date)
                        },
                        onRequestReschedule = { taskId ->
                            showRescheduleDialogForTaskId = taskId
                        },
                        onAddInlineCommitment = { title, priority, duration ->
                            viewModel.addInlineCommitment(title, priority, duration)
                        },
                        onNotesChanged = { viewModel.updateNotes(it) },
                        onCompleteReview = {
                            viewModel.finishReview(onFinished = { tomorrow ->
                                onNavigateToPlan(tomorrow)
                            })
                        }
                    )
                }
            }
        }
    }

    if (showRescheduleDialogForTaskId != null) {
        val taskId = showRescheduleDialogForTaskId!!
        RescheduleChoiceDialog(
            currentDate = uiState.date,
            onDismiss = { showRescheduleDialogForTaskId = null },
            onDateChosen = { chosenDate ->
                viewModel.setTaskDecision(
                    taskId = taskId,
                    decision = ReviewDecisionType.RESCHEDULED,
                    rescheduledDate = chosenDate
                )
                showRescheduleDialogForTaskId = null
            }
        )
    }
}

@Composable
fun ReviewContent(
    uiState: ReviewUiState,
    onDateSelected: (LocalDate) -> Unit,
    onDecisionChanged: (taskId: String, decision: ReviewDecisionType, date: LocalDate?) -> Unit,
    onRequestReschedule: (taskId: String) -> Unit,
    onAddInlineCommitment: (title: String, priority: Priority, duration: Int) -> Unit,
    onNotesChanged: (String) -> Unit,
    onCompleteReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DayflowTheme.colors
    val spacing = DayflowTheme.spacing
    val shapes = DayflowTheme.shapes

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = spacing.screenHorizontal),
        verticalArrangement = Arrangement.spacedBy(spacing.md)
    ) {
        // Date switcher to reopen past reviews or navigate days
        item {
            Spacer(modifier = Modifier.height(spacing.xs))
            DateSelector(
                selectedDate = uiState.date,
                onDateSelected = onDateSelected
            )
        }

        // Executive Review Summary Card
        item {
            ReviewSummaryCard(uiState = uiState)
        }

        // Section: Unfinished Tasks (Core decision hub)
        item {
            ReviewSectionHeader(
                title = "Unfinished Commitments",
                count = uiState.unfinishedTasks.size,
                subtitle = if (uiState.unfinishedTasks.isNotEmpty()) {
                    "Undecided tasks default to Carry Forward into tomorrow's plan."
                } else {
                    "All commitments completed for today! Great job."
                }
            )
        }

        if (uiState.unfinishedTasks.isEmpty()) {
            item {
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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(spacing.sm))
                        Text(
                            text = "No unfinished tasks pending for this date.",
                            style = DayflowTypography.bodyMedium,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        } else {
            items(uiState.unfinishedTasks, key = { "unfinished_${it.id}" }) { task ->
                val decision = uiState.decisions[task.id]?.decision ?: ReviewDecisionType.CARRIED_FORWARD
                val rescheduledDate = uiState.decisions[task.id]?.rescheduledToDate
                UnfinishedTaskReviewCard(
                    task = task,
                    decision = decision,
                    rescheduledDate = rescheduledDate,
                    onSelectDecision = { selectedDecision ->
                        if (selectedDecision == ReviewDecisionType.RESCHEDULED) {
                            onRequestReschedule(task.id)
                        } else {
                            onDecisionChanged(task.id, selectedDecision, null)
                        }
                    },
                    onRequestReschedule = { onRequestReschedule(task.id) }
                )
            }
        }

        // Section: Completed Tasks
        if (uiState.completedTasks.isNotEmpty()) {
            item {
                ReviewSectionHeader(
                    title = "Completed Today",
                    count = uiState.completedTasks.size
                )
            }
            items(uiState.completedTasks, key = { "completed_${it.id}" }) { task ->
                CompletedTaskRow(task = task)
            }
        }

        // Section: Habit Progress
        if (uiState.habits.isNotEmpty()) {
            item {
                ReviewSectionHeader(
                    title = "Habits Progress",
                    count = uiState.habits.size,
                    subtitle = "${uiState.completedHabitsCount} of ${uiState.totalScheduledHabitsCount} targets met"
                )
            }
            items(uiState.habits, key = { "habit_${it.habit.id}" }) { habitItem ->
                HabitReviewRow(habitItem = habitItem)
            }
        }

        // Section: Add New Commitment Inline
        item {
            ReviewSectionHeader(
                title = "Add Commitment for Tomorrow",
                count = uiState.newCommitments.size,
                subtitle = "Queue new tasks directly into tomorrow's plan"
            )
        }
        item {
            InlineCommitmentCreator(
                onAdd = onAddInlineCommitment,
                newCommitments = uiState.newCommitments
            )
        }

        // Section: Tomorrow's Preview
        item {
            TomorrowPlanPreviewCard(
                tomorrowDate = uiState.date.plusDays(1),
                carriedForwardCount = uiState.decisions.values.count { it.decision == ReviewDecisionType.CARRIED_FORWARD },
                rescheduledCount = uiState.decisions.values.count { it.decision == ReviewDecisionType.RESCHEDULED },
                newCommitmentsCount = uiState.newCommitments.size
            )
        }

        // Section: Notes / Reflections
        item {
            ReviewSectionHeader(title = "Evening Reflection & Notes", count = 0)
        }
        item {
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = onNotesChanged,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("What went well today? Any key takeaways or notes?") },
                shape = shapes.input,
                minLines = 3,
                maxLines = 5,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.border,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary
                )
            )
        }

        // Final Action: Complete Review & Plan Tomorrow
        item {
            Spacer(modifier = Modifier.height(spacing.sm))
            Button(
                onClick = onCompleteReview,
                enabled = !uiState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = shapes.button,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.background
                )
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = colors.background,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (uiState.isExistingReviewSaved) "Update Review & Re-plan Tomorrow" else "Complete Review & Plan Tomorrow",
                        style = DayflowTypography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.width(spacing.sm))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(spacing.xl))
        }
    }
}

@Composable
fun ReviewSummaryCard(
    uiState: ReviewUiState,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daily Summary",
                    style = DayflowTypography.titleMedium,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                if (uiState.isExistingReviewSaved) {
                    Surface(
                        color = colors.surfaceVariant,
                        shape = shapes.chip
                    ) {
                        Text(
                            text = "Review Saved",
                            style = DayflowTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = colors.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(spacing.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                Surface(
                    color = colors.surfaceElevated,
                    shape = shapes.chip
                ) {
                    Text(
                        text = "${uiState.completedTasks.size} Completed",
                        style = DayflowTypography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                Surface(
                    color = colors.surfaceElevated,
                    shape = shapes.chip
                ) {
                    Text(
                        text = "${uiState.unfinishedTasks.size} Unfinished",
                        style = DayflowTypography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.textPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                Surface(
                    color = colors.surfaceElevated,
                    shape = shapes.chip
                ) {
                    Text(
                        text = "${uiState.completedHabitsCount}/${uiState.totalScheduledHabitsCount} Habits",
                        style = DayflowTypography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.textSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UnfinishedTaskReviewCard(
    task: Task,
    decision: ReviewDecisionType,
    rescheduledDate: String?,
    onSelectDecision: (ReviewDecisionType) -> Unit,
    onRequestReschedule: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DayflowTheme.colors
    val spacing = DayflowTheme.spacing
    val shapes = DayflowTheme.shapes

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = shapes.secondaryCard,
        colors = CardDefaults.cardColors(
            containerColor = when (decision) {
                ReviewDecisionType.CARRIED_FORWARD -> colors.surface
                ReviewDecisionType.COMPLETED -> colors.surfaceElevated
                ReviewDecisionType.RESCHEDULED -> colors.surface
                ReviewDecisionType.CANCELLED -> colors.surfaceElevated
                ReviewDecisionType.EXCLUDED -> colors.surfaceElevated
            }
        ),
        border = BorderStroke(
            1.dp,
            when (decision) {
                ReviewDecisionType.CARRIED_FORWARD -> colors.primary.copy(alpha = 0.5f)
                ReviewDecisionType.COMPLETED -> colors.primary
                ReviewDecisionType.RESCHEDULED -> colors.secondary
                ReviewDecisionType.CANCELLED -> colors.error.copy(alpha = 0.5f)
                ReviewDecisionType.EXCLUDED -> colors.border
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.cardPadding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PriorityBadge(priority = task.priority)
                Spacer(modifier = Modifier.width(spacing.sm))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = DayflowTypography.bodyMedium,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (decision == ReviewDecisionType.COMPLETED || decision == ReviewDecisionType.CANCELLED) {
                            TextDecoration.LineThrough
                        } else null
                    )
                    if (task.deadlineDate != null) {
                        Text(
                            text = "Deadline: ${task.deadlineDate}",
                            style = DayflowTypography.labelSmall,
                            color = colors.error
                        )
                    }
                }
            }

            if (decision == ReviewDecisionType.RESCHEDULED && rescheduledDate != null) {
                Spacer(modifier = Modifier.height(spacing.xs))
                Row(
                    modifier = Modifier
                        .clip(shapes.chip)
                        .background(colors.surfaceElevated)
                        .clickable(onClick = onRequestReschedule)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = colors.secondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Rescheduled to $rescheduledDate (tap to edit)",
                        style = DayflowTypography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = colors.secondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.sm))

            Text(
                text = "Choose action (undecided defaults to Carry Forward):",
                style = DayflowTypography.labelSmall,
                color = colors.textMuted
            )

            Spacer(modifier = Modifier.height(spacing.xs))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                verticalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                DecisionChip(
                    text = "Carry Forward",
                    icon = Icons.AutoMirrored.Filled.Forward,
                    isSelected = decision == ReviewDecisionType.CARRIED_FORWARD,
                    activeColor = colors.primary,
                    activeContainerColor = colors.surfaceVariant,
                    onClick = { onSelectDecision(ReviewDecisionType.CARRIED_FORWARD) }
                )
                DecisionChip(
                    text = "Reschedule",
                    icon = Icons.Default.EventRepeat,
                    isSelected = decision == ReviewDecisionType.RESCHEDULED,
                    activeColor = colors.secondary,
                    activeContainerColor = colors.surfaceElevated,
                    onClick = { onSelectDecision(ReviewDecisionType.RESCHEDULED) }
                )
                DecisionChip(
                    text = "Complete",
                    icon = Icons.Default.Check,
                    isSelected = decision == ReviewDecisionType.COMPLETED,
                    activeColor = colors.primary,
                    activeContainerColor = colors.surfaceVariant,
                    onClick = { onSelectDecision(ReviewDecisionType.COMPLETED) }
                )
                DecisionChip(
                    text = "Exclude",
                    icon = Icons.Default.NotInterested,
                    isSelected = decision == ReviewDecisionType.EXCLUDED,
                    activeColor = colors.textSecondary,
                    activeContainerColor = colors.surfaceElevated,
                    onClick = { onSelectDecision(ReviewDecisionType.EXCLUDED) }
                )
                DecisionChip(
                    text = "Cancel",
                    icon = Icons.Default.Close,
                    isSelected = decision == ReviewDecisionType.CANCELLED,
                    activeColor = colors.error,
                    activeContainerColor = colors.surfaceElevated,
                    onClick = { onSelectDecision(ReviewDecisionType.CANCELLED) }
                )
            }
        }
    }
}

@Composable
fun DecisionChip(
    text: String,
    icon: ImageVector,
    isSelected: Boolean,
    activeColor: Color,
    activeContainerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DayflowTheme.colors
    val shapes = DayflowTheme.shapes

    Surface(
        modifier = modifier
            .clip(shapes.chip)
            .clickable(onClick = onClick),
        shape = shapes.chip,
        color = if (isSelected) activeContainerColor else colors.surfaceElevated,
        border = BorderStroke(1.dp, if (isSelected) activeColor else colors.border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) activeColor else colors.textSecondary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = text,
                style = DayflowTypography.labelSmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                color = if (isSelected) activeColor else colors.textSecondary
            )
        }
    }
}

@Composable
fun CompletedTaskRow(
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
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(spacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = DayflowTypography.bodyMedium,
                    color = colors.textSecondary,
                    textDecoration = TextDecoration.LineThrough,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            PriorityBadge(priority = task.priority)
        }
    }
}

@Composable
fun HabitReviewRow(
    habitItem: HabitReviewItem,
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
                modifier = Modifier.size(20.dp)
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
                    text = "Goal: ${habitItem.goalFormatted}",
                    style = DayflowTypography.labelSmall,
                    color = colors.textSecondary
                )
            }
            Surface(
                color = if (habitItem.isCompleted) colors.surfaceVariant else colors.surfaceElevated,
                shape = shapes.chip
            ) {
                Text(
                    text = habitItem.progressFormatted,
                    style = DayflowTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (habitItem.isCompleted) colors.primary else colors.textSecondary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun InlineCommitmentCreator(
    onAdd: (title: String, priority: Priority, duration: Int) -> Unit,
    newCommitments: List<Task>,
    modifier: Modifier = Modifier
) {
    val colors = DayflowTheme.colors
    val spacing = DayflowTheme.spacing
    val shapes = DayflowTheme.shapes

    var title by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(Priority.HIGH) }
    var durationMinutes by remember { mutableIntStateOf(30) }

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
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. Review Q3 Deck with Team") },
                shape = shapes.input,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.border,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary
                )
            )

            Spacer(modifier = Modifier.height(spacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Priority picker
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(Priority.HIGH, Priority.MEDIUM).forEach { p ->
                        Surface(
                            modifier = Modifier
                                .clip(shapes.chip)
                                .clickable { priority = p },
                            color = if (priority == p) colors.surfaceVariant else colors.surfaceElevated,
                            shape = shapes.chip,
                            border = BorderStroke(1.dp, if (priority == p) colors.primary else colors.border)
                        ) {
                            Text(
                                text = p.name,
                                style = DayflowTypography.labelSmall.copy(fontWeight = if (priority == p) FontWeight.Bold else FontWeight.Normal),
                                color = if (priority == p) colors.primary else colors.textSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Duration picker
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(15, 30, 45, 60).forEach { d ->
                        Surface(
                            modifier = Modifier
                                .clip(shapes.chip)
                                .clickable { durationMinutes = d },
                            color = if (durationMinutes == d) colors.surfaceElevated else colors.surface,
                            shape = shapes.chip,
                            border = BorderStroke(1.dp, if (durationMinutes == d) colors.secondary else colors.border)
                        ) {
                            Text(
                                text = "${d}m",
                                style = DayflowTypography.labelSmall.copy(fontWeight = if (durationMinutes == d) FontWeight.Bold else FontWeight.Normal),
                                color = if (durationMinutes == d) colors.secondary else colors.textSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing.sm))

            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onAdd(title.trim(), priority, durationMinutes)
                        title = ""
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                shape = shapes.button,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.background
                )
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add to Tomorrow's Plan", style = DayflowTypography.labelMedium)
            }

            if (newCommitments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(spacing.sm))
                Text(
                    text = "Added for tomorrow (${newCommitments.size}):",
                    style = DayflowTypography.labelSmall,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(spacing.xs))
                newCommitments.forEach { commitment ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(spacing.xs))
                        Text(
                            text = commitment.title,
                            style = DayflowTypography.bodySmall,
                            color = colors.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${commitment.estimatedDurationMinutes}m",
                            style = DayflowTypography.labelSmall,
                            color = colors.textMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TomorrowPlanPreviewCard(
    tomorrowDate: LocalDate,
    carriedForwardCount: Int,
    rescheduledCount: Int,
    newCommitmentsCount: Int,
    modifier: Modifier = Modifier
) {
    val colors = DayflowTheme.colors
    val spacing = DayflowTheme.spacing
    val shapes = DayflowTheme.shapes

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = shapes.mainCard,
        colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated),
        border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.5f))
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
                Text(
                    text = "Tomorrow's Plan Preview",
                    style = DayflowTypography.titleMedium,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = colors.surfaceVariant,
                    shape = shapes.chip
                ) {
                    Text(
                        text = tomorrowDate.toString(),
                        style = DayflowTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = colors.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(spacing.xs))
            Text(
                text = "When you complete this review, DAYFLOW's deterministic planner will immediately generate tomorrow's schedule incorporating:",
                style = DayflowTypography.bodySmall,
                color = colors.textSecondary
            )
            Spacer(modifier = Modifier.height(spacing.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                Surface(color = colors.surface, shape = shapes.chip) {
                    Text(
                        text = "$carriedForwardCount Carried Forward",
                        style = DayflowTypography.labelSmall,
                        color = colors.textPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                if (rescheduledCount > 0) {
                    Surface(color = colors.surface, shape = shapes.chip) {
                        Text(
                            text = "$rescheduledCount Rescheduled",
                            style = DayflowTypography.labelSmall,
                            color = colors.textPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                if (newCommitmentsCount > 0) {
                    Surface(color = colors.surface, shape = shapes.chip) {
                        Text(
                            text = "$newCommitmentsCount New",
                            style = DayflowTypography.labelSmall,
                            color = colors.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewSectionHeader(
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
            if (count > 0) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RescheduleChoiceDialog(
    currentDate: LocalDate,
    onDismiss: () -> Unit,
    onDateChosen: (LocalDate) -> Unit
) {
    val colors = DayflowTheme.colors
    val shapes = DayflowTheme.shapes

    var showCustomDatePicker by remember { mutableStateOf(false) }

    if (showCustomDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = currentDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showCustomDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMillis = datePickerState.selectedDateMillis
                        if (selectedMillis != null) {
                            val picked = Instant.ofEpochMilli(selectedMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                            onDateChosen(picked)
                        }
                    }
                ) {
                    Text("Select", color = colors.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDatePicker = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text("Reschedule Task", style = DayflowTypography.titleMedium, color = colors.textPrimary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Choose when you plan to work on this task. Note: Task deadline is preserved independently.",
                        style = DayflowTypography.bodySmall,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { onDateChosen(currentDate.plusDays(1)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = shapes.button
                    ) {
                        Text("Tomorrow (${currentDate.plusDays(1)})", color = colors.textPrimary)
                    }
                    OutlinedButton(
                        onClick = { onDateChosen(currentDate.plusDays(2)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = shapes.button
                    ) {
                        Text("In 2 Days (${currentDate.plusDays(2)})", color = colors.textPrimary)
                    }
                    OutlinedButton(
                        onClick = { onDateChosen(currentDate.plusWeeks(1)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = shapes.button
                    ) {
                        Text("Next Week (${currentDate.plusWeeks(1)})", color = colors.textPrimary)
                    }
                    Button(
                        onClick = { showCustomDatePicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = shapes.button,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = colors.background
                        )
                    ) {
                        Text("Pick Custom Date")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }
}