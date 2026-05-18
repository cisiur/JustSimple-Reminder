package com.justsimple.reminder.ui.alarm

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.justsimple.reminder.data.repository.ReminderRepository
import com.justsimple.reminder.domain.recurrence.RecurrenceEngine
import com.justsimple.reminder.domain.scheduler.AlarmScheduler
import com.justsimple.reminder.domain.settings.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject

@HiltViewModel
class AlarmViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ReminderRepository,
    private val scheduler: AlarmScheduler,
    private val recurrenceEngine: RecurrenceEngine,
    private val userPreferences: UserPreferences,
) : ViewModel() {

    private val reminderId: Long =
        savedStateHandle.get<Long>(AlarmActivity.EXTRA_REMINDER_ID) ?: -1L

    private val _uiState = MutableStateFlow(AlarmUiState())
    val uiState: StateFlow<AlarmUiState> = _uiState.asStateFlow()

    private val timeInputFormatter = DateTimeFormatter.ofPattern("HH:mm")

    init {
        viewModelScope.launch {
            val reminder = repository.getById(reminderId) ?: return@launch
            val use24h = userPreferences.use24hFormat.first()
            val time = LocalTime.parse(reminder.scheduledTime, timeInputFormatter)
            val label = if (use24h) {
                time.format(DateTimeFormatter.ofPattern("HH:mm"))
            } else {
                time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
            }
            _uiState.update { it.copy(title = reminder.title, scheduledTimeLabel = label, isLoading = false) }
        }
    }

    fun dismiss() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val reminder = repository.getById(reminderId) ?: return@launch
            val next = recurrenceEngine.nextTriggerMillis(reminder, now)
            val enabled = next != null && next > 0L

            repository.markTriggered(
                id = reminderId,
                lastTriggeredAt = now,
                nextTriggerAt = next ?: 0L,
                enabled = enabled,
            )
            if (enabled && next != null) {
                scheduler.schedule(reminder.copy(nextTriggerAt = next, enabled = true))
            } else {
                scheduler.cancel(reminderId)
            }
            _uiState.update { it.copy(isDismissed = true) }
        }
    }

    fun snooze(minutes: Int) {
        viewModelScope.launch {
            val snoozeUntil = System.currentTimeMillis() + minutes * 60_000L
            repository.applySnooze(reminderId, snoozeUntil)
            val reminder = repository.getById(reminderId) ?: return@launch
            scheduler.schedule(reminder)
            _uiState.update { it.copy(isDismissed = true) }
        }
    }
}
