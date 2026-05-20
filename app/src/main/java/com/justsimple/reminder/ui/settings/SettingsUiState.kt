package com.justsimple.reminder.ui.settings

data class SettingsUiState(
    val use24hFormat: Boolean = false,
    val isPremium: Boolean = false,
    val appVersion: String = "",
    val showRestoreSuccess: Boolean = false,
    val showRestoreFailure: Boolean = false,
)
