package com.dayflow.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.dayflow.app.data.local.converter.DayflowConverters
import com.dayflow.app.data.local.dao.AppSettingDao
import com.dayflow.app.data.local.dao.BackupDao
import com.dayflow.app.data.local.dao.CategoryDao
import com.dayflow.app.data.local.dao.DailyPlanDao
import com.dayflow.app.data.local.dao.DailyReviewDao
import com.dayflow.app.data.local.dao.HabitDao
import com.dayflow.app.data.local.dao.ReminderDao
import com.dayflow.app.data.local.dao.ReminderOccurrenceDao
import com.dayflow.app.data.local.dao.TaskDao
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

@Database(
    entities = [
        CategoryEntity::class,
        ReminderEntity::class,
        ReminderOccurrenceEntity::class,
        TaskEntity::class,
        TaskSubtaskEntity::class,
        HabitEntity::class,
        HabitLogEntity::class,
        DailyPlanEntity::class,
        DailyPlanItemEntity::class,
        DailyReviewEntity::class,
        AppSettingEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(DayflowConverters::class)
abstract class DayflowDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun reminderDao(): ReminderDao
    abstract fun reminderOccurrenceDao(): ReminderOccurrenceDao
    abstract fun taskDao(): TaskDao
    abstract fun habitDao(): HabitDao
    abstract fun dailyPlanDao(): DailyPlanDao
    abstract fun dailyReviewDao(): DailyReviewDao
    abstract fun appSettingDao(): AppSettingDao
    abstract fun backupDao(): BackupDao

    companion object {
        const val DATABASE_NAME = "dayflow.db"
    }
}
