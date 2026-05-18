package com.justsimple.reminder.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [ReminderEntity::class],
    version = 1,
    exportSchema = true,     // schema JSON written to app/schemas/ — commit this file!
)
@TypeConverters(Converters::class)
abstract class ReminderDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
}

