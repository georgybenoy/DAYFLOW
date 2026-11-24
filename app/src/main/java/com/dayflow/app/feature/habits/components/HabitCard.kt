package com.dayflow.app.feature.habits.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dayflow.app.core.designsystem.DayflowTheme
import com.dayflow.app.core.designsystem.DayflowTypography
import com.dayflow.app.domain.model.Category
import com.dayflow.app.domain.model.Habit
import com.dayflow.app.domain.model.HabitTrackingType
import com.dayflow.app.domain.recurrence.HabitDayStatus
import com.dayflow.app.domain.recurrence.HabitProgress
import com.dayflow.app.domain.recurrence.StreakResult

@Composable
fun HabitCard(
    habit: Habit,
    category: Category?,
    progress: HabitProgress,
    streak: StreakResult,
    weeklyStatus: List<HabitDayStatus>,
    isScheduledToday: Boolean,
    onCardClick: () -> Unit,
    onToggleComplete: () -> Unit,
    onOpenQuickLog: () -> Unit,
    onEdit: () -> Unit,
    onTogglePause: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(DayflowTheme.shapes.mainCard)
            .clickable(onClick = onCardClick),
        color = DayflowTheme.colors.surface,
        shape = DayflowTheme.shapes.mainCard
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Category Badge, Tracking Type, Streak, Overflow Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (category != null) {
                        CategoryPastelChip(category = category)
                    }

                    // Tracking type tag
                    Text(
                        text = habit.trackingType.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = DayflowTypography.labelSmall,
                        color = DayflowTheme.colors.textMuted,
                        modifier = Modifier
                            .background(
                                color = DayflowTheme.colors.surfaceElevated,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )

                    // Streak Badge
                    StreakBadge(streak = streak.currentStreak)
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = "Habit options",
                            tint = DayflowTheme.colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(DayflowTheme.colors.surfaceElevated)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Habit", color = DayflowTheme.colors.textPrimary) },
                            leadingIcon = {
                                Icon(Icons.Outlined.Edit, contentDescription = null, tint = DayflowTheme.colors.textSecondary)
                            },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (habit.isEnabled) "Pause Habit" else "Resume Habit",
                                    color = DayflowTheme.colors.textPrimary
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    if (habit.isEnabled) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                                    contentDescription = null,
                                    tint = DayflowTheme.colors.textSecondary
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onTogglePause()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = DayflowTheme.colors.error) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            // Main Row: Habit Title, Description & Action Button / Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = habit.name,
                        style = DayflowTypography.titleMedium,
                        color = if (habit.isEnabled) DayflowTheme.colors.textPrimary else DayflowTheme.colors.textMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (!habit.description.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = habit.description,
                            style = DayflowTypography.bodySmall,
                            color = DayflowTheme.colors.textSecondary,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Action Target: Checkbox for Binary, or Log Button for Quantity/Duration/Repetition
                if (habit.trackingType == HabitTrackingType.BINARY) {
                    BinaryCompletionButton(
                        isCompleted = progress.isCompleted,
                        onClick = onToggleComplete
                    )
                } else {
                    QuickLogButton(
                        isCompleted = progress.isCompleted,
                        onClick = onOpenQuickLog
                    )
                }
            }

            // Progress Bar & Value
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                LinearProgressIndicator(
                    progress = { progress.progressRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (progress.isCompleted) DayflowTheme.colors.primary else DayflowTheme.colors.primary.copy(alpha = 0.6f),
                    trackColor = DayflowTheme.colors.surfaceElevated
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = progress.formattedProgress,
                        style = DayflowTypography.labelSmall,
                        color = DayflowTheme.colors.textSecondary
                    )

                    if (!isScheduledToday) {
                        Text(
                            text = "Not scheduled today",
                            style = DayflowTypography.labelSmall,
                            color = DayflowTheme.colors.textMuted
                        )
                    }
                }
            }

            // Weekly Calendar Strip
            if (weeklyStatus.isNotEmpty()) {
                WeeklyDotsStrip(weeklyStatus = weeklyStatus)
            }
        }
    }
}

@Composable
fun StreakBadge(
    streak: Int,
    modifier: Modifier = Modifier
) {
    val badgeBg = if (streak > 0) {
        DayflowTheme.colors.primary.copy(alpha = 0.15f)
    } else {
        DayflowTheme.colors.surfaceElevated
    }
    val badgeColor = if (streak > 0) {
        DayflowTheme.colors.primary
    } else {
        DayflowTheme.colors.textMuted
    }

    Row(
        modifier = modifier
            .background(badgeBg, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.LocalFireDepartment,
            contentDescription = null,
            tint = badgeColor,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = "${streak}d",
            style = DayflowTypography.labelSmall,
            color = badgeColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CategoryPastelChip(
    category: Category,
    modifier: Modifier = Modifier
) {
    val categoryColor = try {
        Color(android.graphics.Color.parseColor(category.colorHex))
    } catch (e: Exception) {
        DayflowTheme.colors.secondary
    }

    Row(
        modifier = modifier
            .background(
                color = categoryColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 7.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(categoryColor, CircleShape)
        )
        Text(
            text = category.name,
            style = DayflowTypography.labelSmall,
            color = DayflowTheme.colors.textPrimary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun BinaryCompletionButton(
    isCompleted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (isCompleted) DayflowTheme.colors.primary else Color.Transparent,
        label = "BinaryButtonBg"
    )
    val borderColor = if (isCompleted) DayflowTheme.colors.primary else DayflowTheme.colors.border

    Box(
        modifier = modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isCompleted) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Completed",
                tint = DayflowTheme.colors.background,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun QuickLogButton(
    isCompleted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isCompleted) DayflowTheme.colors.primary.copy(alpha = 0.15f) else DayflowTheme.colors.surfaceElevated
    val contentColor = if (isCompleted) DayflowTheme.colors.primary else DayflowTheme.colors.textPrimary

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, if (isCompleted) DayflowTheme.colors.primary else DayflowTheme.colors.border, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = if (isCompleted) Icons.Default.Check else Icons.Outlined.Add,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = if (isCompleted) "Done" else "Log",
            style = DayflowTypography.labelMedium,
            color = contentColor,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun WeeklyDotsStrip(
    weeklyStatus: List<HabitDayStatus>,
    modifier: Modifier = Modifier
) {
    val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        weeklyStatus.take(7).forEachIndexed { index, status ->
            val label = dayLabels.getOrElse(index) { "" }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = label,
                    style = DayflowTypography.labelSmall.copy(fontSize = 10.sp),
                    color = DayflowTheme.colors.textMuted
                )

                val dotColor = when {
                    status.isCompleted -> DayflowTheme.colors.primary
                    status.progressRatio > 0f -> DayflowTheme.colors.primary.copy(alpha = 0.5f)
                    status.isScheduled -> DayflowTheme.colors.surfaceElevated
                    else -> Color.Transparent
                }

                val borderModifier = if (status.isScheduled && !status.isCompleted) {
                    Modifier.border(1.dp, DayflowTheme.colors.border, CircleShape)
                } else if (!status.isScheduled) {
                    Modifier.border(0.5.dp, DayflowTheme.colors.border.copy(alpha = 0.3f), CircleShape)
                } else {
                    Modifier
                }

                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                        .then(borderModifier),
                    contentAlignment = Alignment.Center
                ) {
                    if (status.isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = DayflowTheme.colors.background,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }
        }
    }
}
