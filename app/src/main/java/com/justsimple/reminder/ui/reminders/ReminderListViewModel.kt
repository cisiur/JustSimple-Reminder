package com.justsimple.reminder.ui.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.justsimple.reminder.data.repository.ReminderRepository
import com.justsimple.reminder.diagnostics.DeviceInfoProvider
import com.justsimple.reminder.domain.entitlement.PremiumManager
import com.justsimple.reminder.domain.formatter.ReminderDisplayFormatter
import com.justsimple.reminder.domain.recurrence.RecurrenceType
import com.justsimple.reminder.domain.scheduler.AlarmScheduler
import com.justsimple.reminder.domain.settings.UserPreferences
import com.justsimple.reminder.domain.usecase.RescheduleAlarmsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReminderListUiState(
    val reminders: List<ReminderUiModel> = emptyList(),
    val isPremium: Boolean = false,
    val showBatteryWarning: Boolean = false,
    val showExactAlarmWarning: Boolean = false,
    val showFreeTierDialog: Boolean = false,
)

@HiltViewModel
class ReminderListViewModel @Inject constructor(
    private val repository: ReminderRepository,
    private val scheduler: AlarmScheduler,
    private val premiumManager: PremiumManager,
    private val userPreferences: UserPreferences,
    private val formatter: ReminderDisplayFormatter,
    private val deviceInfoProvider: DeviceInfoProvider,
    private val rescheduleAlarms: RescheduleAlarmsUseCase,
) : ViewModel() {

    private val _showBatteryWarning = MutableStateFlow(false)
    private val _showExactAlarmWarning = MutableStateFlow(false)
    private val _showFreeTierDialog = MutableStateFlow(false)

    init {
        // Force a fresh CustomerInfo fetch from RevenueCat on every app start.
        // The updatedCustomerInfoListener in PremiumManager handles the result —
        // this call is a safety net for devices where the listener fires late.
        viewModelScope.launch { premiumManager.refreshPremiumStatus() }
    }

    val uiState: StateFlow<ReminderListUiState> = combine(
        repository.observeAll(),
        premiumManager.observePremiumStatus(),
        userPreferences.use24hFormat,
        _showBatteryWarning,
        _showExactAlarmWarning,
    ) { reminders, isPremium, use24h, batteryWarn, alarmWarn ->
        val now = System.currentTimeMillis()
        ReminderListUiState(
            reminders = reminders.map { formatter.toUiModel(it, use24h, now) },
            isPremium = isPremium,
            showBatteryWarning = batteryWarn,
            showExactAlarmWarning = alarmWarn,
        )
    }.combine(_showFreeTierDialog) { state, showDialog ->
        state.copy(showFreeTierDialog = showDialog)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ReminderListUiState(),
    )

    fun refreshWarnings() {
        val info = deviceInfoProvider.get()
        val hadExactAlarmWarning = _showExactAlarmWarning.value
        _showBatteryWarning.value = !info.isIgnoringBatteryOptimizations
        _showExactAlarmWarning.value = !info.canScheduleExactAlarms

        // Permission was just granted — reschedule any alarms that were skipped
        // while canScheduleExact() was false (broadcast may not have arrived yet).
        if (hadExactAlarmWarning && info.canScheduleExactAlarms) {
            viewModelScope.launch { rescheduleAlarms() }
        }
    }

    fun dismissFreeTierDialog() {
        _showFreeTierDialog.value = false
    }

    fun setEnabled(id: Long, enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) {
                val reminder = repository.getById(id) ?: return@launch
                // ONCE reminders whose alarm time has passed cannot be re-enabled
                if (reminder.recurrenceType == RecurrenceType.ONCE &&
                    reminder.nextTriggerAt <= System.currentTimeMillis()
                ) return@launch

                val isPremium = premiumManager.observePremiumStatus().first()
                if (!isPremium && repository.countEnabled() >= 2) {
                    _showFreeTierDialog.value = true
                    return@launch
                }
            }
            repository.setEnabled(id, enabled)
            val reminder = repository.getById(id) ?: return@launch
            if (enabled) scheduler.schedule(reminder) else scheduler.cancel(id)
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            scheduler.cancel(id)
            repository.delete(id)
        }
    }
}
