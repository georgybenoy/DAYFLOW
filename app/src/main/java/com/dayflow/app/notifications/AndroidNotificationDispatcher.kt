package com.dayflow.app.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import com.dayflow.app.MainActivity
import com.dayflow.app.domain.model.Priority
import com.dayflow.app.domain.model.Reminder
import com.dayflow.app.domain.model.ReminderOccurrence
import com.dayflow.app.scheduling.HabitReminderScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidNotificationDispatcher @Inject constructor(
    @ApplicationContext private val context: Context
) : NotificationDispatcher {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override fun dispatchReminderNotification(
        reminder: Reminder,
        occurrence: ReminderOccurrence,
        isSilent: Boolean
    ) {
        val channelId = DayflowNotificationChannels.getChannelForPriority(reminder.priority)
        val notificationId = getNotificationId(occurrence.id)

        val isHabit = HabitReminderScheduler.isHabitReminder(reminder.id)
        val habitId = if (isHabit) HabitReminderScheduler.extractHabitId(reminder.id) else null
        val isBinaryHabit = isHabit && (reminder.description?.contains("Habit: Binary") == true)
        val isNonBinaryHabit = isHabit && !isBinaryHabit

        val contentIntent = if (isHabit && habitId != null) {
            createHabitContentIntent(habitId)
        } else {
            createContentIntent(occurrence.id, reminder.id)
        }

        val completeIntent = NotificationActionReceiver.createCompleteIntent(context, occurrence.id, reminder.id)
        val quickLogIntent = if (isNonBinaryHabit && habitId != null) {
            createQuickLogIntent(occurrence.id, habitId)
        } else null

        val snoozeIntent = NotificationActionReceiver.createSnoozeIntent(context, occurrence.id, reminder.id)
        val skipIntent = NotificationActionReceiver.createSkipIntent(context, occurrence.id, reminder.id)
        val dismissIntent = NotificationActionReceiver.createDismissIntent(context, occurrence.id, reminder.id)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(reminder.title)
            .setContentText(reminder.description ?: "Scheduled commitment")
            .setPriority(
                if (reminder.priority == Priority.URGENT) NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT
            )
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(
                if (reminder.priority == Priority.URGENT) NotificationCompat.VISIBILITY_PUBLIC
                else NotificationCompat.VISIBILITY_PRIVATE
            )
            .setContentIntent(contentIntent)
            .setDeleteIntent(dismissIntent)
            .setAutoCancel(true)
            .setGroup(DayflowNotificationChannels.GROUP_REMINDERS)

        if (isNonBinaryHabit && quickLogIntent != null) {
            builder.addAction(0, "Log Progress", quickLogIntent)
        } else {
            builder.addAction(0, "Complete", completeIntent)
        }

        builder.addAction(0, "Snooze", snoozeIntent)
            .addAction(0, "Skip", skipIntent)

        if (isSilent) {
            builder.setSilent(true)
        }

        try {
            notificationManager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // Handled when POST_NOTIFICATIONS is revoked at runtime
        }
    }

    override fun cancelReminderNotification(occurrenceId: String) {
        val notificationId = getNotificationId(occurrenceId)
        notificationManager.cancel(notificationId)
    }

    private fun getNotificationId(occurrenceId: String): Int {
        return (occurrenceId.hashCode() and 0x7FFFFFFF)
    }

    private fun createContentIntent(occurrenceId: String, reminderId: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("dayflow://reminder/$reminderId")
            putExtra("EXTRA_REMINDER_ID", reminderId)
            putExtra("EXTRA_OCCURRENCE_ID", occurrenceId)
        }

        return TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(intent)
            getPendingIntent(
                (reminderId.hashCode() and 0x7FFFFFFF),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )!!
        }
    }

    private fun createHabitContentIntent(habitId: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("dayflow://habit/$habitId")
            putExtra("EXTRA_HABIT_ID", habitId)
        }

        return TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(intent)
            getPendingIntent(
                (habitId.hashCode() and 0x7FFFFFFF),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )!!
        }
    }

    private fun createQuickLogIntent(occurrenceId: String, habitId: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("dayflow://habit/$habitId/log")
            putExtra("EXTRA_HABIT_ID", habitId)
            putExtra("EXTRA_OCCURRENCE_ID", occurrenceId)
            putExtra("EXTRA_OPEN_QUICK_LOG", true)
        }

        return TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(intent)
            getPendingIntent(
                ((occurrenceId.hashCode() xor 0x55AA) and 0x7FFFFFFF),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )!!
        }
    }

    override fun dispatchBriefingNotification(title: String, summary: String, date: java.time.LocalDate) {
        val channelId = DayflowNotificationChannels.CHANNEL_DAILY_BRIEFING
        val notificationId = NOTIFICATION_ID_BRIEFING

        val openPlanIntent = createDestinationPendingIntent("plan", date.toString(), 1001)
        val openBriefingIntent = createDestinationPendingIntent("briefing", date.toString(), 1002)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(summary))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openBriefingIntent)
            .setAutoCancel(true)
            .addAction(0, "Open Plan", openPlanIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    override fun dispatchReviewNotification(title: String, summary: String, date: java.time.LocalDate) {
        val channelId = DayflowNotificationChannels.CHANNEL_EVENING_REVIEW
        val notificationId = NOTIFICATION_ID_REVIEW

        val openReviewIntent = createDestinationPendingIntent("review", date.toString(), 2001)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(summary))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openReviewIntent)
            .setAutoCancel(true)
            .addAction(0, "Start Review", openReviewIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    private fun createDestinationPendingIntent(destination: String, dateString: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("dayflow://$destination/$dateString")
            putExtra("EXTRA_DESTINATION", destination)
            putExtra("EXTRA_DATE", dateString)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        return TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(intent)
            getPendingIntent(
                requestCode,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )!!
        }
    }

    companion object {
        const val NOTIFICATION_ID_BRIEFING = 90001
        const val NOTIFICATION_ID_REVIEW = 90002
    }
}
