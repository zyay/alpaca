package com.alpaca.app.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Stand-in pronunciation grader using the platform SpeechRecognizer.
 * The interface is the future seam for a LiteRT model trained on common_voice
 * (NNAPI is deprecated; that model should use the GPU delegate).
 */
class PronunciationGrader(private val context: Context) {

    data class Grade(val recognized: String, val score: Float)

    suspend fun listenAndGrade(expected: String, languageTag: String): Grade? {
        val recognized = listen(languageTag) ?: return null
        return Grade(recognized, similarity(expected, recognized))
    }

    private suspend fun listen(languageTag: String): String? = suspendCancellableCoroutine { cont ->
        val resumed = AtomicBoolean(false)
        val main = Handler(Looper.getMainLooper())
        var recognizer: SpeechRecognizer? = null

        fun finish(value: String?) {
            if (resumed.compareAndSet(false, true)) {
                main.post { recognizer?.destroy() }
                cont.resume(value)
            }
        }

        main.post {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                finish(null)
                return@post
            }
            val sr = SpeechRecognizer.createSpeechRecognizer(context)
            recognizer = sr
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            sr.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) = Unit
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit
                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit

                override fun onError(error: Int) = finish(null)

                override fun onResults(results: Bundle) {
                    val list = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    finish(list?.firstOrNull())
                }
            })
            sr.startListening(intent)
            main.postDelayed({ sr.stopListening() }, 7_000)
        }

        cont.invokeOnCancellation { main.post { recognizer?.destroy() } }
    }

    companion object {
        fun similarity(expected: String, recognized: String): Float {
            val a = normalize(expected)
            val b = normalize(recognized)
            if (a.isEmpty() && b.isEmpty()) return 1f
            val distance = levenshtein(a, b)
            return (1f - distance.toFloat() / maxOf(a.length, b.length, 1)).coerceIn(0f, 1f)
        }

        private fun normalize(s: String): String =
            s.lowercase().replace(Regex("[¿?¡!.,;:]"), "").replace(Regex("\\s+"), " ").trim()

        private fun levenshtein(a: String, b: String): Int {
            val dp = IntArray(b.length + 1) { it }
            for (i in 1..a.length) {
                var prev = dp[0]
                dp[0] = i
                for (j in 1..b.length) {
                    val tmp = dp[j]
                    dp[j] = minOf(
                        dp[j] + 1,
                        dp[j - 1] + 1,
                        prev + if (a[i - 1] == b[j - 1]) 0 else 1
                    )
                    prev = tmp
                }
            }
            return dp[b.length]
        }
    }
}
