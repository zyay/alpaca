package com.alpaca.app.ui.quests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alpaca.app.data.db.entities.QuestEntity
import com.alpaca.app.data.db.entities.UserEntity
import com.alpaca.app.di.AppContainer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class QuestsViewModel(private val container: AppContainer) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val quests: List<QuestEntity> = emptyList(),
        val user: UserEntity? = null,
        val message: String? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    init {
        viewModelScope.launch {
            combine(
                container.questRepository.observeQuests(),
                container.gamificationRepository.observeUser()
            ) { quests, user -> quests to user }
                .collect { (quests, user) ->
                    _state.value = _state.value.copy(loading = false, quests = quests, user = user)
                }
        }
    }

    fun claim(questId: String) {
        viewModelScope.launch {
            val gems = container.questRepository.claim(questId)
            if (gems > 0) {
                container.gamificationRepository.addGems(gems)
                show("+ $gems gems")
            } else {
                show("Nothing to claim yet")
            }
        }
    }

    fun buyStreakFreeze() {
        viewModelScope.launch {
            val user = container.gamificationRepository.currentUser()
            when {
                user.streakFreezes >= UserEntity.MAX_FREEZES ->
                    show("Freeze storage is full (${UserEntity.MAX_FREEZES})")
                container.gamificationRepository.buyStreakFreeze() ->
                    show("Streak Freeze equipped")
                else -> show("Not enough gems")
            }
        }
    }

    fun buyEnergyRefill() {
        viewModelScope.launch {
            val user = container.gamificationRepository.currentUser()
            when {
                user.fleeceEnergy >= UserEntity.MAX_ENERGY -> show("Fleece is already full")
                container.gamificationRepository.buyEnergyRefill() ->
                    show("Fleece fully restored")
                else -> show("Not enough gems")
            }
        }
    }

    private fun show(text: String) {
        _state.value = _state.value.copy(message = text)
        viewModelScope.launch {
            delay(2200)
            if (_state.value.message == text) {
                _state.value = _state.value.copy(message = null)
            }
        }
    }
}
