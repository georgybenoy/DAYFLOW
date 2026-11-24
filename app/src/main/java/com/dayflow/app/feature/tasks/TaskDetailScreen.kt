package com.dayflow.app.feature.tasks

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dayflow.app.core.designsystem.DayflowTheme
import com.dayflow.app.core.designsystem.DayflowTypography
import com.dayflow.app.core.designsystem.components.EmptyState
import com.dayflow.app.core.designsystem.components.PriorityBadge
import com.dayflow.app.core.time.TaskFormatter
import com.dayflow.app.domain.model.TaskStatus
import com.dayflow.app.feature.tasks.components.ChangeDeadlineDialog
import com.dayflow.app.feature.tasks.components.RescheduleDialog
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    taskId: String,
    onNavigateToEdit: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TaskDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            onNavigateBack()
        }
    }

    val colors = DayflowTheme.colors
    val spacing = DayflowTheme.spacing
    val shapes = DayflowTheme.shapes
    val scrollState = rememberScrollState()

    var showRescheduleDialog by remember { mutableStateOf(false) }
    var showChangeDeadlineDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var newSubtaskText by remember { mutableStateOf("") }

    val task = uiState.task

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Task Details",
                    style = DayflowTypography.titleMedium,
                    color = colors.textPrimary
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Navigate back",
                        tint = colors.textPrimary
                    )
                }
            },
            actions = {
                if (task != null) {
                    IconButton(onClick = { onNavigateToEdit(task.id) }) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit task",
                            tint = colors.textPrimary
                        )
                    }

                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Outlined.MoreVert,
                                contentDescription = "More actions",
                                tint = colors.textPrimary
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier.background(colors.surfaceElevated)
                        ) {
                            if (task.status == TaskStatus.PENDING) {
                                DropdownMenuItem(
                                    text = { Text("Cancel Task", color = colors.warning) },
                                    leadingIcon = { Icon(Icons.Outlined.Cancel, contentDescription = null, tint = colors.warning) },
                                    onClick = {
                                        menuExpanded = false
                                        viewModel.cancelTask()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Delete Task", color = colors.error) },
                                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = colors.error) },
                                onClick = {
                                    menuExpanded = false
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
        )

        if (uiState.isLoading && task == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.primary)
            }
        } else if (task == null) {
            EmptyState(
                title = "Task Not Found",
                message = "The requested task could not be loaded or has been deleted.",
                icon = Icons.Outlined.CheckCircleOutline,
                actionLabel = "Back to Tasks",
                onActionClick = onNavigateBack,
                modifier = Modifier.padding(top = 48.dp)
            )
        } else {
            val isCompleted = task.status == TaskStatus.COMPLETED
            val deadlineText = TaskFormatter.formatDeadline(task, LocalDateTime.now(), uiState.isOverdue)
            val plannedText = TaskFormatter.formatPlannedWindow(task)
            val durationText = TaskFormatter.formatDuration(task.estimatedDurationMinutes)
            val totalSubtasks = task.subtasks.size
            val completedSubtasks = task.subtasks.count { it.isCompleted }

            val categoryColor = uiState.category?.colorHex?.let { hex ->
                runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrNull()
            } ?: colors.primary

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = spacing.screenHorizontal)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Status Banner
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            when {
                                isCompleted -> colors.primary.copy(alpha = 0.15f)
                                uiState.isOverdue -> colors.error.copy(alpha = 0.15f)
                                task.status == TaskStatus.CANCELLED -> colors.surfaceElevated
                                else -> colors.surface
                            },
                            shapes.mainCard
                        )
                        .padding(spacing.cardPadding)
                ) {
                    Column {
                        Text(
                            text = when {
                                isCompleted -> "STATUS: COMPLETED"
                                uiState.isOverdue -> "STATUS: OVERDUE"
                                task.status == TaskStatus.CANCELLED -> "STATUS: CANCELLED"
                                else -> "STATUS: PENDING"
                            },
                            style = DayflowTypography.labelSmall,
                            color = when {
                                isCompleted -> colors.primary
                                uiState.isOverdue -> colors.error
                                else -> colors.textSecondary
                            }
                        )
                        if (isCompleted && task.completedAt != null) {
                            Text(
                                text = "Completed on ${task.completedAt}",
                                style = DayflowTypography.labelSmall,
                                color = colors.textMuted
                            )
                        }
                    }

                    Button(
                        onClick = viewModel::toggleTaskComplete,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCompleted) colors.surfaceElevated else colors.primary,
                            contentColor = if (isCompleted) colors.textPrimary else colors.surface
                        ),
                        shape = shapes.button
                    ) {
                        Text(if (isCompleted) "Reopen Task" else "Mark Complete")
                    }
                }

                // Title & Details Card
                Card(
                    shape = shapes.mainCard,
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(spacing.cardPadding),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = task.title,
                            style = DayflowTypography.headlineSmall,
                            color = if (isCompleted) colors.textMuted else colors.textPrimary,
                            textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        )

                        if (!task.description.isNullOrBlank()) {
                            Text(
                                text = task.description,
                                style = DayflowTypography.bodyMedium,
                                color = colors.textSecondary
                            )
                        }

                        // Badges Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PriorityBadge(priority = task.priority)

                            uiState.category?.let { cat ->
                                Box(
                                    modifier = Modifier
                                        .background(categoryColor.copy(alpha = 0.15f), shapes.chip)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = cat.name,
                                        style = DayflowTypography.labelSmall,
                                        color = categoryColor
                                    )
                                }
                            }

                            durationText?.let { dur ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Schedule,
                                        contentDescription = null,
                                        tint = colors.textMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = dur,
                                        style = DayflowTypography.labelSmall,
                                        color = colors.textMuted
                                    )
                                }
                            }
                        }
                    }
                }

                // Schedule & Deadline Controls Card
                Card(
                    shape = shapes.mainCard,
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(spacing.cardPadding),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Execution & Deadlines",
                            style = DayflowTypography.titleSmall,
                            color = colors.textPrimary
                        )

                        // Commitment Deadline Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Commitment Deadline",
                                    style = DayflowTypography.labelSmall,
                                    color = colors.textMuted
                                )
                                Text(
                                    text = deadlineText ?: "No deadline set",
                                    style = DayflowTypography.bodyMedium,
                                    color = if (uiState.isOverdue && !isCompleted) colors.error else colors.textPrimary
                                )
                                if (uiState.deadlineReminders.isNotEmpty()) {
                                    val reminderLabels = uiState.deadlineReminders.joinToString { it.label }
                                    Text(
                                        text = "Reminders: $reminderLabels",
                                        style = DayflowTypography.labelSmall,
                                        color = colors.primary
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = { showChangeDeadlineDialog = true },
                                shape = shapes.chip,
                                border = BorderStroke(1.dp, colors.border)
                            ) {
                                Text("Edit Deadline", style = DayflowTypography.labelSmall, color = colors.textPrimary)
                            }
                        }

                        // Planned Window Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Planned Execution",
                                    style = DayflowTypography.labelSmall,
                                    color = colors.textMuted
                                )
                                Text(
                                    text = plannedText ?: "Not scheduled on calendar",
                                    style = DayflowTypography.bodyMedium,
                                    color = colors.textPrimary
                                )
                            }
                            OutlinedButton(
                                onClick = { showRescheduleDialog = true },
                                shape = shapes.chip,
                                border = BorderStroke(1.dp, colors.border)
                            ) {
                                Text("Reschedule", style = DayflowTypography.labelSmall, color = colors.textPrimary)
                            }
                        }

                        // Carry-forward status
                        Text(
                            text = if (task.shouldCarryForward) "• Automatically eligible for daily carry-forward"
                            else "• Excluded from daily carry-forward suggestions",
                            style = DayflowTypography.labelSmall,
                            color = colors.textSecondary
                        )
                    }
                }

                // Subtask Checklist Card
                Card(
                    shape = shapes.mainCard,
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(spacing.cardPadding),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Subtasks ($completedSubtasks/$totalSubtasks)",
                                style = DayflowTypography.titleSmall,
                                color = colors.textPrimary
                            )
                            if (totalSubtasks > 0) {
                                Text(
                                    text = "${(completedSubtasks * 100) / totalSubtasks}%",
                                    style = DayflowTypography.labelSmall,
                                    color = colors.primary
                                )
                            }
                        }

                        if (totalSubtasks > 0) {
                            LinearProgressIndicator(
                                progress = { completedSubtasks.toFloat() / totalSubtasks.toFloat() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp),
                                color = colors.primary,
                                trackColor = colors.surfaceElevated
                            )
                        }

                        // Interactive checklist
                        task.subtasks.forEach { subtask ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Checkbox(
                                        checked = subtask.isCompleted,
                                        onCheckedChange = { isChecked ->
                                            viewModel.toggleSubtask(subtask.id, isChecked)
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = colors.primary,
                                            checkmarkColor = colors.surface,
                                            uncheckedColor = colors.textMuted
                                        )
                                    )
                                    Text(
                                        text = subtask.title,
                                        style = DayflowTypography.bodyMedium,
                                        color = if (subtask.isCompleted) colors.textMuted else colors.textPrimary,
                                        textDecoration = if (subtask.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.deleteSubtask(subtask.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = "Delete subtask",
                                        tint = colors.textMuted
                                    )
                                }
                            }
                        }

                        // Inline add subtask field
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = newSubtaskText,
                                onValueChange = { newSubtaskText = it },
                                placeholder = { Text("Add subtask...") },
                                modifier = Modifier.weight(1f),
                                shape = shapes.input,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = colors.primary,
                                    unfocusedBorderColor = colors.border,
                                    focusedTextColor = colors.textPrimary,
                                    unfocusedTextColor = colors.textPrimary
                                )
                            )
                            Button(
                                onClick = {
                                    viewModel.addSubtask(newSubtaskText)
                                    newSubtaskText = ""
                                },
                                enabled = newSubtaskText.isNotBlank(),
                                shape = shapes.button,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.primary,
                                    contentColor = colors.surface
                                )
                            ) {
                                Text("Add")
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showRescheduleDialog && task != null) {
        RescheduleDialog(
            initialPlannedDate = task.plannedDate,
            initialPlannedStart = task.plannedStart,
            initialPlannedEnd = task.plannedEnd,
            onDismiss = { showRescheduleDialog = false },
            onConfirm = { date, start, end ->
                viewModel.reschedulePlannedTime(date, start, end)
                showRescheduleDialog = false
            }
        )
    }

    if (showChangeDeadlineDialog && task != null) {
        ChangeDeadlineDialog(
            initialDeadlineDate = task.deadlineDate,
            initialDeadlineTime = task.deadlineTime,
            initialReminders = uiState.deadlineReminders,
            onDismiss = { showChangeDeadlineDialog = false },
            onConfirm = { date, time, reminders ->
                viewModel.changeDeadline(date, time, reminders)
                showChangeDeadlineDialog = false
            }
        )
    }

    if (showDeleteDialog && task != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text("Delete Task?", style = DayflowTypography.titleMedium, color = colors.textPrimary)
            },
            text = {
                Text(
                    text = "Are you sure you want to delete \"${task.title}\"? This action cannot be undone.",
                    style = DayflowTypography.bodyMedium,
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTask()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.error,
                        contentColor = colors.surface
                    ),
                    shape = shapes.button
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface,
            shape = shapes.mainCard
        )
    }
}
