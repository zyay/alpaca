package com.alpaca.app.data.db.converters

import androidx.room.TypeConverter
import com.alpaca.app.data.db.entities.LessonStatus

class Converters {
    @TypeConverter
    fun fromStatus(value: LessonStatus): String = value.name

    @TypeConverter
    fun toStatus(value: String): LessonStatus = LessonStatus.valueOf(value)
}
