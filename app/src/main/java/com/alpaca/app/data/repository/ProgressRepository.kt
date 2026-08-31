package com.alpaca.app.data.repository

import com.alpaca.app.data.content.ContentRepository
import com.alpaca.app.data.db.AlpacaDatabase
import com.alpaca.app.data.db.entities.LessonProgressEntity
import com.alpaca.app.data.db.entities.LessonStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProgressRepository(
    private val db: AlpacaDatabase,
    private val content: ContentRepository
) {
    private val progressDao = db.lessonProgressDao()

    fun observeProgress(): Flow<Map<String, LessonProgressEntity>> =
        progressDao.observeAll().map { rows -> rows.associateBy { it.lessonId } }

    suspend fun seedIfNeeded() {
        if (progressDao.count() > 0) return
        com.alpaca.app.data.content.CourseLanguage.available.forEach { language ->
            content.loadUnits(language.id).forEach { unit ->
                unit.lessons.forEachIndexed { index, lesson ->
                    progressDao.upsert(
                        LessonProgressEntity(
                            lessonId = lesson.lessonId,
                            status = if (index == 0) LessonStatus.AVAILABLE else LessonStatus.LOCKED
                        )
                    )
                }
            }
        }
    }

    /**
     * Trail state is derived: lessons unlock strictly in order within their unit,
     * so the effective status never depends on stale rows alone.
     */
    fun effectiveStatus(
        unit: com.alpaca.app.data.content.CourseUnit,
        progress: Map<String, LessonProgressEntity>,
        lessonId: String
    ): LessonStatus {
        val firstIncomplete = unit.lessons.firstOrNull {
            progress[it.lessonId]?.status != LessonStatus.COMPLETE
        }?.lessonId
        return when (lessonId) {
            firstIncomplete -> LessonStatus.AVAILABLE
            else -> {
                val row = progress[lessonId]
                if (row?.status == LessonStatus.COMPLETE) LessonStatus.COMPLETE else LessonStatus.LOCKED
            }
        }
    }

    fun unitUnlocked(
        units: List<com.alpaca.app.data.content.CourseUnit>,
        progress: Map<String, LessonProgressEntity>,
        unitId: String
    ): Boolean {
        val index = units.indexOfFirst { it.unitId == unitId }
        if (index <= 0) return true
        val previous = units[index - 1]
        return previous.lessons.all { progress[it.lessonId]?.status == LessonStatus.COMPLETE }
    }

    suspend fun completeLesson(lessonId: String, scorePercent: Int) {
        if (lessonId == ContentRepository.REVIEW_LESSON_ID) return
        val current = progressDao.get(lessonId) ?: LessonProgressEntity(lessonId)
        progressDao.upsert(
            current.copy(
                status = LessonStatus.COMPLETE,
                bestScore = maxOf(current.bestScore, scorePercent),
                attempts = current.attempts + 1
            )
        )
        val unit = content.unitOfLesson(lessonId) ?: return
        val nextLessonId = unit.lessons
            .dropWhile { it.lessonId != lessonId }
            .getOrNull(1)?.lessonId
        if (nextLessonId != null) {
            val next = progressDao.get(nextLessonId) ?: LessonProgressEntity(nextLessonId)
            if (next.status == LessonStatus.LOCKED) {
                progressDao.upsert(next.copy(status = LessonStatus.AVAILABLE))
            }
        }
    }

    suspend fun recordAttempt(lessonId: String) {
        if (lessonId == ContentRepository.REVIEW_LESSON_ID) return
        val current = progressDao.get(lessonId) ?: LessonProgressEntity(lessonId)
        progressDao.upsert(current.copy(attempts = current.attempts + 1))
    }

    suspend fun completedLessonCount(progress: Map<String, LessonProgressEntity>): Int =
        progress.values.count { it.status == LessonStatus.COMPLETE }
}
