package com.dayflow.app.backup

import com.dayflow.app.backup.model.BackupCategory
import com.dayflow.app.backup.model.BackupPayload
import com.dayflow.app.backup.model.BackupRestoreError
import com.dayflow.app.backup.model.RestoreResult
import com.dayflow.app.data.local.dao.BackupDao
import com.dayflow.app.domain.usecase.RescheduleAllUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

class BackupRestoreTransactionTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val backupDao = mockk<BackupDao>(relaxed = true)
    private val rescheduleAllUseCase = mockk<RescheduleAllUseCase>(relaxed = true)

    private fun createValidArchiveStream(): ByteArrayInputStream {
        val payload = BackupPayload(
            schemaVersion = 1,
            appVersion = "1.0.0",
            createdAt = 1700000000000L,
            categories = listOf(
                BackupCategory(
                    id = "cat-1",
                    name = "Work",
                    colorHex = "#3B82F6",
                    iconName = "work",
                    isDefault = true,
                    createdAtEpochMillis = 1700000000000L
                )
            ),
            preferences = mapOf("user_name" to "Bob")
        )
        val baos = ByteArrayOutputStream()
        BackupEngine.writeBackupToStream(payload, baos)
        return ByteArrayInputStream(baos.toByteArray())
    }

    @Test
    fun `successful restore executes safety snapshot, transaction, settings, and alarms reconciliation`() = runTest {
        val backupsDir = tempFolder.newFolder("backups")
        val stream = createValidArchiveStream()

        var safetySnapshotCalled = false
        var settingsRestored: Map<String, String>? = null

        val result = BackupEngine.executeRestorePipeline(
            archiveStream = stream,
            archiveSize = -1,
            backupDao = backupDao,
            currentSettings = mapOf("user_name" to "Alice"),
            onSaveSafetySnapshot = {
                safetySnapshotCalled = true
            },
            onRestoreSettings = { settings ->
                settingsRestored = settings
            },
            rescheduleAllUseCase = rescheduleAllUseCase,
            backupsDir = backupsDir
        )

        assertTrue("Restore should succeed", result is RestoreResult.Success)
        val success = result as RestoreResult.Success
        assertEquals(1, success.totalRestoredRecords)

        assertTrue("Safety snapshot must be taken before DB changes", safetySnapshotCalled)
        assertEquals("Bob", settingsRestored?.get("user_name"))

        // Verify Room transaction was executed
        coVerify(exactly = 1) {
            backupDao.restoreAllDataTransaction(
                categories = any(),
                reminders = any(),
                occurrences = any(),
                tasks = any(),
                subtasks = any(),
                habits = any(),
                habitLogs = any(),
                dailyPlans = any(),
                planItems = any(),
                reviews = any(),
                appSettings = any()
            )
        }

        // Verify RescheduleAllUseCase was invoked (PRD Example 6)
        coVerify(exactly = 1) { rescheduleAllUseCase(any()) }
    }

    @Test
    fun `safety snapshot failure aborts restore and leaves database untouched`() = runTest {
        val backupsDir = tempFolder.newFolder("backups_safety_fail")
        val stream = createValidArchiveStream()

        val result = BackupEngine.executeRestorePipeline(
            archiveStream = stream,
            archiveSize = -1,
            backupDao = backupDao,
            currentSettings = emptyMap(),
            onSaveSafetySnapshot = {
                throw IOException("Disk space exhausted")
            },
            onRestoreSettings = {},
            rescheduleAllUseCase = rescheduleAllUseCase,
            backupsDir = backupsDir
        )

        assertTrue(result is RestoreResult.Failure)
        val failure = result as RestoreResult.Failure
        assertTrue(failure.error is BackupRestoreError.SafetySnapshotFailed)

        // Database transaction was never invoked
        coVerify(exactly = 0) {
            backupDao.restoreAllDataTransaction(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        }
        coVerify(exactly = 0) { rescheduleAllUseCase() }
    }

    @Test
    fun `database transaction failure rolls back and returns DatabaseTransactionFailed`() = runTest {
        val backupsDir = tempFolder.newFolder("backups_db_fail")
        val stream = createValidArchiveStream()

        coEvery {
            backupDao.restoreAllDataTransaction(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } throws RuntimeException("SQLite constraint failed")

        val result = BackupEngine.executeRestorePipeline(
            archiveStream = stream,
            archiveSize = -1,
            backupDao = backupDao,
            currentSettings = emptyMap(),
            onSaveSafetySnapshot = {},
            onRestoreSettings = {},
            rescheduleAllUseCase = rescheduleAllUseCase,
            backupsDir = backupsDir
        )

        assertTrue(result is RestoreResult.Failure)
        val failure = result as RestoreResult.Failure
        assertTrue(failure.error is BackupRestoreError.DatabaseTransactionFailed)
        assertNotNull(failure.error.cause)

        // Alarms reconciliation must NOT run if DB transaction failed
        coVerify(exactly = 0) { rescheduleAllUseCase() }
    }
}
