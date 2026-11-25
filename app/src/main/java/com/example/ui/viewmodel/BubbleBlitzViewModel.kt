package com.example.ui.viewmodel

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ads.RewardedAdGateway
import com.example.ads.RewardedPlacement
import com.example.audio.SoundEngine
import com.example.data.GameRepository
import com.example.data.GameSaveState
import com.example.game.engine.BoardMetrics
import com.example.game.engine.BubbleGameSession
import com.example.game.engine.EngineEvent
import com.example.game.engine.GamePhase
import com.example.game.engine.LevelGenerator
import com.example.game.model.BoosterType
import com.example.game.model.BubbleColor
import com.example.game.model.EndReason
import com.example.game.model.LevelConfig
import com.example.game.model.LevelObjective
import com.example.game.model.MAX_LEVELS
import com.example.game.model.SpinSegment
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import kotlin.math.max
import kotlin.random.Random

sealed class ActiveModal {
    data object Pause : ActiveModal()
    data object Victory : ActiveModal()
    data object GameOver : ActiveModal()
    data object DailyReward : ActiveModal()
    data object DailyChallenge : ActiveModal()
    data object Achievements : ActiveModal()
    data object Shop : ActiveModal()
    data object LuckySpin : ActiveModal()
    data object Leaderboard : ActiveModal()
    data object Settings : ActiveModal()
    data object Rename : ActiveModal()
    data object ResetConfirm : ActiveModal()
}

data class HudState(
    val score: Int = 0,
    val shots: Int = 0,
    val stars: Int = 3,
    val levelLabel: String = "Level 1",
    val objectiveText: String = "",
    val objectiveBubbleColor: BubbleColor? = null,
    val dropStatusText: String = "",
    val armedBooster: BoosterType? = null,
    val isFreezeActive: Boolean = false,
    val isDaily: Boolean = false,
    val noBooster: Boolean = false
)

data class VictoryDisplayData(
    val levelNumber: Int,
    val isDaily: Boolean,
    val score: Int,
    val stars: Int,
    val coinsEarned: Int,
    val unlockedBoosters: List<BoosterType>,
    val doubledCoins: Boolean = false,
    val isFinalLevel: Boolean = false
)

data class GameOverDisplayData(
    val levelNumber: Int,
    val isDaily: Boolean,
    val score: Int,
    val bestScore: Int,
    val reason: EndReason,
    val canOfferExtraShots: Boolean
)

data class SpinUiState(
    val targetRotationDeg: Float = 0f,
    val isSpinning: Boolean = false,
    val canFreeSpin: Boolean = true,
    val remainingAdSpins: Int = 2,
    val lastPrizeLabel: String? = null
)

class BubbleBlitzViewModel(
    private val appContext: Context,
    private val repository: GameRepository,
    val soundEngine: SoundEngine,
    private val adGateway: RewardedAdGateway
) : ViewModel() {

    val saveState: StateFlow<GameSaveState> = repository.stateFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = GameSaveState()
    )

    val isAdReady: StateFlow<Boolean> = adGateway.isAdReady

    private val _activeModal = MutableStateFlow<ActiveModal?>(null)
    val activeModal: StateFlow<ActiveModal?> = _activeModal.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _session = MutableStateFlow<BubbleGameSession?>(null)
    val session: StateFlow<BubbleGameSession?> = _session.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _hudState = MutableStateFlow(HudState())
    val hudState: StateFlow<HudState> = _hudState.asStateFlow()

    private val _victoryData = MutableStateFlow<VictoryDisplayData?>(null)
    val victoryData: StateFlow<VictoryDisplayData?> = _victoryData.asStateFlow()

    private val _gameOverData = MutableStateFlow<GameOverDisplayData?>(null)
    val gameOverData: StateFlow<GameOverDisplayData?> = _gameOverData.asStateFlow()

    private val _spinState = MutableStateFlow(SpinUiState())
    val spinState: StateFlow<SpinUiState> = _spinState.asStateFlow()

    private val _todayKey = MutableStateFlow(repository.currentRealDateKey())
    val todayKey: StateFlow<String> = _todayKey.asStateFlow()

    private val _dailyCountdown = MutableStateFlow("Ready!")
    val dailyCountdown: StateFlow<String> = _dailyCountdown.asStateFlow()

    private var lastRecordedRemoved = 0
    private var lastRecordedMaxCombo = 0
    private var boardMaxRows = 10

    init {
        viewModelScope.launch {
            repository.ensureInitialized()
            val today = repository.effectiveTodayKey()
            _todayKey.value = today
            refreshSpinAvailability()
        }
        viewModelScope.launch {
            saveState.collect { state ->
                val p = state.profile
                soundEngine.updatePreferences(
                    sfxOn = p.soundEnabled,
                    musicOn = p.musicEnabled,
                    sfxVol = p.sfxVolume,
                    musicVol = p.musicVolume
                )
            }
        }
        viewModelScope.launch {
            while (true) {
                updateDailyCountdownTick()
                delay(1000L)
            }
        }
    }

    private suspend fun updateDailyCountdownTick() {
        val today = repository.currentRealDateKey()
        _todayKey.value = today
        val p = saveState.value.profile
        if (p.dailyLastClaimDate == today) {
            val now = Calendar.getInstance()
            val mid = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val secs = max(0L, (mid.timeInMillis - now.timeInMillis) / 1000L)
            val h = secs / 3600
            val m = (secs % 3600) / 60
            val s = secs % 60
            _dailyCountdown.value = String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
        } else {
            _dailyCountdown.value = "Tap to claim!"
        }
    }

    fun weekKeyFor(dateKey: String): String = repository.weekKeyFor(dateKey)

    fun nextDailyDay(todayKey: String): Int =
        repository.nextDailyStreakDay(saveState.value.profile, todayKey)

    fun showToast(message: String) {
        _toastMessage.value = message
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun openModal(modal: ActiveModal) {
        soundEngine.playButton()
        if (modal == ActiveModal.LuckySpin) {
            viewModelScope.launch { refreshSpinAvailability() }
        }
        _activeModal.value = modal
    }

    fun closeModal() {
        _activeModal.value = null
    }

    fun onBoardSized(width: Float, height: Float) {
        val m = BoardMetrics.compute(width, height)
        boardMaxRows = max(3, m.dangerRow - 4)
        _session.value?.metrics = m
    }

    fun startOrResumeLevel(levelNumber: Int, isDaily: Boolean = false, forceFresh: Boolean = false) {
        viewModelScope.launch {
            val state = saveState.first()
            if (!isDaily && levelNumber > state.profile.unlockedLevel) {
                showToast("Complete level ${state.profile.unlockedLevel} first!")
                return@launch
            }
            val today = repository.effectiveTodayKey()
            _todayKey.value = today
            val config: LevelConfig = if (isDaily) {
                LevelGenerator.generateDailyChallenge(today, maxRows = boardMaxRows)
            } else {
                LevelGenerator.generateLevel(levelNumber, maxRows = boardMaxRows)
            }

            val savedSnap = state.savedSession
            val shouldRestore = !forceFresh && savedSnap != null &&
                savedSnap.isDaily == isDaily &&
                savedSnap.levelNumber == (if (isDaily) 0 else levelNumber) &&
                (!isDaily || savedSnap.dailyDateKey == today)

            val bonusShots = if (!isDaily && !shouldRestore) repository.consumeBonusShots() else 0
            val newSession = BubbleGameSession(config = config, isDaily = isDaily, bonusShots = bonusShots)
            _session.value?.metrics?.let { newSession.metrics = it }

            if (shouldRestore && savedSnap != null) {
                newSession.restoreFromSnapshot(savedSnap)
                lastRecordedRemoved = savedSnap.removedCount
                lastRecordedMaxCombo = savedSnap.maxCombo
            } else {
                lastRecordedRemoved = 0
                lastRecordedMaxCombo = 0
                repository.recordGameStarted()
            }

            wireSessionCallbacks(newSession, today)
            _session.value = newSession
            _isPaused.value = false
            _activeModal.value = null
            _victoryData.value = null
            _gameOverData.value = null
            syncHud(newSession)
            repository.saveActiveSession(newSession.exportSnapshot(today))
        }
    }

    private fun wireSessionCallbacks(s: BubbleGameSession, todayKey: String) {
        s.onEvent = { event, arg ->
            when (event) {
                EngineEvent.SHOOT -> {
                    soundEngine.playShoot()
                    vibrate(10L)
                }
                EngineEvent.BOUNCE -> soundEngine.playBounce()
                EngineEvent.STICK -> soundEngine.playStick()
                EngineEvent.POP -> {
                    for (i in 0 until arg.coerceIn(1, 4)) soundEngine.playPop(i)
                    vibrate(if (arg >= 4) 35L else 15L)
                }
                EngineEvent.DROP_FLOAT -> soundEngine.playDropFloat()
                EngineEvent.BOMB_BOOM -> {
                    soundEngine.playBomb()
                    vibrate(55L)
                }
                EngineEvent.LIGHTNING_ZAP -> {
                    soundEngine.playLightning()
                    vibrate(40L)
                }
                EngineEvent.FIRE_WHOOSH -> {
                    soundEngine.playFire()
                    vibrate(30L)
                }
                EngineEvent.FREEZE_CHIME -> {
                    soundEngine.playFreeze()
                    vibrate(30L)
                }
                EngineEvent.RAINBOW_CHIME -> soundEngine.playRainbow()
                EngineEvent.ROW_DROP -> {
                    soundEngine.playDropFloat()
                    vibrate(30L)
                }
                EngineEvent.COMBO -> soundEngine.playCombo()
                EngineEvent.WIN -> {
                    soundEngine.playVictory()
                    vibrate(70L)
                }
                EngineEvent.LOSE -> {
                    soundEngine.playGameOver()
                    vibrate(110L)
                }
            }
        }

        s.onShotComplete = {
            syncHud(s)
            viewModelScope.launch {
                val delta = max(0, s.removedCount - lastRecordedRemoved)
                if (delta > 0 || s.maxCombo > lastRecordedMaxCombo) {
                    lastRecordedRemoved = s.removedCount
                    lastRecordedMaxCombo = s.maxCombo
                    val unlocked = repository.checkAndUnlockAchievements()
                    repository.recordShotStats(delta, s.maxCombo)
                    if (unlocked.isNotEmpty()) {
                        soundEngine.playReward()
                        showToast("Achievement: ${unlocked.first()}")
                    }
                }
                if (!s.ended) {
                    repository.saveActiveSession(s.exportSnapshot(todayKey))
                }
            }
        }

        s.onGameEnded = { won, reason ->
            syncHud(s)
            viewModelScope.launch {
                val delta = max(0, s.removedCount - lastRecordedRemoved)
                if (delta > 0 || s.maxCombo > lastRecordedMaxCombo) {
                    lastRecordedRemoved = s.removedCount
                    lastRecordedMaxCombo = s.maxCombo
                    repository.recordShotStats(delta, s.maxCombo)
                }
                if (won) {
                    val stars = s.starsEarnedNow()
                    val prevUnlocked = saveState.value.profile.unlockedLevel
                    val result = repository.recordLevelWon(
                        score = s.score,
                        stars = stars,
                        levelNumber = s.config.levelNumber,
                        isDaily = s.isDaily
                    )
                    s.lastCoinsEarned = result.coinsEarned
                    if (result.unlockedBoosters.isNotEmpty()) {
                        soundEngine.playTreasure()
                    }
                    if (!s.isDaily && s.config.levelNumber == prevUnlocked && prevUnlocked < MAX_LEVELS) {
                        soundEngine.playLevelUnlock()
                    }
                    delay(750L)
                    if (_session.value === s && s.won) {
                        _victoryData.value = VictoryDisplayData(
                            levelNumber = s.config.levelNumber,
                            isDaily = s.isDaily,
                            score = s.score,
                            stars = stars,
                            coinsEarned = result.coinsEarned,
                            unlockedBoosters = result.unlockedBoosters,
                            doubledCoins = false,
                            isFinalLevel = !s.isDaily && s.config.levelNumber >= MAX_LEVELS
                        )
                        _activeModal.value = ActiveModal.Victory
                        if (result.newlyUnlockedAchievements.isNotEmpty()) {
                            showToast("Achievement: ${result.newlyUnlockedAchievements.first()}")
                        }
                    }
                } else {
                    repository.recordLevelLost(
                        score = s.score,
                        levelNumber = s.config.levelNumber,
                        isDaily = s.isDaily
                    )
                    delay(750L)
                    if (_session.value === s && s.ended && !s.won) {
                        _gameOverData.value = GameOverDisplayData(
                            levelNumber = s.config.levelNumber,
                            isDaily = s.isDaily,
                            score = s.score,
                            bestScore = max(saveState.value.profile.highScore, s.score),
                            reason = reason,
                            canOfferExtraShots = s.continuesUsed < 2
                        )
                        _activeModal.value = ActiveModal.GameOver
                    }
                }
            }
        }
    }

    fun syncHud(s: BubbleGameSession? = _session.value) {
        if (s == null) return
        val objText: String
        var objColor: BubbleColor? = null
        when (val o = s.config.objective) {
            is LevelObjective.ClearAll -> {
                objText = "Clear all bubbles (${s.remainingNonStoneCount()} left)"
            }
            is LevelObjective.TargetScore -> {
                objText = "Score ${s.score} / ${o.target}"
            }
            is LevelObjective.PopColor -> {
                objColor = o.color
                objText = "Pop ${o.color.label}: ${s.colorCleared.coerceAtMost(o.target)} / ${o.target}"
            }
            is LevelObjective.PopCount -> {
                objText = "Pop bubbles: ${s.removedCount.coerceAtMost(o.target)} / ${o.target}"
            }
        }
        val dropText = when {
            s.freezeTurns > 0 -> "Freeze ${s.freezeTurns}"
            s.config.dropEvery > 0 -> "Row in ${s.config.dropEvery - s.missCount}"
            else -> ""
        }
        _hudState.value = HudState(
            score = s.score,
            shots = s.shots,
            stars = s.starsEarnedNow(),
            levelLabel = if (s.isDaily) "Daily" else "Level ${s.config.levelNumber}",
            objectiveText = objText,
            objectiveBubbleColor = objColor,
            dropStatusText = dropText,
            armedBooster = s.armedBooster,
            isFreezeActive = s.freezeTurns > 0,
            isDaily = s.isDaily,
            noBooster = s.config.noBooster
        )
    }

    fun fireCurrentShot() {
        val s = _session.value ?: return
        if (_isPaused.value || s.ended) return
        val fired = s.fireShot { booster ->
            viewModelScope.launch { repository.consumeBooster(booster) }
        }
        if (fired) {
            syncHud(s)
        }
    }

    fun swapBubbles() {
        val s = _session.value ?: return
        if (_isPaused.value || s.ended) return
        if (s.swapBubbles()) {
            soundEngine.playButton()
            syncHud(s)
        }
    }

    fun onBoosterClicked(type: BoosterType) {
        val s = _session.value ?: return
        if (_isPaused.value || s.phase != GamePhase.READY || s.ended) return
        if (s.config.noBooster) {
            showToast("Boosters disabled in this challenge!")
            return
        }
        val count = saveState.value.boosterCount(type)
        if (count <= 0) {
            showToast("Out of ${type.title}s — visit the Shop!")
            return
        }
        if (type == BoosterType.FREEZE) {
            val activated = s.activateFreeze {
                viewModelScope.launch { repository.consumeBooster(BoosterType.FREEZE) }
            }
            if (!activated) {
                showToast("Freeze already active!")
            } else {
                syncHud(s)
            }
            return
        }
        if (s.toggleArmedBooster(type)) {
            soundEngine.playButton()
            syncHud(s)
        }
    }

    fun pauseGame() {
        val s = _session.value ?: return
        if (s.ended || _isPaused.value) return
        _isPaused.value = true
        soundEngine.playButton()
        _activeModal.value = ActiveModal.Pause
        saveSessionSnapshotNow()
    }

    fun resumeGame() {
        _isPaused.value = false
        _activeModal.value = null
        soundEngine.playButton()
    }

    fun saveSessionSnapshotNow() {
        val s = _session.value ?: return
        if (s.ended) return
        val today = _todayKey.value
        viewModelScope.launch {
            repository.saveActiveSession(s.exportSnapshot(today))
        }
    }

    fun claimDailyReward() {
        viewModelScope.launch {
            val res = repository.claimDailyReward()
            if (res == null) {
                showToast("Already claimed today!")
            } else {
                soundEngine.playTreasure()
                vibrate(40L)
                showToast("Day ${res.first}: ${res.second}!")
            }
        }
    }

    fun buyBooster(type: BoosterType) {
        viewModelScope.launch {
            val ok = repository.buyBooster(type)
            if (ok) {
                soundEngine.playCoin()
                showToast("Bought ${type.title}!")
            } else {
                soundEngine.playGameOver()
                vibrate(60L)
                showToast("Not enough coins!")
            }
        }
    }

    suspend fun refreshSpinAvailability() {
        val free = repository.canSpinFree()
        val adLeft = repository.remainingAdSpins()
        _spinState.value = _spinState.value.copy(canFreeSpin = free, remainingAdSpins = adLeft)
    }

    fun spinWheelFree() {
        if (_spinState.value.isSpinning) return
        performSpin(isAdSpin = false)
    }

    private fun performSpin(isAdSpin: Boolean) {
        viewModelScope.launch {
            val result = repository.consumeSpinAndGrantPrize(isAdSpin)
            if (result == null) {
                showToast("No spins remaining today!")
                refreshSpinAvailability()
                return@launch
            }
            val (idx, segment) = result
            val jitter = (Random.nextFloat() - 0.5f) * 26f
            val baseTarget = -(idx * 45f + 22.5f + jitter)
            val currentRot = _spinState.value.targetRotationDeg
            val delta = (((baseTarget - currentRot) % 360f) + 360f) % 360f
            val finalRot = currentRot + 1800f + delta

            _spinState.value = _spinState.value.copy(
                targetRotationDeg = finalRot,
                isSpinning = true,
                lastPrizeLabel = null
            )

            repeat(18) {
                delay(210L)
                soundEngine.playBounce()
            }
            delay(500L)

            soundEngine.playReward()
            vibrate(70L)
            val prizeText = formatSpinPrize(segment)
            _spinState.value = _spinState.value.copy(
                isSpinning = false,
                lastPrizeLabel = prizeText
            )
            refreshSpinAvailability()
            showToast("Won $prizeText!")
        }
    }

    private fun formatSpinPrize(segment: SpinSegment): String = when {
        segment.coins > 0 -> "${segment.coins} Coins"
        segment.booster != null -> "1 ${segment.booster.title}"
        else -> "+${segment.extraShots} Shots for Next Level"
    }

    // ---- Optional Rewarded Ad Flows (Start.io SDK Validated) ----

    fun watchAdForExtraShots(activity: Activity) {
        val s = _session.value ?: return
        if (!s.ended || s.won || s.continuesUsed >= 2) return
        adGateway.showRewardedAd(
            activity = activity,
            placement = RewardedPlacement.EXTRA_SHOTS,
            onRewardEarned = {
                s.grantExtraShotsAfterAd()
                _activeModal.value = null
                _gameOverData.value = null
                syncHud(s)
                soundEngine.playReward()
                showToast("+5 Extra Shots granted!")
            },
            onAdFailed = { msg -> showToast(msg) }
        )
    }

    fun watchAdForDoubleCoins(activity: Activity) {
        val current = _victoryData.value ?: return
        if (current.doubledCoins || current.coinsEarned <= 0) return
        adGateway.showRewardedAd(
            activity = activity,
            placement = RewardedPlacement.DOUBLE_COINS,
            onRewardEarned = {
                viewModelScope.launch {
                    repository.grantDoubleCoinsReward(current.coinsEarned)
                    _victoryData.value = current.copy(doubledCoins = true)
                    soundEngine.playCoin()
                    showToast("+${current.coinsEarned} bonus coins!")
                }
            },
            onAdFailed = { msg -> showToast(msg) }
        )
    }

    fun watchAdForFreeBooster(activity: Activity) {
        adGateway.showRewardedAd(
            activity = activity,
            placement = RewardedPlacement.FREE_BOOSTER,
            onRewardEarned = {
                viewModelScope.launch {
                    val granted = repository.grantAdBooster()
                    if (granted != null) {
                        soundEngine.playReward()
                        showToast("Free ${granted.title} added!")
                    } else {
                        showToast("Daily ad booster limit reached!")
                    }
                }
            },
            onAdFailed = { msg -> showToast(msg) }
        )
    }

    fun watchAdForDailyBonus(activity: Activity) {
        adGateway.showRewardedAd(
            activity = activity,
            placement = RewardedPlacement.DAILY_BONUS,
            onRewardEarned = {
                viewModelScope.launch {
                    val coins = repository.grantDailyBonusAdCoins()
                    if (coins > 0) {
                        soundEngine.playCoin()
                        showToast("+$coins bonus coins!")
                    } else {
                        showToast("Daily ad bonus already claimed!")
                    }
                }
            },
            onAdFailed = { msg -> showToast(msg) }
        )
    }

    fun watchAdForExtraSpin(activity: Activity) {
        if (_spinState.value.isSpinning) return
        viewModelScope.launch {
            if (repository.remainingAdSpins() <= 0) {
                showToast("Come back tomorrow for more spins!")
                return@launch
            }
            adGateway.showRewardedAd(
                activity = activity,
                placement = RewardedPlacement.EXTRA_SPIN,
                onRewardEarned = { performSpin(isAdSpin = true) },
                onAdFailed = { msg -> showToast(msg) }
            )
        }
    }

    // ---- Settings & Profile ----

    fun renamePlayer(newName: String) {
        viewModelScope.launch {
            repository.renamePlayer(newName)
            _activeModal.value = null
            showToast("Name updated!")
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSettings(soundEnabled = enabled)
            if (enabled) soundEngine.playButton()
        }
    }

    fun setMusicEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.updateSettings(musicEnabled = enabled) }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.updateSettings(vibrationEnabled = enabled) }
    }

    fun setSfxVolume(vol: Float) {
        viewModelScope.launch { repository.updateSettings(sfxVolume = vol) }
    }

    fun setMusicVolume(vol: Float) {
        viewModelScope.launch { repository.updateSettings(musicVolume = vol) }
    }

    fun resetProgress() {
        viewModelScope.launch {
            repository.resetAllProgress()
            _session.value = null
            _isPaused.value = false
            _activeModal.value = null
            showToast("Progress reset")
        }
    }

    @Suppress("DEPRECATION")
    private fun vibrate(ms: Long) {
        if (!saveState.value.profile.vibrationEnabled) return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                val v = appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    v?.vibrate(ms)
                }
            }
        }
    }

    class Factory(
        private val context: Context,
        private val repository: GameRepository,
        private val soundEngine: SoundEngine,
        private val adGateway: RewardedAdGateway
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BubbleBlitzViewModel(context.applicationContext, repository, soundEngine, adGateway) as T
        }
    }
}
