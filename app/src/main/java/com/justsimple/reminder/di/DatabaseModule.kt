package com.justsimple.reminder.di

import android.content.Context
import androidx.room.Room
import com.justsimple.reminder.data.db.ReminderDao
import com.justsimple.reminder.data.db.ReminderDatabase
import com.justsimple.reminder.data.repository.ReminderRepository
import com.justsimple.reminder.data.repository.ReminderRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseModule {

    @Binds
    @Singleton
    abstract fun bindReminderRepository(impl: ReminderRepositoryImpl): ReminderRepository

    companion object {
        @Provides
        @Singleton
        fun provideDatabase(@ApplicationContext context: Context): ReminderDatabase =
            Room.databaseBuilder(context, ReminderDatabase::class.java, "justsimple_reminder.db")
                .fallbackToDestructiveMigration()
                .build()

        @Provides
        fun provideReminderDao(db: ReminderDatabase): ReminderDao = db.reminderDao()
    }
}

