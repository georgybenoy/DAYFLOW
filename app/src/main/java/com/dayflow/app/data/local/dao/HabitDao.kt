package com.dayflow.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dayflow.app.data.local.entity.HabitEntity
import com.dayflow.app.data.local.entity.HabitLogEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface HabitDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity): Long

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Delete
    suspend fun deleteHabit(habit: HabitEntity)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteHabitById(id: String)

    @Query("SELECT * FROM habits WHERE id = :id LIMIT 1")
    fun getHabitById(id: String): Flow<HabitEntity?>

    @Query("SELECT * FROM habits WHERE id = :id LIMIT 1")
    suspend fun getHabitByIdDirect(id: String): HabitEntity?

    @Query("SELECT * FROM habits ORDER BY createdAt DESC")
    fun getAllHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE isEnabled = 1 ORDER BY name ASC")
    fun getActiveHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE isEnabled = 1 ORDER BY name ASC")
    suspend fun getActiveHabitsDirect(): List<HabitEntity>

    // Habit Logs
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateHabitLog(log: HabitLogEntity): Long

    @Update
    suspend fun updateHabitLog(log: HabitLogEntity)

    @Delete
    suspend fun deleteHabitLog(log: HabitLogEntity)

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND localDate = :date LIMIT 1")
    fun getLogForHabitAndDate(habitId: String, date: LocalDate): Flow<HabitLogEntity?>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND localDate = :date LIMIT 1")
    suspend fun getLogForHabitAndDateDirect(habitId: String, date: LocalDate): HabitLogEntity?

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId ORDER BY localDate DESC")
    fun getLogsForHabit(habitId: String): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs WHERE localDate = :date")
    fun getLogsForDate(date: LocalDate): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND localDate BETWEEN :startDate AND :endDate ORDER BY localDate ASC")
    fun getLogsForHabitInRange(habitId: String, startDate: LocalDate, endDate: LocalDate): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND localDate BETWEEN :startDate AND :endDate ORDER BY localDate ASC")
    suspend fun getLogsForHabitInRangeDirect(habitId: String, startDate: LocalDate, endDate: LocalDate): List<HabitLogEntity>

    @Query("SELECT * FROM habit_logs WHERE localDate BETWEEN :startDate AND :endDate")
    fun getLogsInRange(startDate: LocalDate, endDate: LocalDate): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId ORDER BY localDate ASC")
    suspend fun getLogsForHabitDirect(habitId: String): List<HabitLogEntity>
}
