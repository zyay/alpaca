package com.alpaca.app.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.math.sqrt

class PlatformAudioEngine : AudioEngine {

    private val _inputRms = MutableStateFlow(0f)
    override val inputRms: StateFlow<Float> = _inputRms

    private val _playbackLevel = MutableStateFlow(0f)
    override val playbackLevel: StateFlow<Float> = _playbackLevel

    @Volatile
    private var recordingActive = false

    private val playbackQueue = LinkedBlockingQueue<ByteArray>()

    @Volatile
    private var playbackActive = false
    private var playbackThread: Thread? = null
    private var track: AudioTrack? = null

    override fun startRecording(): Flow<ByteArray> = callbackFlow {
        val minBuffer = AudioRecord.getMinBufferSize(
            RECORD_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            RECORD_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBuffer * 2, CHUNK_BYTES)
        )
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            Log.e(TAG, "AudioRecord failed to initialize (missing mic or permission)")
            close(IllegalStateException("AudioRecord uninitialized"))
            return@callbackFlow
        }
        recordingActive = true
        recorder.startRecording()
        val reader = Thread {
            val buffer = ByteArray(CHUNK_BYTES)
            while (recordingActive) {
                val read = recorder.read(buffer, 0, buffer.size)
                if (read > 0) {
                    _inputRms.value = rms(buffer, read)
                    trySend(buffer.copyOf(read))
                } else if (read < 0) {
                    break
                }
            }
        }
        reader.start()
        awaitClose {
            recordingActive = false
            try {
                recorder.stop()
            } catch (_: IllegalStateException) {
            }
            recorder.release()
        }
    }

    override fun startPlayback(sampleRate: Int) {
        if (playbackActive) return
        val minBuffer = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(maxOf(minBuffer, sampleRate * 2 / 5))
            .build()
        track = audioTrack
        audioTrack.play()
        playbackActive = true
        playbackThread = Thread {
            try {
                while (playbackActive) {
                    val chunk = playbackQueue.poll(250, TimeUnit.MILLISECONDS) ?: continue
                    audioTrack.write(chunk, 0, chunk.size)
                }
            } catch (_: InterruptedException) {
                // shutdown
            }
        }.also { it.start() }
    }

    override fun queuePlayback(chunk: ByteArray) {
        if (!playbackActive) return
        _playbackLevel.value = rms(chunk, chunk.size)
        playbackQueue.offer(chunk)
    }

    override fun flushPlayback() {
        playbackQueue.clear()
        val t = track ?: return
        try {
            t.pause()
            t.flush()
            if (playbackActive) t.play()
        } catch (e: IllegalStateException) {
            Log.w(TAG, "flushPlayback on dead track", e)
        }
        _playbackLevel.value = 0f
    }

    override fun stopPlayback() {
        playbackActive = false
        playbackThread?.interrupt()
        playbackThread = null
        playbackQueue.clear()
        val t = track
        track = null
        if (t != null) {
            try {
                t.stop()
            } catch (_: IllegalStateException) {
            }
            t.release()
        }
        _playbackLevel.value = 0f
    }

    override fun stopRecording() {
        recordingActive = false
    }

    override fun release() {
        stopPlayback()
        stopRecording()
    }

    private fun rms(buffer: ByteArray, length: Int): Float {
        var sum = 0.0
        var i = 0
        while (i + 1 < length) {
            val sample = ((buffer[i + 1].toInt() shl 8) or (buffer[i].toInt() and 0xFF)).toShort()
            sum += sample.toDouble() * sample.toDouble()
            i += 2
        }
        val samples = (length / 2).coerceAtLeast(1)
        return (sqrt(sum / samples) / Short.MAX_VALUE).toFloat().coerceIn(0f, 1f)
    }

    companion object {
        private const val TAG = "AudioEngine"
        const val RECORD_RATE = 16_000
        const val CHUNK_BYTES = 6_400 // 200 ms of 16-bit mono at 16 kHz
    }
}
