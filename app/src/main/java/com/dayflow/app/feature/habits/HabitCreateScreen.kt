package com.dayflow.app.feature.habits

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dayflow.app.core.designsystem.DayflowTheme
import com.dayflow.app.core.designsystem.DayflowTypography
import com.dayflow.app.domain.model.Category
import com.dayflow.app.domain.model.HabitTrackingType
import com.dayflow.app.domain.recurrence.ActiveDays
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HabitCreateScreen(
    habitId: String?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HabitCreateViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DayflowTheme.colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isEditMode) "Edit Habit" else "New Habit",
                        style = DayflowTypography.titleLarge,
                        color = DayflowTheme.colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = DayflowTheme.colors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DayflowTheme.colors.background)
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DayflowTheme.colors.background
            ) {
                Button(
                    onClick = { viewModel.saveHabit() },
                    enabled = !uiState.isSaving && uiState.name.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(52.dp),
                    shape = DayflowTheme.shapes.button,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DayflowTheme.colors.primary,
                        contentColor = DayflowTheme.colors.background,
                        disabledContainerColor = DayflowTheme.colors.surfaceElevated,
                        disabledContentColor = DayflowTheme.colors.textMuted
                    )
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = DayflowTheme.colors.background,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (uiState.isEditMode) "Update Habit" else "Create Habit",
                            style = DayflowTypography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = DayflowTheme.colors.primary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Name Field
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = { viewModel.updateName(it) },
                    label = { Text("Habit Name *") },
                    placeholder = { Text("e.g. Morning Jog, Read 30 mins") },
                    isError = uiState.nameError != null,
                    supportingText = uiState.nameError?.let { { Text(it, color = DayflowTheme.colors.error) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = DayflowTheme.shapes.input,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DayflowTheme.colors.primary,
                        unfocusedBorderColor = DayflowTheme.colors.border,
                        focusedLabelColor = DayflowTheme.colors.primary,
                        unfocusedLabelColor = DayflowTheme.colors.textSecondary,
                        focusedTextColor = DayflowTheme.colors.textPrimary,
                        unfocusedTextColor = DayflowTheme.colors.textPrimary
                    )
                )

                // Description Field
                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = { viewModel.updateDescription(it) },
                    label = { Text("Description (Optional)") },
                    placeholder = { Text("Why this habit matters, rules, etc.") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = DayflowTheme.shapes.input,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DayflowTheme.colors.primary,
                        unfocusedBorderColor = DayflowTheme.colors.border,
                        focusedLabelColor = DayflowTheme.colors.primary,
                        unfocusedLabelColor = DayflowTheme.colors.textSecondary,
                        focusedTextColor = DayflowTheme.colors.textPrimary,
                        unfocusedTextColor = DayflowTheme.colors.textPrimary
                    )
                )

                // Tracking Type Selector
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Tracking Type",
                        style = DayflowTypography.titleSmall,
                        color = DayflowTheme.colors.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HabitTrackingType.entries.forEach { type ->
                            val isSelected = uiState.trackingType == type
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.updateTrackingType(type) },
                                label = {
                                    Text(
                                        text = when (type) {
                                            HabitTrackingType.BINARY -> "Yes/No (Binary)"
                                            HabitTrackingType.QUANTITY -> "Quantity"
                                            HabitTrackingType.DURATION -> "Duration"
                                            HabitTrackingType.REPETITION -> "Repetitions"
                                        }
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DayflowTheme.colors.primary,
                                    selectedLabelColor = DayflowTheme.colors.background,
                                    containerColor = DayflowTheme.colors.surfaceElevated,
                                    labelColor = DayflowTheme.colors.textPrimary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = DayflowTheme.colors.border,
                                    selectedBorderColor = DayflowTheme.colors.primary,
                                    enabled = true,
                                    selected = isSelected
                                )
                            )
                        }
                    }
                }

                // Target & Unit Inputs (if not Binary)
                if (uiState.trackingType != HabitTrackingType.BINARY) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.targetValue,
                            onValueChange = { viewModel.updateTargetValue(it) },
                            label = { Text("Daily Target *") },
                            isError = uiState.targetValueError != null,
                            supportingText = uiState.targetValueError?.let { { Text(it, color = DayflowTheme.colors.error) } },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = DayflowTheme.shapes.input,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DayflowTheme.colors.primary,
                                unfocusedBorderColor = DayflowTheme.colors.border,
                                focusedLabelColor = DayflowTheme.colors.primary,
                                unfocusedLabelColor = DayflowTheme.colors.textSecondary,
                                focusedTextColor = DayflowTheme.colors.textPrimary,
                                unfocusedTextColor = DayflowTheme.colors.textPrimary
                            )
                        )

                        OutlinedTextField(
                            value = uiState.unit,
                            onValueChange = { viewModel.updateUnit(it) },
                            label = { Text("Unit") },
                            placeholder = { Text(if (uiState.trackingType == HabitTrackingType.DURATION) "min" else "pages, L, reps") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = DayflowTheme.shapes.input,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DayflowTheme.colors.primary,
                                unfocusedBorderColor = DayflowTheme.colors.border,
                                focusedLabelColor = DayflowTheme.colors.primary,
                                unfocusedLabelColor = DayflowTheme.colors.textSecondary,
                                focusedTextColor = DayflowTheme.colors.textPrimary,
                                unfocusedTextColor = DayflowTheme.colors.textPrimary
                            )
                        )
                    }
                }

                // Category Selector
                if (uiState.categories.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Category",
                            style = DayflowTypography.titleSmall,
                            color = DayflowTheme.colors.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = uiState.categoryId == null,
                                onClick = { viewModel.updateCategory(null) },
                                label = { Text("None") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DayflowTheme.colors.primary,
                                    selectedLabelColor = DayflowTheme.colors.background,
                                    containerColor = DayflowTheme.colors.surfaceElevated,
                                    labelColor = DayflowTheme.colors.textPrimary
                                )
                            )

                            uiState.categories.forEach { cat ->
                                val isSelected = uiState.categoryId == cat.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.updateCategory(cat.id) },
                                    label = { Text(cat.name) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = DayflowTheme.colors.primary,
                                        selectedLabelColor = DayflowTheme.colors.background,
                                        containerColor = DayflowTheme.colors.surfaceElevated,
                                        labelColor = DayflowTheme.colors.textPrimary
                                    )
                                )
                            }
                        }
                    }
                }

                // Active Days of Week
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Active Days",
                        style = DayflowTypography.titleSmall,
                        color = DayflowTheme.colors.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DayOfWeek.entries.forEach { day ->
                            val isActive = ActiveDays.isDayActive(uiState.activeDaysMask, day)
                            val dayLabel = day.name.take(3)

                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isActive) DayflowTheme.colors.primary
                                        else DayflowTheme.colors.surfaceElevated
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isActive) DayflowTheme.colors.primary else DayflowTheme.colors.border,
                                        shape = CircleShape
                                    )
                                    .clickable { viewModel.toggleWeekday(day) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayLabel.take(1),
                                    style = DayflowTypography.labelMedium,
                                    color = if (isActive) DayflowTheme.colors.background else DayflowTheme.colors.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Daily Reminder Preference
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = DayflowTheme.colors.surface,
                    shape = DayflowTheme.shapes.mainCard
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Daily Reminder",
                                    style = DayflowTypography.titleSmall,
                                    color = DayflowTheme.colors.textPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Receive an alarm reminder on scheduled days",
                                    style = DayflowTypography.bodySmall,
                                    color = DayflowTheme.colors.textSecondary
                                )
                            }

                            Switch(
                                checked = uiState.reminderPreference,
                                onCheckedChange = { viewModel.toggleReminderPreference(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = DayflowTheme.colors.background,
                                    checkedTrackColor = DayflowTheme.colors.primary,
                                    uncheckedThumbColor = DayflowTheme.colors.textMuted,
                                    uncheckedTrackColor = DayflowTheme.colors.surfaceElevated
                                )
                            )
                        }

                        if (uiState.reminderPreference) {
                            val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(DayflowTheme.shapes.input)
                                    .background(DayflowTheme.colors.surfaceElevated)
                                    .clickable {
                                        TimePickerDialog(
                                            context,
                                            { _, hour, minute ->
                                                viewModel.updateReminderTime(LocalTime.of(hour, minute))
                                            },
                                            uiState.reminderTime.hour,
                                            uiState.reminderTime.minute,
                                            false
                                        ).show()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.AccessTime,
                                        contentDescription = null,
                                        tint = DayflowTheme.colors.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Reminder Time",
                                        style = DayflowTypography.bodyMedium,
                                        color = DayflowTheme.colors.textPrimary
                                    )
                                }

                                Text(
                                    text = uiState.reminderTime.format(timeFormatter),
                                    style = DayflowTypography.titleSmall,
                                    color = DayflowTheme.colors.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
