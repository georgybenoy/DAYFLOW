package com.dayflow.app.scheduling

import com.dayflow.app.domain.model.OccurrenceStatus
import com.dayflow.app.domain.model.Priority
import com.dayflow.app.domain.model.Reminder
import com.dayflow.app.domain.model.ReminderOccurrence
import com.dayflow.app.domain.model.ReminderPrecision
import com.dayflow.app.domain.model.ReminderType
import com.dayflow.app.domain.recurrence.RecurrenceCalculator
import com.dayflow.app.domain.repository.ReminderRepository
import com.dayflow.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkflowReminderScheduler @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val alarmScheduler: AlarmScheduler,
    private val settingsRepository: SettingsRepository
) {

    suspend fun ensureWorkflowRemindersScheduled() {
        val profile = settingsRepository.userProfile.first()
        val zoneId = ZoneId.systemDefault()
        val now = Instant.now()
        val today = LocalDate.now(zoneId)

        // 1. Morning Briefing System Reminder
        val existingBriefing = reminderRepository.getReminderByIdDirect(REMINDER_ID_BRIEFING)
        val briefingReminder = existingBriefing?.copy(
            startTime = profile.morningBriefingTime,
            isEnabled = true
        ) ?: Reminder(
            id = REMINDER_ID_BRIEFING,
            title = "Morning Briefing",
            description = "Start your day with your plan and priorities",
            categoryId = null,
            type = ReminderType.DAILY,
            startDate = today,
            startTime = profile.morningBriefingTime,
            priority = Priority.MEDIUM,
            precision = ReminderPrecision.EXACT,
            isEnabled = true,
            timeZoneId = zoneId
        )
        reminderRepository.saveReminder(briefingReminder)

        // Check if briefing occurrence is scheduled
        val pendingBriefing = reminderRepository.getPendingOccurrencesDirect()
            .find { it.reminderId == REMINDER_ID_BRIEFING && (it.status == OccurrenceStatus.SCHEDULED || it.status == OccurrenceStatus.SNOOZED) }

        if (pendingBriefing == null) {
            val nextBriefingInstant = RecurrenceCalculator.nextOccurrence(
                reminder = briefingReminder,
                after = now.minusSeconds(1),
                zoneId = zoneId
            )
            if (nextBriefingInstant != null) {
                val occ = ReminderOccurrence(
                    reminderId = REMINDER_ID_BRIEFING,
                    scheduledAt = nextBriefingInstant,
                    status = OccurrenceStatus.SCHEDULED
                )
                if (reminderRepository.scheduleOccurrence(occ)) {
                    alarmScheduler.scheduleOccurrence(occ, briefingReminder)
                }
            }
        }

        // 2. Evening Review System Reminder
        val existingReview = reminderRepository.getReminderByIdDirect(REMINDER_ID_REVIEW)
        val reviewReminder = existingReview?.copy(
            startTime = profile.eveningReviewTime,
            isEnabled = true
        ) ?: Reminder(
            id = REMINDER_ID_REVIEW,
            title = "Evening Review",
            description = "Review today's achievements and prepare tomorrow",
            categoryId = null,
            type = ReminderType.DAILY,
            startDate = today,
            startTime = profile.eveningReviewTime,
            priority = Priority.MEDIUM,
            precision = ReminderPrecision.EXACT,
            isEnabled = true,
            timeZoneId = zoneId
        )
        reminderRepository.saveReminder(reviewReminder)

        val pendingReview = reminderRepository.getPendingOccurrencesDirect()
            .find { it.reminderId == REMINDER_ID_REVIEW && (it.status == OccurrenceStatus.SCHEDULED || it.status == OccurrenceStatus.SNOOZED) }

        if (pendingReview == null) {
            val nextReviewInstant = RecurrenceCalculator.nextOccurrence(
                reminder = reviewReminder,
                after = now.minusSeconds(1),
                zoneId = zoneId
            )
            if (nextReviewInstant != null) {
                val occ = ReminderOccurrence(
                    reminderId = REMINDER_ID_REVIEW,
                    scheduledAt = nextReviewInstant,
                    status = OccurrenceStatus.SCHEDULED
                )
                if (reminderRepository.scheduleOccurrence(occ)) {
                    alarmScheduler.scheduleOccurrence(occ, reviewReminder)
                }
            }
        }
    }

    suspend fun rearmNextOccurrence(reminderId: String) {
        val reminder = reminderRepository.getReminderByIdDirect(reminderId) ?: return
        val pendingOccurrences = reminderRepository.getPendingOccurrencesDirect()
            .filter { it.reminderId == reminderId }

        val now = Instant.now()
        for (occ in pendingOccurrences) {
            reminderRepository.updateOccurrence(occ.copy(status = OccurrenceStatus.DELIVERED, deliveredAt = now))
        }

        val nextInstant = RecurrenceCalculator.nextOccurrence(
            reminder = reminder,
            after = now,
            zoneId = reminder.timeZoneId
        )
        if (nextInstant != null) {
            val newOcc = ReminderOccurrence(
                reminderId = reminderId,
                scheduledAt = nextInstant,
                status = OccurrenceStatus.SCHEDULED
            )
            if (reminderRepository.scheduleOccurrence(newOcc)) {
                alarmScheduler.scheduleOccurrence(newOcc, reminder)
            }
        }
    }

    suspend fun syncWorkflowTimes(briefingTime: LocalTime, reviewTime: LocalTime) {
        val zoneId = ZoneId.systemDefault()
        val now = Instant.now()

        // 1. Update Morning Briefing
        val briefing = reminderRepository.getReminderByIdDirect(REMINDER_ID_BRIEFING)
        if (briefing != null) {
            val updated = briefing.copy(startTime = briefingTime)
            reminderRepository.saveReminder(updated)

            // Cancel old pending occurrences
            val pendingBriefing = reminderRepository.getPendingOccurrencesDirect()
                .filter { it.reminderId == REMINDER_ID_BRIEFING }
            for (occ in pendingBriefing) {
                alarmScheduler.cancelOccurrence(occ.id, REMINDER_ID_BRIEFING)
                reminderRepository.updateOccurrence(occ.copy(status = OccurrenceStatus.CANCELLED))
            }

            // Schedule new next occurrence
            val nextInstant = RecurrenceCalculator.nextOccurrence(
                reminder = updated,
                after = now.minusSeconds(1),
                zoneId = zoneId
            )
            if (nextInstant != null) {
                val newOcc = ReminderOccurrence(
                    reminderId = REMINDER_ID_BRIEFING,
                    scheduledAt = nextInstant,
                    status = OccurrenceStatus.SCHEDULED
                )
                if (reminderRepository.scheduleOccurrence(newOcc)) {
                    alarmScheduler.scheduleOccurrence(newOcc, updated)
                }
            }
        }

        // 2. Update Evening Review
        val review = reminderRepository.getReminderByIdDirect(REMINDER_ID_REVIEW)
        if (review != null) {
            val updated = review.copy(startTime = reviewTime)
            reminderRepository.saveReminder(updated)

            // Cancel old pending occurrences
            val pendingReview = reminderRepository.getPendingOccurrencesDirect()
                .filter { it.reminderId == REMINDER_ID_REVIEW }
            for (occ in pendingReview) {
                alarmScheduler.cancelOccurrence(occ.id, REMINDER_ID_REVIEW)
                reminderRepository.updateOccurrence(occ.copy(status = OccurrenceStatus.CANCELLED))
            }

            // Schedule new next occurrence
            val nextInstant = RecurrenceCalculator.nextOccurrence(
                reminder = updated,
                after = now.minusSeconds(1),
                zoneId = zoneId
            )
            if (nextInstant != null) {
                val newOcc = ReminderOccurrence(
                    reminderId = REMINDER_ID_REVIEW,
                    scheduledAt = nextInstant,
                    status = OccurrenceStatus.SCHEDULED
                )
                if (reminderRepository.scheduleOccurrence(newOcc)) {
                    alarmScheduler.scheduleOccurrence(newOcc, updated)
                }
            }
        }
    }

    companion object {
        const val REMINDER_ID_BRIEFING = "system_morning_briefing"
        const val REMINDER_ID_REVIEW = "system_evening_review"

        fun isWorkflowReminder(reminderId: String): Boolean {
            return reminderId == REMINDER_ID_BRIEFING || reminderId == REMINDER_ID_REVIEW
        }
    }
}
