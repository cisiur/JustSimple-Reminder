package com.justsimple.reminder.reliability

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.justsimple.reminder.diagnostics.OemBrand
import com.justsimple.reminder.diagnostics.OemDetector
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OemReliabilityGuide @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun openBatteryOptimizationSettings() {
        safeStart(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    }

    fun openAppSettings() {
        safeStart(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
        )
    }

    fun openXiaomiAutostart() {
        val intent = Intent("miui.intent.action.APP_PERM_EDITOR").apply {
            setClassName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
        }
        if (!safeStart(intent)) openAppSettings()
    }

    fun openAlarmPermissionSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            safeStart(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
            )
        }
    }

    private fun safeStart(intent: Intent): Boolean {
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }
}

