package com.dayflow.app.core.di

import android.content.Context
import androidx.room.Room
import com.dayflow.app.data.local.DayflowDatabase
import com.dayflow.app.data.local.DayflowDatabaseCallback
import com.dayflow.app.data.local.dao.AppSettingDao
import com.dayflow.app.data.local.dao.BackupDao
import com.dayflow.app.data.local.dao.CategoryDao
import com.dayflow.app.data.local.dao.DailyPlanDao
import com.dayflow.app.data.local.dao.DailyReviewDao
import com.dayflow.app.data.local.dao.HabitDao
import com.dayflow.app.data.local.dao.ReminderDao
import com.dayflow.app.data.local.dao.ReminderOccurrenceDao
import com.dayflow.app.data.local.dao.TaskDao
import com.dayflow.app.data.local.migrations.DayflowMigrations
import com.dayflow.app.data.repository.CategoryRepositoryImpl
import com.dayflow.app.data.repository.DailyPlanRepositoryImpl
import com.dayflow.app.data.repository.DailyReviewRepositoryImpl
import com.dayflow.app.data.repository.HabitRepositoryImpl
import com.dayflow.app.data.repository.ReminderRepositoryImpl
import com.dayflow.app.data.repository.TaskRepositoryImpl
import com.dayflow.app.domain.repository.CategoryRepository
import com.dayflow.app.domain.repository.DailyPlanRepository
import com.dayflow.app.domain.repository.DailyReviewRepository
import com.dayflow.app.domain.repository.HabitRepository
import com.dayflow.app.domain.repository.ReminderRepository
import com.dayflow.app.domain.repository.TaskRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDayflowDatabase(
        @ApplicationContext context: Context
    ): DayflowDatabase {
        return Room.databaseBuilder(
            context,
            DayflowDatabase::class.java,
            DayflowDatabase.DATABASE_NAME
        )
            .addCallback(DayflowDatabaseCallback())
            .addMigrations(*DayflowMigrations.ALL_MIGRATIONS)
            .build()
    }

    @Provides
    fun provideCategoryDao(database: DayflowDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideReminderDao(database: DayflowDatabase): ReminderDao = database.reminderDao()

    @Provides
    fun provideReminderOccurrenceDao(database: DayflowDatabase): ReminderOccurrenceDao = database.reminderOccurrenceDao()

    @Provides
    fun provideTaskDao(database: DayflowDatabase): TaskDao = database.taskDao()

    @Provides
    fun provideHabitDao(database: DayflowDatabase): HabitDao = database.habitDao()

    @Provides
    fun provideDailyPlanDao(database: DayflowDatabase): DailyPlanDao = database.dailyPlanDao()

    @Provides
    fun provideDailyReviewDao(database: DayflowDatabase): DailyReviewDao = database.dailyReviewDao()

    @Provides
    fun provideAppSettingDao(database: DayflowDatabase): AppSettingDao = database.appSettingDao()

    @Provides
    fun provideBackupDao(database: DayflowDatabase): BackupDao = database.backupDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryBindingsModule {

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindReminderRepository(impl: ReminderRepositoryImpl): ReminderRepository

    @Binds
    @Singleton
    abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository

    @Binds
    @Singleton
    abstract fun bindHabitRepository(impl: HabitRepositoryImpl): HabitRepository

    @Binds
    @Singleton
    abstract fun bindDailyPlanRepository(impl: DailyPlanRepositoryImpl): DailyPlanRepository

    @Binds
    @Singleton
    abstract fun bindDailyReviewRepository(impl: DailyReviewRepositoryImpl): DailyReviewRepository
}
