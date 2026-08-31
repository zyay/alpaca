package com.alpaca.app

import com.alpaca.app.data.auth.AuthClient
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Validates the auth DTOs parse exactly what the server /api/auth endpoints emit. */
class AuthClientTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun signupSuccessParses() {
        val payload = """
            {
              "ok": true,
              "token": "c3VjY2Vzcw",
              "userId": "5f0b...",
              "email": "ana@example.com",
              "name": "Ana",
              "extraField": true
            }
        """.trimIndent()
        val r = json.decodeFromString<AuthClient.AuthResponse>(payload)
        assertTrue(r.ok)
        assertEquals("c3VjY2Vzcw", r.token)
        assertEquals("5f0b...", r.userId)
        assertEquals("ana@example.com", r.email)
        assertEquals("Ana", r.name)
        assertNull(r.error)
    }

    @Test
    fun errorResponseParses() {
        val payload = """{"ok": false, "error": "Email already registered"}"""
        val r = json.decodeFromString<AuthClient.AuthResponse>(payload)
        assertFalse(r.ok)
        assertEquals("Email already registered", r.error)
        assertEquals("", r.token)
    }

    @Test
    fun notConfiguredResponseParses() {
        val payload = """{"ok": false, "available": false, "reason": "not_configured"}"""
        val r = json.decodeFromString<AuthClient.AuthResponse>(payload)
        assertFalse(r.ok)
        assertFalse(r.available)
        assertEquals("not_configured", r.reason)
        assertNull(r.error)
    }

    @Test
    fun meResponseParses() {
        val payload = """
            {"ok": true, "userId": "abc123", "email": "bo@example.com", "name": "Bo"}
        """.trimIndent()
        val r = json.decodeFromString<AuthClient.AuthResponse>(payload)
        assertTrue(r.ok)
        assertEquals("abc123", r.userId)
        assertEquals("Bo", r.name)
    }

    @Test
    fun defaultsTolerateSparsePayload() {
        val r = json.decodeFromString<AuthClient.AuthResponse>("{}")
        assertFalse(r.ok)
        assertEquals("", r.token)
        assertEquals("", r.userId)
        assertTrue(r.available)
        assertNull(r.reason)
    }
}
