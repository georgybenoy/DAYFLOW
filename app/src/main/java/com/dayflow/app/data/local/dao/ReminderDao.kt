package com.dayflow.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.dayflow.app.data.local.entity.ReminderEntity
import com.dayflow.app.data.local.entity.ReminderOccurrenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Delete
    suspend fun deleteReminder(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminderById(id: String)

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    fun getReminderById(id: String): Flow<ReminderEntity?>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun getReminderByIdDirect(id: String): ReminderEntity?

    @Query("SELECT * FROM reminders ORDER BY createdAt DESC")
    fun getAllReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE isEnabled = 1 AND isPaused = 0 ORDER BY priority ASC, startTime ASC")
    fun getActiveReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE isEnabled = 1 AND isPaused = 0")
    suspend fun getActiveRemindersDirect(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE isPaused = 1 ORDER BY updatedAt DESC")
    fun getPausedReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE categoryId = :categoryId ORDER BY startTime ASC")
    fun getRemindersByCategory(categoryId: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE id NOT LIKE 'task_deadline_%' ORDER BY createdAt DESC")
    fun getStandaloneReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE id LIKE 'task_deadline_' || :taskId || '%'")
    fun getDeadlineRemindersForTask(taskId: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE id LIKE 'task_deadline_' || :taskId || '%'")
    suspend fun getDeadlineRemindersForTaskDirect(taskId: String): List<ReminderEntity>
}

@Dao
interface ReminderOccurrenceDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertOccurrence(occurrence: ReminderOccurrenceEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOccurrences(occurrences: List<ReminderOccurrenceEntity>): List<Long>

    @Update
    suspend fun updateOccurrence(occurrence: ReminderOccurrenceEntity)

    @Delete
    suspend fun deleteOccurrence(occurrence: ReminderOccurrenceEntity)

    @Query("SELECT * FROM reminder_occurrences WHERE id = :id LIMIT 1")
    fun getOccurrenceById(id: String): Flow<ReminderOccurrenceEntity?>

    @Query("SELECT * FROM reminder_occurrences WHERE id = :id LIMIT 1")
    suspend fun getOccurrenceByIdDirect(id: String): ReminderOccurrenceEntity?

    @Query("SELECT * FROM reminder_occurrences WHERE status = 'SCHEDULED' OR status = 'SNOOZED' ORDER BY scheduledAt ASC")
    suspend fun getPendingOccurrencesDirect(): List<ReminderOccurrenceEntity>

    @Query("DELETE FROM reminder_occurrences WHERE id = :id")
    suspend fun deleteOccurrenceById(id: String)

    @Query("SELECT * FROM reminder_occurrences WHERE reminderId = :reminderId ORDER BY scheduledAt ASC")
    fun getOccurrencesForReminder(reminderId: String): Flow<List<ReminderOccurrenceEntity>>

    @Query("SELECT * FROM reminder_occurrences WHERE status = 'SCHEDULED' AND scheduledAt >= :fromMillis ORDER BY scheduledAt ASC LIMIT :limit")
    fun getUpcomingOccurrences(fromMillis: Long, limit: Int = 20): Flow<List<ReminderOccurrenceEntity>>

    @Query("SELECT * FROM reminder_occurrences WHERE scheduledAt >= :fromMillis ORDER BY scheduledAt ASC")
    fun getOccurrencesFrom(fromMillis: Long): Flow<List<ReminderOccurrenceEntity>>

    @Query("SELECT * FROM reminder_occurrences WHERE scheduledAt BETWEEN :startMillis AND :endMillis ORDER BY scheduledAt ASC")
    fun getOccurrencesBetween(startMillis: Long, endMillis: Long): Flow<List<ReminderOccurrenceEntity>>

    @Query("SELECT * FROM reminder_occurrences WHERE scheduledAt BETWEEN :startMillis AND :endMillis ORDER BY scheduledAt ASC")
    suspend fun getOccurrencesBetweenDirect(startMillis: Long, endMillis: Long): List<ReminderOccurrenceEntity>

    @Query("SELECT * FROM reminder_occurrences WHERE reminderId = :reminderId AND scheduledAt = :scheduledAtMillis LIMIT 1")
    suspend fun getOccurrenceByReminderAndSchedule(reminderId: String, scheduledAtMillis: Long): ReminderOccurrenceEntity?

    @Query("DELETE FROM reminder_occurrences WHERE reminderId = :reminderId")
    suspend fun deleteOccurrencesByReminder(reminderId: String)
}
