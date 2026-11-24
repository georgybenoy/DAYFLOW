package com.dayflow.app.feature.reminders.components

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
import androidx.compose.material.icons.outlined.AccessAlarm
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dayflow.app.core.designsystem.DayflowTheme
import com.dayflow.app.core.designsystem.DayflowTypography
import com.dayflow.app.core.designsystem.components.PriorityBadge
import com.dayflow.app.domain.model.ReminderPrecision
import com.dayflow.app.feature.reminders.ReminderListItem

@Composable
fun ReminderCard(
    item: ReminderListItem,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onTogglePauseClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DayflowTheme.colors
    val spacing = DayflowTheme.spacing
    val shapes = DayflowTheme.shapes
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = shapes.mainCard,
        colors = CardDefaults.cardColors(
            containerColor = colors.surface,
            contentColor = colors.textPrimary
        ),
        border = BorderStroke(1.dp, colors.border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.cardPadding)
        ) {
            // Header Row: Title + Overflow menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.reminder.title,
                        style = DayflowTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (item.reminder.isPaused) colors.textSecondary else colors.textPrimary
                    )

                    if (!item.reminder.description.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.reminder.description,
                            style = DayflowTypography.bodySmall,
                            color = colors.textSecondary,
                            maxLines = 2
                        )
                    }
                }

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = "Options for ${item.reminder.title}",
                            tint = colors.textSecondary
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(colors.surfaceVariant)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit", style = DayflowTypography.bodyMedium, color = colors.textPrimary) },
                            leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null, tint = colors.textPrimary) },
                            onClick = {
                                menuExpanded = false
                                onEditClick()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (item.reminder.isPaused) "Resume" else "Pause",
                                    style = DayflowTypography.bodyMedium,
                                    color = colors.textPrimary
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    if (item.reminder.isPaused) Icons.Outlined.PlayCircle else Icons.Outlined.PauseCircle,
                                    contentDescription = null,
                                    tint = colors.textPrimary
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onTogglePauseClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", style = DayflowTypography.bodyMedium, color = colors.error) },
                            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = colors.error) },
                            onClick = {
                                menuExpanded = false
                                onDeleteClick()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing.sm))

            // Recurrence summary row
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = colors.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = item.recurrenceSummary,
                    style = DayflowTypography.bodySmall,
                    color = colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Next delivery time row
            Text(
                text = item.nextDeliverySummary,
                style = DayflowTypography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = if (item.reminder.isPaused) colors.textMuted else colors.primary
            )

            Spacer(modifier = Modifier.height(spacing.sm))

            // Badges row: Priority + Precision + Category + Paused indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PriorityBadge(priority = item.reminder.priority)

                // Precision indicator
                val (precisionText, precisionColor) = if (item.reminder.precision == ReminderPrecision.EXACT) {
                    "Exact" to colors.primary
                } else {
                    "Approx." to colors.warning
                }

                Box(
                    modifier = Modifier
                        .background(precisionColor.copy(alpha = 0.12f), shapes.chip)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = precisionText,
                        style = DayflowTypography.labelSmall,
                        color = precisionColor
                    )
                }

                // Category pill if present
                if (item.category != null) {
                    Box(
                        modifier = Modifier
                            .background(colors.surfaceElevated, shapes.chip)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.category.name,
                            style = DayflowTypography.labelSmall,
                            color = colors.textSecondary
                        )
                    }
                }

                if (item.reminder.isPaused) {
                    Box(
                        modifier = Modifier
                            .background(colors.warning.copy(alpha = 0.15f), shapes.chip)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Paused",
                            style = DayflowTypography.labelSmall,
                            color = colors.warning
                        )
                    }
                }
            }
        }
    }
}
