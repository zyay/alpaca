package com.alpaca.app.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.alpaca.app.data.db.entities.LessonProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LessonProgressDao {
    @Query("SELECT * FROM lesson_progress")
    fun observeAll(): Flow<List<LessonProgressEntity>>

    @Query("SELECT * FROM lesson_progress WHERE lessonId = :lessonId")
    suspend fun get(lessonId: String): LessonProgressEntity?

    @Query("SELECT COUNT(*) FROM lesson_progress")
    suspend fun count(): Int

    @Upsert
    suspend fun upsert(entity: LessonProgressEntity)
}
