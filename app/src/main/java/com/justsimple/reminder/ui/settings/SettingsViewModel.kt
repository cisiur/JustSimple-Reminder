package com.justsimple.reminder.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.justsimple.reminder.domain.entitlement.PremiumManager
import com.justsimple.reminder.domain.settings.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferences: UserPreferences,
    private val premiumManager: PremiumManager,
) : ViewModel() {

    private val _extras = MutableStateFlow(
        SettingsUiState(
            appVersion = runCatching {
                context.packageManager
                    .getPackageInfo(context.packageName, 0)
                    .versionName ?: ""
            }.getOrDefault(""),
        )
    )

    val uiState: StateFlow<SettingsUiState> = combine(
        _extras,
        userPreferences.use24hFormat,
        premiumManager.observePremiumStatus(),
    ) { extras, use24h, isPremium ->
        extras.copy(use24hFormat = use24h, isPremium = isPremium)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    fun setUse24hFormat(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setUse24hFormat(enabled) }
    }

    fun restorePurchases() {
        viewModelScope.launch {
            try {
                premiumManager.refreshPremiumStatus()
                _extras.update { it.copy(showRestoreSuccess = true) }
            } catch (_: Exception) {
                _extras.update { it.copy(showRestoreFailure = true) }
            }
        }
    }

    fun dismissRestoreResult() =
        _extras.update { it.copy(showRestoreSuccess = false, showRestoreFailure = false) }
}
