package com.dayflow.app.feature.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.WarningAmber
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dayflow.app.backup.model.BackupRestoreError
import com.dayflow.app.backup.model.BackupSnapshotInfo
import com.dayflow.app.backup.model.RestoreResult
import com.dayflow.app.core.designsystem.DayflowTheme
import com.dayflow.app.core.designsystem.DayflowTypography
import com.dayflow.app.core.designsystem.components.DayflowScaffold
import com.dayflow.app.core.time.DateTimeFormatters
import java.time.Instant
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BackupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = DayflowTheme.colors
    val spacing = DayflowTheme.spacing
    val shapes = DayflowTheme.shapes
    val snackbarHostState = remember { SnackbarHostState() }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportToUri(it) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importFromUri(it) }
    }

    LaunchedEffect(uiState.actionMessage) {
        uiState.actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearActionNotification()
        }
    }

    LaunchedEffect(uiState.actionError) {
        uiState.actionError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearActionNotification()
        }
    }

    DayflowScaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Backup & Restore",
                        style = DayflowTypography.titleLarge,
                        color = colors.textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background,
                    titleContentColor = colors.textPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(colors.background)
                .padding(horizontal = spacing.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            item { Spacer(modifier = Modifier.height(spacing.xs)) }

            // Automatic Backup & Status Card
            item {
                BackupStatusCard(
                    lastBackupTime = uiState.lastBackupTime,
                    lastBackupStatus = uiState.lastBackupStatus,
                    retentionCount = uiState.retentionCount
                )
            }

            // Quick Actions: Backup Now, Export, Import
            item {
                BackupActionsCard(
                    isCreatingBackup = uiState.isCreatingBackup,
                    isExporting = uiState.isExporting,
                    isImporting = uiState.isImporting,
                    onCreateBackup = viewModel::createBackupNow,
                    onExportBackup = {
                        val timestamp = System.currentTimeMillis()
                        exportLauncher.launch("dayflow_backup_$timestamp.zip")
                    },
                    onImportBackup = {
                        importLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream", "*/*"))
                    }
                )
            }

            // Honest Disclaimer Card
            item {
                HonestDisclaimerCard()
            }

            // Local Snapshots Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Local Snapshots (${uiState.snapshots.size})",
                        style = DayflowTypography.titleMedium,
                        color = colors.textPrimary
                    )
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = colors.primary,
                            strokeWidth = 2.dp
                        )
                    }
                }
            }

            // Snapshot List Items or Empty State
            if (uiState.snapshots.isEmpty() && !uiState.isLoading) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = shapes.secondaryCard,
                        colors = CardDefaults.cardColors(
                            containerColor = colors.surface,
                            contentColor = colors.textSecondary
                        ),
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(spacing.cardPadding),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Backup,
                                contentDescription = null,
                                tint = colors.textMuted,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(spacing.sm))
                            Text(
                                text = "No local snapshots yet",
                                style = DayflowTypography.bodyMedium,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(spacing.xs))
                            Text(
                                text = "Automatic backups occur every 24 hours. You can also tap 'Backup Now' above to create one immediately.",
                                style = DayflowTypography.bodySmall,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(horizontal = spacing.md)
                            )
                        }
                    }
                }
            } else {
                items(uiState.snapshots, key = { it.filename }) { snapshot ->
                    SnapshotItemCard(
                        snapshot = snapshot,
                        onRestore = { viewModel.promptRestoreSnapshot(snapshot) },
                        onDelete = { viewModel.promptDeleteSnapshot(snapshot) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(spacing.xxl)) }
        }

        // Restore Confirmation Dialog
        uiState.pendingRestoreSnapshot?.let { snapshot ->
            AlertDialog(
                onDismissRequest = viewModel::cancelRestorePrompt,
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.WarningAmber,
                        contentDescription = null,
                        tint = colors.warning
                    )
                },
                title = {
                    Text(
                        text = "Restore Snapshot?",
                        style = DayflowTypography.titleMedium,
                        color = colors.textPrimary
                    )
                },
                text = {
                    Text(
                        text = "Restoring will replace all current tasks, reminders, habits, and daily plans with the contents of ${snapshot.filename}.\n\n" +
                            "A safety snapshot of your current data will be created automatically before restoring so no data is lost.",
                        style = DayflowTypography.bodySmall,
                        color = colors.textSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = viewModel::confirmRestorePendingSnapshot,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = colors.background
                        )
                    ) {
                        Text("Restore")
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::cancelRestorePrompt) {
                        Text("Cancel", color = colors.textSecondary)
                    }
                },
                containerColor = colors.surfaceElevated
            )
        }

        // Delete Confirmation Dialog
        uiState.pendingDeleteSnapshot?.let { snapshot ->
            AlertDialog(
                onDismissRequest = viewModel::cancelDeletePrompt,
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = colors.error
                    )
                },
                title = {
                    Text(
                        text = "Delete Snapshot?",
                        style = DayflowTypography.titleMedium,
                        color = colors.textPrimary
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to delete ${snapshot.filename}?",
                        style = DayflowTypography.bodySmall,
                        color = colors.textSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = viewModel::confirmDeletePendingSnapshot,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.error,
                            contentColor = colors.background
                        )
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::cancelDeletePrompt) {
                        Text("Cancel", color = colors.textSecondary)
                    }
                },
                containerColor = colors.surfaceElevated
            )
        }

        // Restore Success Dialog
        uiState.restoreSuccessResult?.let { success ->
            AlertDialog(
                onDismissRequest = viewModel::dismissResultDialog,
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = colors.success
                    )
                },
                title = {
                    Text(
                        text = "Restore Completed",
                        style = DayflowTypography.titleMedium,
                        color = colors.textPrimary
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                        Text(
                            text = "Successfully restored ${success.totalRestoredRecords} total records from snapshot created on ${DateTimeFormatters.formatDateTime(success.snapshotDate)}.",
                            style = DayflowTypography.bodySmall,
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(spacing.xs))
                        Text(
                            text = "Restored summary:",
                            style = DayflowTypography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        success.counts.forEach { (entity, count) ->
                            Text(
                                text = "• ${entity.replaceFirstChar { it.titlecase(Locale.getDefault()) }}: $count",
                                style = DayflowTypography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = viewModel::dismissResultDialog,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = colors.background
                        )
                    ) {
                        Text("OK")
                    }
                },
                containerColor = colors.surfaceElevated
            )
        }

        // Restore Failure Dialog
        uiState.restoreFailure?.let { error ->
            val errorDescription = when (error) {
                is BackupRestoreError.InvalidZipArchive -> "The archive file is corrupted or not a valid ZIP: ${error.details}"
                is BackupRestoreError.ZipBombDetected -> "Archive exceeded uncompressed size limit or maximum compression ratio (${error.uncompressedSize / (1024L * 1024L)}MB / ${error.limit / (1024L * 1024L)}MB)."
                is BackupRestoreError.ChecksumMismatch -> "Integrity check failed: Archive SHA-256 hash does not match manifest."
                is BackupRestoreError.UnsupportedSchemaVersion -> "Unsupported schema version ${error.version}. Maximum supported version is ${error.maxSupported}."
                is BackupRestoreError.InvalidRecordData -> "Malformed data in entity ${error.recordType} (id=${error.recordId}): ${error.reason}"
                is BackupRestoreError.BrokenReferenceConstraint -> "Referential integrity broken: ${error.details}"
                is BackupRestoreError.SafetySnapshotFailed -> "Could not save pre-restore safety snapshot (${error.reason}). Live database was left untouched."
                is BackupRestoreError.DatabaseTransactionFailed -> "Database transaction failed during restore. All changes were rolled back safely: ${error.causeException.message}"
                is BackupRestoreError.IoError -> "Storage I/O failure: ${error.messageText}"
            }

            AlertDialog(
                onDismissRequest = viewModel::dismissResultDialog,
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.WarningAmber,
                        contentDescription = null,
                        tint = colors.error
                    )
                },
                title = {
                    Text(
                        text = "Restore Failed",
                        style = DayflowTypography.titleMedium,
                        color = colors.textPrimary
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                        Text(
                            text = errorDescription,
                            style = DayflowTypography.bodySmall,
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(spacing.xs))
                        Text(
                            text = "Safety Guarantee: Your existing data was not modified.",
                            style = DayflowTypography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.success
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = viewModel::dismissResultDialog,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.surfaceVariant,
                            contentColor = colors.textPrimary
                        )
                    ) {
                        Text("Dismiss")
                    }
                },
                containerColor = colors.surfaceElevated
            )
        }
    }
}

@Composable
private fun BackupStatusCard(
    lastBackupTime: String?,
    lastBackupStatus: String?,
    retentionCount: Int
) {
    val colors = DayflowTheme.colors
    val spacing = DayflowTheme.spacing
    val shapes = DayflowTheme.shapes

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shapes.mainCard,
        colors = CardDefaults.cardColors(
            containerColor = colors.surfaceElevated,
            contentColor = colors.textPrimary
        ),
        border = BorderStroke(1.dp, colors.border)
    ) {
        Column(modifier = Modifier.padding(spacing.cardPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Automatic Snapshots",
                    style = DayflowTypography.titleMedium,
                    color = colors.textPrimary
                )
                val statusText = when {
                    lastBackupStatus == null -> "IDLE"
                    lastBackupStatus.startsWith("SUCCESS") -> "ACTIVE"
                    else -> "ERROR"
                }
                val statusColor = when {
                    lastBackupStatus == null -> colors.textMuted
                    lastBackupStatus.startsWith("SUCCESS") -> colors.primary
                    else -> colors.error
                }
                Text(
                    text = statusText,
                    style = DayflowTypography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }

            Spacer(modifier = Modifier.height(spacing.xs))

            val formattedTime = lastBackupTime?.let {
                try {
                    DateTimeFormatters.formatDateTime(Instant.parse(it))
                } catch (_: Exception) {
                    it
                }
            } ?: "No backups created yet"

            Text(
                text = "Last backup: $formattedTime",
                style = DayflowTypography.bodySmall,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(spacing.xs))

            Text(
                text = "Cadence: Every 24 hours (WorkManager) • Retention: $retentionCount daily snapshots",
                style = DayflowTypography.bodySmall,
                color = colors.textMuted
            )
        }
    }
}

@Composable
private fun BackupActionsCard(
    isCreatingBackup: Boolean,
    isExporting: Boolean,
    isImporting: Boolean,
    onCreateBackup: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit
) {
    val colors = DayflowTheme.colors
    val spacing = DayflowTheme.spacing
    val shapes = DayflowTheme.shapes

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shapes.secondaryCard,
        colors = CardDefaults.cardColors(
            containerColor = colors.surface,
            contentColor = colors.textPrimary
        ),
        border = BorderStroke(1.dp, colors.border)
    ) {
        Column(modifier = Modifier.padding(spacing.cardPadding)) {
            Text(
                text = "Backup & Export Actions",
                style = DayflowTypography.titleSmall,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(spacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                Button(
                    onClick = onCreateBackup,
                    enabled = !isCreatingBackup && !isExporting && !isImporting,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.background
                    )
                ) {
                    if (isCreatingBackup) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = colors.background,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Backup,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(spacing.xs))
                        Text("Backup Now")
                    }
                }

                OutlinedButton(
                    onClick = onExportBackup,
                    enabled = !isCreatingBackup && !isExporting && !isImporting,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = colors.textPrimary
                    ),
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = colors.textPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.FileUpload,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(spacing.xs))
                        Text("Export")
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing.sm))

            OutlinedButton(
                onClick = onImportBackup,
                enabled = !isCreatingBackup && !isExporting && !isImporting,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = colors.textPrimary
                ),
                border = BorderStroke(1.dp, colors.border)
            ) {
                if (isImporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = colors.textPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.FileDownload,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(spacing.xs))
                    Text("Import Archive from File")
                }
            }
        }
    }
}

@Composable
private fun HonestDisclaimerCard() {
    val colors = DayflowTheme.colors
    val spacing = DayflowTheme.spacing
    val shapes = DayflowTheme.shapes

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shapes.secondaryCard,
        colors = CardDefaults.cardColors(
            containerColor = colors.surfaceElevated,
            contentColor = colors.textPrimary
        ),
        border = BorderStroke(1.dp, colors.border)
    ) {
        Row(
            modifier = Modifier.padding(spacing.cardPadding),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(spacing.sm))
            Column {
                Text(
                    text = "Storage Security Notice",
                    style = DayflowTypography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary
                )
                Spacer(modifier = Modifier.height(spacing.xs))
                Text(
                    text = "Local snapshots are saved inside app-private storage. They do not survive application uninstall or device loss. To keep your data safe across devices, tap 'Export' to save snapshots to external storage or cloud drive.",
                    style = DayflowTypography.bodySmall,
                    color = colors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun SnapshotItemCard(
    snapshot: BackupSnapshotInfo,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = DayflowTheme.colors
    val spacing = DayflowTheme.spacing
    val shapes = DayflowTheme.shapes

    val sizeFormatted = if (snapshot.sizeBytes >= 1024 * 1024) {
        String.format(Locale.getDefault(), "%.2f MB", snapshot.sizeBytes / (1024f * 1024f))
    } else {
        String.format(Locale.getDefault(), "%.1f KB", snapshot.sizeBytes / 1024f)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shapes.secondaryCard,
        colors = CardDefaults.cardColors(
            containerColor = colors.surface,
            contentColor = colors.textPrimary
        ),
        border = BorderStroke(1.dp, colors.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.md, vertical = spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = DateTimeFormatters.formatDateTime(snapshot.createdAt),
                    style = DayflowTypography.titleSmall,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(spacing.xs))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = sizeFormatted,
                        style = DayflowTypography.bodySmall,
                        color = colors.textMuted
                    )
                    snapshot.recordsCount?.let { count ->
                        Text(
                            text = "• $count records",
                            style = DayflowTypography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                IconButton(onClick = onRestore) {
                    Icon(
                        imageVector = Icons.Outlined.Restore,
                        contentDescription = "Restore Snapshot",
                        tint = colors.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete Snapshot",
                        tint = colors.textMuted
                    )
                }
            }
        }
    }
}
