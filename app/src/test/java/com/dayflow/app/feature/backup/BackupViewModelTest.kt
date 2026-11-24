package com.dayflow.app.feature.backup

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.preferencesOf
import com.dayflow.app.backup.BackupService
import com.dayflow.app.backup.model.BackupRestoreError
import com.dayflow.app.backup.model.BackupSnapshotInfo
import com.dayflow.app.backup.model.RestoreResult
import com.dayflow.app.data.local.DataStoreKeys
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class BackupViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val context = mockk<Context>(relaxed = true)
    private val backupService = mockk<BackupService>(relaxed = true)
    private val dataStore = mockk<DataStore<Preferences>>(relaxed = true)

    private val testSnapshot = BackupSnapshotInfo(
        filename = "dayflow_backup_1700000000000.zip",
        file = File("dayflow_backup_1700000000000.zip"),
        sizeBytes = 2048L,
        createdAt = Instant.ofEpochMilli(1700000000000L),
        recordsCount = 15
    )

    private lateinit var viewModel: BackupViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        val prefs = preferencesOf(
            DataStoreKeys.LAST_BACKUP_TIME to "2026-11-19T00:00:00Z",
            DataStoreKeys.LAST_BACKUP_STATUS to "SUCCESS",
            DataStoreKeys.LAST_BACKUP_SNAPSHOT_NAME to "dayflow_backup_1700000000000.zip",
            DataStoreKeys.BACKUP_RETENTION_COUNT to 7
        )
        every { dataStore.data } returns flowOf(prefs)
        coEvery { backupService.getSnapshots() } returns listOf(testSnapshot)

        viewModel = BackupViewModel(
            context = context,
            backupService = backupService,
            dataStore = dataStore
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loads preferences and snapshots list`() = runTest(testDispatcher) {
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("2026-11-19T00:00:00Z", state.lastBackupTime)
        assertEquals("SUCCESS", state.lastBackupStatus)
        assertEquals("dayflow_backup_1700000000000.zip", state.lastSnapshotName)
        assertEquals(7, state.retentionCount)
        assertEquals(1, state.snapshots.size)
        assertEquals(testSnapshot.filename, state.snapshots.first().filename)
        assertFalse(state.isLoading)
    }

    @Test
    fun `createBackupNow executes snapshot creation and refreshes snapshots`() = runTest(testDispatcher) {
        coEvery { backupService.createBackupSnapshot() } returns Result.success(testSnapshot)

        viewModel.createBackupNow()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isCreatingBackup)
        assertEquals("Local snapshot created successfully", state.actionMessage)
        assertNull(state.actionError)
        coVerify(exactly = 1) { backupService.createBackupSnapshot() }
    }

    @Test
    fun `promptRestoreSnapshot sets pendingRestoreSnapshot and confirm triggers restore`() = runTest(testDispatcher) {
        val successResult = RestoreResult.Success(
            snapshotDate = Instant.now(),
            totalRestoredRecords = 15,
            counts = mapOf("tasks" to 10, "habits" to 5)
        )
        coEvery { backupService.restoreFromSnapshot(testSnapshot.file) } returns successResult

        viewModel.promptRestoreSnapshot(testSnapshot)
        assertEquals(testSnapshot, viewModel.uiState.value.pendingRestoreSnapshot)

        viewModel.confirmRestorePendingSnapshot()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.pendingRestoreSnapshot)
        assertFalse(state.isRestoring)
        assertEquals(successResult, state.restoreSuccessResult)
        assertNull(state.restoreFailure)

        coVerify(exactly = 1) { backupService.restoreFromSnapshot(testSnapshot.file) }
    }

    @Test
    fun `cancelRestorePrompt clears pending restore snapshot`() {
        viewModel.promptRestoreSnapshot(testSnapshot)
        assertEquals(testSnapshot, viewModel.uiState.value.pendingRestoreSnapshot)

        viewModel.cancelRestorePrompt()
        assertNull(viewModel.uiState.value.pendingRestoreSnapshot)
    }

    @Test
    fun `restoreSnapshot failure exposes restoreFailure in uiState`() = runTest(testDispatcher) {
        val failureError = BackupRestoreError.ChecksumMismatch(expected = "abc", actual = "xyz")
        coEvery { backupService.restoreFromSnapshot(testSnapshot.file) } returns RestoreResult.Failure(failureError)

        viewModel.restoreSnapshot(testSnapshot.file)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isRestoring)
        assertNull(state.restoreSuccessResult)
        assertEquals(failureError, state.restoreFailure)
    }

    @Test
    fun `promptDeleteSnapshot and confirmDeletePendingSnapshot calls deleteSnapshot`() = runTest(testDispatcher) {
        coEvery { backupService.deleteSnapshot(testSnapshot.filename) } returns true

        viewModel.promptDeleteSnapshot(testSnapshot)
        assertEquals(testSnapshot, viewModel.uiState.value.pendingDeleteSnapshot)

        viewModel.confirmDeletePendingSnapshot()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.pendingDeleteSnapshot)
        coVerify(exactly = 1) { backupService.deleteSnapshot(testSnapshot.filename) }
    }

    @Test
    fun `dismissResultDialog clears success and failure dialog states`() {
        viewModel.promptRestoreSnapshot(testSnapshot)
        viewModel.dismissResultDialog()
        assertNull(viewModel.uiState.value.restoreSuccessResult)
        assertNull(viewModel.uiState.value.restoreFailure)
    }
}
