package com.justsimple.reminder.ui.paywall

import com.revenuecat.purchases.Package

data class PaywallUiState(
    val isLoading: Boolean = true,
    val packages: List<Package> = emptyList(),
    val selectedPackage: Package? = null,
    val isPurchasing: Boolean = false,
    val purchaseSuccess: Boolean = false,
    // non-null when offerings failed to load (→ full-screen error) or a purchase/restore fails (→ snackbar)
    val errorMessage: String? = null,
)
