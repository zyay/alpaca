package com.alpaca.app.audio

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Audio abstraction so the Gemini Live pipeline never touches platform classes
 * directly. Session 1 uses AudioRecord/AudioTrack; a later Oboe engine can be
 * swapped in without touching callers.
 */
interface AudioEngine {
    /** 0..1 mic level of the most recent chunk; used for barge-in detection. */
    val inputRms: StateFlow<Float>

    /** 0..1 level of the most recently queued playback chunk; drives waveform UI. */
    val playbackLevel: StateFlow<Float>

    /** Cold flow of PCM16 mono 16 kHz chunks (~200 ms each). */
    fun startRecording(): Flow<ByteArray>

    fun startPlayback(sampleRate: Int = 24_000)
    fun queuePlayback(chunk: ByteArray)

    /** Barge-in: drop everything queued and stop current audio immediately. */
    fun flushPlayback()

    fun stopPlayback()
    fun stopRecording()
    fun release()
}
