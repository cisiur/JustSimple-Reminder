package com.justsimple.reminder.ui.diagnostics

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.justsimple.reminder.R
import com.justsimple.reminder.data.repository.ReminderRepository
import com.justsimple.reminder.diagnostics.DeviceInfoProvider
import com.justsimple.reminder.diagnostics.OemBrand
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject

data class DiagnosticsUiState(
    val notificationsGranted: Boolean = true,
    val exactAlarmsGranted: Boolean = true,
    val batteryOptimizationOff: Boolean = true,
    val fullScreenIntentGranted: Boolean = true,
    /** Only meaningful (and shown) on API 34+ where USE_FULL_SCREEN_INTENT became a real permission. */
    val showFullScreenCheck: Boolean = false,
    /** Show MIUI-specific "Other Permissions" rows on Xiaomi/Redmi/POCO devices. */
    val showMiuiPermissions: Boolean = false,
    val manufacturer: String = "",
    val model: String = "",
    val androidVersion: String = "",
    val sdkInt: Int = 0,
    val oemBrand: OemBrand = OemBrand.OTHER,
    val nextAlarmText: String = "",
    val isLoading: Boolean = true,
)

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceInfoProvider: DeviceInfoProvider,
    private val repository: ReminderRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiagnosticsUiState())
    val uiState: StateFlow<DiagnosticsUiState> = _uiState.asStateFlow()

    /** Call on every RESUMED to keep data fresh (user may have changed permissions). */
    fun refresh() {
        viewModelScope.launch {
            val info = deviceInfoProvider.get()

            val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
            } else true

            val nextMillis = repository.getNextScheduledMillis()
            val nextText = if (nextMillis != null) {
                val dt = Instant.ofEpochMilli(nextMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime()
                DateTimeFormatter
                    .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                    .format(dt)
            } else {
                context.getString(R.string.diag_none)
            }

            _uiState.update {
                it.copy(
                    notificationsGranted = notifGranted,
                    exactAlarmsGranted = info.canScheduleExactAlarms,
                    batteryOptimizationOff = info.isIgnoringBatteryOptimizations,
                    fullScreenIntentGranted = info.canUseFullScreenIntent,
                    showFullScreenCheck = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
                    showMiuiPermissions = info.oemBrand == OemBrand.XIAOMI,
                    manufacturer = info.manufacturer,
                    model = info.model,
                    androidVersion = info.androidVersion,
                    sdkInt = info.sdkInt,
                    oemBrand = info.oemBrand,
                    nextAlarmText = nextText,
                    isLoading = false,
                )
            }
        }
    }
}
