package com.dayflow.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.dayflow.app.data.local.entity.DailyPlanEntity
import com.dayflow.app.data.local.entity.DailyPlanItemEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface DailyPlanDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: DailyPlanEntity): Long

    @Update
    suspend fun updatePlan(plan: DailyPlanEntity)

    @Delete
    suspend fun deletePlan(plan: DailyPlanEntity)

    @Query("SELECT * FROM daily_plans WHERE localDate = :date LIMIT 1")
    fun getPlanByDate(date: LocalDate): Flow<DailyPlanEntity?>

    @Query("SELECT * FROM daily_plans WHERE localDate = :date LIMIT 1")
    suspend fun getPlanByDateDirect(date: LocalDate): DailyPlanEntity?

    @Query("SELECT * FROM daily_plans WHERE localDate BETWEEN :startDate AND :endDate ORDER BY localDate ASC")
    fun getPlansBetween(startDate: LocalDate, endDate: LocalDate): Flow<List<DailyPlanEntity>>

    @Query("SELECT * FROM daily_plans WHERE localDate BETWEEN :startDate AND :endDate ORDER BY localDate ASC")
    suspend fun getPlansBetweenDirect(startDate: LocalDate, endDate: LocalDate): List<DailyPlanEntity>

    @Query("SELECT * FROM daily_plans WHERE id = :id LIMIT 1")
    fun getPlanById(id: String): Flow<DailyPlanEntity?>

    // Plan Items
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanItems(items: List<DailyPlanItemEntity>): List<Long>

    @Update
    suspend fun updatePlanItem(item: DailyPlanItemEntity)

    @Query("DELETE FROM daily_plan_items WHERE planId = :planId")
    suspend fun deleteItemsForPlan(planId: String)

    @Query("SELECT * FROM daily_plan_items WHERE planId = :planId ORDER BY orderIndex ASC")
    fun getItemsForPlan(planId: String): Flow<List<DailyPlanItemEntity>>

    @Query("SELECT * FROM daily_plan_items WHERE planId = :planId ORDER BY orderIndex ASC")
    suspend fun getItemsForPlanDirect(planId: String): List<DailyPlanItemEntity>

    @Transaction
    suspend fun savePlanWithItems(plan: DailyPlanEntity, items: List<DailyPlanItemEntity>) {
        insertPlan(plan)
        deleteItemsForPlan(plan.id)
        if (items.isNotEmpty()) {
            insertPlanItems(items)
        }
    }
}
