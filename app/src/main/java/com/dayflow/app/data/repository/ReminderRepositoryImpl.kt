package com.dayflow.app.data.repository

import com.dayflow.app.core.common.IoDispatcher
import com.dayflow.app.data.local.dao.ReminderDao
import com.dayflow.app.data.local.dao.ReminderOccurrenceDao
import com.dayflow.app.data.mapper.toDomain
import com.dayflow.app.data.mapper.toEntity
import com.dayflow.app.domain.model.Reminder
import com.dayflow.app.domain.model.ReminderOccurrence
import com.dayflow.app.domain.repository.ReminderRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRepositoryImpl @Inject constructor(
    private val reminderDao: ReminderDao,
    private val occurrenceDao: ReminderOccurrenceDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ReminderRepository {

    override fun getAllReminders(): Flow<List<Reminder>> {
        return reminderDao.getAllReminders().map { list -> list.map { it.toDomain() } }
    }

    override fun getStandaloneReminders(): Flow<List<Reminder>> {
        return reminderDao.getStandaloneReminders().map { list -> list.map { it.toDomain() } }
    }

    override fun getDeadlineRemindersForTask(taskId: String): Flow<List<Reminder>> {
        return reminderDao.getDeadlineRemindersForTask(taskId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getDeadlineRemindersForTaskDirect(taskId: String): List<Reminder> = withContext(ioDispatcher) {
        reminderDao.getDeadlineRemindersForTaskDirect(taskId).map { it.toDomain() }
    }

    override fun getActiveReminders(): Flow<List<Reminder>> {
        return reminderDao.getActiveReminders().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getActiveRemindersDirect(): List<Reminder> = withContext(ioDispatcher) {
        reminderDao.getActiveRemindersDirect().map { it.toDomain() }
    }

    override fun getPausedReminders(): Flow<List<Reminder>> {
        return reminderDao.getPausedReminders().map { list -> list.map { it.toDomain() } }
    }

    override fun getReminderById(id: String): Flow<Reminder?> {
        return reminderDao.getReminderById(id).map { it?.toDomain() }
    }

    override suspend fun getReminderByIdDirect(id: String): Reminder? = withContext(ioDispatcher) {
        reminderDao.getReminderByIdDirect(id)?.toDomain()
    }

    override suspend fun saveReminder(reminder: Reminder) = withContext(ioDispatcher) {
        val existing = reminderDao.getReminderByIdDirect(reminder.id)
        if (existing == null) {
            reminderDao.insertReminder(reminder.toEntity())
        } else {
            reminderDao.updateReminder(reminder.toEntity())
        }
        Unit
    }

    override suspend fun deleteReminder(reminder: Reminder) = withContext(ioDispatcher) {
        reminderDao.deleteReminder(reminder.toEntity())
    }

    override suspend fun deleteReminderById(id: String) = withContext(ioDispatcher) {
        reminderDao.deleteReminderById(id)
    }

    override fun getOccurrencesForReminder(reminderId: String): Flow<List<ReminderOccurrence>> {
        return occurrenceDao.getOccurrencesForReminder(reminderId).map { list -> list.map { it.toDomain() } }
    }

    override fun getOccurrencesBetween(startMillis: Long, endMillis: Long): Flow<List<ReminderOccurrence>> {
        return occurrenceDao.getOccurrencesBetween(startMillis, endMillis).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getOccurrencesBetweenDirect(startMillis: Long, endMillis: Long): List<ReminderOccurrence> = withContext(ioDispatcher) {
        occurrenceDao.getOccurrencesBetweenDirect(startMillis, endMillis).map { it.toDomain() }
    }

    override fun getUpcomingOccurrences(fromInstant: Instant, limit: Int): Flow<List<ReminderOccurrence>> {
        return occurrenceDao.getUpcomingOccurrences(fromInstant.toEpochMilli(), limit).map { list -> list.map { it.toDomain() } }
    }

    override fun getOccurrenceById(id: String): Flow<ReminderOccurrence?> {
        return occurrenceDao.getOccurrenceById(id).map { it?.toDomain() }
    }

    override suspend fun getOccurrenceByIdDirect(id: String): ReminderOccurrence? = withContext(ioDispatcher) {
        occurrenceDao.getOccurrenceByIdDirect(id)?.toDomain()
    }

    override suspend fun getPendingOccurrencesDirect(): List<ReminderOccurrence> = withContext(ioDispatcher) {
        occurrenceDao.getPendingOccurrencesDirect().map { it.toDomain() }
    }

    override suspend fun scheduleOccurrence(occurrence: ReminderOccurrence): Boolean = withContext(ioDispatcher) {
        try {
            occurrenceDao.insertOccurrence(occurrence.toEntity()) > 0
        } catch (e: Exception) {
            // Unique constraint violation (idempotency guard)
            false
        }
    }

    override suspend fun scheduleOccurrences(occurrences: List<ReminderOccurrence>) = withContext(ioDispatcher) {
        occurrenceDao.insertOccurrences(occurrences.map { it.toEntity() })
        Unit
    }

    override suspend fun updateOccurrence(occurrence: ReminderOccurrence) = withContext(ioDispatcher) {
        occurrenceDao.updateOccurrence(occurrence.toEntity())
    }

    override suspend fun deleteOccurrenceById(id: String) = withContext(ioDispatcher) {
        occurrenceDao.deleteOccurrenceById(id)
    }

    override suspend fun deleteOccurrencesByReminder(reminderId: String) = withContext(ioDispatcher) {
        occurrenceDao.deleteOccurrencesByReminder(reminderId)
    }
}
