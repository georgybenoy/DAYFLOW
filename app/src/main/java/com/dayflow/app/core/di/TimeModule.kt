package com.dayflow.app.core.di

import com.dayflow.app.core.time.SystemTimeProvider
import com.dayflow.app.core.time.TimeProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TimeModule {

    @Binds
    @Singleton
    abstract fun bindTimeProvider(
        systemTimeProvider: SystemTimeProvider
    ): TimeProvider
}
