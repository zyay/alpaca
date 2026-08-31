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

    suspend fun loadUnits(languageId: String = CourseLanguage.Spanish.id): List<CourseUnit> =
        withContext(Dispatchers.IO) {
            val all = cache ?: loadAllUnits().also { cache = it }
            all.filter { it.unitId.startsWith("${languageId}_") }
        }

    suspend fun loadUnit(unitId: String): CourseUnit? =
        loadAllUnits().firstOrNull { it.unitId == unitId }

    suspend fun findLesson(lessonId: String): Lesson? =
        loadAllUnits().asSequence().flatMap { it.lessons }.firstOrNull { it.lessonId == lessonId }

    suspend fun unitOfLesson(lessonId: String): CourseUnit? =
        loadAllUnits().firstOrNull { unit -> unit.lessons.any { it.lessonId == lessonId } }

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

    private fun loadAllUnits(): List<CourseUnit> {
        val files = context.assets.list(CONTENT_DIR).orEmpty()
            .filter { it.endsWith(".json") }
            .sorted()
        return files.map { file ->
            try {
                val text = context.assets.open("$CONTENT_DIR/$file")
                    .bufferedReader().use { it.readText() }
                json.decodeFromString<CourseUnit>(text)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load unit file $file", e)
                null
            }
        }.filterNotNull().also {
            Log.i(TAG, "Loaded ${it.size} units, ${it.sumOf { u -> u.lessons.size }} lessons")
        }
    }

    companion object {
        private const val TAG = "Alpaca"
        private const val CONTENT_DIR = "content"
        const val REVIEW_LESSON_ID = "review"
    }
}
