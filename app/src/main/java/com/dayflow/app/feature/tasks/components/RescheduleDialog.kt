package com.dayflow.app.feature.tasks.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RescheduleDialog(
    initialPlannedDate: LocalDate?,
    initialPlannedStart: LocalTime?,
    initialPlannedEnd: LocalTime?,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate?, LocalTime?, LocalTime?) -> Unit
) {
    var plannedDate by remember { mutableStateOf(initialPlannedDate ?: LocalDate.now()) }
    var plannedStart by remember { mutableStateOf(initialPlannedStart) }
    var plannedEnd by remember { mutableStateOf(initialPlannedEnd) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val colors = DayflowTheme.colors
    val shapes = DayflowTheme.shapes
    val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Reschedule Planned Execution",
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
                    text = "Plan when you will execute this task. The task deadline is preserved and will NOT be modified.",
                    style = DayflowTypography.bodySmall,
                    color = colors.textSecondary
                )

                // Quick presets: Today, Tomorrow
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val today = LocalDate.now()
                    val tomorrow = today.plusDays(1)

                    FilterChip(
                        selected = plannedDate == today,
                        onClick = { plannedDate = today },
                        label = { Text("Today") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.primary,
                            selectedLabelColor = colors.surface
                        )
                    )

                    FilterChip(
                        selected = plannedDate == tomorrow,
                        onClick = { plannedDate = tomorrow },
                        label = { Text("Tomorrow") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.primary,
                            selectedLabelColor = colors.surface
                        )
                    )

                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        shape = shapes.chip,
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        Text(
                            text = plannedDate.format(DateTimeFormatter.ofPattern("MMM d")),
                            style = DayflowTypography.labelSmall,
                            color = colors.textPrimary
                        )
                    }
                }

                // Time Pickers
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "Start Time",
                            style = DayflowTypography.labelSmall,
                            color = colors.textMuted
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = { showStartTimePicker = true },
                            shape = shapes.chip,
                            border = BorderStroke(1.dp, colors.border)
                        ) {
                            Text(
                                text = plannedStart?.format(timeFormatter) ?: "Set Start",
                                style = DayflowTypography.labelSmall,
                                color = colors.textPrimary
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "End Time",
                            style = DayflowTypography.labelSmall,
                            color = colors.textMuted
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = { showEndTimePicker = true },
                            shape = shapes.chip,
                            border = BorderStroke(1.dp, colors.border)
                        ) {
                            Text(
                                text = plannedEnd?.format(timeFormatter) ?: "Set End",
                                style = DayflowTypography.labelSmall,
                                color = colors.textPrimary
                            )
                        }
                    }
                }

                // Clear plan button
                TextButton(
                    onClick = {
                        onConfirm(null, null, null)
                    }
                ) {
                    Text("Clear planned execution time", color = colors.error, style = DayflowTypography.labelSmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(plannedDate, plannedStart, plannedEnd) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.surface
                ),
                shape = shapes.button
            ) {
                Text("Save Plan")
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
            initialSelectedDateMillis = plannedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        plannedDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
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

    if (showStartTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = plannedStart?.hour ?: 9,
            initialMinute = plannedStart?.minute ?: 0
        )
        AlertDialog(
            onDismissRequest = { showStartTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    plannedStart = LocalTime.of(timePickerState.hour, timePickerState.minute)
                    showStartTimePicker = false
                }) {
                    Text("OK", color = colors.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartTimePicker = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            text = { TimePicker(state = timePickerState) },
            containerColor = colors.surface
        )
    }

    if (showEndTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = plannedEnd?.hour ?: (plannedStart?.hour?.plus(1) ?: 10),
            initialMinute = plannedEnd?.minute ?: 0
        )
        AlertDialog(
            onDismissRequest = { showEndTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    plannedEnd = LocalTime.of(timePickerState.hour, timePickerState.minute)
                    showEndTimePicker = false
                }) {
                    Text("OK", color = colors.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndTimePicker = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            text = { TimePicker(state = timePickerState) },
            containerColor = colors.surface
        )
    }
}
