package com.justsimple.reminder.di

import com.justsimple.reminder.domain.scheduler.AlarmScheduler
import com.justsimple.reminder.domain.scheduler.AlarmSchedulerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SchedulerModule {

    @Binds
    @Singleton
    abstract fun bindAlarmScheduler(impl: AlarmSchedulerImpl): AlarmScheduler
}

