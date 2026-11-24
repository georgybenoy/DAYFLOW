package com.dayflow.app.scheduling.receiver

import android.app.AlarmManager
import android.content.Intent
import com.dayflow.app.domain.usecase.RescheduleAllUseCase
import com.dayflow.app.scheduling.AndroidAlarmScheduler
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExactAlarmPermissionReceiverTest {

    private val rescheduleAllUseCase: RescheduleAllUseCase = mockk(relaxed = true)
    private val alarmScheduler: AndroidAlarmScheduler = mockk(relaxed = true)
    private lateinit var receiver: ExactAlarmPermissionReceiver

    @Before
    fun setup() {
        receiver = ExactAlarmPermissionReceiver().apply {
            this.rescheduleAllUseCase = this@ExactAlarmPermissionReceiverTest.rescheduleAllUseCase
            this.alarmScheduler = this@ExactAlarmPermissionReceiverTest.alarmScheduler
            this.ioDispatcher = Dispatchers.Unconfined
        }
    }

    @Test
    fun onReceive_permissionStateChanged_triggersRescheduleAll() = runTest {
        val intent = Intent(AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)
        receiver.handleIntent(intent)

        verify(exactly = 1) { alarmScheduler.refreshCapability() }
        coVerify(exactly = 1) { rescheduleAllUseCase(any()) }
    }

    @Test
    fun onReceive_otherAction_doesNotTriggerReschedule() = runTest {
        val intent = Intent("com.dayflow.app.action.UNKNOWN")
        receiver.handleIntent(intent)

        verify(exactly = 0) { alarmScheduler.refreshCapability() }
        coVerify(exactly = 0) { rescheduleAllUseCase(any()) }
    }
}
