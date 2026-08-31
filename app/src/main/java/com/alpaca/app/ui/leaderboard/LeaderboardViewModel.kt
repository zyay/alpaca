package com.alpaca.app.ui.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alpaca.app.BuildConfig
import com.alpaca.app.data.league.LeagueClient
import com.alpaca.app.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LeaderboardViewModel(private val container: AppContainer) : ViewModel() {

    data class EntryUi(val id: String, val name: String, val xp: Int, val isYou: Boolean)

    data class UiState(
        val loading: Boolean = true,
        val online: Boolean = false,
        val entries: List<EntryUi> = emptyList(),
        val yourRank: Int? = null,
        val resetsInMs: Long = 0,
        val week: String = "",
        val errorMessage: String? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    private val FakeHerd = listOf(
        EntryUi("bot-1", "Lucía", 812, false),
        EntryUi("bot-2", "Mateo", 704, false),
        EntryUi("bot-3", "Valentina", 655, false),
        EntryUi("bot-4", "Diego", 598, false),
        EntryUi("bot-5", "Sofía", 540, false),
        EntryUi("bot-6", "Sebastián", 481, false),
        EntryUi("bot-7", "Camila", 402, false),
        EntryUi("bot-8", "Nicolás", 350, false),
        EntryUi("bot-9", "Isabella", 287, false),
        EntryUi("bot-10", "Tomás", 233, false)
    )

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val baseUrl = BuildConfig.VERCEL_BASE_URL
            val deviceId = container.prefs.ensureDeviceId()
            if (baseUrl.isBlank()) {
                offlinePreview(deviceId, reason = null)
                return@launch
            }
            container.leagueClient.standings(baseUrl, deviceId).fold(
                onSuccess = { standings ->
                    if (!standings.available) {
                        offlinePreview(
                            deviceId,
                            reason = standings.reason ?: "League backend not configured"
                        )
                    } else {
                        val prefs = container.prefs.prefs.first()
                        val rows = standings.entries.map { entry ->
                            EntryUi(entry.id, entry.name, entry.xp, entry.id == deviceId)
                        }
                        val merged = if (rows.any { it.isYou }) rows else rows + EntryUi(
                            deviceId, prefs.displayName, standings.yourXp ?: 0, true
                        )
                        val sorted = merged.sortedByDescending { it.xp }
                        _state.value = UiState(
                            loading = false,
                            online = true,
                            entries = sorted,
                            yourRank = standings.yourRank
                                ?: sorted.indexOfFirst { it.isYou }.takeIf { it >= 0 }?.plus(1),
                            resetsInMs = standings.resetsInMs,
                            week = standings.week
                        )
                    }
                },
                onFailure = { err ->
                    offlinePreview(deviceId, reason = err.message ?: "Network error")
                }
            )
        }
    }

    /** No backend configured (CI builds / offline): show the local herd. */
    private suspend fun offlinePreview(deviceId: String, reason: String?) {
        val prefs = container.prefs.prefs.first()
        val user = container.gamificationRepository.currentUser()
        val rows = (FakeHerd + EntryUi(deviceId, prefs.displayName, user.xp, true))
            .sortedByDescending { it.xp }
        _state.value = UiState(
            loading = false,
            online = false,
            entries = rows,
            errorMessage = reason
        )
    }
}
