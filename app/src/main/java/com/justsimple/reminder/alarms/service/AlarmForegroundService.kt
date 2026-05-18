package com.justsimple.reminder.alarms.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.justsimple.reminder.R
import com.justsimple.reminder.alarms.receivers.AlarmReceiver
import com.justsimple.reminder.data.db.ReminderEntity
import com.justsimple.reminder.data.repository.ReminderRepository
import com.justsimple.reminder.ui.alarm.AlarmActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject

/**
 * Foreground service that drives alarm delivery.
 *
 * Flow:
 *  1. AlarmReceiver starts this service with EXTRA_REMINDER_ID.
 *  2. startForeground() is called immediately to satisfy the 5-second ANR limit.
 *  3. The reminder is fetched from Room on the IO dispatcher.
 *  4. The notification is rebuilt with the real reminder title.
 *  5. fullScreenIntent launches AlarmActivity — shown on the lock screen.
 *  6. AlarmActivity calls stopAlarmService() when the user dismisses / snoozes.
 */
@AndroidEntryPoint
class AlarmForegroundService : Service() {

    @Inject lateinit var repository: ReminderRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val reminderId = intent?.getLongExtra(AlarmReceiver.EXTRA_REMINDER_ID, -1L) ?: -1L

        // Start foreground immediately — must happen within 5 s of startForegroundService()
        // Pass reminderId so the placeholder already carries a fullScreenIntent,
        // which is what triggers full-screen display when the screen is locked.
        startForegroundCompat(buildPlaceholderNotification(reminderId))

        if (reminderId == -1L) {
            Log.e(TAG, "No reminderId in intent — stopping service")
            stopSelf(startId)
            return START_NOT_STICKY
        }

        // Launch AlarmActivity NOW — before any async work.
        // Delaying until after the DB fetch causes Android to fall back to a
        // heads-up notification because the window for a synchronous activity
        // launch from a foreground service has already passed.
        // FLAG_ACTIVITY_NO_USER_ACTION is required so the system treats this as
        // an alarm (not a user-initiated launch), which matters for lock-screen display.
        val activityIntent = AlarmActivity.createIntent(this, reminderId).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
        }
        startActivity(activityIntent)

        // Async: fetch reminder title and update the foreground notification.
        serviceScope.launch {
            val reminder = repository.getById(reminderId)
            if (reminder == null) {
                Log.e(TAG, "Reminder $reminderId not found in DB — stopping service")
                stopSelf(startId)
                return@launch
            }

            val notification = buildAlarmNotification(reminder)
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(NOTIFICATION_ID, notification)
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Notification builders ─────────────────────────────────────────────────

    private fun buildPlaceholderNotification(reminderId: Long = -1L): Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("…")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)

        if (reminderId != -1L) {
            val fsi = PendingIntent.getActivity(
                this,
                reminderId.toInt(),
                AlarmActivity.createIntent(this, reminderId).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            builder.setFullScreenIntent(fsi, true)
        }

        return builder.build()
    }

    private fun buildAlarmNotification(reminder: ReminderEntity): Notification {
        val timeDisplay = runCatching {
            LocalTime.parse(reminder.scheduledTime, DateTimeFormatter.ofPattern("HH:mm"))
                .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
        }.getOrDefault(reminder.scheduledTime)

        val fullScreenIntent = AlarmActivity.createIntent(this, reminder.id).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            reminder.id.toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(reminder.title)
            .setContentText(getString(R.string.notification_alarm_body, timeDisplay))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setOngoing(true)
            .setAutoCancel(false)
            // Vibrate + default sound so the alarm is audible even without AlarmActivity
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
    }

    // ── startForeground helper ────────────────────────────────────────────────

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    // ── Public helpers (called by AlarmActivity) ──────────────────────────────

    companion object {
        const val CHANNEL_ID = "alarm_channel"
        const val NOTIFICATION_ID = 1001
        private const val TAG = "AlarmForegroundService"

        /** Creates an Intent to stop this service (called from AlarmActivity after dismiss/snooze). */
        fun stopIntent(context: Context): Intent =
            Intent(context, AlarmForegroundService::class.java)

        /** Creates and registers the alarm notification channel. Safe to call multiple times. */
        fun ensureNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_alarm_name),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = context.getString(R.string.notification_channel_alarm_description)
                    enableVibration(true)
                    setBypassDnd(true)        // alarm must bypass Do Not Disturb
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.createNotificationChannel(channel)
            }
        }
    }
}
