package com.alpaca.app.gemini

import kotlinx.serialization.Serializable

// ---- Client -> server -------------------------------------------------------

@Serializable
data class SetupRequest(val setup: Setup)

@Serializable
data class Setup(
    val model: String,
    val generationConfig: GenerationConfig,
    val systemInstruction: SystemInstruction? = null,
    val outputAudioTranscription: AudioTranscriptionConfig? = null,
    val inputAudioTranscription: AudioTranscriptionConfig? = null
)

// Serializes to {} — the Live API takes an empty config object.
@Serializable
data class AudioTranscriptionConfig(val unused: String? = null)

@Serializable
data class GenerationConfig(
    val responseModalities: List<String>,
    val speechConfig: SpeechConfig? = null
)

@Serializable
data class SpeechConfig(
    val voiceConfig: VoiceConfig? = null
)

@Serializable
data class VoiceConfig(val prebuiltVoiceConfig: PrebuiltVoiceConfig)

@Serializable
data class PrebuiltVoiceConfig(val voiceName: String)

@Serializable
data class SystemInstruction(val parts: List<TextPart>)

@Serializable
data class TextPart(val text: String)

@Serializable
data class RealtimeInputMessage(val realtimeInput: RealtimeInput)

@Serializable
data class RealtimeInput(
    val mediaChunks: List<MediaChunk>? = null,
    val activityStart: ActivitySignal? = null,
    val activityEnd: ActivitySignal? = null
)

@Serializable
data class MediaChunk(val mimeType: String, val data: String)

// Serializes to {} because default-valued fields are not encoded.
@Serializable
data class ActivitySignal(val unused: String? = null)

// ---- Server -> client -------------------------------------------------------

@Serializable
data class ServerMessage(
    val setupComplete: SetupComplete? = null,
    val serverContent: ServerContent? = null,
    val goAway: GoAway? = null,
    val error: ErrorPayload? = null
)

@Serializable
data class SetupComplete(val unused: String? = null)

@Serializable
data class ServerContent(
    val modelTurn: ModelTurn? = null,
    val turnComplete: Boolean? = null,
    val interrupted: Boolean? = null,
    val outputTranscription: TextSegment? = null,
    val inputTranscription: TextSegment? = null
)

@Serializable
data class TextSegment(val text: String? = null)

@Serializable
data class ModelTurn(val parts: List<Part> = emptyList())

@Serializable
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@Serializable
data class InlineData(val mimeType: String = "", val data: String = "")

@Serializable
data class GoAway(val timeLeft: String? = null)

@Serializable
data class ErrorPayload(
    val code: Int? = null,
    val message: String? = null,
    val status: String? = null
)
