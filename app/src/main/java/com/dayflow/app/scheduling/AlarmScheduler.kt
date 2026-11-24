package com.dayflow.app.scheduling

import com.dayflow.app.domain.model.Reminder
import com.dayflow.app.domain.model.ReminderOccurrence
import kotlinx.coroutines.flow.Flow

/**
 * Interface responsible for managing system alarms via AlarmManager.
 * Enforces one-shot scheduling, idempotent PendingIntent derivation, and
 * graceful inexact fallback.
 */
interface AlarmScheduler {

    /**
     * Observable flow emitting the current system scheduling capability.
     */
    val schedulingCapability: Flow<SchedulingCapability>

    /**
     * Synchronously queries current scheduling capability.
     */
    fun getSchedulingCapability(): SchedulingCapability

    /**
     * Schedules a one-shot system alarm for [occurrence].
     * Returns true if successfully scheduled, or false if scheduling failed.
     */
    suspend fun scheduleOccurrence(occurrence: ReminderOccurrence, reminder: Reminder): Boolean

    /**
     * Cancels an existing scheduled alarm for [occurrenceId].
     */
    suspend fun cancelOccurrence(occurrenceId: String, reminderId: String)

    /**
     * Cancels all alarms associated with [reminderId].
     */
    suspend fun cancelAllForReminder(reminderId: String)
}
