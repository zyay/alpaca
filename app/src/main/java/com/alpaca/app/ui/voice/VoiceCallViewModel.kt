package com.alpaca.app.ui.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alpaca.app.BuildConfig
import com.alpaca.app.di.AppContainer
import com.alpaca.app.gemini.VoiceSessionState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class VoiceCallViewModel(private val container: AppContainer) : ViewModel() {

    data class Scenario(
        val id: String,
        val title: String,
        val emoji: String,
        val blurb: String,
        val prompt: String
    )

    val scenarios = listOf(
        Scenario(
            id = "coffee",
            title = "Ordering coffee in Madrid",
            emoji = "☕",
            blurb = "A busy café on Gran Vía. Order something and ask for the check.",
            prompt = SCENARIO_PREFIX +
                "Scenario: the user walks into your café in Madrid. You are the barista. " +
                "Take their order, offer something to eat, and tell them the price in euros."
        ),
        Scenario(
            id = "ticket",
            title = "Buying a train ticket",
            emoji = "🚆",
            blurb = "At the Atocha station counter, buying a ticket to Barcelona.",
            prompt = SCENARIO_PREFIX +
                "Scenario: the user is at the ticket counter in Madrid's Atocha station. " +
                "You sell train tickets. Ask where they want to go, when, and offer a departure time."
        ),
        Scenario(
            id = "directions",
            title = "Asking for directions",
            emoji = "🗺️",
            blurb = "You're lost near the Plaza Mayor and need the metro.",
            prompt = SCENARIO_PREFIX +
                "Scenario: the user is a lost tourist near Plaza Mayor. You are a friendly local. " +
                "Give simple directions to the nearest metro station using gira, sigue recto, cerca, lejos."
        ),
        Scenario(
            id = "friend",
            title = "Meeting a new friend",
            emoji = "👋",
            blurb = "Small talk at a language exchange meetup.",
            prompt = SCENARIO_PREFIX +
                "Scenario: you and the user just met at a language exchange in Madrid. " +
                "Introduce yourself and ask about their name, where they are from, and what they like."
        )
    )

    private val client get() = container.geminiClient

    val sessionState: StateFlow<VoiceSessionState> = client.state
    val playbackLevel: StateFlow<Float> = container.audioEngine.playbackLevel

    private val _noKey = MutableStateFlow(false)
    val noKey: StateFlow<Boolean> = _noKey

    private var bargeInJob: Job? = null

    fun start(scenario: Scenario) {
        if (BuildConfig.GEMINI_API_KEY.isBlank()) {
            _noKey.value = true
            return
        }
        _noKey.value = false
        client.connect(
            apiKey = BuildConfig.GEMINI_API_KEY,
            modelId = BuildConfig.GEMINI_MODEL_ID,
            systemPrompt = scenario.prompt,
            scope = viewModelScope
        )
        startBargeInMonitor()
    }

    private fun startBargeInMonitor() {
        bargeInJob?.cancel()
        bargeInJob = viewModelScope.launch {
            container.audioEngine.inputRms.collect { rms ->
                if (rms > BARGE_IN_RMS && client.state.value is VoiceSessionState.Speaking) {
                    client.bargeIn()
                }
            }
        }
    }

    fun toggleMute(): Boolean {
        client.muted = !client.muted
        return client.muted
    }

    fun end() {
        client.disconnect()
    }

    override fun onCleared() {
        client.disconnect()
        super.onCleared()
    }

    companion object {
        private const val BARGE_IN_RMS = 0.08f
        private const val SCENARIO_PREFIX =
            "You are Paco, a friendly language tutor inside Alpaca, a Spanish-learning app. " +
                "The learner is an English speaker at A1-A2 level. Speak ONLY in simple Spanish, " +
                "at most two short sentences per turn. If they make a mistake, gently repeat the " +
                "correct form, then continue the conversation. Stay in character at all times. "
    }
}
