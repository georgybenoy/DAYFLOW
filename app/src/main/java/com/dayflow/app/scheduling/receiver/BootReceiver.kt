package com.dayflow.app.scheduling.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dayflow.app.core.common.IoDispatcher
import com.dayflow.app.domain.usecase.RescheduleAllUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var rescheduleAllUseCase: RescheduleAllUseCase

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
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
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            rescheduleAllUseCase()
        }
    }
}
