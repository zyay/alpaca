package com.alpaca.app.audio

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator

/**
 * Tiny zero-asset sound effects via the platform ToneGenerator,
 * gated by the user's sound preference.
 */
class SoundPlayer(context: Context) {
    private val tone = runCatching {
        ToneGenerator(AudioManager.STREAM_MUSIC, 70)
    }.getOrNull()

    @Volatile
    var enabled: Boolean = true

    fun correct() = play(ToneGenerator.TONE_PROP_ACK)
    fun wrong() = play(ToneGenerator.TONE_PROP_NACK)
    fun select() = play(ToneGenerator.TONE_PROP_BEEP)
    fun finish() {
        play(ToneGenerator.TONE_CDMA_HIGH_L)
        tone?.let { tg ->
            android.os.Handler(android.os.Looper.getMainLooper())
                .postDelayed({ runCatching { tg.startTone(ToneGenerator.TONE_CDMA_CONFIRM, 150) } }, 140)
        }
    }

    private fun play(code: Int) {
        if (!enabled) return
        runCatching { tone?.startTone(code, 120) }
    }

    fun release() = runCatching { tone?.release() }
}
