package com.alpaca.app.ui.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alpaca.app.BuildConfig
import com.alpaca.app.data.content.CourseLanguage
import com.alpaca.app.di.AppContainer
import com.alpaca.app.gemini.GeminiLiveClient
import com.alpaca.app.gemini.VoiceSessionState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class VoiceCallViewModel(private val container: AppContainer) : ViewModel() {

    data class Scenario(
        val id: String,
        val title: String,
        val emoji: String,
        val blurb: String,
        val tail: String
    )

    // Language-neutral roleplay setups; the persona prefix localizes them.
    val scenarios = listOf(
        Scenario(
            id = "coffee",
            title = "Ordering coffee",
            emoji = "☕",
            blurb = "A busy café. Order something and ask for the check.",
            tail = "Scenario: the user walks into your café. You are the barista. " +
                "Take their order, offer something to eat, and tell them the price."
        ),
        Scenario(
            id = "ticket",
            title = "Buying a train ticket",
            emoji = "🚆",
            blurb = "At the station counter, buying a ticket to another city.",
            tail = "Scenario: the user is at the ticket counter of the main station. " +
                "You sell train tickets. Ask where they want to go, when, and offer a departure time."
        ),
        Scenario(
            id = "directions",
            title = "Asking for directions",
            emoji = "🗺️",
            blurb = "You're lost downtown and need the metro.",
            tail = "Scenario: the user is a lost tourist downtown. You are a friendly local. " +
                "Give simple directions to the nearest metro station (left, right, straight, near, far)."
        ),
        Scenario(
            id = "friend",
            title = "Meeting a new friend",
            emoji = "👋",
            blurb = "Small talk at a language exchange meetup.",
            tail = "Scenario: you and the user just met at a language exchange meetup. " +
                "Introduce yourself and ask about their name, where they are from, and what they like."
        ),
        Scenario(
            id = "market",
            title = "Shopping at the market",
            emoji = "🍎",
            blurb = "Buy fruit and vegetables, ask prices, count money.",
            tail = "Scenario: you run a fruit stall at the local market. " +
                "The user buys fruit. Offer items, say prices per kilo, weigh produce, and chat a little."
        ),
        Scenario(
            id = "hotel",
            title = "Hotel check-in",
            emoji = "🏨",
            blurb = "You have a booking and questions about breakfast.",
            tail = "Scenario: you are a hotel receptionist. " +
                "Check the user in, confirm the booking name, mention breakfast times and the wifi password."
        ),
        Scenario(
            id = "doctor",
            title = "At the pharmacy",
            emoji = "💊",
            blurb = "You feel bad and need medicine. Describe symptoms.",
            tail = "Scenario: you are a pharmacist. The user feels unwell. " +
                "Ask what hurts, recommend simple over-the-counter help and dosing."
        ),
        Scenario(
            id = "reservation",
            title = "Booking a table by phone",
            emoji = "📞",
            blurb = "Call a restaurant for tonight: how many people, at what time.",
            tail = "Scenario: you answer the phone at a restaurant. " +
                "Take the user's reservation: how many people, what time, and a name."
        )
    )

    private val client get() = container.geminiClient

    val sessionState: StateFlow<VoiceSessionState> = client.state
    val playbackLevel: StateFlow<Float> = container.audioEngine.playbackLevel

    private val _noCredentials = MutableStateFlow(false)
    val noCredentials: StateFlow<Boolean> = _noCredentials

    private var bargeInJob: Job? = null

    fun start(scenario: Scenario) {
        viewModelScope.launch {
            val language = CourseLanguage.byId(container.prefs.prefs.first().currentLanguage)
            val creds = resolveCredentials()
            if (creds == null) {
                _noCredentials.value = true
                client.disconnect()
                return@launch
            }
            _noCredentials.value = false
            container.prefs.incrementCalls()
            client.connect(
                wsUrl = creds.first,
                modelId = creds.second,
                systemPrompt = persona(language) + scenario.tail,
                scope = viewModelScope
            )
            startBargeInMonitor()
        }
    }

    /**
     * Preferred: short-lived token from the Vercel backend (raw key never ships).
     * Fallback: local raw key from local.properties, development only.
     */
    private suspend fun resolveCredentials(): Pair<String, String>? {
        val baseUrl = BuildConfig.VERCEL_BASE_URL
        if (baseUrl.isNotBlank()) {
            container.tokenClient.fetch(baseUrl).getOrNull()?.let { c ->
                return c.wsUrl to c.modelId
            }
            // Token backend unreachable: fall through only if a local key exists.
        }
        val key = BuildConfig.GEMINI_API_KEY
        return if (key.isNotBlank()) {
            GeminiLiveClient.wsUrlWithKey(key) to BuildConfig.GEMINI_MODEL_ID
        } else {
            null
        }
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

        private fun persona(language: CourseLanguage): String =
            "You are a friendly native ${language.displayName} tutor inside Alpaca, a " +
                "${language.displayName}-learning app. The learner is an English speaker at " +
                "A1-A2 level. Speak ONLY in simple ${language.displayName}, at most two short " +
                "sentences per turn. If they make a mistake, gently repeat the correct form, " +
                "then continue the conversation. Stay in character at all times. "
    }
}
