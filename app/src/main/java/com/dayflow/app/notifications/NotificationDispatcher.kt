package com.dayflow.app.notifications

import com.dayflow.app.domain.model.Reminder
import com.dayflow.app.domain.model.ReminderOccurrence

interface NotificationDispatcher {

    /**
     * Dispatches an Android system notification for a triggered reminder occurrence.
     *
     * @param reminder The parent reminder configuration.
     * @param occurrence The specific occurrence triggering delivery.
     * @param isSilent If true (e.g. during quiet hours), notification is posted silently.
     */
    fun dispatchReminderNotification(
        reminder: Reminder,
        occurrence: ReminderOccurrence,
        isSilent: Boolean = false
    )

    /**
     * Dismisses/cancels an active notification associated with [occurrenceId].
     */
    fun cancelReminderNotification(occurrenceId: String)

    /**
     * Dispatches the daily morning briefing notification on CHANNEL_DAILY_BRIEFING.
     * Features an "Open Plan" action button.
     */
    fun dispatchBriefingNotification(
        title: String,
        summary: String,
        date: java.time.LocalDate = java.time.LocalDate.now()
    )

    /**
     * Dispatches the evening review notification on CHANNEL_EVENING_REVIEW.
     * Features a "Start Review" action button.
     */
    fun dispatchReviewNotification(
        title: String,
        summary: String,
        date: java.time.LocalDate = java.time.LocalDate.now()
    )
}
