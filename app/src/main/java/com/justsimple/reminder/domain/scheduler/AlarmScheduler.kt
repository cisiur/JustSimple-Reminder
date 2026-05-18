package com.justsimple.reminder.domain.scheduler

import com.justsimple.reminder.data.db.ReminderEntity

interface AlarmScheduler {
    /** Schedule (or reschedule) a single reminder. No-op if disabled or past. */
    fun schedule(reminder: ReminderEntity)

    /** Cancel any pending alarm for this reminder id. */
    fun cancel(reminderId: Long)

    /** Cancel + reschedule every reminder in the list (boot / time-change restore). */
    fun rescheduleAll(reminders: List<ReminderEntity>)

    /**
     * Returns true if the app currently holds permission to schedule exact alarms.
     * Always true below API 31. Used by Settings and Diagnostics screens.
     */
    fun canScheduleExact(): Boolean
}
