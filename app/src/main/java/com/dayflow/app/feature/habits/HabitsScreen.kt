package com.dayflow.app.feature.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import com.dayflow.app.core.designsystem.components.DateSelector
import com.dayflow.app.core.designsystem.components.EmptyState
import com.dayflow.app.core.designsystem.components.ProgressRing
import com.dayflow.app.feature.habits.components.HabitCard
import com.dayflow.app.feature.habits.components.QuickLogBottomSheet
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen(
    onNavigateToCreate: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onNavigateToDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HabitsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var habitToDeleteId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DayflowTheme.colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Habits",
                        style = DayflowTypography.headlineMedium,
                        color = DayflowTheme.colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DayflowTheme.colors.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreate,
                containerColor = DayflowTheme.colors.primary,
                contentColor = DayflowTheme.colors.background,
                shape = DayflowTheme.shapes.button
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create habit"
                )
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
            ) {
                // Date Selector Header
                DateSelector(
                    selectedDate = uiState.selectedDate,
                    onDateSelected = { viewModel.selectDate(it) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                if (uiState.habits.isEmpty()) {
                    EmptyState(
                        icon = Icons.Outlined.Repeat,
                        title = "No habits yet",
                        message = "Build consistent routines. Track binary, quantity, duration, or repetition goals.",
                        actionLabel = "Create Habit",
                        onActionClick = onNavigateToCreate,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Daily Progress Summary Card
                        item {
                            HabitDailyProgressCard(
                                progress = uiState.dailyProgress,
                                completedCount = uiState.completedCount,
                                totalCount = uiState.totalScheduledCount,
                                isToday = uiState.selectedDate == LocalDate.now()
                            )
                        }

                        // Active Habits Section
                        if (uiState.activeHabits.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Active Habits",
                                    style = DayflowTypography.titleSmall,
                                    color = DayflowTheme.colors.textSecondary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                )
                            }

                            items(uiState.activeHabits, key = { it.habit.id }) { itemModel ->
                                HabitCard(
                                    habit = itemModel.habit,
                                    category = itemModel.category,
                                    progress = itemModel.progress,
                                    streak = itemModel.streak,
                                    weeklyStatus = itemModel.weeklyStatus,
                                    isScheduledToday = itemModel.isScheduledToday,
                                    onCardClick = { onNavigateToDetail(itemModel.habit.id) },
                                    onToggleComplete = { viewModel.toggleBinaryHabit(itemModel.habit.id) },
                                    onOpenQuickLog = { viewModel.openQuickLog(itemModel.habit) },
                                    onEdit = { onNavigateToEdit(itemModel.habit.id) },
                                    onTogglePause = { viewModel.toggleHabitEnable(itemModel.habit.id) },
                                    onDelete = { habitToDeleteId = itemModel.habit.id }
                                )
                            }
                        }

                        // Paused Habits Section
                        if (uiState.pausedHabits.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Paused Habits",
                                    style = DayflowTypography.titleSmall,
                                    color = DayflowTheme.colors.textMuted,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                                )
                            }

                            items(uiState.pausedHabits, key = { it.habit.id }) { itemModel ->
                                HabitCard(
                                    habit = itemModel.habit,
                                    category = itemModel.category,
                                    progress = itemModel.progress,
                                    streak = itemModel.streak,
                                    weeklyStatus = itemModel.weeklyStatus,
                                    isScheduledToday = itemModel.isScheduledToday,
                                    onCardClick = { onNavigateToDetail(itemModel.habit.id) },
                                    onToggleComplete = { viewModel.toggleBinaryHabit(itemModel.habit.id) },
                                    onOpenQuickLog = { viewModel.openQuickLog(itemModel.habit) },
                                    onEdit = { onNavigateToEdit(itemModel.habit.id) },
                                    onTogglePause = { viewModel.toggleHabitEnable(itemModel.habit.id) },
                                    onDelete = { habitToDeleteId = itemModel.habit.id }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Quick Log Bottom Sheet
    uiState.quickLogHabit?.let { habit ->
        QuickLogBottomSheet(
            habit = habit,
            currentLog = uiState.quickLogCurrentLog,
            onSave = { value -> viewModel.saveQuickLog(value) },
            onDismiss = { viewModel.closeQuickLog() }
        )
    }

    // Delete Confirmation Dialog
    if (habitToDeleteId != null) {
        AlertDialog(
            onDismissRequest = { habitToDeleteId = null },
            title = { Text("Delete Habit", color = DayflowTheme.colors.textPrimary) },
            text = {
                Text(
                    "Are you sure you want to delete this habit? All progress logs and linked reminders will be permanently removed.",
                    color = DayflowTheme.colors.textSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        habitToDeleteId?.let { viewModel.deleteHabit(it) }
                        habitToDeleteId = null
                    }
                ) {
                    Text("Delete", color = DayflowTheme.colors.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { habitToDeleteId = null }) {
                    Text("Cancel", color = DayflowTheme.colors.textPrimary)
                }
            },
            containerColor = DayflowTheme.colors.surfaceElevated,
            shape = DayflowTheme.shapes.mainCard
        )
    }
}

@Composable
private fun HabitDailyProgressCard(
    progress: Float,
    completedCount: Int,
    totalCount: Int,
    isToday: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(DayflowTheme.shapes.mainCard),
        color = DayflowTheme.colors.surface,
        shape = DayflowTheme.shapes.mainCard
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isToday) "Today's Habits" else "Scheduled Habits",
                    style = DayflowTypography.titleMedium,
                    color = DayflowTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$completedCount of $totalCount completed",
                    style = DayflowTypography.bodyMedium,
                    color = DayflowTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.height(2.dp))
                val message = when {
                    totalCount == 0 -> "No habits scheduled for this day"
                    completedCount == totalCount -> "All habits completed! Great job!"
                    completedCount > 0 -> "Making good progress"
                    else -> "Ready to start today's routines"
                }
                Text(
                    text = message,
                    style = DayflowTypography.labelSmall,
                    color = DayflowTheme.colors.primary
                )
            }

            ProgressRing(
                progress = progress,
                size = 72.dp,
                strokeWidth = 8.dp
            )
        }
    }
}
