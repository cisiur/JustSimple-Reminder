package com.justsimple.reminder.ui.alarm

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.justsimple.reminder.alarms.service.AlarmForegroundService
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TODO Module 8: window flags (FLAG_KEEP_SCREEN_ON, setShowWhenLocked, setTurnScreenOn)
        // TODO Module 8: AlarmScreen composable (title, time, Dismiss, Snooze)
        setContent { }
    }

    override fun onDestroy() {
        // Stop the foreground service when the alarm UI is gone
        stopService(AlarmForegroundService.stopIntent(this))
        super.onDestroy()
    }

    companion object {
        const val EXTRA_REMINDER_ID = "reminder_id"

        fun createIntent(context: Context, reminderId: Long): Intent =
            Intent(context, AlarmActivity::class.java).apply {
                putExtra(EXTRA_REMINDER_ID, reminderId)
            }
    }
}
