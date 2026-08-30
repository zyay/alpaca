package com.alpaca.app.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class LessonStatus { LOCKED, AVAILABLE, COMPLETE }

@Entity(tableName = "lesson_progress")
data class LessonProgressEntity(
    @PrimaryKey val lessonId: String,
    val status: LessonStatus = LessonStatus.LOCKED,
    val bestScore: Int = 0,
    val attempts: Int = 0
)
