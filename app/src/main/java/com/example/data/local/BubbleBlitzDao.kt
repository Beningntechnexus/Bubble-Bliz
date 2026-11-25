package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BubbleBlitzDao {
    @Query("SELECT * FROM player_profile WHERE id = 1")
    fun observeProfile(): Flow<PlayerProfileEntity?>

    @Query("SELECT * FROM player_profile WHERE id = 1")
    suspend fun getProfileOnce(): PlayerProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: PlayerProfileEntity)

    @Query("SELECT * FROM level_progress ORDER BY levelNumber ASC")
    fun observeAllLevels(): Flow<List<LevelProgressEntity>>

    @Query("SELECT * FROM level_progress")
    suspend fun getAllLevelsOnce(): List<LevelProgressEntity>

    @Query("SELECT * FROM level_progress WHERE levelNumber = :level")
    suspend fun getLevelOnce(level: Int): LevelProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLevelProgress(progress: LevelProgressEntity)

    @Query("DELETE FROM level_progress")
    suspend fun clearAllLevels()

    @Query("SELECT * FROM achievements")
    fun observeAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements")
    suspend fun getAchievementsOnce(): List<AchievementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAchievement(achievement: AchievementEntity)

    @Query("DELETE FROM achievements")
    suspend fun clearAllAchievements()

    @Query("SELECT * FROM local_scores ORDER BY score DESC, createdAt DESC LIMIT 10")
    fun observeTopScores(): Flow<List<LocalScoreEntity>>

    @Query("SELECT * FROM local_scores ORDER BY score DESC, createdAt DESC")
    suspend fun getAllScoresOnce(): List<LocalScoreEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScore(score: LocalScoreEntity)

    @Query("DELETE FROM local_scores WHERE id NOT IN (SELECT id FROM local_scores ORDER BY score DESC, createdAt DESC LIMIT 10)")
    suspend fun trimScoresToTop10()

    @Query("DELETE FROM local_scores")
    suspend fun clearAllScores()

    @Query("SELECT * FROM saved_session WHERE id = 1")
    fun observeSavedSession(): Flow<SavedSessionEntity?>

    @Query("SELECT * FROM saved_session WHERE id = 1")
    suspend fun getSavedSessionOnce(): SavedSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSavedSession(session: SavedSessionEntity)

    @Query("DELETE FROM saved_session")
    suspend fun clearSavedSession()
}
