package com.justsimple.reminder.domain.recurrence

import com.justsimple.reminder.data.db.ReminderEntity
import java.time.LocalDate
import javax.inject.Inject
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Pure, Android-free class. All logic operates on epoch millis + java.time.
 * Unit-tested without Android instrumentation.
 *
 * Single entry point: [nextTriggerMillis].
 */
class RecurrenceEngine @Inject constructor() {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    /**
     * Returns the next time this reminder should fire, as epoch millis UTC.
     * Returns null only for [RecurrenceType.ONCE] reminders whose scheduled
     * moment is in the past — they should be disabled after firing.
     *
     * Snooze precedence rule: if [ReminderEntity.snoozeUntil] is set and is
     * strictly in the future it is returned immediately, bypassing recurrence.
     *
     * @param reminder  the reminder to evaluate
     * @param nowMillis current wall-clock time (epoch millis); injectable for testing
     * @param zone      timezone used for all date/time conversions; injectable for testing
     */
    fun nextTriggerMillis(
        reminder: ReminderEntity,
        nowMillis: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long? {
        // Snooze takes absolute precedence over normal recurrence
        reminder.snoozeUntil?.let { snooze ->
            if (snooze > nowMillis) return snooze
        }

        val date = LocalDate.parse(reminder.scheduledDate)
        val time = LocalTime.parse(reminder.scheduledTime, timeFormatter)

        return when (reminder.recurrenceType) {
            RecurrenceType.ONCE ->
                NextTriggerCalculator.once(date, time, nowMillis, zone)

            RecurrenceType.DAILY ->
                NextTriggerCalculator.daily(time, nowMillis, zone)

            RecurrenceType.WEEKLY ->
                NextTriggerCalculator.weekly(date.dayOfWeek, time, nowMillis, zone)

            RecurrenceType.MONTHLY ->
                NextTriggerCalculator.monthly(date.dayOfMonth, time, nowMillis, zone)
        }
    }

    /**
     * Convenience: returns true when a ONCE reminder's moment has already passed
     * and it should be marked disabled after triggering.
     */
    fun isExpired(reminder: ReminderEntity, nowMillis: Long = System.currentTimeMillis()): Boolean =
        reminder.recurrenceType == RecurrenceType.ONCE &&
            nextTriggerMillis(reminder, nowMillis) == null
}
