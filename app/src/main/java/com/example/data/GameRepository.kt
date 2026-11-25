package com.example.data

import com.example.data.local.AchievementEntity
import com.example.data.local.BubbleBlitzDao
import com.example.data.local.LevelProgressEntity
import com.example.data.local.LocalScoreEntity
import com.example.data.local.PlayerProfileEntity
import com.example.data.local.SavedSessionEntity
import com.example.game.engine.CellSnapshot
import com.example.game.engine.SessionSnapshot
import com.example.game.model.ACHIEVEMENTS
import com.example.game.model.BoosterType
import com.example.game.model.DAILY_REWARD_TIERS
import com.example.game.model.MAX_LEVELS
import com.example.game.model.SPIN_SEGMENTS
import com.example.game.model.SpinSegment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

data class GameSaveState(
    val profile: PlayerProfileEntity = PlayerProfileEntity(),
    val levelStars: Map<Int, Int> = emptyMap(),
    val levelBestScores: Map<Int, Int> = emptyMap(),
    val achievements: Map<String, Long> = emptyMap(),
    val topScores: List<LocalScoreEntity> = emptyList(),
    val savedSession: SessionSnapshot? = null
) {
    val completedLevelsCount: Int get() = levelStars.count { it.value > 0 }
    val totalStars: Int get() = levelStars.values.sum()
    val threeStarLevelsCount: Int get() = levelStars.count { it.value >= 3 }

    fun boosterCount(type: BoosterType): Int = when (type) {
        BoosterType.BOMB -> profile.bombCount
        BoosterType.RAINBOW -> profile.rainbowCount
        BoosterType.LIGHTNING -> profile.lightningCount
        BoosterType.FIRE -> profile.fireCount
        BoosterType.FREEZE -> profile.freezeCount
    }
}

data class LevelCompletionResult(
    val stars: Int,
    val coinsEarned: Int,
    val unlockedBoosters: List<BoosterType>,
    val newlyUnlockedAchievements: List<String>
)

class GameRepository(private val dao: BubbleBlitzDao) {

    val stateFlow: Flow<GameSaveState> = combine(
        dao.observeProfile(),
        dao.observeAllLevels(),
        dao.observeAchievements(),
        dao.observeTopScores(),
        dao.observeSavedSession()
    ) { profile, levels, achs, scores, session ->
        val p = profile ?: PlayerProfileEntity()
        val starsMap = levels.associate { it.levelNumber to it.stars }
        val scoreMap = levels.associate { it.levelNumber to it.bestScore }
        val achMap = achs.associate { it.id to it.unlockedAt }
        val snap = session?.let { decodeSnapshot(it.payloadJson) }
        GameSaveState(
            profile = p,
            levelStars = starsMap,
            levelBestScores = scoreMap,
            achievements = achMap,
            topScores = scores,
            savedSession = snap
        )
    }

    suspend fun ensureInitialized(): PlayerProfileEntity {
        val existing = dao.getProfileOnce()
        if (existing != null) return existing
        val initial = PlayerProfileEntity(maxDateSeen = currentRealDateKey())
        dao.upsertProfile(initial)
        return initial
    }

    fun currentRealDateKey(): String {
        val cal = Calendar.getInstance()
        return String.format(
            Locale.US,
            "%04d-%02d-%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    suspend fun effectiveTodayKey(): String {
        val real = currentRealDateKey()
        val p = ensureInitialized()
        if (p.maxDateSeen.isEmpty() || real > p.maxDateSeen) {
            dao.upsertProfile(p.copy(maxDateSeen = real))
            return real
        }
        return p.maxDateSeen
    }

    fun weekKeyFor(dateKey: String): String {
        val parts = dateKey.split("-")
        if (parts.size != 3) return dateKey
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt(), 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val dow = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
        cal.add(Calendar.DAY_OF_MONTH, -dow)
        return String.format(
            Locale.US,
            "%04d-%02d-%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun dayDifference(fromKey: String, toKey: String): Int {
        fun toUtcDays(key: String): Long {
            val parts = key.split("-")
            if (parts.size != 3) return 0L
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt(), 0, 0, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis / 86_400_000L
        }
        return (toUtcDays(toKey) - toUtcDays(fromKey)).toInt()
    }

    fun nextDailyStreakDay(profile: PlayerProfileEntity, todayKey: String): Int {
        val last = profile.dailyLastClaimDate
        if (last.isEmpty()) return 1
        val diff = dayDifference(last, todayKey)
        return when {
            diff == 1 -> (profile.dailyStreakDay % 7) + 1
            diff <= 0 -> profile.dailyStreakDay.coerceAtLeast(1)
            else -> 1
        }
    }

    suspend fun claimDailyReward(): Pair<Int, String>? {
        val today = effectiveTodayKey()
        val p = ensureInitialized()
        if (p.dailyLastClaimDate == today) return null
        val day = nextDailyStreakDay(p, today)
        val tier = DAILY_REWARD_TIERS[day - 1]
        var updated = p.copy(
            coins = p.coins + tier.coins,
            dailyLastClaimDate = today,
            dailyStreakDay = day
        )
        tier.booster?.let { updated = addBoosterCount(updated, it, 1) }
        repeat(tier.randomBoosters) {
            val rnd = BoosterType.entries[Random.nextInt(BoosterType.entries.size)]
            updated = addBoosterCount(updated, rnd, 1)
        }
        if (tier.allBoosters) {
            for (b in BoosterType.entries) {
                updated = addBoosterCount(updated, b, 1)
            }
        }
        dao.upsertProfile(updated)
        return day to tier.title
    }

    suspend fun grantDailyBonusAdCoins(): Int {
        val today = effectiveTodayKey()
        val p = ensureInitialized()
        if (p.dailyBonusAdDate == today) return 0
        dao.upsertProfile(p.copy(coins = p.coins + 100, dailyBonusAdDate = today))
        return 100
    }

    suspend fun buyBooster(type: BoosterType): Boolean {
        val p = ensureInitialized()
        if (p.coins < type.price) return false
        var updated = p.copy(
            coins = p.coins - type.price,
            shopPurchasesCount = p.shopPurchasesCount + 1
        )
        updated = addBoosterCount(updated, type, 1)
        dao.upsertProfile(updated)
        return true
    }

    suspend fun grantAdBooster(): BoosterType? {
        val today = effectiveTodayKey()
        var p = ensureInitialized()
        if (p.adBoosterDate != today) {
            p = p.copy(adBoosterDate = today, adBoosterCount = 0)
        }
        if (p.adBoosterCount >= 3) return null
        val reward = BoosterType.entries[Random.nextInt(BoosterType.entries.size)]
        p = p.copy(adBoosterCount = p.adBoosterCount + 1)
        p = addBoosterCount(p, reward, 1)
        dao.upsertProfile(p)
        return reward
    }

    suspend fun canSpinFree(): Boolean {
        val today = effectiveTodayKey()
        val p = ensureInitialized()
        return p.spinDate != today || p.spinFreeUsed < 1
    }

    suspend fun remainingAdSpins(): Int {
        val today = effectiveTodayKey()
        val p = ensureInitialized()
        return if (p.spinDate != today) 2 else max(0, 2 - p.spinAdUsed)
    }

    suspend fun consumeSpinAndGrantPrize(isAdSpin: Boolean): Pair<Int, SpinSegment>? {
        val today = effectiveTodayKey()
        var p = ensureInitialized()
        if (p.spinDate != today) {
            p = p.copy(spinDate = today, spinFreeUsed = 0, spinAdUsed = 0)
        }
        if (!isAdSpin && p.spinFreeUsed >= 1) return null
        if (isAdSpin && p.spinAdUsed >= 2) return null

        val totalWeight = SPIN_SEGMENTS.sumOf { it.weight }
        var roll = Random.nextFloat() * totalWeight
        var chosenIdx = 0
        for (i in SPIN_SEGMENTS.indices) {
            roll -= SPIN_SEGMENTS[i].weight
            if (roll <= 0f) {
                chosenIdx = i
                break
            }
        }
        val segment = SPIN_SEGMENTS[chosenIdx]
        p = if (isAdSpin) {
            p.copy(spinAdUsed = p.spinAdUsed + 1)
        } else {
            p.copy(spinFreeUsed = 1)
        }
        if (segment.coins > 0) p = p.copy(coins = p.coins + segment.coins)
        segment.booster?.let { p = addBoosterCount(p, it, 1) }
        if (segment.extraShots > 0) p = p.copy(bonusShots = p.bonusShots + segment.extraShots)
        dao.upsertProfile(p)
        return chosenIdx to segment
    }

    suspend fun consumeBooster(type: BoosterType): Boolean {
        val p = ensureInitialized()
        val current = when (type) {
            BoosterType.BOMB -> p.bombCount
            BoosterType.RAINBOW -> p.rainbowCount
            BoosterType.LIGHTNING -> p.lightningCount
            BoosterType.FIRE -> p.fireCount
            BoosterType.FREEZE -> p.freezeCount
        }
        if (current <= 0) return false
        var updated = addBoosterCount(p, type, -1)
        updated = updated.copy(boostersUsedTotal = updated.boostersUsedTotal + 1)
        dao.upsertProfile(updated)
        checkAndUnlockAchievements()
        return true
    }

    suspend fun consumeBonusShots(): Int {
        val p = ensureInitialized()
        if (p.bonusShots <= 0) return 0
        val count = p.bonusShots
        dao.upsertProfile(p.copy(bonusShots = 0))
        return count
    }

    suspend fun recordGameStarted() {
        val p = ensureInitialized()
        dao.upsertProfile(p.copy(gamesPlayed = p.gamesPlayed + 1))
    }

    suspend fun recordShotStats(bubblesRemovedDelta: Int, maxComboInGame: Int) {
        if (bubblesRemovedDelta <= 0 && maxComboInGame <= 0) return
        val p = ensureInitialized()
        dao.upsertProfile(
            p.copy(
                bubblesPoppedTotal = p.bubblesPoppedTotal + max(0, bubblesRemovedDelta),
                maxCombo = max(p.maxCombo, maxComboInGame)
            )
        )
        checkAndUnlockAchievements()
    }

    suspend fun recordLevelLost(score: Int, levelNumber: Int, isDaily: Boolean) {
        dao.clearSavedSession()
        val today = effectiveTodayKey()
        val week = weekKeyFor(today)
        var p = ensureInitialized()
        val newWeekScore = (if (p.weekKey == week) p.weekScore else 0) + score
        p = p.copy(
            highScore = max(p.highScore, score),
            totalScore = p.totalScore + score,
            weekKey = week,
            weekScore = newWeekScore
        )
        dao.upsertProfile(p)
        if (score > 0) {
            dao.insertScore(
                LocalScoreEntity(
                    playerName = p.name,
                    score = score,
                    levelNumber = if (isDaily) 0 else levelNumber,
                    dateKey = today,
                    createdAt = System.currentTimeMillis()
                )
            )
            dao.trimScoresToTop10()
        }
    }

    suspend fun recordLevelWon(
        score: Int,
        stars: Int,
        levelNumber: Int,
        isDaily: Boolean
    ): LevelCompletionResult {
        dao.clearSavedSession()
        val today = effectiveTodayKey()
        val week = weekKeyFor(today)
        var p = ensureInitialized()
        val newWeekScore = (if (p.weekKey == week) p.weekScore else 0) + score
        val unlockedBoosters = mutableListOf<BoosterType>()
        var coinsEarned: Int

        if (isDaily) {
            val firstToday = !(p.challengeDate == today && p.challengeDone)
            if (firstToday) {
                coinsEarned = 200
                val b = BoosterType.entries[Random.nextInt(BoosterType.entries.size)]
                p = addBoosterCount(p, b, 1)
                unlockedBoosters.add(b)
                p = p.copy(challengeDate = today, challengeDone = true)
            } else {
                coinsEarned = 20
            }
        } else {
            val existingLevel = dao.getLevelOnce(levelNumber)
            val prevStars = existingLevel?.stars ?: 0
            val firstClear = prevStars == 0
            coinsEarned = 40 + stars * 20 + min(60, levelNumber)
            if (!firstClear) coinsEarned = (coinsEarned * 0.4f).roundToInt()

            dao.upsertLevelProgress(
                LevelProgressEntity(
                    levelNumber = levelNumber,
                    stars = max(prevStars, stars),
                    bestScore = max(existingLevel?.bestScore ?: 0, score),
                    completedAt = System.currentTimeMillis()
                )
            )

            if (firstClear) {
                if (levelNumber % 10 == 0) {
                    coinsEarned += 250
                    repeat(2) {
                        val b = BoosterType.entries[Random.nextInt(BoosterType.entries.size)]
                        p = addBoosterCount(p, b, 1)
                        unlockedBoosters.add(b)
                    }
                } else if (levelNumber % 3 == 0) {
                    val b = BoosterType.entries[Random.nextInt(BoosterType.entries.size)]
                    p = addBoosterCount(p, b, 1)
                    unlockedBoosters.add(b)
                }
            }
            val nextUnlocked = max(p.unlockedLevel, min(MAX_LEVELS, levelNumber + 1))
            p = p.copy(unlockedLevel = nextUnlocked, currentLevel = nextUnlocked)
        }

        p = p.copy(
            coins = p.coins + coinsEarned,
            highScore = max(p.highScore, score),
            totalScore = p.totalScore + score,
            gamesWon = p.gamesWon + 1,
            weekKey = week,
            weekScore = newWeekScore
        )
        dao.upsertProfile(p)

        if (score > 0) {
            dao.insertScore(
                LocalScoreEntity(
                    playerName = p.name,
                    score = score,
                    levelNumber = if (isDaily) 0 else levelNumber,
                    dateKey = today,
                    createdAt = System.currentTimeMillis()
                )
            )
            dao.trimScoresToTop10()
        }

        val unlockedAchs = checkAndUnlockAchievements()
        return LevelCompletionResult(
            stars = stars,
            coinsEarned = coinsEarned,
            unlockedBoosters = unlockedBoosters,
            newlyUnlockedAchievements = unlockedAchs
        )
    }

    suspend fun grantDoubleCoinsReward(amount: Int) {
        if (amount <= 0) return
        val p = ensureInitialized()
        dao.upsertProfile(p.copy(coins = p.coins + amount))
    }

    suspend fun checkAndUnlockAchievements(): List<String> {
        val p = ensureInitialized()
        val levels = dao.getAllLevelsOnce()
        val doneCount = levels.count { it.stars > 0 }
        val threeStarCount = levels.count { it.stars >= 3 }
        val unlockedIds = dao.getAchievementsOnce().map { it.id }.toHashSet()

        val newlyUnlocked = mutableListOf<String>()
        var bonusCoins = 0
        for (def in ACHIEVEMENTS) {
            if (unlockedIds.contains(def.id)) continue
            val progress = when (def.id) {
                "pop1" -> p.bubblesPoppedTotal
                "win1", "lv10", "lv25", "lv100" -> doneCount
                "b1000" -> p.bubblesPoppedTotal
                "combo10" -> p.maxCombo
                "star3" -> threeStarCount
                "boost" -> p.boostersUsedTotal
                else -> 0
            }
            if (progress >= def.target) {
                dao.upsertAchievement(AchievementEntity(def.id, System.currentTimeMillis()))
                bonusCoins += def.rewardCoins
                newlyUnlocked.add("${def.title} (+${def.rewardCoins} coins)")
            }
        }
        if (bonusCoins > 0) {
            val refreshed = ensureInitialized()
            dao.upsertProfile(refreshed.copy(coins = refreshed.coins + bonusCoins))
        }
        return newlyUnlocked
    }

    suspend fun renamePlayer(newName: String) {
        val clean = newName.trim().take(12)
        if (clean.isEmpty()) return
        val p = ensureInitialized()
        dao.upsertProfile(p.copy(name = clean))
    }

    suspend fun updateSettings(
        soundEnabled: Boolean? = null,
        musicEnabled: Boolean? = null,
        vibrationEnabled: Boolean? = null,
        sfxVolume: Float? = null,
        musicVolume: Float? = null
    ) {
        val p = ensureInitialized()
        dao.upsertProfile(
            p.copy(
                soundEnabled = soundEnabled ?: p.soundEnabled,
                musicEnabled = musicEnabled ?: p.musicEnabled,
                vibrationEnabled = vibrationEnabled ?: p.vibrationEnabled,
                sfxVolume = sfxVolume ?: p.sfxVolume,
                musicVolume = musicVolume ?: p.musicVolume
            )
        )
    }

    suspend fun saveActiveSession(snapshot: SessionSnapshot) {
        dao.upsertSavedSession(
            SavedSessionEntity(
                id = 1,
                payloadJson = encodeSnapshot(snapshot),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearActiveSession() {
        dao.clearSavedSession()
    }

    suspend fun resetAllProgress() {
        dao.clearAllLevels()
        dao.clearAllAchievements()
        dao.clearAllScores()
        dao.clearSavedSession()
        dao.upsertProfile(PlayerProfileEntity(maxDateSeen = currentRealDateKey()))
    }

    private fun addBoosterCount(p: PlayerProfileEntity, type: BoosterType, delta: Int): PlayerProfileEntity {
        return when (type) {
            BoosterType.BOMB -> p.copy(bombCount = max(0, p.bombCount + delta))
            BoosterType.RAINBOW -> p.copy(rainbowCount = max(0, p.rainbowCount + delta))
            BoosterType.LIGHTNING -> p.copy(lightningCount = max(0, p.lightningCount + delta))
            BoosterType.FIRE -> p.copy(fireCount = max(0, p.fireCount + delta))
            BoosterType.FREEZE -> p.copy(freezeCount = max(0, p.freezeCount + delta))
        }
    }

    private fun encodeSnapshot(snap: SessionSnapshot): String {
        val header = listOf(
            snap.levelNumber,
            if (snap.isDaily) 1 else 0,
            snap.dailyDateKey,
            snap.parity,
            snap.shots,
            snap.shotsMax,
            snap.score,
            snap.removedCount,
            snap.colorCleared,
            snap.combo,
            snap.maxCombo,
            snap.missCount,
            snap.freezeTurns,
            snap.curColorId,
            snap.nextColorId,
            if (snap.usedBooster) 1 else 0
        ).joinToString(",")
        val rows = snap.grid.joinToString(";") { row ->
            row.joinToString(",") { cell ->
                if (cell == null) "_"
                else "${cell.colorId}:${if (cell.isStone) 1 else 0}:${if (cell.hasBomb) 1 else 0}"
            }
        }
        return "$header|$rows"
    }

    private fun decodeSnapshot(payload: String): SessionSnapshot? {
        return try {
            val parts = payload.split("|", limit = 2)
            if (parts.size != 2) return null
            val h = parts[0].split(",")
            if (h.size < 16) return null
            val grid = if (parts[1].isEmpty()) emptyList() else parts[1].split(";").map { rowStr ->
                rowStr.split(",").map { cellStr ->
                    if (cellStr == "_") null
                    else {
                        val f = cellStr.split(":")
                        CellSnapshot(
                            colorId = f[0].toInt(),
                            isStone = f[1].toInt() == 1,
                            hasBomb = f[2].toInt() == 1
                        )
                    }
                }
            }
            SessionSnapshot(
                levelNumber = h[0].toInt(),
                isDaily = h[1].toInt() == 1,
                dailyDateKey = h[2],
                grid = grid,
                parity = h[3].toInt(),
                shots = h[4].toInt(),
                shotsMax = h[5].toInt(),
                score = h[6].toInt(),
                removedCount = h[7].toInt(),
                colorCleared = h[8].toInt(),
                combo = h[9].toInt(),
                maxCombo = h[10].toInt(),
                missCount = h[11].toInt(),
                freezeTurns = h[12].toInt(),
                curColorId = h[13].toInt(),
                nextColorId = h[14].toInt(),
                usedBooster = h[15].toInt() == 1
            )
        } catch (_: Exception) {
            null
        }
    }
}
