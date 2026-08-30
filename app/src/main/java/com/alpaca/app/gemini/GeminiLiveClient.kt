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
 * Prototype client for the Gemini Multimodal Live API over a direct WebSocket.
 * The API key is passed per-connect and never persisted. Production should swap
 * this for a server-issued ephemeral token instead of `?key=` auth.
 *
 * Message shapes verified against https://ai.google.dev/gemini-api/docs/live-api
 * at build time; the Live API was in preview and schemas may drift.
 */
class GeminiLiveClient(private val audioEngine: AudioEngine) {

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

    @Volatile
    var muted: Boolean = false

    private var webSocket: WebSocket? = null
    private var scope: CoroutineScope? = null
    private var micJob: Job? = null

    fun connect(apiKey: String, modelId: String, systemPrompt: String, scope: CoroutineScope) {
        disconnect()
        this.scope = scope
        _state.value = VoiceSessionState.Connecting

        val url = LIVE_ENDPOINT + "?key=$apiKey"
        val request = Request.Builder().url(url).build()
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
                                    voiceConfig = VoiceConfig(PrebuiltVoiceConfig(VOICE))
                                )
                            ),
                            systemInstruction = SystemInstruction(listOf(TextPart(systemPrompt)))
                        )
                    )
                    webSocket.send(json.encodeToString(setup))
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    handleServerMessage(text)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.e(TAG, "Live socket failure code=${response?.code}", t)
                    _state.value = VoiceSessionState.Error(
                        when (response?.code) {
                            400, 401, 403 -> "Google rejected the API key. Rotate it and update local.properties."
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
            audioEngine.startPlayback()
            startMicStreaming()
            _state.value = VoiceSessionState.Listening
            return
        }

        message.serverContent?.let { content ->
            if (content.interrupted == true) {
                audioEngine.flushPlayback()
            }
            content.modelTurn?.parts?.forEach { part ->
                val inline = part.inlineData ?: return@forEach
                val pcm = Base64.decode(inline.data, Base64.DEFAULT)
                audioEngine.queuePlayback(pcm)
                _state.value = VoiceSessionState.Speaking
            }
            if (content.turnComplete == true) {
                _state.value = VoiceSessionState.Listening
            }
        }
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

    /** Barge-in: stop Paco's audio instantly and tell the model the user is talking. */
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
    }

    private fun stopMicAndAudio() {
        audioEngine.stopPlayback()
    }

    companion object {
        private const val TAG = "GeminiLive"
        private const val LIVE_ENDPOINT =
            "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent"
        private const val VOICE = "Kore"
    }
}
