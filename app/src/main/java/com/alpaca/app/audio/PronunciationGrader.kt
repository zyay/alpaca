package com.alpaca.app.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.text.Normalizer
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Stand-in pronunciation grader using the platform SpeechRecognizer.
 * The interface is the future seam for a LiteRT model trained on common_voice
 * (NNAPI is deprecated; that model should use the GPU delegate).
 */
class PronunciationGrader(private val context: Context) {

    data class WordGrade(val word: String, val score: Float, val ok: Boolean)

    data class Grade(
        val recognized: String,
        val score: Float,
        val words: List<WordGrade> = emptyList()
    )

    suspend fun listenAndGrade(expected: String, languageTag: String): Grade? {
        val recognized = listen(languageTag) ?: return null
        return Grade(
            recognized = recognized,
            score = detailedScore(expected, recognized),
            words = gradeWords(expected, recognized)
        )
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
        private const val WORD_MATCH_THRESHOLD = 0.75f

        fun similarity(expected: String, recognized: String): Float {
            val a = normalize(expected)
            val b = normalize(recognized)
            if (a.isEmpty() && b.isEmpty()) return 1f
            val distance = levenshtein(a, b)
            return (1f - distance.toFloat() / maxOf(a.length, b.length, 1)).coerceIn(0f, 1f)
        }

        /** Blend of whole-phrase and word-alignment similarity; words drive the feedback UI. */
        fun detailedScore(expected: String, recognized: String): Float {
            val charScore = similarity(expected, recognized)
            val words = gradeWords(expected, recognized)
            if (words.isEmpty()) return charScore
            val wordScore = words.map { it.score }.average().toFloat()
            return (0.5f * charScore + 0.5f * wordScore).coerceIn(0f, 1f)
        }

        /**
         * Aligns expected and recognized words with an edit-distance DP whose
         * substitution cost is 1 - word similarity, so a near-miss ("perro" vs
         * "pero") still pairs with its intended word instead of cascading.
         */
        fun gradeWords(expected: String, recognized: String): List<WordGrade> {
            val e = tokenize(expected)
            val r = tokenize(recognized)
            if (e.isEmpty()) return emptyList()
            if (r.isEmpty()) return e.map { WordGrade(it, 0f, false) }

            val m = e.size
            val n = r.size
            val dp = Array(m + 1) { FloatArray(n + 1) }
            for (i in 1..m) dp[i][0] = dp[i - 1][0] + 1f
            for (j in 1..n) dp[0][j] = dp[0][j - 1] + 1f
            for (i in 1..m) {
                for (j in 1..n) {
                    val sub = dp[i - 1][j - 1] + (1f - wordSimilarity(e[i - 1], r[j - 1]))
                    dp[i][j] = minOf(sub, dp[i - 1][j] + 1f, dp[i][j - 1] + 1f)
                }
            }

            val grades = ArrayList<WordGrade>(m)
            var i = m
            var j = n
            while (i > 0 || j > 0) {
                when {
                    i > 0 && j > 0 &&
                        dp[i][j] == dp[i - 1][j - 1] + (1f - wordSimilarity(e[i - 1], r[j - 1])) -> {
                        val score = wordSimilarity(e[i - 1], r[j - 1])
                        grades += WordGrade(e[i - 1], score, score >= WORD_MATCH_THRESHOLD)
                        i--; j--
                    }
                    i > 0 && dp[i][j] == dp[i - 1][j] + 1f -> {
                        grades += WordGrade(e[i - 1], 0f, false)
                        i--
                    }
                    j > 0 -> j--
                    else -> break
                }
            }
            return grades.reversed()
        }

        private fun tokenize(s: String): List<String> =
            normalize(s).split(' ').filter { it.isNotEmpty() }

        private fun wordSimilarity(a: String, b: String): Float {
            val distance = levenshtein(a, b)
            return (1f - distance.toFloat() / maxOf(a.length, b.length, 1)).coerceIn(0f, 1f)
        }

        /**
         * Lowercase, strip punctuation and fold diacritics so "café" matches
         * "cafe" and a recognizer that drops accents is not punished. ß folds
         * to ss; Cyrillic е/ё merge (Russian text normally omits ё anyway).
         */
        private fun normalize(s: String): String {
            val folded = Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD)
                .replace(Regex("\\p{Mn}+"), "")
                .replace("ß", "ss")
                .replace("ё", "е")
            return folded
                .replace(Regex("[\\p{Punct}¿¡«»„“”]"), "")
                .replace(Regex("\\s+"), " ")
                .trim()
        }

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
