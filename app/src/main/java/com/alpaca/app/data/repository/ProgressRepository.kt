package com.alpaca.app.data.repository

import com.alpaca.app.data.content.ContentRepository
import com.alpaca.app.data.content.CourseUnit
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
        val unit = content.loadUnit()
        unit.lessons.forEachIndexed { index, lesson ->
            progressDao.upsert(
                LessonProgressEntity(
                    lessonId = lesson.lessonId,
                    status = if (index == 0) LessonStatus.AVAILABLE else LessonStatus.LOCKED
                )
            )
        }
    }

    /**
     * Trail state is derived: lessons unlock strictly in order, so the effective
     * status never depends on stale rows alone.
     */
    fun effectiveStatus(
        unit: CourseUnit,
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

    suspend fun completeLesson(lessonId: String, scorePercent: Int) {
        val unit = content.loadUnit()
        val current = progressDao.get(lessonId) ?: LessonProgressEntity(lessonId)
        progressDao.upsert(
            current.copy(
                status = LessonStatus.COMPLETE,
                bestScore = maxOf(current.bestScore, scorePercent),
                attempts = current.attempts + 1
            )
        )
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
        val current = progressDao.get(lessonId) ?: LessonProgressEntity(lessonId)
        progressDao.upsert(current.copy(attempts = current.attempts + 1))
    }
}
