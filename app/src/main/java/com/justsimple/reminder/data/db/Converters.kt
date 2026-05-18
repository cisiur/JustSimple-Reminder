package com.justsimple.reminder.data.db

import androidx.room.TypeConverter
import com.justsimple.reminder.domain.recurrence.RecurrenceType

class Converters {
    @TypeConverter fun fromRecurrenceType(value: RecurrenceType): String = value.name
    @TypeConverter fun toRecurrenceType(value: String): RecurrenceType = RecurrenceType.valueOf(value)
}

