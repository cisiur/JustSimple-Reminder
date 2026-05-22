package com.justsimple.reminder

import android.app.Application
import com.justsimple.reminder.alarms.service.AlarmForegroundService
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class JustSimpleReminderApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AlarmForegroundService.ensureNotificationChannel(this)
        Purchases.configure(
            PurchasesConfiguration.Builder(this, BuildConfig.REVENUECAT_API_KEY).build()
        )
        // Module 12: MobileAds.initialize()
    }
}
