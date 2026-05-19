package com.justsimple.reminder.alarms.receivers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.justsimple.reminder.alarms.service.AlarmForegroundService
import com.justsimple.reminder.data.repository.ReminderRepository
import com.justsimple.reminder.domain.recurrence.RecurrenceEngine
import com.justsimple.reminder.domain.scheduler.AlarmScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Handles Dismiss and Snooze actions fired from the alarm notification buttons.
 *
 * This receiver is needed so the user can act on the alarm even when the screen is
 * on and only the heads-up notification is showing (not AlarmActivity).
 *
 * After handling the action it:
 *  1. Stops AlarmForegroundService (which stops the alarm sound).
 *  2. Cancels the notification.
 *  3. Sends ACTION_CLOSE_ALARM so AlarmActivity finishes itself if it is visible.
 */
@AndroidEntryPoint
class AlarmActionReceiver : BroadcastReceiver() {

    @Inject lateinit var repository: ReminderRepository
    @Inject lateinit var scheduler: AlarmScheduler
    @Inject lateinit var recurrenceEngine: RecurrenceEngine

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        if (reminderId == -1L) {
            Log.e(TAG, "Missing reminderId — ignoring action=$action")
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (action) {
                    ACTION_DISMISS -> handleDismiss(context, reminderId)
                    ACTION_SNOOZE  -> handleSnooze(context, reminderId, SNOOZE_MINUTES)
                    else           -> Log.w(TAG, "Unknown action: $action")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling action=$action id=$reminderId", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    // ── Action handlers ───────────────────────────────────────────────────────

    private suspend fun handleDismiss(context: Context, reminderId: Long) {
        val now = System.currentTimeMillis()
        val reminder = repository.getById(reminderId) ?: return
        val next = recurrenceEngine.nextTriggerMillis(reminder, now)
        val enabled = next != null && next > 0L

        repository.markTriggered(
            id = reminderId,
            lastTriggeredAt = now,
            nextTriggerAt = next ?: 0L,
            enabled = enabled,
        )
        if (enabled && next != null) {
            scheduler.schedule(reminder.copy(nextTriggerAt = next, enabled = true))
        } else {
            scheduler.cancel(reminderId)
        }

        closeAlarm(context)
    }

    private suspend fun handleSnooze(context: Context, reminderId: Long, minutes: Int) {
        val snoozeUntil = System.currentTimeMillis() + minutes * 60_000L
        repository.applySnooze(reminderId, snoozeUntil)
        val reminder = repository.getById(reminderId) ?: return
        scheduler.schedule(reminder)
        closeAlarm(context)
    }

    // ── Cleanup ───────────────────────────────────────────────────────────────

    private fun closeAlarm(context: Context) {
        // Stop the foreground service → stops alarm sound
        context.stopService(Intent(context, AlarmForegroundService::class.java))

        // Cancel the notification
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(AlarmForegroundService.NOTIFICATION_ID)

        // Tell AlarmActivity to close itself if it is currently visible
        context.sendBroadcast(
            Intent(ACTION_CLOSE_ALARM).apply { setPackage(context.packageName) }
        )
    }

    // ── Constants ─────────────────────────────────────────────────────────────

    companion object {
        const val ACTION_DISMISS    = "com.justsimple.reminder.ACTION_ALARM_DISMISS"
        const val ACTION_SNOOZE     = "com.justsimple.reminder.ACTION_ALARM_SNOOZE"
        const val ACTION_CLOSE_ALARM = "com.justsimple.reminder.ACTION_CLOSE_ALARM"
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val SNOOZE_MINUTES    = 10

        private const val TAG = "AlarmActionReceiver"
    }
}
