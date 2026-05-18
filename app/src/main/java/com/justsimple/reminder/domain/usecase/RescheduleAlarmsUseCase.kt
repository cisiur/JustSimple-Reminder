package com.justsimple.reminder.domain.usecase

import android.util.Log
import com.justsimple.reminder.data.repository.ReminderRepository
import com.justsimple.reminder.domain.recurrence.RecurrenceEngine
import com.justsimple.reminder.domain.scheduler.AlarmScheduler
import javax.inject.Inject

/**
 * Recalculates nextTriggerAt for every enabled reminder and syncs AlarmManager.
 *
 * Called by:
 *  - BootReceiver  → restores alarms lost when the device was powered off
 *  - TimeChangeReceiver → corrects alarms after time/timezone change or app update
 *
 * Logic per reminder:
 *  - ONCE, moment passed while device was off  → mark enabled=false (expired)
 *  - nextTriggerAt changed (timezone drift)    → update Room + reschedule
 *  - nextTriggerAt still correct               → reschedule only (boot restore)
 */
class RescheduleAlarmsUseCase @Inject constructor(
    private val repository: ReminderRepository,
    private val engine: RecurrenceEngine,
    private val scheduler: AlarmScheduler,
) {
    suspend operator fun invoke() {
        val now = System.currentTimeMillis()
        val enabled = repository.getAllEnabled()
        Log.d(TAG, "Rescheduling ${enabled.size} enabled reminders")

        enabled.forEach { reminder ->
            val next = engine.nextTriggerMillis(reminder, now)

            when {
                next == null -> {
                    // ONCE reminder that expired while device was off
                    repository.markTriggered(
                        id = reminder.id,
                        lastTriggeredAt = now,
                        nextTriggerAt = 0L,
                        enabled = false,
                    )
                    scheduler.cancel(reminder.id)
                    Log.d(TAG, "Expired ONCE reminder id=${reminder.id}")
                }

                next != reminder.nextTriggerAt -> {
                    // nextTriggerAt drifted (timezone change) — update Room then reschedule
                    val updated = reminder.copy(nextTriggerAt = next, updatedAt = now)
                    repository.update(updated)
                    scheduler.schedule(updated)
                    Log.d(TAG, "Rescheduled (updated) id=${reminder.id} → $next")
                }

                else -> {
                    // nextTriggerAt is still correct — just restore the AlarmManager slot
                    scheduler.schedule(reminder)
                    Log.d(TAG, "Rescheduled (restored) id=${reminder.id} at ${reminder.nextTriggerAt}")
                }
            }
        }
    }

    private companion object {
        const val TAG = "RescheduleAlarmsUseCase"
    }
}
