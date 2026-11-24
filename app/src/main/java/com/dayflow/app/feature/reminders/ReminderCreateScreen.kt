package com.dayflow.app.feature.reminders

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dayflow.app.core.designsystem.DayflowTheme
import com.dayflow.app.core.designsystem.DayflowTypography
import com.dayflow.app.core.designsystem.components.PriorityBadge
import com.dayflow.app.core.time.ReminderFormatter
import com.dayflow.app.domain.model.Priority
import com.dayflow.app.domain.model.ReminderPrecision
import com.dayflow.app.domain.model.ReminderType
import com.dayflow.app.domain.recurrence.ReminderValidationError
import com.dayflow.app.domain.recurrence.ReminderValidationResult
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReminderCreateScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    reminderId: String? = null,
    viewModel: ReminderCreateViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    LaunchedEffect(reminderId) {
        viewModel.initForReminder(reminderId)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ReminderCreateEvent.SaveSuccess -> onNavigateBack()
                is ReminderCreateEvent.ShowError -> { /* surface error message */ }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DayflowTheme.colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isEditing) "Edit Reminder" else "New Reminder",
                        style = DayflowTypography.titleLarge,
                        color = DayflowTheme.colors.textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Navigate back",
                            tint = DayflowTheme.colors.textPrimary
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.saveReminder() },
                        enabled = !uiState.isSaving
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = DayflowTheme.colors.primary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Save",
                                style = DayflowTypography.labelLarge,
                                color = DayflowTheme.colors.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DayflowTheme.colors.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = DayflowTheme.spacing.screenHorizontal)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(DayflowTheme.spacing.sectionSpacing)
        ) {
            // Section 1: Title & Description
            Column(verticalArrangement = Arrangement.spacedBy(DayflowTheme.spacing.sm)) {
                val hasTitleError = (uiState.validationResult as? ReminderValidationResult.Invalid)
                    ?.errors?.contains(ReminderValidationError.BlankTitle) == true

                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = viewModel::onTitleChange,
                    label = { Text("Reminder Title *") },
                    placeholder = { Text("e.g. Drink Water, Team Standup") },
                    isError = hasTitleError,
                    supportingText = if (hasTitleError) {
                        { Text("Title cannot be empty", color = DayflowTheme.colors.error) }
                    } else null,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DayflowTheme.colors.primary,
                        unfocusedBorderColor = DayflowTheme.colors.border,
                        focusedTextColor = DayflowTheme.colors.textPrimary,
                        unfocusedTextColor = DayflowTheme.colors.textPrimary,
                        focusedLabelColor = DayflowTheme.colors.primary,
                        unfocusedLabelColor = DayflowTheme.colors.textSecondary
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = DayflowTheme.shapes.input
                )

                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = viewModel::onDescriptionChange,
                    label = { Text("Notes / Description (Optional)") },
                    placeholder = { Text("Add any extra instructions or context") },
                    minLines = 2,
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DayflowTheme.colors.primary,
                        unfocusedBorderColor = DayflowTheme.colors.border,
                        focusedTextColor = DayflowTheme.colors.textPrimary,
                        unfocusedTextColor = DayflowTheme.colors.textPrimary,
                        focusedLabelColor = DayflowTheme.colors.primary,
                        unfocusedLabelColor = DayflowTheme.colors.textSecondary
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = DayflowTheme.shapes.input
                )
            }

            // Section 2: Category Selector (if available)
            if (uiState.availableCategories.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "CATEGORY",
                        style = DayflowTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = DayflowTheme.colors.textSecondary
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.availableCategories) { cat ->
                            val isSelected = uiState.categoryId == cat.id
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    viewModel.onCategorySelect(if (isSelected) null else cat.id)
                                },
                                label = { Text(cat.name, style = DayflowTypography.labelMedium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DayflowTheme.colors.primary.copy(alpha = 0.2f),
                                    selectedLabelColor = DayflowTheme.colors.primary,
                                    containerColor = DayflowTheme.colors.surfaceVariant,
                                    labelColor = DayflowTheme.colors.textSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = if (isSelected) DayflowTheme.colors.primary else DayflowTheme.colors.border,
                                    enabled = true,
                                    selected = isSelected
                                ),
                                shape = DayflowTheme.shapes.chip
                            )
                        }
                    }
                }
            }

            // Section 3: Recurrence Type Selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "RECURRENCE TYPE",
                    style = DayflowTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = DayflowTheme.colors.textSecondary
                )

                val types = listOf(
                    ReminderType.ONE_TIME to "One-time",
                    ReminderType.DAILY to "Daily",
                    ReminderType.WEEKLY to "Weekly",
                    ReminderType.CUSTOM_WEEKDAYS to "Custom Days",
                    ReminderType.FIXED_INTERVAL to "Fixed Interval",
                    ReminderType.COMPLETION_INTERVAL to "After Completion"
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    types.forEach { (type, label) ->
                        val isSelected = uiState.type == type
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.onTypeSelect(type) },
                            label = { Text(label, style = DayflowTypography.labelMedium) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DayflowTheme.colors.primary.copy(alpha = 0.25f),
                                selectedLabelColor = DayflowTheme.colors.primary,
                                containerColor = DayflowTheme.colors.surfaceVariant,
                                labelColor = DayflowTheme.colors.textSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = if (isSelected) DayflowTheme.colors.primary else DayflowTheme.colors.border,
                                enabled = true,
                                selected = isSelected
                            ),
                            shape = DayflowTheme.shapes.chip
                        )
                    }
                }
            }

            // Section 4: Schedule Parameters Dynamic to Type
            Card(
                shape = DayflowTheme.shapes.mainCard,
                colors = CardDefaults.cardColors(
                    containerColor = DayflowTheme.colors.surface,
                    contentColor = DayflowTheme.colors.textPrimary
                ),
                border = BorderStroke(1.dp, DayflowTheme.colors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(DayflowTheme.spacing.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "SCHEDULE CONFIGURATION",
                        style = DayflowTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = DayflowTheme.colors.primary
                    )

                    when (uiState.type) {
                        ReminderType.ONE_TIME -> {
                            // Date & Time pickers
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                DateTimePickerBox(
                                    title = "Date",
                                    value = uiState.startDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                                    icon = Icons.Outlined.CalendarMonth,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        showDatePicker(context, uiState.startDate) { newDate ->
                                            viewModel.onStartDateChange(newDate)
                                        }
                                    }
                                )
                                DateTimePickerBox(
                                    title = "Time",
                                    value = uiState.startTime.format(DateTimeFormatter.ofPattern("hh:mm a")),
                                    icon = Icons.Outlined.Schedule,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        showTimePicker(context, uiState.startTime) { newTime ->
                                            viewModel.onStartTimeChange(newTime)
                                        }
                                    }
                                )
                            }
                        }

                        ReminderType.DAILY, ReminderType.WEEKLY -> {
                            DateTimePickerBox(
                                title = "Trigger Time",
                                value = uiState.startTime.format(DateTimeFormatter.ofPattern("hh:mm a")),
                                icon = Icons.Outlined.Schedule,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    showTimePicker(context, uiState.startTime) { newTime ->
                                        viewModel.onStartTimeChange(newTime)
                                    }
                                }
                            )
                        }

                        ReminderType.CUSTOM_WEEKDAYS -> {
                            DateTimePickerBox(
                                title = "Trigger Time",
                                value = uiState.startTime.format(DateTimeFormatter.ofPattern("hh:mm a")),
                                icon = Icons.Outlined.Schedule,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    showTimePicker(context, uiState.startTime) { newTime ->
                                        viewModel.onStartTimeChange(newTime)
                                    }
                                }
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Active Days",
                                    style = DayflowTypography.bodySmall,
                                    color = DayflowTheme.colors.textSecondary
                                )
                                val dayNames = listOf("M", "T", "W", "T", "F", "S", "S")
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    dayNames.forEachIndexed { index, name ->
                                        val isDayActive = (uiState.activeDaysMask and (1 shl index)) != 0
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .background(
                                                    color = if (isDayActive) DayflowTheme.colors.primary else DayflowTheme.colors.surfaceVariant,
                                                    shape = DayflowTheme.shapes.chip
                                                )
                                                .clickable { viewModel.toggleWeekday(index) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = name,
                                                style = DayflowTypography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = if (isDayActive) DayflowTheme.colors.background else DayflowTheme.colors.textSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        ReminderType.FIXED_INTERVAL, ReminderType.COMPLETION_INTERVAL -> {
                            // Interval minutes selector
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = if (uiState.type == ReminderType.FIXED_INTERVAL) "Interval Cadence" else "Interval After Completion",
                                    style = DayflowTypography.bodySmall,
                                    color = DayflowTheme.colors.textSecondary
                                )

                                val quickIntervals = listOf(
                                    30 to "30m",
                                    60 to "1h",
                                    120 to "2h",
                                    180 to "3h",
                                    240 to "4h"
                                )

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    quickIntervals.forEach { (mins, label) ->
                                        val isSelected = uiState.intervalMinutes == mins
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { viewModel.onIntervalMinutesChange(mins) },
                                            label = { Text(label, style = DayflowTypography.labelSmall) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = DayflowTheme.colors.primary.copy(alpha = 0.25f),
                                                selectedLabelColor = DayflowTheme.colors.primary,
                                                containerColor = DayflowTheme.colors.surfaceVariant,
                                                labelColor = DayflowTheme.colors.textSecondary
                                            ),
                                            shape = DayflowTheme.shapes.chip
                                        )
                                    }
                                }
                            }

                            // Window bounds: Start & End time
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                DateTimePickerBox(
                                    title = "Window Start",
                                    value = uiState.startTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                                    icon = Icons.Outlined.Schedule,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        showTimePicker(context, uiState.startTime) { newTime ->
                                            viewModel.onStartTimeChange(newTime)
                                        }
                                    }
                                )
                                DateTimePickerBox(
                                    title = "Window End",
                                    value = (uiState.endTime ?: LocalTime.of(22, 0)).format(DateTimeFormatter.ofPattern("HH:mm")),
                                    icon = Icons.Outlined.Schedule,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        showTimePicker(context, uiState.endTime ?: LocalTime.of(22, 0)) { newTime ->
                                            viewModel.onEndTimeChange(newTime)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Section 5: Priority & Precision
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Priority
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "PRIORITY",
                        style = DayflowTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = DayflowTheme.colors.textSecondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(Priority.LOW, Priority.MEDIUM, Priority.HIGH, Priority.URGENT).forEach { p ->
                            val isSelected = uiState.priority == p
                            Box(
                                modifier = Modifier
                                    .clickable { viewModel.onPrioritySelect(p) }
                                    .padding(vertical = 4.dp)
                            ) {
                                PriorityBadge(priority = p)
                            }
                        }
                    }
                }
            }

            // Precision & Snooze
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Precision Selector
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "PRECISION",
                        style = DayflowTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = DayflowTheme.colors.textSecondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = uiState.precision == ReminderPrecision.EXACT,
                            onClick = { viewModel.onPrecisionSelect(ReminderPrecision.EXACT) },
                            label = { Text("Exact", style = DayflowTypography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DayflowTheme.colors.primary.copy(alpha = 0.25f),
                                selectedLabelColor = DayflowTheme.colors.primary,
                                containerColor = DayflowTheme.colors.surfaceVariant,
                                labelColor = DayflowTheme.colors.textSecondary
                            ),
                            shape = DayflowTheme.shapes.chip
                        )
                        FilterChip(
                            selected = uiState.precision == ReminderPrecision.APPROXIMATE,
                            onClick = { viewModel.onPrecisionSelect(ReminderPrecision.APPROXIMATE) },
                            label = { Text("Approx.", style = DayflowTypography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DayflowTheme.colors.warning.copy(alpha = 0.25f),
                                selectedLabelColor = DayflowTheme.colors.warning,
                                containerColor = DayflowTheme.colors.surfaceVariant,
                                labelColor = DayflowTheme.colors.textSecondary
                            ),
                            shape = DayflowTheme.shapes.chip
                        )
                    }
                }

                // Snooze duration
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "DEFAULT SNOOZE",
                        style = DayflowTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = DayflowTheme.colors.textSecondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(5L to "5m", 10L to "10m", 15L to "15m").forEach { (m, label) ->
                            val isSelected = uiState.snoozeDurationMinutes == m
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.onSnoozeDurationChange(m) },
                                label = { Text(label, style = DayflowTypography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DayflowTheme.colors.primary.copy(alpha = 0.25f),
                                    selectedLabelColor = DayflowTheme.colors.primary,
                                    containerColor = DayflowTheme.colors.surfaceVariant,
                                    labelColor = DayflowTheme.colors.textSecondary
                                ),
                                shape = DayflowTheme.shapes.chip
                            )
                        }
                    }
                }
            }

            // Section 6: Live Validation Errors Display
            if (uiState.validationResult is ReminderValidationResult.Invalid) {
                val errors = (uiState.validationResult as ReminderValidationResult.Invalid).errors
                Card(
                    shape = DayflowTheme.shapes.secondaryCard,
                    colors = CardDefaults.cardColors(
                        containerColor = DayflowTheme.colors.error.copy(alpha = 0.12f),
                        contentColor = DayflowTheme.colors.error
                    ),
                    border = BorderStroke(1.dp, DayflowTheme.colors.error.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.ErrorOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Please correct the following errors:", style = DayflowTypography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }
                        errors.forEach { err ->
                            val msg = when (err) {
                                ReminderValidationError.BlankTitle -> "• Reminder title is required"
                                ReminderValidationError.MissingInterval -> "• Interval in minutes is required"
                                ReminderValidationError.IntervalTooShort -> "• Interval must be at least 1 minute"
                                ReminderValidationError.MissingWindowTimes -> "• Window start and end times are required"
                                ReminderValidationError.NoWeekdaysSelected -> "• Select at least one active weekday"
                                ReminderValidationError.EndDateBeforeStartDate -> "• End date cannot be before start date"
                            }
                            Text(msg, style = DayflowTypography.bodySmall)
                        }
                    }
                }
            }

            // Section 7: Live "Next 5 Occurrences" Preview (Computed strictly by RecurrenceCalculator)
            Card(
                shape = DayflowTheme.shapes.mainCard,
                colors = CardDefaults.cardColors(
                    containerColor = DayflowTheme.colors.surfaceVariant,
                    contentColor = DayflowTheme.colors.textPrimary
                ),
                border = BorderStroke(1.dp, DayflowTheme.colors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(DayflowTheme.spacing.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PREVIEW: NEXT 5 OCCURRENCES",
                            style = DayflowTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = DayflowTheme.colors.primary
                        )
                        Text(
                            text = "Calculated Live",
                            style = DayflowTypography.labelSmall,
                            color = DayflowTheme.colors.textSecondary
                        )
                    }

                    if (uiState.previewOccurrences.isEmpty()) {
                        Text(
                            text = "No future occurrences generated with current settings.",
                            style = DayflowTypography.bodySmall,
                            color = DayflowTheme.colors.textSecondary
                        )
                    } else {
                        uiState.previewOccurrences.forEachIndexed { index, instant ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${index + 1}.",
                                        style = DayflowTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = DayflowTheme.colors.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = ReminderFormatter.formatOccurrenceTime(instant),
                                        style = DayflowTypography.bodyMedium,
                                        color = DayflowTheme.colors.textPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Save Reminder Action Button
            Button(
                onClick = { viewModel.saveReminder() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = DayflowTheme.colors.primary,
                    contentColor = DayflowTheme.colors.background
                ),
                shape = DayflowTheme.shapes.button,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !uiState.isSaving
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = DayflowTheme.colors.background,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (uiState.isEditing) "Update Reminder" else "Save Reminder",
                        style = DayflowTypography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}

@Composable
private fun DateTimePickerBox(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = DayflowTheme.shapes.secondaryCard,
        colors = CardDefaults.cardColors(
            containerColor = DayflowTheme.colors.surfaceVariant,
            contentColor = DayflowTheme.colors.textPrimary
        ),
        border = BorderStroke(1.dp, DayflowTheme.colors.border),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = title,
                style = DayflowTypography.labelSmall,
                color = DayflowTheme.colors.textSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = DayflowTheme.colors.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = value,
                    style = DayflowTypography.titleSmall,
                    color = DayflowTheme.colors.textPrimary
                )
            }
        }
    }
}

private fun showDatePicker(context: android.content.Context, initialDate: LocalDate, onDateSelected: (LocalDate) -> Unit) {
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            onDateSelected(LocalDate.of(year, month + 1, dayOfMonth))
        },
        initialDate.year,
        initialDate.monthValue - 1,
        initialDate.dayOfMonth
    ).show()
}

private fun showTimePicker(context: android.content.Context, initialTime: LocalTime, onTimeSelected: (LocalTime) -> Unit) {
    TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            onTimeSelected(LocalTime.of(hourOfDay, minute))
        },
        initialTime.hour,
        initialTime.minute,
        false
    ).show()
}
