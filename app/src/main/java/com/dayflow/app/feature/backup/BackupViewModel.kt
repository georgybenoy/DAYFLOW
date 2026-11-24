package com.dayflow.app.feature.backup

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dayflow.app.backup.BackupEngine
import com.dayflow.app.backup.BackupService
import com.dayflow.app.backup.model.BackupRestoreError
import com.dayflow.app.backup.model.BackupSnapshotInfo
import com.dayflow.app.backup.model.RestoreResult
import com.dayflow.app.data.local.DataStoreKeys
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class BackupUiState(
    val isLoading: Boolean = false,
    val lastBackupTime: String? = null,
    val lastBackupStatus: String? = null,
    val lastSnapshotName: String? = null,
    val retentionCount: Int = BackupEngine.DEFAULT_RETENTION_COUNT,
    val snapshots: List<BackupSnapshotInfo> = emptyList(),
    val isCreatingBackup: Boolean = false,
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val isRestoring: Boolean = false,
    val pendingRestoreSnapshot: BackupSnapshotInfo? = null,
    val pendingDeleteSnapshot: BackupSnapshotInfo? = null,
    val restoreSuccessResult: RestoreResult.Success? = null,
    val restoreFailure: BackupRestoreError? = null,
    val actionMessage: String? = null,
    val actionError: String? = null
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupService: BackupService,
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState(isLoading = true))
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    init {
        observeDataStorePreferences()
        loadSnapshots()
    }

    private fun observeDataStorePreferences() {
        viewModelScope.launch {
            dataStore.data.collect { prefs ->
                _uiState.update { current ->
                    current.copy(
                        lastBackupTime = prefs[DataStoreKeys.LAST_BACKUP_TIME],
                        lastBackupStatus = prefs[DataStoreKeys.LAST_BACKUP_STATUS],
                        lastSnapshotName = prefs[DataStoreKeys.LAST_BACKUP_SNAPSHOT_NAME],
                        retentionCount = prefs[DataStoreKeys.BACKUP_RETENTION_COUNT] ?: BackupEngine.DEFAULT_RETENTION_COUNT
                    )
                }
            }
        }
    }

    fun loadSnapshots() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val snapshots = backupService.getSnapshots()
            _uiState.update { it.copy(snapshots = snapshots, isLoading = false) }
        }
    }

    fun createBackupNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingBackup = true, actionError = null, actionMessage = null) }
            val result = backupService.createBackupSnapshot()
            val snapshots = backupService.getSnapshots()
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isCreatingBackup = false,
                        snapshots = snapshots,
                        actionMessage = "Local snapshot created successfully"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isCreatingBackup = false,
                        snapshots = snapshots,
                        actionError = result.exceptionOrNull()?.message ?: "Backup creation failed"
                    )
                }
            }
        }
    }

    fun promptRestoreSnapshot(snapshot: BackupSnapshotInfo) {
        _uiState.update { it.copy(pendingRestoreSnapshot = snapshot) }
    }

    fun cancelRestorePrompt() {
        _uiState.update { it.copy(pendingRestoreSnapshot = null) }
    }

    fun confirmRestorePendingSnapshot() {
        val snapshot = _uiState.value.pendingRestoreSnapshot ?: return
        restoreSnapshot(snapshot.file)
    }

    fun promptDeleteSnapshot(snapshot: BackupSnapshotInfo) {
        _uiState.update { it.copy(pendingDeleteSnapshot = snapshot) }
    }

    fun cancelDeletePrompt() {
        _uiState.update { it.copy(pendingDeleteSnapshot = null) }
    }

    fun confirmDeletePendingSnapshot() {
        val snapshot = _uiState.value.pendingDeleteSnapshot ?: return
        viewModelScope.launch {
            backupService.deleteSnapshot(snapshot.filename)
            _uiState.update { it.copy(pendingDeleteSnapshot = null) }
            loadSnapshots()
        }
    }

    fun restoreSnapshot(snapshotFile: File) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRestoring = true,
                    pendingRestoreSnapshot = null,
                    actionError = null,
                    actionMessage = null,
                    restoreSuccessResult = null,
                    restoreFailure = null
                )
            }
            val result = backupService.restoreFromSnapshot(snapshotFile)
            val snapshots = backupService.getSnapshots()
            when (result) {
                is RestoreResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isRestoring = false,
                            snapshots = snapshots,
                            restoreSuccessResult = result,
                            actionMessage = "Restore completed successfully"
                        )
                    }
                }
                is RestoreResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isRestoring = false,
                            snapshots = snapshots,
                            restoreFailure = result.error
                        )
                    }
                }
            }
        }
    }

    fun exportToUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, actionError = null, actionMessage = null) }
            try {
                val outputStream = context.contentResolver.openOutputStream(uri)
                if (outputStream == null) {
                    _uiState.update { it.copy(isExporting = false, actionError = "Failed to access destination file") }
                    return@launch
                }
                val result = outputStream.use { stream ->
                    backupService.exportBackup(stream)
                }
                if (result.isSuccess) {
                    _uiState.update { it.copy(isExporting = false, actionMessage = "Backup archive exported successfully") }
                } else {
                    _uiState.update { it.copy(isExporting = false, actionError = result.exceptionOrNull()?.message ?: "Export failed") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isExporting = false, actionError = e.message ?: "Export error") }
            }
        }
    }

    fun importFromUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isImporting = true,
                    actionError = null,
                    actionMessage = null,
                    restoreSuccessResult = null,
                    restoreFailure = null
                )
            }
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    _uiState.update { it.copy(isImporting = false, actionError = "Failed to read backup file") }
                    return@launch
                }
                val size = try {
                    context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: -1L
                } catch (_: Exception) {
                    -1L
                }
                val result = inputStream.use { stream ->
                    backupService.importBackup(stream, size)
                }
                val snapshots = backupService.getSnapshots()
                when (result) {
                    is RestoreResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isImporting = false,
                                snapshots = snapshots,
                                restoreSuccessResult = result,
                                actionMessage = "Backup imported and restored successfully"
                            )
                        }
                    }
                    is RestoreResult.Failure -> {
                        _uiState.update {
                            it.copy(
                                isImporting = false,
                                snapshots = snapshots,
                                restoreFailure = result.error
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isImporting = false, actionError = e.message ?: "Import error") }
            }
        }
    }

    fun dismissResultDialog() {
        _uiState.update { it.copy(restoreSuccessResult = null, restoreFailure = null) }
    }

    fun clearActionNotification() {
        _uiState.update { it.copy(actionMessage = null, actionError = null) }
    }
}
