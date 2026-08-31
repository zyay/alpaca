package com.alpaca.app.ui.settings

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alpaca.app.billing.BillingManager
import com.alpaca.app.data.datastore.UserPrefs
import com.alpaca.app.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val container: AppContainer) : ViewModel() {
    val prefs: StateFlow<UserPrefs> =
        container.prefs.prefs.stateIn(viewModelScope, SharingStarted.Eagerly, UserPrefs())

    data class BillingUi(
        val connected: Boolean = false,
        val priceText: String? = null
    )

    private val _billing = MutableStateFlow(BillingUi())
    val billing: StateFlow<BillingUi> = _billing

    init {
        viewModelScope.launch {
            val ok = container.billingManager.connect()
            _billing.value = _billing.value.copy(connected = ok)
            container.billingManager.priceText.collect { price ->
                _billing.value = _billing.value.copy(priceText = price)
            }
        }
    }

    fun setSound(enabled: Boolean) = viewModelScope.launch { container.prefs.setSound(enabled) }
    fun setHaptics(enabled: Boolean) = viewModelScope.launch { container.prefs.setHaptics(enabled) }
    fun setName(name: String) = viewModelScope.launch { container.prefs.setDisplayName(name) }
    fun setMax(enabled: Boolean) = viewModelScope.launch { container.prefs.setAlpacaMax(enabled) }
    fun setDynamicColor(enabled: Boolean) =
        viewModelScope.launch { container.prefs.setDynamicColor(enabled) }

    fun buyMax(activity: Activity): Boolean =
        if (_billing.value.connected) {
            container.billingManager.launchPurchase(activity)
        } else {
            false
        }

    fun restorePurchases() = container.billingManager.restorePurchases()
}
