package com.justsimple.reminder.data.repository

import com.justsimple.reminder.data.db.ReminderDao
import com.justsimple.reminder.data.db.ReminderEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRepositoryImpl @Inject constructor(
    private val dao: ReminderDao,
) : ReminderRepository {

    // ── Streams ──────────────────────────────────────────────────────────────

    override fun observeAll(): Flow<List<ReminderEntity>> = dao.observeAll()

    // ── Reads ────────────────────────────────────────────────────────────────

    override suspend fun getById(id: Long): ReminderEntity? = dao.getById(id)
    override suspend fun getAllEnabled(): List<ReminderEntity> = dao.getAllEnabled()
    override suspend fun getEnabledFuture(nowMillis: Long) = dao.getEnabledFuture(nowMillis)
    override suspend fun countEnabled(): Int = dao.countEnabled()
    override suspend fun getNextScheduledMillis(): Long? = dao.getNextScheduledMillis()

    // ── Writes ───────────────────────────────────────────────────────────────

    override suspend fun insert(reminder: ReminderEntity): Long = dao.insert(reminder)
    override suspend fun update(reminder: ReminderEntity) = dao.update(reminder)
    override suspend fun delete(id: Long) = dao.deleteById(id)

    // ── Targeted updates ─────────────────────────────────────────────────────

    override suspend fun setEnabled(id: Long, enabled: Boolean) =
        dao.updateEnabled(id, enabled, System.currentTimeMillis())

    override suspend fun markTriggered(
        id: Long,
        lastTriggeredAt: Long,
        nextTriggerAt: Long,
        enabled: Boolean,
    ) = dao.markTriggered(id, lastTriggeredAt, nextTriggerAt, enabled, System.currentTimeMillis())

    override suspend fun applySnooze(id: Long, snoozeUntil: Long) =
        dao.applySnooze(id, snoozeUntil, System.currentTimeMillis())

    override suspend fun clearSnooze(id: Long, nextTriggerAt: Long, enabled: Boolean) =
        dao.clearSnooze(id, nextTriggerAt, enabled, System.currentTimeMillis())
}

