package com.justsimple.reminder.alarms.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        // TODO Module 5: launch foreground service / AlarmActivity, then finish()
        pending.finish()
    }

    companion object {
        const val EXTRA_REMINDER_ID = "reminder_id"
    }
}

