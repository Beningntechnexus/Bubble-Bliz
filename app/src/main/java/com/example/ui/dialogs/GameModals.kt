package com.example.ui.dialogs

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GameSaveState
import com.example.game.engine.LevelGenerator
import com.example.game.model.ACHIEVEMENTS
import com.example.game.model.BoosterType
import com.example.game.model.DAILY_REWARD_TIERS
import com.example.ui.components.CoinBadgeIcon
import com.example.ui.components.GlossyButton
import com.example.ui.components.GlossyCircleButton
import com.example.ui.components.GlossyModalCard
import com.example.ui.components.GlossyStyle
import com.example.ui.theme.BrightLime
import com.example.ui.theme.FrostCyan
import com.example.ui.theme.GoldYellow
import com.example.ui.theme.SoftLavender
import com.example.ui.viewmodel.ActiveModal
import com.example.ui.viewmodel.BubbleBlitzViewModel
import kotlin.math.max

@Composable
fun ModalHeader(title: String, onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = Color(0xFFFFE36A)
        )
        GlossyCircleButton(
            icon = Icons.Rounded.Close,
            contentDescriptionText = "Close dialog",
            onClick = onClose,
            size = 40.dp,
            tag = "modal_close_button"
        )
    }
}

@Composable
fun PauseModal(
    saveState: GameSaveState,
    viewModel: BubbleBlitzViewModel,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onHome: () -> Unit
) {
    val p = saveState.profile
    GlossyModalCard {
        ModalHeader("Paused", onClose = onResume)
        ModalToggleRow("Sound Effects", p.soundEnabled) { viewModel.setSoundEnabled(it) }
        Spacer(Modifier.height(8.dp))
        ModalToggleRow("Background Music", p.musicEnabled) { viewModel.setMusicEnabled(it) }
        Spacer(Modifier.height(14.dp))
        GlossyButton("▶  RESUME", onClick = onResume, style = GlossyStyle.GREEN, tag = "resume_button", modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        GlossyButton("↻  RESTART", onClick = onRestart, style = GlossyStyle.BLUE, tag = "restart_button", modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        GlossyButton("⌂  HOME", onClick = onHome, style = GlossyStyle.PURPLE, tag = "pause_home_button", modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun DailyRewardModal(
    saveState: GameSaveState,
    todayKey: String,
    activity: Activity?,
    viewModel: BubbleBlitzViewModel,
    onClose: () -> Unit
) {
    val p = saveState.profile
    val claimedToday = p.dailyLastClaimDate == todayKey
    val currentDay = if (claimedToday) {
        p.dailyStreakDay.coerceIn(1, 7)
    } else {
        viewModel.nextDailyDay(todayKey)
    }
    val bonusDone = p.dailyBonusAdDate == todayKey

    GlossyModalCard {
        ModalHeader("Daily Reward", onClose = onClose)
        Column(
            modifier = Modifier
                .heightIn(max = 360.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val rows = DAILY_REWARD_TIERS.chunked(4)
            for (rowTiers in rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (tier in rowTiers) {
                        val isGot = if (claimedToday) tier.day <= currentDay else tier.day < currentDay
                        val isNow = !claimedToday && tier.day == currentDay
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isNow) Color(0x33FFD93A) else Color(0x40000000))
                                .border(
                                    width = 2.dp,
                                    color = if (isNow) GoldYellow else Color.White.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .alpha(if (isGot) 0.6f else 1f)
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Day ${tier.day}", style = MaterialTheme.typography.labelSmall, color = GoldYellow)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = tier.title,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2
                                )
                                if (isGot) {
                                    Spacer(Modifier.height(2.dp))
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = "Claimed", tint = BrightLime, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                    if (rowTiers.size < 4) {
                        repeat(4 - rowTiers.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        if (!claimedToday) {
            GlossyButton(
                text = "CLAIM DAY $currentDay",
                onClick = { viewModel.claimDailyReward() },
                style = GlossyStyle.YELLOW,
                tag = "claim_daily_button",
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Text(
                text = "Come back tomorrow for Day ${currentDay % 7 + 1}!",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            if (!bonusDone && activity != null) {
                Spacer(Modifier.height(8.dp))
                GlossyButton(
                    text = "WATCH AD – +100 BONUS COINS",
                    onClick = { viewModel.watchAdForDailyBonus(activity) },
                    style = GlossyStyle.GREEN,
                    fontSize = 15.sp,
                    tag = "daily_bonus_ad_button",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun DailyChallengeModal(
    saveState: GameSaveState,
    todayKey: String,
    onPlayChallenge: () -> Unit,
    onClose: () -> Unit
) {
    val done = saveState.profile.challengeDate == todayKey && saveState.profile.challengeDone
    val challengeDesc = remember(todayKey) {
        LevelGenerator.describeObjective(LevelGenerator.generateDailyChallenge(todayKey))
    }
    GlossyModalCard {
        ModalHeader("Daily Challenge", onClose = onClose)
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Rounded.CalendarMonth,
                contentDescription = null,
                tint = GoldYellow,
                modifier = Modifier.size(54.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = challengeDesc,
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFFFFE36A),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Reward: 200 Coins + 1 Free Booster",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )
            if (done) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Completed today ✔ Practice anytime for fun.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF9FFF7A),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(14.dp))
            GlossyButton(
                text = if (done) "▶  PRACTICE" else "▶  PLAY CHALLENGE",
                onClick = onPlayChallenge,
                style = GlossyStyle.GREEN,
                tag = "start_daily_challenge_button",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun AchievementsModal(
    saveState: GameSaveState,
    onClose: () -> Unit
) {
    val p = saveState.profile
    GlossyModalCard {
        ModalHeader("Achievements", onClose = onClose)
        Column(
            modifier = Modifier
                .heightIn(max = 420.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (def in ACHIEVEMENTS) {
                val unlocked = saveState.achievements.containsKey(def.id)
                val prog = when (def.id) {
                    "pop1", "b1000" -> p.bubblesPoppedTotal
                    "win1", "lv10", "lv25", "lv100" -> saveState.completedLevelsCount
                    "combo10" -> p.maxCombo
                    "star3" -> saveState.threeStarLevelsCount
                    "boost" -> p.boostersUsedTotal
                    else -> 0
                }.coerceAtMost(def.target)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x33000000))
                        .alpha(if (unlocked) 1f else 0.72f)
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = if (unlocked) GoldYellow else SoftLavender,
                        modifier = Modifier.size(30.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(def.title, style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text("${def.description}  •  +${def.rewardCoins} coins", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0x66000000))
                        ) {
                            val frac = (prog.toFloat() / def.target).coerceIn(0f, 1f)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(frac)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(BrightLime)
                            )
                        }
                    }
                    Text(
                        text = if (unlocked) "✔" else "$prog/${def.target}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (unlocked) BrightLime else Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun ShopModal(
    saveState: GameSaveState,
    todayKey: String,
    activity: Activity?,
    viewModel: BubbleBlitzViewModel,
    onClose: () -> Unit
) {
    val p = saveState.profile
    val adLeft = max(0, 3 - if (p.adBoosterDate == todayKey) p.adBoosterCount else 0)

    GlossyModalCard {
        ModalHeader("Booster Shop", onClose = onClose)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CoinBadgeIcon(24.dp)
            Spacer(Modifier.width(6.dp))
            Text(
                text = "%,d Coins".format(p.coins),
                style = MaterialTheme.typography.titleLarge,
                color = GoldYellow
            )
        }
        Column(
            modifier = Modifier
                .heightIn(max = 360.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (b in BoosterType.entries) {
                val owned = saveState.boosterCount(b)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x33000000))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = boosterIcon(b),
                        contentDescription = b.title,
                        tint = GoldYellow,
                        modifier = Modifier.size(30.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${b.title} (x$owned)", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text(b.description, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f), maxLines = 2)
                    }
                    GlossyButton(
                        text = "${b.price}",
                        onClick = { viewModel.buyBooster(b) },
                        style = GlossyStyle.YELLOW,
                        fontSize = 15.sp,
                        minHeight = 44.dp,
                        tag = "buy_${b.key}_button"
                    )
                }
            }
        }
        if (activity != null) {
            Spacer(Modifier.height(10.dp))
            GlossyButton(
                text = if (adLeft > 0) "WATCH AD – FREE BOOSTER ($adLeft left)" else "Daily free booster limit reached",
                onClick = { viewModel.watchAdForFreeBooster(activity) },
                enabled = adLeft > 0,
                style = GlossyStyle.GREEN,
                fontSize = 15.sp,
                tag = "shop_free_booster_ad_button",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun LeaderboardModal(
    saveState: GameSaveState,
    weekKey: String,
    onClose: () -> Unit
) {
    val p = saveState.profile
    val weekScore = if (p.weekKey == weekKey) p.weekScore else 0
    GlossyModalCard {
        ModalHeader("Leaderboard", onClose = onClose)
        Column(
            modifier = Modifier
                .heightIn(max = 430.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x337BE34A))
                    .border(1.5.dp, BrightLime, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "LOCAL ONLY — Scores and statistics are stored locally on this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            StatItemRow("Best Score", "%,d".format(p.highScore))
            StatItemRow("Highest Level Unlocked", "${p.unlockedLevel}")
            StatItemRow("Score This Week", "%,d".format(weekScore))
            StatItemRow("Total Stars Earned", "${saveState.totalStars}")
            StatItemRow("Bubbles Popped", "%,d".format(p.bubblesPoppedTotal))
            StatItemRow("Games Won / Played", "${p.gamesWon} / ${p.gamesPlayed}")

            Spacer(Modifier.height(4.dp))
            Text("Your Top Local Scores", style = MaterialTheme.typography.titleMedium, color = GoldYellow)
            if (saveState.topScores.isEmpty()) {
                Text("Play a level to record your first local score!", style = MaterialTheme.typography.bodyMedium, color = Color.White)
            } else {
                saveState.topScores.forEachIndexed { idx, entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x33000000))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("#${idx + 1}", style = MaterialTheme.typography.titleLarge, color = GoldYellow, modifier = Modifier.width(36.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${entry.playerName} (You)", style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Text(
                                text = (if (entry.levelNumber > 0) "Level ${entry.levelNumber}" else "Daily Challenge") + " • ${entry.dateKey}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                        Text("%,d".format(entry.score), style = MaterialTheme.typography.titleMedium, color = FrostCyan)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsModal(
    saveState: GameSaveState,
    viewModel: BubbleBlitzViewModel,
    onClose: () -> Unit
) {
    val p = saveState.profile
    GlossyModalCard {
        ModalHeader("Settings", onClose = onClose)
        Column(
            modifier = Modifier
                .heightIn(max = 440.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ModalToggleRow("Sound Effects", p.soundEnabled) { viewModel.setSoundEnabled(it) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x33000000))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("SFX Volume", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                Slider(
                    value = p.sfxVolume,
                    onValueChange = { viewModel.setSfxVolume(it) },
                    enabled = p.soundEnabled,
                    colors = SliderDefaults.colors(thumbColor = GoldYellow, activeTrackColor = BrightLime)
                )
            }
            ModalToggleRow("Background Music", p.musicEnabled) { viewModel.setMusicEnabled(it) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x33000000))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Music Volume", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                Slider(
                    value = p.musicVolume,
                    onValueChange = { viewModel.setMusicVolume(it) },
                    enabled = p.musicEnabled,
                    colors = SliderDefaults.colors(thumbColor = GoldYellow, activeTrackColor = BrightLime)
                )
            }
            ModalToggleRow("Vibration", p.vibrationEnabled) { viewModel.setVibrationEnabled(it) }

            GlossyButton(
                text = "RENAME PLAYER (${p.name})",
                onClick = { viewModel.openModal(ActiveModal.Rename) },
                style = GlossyStyle.BLUE,
                fontSize = 15.sp,
                modifier = Modifier.fillMaxWidth()
            )
            GlossyButton(
                text = "RESET GAME PROGRESS",
                onClick = { viewModel.openModal(ActiveModal.ResetConfirm) },
                style = GlossyStyle.RED,
                fontSize = 15.sp,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "BUBBLE BLITZ • Bitogin Tech • Pop. Strategize. Conquer.\nVersion 1.0.0",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun RenamePlayerModal(
    currentName: String,
    onSave: (String) -> Unit,
    onClose: () -> Unit
) {
    var nameInput by remember(currentName) { mutableStateOf(currentName) }
    GlossyModalCard {
        ModalHeader("Player Name", onClose = onClose)
        OutlinedTextField(
            value = nameInput,
            onValueChange = { if (it.length <= 12) nameInput = it },
            singleLine = true,
            label = { Text("Enter name (max 12 chars)") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = GoldYellow,
                unfocusedBorderColor = SoftLavender
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        GlossyButton(
            text = "SAVE NAME",
            onClick = { onSave(nameInput) },
            style = GlossyStyle.GREEN,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun ResetConfirmModal(
    onConfirmReset: () -> Unit,
    onCancel: () -> Unit
) {
    GlossyModalCard {
        ModalHeader("Reset Progress?", onClose = onCancel)
        Text(
            text = "This permanently deletes your unlocked levels, stars, coins, boosters, and achievements on this device.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White
        )
        Spacer(Modifier.height(14.dp))
        GlossyButton(
            text = "YES, RESET EVERYTHING",
            onClick = onConfirmReset,
            style = GlossyStyle.RED,
            fontSize = 16.sp,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        GlossyButton(
            text = "CANCEL",
            onClick = onCancel,
            style = GlossyStyle.PURPLE,
            fontSize = 16.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ModalToggleRow(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x33000000))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = Color.White)
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BrightLime)
        )
    }
}

@Composable
private fun StatItemRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x33000000))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = Color.White)
        Text(value, style = MaterialTheme.typography.titleMedium, color = GoldYellow)
    }
}

fun boosterIcon(type: BoosterType): ImageVector = when (type) {
    BoosterType.BOMB -> Icons.Rounded.Whatshot
    BoosterType.RAINBOW -> Icons.Rounded.AutoAwesome
    BoosterType.LIGHTNING -> Icons.Rounded.Bolt
    BoosterType.FIRE -> Icons.Rounded.LocalFireDepartment
    BoosterType.FREEZE -> Icons.Rounded.AcUnit
}
