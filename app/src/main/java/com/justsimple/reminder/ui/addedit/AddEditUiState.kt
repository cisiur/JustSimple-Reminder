package com.justsimple.reminder.ui.addedit

import com.justsimple.reminder.domain.recurrence.RecurrenceType
import java.time.LocalDate
import java.time.LocalTime

data class AddEditUiState(
    val title: String = "",
    val scheduledDate: LocalDate = LocalDate.now(),
    val scheduledTime: LocalTime = LocalTime.of(9, 0),
    val recurrenceType: RecurrenceType = RecurrenceType.ONCE,
    val isLoading: Boolean = true,
    val isSaved: Boolean = false,
    val showFreeTierDialog: Boolean = false,
    val showPastDateError: Boolean = false,
    val titleError: Boolean = false,
)
