package com.justsimple.reminder

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class JustSimpleReminderApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Module 11: RevenueCat.configure()
        // Module 12: MobileAds.initialize()
    }
}
