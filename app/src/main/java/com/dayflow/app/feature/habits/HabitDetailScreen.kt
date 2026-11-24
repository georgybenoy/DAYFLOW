package com.dayflow.app.feature.habits

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dayflow.app.core.designsystem.DayflowTheme
import com.dayflow.app.core.designsystem.DayflowTypography
import com.dayflow.app.domain.model.HabitTrackingType
import com.dayflow.app.domain.recurrence.ActiveDays
import com.dayflow.app.feature.habits.components.BinaryCompletionButton
import com.dayflow.app.feature.habits.components.CategoryPastelChip
import com.dayflow.app.feature.habits.components.HabitCalendar
import com.dayflow.app.feature.habits.components.QuickLogBottomSheet
import com.dayflow.app.feature.habits.components.QuickLogButton
import com.dayflow.app.feature.habits.components.WeeklyDotsStrip
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailScreen(
    habitId: String,
    onNavigateToEdit: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HabitDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
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
                        text = "Habit Detail",
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
                actions = {
                    if (uiState.habit != null) {
                        IconButton(onClick = { viewModel.toggleEnable() }) {
                            Icon(
                                imageVector = if (uiState.habit!!.isEnabled) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                                contentDescription = if (uiState.habit!!.isEnabled) "Pause" else "Resume",
                                tint = DayflowTheme.colors.textSecondary
                            )
                        }
                        IconButton(onClick = { onNavigateToEdit(habitId) }) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Edit",
                                tint = DayflowTheme.colors.textSecondary
                            )
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Delete",
                                tint = DayflowTheme.colors.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DayflowTheme.colors.background)
            )
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
        } else if (uiState.habit == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Habit not found",
                    style = DayflowTypography.bodyLarge,
                    color = DayflowTheme.colors.textSecondary
                )
            }
        } else {
            val habit = uiState.habit!!

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Info
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = DayflowTheme.colors.surface,
                    shape = DayflowTheme.shapes.mainCard
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (uiState.category != null) {
                                CategoryPastelChip(category = uiState.category!!)
                            }
                            Text(
                                text = habit.trackingType.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = DayflowTypography.labelSmall,
                                color = DayflowTheme.colors.textMuted,
                                modifier = Modifier
                                    .background(
                                        DayflowTheme.colors.surfaceElevated,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                            if (!habit.isEnabled) {
                                Text(
                                    text = "PAUSED",
                                    style = DayflowTypography.labelSmall,
                                    color = DayflowTheme.colors.error,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .background(
                                            DayflowTheme.colors.error.copy(alpha = 0.15f),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = habit.name,
                            style = DayflowTypography.headlineSmall,
                            color = DayflowTheme.colors.textPrimary,
                            fontWeight = FontWeight.Bold
                        )

                        if (!habit.description.isNullOrBlank()) {
                            Text(
                                text = habit.description!!,
                                style = DayflowTypography.bodyMedium,
                                color = DayflowTheme.colors.textSecondary
                            )
                        }
                    }
                }

                // Streak Cards Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Current Streak
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = DayflowTheme.colors.surface,
                        shape = DayflowTheme.shapes.mainCard
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = DayflowTheme.colors.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "CURRENT",
                                    style = DayflowTypography.labelSmall,
                                    color = DayflowTheme.colors.textSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "${uiState.streak.currentStreak} days",
                                style = DayflowTypography.titleLarge,
                                color = DayflowTheme.colors.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Longest Streak
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = DayflowTheme.colors.surface,
                        shape = DayflowTheme.shapes.mainCard
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "🏆 BEST STREAK",
                                style = DayflowTypography.labelSmall,
                                color = DayflowTheme.colors.textSecondary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${uiState.streak.longestStreak} days",
                                style = DayflowTypography.titleLarge,
                                color = DayflowTheme.colors.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Today's Status & Action Card
                uiState.todayProgress?.let { progress ->
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
                                        text = "Today's Execution",
                                        style = DayflowTypography.titleMedium,
                                        color = DayflowTheme.colors.textPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = progress.formattedProgress,
                                        style = DayflowTypography.bodyMedium,
                                        color = DayflowTheme.colors.textSecondary
                                    )
                                }

                                if (habit.trackingType == HabitTrackingType.BINARY) {
                                    BinaryCompletionButton(
                                        isCompleted = progress.isCompleted,
                                        onClick = { viewModel.toggleTodayBinary() }
                                    )
                                } else {
                                    QuickLogButton(
                                        isCompleted = progress.isCompleted,
                                        onClick = { viewModel.openQuickLog() }
                                    )
                                }
                            }

                            LinearProgressIndicator(
                                progress = { progress.progressRatio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = DayflowTheme.colors.primary,
                                trackColor = DayflowTheme.colors.surfaceElevated
                            )

                            if (uiState.weeklyStatus.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "This Week",
                                    style = DayflowTypography.labelSmall,
                                    color = DayflowTheme.colors.textMuted
                                )
                                WeeklyDotsStrip(weeklyStatus = uiState.weeklyStatus)
                            }
                        }
                    }
                }

                // Monthly Heatmap History
                Text(
                    text = "Monthly Consistency",
                    style = DayflowTypography.titleMedium,
                    color = DayflowTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )

                HabitCalendar(
                    habit = habit,
                    logs = uiState.allLogs
                )

                // Schedule & Reminder Config Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = DayflowTheme.colors.surface,
                    shape = DayflowTheme.shapes.mainCard
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Schedule & Settings",
                            style = DayflowTypography.titleSmall,
                            color = DayflowTheme.colors.textPrimary,
                            fontWeight = FontWeight.Bold
                        )

                        // Weekdays active
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Repeat,
                                contentDescription = null,
                                tint = DayflowTheme.colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            val activeDayNames = DayOfWeek.entries
                                .filter { ActiveDays.isDayActive(habit.activeDaysMask, it) }
                                .joinToString(", ") { it.name.take(3) }
                            Text(
                                text = if (habit.activeDaysMask == ActiveDays.ALL_DAYS_MASK) "Every day"
                                else if (habit.activeDaysMask == ActiveDays.WEEKDAYS_MASK) "Weekdays (Mon-Fri)"
                                else if (habit.activeDaysMask == ActiveDays.WEEKENDS_MASK) "Weekends (Sat-Sun)"
                                else activeDayNames,
                                style = DayflowTypography.bodyMedium,
                                color = DayflowTheme.colors.textSecondary
                            )
                        }

                        // Reminder info
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = null,
                                tint = DayflowTheme.colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            val reminderText = if (habit.reminderPreference && habit.reminderTime != null) {
                                val formatter = DateTimeFormatter.ofPattern("hh:mm a")
                                "Reminder at ${habit.reminderTime!!.format(formatter)}"
                            } else {
                                "Reminders disabled"
                            }
                            Text(
                                text = reminderText,
                                style = DayflowTypography.bodyMedium,
                                color = DayflowTheme.colors.textSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Quick Log Sheet
    if (uiState.quickLogOpen && uiState.habit != null) {
        QuickLogBottomSheet(
            habit = uiState.habit!!,
            currentLog = uiState.todayLog,
            onSave = { value -> viewModel.saveQuickLog(value) },
            onDismiss = { viewModel.closeQuickLog() }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Habit", color = DayflowTheme.colors.textPrimary) },
            text = {
                Text(
                    "Are you sure you want to permanently delete this habit and all logged history?",
                    color = DayflowTheme.colors.textSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteHabit()
                        showDeleteConfirm = false
                    }
                ) {
                    Text("Delete", color = DayflowTheme.colors.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = DayflowTheme.colors.textPrimary)
                }
            },
            containerColor = DayflowTheme.colors.surfaceElevated,
            shape = DayflowTheme.shapes.mainCard
        )
    }
}
