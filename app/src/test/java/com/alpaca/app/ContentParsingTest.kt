package com.alpaca.app

import com.alpaca.app.data.content.CourseUnit
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Validates every bundled content unit parses with the exact runtime config
 * and catches duplicate lesson ids / empty exercises at build time.
 */
class ContentParsingTest {

    private val json = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "type"
    }

    @Test
    fun allUnitFilesParse() {
        val dir = File("src/main/assets/content")
        val files = dir.listFiles { f -> f.extension == "json" }.orEmpty()
        assertTrue("No content JSON files found in ${dir.absolutePath}", files.isNotEmpty())
        assertTrue("Expected at least 18 units, found ${files.size}", files.size >= 18)

        val languages = mutableSetOf<String>()
        for (file in files) {
            val unit = json.decodeFromString<CourseUnit>(file.readText())
            assertTrue(
                "${unit.unitId} must match <lang>_uN (e.g. es_u1)",
                Regex("^[a-z]{2}_u\\d+$").matches(unit.unitId)
            )
            languages += unit.unitId.substringBefore('_')
            assertTrue("${unit.unitId} has no lessons", unit.lessons.isNotEmpty())
            for (lesson in unit.lessons) {
                assertTrue(
                    "${unit.unitId}/${lesson.lessonId} has no exercises",
                    lesson.exercises.isNotEmpty()
                )
                assertTrue(
                    "${lesson.lessonId} must be prefixed by its unit id ${unit.unitId}",
                    lesson.lessonId.startsWith("${unit.unitId}_")
                )
            }
        }
        assertEquals(
            "Expected 7 course languages (es, fr, de, it, pt, en, ru)",
            setOf("es", "fr", "de", "it", "pt", "en", "ru"),
            languages
        )
    }

    @Test
    fun noDuplicateUnitIdsAcrossLanguages() {
        val dir = File("src/main/assets/content")
        val ids = dir.listFiles { f -> f.extension == "json" }.orEmpty()
            .map { f -> json.decodeFromString<CourseUnit>(f.readText()).unitId }
        assertEquals(ids.size, ids.distinct().size)
    }

    @Test
    fun noDuplicateLessonIdsAcrossUnits() {
        val dir = File("src/main/assets/content")
        val ids = dir.listFiles { f -> f.extension == "json" }.orEmpty()
            .flatMap { f -> json.decodeFromString<CourseUnit>(f.readText()).lessons }
            .map { it.lessonId }
        assertEquals(ids.size, ids.distinct().size)
    }

    @Test
    fun matchPairsAreTwoElementPairs() {
        val dir = File("src/main/assets/content")
        val units = dir.listFiles { f -> f.extension == "json" }.orEmpty()
            .map { json.decodeFromString<CourseUnit>(it.readText()) }
        for (unit in units) {
            for (lesson in unit.lessons) {
                for (exercise in lesson.exercises) {
                    if (exercise is com.alpaca.app.data.content.MatchPairsExercise) {
                        exercise.pairs.forEachIndexed { i, pair ->
                            assertEquals(
                                "${unit.unitId}/${lesson.lessonId} pair $i",
                                2,
                                pair.size
                            )
                        }
                    }
                }
            }
        }
    }
}
