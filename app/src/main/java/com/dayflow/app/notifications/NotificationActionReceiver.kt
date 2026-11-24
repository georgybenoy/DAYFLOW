package com.dayflow.app.notifications

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.dayflow.app.core.common.IoDispatcher
import com.dayflow.app.domain.model.HabitLog
import com.dayflow.app.domain.model.HabitTrackingType
import com.dayflow.app.domain.model.OccurrenceStatus
import com.dayflow.app.domain.model.ReminderOccurrence
import com.dayflow.app.domain.model.ReminderType
import com.dayflow.app.domain.recurrence.RecurrenceCalculator
import com.dayflow.app.domain.repository.HabitRepository
import com.dayflow.app.domain.repository.ReminderRepository
import com.dayflow.app.domain.repository.SettingsRepository
import com.dayflow.app.scheduling.AlarmScheduler
import com.dayflow.app.scheduling.HabitReminderScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var reminderRepository: ReminderRepository

    @Inject
    lateinit var habitRepository: HabitRepository

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    @Inject
    lateinit var notificationDispatcher: NotificationDispatcher

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val occurrenceId = intent.getStringExtra(EXTRA_OCCURRENCE_ID) ?: return
        val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return

        val pendingResult = goAsync()
        CoroutineScope(ioDispatcher).launch {
            try {
                handleAction(action, occurrenceId, reminderId)
            } finally {
                pendingResult?.finish()
            }
        }
    }

    internal suspend fun handleAction(action: String, occurrenceId: String, reminderId: String) {
        val now = Instant.now()
        val occurrence = reminderRepository.getOccurrenceByIdDirect(occurrenceId)
        val reminder = reminderRepository.getReminderByIdDirect(reminderId)

        when (action) {
            ACTION_COMPLETE -> {
                notificationDispatcher.cancelReminderNotification(occurrenceId)
                if (occurrence != null) {
                    reminderRepository.updateOccurrence(
                        occurrence.copy(
                            status = OccurrenceStatus.COMPLETED,
                            handledAt = now
                        )
                    )
                }

                if (reminder != null) {
                    val updatedReminder = reminder.copy(lastCompletedAt = now)
                    reminderRepository.saveReminder(updatedReminder)

                    // If linked to a habit, log the habit completion for today if binary
                    if (HabitReminderScheduler.isHabitReminder(reminder.id)) {
                        val habitId = HabitReminderScheduler.extractHabitId(reminder.id)
                        val habit = habitRepository.getHabitByIdDirect(habitId)
                        if (habit != null && habit.trackingType == HabitTrackingType.BINARY) {
                            val today = LocalDate.now()
                            val existingLog = habitRepository.getLogForHabitAndDateDirect(habitId, today)
                            habitRepository.logHabitProgress(
                                (existingLog ?: HabitLog(habitId = habitId, localDate = today)).copy(
                                    completedValue = 1.0,
                                    isCompleted = true,
                                    loggedAt = now
                                )
                            )
                        }
                    }

                    // For completion intervals, schedule next slot relative to completion time
                    if (reminder.type == ReminderType.COMPLETION_INTERVAL) {
                        val nextInstant = RecurrenceCalculator.nextAfterCompletion(
                            reminder = updatedReminder,
                            completedAt = now
                        )
                        if (nextInstant != null) {
                            val newOccurrence = ReminderOccurrence(
                                reminderId = reminder.id,
                                scheduledAt = nextInstant,
                                status = OccurrenceStatus.SCHEDULED
                            )
                            if (reminderRepository.scheduleOccurrence(newOccurrence)) {
                                alarmScheduler.scheduleOccurrence(newOccurrence, updatedReminder)
                            }
                        }
                    }
                }
            }

            ACTION_SNOOZE -> {
                notificationDispatcher.cancelReminderNotification(occurrenceId)
                if (reminder != null && occurrence != null) {
                    val defaultSnoozeMinutes = 10L
                    val snoozeOccurrence = RecurrenceCalculator.nextAfterSnooze(
                        reminder = reminder,
                        snoozedAt = now,
                        snoozeDurationMinutes = defaultSnoozeMinutes
                    )

                    // Persist transient occurrence and schedule alarm
                    if (reminderRepository.scheduleOccurrence(snoozeOccurrence)) {
                        alarmScheduler.scheduleOccurrence(snoozeOccurrence, reminder)
                    }

                    // Update handled state on current occurrence
                    reminderRepository.updateOccurrence(
                        occurrence.copy(handledAt = now)
                    )
                }
            }

            ACTION_SKIP -> {
                notificationDispatcher.cancelReminderNotification(occurrenceId)
                if (occurrence != null) {
                    reminderRepository.updateOccurrence(
                        occurrence.copy(
                            status = OccurrenceStatus.SKIPPED,
                            handledAt = now
                        )
                    )
                }

                if (reminder != null && reminder.type == ReminderType.COMPLETION_INTERVAL && occurrence != null) {
                    // Skip advances from planned scheduledAt to maintain cadence
                    val nextInstant = RecurrenceCalculator.nextAfterSkip(
                        reminder = reminder,
                        scheduledAt = occurrence.scheduledAt
                    )
                    if (nextInstant != null) {
                        val newOccurrence = ReminderOccurrence(
                            reminderId = reminder.id,
                            scheduledAt = nextInstant,
                            status = OccurrenceStatus.SCHEDULED
                        )
                        if (reminderRepository.scheduleOccurrence(newOccurrence)) {
                            alarmScheduler.scheduleOccurrence(newOccurrence, reminder)
                        }
                    }
                }
            }

            ACTION_DISMISS -> {
                // Invariant: Dismissal MUST NOT complete the reminder
                notificationDispatcher.cancelReminderNotification(occurrenceId)
                if (occurrence != null && occurrence.handledAt == null) {
                    reminderRepository.updateOccurrence(occurrence.copy(handledAt = now))
                }
            }
        }
    }

    companion object {
        const val ACTION_COMPLETE = "com.dayflow.app.action.COMPLETE"
        const val ACTION_SNOOZE = "com.dayflow.app.action.SNOOZE"
        const val ACTION_SKIP = "com.dayflow.app.action.SKIP"
        const val ACTION_DISMISS = "com.dayflow.app.action.DISMISS"

        const val EXTRA_OCCURRENCE_ID = "extra_occurrence_id"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"

        fun createCompleteIntent(context: Context, occurrenceId: String, reminderId: String): PendingIntent {
            val intent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = ACTION_COMPLETE
                putExtra(EXTRA_OCCURRENCE_ID, occurrenceId)
                putExtra(EXTRA_REMINDER_ID, reminderId)
                data = Uri.parse("dayflow://action/complete/$occurrenceId")
            }
            return PendingIntent.getBroadcast(
                context,
                (occurrenceId.hashCode() xor ACTION_COMPLETE.hashCode()) and 0x7FFFFFFF,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        fun createSnoozeIntent(context: Context, occurrenceId: String, reminderId: String): PendingIntent {
            val intent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = ACTION_SNOOZE
                putExtra(EXTRA_OCCURRENCE_ID, occurrenceId)
                putExtra(EXTRA_REMINDER_ID, reminderId)
                data = Uri.parse("dayflow://action/snooze/$occurrenceId")
            }
            return PendingIntent.getBroadcast(
                context,
                (occurrenceId.hashCode() xor ACTION_SNOOZE.hashCode()) and 0x7FFFFFFF,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        fun createSkipIntent(context: Context, occurrenceId: String, reminderId: String): PendingIntent {
            val intent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = ACTION_SKIP
                putExtra(EXTRA_OCCURRENCE_ID, occurrenceId)
                putExtra(EXTRA_REMINDER_ID, reminderId)
                data = Uri.parse("dayflow://action/skip/$occurrenceId")
            }
            return PendingIntent.getBroadcast(
                context,
                (occurrenceId.hashCode() xor ACTION_SKIP.hashCode()) and 0x7FFFFFFF,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        fun createDismissIntent(context: Context, occurrenceId: String, reminderId: String): PendingIntent {
            val intent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = ACTION_DISMISS
                putExtra(EXTRA_OCCURRENCE_ID, occurrenceId)
                putExtra(EXTRA_REMINDER_ID, reminderId)
                data = Uri.parse("dayflow://action/dismiss/$occurrenceId")
            }
            return PendingIntent.getBroadcast(
                context,
                (occurrenceId.hashCode() xor ACTION_DISMISS.hashCode()) and 0x7FFFFFFF,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
