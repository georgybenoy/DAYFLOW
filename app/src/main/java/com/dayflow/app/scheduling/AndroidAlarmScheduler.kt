package com.dayflow.app.scheduling

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.dayflow.app.domain.model.Reminder
import com.dayflow.app.domain.model.ReminderOccurrence
import com.dayflow.app.scheduling.receiver.AlarmReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) : AlarmScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    private val _capabilityFlow = MutableStateFlow(getSchedulingCapability())
    override val schedulingCapability: Flow<SchedulingCapability> = _capabilityFlow.asStateFlow()

    override fun getSchedulingCapability(): SchedulingCapability {
        val notificationsEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }

        if (!notificationsEnabled) {
            return SchedulingCapability.NOTIFICATIONS_DENIED
        }

        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager?.canScheduleExactAlarms() ?: false
        } else {
            true
        }

        return if (canExact) {
            SchedulingCapability.EXACT
        } else {
            SchedulingCapability.APPROXIMATE_FALLBACK
        }
    }

    /**
     * Refreshes capability flow (e.g. after permissions change).
     */
    fun refreshCapability() {
        _capabilityFlow.value = getSchedulingCapability()
    }

    override suspend fun scheduleOccurrence(
        occurrence: ReminderOccurrence,
        reminder: Reminder
    ): Boolean {
        if (alarmManager == null) return false

        val triggerAtMillis = occurrence.scheduledAt.toEpochMilli()
        val pendingIntent = createAlarmPendingIntent(occurrence.id, reminder.id)

        return try {
            val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }

            if (canExact) {
                // One-shot exact alarm that fires in Doze
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                // Inexact fallback disclosed to the user
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
            true
        } catch (e: SecurityException) {
            // Permission revoked concurrently -> Fallback to inexact
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                refreshCapability()
                true
            } catch (fallbackException: Exception) {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun cancelOccurrence(occurrenceId: String, reminderId: String) {
        if (alarmManager == null) return
        val pendingIntent = createAlarmPendingIntent(occurrenceId, reminderId)
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    override suspend fun cancelAllForReminder(reminderId: String) {
        // Individual occurrences are cancelled via cancelOccurrence(occ.id, reminderId)
    }

    private fun createAlarmPendingIntent(occurrenceId: String, reminderId: String): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM_TRIGGER
            putExtra(AlarmReceiver.EXTRA_OCCURRENCE_ID, occurrenceId)
            putExtra(AlarmReceiver.EXTRA_REMINDER_ID, reminderId)
            data = Uri.parse("dayflow://occurrence/$occurrenceId")
        }

        val requestCode = (occurrenceId.hashCode() and 0x7FFFFFFF)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
