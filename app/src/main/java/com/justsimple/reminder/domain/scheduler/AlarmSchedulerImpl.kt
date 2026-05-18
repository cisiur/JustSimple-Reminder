package com.justsimple.reminder.domain.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.justsimple.reminder.alarms.receivers.AlarmReceiver
import com.justsimple.reminder.data.db.ReminderEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alarmManager: AlarmManager,
) : AlarmScheduler {

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Schedules (or reschedules) an exact alarm for [reminder] using
     * [AlarmManager.setAlarmClock].
     *
     * setAlarmClock() is the strongest exact-alarm mechanism on Android:
     *  - Survives Doze mode
     *  - Shows the alarm-clock icon in the status bar
     *  - Has the best delivery reliability across OEM variants
     *
     * If the reminder is disabled, or its nextTriggerAt is in the past,
     * or the app lacks SCHEDULE_EXACT_ALARM permission, the call is a no-op
     * (and any existing alarm is cancelled).
     */
    override fun schedule(reminder: ReminderEntity) {
        if (!reminder.enabled || reminder.nextTriggerAt <= 0L) {
            cancel(reminder.id)
            return
        }

        if (!canScheduleExact()) {
            Log.w(TAG, "Cannot schedule exact alarm — permission not granted. id=${reminder.id}")
            return
        }

        val triggerMillis = reminder.nextTriggerAt
        if (triggerMillis <= System.currentTimeMillis()) {
            Log.w(TAG, "nextTriggerAt is in the past, skipping. id=${reminder.id}")
            cancel(reminder.id)
            return
        }

        val operation = buildAlarmPendingIntent(reminder.id)

        // showIntent: tapping the status-bar alarm clock icon opens the app
        val showIntent = buildShowIntent()

        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerMillis, showIntent)
        alarmManager.setAlarmClock(alarmClockInfo, operation)

        Log.d(TAG, "Scheduled alarm id=${reminder.id} at $triggerMillis (\"${reminder.title}\")")
    }

    /**
     * Cancels any pending alarm for [reminderId].
     * Safe to call even if no alarm was scheduled.
     */
    override fun cancel(reminderId: Long) {
        val operation = buildAlarmPendingIntent(reminderId)
        alarmManager.cancel(operation)
        Log.d(TAG, "Cancelled alarm id=$reminderId")
    }

    /**
     * Cancels all existing alarms then reschedules every reminder in [reminders].
     * Used by boot restore and time/timezone change handlers.
     */
    override fun rescheduleAll(reminders: List<ReminderEntity>) {
        reminders.forEach { schedule(it) }
        Log.d(TAG, "Rescheduled ${reminders.size} alarms")
    }

    // ── Permission check ─────────────────────────────────────────────────────

    /**
     * Returns true if the app is allowed to schedule exact alarms.
     * Always true below API 31 — the permission wasn't required before S.
     */
    fun canScheduleExact(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

    // ── PendingIntent helpers ────────────────────────────────────────────────

    /**
     * Builds the PendingIntent that AlarmManager fires when the alarm triggers.
     * Each reminder gets a unique requestCode (its id cast to Int) so alarms
     * don't overwrite each other.
     *
     * FLAG_UPDATE_CURRENT: re-use an existing PendingIntent if present,
     * updating its extras. This is what makes reschedule work correctly.
     * FLAG_IMMUTABLE: required on API 31+.
     */
    private fun buildAlarmPendingIntent(reminderId: Long): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM_TRIGGER
            putExtra(AlarmReceiver.EXTRA_REMINDER_ID, reminderId)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(
            context,
            reminderId.toRequestCode(),
            intent,
            flags,
        )
    }

    /**
     * Builds the PendingIntent shown when the user taps the alarm-clock icon
     * in the status bar. Opens MainActivity.
     */
    private fun buildShowIntent(): PendingIntent {
        val intent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?: Intent(context, Class.forName("${context.packageName}.MainActivity"))
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        return PendingIntent.getActivity(
            context,
            REQUEST_CODE_SHOW,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /**
     * Converts a Long reminder id to an Int request code.
     * Room auto-generates ids starting at 1, so the cast is safe for any
     * realistic number of reminders. Collisions at Int.MAX_VALUE are
     * practically impossible in a reminder app.
     */
    private fun Long.toRequestCode(): Int = (this and 0x7FFFFFFF).toInt()

    // ── Constants ────────────────────────────────────────────────────────────

    companion object {
        private const val TAG = "AlarmScheduler"
        const val ACTION_ALARM_TRIGGER = "com.justsimple.reminder.ACTION_ALARM_TRIGGER"
        private const val REQUEST_CODE_SHOW = 0
    }
}
