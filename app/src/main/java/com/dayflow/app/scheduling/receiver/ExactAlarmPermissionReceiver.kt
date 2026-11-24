package com.dayflow.app.scheduling.receiver

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.dayflow.app.core.common.IoDispatcher
import com.dayflow.app.domain.usecase.RescheduleAllUseCase
import com.dayflow.app.scheduling.AlarmScheduler
import com.dayflow.app.scheduling.AndroidAlarmScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ExactAlarmPermissionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var rescheduleAllUseCase: RescheduleAllUseCase

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    override fun onReceive(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            intent.action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
        ) {
            val pendingResult = goAsync()
            CoroutineScope(ioDispatcher).launch {
                try {
                    handleIntent(intent)
                } finally {
                    pendingResult?.finish()
                }
            }
        }
    }

    internal suspend fun handleIntent(intent: Intent) {
        if (intent.action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED) {
            (alarmScheduler as? AndroidAlarmScheduler)?.refreshCapability()
            rescheduleAllUseCase()
        }
    }
}
