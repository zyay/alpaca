package com.alpaca.app.data.coach

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Fetches structured post-call feedback from the Alpaca backend, which forwards
 * the transcript to a Gemini text model. The raw API key never ships in the APK.
 */
class CoachClient {

    @Serializable
    data class CoachTip(val title: String, val tip: String)

    @Serializable
    data class VocabItem(val term: String, val translation: String)

    @Serializable
    data class CoachFeedback(
        val strengths: List<String> = emptyList(),
        val improvements: List<CoachTip> = emptyList(),
        val vocab: List<VocabItem> = emptyList()
    )

    @Serializable
    data class CoachRequest(
        val language: String,
        val level: String,
        val scenario: String,
        val transcript: List<Line>
    ) {
        @Serializable
        data class Line(val role: String, val text: String)
    }

    private val client = OkHttpClient.Builder().build()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetch(baseUrl: String, request: CoachRequest): Result<CoachFeedback> =
        kotlin.runCatching {
            val payload = json.encodeToString(CoachRequest.serializer(), request)
            val httpRequest = Request.Builder()
                .url(baseUrl.trimEnd('/') + "/api/coach")
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(httpRequest).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    error("Coach backend returned ${response.code}: ${body.take(200)}")
                }
                json.decodeFromString<CoachFeedback>(body)
            }
        }
}
