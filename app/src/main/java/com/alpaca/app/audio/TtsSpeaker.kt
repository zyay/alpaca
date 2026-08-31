package com.alpaca.app.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** TTS wrapper for listening exercises; the course language is passed per call. */
class TtsSpeaker(context: Context) {
    private val ready = CompletableDeferred<Boolean>()
    private val engine: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        ready.complete(status == TextToSpeech.SUCCESS)
    }

    init {
        engine.setSpeechRate(0.9f)
    }

    /** Suspends until the utterance finishes playing. */
    suspend fun speak(text: String, languageTag: String = "es-ES", rate: Float = 0.9f) {
        if (!ready.await()) return
        engine.language = Locale.forLanguageTag(languageTag)
        engine.setSpeechRate(rate)
        val utteranceId = UUID.randomUUID().toString()
        val resumed = AtomicBoolean(false)
        suspendCancellableCoroutine { cont ->
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String) = Unit
                override fun onDone(id: String) {
                    if (id == utteranceId && resumed.compareAndSet(false, true)) cont.resume(Unit)
                }

                @Deprecated("Deprecated in Java")
                override fun onError(id: String) {
                    if (id == utteranceId && resumed.compareAndSet(false, true)) cont.resume(Unit)
                }
            })
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            cont.invokeOnCancellation { engine.stop() }
        }
    }

    fun shutdown() {
        engine.stop()
        engine.shutdown()
    }
}
