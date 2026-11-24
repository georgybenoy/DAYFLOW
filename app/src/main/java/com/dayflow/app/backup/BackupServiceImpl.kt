package com.dayflow.app.backup

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.dayflow.app.backup.model.BackupManifest
import com.dayflow.app.backup.model.BackupPayload
import com.dayflow.app.backup.model.BackupRestoreError
import com.dayflow.app.backup.model.BackupSnapshotInfo
import com.dayflow.app.backup.model.RestoreResult
import com.dayflow.app.backup.model.toBackup
import com.dayflow.app.core.common.IoDispatcher
import com.dayflow.app.data.local.DataStoreKeys
import com.dayflow.app.data.local.dao.BackupDao
import com.dayflow.app.domain.usecase.RescheduleAllUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant
import java.util.zip.ZipFile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupDao: BackupDao,
    private val dataStore: DataStore<Preferences>,
    private val rescheduleAllUseCase: RescheduleAllUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : BackupService {

    private val backupsDir: File
        get() = File(context.filesDir, "backups").apply { mkdirs() }

    override suspend fun createBackupSnapshot(): Result<BackupSnapshotInfo> = withContext(ioDispatcher) {
        try {
            BackupEngine.cleanupTempFiles(backupsDir)

            val payload = buildCurrentPayload()
            val timestamp = payload.createdAt
            val tempFile = File(backupsDir, "backup_temp_${timestamp}.tmp")
            val targetFile = File(backupsDir, "dayflow_backup_${timestamp}.zip")

            val manifest = BackupEngine.createBackupArchive(payload, targetFile, tempFile)

            val retentionCount = dataStore.data.first()[DataStoreKeys.BACKUP_RETENTION_COUNT]
                ?: BackupEngine.DEFAULT_RETENTION_COUNT
            BackupEngine.pruneOldSnapshots(backupsDir, retentionCount)

            dataStore.edit { prefs ->
                prefs[DataStoreKeys.LAST_BACKUP_TIME] = Instant.ofEpochMilli(timestamp).toString()
                prefs[DataStoreKeys.LAST_BACKUP_STATUS] = "SUCCESS"
                prefs[DataStoreKeys.LAST_BACKUP_SNAPSHOT_NAME] = targetFile.name
            }

            val snapshotInfo = BackupSnapshotInfo(
                filename = targetFile.name,
                file = targetFile,
                sizeBytes = targetFile.length(),
                createdAt = Instant.ofEpochMilli(timestamp),
                recordsCount = manifest.totalRecords
            )
            Result.success(snapshotInfo)
        } catch (e: Exception) {
            dataStore.edit { prefs ->
                prefs[DataStoreKeys.LAST_BACKUP_STATUS] = "FAILURE: ${e.message ?: "Unknown error"}"
            }
            Result.failure(e)
        }
    }

    override suspend fun restoreFromSnapshot(snapshotFile: File): RestoreResult = withContext(ioDispatcher) {
        if (!snapshotFile.exists() || !snapshotFile.canRead()) {
            return@withContext RestoreResult.Failure(
                BackupRestoreError.IoError("Snapshot file does not exist or cannot be read: ${snapshotFile.name}")
            )
        }

        try {
            FileInputStream(snapshotFile).use { fis ->
                val currentPrefs = dataStore.data.first().asMap()
                    .mapKeys { it.key.name }
                    .mapValues { it.value.toString() }

                BackupEngine.executeRestorePipeline(
                    archiveStream = fis,
                    archiveSize = snapshotFile.length(),
                    backupDao = backupDao,
                    currentSettings = currentPrefs,
                    onSaveSafetySnapshot = { safetyFile ->
                        val safetyPayload = buildCurrentPayload()
                        val safetyTemp = File(backupsDir, "safety_temp_${System.currentTimeMillis()}.tmp")
                        BackupEngine.createBackupArchive(safetyPayload, safetyFile, safetyTemp)
                    },
                    onRestoreSettings = { settingsMap ->
                        restorePreferences(settingsMap)
                    },
                    rescheduleAllUseCase = rescheduleAllUseCase,
                    backupsDir = backupsDir
                )
            }
        } catch (e: Exception) {
            RestoreResult.Failure(BackupRestoreError.IoError(e.message ?: "Failed reading snapshot file"))
        }
    }

    override suspend fun exportBackup(outputStream: OutputStream): Result<BackupManifest> = withContext(ioDispatcher) {
        try {
            val payload = buildCurrentPayload()
            val manifest = BackupEngine.writeBackupToStream(payload, outputStream)
            Result.success(manifest)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun importBackup(inputStream: InputStream, sizeHint: Long): RestoreResult = withContext(ioDispatcher) {
        try {
            val currentPrefs = dataStore.data.first().asMap()
                .mapKeys { it.key.name }
                .mapValues { it.value.toString() }

            BackupEngine.executeRestorePipeline(
                archiveStream = inputStream,
                archiveSize = sizeHint,
                backupDao = backupDao,
                currentSettings = currentPrefs,
                onSaveSafetySnapshot = { safetyFile ->
                    val safetyPayload = buildCurrentPayload()
                    val safetyTemp = File(backupsDir, "safety_temp_${System.currentTimeMillis()}.tmp")
                    BackupEngine.createBackupArchive(safetyPayload, safetyFile, safetyTemp)
                },
                onRestoreSettings = { settingsMap ->
                    restorePreferences(settingsMap)
                },
                rescheduleAllUseCase = rescheduleAllUseCase,
                backupsDir = backupsDir
            )
        } catch (e: Exception) {
            RestoreResult.Failure(BackupRestoreError.IoError(e.message ?: "Failed importing backup archive"))
        }
    }

    override suspend fun getSnapshots(): List<BackupSnapshotInfo> = withContext(ioDispatcher) {
        if (!backupsDir.exists() || !backupsDir.isDirectory) return@withContext emptyList()

        val files = backupsDir.listFiles { file ->
            file.isFile && file.name.startsWith("dayflow_backup_") && file.name.endsWith(".zip")
        } ?: return@withContext emptyList()

        files.map { file ->
            val manifest = readManifestQuickly(file)
            val createdAt = if (manifest != null) {
                Instant.ofEpochMilli(manifest.createdAt)
            } else {
                extractTimestampFromFilename(file.name) ?: Instant.ofEpochMilli(file.lastModified())
            }
            BackupSnapshotInfo(
                filename = file.name,
                file = file,
                sizeBytes = file.length(),
                createdAt = createdAt,
                recordsCount = manifest?.totalRecords
            )
        }.sortedByDescending { it.createdAt }
    }

    override suspend fun deleteSnapshot(filename: String): Boolean = withContext(ioDispatcher) {
        val file = File(backupsDir, filename)
        // Path traversal guard
        if (file.canonicalFile.parentFile != backupsDir.canonicalFile) {
            return@withContext false
        }
        if (file.exists() && file.isFile) {
            file.delete()
        } else {
            false
        }
    }

    override suspend fun cleanupTempFiles() = withContext(ioDispatcher) {
        BackupEngine.cleanupTempFiles(backupsDir)
    }

    // --- Private Helpers ---

    private suspend fun buildCurrentPayload(): BackupPayload {
        val categories = backupDao.getAllCategories().map { it.toBackup() }
        val reminders = backupDao.getAllReminders().map { it.toBackup() }
        val occurrences = backupDao.getAllReminderOccurrences().map { it.toBackup() }
        val tasks = backupDao.getAllTasks().map { it.toBackup() }
        val subtasks = backupDao.getAllTaskSubtasks().map { it.toBackup() }
        val habits = backupDao.getAllHabits().map { it.toBackup() }
        val habitLogs = backupDao.getAllHabitLogs().map { it.toBackup() }
        val dailyPlans = backupDao.getAllDailyPlans().map { it.toBackup() }
        val planItems = backupDao.getAllDailyPlanItems().map { it.toBackup() }
        val dailyReviews = backupDao.getAllDailyReviews().map { it.toBackup() }
        val appSettings = backupDao.getAllAppSettings().map { it.toBackup() }

        val prefs = dataStore.data.first()
        val preferencesMap = mutableMapOf<String, String>()
        prefs[DataStoreKeys.USER_NAME]?.let { preferencesMap[DataStoreKeys.USER_NAME.name] = it }
        prefs[DataStoreKeys.THEME_PREFERENCE]?.let { preferencesMap[DataStoreKeys.THEME_PREFERENCE.name] = it }
        prefs[DataStoreKeys.MORNING_BRIEFING_TIME]?.let { preferencesMap[DataStoreKeys.MORNING_BRIEFING_TIME.name] = it }
        prefs[DataStoreKeys.EVENING_REVIEW_TIME]?.let { preferencesMap[DataStoreKeys.EVENING_REVIEW_TIME.name] = it }
        prefs[DataStoreKeys.QUIET_HOURS_START]?.let { preferencesMap[DataStoreKeys.QUIET_HOURS_START.name] = it }
        prefs[DataStoreKeys.QUIET_HOURS_END]?.let { preferencesMap[DataStoreKeys.QUIET_HOURS_END.name] = it }
        prefs[DataStoreKeys.IS_ONBOARDING_COMPLETED]?.let { preferencesMap[DataStoreKeys.IS_ONBOARDING_COMPLETED.name] = it.toString() }
        prefs[DataStoreKeys.EXACT_ALARM_ACKNOWLEDGED]?.let { preferencesMap[DataStoreKeys.EXACT_ALARM_ACKNOWLEDGED.name] = it.toString() }
        prefs[DataStoreKeys.BACKUP_RETENTION_COUNT]?.let { preferencesMap[DataStoreKeys.BACKUP_RETENTION_COUNT.name] = it.toString() }

        return BackupPayload(
            schemaVersion = BackupEngine.CURRENT_SCHEMA_VERSION,
            appVersion = BackupEngine.CURRENT_APP_VERSION,
            createdAt = System.currentTimeMillis(),
            categories = categories,
            reminders = reminders,
            reminderOccurrences = occurrences,
            tasks = tasks,
            subtasks = subtasks,
            habits = habits,
            habitLogs = habitLogs,
            dailyPlans = dailyPlans,
            planItems = planItems,
            reviews = dailyReviews,
            appSettings = appSettings,
            preferences = preferencesMap
        )
    }

    private suspend fun restorePreferences(preferencesMap: Map<String, String>) {
        dataStore.edit { prefs ->
            preferencesMap[DataStoreKeys.USER_NAME.name]?.let { prefs[DataStoreKeys.USER_NAME] = it }
            preferencesMap[DataStoreKeys.THEME_PREFERENCE.name]?.let { prefs[DataStoreKeys.THEME_PREFERENCE] = it }
            preferencesMap[DataStoreKeys.MORNING_BRIEFING_TIME.name]?.let { prefs[DataStoreKeys.MORNING_BRIEFING_TIME] = it }
            preferencesMap[DataStoreKeys.EVENING_REVIEW_TIME.name]?.let { prefs[DataStoreKeys.EVENING_REVIEW_TIME] = it }
            preferencesMap[DataStoreKeys.QUIET_HOURS_START.name]?.let { prefs[DataStoreKeys.QUIET_HOURS_START] = it }
            preferencesMap[DataStoreKeys.QUIET_HOURS_END.name]?.let { prefs[DataStoreKeys.QUIET_HOURS_END] = it }
            preferencesMap[DataStoreKeys.IS_ONBOARDING_COMPLETED.name]?.toBooleanStrictOrNull()?.let {
                prefs[DataStoreKeys.IS_ONBOARDING_COMPLETED] = it
            }
            preferencesMap[DataStoreKeys.EXACT_ALARM_ACKNOWLEDGED.name]?.toBooleanStrictOrNull()?.let {
                prefs[DataStoreKeys.EXACT_ALARM_ACKNOWLEDGED] = it
            }
            preferencesMap[DataStoreKeys.BACKUP_RETENTION_COUNT.name]?.toIntOrNull()?.let {
                prefs[DataStoreKeys.BACKUP_RETENTION_COUNT] = it
            }
        }
    }

    private fun readManifestQuickly(zipFile: File): BackupManifest? {
        return try {
            ZipFile(zipFile).use { zf ->
                val entry = zf.getEntry(BackupEngine.MANIFEST_ENTRY_NAME) ?: return null
                zf.getInputStream(entry).use { stream ->
                    val text = stream.bufferedReader(Charsets.UTF_8).readText()
                    BackupEngine.jsonConfig.decodeFromString(BackupManifest.serializer(), text)
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun extractTimestampFromFilename(filename: String): Instant? {
        // dayflow_backup_<epochMillis>.zip
        return try {
            val millisPart = filename.removePrefix("dayflow_backup_").removeSuffix(".zip")
            Instant.ofEpochMilli(millisPart.toLong())
        } catch (_: Exception) {
            null
        }
    }
}
