package com.justsimple.reminder.domain.entitlement

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PremiumManager @Inject constructor() {
    private val _isPremium = MutableStateFlow(false)

    fun isPremium(): Boolean = _isPremium.value
    fun observePremiumStatus(): Flow<Boolean> = _isPremium.asStateFlow()
    suspend fun refreshPremiumStatus() {
        // TODO Module 11: query RevenueCat CustomerInfo
    }

    internal fun setPremium(premium: Boolean) {
        _isPremium.value = premium
    }
}

