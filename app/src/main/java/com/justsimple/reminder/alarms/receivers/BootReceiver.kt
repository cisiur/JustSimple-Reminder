package com.justsimple.reminder.alarms.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        // TODO Module 5: reschedule all enabled reminders
        pending.finish()
    }
}

