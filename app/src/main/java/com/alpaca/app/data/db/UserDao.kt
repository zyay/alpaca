package com.alpaca.app.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.alpaca.app.data.db.entities.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM user WHERE id = 1")
    fun observeUser(): Flow<UserEntity?>

    @Query("SELECT * FROM user WHERE id = 1")
    suspend fun getUser(): UserEntity?

    @Upsert
    suspend fun upsert(user: UserEntity)
}
