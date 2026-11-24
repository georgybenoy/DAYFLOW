package com.dayflow.app.scheduling.receiver

import android.content.Intent
import com.dayflow.app.domain.usecase.RescheduleAllUseCase
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TimeChangeReceiverTest {

    private val rescheduleAllUseCase: RescheduleAllUseCase = mockk(relaxed = true)
    private lateinit var receiver: TimeChangeReceiver

    @Before
    fun setup() {
        receiver = TimeChangeReceiver().apply {
            this.rescheduleAllUseCase = this@TimeChangeReceiverTest.rescheduleAllUseCase
            this.ioDispatcher = Dispatchers.Unconfined
        }
    }

    @Test
    fun onReceive_timeSet_triggersRescheduleAll() = runTest {
        val intent = Intent(Intent.ACTION_TIME_CHANGED)
        receiver.handleIntent(intent)

        coVerify(exactly = 1) { rescheduleAllUseCase(any()) }
    }

    @Test
    fun onReceive_timezoneChanged_triggersRescheduleAll() = runTest {
        val intent = Intent(Intent.ACTION_TIMEZONE_CHANGED)
        receiver.handleIntent(intent)

        coVerify(exactly = 1) { rescheduleAllUseCase(any()) }
    }

    @Test
    fun onReceive_dateChanged_triggersRescheduleAll() = runTest {
        val intent = Intent(Intent.ACTION_DATE_CHANGED)
        receiver.handleIntent(intent)

        coVerify(exactly = 1) { rescheduleAllUseCase(any()) }
    }

    @Test
    fun onReceive_unrelatedAction_doesNotTriggerReschedule() = runTest {
        val intent = Intent("com.dayflow.app.action.UNRELATED")
        receiver.handleIntent(intent)

        coVerify(exactly = 0) { rescheduleAllUseCase(any()) }
    }
}
