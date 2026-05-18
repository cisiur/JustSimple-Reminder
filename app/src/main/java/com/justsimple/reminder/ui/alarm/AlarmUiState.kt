package com.justsimple.reminder.ui.alarm

data class AlarmUiState(
    val title: String = "",
    val scheduledTimeLabel: String = "",
    val isLoading: Boolean = true,
    val isDismissed: Boolean = false,
)
