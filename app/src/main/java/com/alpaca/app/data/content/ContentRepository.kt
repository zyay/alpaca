package com.alpaca.app.data.content

import android.content.Context
import android.util.Log
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
    private var cache: CourseUnit? = null

    suspend fun loadUnit(): CourseUnit = withContext(Dispatchers.IO) {
        cache?.let { return@withContext it }
        val text = context.assets.open("content/spanish_unit1.json")
            .bufferedReader().use { it.readText() }
        val unit = json.decodeFromString<CourseUnit>(text)
        Log.i(TAG, "Loaded unit '${unit.unitId}' with ${unit.lessons.size} lessons")
        cache = unit
        unit
    }

    suspend fun findLesson(lessonId: String): Lesson? =
        loadUnit().lessons.firstOrNull { it.lessonId == lessonId }

    companion object {
        private const val TAG = "Alpaca"
    }
}
