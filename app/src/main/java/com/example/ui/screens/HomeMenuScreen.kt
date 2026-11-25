package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Leaderboard
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GameSaveState
import com.example.game.engine.LevelGenerator
import com.example.game.model.MAX_LEVELS
import com.example.ui.components.CoinPill
import com.example.ui.components.GlossyButton
import com.example.ui.components.GlossyCircleButton
import com.example.ui.components.GlossyMenuRow
import com.example.ui.components.GlossyStyle
import com.example.ui.components.PlayerAvatarCanvas
import com.example.ui.components.TreasureChestCanvas
import com.example.ui.theme.GoldYellow

@Composable
fun HomeMenuScreen(
    saveState: GameSaveState,
    todayKey: String,
    dailyCountdown: String,
    onContinueClick: () -> Unit,
    onDailyRewardClick: () -> Unit,
    onDailyChallengeClick: () -> Unit,
    onLevelsClick: () -> Unit,
    onAchievementsClick: () -> Unit,
    onShopClick: () -> Unit,
    onSpinClick: () -> Unit,
    onLeaderboardClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onRenameClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile = saveState.profile
    val hasResume = saveState.savedSession != null &&
        !saveState.savedSession.isDaily &&
        saveState.savedSession.levelNumber == profile.currentLevel

    val dailyClaimed = profile.dailyLastClaimDate == todayKey
    val dailyChallengeDone = profile.challengeDate == todayKey && profile.challengeDone
    val challengeSubtitle = if (dailyChallengeDone) {
        "Completed today ✔"
    } else {
        LevelGenerator.describeObjective(LevelGenerator.generateDailyChallenge(todayKey))
    }

    val dailyBorderColor by animateColorAsState(
        targetValue = if (dailyClaimed) Color(0xFFA67CFF) else GoldYellow,
        label = "dailyBorder"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Profile & Coin Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .border(3.dp, GoldYellow, CircleShape)
                        .clickable(onClick = onRenameClick)
                ) {
                    PlayerAvatarCanvas(size = 54.dp, contentDescriptionText = "Player avatar, tap to rename")
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xCC2A1A8F))
                        .clickable(onClick = onRenameClick)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Level ${profile.currentLevel}  •  ★ ${saveState.totalStars}/${MAX_LEVELS * 3}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GoldYellow,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(0x55000000))
                    ) {
                        val frac = (saveState.completedLevelsCount.toFloat() / MAX_LEVELS).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(frac)
                                .height(7.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(Brush.horizontalGradient(listOf(Color(0xFFFFE36A), Color(0xFFFFA600))))
                        )
                    }
                }

                CoinPill(coins = profile.coins, onClick = onShopClick)

                GlossyCircleButton(
                    icon = Icons.Rounded.Settings,
                    contentDescriptionText = "Settings",
                    onClick = onSettingsClick,
                    size = 48.dp,
                    tag = "settings_button"
                )
            }

            Spacer(Modifier.height(10.dp))

            // Daily Reward Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("daily_reward_card")
                    .shadow(8.dp, RoundedCornerShape(22.dp))
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF5A2BB8), Color(0xFF2E1580))))
                    .border(3.dp, dailyBorderColor, RoundedCornerShape(22.dp))
                    .clickable(onClick = onDailyRewardClick)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TreasureChestCanvas(width = 74.dp, contentDescriptionText = "Daily reward treasure chest")
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Daily Reward",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 21.sp),
                        color = Color(0xFFFFE36A)
                    )
                    Text(
                        text = if (dailyClaimed) "Come back tomorrow!" else "Your reward is ready!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x66000000))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (dailyClaimed) "Next: $dailyCountdown" else "Tap to claim!",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                            color = if (dailyClaimed) Color.White else GoldYellow
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Continue / Resume Level Button
            GlossyButton(
                text = if (hasResume) "▶  Resume" else "▶  Continue",
                subtitle = "Level ${profile.currentLevel}",
                onClick = onContinueClick,
                style = GlossyStyle.YELLOW,
                fontSize = 26.sp,
                minHeight = 70.dp,
                tag = "continue_button",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))

            // Menu Rows
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GlossyMenuRow(
                    icon = Icons.Rounded.CalendarMonth,
                    title = "Daily Challenge",
                    subtitle = challengeSubtitle,
                    style = GlossyStyle.PURPLE,
                    trailingIcon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    onClick = onDailyChallengeClick,
                    tag = "daily_challenge_row"
                )
                GlossyMenuRow(
                    icon = Icons.Rounded.Map,
                    title = "Levels",
                    subtitle = "Explore 100 Levels in Green Valley",
                    style = GlossyStyle.GREEN,
                    trailingIcon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    onClick = onLevelsClick,
                    tag = "levels_row"
                )
                GlossyMenuRow(
                    icon = Icons.Rounded.Star,
                    title = "Achievements",
                    subtitle = "Earn Coins & Rewards",
                    style = GlossyStyle.BLUE,
                    trailingIcon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    onClick = onAchievementsClick,
                    tag = "achievements_row"
                )
            }

            Spacer(Modifier.height(12.dp))

            // Bottom Action Circles
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BottomCircularFeature(
                    icon = Icons.Rounded.ShoppingCart,
                    label = "Shop",
                    onClick = onShopClick,
                    tag = "shop_button"
                )
                BottomCircularFeature(
                    icon = Icons.Rounded.Casino,
                    label = "Lucky Spin",
                    onClick = onSpinClick,
                    tag = "spin_button"
                )
                BottomCircularFeature(
                    icon = Icons.Rounded.Leaderboard,
                    label = "Leaderboard",
                    onClick = onLeaderboardClick,
                    tag = "leaderboard_button"
                )
            }
        }
    }
}

@Composable
private fun BottomCircularFeature(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .testTag(tag)
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .shadow(6.dp, CircleShape)
                .background(Color(0xFF1E0A66), CircleShape)
                .padding(bottom = 4.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFFB88CFF), Color(0xFF5A25D0), Color(0xFF2F0F88))
                    )
                )
                .border(2.5.dp, Color.White.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
        )
    }
}
