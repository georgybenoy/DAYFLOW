package com.dayflow.app.core.di

import com.dayflow.app.notifications.AndroidNotificationDispatcher
import com.dayflow.app.notifications.NotificationDispatcher
import com.dayflow.app.scheduling.AlarmScheduler
import com.dayflow.app.scheduling.AndroidAlarmScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SchedulingModule {

    @Binds
    @Singleton
    abstract fun bindAlarmScheduler(impl: AndroidAlarmScheduler): AlarmScheduler

    @Binds
    @Singleton
    abstract fun bindNotificationDispatcher(impl: AndroidNotificationDispatcher): NotificationDispatcher
}
