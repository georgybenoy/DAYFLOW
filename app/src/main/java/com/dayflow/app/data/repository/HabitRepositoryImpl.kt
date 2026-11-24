package com.dayflow.app.data.repository

import com.dayflow.app.core.common.IoDispatcher
import com.dayflow.app.data.local.dao.HabitDao
import com.dayflow.app.data.mapper.toDomain
import com.dayflow.app.data.mapper.toEntity
import com.dayflow.app.domain.model.Habit
import com.dayflow.app.domain.model.HabitLog
import com.dayflow.app.domain.repository.HabitRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HabitRepositoryImpl @Inject constructor(
    private val habitDao: HabitDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : HabitRepository {

    override fun getAllHabits(): Flow<List<Habit>> {
        return habitDao.getAllHabits().map { list -> list.map { it.toDomain() } }
    }

    override fun getActiveHabits(): Flow<List<Habit>> {
        return habitDao.getActiveHabits().map { list -> list.map { it.toDomain() } }
    }

    override fun getHabitById(id: String): Flow<Habit?> {
        return habitDao.getHabitById(id).map { it?.toDomain() }
    }

    override suspend fun getHabitByIdDirect(id: String): Habit? = withContext(ioDispatcher) {
        habitDao.getHabitByIdDirect(id)?.toDomain()
    }

    override suspend fun getActiveHabitsDirect(): List<Habit> = withContext(ioDispatcher) {
        habitDao.getActiveHabitsDirect().map { it.toDomain() }
    }

    override suspend fun saveHabit(habit: Habit) = withContext(ioDispatcher) {
        habitDao.insertHabit(habit.toEntity())
        Unit
    }

    override suspend fun deleteHabit(habit: Habit) = withContext(ioDispatcher) {
        habitDao.deleteHabit(habit.toEntity())
    }

    override suspend fun deleteHabitById(id: String) = withContext(ioDispatcher) {
        habitDao.deleteHabitById(id)
    }

    override fun getLogForHabitAndDate(habitId: String, date: LocalDate): Flow<HabitLog?> {
        return habitDao.getLogForHabitAndDate(habitId, date).map { it?.toDomain() }
    }

    override suspend fun getLogForHabitAndDateDirect(habitId: String, date: LocalDate): HabitLog? = withContext(ioDispatcher) {
        habitDao.getLogForHabitAndDateDirect(habitId, date)?.toDomain()
    }

    override fun getLogsForHabit(habitId: String): Flow<List<HabitLog>> {
        return habitDao.getLogsForHabit(habitId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getLogsForHabitDirect(habitId: String): List<HabitLog> = withContext(ioDispatcher) {
        habitDao.getLogsForHabitDirect(habitId).map { it.toDomain() }
    }

    override fun getLogsForDate(date: LocalDate): Flow<List<HabitLog>> {
        return habitDao.getLogsForDate(date).map { list -> list.map { it.toDomain() } }
    }

    override fun getLogsForHabitInRange(
        habitId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<HabitLog>> {
        return habitDao.getLogsForHabitInRange(habitId, startDate, endDate).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getLogsForHabitInRangeDirect(
        habitId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<HabitLog> = withContext(ioDispatcher) {
        habitDao.getLogsForHabitInRangeDirect(habitId, startDate, endDate).map { it.toDomain() }
    }

    override fun getLogsInRange(startDate: LocalDate, endDate: LocalDate): Flow<List<HabitLog>> {
        return habitDao.getLogsInRange(startDate, endDate).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun logHabitProgress(log: HabitLog) = withContext(ioDispatcher) {
        habitDao.insertOrUpdateHabitLog(log.toEntity())
        Unit
    }

    override suspend fun deleteHabitLog(log: HabitLog) = withContext(ioDispatcher) {
        habitDao.deleteHabitLog(log.toEntity())
    }
}
