package com.justsimple.reminder

import android.app.Application
import com.google.android.gms.ads.MobileAds
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
        // Initialize AdMob — must be called once before any ad is loaded.
        // The callback fires when the SDK is ready; we don't need to act on it.
        MobileAds.initialize(this) {}
    }
}
