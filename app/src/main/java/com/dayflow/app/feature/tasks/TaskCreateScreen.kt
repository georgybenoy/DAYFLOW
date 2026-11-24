package com.dayflow.app.feature.tasks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dayflow.app.core.designsystem.DayflowTheme
import com.dayflow.app.core.designsystem.DayflowTypography
import com.dayflow.app.core.designsystem.components.PriorityBadge
import com.dayflow.app.domain.model.DeadlineReminderLeadTime
import com.dayflow.app.domain.model.Priority
import com.dayflow.app.domain.model.TaskValidationError
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCreateScreen(
    taskId: String? = null,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TaskCreateViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            onNavigateBack()
        }
    }

    val colors = DayflowTheme.colors
    val spacing = DayflowTheme.spacing
    val shapes = DayflowTheme.shapes
    val scrollState = rememberScrollState()

    var showDeadlineDatePicker by remember { mutableStateOf(false) }
    var showDeadlineTimePicker by remember { mutableStateOf(false) }
    var showPlannedDatePicker by remember { mutableStateOf(false) }
    var showPlannedStartTimePicker by remember { mutableStateOf(false) }
    var showPlannedEndTimePicker by remember { mutableStateOf(false) }

    var newSubtaskText by remember { mutableStateOf("") }
    val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .imePadding()
    ) {
        TopAppBar(
            title = {
                Text(
                    text = if (uiState.isEditMode) "Edit Task" else "New Task",
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
            colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = spacing.screenHorizontal)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title input
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::updateTitle,
                label = { Text("Task Title *") },
                placeholder = { Text("e.g. Complete assignment report") },
                isError = uiState.validationError is TaskValidationError.BlankTitle,
                supportingText = {
                    if (uiState.validationError is TaskValidationError.BlankTitle) {
                        Text("Task title cannot be blank", color = colors.error)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.border,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary
                ),
                shape = shapes.input
            )

            // Description input
            OutlinedTextField(
                value = uiState.description,
                onValueChange = viewModel::updateDescription,
                label = { Text("Description (Optional)") },
                placeholder = { Text("Add instructions or notes...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.border,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary
                ),
                shape = shapes.input
            )

            // Priority Selection
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Priority Level",
                    style = DayflowTypography.titleSmall,
                    color = colors.textPrimary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Priority.entries.forEach { priority ->
                        val isSelected = uiState.priority == priority
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.updatePriority(priority) },
                            label = { PriorityBadge(priority = priority) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.surfaceElevated,
                                containerColor = colors.surface
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) colors.primary else colors.border
                            )
                        )
                    }
                }
            }

            // Category Selection
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Category",
                    style = DayflowTypography.titleSmall,
                    color = colors.textPrimary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.categories.forEach { cat ->
                        val isSelected = uiState.categoryId == cat.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.updateCategory(cat.id) },
                            label = { Text(cat.name, style = DayflowTypography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.primary,
                                selectedLabelColor = colors.surface
                            )
                        )
                    }
                }
            }

            // Deadline Section
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
                            text = "Commitment Deadline",
                            style = DayflowTypography.titleSmall,
                            color = colors.textPrimary
                        )
                        if (uiState.deadlineDate != null) {
                            TextButton(onClick = { viewModel.updateDeadlineDate(null) }) {
                                Text("Clear", color = colors.error, style = DayflowTypography.labelSmall)
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { showDeadlineDatePicker = true },
                            shape = shapes.chip,
                            border = BorderStroke(1.dp, colors.border),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = uiState.deadlineDate?.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
                                    ?: "Set Deadline Date",
                                style = DayflowTypography.labelSmall,
                                color = colors.textPrimary
                            )
                        }

                        if (uiState.deadlineDate != null) {
                            OutlinedButton(
                                onClick = { showDeadlineTimePicker = true },
                                shape = shapes.chip,
                                border = BorderStroke(1.dp, colors.border),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = uiState.deadlineTime?.format(timeFormatter) ?: "Set Time (Optional)",
                                    style = DayflowTypography.labelSmall,
                                    color = colors.textPrimary
                                )
                            }
                        }
                    }

                    // Deadline Reminders Multi-select
                    if (uiState.deadlineDate != null) {
                        Text(
                            text = "Deadline Reminders (Critical Channel)",
                            style = DayflowTypography.labelSmall,
                            color = colors.textMuted
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            DeadlineReminderLeadTime.entries.forEach { lead ->
                                val isSelected = uiState.deadlineReminders.contains(lead)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.toggleDeadlineReminder(lead) },
                                    label = { Text(lead.label, style = DayflowTypography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = colors.primary,
                                        selectedLabelColor = colors.surface
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Estimated Duration
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Estimated Duration",
                    style = DayflowTypography.titleSmall,
                    color = colors.textPrimary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val presets = listOf(15, 30, 45, 60, 90, 120)
                    presets.forEach { minutes ->
                        val isSelected = uiState.estimatedDurationMinutes == minutes
                        val label = if (minutes < 60) "${minutes}m" else "${minutes / 60}h" + if (minutes % 60 > 0) " ${minutes % 60}m" else ""
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                viewModel.updateEstimatedDuration(if (isSelected) null else minutes)
                            },
                            label = { Text(label, style = DayflowTypography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.primary,
                                selectedLabelColor = colors.surface
                            )
                        )
                    }
                }
            }

            // Planned Execution Window
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
                        Column {
                            Text(
                                text = "Planned Execution Window",
                                style = DayflowTypography.titleSmall,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Independent from deadline",
                                style = DayflowTypography.labelSmall,
                                color = colors.textMuted
                            )
                        }
                        if (uiState.plannedDate != null) {
                            TextButton(onClick = { viewModel.updatePlannedDate(null) }) {
                                Text("Clear", color = colors.error, style = DayflowTypography.labelSmall)
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { showPlannedDatePicker = true },
                        shape = shapes.chip,
                        border = BorderStroke(1.dp, colors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = uiState.plannedDate?.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
                                ?: "Set Planned Date",
                            style = DayflowTypography.labelSmall,
                            color = colors.textPrimary
                        )
                    }

                    if (uiState.plannedDate != null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = { showPlannedStartTimePicker = true },
                                shape = shapes.chip,
                                border = BorderStroke(1.dp, colors.border),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = uiState.plannedStart?.format(timeFormatter) ?: "Start Time",
                                    style = DayflowTypography.labelSmall,
                                    color = colors.textPrimary
                                )
                            }
                            OutlinedButton(
                                onClick = { showPlannedEndTimePicker = true },
                                shape = shapes.chip,
                                border = BorderStroke(1.dp, colors.border),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = uiState.plannedEnd?.format(timeFormatter) ?: "End Time",
                                    style = DayflowTypography.labelSmall,
                                    color = colors.textPrimary
                                )
                            }
                        }
                    }

                    if (uiState.validationError is TaskValidationError.InvalidPlannedTimeWindow) {
                        Text(
                            text = "Planned end time cannot be before planned start time",
                            style = DayflowTypography.labelSmall,
                            color = colors.error
                        )
                    }
                }
            }

            // Carry Forward Preference
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface, shapes.mainCard)
                    .padding(spacing.cardPadding)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Carry forward if unfinished",
                        style = DayflowTypography.titleSmall,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Suggests carry-forward in the evening review",
                        style = DayflowTypography.labelSmall,
                        color = colors.textSecondary
                    )
                }
                Switch(
                    checked = uiState.shouldCarryForward,
                    onCheckedChange = viewModel::updateCarryForward,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.surface,
                        checkedTrackColor = colors.primary
                    )
                )
            }

            // Subtasks Section
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
                    Text(
                        text = "Subtasks (${uiState.subtasks.size})",
                        style = DayflowTypography.titleSmall,
                        color = colors.textPrimary
                    )

                    // Subtask input
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = newSubtaskText,
                            onValueChange = { newSubtaskText = it },
                            placeholder = { Text("Add subtask title...") },
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

                    // Subtask items
                    uiState.subtasks.forEachIndexed { index, subtask ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(colors.surfaceElevated, shapes.chip)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${index + 1}. ${subtask.title}",
                                style = DayflowTypography.bodySmall,
                                color = colors.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (index > 0) {
                                    IconButton(
                                        onClick = { viewModel.moveSubtaskUp(index) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.KeyboardArrowUp,
                                            contentDescription = "Move up",
                                            tint = colors.textMuted
                                        )
                                    }
                                }
                                if (index < uiState.subtasks.size - 1) {
                                    IconButton(
                                        onClick = { viewModel.moveSubtaskDown(index) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.KeyboardArrowDown,
                                            contentDescription = "Move down",
                                            tint = colors.textMuted
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { viewModel.removeSubtask(subtask.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = "Delete subtask",
                                        tint = colors.error
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Save Task Button
            Button(
                onClick = viewModel::saveTask,
                enabled = !uiState.isSaving,
                shape = shapes.button,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = if (uiState.isEditMode) "Save Changes" else "Create Task",
                    style = DayflowTypography.titleMedium
                )
            }
        }
    }

    // Date/Time Dialogs
    if (showDeadlineDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = (uiState.deadlineDate ?: LocalDate.now())
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDeadlineDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        viewModel.updateDeadlineDate(
                            Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        )
                    }
                    showDeadlineDatePicker = false
                }) {
                    Text("Select", color = colors.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeadlineDatePicker = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showDeadlineTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = uiState.deadlineTime?.hour ?: 18,
            initialMinute = uiState.deadlineTime?.minute ?: 0
        )
        AlertDialog(
            onDismissRequest = { showDeadlineTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateDeadlineTime(LocalTime.of(timePickerState.hour, timePickerState.minute))
                    showDeadlineTimePicker = false
                }) {
                    Text("OK", color = colors.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeadlineTimePicker = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            text = { TimePicker(state = timePickerState) },
            containerColor = colors.surface
        )
    }

    if (showPlannedDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = (uiState.plannedDate ?: LocalDate.now())
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showPlannedDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        viewModel.updatePlannedDate(
                            Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        )
                    }
                    showPlannedDatePicker = false
                }) {
                    Text("Select", color = colors.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPlannedDatePicker = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showPlannedStartTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = uiState.plannedStart?.hour ?: 9,
            initialMinute = uiState.plannedStart?.minute ?: 0
        )
        AlertDialog(
            onDismissRequest = { showPlannedStartTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updatePlannedStart(LocalTime.of(timePickerState.hour, timePickerState.minute))
                    showPlannedStartTimePicker = false
                }) {
                    Text("OK", color = colors.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPlannedStartTimePicker = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            text = { TimePicker(state = timePickerState) },
            containerColor = colors.surface
        )
    }

    if (showPlannedEndTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = uiState.plannedEnd?.hour ?: (uiState.plannedStart?.hour?.plus(1) ?: 10),
            initialMinute = uiState.plannedEnd?.minute ?: 0
        )
        AlertDialog(
            onDismissRequest = { showPlannedEndTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updatePlannedEnd(LocalTime.of(timePickerState.hour, timePickerState.minute))
                    showPlannedEndTimePicker = false
                }) {
                    Text("OK", color = colors.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPlannedEndTimePicker = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            text = { TimePicker(state = timePickerState) },
            containerColor = colors.surface
        )
    }
}
