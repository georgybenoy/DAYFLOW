package com.dayflow.app.scheduling.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dayflow.app.core.common.IoDispatcher
import com.dayflow.app.domain.model.OccurrenceStatus
import com.dayflow.app.domain.model.Priority
import com.dayflow.app.domain.model.Reminder
import com.dayflow.app.domain.model.ReminderOccurrence
import com.dayflow.app.domain.model.ReminderType
import com.dayflow.app.domain.recurrence.RecurrenceCalculator
import com.dayflow.app.domain.repository.ReminderRepository
import com.dayflow.app.domain.repository.SettingsRepository
import com.dayflow.app.notifications.NotificationDispatcher
import com.dayflow.app.scheduling.AlarmScheduler
import com.dayflow.app.domain.usecase.GetBriefingUseCase
import com.dayflow.app.scheduling.WorkflowReminderScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalTime
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var reminderRepository: ReminderRepository

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    @Inject
    lateinit var notificationDispatcher: NotificationDispatcher

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var getBriefingUseCase: GetBriefingUseCase

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    override fun onReceive(context: Context, intent: Intent) {
        val occurrenceId = intent.getStringExtra(EXTRA_OCCURRENCE_ID) ?: return
        val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return

        val pendingResult = goAsync()
        CoroutineScope(ioDispatcher).launch {
            try {
                handleAlarm(occurrenceId, reminderId)
            } finally {
                pendingResult?.finish()
            }
        }
    }

    internal suspend fun handleAlarm(occurrenceId: String, reminderId: String) {
        val occurrence = reminderRepository.getOccurrenceByIdDirect(occurrenceId) ?: return
        val reminder = reminderRepository.getReminderByIdDirect(reminderId) ?: return

        // 1. Idempotency Guard: Only deliver if still in SCHEDULED or SNOOZED state
        if (occurrence.status != OccurrenceStatus.SCHEDULED && occurrence.status != OccurrenceStatus.SNOOZED) {
            return
        }

        // 2. Quiet Hours Check
        val profile = settingsRepository.userProfile.first()
        val nowLocalTime = LocalTime.now()
        val isQuietTime = isTimeInQuietHours(nowLocalTime, profile.quietHoursStart, profile.quietHoursEnd)
        val shouldSilence = isQuietTime && reminder.priority != Priority.URGENT

        // 3. Dispatch Notification
        if (WorkflowReminderScheduler.isWorkflowReminder(reminderId)) {
            val today = java.time.LocalDate.now()
            if (reminderId == WorkflowReminderScheduler.REMINDER_ID_BRIEFING) {
                val briefing = getBriefingUseCase(today)
                notificationDispatcher.dispatchBriefingNotification(
                    title = briefing.greeting,
                    summary = briefing.summaryText,
                    date = today
                )
            } else if (reminderId == WorkflowReminderScheduler.REMINDER_ID_REVIEW) {
                notificationDispatcher.dispatchReviewNotification(
                    title = "Evening Review Ready",
                    summary = "Take 2 minutes to review today's achievements and prepare tomorrow's plan.",
                    date = today
                )
            }
        } else {
            notificationDispatcher.dispatchReminderNotification(
                reminder = reminder,
                occurrence = occurrence,
                isSilent = shouldSilence
            )
        }

        // 4. Mark occurrence as DELIVERED
        if (!occurrenceId.startsWith("debug_test")) {
            val now = Instant.now()
            val deliveredOccurrence = occurrence.copy(
                status = OccurrenceStatus.DELIVERED,
                deliveredAt = now
            )
            reminderRepository.updateOccurrence(deliveredOccurrence)

            // 5. Compute and schedule next occurrence
            when (reminder.type) {
                ReminderType.COMPLETION_INTERVAL -> {
                    // Completion intervals do not advance on delivery; they strictly advance
                    // upon user interaction (Complete or Skip) in NotificationActionReceiver.
                }
                ReminderType.ONE_TIME,
                ReminderType.DAILY,
                ReminderType.WEEKLY,
                ReminderType.CUSTOM_WEEKDAYS,
                ReminderType.FIXED_INTERVAL -> {
                    val nextInstant = RecurrenceCalculator.nextOccurrence(
                        reminder = reminder,
                        after = occurrence.scheduledAt,
                        zoneId = reminder.timeZoneId
                    )

                    if (nextInstant != null) {
                        val nextOccurrence = ReminderOccurrence(
                            reminderId = reminder.id,
                            scheduledAt = nextInstant,
                            status = OccurrenceStatus.SCHEDULED
                        )

                        if (reminderRepository.scheduleOccurrence(nextOccurrence)) {
                            alarmScheduler.scheduleOccurrence(nextOccurrence, reminder)
                        }
                    }
                }
            }
        }
    }

    private fun isTimeInQuietHours(time: LocalTime, start: LocalTime, end: LocalTime): Boolean {
        return if (start.isBefore(end)) {
            time >= start && time <= end
        } else {
            // Midnight-crossing quiet hours (e.g. 22:00 to 07:00)
            time >= start || time <= end
        }
    }

    companion object {
        const val ACTION_ALARM_TRIGGER = "com.dayflow.app.action.ALARM_TRIGGER"
        const val EXTRA_OCCURRENCE_ID = "extra_occurrence_id"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
    }
}
