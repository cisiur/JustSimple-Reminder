package com.justsimple.reminder.ui.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.justsimple.reminder.data.repository.ReminderRepository
import com.justsimple.reminder.diagnostics.DeviceInfoProvider
import com.justsimple.reminder.domain.entitlement.PremiumManager
import com.justsimple.reminder.domain.formatter.ReminderDisplayFormatter
import com.justsimple.reminder.domain.scheduler.AlarmScheduler
import com.justsimple.reminder.domain.settings.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReminderListUiState(
    val reminders: List<ReminderUiModel> = emptyList(),
    val isPremium: Boolean = false,
    val showBatteryWarning: Boolean = false,
    val showExactAlarmWarning: Boolean = false,
)

@HiltViewModel
class ReminderListViewModel @Inject constructor(
    private val repository: ReminderRepository,
    private val scheduler: AlarmScheduler,
    private val premiumManager: PremiumManager,
    private val userPreferences: UserPreferences,
    private val formatter: ReminderDisplayFormatter,
    private val deviceInfoProvider: DeviceInfoProvider,
) : ViewModel() {

    private val _showBatteryWarning = MutableStateFlow(false)
    private val _showExactAlarmWarning = MutableStateFlow(false)

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
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ReminderListUiState(),
    )

    /** Called every time the screen resumes so warning banners reflect current state. */
    fun refreshWarnings() {
        val info = deviceInfoProvider.get()
        _showBatteryWarning.value = !info.isIgnoringBatteryOptimizations
        _showExactAlarmWarning.value = !info.canScheduleExactAlarms
    }

    fun setEnabled(id: Long, enabled: Boolean) {
        viewModelScope.launch {
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
