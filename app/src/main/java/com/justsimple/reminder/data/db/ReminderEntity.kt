package com.justsimple.reminder.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.justsimple.reminder.domain.recurrence.RecurrenceType

/**
 * Single source of truth for a reminder row.
 *
 * Dates are stored as strings to survive timezone migrations cleanly:
 *   scheduledDate → ISO-8601 "YYYY-MM-DD"  (user's chosen calendar date)
 *   scheduledTime → "HH:mm"                 (24-hour, user's chosen clock time)
 *
 * nextTriggerAt is the ground truth for when AlarmManager fires next.
 * It is always recomputed by RecurrenceEngine and stored here so boot
 * restore and time-change reschedules need only one DB read.
 */
@Entity(
    tableName = "reminders",
    indices = [
        Index(value = ["nextTriggerAt"]),          // ORDER BY nextTriggerAt fast-path
        Index(value = ["enabled", "nextTriggerAt"]) // reschedule query filter
    ]
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val title: String,

    /** Whether AlarmManager has an active alarm for this reminder. */
    val enabled: Boolean,

    /** User-chosen calendar date — "YYYY-MM-DD". */
    val scheduledDate: String,

    /** User-chosen clock time — "HH:mm" (always 24 h internally). */
    val scheduledTime: String,

    val recurrenceType: RecurrenceType,

    /** Epoch millis UTC. Recomputed by RecurrenceEngine on every save / trigger / snooze. */
    val nextTriggerAt: Long,

    /** Epoch millis of the most recent successful trigger. Null until first fire. */
    val lastTriggeredAt: Long?,

    /**
     * If non-null, the alarm was snoozed and should next fire at this time.
     * RecurrenceEngine returns this value as nextTriggerAt when it is > now.
     * Cleared to null when the snoozed alarm fires.
     */
    val snoozeUntil: Long?,

    val createdAt: Long,
    val updatedAt: Long,
)

