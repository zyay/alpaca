package com.alpaca.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.alpaca.app.data.db.converters.Converters
import com.alpaca.app.data.db.entities.LessonProgressEntity
import com.alpaca.app.data.db.entities.MistakeEntity
import com.alpaca.app.data.db.entities.QuestEntity
import com.alpaca.app.data.db.entities.UserEntity

@Database(
    entities = [
        UserEntity::class, LessonProgressEntity::class, MistakeEntity::class, QuestEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AlpacaDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun lessonProgressDao(): LessonProgressDao
    abstract fun mistakeDao(): MistakeDao
    abstract fun questDao(): QuestDao

    companion object {
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user ADD COLUMN gems INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user ADD COLUMN streakFreezes INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `quests` (" +
                        "`questId` TEXT NOT NULL, " +
                        "`epochDay` INTEGER NOT NULL, " +
                        "`type` TEXT NOT NULL, " +
                        "`target` INTEGER NOT NULL, " +
                        "`progress` INTEGER NOT NULL, " +
                        "`rewardGems` INTEGER NOT NULL, " +
                        "`claimed` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`questId`))"
                )
            }
        }

        fun create(context: Context): AlpacaDatabase =
            Room.databaseBuilder(context, AlpacaDatabase::class.java, "alpaca.db")
                .addMigrations(MIGRATION_2_3)
                .fallbackToDestructiveMigration()
                .build()
    }
}
