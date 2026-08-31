package com.alpaca.app.data.auth

import java.util.concurrent.TimeUnit
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Account signup/login against the Alpaca backend (the /api/auth endpoints on Vercel).
 * Sessions are opaque bearer tokens stored server-side in Redis.
 */
class AuthClient {
    @Serializable
    data class AuthResponse(
        val ok: Boolean = false,
        val token: String = "",
        val userId: String = "",
        val email: String = "",
        val name: String = "",
        val available: Boolean = true,
        val reason: String? = null,
        val error: String? = null
    )

    private val client = OkHttpClient.Builder()
        .callTimeout(10, TimeUnit.SECONDS)
        .build()
    private val json = Json { ignoreUnknownKeys = true }
    private val jsonType = "application/json".toMediaType()

    suspend fun signup(baseUrl: String, email: String, password: String, name: String): Result<AuthResponse> =
        send(
            baseUrl.trimEnd('/') + "/api/auth/signup",
            json.encodeToString(SignupBody(email, password, name))
        )

    suspend fun login(baseUrl: String, email: String, password: String): Result<AuthResponse> =
        send(
            baseUrl.trimEnd('/') + "/api/auth/login",
            json.encodeToString(LoginBody(email, password))
        )

    suspend fun me(baseUrl: String, token: String): Result<AuthResponse> =
        runCatching { execute(url = baseUrl.trimEnd('/') + "/api/auth/me", body = null, token = token) }

    suspend fun logout(baseUrl: String, token: String): Result<Unit> = runCatching {
        execute(url = baseUrl.trimEnd('/') + "/api/auth/logout", body = "{}", token = token)
        Unit
    }

    @Serializable
    private data class SignupBody(val email: String, val password: String, val name: String)

    @Serializable
    private data class LoginBody(val email: String, val password: String)

    private suspend fun send(url: String, body: String): Result<AuthResponse> =
        runCatching { execute(url = url, body = body, token = null) }

    private fun execute(url: String, body: String?, token: String?): AuthResponse {
        val builder = Request.Builder().url(url)
        if (body != null) {
            builder.post(body.toRequestBody(jsonType))
        } else {
            builder.get()
        }
        if (token != null) builder.header("Authorization", "Bearer $token")
        client.newCall(builder.build()).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (response.code == 401) {
                // Expired/missing session on /me and /auth/logout paths.
                return AuthResponse(ok = false, error = "Not signed in")
            }
            if (!response.isSuccessful) {
                val parsed = runCatching { json.decodeFromString<AuthResponse>(text) }.getOrNull()
                error(parsed?.error ?: "Backend returned ${response.code}")
            }
            return json.decodeFromString<AuthResponse>(text)
        }
    }
}
