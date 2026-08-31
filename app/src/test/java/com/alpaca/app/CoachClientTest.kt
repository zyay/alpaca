package com.alpaca.app

import com.alpaca.app.data.coach.CoachClient
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoachClientTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun coachResponseParses() {
        val payload = """
            {
              "strengths": ["Great use of 'quiero un café'."],
              "improvements": [
                {"title": "Article agreement", "tip": "Say 'unos libros', not 'un libros'."}
              ],
              "vocab": [{"term": "la cuenta", "translation": "the check"}],
              "extraField": true
            }
        """.trimIndent()

        val feedback = json.decodeFromString<CoachClient.CoachFeedback>(payload)
        assertEquals(1, feedback.strengths.size)
        assertEquals("Article agreement", feedback.improvements.first().title)
        assertEquals("the check", feedback.vocab.first().translation)
    }

    @Test
    fun sparseResponseDefaultsToEmptyLists() {
        val feedback = json.decodeFromString<CoachClient.CoachFeedback>("{}")
        assertTrue(feedback.strengths.isEmpty())
        assertTrue(feedback.improvements.isEmpty())
        assertTrue(feedback.vocab.isEmpty())
    }
}
