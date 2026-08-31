package com.alpaca.app.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.alpaca.app.data.db.entities.QuestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestDao {
    @Query("SELECT * FROM quests WHERE epochDay = :day")
    fun observeForDay(day: Long): Flow<List<QuestEntity>>

    @Query("SELECT * FROM quests WHERE epochDay = :day")
    suspend fun getForDay(day: Long): List<QuestEntity>

    @Upsert
    suspend fun upsertAll(quests: List<QuestEntity>)

    @Query(
        "UPDATE quests SET progress = progress + :delta " +
            "WHERE questId = :questId AND epochDay = :day"
    )
    suspend fun addProgress(questId: String, day: Long, delta: Int)

    @Query("UPDATE quests SET claimed = 1 WHERE questId = :questId AND epochDay = :day")
    suspend fun markClaimed(questId: String, day: Long)

    @Query("DELETE FROM quests WHERE epochDay < :day")
    suspend fun deleteBefore(day: Long)
}
