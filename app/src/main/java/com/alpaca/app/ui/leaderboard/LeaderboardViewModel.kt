package com.alpaca.app.ui.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alpaca.app.di.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class LeaderboardViewModel(container: AppContainer) : ViewModel() {
    val userXp: StateFlow<Int> =
        container.gamificationRepository.observeUser()
            .map { it.xp }
            .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val displayName: StateFlow<String> =
        container.prefs.prefs
            .map { it.displayName }
            .stateIn(viewModelScope, SharingStarted.Eagerly, "Explorer")
}
