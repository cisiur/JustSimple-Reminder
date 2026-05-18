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
 * Restores all AlarmManager slots after the device reboots.
 *
 * AlarmManager alarms do not survive a power cycle — this receiver is the
 * mechanism that brings them back. It also handles QUICKBOOT_POWERON (Xiaomi
 * fast-boot) and HTC's equivalent action.
 *
 * Uses goAsync() so the coroutine can finish the DB work before the system
 * decides the receiver is done (default 10-second budget).
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var rescheduleAlarms: RescheduleAlarmsUseCase

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in HANDLED_ACTIONS) return

        Log.d(TAG, "Boot/quickboot received: $action")
        val pendingResult = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                rescheduleAlarms()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reschedule alarms after boot", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
        private val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON",
        )
    }
}
