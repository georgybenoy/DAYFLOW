package com.dayflow.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.dayflow.app.data.local.entity.AppSettingEntity
import com.dayflow.app.data.local.entity.CategoryEntity
import com.dayflow.app.data.local.entity.DailyPlanEntity
import com.dayflow.app.data.local.entity.DailyPlanItemEntity
import com.dayflow.app.data.local.entity.DailyReviewEntity
import com.dayflow.app.data.local.entity.HabitEntity
import com.dayflow.app.data.local.entity.HabitLogEntity
import com.dayflow.app.data.local.entity.ReminderEntity
import com.dayflow.app.data.local.entity.ReminderOccurrenceEntity
import com.dayflow.app.data.local.entity.TaskEntity
import com.dayflow.app.data.local.entity.TaskSubtaskEntity

@Dao
interface BackupDao {

    // --- Export Queries ---

    @Query("SELECT * FROM categories ORDER BY id ASC")
    suspend fun getAllCategories(): List<CategoryEntity>

    @Query("SELECT * FROM reminders ORDER BY id ASC")
    suspend fun getAllReminders(): List<ReminderEntity>

    @Query("SELECT * FROM reminder_occurrences ORDER BY id ASC")
    suspend fun getAllReminderOccurrences(): List<ReminderOccurrenceEntity>

    @Query("SELECT * FROM tasks ORDER BY id ASC")
    suspend fun getAllTasks(): List<TaskEntity>

    @Query("SELECT * FROM task_subtasks ORDER BY id ASC")
    suspend fun getAllTaskSubtasks(): List<TaskSubtaskEntity>

    @Query("SELECT * FROM habits ORDER BY id ASC")
    suspend fun getAllHabits(): List<HabitEntity>

    @Query("SELECT * FROM habit_logs ORDER BY id ASC")
    suspend fun getAllHabitLogs(): List<HabitLogEntity>

    @Query("SELECT * FROM daily_plans ORDER BY id ASC")
    suspend fun getAllDailyPlans(): List<DailyPlanEntity>

    @Query("SELECT * FROM daily_plan_items ORDER BY id ASC")
    suspend fun getAllDailyPlanItems(): List<DailyPlanItemEntity>

    @Query("SELECT * FROM daily_reviews ORDER BY id ASC")
    suspend fun getAllDailyReviews(): List<DailyReviewEntity>

    @Query("SELECT * FROM app_settings ORDER BY `key` ASC")
    suspend fun getAllAppSettings(): List<AppSettingEntity>

    // --- Table Clear Queries (Executed in Child -> Parent order) ---

    @Query("DELETE FROM reminder_occurrences")
    suspend fun clearReminderOccurrences()

    @Query("DELETE FROM reminders")
    suspend fun clearReminders()

    @Query("DELETE FROM task_subtasks")
    suspend fun clearTaskSubtasks()

    @Query("DELETE FROM tasks")
    suspend fun clearTasks()

    @Query("DELETE FROM habit_logs")
    suspend fun clearHabitLogs()

    @Query("DELETE FROM habits")
    suspend fun clearHabits()

    @Query("DELETE FROM daily_plan_items")
    suspend fun clearDailyPlanItems()

    @Query("DELETE FROM daily_plans")
    suspend fun clearDailyPlans()

    @Query("DELETE FROM daily_reviews")
    suspend fun clearDailyReviews()

    @Query("DELETE FROM app_settings")
    suspend fun clearAppSettings()

    @Query("DELETE FROM categories")
    suspend fun clearCategories()

    // --- Bulk Insert Queries (Executed in Parent -> Child order) ---

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertReminders(reminders: List<ReminderEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertReminderOccurrences(occurrences: List<ReminderOccurrenceEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTaskSubtasks(subtasks: List<TaskSubtaskEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertHabits(habits: List<HabitEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertHabitLogs(logs: List<HabitLogEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertDailyPlans(plans: List<DailyPlanEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertDailyPlanItems(items: List<DailyPlanItemEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertDailyReviews(reviews: List<DailyReviewEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAppSettings(settings: List<AppSettingEntity>)

    /**
     * Atomically clears all user data and restores provided dataset within ONE Room transaction.
     * If any operation fails or violates constraints, SQLite rolls back completely.
     */
    @Transaction
    suspend fun restoreAllDataTransaction(
        categories: List<CategoryEntity>,
        reminders: List<ReminderEntity>,
        occurrences: List<ReminderOccurrenceEntity>,
        tasks: List<TaskEntity>,
        subtasks: List<TaskSubtaskEntity>,
        habits: List<HabitEntity>,
        habitLogs: List<HabitLogEntity>,
        dailyPlans: List<DailyPlanEntity>,
        planItems: List<DailyPlanItemEntity>,
        reviews: List<DailyReviewEntity>,
        appSettings: List<AppSettingEntity>
    ) {
        // 1. Clear tables (children first, then parents)
        clearReminderOccurrences()
        clearReminders()
        clearTaskSubtasks()
        clearTasks()
        clearHabitLogs()
        clearHabits()
        clearDailyPlanItems()
        clearDailyPlans()
        clearDailyReviews()
        clearAppSettings()
        clearCategories()

        // 2. Insert tables (parents first, then children)
        if (categories.isNotEmpty()) insertCategories(categories)
        if (reminders.isNotEmpty()) insertReminders(reminders)
        if (occurrences.isNotEmpty()) insertReminderOccurrences(occurrences)
        if (tasks.isNotEmpty()) insertTasks(tasks)
        if (subtasks.isNotEmpty()) insertTaskSubtasks(subtasks)
        if (habits.isNotEmpty()) insertHabits(habits)
        if (habitLogs.isNotEmpty()) insertHabitLogs(habitLogs)
        if (dailyPlans.isNotEmpty()) insertDailyPlans(dailyPlans)
        if (planItems.isNotEmpty()) insertDailyPlanItems(planItems)
        if (reviews.isNotEmpty()) insertDailyReviews(reviews)
        if (appSettings.isNotEmpty()) insertAppSettings(appSettings)
    }
}