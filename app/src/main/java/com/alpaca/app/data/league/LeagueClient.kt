package com.alpaca.app.data.league

import java.util.concurrent.TimeUnit
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Talks to the weekly-league endpoints on the Alpaca backend
 * (`GET/POST <VERCEL_BASE_URL>/api/league`). Anonymous: identified only by a
 * random device id stored in DataStore.
 */
class LeagueClient {
    @Serializable
    data class LeagueEntry(val id: String, val name: String, val xp: Int)

    @Serializable
    data class LeagueStandings(
        val available: Boolean = false,
        val week: String = "",
        val resetsInMs: Long = 0,
        val entries: List<LeagueEntry> = emptyList(),
        val yourRank: Int? = null,
        val yourXp: Int? = null,
        val reason: String? = null
    )

    @Serializable
    private data class ReportRequest(val deviceId: String, val name: String, val xp: Int)

    @Serializable
    private data class ReportAck(val available: Boolean = false, val reason: String? = null)

    private val client = OkHttpClient.Builder()
        .callTimeout(10, TimeUnit.SECONDS)
        .build()
    private val json = Json { ignoreUnknownKeys = true }
    private val jsonType = "application/json".toMediaType()

    suspend fun reportXp(
        baseUrl: String,
        deviceId: String,
        name: String,
        xpGained: Int
    ): Result<Unit> = kotlin.runCatching {
        val request = Request.Builder()
            .url(baseUrl.trimEnd('/') + "/api/league")
            .post(json.encodeToString(ReportRequest(deviceId, name, xpGained)).toRequestBody(jsonType))
            .build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) error("League backend returned ${response.code}")
            val ack = json.decodeFromString<ReportAck>(body)
            if (!ack.available) error("League unavailable: ${ack.reason ?: "unknown"}")
        }
    }

    suspend fun standings(baseUrl: String, deviceId: String): Result<LeagueStandings> =
        kotlin.runCatching {
            val request = Request.Builder()
                .url(baseUrl.trimEnd('/') + "/api/league?deviceId=" + deviceId)
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) error("League backend returned ${response.code}")
                json.decodeFromString<LeagueStandings>(body)
            }
        }
}
