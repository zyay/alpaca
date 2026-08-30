package com.alpaca.app.gemini

sealed class VoiceSessionState {
    data object Idle : VoiceSessionState()
    data object Connecting : VoiceSessionState()
    data object Listening : VoiceSessionState()
    data object Speaking : VoiceSessionState()
    data class Error(val message: String) : VoiceSessionState()
}
