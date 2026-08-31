package com.alpaca.app.data.repository

import com.alpaca.app.data.db.AlpacaDatabase
import com.alpaca.app.data.db.entities.MistakeEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MistakeRepository(db: AlpacaDatabase) {
    private val dao = db.mistakeDao()

    fun observeAll(): Flow<List<MistakeEntity>> = dao.observeAll()

    fun observeCount(): Flow<Int> = dao.observeAll().map { it.size }

    suspend fun recent(limit: Int = 12): List<MistakeEntity> = dao.recent(limit)

    suspend fun count(): Int = dao.count()

    suspend fun log(lessonId: String, exerciseIndex: Int, promptLabel: String) {
        dao.insert(
            MistakeEntity(
                lessonId = lessonId,
                exerciseIndex = exerciseIndex,
                promptLabel = promptLabel,
                createdAtEpochMs = System.currentTimeMillis()
            )
        )
    }

    suspend fun consume(mistakes: List<MistakeEntity>) {
        dao.deleteByIds(mistakes.map { it.id })
    }
}
