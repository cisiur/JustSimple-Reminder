package com.justsimple.reminder.alarms.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.justsimple.reminder.alarms.service.AlarmForegroundService

/**
 * Entry point fired by AlarmManager when a reminder is due.
 *
 * Responsibilities:
 *  1. Extract the reminder id from the intent.
 *  2. Start AlarmForegroundService, which posts the alarm notification and
 *     launches AlarmActivity — even on the lock screen.
 *
 * goAsync() is NOT needed here because we immediately hand off to a Service.
 * BroadcastReceiver's 10-second window is sufficient for startForegroundService().
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, INVALID_ID)
        if (reminderId == INVALID_ID) {
            Log.e(TAG, "onReceive: missing EXTRA_REMINDER_ID — ignoring")
            return
        }

        Log.d(TAG, "Alarm received for reminderId=$reminderId")

        val serviceIntent = Intent(context, AlarmForegroundService::class.java).apply {
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    companion object {
        const val EXTRA_REMINDER_ID = "reminder_id"
        private const val INVALID_ID = -1L
        private const val TAG = "AlarmReceiver"
    }
}
