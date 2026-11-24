package com.dayflow.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dayflow.app.data.local.entity.AppSettingEntity
import com.dayflow.app.data.local.entity.DailyReviewEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface DailyReviewDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: DailyReviewEntity): Long

    @Update
    suspend fun updateReview(review: DailyReviewEntity)

    @Query("SELECT * FROM daily_reviews WHERE localDate = :date LIMIT 1")
    fun getReviewByDate(date: LocalDate): Flow<DailyReviewEntity?>

    @Query("SELECT * FROM daily_reviews WHERE localDate = :date LIMIT 1")
    suspend fun getReviewByDateDirect(date: LocalDate): DailyReviewEntity?

    @Query("SELECT * FROM daily_reviews ORDER BY localDate DESC")
    fun getAllReviews(): Flow<List<DailyReviewEntity>>
}

@Dao
interface AppSettingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSetting(setting: AppSettingEntity): Long

    @Query("SELECT * FROM app_settings WHERE `key` = :key LIMIT 1")
    fun getSetting(key: String): Flow<AppSettingEntity?>

    @Query("SELECT * FROM app_settings WHERE `key` = :key LIMIT 1")
    suspend fun getSettingDirect(key: String): AppSettingEntity?

    @Query("DELETE FROM app_settings WHERE `key` = :key")
    suspend fun deleteSetting(key: String)
}
