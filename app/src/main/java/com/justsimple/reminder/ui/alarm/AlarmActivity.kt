package com.justsimple.reminder.ui.alarm

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import com.justsimple.reminder.alarms.receivers.AlarmActionReceiver
import com.justsimple.reminder.alarms.service.AlarmForegroundService
import com.justsimple.reminder.ui.theme.JustSimpleReminderTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AlarmActivity : ComponentActivity() {

    /**
     * Receives ACTION_CLOSE_ALARM from AlarmActionReceiver when the user taps
     * Dismiss or Snooze directly on the notification (without touching this Activity).
     */
    private val closeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        acquireScreenWakeLock()

        // Back button does nothing — user must explicitly dismiss or snooze
        onBackPressedDispatcher.addCallback(this) { /* intentionally empty */ }

        // Close this activity when notification buttons are used
        ContextCompat.registerReceiver(
            this,
            closeReceiver,
            IntentFilter(AlarmActionReceiver.ACTION_CLOSE_ALARM),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        setContent {
            JustSimpleReminderTheme {
                AlarmScreen(
                    onFinish = {
                        stopService(AlarmForegroundService.stopIntent(this))
                        finish()
                    },
                )
            }
        }
    }

    override fun onDestroy() {
        unregisterReceiver(closeReceiver)
        stopService(AlarmForegroundService.stopIntent(this))
        super.onDestroy()
    }

    private fun acquireScreenWakeLock() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            getSystemService(KeyguardManager::class.java)
                ?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    companion object {
        const val EXTRA_REMINDER_ID = "reminder_id"

        fun createIntent(context: Context, reminderId: Long): Intent =
            Intent(context, AlarmActivity::class.java).apply {
                putExtra(EXTRA_REMINDER_ID, reminderId)
            }
    }
}
