package com.justsimple.reminder.alarms.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.justsimple.reminder.domain.usecase.RescheduleAlarmsUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Recalculates and reschedules all alarms when the system clock or timezone
 * changes, or when the app itself is updated (MY_PACKAGE_REPLACED).
 *
 * Why MY_PACKAGE_REPLACED: app updates clear AlarmManager slots on some OEMs,
 * so we restore them immediately after the new APK is installed.
 */
@AndroidEntryPoint
class TimeChangeReceiver : BroadcastReceiver() {

    @Inject lateinit var rescheduleAlarms: RescheduleAlarmsUseCase

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in HANDLED_ACTIONS) return

        Log.d(TAG, "Time/timezone/package change: $action")
        val pendingResult = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                rescheduleAlarms()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reschedule alarms after time change", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "TimeChangeReceiver"

        // Fired on API 31+ when the user grants or revokes SCHEDULE_EXACT_ALARM.
        // Adding it here (instead of a separate receiver) keeps reschedule logic in one place.
        private const val ACTION_EXACT_ALARM_PERMISSION_CHANGED =
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"

        private val HANDLED_ACTIONS = setOf(
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            ACTION_EXACT_ALARM_PERMISSION_CHANGED,
        )
    }
}
