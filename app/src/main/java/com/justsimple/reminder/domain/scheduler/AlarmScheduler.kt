package com.justsimple.reminder.domain.scheduler

import com.justsimple.reminder.data.db.ReminderEntity

interface AlarmScheduler {
    fun schedule(reminder: ReminderEntity)
    fun cancel(reminderId: Long)
    fun rescheduleAll(reminders: List<ReminderEntity>)
}

