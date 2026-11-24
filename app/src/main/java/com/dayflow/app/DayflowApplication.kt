package com.dayflow.app

import android.app.Application
import android.os.StrictMode
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.dayflow.app.backup.BackupWorkScheduler
import com.dayflow.app.core.common.DayflowLogger
import com.dayflow.app.notifications.DayflowNotificationChannels
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class DayflowApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var backupWorkScheduler: BackupWorkScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            enableStrictMode()
        }
        DayflowLogger.i("App", "Application initializing")
        DayflowNotificationChannels.createChannels(this)
        backupWorkScheduler.schedulePeriodicBackup()
    }

    private fun enableStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .build()
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .build()
        )
    }
}
