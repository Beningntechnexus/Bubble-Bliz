package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_profile")
data class PlayerProfileEntity(
    @PrimaryKey val id: Int = 1,
    val name: String = "Player",
    val currentLevel: Int = 1,
    val unlockedLevel: Int = 1,
    val coins: Int = 300,
    val bombCount: Int = 3,
    val rainbowCount: Int = 2,
    val lightningCount: Int = 2,
    val fireCount: Int = 1,
    val freezeCount: Int = 1,
    val highScore: Int = 0,
    val dailyLastClaimDate: String = "",
    val dailyStreakDay: Int = 0,
    val dailyBonusAdDate: String = "",
    val challengeDate: String = "",
    val challengeDone: Boolean = false,
    val spinDate: String = "",
    val spinFreeUsed: Int = 0,
    val spinAdUsed: Int = 0,
    val bonusShots: Int = 0,
    val soundEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val sfxVolume: Float = 0.85f,
    val musicVolume: Float = 0.55f,
    val bubblesPoppedTotal: Int = 0,
    val boostersUsedTotal: Int = 0,
    val maxCombo: Int = 0,
    val gamesPlayed: Int = 0,
    val gamesWon: Int = 0,
    val totalScore: Long = 0L,
    val weekKey: String = "",
    val weekScore: Int = 0,
    val adBoosterDate: String = "",
    val adBoosterCount: Int = 0,
    val shopPurchasesCount: Int = 0,
    val maxDateSeen: String = ""
)

@Entity(tableName = "level_progress")
data class LevelProgressEntity(
    @PrimaryKey val levelNumber: Int,
    val stars: Int,
    val bestScore: Int,
    val completedAt: Long
)

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val id: String,
    val unlockedAt: Long
)

@Entity(tableName = "local_scores")
data class LocalScoreEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playerName: String,
    val score: Int,
    val levelNumber: Int,
    val dateKey: String,
    val createdAt: Long
)

@Entity(tableName = "saved_session")
data class SavedSessionEntity(
    @PrimaryKey val id: Int = 1,
    val payloadJson: String,
    val updatedAt: Long
)
