package com.alpaca.app

import com.alpaca.app.audio.PronunciationGrader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PronunciationGraderTest {

    @Test
    fun perfectMatchScoresFull() {
        assertEquals(1f, PronunciationGrader.similarity("Buenos días", "Buenos días"), 0.001f)
        assertTrue(PronunciationGrader.detailedScore("Buenos días", "Buenos días") > 0.95f)
    }

    @Test
    fun diacriticsAreFolded() {
        assertEquals(1f, PronunciationGrader.similarity("café", "cafe"), 0.001f)
        assertEquals(1f, PronunciationGrader.similarity("Straße", "strasse"), 0.001f)
        assertEquals(1f, PronunciationGrader.similarity("ёж", "еж"), 0.001f)
    }

    @Test
    fun punctuationIsIgnored() {
        assertEquals(1f, PronunciationGrader.similarity("¡Hola!", "hola"), 0.001f)
        assertEquals(1f, PronunciationGrader.similarity("Comment ça va ?", "comment ca va"), 0.001f)
    }

    @Test
    fun wrongWordScoresLow() {
        assertTrue(PronunciationGrader.detailedScore("gato", "perro") < 0.5f)
    }

    @Test
    fun missingWordIsFlaggedRed() {
        val grades = PronunciationGrader.gradeWords("el gato negro", "el gato")
        assertEquals(3, grades.size)
        assertFalse(grades[2].ok)
        assertEquals("negro", grades[2].word)
        assertTrue(grades[0].ok)
    }

    @Test
    fun nearMissWordStillAlignsToItsSlot() {
        val grades = PronunciationGrader.gradeWords("Buenos días", "Buenos dia")
        assertEquals(2, grades.size)
        assertTrue(grades[0].ok)
        // "dia" vs "días" is one letter off — should pair, not cascade.
        assertTrue(grades[1].score > 0.7f)
    }

    @Test
    fun extraRecognizedWordsDoNotBreakAlignment() {
        val grades = PronunciationGrader.gradeWords("gracias", "muchas gracias")
        assertEquals(1, grades.size)
        assertTrue(grades[0].ok)
    }

    @Test
    fun emptyRecognizedFlagsAllWords() {
        val grades = PronunciationGrader.gradeWords("uno dos", "")
        assertEquals(2, grades.size)
        grades.forEach { assertFalse(it.ok) }
    }
}
