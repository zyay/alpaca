package com.alpaca.app.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.alpaca.app.data.datastore.UserPreferencesStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Play Billing for the Alpaca Max subscription. Ownership is mirrored into
 * DataStore (`prefs.alpacaMax`) so the rest of the app reads one flag.
 * Requires the `alpaca_max_monthly` subscription to exist in Play Console.
 */
class BillingManager(
    context: Context,
    private val prefs: UserPreferencesStore
) : PurchasesUpdatedListener {

    companion object {
        const val PRODUCT_MAX = "alpaca_max_monthly"
    }

    private val client = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected

    private val _priceText = MutableStateFlow<String?>(null)
    val priceText: StateFlow<String?> = _priceText

    private var maxDetails: ProductDetails? = null

    suspend fun connect(): Boolean = suspendCancellableCoroutine { cont ->
        if (_connected.value) {
            cont.resume(true)
            return@suspendCancellableCoroutine
        }
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                val ok = result.responseCode == BillingClient.BillingResponseCode.OK
                _connected.value = ok
                if (ok) {
                    restorePurchases()
                    queryPrice()
                }
                if (cont.isActive) cont.resume(ok)
            }

            override fun onBillingServiceDisconnected() {
                _connected.value = false
            }
        })
    }

    private fun queryPrice() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_MAX)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            )
            .build()
        client.queryProductDetailsAsync(params) { result, detailsList ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val details = detailsList.firstOrNull()
                maxDetails = details
                _priceText.value = details?.subscriptionOfferDetails
                    ?.firstOrNull()?.pricingPhases?.pricingPhaseList
                    ?.firstOrNull()?.formattedPrice
            }
        }
    }

    fun restorePurchases() {
        client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                handlePurchases(purchases)
            }
        }
    }

    /** Returns false when the billing service isn't connected yet. */
    fun launchPurchase(activity: Activity): Boolean {
        val details = maxDetails ?: return false
        val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken
            ?: return false
        val flow = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .setOfferToken(offerToken)
                        .build()
                )
            )
            .build()
        return client.launchBillingFlow(activity, flow).responseCode ==
            BillingClient.BillingResponseCode.OK
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            handlePurchases(purchases)
        }
    }

    private fun handlePurchases(purchases: List<Purchase>) {
        purchases
            .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            .filter { purchase -> purchase.products.contains(PRODUCT_MAX) }
            .forEach { purchase ->
                if (!purchase.isAcknowledged) {
                    client.acknowledgePurchase(
                        AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken)
                            .build()
                    ) { }
                }
                scope.launch { prefs.setAlpacaMax(true) }
            }
    }
}
