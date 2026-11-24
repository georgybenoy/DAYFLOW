package com.dayflow.app.feature.tasks.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dayflow.app.core.designsystem.DayflowTheme
import com.dayflow.app.core.designsystem.DayflowTypography
import com.dayflow.app.core.designsystem.components.PriorityBadge
import com.dayflow.app.core.time.TaskFormatter
import com.dayflow.app.domain.model.Category
import com.dayflow.app.domain.model.Task
import com.dayflow.app.domain.model.TaskStatus
import java.time.LocalDateTime

@Composable
fun TaskCard(
    task: Task,
    isOverdue: Boolean,
    category: Category?,
    onTaskClick: () -> Unit,
    onToggleComplete: () -> Unit,
    onEditClick: () -> Unit,
    onRescheduleClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val colors = DayflowTheme.colors
    val shapes = DayflowTheme.shapes
    val spacing = DayflowTheme.spacing

    val isCompleted = task.status == TaskStatus.COMPLETED
    val deadlineText = TaskFormatter.formatDeadline(task, LocalDateTime.now(), isOverdue)
    val plannedText = TaskFormatter.formatPlannedWindow(task)
    val durationText = TaskFormatter.formatDuration(task.estimatedDurationMinutes)

    val totalSubtasks = task.subtasks.size
    val completedSubtasks = task.subtasks.count { it.isCompleted }

    val categoryColor = category?.colorHex?.let { hex ->
        runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrNull()
    } ?: colors.primary

    Card(
        shape = shapes.mainCard,
        colors = CardDefaults.cardColors(
            containerColor = colors.surface,
            contentColor = colors.textPrimary
        ),
        border = BorderStroke(
            1.dp,
            if (isOverdue && !isCompleted) colors.error.copy(alpha = 0.5f) else colors.border
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onTaskClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.cardPadding)
        ) {
            // Header: Status Checkbox + Title + Overflow Menu
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onToggleComplete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        if (isCompleted) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Mark ${task.title} as incomplete",
                                tint = colors.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircleOutline,
                                contentDescription = "Mark ${task.title} as completed",
                                tint = if (isOverdue) colors.error else colors.textMuted,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = task.title,
                        style = DayflowTypography.titleSmall,
                        color = if (isCompleted) colors.textMuted else colors.textPrimary,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = "Task options for ${task.title}",
                            tint = colors.textMuted
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(colors.surfaceElevated)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Task", color = colors.textPrimary) },
                            leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null, tint = colors.textSecondary) },
                            onClick = {
                                menuExpanded = false
                                onEditClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Reschedule", color = colors.textPrimary) },
                            leadingIcon = { Icon(Icons.Outlined.Event, contentDescription = null, tint = colors.textSecondary) },
                            onClick = {
                                menuExpanded = false
                                onRescheduleClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = colors.error) },
                            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = colors.error) },
                            onClick = {
                                menuExpanded = false
                                onDeleteClick()
                            }
                        )
                    }
                }
            }

            // Description (if present)
            if (!task.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = task.description,
                    style = DayflowTypography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 36.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Badges row: Priority, Category, Duration
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 36.dp)
            ) {
                PriorityBadge(priority = task.priority)

                category?.let { cat ->
                    Box(
                        modifier = Modifier
                            .background(categoryColor.copy(alpha = 0.15f), shape = shapes.chip)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
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
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = dur,
                            style = DayflowTypography.labelSmall,
                            color = colors.textMuted
                        )
                    }
                }
            }

            // Subtasks Progress (if any subtasks exist)
            if (totalSubtasks > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 36.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = TaskFormatter.formatSubtaskProgress(completedSubtasks, totalSubtasks),
                            style = DayflowTypography.labelSmall,
                            color = colors.textMuted
                        )
                        Text(
                            text = "${(completedSubtasks * 100) / totalSubtasks}%",
                            style = DayflowTypography.labelSmall,
                            color = colors.textSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { completedSubtasks.toFloat() / totalSubtasks.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = colors.primary,
                        trackColor = colors.surfaceElevated
                    )
                }
            }

            // Footer: Deadline / Planned Window
            if (deadlineText != null || plannedText != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 36.dp)
                ) {
                    deadlineText?.let { text ->
                        Text(
                            text = text,
                            style = DayflowTypography.labelSmall,
                            color = if (isOverdue && !isCompleted) colors.error else colors.primary
                        )
                    }

                    plannedText?.let { text ->
                        Text(
                            text = text,
                            style = DayflowTypography.labelSmall,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}
