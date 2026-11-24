package com.dayflow.app.backup

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * WorkManager worker executing periodic automatic backups of DAYFLOW data.
 * Runs once every 24 hours, retains newest 7 snapshots, and records status in DataStore.
 */
@HiltWorker
class DayflowBackupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val backupService: BackupService
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val result = backupService.createBackupSnapshot()
            if (result.isSuccess) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "dayflow_periodic_backup"
    }
}
