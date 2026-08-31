package com.alpaca.app.data.content

import android.content.Context
import android.util.Log
import com.alpaca.app.data.db.entities.MistakeEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class ContentRepository(private val context: Context) {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        classDiscriminator = "type"
    }

    @Volatile
    private var cache: List<CourseUnit>? = null

    suspend fun loadUnits(): List<CourseUnit> = withContext(Dispatchers.IO) {
        cache?.let { return@withContext it }
        val units = UNIT_FILES.map { (unitId, file) ->
            try {
                val text = context.assets.open(file).bufferedReader().use { it.readText() }
                json.decodeFromString<CourseUnit>(text)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load unit $unitId", e)
                null
            }
        }.filterNotNull()
        Log.i(TAG, "Loaded ${units.size} units, ${units.sumOf { it.lessons.size }} lessons")
        cache = units
        units
    }

    suspend fun loadUnit(unitId: String): CourseUnit? =
        loadUnits().firstOrNull { it.unitId == unitId }

    suspend fun findLesson(lessonId: String): Lesson? =
        loadUnits().asSequence().flatMap { it.lessons }.firstOrNull { it.lessonId == lessonId }

    /**
     * Rebuilds a practice lesson from the learner's logged mistakes.
     * Falls back gracefully when the referenced exercise no longer exists.
     */
    suspend fun buildReviewLesson(mistakes: List<MistakeEntity>): Lesson? {
        val exercises = mistakes.mapNotNull { mistake ->
            findLesson(mistake.lessonId)?.exercises?.getOrNull(mistake.exerciseIndex)
        }
        if (exercises.isEmpty()) return null
        return Lesson(
            lessonId = REVIEW_LESSON_ID,
            title = "Mistake review",
            exercises = exercises.distinctBy { it.toString() }
        )
    }

    companion object {
        private const val TAG = "Alpaca"
        const val REVIEW_LESSON_ID = "review"
        private val UNIT_FILES = listOf(
            "es_u1" to "content/spanish_unit1.json",
            "es_u2" to "content/spanish_unit2.json",
            "es_u3" to "content/spanish_unit3.json",
            "es_u4" to "content/spanish_unit4.json",
            "es_u5" to "content/spanish_unit5.json"
        )
    }
}
