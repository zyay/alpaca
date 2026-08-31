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

        for (file in files) {
            val unit = json.decodeFromString<CourseUnit>(file.readText())
            assertTrue("${unit.unitId} must be an es unit id", unit.unitId.startsWith("es_u"))
            assertTrue("${unit.unitId} has no lessons", unit.lessons.isNotEmpty())
            for (lesson in unit.lessons) {
                assertTrue(
                    "${unit.unitId}/${lesson.lessonId} has no exercises",
                    lesson.exercises.isNotEmpty()
                )
            }
        }
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
