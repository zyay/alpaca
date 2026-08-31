package com.alpaca.app.ui.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alpaca.app.BuildConfig
import com.alpaca.app.data.content.CourseLanguage
import com.alpaca.app.data.coach.CoachClient
import com.alpaca.app.di.AppContainer
import com.alpaca.app.gemini.GeminiLiveClient
import com.alpaca.app.gemini.VoiceSessionState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VoiceCallViewModel(private val container: AppContainer) : ViewModel() {

    data class Scenario(
        val id: String,
        val title: String,
        val emoji: String,
        val blurb: String,
        val tail: String
    )

    enum class Level(val id: String, val label: String, val blurb: String) {
        BEGINNER("beginner", "Beginner", "Very slow, short sentences, hints in English"),
        CONFIDENT("confident", "Confident", "Natural pace, everyday phrases only"),
        ADVANCED("advanced", "Advanced", "Full-speed, idiomatic, mistakes corrected directly");

        companion object {
            fun fromId(id: String): Level =
                entries.firstOrNull { it.id == id } ?: BEGINNER
        }
    }

    sealed interface CoachUiState {
        data object Idle : CoachUiState
        data object Loading : CoachUiState
        data class Ready(val feedback: CoachClient.CoachFeedback) : CoachUiState
        data object Unavailable : CoachUiState
    }

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
    val transcript: StateFlow<List<GeminiLiveClient.TranscriptEntry>> = client.transcript
    val sessionEnding: StateFlow<Boolean> = client.sessionEnding
    val settings: StateFlow<com.alpaca.app.data.datastore.UserPrefs> =
        container.prefs.prefs.stateIn(viewModelScope, SharingStarted.Eagerly, com.alpaca.app.data.datastore.UserPrefs())

    private val _noCredentials = MutableStateFlow(false)
    val noCredentials: StateFlow<Boolean> = _noCredentials

    private val _coachState = MutableStateFlow<CoachUiState>(CoachUiState.Idle)
    val coachState: StateFlow<CoachUiState> = _coachState

    private var bargeInJob: Job? = null
    private var lastCall: Pair<Scenario, CourseLanguage>? = null

    fun start(scenario: Scenario) {
        viewModelScope.launch {
            val prefs = container.prefs.prefs.first()
            val language = CourseLanguage.byId(prefs.currentLanguage)
            val creds = resolveCredentials()
            if (creds == null) {
                _noCredentials.value = true
                client.disconnect()
                return@launch
            }
            _noCredentials.value = false
            _coachState.value = CoachUiState.Idle
            lastCall = scenario to language
            container.prefs.incrementCalls()
            client.connect(
                wsUrl = creds.first,
                modelId = creds.second,
                systemPrompt = persona(language, Level.fromId(prefs.voiceLevel)) + scenario.tail,
                voiceName = prefs.voiceName,
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

    /** Snapshot the transcript before disconnect wipes it, then ask the backend for feedback. */
    fun finishAndCoach() {
        val (scenario, language) = lastCall ?: return
        val entries = client.transcript.value
        client.disconnect()
        bargeInJob?.cancel()
        if (entries.none { !it.isTutor }) {
            _coachState.value = CoachUiState.Unavailable
            return
        }
        _coachState.value = CoachUiState.Loading
        viewModelScope.launch {
            val baseUrl = BuildConfig.VERCEL_BASE_URL
            if (baseUrl.isBlank()) {
                _coachState.value = CoachUiState.Unavailable
                return@launch
            }
            val prefs = container.prefs.prefs.first()
            val request = CoachClient.CoachRequest(
                language = language.displayName,
                level = Level.fromId(prefs.voiceLevel).id,
                scenario = scenario.title,
                transcript = entries.map {
                    CoachClient.CoachRequest.Line(
                        role = if (it.isTutor) "tutor" else "user",
                        text = it.text
                    )
                }
            )
            _coachState.value = container.coachClient.fetch(baseUrl, request)
                .fold(
                    onSuccess = { CoachUiState.Ready(it) },
                    onFailure = { CoachUiState.Unavailable }
                )
        }
    }

    fun resetCoach() {
        _coachState.value = CoachUiState.Idle
    }

    fun setLevel(level: Level) {
        viewModelScope.launch { container.prefs.setVoiceLevel(level.id) }
    }

    fun setVoice(voice: String) {
        viewModelScope.launch { container.prefs.setVoiceName(voice) }
    }

    override fun onCleared() {
        client.disconnect()
        super.onCleared()
    }

    companion object {
        private const val BARGE_IN_RMS = 0.08f

        private fun persona(language: CourseLanguage, level: Level): String {
            val base = "You are a friendly native ${language.displayName} tutor inside Alpaca, a " +
                "${language.displayName}-learning app. Stay in character at all times. "
            return when (level) {
                Level.BEGINNER -> base +
                    "The learner is an English speaker at A1 level. Speak ONLY in very simple " +
                    "${language.displayName}, at most one or two short sentences per turn, slowly " +
                    "and clearly. If they make a mistake, gently repeat the correct form, then " +
                    "continue the conversation. After a difficult word you may add a short English " +
                    "hint in parentheses. "
                Level.CONFIDENT -> base +
                    "The learner is an English speaker at A2-B1 level. Speak ONLY in " +
                    "${language.displayName} at a natural pace, at most two or three short sentences " +
                    "per turn, using everyday phrases. If they make a mistake, model the correct " +
                    "form naturally in your reply. "
                Level.ADVANCED -> base +
                    "The learner is an English speaker at B1-B2 level. Speak ONLY in " +
                    "${language.displayName} at full conversational speed with idiomatic, native " +
                    "phrasing. Keep the conversation flowing across several exchanges. When they " +
                    "make a mistake, correct it explicitly and briefly explain the rule before " +
                    "moving on. "
            }
        }
    }
}
