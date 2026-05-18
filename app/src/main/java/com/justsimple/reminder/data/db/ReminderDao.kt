package com.justsimple.reminder.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    // ── Observation ──────────────────────────────────────────────────────────

    /** Live list sorted by next fire time — drives the main screen LazyColumn. */
    @Query("SELECT * FROM reminders ORDER BY nextTriggerAt ASC")
    fun observeAll(): Flow<List<ReminderEntity>>

    // ── One-shot reads ───────────────────────────────────────────────────────

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getById(id: Long): ReminderEntity?

    /** All enabled reminders — used by boot/time-change receivers to reschedule. */
    @Query("SELECT * FROM reminders WHERE enabled = 1")
    suspend fun getAllEnabled(): List<ReminderEntity>

    /**
     * Enabled reminders whose nextTriggerAt is in the future.
     * Used after a time-zone change to decide which ones still need rescheduling.
     */
    @Query("SELECT * FROM reminders WHERE enabled = 1 AND nextTriggerAt > :nowMillis")
    suspend fun getEnabledFuture(nowMillis: Long): List<ReminderEntity>

    /** How many reminders are currently enabled — for the free-tier gate. */
    @Query("SELECT COUNT(*) FROM reminders WHERE enabled = 1")
    suspend fun countEnabled(): Int

    /** Smallest nextTriggerAt among all enabled reminders — shown on the Diagnostics screen. */
    @Query("SELECT MIN(nextTriggerAt) FROM reminders WHERE enabled = 1")
    suspend fun getNextScheduledMillis(): Long?

    // ── Writes ───────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: ReminderEntity): Long

    @Update
    suspend fun update(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: Long)

    // ── Targeted field updates (avoid full-row rewrites for hot paths) ────────

    /**
     * Toggle the enabled flag and refresh updatedAt.
     * Called when the user flips the switch on the list screen.
     */
    @Query("UPDATE reminders SET enabled = :enabled, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateEnabled(id: Long, enabled: Boolean, updatedAt: Long)

    /**
     * After an alarm fires, record when it triggered and advance the schedule.
     * For ONCE reminders pass enabled=false and nextTriggerAt=0.
     * For recurring reminders pass the next computed epoch and enabled=true.
     */
    @Query("""
        UPDATE reminders
        SET lastTriggeredAt = :lastTriggeredAt,
            nextTriggerAt   = :nextTriggerAt,
            snoozeUntil     = NULL,
            enabled         = :enabled,
            updatedAt       = :updatedAt
        WHERE id = :id
    """)
    suspend fun markTriggered(
        id: Long,
        lastTriggeredAt: Long,
        nextTriggerAt: Long,
        enabled: Boolean,
        updatedAt: Long,
    )

    /**
     * Apply a snooze: set snoozeUntil and advance nextTriggerAt to the same value
     * so the AlarmManager is rescheduled to the snooze time.
     */
    @Query("""
        UPDATE reminders
        SET snoozeUntil   = :snoozeUntil,
            nextTriggerAt = :snoozeUntil,
            updatedAt     = :updatedAt
        WHERE id = :id
    """)
    suspend fun applySnooze(id: Long, snoozeUntil: Long, updatedAt: Long)

    /**
     * After a snoozed alarm fires, clear the snooze flag and advance to the
     * next regular occurrence (or disable for ONCE).
     */
    @Query("""
        UPDATE reminders
        SET snoozeUntil   = NULL,
            nextTriggerAt = :nextTriggerAt,
            enabled       = :enabled,
            updatedAt     = :updatedAt
        WHERE id = :id
    """)
    suspend fun clearSnooze(id: Long, nextTriggerAt: Long, enabled: Boolean, updatedAt: Long)
}

