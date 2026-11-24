package com.dayflow.app.feature.reminders

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dayflow.app.core.designsystem.DayflowTheme
import com.dayflow.app.core.designsystem.DayflowTypography
import com.dayflow.app.core.designsystem.components.EmptyState
import com.dayflow.app.core.designsystem.components.PermissionBanner
import com.dayflow.app.core.designsystem.components.PriorityBadge
import com.dayflow.app.core.time.ReminderFormatter
import com.dayflow.app.feature.reminders.components.ReminderCard
import com.dayflow.app.scheduling.SchedulingCapability

@Composable
fun RemindersScreen(
    onNavigateToCreate: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onNavigateToDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RemindersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var reminderToDelete by remember { mutableStateOf<ReminderListItem?>(null) }
    var selectedSectionIndex by remember { mutableIntStateOf(0) } // 0: Active, 1: Paused

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DayflowTheme.colors.background)
        ) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DayflowTheme.spacing.screenHorizontal)
                    .padding(top = DayflowTheme.spacing.largeSection, bottom = DayflowTheme.spacing.sm)
            ) {
                Text(
                    text = "Reminders",
                    style = DayflowTypography.headlineLarge,
                    color = DayflowTheme.colors.textPrimary
                )
                Text(
                    text = "Keep commitments and build consistent cadence",
                    style = DayflowTypography.bodyMedium,
                    color = DayflowTheme.colors.textSecondary
                )
            }

            // Permission Banner (if exact alarm or notification permission absent)
            when (uiState.schedulingCapability) {
                SchedulingCapability.NOTIFICATIONS_DENIED -> {
                    PermissionBanner(
                        title = "Notifications Disabled",
                        description = "Notifications are disabled. DAYFLOW cannot alert you when commitments are due. Enable notifications in system settings.",
                        actionLabel = "Open Settings",
                        onActionClick = { openAppSettings(context, SchedulingCapability.NOTIFICATIONS_DENIED) },
                        modifier = Modifier.padding(horizontal = DayflowTheme.spacing.screenHorizontal, vertical = 6.dp)
                    )
                }
                SchedulingCapability.APPROXIMATE_FALLBACK -> {
                    PermissionBanner(
                        title = "Approximate Delivery Fallback",
                        description = "Exact alarms are disabled. Reminders will deliver approximately within a 10-15 minute window. Enable exact alarms for precision timing.",
                        actionLabel = "Enable Exact",
                        onActionClick = { openAppSettings(context, SchedulingCapability.APPROXIMATE_FALLBACK) },
                        modifier = Modifier.padding(horizontal = DayflowTheme.spacing.screenHorizontal, vertical = 6.dp)
                    )
                }
                SchedulingCapability.EXACT -> { /* All good */ }
            }

            // Upcoming Occurrences Ribbon (if available)
            if (uiState.upcomingOccurrences.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp)
                ) {
                    Text(
                        text = "UPCOMING OCCURRENCES",
                        style = DayflowTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = DayflowTheme.colors.textSecondary,
                        modifier = Modifier.padding(horizontal = DayflowTheme.spacing.screenHorizontal)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = DayflowTheme.spacing.screenHorizontal),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.upcomingOccurrences) { item ->
                            Card(
                                shape = DayflowTheme.shapes.secondaryCard,
                                colors = CardDefaults.cardColors(
                                    containerColor = DayflowTheme.colors.surfaceVariant,
                                    contentColor = DayflowTheme.colors.textPrimary
                                ),
                                border = BorderStroke(1.dp, DayflowTheme.colors.border),
                                modifier = Modifier
                                    .width(200.dp)
                                    .clickable { onNavigateToDetail(item.reminder.id) }
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = item.reminder.title,
                                            style = DayflowTypography.titleSmall,
                                            color = DayflowTheme.colors.textPrimary,
                                            maxLines = 1,
                                            modifier = Modifier.weight(1f)
                                        )
                                        PriorityBadge(priority = item.reminder.priority)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.Schedule,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp),
                                            tint = DayflowTheme.colors.primary
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = ReminderFormatter.formatOccurrenceTime(item.occurrence.scheduledAt, item.reminder.timeZoneId),
                                            style = DayflowTypography.labelSmall,
                                            color = DayflowTheme.colors.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Tab Row: Active vs Paused
            TabRow(
                selectedTabIndex = selectedSectionIndex,
                containerColor = DayflowTheme.colors.background,
                contentColor = DayflowTheme.colors.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedSectionIndex]),
                        color = DayflowTheme.colors.primary
                    )
                },
                modifier = Modifier.padding(horizontal = DayflowTheme.spacing.screenHorizontal, vertical = 6.dp)
            ) {
                Tab(
                    selected = selectedSectionIndex == 0,
                    onClick = { selectedSectionIndex = 0 },
                    text = {
                        Text(
                            text = "Active (${uiState.activeReminders.size})",
                            style = DayflowTypography.titleSmall,
                            color = if (selectedSectionIndex == 0) DayflowTheme.colors.primary else DayflowTheme.colors.textSecondary
                        )
                    }
                )
                Tab(
                    selected = selectedSectionIndex == 1,
                    onClick = { selectedSectionIndex = 1 },
                    text = {
                        Text(
                            text = "Paused (${uiState.pausedReminders.size})",
                            style = DayflowTypography.titleSmall,
                            color = if (selectedSectionIndex == 1) DayflowTheme.colors.primary else DayflowTheme.colors.textSecondary
                        )
                    }
                )
            }

            // Content List
            if (uiState.isLoading && uiState.activeReminders.isEmpty() && uiState.pausedReminders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = DayflowTheme.colors.primary)
                }
            } else {
                val currentList = if (selectedSectionIndex == 0) uiState.activeReminders else uiState.pausedReminders

                if (currentList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .padding(DayflowTheme.spacing.screenHorizontal),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedSectionIndex == 0) {
                            EmptyState(
                                title = "No active reminders",
                                message = "Schedule daily routines, interval water breaks, or one-time alerts to stay on track.",
                                icon = Icons.Outlined.NotificationsNone,
                                actionLabel = "Add Reminder",
                                onActionClick = onNavigateToCreate
                            )
                        } else {
                            EmptyState(
                                title = "No paused reminders",
                                message = "Active reminders you pause will be stored safely here without triggering alarms.",
                                icon = Icons.Outlined.PauseCircle
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentPadding = PaddingValues(
                            start = DayflowTheme.spacing.screenHorizontal,
                            end = DayflowTheme.spacing.screenHorizontal,
                            top = 8.dp,
                            bottom = 88.dp // Space for FAB
                        ),
                        verticalArrangement = Arrangement.spacedBy(DayflowTheme.spacing.cardSpacing)
                    ) {
                        items(currentList, key = { it.reminder.id }) { item ->
                            ReminderCard(
                                item = item,
                                onClick = { onNavigateToDetail(item.reminder.id) },
                                onEditClick = { onNavigateToEdit(item.reminder.id) },
                                onTogglePauseClick = {
                                    if (item.reminder.isPaused) {
                                        viewModel.resumeReminder(item.reminder.id)
                                    } else {
                                        viewModel.pauseReminder(item.reminder.id)
                                    }
                                },
                                onDeleteClick = { reminderToDelete = item }
                            )
                        }
                    }
                }
            }
        }

        // Accessible Floating Action Button
        FloatingActionButton(
            onClick = onNavigateToCreate,
            containerColor = DayflowTheme.colors.primary,
            contentColor = DayflowTheme.colors.background,
            shape = DayflowTheme.shapes.button,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Create new reminder"
            )
        }
    }

    // Delete Confirmation Dialog
    reminderToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { reminderToDelete = null },
            title = {
                Text(
                    text = "Delete Reminder?",
                    style = DayflowTypography.titleMedium,
                    color = DayflowTheme.colors.textPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete \"${item.reminder.title}\"? All future scheduled occurrences will be cancelled.",
                    style = DayflowTypography.bodyMedium,
                    color = DayflowTheme.colors.textSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteReminder(item.reminder.id)
                        reminderToDelete = null
                    }
                ) {
                    Text("Delete", color = DayflowTheme.colors.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { reminderToDelete = null }) {
                    Text("Cancel", color = DayflowTheme.colors.textPrimary)
                }
            },
            containerColor = DayflowTheme.colors.surfaceVariant
        )
    }
}

private fun openAppSettings(context: Context, capability: SchedulingCapability) {
    val intent = when (capability) {
        SchedulingCapability.NOTIFICATIONS_DENIED -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
            }
        }
        SchedulingCapability.APPROXIMATE_FALLBACK -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
            }
        }
        SchedulingCapability.EXACT -> null
    }

    intent?.let {
        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(it)
        } catch (_: Exception) {
            val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
        }
    }
}
