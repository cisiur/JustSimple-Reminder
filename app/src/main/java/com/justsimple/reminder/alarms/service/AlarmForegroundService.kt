package com.justsimple.reminder.alarms.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.justsimple.reminder.R
import com.justsimple.reminder.alarms.receivers.AlarmActionReceiver
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
 *  2. startForeground() + MediaPlayer start immediately (within 5 s ANR window).
 *  3. AlarmActivity is launched immediately (before the DB fetch).
 *  4. DB fetch happens async; notification is updated with the real reminder title.
 *  5. AlarmActivity (or notification buttons) stop the service, which stops the sound.
 */
@AndroidEntryPoint
class AlarmForegroundService : Service() {

    @Inject lateinit var repository: ReminderRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var mediaPlayer: MediaPlayer? = null

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val reminderId = intent?.getLongExtra(AlarmReceiver.EXTRA_REMINDER_ID, -1L) ?: -1L

        // Start foreground immediately — must happen within 5 s of startForegroundService().
        // The placeholder carries a fullScreenIntent so lock-screen delivery works even
        // before the DB fetch completes.
        startForegroundCompat(buildPlaceholderNotification(reminderId))

        if (reminderId == -1L) {
            Log.e(TAG, "No reminderId in intent — stopping service")
            stopSelf(startId)
            return START_NOT_STICKY
        }

        // Start alarm sound immediately — reliable looping via MediaPlayer on the
        // STREAM_ALARM audio stream (bypasses Do Not Disturb / silent mode).
        playAlarmSound()

        // Launch AlarmActivity NOW — before any async work.
        // Delaying until after the DB fetch causes Android to fall back to a
        // heads-up notification because the window for a synchronous activity
        // launch from a foreground service has already passed.
        val activityIntent = AlarmActivity.createIntent(this, reminderId).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
        }
        startActivity(activityIntent)

        // Async: fetch reminder and update notification with real title + action buttons.
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
        stopAlarmSound()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Sound ─────────────────────────────────────────────────────────────────

    private fun playAlarmSound() {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@AlarmForegroundService, uri)
                isLooping = true
                prepare()
                start()
            }
            Log.d(TAG, "Alarm sound started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play alarm sound", e)
        }
    }

    private fun stopAlarmSound() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping alarm sound", e)
        } finally {
            mediaPlayer = null
        }
    }

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

        // fullScreenIntent — tapping the notification body opens AlarmActivity
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            reminder.id.toInt(),
            AlarmActivity.createIntent(this, reminder.id).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // Action: Dismiss
        val dismissPendingIntent = PendingIntent.getBroadcast(
            this,
            (reminder.id + 10_000L).toInt(),
            Intent(AlarmActionReceiver.ACTION_DISMISS, null, this, AlarmActionReceiver::class.java)
                .putExtra(AlarmActionReceiver.EXTRA_REMINDER_ID, reminder.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // Action: Snooze 10 min
        val snoozePendingIntent = PendingIntent.getBroadcast(
            this,
            (reminder.id + 20_000L).toInt(),
            Intent(AlarmActionReceiver.ACTION_SNOOZE, null, this, AlarmActionReceiver::class.java)
                .putExtra(AlarmActionReceiver.EXTRA_REMINDER_ID, reminder.id),
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
            // setOnlyAlertOnce: updating from placeholder must not re-trigger the heads-up.
            // No setSilent() — channel has no sound so there's no double audio with MediaPlayer.
            .setOnlyAlertOnce(true)
            .addAction(0, getString(R.string.notification_action_dismiss), dismissPendingIntent)
            .addAction(0, getString(R.string.notification_action_snooze), snoozePendingIntent)
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

    // ── Companion ─────────────────────────────────────────────────────────────

    companion object {
        // v2: channel with USAGE_ALARM AudioAttributes (notification sound is now handled
        // by MediaPlayer instead, but the channel is kept for fallback/legacy devices).
        // v3: removed channel sound (MediaPlayer handles audio); IMPORTANCE_HIGH +
        // vibration still triggers the heads-up popup without a double notification ding.
        const val CHANNEL_ID = "alarm_channel_v3"
        const val NOTIFICATION_ID = 1001
        private const val TAG = "AlarmForegroundService"

        fun stopIntent(context: Context): Intent =
            Intent(context, AlarmForegroundService::class.java)

        fun ensureNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_alarm_name),
                    // IMPORTANCE_HIGH = heads-up popup + vibration, no channel sound
                    // (MediaPlayer on STREAM_ALARM handles audio independently).
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = context.getString(R.string.notification_channel_alarm_description)
                    // No setSound() — MediaPlayer plays the alarm ringtone directly.
                    // Vibration alone is enough for Android to show the heads-up popup.
                    enableVibration(true)
                    setBypassDnd(true)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.createNotificationChannel(channel)
            }
        }
    }
}
