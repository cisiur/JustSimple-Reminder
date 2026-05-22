package com.justsimple.reminder.domain.entitlement

import com.justsimple.reminder.billing.RevenueCatManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PremiumManager @Inject constructor(
    private val revenueCatManager: RevenueCatManager,
) {
    private val _isPremium = MutableStateFlow(false)

    fun isPremium(): Boolean = _isPremium.value
    fun observePremiumStatus(): Flow<Boolean> = _isPremium.asStateFlow()

    suspend fun refreshPremiumStatus() {
        val customerInfo = revenueCatManager.getCustomerInfo()
        _isPremium.value = customerInfo.entitlements[ENTITLEMENT_ID]?.isActive == true
    }

    internal fun setPremium(premium: Boolean) {
        _isPremium.value = premium
    }

    companion object {
        const val ENTITLEMENT_ID = "premium"
    }
}
