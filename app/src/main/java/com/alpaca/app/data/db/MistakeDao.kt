package com.alpaca.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.alpaca.app.data.db.entities.MistakeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MistakeDao {
    @Query("SELECT * FROM mistakes ORDER BY createdAtEpochMs ASC")
    fun observeAll(): Flow<List<MistakeEntity>>

    @Query("SELECT * FROM mistakes ORDER BY createdAtEpochMs DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<MistakeEntity>

    @Query("SELECT COUNT(*) FROM mistakes")
    suspend fun count(): Int

    @Insert
    suspend fun insert(mistake: MistakeEntity)

    @Query("DELETE FROM mistakes WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM mistakes")
    suspend fun clearAll()
}
