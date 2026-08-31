package com.alpaca.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.alpaca.app.data.db.converters.Converters
import com.alpaca.app.data.db.entities.LessonProgressEntity
import com.alpaca.app.data.db.entities.MistakeEntity
import com.alpaca.app.data.db.entities.UserEntity

@Database(
    entities = [UserEntity::class, LessonProgressEntity::class, MistakeEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AlpacaDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun lessonProgressDao(): LessonProgressDao
    abstract fun mistakeDao(): MistakeDao

    companion object {
        fun create(context: Context): AlpacaDatabase =
            Room.databaseBuilder(context, AlpacaDatabase::class.java, "alpaca.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
