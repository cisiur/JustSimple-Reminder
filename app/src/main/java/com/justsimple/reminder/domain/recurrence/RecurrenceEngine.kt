package com.justsimple.reminder.domain.recurrence

import com.justsimple.reminder.data.db.ReminderEntity

/**
 * Pure, Android-free class. All logic operates on epoch millis and java.time.
 * Unit-tested without Android instrumentation.
 */
class RecurrenceEngine {
    /**
     * Returns the next trigger time in epoch millis, or null if the reminder has expired.
     * Snooze takes precedence over normal recurrence.
     */
    fun nextTriggerMillis(reminder: ReminderEntity, nowMillis: Long): Long? {
        // TODO Module 3
        return null
    }
}

