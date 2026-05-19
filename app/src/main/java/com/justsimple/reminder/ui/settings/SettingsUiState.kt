package com.justsimple.reminder.ui.settings

data class SettingsUiState(
    val use24hFormat: Boolean = false,
    val isPremium: Boolean = false,
    // Standard permissions
    val notificationsGranted: Boolean = true,
    val exactAlarmsGranted: Boolean = true,
    // Xiaomi / MIUI "Other Permissions" — only shown on MIUI devices.
    // We cannot read these states via standard Android APIs (they are MIUI-proprietary),
    // so the rows are always shown as "tap to verify" without a granted/denied indicator.
    val showMiuiPermissions: Boolean = false,
    val appVersion: String = "",
    val showRestoreSuccess: Boolean = false,
    val showRestoreFailure: Boolean = false,
)
