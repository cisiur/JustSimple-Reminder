package com.justsimple.reminder

import android.app.Application
import com.justsimple.reminder.alarms.service.AlarmForegroundService
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class JustSimpleReminderApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AlarmForegroundService.ensureNotificationChannel(this)
        // Module 11: RevenueCat.configure()
        // Module 12: MobileAds.initialize()
    }
}
