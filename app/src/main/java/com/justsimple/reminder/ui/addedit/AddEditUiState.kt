package com.justsimple.reminder.ui.addedit

import com.justsimple.reminder.domain.recurrence.RecurrenceType
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

data class AddEditUiState(
    val title: String = "",
    val scheduledDate: LocalDate = LocalDate.now(),
    // Default to now + 5 min (seconds/nanos stripped) so the alarm is always in the future.
    val scheduledTime: LocalTime = LocalTime.now().plusMinutes(5).truncatedTo(ChronoUnit.MINUTES),
    val recurrenceType: RecurrenceType = RecurrenceType.ONCE,
    val isLoading: Boolean = true,
    val isSaved: Boolean = false,
    val showFreeTierDialog: Boolean = false,
    val showPastDateError: Boolean = false,
)
