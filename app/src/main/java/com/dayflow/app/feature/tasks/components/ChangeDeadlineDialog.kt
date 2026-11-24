package com.dayflow.app.feature.tasks.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dayflow.app.core.designsystem.DayflowTheme
import com.dayflow.app.core.designsystem.DayflowTypography
import com.dayflow.app.domain.model.DeadlineReminderLeadTime
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeDeadlineDialog(
    initialDeadlineDate: LocalDate?,
    initialDeadlineTime: LocalTime?,
    initialReminders: Set<DeadlineReminderLeadTime>,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate?, LocalTime?, Set<DeadlineReminderLeadTime>) -> Unit
) {
    var deadlineDate by remember { mutableStateOf(initialDeadlineDate) }
    var deadlineTime by remember { mutableStateOf(initialDeadlineTime) }
    var selectedReminders by remember { mutableStateOf(initialReminders) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val colors = DayflowTheme.colors
    val shapes = DayflowTheme.shapes
    val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Change Task Deadline",
                style = DayflowTypography.titleMedium,
                color = colors.textPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Update the formal commitment deadline. Any scheduled deadline reminders will be recalculated and updated on the Critical Deadlines channel.",
                    style = DayflowTypography.bodySmall,
                    color = colors.textSecondary
                )

                // Date Picker Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Deadline Date",
                        style = DayflowTypography.labelSmall,
                        color = colors.textMuted
                    )
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        shape = shapes.chip,
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        Text(
                            text = deadlineDate?.format(DateTimeFormatter.ofPattern("MMM d, yyyy")) ?: "Set Date",
                            style = DayflowTypography.labelSmall,
                            color = colors.textPrimary
                        )
                    }
                }

                // Time Picker Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Deadline Time",
                        style = DayflowTypography.labelSmall,
                        color = colors.textMuted
                    )
                    OutlinedButton(
                        onClick = { showTimePicker = true },
                        shape = shapes.chip,
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        Text(
                            text = deadlineTime?.format(timeFormatter) ?: "Set Time",
                            style = DayflowTypography.labelSmall,
                            color = colors.textPrimary
                        )
                    }
                }

                // Reminders
                if (deadlineDate != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Deadline Reminders",
                        style = DayflowTypography.labelSmall,
                        color = colors.textMuted
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        DeadlineReminderLeadTime.entries.forEach { lead ->
                            val isSelected = selectedReminders.contains(lead)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedReminders = if (isSelected) {
                                        selectedReminders - lead
                                    } else {
                                        selectedReminders + lead
                                    }
                                },
                                label = { Text(lead.label, style = DayflowTypography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.primary,
                                    selectedLabelColor = colors.surface
                                )
                            )
                        }
                    }
                }

                // Clear Deadline Button
                TextButton(
                    onClick = {
                        deadlineDate = null
                        deadlineTime = null
                        selectedReminders = emptySet()
                    }
                ) {
                    Text("Clear deadline", color = colors.error, style = DayflowTypography.labelSmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(deadlineDate, deadlineTime, selectedReminders) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.surface
                ),
                shape = shapes.button
            ) {
                Text("Update Deadline")
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

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = (deadlineDate ?: LocalDate.now())
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        deadlineDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) {
                    Text("Select", color = colors.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = deadlineTime?.hour ?: 18,
            initialMinute = deadlineTime?.minute ?: 0
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    deadlineTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) {
                    Text("OK", color = colors.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            text = { TimePicker(state = timePickerState) },
            containerColor = colors.surface
        )
    }
}
