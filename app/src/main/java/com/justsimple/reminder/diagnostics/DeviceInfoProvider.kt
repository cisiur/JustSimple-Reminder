package com.justsimple.reminder.diagnostics

import android.app.AlarmManager
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.os.Process
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
    val canUseFullScreenIntent: Boolean,
    /** Whether the app may start activities from the background (AppOps, API 29+). */
    val canStartActivityFromBackground: Boolean,
)

@Singleton
class DeviceInfoProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun get(): DeviceInfo {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Detect OEM early so we can adjust permission checks per manufacturer.
        val oemBrand = OemDetector.detect()
        val isMiui = oemBrand == OemBrand.XIAOMI

        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            am.canScheduleExactAlarms()
        } else true

        // On API 34+: check the standard USE_FULL_SCREEN_INTENT permission.
        // On MIUI pre-34: the "Display on lock screen" permission is MIUI-specific and cannot be
        // read via standard APIs — default to false so the user gets a prompt to verify it.
        // On non-MIUI pre-34: this permission is auto-granted by the system.
        val canFullScreen = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
                context.packageManager.checkPermission(
                    android.Manifest.permission.USE_FULL_SCREEN_INTENT, context.packageName
                ) == PackageManager.PERMISSION_GRANTED
            isMiui -> false  // assume denied — better a false negative than a false positive
            else -> true
        }

        // Background activity launch check (API 29+).
        // AppOpsManager.OPSTR_START_ACTIVITIES_FROM_BACKGROUND is @hide in the public SDK,
        // so we use the raw string value directly — same constant, no reflection needed.
        // On MIUI this maps to "Open new windows while running in the background".
        // Default: false on MIUI (conservative — user must explicitly confirm grant),
        //          true on other ROMs (the op string may simply be unknown, assume granted).
        val canStartFromBg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
                appOps.checkOpNoThrow(
                    "android:start_activities_from_background",
                    Process.myUid(),
                    context.packageName,
                ) == AppOpsManager.MODE_ALLOWED
            }.getOrDefault(!isMiui) // on MIUI default to false, on others assume granted
        } else true

        return DeviceInfo(
            manufacturer = Build.MANUFACTURER,
            brand = Build.BRAND,
            model = Build.MODEL,
            androidVersion = Build.VERSION.RELEASE,
            sdkInt = Build.VERSION.SDK_INT,
            oemBrand = oemBrand,
            canScheduleExactAlarms = canExact,
            isIgnoringBatteryOptimizations = pm.isIgnoringBatteryOptimizations(context.packageName),
            canUseFullScreenIntent = canFullScreen,
            canStartActivityFromBackground = canStartFromBg,
        )
    }
}

