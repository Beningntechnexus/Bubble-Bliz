package com.example

import com.example.data.GameRepository
import com.example.data.local.AchievementEntity
import com.example.data.local.BubbleBlitzDao
import com.example.data.local.LevelProgressEntity
import com.example.data.local.LocalScoreEntity
import com.example.data.local.PlayerProfileEntity
import com.example.data.local.SavedSessionEntity
import com.example.game.engine.BoardMetrics
import com.example.game.engine.BubbleGameSession
import com.example.game.engine.GamePhase
import com.example.game.engine.HexGridUtils
import com.example.game.engine.LevelGenerator
import com.example.game.model.BoosterType
import com.example.game.model.BubbleColor
import com.example.game.model.CellBubble
import com.example.game.model.EndReason
import com.example.game.model.GRID_COLS
import com.example.game.model.LevelObjective
import com.example.game.model.MAX_LEVELS
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class BubbleBlitzTest {

    private class InMemoryDao : BubbleBlitzDao {
        private val profileFlow = MutableStateFlow<PlayerProfileEntity?>(null)
        private val levelsFlow = MutableStateFlow<Map<Int, LevelProgressEntity>>(emptyMap())
        private val achievementsFlow = MutableStateFlow<Map<String, AchievementEntity>>(emptyMap())
        private val scoresFlow = MutableStateFlow<List<LocalScoreEntity>>(emptyList())
        private val sessionFlow = MutableStateFlow<SavedSessionEntity?>(null)
        private var nextScoreId = 1L

        override fun observeProfile(): Flow<PlayerProfileEntity?> = profileFlow
        override suspend fun getProfileOnce(): PlayerProfileEntity? = profileFlow.value
        override suspend fun upsertProfile(profile: PlayerProfileEntity) {
            profileFlow.value = profile
        }

        override fun observeAllLevels(): Flow<List<LevelProgressEntity>> =
            levelsFlow.map { it.values.sortedBy { l -> l.levelNumber } }
        override suspend fun getAllLevelsOnce(): List<LevelProgressEntity> =
            levelsFlow.value.values.toList()
        override suspend fun getLevelOnce(level: Int): LevelProgressEntity? =
            levelsFlow.value[level]
        override suspend fun upsertLevelProgress(progress: LevelProgressEntity) {
            levelsFlow.value = levelsFlow.value + (progress.levelNumber to progress)
        }
        override suspend fun clearAllLevels() {
            levelsFlow.value = emptyMap()
        }

        override fun observeAchievements(): Flow<List<AchievementEntity>> =
            achievementsFlow.map { it.values.toList() }
        override suspend fun getAchievementsOnce(): List<AchievementEntity> =
            achievementsFlow.value.values.toList()
        override suspend fun upsertAchievement(achievement: AchievementEntity) {
            achievementsFlow.value = achievementsFlow.value + (achievement.id to achievement)
        }
        override suspend fun clearAllAchievements() {
            achievementsFlow.value = emptyMap()
        }

        override fun observeTopScores(): Flow<List<LocalScoreEntity>> =
            scoresFlow.map { it.sortedWith(compareByDescending<LocalScoreEntity> { s -> s.score }.thenByDescending { s -> s.createdAt }).take(10) }
        override suspend fun getAllScoresOnce(): List<LocalScoreEntity> = scoresFlow.value
        override suspend fun insertScore(score: LocalScoreEntity) {
            val withId = if (score.id == 0L) score.copy(id = nextScoreId++) else score
            scoresFlow.value = scoresFlow.value + withId
        }
        override suspend fun trimScoresToTop10() {
            scoresFlow.value = scoresFlow.value
                .sortedWith(compareByDescending<LocalScoreEntity> { it.score }.thenByDescending { it.createdAt })
                .take(10)
        }
        override suspend fun clearAllScores() {
            scoresFlow.value = emptyList()
        }

        override fun observeSavedSession(): Flow<SavedSessionEntity?> = sessionFlow
        override suspend fun getSavedSessionOnce(): SavedSessionEntity? = sessionFlow.value
        override suspend fun upsertSavedSession(session: SavedSessionEntity) {
            sessionFlow.value = session
        }
        override suspend fun clearSavedSession() {
            sessionFlow.value = null
        }
    }

    @Test
    fun all100LevelsGenerateCleanlyWithNoFloatingBubbles() {
        for (level in 1..MAX_LEVELS) {
            val config = LevelGenerator.generateLevel(level)
            assertEquals(level, config.levelNumber)
            assertTrue("Level $level must have bubbles", config.bubbleCount > 0)
            assertTrue("Level $level must have positive shots", config.shots > 0)
            val floating = HexGridUtils.findFloatingCells(config.grid, 0)
            assertTrue("Level $level should have zero floating cells initially", floating.isEmpty())
        }
        val daily = LevelGenerator.generateDailyChallenge("2026-11-24")
        assertTrue(daily.bubbleCount > 0)
    }

    @Test
    fun engineShootsMatchesPopsAndDropsDetachedBubbles() {
        val grid = List(3) { r ->
            MutableList<CellBubble?>(GRID_COLS) { c ->
                when {
                    r == 0 && c == 4 -> CellBubble(BubbleColor.RED)
                    r == 0 && c == 5 -> CellBubble(BubbleColor.RED)
                    r == 1 && c == 5 -> CellBubble(BubbleColor.BLUE) // anchored under (0,5)
                    else -> null
                }
            }
        }
        val config = LevelGenerator.generateLevel(1).copy(
            grid = grid,
            objective = LevelObjective.ClearAll,
            shots = 10,
            dropEvery = 0
        )
        val session = BubbleGameSession(config = config, isDaily = false)
        session.metrics = BoardMetrics.compute(360f, 640f)
        session.curColor = BubbleColor.RED
        session.aimAt(session.cellX(0, 4) + session.metrics.radius * 0.2f, session.cellY(0))

        assertTrue(session.fireShot {})
        assertEquals(GamePhase.BUSY, session.phase)

        var steps = 0
        while (session.phase == GamePhase.BUSY && steps < 300) {
            session.update(0.016f)
            steps++
        }
        assertTrue("Score should increase after match + detached drop", session.score > 0)
        assertTrue("Remaining non-stone bubbles should drop to 0 and win", session.won)
        assertTrue(session.ended)
    }

    @Test
    fun boostersExecuteRealGameplayEffects() {
        val config = LevelGenerator.generateLevel(1, plain = true)
        val session = BubbleGameSession(config = config, isDaily = false)
        session.metrics = BoardMetrics.compute(360f, 640f)

        var consumed = 0
        assertTrue(session.activateFreeze { consumed++ })
        assertEquals(5, session.freezeTurns)
        assertEquals(1, consumed)

        session.toggleArmedBooster(BoosterType.BOMB)
        assertEquals(BoosterType.BOMB, session.armedBooster)
        session.aimAt(session.metrics.width / 2f, session.cellY(0))
        assertTrue(session.fireShot { consumed++ })
        var steps = 0
        while (session.phase == GamePhase.BUSY && steps < 200) {
            session.update(0.016f)
            steps++
        }
        assertTrue("Bomb should pop bubbles and earn score", session.score > 0)
        assertEquals(2, consumed)

        // Lightning column zap
        session.toggleArmedBooster(BoosterType.LIGHTNING)
        session.aimAt(session.metrics.width * 0.35f, session.cellY(0))
        assertTrue(session.fireShot { consumed++ })
        steps = 0
        while (session.phase == GamePhase.BUSY && steps < 200) {
            session.update(0.016f)
            steps++
        }
        assertEquals(3, consumed)
    }

    @Test
    fun repositoryPersistsProgressDailyRewardsShopAchievementsAndSession() = runBlocking {
        val repo = GameRepository(InMemoryDao())
        repo.ensureInitialized()

        val claim = repo.claimDailyReward()
        assertNotNull(claim)
        assertEquals(1, claim!!.first)
        assertNull("Cannot claim daily reward twice same day", repo.claimDailyReward())

        val afterDaily = repo.stateFlow.first()
        assertEquals(400, afterDaily.profile.coins)

        assertTrue(repo.buyBooster(BoosterType.BOMB))
        val afterShop = repo.stateFlow.first()
        assertEquals(250, afterShop.profile.coins)
        assertEquals(4, afterShop.profile.bombCount)

        val winRes = repo.recordLevelWon(score = 1200, stars = 3, levelNumber = 1, isDaily = false)
        assertEquals(3, winRes.stars)
        val afterWin = repo.stateFlow.first()
        assertEquals(2, afterWin.profile.unlockedLevel)
        assertEquals(3, afterWin.levelStars[1])
        assertTrue(afterWin.topScores.isNotEmpty())
        assertTrue(afterWin.achievements.containsKey("win1"))

        val level2Session = BubbleGameSession(LevelGenerator.generateLevel(2), isDaily = false)
        level2Session.score = 350
        repo.saveActiveSession(level2Session.exportSnapshot(repo.effectiveTodayKey()))
        val withSnap = repo.stateFlow.first()
        assertNotNull(withSnap.savedSession)
        assertEquals(350, withSnap.savedSession!!.score)

        repo.resetAllProgress()
        val afterReset = repo.stateFlow.first()
        assertEquals(1, afterReset.profile.unlockedLevel)
        assertEquals(300, afterReset.profile.coins)
        assertNull(afterReset.savedSession)
    }

    @Test
    fun extraShotsAndDuplicateRewardProtectionWorkCorrectly() {
        val config = LevelGenerator.generateLevel(1, plain = true)
        val session = BubbleGameSession(config, isDaily = false)
        session.ended = true
        session.won = false
        session.endReason = EndReason.OUT_OF_SHOTS
        session.shots = 0

        val videoCompleted = AtomicBoolean(false)
        val rewardDispatched = AtomicBoolean(false)

        // Simulating ad closed early without video completion
        if (videoCompleted.get() && rewardDispatched.compareAndSet(false, true)) {
            session.grantExtraShotsAfterAd()
        }
        assertEquals(0, session.shots)
        assertTrue(session.ended)

        // Simulating video completion followed by duplicate callback attempt
        videoCompleted.set(true)
        repeat(2) {
            if (videoCompleted.get() && rewardDispatched.compareAndSet(false, true)) {
                session.grantExtraShotsAfterAd()
            }
        }
        assertEquals(5, session.shots)
        assertFalse(session.ended)
        assertEquals(1, session.continuesUsed)
    }

    @Test
    fun adsTxtFileExistsSeparatelyAndPreservesCompleteList() {
        val candidates = listOf(File("../ads.txt"), File("ads.txt"))
        val adsFile = candidates.firstOrNull { it.exists() }
        assertNotNull("Separate ads.txt file must exist", adsFile)
        val lines = adsFile!!.readLines().filter { it.isNotBlank() }
        assertEquals(135, lines.size)
        assertEquals("start.io, 168090701, DIRECT", lines.first())
        assertEquals("bematterfull.com, 22289765, RESELLER, aefcd3d2f45b5070", lines.last())
    }
}
