package com.dayflow.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.provider.Settings
import com.dayflow.app.domain.model.Priority

object DayflowNotificationChannels {

    const val CHANNEL_CRITICAL_DEADLINES = "channel_critical_deadlines"
    const val CHANNEL_ROUTINE_REMINDERS = "channel_routine_reminders"
    const val CHANNEL_DAILY_BRIEFING = "channel_daily_briefing"
    const val CHANNEL_EVENING_REVIEW = "channel_evening_review"
    const val CHANNEL_GENERAL = "channel_general"

    const val GROUP_REMINDERS = "group_dayflow_reminders"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val channels = listOf(
            NotificationChannel(
                CHANNEL_CRITICAL_DEADLINES,
                "Critical Deadlines",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent commitments, deadlines, and high-priority alarms."
                enableLights(true)
                enableVibration(true)
                setBypassDnd(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            },
            NotificationChannel(
                CHANNEL_ROUTINE_REMINDERS,
                "Routine Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Recurring reminders, habit tracking nudges, and hydration alerts."
                enableLights(true)
                enableVibration(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
            },
            NotificationChannel(
                CHANNEL_DAILY_BRIEFING,
                "Daily Briefing",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Morning overview and daily execution roadmap."
                enableLights(false)
                enableVibration(false)
            },
            NotificationChannel(
                CHANNEL_EVENING_REVIEW,
                "Evening Review",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Evening reflection, unfinished task review, and tomorrow's preview."
                enableLights(false)
                enableVibration(false)
            },
            NotificationChannel(
                CHANNEL_GENERAL,
                "General Notifications",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Automated backups, integrity validations, and system status updates."
                enableLights(false)
                enableVibration(false)
            }
        )

        notificationManager.createNotificationChannels(channels)
    }

    /**
     * Maps reminder priority to the appropriate notification channel ID.
     */
    fun getChannelForPriority(priority: Priority): String {
        return when (priority) {
            Priority.URGENT -> CHANNEL_CRITICAL_DEADLINES
            Priority.HIGH, Priority.MEDIUM, Priority.LOW -> CHANNEL_ROUTINE_REMINDERS
        }
    }
}
