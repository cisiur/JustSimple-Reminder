package com.justsimple.reminder.domain.scheduler

import android.app.AlarmManager
import android.content.Context
import com.justsimple.reminder.data.db.ReminderEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alarmManager: AlarmManager
) : AlarmScheduler {
    override fun schedule(reminder: ReminderEntity) {
        // TODO Module 4: AlarmManager.setAlarmClock()
    }

    override fun cancel(reminderId: Long) {
        // TODO Module 4
    }

    override fun rescheduleAll(reminders: List<ReminderEntity>) {
        reminders.forEach { schedule(it) }
    }
}

