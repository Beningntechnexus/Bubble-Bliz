package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PlayerProfileEntity::class,
        LevelProgressEntity::class,
        AchievementEntity::class,
        LocalScoreEntity::class,
        SavedSessionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class BubbleBlitzDatabase : RoomDatabase() {
    abstract fun dao(): BubbleBlitzDao

    companion object {
        @Volatile
        private var INSTANCE: BubbleBlitzDatabase? = null

        fun getInstance(context: Context): BubbleBlitzDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BubbleBlitzDatabase::class.java,
                    "bubble_blitz.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
