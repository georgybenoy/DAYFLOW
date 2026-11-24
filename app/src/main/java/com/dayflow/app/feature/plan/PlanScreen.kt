package com.dayflow.app.feature.plan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dayflow.app.core.designsystem.DayflowTheme
import com.dayflow.app.core.designsystem.DayflowTypography
import com.dayflow.app.core.designsystem.components.EmptyState
import com.dayflow.app.core.designsystem.components.PriorityBadge
import com.dayflow.app.domain.model.DailyPlanItem
import com.dayflow.app.domain.model.PlanItemSourceType
import com.dayflow.app.domain.model.PlanStatus
import com.dayflow.app.domain.model.Task
import com.dayflow.app.domain.planner.PlanConflict
import com.dayflow.app.domain.planner.UnscheduledItem
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun PlanScreen(
    onNavigateBack: () -> Unit,
    viewModel: PlanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    PlanContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onMoveItemUp = viewModel::moveItemUp,
        onMoveItemDown = viewModel::moveItemDown,
        onUpdateItemTime = viewModel::updateItemTime,
        onRemoveItem = viewModel::removeItemToUnscheduled,
        onScheduleTask = viewModel::scheduleUnscheduledTask,
        onToggleItemCompletion = viewModel::toggleItemCompletion,
        onRegenerate = viewModel::regeneratePlan,
        onApprove = viewModel::approvePlan
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanContent(
    uiState: PlanUiState,
    onNavigateBack: () -> Unit,
    onMoveItemUp: (Int) -> Unit,
    onMoveItemDown: (Int) -> Unit,
    onUpdateItemTime: (String, LocalTime, LocalTime) -> Unit,
    onRemoveItem: (String) -> Unit,
    onScheduleTask: (Task) -> Unit,
    onToggleItemCompletion: (String) -> Unit,
    onRegenerate: () -> Unit,
    onApprove: () -> Unit
) {
    val colors = DayflowTheme.colors
    val shapes = DayflowTheme.shapes
    val spacing = DayflowTheme.spacing

    var editingItem by remember { mutableStateOf<DailyPlanItem?>(null) }

    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault()) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Daily Plan",
                            style = DayflowTypography.titleMedium,
                            color = colors.textPrimary
                        )
                        Text(
                            text = uiState.date.format(dateFormatter),
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
                    IconButton(
                        onClick = onRegenerate,
                        enabled = !uiState.isLoading && !uiState.isSaving
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Regenerate Draft Plan",
                            tint = colors.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background
                )
            )
        },
        bottomBar = {
            Surface(
                color = colors.surface,
                tonalElevation = 4.dp,
                border = BorderStroke(1.dp, colors.border)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.screenHorizontal, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onRegenerate,
                        modifier = Modifier.weight(1f),
                        shape = shapes.button,
                        enabled = !uiState.isLoading && !uiState.isSaving,
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Regenerate",
                            style = DayflowTypography.labelLarge,
                            color = colors.textPrimary
                        )
                    }

                    Button(
                        onClick = onApprove,
                        modifier = Modifier.weight(1.5f),
                        shape = shapes.button,
                        enabled = !uiState.isLoading && !uiState.isSaving,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = colors.background
                        )
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = colors.background,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (uiState.planStatus == PlanStatus.APPROVED) "Update Plan" else "Approve Plan",
                                style = DayflowTypography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        },
        containerColor = colors.background
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = colors.primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = spacing.screenHorizontal),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    // Status Badge Banner
                    PlanStatusHeader(
                        status = uiState.planStatus,
                        totalScheduledMinutes = uiState.totalScheduledMinutes,
                        scheduledItemCount = uiState.scheduledItems.size
                    )
                }

                // Conflict Warnings
                if (uiState.conflicts.isNotEmpty()) {
                    item {
                        ConflictBanner(conflicts = uiState.conflicts)
                    }
                }

                // Section 1: Scheduled Items
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Scheduled Execution (${uiState.scheduledItems.size})",
                            style = DayflowTypography.titleMedium,
                            color = colors.textPrimary
                        )
                        val hours = uiState.totalScheduledMinutes / 60
                        val mins = uiState.totalScheduledMinutes % 60
                        Text(
                            text = "${hours}h ${mins}m scheduled",
                            style = DayflowTypography.labelSmall,
                            color = colors.primary
                        )
                    }
                }

                if (uiState.scheduledItems.isEmpty()) {
                    item {
                        EmptyState(
                            title = "No scheduled items",
                            message = "No items could fit into the available window. Check unscheduled tasks below.",
                            icon = Icons.Outlined.Schedule
                        )
                    }
                } else {
                    itemsIndexed(uiState.scheduledItems, key = { _, item -> item.id }) { index, item ->
                        PlanItemCard(
                            item = item,
                            index = index,
                            totalCount = uiState.scheduledItems.size,
                            timeFormatter = timeFormatter,
                            onMoveUp = { onMoveItemUp(index) },
                            onMoveDown = { onMoveItemDown(index) },
                            onEditTime = { editingItem = item },
                            onRemove = { onRemoveItem(item.id) },
                            onToggleCompletion = { onToggleItemCompletion(item.id) }
                        )
                    }
                }

                // Section 2: Unscheduled Tasks
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Unscheduled Tasks (${uiState.unscheduledItems.size})",
                                style = DayflowTypography.titleMedium,
                                color = colors.textPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Eligible tasks that could not fit into today's window. Never discarded.",
                            style = DayflowTypography.labelSmall,
                            color = colors.textMuted
                        )
                    }
                }

                if (uiState.unscheduledItems.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = shapes.secondaryCard,
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
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "All eligible tasks fit into today's schedule!",
                                    style = DayflowTypography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }
                } else {
                    itemsIndexed(uiState.unscheduledItems, key = { _, unscheduled -> unscheduled.task.id }) { _, unscheduled ->
                        UnscheduledTaskCard(
                            unscheduled = unscheduled,
                            onSchedule = { onScheduleTask(unscheduled.task) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // Edit Item Time Dialog
    editingItem?.let { item ->
        EditTimeDialog(
            item = item,
            onDismiss = { editingItem = null },
            onSave = { start, end ->
                onUpdateItemTime(item.id, start, end)
                editingItem = null
            }
        )
    }
}

@Composable
fun PlanStatusHeader(
    status: PlanStatus,
    totalScheduledMinutes: Int,
    scheduledItemCount: Int
) {
    val colors = DayflowTheme.colors
    val shapes = DayflowTheme.shapes
    val spacing = DayflowTheme.spacing

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shapes.mainCard,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, if (status == PlanStatus.APPROVED) colors.primary.copy(alpha = 0.5f) else colors.border)
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
                        .background(
                            if (status == PlanStatus.APPROVED) colors.primary.copy(alpha = 0.15f)
                            else colors.surfaceElevated
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (status == PlanStatus.APPROVED) "APPROVED PLAN" else "DRAFT PLAN",
                        style = DayflowTypography.labelSmall,
                        color = if (status == PlanStatus.APPROVED) colors.primary else colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "$scheduledItemCount items",
                    style = DayflowTypography.labelMedium,
                    color = colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (status == PlanStatus.APPROVED) {
                    "This is your active execution plan for today. Check off items as you complete them."
                } else {
                    "Deterministic draft generated. Review order and time slots, then approve to commit your schedule."
                },
                style = DayflowTypography.bodySmall,
                color = colors.textSecondary
            )
        }
    }
}

@Composable
fun ConflictBanner(conflicts: List<PlanConflict>) {
    val colors = DayflowTheme.colors
    val shapes = DayflowTheme.shapes
    val spacing = DayflowTheme.spacing

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shapes.mainCard,
        colors = CardDefaults.cardColors(
            containerColor = colors.error.copy(alpha = 0.08f)
        ),
        border = BorderStroke(1.dp, colors.error.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.cardPadding)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Conflicts detected",
                    tint = colors.error,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Schedule Warnings (${conflicts.size})",
                    style = DayflowTypography.titleSmall,
                    color = colors.error,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            conflicts.forEach { conflict ->
                Text(
                    text = "• ${conflict.message}",
                    style = DayflowTypography.bodySmall,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun PlanItemCard(
    item: DailyPlanItem,
    index: Int,
    totalCount: Int,
    timeFormatter: DateTimeFormatter,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEditTime: () -> Unit,
    onRemove: () -> Unit,
    onToggleCompletion: () -> Unit
) {
    val colors = DayflowTheme.colors
    val shapes = DayflowTheme.shapes
    val spacing = DayflowTheme.spacing

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shapes.secondaryCard,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, if (item.isCompleted) colors.primary.copy(alpha = 0.3f) else colors.border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Completion Checkbox
                IconButton(
                    onClick = onToggleCompletion,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (item.isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircleOutline,
                        contentDescription = "Toggle completion",
                        tint = if (item.isCompleted) colors.primary else colors.textMuted
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Time Badge
                Box(
                    modifier = Modifier
                        .clip(shapes.chip)
                        .background(colors.surfaceElevated)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${item.startTime.format(timeFormatter)} - ${item.endTime.format(timeFormatter)}",
                        style = DayflowTypography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = colors.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Source Type tag
                Text(
                    text = when (item.sourceType) {
                        PlanItemSourceType.TASK -> "Task"
                        PlanItemSourceType.HABIT -> "Habit"
                        PlanItemSourceType.CUSTOM -> "Event"
                    },
                    style = DayflowTypography.labelSmall,
                    color = colors.textMuted
                )

                Spacer(modifier = Modifier.weight(1f))

                // Reorder controls
                IconButton(
                    onClick = onMoveUp,
                    enabled = index > 0,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Move item up",
                        tint = if (index > 0) colors.textPrimary else colors.textMuted.copy(alpha = 0.3f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onMoveDown,
                    enabled = index < totalCount - 1,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = "Move item down",
                        tint = if (index < totalCount - 1) colors.textPrimary else colors.textMuted.copy(alpha = 0.3f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title and action buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.title,
                    style = DayflowTypography.titleSmall,
                    color = if (item.isCompleted) colors.textMuted else colors.textPrimary,
                    textDecoration = if (item.isCompleted) TextDecoration.LineThrough else null,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onEditTime,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit time",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove to unscheduled",
                            tint = colors.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UnscheduledTaskCard(
    unscheduled: UnscheduledItem,
    onSchedule: () -> Unit
) {
    val colors = DayflowTheme.colors
    val shapes = DayflowTheme.shapes
    val spacing = DayflowTheme.spacing

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shapes.secondaryCard,
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = unscheduled.task.title,
                        style = DayflowTypography.titleSmall,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    PriorityBadge(priority = unscheduled.task.priority)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = unscheduled.reason,
                    style = DayflowTypography.labelSmall,
                    color = colors.textMuted
                )

                unscheduled.reschedulingSuggestion?.let { suggestion ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = suggestion,
                        style = DayflowTypography.labelSmall,
                        color = colors.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedButton(
                onClick = onSchedule,
                shape = shapes.button,
                border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = colors.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Schedule",
                    style = DayflowTypography.labelSmall,
                    color = colors.primary
                )
            }
        }
    }
}

@Composable
fun EditTimeDialog(
    item: DailyPlanItem,
    onDismiss: () -> Unit,
    onSave: (LocalTime, LocalTime) -> Unit
) {
    val colors = DayflowTheme.colors
    val shapes = DayflowTheme.shapes

    var startHour by remember { mutableStateOf(item.startTime.hour) }
    var startMinute by remember { mutableStateOf(item.startTime.minute) }
    var durationMinutes by remember {
        mutableStateOf(java.time.Duration.between(item.startTime, item.endTime).toMinutes().toInt().coerceAtLeast(15))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Adjust Slot Time",
                style = DayflowTypography.titleMedium,
                color = colors.textPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = item.title,
                    style = DayflowTypography.bodyMedium,
                    color = colors.textSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Start Time:",
                        style = DayflowTypography.labelLarge,
                        color = colors.textPrimary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = { startHour = (startHour + 23) % 24 },
                            shape = shapes.button
                        ) { Text("-1h") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = String.format(Locale.getDefault(), "%02d:%02d", startHour, startMinute),
                            style = DayflowTypography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                            color = colors.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = { startHour = (startHour + 1) % 24 },
                            shape = shapes.button
                        ) { Text("+1h") }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Duration:",
                        style = DayflowTypography.labelLarge,
                        color = colors.textPrimary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = { durationMinutes = (durationMinutes - 15).coerceAtLeast(15) },
                            shape = shapes.button
                        ) { Text("-15m") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${durationMinutes}m",
                            style = DayflowTypography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                            color = colors.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = { durationMinutes = (durationMinutes + 15).coerceAtMost(360) },
                            shape = shapes.button
                        ) { Text("+15m") }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newStart = LocalTime.of(startHour, startMinute)
                    val newEnd = newStart.plusMinutes(durationMinutes.toLong())
                    onSave(newStart, newEnd)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.background
                )
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textSecondary)
            }
        },
        containerColor = colors.surface,
        shape = shapes.mainCard
    )
}
