package com.justsimple.reminder.ui.paywall

import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.justsimple.reminder.billing.PurchaseOutcome
import com.justsimple.reminder.billing.RevenueCatManager
import com.justsimple.reminder.domain.entitlement.PremiumManager
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PaywallViewModel @Inject constructor(
    private val revenueCatManager: RevenueCatManager,
    private val premiumManager: PremiumManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaywallUiState())
    val uiState: StateFlow<PaywallUiState> = _uiState.asStateFlow()

    init {
        loadOfferings()
    }

    fun loadOfferings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val packages = revenueCatManager.getOfferings()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        packages = packages,
                        selectedPackage = packages.firstOrNull(),
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun selectPackage(pkg: Package) {
        _uiState.update { it.copy(selectedPackage = pkg) }
    }

    fun purchase(activity: ComponentActivity) {
        val pkg = _uiState.value.selectedPackage ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isPurchasing = true, errorMessage = null) }
            when (val result = revenueCatManager.purchase(activity, pkg)) {
                is PurchaseOutcome.Success -> handleCustomerInfo(result.customerInfo)
                PurchaseOutcome.Cancelled -> _uiState.update { it.copy(isPurchasing = false) }
                is PurchaseOutcome.Failure -> _uiState.update {
                    it.copy(isPurchasing = false, errorMessage = result.message)
                }
            }
        }
    }

    fun restore() {
        viewModelScope.launch {
            _uiState.update { it.copy(isPurchasing = true, errorMessage = null) }
            try {
                val customerInfo = revenueCatManager.restorePurchases()
                handleCustomerInfo(customerInfo)
            } catch (e: Exception) {
                _uiState.update { it.copy(isPurchasing = false, errorMessage = e.message) }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun handleCustomerInfo(customerInfo: CustomerInfo) {
        val isPremium = customerInfo.entitlements[PremiumManager.ENTITLEMENT_ID]?.isActive == true
        premiumManager.setPremium(isPremium)
        _uiState.update { it.copy(isPurchasing = false, purchaseSuccess = isPremium) }
    }
}
