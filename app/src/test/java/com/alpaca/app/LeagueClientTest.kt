package com.alpaca.app

import com.alpaca.app.data.league.LeagueClient
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Validates the league DTOs parse exactly what server/api/league.js emits. */
class LeagueClientTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun standingsParse() {
        val payload = """
            {
              "available": true,
              "week": "2026-W36",
              "resetsInMs": 172800000,
              "entries": [
                {"id": "a", "name": "Lucía", "xp": 812},
                {"id": "b", "name": "Mateo", "xp": 704}
              ],
              "yourRank": 3,
              "yourXp": 233,
              "extraField": true
            }
        """.trimIndent()
        val s = json.decodeFromString<LeagueClient.LeagueStandings>(payload)
        assertTrue(s.available)
        assertEquals("2026-W36", s.week)
        assertEquals(172800000L, s.resetsInMs)
        assertEquals(2, s.entries.size)
        assertEquals("Lucía", s.entries[0].name)
        assertEquals(812, s.entries[0].xp)
        assertEquals(3, s.yourRank)
        assertEquals(233, s.yourXp)
    }

    @Test
    fun notConfiguredResponseParses() {
        val payload = """{"available": false, "reason": "not_configured"}"""
        val s = json.decodeFromString<LeagueClient.LeagueStandings>(payload)
        assertFalse(s.available)
        assertEquals("not_configured", s.reason)
        assertTrue(s.entries.isEmpty())
        assertNull(s.yourRank)
    }

    @Test
    fun defaultsTolerateSparsePayload() {
        val s = json.decodeFromString<LeagueClient.LeagueStandings>("{}")
        assertFalse(s.available)
        assertEquals("", s.week)
        assertEquals(0, s.resetsInMs)
        assertTrue(s.entries.isEmpty())
    }
}
