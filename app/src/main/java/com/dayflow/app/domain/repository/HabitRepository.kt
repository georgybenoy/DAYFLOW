package com.dayflow.app.domain.repository

import com.dayflow.app.domain.model.Habit
import com.dayflow.app.domain.model.HabitLog
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface HabitRepository {
    fun getAllHabits(): Flow<List<Habit>>
    fun getActiveHabits(): Flow<List<Habit>>
    fun getHabitById(id: String): Flow<Habit?>
    suspend fun getHabitByIdDirect(id: String): Habit?
    suspend fun getActiveHabitsDirect(): List<Habit>

    suspend fun saveHabit(habit: Habit)
    suspend fun deleteHabit(habit: Habit)
    suspend fun deleteHabitById(id: String)

    // Logs
    fun getLogForHabitAndDate(habitId: String, date: LocalDate): Flow<HabitLog?>
    suspend fun getLogForHabitAndDateDirect(habitId: String, date: LocalDate): HabitLog?
    fun getLogsForHabit(habitId: String): Flow<List<HabitLog>>
    suspend fun getLogsForHabitDirect(habitId: String): List<HabitLog>
    fun getLogsForDate(date: LocalDate): Flow<List<HabitLog>>
    fun getLogsForHabitInRange(habitId: String, startDate: LocalDate, endDate: LocalDate): Flow<List<HabitLog>>
    suspend fun getLogsForHabitInRangeDirect(habitId: String, startDate: LocalDate, endDate: LocalDate): List<HabitLog>
    fun getLogsInRange(startDate: LocalDate, endDate: LocalDate): Flow<List<HabitLog>>
    suspend fun logHabitProgress(log: HabitLog)
    suspend fun deleteHabitLog(log: HabitLog)
}
