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
class BootReceiverTest {

    private val rescheduleAllUseCase: RescheduleAllUseCase = mockk(relaxed = true)
    private lateinit var receiver: BootReceiver

    @Before
    fun setup() {
        receiver = BootReceiver().apply {
            this.rescheduleAllUseCase = this@BootReceiverTest.rescheduleAllUseCase
            this.ioDispatcher = Dispatchers.Unconfined
        }
    }

    @Test
    fun onReceive_bootCompleted_triggersRescheduleAll() = runTest {
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)
        receiver.handleIntent(intent)

        coVerify(exactly = 1) { rescheduleAllUseCase(any()) }
    }

    @Test
    fun onReceive_otherAction_doesNotTriggerReschedule() = runTest {
        val intent = Intent("com.dayflow.app.action.UNKNOWN")
        receiver.handleIntent(intent)

        coVerify(exactly = 0) { rescheduleAllUseCase(any()) }
    }
}
