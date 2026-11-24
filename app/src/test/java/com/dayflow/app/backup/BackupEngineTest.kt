package com.dayflow.app.backup

import com.dayflow.app.backup.model.BackupAppSetting
import com.dayflow.app.backup.model.BackupCategory
import com.dayflow.app.backup.model.BackupDailyPlan
import com.dayflow.app.backup.model.BackupDailyPlanItem
import com.dayflow.app.backup.model.BackupDailyReview
import com.dayflow.app.backup.model.BackupHabit
import com.dayflow.app.backup.model.BackupHabitLog
import com.dayflow.app.backup.model.BackupPayload
import com.dayflow.app.backup.model.BackupReminder
import com.dayflow.app.backup.model.BackupReminderOccurrence
import com.dayflow.app.backup.model.BackupRestoreError
import com.dayflow.app.backup.model.BackupTask
import com.dayflow.app.backup.model.BackupTaskSubtask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BackupEngineTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun createSamplePayload(): BackupPayload {
        val cat = BackupCategory(
            id = "cat-1",
            name = "Health",
            colorHex = "#22C55E",
            iconName = "fitness",
            isDefault = true,
            createdAtEpochMillis = 1700000000000L
        )

        val rem = BackupReminder(
            id = "rem-1",
            title = "Drink Water",
            description = "Hydration check",
            categoryId = "cat-1",
            type = "DAILY",
            startDate = "2026-11-19",
            startTime = "08:00",
            intervalMinutes = 120,
            activeDaysMask = 127,
            endTime = "22:00",
            endDate = null,
            priority = "MEDIUM",
            precision = "APPROXIMATE",
            isEnabled = true,
            isPaused = false,
            timeZoneId = "UTC",
            anchorDate = null,
            anchorTime = null,
            lastCompletedAtEpochMillis = null,
            createdAtEpochMillis = 1700000000000L,
            updatedAtEpochMillis = 1700000000000L
        )

        val occ = BackupReminderOccurrence(
            id = "occ-1",
            reminderId = "rem-1",
            scheduledAtEpochMillis = 1700003600000L,
            status = "SCHEDULED",
            snoozedUntilEpochMillis = null,
            deliveredAtEpochMillis = null,
            handledAtEpochMillis = null,
            alarmRequestCode = 101
        )

        val task = BackupTask(
            id = "task-1",
            title = "Write Architecture Documentation",
            description = "Document backup pipeline",
            categoryId = "cat-1",
            priority = "HIGH",
            deadlineDate = "2026-11-20",
            deadlineTime = "18:00",
            estimatedDurationMinutes = 45,
            plannedDate = "2026-11-19",
            plannedStart = "10:00",
            plannedEnd = "10:45",
            status = "PENDING",
            shouldCarryForward = true,
            createdAtEpochMillis = 1700000000000L,
            completedAtEpochMillis = null,
            updatedAtEpochMillis = 1700000000000L
        )

        val subtask = BackupTaskSubtask(
            id = "sub-1",
            taskId = "task-1",
            title = "Diagram 10-step restore flow",
            isCompleted = false,
            orderIndex = 0,
            createdAtEpochMillis = 1700000000000L
        )

        val habit = BackupHabit(
            id = "habit-1",
            name = "Morning Meditation",
            description = "Mindfulness",
            categoryId = "cat-1",
            trackingType = "BINARY",
            targetValue = 1.0,
            unit = null,
            activeDaysMask = 127,
            reminderPreference = false,
            reminderTime = "07:30",
            isEnabled = true,
            createdAtEpochMillis = 1700000000000L,
            updatedAtEpochMillis = 1700000000000L
        )

        val log = BackupHabitLog(
            id = "log-1",
            habitId = "habit-1",
            localDate = "2026-11-19",
            completedValue = 1.0,
            isCompleted = true,
            loggedAtEpochMillis = 1700001000000L
        )

        val plan = BackupDailyPlan(
            id = "plan-1",
            localDate = "2026-11-19",
            status = "APPROVED",
            createdAtEpochMillis = 1700000000000L,
            approvedAtEpochMillis = 1700000500000L,
            updatedAtEpochMillis = 1700000500000L
        )

        val item = BackupDailyPlanItem(
            id = "item-1",
            planId = "plan-1",
            sourceType = "TASK",
            sourceId = "task-1",
            title = "Write Architecture Documentation",
            startTime = "09:00",
            endTime = "09:45",
            orderIndex = 0,
            isCompleted = false
        )

        val review = BackupDailyReview(
            id = "rev-1",
            localDate = "2026-11-19",
            notes = "Great productivity today",
            reviewedAtEpochMillis = 1700078400000L,
            tasksCompletedCount = 1,
            tasksUnfinishedCount = 0,
            habitsCompletedCount = 1,
            decisionsJson = "[]"
        )

        val setting = BackupAppSetting(
            key = "last_sync_key",
            value = "ok",
            updatedAtEpochMillis = 1700000000000L
        )

        return BackupPayload(
            schemaVersion = 1,
            appVersion = "1.0.0",
            createdAt = 1700000000000L,
            categories = listOf(cat),
            reminders = listOf(rem),
            reminderOccurrences = listOf(occ),
            tasks = listOf(task),
            subtasks = listOf(subtask),
            habits = listOf(habit),
            habitLogs = listOf(log),
            dailyPlans = listOf(plan),
            planItems = listOf(item),
            reviews = listOf(review),
            appSettings = listOf(setting),
            preferences = mapOf("theme" to "DARK", "user_name" to "Alice")
        )
    }

    @Test
    fun `round-trip packaging and extraction preserves all entities losslessly`() {
        val payload = createSamplePayload()
        val targetZip = tempFolder.newFile("target.zip")
        val tempZip = tempFolder.newFile("temp.tmp")

        val manifest = BackupEngine.createBackupArchive(payload, targetZip, tempZip)

        assertNotNull(manifest)
        assertEquals(1, manifest.schemaVersion)
        assertEquals("1.0.0", manifest.appVersion)
        assertEquals(11, manifest.totalRecords)

        // Read and verify archive
        val contents = BackupEngine.verifyZipArchive(FileInputStream(targetZip), targetZip.length())
        assertEquals(manifest.dataSha256, contents.manifest.dataSha256)

        val decodedPayload = BackupEngine.jsonConfig.decodeFromString(
            BackupPayload.serializer(),
            String(contents.dataBytes, Charsets.UTF_8)
        )

        assertEquals(payload.categories.size, decodedPayload.categories.size)
        assertEquals(payload.reminders.size, decodedPayload.reminders.size)
        assertEquals(payload.reminderOccurrences.size, decodedPayload.reminderOccurrences.size)
        assertEquals(payload.tasks.size, decodedPayload.tasks.size)
        assertEquals(payload.subtasks.size, decodedPayload.subtasks.size)
        assertEquals(payload.habits.size, decodedPayload.habits.size)
        assertEquals(payload.habitLogs.size, decodedPayload.habitLogs.size)
        assertEquals(payload.dailyPlans.size, decodedPayload.dailyPlans.size)
        assertEquals(payload.planItems.size, decodedPayload.planItems.size)
        assertEquals(payload.reviews.size, decodedPayload.reviews.size)
        assertEquals(payload.appSettings.size, decodedPayload.appSettings.size)
        assertEquals(payload.preferences, decodedPayload.preferences)
    }

    @Test
    fun `writeBackupToStream streams archive properly`() {
        val payload = createSamplePayload()
        val baos = ByteArrayOutputStream()

        val manifest = BackupEngine.writeBackupToStream(payload, baos)
        assertNotNull(manifest)
        assertEquals(11, manifest.totalRecords)

        val bais = ByteArrayInputStream(baos.toByteArray())
        val contents = BackupEngine.verifyZipArchive(bais, baos.size().toLong())
        assertEquals(manifest.dataSha256, contents.manifest.dataSha256)
    }

    @Test
    fun `corrupt ZIP archive throws InvalidZipArchive`() {
        val corruptBytes = "Not a valid zip content".toByteArray()
        val bais = ByteArrayInputStream(corruptBytes)

        val error = assertThrows(BackupRestoreError.InvalidZipArchive::class.java) {
            BackupEngine.verifyZipArchive(bais, corruptBytes.size.toLong())
        }
        assertNotNull(error)
    }

    @Test
    fun `tampered data json causes checksum mismatch`() {
        val payload = createSamplePayload()
        val targetZip = tempFolder.newFile("tampered.zip")
        val tempZip = tempFolder.newFile("temp.tmp")

        BackupEngine.createBackupArchive(payload, targetZip, tempZip)

        // Read archive contents, tamper data, and reassemble
        val originalContents = BackupEngine.verifyZipArchive(FileInputStream(targetZip), targetZip.length())
        val tamperedData = (String(originalContents.dataBytes) + "   ").toByteArray(Charsets.UTF_8)

        val tamperedZip = tempFolder.newFile("tampered_final.zip")
        FileOutputStream(tamperedZip).use { fos ->
            ZipOutputStream(fos).use { zos ->
                zos.putNextEntry(ZipEntry(BackupEngine.DATA_ENTRY_NAME))
                zos.write(tamperedData)
                zos.closeEntry()

                zos.putNextEntry(ZipEntry(BackupEngine.MANIFEST_ENTRY_NAME))
                zos.write(BackupEngine.jsonConfig.encodeToString(
                    com.dayflow.app.backup.model.BackupManifest.serializer(),
                    originalContents.manifest
                ).toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
        }

        val verified = BackupEngine.verifyZipArchive(FileInputStream(tamperedZip), tamperedZip.length())
        val actualSha = BackupEngine.computeSha256(verified.dataBytes)
        // Checksum does not match manifest
        assertTrue(actualSha != verified.manifest.dataSha256)
    }

    @Test
    fun `unsupported newer schema version is detected`() {
        val payload = createSamplePayload().copy(schemaVersion = 999)
        val targetZip = tempFolder.newFile("future_schema.zip")
        val tempZip = tempFolder.newFile("temp.tmp")

        BackupEngine.createBackupArchive(payload, targetZip, tempZip)

        val contents = BackupEngine.verifyZipArchive(FileInputStream(targetZip), targetZip.length())
        assertTrue(contents.manifest.schemaVersion > BackupEngine.CURRENT_SCHEMA_VERSION)
    }

    @Test
    fun `broken foreign key in task subtask throws BrokenReferenceConstraint`() {
        val payload = createSamplePayload().let { p ->
            p.copy(subtasks = listOf(
                BackupTaskSubtask(
                    id = "sub-orphan",
                    taskId = "non-existent-task-id",
                    title = "Orphan subtask",
                    isCompleted = false,
                    orderIndex = 0,
                    createdAtEpochMillis = 1700000000000L
                )
            ))
        }

        val error = assertThrows(BackupRestoreError.BrokenReferenceConstraint::class.java) {
            BackupEngine.validatePayloadReferences(payload)
        }
        assertTrue(error.details.contains("task-1") || error.details.contains("non-existent-task-id"))
    }

    @Test
    fun `broken foreign key in reminder occurrence throws BrokenReferenceConstraint`() {
        val payload = createSamplePayload().let { p ->
            p.copy(reminderOccurrences = listOf(
                p.reminderOccurrences.first().copy(reminderId = "non-existent-rem-id")
            ))
        }

        val error = assertThrows(BackupRestoreError.BrokenReferenceConstraint::class.java) {
            BackupEngine.validatePayloadReferences(payload)
        }
        assertTrue(error.details.contains("non-existent-rem-id"))
    }

    @Test
    fun `broken foreign key in habit log throws BrokenReferenceConstraint`() {
        val payload = createSamplePayload().let { p ->
            p.copy(habitLogs = listOf(
                p.habitLogs.first().copy(habitId = "non-existent-habit-id")
            ))
        }

        val error = assertThrows(BackupRestoreError.BrokenReferenceConstraint::class.java) {
            BackupEngine.validatePayloadReferences(payload)
        }
        assertTrue(error.details.contains("non-existent-habit-id"))
    }

    @Test
    fun `broken foreign key in plan item throws BrokenReferenceConstraint`() {
        val payload = createSamplePayload().let { p ->
            p.copy(planItems = listOf(
                p.planItems.first().copy(planId = "non-existent-plan-id")
            ))
        }

        val error = assertThrows(BackupRestoreError.BrokenReferenceConstraint::class.java) {
            BackupEngine.validatePayloadReferences(payload)
        }
        assertTrue(error.details.contains("non-existent-plan-id"))
    }

    @Test
    fun `invalid date format in record throws InvalidRecordData`() {
        val payload = createSamplePayload().let { p ->
            p.copy(reminders = listOf(
                p.reminders.first().copy(startDate = "not-a-valid-date")
            ))
        }

        val error = assertThrows(BackupRestoreError.InvalidRecordData::class.java) {
            BackupEngine.validatePayloadRecords(payload)
        }
        assertEquals("Reminder", error.recordType)
    }

    @Test
    fun `pruneOldSnapshots keeps exactly 7 newest snapshots and deletes older`() {
        val backupsDir = tempFolder.newFolder("backups")

        // Create 10 dummy snapshots with sequential timestamps
        val files = (1..10).map { i ->
            val file = File(backupsDir, "dayflow_backup_${1000L + i * 100}.zip")
            file.writeText("dummy snapshot $i")
            file.setLastModified(1000L + i * 100)
            file
        }

        assertEquals(10, backupsDir.listFiles()?.size)

        // Prune keeping newest 7
        BackupEngine.pruneOldSnapshots(backupsDir, maxSnapshots = 7)

        val remaining = backupsDir.listFiles()?.filter { it.name.startsWith("dayflow_backup_") } ?: emptyList()
        assertEquals(7, remaining.size)

        // 3 oldest files (indices 0, 1, 2: i=1, 2, 3) must be deleted
        assertFalse(files[0].exists())
        assertFalse(files[1].exists())
        assertFalse(files[2].exists())

        // 7 newest files must remain
        assertTrue(files[3].exists())
        assertTrue(files[9].exists())
    }

    @Test
    fun `cleanupTempFiles deletes temporary files and keeps snapshots`() {
        val backupsDir = tempFolder.newFolder("backups_temp_test")
        val validSnapshot = File(backupsDir, "dayflow_backup_12345.zip").apply { writeText("valid") }
        val tempFile1 = File(backupsDir, "backup_temp_12345.tmp").apply { writeText("temp1") }
        val tempFile2 = File(backupsDir, "safety_temp_99999.tmp").apply { writeText("temp2") }

        assertEquals(3, backupsDir.listFiles()?.size)

        BackupEngine.cleanupTempFiles(backupsDir)

        val filesAfter = backupsDir.listFiles()?.toList() ?: emptyList()
        assertEquals(1, filesAfter.size)
        assertTrue(validSnapshot.exists())
        assertFalse(tempFile1.exists())
        assertFalse(tempFile2.exists())
    }
}
