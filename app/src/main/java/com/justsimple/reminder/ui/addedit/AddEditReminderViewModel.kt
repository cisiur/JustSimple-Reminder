package com.justsimple.reminder.ui.addedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.justsimple.reminder.data.db.ReminderEntity
import com.justsimple.reminder.data.repository.ReminderRepository
import com.justsimple.reminder.domain.entitlement.PremiumManager
import com.justsimple.reminder.domain.recurrence.RecurrenceEngine
import com.justsimple.reminder.domain.recurrence.RecurrenceType
import com.justsimple.reminder.domain.scheduler.AlarmScheduler
import com.justsimple.reminder.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class AddEditReminderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ReminderRepository,
    private val scheduler: AlarmScheduler,
    private val premiumManager: PremiumManager,
    private val recurrenceEngine: RecurrenceEngine,
) : ViewModel() {

    private val reminderId: Long? = savedStateHandle.get<Long>(Screen.EditReminder.ARG)

    private val _uiState = MutableStateFlow(AddEditUiState())
    val uiState: StateFlow<AddEditUiState> = _uiState.asStateFlow()

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    init {
        viewModelScope.launch {
            if (reminderId != null) {
                val reminder = repository.getById(reminderId)
                if (reminder != null) {
                    _uiState.update {
                        it.copy(
                            title = reminder.title,
                            scheduledDate = LocalDate.parse(reminder.scheduledDate),
                            scheduledTime = LocalTime.parse(reminder.scheduledTime, timeFormatter),
                            recurrenceType = reminder.recurrenceType,
                            isLoading = false,
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onTitleChange(value: String) =
        _uiState.update { it.copy(title = value) }

    fun onDateChange(date: LocalDate) =
        _uiState.update { it.copy(scheduledDate = date, showPastDateError = false) }

    fun onTimeChange(time: LocalTime) =
        _uiState.update { it.copy(scheduledTime = time, showPastDateError = false) }

    fun onRecurrenceChange(type: RecurrenceType) =
        _uiState.update { it.copy(recurrenceType = type, showPastDateError = false) }

    fun dismissFreeTierDialog() = _uiState.update { it.copy(showFreeTierDialog = false) }

    fun dismissPastDateError() = _uiState.update { it.copy(showPastDateError = false) }

    fun save() {
        val state = _uiState.value
        // Use "Reminder" as the fallback title when the user leaves the field empty.
        val effectiveTitle = state.title.trim().ifBlank { DEFAULT_TITLE }

        viewModelScope.launch {
            val now = System.currentTimeMillis()

            // Validate: ONCE reminder must be in the future
            if (state.recurrenceType == RecurrenceType.ONCE) {
                val selectedMillis = state.scheduledDate
                    .atTime(state.scheduledTime)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
                if (selectedMillis <= now) {
                    _uiState.update { it.copy(showPastDateError = true) }
                    return@launch
                }
            }

            val dateStr = state.scheduledDate.toString()
            val timeStr = state.scheduledTime.format(timeFormatter)

            val existing = if (reminderId != null) repository.getById(reminderId) else null
            val base = if (existing != null) {
                existing.copy(
                    title = effectiveTitle,
                    scheduledDate = dateStr,
                    scheduledTime = timeStr,
                    recurrenceType = state.recurrenceType,
                    updatedAt = now,
                )
            } else {
                ReminderEntity(
                    title = effectiveTitle,
                    scheduledDate = dateStr,
                    scheduledTime = timeStr,
                    recurrenceType = state.recurrenceType,
                    enabled = true,
                    nextTriggerAt = 0L,
                    lastTriggeredAt = null,
                    snoozeUntil = null,
                    createdAt = now,
                    updatedAt = now,
                )
            }

            val nextTrigger = recurrenceEngine.nextTriggerMillis(base, now) ?: 0L
            val toSave = base.copy(nextTriggerAt = nextTrigger, enabled = nextTrigger > 0L)

            // Free tier check: applies whenever a reminder would become enabled,
            // and it wasn't already enabled before this save.
            val wasAlreadyEnabled = existing?.enabled == true
            if (toSave.enabled && !wasAlreadyEnabled) {
                val isPremium = premiumManager.observePremiumStatus().first()
                if (!isPremium && repository.countEnabled() >= 2) {
                    _uiState.update { it.copy(showFreeTierDialog = true) }
                    return@launch
                }
            }

            if (existing != null) {
                repository.update(toSave)
                if (toSave.enabled) scheduler.schedule(toSave) else scheduler.cancel(toSave.id)
            } else {
                val insertedId = repository.insert(toSave)
                val inserted = toSave.copy(id = insertedId)
                if (inserted.enabled) scheduler.schedule(inserted)
            }

            _uiState.update { it.copy(isSaved = true) }
        }
    }

    companion object {
        private const val DEFAULT_TITLE = "Reminder"
    }
}
