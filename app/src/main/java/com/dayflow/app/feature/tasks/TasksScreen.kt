package com.dayflow.app.feature.tasks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dayflow.app.core.designsystem.DayflowTheme
import com.dayflow.app.core.designsystem.DayflowTypography
import com.dayflow.app.core.designsystem.components.EmptyState
import com.dayflow.app.domain.model.Category
import com.dayflow.app.domain.model.Priority
import com.dayflow.app.domain.model.Task
import com.dayflow.app.feature.tasks.components.RescheduleDialog
import com.dayflow.app.feature.tasks.components.TaskCard
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun TasksScreen(
    onNavigateToCreate: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onNavigateToDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TasksViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var taskToReschedule by remember { mutableStateOf<Task?>(null) }
    var taskToDelete by remember { mutableStateOf<Task?>(null) }

    TasksContent(
        uiState = uiState,
        onSelectTab = viewModel::selectTab,
        onSelectCategory = viewModel::selectCategory,
        onSelectPriority = viewModel::selectPriority,
        onToggleComplete = viewModel::toggleTaskComplete,
        onTaskClick = onNavigateToDetail,
        onEditClick = onNavigateToEdit,
        onRescheduleClick = { task -> taskToReschedule = task },
        onDeleteClick = { task -> taskToDelete = task },
        onCreateClick = onNavigateToCreate,
        modifier = modifier
    )

    taskToReschedule?.let { task ->
        RescheduleDialog(
            initialPlannedDate = task.plannedDate,
            initialPlannedStart = task.plannedStart,
            initialPlannedEnd = task.plannedEnd,
            onDismiss = { taskToReschedule = null },
            onConfirm = { date, start, end ->
                viewModel.reschedulePlannedTime(task.id, date, start, end)
                taskToReschedule = null
            }
        )
    }

    taskToDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = {
                Text(
                    text = "Delete Task?",
                    style = DayflowTypography.titleMedium,
                    color = DayflowTheme.colors.textPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete \"${task.title}\"? Any associated subtasks and deadline reminders will also be deleted.",
                    style = DayflowTypography.bodyMedium,
                    color = DayflowTheme.colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTask(task.id)
                        taskToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DayflowTheme.colors.error,
                        contentColor = DayflowTheme.colors.surface
                    ),
                    shape = DayflowTheme.shapes.button
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) {
                    Text("Cancel", color = DayflowTheme.colors.textSecondary)
                }
            },
            containerColor = DayflowTheme.colors.surface,
            shape = DayflowTheme.shapes.mainCard
        )
    }
}

@Composable
fun TasksContent(
    uiState: TasksUiState,
    onSelectTab: (TaskFilterTab) -> Unit,
    onSelectCategory: (String?) -> Unit,
    onSelectPriority: (Priority?) -> Unit,
    onToggleComplete: (Task) -> Unit,
    onTaskClick: (String) -> Unit,
    onEditClick: (String) -> Unit,
    onRescheduleClick: (Task) -> Unit,
    onDeleteClick: (Task) -> Unit,
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DayflowTheme.colors
    val spacing = DayflowTheme.spacing
    val categoryMap = remember(uiState.categories) { uiState.categories.associateBy { it.id } }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Text(
                text = "Tasks",
                style = DayflowTypography.headlineMedium,
                color = colors.textPrimary,
                modifier = Modifier.padding(horizontal = spacing.screenHorizontal, vertical = 8.dp)
            )

            // Filter Tabs
            ScrollableTabRow(
                selectedTabIndex = uiState.selectedTab.ordinal,
                containerColor = colors.background,
                contentColor = colors.primary,
                edgePadding = spacing.screenHorizontal,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[uiState.selectedTab.ordinal]),
                        color = colors.primary
                    )
                },
                divider = {}
            ) {
                TaskFilterTab.entries.forEach { tab ->
                    val count = uiState.activeTaskCounts[tab] ?: 0
                    Tab(
                        selected = uiState.selectedTab == tab,
                        onClick = { onSelectTab(tab) },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = tab.title,
                                    style = DayflowTypography.titleSmall,
                                    color = if (uiState.selectedTab == tab) colors.primary else colors.textMuted
                                )
                                if (count > 0) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (tab == TaskFilterTab.OVERDUE) colors.error.copy(alpha = 0.2f)
                                                else colors.surfaceElevated,
                                                shape = DayflowTheme.shapes.chip
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = count.toString(),
                                            style = DayflowTypography.labelSmall,
                                            color = if (tab == TaskFilterTab.OVERDUE) colors.error else colors.textSecondary
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            }

            // Category & Priority filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = spacing.screenHorizontal, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Priority chips
                Priority.entries.forEach { priority ->
                    val isSelected = uiState.selectedPriority == priority
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectPriority(priority) },
                        label = { Text(priority.name, style = DayflowTypography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.primary,
                            selectedLabelColor = colors.surface
                        )
                    )
                }

                // Category chips
                uiState.categories.forEach { category ->
                    val isSelected = uiState.selectedCategoryId == category.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectCategory(category.id) },
                        label = { Text(category.name, style = DayflowTypography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.primary,
                            selectedLabelColor = colors.surface
                        )
                    )
                }
            }

            // Task List
            if (uiState.isLoading && uiState.tasks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.primary)
                }
            } else if (uiState.tasks.isEmpty()) {
                val (emptyTitle, emptyMsg, emptyIcon) = when (uiState.selectedTab) {
                    TaskFilterTab.TODAY -> Triple(
                        "No tasks planned for today",
                        "Create a task or plan one from upcoming commitments to stay on top of your daily execution.",
                        Icons.Outlined.Checklist
                    )
                    TaskFilterTab.UPCOMING -> Triple(
                        "No upcoming tasks",
                        "You have no tasks scheduled for future dates. Add assignments and commitments in advance.",
                        Icons.Outlined.Event
                    )
                    TaskFilterTab.OVERDUE -> Triple(
                        "No overdue tasks!",
                        "You are fully caught up with all your deadlines. Great job maintaining momentum!",
                        Icons.Outlined.CheckCircle
                    )
                    TaskFilterTab.COMPLETED -> Triple(
                        "No completed tasks yet",
                        "Completed tasks will be archived here for review and historical streak insights.",
                        Icons.Outlined.CheckCircle
                    )
                    TaskFilterTab.ALL -> Triple(
                        "No tasks recorded",
                        "Tap the plus button below to create your first task with priority and deadline tracking.",
                        Icons.Outlined.Checklist
                    )
                }

                EmptyState(
                    title = emptyTitle,
                    message = emptyMsg,
                    icon = emptyIcon,
                    actionLabel = if (uiState.selectedTab != TaskFilterTab.COMPLETED && uiState.selectedTab != TaskFilterTab.OVERDUE) "New Task" else null,
                    onActionClick = onCreateClick,
                    modifier = Modifier.padding(top = 48.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = spacing.screenHorizontal,
                        end = spacing.screenHorizontal,
                        top = 8.dp,
                        bottom = 88.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(spacing.cardSpacing)
                ) {
                    items(uiState.tasks, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            isOverdue = uiState.overdueTaskIds.contains(task.id),
                            category = task.categoryId?.let { categoryMap[it] },
                            onTaskClick = { onTaskClick(task.id) },
                            onToggleComplete = { onToggleComplete(task) },
                            onEditClick = { onEditClick(task.id) },
                            onRescheduleClick = { onRescheduleClick(task) },
                            onDeleteClick = { onDeleteClick(task) }
                        )
                    }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = onCreateClick,
            containerColor = colors.primary,
            contentColor = colors.surface,
            shape = DayflowTheme.shapes.button,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Create new task"
            )
        }
    }
}
