package com.justsimple.reminder.alarms.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TimeChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        // TODO Module 5: recalculate nextTriggerAt and reschedule all enabled reminders
        pending.finish()
    }
}

