package com.dayflow.app.scheduling

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.dayflow.app.domain.model.Priority
import com.dayflow.app.domain.model.Reminder
import com.dayflow.app.domain.model.ReminderOccurrence
import com.dayflow.app.domain.model.ReminderType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlarmManager
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
class AndroidAlarmSchedulerTest {

    private lateinit var context: Context
    private lateinit var alarmManager: AlarmManager
    private lateinit var shadowAlarmManager: ShadowAlarmManager
    private lateinit var scheduler: AndroidAlarmScheduler

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        shadowAlarmManager = shadowOf(alarmManager)
        scheduler = AndroidAlarmScheduler(context)
    }

    private fun sampleReminder(id: String = "rem_sched_1"): Reminder {
        return Reminder(
            id = id,
            title = "Hydration Reminder",
            type = ReminderType.DAILY,
            startDate = LocalDate.of(2026, 11, 18),
            startTime = LocalTime.of(10, 0),
            priority = Priority.MEDIUM,
            timeZoneId = ZoneId.of("UTC")
        )
    }

    private fun sampleOccurrence(reminderId: String, triggerTime: Instant): ReminderOccurrence {
        return ReminderOccurrence(
            id = "occ_sched_1",
            reminderId = reminderId,
            scheduledAt = triggerTime
        )
    }

    @Test
    fun scheduleOccurrence_schedulesExactAlarmInAlarmManager() = runBlocking {
        val reminder = sampleReminder()
        val triggerAt = Instant.now().plusSeconds(3600)
        val occurrence = sampleOccurrence(reminder.id, triggerAt)

        val scheduled = scheduler.scheduleOccurrence(occurrence, reminder)
        assertTrue(scheduled)

        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertEquals(1, scheduledAlarms.size)
        val alarm = scheduledAlarms[0]
        assertEquals(triggerAt.toEpochMilli(), alarm.triggerAtTime)
    }

    @Test
    fun cancelOccurrence_removesAlarmFromAlarmManager() = runBlocking {
        val reminder = sampleReminder()
        val triggerAt = Instant.now().plusSeconds(3600)
        val occurrence = sampleOccurrence(reminder.id, triggerAt)

        scheduler.scheduleOccurrence(occurrence, reminder)
        assertEquals(1, shadowAlarmManager.scheduledAlarms.size)

        scheduler.cancelOccurrence(occurrence.id, reminder.id)
        assertEquals(0, shadowAlarmManager.scheduledAlarms.size)
    }

    @Test
    fun schedulingCapability_evaluatesCorrectly() {
        val capability = scheduler.getSchedulingCapability()
        assertNotNull(capability)

        // By default without POST_NOTIFICATIONS runtime permission granted, capability is NOTIFICATIONS_DENIED
        assertEquals(SchedulingCapability.NOTIFICATIONS_DENIED, capability)

        // Grant POST_NOTIFICATIONS
        val app = ApplicationProvider.getApplicationContext<Context>() as android.app.Application
        shadowOf(app).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)

        val updatedCapability = scheduler.getSchedulingCapability()
        assertTrue(
            "Expected EXACT or APPROXIMATE_FALLBACK once notifications are granted",
            updatedCapability == SchedulingCapability.EXACT || updatedCapability == SchedulingCapability.APPROXIMATE_FALLBACK
        )
    }
}
