package com.alpaca.app.gemini

import android.util.Base64
import android.util.Log
import com.alpaca.app.audio.AudioEngine
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

/**
 * Client for the Gemini Multimodal Live API over a direct WebSocket, authenticated
 * by a server-issued ephemeral token (or a raw dev key via [wsUrlWithKey]).
 *
 * Message shapes verified against https://ai.google.dev/gemini-api/docs/live-api
 * at build time; the Live API was in preview and schemas may drift.
 */
class GeminiLiveClient(private val audioEngine: AudioEngine) {

    data class TranscriptEntry(val isTutor: Boolean, val text: String)

    private val httpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(25, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    private val _state = MutableStateFlow<VoiceSessionState>(VoiceSessionState.Idle)
    val state: StateFlow<VoiceSessionState> = _state

    /** Rolling conversation transcript for live captions and the post-call coach. */
    private val _transcript = MutableStateFlow<List<TranscriptEntry>>(emptyList())
    val transcript: StateFlow<List<TranscriptEntry>> = _transcript

    /** True once the server signals goAway (load-balanced disconnect imminent). */
    private val _sessionEnding = MutableStateFlow(false)
    val sessionEnding: StateFlow<Boolean> = _sessionEnding

    @Volatile
    var muted: Boolean = false

    private var webSocket: WebSocket? = null
    private var scope: CoroutineScope? = null
    private var micJob: Job? = null
    private var lastConnectParams: Array<String>? = null
    private var reconnectAttempted = false
    private var tutorTurnHasTranscription = false
    private var turnSealed = false

    fun connect(
        wsUrl: String,
        modelId: String,
        systemPrompt: String,
        voiceName: String,
        scope: CoroutineScope
    ) {
        disconnect()
        lastConnectParams = arrayOf(wsUrl, modelId, systemPrompt, voiceName)
        reconnectAttempted = false
        this.scope = scope
        openSocket(wsUrl, modelId, systemPrompt, voiceName)
    }

    private fun openSocket(wsUrl: String, modelId: String, systemPrompt: String, voiceName: String) {
        _state.value = VoiceSessionState.Connecting
        _sessionEnding.value = false

        val request = Request.Builder().url(wsUrl).build()
        webSocket = httpClient.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    val setup = SetupRequest(
                        Setup(
                            model = "models/$modelId",
                            generationConfig = GenerationConfig(
                                responseModalities = listOf("AUDIO"),
                                speechConfig = SpeechConfig(
                                    voiceConfig = VoiceConfig(PrebuiltVoiceConfig(voiceName))
                                )
                            ),
                            systemInstruction = SystemInstruction(listOf(TextPart(systemPrompt))),
                            outputAudioTranscription = AudioTranscriptionConfig(),
                            inputAudioTranscription = AudioTranscriptionConfig()
                        )
                    )
                    webSocket.send(json.encodeToString(setup))
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    handleServerMessage(text)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.e(TAG, "Live socket failure code=${response?.code}", t)
                    // Mid-call network drop: one silent reconnect before giving up.
                    val params = lastConnectParams
                    if (response == null && params != null && !reconnectAttempted &&
                        _state.value.let { it is VoiceSessionState.Listening || it is VoiceSessionState.Speaking }
                    ) {
                        reconnectAttempted = true
                        Log.w(TAG, "Abnormal close — reconnecting once")
                        openSocket(params[0], params[1], params[2], params[3])
                        return
                    }
                    _state.value = VoiceSessionState.Error(
                        when (response?.code) {
                            400, 401, 403 -> "Google rejected the session. Check the token backend / GEMINI key."
                            404 -> "Model \"$modelId\" not found. Update GEMINI_MODEL_ID (see ai.google.dev Live API docs)."
                            null -> "Connection lost: ${t.message ?: "network error"}"
                            else -> "Connection failed (${response.code}): ${t.message ?: ""}"
                        }
                    )
                    stopMicAndAudio()
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    _state.value = VoiceSessionState.Idle
                    stopMicAndAudio()
                }
            }
        )
    }

    private fun handleServerMessage(text: String) {
        val message = try {
            json.decodeFromString<ServerMessage>(text)
        } catch (e: Exception) {
            Log.w(TAG, "Unparsed Live message", e)
            return
        }

        message.error?.let { err ->
            Log.e(TAG, "Live API error: $err")
            _state.value = VoiceSessionState.Error(
                when {
                    err.message?.contains("API key", ignoreCase = true) == true ->
                        "Invalid API key. Rotate it and update local.properties."
                    err.message?.contains("not found", ignoreCase = true) == true ||
                        err.message?.contains("not supported", ignoreCase = true) == true ->
                        "Model not supported. Update GEMINI_MODEL_ID (see ai.google.dev Live API docs)."
                    else -> err.message ?: "Gemini Live error"
                }
            )
            stopMicAndAudio()
            return
        }

        if (message.setupComplete != null) {
            reconnectAttempted = false
            audioEngine.startPlayback()
            startMicStreaming()
            _state.value = VoiceSessionState.Listening
            return
        }

        message.goAway?.let {
            Log.w(TAG, "goAway received, timeLeft=${it.timeLeft}")
            _sessionEnding.value = true
        }

        message.serverContent?.let { content ->
            if (content.interrupted == true) {
                audioEngine.flushPlayback()
            }
            content.outputTranscription?.text?.let { segment ->
                tutorTurnHasTranscription = true
                appendTranscript(isTutor = true, segment)
            }
            content.inputTranscription?.text?.let { segment ->
                appendTranscript(isTutor = false, segment)
            }
            content.modelTurn?.parts?.forEach { part ->
                val inline = part.inlineData
                if (inline != null) {
                    val pcm = Base64.decode(inline.data, Base64.DEFAULT)
                    audioEngine.queuePlayback(pcm)
                    _state.value = VoiceSessionState.Speaking
                } else if (part.text != null && !tutorTurnHasTranscription) {
                    // Fallback when the server skips transcription segments.
                    appendTranscript(isTutor = true, part.text ?: "")
                }
            }
            if (content.turnComplete == true) {
                _state.value = VoiceSessionState.Listening
                tutorTurnHasTranscription = false
                sealTranscriptTurn()
            }
        }
    }

    /** Transcription segments stream in pieces — extend the tail entry until the turn completes. */
    private fun appendTranscript(isTutor: Boolean, segment: String) {
        if (segment.isBlank()) return
        val current = _transcript.value
        val tail = current.lastOrNull()
        val updated = if (!turnSealed && tail != null && tail.isTutor == isTutor) {
            current.dropLast(1) + tail.copy(text = (tail.text + segment).trim())
        } else {
            turnSealed = false
            current + TranscriptEntry(isTutor, segment.trim())
        }
        _transcript.value = if (updated.size > MAX_TRANSCRIPT_ENTRIES) {
            updated.takeLast(MAX_TRANSCRIPT_ENTRIES)
        } else {
            updated
        }
    }

    /** Start the next message on a fresh entry so speaker turns never merge. */
    private fun sealTranscriptTurn() {
        turnSealed = true
    }

    private fun startMicStreaming() {
        micJob?.cancel()
        micJob = scope?.launch(Dispatchers.IO) {
            try {
                audioEngine.startRecording().collect { chunk ->
                    if (muted) return@collect
                    val message = RealtimeInputMessage(
                        RealtimeInput(
                            mediaChunks = listOf(
                                MediaChunk(
                                    mimeType = "audio/pcm;rate=16000",
                                    data = Base64.encodeToString(chunk, Base64.NO_WRAP)
                                )
                            )
                        )
                    )
                    webSocket?.send(json.encodeToString(message))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Mic streaming failed (mic unavailable on emulator?)", e)
                _state.value = VoiceSessionState.Error(
                    "Microphone unavailable. On the emulator, enable the virtual mic in AVD settings, or use a physical device."
                )
            }
        }
    }

    /** Barge-in: stop the tutor's audio instantly and tell the model the user is talking. */
    fun bargeIn() {
        audioEngine.flushPlayback()
        webSocket?.send(
            json.encodeToString(
                RealtimeInputMessage(RealtimeInput(activityStart = ActivitySignal()))
            )
        )
        _state.value = VoiceSessionState.Listening
    }

    fun disconnect() {
        micJob?.cancel()
        micJob = null
        stopMicAndAudio()
        webSocket?.close(1000, "session end")
        webSocket = null
        _state.value = VoiceSessionState.Idle
        _sessionEnding.value = false
        _transcript.value = emptyList()
        tutorTurnHasTranscription = false
        turnSealed = false
    }

    private fun stopMicAndAudio() {
        audioEngine.stopPlayback()
    }

    companion object {
        private const val TAG = "GeminiLive"
        private const val MAX_TRANSCRIPT_ENTRIES = 40
        private const val LIVE_ENDPOINT =
            "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent"

        /** Local-dev fallback: direct connection authenticated by a raw API key. */
        fun wsUrlWithKey(apiKey: String): String = "$LIVE_ENDPOINT?key=$apiKey"

        val availableVoices = listOf("Kore", "Puck", "Aoede", "Charon")
        const val DEFAULT_VOICE = "Kore"
    }
}
