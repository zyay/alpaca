package com.alpaca.app.gemini

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Fetches short-lived Gemini Live credentials from the Alpaca backend
 * (Vercel serverless function). The raw API key never ships in the APK.
 */
class TokenClient {
    @Serializable
    data class LiveCredentials(
        val token: String,
        val wsUrl: String,
        val modelId: String
    )

    private val client = OkHttpClient.Builder().build()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetch(baseUrl: String): Result<LiveCredentials> = kotlin.runCatching {
        val request = Request.Builder()
            .url(baseUrl.trimEnd('/') + "/api/token")
            .post("{}".toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                error(
                    when (response.code) {
                        401, 403 -> "Backend rejected the request (${response.code}). Is GEMINI_API_KEY set on Vercel?"
                        502, 500 -> "The token backend failed: ${body.take(200)}"
                        else -> "Token backend returned ${response.code}"
                    }
                )
            }
            json.decodeFromString<LiveCredentials>(body)
        }
    }
}
