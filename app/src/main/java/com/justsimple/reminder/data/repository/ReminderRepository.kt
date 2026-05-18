package com.justsimple.reminder.data.repository

import com.justsimple.reminder.data.db.ReminderEntity
import kotlinx.coroutines.flow.Flow

interface ReminderRepository {

    // ── Streams ──────────────────────────────────────────────────────────────

    /** Emits every time the reminders table changes. Sorted by nextTriggerAt. */
    fun observeAll(): Flow<List<ReminderEntity>>

    // ── Reads ────────────────────────────────────────────────────────────────

    suspend fun getById(id: Long): ReminderEntity?
    suspend fun getAllEnabled(): List<ReminderEntity>
    suspend fun getEnabledFuture(nowMillis: Long): List<ReminderEntity>
    suspend fun countEnabled(): Int

    /** Returns epoch millis of the soonest pending alarm, or null if none. */
    suspend fun getNextScheduledMillis(): Long?

    // ── Writes ───────────────────────────────────────────────────────────────

    suspend fun insert(reminder: ReminderEntity): Long
    suspend fun update(reminder: ReminderEntity)
    suspend fun delete(id: Long)

    // ── Targeted updates (called by alarm delivery path) ─────────────────────

    suspend fun setEnabled(id: Long, enabled: Boolean)

    /**
     * Record that the alarm fired. For ONCE reminders pass enabled=false.
     * Clears snoozeUntil automatically.
     */
    suspend fun markTriggered(
        id: Long,
        lastTriggeredAt: Long,
        nextTriggerAt: Long,
        enabled: Boolean,
    )

    /** Store a snooze — moves nextTriggerAt to snoozeUntil. */
    suspend fun applySnooze(id: Long, snoozeUntil: Long)

    /** Clear snooze state and advance to the next regular occurrence. */
    suspend fun clearSnooze(id: Long, nextTriggerAt: Long, enabled: Boolean)
}

