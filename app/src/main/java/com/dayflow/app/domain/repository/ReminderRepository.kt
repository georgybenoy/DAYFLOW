package com.dayflow.app.domain.repository

import com.dayflow.app.domain.model.Reminder
import com.dayflow.app.domain.model.ReminderOccurrence
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface ReminderRepository {
    fun getAllReminders(): Flow<List<Reminder>>
    fun getStandaloneReminders(): Flow<List<Reminder>>
    fun getActiveReminders(): Flow<List<Reminder>>
    suspend fun getActiveRemindersDirect(): List<Reminder>
    fun getPausedReminders(): Flow<List<Reminder>>
    fun getReminderById(id: String): Flow<Reminder?>
    suspend fun getReminderByIdDirect(id: String): Reminder?
    fun getDeadlineRemindersForTask(taskId: String): Flow<List<Reminder>>
    suspend fun getDeadlineRemindersForTaskDirect(taskId: String): List<Reminder>
    suspend fun saveReminder(reminder: Reminder)
    suspend fun deleteReminder(reminder: Reminder)
    suspend fun deleteReminderById(id: String)

    // Occurrences
    fun getOccurrencesForReminder(reminderId: String): Flow<List<ReminderOccurrence>>
    fun getOccurrencesBetween(startMillis: Long, endMillis: Long): Flow<List<ReminderOccurrence>>
    suspend fun getOccurrencesBetweenDirect(startMillis: Long, endMillis: Long): List<ReminderOccurrence>
    fun getUpcomingOccurrences(fromInstant: Instant, limit: Int = 20): Flow<List<ReminderOccurrence>>
    fun getOccurrenceById(id: String): Flow<ReminderOccurrence?>
    suspend fun getOccurrenceByIdDirect(id: String): ReminderOccurrence?
    suspend fun getPendingOccurrencesDirect(): List<ReminderOccurrence>
    suspend fun scheduleOccurrence(occurrence: ReminderOccurrence): Boolean
    suspend fun scheduleOccurrences(occurrences: List<ReminderOccurrence>)
    suspend fun updateOccurrence(occurrence: ReminderOccurrence)
    suspend fun deleteOccurrenceById(id: String)
    suspend fun deleteOccurrencesByReminder(reminderId: String)
}
