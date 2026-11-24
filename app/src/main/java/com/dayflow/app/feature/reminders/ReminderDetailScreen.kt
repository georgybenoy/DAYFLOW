package com.dayflow.app.feature.reminders

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Snooze
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dayflow.app.core.designsystem.DayflowTheme
import com.dayflow.app.core.designsystem.DayflowTypography
import com.dayflow.app.core.designsystem.components.EmptyState
import com.dayflow.app.core.designsystem.components.PriorityBadge
import com.dayflow.app.core.time.ReminderFormatter
import com.dayflow.app.domain.model.OccurrenceStatus
import com.dayflow.app.domain.model.ReminderPrecision

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderDetailScreen(
    reminderId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ReminderDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(reminderId) {
        viewModel.initForReminder(reminderId)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ReminderDetailEvent.ReminderDeleted -> onNavigateBack()
                is ReminderDetailEvent.ShowMessage -> { /* handle toast */ }
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
                        text = "Reminder Details",
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
                    if (uiState.reminder != null) {
                        IconButton(onClick = { onNavigateToEdit(reminderId) }) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Edit reminder",
                                tint = DayflowTheme.colors.primary
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
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = DayflowTheme.colors.primary)
            }
        } else if (uiState.reminder == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    title = "Reminder not found",
                    message = "This reminder may have been deleted or moved.",
                    icon = Icons.Outlined.NotificationsActive,
                    actionLabel = "Go Back",
                    onActionClick = onNavigateBack
                )
            }
        } else {
            val reminder = uiState.reminder!!

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = DayflowTheme.spacing.screenHorizontal),
                verticalArrangement = Arrangement.spacedBy(DayflowTheme.spacing.sectionSpacing)
            ) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    // Header Card
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
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = reminder.title,
                                style = DayflowTypography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = DayflowTheme.colors.textPrimary
                            )

                            if (!reminder.description.isNullOrBlank()) {
                                Text(
                                    text = reminder.description,
                                    style = DayflowTypography.bodyMedium,
                                    color = DayflowTheme.colors.textSecondary
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = DayflowTheme.colors.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = uiState.recurrenceSummary,
                                    style = DayflowTypography.bodyMedium,
                                    color = DayflowTheme.colors.primary
                                )
                            }

                            // Badges row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PriorityBadge(priority = reminder.priority)

                                val (precisionText, precisionColor) = if (reminder.precision == ReminderPrecision.EXACT) {
                                    "Exact" to DayflowTheme.colors.primary
                                } else {
                                    "Approx." to DayflowTheme.colors.warning
                                }

                                Box(
                                    modifier = Modifier
                                        .background(precisionColor.copy(alpha = 0.12f), DayflowTheme.shapes.chip)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = precisionText,
                                        style = DayflowTypography.labelSmall,
                                        color = precisionColor
                                    )
                                }

                                if (uiState.category != null) {
                                    Box(
                                        modifier = Modifier
                                            .background(DayflowTheme.colors.surfaceElevated, DayflowTheme.shapes.chip)
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = uiState.category!!.name,
                                            style = DayflowTypography.labelSmall,
                                            color = DayflowTheme.colors.textSecondary
                                        )
                                    }
                                }

                                if (reminder.isPaused) {
                                    Box(
                                        modifier = Modifier
                                            .background(DayflowTheme.colors.warning.copy(alpha = 0.15f), DayflowTheme.shapes.chip)
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Paused",
                                            style = DayflowTypography.labelSmall,
                                            color = DayflowTheme.colors.warning
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Action Bar (Pause/Resume & Delete)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.togglePause() },
                            modifier = Modifier.weight(1f),
                            shape = DayflowTheme.shapes.button,
                            border = BorderStroke(1.dp, DayflowTheme.colors.border)
                        ) {
                            Icon(
                                imageVector = if (reminder.isPaused) Icons.Outlined.PlayCircle else Icons.Outlined.PauseCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = DayflowTheme.colors.textPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (reminder.isPaused) "Resume" else "Pause",
                                style = DayflowTypography.labelMedium,
                                color = DayflowTheme.colors.textPrimary
                            )
                        }

                        OutlinedButton(
                            onClick = { showDeleteConfirmDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = DayflowTheme.shapes.button,
                            border = BorderStroke(1.dp, DayflowTheme.colors.error.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = DayflowTheme.colors.error
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Delete",
                                style = DayflowTypography.labelMedium,
                                color = DayflowTheme.colors.error
                            )
                        }
                    }
                }

                // Active Occurrence Action Card (if active)
                if (uiState.activeOccurrence != null) {
                    item {
                        val activeOcc = uiState.activeOccurrence!!
                        Card(
                            shape = DayflowTheme.shapes.mainCard,
                            colors = CardDefaults.cardColors(
                                containerColor = DayflowTheme.colors.surfaceVariant,
                                contentColor = DayflowTheme.colors.textPrimary
                            ),
                            border = BorderStroke(1.dp, DayflowTheme.colors.primary.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(DayflowTheme.spacing.cardPadding),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "ACTIVE OCCURRENCE",
                                    style = DayflowTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = DayflowTheme.colors.primary
                                )
                                Text(
                                    text = "Scheduled: ${ReminderFormatter.formatOccurrenceTime(activeOcc.scheduledAt, reminder.timeZoneId)}",
                                    style = DayflowTypography.bodyMedium,
                                    color = DayflowTheme.colors.textPrimary
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.completeActiveOccurrence(activeOcc.id) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = DayflowTheme.colors.primary,
                                            contentColor = DayflowTheme.colors.background
                                        ),
                                        shape = DayflowTheme.shapes.button,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Complete", style = DayflowTypography.labelSmall)
                                    }

                                    OutlinedButton(
                                        onClick = { viewModel.snoozeActiveOccurrence(activeOcc.id, 10L) },
                                        shape = DayflowTheme.shapes.button,
                                        border = BorderStroke(1.dp, DayflowTheme.colors.border),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Outlined.Snooze, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Snooze", style = DayflowTypography.labelSmall)
                                    }

                                    OutlinedButton(
                                        onClick = { viewModel.skipActiveOccurrence(activeOcc.id) },
                                        shape = DayflowTheme.shapes.button,
                                        border = BorderStroke(1.dp, DayflowTheme.colors.border),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Outlined.FastForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Skip", style = DayflowTypography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }

                // Section: Occurrence History
                item {
                    Text(
                        text = "OCCURRENCE HISTORY",
                        style = DayflowTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = DayflowTheme.colors.textSecondary
                    )
                }

                if (uiState.occurrenceHistory.isEmpty()) {
                    item {
                        Card(
                            shape = DayflowTheme.shapes.secondaryCard,
                            colors = CardDefaults.cardColors(containerColor = DayflowTheme.colors.surface),
                            border = BorderStroke(1.dp, DayflowTheme.colors.border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "No recorded occurrences yet.",
                                    style = DayflowTypography.bodySmall,
                                    color = DayflowTheme.colors.textSecondary
                                )
                            }
                        }
                    }
                } else {
                    items(uiState.occurrenceHistory, key = { it.occurrence.id }) { item ->
                        Card(
                            shape = DayflowTheme.shapes.secondaryCard,
                            colors = CardDefaults.cardColors(
                                containerColor = DayflowTheme.colors.surface,
                                contentColor = DayflowTheme.colors.textPrimary
                            ),
                            border = BorderStroke(1.dp, DayflowTheme.colors.border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = item.formattedTime,
                                        style = DayflowTypography.titleSmall,
                                        color = DayflowTheme.colors.textPrimary
                                    )
                                    if (item.occurrence.handledAt != null) {
                                        Text(
                                            text = "Handled: ${ReminderFormatter.formatOccurrenceTime(item.occurrence.handledAt, reminder.timeZoneId)}",
                                            style = DayflowTypography.labelSmall,
                                            color = DayflowTheme.colors.textMuted
                                        )
                                    }
                                }

                                val (statusBg, statusText) = when (item.occurrence.status) {
                                    OccurrenceStatus.COMPLETED -> DayflowTheme.colors.primary.copy(alpha = 0.15f) to DayflowTheme.colors.primary
                                    OccurrenceStatus.SNOOZED -> DayflowTheme.colors.warning.copy(alpha = 0.15f) to DayflowTheme.colors.warning
                                    OccurrenceStatus.DELIVERED -> DayflowTheme.colors.primary.copy(alpha = 0.10f) to DayflowTheme.colors.textPrimary
                                    OccurrenceStatus.SCHEDULED -> DayflowTheme.colors.surfaceElevated to DayflowTheme.colors.primary
                                    OccurrenceStatus.SKIPPED -> DayflowTheme.colors.surfaceElevated to DayflowTheme.colors.textSecondary
                                    OccurrenceStatus.MISSED -> DayflowTheme.colors.error.copy(alpha = 0.15f) to DayflowTheme.colors.error
                                    OccurrenceStatus.CANCELLED -> DayflowTheme.colors.surfaceElevated to DayflowTheme.colors.textMuted
                                }

                                Box(
                                    modifier = Modifier
                                        .background(statusBg, DayflowTheme.shapes.chip)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = item.statusText,
                                        style = DayflowTypography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                        color = statusText
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = "Delete Reminder?",
                    style = DayflowTypography.titleMedium,
                    color = DayflowTheme.colors.textPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete this reminder? All occurrences and pending alarms will be cancelled.",
                    style = DayflowTypography.bodyMedium,
                    color = DayflowTheme.colors.textSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteReminder()
                        showDeleteConfirmDialog = false
                    }
                ) {
                    Text("Delete", color = DayflowTheme.colors.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = DayflowTheme.colors.textPrimary)
                }
            },
            containerColor = DayflowTheme.colors.surfaceVariant
        )
    }
}
