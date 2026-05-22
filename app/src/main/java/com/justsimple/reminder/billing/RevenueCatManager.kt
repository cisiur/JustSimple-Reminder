package com.justsimple.reminder.billing

import androidx.activity.ComponentActivity
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesTransactionException
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitPurchase
import com.revenuecat.purchases.awaitRestore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RevenueCatManager @Inject constructor() {

    suspend fun getOfferings(): List<Package> =
        Purchases.sharedInstance.awaitOfferings().current?.availablePackages.orEmpty()

    suspend fun purchase(activity: ComponentActivity, pkg: Package): PurchaseOutcome {
        return try {
            val params = PurchaseParams.Builder(activity, pkg).build()
            val result = Purchases.sharedInstance.awaitPurchase(params)
            PurchaseOutcome.Success(result.customerInfo)
        } catch (e: PurchasesTransactionException) {
            if (e.userCancelled) PurchaseOutcome.Cancelled
            else PurchaseOutcome.Failure(e.message)
        }
    }

    suspend fun restorePurchases(): CustomerInfo =
        Purchases.sharedInstance.awaitRestore()

    suspend fun getCustomerInfo(): CustomerInfo =
        Purchases.sharedInstance.awaitCustomerInfo()
}

sealed class PurchaseOutcome {
    data class Success(val customerInfo: CustomerInfo) : PurchaseOutcome()
    data object Cancelled : PurchaseOutcome()
    data class Failure(val message: String?) : PurchaseOutcome()
}
