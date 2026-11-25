package com.example

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.game.model.MAX_LEVELS
import com.example.ui.components.SceneBackgroundCanvas
import com.example.ui.components.ToastBanner
import com.example.ui.dialogs.AchievementsModal
import com.example.ui.dialogs.DailyChallengeModal
import com.example.ui.dialogs.DailyRewardModal
import com.example.ui.dialogs.GameOverModal
import com.example.ui.dialogs.LeaderboardModal
import com.example.ui.dialogs.LuckySpinModal
import com.example.ui.dialogs.PauseModal
import com.example.ui.dialogs.RenamePlayerModal
import com.example.ui.dialogs.ResetConfirmModal
import com.example.ui.dialogs.SettingsModal
import com.example.ui.dialogs.ShopModal
import com.example.ui.dialogs.VictoryModal
import com.example.ui.screens.GameScreen
import com.example.ui.screens.HomeMenuScreen
import com.example.ui.screens.LevelMapScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.BubbleBlitzTheme
import com.example.ui.viewmodel.ActiveModal
import com.example.ui.viewmodel.BubbleBlitzViewModel
import kotlinx.coroutines.delay

const val ROUTE_SPLASH = "splash"
const val ROUTE_MENU = "menu"
const val ROUTE_MAP = "level_map"
const val ROUTE_GAME = "game"

class MainActivity : ComponentActivity() {

    private val app by lazy { application as BubbleBlitzApp }

    private val viewModel: BubbleBlitzViewModel by viewModels {
        BubbleBlitzViewModel.Factory(
            context = applicationContext,
            repository = app.repository,
            soundEngine = app.soundEngine,
            adGateway = app.adManager
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        app.adManager.preload(this)

        setContent {
            BubbleBlitzTheme {
                BubbleBlitzRoot(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        app.soundEngine.setForeground(true)
        app.adManager.preload(this)
    }

    override fun onPause() {
        super.onPause()
        app.soundEngine.setForeground(false)
        viewModel.saveSessionSnapshotNow()
    }
}

@Composable
fun BubbleBlitzRoot(viewModel: BubbleBlitzViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_SPLASH
    val activity = LocalContext.current as? Activity

    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val activeModal by viewModel.activeModal.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()
    val isPaused by viewModel.isPaused.collectAsStateWithLifecycle()
    val hudState by viewModel.hudState.collectAsStateWithLifecycle()
    val victoryData by viewModel.victoryData.collectAsStateWithLifecycle()
    val gameOverData by viewModel.gameOverData.collectAsStateWithLifecycle()
    val spinState by viewModel.spinState.collectAsStateWithLifecycle()
    val todayKey by viewModel.todayKey.collectAsStateWithLifecycle()
    val dailyCountdown by viewModel.dailyCountdown.collectAsStateWithLifecycle()

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(2300L)
            viewModel.clearToast()
        }
    }

    // Handle Back button cleanly for modals & gameplay
    BackHandler(enabled = activeModal != null || currentRoute == ROUTE_GAME) {
        when {
            activeModal == ActiveModal.Pause -> viewModel.resumeGame()
            activeModal == ActiveModal.Victory || activeModal == ActiveModal.GameOver -> {
                viewModel.closeModal()
                navController.popBackStack(ROUTE_MENU, inclusive = false)
            }
            activeModal != null -> viewModel.closeModal()
            currentRoute == ROUTE_GAME -> viewModel.pauseGame()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Shared Cartoon Scene Background (Day on Splash/Menu, Night during Gameplay)
        if (currentRoute != ROUTE_MAP) {
            SceneBackgroundCanvas(
                isNight = currentRoute == ROUTE_GAME,
                modifier = Modifier.fillMaxSize()
            )
        }

        NavHost(
            navController = navController,
            startDestination = ROUTE_SPLASH,
            enterTransition = { fadeIn() + scaleIn(initialScale = 1.03f) },
            exitTransition = { fadeOut() }
        ) {
            composable(ROUTE_SPLASH) {
                SplashScreen(
                    onPlayClick = {
                        viewModel.soundEngine.playButton()
                        navController.navigate(ROUTE_MENU) {
                            popUpTo(ROUTE_SPLASH) { inclusive = true }
                        }
                    }
                )
            }

            composable(ROUTE_MENU) {
                HomeMenuScreen(
                    saveState = saveState,
                    todayKey = todayKey,
                    dailyCountdown = dailyCountdown,
                    onContinueClick = {
                        viewModel.soundEngine.playButton()
                        viewModel.startOrResumeLevel(saveState.profile.currentLevel)
                        navController.navigate(ROUTE_GAME)
                    },
                    onDailyRewardClick = { viewModel.openModal(ActiveModal.DailyReward) },
                    onDailyChallengeClick = { viewModel.openModal(ActiveModal.DailyChallenge) },
                    onLevelsClick = {
                        viewModel.soundEngine.playButton()
                        navController.navigate(ROUTE_MAP)
                    },
                    onAchievementsClick = { viewModel.openModal(ActiveModal.Achievements) },
                    onShopClick = { viewModel.openModal(ActiveModal.Shop) },
                    onSpinClick = { viewModel.openModal(ActiveModal.LuckySpin) },
                    onLeaderboardClick = { viewModel.openModal(ActiveModal.Leaderboard) },
                    onSettingsClick = { viewModel.openModal(ActiveModal.Settings) },
                    onRenameClick = { viewModel.openModal(ActiveModal.Rename) }
                )
            }

            composable(ROUTE_MAP) {
                LevelMapScreen(
                    saveState = saveState,
                    onBackClick = {
                        viewModel.soundEngine.playButton()
                        navController.popBackStack()
                    },
                    onLevelClick = { level ->
                        if (level > saveState.profile.unlockedLevel) {
                            viewModel.soundEngine.playButton()
                            viewModel.showToast("Complete level ${saveState.profile.unlockedLevel} first!")
                        } else {
                            viewModel.soundEngine.playButton()
                            viewModel.startOrResumeLevel(level)
                            navController.navigate(ROUTE_GAME)
                        }
                    }
                )
            }

            composable(ROUTE_GAME) {
                GameScreen(
                    session = session,
                    saveState = saveState,
                    hudState = hudState,
                    isPaused = isPaused,
                    onBoardSized = { w, h -> viewModel.onBoardSized(w, h) },
                    onFireShot = { viewModel.fireCurrentShot() },
                    onSwapBubbles = { viewModel.swapBubbles() },
                    onBoosterClick = { viewModel.onBoosterClicked(it) },
                    onPauseClick = { viewModel.pauseGame() }
                )
            }
        }

        // Modal Overlay Layer
        AnimatedVisibility(
            visible = activeModal != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val modal = activeModal
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xBD0A0428))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            if (modal != ActiveModal.Victory &&
                                modal != ActiveModal.GameOver &&
                                modal != ActiveModal.Pause &&
                                !spinState.isSpinning
                            ) {
                                viewModel.closeModal()
                            }
                        }
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                ) {
                    when (modal) {
                        ActiveModal.Pause -> PauseModal(
                            saveState = saveState,
                            viewModel = viewModel,
                            onResume = { viewModel.resumeGame() },
                            onRestart = {
                                val s = session
                                if (s != null) {
                                    viewModel.startOrResumeLevel(
                                        levelNumber = s.config.levelNumber,
                                        isDaily = s.isDaily,
                                        forceFresh = true
                                    )
                                }
                            },
                            onHome = {
                                viewModel.saveSessionSnapshotNow()
                                viewModel.closeModal()
                                navController.popBackStack(ROUTE_MENU, inclusive = false)
                            }
                        )

                        ActiveModal.Victory -> victoryData?.let { data ->
                            VictoryModal(
                                data = data,
                                activity = activity,
                                onNextLevel = {
                                    viewModel.closeModal()
                                    if (data.isDaily || data.levelNumber >= MAX_LEVELS) {
                                        navController.popBackStack(ROUTE_MENU, inclusive = false)
                                    } else {
                                        viewModel.startOrResumeLevel(data.levelNumber + 1, forceFresh = true)
                                    }
                                },
                                onReplay = {
                                    viewModel.closeModal()
                                    viewModel.startOrResumeLevel(data.levelNumber, isDaily = data.isDaily, forceFresh = true)
                                },
                                onWatchAdDoubleCoins = { act -> viewModel.watchAdForDoubleCoins(act) }
                            )
                        }

                        ActiveModal.GameOver -> gameOverData?.let { data ->
                            GameOverModal(
                                data = data,
                                activity = activity,
                                onWatchAdExtraShots = { act -> viewModel.watchAdForExtraShots(act) },
                                onRetry = {
                                    viewModel.closeModal()
                                    viewModel.startOrResumeLevel(data.levelNumber, isDaily = data.isDaily, forceFresh = true)
                                },
                                onHome = {
                                    viewModel.closeModal()
                                    navController.popBackStack(ROUTE_MENU, inclusive = false)
                                }
                            )
                        }

                        ActiveModal.DailyReward -> DailyRewardModal(
                            saveState = saveState,
                            todayKey = todayKey,
                            activity = activity,
                            viewModel = viewModel,
                            onClose = { viewModel.closeModal() }
                        )

                        ActiveModal.DailyChallenge -> DailyChallengeModal(
                            saveState = saveState,
                            todayKey = todayKey,
                            onPlayChallenge = {
                                viewModel.soundEngine.playButton()
                                viewModel.startOrResumeLevel(levelNumber = 0, isDaily = true)
                                navController.navigate(ROUTE_GAME)
                            },
                            onClose = { viewModel.closeModal() }
                        )

                        ActiveModal.Achievements -> AchievementsModal(
                            saveState = saveState,
                            onClose = { viewModel.closeModal() }
                        )

                        ActiveModal.Shop -> ShopModal(
                            saveState = saveState,
                            todayKey = todayKey,
                            activity = activity,
                            viewModel = viewModel,
                            onClose = { viewModel.closeModal() }
                        )

                        ActiveModal.LuckySpin -> LuckySpinModal(
                            spinState = spinState,
                            activity = activity,
                            viewModel = viewModel,
                            onClose = { if (!spinState.isSpinning) viewModel.closeModal() }
                        )

                        ActiveModal.Leaderboard -> LeaderboardModal(
                            saveState = saveState,
                            weekKey = remember(todayKey) { viewModel.weekKeyFor(todayKey) },
                            onClose = { viewModel.closeModal() }
                        )

                        ActiveModal.Settings -> SettingsModal(
                            saveState = saveState,
                            viewModel = viewModel,
                            onClose = { viewModel.closeModal() }
                        )

                        ActiveModal.Rename -> RenamePlayerModal(
                            currentName = saveState.profile.name,
                            onSave = { viewModel.renamePlayer(it) },
                            onClose = { viewModel.closeModal() }
                        )

                        ActiveModal.ResetConfirm -> ResetConfirmModal(
                            onConfirmReset = {
                                viewModel.resetProgress()
                                navController.popBackStack(ROUTE_MENU, inclusive = false)
                            },
                            onCancel = { viewModel.openModal(ActiveModal.Settings) }
                        )

                        null -> Unit
                    }
                }
            }
        }

        // Top Floating Toast
        ToastBanner(
            message = toastMessage,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 60.dp)
        )
    }
}
