package com.dayflow.app.backup

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupWorkScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Schedules periodic automatic backups every 24 hours with KEEP policy.
     * Guarantees single active periodic work registration across app updates and boots.
     */
    fun schedulePeriodicBackup() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<DayflowBackupWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
            flexTimeInterval = 2,
            flexTimeIntervalUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DayflowBackupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )
    }

    /**
     * Cancels any scheduled backup work (used for test teardown).
     */
    fun cancelBackup() {
        WorkManager.getInstance(context).cancelUniqueWork(DayflowBackupWorker.WORK_NAME)
    }
}
