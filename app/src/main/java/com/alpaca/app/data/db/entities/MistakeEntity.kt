package com.alpaca.app.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mistakes")
data class MistakeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val lessonId: String,
    val exerciseIndex: Int,
    val promptLabel: String,
    val createdAtEpochMs: Long
)
