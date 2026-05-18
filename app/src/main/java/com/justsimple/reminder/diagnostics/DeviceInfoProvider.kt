package com.justsimple.reminder.diagnostics

import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class DeviceInfo(
    val manufacturer: String,
    val brand: String,
    val model: String,
    val androidVersion: String,
    val sdkInt: Int,
    val oemBrand: OemBrand,
    val canScheduleExactAlarms: Boolean,
    val isIgnoringBatteryOptimizations: Boolean,
    val canUseFullScreenIntent: Boolean
)

@Singleton
class DeviceInfoProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun get(): DeviceInfo {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            am.canScheduleExactAlarms()
        } else true
        val canFullScreen = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            context.packageManager.checkPermission(
                android.Manifest.permission.USE_FULL_SCREEN_INTENT, context.packageName
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        return DeviceInfo(
            manufacturer = Build.MANUFACTURER,
            brand = Build.BRAND,
            model = Build.MODEL,
            androidVersion = Build.VERSION.RELEASE,
            sdkInt = Build.VERSION.SDK_INT,
            oemBrand = OemDetector.detect(),
            canScheduleExactAlarms = canExact,
            isIgnoringBatteryOptimizations = pm.isIgnoringBatteryOptimizations(context.packageName),
            canUseFullScreenIntent = canFullScreen
        )
    }
}

