package com.justsimple.reminder.ui.reminders

data class ReminderUiModel(
    val id: Long,
    val title: String,
    val dateLabel: String,        // "Mon, 14 Jul"
    val timeLabel: String,        // "07:30"  or  "7:30 AM"
    val recurrenceLabel: String,  // "Every day"
    val timeRemaining: String,    // "2h", "3 days", ""
    val enabled: Boolean,
    val nextTriggerAt: Long,
    val isExpired: Boolean,       // ONCE reminder whose alarm time has passed — cannot be re-enabled
)
