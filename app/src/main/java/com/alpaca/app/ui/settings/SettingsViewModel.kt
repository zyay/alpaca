package com.alpaca.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alpaca.app.data.datastore.UserPrefs
import com.alpaca.app.di.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val container: AppContainer) : ViewModel() {
    val prefs: StateFlow<UserPrefs> =
        container.prefs.prefs.stateIn(viewModelScope, SharingStarted.Eagerly, UserPrefs())

    fun setSound(enabled: Boolean) = viewModelScope.launch { container.prefs.setSound(enabled) }
    fun setHaptics(enabled: Boolean) = viewModelScope.launch { container.prefs.setHaptics(enabled) }
    fun setName(name: String) = viewModelScope.launch { container.prefs.setDisplayName(name) }
    fun setMax(enabled: Boolean) = viewModelScope.launch { container.prefs.setAlpacaMax(enabled) }
    fun setDynamicColor(enabled: Boolean) =
        viewModelScope.launch { container.prefs.setDynamicColor(enabled) }
}
